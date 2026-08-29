# O Brain

O sistema de IA dos mobs modernos. Extensível **por acréscimo** — e é isso que
permite estender aldeão sem reescrever nada.

```text
MUNDO → SENSOR → MEMORY → ACTIVITY → TASK → AÇÃO
```

## Memórias

Memória não é variável: é **conhecimento com tempo de vida**.

`[FATO]` MC 1.21.1 — memórias relevantes de aldeão, com os tipos reais:

```java
MemoryModuleType<GlobalPos>        HOME
MemoryModuleType<GlobalPos>        JOB_SITE
MemoryModuleType<GlobalPos>        POTENTIAL_JOB_SITE
MemoryModuleType<GlobalPos>        MEETING_POINT
MemoryModuleType<List<GlobalPos>>  SECONDARY_JOB_SITE
MemoryModuleType<WalkTarget>       WALK_TARGET
MemoryModuleType<LookTarget>       LOOK_TARGET
MemoryModuleType<BlockPos>         NEAREST_BED
MemoryModuleType<Long>             CANT_REACH_WALK_TARGET_SINCE
```

> **`JOB_SITE`, `HOME` e `MEETING_POINT` são `GlobalPos`** — dimensão + posição.
> Tratar como `BlockPos` compila em alguns caminhos e erra entre dimensões.
>
> `CANT_REACH_WALK_TARGET_SINCE` é o mecanismo Vanilla de desistência: ele existe
> justamente porque **retry infinito é bug**. Use-o como modelo.

### Operações

```java
brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(alvo, velocidade, alcance));
brain.remember(MemoryModuleType.JOB_SITE, globalPos, ticksDeValidade);
brain.getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
brain.forget(MemoryModuleType.WALK_TARGET);
```

Verifique os nomes na sua versão — a API de acesso mudou entre releases.

### Criar memória própria

```java
public static final MemoryModuleType<BlockPos> MEU_ALVO =
        Registry.register(Registries.MEMORY_MODULE_TYPE,
                Identifier.of(MOD_ID, "meu_alvo"),
                new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
```

Com codec, a memória **persiste**. Sem codec, é só de sessão. Escolha
conscientemente — ver `templates/memory-plan.md`.

**Registrar memória é o degrau 2 da escada:** não toca em nada do Vanilla e
convive com outros mods.

Para cada memória defina: **nome, tipo, quem escreve, quem lê, tempo de vida,
expiração, default, persistência, sincronização, condições de limpeza.**

> A memória precisa estar **registrada no perfil do Brain** do aldeão para ser
> usada. Memória não registrada não funciona.

## Sensores

**Sensor percebe. Task age.** Esta separação é rígida, e violá-la é o segundo
anti-padrão mais comum do domínio.

```text
SENSOR → observa periodicamente → escreve MEMORY
```

O sensor tem **frequência própria** — é ele que impede o aldeão de varrer o mundo
todo tick. Para cada sensor, documente: **entrada, área de busca, frequência,
saída, memória escrita, expiração, custo.**

Sensor caro rodando com frequência alta, multiplicado pela população da vila, é
a causa número um de lag em mod de aldeão.

## Activities

`[FATO]` MC 1.21.1 — as constantes de `net.minecraft.entity.ai.brain.Activity`:

```text
CORE · IDLE · WORK · PLAY · REST · MEET · PANIC
PRE_RAID · RAID · HIDE · FIGHT · CELEBRATE · AVOID · ADMIRE_ITEM · RIDE
```

A Activity **filtra quais tasks podem rodar**. `CORE` roda sempre; as outras são
mutuamente exclusivas.

### Quem escolhe a Activity

> **A `Schedule`.** Através de uma task Vanilla de `CORE` que consulta o horário.

Consequência que decide arquitetura:

> **Uma Activity que a Schedule não conhece nunca é escolhida.**

Registrar `meumod:trabalho_da_colonia` como Activity e esperar que ela ative não
funciona. Forçá-la exigiria uma task de CORE chamando `doExclusively` a cada
tick — mais peça para o mesmo efeito.

**A alternativa que quase sempre é melhor:** ponha a task em `CORE` e carregue
nela as condições que a Activity daria.

```java
@Override
protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
    return ehHorarioDeTrabalho(world, villager)      // a condição da Activity
        && temDestino(villager);                      // a condição do estado
}
```

O resultado em jogo é o mesmo; muda o lugar do registro, para o lado mais barato
e de menos conflito.

Ver `workflows/add-activity.md` antes de decidir criar uma.

## Tasks

`[FATO]` assinaturas de `MultiTickTask` em MC 1.21.1:

```java
public abstract class MultiTickTask<E extends LivingEntity> implements Task<E> {
    public MultiTickTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemories);
    public MultiTickTask(Map<...>, int runTime);
    public MultiTickTask(Map<...>, int minRunTime, int maxRunTime);

    protected boolean shouldRun(ServerWorld world, E entity);
    protected void    run(ServerWorld world, E entity, long time);
    protected boolean shouldKeepRunning(ServerWorld world, E entity, long time);
    protected void    keepRunning(ServerWorld world, E entity, long time);
    protected void    finishRunning(ServerWorld world, E entity, long time);
}
```

`tick`, `tryStarting` e `stop` são **`final`** — o Brain dirige o ciclo:

```text
shouldRun → run → (shouldKeepRunning → keepRunning)* → finishRunning
```

`[FATO]` `shouldKeepRunning` devolvendo `false` faz o Brain encerrar a task no
mesmo tick e reavaliar. É assim que uma task cede lugar a outra.

### O gate de memórias

O primeiro parâmetro do construtor é um gate **declarativo**:

```java
super(Map.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT), MIN, MAX);
```

O Brain nem tenta iniciar a task fora da condição. **Melhor que checar dentro de
`shouldRun`** — é mais barato e fica visível para quem lê o código.

## Instalar sem quebrar o Vanilla

```java
brain.setTaskList(Activity.CORE, PRIORIDADE, ImmutableList.of(new MinhaTask()));
```

`[FATO]` verificado no bytecode de `Brain` em 1.21.1: a implementação central usa
`Map.computeIfAbsent` + `Set.add` para a lista de tasks. Ela **acrescenta** —
nenhuma task Vanilla é removida, e **você não precisa remover nada.**

> **A nuance que o bytecode revela.** No mesmo método, `requiredActivityMemories`
> e `forgettingActivityMemories` usam `Map.put`, não `computeIfAbsent`: as
> **memórias exigidas da Activity são substituídas**, não acrescentadas.
>
> Na prática isso é inofensivo em `CORE`, cujo conjunto de memórias exigidas é
> vazio no Vanilla — passar vazio sobre vazio não muda nada. Mas se você usar
> uma sobrecarga que recebe `Set` de memórias numa Activity que **tem**
> requisitos, você os apaga.
>
> Regra prática: use a sobrecarga `(Activity, int, ImmutableList)` e não passe
> conjuntos de memória, salvo se souber exatamente o que está substituindo.

`[FATO]` São **cinco** sobrecargas de `setTaskList` em 1.21.1 — um Mixin que mire
o método precisa de descriptor.

Três cuidados:

1. **Prioridade é ordem dentro da Activity**, não importância global. Ficar
   **depois** das tasks Vanilla de CORE costuma ser certo: pânico e incursão
   devem decidir primeiro.
2. **Não assuma índice de lista** — outro mod pode ter inserido antes.
3. **Degrade.** Capture a exceção: um aldeão sem a sua task é um aldeão Vanilla,
   que é o estado de antes.

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORIDADE, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] não instalou a task — este aldeão fica vanilla", falha);
    }
}
```

A instalação é chamada por um Mixin mínimo em `initBrain` — três linhas que
delegam. Ver `references/vanilla-extension-points.md`.

## Schedule

`[FATO]` MC 1.21.1: `net.minecraft.entity.ai.brain.Schedule` traz `EMPTY`,
`SIMPLE`, `VILLAGER_BABY`, `VILLAGER_DEFAULT`, a constante `WORK_TIME` e um
`ScheduleBuilder`.

```text
TEMPO DO MUNDO → SCHEDULE → ACTIVITY → conjunto de TASKS
```

```java
// ✗ um horário paralelo briga com a Schedule Vanilla
if (world.getTimeOfDay() % 24000 > 2000) { ... }

// ✓ pergunte qual Activity está ativa, ou consulte a Schedule
```

Recriar o horário faz o aldeão trocar de modo no meio da sua lógica — o Vanilla
não sabe que você tem uma opinião.

## Prioridades e conflito

Prioridade sozinha não explica o comportamento. O que explica é a combinação:

```text
qual ACTIVITY está ativa  ×  a ordem dentro dela  ×  o gate de memórias
```

Uma task de prioridade excelente numa Activity que não está ativa **não roda**.
É a causa mais comum de "minha task nunca executa".

## Depurar

Comece pelo **estado**, não pelo código:

```text
1. Qual Activity está ativa?     deveria ser essa?
2. Quais memórias estão preenchidas?  alguma vencida?
3. Qual sensor deveria escrever?  rodou?
4. O gate de memórias da task passa?
5. Qual task está bloqueando a desejada?
6. WALK_TARGET está posto e sendo MANTIDO?
```

Quase sempre a task está correta e a memória que ela exige nunca foi escrita.
Ver `references/villager-debugging.md`.
