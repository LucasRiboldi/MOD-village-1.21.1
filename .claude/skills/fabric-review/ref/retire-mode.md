# Retire sub-flow — 语义淘汰 canonical 条目 (W3-C: 吸收原 fabric-audit)

`maintain` 模式的 **retire** 子流程:把陈旧 / 孤儿 / 被取代的 **已 canonical** 条目按语义淘汰收口 —— 而不是一删了之。引擎是 `fabric doctor`(跑 lint、算 health、给 orphan/stale 信号);本子流程按用户意图挑动作,守两条红线,落盘仍走 `fab_review` 写路径(单一写路径,不自改 store `knowledge/`)。

写新条目用 default archive;批量审 pending 用 `pending` 模式;retire 专管 **已归档条目的退役**。

## 进入 retire 子流程

`maintain` 关键词命中「审计 / 体检知识库」「清理陈旧知识」「这些旧决策还要吗」「知识库瘦身」「淘汰旧决策」「deprecate 条目」,或 doctor 报了 orphan/stale/低 health 想逐条处置时。

## 两条红线(NON-NEGOTIABLE)

1. **deprecate-over-delete**:陈旧 ≠ 该删。一条「当时为什么这么决策」的 decision/pitfall 即使方案已换,其 rationale 仍是知识。退役 = 降 maturity(proven→verified→draft)/ 标 `deprecated` + 记 `superseded_by`(保留正文),而非 `rm`。删除只用于「从未成立 / 纯噪声 / 重复」的条目。
2. **rescue-before-delete**:任何 *打算删* 的条目,删前必做抢救检查 —— 它是否携带别处没有的独特 rationale / 反例 / 边界?有则先 merge 进取代它的条目(或在新条目加 `related` 边指回),再删空壳。抢救检查没做过,不许删。

## 意图 → 动作映射

| 意图 | 动作 |
|---|---|
| 体检 / 健康度 | `fabric doctor`(读 lint + health rollup);零阻断,只报告 |
| 找孤儿 / 陈旧条目 | `fabric doctor`(消费 orphan / stale / orphan-demote 信号) |
| 退役一条陈旧条目 | **不删** → 降 maturity 或标 deprecated + `superseded_by`;经 `fab_review` 落盘 |
| 删一条「从未成立 / 重复」条目 | 先跑 rescue 检查(独特 rationale?有则 merge/加 related);确认空壳后才删 |
| 被取代但有价值 | rescue:把独特 rationale merge 进取代条目,新条目加 `related` 边指回,再退役旧条目 |

## 流程(逐条处置)

1. `fabric doctor` 取 KB health + orphan/stale 候选清单(引擎给信号,不自算)。
2. 对每个候选判 **三态**:still-valid(留) / superseded(退役,走 deprecate) / never-valid(删,走 rescue 检查)。
3. superseded → deprecate:降 maturity 或标 deprecated + `superseded_by`,保留正文 rationale。
4. never-valid → rescue-before-delete:独特知识?有则 merge + `related`,无则删空壳。
5. 处置经 `fab_review` 写路径落盘(本子流程给决策,fab_review 做写入),保持单一写路径。

## Scope re-assignment(迁移 / backfill 后纠偏)

`fabric store migrate backfill` 给老条目补 `semantic_scope`,**默认把所有 team-layer 条目标成 `semantic_scope: team`** —— over-broad 的保守默认,会让项目专属知识错误暴露给所有项目。纠偏:

- **team-scope 判定测试**:「换一个**没有本 app 代码**的不同 repo,这条知识还成立吗?」—— 成立才是 `team`。app **内部**跨功能 / 跨玩法复用的共享组件(同一 app 多处用)**≠ team**,仍是 `project:<id>`(跨玩法复用 ≠ 跨项目复用,如 VoiceRoom)。
- **纠偏动作**:`fabric store migrate scope <store> --to project:<id> --id <id>`。
- 完整判定树 / worked examples 见单一真源:`fabric-archive/ref/phase-3-7-semantic-scope.md`。

## Constraints

- 本子流程**只读 + 给处置建议**;实际写入(降级 / 标记 / 删)经 `fab_review` 写路径,不自行改 store `knowledge/`。
- NEVER 绕过 rescue 检查直接删;删前 MUST 先跑抢救。删是最后手段,默认 deprecate。
- store counters 派生态严禁手改;退役改的是 markdown frontmatter(maturity / deprecated / superseded_by),再 `fabric doctor --fix` reconcile。
- health / orphan / stale 一律取自 `fabric doctor` JSON 输出,不在 skill 内重算(单一真源)。
