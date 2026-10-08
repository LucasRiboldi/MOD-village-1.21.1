# Playtest 2026-10-08, 18:19–19:10 (JAR 0.3.19) — Spark `D66XrVOZl4`

Sem crash. Spark: TPS 20 em 49 das 51 janelas (a de 9,0 repete a anterior e tem MSPT máx. 47 ms:
pausa), MSPT mediano 9,5–15 ms, picos de 370 e 660 ms só no carregamento. **Mod 1,4%** da thread
(`VillageDetectionHandler.onEndTick` 0,93%). `cost_ledger`: ciclo 29 ms (p95 36), planejador 19,5 ms.

## `time_ledger.py`

| profissão | work | walk | wait | blocked | idle | veredito |
|---|---|---|---|---|---|---|
| BUILDER | 1% | 18% | 0% | 0% | 81% | Regra 51 |
| CARPENTER | 10% | 25% | 56% | 1% | 8% | espera 56% |
| FARMER | 1% | 33% | 50% | 0% | 16% | Regra 51, espera |
| LUMBERJACK | 6% | 56% | 0% | 6% | 32% | Regra 51 |
| MASON | 11% | 22% | 50% | 1% | 17% | Regra 51, espera |
| MINER | 17% | 38% | 21% | 3% | 20% | Regra 51 |
| SHEPHERD | 0% | 16% | 15% | 2% | 66% | Regra 51 |
| SMELTER | 0% | 0% | 0% | 6% | 93% | Regra 51 |

## `action_report.py` (48,7 min)

| profissão | aldeões | pegas | feitas | soltas | cancel. | ações por minuto |
|---|---|---|---|---|---|---|
| BUILDER | 2 | 1 | 1 | 0 | 0 | PAVED 1.4, SET_ASIDE 0.1 |
| CARPENTER | 3 | 166 | 163 | 0 | 0 | CRAFTED 8.5 |
| FARMER | 2 | 6 | 0 | 4 | 0 | HARVESTED 1.6 |
| LUMBERJACK | 5 | 15 | 0 | 14 | 0 | FELLED_TREE 6.6 |
| MASON | 2 | 106 | 105 | 0 | 0 | CRAFTED 6.1 |
| MINER | 3 | 13 | 2 | 11 | 0 | MINED 23.4, GAVE_UP 0.2 |
| SHEPHERD | 3 | 47 | 1 | 35 | 11 | SHEARED 0.3 |
| SMELTER | 3 | 26 | 6 | 20 | 0 | SMELTED 0.1 |

Recusas: construtor 6× `SET_ASIDE no material` (funil ×3, trilho ×2, vaso); mineiro 7× `TOO_HIGH`,
6× `got no closer`, 5× `not moved`; lenhador 2× e fazendeiro 1× `STRANDED`.

## O que a 0.3.19 entregou

- Construtor calçou 64 blocos com a obra parada (`paved the path … while the build waits`).
- Lampião e corrente apareceram no baú depois de quatro faltas (18:21–18:23).
- Bosque: `planted … (grove k of 6)` seis vezes. Pastor ajudante pôs corrente na obra.

## Erros, por ordem de impacto

### 1. Celeiro parado 40 min num funil — **corrigido** (lista do feno)

`barn_majest` andou de 376 para 33 peças em 8 min e parou: `waiting for minecraft:hopper` de 18:27
a 19:09. O funil tem receita de bancada, então nunca contava falta: esperava 5 lingotes. O mineiro
731d73b0 ficou 37 min com `wants raw_iron, 0 of 21` e cavou 571 blocos. **Medido no save:** dos 1.136
blocos que os mineiros cavaram, nenhum tinha minério de ferro colado que tenha ficado na parede, e
só 3 a dois blocos — a escada não cruza ferro, embora a área da mina tenha 977 minérios de ferro.
Não é falha de detecção; é a forma da mina (ver pendência abaixo).

Correção: funil, trilho e vaso entram na lista de peças que aparecem no baú depois das faltas, como
lampião e corrente. `theHopperAppearsInTheBuildersChestAfterFourMisses`.

### 2. Celeiro um bloco acima do chão; lenhadores encalham nele — **corrigido**

A planta tem 5 peças na camada 0 (funis e feno debaixo do piso) e 85 na 1. Sem porta e sem encaixe,
não tinha camada da rua, e a camada 0 ia "um acima do chão": no save, grama em y 73, ar em y 74 e as
paredes desde y 75, com alçapões na altura da cabeça. Os dois `STRANDED` de lenhador (−699 74 −792 e
−701 74 −780) são na beira dele, e o `not moved` do lenhador marcou como inalcançáveis árvores a 129
blocos (−779 68 −894, três lenhadores, 34 min, nenhuma derrubada).

Correção: `StreetLayer.sunkenBase` — sem porta nem encaixe, camada 0 com menos da metade das peças da
camada 1 é subterrânea, e a rua fica nela. Das quatro plantas do mod, só o celeiro muda.
`StreetLayerTest`, `theBarnStandsOnTheGroundAndNotOneAboveIt`. **O celeiro já aberto no save mantém a
origem gravada** e continua suspenso.

### 3. Pastor não acha o rebanho — **corrigido**

`found no sheep with wool within 32 blocks` o tempo todo, com `the flock is full (16 sheep)`. O
rebanho é contado na caixa da vila + 10; a tosquia procurava só ±32 do centro (−717, −842), e o
curral fica em −728, −799, 43 blocos ao norte. No save, 10 das 14 ovelhas tinham lã. Correção:
`ShepherdFlock.shearingArea` — a busca é o raio somado à área do rebanho. `ShepherdShearingAreaTest`.

### 4. Lenhador com 0% de conclusão — não é defeito do ofício

A tarefa de tora é o trabalho contínuo (768 toras): nunca termina, só é solta. As 14 solturas são
`WORK_STALLED`; árvore mediana a 76 blocos (p90 127). O encalhe é o item 2.

### 5. Fazendeiro e fundidor — consequência

Fazendeiro: `nothing ripe and no empty plot` (roça pequena para dois). Fundidor: sem minério (item 1).

## Pendências

- Mina que acha ferro: a escada não cruza minério; ramal até o minério visível por perto, ou galeria
  de faixas. Decisão de desenho.
- Mineiro cc99a53b de novo na pedra −751 31 −942 (item 2 do relatório da manhã, aberto).
- Lenhador que não sai do lugar pune a árvore (`refused N times`): o castigo devia ir ao aldeão.
- A1 (espera do fabricante, 50–56% no carpinteiro e no pedreiro) segue à espera de escolha.
- ADR-036 item 6: a obra inteira para por uma peça em resolução.
