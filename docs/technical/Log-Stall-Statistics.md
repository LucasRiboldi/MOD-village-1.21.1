# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-30 05:01 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 22
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 0 | observado |
| `miner_no_standing_room` | MineDigging | 2 | observado |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 0 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 2 | observado |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 8 | candidato a loop |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 28 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 3 | candidato a loop |
| `stranded_cannot_dig_out` | StrandedEscape | 1 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 11 | candidato a loop |
| `cycle_over_tick` | VillageDetectionHandler | 17 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 2 | observado |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 2 | progresso |
| `stairs_backfilled` | EscapeBackfill | 1 | progresso |
| `supper_shared` | VillageMeals | 0 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `NO_TARGET` | 2 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `NO_TARGET` | 2 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 4 |
| `BUILDER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 1 |
| `BUILDER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 1 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 6 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 8 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_WORKER` | 1 |
| `CARPENTER` | `CRAFT_WOOD` | `ABANDONED` | `WORK_STALLED` | 1 |
| `CARPENTER` | `CRAFT_WOOD` | `ERROR` | `WORK_STALLED` | 1 |
| `COLONY` | `COORDINATION` | `WAITING` | `NO_WORKER` | 1 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 13 |
| `FARMER` | `FARMING` | `WAITING` | `NO_WORKER` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 15 |
| `MASON` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 2 |
| `MASON` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 2 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 3 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 7 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 7 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 5 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 3 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 8 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 5 |
| `MINER` | `MINING` | `WAITING` | `NO_WORKER` | 5 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 9 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 1 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| Nenhuma obra esperando peca | 0 |

## Candidatos a investigacao

- `missing_profession`: 8 ocorrencias em ColonyCycle / ProductionHands.
- `site_sweep_budget_exhausted`: 28 ocorrencias em RingSweep / BuildSiteScanner.
- `worker_stranded`: 3 ocorrencias em WorkStall / StrandedWorkers.
- `miner_chest_full`: 11 ocorrencias em MinerHaul / ChestDepositor.
- `cycle_over_tick`: 17 ocorrencias em VillageDetectionHandler.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6386 |
| `miner_no_standing_room` | 25 |
| `construction_waiting_resources` | 403 |
| `builder_pathing_stalled` | 158 |
| `surface_worker_unreachable` | 7 |
| `missing_profession` | 107 |
| `site_sweep_budget_exhausted` | 430 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 29 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 13 |
| `miner_chest_full` | 12 |
| `cycle_over_tick` | 369 |
| `server_overloaded` | 1 |
| `log_error_line` | 29 |
| `house_finished` | 4 |
| `stranded_freed` | 25 |
| `stairs_backfilled` | 11 |
| `supper_shared` | 6 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
