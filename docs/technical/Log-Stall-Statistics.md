# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-09 15:46 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 30
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 673 | candidato a loop |
| `miner_no_standing_room` | MineDigging | 5 | candidato a loop |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 0 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 136 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 40 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 11 | candidato a loop |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 8 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 3 | candidato a loop |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 8 | progresso |
| `stairs_backfilled` | EscapeBackfill | 1 | progresso |
| `supper_shared` | VillageMeals | 3 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 35 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TARGET` | 15 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 10 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 5 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 5 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 4 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 4 |
| `LUMBERJACK` | `HARVESTING` | `RECOVERED` | `NO_TASK` | 1 |
| `LUMBERJACK` | `HARVESTING` | `WAITING` | `NO_TASK` | 1 |
| `MINER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 3 |
| `MINER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 3 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 25 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 25 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 1855 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 9 |
| `MINER` | `MINING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 11074 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 692 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 10 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 30 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 2 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 3 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 15 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 15 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| Nenhuma obra esperando peca | 0 |

## Candidatos a investigacao

- `miner_no_branch_work`: 673 ocorrencias em MineClaims / MinerWork.
- `miner_no_standing_room`: 5 ocorrencias em MineDigging.
- `builder_pathing_stalled`: 136 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 40 ocorrencias em RingSweep / BuildSiteScanner.
- `worker_stranded`: 11 ocorrencias em WorkStall / StrandedWorkers.
- `cycle_over_tick`: 8 ocorrencias em VillageDetectionHandler.
- `log_error_line`: 3 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 7059 |
| `miner_no_standing_room` | 30 |
| `construction_waiting_resources` | 1820 |
| `builder_pathing_stalled` | 414 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 659 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 49 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 15 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 455 |
| `server_overloaded` | 22 |
| `log_error_line` | 55 |
| `house_finished` | 6 |
| `stranded_freed` | 42 |
| `stairs_backfilled` | 12 |
| `supper_shared` | 12 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
