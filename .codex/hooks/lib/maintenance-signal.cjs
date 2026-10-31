// ISS-20260713-040 residual: Signal D (maintenance) evaluate + doctor_run scan.
// Extracted from fabric-hint.cjs so the orchestrator stays thin.
const { renderBanner } = require("./banner-i18n.cjs");
const hintConfig = require("./hint-config.cjs");

const {
  DEFAULT_MAINTENANCE_HINT_DAYS,
  DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS,
  MS_PER_DAY,
  MAINTENANCE_HINT_MIN_CANONICAL,
} = hintConfig;

const EVENT_TYPE_DOCTOR_RUN = "doctor_run";

/**
 * Find the most recent doctor_run event ts in the ledger.
 * Returns epoch ms of the newest doctor_run, or null if none.
 */
function findLastDoctorRunTs(events) {
  if (!Array.isArray(events)) return null;
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const ev = events[i];
    if (ev && ev.event_type === EVENT_TYPE_DOCTOR_RUN && typeof ev.ts === "number") {
      return ev.ts;
    }
  }
  return null;
}

/**
 * Signal D — maintenance hint.
 *
 * Trigger when ALL hold:
 *   1. No doctor_run in the last maintenance_hint_days (or never)
 *   2. Canonical node count >= MAINTENANCE_HINT_MIN_CANONICAL
 *   3. Not within maintenance_hint_cooldown_days of previous emit
 *
 * Returns soft decision object or null. Never throws.
 */
function evaluateMaintenanceSignal(events, now, canonicalCount, lastEmitMs, thresholds) {
  const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
  const cfg = thresholds || {};
  const days =
    typeof cfg.maintenanceHintDays === "number" && cfg.maintenanceHintDays > 0
      ? cfg.maintenanceHintDays
      : DEFAULT_MAINTENANCE_HINT_DAYS;
  const cooldownDays =
    typeof cfg.maintenanceHintCooldownDays === "number" && cfg.maintenanceHintCooldownDays > 0
      ? cfg.maintenanceHintCooldownDays
      : DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS;
  const variant = typeof cfg.variant === "string" ? cfg.variant : "zh-CN";

  if (canonicalCount < MAINTENANCE_HINT_MIN_CANONICAL) {
    return null;
  }

  // Cooldown gate — future-stamped lastEmit (clock skew) treated as expired.
  if (
    typeof lastEmitMs === "number" &&
    Number.isFinite(lastEmitMs) &&
    nowMs >= lastEmitMs &&
    nowMs - lastEmitMs < cooldownDays * MS_PER_DAY
  ) {
    return null;
  }

  const lastDoctorTs = findLastDoctorRunTs(events);
  let ageDays = null;
  if (lastDoctorTs !== null) {
    ageDays = (nowMs - lastDoctorTs) / MS_PER_DAY;
    if (ageDays < days) return null;
  }

  const line2 = renderBanner("maintenanceLine2", variant, {});
  const line1 =
    lastDoctorTs === null
      ? renderBanner("maintenanceLine1Never", variant, {})
      : renderBanner("maintenanceLine1Aged", variant, {
          days,
          ageDays: ageDays.toFixed(1),
        });
  const reason = `${line1}\n${line2}`;

  return {
    decision: "soft",
    reason,
    signal: "maintenance",
    recommended_skill: null,
    threshold: days,
    actual_value: ageDays,
  };
}

module.exports = {
  EVENT_TYPE_DOCTOR_RUN,
  findLastDoctorRunTs,
  evaluateMaintenanceSignal,
};
