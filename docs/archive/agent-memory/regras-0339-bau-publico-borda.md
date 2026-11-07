---
name: regras-0339-bau-publico-borda
description: 0.3.39 (57702fb5): baú público numa casa, 1 em 5 obras na borda, carpinteiro calça a rua e guarda toda peça de madeira, fazendeiro e pastor fora da ajuda à obra; não jogado
metadata:
  type: project
---

Regras do autor de 2026-10-12, aplicadas na 0.3.39 (JAR 4C6C345A, não jogado):
- `PublicChestPlacement`: põe o baú público numa casa da colônia.
- `VillageExpansion.EDGE_EVERY` = 5, `EDGE_BAND` = 16: uma em cada 5 obras procura lote na faixa da borda.
- `ItemStock.isWoodPiece`: o carpinteiro guarda um pouco de toda peça de madeira do jogo.
- `BuilderPaving.PAVERS`: o carpinteiro também calça a rua.
- `BuildHelper.HELPS` ficou só com carpinteiro e pedreiro.

**Armadilhas dos GameTests:**
- `isSkyVisible` depende da luz do céu, que só é recalculada depois de pôr o teto. Use a geometria (há bloco em cima).
- Registrar um `Building` depois de medir a caixa empurra a caixa 16 blocos (Regra 56) para cima das arenas vizinhas. Registre a casa antes da colônia.
- A varredura de baús (`VillageChests.around`) lê chunks inteiros. Filtre pela caixa.

**Why:** o autor quer que a vila guarde o excedente e cresça para as bordas.

**How to apply:**
- No próximo playtest, conferir o ócio do fazendeiro e do pastor (Regra 51), que perderam a ajuda à obra.
- Ver [[playtest-0337-baus-publicos]].
