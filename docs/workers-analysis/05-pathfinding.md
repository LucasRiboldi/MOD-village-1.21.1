# 05 — Pathfinding

É a parte tecnicamente mais forte do Workers, e a que menos se aplica
diretamente ao Village Colony. Vale entender por inteiro mesmo assim: os
problemas que ela resolve são reais, e o seu mod vai encontrar alguns
deles por outro caminho.

---

## 1. A pilha, de baixo para cima

```text
WorkerPathNavigation                 (workers, 70 linhas)
   └─ WorkersGroundPathNavigation    (workers, 132)
        └─ AsyncPathNavigation       (RECRUITS — não está no repositório)

WorkersAsyncPathfinder               (workers, 273)  extends PathFinder
   └─ usa AsyncPath, NodeEvaluatorCache, NodeEvaluatorGenerator (RECRUITS)

WorkersNodeEvaluator                 (workers, 784)  extends NodeEvaluator
```

**Metade da infraestrutura é do Recruits e não está aqui.** O que o
Workers escreveu por conta própria são as duas classes de algoritmo — o
pathfinder e o node evaluator — e o comentário explica por quê:

```java
// WorkerPathNavigation.java:17-25
// They build on the recruits async navigation INFRASTRUCTURE
// (AsyncGroundPathNavigation, which the released recruits mod exposes),
// but use WORKER-OWN copies of the improved pathfinder + node evaluator
// [...] because the released recruits mod ships the old algorithm.
```

Ou seja: o autor não conseguiu atualizar o mod-base a tempo e duplicou o
algoritmo no addon. É dívida técnica assumida e documentada.

---

## 2. `WorkersAsyncPathfinder` — três ideias que valem

### 2.1 Heurística com peso no eixo Y

```java
// WorkersAsyncPathfinder.java:31, 260-265
private static final float Y_WEIGHT = 3.0F;

private float weightedDistance(Node node, Target target) {
    float dy = ((float) target.y - node.y) * Y_WEIGHT;
    return sqrt(dx*dx + dy*dy + dz*dz);
}
```

Distância vertical vale três vezes a horizontal na heurística. Efeito: o
A* **compromete-se** com rotas que convergem para o Y do alvo — descer na
caverna, tomar a escada — em vez de empacar num ponto horizontalmente
próximo e verticalmente errado.

O comentário diz que é o que conserta "miners being dumb about buried
targets" (`AbstractWorkerEntity:92-93`).

### 2.2 Orçamento adaptativo com detecção de estagnação

```java
// WorkersAsyncPathfinder.java:112-124
int baseBudget = (int)(maxVisitedNodes * multiplier);
int fullBudget = baseBudget * 8;                 // EXACT_SEARCH_BUDGET
int noProgressLimit = Math.max(64, baseBudget / 2);
```

E no laço (linhas 167-187):

```java
if (node.h < bestHSeen) { bestHSeen = node.h; nodesSinceImprovement = 0; }
else                    { nodesSinceImprovement++; }
...
if (i >= baseBudget && nodesSinceImprovement >= noProgressLimit) break;
```

Tradução: **gasta 8× o orçamento normal enquanto estiver melhorando, e
desiste rápido quando parar de melhorar.**

O comentário nomeia o caso que motivou:

> An unreachable target (walled in, in lava, dead mob) stalls and stops
> shortly after the base budget instead of grinding to 8x every time —
> exactly the mass-battle spike case.

Isso é **desempenho por não fazer trabalho inútil**, não por fazer o
trabalho mais rápido. É o tipo de otimização que se paga em servidor real
e que nenhum teste unitário jamais indicaria.

### 2.3 Duas medidas de distância, para dois fins

Esta é a mais fina das três, e a mais fácil de errar.

```java
// WorkersAsyncPathfinder.java:126-136, 210-226
Node closestTrueNode;   // menor distância EUCLIDIANA pura
float closestTrueDist;
...
if (exact) {
    best = reconstructPath(reachedNode, ...,  true);
} else {
    best = reconstructPath(closestTrueNode, ..., false);
}
```

O A* usa a heurística com peso em Y para **buscar**. Mas quando falha, o
fallback usa a distância **sem peso** para escolher onde parar. O
comentário explica o defeito que isso conserta:

> Using true distance (not the Y-weighted heuristic / getBestNode) stops
> the recruit from preferring to climb onto the nearest raised block on
> flat ground.

Com a heurística pesada, o "melhor nó" em terreno plano seria o mais alto
— e o mob subia em cima da pedra mais próxima em vez de andar. Duas
métricas, dois papéis. **Lição transferível para qualquer busca com
heurística enviesada: a métrica que guia não é a métrica que decide.**

### 2.4 Chegada exata: alvo, ou alvo+1 em Y

```java
// WorkersAsyncPathfinder.java:146-161
boolean sameColumn = node.x == target.x && node.z == target.z;
if (sameColumn && (node.y == target.y || node.y == target.y + 1)) {
    target.setReached();
}
```

Com o comentário:

> Move orders point at the surface block, which is solid — you cannot
> stand INSIDE it, so the standable spot is one above. Without this,
> ground targets never registered as reached and always fell through to
> the fallback.

Este é o defeito de "chega perto mas nunca chega" resolvido na raiz.

**E ele conversa diretamente com o seu E4** (`Project-State.md §17`):

> `path held: no` e o aldeão chega assim mesmo

Não é a mesma causa — o seu E4 é sobre a memória `WALK_TARGET` do Brain
ser descartada, e o Workers nem usa Brain. Mas é a mesma família de
problema: **"chegou" e "o caminho terminou" são coisas diferentes**, e o
seu `GoToWorkTargetTask.COMPLETION_RANGE = 2` é a resposta que você deu à
mesma pergunta.

---

## 3. `WorkersNodeEvaluator` — modelagem de custo

784 linhas, das quais o interessante é a modelagem explícita:

```java
// constantes, linhas 52-71
SOFT_FALL_MIN = 2;   SOFT_FALL_MALUS = 2.0F;
HARD_FALL_MIN = 4;   HARD_FALL_MALUS = 12.0F;
MAX_SAFE_FALL = 5;                          // acima disso: BLOCKED
WATER_ADJACENT_MALUS = 4.0F;
WATER_CROSS_BASE     = 8.0F;
WATER_CROSS_GROWTH   = 6.0F;
WATER_CROSS_MAX_PROBE = 4;
DEAD_END_EXITS = 1;  DEAD_END_MALUS = 4.0F;
PATH_BLOCK_BONUS = 2.0F;
```

### 3.1 Água por **largura**, não por presença

O achado mais elegante do arquivo:

```java
// linhas ~236-250
private int waterCrossingWidth(BlockPos pos) {
    return Math.min(waterSpanAlongAxis(pos, 1, 0),
                    waterSpanAlongAxis(pos, 0, 1));
}
```

Mede a **menor** travessia de água que passa por aquele ponto, ao longo
de X e de Z, com sonda limitada a 4 blocos. Um riacho de 1 bloco de
largura, mesmo com 200 de comprimento, mede 1 e continua barato. Um lago
mede 4+ em ambos os eixos e recebe custo proibitivo.

E o comentário registra a correção que originou isso:

> a hard 128 here made even a single stream block effectively impassable,
> so recruits never set foot in water at all.

**A lição:** custo proibitivo é uma forma de bug. O primeiro remédio
("água é cara") produziu um problema pior que o original, e a correção
foi trocar um valor por uma **medida** da situação real.

### 3.2 Cache de nós já modelados

```java
// linhas 132-137
long key = BlockPos.asLong(x, y, z);
if (!this.shapedPositions.add(key)) return node;
```

O A* consulta o mesmo nó muitas vezes; a modelagem cara (sondar queda,
medir largura de água, contar saídas) roda **uma vez por posição por
busca**. É `LongOpenHashSet` do fastutil, limpo em `prepare()`.

Padrão simples, e obrigatório em qualquer varredura que rode dentro de um
laço de busca.

### 3.3 Preferência por corredores e por caminho batido

```java
int exits = countWalkableNeighbours(pos);
if (exits <= DEAD_END_EXITS) node.costMalus += DEAD_END_MALUS;

Block below = level.getBlockState(pos.below()).getBlock();
if (below instanceof DirtPathBlock || below instanceof GravelBlock)
    node.costMalus = Math.max(0.0F, node.costMalus - PATH_BLOCK_BONUS);
```

Beco sem saída é penalizado; estrada de terra é bonificada. O segundo é
puramente estético — faz o mob **usar as ruas da vila** — e é barato.

**Nota direta para o seu projeto:** a sua TASK-043 vai pavimentar
estradas. Um bônus de custo em caminho de terra faria os aldeões
*usarem* a estrada que a colônia construir, sem nenhum código de
navegação. É a maneira mais barata de tornar a estrada visível no
comportamento. Só que o aldeão Vanilla usa `WalkNodeEvaluator`, e mudar
isso significa mixin em navegação — o que a sua ADR-004 desencoraja.
Registrar como ideia, não como tarefa.

---

## 4. Portas, cercas e obstáculos

| Obstáculo | Tratamento | Onde |
|---|---|---|
| porta | `setCanOpenDoors(true)`, `setCanPassDoors(true)` + `WorkerOpenDoorGoal` na prioridade 6 | `WorkerPathNavigation:36-38`, `AbstractWorkerEntity:81` |
| porta fechada de madeira | malus `0.0F` — custo zero, é caminho normal | `WorkersNodeEvaluator:100` |
| cerca / fence gate | malus `-1.0F` = **intransponível** | idem, linha 102 |
| lava | `-1.0F` | linha 104 |
| folha | `-1.0F` | linha 105 |
| alçapão | `-1.0F` | linha 99 |
| neve fofa | `-1.0F` | linha 98 |
| fogo / dano | `32.0F` | linhas 96-97 |
| queda | escalonado 2/12/bloqueado | §3 acima |

**O fence gate não tem tratamento especial.** É cerca, e cerca é
intransponível. O trabalhador contorna. É a decisão certa e simples —
abrir portão exige lógica de estado do bloco que não paga.

### A correção da porta herdada

```java
// AbstractWorkerEntity.java:77-81
// The inherited recruits door goal only activates on a RecruitPathNavigation,
// which workers don't use, so it never fires. Swap it for a worker-aware copy
this.goalSelector.removeAllGoals(g -> g instanceof RecruitsDoorInteractGoal);
this.goalSelector.addGoal(6, new WorkerOpenDoorGoal(this, true));
```

Um Goal herdado que checava o **tipo da navegação** e portanto nunca
disparava para a subclasse. Falha silenciosa por herança. Vale como
alerta geral: comportamento herdado que testa `instanceof` da própria
infraestrutura quebra em silêncio quando a infraestrutura é trocada.

---

## 5. Detecção de travamento (*stuck*)

**Não há detecção de stuck no código do Workers.** O que existe:

* o watchdog de distância que libera a área (`AbstractWorkerEntity:119-123`);
* um pulo reativo, só no upkeep:
  ```java
  // RecruitStorageUpkeepGoal.java:155-157
  if (this.recruit.horizontalCollision) { this.recruit.getJumpControl().jump(); }
  ```
* o retry de 60 s dos Goals de baú, que recomeça a máquina.

A detecção de stuck de verdade, se existir, está no `AsyncPathNavigation`
do Recruits — fora deste repositório. **Não é possível afirmar que
existe.**

---

## 6. Um defeito encontrado

Registrado porque a análise tem de ser honesta nos dois sentidos:

```java
// WorkerPathNavigation.java:29-49
private static BiFunction<Integer, NodeEvaluator, PathFinder> pathfinderSupplier =
        (range, nodeEvaluator) -> new PathFinder(nodeEvaluator, range);

public WorkerPathNavigation(AbstractWorkerEntity worker, Level world) {
    super(worker, world);
    if (RecruitsServerConfig.UseAsyncPathfinding.get()) {
        pathfinderSupplier = (range, ne) ->
                new WorkersAsyncPathfinder(ne, range, nodeEvaluatorGenerator, this.level);
    }
}
```

O campo é **`static`** e é reatribuído no construtor de **instância**,
capturando `this.level` daquele trabalhador. Todos os trabalhadores do
servidor passam a compartilhar o supplier do último construído.

Em single-player com um mundo só, isso nunca aparece. Num servidor com
Overworld e Nether, um trabalhador construído no Nether faz o
`pathfinderSupplier` global apontar para o `Level` do Nether — e o
`WorkersAsyncPathfinder` recebe o `Level` errado no campo que ele usa
para construir o `AsyncPath`.

Não foi possível confirmar o efeito sem o código do `AsyncPath`. Mas o
padrão — `static` mutável capturando estado de instância — é
inequivocamente errado. **Não copiar. E é um bom lembrete para o seu §9,
"Registro único, Overworld":** o mesmo tipo de suposição, escrita no
código e não no tipo.

---

## 7. O que se aplica ao Village Colony

### Aplica-se pouco, e por bom motivo

O seu mod usa o **aldeão Vanilla** com `Brain` e `WALK_TARGET`
(`GoToWorkTargetTask`). Você não controla o pathfinder: quem navega é o
`VillagerNavigation` do jogo, guiado pela memória. Trocar isso exigiria
mixin em navegação, contra a ADR-004.

E isso está **certo** para os seus objetivos: o aldeão que navega como
aldeão continua compatível com todo o resto do Vanilla e com outros mods.

### Aplica-se o método

Cinco lições transferíveis, nenhuma exigindo código do Workers:

1. **A métrica que guia a busca não é a que decide o resultado.**
   Vale para qualquer busca sua — `BuildSiteScanner`, `TreeScanner`,
   `ChestScanner`.
2. **Orçamento adaptativo:** gaste enquanto melhora, desista quando
   estagnar. O seu `BuildSiteScanner.sweepPausedAt` já é primo disso —
   você separou "não achei" de "não terminei de olhar". A peça que falta
   é a estagnação: *estou olhando e não estou chegando mais perto*.
3. **Custo proibitivo é um bug em potencial.** Onde você tem uma regra
   de aceitação binária — a grama do campo da TASK-047 é o exemplo — a
   correção certa raramente é trocar o limiar; é medir a situação real.
   Você chegou a essa conclusão na TASK-047; o Workers a documenta na
   água.
4. **Cache de posição já avaliada** em qualquer varredura dentro de laço.
5. **"Chegou" e "o caminho acabou" são perguntas diferentes.** Ver o seu
   E4 e o `COMPLETION_RANGE`.
