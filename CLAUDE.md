## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost). **It costs the curated community names:** `update` re-clusters, the community ids shift, and the 169 hand-written labels are replaced by hub-derived ones (`TaskService` instead of `Colony Cycle Orchestration`). It backs the old graph up to `graphify-out/<date>/` first — and overwrites that directory if run twice in one day, so copy it aside before a second run.

  Two ways back, and they trade different things:

  1. **Restore the backup** — keeps the curated names, loses the new code. `cp` of `graph.json`, `GRAPH_REPORT.md`, `manifest.json`, `cost.json` and `.graphify_labels.json`, then delete `.graphify_labels.json.sig` (it belongs to the labels you just replaced), rewrite `.graphify_root` with the absolute path (`update` sets it to `.`), and re-run `graphify export html`.
  2. **Carry the names onto the new graph** — keeps both, and this is the one to reach for. `scripts/graphify_relabel.py` does it: for each new community it takes the curated `community_name` that the majority of its nodes carried in the previous `graph.json`, then rewrites `graph.json`, `.graphify_labels.json`, `GRAPH_REPORT.md` and `graph.html`. Measured 2026-09-02 against the 3,167-node curated graph: **143 of 158 decided by vote**, the rest by `scripts/community_names.json`.

     ```
     python scripts/graphify_relabel.py --reference graphify-out/<backup>/graph.json --dry-run
     python scripts/graphify_relabel.py --reference graphify-out/<backup>/graph.json --overrides scripts/community_names.json
     ```

     Run `--dry-run` first: it lists the communities with no clear majority, with sample members, which is exactly what you need to add a name to `scripts/community_names.json`. That file is keyed by **node id, not community id** — node ids are deterministic and survive re-clustering, community ids do not, so a number-keyed override would silently name the wrong community after the next `update`.

     The script's decision logic is covered by `python -m unittest discover -s tests` (28 cases, stdlib only, no pytest needed). It is **not** wired into `./gradlew build` on purpose: this is a Java project, and making the build require a Python interpreter would break it on machines that do not have one, for a maintenance utility that never ships with the mod. Run it by hand when you touch the script.

  Do **not** expect `graphify label` to do this. With no LLM backend configured — there is no `GEMINI_API_KEY`/`GOOGLE_API_KEY` here — it prints `no LLM backend configured; keeping Community N placeholders`, does nothing useful, and still overwrites `graphify-out/<date>/`. The agent doing the labeling *is* the LLM; option 2 is that job.

  Re-clustering also moves nodes between communities, so a name can end up sitting oddly on a node you remember elsewhere. `ColonyPos` left `Geometry Helpers` for `Colony Center and Observation`, because 12 of that old community's 20 nodes went there; the 8 that stayed are now `ColonyPos Distance`. The name follows the majority of the members, not any one node.
- `.graphifyignore` at the root excludes `.claude/`, and it has to. The `.gitignore` reopens `.claude/skills/` so the skills are versioned, graphify reads `.gitignore`, and without the exclusion `update` indexes the 158 skill markdowns and adds ~2.2k heading nodes to the mod's graph — measured 2026-09-02: 3,167 to 5,422 nodes, which pushed `graph.html` past the 5,000 limit into aggregated community view. `.graphifyignore` is read after `.gitignore` and can only ever exclude more, never re-include.

## As skills do projeto: ofereça, não decida sozinho

Este projeto carrega quatro skills próprias em `.claude/skills/`. Elas são
versionadas de propósito — são o conhecimento acumulado do projeto, e não
configuração de máquina.

| skill | quando ela é a ferramenta certa |
|---|---|
| `fabric-development` | escrever ou alterar código de mod: registro, Mixin, networking, persistência, datagen, gametest, crash, lag, bug que só aparece em servidor |
| `minecraft-villager-systems` | qualquer coisa que toque aldeão ou vila: Brain, Memory, Sensor, Activity, POI, profissão, local de trabalho, Schedule, trades, reprodução, aldeão parado |
| `minecraft-code-research` | investigar como o Minecraft faz alguma coisa antes de imitá-la — precede as duas de cima |
| `graphify` | pergunta sobre a base: onde está, como se liga, o que depende do quê |

**A regra: quando você perceber que uma delas ajudaria na tarefa em mãos,
diga isso e ofereça rodá-la — antes de sair fazendo à mão.** Uma linha
basta: qual skill, o que ela acrescentaria aqui, e a pergunta. Quem
decide é o autor.

Ela vale inclusive — e principalmente — quando a tarefa parece que dá
para tocar direto. O caso que a motivou é justamente esse: sai mais caro
descobrir no meio do trabalho que a skill teria dado o caminho pronto do
que gastar uma linha perguntando antes.

**O que a regra não é.** Não é pedir permissão para trabalhar, e não é
oferecer skill em toda mensagem. Se nenhuma se aplica, siga sem citar
nenhuma; se o autor já disse que não quer, não repita a oferta no mesmo
assunto. E oferecer não é esperar de braços cruzados: o que não depende
da resposta continua andando.
