---
name: playtest-0337-baus-publicos
description: Playtest 0.3.37 (Spark cKZHDMdNUm): mina voltou, grama faltando, 267 toras no chão, postes; 0.3.38 (5ab7efb0) corrige; JAR B2ED23E5 não jogado
metadata:
  type: project
---

O playtest da 0.3.37 (12-10, 9 min) mostrou que as correções anteriores funcionaram: a mina minerou 180 blocos e a bancada de flecheiro saiu em 2 min (o sílex apareceu no baú).

A 0.3.38 corrige o que o autor apontou nesse playtest:
- Grama e relva alta surgem sozinhas (`GrowingPieces`, chamado em `reconsiderDeferredPieces`).
- O excedente vai para os baús públicos (`PublicChests` = `VillageChests.around` menos a boca da mina).
- `TreeFelling.finishTheTrunk` acaba a árvore deixada pela metade.
- Um poste por casa (`ShepherdPens.hasLonePostNear`).

**Armadilha da bateria:** acrescentar testes muda a posição das arenas. A colônia que a detecção funda na bateria (caixa de 145×145) passou a cobrir a arena do `MineReturnGameTest`, que falhou 3 vezes seguidas. Se um teste antigo começar a falhar em todas as rodadas, imprimir as colônias que cobrem a posição antes de suspeitar do código.

**Why:** com o baú do ofício cheio, o produto caía no chão ou era descartado. O autor quer que madeira e pedra crua passem do limite e fiquem guardadas.

**How to apply:**
- Sem baú público na vila, o excedente ainda cai no chão; decisão pendente no TODO.
- Ficou aberto: 383 recusas `TOO_HIGH` de um mineiro.
- Ver [[playtest-0336-e-expansao]] e [[gametest-interferencia-global]].
