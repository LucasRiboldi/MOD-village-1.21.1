# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-09 22:23 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 31
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 2022 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 10 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 28 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 3 | candidato a loop |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 3 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 3 | candidato a loop |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 3 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 9 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 78 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TASK` | 6 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TARGET` | 31 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 6 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 26 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 16 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 16 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 7 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 7 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 1 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 3 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 4 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 10 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 10 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 174 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 175 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:stonecutter` | 2022 |

## Candidatos a investigacao

- `construction_waiting_resources`: 2022 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 10 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 28 ocorrencias em RingSweep / BuildSiteScanner.
- `worker_stranded`: 3 ocorrencias em WorkStall / StrandedWorkers.
- `cycle_over_tick`: 3 ocorrencias em VillageDetectionHandler.
- `log_error_line`: 3 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6394 |
| `miner_no_standing_room` | 27 |
| `construction_waiting_resources` | 3864 |
| `builder_pathing_stalled` | 343 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 671 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 41 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 16 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 457 |
| `server_overloaded` | 22 |
| `log_error_line` | 55 |
| `house_finished` | 7 |
| `stranded_freed` | 37 |
| `stairs_backfilled` | 11 |
| `supper_shared` | 18 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
