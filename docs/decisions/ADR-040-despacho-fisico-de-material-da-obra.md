# ADR-040 - Despacho fisico de material da obra

**Status:** Aceita
**Data:** 2026-10-10
**Decision Type:** Gameplay + Architecture
**Origem:** opcao B escolhida pelo autor para reduzir material instantaneo,
eliminar esperas por rotas apenas teoricas e manter a obra fluida.
**Amends:** ADR-035 secao 3, fase 2; ADR-022; ADR-036 item 6.

## Contexto

`MaterialRequest` tornou visivel a proxima peca que a obra espera, mas ficou
expressamente observacional na fase 1 da ADR-035. O suprimento ainda decide por
`hasRouteInBiome`: quando a resposta e verdadeira, a obra espera uma profissao
indefinidamente, mesmo se nenhum trabalho executavel foi despachado ou nenhum
item chegou a um bau que a atende. O playtest registrou 334 insercoes terminais
nessa area e uma obra pode ficar parada pelo caso oposto: uma rota teorica que
nunca entrega.

## Decisao

1. Cada proxima peca ausente de uma obra aberta tera um registro efemero de
   despacho, indexado por projeto e item. Ele nao duplica inventario nem e
   salvo: o mundo e os baus continuam sendo a fonte da verdade.
2. Uma tentativa conta somente quando o ciclo da colonia abriu ou renovou uma
   demanda executavel para a cadeia da peca. Consultas repetidas do construtor
   e reservas de tarefa nao contam.
3. Entre duas tentativas, a cadeia so e considerada entregue quando a peca ou
   uma alternativa aceita aparece fisicamente em um bau que atende a obra.
   Esse evento zera a contagem. A aparicao por jogador tambem e entrega fisica,
   mas nao recebe atribuicao de profissao sem evento de deposito comprovado.
4. Apos quatro tentativas despachadas sem entrega (duas, um minuto, desde a
   segunda emenda de 2026-10-11, abaixo), a peca terminal ou o
   ingrediente sem rota e depositado pelo caminho ja definido na ADR-036 item
   6, no bau da profissao que a usa; se nao houver oficio produtor, no bau que
   atende o construtor. A linha `VC_SUPPLY_ERROR` permanece obrigatoria.
5. `SupplyRoutes` continua explicando a cadeia, mas deixa de ser prova de
   entrega. Uma rota sem executor nao pode impedir o contador de fallback.
6. A telemetria separa `STOCK` observado, entrega profissional com evento de
   deposito e `FALLBACK` terminal. Nao e permitido inferir profissao de uma
   pilha anonima ja presente no bau.

## Consequencias

- Uma obra nao materializa a peca na primeira leitura: quatro ciclos reais de
  demanda preservam a prioridade da producao e coleta locais.
- Uma rota que nunca se transforma em trabalho ou entrega deixa de ocupar a
  obra indefinidamente.
- A implementacao deve ligar o registro ao ciclo que cria as demandas e ao
  deposito fisico, e nao a `BuilderMaterials.hasMaterialForNextBlock`.
- A regra nao altera folhas: a decisao de nao colher a copa permanece, e elas
  alcancam o fallback somente depois das quatro tentativas da cadeia aplicavel.

## Verificacao

Um GameTest deve provar que quatro consultas do construtor sem novo despacho
nao esgotam a tolerancia; quatro ciclos com demanda executavel e sem entrega
abastecem o destino correto; e um deposito fisico antes do quarto ciclo zera a
contagem. O proximo save deve confirmar a queda de `VC_SUPPLY_ERROR`, a
progressao da obra e os percentuais de `time_ledger.py`.

## Emenda de 2026-10-11 (P9, autossuficiencia)

Escolha do autor: a peca que surge no bau e o ultimo recurso. O prazo da obra
passa de quatro para dez ciclos com tarefa de rota aberta e sem entrega
(`BiomeConstructionSupply.DISPATCHES_BEFORE_STOCKING`), cerca de 5 minutos.
Medida que o sustenta: no playtest de 11-10 a mina entregava ~13 pedregulhos
por minuto; 5 minutos sem uma unidade e rota que nao entrega. A busca vazia de
recurso natural (`LocateFallback`) continua com quatro.

## Segunda emenda de 2026-10-11 (depois do playtest da 0.3.25)

Regra do autor: se em um minuto nenhuma rota avancou em conseguir o bloco, ele
surge no bau do construtor. O prazo passa a dois ciclos (`DISPATCHES_BEFORE_STOCKING
= 2`), revertendo os dez da primeira emenda.

## Emenda de 2026-10-20 (G2, depois da bancada de simulação por bioma)

Regra do autor: *"se não tiver material nos baús, se não tiver rota ou a rota para adquirir
travar, registrar o travamento e fazer todos blocos necessários aparecerem no baú do
construtor. Guardar os erros desta função sempre para no futuro todos itens terem uma rota
própria de serem adquiridos."*

A bancada mostrou o construtor 58% bloqueado e 107 a 469 "SET_ASIDE: no material" por vila,
com a regra da segunda emenda escrita. Duas brechas:

1. **Um contador por obra, zerado quando o item mudava.** O construtor põe de lado a peça sem
   material e passa à seguinte; o item da vez trocava de ciclo em ciclo e a contagem nunca
   passava de 1. Agora cada peça que trava a obra — a da vez, se a obra espera, e as postas de
   lado por material — tem o próprio contador (`ConstructionSupplyDispatches`).
2. **Rota de papel sem tarefa aberta não contava** (§2 desta ADR). Revogado: rota que ninguém
   executa é rota travada.

**Travou** é passar `DISPATCHES_BEFORE_STOCKING` ciclos sem progresso — a peça não aumentou nos
baús e a obra não assentou mais dela; rota lenta que entrega não dispara nada. Ao travar, tudo
o que a obra ainda pede da peça aparece no baú do construtor.

**Registro permanente:** toda peça que surge sem a profissão a ter obtido — desta regra, do
artesão e da busca vazia — vai para `<pasta do jogo>/villagecolony/supply-gaps.jsonl`
(`SupplyGapLedger`), com o motivo: `NO_ROUTE`, `NO_TASK`, `STALLED` ou `NOT_FOUND`. O arquivo
só cresce e fica fora de `logs/`. `scripts/supply_gaps.py` o resume como a lista de itens que
ainda pedem rota própria.
