# Exemplo — bloco de trabalho e POI

**Pedido:**

> "Criei um bloco 'forja'. Quero que um aldeão o reivindique e trabalhe nele."

Este exemplo mostra a armadilha dos block states — que pega praticamente todo
mundo na primeira vez.

---

## 1. Verificar a forma na versão

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
```

`[FATO]` MC 1.21.1 — é um `record`:

```java
PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance)
```

Em versões anteriores a forma era outra. **Tutorial antigo não compila.**

## 2. O bloco tem estados?

A forja acende quando está trabalhando:

```java
public static final BooleanProperty ACESA = BooleanProperty.of("acesa");
```

Dois estados: `acesa=false` e `acesa=true`.

## 3. A armadilha

```java
// ✗ o aldeão LARGA O EMPREGO quando começa a trabalhar
Set.of(ModBlocks.FORJA.getDefaultState())

// ✓ todos os estados
ImmutableSet.copyOf(ModBlocks.FORJA.getStateManager().getStates())
```

Com o primeiro, a forja acesa **deixa de ser um POI**. O aldeão reivindica,
começa a trabalhar, o bloco acende, o POI some, e ele perde a profissão — um
loop que parece completamente inexplicável se você não sabe disto.

> **A regra:** `blockStates`, não blocos. Todos os estados relevantes.
>
> Exceção legítima: quando a distinção é intencional — a cama só conta como POI
> na cabeceira.

## 4. `ticketCount` e `searchDistance`

```java
new PointOfInterestType(
        ImmutableSet.copyOf(ModBlocks.FORJA.getStateManager().getStates()),
        1,     // ticketCount: um aldeão por forja
        6);    // searchDistance
```

| Escolha | Consequência |
|---|---|
| `ticketCount = 1` | local exclusivo — o normal para trabalho |
| `ticketCount > 1` | vários aldeões no mesmo bloco |
| `searchDistance` pequeno | ele não acha o bloco que está ali |
| `searchDistance` grande | reivindica de longe, **nunca chega**, e a busca fica cara |

O critério para o `searchDistance`: **ele consegue ir até lá no horário de
trabalho e voltar para dormir?**

## 5. Registrar — na ordem

```text
BLOCO → POI → PROFISSÃO
```

```text
[ ] bloco registrado antes
[ ] POI em Registries.POINT_OF_INTEREST_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
```

## 6. Testar a reivindicação — antes de qualquer comportamento

Este é o teste de maior valor do workflow, e vale fazer **agora**, sem nenhuma
task escrita. Sem lógica no caminho, o diagnóstico é direto.

```bash
./gradlew build && ./gradlew runClient
```

```text
[x] colocar a forja perto de um aldeão SEM profissão
[x] ele adquire a profissão
[x] só UM aldeão adquire (ticketCount)
[x] a forja ACENDE e ele NÃO perde a profissão   ← o teste do passo 3
[x] quebrar a forja → ele perde (se nível 1)
[x] recolocar → outro pode adquirir
```

Se o quarto falhar, você esqueceu um block state.

## 7. As memórias

`[FATO]` MC 1.21.1:

```java
MemoryModuleType<GlobalPos>  JOB_SITE            reivindicado
MemoryModuleType<GlobalPos>  POTENTIAL_JOB_SITE  achado, não confirmado
```

**São `GlobalPos`** — dimensão + posição. Tratar como `BlockPos` compila em
alguns caminhos e erra entre dimensões.

E a separação "achei" / "é meu" é informação de desenho: se o seu sistema tem
reivindicação, provavelmente precisa da mesma.

## 8. Não varra para encontrar POIs

```text
✗  varrer 64³ blocos procurando forjas    ← ~2 milhões de leituras
✓  perguntar ao PointOfInterestStorage     ← índice espacial do jogo
```

O jogo já mantém o índice. Ver `references/villager-performance.md`.

## 9. Ciclo de vida

```text
DISCOVER → CLAIM → ASSIGN → WORK → RELEASE → REASSIGN
```

```text
[x] bloco destruído       → POI some, o aldeão perde JOB_SITE
[x] aldeão morre          → ticket liberado (AFTER_DEATH)
[x] aldeão convertido     → ticket liberado (MOB_CONVERSION)  ← o mais esquecido
[x] chunk descarrega      → POI permanece no índice, o aldeão não age
[x] bloco muda de estado  → continua sendo POI
```

## 10. Resources

```text
[ ] lang            [ ] blockstate (os dois estados)
[ ] modelo aceso e apagado                [ ] modelo de item
[ ] textura         [ ] loot table        [ ] tags de mineração
[ ] item group
```

## Entregar

> **Feature:** a forja é reconhecida como local de trabalho.
>
> **Detalhe que decidiu o desenho:** o POI inclui **todos** os block states. Com
> só o padrão, o aldeão largaria a profissão assim que a forja acendesse.
>
> **Verificado rodando:** aquisição, exclusividade, forja acesa mantendo o POI,
> quebra liberando, `runServer`.

---

## O que este exemplo demonstra

1. **`blockStates`, não blocos.** É a armadilha número um do POI, e o sintoma —
   "ele larga o emprego ao começar a trabalhar" — não sugere a causa.
2. **Testar a reivindicação antes do comportamento.** Sem lógica no caminho, o
   diagnóstico é direto.
3. **`searchDistance` tem um critério concreto:** ele consegue ir e voltar?
4. **`JOB_SITE` é `GlobalPos`.**
5. **Consultar o índice, não varrer.**
