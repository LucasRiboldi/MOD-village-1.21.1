# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-10 03:27 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 35
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
| `miner_no_standing_room` | MineDigging | 0 | observado |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 16 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 34 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 7 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 0 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 1 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 4 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 0 | observado |
| `house_finished` | BuilderWork | 1 | progresso |
| `stranded_freed` | StrandedEscape | 0 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 1 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 2 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 7 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TASK` | 1 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 7 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 2 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 2 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 2 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 2 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 11 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 11 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 1 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 1 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 2 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 1 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 32 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 32 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:stonecutter` | 16 |

## Candidatos a investigacao

- `construction_waiting_resources`: 16 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 34 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 7 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 4 ocorrencias em VillageDetectionHandler.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6394 |
| `miner_no_standing_room` | 27 |
| `construction_waiting_resources` | 4093 |
| `builder_pathing_stalled` | 401 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 685 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 41 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 17 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 469 |
| `server_overloaded` | 22 |
| `log_error_line` | 64 |
| `house_finished` | 8 |
| `stranded_freed` | 37 |
| `stairs_backfilled` | 11 |
| `supper_shared` | 21 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
