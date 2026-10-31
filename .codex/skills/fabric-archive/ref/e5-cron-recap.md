# E5 Scheduled Daily Recap — full reference

> **Loaded on demand.** Only relevant when invocation context = `cron` / `/loop` (E5 entry). SKILL.md's Phase 0 already gates this — if the user just typed `/fabric-archive`, none of the below applies.

## E5 周期触发 (Scheduled Daily Recap)

## Overview

`今日复盘` = E5 entry point. Default scope = today. Falls back to historical scan if today yields no candidates (silent-skip per Phase 4.5).

E5 是 5 入口模型中唯一由 OS 调度器或 Claude Code `/loop` 周期触发的入口形态。fabric 端**零代码**——不提供 `fabric schedule` 子命令,亦不内嵌 daemon。用户基于自己的执行环境二选一接入: `/loop`(Claude Code 原生,推荐) 或 OS cron(跨平台 fallback)。

### /loop sample (primary path for Claude Code)

```
/loop /fabric-archive 今日复盘 --cron "0 23 * * *"
```

每晚 23:00 在当前 Claude Code session 中触发 fabric-archive skill,scope = today。`/loop` 复用现有 Claude session 鉴权,无需独立 token。

### OS cron sample (cross-platform alternative)

```
# crontab -e
0 23 * * * cd /path/to/project && claude code -p "/fabric-archive 今日复盘" 2>&1 >> /var/log/fabric-daily-recap.log
```

适用于:
- 非 Claude Code 环境(纯 server / CI 节点)
- 希望脱离 /loop session 生命周期独立运行的场景
- 已有 cron / launchd 调度基础设施的团队

macOS 用户可改用 `launchd` plist;Linux 用户直接 `crontab -e`。命令需自行确保 `claude code` CLI 已安装且鉴权可用。

### E5 prompt parse contract

当用户或 cron 以 `今日复盘` / `daily recap` 字面短语触发 fabric-archive 时,skill 应按以下契约处理:

- **Phase 0 Range Resolution**: 识别 `今日复盘` / `daily recap` 为 magic phrase, 直接设置 `time_window = today` (00:00 local timezone → current ts), 无需 AskUserQuestion 兜底。
- **Phase 1.5 Onboard Coverage**: 跳过 (entry_point = E5_cron, 非 E2_explicit, 不弹 onboard 弹问)。
- **Phase 4.5 Persist Archive Attempt**: 始终写入 `session_archive_attempted` event。当今日无 archive 信号触发 viability gate FAIL 时,走 silent-skip 路径(outcome = `skipped_no_signal`),skill 静默退出,cron 日志为空。

### Trade-off table (/loop vs OS cron)

| 维度 | /loop | OS cron |
|---|---|---|
| 鉴权 | 复用 Claude session | 独立 token / login |
| 跨平台 | Claude Code 全平台一致 | macOS launchd / Linux cron 不同 |
| Token 成本 | 累积 (长 session) | 每 tick fresh, 无累积 |
| 调试 | Claude UI 可见 | 日志文件 |

### Failure modes

- **/loop session crash**: 归档暂停,用户需重启 `/loop`。无自动恢复机制——`/loop` 与 Claude Code session 生命周期绑定。
- **OS cron**: 自带恢复(下一个 tick 重新启动),但需独立 `claude code` CLI 安装与鉴权;鉴权 token 过期时 cron job 会静默失败,需人工 `claude login` 重置。

### NOT in scope

