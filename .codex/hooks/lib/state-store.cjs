/**
 * v2.0.0-rc.37 NEW-19: shared `.fabric/.cache/` sidecar I/O for hook scripts.
 *
 * Hooks persist tiny per-session state (turn counters, last-emit timestamps,
 * shown-hint sets) under `.fabric/.cache/`. Each hook had its own copy of the
 * read-JSON-or-null / write-JSON-best-effort / read-text / write-text helpers
 * (cite-policy-evict's readEvictState/writeEvictState, broad's
 * readBroadLastEmit/writeBroadLastEmit, fabric-hint's shown-cache + edit-counter
 * + maintenance-last-emit). This module is the single canonical implementation.
 *
 * Provides (all keyed on a bare `fileName` resolved under .fabric/.cache/):
 *   - cachePath(projectRoot, fileName) → absolute path
 *   - readJsonState(root, fileName, validate?) → parsed | null
 *       null on missing / parse error / validate() === false. Never throws.
 *   - writeJsonState(root, fileName, value) → boolean
 *       mkdir -p + write; false on failure. Never throws.
 *   - readTextState(root, fileName) → trimmed string | null
 *   - writeTextState(root, fileName, text) → boolean
 *
 * Never-throw contract: write failures return false (counter loss is
 * acceptable — the hook never blocks user flow on sidecar I/O, KT-DEC-0007).
 */

// Namespace import (not destructured) so the atomic write goes through a single
// mutable fs reference — also what the atomicity tests spy on (ISS-016).
const fs = require("node:fs");
const fsp = require("node:fs/promises");
const { dirname, join } = require("node:path");

const CACHE_DIR_REL = join(".fabric", ".cache");

function cachePath(projectRoot, fileName) {
  return join(projectRoot, CACHE_DIR_REL, fileName);
}

// ISS-016: write to a unique temp file then rename over the target. rename is
// atomic on POSIX, so a reader sees either the old or the new file in full —
// never a truncated/garbled write from a crash or concurrent writer. The temp
// suffix (pid + clock) keeps concurrent windows from colliding on the temp.
function atomicWrite(path, data) {
  fs.mkdirSync(dirname(path), { recursive: true });
  const tmp = `${path}.tmp-${process.pid}-${Date.now()}`;
  try {
    fs.writeFileSync(tmp, data);
    fs.renameSync(tmp, path);
  } catch (err) {
    try {
      if (fs.existsSync(tmp)) fs.unlinkSync(tmp);
    } catch {
      // best-effort temp cleanup
    }
    throw err;
  }
}

async function atomicWriteAsync(path, data) {
  await fsp.mkdir(dirname(path), { recursive: true });
  const tmp = `${path}.tmp-${process.pid}-${Date.now()}`;
  try {
    await fsp.writeFile(tmp, data);
    await fsp.rename(tmp, path);
  } catch (err) {
    try {
      await fsp.rm(tmp, { force: true });
    } catch {
      // best-effort temp cleanup
    }
    throw err;
  }
}

function readJsonState(projectRoot, fileName, validate) {
  const path = cachePath(projectRoot, fileName);
  if (!fs.existsSync(path)) return null;
  try {
    const parsed = JSON.parse(fs.readFileSync(path, "utf8"));
    if (typeof validate === "function" && !validate(parsed)) return null;
    return parsed;
  } catch {
    return null;
  }
}

async function readJsonStateAsync(projectRoot, fileName, validate) {
  const path = cachePath(projectRoot, fileName);
  try {
    const parsed = JSON.parse(await fsp.readFile(path, "utf8"));
    if (typeof validate === "function" && !validate(parsed)) return null;
    return parsed;
  } catch {
    return null;
  }
}

function writeJsonState(projectRoot, fileName, value) {
  try {
    atomicWrite(cachePath(projectRoot, fileName), JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}

async function writeJsonStateAsync(projectRoot, fileName, value) {
  try {
    await atomicWriteAsync(cachePath(projectRoot, fileName), JSON.stringify(value));
    return true;
  } catch {
    return false;
  }
}

function readTextState(projectRoot, fileName) {
  const path = cachePath(projectRoot, fileName);
  if (!fs.existsSync(path)) return null;
  try {
    return fs.readFileSync(path, "utf8").trim();
  } catch {
    return null;
  }
}

async function readTextStateAsync(projectRoot, fileName) {
  const path = cachePath(projectRoot, fileName);
  try {
    return (await fsp.readFile(path, "utf8")).trim();
  } catch {
    return null;
  }
}

function writeTextState(projectRoot, fileName, text) {
  try {
    atomicWrite(cachePath(projectRoot, fileName), String(text));
    return true;
  } catch {
    return false;
  }
}

async function writeTextStateAsync(projectRoot, fileName, text) {
  try {
    await atomicWriteAsync(cachePath(projectRoot, fileName), String(text));
    return true;
  } catch {
    return false;
  }
}

// Active-session sidecar — SessionStart / edit hooks stamp the client
// session_id so MCP fab_recall can fall back when the agent omits the arg.
// File: .fabric/.cache/active-session-<session_id>.json  Shape: { session_id, ts }.
//
// ONE FILE PER SESSION, not one shared slot. The user runs several client
// windows against the same repo concurrently, and a single slot is
// last-writer-wins: window B's stamp silently becomes window A's answer, so A's
// knowledge_context_planned events get attributed to B's session and cite
// coverage joins recalls to the wrong window's edits. Per-session files make the
// ambiguity VISIBLE to the reader (see readActiveSessionId, which refuses to
// guess when several sessions are live) instead of resolving it wrongly.
const ACTIVE_SESSION_FILE_PREFIX = "active-session-";
const ACTIVE_SESSION_FILE_SUFFIX = ".json";

// Untrusted session ids must not introduce path separators or traversal
// segments into .fabric/.cache/ (mirrors sessionHintsCachePath in
// knowledge-hint-narrow.cjs — same threat, same normalization).
function sanitizeSessionIdForFileName(sessionId) {
  return (
    String(sessionId || "anonymous")
      .replace(/[^A-Za-z0-9_.-]/g, "-")
      .replace(/^[.-]+/, "")
      .slice(0, 128) || "anonymous"
  );
}

function activeSessionFileName(sessionId) {
  return `${ACTIVE_SESSION_FILE_PREFIX}${sanitizeSessionIdForFileName(sessionId)}${ACTIVE_SESSION_FILE_SUFFIX}`;
}

function writeActiveSession(projectRoot, sessionId, nowMs) {
  if (typeof sessionId !== "string" || sessionId.length === 0) return false;
  const ts =
    typeof nowMs === "number" && Number.isFinite(nowMs) ? nowMs : Date.now();
  return writeJsonState(projectRoot, activeSessionFileName(sessionId), {
    session_id: sessionId,
    ts,
  });
}

module.exports = {
  cachePath,
  readJsonState,
  readJsonStateAsync,
  writeJsonState,
  writeJsonStateAsync,
  readTextState,
  readTextStateAsync,
  writeTextState,
  writeTextStateAsync,
  writeActiveSession,
  activeSessionFileName,
  ACTIVE_SESSION_FILE_PREFIX,
  ACTIVE_SESSION_FILE_SUFFIX,
  atomicWrite,
  atomicWriteAsync,
  CACHE_DIR_REL,
};
