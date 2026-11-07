---
name: decisoes-autor-2026-10-27-pendentes
description: "Decisões do autor (27-10) sobre como corrigir os pontos que ficaram abertos depois da 0.3.95 — fazendeiro, lenhador, volta à casa, mineiro, peças, cercados, escala, ociosidade"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-27T19:59:29.728Z
---

Decisões do autor em 27-10, tomadas depois de [[correcoes-0395-biomas]], a partir das medições nos logs da 0.3.93. Nenhuma foi implementada ainda.

1. **Fazendeiro — registro de canteiros.** A colônia guarda a lista de canteiros e roças: um canteiro entra quando é plantado ou construído e sai quando é quebrado. O fazendeiro consulta essa lista em vez de varrer o terreno. Motivo: "budget ran out" apareceu 622× e a população ficou parada.
2. **Lenhador — cota grande que fecha ao esvaziar.** A tarefa continua com 256 toras, mas termina como CONCLUÍDA quando não há mais árvore madura por perto, em vez de ser solta. Hoje 0% das tarefas terminam.
3. **Volta à casa — trabalho perto da cama.** Reforça a Regra 74: tarefa longe da cama vai para quem mora mais perto, e a troca de cama entre dois aldeões passa a ser forçada. Contexto: ~1.800 teleportes noturnos, quase todos longe e no mesmo nível; houve 95 "moves home" e 0 trocas.
4. **Mineiro — filtrar alvos pela galeria.** Só vira alvo a pedra alcançável do piso da galeria (até 1 bloco acima e sem buraco embaixo). Hoje há centenas de NO_STANDING_POSITION, TOO_HIGH e NO_PATH.
5. **Peças — cama e lã de qualquer cor.** A obra aceita a cama ou a lã de outra cor, com a branca primeiro. O corante só surge pela Regra 41 quando nenhuma cor serve. Na neve, o bloco de neve sai da pá do pedreiro (bola de neve → bloco) e o diorito vem da mina.
6. **Cercados — as três medidas juntas:**
   - cama e baú não podem ficar dentro de área cercada sem saída;
   - o curral é caro na navegação, e só o pastor entra quando a tarefa é ali;
   - o portão aberto por aldeão fecha logo depois.
7. **Escala — lotes candidatos guardados.** A colônia guarda os lotes livres já medidos e reavalia só os que ficam perto de uma mudança. Contexto: 746 mil recusas de "ROAD" e cerca de 250 avisos de ciclo maior que um tique.
8. **Ociosidade — ajudante da cadeia.** Quem fica sem tarefa do ofício ajuda quem abastece a própria cadeia: fundidor e pedreiro buscam pedra ou carregam baús, e o construtor ajuda o lenhador. É a cadeia da Regra 51, ampliada.

**Mina — Regra 119** (f3a4a8f2, pedido do autor em 27-10):
- caracol de 4 lances de 3 degraus, salão de 17×17×3 e quatro ramais em caracol, um por canto, cada um terminando num salão no quadrante diagonal;
- o nível seguinte desce do meio do salão do ramal zero;
- a rede de corredores (ADR-042) foi desligada;
- os invariantes estão no `MineShaftLayoutTest`.

Ainda falta jogar e remover o código morto da rede (NetworkDigging/Route/Safety/Veins, MineNetwork, MineNetworks).

**Feito em 66ce8c2f:**
- **item 1, Regra 121:** o CropPatch consulta o registro inteiro sempre antes da varredura, e o FarmerTill registra o canteiro que ara;
- **item 2, Regra 120:** a tarefa de madeira fecha concluída quando não há árvore no alcance (`TreeFelling.noTreeInReach`).

**Item 4 (mineiro):** a geometria da Regra 119 já põe toda célula planejada a no máximo 2 acima do piso. Os veios (MineVein/MineOreHeading) ainda podem mirar pedra alta, e isso não foi tratado.

**Item 6 feito, Regra 122** (8a2d45c0): `PenFloors` em integration, `LandPathNodeMakerMixin` e `PenLift.leaveBedInPen`.

**Também feitos:**
- item 5, Regra 126 (b467216f), em que a peça colorida sai branca;
- os códigos UNCLASSIFIED (e9f27523);
- a rede de caminhos, fase 2 (30376c9d, Regras 123 a 125).

**0.3.96 fechada** (3e4cfe34): JAR EEB3B4A8… instalado, não jogado.

**Ainda abertos:**
- item 3 (trabalho perto da cama);
- item 8 (ajudante da cadeia);
- D5 (atalho ao chafariz);
- Regra 127 (lote pela centralidade, que é o item 7);
- neve (bloco de neve e diorito);
- o código morto da rede de túneis;
- o veio do mineiro.

**Pedido novo do autor em 27-10:** estudo de uma rede de caminhos da vila, registrada entre as entradas das estruturas e o chafariz, com 3 blocos de largura, terra do bioma, buracos preenchidos e no máximo 1 bloco de desnível. Ele pediu pesquisa e alternativas, ainda sem código.
O estudo foi feito em ca174bdd: `docs/research/2026-10-27-rede-de-caminhos.md`.
- **Recomendação:** grafo explícito com A* de custo de terreno, cada entrada ligada à rede mais próxima, lote escolhido pela rede.
- **Esperando o autor:** as decisões D1 a D9. A D1 trata da Regra 70 (corredor de 1 bloco) contra os 3 de largura; a D2 trata de "nunca aterra" contra aterrar o buraco.

Sem decisão necessária: os códigos UNCLASSIFIED só precisam de classificação no diário (mineiro NO_PATH, ovelha sem lã, preso abaixo, cercado). O smooth_sandstone do deserto funciona, só devagar.

**Why:** o autor quer um funcionamento harmônico e escalável, e pediu para ser consultado antes de cada correção.
**How to apply:** implementar por marco, um commit verificado por item. Numerar como Regras 119 em diante em `docs/RULES.md`. A ordem sugerida é 1, 2, 4, 6, 5, 3, 8 e depois 7, porque o item 7 pede medição de custo antes (ADR-035 §5).
