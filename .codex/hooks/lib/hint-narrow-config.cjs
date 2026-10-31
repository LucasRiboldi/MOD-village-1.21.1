// ISS-20260713-053: narrow hint config readers.
//
// config-single-home W7: these numbers are no longer individually configurable.
// `hint_narrow_top_k` / `hint_narrow_cooldown_hours` / `hint_summary_max_len`
// now come from the nudge_mode preset (lib/nudge-policy.cjs NUDGE_PRESETS), and
// the dedup window / AI-sink switch are fixed. Rationale: a user asking for a
// quieter experience picks a volume, not six numbers — and the six were never
// individually tuned in practice. The reader signatures are unchanged so every
// call site and telemetry field stays put; only where the value comes from moved.

const FABRIC_DIR_REL = ".fabric";
const FABRIC_CONFIG_FILE = "fabric-config.json";
// Per-file dedup window (in PreToolUse turns) for the narrow hint. Fixed at 5:
// it exists to stop one hot file re-firing the same hint until the agent learns
// to ignore it — a correctness guard on the hint's usefulness, not a taste dial.
const DEFAULT_HINT_NARROW_DEDUP_WINDOW_TURNS = 5;
// The AI sink is unconditional (D5 flow ⊥ observation): nudge_mode governs the
// HUMAN channel only, and knowledge that never reaches the model is Fabric not
// working. Kept as a named constant because telemetry records the channel.
const DEFAULT_HINT_REMINDER_TO_CONTEXT = true;
const { readConfig, readPolicy } = require("./config-cache.cjs");
const { resolveNudgePreset } = require("./nudge-policy.cjs");

function _readNarrowConfigValue(projectRoot) {
  const parsed = readConfig(projectRoot);
  return Object.keys(parsed).length > 0 ? parsed : null;
}

function readNarrowTopK(projectRoot) {
  return resolveNudgePreset(projectRoot).narrowTopK;
}

function readNarrowDedupWindowTurns(_projectRoot) {
  return DEFAULT_HINT_NARROW_DEDUP_WINDOW_TURNS;
}

function readNarrowCooldownHours(projectRoot) {
  return resolveNudgePreset(projectRoot).narrowCooldownHours;
}

// config-single-home W3: preference class — the first policy layer that declares
// the list wins (projects[<project_id>] then defaults). An empty array is a
// MEANINGFUL value ("dismiss nothing"), so presence is what decides, not length.
function readNarrowDismissed(projectRoot) {
  for (const layer of readPolicy(projectRoot)) {
    if (Array.isArray(layer.hint_dismiss_signals)) {
      return layer.hint_dismiss_signals.includes("narrow");
    }
  }
  return false;
}

function readReminderToContext(_projectRoot) {
  return DEFAULT_HINT_REMINDER_TO_CONTEXT;
}

function readSummaryMaxLen(projectRoot) {
  return resolveNudgePreset(projectRoot).summaryMaxLen;
}

module.exports = {
  FABRIC_DIR_REL,
  FABRIC_CONFIG_FILE,
  DEFAULT_HINT_NARROW_DEDUP_WINDOW_TURNS,
  DEFAULT_HINT_REMINDER_TO_CONTEXT,
  _readNarrowConfigValue,
  readNarrowTopK,
  readNarrowDedupWindowTurns,
  readNarrowCooldownHours,
  readNarrowDismissed,
  readReminderToContext,
  readSummaryMaxLen,
};
