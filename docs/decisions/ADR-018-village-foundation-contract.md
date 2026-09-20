# ADR-018 - Contrato de fundacao automatica da vila

**Estado:** Aceita pelo autor em 2026-09-20
**Escopo:** deteccao de vila e registro de trabalhadores Fabric 1.21.1.

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
- A criacao evita acavalamento, mas pode adiar a fundacao se nao houver local
  seguro. A proxima deteccao tenta novamente e registra um `WARN`.
- O GameTest `BigHouseModBlueprintGameTest` prova o conteudo da planta, e
  `VillageFoundationGameTest` prova a colocacao, as seis funcoes e os seis
  registros distintos em terreno isolado. A existencia e o posicionamento no
  save do autor continuam exigindo playtest.

## Verificacao

`./gradlew.bat runGametest` passou; a rodada final executou 384/384 GameTests,
incluindo `BigHouseModBlueprintGameTest` e `VillageFoundationGameTest`. O
playtest no save do autor continua pendente.
