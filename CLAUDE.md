## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost). **It costs the curated community names:** `update` re-clusters, the community ids shift, and the 169 hand-written labels are replaced by hub-derived ones (`TaskService` instead of `Colony Cycle Orchestration`). It backs the old graph up to `graphify-out/<date>/` first — and overwrites that directory if run twice in one day, so copy it aside before a second run. Restoring is `cp` of `graph.json`, `GRAPH_REPORT.md`, `manifest.json`, `cost.json` and `.graphify_labels.json`, then delete `.graphify_labels.json.sig`, rewrite `.graphify_root` with the absolute path, and re-run `graphify export html`.
- `.graphifyignore` at the root excludes `.claude/`, and it has to. The `.gitignore` reopens `.claude/skills/` so the skills are versioned, graphify reads `.gitignore`, and without the exclusion `update` indexes the 158 skill markdowns and adds ~2.2k heading nodes to the mod's graph — measured 2026-09-02: 3,167 to 5,422 nodes, which pushed `graph.html` past the 5,000 limit into aggregated community view. `.graphifyignore` is read after `.gitignore` and can only ever exclude more, never re-include.
