# 03 — Análise da IA

A pergunta do briefing: quais dos conceitos `Goal / Brain / Task /
Behavior / Memory / State / Scheduler / Priority / Cooldown` o Workers
realmente usa.

---

## 1. Resposta direta

| Conceito | Usa? | Onde |
|---|---|---|
| **Goal** | **sim** — é o único mecanismo | `goalSelector`, 19 Goals |
| **State** | **sim** — `enum State` por Goal | todos os `*WorkGoal` |
| **Priority** | **sim**, em dois lugares | `goalSelector` (int) e escolha de área |
| **Cooldown** | **sim**, ad-hoc | contadores `int` dentro dos Goals |
| **Brain** | **não** — o worker não é aldeão | — |
| **Memory** | **não** | estado mora em campo do Goal ou da Area |
| **Behavior** | **não** | — |
| **Task** (objeto) | **não** | ver `04-task-system.md` |
| **Scheduler** | **não** | o `goalSelector` do Vanilla faz as vezes |

O Workers roda **inteiramente no sistema de Goal do Vanilla**, que é a
API antiga (`PathfinderMob`), e não no sistema de Brain/Behavior/Memory
que o aldeão Vanilla usa desde a 1.14.

Isso é uma diferença dura com o Village Colony, que **é** obrigado a usar
Brain, porque usa o aldeão Vanilla (`fabric/brain/GoToWorkTargetTask`,
ADR-004 §11). O Workers não teve essa restrição porque criou entidade
própria.

---

## 2. O padrão universal: Goal como máquina de estados

Todo Goal de profissão tem a mesma forma:

```java
public class XWorkGoal extends Goal {
    public State state;              // enum, campo público
    public BlockPos blockPos;        // o alvo do momento
    public Stack<...> stackDeCoisas; // o que falta fazer

    canUse()   → cinco perguntas em série
    start()    → setState(PRIMEIRO_ESTADO)
    tick()     → switch(state) { ... }
    setState() → this.state = state;  (+ um log comentado)
}
```

O `setState` tem sempre a mesma linha comentada, em todos os Goals:

```java
// LumberjackWorkGoal:301, DepositItemsToStorage:261,
// GetNeededItemsFromStorage:309, BuilderWorkGoal:331
//if(worker.getOwner() != null) worker.getOwner()
//    .sendSystemMessage(Component.literal(state.toString()));
```

É o instrumento de depuração do autor, deixado comentado no lugar. O seu
`Project-State.md §11` diz "instrumentar antes de suspeitar" — aqui está
a mesma lição, escrita de outro jeito e paga do mesmo modo.

**Tamanho dos enums de estado:**

| Goal | Estados |
|---|---:|
| `LumberjackWorkGoal` | 17 |
| `GetNeededItemsFromStorage` | 15 |
| `DepositItemsToStorage` | 14 |
| `BuilderWorkGoal` | ~13 |
| `WorkerGoHomeGoal` | 4 |

Dezessete estados num `switch` de 210 linhas é o custo real desse padrão.

---

## 3. Escolha de tarefa e prioridade

Não há fila. O trabalhador **procura o próprio trabalho**, e a decisão
inteira está em um método estático por profissão:

```java
// LumberjackWorkGoal.java:521-552
public static List<LumberArea> getAvailableWorkAreasByPriority(
        ServerLevel level, LumberjackEntity lumberjack, LumberArea currentArea) {

    List<LumberArea> list = level.getEntitiesOfClass(
            LumberArea.class, lumberjack.getBoundingBox().inflate(64));

    for (LumberArea area : list) {
        if (area == null || area == currentArea || !area.canWorkHere(lumberjack)) continue;

        int priority = 0;
        priority += area.isWorkerPerfectCandidate(lumberjack) ? 10 : 1;
        if (!area.isBeingWorkedOn()) priority += 3;
        priority += area.time;                    // ← envelhecimento
        priorityMap.put(area, priority);
    }
    // ordena decrescente
}
```

Três achados aqui, todos aproveitáveis:

**(a) `isWorkerPerfectCandidate` — pontuar por prontidão.**
Vale 10 pontos. Pergunta: *este trabalhador já carrega o que esta área
exige?* (`LumberArea:89-107`: muda certa, machado, tesoura). Um lenhador
com machado prefere a área que precisa de machado. É pontuação por
aptidão momentânea, não por profissão.

**(b) `area.time` — envelhecimento contra a inanição.**
`AbstractWorkAreaEntity.tick()` faz `if(tickCount % 20 == 0) time++;`
(linha 110), e `setTime(0)` acontece quando alguém começa a trabalhar
(`LumberjackWorkGoal:103`). Ou seja: **a área acumula prioridade por
segundo em que ninguém a atende**. É *aging* clássico de escalonador, e é
o que impede que a área mais distante nunca seja visitada.

**(c) `isBeingWorkedOn` — a reserva.** Vale só 3 pontos, e não é um
bloqueio. É reserva **fraca**: outro trabalhador pode pegar a mesma área
se ela pontuar melhor por outros critérios. Ver §5.

---

## 4. Cooldown, retry e timeout

Não existe mecanismo geral. Cada Goal reinventa com um `int`:

```java
// DepositItemsToStorage:180-183 — retry de 60 s
if(++retryTime >= 20*60){ retryTime = 0; this.start(); }

// WorkerGoHomeGoal:88-89 — cooldown de 1 s antes de procurar casa
if (++cooldown < 20) return;

// DepositItemsToStorage:128-131 — espera a animação do baú
if(timer++ < 40){ return; } timer = 0;

// RecruitStorageUpkeepGoal:46-49 — orçamentos por execução
OPEN_TIME = 16; FOOD_BUDGET = 4; MAX_CHESTS = 16;
```

**Timeout de tarefa não existe.** Um trabalhador preso em
`PREPARE_WOOD_CUTTING` sem machado e sem baú fica ali indefinidamente —
o `return` sem troca de estado (`LumberjackWorkGoal:216`) é um laço
fechado. Só sai quando a noite chega e `canUse()` vira falso.

Isso é uma **lacuna real do Workers**, e é o tipo de coisa que o
`Task.state` do seu projeto já resolve por construção.

---

## 5. Concorrência: dois trabalhadores no mesmo recurso

O Workers usa **três níveis de reserva**, todos frouxos:

### Nível 1 — a área: `isBeingWorkedOn`

```java
// AbstractWorkAreaEntity.java:49
public boolean isBeingWorkedOn;
```

Um `boolean` público, sem dono. Escrito por quem começa
(`LumberjackWorkGoal:102`), limpo por quem termina (linha 272) e por
quem morre (`AbstractWorkerEntity.die():547`).

E há um **watchdog de distância**, que é a parte interessante:

```java
// AbstractWorkerEntity.java:119-123
if(tickCount % 20 == 0){
    if(this.getCurrentWorkArea() != null){
        double distance = this.getHorizontalDistanceTo(getCurrentWorkArea().position());
        if(distance >= 1000) this.getCurrentWorkArea().isBeingWorkedOn = false;
    }
}
```

Se o trabalhador se afastou demais (1000 em distância **quadrada** ≈ 31
blocos), a área é liberada. É o remédio para a reserva órfã: o dono levou
o trabalhador embora, o chunk descarregou, o Goal travou. Sem isso, uma
área ficaria marcada para sempre.

**Isto é um achado de nível A.** É exatamente o problema que o seu
`Task.release()` resolve na morte do trabalhador — e o Workers mostra um
segundo caso que a morte não cobre: o trabalhador **vivo que sumiu do
lugar**. Vale conferir se o seu `LumberjackWork`/`WorkAssignment` libera
tarefa quando o aldeão se afasta do alvo indefinidamente.

### Nível 2 — a árvore: `Tree.setInWork`

```java
// world/Tree.java:62-68
public void setInWork(boolean b) { this.isInWorks = b; }
```

**Está morto.** `setInWork(true)` é chamado em `LumberjackWorkGoal:167`,
`setInWork(false)` em :84, e `isInWorks()` **nunca é lido em lugar
nenhum**. Verificado por grep em todo o `src/main`. A reserva por árvore
existe na API e não existe no comportamento.

Ela também não funcionaria: cada `Tree` vive na `Stack` de uma
`LumberArea`, e dois lenhadores na mesma área usam a **mesma** pilha —
`stackOfTrees.pop()` (linha 166) já garante exclusão por remoção. A
reserva seria redundante ali, e inútil entre áreas.

### Nível 3 — o baú: nenhuma

Dois trabalhadores podem abrir e esvaziar o mesmo baú ao mesmo tempo. O
que existe é uma marca de animação, e ela é compartilhada:

```java
// AbstractChestGoal.java:87-93
compoundTag = chestBlockEntity.getPersistentData();
if(compoundTag.contains("isOpened")) isOpened = ...
```

`isOpened` é escrito no `getPersistentData()` do `ChestBlockEntity` para
evitar que dois trabalhadores toquem o som de abrir duas vezes. É
proteção de **animação**, não de conteúdo. E note: escrever em
`getPersistentData()` de um block entity Vanilla é sujeira que fica no
save do jogador.

**Conclusão do §5:** a concorrência do Workers é otimista e frouxa em
todos os níveis. Funciona porque na prática há poucos trabalhadores por
área. Não escalaria, e não sobreviveria a uma revisão. **Não copiar.**

---

## 6. Comportamento em condições adversas

Levantado do código, condição a condição:

| Situação | O que o Workers faz | Onde |
|---|---|---|
| sem ferramenta | `addNeededItem(...)`, `return` sem trocar estado. `needsToGetItems()` vira true, `GetNeededItemsFromStorage` assume | `LumberjackWorkGoal:186-192` |
| sem material | idem, com `required=true` | `LumberjackWorkGoal:463` |
| baú cheio | tenta o próximo baú, depois marca a área como visitada, avisa o dono, tenta outra área | `DepositItemsToStorage:186-193` |
| nenhum armazém | avisa o dono, espera 60 s, recomeça | `DepositItemsToStorage:174-184` |
| inventário próprio cheio | avisa, e liga `forcedDeposit = true` | `GetNeededItemsFromStorage:226-233` |
| item não existe em armazém nenhum | limpa `neededItems` e desiste | `GetNeededItemsFromStorage:210-212` |
| área removida do mundo | `isAreaNotRemoved()` no `canUse()`, zera a referência | `LumberjackWorkGoal:54-60` |
| baú sumiu do mundo | remove da `storageMap`, tenta o próximo | `DepositItemsToStorage:111-114` |
| cama sumiu | zera `assignedBedPos`, volta a `MOVE_TO_HOME` | `WorkerGoHomeGoal:124-131` |
| noite | `needsToSleep()` = `!isDay()` desliga todos os Goals de trabalho | `AbstractWorkerEntity:287` |
| trabalhador morre | libera área e casa | `AbstractWorkerEntity:544-550` |

**O que impressiona é a cobertura.** Cada erro tem estado próprio, e o
estado sabe o que fazer. Onze estados de erro nomeados entre os dois
Goals de baú:

```text
ERROR_NO_STORAGE_FOUND        ERROR_STORAGE_FULL
ERROR_STORAGE_NO_CONTAINERS   ERROR_ITEM_NOT_IN_STORAGE
ERROR_OWN_INVENTORY_FULL
```

**Isto é o achado de nível A do documento.** Não a implementação — a
disciplina. Cada modo de falha tem *nome*, e o nome vira mensagem ao
jogador. Compare com o que o seu §17 registra sobre o E14: três sessões
gastas porque a Fase 10 dizia "planned no building" sem dizer **qual dos
cinco motivos**. A `ConstructionPlanner.silent` que você escreveu em
08-14 é exatamente esta ideia, chegada pelo caminho caro.

### A anti-spam de notificação

```java
// AbstractWorkerEntity.java:418-472
private long lastNotifyDay = Long.MIN_VALUE;
private boolean notifyGateOpen = true;

public boolean canNotifyOwner() {
    long day = level.getDayTime() / 24000L;
    if (day != lastNotifyDay) return true;   // dia novo reabre o portão
    return notifyGateOpen;
}
```

Uma mensagem por dia de jogo, por trabalhador. E o portão reabre quando o
jogador **interage** com o trabalhador (`mobInteract:487-493`) — "você
veio falar comigo, então eu volto a falar". Há ainda
`notifyOwnerAlways()` para o que não pode ser silenciado.

Pensado. É o mesmo problema do E11 do seu §17 — nove dispensas em
dezesseis minutos, uma linha de log por ciclo. O Workers resolveu o
sintoma (o spam); o E11 pede resolver a causa (o rodízio). Vale ter os
dois.

---

## 7. Os comentários como fonte histórica

Sem `.git`, os comentários do autor são o único registro de correção. Os
mais informativos, todos verificados no arquivo:

```java
// AbstractWorkerEntity.java:77-81
// The inherited recruits door goal only activates on a RecruitPathNavigation,
// which workers don't use, so it never fires. Swap it for a worker-aware copy
// that checks WorkersGroundPathNavigation instead.
```
→ herança de outro mod causou um Goal que nunca disparava. **Silencioso.**

```java
// LumberjackWorkGoal.java:186-188
// Register on the worker directly (not just locally) so needsToGetItems()
// turns true and GetNeededItemsFromStorage fetches the tool now instead
// of the goal getting stuck in this prepare state.
```
→ o Goal travava esperando ferramenta que ninguém ia buscar. **Deadlock.**

```java
// GetNeededItemsFromStorage.java:76-77
// Pre-scan: only target chests that actually contain a needed item, so the
// worker walks straight to the right chest instead of opening every chest.
```
→ desempenho: o trabalhador abria todos os baús do armazém.

```java
// LumberjackWorkGoal.java:446-447
// No sapling selected -> nothing to replant. The "any sapling" fallback
// was removed so the lumberjack only ever plants the chosen sapling.
```
→ o fallback "qualquer muda" plantava a espécie errada.

```java
// WorkersNodeEvaluator.java:88-92
// Water is traversable but expensive. [...] a hard 128 here made even a
// single stream block effectively impassable, so recruits never set foot
// in water at all.
```
→ o custo proibitivo criou um bug pior que o que resolvia.

```java
// BuilderWorkGoal.java:169
this.builderEntity.currentBuildArea.scanBreakArea();//SOMETHING WRONG I CAN FEEL IT
```
→ defeito conhecido e não localizado, deixado no código.

Esses seis comentários valem mais que o `update.json`. Cada um é um
problema real que um mod de trabalhadores encontra, e três deles — o Goal
que nunca dispara, o deadlock por ferramenta, e o custo proibitivo que
piora — são armadilhas que o seu projeto pode encontrar de forma
diferente mas pelo mesmo motivo.

---

## 8. O que trazer, e o que não

### Trazer (conceito, reimplementado)

1. **Estados de erro nomeados.** Todo motivo de não-trabalho tem nome
   próprio e mensagem própria. Você já começou isso com
   `ConstructionPlanner.silent`; generalize.
2. **Envelhecimento na seleção de alvo** (`area.time`). Impede inanição
   sem escalonador.
3. **Pontuação por prontidão** (`isWorkerPerfectCandidate`). O
   trabalhador que já tem a ferramenta é preferido para o trabalho que a
   exige.
4. **Watchdog de distância** para liberar reserva órfã de trabalhador
   vivo que se afastou.
5. **Portão de notificação por dia de jogo**, reaberto por interação.

### Não trazer

1. **Goal como máquina de 17 estados.** É o padrão do Workers inteiro e é
   exatamente o que sua ADR-004 e sua camada `core/task` evitam. Você já
   tem `Task` com estado explícito, testável sem Minecraft.
2. **Reserva por booleano público sem dono.** Sem transação, sem
   liberação garantida.
3. **`DONE → this.start()`.** Reiniciar o Goal a partir de dentro
   esconde o fim do trabalho e impede um estado de repouso barato.
4. **Estado do trabalho morando na área.** Acoplamento total,
   intestável.
