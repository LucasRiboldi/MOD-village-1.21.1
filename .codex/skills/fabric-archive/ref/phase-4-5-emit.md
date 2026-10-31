# Phase 4.5 — Persist Archive Attempt (ref)

> **Loaded on demand.** SKILL.md hot path retains the MANDATORY-on-every-invocation rule, the dry-run cross-reference, and the outcome enum. This file holds the full event emission jsonc schema, outcome decision matrix, covered_through_ts watermark spec, multi-session emission rule, append-pattern reference, and the E5-cron-silent-skip worked example.

## What to emit

For EACH `session_id` in the run's scope (multi-session E4 runs emit MULTIPLE events — one per session_id; single-session E1/E2/E3/E5 runs emit ONE event), append ONE `session_archive_attempted` line to `.fabric/events.jsonl`:

```jsonc
{
  "kind": "fabric-event",
  "id": "<uuid or ts-derived>",
  "ts": <epoch ms>,
  "schema_version": 1,
  "session_id": "<the session this event pertains to>",
  "event_type": "session_archive_attempted",
  "outcome": "proposed" | "viability_failed" | "user_dismissed" | "skipped_no_signal",
  "covered_through_ts": <max event ts scanned for this session>,
  "candidates_proposed": <integer, default 0>,
  "knowledge_proposed_ids": ["<idempotency_key_1>", "..."]   // default []
}
```

## Outcome decision matrix

| Skill terminal state                                                 | outcome              | candidates_proposed | knowledge_proposed_ids                          |
|----------------------------------------------------------------------|----------------------|---------------------|-------------------------------------------------|
| Phase 4 wrote ≥ 1 pending entry                                      | `proposed`           | N (count written)   | `[idempotency_key_1, idempotency_key_2, ...]` (from each fab_propose response) |
| Phase 2.5 viability_failed AND entry_point ∈ {E2_explicit, E4_user_range} AND user saw + accepted the gate-FAIL message | `viability_failed`   | 0                   | `[]`                                            |
| Phase 3 batch review — user dismissed ALL presented candidates       | `user_dismissed`     | 0                   | `[]`                                            |
| Phase 1 filter dropped every session in scope OR Phase 2.5 silent-skip path (E1_hook / E3_ai_self_trigger / E5_cron) | `skipped_no_signal`  | 0                   | `[]`                                            |

Rationale highlights:
- `user_dismissed` is the ONLY outcome that suppresses future auto-rescan (respects user decision per Q3.4).
- `proposed` populates `knowledge_proposed_ids` so the cross-session digest in Phase 1 can dedupe future runs against already-proposed entries.
- `viability_failed` vs `skipped_no_signal` distinguishes "user was prompted but the gate stopped us" from "we never bothered the user" — both allow rescan but the doctor history report differentiates them.

## covered_through_ts watermark

```
covered_through_ts = max(events_in_scope[*].ts)
```

where `events_in_scope` is the set of events the skill actually examined for THAT session_id (Phase 2 + Phase 1 digest input). On rescan, Phase 1 compares the current `max(ts)` against this stored watermark — only sessions with new events past the watermark are eligible candidates.

## Multi-session emission rule

When the run scope spans multiple session_ids (E4 user-range with `--since` / topic-keyword matching multiple sessions), emit ONE `session_archive_attempted` event PER session_id. Each event's `covered_through_ts` is computed against that session's own event subset. The `knowledge_proposed_ids` for a multi-session `proposed` run lists ALL idempotency_keys produced by the run; ledger consumers that want per-session breakdown should join against `source_sessions` on each pending entry.

## Append pattern (Bash echo, 4KB-safe, fail-tolerant)

Reuse the Phase 2.5 `events.jsonl Constraint Note` pattern: single-line JSON ≤ 4KB, no embedded newlines. Best-effort write — if the append fails (disk full, permission denied, race), the skill MUST still exit successfully. Log the failure to stderr only; do NOT surface it to the user. Rationale: a missing `session_archive_attempted` event degrades gracefully — the next Phase 1 digest treats the session as "never archived" and re-evaluates it, which is the safe-default behavior.

```bash
# Pseudo — actual implementation uses the same pattern as the legacy
# knowledge_archive_aborted emit at the end of Phase 2.5.
echo '{"kind":"fabric-event","id":"...","ts":..., "schema_version":1, "session_id":"...", "event_type":"session_archive_attempted","outcome":"...","covered_through_ts":...,"candidates_proposed":0,"knowledge_proposed_ids":[]}' >> .fabric/events.jsonl
```

The per-field caps from Phase 2.5's constraint note carry over: `knowledge_proposed_ids` capped at 20 entries (drop tail with `...` marker in `id` field if truncated); other fields are bounded by schema.

## Worked example: E5 cron silent-skip

Setup: An OS cron job runs `fabric-archive` at 03:00 daily for the "today" range (E5 entry_point). Today's session was routine config edits — no archive signals fire.

Trace:
1. Phase 0 resolves `entry_point=E5_cron`, range = "today" → 1 session_id in scope.
2. Phase 1 digest collects events for that session_id; nothing dropped.
3. Phase 1.5 onboard is skipped (E5 is not E2).
4. Phase 2.5 viability gate runs — `archive_signals_hit=0` → `gate=FAIL (reason=no_signal)`.
5. `entry_point=E5_cron` ∈ {E1, E3, E5} → SILENT-SKIP branch. No message rendered.
6. Phase 4.5 (mandatory) appends ONE event:
   ```
   {"kind":"fabric-event","id":"...","ts":<now>,"schema_version":1,"session_id":"<today-session-id>","event_type":"session_archive_attempted","outcome":"skipped_no_signal","covered_through_ts":<max ts of today's events>,"candidates_proposed":0,"knowledge_proposed_ids":[]}
   ```
7. Skill exits silently. Cron output is empty.

Next day's cron rescan: Phase 1 sees `covered_through_ts < max(ts of session's new events)` → session is rescan-eligible → loop continues without `user_dismissed` block.
