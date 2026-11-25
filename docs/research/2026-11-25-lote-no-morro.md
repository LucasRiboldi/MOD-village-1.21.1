# Lote no morro — E3 do jogo da 0.4.139 (2026-11-25)

Pedido do autor: pesquisar alternativas para a vila do deserto na montanha (mundo 1, colônia f6350e62), onde nenhuma obra
coube. Este documento só levanta as alternativas; nada foi decidido.

## O que o log mostra

- A vila gerada vai da altura 66 à 165 (caixa 94×135, centro em y 125).
- Em 15 minutos de jogo o mod recusou 27.411 candidatos e aprovou 0 (`lot refusals`, 16:24:59):
  - 14.302 (52%) "sem chão na janela de altura" (`NO_GROUND`);
  - 9.930 (36%) "fora do nível da rua" (`OFF_ROAD_LEVEL`): 5.633 a 3 abaixo, 1.288 a 4, 779 a 5, 681 a 6, 965 a 7 e
    584 a 8;
  - 2.873 (10%) "chão que não é solo natural" (laje, degrau e vaso da vila);
  - 146 da Regra 3, 83 de rua reservada, 72 perto demais, 5 de cama.
- A faixa anexada a leste teve 544 pontos de porta testados, 0 lotes e nenhum com caminho de até 32 blocos.
- O curral não achou lote em nenhuma varredura (7 vezes), e a casa do mineiro também não (26 buscas locais).
- O construtor ficou 100% parado.
- Ressalva: foram só 15 minutos de jogo. A varredura fez 20 passadas sobre 1.715 colunas e não completou nenhuma volta.

## Por que recusa

O lote é medido pela rua ao lado dele (`RoadsideSites.siteFor`, `roadY`). Em `LotLevel.flatGroundAt`, cada coluna da
pegada precisa ter:

- chão de até `LotTerraform.MAX_FILL` = 2 abaixo da rua;
- chão de até `LotTerraform.MAX_CUT` = 5 acima da rua, e nada acima de +6 (`LotGround.groundInColumn`).

Uma coluna fora dessas faixas reprova o lote inteiro. Numa montanha, a rua corre numa prateleira: de um lado o morro
sobe mais de 5 (`NO_GROUND`), do outro o chão cai 3 ou mais (`OFF_ROAD_LEVEL`). Quase nenhuma pegada de casa cabe
inteira nessa faixa de 8 blocos.

## Alternativas

1. **Abrir os limites.** Aterro até 4 e corte até 8, só com chão do bioma (Regra 111). Pela distribuição acima, o aterro
   de 4 cobre 6.921 das 9.930 recusas de nível, mas o lote ainda precisa passar coluna por coluna. Risco: aterro alto vira
   muro de terra ao lado da casa, que o plano de alturas (Regra 357) depois suaviza.
2. **Lote pelo custo.** Troca o "passa ou não passa" por coluna pela soma de corte e aterro da pegada. A colônia escolhe o
   lote mais barato abaixo de um teto, e esse lote entra como âncora no plano de alturas (ADR-063). O Zelador prepara o
   platô antes da obra. Risco: muda a escolha de lote em todo bioma; precisa de ADR, porque mexe nas Regras 19 e 111 e na
   fase 1 da terraplanagem.
3. **Afrouxar em degraus só quando nada cabe.** Depois de uma volta inteira sem lote, a colônia sobe um degrau na régua:
   aterro 3 e corte 6, depois aterro 4 e corte 8. A régua volta ao normal quando um lote sai. Uma linha `VC_LOT_RELAX`
   mostra o degrau. Nas vilas planas nada muda. Risco: a primeira volta completa pode demorar; no mundo 1 nenhuma volta
   completou em 15 minutos.

Fora da conta: a casa sobre pilares ou em dois níveis exigiria plantas novas, e a busca de lote fora da rua mudaria a
Regra 17 (a porta dá para a rua).

## Revisão pedida pelo autor: dois lados acessíveis

Pedido (25-11): afrouxar no sentido de que a estrutura seja acessível, com degrau de 1, por pelo menos 2 lados, e o
da entrada sempre seja um deles.

### Outra causa, achada na revisão

Durante os 15 minutos inteiros (12 vezes, de 16:12 a 16:24), o construtor esperou com "still sweeping — the budget ran
out before an answer — local search for the miner home is still in progress". A busca local da casa do mineiro roda em
volta da boca da mina, e a boca estava enterrada a 22 blocos do topo (E2). A janela de altura em volta de y 127,
dentro do morro, só acha `NO_GROUND`. A busca terminou 26 vezes sem lote e ocupou o orçamento da varredura. A Regra 359
(0.4.140) já põe a boca no chão aberto; isso deve destravar esta parte, mas só o próximo jogo confirma.

### Como a regra fica

- A pegada continua com a base na altura da rua, e a porta dá para ela (Regras 17 e 19).
- Cada coluna da pegada pode ser cortada até `CUT` acima e aterrada até `FILL` abaixo, só com o chão do bioma (Regra 111).
- A régua por coluna da borda acaba. No lugar, conta-se quantos lados são acessíveis:
  - o lado da entrada é acessível por construção, porque encosta na rua;
  - outro lado é acessível se uma coluna logo fora dele está a até 1 da base e dela se anda, com degrau de no máximo 1,
    até a rua da entrada sem passar pela pegada, em até 16 passos.
- O lote vale com 2 lados acessíveis ou mais. Os outros lados podem ser barranco; o plano de alturas (Regra 357) os marca
  como conflito para a rampa da fase 4.

### Simulação no save do mundo 1

Os scripts `scripts/research/lot_terrain_dump.py` e `scripts/research/lot_two_sides_sim.py` leem o topo de cada coluna da
caixa da vila no save e testam pegadas quadradas encostadas em cada coluna de rua (3 posições ao longo dela).

| Pegada | Régua de hoje | 2 lados, aterro 4, corte 8 | 2 lados, aterro 8, corte 8 | 3 lados, 8 e 8 |
|---|---|---|---|---|
| 5×5 | 228 | 350 | 466 | 329 |
| 7×7 | 64 | 235 | 367 | 252 |
| 9×9 | 16 | 124 | 224 | 143 |

Limite da simulação: a régua de hoje deu 228 lotes 5×5 no modelo, e o jogo achou 0. O modelo é mais generoso que o mod:
ele toma toda coluna de arenito liso ao nível da areia como rua, não confere o espaçamento entre obras, a reserva de rua
nem a Regra 3 bloco a bloco, e não sofre o orçamento da varredura. Os números servem para comparar as réguas entre si,
não como previsão de quantos lotes o jogo vai achar.
