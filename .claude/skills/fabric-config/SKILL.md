---
name: fabric-config
description: 配置体检 + 对话式调整 — 把「太吵了/催得太勤/想让团队统一」这类感受翻译成具体配置项,并写到它唯一的那一层。CLI `fabric config` 是引擎;本 skill 负责解释现状与翻译诉求。Triggers 配置体检/太吵了/提示太多/改配置/fabric 配置.
---

# fabric-config — 配置体检 + 对话式调整

用户表达的是**感受**("提示太吵"、"归档催得太勤"、"团队想统一"),配置里存的是**键值**。本 skill 只做两件事:把当前配置讲成人话(体检),把感受翻译成键+层(调整)。做事的是 `fabric config` CLI。

## 唯一数据来源

```
fabric config --list --json
```

返回:`profile`(当前节奏档位,`null` = 自定义)、`profiles`(三个档位各自的 label/description/keys)、`fields[]`(每项的 `key` / `value` 生效值 / `source` 来自哪层 / `home` 它的家 / `label` / `description` / `default` / `allowed`)。

**`label` 与 `description` 已经是本机语言的成品文案 —— 直接用,NEVER 自己另写一套解释。** 说明的单一来源在 CLI 的 i18n 里;本 skill 复述就会漂移。

## 三个 home,决定「写哪」

| `home` | 物理位置 | 语义 |
|---|---|---|
| `preference` | `~/.fabric/fabric-global.json` → `defaults` 或 `projects[<project_id>]` | 人的偏好。默认写 `defaults`(全机器);只有用户明确说「只改这个项目」才 `--scope project` |
| `corpus` | 绑定的团队 store 根 `store-config.json` | 知识库自身属性,随 store 分发给全团队 |
| `global_root` | `~/.fabric/fabric-global.json` 顶层 | 全机器唯一(目前只有语言) |

`--scope` 只在 `preference` 项上有意义;对另外两类传 `--scope` 会被 CLI 拒绝(它们各自只有一个家)。

## 流程 A — 体检

1. 跑 `fabric config --list --json`。
2. 先报**档位**:`profile` 有值就说这个档位的 `label` + `description`;是 `null` 就说「自定义」并列出与最接近档位的差异。
3. 再报**被改过的项**:只列 `source !== "default"` 的,格式 `<label>:<value>(<source 中文名>)`。没有被改过的项就说一句「其余都是出厂值」——**不要把 19 项全念一遍**。
4. 最后提**值得注意的**,只在真的成立时才说:
   - `embed_enabled` 为 true → 提醒它只是意图开关,用 `fabric info recall` 看语义检索有没有真的在跑。
   - 有 `source === "store"` 的项 → 说明这是团队共享的,改它影响所有绑定同一 store 的人。
   - 存在 `projects[<id>]` 层的覆盖 → 说明本项目和其他项目行为会不一样。

## 流程 B — 对话式调整

**先问档位,再问单键。** 大多数诉求(下表前四行)整档解决,不要引导用户逐个调数字。

| 用户说 | 动作 |
|---|---|
| 太吵 / 提示太多 / 别烦我 | `--profile quiet` |
| 恢复默认 / 出厂设置 | `--profile standard` |
| 知识库刚起步 / 怕漏东西 / 多催我 | `--profile coach` |
| 归档催太勤 / 催得不够 | 整档更贴切;用户坚持只调一项时才动 `archive_edit_threshold` 或 `archive_hint_hours` |
| 找不到相关知识 | 先 `fabric info recall` 看语义检索状态,再看 `default_layer_filter` 是否把范围收窄了 |
| 待审草稿堆积 | `review_hint_pending_count` / `review_hint_pending_age_days`;先建议去 `fabric-review` 清一批 |
| 只想改这个项目 | 同样的键 + `--scope project` |
| 想让团队都一样 | 只有 `home === "corpus"` 的项能随 store 分发;`preference` 项做不到,要如实说明 |

命令形态:

```
fabric config --profile <quiet|standard|coach> [--scope project]
fabric config --set <key> --value <v> [--scope project]
```

档位与单键**没有优先级关系**:档位就是一次性写那几个键,之后单独改某个键直接生效。不要向用户解释成"覆盖"。

## 红线

- NEVER 直接编辑 `~/.fabric/fabric-global.json`、`store-config.json` 或 `.fabric/fabric-config.json` —— 一律走 `fabric config`,它负责校验、加锁写入和路由。
- NEVER 碰 `.fabric/fabric-config.json` 里的身份字段(`project_id` / `required_stores` / `write_routes` / `active_*`):那是 `fabric install` 与 `fabric store bind` 的产物,手改会让本仓找不到自己的 store。
- 改动 `home === "corpus"` 的项后,MUST 告诉用户去 store 仓库 commit `store-config.json`,否则团队看不到。
- 不臆造配置项:`fields[]` 里没有的键就是不可配。检索权重、载荷上限这类专家参数**故意**不在面板上,需要时直说「这项要手改 JSON,一般不建议」。
- 改完 MUST 复跑一次 `fabric config --list` 并把新的生效值念给用户,不要只报"已设置"。
