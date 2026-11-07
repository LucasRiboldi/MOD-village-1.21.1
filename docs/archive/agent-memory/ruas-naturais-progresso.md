---
name: ruas-naturais-progresso
description: Frente das ruas naturais (branch fix/playtest-0102) — o que está feito (Regras 138–140) e o que falta dos 5 passos do estudo
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T12:32:01.717Z
---

Branch `fix/playtest-0102`, estudo `docs/research/2026-10-28-ruas-naturais.md` (5 passos). Estado em 2026-10-28:

- Passo 1 feito (efca3b0e, Regra 138): `StreetSurface` mistura gasta + protegida em `BlockProtection.mayBreak`.
- Passo 2 quase todo (ab39acce Regra 139 `LotScore` + `RoadIndex.findAmongRoads` janela 64; 46a1ce60 Regra 140 rua de 3 com `RoadPaving.layEdges`, só o meio é ponta `isMiddleOf`, pontas da mais perto do centro). 903/903 gametests. **Não jogado.**
- **Falta do passo 2:** "construídos juntos" no sentido estrito — hoje o planejador calça o trecho na hora, sem custo (Regra 15); o estudo pede o trecho como peças de chão assentadas pelo construtor junto da obra. O lote já nasce na beira do trecho novo (`JUST_PAVED`).
- Passos 3–5 abertos: curva pelo relevo + quintal 2 + folga lateral 4–6; lote verde a cada ~5 casas, pracinha, árvores e luz; medida (colunas de rua/ramais no VC_STATE, `state_report.py`).
- Depois das ruas vem a mina ([[mina-natural]] doc `docs/research/2026-10-28-mina-natural.md`, começa por `MineRoute`) e os pontos restantes da 0.3.102 (baús, cama com baú, fazenda, noite, terracota, curral, ar da planta, portão como porta, smooth).
- Versão ainda 0.3.102 no gradle: bump, CHANGELOG, STATE, JAR e PR ficam para o marco de entrega.

**Why:** a sessão é longa e a frente tem muitos passos; sem isto a próxima retoma do zero.
**How to apply:** retomar pelo item "falta do passo 2" ou passo 3, conforme o autor; conferir `git log` da branch antes.
