/**
 * v2.0.0-rc.37 NEW-30: shared client-protocol adapter for hook scripts.
 *
 * The two host clients (Claude Code / Codex CLI) differ in how a
 * hook surfaces context back to the model:
 *   - Claude Code reads a stdout JSON envelope
 *     ({ hookSpecificOutput: { hookEventName, additionalContext } }).
 *   - Codex CLI reads plain stderr text.
 * Each hook had its own copy of the detect-client + read-stdin + pick-channel
 * logic (fabric-hint.detectClient, cite-policy-evict.isClaudeCode + readStdinJson,
 * knowledge-hint-broad inline CLAUDE_PROJECT_DIR check). This module is the
 * single canonical implementation so the protocol choice lives in one place.
 *
 * Provides:
 *   - detectClient(dirnameHint?) → 'cc' | 'codex' | undefined
 *       3-tier: FABRIC_HINT_CLIENT env override → CLAUDE_PROJECT_DIR (cc) →
 *       __dirname path heuristic (.claude / .codex). dirnameHint
 *       defaults to this lib's own dir (which still lives under the client
 *       dir, e.g. .claude/hooks/lib), so the heuristic stays accurate.
 *   - isClaudeCode() → boolean   (CLAUDE_PROJECT_DIR present)
 *   - readStdinJson({ timeoutMs }) → Promise<object | null>
 *       Async stdin JSON reader; null on parse error / closed stdin / timeout.
 *   - emitContext(text, { client, eventName, streams, forceStderr }) → void
 *       Standardised output: Claude Code → stdout JSON envelope; Codex
 *       → plain stderr. forceStderr pins stderr even on Claude Code (used for
 *       SessionStart one-shot reminders). Best-effort — never throws.
 *
 * Never-throw contract (KT-DEC-0007): every path degrades silently rather than
 * blocking the host's main flow.
 */

function isClaudeCode() {
  return (
    typeof process.env.CLAUDE_PROJECT_DIR === "string" &&
    process.env.CLAUDE_PROJECT_DIR.length > 0
  );
}

function detectClient(dirnameHint) {
  const envClient = process.env.FABRIC_HINT_CLIENT;
  if (typeof envClient === "string" && envClient.length > 0) {
    const normalised = envClient.trim().toLowerCase();
    if (normalised === "cc" || normalised === "codex") {
      return normalised;
    }
  }
  if (isClaudeCode()) return "cc";
  // Path heuristic against the caller's directory (defaults to this lib's dir,
  // which sits under the client root, e.g. .codex/hooks/lib).
  const dir = typeof dirnameHint === "string" && dirnameHint.length > 0 ? dirnameHint : __dirname;
  try {
    if (dir.includes(".claude/") || dir.includes(".claude\\")) return "cc";
    if (dir.includes(".codex/") || dir.includes(".codex\\")) return "codex";
  } catch {
    // fall through
  }
  return undefined;
}

function readStdinJson(opts) {
  const { timeoutMs = 1000 } = opts || {};
  return new Promise((resolve) => {
    let buffer = "";
    process.stdin.on("data", (chunk) => {
      buffer += chunk;
    });
    process.stdin.on("end", () => {
      try {
        resolve(JSON.parse(buffer));
      } catch {
        resolve(null);
      }
    });
    process.stdin.on("error", () => resolve(null));
    // Defensive timeout: if stdin never closes (host bug), give up.
    setTimeout(() => resolve(null), timeoutMs).unref();
  });
}

function emitContext(text, opts) {
  const { client, eventName = "UserPromptSubmit", streams = {}, forceStderr = false } = opts || {};
  const stdout = streams.stdout || process.stdout;
  const stderr = streams.stderr || process.stderr;
  const useStdoutEnvelope =
    !forceStderr && (client === "cc" || (client === undefined && isClaudeCode()));
  try {
    if (useStdoutEnvelope) {
      const envelope = {
        hookSpecificOutput: { hookEventName: eventName, additionalContext: text },
      };
      stdout.write(`${JSON.stringify(envelope)}\n`);
    } else {
      stderr.write(`${text}\n`);
    }
  } catch {
    // best-effort — never throw
  }
}

// v2.2 dual-sink (Goal A / D7): two-channel emit. Unlike emitContext (which
// picks ONE channel), emitDualSink surfaces a knowledge breadcrumb to BOTH the
// human and the AI in one render, split into two fields, with the protocol
// shaped per client:
//
//   cc / codex (symmetric, D7): a single stdout JSON envelope carrying
//     { systemMessage: <human>,                          // the human sink
//       hookSpecificOutput: { hookEventName, additionalContext: <ai> } }  // AI sink
//     camelCase + nested. `systemMessage` is the universal human-facing field
//     (verified against official hook docs in the mode④ design session); it is
//     what fixes the "stderr human channel is dead on CC" gap — CC suppresses
//     hook stderr at exit 0, so the human never saw the old breadcrumb.
//
//   unknown client (detection failed, not CC): fall back to a plain stderr
//     breadcrumb (human preferred, else ai) — no known JSON contract to target.
//
// Either field may be null/empty: pass { human, ai } and only the present
// channels are written (e.g. a PreToolUse miss passes human:null → AI-only;
// nudge_mode silent passes human:null too). The AI field is ALWAYS the caller's
// to decide independently — this fn never derives one channel from the other,
// preserving the flow ⊥ observation invariant (D5).
//
// Never-throw contract (KT-DEC-0007): every path degrades silently.
function emitDualSink(payload, opts) {
  const { human = null, ai = null } = payload || {};
  const { client, eventName = "SessionStart", streams = {} } = opts || {};
  const stdout = streams.stdout || process.stdout;
  const stderr = streams.stderr || process.stderr;
  const hasHuman = typeof human === "string" && human.length > 0;
  const hasAi = typeof ai === "string" && ai.length > 0;
  const resolved = client || detectClient();
  try {
    const useEnvelope =
      resolved === "cc" ||
      resolved === "codex" ||
      (resolved === undefined && isClaudeCode());
    if (useEnvelope) {
      const envelope = {};
      if (hasHuman) envelope.systemMessage = human;
      if (hasAi) {
        envelope.hookSpecificOutput = {
          hookEventName: eventName,
          additionalContext: ai,
        };
      }
      if (Object.keys(envelope).length > 0) {
        stdout.write(`${JSON.stringify(envelope)}\n`);
      }
      return;
    }
    // Unknown client: no JSON contract — surface the human breadcrumb (or ai)
    // on stderr as a last resort so something is visible.
    const fallback = hasHuman ? human : hasAi ? ai : null;
    if (fallback !== null) stderr.write(`${fallback}\n`);
  } catch {
    // best-effort — never throw
  }
}

module.exports = {
  isClaudeCode,
  detectClient,
  readStdinJson,
  emitContext,
  emitDualSink,
};
