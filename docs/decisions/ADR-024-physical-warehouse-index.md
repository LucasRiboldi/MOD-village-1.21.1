# ADR-024 - Armazém físico por fotografia (WarehouseIndex)

**Status:** Aceita
**Data:** 2026-09-24

## Contexto

`ColonySupply.take`/`give` já resolvem material físico — baú percorrido do
mais próximo, fabricação de dois degraus quando falta — mas cada chamada lê
o mundo de novo, sem noção de "o que já foi prometido nesta passagem do
ciclo". Duas tarefas que pedem o mesmo recurso escasso no mesmo ciclo podem
ambas ver estoque suficiente e ambas tentar retirá-lo, porque nenhuma sabe
da outra.

A Task 8 do plano de confiabilidade operacional (Decision 4B) pede um
contrato puro — sem `Item`, sem baú, sem `ServerWorld` — que modele uma
fotografia do estoque físico e reserve contra ela dentro do mesmo ciclo,
antes que a integração (Task 9) precise decidir onde cada bloco físico
mora.

## Decisão

`WarehouseIndex` é uma fotografia imutável de quantidade por `ResourceId`,
construída como `complete` (toda leitura de baú reconhecido terminou) ou
`incomplete` (algum baú ficou em chunk descarregado). A fotografia em si
nunca muda; o que muda é a reserva por cima dela.

`reserve(SupplyRequest)` é *greedy* por ordem de chamada: compromete
estoque na hora, sem olhar prioridade. `reserveBatch(List<SupplyRequest>)`
ordena os pedidos por `SupplyPriority` antes de reservar — construção
ativa, trabalho de profissão/ferramenta ativo, alívio de capacidade,
objetivo de estoque geral — para que um pedido de prioridade baixa nunca
consuma estoque escasso antes de um de prioridade alta, mesmo chegando
primeiro na lista.

Uma fotografia incompleta recusa toda reserva com
`SNAPSHOT_INCOMPLETE`, mesmo quando o que foi lido pareceria bastar: o que
faltou ler pode ser exatamente o que decidiria. `invalidate()` esquece toda
reserva sem alterar o estoque visto — chamado ao fim de cada ciclo, para
que reserva de um ciclo nunca vaze para o seguinte.

**Emenda 2026-09-28 — materializacao fatiada e prioridade compartilhada.** Na
integracao Fabric, `ColonyChestSurvey` le no maximo oito baus por colonia em
cada rodada e guarda somente um cursor de memoria ate a rodada terminar. Baus
sem vinculo de profissao sao lidos antes dos baus de profissao, preservando a
ordem de distancia dentro de cada grupo.

Enquanto houver baus pendentes, o ciclo pode decidir reserva, pedido,
planejamento e fabricacao com somente as quantidades que ja foram lidas. Bau
ainda nao lido nao vira saldo zero, sobra presumida nem dado persistido; a
rodada e descartada ao fechar, quando a lista de baus muda e no ciclo de vida
do servidor. Leitura parcial causada por bau inalcançavel continua bloqueando a
decisao, pois nao e uma continuacao normal de rodada e exige diagnostico fisico.

**Sem estoque virtual.** Todo recurso que o índice conhece veio de leitura
física de baú reconhecido. A única exceção documentada é a **ADR-022**: uma
peça final sintetizada quando nenhuma alternativa da família tem rota
local no bioma. Essa peça entra no baú da obra como depósito físico comum
antes de qualquer consulta a este índice — ela não é um atalho do
`WarehouseIndex`, é o que o `WarehouseIndex` acaba enxergando depois que o
depósito acontece.

## Consequências

- Duas tarefas do mesmo ciclo não podem prometer o mesmo estoque escasso
  duas vezes.
- Prioridade é decisão explícita e testável, não ordem de chegada acidental.
- Bau ainda pendente nunca e tratado como "zero" nem como "sobra o bastante";
  a decisao usa apenas o que foi observado. Bau inalcançavel continua bloqueado
  e informa o motivo.
- O contrato não sabe onde um baú fica nem que item Minecraft representa um
  `ResourceId`; isso é responsabilidade de
  `fabric.integration.WarehouseObserver` (Task 9), que nunca entra em
  `core`.

## Verificação

`WarehouseIndexTest` cobre: reserva impede dupla contagem no mesmo ciclo,
motivo de bloqueio por estoque insuficiente, fotografia incompleta bloqueia a
reserva do contrato puro, recurso nunca visto e zero (nao erro),
`invalidate()` limpa reserva sem tocar estoque, `reserveBatch` ordena por
prioridade enquanto `reserve` sozinho é *greedy*, a ordem de
`SupplyPriority` é explícita, e quantidade zero/negativa é recusada.
`StorageGameTest.aSlicedSurveyPublishesTheLastChestOnlyAfterItsRoundCloses`
prova que o conteudo do nono bau nao aparece na fotografia antes do fechamento
da rodada.
`StorageGameTest.pendingSurveyReadsSharedChestBeforeProfessionStorage` prova
que um bau compartilhado e consultado antes dos baus vinculados a profissao.
