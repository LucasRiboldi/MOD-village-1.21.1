# Phase 1 — Collect Cross-Session Digests (ref)

> **Loaded on demand.** SKILL.md hot path retains Phase 1's purpose statement + 5-step summary + graceful-degradation note. This file holds Steps 1-5 detailed implementation (events.jsonl tail-scan, anchor-walk, digest load, rc.25 TASK-05 ledger filter algorithm + constants + worked examples, cross-session context build).

> **v2.0.0-rc.37 NEW-9 — Steps 1-4.5 moved server-side.** The deterministic part of this algorithm (events.jsonl tail-scan, anchor-find, session forward-collect, and the Step 4.5 outcome-ledger filter state machine) now runs in the server and is exposed as the `fab_archive_scan` MCP tool — call it instead of hand-running `tail`/grep. The tool returns the already-filtered `session_ids[]` + `anchor_ts` + `covered_through_ts` + `already_proposed_keys[]`. Steps 1-4.5 below remain as the AUTHORITATIVE SPEC of what the server computes (and the contract tests pin it); the Skill no longer executes them by hand. Step 5 (digest load + cross-session context stitch) stays LLM-side per Boundary B.

## Step 1 — Read events.jsonl tail

Use `Bash` with `tail -n 200 .fabric/events.jsonl` (tolerate ENOENT — empty ledger is a normal first-run state).

## Step 2 — Find the anchor

Walk the tail backwards to locate the most recent `knowledge_proposed` event (`event_type === "knowledge_proposed"`). The anchor's `ts` becomes the lower bound for digest selection. If NO anchor exists, treat all digests in the cache as in-scope.

## Step 3 — Collect session_ids since anchor

Scan the tail forward from the anchor and collect every distinct `session_id` field that appears on any event newer than the anchor. Distinct ordering preserved.

## Step 4 — Load digests

For each collected `session_id`, read `.fabric/.cache/session-digests/<session_id>.md`. Missing digest files degrade silently (the digest write was best-effort, so a Stop hook crash can produce a session_id without a digest). Cap the loaded digest set at `archive_digest_max_sessions` most-recent sessions (config-resolved, default 10) to bound LLM context (~50KB worst-case at default).

## Step 4.5 — Filter via session_archive_attempted ledger (rc.25 TASK-05)

Before Step 5 builds the cross-session context, drop sessions that the outcome ledger says we should not re-scan. For each `session_id` collected in Steps 1-3, scan `.fabric/events.jsonl` for events where `event_type === "session_archive_attempted"` AND `session_id` matches, keep the most-recent one by `ts`, and apply this state machine:

- **(a) Look up the most recent `session_archive_attempted`** event for this `session_id` (none found → fall through to (e)).
- **(b) `outcome === "user_dismissed"` → drop (permanent skip).** The user explicitly rejected this session's candidates; never auto-re-scan it. Respect the dismissal forever — re-scanning would re-propose the same content the user already declined.
- **(c) `(nowMs - attempted_event.ts) < ANTI_LOOP_HOURS * 3_600_000` → drop (cooldown skip).** Anti-loop window: even if outcome is otherwise re-scannable, never re-scan a session within 12 hours of the last attempt. Aligns 心智 with the Stop-hook cooldown so a single user does not see the same session repeatedly within one work day.
- **(d) `covered_through_ts` present → check for high-value signal in `ts > covered_through_ts` events for this `session_id`.** Tail-scan `events.jsonl` for events newer than the watermark whose `session_id` matches. A session passes this gate iff at least ONE of:
  - ≥1 event with `event_type ∈ HIGH_VALUE_EVENT_TYPES` (`knowledge_context_planned`, `edit_paths_recorded`), OR
  - the latest `assistant_turn_observed` event body contains ≥1 of `NORMATIVE_KEYWORDS` (substring match, case-insensitive for English entries).

  No high-value signal → drop (no new content worth re-scanning, even though the cooldown has expired). Has signal → keep for re-scan.
- **(e) Never attempted (no `session_archive_attempted` event found for this `session_id`) → keep.** First-time scan; nothing to filter against.
- **(f) Cross-session pending dedupe** (operates on candidate observations, not on `session_id` filter): gather all `knowledge_proposed_ids` from `session_archive_attempted` events with `outcome === "proposed"` across ALL sessions in the recent window (NOT just the current candidate session). This builds a global set of idempotency keys already proposed by prior archive runs but not yet reviewed by the user (the active write store may still contain matching pending entries). When classifying new observations in Phase 3, drop any candidate whose computed `idempotency_key` matches an id already in this set — it was already proposed by an earlier archive run, the user just hasn't reviewed it yet, so re-proposing would duplicate pending entries and inflate `candidates_proposed` counts. Per Phase 4.5 dedupe consumer of `knowledge_proposed_ids`.

The resulting filtered `session_id[]` proceeds into Step 5's digest concatenation. Sessions filtered out in this step do NOT contribute to `### Cross-session digest`, are NOT included in `source_sessions` on any fab_propose call, and are NOT referenced in `session_context` bodies.

### Constants (rc.25 — verbatim)

- `ANTI_LOOP_HOURS = 12` — cooldown window in hours between consecutive re-scans of the same `session_id`. Rationale: 心智对齐 hook cooldown (`stop_hook_cooldown_hours = 12`); identical mental model avoids user confusion when a session shows up in both hook reminders and archive re-scan candidates.
- `HIGH_VALUE_EVENT_TYPES = ['knowledge_context_planned', 'edit_paths_recorded']` — event types that count as "new substantive activity worth re-scanning" past `covered_through_ts`. Chat accumulation (`assistant_turn_observed` alone) does NOT count — it would let mere conversation noise trigger re-scans.
- `NORMATIVE_KEYWORDS = ['以后','always','never','from now on','下次','记一下','永远不要']` — substring patterns scanned against the latest `assistant_turn_observed` body for the session. Mixed CN/EN to cover bilingual users. If any keyword hits, the session is flagged as having high-value chat-only signal even without code edits.

### Worked examples

- **Session X (user_dismissed)** — last `session_archive_attempted` ts = 3 days ago, outcome = `user_dismissed`. Rule (b) fires → permanent skip. Session X is dropped even if 50 new `knowledge_context_planned` events have accumulated since.
- **Session Y (proposed 6h ago)** — last `session_archive_attempted` ts = 6h ago, outcome = `proposed`. Rule (c) fires: 6h < 12h cooldown window → drop (cooldown skip). Y becomes eligible again after the 12h window closes, provided high-value signal accumulates by then.
- **Session Z (viability_failed 14h ago + 3 new plan_context)** — last `session_archive_attempted` ts = 14h ago, outcome = `viability_failed`, `covered_through_ts` = T₀. Rules (b)(c) pass. Rule (d) tail-scans for `session_id === Z AND ts > T₀`: finds 3 `knowledge_context_planned` events. HIGH_VALUE_EVENT_TYPES match → keep Z for re-scan. The previous viability failure does not block a re-scan once new substantive activity has accumulated.

## Step 5 — Build cross-session context

Concatenate the loaded digests into a single `### Cross-session digest` block to carry into Phase 2.5 + Phase 1. Use this block to:

- Detect session-spanning patterns (e.g. a discussion that started in session A and continued in session B).
- Populate the `source_sessions` array on every fab_propose call — the array form (T5) replaces the legacy `source_session` string.
- Inform the `session_context` blob written to each pending entry's body (3-5 lines summarizing goal + key turning point, per T6).

## Graceful degradation

If `.fabric/.cache/session-digests/` is missing entirely, this phase reports an empty context and Phase 2 falls back to the single-session behaviour. Tests that synthesize events.jsonl without populating the digest cache continue to work. If `session_archive_attempted` events are missing entirely (pre-rc.25 ledger or rotation has trimmed older events), treat all sessions as never-attempted (current default behavior) — Step 4.5 rule (e) applies uniformly, so the filter degrades to the legacy "scan everything since anchor" semantics without raising errors.
