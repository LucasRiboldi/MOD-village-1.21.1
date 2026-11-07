---
name: took-zero-e-bau-cheio
description: "took 0" do mineiro mede o que entrou no baú, não o que saiu da pedra; baú cheio fazia a mina ser cavada para sempre
metadata:
  type: project
---

`Miner ... took 0 from <pos>` no log **não** significa que a pedra não dropou.
O número é `haul.stored()` — o que chegou ao baú. Com o baú cheio o drop cai
no chão e o contador marca zero do mesmo jeito.

Medido em jogo (22-09): quarenta e sete linhas de
`Miner chest at ... is full — dropped 1 of minecraft:cobblestone`, com o
mineiro alternando entre as **mesmas duas posições** da boca da mina de
segundo em segundo. De fora, isso se vê como a mina sendo cavada sem fim sem
render nada.

**Why:** eu levantei a hipótese de que o zero vinha da ferramenta —
`Block.getDroppedStacks(..., ItemStack.EMPTY)` calcula o drop com a mão nua, e
pedra de mão nua não dropa. A hipótese era plausível e **estava errada**: a
linha de baú cheio prova que o pedregulho caiu. A mudança foi revertida, e
revertê-la importou por um segundo motivo — passar a picareta real mudaria o
drop caso houvesse Toque Suave, trocando pedregulho por pedra.

**How to apply:** antes de culpar a ferramenta por um `took 0`, procure
`is full — dropped` no mesmo intervalo. E lembre que
`theStoneLeavesTheWorldAndReachesTheChest` passa nos dois estados, então a
bateria **não** distingue as duas causas — foi só o log do jogo que
distinguiu.

O conserto: baú cheio encerra a tarefa em vez de cavar para o chão, coberto
por `MinerGameTest.aFullChestStopsTheMinerInsteadOfPilingStoneOnTheFloor` e
verificado por mutação (desligada a guarda, ele é o único teste que cai).

Ver [[bau-cheio-destroi-o-que-a-obra-espera]] — mesma família de defeito, do
outro lado do baú.
