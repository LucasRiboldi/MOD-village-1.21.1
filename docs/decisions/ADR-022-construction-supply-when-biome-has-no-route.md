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

## Emenda N3 - 2026-09-24: equivalente antes da peca pronta

Decisao do autor na revisao de naturalidade
(`docs/technical/Revisao-Naturalidade-2026-09-24.md`): *"trocar por
equivalente; nao existindo no bioma, dai fazer o item nascer no bau pronto"*.

A regra desta ADR ja consultava a familia inteira antes de criar a peca, mas
a familia so existia para madeira, cama e terracota. Agora ela tambem inclui:

- as familias marcadas em tag do jogo: `fence_gates`, `buttons` (o botao de
  madeira continua trocando so por madeira), `wool`, `wool_carpets`,
  `banners`, `saplings` e `small_flowers` (`MaterialChoice`);
- as familias que o jogo so agrupa pelo nome (`EquivalentPieces`):
  - escada, laje e muro de pedra, comecando pelo pedregulho e incluindo
    arenito;
  - pedra musgosa, que cai para a pedra comum;
  - granito, diorito e andesito;
  - vidraca e vidro, com as cores e o incolor por ultimo;
  - terracota vitrificada, que cai para a terracota;
  - madeira descascada de qualquer especie;
  - grama e samambaia, baixas e altas.

O preferido continua primeiro. A peca so nasce pronta quando nenhum membro da
familia tem rota no bioma.

Medido pela `ConstructionSupplyAuditGameTest`: as entradas de fornecimento
automatico por estilo cairam de **119 para 102**. As 102 restantes sao
estacoes de trabalho, pecas de madeira no deserto, gelo e neve, plantas e
chao, e nenhum parente delas tem rota no bioma. Verificacao unitaria:
`EquivalentPiecesTest` (7 casos).

## Emenda — 2026-09-26: relógio salvo e receita de tingir ignorada

Decisão do autor depois da sessão de 26-09, em que a obra esperou o tapete
verde e o jogo fechou 30 s antes de os 10 ciclos vencerem:

- **O relógio de rota atrasada é salvo com o mundo** (`WorkMarksSavedData`,
  lista `supplyWaits`). O início é contado em tiques do mundo, que o jogo
  salva, então a espera continua de onde parou.
- **Receita de tingir peça pronta é ignorada** (`RecolorRecipes`, no `core`):
  o livro devolvia "corante verde + tapete de outra cor" antes da receita de
  lã. Tingir lã e misturar corante continuam, porque são produção.
- A regra "a peça decorativa aceita a primeira da família que estiver no
  baú" já valia (`MaterialChoice`, tag `wool_carpets`); ficou presa por
  `CarpetFamilyGameTest`.
