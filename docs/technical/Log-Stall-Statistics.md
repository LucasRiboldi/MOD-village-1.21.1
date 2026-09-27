# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-27 02:37 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 13
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 26 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 1 | observado |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 31 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 1 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 1 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 41 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 1 | observado |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 0 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 1 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 1 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 3 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 4 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 6 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 10 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 8 |
| `MASON` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 1 |
| `MASON` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 4 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 4 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 11 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 2 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 14 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 2 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 22 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 1 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:oak_log` | 21 |
| `minecraft:glass_pane` | 5 |

## Candidatos a investigacao

- `construction_waiting_resources`: 26 ocorrencias em BuilderWork / WaitingWork.
- `site_sweep_budget_exhausted`: 31 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 41 ocorrencias em VillageDetectionHandler.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6383 |
| `miner_no_standing_room` | 11 |
| `construction_waiting_resources` | 403 |
| `builder_pathing_stalled` | 130 |
| `missing_profession` | 93 |
| `site_sweep_budget_exhausted` | 284 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 17 |
| `stranded_cannot_dig_out` | 3 |
| `construction_let_go` | 13 |
| `miner_chest_full` | 1 |
| `cycle_over_tick` | 310 |
| `log_error_line` | 16 |
| `house_finished` | 4 |
| `stranded_freed` | 15 |
| `stairs_backfilled` | 8 |
| `supper_shared` | 4 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
