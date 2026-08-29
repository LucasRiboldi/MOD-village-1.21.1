# Workflow — nova memória ou sensor

**Sensor percebe. Memória guarda. Task age.** Este workflow cobre as duas
primeiras; violar a separação é o segundo anti-padrão mais comum do domínio.

---

## 1. Qual das duas você precisa?

```text
"o aldeão precisa LEMBRAR de X"     → MEMÓRIA
"o aldeão precisa NOTAR X"          → SENSOR (que escreve numa memória)
```

Frequentemente as duas: o sensor percebe e escreve; a memória guarda; a task lê.

## 2. Existe uma Vanilla que serve?

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i "<conceito>"
```

`[FATO]` MC 1.21.1 — memórias de aldeão já existentes:

```java
MemoryModuleType<GlobalPos>        HOME · JOB_SITE · POTENTIAL_JOB_SITE · MEETING_POINT
MemoryModuleType<List<GlobalPos>>  SECONDARY_JOB_SITE
MemoryModuleType<WalkTarget>       WALK_TARGET
MemoryModuleType<LookTarget>       LOOK_TARGET
MemoryModuleType<BlockPos>         NEAREST_BED
MemoryModuleType<Long>             CANT_REACH_WALK_TARGET_SINCE
```

**Reusar a Vanilla é melhor:** o jogo já a preenche, expira e persiste. Criar
uma paralela para o mesmo conceito produz duas verdades que divergem.

---

## MEMÓRIA

## 3. Plano

`templates/memory-plan.md`:

```text
NOME · TIPO
QUEM ESCREVE      sensor? task? evento?
QUEM LÊ
TEMPO DE VIDA     permanente na sessão, ou expira?
EXPIRAÇÃO         em quantos ticks
DEFAULT           o que significa "vazia"
PERSISTÊNCIA      tem codec?
SINCRONIZAÇÃO     o cliente precisa?
LIMPEZA           quando é esquecida
```

O campo **limpeza** é o mais esquecido, e produz o aldeão que "lembra" de um alvo
que não existe mais.

## 4. Registrar

```java
public static final MemoryModuleType<BlockPos> MEU_ALVO =
        Registry.register(Registries.MEMORY_MODULE_TYPE,
                Identifier.of(MOD_ID, "meu_alvo"),
                new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));   // com codec = persiste
```

```text
[ ] com codec se precisa sobreviver ao save; sem, se é intenção do momento
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
```

**Registro é o degrau 2 da escada** — não toca em nada do Vanilla.

## 5. Registrar no perfil do Brain

> A memória precisa estar **no perfil do Brain do aldeão** para poder ser usada.

Esta é a etapa que falta quando "registrei a memória e não funciona". Verifique
como o perfil é montado na sua versão, e como acrescentar sem substituir o
conjunto Vanilla.

## 6. Usar

```java
brain.remember(ModMemories.MEU_ALVO, pos);                  // permanente na sessão
brain.remember(ModMemories.MEU_ALVO, pos, 600);             // expira em 600 ticks
brain.getOptionalRegisteredMemory(ModMemories.MEU_ALVO);
brain.forget(ModMemories.MEU_ALVO);
```

E use-a como **gate** da task:

```java
super(Map.of(ModMemories.MEU_ALVO, MemoryModuleState.VALUE_PRESENT), MIN, MAX);
```

Mais barato que checar em `shouldRun`, e visível para quem lê.

---

## SENSOR

## 7. Plano

`templates/sensor-plan.md`:

```text
ENTRADA        o que ele observa
ÁREA           raio ou caixa
FREQUÊNCIA     a cada quantos ticks
SAÍDA          a memória que escreve
EXPIRAÇÃO      quanto dura o que ele escreveu
CUSTO          área × frequência × população
```

## 8. A regra rígida

```text
✓  sensor OBSERVA e ESCREVE memória
✗  sensor executa ação de gameplay
✗  sensor quebra bloco, move o aldeão, muda inventário
```

Sensor roda com frequência própria, **fora do controle da Activity** e sem o gate
de memórias. Ação ali acontece quando não deveria.

## 9. O custo

```text
custo = área × frequência × nº de aldeões
```

Um sensor de raio 32, a cada 20 ticks, com 50 aldeões, é a causa número um de lag
em mod de aldeão.

```text
[ ] a frequência é a MENOR que resolve?
[ ] a área é a MENOR que resolve?      (raio é cúbico: metade = 1/8 do custo)
[ ] dá para reagir a um EVENTO em vez de perceber periodicamente?
[ ] dá para consultar o PointOfInterestStorage em vez de varrer?
```

A terceira e a quarta eliminam o sensor por completo, quando aplicáveis.

## 10. Leitura segura

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`world.getBlockState` **força carga de chunk** — de dentro de um sensor, isso é
gerar terreno periodicamente, por aldeão.

---

## Testar

```bash
./gradlew build
./gradlew runGametest
./gradlew runServer
```

```text
[ ] a memória é escrita quando deveria
[ ] expira quando deveria
[ ] é limpa quando o alvo some
[ ] a task que a exige roda
[ ] com codec: sobrevive a fechar e reabrir o mundo
[ ] sem codec: NÃO sobrevive — e isso é intencional
[ ] com 50 aldeões, o sensor não pesa
```

Os dois itens de codec são um par: verifique o que você escolheu, não o que
esperava.

## Fechamento

`checklists/brain-memory-sensor.md`.
