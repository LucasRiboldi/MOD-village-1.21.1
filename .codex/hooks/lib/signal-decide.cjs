// ISS-20260713-040: Stop-hook signal decide() policy for fabric-hint.
const { renderBanner } = require("./banner-i18n.cjs");
const hintConfig = require("./hint-config.cjs");
const {
  DEFAULT_UNDERSEED_NODE_THRESHOLD,
  DEFAULT_ARCHIVE_EDIT_THRESHOLD,
  DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT,
  DEFAULT_REVIEW_HINT_PENDING_COUNT,
  DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS,
  DEFAULT_ARCHIVE_HINT_HOURS,
  MS_PER_HOUR,
  MS_PER_DAY,
  UNDERSEED_POST_INIT_QUIET_HOURS,
  UNDERSEED_NO_PROPOSED_HOURS,
} = hintConfig;

const EVENT_TYPE_PROPOSED = "knowledge_proposed";
const EVENT_TYPE_INIT_SCAN_COMPLETED = "init_scan_completed";

function decide(events, now, pendingStats, underseedStats, editCounterStats, thresholds, banner, importInFlight, backlogStats) {
  const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
  const stats = pendingStats || { count: 0, oldestAgeMs: null };
  const underseed =
    underseedStats || { nodeCount: 0, threshold: DEFAULT_UNDERSEED_NODE_THRESHOLD };
  // crack 1: per-session edit view. `editsSinceArchive` = current session's
  // file_mutated count since its own archive anchor; `anchorPresent` = the
  // session has any ledger activity (the trigger gate, replacing the old
  // "global knowledge_proposed exists" gate).
  const editStats =
    editCounterStats || {
      editsSinceArchive: 0,
      threshold: DEFAULT_ARCHIVE_EDIT_THRESHOLD,
      anchorPresent: false,
    };
  // crack 2: cross-session backlog view (dead sessions with unarchived work).
  const backlog =
    backlogStats || {
      deadSessionCount: 0,
      threshold: DEFAULT_ARCHIVE_BACKLOG_SESSION_COUNT,
    };
  const cfg = thresholds || {};
  // crack 2: the global archive_hint_hours timer is retired (the cross-session
  // case is now the archive_backlog signal). cfg.archiveHintHours is still
  // accepted on the thresholds bag for back-compat but no longer drives Signal A.
  const reviewHintPendingCount =
    typeof cfg.reviewHintPendingCount === "number" && cfg.reviewHintPendingCount > 0
      ? cfg.reviewHintPendingCount
      : DEFAULT_REVIEW_HINT_PENDING_COUNT;
  const reviewHintPendingAgeDays =
    typeof cfg.reviewHintPendingAgeDays === "number" && cfg.reviewHintPendingAgeDays > 0
      ? cfg.reviewHintPendingAgeDays
      : DEFAULT_REVIEW_HINT_PENDING_AGE_DAYS;
  // rc.16 TASK-002: banner variant for the i18n lib. Defaults to 'zh-CN' so
  // existing test callers (which never pass thresholds.variant) get the rc.15
  // byte-identical Chinese output. main() always supplies the resolved variant.
  const variant = typeof cfg.variant === "string" ? cfg.variant : "zh-CN";

  // ---- Archive signal (crack 1 — per-session edit count) -------------------
  // In-session lane: nudge when THIS session has accumulated >= threshold file
  // mutations since its OWN archive anchor (computed per-session in main() from
  // file_mutated events — `editStats.editsSinceArchive`). The old global
  // 24h-OR-N-edits trigger is retired: the hours branch became the
  // archive_backlog signal below (crack 2), and the edit count is now
  // session-scoped so a neighbour window's archive can't zero this window's
  // work. `anchorPresent` gates the trigger (a session with zero ledger
  // activity has nothing to count).
  //
  // `lastProposedTs` / `hoursElapsed` are still derived here for the IMPORT
  // signal's "no knowledge_proposed in last 24h" guard further down.
  let lastProposedTs = null;
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const ev = events[i];
    if (ev && ev.event_type === EVENT_TYPE_PROPOSED && typeof ev.ts === "number") {
      lastProposedTs = ev.ts;
      break;
    }
  }
  const hoursElapsed =
    lastProposedTs === null ? null : (nowMs - lastProposedTs) / MS_PER_HOUR;

  const triggerByEdits =
    editStats.anchorPresent === true &&
    typeof editStats.editsSinceArchive === "number" &&
    editStats.editsSinceArchive >= editStats.threshold;

  // PRECEDENCE: in-session archive wins over backlog/review/import — recent
  // local work is the most actionable reminder.
  if (triggerByEdits) {
    // 人-first banner: edit-count fragment only (the hours fragment retired with
    // the global timer). Substring contracts ('次编辑', '阈值 N', 'fabric-archive')
    // preserved by banner-i18n's zh-CN templates. The activity overview line is
    // injected by main() via `banner` so decide() stays pure / filesystem-free.
    const parts = [
      renderBanner("archivePartsEdits", variant, {
        count: editStats.editsSinceArchive,
        threshold: editStats.threshold,
      }),
    ];
    const line1 = renderBanner("archiveLine1", variant, { parts: parts.join(" / ") });
    const activity = banner && typeof banner.activityOverview === "string"
      ? banner.activityOverview
      : "";
    const line2 = activity.length > 0
      ? renderBanner("archiveActivity", variant, { activity })
      : "";
    const line3 = renderBanner("archiveCta", variant, {});
    const reason = [line1, line2, line3].filter((l) => l.length > 0).join("\n");
    return {
      decision: "soft",
      reason,
      signal: "archive",
      recommended_skill: "fabric-archive",
      // v2.1 NEW-N-3: surface the firing sub-signal's numbers for the
      // hook_signal_emitted ledger row main() writes.
      threshold: editStats.threshold,
      actual_value: editStats.editsSinceArchive,
    };
  }

  // ---- Archive backlog signal (crack 2 — cross-session safety net) ---------
  // Fires when N+ DEAD sessions (session_ended / idle) carry unarchived
  // high-value work — the per-session replacement for the old global-24h timer
  // (which any neighbour's archive reset, orphaning low-signal ended sessions).
  // KT-DEC-0007: a soft reminder, never a gate. Ranked AFTER in-session archive
  // but BEFORE review/import: losing knowledge from an ended session is a
  // sharper signal than a review/import backlog.
  if (backlog.threshold > 0 && backlog.deadSessionCount >= backlog.threshold) {
    const line1 = renderBanner("backlogLine1", variant, { count: backlog.deadSessionCount });
    const line2 = renderBanner("backlogCta", variant, {});
    const reason = `${line1}\n${line2}`;
    return {
      decision: "soft",
      reason,
      signal: "archive_backlog",
      recommended_skill: "fabric-archive",
      threshold: backlog.threshold,
      actual_value: backlog.deadSessionCount,
    };
  }

  // ---- Review signal (rc.3 TASK-004) ---------------------------------------
  const triggerByPendingCount = stats.count >= reviewHintPendingCount;
  const triggerByPendingAge =
    stats.oldestAgeMs !== null && stats.oldestAgeMs / MS_PER_DAY >= reviewHintPendingAgeDays;

  // v2.0.0-rc.8 (TASK-002): suppress ONLY Signal B while a fabric-import
  // skill run is in flight (read from .fabric/.import-state.json by main()
  // and threaded in as `importInFlight`). Signals A, C, D are unaffected.
  // We fall through to Signal C evaluation rather than returning null —
  // review backlog should not pre-empt import-recommendation evaluation
  // when import is mid-run.
  if ((triggerByPendingCount || triggerByPendingAge) && importInFlight !== true) {
    // rc.7 T4: 人-first banner reformat for Signal B. Keeps the pending
    // count and age substrings (`${count} 条`, `${days} 天`) so existing
    // tests pass; drops the Agent-jussive "建议调用 ... skill ..." for a
    // polite question framing aimed at the human reader.
    // ISS-20260712-017: pass locale-neutral oldestDays; each banner variant
    // owns its age suffix (no zh-hardcoded ageSuffix + en string-replace).
    const oldestDays =
      stats.oldestAgeMs !== null
        ? (stats.oldestAgeMs / MS_PER_DAY).toFixed(1)
        : "";
    // rc.16 TASK-002: i18n via lib. Substrings ('${count} 条', 'fabric-review')
    // preserved by the lib's zh-CN templates.
    const line1 = renderBanner("reviewLine1", variant, {
      count: stats.count,
      oldestDays,
    });
    const line2 = renderBanner("reviewCta", variant, {});
    const reason = `${line1}\n${line2}`;
    return {
      decision: "soft",
      reason,
      signal: "review",
      recommended_skill: "fabric-review",
      // v2.1 NEW-N-3: dual trigger (pending-count OR oldest-age). Report the
      // count pair when it fired, else the oldest-age-in-days pair.
      threshold: triggerByPendingCount ? reviewHintPendingCount : reviewHintPendingAgeDays,
      actual_value: triggerByPendingCount ? stats.count : stats.oldestAgeMs / MS_PER_DAY,
    };
  }

  // ---- Import signal (rc.5 TASK-010) — underseeded corpus -------------------
  // All three conditions must hold (logical AND):
  //  1. node count < threshold (sparse corpus)
  //  2. init_scan_completed event >= 24h ago (workspace has been initialized
  //     for at least a day — we don't nag during the immediate post-init
  //     window when the user is still authoring baseline knowledge)
  //  3. no knowledge_proposed event in last 24h (user isn't actively
  //     archiving — if they were, the archive signal would have fired anyway,
  //     but we keep this guard explicit per spec)
  let lastInitScanTs = null;
  for (let i = events.length - 1; i >= 0; i -= 1) {
    const ev = events[i];
    if (
      ev &&
      ev.event_type === EVENT_TYPE_INIT_SCAN_COMPLETED &&
      typeof ev.ts === "number"
    ) {
      lastInitScanTs = ev.ts;
      break;
    }
  }
  const hoursSinceInit =
    lastInitScanTs === null ? null : (nowMs - lastInitScanTs) / MS_PER_HOUR;
  const hoursSinceProposed = hoursElapsed; // reuse archive-signal calc above
  const triggerUnderseed =
    // #3: null = undeterminable canonical count (old snapshot) → skip. Guard
    // first because `null < threshold` coerces to true in JS and would else
    // false-fire the underseed nudge on a stale corpus.
    underseed.nodeCount != null &&
    underseed.nodeCount < underseed.threshold &&
    hoursSinceInit !== null &&
    hoursSinceInit >= UNDERSEED_POST_INIT_QUIET_HOURS &&
    (hoursSinceProposed === null || hoursSinceProposed >= UNDERSEED_NO_PROPOSED_HOURS);

  if (triggerUnderseed) {
    // rc.16 TASK-002: i18n via lib. Substrings ('${nodeCount}/${threshold}',
    // 'fabric-import', '${hoursSinceInit}h') preserved by the lib's zh-CN
    // templates. Note: hoursSinceInit is passed as already-toFixed(1) string
    // to keep the lib pure (no number formatting in render path).
    const line1 = renderBanner("importLine1", variant, {
      nodeCount: underseed.nodeCount,
      threshold: underseed.threshold,
      hoursSinceInit: hoursSinceInit.toFixed(1),
    });
    const line2 = renderBanner("importCta", variant, {});
    const reason = `${line1}\n${line2}`;
    return {
      decision: "soft",
      reason,
      signal: "import",
      // W3-C: fabric-import folded into fabric-archive `source` mode.
      recommended_skill: "fabric-archive",
      // v2.1 NEW-N-3: underseed corpus trigger — node-count vs threshold. The
      // "import" signal collapses to schema signal_type "other" in main().
      threshold: underseed.threshold,
      actual_value: underseed.nodeCount,
    };
  }

  return null;
}

module.exports = {
  decide,
  EVENT_TYPE_PROPOSED,
  EVENT_TYPE_INIT_SCAN_COMPLETED,
};
