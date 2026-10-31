#!/usr/bin/env node
// v2.1.0-rc.1 P4 — hook-side resolved-bindings snapshot reader (F4/S63/S65).
//
// Hooks are a REMINDER layer (KT-DEC-0007) and must never block. They are also
// FORBIDDEN from re-resolving stores or walking `.fabric` store trees directly
// — a hook reads ONLY the CLI-pre-generated snapshot at
// `~/.fabric/state/bindings/<workspace_binding_id>_resolved.json` (written by P3
// install/sync/bind). This keeps the resolver logic in one place (the CLI) and
// keeps hooks a thin, store-unaware-by-construction projection. Missing /
// unreadable / malformed snapshot → null (harmless degrade; the hook proceeds
// without store labels). Zero-dep CJS so it inline-loads at hook runtime.

const { existsSync, readFileSync, readdirSync, statSync } = require("node:fs");
const { join } = require("node:path");
const { homedir } = require("node:os");

// Canonical knowledge type dirs (mirror STORE_KNOWLEDGE_TYPE_DIRS in
// packages/shared/src/schemas/store.ts). Kept inline — this zero-dep reader
// runs in user repos without node_modules access.
const KNOWLEDGE_CANONICAL_TYPES = [
  "decisions",
  "pitfalls",
  "guidelines",
  "models",
  "processes",
];
const KNOWLEDGE_SUBDIR = "knowledge";
const PENDING_SUBDIR = "pending";

// `~/.fabric` (FABRIC_HOME override mirrors the CLI's resolveGlobalRoot).
function resolveGlobalRoot() {
  return join(process.env.FABRIC_HOME || homedir(), ".fabric");
}

function bindingsSnapshotPath(bindingId, globalRoot) {
  return join(
    globalRoot || resolveGlobalRoot(),
    "state",
    "bindings",
    bindingId + "_resolved.json",
  );
}

// Read + shallow-validate the snapshot. Returns the parsed object, or null when
// absent / unreadable / not the expected shape. NEVER throws.
function readBindingsSnapshot(bindingId, globalRoot) {
  if (typeof bindingId !== "string" || bindingId.length === 0) {
    return null;
  }
  const path = bindingsSnapshotPath(bindingId, globalRoot);
  if (!existsSync(path)) {
    return null;
  }
  try {
    const parsed = JSON.parse(readFileSync(path, "utf8"));
    if (
      parsed &&
      typeof parsed === "object" &&
      parsed.read_set &&
      Array.isArray(parsed.read_set.stores)
    ) {
      return parsed;
    }
    return null;
  } catch {
    return null;
  }
}

// Recursively count *.md files under `dir`, tracking the oldest mtime. Missing
// / unreadable dirs contribute zero (degrade silently — a hook never throws).
// ISS-20260713-031: process-local cache keyed by store root mtimes — avoids full
// recursive recount on every SessionStart/Stop when content is unchanged.
const liveStatsCache = new Map(); // key -> { pendingCount, canonicalCount, oldestPendingMtimeMs }

function storeDirsFingerprint(dirs) {
  const parts = [];
  for (const d of dirs) {
    if (typeof d !== "string" || d.length === 0) continue;
    try {
      parts.push(d + "|" + String(statSync(d).mtimeMs));
    } catch {
      parts.push(d + "|missing");
    }
  }
  return parts.join("\n");
}

function countMarkdownFiles(dir) {
  let count = 0;
  let oldestMtimeMs = null;
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return { count, oldestMtimeMs };
  }
  for (const entry of entries) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      const nested = countMarkdownFiles(fullPath);
      count += nested.count;
      if (
        nested.oldestMtimeMs !== null &&
        (oldestMtimeMs === null || nested.oldestMtimeMs < oldestMtimeMs)
      ) {
        oldestMtimeMs = nested.oldestMtimeMs;
      }
      continue;
    }
    if (!entry.isFile() || !entry.name.endsWith(".md")) {
      continue;
    }
    let mtimeMs;
    try {
      mtimeMs = statSync(fullPath).mtimeMs;
    } catch {
      continue;
    }
    count += 1;
    if (oldestMtimeMs === null || mtimeMs < oldestMtimeMs) {
      oldestMtimeMs = mtimeMs;
    }
  }
  return { count, oldestMtimeMs };
}

// LIVE store-backed knowledge counts for nudges (underseed canonical_count,
// review-backlog pending_count). The snapshot's cached `knowledge_stats` is a
// store-global projection frozen at write time, so it goes stale whenever store
// content changes out-of-band (a `git pull` in the store repo, a sync run from a
// *different* bound workspace) — that staleness is the root cause of the phantom
// review-backlog (KT-PIT-0017) and the false "knowledge sparse" underseed nudge.
//
// Fix: the snapshot persists the resolved store ROOT dirs (`knowledge_store_dirs`,
// stable across content sync — they only change when mounts/bindings change,
// which regenerates the snapshot). Recount the *.md files under those dirs LIVE
// so the numbers are always fresh regardless of how content changed. Falls back
// to the cached `knowledge_stats` for snapshots written before this field
// existed. Returns null only when neither source is available.
// ISS-20260713-022: single authority for hook-side counts when knowledge_store_dirs
// is present = LIVE recount (not store-counters JSON, not dual algorithms).
// Snapshot knowledge_stats is write-time only and never used for nudges when live
// dirs are available (KT-PIT-0017 false-nudge).
function liveKnowledgeStats(snapshot) {
  if (!snapshot || typeof snapshot !== "object") {
    return null;
  }
  const dirs = snapshot.knowledge_store_dirs;
  if (Array.isArray(dirs) && dirs.length > 0) {
    const fp = storeDirsFingerprint(dirs);
    const hit = liveStatsCache.get(fp);
    if (hit) return hit;
    let pendingCount = 0;
    let canonicalCount = 0;
    let oldestPendingMtimeMs = null;
    for (const storeDir of dirs) {
      if (typeof storeDir !== "string" || storeDir.length === 0) {
        continue;
      }
      for (const type of KNOWLEDGE_CANONICAL_TYPES) {
        canonicalCount += countMarkdownFiles(join(storeDir, KNOWLEDGE_SUBDIR, type)).count;
      }
      const pending = countMarkdownFiles(join(storeDir, KNOWLEDGE_SUBDIR, PENDING_SUBDIR));
      pendingCount += pending.count;
      if (
        pending.oldestMtimeMs !== null &&
        (oldestPendingMtimeMs === null || pending.oldestMtimeMs < oldestPendingMtimeMs)
      ) {
        oldestPendingMtimeMs = pending.oldestMtimeMs;
      }
    }
    const result = { pendingCount, canonicalCount, oldestPendingMtimeMs };
    liveStatsCache.set(fp, result);
    // bound cache size
    if (liveStatsCache.size > 16) {
      const first = liveStatsCache.keys().next().value;
      liveStatsCache.delete(first);
    }
    return result;
  }
  // #3 (GH issue): snapshot predates knowledge_store_dirs. The cached
  // `knowledge_stats` projection is frozen at snapshot-write time and goes stale
  // out-of-band (store grew via git pull / cross-workspace sync), so trusting it
  // re-introduced exactly the false-nudge this whole field cures — observed a
  // store with 61 live canonical entries whose cached count was frozen at 1,
  // mis-firing the "knowledge sparse → /fabric-archive" underseed nudge AND
  // defeating the fabric-import `canonical > 50 → SKIP` guard. read_set carries
  // no resolved store root either (alias/uuid only), so a live recount is
  // impossible without re-resolution (which hooks must not do). Return null
  // ("undeterminable") so callers SKIP the nudge rather than act on a stale
  // count — old snapshots self-heal on the next install/sync/store-op (which
  // regenerates the snapshot WITH knowledge_store_dirs). 宁可不弹也别误弹
  // (KT-DEC-0007: hook = nudge, never a false-positive gate).
  return null;
}

// Render a compact, per-store label line for a SessionStart / Stop hook from a
// snapshot. Empty string when there is nothing to show (degrade silently). The
// label is provenance only — it never re-resolves; it just echoes the read-set
// the CLI already computed, with the write-target flagged (F4 store labels).
function formatStoreLabels(snapshot) {
  if (!snapshot || !snapshot.read_set || !Array.isArray(snapshot.read_set.stores)) {
    return "";
  }
  const writeAlias = snapshot.write_target && snapshot.write_target.alias;
  const parts = snapshot.read_set.stores.map((store) => {
    const tag = store.alias === writeAlias ? " (write)" : store.writable ? "" : " (ro)";
    return store.alias + tag;
  });
  if (parts.length === 0) {
    return "";
  }
  return "[fabric] read-set stores: " + parts.join(", ");
}

module.exports = {
  resolveGlobalRoot,
  bindingsSnapshotPath,
  readBindingsSnapshot,
  liveKnowledgeStats,
  formatStoreLabels,
};
