---
name: processo-main-e-acoes-continuas
description: "14-10 — branch integrada na main e STATE enxugado; a frente \"ações contínuas\" esbarra na Regra 59"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T13:11:47.796Z
---

**Processo (14-10):** a branch `claude/autossuficiencia` (74 commits, 0.3.25→0.3.55) foi integrada
na `main` com merge --no-ff (`bb6e7705`), build+test 1512/0 antes, push feito. Corrige o erro B1
da varredura (branch gigante sem merge). Daqui: **branch curta por ciclo** a partir da main, PR
curto. STATE enxugado de 315→105 linhas (blocos de versão movidos para `Historico-2026-10.md`);
P0 reescrito para 14-10 (ações contínuas, mina como raiz). Varredura em
`docs/audit/2026-10-14-varredura-engenharia.md`.

**Frente "ações contínuas / profissões ininterruptas" — BLOQUEADA por decisão do autor:**
A cadeia da Regra 51 (ofício → manutenção → ajudar a obra → recolher do chão) tem o passo 3
("ajudar a obra") restrito: `BuildHelper.HELPS = {CARPENTER, MASON}` e a **Regra 59 (13-10) diz
explicitamente que fazendeiro e pastor NÃO ajudam a obra** (revogou a emenda da 51 que os deixava).
Então o A3 do doc de ócio ("generalizar o BuildHelper para todo ofício ocioso") **conflita com a
Regra 59** — não dá para aplicar sem reverter decisão do autor. E a **manutenção do ofício** (passo
2) já está em boa parte feita: `SmelterAdvance` (fundidor adianta cru), `OutskirtsPlanting`
(lenhador planta mudas), `ShepherdFlock/Herding`, `FarmerWork` (planta canteiro), `FarmerBakery`.

O laço improdutivo que sobra (pastor "no sheep with wool" 9-25×, fundidor sem cru, fazendeiro sem
lote) é `ShepherdWork.release` etc. largando a tarefa sem recuar — mas o recuo "ajudar a obra" é
proibido para eles (Regra 59).

**Autor escolheu (14-10): "mais manutenção do ofício, sem tocar a obra".** Investiguei o que já
existe e CONCLUÍ que a manutenção já é rica: pastor `ShepherdHerding.plan` (leva animal solto ao
curral) + `ShepherdFlock.tend`; fundidor `SmelterAdvance` (adianta cru, tetos KEEP=16/MINE_FILL=64);
fazendeiro planta canteiro (`FarmerWork`). O ócio residual NÃO é falta de manutenção — é a
manutenção **esgotar** (rebanho cheio, sem animal solto, cru acabou, sem canteiro vazio) e aí não
haver mais o que fazer sem ajudar a obra (vetado). **Decisão de engenharia: não adicionar manutenção
especulativa** (retorno decrescente, contra "simplificar"); o ganho real é (a) a mina (P0 medido,
A1/C1) e (b) MEDIR num playtest de dia onde cada manutenção esgota — "o log decide a constante",
não o palpite. Não implementei nada nesta frente de propósito. Relacionado:
[[regra-51-tempo-ocioso]], [[regras-0339-bau-publico-borda]], [[log-decide-a-constante]].
