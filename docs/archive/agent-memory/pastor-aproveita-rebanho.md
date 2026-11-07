---
name: pastor-aproveita-rebanho
description: "0.3.56 FlockHarvest — pastor abate o excedente do rebanho (couro/carne), cozinha sem carvão, recolhe ovo"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T15:06:52.483Z
---

0.3.56 (14-10, Regra 68). Manutenção do ofício do pastor (não toca a obra, Regra 59). Pedido:
"recurso dos outros animais — couro/carne a partir do 5º adulto, recolher ovos, cozinhar, guardar
parte".

**Conciliação importante:** o pastor já CRIA até `FLOCK_TARGET=12` adultos/espécie (`ShepherdFlock`).
Abater acima de 4 (o "5º") brigaria com a criação (ping-pong). Decisão do autor: abater acima de
`HARVEST_KEEP=8` — faixa de colheita 8→12, criação para em 12, abate para em 8, folga de 4.

**`FlockHarvest`** (fabric/work): `surplus(adults)=max(0,adults-8)`, `hasSurplus`; `step(world,
colony, area, chests)` chamado em `ShepherdFlock.tend` (1 ação/ciclo, como `pairOf`). Abate 1 adulto
da 1ª espécie com excedente via `victim.kill()` e deposita o drop Vanilla DIRETO no baú (não a loot
table caindo): vaca 1 couro+2 beef; porco 2 porkchop; ovelha 2 mutton; galinha 1 chicken+1 feather.
`cookSurplusMeat`: carne crua acima de `ITEM_KEEP=16` vira cozida por `CraftingLookup.smelted`
(SEM combustível — o mod já funde sem carvão, SmelterWork diz "transforma sem combustível"; carvão
no mod é recurso do mineiro, não fuel). Ovo: a galinha já larga no chão (ChickenEntity.eggLayTime,
Vanilla) e a coleta do chão recolhe — FlockHarvest não mexe na galinha.

Dois limites distintos: ADULTS vivos (8) protege o rebanho; itens (16) protege o estoque.

APIs 1.21.1 confirmadas: PassiveEntity.isBaby/getBreedingAge; LivingEntity.kill()/damage;
`new Box(BlockPos,BlockPos)` NÃO existe → usar `Box.enclosing(a,b)` (erro de compilação no gametest).
Teste: `FlockHarvestTest` (unidade, mutação HARVEST_KEEP=0 derruba), `FlockHarvestGameTest` (área
restrita à arena por causa da interferência entre gametests). test 1515/0, runGametest 768/768.

JAR 0.3.56 = 9BE8707A… **Troca pendente: o jogo estava aberto com a 0.3.55 (arquivo travado,
"Device or resource busy"); há 0.3.55 E 0.3.56 na pasta de mods — o 0.3.55 só sai quando o jogo
fechar, senão o próximo boot falha (2 mods mesmo id).** Branch `claude/pastor-rebanho`, falta
commit/push/merge e playtest. Relacionado: [[regra-51-tempo-ocioso]],
[[processo-main-e-acoes-continuas]], [[gametest-interferencia-global]].
