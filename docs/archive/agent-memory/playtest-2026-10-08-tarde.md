---
name: playtest-2026-10-08-tarde
description: playtest 0.3.12 (08-10 10:01) — fundidor em roda, falso baú cheio do mineiro, regressão do E1; 0.3.13; detecção funda colônia na bateria
metadata:
  type: project
---

0.3.13 (commits 79ff783e..4562278b, enviados; branch claude/obra-lote-e-trava, sem PR).

- Fundidor 191 pegas/191 soltas sem minério → trava `SMELT_INPUT` (mantém o plano B de fundir para a obra).
- Mineiro "chest is full" com espaço: o teto por tipo descartava pedra e contava como baú cheio → `Haul.spilled`.
- Regressão minha do E1: BFS devolvia o lugar mais perto do MINEIRO (borda do braço, 4,1 > 4) → ele parava "walking to" a própria posição. Lição: busca que acha "um" lugar deve escolher pela régua antiga (mais perto do alvo), não pela ordem da busca.
- Bateria: 669→672 testes mudou a grade, e a detecção de vila fundou colônia com aldeões de outro teste, BigHouseMOD ao lado do lenhador → `isColonyInfrastructure` protegeu o tronco. Bisect "código novo + testes antigos" isolou em uma rodada. Tarefa aberta (task_75e6430c). Ver [[gametest-interferencia-global]].
- E3 e E7 funcionaram em jogo. Abertos: lenhador 49% blocked (árvores 58–134 blocos), mina sem entrada ligada (M2).

**How to apply:** próximo playtest confere a lista do TODO.md "Playtest da 0.3.13"; sempre mostrar action_report ([[spark-com-action-report]]).
