# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-03 13:56 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 23
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 375 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 95 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 1 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 40 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 3 | candidato a loop |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 1 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 253 | candidato a loop |
| `cycle_over_tick` | VillageDetectionHandler | 1 | observado |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 4 | candidato a loop |
| `house_finished` | BuilderWork | 2 | progresso |
| `stranded_freed` | StrandedEscape | 3 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 1 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 3 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 4 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 3 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 23 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 23 |
| `MASON` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 15 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 15 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 12 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 25 |
| `MINER` | `MINING` | `RECOVERED` | `NO_WORKER` | 1 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 12 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 26 |
| `MINER` | `MINING` | `WAITING` | `NO_WORKER` | 1 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 14 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 19 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 20 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 30 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 31 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:bricks` | 221 |
| `minecraft:glass_pane` | 62 |
| `minecraft:white_terracotta` | 42 |
| `minecraft:cobblestone` | 30 |
| `minecraft:dandelion` | 15 |
| `minecraft:oak_fence` | 4 |
| `minecraft:poppy` | 1 |

## Candidatos a investigacao

- `construction_waiting_resources`: 375 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 95 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 40 ocorrencias em RingSweep / BuildSiteScanner.
- `worker_stranded`: 3 ocorrencias em WorkStall / StrandedWorkers.
- `miner_chest_full`: 253 ocorrencias em MinerHaul / ChestDepositor.
- `log_error_line`: 4 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6386 |
| `miner_no_standing_room` | 25 |
| `construction_waiting_resources` | 778 |
| `builder_pathing_stalled` | 253 |
| `surface_worker_unreachable` | 7 |
| `missing_profession` | 108 |
| `site_sweep_budget_exhausted` | 470 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 32 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 14 |
| `miner_chest_full` | 265 |
| `cycle_over_tick` | 370 |
| `server_overloaded` | 1 |
| `log_error_line` | 33 |
| `house_finished` | 6 |
| `stranded_freed` | 28 |
| `stairs_backfilled` | 11 |
| `supper_shared` | 7 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
