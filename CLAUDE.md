## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost). **It costs the curated community names:** `update` re-clusters, the community ids shift, and the 169 hand-written labels are replaced by hub-derived ones (`TaskService` instead of `Colony Cycle Orchestration`). It backs the old graph up to `graphify-out/<date>/` first — and overwrites that directory if run twice in one day, so copy it aside before a second run.

  Two ways back, and they trade different things:

  1. **Restore the backup** — keeps the curated names, loses the new code. `cp` of `graph.json`, `GRAPH_REPORT.md`, `manifest.json`, `cost.json` and `.graphify_labels.json`, then delete `.graphify_labels.json.sig` (it belongs to the labels you just replaced), rewrite `.graphify_root` with the absolute path (`update` sets it to `.`), and re-run `graphify export html`.
  2. **Carry the names onto the new graph** — keeps both. For each new community, take the curated `community_name` that the majority of its nodes carried in the previous `graph.json`; hand-name only the communities with no clear majority. Done 2026-09-02 against the 3,167-node curated graph: **145 of 158 mapped at ≥50% overlap**, 13 needed a name. Where one curated name won two communities, the larger overlap keeps it and the other gets its hub appended. Then write `community_name` into `graph.json` and `.graphify_labels.json`, and regenerate `GRAPH_REPORT.md` (`graphify.report.generate`) and `graph.html`.

  Do **not** expect `graphify label` to do this. With no LLM backend configured — there is no `GEMINI_API_KEY`/`GOOGLE_API_KEY` here — it prints `no LLM backend configured; keeping Community N placeholders`, does nothing useful, and still overwrites `graphify-out/<date>/`. The agent doing the labeling *is* the LLM; option 2 is that job.

  Re-clustering also moves nodes between communities, so a name can end up sitting oddly on a node you remember elsewhere. `ColonyPos` left `Geometry Helpers` for `Colony Center and Observation`, because 12 of that old community's 20 nodes went there; the 8 that stayed are now `ColonyPos Distance`. The name follows the majority of the members, not any one node.
- `.graphifyignore` at the root excludes `.claude/`, and it has to. The `.gitignore` reopens `.claude/skills/` so the skills are versioned, graphify reads `.gitignore`, and without the exclusion `update` indexes the 158 skill markdowns and adds ~2.2k heading nodes to the mod's graph — measured 2026-09-02: 3,167 to 5,422 nodes, which pushed `graph.html` past the 5,000 limit into aggregated community view. `.graphifyignore` is read after `.gitignore` and can only ever exclude more, never re-include.
