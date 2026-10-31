#!/usr/bin/env node
// ux-w2-6: the SINGLE PreToolUse hook. Previously the Edit|Write|MultiEdit
// matcher carried TWO commands — knowledge-hint-narrow.cjs (narrow KB hint) and
// cite-policy-evict.cjs (recall-before-edit nudge) — so a single edit produced
// TWO additionalContext envelopes (双弹). This orchestrator runs both in ONE
// process and merges their output into ONE envelope.
//
// narrow.cjs and cite-policy-evict.cjs stay as standalone modules (their full
// contract test-suites are unchanged); this entry imports them as libs, reads
// stdin ONCE (stdin is single-read), hands the parsed payload to each via the
// `env.payload` test seam, captures each one's stdout, and folds the two
// envelopes into a single `{ systemMessage?, hookSpecificOutput.additionalContext? }`.
// Each sub-hook stays best-effort/silent-on-failure, so a throw in one never
// blocks the edit or suppresses the other (KT-DEC-0007).

// W4 I2: narrow lives under lib/ because it is a lib — no client registers it
// as a hook entry point; this file is its only caller. cite-policy-evict stays
// at the top level because Codex DOES register it (.codex/hooks.json), i.e. it
// is a real, client-asymmetric entry point rather than a misfiled helper.
const narrow = require("./lib/knowledge-hint-narrow.cjs");
const cite = require("./cite-policy-evict.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

function readStdinPayload() {
  try {
    const raw = require("node:fs").readFileSync(0, "utf8");
    if (!raw || raw.trim().length === 0) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

// Parse a captured stdout chunk as a Claude Code hook envelope. Returns null for
// empty / non-JSON output (e.g. a stderr-only codex breadcrumb leaves stdout
// empty). Tolerant: a malformed line is ignored, never thrown.
function parseEnvelope(text) {
  const trimmed = typeof text === "string" ? text.trim() : "";
  if (trimmed.length === 0) return null;
  try {
    return JSON.parse(trimmed);
  } catch {
    return null;
  }
}

// Fold narrow + cite envelopes into one. additionalContext (AI sink) is
// concatenated (narrow hint first, then the cite nudge); systemMessage (human
// sink) likewise. Either side may be absent.
function mergeEnvelopes(narrowText, citeText) {
  const a = parseEnvelope(narrowText);
  const b = parseEnvelope(citeText);
  if (a === null && b === null) return null;

  const aiParts = [];
  const humanParts = [];
  let eventName = "PreToolUse";
  for (const env of [a, b]) {
    if (env === null) continue;
    const hso = env.hookSpecificOutput;
    if (hso && typeof hso.additionalContext === "string" && hso.additionalContext.length > 0) {
      aiParts.push(hso.additionalContext);
      if (typeof hso.hookEventName === "string") eventName = hso.hookEventName;
    }
    if (typeof env.systemMessage === "string" && env.systemMessage.length > 0) {
      humanParts.push(env.systemMessage);
    }
  }

  const merged = {};
  if (humanParts.length > 0) merged.systemMessage = humanParts.join("\n");
  if (aiParts.length > 0) {
    merged.hookSpecificOutput = {
      hookEventName: eventName,
      additionalContext: aiParts.join("\n"),
    };
  }
  return Object.keys(merged).length > 0 ? `${JSON.stringify(merged)}\n` : null;
}

async function main(env, stdio) {
  try {
    const out = (stdio && stdio.stdout) || process.stdout;
    const err = (stdio && stdio.stderr) || process.stderr;
    // Read stdin ONCE and share the parsed payload with both sub-hooks (stdin is
    // a single-read stream — the prior two-command wiring read it twice).
    const payload = env && env.payload !== undefined ? env.payload : readStdinPayload();
    const sub = { ...(env || {}), payload };

    const narrowChunks = [];
    const citeChunks = [];
    const capture = (sink) => ({ write: (c) => sink.push(String(c)) });

    try {
      await narrow.main(sub, { stdout: capture(narrowChunks), stderr: err });
    } catch {
      // narrow best-effort — never block the edit
    }
    try {
      await cite.main(sub, { stdout: capture(citeChunks), stderr: err });
    } catch {
      // cite best-effort — never block the edit
    }

    const merged = mergeEnvelopes(narrowChunks.join(""), citeChunks.join(""));
    if (merged !== null) out.write(merged);
  } catch {
    // Silent — the PreToolUse hook MUST NEVER block the edit on its own failure.
  }
}

module.exports = { main, mergeEnvelopes, parseEnvelope };

if (require.main === module) {
  // Inject the resolved project root so the narrow + cite sub-hooks write
  // their .fabric ledgers to the repo root, not the session's subdirectory.
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot });
}
