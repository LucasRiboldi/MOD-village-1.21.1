# Desenvolver IA

**Nunca assuma que o mob usa `Goal`.** Existem dois sistemas, e escrever para o
errado custa a implementação inteira.

```bash
javap -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -i "brain\|goal"
```

`initGoals` → sistema Goal. `initBrain` → sistema Brain (Villager, Piglin,
Axolotl, Warden, Allay).

Análise em `minecraft-code-research/references/ai-brain-analysis.md`.

---

## Goal — mobs clássicos

```java
goalSelector.add(3, new MeuGoal(this));
```

O que decide convivência é **prioridade + controles**. Dois goals que pedem o
mesmo controle (`MOVEMENT`, `LOOK`, `JUMP`, `TARGET`) não rodam juntos; o de
prioridade melhor ganha. Prioridade sozinha não explica nada.

---

## Brain — o modelo

```text
MUNDO → SENSOR → MEMORY → ACTIVITY → TASK → AÇÃO
```

A indireção é o que torna o sistema extensível: você acrescenta uma peça sem
tocar nas outras.

| Peça | Papel | Separação obrigatória |
|---|---|---|
| Sensor | percebe | **não age** |
| Memory | sabe, com validade | não é banco de dados |
| Activity | modo atual | escolhida pela `Schedule` |
| Task | age | não faz varredura enorme |

Sensor que executa ação de gameplay e task que faz percepção pesada são os dois
anti-padrões mais comuns aqui.

## Escrever uma task

Assinaturas verificadas em MC 1.21.1 (yarn `1.21.1+build.3`):

```java
public final class MinhaTask extends MultiTickTask<VillagerEntity> {

    private static final float VELOCIDADE = 0.5f;
    private static final int MAX_RUN_TIME = 24_000;

    public MinhaTask() {
        super(Map.of(), MAX_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return temTrabalho(villager);
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity villager, long time) {
        mirar(villager);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
        return temTrabalho(villager);
    }
}
```

`tick`, `tryStarting` e `stop` são **`final`** — o Brain dirige o ciclo. Você
sobrescreve `shouldRun` / `run` / `shouldKeepRunning` / `keepRunning` /
`finishRunning`.

### Use o gate de memórias

O primeiro parâmetro do construtor é um mapa de memórias exigidas:

```java
super(Map.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT), ...);
```

É um **gate declarativo**: o Brain nem tenta iniciar a task fora da condição.
Melhor que checar dentro de `shouldRun` — é mais barato e fica visível para quem
lê o código.

## Instalar sem quebrar Vanilla

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] não instalou a task — este aldeão fica vanilla", falha);
    }
}
```

`[FATO]` verificado no bytecode de `Brain` em 1.21.1: a lista de tasks é montada
com `Map.computeIfAbsent` + `Set.add`. `setTaskList` **acrescenta**, não
substitui — nenhuma task Vanilla é removida, e por isso você não precisa remover
nada.

> **Cuidado com as sobrecargas que recebem `Set` de memórias.** No mesmo método,
> `requiredActivityMemories` usa `Map.put`: os requisitos de memória da Activity
> são **substituídos**, não acrescentados. Em `CORE` isso é inofensivo (o
> conjunto Vanilla é vazio), mas numa Activity com requisitos, você os apaga.
>
> São **cinco** sobrecargas em 1.21.1. Prefira `(Activity, int, ImmutableList)`.

Chamada por um Mixin mínimo em `initBrain` (`mixin-development.md`).

## Activity nova — quase sempre não

A tentação é registrar `meumod:trabalho` como Activity própria. Antes disso:

> **Quem escolhe a Activity ativa é a `Schedule`, através de uma task Vanilla de
> CORE.** Uma Activity que a Schedule não conhece **nunca é escolhida.**

Forçá-la exigiria justamente uma task de CORE chamando `doExclusively` a cada
tick — mais peça para o mesmo efeito.

**Alternativa que costuma ser melhor:** ponha a task em `CORE` e carregue nela as
condições que a Activity daria (só age com destino posto, só no horário de
trabalho). O resultado em jogo é o mesmo, pelo caminho mais barato e de menos
conflito.

### Prioridade dentro de CORE

É ordem **dentro** da Activity, não importância global. Ficar **depois** das
tasks Vanilla de CORE costuma ser o que você quer: água, portas, pânico,
acordar, sino e incursão decidem primeiro.

E **não assuma índice de lista** — outro mod pode ter inserido antes.

## Movimento — o erro mais comum

```java
// ✗ o cérebro reescreve o destino no mesmo tick; o mob anda dois blocos e volta
villager.getNavigation().startMovingTo(x, y, z, speed);

// ✓ quem manda é a memória
brain.remember(MemoryModuleType.WALK_TARGET,
        new WalkTarget(new BlockPosLookTarget(destino), VELOCIDADE, ALCANCE));
```

E **mantenha** a memória enquanto o destino valer — é a task que a repõe que
segura o mob no caminho. As tasks Vanilla de movimento só agem quando ela está
posta.

## Memórias próprias

```java
public static final MemoryModuleType<BlockPos> MEU_ALVO =
        Registry.register(Registries.MEMORY_MODULE_TYPE,
                Identifier.of(MOD_ID, "meu_alvo"),
                new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
```

Registrar memória e sensor **não toca em nada do Vanilla** — é o degrau 2 da
escada e convive com outros mods. Prefira isto a guardar estado de IA num
`static Map`.

Para cada memória, defina: **quem escreve, quem lê, tempo de vida, expiração,
default, se persiste, quando é limpa.**

A memória precisa estar **registrada no perfil do Brain** para ser usada.

## Horário

```java
// ✗ um horário paralelo briga com a Schedule Vanilla
if (world.getTimeOfDay() % 24000 > 2000) { ... }

// ✓ pergunte qual Activity está ativa
```

O Vanilla já tem o mecanismo e ele já é quem decide. Recriar faz o mob trocar de
modo no meio da sua lógica.

## Performance

IA roda por entidade. Os custos, do maior para o menor:

```text
PATHFINDING          o mais caro — recalcule só quando o destino muda ou falha
BUSCA DE BLOCO       raio é cúbico; metade do raio é 1/8 do custo
BUSCA DE ENTIDADE    por caixa, nunca iterando o mundo
SENSOR               tem frequência própria; respeite-a
```

**Nunca decida coisa complexa todo tick.** Além do custo, produz oscilação: a
entidade troca de alvo entre ticks e nunca chega a lugar nenhum.

## Falha

```text
[ ] alvo sumiu       → esquecer a memória, não insistir
[ ] caminho falhou   → desistir depois de N tentativas
[ ] POI ocupado      → liberar a reivindicação
[ ] chunk descarregou → pular, não travar
[ ] timeout          → toda task tem MAX_RUN_TIME
```

**Retry infinito num alvo inalcançável é o pior caso possível**: gasta o cálculo
mais caro do jogo, para sempre, sem progresso.

## Depurar

Comece pelo **estado**, não pelo código. Quase sempre a task está correta e a
memória que ela exige nunca foi escrita.

```text
Qual Activity está ativa? deveria ser essa?
Quais memórias estão preenchidas? alguma vencida?
Qual sensor deveria escrever? rodou?
O gate de memórias da task passa?
Qual task está bloqueando a que você quer?
```

Ver `debugging.md`.
