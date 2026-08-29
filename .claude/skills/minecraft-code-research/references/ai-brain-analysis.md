# Análise de IA

**Nunca assuma que o Minecraft usa só `Goal`.** Existem dois sistemas de IA
convivendo, e desenhar para o errado custa a arquitetura inteira.

## Descobrir qual sistema o mob usa

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
javap -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -i "brain\|goal"
```

| Sinal | Sistema |
|---|---|
| `initGoals`, `goalSelector`, `targetSelector` | **Goal** — mobs clássicos (zumbi, esqueleto, vaca) |
| `initBrain`, `Brain<...> getBrain()`, `createBrainProfile` | **Brain** — mobs modernos (Villager, Piglin, Axolotl, Warden, Allay) |

Alguns mobs têm os dois: um `Brain` para a decisão principal e `Goal`s residuais.
Verifique antes de escolher onde encaixar.

## O modelo Goal

```text
GoalSelector (prioridade numérica, menor = mais importante)
    └── Goal
         ├── canStart()        deve começar?
         ├── shouldContinue()  deve seguir?
         ├── start() / stop()
         ├── tick()
         └── getControls()     MOVEMENT, LOOK, JUMP, TARGET
```

O que decide a convivência é `getControls()`: dois goals que pedem o mesmo
controle não rodam juntos; o de prioridade melhor ganha. **Prioridade sozinha não
explica nada** — é a combinação prioridade + controles.

## O modelo Brain

Mais indireto, e é justamente a indireção que o torna extensível:

```text
    MUNDO
      │
   SENSOR          observa periodicamente
      │
   MEMORY          o que a entidade "sabe", com validade
      │
   ACTIVITY        o modo atual (CORE, IDLE, WORK, REST, MEET, PANIC...)
      │
    TASK           o comportamento que age
      │
    AÇÃO           WALK_TARGET, LOOK_TARGET, interação, mudança de mundo
```

As peças, em nomes Yarn 1.21.1:

| Peça | Classe | Papel |
|---|---|---|
| Brain | `net.minecraft.entity.ai.brain.Brain` | coordena tudo |
| Memory | `MemoryModuleType<T>` | conhecimento com validade |
| Activity | `Activity` | modo funcional |
| Sensor | `Sensor` / `SensorType` | percepção → memória |
| Task | `Task<E>`, `MultiTickTask<E>` | ação |
| Schedule | `Schedule` | horário → Activity |

### Perguntas antes de mexer

```text
Qual MEMÓRIA representa o estado necessário?
Qual SENSOR a preenche?
Qual ACTIVITY controla o comportamento?
Qual TASK executa?
Qual condição INICIA? Qual INTERROMPE?
Qual PRIORIDADE?
Qual comportamento Vanilla precisa ser PRESERVADO?
```

Se você não sabe responder a memória, provavelmente está prestes a guardar estado
de IA num `static Map` — ver `anti-patterns.md`.

## Memórias

Memória não é variável: é conhecimento **com tempo de vida**.

```java
brain.remember(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, completionRange));
brain.remember(MemoryModuleType.JOB_SITE, pos, expiryTicks);   // com validade
brain.getOptionalRegisteredMemory(MemoryModuleType.JOB_SITE);
brain.forget(MemoryModuleType.WALK_TARGET);
```

Para cada memória nova, defina: **nome, tipo, quem escreve, quem lê, tempo de
vida, expiração, default, persistência, sincronização, condições de limpeza.**

Memória precisa estar **registrada** no perfil do Brain para poder ser usada.
Memória não registrada lança ou é silenciosamente descartada, dependendo do
caminho.

## Tasks — o que verificar em 1.21.1

Assinaturas confirmadas por `javap` sobre `minecraft-merged` 1.21.1 (yarn
`1.21.1+build.3`):

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

`tryStarting`, `tick` e `stop` são `final` — você não os sobrescreve; o Brain os
chama. O ciclo real é: `shouldRun` → `run` → (`shouldKeepRunning` → `keepRunning`)*
→ `finishRunning`.

O mapa de memórias exigidas é o **gate declarativo**: com `MemoryModuleState`
você diz "esta memória precisa existir / precisa não existir" e o Brain nem tenta
iniciar a task fora disso. Usar o mapa é melhor que checar dentro de `shouldRun`,
porque é mais barato e fica visível para quem lê.

## Registrar uma task sem quebrar Vanilla

```java
brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new MinhaTask()));
```

`[FATO]` verificado: `setTaskList` **acrescenta** à lista da Activity, não
substitui. Nenhuma task Vanilla é removida.

Três armadilhas reais:

1. **Activity precisa estar no repertório do Brain.** Registrar uma Activity nova
   não basta: quem escolhe a Activity ativa é a `Schedule`, através de uma task
   Vanilla de `CORE`. Uma Activity que a Schedule não conhece **nunca é
   escolhida** — e forçá-la exigiria justamente uma task de CORE chamando
   `doExclusively` a cada tick. Muitas vezes a solução mais barata é pôr a task em
   `CORE` e carregar nela as condições que a Activity daria.

2. **Prioridade é ordem dentro da Activity, não importância global.** Ficar depois
   das tasks Vanilla de CORE costuma ser o que você quer: pânico e incursão
   decidem primeiro.

3. **Não assuma índice de lista.** Outro mod pode ter inserido antes de você.

## Movimento: quem manda é a memória

Erro clássico: chamar `getNavigation().startMovingTo(...)` e ver o mob ignorar.

`[FATO]` em 1.21.1: em mobs de Brain, quem controla o caminho é a memória
`WALK_TARGET`. As tasks Vanilla de movimento só agem quando ela está posta, e o
cérebro reescreve o destino no mesmo tick seguindo a agenda dele. Navegação
direta é sobrescrita.

O caminho que funciona:

```java
brain.remember(MemoryModuleType.WALK_TARGET,
        new WalkTarget(new BlockPosLookTarget(destino), velocidade, alcance));
```

E **manter** a memória enquanto o destino valer — a task que a mantém é o que
segura o mob no caminho, sem cancelar nem remover nada do Vanilla.

## Schedule

```text
TEMPO DO MUNDO → SCHEDULE → ACTIVITY → conjunto de TASKS
```

Não implemente horário com `if (world.getTimeOfDay() % X)` antes de olhar como a
`Schedule` do mob funciona. O Vanilla já tem o mecanismo, ele já é o que decide, e
um horário paralelo vai brigar com ele — o mob vai trocar de modo no meio da sua
lógica.

Para agir "só no horário de trabalho", consulte a Activity/Schedule vigente em vez
de recriar a regra.

## Pathfinding e custo

Pathfinding está entre os cálculos mais caros do jogo. Ao analisar IA, meça:

```text
Com que frequência o alvo é recalculado?
Quantas entidades fazem isso simultaneamente?
O que acontece quando o caminho falha? Há retry infinito?
O alvo está em chunk carregado?
```

Ver `performance-analysis.md`.

## Depurar IA

A pergunta central é sempre a mesma: **o que a entidade acha que sabe?**

```text
Qual Activity está ativa?          → deveria ser essa?
Quais memórias estão preenchidas?  → alguma está errada ou vencida?
Qual sensor deveria atualizá-la?   → rodou?
Qual task está rodando?            → e qual está bloqueando a que você quer?
O gate de memórias da task passa?
```

Ordem eficiente: Activity → memórias → sensor → task → prioridade → pathfinding.
Comece pelo estado, não pelo código: quase sempre a task está correta e a memória
que ela exige nunca foi escrita.

Registro em `templates/ai-system-analysis.md`.
