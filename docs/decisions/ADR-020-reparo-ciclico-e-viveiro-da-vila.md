# ADR-020 — reparo cíclico de construções e viveiro da vila

## Status

Aceita em 2026-09-20.

## Contexto

Uma construção abandonada pode deixar blocos válidos no mundo e continuar
ocupando o lote. Abrir uma obra nova sem revisar esse registro deixa esqueletos
permanentes; bloquear a vila até a mesma tentativa funcionar também pode parar
todo o crescimento quando falta material ou acesso.

Cada vila também precisa de uma reserva previsível de árvores da madeira do seu
bioma. A reserva deve ficar fora do centro, mas ainda ao alcance do lenhador,
sem plantar indefinidamente uma muda a cada passagem de trabalho.

## Decisão

1. Antes de escolher uma obra nova, o planejador varre as construções do mod
   registradas para a colônia e reabre no máximo uma tentativa de reparo por
   ciclo. A planta vem do mesmo blueprint e cada posição já ocupada pelo bloco
   esperado é removida da lista de trabalho.
2. Ao terminar ou abandonar a tentativa, o registro de construções é fundido
   pela mesma caixa e mantém a identidade do edifício. Uma tentativa que não
   existe mais é pulada uma vez; no ciclo seguinte a obra volta a ser candidata.
   Assim o reparo é insistente sem criar um bloqueio permanente.
3. O construtor continua sendo o executor do reparo. O pedreiro (`MASON`) é o
   ofício equivalente ao ferreiro no catálogo atual e permanece responsável
   pelas peças de pedra; não se cria uma profissão nova sem uma necessidade
   Vanilla correspondente.
4. Cada vila mantém até dez viveiros válidos. O agricultor e o lenhador podem
   disparar a mesma função; ela usa a muda declarada para o bioma, coloca terra
   enraizada sob a muda e percorre primeiro o anel mais distante acessível,
   recuando somente quando a borda está ocupada.

## Consequências

- Uma obra parcialmente construída não é apagada nem duplicada durante o
  reparo.
- Uma tentativa impossível não impede a próxima construção, mas o projeto
  incompleto permanece elegível para a próxima varredura.
- O contador considera muda ou árvore sobre terra enraizada; árvores naturais
  continuam fora da contagem quando não têm esse marcador.
- O comportamento visual de rota, entrega de blocos e crescimento das árvores
  ainda precisa ser confirmado em um save carregado pelo autor.

## Validação

`ConstructionProjectTest` prova que blocos de pé não voltam para a lista,
`BuildingRegistryTest` prova a fusão sem duplicação, e
`TreeNurseryGameTest` prova o limite de dez árvores. A validação final também
exige `runGametest` e playtest no mundo.

## Emenda — 2026-09-26: lote do lenhador sem árvore

Sessão de jogo de 26-09: numa vila de planície sem árvore natural, o viveiro
plantou uma muda a cada ~6 min e o lenhador cortou 15 toras em 33 min. Quando
o lenhador procura e não acha árvore, o viveiro planta até 4 mudas de uma vez
(`FarmerNursery.BATCH`), no mesmo ritmo de 5 min e no mesmo teto de 10. O
plantio do fazendeiro continua de uma em uma. Prova:
`TreeNurseryGameTest.aLumberjackWithoutTreesGetsABatchOfSaplings`.
