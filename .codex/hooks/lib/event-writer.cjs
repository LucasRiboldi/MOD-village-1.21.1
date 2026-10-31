// ux-w2-9: the SINGLE guarded events.jsonl write path for .cjs hooks.
//
// Before this, every hook (knowledge-hint-broad / fabric-hint / post-tooluse-
// mutation) hand-stamped the event envelope (kind/id/ts/schema_version) and
// called appendLockedLine directly. A forgotten field or a non-string event_type
// produced a row the doctor's event-ledger Zod read (event_ledger_schema_compat)
// would reject. This module centralizes the stamp + guard so every cjs-written
// row satisfies the same envelope contract the TS appendEventLedgerEvent enforces
// via Zod — without pulling Zod into the no-build .cjs runtime.
//
// Contract (mirrors eventLedgerEventSchema's envelope):
//   - event_type MUST be a non-empty string (the discriminator). Missing/blank →
//     the event is REJECTED (returns false), never written.
//   - kind / schema_version are FORCED to the ledger constants.
//   - id / ts are stamped only when absent (caller may pre-stamp for determinism).
//   - Never throws — hooks must never block; a write failure returns false.
//
// ISS-20260713-001: audit appends wait/retry (appendLockedLineWait) instead of
// drop-on-contention used by pure telemetry (injections.jsonl).
// ISS-20260713-007: free-text fields are secret/PII-redacted before write
// (mirrors packages/server event-ledger REDACTED_EVENT_FIELDS).

const { join } = require("node:path");
const { appendLockedLineWait } = require("./injection-log.cjs");

const EVENT_KIND = "fabric-event";
const SCHEMA_VERSION = 1;

// Field-name redaction (parity with packages/server/src/services/event-ledger.ts).
const REDACTED_EVENT_FIELDS = new Set([
  "intent",
  "kb_line_raw",
  "message",
  "prompt",
  "rationale",
  "reason",
  "summary",
  "user_messages_summary",
]);

// Lightweight credential + PII patterns (hook CJS cannot import packages/shared).
// Keep in sync with packages/shared/src/store/secret-scan.ts CREDENTIAL+PII rules.
const REDACT_RES = [
  [/\bAKIA[0-9A-Z]{16}\b/g, "[REDACTED:aws-access-key-id]"],
  [/-----BEGIN (?:RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----/g, "[REDACTED:private-key-block]"],
  [/\bsk-[A-Za-z0-9]{20,}\b/g, "[REDACTED:openai-api-key]"],
  [/\bgh[pousr]_[A-Za-z0-9]{20,}\b/g, "[REDACTED:github-token]"],
  [/\bxox[baprs]-[A-Za-z0-9-]{10,}\b/g, "[REDACTED:slack-token]"],
  [
    /(?:password|passwd|secret|api[_-]?key|access[_-]?token|token)\s*[:=]\s*(?:"[^'"\s]{8,}"|'[^'"\s]{8,}'|[A-Za-z0-9_./+=:@-]{8,})/gi,
    "[REDACTED:credential-assignment]",
  ],
  [/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, "[REDACTED:email-address]"],
  [
    /\b(?:(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\.){3}(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)\b/g,
    "[REDACTED:ipv4-address]",
  ],
  [
    /(?<!\d)(?:\+?1[-.\s]?)?(?:\(?\d{3}\)?[-.\s]?)\d{3}[-.\s]?\d{4}(?!\d)/g,
    "[REDACTED:phone-number]",
  ],
];

function redactText(text) {
  if (typeof text !== "string" || text.length === 0) return text;
  let out = text;
  for (const [re, placeholder] of REDACT_RES) {
    re.lastIndex = 0;
    out = out.replace(re, placeholder);
  }
  return out;
}

function redactEventFields(event) {
  const out = { ...event };
  for (const key of Object.keys(out)) {
    if (REDACTED_EVENT_FIELDS.has(key) && typeof out[key] === "string") {
      out[key] = redactText(out[key]);
    }
  }
  return out;
}

function safeUuid() {
  try {
    // eslint-disable-next-line global-require
    return require("node:crypto").randomUUID();
  } catch {
    // crypto unavailable (exotic runtime) → time+rand fallback, still unique enough.
    return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
  }
}

// Stamp the canonical envelope onto a caller event, or return null when the event
// fails the guard (not an object / missing event_type). Pure (no I/O).
function stampEvent(event) {
  if (!event || typeof event !== "object" || Array.isArray(event)) return null;
  if (typeof event.event_type !== "string" || event.event_type.length === 0) return null;
  const redacted = redactEventFields(event);
  return {
    ...redacted,
    kind: EVENT_KIND,
    schema_version: SCHEMA_VERSION,
    id: typeof redacted.id === "string" && redacted.id.length > 0 ? redacted.id : `event:${safeUuid()}`,
    ts: typeof redacted.ts === "number" ? redacted.ts : Date.now(),
  };
}

// Append ONE guarded event line to <fabricDir>/events.jsonl. Returns true on a
// successful write, false if the event failed the guard or the append errored.
function appendEvent(fabricDir, event) {
  const stamped = stampEvent(event);
  if (stamped === null) return false;
  try {
    return appendLockedLineWait(join(fabricDir, "events.jsonl"), JSON.stringify(stamped) + "\n");
  } catch {
    return false;
  }
}

// Append MANY guarded events in a SINGLE locked write (preserves the batched
// append the post-tooluse hook relied on). Invalid events are dropped; returns
// the count actually written. A zero-valid batch performs no write.
function appendEvents(fabricDir, events) {
  if (!Array.isArray(events)) return 0;
  const stamped = events.map(stampEvent).filter((e) => e !== null);
  if (stamped.length === 0) return 0;
  try {
    const ok = appendLockedLineWait(
      join(fabricDir, "events.jsonl"),
      stamped.map((e) => JSON.stringify(e)).join("\n") + "\n",
    );
    return ok ? stamped.length : 0;
  } catch {
    return 0;
  }
}

module.exports = { appendEvent, appendEvents, stampEvent, redactText };
