# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-28 05:19 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 21
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 1 | observado |
| `miner_no_standing_room` | MineDigging | 2 | observado |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 0 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 5 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 7 | candidato a loop |
| `missing_profession` | ColonyCycle / ProductionHands | 2 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 19 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 2 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 17 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 1 | observado |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 2 | progresso |
| `stairs_backfilled` | EscapeBackfill | 1 | progresso |
| `supper_shared` | VillageMeals | 1 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 2 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `BUILDER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 2 |
| `BUILDER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 2 |
| `CARPENTER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 2 |
| `CARPENTER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 2 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 3 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 6 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 4 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_WORKER` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 6 |
| `MASON` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 1 |
| `MASON` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 1 |
| `MASON` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 2 |
| `MINER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 1 |
| `MINER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 5 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 5 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 3 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 3 |
| `MINER` | `MINING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 13 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 5 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 6 |
| `MINER` | `MINING` | `WAITING` | `NO_WORKER` | 1 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 11 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 1 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 2 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| Nenhuma obra esperando peca | 0 |

## Candidatos a investigacao

- `builder_pathing_stalled`: 5 ocorrencias em BuilderApproach / BuilderWork.
- `surface_worker_unreachable`: 7 ocorrencias em SurfaceGatheringWork.
- `site_sweep_budget_exhausted`: 19 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 17 ocorrencias em VillageDetectionHandler.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6386 |
| `miner_no_standing_room` | 23 |
| `construction_waiting_resources` | 403 |
| `builder_pathing_stalled` | 156 |
| `surface_worker_unreachable` | 7 |
| `missing_profession` | 99 |
| `site_sweep_budget_exhausted` | 402 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 26 |
| `stranded_cannot_dig_out` | 4 |
| `construction_let_go` | 13 |
| `miner_chest_full` | 1 |
| `cycle_over_tick` | 352 |
| `server_overloaded` | 1 |
| `log_error_line` | 27 |
| `house_finished` | 4 |
| `stranded_freed` | 23 |
| `stairs_backfilled` | 10 |
| `supper_shared` | 6 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
