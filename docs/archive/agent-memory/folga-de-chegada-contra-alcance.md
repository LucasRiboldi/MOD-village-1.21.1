---
name: folga-de-chegada-contra-alcance
description: "construtor \"chega\" na borda do alcance com folga 2 e nunca entra nele; e o whyNotReached mente o destino"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-30T05:05:00.402Z
---

Playtest de 2026-09-30 (01:53–01:58, JAR D0512A8E): a casa do pastor em
1756,71,-5325 abriu e nenhum bloco foi posto. Dois construtores pararam a 5–6
blocos do alvo, num lote plano e livre (conferido lendo o save).

Causa: `BuilderApproach.footOf` (4 args) escolhe o ponto de pé mais próximo do
aldeão dentro de REACH=5 do alvo, ou seja, na borda. `BuilderWork` grava esse
ponto com `WorkTargets.DEFAULT_ARRIVAL = 2`, e o Vanilla dá por chegado com
Manhattan ≤ 2 entre blocos. Ele para até 2 blocos fora do círculo, e
`isWithinReach` (posição física até o centro do alvo, ≤ 5) nunca fica verdadeiro.

Armadilha do diagnóstico: `whyNotReached` imprime "it was walking to" com
`standingSpotNear(coluna do alvo)`, e não o destino real em `WorkTargets`.

**Why:** a folga de chegada e o alcance de trabalho são duas distâncias
medidas de jeitos diferentes; um ponto na borda do alcance mais qualquer folga
fica fora dele. Irmão de [[mineiro-em-cima-da-mina]].

**How to apply:** quando um trabalhador para "N blocos away" sem se mover num
terreno livre, compare a folga de chegada do destino com o alcance de quem
executa antes de caçar pathfinding. Ver [[lento-nao-e-travado]].

**Estado (2026-09-30):** corrigido em `15882dd` (branch
`claude/sync-local-github-80cc8a`, sem push): ARRIVAL 0, STANDING_RADIUS 4.
GameTest vermelho antes, 499/499 depois. Falta instalar o JAR e ver a obra
andar no save.
