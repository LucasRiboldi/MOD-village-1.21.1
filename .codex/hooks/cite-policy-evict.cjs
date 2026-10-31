#!/usr/bin/env node
/**
 * v2.1 ⑤ cite-redesign (P5) — recall-based cite accounting hook.
 *
 * PreToolUse(Edit/Write/MultiEdit) hook (all three clients). REPLACES the
 * rc.34 turn-counter UserPromptSubmit reminder: instead of demanding a
 * hand-written `KB:` first line (which the cold-eval converged 2/5 weakest —
 * it forces the agent to declare a citation before it has thought, and the
 * `KB: none` escape hatch made the rule inert), this hook infers the citation
 * from REAL behavior.
 *
 * Core idea: the audit value of a cite = "which knowledge informed this edit".
 * That fact is observable WITHOUT the agent hand-writing anything: if the
 * agent ran `fab_recall(paths)` / `fab_plan_context(paths)` whose target paths
 * overlap the file it is now editing, the server already logged a
 * `knowledge_context_planned` event (target_paths + final_stable_ids +
 * session_id) into `.fabric/events.jsonl`. The recall→edit path overlap IS the
 * citation — doctor `--cite-coverage` (C3) reconstructs it by joining
 * knowledge_context_planned ⋈ edit_intent_checked. No ledger write is needed
 * here; the join is the accounting.
 *
 * Hook responsibility (runtime, this file): the NUDGE. On a PreToolUse edit:
 *   - recall-backed (a recent in-session knowledge_context_planned overlaps the
 *     edit path) → silent. The edit is informed; nothing to remind.
 *   - manual override (the agent already wrote a `KB:` line this session →
 *     observed as an assistant_turn_observed event carrying cite_ids) → silent.
 *     The legacy hand-written cite path is still honored (back-compat).
 *   - otherwise → soft nudge: "改前先 fab_recall(paths)". NUDGE, never a gate
 *     (KT-DEC-0007): Claude Code receives it as a PreToolUse additionalContext
 *     envelope on stdout; Codex as stderr. The edit always proceeds.
 *
 * Config (.fabric/fabric-config.json, with ~/.fabric/fabric-global.json fallback):
 *   - `cite_recall_nudge` (boolean, default true) — master switch. Set false to
 *     silence the nudge entirely (mirrors the cite_evict_interval=0 opt-out
 *     convention of the rc.34 hook this replaces).
 *   - `cite_recall_window_minutes` (number, default 30, >=0) — how far back a
 *     recall counts as "for this edit". 0 = unbounded (any prior in-session
 *     recall of an overlapping path counts).
 *   - `cite_nudge_ignore_globs` (string[], default [".workflow/**"]) — F2: edit
 *     paths matching any glob are exempt from the nudge. Orchestration / meta
 *     files (e.g. `.workflow/` scratchpads) are not source the cite policy is
 *     meant to govern, so editing them should never demand a recall. User globs
 *     are MERGED with the default (not replaced), so the `.workflow/` exemption
 *     always holds. `*` = within a segment, `**` = across segments.
 *
 * Failure invariant: every error path (stdin parse, ledger read, config read,
 * emit failure) MUST end in silent exit 0. The hook never blocks the edit on
 * its own malfunction.
 *
 * Cross-client: PreToolUse(Edit|Write|MultiEdit) is registered on all three
 * clients (Claude Code / Codex CLI) — see hooks/configs/*.json. This
 * is strictly better parity than the rc.34 hook, which was Claude-Code-only
 * for the per-turn window and SessionStart-only for Codex.
 */

const { readFileSync } = require("node:fs");
const { isAbsolute, join, relative } = require("node:path");

// Shared config read + client-aware emit (Claude Code stdout envelope vs
// Codex stderr). The installer copies every lib/*.cjs alongside the hook.
const {
  readConfig,
  readGlobalConfig,
  readPolicy,
  readConfigNumber,
  readConfigBoolean,
} = require("./lib/config-cache.cjs");
const { isClaudeCode, readStdinJson, emitContext } = require("./lib/client-adapter.cjs");
const eventReader = require("./lib/event-reader.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

const EVENTS_LEDGER_REL = join(".fabric", "events.jsonl");

// Tool names that trigger the recall-nudge branch. PreToolUse fires on many
// tool names across clients; we only react to file-edit tools (mirrors
// knowledge-hint-narrow.cjs EDIT_TOOL_NAMES).
const EDIT_TOOL_NAMES = new Set(["Edit", "Write", "MultiEdit", "apply_patch"]);

// Default recency window: a fab_recall within the last 30 minutes counts as
// "informing" the edit. Generous — a long edit session after one recall sweep
// should not re-nudge on every file.
const DEFAULT_CITE_RECALL_WINDOW_MINUTES = 30;

// -----------------------------------------------------------------------------
// Config
// -----------------------------------------------------------------------------

/**
 * Read `cite_recall_nudge` from project config, then machine-global config.
 * Default true (ON).
 * Any failure path (missing file, parse error, non-boolean) → default.
 */
function readNudgeEnabled(cwd) {
  return readConfigBoolean(cwd, "cite_recall_nudge", true, { globalFallback: true });
}

/**
 * TASK-005 (grill G5 / C-004 "全 nudge MUST 可 dismiss"): unified per-signal
 * opt-out. Returns true when "cite-evict" is listed in the GLOBAL policy layer's
 * `hint_dismiss_signals` (config-single-home W3 — the repo fabric-config.json is
 * identity-only, so a copy there is inert) — the same enum that
 * silences the Stop (archive) and SessionStart (review/import/maintenance)
 * surfaces. This is a SECOND opt-out lever alongside the pre-existing
 * `cite_recall_nudge:false` boolean (both silence this hook); listing the key
 * here keeps a single durable dismiss surface across all nudges. Any
 * read/parse failure → not dismissed (never-block).
 */
// config-single-home W3: preference class — first policy layer that declares the
// list wins. An empty array is meaningful ("dismiss nothing").
function readCiteEvictDismissed(cwd) {
  for (const layer of readPolicy(cwd)) {
    if (Array.isArray(layer.hint_dismiss_signals)) {
      return layer.hint_dismiss_signals.includes("cite-evict");
    }
  }
  return false;
}

/**
 * Read `cite_recall_window_minutes` from project config, then machine-global
 * config. Default 30, floor 0 (0 = unbounded). Reuses the shared defensive
 * numeric reader.
 */
function readWindowMinutes(cwd) {
  return readConfigNumber(cwd, "cite_recall_window_minutes", DEFAULT_CITE_RECALL_WINDOW_MINUTES, {
    min: 0,
    integer: true,
    globalFallback: true,
  });
}

// F2: meta/orchestration paths exempt from the cite nudge by default. The cite
// policy governs SOURCE edits ("which knowledge informed this code change");
// editing a `.workflow/` scratchpad is not such an edit, so nudging there is
// pure noise with no clean opt-out before F2.
const DEFAULT_CITE_NUDGE_IGNORE_GLOBS = [".workflow/**"];

/**
 * Read `cite_nudge_ignore_globs` (string[]) from the PREFERENCE policy layer
 * (`global.projects[<project_id>]` → `global.defaults`) and MERGE it with the
 * built-in defaults. Any failure path (no config, non-array, non-string entries)
 * → defaults only. User entries never shrink the default exemption set; they only
 * widen it.
 *
 * config-single-home W3: reads the policy layer rather than the repo config —
 * an ignore-glob list is a user preference, and the repo file is identity-only.
 */
function readIgnoreGlobs(cwd) {
  const out = [...DEFAULT_CITE_NUDGE_IGNORE_GLOBS];
  const layers = [];
  try {
    const global = readGlobalConfig();
    const projects = global && typeof global.projects === "object" && global.projects !== null ? global.projects : {};
    const projectId = readConfig(cwd).project_id;
    if (typeof projectId === "string" && projectId !== "" && Array.isArray(projects[projectId]?.cite_nudge_ignore_globs)) {
      layers.push(projects[projectId].cite_nudge_ignore_globs);
    }
    if (global && typeof global.defaults === "object" && global.defaults !== null && Array.isArray(global.defaults.cite_nudge_ignore_globs)) {
      layers.push(global.defaults.cite_nudge_ignore_globs);
    }
  } catch {
    // fall through to defaults
  }
  for (const list of layers) {
    for (const g of list) {
      if (typeof g === "string" && g.length > 0 && !out.includes(g)) out.push(g);
    }
  }
  return out;
}

/**
 * Compile a simple glob to an anchored RegExp. `**` matches across path
 * separators, `*` matches within a single segment; all other regex-special
 * characters are escaped. Intentionally minimal — the patterns are short path
 * prefixes like `.workflow/**`, not a full gitignore dialect.
 */
function globToRegExp(glob) {
  let re = "";
  for (let i = 0; i < glob.length; i++) {
    const c = glob[i];
    if (c === "*") {
      if (glob[i + 1] === "*") {
        re += ".*";
        i++;
      } else {
        re += "[^/]*";
      }
    } else if (".+?^${}()|[]\\/".includes(c)) {
      re += "\\" + c;
    } else {
      re += c;
    }
  }
  return new RegExp("^" + re + "$");
}

/**
 * True if `normPath` (project-relative, forward-slashed) matches any ignore
 * glob. Used to drop meta/orchestration edits from the nudge.
 */
function pathIsIgnored(normPath, globs) {
  if (typeof normPath !== "string" || normPath.length === 0) return false;
  for (const g of globs) {
    try {
      if (globToRegExp(g).test(normPath)) return true;
    } catch {
      // a malformed user glob never breaks the hook — just skip it
    }
  }
  return false;
}

// -----------------------------------------------------------------------------
// Payload parsing (mirror of knowledge-hint-narrow.cjs conventions)
// -----------------------------------------------------------------------------

function extractToolName(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (typeof payload.tool_name === "string") return payload.tool_name;
  if (typeof payload.tool === "string") return payload.tool;
  return null;
}

function extractToolInput(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (payload.tool_input && typeof payload.tool_input === "object") return payload.tool_input;
  if (payload.input && typeof payload.input === "object") return payload.input;
  return null;
}

/**
 * Pull edit target paths from a tool_input object. Handles scalar file_path,
 * array file_paths, and MultiEdit edits[]. Deduped, first-occurrence order.
 */

/**
 * ISS-20260711-212: harvest paths from a Codex apply_patch tool_input.
 * Codex may pass the patch body as a string (`input` / `patch` / `content`)
 * carrying `*** Update|Add|Delete File:` directives (same grammar fabric-hint
 * already parses for transcript digests).
 */
function extractApplyPatchPaths(toolInput) {
  if (!toolInput || typeof toolInput !== "object") return [];
  const candidates = [toolInput.input, toolInput.patch, toolInput.content, toolInput.file_path];
  const collected = [];
  const fileDirectiveRe = /^\*\*\*\s+(?:Update|Add|Delete)\s+File:\s+(.+?)\s*$/gm;
  for (const c of candidates) {
    if (typeof c !== "string" || c.length === 0) continue;
    // Plain path form (rare): treat non-patch strings that look like paths.
    if (!c.includes("***") && (c.includes("/") || c.endsWith(".ts") || c.endsWith(".js") || c.endsWith(".md"))) {
      // Only accept when the field is file_path-like and short.
      if (c.length < 512 && !c.includes("\n")) collected.push(c);
      continue;
    }
    let m;
    fileDirectiveRe.lastIndex = 0;
    while ((m = fileDirectiveRe.exec(c)) !== null) {
      const fp = m[1].trim();
      if (fp.length > 0) collected.push(fp);
    }
  }
  return collected;
}

function extractPaths(toolInput) {
  if (!toolInput || typeof toolInput !== "object") return [];
  const collected = [];
  if (typeof toolInput.file_path === "string" && toolInput.file_path.length > 0) {
    collected.push(toolInput.file_path);
  }
  if (Array.isArray(toolInput.file_paths)) {
    for (const p of toolInput.file_paths) {
      if (typeof p === "string" && p.length > 0) collected.push(p);
    }
  }
  if (Array.isArray(toolInput.edits)) {
    for (const edit of toolInput.edits) {
      if (edit && typeof edit === "object" && typeof edit.file_path === "string" && edit.file_path.length > 0) {
        collected.push(edit.file_path);
      }
    }
  }
  // ISS-20260711-212: Codex apply_patch path harvest
  for (const p of extractApplyPatchPaths(toolInput)) {
    collected.push(p);
  }

  const seen = new Set();
  const out = [];
  for (const p of collected) {
    if (seen.has(p)) continue;
    seen.add(p);
    out.push(p);
  }
  return out;
}

function resolveSessionId(payload, env) {
  // ISS-20260712-006: never invent "anonymous" — that sentinel cannot match real
  // knowledge_context_planned rows and false-fires the cite nudge after real recalls.
  // Missing session_id → fail open (caller skips nudge).
  if (payload && typeof payload === "object" && typeof payload.session_id === "string" && payload.session_id.length > 0) {
    return payload.session_id;
  }
  const envBag = (env && env.processEnv) || process.env;
  if (envBag && typeof envBag.FABRIC_SESSION_ID === "string" && envBag.FABRIC_SESSION_ID.length > 0) {
    return envBag.FABRIC_SESSION_ID;
  }
  return null;
}

// -----------------------------------------------------------------------------
// Path overlap
// -----------------------------------------------------------------------------

/**
 * Normalize a path for overlap comparison: project-relative when possible,
 * forward-slashed, leading "./" and "/" stripped. Absolute paths inside the
 * project become relative; absolute paths outside collapse to a basename-style
 * tail so an abs-vs-rel suffix match still works.
 */
function normalizeForCompare(p, projectRoot) {
  if (typeof p !== "string" || p.length === 0) return "";
  let s = p;
  if (isAbsolute(s) && typeof projectRoot === "string" && projectRoot.length > 0) {
    const rel = relative(projectRoot, s);
    if (rel.length > 0 && !rel.startsWith("..")) s = rel;
  }
  s = s.split("\\").join("/");
  while (s.startsWith("./")) s = s.slice(2);
  while (s.startsWith("/")) s = s.slice(1);
  return s;
}

/**
 * Does `editNorm` fall within the scope a recall asked for (`recallNorm`)?
 * True when they are equal, when one is a path-boundary suffix of the other
 * (handles abs-vs-rel skew), or when one is an ancestor directory of the
 * other. Conservative — avoids basename-only matches that would over-fire.
 */
function pathPairOverlaps(editNorm, recallNorm) {
  if (editNorm.length === 0 || recallNorm.length === 0) return false;
  if (editNorm === recallNorm) return true;
  if (editNorm.endsWith("/" + recallNorm) || recallNorm.endsWith("/" + editNorm)) return true;
  if (editNorm.startsWith(recallNorm + "/") || recallNorm.startsWith(editNorm + "/")) return true;
  return false;
}

function pathsOverlap(recallPaths, editPaths, projectRoot) {
  if (!Array.isArray(recallPaths) || !Array.isArray(editPaths)) return false;
  const edits = editPaths.map((e) => normalizeForCompare(e, projectRoot)).filter((e) => e.length > 0);
  const recalls = recallPaths.map((r) => normalizeForCompare(r, projectRoot)).filter((r) => r.length > 0);
  for (const e of edits) {
    for (const r of recalls) {
      if (pathPairOverlaps(e, r)) return true;
    }
  }
  return false;
}

// -----------------------------------------------------------------------------
// Events ledger read
// -----------------------------------------------------------------------------

/**
 * Read + parse `.fabric/events.jsonl` best-effort. Returns an array of parsed
 * line objects (only those with a numeric `ts`). Never throws — a missing or
 * corrupt ledger yields []. Lines that fail JSON.parse are skipped.
 */
function readEventsLedger(cwd) {
  // ISS-20260713-032/008: shared tail reader (no full-file load).
  try {
    return eventReader.readRecentEvents(cwd);
  } catch {
    return [];
  }
}

// -----------------------------------------------------------------------------
// Decision
// -----------------------------------------------------------------------------

/**
 * Pure decision helper (unit-testable). Given the ledger events, the edit
 * target paths, the session id, the current time and the recency window,
 * decide whether the edit is recall-backed and/or manually cited.
 *
 * @returns {{ recallBacked: boolean, recalledIds: string[],
 *             matchedRecallTs: number|null, manualCited: boolean }}
 *
 * Contract:
 *   - Only events with matching `session_id` are considered.
 *   - windowMs <= 0 → unbounded (any prior in-session event counts).
 *   - recallBacked: a `knowledge_context_planned` event whose `target_paths`
 *     overlap `editPaths` exists in-window. recalledIds = its final_stable_ids
 *     (union across all matching recalls). matchedRecallTs = the latest match.
 *   - manualCited: an `assistant_turn_observed` event with a non-empty
 *     `cite_ids` array exists in-window (the legacy hand-written-`KB:` path).
 */
function evaluateRecallCite({ events, editPaths, sessionId, nowMs, windowMs, projectRoot }) {
  const result = { recallBacked: false, recalledIds: [], matchedRecallTs: null, manualCited: false };
  if (!Array.isArray(events) || events.length === 0) return result;
  const sinceMs = typeof windowMs === "number" && windowMs > 0 ? nowMs - windowMs : null;
  const recalledSet = new Set();
  for (const ev of events) {
    if (ev.session_id !== sessionId) continue;
    if (typeof ev.ts !== "number") continue;
    if (sinceMs !== null && ev.ts < sinceMs) continue;
    // Future events (ts > nowMs) are ignored — a recall cannot inform an edit
    // that happened before it.
    if (ev.ts > nowMs) continue;

    if (ev.event_type === "knowledge_context_planned") {
      if (pathsOverlap(ev.target_paths, editPaths, projectRoot)) {
        result.recallBacked = true;
        if (result.matchedRecallTs === null || ev.ts > result.matchedRecallTs) {
          result.matchedRecallTs = ev.ts;
        }
        const ids = Array.isArray(ev.final_stable_ids) ? ev.final_stable_ids : [];
        for (const id of ids) {
          if (typeof id === "string" && id.length > 0) recalledSet.add(id);
        }
      }
    } else if (ev.event_type === "assistant_turn_observed") {
      const ids = Array.isArray(ev.cite_ids) ? ev.cite_ids : [];
      if (ids.some((id) => typeof id === "string" && id.length > 0)) {
        result.manualCited = true;
      }
    }
  }
  result.recalledIds = [...recalledSet];
  return result;
}

// -----------------------------------------------------------------------------
// Nudge rendering
// -----------------------------------------------------------------------------

/**
 * Build the soft nudge body. Compact, non-blocking — it tells the agent to
 * recall BEFORE editing so the citation is auto-accounted. NUDGE not gate.
 */
function renderNudge(editPaths) {
  const target = Array.isArray(editPaths) && editPaths.length > 0
    ? editPaths.slice(0, 3).join(", ") + (editPaths.length > 3 ? ` (+${editPaths.length - 3})` : "")
    : "this file";
  return [
    `[fabric cite] 改 ${target} 前未检测到相关 fab_recall —`,
    "建议先调 fab_recall(paths=[<被改文件>]) 让系统自动记账引用的 KB(无需手写首行 KB:)。",
    "已 recall 过可忽略本提示。仍可手写首行 `KB: <id> [applied]` 显式 override。",
    "(nudge only — 不阻塞本次编辑;cite 覆盖率见 fabric audit cite)",
  ].join("\n");
}

// -----------------------------------------------------------------------------
// Main
// -----------------------------------------------------------------------------

async function main(env, stdio) {
  try {
    const cwd =
      (env && typeof env.cwd === "string" && env.cwd) ||
      process.env.CLAUDE_PROJECT_DIR ||
      process.cwd();

    if (!readNudgeEnabled(cwd)) {
      return; // feature off — silent
    }

    // TASK-005 (grill G5 / C-004): durable per-signal opt-out via the unified
    // hint_dismiss_signals enum. "cite-evict" here silences this nudge exactly
    // like cite_recall_nudge:false, but through the same enum that dismisses
    // every other Fabric nudge surface.
    if (readCiteEvictDismissed(cwd)) {
      return; // dismissed via hint_dismiss_signals — silent
    }

    const payload = env && env.payload !== undefined ? env.payload : await readStdinJson();

    const toolName = extractToolName(payload);
    if (!toolName || !EDIT_TOOL_NAMES.has(toolName)) {
      // ISS-20260711-214 / NEW-21: Codex registers this script as a 2nd
      // SessionStart entry. SessionStart payloads have no tool_name — emit a
      // one-shot soft cite tip (never a gate) instead of going fully silent.
      const eventName =
        payload &&
        (payload.hook_event_name || payload.event_name || payload.event || payload.type);
      const isSessionStart =
        eventName === "SessionStart" ||
        eventName === "session_start" ||
        (payload &&
          typeof payload === "object" &&
          !toolName &&
          payload.tool_input === undefined &&
          payload.input === undefined);
      if (isSessionStart) {
        const streams = (env && env.stdio) || stdio || {};
        emitContext(
          [
            "[fabric cite] SessionStart: 编辑前先 fab_recall(paths=[...])，系统会自动记账引用。",
            "(nudge only — 不阻塞; cite 覆盖率见 fabric audit cite)",
          ].join("\n"),
          {
            client: isClaudeCode() || (env && env.forceClaudeCode === true) ? "cc" : undefined,
            eventName: "SessionStart",
            forceStderr: true,
            streams,
          },
        );
      }
      return;
    }

    const editPaths = extractPaths(extractToolInput(payload));
    if (editPaths.length === 0) {
      return; // no recognizable edit target — silent
    }

    // F2: drop meta/orchestration edits (e.g. `.workflow/` scratchpads) before
    // nudging. If EVERY target is exempt, stay silent — the cite policy does not
    // apply to these paths. A mixed batch keeps the non-exempt targets.
    const ignoreGlobs = readIgnoreGlobs(cwd);
    const nudgePaths = editPaths.filter((p) => !pathIsIgnored(normalizeForCompare(p, cwd), ignoreGlobs));
    if (nudgePaths.length === 0) {
      return; // all edit targets are cite-exempt — silent
    }

    const sessionId = resolveSessionId(payload, env);
    const nowMs = env && typeof env.nowMs === "number" ? env.nowMs : Date.now();
    const windowMs = readWindowMinutes(cwd) * 60_000;

    const events = readEventsLedger(cwd);
    const decision = evaluateRecallCite({
      events,
      editPaths: nudgePaths,
      sessionId,
      nowMs,
      windowMs,
      projectRoot: cwd,
    });

    // Recall-backed or manually cited → the edit is informed; stay silent.
    if (decision.recallBacked || decision.manualCited) {
      return;
    }

    // No recall, no manual cite → soft nudge. Claude Code: PreToolUse stdout
    // additionalContext envelope. Codex: stderr. Never a gate.
    const streams = (env && env.stdio) || stdio || {};
    const onClaudeCode = isClaudeCode() || (env && env.forceClaudeCode === true);
    emitContext(renderNudge(nudgePaths), {
      client: onClaudeCode ? "cc" : undefined,
      eventName: "PreToolUse",
      forceStderr: !onClaudeCode,
      streams,
    });
  } catch {
    // Silent — never block the edit on hook failure.
  }
}

module.exports = {
  main,
  extractToolName,
  extractToolInput,
  extractPaths,
  resolveSessionId,
  readNudgeEnabled,
  // TASK-005 (grill G5 / C-004): "cite-evict" dismiss reader — exported for tests.
  readCiteEvictDismissed,
  readWindowMinutes,
  readIgnoreGlobs,
  globToRegExp,
  pathIsIgnored,
  readEventsLedger,
  normalizeForCompare,
  pathPairOverlaps,
  pathsOverlap,
  evaluateRecallCite,
  renderNudge,
};

if (require.main === module) {
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot });
}
