---
name: reparo-adota-casa-fantasma
description: "Building registrada sem blocos no mundo é adotada pelo reparo cíclico, que roda antes da alternância e devolve a própria casa como obra nova"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-22T22:28:40.061Z
---

`ConstructionPlanner.plan` roda `BuildingRepairPlanner.open` **antes** da
regra de alternância. O reparo varre toda `Building` registrada, lê a planta
e conta os blocos de pé no mundo; se faltar qualquer um, ele abre reparo
daquela casa e `plan()` retorna nele, sem nunca chegar ao rodízio.

Consequência para teste: registrar uma `Building` só no registro — o que o
construtor de 5 argumentos de `Building` foi feito para permitir, e o javadoc
dele ainda diz que serve "os testes que montam o registro" — virou **cenário
impossível** para qualquer teste que depois chame `plan()`. Zero bloco de pé
contra os N da planta faz o reparo adotar a casa e devolvê-la como obra nova.

**Why:** foi exatamente isso que manteve
`FarmPlanGameTest.thenextturnafterahouseisnonresidential` vermelho por dias e
listado como gate P0. A asserção lia o reparo como "abriu outra casa e quebrou
a alternância", e a regra de alternância estava certa o tempo todo. O reparo
cíclico (P0.10) entrou depois do teste (09-20) e tornou o cenário inalcançável
sem que ninguém mexesse no teste.

**How to apply:** um GameTest que precise de "a vila já construiu X" e depois
chame `ConstructionPlanner.plan` tem de **assentar a planta no mundo**, não só
registrar a caixa. Leia com `HousePlans.blueprintOf` (que já aplica o giro
para a rua) e assente cada `BlueprintBlock` com `getDefaultState()` —
`standingBlocks` compara com `isOf`, então o estado não precisa casar, só o
bloco. Registre a `Building` com a caixa derivada de `blueprint.size()`, e não
uma caixa inventada.

Quando um teste desses falhar, leia o `build/gametest/logs/latest.log`
procurando `starts repair sweep for` na coordenada da falha: a linha diz na
cara que foi o reparo, e não a regra sob teste. A mensagem de asserção sozinha
mente sobre a causa.

Ver [[teste-vazio-por-condicao-impossivel]] — é o mesmo gênero de defeito — e
[[contagem-honesta-de-testes]] para extrair a mensagem real do
`build/gametest-report.xml`, já que o stdout do Gradle só imprime o nome do
teste que caiu, sem a asserção.

**Segundo caso, em jogo (25-09):** o reparo também adotava obra ABANDONADA
(`finished=false`), e roda antes do rodízio. Assinatura no log: "gives up on X"
e "starts repair sweep for X" na mesma origem e no mesmo segundo; e toda
`planned` diz "drawn from none fitting, the offered plan". A vila ficou presa a
`plains_temple_4` por dias. Decisão do autor: abandonada só volta na vez do tipo
(`HousePlans.isTurnOf`, no reparo e na retomada). Em teste de retomada, ponha
um bloco da planta no mundo: com zero de pé sobre caixa construída a obra é
descartada por outra regra e o teste passa à toa.
