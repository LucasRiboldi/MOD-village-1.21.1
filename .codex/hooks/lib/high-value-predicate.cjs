// @generated from packages/shared/src/high-value-predicate.ts by scripts/build-hook-project-context.mjs; DO NOT EDIT
'use strict';


// ../shared/src/high-value-predicate.ts
var HIGH_VALUE_ARCHIVE_EVENT_TYPES = /* @__PURE__ */ new Set([
  "knowledge_context_planned",
  "edit_paths_recorded",
  "edit_intent_checked"
  // the real high-freq edit signal (rc.37 NEW-14/B3)
]);
var NORMATIVE_KEYWORDS = [
  "\u4EE5\u540E",
  "always",
  "never",
  "from now on",
  "\u4E0B\u6B21",
  "\u8BB0\u4E00\u4E0B",
  "\u6C38\u8FDC\u4E0D\u8981"
];
function isHighValueArchiveCandidate(events, sessionId, watermarkTs) {
  if (!Array.isArray(events)) return false;
  if (typeof sessionId !== "string" || sessionId.length === 0) return false;
  const wm = typeof watermarkTs === "number" ? watermarkTs : 0;
  let latestTurn = null;
  for (const raw of events) {
    const e = raw;
    if (!e || typeof e.ts !== "number" || e.ts <= wm) continue;
    if (e.session_id !== sessionId) continue;
    if (typeof e.event_type === "string" && HIGH_VALUE_ARCHIVE_EVENT_TYPES.has(e.event_type)) {
      return true;
    }
    if (e.event_type === "assistant_turn_observed") {
      if (latestTurn === null || typeof latestTurn.ts === "number" && e.ts > latestTurn.ts) {
        latestTurn = e;
      }
    }
  }
  if (latestTurn !== null) {
    const haystack = JSON.stringify(latestTurn).toLowerCase();
    for (const kw of NORMATIVE_KEYWORDS) {
      if (haystack.includes(kw.toLowerCase())) return true;
    }
  }
  return false;
}

exports.HIGH_VALUE_ARCHIVE_EVENT_TYPES = HIGH_VALUE_ARCHIVE_EVENT_TYPES;
exports.NORMATIVE_KEYWORDS = NORMATIVE_KEYWORDS;
exports.isHighValueArchiveCandidate = isHighValueArchiveCandidate;
