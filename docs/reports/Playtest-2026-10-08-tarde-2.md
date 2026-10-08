# Playtest 2026-10-08, 15:14–15:53 (JAR 0.3.16) — Spark `3Oy2cLrBoT`

Sem crash. Spark: TPS 20 (mín. 13,4), 12,0 ms/tique em média, pico de 86 ms; **mod 2,5%** da thread
(`VillageDetectionHandler.onEndTick` 1,98%; `ReachMap` 0,01%).

## `time_ledger.py`

| profissão | work | walk | wait | blocked | idle |
|---|---|---|---|---|---|
| BUILDER | 0% | 3% | 0% | 14% | 82% |
| CARPENTER | 11% | 28% | 52% | 1% | 8% |
| FARMER | 0% | 42% | 39% | 0% | 19% |
| LUMBERJACK | 6% | 44% | 4% | 31% | 15% |
| MASON | 11% | 28% | 51% | 1% | 9% |
| MINER | 0% | 57% | 35% | 0% | 9% |
| SHEPHERD | 1% | 20% | 18% | 0% | 61% |
| SMELTER | 1% | 1% | 0% | 0% | 98% |

O `time_ledger` agrupa pelo ofício de agora: o construtor que pôs os blocos (9353ad2c) aparece no
diário como SMELTER — a profissão dele foi trocada durante a sessão; a obra seguiu com ele.

## `action_report.py` (35,7 min)

| profissão | pegas | feitas | soltas | conclusão | ações/min |
|---|---|---|---|---|---|
| CARPENTER | 141 | 138 | 0 | 98% | CRAFTED 9,1 |
| MASON | 95 | 93 | 0 | 98% | CRAFTED 6,4 |
| LUMBERJACK | 7 | 0 | 6 | 0% | FELLED_TREE 8,0 |
| MINER | 6 | 0 | 3 | 0% | — (0 pedras) |
| SHEPHERD | 29 | 0 | 20 | 0% | SHEARED 1,1 |
| FARMER | 7 | 0 | 5 | 0% | HARVESTED 1,5 |
| SMELTER (o construtor) | 2 | 2 | 0 | 100% | PLACED 1,5 |

Motivos: MINER 3× "walked 2400 ticks without arriving"; LUMBERJACK 1× STRANDED.

## O que funcionou (pedidos de 0.3.16)
- **Bosque:** três mudas a -785/-779, 67, -892/-898 (grove 1–3 de 3).
- **Pastor ajudante:** entrou na obra duas vezes (211 e 139 peças restantes) e saiu para a lã.
- **Feno:** estocado no baú da obra peça a peça (52) — a obra seguiu.
- **Obra:** de 429 para 99 peças em 12 min.

## Corrigido (0.3.17)
1. **Mineiro descendo e voltando:** a descida já funcionava (superfície → nível 1 → nível 2), mas
   quem passava de uma entrada (3 abaixo, 4 ao lado) era puxado de volta a ela, e aldeão sobe um; e a
   descida inteira estourava os 2.400 tiques. Agora, na escada, a próxima entrada é a primeira abaixo
   dele; "na escada" é logo abaixo da boca ou debaixo da terra perto de uma entrada (na superfície em
   cima da mina, vai à boca); na descida o orçamento de caminhada não corre.
2. **Frase do feno estocado:** dizia "no recipe"; agora "no route delivered it".

## Visto e não corrigido
- **Obra parada 20 min nas últimas 99 peças:** todas são lampião e corrente (ferro). O gargalo é o
  mineiro, corrigido acima; se ainda faltar ferro, decidir se lampião e corrente entram na lista do
  feno.
- **Lenhador 31% blocked** (as árvores do bosque ainda são mudas).
- **Pastor:** a ajuda dura pouco, porque a lã volta à fila logo.
- `GroundPickupGameTest` caiu nas três últimas rodadas de mutação e em nenhuma bateria limpa.
