#!/usr/bin/env node
/**
 * lifecycle-refactor W2-T2 — SessionEnd marker hook (previously dormant).
 *
 * SessionEnd fires once when a client session boots down. This hook is a
 * PURE MARKER: it appends a single `session_ended` event (session_id + ts,
 * via the shared envelope) to `.fabric/events.jsonl` and does NOTHING else.
 *
 * Design (lifecycle-concept-final.md §1 FROZEN invariants + §5 row2):
 *   - ZERO compute: the hook never reads/aggregates the ledger. ALL
 *     surfaced→cited→edited funnel reconciliation is doctor-side (offline).
 *   - hook = nudge/marker, never a gate (KT-DEC-0007): every error path ends
 *     in a silent exit 0; we never throw upward.
 *   - Front-stage O(1): one advisory-locked append, no traversal.
 *   - Per-event session_id: when the client omits session_id we DEGRADE by
 *     skipping the append entirely (a marker with no session is useless for
 *     the per-session funnel doctor reconstructs).
 *   - Hooks never require() the server package — only the co-located lib/*.cjs.
 *
 * The emitted line matches `sessionEndedEventSchema`
 * (packages/shared/src/schemas/event-ledger.ts):
 *   { kind:"fabric-event", id, ts, schema_version:1, session_id?, event_type:"session_ended" }
 *
 * Stdout/stderr are intentionally empty — SessionEnd is observation-only.
 */

const { randomUUID } = require("node:crypto");
const { existsSync } = require("node:fs");
const { join } = require("node:path");

// W1-01 (ISS-011) parity: route the shared-ledger append through the
// advisory-lock primitive so concurrent SessionEnd fires (multi-window) never
// interleave a partial line. Drop-on-contention, best-effort — same primitive
// the narrow/broad hooks use.
const { appendLockedLine } = require("./lib/injection-log.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

const FABRIC_DIR_REL = ".fabric";
const EVENTS_LEDGER_FILE = "events.jsonl";

/**
 * Read stdin (or a test-supplied raw string) as JSON. Returns null on any
 * parse failure — the hook stays silent rather than crashing session teardown.
 */
function readPayload(rawStdin) {
  if (typeof rawStdin !== "string" || rawStdin.length === 0) return null;
  try {
    const parsed = JSON.parse(rawStdin);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

/**
 * Extract the REAL payload session_id (never a synthetic fallback). The
 * funnel reconciliation in doctor keys on the client's own session id, so a
 * fabricated id would silently corrupt the per-session join. Returns null when
 * absent — the caller then skips the append (degraded, per §1).
 */
function extractSessionId(payload) {
  if (
    payload &&
    typeof payload === "object" &&
    typeof payload.session_id === "string" &&
    payload.session_id.length > 0
  ) {
    return payload.session_id;
  }
  return null;
}

/**
 * Append one `session_ended` marker to `.fabric/events.jsonl`. Best-effort:
 *   - Skips silently when `.fabric/` does not exist (project not init'd).
 *   - Skips silently when sessionId is null (degraded — no session to mark).
 *   - ANY error (append, JSON throw) is swallowed — never blocks teardown.
 */
function appendSessionEnded(projectRoot, now, sessionId) {
  try {
    if (typeof sessionId !== "string" || sessionId.length === 0) return;
    const fabricDir = join(projectRoot, FABRIC_DIR_REL);
    if (!existsSync(fabricDir)) return;
    const tsMs = now instanceof Date ? now.getTime() : Number(now);
    const event = {
      kind: "fabric-event",
      id: `event:${randomUUID()}`,
      ts: tsMs,
      schema_version: 1,
      session_id: sessionId,
      event_type: "session_ended",
    };
    appendLockedLine(join(fabricDir, EVENTS_LEDGER_FILE), JSON.stringify(event) + "\n");
  } catch {
    // Silent — marker failure must never block session teardown.
  }
}

// -----------------------------------------------------------------------------
// Main — invoked as a CLI (require.main === module) and in-process by tests.
// Wraps the entire flow in try/catch: ANY error → silent exit 0.
// -----------------------------------------------------------------------------

function main(env) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const now = (env && env.now) || new Date();
    const payload =
      env && env.payload !== undefined ? env.payload : readPayload(env && env.stdin);
    const sessionId = extractSessionId(payload);
    appendSessionEnded(cwd, now, sessionId);
  } catch {
    // Silent — never block session teardown on hook failure.
  }
}

module.exports = {
  main,
  readPayload,
  extractSessionId,
  appendSessionEnded,
  CONSTANTS: {
    FABRIC_DIR_REL,
    EVENTS_LEDGER_FILE,
  },
};

if (require.main === module) {
  // Read stdin synchronously (small hook payloads, no concurrency concerns).
  let stdinRaw = "";
  try {
    stdinRaw = require("node:fs").readFileSync(0, "utf8");
  } catch {
    // No stdin — proceed with empty payload (degrades to a no-op append).
  }
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot, now: new Date(), stdin: stdinRaw });
  process.exit(0);
}
