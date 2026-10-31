# Worked Examples — full reference

> **Loaded on demand.** SKILL.md gives the operative contract (the 5 types + layer heuristic + slug rules + MCP call shape). These end-to-end examples illustrate the integration — load when you want to see all fields filled out together.

## Worked Examples

## Example 1 — decision (team)

Session: User and agent debated whether the Stop-hook should be one .cjs script or three per-client scripts. Settled on one because stdout JSON shape `{"decision":"block","reason"}` is identical across Claude / Codex.

Skill output:

```ts
mcp__fabric__fab_propose({
  source_sessions: ["WFS-2026-05-10-rc2"],
  recent_paths: ["templates/claude-hooks/", "packages/cli/src/commands/hooks.ts"],
  user_messages_summary: "User pushed back on three-script proposal; agreed single .cjs because stdout JSON shape is universal across Claude Code and Codex CLI.",
  type: "decisions",
  slug: "single-cjs-hook-script",
  layer: "team",
  relevance_scope: "narrow",
  relevance_paths: [
    "templates/claude-hooks/**/*.cjs",
    "packages/cli/src/commands/hooks.ts"
  ],
  proposed_reason: "decision-confirmation",
  session_context: "Session goal: ship Stop-hook for v2 release.\nTurning point: user rejected 3-script proposal after seeing identical stdout JSON across Claude / Codex.\nResult: single .cjs path locked in."
})
```

Layer = team (引用本项目代码 + fabric-import 路径产物 signals). Scope = narrow (tied to hook templates + hooks command module; single-module evidence in edit_paths).

### Example 2 — pitfall (team)

Session: deepMerge silently replaced the existing `hooks.Stop[]` array in `.claude/settings.json` instead of appending. Cost ~30 min to diagnose.

Skill output:

```ts
mcp__fabric__fab_propose({
  source_sessions: ["WFS-2026-05-10-rc2"],
  recent_paths: ["packages/cli/src/config/json.ts"],
  user_messages_summary: "deepMerge default behavior REPLACES arrays. hooks.Stop[] needs an array-append-with-dedupe special case keyed on .command string match.",
  type: "pitfalls",
  slug: "deepmerge-array-replace-trap",
  layer: "team",
  relevance_scope: "broad",
  relevance_paths: [],
  proposed_reason: "diagnostic-then-fix",
  session_context: "Session goal: wire hook installer for v2.\nTurning point: spent ~30 min chasing why prior Stop[] entries vanished — root cause was deepMerge replacing arrays silently.\nResult: array-append-with-dedupe special case added."
})
```

Layer = team (绑定本项目代码的 pitfall signal). Scope = broad (deepMerge gotcha is cross-cutting — applies anywhere JSON merge is used, not just `json.ts`).

### Example 3 — guideline (personal)

Session: User mentioned across three projects that they prefer 2-space indent in TypeScript and 4-space in Python.

Skill output:

```ts
mcp__fabric__fab_propose({
  source_sessions: ["WFS-2026-05-10-rc2"],
  recent_paths: [".editorconfig"],
  user_messages_summary: "Personal indent preference: 2-space TS / 4-space Py. Stable across multiple projects, not project-specific.",
  type: "guidelines",
  slug: "indent-style-by-language",
  layer: "personal",
  relevance_scope: "broad",
  relevance_paths: [],
  proposed_reason: "explicit-user-mark",
  session_context: "Session goal: align editor config.\nTurning point: user said '一直 prefer 2-space TS / 4-space Py，across projects'.\nResult: personal-layer guideline; not bound to this project."
})
```

Layer = personal (跨项目通用 + 工具/编辑器偏好 signals dominate; no 强 team signal applies). Scope = broad with `relevance_paths=[]` (personal layer ALWAYS forces broad — paths don't generalize across projects per Phase 3.5 special case).

