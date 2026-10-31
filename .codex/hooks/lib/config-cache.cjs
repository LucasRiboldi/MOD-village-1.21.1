/**
 * v2.0.0-rc.37 NEW-19: shared fabric-config reader for hook scripts.
 *
 * Before this lib, every hook re-implemented the same defensive
 * `readFileSync(.fabric/fabric-config.json) → JSON.parse → validate one key →
 * fall back to default` boilerplate, once PER KEY (knowledge-hint-broad alone
 * read the file 5× per SessionStart fire: cooldown / top_k / underseed /
 * summary_max_len / reminder_to_context). This module centralises the read +
 * mtime-keyed memoisation so a single hook fire parses the config once.
 *
 * Provides:
 *   - readConfig(projectRoot) → object
 *       Parsed fabric-config.json (memoised on path+mtime). Returns `{}` on
 *       any failure (missing file / parse error / non-object). Never throws.
 *       mtime-keyed so a config rewrite mid-process (test harness) invalidates
 *       the cached value automatically — production hooks are single-shot so
 *       the common case is one stat + one parse.
 *   - readGlobalConfig() → object
 *       Parsed ~/.fabric/fabric-global.json (or $FABRIC_HOME equivalent),
 *       with the same cache and never-throw behavior.
 *   - readConfigNumber(root, key, fallback, { min, max, integer }) → number
 *   - readConfigBoolean(root, key, fallback) → boolean
 *   - readConfigString(root, key, fallback) → string
 *       PREFERENCE-class getters. Resolution order:
 *         global.projects[<project_id>] > global.defaults > fallback
 *       CORPUS-class knobs are NOT served here — they live in the store and are
 *       read via the sibling `store-config-reader.cjs` (single owner for the
 *       store-config read + team-store root resolution).
 *   - configPathFor(projectRoot) → absolute config path
 *   - clearConfigCache() → void   (test helper)
 *
 * config-single-home W3: the repo config carries IDENTITY only, so the typed
 * getters no longer read it — `readConfig()` remains for identity lookups
 * (project_id / workspace_binding_id / store bindings). A policy key left behind
 * in a repo config is inert, matching the server-side cascade exactly, so there
 * is no "works in the hook but not in recall" split-brain.
 *
 * Never-throw contract: every export degrades to its fallback rather than
 * throwing, preserving the reminder-layer hook invariant (KT-DEC-0007: hooks
 * never block on their own malfunction).
 */

const { existsSync, readFileSync, statSync } = require("node:fs");
const { join } = require("node:path");
const { homedir } = require("node:os");

const FABRIC_DIR_REL = ".fabric";
const FABRIC_CONFIG_FILE = "fabric-config.json";
const GLOBAL_CONFIG_FILE = "fabric-global.json";

// path → { mtime, value }. Per-process; mtime-keyed for test-mutation safety.
const _cache = new Map();

function configPathFor(projectRoot) {
  return join(projectRoot, FABRIC_DIR_REL, FABRIC_CONFIG_FILE);
}

function readConfig(projectRoot) {
  const path = configPathFor(projectRoot);
  let mtime;
  try {
    if (!existsSync(path)) {
      _cache.delete(path);
      return {};
    }
    mtime = statSync(path).mtimeMs;
  } catch {
    return {};
  }
  const cached = _cache.get(path);
  if (cached && cached.mtime === mtime) return cached.value;
  let value = {};
  try {
    const raw = JSON.parse(readFileSync(path, "utf8"));
    if (raw && typeof raw === "object") value = raw;
  } catch {
    value = {};
  }
  _cache.set(path, { mtime, value });
  return value;
}

/**
 * Read the machine-global config at ~/.fabric/fabric-global.json (mtime-cached).
 * Returns {} on any failure. Never throws.
 */
function readGlobalConfig() {
  // FABRIC_HOME is a $HOME stand-in, so the `.fabric` segment is ALWAYS appended
  // — matching shared's resolveGlobalRoot and the sibling
  // bindings-snapshot-reader.cjs. The previous form (`FABRIC_HOME || join(home,
  // ".fabric")`) treated FABRIC_HOME as the global root itself, so under any
  // isolated home (tests / CI / sandboxes) this reader looked one level too high
  // and silently found no config. Invisible in production only because nothing
  // sets FABRIC_HOME there.
  const globalRoot = join(process.env.FABRIC_HOME || homedir(), ".fabric");
  const path = join(globalRoot, GLOBAL_CONFIG_FILE);
  let mtime;
  try {
    if (!existsSync(path)) {
      _cache.delete(path);
      return {};
    }
    mtime = statSync(path).mtimeMs;
  } catch {
    return {};
  }
  const cached = _cache.get(path);
  if (cached && cached.mtime === mtime) return cached.value;
  let value = {};
  try {
    const raw = JSON.parse(readFileSync(path, "utf8"));
    if (raw && typeof raw === "object") value = raw;
  } catch {
    value = {};
  }
  _cache.set(path, { mtime, value });
  return value;
}

function clearConfigCache() {
  _cache.clear();
}

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

/**
 * The two PREFERENCE-class policy segments, most-specific first.
 * `projects[<project_id>]` = this repo's exceptions; `defaults` = machine-wide.
 * project_id comes from the repo config, which is exactly what it still owns.
 */
function readPolicy(projectRoot) {
  const global = readGlobalConfig();
  const defaults = isPlainObject(global.defaults) ? global.defaults : {};
  const projects = isPlainObject(global.projects) ? global.projects : {};
  const projectId = readConfig(projectRoot).project_id;
  const scoped =
    typeof projectId === "string" && projectId !== "" && isPlainObject(projects[projectId])
      ? projects[projectId]
      : {};
  return [scoped, defaults];
}

// opts:
//   min / max   — inclusive range; out-of-range → fallback
//   integer     — require Number.isInteger; non-integer → fallback (strict)
//   floor       — accept any in-range number, return Math.floor(v) (lenient)
// `integer` and `floor` are independent: `integer` rejects fractional values,
// `floor` truncates them. Pick whichever matches the caller's legacy contract.
function numberValidator(opts) {
  const { min, max, integer, floor } = opts || {};
  return (v) => {
    if (typeof v !== "number" || !Number.isFinite(v)) return undefined;
    if (integer && !Number.isInteger(v)) return undefined;
    if (typeof min === "number" && v < min) return undefined;
    if (typeof max === "number" && v > max) return undefined;
    return floor ? Math.floor(v) : v;
  };
}

function firstValid(layers, key, validate) {
  for (const layer of layers) {
    const candidate = validate(layer[key]);
    if (candidate !== undefined) return candidate;
  }
  return undefined;
}

function readConfigNumber(projectRoot, key, fallback, opts) {
  const validate = numberValidator(opts);
  return firstValid(readPolicy(projectRoot), key, validate) ?? fallback;
}

function readConfigBoolean(projectRoot, key, fallback) {
  const validate = (v) => (typeof v === "boolean" ? v : undefined);
  const found = firstValid(readPolicy(projectRoot), key, validate);
  return found === undefined ? fallback : found;
}

function readConfigString(projectRoot, key, fallback) {
  const validate = (v) => (typeof v === "string" && v.length > 0 ? v : undefined);
  return firstValid(readPolicy(projectRoot), key, validate) ?? fallback;
}

module.exports = {
  readConfig,
  readGlobalConfig,
  readPolicy,
  clearConfigCache,
  readConfigNumber,
  readConfigBoolean,
  readConfigString,
  configPathFor,
};
