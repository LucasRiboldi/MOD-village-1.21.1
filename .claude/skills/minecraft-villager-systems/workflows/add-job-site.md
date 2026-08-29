# Workflow — novo local de trabalho (POI)

O POI é o que liga **bloco no mundo** a **comportamento de aldeão**. Sem ele, o
aldeão ignora o seu bloco.

---

## 1. Precisa mesmo?

```text
[ ] existe um POI Vanilla que já serve?
[ ] o bloco precisa ser local de TRABALHO, ou basta um bloco com que ele interage?
[ ] isto exige profissão nova, ou o POI serve a uma existente?
```

Um bloco com que o aldeão apenas interage (deposita, pega) **não precisa ser
POI** — precisa de uma task que o encontre.

## 2. Verificar a versão

`[FATO]` MC 1.21.1 — `PointOfInterestType` é um `record`:

```java
PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance)
```

```bash
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestTypes | head -20
```

| Componente | Escolha |
|---|---|
| `blockStates` | **todos** os estados do bloco |
| `ticketCount` | 1 para local exclusivo |
| `searchDistance` | o menor que resolve |

## 3. A armadilha dos block states

```java
// ✗ o aldeão perde o local quando o bloco muda de estado
Set.of(ModBlocks.FORJA.getDefaultState())

// ✓ todos os estados
ImmutableSet.copyOf(ModBlocks.FORJA.getStateManager().getStates())
```

Um bloco com orientação ou com ligado/desligado tem vários estados. Registrar só
um faz a forja acesa deixar de ser local de trabalho — e o aldeão larga o emprego
quando começa a trabalhar.

Exceção: quando a distinção é intencional (a cama só conta na cabeceira).

## 4. `searchDistance`

```text
pequeno demais → o aldeão não acha o bloco que está ali
grande demais  → ele reivindica de longe e nunca chega, e a busca fica cara
```

O aldeão precisa **conseguir ir até lá** dentro do horário de trabalho e voltar
para dormir. Ver `references/work-and-schedules.md`.

## 5. Plano

`templates/job-site-plan.md`.

## 6. Registrar

```text
BLOCO registrado
  ↓
POINT_OF_INTEREST_TYPE registrado
  ↓
PROFISSÃO que o reconhece (predicado)
```

```text
[ ] bloco registrado antes
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
[ ] todos os block states
```

```bash
./gradlew build
./gradlew runClient
```

## 7. Testar a reivindicação — cedo

Este é o teste que vale mais neste workflow, e vale fazer **antes** de escrever
qualquer comportamento:

```text
[ ] colocar o bloco perto de um aldeão SEM profissão
[ ] ele adquire a profissão?
[ ] só UM aldeão adquire? (ticketCount)
[ ] quebrar o bloco → ele perde a profissão (se nível 1)
[ ] recolocar → outro aldeão pode adquirir
```

Se ele não adquire, o problema é o POI ou o predicado da profissão — e ainda não
há comportamento no caminho para confundir o diagnóstico.

## 8. O ciclo de vida completo

```text
DISCOVER → CLAIM → ASSIGN → WORK → RELEASE → REASSIGN
```

Cada transição precisa de comportamento definido:

```text
[ ] bloco destruído          → POI some, aldeão perde JOB_SITE
[ ] aldeão morre             → o ticket é liberado
[ ] aldeão convertido        → idem  ← MOB_CONVERSION
[ ] chunk descarrega         → o POI continua no índice; o aldeão não age
[ ] outro aldeão já tem      → ticketCount esgotado
[ ] o bloco muda de estado   → continua sendo POI (se todos registrados)
```

## 9. As memórias

`[FATO]` MC 1.21.1 — e repare no tipo:

```java
MemoryModuleType<GlobalPos>  JOB_SITE            o local reivindicado
MemoryModuleType<GlobalPos>  POTENTIAL_JOB_SITE  achado, não confirmado
```

**São `GlobalPos`** (dimensão + posição), não `BlockPos`. Tratar como `BlockPos`
erra entre dimensões.

O Vanilla separa "achei" de "é meu" — se o seu sistema tem reivindicação,
provavelmente precisa da mesma separação.

## 10. Não varra: consulte

Se o seu mod precisa saber onde há POIs, use o índice do jogo:

```text
✗  varrer 64³ blocos procurando            ← milhões de leituras
✓  perguntar ao PointOfInterestStorage      ← consulta indexada
```

Ver `references/villager-performance.md`.

## 11. Testar completo

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

```text
[ ] o ciclo de vida inteiro (passo 8)
[ ] o aldeão vai até o local no horário de trabalho
[ ] o bloco tem lang, modelo, textura e loot table
[ ] fechar e reabrir o mundo mantém a reivindicação
[ ] com 20 aldeões e 20 blocos, a distribuição faz sentido
```

## Fechamento

`checklists/job-site.md`.

Sintomas para conferir no relato:

```text
o aldeão ignora o bloco       → POI ou block state
adota e larga                  → predicado da profissão
dois no mesmo bloco            → ticketCount
nunca chega                    → searchDistance grande demais
perde ao virar o bloco         → só um block state registrado
```
