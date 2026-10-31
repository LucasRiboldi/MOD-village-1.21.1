# Relate sub-flow — 知识图谱关联 (W3-C: 吸收原 fabric-connect)

`maintain` 模式的 **relate** 子流程:把孤立的 canonical 条目连成图,发现彼此**隐藏关联**(同一决策的正反面、pitfall↔规避它的 guideline、被取代↔取代),回写 frontmatter 的 `related: [<stable_id>...]` 图边(v2.2 H2)。下游 `fab_recall include_related:true` 据此一跳拉回连通知识。

`related` 是**有向引用**(A.related=[B] 表示「读 A 时也该看 B」),按需补反向边。

**默认不主动建边**:仅用户显式说「连一下 / 找相关条目 / 补 related 边 / 知识库连通性」时进入;不在任何 nudge 里主动提议(避免噪声边稀释图价值)。检索时临时拉关联直接 `fab_recall include_related:true`,无需建边。

## 关联类型(提议 related 边的判据)

| 类型 | 例 |
|---|---|
| 互补 | decision「用 JWT」 ↔ pitfall「JWT 过期未刷新踩坑」 |
| 规避 | pitfall「sprite 黑边」 ↔ guideline「premultiplyAlpha 正确设置」 |
| 取代 | 旧 decision ↔ 取代它的新 decision(配合 deprecated/superseded_by) |
| 同域 | 同一子系统 / 共 relevance_paths 的条目 |
| 引用链 | A 的 rationale 依赖 B 的结论 |

## 流程

1. **拿候选池**:`fab_recall(paths=[...])` 拿相关条目 + 每条 `read_path`。现有 `related` 边**不在 recall wire 上**(lean wire 只回 summary/must_read_if/impact/knowledge_type — KT-GLD-0005),要看已连状态须对候选的 `read_path` 做原生 Read、从 frontmatter 读 `related`。小库(<200 条 canonical)可扫全 index;大库按用户给的范围/paths 收窄。
2. **AI 语义判断**(不是字面):对候选两两/成簇按上表五种类型判**隐藏语义关联**。判据基点是**读 summary + rationale 后的语义理解**,不是词面重叠 —— 「权限↔访问控制」「pitfall↔绕过它的 guideline」这类语义对靠 tokenize/Jaccard 抓不到。
3. **稀疏优于稠密**:一次触发**最多提议 5-10 条**高置信边(用户一次审 20 条会疲劳,信噪比比覆盖率重要)。低置信「话题相邻」不连,宁缺毋滥。
4. **输出结构**:每条提议 = `(源 id, 目标 id, 关联类型, 一句语义理由)`;按需提议反向边。理由必须点出**语义因果**(哪条决策为什么该带出哪条 pitfall),不是「共享标签 X」。
5. **落盘**:**复用既有 `fab_review` modify 写路径**(零新写面):在源条目 frontmatter 的 `related` inline 数组追加目标 stable_id;`fabric doctor --fix` reconcile 进 agents.meta。
6. **回报**:新增/反向边数 + 连通性变化(孤岛减少)。

## Constraints

- 本子流程**只提议 + 经 `fab_review` modify 写路径落盘**;不自行改 store `knowledge/`,不手改 store counters(派生态)。
- `related` MUST 只填**真实存在的 stable_id**(先 `fab_recall` 验证目标在库);NEVER 编造 / 指向 pending。
- **稀疏优于稠密**:每次触发上限 5-10 条,只连高置信语义关联;低置信「相邻」不连(信噪比 > 覆盖率)。
- **禁止字面 Jaccard 判据**:不要把 tokenize 重叠 / tag 交集当独立提议信号 —— rc.8 已 retire doctor 侧的纯函数 `suggestRelatedEdges` 字面提议器,理由:字面判据看不见「权限↔访问控制」「pitfall↔规避它的 guideline」这类语义对,给 AI 的候选池反而带字面偏见。词面重叠只能是「值得看一眼」的弱提示,最终必须过 AI 语义判据。
- 反向边按需补,不强制双向(有向语义:A 该带出 B ≠ B 该带出 A)。
- **§4 隐私铁律**:`KT→KP` FORBIDDEN(team 条目 MUST NOT 指向 personal id);不确定目标是否 personal 时 OMIT 该边。
