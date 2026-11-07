---
name: graphify-update-infla-grafo
description: "`graphify update .` re-inclui .claude/skills/ e soma ~2.2k nós; a exclusão do build inicial não persiste"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-02T12:07:33.245Z
---

O build completo via `/graphify .` produziu 3.167 nós / 169 comunidades com
rótulos escritos à mão — e só depois de o `.claude/` ter sido **excluído a
mão**, na pergunta que a skill faz no começo. Rodar `graphify update .`
devolve **5.422 nós e 335 comunidades**, com os rótulos trocados por nomes
derivados do hub.

**Why:** a exclusão do `.claude/` não persiste em lugar nenhum. Ela vale só
para aquela execução do build; o `update` re-detecta a árvore inteira por
conta própria. Medido em 2026-09-02 pelo diff dos conjuntos de nós: dos
**2.256 nós somados, 2.243 vêm de `.claude/skills/`** (títulos de markdown
das definições de skill), 10 de `src/test` (código novo, legítimo) e 2 de
`CLAUDE.md`. O `update` re-extraiu só 5 arquivos e ainda assim inflou — o
volume vem da re-detecção, não da re-extração.

Passar de 5.000 nós faz o `graph.html` cair para visão agregada por
comunidade e perder o detalhe por nó (`--obsidian` ainda dá o detalhe).

**Hipótese que testei e caiu:** `.graphify_root` reescrito de caminho
absoluto para `.` pelo `update`. Ele *é* reescrito, mas rodar o `update`
com a raiz absoluta restaurada deu **exatamente o mesmo** 5.422/335. A raiz
não é a causa. (Se um id malformado tipo
`..._villagedetectionhandler_java_scanresult` reaparecer, é outra coisa.)

**How to apply — a inflação já está corrigida** no commit `819ff7b`: existe
um `.graphifyignore` na raiz com `.claude/`. Ele é necessário porque o
`.gitignore` daqui **reabre** `.claude/skills/` (`.claude/*` +
`!.claude/skills/`) para versionar as skills, e o graphify lê o
`.gitignore`; o `.graphifyignore` é lido depois e só exclui mais. Verificado:
com ele no lugar o `update` deu **3.179 nós** em vez de 5.422.

**O `update` ainda custa os nomes curados** — ele re-clusteriza, os ids de
comunidade mudam, e os 169 rótulos à mão viram nomes de hub. O
`.graphifyignore` resolveu a inflação, não isto.

**`graphify label` não resolve aqui.** Sem `GEMINI_API_KEY`/`GOOGLE_API_KEY`
ele imprime `no LLM backend configured; keeping Community N placeholders`,
não faz nada útil, e ainda sobrescreve o backup em `graphify-out/<data>/`.

**O que funciona: transportar os nomes por sobreposição de nós.** Para cada
comunidade nova, pegue o `community_name` curado que a maioria dos seus nós
carregava no `graph.json` anterior; nomeie à mão só as sem maioria clara.
Medido em 2026-09-02: **145 de 158 mapearam com ≥50%**, 13 precisaram de
nome. Quando um nome curado ganha duas comunidades, a de maior sobreposição
fica com ele e a outra recebe o hub como qualificador. Depois grave
`community_name` no `graph.json` e no `.graphify_labels.json`, e regenere
`GRAPH_REPORT.md` (`graphify.report.generate`) e o `graph.html`. É
determinístico e não custa LLM nenhum — o agente que rotula É o LLM.

A re-clusterização também move nós de comunidade: `ColonyPos` saiu de
`Geometry Helpers` para `Colony Center and Observation` (12 dos 20 nós
antigos foram junto), e os 8 que ficaram viraram `ColonyPos Distance`. O
nome segue a maioria dos membros, não um nó específico.

Restauração testada (o `update` faz backup em `graphify-out/<data>/`, e
sobrescreve esse diretório se rodar duas vezes no mesmo dia — copie para
outro nome antes):

```
cp graphify-out/<backup>/{graph.json,GRAPH_REPORT.md,manifest.json,cost.json,.graphify_labels.json} graphify-out/
rm -f graphify-out/.graphify_labels.json.sig     # pertence aos rótulos novos
python -c "import os;open('graphify-out/.graphify_root','w').write(os.path.abspath('.'))"
rm -f graphify-out/graph.html && python -m graphify export html
```

`graphify-out/` é gitignorado desde o commit `faa39c0`, então nada disso
entra no histórico. Ver [[village-colony-build-env]].
