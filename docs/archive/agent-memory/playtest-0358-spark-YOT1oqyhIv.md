---
name: playtest-0358-spark-yot1oqyhiv
description: "Playtest 0.3.58 (14-10, Spark YOT1oqyhIv, 100min): TPS 20 ok, vila madura sem obra; mineiro 72× NO_STANDING_POSITION e fundidor 42× sem clay são os dois defeitos reais"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T01:33:58.232Z
---

Playtest da **0.3.58** (14-10, Spark `YOT1oqyhIv`, ~100 min, 18:03→19:43).
JAR instalado = `429511EA...` (expansão ágil `FULL_AGAIN_AFTER=FULL_AFTER`).

**TPS 20 estável** (MSPT med 7,7 ms; picos isolados 171 ms), 625 entidades no pico.
Não é performance. Vila **madura e cheia**: 821 cobble, 252 toras, 326 planks, 190 wool.

`time_ledger` (Regra 51 = ocioso+bloqueado+encalhado ≤15%): **todas reprovam** menos
FARMER/SMELTER (que reprovam por ESPERA 58%). BUILDER 64%, MASON 78%, MINER 64%,
SHEPHERD 69%, LUMBERJACK 47%, CARPENTER 29%.

`action_report` separa **ócio por vila-madura** (sem defeito) de **laço de recusa** (defeito):
- **MINER — DEFEITO REAL**: 2 mineiros, 0 tarefas feitas, 40 soltas. 72× TARGET_REJECTED
  NO_STANDING_POSITION + 38× GAVE_UP/"no closer than"/"not moved a block". Alvo COLLECT_STONE
  raw_iron ×19 cada. É o P0 aberto no STATE (o mineiro não alcança/não acha pé). MINED só 0,8/min.
- **SMELTER — DEFEITO REAL**: 101 pegas, 84 soltas (16% conclusão). 42× "no clay in any chest"
  + 42× COLLECT_SURFACE_RESOURCE clay solto + 32× "nothing in the whole radius". Pede clay/terracotta
  que **não existe no bioma** (só 8 CLAY no estoque). É o padrão [[rota-na-receita-nao-e-rota-no-mundo]]
  / [[castigo-por-oficio-vira-rodizio]]: recusa→reinstala sem trocar de tarefa (C4 da varredura).
- **LUMBERJACK**: 46% ocioso mas 0 defeito de laço — 2 lenhadores, só 3 soltas; 23 árvores/aldeão.
  Ócio de vila madura (sem obra pedindo madeira). FELLED_TREE alto confirma que trabalha quando há.
- **BUILDER/MASON/CARPENTER/SHEPHERD**: ócio alto mas conclusão 90-100%, 0 soltas (exceto shepherd 7).
  É **vila madura sem obra**: 52× "no empty plot", 173× "no task open", só 5 "planned minecraft"
  e 11 "opened a build task" em 100 min. 0× ABANDONED, 1× VC_SUPPLY_ERROR. A expansão ágil da 0.3.58
  **não gerou obra suficiente** — ou o terreno em volta não passa na salvaguarda (plano/seco/sem colônia),
  ou a vila atingiu um teto de lote.

**Conclusão:** dois defeitos de produto confirmados (mineiro C1/C4, fundidor C4), e um sinal de que
a expansão ágil não está abrindo obra na vila madura. O ócio das construtoras é consequência da falta
de obra, não defeito próprio. Scripts: `time_ledger.py`, `action_report.py` + parser Spark ad-hoc
(campo 7: TPS/MSPT) em /tmp/spark_tps.py.

**ANÁLISE 2 (leitura das frases do log, 14-10) — refuta a 1ª hipótese do mineiro:**
O defeito dominante do mineiro NÃO é NO_STANDING_POSITION (A1/A3). É **o mineiro não chega à
frente/boca**. Provas no `latest.log`:
- O y do mineiro fica **80–95 quase o tempo todo**; o alvo está em y68–80 (dentro da mina).
  `went one level deeper` aparece **1× em 100 min** ([[frase-ausente-refuta-mecanismo]]: é o termômetro).
- GAVE_UP: 22× "not moved a block in 300 ticks" + "got no closer than N" espalhado de **4 a 115 bl**.
  ~30 desistências a ≥19 bl (longe, não aproxima) vs ~24 a ≤13 bl (perto, trava).
- Assinatura: `walking to the mine mouth at 255,80,1153 ... closest 29,6→29,4` (stall 683/1200) — passa
  o expediente andando para a BOCA e não chega. É o P0 `[[mina-longe-por-vila-grande]]`.
- `MineApproachPath` da 0.3.55 **está rodando** (305 menções) mas **não resolveu** o não-chegar-à-boca.
- Distribuição de "blocks away" em todo o log: linhas espalhadas de 15 a 115 bl — o mineiro quase
  nunca está no REACH 4 onde mineraria.

**Decisão reformulada:** A1/A3 (roomBeside/contornar) consertam o passo 3 de um processo que trava no
passo 1 (chegar à frente). A opção recomendada tem de atacar **a aproximação/chegada à boca**, não a
escolha da pedra. Pendente: escolha do autor entre reformulações (ver conversa 14-10).

Relacionado: [[mina-longe-por-vila-grande]], [[ociosidade-vila-madura]], [[profissoes-paradas-0339]].
