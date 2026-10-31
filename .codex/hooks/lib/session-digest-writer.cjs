/**
 * v2.0.0-rc.7 T5: session-digest writer.
 *
 * Persist a per-session ~5KB digest under
 * `<projectRoot>/.fabric/.cache/session-digests/<session_id>.md` so the
 * fabric-archive Skill (Phase 0.0) can stitch together cross-session
 * context when it runs after Signal A fires.
 *
 * ISS-20260713-069 privacy posture:
 *   - Digests hold truncated user_messages + edit_paths for archive mining only.
 *   - Credential/PII redaction applied before write (DIGEST_SECRET_RES).
 *   - Soft retention DIGEST_MAX_AGE_MS (30d): every write purges stale digests.
 *   - Operators: delete .fabric/.cache/session-digests/ anytime; not synced via git.
 *
 * Contract (non-blocking, best-effort):
 *   - writeDigest({ projectRoot, session_id, user_messages, edit_paths, title })
 *   - returns { written: boolean, path: string | null } — never throws
 *   - silently no-ops on ENOENT, EPERM, malformed inputs
 *   - caps file at SIZE_CAP_BYTES (5120 = 5KB) by truncating user_messages
 *     bullets from the tail (oldest preserved)
 *   - atomic write: temp file + rename
 *
 * Digest shape (markdown):
 *
 *   # <title or fallback>
 *
 *   _Session: <session_id> · written: <iso>_
 *
 *   ## User messages (top 10)
 *
 *   - <msg 1, trimmed to MAX_MSG_CHARS>
 *   - <msg 2, ...>
 *
 *   ## Edits
 *
 *   - <path 1>
 *   - <path 2>
 */
"use strict";

const {
  existsSync,
  mkdirSync,
  renameSync,
  writeFileSync,
  unlinkSync,
  readdirSync,
  statSync,
} = require("node:fs");
const { dirname, join } = require("node:path");

const FABRIC_DIR = ".fabric";
const CACHE_REL = join(FABRIC_DIR, ".cache", "session-digests");
const SIZE_CAP_BYTES = 5120; // ~5KB
const MAX_USER_MESSAGES = 10;
const MAX_MSG_CHARS = 500;
const MAX_EDIT_PATHS = 60;
// ISS-20260608-028: soft retention for local digests (ms). Best-effort unlink on write.
const DIGEST_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;

// Lightweight credential redaction for digest bullets (hook CJS cannot import
// packages/shared). Patterns mirror store/secret-scan credential rules.
const DIGEST_SECRET_RES = [
  [/\bAKIA[0-9A-Z]{16}\b/g, "[REDACTED]"],
  [/-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----/g, "[REDACTED]"],
  [/\bsk-[A-Za-z0-9]{20,}\b/g, "[REDACTED]"],
  [/\bgh[pousr]_[A-Za-z0-9]{20,}\b/g, "[REDACTED]"],
  [/\bxox[baprs]-[A-Za-z0-9-]{10,}\b/g, "[REDACTED]"],
  [/(?:password|passwd|secret|api[_-]?key|access[_-]?token|token)\s*[:=]\s*(?:"[^'"\s]{8,}"|'[^'"\s]{8,}'|[A-Za-z0-9_./+=:@-]{8,})/gi, "[REDACTED]"],
  // ISS-20260713-027: PII parity with packages/shared secret-scan PII_RULES
  [/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, "[REDACTED:email]"],
  [/\b(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\b/g, "[REDACTED:ipv4]"],
  [/(?<!\d)(?:\+?1[-.\s]?)?(?:\(?\d{3}\)?[-.\s]?)\d{3}[-.\s]?\d{4}(?!\d)/g, "[REDACTED:phone]"],
];

function redactDigestText(text) {
  if (typeof text !== "string" || text.length === 0) return text;
  let out = text;
  for (const entry of DIGEST_SECRET_RES) {
    const re = Array.isArray(entry) ? entry[0] : entry;
    const ph = Array.isArray(entry) ? entry[1] : "[REDACTED]";
    re.lastIndex = 0;
    out = out.replace(re, ph);
  }
  return out;
}

function pruneStaleDigests(cacheDir, nowMs) {
  try {
    if (!existsSync(cacheDir)) return;
    const now = typeof nowMs === "number" && Number.isFinite(nowMs) ? nowMs : Date.now();
    for (const name of readdirSync(cacheDir)) {
      if (!name.endsWith(".md")) continue;
      const full = join(cacheDir, name);
      try {
        const st = statSync(full);
        if (now - st.mtimeMs > DIGEST_MAX_AGE_MS) unlinkSync(full);
      } catch {
        // best-effort per file
      }
    }
  } catch {
    // best-effort
  }
}

function sanitizeSessionId(id) {
  if (typeof id !== "string") return "";
  // Allow alphanumeric, dash, underscore. Strip everything else to avoid
  // path traversal / weird filename characters. Hard-cap 64 chars.
  const cleaned = id.replace(/[^A-Za-z0-9_\-]/g, "_").slice(0, 64);
  return cleaned;
}

function truncateString(s, max) {
  if (typeof s !== "string") return "";
  const t = s.replace(/\s+/g, " ").trim();
  if (t.length <= max) return t;
  return `${t.slice(0, max - 1)}…`;
}

function renderDigest({ session_id, title, user_messages, edit_paths }) {
  const safeTitle =
    typeof title === "string" && title.trim().length > 0
      ? truncateString(title, 120)
      : "(untitled session)";
  const safeSessionId = sanitizeSessionId(session_id) || "(unknown)";
  const userMsgs = Array.isArray(user_messages) ? user_messages : [];
  const edits = Array.isArray(edit_paths) ? edit_paths : [];

  const messageBullets = userMsgs
    .slice(0, MAX_USER_MESSAGES)
    .map((m) => `- ${truncateString(redactDigestText(String(m ?? "")), MAX_MSG_CHARS)}`)
    .filter((line) => line.length > 2);

  const editBullets = edits
    .slice(0, MAX_EDIT_PATHS)
    .map((p) => `- ${truncateString(redactDigestText(String(p ?? "")), 200)}`)
    .filter((line) => line.length > 2);

  const messagesSection =
    messageBullets.length > 0
      ? messageBullets.join("\n")
      : "_(no user messages captured)_";
  const editsSection =
    editBullets.length > 0 ? editBullets.join("\n") : "_(no edits captured)_";

  return [
    `# ${safeTitle}`,
    "",
    `_Session: ${safeSessionId} · written: ${new Date().toISOString()}_`,
    "",
    "## User messages (top 10)",
    "",
    messagesSection,
    "",
    "## Edits",
    "",
    editsSection,
    "",
  ].join("\n");
}

/**
 * Soft-cap the rendered digest to SIZE_CAP_BYTES. If the rendered text exceeds
 * the cap we drop user message bullets from the tail (keeping the earliest /
 * oldest entries first since those usually frame the session goal) until the
 * size fits OR we run out of trimmable content.
 */
function capSize(text, original) {
  if (Buffer.byteLength(text, "utf8") <= SIZE_CAP_BYTES) return text;
  let userMessages = Array.isArray(original.user_messages)
    ? [...original.user_messages]
    : [];
  let attempt = text;
  while (
    Buffer.byteLength(attempt, "utf8") > SIZE_CAP_BYTES &&
    userMessages.length > 0
  ) {
    userMessages.pop();
    attempt = renderDigest({ ...original, user_messages: userMessages });
  }
  if (Buffer.byteLength(attempt, "utf8") <= SIZE_CAP_BYTES) return attempt;
  // Last resort: hard-truncate.
  const buf = Buffer.from(attempt, "utf8").slice(0, SIZE_CAP_BYTES);
  return buf.toString("utf8");
}

function writeDigest(opts) {
  if (opts === null || typeof opts !== "object") {
    return { written: false, path: null };
  }
  const projectRoot = typeof opts.projectRoot === "string" ? opts.projectRoot : "";
  const safeSessionId = sanitizeSessionId(opts.session_id);
  if (projectRoot.length === 0 || safeSessionId.length === 0) {
    return { written: false, path: null };
  }
  const cacheDir = join(projectRoot, CACHE_REL);
  const target = join(cacheDir, `${safeSessionId}.md`);
  const tmp = `${target}.tmp-${process.pid}-${Date.now()}`;

  try {
    const rendered = renderDigest(opts);
    const capped = capSize(rendered, opts);
    if (!existsSync(cacheDir)) {
      mkdirSync(cacheDir, { recursive: true });
    }
    pruneStaleDigests(cacheDir);
    writeFileSync(tmp, capped, "utf8");
    renameSync(tmp, target);
    return { written: true, path: target };
  } catch {
    // Best-effort. Never let digest write failure propagate to the Stop hook.
    try {
      if (existsSync(tmp)) unlinkSync(tmp);
    } catch {
      // ignore
    }
    return { written: false, path: null };
  }
}

function purgeSessionDigestsBestEffort(projectRoot, nowMs) {
  const cacheDir = join(projectRoot, CACHE_REL);
  pruneStaleDigests(cacheDir, nowMs);
}
module.exports = {
  writeDigest,
  // exposed for tests
  renderDigest,
  capSize,
  CONSTANTS: {
    CACHE_REL,
    SIZE_CAP_BYTES,
    MAX_USER_MESSAGES,
    MAX_MSG_CHARS,
    MAX_EDIT_PATHS,
  },
  pruneStaleDigests,
  purgeSessionDigestsBestEffort,
  DIGEST_MAX_AGE_MS,
};
