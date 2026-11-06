# ADR-047 — A obra MAE entra na fila do construtor

**Estado:** aceita pelo autor em 2026-11-06. Emenda as ADR-044, ADR-045 e ADR-046. Regra 213.

## Contexto

A obra pedida por `/vc mae obra` era escrita pelo próprio MAE, sem aldeão, e conferia a cada passo a impressão das seções inteiras.
Em jogo, as 5 obras da 0.4.35 pararam em `world_changed_since_approval`, com 2 a 27 blocos postos, e ficaram como ruína (Regra 205).

Reproduzido em GameTest, 3 de 3: a grama coberta por um bloco da obra vira terra no tique aleatório (`SpreadableBlock.randomTick`), e
a impressão muda. Os testes nunca viram isso porque o `TestServer` roda com `RANDOM_TICK_SPEED` 0. Não foi medido quanto das paradas do
jogo vem da grama e quanto de aldeão, água ou plantação na mesma seção: o log só guarda a mensagem final.

A ideia do mod é o contrário de um mundo parado: os aldeões constroem, e o mundo é a fonte da verdade.

## Decisão do autor

1. O mundo vivo não para a obra.
2. A obra não deixa blocos soltos: sempre termina.
3. A obra é a próxima construção na fila do construtor.

## Como

- `MaeSites` confere o lote uma vez, com a limpeza e a proteção de sempre (`MaeClearing.survey`), e põe o pedido em `MaeBuildQueue`,
  gravada no save. A conferência cobre o lote com folga de 3 blocos, e não a região de 12.
- `ConstructionPlanner.plan` abre o primeiro pedido antes do reparo e da busca de lote, quando a vaga está livre e há construtor.
  `SiteOpening.openPlanned` é o mesmo trecho de toda obra: preparo do lote, chão em volta e tarefa `BUILD`.
- Daí em diante é uma obra comum: material dos baús e bloco a bloco. Peça com outro bloco no lugar é pulada (Regra 3), sem parar a obra.
- Pedido em andamento não é largado por distância nem por espera. Obra que some sem terminar volta para a fila. Lote que não abre mais
  procura outro perto.
- Só o jogador encerra um pedido: `/vc mae parar` ou a tocha das almas (Regra 108). O que já foi posto fica.
- Região e pedido de versão anterior são soltos sozinhos: o pedido vira fila, e a região volta para a vila. Escrita sem confirmação
  continua presa até a análise (ADR-044).

## Consequências

- O grafo não congela durante a obra, porque não há região reservada.
- A prova por seção não vale durante a obra pedida. Uma mudança do jogador no lote aberto só é vista quando o construtor chega naquele
  bloco, como em toda obra da colônia. Este é o preço aceito.
- A obra pedida passa a depender de construtor na vila, e espera a obra em andamento.
- O plano do lote é o do construtor (`LotTerraform`, `FoundationPreparation`), e não o do `terrain_v2`.
- `MaeStructureSite`, `MaeStructureExecution`, `MaeTerrainExecution` e `road_v2` deixam de ser chamados pela obra. Continuam no código
  com os testes deles; a remoção é decisão à parte.
- Fica sem efeito para a obra pedida: Regra 187 (entrega da região), Regra 203 (tanque numa escrita só; a água entra pelo construtor) e
  Regra 205 (obra falha devolve a região).

## Emenda de 2026-11-06 (autor)

A escrita própria do MAE saiu do código (varredura, 4B), e o lote usa só a régua do construtor (8A). A prova de geração continua sendo calculada em chunk novo, sem nenhum leitor: fica até o autor decidir. Região de versão anterior vira pedido e sai do save, mesmo com escrita sem confirmação, porque não há mais quem a analise.
