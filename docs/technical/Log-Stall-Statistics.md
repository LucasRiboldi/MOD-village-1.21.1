# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-11-01 19:16 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 34
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 2 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 42 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 9 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 1 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 7 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 8 | candidato a loop |
| `house_finished` | BuilderWork | 1 | progresso |
| `stranded_freed` | StrandedEscape | 1 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 0 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 2 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 2 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 7 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 7 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 3 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 3 |
| `LUMBERJACK` | `HARVESTING` | `RECOVERED` | `NO_TASK` | 3 |
| `LUMBERJACK` | `HARVESTING` | `WAITING` | `NO_TASK` | 3 |
| `MASON` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 2 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 2 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 1 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 1 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 3 |
| `SHEPHERD` | `COLLECT_WOOL` | `ABANDONED` | `WORK_STALLED` | 4 |
| `SHEPHERD` | `COLLECT_WOOL` | `ERROR` | `WORK_STALLED` | 4 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 5 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 6 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 5 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 6 |

## Fallback terminal de materiais

Estes sao os blocos comprovadamente inseridos sem entrega fisica. Pecas tiradas
de baus existentes ficam fora desta tabela: o log nao atribui uma pilha a uma
profissao sem evidencia de origem.

| Peca | Profissao atendida | Tentativas | Insercoes |
|---|---|---:|---:|
| `minecraft:glass_pane` | `BUILDER` | 2 | 2 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| `minecraft:oak_log` | 2 |

## Candidatos a investigacao

- `builder_pathing_stalled`: 42 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 9 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 7 ocorrencias em VillageDetectionHandler.
- `log_error_line`: 8 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 10482 |
| `miner_no_standing_room` | 52 |
| `construction_waiting_resources` | 2072 |
| `builder_pathing_stalled` | 698 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 1195 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 269 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 15 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 1055 |
| `server_overloaded` | 22 |
| `log_error_line` | 74 |
| `house_finished` | 28 |
| `stranded_freed` | 258 |
| `stairs_backfilled` | 12 |
| `supper_shared` | 53 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
