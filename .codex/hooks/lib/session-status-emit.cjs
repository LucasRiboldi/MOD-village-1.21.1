// ISS-20260713-040 residual: human-only session status breadcrumb + activity overview.
// Extracted from fabric-hint.cjs.
const { renderBanner, readFabricLanguage } = require("./banner-i18n.cjs");
const sessionSignalState = require("./session-signal-state.cjs");
const ledgerScan = require("./ledger-scan.cjs");
const { resolveHookSessionId } = require("./stop-stdin.cjs");

let clientAdapter = null;
try {
  clientAdapter = require("./client-adapter.cjs");
} catch {
  clientAdapter = null;
}

let nudgePolicy = null;
try {
  nudgePolicy = require("./nudge-policy.cjs");
} catch {
  nudgePolicy = null;
}

/**
 * Human-facing session status breadcrumb when no actionable signal fired.
 * Human sink ONLY. Never throws.
 */
function emitSessionStatus(cwd, events, stdinPayload, nowMs, pendingStats, out) {
  if (nudgePolicy === null || clientAdapter === null) return;
  if (typeof clientAdapter.emitDualSink !== "function") return;
  const sessionId = resolveHookSessionId(stdinPayload);
  if (typeof sessionId !== "string" || sessionId.length === 0) return;

  const mode =
    typeof nudgePolicy.readNudgeMode === "function" ? nudgePolicy.readNudgeMode(cwd) : "normal";
  if (mode === "silent") return;

  const tally = ledgerScan.tallySessionActivity(events, sessionId);
  const pending =
    pendingStats && typeof pendingStats.count === "number"
      ? pendingStats.count
      : pendingStats && typeof pendingStats.total === "number"
        ? pendingStats.total
        : 0;
  if (tally.edits === 0 && tally.consumed === 0 && pending === 0) return;

  const cache = sessionSignalState.readShownCache(cwd, sessionId);
  const firstThisSession = cache._status === undefined;
  if (mode !== "verbose" && !firstThisSession) return;

  const variant = readFabricLanguage(cwd);
  const line1 = renderBanner("statusLine", variant, {
    edits: tally.edits,
    consumed: tally.consumed,
    pending,
  });
  const human = firstThisSession
    ? `${line1}\n${renderBanner("statusTier", variant, { mode })}`
    : line1;

  clientAdapter.emitDualSink(
    { human, ai: null },
    { client: clientAdapter.detectClient(__dirname), eventName: "Stop", streams: { stdout: out } },
  );

  cache._status = nowMs;
  sessionSignalState.writeShownCache(cwd, cache, sessionId);
}

/**
 * Format "dir1 (N edits), dir2 (M edits)" for Signal A banner.
 * Empty string when no aggregable activity.
 */
function formatActivityOverview(projectRoot, anchorTs) {
  const top = ledgerScan.getTopEditedDirectories(projectRoot, 3, anchorTs);
  if (top.length === 0) return "";
  return top.map((e) => `${e.dir} (${e.count} edits)`).join(", ");
}

module.exports = {
  emitSessionStatus,
  formatActivityOverview,
};
