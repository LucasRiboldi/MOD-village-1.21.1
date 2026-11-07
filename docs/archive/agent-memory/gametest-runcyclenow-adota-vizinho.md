---
name: gametest-runcyclenow-adota-vizinho
description: "runCycleNow num GameTest roda a detecção de vila e adota as camas das arenas vizinhas como colônia nova; e testes do mesmo lote se engolem"
metadata:
  type: feedback
---

`VillageDetectionHandler.runCycleNow` chama `detectAround`. Na arena, isso adota as camas dos testes vizinhos
como colônia nova (7 adultos e casa fundadora), e essa colônia toma aldeões e baús. Assim o
`CraftingGameTest` caía 3 de 3 com `-PgametestOnly=ColonyDetection,Crafting`.

Testes do mesmo `batchId` rodam ao mesmo tempo. A colônia com casa de um teste engole as camas do outro, e o
log diz *"is part of colony … — NEAR_ITS_BUILDINGS"*.

O CI começa limpo e ainda assim caía. O mundo guardado em `build/gametest/world` só somava a isso, e agora é
apagado antes de cada bateria (0.3.91).

**Why:** o "intermitente pelo arranjo das arenas" era isso. Dava para medir, ler o log do lote e corrigir.

**How to apply:**
- Teste que só precisa do ciclo da colônia usa `runColonyCycleNow`.
- Teste de detecção usa `batchId` próprio.
- Cenário longe da arena não serve para o aldeão ser visto: o chunk não processa entidade.
- Ao investigar uma queda, procure no log do lote "Bed cluster … part of colony" e colônias de outros
  testes contratando ali perto.

Relacionado: [[bateria-gametest-instavel]], [[gametest-interferencia-global]].
