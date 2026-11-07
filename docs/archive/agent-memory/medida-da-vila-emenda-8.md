---
name: medida-da-vila-emenda-8
description: Regra do autor 2026-10-03 para medir a vila — peças Vanilla, 15 em volta, só o lado que passa, lados ímpares, rua só com 10 caminhos fora (3a28555f)
metadata:
  type: project
---

ADR-003 Emenda 8 (commit 3a28555f, pushed; JAR 00658B2C…92E4). Partida: retângulo das PEÇAS da
vila gerada (a caixa da estrutura soma +12 por lado). Obra: 15 em volta; só o lado que passa
avança. Lados sempre ímpares (linha extra do lado que cresceu) → centro num bloco. Rua:
corrente ≥10 caminhos fora, encostada num caminho de dentro, até o mais distante (RoadSpill,
lê faixa Y da caixa, não heightmap). Save antigo sem boundsModel=2 é medido de novo.

**Why:** o autor rejeitou quadrado e a folga de 12; quer centro identificável e rua do jogador contando.
**How to apply:** vila do autor deve aparecer `measured at 143x117` centro (-241,419). Sem
GameTest da medida pelas peças. Ver [[quadrado-da-vila-proposta]], [[emenda-7-crescimento-sem-teto]].
