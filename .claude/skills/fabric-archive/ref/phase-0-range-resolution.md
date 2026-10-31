# Phase 0 — Range Resolution (ref)

> **Loaded on demand.** This file holds ONLY what has no code counterpart: the bilingual parsing tables, the session_id resolution algorithm, and the AskUserQuestion fallback. Read when entry_point ∈ {E2_explicit_user_invoke, E4_user_range_rollback} AND the user prompt likely carries a range hint that needs parsing.
>
> **The carry-forward contract is NOT here.** What Phase 0 hands to `fab_archive_scan` is stated in SKILL.md's Phase 0 table and, authoritatively, in `archiveScanInputSchema.range`'s `describe`. A prose copy of it lived here until 2026-08-11 and had drifted into saying the opposite of the implementation (ISS-20260806-001) — it was deleted rather than patched.

## Step 2 — Time-window parsing

Match the user prompt against the following bilingual patterns (case-insensitive substring match, leftmost-longest wins). The matched span yields a `[ts_start, ts_end]` pair in Unix milliseconds. `now` = the skill invocation timestamp.

### zh-CN pattern table

| Pattern | ts_start | ts_end |
|---|---|---|
| `今日` / `今天` | `floor(now, day)` (本地时区 00:00) | `now` |
| `上周` / `过去一周` | `now - 7d` | `now` |
| `过去 N 天` / `近 N 天` (N ∈ 1..30) | `now - N*24h` | `now` |
| `自上次归档` / `自上次 archive` | tail-scan events.jsonl → most recent `knowledge_proposed.ts` (fallback `events[0].ts`) | `now` |

### en pattern table

| Pattern | ts_start | ts_end |
|---|---|---|
| `today` | `floor(now, day)` (local TZ 00:00) | `now` |
| `last week` / `past week` | `now - 7d` | `now` |
| `past N days` / `last N days` (N ∈ 1..30) | `now - N*24h` | `now` |
| `since last archive` / `since last archived` | tail-scan events.jsonl → most recent `knowledge_proposed.ts` (fallback `events[0].ts`) | `now` |

Notes:

- Patterns are non-exclusive — if the prompt matches multiple (e.g. "今日 cite policy"), apply time-window THEN topic-keyword as AND.
- Numeric N must parse as a positive integer ≤ 30; reject anything else as parse-miss.
- All other date phrasings (specific dates like `5月10日`, relative phrasings like `三天前下午`) are NOT handled here — emit parse-miss and let Step 5 fallback collect a structured answer.

## Step 3 — Topic-keyword extraction

After time-window matching (or alongside it when both apply), extract content keywords from the prompt:

1. Strip recognised time-window tokens (e.g. remove `今日` / `last week` from the residual prompt).
2. Tokenize residual on whitespace + CJK boundary. Combine adjacent CJK characters into one token; split en words on spaces.
3. Filter **stop-words**: skill control verbs (`archive`, `归档`, `下`, `的`), articles / particles (`the`, `a`, `an`, `了`, `吧`), pronouns (`it`, `this`, `that`, `这个`, `那个`), and 1-character en tokens.
4. Retain **2-5 word tokens** (or 1-token CJK content words ≥ 2 chars like `rc.20`, `cite`). Cap at 8 keywords; drop weaker (later-position) ones.

The retained set is `topic_keywords[]`. Empty set = no keyword filter.

## Step 4 — session_id resolution algorithm

Given `time_window = [ts_start, ts_end] | null` and `topic_keywords[] | []`:

```
Step a — Read events.jsonl tail (last 500 events) via `Bash: tail -n 500
         .fabric/events.jsonl`. ENOENT → empty list (no resolution possible
         → emit parse-miss → Step 5 fallback).

Step b — Per distinct session_id present in the tail, compute:
           ts_min      = min(ts) over events with this session_id
           ts_max      = max(ts) over events with this session_id
           digest_path = .fabric/.cache/session-digests/<session_id>.md
           digest_body = Read(digest_path) if exists, else ""

Step c — TIME-WINDOW FILTER (skip when time_window is null):
           Keep session_id IFF [ts_min, ts_max] intersects [ts_start, ts_end]
           (i.e. ts_max >= ts_start AND ts_min <= ts_end).
           Multiple time intervals are OR'd within the time-window filter
           category (none currently supported; reserved for future ranges).

Step d — TOPIC-KEYWORD FILTER (skip when topic_keywords is empty):
           Keep session_id IFF digest_body (case-insensitive) contains
           AT LEAST ONE keyword from topic_keywords[].
           Multiple keywords are OR'd within the keyword filter category.

Step e — AND across filter categories:
           A session must pass BOTH filters when BOTH are present.
           Pass either filter alone when only one is present.
           Pass-through (all sessions) when neither is present.

Step f — Result: distinct session_id[] (preserve event-order); if empty AND
         a parse hit was claimed → degrade to Step 5 fallback (user wanted a
         range that resolved to zero sessions).
```

## Step 5 — AskUserQuestion fallback (E2 / E4 only)

When Step 2/3 emit parse-miss OR Step 4 resolves to zero sessions AND the invocation type permits prompting (E2 user-active or E4 user回溯-active — NEVER E1 hook / E3 AI-self / E5 cron), surface a structured question. UX i18n Policy class 5 applies: `header` + `question` translate per `fabric_language`; `options[]` routing keys stay English.

```ts
AskUserQuestion({
  header: "Archive range",                              // zh-CN: "归档范围"
  question:
    "Which session range should this archive cover? " +
    "(today = current calendar day; last-week = past 7 days; " +
    "since-last-archive = newer than last knowledge_proposed event; " +
    "custom = type a free-form range)",
  options: ["today", "last-week", "since-last-archive", "custom"]
})
```

Routing:

| Choice | Action |
|---|---|
| `today` | Re-enter Step 2 with synthetic prompt `今日` / `today` (per `fabric_language`); resolve session_ids; proceed to Phase 0.5. |
| `last-week` | Re-enter Step 2 with synthetic prompt `上周` / `last week`; proceed to Phase 0.5. |
| `since-last-archive` | Re-enter Step 2 with synthetic prompt `自上次归档` / `since last archive`; proceed to Phase 0.5. |
| `custom` | Surface a one-line text prompt to the user ("type a range, e.g. 'rc.20', 'past 3 days', '上周 cite policy'"). Re-enter Phase 0 Step 1 with the user-typed sub-prompt. Loop max 1 time — a second parse-miss **omits `range`** (default incremental scan), never `"all"`. |

A resolution that lands on zero sessions is a parse-miss, not an empty list: NEVER hand `session_id: []` forward. Degrade to the Step 5 fallback, or — when the invocation type forbids prompting (E1 / E3 / E5) — omit `range`.
