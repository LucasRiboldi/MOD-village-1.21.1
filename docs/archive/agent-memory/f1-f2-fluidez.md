---
name: f1-f2-fluidez
description: F-1 (espera do alvo inalcançável: MineDescent, EmptySweeps, tora de casa no teto) e F-2 (TaskChain) aplicados em a6c71160; não vistos em jogo
metadata:
  type: project
---

Aplicados em 2026-10-02 (a6c71160, JAR f97efa85), do estudo
`docs/research/2026-10-02-melhorias-profissoes-vida-natural.md`:

- MineDescent: a marca do MineMarks é por pedra/cubo de 8, mas o que se repetia
  era o PONTO onde a descida trava (-241,10,379) com pedras diferentes. 2ª
  parada no mesmo ponto (±2) → toda pedra abaixo (±16 lateral) fora por
  MineMarks.memoryFor. Só conta parada abaixo da superfície (!isOut).
- EmptySweeps: varredura completa vazia (RingSweep não pausado) solta a tarefa;
  canReserveTask recusa COLLECT_SURFACE/SOIL por 5/10/20 min.
- TreeMarks.rejectBuilt: tora em construção (isColonyBuilt/isVillageOriginal)
  vai ao teto do castigo (48.000 tiques).
- TaskChain + core LastShortage: concluído pedido de recurso com falta no
  último ciclo e nenhum igual à espera → abre outro; IdleHands entrega.

**Why:** o F10 entregava tarefa aberta a cada 1 s, mas a tarefa só NASCIA no
ciclo de 30 s.
**How to apply:** no próximo log contar `The way down stops at`, `has no sand
anywhere`, `chained the next`; se `chained` explodir, a fila cresce —
conferir `assigned N tasks (N open)`. Ver [[mine-return-rastro]],
[[fluidez-passo-do-ciclo]].
