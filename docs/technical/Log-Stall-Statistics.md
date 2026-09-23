# Estatistica de travamentos e repeticoes

**Gerado em:** 2026-09-23 12:28 UTC
**Log analisado:** `latest.log`
**Sessoes no historico:** 3
**Limiar de candidato a loop:** 3 ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
| `miner_no_branch_work` | MineClaims / MinerWork | 10 | candidato a loop |
| `miner_no_standing_room` | MineDigging | 0 | observado |
| `construction_waiting_resources` | BuilderWork / WaitingWork | 17 | candidato a loop |
| `builder_pathing_stalled` | BuilderApproach / BuilderWork | 1 | observado |
| `surface_worker_unreachable` | SurfaceGatheringWork | 0 | observado |
| `missing_profession` | ColonyCycle / ProductionHands | 0 | observado |
| `site_sweep_budget_exhausted` | RingSweep / BuildSiteScanner | 8 | candidato a loop |
| `site_sweep_restarted` | SweepLog / BuildSiteScanner | 0 | observado |

## Atividades por profissao

| Profissao | Atividade | Resultado | Motivo | Ocorrencias |
|---|---|---|---|---:|
| Nenhuma transicao VC_ACTIVITY observada | - | - | - | 0 |

## Candidatos a investigacao

- `miner_no_branch_work`: 10 ocorrencias em MineClaims / MinerWork.
- `construction_waiting_resources`: 17 ocorrencias em BuilderWork / WaitingWork.
- `site_sweep_budget_exhausted`: 8 ocorrencias em RingSweep / BuildSiteScanner.

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
| `miner_no_branch_work` | 6383 |
| `construction_waiting_resources` | 177 |
| `builder_pathing_stalled` | 10 |
| `missing_profession` | 13 |
| `site_sweep_budget_exhausted` | 66 |

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
