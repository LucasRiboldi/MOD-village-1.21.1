---
name: gametest-navegacao-espera-pousar
description: findPathTo no mesmo tique do spawnEntity devolve null; espere ~5 tiques (waitAndRun) antes de pedir caminho num GameTest
metadata:
  node_type: memory
  type: feedback
  modified: 2026-09-25T14:34:59.948Z
---

Num GameTest, `villager.getNavigation().findPathTo(...)` chamado logo depois de `context.spawnEntity` devolve `null`, mesmo com caminho óbvio. Com `context.waitAndRun(5, ...)` funciona (o forense MineStallForensicGameTest já fazia assim).

**Why:** em 2026-09-25 o controle e o caso real do MinerCautionGameTest falharam juntos com "sem caminho"; parecia defeito do cenário e era o aldeão ainda sem pousar.

**How to apply:** todo GameTest de navegação espera alguns tiques antes do findPathTo; e sempre tenha um teste de controle (sem a mudança) que prove que o cenário mede algo — ver [[teste-vazio-por-condicao-impossivel]].
