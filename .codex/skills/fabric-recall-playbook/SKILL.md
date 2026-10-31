---
name: fabric-recall-playbook
description: "Knowledge retrieval playbook — when/how to fab_recall, lazy body Read, pending triage, failure paths, and evidence-vs-knowledge boundary. Use before editing files, when knowledge might apply, when user asks why something was decided, or when retrieval seems empty."
---

# fabric-recall-playbook — retrieval playbook

Fabric is **curated knowledge**, not local evidence memory (logs/terminal/session dumps). This skill teaches **how to use** existing MCP/CLI retrieval — it does not replace `fab_recall` and does not capture terminal output.

## Hard rules

- Agents **MUST** call `fab_recall(paths=[...], session_id=...)` before editing files when knowledge might apply.
- Agents **MUST** load bodies only via native `Read` of selected `entries[].read_path` (lazy body; no bulk dump).
- Agents **NEVER** invent `stable_id` / cite ids without a real recall (or pending search) hit.
- Agents **NEVER** auto-promote logs, terminal dumps, or raw transcripts to canonical knowledge.
- For scenario tables and failure-path checklists, open `ref/scenarios.md` (linked from this skill).

## Pre-action gating checklist (MUST before Edit/Write)

When knowledge might apply, gate edits with this numbered checklist (soft policy — hooks stay nudge-only, KT-DEC-0007; **never** `decision:block` for missing recall):

1. Call `fab_recall(paths=[files you will touch], session_id=<client-session-id>)` before any Edit/Write.
2. Optionally `Read` only selected high-impact `entries[].read_path` bodies (lazy; no bulk dump).
3. If a hit is wrong-scope / outdated / not applicable, say `dismissed: <id> (<reason>)` with reason enum `scope-mismatch | outdated | not-applicable | other:<text>` before overriding.

## Mental model

| Layer | Product | Question |
| --- | --- | --- |
| Evidence | companion tools (optional) | What just happened (command/test/session)? |
| **Knowledge** | **Fabric** | What should we remember (decision/pitfall/guideline)? |
| Orchestration | maestro-flow | How do we execute work? |

**One-way pipeline only:** evidence → human/skill dewatering → `pending` → `fabric-review` → canonical.  
**Never:** auto-promote logs/transcripts to canonical; invent cites without recall.

## Default workflow (agent)

1. **Pre-action gate** — run the Pre-action gating checklist above (`fab_recall(paths=..., session_id=...)` before Edit/Write).
2. **Rank by description** — use `must_read_if` / `impact` / summary; do **not** bulk-read every body.
3. **On demand body** — `Read` only selected `entries[].read_path` (native Read = body consumption).
4. **Apply or dismiss** — if a hit is wrong scope/outdated, say `dismissed: <id> (reason)`.
5. **Backlog / search pending** — `fab_pending action=list|search` (not glob local `.fabric/knowledge/pending`).
6. **Empty / surprising results** — run diagnostics (below); do not invent stable_ids.

## Scenario playbooks

Detailed scenario matrices live in `ref/scenarios.md`. Summary:

### A. About to edit code
- `fab_recall(paths=[files you will touch], intent="optional short goal")`
- Read at most the top few high-impact bodies
- Cite is automatic when recall paths overlap edits (no forced `KB:` line)

### B. User asks "why did we decide X?" / prior pitfall
- `fab_recall` with intent + related paths, or `fab_pending action=search query="..."`
- Prefer decisions/pitfalls with matching tags or paths
- Verify body before claiming the decision text

### C. Pending backlog
- `fab_pending action="list"` (optionally filter type/layer)
- Batch review via `fabric-review` skill when backlog is large

### D. Empty recall / "why didn't this surface?"
- Check: store bound? `semantic_scope`? broad vs narrow timing? wrong project bind?
- Prefer `fabric audit why-not-surfaced <id>` when available
- Re-run recall with narrower `paths` / clearer `intent`

### E. After a decision / wrong-turn
- Propose archive via `fabric-archive` (user-driven or wrong-turn-revert) — do not dump session logs as knowledge

## Failure paths (short)

| Symptom | Do |
| --- | --- |
| Empty candidates | Diagnose bind/scope/timing; widen intent carefully; do not fabricate ids |
| Too much noise | Prefer description ranking; read fewer bodies; tighten paths |
| Cite unresolved | Treat as debt; fix with real recall or dismiss explicitly |
| propose fails write-target | Check MCP cwd / store bind / write-target (not a recall bug) |

## Out of scope (this skill)

- Capturing terminal/test logs as knowledge (evidence tools)
- Workflow orchestration (maestro-flow)
- Replacing doctor/install/sync CLIs

## Related tools

- MCP: `fab_recall`, `fab_pending`, `fab_propose`, `fab_review`
- Skills: `fabric-archive`, `fabric-review`, `fabric-store`, `fabric-sync`
- CLI: `fabric doctor`, `fabric audit cite`, `fabric store …`
