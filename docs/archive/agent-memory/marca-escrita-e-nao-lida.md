---
name: marca-escrita-e-nao-lida
description: "A mesma pedra/árvore servida de novo com castigo ativo é furo de porta, não de marca — confira todos os caminhos que servem alvo"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-15T23:08:38.568Z
---

Quando um alvo recusado volta a ser servido **com o castigo ainda
correndo**, o defeito quase nunca é a marca: é um caminho de escolha de
alvo que não a consulta.

No E44 de 2026-09-15 o log dizia `refused 1 times now, skips it for 6000
ticks` e a mesma pedra voltava 1.200 tiques depois. A marca estava certa;
`MineDigging.nextCut` perguntava `MineMarks.isOutOfReach` só para a posição
do **túnel**, e o minério **colado na parede** era servido por outra linha,
passando apenas pelo `nowhereToStand`.

**Why:** o mod tem vários leitores que escolhem alvo — cursor do túnel,
veio (`followingTheVein`), superfície, areia, degrau de volta — e cada um é
uma porta independente. O javadoc do `nowhereToStand` até afirma "toda
posição que vira alvo passa por aqui", e a afirmação era falsa para a marca.

**How to apply:** ao ver alvo repetido com castigo ativo, faça
`grep -rn "isUnreachableAround\|isOutOfReach"` e compare a lista com **todos**
os pontos que retornam alvo. A porta que falta é o defeito. Vale igual para
[[dois-mineiros-mesmo-tunel]] e para o lado do lenhador em
[[gametest-copia-do-predicado]].
