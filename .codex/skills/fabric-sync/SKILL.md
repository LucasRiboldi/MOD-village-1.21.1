---
name: fabric-sync
description: 多 store git 同步 thin shim — 路由到 `fabric sync` 引擎,仅 rebase 冲突时由 AI 辅助决断。Triggers 同步知识库/sync stores.
---

# fabric-sync — store 同步 thin shim

跨挂载知识 store 的 git 同步意图路由。CLI `fabric sync` 是事务/状态机引擎(遍历 store、rebase、push、persist session);本 skill 只在**冲突**时补上 AI 决断辅助 —— 其余全交 CLI。

## 意图 → 命令映射

| 意图 | 命令 |
|---|---|
| 同步所有挂载 store | `fabric sync`(逐 store 报 `synced` / `offline`(defer,不报错) / `conflict`) |
| 冲突解决后继续 | `fabric sync --continue` |
| 放弃该 store 的本次同步 | `fabric sync --abort` |
| 列出挂载的 store | `fabric store list`(见 `fabric-store`) |

## 唯一需 AI 的一步 — 冲突辅助(仅 `conflict` 时)

rebase 停在中途时:展示冲突文件的 `<<<<<<<`/`=======`/`>>>>>>>` 段,解释两侧(ours=本地草稿/晋升,theirs=远端协作者)并给合并建议(知识条目通常可并存或取更高 maturity)。**NEVER 擅自丢弃任一侧**;用户拍板后 `--continue`,放弃则 `--abort`。

## 红线

- Hook/skill NEVER 直接解析或执行 store 内文件(S65:store 是数据-only);store 状态 MUST 经 CLI JSON 取。
- 仅 remote-backed store 参与同步;local-only 跳过并提示加 remote 备份。
- 冲突合并不静默丢知识;promotion 经普通 git commit,不跨 store 搬运条目。
