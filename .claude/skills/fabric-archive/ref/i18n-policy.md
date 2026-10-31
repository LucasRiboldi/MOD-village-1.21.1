# UX i18n Policy — full reference

> **Shared core (rc.37 NEW-13):** the cross-skill invariants — protected-token
> NEVER-translate list, AskUserQuestion routing-key rule, layer heuristic, and
> events-emit convention — live once in `../../lib/shared-policy.md`. This file
> keeps only the fabric-archive-specific 5-class examples. Read the shared lib
> for the common rules; do not fork them here.

> **Loaded on demand.** Only consult when rendering bilingual output AND you're unsure which class a string belongs to. SKILL.md gives the operative rule: read `.fabric/fabric-config.json` → `fabric_language`, emit prose in resolved variant, never translate protected tokens. The 5-class taxonomy below disambiguates edge cases.

## UX i18n Policy (5-class bilingualization)

The skill consults `fabric_language` from `.fabric/fabric-config.json`
(固化于 init 时，via `lib/detect-language.ts:detectExistingLanguage`; default `"en"` when no
CJK signal is detected in README + docs/; may resolve to `"match-existing"`,
`"zh-CN"`, `"en"`, or `"zh-CN-hybrid"`). All user-facing text in the
following 5 categories MUST be rendered in the resolved language:

1. **Roll-up templates** — the `# Archive Review — N candidates` batch
   review block (one per candidate) AND any final session summary the
   skill emits after Phase 4 completes. zh-CN ↔ en mirror.
2. **Errors / Preconditions warnings** — abort + gate-fail messages (e.g.
   the "没有触发归档信号…" trigger-miss and the "本次会话为常规执行…"
   viability-gate-FAIL message). zh-CN ↔ en mirror.
3. **Confirmation prompts** — the per-candidate `Confirm? (Y to accept,
   edit … inline, N to skip)` line in the batch review template. zh-CN
   ↔ en mirror.
4. **Dry-run table headers** — v2.0.0-rc.27 TASK-007 added a dry-run
   override path (see Phase 4.5 "dry-run") so users can preview the
   archive proposal without writing pending entries. The dry-run summary
   header and per-candidate preview labels MUST be bilingualized per
   this policy. zh-CN ↔ en mirror.
5. **AskUserQuestion** — `header` + `question` fields (NOT `options[]`).
   zh-CN ↔ en mirror. fabric-archive itself does not surface
   AskUserQuestion in the current contract (Phase 3 batch review is a
   single markdown screen, not a structured question), but if a future
   version adds one — e.g. to confirm layer flip — this rule applies.

Rendering rule:

- `fabric_language === "zh-CN"` → emit the zh-CN variant; pure monolingual, no language mixing inside a single user-facing block.
- `fabric_language === "en"` → emit the en variant; pure monolingual, no language mixing inside a single user-facing block.
- `fabric_language === "zh-CN-hybrid"` → emit Chinese narrative prose with English technical terms preserved. Protected tokens (always EN): MCP tool names (e.g. `fab_recall`), CLI command names (e.g. `fabric install`), file paths, technical concepts (`Skill`, `SessionStart`, `hook`, `MCP`, `revision_hash`, `pending`, `proven`, `verified`, `draft`).
- `fabric_language === "match-existing"` or any other value → emit the en variant; pure monolingual.

Protected tokens (`fab_propose`, `relevance_scope`,
`relevance_paths`, `narrow`, `broad`, `source_sessions`, `proposed_reason`,
`session_context`, `intent_clues`, `impact`, `must_read_if`,
`pending_path`, `layer`, `team`, `personal`,
`knowledge_scope_degraded`, `MUST`, `NEVER`, `knowledge/pending`, the verbatim
`强 team` / `强 personal` / `默认 team` heuristic block, etc.) are NEVER
translated — they appear verbatim in both language variants. The
bilingualization scope is prose ONLY.

### AskUserQuestion i18n Policy (value vs label)

When a skill (this one or any sibling skill the user is composing with)
issues an `AskUserQuestion`, the `header` and `question` strings are
user-facing prose → translated per `fabric_language`. The `options[]`
array entries (e.g. `["approve", "reject", "modify", "defer", "skip"]` in
fabric-review, or `["team", "personal"]` for a layer-flip target) are
**routing keys** consumed by the skill state machine — they MUST remain
English regardless of `fabric_language`.

```ts
// EN (fabric_language === "en")
AskUserQuestion({
  header: "Layer-flip target",
  question: "Move '{title}' to which layer? (current: {current_layer})",
  options: ["team", "personal"]
})

// zh-CN (fabric_language === "zh-CN")
AskUserQuestion({
  header: "Layer 切换目标",
  question: "将 '{title}' 切换到哪一层？(当前: {current_layer})",
  options: ["team", "personal"]   // 不翻译 — routing key
})
```

Rationale: localizing routing keys would force every routing branch to
dual-string match (e.g. `if (choice === "team" || choice === "团队")`),
which doubles the surface area for protected-token regressions and breaks
the option-list invariants that downstream tooling depends on. Keeping
`options[]` English-only is contract-locked across all three skills.
