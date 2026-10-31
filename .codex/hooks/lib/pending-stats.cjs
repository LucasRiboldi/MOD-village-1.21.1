// ISS-20260713-020: pending / canonical knowledge stats for Stop-hook signals.
// Live recount via bindings snapshot (KT-PIT-0017 / KT-PIT-0019).

const { existsSync, readdirSync, statSync, readFileSync } = require("node:fs");
const { join } = require("node:path");

const FABRIC_DIR = ".fabric";
const PENDING_DIR = "knowledge/pending";
const PENDING_TYPES = ["decisions", "pitfalls", "guidelines", "models", "processes"];

let bindingsSnapshotReader = null;
try {
  bindingsSnapshotReader = require("./bindings-snapshot-reader.cjs");
} catch {
  bindingsSnapshotReader = null;
}

function readWorkspaceBindingId(cwd) {
  try {
    const parsed = JSON.parse(readFileSync(join(cwd, ".fabric", "fabric-config.json"), "utf8"));
    if (typeof parsed.workspace_binding_id === "string") return parsed.workspace_binding_id;
    return typeof parsed.project_id === "string" ? parsed.project_id : null;
  } catch {
    return null;
  }
}

function readSnapshotKnowledgeStats(projectRoot, now) {
  const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
  const empty = { pendingCount: 0, oldestPendingAgeMs: null, canonicalCount: 0 };
  if (bindingsSnapshotReader === null) {
    return null;
  }
  const bindingId = readWorkspaceBindingId(projectRoot);
  if (bindingId === null) {
    return null;
  }
  try {
    const snapshot = bindingsSnapshotReader.readBindingsSnapshot(bindingId);
    if (!snapshot) {
      return empty;
    }
    const live = bindingsSnapshotReader.liveKnowledgeStats(snapshot);
    if (live === null) {
      return undefined;
    }
    const pendingCount =
      Number.isFinite(live.pendingCount) && live.pendingCount > 0 ? Math.floor(live.pendingCount) : 0;
    const canonicalCount =
      Number.isFinite(live.canonicalCount) && live.canonicalCount > 0
        ? Math.floor(live.canonicalCount)
        : 0;
    const oldestPendingAgeMs =
      pendingCount > 0 &&
      Number.isFinite(live.oldestPendingMtimeMs) &&
      live.oldestPendingMtimeMs > 0
        ? Math.max(0, nowMs - live.oldestPendingMtimeMs)
        : null;
    return { pendingCount, oldestPendingAgeMs, canonicalCount };
  } catch {
    return empty;
  }
}

function readLegacyPendingStats(projectRoot, now) {
  const nowMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
  const baseDir = join(projectRoot, FABRIC_DIR, PENDING_DIR);

  let count = 0;
  let oldestMtime = null;

  if (!existsSync(baseDir)) {
    return { count: 0, oldestAgeMs: null };
  }

  for (const type of PENDING_TYPES) {
    const typeDir = join(baseDir, type);
    if (!existsSync(typeDir)) continue;

    let entries;
    try {
      entries = readdirSync(typeDir);
    } catch {
      continue;
    }

    for (const entry of entries) {
      if (!entry.endsWith(".md")) continue;
      const filePath = join(typeDir, entry);
      let mtime;
      try {
        mtime = statSync(filePath).mtimeMs;
      } catch {
        continue;
      }
      count += 1;
      if (oldestMtime === null || mtime < oldestMtime) {
        oldestMtime = mtime;
      }
    }
  }

  return {
    count,
    oldestAgeMs: count > 0 && oldestMtime !== null ? nowMs - oldestMtime : null,
  };
}

/**
 * Read pending counts from the CLI-generated resolved-bindings snapshot.
 */
function readPendingStats(projectRoot, now) {
  const stats = readSnapshotKnowledgeStats(projectRoot, now);
  if (stats != null) {
    return { count: stats.pendingCount, oldestAgeMs: stats.oldestPendingAgeMs };
  }
  return readLegacyPendingStats(projectRoot, now);
}

/**
 * Count canonical knowledge entries from the CLI-generated resolved-bindings snapshot.
 */
function countCanonicalNodes(projectRoot) {
  const stats = readSnapshotKnowledgeStats(projectRoot);
  if (stats === undefined) return null;
  return stats === null ? 0 : stats.canonicalCount;
}

module.exports = {
  readWorkspaceBindingId,
  readSnapshotKnowledgeStats,
  readLegacyPendingStats,
  readPendingStats,
  countCanonicalNodes,
  PENDING_DIR,
  PENDING_TYPES,
};
