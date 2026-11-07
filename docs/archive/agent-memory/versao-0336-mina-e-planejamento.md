---
name: versao-0336-mina-e-planejamento
description: "0.3.36 (dc178996, pushed): mina 1×2 sem labirinto, oficina única, casa por cama, ociosos com trabalho; JAR C3E0D0F6 instalado, não jogado; expansão do terreno espera escolha A–E"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-12T17:20:12.342Z
---

0.3.36 corrige o labirinto da 0.3.35. A rede antiga tinha corredor 3×3 e só contava minério exposto: 1.411 blocos cavados deram 2 de ferro, e a segurança pôs 2.392 blocos. Também entraram as regras do autor de 2026-10-12 (Regra 55):
- oficina única;
- casa só por falta de cama;
- roça grande primeiro;
- SmelterAdvance, OutskirtsPlanting e pastor com qualquer comida;
- grama e flores surgem sozinhas;
- casca de 5 camadas sob a vila.

**Why:** "ferreiro" no pedido do autor foi lido como fundidor (SMELTER), porque é o ofício do forno.

**How to apply:** no próximo playtest, contar `an idle smelter advanced`, `planted N saplings outside` e a razão minério/bloco cavado. A expansão do terreno espera a escolha do autor entre as opções A–E (eu recomendei C+D) em docs/research/2026-10-12-expansao-do-terreno.md. O MinerDirectPathGameTest continua instável (~1 em 3). Ver [[mineiro-preso-na-descida]], [[bateria-gametest-instavel]].
