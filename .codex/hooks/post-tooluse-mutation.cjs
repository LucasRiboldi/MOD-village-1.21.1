#!/usr/bin/env node
/**
 * lifecycle-refactor W2-T3 — PostToolUse mutation marker hook (previously
 * dormant). Closes the mutation env opened by the PreToolUse narrow hint.
 *
 * PostToolUse fires AFTER a tool call completes. This hook serves two markers,
 * both appended to `.fabric/events.jsonl` and both observation-only:
 *   - Edit/Write/MultiEdit → one `file_mutated` event per edited path, carrying
 *     the `tool_call_id` so doctor can pair the Pre (intent) and Post (mutation)
 *     halves of a single call (the per-call key also guards parallel-fire races).
 *   - Read → one `knowledge_body_read` event per Fabric knowledge file opened
 *     (KT-DEC-0030). After retrieval collapsed to one lean tool (KT-DEC-0026),
 *     fab_recall returns descriptions + paths only; the agent reads a body via a
 *     NATIVE Read, and this marker is the observable trace doctor uses for the
 *     planned → body_read → cite[applied] funnel.
 *
 * Design (lifecycle-concept-final.md §1 FROZEN invariants + §5 row7):
 *   - LOW compute: extract paths + tool_call_id, append; the hook never reads
 *     or aggregates the ledger, never runs `git diff`. ALL mutation_pool /
 *     attribution work is doctor-side (offline, §5 row7).
 *   - hook = nudge/marker, never a gate (KT-DEC-0007): every error path ends
 *     in a silent exit 0; we never throw upward.
 *   - Front-stage O(1) per path: advisory-locked append, no traversal.
 *   - Per-event session_id: threaded from the REAL payload when present
 *     (omitted when the client omits it — the marker is still useful for the
 *     tool_call_id pairing even without a session).
 *   - Hooks never require() the server package — only co-located lib/*.cjs.
 *
 * Each emitted line matches `fileMutatedEventSchema`
 * (packages/shared/src/schemas/event-ledger.ts):
 *   { kind:"fabric-event", id, ts, schema_version:1, session_id?,
 *     event_type:"file_mutated", path, tool_call_id, tool_name? }
 *
 * Stdout/stderr are intentionally empty — PostToolUse is observation-only and
 * never blocks the host's tool pipeline.
 */

const { randomUUID } = require("node:crypto");
const { existsSync } = require("node:fs");
const { isAbsolute, join, relative } = require("node:path");

// W1-01 (ISS-011) parity: route every shared-ledger append through the
// advisory-lock primitive so concurrent PostToolUse fires (multi-window /
// parallel edits) never interleave a partial line. ux-w2-9: route batched event
// writes through the single guarded event-writer (envelope stamp + event_type
// guard + advisory-lock append) so every row satisfies the event-ledger schema.
const { appendEvents } = require("./lib/event-writer.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

const FABRIC_DIR_REL = ".fabric";
const EVENTS_LEDGER_FILE = "events.jsonl";

// Tool names that trigger the mutation marker. PostToolUse fires on many tool
// names across clients; we only react to the file-edit tools (matches the
// PreToolUse narrow hint's EDIT_TOOL_NAMES so Pre/Post pair on the same set).
const EDIT_TOOL_NAMES = new Set(["Edit", "Write", "MultiEdit", "apply_patch"]);

// KT-DEC-0030: tool names that read a file body. After retrieval collapsed to
// one lean tool (KT-DEC-0026), the agent consumes a knowledge body via a NATIVE
// Read of the store file — so a Read landing on a `<store>/knowledge/<type>/
// <ID>--*.md` path is the observable "body opened" signal. Only `Read` is in
// scope (Edit/Write already covered by the mutation marker above).
const READ_TOOL_NAMES = new Set(["Read"]);

// Matches a Fabric knowledge file path and captures the stable_id from the
// basename. The id grammar mirrors KT-DEC-0004 (`K[PT]-(DEC|MOD|GLD|PIT|PRO)-NNNN`).
// The path MUST sit under `/knowledge/` and end at a `<type>/<ID>--*.md` file.
// The intermediate segment group is `(?:[^\\/]+[\\/])+` (one-or-more), NOT a
// single segment: project-scoped bodies live under a deeper
// `/knowledge/projects/<project-id>/<type>/` prefix (3 segments), and pinning
// this to exactly one segment silently dropped every project-scoped
// knowledge_body_read (88% of entries in a project-heavy store).
const KNOWLEDGE_BODY_PATH_RE =
  /[\\/]knowledge[\\/](?:[^\\/]+[\\/])+(K[PT]-(?:DEC|MOD|GLD|PIT|PRO)-\d{3,})--[^\\/]*\.md$/;

// Captures the store alias from a multistore path (`.../stores/<alias>/...`).
// Absent for legacy dual-root layouts → store stays undefined (still a valid event).
const STORE_ALIAS_RE = /[\\/]stores[\\/]([^\\/]+)[\\/]/;

/**
 * Read stdin (or a test-supplied raw string) as JSON. Returns null on any
 * parse failure — the hook stays silent rather than crashing the tool pipeline.
 */
function readPayload(rawStdin) {
  if (typeof rawStdin !== "string" || rawStdin.length === 0) return null;
  try {
    const parsed = JSON.parse(rawStdin);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

/**
 * Extract the tool name. Mirrors the narrow hint's probe:
 *   - Claude Code / Codex: { tool_name, ... }
 */
function extractToolName(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (typeof payload.tool_name === "string") return payload.tool_name;
  if (typeof payload.tool === "string") return payload.tool;
  return null;
}

/**
 * Extract the tool_input object from the `tool_input`
 * (Claude/Codex) convention.
 */
function extractToolInput(payload) {
  if (!payload || typeof payload !== "object") return null;
  if (payload.tool_input && typeof payload.tool_input === "object") {
    return payload.tool_input;
  }
  if (payload.input && typeof payload.input === "object") {
    return payload.input;
  }
  return null;
}

/**
 * Pull file paths out of a tool_input object. Same three shapes the narrow
 * hint handles (single file_path / array file_paths / MultiEdit edits[]).
 * Returns a deduped array of strings — empty when none recognizable.
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
      if (
        edit &&
        typeof edit === "object" &&
        typeof edit.file_path === "string" &&
        edit.file_path.length > 0
      ) {
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

/**
 * Extract the per-call id. Claude Code's PostToolUse payload carries the id of
 * the originating tool_use block as `tool_use_id`; older drafts / other clients
 * variously used `tool_call_id` / `call_id` / `id`. We probe in that order.
 *
 * Returns null when none is present — the caller then synthesizes a best-effort
 * fallback key so the marker still lands (per W2-T3: "缺失则用 best-effort
 * fallback key 但仍 append"). A fallback key cannot pair with the Pre half but
 * still records the mutation, which is strictly better than dropping it.
 */
function extractToolCallId(payload) {
  if (!payload || typeof payload !== "object") return null;
  const candidates = ["tool_use_id", "tool_call_id", "call_id", "id"];
  for (const key of candidates) {
    const v = payload[key];
    if (typeof v === "string" && v.length > 0) return v;
  }
  return null;
}

/**
 * Normalize a path to a project-relative, forward-slash form. Drops paths that
 * escape the project tree (mirrors appendEditIntentToLedger in the narrow
 * hook). Returns null for out-of-tree / empty.
 */
function normalizePath(projectRoot, p) {
  if (typeof p !== "string" || p.length === 0) return null;
  let rel;
  if (isAbsolute(p)) {
    rel = relative(projectRoot, p);
    if (rel.startsWith("..")) return null;
  } else {
    if (p.startsWith("..")) return null;
    rel = p;
  }
  const slashed = rel.split(/[\\/]/).join("/");
  return slashed.length > 0 ? slashed : null;
}

/**
 * Append one `file_mutated` marker per edited path to `.fabric/events.jsonl`.
 * Best-effort:
 *   - Skips silently when `.fabric/` does not exist (project not init'd).
 *   - Skips silently when there are no in-tree paths.
 *   - ANY error (append, JSON throw) is swallowed — never blocks the pipeline.
 *
 * One JSON line per path (PIPE_BUF-atomic). The per-path lines share a single
 * tool_call_id so doctor can group them as one tool call.
 */
function appendFileMutated(projectRoot, now, paths, toolCallId, toolName, sessionId) {
  try {
    const fabricDir = join(projectRoot, FABRIC_DIR_REL);
    if (!existsSync(fabricDir)) return;
    const pathList = Array.isArray(paths)
      ? paths.map((p) => normalizePath(projectRoot, p)).filter((p) => p !== null)
      : [];
    if (pathList.length === 0) return;
    const tsMs = now instanceof Date ? now.getTime() : Number(now);
    // Best-effort fallback key when the client omits the tool-call id: the
    // mutation is still recorded (can't pair with the Pre half, but the path
    // signal is preserved). Per-fire UUID keeps parallel fires distinct.
    const callId =
      typeof toolCallId === "string" && toolCallId.length > 0
        ? toolCallId
        : `fallback:${randomUUID()}`;
    const validSessionId =
      typeof sessionId === "string" && sessionId.length > 0 ? sessionId : null;
    const validToolName =
      typeof toolName === "string" && toolName.length > 0 ? toolName : null;
    appendEvents(
      fabricDir,
      pathList.map((p) => ({
        ts: tsMs,
        ...(validSessionId ? { session_id: validSessionId } : {}),
        event_type: "file_mutated",
        path: p,
        tool_call_id: callId,
        ...(validToolName ? { tool_name: validToolName } : {}),
      })),
    );
  } catch {
    // Silent — marker failure must never block the tool pipeline.
  }
}

/**
 * KT-DEC-0030: parse a Read path into a knowledge-body-read descriptor. Returns
 * `{ stable_id, store, path }` when the path is a Fabric knowledge file, else
 * null. `store` is omitted when no `stores/<alias>/` segment is present (legacy
 * dual-root layout). `path` is forward-slash-normalized but NOT made
 * project-relative — knowledge bodies live under ~/.fabric, outside the project
 * tree, so the home-anchored path is the meaningful identifier.
 */
function extractKnowledgeBodyRead(filePath) {
  if (typeof filePath !== "string" || filePath.length === 0) return null;
  const idMatch = KNOWLEDGE_BODY_PATH_RE.exec(filePath);
  if (idMatch === null) return null;
  const storeMatch = STORE_ALIAS_RE.exec(filePath);
  const slashed = filePath.split(/[\\/]/).join("/");
  // ISS-20260713-024: prefer store-relative path (drop home prefix) to avoid
  // leaking OS username in events.jsonl when ledgers are shared.
  let pathOut = slashed;
  const storesIdx = slashed.indexOf("/stores/");
  if (storesIdx !== -1) {
    pathOut = slashed.slice(storesIdx + 1); // stores/<alias>/knowledge/...
  } else {
    const home = (process.env.HOME || process.env.USERPROFILE || "").split(/[\\/]/).join("/");
    if (home && slashed.startsWith(home + "/")) {
      pathOut = "~/" + slashed.slice(home.length + 1);
    }
  }
  return {
    stable_id: idMatch[1],
    store: storeMatch !== null ? storeMatch[1] : null,
    path: pathOut,
  };
}

/**
 * Append one `knowledge_body_read` marker per Fabric knowledge file read.
 * Best-effort, identical guarantees to appendFileMutated (silent on any error,
 * skips when `.fabric/` absent). Non-knowledge reads produce zero events — the
 * common case (the agent reads source files far more than knowledge bodies),
 * so the hook stays a near-noop on the hot Read path.
 */
function appendKnowledgeBodyRead(projectRoot, now, paths, toolCallId, toolName, sessionId) {
  try {
    const fabricDir = join(projectRoot, FABRIC_DIR_REL);
    if (!existsSync(fabricDir)) return;
    const reads = Array.isArray(paths)
      ? paths.map((p) => extractKnowledgeBodyRead(p)).filter((r) => r !== null)
      : [];
    if (reads.length === 0) return;
    const tsMs = now instanceof Date ? now.getTime() : Number(now);
    const callId =
      typeof toolCallId === "string" && toolCallId.length > 0
        ? toolCallId
        : `fallback:${randomUUID()}`;
    const validSessionId =
      typeof sessionId === "string" && sessionId.length > 0 ? sessionId : null;
    const validToolName =
      typeof toolName === "string" && toolName.length > 0 ? toolName : null;
    appendEvents(
      fabricDir,
      reads.map((r) => ({
        ts: tsMs,
        ...(validSessionId ? { session_id: validSessionId } : {}),
        event_type: "knowledge_body_read",
        stable_id: r.stable_id,
        ...(r.store ? { store: r.store } : {}),
        path: r.path,
        tool_call_id: callId,
        ...(validToolName ? { tool_name: validToolName } : {}),
      })),
    );
  } catch {
    // Silent — marker failure must never block the tool pipeline.
  }
}

// -----------------------------------------------------------------------------
// Main — invoked as a CLI (require.main === module) and in-process by tests.
// Wraps the entire flow in try/catch: ANY error → silent exit 0.
// -----------------------------------------------------------------------------

function main(env) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const now = (env && env.now) || new Date();
    const payload =
      env && env.payload !== undefined ? env.payload : readPayload(env && env.stdin);
    if (payload === null || payload === undefined) return;

    const toolName = extractToolName(payload);
    const isEdit = toolName && EDIT_TOOL_NAMES.has(toolName);
    const isRead = toolName && READ_TOOL_NAMES.has(toolName);
    if (!isEdit && !isRead) return;

    const toolInput = extractToolInput(payload);
    const paths = extractPaths(toolInput);
    if (paths.length === 0) return;

    const toolCallId = extractToolCallId(payload);
    const sessionId =
      payload && typeof payload === "object" && typeof payload.session_id === "string"
        ? payload.session_id
        : null;

    // Keep MCP fab_recall session_id fallback fresh across the session.
    if (typeof sessionId === "string" && sessionId.length > 0) {
      try {
        const { writeActiveSession } = require("./lib/state-store.cjs");
        const tsMs = now instanceof Date ? now.getTime() : Number(now) || Date.now();
        writeActiveSession(cwd, sessionId, tsMs);
      } catch {
        // best-effort — never block PostToolUse
      }
    }

    if (isEdit) {
      appendFileMutated(cwd, now, paths, toolCallId, toolName, sessionId);
    } else {
      // KT-DEC-0030: observe native Reads of store knowledge bodies. Non-
      // knowledge Reads filter out inside appendKnowledgeBodyRead (zero events).
      appendKnowledgeBodyRead(cwd, now, paths, toolCallId, toolName, sessionId);
    }
  } catch {
    // Silent — never block the tool pipeline on hook failure.
  }
}

module.exports = {
  main,
  readPayload,
  extractToolName,
  extractToolInput,
  extractPaths,
  extractToolCallId,
  normalizePath,
  appendFileMutated,
  extractKnowledgeBodyRead,
  appendKnowledgeBodyRead,
  CONSTANTS: {
    FABRIC_DIR_REL,
    EVENTS_LEDGER_FILE,
    EDIT_TOOL_NAMES,
    READ_TOOL_NAMES,
  },
};

if (require.main === module) {
  // Read stdin synchronously (small hook payloads, no concurrency concerns).
  let stdinRaw = "";
  try {
    stdinRaw = require("node:fs").readFileSync(0, "utf8");
  } catch {
    // No stdin — proceed with empty payload (E*: no append without paths).
  }
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot, now: new Date(), stdin: stdinRaw });
  process.exit(0);
}
