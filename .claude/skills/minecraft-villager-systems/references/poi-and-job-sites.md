# POI e locais de trabalho

O POI (Point of Interest) é a peça que liga **bloco no mundo** a **comportamento
de aldeão**. Entendê-lo é o que separa "criar um bloco de trabalho" de "criar um
bloco que o aldeão ignora".

## O modelo

```text
POI
├── Type          quais BlockStates contam como este POI
├── Position      onde
├── Ticket        quantos aldeões podem reivindicar
├── Capacity      o limite de tickets
└── Lifecycle     nasce com o bloco, morre com o bloco
```

`[FATO]` MC 1.21.1 — `net.minecraft.world.poi.PointOfInterestType` é um `record`:

```java
PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance)
```

| Componente | Significa |
|---|---|
| `blockStates` | **quais estados de bloco** são este POI — não o bloco, os estados |
| `ticketCount` | quantos aldeões podem reivindicar simultaneamente (normalmente 1) |
| `searchDistance` | de que distância um aldeão pode reivindicá-lo |

Classes relacionadas: `PointOfInterestTypes` (registro das constantes Vanilla),
`PointOfInterestStorage` (o índice espacial), `PointOfInterest`,
`PointOfInterestSet`.

## `blockStates`, não blocos

Este detalhe pega quase todo mundo na primeira vez.

Um bloco com propriedades (orientação, ligado/desligado) tem **vários** block
states. Se você registrar só um deles como POI, o aldeão para de reconhecer o
bloco quando ele muda de estado — a forja acesa deixa de ser local de trabalho.

```java
// ✓ todos os estados do bloco
ModBlocks.FORJA.getStateManager().getStates()
```

Salvo quando a distinção é intencional: por exemplo, uma cama só conta como POI
na metade da cabeceira.

## `PointOfInterestStorage` — o índice que você deve usar

O jogo mantém um **índice espacial** de POIs por chunk. Perguntar a ele é
incomparavelmente mais barato que varrer blocos.

```text
✗  varrer 64³ blocos procurando camas          ← milhões de leituras
✓  perguntar ao PointOfInterestStorage          ← consulta indexada
```

Se o seu mod precisa saber "onde há camas/locais de trabalho/sinos por perto", a
resposta quase nunca é varredura. Ver `references/villager-performance.md`.

## Ciclo de vida de um local de trabalho

```text
DISCOVER   o aldeão sem profissão encontra um POI livre
   ↓
CLAIM      ele reivindica — um ticket é consumido
   ↓
ASSIGN     ele ganha a profissão correspondente
   ↓
WORK       vai até lá no horário de trabalho
   ↓
RELEASE    perde a reivindicação (bloco quebrado, aldeão morto, distância)
   ↓
REASSIGN   o POI volta a ficar livre
```

As perguntas que precisam de resposta antes de criar um:

```text
Quem PROCURA?          uma task Vanilla, no horário e no estado certos
Quem REIVINDICA?       o aldeão, consumindo um ticket
Quem LIBERA?           morte, quebra do bloco, perda de profissão
E se DESTRUIR o bloco? o POI some; o aldeão perde JOB_SITE
E se MOVER?            POI é posição — mover é destruir e criar
E se o CHUNK descarregar?  o POI continua no índice; o aldeão não age
E se OUTRO aldeão já tiver? ticketCount esgotado → não reivindica
```

## As memórias envolvidas

`[FATO]` MC 1.21.1, e repare no tipo:

```java
MemoryModuleType<GlobalPos>        JOB_SITE            o local reivindicado
MemoryModuleType<GlobalPos>        POTENTIAL_JOB_SITE  achado, ainda não confirmado
MemoryModuleType<List<GlobalPos>>  SECONDARY_JOB_SITE  locais auxiliares
MemoryModuleType<GlobalPos>        HOME                a cama
MemoryModuleType<GlobalPos>        MEETING_POINT       o sino
```

**São `GlobalPos`** — dimensão + posição. Tratar como `BlockPos` erra entre
dimensões.

`POTENTIAL_JOB_SITE` existindo separado de `JOB_SITE` é informação de desenho: o
Vanilla separa "achei" de "é meu". Se o seu sistema tem reivindicação, provavelmente
precisa da mesma separação.

## A ligação com a profissão

`[FATO]` o record `VillagerProfession` traz **dois** predicados de POI:

```java
Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation();       // o que ele mantém
Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation(); // o que ele pode adquirir
```

E `VillagerProfession.IS_ACQUIRABLE_JOB_SITE` é o predicado padrão de "isto pode
ser adquirido como local de trabalho".

Dois predicados, não um, porque **manter** e **adquirir** são regras diferentes:
um aldeão pode manter um local que já não poderia adquirir.

## Registrar um POI

```text
BLOCO
  ↓
POINT_OF_INTEREST_TYPE registrado, com TODOS os block states
  ↓
PROFISSÃO que o reconhece (predicado)
  ↓
o aldeão encontra, reivindica, trabalha
```

**Registro é o degrau 2 da escada** — não toca em nada do Vanilla.

```text
[ ] registrado em Registries.POINT_OF_INTEREST_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] TODOS os block states relevantes incluídos
[ ] ticketCount coerente (1 para local exclusivo)
[ ] searchDistance coerente (grande demais = busca cara)
[ ] a profissão o reconhece
```

Ver `workflows/add-job-site.md` e `templates/job-site-plan.md`.

## Erros que aparecem em jogo

| Sintoma | Causa |
|---|---|
| o aldeão ignora o bloco | POI não registrado, ou block state faltando |
| ele adota e larga | o predicado da profissão não bate com o POI |
| dois aldeões no mesmo bloco | `ticketCount` maior que 1 sem querer |
| ele nunca chega | `searchDistance` grande demais; ele reivindica de longe |
| ele perde o trabalho ao virar o bloco | só um block state registrado |
| ele não reivindica nada | não há POI livre, ou ele já tem profissão |
| busca cara com vila grande | varredura própria em vez do `PointOfInterestStorage` |

## Antes de criar um POI

```text
[ ] existe um POI Vanilla que já serve?
[ ] o bloco precisa mesmo ser local de TRABALHO,
    ou basta um bloco com que o aldeão interage?
[ ] isto precisa de profissão nova, ou só de comportamento?
```

A última é a que mais economiza trabalho. Ver
`examples/guard-villager-decision.md`.
