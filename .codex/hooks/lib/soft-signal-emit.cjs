// ISS-20260713-040 residual: soft-nudge emit + hook_signal_emitted telemetry.
// Extracted from fabric-hint.cjs (KT-DEC-0007 soft-only path).
const { existsSync } = require("node:fs");
const { join } = require("node:path");
const { appendEvent } = require("./event-writer.cjs");
const hintConfig = require("./hint-config.cjs");

const FABRIC_DIR = hintConfig.FABRIC_DIR;

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

const SIGNAL_TYPE_ENUM = new Set(["archive", "archive_backlog", "review", "maintenance", "other"]);
// High-value (knowledge-loss) signals surface at lower nudge_mode volumes.
const HIGH_VALUE_SIGNALS = new Set(["archive", "archive_backlog"]);
// ONLY signals allowed to emit a nudge on Stop (C-003 archive family).
const STOP_EMIT_SIGNALS = new Set(["archive", "archive_backlog"]);

/**
 * Soft-emit path for every Stop-hook signal. Mutates `result` (strips
 * threshold/actual_value). Never blocking (KT-DEC-0007).
 */
function emitSoftSignal(out, result, cwd, highValue) {
  const reasonText = typeof result.reason === "string" ? result.reason : "";
  delete result.threshold;
  delete result.actual_value;
  const client =
    clientAdapter && typeof clientAdapter.detectClient === "function"
      ? clientAdapter.detectClient(__dirname)
      : undefined;
  if (client && clientAdapter && typeof clientAdapter.emitDualSink === "function") {
    const humanGate =
      nudgePolicy !== null
        ? nudgePolicy.resolveHumanSink(cwd, "stop", { highValue })
        : { emitHuman: true };
    clientAdapter.emitDualSink(
      { human: humanGate.emitHuman ? reasonText : null, ai: reasonText },
      { client, eventName: "Stop", streams: { stdout: out } },
    );
    return;
  }
  out.write(JSON.stringify(result));
}

/**
 * Best-effort hook_signal_emitted ledger row at actual delivery time.
 * Never blocks the hook.
 */
function emitSignalFiredEvent(cwd, sessionId, result) {
  try {
    if (!result || typeof result.signal !== "string") return;
    const threshold = result.threshold;
    const actualValue = result.actual_value;
    if (
      typeof threshold !== "number" ||
      !Number.isFinite(threshold) ||
      typeof actualValue !== "number" ||
      !Number.isFinite(actualValue)
    ) {
      return;
    }
    const fabricDir = join(cwd, FABRIC_DIR);
    if (!existsSync(fabricDir)) return;
    const signalType = SIGNAL_TYPE_ENUM.has(result.signal) ? result.signal : "other";
    let idSuffix;
    try {
      idSuffix = require("node:crypto").randomUUID();
    } catch {
      idSuffix = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
    }
    const event = {
      kind: "fabric-event",
      id: `event:${idSuffix}`,
      ts: Date.now(),
      schema_version: 1,
      event_type: "hook_signal_emitted",
      signal_type: signalType,
      threshold,
      actual_value: actualValue,
      fired: true,
    };
    if (typeof sessionId === "string" && sessionId.length > 0) event.session_id = sessionId;
    appendEvent(fabricDir, event);
  } catch {
    // best-effort telemetry
  }
}

module.exports = {
  SIGNAL_TYPE_ENUM,
  HIGH_VALUE_SIGNALS,
  STOP_EMIT_SIGNALS,
  emitSoftSignal,
  emitSignalFiredEvent,
};
