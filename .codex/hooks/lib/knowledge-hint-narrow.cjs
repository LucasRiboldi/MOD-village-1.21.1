#!/usr/bin/env node
/**
 * rc.6 TASK-020 (E2 + E4) — PreToolUse narrow-injection hook + edit-counter sidecar.
 * rc.6 TASK-021 (E3) — Session-hints cache emit gate (extends TASK-020).
 * rc.6 TASK-023 (E6) — Hint-silence-counter telemetry (companion to E4).
 *
 * Three coupled responsibilities behind a single PreToolUse trigger
 * (Edit / Write / MultiEdit):
 *
 *   E2 — Narrow knowledge hint
 *     Read the tool_input payload, extract the file path(s) the user is
 *     about to edit, dedupe within the request, then invoke
 *     `fabric plan-context-hint --paths p1,p2,...` and render any matching
 *     narrow-scoped knowledge entries to stderr so the Agent sees relevant
 *     decisions/pitfalls/guidelines *before* the edit lands.
 *
 *     Output contract (stderr only) when narrow.length > 0:
 *       [fabric] N narrow-scoped knowledge entries match your edit targets:
 *         [<id>] (<type>/<maturity>) <summary-line>
 *         [<id>] (<type>/<maturity>) <summary-line>
 *         ...
 *       (如需重读 broad 决策，跑 fabric plan-context-hint --all)
 *
 *     When narrow.length === 0: complete silence (exit 0, no stderr).
 *
 *   E3 — Session-hints cache (per-session dedupe)
 *     Read `.fabric/.cache/session-hints-{session_id}.json` BEFORE rendering.
 *     Cache shape:
 *       { session_id, revision_hash, hinted_paths: string[],
 *         hinted_stable_ids: string[], last_emitted_index_hash: string }
 *
 *     Emit-gate decision (in order):
 *       1. If cache.revision_hash !== current revision_hash → drop cache
 *          wholesale (treat as fresh; re-emit allowed).
 *       2. Compute current_index_hash = sha256(JSON.stringify(narrow ids));
 *          if it equals cache.last_emitted_index_hash → SKIP emit (silent).
 *       3. Filter narrow entries: drop any whose stable_id is already in
 *          cache.hinted_stable_ids. Also drop the request entirely if every
 *          target path is already in cache.hinted_paths.
 *       4. If filtered narrow set is non-empty → emit + update cache (append
 *          new paths + stable_ids, set last_emitted_index_hash).
 *
 *     session_id resolution: payload.session_id → env FABRIC_SESSION_ID →
 *     synthetic per-process UUID (degenerates to process-lifetime dedupe).
 *     Cache files are per-session; concurrent sessions never collide.
 *
 *   E4 — Edit-counter sidecar
 *     Unconditionally append one ISO-8601 timestamp line to
 *     `.fabric/.cache/edit-counter` per PreToolUse fire. This sidecar is
 *     consumed by TASK-022 (rc.6 E5) to upgrade Signal A from
 *     "hours-since-last-knowledge_proposed" to "edits-since-last-archive".
 *
 *     Runs BEFORE the CLI invocation so a CLI failure does not lose the
 *     counter signal. One line per fire, regardless of how many paths the
 *     request touched (the timestamp is per-invocation, not per-path).
 *
 * Stdout is intentionally empty. PreToolUse hooks may pollute stdout to
 * signal `decision:block`, but this hook is informational only — it never
 * blocks tool execution.
 *
 * Failure invariant: any error path (spawn failure, ENOENT, timeout,
 * JSON.parse throw, sidecar/cache write failure) MUST end in silent exit 0.
 * The hook never blocks Edit/Write/MultiEdit on its own malfunction.
 */

const { spawnSync } = require("node:child_process");
const { createHash, randomUUID } = require("node:crypto");
const {
  existsSync,
  mkdirSync,
  readFileSync,
  renameSync,
  statSync,
  writeFileSync,
} = require("node:fs");
const { dirname, join } = require("node:path");

// KT-GLD-0006: the rc.35 opaque-summary substitution (resolveOpaqueSummaries) is
// retired — the write-time mechanical floor in extractKnowledge prevents
// degenerate summaries at the source, so the narrow hook no longer band-aids them.
// v2.0.0-rc.37 NEW-17: shared sidecar I/O for the plan-context-hint result
// cache (skips a redundant CLI cold-start spawn when the same path-set is
// re-edited within a session and the knowledge graph hasn't changed).
const { readJsonStateAsync, writeJsonStateAsync } = require("./state-store.cjs");
const { createProjectContextResolver } = require("./project-root.cjs");
const toolPathExtract = require("./tool-path-extract.cjs");
const hintNarrowConfig = require("./hint-narrow-config.cjs");
const hintSummaryFormat = require("./hint-summary-format.cjs");
// W1-01 (ISS-011): the PreToolUse hook is the highest-frequency, most
// concurrency-exposed write surface in Fabric. Multi-window edits spawn
// concurrent hook processes that all append to the SAME non-session-scoped
// ledger/counter files; a bare appendFileSync can interleave a partial write
// and corrupt a line. Route every shared-file append through the advisory-lock
// primitive (drop-on-contention, best-effort — matches injection-log).
const { appendLockedLine } = require("./injection-log.cjs");
// lifecycle-refactor W1-T2: client discriminator for the hook_surface_emitted
// event (schema requires the `client` enum). Mirrors the broad hook's import.
// v2.2 dual-sink (Goal A): + emitDualSink (PreToolUse two-channel emit).
const { detectClient, emitDualSink } = require("./client-adapter.cjs");
// v2.2 dual-sink (Goal A / D4 + C5): human-output gate. On a narrow HIT the human
// systemMessage is gated by nudge_mode (a miss is already a silent early-return
// above); the AI additionalContext is emitted regardless (flow ⊥ observation).
// Optional require so an old install degrades to "always emit human".
let nudgePolicy = null;
try {
  nudgePolicy = require("./nudge-policy.cjs");
} catch {
  // Lib missing (old install) — human sink always emits (legacy behavior).
}
// v2.1.0-rc.1 P4 (F4/S63): hook-side reader for the CLI pre-generated
// resolved-bindings snapshot. Store-aware hint surfaces the write-target store
// for the edited file WITHOUT re-resolving or walking store trees. Best-effort.
let bindingsSnapshotReader = null;
try {
  bindingsSnapshotReader = require("./bindings-snapshot-reader.cjs");
} catch {
  // Lib missing (old install) — store labels degrade to silent absence.
}

// Read the project's own `project_id` (the snapshot key) from its config. Not a
// store-tree read — it is how the hook learns which snapshot to fetch.
function readProjectId(cwd) {
  try {
    const parsed = JSON.parse(readFileSync(join(cwd, ".fabric", "fabric-config.json"), "utf8"));
    return typeof parsed.project_id === "string" ? parsed.project_id : null;
  } catch {
    return null;
  }
}

// -----------------------------------------------------------------------------
// CONSTANTS
// -----------------------------------------------------------------------------

// `fabric plan-context-hint` is a thin wrapper over planContext(); on a
// well-seeded repo it returns in ~100ms. Two-second cap mirrors
// knowledge-hint-broad.cjs — any pathological hang must not stall edits.
const CLI_TIMEOUT_MS = 8000; // ISS-20260713-028: after body-less wire, still allow cold cache

// Maximum summary length per entry. Bounds each stderr line so a sloppy
// pending entry can't blow up terminal width. Truncation appends an ellipsis.
// v2.0.0-rc.33 W4-A3: `hint_summary_max_len` in fabric-config overrides this
// default (range 40..240). Resolved per-invocation via readSummaryMaxLen.
const DEFAULT_SUMMARY_MAX_LEN = 80;

// Edit-counter sidecar — workspace-relative path. Process-local file; no
// network. TASK-022 will read this back to compute edits-since-archive.
const EDIT_COUNTER_DIR_REL = join(".fabric", ".cache");
const EDIT_COUNTER_FILE = "edit-counter";

// rc.35 TASK-07 (P0-2): events.jsonl path. PreToolUse Edit fires append a
// `edit_intent_checked` event (ledger_source: 'hook') so doctor cite-
// coverage's editsTouched metric sees actual edit signals. Without this
// signal the entire cite-policy contract validation is structurally inert
// (rc.30 audit P0-2: 18582 turns / 240 edits / 0 events).
const EVENTS_LEDGER_DIR_REL = ".fabric";
const EVENTS_LEDGER_FILE = "events.jsonl";

// rc.6 TASK-023 (E6): hint-silence-counter sidecar — companion to the
// edit-counter above. Where edit-counter records every PreToolUse fire
// (numerator-agnostic), the silence-counter records only those fires that
// produced no narrow stderr emission (matched-narrow == 0 OR emit-gate
// returned render=false). Doctor lint #26 reads both files to derive a
// silence rate over a 30d window; a sustained >95% rate is a usage-pattern
// signal that narrow scope has drifted from where edits actually happen.
//
// Lives in the same .fabric/.cache/ directory so a single doctor cleanup
// pass can reason about both files together.
const HINT_SILENCE_COUNTER_DIR_REL = join(".fabric", ".cache");
const HINT_SILENCE_COUNTER_FILE = "hint-silence-counter";

// rc.6 TASK-021 (E3): session-hints cache lives alongside the edit-counter
// in .fabric/.cache/. One file per session, named session-hints-{id}.json.
// File-name prefix is referenced by the doctor lint #27 cleanup pass that
// deletes files with mtime older than 7 days.
const SESSION_HINTS_DIR_REL = join(".fabric", ".cache");
const SESSION_HINTS_FILE_PREFIX = "session-hints-";
const SESSION_HINTS_FILE_SUFFIX = ".json";

// Synthetic session id used when neither payload.session_id nor
// FABRIC_SESSION_ID is available. Generated once per process so a single
// hook invocation lifetime acts like a degenerate session (dedupes within
// the process; degrades back to per-fire renders on next spawn). This is
// the documented fallback chain — clients that want robust dedupe should
// pass session_id through the hook payload.
let SYNTHETIC_SESSION_ID = null;

// Tool names that trigger the narrow-injection branch. PreToolUse fires on
// many tool names across clients; we only react to file-edit tools.
const EDIT_TOOL_NAMES = new Set(["Edit", "Write", "MultiEdit", "apply_patch"]);

// v2.0.0-rc.33 W2 fabric-config keys & defaults. Mirror of the schema in
// packages/shared/src/schemas/fabric-config.ts — hooks cannot require()
// shared modules (rendered as standalone templates at init), so the values
// are duplicated inline. Keep these in sync if the schema changes.
const FABRIC_DIR_REL = ".fabric";
const FABRIC_CONFIG_FILE = "fabric-config.json";
// rc.37 NEW-17: derived index the server rewrites on any knowledge edit; its
// mtime is the cheap freshness token for the plan-context-hint result cache.
const AGENTS_META_FILE = "agents.meta.json";

// W2-1 (P0-9): narrow TopK upper bound. Five matches the per-Edit hint
// "terse banner" UX: any more and the model's working memory bloats.
const DEFAULT_HINT_NARROW_TOP_K = 5;

// W2-2 (P0-9): per-file dedup window in turns. Same (file_path, stable_id)
// stays silent for this many PreToolUse fires across sessions, addressing
// the rc.32 finding that a single hot file (e.g. GameRoom.tsx edited 30
// times in a row) re-fired identical narrow hints and trained the agent
// to ignore them. Distinct sidecar from session-hints (E3) so window-only
// suppression doesn't poison cross-session dedupe semantics.
const DEFAULT_HINT_NARROW_DEDUP_WINDOW_TURNS = 5;
// ONE FILE PER SESSION. This sidecar's `counter` is a TURN counter, and a turn
// is a property of one conversation — a counter shared across concurrently open
// client windows makes "within the last N turns" meaningless, and the unlocked
// read-modify-write loses whichever window writes first.
//
// The original comment called the global scope deliberate ("workspace-level"
// suppression so a hot file's repeated hints don't train the agent to ignore
// them). That rationale does not survive multi-window: the agent being trained
// is per-session, and window B's agent has never seen the hint window A saw, so
// suppressing it for B does not prevent any training — it just silently costs B
// the hint.
const NARROW_DEDUP_WINDOW_FILE_PREFIX = "narrow-dedup-window-";
const NARROW_DEDUP_WINDOW_FILE_SUFFIX = ".json";
// Cap the recent-emission ring buffer at this many records so the sidecar
// stays bounded on long-running workspaces. The window check only needs the
// last `window` entries per (path, entry_id) so a 4x safety multiplier is
// generous. Pruning happens lazily on write.
const NARROW_DEDUP_RING_CAP = 1000;

// W2-5 (P1-8): cooldown between narrow-hint re-emits in hours. 0 = no
// cooldown (rc.32 behavior, every PreToolUse fire is gate-eligible).
const DEFAULT_HINT_NARROW_COOLDOWN_HOURS = 0;
const MS_PER_HOUR = 60 * 60 * 1000;
const HINT_NARROW_LAST_EMIT_FILE = join(
  ".fabric",
  ".cache",
  "knowledge-hint-narrow-last-emit",
);

// W2-6 (P0-7): mirror of the broad hook flag — when true, emit the banner
// as a Claude Code PreToolUse hookSpecificOutput.additionalContext JSON
// envelope on stdout so the model receives the reminder IN-CONTEXT.
const DEFAULT_HINT_REMINDER_TO_CONTEXT = true;

// -----------------------------------------------------------------------------
// Payload parsing
// -----------------------------------------------------------------------------

/**
 * Read stdin (or a test-supplied raw string) as JSON. Returns null on any
 * parse failure — the hook stays silent rather than crashing the edit.
 */
function readPayload(rawStdin) {
  if (typeof rawStdin !== "string" || rawStdin.length === 0) return null;
  try {
    const parsed = JSON.parse(rawStdin);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return null;
    }
    return parsed;
  } catch (e) {
    // v2.0.0-rc.29 REVIEW (codex LOW-1): apply BUG-L1's malformed-input
    // diagnostic uniformly across hook scripts. fabric-hint.cjs got the stderr
    // trace in TASK-008; without this matching write here, a broken Codex
    // host payload silently kills the narrow hint with no operator
    // signal at all. Best-effort: a failed stderr write must not throw upward
    // (hook contract — never crash the host's edit pipeline).
    try {
      const message = (e && typeof e === "object" && "message" in e) ? String(e.message) : String(e);
      process.stderr.write(`[fabric-knowledge-hint-narrow] malformed input: ${message}\n`);
    } catch {
      // stderr write itself failed (sandbox / closed fd) — accept silence.
    }
    return null;
  }
}

/**
 * Extract the tool name from a hook payload. Both supported clients use the
 * same shape:
 *   - Claude Code:  { tool_name, tool_input: { ... } }
 *   - Codex CLI:    { tool_name, tool_input: { ... } } (mirrors Claude)
 * Returns null when no recognizable shape is present.
 */
function extractToolName(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (typeof payload.tool_name === "string") return payload.tool_name;
  return null;
}

/**
 * Extract the tool_input object from a hook payload (the `tool_input`
 * convention shared by Claude Code and Codex CLI).
 */
function extractToolInput(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (payload.tool_input && typeof payload.tool_input === "object") {
    return payload.tool_input;
  }
  return null;
}

/**
 * Pull file paths out of a tool_input object. Handles three shapes:
 *   - single Edit/Write: { file_path: "src/foo.ts", ... }
 *   - bulk variant:      { file_paths: ["src/foo.ts", "src/bar.ts"] }
 *   - MultiEdit:         { file_path: "...", edits: [{file_path?, ...}, ...] }
 *     (Claude Code's MultiEdit currently issues per-edit operations against
 *     a single `file_path`; older drafts carried per-edit `file_path`. We
 *     accept both to be defensive.)
 *
 * Returns a deduped array of strings — empty when no path is recognizable.
 * Order: first occurrence wins (stable across re-renders of the same payload).
 */

/**
 * ISS-20260711-212: harvest paths from a Codex apply_patch tool_input.
 * Codex may pass the patch body as a string (`input` / `patch` / `content`)
 * carrying `*** Update|Add|Delete File:` directives (same grammar fabric-hint
 * already parses for transcript digests).
 */
function extractApplyPatchPaths(toolInput) {
  return toolPathExtract.extractApplyPatchPaths(toolInput);
}

function extractPaths(toolInput) {
  return toolPathExtract.extractPaths(toolInput);
}

// -----------------------------------------------------------------------------
// Edit-counter sidecar (E4)
// -----------------------------------------------------------------------------

/**
 * Append a single line to .fabric/.cache/edit-counter recording a PreToolUse
 * fire. Creates the directory if missing. Best-effort: any write failure is
 * swallowed so a read-only .fabric/ never blocks the edit.
 *
 * Per TASK-020 convergence: ONE LINE per PreToolUse fire, regardless of how
 * many paths the request touched (the timestamp is per-invocation, not
 * per-path). TASK-022 (rc.6 E5) counts fires, not paths.
 *
 * rc.7 T4 upgrade — the line is now a JSON object:
 *   {"ts":"<ISO-8601>","paths":["a/b/c.ts","d/e.ts"]}
 * so the Stop hook can derive a "top edited directories" activity overview
 * for the 人-first reminder banner (Signal A).
 *
 * Back-compat:
 *   - countEditsSince() reads each line by extracting the first ISO-8601
 *     substring it sees (works on both JSON-line and legacy plain-ISO files).
 *   - Existing sidecars from rc.6 (plain ISO per line) continue to count
 *     correctly; the activity-overview helper simply skips lines with no
 *     `paths` array.
 *   - When the caller cannot supply paths (e.g. unrecognized tool, payload
 *     parse failure) we still write the JSON line with an empty `paths`
 *     array. The fire-count signal is preserved; the activity overview
 *     just contributes nothing from those lines.
 */
/**
 * rc.35 TASK-07 (P0-2): append one `edit_intent_checked` event per touched
 * path to `.fabric/events.jsonl`. Carries `ledger_source: 'hook'` so doctor
 * cite-coverage can distinguish hook-originated edit signals from
 * AI/human-originated `appendLedgerEntry` calls.
 *
 * Best-effort:
 *   - Skips silently when `.fabric/` does not exist (project not init'd).
 *   - Skips silently when paths is empty (counter signal is preserved by
 *     the sibling appendEditCounter call; cite-coverage only cares about
 *     non-empty path events).
 *   - ANY error (mkdir, append, JSON throw) is swallowed — the hook must
 *     remain non-blocking per the rc.6 contract.
 *
 * Atomicity:
 *   - One JSON line per path. Append on small writes (< PIPE_BUF, ~4KB on
 *     POSIX) is atomic at the OS level, so concurrent PreToolUse fires
 *     from parallel sessions interleave cleanly without partial writes.
 */
function appendEditIntentToLedger(projectRoot, now, paths, toolName, sessionId) {
  try {
    const fabricDir = join(projectRoot, EVENTS_LEDGER_DIR_REL);
    // No .fabric/ → project not initialised. Bail before any write.
    if (!existsSync(fabricDir)) return;
    const { isAbsolute: pathIsAbsolute, relative: pathRelative } = require("node:path");
    const pathList = Array.isArray(paths)
      ? paths
          .filter((p) => typeof p === "string" && p.length > 0)
          .map((p) => {
            if (pathIsAbsolute(p)) {
              const rel = pathRelative(projectRoot, p);
              return rel.startsWith("..") ? null : rel;
            }
            // Already-relative paths: drop ones that escape the project tree.
            return p.startsWith("..") ? null : p;
          })
          .filter((p) => typeof p === "string" && p.length > 0)
          // Use forward slashes for cross-platform consistency on disk.
          .map((p) => p.split(/[\\/]/).join("/"))
      : [];
    if (pathList.length === 0) return;
    const tsMs = now instanceof Date ? now.getTime() : Number(now);
    const ledgerEntryId = `hook:${randomUUID()}`;
    const intent = typeof toolName === "string" && toolName.length > 0 ? toolName : "edit";
    // rc.38 UX-8 (C): thread the REAL payload session_id (never the synthetic
    // fallback) so doctor cite-coverage's expected_but_missed arm can correlate
    // this edit against the same session's assistant_turn cite lines. Omitting
    // it (the rc.35 oversight) left the correlation key undefined → missed
    // permanently 0 → cite_compliance_rate structurally pinned at 100%.
    const validSessionId =
      typeof sessionId === "string" && sessionId.length > 0 ? sessionId : null;
    const lines = pathList
      .map((p) => JSON.stringify({
        kind: "fabric-event",
        id: `event:${randomUUID()}`,
        ts: tsMs,
        schema_version: 1,
        ...(validSessionId ? { session_id: validSessionId } : {}),
        event_type: "edit_intent_checked",
        path: p,
        compliant: true,
        intent,
        ledger_entry_id: ledgerEntryId,
        ledger_source: "hook",
        matched_rule_context_ts: null,
        window_ms: 0,
      }))
      .join("\n") + "\n";
    appendLockedLine(join(fabricDir, EVENTS_LEDGER_FILE), lines);
  } catch {
    // Silent — events ledger failure must never block the edit.
  }
}

function appendEditCounter(projectRoot, now, paths) {
  try {
    const dir = join(projectRoot, EDIT_COUNTER_DIR_REL);
    const file = join(dir, EDIT_COUNTER_FILE);
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
    }
    const iso = now instanceof Date ? now.toISOString() : new Date(now).toISOString();
    // v2.0.0-rc.27 TASK-005 (audit §2.8): normalize every path to a
    // project-relative form BEFORE persistence. rc.26 wrote whatever the
    // tool_input handed in — frequently absolute paths like
    // `/Users/wepie/.../foo.ts` — which then leaked into the archive banner's
    // "recent activity centered on: Users/wepie/" prose (the dirname pass
    // stripped the leading `/` but produced a $HOME-prefix surface). The
    // normalize-on-write keeps the sidecar containing only project-internal
    // paths so downstream banner rendering can't accidentally surface
    // host-system paths.
    //
    // Strategy: for each path, attempt path.relative(projectRoot, abs). When
    // the result starts with `..` (path is outside the project tree) we
    // silently drop the entry — out-of-tree edits are not meaningful
    // activity for THIS project's banner. Bare relative paths (already in
    // canonical form) round-trip through relative() unchanged.
    const { isAbsolute: pathIsAbsolute, relative: pathRelative } = require("node:path");
    const pathList = Array.isArray(paths)
      ? paths
          .filter((p) => typeof p === "string" && p.length > 0)
          .map((p) => {
            if (pathIsAbsolute(p)) {
              const rel = pathRelative(projectRoot, p);
              // path.relative returns `..` segments when p escapes projectRoot.
              return rel.startsWith("..") ? null : rel;
            }
            return p;
          })
          .filter((p) => typeof p === "string" && p.length > 0)
      : [];
    const line = JSON.stringify({ ts: iso, paths: pathList });
    appendLockedLine(file, `${line}\n`);
  } catch {
    // Silent — sidecar failure must never block the edit.
  }
}

/**
 * rc.6 TASK-023 (E6): append one ISO-8601 timestamp line to
 * `.fabric/.cache/hint-silence-counter`. Called from main() on every silent
 * fire path — i.e. when the hook completes without emitting any narrow
 * stderr lines. This includes:
 *
 *   - matched-narrow == 0 (CLI returned an empty narrow set)
 *   - emit-gate render === false (session-hints dedupe filtered everything
 *     out)
 *
 * Together with appendEditCounter (E4), this lets doctor lint #26 compute a
 * silence rate: silence_count / total_fires over a rolling window. The
 * write semantics mirror appendEditCounter exactly — single timestamp line
 * per silent fire, best-effort (failures swallowed), directory created if
 * missing.
 */
function appendHintSilenceCounter(projectRoot, now) {
  try {
    const dir = join(projectRoot, HINT_SILENCE_COUNTER_DIR_REL);
    const file = join(dir, HINT_SILENCE_COUNTER_FILE);
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
    }
    const iso = now instanceof Date ? now.toISOString() : new Date(now).toISOString();
    appendLockedLine(file, `${iso}\n`);
  } catch {
    // Silent — sidecar failure must never block the edit.
  }
}

// -----------------------------------------------------------------------------
// Session-hints cache (E3) — per-session emit-gate
// -----------------------------------------------------------------------------

/**
 * Resolve the session id used to key the cache file. Priority:
 *   1. payload.session_id (string, non-empty) — preferred; threads through
 *      from the client hook payload (Claude Code / Codex CLI).
 *   2. process.env.FABRIC_SESSION_ID — environment fallback.
 *   3. SYNTHETIC_SESSION_ID — a process-lifetime UUID, generated lazily so
 *      tests can stub it (see resetSyntheticSessionId).
 *
 * The synthetic id keeps the emit-gate honest even when no upstream id is
 * available: a single hook spawn won't re-render the same hint twice within
 * its process lifetime, but a fresh spawn starts a new "session" — which is
 * the documented degradation for clients that don't pass session_id.
 */
function resolveSessionId(payload, env) {
  if (payload && typeof payload === "object") {
    const fromPayload = payload.session_id;
    if (typeof fromPayload === "string" && fromPayload.length > 0) {
      return fromPayload;
    }
  }
  const envBag = (env && env.processEnv) || process.env;
  const fromEnv = envBag && envBag.FABRIC_SESSION_ID;
  if (typeof fromEnv === "string" && fromEnv.length > 0) {
    return fromEnv;
  }
  if (SYNTHETIC_SESSION_ID === null) {
    try {
      SYNTHETIC_SESSION_ID = randomUUID();
    } catch {
      // randomUUID is available on Node >= 14.17 / 16; if it ever throws,
      // fall back to a coarse pid/time stamp so the cache still keys on
      // something stable for the process lifetime.
      SYNTHETIC_SESSION_ID = `pid-${process.pid}-${Date.now()}`;
    }
  }
  return SYNTHETIC_SESSION_ID;
}

/**
 * Test seam: reset the synthetic session id cache. Lets unit tests verify
 * the fallback chain independently per case.
 */
function resetSyntheticSessionId() {
  SYNTHETIC_SESSION_ID = null;
}

/**
 * Compute the absolute path to a session-hints cache file. Exposed as a
 * helper so the doctor cleanup pass and tests share the same naming
 * convention.
 */
function sessionHintsCachePath(projectRoot, sessionId) {
  // ISS-20260711-259: sanitize session_id before embedding in a filesystem path
  // (mirrors narrowResultCacheFileName). Untrusted session ids must not introduce
  // path separators or traversal segments into .fabric/.cache/.
  const safe = String(sessionId || "anonymous")
    .replace(/[^A-Za-z0-9_.-]/g, "-")
    .replace(/^[.-]+/, "")
    .slice(0, 128) || "anonymous";
  return join(
    projectRoot,
    SESSION_HINTS_DIR_REL,
    `${SESSION_HINTS_FILE_PREFIX}${safe}${SESSION_HINTS_FILE_SUFFIX}`,
  );
}

/**
 * Load + parse the session-hints cache for `sessionId`. Returns null on
 * any failure (missing file, parse error, shape mismatch). Never throws —
 * cache miss falls through to a fresh emit.
 */
function readSessionHintsCache(projectRoot, sessionId) {
  try {
    const file = sessionHintsCachePath(projectRoot, sessionId);
    if (!existsSync(file)) return null;
    const raw = readFileSync(file, "utf8");
    if (raw.length === 0) return null;
    const parsed = JSON.parse(raw);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return null;
    }
    // Defensive shape coercion: missing fields default to safe empties so
    // downstream code can treat the result as a fully-shaped cache.
    return {
      session_id:
        typeof parsed.session_id === "string" ? parsed.session_id : sessionId,
      revision_hash:
        typeof parsed.revision_hash === "string" ? parsed.revision_hash : "",
      hinted_paths: Array.isArray(parsed.hinted_paths)
        ? parsed.hinted_paths.filter((p) => typeof p === "string" && p.length > 0)
        : [],
      hinted_stable_ids: Array.isArray(parsed.hinted_stable_ids)
        ? parsed.hinted_stable_ids.filter(
            (id) => typeof id === "string" && id.length > 0,
          )
        : [],
      last_emitted_index_hash:
        typeof parsed.last_emitted_index_hash === "string"
          ? parsed.last_emitted_index_hash
          : "",
    };
  } catch {
    return null;
  }
}

/**
 * Atomically write the session-hints cache. Writes to a sibling tmp file
 * and renames into place — keeps observers from reading a half-written
 * JSON document. Silent on any failure (read-only fs, ENOSPC, etc).
 */
function writeSessionHintsCache(projectRoot, cache) {
  try {
    const dir = join(projectRoot, SESSION_HINTS_DIR_REL);
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
    }
    const file = sessionHintsCachePath(projectRoot, cache.session_id);
    const tmp = `${file}.tmp-${process.pid}`;
    writeFileSync(tmp, JSON.stringify(cache, null, 2), "utf8");
    renameSync(tmp, file);
  } catch {
    // Silent — cache write must never block the edit.
  }
}

/**
 * Compute a stable index hash for the narrow set. Sorted stable_ids are
 * concatenated so two calls with the same id set (regardless of CLI output
 * order) hash identically. Returns "" for an empty narrow set so the
 * emit-gate never accidentally short-circuits on empty input (the empty
 * branch is handled earlier in main()).
 */
function computeIndexHash(narrow) {
  if (!Array.isArray(narrow) || narrow.length === 0) return "";
  const ids = narrow
    .map((entry) => (entry && typeof entry.id === "string" ? entry.id : ""))
    .filter((id) => id.length > 0)
    .slice()
    .sort();
  if (ids.length === 0) return "";
  return createHash("sha256").update(JSON.stringify(ids)).digest("hex");
}

/**
 * Apply the emit-gate. Returns `{ render, narrow, cache }`:
 *   render: boolean — true if the caller should render to stderr
 *   narrow: NarrowEntry[] — filtered set (drops already-hinted stable_ids)
 *   cache:  the merged cache object to persist if render === true
 *
 * Semantics (mirror the file header):
 *   1. revision_hash mismatch (or empty cache.revision_hash, or no existing
 *      cache) → treat as fresh. Filter is identity on input.
 *   2. revision_hash matches AND every target path is in cache.hinted_paths
 *      → skip emit silently (returns render=false).
 *   3. revision_hash matches AND current_index_hash equals
 *      cache.last_emitted_index_hash → skip (returns render=false).
 *   4. Otherwise filter narrow by hinted_stable_ids; if the filtered set is
 *      empty → skip (render=false). Else render the filtered set.
 *
 * The caller (main) commits the cache via writeSessionHintsCache only when
 * render === true — keeps cache writes coupled to actual stderr emissions.
 */
function applyEmitGate(cache, narrow, targetPaths, currentRevisionHash) {
  // Branch 1: no cache or stale revision_hash → fresh emit.
  const isFresh =
    cache === null ||
    typeof cache.revision_hash !== "string" ||
    cache.revision_hash.length === 0 ||
    cache.revision_hash !== currentRevisionHash;

  const currentIndexHash = computeIndexHash(narrow);

  // Effective cache view for the merge step. Fresh runs start from an empty
  // baseline; non-fresh inherit the prior session's accumulation.
  const baseline = isFresh
    ? {
        session_id: cache && typeof cache.session_id === "string" ? cache.session_id : "",
        revision_hash: currentRevisionHash,
        hinted_paths: [],
        hinted_stable_ids: [],
        last_emitted_index_hash: "",
      }
    : cache;

  if (!isFresh) {
    // Branch 2: every target path already hinted.
    const allPathsKnown =
      targetPaths.length > 0 &&
      targetPaths.every((p) => baseline.hinted_paths.includes(p));
    if (allPathsKnown) {
      return { render: false, narrow: [], cache: baseline };
    }

    // Branch 3: index hash matches the last emission verbatim.
    if (
      currentIndexHash.length > 0 &&
      currentIndexHash === baseline.last_emitted_index_hash
    ) {
      return { render: false, narrow: [], cache: baseline };
    }
  }

  // Branch 4: filter narrow entries whose stable_id is already known.
  const knownIds = new Set(baseline.hinted_stable_ids);
  const filtered = narrow.filter(
    (entry) => !(entry && typeof entry.id === "string" && knownIds.has(entry.id)),
  );
  if (filtered.length === 0) {
    return { render: false, narrow: [], cache: baseline };
  }

  // Build the to-persist cache. Append new paths + stable_ids, refresh
  // index hash. Use Set-based merge to preserve uniqueness without
  // allocating a Set on every emit.
  const mergedPaths = mergeUnique(baseline.hinted_paths, targetPaths);
  const newIds = filtered
    .map((e) => (e && typeof e.id === "string" ? e.id : ""))
    .filter((id) => id.length > 0);
  const mergedIds = mergeUnique(baseline.hinted_stable_ids, newIds);

  return {
    render: true,
    narrow: filtered,
    cache: {
      session_id: baseline.session_id,
      revision_hash: currentRevisionHash,
      hinted_paths: mergedPaths,
      hinted_stable_ids: mergedIds,
      last_emitted_index_hash: currentIndexHash,
    },
  };
}

// Order-preserving dedupe merge — extracted because both hinted_paths and
// hinted_stable_ids share the same merge semantics.
function mergeUnique(existing, incoming) {
  const seen = new Set(existing);
  const out = existing.slice();
  for (const item of incoming) {
    if (typeof item !== "string" || item.length === 0) continue;
    if (seen.has(item)) continue;
    seen.add(item);
    out.push(item);
  }
  return out;
}

// -----------------------------------------------------------------------------
// CLI invocation (E2)
// -----------------------------------------------------------------------------

/**
 * Normalize hook-supplied file paths to the project-root-relative,
 * forward-slashed form `plan-context-hint` requires.
 *
 * Claude Code / Codex pass `tool_input.file_path` as an ABSOLUTE path, but
 * `plan-context-hint` (planContext) REJECTS absolute paths and exits non-zero —
 * a failure the best-effort spawn swallows, which silently zeroed EVERY narrow
 * hint (the entire narrow-scoped KB tier never surfaced in real sessions).
 * Mirrors the appendEditCounter / appendEditIntentToLedger normalization.
 * `cwd` is the git-anchored project root (KT-DEC-0050). Paths that escape the
 * project tree (relative starts with "..") are dropped. Returns a
 * possibly-empty array; the caller treats empty as "nothing to hint".
 */
function toProjectRelativePaths(cwd, paths) {
  return toolPathExtract.toProjectRelativePaths(cwd, paths);
}

/**
 * Spawn `fabric plan-context-hint --paths p1,p2,...` and return parsed JSON.
 * Returns null on any failure (ENOENT, non-zero exit, malformed JSON,
 * timeout). Never throws.
 *
 * Spawn strategy mirrors knowledge-hint-broad.cjs: try `fabric` first, then
 * `fab`. If neither is on PATH, return null — the hook stays silent.
 */
function invokePlanContextHint(cwd, paths) {
  if (!Array.isArray(paths) || paths.length === 0) return null;
  // Normalize ABSOLUTE hook paths → project-relative BEFORE the CLI sees them;
  // planContext rejects absolute paths and the swallowed failure would silently
  // zero every narrow hint. See toProjectRelativePaths for the full rationale
  // (KT-DEC-0050). Empty after normalization → nothing in-tree to hint.
  const relPaths = toProjectRelativePaths(cwd, paths);
  if (relPaths.length === 0) return null;
  const pathsArg = relPaths.join(",");
  const candidates = ["fabric", "fab"];
  // rc.31 NEW-6: see knowledge-hint-broad.cjs for rationale — surface plan-
  // context-hint failures on stderr so degraded KB chain is observable.
  let lastFailure = null;
  for (const bin of candidates) {
    let res;
    try {
      res = spawnSync(bin, ["plan-context-hint", "--paths", pathsArg], {
        cwd,
        encoding: "utf8",
        timeout: CLI_TIMEOUT_MS,
        stdio: ["ignore", "pipe", "pipe"],
      });
    } catch {
      continue;
    }
    if (res.error) {
      if (res.error.code !== "ENOENT") {
        lastFailure = { bin, reason: String(res.error.message || res.error.code || res.error) };
      }
      continue;
    }
    if (res.status === null || res.status !== 0) {
      const stderrSnip = (res.stderr || "").trim().slice(0, 240);
      if (stderrSnip.length > 0) {
        lastFailure = { bin, reason: stderrSnip };
      }
      continue;
    }
    const raw = (res.stdout || "").trim();
    if (raw.length === 0) continue;
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object") return parsed;
    } catch (err) {
      lastFailure = { bin, reason: `malformed JSON from plan-context-hint: ${String(err && err.message || err)}` };
    }
  }
  if (lastFailure !== null) {
    process.stderr.write(
      `[fabric-hint] plan-context-hint (${lastFailure.bin}) failed: ${lastFailure.reason.replace(/\n/g, " ")}\n`,
    );
    // P1 observability: a REAL CLI failure (non-ENOENT) previously left NO
    // structured trace — only this ephemeral stderr line — so a systemic
    // narrow-chain outage (e.g. the absolute-path rejection that silently
    // zeroed narrow surfacing for a month) was invisible to doctor/metrics.
    // Emit a best-effort `narrow_hint_failed` event so the degradation is
    // countable in events.jsonl. ENOENT (fabric not on PATH) leaves lastFailure
    // null and is deliberately NOT recorded — that is the benign "not installed"
    // path, not a failure. Never throws / never blocks the edit (KT-DEC-0007).
    try {
      const fabricDir = join(cwd, FABRIC_DIR_REL);
      if (existsSync(fabricDir)) {
        const failEvent = {
          kind: "fabric-event",
          id: `event:${randomUUID()}`,
          ts: Date.now(),
          schema_version: 1,
          event_type: "narrow_hint_failed",
          hook_name: "knowledge-hint-narrow",
          reason: String(lastFailure.reason).slice(0, 240),
        };
        appendLockedLine(join(fabricDir, EVENTS_LEDGER_FILE), JSON.stringify(failEvent) + "\n");
      }
    } catch {
      // best-effort telemetry — never block the edit
    }
  }
  return null;
}

// -----------------------------------------------------------------------------
// v2.0.0-rc.37 NEW-17 — plan-context-hint result cache (per-session).
//
// Each PreToolUse fire is a separate process, so "in-memory" caching means a
// per-session sidecar of the CLI result keyed on the edited path-set. A repeat
// edit to the same file(s) — common during iterative work on one module —
// re-reads the cached result instead of paying another `fabric plan-context-
// hint` cold-start spawn (~50-150ms). MultiEdit's N paths already collapse to
// ONE spawn (extractPaths dedupes + invokePlanContextHint joins --paths); this
// cache extends that win across fires within a stable knowledge graph.
//
// Freshness: the cache is invalidated wholesale when `.fabric/agents.meta.json`
// mtime changes (the derived index the server rewrites on any knowledge edit).
// This is a cheap stat — no spawn — so the freshness check itself is ~free.
// -----------------------------------------------------------------------------

// Bound the per-session result map so a long stable session editing many
// distinct files can't grow the sidecar without limit.
const NARROW_RESULT_CACHE_MAX_ENTRIES = 50;

function metaFreshnessToken(cwd) {
  try {
    const metaPath = join(cwd, FABRIC_DIR_REL, AGENTS_META_FILE);
    if (!existsSync(metaPath)) return null;
    return statSync(metaPath).mtimeMs;
  } catch {
    return null;
  }
}

function narrowResultCacheFileName(sessionId) {
  const safe = String(sessionId || "anonymous").replace(/[^A-Za-z0-9_.-]/g, "-");
  return `narrow-result-cache-${safe}.json`;
}

// Order-independent key for a path-set (sorted + NUL-joined so [a,b] and [b,a]
// hit the same cache slot).
function pathSetKey(paths) {
  return [...paths].sort().join("\u0000");
}

// Returns the cached cliPayload for `paths` iff the cache's meta token matches
// the current knowledge-graph freshness, else null (caller spawns the CLI).
async function readNarrowResultCache(cwd, sessionId, paths, metaToken) {
  if (metaToken === null) return null;
  const cache = await readJsonStateAsync(
    cwd,
    narrowResultCacheFileName(sessionId),
    (parsed) => parsed && typeof parsed === "object" && parsed.results && typeof parsed.results === "object",
  );
  if (!cache || cache.meta_token !== metaToken) return null;
  const hit = cache.results[pathSetKey(paths)];
  return hit && typeof hit === "object" ? hit : null;
}

// Persist `cliPayload` under the path-set key. Resets the map when the meta
// token changed (stale graph) and caps the map size (FIFO-ish: drop oldest
// insertion-order keys). Best-effort — never throws.
async function writeNarrowResultCache(cwd, sessionId, paths, metaToken, cliPayload) {
  if (metaToken === null) return;
  const fileName = narrowResultCacheFileName(sessionId);
  const prior = await readJsonStateAsync(
    cwd,
    fileName,
    (parsed) => parsed && typeof parsed === "object" && parsed.results && typeof parsed.results === "object",
  );
  const results =
    prior && prior.meta_token === metaToken && prior.results ? { ...prior.results } : {};
  results[pathSetKey(paths)] = cliPayload;
  const keys = Object.keys(results);
  if (keys.length > NARROW_RESULT_CACHE_MAX_ENTRIES) {
    for (const stale of keys.slice(0, keys.length - NARROW_RESULT_CACHE_MAX_ENTRIES)) {
      delete results[stale];
    }
  }
  await writeJsonStateAsync(cwd, fileName, { meta_token: metaToken, results });
}

// -----------------------------------------------------------------------------
// v2.0.0-rc.33 W2 — fabric-config readers + per-file dedup-window sidecar.
//
// All readers follow the project convention: inline JSON.parse of
// .fabric/fabric-config.json with default-on-failure. Hooks cannot require()
// the TS schema, so the schema's range constraints are duplicated inline as
// guard clauses (kept in sync with packages/shared/src/schemas/fabric-config.ts).
// -----------------------------------------------------------------------------

function _readNarrowConfigValue(projectRoot) {
  return hintNarrowConfig._readNarrowConfigValue(projectRoot);
}

function readNarrowTopK(projectRoot) {
  return hintNarrowConfig.readNarrowTopK(projectRoot);
}

function readNarrowDedupWindowTurns(projectRoot) {
  return hintNarrowConfig.readNarrowDedupWindowTurns(projectRoot);
}

function readNarrowCooldownHours(projectRoot) {
  return hintNarrowConfig.readNarrowCooldownHours(projectRoot);
}

// TASK-005 (grill G5 / C-004 "全 nudge MUST 可 dismiss"): the narrow per-edit
// hint is now individually silenceable via the unified
// `hint_dismiss_signals` enum — listing "narrow" suppresses this PreToolUse
// nudge across ALL sessions, exactly as an "archive"/"review" entry silences
// the Stop / SessionStart surfaces. Default OFF (empty list) keeps narrow ON
// (impact-bearing, C-004). Any read/parse failure → not dismissed (never-block).
function readNarrowDismissed(projectRoot) {
  return hintNarrowConfig.readNarrowDismissed(projectRoot);
}

function readReminderToContext(projectRoot) {
  return hintNarrowConfig.readReminderToContext(projectRoot);
}

function readNarrowLastEmit(projectRoot) {
  const p = join(projectRoot, HINT_NARROW_LAST_EMIT_FILE);
  if (!existsSync(p)) return null;
  try {
    const raw = readFileSync(p, "utf8").trim();
    if (raw.length === 0) return null;
    const asNum = Number(raw);
    if (Number.isFinite(asNum) && asNum > 0) return asNum;
    const ms = Date.parse(raw);
    if (Number.isFinite(ms)) return ms;
  } catch {
    // ignore
  }
  return null;
}

function writeNarrowLastEmit(projectRoot, nowMs) {
  const p = join(projectRoot, HINT_NARROW_LAST_EMIT_FILE);
  try {
    if (!existsSync(dirname(p))) {
      mkdirSync(dirname(p), { recursive: true });
    }
    writeFileSync(p, String(nowMs));
  } catch {
    // Silent — sidecar failure must never block edits.
  }
}

/**
 * v2.0.0-rc.33 W2-2: per-file dedup window sidecar.
 *
 * On-disk shape (in .fabric/.cache/narrow-dedup-window.json):
 *   {
 *     "counter": <monotonic int — incremented on each render>,
 *     "recent": [
 *       { "path": "<file_path>", "entry_id": "<stable_id>", "at_turn": <int> },
 *       ...
 *     ]
 *   }
 *
 * The `recent` array is a ring buffer capped at NARROW_DEDUP_RING_CAP entries
 * so the sidecar stays bounded on long-running workspaces. Pruning happens
 * lazily on write.
 *
 * Read failures and shape mismatches both return a fresh zero-state — the
 * window degrades to "no dedup" rather than blocking the hint.
 */
function narrowDedupWindowPath(projectRoot, sessionId) {
  // Same sanitization as sessionHintsCachePath (ISS-20260711-259): untrusted
  // session ids must not introduce path separators into .fabric/.cache/.
  const safe =
    String(sessionId || "anonymous")
      .replace(/[^A-Za-z0-9_.-]/g, "-")
      .replace(/^[.-]+/, "")
      .slice(0, 128) || "anonymous";
  return join(
    projectRoot,
    ".fabric",
    ".cache",
    `${NARROW_DEDUP_WINDOW_FILE_PREFIX}${safe}${NARROW_DEDUP_WINDOW_FILE_SUFFIX}`,
  );
}

function readNarrowDedupWindow(projectRoot, sessionId) {
  const empty = { revision_hash: "", counter: 0, recent: [] };
  const p = narrowDedupWindowPath(projectRoot, sessionId);
  if (!existsSync(p)) return empty;
  try {
    const raw = readFileSync(p, "utf8");
    if (raw.length === 0) return empty;
    const parsed = JSON.parse(raw);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return empty;
    }
    const counter =
      typeof parsed.counter === "number" && Number.isFinite(parsed.counter)
        ? parsed.counter
        : 0;
    const revision_hash =
      typeof parsed.revision_hash === "string" ? parsed.revision_hash : "";
    const recent = Array.isArray(parsed.recent)
      ? parsed.recent.filter(
          (r) =>
            r &&
            typeof r === "object" &&
            typeof r.path === "string" &&
            r.path.length > 0 &&
            typeof r.entry_id === "string" &&
            r.entry_id.length > 0 &&
            typeof r.at_turn === "number" &&
            Number.isFinite(r.at_turn),
        )
      : [];
    return { revision_hash, counter, recent };
  } catch {
    return empty;
  }
}

function writeNarrowDedupWindow(projectRoot, sessionId, state) {
  const p = narrowDedupWindowPath(projectRoot, sessionId);
  try {
    if (!existsSync(dirname(p))) {
      mkdirSync(dirname(p), { recursive: true });
    }
    // Lazy prune: keep only the most recent NARROW_DEDUP_RING_CAP records.
    // Newer records are at the tail; slicing from -CAP preserves ring semantics.
    const recent =
      state.recent.length > NARROW_DEDUP_RING_CAP
        ? state.recent.slice(-NARROW_DEDUP_RING_CAP)
        : state.recent;
    const tmp = `${p}.tmp-${process.pid}`;
    writeFileSync(
      tmp,
      JSON.stringify({
        revision_hash: state.revision_hash || "",
        counter: state.counter,
        recent,
      }),
    );
    renameSync(tmp, p);
  } catch {
    // Silent — sidecar failure must never block edits.
  }
}

/**
 * Apply the dedup-window filter. Returns `{ filtered, nextState }`:
 *   filtered: NarrowEntry[] — entries whose (path, id) is NOT within `window`
 *             turns of a prior emission for any of `targetPaths`.
 *   nextState: the merged window state to persist if the caller decides to
 *             render (records appended with at_turn = state.counter + 1).
 *
 * Decision rule: an entry is filtered out if ALL of its candidate
 * (path, entry_id) pairs already appear in `state.recent` with
 * `state.counter - at_turn < window`. The entry's "candidate pairs" are
 * (path, entry.id) for every path in targetPaths (the entry was about to be
 * surfaced for those paths). One path still missing → keep the entry.
 *
 * Side-effect-free; caller persists nextState only after a successful render.
 */
function applyNarrowDedupWindow(state, narrow, targetPaths, windowTurns, currentRevisionHash) {
  const revHash =
    typeof currentRevisionHash === "string" ? currentRevisionHash : "";
  // Wholesale drop on revision flip — mirrors E3 emit-gate semantics so the
  // two layers stay coherent. Without this coordination a revision-graph
  // change would re-emit at the E3 layer but the dedup-window layer would
  // still suppress the hint.
  const liveState =
    state && state.revision_hash === revHash && revHash.length > 0
      ? state
      : { revision_hash: revHash, counter: state ? state.counter : 0, recent: [] };

  if (!Array.isArray(narrow) || narrow.length === 0) {
    return { filtered: [], nextState: liveState };
  }
  if (!Array.isArray(targetPaths) || targetPaths.length === 0) {
    return { filtered: narrow.slice(), nextState: liveState };
  }

  const currentTurn = liveState.counter + 1;
  const cutoff = currentTurn - windowTurns;

  // Build a (path, entry_id) → at_turn lookup. Most recent wins on duplicates.
  const lookup = new Map();
  for (const rec of liveState.recent) {
    const key = `${rec.path}\u0000${rec.entry_id}`;
    const existing = lookup.get(key);
    if (existing === undefined || rec.at_turn > existing) {
      lookup.set(key, rec.at_turn);
    }
  }

  const filtered = [];
  const newRecords = [];
  for (const entry of narrow) {
    const entryId = entry && typeof entry.id === "string" ? entry.id : null;
    if (entryId === null) {
      // No id — can't dedup, surface defensively.
      filtered.push(entry);
      continue;
    }
    // Entry is suppressed only if every targetPath has a recent record.
    let allRecent = true;
    for (const path of targetPaths) {
      const key = `${path}\u0000${entryId}`;
      const lastTurn = lookup.get(key);
      if (lastTurn === undefined || lastTurn < cutoff) {
        allRecent = false;
        break;
      }
    }
    if (!allRecent) {
      filtered.push(entry);
      for (const path of targetPaths) {
        newRecords.push({ path, entry_id: entryId, at_turn: currentTurn });
      }
    }
  }

  const nextState = {
    revision_hash: revHash,
    counter: currentTurn,
    recent: filtered.length > 0 ? liveState.recent.concat(newRecords) : liveState.recent,
  };
  return { filtered, nextState };
}

// -----------------------------------------------------------------------------
// Rendering
// -----------------------------------------------------------------------------

// v2.0.0-rc.33 W4-A3: maxLen sourced from fabric-config#hint_summary_max_len.
function truncateSummary(raw, maxLen) {
  return hintSummaryFormat.truncateSummary(raw, maxLen);
}

function formatEntryLine(entry, maxLen) {
  return hintSummaryFormat.formatEntryLine(entry, maxLen);
}

function readSummaryMaxLen(projectRoot) {
  return hintNarrowConfig.readSummaryMaxLen(projectRoot);
}

/**
 * Render the narrow-match block to an array of stderr lines. Returns []
 * when there is nothing to render (empty entries set). Callers stay silent
 * on empty output.
 *
 * Protocol gate (rc.18): only `payload.version === 2` payloads are
 * rendered. Anything else returns []. When the payload exists but carries
 * a mismatched (non-undefined) version, a one-line stderr breadcrumb is
 * emitted as a debug aid — see `_protocol-v2-decisions.md` (Decision 2,
 * "silent-skip + one-line stderr breadcrumb"). The wire field is
 * `payload.entries` (renamed from `payload.narrow` in protocol v2,
 * Decision 1).
 *
 * Output shape:
 *   [fabric] N narrow-scoped knowledge entries match your edit targets:
 *     [<id>] (<type>/<maturity>) <summary>
 *     ...
 *   (如需重读 broad 决策，跑 fabric plan-context-hint --all)
 */
function renderSummary(payload, maxLen) {
  if (!payload || payload.version !== 2) {
    if (payload && payload.version !== undefined) {
      // breadcrumb only if payload exists but version mismatches (avoid
      // spam on null). Best-effort write — silent-on-failure honors the
      // hook's "never block edits" contract.
      try {
        process.stderr.write(
          `[fabric] hint payload version=${payload.version} unsupported (expected 2), skipping\n`,
        );
      } catch {
        // ignore — stderr unavailable, silent-skip still applies
      }
    }
    return [];
  }
  const entries = Array.isArray(payload.entries) ? payload.entries : [];
  if (entries.length === 0) return [];

  const lines = [
    `[fabric] ${entries.length} narrow-scoped knowledge entries match your edit targets:`,
  ];
  for (const entry of entries) {
    lines.push(formatEntryLine(entry, maxLen));
  }
  lines.push("  (如需重读 broad 决策，跑 fabric plan-context-hint --all)");
  return lines;
}

// -----------------------------------------------------------------------------
// Main — invoked as a CLI (require.main === module) and in-process by tests
// -----------------------------------------------------------------------------

async function main(env, stdio) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const now = (env && env.now) || new Date();
    const nowMs = now instanceof Date ? now.getTime() : Number(now);
    const err = (stdio && stdio.stderr) || process.stderr;
    const out = (stdio && stdio.stdout) || process.stdout;

    // Parse hook payload. Test seam: env.payload short-circuits stdin so
    // unit tests don't need to muck with process.stdin.
    const payload =
      env && env.payload !== undefined ? env.payload : readPayload(env && env.stdin);

    // E4 runs UNCONDITIONALLY — append a line even when payload is null or
    // the tool is unrecognized. The counter signal measures hook fires, not
    // successful renders (TASK-022 wants the raw edit-attempt cadence).
    //
    // rc.7 T4: best-effort path extraction is done BEFORE the counter write
    // so the JSON line can carry the touched paths for the Stop hook's
    // 人-first activity-overview banner. Failure to extract paths (null
    // payload, unrecognized tool, etc.) yields an empty paths array — the
    // fire-count signal is preserved.
    //
    // Test seam: env.skipCounter disables the side-effect for tests that
    // want to assert rendering behaviour without touching the filesystem.
    let toolName = null;
    let toolInput = null;
    let paths = [];
    if (payload !== null && payload !== undefined) {
      try {
        toolName = extractToolName(payload);
        if (toolName && EDIT_TOOL_NAMES.has(toolName)) {
          toolInput = extractToolInput(payload);
          paths = extractPaths(toolInput);
        }
      } catch {
        // Defensive — extractors already swallow most failures, but the
        // counter write must not be lost if a future extractor throws.
        toolName = null;
        paths = [];
      }
    }
    if (!(env && env.skipCounter === true)) {
      appendEditCounter(cwd, now, paths);
      // rc.35 TASK-07 (P0-2): mirror the edit-counter sidecar into the
      // events.jsonl ledger so doctor cite-coverage's editsTouched metric
      // sees actual edit signals. Best-effort — failure is swallowed inside
      // appendEditIntentToLedger and does not block the hook.
      // rc.38 UX-8 (C): pass the REAL payload session_id (not resolveSessionId,
      // which would substitute a synthetic per-process id that matches no
      // assistant_turn and would inflate expected_but_missed with false
      // positives under --client=all). null when the client omits session_id.
      const payloadSessionId =
        payload && typeof payload === "object" && typeof payload.session_id === "string"
          ? payload.session_id
          : null;
      appendEditIntentToLedger(cwd, now, paths, toolName, payloadSessionId);
      // Keep MCP fab_recall session_id fallback fresh (same sidecar as SessionStart).
      if (typeof payloadSessionId === "string" && payloadSessionId.length > 0) {
        try {
          const { writeActiveSession } = require("./state-store.cjs");
          const tsMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
          writeActiveSession(cwd, payloadSessionId, tsMs);
        } catch {
          // best-effort
        }
      }
    }

    // E2 path is conditional on a recognized tool + extractable paths.
    if (payload === null || payload === undefined) return;
    if (!toolName || !EDIT_TOOL_NAMES.has(toolName)) return;
    if (paths.length === 0) return;

    // TASK-005 (grill G5 / C-004): durable per-signal opt-out. When "narrow" is
    // listed in hint_dismiss_signals (global policy layer — config-single-home
    // W3; the repo fabric-config.json is inert for it), this per-edit hint is
    // silenced across all sessions — treated exactly like a cooldown hit
    // (silent return AFTER the unconditional edit-counter side-effect above, so
    // telemetry still measures the edit cadence). Test seam env.skipDismiss.
    if (!(env && env.skipDismiss === true) && readNarrowDismissed(cwd)) return;

    // v2.0.0-rc.33 W2-5 (P1-8): cooldown gate. When configured > 0, suppress
    // the hint for that many hours after a successful emit. Counted as
    // silence so doctor lint #26 sees the suppression. Test seam
    // env.skipCooldown bypasses for unit tests.
    const cooldownHours = readNarrowCooldownHours(cwd);
    if (cooldownHours > 0 && !(env && env.skipCooldown === true)) {
      const lastEmitMs = readNarrowLastEmit(cwd);
      if (
        typeof lastEmitMs === "number" &&
        nowMs - lastEmitMs < cooldownHours * MS_PER_HOUR
      ) {
        if (!(env && env.skipSilenceCounter === true)) {
          appendHintSilenceCounter(cwd, now);
        }
        return;
      }
    }

    // Resolve session id up-front (needed for the rc.37 NEW-17 result cache
    // key, and reused by the E3 emit-gate below).
    const sessionId = resolveSessionId(payload, env);

    // Stamp active-session sidecar with the REAL client session_id (never the
    // synthetic per-process fallback) so fab_recall can attach session_id when
    // the agent omits it.
    try {
      const realSid =
        payload && typeof payload === "object" && typeof payload.session_id === "string"
          ? payload.session_id
          : null;
      if (typeof realSid === "string" && realSid.length > 0) {
        const { writeActiveSession } = require("./state-store.cjs");
        writeActiveSession(cwd, realSid, nowMs);
      }
    } catch {
      // best-effort — never block PreToolUse
    }

    // Test seam: env.cliResult short-circuits the CLI spawn so unit tests
    // can feed canned plan-context-hint JSON without a built CLI binary.
    //
    // rc.37 NEW-17: when not in test-seam mode, first consult the per-session
    // result cache keyed on the edited path-set. A hit (same paths, unchanged
    // knowledge-graph mtime) skips the redundant `fabric plan-context-hint`
    // cold-start spawn. Misses spawn the CLI then populate the cache. The
    // env.skipResultCache seam disables both read+write for tests asserting
    // raw spawn behaviour.
    let cliPayload;
    if (env && env.cliResult !== undefined) {
      cliPayload = env.cliResult;
    } else {
      const useResultCache = !(env && env.skipResultCache === true);
      const metaToken = useResultCache ? metaFreshnessToken(cwd) : null;
      const cached = useResultCache
        ? await readNarrowResultCache(cwd, sessionId, paths, metaToken)
        : null;
      if (cached !== null) {
        cliPayload = cached;
      } else {
        cliPayload = invokePlanContextHint(cwd, paths);
        if (useResultCache && cliPayload !== null && cliPayload !== undefined) {
          await writeNarrowResultCache(cwd, sessionId, paths, metaToken, cliPayload);
        }
      }
    }
    if (cliPayload === null || cliPayload === undefined) return;

    // Protocol v2 (rc.18 TASK-005): wire field is `entries`, no v1 shim.
    //
    // v2.0.0-rc.27 TASK-005 (audit §2.5/§2.7): filter to entries whose
    // `relevance_scope === "narrow"` so broad cross-cutting entries do NOT
    // pollute the PreToolUse banner. rc.26 emitted broad + narrow as a
    // single list — every Edit fired a hint even for paths the entry never
    // anchored against (audit §2.5 reproduction). Broad entries are already
    // surfaced once per session by the SessionStart hook so the PreToolUse
    // surface should be narrow-only by design.
    //
    // Defensive default: when the CLI omits `relevance_scope` (older server
    // / malformed item) we treat it as broad and skip — pre-rc.27 entries
    // without the field are exactly the broad-leak surface §2.5 calls out.
    const allEntries = Array.isArray(cliPayload.entries) ? cliPayload.entries : [];
    const narrowFiltered = allEntries.filter((entry) => entry && entry.relevance_scope === "narrow");

    // v2.0.0-rc.33 W2-1 (P0-9): apply TopK slice to narrow set BEFORE the
    // emit-gate / dedup-window cascade. The server-side ranking already
    // produced a sensible order, so slicing here bounds the per-Edit hint
    // surface area to `hint_narrow_top_k` (default 5) so the agent's working
    // memory isn't displaced by an unwieldy banner.
    const topK = readNarrowTopK(cwd);
    const narrow = narrowFiltered.length > topK
      ? narrowFiltered.slice(0, topK)
      : narrowFiltered;

    if (narrow.length === 0) {
      // rc.6 TASK-023 (E6): silence-counter — matched-narrow == 0. The CLI
      // had a chance to match against the extracted paths but came back
      // empty. Test seam env.skipSilenceCounter mirrors env.skipCounter.
      if (!(env && env.skipSilenceCounter === true)) {
        appendHintSilenceCounter(cwd, now);
      }
      return;
    }

    // -------------------------------------------------------------------------
    // E3 emit-gate (TASK-021) — session-hints cache.
    //
    // Sits between the CLI result and the renderSummary() call. The gate
    // decides whether to emit at all (silence on duplicate) and may also
    // narrow the entries we render (skip individual stable_ids that we've
    // already shown earlier in the same session).
    //
    // NOTE for TASK-023 (E6 silence-counter): the "skip emit" branch is
    // the natural anchor for the matched-narrow == 0 silence counter — by
    // the time we reach this comment the CLI has returned a non-empty
    // narrow set, so an "all-skipped" gate decision is equivalent to a
    // matched-narrow == 0 outcome from the user's perspective. TASK-023
    // can add the counter increment either here (before the early return)
    // or inside applyEmitGate when render === false.
    // -------------------------------------------------------------------------
    // sessionId already resolved up-front (rc.37 NEW-17) for the result cache.
    const currentRevisionHash =
      typeof cliPayload.revision_hash === "string" ? cliPayload.revision_hash : "";
    // Test seam: env.cacheSeed short-circuits the on-disk cache read so unit
    // tests can preload a known cache state without touching the filesystem.
    const cache =
      env && env.cacheSeed !== undefined
        ? env.cacheSeed
        : readSessionHintsCache(cwd, sessionId);
    const gateDecision = applyEmitGate(cache, narrow, paths, currentRevisionHash);
    if (!gateDecision.render) {
      // rc.6 TASK-023 (E6): silence-counter — emit-gate filtered everything
      // out. From the user's perspective this is indistinguishable from
      // matched-narrow == 0: the CLI had matches, but session-hints dedupe
      // suppressed the render. Counted as silence so doctor lint #26 sees
      // narrow-scope drift even when dedupe is masking the matches.
      if (!(env && env.skipSilenceCounter === true)) {
        appendHintSilenceCounter(cwd, now);
      }
      return;
    }

    // v2.0.0-rc.33 W2-2 (P0-9): per-file dedup window. The E3 session-hints
    // cache covers per-session dedupe; this layer adds workspace-level "same
    // (file, entry) not within last N turns" suppression so a hot file's
    // identical hints don't train the agent to ignore them. Counted as
    // silence on full filter-out so doctor lint #26 visibility is preserved.
    const windowTurns = readNarrowDedupWindowTurns(cwd);
    const windowState =
      env && env.dedupWindowSeed !== undefined
        ? env.dedupWindowSeed
        : readNarrowDedupWindow(cwd, sessionId);
    const dedupDecision = applyNarrowDedupWindow(
      windowState,
      gateDecision.narrow,
      paths,
      windowTurns,
      currentRevisionHash,
    );
    if (dedupDecision.filtered.length === 0) {
      // v2.0.0-rc.33 W4 review-fix (gemini Critical-1): persist the counter
      // BEFORE returning so the turn-window check still advances on suppressed
      // fires. Skipping the write here caused dedup state to permanently stick
      // — every subsequent fire would read the old counter, see at_turn within
      // the window, and keep suppressing. Now: counter ticks on every fire,
      // window-naturally expires after `windowTurns` PreToolUse events.
      if (!(env && env.skipCacheWrite === true)) {
        writeNarrowDedupWindow(cwd, sessionId, dedupDecision.nextState);
      }
      if (!(env && env.skipSilenceCounter === true)) {
        appendHintSilenceCounter(cwd, now);
      }
      return;
    }

    // Persist the cache BEFORE rendering. If the render itself throws (e.g.
    // stderr write errors), the cache update still reflects the intent —
    // the alternative (post-render write) could leave us in a state where
    // the user saw the hint but the cache says "not yet shown", causing a
    // double-emit on the next fire. We prefer the silent-but-recorded
    // outcome to the double-emit one.
    //
    // Test seam: env.skipCacheWrite disables the on-disk write so tests
    // can assert the gate decision without filesystem side effects.
    if (!(env && env.skipCacheWrite === true)) {
      writeSessionHintsCache(cwd, {
        ...gateDecision.cache,
        session_id: sessionId,
      });
      writeNarrowDedupWindow(cwd, sessionId, dedupDecision.nextState);
    }

    const summaryMaxLen = readSummaryMaxLen(cwd);
    // KT-GLD-0006: the rc.35 opaque-summary runtime substitution is retired — the
    // write-time mechanical floor in extractKnowledge prevents degenerate summaries
    // at the source, so the narrow hook renders the description summary as-is.
    const lines = renderSummary({ ...cliPayload, entries: dedupDecision.filtered }, summaryMaxLen);
    if (lines.length === 0) return;

    // v2.1.0-rc.1 P4 (F4/S63): store-aware hint — append the write-target store
    // so the edit-time hint says WHERE a derived knowledge entry would land.
    // Best-effort; missing snapshot / single-store setup omits the line.
    if (bindingsSnapshotReader !== null) {
      try {
        const projectId = readProjectId(cwd);
        if (projectId) {
          const snapshot = bindingsSnapshotReader.readBindingsSnapshot(projectId);
          const writeAlias =
            snapshot && snapshot.write_target && snapshot.write_target.alias;
          if (writeAlias) {
            lines.push(`[fabric] writes here land in store '${writeAlias}'`);
          }
        }
      } catch {
        // store label is decorative provenance — never crash the hook
      }
    }

    // v2.2 dual-sink (Goal A / C5): a narrow HIT emits BOTH channels. The human
    // systemMessage is gated by nudge_mode (a MISS already returned silently far
    // above — narrow.length===0 / gate-skip / dedup-filter); the AI
    // additionalContext is emitted regardless (gated only by reminder_to_context),
    // preserving flow ⊥ observation (D5). emitDualSink shapes the protocol per
    // client (CC/Codex camelCase nested; unknown → stderr).
    const text = lines.join("\n");
    const humanGate =
      nudgePolicy !== null
        ? nudgePolicy.resolveHumanSink(cwd, "pre_tool_use", { hit: true })
        : { emitHuman: true };
    const human = humanGate.emitHuman ? text : null;
    const ai = readReminderToContext(cwd) ? text : null;
    if (!(env && env.skipStdout === true)) {
      emitDualSink(
        { human, ai },
        { client: detectClient(), eventName: "PreToolUse", streams: { stdout: out, stderr: err } },
      );
    } else if (human !== null) {
      // skipStdout test seam: still surface the human breadcrumb to stderr.
      err.write(`${text}\n`);
    }

    // lifecycle-refactor W1-T2: hook_surface_emitted — record WHICH narrow-scoped
    // stable_ids this PreToolUse fire surfaced into the edit, so doctor can join
    // surfaced→edited (this is the join's LEFT half; the edit_intent_checked event
    // appended above supplies the edited path / RIGHT half, keyed on the same
    // real payload session_id). Fires only after all gates passed and lines were
    // rendered (so it tracks genuinely-surfaced hints, never bloat). Best-effort,
    // never blocks the edit (KT-DEC-0007); the schema's `client` is a required
    // enum, so skip when the client is undetectable rather than emit an invalid
    // row. Mirrors the broad SessionStart emit (knowledge-hint-broad.cjs).
    try {
      const surfaceClient = detectClient();
      const fabricDir = join(cwd, FABRIC_DIR_REL);
      if (surfaceClient !== undefined && existsSync(fabricDir)) {
        const renderedIds = dedupDecision.filtered
          .map((e) => (e && typeof e.id === "string" ? e.id : null))
          .filter((x) => x !== null);
        const realSessionId =
          payload &&
          typeof payload === "object" &&
          typeof payload.session_id === "string" &&
          payload.session_id.length > 0
            ? payload.session_id
            : null;
        const surfaceEvent = {
          kind: "fabric-event",
          id: `event:${randomUUID()}`,
          ts: nowMs,
          schema_version: 1,
          ...(realSessionId ? { session_id: realSessionId } : {}),
          event_type: "hook_surface_emitted",
          hook_name: "knowledge-hint-narrow",
          client: surfaceClient,
          target_channel: "stderr",
          rendered_ids: renderedIds,
          delivery_status: "delivered",
        };
        appendLockedLine(join(fabricDir, EVENTS_LEDGER_FILE), JSON.stringify(surfaceEvent) + "\n");
      }
    } catch {
      // best-effort telemetry — never block the edit
    }

    // v2.2 dual-sink (Goal A): the legacy rc.33 W2-6 CC-only stdout envelope is
    // replaced by emitDualSink above (which carries BOTH the human systemMessage
    // and the AI additionalContext, shaped per client). reminder_to_context still
    // gates whether the AI sink is populated (see `ai` above).

    // v2.0.0-rc.33 W2-5: record successful emit for cooldown gate.
    if (cooldownHours > 0 && !(env && env.skipCooldownWrite === true)) {
      writeNarrowLastEmit(cwd, nowMs);
    }
  } catch {
    // Silent — never block edits on hook failure.
  }
}

module.exports = {
  main,
  readPayload,
  extractToolName,
  extractToolInput,
  extractPaths,
  appendEditCounter,
  appendHintSilenceCounter,
  toProjectRelativePaths,
  invokePlanContextHint,
  renderSummary,
  truncateSummary,
  formatEntryLine,
  // rc.35 TASK-07 (P0-2): cite-infrastructure wire-up. Exported so the
  // integration test can drive the writer directly without standing up the
  // entire PreToolUse main() flow.
  appendEditIntentToLedger,
  // rc.6 TASK-021 (E3) — session-hints cache exports for tests / future
  // consumers (TASK-023 silence-counter telemetry will reuse the same
  // session-id resolution + cache shape).
  resolveSessionId,
  resetSyntheticSessionId,
  sessionHintsCachePath,
  readSessionHintsCache,
  writeSessionHintsCache,
  computeIndexHash,
  applyEmitGate,
  // v2.0.0-rc.33 W2-1 / W2-2 / W2-5 / W2-6 — exports for unit tests.
  readNarrowTopK,
  readNarrowDedupWindowTurns,
  readNarrowCooldownHours,
  // TASK-005 (grill G5 / C-004): "narrow" dismiss reader — exported for tests.
  readNarrowDismissed,
  readReminderToContext,
  readNarrowLastEmit,
  writeNarrowLastEmit,
  narrowDedupWindowPath,
  readNarrowDedupWindow,
  writeNarrowDedupWindow,
  applyNarrowDedupWindow,
  readSummaryMaxLen,
  // v2.0.0-rc.37 NEW-17 — plan-context-hint result cache exports for tests.
  metaFreshnessToken,
  narrowResultCacheFileName,
  pathSetKey,
  readNarrowResultCache,
  writeNarrowResultCache,
  CONSTANTS: {
    CLI_TIMEOUT_MS,
    SUMMARY_MAX_LEN: DEFAULT_SUMMARY_MAX_LEN,
    DEFAULT_SUMMARY_MAX_LEN,
    EDIT_COUNTER_DIR_REL,
    EDIT_COUNTER_FILE,
    HINT_SILENCE_COUNTER_DIR_REL,
    HINT_SILENCE_COUNTER_FILE,
    EVENTS_LEDGER_DIR_REL,
    EVENTS_LEDGER_FILE,
    EDIT_TOOL_NAMES,
    SESSION_HINTS_DIR_REL,
    SESSION_HINTS_FILE_PREFIX,
    SESSION_HINTS_FILE_SUFFIX,
    DEFAULT_HINT_NARROW_TOP_K,
    DEFAULT_HINT_NARROW_DEDUP_WINDOW_TURNS,
    DEFAULT_HINT_NARROW_COOLDOWN_HOURS,
    DEFAULT_HINT_REMINDER_TO_CONTEXT,
    NARROW_DEDUP_WINDOW_FILE_PREFIX,
    NARROW_DEDUP_WINDOW_FILE_SUFFIX,
    NARROW_DEDUP_RING_CAP,
    HINT_NARROW_LAST_EMIT_FILE,
  },
};

if (require.main === module) {
  // Read stdin synchronously (small hook payloads, no concurrency concerns).
  let stdinRaw = "";
  try {
    stdinRaw = require("node:fs").readFileSync(0, "utf8");
  } catch {
    // No stdin — proceed with empty payload (E4 still runs).
  }
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main(
    { cwd: context.workspaceRoot, now: new Date(), stdin: stdinRaw },
    { stderr: process.stderr },
  ).finally(() => process.exit(0));
}
