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
