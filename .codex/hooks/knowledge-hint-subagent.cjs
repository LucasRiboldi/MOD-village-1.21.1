#!/usr/bin/env node
/**
 * SubagentStart knowledge injection (W5 #6).
 *
 * A sub-agent starts with a blank conversation. It does NOT inherit the
 * dispatcher's SessionStart injection, and it does not inherit whatever the
 * dispatcher recalled during the session — yet `trellis-implement` /
 * `trellis-check` and their equivalents are precisely the agents that go on to
 * EDIT the repo. Every standing decision, pitfall and guideline the main agent
 * was handed was invisible to the one actually writing the code.
 *
 * ## Why this is a SubagentStart hook and not PreToolUse
 *
 * The obvious-looking design — a PreToolUse hook on the dispatch tool that
 * rewrites the sub-agent's prompt — does not work:
 *
 *   - PreToolUse matchers filter on TOOL NAME; the dispatch tool is not a
 *     stable matcher target across clients.
 *   - PreToolUse `additionalContext` lands next to the tool RESULT, i.e. in the
 *     DISPATCHER's context. It would nudge the wrong agent and rely on the
 *     dispatcher choosing to relay it.
 *
 * `SubagentStart` is the purpose-built event: its `additionalContext` is
 * injected into the sub-agent's own conversation before it begins work.
 *
 * ## Why this injects the knowledge instead of an instruction to fetch it
 *
 * "Call fab_recall on the files you are about to touch" is an inert
 * instruction for most sub-agents: an agent definition declares its own tool
 * allowlist, and the edit-capable ones commonly ship without MCP tools at all
 * (Read/Write/Edit/Bash/Glob/Grep). Telling an agent to call a tool it does not
 * have produces nothing. So this hook ships the CONTENT — the same broad index
 * the dispatcher got — and mentions fab_recall only as a conditional upgrade.
 *
 * ## Why it reuses knowledge-hint-broad's renderer rather than its main()
 *
 * Rendering is delegated to `buildSessionStartSinks` so the sub-agent sees a
 * byte-identical index to the dispatcher's — a second renderer here would be a
 * second copy of one fact, i.e. guaranteed drift.
 *
 * `main()` is deliberately NOT reused, because its SessionStart-specific
 * behaviour is actively wrong at this cadence:
 *   - the broad cooldown sidecar is one shared slot; a dispatch-time emit would
 *     silence the human's next real SessionStart banner (and vice versa);
 *   - the human `systemMessage` breadcrumb is an operator-facing census — a
 *     sub-agent cannot act on it, and it would double-print per dispatch;
 *   - injection telemetry keys the hit-rate DENOMINATOR off SessionStart
 *     injections. Counting N dispatches as N session starts silently deflates
 *     that rate; a metric that lies is worse than a missing one.
 *
 * Never-block contract (KT-DEC-0007): any failure exits silently. A sub-agent
 * that starts without the index is strictly better than one that fails to
 * start.
 */

const broad = require("./knowledge-hint-broad.cjs");
const { emitDualSink, detectClient } = require("./lib/client-adapter.cjs");
const { readFabricLanguage } = require("./lib/banner-i18n.cjs");
const { createProjectContextResolver } = require("./lib/project-root.cjs");

/**
 * The framing that makes the injected index actionable for a sub-agent.
 *
 * It states the one thing the sub-agent cannot know on its own — that its
 * context is fresh and this is not a recap of something it already saw — and
 * makes the retrieval line conditional, because the agent's own tool allowlist
 * is the authority on whether fab_recall exists for it.
 */
function preamble(zh) {
  return zh
    ? [
        "  你是被派发的子代理: 你的上下文是全新的, 没有继承派发方的 SessionStart 注入,",
        "  也没有继承它这一会话里召回过的任何条目。下面是本仓库的常驻知识索引 —— 改任何",
        "  文件前按它行事。若你的工具白名单里有 fab_recall, 先对将改文件调一次拿更精确的",
        "  narrow 条目; 若没有, 就按下面的索引行事, 需要正文时用 Read 取文末给出的路径。",
      ].join("\n")
    : [
        "  You are a dispatched sub-agent: your context is fresh. You did NOT inherit",
        "  the dispatcher's SessionStart injection, nor anything it recalled during the",
        "  session. Below is this repo's standing knowledge index — act on it before you",
        "  edit any file. If fab_recall is in your tool allowlist, call it on the files",
        "  you are about to touch for the narrow entries too; if it is not, act on the",
        "  index below and Read the path given in the footer when you need a body.",
      ].join("\n");
}

function main(env, stdio) {
  try {
    const cwd = (env && env.cwd) || process.cwd();
    const out = (stdio && stdio.stdout) || process.stdout;
    const err = (stdio && stdio.stderr) || process.stderr;

    // Test seam mirrors knowledge-hint-broad: env.payload short-circuits the
    // CLI spawn so unit tests need no built binary.
    const payload =
      env && env.payload !== undefined ? env.payload : broad.invokePlanContextHint(cwd);
    if (payload === null || payload === undefined) return; // silent

    const { ai, hasRenderedContent } = broad.buildSessionStartSinks(cwd, payload, env);
    if (!hasRenderedContent) return;
    if (typeof ai !== "string" || ai.length === 0) return;

    const zh = readFabricLanguage(cwd) === "zh-CN";
    const context = `${preamble(zh)}\n${ai}`;

    if (env && env.skipStdout === true) {
      err.write(`${context}\n`);
      return;
    }
    // human: null on purpose — the census breadcrumb is operator-facing and the
    // operator already got it at SessionStart. Only the AI sink is emitted.
    emitDualSink(
      { human: null, ai: context },
      {
        client: detectClient(),
        eventName: "SubagentStart",
        streams: { stdout: out, stderr: err },
      },
    );
  } catch {
    // Silent — SubagentStart MUST NEVER block the sub-agent from starting.
  }
}

module.exports = { main, preamble };

if (require.main === module) {
  const context = createProjectContextResolver({ explicitRoot: process.env.CLAUDE_PROJECT_DIR });
  main({ cwd: context.workspaceRoot });
}
