---
name: fabric-store
description: store 运维 thin shim — 把「创建/挂载/绑定/列出/切换写库/迁移」意图路由到 `fabric store …` CLI。CLI 是引擎与安全门;本 skill 只指路。Triggers store 运维/挂载 store/绑定知识库.
---

# fabric-store — store 运维 thin shim

「知识 store」操作的意图路由层:CLI (`fabric store …`) 是引擎(做事 + 守破坏性操作的 confirm 门),本 skill 只把意图映射到命令。*store* 是 `~/.fabric/stores/<uuid>/` 下的平行 git 仓,与用户产品仓无关;同步见 `fabric-sync`,写条目见 `fabric-archive`/`fabric-review`。

## 意图 → 命令映射

| 意图 | 命令 |
|---|---|
| 创建全新本地 store | `fabric store create --alias <a> [--remote <url>]` |
| 挂载已存在的磁盘 store | `fabric store mount --uuid <u> --alias <a> [--remote <url>]` |
| 本项目声明需要某 store | `fabric store bind <alias-or-uuid>` |
| 列出挂载的 store | `fabric store list` |
| 设置默认写库 | `fabric store switch-write <alias>` |
| 给某 scope 单独路由写库 | `fabric store switch-write <alias> --scope <semantic_scope>` |
| 解释某 alias 如何解析 | `fabric store explain <alias>` |
| 迁移知识条目坐标(改 scope / 提升 / 补全) | `fabric store migrate {scope,promote,backfill}`(破坏性 → CLI 跑 confirm 门,`--dry-run` 预览) |
| 同步 (pull+push) | 见 `fabric-sync` skill |

## 红线(其余安全由 CLI 兜)

- `store remove` = detach ≠ delete:卸 registry,保留磁盘 git 树。
- `store mount` 拒绝幽灵挂载(磁盘无 store 树的 uuid)。
- 破坏性 `store migrate *` MUST 经 CLI 本身的 confirm-before-mutate 门(确定性来自 CLI 不靠本 skill 厚度);本 skill 不自行改 store `knowledge/` 或 counters(派生态)。
- Hook/skill NEVER 直接解析或执行 store 内文件(S65:store 是数据-only);store 状态一律经 CLI JSON 输出取。
