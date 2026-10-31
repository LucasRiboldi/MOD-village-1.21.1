// ISS-20260713-053: shared tool_input path harvest for knowledge-hint-narrow.
const { relative, isAbsolute } = require("node:path");

function extractApplyPatchPaths(toolInput) {
  if (!toolInput || typeof toolInput !== "object") return [];
  const candidates = [toolInput.input, toolInput.patch, toolInput.content, toolInput.file_path];
  const collected = [];
  const fileDirectiveRe = /^\*\*\*\s+(?:Update|Add|Delete)\s+File:\s+(.+?)\s*$/gm;
  for (const c of candidates) {
    if (typeof c !== "string" || c.length === 0) continue;
    if (!c.includes("***") && (c.includes("/") || c.endsWith(".ts") || c.endsWith(".js") || c.endsWith(".md"))) {
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
 * Normalize absolute paths under cwd to project-relative; drop out-of-tree.
 * Preserves already-relative in-tree paths. POSIX-style separators in output.
 */
function toProjectRelativePaths(cwd, paths) {
  if (!Array.isArray(paths)) return [];
  return paths
    .filter((p) => typeof p === "string" && p.length > 0)
    .map((p) => {
      const rel = isAbsolute(p) ? relative(cwd, p) : p;
      return rel.startsWith("..") ? null : rel;
    })
    .filter((p) => typeof p === "string" && p.length > 0)
    .map((p) => p.split(/[\\/]/).join("/"));
}

module.exports = {
  extractApplyPatchPaths,
  extractPaths,
  toProjectRelativePaths,
};
