// ISS-20260713-053: shared summary line formatting for narrow/broad hints.
const DEFAULT_SUMMARY_MAX_LEN = 80;
function truncateSummary(raw, maxLen) {
  const s = typeof raw === "string" ? raw : "";
  const flat = s.replace(/\s+/g, " ").trim();
  const cap = typeof maxLen === "number" && maxLen > 0 ? maxLen : DEFAULT_SUMMARY_MAX_LEN;
  if (flat.length <= cap) return flat;
  return `${flat.slice(0, cap - 1)}…`;
}

function formatEntryLine(entry, maxLen) {
  const id = entry.id || "(no-id)";
  const type = entry.type || "unknown";
  const maturity = entry.maturity || "unknown";
  const summary = truncateSummary(entry.summary, maxLen);
  const tail = summary.length > 0 ? ` ${summary}` : "";
  // lifecycle-refactor W3-T2 (§7 图谱消费 / §5 hook 沿 related 二阶召回): mark entries
  // pulled in by a surfaced entry's one-hop `related` graph edge with their source
  // provenance. Omitted for ordinarily-ranked entries — no fake graph annotation
  // is ever synthesized (graph-empty honesty).
  const provenance =
    typeof entry.related_to === "string" && entry.related_to.length > 0
      ? ` (related-to-${entry.related_to})`
      : "";
  const head = `  [${id}] (${type}/${maturity})${tail}${provenance}`;
  // TASK-003 (impact-map MVP): when the entry declares a non-empty impact list,
  // append a ⚠️ consequence line right after the entry (rendered as a separate
  // stderr line — the caller joins the returned string on "\n"). Omitted for
  // entries with no/empty impact so the existing narrow-hint format is unchanged.
  const impact =
    Array.isArray(entry.impact) && entry.impact.length > 0
      ? `\n      ⚠️ 后果: ${entry.impact.filter((s) => typeof s === "string" && s.length > 0).join(" | ")}`
      : "";
  return `${head}${impact}`;
}

module.exports = { DEFAULT_SUMMARY_MAX_LEN, truncateSummary, formatEntryLine };
