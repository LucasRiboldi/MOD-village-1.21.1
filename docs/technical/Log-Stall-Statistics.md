# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-10-09 18:11 UTC
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
| `construction_waiting_resources` | BuilderWork / WaitingWork | 0 | observado |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 32 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 6 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |
| `worker_stranded` | WorkStall / StrandedWorkers | 1 | observado |
| `stranded_cannot_dig_out` | StrandedEscape | 0 | observado |
| `construction_let_go` | ConstructionPlanner / WaitingWork | 1 | observado |
| `miner_chest_full` | MinerHaul / ChestDepositor | 0 | observado |
| `cycle_over_tick` | VillageDetectionHandler | 6 | candidato a loop |
| `server_overloaded` | Minecraft (servidor) | 0 | observado |
| `log_error_line` | qualquer (nivel ERROR) | 3 | candidato a loop |
| `house_finished` | BuilderWork | 1 | progresso |
| `stranded_freed` | StrandedEscape | 1 | progresso |
| `stairs_backfilled` | EscapeBackfill | 0 | progresso |
| `supper_shared` | VillageMeals | 1 | progresso |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 2 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 3 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 1 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 10 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 1 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TARGET` | 4 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 3 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 1 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 1 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 2 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 2 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 4 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 2 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 4 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 3 |
| `MINER` | `MINING` | `WAITING` | `SWEEP_INCOMPLETE` | 2 |
| `SHEPHERD` | `SHEPHERDING` | `RECOVERED` | `NO_TASK` | 1 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 2 |
| `SMELTER` | `SMELTING` | `RECOVERED` | `NO_TASK` | 16 |
| `SMELTER` | `SMELTING` | `WAITING` | `NO_TASK` | 16 |

## Pecas que as obras mais esperaram

| Peca | Linhas `waiting for` |
|---|---:|
| Nenhuma obra esperando peca | 0 |

## Candidatos a investigacao

- `builder_pathing_stalled`: 32 ocorrencias em BuilderApproach / BuilderWork.
- `site_sweep_budget_exhausted`: 6 ocorrencias em RingSweep / BuildSiteScanner.
- `cycle_over_tick`: 6 ocorrencias em VillageDetectionHandler.
- `log_error_line`: 3 ocorrencias em qualquer (nivel ERROR).

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 7059 |
| `miner_no_standing_room` | 30 |
| `construction_waiting_resources` | 1820 |
| `builder_pathing_stalled` | 446 |
| `surface_worker_unreachable` | 37 |
| `missing_profession` | 110 |
| `site_sweep_budget_exhausted` | 665 |
| `site_sweep_restarted` | 1 |
| `worker_stranded` | 50 |
| `stranded_cannot_dig_out` | 5 |
| `construction_let_go` | 16 |
| `miner_chest_full` | 270 |
| `cycle_over_tick` | 461 |
| `server_overloaded` | 22 |
| `log_error_line` | 58 |
| `house_finished` | 7 |
| `stranded_freed` | 43 |
| `stairs_backfilled` | 12 |
| `supper_shared` | 13 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
