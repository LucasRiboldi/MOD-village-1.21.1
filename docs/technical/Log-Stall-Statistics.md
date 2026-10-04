# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-04 05:32 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 25
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 14 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 6 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 2 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 1 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 51 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 1 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 1 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 5 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 19 | candidato a loop |
| `log_error_line` | qualquer (nivel ERROR) | 2 | observado |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 1 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 0 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 20 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `NO_TARGET` | 5 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 26 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 22 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 10 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 23 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 6 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 6 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 4 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 6 |
| `MINER` | `MINING` | `RECOVERED` | `NO_WORKER` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 2 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 4 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 7 |
| `MINER` | `MINING` | `WAITING` | `NO_WORKER` | 1 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 1 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:glass_pane` | 14 |

## Candidatos a investigacao

- `construction_waiting_resources`: 14 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 6 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 51 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 5 ocorrencias em VillageDetectionHandler.
- `server_overloaded`: 19 ocorrencias em Minecraft (servidor).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6386 |
| `miner_no_standing_room` | 25 |
| `construction_waiting_resources` | 1384 |
| `builder_pathing_stalled` | 259 |
| `surface_worker_unreachable` | 9 |
| `missing_profession` | 109 |
| `site_sweep_budget_exhausted` | 561 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 36 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 15 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 391 |
| `server_overloaded` | 20 |
| `log_error_line` | 36 |
| `house_finished` | 6 |
| `stranded_freed` | 32 |
| `stairs_backfilled` | 11 |
| `supper_shared` | 7 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
