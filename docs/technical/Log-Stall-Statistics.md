# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-24 12:42 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 4
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
| `miner_no_standing_room` | MineDigging | 7 | candidato a loop |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 79 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 106 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 77 | candidato a loop |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 58 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 1 | observado |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| `BUILDER` | `BUILDING` | `RECOVERED` | `ALREADY_OPEN` | 13 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `NO_TARGET` | 6 |
| `BUILDER` | `BUILDING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 4 |
| `BUILDER` | `BUILDING` | `WAITING` | `ALREADY_OPEN` | 14 |
| `BUILDER` | `BUILDING` | `WAITING` | `NO_TARGET` | 22 |
| `BUILDER` | `BUILDING` | `WAITING` | `SWEEP_INCOMPLETE` | 27 |
| `BUILDER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 36 |
| `BUILDER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 36 |
| `CARPENTER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 12 |
| `CARPENTER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 12 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_TASK` | 9 |
| `CARPENTER` | `CRAFTING` | `RECOVERED` | `NO_WORKER` | 7 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_TASK` | 14 |
| `CARPENTER` | `CRAFTING` | `WAITING` | `NO_WORKER` | 9 |
| `CARPENTER` | `CRAFT_WOOD` | `ABANDONED` | `WORK_STALLED` | 16 |
| `CARPENTER` | `CRAFT_WOOD` | `ERROR` | `WORK_STALLED` | 16 |
| `COLONY` | `COORDINATION` | `RECOVERED` | `NO_WORKER` | 7 |
| `COLONY` | `COORDINATION` | `WAITING` | `NO_WORKER` | 8 |
| `FARMER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 3 |
| `FARMER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 3 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TARGET` | 21 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_TASK` | 5 |
| `FARMER` | `FARMING` | `RECOVERED` | `NO_WORKER` | 2 |
| `FARMER` | `FARMING` | `RECOVERED` | `SWEEP_INCOMPLETE` | 24 |
| `FARMER` | `FARMING` | `WAITING` | `NO_TASK` | 6 |
| `FARMER` | `FARMING` | `WAITING` | `NO_WORKER` | 4 |
| `FARMER` | `FARMING` | `WAITING` | `SWEEP_INCOMPLETE` | 31 |
| `FARMER` | `MAINTAIN_FOOD` | `ABANDONED` | `WORK_STALLED` | 7 |
| `FARMER` | `MAINTAIN_FOOD` | `ERROR` | `WORK_STALLED` | 7 |
| `LUMBERJACK` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 30 |
| `LUMBERJACK` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 30 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ABANDONED` | `WORK_STALLED` | 53 |
| `LUMBERJACK` | `COLLECT_WOOD` | `ERROR` | `WORK_STALLED` | 53 |
| `LUMBERJACK` | `HARVESTING` | `RECOVERED` | `NO_TASK` | 34 |
| `LUMBERJACK` | `HARVESTING` | `RECOVERED` | `NO_WORKER` | 9 |
| `LUMBERJACK` | `HARVESTING` | `WAITING` | `NO_TASK` | 37 |
| `LUMBERJACK` | `HARVESTING` | `WAITING` | `NO_WORKER` | 11 |
| `MASON` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 66 |
| `MASON` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 66 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_TASK` | 12 |
| `MASON` | `CRAFTING` | `WAITING` | `NO_WORKER` | 10 |
| `MINER` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 16 |
| `MINER` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 16 |
| `MINER` | `COLLECT_STONE` | `ABANDONED` | `WORK_STALLED` | 45 |
| `MINER` | `COLLECT_STONE` | `ERROR` | `WORK_STALLED` | 45 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TARGET` | 2 |
| `MINER` | `MINING` | `RECOVERED` | `NO_TASK` | 33 |
| `MINER` | `MINING` | `RECOVERED` | `NO_WORKER` | 19 |
| `MINER` | `MINING` | `WAITING` | `NO_TARGET` | 3 |
| `MINER` | `MINING` | `WAITING` | `NO_TASK` | 36 |
| `MINER` | `MINING` | `WAITING` | `NO_WORKER` | 25 |
| `SHEPHERD` | `BUILD_STRUCTURE` | `ABANDONED` | `WORK_STALLED` | 39 |
| `SHEPHERD` | `BUILD_STRUCTURE` | `ERROR` | `WORK_STALLED` | 39 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_TASK` | 12 |
| `SHEPHERD` | `SHEPHERDING` | `WAITING` | `NO_WORKER` | 10 |

## Candidatos a investigacao

- `miner_no_standing_room`: 7 ocorrencias em MineDigging.
- `construction_waiting_resources`: 79 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 106 ocorrencias em BuilderApproach / BuilderWork.
- `missing_profession`: 77 ocorrencias em ColonyCycle / ProductionHands.
- `site_sweep_budget_exhausted`: 58 ocorrencias em RingSweep / BuildSiteScanner.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6383 |
| `miner_no_standing_room` | 7 |
| `construction_waiting_resources` | 256 |
| `builder_pathing_stalled` | 116 |
| `missing_profession` | 90 |
| `site_sweep_budget_exhausted` | 124 |
| `site_sweep_restarted` | 1 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
