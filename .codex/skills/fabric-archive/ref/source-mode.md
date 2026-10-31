# Source Mode — cold-start bootstrap (ref)

> **Loaded on demand, and as ONE hop.** Source mode swaps only fabric-archive's GATHER stage: it mines `git log` + `docs/*.md` instead of the digest ledger. Everything downstream (classify / scope / `fab_propose`) is the shared default-mode pipeline — this file never restates it.
>
> Five sections, all of which a source-mode run needs in sequence: mine → dedupe → roll up, plus the checkpoint that makes it resumable and the recovery path for when it isn't. They lived in five separate files until 2026-08-11; splitting one linear procedure across five `Read` calls cost five hops to learn one thing (W3 T4).

**Scope lock (NON-NEGOTIABLE).** Every mined entry is `relevance_scope="broad"` + `relevance_paths=[]`. LLM-inferred narrow lies about applicability; narrowing is deferred to `fab_review.modify` after import. Rationale in section A.

---

## A. Mining — git log + docs (Phase 2)

### Mandatory Scope Rule — Always Broad + Empty Paths (Q-1 Resolution)

**EVERY `fab_propose` call issued from this skill MUST set:**

- `relevance_scope = "broad"`
- `relevance_paths = []`

This is non-negotiable and applies to BOTH Step 2.1 (git mining) AND Step 2.2 (docs mining). No exceptions, no per-candidate override, no Agent judgment.

**Rationale — why archive source mode cannot bind paths from git history:**

1. `archive source mode` is LLM-driven (mines git log + docs), not session-driven (no live `edit_paths` signal).
2. `git diff --stat` lists files touched by a commit, but those files are the commit's **effect surface**, not the **applicability surface** of the underlying observation. A pitfall surfaced by a fix in `packages/server/src/retry.ts` may apply to every retry call-site in the repo, not just that one file.
3. LLM-inferred `relevance_paths` from historical commit metadata produces false-narrow bindings — `relevance_paths` becomes a lie about applicability. Post-rc.37 A1 the server no longer filters by `relevance_scope`, so false-narrow does NOT hide knowledge from AI recall (every selectable entry is surfaced regardless of scope). The damage is now downstream: doctor lint accounting, future-AI judgment, and any consumer that reads `relevance_paths` literally treats the wrong globs as ground truth. Broad+[] keeps the metadata honest until the user has the real applicability surface in hand to declare narrow.
4. Doc-mined observations are usually architectural / cross-cutting (a `docs/architecture.md` "Why a monolith?" decision applies to the whole codebase, not just to `docs/`).

**Strict prohibitions — DO NOT attempt any of the following:**

- DO NOT derive `relevance_paths` from `git log --name-only` / `git show --stat` / `git diff` file lists.
- DO NOT derive `relevance_paths` from the path of a mined Markdown file (e.g. do NOT bind a `docs/architecture.md` observation to `["docs/**"]`).
- DO NOT extract path-shaped tokens from commit subjects / bodies / doc text and lift them into `relevance_paths`.
- DO NOT classify a candidate as `relevance_scope = "narrow"` under ANY heuristic.
- DO NOT copy the public-prefix-generalization logic from fabric-archive Phase 3.5 — that logic is valid only when bound to a real-time `edit_paths` signal from an active session, which archive source mode lacks.

**Cross-reference — archive source mode vs fabric-archive scope handling:**

| Skill            | Scope decision     | Why                                                                   |
|------------------|--------------------|-----------------------------------------------------------------------|
| `fabric-archive` | narrow OR broad, case-by-case per Phase 3.5 rules | Has live `edit_paths` from the active session — the actual applicability surface. |
| `archive source mode`  | ALWAYS broad + `[]` (this skill) | LLM-only, no live session signal; git-history paths are effect-surface, not applicability-surface. |

`fabric-archive`'s Phase 3.5 scope decision (narrow-vs-broad rules + public-prefix generalization + glob blacklist) is INTENTIONALLY MORE PERMISSIVE than archive source mode because archive has the data to bind safely. archive source mode is the STRICTER case.

**Post-import narrowing path — deferred to user, via `fab_review.modify`:**

After import completes, the user reviews each kept pending entry via `fabric-review`. When the user judges that an imported entry is actually narrow-scoped, they (or the reviewing Agent on their explicit instruction) issue:

```ts
mcp__fabric__fab_review({
  action: "modify",
  pending_path: "<the imported pending or its post-approval canonical path>",
  changes: {
    relevance_scope: "narrow",
    relevance_paths: ["packages/server/src/retry/**", "packages/server/src/lib/retry.ts"]
  }
})
```

This is the ONLY legal path for an imported entry to acquire `relevance_paths`. The narrowing decision is the user's, informed by the actual `relevance_paths` candidates they propose — not the skill's, inferred from git metadata.

**Lint backstop:** doctor lint #23 (`narrow_no_paths`) warns on any `relevance_scope=narrow` entry with empty `relevance_paths`. If this skill ever deviates from the broad+[] rule and writes narrow without paths, lint #23 catches the mistake post-hoc.

### Step 2.1 — Git Log Mining

Bash command (executed via the `Bash` tool — substitute `<window>` and `<commits-cap>` with values resolved from Phase 0.5 config load):

```bash
git log --since="<window> months ago" --pretty=format:"%H%n%s%n%b%n---ENDCOMMIT---" -n <commits-cap>
```

- `<window>` resolves to `import_window_first_run_months` on a first-run (default 60) or `import_window_rerun_months` on subsequent runs (default 2); first-run-vs-rerun is decided per the Phase 0.5 rule.
- `<commits-cap>` resolves to `import_max_commits_scan` (default 500).

Tolerate empty output (shallow clone or new repo). Cap the working set at the **`import_max_commits_scan`-most-recent commits (config-resolved)** regardless of date range to bound LLM context.

For each commit:

1. Inspect the conventional-commit prefix in the subject line. Strong signals:
   - `feat(...)` with a non-empty body → likely **decision** or **model** (a new capability landed; the body usually explains why)
   - `fix(...)` with body length >100 chars → likely **pitfall** (a bug worth diagnosing was non-trivial)
   - `refactor(...)` with body → likely **decision** (architectural choice was made)
   - `docs(...)` → usually a **guideline** if the body announces a convention; skip if it's just typo/reformat
   - `chore(...)`, `test(...)`, `ci(...)` → almost always skip (mechanical; no reusable insight)
2. Read the commit body. Extract the LLM-judged "core observation" — what would a future engineer want to know about this commit beyond the diff? Aim for 1–2 sentences in zh-CN (project fabric_language; mirror fabric-archive M3 style).
3. Apply the **Skip Decision Tree** below. If the commit is skip-worthy, record it in `p2_processed_commits[]` with `skipped: true` and move on.
4. For non-skipped commits, classify type / propose slug / draft summary. Then call `fab_propose` with the **mandatory broad + [] scope** (see "Mandatory Scope Rule" above):

```ts
mcp__fabric__fab_propose({
  source_sessions: ["fabric-archive-source-<ISO8601-date>"],   // T5: array form; stable per import run
  recent_paths: ["<files touched by this commit, capped at 20>"],   // provenance only, NOT a path-binding signal
  user_messages_summary: "<zh-CN 1-2 sentence summary of the commit's core observation; cite the commit sha as 'src=<sha7>'>",
  type: "decisions" | "pitfalls" | "guidelines" | "models" | "processes",
  slug: "<kebab-case 2-5 words derived from commit subject + body>",
  relevance_scope: "broad",                                          // MANDATORY — never "narrow" from archive source mode
  relevance_paths: [],                                               // MANDATORY — never derived from git history
  proposed_reason: "<inferred per Step 2.1.5 — varies>",
  session_context: "Imported from git log analysis. Origin: commit <sha7> (<subject 30 chars>). No live session — see commit body for full context.",
  // rc.23 TASK-006 four OPTIONAL triage fields — populate from commit body when clean, omit otherwise.
  intent_clues: ["<inferred trigger if commit body suggests one>"],
  tags: ["<lang/framework from recent_paths extensions>"],
  impact: ["<consequence stated in commit body / doc>"],
  must_read_if: "<one-line strongest trigger from commit's touched-path family>"
})
```

Note: `recent_paths` carries the touched-file list for **provenance display** only. It is NOT lifted into `relevance_paths` — those two fields serve different purposes and the prohibition on path inference from git history applies.

5. On success the server returns `{pending_path, idempotency_key}`. Append to `.fabric/.import-state.json`:
   - `p2_processed_commits[].push({sha: <full sha>, skipped: false, pending_path, type, slug})`
   - `last_checkpoint_at = <ISO8601 now>`
   Update is atomic via the 2-step `.tmp` + `mv` pattern documented in the **Atomic State Write** section under "Checkpoint Logic" below.
6. **Hard cap**: at most **`import_max_pending_per_run` new pending entries (config-resolved, default 10)** per Phase 2 run. When the cap is reached, mark `p2_cap_reached = true` and stop git-log iteration.

### Step 2.1.5 — Proposed Reason Inference (rc.7 T6)

For each non-skipped commit OR doc section, infer `proposed_reason` from prefix + body signal jointly. The 6 reasons below are the full enum accepted by `fab_propose` (schema-locked):

| Source signal | Body cue | Inferred reason |
|---|---|---|
| `feat(...)` commit | "vs" / "instead of" / "chose" / "rejected X for Y" | `decision-confirmation` |
| `feat(...)` commit | Announces new dep/lib/abstraction, no alternative cited | `new-dependency-or-pattern` |
| `fix(...)` commit | Cites wrong direction tried + reverted | `wrong-turn-revert` |
| `fix(...)` commit | Cites long diagnostic chain → root cause | `diagnostic-then-fix` |
| `refactor(...)` commit | Cites structural rationale (without "vs" alternatives) | `decision-confirmation` |
| `docs(...)` commit | Announces convention ("always X" / "never Y") | `explicit-user-mark` |
| Any commit | Body explicitly rejects an approach + states why | `dismissal-with-reason` |
| Doc section | "Why we chose X over Y" heading | `decision-confirmation` |
| Doc section | "Don't do Y because..." section | `dismissal-with-reason` |
| Doc section | "Always" / "Never" guidelines | `explicit-user-mark` |
| Doc section | Architecture/design narrative (descriptive, no choice rationale) | `new-dependency-or-pattern` |

**Edge cases:**

- `chore(` / `test(` / `ci(` should already be skipped per the Skip Decision Tree below; if they slip through, default to `new-dependency-or-pattern`.
- Ambiguous signals: prefer the reason matching **body content** over **prefix** (a `feat(` with strong revert-language is `wrong-turn-revert`, not `new-dependency-or-pattern`).

**Fallback**: when no row clearly applies, use `new-dependency-or-pattern` (the broadest "noticed something new" semantic).

### Step 2.2 — Docs Mining

Bash command:

```bash
find docs/ -maxdepth 3 -name '*.md' -type f 2>/dev/null
ls -1 *.md 2>/dev/null   # root-level architectural docs
```

For each Markdown file:

1. **Skip filter**:
   - `README.md` → skip (its first paragraph already lives in init-scan; body too generic for fine-grained classification)
   - `CHANGELOG.md` → skip (rendered from commit log; mining commits already covers it)
   - `LICENSE.md`, `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md` → skip (boilerplate)
   - Files <300 bytes → skip (too thin to extract meaningful observations)
2. Read the file. Identify candidate observations: section headings that read like decisions ("we chose X over Y"), guidelines ("always do X"), pitfalls ("don't do Y because..."), or process steps ("the deploy procedure is..."). Architecture diagrams in fenced code blocks are strong **model** signals.
3. For each observation, classify type / propose slug / draft summary. Call `fab_propose` with the same shape as Step 2.1 (including the **mandatory `relevance_scope: "broad"` + `relevance_paths: []`**), replacing `recent_paths` with `[<this doc path>]` and citing `src=<doc-relative-path>` in the summary.
4. Append to `.fabric/.import-state.json`:
   - `p2_processed_docs[].push({path: <doc path>, observations_proposed: <count>, pending_paths: [...]})`
5. **Hard cap shared with Step 2.1**: total new pending entries across git + docs is capped at `import_max_pending_per_run` (config-resolved, default 10) per Phase 2 run.

### Skip Decision Tree

```
A candidate signal surfaces (commit body or doc section).
  ├─ Is it cosmetic only? ("fix typo", whitespace, formatting)
  │    └─ YES → skip
  ├─ Is the body just metadata? (Co-Authored-By, Signed-off-by, no prose)
  │    └─ YES → skip
  ├─ Is the same observation already covered by an init-scan baseline title (Phase 1 list)?
  │    └─ YES → skip (don't re-propose what init already captured)
  ├─ Does the observation fit one of {decisions, pitfalls, guidelines, models, processes}?
  │    └─ NO  → skip (not classifiable = not yet ripe)
  ├─ Is the slug derivable as 2-5 kebab-case words?
  │    └─ NO  → skip (signal too vague for stable identifier)
  └─ Else → propose via fab_propose
```

After Step 2.2 completes (or hits the cap), update `.fabric/.import-state.json`: `phase = "P2-done"`, `last_checkpoint_at = <ISO8601 now>`.

### Dry-Run Mode

When the user invocation carries the verbatim token `--dry-run`, Phase 2 runs WITHOUT calling `fab_propose`. Instead it prints a table. v2.0.0-rc.37 NEW-10 dropped the legacy substring fallback on bare `dry-run` / `预览` because those caused false positives on incidental mentions ("preview the table" / "do a dry run later"). UX i18n Policy class 4 — header + column titles bilingualized; row content (slug / commit sha / doc path) NOT translated. Protected tokens `broad`, `relevance_scope`, `relevance_paths` appear verbatim:

#### zh-CN variant (`fabric_language === "zh-CN"`)

```md
# Import 预览 — 将提议 N 条 pending 条目（全部 relevance_scope=broad, relevance_paths=[]）

| # | 来源                  | 类型      | Slug                          | 作用域 | 摘要 (zh-CN)                                                |
|---|-----------------------|-----------|-------------------------------|--------|-------------------------------------------------------------|
| 1 | git c0a351d           | decisions | layer-flip-id-mutation        | broad+[] | layer 切换是唯一合法的 stable_id 变更途径，绑定原子事务。 |
| 2 | docs/architecture.md  | decisions | monolith-over-microservices   | broad+[] | 决定保留单体架构，三人团队不值微服务运维成本。            |
| 3 | git 50367b5           | pitfalls  | thundering-herd-no-backoff    | broad+[] | 重试无指数回退导致雪崩；必须 jittered exponential backoff。|
```

#### en variant (`fabric_language === "en"`)

```md
# Import Dry Run — would propose N pending entries (all relevance_scope=broad, relevance_paths=[])

| # | Source                | Type      | Slug                          | Scope    | Summary                                                       |
|---|-----------------------|-----------|-------------------------------|----------|---------------------------------------------------------------|
| 1 | git c0a351d           | decisions | layer-flip-id-mutation        | broad+[] | Layer change is the only legal stable_id mutation path; atomic txn. |
| 2 | docs/architecture.md  | decisions | monolith-over-microservices   | broad+[] | Keep the monolith — 3-engineer team can't justify microservice ops. |
| 3 | git 50367b5           | pitfalls  | thundering-herd-no-backoff    | broad+[] | Retries without exponential backoff caused a thundering herd outage. |
```

Every dry-run row MUST show `broad+[]` in the Scope column (constant for archive source mode). A row showing anything else is a skill bug — refuse to proceed and surface the violation. Dry-run output is informational only. The state file is NOT written to in dry-run mode (so a real run later starts clean). Phase 3 is also skipped in dry-run.

### Idempotency Note — T5 array form

The server derives `idempotency_key = sha256({source_session, type, slug})` for every `fab_propose` call. Re-invoking with the same `(source_session, type, slug)` triple is SAFE: the server appends new evidence to the existing pending file rather than overwriting or producing duplicates — this is why `archive source mode` resume after Ctrl-C / crash never produces duplicate pending entries for already-processed commits.

**T5 array-form note (rc.7+)**: when `source_sessions` is passed as an array (rc.7 T5 contract), only `source_sessions[0]` participates in the server-side idempotency hash. Server formula at `packages/server/src/services/extract-knowledge.ts:78` is `sha256(JSON.stringify({source_session: sourceSessions[0], type, slug}))`. Implications for archive source mode:

- Every Phase 2 call uses `source_sessions: ["fabric-archive-source-<ISO8601-date>"]` (single-element array, stable per import run). First-element-only rule means re-runs on the same date produce the same idempotency key per `(type, slug)` → resume-safe by construction.
- If a future enhancement adds a trailing element (e.g. `["fabric-archive-source-<date>", "<commit-sha>"]`), only the first element participates in the hash — the commit-sha tail would NOT change the idempotency key for the same `(type, slug)`. Plan accordingly.
- The formula is intentionally stable across the rc.5 → rc.7 migration; adding or removing tail entries does NOT change the idempotency key, preserving rc.5 single-session compat.

---

## B. Dedupe vs canonical (Phase 3)

For each pending entry created in Phase 2 (read from `p2_processed_commits[].pending_path` and `p2_processed_docs[].pending_paths`), check if it duplicates / contradicts / is subsumed by an existing canonical entry. **Semantic comparison is the LLM's job — `fab_pending` does not compare meaning.**

### Step 3.1 — Search Canonical of Same Type

For each just-proposed pending entry (read its frontmatter via the `Read` tool to get type + slug + title):

```ts
mcp__fabric__fab_pending({
  action: "search",
  query: "<title or summary keywords from the pending entry>",
  filters: { type: "<same type as pending>" }
})
```

The server returns ranked `items[]` of CANONICAL entries (not pending) of the same type. Cap the comparison set at the top 5 results.

### Step 3.2 — Semantic Compare

For each `(pending, canonical)` pair the LLM judges:

- **Duplicate** — same essential claim. LLM 主观判断：标题与摘要表达同一核心结论，新 pending 未提供新证据。具体阈值不可量化。Action: **reject** the new pending.
- **Subsumption** (pending narrower) — canonical fully covers the pending plus more. Action: **reject** the new pending (canonical already serves).
- **Subsumption-with-novelty** (pending adds evidence) — canonical covers the claim but the new pending brings new evidence (commit sha, file paths). Action: **modify** the canonical to merge in the new evidence; **reject** the new pending citing the modified canonical.
- **Contradiction** — opposing claims about the same scope. Action: leave pending; flag for user via roll-up. The user must decide via `fabric-review` later — `archive source mode` does NOT auto-resolve contradictions.
- **Genuinely new** — no canonical match. Action: leave pending in place (will surface in next `fabric-review` run for normal approval flow).

### Step 3.3 — Issue Dedup MCP Calls

For each `reject`-classified pending:

```ts
mcp__fabric__fab_review({
  action: "reject",
  pending_paths: ["<the new pending path>"],
  reason: "duplicate of <stable_id of canonical>"   // OR "subsumed by <stable_id>"
})
```

For each `subsumption-with-novelty` case (modify canonical, then reject pending):

```ts
// Step A: merge new evidence into canonical
mcp__fabric__fab_review({
  action: "modify",
  pending_path: "<canonical's pending_path-style relative path>",
  changes: { summary: "<merged summary; original + new evidence cite>" }
})

// Step B: reject the now-superseded pending
mcp__fabric__fab_review({
  action: "reject",
  pending_paths: ["<the new pending path>"],
  reason: "merged into <stable_id of modified canonical>"
})
```

Append to `.fabric/.import-state.json` after EACH successful MCP call:

- `p3_dedup_completed[].push({pending_path: <new pending>, action: "reject" | "modify-then-reject" | "kept", canonical_ref: "<stable_id>" | null})`
- `last_checkpoint_at = <ISO8601 now>`

### Step 3.4 — Phase 3 Completion

After all Phase 2 outputs are dedup-reviewed:

- Update `.fabric/.import-state.json`: `phase = "complete"`, `last_checkpoint_at = <ISO8601 now>`, `final_summary = {proposed: N, kept: K, rejected_dup: R, merged: M, contradictions_flagged: C}`.
- Render the final roll-up to the user (see section C of this file).

> Setting `phase = "complete"` in `.fabric/.import-state.json` is enough to silence the SessionStart underseed self-check banner (`shouldRecommendImport()` returns false for any non-`absent` state). 无需额外清理 sentinel 文件 — 该机制已在 rc.8 下线。

The user MAY manually delete `.fabric/.import-state.json` to reset, or the skill MAY offer a one-line "reset state and re-run from scratch?" prompt the next time it is invoked with `phase="complete"` already present.

---

## C. Output contract — the roll-up

UX i18n Policy class 1 — render either the en variant or the zh-CN variant per `fabric_language`; the protected tokens (`relevance_scope`, `relevance_paths`, `broad`, `pending_path`, `layer`, `team`, `personal`, `fab_review`, `.fabric/.import-state.json`, etc.) appear verbatim in BOTH variants.

### en variant (`fabric_language === "en"`)

```md
# Import Summary — phase=<P1-done | P2-done | complete>

## Phase 2 — Mining
- Commits scanned: <N>     (skipped: <S> — cosmetic/metadata/baseline-overlap)
- Docs scanned:    <D>     (skipped: <DS> — README/CHANGELOG/boilerplate)
- Pending proposed: <P>     (cap_reached: <true|false>)
- Scope: all <P> proposed entries use relevance_scope=broad, relevance_paths=[] (archive source mode contract).

## Phase 3 — Dedup
- Kept (genuinely new):       <K>
- Rejected (duplicate):       <RD>
- Modified-then-rejected:     <MR>     (canonical entries enriched: <list of stable_ids>)
- Contradictions flagged:     <C>     (require manual fabric-review)

## State
- .fabric/.import-state.json phase: <phase>
- last_checkpoint_at: <ISO8601>
- Re-invoke to continue if phase != complete.

## Next Steps
- Run `fabric-review` to approve the <K> kept pending entries.
- Resolve <C> contradictions manually if any.
- If any kept entry is actually narrow-scoped, narrow it via `fab_review action="modify"` with `changes.relevance_scope="narrow"` + `changes.relevance_paths=[...]` (this skill cannot narrow — see Mandatory Scope Rule in Phase 2).
```

### zh-CN variant (`fabric_language === "zh-CN"`)

```md
# Import 汇总 — phase=<P1-done | P2-done | complete>

## Phase 2 — 挖掘
- 扫描 commit 数: <N>      (跳过: <S> — cosmetic/metadata/与 baseline 重叠)
- 扫描文档数:    <D>      (跳过: <DS> — README/CHANGELOG/样板文件)
- 提议 pending:  <P>      (cap_reached: <true|false>)
- 作用域: 全部 <P> 条提议使用 relevance_scope=broad, relevance_paths=[] (archive source mode 契约)。

## Phase 3 — 去重
- 保留 (新知识):              <K>
- 已驳回 (重复):              <RD>
- 修改后驳回:                 <MR>     (被合入 evidence 的 canonical 条目: <stable_ids 列表>)
- 已标记冲突:                 <C>     (需手动通过 fabric-review 解决)

## 状态
- .fabric/.import-state.json phase: <phase>
- last_checkpoint_at: <ISO8601>
- 如 phase != complete, 请重新调用 archive source mode 续作。

## 下一步
- 运行 `fabric-review` 审批 <K> 条新 pending。
- 手动解决 <C> 条 contradictions 标记 (如有)。
- 若某条 kept 条目实际是 narrow-scoped, 通过 `fab_review action="modify"` 配 `changes.relevance_scope="narrow"` + `changes.relevance_paths=[...]` 收窄 (本 skill 无法收窄 — 见 Phase 2 Mandatory Scope Rule)。
```

---

## D. Checkpoint — .fabric/.import-state.json

The state file lives at `.fabric/.import-state.json` and is the single source of resumability for archive source mode. It is written via the explicit 2-step atomic pattern documented below so a crash between phases / between sub-steps never corrupts it.

### Atomic State Write (2-step pattern)

**Every** update to `.fabric/.import-state.json` MUST use the following two-step pattern, executed by the skill itself (not delegated to an external helper):

- **Step A**: `Write` tool → `.fabric/.import-state.json.tmp` (full JSON content; never partial / never appended).
- **Step B**: `Bash` → `mv .fabric/.import-state.json.tmp .fabric/.import-state.json`.

This 2-step pattern is mandatory for every state file update. `mv` is atomic on POSIX (`rename(2)` on the same filesystem guarantees the target either points to the old or new inode, never to a half-written file). `Write` alone is NOT atomic — the open + truncate + write sequence opens a window in which a crash leaves a zero-length or partially-written file on disk, which Phase 0.1 then has to discard. The `.tmp` + `mv` pattern eliminates that window.

Crash safety expectations:

- Crash between Step A and Step B → leaves `.fabric/.import-state.json.tmp`. Phase 0 residue scan (section E of this file) triages it on next invocation.
- Crash during Step B (between the `rename` syscall start and return) → POSIX `rename` is atomic; either the prior `.import-state.json` is intact, or the new one is in place. No torn state.
- Crash before Step A → no state mutation occurred; prior state file is unchanged.

The legacy phrasing `atomicWriteJson` / `write-temp-then-rename` used in earlier drafts of this skill refers to this exact 2-step pattern; the explicit Step A / Step B description above is the canonical form.

### events.jsonl Constraint Note

Event lines appended to `.fabric/events.jsonl` are subject to POSIX single-write atomicity: only writes ≤ 4KB (`PIPE_BUF`) are guaranteed atomic via `Bash: echo "..." >> file`. Lines exceeding 4KB risk interleaved corruption under concurrent skill + server writes to the same ledger.

Skills MUST ensure:

- Each event JSON line is a **single line** (no embedded newlines; escape `\n` in any string value).
- `session_context` and other free-form text fields **self-truncate** to keep the entire serialized line under 4KB. Suggested per-field caps: `session_context` first 500 chars; `source_sessions` cap at 5 entries; `recent_paths` cap at 20 entries; `user_messages_summary` first 500 chars.
- If approaching the 4KB ceiling after the per-field caps, drop optional fields (e.g. tags / extra metadata) **before** truncating semantic content.
- This constraint applies to any event the skill itself appends; MCP-server-side appends (via `appendEventLedgerEvent`) are already line-length-bounded server-side.

### Schema (all fields)

```json
{
  "phase": "P1-done | P2-done | complete",
  "started_at": "<ISO8601 first invocation>",
  "last_checkpoint_at": "<ISO8601 most recent successful sub-step>",
  "p1_baseline_titles": ["<title1>", "<title2>"],
  "p2_processed_commits": [
    { "sha": "<full sha>", "skipped": true,
      "skip_reason": "cosmetic | metadata-only | already-in-baseline | unclassifiable | overlong-slug" },
    { "sha": "<full sha>", "skipped": false,
      "pending_path": "knowledge/pending/<type>/<slug>.md",
      "type": "<one of 5>", "slug": "<kebab-case-slug>" }
  ],
  "p2_processed_docs": [
    { "path": "docs/<file>.md", "observations_proposed": 2,
      "pending_paths": ["<path1>", "<path2>"] }
  ],
  "p2_cap_reached": false,
  "p3_dedup_completed": [
    { "pending_path": "<new pending path>",
      "action": "reject | modify-then-reject | kept",
      "canonical_ref": "<stable_id or null>" }
  ],
  "errors": [
    { "step": "P2.git", "ref": "<commit sha or doc path>", "error": "<message>" }
  ],
  "final_summary": {
    "proposed": 0, "kept": 0, "rejected_dup": 0, "merged": 0, "contradictions_flagged": 0
  }
}
```

### Resume Logic (Idempotent Re-Invocation)

On every skill invocation, BEFORE Phase 1 starts:

1. Read `.fabric/.import-state.json`. ENOENT → fresh run, initialize state with `phase: "P1-done"` after Phase 1 completes (state file is created at end of Phase 1, not at start).
2. If `phase === "complete"` AND `last_checkpoint_at < 24h ago` → SKIP this invocation (precondition warning) unless user explicitly typed `re-run import` or `reset import`.
3. If `phase === "complete"` AND `last_checkpoint_at ≥ 24h ago` → ask the user (free-text prompt, NOT AskUserQuestion since this is rare). UX i18n Policy class 3 — confirmation prompts:

   - zh-CN: `上次 import 已完成 (<N> 天前)。重新运行将基于当前 canonical 重做 P2/P3。继续？(y/n)`
   - en: `Last import completed (<N> days ago). Re-running will redo P2/P3 against the current canonical set. Continue? (y/n)`

   If `n`, exit.
4. If `phase === "P1-done"` → skip Phase 1; resume from Phase 2 Step 2.1; iterate git log skipping any sha already in `p2_processed_commits[]`.
5. If `phase === "P2-done"` → skip Phase 1 + Phase 2; resume from Phase 3 Step 3.1; iterate Phase 2 outputs skipping any pending_path already in `p3_dedup_completed[]`.
6. After every successful sub-step (one commit processed, one doc processed, one dedup pair resolved), write the updated state file via the 2-step `.tmp` + `mv` pattern. Failures append to `errors[]` and proceed (or halt with prompt if cumulative errors `>5`).

The contract: re-invoking archive source mode after ANY interruption (Ctrl-C, crash, network blip on MCP) MUST NOT propose duplicates of already-proposed entries and MUST NOT redo already-completed dedup decisions.

---

## E. Crash recovery — .tmp residue + torn state

### Phase 0 — Init & .tmp Residue Scan

Before reading `.fabric/.import-state.json`, scan for residue left by a
prior crashed run. Skill state writes use a 2-step atomic pattern (Write
`.tmp` then `Bash mv`); a crash between Step A and Step B leaves a
`.fabric/.import-state.json.tmp` sidecar that the next invocation MUST
triage.

1. Does `.fabric/.import-state.json.tmp` exist? (`Bash: ls .fabric/.import-state.json.tmp 2>/dev/null`)
   - **Does not exist** → proceed normally to Phase 0.1 (no residue work).
   - **Exists** → triage:
     1. `Read` the `.tmp` file; try `JSON.parse` on the content.
     2. Compare `mtime` of `.tmp` vs `.fabric/.import-state.json` via `Bash: stat`.
        - **Parse OK + .tmp mtime newer than main file** → rescue:
          `Bash: mv .fabric/.import-state.json.tmp .fabric/.import-state.json`
          (commits the last incomplete write atomically).
        - **Parse OK + .tmp mtime older than main file** → stale residue
          from an earlier run that subsequently completed; delete it:
          `Bash: rm .fabric/.import-state.json.tmp`.
        - **Parse fails** (syntax error / unterminated structure / truncated
          mid-write) → half-written, unrecoverable; delete it:
          `Bash: rm .fabric/.import-state.json.tmp`.
     3. After triage, proceed to Phase 0.1.

The 5-minute mtime heuristic (treat any `.tmp` older than 5 minutes as
stale regardless of parse result) is an acceptable conservative simplification:
no legitimate atomic write window stays open that long; anything older
than 5 minutes is definitely crash residue. Implementations MAY use either
the mtime-comparison rule above OR the 5-minute staleness rule.

#### Phase 0.1 — State Corruption Recovery

After residue triage, `Read` `.fabric/.import-state.json`. Detect
corruption if ANY of the following hold:

- `JSON.parse` throws (syntax error / unterminated structure / truncated)
- Missing required field: `phase` OR `started_at` OR `last_checkpoint_at`
- `phase` value not in the enum `{P1-done, P2-done, complete}`

On corruption (any condition above):

1. `Bash: mv .fabric/.import-state.json .fabric/.import-state.json.corrupt-<ISO8601>`
   (preserve the corrupt file for postmortem; do NOT silently overwrite).
2. Phase 1 restarts from scratch (Phase 1 produces no MCP calls, so re-run
   is safe — re-querying mounted store canonical titles via `fab_pending search`
   idempotent; the `p1_baseline_titles` array is regenerated).
3. DO NOT attempt automatic partial recovery; corrupt state is a signal
   that something serious happened (disk-full, kill -9 mid-write, fs
   error). Discard-and-restart is the only safe path.

ENOENT (state file absent) is NOT corruption — it is the normal
first-run state. Proceed to Phase 0.5.

---
