---
name: corredor-e-mineiro-profundidade
description: Regras do autor de 22-10 — caminho é corredor de 1 bloco saindo da porta em reta com curvas de 90° (Regra 70); o mineiro não constrói fora da montanha e desce antes de ir de lado (Regra 71)
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-22T05:56:20.713Z
---

**Regra 70 — corredor de caminho (autor, 2026-10-22):**
- "Todo caminho é uma linha reta até encontrar outro caminho", "com quebras de 90 graus, sem considerar
  blocos na diagonal", "caminho deve ser apenas um bloco conectado a outro".
- "Sempre saem da porta das estruturas em direção linear a outra linha de caminhos".
- O objetivo é evitar "tudo virar caminho".
- Implementada em aff29065: `Corridor` (busca por posição, direção e curvas; no máximo 2 curvas, 2 retos
  antes da primeira, fecha no primeiro encontro ao sair da fila), usado por `WorkPath` e `StripLot`. O
  `BuilderPaving.isGap` só fecha buraco no meio de uma reta.

**Regra 71 — mineiro (autor, 2026-10-22):**
- "Deve ser evitada a construção de caminhos (adicionando blocos em formato de labirinto) para fora da
  montanha; sempre focar em ir em direção à profundidade mais do que para os lados se precisar construir
  para se locomover".
- Implementada em 62501caf: `MinerTerrain` (céu aberto não recebe bloco) e `DetourMoves.Costs.MINER`
  (cavar descendo 6, de lado 12, subindo 18, pôr 20).
- Implementada também em dd668591: `MineAccess` cava um acesso só cavando, descendo da vila, quando a
  entrada da mina é inalcançável a pé. Foi escolha do autor contra "mudar a boca" e "entrar pelo nível".

**Why:** no deserto, o desvio do mineiro pôs piso no ar ("0 dug, 8 placed") até uma entrada no topo da
duna, e o construtor encheu áreas de caminho pela regra dos 3 lados.

**How to apply:**
- Qualquer código novo que trace caminho até uma construção usa `Corridor`. Nunca uma busca livre nem uma
  troca de eixo a cada passo.
- Qualquer movimento do mineiro que construa usa `MinerTerrain` e `Costs.MINER`.
- Ainda não seguem o corredor, sem pedido do autor: a extensão de rua (`RoadExtension`, já reta) e o recuo
  de 3 (`siteAt`, já reto). Os caminhos já existentes em mancha não foram desfeitos.

Relacionado: [[movimentacao-natural]], [[decisoes-autor-2026-10-22-deserto]].
