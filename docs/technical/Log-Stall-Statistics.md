# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-26 03:41 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 6
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 47 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 2 | observado |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 20 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 2 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 1 | observado |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 2 | observado |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 2 | progresso |
| `stairs_backfilled` | EscapeBackfill | 2 | progresso |
| `supper_shared` | VillageMeals | 0 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 3 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 5 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 17 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 18 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 5 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 5 |
| `LUMBERJACK` | `HARVESTING` | `RECOVERED` | `NO_TASK` | 5 |
| `LUMBERJACK` | `HARVESTING` | `WAITING` | `NO_TASK` | 6 |
| `MASON` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 3 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 3 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 3 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 14 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 3 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 14 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 6 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 1 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 2 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 1 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:dirt` | 14 |
| `minecraft:oak_planks` | 10 |
| `minecraft:green_carpet` | 9 |
| `minecraft:cobblestone_stairs` | 7 |
| `minecraft:oak_log` | 7 |

## Candidatos a investigacao

- `construction_waiting_resources`: 47 ocorrencias em BuilderWork / WaitingWork.
- `site_sweep_budget_exhausted`: 20 ocorrencias em RingSweep / BuildSiteScanner.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6383 |
| `miner_no_standing_room` | 7 |
| `construction_waiting_resources` | 308 |
| `builder_pathing_stalled` | 118 |
| `missing_profession` | 90 |
| `site_sweep_budget_exhausted` | 155 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 3 |
| `stranded_cannot_dig_out` | 1 |
| `construction_let_go` | 13 |
| `cycle_over_tick` | 198 |
| `log_error_line` | 6 |
| `stranded_freed` | 3 |
| `stairs_backfilled` | 2 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
