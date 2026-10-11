# ADR-041 — Decisões do autor sobre a auditoria de 10-10

**Status:** Accepted
**Date:** 2026-10-10
**Decision Type:** Gameplay + Processo
**Origem:** resposta do autor às decisões pendentes do
[`Roteiro-Correcoes-2026-10-10.md`](../technical/Roteiro-Correcoes-2026-10-10.md)
(seção "Decisões do autor que este roteiro espera").

## Decisões

| # | Pergunta do roteiro | Decisão |
|---|---|---|
| 1 | Liberar o commit e o PR da auditoria (Fase 0.1) | **Liberado.** Commitar e abrir PR |
| 2 | Integrar ou encerrar a `codex/fluxo-grafo-waypoints` (AUD-05) | **Integrar.** Merge de três vias numa branch a partir da `main`, sem reescrever a branch do Codex (o worktree dela está ativo) |
| 3 | Qual é a regra do ócio (`CLAUDE.md` §0.4) | **A abrangente: ocioso + bloqueado + encalhado ≤ 15%** por profissão no expediente. É a conta do `time_ledger.py`; o `IdleYield` passa a usar a mesma soma, e não só o ócio puro (`WorkTime.java:195`) |
| 4 | `/vc log` com ou sem permissão (AUD-17) | **Aberto, e com dados amplos.** Continua sem exigir nível de operador e passa a mostrar, por profissão, a última janela do `WorkTime` (trabalho, caminhada, espera, bloqueio, ócio, encalhe) e a marca da regra do ócio (AUD-19) |
| 5 | Começar as correções | **Sim**, na ordem do roteiro: Fase 0 e depois Fase 1 |

Não respondido, e portanto não feito: apagar as 7 branches já contidas na `main` (AUD-24).

## Consequências

- **Decisão 3:** com a soma abrangente, um ofício **bloqueado** também cede vaga. O
  roteiro registrava o risco de ceder trabalhador de um ofício que espera a cadeia
  (o mineiro sem boca de mina). A correção mede isso no playtest seguinte com o
  `check-tick-budget.sh`: se o ofício que cedeu continuar parado, a causa é a cadeia
  (Fase 2.2), não o número de mãos.
- **Decisão 4:** o `/vc log` passa a expor a proporção do expediente por profissão a
  qualquer jogador. É uma escolha de transparência sobre informação de jogo, não de
  segurança: o comando continua só de leitura.
- **Numeração:** a regra do ócio é chamada de "Regra 51" no `CLAUDE.md` §0.4, no
  `time_ledger.py` e no `STATE.md`, mas a Regra 51 do `docs/RULES.md` (06-10) é outra:
  a peça que aparece no baú na 4ª tentativa. A colisão fica registrada como AUD-26
  para o autor escolher o número; até lá, o texto diz "regra do ócio".

## Estado

| # | Commit | O que foi feito | Verificado |
|---|---|---|---|
| 1 | — | auditoria e roteiro commitados | — |
| 2 | — | — | — |
| 3 | — | — | — |
| 4 | — | — | — |
