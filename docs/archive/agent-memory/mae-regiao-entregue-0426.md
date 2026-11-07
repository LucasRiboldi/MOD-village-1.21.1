---
name: mae-regiao-entregue-0426
description: "0.4.26 emenda a ADR-045 item 10: obra pronta entrega a região em vila nova, porta pelo grafo, retomada; autor escolheu, não jogada"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-05T13:40:10.114Z
---

Decisão do autor em 2026-11-05: a região MAE tem fim (Regra 187). Antes, uma reserva parava para sempre a releitura do grafo e o índice de ruas da dimensão, e `road_v2` e o caminho comum nunca se encontravam. Alternativas recusadas: manter a região fechada; não mudar.

**Why:** sem isso a primeira obra congelava o grafo do mundo e a porta da obra ficava sem ligação física.

**How to apply:** em vila nova não reintroduzir `road_v2` em `MaeSites`; a porta vem da releitura. Falta: primeira obra em jogo, carga, gate da água, Fase 6. Os GameTests do MAE esperam `road_v2` e precisam de revisão quando os testes forem liberados. Ver [[mae-fase3-structure-v3]].
