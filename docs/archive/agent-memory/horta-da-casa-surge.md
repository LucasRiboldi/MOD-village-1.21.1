---
name: horta-da-casa-surge
description: 0.3.57 — a horta da casa (farmland/water/wheat) travava a vila; agora surge sem gastar recurso
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T18:10:42.514Z
---

Playtest 0.3.56 (Spark hquYC03AAc, 96 min): a vila parou. Obra `plains_small_house_8` travada nos
últimos ~12 blocos — a **horta** da casa de planície: **35 farmland + 16 wheat + 2 water**. Com a
obra aberta e travada, o planejador não abria outra (143× "assigned 0 tasks (N open)", 226× "still
sweeping") → lenhador 100% ocioso, pedreiro 99%, construtor 75%, pastor 83%, fundidor 0% (191× no
clay). Raiz no log: `The colony could not make minecraft:farmland — no bench or stonecutter recipe,
and the furnace makes none either` + `VC_SUPPLY_ERROR piece=farmland reason=placement failed` +
`retries farmland` 16× em loop. `farmland` não é item fabricável nem coletável.

**Correção 0.3.57 (Regra 69, decisão do autor: "o construtor cria tudo do nada sem gastar"):**
em `BuilderPlacement.placeOne`, ANTES da cadeia de material/`canPlaceAt`, um ramo molda no local os
blocos de chão sem item — `BlockShaping.isShapedFromTheGround` (FARMLAND, WATER, DIRT_PATH,
CropBlock, hasNoItemOfItsOwn, growsByItself) e não `isNeverPlaced` (lava não surge). `groundFor`
garante dirt embaixo de farmland/caminho/cultivo/grass se a posição abaixo é substituível. **Só
sobre lugar livre** (`isReplaceable` || `mayReplaceGround` || `tillsTheGround`) — senão sobrescrevia
bloco posto e quebrava `BuilderGameTest.farmlandDoesNotReplaceAPlacedBlock` (peguei na 1ª rodada).

O ramo `isShapedFromTheGround` JÁ existia mais abaixo em placeOne, mas DEPOIS do `canPlaceAt` que
falhava para farmland no ar — por isso nunca era alcançado. A correção foi movê-lo para cima +
garantir o chão. Removi o ramo antigo (virou morto).

Teste: `GardenSurgesGameTest` (farmland no ar + baú vazio → surge, obra não trava). Mutação
(trocar isShapedFromTheGround por isNeverPlaced) derruba 6 testes de construção — o surgimento de
blocos de chão é essencial a vários. Cuidado: `new Box(BlockPos,BlockPos)` não existe
(`Box.enclosing`); e muitos `--rerun-tasks` seguidos sujam estado estático (16 falsas falhas de
unidade que `gradlew clean test` zerou). test 1515/0, runGametest 769/769. JAR 0.3.57 = 4DB9A471…
Relacionado: [[rota-na-receita-nao-e-rota-no-mundo]], [[obra-esperando-tijolo]],
[[deadlock-no-de-seguranca-sem-enchimento]], [[riscado-nao-e-assentado]].
