# Workflow — novo comportamento (task)

O caminho mais comum para "quero que o aldeão faça X". Não exige profissão nova,
não exige Activity nova, e exige **no máximo um** Mixin — que provavelmente já
existe.

---

## 1. Domínio

```text
O QUE ele faz, em uma frase
QUANDO começa
QUANDO termina
O QUE ele precisa saber para isso     ← vira memória
```

Se a resposta da última for "nada", desconfie: um comportamento sem estado
costuma rodar sempre e não é o que você quer.

## 2. Pesquisa

```bash
ls docs/research 2>/dev/null
grep -ril "brain\|task\|memory" docs/ | head
```

`workflows/analyze-villager-behavior.md` se a cadeia não estiver mapeada.
`minecraft-code-research` se faltar fato sobre o Vanilla.

## 3. As peças necessárias

```text
[ ] preciso de MEMÓRIA nova?     → workflows/add-memory-or-sensor.md
[ ] preciso de SENSOR novo?      → idem
[ ] preciso de ACTIVITY nova?    → workflows/add-activity.md (leia antes: quase sempre não)
[ ] preciso de POI?              → workflows/add-job-site.md
[ ] preciso de PROFISSÃO?        → provavelmente NÃO. examples/guard-villager-decision.md
```

**A maioria dos comportamentos precisa de: uma task + uma memória.** Nada mais.

## 4. Plano

`templates/behavior-plan.md`. Os campos que decidem:

```text
PRECONDIÇÕES     o que precisa ser verdade
MEMÓRIAS         quais ele exige (vira o gate do construtor)
ACTIVITY         onde vive — CORE, normalmente
PRIORIDADE       depois das tasks Vanilla de CORE
AÇÃO             o que faz
CONCLUSÃO        quando termina com sucesso
INTERRUPÇÃO      o que o faz parar
TIMEOUT          quanto tempo no máximo
FALHA            o que acontece, e o que fazer depois
CUSTO            frequência × população
```

## 5. Escrever a task

```java
public final class MinhaTask extends MultiTickTask<VillagerEntity> {

    private static final int MAX_RUN_TIME = 24_000;

    public MinhaTask() {
        super(Map.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT),
              MAX_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return ehHorarioDeTrabalho(world, villager)
            && temAlvo(villager);
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity villager, long time) {
        mirar(villager);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
        return ehHorarioDeTrabalho(world, villager)
            && temAlvo(villager);
    }
}
```

`[FATO]` MC 1.21.1: `tick`, `tryStarting` e `stop` são `final` — o Brain dirige.
Você sobrescreve `shouldRun` / `run` / `shouldKeepRunning` / `keepRunning` /
`finishRunning`.

```text
[ ] usei o GATE de memórias no construtor, não checagem em shouldRun
[ ] shouldRun e shouldKeepRunning são coerentes
[ ] MAX_RUN_TIME definido
[ ] a task perde para PANIC/RAID (via condição ou ordem)
```

## 6. Movimento

```java
// ✗ o cérebro reescreve no mesmo tick
villager.getNavigation().startMovingTo(x, y, z, 0.5f);

// ✓
brain.remember(MemoryModuleType.WALK_TARGET,
        new WalkTarget(new BlockPosLookTarget(destino), 0.5f, ALCANCE));
```

E **mantenha** a memória em `keepRunning` enquanto o destino valer.

## 7. Instalar

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORIDADE, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] não instalou a task — este aldeão fica vanilla", falha);
    }
}
```

Chamado pelo Mixin em `initBrain` que **já deve existir**. Se você está criando o
segundo Mixin em `VillagerEntity`, pare e reveja.

```text
[ ] setTaskList ACRESCENTA — nada Vanilla é removido
[ ] prioridade DEPOIS das tasks Vanilla de CORE
[ ] não assumo índice de lista
[ ] exceção capturada — degrada para Vanilla
```

```bash
./gradlew build
```

## 8. Falha

```text
[ ] alvo sumiu           → esquecer, escolher outro
[ ] caminho falhou       → desistir após N tentativas
[ ] chunk descarregou    → pular (caso NORMAL)
[ ] anoiteceu            → deixar a Schedule assumir
[ ] inimigo perto        → deixar PANIC assumir
[ ] inventário cheio     → depositar antes
[ ] timeout              → MAX_RUN_TIME encerra
```

**Retry infinito é bug.** `[FATO]` o Vanilla tem
`CANT_REACH_WALK_TARGET_SINCE` justamente por isso.

## 9. Testar

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

```text
[ ] a task roda quando deveria
[ ] NÃO roda quando não deveria
[ ] o aldeão continua dormindo, comendo e socializando   ← o que separa
[ ] em pânico, ele foge (a task cede)
[ ] com 10 aldeões, o TPS aguenta
[ ] fechar e reabrir o mundo não deixa estado inconsistente
```

O terceiro item é o teste de não-regressão do Vanilla, e é o mais pulado.

## 10. Documentar

```text
[ ] behavior-plan.md preenchido
[ ] a memória usada está documentada
[ ] descobertas sobre o Vanilla voltam para docs/research/
```

## Fechamento

`checklists/behavior-activity.md` e `checklists/villager-feature.md`.

Relate o que foi **verificado rodando**, separado do que apenas tem teste escrito.
