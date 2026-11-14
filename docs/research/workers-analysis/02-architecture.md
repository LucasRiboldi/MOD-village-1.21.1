# 02 — Arquitetura do Workers

Mapa construído a partir do código, não de nomes de arquivo.

---

## 1. O mapa real

```text
Player (dono)
 │
 ├── contrata via Recruits ────────► RecruitsHireTradesRegistry
 │                                   (VillagerEvents.registerWorkerTrades)
 │
 ├── planta no mundo ──────────────► AbstractWorkAreaEntity
 │                                    ├── LumberArea      ┐
 │                                    ├── CropArea        │ cada uma:
 │                                    ├── MiningArea      │  - caixa AABB
 │                                    ├── FishingArea     │  - varre o mundo
 │                                    ├── AnimalPenArea   │  - guarda Stack<BlockPos>
 │                                    ├── BuildArea       │  - tem GUI própria
 │                                    ├── StorageArea     │  - tem pacote próprio
 │                                    ├── KitchenArea     │
 │                                    ├── MarketArea      │
 │                                    └── HomeArea        ┘
 │
 └── possui ────────────────────────► AbstractWorkerEntity
                                       (extends recruits.AbstractChunkLoaderEntity)
                                        │
                                        ├── inventory (SimpleContainer, slots 0-5
                                        │              reservados p/ equipamento)
                                        ├── List<NeededItem> neededItems
                                        ├── farmedItems : int
                                        ├── lastStorage : UUID
                                        ├── homeAreaUUID : UUID
                                        ├── morale (herdado)
                                        ├── followState : int  (0 wander, 1 hold,
                                        │                       6 working)
                                        │
                                        └── goalSelector
                                             prio 0  WorkerTakeCoverGoal
                                             prio 0  WorkerFleeGoal
                                             prio 1  WorkerGoHomeGoal
                                             prio 2  DepositItemsToStorage
                                             prio 2  GetNeededItemsFromStorage
                                             prio 6  WorkerOpenDoorGoal
                                             prio ?  <Profissão>WorkGoal
```

Fonte: `entities/AbstractWorkerEntity.java:68-84` (registerGoals),
`entities/workarea/` (11 arquivos), `WorkersMain.java:52-80`.

---

## 2. As camadas, e o que cada uma faz

### 2.1 `entities/` — os trabalhadores

Uma subclasse por profissão. Nove no total:

| Classe | Linhas | Goal principal |
|---|---:|---|
| `FarmerEntity` | — | `FarmerWorkGoal` (653) |
| `LumberjackEntity` | — | `LumberjackWorkGoal` (592) |
| `MinerEntity` | — | `MinerWorkGoal` (516) |
| `BuilderEntity` | — | `BuilderWorkGoal` (719) |
| `FishermanEntity` | — | `FishermanWorkGoal` (283) |
| `AnimalFarmerEntity` | — | `AnimalFarmerWorkGoal` (488) |
| `CookEntity` | — | `CookWorkGoal` (512) |
| `CourierEntity` | 356 | `CourierWorkGoal` (619) |
| `MerchantEntity` | 715 | `MerchantWorkGoal` (394) |

O contrato da base é pequeno e claro
(`AbstractWorkerEntity.java:86,291,309`):

```java
public abstract AbstractWorkAreaEntity getCurrentWorkArea();
public abstract Predicate<ItemEntity> getAllowedItems();
public abstract List<Item> inventoryInputHelp();
```

Três métodos. Todo o resto do comportamento comum — inventário, coleta de
drops, `NeededItem`, moral, notificação ao dono, quebra de bloco com
progresso — está na base.

### 2.2 `entities/ai/` — os Goals

19 Goals. Três famílias:

* **de profissão** — uma máquina de estados por profissão;
* **de logística** — `AbstractChestGoal` e as duas filhas
  (`DepositItemsToStorage`, `GetNeededItemsFromStorage`);
* **de sobrevivência** — `WorkerGoHomeGoal`, `WorkerFleeGoal`,
  `WorkerTakeCoverGoal`.

### 2.3 `entities/ai/navigation/` — o pathfinding próprio

Quatro classes, 1.200+ linhas. É a parte tecnicamente mais forte do mod.
Documentada em `05-pathfinding.md`.

### 2.4 `entities/workarea/` — as áreas

Onze `Entity` (não `BlockEntity`) invisíveis. Documentadas em
`07-work-areas.md`.

### 2.5 `world/` — os modelos de dados

O único pacote do Workers que se parece com um "domínio":

```text
Tree.java                 três Stack<BlockPos>: shear, strip, break
NeededItem.java           pedido de item com Predicate + contagem + fonte
BuildBlock.java           bloco de uma construção
BuildBlockParse.java      BlockState → Item
ScannedBlock.java         bloco varrido
StructureManager.java     leitura/escrita de estruturas em disco
CourierRoute.java         rota com waypoints e ações
CourierAction.java        ação num waypoint
NeededItem, WorkersMerchantTrade, VillagerInviteRegistry
```

Mesmo assim, `Tree` e `BuildBlock` importam `net.minecraft.core.BlockPos`
e `BlockState` — não há isolamento de tipo. É a diferença mais visível
com a sua ADR-005.

### 2.6 `client/` — GUI e render

30 classes, 4.500+ linhas. Uma tela por área de trabalho, mais widgets de
scroll, dropdown com pastas, e um `StructurePreviewWidget` que renderiza
a estrutura projetada no mundo. É metade do esforço visível do mod.

### 2.7 `network/` — 27 mensagens

Uma por operação de GUI. Padrão `MessageUpdate<Area>` para cada tipo de
área. Documentado em `10-project-comparison.md §GUI/Networking`.

---

## 3. O fluxo de trabalho, do início ao fim

Usando o lenhador como exemplo canônico
(`entities/ai/LumberjackWorkGoal.java`):

```text
canUse()  ─── não dorme? é do dono? não precisa de baú? área existe?
   │
   ▼
SELECT_WORK_AREA      varre LumberArea num raio de 64, pontua e ordena
   │                  (getAvailableWorkAreasByPriority, linha 521)
   ▼
MOVE_TO_WORK_AREA     navigation.moveTo até 10 blocos
   ▼
PREPARE_BONE_MEAL     area.scanBoneMealArea() → Stack<BlockPos>
   ▼  BONE_MEAL       aplica bone meal em cada muda
   ▼
SCAN_TREES            area.scanForTrees() → Stack<Tree>, flood fill
   ▼                  ordena por distância ao trabalhador
SELECT_TREE           pop() da pilha; tree.setInWork(true)
   ▼
MOVE_TO_TREE          até 30 blocos
   ▼
PREPARE_SHEAR_LEAVES  troca item na mão; se faltar, addNeededItem e para
   ▼  SHEAR_LEAVES
PREPARE_STRIP_LOGS    idem, com machado
   ▼  STRIP_WOOD
PREPARE_WOOD_CUTTING  idem
   ▼
WOOD_CUTTING          mineBlock() um bloco por vez, com progresso visual
   ▼                  (roda TODO tick, fora do gate de %10)
PREPARE_PLANT_SAPLINGS / PLANT_SAPLINGS
   ▼
DONE                  libera a área, volta followState a 0, e chama
                      this.start() — reinicia o ciclo
```

**Dois detalhes de projeto que valem registrar:**

1. `tick()` roda o gate `if(lumberjack.tickCount % 10 != 0) return;`
   *depois* de tratar `WOOD_CUTTING` (linhas 81-88). A quebra de bloco
   precisa de todo tick para a barra de progresso; o resto da máquina
   roda a 2 Hz. É a mesma separação de dois relógios que o seu
   `LumberjackWork` documenta ("run" no ciclo longo, "tick" no tick).

2. `DONE` chama `this.start()`. O Goal nunca termina — ele se reinicia. A
   parada real vem do `canUse()` falhar (noite, comando do dono, baú
   cheio). É simples e funciona, mas significa que **não há estado de
   repouso**: o trabalhador está sempre a um `SELECT_WORK_AREA` de
   distância de varrer o mundo de novo.

---

## 4. Quem manda em quem — o `followState`

Um `int` herdado do Recruits, e é o árbitro de toda a concorrência entre
Goals:

```text
0  wandering   → aceita trabalho
1  hold        → bloqueia AbstractChestGoal (AbstractChestGoal:39-41)
6  working     → em trabalho
```

Cada Goal escreve nele com cuidado, e os comentários mostram que isso
custou correção:

```java
// entities/ai/LumberjackWorkGoal.java:66-68
// Only claim the working state when idle. If the owner issued a command
// (follow/hold/...), the state is no longer 0 and must not be overridden.
if(lumberjack.getFollowState() == 0) lumberjack.setFollowState(6);
```

```java
// entities/ai/LumberjackWorkGoal.java:273-275
// Only fall back to wander if we are still in the working state.
// If the owner changed the follow state mid-cycle, keep their command.
if(lumberjack.getFollowState() == 6) lumberjack.setFollowState(0);
```

**Este é um dos achados mais úteis da análise.** É um mutex global de um
inteiro, feito à mão, e cada uso dele precisou aprender a não pisar no
comando do jogador. Um projeto que tenha um objeto `Task` com estado
explícito — como o seu — não paga esse preço.

---

## 5. Registro e inicialização

`WorkersMain.java` — Forge padrão. `DeferredRegister` para blocos, itens,
entidades, menus, POIs, profissões e sons. Nada de notável, com uma
exceção:

```java
// WorkersMain.java:76-79
isDynamicTreesInstalled = modList.isLoaded("dynamictrees");
isFarmersDelightInstalled = modList.isLoaded("farmersdelight");
```

Duas flags estáticas booleanas, consultadas em todo lugar
(`LumberjackWorkGoal:437`, `LumberArea:122,137`). É a forma mais barata
de compat opcional que existe, e funciona porque `compat/DynamicTrees` e
`compat/FarmersDelight` só são tocadas atrás da flag. Padrão simples e
legítimo.

---

## 6. O que a arquitetura NÃO tem

Registrado porque a ausência é informação:

```text
sem camada de domínio         BlockState e Level aparecem em todo lugar
sem serviço de colônia        não existe entidade "vila" ou "assentamento"
sem fila de tarefas           ver 04-task-system.md
sem persistência global       nenhum SavedData; tudo é NBT de entidade
sem teste                     zero
sem injeção de dependência    tudo por campo público e acesso direto
sem interface entre camadas   Goal chama area.scanX() e mexe no Stack dela
```

O acoplamento Goal↔Area é total: `LumberjackWorkGoal` lê e **modifica**
`lumberjack.currentLumberArea.stackOfTrees` diretamente
(`LumberjackWorkGoal:149,166`). A área é, na prática, o *scratchpad* do
Goal. Isso é rápido de escrever e impossível de testar.
