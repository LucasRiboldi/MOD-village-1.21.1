// ISS-20260713-040 residual: graph_edge_candidate_requested emit + session de-dup.
// Extracted from fabric-hint.cjs (lifecycle-refactor W3-A2 §7).
const { existsSync, mkdirSync, readFileSync, writeFileSync } = require("node:fs");
const { dirname, join } = require("node:path");
const { appendEvent } = require("./event-writer.cjs");
const sessionSignalState = require("./session-signal-state.cjs");
const hintConfig = require("./hint-config.cjs");

const FABRIC_DIR = hintConfig.FABRIC_DIR;
const EVENT_TYPE_PROPOSED = "knowledge_proposed";
const STABLE_ID_RE = /^K[TP]-[A-Z]{3}-\d{4}$/;
const GRAPH_EDGE_REQUESTED_SIDECAR = ".fabric/.cache/graph-edge-requested";

let stateStore = null;
try {
  stateStore = require("./state-store.cjs");
} catch {
  stateStore = null;
}

/**
 * After a successful archive, request edge extraction for a freshly canonical
 * entry. Only emits when the newest knowledge_proposed carries a real stable_id.
 * Best-effort, never throws (KT-DEC-0007).
 */
function emitGraphEdgeCandidateBestEffort(cwd, events, sessionId) {
  try {
    if (!Array.isArray(events) || events.length === 0) return;
    const fabricDir = join(cwd, FABRIC_DIR);
    if (!existsSync(fabricDir)) return;

    let stableId = null;
    let store;
    for (let i = events.length - 1; i >= 0; i -= 1) {
      const ev = events[i];
      if (!ev || ev.event_type !== EVENT_TYPE_PROPOSED) continue;
      const candidate = typeof ev.stable_id === "string" ? ev.stable_id : null;
      if (candidate && STABLE_ID_RE.test(candidate)) {
        stableId = candidate;
        if (typeof ev.store === "string" && ev.store.length > 0) store = ev.store;
      }
      break;
    }
    if (stableId === null) return;

    const sidecarPath = join(
      cwd,
      sessionSignalState.sessionScopedCacheFile(GRAPH_EDGE_REQUESTED_SIDECAR, sessionId),
    );
    try {
      if (existsSync(sidecarPath)) {
        const prev = readFileSync(sidecarPath, "utf8").trim();
        if (prev === stableId) return;
      }
    } catch {
      // unreadable sidecar → fall through
    }

    let idSuffix;
    try {
      idSuffix = require("node:crypto").randomUUID();
    } catch {
      idSuffix = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
    }
    const event = {
      kind: "fabric-event",
      id: `event:${idSuffix}`,
      ts: Date.now(),
      schema_version: 1,
      event_type: "graph_edge_candidate_requested",
      stable_id: stableId,
    };
    if (store !== undefined) event.store = store;
    if (typeof sessionId === "string" && sessionId.length > 0) event.session_id = sessionId;
    appendEvent(fabricDir, event);

    try {
      if (stateStore && typeof stateStore.atomicWrite === "function") {
        stateStore.atomicWrite(sidecarPath, stableId);
      } else {
        mkdirSync(dirname(sidecarPath), { recursive: true });
        writeFileSync(sidecarPath, stableId);
      }
    } catch {
      // de-dup marker write failed — at worst re-request next Stop
    }
  } catch {
    // best-effort §7 signal — never block Stop hook
  }
}

module.exports = {
  STABLE_ID_RE,
  GRAPH_EDGE_REQUESTED_SIDECAR,
  emitGraphEdgeCandidateBestEffort,
};
