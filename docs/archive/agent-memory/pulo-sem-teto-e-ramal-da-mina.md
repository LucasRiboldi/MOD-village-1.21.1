---
name: pulo-sem-teto-e-ramal-da-mina
description: 0.4.25 corrige a volta para casa (nó OFF_STREET escapa da conferência de cabeça) e troca boca nova da mina por ramal; não jogada
metadata:
  node_type: memory
  type: project
  modified: 2026-11-05T12:52:25.542Z
---

Jogo da 0.4.24 (2026-11-05, colônia 9802360d): 36 de 48 teleportes "stuck with no path home" saíam do mesmo ponto da casa principal, em 3 mundos diferentes. Causa: `OFF_STREET` (DAMAGE_CAUTIOUS) faz `getJumpOnTopNode` pular a conferência de cabeça, e com `STEP`=2 o caminho sobe 2 blocos embaixo do teto. Parado no mesmo ponto com y alternando é o sinal: o aldeão está pulando contra o piso de cima.

**Why:** a mesma falha vale para qualquer teto baixo na vila, não só a casa; mudar `ColonyPathing` de novo pode reabrir.

**How to apply:** ver `LandPathNodeMakerMixin` antes de culpar planta de casa. Mina: `MineBranches` abre ramal antes de boca nova, 50 falhas ou só água liberam a boca; `MineEdge.PAST` só vale na sessão. `VC_PENS` decide o cercado grande no próximo jogo. Fim de ciclo em [[regra-blocos-surgidos-fabricacao]] continua valendo.
