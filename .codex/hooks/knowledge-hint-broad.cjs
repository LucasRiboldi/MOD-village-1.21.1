#!/usr/bin/env node
/**
 * rc.6 TASK-019 (E1) — SessionStart broad-injection hook.
 *
 * Stateless ambient-awareness hook: on every SessionStart event, invokes
 * `fabric plan-context-hint --all` to fetch the workspace's broad-scoped
 * knowledge index, then renders a human-readable summary to stderr so the
 * Agent's session opens with passive awareness of what knowledge exists.
 *
 * No state file. No fingerprint dedup. No cooldown. SessionStart fires once
 * per session boot — the rendering cost is paid exactly that often. The
 * narrow-injection sibling (E2, knowledge-hint-narrow.cjs) handles
 * per-Edit/Write hints with a session-hints cache.
 *
 * Output contract (W2 / KT-DEC-0027/0028/0029 — the SessionStart spine):
 *
 *   AI sink (additionalContext) — the dynamically generated "MEMORY.md":
 *     [fabric:SessionStart] <store>
 *     ALWAYS-ACTIVE RULES (unconditional · act on the line): # guideline/model, BROAD only
 *       [guideline] team:KT-GLD-0001 · <summary> # INDEX line; body on demand (KT-DEC-0036)
 *     REFERENCE (situational · Read when must_read_if fires): # decision/pitfall/process, BROAD
 *       [decision] team:KT-DEC-0001 — <must_read_if>
 *       … N more folded (broad index > backstop 50; prune via fabric-audit)
 *     Load full content: fab_recall(paths), or Read <store>/knowledge/<type>/<id>--*.md
 *
 *   Human sink (systemMessage) — broad-only census breadcrumb; SessionStart is
 *   SILENT about narrow-scoped knowledge (no on-demand counts, no
 *   dropped-other-project line).
 *
 *   When 0 entries / CLI unavailable / CLI error / parse failure:
 *     (no output — silent exit 0)
 *
 * Stdout is intentionally empty: Stop hooks may pollute stdout to signal
 * `decision:block`, but SessionStart is informational, never blocking.
 *
 * Failure invariant: any error path (spawn failure, ENOENT, timeout,
 * JSON.parse throw) MUST end in silent exit 0. The hook never blocks
 * session start on its own malfunction.
 */

const { spawnSync } = require("node:child_process");
const { existsSync, readdirSync, readFileSync } = require("node:fs");
const { join } = require("node:path");

// W3-B F-005 (C-003 HUD-shared layer): the SessionStart AI sink reskin consumes
// the parity-trivial shared structural primitives — sectionBar (title row) +
// scopeBadge ([team]/[project]/[personal]) — from the .cjs theme mirror. This is
// the ONLY shared-theme dependency the hook may take: lib/theme.cjs is byte-locked
// to packages/shared/src/theme.ts, which lib/theme.cjs is compiled from. The hook MUST
// NEVER reach for the CLI-only ESM/TS structure layer (the tree / grid primitives
// under packages/cli/src/tui) — it is unrequireable from a .cjs runtime; the HUD
// deliberately uses plain two-space indent instead of the complex tree() primitive.
const { headerRule, scopeBadge } = require("./lib/theme.cjs");

// W1-01 (ISS-012): the SessionStart broad hook appends a hook_surface_emitted
// event to the shared events.jsonl. ux-w2-9: route through the single guarded
// event-writer (envelope stamp + event_type guard + advisory-lock append) so
// the row always satisfies the event-ledger schema the doctor reads.
const { appendEvent } = require("./lib/event-writer.cjs");
const eventReader = require("./lib/event-reader.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

// rc.16 TASK-003: shared banner-i18n lib (resolves fabric_language config and
// renders localized banner text). Mirror of the wiring in fabric-hint.cjs
// (TASK-002). Variant is resolved ONCE per main() invocation via
// readFabricLanguage(cwd) and threaded into renderBanner — no fs in render path.
const { renderBanner, readFabricLanguage } = require("./lib/banner-i18n.cjs");
// v2.0.0-rc.37 NEW-19: shared fabric-config reader + sidecar I/O. Replaces the
// five per-key readFileSync+parse config readers (one parse per fire now) and
// the bespoke last-emit sidecar helpers. The L78 "refactor into lib/ if a
// third hook needs it" note is now realised.
const {
  readConfigNumber,
  readConfigBoolean,
  readConfigString,
  readPolicy,
} = require("./lib/config-cache.cjs");
const {
  readTextState,
  writeTextState,
  writeActiveSession,
} = require("./lib/state-store.cjs");
// v2.0.0-rc.37 NEW-30: shared client detection (replaces the inline
// CLAUDE_PROJECT_DIR single-bit check below).
// v2.2 dual-sink (Goal A): + emitDualSink (two-channel SessionStart emit).
const { isClaudeCode, detectClient, emitDualSink } = require("./lib/client-adapter.cjs");
// v2.2 dual-sink (Goal A / D4): human-output gate (nudge_mode + observe.*). Only
// governs the human systemMessage — the AI additionalContext is emitted
// regardless (flow ⊥ observation). Optional require so an old install lacking the
// lib degrades to "always emit human" (the pre-dual-sink default).
let nudgePolicy = null;
try {
  nudgePolicy = require("./lib/nudge-policy.cjs");
} catch {
  // Lib missing (old install) — human sink always emits (legacy behavior).
}
// v2.1.0-rc.1 P4 (F4/S63): hook-side reader for the CLI pre-generated
// resolved-bindings snapshot. The hook NEVER re-resolves stores or walks store
// trees — it only echoes the read-set the CLI already computed. Best-effort.
let bindingsSnapshotReader = null;
try {
  bindingsSnapshotReader = require("./lib/bindings-snapshot-reader.cjs");
} catch {
  // Lib missing (old install) — store labels degrade to silent absence.
}
// config-layering W3 (TASK-004): SSOT store-layer config reader. Adds the STORE
// layer + env layer to the broad_index_backstop / underseed_node_threshold
// cascades (env > project > store > default). Optional require so an old install
// lacking the lib degrades to the pre-store project > default behavior.
let storeConfigReader = null;
try {
  storeConfigReader = require("./lib/store-config-reader.cjs");
} catch {
  // Lib missing (old install) — cascade degrades to project > default (no store).
}
// v2.2 HK3-telemetry (W3-T1): injection-side per-inject logger. Optional require
// so an old install lacking the lib degrades to silent absence (no telemetry,
// hook still works).
let injectionLog = null;
try {
  injectionLog = require("./lib/injection-log.cjs");
} catch {
  // Lib missing (old install) — injection telemetry degrades to silent absence.
}

// Read the workspace binding id from `.fabric/fabric-config.json` (the snapshot
// key). Defaults to project_id when workspace_binding_id is absent.
function readWorkspaceBindingId(cwd) {
  try {
    const raw = readFileSync(join(cwd, ".fabric", "fabric-config.json"), "utf8");
    const parsed = JSON.parse(raw);
    if (typeof parsed.workspace_binding_id === "string") return parsed.workspace_binding_id;
    return typeof parsed.project_id === "string" ? parsed.project_id : null;
  } catch {
    return null;
  }
}

// TASK-005 (grill G5 / C-004): read the durable `hint_dismiss_signals` opt-out
// list. The same enum silences a signal on EVERY surface — so a `"review"` /
// `"import"` / `"maintenance"` entry suppresses that segment of the SessionStart
// summary line, exactly as it suppresses the corresponding Stop nudge and the
// per-edit nudges. Returns a Set (empty on any read/parse error — never-block).
//
// config-single-home W3: `hint_dismiss_signals` is a PREFERENCE knob, so it
// resolves from the global policy layer (`projects[<project_id>]` then
// `defaults`) — the first layer that DECLARES the list wins, and an empty array
// is a meaningful "dismiss nothing". This reader used to open the repo's
// `.fabric/fabric-config.json` directly, making it the only one of the four
// dismiss readers (the others: lib/session-signal-state.cjs,
// lib/hint-narrow-config.cjs, templates/hooks/cite-policy-evict.cjs) that looked
// at a file the W3 split turned identity-only. One key, two homes: dismissing
// "review" in the repo file silenced the SessionStart segment but not the Stop
// nudge, and vice-versa in the global file. Now all four share readPolicy().
function readDismissedSummarySignals(cwd) {
  const dismissed = new Set();
  try {
    for (const layer of readPolicy(cwd)) {
      const list = layer && layer.hint_dismiss_signals;
      if (Array.isArray(list)) {
        for (const s of list) {
          if (typeof s === "string") dismissed.add(s);
        }
        break;
      }
    }
  } catch {
    // never-block
  }
  return dismissed;
}

function readSnapshotCanonicalCount(projectRoot) {
  // No reader / not bound → degrade to an empty corpus (0), the documented
  // store-only behavior (KT-DEC-0007). Only the "snapshot EXISTS but predates
  // knowledge_store_dirs" case below is undeterminable → null (skip).
  if (bindingsSnapshotReader === null) {
    return 0;
  }
  const bindingId = readWorkspaceBindingId(projectRoot);
  if (bindingId === null) {
    return 0;
  }
  try {
    const snapshot = bindingsSnapshotReader.readBindingsSnapshot(bindingId);
    // No snapshot file at all → treat as an empty corpus (KT-DEC-0007),
    // preserving the fresh-project underseed nudge.
    if (!snapshot) {
      return 0;
    }
    // LIVE recount off the snapshot's resolved store dirs. The cached
    // knowledge_stats.canonical_count is frozen at snapshot-write time and goes
    // stale when store content syncs in out-of-band (e.g. the store grew from 1
    // → 57 nodes via a `git pull`/cross-workspace sync that never regenerated
    // THIS workspace's snapshot), which mis-fired the "knowledge sparse"
    // underseed nudge (KT-PIT-0017, same stale-projection root cause).
    const live = bindingsSnapshotReader.liveKnowledgeStats(snapshot);
    // #3: a snapshot that predates knowledge_store_dirs makes liveKnowledgeStats
    // return null — the count is undeterminable and the cached projection is
    // unreliable. Return null (not 0) so countCanonicalNodes / shouldRecommendImport
    // SKIP the nudge instead of false-firing on stale data; the snapshot
    // self-heals on the next install/sync. A genuine live 0 (dirs present, no
    // *.md) still returns 0 and fires correctly.
    if (live === null) {
      return null;
    }
    return Number.isFinite(live.canonicalCount) ? Math.floor(live.canonicalCount) : 0;
  } catch {
    // Read/parse fault → degrade to empty (0), preserving prior behavior. The
    // only undeterminable→skip path is the explicit live===null above.
    return 0;
  }
}


// -----------------------------------------------------------------------------
// rc.12: SessionStart broad-menu is now unconditionally emitted on every
// SessionStart fire (matching Skill-style progressive disclosure). Prior
// versions (rc.5-rc.11) wrote `.fabric/.cache/sessionstart-last-hash` as a
// revision_hash cooldown sidecar to suppress re-emission on unchanged
// knowledge graphs; that gate was removed in rc.12. Orphaned sidecar files
// on existing dogfood repos are harmless dead state and are intentionally
// NOT cleaned up (zero-user clean-slate — no migration logic needed).
// -----------------------------------------------------------------------------

const FABRIC_DIR_REL = ".fabric";

// rc.8 underseed self-check constants (mirror fabric-hint.cjs ~line 76 / 83).
// Intentionally duplicated inline — hooks are independent .cjs files and
// cannot `require` each other. If a third hook ever needs the same logic,
// refactor into packages/cli/templates/hooks/lib/. Keep these values in sync
// with packages/cli/templates/hooks/fabric-hint.cjs.
const IMPORT_STATE_FILE = ".import-state.json";
const KNOWLEDGE_CANONICAL_TYPES = [
  "decisions",
  "pitfalls",
  "guidelines",
  "models",
  "processes",
];
const DEFAULT_UNDERSEED_NODE_THRESHOLD = 10;

// W2-1 (KT-DEC-0028): the broad index is shown in FULL (no top-K hard cap). The
// only scale guard is a backstop: when the rendered broad index exceeds this
// many lines, the overflow tail folds into one marker that doubles as a drift
// signal (the W4 doctor `broad-index-drift` lint is the authoritative detector).
// Overridable via fabric-config.json#broad_index_backstop (W4 schema range 20..500).
const DEFAULT_HINT_BROAD_INDEX_BACKSTOP = 50;

// v2.0.0-rc.33 W2-5 (P1-8): cooldown (in hours) between broad-hint re-emits.
// Default 0 preserves rc.32 behavior — every SessionStart re-fires the banner.
// Cache key uses a separate sidecar from the fabric-hint Signal A/B/C cache
// so the two cooldowns don't interfere.
const DEFAULT_HINT_BROAD_COOLDOWN_HOURS = 24; // ISS-20260713-033: quiet default; verbose preset uses 0

/**
 * config-single-home W7: the presentation numbers now come from the nudge_mode
 * preset. `nudgePolicy` is an OPTIONAL require (an old install may lack the lib),
 * so fall back to the `normal` row — which is value-identical to the retired
 * per-key defaults, keeping the degraded path on the historical behavior.
 */
// Built lazily, not as a module-level const: DEFAULT_SUMMARY_MAX_LEN is declared
// further down the file, and a `const` referencing it up here would hit the TDZ
// at module-eval time.
function fallbackNudgePreset() {
  return {
    narrowTopK: 5,
    summaryMaxLen: DEFAULT_SUMMARY_MAX_LEN,
    broadCooldownHours: DEFAULT_HINT_BROAD_COOLDOWN_HOURS,
    narrowCooldownHours: 0,
  };
}

function resolveNudgePreset(projectRoot) {
  try {
    return nudgePolicy?.resolveNudgePreset(projectRoot) ?? fallbackNudgePreset();
  } catch {
    return fallbackNudgePreset();
  }
}
const MS_PER_HOUR = 60 * 60 * 1000;
const MS_PER_DAY = 24 * MS_PER_HOUR;

// TASK-005 (grill G5): the maintenance signal (formerly fabric-hint Signal D)
// moved off Stop onto the SessionStart summary line. Mirror the fabric-hint
// thresholds so the two surfaces agree: nag when doctor/lint has not run in
// `maintenance_hint_days` (default 14) AND the corpus is non-trivial
// (canonical >= min). Cooldown lives at the Stop cadence; SessionStart fires at
// most once per boot so it needs no extra cooldown here.
const DEFAULT_MAINTENANCE_HINT_DAYS = 14;
const MAINTENANCE_HINT_MIN_CANONICAL = 5;
const EVENT_LEDGER_FILE_NAME = "events.jsonl";
const EVENT_TYPE_DOCTOR_RUN = "doctor_run";

// TASK-005: minimal, self-contained doctor_run staleness probe for the
// SessionStart maintenance summary. Mechanically scans .fabric/events.jsonl
// tail-first for the newest doctor_run ts; returns its age in days, or null
// when the ledger is missing/empty or no doctor_run has ever fired (the caller
// treats null as "never run → stale"). NEVER throws (SessionStart never-block).
function readLastDoctorRunAgeDays(cwd, nowMs) {
  // ISS-20260713-008: tail-window only — never load full events.jsonl on SessionStart.
  try {
    return eventReader.readLastDoctorRunAgeDays(cwd, nowMs);
  } catch {
    return null;
  }
}
// v2.0.0-rc.37 NEW-19: state-store resolves this basename under .fabric/.cache/.
const HINT_BROAD_LAST_EMIT_FILE_NAME = "knowledge-hint-broad-last-emit";

// v2.0.0-rc.33 W2-6 (P0-7): when true, emit banner as
// hookSpecificOutput.additionalContext JSON on stdout (Claude Code PreToolUse
// contract) so the model receives the reminder in-context. Stderr remains the
// human-facing channel for logs / breadcrumbs.
const DEFAULT_HINT_REMINDER_TO_CONTEXT = true;

// -----------------------------------------------------------------------------
// rc.8 underseed self-check helpers.
//
// These three helpers (countCanonicalNodes / readUnderseedThreshold /
// isImportTouched) are inline copies of the equivalent logic in
// packages/cli/templates/hooks/fabric-hint.cjs (~lines 218 / 749). Hooks
// cannot `require` each other (each .cjs is rendered as a standalone template
// at init time), so duplication is the documented convention. Cross-reference:
// keep both copies in sync; if a third hook needs the same logic, extract to
// packages/cli/templates/hooks/lib/.
// -----------------------------------------------------------------------------

/**
 * Count canonical knowledge entries from the CLI-generated resolved-bindings
 * snapshot. Store-only: hooks never walk project-local knowledge or store
 * trees — a missing snapshot degrades to zero (KT-DEC-0007).
 */
function countCanonicalNodes(projectRoot) {
  // #3: null = undeterminable (old snapshot lacking store dirs, or no binding
  // context). Propagate it — shouldRecommendImport SKIPS on null rather than
  // treating it as zero and false-firing the underseed nudge on a stale corpus.
  return readSnapshotCanonicalCount(projectRoot);
}

/**
 * Resolve the underseed-node threshold from .fabric/fabric-config.json
 * (underseed_node_threshold), falling back to DEFAULT_UNDERSEED_NODE_THRESHOLD.
 * Any read/parse failure → default (never block on config errors).
 */
function readUnderseedThreshold(projectRoot) {
  // config-single-home W3: CORPUS class — `env > store > default`. "How many
  // entries is too few" is a property of THIS knowledge base, so the store that
  // owns the corpus is its only home; there is no project/global layer to lose
  // to. The store layer (single-owner store-config-reader.cjs) is integer-strict
  // + positive, matching `underseed_node_threshold: z.number().int().positive()`.
  if (storeConfigReader === null) {
    return DEFAULT_UNDERSEED_NODE_THRESHOLD;
  }
  const envVal = storeConfigReader.readEnvInt("FABRIC_UNDERSEED_NODE_THRESHOLD", { min: 1 });
  if (typeof envVal === "number") {
    return envVal;
  }
  const storeRoot = storeConfigReader.resolveTeamStoreRootFromProject(projectRoot);
  const storeVal = storeConfigReader.readStoreConfigNumber(storeRoot, "underseed_node_threshold", {
    min: 1,
  });
  return typeof storeVal === "number" ? storeVal : DEFAULT_UNDERSEED_NODE_THRESHOLD;
}

/**
 * W2-1 (KT-DEC-0028): resolve broad_index_backstop from fabric-config.json. Caps
 * the rendered broad index line count; the overflow tail folds into a drift
 * marker. Validates the W4 schema's 20..500 range inline so a malformed config
 * silently falls back to the default.
 */
function readBroadIndexBackstop(projectRoot) {
  // config-single-home W3: CORPUS class — `env > store > default`. The backstop
  // scales with how many broad entries a store actually holds, so it belongs to
  // the store, not to a repo or a machine-wide preference. Range 20..500 mirrors
  // `broad_index_backstop: z.number().int().min(20).max(500)`.
  const range = { min: 20, max: 500 };
  if (storeConfigReader === null) {
    return DEFAULT_HINT_BROAD_INDEX_BACKSTOP;
  }
  const envVal = storeConfigReader.readEnvInt("FABRIC_BROAD_INDEX_BACKSTOP", range);
  if (typeof envVal === "number") {
    return envVal;
  }
  const storeRoot = storeConfigReader.resolveTeamStoreRootFromProject(projectRoot);
  const storeVal = storeConfigReader.readStoreConfigNumber(storeRoot, "broad_index_backstop", range);
  return typeof storeVal === "number" ? storeVal : DEFAULT_HINT_BROAD_INDEX_BACKSTOP;
}

/**
 * Min hours between SessionStart broad menus. config-single-home W7: derived
 * from the nudge_mode preset rather than its own `hint_broad_cooldown_hours`
 * key — "how often do I want to see the menu" is the volume dial, not a
 * separate number. 0 = no cooldown (verbose re-emits every session open).
 */
function readBroadCooldownHours(projectRoot) {
  return resolveNudgePreset(projectRoot).broadCooldownHours;
}

/**
 * Whether the banner is also written to stdout as a Claude-Code-shaped JSON
 * envelope (hookSpecificOutput.additionalContext) so the MODEL receives it.
 *
 * config-single-home W7: no longer configurable — always true. This is the AI
 * sink, and D5 (flow ⊥ observation) makes it independent of how quiet the human
 * channel is; knowledge that never reaches the model is Fabric not working, so
 * there is no user-meaningful "off". Kept as a function because telemetry
 * records the target channel per emit.
 */
function readReminderToContext(_projectRoot) {
  return DEFAULT_HINT_REMINDER_TO_CONTEXT;
}

/**
 * v2.0.0-rc.33 W2-5: read/write the broad-hint last-emit timestamp sidecar.
 * Distinct from fabric-hint's shown-cache so signal cooldowns stay isolated.
 * Returns epoch ms or null when missing/unreadable.
 */
function readBroadLastEmit(projectRoot) {
  const raw = readTextState(projectRoot, HINT_BROAD_LAST_EMIT_FILE_NAME);
  if (raw === null || raw.length === 0) return null;
  const asNum = Number(raw);
  if (Number.isFinite(asNum) && asNum > 0) return asNum;
  const ms = Date.parse(raw);
  if (Number.isFinite(ms)) return ms;
  return null;
}

function writeBroadLastEmit(projectRoot, nowMs) {
  // Silent — sidecar failure must never block session start.
  writeTextState(projectRoot, HINT_BROAD_LAST_EMIT_FILE_NAME, String(nowMs));
}

/**
 * Classify the on-disk import lifecycle by reading
 * `.fabric/.import-state.json`. Returns one of:
 *   - 'absent'      — state file missing → user has NEVER started import
 *   - 'in_progress' — file present, phase is anything that is not 'complete'
 *                     (covers 'P1-done', 'P2-done', 'phase 1', 'in_progress',
 *                     '1', and any other live-import marker)
 *   - 'complete'    — file present and phase === 'complete'
 *   - 'error'       — file present but unreadable / unparseable JSON
 *
 * Recommendation rule (see shouldRecommendImport): only 'absent' triggers a
 * banner — both 'in_progress' (user is actively importing) and 'complete'
 * (user already imported) suppress the banner. 'error' also suppresses
 * (defensive: do not nag when state is unreadable, the user has clearly
 * touched the file).
 */
function isImportTouched(projectRoot) {
  const statePath = join(projectRoot, FABRIC_DIR_REL, IMPORT_STATE_FILE);
  if (!existsSync(statePath)) return "absent";
  let raw;
  try {
    raw = readFileSync(statePath, "utf8");
  } catch {
    return "error";
  }
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return "error";
  }
  if (!parsed || typeof parsed !== "object") return "error";
  return parsed.phase === "complete" ? "complete" : "in_progress";
}

/**
 * rc.8 underseed self-check: determine whether the SessionStart hook should
 * surface the one-line `/fabric-archive` recommendation banner.
 *
 * Three-condition truth table (ALL must hold to return true):
 *   1. the workspace is fabric-bound — readWorkspaceBindingId(cwd) !== null
 *      (a resolved binding id in fabric-config.json; otherwise the
 *       recommendation is meaningless — `fabric-import` requires a bound
 *       workspace. Store-only: replaces the legacy derived-index-file
 *       existence probe.)
 *   2. countCanonicalNodes(cwd) < readUnderseedThreshold(cwd)
 *      (knowledge graph is sparse — import would meaningfully enrich it).
 *   3. isImportTouched(cwd) === 'absent'
 *      (.import-state.json is missing entirely; user has neither started
 *       nor completed an import. ANY phase value — including 'in_progress'
 *       and 'complete' — returns false because the user has either started
 *       or finished.)
 *
 * Best-effort: any unexpected error → return false (do not nag on faults).
 */
function shouldRecommendImport(projectRoot, liveTotal) {
  try {
    if (readWorkspaceBindingId(projectRoot) === null) return false;

    const threshold = readUnderseedThreshold(projectRoot);
    // P0 (Goal H3 / KT-PIT-0017 + KT-PIT-0019): prefer the LIVE census total the
    // HUD displays as the single count source. The census walks the read-set
    // fresh every fire (never a frozen snapshot projection), so feeding it here
    // makes the import nudge and the HUD agree by construction — killing the
    // "HUD shows 61 entries but the nudge claims the KB is sparse" contradiction
    // the stale-snapshot count produced. Fall back to the snapshot-derived count
    // only when no live total is supplied (e.g. direct unit-test calls).
    const nodeCount =
      typeof liveTotal === "number" && Number.isFinite(liveTotal)
        ? liveTotal
        : countCanonicalNodes(projectRoot);
    // #3: undeterminable count (old snapshot predating knowledge_store_dirs) →
    // skip. `null < threshold` coerces to true in JS, so an explicit guard is
    // required — otherwise the stale-snapshot case would still false-fire.
    if (nodeCount === null) return false;
    if (nodeCount >= threshold) return false;

    if (isImportTouched(projectRoot) !== "absent") return false;

    return true;
  } catch {
    return false;
  }
}

// -----------------------------------------------------------------------------
// CONSTANTS
// -----------------------------------------------------------------------------

// Per-type truncation triggers when total broad-scope entries > N.
// v2.0.0-rc.29 TASK-007 (BUG-F1): lowered from 30 → 12. SessionStart hint
// should bias toward "is there anything relevant?" rather than "exhaustive
// index" — at 30, the banner consumed several terminal screens on
// well-seeded repos and operators reported scroll fatigue. 12 keeps a
// dense-enough scan (still fits "top hits per type" in 1-2 screenfuls)
// without prompting the user to mentally truncate themselves. The constant
// stays a stable rendering boundary; downstream consumers (banner-i18n.cjs,
// truncation summary lines) consume it as a single source of truth.
const TRUNCATION_THRESHOLD = 12;

// Goal H4 action ladder — review rung: surface a single `/fabric-review` line when
// the LIVE pending backlog exceeds this. Mirrors fabric-hint.cjs's
// DEFAULT_REVIEW_HINT_PENDING_COUNT (the Stop hook's review threshold) so the two
// surfaces agree on "how much pending is too much". Strictly `> threshold`.
const REVIEW_PENDING_THRESHOLD = 10;

// ...and the OTHER half of that agreement (W3): the Stop hook fires on
// count-OR-age, so a count-only rung here meant 3 entries rotting for a month
// nudged at session END but stayed invisible at session START — the one moment
// the user is actually deciding what to do next. `oldestPendingMtimeMs` was
// already being computed by liveKnowledgeStats and thrown away here.
// Default mirrors hint-config's DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS; the knob
// is the same `review_hint_pending_age_days` so one config edit moves both.
const DEFAULT_REVIEW_PENDING_AGE_DAYS = 7;

// `fabric plan-context-hint` is a thin wrapper over planContext(); on a
// well-seeded repo it returns in ~100ms. Two-second cap is defensive — any
// pathological hang must not stall session start.
const CLI_TIMEOUT_MS = 8000; // ISS-20260713-028: after body-less wire, still allow cold cache

// Maximum summary length per entry. Keeps each line bounded so stderr does
// not blow up terminal width with multi-paragraph summaries from sloppy
// pending entries. Truncation appends an ellipsis. v2.0.0-rc.33 W4-A3:
// `hint_summary_max_len` in fabric-config overrides this default (range 40..240).
const DEFAULT_SUMMARY_MAX_LEN = 80;

// config-single-home W7: derived from the nudge_mode preset (verbose gets longer
// lines) instead of its own `hint_summary_max_len` key.
function readSummaryMaxLen(projectRoot) {
  return resolveNudgePreset(projectRoot).summaryMaxLen;
}

// Canonical type order — render groups in this sequence so output is stable
// across runs (Object.keys iteration order is insertion order, but the JSON
// payload may shuffle if planContext's internal sort changes). Unknown types
// are appended after canonical types in encounter order.
const CANONICAL_TYPE_ORDER = [
  "decision",
  "pitfall",
  "guideline",
  "model",
  "process",
];

// Canonical maturity order for truncation rendering. proven is the highest-
// signal tier so it gets full per-line treatment; verified gets id-list; draft
// gets count-only. Unknown maturities fall through to the verified bucket.
const MATURITY_PROVEN = "proven";
const MATURITY_VERIFIED = "verified";
const MATURITY_DRAFT = "draft";

// rc.8 underseed self-check banner: single line, emoji-prefixed (cf.
// fabric-hint.cjs Signal C `📋 Fabric:`). rc.16 TASK-003 routed the literal
// through the banner-i18n lib (key: 'broadImportBanner') — see main() below
// for the renderBanner call site. Substring contracts preserved across all
// variants: leading two-space indent, `📋 Fabric:` prefix, `/fabric-archive`
// verbatim token (asserted by knowledge-hint-broad.test.ts).

// -----------------------------------------------------------------------------
// CLI invocation
// -----------------------------------------------------------------------------

/**
 * Spawn `fabric plan-context-hint --all` and return parsed JSON. Returns
 * null on any failure (ENOENT, non-zero exit, malformed JSON). Never throws.
 *
 * If `fabric` is not on PATH, return null — the hook stays silent rather
 * than nagging about install state.
 */
function invokePlanContextHint(cwd) {
  const candidates = ["fabric"];
  // rc.31 NEW-6: capture the last meaningful failure so we can surface it on
  // stderr before fail-open. Without this, hook silently swallows backend
  // crashes (e.g. agents_meta_invalid → plan-context-hint exits with stderr
  // payload and the AI / user never sees KB chain is dead).
  let lastFailure = null;
  for (const bin of candidates) {
    let res;
    try {
      res = spawnSync(bin, ["plan-context-hint", "--all"], {
        cwd,
        encoding: "utf8",
        timeout: CLI_TIMEOUT_MS,
        stdio: ["ignore", "pipe", "pipe"],
      });
    } catch {
      continue; // spawn throw (extremely rare) — try next candidate
    }
    // ENOENT surfaces as error on the result object. Skip silently for ENOENT
    // (bin not installed is the only legitimate reason to bail).
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
    // Single warning line — never throws, never blocks the hook. Lets users /
    // AI notice that the KB chain is degraded instead of being silently empty.
    process.stderr.write(
      `[fabric-hint] plan-context-hint (${lastFailure.bin}) failed: ${lastFailure.reason.replace(/\n/g, " ")}\n`,
    );
  }
  return null;
}

// -----------------------------------------------------------------------------
// Rendering
// -----------------------------------------------------------------------------

/**
 * Group narrow entries by type (preserving canonical order), then by maturity
 * within each type. Returns { typeOrder: string[], byType: Map<type, Map<maturity, entries[]>> }.
 */
function groupEntries(narrow) {
  const byType = new Map();
  const encounterOrder = [];

  for (const entry of narrow) {
    const type = entry.type || "unknown";
    if (!byType.has(type)) {
      byType.set(type, new Map());
      encounterOrder.push(type);
    }
    const maturity = entry.maturity || "unknown";
    const maturityMap = byType.get(type);
    if (!maturityMap.has(maturity)) maturityMap.set(maturity, []);
    maturityMap.get(maturity).push(entry);
  }

  // Stable type order: canonical types first (when present), then anything
  // else in encounter order.
  const typeOrder = [];
  for (const t of CANONICAL_TYPE_ORDER) {
    if (byType.has(t)) typeOrder.push(t);
  }
  for (const t of encounterOrder) {
    if (!CANONICAL_TYPE_ORDER.includes(t)) typeOrder.push(t);
  }

  return { typeOrder, byType };
}

// v2.0.0-rc.33 W4-A3: maxLen is now caller-supplied (sourced from
// fabric-config#hint_summary_max_len in main; tests + ad-hoc callers may
// omit to fall back to DEFAULT_SUMMARY_MAX_LEN).
function truncateSummary(raw, maxLen) {
  const s = typeof raw === "string" ? raw : "";
  // Collapse newlines / runs of whitespace so each entry fits one line.
  const flat = s.replace(/\s+/g, " ").trim();
  const cap = typeof maxLen === "number" && maxLen > 0 ? maxLen : DEFAULT_SUMMARY_MAX_LEN;
  if (flat.length <= cap) return flat;
  return `${flat.slice(0, cap - 1)}…`;
}

function formatEntryLine(entry, maxLen) {
  const id = entry.id || "(no-id)";
  const summary = truncateSummary(entry.summary, maxLen);
  // lifecycle-refactor W3-T2 (§7 图谱消费 / §5 hook 沿 related 二阶召回): when this
  // entry was pulled in by following a surfaced entry's `related` graph edge,
  // tag the line with its provenance so the agent knows it arrived via the graph,
  // not its own ranking. Omitted entirely for ordinarily-ranked entries — no fake
  // "related" annotation is ever synthesized (graph-empty honesty).
  const provenance =
    typeof entry.related_to === "string" && entry.related_to.length > 0
      ? ` (related-to-${entry.related_to})`
      : "";
  return summary.length > 0
    ? `    - ${id} · ${summary}${provenance}`
    : `    - ${id}${provenance}`;
}

/**
 * Render full per-type listing — used when total narrow entries <= 30.
 * Each entry gets one line: `    - <id> · <summary>`. Type/maturity headers
 * group the listing.
 */
function renderFull(narrow, maxLen) {
  const { typeOrder, byType } = groupEntries(narrow);
  const lines = [];
  for (const type of typeOrder) {
    const maturityMap = byType.get(type);
    // Within each type, render maturity buckets in proven > verified > draft
    // > unknown order so the most-trusted entries surface first.
    const maturities = [];
    for (const m of [MATURITY_PROVEN, MATURITY_VERIFIED, MATURITY_DRAFT]) {
      if (maturityMap.has(m)) maturities.push(m);
    }
    for (const m of maturityMap.keys()) {
      if (![MATURITY_PROVEN, MATURITY_VERIFIED, MATURITY_DRAFT].includes(m)) {
        maturities.push(m);
      }
    }
    for (const maturity of maturities) {
      lines.push(`  [${type}] (${maturity}):`);
      for (const entry of maturityMap.get(maturity)) {
        lines.push(formatEntryLine(entry, maxLen));
      }
    }
  }
  return lines;
}

/**
 * Render grouped truncation — used when total narrow entries > 30. Per the
 * task spec: proven entries get full per-line treatment; verified entries get
 * an inline id list (no summary); draft (and unknown) buckets collapse to a
 * count.
 */
function renderTruncated(narrow, maxLen) {
  const { typeOrder, byType } = groupEntries(narrow);
  const lines = [];
  for (const type of typeOrder) {
    const maturityMap = byType.get(type);

    // Proven: full per-line listing.
    const proven = maturityMap.get(MATURITY_PROVEN);
    if (proven && proven.length > 0) {
      lines.push(`  [${type}] proven (${proven.length}):`);
      for (const entry of proven) {
        lines.push(formatEntryLine(entry, maxLen));
      }
    }

    // Verified: inline id list.
    const verified = maturityMap.get(MATURITY_VERIFIED);
    if (verified && verified.length > 0) {
      const ids = verified.map((e) => e.id || "(no-id)").join(", ");
      lines.push(`  [${type}] verified (${verified.length}): ${ids}`);
    }

    // Draft + any unknown maturity: count-only.
    let countOnly = 0;
    for (const [maturity, entries] of maturityMap.entries()) {
      if (maturity === MATURITY_PROVEN || maturity === MATURITY_VERIFIED) continue;
      countOnly += entries.length;
    }
    if (countOnly > 0) {
      lines.push(`  [${type}] draft: ${countOnly} entries`);
    }
  }
  return lines;
}

/**
 * Top-level rendering — picks the mode based on entry count and prepends the
 * session-start banner + appends the revision_hash and usage hint footers.
 *
 * Returns an array of lines (one stderr write per line keeps the formatter
 * trivial and testable). Returns [] when there is nothing meaningful to say
 * (empty entries set) so callers know to stay silent.
 *
 * Protocol v2 gate (rc.18): payloads must carry `version: 2`. A null/missing
 * payload returns [] silently; a payload with a mismatched `version` returns []
 * after writing exactly one stderr breadcrumb so operators grepping a stuck-
 * banner report can diagnose the version drift without source-diving.
 */
function renderSummary(payload, maxLen) {
  if (!payload || payload.version !== 2) {
    if (payload && payload.version !== undefined) {
      try {
        process.stderr.write(
          `[fabric] hint payload version=${payload.version} unsupported (expected 2), skipping\n`,
        );
      } catch (err) {
        // ISS-20260713-051: never empty-catch on user-visible injection path.
        try {
          const message = err && typeof err === "object" && "message" in err ? String(err.message) : String(err);
          process.stderr.write(`[fabric-knowledge-hint-broad] stderr write failed: ${message}\n`);
        } catch {
          /* last-resort: still must not throw upward */
        }
      }
    }
    return [];
  }
  // Protocol v2 (rc.18): the wire field is now `payload.entries`, matching what
  // this renderer always called it locally. The historical `narrow` name (which
  // degenerated in --all mode) has been retired without a compat shim per
  // pre-user clean-slate policy.
  const entries = Array.isArray(payload.entries) ? payload.entries : [];
  if (entries.length === 0) return [];

  const truncated = entries.length > TRUNCATION_THRESHOLD;
  const banner = truncated
    ? `[fabric] Session start — ${entries.length} broad-scoped knowledge entries available (truncated):`
    : `[fabric] Session start — ${entries.length} broad-scoped knowledge entries available:`;

  // KT-DEC-0028 completeness: the rendered census is bounded by the per-line
  // maxLen + TRUNCATION_THRESHOLD grouped mode, not by a body char budget.
  const body = truncated ? renderTruncated(entries, maxLen) : renderFull(entries, maxLen);

  const lines = [banner, ...body];
  const revHash = typeof payload.revision_hash === "string" ? payload.revision_hash : null;
  if (revHash !== null && revHash.length > 0) {
    lines.push(`  revision_hash: ${revHash}`);
  }

  // rc.22 Scope D T-D4 (TASK-011): meta auto-refresh breadcrumb. Emitted ONLY
  // when the server's planContext() detected meta drift and rebuilt the meta
  // in-place (auto_healed === true). One informational line — operators need
  // a paper trail when revision_hash flips mid-session.
  //
  // Variant resolution:
  //   - Both hashes present → full transition line (`sha PREV → CUR`) with
  //     8-char hex prefixes stripped of the `sha256:` scheme prefix.
  //   - auto_healed:true but previous_revision_hash missing → generic line
  //     (T10 noted the server may emit `auto_healed:true` alone if it lost
  //     the prior hash for any reason). Stays informational.
  //
  // i18n: routed through renderBanner so zh-CN / en / zh-CN-hybrid variants
  // share one call site. fabric_language is resolved via readFabricLanguage()
  // ONLY when the line is actually emitted (keeps the no-banner path free of
  // the extra config read, matching the broadImportBanner site below).
  if (payload.auto_healed === true) {
    const variant = readFabricLanguage(process.cwd());
    const prevRaw =
      typeof payload.previous_revision_hash === "string"
        ? payload.previous_revision_hash
        : null;
    const curRaw =
      typeof payload.revision_hash === "string" ? payload.revision_hash : null;
    if (prevRaw && curRaw) {
      // Strip optional `sha256:` scheme prefix, then take first 8 hex chars.
      const stripScheme = (h) =>
        h.startsWith("sha256:") ? h.slice("sha256:".length) : h;
      const prev = stripScheme(prevRaw).slice(0, 8);
      const cur = stripScheme(curRaw).slice(0, 8);
      lines.push(
        renderBanner("metaAutoRefreshedBanner", variant, { prev, cur }),
      );
    } else {
      // Defensive: auto_healed:true but no usable previous hash → generic line.
      lines.push(renderBanner("metaAutoRefreshedBannerGeneric", variant, {}));
    }
  }

  // W2-4 (KT-DEC-0026): single lean retrieval flow. The two-step
  // fab_plan_context → fab_get_knowledge_sections footer is retired — fab_recall
  // returns descriptions + read paths, and bodies load via a native Read.
  lines.push(
    "  Load full content: `fab_recall(paths)`, or Read `<store>/knowledge/<type>/<id>--*.md` directly.",
  );
  return lines;
}

// -----------------------------------------------------------------------------
// v2.2 dual-sink (Goal A): two-sink SessionStart rendering.
//
// HUMAN sink (systemMessage): a grouped census (§3 / D8) — always-loaded vs
// on-demand split + [team]/[personal] + ✗ dropped-other-project. Count-summary
// form (not a per-entry wall of text); the verbose nudge_mode appends the legacy
// per-entry renderSummary listing on top.
//
// AI sink (additionalContext): the always-active (guideline/model) BODIES (§3 /
// D9), bounded by the injection char budget with overflow degrading to summary +
// a recall pointer, followed by on-demand category counts. Replaces the legacy
// top_k=8 id-list that used to be the AI payload.
// -----------------------------------------------------------------------------

// Singular display label for a plural knowledge_type.
const TYPE_SINGULAR = {
  decisions: "decision",
  pitfalls: "pitfall",
  guidelines: "guideline",
  models: "model",
  processes: "process",
};

const ALWAYS_TYPES = ["guidelines", "models"];

// Normalize a knowledge_type to its canonical PLURAL form. Frontmatter / entries
// may carry the singular ("decision") while the census keys on the plural enum
// ("decisions"); fold both so counting + display stay consistent.
const TYPE_TO_PLURAL = {
  decision: "decisions",
  pitfall: "pitfalls",
  guideline: "guidelines",
  model: "models",
  process: "processes",
};
function toPluralType(type) {
  return TYPE_TO_PLURAL[type] || type;
}

// Fallback census when payload.census is absent (old CLI / unit-test payloads):
// count the (possibly sliced) entries by knowledge_type so the human banner still
// has something to group. Production payloads always carry the unsliced census.
function deriveCensusFromEntries(entries) {
  const census = {
    by_type: {},
    by_layer: { team: 0, personal: 0, project: 0 },
    broad_by_type: {},
    narrow_total: 0,
    dropped_other_project: 0,
    total: 0,
  };
  if (!Array.isArray(entries)) return census;
  for (const e of entries) {
    const type = e && typeof e.type === "string" ? toPluralType(e.type) : null;
    if (type === null) continue;
    const isNarrow = e.relevance_scope === "narrow";
    census.by_type[type] = (census.by_type[type] || 0) + 1;
    if (isNarrow) census.narrow_total += 1;
    else census.broad_by_type[type] = (census.broad_by_type[type] || 0) + 1;
    census.total += 1;
  }
  return census;
}

// G2/F05: the empty-KB predicate, named once. renderHumanCensus swaps in the
// first-run guidance for exactly this shape, and main() has to recognise the SAME
// shape to keep that guidance out of the emit-side bookkeeping (cooldown stamp +
// injection telemetry) — a second hand-written copy of the condition is how those
// two would drift apart.
// (dropped_other_project > 0 keeps falling through to the normal HUD: that path
// already renders "0 entries" plus the cross-project drop note.)
function isEmptyKbCensus(census) {
  const c = census || {};
  const total = typeof c.total === "number" ? c.total : 0;
  return total === 0 && (c.dropped_other_project || 0) === 0;
}

// Render the human-facing scope-primary status HUD (Goal H2). `lang` is
// "zh-CN" | other (en). Returns an array of lines (empty when census is empty).
//
// Shape (KT-DEC-0029 — SessionStart is scope-primary; broad is the spine that's
// injected this session, narrow surfaces contextually via the PreToolUse hint):
//   ▸ [fabric] 共 N 条 · 团队X · 项目Y · 个人Z
//     broad B · 本会话注入
//       ├ 常驻规则 G+M  guideline G · model M     (KT-DEC-0027 resident tier)
//       └ 情境参考 D+P+Pr  decision D · pitfall P · process Pr  (reference tier)
//     narrow M · 编辑对应文件时浮现                (合计 only, no per-type)
// Self-consistency invariant: broad (= 常驻 + 参考) + narrow == total.
function renderHumanCensus(census, opts) {
  const { lang } = opts || {};
  const c = census || {};
  const total = typeof c.total === "number" ? c.total : 0;
  const zh = lang === "zh-CN";
  // G2: an EMPTY knowledge base is exactly when a new user needs to hear
  // something. This used to `return []`, and because every downstream human
  // sink segment in buildSessionStartSinks — plus the final emit gate — keys
  // off `humanLines.length > 0`, SessionStart went completely silent: no count,
  // no next step, no way to tell "empty KB" from "hook broken". Returning two
  // lines here re-opens all of those gates without touching any of them.
  // (dropped_other_project > 0 keeps falling through to the normal HUD below —
  // that path already renders "0 entries" plus the cross-project drop note.)
  if (isEmptyKbCensus(census)) {
    return [
      zh ? "▸ [fabric] 共 0 条 —— 知识库为空" : "▸ [fabric] 0 entries — knowledge base is empty",
      zh
        ? "  下一步: /fabric-archive source 从 git/docs 回灌 · fabric store bind <alias> 绑定已有库"
        : "  Next: /fabric-archive source to backfill from git/docs · fabric store bind <alias> to bind an existing store",
    ];
  }

  const broadByType = c.broad_by_type || {};
  const narrowTotal = typeof c.narrow_total === "number" ? c.narrow_total : 0;
  // Per-tier broad counts. `broad_by_type` keys on the plural enum.
  const g = broadByType.guidelines || 0;
  const m = broadByType.models || 0;
  const d = broadByType.decisions || 0;
  const p = broadByType.pitfalls || 0;
  const pr = broadByType.processes || 0;
  const residentN = g + m; // 常驻规则 (always-active: guideline + model)
  const referenceN = d + p + pr; // 情境参考 (decision + pitfall + process)
  const broadN = residentN + referenceN;

  const layer = c.by_layer || {};
  const teamCount = layer.team || 0;
  const personalCount = layer.personal || 0;
  const projectCount = layer.project || 0;

  const lines = [];
  // Header: total entry count + semantic_scope breakdown (KT-MOD-0001 三轴).
  const scopeSegs = [zh ? `团队 ${teamCount}` : `team ${teamCount}`];
  if (projectCount > 0) scopeSegs.push(zh ? `项目 ${projectCount}` : `project ${projectCount}`);
  scopeSegs.push(zh ? `个人 ${personalCount}` : `personal ${personalCount}`);
  const totalLabel = zh ? `共 ${total} 条` : `${total} ${total === 1 ? "entry" : "entries"}`;
  lines.push(`▸ [fabric] ${totalLabel} · ${scopeSegs.join(" · ")}`);

  // broad spine — injected this session.
  lines.push(zh ? `  broad ${broadN} · 本会话注入` : `  broad ${broadN} · injected this session`);
  const residentDetail = [];
  if (g > 0) residentDetail.push(`guideline ${g}`);
  if (m > 0) residentDetail.push(`model ${m}`);
  const refDetail = [];
  if (d > 0) refDetail.push(`decision ${d}`);
  if (p > 0) refDetail.push(`pitfall ${p}`);
  if (pr > 0) refDetail.push(`process ${pr}`);
  const dash = zh ? "—" : "—";
  lines.push(
    zh
      ? `    ├ 常驻规则 ${residentN}  ${residentDetail.join(" · ") || dash}`
      : `    ├ resident ${residentN}  ${residentDetail.join(" · ") || dash}`,
  );
  lines.push(
    zh
      ? `    └ 情境参考 ${referenceN}  ${refDetail.join(" · ") || dash}`
      : `    └ reference ${referenceN}  ${refDetail.join(" · ") || dash}`,
  );

  // narrow remainder — 合计 only (no per-type; it's file-specific, surfaces on edit).
  lines.push(
    zh
      ? `  narrow ${narrowTotal} · 编辑对应文件时浮现`
      : `  narrow ${narrowTotal} · surfaces when you edit matching files`,
  );
  return lines;
}

// Goal H2: the SessionStart store label, scope-primary wording — `写入 X · 只读 Y`
// (write target receives new knowledge; the rest are read-only sources). Replaces
// the legacy `read-set stores: a (write), b (ro)` jargon line inline (kept local
// to this hook so the shared lib formatStoreLabels — used by other hooks — is
// untouched). Empty string when there is nothing to show.
function renderScopeStoreLabel(snapshot, lang) {
  if (!snapshot || !snapshot.read_set || !Array.isArray(snapshot.read_set.stores)) return "";
  const stores = snapshot.read_set.stores;
  if (stores.length === 0) return "";
  const zh = lang === "zh-CN";
  const writeAlias = snapshot.write_target && snapshot.write_target.alias;
  const writeStores = [];
  const readonlyStores = [];
  for (const s of stores) {
    const alias = s && typeof s.alias === "string" ? s.alias : null;
    if (alias === null) continue;
    if (alias === writeAlias) writeStores.push(alias);
    else readonlyStores.push(alias);
  }
  const segs = [];
  if (writeStores.length > 0) segs.push((zh ? "写入 " : "write ") + writeStores.join(", "));
  if (readonlyStores.length > 0) segs.push((zh ? "只读 " : "readonly ") + readonlyStores.join(", "));
  if (segs.length === 0) return "";
  return "  " + segs.join(" · ");
}

// W2 (KT-DEC-0027/0028/0029): render the AI-facing sink — the dynamically
// generated "MEMORY.md" spine injected into the SessionStart context. Two
// type-tiered sections over the BROAD knowledge (narrow stays silent — D0029):
//
//   ALWAYS-ACTIVE RULES (guideline/model): INDEX LINE only (title + summary) —
//     never the eager body (KT-DEC-0036). The body is one cheap on-demand fetch
//     away, so injecting it on every SessionStart is a permanent context tax
//     (KT-GLD-0005) we no longer pay; each entry stays individually visible.
//   REFERENCE (decision/pitfall/process): TITLE + must_read_if hook only
//     (situational; the agent Reads the body on demand) — never the body.
//
// `broadIndexBackstop` (D0028) caps the total rendered index lines; the overflow
// tail folds into one marker that doubles as the drift signal (fabric-audit /
// the W4 doctor lint is the authoritative detector). `entries` is the broad
// plan-context-hint entry list ({id,type,maturity,summary,relevance_scope,
// must_read_if}); `alwaysBodies` is always_bodies[] ({id,type,layer,summary,body}).
const REFERENCE_TYPES = new Set(["decision", "pitfall", "process"]);

// W3-B F-005: map a knowledge entry's layer to a scopeBadge() scope key
// (team→'team' · project→'project' · personal→'personal'). Bodies carry an
// explicit `.layer`; plan-context reference entries do not, so the layer is
// recovered from the id prefix (`team:KT-…` / `project:…` / `personal:…`), with a
// `KP-` id always personal. Anything unrecognized falls back to 'team' — the
// broadest, default tier — so the badge never renders an undefined scope.
const VALID_SCOPES = new Set(["team", "project", "personal"]);
function layerToScope(entry) {
  const e = entry || {};
  if (typeof e.layer === "string" && VALID_SCOPES.has(e.layer)) return e.layer;
  const id = typeof e.id === "string" ? e.id : "";
  const prefix = id.includes(":") ? id.slice(0, id.indexOf(":")) : "";
  if (VALID_SCOPES.has(prefix)) return prefix;
  // `KP-*` ids are the personal convention even without a `personal:` prefix.
  if (/(^|:)KP-/.test(id)) return "personal";
  return "team";
}

function renderAiSink(opts) {
  const { entries, alwaysBodies, storeLabel, broadIndexBackstop, summaryMaxLen, lang } =
    opts || {};
  const zh = lang === "zh-CN";
  const bodies = Array.isArray(alwaysBodies) ? alwaysBodies : [];
  // REFERENCE = broad decision/pitfall/process. narrow entries stay silent (D0029).
  const referenceEntries = (Array.isArray(entries) ? entries : []).filter((e) => {
    if (!e || e.relevance_scope === "narrow") return false;
    return REFERENCE_TYPES.has(TYPE_SINGULAR[toPluralType(e.type)] || e.type);
  });
  // Nothing to inject → empty so main() stays silent on an empty knowledge base.
  if (bodies.length === 0 && referenceEntries.length === 0) return "";

  const backstop =
    typeof broadIndexBackstop === "number" && broadIndexBackstop > 0 ? broadIndexBackstop : 0;
  let indexCount = 0; // total rendered index lines (always + reference), for the backstop.

  const lines = [];
  // W3-B F-005 (mockup #3): a single sectionBar title carrying the active /
  // reference counts, replacing the legacy `[fabric:SessionStart] <store>` +
  // flat `ALWAYS-ACTIVE RULES (...)` header. The store label is preserved on the
  // human sink (renderScopeStoreLabel); the AI sink leads with the count title.
  // sectionBar degrades to `# <title>` when color is off (NO_COLOR / non-TTY),
  // compiled from the TS source (packages/shared/src/theme.ts).
  lines.push(
    headerRule(
      zh
        ? `Fabric Knowledge · ${bodies.length} 常驻 · ${referenceEntries.length} 参考`
        : `Fabric Knowledge · ${bodies.length} active · ${referenceEntries.length} reference`,
    ),
  );

  // ALWAYS-ACTIVE — plain sub-section label (the `RULES (...)` qualifier is kept
  // so the contract substring + framing survive the reskin). Index-only (title +
  // summary), never the eager body.
  lines.push(
    zh
      ? "  ALWAYS-ACTIVE RULES (无条件适用 · 照此行遵循,正文按需取):"
      : "  ALWAYS-ACTIVE RULES (unconditional · act on the line; body on demand):",
  );
  if (bodies.length === 0) {
    lines.push(zh ? "  (无 always-active 条目)" : "  (none)");
  } else {
    // KT-DEC-0036: render each always-active entry as a single index line
    // (scope badge + title + summary). The body is one cheap on-demand fetch away
    // (see footer), so injecting it on every SessionStart is a permanent context
    // tax (KT-GLD-0005) we no longer pay.
    for (const b of bodies) {
      const badge = scopeBadge(layerToScope(b));
      const label = `[${TYPE_SINGULAR[b.type] || b.type}] ${b.id}`;
      // ux-w1-3: bound the always-active summary by hint_summary_max_len, mirroring
      // the REFERENCE must_read_if hook below — one long entry must not blow up the
      // SessionStart sink.
      const raw = typeof b.summary === "string" ? b.summary.trim() : "";
      const summary = truncateSummary(raw, summaryMaxLen);
      lines.push(
        summary.length > 0 ? `  ${badge} ${label} · ${summary}` : `  ${badge} ${label}`,
      );
      indexCount += 1;
    }
  }

  // REFERENCE — broad decision/pitfall/process: scope badge + title + must_read_if hook.
  if (referenceEntries.length > 0) {
    lines.push(
      zh
        ? "  REFERENCE (情境触发 · 命中 must_read_if 时 Read / fab_recall):"
        : "  REFERENCE (situational · Read when must_read_if fires / fab_recall):",
    );
    let folded = 0;
    for (const e of referenceEntries) {
      if (backstop > 0 && indexCount >= backstop) {
        folded += 1;
        continue;
      }
      const badge = scopeBadge(layerToScope(e));
      const type = TYPE_SINGULAR[toPluralType(e.type)] || e.type;
      const rawHook =
        typeof e.must_read_if === "string" && e.must_read_if.length > 0
          ? e.must_read_if
          : typeof e.summary === "string"
            ? e.summary
            : "";
      const hookText = truncateSummary(rawHook, summaryMaxLen);
      lines.push(
        hookText.length > 0
          ? `  ${badge} [${type}] ${e.id} — ${hookText}`
          : `  ${badge} [${type}] ${e.id}`,
      );
      indexCount += 1;
    }
    // D0028 backstop: fold the overflow tail into one marker + drift signal.
    if (folded > 0) {
      lines.push(
        zh
          ? `  … 另 ${folded} 条 broad 条目折叠 (broad index > backstop ${backstop})。先跑 fabric-audit 瘦身;确需全展示再调 .fabric/fabric-config.json#broad_index_backstop (20..500)`
          : `  … ${folded} more broad entr${folded === 1 ? "y" : "ies"} folded (broad index > backstop ${backstop}). Run fabric-audit to prune first; raise .fabric/fabric-config.json#broad_index_backstop (20..500) only if you truly need them all`,
      );
    }
  }

  // W2-4 footer: single lean retrieval flow — no two-step.
  lines.push(
    zh
      ? "取正文: fab_recall(paths), 或 Read <store>/knowledge/<type>/<id>--*.md"
      : "Load full content: fab_recall(paths), or Read <store>/knowledge/<type>/<id>--*.md",
  );
  // H6 scope discipline: this sink carries ONLY broad (always-relevant) knowledge;
  // narrow (file-specific) entries surface contextually via the PreToolUse hint
  // when you edit a matching file (KT-DEC-0029). Stops the agent from assuming
  // SessionStart is the whole KB.
  lines.push(
    zh
      ? "范围: 此处仅 broad(始终相关);narrow(文件专属)在你编辑对应文件时由 PreToolUse 浮现"
      : "Scope: broad only (always relevant) here; narrow (file-specific) surfaces via the PreToolUse hint when you edit a matching file",
  );
  return lines.join("\n");
}

// -----------------------------------------------------------------------------
// Main entry — invoked both as a CLI (require.main === module) and in-process
// by tests. Wraps the entire flow in try/catch: ANY error → silent exit 0.
// -----------------------------------------------------------------------------

// Block 5 (Option X): build the two SessionStart sinks (human systemMessage +
// AI additionalContext) from a plan-context-hint payload, WITHOUT emitting or
// recording telemetry. This is the single shared renderer: main() calls it then
// emits + logs; `fabric inspect` calls it then prints (byte-identical injection
// by construction — same code, same config/FS reads). Pure-ish: it reads config
// + snapshot + .md summaries for `cwd` but has no stdout/ledger side effects.
//
// Returns:
//   human               — gated final human text (null when gated off / empty)
//   ai                  — gated final AI text (null when reminder-to-context off / empty)
//   resolvedPayload     — the plan-context payload, passed through unchanged (for telemetry / --explain)
//   hasRenderedContent  — true when ANY sink rendered content (main's silent-exit gate)
//   emptyKbGuidance     — true when the render was the empty-KB guidance, i.e. NOTHING
//                         was surfaced (main skips emit-side bookkeeping then)
//   reminderToContext   — readReminderToContext(cwd) (telemetry target-channel)
function buildSessionStartSinks(cwd, payload, env) {
  // KT-GLD-0006: the rc.35 opaque-summary runtime substitution
  // (resolveOpaqueSummaries) is retired. The write-time mechanical floor in
  // extractKnowledge (summary !== stable_id/slug + length floor) prevents
  // degenerate summaries at the source, so SessionStart no longer band-aids them
  // at render time; surviving legacy opaque summaries are fixed by the
  // review-time cold-eval audit pass.
  const resolvedPayload = payload;

  const summaryMaxLen = readSummaryMaxLen(cwd);
  const fabricLanguageForEmit = readFabricLanguage(cwd);

  const census =
    env && env.census !== undefined
      ? env.census
      : payload && payload.census
        ? payload.census
        : deriveCensusFromEntries(resolvedPayload && resolvedPayload.entries);
  const alwaysBodies =
    env && env.alwaysBodies !== undefined
      ? env.alwaysBodies
      : payload && Array.isArray(payload.always_bodies)
        ? payload.always_bodies
        : [];

  // H3: the LIVE census total is the single count source for the import gate —
  // computed AFTER census so the nudge and the HUD agree by construction.
  const censusTotal = census && typeof census.total === "number" ? census.total : undefined;
  const recommendImport = shouldRecommendImport(cwd, censusTotal);

  // Read the resolved-bindings snapshot ONCE — reused for the scope store label
  // (写入/只读) and the H4 review-rung pending count. Best-effort/decorative: any
  // failure leaves snapshot null and the dependent lines simply don't render.
  let snapshot = null;
  if (bindingsSnapshotReader !== null) {
    try {
      const bindingId = readWorkspaceBindingId(cwd);
      if (bindingId) snapshot = bindingsSnapshotReader.readBindingsSnapshot(bindingId);
    } catch {
      snapshot = null;
    }
  }

  const humanGate =
    nudgePolicy !== null
      ? nudgePolicy.resolveHumanSink(cwd, "session_start", {})
      : { emitHuman: true, verbosity: "normal" };

  // ---- HUMAN sink: §3 grouped census (+ verbose per-entry detail). ----
  const humanLines = renderHumanCensus(census, { lang: fabricLanguageForEmit });
  if (humanLines.length > 0 && humanGate.verbosity === "verbose") {
    const detail = renderSummary(resolvedPayload, summaryMaxLen);
    humanLines.push(...detail);
  }
  // H2: scope store label — `写入 X · 只读 Y` (replaces the legacy read-set jargon).
  if (humanLines.length > 0 && snapshot !== null) {
    const storeLabel = renderScopeStoreLabel(snapshot, fabricLanguageForEmit);
    if (storeLabel) humanLines.push(storeLabel);
  }

  // H4 backlog summary (KT-DEC-0007: nudge, never a gate). TASK-005 (grill G5 /
  // C-003): the three NON-immediate signals — review / import / maintenance —
  // moved OFF the per-Stop fabric-hint nudge (only archive stays there, C-003)
  // ONTO a SINGLE SessionStart summary line here. It fires at most once per boot
  // (SessionStart cadence), so it replaces the per-turn Stop repetition that was
  // the dominant nudge noise. Steady state (no backlog) stays fully silent — the
  // segments only render when their live thresholds trip. C-004: each segment is
  // individually dismissable via `hint_dismiss_signals` (import/review/maintenance).
  if (humanLines.length > 0 && fabricLanguageForEmit !== null) {
    const zh = fabricLanguageForEmit === "zh-CN" || fabricLanguageForEmit === "zh-CN-hybrid";
    const dismissed = readDismissedSummarySignals(cwd);
    const nowMs =
      env && env.now !== undefined
        ? env.now instanceof Date
          ? env.now.getTime()
          : Number(env.now) || Date.now()
        : Date.now();
    const segments = [];

    // import — KB is sparse (recommendImport, off the live census total).
    if (recommendImport && !dismissed.has("import")) {
      segments.push(zh ? "import 待补(KB 稀疏)" : "import (sparse KB)");
    }

    // review — pending backlog exceeds REVIEW_PENDING_THRESHOLD (live count) OR
    // the oldest pending entry has been sitting longer than the age knob. Two
    // different failures: "too many to face" vs "few but rotting"; the count
    // rung alone never catches the second.
    if (!dismissed.has("review") && snapshot !== null && bindingsSnapshotReader !== null) {
      let pendingCount = 0;
      let oldestPendingMtimeMs = null;
      try {
        const live = bindingsSnapshotReader.liveKnowledgeStats(snapshot);
        if (live && Number.isFinite(live.pendingCount)) pendingCount = Math.floor(live.pendingCount);
        if (live && Number.isFinite(live.oldestPendingMtimeMs)) {
          oldestPendingMtimeMs = live.oldestPendingMtimeMs;
        }
      } catch {
        pendingCount = 0;
      }
      const ageDays = readConfigNumber(
        cwd,
        "review_hint_pending_age_days",
        DEFAULT_REVIEW_PENDING_AGE_DAYS,
        { min: Number.MIN_VALUE },
      );
      // Guard on pendingCount > 0: a store with zero pending has no oldest
      // entry, and a stale/absent mtime must not manufacture an age nudge.
      const oldestDays =
        pendingCount > 0 && oldestPendingMtimeMs !== null
          ? (nowMs - oldestPendingMtimeMs) / 86_400_000
          : null;
      if (pendingCount > REVIEW_PENDING_THRESHOLD) {
        segments.push(
          zh ? `review ${pendingCount} 条 pending 待审` : `review ${pendingCount} pending`,
        );
      } else if (oldestDays !== null && oldestDays >= ageDays) {
        segments.push(
          zh
            ? `review ${pendingCount} 条 pending 已挂 ${oldestDays.toFixed(0)} 天`
            : `review ${pendingCount} pending, oldest ${oldestDays.toFixed(0)}d`,
        );
      }
    }

    // maintenance — doctor/lint not run in maintenance_hint_days AND corpus
    // non-trivial. `censusTotal` is the live canonical count computed above.
    if (!dismissed.has("maintenance")) {
      const canonicalTotal = typeof censusTotal === "number" ? censusTotal : 0;
      if (canonicalTotal >= MAINTENANCE_HINT_MIN_CANONICAL) {
        const ageDays = readLastDoctorRunAgeDays(cwd, nowMs);
        const staleDays = readConfigNumber(cwd, "maintenance_hint_days", DEFAULT_MAINTENANCE_HINT_DAYS, {
          min: 1,
        });
        if (ageDays === null) {
          segments.push(zh ? "maintenance 从未跑 lint" : "maintenance (lint never run)");
        } else if (ageDays >= staleDays) {
          segments.push(
            zh
              ? `maintenance 已 ${ageDays.toFixed(0)} 天未 lint`
              : `maintenance (${ageDays.toFixed(0)}d since lint)`,
          );
        }
      }
    }

    if (segments.length > 0) {
      humanLines.push(
        zh
          ? `  📋 Fabric backlog: ${segments.join(" · ")} — 需要时调 /fabric-review · /fabric-archive source · fabric doctor`
          : `  📋 Fabric backlog: ${segments.join(" · ")} — run /fabric-review · /fabric-archive source · fabric doctor as needed`,
      );
    }
  }

  // H5: the `下一步: …fab_recall…` AI-plumbing line is retired from the human sink
  // (the AI gets it from its own footer + the MCP server directive). Keep only the
  // pointer to the byte-identical inspector for this injection.
  if (humanLines.length > 0) {
    humanLines.push(
      fabricLanguageForEmit === "zh-CN"
        ? "  看具体注入: fabric inspect (--explain 看每条来源)"
        : "  Inspect this injection: fabric inspect (--explain for per-entry provenance)",
    );
  }

  // ---- AI sink: spine — always-active INDEX lines (no eager body, KT-DEC-0036)
  // + reference, bounded by the broad_index_backstop fold. ----
  const broadIndexBackstop = readBroadIndexBackstop(cwd);
  const aiText = renderAiSink({
    entries: resolvedPayload && Array.isArray(resolvedPayload.entries) ? resolvedPayload.entries : [],
    alwaysBodies,
    broadIndexBackstop,
    summaryMaxLen,
    lang: fabricLanguageForEmit,
  });

  const hasRenderedContent = humanLines.length > 0 || (typeof aiText === "string" && aiText.length > 0);
  const human = humanGate.emitHuman && humanLines.length > 0 ? humanLines.join("\n") : null;
  const reminderToContext = readReminderToContext(cwd);
  const ai = reminderToContext && aiText && aiText.length > 0 ? aiText : null;

  return {
    human,
    ai,
    resolvedPayload,
    hasRenderedContent,
    reminderToContext,
    // F05: true when the ONLY thing rendered was the empty-KB first-run guidance
    // (no knowledge was surfaced, because there is none).
    emptyKbGuidance: isEmptyKbCensus(census),
  };
}

function main(env, stdio) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const now = (env && env.now) || new Date();
    const nowMs = now instanceof Date ? now.getTime() : Number(now);
    const err = (stdio && stdio.stderr) || process.stderr;
    const out = (stdio && stdio.stdout) || process.stdout;

    // v2.0.0-rc.33 W2-5 (P1-8): cooldown gate. When configured > 0 hours, the
    // broad banner stays silent for that many hours after a successful emit.
    // 0 (default) preserves rc.32 behavior — every SessionStart re-fires the
    // banner. Test seam env.skipCooldown bypasses for unit tests.
    const cooldownHours = readBroadCooldownHours(cwd);
    if (cooldownHours > 0 && !(env && env.skipCooldown === true)) {
      const lastEmitMs = readBroadLastEmit(cwd);
      if (
        typeof lastEmitMs === "number" &&
        // rc.34 TASK-01 + review-fix (Gemini P1): when lastEmit is in the
        // FUTURE relative to now (backward clock skew — NTP sync /
        // suspend-wake / TZ change), the gate fires immediately. Otherwise
        // standard cooldown check. Math.max(0, …) was a no-op (silent for
        // cooldown + |skew| under both formulations); this guard actually
        // heals the skew on the next invocation by treating future-stamped
        // sidecar as "expired."
        nowMs >= lastEmitMs &&
        nowMs - lastEmitMs < cooldownHours * MS_PER_HOUR
      ) {
        return; // still in cooldown — silent
      }
    }

    // Test seam: env.payload short-circuits the CLI spawn so unit tests can
    // feed canned plan-context-hint JSON without depending on a built CLI.
    const payload =
      env && env.payload !== undefined ? env.payload : invokePlanContextHint(cwd);
    if (payload === null || payload === undefined) return; // silent

    // W2-1 (KT-DEC-0028): broad 全显示 — the legacy hint_broad_top_k hard cap is
    // retired. SessionStart must SEE every broad entry; scale is bounded by the
    // per-line char cap + broad_index_backstop fold, not by dropping entries.

    // Block 5 (Option X): build both sinks via the shared renderer (same code
    // `fabric inspect` uses → byte-identical injection). Side-effect-free; the
    // emit + telemetry below stay in main().
    const { human, ai, resolvedPayload, hasRenderedContent, reminderToContext, emptyKbGuidance } =
      buildSessionStartSinks(cwd, payload, env);

    // Nothing to say at all → silent exit (preserves the empty-payload contract).
    if (!hasRenderedContent) return;

    // v2.2 dual-sink (Goal A / D7): emit both channels in one render. The human
    // systemMessage is gated by nudge_mode (emitHuman); the AI additionalContext
    // is emitted regardless. emitDualSink shapes the protocol per client (CC/Codex
    // camelCase nested envelope; unknown → stderr).
    if (!(env && env.skipStdout === true)) {
      emitDualSink(
        { human, ai },
        { client: detectClient(), eventName: "SessionStart", streams: { stdout: out, stderr: err } },
      );
    } else if (human !== null) {
      // skipStdout test seam: still surface the human breadcrumb to stderr.
      err.write(`${human}\n`);
    }

    // v2.2 HK3-telemetry (W3-T1): record the injection side. We just OFFERED the
    // agent `resolvedPayload.entries` (the top_k-sliced broad menu); log their
    // ids so the true hit rate (consumed ÷ injected) is computable against the
    // consumption-side metrics.jsonl. Best-effort — never affects the emit.
    if (injectionLog !== null && !emptyKbGuidance) {
      const injectedEntries = Array.isArray(resolvedPayload && resolvedPayload.entries)
        ? resolvedPayload.entries
        : [];
      injectionLog.logInjection(cwd, {
        surface: "broad",
        stableIds: injectedEntries.map((e) => (e && e.id) || "").filter(Boolean),
        count: injectedEntries.length,
        revisionHash:
          resolvedPayload && typeof resolvedPayload.revision_hash === "string"
            ? resolvedPayload.revision_hash
            : null,
        ts: nowMs,
      });
    }

    // v2.2 dual-sink (Goal A): the legacy rc.33 W2-6 stdout JSON envelope is
    // replaced by emitDualSink above (which carries BOTH the human systemMessage
    // and the AI additionalContext, shaped per client). hint_reminder_to_context
    // still gates whether the AI sink is populated (see `ai` above).

    // v2.1 NEW-N-3 (ADJ-NEWN-3): hook_surface_emitted instrumentation. One
    // best-effort ledger row recording WHICH broad-scoped ids were surfaced
    // into the session — the join key for measuring hook→behavior delta (did
    // the agent fab_recall what the hook surfaced?). SessionStart fires once
    // per session boot so this never bloats the ledger. Never blocks the hook
    // (KT-DEC-0007): any failure (no .fabric/, undetected client, write error)
    // degrades to silent skip. Client is omitted-by-skip when undetectable
    // because the schema's `client` enum admits only cc/codex.
    try {
      const surfaceClient = detectClient();
      const fabricDir = join(cwd, FABRIC_DIR_REL);
      if (surfaceClient !== undefined && !emptyKbGuidance && existsSync(fabricDir)) {
        const renderedIds =
          resolvedPayload && Array.isArray(resolvedPayload.entries)
            ? resolvedPayload.entries
                .map((e) => (e && typeof e.id === "string" ? e.id : null))
                .filter((x) => x !== null)
            : [];
        let idSuffix;
        try {
          idSuffix = require("node:crypto").randomUUID();
        } catch {
          idSuffix = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
        }
        const surfaceEvent = {
          kind: "fabric-event",
          id: `event:${idSuffix}`,
          ts: Date.now(),
          schema_version: 1,
          event_type: "hook_surface_emitted",
          hook_name: "knowledge-hint-broad",
          client: surfaceClient,
          target_channel: reminderToContext ? "stdout-additionalContext" : "stderr",
          rendered_ids: renderedIds,
          delivery_status: "delivered",
        };
        appendEvent(fabricDir, surfaceEvent);
      }
    } catch {
      // best-effort telemetry — never block session start
    }

    // v2.0.0-rc.33 W2-5 (P1-8): record successful emit timestamp for the
    // cooldown gate's next-invocation check. Skip when cooldown is disabled
    // (cooldownHours === 0) to avoid polluting the FS with a never-read
    // sidecar on rc.32-style "no cooldown" workspaces.
    //
    // F05: also skip for the empty-KB guidance. Before G2 that render short-
    // circuited above (humanLines was empty), so this tail never ran for an empty
    // KB; making the guidance render revived it and let the guidance BURN the 24h
    // cooldown. The exact sequence that breaks: session 1 on an empty KB shows
    // "bind a store", stamps the sidecar → the user binds one → session 2 minutes
    // later is silent for a full day, hiding the first REAL knowledge HUD, which
    // is precisely the first-touch path G2 set out to fix. The cooldown exists to
    // stop the KB banner repeating; the guidance is not that banner, and it is
    // self-limiting — it stops the moment the KB stops being empty.
    if (cooldownHours > 0 && !emptyKbGuidance && !(env && env.skipCooldownWrite === true)) {
      writeBroadLastEmit(cwd, nowMs);
    }

    // Active-session sidecar for MCP fab_recall session_id fallback: when the
    // agent omits session_id, planContext can still stamp knowledge_context_planned
    // with the real client session so recall_coverage correlates. Best-effort.
    // Sources: require.main stdin parse → env.session_id, env.payload.session_id
    // (tests), FABRIC_SESSION_ID.
    try {
      const seamSid =
        env && typeof env.session_id === "string" && env.session_id.length > 0
          ? env.session_id
          : null;
      const payloadSid =
        env &&
        env.payload &&
        typeof env.payload === "object" &&
        typeof env.payload.session_id === "string" &&
        env.payload.session_id.length > 0
          ? env.payload.session_id
          : null;
      const envSid =
        typeof process.env.FABRIC_SESSION_ID === "string" &&
        process.env.FABRIC_SESSION_ID.length > 0
          ? process.env.FABRIC_SESSION_ID
          : null;
      const activeSid = seamSid || payloadSid || envSid;
      if (activeSid && !(env && env.skipActiveSessionWrite === true)) {
        writeActiveSession(cwd, activeSid, nowMs);
      }
    } catch {
      // never block session start
    }
  } catch {
    // Silent — never block session start on hook failure.
  }
}

module.exports = {
  main,
  buildSessionStartSinks,
  invokePlanContextHint,
  groupEntries,
  renderFull,
  renderTruncated,
  renderSummary,
  // W3-B F-005: the reskinned SessionStart AI sink + its layer→scope mapper,
  // exported for the NO_COLOR snapshot test (hud-reskin.test.ts).
  renderAiSink,
  layerToScope,
  truncateSummary,
  // rc.8 underseed self-check helpers (exported for unit testing).
  countCanonicalNodes,
  readUnderseedThreshold,
  isImportTouched,
  shouldRecommendImport,
  // TASK-005 (grill G5): SessionStart backlog-summary helpers (exported for tests).
  readLastDoctorRunAgeDays,
  readDismissedSummarySignals,
  // W2-1 (KT-DEC-0028) + rc.33 W2-5 / W2-6 helpers.
  readBroadIndexBackstop,
  readBroadCooldownHours,
  readReminderToContext,
  readBroadLastEmit,
  writeBroadLastEmit,
  readSummaryMaxLen,
  CONSTANTS: {
    TRUNCATION_THRESHOLD,
    CLI_TIMEOUT_MS,
    SUMMARY_MAX_LEN: DEFAULT_SUMMARY_MAX_LEN,
    DEFAULT_SUMMARY_MAX_LEN,
    CANONICAL_TYPE_ORDER,
    MATURITY_PROVEN,
    MATURITY_VERIFIED,
    MATURITY_DRAFT,
    DEFAULT_UNDERSEED_NODE_THRESHOLD,
    KNOWLEDGE_CANONICAL_TYPES,
    DEFAULT_HINT_BROAD_INDEX_BACKSTOP,
    DEFAULT_HINT_BROAD_COOLDOWN_HOURS,
    DEFAULT_HINT_REMINDER_TO_CONTEXT,
    HINT_BROAD_LAST_EMIT_FILE_NAME,
  },
};

if (require.main === module) {
  // Claude Code / Codex pass SessionStart JSON on stdin (session_id, …).
  // Read once so we can stamp the active-session sidecar for MCP fab_recall.
  let stdinRaw = "";
  try {
    stdinRaw = require("node:fs").readFileSync(0, "utf8");
  } catch {
    // no stdin — leave session_id unset
  }
  let session_id = null;
  if (stdinRaw.length > 0) {
    try {
      const parsed = JSON.parse(stdinRaw);
      if (parsed && typeof parsed.session_id === "string" && parsed.session_id.length > 0) {
        session_id = parsed.session_id;
      }
    } catch {
      // non-JSON stdin — ignore
    }
  }
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main(
    { cwd: context.workspaceRoot, session_id },
    { stderr: process.stderr },
  );
  process.exit(0);
}
