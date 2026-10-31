# Phase 4 — Persist via MCP (ref)

> **Loaded on demand.** SKILL.md hot path retains the per-candidate one-call rule + brief mention of `proposed_reason` enum + session_context format. This file holds the full MCP call shape (with all rc.7/rc.23 optional fields), the C1 triage-field inference table, the proposed_reason mapping table, and the T5 array-form idempotency notes.

## Full MCP tool call shape

```ts
mcp__fabric__fab_propose({
  source_sessions: ["<session id1>", "<session id2>", ...],  // T5: array form (Phase 1)
  recent_paths: ["<path1>", "<path2>", ...],   // capped at archive_max_recent_paths (config-resolved, default 20)
  user_messages_summary: "<compact prose ≤500 chars>",
  type: "decisions" | "pitfalls" | "guidelines" | "models" | "processes",
  slug: "<kebab-case-2-to-5-words>",
  // v2.2 C1 (W1): author-facing scope is TWO fields only — `audience` + `paths`.
  // The engine derives layer / visibility_store / store / relevance_scope.
  audience: "team" | "personal" | "project:<id>" | "org:<...>",  // WHO it's for (open coordinate); omit → engine default (project:<active> | team)
  paths: ["<glob1>", "<literal2>", ...],  // relevance anchors; non-empty ⇒ narrow, empty/omit ⇒ broad (relevance_scope is derived from this — no separate flag)
  // v2.0.0-rc.7 T6: required fields for future-self reviewability.
  proposed_reason:
    "explicit-user-mark"      // user said "always / never / 下次注意" etc.
    | "diagnostic-then-fix"   // long debug loop surfaced a new pattern/pitfall
    | "decision-confirmation" // ≥2 options weighed AND rationale stated → decision/model
    | "wrong-turn-revert"     // tried path X, reverted → pitfall
    | "new-dependency-or-pattern" // new dep/lib/abstraction introduced
    | "dismissal-with-reason",    // user rejected approach AND said why
  session_context: "<3-5 line markdown: session goal + key turning point>",
  // v2.0.0-rc.23 TASK-006 (a-C1): four OPTIONAL structured triage fields.
  // Lift implicit signals out of `## Session context` prose so future-self
  // reviewers / plan-context retrievers can triage relevance from
  // frontmatter alone, without re-reading the body. Omit any field the
  // skill cannot infer cleanly — guessing is worse than omitting.
  intent_clues: ["<short trigger>", "<negative trigger e.g. 'NOT for X'>"],  // when this rule applies / when NOT
  impact: ["<consequence of ignoring>"],    // why future-self should care
  must_read_if: "<one-line strong trigger>", // single condition; if it holds, the entry is required reading
  tags: ["<lang/framework>", "<topic>"],    // tech_stack values merge here (v-next grill D2)
})
```

## C1 triage-field inference table

| Field          | Inference source                                                                 | Skip when                          |
|----------------|----------------------------------------------------------------------------------|------------------------------------|
| `intent_clues` | Pull from `session_context` turning point + negative phrasing in the transcript ("not for", "don't do X when") | No clear trigger phrasing surfaced |
| `tags`         | Merge tech-stack values (`.ts`→`typescript`, `.tsx`→`react`, `.go`→`go`, `package.json`→`nodejs`) plus topic tags. Replaces deprecated `tech_stack` field (v-next grill D2) | No meaningful tags to infer       |
| `impact`       | Pull from the diagnostic-loop body — "wasted 30 min", "production outage", "silent data loss" | No observable consequence stated   |
| `must_read_if` | Strongest single trigger from the worth-archive signal: a file path, a routine, a recurring condition; ≤160 chars | No single dominant trigger fits    |

All four fields are STRICTLY OPTIONAL. The schema accepts the call without any of them — omit rather than guess. None of the four participate in the idempotency_key hash (server formula at `extract-knowledge.ts:100-106` is frozen to `{source_session, type, slug}`), so partial-vs-full fill of these fields on the same triple is safe.

## proposed_reason → classification mapping

The skill infers `proposed_reason` from the classification + viability-gate signal that fired:

| Signal fired (Phase 2.5)       | Classification | Default proposed_reason     |
|--------------------------------|----------------|-----------------------------|
| Explicit normative language    | guideline      | `explicit-user-mark`        |
| Wrong-turn-and-revert          | pitfall        | `wrong-turn-revert`         |
| Long diagnostic loop           | pitfall/model  | `diagnostic-then-fix`       |
| New dependency adoption        | decision/model | `new-dependency-or-pattern` |
| New pattern emergence          | model          | `new-dependency-or-pattern` |
| Decision confirmation          | decision       | `decision-confirmation`     |
| Explicit dismissal-with-reason | decision       | `dismissal-with-reason`     |
| Process formalization          | process        | `new-dependency-or-pattern` |

## session_context format

```
Session goal: <one-line of what the user was trying to accomplish>
Turning point: <one-line of the key moment that produced the worth-archive observation>
[optional 1-3 more lines of supporting context]
```

Future-self reviewing the pending entry MUST be able to understand WHY this entry was proposed without conversation transcript access — `proposed_reason` is the structured why; `session_context` is the narrative why.

Note on type plurality: the MCP enum uses plural directory-form (decisions / pitfalls / guidelines / models / processes), while the conceptual classification uses singular nouns (decision / pitfall / guideline / model / process). They map 1:1.

The server returns `{ pending_path, idempotency_key }`. Display `pending_path` to the user so they can `Read` the persisted entry if they wish.

## Idempotency Notes (T5 array-form, rc.7+)

The MCP tool derives `idempotency_key = sha256({source_session, type, slug})`. Calling `fab_propose` twice with the same `(source_session, type, slug)` triple is SAFE: the server appends new evidence to the existing pending file rather than overwriting or producing duplicates. The skill MAY be re-invoked on the same session without producing junk.

If the skill needs to record a genuinely separate observation in the same session+type, the slug MUST differ.

**T5 array-form**: when `source_sessions` is passed as an array (rc.7 T5 contract), only `source_sessions[0]` participates in the server-side idempotency hash. Server formula at `packages/server/src/services/extract-knowledge.ts:78` is `sha256(JSON.stringify({source_session: sourceSessions[0], type, slug}))`. Implications:

- Same `(type, slug)` but a different **first** session → distinct idempotency key → produces two pending files.
- Same first session but different tail sessions → evidence-merge into the SAME pending file; tail `session_id`s are NOT recorded as independent evidence keys.
- The formula is intentionally stable across the rc.5 → rc.7 migration; adding or removing tail entries does NOT change the idempotency key, preserving rc.5 single-session compat.
