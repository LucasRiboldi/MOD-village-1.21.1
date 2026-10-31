// ISS-20260713-020: ledger scan helpers for Stop-hook archive / backlog signals.

const { existsSync, readFileSync, statSync } = require("node:fs");
const { join } = require("node:path");
const { isHighValueArchiveCandidate } = require("./high-value-predicate.cjs");

const EDIT_COUNTER_FILE_REL = join(".fabric", ".cache", "edit-counter");
const ARCHIVE_BACKLOG_ANTI_LOOP_HOURS = 12;
const DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT = 2;
const DEFAULT_ARCHIVE_BACKLOG_IDLE_HOURS = 24;
const MS_PER_HOUR = 60 * 60 * 1000;
const FABRIC_DIR = ".fabric";
const EVENT_LEDGER_FILE = "events.jsonl";
// ISS-20260713-056/070: align with server events-jsonl-gates G7 default (10MB).
const EVENTS_JSONL_SIZE_WARN_BYTES = 10 * 1024 * 1024;
/** @type {{ path: string, mtimeMs: number, size: number, events: object[] } | null} */
let _ledgerCache = null;

function hasHighValueArchiveSignal(events, watermarkTs, sessionId) {
  return isHighValueArchiveCandidate(events, sessionId, watermarkTs);
}

/**
 * Read the events.jsonl ledger from <projectRoot>/.fabric/events.jsonl.
 * Mirrors pre-extract fabric-hint.readLedger / server readEventLedger semantics:
 *   - ENOENT → return []
 *   - full-file read (NOT a 256KB tail) — Stop-hook decide needs historical anchors
 *   - drop final fragment if file lacks trailing newline (partial-tail tolerance)
 *   - JSON.parse per line, swallow per-line errors (corrupt-line tolerance)
 *
 * Intentional contrast: event-reader.readRecentEvents is a bounded tail helper
 * for SessionStart/cite-policy paths. fabric-hint signal paths must stay full-file.
 */
function readLedger(projectRoot) {
  const eventPath = join(projectRoot, FABRIC_DIR, EVENT_LEDGER_FILE);
  if (!existsSync(eventPath)) {
    return [];
  }

  let st;
  try {
    st = statSync(eventPath);
  } catch {
    return [];
  }

  // ISS-20260713-048: process-local mtime+size cache (same-process re-entry).
  if (
    _ledgerCache &&
    _ledgerCache.path === eventPath &&
    _ledgerCache.mtimeMs === st.mtimeMs &&
    _ledgerCache.size === st.size
  ) {
    return _ledgerCache.events;
  }

  if (st.size > EVENTS_JSONL_SIZE_WARN_BYTES) {
    try {
      const mb = (st.size / (1024 * 1024)).toFixed(1);
      process.stderr.write(
        "[fabric-hint] events.jsonl is " +
          mb +
          "MB (>10MB); Stop-hook full-read may be slow. Run `fabric doctor --fix` to rotate, or set fabric_event_retention_days in .fabric/fabric-config.json.\n",
      );
    } catch {
      /* never block */
    }
  }

  let raw;
  try {
    raw = readFileSync(eventPath, "utf8");
  } catch {
    return [];
  }

  const lines = raw.split(/\r?\n/);
  const hasTrailingNewline = raw.endsWith("\n");
  if (!hasTrailingNewline && lines.length > 0) {
    lines.pop();
  }

  const events = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.length === 0) continue;
    try {
      const parsed = JSON.parse(trimmed);
      if (parsed && typeof parsed === "object") {
        events.push(parsed);
      }
    } catch {
      // corrupt JSON line — drop silently
    }
  }
  _ledgerCache = { path: eventPath, mtimeMs: st.mtimeMs, size: st.size, events };
  return events;
}

function clearLedgerCache() {
  _ledgerCache = null;
}

function countEditsSince(projectRoot, anchorTs) {
  const filePath = join(projectRoot, EDIT_COUNTER_FILE_REL);
  if (!existsSync(filePath)) return 0;
  let raw;
  try {
    raw = readFileSync(filePath, "utf8");
  } catch {
    return 0;
  }
  const lines = raw.split(/\r?\n/);
  let count = 0;
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.length === 0) continue;
    // rc.7 T4: support both line shapes —
    //   legacy (rc.6): bare ISO-8601 timestamp per line
    //   new (rc.7):    {"ts":"<iso>","paths":[...]} JSON per line
    let ms = Number.NaN;
    if (trimmed.charCodeAt(0) === 123 /* '{' */) {
      try {
        const obj = JSON.parse(trimmed);
        if (obj && typeof obj === "object" && typeof obj.ts === "string") {
          ms = Date.parse(obj.ts);
        }
      } catch {
        // fall through — malformed JSON, skip line
      }
    } else {
      ms = Date.parse(trimmed);
    }
    if (!Number.isFinite(ms)) continue; // malformed → skip
    if (anchorTs === null || ms > anchorTs) {
      count += 1;
    }
  }
  return count;
}

// ---------------------------------------------------------------------------
// Two-lane archive strategy (crack 1 + 2).
//
// In-session lane (crack 1): the archive nudge's edit trigger counts ONLY the
// current session's `file_mutated` events since the current session's OWN
// archive watermark — a neighbour window archiving (which moves the GLOBAL
// `knowledge_proposed` anchor) must never zero THIS window's unarchived work.
// We read the event ledger (file_mutated carries session_id, written by
// post-tooluse-mutation.cjs; session_archive_attempted carries
// covered_through_ts), NOT the session-blind `.fabric/.cache/edit-counter`
// sidecar — that stays for the activity-overview DISPLAY line only.
//
// Cross-session lane (crack 2): `countBacklogSessions` is the safety net that
// replaces the old global-24h timer (which any neighbour's archive reset, so a
// low-signal "dead" session was orphaned forever). It reads events.jsonl
// directly — never the resolved-bindings snapshot (KT-PIT-0017/0019 stale
// projection class).
// ---------------------------------------------------------------------------

// rc cross-session backlog constants. ANTI_LOOP mirrors archive-scan.ts.

function sessionArchiveWatermark(events, sessionId) {
  if (!Array.isArray(events) || typeof sessionId !== "string" || sessionId.length === 0) {
    return null;
  }
  let wm = null;
  for (const ev of events) {
    if (!ev || ev.session_id !== sessionId) continue;
    if (ev.event_type !== "session_archive_attempted") continue;
    if (typeof ev.covered_through_ts !== "number") continue;
    if (wm === null || ev.covered_through_ts > wm) wm = ev.covered_through_ts;
  }
  return wm;
}

// Earliest event ts carrying this session_id, else null.
function sessionFirstActivityTs(events, sessionId) {
  if (!Array.isArray(events) || typeof sessionId !== "string" || sessionId.length === 0) {
    return null;
  }
  let first = null;
  for (const ev of events) {
    if (!ev || ev.session_id !== sessionId || typeof ev.ts !== "number") continue;
    if (first === null || ev.ts < first) first = ev.ts;
  }
  return first;
}

// Per-session archive anchor: this session's own last archive watermark, else
// its first activity ts. null only when the session has zero ledger presence.
function sessionAnchorTs(events, sessionId) {
  const wm = sessionArchiveWatermark(events, sessionId);
  if (wm !== null) return wm;
  return sessionFirstActivityTs(events, sessionId);
}

// Count this session's `file_mutated` events strictly after the anchor (anchor
// null → count all of the session's mutations). Replaces the session-blind
// countEditsSince(edit-counter) for the archive TRIGGER (crack 1).
function countSessionMutationsSince(events, sessionId, anchorTs) {
  if (!Array.isArray(events) || typeof sessionId !== "string" || sessionId.length === 0) {
    return 0;
  }
  let count = 0;
  for (const ev of events) {
    if (!ev || ev.session_id !== sessionId) continue;
    if (ev.event_type !== "file_mutated" || typeof ev.ts !== "number") continue;
    if (anchorTs === null || ev.ts > anchorTs) count += 1;
  }
  return count;
}

// Cross-session safety net (crack 2). Counts DEAD sessions (carry a
// `session_ended` marker OR have been idle beyond idleHours) — OTHER than the
// current one — that hold unarchived high-value work and are NOT
// `user_dismissed` / inside the 12h anti-loop cooldown. This is the per-session
// replacement for the global-24h archive timer: it is NOT reset by any
// neighbour's archive, so a low-signal session that simply ended is no longer
// orphaned. Mirrors archive-scan.ts's outcome-filter semantics.
function countBacklogSessions(events, nowMs, currentSessionId, idleHours) {
  if (!Array.isArray(events)) return 0;
  const idleMs =
    (typeof idleHours === "number" && idleHours > 0 ? idleHours : DEFAULT_ARCHIVE_BACKLOG_IDLE_HOURS) *
    MS_PER_HOUR;
  const lastActivity = new Map(); // sid -> max ts
  const ended = new Set(); // sid with a session_ended marker
  const lastAttempt = new Map(); // sid -> latest session_archive_attempted event
  const sessions = new Set();
  for (const ev of events) {
    if (!ev || typeof ev.session_id !== "string" || ev.session_id.length === 0) continue;
    const sid = ev.session_id;
    sessions.add(sid);
    if (typeof ev.ts === "number") {
      const prev = lastActivity.get(sid);
      if (prev === undefined || ev.ts > prev) lastActivity.set(sid, ev.ts);
    }
    if (ev.event_type === "session_ended") ended.add(sid);
    if (ev.event_type === "session_archive_attempted" && typeof ev.ts === "number") {
      const prior = lastAttempt.get(sid);
      if (!prior || (typeof prior.ts === "number" && ev.ts > prior.ts)) lastAttempt.set(sid, ev);
    }
  }
  let count = 0;
  for (const sid of sessions) {
    if (sid === currentSessionId) continue; // live lane handles the current session
    const last = lastActivity.get(sid);
    const isDead = ended.has(sid) || (typeof last === "number" && nowMs - last >= idleMs);
    if (!isDead) continue;
    const attempt = lastAttempt.get(sid);
    if (attempt && attempt.outcome === "user_dismissed") continue; // respect dismissal
    if (
      attempt &&
      typeof attempt.ts === "number" &&
      nowMs - attempt.ts < ARCHIVE_BACKLOG_ANTI_LOOP_HOURS * MS_PER_HOUR
    ) {
      continue; // inside anti-loop cooldown
    }
    // Probe high-value work since the session's OWN archive watermark — null
    // (never archived) means probe the whole session (wm→0), so a high-value
    // signal that was the session's first event still counts. Using the
    // first-activity anchor here would wrongly exclude it (strict `> anchor`).
    const wm = sessionArchiveWatermark(events, sid);
    if (!hasHighValueArchiveSignal(events, wm, sid)) continue; // no unarchived high-value work
    count += 1;
  }
  return count;
}

function tallySessionActivity(events, sessionId) {
  let edits = 0;
  let consumed = 0;
  if (!Array.isArray(events) || typeof sessionId !== "string" || sessionId.length === 0) {
    return { edits, consumed };
  }
  for (const ev of events) {
    if (!ev || ev.session_id !== sessionId) continue;
    if (ev.event_type === "file_mutated") edits += 1;
    // ISS-20260711-222: production emits knowledge_body_read (KT-DEC-0030);
    // knowledge_consumed is retired as a live producer. Count both so historic
    // ledgers still contribute without zeroing modern sessions.
    else if (
      ev.event_type === "knowledge_body_read" ||
      ev.event_type === "knowledge_consumed"
    )
      consumed += 1;
  }
  return { edits, consumed };
}

function getTopEditedDirectories(projectRoot, topN, anchorTs) {
  const n = typeof topN === "number" && Number.isFinite(topN) && topN > 0
    ? Math.floor(topN)
    : 3;
  const filePath = join(projectRoot, EDIT_COUNTER_FILE_REL);
  if (!existsSync(filePath)) return [];
  let raw;
  try {
    raw = readFileSync(filePath, "utf8");
  } catch {
    return [];
  }
  const lines = raw.split(/\r?\n/);
  const counts = new Map();
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.length === 0) continue;
    // Only the JSON-line shape carries paths. Bare ISO lines (legacy rc.6
    // sidecar) cannot contribute to the activity overview.
    if (trimmed.charCodeAt(0) !== 123 /* '{' */) continue;
    let obj;
    try {
      obj = JSON.parse(trimmed);
    } catch {
      continue;
    }
    if (!obj || typeof obj !== "object") continue;
    // anchor gating mirrors countEditsSince() — strictly newer than anchor.
    if (typeof obj.ts === "string") {
      const ms = Date.parse(obj.ts);
      if (anchorTs !== null && Number.isFinite(ms) && ms <= anchorTs) continue;
      if (anchorTs !== null && !Number.isFinite(ms)) continue;
    } else if (anchorTs !== null) {
      // No parseable ts and an anchor was requested → can't decide, skip.
      continue;
    }
    const paths = Array.isArray(obj.paths) ? obj.paths : [];
    // Within one hook fire we dedupe the same directory bucket so a
    // MultiEdit that touched 5 files under packages/cli/ contributes 1 to
    // the bucket, not 5. The fire-cadence semantic stays consistent.
    const fireBuckets = new Set();
    for (const p of paths) {
      if (typeof p !== "string" || p.length === 0) continue;
      // Normalise to forward-slash for cross-platform stability and strip
      // any leading "./". POSIX-style only — the hook ships under POSIX
      // path conventions even on Windows (the project doesn't currently
      // ship a CRLF/backslash test matrix for the sidecar).
      //
      // v2.0.0-rc.27 TASK-005 (audit §2.8 leak surface): absolute paths
      // already accumulated in legacy sidecars start with `/`. We strip
      // the leading slash and also reject buckets that resolve to user-home
      // segments (`Users/<name>/...`, `home/<name>/...`) so historical
      // pollution from absolute-path writes doesn't surface the user's
      // $HOME in the archive banner. The rc.27 appendEditCounter no longer
      // writes such paths, but the sidecar is append-only so old lines
      // persist until rotation.
      let norm = p.replace(/\\/g, "/").replace(/^\.\//, "");
      // Strip leading `/` so a stale absolute entry doesn't generate a leak.
      while (norm.startsWith("/")) norm = norm.slice(1);
      const segs = norm.split("/").filter((s) => s.length > 0);
      // Reject any bucket whose top segments look like a host-system home
      // prefix. The pattern is `<top>/<user>/...` where top ∈ Users|home|root.
      // This silently drops legacy absolute-path entries from $HOME without
      // mangling the buckets for legitimate project-relative `Users/...`
      // (unlikely but possible) — the heuristic favours $HOME leak prevention
      // over false-positive bucketing of project paths named after Unix
      // conventions.
      if (segs.length >= 2 && (segs[0] === "Users" || segs[0] === "home" || segs[0] === "root")) {
        continue;
      }
      // v2.0.0-rc.27 TASK-005 (audit §2.8 file-as-dir): when segs[1] looks
      // like a file (contains a dot-extension at the end), surface segs[0]
      // alone instead of `segs[0]/segs[1]/` — a 2-seg path of the form
      // `assets/foo.ts` would otherwise render as "assets/foo.ts/" which
      // misleads the operator about whether they're seeing a file or a
      // directory. The extension regex is permissive: any `.X` where X is
      // 1-8 alphanumerics counts. README.md / package.json / foo.ts all
      // match; "v1.2" or "dotted.module" do too — acceptable false-positive
      // rate, since the worst outcome is over-aggregation to the parent.
      const looksLikeFile = (segment) => /\.[A-Za-z0-9]{1,8}$/u.test(segment);
      let bucket;
      if (segs.length >= 2) {
        if (looksLikeFile(segs[1])) {
          bucket = `${segs[0]}/`;
        } else {
          // Leading 2 segments: "packages/cli", "docs/decisions", etc. We
          // trail with "/" so the banner reads "packages/cli/" — clearly a
          // directory rather than a file basename.
          bucket = `${segs[0]}/${segs[1]}/`;
        }
      } else if (segs.length === 1) {
        // Single segment — treat the basename as its own bucket. Bare
        // root-level files (README.md, package.json) get some signal too.
        bucket = segs[0];
      } else {
        continue;
      }
      fireBuckets.add(bucket);
    }
    for (const b of fireBuckets) {
      counts.set(b, (counts.get(b) || 0) + 1);
    }
  }
  if (counts.size === 0) return [];
  const sorted = Array.from(counts.entries()).map(([dir, count]) => ({ dir, count }));
  // Sort desc by count; tie-break alphabetically so output is deterministic.
  sorted.sort((a, b) => (b.count - a.count) || (a.dir < b.dir ? -1 : a.dir > b.dir ? 1 : 0));
  return sorted.slice(0, n);
}


module.exports = {
  readLedger,
  clearLedgerCache,
  EVENTS_JSONL_SIZE_WARN_BYTES,
  hasHighValueArchiveSignal,
  sessionArchiveWatermark,
  sessionFirstActivityTs,
  sessionAnchorTs,
  countSessionMutationsSince,
  countBacklogSessions,
  tallySessionActivity,
  countEditsSince,
  getTopEditedDirectories,
  EDIT_COUNTER_FILE_REL,
  ARCHIVE_BACKLOG_ANTI_LOOP_HOURS,
  DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT,
  DEFAULT_ARCHIVE_BACKLOG_IDLE_HOURS,
};
