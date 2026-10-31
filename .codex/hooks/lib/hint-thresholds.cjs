// ISS-20260713-052: Stop-hook threshold bag assembly for fabric-hint.
const hintConfig = require("./hint-config.cjs");
const { readFabricLanguage } = require("./banner-i18n.cjs");

/**
 * Resolve externalized thresholds + fabric_language once per main().
 * Never throws — falls back to documented defaults.
 */
function buildStopThresholds(projectRoot) {
  let variant = "zh-CN";
  try {
    variant = readFabricLanguage(projectRoot);
  } catch {
    variant = "zh-CN";
  }

  try {
    return {
      archiveHintHours: hintConfig.readArchiveHintHours(projectRoot),
      reviewHintPendingCount: hintConfig.readReviewHintPendingCount(projectRoot),
      reviewHintPendingAgeDays: hintConfig.readReviewHintPendingAgeDays(projectRoot),
      maintenanceHintDays: hintConfig.readMaintenanceHintDays(projectRoot),
      maintenanceHintCooldownDays: hintConfig.readMaintenanceHintCooldownDays(projectRoot),
      variant,
    };
  } catch {
    return {
      archiveHintHours: hintConfig.DEFAULT_ARCHIVE_HINT_HOURS,
      reviewHintPendingCount: hintConfig.DEFAULT_REVIEW_HINT_PENDING_COUNT,
      reviewHintPendingAgeDays: hintConfig.DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS,
      maintenanceHintDays: hintConfig.DEFAULT_MAINTENANCE_HINT_DAYS,
      maintenanceHintCooldownDays: hintConfig.DEFAULT_MAINTENANCE_HINT_COOLDOWN_DAYS,
      variant,
    };
  }
}

module.exports = { buildStopThresholds };
