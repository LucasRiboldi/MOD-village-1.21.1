# UX i18n Policy — fabric-review full reference

> **Shared core (rc.37 NEW-13):** the cross-skill invariants — protected-token
> NEVER-translate list, AskUserQuestion routing-key rule, layer heuristic, and
> events-emit convention — live once in `../../lib/shared-policy.md`. This file
> keeps only the fabric-review-specific 5-class examples. Read the shared lib
> for the common rules; do not fork them here.

> **Loaded on demand.** Only consult when you need to disambiguate which of the 5 classes a given string belongs to. SKILL.md gives the operative rule.

## Language resolution

The effective language is the **machine-wide base tone** in
`~/.fabric/fabric-global.json` → `language`, resolved by
`resolveGlobalLocale()`. Only two values are legal: `zh-CN` and `en`
(ISS-20260712-016).

The per-project `fabric_language` field and the README/docs content-detection
path are **retired for skill rendering** — `resolveFabricLocale(projectRoot)`
ignores its argument and delegates straight to `resolveGlobalLocale()`. Do not
branch on `fabric_language`, `match-existing`, or `zh-CN-hybrid` here; they no
longer reach this surface.

Rendering rule: emit the variant matching the resolved language, **pure
monolingual** — no language mixing inside a single user-facing block.
Protected tokens are the sole exception (see below).

## The 5 bilingualized classes

All user-facing text in these 5 categories MUST be rendered in the resolved language:

1. **Roll-up templates** — the `# Review Summary — mode={pending|maintain}` final block,
   the `## Health Overview` dashboard in the maintain/health sub-flow, and any per-item
   display blocks (`## [type=...] [layer=...] pending_path=...` lines).
   zh-CN ↔ en mirror.
2. **Errors / Preconditions warnings** — abort + trigger-miss messages
   (e.g. "没有触发 review 信号…" / "No review signal detected…").
   zh-CN ↔ en mirror.
3. **Confirmation prompts** — free-text reject-reason follow-up, the
   "Type relevance_paths (comma-separated globs, …)" narrow-scope
   follow-up, and any other free-text prompts. zh-CN ↔ en mirror.
4. **Dry-run table headers** — fabric-review does not currently expose
   a dry-run mode; this slot is reserved for parity with the other skills.
   IF a future revision adds dry-run, the table header MUST be
   bilingualized per this policy. zh-CN ↔ en mirror.
5. **AskUserQuestion** — `header` + `question` fields (NOT `options[]`).
   zh-CN ↔ en mirror. fabric-review is the heaviest AskUserQuestion
   consumer (per-item action, layer-flip target, stale-item action,
   modify-extended option set), so this class applies broadly.

## Protected tokens (NEVER translated)

`fab_review`, `fab_pending`, `fab_propose`, `relevance_scope`, `relevance_paths`,
`narrow`, `broad`, `source_sessions`, `proposed_reason`, `session_context`,
`pending_path`, `stable_id`, `layer`, `team`, `personal`,
`knowledge_scope_degraded`, `MUST`, `NEVER`, `knowledge/pending`, and every MCP
tool / CLI command / file path — these appear verbatim in both language
variants. The bilingualization scope is prose ONLY.

## `options[]` stay English — the routing-key rule

`AskUserQuestion` `header` + `question` are user-facing prose → translated.
The `options[]` entries are **routing keys** consumed by the skill's own
`switch` over `choice` — they MUST remain English regardless of language.

Canonical option arrays used by this skill (every value stays English in BOTH variants):

- Per-item action: `["approve", "reject", "modify", "defer", "skip"]`
- Per-stale-item action (maintain/health): `["defer", "demote", "skip"]`
- Layer-flip target: `["team", "personal"]`
- Modify-extended (import-origin narrow-scope nudge):
  `["narrow scope", "edit summary", "change layer", "change maturity", "skip"]`

Rationale: localizing routing keys would force every routing branch to
dual-string match (e.g. `if (choice === "approve" || choice === "通过")`),
which doubles the surface area for protected-token regressions and breaks
the option-list invariants that downstream tooling depends on. Keeping
`options[]` English-only is contract-locked across all skills.

> Concrete bilingual AskUserQuestion call shapes live where they are used —
> `ref/per-mode-flows.md` (per-item action, stale triage) and
> `ref/modify-flow.md` (layer-flip target, modify-extended). They are not
> duplicated here.
