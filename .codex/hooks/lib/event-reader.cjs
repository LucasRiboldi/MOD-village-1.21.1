// ISS-20260713-008: bounded events.jsonl reads for hooks.
// Prefer a fixed tail window so SessionStart/Stop never load multi-MB ledgers.

const { existsSync, openSync, readSync, closeSync, fstatSync } = require("node:fs");
const { join } = require("node:path");

const DEFAULT_TAIL_BYTES = 256 * 1024; // 256KB

/**
 * Read the last `maxBytes` of a file as utf8. Returns "" if missing.
 * @param {string} filePath
 * @param {number} [maxBytes]
 */
function readFileTail(filePath, maxBytes) {
  const budget = typeof maxBytes === "number" && maxBytes > 0 ? maxBytes : DEFAULT_TAIL_BYTES;
  if (!existsSync(filePath)) return "";
  let fd;
  try {
    fd = openSync(filePath, "r");
    const st = fstatSync(fd);
    const size = st.size;
    if (size === 0) return "";
    const start = size > budget ? size - budget : 0;
    const len = size - start;
    const buf = Buffer.alloc(len);
    readSync(fd, buf, 0, len, start);
    let text = buf.toString("utf8");
    // If we started mid-line, drop the partial first line.
    if (start > 0) {
      const nl = text.indexOf("\n");
      if (nl !== -1) text = text.slice(nl + 1);
    }
    return text;
  } catch {
    return "";
  } finally {
    if (fd !== undefined) {
      try {
        closeSync(fd);
      } catch {
        /* ignore */
      }
    }
  }
}

/**
 * Parse JSONL events from a string (corrupt lines skipped).
 * @param {string} raw
 * @returns {object[]}
 */
function parseJsonlEvents(raw) {
  if (!raw || typeof raw !== "string") return [];
  // Match event-ledger / pre-extract fabric-hint readLedger: drop a trailing
  // partial line when the raw buffer lacks a terminating newline.
  const lines = raw.split(/\r?\n/);
  if (!raw.endsWith("\n") && lines.length > 0) {
    lines.pop();
  }
  const events = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.length === 0) continue;
    try {
      const parsed = JSON.parse(trimmed);
      if (parsed && typeof parsed === "object") events.push(parsed);
    } catch {
      /* corrupt */
    }
  }
  return events;
}

/**
 * Read recent events from <projectRoot>/.fabric/events.jsonl via tail window.
 * @param {string} projectRoot
 * @param {{ maxBytes?: number, eventType?: string }} [opts]
 */
function readRecentEvents(projectRoot, opts) {
  const file = join(projectRoot, ".fabric", "events.jsonl");
  const raw = readFileTail(file, opts && opts.maxBytes);
  let events = parseJsonlEvents(raw);
  if (opts && typeof opts.eventType === "string") {
    events = events.filter((e) => e && e.event_type === opts.eventType);
  }
  return events;
}

/**
 * Age in days of the newest doctor_run in the tail window, or null.
 */
function readLastDoctorRunAgeDays(projectRoot, nowMs, maxBytes) {
  const events = readRecentEvents(projectRoot, {
    maxBytes: maxBytes || DEFAULT_TAIL_BYTES,
    eventType: "doctor_run",
  });
  let latest = null;
  for (const e of events) {
    if (typeof e.ts === "number" && (latest === null || e.ts > latest)) latest = e.ts;
  }
  if (latest === null) return null;
  return (nowMs - latest) / (24 * 60 * 60 * 1000);
}

module.exports = {
  DEFAULT_TAIL_BYTES,
  readFileTail,
  parseJsonlEvents,
  readRecentEvents,
  readLastDoctorRunAgeDays,
};
