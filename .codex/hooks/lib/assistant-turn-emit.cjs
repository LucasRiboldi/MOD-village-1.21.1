// ISS-20260713-040: cite / assistant-turn emission helpers for fabric-hint Stop hook.
const { existsSync } = require("node:fs");
const { join } = require("node:path");
const { appendLockedLine } = require("./injection-log.cjs");
const { appendEvent } = require("./event-writer.cjs");
const { summarizeTranscript } = require("./transcript-summary.cjs");

const FABRIC_DIR = ".fabric";
const METRICS_LEDGER_FILE = "metrics.jsonl";
const EVENT_TYPE_ASSISTANT_TURN_OBSERVED = "assistant_turn_observed";

let citeLineParser = null;
try {
  citeLineParser = require("./cite-line-parser.cjs");
} catch {
  citeLineParser = null;
}

let clientAdapter = null;
try {
  clientAdapter = require("./client-adapter.cjs");
} catch {
  clientAdapter = null;
}

function parseKbLine(raw) {
  if (typeof raw !== "string") {
    return { cite_ids: [], cite_tags: [], cite_commitments: [] };
  }
  const composed = `KB: ${raw}`;
  if (citeLineParser && typeof citeLineParser.parseCiteLine === "function") {
    return citeLineParser.parseCiteLine(composed);
  }
  return { cite_ids: [], cite_tags: [], cite_commitments: [] };
}

function detectClient() {
  if (clientAdapter && typeof clientAdapter.detectClient === "function") {
    // Prefer hook template dir (parent of lib/) so path heuristic still sees .claude/.codex
    return clientAdapter.detectClient(join(__dirname, ".."));
  }
  const envClient = process.env.FABRIC_HINT_CLIENT;
  if (typeof envClient === "string" && envClient.length > 0) {
    const normalised = envClient.trim().toLowerCase();
    if (normalised === "cc" || normalised === "codex") {
      return normalised;
    }
  }
  return undefined;
}

function extractAndWriteAssistantTurnsBestEffort(cwd, stdinPayload) {
  if (stdinPayload === null || typeof stdinPayload !== "object") return;
  try {
    const sessionId = stdinPayload.session_id;
    if (typeof sessionId !== "string" || sessionId.length === 0) return;
    const transcript = summarizeTranscript(stdinPayload.transcript_path);
    const turns = transcript.assistant_turns;
    if (!Array.isArray(turns) || turns.length === 0) return;

    const fabricDir = join(cwd, FABRIC_DIR);
    if (!existsSync(fabricDir)) {
      return;
    }
    const client = detectClient();
    let randomUUID;
    try {
      ({ randomUUID } = require("node:crypto"));
    } catch {
      randomUUID = null;
    }

    let emptyShellCount = 0;
    for (const turn of turns) {
      try {
        const citeIds = Array.isArray(turn.cite_ids) ? turn.cite_ids : [];
        const citeCommitments = Array.isArray(turn.cite_commitments)
          ? turn.cite_commitments
          : [];
        const isEmptyShell =
          (turn.kb_line_raw === null || turn.kb_line_raw === undefined) &&
          citeIds.length === 0 &&
          citeCommitments.length === 0;
        if (isEmptyShell) {
          emptyShellCount += 1;
          continue;
        }
        const idSuffix = typeof randomUUID === "function"
          ? randomUUID()
          : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
        const event = {
          kind: "fabric-event",
          id: `event:${idSuffix}`,
          ts: Date.now(),
          schema_version: 1,
          session_id: sessionId,
          event_type: EVENT_TYPE_ASSISTANT_TURN_OBSERVED,
          kb_line_raw: turn.kb_line_raw,
          cite_ids: citeIds,
          cite_tags: Array.isArray(turn.cite_tags) ? turn.cite_tags : [],
          cite_commitments: citeCommitments,
          turn_id: `${sessionId}-${turn.envelope_index}`,
          envelope_index: turn.envelope_index,
          timestamp: new Date().toISOString(),
        };
        if (client !== undefined) event.client = client;
        appendEvent(fabricDir, event);
      } catch {
        // Per-turn failure must not abort remaining turns.
      }
    }

    if (emptyShellCount > 0) {
      try {
        const counterKey =
          client !== undefined
            ? `${EVENT_TYPE_ASSISTANT_TURN_OBSERVED}:${client}`
            : EVENT_TYPE_ASSISTANT_TURN_OBSERVED;
        const metricsRow = {
          timestamp: new Date().toISOString(),
          window: "stop",
          counters: { [counterKey]: emptyShellCount },
        };
        const metricsPath = join(fabricDir, METRICS_LEDGER_FILE);
        appendLockedLine(metricsPath, JSON.stringify(metricsRow) + "\n");
      } catch {
        // metrics fold is observability-only
      }
    }
  } catch {
    // Outer guard — never throw.
  }
}

module.exports = {
  parseKbLine,
  detectClient,
  extractAndWriteAssistantTurnsBestEffort,
  EVENT_TYPE_ASSISTANT_TURN_OBSERVED,
};
