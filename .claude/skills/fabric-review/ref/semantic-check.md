# Semantic Check Guidance — fabric-review

LLM-assisted duplicate / contradiction / subsumption detection plus activation/actionability review during `pending` mode (and on demand during the `maintain`/browse sub-flow).

> Boundary B (locked): "extraction classification / layer inference / slug naming / mode inference / **semantic dedup** → Skill (LLM); pending file write / frontmatter assembly / idempotency check / counter mgmt / layer-flip transaction / atomic promote → MCP (deterministic)"

Semantic check is the LLM's job — the MCP tool does NOT compare meaning.

## Procedure

For each pending entry to be presented:

1. Call `fab_recall(paths=[<the code paths the pending entry is about>])` to fetch the ranked canonical neighbours. Do NOT gather this set with `fab_pending action="search"`: its query is a substring match, so it only returns entries that reused your wording — and a differently-worded duplicate is the whole reason this check exists. An empty `search` result means the guessed terms missed, not that nothing overlaps.
2. Compare semantically (LLM judgment, not string match). 三类判断均为 LLM 主观判断 dup/subsumption；具体阈值不可量化（不使用百分比 / 相似度数值伪精度）：
   - **Duplicate** — same essential claim. 标题与摘要表达同一核心结论，pending 未提供新证据或新上下文。Flag: `⚠ Possible duplicate of <stable_id>`.
   - **Contradiction** — opposing claims about the same subject. 例：一个 entry 说 "use X"，pending 说 "avoid X"，且作用域一致。Flag: `⚠ Contradicts <stable_id>`.
   - **Subsumption** — pending fully covered by an existing entry plus extras. Flag: `⚠ Subsumed by <stable_id>; consider modify-to-merge`.
3. Run activation/actionability review. A pending entry is useful only if reading it changes next action. Flag:
   - `⚠ reached-but-inert` — the entry only records a topic, event, or abstract principle and does not say what the next agent should do differently.
   - `⚠ Weak must_read_if` — `must_read_if` is missing, generic, or does not name a concrete trigger condition.
   - `⚠ Weak intent_clues` — `intent_clues` are keyword stuffing, all-positive with no useful exclusion, or fail to route the entry.
   - `⚠ Weak impact` — `impact` is empty, tautological, or does not explain the cost/benefit of applying the entry.
4. Surface the flags in the per-item display block (see `per-mode-flows.md` pending mode step 4). Semantic flags are informational; activation flags recommend `modify-content` before approval.
5. The user decides:
   - Still approve → flag is informational; pending becomes canonical alongside the existing entry.
   - Modify-to-harmonize → user supplies edits via `modify` action; consider merging language with the existing entry.
   - Modify-to-activate → rewrite `summary`, `must_read_if`, `intent_clues`, `impact`, or body so the entry changes next action.
   - Reject as duplicate → reason field MUST cite the existing stable_id (e.g. `reason="duplicate of KT-D-7"`).

## What NOT to do

DO NOT call `AskUserQuestion` to ask "is this a duplicate?" — the LLM has already judged. The user only chooses among approve / reject / modify, which is a genuine choice.

DO NOT approve a `reached-but-inert` entry only because the statement is true. True-but-inert knowledge should be rewritten or rejected; otherwise it becomes store noise that future agents read without changing behaviour.
