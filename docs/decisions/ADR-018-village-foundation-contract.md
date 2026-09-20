# ADR-018 - Contrato de fundacao automatica da vila

**Estado:** Aceita pelo autor em 2026-09-20
**Escopo:** deteccao de vila e registro de trabalhadores Fabric 1.21.1.

## Contexto

Uma vila detectada podia existir com menos adultos, sem cama vinculada e sem
bau proprio para uma ou mais funcoes. Isso deixava a economia aguardando uma
profissao que nunca seria criada e fazia a regra de armazenamento depender de
um acaso da geracao Vanilla.

A ADR-011 tratava o construtor como capacidade temporaria e descrevia sete
profissoes produtoras. Essa decisao continua valida para crescimento de
producao, mas nao cobre a fundacao minima exigida para toda vila.

## Decisao

1. Toda vila adotada pelo mod recebe, na primeira deteccao, no minimo um
   adulto para cada funcao de `ProfessionAssigner.FOUNDATION_ORDER`:
   `MINER`, `LUMBERJACK`, `MASON`, `SMELTER`, `CARPENTER`, `FARMER`,
   `BREEDER` e `BUILDER`.
2. `SHEPHERD` de saves antigos e normalizado como `BREEDER` para esta regra;
   ele nao cria uma nona vaga.
3. Cada trabalhador fundacional deve ter uma cama Vanilla exclusiva, gravada
   na memoria `HOME`, e um bau proprio registrado no `StorageRegistry`.
4. A fundacao pode criar aldeoes adultos, camas brancas e bau, mas somente em
   blocos substituiveis com piso solido. Ela nunca sobrescreve blocos
   existentes do jogador, da vila ou de estruturas Vanilla.
5. A mesma garantia e idempotente e roda novamente quando a vila e detectada;
   mortes ou dados ausentes podem ser repostos sem duplicar uma funcao que ja
   tenha titular vivo.

## Consequencias

- A populacao inicial minima da colônia passa a ser oito adultos funcionais.
- O crescimento posterior continua usando as quotas produtoras da ADR-011;
  `BUILDER` e uma funcao fundacional permanente, mas a capacidade de executar
  `BUILD` continua sendo tratada pelo fluxo de construcao.
- A criacao evita acavalamento, mas pode adiar a fundacao se nao houver local
  seguro. A proxima deteccao tenta novamente e registra um `WARN`.
- O GameTest `VillageFoundationGameTest` prova a regra em terreno isolado.
  A existencia e o posicionamento no save do autor continuam exigindo
  playtest.

## Verificacao

`./gradlew.bat test` e `./gradlew.bat clean runGametest` passaram; a rodada
final executou 381/381 GameTests, incluindo o teste desta ADR. O playtest no
save do autor continua pendente.
