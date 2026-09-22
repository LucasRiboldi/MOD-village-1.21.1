# ADR-022 - Suprimento de construcao sem rota no bioma

**Status:** Aceita
**Data:** 2026-09-22

## Contexto

Uma obra pode exigir uma peca cuja receita Vanilla depende de um recurso sem
origem fisica para a colonia no bioma atual. O caso que motivou a decisao foi
o `brewing_stand`: sem rota para haste de blaze, o carpinteiro nunca poderia
abrir a receita e a obra permanecia em `WAITING_RESOURCES`.

A politica anterior de exigir que o jogador fornecesse esse insumo conflita com
a regra definida pelo autor: todo recurso ou bloco de construcao sem rota no
bioma deve aparecer no bau do construtor.

## Decisao

Para cada peca de construcao ausente, a integracao considera toda a familia de
alternativas aceita pela obra. Cada alternativa e resolvida recursivamente por
receitas Vanilla de criacao, corte de pedra e fundicao, ate recursos diretos
que a colonia sabe obter no bioma.

Se ao menos uma alternativa possuir rota local, nada e criado: os oficios e a
cadeia normal continuam responsaveis. Se nenhuma alternativa possuir rota, uma
unidade da peca preferida e depositada por demanda no bau da colonia mais
proximo da origem da obra, que e o mesmo fornecedor consultado pelo construtor.
Enquanto a unidade estiver la, ela nao e duplicada. O bloco final, e nao
ingredientes inventados, e entregue.

Esta decisao substitui a secao 3.3 da ADR-009 somente para requisitos de
construcao. Ela nao muda a economia de comida, ferramentas, trocas ou metas
gerais da colonia.

## Consequencias

- Obras deixam de esperar indefinidamente por recurso de outro bioma, dimensao
  ou origem desconhecida.
- Rotas locais, inclusive alternativas de material, continuam tendo prioridade
  e preservam o trabalho das profissoes.
- Conteudo de mod ou datapack sem rota conhecida segue a contingencia e recebe
  a peca final, desde que haja espaco no bau da obra.
- Um bau sem espaco ainda impede a entrega; o evento fica registrado para
  permitir diagnostico sem perda de item.

## Verificacao

`BuilderGameTest.unobtainableConstructionPieceIsStockedForTheBuilder` prova o
fermentador sem haste de blaze. `locallyCraftablePieceStillWaitsForWorkers`
prova que uma porta de carvalho em planicie nao e materializada.
