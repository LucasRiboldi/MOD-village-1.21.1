// config-layering W3 (TASK-004) — SINGLE-OWNER store-layer config reader for
// hooks (SSOT: one owner file for the store-config number read + the team-store
// root resolution; consumers require this, never copy the body).
//
// The store layer is the `store-config.json` committed at a store ROOT (parallel
// to store.json). A team may DEFAULT the corpus-shaping hook knobs
// (broad_index_backstop / underseed_node_threshold) there so every repo bound to
// the store inherits the tuning. This lib gives the hooks:
//   - resolveTeamStoreRoot(snapshot) / resolveTeamStoreRootFromProject(root):
//     the TEAM/shared store ROOT, aligned with the server's
//     config-loader.resolveStoreConfig → resolveWriteTargetStoreDir('team') so
//     hook and server read the SAME store-config.json.
//   - readStoreConfigNumber(storeRoot, key, {min,max}): a tolerant, integer +
//     range-guarded single-field read matching storeConfigSchema's per-knob
//     constraints (write-strict / read-tolerant, KT-DEC-0048).
//   - readEnvInt(name, {min,max}): the shared env-layer coercion for the cascade.
//
// KT-DEC-0070: a hook lib MUST NOT require server/shared TS — this is a zero-dep
// sibling .cjs (only node:fs / node:path + the sibling bindings-snapshot-reader
// for the from-project convenience). The STORE_LAYOUT.configFile / knowledgeDir
// basenames are inlined as local constants (kept in sync with
// packages/shared/src/schemas/store.ts). Hot-path safe (C-008): every read
// swallows failure → undefined/null, never throws.

const { existsSync, readFileSync } = require("node:fs");
const { join, basename, dirname } = require("node:path");

// Inlined from STORE_LAYOUT (packages/shared/src/schemas/store.ts). Kept as local
// literals to honor the no-TS-require rule (KT-DEC-0070). Keep in sync if the
// canonical basenames ever change.
const STORE_CONFIG_FILE = "store-config.json";
const STORE_KNOWLEDGE_DIR = "knowledge";

const FABRIC_DIR_REL = ".fabric";
const FABRIC_CONFIG_FILE = "fabric-config.json";

/**
 * Tolerant single-field numeric read from a store's `store-config.json`. Mirrors
 * config-loader.resolveStoreConfig's store-layer semantics for a single knob:
 * the value is honored ONLY when it is a finite INTEGER within [min, max]
 * (matching storeConfigSchema's `.int().min().max()` per-field constraints).
 * Any miss — no store root, absent file, malformed JSON, non-object, non-integer,
 * or out-of-range — returns `undefined` so the caller's cascade falls through to
 * its default. NEVER throws (hot-path safe).
 */
function readStoreConfigNumber(storeRoot, key, opts) {
  if (typeof storeRoot !== "string" || storeRoot.length === 0) {
    return undefined;
  }
  const { min, max } = opts || {};
  try {
    const parsed = JSON.parse(readFileSync(join(storeRoot, STORE_CONFIG_FILE), "utf8"));
    if (!parsed || typeof parsed !== "object") {
      return undefined;
    }
    const v = parsed[key];
    if (typeof v !== "number" || !Number.isFinite(v) || !Number.isInteger(v)) {
      return undefined;
    }
    if (typeof min === "number" && v < min) {
      return undefined;
    }
    if (typeof max === "number" && v > max) {
      return undefined;
    }
    return v;
  } catch {
    // Absent file / malformed JSON → fall through to the next cascade layer.
    return undefined;
  }
}

/**
 * Read a NUMERIC env override, coercing the (always-string) env value to a finite
 * INTEGER within [min, max]. Mirrors config-loader.envNum + intGuard: unset /
 * blank / non-numeric / non-integer / out-of-range → undefined (layer absent).
 */
function readEnvInt(name, opts) {
  const raw = process.env[name];
  if (typeof raw !== "string" || raw.trim() === "") {
    return undefined;
  }
  const n = Number(raw);
  if (!Number.isFinite(n) || !Number.isInteger(n)) {
    return undefined;
  }
  const { min, max } = opts || {};
  if (typeof min === "number" && n < min) {
    return undefined;
  }
  if (typeof max === "number" && n > max) {
    return undefined;
  }
  return n;
}

// Store ROOT from a snapshot dir entry. knowledge_store_dirs entries are already
// store ROOTs (bindings.ts persists `join(globalRoot, storeRelativePathForMount)`),
// but defensively derive the root if a dir ever points at the knowledge/ subdir.
function deriveStoreRoot(dir) {
  if (typeof dir !== "string" || dir.length === 0) {
    return null;
  }
  return basename(dir) === STORE_KNOWLEDGE_DIR ? dirname(dir) : dir;
}

/**
 * Resolve the TEAM/shared store ROOT from a resolved-bindings snapshot. Aligns
 * with the server's resolveWriteTargetStoreDir('team'): the snapshot's
 * `write_target` IS the resolved non-personal (team) write target, and
 * `knowledge_store_dirs[i]` is the ROOT of `read_set.stores[i]` (same read-set
 * order, bindings.ts). Returns the store ROOT string, or null when the snapshot
 * lacks a write target / matching dir (degrade → store layer absent).
 */
function resolveTeamStoreRoot(snapshot) {
  if (!snapshot || typeof snapshot !== "object") {
    return null;
  }
  // config-single-home W3: prefer the EXPLICIT field. The positional fallback
  // below pairs `write_target` with `knowledge_store_dirs[i]` via read-set order —
  // a contract nothing enforces, so it silently returns the WRONG store if that
  // ordering ever changes. Snapshots written before this field existed still take
  // the fallback path.
  if (typeof snapshot.write_target_store_dir === "string" && snapshot.write_target_store_dir.length > 0) {
    return deriveStoreRoot(snapshot.write_target_store_dir);
  }
  const target = snapshot.write_target;
  if (!target || typeof target.store_uuid !== "string") {
    return null;
  }
  const stores =
    snapshot.read_set && Array.isArray(snapshot.read_set.stores) ? snapshot.read_set.stores : [];
  const dirs = Array.isArray(snapshot.knowledge_store_dirs) ? snapshot.knowledge_store_dirs : [];
  const idx = stores.findIndex((s) => s && s.store_uuid === target.store_uuid);
  if (idx < 0 || idx >= dirs.length) {
    return null;
  }
  return deriveStoreRoot(dirs[idx]);
}

// Read the workspace binding id (the snapshot key) from a project's
// fabric-config.json, defaulting to project_id. null on any read/parse failure.
function readWorkspaceBindingId(projectRoot) {
  try {
    const raw = readFileSync(join(projectRoot, FABRIC_DIR_REL, FABRIC_CONFIG_FILE), "utf8");
    const parsed = JSON.parse(raw);
    if (typeof parsed.workspace_binding_id === "string") {
      return parsed.workspace_binding_id;
    }
    return typeof parsed.project_id === "string" ? parsed.project_id : null;
  } catch {
    return null;
  }
}

/**
 * Convenience for callers that only hold a projectRoot (e.g. hint-config.cjs
 * behind fabric-hint): load the resolved-bindings snapshot for the workspace and
 * resolve the team store ROOT from it. Returns null on any miss (no binding id,
 * no snapshot, no reader lib). The bindings-snapshot-reader require is LAZY so
 * cross-package importers that only use readStoreConfigNumber/readEnvInt never
 * pay it (and it never throws at module load).
 */
function resolveTeamStoreRootFromProject(projectRoot) {
  const bindingId = readWorkspaceBindingId(projectRoot);
  if (bindingId === null) {
    return null;
  }
  let snapshotReader;
  try {
    snapshotReader = require("./bindings-snapshot-reader.cjs");
  } catch {
    return null;
  }
  let snapshot;
  try {
    snapshot = snapshotReader.readBindingsSnapshot(bindingId);
  } catch {
    return null;
  }
  return resolveTeamStoreRoot(snapshot);
}

module.exports = {
  STORE_CONFIG_FILE,
  readStoreConfigNumber,
  readEnvInt,
  resolveTeamStoreRoot,
  resolveTeamStoreRootFromProject,
};
