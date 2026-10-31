# Phase 1.5 — First-run Onboard Phase (ref)

> **Loaded on demand.** SKILL.md hot path only runs this when entry_point ∈ {E2_explicit_user_invoke, E4_user_range_rollback} AND `fabric onboard-coverage --json` reports `missing.length > 0`. For E1/E3/E5 entries OR fully-covered workspaces, this entire phase is skipped — no reason to load.

## Phase 1.5 — First-run Onboard Phase

#### Phase 1.5 Trigger Gate (rc.25 — entry-context aware)

Before running ANY of the onboard coverage steps below, evaluate the
**entry-context gate**. Onboard slot collection is an interactive,
one-time project-tone capture flow that REQUIRES live user dialogue.
Non-user-active entries (hook / AI self-trigger / cron) either interrupt
the user mid-work or run unattended where dialogue is impossible, so
they MUST skip Phase 1.5 entirely and fall through to Phase 0.

Read `context.entry_point` — already determined in **Phase 0 Range
Resolution** (see TASK-04 / Phase 0 section above). The 5-entry model
is the canonical taxonomy for this gate.

##### Entry-context detection rules

| Entry | Symbol | Detection rule (LLM-native, evaluated at skill entry) |
|-------|--------|-------------------------------------------------------|
| **E1** | `hook_passive` | stdout JSON `{decision:'block', ...}` from `fabric-hint.cjs` detected at skill entry (the Stop-hook reminder path). |
| **E2** | `explicit_user_invoke` | User prompt is a direct invocation: `fabric archive` / `/fabric-archive` / `archive what we just did` / `归档一下` / similar imperative. |
| **E3** | `ai_self_trigger` | DEFAULT else-branch: the AI invoked `fabric-archive` itself, following the AGENTS.md self-archive policy (a clear archival signal — `User-driven normative` or `Wrong-turn-and-revert` — surfaced this turn), AND none of the affirmative non-AI entry signals are present (no E1 hook `{decision:'block'}` block, no E2/E4 explicit user invocation, no E5 cron literal). v2.2 C1 W3b retired the AI-printed routing marker — E3 is now the deterministic else-branch, so routing no longer depends on the AI emitting an exact magic string (every other entry has its own non-AI affirmative detector; E3 is what remains). The signal category does not need to be conveyed to the skill — Phase 2.5's viability gate re-derives it independently. |
| **E4** | `user_range_rollback` | Prompt contains a **range hint** (parsed in Phase 0 — e.g. `今日` / `上周` / `rc.20`) AND the user is invoking. Sub-mode of E2. |
| **E5** | `cron` | Prompt contains literal `今日复盘` / `daily recap` / `daily-archive` AND no human is present (running under `/loop`, OS cron, or scheduled trigger). |

##### Gate decision

```
IF context.entry_point ∈ {E2_explicit_user_invoke, E4_user_range_rollback}:
    → gate = PROCEED       # user is live, dialogue is possible
    → continue to Step 1 (Check coverage) below
ELSE (E1_hook_passive | E3_ai_self_trigger | E5_cron):
    → gate = SKIP           # no live user, onboard prompting would misfire
    → emit one-line log: "Phase 1.5 skipped (entry=<E1|E3|E5>, no live user)"
    → proceed directly to Phase 2
```

##### Rationale

Onboard slot collection is a one-time project-tone capture flow that
requires user dialogue. Non-user-active entries (hook / AI / cron)
interrupt the user mid-work or run unattended where dialogue is
impossible, so they MUST skip Phase 1.5. The S5 slot semantics
(`tech-stack-decision`, `architecture-pattern`, ...) are user-validated
baselines — populating them from a hook fire-and-forget or a cron daily
recap would defeat the purpose of capturing _user-confirmed_ project
tone.

##### Tradeoff (documented in CHANGELOG)

A first-time user whose ONLY invocations ever come via hook (never an
explicit `/fabric-archive`) will not see the onboard prompt; the 5
onboard slots remain empty. Mitigation: documentation tells users to
run an explicit `fabric archive` at least once to populate the onboard
baseline.

##### Worked example

```
$ /loop 24h /fabric-archive 今日复盘
  → cron context, no live user
  → Phase 0 detects literal "今日复盘" + no-human marker
  → context.entry_point = E5_cron
  → Phase 1.5 Trigger Gate evaluates: E5 ∉ {E2, E4} → SKIP
  → emit log "Phase 1.5 skipped (entry=E5, no live user)"
  → proceed directly to Phase 2 (collect candidates for daily window)
```

Contrast with E2:

```
$ /fabric-archive
  → user typed explicit invocation
  → Phase 0: context.entry_point = E2_explicit_user_invoke
  → Phase 1.5 Trigger Gate evaluates: E2 ∈ {E2, E4} → PROCEED
  → run Step 1 (Check coverage) below
```

---

After F8a removed the auto-`fabric scan` baseline pipeline, a freshly installed
Fabric workspace ships with an EMPTY mounted store `knowledge/` tree. Five fixed
**S5 onboard slots** capture the "project tone" baseline that the AI needs
for high-quality plan_context retrieval from day one:

- `tech-stack-decision` — primary languages / frameworks / runtime stack
- `architecture-pattern` — module layout, service boundaries, layering rules
- `code-style-tone` — naming / formatting / idiom conventions the project enforces
- `build-system-idiom` — build tool quirks, scripts, deploy pipeline shape
- `domain-vocabulary` — business / product terminology that names code entities

This phase runs ONCE per archive-skill invocation, BEFORE Phase 2 evidence
gathering, so coverage state is fresh for the session.

#### Step 1 — Check coverage

Invoke `fabric onboard-coverage --json` and parse the JSON payload:

```bash
fabric onboard-coverage --json
```

Expected shape:

```json
{
  "filled":    { "tech-stack-decision": ["KT-DEC-0012"], ... },
  "missing":   ["architecture-pattern", "code-style-tone"],
  "opted_out": ["domain-vocabulary"],
  "total": 5
}
```

#### Step 2 — Decide

```
IF missing.length === 0:
    → skip Phase 1.5 entirely; proceed to Phase 0.
ELSE:
    → ask the user how to handle the missing slots (Step 3).
```

#### Step 3 — Prompt user

Present a single roll-up listing each missing slot. UX i18n Policy class 5
applies: the `header` + `question` strings are translated per
`fabric_language`; the `options[]` routing keys stay English.

```ts
AskUserQuestion({
  header: "Onboard coverage",  // zh-CN: "首装基调覆盖"
  question:
    "KB is missing the following project-tone slots: " +
    missing.join(", ") +
    ". Tour the project and propose pending entries for each?",
  options: ["fill-all", "fill-each", "dismiss-all", "skip"]
})
```

`fab_propose` is called with `onboard_slot: <slot>` set so each
proposed entry counts toward coverage once approved via fab_review.

| User choice    | Action |
|----------------|--------|
| `fill-all`     | For EACH slot in `missing`, run Step 4 (Tour-and-propose). All proposals share session_id; one batch review at the end (Phase 3). |
| `fill-each`    | Loop slot-by-slot through `missing`. Per slot: ask user `confirm | dismiss | skip` (per-slot AskUserQuestion); `confirm` → run Step 4; `dismiss` → `fabric config dismiss-slot <slot>`; `skip` → leave for next archive run. |
| `dismiss-all`  | For EACH slot in `missing`, invoke `Bash("fabric config dismiss-slot <slot>")`. Print a one-line confirmation each. Skip to Phase 0. |
| `skip`         | No-op. Slots remain in `missing` for the next archive run. Skip to Phase 0. |

#### Step 4 — Tour-and-propose (per-slot)

For each slot to fill, the LLM independently sources slot-specific evidence
from the project (no user prompt — this is a Read-only tour):

| Slot                     | Source files (LLM should Read these) |
|--------------------------|---------------------------------------|
| `tech-stack-decision`    | `package.json` (+ lockfile), `pyproject.toml` / `Cargo.toml` / `go.mod`, `tsconfig.json`, root README |
| `architecture-pattern`   | Top-level dir tree (`ls -F`), 1-2 entry-point files (`src/index.ts`, `main.go`, etc.), framework-config files (`next.config`, `vite.config`, `astro.config`) |
| `code-style-tone`        | `.editorconfig`, `prettier.config.*`, `eslint.config.*`, `biome.*`, `.prettierrc*`, framework lint config, 2-3 representative source files for naming-pattern inference |
| `build-system-idiom`     | `package.json` `scripts` block, `Makefile`, `taskfile.yaml`, CI yml (`.github/workflows/*.yml`), Dockerfile if present |
| `domain-vocabulary`      | README, `docs/*.md`, top-level `src/` directory names (often domain-aligned), public API entry types |

After Read-ing the slot-specific sources, classify the observation:

- `tech-stack-decision` → type=`decisions`, `proposed_reason=decision-confirmation`
- `architecture-pattern` → type=`models`, `proposed_reason=new-dependency-or-pattern`
- `code-style-tone` → type=`guidelines`, `proposed_reason=explicit-user-mark` (the project ITSELF is the mark)
- `build-system-idiom` → type=`processes`, `proposed_reason=new-dependency-or-pattern`
- `domain-vocabulary` → type=`models`, `proposed_reason=new-dependency-or-pattern`

Call `fab_propose` with the inferred fields PLUS `onboard_slot:
<slot>`. The pending file's frontmatter will carry the slot label, and the
next `fabric onboard-coverage` run will see the slot as filled (once approved
via fab_review).

Example:

```ts
mcp__fabric__fab_propose({
  source_sessions: ["<current-session-id>"],
  recent_paths: ["package.json", "tsconfig.json"],
  user_messages_summary: "Project uses TypeScript + pnpm workspace + Vitest. Node 20 LTS target. ESM-only.",
  type: "decisions",
  slug: "primary-tech-stack",
  layer: "team",
  relevance_scope: "broad",        // tech stack applies everywhere
  relevance_paths: [],
  proposed_reason: "decision-confirmation",
  session_context:
    "Session goal: capture onboard tech-stack baseline.\nTurning point: read package.json + tsconfig.json + pnpm-workspace.yaml; stack confirmed.",
  onboard_slot: "tech-stack-decision",    // ← claims the slot
  tags: ["typescript", "nodejs", "pnpm", "vitest"]
})
```

#### Onboard phase constraints (DO NOT TRANSLATE)

- MUST run BEFORE Phase 2 evidence gathering — onboard is a separate flow,
  not interleaved with session-archive candidates.
- MUST call `fabric onboard-coverage --json` before deciding; never assume
  coverage state.
- NEVER fill a slot that is in `opted_out` — `fabric onboard-coverage` already
  excludes those from `missing`, but the Skill MUST NOT re-propose them
  even if the user asks "fill all of them" — the dismiss is intentional.
- NEVER prompt the user when `missing.length === 0` — silent skip.
- NEVER set `onboard_slot` on a regular session-archive candidate in
  Phase 4 — that field is RESERVED for the onboard phase. Mixing the
  two would let session-archive proposals masquerade as onboard
  coverage and let any random pending file claim a slot.
- MUST emit `onboard_slot: <slot>` verbatim — the slot name is one of
  the locked S5 strings (tech-stack-decision / architecture-pattern /
  code-style-tone / build-system-idiom / domain-vocabulary). The
  fab_propose schema enum will reject anything else.
