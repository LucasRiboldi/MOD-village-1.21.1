// @generated from packages/shared/src/cite-line-parser.ts by scripts/build-hook-project-context.mjs; DO NOT EDIT
'use strict';


// ../shared/src/cite-line-parser.ts
var ID_RE = /^K[TP]-[A-Z]+-\d+$/;
var SENTINEL_RE = /^KB:\s*none\b\s*(?:\[[^\]]*\])?\s*$/i;
var QUALIFIED_ID = String.raw`(?:[^\s,:]+:)?K[TP]-[A-Z]+-\d+`;
var FULL_RE = new RegExp(
  String.raw`^KB:\s+(${QUALIFIED_ID}(?:\s*,\s*${QUALIFIED_ID})*)(?:\s+\(([^)]*)\))?(?:\s+\[([^\]]+)\])?(?:\s+→\s*(.+))?\s*$`
);
function splitStorePrefix(token) {
  const colon = token.lastIndexOf(":");
  return colon === -1 ? { store: null, id: token } : { store: token.slice(0, colon), id: token.slice(colon + 1) };
}
var CHAINED_FROM_ID_RE = /chained-from\s+(K[TP]-[A-Z]+-\d+)/i;
function normalizeCiteTag(rawTag) {
  const head = rawTag.trim().split(/[\s:]+/)[0].toLowerCase();
  if (head === "applied" || head === "dismissed" || head === "none") {
    return head;
  }
  return "none";
}
function parseTag(rawTag) {
  if (!rawTag) return "none";
  return normalizeCiteTag(rawTag);
}
function parseContractTail(tail) {
  const result = { operators: [], skip_reason: null };
  if (!tail) return result;
  const tokens = tail.trim().split(/\s+/).filter((t) => t.length > 0);
  for (const token of tokens) {
    const skipMatch = token.match(/^skip:(.+)$/i);
    if (skipMatch) {
      if (result.skip_reason === null) result.skip_reason = skipMatch[1];
      continue;
    }
    const notEditMatch = token.match(/^!edit:(.+)$/i);
    if (notEditMatch) {
      result.operators.push({ kind: "not_edit", target: notEditMatch[1] });
      continue;
    }
    const opMatch = token.match(/^(edit|require|forbid):(.+)$/i);
    if (opMatch) {
      result.operators.push({
        kind: opMatch[1].toLowerCase(),
        target: opMatch[2]
      });
    }
  }
  return result;
}
function parseLine(line) {
  const trimmed = line.trim();
  if (trimmed.length === 0) return null;
  if (SENTINEL_RE.test(trimmed)) {
    return { ids: [], stores: [], tag: "none", commitment: null };
  }
  const fullMatch = trimmed.match(FULL_RE);
  if (fullMatch) {
    const split = fullMatch[1].split(",").map((part) => part.trim()).filter((part) => part.length > 0).map(splitStorePrefix);
    if (split.some((entry) => !ID_RE.test(entry.id))) return null;
    const primaryIds = split.map((entry) => entry.id);
    const primaryStores = split.map((entry) => entry.store);
    const rawTag = fullMatch[3];
    const tag = parseTag(rawTag);
    const chainedIds = [];
    if (rawTag !== void 0) {
      const chained = CHAINED_FROM_ID_RE.exec(rawTag);
      if (chained !== null && ID_RE.test(chained[1])) {
        chainedIds.push(chained[1]);
      }
    }
    return {
      ids: [...primaryIds, ...chainedIds],
      // chained-from ids are never store-qualified → null per chained id.
      stores: [...primaryStores, ...chainedIds.map(() => null)],
      tag,
      commitment: parseContractTail(fullMatch[4])
    };
  }
  return null;
}
function parseCiteLine(raw) {
  const result = {
    cite_ids: [],
    cite_tags: [],
    cite_commitments: [],
    cite_stores: []
  };
  if (typeof raw !== "string") return result;
  for (const line of raw.split(/\r?\n/)) {
    const parsed = parseLine(line);
    if (!parsed) continue;
    result.cite_tags.push(parsed.tag);
    for (let i = 0; i < parsed.ids.length; i += 1) {
      result.cite_ids.push(parsed.ids[i]);
      result.cite_stores.push(parsed.stores[i] ?? null);
    }
    if (parsed.commitment !== null) {
      for (let i = 0; i < parsed.ids.length; i += 1) {
        result.cite_commitments.push(parsed.commitment);
      }
    }
  }
  return result;
}

exports.normalizeCiteTag = normalizeCiteTag;
exports.parseCiteLine = parseCiteLine;
