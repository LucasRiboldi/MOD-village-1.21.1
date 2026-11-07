---
name: vila-viva-0441
description: "0.4.41, cinco pontos da vila viva (Regras 217-221); causa dos poucos nascimentos era trigo cru em baú alheio; candidata"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-07T02:01:30.616Z
---

0.4.41 (2026-11-06), branch `feat/vila-viva`, candidata sem jogo. Base: jogo da 0.4.40, Spark WCWRBHTKlX.

- Poucos nascimentos: 201 trigos e 35 comestíveis nos baús, 27 de 39 adultos com 0 pontos. Só o baú do fazendeiro assava. A felicidade conta trigo como comida, então a vila parecia feliz sem ninguém poder comer. Regra 217.
- Regra 218: 1 obra repetida a cada 5 adultos, tipo com menos de pé; teto de camas 6; `VILLAGE_COMPLETE` no lugar de `NOT_IN_GAME`. O autor não deu outro número, usei 5.
- Regra 219: rotina à vista, só depois de 1 min sem tarefa e dentro da caixa. Sem a espera, GameTests de construtor e pastor quebraram: o aldeão saía andando entre tarefas.
- Regra 220: sem ofício calça e tira neve. Tirar neve sem teste próprio.
- Regra 221: 1 ponto de comida por adulto por dia, ferramenta dura 3 dias (2 tábuas + 3 pedras), manutenção de 4 itens por construção por dia, nunca com obra aberta ou abaixo de 36.

**Why:** o autor pediu "todos os pontos" depois da avaliação "a vila precisa ser mais viva".

**How to apply:** no próximo jogo, medir nascimentos por dia, obras repetidas, `UPKEEP` no diário, `VC_COST` e se falta tábua em obra. Se a madeira faltar, a Regra 221 é a primeira suspeita. Instáveis em aberto: `workerCollectsWhileWalkingPast` e `animalsWalkInThroughEachOfTheFourGates` (`TODO.md`).

Relacionado: [[tudo-pelo-grafo-0440]], [[gametest-interferencia-global]].
