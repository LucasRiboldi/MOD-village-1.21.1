# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-22 11:53 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 1
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 6337 | candidato a loop |
| `miner_no_standing_room` | MineDigging | 0 | observado |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 123 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 7 | candidato a loop |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 11 | candidato a loop |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 40 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |

## Candidatos a investigacao

- `miner_no_branch_work`: 6337 ocorrencias em MineClaims / MinerWork.
- `construction_waiting_resources`: 123 ocorrencias em BuilderWork / WaitingWork.
- `builder_pathing_stalled`: 7 ocorrencias em BuilderApproach / BuilderWork.
- `missing_profession`: 11 ocorrencias em ColonyCycle / ProductionHands.
- `site_sweep_budget_exhausted`: 40 ocorrencias em RingSweep / BuildSiteScanner.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6337 |
| `construction_waiting_resources` | 123 |
| `builder_pathing_stalled` | 7 |
| `missing_profession` | 11 |
| `site_sweep_budget_exhausted` | 40 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
