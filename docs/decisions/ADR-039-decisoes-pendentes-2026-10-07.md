# ADR-039 — Respostas às pendências de 07-10

**Status:** Accepted
**Date:** 2026-10-07
**Decision Type:** Gameplay + Processo
**Origem:** resposta do autor à lista `docs/technical/Decisoes-Pendentes-2026-10-07.md`.

## Decisões

| # | Decisão |
|---|---|
| A1 | Buscar alternativas de correção para os testes intermitentes (KF-003) |
| C | Listar as memórias que podem ir para o save, para o autor escolher |
| D1 | Carpinteiro adianta peças |
| D2 | Construtor calça o caminho até a obra |
| D3 | Ramais rumo ao minério que a vila precisa |
| E1 | A espera depois de "nada no raio" cresce em partes |
| E2 | **Não confirmado:** o animal solto continua sendo alimentado; um dia a coleta do pastor o recolhe |
| E3 | Confirmado: a reunião doa a comida mesmo sem cama sobrando |
| E4 | Confirmado: a boca da mina fica dentro da vila |
| F1 | Aceitar pedra como solo de lote (P0.7) |
| F2, F3 | Incorporar as branches do Codex ao projeto, para nada se perder |
| F5 | Listar as opções para `docs/workers-analysis/` |

## Estado

| # | Commit | O que foi feito | Verificado |
|---|---|---|---|
| E1 | `e731a169` | `EmptySweeps`: 1, 2, 3, 4 e 5 minutos (um minuto por varredura vazia, teto nos 5 da P7) | `EmptySweepsTest` |
| E2 | `5fbefee7` | `ShepherdFlock`: a procriação não exige mais animal amarrado ou em curral | `ShepherdLooseFlockGameTest`, mutação pega |
| D1 | `0b12afc4` | `OAK_STAIRS`, `OAK_SLAB`, `OAK_FENCE` viram recursos e o carpinteiro ganha a fila de adiantamento (5 de cada). **Defeito da P5b corrigido junto:** a tarefa ignorava a peça pedida e o pedreiro caía em tora → tábua; `CraftingSteps.produceForGoal` faz a peça até um alvo fixo | `CraftingAdvanceGameTest` (carpinteiro e pedreiro), mutação pega |
| D2 | `de1a46bb` | `WorkPath`: obra que não encosta em rua ganha caminho reto do caminho mais perto até a borda do lote, 4 blocos por ciclo, até 24 de distância, Regra 3 respeitada | `WorkPathGameTest`. **A mutação da parada na borda sobreviveu**: outra guarda também segura |
| D3 | `1b53c2c6` | `MineOreHeading`: o mineiro sem ramal pega primeiro o que tem mais minério procurado nas posições por cavar (carvão abaixo de 32 no baú, ferro abaixo de 16; sem falta, qualquer minério). Ordem guardada 200 tiques | `MineOreHeadingGameTest`, mutação pega |
| A1 | `1e55cb9e` | **Causa achada:** o tique do servidor de teste rodava a fusão de colônias a cada 600 tiques, e colônias de cenários vizinhos viravam uma no meio do teste (7 a 11 fusões por bateria). O tique de teste deixa de fundir | 6 baterias seguidas 644/644 com 0 fusões; antes, falha em 4 das 7 anteriores |
| F1 | — | Já estava no código desde 15-09 (`LotGround.isLotGround`: qualquer bloco sólido). A regra sai de "a decidir" no `RULES.md` | — |
| F2 | `da5a1cb0`, `1a31ae07` | O `Barn_Majest.nbt` da branch (diferente do em uso) foi para `docs/archive/structures/`; o `Storage_Majest.nbt` era idêntico. A branch entrou na história por merge sem trazer os arquivos: nome com maiúscula não vale no jogo e colide no Windows | — |
| F3 | — | `codex/village-visuals-logistics-mine-sweep` já estava inteira na `main` (0 commits à frente) | — |
| E3, E4 | — | Confirmados: nada muda | — |

## Números do agente (para revisar)

`MineOreHeading.COAL_WANTED = 32`, `IRON_WANTED = 16`, `KEEP_TICKS = 200`; `WorkPath.STEPS_PER_CYCLE = 4`,
`MAX_GAP = 24`; `AdvanceStock.CARPENTER_PIECES` só de carvalho (vila de outra madeira cede a vez
pela paciência do adiantamento).
