// ISS-20260713-020: transcript summarization for session digests / cite extraction.
// ISS-20260713-041: path sandbox — only read under known client transcript roots.
// ISS-20260713-044: refuse filesystem root `/` as allowlisted root (fail-closed).
// ISS-20260713-045: always read via validated realpath (no TOCTOU original-path read).

const { existsSync, readFileSync, realpathSync, statSync } = require("node:fs");
const { isAbsolute, join, resolve, relative, dirname, basename } = require("node:path");
const { homedir } = require("node:os");

let citeLineParser = null;
try {
  citeLineParser = require("./cite-line-parser.cjs");
} catch {
  citeLineParser = null;
}

/** Default max transcript size before fail-closed empty summary (ISS-041). */
const MAX_TRANSCRIPT_BYTES = 8 * 1024 * 1024;

/**
 * ISS-044: filesystem root is never a valid transcript allowlist entry — it would
 * make every absolute path "under" the root and fully disable the sandbox.
 * @param {string} root
 * @returns {boolean}
 */
function isFilesystemRoot(root) {
  if (typeof root !== "string" || root.length === 0) return true;
  // POSIX root, or Windows drive root like `C:\` / `C:/`.
  if (root === "/" || root === "\\") return true;
  if (/^[A-Za-z]:[\\/]?$/.test(root)) return true;
  return false;
}

/**
 * Resolve + normalize a candidate root. Returns null when unusable (incl. `/`).
 * @param {string} p
 * @returns {string|null}
 */
function normalizeTranscriptRoot(p) {
  if (typeof p !== "string" || p.trim().length === 0) return null;
  let resolved;
  try {
    resolved = existsSync(p) ? realpathSync(p) : resolve(p);
  } catch {
    try {
      resolved = resolve(p);
    } catch {
      return null;
    }
  }
  if (isFilesystemRoot(resolved)) return null;
  return resolved;
}

/**
 * Known client transcript roots (absolute, realpath'd when possible).
 * Claude Code: ~/.claude/projects/**
 * Codex: ~/.codex/**
 *
 * Test seam: FABRIC_TRANSCRIPT_ROOTS=comma-separated absolute paths.
 * ISS-044: env override is honored ONLY when NODE_ENV=test or FABRIC_TEST=1 —
 * production never trusts this env (otherwise a planted ROOTS=/ or extra root
 * would widen the Stop-hook read surface). When the seam is active, any entry
 * that resolves to filesystem root `/` is dropped; empty after filter → defaults
 * (fail-closed: never open the whole filesystem).
 */
function isTranscriptTestSeamEnabled() {
  return process.env.NODE_ENV === "test" || process.env.FABRIC_TEST === "1";
}

function getAllowedTranscriptRoots() {
  const home = homedir();
  const defaults = [join(home, ".claude", "projects"), join(home, ".codex")]
    .map((p) => normalizeTranscriptRoot(p))
    .filter((p) => p !== null);

  if (!isTranscriptTestSeamEnabled()) return defaults;

  const env = process.env.FABRIC_TRANSCRIPT_ROOTS;
  if (typeof env === "string" && env.trim().length > 0) {
    const fromEnv = env
      .split(",")
      .map((s) => s.trim())
      .filter((s) => s.length > 0)
      .map((p) => normalizeTranscriptRoot(p))
      .filter((p) => p !== null);
    // Fail-closed: empty after root-filter → defaults, not "allow everything".
    if (fromEnv.length > 0) return fromEnv;
  }
  return defaults;
}

/**
 * Resolve path to a realpath (or parent realpath + remaining basename segments)
 * for prefix checks. Returns null when the path cannot be resolved safely.
 * @param {string} transcriptPath
 * @returns {string|null}
 */
function resolveTranscriptRealpath(transcriptPath) {
  try {
    if (existsSync(transcriptPath)) {
      return realpathSync(transcriptPath);
    }
    // Missing file: walk up to an existing parent, realpath it, rejoin suffix.
    let cur = transcriptPath;
    const suffix = [];
    for (let i = 0; i < 64; i++) {
      if (existsSync(cur)) {
        const real = realpathSync(cur);
        return suffix.length === 0 ? real : resolve(real, ...suffix);
      }
      const base = basename(cur);
      const parent = dirname(cur);
      if (parent === cur) break;
      suffix.unshift(base);
      cur = parent;
    }
    // Nothing on disk — still allow pure absolute path under roots via resolve.
    return resolve(transcriptPath);
  } catch {
    return null;
  }
}

/**
 * True when `real` is under a non-root allowlisted prefix (ISS-041 + ISS-044).
 * @param {string} real
 * @param {string[]} roots
 * @returns {boolean}
 */
function isUnderAllowedRoots(real, roots) {
  for (const root of roots) {
    if (isFilesystemRoot(root)) continue; // defense-in-depth
    const rel = relative(root, real);
    // under root when relative is empty (exact) or non-escaping non-absolute
    if (rel === "" || (!rel.startsWith("..") && !isAbsolute(rel))) {
      return true;
    }
  }
  return false;
}

/**
 * ISS-041/044/045 sandbox gate: absolute path, .jsonl suffix, realpath under
 * allowlisted non-root roots, size ≤ MAX_TRANSCRIPT_BYTES.
 * Returns the validated realpath to READ, or null when denied (fail-closed).
 * Callers MUST read this returned path — never the unsanitized original.
 * @param {string} transcriptPath
 * @returns {string|null}
 */
function resolveAllowedTranscriptPath(transcriptPath) {
  if (typeof transcriptPath !== "string" || transcriptPath.length === 0) return null;
  if (!isAbsolute(transcriptPath)) return null;
  // Require .jsonl suffix (case-sensitive match on lowercased path end).
  if (!transcriptPath.toLowerCase().endsWith(".jsonl")) return null;

  const real = resolveTranscriptRealpath(transcriptPath);
  if (!real) return null;
  // Resolved path must itself still look like a transcript target.
  if (!isAbsolute(real)) return null;
  if (!real.toLowerCase().endsWith(".jsonl")) return null;

  const roots = getAllowedTranscriptRoots();
  if (roots.length === 0) return null;
  if (!isUnderAllowedRoots(real, roots)) return null;

  // Size gate only when the file exists; missing file is handled by existsSync later
  // (still "allowed" path so empty summary is from ENOENT, not sandbox).
  try {
    if (existsSync(real)) {
      const st = statSync(real);
      if (!st.isFile()) return null;
      if (st.size > MAX_TRANSCRIPT_BYTES) return null;
    }
  } catch {
    return null;
  }
  return real;
}

/**
 * ISS-041 sandbox gate (boolean facade). Prefer resolveAllowedTranscriptPath
 * when the caller will open the file (ISS-045).
 * @param {string} transcriptPath
 * @returns {boolean}
 */
function isAllowedTranscriptPath(transcriptPath) {
  return resolveAllowedTranscriptPath(transcriptPath) !== null;
}

/**
 * v2.0.0-rc.7 T5: extract user_messages + edit_paths + 1-line title from the
 * transcript JSONL referenced by the hook's stdin payload. Best-effort, never
 * throws.
 *
 * Claude Code's transcript_path points at a JSONL where each line is a
 * message envelope. We sniff for `role: "user"` lines (text content) and
 * for tool-use entries naming Edit / Write / MultiEdit to harvest file_path.
 *
 * v2.0.0-rc.20 TASK-03: additionally collects `assistant_turns[]` — one
 * entry per assistant envelope with the parsed KB-line cite metadata. Field
 * is additive; existing callers (writeSessionDigestBestEffort) ignore it.
 *
 * ISS-20260713-041: path must be absolute under known client transcript roots
 * (or FABRIC_TRANSCRIPT_ROOTS), end with .jsonl, and stay within size cap.
 * Violations return empty summary without throw (fail-closed).
 */
function summarizeTranscript(transcriptPath) {
  // rc.20 TASK-03: additive `assistant_turns` array — one entry per assistant
  // envelope, regardless of whether the first line matched KB:. Downstream
  // consumers (extractAndWriteAssistantTurnsBestEffort) emit one
  // assistant_turn_observed event per element; `kb_line_raw=null` when no
  // KB: line was found.
  const out = { user_messages: [], edit_paths: [], title: "", assistant_turns: [] };
  if (typeof transcriptPath !== "string" || transcriptPath.length === 0) return out;
  // ISS-041/044/045: allow-check returns the validated realpath; always read THAT
  // path (never the unsanitized original) so a TOCTOU swap / symlink diverge
  // cannot open a different file than the one prefix-checked.
  const allowedReal = resolveAllowedTranscriptPath(transcriptPath);
  if (!allowedReal) return out;
  if (!existsSync(allowedReal)) return out;
  let raw;
  try {
    raw = readFileSync(allowedReal, "utf8");
  } catch {
    return out;
  }
  const lines = raw.split(/\r?\n/);
  let envelopeIndex = -1;
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.length === 0) continue;
    let envelope;
    try {
      envelope = JSON.parse(trimmed);
    } catch {
      continue;
    }
    if (envelope === null || typeof envelope !== "object") continue;
    envelopeIndex += 1;

    // v2.0.0-rc.27 TASK-009 (audit §2.16): Codex CLI uses a different
    // envelope shape — { type:"response_item", payload:{ type:"message",
    // role, content:[{type:"input_text"|"output_text", text}] } } — vs Claude
    // Code's { type:"user", message:{ role, content } }. Resolve role +
    // content from whichever shape is present; without this, every Codex
    // session's digest came out empty (audit §2.16 — fixed here).
    const role =
      envelope.role ||
      (envelope.message && envelope.message.role) ||
      (envelope.payload && envelope.payload.role);
    if (role === "user") {
      const content =
        envelope.content ||
        (envelope.message && envelope.message.content) ||
        (envelope.payload && envelope.payload.content);
      if (typeof content === "string") {
        out.user_messages.push(content);
      } else if (Array.isArray(content)) {
        for (const block of content) {
          if (block && typeof block === "object" && typeof block.text === "string") {
            out.user_messages.push(block.text);
          }
        }
      }
    }

    // rc.20 TASK-03: assistant envelope — capture first non-empty line of the
    // first text block and parse for `KB:` prefix. We push ONE assistant_turns
    // entry per assistant envelope (even when no KB: line) so downstream can
    // distinguish "turn observed, no KB" (kb_line_raw=null) from "no turn".
    if (role === "assistant") {
      const content =
        envelope.content ||
        (envelope.message && envelope.message.content) ||
        (envelope.payload && envelope.payload.content);
      let firstText = null;
      if (typeof content === "string") {
        firstText = content;
      } else if (Array.isArray(content)) {
        for (const block of content) {
          if (block && typeof block === "object" && block.type === "text" && typeof block.text === "string") {
            firstText = block.text;
            break;
          }
        }
      }
      let kbLineRaw = null;
      let citeIds = [];
      let citeTags = [];
      // rc.24 TASK-04: parallel `cite_commitments` array, populated by the
      // shared cite-line parser. One entry per non-sentinel cite (index-aligned
      // with cite_ids). Sentinel `KB: none` contributes a `cite_tags=["none"]`
      // entry but no commitment — matches the parseCiteLine index contract.
      let citeCommitments = [];
      // v2.0.0-rc.27 TASK-009: Codex assistant blocks carry text under
      // `type:"output_text"` (not `type:"text"`). Fall back when no text-typed
      // block matched but a typed output_text block exists.
      if (firstText === null && Array.isArray(content)) {
        for (const block of content) {
          if (block && typeof block === "object" && block.type === "output_text" && typeof block.text === "string") {
            firstText = block.text;
            break;
          }
        }
      }
      if (typeof firstText === "string" && firstText.length > 0) {
        // Leading contiguous `KB:` lines (applied + dismissed in one reply).
        // parseCiteLine already supports multi-line input; previously only the
        // first non-empty line was considered, so a second `KB: … [dismissed]`
        // was dropped (ccpm dogfood 2026-07-12). Still require the FIRST
        // non-empty line to be a `KB:` line so prose-only turns stay empty.
        const linesOfText = firstText.split(/\r?\n/);
        const kbBlockLines = [];
        for (const l of linesOfText) {
          const trimmed = l.trim();
          if (trimmed.length === 0) {
            // Allow blank lines only after we already started a KB: prefix block.
            if (kbBlockLines.length > 0) continue;
            continue;
          }
          if (/^KB:\s*/i.test(trimmed)) {
            kbBlockLines.push(trimmed);
            continue;
          }
          // First non-empty non-KB line ends the leading block (or means no cite).
          break;
        }
        if (kbBlockLines.length > 0) {
          // rc.24 TASK-04: route the FULL `KB: ...` block to the shared parser.
          // parseCiteLine handles sentinels (`KB: none [<reason>]`) AND full
          // cite form including contract tail (`KB: KT-DEC-0001 [recalled] →
          // edit:foo.ts`) uniformly. Multi-line applied+dismissed is now kept.
          const kbBlock = kbBlockLines.join("\n");
          kbLineRaw = kbBlock;
          if (citeLineParser && typeof citeLineParser.parseCiteLine === "function") {
            const parsed = citeLineParser.parseCiteLine(kbBlock);
            citeIds = parsed.cite_ids;
            citeTags = parsed.cite_tags;
            citeCommitments = parsed.cite_commitments;
          }
          // Degraded mode (lib missing) → keep kbLineRaw but emit empty
          // arrays; doctor downstream treats this as "turn observed, parse
          // unavailable" without crashing.
        }
      }
      out.assistant_turns.push({
        envelope_index: envelopeIndex,
        kb_line_raw: kbLineRaw,
        cite_ids: citeIds,
        cite_tags: citeTags,
        cite_commitments: citeCommitments,
      });
    }

    // Tool use — look for Edit / Write / MultiEdit and harvest file_path.
    const candidates = [];
    if (envelope.type === "tool_use") candidates.push(envelope);
    const msgContent = envelope.message && envelope.message.content;
    if (Array.isArray(msgContent)) {
      for (const block of msgContent) {
        if (block && block.type === "tool_use") candidates.push(block);
      }
    }
    for (const tu of candidates) {
      const name = tu.name;
      if (name === "Edit" || name === "Write" || name === "MultiEdit") {
        const input = tu.input || tu.parameters || {};
        const fp = input.file_path || input.filePath || input.path;
        if (typeof fp === "string" && fp.length > 0) {
          out.edit_paths.push(fp);
        }
        if (name === "MultiEdit" && Array.isArray(input.edits)) {
          for (const e of input.edits) {
            const f = e && (e.file_path || e.filePath || e.path);
            if (typeof f === "string" && f.length > 0) out.edit_paths.push(f);
          }
        }
      }
    }

    // v2.0.0-rc.27 TASK-009 (audit §2.16): Codex apply_patch path. Codex
    // emits one response_item envelope per file-edit invocation with payload
    // shape { type:"custom_tool_call", name:"apply_patch", input:<patch
    // string> }. The patch body lists target files via `*** Update File:`,
    // `*** Add File:`, `*** Delete File:` directives — harvest those.
    if (
      envelope.type === "response_item" &&
      envelope.payload &&
      envelope.payload.type === "custom_tool_call" &&
      envelope.payload.name === "apply_patch" &&
      typeof envelope.payload.input === "string"
    ) {
      const patchInput = envelope.payload.input;
      const fileDirectiveRe = /^\*\*\*\s+(?:Update|Add|Delete)\s+File:\s+(.+?)\s*$/gm;
      let m;
      while ((m = fileDirectiveRe.exec(patchInput)) !== null) {
        const fp = m[1].trim();
        if (fp.length > 0) out.edit_paths.push(fp);
      }
    }
  }
  // 1-line title = first non-empty user message (trimmed). Falls back to "".
  if (out.user_messages.length > 0) {
    const first = out.user_messages[0].replace(/\s+/g, " ").trim();
    out.title = first.slice(0, 80);
  }
  // Dedup edit_paths preserving order.
  const seen = new Set();
  out.edit_paths = out.edit_paths.filter((p) => {
    if (seen.has(p)) return false;
    seen.add(p);
    return true;
  });
  return out;
}

module.exports = {
  summarizeTranscript,
  isAllowedTranscriptPath,
  resolveAllowedTranscriptPath,
  getAllowedTranscriptRoots,
  MAX_TRANSCRIPT_BYTES,
};
