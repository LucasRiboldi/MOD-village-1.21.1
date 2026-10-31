# Shared skill policy — cross-skill canonical core (rc.37 NEW-13)

> **Single source of truth** for the policy invariants that fabric-archive
> (incl. its source mode) and fabric-review (incl. its retire / relate
> sub-flows) depend on. Each skill's `ref/` keeps
> only its skill-specific examples and points here (`../../lib/shared-policy.md`)
> for the common rules. Edit invariants HERE — never fork them per skill.

## 0. Pre-action gating (MUST before Edit/Write)

Agents **MUST** call `fab_recall(paths=[...], session_id=...)` before Edit/Write when knowledge might apply. Optionally Read selected `entries[].read_path`; say `dismissed: <id> (<reason>)` when skipping a hit. Hooks stay soft-nudge only (KT-DEC-0007) — **never** permanent `decision:block` for missing recall.

## 1. Protected tokens — NEVER translated

When rendering bilingual (zh-CN ↔ en) output, prose is translated but the
following classes of token appear **verbatim in both variants**:

- **MCP tool + field names**: `fab_propose`, `fab_review`, `fab_pending`,
  `fab_recall`, `fab_archive_scan`,
  `relevance_scope`, `relevance_paths`, `source_sessions`, `proposed_reason`,
  `session_context`, `intent_clues`, `impact`, `must_read_if`,
  `evidence_paths`, `tags`, `pending_path`, `layer`.
- **Enum / routing values**: `narrow`, `broad`, `team`, `personal`, `draft`,
  `verified`, `proven`, `knowledge_scope_degraded`.
- **Imperatives + paths**: `MUST`, `NEVER`, `knowledge/pending`, file paths.

The authoritative machine-checked list is `PROTECTED_TOKENS` in
`@fenglimg/fabric-shared` (enforced by `scripts/lint-protected-tokens.ts`).
Bilingualization scope is **prose ONLY**.

## 2. AskUserQuestion routing-key invariant

When any skill issues an `AskUserQuestion`:

- `header` + `question` → user-facing prose → **translated** per
  `.fabric/fabric-config.json` → `fabric_language`.
- `options[]` entries → **routing keys** consumed by the skill's `switch`
  over the returned choice → stay **English** in BOTH variants.

Canonical option arrays (English in every locale):

- Per-item review action: `["approve", "reject", "modify", "defer", "skip"]`
- Stale-item action (review health mode): `["defer", "demote", "skip"]`
- Layer-flip target: `["team", "personal"]`

A skill that translates `options[]` MUST then dual-string-match
(`choice === "approve" || choice === "通过"`); avoid this — keep options
English so the state machine stays single-string.

## 3. Layer heuristic (team vs personal)

Default classification when archiving / proposing knowledge:

- **强 team** — cross-cutting decisions, architecture, shared pitfalls,
  conventions the whole repo depends on.
- **强 personal** — individual workflow preferences, local environment quirks,
  notes scoped to one contributor (`KP-*` ids, in-repo `~/.fabric` root).
- **默认 team** — when ambiguous, default to `team` (shared visibility is the
  safer default; a mis-scoped personal entry hides knowledge from the team).

This block is itself a protected token sequence — render `强 team` /
`强 personal` / `默认 team` verbatim.

## 4. Events emit convention

Skills persist lifecycle via MCP tools (which emit the canonical events) —
they do NOT hand-write `.fabric/events.jsonl`:

- `fab_propose` → `knowledge_proposed` (+ archive-attempt events).
- `fab_review` approve → `knowledge_promote_started` → `knowledge_promoted`.
- `fab_review` modify-layer → `knowledge_layer_changed` (+ id-redirect).
- `fab_pending` list/search → read-only browse/search (no events emitted).

Never instruct the user to delete or hand-edit the event ledger; it is the
append-only audit trail. Counter rollups live in `.fabric/metrics.jsonl`.
