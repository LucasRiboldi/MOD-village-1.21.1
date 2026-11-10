# Qualidade do serviço das profissões — jogos de 7 a 9 de novembro

Pedido do autor em 2026-11-10: entender a qualidade do serviço, o peso das atividades e como a vila pode crescer mais natural.
**Só análise: nada foi corrigido.** Aguarda a escolha do autor (`CLAUDE.md` §0.2.2).

## Fontes

- **Logs:** 7 logs da 0.4.43 (`2026-11-07-2` a `latest.log`), cerca de 15 horas de jogo, 5 colônias.
- **Diários:** 5 diários de ações do mesmo período (`villagecolony-actions-20261107-090434` a `20261108-200548`), 151 horas-aldeão.
- **Limite:** o código jogado é o da 0.4.43; as 0.4.44 a 0.4.46 ainda não foram jogadas.

## Resumo em números

- **Só 11% do expediente é trabalho.** Na média ponderada por hora, 28% é andar e cerca de 43% é ócio, espera ou bloqueio.
- **Nascimentos:** cerca de 1 aldeão por hora quando há algum. Os casos: 19 para 22 em 2 h 27, 15 para 20 em 8 h, 12 para 16, e 13 parado em 3 sessões.
- **Obras:** 106 obras abertas em cerca de 15 h. O construtor trabalha 40% do tempo e fica 19% bloqueado.

## Tempo por ofício (`VC_TIME`, todos os logs)

| Ofício | Horas | Trabalho | Andando | Esperando | Bloqueado | Ocioso | Parado |
|---|---|---|---|---|---|---|---|
| Mineiro | 17,6 | 20% | 33% | 13% | 4% | 28% | 33% |
| Pastor | 14,5 | 0% | 22% | 1% | 4% | 72% | 77% |
| Pedreiro | 13,6 | 13% | 35% | 22% | 2% | 27% | 30% |
| Fundidor | 13,2 | 8% | 11% | 13% | 22% | 46% | 68% |
| Carpinteiro | 12,3 | 9% | 16% | 19% | 0% | 56% | 56% |
| Lenhador | 9,3 | 5% | 43% | 0% | 3% | 49% | 52% |
| Fazendeiro | 6,1 | 3% | 45% | 39% | 1% | 11% | 13% |
| Construtor | 6,1 | 40% | 24% | 2% | 19% | 14% | 34% |
| Sem ofício | 5,0 | 3% | 18% | 0% | 3% | 75% | 78% |

"Parado" é ocioso + bloqueado + encalhado; a Regra 51 pede no máximo 15%. Só o fazendeiro fica abaixo, e mesmo assim trabalha 3%.

## Produção, percurso e tarefas (diários)

| Ofício | Aldeões | Metros andados por hora | Produção por hora | Tarefas pegas / feitas / soltas |
|---|---|---|---|---|
| Mineiro | 24 | 519 | 431 blocos minerados | 193 / 9 / 164, e 120 desistências |
| Pedreiro | 16 | 328 | 84 nivelados, 67 fabricados, 35 calçados | 1124 / 1112 / 0 |
| Pastor | 17 | 91 | 6,9 tosquias | 94 / 41 / 46 |
| Fundidor | 15 | 6 | 153 fundidos, 30 fabricados | 690 / 281 / 401 |
| Carpinteiro | 16 | 74 | 90 fabricados | 917 / 905 / 0 |
| Lenhador | 11 | 854 | 102 árvores derrubadas | 104 / 20 / 80 |
| Construtor | 8 | 1830 | 502 blocos postos, 44 postos de lado | 122 / 95 / 21 |
| Fazendeiro | 7 | 550 | 87 colheitas | 39 / 0 / 32 |
| Sem ofício | 7 | 804 | 124 calçados, 34 carregados | — |

## Achados

### 1. O material errado é fabricado enquanto a obra espera (alta)

- O construtor pôs de lado 743 vezes "no material": `smooth_sandstone` 381, `acacia_log` 110, `cut_sandstone` 63, `oak_log` e `stripped_oak_log` 29 cada.
- No mesmo período o fundidor fundiu `stone` 1971, `smooth_sandstone` 1397 e `smooth_stone` 998. O pedreiro fez `sand` 1046.
- O fundidor soltou 401 de 690 tarefas; 234 por "no raw for the furnace in any chest — the mason makes it".
- **Causa provável:** a fila de fabricação não põe a peça que a obra aberta espera à frente do estoque. Falta conferir no código a ordem em `CraftingWork` e `ValueGoods`.

### 2. O estoque de enfeite gasta o tempo dos artesãos (alta)

- Os artesãos tentam fazer, para o estoque, itens que não conseguem: `raw_copper_block` 13 vezes, `coal_block` 11, `waxed_chiseled_copper` 8, `hopper` 8, vidros coloridos, portas e escadas de cobre.
- Fazem também velas das 8 cores (16 de cada) e `sea_lantern`, este último com prismarinho que surge sozinho (`VC_SUPPLY` `AUTOMATIC_DROP`, 144 no jogo de 7 de novembro).
- Carpinteiro 56% ocioso e fundidor 46%, mesmo assim.

### 3. O lenhador anda mais do que corta (alta)

- Árvore a 53 blocos na mediana, 104 no p90 e 194 no máximo (1872 leituras).
- 854 metros andados por hora, 43% do tempo andando e 5% trabalhando.
- Soltou 80 de 104 tarefas: 54 por "made no progress" e 24 por "got no closer to the tree".
- **Causa provável:** a conta de "sem progresso" corre durante a caminhada longa, e a árvore perto já foi cortada.

### 4. O mineiro cava sem precisar (média)

- 24 mineiros em 5 colônias, 431 blocos por hora cada. O pedregulho no estoque foi de 554 a 13.579 em 16 minutos no jogo de 7 de novembro.
- Soltou 164 de 193 tarefas e desistiu 120 vezes ("has not moved a block", "got no closer").
- **Causa provável:** não há teto de estoque de pedra, e há mais mineiros que pedra pedida.

### 5. Pastor e sem ofício quase sem serviço (média)

- **Pastor:** 72% ocioso, com 64 esperas "NO_TASK"; 30 vezes preso no curral ("fenced in"); 25 vezes "no sheep with wool within the radius".
- **Sem ofício:** 75% ocioso; quando trabalha, calça (124 por hora).

### 6. O fazendeiro espera e a comida não sobra (alta, porque segura os nascimentos)

- Fazendeiro: 3% trabalho, 45% andando, 39% esperando; 0 de 39 tarefas feitas.
- Esperas: "NO_TARGET" 253 vezes, "SWEEP_INCOMPLETE" 214, e "nothing ripe and no empty plot within 32 blocks".
- 1 fazendeiro por vila; trigo no estoque entre 7 e 41 nas leituras.
- Nascimento depende de comida, e ela é a provável trava da vila crescer.

### 7. O diário registra o item errado (baixa, mas engana a análise)

- `CRAFTED` grava o alvo da tarefa (`dirt`) e não o que foi feito: `{"what":"dirt","detail":"stocked 16 of sea_lantern"}`.
- Origem: `CraftingWork.java:434` usa `job.task.targetResource()`.

## Peso das atividades e equilíbrio

Hoje o peso não segue a necessidade da vila:

- **Sobra:** mineiro (pedra sobrando), pedreiro e fundidor (estoque fora do que a obra pede), pastor (lã sem demanda).
- **Falta:** fazendeiro (comida, que segura nascimentos), lenhador (madeira longe) e a peça certa para o construtor.

Uma vila "viva" deveria pôr o peso onde a falta aparece, com três regras simples:

1. A obra aberta manda na fila de todo artesão.
2. O estoque tem teto por material, e acima do teto o ofício descansa ou muda para o produto que falta.
3. A vaga de cada ofício segue a necessidade: mais fazendeiro quando falta comida, menos mineiro quando sobra pedra.

## Opções de correção, para o autor escolher

1. **Material certo primeiro (achados 1 e 2), recomendada.** A peça que falta na obra aberta vai para a frente da fila do artesão. O estoque de enfeite só sai sem peça faltando, e só com item que dá para fazer. Efeito esperado: menos "no material" e menos ócio de fundidor e carpinteiro. Risco: médio, porque mexe na ordem de fabricação.
2. **Comida e nascimentos (achado 6).** Mais fazendeiros quando o estoque de comida está baixo, plantação nova a até 32 blocos e o pastor criando e abatendo para comida. Efeito esperado: mais nascimentos e uma vila que cresce. Risco: médio, porque mexe em vagas e plantação.
3. **Lenhador e mineiro com medida (achados 3 e 4).** O lenhador prefere o bosque plantado perto da vila, e a conta de "sem progresso" não corre enquanto ele anda. O mineiro ganha teto de pedra e uma vaga a menos quando sobra. Efeito esperado: menos andar à toa e menos estoque inútil. Risco: baixo a médio.
4. **O diário grava o produto (achado 7):** correção pequena, que pode entrar junto com qualquer opção.
