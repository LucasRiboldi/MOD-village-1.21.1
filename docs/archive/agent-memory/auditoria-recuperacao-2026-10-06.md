---
name: auditoria-recuperacao-2026-10-06
description: "Auditoria VILA-RESET em docs/audit (branch claude/auditoria-recuperacao, bb3a54b8); P0-01 linhas paralelas, P0-02 save só no SERVER_STOPPING; espera decisões D-01..D-05"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-06T13:17:57.330Z
---

Auditoria de recuperação feita em 2026-10-06, só documentos, em
`docs/audit/` na branch `claude/auditoria-recuperacao` (commit bb3a54b8,
enviada, sem PR). Veredito AMARELO. Nenhum código alterado.

- **P0-01:** `claude/corrigiveis-sem-jogo` e `codex/village-visuals-logistics-mine-sweep`
  colidem em lógica (mina SHAPE_VERSION 7×8, FluidColumns × VillageFluidIndex,
  dois painéis). Merge-tree: 22 conflitos, VillageFocusPlayerGameTest não
  compila após fusão, closePlan duplicado, import Worker. Ordem sugerida: A
  (Claude) na main, B por cima, após decisões D-01..D-03 do autor.
- **P0-02:** `ColonySavedData.sync` só roda em SERVER_STOPPING
  (`ServerLifecycleHandler.java:44,205`) — crash perde o registro. Mecanismo
  confirmado, perda não reproduzida.
- Refutado: ArchUnit `allowStoreUpdate=true` NÃO aceita regressão (refreeze=false).
- Decisões do autor 06-10: D-01 salões (Codex), D-02 motor ADR-031 (Codex), D-03 painel Claude,
  D-04 emendar (ferramenta, BigHouseMOD+aldeões, pedregulho, peça sem rota podem surgir do nada → ADR-034,
  Constituição 1.2.0), D-05 vale o código (carpinteiro titular).
- **Integração feita** em `integra/linhas-2026-10-06` (852f3a54 + merge da auditoria 20b9c94f), enviada,
  SEM PR/merge na main ainda. 610/610 GameTests ×2, 1.325 unitários. PIT não rodado. Nada visto em jogo.
  Pendente P2: CropPatch/SandGathering sem pular fluidos (FluidColumns removido).
- Próxima ação: PR para main (com aval) e playtest: mina com save antigo, painéis com Iris, argila no lago. Método de
  trabalho proposto: Protocolo PASSO (RECOVERY-ROADMAP §6).

**Why:** o autor pediu diagnóstico antes de qualquer mudança e aprovação antes de refatorar.
**How to apply:** não implemente nada da auditoria sem aprovação; comece pelas decisões; ver [[sessao-de-jogo-destrava-o-plano]].

**ADR-035 (06-10, tarde):** pontos 1, 4, 5, 6, 7 feitos e enviados em `integra/linhas-2026-10-06`. Pendentes: ponto 3 (pedido de material, fase 1 — desenho na tabela da ADR-035) e ponto 2 (regra "uma linha" em CLAUDE/AGENTS + PR para a main). KF-003: duas intermitentes de fabricação (1/10). Filtro: `-PgametestOnly=X`, `-PgametestAudit=only`.

**ADR-035 concluída (06-10, 15h):** os 7 pontos aplicados; PR #9 (`integra/linhas-2026-10-06` → main) aberto, sem merge — aval do autor. Verificação final: 1.338 unit, 95 Python, 613/613 ×2, auditoria 1/1, PIT 87%, JAR 20090AF3…. Próximo: playtest da lista no STATE.md; fase 2 do pedido de material depende dele.
