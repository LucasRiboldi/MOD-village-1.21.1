---
name: auditoria-tecnica-2026-10-10
description: "Auditoria D1–D12 de 10-10 na branch claude/auditoria-tecnica-2026-10-10, SEM commit por pedido do autor; fila AUD-01..24; sessão 0.3.23 parcial reprova Regra 51"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-11T00:43:26.600Z
---

Auditoria técnica de 2026-10-10 sobre `b290ed10` (JAR 0.3.23). Branch
`claude/auditoria-tecnica-2026-10-10`, **tudo na working tree, sem commit**: o
pedido da auditoria dizia "não commite". Nada em `src/` foi alterado.

Entregue: skills (`modpack-modsmith` de rdl7999-hub/minecraft-modpack-modsmith +
5 escritas na sessão) em `.claude/skills/`; `docs/technical/avaliacao/`
(D01–D12, ACTION-QUEUE, EXECUTIVE-SUMMARY, agent-setup, régua `2026-10-11-b290ed10/`);
`scripts/audit/check-*.sh` + sondas em `scripts/audit/probes/`; STATE, TODO
(seção AUD), README e CHANGELOG atualizados; retrato em
`docs/archive/technical/Project-Audit-2026-10-10.md`.

Medido: 1398 unitários, 690/690 GameTests ×3 (+1 com Fabric API 0.116.17), PIT
1804/2106 = 85,66% (folga de 13 mutantes), ciclo p95 67 ms na 0.3.22.

**Roteiro:** `docs/technical/Roteiro-Correcoes-2026-10-10.md` (fases 0–6, AUD-01..25), também sem commit.
Validado contra o código: a correção do fundidor em roda (`cd7ca2c8`) e a ADR-040 estão SÓ na
`codex/fluxo-grafo-waypoints`; a raiz do P0 é o mineiro sem boca de mina (`no mine mouth … budget
ran out`, 115× na 0.3.22 e 50× na 0.3.23, MineVein.java:184-191), com ~20 mineiros saindo do ofício
por sessão (hipótese: IdleYield pune quem espera a cadeia). A sessão 0.3.23 (21:24–22:06) reprovou
a Regra 51: SMELTER 91%, BUILDER 92%, MINER 17%; fundidor 311/311 soltas. O ciclo deu p95 27,7 ms,
então o AUD-04 virou P2 até nova medida.

**Pendente:** (1) o autor liberar o commit; (2) decisões 0.2 (linha Codex) e 0.3 (qual é a Regra 51)
antes da Fase 2; (3) a Fase 1 (AUD-03, AUD-02) pode começar sem jogo.

**Why:** o próximo ciclo começa por AUD-01/AUD-05, e o trabalho não commitado
some se a branch for trocada sem cuidado.
**How to apply:** antes de tocar código, `git status` na branch da auditoria;
ver [[persistentstate-engole-excecao]] para AUD-02 e [[regua-c14-cega-a-secao]].
