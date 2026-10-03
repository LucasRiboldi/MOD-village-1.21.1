# ADR-018 - Contrato de fundacao automatica da vila

**Estado:** Aceita pelo autor em 2026-09-20
**Escopo:** deteccao de vila e registro de trabalhadores Fabric 1.21.1.

> A referencia historica a `BREEDER` foi substituida por `SHEPHERD` na
> [ADR-021](ADR-021-pastor-canonico-e-migracao-do-criador.md). Os seis
> conjuntos fundacionais permanecem iguais.

## Contexto

Uma vila detectada podia existir com menos adultos, sem cama vinculada e sem
bau proprio para uma ou mais funcoes. Isso deixava a economia aguardando uma
profissao que nunca seria criada e fazia a regra de armazenamento depender de
um acaso da geracao Vanilla. Colocar camas e baus espalhados tambem permitia
que a fundacao ocupasse estruturas existentes.

A ADR-011 tratava o construtor como capacidade temporaria e descrevia sete
profissoes produtoras. Essa decisao continua valida para crescimento de
producao, mas nao cobre a fundacao minima exigida para toda vila.

## Decisao

1. Toda vila adotada pelo mod recebe, na primeira deteccao, no minimo um
   adulto para cada funcao de `ProfessionAssigner.FOUNDATION_ORDER`:
   `MINER`, `LUMBERJACK`, `MASON`, `SMELTER`, `BREEDER` e `BUILDER`.
   `CARPENTER` e `FARMER` continuam na ordem de crescimento, mas nao ocupam
   conjuntos da casa fundacional.
2. `SHEPHERD` de saves antigos e normalizado como `BREEDER` para esta regra;
   ele nao cria uma nona vaga.
3. Toda vila recebe uma estrutura exclusiva do mod chamada `BigHouseMOD`.
   Ela e uma copia editada da big house Vanilla: a entrada Vanilla continua
   existindo e nao e alterada; o blueprint do mod remove moveis e decoracoes e
   mantem somente seis camas e seis baus, em um arranjo com passagem livre.
4. Cada trabalhador fundacional deve ter uma cama Vanilla exclusiva dentro da
   `BigHouseMOD`, gravada na memoria `HOME`, e um dos seis baus distintos da
   casa registrado no `StorageRegistry`.
5. A fundacao pode criar aldeoes adultos e colocar a `BigHouseMOD`, mas somente
   em um lote vazio e seguro. Ela nunca sobrescreve blocos existentes do
   jogador, da vila ou de estruturas Vanilla.
6. A mesma garantia e idempotente e roda novamente quando a vila e detectada;
   mortes ou dados ausentes podem ser repostos sem duplicar uma funcao que ja
   tenha titular vivo.

## Consequencias

- A populacao inicial minima da colônia passa a ser seis adultos funcionais
  alojados em uma estrutura do mod.
- Os seis baus da `BigHouseMOD` sao privados dos moradores da casa e nao entram
  no estoque publico da vila; cada um continua reservado a um trabalhador.
- O crescimento posterior continua usando as quotas produtoras da ADR-011;
  `BUILDER` e uma funcao fundacional permanente, mas a capacidade de executar
  `BUILD` continua sendo tratada pelo fluxo de construcao.
- A criacao evita acavalamento. A vila ainda nao e adotada se nao houver local
  seguro para a casa; a proxima deteccao tenta a adocao inteira novamente e
  registra um `WARN`.
- O GameTest `BigHouseModBlueprintGameTest` prova o conteudo da planta, e
  `VillageFoundationGameTest` prova a colocacao, as seis funcoes e os seis
  registros distintos em terreno isolado. A existencia e o posicionamento no
  save do autor continuam exigindo playtest.

## Verificacao

`./gradlew.bat runGametest` passou; a rodada final executou 384/384 GameTests,
incluindo `BigHouseModBlueprintGameTest` e `VillageFoundationGameTest`. O
playtest no save do autor continua pendente.

## Emenda N1 - 2026-09-24: sem reposicao de mortos

Decisao do autor na revisao de naturalidade
(`docs/technical/Revisao-Naturalidade-2026-09-24.md`). Substitui o item 6 da
Decisao e ajusta os itens 1 e 5.

1. A fundacao povoa a vila **uma vez**. Ela roda quando a colonia nasce ou
   quando a `BigHouseMOD` acaba de ser colocada. Em nenhum outro ciclo ela cria
   aldeao, e um titular que morre nao e reposto.
2. Quando a `BigHouseMOD` e colocada, nasce **um adulto para cada cama** da
   casa, mesmo que a vila Vanilla ja tivesse adultos. A cama recebe o bilhete
   do ponto de interesse do Vanilla, e a memoria `HOME` aponta para a cabeca da
   cama, como no Vanilla. Por isso a morte do morador devolve a cama ao jogo.
3. Sem lote para a casa, a vila recem-detectada nao e registrada como colonia
   e nao recebe adulto, cama ou bau avulso.
4. Depois da fundacao, a vila cresce por **procriacao Vanilla**. Uma vez por
   dia, ao fim do expediente, e so se houver cama sobrando, `VillageMeals` tira
   comida dos baus da colonia (pao, cenoura, batata, beterraba) e poe no
   inventario de cada adulto ate os 12 pontos que o Vanilla exige. O filhote
   cresce e recebe profissao pelo fluxo normal de atribuicao.

Consequencia aceita: uma colonia sem comida e com titulares mortos perde a
funcao ate alguem nascer e crescer.

Verificacao: `VillageFoundationGameTest.everyBedOfTheHouseGetsItsOwnNewborn`,
`aDeadResidentIsNotReplacedByTheNextPass` e `VillageMealsGameTest` (dois
casos). Que a procriacao acontece de fato em jogo continua dependendo de
playtest.

## Emenda N2 - 2026-09-26: adocao atomica da BigHouseMOD

O autor tornou imutavel o nascimento da `BigHouseMOD` com suas camas, baus e
aldeoes. Para uma vila nova, `VillageAdoption` tenta colocar a casa antes de
registrar camas Vanilla, trabalhadores ou recursos da colonia. Se o lote nao
for seguro ou a planta nao puder ser colocada, a entrada nova e removida do
registro no mesmo ciclo. A deteccao posterior recomeca a adocao e tenta a casa
de novo.

Essa regra nao remove colonia preexistente de um save: ela continua tentando
reparar a fundacao quando for detectada, para nao apagar estado do jogador.

Verificacao: `ColonyDetectionGameTest.aVillageWithoutASafeBigHouseLotIsNotAdopted`
falhou contra a ordem anterior e a rodada integral passou com 480/480
GameTests. A criacao e o aspecto da casa no save do autor continuam pendentes
de playtest.
