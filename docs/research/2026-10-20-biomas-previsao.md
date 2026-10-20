# Previsão por bioma — 2026-10-20

Pedido do autor: o mod precisa funcionar, sem travar nem ficar ocioso, em todos os
biomas. Até hoje só a planície foi jogada (mais um playtest num mundo só de deserto,
P1.3, 2026-09-18, citado em `LotGround.isBiomeGround`).

Este documento junta duas coisas: a **previsão por leitura do código** (o que cada
profissão procura no mundo e o que acontece quando isso falta) e a **bancada de
simulação** que mede o efeito sem jogador. Legenda: **[C]** conferido no código nesta
sessão, **[I]** inferido (do código lido por exploradores, ou do que se sabe do jogo)
e ainda sem prova.

## 1. Onde o mod atende vila

- O jogo gera vila em seis biomas: plains, meadow, desert, savanna, snowy_plains, taiga
  (tags `has_structure/village_*` do 1.21.1). [C]
- `VillageBiomes.WOOD` atende 12: as seis, mais sunflower_plains, snowy_taiga, as duas
  old growth taiga e savanna_plateau. [C]
- O bioma é lido **num ponto só, o centro da colônia** (`VillageScanner.java:118`,
  `ColonyCycleRunner.java:294`, `HousePlans.java:496`, `TreeNursery.java:67`). [C]

**Risco D1, grave.** Uma vila verdadeira cujo centro cai num rio, numa floresta, numa
praia, num grove ou num rio congelado é **ignorada em silêncio**: o aglomerado é
descartado (`VillageScanner.java:126`) e nenhuma colônia nasce. Uma colônia que já
existe e cujo centro desliza para lá não é abandonada (`ColonyAbandonment.java:112`), mas
deixa de acompanhar a vila. Não há linha de log para o descarte. [C para o descarte, I
para a frequência]

**Risco D2.** windswept_savanna, ice_spikes, cherry_grove e grove ficam fora da tabela.
Vila Vanilla não nasce neles, mas a borda de uma vila de savana ou de neve pode passar
por eles. [I]

## 2. Profissão × bioma

| Profissão | Planície | Taiga | Neve | Savana | Deserto | Meadow |
|---|---|---|---|---|---|---|
| Lenhador | OK | OK (pinheiro 2×2) | **ocioso**: árvores raras, viveiro barrado pela neve | risco: copa de acácia torta | lento: só viveiro, sem estoque de reserva | OK, terreno íngreme |
| Fazendeiro | OK | parcial: podzol recusado | **bloqueio** para roça nova | parcial: coarse dirt recusado | **bloqueio** para roça nova | OK |
| Pastor | OK com ovelha | depende do spawn | ocioso, lã fabricada | depende do spawn | ocioso, lã fabricada | OK |
| Mineiro | OK | OK | OK | OK | gargalo: arenito sob a areia | altura (playtests 0.3.63/64) |
| Pedreiro | OK | OK | OK | OK | gargalo: arenito cortado depende do mineiro | OK |
| Fundidor | argila de lago | idem | gelo e neve OK; vidro só com areia na água | idem | areia OK; argila fabricada (KnownAbsent) | idem |
| Construtor | OK | OK | OK | platô: degrau reprova lote | casa de arenito OK; madeira fabricada | **sem lote plano** |
| Carpinteiro | OK | OK | em cadeia com o lenhador | OK | tábua só pelo viveiro | OK |

### Conferido nesta sessão

- **F1 — roça nova na neve e no deserto.** `FarmerTill.fits` (`FarmerTill.java:124-125`)
  aceita só `GRASS_BLOCK` ou `DIRT` **com ar em cima**. Na planície nevada a camada de
  neve (`Blocks.SNOW`) cobre o chão, e no deserto não há grama nem terra. Água conta só
  como fluido (`:133`): gelo, que é o que a água vira no frio, não serve. [C]
- **F2 — viveiro do lenhador na neve.** `TreeNursery.isSpotForANursery` exige ar em cima
  (`TreeNursery.java:113`): camada de neve reprova o lugar. Na neve, onde árvore já é
  rara, o lenhador também não planta. [C]
- **F3 — chão de lote.** A camada de neve conta como "nada" (`LotGround.isNothing`,
  substituível) e o lote olha o chão sólido embaixo: não trava. Bloco de neve, gelo
  compactado, areia, arenito e terracota contam como chão do bioma
  (`LotGround.java:209-225`). Gelo comum e gelo azul não contam. [C]
- **F4 — rua.** A rua do mod é sempre `dirt_path` (`VillageRoad.java:28-40`), e
  `isPaving`/`RoadIndex.isRoadArea` não reconhecem a rua de arenito liso da vila do
  deserto. Como arenito liso conta como chão do bioma (F3), um lote pode ser planejado
  sobre a rua original. [C para os predicados, I para o efeito]

### Inferido, a provar na simulação

- Pastor: o mod nunca cria animal, só junta os que já existem (`ShepherdHerding`).
  Planície nevada e deserto quase não têm ovelha nativa; a lã sai do estoque fabricado
  (`EmptyFlock` → `LocateFallback`). A vila "funciona", mas o pastor fica ocioso.
- Lenhador e mineiro **não têm** estoque de reserva: onde o mundo não entrega, eles ficam
  ociosos de verdade. Pastor, fundidor e coleta de superfície têm.
- Acácia: a copa precisa ficar a até 6 blocos do tronco (`TreeHarvester`); a acácia
  torta pode virar recusa repetida.
- Rota `FARMED` vale em todo bioma (`BiomeConstructionSupply.java:447`): o planejador
  conta com trigo mesmo onde não há roça possível.

## 3. Terreno e locomoção

- Perigos de bloco (fogo, magma, cacto, arbusto de baga, teia, neve em pó, rosa do wither)
  viram barreira no grafo de navegação (`WorldTerrain.java:93-101`). [I]
- `ClimbTerrain.isOpen` olha só colisão e lava (`ClimbTerrain.java:116-120`): arbusto de
  baga e neve em pó não têm colisão, e a fuga do encalhado pode atravessá-los. [I]
- Nenhum código trata clima: neve que cai, água que congela. [I, grep vazio]
- Rio largo é barreira, sem travessia. [I]
- Regra 19 com tolerância zero por coluna (`LotLevel.java:82`): duna, platô e encosta
  reprovam lote. É a mesma raiz da ociosidade da vila montanhosa (terraplanagem, plano
  guardado). [C para a constante]

## 4. A bancada de simulação

`python scripts/biome_sim.py <alvo> [--days N]` (lista: `--list`):

1. sobe `gradlew runBiomeSim`: servidor sem tela com `-Dvillagecolony.simulate=true`;
2. acha a vila (`/locate structure`), ou põe uma (`/place structure`, que no 1.21.1 ignora
   o bioma: o predicado é sempre verdadeiro, conferido por `javap`);
3. `/forceload` de 13×13 chunks em volta: chunk forçado tem tique de entidade e tique
   aleatório, então aldeão anda e lavoura cresce;
4. `SimulatedObserver`: o centro dos chunks forçados conta como um jogador dentro da vila
   (descoberta, foco, ciclo, varredura). Só liga com a propriedade **e** em ambiente de
   desenvolvimento: o JAR do jogador nunca o liga;
5. `/tick sprint` pelos dias pedidos; depois `time_ledger`, `cost_ledger`, `action_report`
   e a contagem de sinais do log, em `run/biome-sim/reports/<alvo>/`.

Modos: `natural` (mundo normal, semente fixa — borda e relevo reais) e `fixed` (pacote de
dados com o bioma inteiro fixo — mede recurso e chão, mas o relevo continua o do ruído
normal, então não mede a montanha da meadow).

**Limites:** sem jogador perto não nasce monstro (não há ataque nem saque); fora dos
chunks forçados nada simula.

## 5. Resultados

### Rodada 1 — 2026-10-20, 0.3.68 + chave, semente 20261020, 1 dia por vila

| Vila (natural) | Aldeões | Colônia | O que a varredura viu (`VC_SIM`) |
|---|---|---|---|
| planície (-496, -336) | 9 → 16 | nasceu | 1 aglomerado, nenhuma recusa |
| savana (272, 224) | 7 → 14 | nasceu | uma casa de 1 cama a ~60 blocos ficou de fora (`TOO_FEW_BEDS`) |
| deserto (-2480, 4656) | 7 → 7 | **não** | 3 aglomerados de 1, 2 e 2 camas, todos `TOO_FEW_BEDS` |
| taiga (336, -2608) | 6 → 6 | **não** | 2 recusas `TOO_FEW_BEDS`; o 3º, de 3 camas, ignorado: centro em `cold_ocean` |
| neve (1984, -2688) | 14 → 14 | **não** | a vila inteira, 19 camas, ignorada: centro em `forest` |

**Três das cinco vilas Vanilla nunca viram colônia.** As duas causas, provadas pela
própria varredura:

- **D3 — vila espalhada se parte em aglomerados pequenos.** Duas camas só se juntam a
  até 32 blocos (`VillageDetector.CLUSTER_DISTANCE`). Vila de deserto, savana e taiga
  tem casas de 1 ou 2 camas, longe umas das outras; cada pedaço fica abaixo de 3 camas.
- **D1 confirmado — o bioma lido num ponto só.** O centro é a cama mais perto da média
  das camas. Numa vila de neve, ela estava num pedaço de floresta; numa de taiga, à
  beira de um oceano frio. A vila é descartada sem nenhuma linha no log de jogo.

Tempo (1 dia é pouco — 3 janelas de 5 min; sinal, não prova): na planície, construtor
85% bloqueado e pedreiro 71% ocioso; na savana, lenhador 79% ocioso e pastor 48%
bloqueado. Mineiro e fundidor ficam acima de 50% de trabalho nas duas.

Desempenho: sem colônia, 1.130–1.250 tiques/s; 1 dia de jogo em ~20 s.

### Rodada 2 — 0.3.69 (ADR-003 Emenda 9: junção pela vila gerada, estilo pela vila gerada)

| Vila | Varredura | Colônia |
|---|---|---|
| planície | 1 aglomerado | nasceu, 9 camas |
| savana | 1 aglomerado (a casa de 1 cama entrou) | nasceu, 6 camas |
| neve | 1 aglomerado | **nasceu, 19 camas** (antes ignorada) |
| deserto | 1 aglomerado, nenhuma recusa | **não**: 40× "Deferred colony adoption ... until BigHouseMOD has a safe lot" |
| taiga | 1 aglomerado, nenhuma recusa | **não**: idem |

- **D4 — a casa fundadora não acha lote.** A colônia nasce junto com a BigHouseMOD
  (7×11), que exige 77 colunas exatamente na mesma altura, fora das peças da vila e a até
  32 blocos do centro (`BigHouseFoundation.findSafeOrigin`/`flatFloor`). Na duna e na
  encosta não há. Decisão do autor: procurar mais longe e, sem lote plano exato, aplainar
  o mais nivelado (desnível até 2).

## 6. Rodadas

Cada rodada: `run/biome-sim/reports/<alvo>/` (log, `signals.txt`, `time_ledger.txt`,
`cost_ledger.txt`, `action_report.txt`). O diagnóstico `VC_SIM` só existe com a chave.
