// v2.2 HK3-telemetry (W3-T1) + ISS-20260713-006 ownership tokens.
// Advisory lock for concurrent jsonl appends. Server withFileLock and hooks
// share *.lock paths for events.jsonl — reclaim MUST be token-aware so a hook
// never steals a live server critical section.

const { appendFileSync, mkdirSync, openSync, closeSync, readFileSync, writeFileSync, statSync, rmSync, existsSync, readdirSync, renameSync } = require("node:fs");
const { join, dirname } = require("node:path");
const { randomBytes } = require("node:crypto");

const STALE_LOCK_MS = 5000;
const DEFAULT_WAIT_MS = 200;
const DEFAULT_RETRY_MS = 20;

function makeToken() {
  return `${process.pid}.${Date.now()}.${randomBytes(4).toString("hex")}`;
}

/**
 * Acquire lock with ownership token. Returns { fd, token, lockPath } or null.
 * On stale reclaim, only unlink when on-disk token is readable and age > staleMs
 * (token still written by previous holder; we do not unlink if file empty/missing mid-race).
 */
function tryAcquireLock(lockPath) {
  let fd;
  try {
    fd = openSync(lockPath, "wx");
    const token = makeToken();
    try {
      writeFileSync(lockPath, token, "utf8");
    } catch {
      try {
        closeSync(fd);
      } catch {
        /* ignore */
      }
      try {
        rmSync(lockPath, { force: true });
      } catch {
        /* ignore */
      }
      return null;
    }
    return { fd, token, lockPath };
  } catch (err) {
    if (!err || err.code !== "EEXIST") return null;
    // Contended: reclaim only if stale AND we can read a token (ownership stamp).
    try {
      const st = statSync(lockPath);
      if (Date.now() - st.mtimeMs <= STALE_LOCK_MS) return null; // fresh holder → do not steal
      let staleToken = null;
      try {
        staleToken = readFileSync(lockPath, "utf8");
      } catch {
        return null;
      }
      // ISS-20260713-006: only reclaim when token is non-empty (owned stamp).
      // Empty/corrupt → wait for server-side wait path or next retry; never blind unlink.
      if (typeof staleToken !== "string" || staleToken.length === 0) return null;
      // Compare-and-unlink best-effort: re-read after unlink attempt race is possible
      // but we refuse unconditional force-unlink without a prior token read.
      try {
        const again = readFileSync(lockPath, "utf8");
        if (again !== staleToken) return null; // holder changed
        rmSync(lockPath, { force: true });
      } catch {
        return null;
      }
      fd = openSync(lockPath, "wx");
      const token = makeToken();
      writeFileSync(lockPath, token, "utf8");
      return { fd, token, lockPath };
    } catch {
      return null;
    }
  }
}

function releaseLock(lockPath, token) {
  try {
    if (!existsSync(lockPath)) return;
    const current = readFileSync(lockPath, "utf8");
    if (current === token) {
      rmSync(lockPath, { force: true });
    }
  } catch {
    /* best-effort */
  }
}

function releaseAndWrite(path, held, line) {
  try {
    try {
      closeSync(held.fd);
    } catch {
      /* ignore */
    }
    appendFileSync(path, line);
  } finally {
    releaseLock(held.lockPath, held.token);
  }
}

function appendLockedLine(path, line) {
  const lockPath = `${path}.lock`;
  const held = tryAcquireLock(lockPath);
  if (held === null) return;
  releaseAndWrite(path, held, line);
}

function appendLockedLineWait(path, line, opts) {
  const lockPath = `${path}.lock`;
  const maxWaitMs = opts && typeof opts.maxWaitMs === "number" ? opts.maxWaitMs : DEFAULT_WAIT_MS;
  const retryDelayMs =
    opts && typeof opts.retryDelayMs === "number" ? opts.retryDelayMs : DEFAULT_RETRY_MS;
  const deadline = Date.now() + maxWaitMs;
  while (Date.now() <= deadline) {
    const held = tryAcquireLock(lockPath);
    if (held !== null) {
      releaseAndWrite(path, held, line);
      return true;
    }
    const end = Date.now() + retryDelayMs;
    while (Date.now() < end) {
      /* spin */
    }
  }
  return false;
}


const INJECTIONS_MAX_BYTES = 2 * 1024 * 1024; // 2MB
const INJECTIONS_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000; // 30d
let lastInjectionsPruneMs = 0;

function pruneInjectionsLedger(filePath) {
  // ISS-20260713-023: best-effort size/age prune (at most once per process minute)
  const now = Date.now();
  if (now - lastInjectionsPruneMs < 60_000) return;
  lastInjectionsPruneMs = now;
  try {
    if (!existsSync(filePath)) return;
    const st = statSync(filePath);
    if (st.size < INJECTIONS_MAX_BYTES && now - st.mtimeMs < INJECTIONS_MAX_AGE_MS) return;
    const raw = readFileSync(filePath, "utf8");
    const lines = raw.split(/\n/).filter((l) => l.trim().length > 0);
    const kept = [];
    for (const line of lines) {
      try {
        const o = JSON.parse(line);
        if (typeof o.ts === "number" && now - o.ts > INJECTIONS_MAX_AGE_MS) continue;
        kept.push(line);
      } catch {
        /* drop corrupt */
      }
    }
    // if still huge, keep last 2000 lines
    const trimmed = kept.length > 2000 ? kept.slice(-2000) : kept;
    const tmp = filePath + ".tmp-" + process.pid;
    writeFileSync(tmp, trimmed.length ? trimmed.join("\n") + "\n" : "", "utf8");
    renameSync(tmp, filePath);
  } catch {
    /* never block hook */
  }
}

function logInjection(projectRoot, record) {
  try {
    if (!projectRoot || !record || typeof record.surface !== "string") {
      return;
    }
    const stableIds = Array.isArray(record.stableIds)
      ? record.stableIds.filter((id) => typeof id === "string")
      : [];
    const count = typeof record.count === "number" ? record.count : stableIds.length;
    if (count <= 0) {
      return;
    }
    const row = {
      ts: typeof record.ts === "number" ? record.ts : Date.now(),
      surface: record.surface,
      count,
      stable_ids: stableIds,
      revision_hash: typeof record.revisionHash === "string" ? record.revisionHash : null,
    };
    const path = join(projectRoot, ".fabric", "injections.jsonl");
    mkdirSync(dirname(path), { recursive: true });
    appendLockedLine(path, `${JSON.stringify(row)}\n`);
    pruneInjectionsLedger(path);
  } catch {
    // Telemetry is best-effort
  }
}

module.exports = { logInjection, appendLockedLine, appendLockedLineWait };
