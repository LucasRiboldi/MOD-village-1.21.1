---
name: e51-identidade-da-vila
description: BigHouseMOD nascendo várias vezes era a identidade da vila só por centro (64 blocos); corrigido em 2895d71, save com 7 BigHouseMOD em 44cd9e5a
metadata:
  type: project
---

2026-09-30 21h: a vila do autor passa de 128 blocos; aglomerado de camas a 65+
blocos do centro virava colônia nova → fundação (BigHouseMOD + 7 adultos) → só
depois a fusão (ADR-007) absorvia, e a recém-nascida venceu (sobrevivente era
"mais camas vistas"). Fix `2895d71` (pushed): `ColonyIdentity` (centro ≤64, mesma
vila gerada, cama a ≤32 de construção), fusão por construções a ≤32, sobrevivente
= mais trabalhadores. Análise: docs/technical/Identidade-da-Vila-2026-09-30.md.

**Why:** o autor quer UMA BigHouseMOD por vila; o save ainda tem 7 na `44cd9e5a`.
**How to apply:** JAR 89C93B68… (bf9b94f) instalado em mods em 30-09 ~21h55 (anterior AA3FDED3 salvo no scratchpad); não verificado em jogo — procurar `is part of colony` e nenhum
`Placed BigHouseMOD` na vila conhecida. Decisões pendentes do autor: limpar o
save; restringir execução das profissões à vila do jogador (VillageFocus.isWorking,
desfaz decisão de 30-09). ArchUnit congela violação pela assinatura do método:
mudar assinatura de método que chama o adaptador reprova; ConversionBoundaryTest
proíbe `new BlockPos(colonyPos...)` fora do adaptador. Ver [[playtest-2026-09-30-noite-e49-e50]].
