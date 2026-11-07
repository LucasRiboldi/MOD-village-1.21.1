---
name: lenhador-expulso-pelo-guarda
description: "o guarda de imobilidade expulsava quem cortava árvore aos 300 ticks; cortar é ficar parado, e o lenhador era o único que não zerava"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-12T04:20:34.786Z
---

Relato do autor em jogo, 2026-09-12: *"o lenhador tem que cortar todo o tronco
das arvores sem deixar troncos da arvora sem cortar, mesmo que nao alcance"*.

**A causa não era alcance.** O log mostra a assinatura:

```
chopping — tree at 2428,67,-2056, 1 blocks away, block 38 of 180, 20 logs so far
has not moved a block in 300 work ticks — wood task returned to the queue
chopping — tree at 2472,69,-2096, 0 blocks away, block 34 of 71
  -> mesma expulsão, com ZERO blocos de distância
```

`WorkStall.stuck()` pergunta *"o aldeão saiu do bloco?"* e dispara em
`LIMIT = 300`. **Cortar árvore é ficar parado** — uma cerejeira de 180 blocos
leva mais de 300 tiques de pé no mesmo lugar. O lenhador era a **única das seis
profissões** que não chamava `job.stall.reset()` ao agir sobre o alvo:
`BuilderWork`, `CraftingWork`, `FarmerWork`, `MinerWork` e `ShepherdWork` todas
chamavam, e a javadoc de `WorkStall.reset()` já dizia que o lugar é *"o ramo em
que a profissão age sobre o alvo"*. O conserto foi uma linha em
`TreeFelling.chop`.

**O ciclo vicioso que isso criava, e que custou a madeira da colônia:**
expulsão deixa tora de pé -> `TreeMarks.markUnreachable` pula a árvore por
6.000 tiques -> a copa decai nesse tempo -> o resto vira
`N logs without a living canopy`, que é **recusa definitiva** -> madeira perdida.
É a causa de `no oak_log in the colony chests` que parou o construtor em 09-11.
Ver [[casa-nao-sobe-e-fome-nao-travamento]].

**Why:** um guarda anti-travamento que mede *imobilidade* pune toda profissão
cujo trabalho é ficar parado. Os dois erros são simétricos e igualmente ruins —
guarda que nunca fala deixa trabalhador preso para sempre; guarda que fala
sempre expulsa quem trabalha. Por isso os dois gametests existem em par:
`theStallGuardReturnsTheTaskAndForgetsTheTree` e
`theStallGuardDoesNotEvictALumberjackWhoIsChopping`.

**How to apply:** ao criar profissão nova, confira se ela zera o `stall` no
ramo em que age. E desconfie de toda recusa cujo número é exatamente um
limite — a nota de 2026-08-19 no `TreeHarvester` já dizia: *"quando o número da
recusa é exatamente o limite, o limite é a causa"*. Em 09-12 o número era 11,
não 24, e foi o que descartou o teto como culpado.

**Decisão do autor no mesmo dia:** o teto de colheita de 24 troncos saiu, e a
árvore sai sempre inteira. Ele foi avisado do preço e escolheu assim. Sobrou
**uma** defesa para construção de tronco: a regra da copa (tronco sem folha
viva ligada não é árvore). Ficou exposta a construção de tronco **encostada
numa árvore viva**, que partilha a copa dela — e não há como fazer melhor
lendo o mundo: `BlockProtection.isPlayerPlaced` só funciona para folha
(`LeavesBlock.PERSISTENT`), porque o Minecraft não registra autoria de tronco.
Se ele reclamar de construção comida, é este o trade-off, e a saída seria uma
heurística de forma (largura do tronco), não o teto de volta.
