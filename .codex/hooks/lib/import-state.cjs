// ISS-20260713-020: fabric-import in-flight gate for Signal B (review hint).

const { existsSync, readFileSync } = require("node:fs");
const { join } = require("node:path");

const IMPORT_STATE_FILE_REL = join(".fabric", ".import-state.json");
const IMPORT_IN_FLIGHT_MAX_AGE_HOURS = 24;
const MS_PER_HOUR = 60 * 60 * 1000;

/**
 * Detect whether a fabric-import skill run is currently in flight.
 * NEVER throws — never-block invariant.
 */
function isImportInFlight(projectRoot, now) {
  try {
    const p = join(projectRoot, IMPORT_STATE_FILE_REL);
    if (!existsSync(p)) return false;
    let raw;
    try {
      raw = readFileSync(p, "utf8");
    } catch {
      return false;
    }
    let parsed;
    try {
      parsed = JSON.parse(raw);
    } catch {
      return false;
    }
    if (parsed === null || typeof parsed !== "object") return false;
    if (parsed.phase === "complete") return false;
    const ts = parsed.last_checkpoint_at;
    if (typeof ts !== "string" || ts.length === 0) return false;
    const ms = Date.parse(ts);
    if (!Number.isFinite(ms)) return false;
    const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
    const ageHours = (nowMs - ms) / MS_PER_HOUR;
    if (ageHours > IMPORT_IN_FLIGHT_MAX_AGE_HOURS) return false;
    return true;
  } catch {
    return false;
  }
}

module.exports = {
  isImportInFlight,
  IMPORT_STATE_FILE_REL,
  IMPORT_IN_FLIGHT_MAX_AGE_HOURS,
};
