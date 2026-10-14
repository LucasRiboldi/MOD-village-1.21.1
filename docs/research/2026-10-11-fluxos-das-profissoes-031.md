# Fluxo das oito profissões — playtest 0.3.31 (11-10, 23:12–23:41)

Fontes: `latest.log` (9.346 linhas), diário `villagecolony-actions-20261011-231251.jsonl`,
`scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py`,
`scripts/analyze_village_log.py` e o Spark `qWTXLoLP5g`.

## Desempenho (Spark)

TPS 20 em todas as janelas de 1 min (uma de 7,41 é a pausa do jogo); MSPT mediano 6,6–9,3 ms;
um pico de 488 ms. O mod é 2,5% do tempo do servidor (`VillageDetectionHandler.tickActiveServer`
1,25%, `SmelterWork.tick` 0,48%, `MinerWork.tick` 0,40%). `cost_ledger`: ciclo da colônia médio
22 ms, p95 47 ms (planejador 15,6 / 38,1 ms) — abaixo do corte de 50 ms da Fase 4.
**Não há lag do mod; o problema é fluxo.**

## Tempos por profissão (expediente, `time_ledger`)

| Profissão | s | trabalho | andando | espera | bloqueado | ocioso | encalhado | Regra 51 (≤15%) |
|---|---|---|---|---|---|---|---|---|
| Construtor | 1426 | 0% | 5% | 6% | 3% | **85%** | 2% | reprova (89%) |
| Carpinteiro | 1426 | 11% | 7% | 16% | 0% | **67%** | 0% | reprova (67%) |
| Fazendeiro | 1426 | 0% | **46%** | **35%** | 0% | 18% | 0% | reprova (18%) |
| Lenhador | 1426 | 12% | **39%** | 0% | 6% | 42% | 0% | reprova (49%) |
| Pedreiro | 1426 | 18% | 7% | 11% | 9% | **57%** | 0% | reprova (65%) |
| Mineiro | 2139 | 21% | 33% | 14% | 5% | 27% | 0% | reprova (32%) |
| Pastor | 1426 | 8% | 13% | 6% | **28%** | 46% | 0% | reprova (74%) |
| Fundidor | 2137 | 3% | 38% | **55%** | 0% | 4% | 0% | espera > 40% |

## O que cada uma fez, e por quê (diário + log)

| Profissão | Ações/min | Pausas, erros, bloqueios, ócio | Causa | Correção |
|---|---|---|---|---|
| **Construtor** | PLACED 0,0 | 5 pegas, 1 encalhe; os dois construtores sem tarefa por 18 min | O 5f9e06e2 estava **dentro da mina** (y=39) e foi mandado a −341, 41, −1016, uma galeria debaixo da casa; encalhou e largou a obra. O pedreiro já estava como **ajudante**, e o planejador contava a vaga dele como "a obra já tem tarefa": não abriu outra para o construtor | `ConstructionPlanner.ensureTask` ignora a vaga do ajudante; `BuilderApproach.footOf` não manda para patamar debaixo do lote |
| **Pedreiro** | PLACED 7,1, CRAFTED 1,2 | segurou a vaga de ajudante 21.587 tiques (182 blocos) | ele fez a obra que era do construtor | idem acima |
| **Fazendeiro** | HARVESTED 1,0 | 2 desistências "could not reach the crop" depois de 20 min; 18 min sem nada maduro | Roça pequena e trigo verde; sem semente no baú, arar não acontece; não havia alternativa | Roça descansando → devolve a colheita e ajuda a obra (`BuildHelper.HELPS` + `FieldRest`) |
| **Mineiro** | MINED 18,8 | 9 desistências "got no closer" (400 tiques cada); "waiting for a branch" com 2 ramais esgotados; pedra cavada na superfície em −378, 68, −1026 | (a) sem ramal livre, cavava a encosta em volta da vila; (b) com a pedra no fim do ramal (22 blocos da entrada), `legDown` a dava por "fora da mina" e o mineiro na superfície andava para a entrada enterrada embaixo dele; (c) no buraco, 5 abaixo do destino, devolvia a tarefa e repetia | Regra 53 (boca só na borda; sem cava de superfície com mina aberta); `LEVEL_REACH` 28; 2+ abaixo do destino sem desvio → encalhado, sai pela fuga |
| **Carpinteiro** | CRAFTED 4,8 | 26 "feitos" com 0 peça ("no room in the personal chest for oak_planks") | Tora → tábua sem teto: 960 tábuas, baús pessoais cheios, tarefa reaberta a cada 30 s | `StockRules.logsToConvert` para no teto de 64 sem obra pedindo |
| **Pastor** | CRAFTED 4,0 | 7 solturas "no sheep with wool"; mesma roda de 0 peça do carpinteiro | sem ovelha com lã no raio; 64 lãs surgiram com **218 nos outros baús** | `ChestTransfer`: o que a vila tem é levado, não criado |
| **Lenhador** | FELLED 12,2 | 39% andando; 81 recusas "Not a tree" (toras de casa) | regra de 11-10: a árvore mais perto **dele** dentro do alcance da vila; ele vai encadeando até a borda (−281, −928, ~100 blocos do centro) | sem mudança (decisão do autor); ver Pendências |
| **Fundidor** | SMELTED 2,4 | 55% espera; 238 carvões vegetais "para as obras" | a reserva de cru recusa pedra (232 pedras e 233 pedras lisas) e o log diz "nenhum baú tinha pedra"; sem teto, o fallback fez carvão com 446 de carvão nos baús | `SmelterFallback.stocked`: nada "para as obras" acima do teto (carvão vegetal conta o mineral) |

## Obras

- `plains_medium_house_1` (obra 162a476a): às 20:45 tinha 8 peças por pôr; ao carregar o save às
  23:14 voltou **girada pela rua nova** — 292 por pôr, 27 blocos da própria casa limpos do lote,
  120 peças "is in the way". É a "obra nascendo por cima de outra estrutura": ela nasceu por cima
  dela mesma. Correção: `BlueprintPlacement.resumedBlueprintOf` escolhe o giro que mais casa com o
  que está de pé.
- **Sem apoio:** na sessão anterior a mesma casa ficou 25 min com 1 peça (tocha de parede) e
  "no build task"; nesta, 3 min com 2. A vaga única de obra fica presa. Correção:
  `UnsupportedRelease` — na segunda janela de paciência, até 4 peças sem apoio são largadas e a
  obra fecha, com a linha `VC_UNSUPPORTED`.
- **Itens que só aparecem:** um `VC_SUPPLY_ERROR` (lã). A causa está acima (o baú da profissão
  contado sozinho).

## Alternativas consideradas

| Problema | Alternativas | Escolha |
|---|---|---|
| Mineiro sem ramal | cavar a superfície (ADR-037 M1); ajudar a obra; abrir mais ramais | parar de cavar a superfície (pedido do autor). Ajudar a obra exigiria largar a tarefa de pedra — fica como pendência |
| Construtor fora da obra | o pedreiro continua; reservar a obra só a construtor; reabrir a tarefa do construtor | reabrir a tarefa (Regra 14: o construtor é quem faz a obra; a ajuda segue) |
| Fazendeiro sem trabalho | arar (já existe, pede semente); compostor; ajudar a obra; dormir | ajudar a obra com a roça descansando (Regra 51) |
| Giro na retomada | gravar o giro no save; deduzir do mundo | deduzir do mundo — sem estado novo em disco, como a retomada já faz com o progresso |
| Peça sem apoio | esperar sempre (decisão de 11-10); pôr bloco de apoio; trocar tocha de parede por tocha de chão; largar | largar, só até 4 e só depois de duas janelas; casa a que falta parede não fecha |
| Surgimento com estoque em outro baú | contar a vila inteira (quebra o P0.55 do fundidor); transferir | transferir |

## Pendências (não feitas)

- Lenhador: 39% andando pela regra da árvore mais perto dele. Alternativa a decidir: limitar a
  corrente a N árvores antes de voltar para perto do centro, ou preferir a mais perto da vila
  em distâncias parecidas.
- Fundidor: a frase "none of N colony chests had stone" quando o que barra é a reserva de cru.
- Mineiro: "waiting for a branch" com ramais esgotados — descer o nível mais cedo ou dar o mineiro
  sobrando à obra.
- Nada disto foi jogado: cada correção tem GameTest ou unitário, e o efeito se mede no próximo
  `time_ledger` (Regra 50).
