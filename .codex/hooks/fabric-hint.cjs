#!/usr/bin/env node
const { existsSync, mkdirSync, readFileSync, readdirSync, statSync, writeFileSync } = require("node:fs");
const { dirname, join } = require("node:path");

// W1-01 (ISS-012): Stop / SessionStart hooks append to shared, non-session-scoped
// ledgers (events.jsonl, metrics.jsonl). Under multi-window concurrency a bare
// appendFileSync can interleave a partial write; route through the advisory-lock
// primitive (drop-on-contention, best-effort — matches injection-log).
// ux-w2-9: events.jsonl writes go through the single guarded event-writer
// (envelope stamp + event_type guard); metrics.jsonl stays on the raw locked
// primitive (it is not a schema-governed event ledger).
const { appendLockedLine } = require("./lib/injection-log.cjs");
const { appendEvent } = require("./lib/event-writer.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

// ISS-20260713-020: concern modules extracted from this monolith
const pendingStatsLib = require("./lib/pending-stats.cjs");
const importStateLib = require("./lib/import-state.cjs");
const ledgerScan = require("./lib/ledger-scan.cjs");
const { summarizeTranscript } = require("./lib/transcript-summary.cjs");

// ISS-20260713-040: further concern modules
const hintConfig = require("./lib/hint-config.cjs");
const sessionSignalState = require("./lib/session-signal-state.cjs");
const assistantTurnEmit = require("./lib/assistant-turn-emit.cjs");
const signalDecide = require("./lib/signal-decide.cjs");
// residual thin-orchestrator extracts (main / emitSoft / maintenance / graph-edge)
const maintenanceSignal = require("./lib/maintenance-signal.cjs");
const graphEdgeEmit = require("./lib/graph-edge-emit.cjs");
const softSignalEmit = require("./lib/soft-signal-emit.cjs");
const hintThresholds = require("./lib/hint-thresholds.cjs");
const stopStdin = require("./lib/stop-stdin.cjs");
const sessionStatusEmit = require("./lib/session-status-emit.cjs");

const readWorkspaceBindingId = pendingStatsLib.readWorkspaceBindingId;
const readSnapshotKnowledgeStats = pendingStatsLib.readSnapshotKnowledgeStats;
const readLegacyPendingStats = pendingStatsLib.readLegacyPendingStats;
const readPendingStats = pendingStatsLib.readPendingStats;
const countCanonicalNodes = pendingStatsLib.countCanonicalNodes;
// pending constants still defined below for CONSTANTS export compatibility when present
const isImportInFlight = importStateLib.isImportInFlight;
const IMPORT_STATE_FILE_REL = importStateLib.IMPORT_STATE_FILE_REL;
const IMPORT_IN_FLIGHT_MAX_AGE_HOURS = importStateLib.IMPORT_IN_FLIGHT_MAX_AGE_HOURS;
const readLedger = ledgerScan.readLedger;
const hasHighValueArchiveSignal = ledgerScan.hasHighValueArchiveSignal;
const sessionArchiveWatermark = ledgerScan.sessionArchiveWatermark;
const sessionFirstActivityTs = ledgerScan.sessionFirstActivityTs;
const sessionAnchorTs = ledgerScan.sessionAnchorTs;
const countSessionMutationsSince = ledgerScan.countSessionMutationsSince;
const countBacklogSessions = ledgerScan.countBacklogSessions;
const tallySessionActivity = ledgerScan.tallySessionActivity;
const countEditsSince = ledgerScan.countEditsSince;
const getTopEditedDirectories = ledgerScan.getTopEditedDirectories;
const EDIT_COUNTER_FILE_REL = ledgerScan.EDIT_COUNTER_FILE_REL;
const ARCHIVE_BACKLOG_ANTI_LOOP_HOURS = ledgerScan.ARCHIVE_BACKLOG_ANTI_LOOP_HOURS;
const DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT = ledgerScan.DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT;
const DEFAULT_ARCHIVE_BACKLOG_IDLE_HOURS = ledgerScan.DEFAULT_ARCHIVE_BACKLOG_IDLE_HOURS;

// v2.0.0-rc.7 T5: session-digest writer. Best-effort (never blocks Stop hook
// on failure — see contract in lib/session-digest-writer.cjs).
let sessionDigestWriter = null;
try {
  sessionDigestWriter = require("./lib/session-digest-writer.cjs");
} catch {
  // Helper module missing — degrade silently. Digest writing is opt-in
  // observability; the rest of fabric-hint must still function.
  sessionDigestWriter = null;
}

// v2.0.0-rc.16 TASK-002 (F2-apply): banner-i18n lib for the 5 Signal
// banners (A/B/C/D-never/D-aged). Resolved ONCE per main() invocation and
// threaded into decide() / evaluateMaintenanceSignal() via the existing
// thresholds object. Lib is required at module load; failure to load is
// fatal-here-but-silent: the require itself can't throw without the .cjs
// being missing entirely (a packaging bug we'd want to surface during
// install integration tests, not silently swallow).
const { renderBanner, readFabricLanguage } = require("./lib/banner-i18n.cjs");

// v2.0.0-rc.24 TASK-04: shared cite-line parser (CJS twin of
// packages/shared/src/cite-line-parser.ts, byte-shipped via installHookLibs).
// Provides `parseCiteLine(raw)` → { cite_ids, cite_tags, cite_commitments }.
// Hook runtime has no node_modules access; the twin is hand-synced and
// behavior-parity-tested against the TS source.
let citeLineParser = null;
try {
  citeLineParser = require("./lib/cite-line-parser.cjs");
} catch {
  // Helper module missing — degrade silently. parseKbLine falls back to a
  // legacy in-file regex when the lib is unavailable (e.g. mid-upgrade where
  // hook script lands before lib is copied). New cite_commitments output is
  // empty in degraded mode.
  citeLineParser = null;
}

// v2.0.0-rc.37 NEW-30: shared client-protocol adapter. Guarded require (this
// hook runs in arbitrary user repos); detectClient delegates the 3-tier
// detection to the lib, falling back to env-only when the lib is absent.
let clientAdapter = null;
try {
  clientAdapter = require("./lib/client-adapter.cjs");
} catch {
  clientAdapter = null;
}

// v2.2 dual-sink (Goal A / D4): human-output gate for the archive nudge. The Stop
// archive nudge is SOFT (additionalContext, never decision:block — D3) and the
// human systemMessage is gated by nudge_mode. Optional require — absent → human
// always emits (legacy posture).
let nudgePolicy = null;
try {
  nudgePolicy = require("./lib/nudge-policy.cjs");
} catch {
  nudgePolicy = null;
}

// v2.0.0-rc.37 NEW-16: shared config + sidecar I/O for the per-signal dismiss
// feature (config-level durable opt-out + session-scoped sidecar). Guarded
// require (house style); dismiss simply doesn't fire if the lib is absent.
let configCache = null;
let stateStore = null;
try {
  configCache = require("./lib/config-cache.cjs");
} catch {
  configCache = null;
}
try {
  stateStore = require("./lib/state-store.cjs");
} catch {
  stateStore = null;
}

// v2.1.0-rc.1 P4 (F4/S63): hook-side reader for the CLI pre-generated
// resolved-bindings snapshot. The Stop hint surfaces the read-set stores
// (per-store, NOT aggregated into one pile) without re-resolving / walking
// store trees. Best-effort — a missing lib/snapshot omits the store line.
let bindingsSnapshotReader = null;
try {
  bindingsSnapshotReader = require("./lib/bindings-snapshot-reader.cjs");
} catch {
  bindingsSnapshotReader = null;
}
// CONSTANTS — SSOT for thresholds/defaults lives in lib/hint-config.cjs.
// Facade re-exports for CONSTANTS surface + main() local names (tests depend on
// hook.CONSTANTS.*). Event-type / ledger file names that are unique to this
// orchestrator's I/O stay local.
const FABRIC_DIR = hintConfig.FABRIC_DIR;
const EVENT_LEDGER_FILE = "events.jsonl";
// v2.0.0-rc.39 (P1 emit-fold): empty-shell assistant_turn_observed fold target.
const METRICS_LEDGER_FILE = "metrics.jsonl";
const EVENT_TYPE_PROPOSED = "knowledge_proposed";
const EVENT_TYPE_INIT_SCAN_COMPLETED = "init_scan_completed";
// G3: hasHighValueArchiveSignal thin wrapper over shared SST twin (see ledger-scan).
const EVENT_TYPE_DOCTOR_RUN = maintenanceSignal.EVENT_TYPE_DOCTOR_RUN || "doctor_run";
const EVENT_TYPE_ASSISTANT_TURN_OBSERVED = "assistant_turn_observed";
// Threshold / path defaults — re-export only (no dual literals).
const DEFAULT_ARCHIVE_HINT_HOURS = hintConfig.DEFAULT_ARCHIVE_HINT_HOURS;
const MS_PER_HOUR = hintConfig.MS_PER_HOUR;
const DEFAULT_ARCHIVE_EDIT_THRESHOLD = hintConfig.DEFAULT_ARCHIVE_EDIT_THRESHOLD;
const PENDING_DIR = hintConfig.PENDING_DIR;
const PENDING_TYPES = hintConfig.PENDING_TYPES;
const DEFAULT_REVIEW_HINT_PENDING_COUNT = hintConfig.DEFAULT_REVIEW_HINT_PENDING_COUNT;
const DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS = hintConfig.DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS;
const MS_PER_DAY = hintConfig.MS_PER_DAY;
const DEFAULT_MAINTENANCE_HINT_DAYS = hintConfig.DEFAULT_MAINTENANCE_HINT_DAYS;
const DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS = hintConfig.DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS;
const KNOWLEDGE_CANONICAL_TYPES = hintConfig.KNOWLEDGE_CANONICAL_TYPES;
const DEFAULT_UNDERSEED_NODE_THRESHOLD = hintConfig.DEFAULT_UNDERSEED_NODE_THRESHOLD;
const UNDERSEED_POST_INIT_QUIET_HOURS = hintConfig.UNDERSEED_POST_INIT_QUIET_HOURS;
const UNDERSEED_NO_PROPOSED_HOURS = hintConfig.UNDERSEED_NO_PROPOSED_HOURS;
const CONFIG_FILE = hintConfig.CONFIG_FILE;
const DEFAULT_COOLDOWN_HOURS = hintConfig.DEFAULT_COOLDOWN_HOURS;
const SHOWN_CACHE_FILE = hintConfig.SHOWN_CACHE_FILE;
const MAINTENANCE_HINT_LAST_EMIT_FILE = hintConfig.MAINTENANCE_HINT_LAST_EMIT_FILE;
const MAINTENANCE_HINT_MIN_CANONICAL = hintConfig.MAINTENANCE_HINT_MIN_CANONICAL;

// v2.0.0-rc.8 (TASK-002): in-flight import gate for Signal B.
// fabric-import skill writes `.fabric/.import-state.json` checkpoints after
// every successful sub-step (P1/P2/P3 — see fabric-import/SKILL.md). The
// Stop hook reads this file as a soft signal to know that an import is
// mid-run, so we can silence Signal B (review hint at pending count >= 10)
// to avoid interrupting the import while it accumulates pending entries.
//
// Gate is intentionally narrow: ONLY Signal B is suppressed. Signals A
// (archive), C (import recommendation), D (maintenance) retain their
// pre-existing behaviour byte-for-byte. The 24h TTL on `last_checkpoint_at`
// guards against stale state files that would otherwise permanently
// silence Signal B if a user abandoned an import without completing.

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
// ---------------------------------------------------------------------------
// Observability grill (a + Q4): session-activity status breadcrumb.
//
// A no-signal Stop used to return SILENT — the human only ever heard from
// Fabric when there was a nudge to act on, never a "here is what I did" recap,
// which reads as "Fabric does nothing in the background". These two helpers add
// a HUMAN-ONLY trust anchor (the AI gets no activity recap — flow ⊥ observation,
// D5) plus the nudge_mode tier-guidance line (so the human-channel volume knob
// is discoverable). Cadence is gated by nudge_mode at emit time.
// ---------------------------------------------------------------------------

// Session-scoped tally. Counts ONLY events that carry session_id, filtered to
// the current session — knowledge_context_planned / knowledge_proposed lack
// session_id and are intentionally excluded (a cross-session count would
// mislead). Exported for unit tests.
// Emit the human-facing session status breadcrumb when no actionable signal
// fired. Human sink ONLY. Cadence by nudge_mode: silent → never; minimal/normal
// → once per session; verbose → every turn. Folds in the tier-guidance line on
// the first status of the session so the volume knob is discoverable. Never
// throws — the caller wraps it, but every branch degrades silently anyway.
function emitSessionStatus(cwd, events, stdinPayload, nowMs, pendingStats, out) {
  return sessionStatusEmit.emitSessionStatus(cwd, events, stdinPayload, nowMs, pendingStats, out);
}

/**
 * rc.7 T4: format the "最近活动集中在: <dir1> (N edits), <dir2> (M edits)"
 * fragment used by the Signal A banner. Returns empty string when there is
 * no aggregable activity (so the banner caller can skip the line entirely).
 */
function formatActivityOverview(projectRoot, anchorTs) {
  return sessionStatusEmit.formatActivityOverview(projectRoot, anchorTs);
}

/**
 * Resolve the archive_edit_threshold from .fabric/fabric-config.json,
 * falling back to DEFAULT_ARCHIVE_EDIT_THRESHOLD (20). Any read/parse failure
 * or non-positive value → default. Mirrors readUnderseedThreshold's contract.
 */

/**
 * Decide whether to emit a hook reminder.
 *
 * rc.6 archive signal (TASK-022 / E5 — Signal A, 24h-OR-N-edits):
 *   - Trigger when EITHER (a) hours since last knowledge_proposed >= 24,
 *     OR (b) edit-counter lines with ts > last-knowledge_proposed >= threshold
 *     (default 20).
 *   - If no knowledge_proposed event has ever been recorded, Signal A stays
 *     silent regardless of edit count (a never-archived workspace is handled
 *     by Signal C / import; Signal A needs an anchor event to count from).
 *   - The edit-count branch was dropped in rc.5 (TASK-015) because the prior
 *     `5 plan_contexts` proxy was inflated by hook auto-fires. rc.6 (TASK-022)
 *     reintroduces it on a reliable substrate: the PreToolUse sidecar
 *     written by TASK-020 / E4. Missing/malformed edit-counter degrades
 *     safely to the 24h-only path.
 *
 * rc.3 review signal (TASK-004 — Signal B):
 *   - Trigger when (pending count >= 10) OR (oldest pending mtime age >= 7 days).
 *
 * rc.5 import signal (TASK-010 — Signal C):
 *   - Trigger when canonical node count < underseed threshold AND an
 *     init_scan_completed event has fired at least 24h ago AND no
 *     knowledge_proposed event has fired in the last 24h.
 *
 * Precedence: archive > review > import. Archive wins when both archive AND
 * any other signal fire — recent in-session work is the most urgent reminder.
 * Review wins over import because pending overflow is a sharper backlog signal
 * than a sparse corpus.
 *
 * The `editCounterStats` parameter is the per-session edit view (crack 1)
 * computed in main() from file_mutated events:
 *   { editsSinceArchive: number, threshold: number, anchorPresent: boolean }
 * The `backlogStats` parameter (crack 2) is the cross-session view:
 *   { deadSessionCount: number, threshold: number }
 * Both default to a no-trigger shape when omitted (back-compat for callers
 * pre-dating the two-lane split).
 *
 * Returns one of (ux-w0-3: `decision: 'soft'` — a reminder, never a gate):
 *   - { decision: 'soft', reason, signal: 'archive', recommended_skill: 'fabric-archive' }
 *   - { decision: 'soft', reason, signal: 'archive_backlog', recommended_skill: 'fabric-archive' }
 *   - { decision: 'soft', reason, signal: 'review', recommended_skill: 'fabric-review' }
 *   - { decision: 'soft', reason, signal: 'import', recommended_skill: 'fabric-import' }
 *   - null on no trigger
 */
// rc.7 T7: thresholds is the externalized-config view passed in by main().
// The shape mirrors the DEFAULT_ constants 1:1 so tests can synthesize it
// without touching the filesystem. Omitting the arg falls back to documented
// defaults so existing in-process callers (tests that pre-date T7) still
// pass without modification — they implicitly exercise the default path.
function decide(events, now, pendingStats, underseedStats, editCounterStats, thresholds, banner, importInFlight, backlogStats) {
  return signalDecide.decide(events, now, pendingStats, underseedStats, editCounterStats, thresholds, banner, importInFlight, backlogStats);
}

// ---------------------------------------------------------------------------
// rc.7 T7: config readers for the three externalized thresholds + two new
// maintenance_hint_* fields. All readers share the same contract as the
// pre-existing readers in this file: synchronous fs read, missing file or
// malformed JSON → return the documented default, never throw. Caching is
// not done at the reader layer because each main() invocation reads at
// most once per field and the file is <1KB.
// ---------------------------------------------------------------------------


// crack 2: cross-session backlog signal thresholds.


/**
 * Resolve the cooldown setting from .fabric/fabric-config.json
 * (archive_hint_cooldown_hours), falling back to DEFAULT_COOLDOWN_HOURS.
 * Any read/parse failure → default (never block on config errors).
 */

/**
 * Resolve the underseed-node threshold from .fabric/fabric-config.json
 * (underseed_node_threshold), falling back to DEFAULT_UNDERSEED_NODE_THRESHOLD.
 * Any read/parse failure → default (never block on config errors).
 */

// F13 (ISS-20260531-038): the reminder cooldown sidecars were process-global
// (one file per project, no session key), so in concurrent multi-window sessions
// one window firing a nudge wrote the cooldown and silenced that nudge in EVERY
// other window. Scope the sidecar filename by sessionId — mirrors the already-
// session-scoped dismiss sidecar (sessionDismissFileName). Backward-compatible:
// a null/absent sessionId falls back to the legacy non-scoped path (upgrade +
// pre-session-id callers), so existing on-disk state and tests are unaffected;
// the Stop hook always passes the real session_id from its stdin payload.
function resolveHookSessionId(payload, env) {
  return stopStdin.resolveHookSessionId(payload, env);
}


// -----------------------------------------------------------------------------
// v2.0.0-rc.37 NEW-16 — per-signal dismiss.
//
// Two suppression levers, both honoured at emit time (a chosen signal whose
// type is dismissed exits silently, exactly like a cooldown hit):
//   1. Durable opt-out — hint_dismiss_signals: string[] in the GLOBAL policy
//      layer (~/.fabric/fabric-global.json → projects[<project_id>] ?? defaults).
//      config-single-home W3 made the repo fabric-config.json identity-only, so
//      a copy left there is inert — and renderDismissOption must name the file
//      readDismissedSignals actually opens (dismiss-signal-parity.test.ts).
//      Mirrors the cite_evict_interval=0 opt-out convention; survives across
//      sessions. The concrete user-actionable lever surfaced in the nudge.
//   2. Session-scoped — .fabric/.cache/hint-dismiss-{sessionId}.json
//      { dismissed: string[] }. Ephemeral; written by the agent when the user
//      asks to silence a nudge type for the current session (Fabric's
//      AI-driven write convention — no new CLI surface).
//
// The five signal types ('archive' / 'archive_backlog' / 'review' / 'import' /
// 'maintenance' — DISMISSABLE_SIGNALS is the canonical list)
// each have an independent cooldown ALREADY (signal-keyed SHOWN_CACHE for
// A/B/C + the maintenance day-cooldown sidecar), so dismiss layers cleanly on
// top of per-signal cadence without a physical 4-hook split (which would 4×
// the per-Stop process spawn and break the deliberate single-nudge-per-turn
// precedence model — KT-DEC-0007 anti-nag spirit).
// -----------------------------------------------------------------------------


// Returns a Set of dismissed signal types (config-durable ∪ session sidecar).
// Never throws — degrades to an empty set when libs are absent.

// Persist a session-scoped dismiss set (additive merge). Exposed for the
// agent-driven write path + tests; not auto-invoked by the hook. Never throws.

// Bilingual one-line dismiss hint appended to every nudge so the user knows
// the lever exists. Variant fold mirrors banner-i18n: zh-CN / zh-CN-hybrid →
// Chinese; en / match-existing / unknown → English.

/**
 * v2.0.0-rc.7 T10: find the most recent doctor_run event ts in the ledger.
 * Returns the ts (epoch ms) of the newest doctor_run event, or null if none
 * has ever fired. Walks the events array tail-first for efficiency (early-out
 * on first match).
 */
function findLastDoctorRunTs(events) {
  return maintenanceSignal.findLastDoctorRunTs(events);
}

/**
 * v2.0.0-rc.7 T10: read the Signal-D cooldown sidecar timestamp (epoch ms).
 * Missing file / parse failure → null (allow signal to fire).
 */


/**
 * v2.0.0-rc.7 T10: Signal D — maintenance hint.
 *
 * Trigger when ALL of the following hold:
 *   1. No doctor_run event has fired in the last `maintenance_hint_days`
 *      (default 14), OR no doctor_run event has ever fired.
 *   2. Canonical node count >= MAINTENANCE_HINT_MIN_CANONICAL (default 5).
 *      A fresh workspace with no knowledge has nothing to lint.
 *   3. Cooldown: not within `maintenance_hint_cooldown_days` (default 7) of
 *      the previous Signal-D emit. Tracked via dedicated sidecar
 *      `.fabric/.cache/maintenance-hint-last-emit`.
 *
 * Returns one of (ux-w0-3: `decision: 'soft'` — a reminder, never a gate):
 *   - { decision: 'soft', reason, signal: 'maintenance', recommended_skill: null }
 *   - null on no trigger
 *
 * `recommended_skill` is intentionally null — the maintenance prompt
 * recommends a CLI invocation (`fabric doctor`), not a Skill, because
 * doctor is a CLI surface (Q-13 boundary). The hook payload still shapes the
 * `recommended_skill` key so consumers can branch on it.
 */
function evaluateMaintenanceSignal(events, now, canonicalCount, lastEmitMs, thresholds) {
  return maintenanceSignal.evaluateMaintenanceSignal(events, now, canonicalCount, lastEmitMs, thresholds);
}

// lifecycle-refactor W3-A2 (§7 graph generation signal): after a successful
// archive the Stop hook REQUESTS edge extraction by emitting one
// graph_edge_candidate_requested{stable_id, store?}. The hook never PRODUCES
// edges (that is the archive/import skill's or doctor co-occurrence's job,
// KT-DEC-0007) — it only flags "this entry just landed; someone should extract
// its `related` edges". FROZEN-safe: O(1) tail scan, best-effort silent, single
// advisory-locked appendLockedLine (same primitive the rest of this hook uses).
//
// HONEST stable_id sourcing — the deliberate limitation: pending entries (the
// fabric-archive → extractKnowledge path) carry NO canonical stable_id (id is
// late-bound at fab_review approve), so their knowledge_proposed event omits
// stable_id (or sets the `pending:<key>` sentinel). A graph edge between
// id-less pending drafts is meaningless, so we DO NOT fabricate one. We emit
// ONLY when the most-recent knowledge_proposed event carries a real
// K[TP]-XXX-NNNN stable_id (the approve/promote path) — i.e. an entry that
// actually has a canonical node to attach edges to. When the latest proposed
// is id-less we honestly skip; the request will fire on the approve event that
// allocates the id. A session-scoped sidecar de-dupes so repeated Stop fires in
// one session don't re-request the same id.
const STABLE_ID_RE = graphEdgeEmit.STABLE_ID_RE;
const GRAPH_EDGE_REQUESTED_SIDECAR = graphEdgeEmit.GRAPH_EDGE_REQUESTED_SIDECAR;

function emitGraphEdgeCandidateBestEffort(cwd, events, sessionId) {
  return graphEdgeEmit.emitGraphEdgeCandidateBestEffort(cwd, events, sessionId);
}

// v2.1 NEW-N-3 (ADJ-NEWN-3): hook_signal_emitted instrumentation. Writes ONE
// best-effort ledger row at the point a nudge is actually delivered (post-
// cooldown), so the join key measures nudge-trigger logic (which signal fired,
// at what threshold vs. actual). Emitted at delivery rather than at
// threshold-cross so it inherits the cooldown gate — a fired-but-cooled signal
// does not spam the ledger every session. Skips silently when threshold /
// actual_value are not finite numbers (e.g. maintenance "never run" → null
// age). Never blocks the hook (KT-DEC-0007).
const SIGNAL_TYPE_ENUM = softSignalEmit.SIGNAL_TYPE_ENUM;
function emitSignalFiredEvent(cwd, sessionId, result) {
  return softSignalEmit.emitSignalFiredEvent(cwd, sessionId, result);
}

/**
 * v2.0.0-rc.7 T5: best-effort sync stdin reader for the Stop hook.
 *
 * Claude Code passes a JSON payload via stdin on Stop hook fire (session_id,
 * transcript_path, hook_event_name, etc.). We try to read it synchronously so
 * we can derive a session digest. On any failure (closed stdin, non-TTY where
 * fd 0 is not readable, parse error, foreign client) we degrade silently.
 *
 * Returns the parsed JSON object on success, or null on any error. NEVER
 * throws.
 */
function tryReadStdinJson() {
  return stopStdin.tryReadStdinJson();
}

/**
 * v2.0.0-rc.20 TASK-03 → v2.0.0-rc.24 TASK-04: legacy shim signature for
 * parsing the raw text that follows the `KB:` prefix on the first non-empty
 * line of an assistant turn. As of rc.24 the implementation delegates to the
 * shared `parseCiteLine` (inline-shipped via lib/cite-line-parser.cjs) to
 * eliminate per-client regex drift.
 *
 * Contract (rc.24 strict mode — superset of rc.20):
 *   - Sentinel `none` (incl. `[no-relevant]` / `[not-applicable]` tail)
 *     → cite_ids=[], cite_tags=["none"], cite_commitments=[]
 *   - `KT-DEC-0001 [planned]` → cite_ids=["KT-DEC-0001"], cite_tags=["planned"],
 *     cite_commitments=[{operators:[], skip_reason:null}]
 *   - `KT-DEC-0001 [recalled] → edit:foo.ts` → cite_commitments=[{operators:
 *     [{kind:"edit", target:"foo.ts"}], skip_reason:null}]
 *   - `KT-DEC-0001 [recalled] → skip:sequencing` → cite_commitments=[{operators:
 *     [], skip_reason:"sequencing"}]
 *   - Id form is now strict `K[TP]-[A-Z]+-\d+` (rc.20 lax form `KP-001`
 *     without letter-prefix is rejected — see TASK-03 schema).
 *
 * Argument is the post-`KB:` substring (matches the rc.20 call site). Returns
 * { cite_ids, cite_tags, cite_commitments }; cite_commitments was added in
 * rc.24 and is always present (empty array when no cite-line found).
 *
 * Never throws.
 */

/**
 * v2.0.0-rc.20 TASK-03: detect which client surface invoked the hook so the
 * emitted assistant_turn_observed event can carry a `client` discriminator
 * without having to inspect the transcript shape.
 *
 * Resolution order (first match wins):
 *   1. `FABRIC_HINT_CLIENT` env var — explicit override, set by the per-
 *      client install pipeline when the hook-config schema supports env
 *      injection.
 *   2. Path heuristic against `__dirname` — `.claude/` → "cc", `.codex/` →
 *      "codex". Covers the dominant deployment shape (hook script lives
 *      under the client's per-repo dir).
 *
 * Returns `undefined` when neither signal fires (a custom deployment). The
 * Zod schema marks `client` optional, so omitting it leaves the event valid.
 */

/**
 * v2.0.0-rc.20 TASK-03: emit one `assistant_turn_observed` event per
 * assistant envelope harvested from the transcript. Wrapped in try/catch
 * (best-effort, never throws — Stop hook MUST stay non-blocking on any
 * failure here). The event shape mirrors
 * assistantTurnObservedEventSchema in
 * packages/shared/src/schemas/event-ledger.ts (registered in rc.20 TASK-02).
 *
 * Call site sits immediately AFTER writeSessionDigestBestEffort so both
 * digest + per-turn events derive from the same transcript snapshot.
 *
 * `id` mirrors the server's convention (`event:<uuid>`) using
 * crypto.randomUUID when available — falls back to a timestamp+counter
 * tuple on older Node where randomUUID is missing (cjs hook tooling
 * defensively targets Node 18+, but the fallback keeps it event-shaped).
 */

/**
 * v2.0.0-rc.7 T5: writeSessionDigestBestEffort — non-blocking digest fan-out.
 * Called from main() before the existing decide() flow. Failure is silently
 * swallowed; the Stop hook contract remains "never block on hook failure".
 */
function writeSessionDigestBestEffort(projectRoot, stdinPayload) {
  return stopStdin.writeSessionDigestBestEffort(projectRoot, stdinPayload);
}

// ux-w0-3 (KT-DEC-0007): the SINGLE soft-emit path for EVERY Stop-hook signal
// (archive / archive_backlog / review / import / maintenance). A nudge is a
// reminder layer, NEVER a gate — so no signal ever emits a blocking decision.
// The AI channel always carries the reason (flow ⊥ observation, D3); the human
// systemMessage is gated by nudge_mode (D4/D5), with high-value signals
// (knowledge-loss: archive / archive_backlog) surfacing at lower volumes.
// Mutates `result` (strips telemetry-only threshold/actual_value, like the prior
// inline paths). When the client adapter is unavailable, falls back to a plain
// non-blocking JSON payload (decision stays "soft", never blocking).
function emitSoftSignal(out, result, cwd, highValue) {
  return softSignalEmit.emitSoftSignal(out, result, cwd, highValue);
}

// High-value (knowledge-loss) signals surface at lower nudge_mode volumes.
const HIGH_VALUE_SIGNALS = softSignalEmit.HIGH_VALUE_SIGNALS;

// TASK-005 (grill G5 / C-003): the ONLY signals allowed to emit a nudge on the
// Stop hook. C-003 LOCKS the archive family here (趁热归档 value dies if moved to
// SessionStart). review / import / maintenance moved to the SessionStart summary
// line (knowledge-hint-broad.cjs) and are silent on Stop.
const STOP_EMIT_SIGNALS = softSignalEmit.STOP_EMIT_SIGNALS;

/**
 * Main entry — invoked both as a CLI (require.main === module) and in-process by tests.
 *
 * Wraps the entire flow in try/catch: ANY error → silent exit 0. The hook MUST NEVER
 * block tool execution on its own failure (per existing fabric-*-reminder.cjs precedent).
 */
function main(env, stdio) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const now = (env && env.now) || new Date();
    const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
    const out = (stdio && stdio.stdout) || process.stdout;

    // v2.0.0-rc.7 T5: session-digest write (best-effort). Tests can inject
    // a pre-parsed stdin payload via env.stdin_payload so the digest path
    // is exercised without needing a real stdin pipe.
    const stdinPayload =
      (env && env.stdin_payload) !== undefined
        ? env.stdin_payload
        : tryReadStdinJson();
    writeSessionDigestBestEffort(cwd, stdinPayload);
    // v2.0.0-rc.20 TASK-03: per-turn cite-policy observation events. Same
    // best-effort contract as the digest writer — never throws, never blocks
    // the Stop hook on failure. Shares the transcript snapshot read by
    // writeSessionDigestBestEffort (each call re-reads independently; the
    // transcript file is small in practice and re-parse cost is dwarfed by
    // the hook's other I/O).
    extractAndWriteAssistantTurnsBestEffort(cwd, stdinPayload);

    const events = readLedger(cwd);

    // lifecycle-refactor W3-A2 (§7): request graph-edge extraction for a freshly
    // archived canonical entry. Runs UNCONDITIONALLY here (before the nudge
    // cooldown/dismiss early-returns) so the §7 signal is independent of whether
    // a reminder banner is shown this Stop. Best-effort, never throws.
    try {
      emitGraphEdgeCandidateBestEffort(cwd, events, resolveHookSessionId(stdinPayload));
    } catch {
      // never block the Stop hook
    }

    let pendingStats;
    try {
      pendingStats = readPendingStats(cwd, now);
    } catch {
      // Defensive — readPendingStats already silences ENOENT/stat errors,
      // but a defense-in-depth try/catch keeps the never-block invariant.
      pendingStats = { count: 0, oldestAgeMs: null };
    }
    let underseedStats;
    try {
      underseedStats = {
        nodeCount: countCanonicalNodes(cwd),
        threshold: readUnderseedThreshold(cwd),
      };
    } catch {
      underseedStats = { nodeCount: 0, threshold: DEFAULT_UNDERSEED_NODE_THRESHOLD };
    }

    // Edit-counter view (rc.6 TASK-022 / E5). We need the last knowledge_proposed
    // ts to anchor the count; rather than rescanning events here, we mirror
    // decide()'s scan locally to keep the helper pure. The threshold comes
    // from fabric-config.json (archive_edit_threshold, default 20).
    // crack 1: per-session edit view. anchor = THIS session's own last archive
    // watermark (session_archive_attempted.covered_through_ts) else its first
    // ledger activity; count = THIS session's file_mutated events since anchor.
    // Reads the event ledger, NOT the session-blind edit-counter sidecar.
    let editCounterStats;
    try {
      const sid = resolveHookSessionId(stdinPayload);
      const anchorTs = sessionAnchorTs(events, sid);
      editCounterStats = {
        editsSinceArchive: countSessionMutationsSince(events, sid, anchorTs),
        threshold: readArchiveEditThreshold(cwd),
        anchorPresent: anchorTs !== null,
      };
    } catch {
      editCounterStats = {
        editsSinceArchive: 0,
        threshold: DEFAULT_ARCHIVE_EDIT_THRESHOLD,
        anchorPresent: false,
      };
    }

    // crack 2: cross-session backlog view — count DEAD sessions (other than the
    // current one) carrying unarchived high-value work. Drives the
    // archive_backlog signal that replaces the retired global-24h timer.
    let backlogStats;
    try {
      const sid = resolveHookSessionId(stdinPayload);
      backlogStats = {
        deadSessionCount: countBacklogSessions(events, nowMs, sid, readArchiveBacklogIdleHours(cwd)),
        threshold: readArchiveBacklogSessionCount(cwd),
      };
    } catch {
      backlogStats = {
        deadSessionCount: 0,
        threshold: DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT,
      };
    }

    // rc.7 T7 / ISS-20260713-052: threshold bag assembled in lib/hint-thresholds.cjs
    const thresholds = hintThresholds.buildStopThresholds(cwd);
    const variant = thresholds.variant;

    // rc.7 T4: build the 人-first banner activity overview from the
    // edit-counter sidecar. Anchored at the last knowledge_proposed event
    // so the overview matches Signal A's "since last archive" semantics.
    // Failure (missing sidecar, malformed lines, etc.) degrades silently
    // to an empty string — the banner just omits the activity line.
    let activityOverview = "";
    try {
      let anchorTs = null;
      for (let i = events.length - 1; i >= 0; i -= 1) {
        const ev = events[i];
        if (ev && ev.event_type === EVENT_TYPE_PROPOSED && typeof ev.ts === "number") {
          anchorTs = ev.ts;
          break;
        }
      }
      activityOverview = formatActivityOverview(cwd, anchorTs);
    } catch {
      activityOverview = "";
    }

    // v2.0.0-rc.8 (TASK-002): probe `.fabric/.import-state.json` to
    // determine whether a fabric-import skill run is currently in flight.
    // Threaded into decide() so Signal B (review hint) is suppressed for
    // the duration of an active import — preventing the Stop hook from
    // interrupting the import when its pending pile crosses the review
    // threshold. See isImportInFlight() docstring for the full truth table.
    let importInFlight = false;
    try {
      importInFlight = isImportInFlight(cwd, now);
    } catch {
      importInFlight = false;
    }

    let result = decide(
      events,
      now,
      pendingStats,
      underseedStats,
      editCounterStats,
      thresholds,
      { activityOverview },
      importInFlight,
      backlogStats,
    );

    // v2.0.0-rc.7 T10: Signal D — maintenance hint. Evaluated AFTER A/B/C
    // because the existing three signals carry higher urgency (in-flight
    // archive backlog > review backlog > sparse corpus > stale lint). The
    // maintenance prompt only surfaces when none of the in-flight signals
    // fire and the corpus has had time to accumulate enough lint surface
    // for the prompt to be actionable.
    if (result === null) {
      try {
        const lastEmit = readMaintenanceLastEmit(cwd, resolveHookSessionId(stdinPayload));
        result = evaluateMaintenanceSignal(
          events,
          now,
          underseedStats.nodeCount,
          lastEmit,
          thresholds,
        );
      } catch {
        result = null;
      }
    }

    // TASK-005 (grill G5 / C-003 + C-004): four-signal split. Only the archive
    // family (archive / archive_backlog) stays on Stop — its value is 趁热归档,
    // so C-003 LOCKS it here and forbids moving it to SessionStart. The other
    // three signals (review / import / maintenance) are NON-immediate backlog
    // reminders; per-Stop repetition was the dominant noise source, so they are
    // now surfaced ONCE at SessionStart as a summary line (see
    // knowledge-hint-broad.cjs buildSessionStartSinks). On Stop they are
    // treated exactly like "no actionable signal": we fall through to the
    // session-activity breadcrumb path below rather than emitting a nudge.
    // decide()/evaluateMaintenanceSignal() keep returning the full B/C/D
    // objects (their pure contracts + unit tests are unchanged); this is the
    // sole Stop-emission allowlist.
    if (result !== null && !STOP_EMIT_SIGNALS.has(result.signal)) {
      result = null;
    }

    // TASK-005 (grill G5 / C-003): downgrade the archive nudge to a SINGLE terse
    // line. The multi-line archiveLine1/Activity/Cta banner was per-Stop noise;
    // collapse to one `archiveSingle` line that still carries the edit-count
    // fragment + the `/fabric-archive` CTA token. archive_backlog is already a
    // two-liner and stays as-is (cross-session sweeps are rarer / higher-value).
    if (result !== null && result.signal === "archive") {
      const parts = renderBanner("archivePartsEdits", variant, {
        count: editCounterStats.editsSinceArchive,
        threshold: editCounterStats.threshold,
      });
      result.reason = renderBanner("archiveSingle", variant, { parts });
    }

    if (result === null) {
      // Observability grill (a): no actionable signal — instead of returning
      // silently (which made Fabric feel inert in the background), surface a
      // session-activity status breadcrumb to the human sink (gated by
      // nudge_mode). Best-effort: never block the Stop hook on it.
      try {
        emitSessionStatus(cwd, events, stdinPayload, nowMs, pendingStats, out);
      } catch {
        // status breadcrumb is decorative — never throw
      }
      return;
    }

    // v2.2 dual-sink (Goal A / D6): VALUE-GATE the in-session archive nudge. The
    // edit trigger is the CHECK cadence; the nudge only fires when a high-value
    // signal accrued since the last archive (decouples check from disturb).
    // crack 1: re-anchored PER SESSION (watermark = this session's own anchor,
    // probe scoped to this session) so a neighbour window's high-value work past
    // the same global watermark can't keep — or suppress — THIS window's nudge.
    // archive_backlog already incorporates high-value in its count, so it is not
    // re-gated here. Other signals (review/import/maintenance) are unaffected.
    if (result.signal === "archive") {
      const sid = resolveHookSessionId(stdinPayload);
      // ISS-20260713-043: value-gate must use sessionArchiveWatermark (null when
      // never archived → treated as 0), NOT sessionAnchorTs. sessionAnchorTs falls
      // back to first-activity ts, and hasHighValueArchiveSignal uses strict `>`;
      // that wrongly swallows a never-archived session whose first event is already
      // high-value. Backlog path (countBacklogSessions) already uses watermark.
      const watermarkTs = sessionArchiveWatermark(events, sid);
      if (!hasHighValueArchiveSignal(events, watermarkTs, sid)) {
        // ISS-20260713-050: value-gate suppress must NOT bare-return. Fall through
        // to the no-signal path so emitSessionStatus can still surface a human
        // trust-anchor (when nudge_mode allows). Archive CTA stays suppressed.
        result = null;
      }
    }

    if (result === null) {
      try {
        emitSessionStatus(cwd, events, stdinPayload, nowMs, pendingStats, out);
      } catch {
        // status breadcrumb is decorative — never throw
      }
      return;
    }

    // v2.0.0-rc.37 NEW-16: per-signal dismiss. A chosen signal whose type the
    // user dismissed (config-durable or session sidecar) exits silently —
    // same shape as a cooldown hit. Covers BOTH maintenance and A/B/C paths.
    const sessionId =
      stdinPayload && typeof stdinPayload.session_id === "string"
        ? stdinPayload.session_id
        : null;
    if (readDismissedSignals(cwd, sessionId).has(result.signal)) {
      return;
    }
    // Append the bilingual dismiss-option line so the lever is discoverable.
    if (typeof result.reason === "string") {
      result.reason = `${result.reason}\n${renderDismissOption(result.signal, variant)}`;
    }

    // v2.1.0-rc.1 P4 (F4/S63): surface the read-set stores on the Stop hint so
    // backlog/maintenance nudges are read per-store, not as one undifferentiated
    // pile. Best-effort; missing snapshot / single-store omits the line.
    if (bindingsSnapshotReader !== null && typeof result.reason === "string") {
      try {
        const bindingId = readWorkspaceBindingId(cwd);
        if (bindingId) {
          const label = bindingsSnapshotReader.formatStoreLabels(
            bindingsSnapshotReader.readBindingsSnapshot(bindingId),
          );
          if (label) {
            result.reason = `${result.reason}\n${label}`;
          }
        }
      } catch {
        // store label is decorative provenance — never crash the hook
      }
    }

    // TASK-005 (grill G5 / C-003): the maintenance (Signal D) Stop-emit branch
    // — with its day-based MAINTENANCE_HINT_LAST_EMIT_FILE cooldown — is retired.
    // Maintenance moved to the SessionStart summary line, so it can no longer
    // reach here (STOP_EMIT_SIGNALS nulls it above). Only the archive family
    // flows through, and it uses the hour-based shared cooldown cache below.

    // Cooldown throttle: once a signal fires, stay silent for
    // archive_hint_cooldown_hours (default 12h) regardless of state drift.
    // Pure reminder-noise reduction; the underlying trigger logic is unchanged.
    const cooldownMs = readCooldownHours(cwd) * MS_PER_HOUR;
    const cache = readShownCache(cwd, resolveHookSessionId(stdinPayload));
    const lastShown = cache[result.signal];
    // rc.34 TASK-01 + review-fix (Gemini P1): future-stamped lastShown
    // (backward clock skew) bypasses cooldown — sidecar treated as expired.
    if (
      typeof lastShown === "number" &&
      nowMs >= lastShown &&
      nowMs - lastShown < cooldownMs
    ) {
      return; // Still in cooldown — silent.
    }

    emitSignalFiredEvent(cwd, sessionId, result);
    // ux-w0-3 (KT-DEC-0007): EVERY A/B/C signal (archive / archive_backlog /
    // review / import) emits SOFT via the shared path — additionalContext(AI) +
    // nudge_mode-gated systemMessage(human), NEVER decision:block. Previously
    // only `archive` was soft and review/import blocked; the block contract is
    // retired (a nudge is a reminder layer, never a gate).
    emitSoftSignal(out, result, cwd, HIGH_VALUE_SIGNALS.has(result.signal));
    cache[result.signal] = nowMs;
    writeShownCache(cwd, cache, resolveHookSessionId(stdinPayload));
  } catch {
    // Silent — never block on hook failure.
  }
}


function _readConfigNumber(projectRoot, fieldName, defaultValue) {
  return hintConfig._readConfigNumber(projectRoot, fieldName, defaultValue);
}
function readArchiveHintHours(projectRoot) { return hintConfig.readArchiveHintHours(projectRoot); }
function readReviewHintPendingCount(projectRoot) { return hintConfig.readReviewHintPendingCount(projectRoot); }
function readReviewHintPendingAgeDays(projectRoot) { return hintConfig.readReviewHintPendingAgeDays(projectRoot); }
function readMaintenanceHintDays(projectRoot) { return hintConfig.readMaintenanceHintDays(projectRoot); }
function readMaintenanceHintCooldownDays(projectRoot) { return hintConfig.readMaintenanceHintCooldownDays(projectRoot); }
function readArchiveBacklogSessionCount(projectRoot) { return hintConfig.readArchiveBacklogSessionCount(projectRoot); }
function readArchiveBacklogIdleHours(projectRoot) { return hintConfig.readArchiveBacklogIdleHours(projectRoot); }
function readCooldownHours(projectRoot) { return hintConfig.readCooldownHours(projectRoot); }
function readUnderseedThreshold(projectRoot) { return hintConfig.readUnderseedThreshold(projectRoot); }
function readArchiveEditThreshold(projectRoot) { return hintConfig.readArchiveEditThreshold(projectRoot); }


const DISMISSABLE_SIGNALS = sessionSignalState.DISMISSABLE_SIGNALS;
function sessionScopedCacheFile(baseRelPath, sessionId) {
  return sessionSignalState.sessionScopedCacheFile(baseRelPath, sessionId);
}
function readShownCache(projectRoot, sessionId) {
  return sessionSignalState.readShownCache(projectRoot, sessionId);
}
function writeShownCache(projectRoot, cache, sessionId) {
  return sessionSignalState.writeShownCache(projectRoot, cache, sessionId);
}
function sessionDismissFileName(sessionId) {
  return sessionSignalState.sessionDismissFileName(sessionId);
}
function readDismissedSignals(projectRoot, sessionId) {
  return sessionSignalState.readDismissedSignals(projectRoot, sessionId);
}
function writeSessionDismiss(projectRoot, sessionId, signals) {
  return sessionSignalState.writeSessionDismiss(projectRoot, sessionId, signals);
}
function renderDismissOption(signal, variant) {
  return sessionSignalState.renderDismissOption(signal, variant);
}
function readMaintenanceLastEmit(projectRoot, sessionId) {
  return sessionSignalState.readMaintenanceLastEmit(projectRoot, sessionId);
}
function writeMaintenanceLastEmit(projectRoot, nowMs, sessionId) {
  return sessionSignalState.writeMaintenanceLastEmit(projectRoot, nowMs, sessionId);
}


function parseKbLine(raw) {
  return assistantTurnEmit.parseKbLine(raw);
}
function detectClient() {
  return assistantTurnEmit.detectClient();
}
function extractAndWriteAssistantTurnsBestEffort(cwd, stdinPayload) {
  return assistantTurnEmit.extractAndWriteAssistantTurnsBestEffort(cwd, stdinPayload);
}


module.exports = {
  main,
  readLedger,
  readPendingStats,
  countCanonicalNodes,
  countEditsSince,
  // observability grill (a): session-activity tally for the human status line.
  tallySessionActivity,
  // rc.7 T4: top-edited-directories aggregator + banner overview formatter.
  getTopEditedDirectories,
  formatActivityOverview,
  // v2.0.0-rc.8 (TASK-002): in-flight import gate for Signal B (exported
  // for unit testing of the truth table).
  isImportInFlight,
  decide,
  // crack 1 + 2: two-lane archive strategy helpers (exported for unit testing).
  hasHighValueArchiveSignal,
  sessionArchiveWatermark,
  sessionFirstActivityTs,
  sessionAnchorTs,
  countSessionMutationsSince,
  countBacklogSessions,
  readArchiveBacklogSessionCount,
  readArchiveBacklogIdleHours,
  readCooldownHours,
  readUnderseedThreshold,
  readArchiveEditThreshold,
  // v2.0.0-rc.37 NEW-16: per-signal dismiss helpers (exported for tests +
  // the agent-driven session-dismiss write path).
  readDismissedSignals,
  writeSessionDismiss,
  sessionDismissFileName,
  renderDismissOption,
  DISMISSABLE_SIGNALS,
  // v2.0.0-rc.7 T5: session digest helpers (exported for unit testing).
  tryReadStdinJson,
  summarizeTranscript,
  writeSessionDigestBestEffort,
  // v2.0.0-rc.7 T10: Signal D helpers (exported for unit testing).
  evaluateMaintenanceSignal,
  findLastDoctorRunTs,
  readMaintenanceLastEmit,
  writeMaintenanceLastEmit,
  // rc.7 T7: externalized-threshold readers (3 moved + 2 new for T10).
  readArchiveHintHours,
  readReviewHintPendingCount,
  readReviewHintPendingAgeDays,
  readMaintenanceHintDays,
  readMaintenanceHintCooldownDays,
  readShownCache,
  writeShownCache,
  // v2.0.0-rc.20 TASK-03 / TASK-09: cite-policy parsing + per-turn emission
  // helpers (exported for unit testing of the parse + emit contract).
  parseKbLine,
  detectClient,
  extractAndWriteAssistantTurnsBestEffort,
  // lifecycle-refactor W3-A2 (§7): graph-edge-candidate request emitter
  // (exported for unit testing of the honest stable_id-gating + de-dup).
  emitGraphEdgeCandidateBestEffort,
  CONSTANTS: {
    FABRIC_DIR,
    EVENT_LEDGER_FILE,
    METRICS_LEDGER_FILE,
    EVENT_TYPE_ASSISTANT_TURN_OBSERVED,
    EVENT_TYPE_PROPOSED,
    EVENT_TYPE_INIT_SCAN_COMPLETED,
    // rc.7 T7: legacy aliases kept for back-compat with the existing test
    // CONSTANTS surface. They point at the same documented defaults the
    // readers return when the config file is absent — never branch on these
    // in production code, always go through the readers so a config
    // override is honored.
    THRESHOLD_HOURS: DEFAULT_ARCHIVE_HINT_HOURS,
    THRESHOLD_PENDING_COUNT: DEFAULT_REVIEW_HINT_PENDING_COUNT,
    THRESHOLD_PENDING_AGE_DAYS: DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS,
    DEFAULT_ARCHIVE_HINT_HOURS,
    DEFAULT_REVIEW_HINT_PENDING_COUNT,
    DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS,
    DEFAULT_MAINTENANCE_HINT_DAYS,
    DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS,
    PENDING_DIR,
    PENDING_TYPES,
    KNOWLEDGE_CANONICAL_TYPES,
    DEFAULT_UNDERSEED_NODE_THRESHOLD,
    UNDERSEED_POST_INIT_QUIET_HOURS,
    UNDERSEED_NO_PROPOSED_HOURS,
    CONFIG_FILE,
    DEFAULT_COOLDOWN_HOURS,
    SHOWN_CACHE_FILE,
    EDIT_COUNTER_FILE_REL,
    DEFAULT_ARCHIVE_EDIT_THRESHOLD,
    EVENT_TYPE_DOCTOR_RUN,
    MAINTENANCE_HINT_LAST_EMIT_FILE,
    MAINTENANCE_HINT_MIN_CANONICAL,
    // v2.0.0-rc.8 (TASK-002): in-flight import gate for Signal B.
    IMPORT_STATE_FILE_REL,
    IMPORT_IN_FLIGHT_MAX_AGE_HOURS,
    // lifecycle-refactor W3-A2 (§7): graph-edge-request de-dup sidecar.
    GRAPH_EDGE_REQUESTED_SIDECAR,
  },
};

if (require.main === module) {
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot, now: new Date() }, { stdout: process.stdout });
  process.exit(0);
}
