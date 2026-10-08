# ADR-039 — Respostas às pendências de 07-10

**Status:** Accepted
**Date:** 2026-10-07
**Decision Type:** Gameplay + Processo
**Origem:** resposta do autor à lista `docs/technical/Decisoes-Pendentes-2026-10-07.md`.

## Decisões

| # | Decisão |
|---|---|
| A1 | Buscar alternativas de correção para os testes intermitentes (KF-003) |
| C | Listar as memórias que podem ir para o save, para o autor escolher. **Escolha:** 1 árvores e mudas, 2 colunas de coleta, 3 veio e rastro, 4 ramais reservados, 5 vez do adiantamento, 7 coletas do pastor; a 6 (espera depois de busca vazia) fica fora |
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
| C | `3bbb8f68` | Arquivo `villagecolony_memory` (`WorkMemorySavedData`), uma seção por memória escolhida, montado e lido por `WorkMemory`; lido depois das minas | `WorkMemoryGameTest`, mutação pega. **Não verificado:** fechar e abrir um mundo de verdade |

## Números do agente (para revisar)

`MineOreHeading.COAL_WANTED = 32`, `IRON_WANTED = 16`, `KEEP_TICKS = 200`; `WorkPath.STEPS_PER_CYCLE = 4`,
`MAX_GAP = 24`; `AdvanceStock.CARPENTER_PIECES` só de carvalho (vila de outra madeira cede a vez
pela paciência do adiantamento).

## Segunda rodada (07-10, tarde)

| # | Decisão do autor | Commit | Verificado |
|---|---|---|---|
| 1 | Não existe profissão "artesão" (era o nome do agente para carpinteiro e pedreiro). A P3c vale: o produto vai para o **baú da profissão** de quem fez | `5c2e964a` | `thePieceGoesToTheProfessionChest`, mutação pega |
| 4 | **Toda profissão se adequa ao bioma.** O adiantamento de carpinteiro, pedreiro e fundidor segue as peças das casas do estilo da vila (`BiomePieces`), mais usadas primeiro; pinheiro e acácia ganham escada, laje e cerca | `b99fe1d4` | `BiomePiecesGameTest`, mutação pega |
| 5 | Peça sem rota e sem baú: depois de algumas tentativas aparece no baú de quem precisa | — | já em vigor (`BiomeConstructionSupply`, 4 tentativas, baú do construtor) |
| 6 | O mineiro **não perde o ofício** | `2a779c9e` | `theMinerKeepsHisTradeAfterGivingUp`, mutação pega |
| 7 | O que é antigo fica antigo; as mudanças valem daqui para frente | — | casas antigas sem piso não são refeitas |

### Item 2 — KF-002

O cenário de coleta é montado a 65–97 blocos da arena, numa direção sorteada, em terreno gerado
que ninguém controla; em 22-09 três hipóteses de carregamento de chunk foram refutadas. Não
reapareceu em ~27 baterias de 07-10. Recomendação: montar o cenário **dentro da arena**, com o
raio protegido encurtado por gancho de teste. Fica aberto até a bateria repetida confirmar.

### Item 3 — o tique do servidor de teste, com foco na colônia local do jogador

Depois de `1e55cb9e`, o ciclo de 600 tiques no servidor de teste só faz uma coisa com as colônias
dos cenários: marca ativa/dormente pelo chunk do centro (`updateLifecycles`). Detecção, ciclo da
colônia e planejamento já exigem jogador por perto (Emenda 6), e no teste não há jogador.

| Alternativa | O que muda | Prós | Contras |
|---|---|---|---|
| **A. Como está** | fusão desligada só no teste | já medido: 0 fusões, baterias limpas | em jogo, colônias longe do jogador ainda se fundem e mudam de estado |
| **B. Tudo pela colônia do jogador (recomendada)** | fusão e ciclo de vida só para colônias atendidas (jogador dentro da caixa ou há até 5 min) | uma regra só para jogo e teste; custo menor; nada muda numa vila que o jogador não vê | duas vilas distantes que crescem até se tocar só se fundem quando o jogador passar por lá |
| **C. Teste sem ciclo** | o tique de teste roda só o trabalho por tique; quem precisa do ciclo chama `runCycleNow` | isolamento total dos cenários | não muda nada em jogo |

**Pede:** escolher entre A, B e C (B cobre o teste sem regra especial para ele).

## Terceira rodada (07-10, fim da tarde)

| # | Decisão do autor | Commit | Verificado |
|---|---|---|---|
| 3 | **Opção B:** junção e ciclo de vida só na colônia atendida; sai a exceção do tique de teste | `2d509553` | `onlyTheAttendedColonyMerges`; filtro anulado devolve 17 junções à bateria |
| Plano de testes | D2 (o teste passava pela barreira da arena, não pela regra), B1, B3, B4, B5 (pastor andando de verdade), B6 (o teste antigo do morro não provava o morro), arquivo do save | `1e0406b3` … último | cada um com a mutação que o derruba |
