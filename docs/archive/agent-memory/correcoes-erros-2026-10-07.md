---
name: correcoes-erros-2026-10-07
description: "ADR-039 na main (PR #15, #16); KF-003 era fusão de colônias no tique de teste; JAR 0.3.4 instalado, sem release no GitHub"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-07T20:08:59.576Z
---

Estado em 2026-10-07 (tarde): main = f0c9a2f2. PR #15 (A3 walk, folga KF-003, teste das portas) e
PR #16 (ADR-039: E1, E2, D1–D3, causa do KF-003, F1–F3) mesclados. JAR 0.3.4 `614C60B3…EDF5` em
build/libs, downloads/ e .minecraft/mods; **sem versão publicada no GitHub** (não foi pedido).

Lições:
- **KF-003 era fusão de colônias**: `tickGameTestServer` rodava o ciclo de 600 tiques com
  `ColonyMergeTrigger`, e cenários vizinhos viravam uma colônia no meio do teste (7–11 fusões
  por bateria). Contar `absorbed colony` no log da bateria é a assinatura. Corrigido em 1e55cb9e.
- **Aldeão nunca baixa o braço no servidor** (só HostileEntity/jogador chamam tickHandSwing).
- **Mina de GameTest desce abaixo do mundo**: a arena fica em y −60; teste que usa posição de
  ramal precisa da boca ~80 acima.
- Índice de ramal: posições exclusivas vão de SHARED_BLOCKS a SHARED_BLOCKS + ARM_BLOCKS
  (use `beyondTheArm`).
- Assert de teste intermitente que imprime o que viu (mina, baú) achou a causa em uma rodada.

Depois (PR #17, main c3db744e, JAR 0.3.5 `5F6BAEFB…0058` instalado): memórias 1–5 e 7 vão ao save
`villagecolony_memory` via `fabric.work.WorkMemory`; fechar/abrir mundo de verdade NÃO verificado.

Rodada 2 (PR #18, main 399113f0, JAR 0.3.6 `DAC96728…7DE8` instalado): produto no baú da profissão,
peças do bioma (BiomePieces lê as plantas da vila), mineiro nunca larga o ofício (giveUpProfession),
TODO.md enxuto (histórico em docs/archive/technical/TODO-ate-2026-10-07.md), scripts/gametest_battery.py.
Não existe profissão "artesão" — não usar a palavra com o autor. Regra do autor: toda profissão se
adequa ao bioma; o antigo fica antigo.

Rodada 3: branch claude/opcao-b-e-plano-de-testes enviada, SEM PR/merge (autor pediu só commit push);
JAR 0.3.7 `10407A73…7DAE` instalado. Opção B (junção/ciclo de vida só na colônia atendida) e plano de
testes (D2, B1, B3–B6, save) com mutação. Lições: cenário fora da arena 8x8 bate na barreira da borda
(D2 passava por ela); a arena fechada faz animal a ≤6 da cerca contar como "em curral".

Pendente com o autor: merge da branch, destino de docs/workers-analysis
(F5), números do agente na ADR-039, playtest. D2: mutação da parada na borda sobreviveu.
Ver [[decisoes-adr-037]].
