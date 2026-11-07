---
name: e42-lacuna-do-planejador
description: "O E42 era o planejador, não o WorkAssignment; a mutação que o prova e as duas armadilhas do cenário"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-10T02:20:57.427Z
---

O **E42** (impasse entre profissões) foi fechado em 2026-09-09 à noite por
`theHouseGoesUpAfterTheFarmStepsAside`, em `FarmPlanGameTest`, batch
`farm_standoff`. A lacuna era **o planejador**, não o `WorkAssignment` — a
tentativa anterior (`CrossProfessionStandoffTest`) foi retirada por supor fome
de prioridade que não existe.

**A mutação que mede a lacuna**, e que serve para reconferir o teste:
remover `&& !FarmPlans.postponed(colony.id(), world.getTime())` da condição em
`ConstructionPlanner.plan` (~L275). Sob ela, medido: **681 unitários e 274
gametests passavam** antes deste teste existir. Com ele, **exatamente 1 teste
falha** — o que prova que não é outra invariante com outro nome.

**Duas armadilhas do cenário, ambas custaram rodada:**

1. `FarmerWork.shortenSearchTo(0)` **não serve**. A conta é
   `Math.max(dx, dz) <= reach`, então com zero o lote sobre o próprio centro
   tem distância 0 e **passa** — o impasse não se monta e a primeira asserção
   fica verde por outro motivo. Use **-1**.
2. A roça de planície é **13x9** (medido: `ColonyPos[x=7,y=2,z=9]` e
   `[x=13,y=2,z=9]`). Com raio de varredura 6 nenhum lote é achado, e o teste
   falha sem que haja defeito.

**`FarmPlans.clearAll()` não vai em `finally` de gametest:** ele limpa `READ` e
`POSTPONED` **globais** e o javadoc diz "chamado ao parar o servidor". Esvaziar
o cache de plantas no meio da bateria é a interferência que este projeto já
pagou — ver [[gametest-interferencia-global]]. Colônia de `UUID.randomUUID()`
deixa entrada órfã inofensiva.

Ver também [[gametest-copia-do-predicado]]: `theFarmLotStaysWithinTheFarmersReach`
chama `withinTheFarmersReach` **fora do fluxo**, e é a mesma armadilha.
