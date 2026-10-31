# Cite-contract + 裁决阶梯参考 (v2.2 SK5)

> 本文是 cite policy 与裁决(adjudication)的**权威详参**,从 bootstrap (`.fabric/AGENTS.md`) 下沉至此,避免 bootstrap 随治理细节膨胀。bootstrap 只留**可执行 core**(cite 行格式 + 验证义务 + operator 例),完整 enum 词典 / 类型路由 / 稽核 / backward-compat / 裁决阶梯看这里。`fabric audit cite` 的稽核口径以本文为准。

## 1. Cite 行格式 (回顾)

edit / decide / propose plan 之前,**回复首行**:

```
KB: <id> (<≤8字 用法>) [applied|dismissed:<reason>]
KB: none [<reason>]
```

- `[applied]` 前必须先 `fab_recall`(并按需对其正文路径做原生 Read)实际抓 KB body —— 防编造 id。验证不通过 = 不能 cite。
- **store 前缀** (多 store):read-set 含多 store 且同一 local id 跨 store shadow 时,cite 必须 store-qualified:`KB: <store-alias>:<id> ...`(如 `KB: team:KT-DEC-0001 (auth) [applied]`)。单 store / 无歧义时裸 `KB: <id>` 仍 valid。personal-only 条目 cite 进团队产物 = 强 warning(防泄漏 R5#3)。

## 2. Contract 语法 (decisions/pitfalls 类 `[applied]`)

cite 尾段加 contract:`→ <operator> [<operator> ...]`

| operator | 含义 |
|---|---|
| `edit:<glob>` | 本 cite 承诺会改的文件范围 |
| `!edit:<glob>` | 承诺**不**改的范围 |
| `require:<symbol>` | 实现必须含某 symbol |
| `forbid:<symbol>` | 实现必须不含某 symbol |
| `skip:<reason>` | 本条 applied 但某 operator 跳过,附理由 |

例:`KB: K-001 (auth) [applied] → edit:src/auth/**/*.ts !edit:src/legacy/**`

## 3. 枚举词典

- **skip reason**:`sequencing | conditional | semantic | aesthetic | architectural | other:<text>`
- **dismissed reason**:`scope-mismatch | outdated | not-applicable | other:<text>`
- **`KB: none` sentinel**:`[no-relevant]`(已调 recall/plan_context 但无可用条目) / `[not-applicable]`(当前动作不在 cite 范围:纯探索 / Bash 只读 / 用户问答)。裸 `KB: none` 仍 valid,归 `[unspecified]`(legacy 兼容)。

## 4. 类型路由

- `models` 类引用 = reference cite,**不需 contract**。
- `guidelines` / `processes` 类暂不强制 contract,推后 LLM-judge。
- `decisions` / `pitfalls` 类 `[applied]` **需** contract。

## 5. 稽核 + Backward compat

- 稽核:`fabric audit cite [--since=7d] [--client=cc|codex|all]` 输出覆盖率,含 `KB: none` sentinel 拆分。不阻断工作,只记录。
- Backward compat:解析器同时接受老 4-state tags(`planned` / `recalled` / `chained-from <id>`),都映射到 `[applied]` 语义;旧 session cite 仍计入 cite-coverage。

## 6. 裁决阶梯 (adjudication ladder)

执行中遇到**需要拍板**的分歧(plan 选型 / review 取舍 / cross-LLM 不一致),按三级阶梯收口,**只有真正属于 human 的决定才阻塞**:

1. **AI 自决** — 已有清晰证据 + 明确推荐时,直接执行,记 rationale。不为有默认值的选择拆 sub-question。
2. **多-LLM 评审(含 ≥1 零上下文冷评)** — 主观 / 高风险 / 跨 LLM 分歧时,maestro delegate ≥2 LLM(至少一个零上下文,防执行者自我合理化)。一致 PASS 自动闭;一致 BLOCK + fix verbatim 采纳重跑。
3. **非阻塞队列** — 仍分歧 / 主观 / 不可逆 / 属 frame 级(只有 human 能挑战 frame)→ 升 `needs_adjudication` 非阻塞队列,带两方理由 + AI 倾向裁决,继续推进不卡死。

**Anchor**:critic 只能 frame 内审计;多-LLM 收敛 ≠ 正确,frame 级判断留给 human。cross-LLM 给 suggested fix 时直接 verbatim 采纳(+ trade-off 注释)。
