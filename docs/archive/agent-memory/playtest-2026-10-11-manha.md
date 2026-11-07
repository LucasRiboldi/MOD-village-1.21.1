---
name: playtest-2026-10-11-manha
description: Playtest 0.3.24 em mundo novo (11-10 10:04–11:44) + Spark 3Q6g9MrO1o — mina funciona, fundidor saiu da roda, Regra 51 ainda reprova; hotspot no SmelterWork
metadata:
  type: project
---

Mundo NOVO ("Novo mundo", 0 colônias carregadas): o sinal da RC2 no save antigo (-768,66,-923) não pôde ser visto.

- Mina: 1022 MINED (11/min, alvo >0,8), desceu um nível 11:29; "the way down got no closer" = 0 (RC1 não precisou disparar).
- Aberto: 31 GAVE_UP do mineiro parado na boca (-396,68,-1059) sem chegar mais perto de alvos em -386,59..61,-1062 — rota da boca ao alvo some; o 3º mineiro (9d41d5db) minerou 2.
- Fundidor: 2 pegas/0 soltas (a roda de 311/311 acabou), mas 51% espera; nunca fez vidro: areia nunca coletada (0 no diário; 33 sand protegidas pela Regra 3). glass_pane e cobblestone saíram pela regra da 4ª tentativa (16 e 58 VC_SUPPLY_ERROR).
- Regra 51 reprova: BUILDER 81%, SHEPHERD 68%, MINER 43%, LUMBERJACK 29%, MASON 22%, CARPENTER 16%; passam FARMER 10%, SMELTER 1%.
- Custo: TPS 20 os 61 min, MSPT mediano ~7 ms, máx 66,6. cost_ledger total p95 47 / máx 57 ms (abaixo do gatilho 50 da Fase 4 por pouco). Spark: mod = 7,9% do tempo ocupado; 72% disso é SmelterWork.smeltFrom→mayStillSmelt (conta todos os baús dentro do laço de baús: O(baús²×crus) por peça, e de novo por item no workForTheBuilds).
- Nada corrigido nesta sessão; só análise. Ver [[roteiro-correcoes-2026-10-10]], [[spark-com-action-report]].
