// ISS-20260713-040 residual: Stop-hook stdin payload + session digest write.
// Extracted from fabric-hint.cjs.
const { readFileSync } = require("node:fs");
const { summarizeTranscript } = require("./transcript-summary.cjs");

let sessionDigestWriter = null;
try {
  sessionDigestWriter = require("./session-digest-writer.cjs");
} catch {
  sessionDigestWriter = null;
}

/**
 * Resolve session_id from Stop stdin payload, then FABRIC_SESSION_ID env.
 * Returns null when both missing (cadence/status fail open).
 */
function resolveHookSessionId(payload, env) {
  if (payload && typeof payload.session_id === "string" && payload.session_id.length > 0) {
    return payload.session_id;
  }
  const envBag = (env && env.processEnv) || process.env;
  if (envBag && typeof envBag.FABRIC_SESSION_ID === "string" && envBag.FABRIC_SESSION_ID.length > 0) {
    return envBag.FABRIC_SESSION_ID;
  }
  return null;
}

/**
 * Best-effort sync stdin reader for the Stop hook JSON payload.
 * Returns parsed object or null. NEVER throws.
 */
function tryReadStdinJson() {
  try {
    if (process.stdin.isTTY === true) return null;
    const buf = readFileSync(0, "utf8");
    if (typeof buf !== "string" || buf.trim().length === 0) return null;
    const parsed = JSON.parse(buf);
    if (parsed === null || typeof parsed !== "object") return null;
    return parsed;
  } catch (e) {
    try {
      const message =
        e && typeof e === "object" && "message" in e ? String(e.message) : String(e);
      process.stderr.write(`[fabric-hint] malformed input: ${message}\n`);
    } catch {
      // stderr write failed
    }
    return null;
  }
}

/**
 * Non-blocking session digest fan-out. Failure is silently swallowed.
 */
function writeSessionDigestBestEffort(projectRoot, stdinPayload) {
  if (sessionDigestWriter === null) return;
  if (stdinPayload === null) return;
  try {
    const sessionId = stdinPayload.session_id;
    if (typeof sessionId !== "string" || sessionId.length === 0) return;
    const transcript = summarizeTranscript(stdinPayload.transcript_path);
    sessionDigestWriter.writeDigest({
      projectRoot,
      session_id: sessionId,
      title: transcript.title,
      user_messages: transcript.user_messages,
      edit_paths: transcript.edit_paths,
    });
  } catch {
    // Best-effort. Stop hook continues.
  }
}

module.exports = {
  resolveHookSessionId,
  tryReadStdinJson,
  writeSessionDigestBestEffort,
};
