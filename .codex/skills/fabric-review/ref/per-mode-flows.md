# Per-Mode Flows + Output Contract — fabric-review

Step-by-step procedures, bilingual rendering blocks, and the closing roll-up for the **2 modes** (`pending` / `maintain`) referenced from SKILL.md.

> **Language.** Every bilingual block below is selected by the machine-wide
> `~/.fabric/fabric-global.json` → `language` (`zh-CN` | `en` only). The
> per-project `fabric_language` field is retired for skill rendering —
> `resolveFabricLocale()` delegates to `resolveGlobalLocale()`. Emit ONE
> language per block; never mix.

---

## Mode: pending — Approve / Reject / Modify Backlog

1. Call `fab_pending` with `action: "list"`, no filters (or `filters.layer="both"` if user explicitly mentioned both layers).
2. Server returns `items[]` (each = `{pending_path, type, layer, maturity, tags?, title?, summary?}`).
3. Before presenting, perform **Semantic Check + Activation Check** (see `ref/semantic-check.md`) by issuing one or more `fab_recall` calls on the paths each pending entry is about, to surface possible duplicates / contradictions among already-canonical entries, then judge whether the pending entry changes next action. (Ranked retrieval, not `fab_pending action="search"` — see `ref/semantic-check.md` for why a substring gate cannot do dedupe.)
4. For each pending item, render a per-item block. Render `proposed_reason` (frontmatter) with its display-time description from `PROPOSED_REASON_DESCRIPTIONS_BY_LOCALE` enum (v-next grill D8) + first line of `## Context` so future-self has full context without re-reading the transcript. UX i18n Policy class 1 — roll-up templates; protected tokens (`pending_path`, `layer`, `team`, `decisions`, `proposed_reason`, `Tags`, etc.) appear verbatim in BOTH variants:

   **en variant** (`language === "en"`):

   ```md
   ## [type=decisions] [layer=team] pending_path=knowledge/pending/decisions/single-cjs-hook.md
   Title: Single .cjs hook across clients
   Summary: stdout JSON shape is identical across the three clients; one script suffices.
   Maturity: draft   Tags: [hook, cli]
   must_read_if: editing Stop-hook JSON output across clients
   intent_clues: [hook parity, client stdout JSON, NOT UI copy]
   impact: avoids split client hooks drifting silently
   Proposed reason: decision-confirmation — ≥2 alternatives weighed; rationale stated.
   Context: Session goal: ship Stop-hook for v2 release.
   ⚠ Possible duplicate of KT-D-0007 (overlap: high)
   ⚠ reached-but-inert if summary/triage fields do not change next action
   ```

   **zh-CN variant** (`language === "zh-CN"`):

   ```md
   ## [type=decisions] [layer=team] pending_path=knowledge/pending/decisions/single-cjs-hook.md
   标题: 单 .cjs hook 跨客户端
   摘要: 三客户端 stdout JSON 格式一致，单脚本即可。
   成熟度: draft   Tags: [hook, cli]
   must_read_if: editing Stop-hook JSON output across clients
   intent_clues: [hook parity, client stdout JSON, NOT UI copy]
   impact: avoids split client hooks drifting silently
   Proposed reason: decision-confirmation — ≥2 候选方案经权衡后确认选型。
   Context: Session goal: ship Stop-hook for v2 release.
   ⚠ 可能重复 KT-D-0007 (overlap: high)
   ⚠ reached-but-inert: 摘要 / triage 字段若不能改变下一步动作,先走 modify-content
   ```

   The Skill MUST read `proposed_reason` from the pending file's frontmatter (parse the YAML block, key `proposed_reason`) and render its display-time description from `PROPOSED_REASON_DESCRIPTIONS_BY_LOCALE` enum (v-next grill D8 — body `## Why proposed` is removed; the enum is the single source for reason descriptions). Read the first non-blank line of `## Context` (renamed from `## Session context`) from the body. If either is missing on a pre-rc.7 pending entry, render the legacy fallback (UX i18n Policy class 1):

   - en: `Proposed reason: <legacy entry, no reason recorded>` and `Context: <not recorded>`
   - zh-CN: `Proposed reason: <历史条目，未记录 reason>` 与 `Context: <未记录>`

   …so the reviewer can still proceed.

   If `must_read_if`, `intent_clues`, or `impact` are missing on an old pending entry, render them as `<not recorded>` and consider that an activation warning, not a parser failure.

5. Surface a per-item AskUserQuestion. UX i18n Policy class 5 — `header` + `question` translated; `options[]` remain English routing keys:

   ```ts
   // EN
   AskUserQuestion({
     header: "Review pending entry",
     question: "What action for 'Single .cjs hook across clients'?  ({pending_path})",
     options: ["approve", "reject", "modify", "defer", "skip"]
   })

   // zh-CN
   AskUserQuestion({
     header: "审核 pending 条目",
     question: "对 '单 .cjs hook 跨客户端' 执行什么操作？({pending_path})",
     options: ["approve", "reject", "modify", "defer", "skip"]   // 不翻译 — routing key
   })
   ```

6. Route the user's choice:
   - `approve` → accumulate pending_path into a batch; flush via single `fab_review action="approve"` with `pending_paths=[…]` after the loop ends.
   - `reject` → ask the user for a one-line reason via free-text follow-up; call `fab_review action="reject"` with `pending_paths=[path]` and `reason`.
   - `modify` → decide the change with the user (see `ref/modify-flow.md`), then route by kind:
     - **content edit** (title/summary/tags/maturity/relevance_*/related/must_read_if/intent_clues/impact/semantic_scope — NO layer flip) → accumulate `{pending_path, changes}` into a content-modify batch and flush via a single `fab_review action="modify-content-batch"` with `items=[…]` after the loop ends. Mirrors approve's accumulate-flush; collapses N per-item modify round-trips (each paying a first-reconcile gate wait) into one call — the maintain loop's dominant cost.
     - **layer flip** (`changes.layer` differs) → interactive + rare; call `fab_review action="modify-layer"` immediately per-item (NEVER batched — each needs the team/personal AskUserQuestion confirm; see `ref/modify-flow.md`).
   - `defer` → call `fab_review action="defer"` with `pending_paths=[path]`; optional `until` ISO datetime if the user supplies one ("defer 2 weeks" → compute and set).
   - `skip` → no MCP call; move to next item.

7. After the loop, flush any accumulated batches (approve `pending_paths[]`, content-modify `items[]`) with their single calls, then emit the roll-up (see **Output Contract** below). Surface every `ok:false` row from the content-modify batch's `modified[]` so a failed item is not silently lost.

---

## Mode: maintain — Operate on Already-Canonical Knowledge

`maintain` merges the legacy `topic` / `health` / `revisit` modes plus the `retire` and `relate` sub-flows. Pick the sub-flow from the same keyword scan that selected the mode; all five write through `fab_review` only.

### Sub-flow: browse-by-topic

1. Extract the topic keyword(s) from the user's message (e.g. "find about deepMerge" → query="deepMerge").
2. Call `fab_pending action="search"` with `query` and any obvious filters (if user said "team-only" → `filters.layer="team"`).
3. Server returns `items[]` ranked by relevance — these are entries already in mounted store `knowledge/<type>/` (NOT pending), unless `filters` says otherwise.
4. Render top-N (cap at `review_topic_result_cap`, config-resolved, default 8) results with title / summary / pending_path.
5. If the user follow-up indicates intent to act ("approve all", "modify the second one"), pivot into the corresponding pending-mode action — the search result already gives the `pending_path` needed for the action.
6. NEVER surface a per-item AskUserQuestion just for browsing — only when the user signals an action verb.

### Sub-flow: health / staleness

1. Call `fab_pending action="list"` with `filters.maturity="draft"` (or no filter for full corpus inspection).
2. Tail `.fabric/events.jsonl` for layer_changed / demoted / rejected counts in the trailing 30 days.
3. Compute stale candidates: pending entries with mtime older than `review_stale_pending_days` (config-resolved, default 14) OR maturity=draft entries with no recent evidence-append events.
4. Render a corpus dashboard. UX i18n Policy class 1:

   **en variant**:

   ```md
   ## Health Overview
   - Pending: 12 entries (oldest 18d) — recommend `defer` or `reject`
   - Drafts: 8 (3 are stale candidates: KP-G-3, KP-G-5, KT-P-9)
   - Layer flips (30d): 2
   - Rejections (30d): 1
   ```

   **zh-CN variant**:

   ```md
   ## 健康度总览
   - Pending: 12 条 (最旧 18 天) — 建议 `defer` 或 `reject`
   - Drafts: 8 条 (3 条为陈旧候选: KP-G-3, KP-G-5, KT-P-9)
   - Layer 切换 (30 天): 2
   - 已驳回 (30 天): 1
   ```

5. For each stale candidate, surface AskUserQuestion. UX i18n Policy class 5:

   ```ts
   // EN
   AskUserQuestion({
     header: "Stale entry triage",
     question: "Action for stale entry '{title}'?",
     options: ["defer", "demote", "skip"]
   })

   // zh-CN
   AskUserQuestion({
     header: "陈旧条目处理",
     question: "对陈旧条目 '{title}' 执行什么操作？",
     options: ["defer", "demote", "skip"]   // 不翻译
   })
   ```

   Route `defer` → `fab_review action="defer"`; `demote` → `fab_review action="modify-content"` with `changes.maturity` lowered (or `reject` if the user wants outright removal of a pending entry).

### Sub-flow: revisit

1. The user referenced a specific entry (by id `KT-D-7` or by slug `single-cjs-hook`).
2. Call `fab_pending action="list"` with `filters` narrowed by best-guess fields; if the entry is canonical (has stable_id), use the path returned by `fab_pending` instead of inventing a store path.
3. Display the full body (frontmatter + content). Tail the events.jsonl for any history events tagged with this stable_id.
4. Surface AskUserQuestion `{options: ["approve", "modify", "reject", "skip"]}` only if the entry is still pending; for canonical entries the only mutation paths are `modify-content` / `modify-layer` / `retire`.

   When the user's stated intent is a layer change ("that's actually personal not team"), confirm the target via `AskUserQuestion {options: ["team", "personal"]}` BEFORE calling `fab_review action="modify-layer"`, then render `Layer flipped: <prior_stable_id> → <new_stable_id>` from the server response — never swallow the id change (see `ref/modify-flow.md`).

### Sub-flow: retire

See `ref/retire-mode.md` — two red lines (deprecate-over-delete / rescue-before-delete), intent→action map, tri-state procedure, scope re-assignment.

### Sub-flow: relate

See `ref/relate-mode.md` — edge-type criteria, sparse-over-dense cap, `KT→KP` privacy iron law.

---

## Output Contract — closing roll-up

After each invocation the skill MUST produce a roll-up. UX i18n Policy class 1. Protected tokens (event-type strings such as `knowledge_promoted` / `knowledge_layer_changed` / `knowledge_rejected` / `knowledge_deferred`, plus `.fabric/events.jsonl`) appear verbatim in BOTH variants:

**en variant**:

```md
# Review Summary — mode={pending|maintain}
- Listed: N entries
- Approved: M (new stable_ids: KT-D-12, KT-G-4, KP-P-2)
- Rejected: R
- Modified: U (incl. K layer flips)
- Deferred: D
- Skipped: S

## Events appended (.fabric/events.jsonl tail)
- knowledge_promote_started ×M
- knowledge_promoted ×M
- knowledge_layer_changed ×K
- knowledge_rejected ×R
- knowledge_deferred ×D
```

**zh-CN variant**:

```md
# Review 汇总 — mode={pending|maintain}
- 列出: N 条
- 已批准: M (新分配 stable_ids: KT-D-12, KT-G-4, KP-P-2)
- 已驳回: R
- 已修改: U (含 K 次 layer 切换)
- 已延后: D
- 已跳过: S

## 追加事件 (.fabric/events.jsonl 末尾)
- knowledge_promote_started ×M
- knowledge_promoted ×M
- knowledge_layer_changed ×K
- knowledge_rejected ×R
- knowledge_deferred ×D
```

Also surface the target store alias/UUID for every mutation so the user can inspect that store repo's `git status` when needed.

### events.jsonl atomicity constraint

Event lines appended to `.fabric/events.jsonl` are subject to POSIX single-write atomicity: only writes ≤ 4KB (`PIPE_BUF`) are guaranteed atomic via `Bash: echo "..." >> file`. Lines exceeding 4KB risk interleaved corruption under concurrent skill + server writes to the same ledger.

Skills MUST ensure:

- Each event JSON line is a **single line** (no embedded newlines; escape `\n` in any string value).
- `session_context` and other free-form text fields **self-truncate** to keep the entire serialized line under 4KB. Suggested per-field caps: `session_context` first 500 chars; `source_sessions` cap at 5 entries; `recent_paths` cap at 20 entries; `user_messages_summary` first 500 chars.
- If approaching the 4KB ceiling after the per-field caps, drop optional fields (e.g. tags / extra metadata) **before** truncating semantic content (the summary / context that carries the actual observation).
- The promote / reject / modify / defer events listed above are emitted by the MCP server via `appendEventLedgerEvent` and are already length-bounded server-side; this constraint applies to any event the skill itself appends directly to the ledger (rare, but possible for diagnostic markers).

---

## Mode Inference — Examples & Anti-Pattern (companion to SKILL.md table)

### Inference examples (sample user messages → expected mode)

- "review the pending knowledge" → `pending` (Step 1 keyword "review pending")
- "find anything about deepMerge" → `maintain` / browse-by-topic (Step 1 keyword "find … about")
- "anything stale in our knowledge base?" → `maintain` / health (Step 1 keyword "stale")
- "look at KT-D-7" → `maintain` / revisit (Step 1 keyword "look at <id>")
- "清理陈旧知识" → `maintain` / retire
- "补 related 边" → `maintain` / relate
- (Stop-hook fired with signal=review, no user typing) → `pending` (Step 2 default, overflow threshold tripped)

### Anti-pattern (Hard Rule restatement)

NEVER emit an `AskUserQuestion` whose options include {pending, maintain} (or the legacy {topic, health, revisit} aliases). The user does not pick the mode. If inference is genuinely ambiguous after both steps, default to `pending` and proceed; the user can always cancel and redirect.
