# Phase 2.5 — Viability Gate (ref)

> **Loaded on demand.** SKILL.md hot path retains the signal lists, gate decision pseudocode, and entry-point branching summary. This file holds the verbose explanations for each signal, gate-FAIL message variants (zh-CN/en), the events.jsonl 4KB atomicity constraint note, and rationale.

## Archive signals — verbose explanation (rc.37 NEW-4 simplified 8 → 3)

Scan `user_messages_summary` + `recent_paths` + the events tail collected in Phase 2. The legacy 8-signal list was simplified in v2.0.0-rc.37 NEW-4 to **3 major categories**, mirroring the AGENTS.md Self-archive policy rc.37 NEW-2 simplification. Legacy signal names remain valid scoring inputs for back-compat (any path that fires a legacy signal also fires the corresponding category):

### Category 1 — User-driven knowledge expression

Covers legacy signals #1 (normative language) + #6 (decision confirmation) + #7 (dismissal-with-reason). All three are "user message contains structured knowledge worth keeping":

- **Normative language** — user said `always` / `never` / `from now on` / `下次注意` / `记一下` / `以后` / `永远不要`. Strongest single cue.
- **Decision confirmation** — ≥ 2 alternatives were weighed AND a rationale was given before settling. The rationale is the archivable knowledge.
- **Dismissal-with-reason** — user rejected an approach AND stated why. The why is archivable, not the dismissal itself.

### Category 2 — Reflective discovery

Covers legacy signals #2 (wrong-turn-and-revert) + #3 (long diagnostic loop) + #5 (new pattern emergence). All three are "AI execution surfaced a non-obvious insight":

- **Wrong-turn-and-revert** — a path was edited, then reverted (or partially undone) after diagnosis. The why-not lives in the revert.
- **Long diagnostic loop** — an issue took > 15 minutes (or > ~10 tool turns) of debugging before resolution. Non-obvious cause worth capturing.
- **New pattern emergence** — a reusable abstraction or naming convention was named in-session ("the X phase", "the Y pattern", "let's call this Z").

### Category 3 — Concrete artifact change

Covers legacy signals #4 (new dependency) + #8 (process formalization). Both surface in tangible workspace artifacts:

- **New dependency adoption** — a new package / library / external tool was introduced (`package.json` / `pyproject.toml` / `Cargo.toml` diff adds a dep).
- **Process formalization** — a multi-step procedure was executed in a specific order AND the order was identified as load-bearing.

## Anti-archive signals — verbose explanation

These force the gate to FAIL **unless** an archive signal also fires (i.e. anti-signals are dominated by any archive signal per the gate decision rules):

1. **Typo-only edits** — the entire session is whitespace / spelling / formatting changes. No semantic content to archive.
2. **Pure refactor** — rename / move / extract with no behavior change AND no naming convention being established.
3. **Narrow rename request** — user asked to rename one symbol / file with no rationale. Zero generalization potential.
4. **Duplicate of existing canonical** — v2.0.0-rc.37 NEW-4: this check is now **mandatory** (was "do a quick Glob before deciding"). Pre-PASS MUST step: for each candidate, call `fab_recall(paths=[<the code paths the candidate is about>])` so the MCP read path RANKS the mounted stores against it, and `fab_pending action="list"` for the un-reviewed backlog. If duplicate found → drop candidate. Silently writing a near-duplicate is the highest-noise failure mode.

   **MUST NOT use `fab_pending action="search"` for this gate.** Its query is a substring match over title/summary/tags/path, so an empty result means your guessed terms missed — not that no duplicate exists. Observed: six hand-picked terms returned nothing while two of the three candidates duplicated canonical entries that `fab_recall` surfaced immediately on the same paths. A control query proves the PROBE is alive, never that the TERMS were right.

## Gate-FAIL user messages (E2 / E4 only)

For the user-active branch, the gate-FAIL message variants are:

### zh-CN variant

```
本次会话为常规执行，无新知识可归档（gate=<reason>）。如需强制归档，请显式调用 fabric-archive。
```

### en variant

```
Current session is routine execution; no new knowledge to archive (gate=<reason>). To force-archive, explicitly invoke fabric-archive.
```

In BOTH branches: do NOT proceed to Phase 3, do NOT call any MCP tool. The legacy `knowledge_archive_aborted` event line (`{"ts":"...","kind":"knowledge_archive_aborted","reason":"<reason>","session":"<id>"}`) MAY be appended in addition to the mandatory Phase 4.5 `session_archive_attempted` event — they serve different audit purposes (legacy abort reason vs new outcome state machine) and the two coexist during the rc.25 transition window.

## events.jsonl Constraint Note (POSIX 4KB atomicity)

Event lines appended to `.fabric/events.jsonl` are subject to POSIX single-write atomicity: only writes ≤ 4KB (`PIPE_BUF`) are guaranteed atomic via `Bash: echo "..." >> file`. Lines exceeding 4KB risk interleaved corruption under concurrent skill + server writes to the same ledger.

Skills MUST ensure:

- Each event JSON line is a **single line** (no embedded newlines; escape `\n` in any string value).
- `session_context` and other free-form text fields **self-truncate** to keep the entire serialized line under 4KB. Suggested per-field caps: `session_context` first 500 chars; `source_sessions` cap at 5 entries; `recent_paths` cap at 20 entries; `user_messages_summary` first 500 chars.
- If approaching the 4KB ceiling after the per-field caps, drop optional fields (e.g. tags / extra metadata) **before** truncating semantic content (the summary / context that carries the actual observation).
- This constraint applies to any event the skill itself appends (e.g. the abort signal above); MCP-server-side appends (via `appendEventLedgerEvent`) are already line-length-bounded server-side.
