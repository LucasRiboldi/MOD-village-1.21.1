---
name: playtest-0417-encalhado-falso-solto
description: "Playtest 0.4.17 (04-11) — construtor preso 50 min por falso \"solto after 0 steps\"; 0.4.18 candidata instalada, não jogada"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-05T00:34:06.837Z
---

Playtest 0.4.17 (2026-11-04, 68 min, 20 TPS, sem erro do mod; MAE não usado). Construtor ficou
50 min em -1584,102,3565: `frozen twice` → `isOut` (só altura do terreno) → "out after 0 steps",
a cada 2 min, antes do ClimbOut agir; cama a 2 blocos (teleporte à cama não salvaria).

Correção C escolhida pelo autor, na branch `fix/encalhado-falso-solto` (commits 75bdfce0 + 0dc25840,
sem push/PR): `StrandedWorkers.mayLeaveAt` + PINNED (re-encalhe onde foi solto sem mover fica na
fuga); `BedTeleport.toSafety` pula cama a ≤ SAME_PLACE e vai ao centro (Regra 114). JAR 0.4.18
SHA-256 0D6C9D98…759D nas três cópias; 0.4.17 `.disabled`. Sem testes (modo §0.2.2).

**Why:** prova só no próximo playtest: procurar "stranded again where it was let go" e teleporte
para "the village center"; construtor sem bloqueio longo.

**How to apply:** também na 0.4.18 (SHA 39AAD519…7D7C, commits a18bc6ea/2e1e361e/7be62306):
fazendeiro chama TaskProgressWatch.progressed na colheita; 4 linhas por bloco em DEBUG; curral
BLOCKED a não-pastor de fora + strandKeepingWork com portão (Regra 122 revista). Mineiro não era
defeito (13/22 ferros em y=-8). Lenhador = mesmo falso solto; lote baixo = save antigo. No playtest:
conferir 0 "fenced in" com "drops its work" e tarefas de trigo concluídas.
StrandedEscape está em 500 linhas exatas. Ver [[climb-out-nunca-desiste]], [[merge-na-main-autorizado]].

Jogo da 0.4.18 (2026-11-04, mundo "Novo mundo", colônia 1d93f45b): 7 notas viraram a 0.4.19 na mesma branch
(VillagerMoves, TreeClearing, PathSupport, pastor, DryLand, BridgeDeck; Regras 171-174), instalada, não jogada, sem
testes. A 0.4.20 (mesma branch, instalada, não jogada) traz /vc mae mostrar, colony/miner_house com lote sem rua, boca da mina na rede e velas. Abertos no TODO.md. Stub nbtlib.py no scratchpad: rodar scripts com python -P.

0.4.22 (05-11, mesma branch, instalada, não jogada): grafo como mapa de trilhos (RoadLines, Regra 177, emenda ADR-043), RoadGraphRefresh a cada 5 min, /vc mae reler. Simulado fora do jogo com save "Novo mundo": harness em scratchpad, dump por script; 7 releituras até peça única. Conferir no jogo: VC_ROAD_GRAPH, sinalizador, portas brancas, construtor calçando (só sem obra aberta).
