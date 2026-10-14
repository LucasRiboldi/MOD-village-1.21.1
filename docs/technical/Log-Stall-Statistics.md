# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-12 02:42 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 32
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 221 | candidato a loop |
| `miner_no_standing_room` | MineDigging | 3 | candidato a loop |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 0 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 11 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 13 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 3 | candidato a loop |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 0 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 8 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 6 | candidato a loop |
| `house_finished` | BuilderWork | 0 | progresso |
| `stranded_freed` | StrandedEscape | 3 | progresso |
| `stairs_backfilled` | EscapeBackfill | 1 | progresso |
| `supper_shared` | VillageMeals | 0 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 1 |
| `BUILDER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 3 |
| `BUILDER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 3 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 4 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 4 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 8 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TASK` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TARGET` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 2 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 7 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 2 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 2 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 1 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 1 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 9 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 9 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 4305 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 1 |
| `MINER` | `MINING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 580 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 233 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 2 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 6 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 10 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 12 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 1 |

## Fallback terminal de materiais

Estes sao os blocos comprovadamente inseridos sem entrega fisica. Pecas tiradas
de baus existentes ficam fora desta tabela: o log nao atribui uma pilha a uma
profissao sem evidencia de origem.

| Peca | Profissao atendida | Tentativas | Insercoes |
|---|---|---:|---:|
| `minecraft:white_wool` | `SHEPHERD` | 4 | 1 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| Nenhuma obra esperando peca | 0 |

## Candidatos a investigacao

- `miner_no_branch_work`: 221 ocorrencias em MineClaims / MinerWork.
- `miner_no_standing_room`: 3 ocorrencias em MineDigging.
- `builder_pathing_stalled`: 11 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 13 ocorrencias em RingSweep / BuildSiteScanner.
- `worker_stranded`: 3 ocorrencias em WorkStall / StrandedWorkers.
- `cycle_over_tick`: 8 ocorrencias em VillageDetectionHandler.
- `log_error_line`: 6 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6607 |
| `miner_no_standing_room` | 28 |
| `construction_waiting_resources` | 2045 |
| `builder_pathing_stalled` | 462 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 691 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 43 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 15 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 558 |
| `server_overloaded` | 22 |
| `log_error_line` | 64 |
| `house_finished` | 9 |
| `stranded_freed` | 39 |
| `stairs_backfilled` | 12 |
| `supper_shared` | 11 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
