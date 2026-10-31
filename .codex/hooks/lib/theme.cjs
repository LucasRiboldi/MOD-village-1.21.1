// @generated from packages/shared/src/theme.ts by scripts/build-hook-project-context.mjs; DO NOT EDIT
'use strict';


// ../shared/src/theme.ts
var ANSI = {
  reset: "\x1B[0m",
  bold: "\x1B[1m",
  dim: "\x1B[2m"
};
var PALETTE = {
  success: "\x1B[38;2;46;204;113m",
  // emerald
  warn: "\x1B[38;2;180;120;0m",
  // darker amber — ISS-20260713-068 light-terminal contrast
  error: "\x1B[38;2;231;76;60m",
  // alizarin
  drift: "\x1B[38;2;155;89;182m",
  // amethyst
  ai: "\x1B[38;2;52;152;219m",
  // peter-river blue
  human: "\x1B[38;2;26;188;156m",
  // turquoise
  accent: "\x1B[38;2;155;89;182m",
  // amethyst (headers / emphasis)
  muted: ANSI.dim
};
var PALETTE_256 = {
  success: "\x1B[38;5;77m",
  warn: "\x1B[38;5;178m",
  error: "\x1B[38;5;203m",
  drift: "\x1B[38;5;141m",
  ai: "\x1B[38;5;75m",
  human: "\x1B[38;5;80m",
  accent: "\x1B[38;5;141m",
  muted: ANSI.dim
};
function isColorEnabled(env = process.env, isTTY) {
  if (env.NO_COLOR) return false;
  const force = env.FORCE_COLOR;
  if (force !== void 0) return force !== "0" && force.toLowerCase() !== "false";
  return Boolean(isTTY ?? process.stdout.isTTY);
}
function detectColorDepth(env = process.env, isTTY) {
  if (!isColorEnabled(env, isTTY)) return "none";
  const colorterm = (env.COLORTERM || "").toLowerCase();
  if (colorterm.includes("truecolor") || colorterm.includes("24bit")) return "truecolor";
  const term = (env.TERM || "").toLowerCase();
  if (term.includes("256color") || term.includes("256")) return "ansi256";
  if (term === "linux" || term === "dumb") return "ansi16";
  return "truecolor";
}
function paint(token, text, colorOn = isColorEnabled(), depth = detectColorDepth()) {
  if (!colorOn || depth === "none") return text;
  const pal = depth === "ansi256" ? PALETTE_256 : PALETTE;
  return `${pal[token]}${text}${ANSI.reset}`;
}
var SYMBOL_ASCII = { ok: "[ok]", warn: "[warn]", error: "[error]" };
var SYMBOL_GLYPH = { ok: "[ok] \u2713", warn: "[warn] !", error: "[error] x" };
var SYMBOL_TOKEN = { ok: "success", warn: "warn", error: "error" };
function symbol(kind, colorOn = isColorEnabled()) {
  return colorOn ? paint(SYMBOL_TOKEN[kind], SYMBOL_GLYPH[kind], true) : SYMBOL_ASCII[kind];
}
function sectionBar(title, colorOn = isColorEnabled()) {
  return colorOn ? `${ANSI.bold}${PALETTE.accent}\u258C ${title}${ANSI.reset}` : `# ${title}`;
}
function headerRule(title, colorOn = isColorEnabled()) {
  const head = colorOn ? `${ANSI.bold}${PALETTE.human}${title}${ANSI.reset}` : title;
  const rule = paint("muted", (colorOn ? "\u2500" : "-").repeat(40), colorOn);
  return `${head}
${rule}`;
}
var SCOPE_BADGE_TOKEN = { team: "drift", project: "ai", personal: "human" };
function scopeBadge(scope, colorOn = isColorEnabled()) {
  const text = `[${scope}]`;
  return colorOn ? paint(SCOPE_BADGE_TOKEN[scope], text, true) : text;
}

exports.ANSI = ANSI;
exports.PALETTE = PALETTE;
exports.PALETTE_256 = PALETTE_256;
exports.SYMBOL_ASCII = SYMBOL_ASCII;
exports.detectColorDepth = detectColorDepth;
exports.headerRule = headerRule;
exports.isColorEnabled = isColorEnabled;
exports.paint = paint;
exports.scopeBadge = scopeBadge;
exports.sectionBar = sectionBar;
exports.symbol = symbol;
