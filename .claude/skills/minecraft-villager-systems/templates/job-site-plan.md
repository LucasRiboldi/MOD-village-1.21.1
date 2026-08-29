# Plano de local de trabalho — <nome>

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Precisa mesmo de POI?

```text
[ ] existe um POI Vanilla que já serve?
[ ] o bloco precisa ser local de TRABALHO, ou basta um bloco com que ele interage?
[ ] isto exige profissão nova, ou serve a uma existente?
```

<Um bloco com que o aldeão apenas interage não precisa ser POI — precisa de uma
task que o encontre.>

## O bloco

| | |
|---|---|
| Bloco | `meumod:<path>` |
| Tem block states? | quais |
| Tem BlockEntity? | |

## O POI

`[FATO]` MC 1.21.1 — `PointOfInterestType` é um `record`:

```java
PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance)
```

| Componente | Valor | Justificativa |
|---|---|---|
| `blockStates` | | |
| `ticketCount` | | 1 = exclusivo |
| `searchDistance` | | o menor que resolve |

### Block states — a armadilha

```java
// ✗ o aldeão perde o local quando o bloco muda de estado
Set.of(ModBlocks.FORJA.getDefaultState())

// ✓
ImmutableSet.copyOf(ModBlocks.FORJA.getStateManager().getStates())
```

```text
[ ] TODOS os block states relevantes estão incluídos
[ ] se algum foi deliberadamente excluído, o motivo está escrito
```

### `searchDistance`

```text
pequeno demais → ele não acha o bloco que está ali
grande demais  → reivindica de longe, nunca chega, e a busca fica cara
```

```text
[ ] ele consegue ir até lá dentro do horário de trabalho
[ ] e voltar para dormir
```

## A profissão que o reconhece

| | |
|---|---|
| Profissão | |
| `heldWorkstation` casa? | |
| `acquirableWorkstation` casa? | |

## Ciclo de vida

```text
DISCOVER → CLAIM → ASSIGN → WORK → RELEASE → REASSIGN
```

| Evento | Comportamento |
|---|---|
| bloco destruído | POI some; o aldeão perde `JOB_SITE` |
| aldeão morre | ticket liberado |
| aldeão convertido | idem — **`MOB_CONVERSION`** |
| aldeão muda de profissão | |
| chunk descarrega | POI permanece no índice; o aldeão não age |
| outro aldeão já reivindicou | `ticketCount` esgotado |
| bloco muda de estado | continua sendo POI (se todos registrados) |

## Memórias envolvidas

`[FATO]` MC 1.21.1 — e repare no tipo:

```java
MemoryModuleType<GlobalPos>  JOB_SITE            reivindicado
MemoryModuleType<GlobalPos>  POTENTIAL_JOB_SITE  achado, não confirmado
```

> **`GlobalPos`**, não `BlockPos`. O Vanilla separa "achei" de "é meu" — se o seu
> sistema tem reivindicação, provavelmente precisa da mesma separação.

## Registro

```text
[ ] bloco registrado ANTES
[ ] POI em Registries.POINT_OF_INTEREST_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
```

## Resources do bloco

```text
[ ] lang
[ ] modelo + blockstate + textura
[ ] modelo de item
[ ] loot table
[ ] tags de mineração
[ ] item group
```

## Busca — não varra

```text
[ ] uso o PointOfInterestStorage em vez de varrer blocos
```

> Varrer 64³ blocos são milhões de leituras; o índice do jogo já existe.

## Teste — a reivindicação primeiro

> Faça **antes** de escrever comportamento. Sem lógica no caminho, o diagnóstico
> é direto.

```text
[ ] colocar o bloco perto de um aldeão SEM profissão
[ ] ele adquire a profissão
[ ] só UM aldeão adquire
[ ] quebrar o bloco → ele perde (se nível 1)
[ ] recolocar → outro pode adquirir
```

## Teste completo

```text
[ ] o ciclo de vida inteiro
[ ] ele vai até o local no horário de trabalho
[ ] os resources aparecem
[ ] fechar e reabrir o mundo mantém a reivindicação
[ ] com 20 aldeões e 20 blocos, a distribuição faz sentido
[ ] servidor dedicado
```

## Sintomas a conferir

```text
ignora o bloco          → POI ou block state
adota e larga           → predicado da profissão
dois no mesmo bloco     → ticketCount
nunca chega             → searchDistance
perde ao virar o bloco  → só um block state registrado
```
