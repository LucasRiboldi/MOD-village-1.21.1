# Phase 3 — REVIEW macro-phase (ref)

> **Loaded on demand, and as ONE hop.** Everything between "I have candidates" and "I am ready to call `fab_propose`": what TYPE it is, which STORE it lands in, WHEN it surfaces, WHO it is for, and what it LINKS to.
>
> These were four files (3 / 3.5 / 3.6 / 3.7) until 2026-08-11. They are read in sequence on every single archive run, and the decisions interlock — `layer=personal` forces broad + empty paths, and the audience axis only exists when `layer=team`. Four hops to make one interlocking decision was the cost of the split (W3 T4).

**The three axes are orthogonal** — do not collapse them (KT-MOD-0001):
`layer` = which physical STORE · `relevance_scope` (derived from `paths`) = WHEN it surfaces · `semantic_scope`/`audience` = WHO it is for.

---

## 3 — Classify: type, layer, slug, batch review

### Five Knowledge Types (verbose)

- **model** — A reusable mental abstraction or domain object schema. Worth-archive signal: the user names something ("the X pattern", "the Y phase"). Skip-it signal: ad-hoc terminology used once. Positive: "Wave-1/Wave-2 task DAG decomposition for parallel-safe planning". Negative: "the thing we did just now" (too thin, no reusable abstraction).
- **decision** — A choice between alternatives with rationale. Worth-archive signal: ≥2 options were weighed AND a rationale was given. Skip-it signal: the choice was forced by external constraint with no real alternative. Positive: "Single .cjs hook script over three per-client scripts — rationale: identical stdout JSON shape across Claude/Codex". Negative: "Used the existing fab_propose schema" (no alternative was considered).
- **guideline** — A normative rule for future similar situations. Worth-archive signal: the user said "always" / "never" / "from now on". Skip-it signal: a one-off preference that won't generalize. Positive: "Slug naming: kebab-case, 2-5 words, 20-40 chars, semantic core only". Negative: "Use 4-space indent in this one file" (too narrow).
- **pitfall** — A trap that wasted time and is non-obvious. Worth-archive signal: a bug took >15 min to diagnose AND is repeatable. Skip-it signal: a typo or one-time API quirk. Positive: "deepMerge replaces arrays — hooks.Stop[] needs special-case append-with-dedupe". Negative: "Forgot a comma in JSON" (too obvious). **Name the root cause with a label from the vocabulary below** — a symptom-only pitfall is unretrievable.
- **process** — A multi-step procedure with a stable shape. Worth-archive signal: the steps were executed in a specific order AND the order matters. Skip-it signal: a one-shot script with no reusable structure. Positive: "fab_review approve = counter++ → frontmatter inject → git mv → meta rebuild → event append (5 atomic steps)". Negative: "Ran the tests, then committed" (trivial, no reusable shape).

### Pitfall root-cause vocabulary (closed list — pick exactly one)

A pitfall entry whose body only narrates the symptom is unretrievable: the next agent hits the same trap wearing different clothes and no keyword matches. Name the **root cause** in the summary using one of these seven labels, then describe the symptom. The labels are not invented — they are the clusters that actually appear across the 83 pitfalls in the corpus (2026-08-11 census). If a candidate genuinely fits none of them, that is a signal worth flagging in `session_context`, not a licence to coin an eighth on the spot.

| Label | The root cause | Corpus shape |
|---|---|---|
| `dual-truth` | One fact is maintained in two places, so it drifts. Includes "the comment/schema says X, the code does Y". | runtime default vs schema default; CLI and hook rendering the same thing separately; a lock whose comment claims coverage it doesn't have |
| `built-not-wired` | The capability exists and is reachable-looking, but nothing calls it — or a schema declares a field the implementation never reads. Green tests, dead feature. | detection restored while the `--fix` arm stayed empty; zod `strip` silently dropping an undeclared field; declared escape hatch never read |
| `oracle-lied` | The test or metric is itself broken, so it reports the wrong colour. The failure is in the judge, not the subject. | mutation range that scores itself; property test aimed at the value axis instead of the failure axis; wrong denominator; wall-clock flake |
| `probe-lied` | The survey/search tool under- or over-reports, so the *judgement* built on it is wrong. Distinct from `oracle-lied`: the measurement is fine, the enumeration isn't. | NUL byte making grep treat a file as binary; substring match mistaken for a reference; a stale census snapshot read as current fact |
| `stale-artifact` | The thing running is not the thing you edited — an installed copy, a global CLI, a long-lived process, an old path. | MCP server running the published copy; install timestamp predating the fix; lint script pointing at a moved path |
| `scope-mismatch` | State or a path root is resolved at the wrong scope: global vs per-session, workspace vs store, cwd vs project root. | a global counter cached in a per-workspace snapshot; hook using `process.cwd()`; single-slot sidecar under concurrent windows |
| `name-smuggled-premise` | A name carries an unexamined claim — "rename to Y" assumes X and Y are the same thing; a field name implies semantics it doesn't have. | `installed[]` read as "created this run"; an alias whose literal value equals a category word; false-friend alignment |

Two labels are legitimate on one entry only when the pitfall is genuinely the *interaction* (e.g. a `stale-artifact` that only bites because a `probe-lied` hid it). Prefer one.

### The repeat question — ask it on every pitfall

Before proposing, ask: **是不是同一个坑的第二次?上次的修复为什么没盖住这次?**

A meaningful share of the corpus is second-hits: an earlier fix corrected one instance of a class and the class re-fired somewhere else (anchor counting fixed while the value gate stayed global; a swallowed field fixed in one action and still swallowed in another). When the answer is yes:

- Put the **gap in the previous fix** in the summary, not just the new symptom — "上次只修了 X 面,Y 面同因未覆盖" is the reusable content; the second symptom alone is noise.
- Record the earlier entry as a `related` edge in Phase 3.6 rather than filing an independent lookalike. Two entries describing one class is how a KB stops being searchable.
- If the previous fix was correct but never propagated, the label is usually `dual-truth` or `built-not-wired`, not whatever the new symptom looks like.

When the answer is no, one line of `session_context` saying so is enough — the point is that the question gets asked, not that it gets a long answer.

### Slug Naming — examples

Passing examples: `wave-1-parallel-task-dag` (4 words, 24 chars), `deepmerge-array-replace-trap` (4 words, 28 chars).

Failing examples: `the_solution` (underscore + article), `fix` (1 word, too short), `how-we-decided-to-handle-the-merge-conflict-in-stop-hook-config` (overlong).

### Batch Review Template (bilingual)

Present all candidates in a single screen. UX i18n Policy classes 1 + 3 — the roll-up structure AND the per-candidate `Confirm?` prompt are bilingualized; protected tokens (`relevance_scope`, `relevance_paths`, `narrow`, `broad`, `layer`, `team`, `personal`, `pending_path`, etc.) appear verbatim in BOTH variants. Field VALUES (slugs, file paths, type/layer enum strings like `decision` / `team`) are data and are NOT translated.

#### en variant (`fabric_language === "en"`)

```md
# Archive Review — N candidates

## C1 [type=decision] [layer=team] [relevance_scope=narrow] slug=wave-1-parallel-task-dag
Summary: <1-2 sentences capturing the observation>
Layer reasoning: <which 强 team / 强 personal signal applied, or default team>
Scope reasoning: <why narrow or broad — see Phase 3.5>
relevance_paths: ["packages/cli/src/commands/plan.ts", "packages/cli/templates/**/*.md"]
Confirm? (Y to accept, edit type/layer/slug/relevance_scope/relevance_paths inline, N to skip)

## C2 [type=pitfall] [layer=team] [relevance_scope=broad] slug=deepmerge-array-replace-trap
Summary: ...
Layer reasoning: ...
Scope reasoning: ...
relevance_paths: []
Confirm? ...
```

#### zh-CN variant (`fabric_language === "zh-CN"`)

```md
# 归档 Review — N 条候选

## C1 [type=decision] [layer=team] [relevance_scope=narrow] slug=wave-1-parallel-task-dag
摘要: <1-2 句捕捉该观察>
Layer 判定: <命中哪条 强 team / 强 personal 信号，或默认 team>
Scope 判定: <为什么 narrow 或 broad — 见 Phase 3.5>
relevance_paths: ["packages/cli/src/commands/plan.ts", "packages/cli/templates/**/*.md"]
确认？(Y 接受 / 内联编辑 type/layer/slug/relevance_scope/relevance_paths / N 跳过)

## C2 [type=pitfall] [layer=team] [relevance_scope=broad] slug=deepmerge-array-replace-trap
摘要: ...
Layer 判定: ...
Scope 判定: ...
relevance_paths: []
确认？...
```

The user MAY edit type/layer/slug/relevance_scope/relevance_paths inline before confirming. The user MAY skip individual candidates without rejecting the whole batch. Inline-editing `[relevance_scope=...]` triggers a re-derivation of `relevance_paths` per the Phase 3.5 rules (narrow ⇒ recompute from edit_paths; broad ⇒ force `[]`).

---

## 3.5 — Relevance scope + relevance_paths

### relevance_paths derivation algorithm (rc.37 multi-signal — NEW-7)

rc.37 NEW-7 widens Step 1 from the rc.5 single-signal (`edit_paths` only) to three sources:

1. **`edit_paths`** — files modified by `Edit` / `Write` / `MultiEdit` tool calls. The primary activation signal: if the agent CHANGED a file, the knowledge derived in this session most likely applies there.
2. **`read_paths`** — files inspected via `Read` / `Grep` / `Glob` without modification. Secondary signal: read-only inspection often anchors the applicability surface even when no write happened (e.g. discovering that a pitfall surfaces in a getter that the agent only READ).
3. **`user_mentioned_paths`** — paths the user typed verbatim in messages (`packages/server/src/foo.ts`, `\`packages/cli/**/*.ts\`` etc.). Strongest signal of all: an explicit user-named path is ground-truth applicability surface, independent of what the agent did.

```
Step 1: COLLECT (rc.37 NEW-7 — three sources)
  edit_paths = []
  read_paths = []
  user_mentioned_paths = []

  // 1a — edit signal (rc.5 primary)
  Scan session transcript for tool_use entries where
    tool_use.name ∈ {Edit, Write, MultiEdit}
  Extract the file_path argument from each, push into edit_paths.

  // 1b — read signal (rc.37 NEW-7 secondary)
  Scan session transcript for tool_use entries where
    tool_use.name ∈ {Read, Grep, Glob}
  Extract the file_path / path / glob argument from each, push into read_paths.

  // 1c — user-mentioned signal (rc.37 NEW-7 ground truth)
  Scan user messages for token sequences matching workspace-relative
  path patterns: `<segment>/<segment>/...<ext>` or `<segment>/**` or
  ``<path>`` (backtick-quoted). De-dupe and push into user_mentioned_paths.

Step 2: DEDUPE + CLASSIFY
  // Union all three sources for the relevance_paths candidate set.
  candidate_paths = unique(edit_paths ∪ user_mentioned_paths)
  // read_paths stay separate — they become evidence_paths (Step 6) rather
  // than activation triggers. A path that appears in BOTH edit_paths and
  // read_paths goes to candidate_paths (writes dominate reads).
  evidence_candidate_paths = unique(read_paths \ edit_paths)

Step 3: BLACKLIST FILTER (applies to BOTH candidate sets)
  Drop paths matching any of:
    - **/*.<ext>          where <ext> is a single trivial extension on a single file
                          (i.e. avoid emitting bare **/*.md as a relevance pattern)
    - Repo-root single files: README.md, package.json, package-lock.json,
      pnpm-lock.yaml, tsconfig.json, .gitignore, LICENSE, CHANGELOG.md

Step 4: PUBLIC-PREFIX GENERALIZE (depth ≤ 2, minGroupSize = 2)
  Group remaining candidate_paths by common prefix.
  For each group of ≥ 2 sibling paths sharing a prefix:
    - Compute longest common directory prefix
    - Limit generalization depth: at most 2 levels below the common prefix
    - Emit glob: <common-prefix>/**/*.<ext>  (or <common-prefix>/**/<filename>)
  Singleton paths (group size = 1) are kept as-is (literal path, no glob).
  (Evidence paths are NOT generalized — they stay literal so plan-context
  retrieval can do exact-match recall lookups.)

Step 5: SCOPE GATE
  IF relevance_scope == broad → relevance_paths = []  (force empty regardless of candidate_paths)
  IF relevance_scope == narrow → relevance_paths = result of Step 4

Step 6: ATTACH evidence_paths to FRONTMATTER (rc.37 NEW-7 upgrade)
  Pass evidence_candidate_paths (from Step 2, post-blacklist Step 3) to
  fab_propose as the `evidence_paths` input field. Server writes
  them to frontmatter `evidence_paths: [...]` (NOT to body `## Evidence`).
  This makes evidence consumable by plan-context retrieval as structured
  data instead of forcing markdown re-parsing every recall. The legacy
  body `## Evidence` block stays for back-compat readers but is no longer
  the source of truth.
```

### Worked generalization example

Edit history during session:

```
packages/server/src/services/extract.ts
packages/server/src/services/review/review.ts
packages/server/src/services/promote.ts
packages/cli/src/commands/plan.ts
README.md
```

Step 1-2 (collect + dedupe): all 5 unique.
Step 3 (blacklist): drop `README.md` (repo-root single file).
Step 4 (generalize, depth ≤ 2, minGroupSize = 2):
- `packages/server/src/services/{extract,review,promote}.ts` → group size 3 ≥ 2, common prefix `packages/server/src/services/`, glob: `packages/server/src/services/**/*.ts`
- `packages/cli/src/commands/plan.ts` → group size 1, kept literal.

Step 5 (assume `relevance_scope=narrow`):

```json
"relevance_paths": [
  "packages/server/src/services/**/*.ts",
  "packages/cli/src/commands/plan.ts"
]
```

If `relevance_scope=broad` had been chosen instead, `relevance_paths` would be `[]` regardless of the above.

### Inline-edit support during batch review

The user MAY inline-edit `[relevance_scope=...]` in the batch review. When this happens:

- Edit changes `narrow → broad`: clear `relevance_paths` to `[]`.
- Edit changes `broad → narrow`: re-run Steps 1-4 of the derivation algorithm to recompute.
- The user MAY also directly inline-edit `relevance_paths` to a custom array; treat this as authoritative and skip auto-derivation.

---

## 3.6 — Related edges (§7 graph)

For each candidate, identify the **`related`** graph edges to other KB entries — the store-qualified `stable_id`s this entry semantically links to (the decision it supersedes, the pitfall it explains, the model it instantiates). You discovered these ids during the session via `fab_recall` / plan-context, so cite the ones you actually saw, NEVER invent stable_ids.

Because `fab_propose` has no dedicated `related` input, record the candidate edges as one line inside `session_context` (e.g. `related: team:KT-DEC-0007, team:KT-PIT-0011`) so they survive to approve-time frontmatter authoring (`fabric-review` writes the canonical `related: [...]` frontmatter).

### §4 privacy iron law — KT→KP is FORBIDDEN

A **team** (`KT-*`) entry's `related` MUST NOT point at a **personal** (`KP-*`) id: that would write a personal-knowledge topology pointer into a shared store.

| Edge | Allowed? |
| --- | --- |
| `KT→KT` | ✅ |
| `KP→KP` | ✅ |
| `KP→KT` | ✅ |
| `KT→KP` | ❌ FORBIDDEN |

When unsure whether a target is personal, OMIT the edge.

---

## 3.7 — Semantic scope (audience axis)

### The three orthogonal axes (KT-MOD-0001)

A knowledge entry is positioned on three independent axes — do NOT collapse them:

| Axis | Field | Values | Decided by |
| --- | --- | --- | --- |
| **Store** (physical repo, privacy boundary) | `visibility_store` | `team` shared store / `personal` store | Phase 3 `强 team` / `强 personal` heuristic |
| **Audience** (logical resolution coordinate) | `semantic_scope` | `team` / `project:<id>` / `personal` / `org:<…>` | **this phase** |
| **Display** (how broadly it surfaces) | `relevance_scope` | `narrow` / `broad` | Phase 3.5 |

Phase 3 picks the STORE (team vs personal). This phase picks the AUDIENCE *within* the shared team store: is the entry for the whole team across all projects (`team`), or only for the current project (`project:<active_project>`)?

### When this phase runs

- **Runs** only when `layer=team` AND `.fabric/fabric-config.json` has a non-empty `active_project`.
- **Skips** otherwise — the engine auto-derives `semantic_scope` at the write path (`resolveWriteScopeMeta`): `layer=personal` → `personal`; `layer=team` with no `active_project` → `team`. An explicit input always wins over auto-derivation (`semanticScope ?? defaultWriteScope(...)`).

### Decision tree (per team candidate, when active_project is set)

```
Is the knowledge tied to THIS project's code / business domain / workspace paths?
├─ YES (this-project-only) → OMIT semantic_scope
│                            → engine derives `project:<active_project>`  ← DEFAULT, most candidates
└─ NO  (team-wide, cross-project: methodology, team convention, tooling
        not bound to this repo) → pass explicit `semantic_scope: team`
                                 → entry stays visible across every project
```

**Why the explicit escape hatch matters.** Without this step, *every* team archive in a project-bound repo is silently narrowed to `project:<active_project>`. Genuinely cross-project team knowledge (a naming convention, a review checklist, a tooling decision) would be trapped inside one project and invisible elsewhere. `semantic_scope: team` is the only way to opt out of the project narrowing.

### Worked examples (active_project = `fabric-v2`)

| Candidate | layer | semantic_scope | Result |
| --- | --- | --- | --- |
| "The resolver's two-axis tie-break lives in `cross-store-write.ts`" | team | OMIT | `project:fabric-v2` — binds this repo's code |
| "We always write commit messages in Chinese, type: prefix" | team | `team` | `team` — team convention, spans every project |
| "Black-edge sprite root cause = inverted `premultiplyAlpha`" (a different game repo's domain) | team | `team` | `team` — not about fabric-v2 |
| "VoiceRoom 组件被本 app 多个玩法复用"(同一 app 内跨功能,**不**跨项目) | team | OMIT | `project:<active>` — 跨玩法复用 ≠ 跨项目;app 内共享组件仍绑本项目,**不是** team |
| First-person editor preference | personal | (n/a) | `personal` — store=personal, phase skipped |

### Inline-edit support during batch review

The user MAY inline-edit `[semantic_scope=...]` in the batch review. Treat it as authoritative: a switch to `team` drops the project narrowing; a switch to `project:<active_project>` (or OMIT) restores the default. Personal-layer candidates have no `semantic_scope` choice — they are always `personal`.

---
