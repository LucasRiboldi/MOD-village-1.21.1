# 07 — Work Areas

O conceito que define o Workers. Onze classes, ~3.200 linhas, e o
mecanismo por onde o jogador diz ao mod o que fazer e onde.

---

## 1. O que é uma Work Area

Uma **`Entity`** — não um `BlockEntity`, não um bloco — invisível,
invulnerável, sem gravidade, sem colisão, que o jogador coloca com um item
e que carrega:

```java
// AbstractWorkAreaEntity.java:40-52
PLAYER_UUID, PLAYER_NAME     dono
TEAM_STRING_ID, TEAM_ACCESS  time e permissão
WIDTH, DEPTH, HEIGHT         dimensões
FACING                       orientação
isDone, isBeingWorkedOn      estado de trabalho
time                         idade desde o último atendimento
```

Todos como `EntityDataAccessor` (`SynchedEntityData`) — ou seja,
**sincronizados automaticamente com o cliente**, que é o que faz as GUIs
e o render da caixa funcionarem sem uma linha de código de rede para os
campos.

### Por que `Entity` e não `BlockEntity`

Inferência a partir do código, não afirmação do autor:

* a caixa é 3D e orientada; um bloco teria de guardar tudo em NBT e
  sincronizar à mão;
* `level.getEntitiesOfClass(LumberArea.class, box.inflate(64))` é uma
  consulta de uma linha — a busca espacial vem de graça;
* `SynchedEntityData` resolve a sincronização cliente-servidor;
* entidades salvam com o chunk, sem `SavedData`.

O custo: entidades **ticam**. Onze tipos de área, uma por lugar de
trabalho, todas rodando `tick()` a 20 Hz — mesmo que o corpo seja só
`if(tickCount % 20 == 0) time++`. E entidade some com o chunk, o que
gerou o mecanismo de busca por UUID em raio (`§6` abaixo).

---

## 2. A caixa: `createArea()`

```java
// AbstractWorkAreaEntity.java:215-230
public AABB createArea() {
    Direction facing = getFacing();
    int width = getWidthSize() - 1;
    int depth = getDepthSize() - 1;
    BlockPos start = this.getOnPos();
    BlockPos end = switch (facing) {
        case NORTH -> start.offset(width, height, -depth);
        case SOUTH -> start.offset(-width, height, depth);
        case EAST  -> start.offset(depth, height, width);
        default    -> start.offset(-depth, height, -width);   // WEST
    };
    return new AABB(start, end);
}
```

E a `MiningArea` sobrescreve com uma versão melhor, cujo comentário conta
a correção:

```java
// MiningArea.java:104-106
// Unified, facing-relative axes for BOTH custom and stairs so switching mode
// never rotates the area: width (length) runs along the facing direction and
// depth (breadth) runs to the right of it.
int fwdX, fwdZ, sideX, sideZ;
```

Trocar o modo de mineração **girava a área** no chão do jogador. A
correção foi parar de escrever quatro `case` de offsets e passar a
calcular vetores unitários "frente" e "lado" a partir da direção. É a
mesma matemática, escrita de um jeito que não pode discordar de si mesma.

**Lição transferível, e ela toca a sua TASK-046 (E8, orientação dos
blocos):** quando quatro ramos de `switch` sobre `Direction` produzem a
mesma geometria de quatro jeitos, o código vai discordar de si mesmo em
algum deles. Vetor unitário por direção, calculado uma vez, elimina a
classe inteira de erro.

---

## 3. O ciclo do briefing, mapeado

```text
Entrada       jogador coloca o item → MessageAddWorkArea → entidade nasce
                ↓
Detecção      Goal faz level.getEntitiesOfClass(XArea.class, box.inflate(64))
                ↓
Validação     area.canWorkHere(worker)   — dono ou time
              area.isWorkerPerfectCandidate(worker) — tem as ferramentas?
              !area.isRemoved()
                ↓
Reserva       area.setBeingWorkedOn(true)   ← booleano, sem dono, frouxa
              area.setTime(0)               ← zera o envelhecimento
                ↓
Execução      area.scanXArea()              ← a ÁREA varre o mundo
              → preenche Stack<BlockPos> pública na própria área
              o Goal consome a Stack com pop()
                ↓
Resultado     drops caem, aiStep() recolhe num raio de 5,5
                ↓
Armazenamento farmedItems > 128 → DepositItemsToStorage → StorageArea
```

**A inversão importante:** quem varre o mundo é a **área**, não o
trabalhador. `LumberArea.scanForTrees()`, `CropArea.scanBreakArea()`,
`MiningArea.scanFloorArea()`, `HomeArea.scanRoomQuality()`. O Goal só
pede e consome.

Isso é sensato — a área é o objeto que conhece os limites — e é levado
longe demais: a área guarda a `Stack` de trabalho **pública e mutável**, e
o Goal a modifica (`stackOfTrees.pop()`, `stackToPlace` reordenada). A
área deixou de ser um lugar e virou um bloco de rascunho compartilhado.

---

## 4. As onze áreas, e o que cada uma varre

| Área | Varre | Guarda |
|---|---|---|
| `LumberArea` | árvores por flood fill 6-direções, com validação de folha viva | `Stack<Tree>`, `stackToPlant`, `stackToBoneMeal` |
| `CropArea` | plantar / arar / colher / bone meal / colher-sem-quebrar | 5 stacks |
| `MiningArea` | chão, paredes com minério, escada asc/desc | `stackToBreak`, `stackToFill` |
| `BuildArea` | blocos a colocar, a quebrar, área a limpar, multiblocos | 4 stacks |
| `StorageArea` | baús acessíveis | `Map<BlockPos, Container>` |
| `HomeArea` | qualidade do cômodo (5 bits), cama | `assignedBedPos`, `chestPos` |
| `KitchenArea` | fornos e contêineres | contagens sincronizadas |
| `MarketArea` | contêineres do mercado | idem |
| `AnimalPenArea` | animais para abate / cria / tosa | listas de `Animal` |
| `FishingArea` | (só valida água) | — |
| `FishingArea`/`AnimalPen` | — | — |

### O flood fill de árvore — comparação direta com o seu `TreeScanner`

```java
// LumberArea.java:262-303
private void scanTree(Level level, BlockPos start, Set<BlockPos> visited, Tree tree) {
    Queue<BlockPos> toVisit = new ArrayDeque<>();
    toVisit.add(start);
    while (!toVisit.isEmpty()) {
        BlockPos pos = toVisit.poll();
        if (!visited.add(pos)) continue;
        if (isLog(level.getBlockState(pos))) {
            tree.addToBreak(pos);
            // folhas num cubo 8×8×8 ao redor de CADA tronco
            for(int x = -4; x < 4; x++)
             for(int y = -4; y < 4; y++)
              for(int z = -4; z < 4; z++) { ... }
            for (Direction dir : Direction.values()) toVisit.add(pos.relative(dir));
        }
    }
}
```

**Custo:** 512 `getBlockState` por tronco. Uma árvore de 20 troncos =
10.240 consultas de bloco, sem cache, com `Stack.contains()` (busca
linear) dentro do laço mais interno. E `scanForTrees` roda isso para
**toda** a área, dentro de um único tick do servidor.

Isso é um gargalo de verdade. É a mesma família do defeito que o seu §11
registra — `World.getBlockEntity` chamado de dentro do evento de chunk, a
thread do servidor travada. Aqui não força chunk, mas é O(troncos × 512)
num tick.

**A validação de folha viva, porém, é excelente:**

```java
// LumberArea.java:232-250
private boolean hasNaturalLeavesConnected(Tree tree, Level level) {
    for (BlockPos logPos : tree.getStackToBreak())
        for (BlockPos offset : BlockPos.betweenClosed(logPos.offset(-4,-4,-4), logPos.offset(4,4,4))) {
            BlockState state = level.getBlockState(offset);
            if (isLeaf(state)
                && !state.getOptionalValue(BlockStateProperties.PERSISTENT).orElse(false)
                && state.getOptionalValue(BlockStateProperties.DISTANCE).orElse(7) < 7)
                return true;
        }
    return false;
}
```

Uma coluna de troncos **sem copa viva não é árvore, é construção**. O
teste é duplo: a folha não pode ser `PERSISTENT` (colocada pelo jogador) e
precisa ter `DISTANCE < 7` (ligada a tronco).

**Você tomou exatamente esta decisão**, em 2026-08-12: "tronco sem copa
viva não é árvore, é construção" (`Project-State.md §10`). Duas
implementações independentes chegando à mesma regra pelo mesmo motivo — a
casa de madeira do jogador sendo derrubada. O detalhe que vale conferir no
seu `TreeScanner` é se você checa **as duas** propriedades ou só a
conectividade: `PERSISTENT` é o que separa folha natural de folha
decorativa colocada à mão.

### `HomeArea.scanRoomQuality` — a bitmask de qualidade

```java
// HomeArea.java:243-305
// bit 0 = paredes/teto fechados   bit 1 = porta      bit 2 = luz
// bit 3 = baú                     bit 4 = cama
public boolean canMoveIn() {
    return hasWalls() && hasDoor() && hasLight() && hasBed() && hasChest();
}
public int getQualityScore() { return Integer.bitCount(ROOM_QUALITY); }
```

E `checkEnclosed()` (linha 308) é um **BFS de fora para dentro**: semeia
todos os blocos de ar da borda de uma caixa inflada em 4, propaga por ar,
e se o ar externo alcança o interior, o cômodo tem buraco. É a maneira
correta de perguntar "isto é um cômodo fechado?" — e é a mesma técnica
que o Vanilla usa para portal/vila, feita à mão.

**Isto é altamente reaproveitável para você**, e por uma razão concreta:
a sua Fase 11 registra a casa construída como infraestrutura, e a Regra 6
manda a vila crescer. Uma métrica de "esta casa está habitável" — fechada,
com porta, luz, cama e baú — é exatamente o que decide se a construção
*valeu*. Cinco bits, uma varredura, e um score comparável.

O custo: `BlockPos.betweenClosedStream(area.inflate(1))` mais um BFS sobre
`area.inflate(4)`. Para um cômodo 7×5×4 isso é ~1.500 posições. Aceitável
**se** rodar quando alguém chega em casa, e não por tick. É o que ele faz
(`WorkerGoHomeGoal:223`).

---

## 5. Extensibilidade — o custo real de uma profissão nova

A pergunta do briefing §9: *existe arquitetura extensível que permita
adicionar profissões sem modificar excessivamente o núcleo?*

**Resposta: não.** Contagem do que uma profissão nova exige:

```text
 1. XEntity extends AbstractWorkerEntity          classe nova
 2. XWorkGoal extends Goal                        classe nova (300-700 linhas)
 3. XArea extends AbstractWorkAreaEntity          classe nova
 4. XAreaScreen extends Screen                    classe nova (200-500 linhas)
 5. MessageUpdateXArea                            classe nova
 6. registrar a mensagem em WorkersMain           EDITA o núcleo
 7. ModEntityTypes.X + ModEntityTypes.XAREA       EDITA o núcleo
 8. ModItems: ovo de spawn + item da área         EDITA o núcleo
 9. WorkAreaTypes.X(índice N)                     EDITA o núcleo (enum)
10. VillagerEvents: TITLE_X, DESCRIPTION_X        EDITA o núcleo
11. VillagerEvents.registerWorkerTrades           EDITA o núcleo
12. WorkersServerConfig.XCost                     EDITA o núcleo
13. render, layer, texturas, lang                 EDITA o núcleo
```

**Cinco classes novas e oito arquivos do núcleo editados.** O `enum
WorkAreaTypes` com índices numéricos manuais (`CROPAREA(0) ... KITCHEN(9)`)
é o pior deles: um índice serializado que não pode ser reordenado sem
quebrar saves.

Não há registro, não há factory, não há interface de profissão, e não há
API para outro mod. **É um sistema fechado, projetado para nove profissões
escritas pelo mesmo autor.**

### O contraste com o seu

Você já tem `ProfessionRegistry` + `ProfessionType` + `Capability` +
`ToolType` no Core, e `TaskType.required()` declarando a capacidade. Uma
profissão nova no seu modelo é:

```text
1. entrada em ProfessionType / ProfessionRegistry     dado
2. um XWork em fabric/work                            classe nova
3. talvez um TaskType novo                            dado
```

**A sua arquitetura já é estritamente mais extensível.** A análise
confirma o valor da separação `Capability` ↔ `Profession` que a emenda da
ADR-006 §6 criou — é exatamente a indireção que o Workers não tem.

O que falta no seu, e o Workers tem: um lugar por profissão para
**parâmetros** (raio, teto, ferramentas exigidas). Hoje isso está
espalhado em constantes dentro de `LumberjackWork`
(`SEARCH_RADIUS = 64`). Ver `12 §3.4`.

---

## 6. Referenciar área por UUID — e o problema que isso cria

```java
// WorkerGoHomeGoal.java:247-254
private HomeArea findHomeAreaByUUID(ServerLevel level, UUID uuid) {
    return level.getEntitiesOfClass(HomeArea.class, worker.getBoundingBox().inflate(256))
            .stream()
            .filter(a -> uuid.equals(a.getUUID()))
            .findFirst()
            .orElse(null);
}
```

Guarda-se o `UUID` da área e, para resolvê-lo, **varre todas as entidades
daquele tipo num cubo de 512 blocos de lado** e filtra. Roda em
`tickSelectHome`, `tickMoveToHome`, `tickGoToBed`, `tickSleep` e
`releaseHomeArea`.

Existe `ServerLevel.getEntity(UUID)` na 1.20.1, que é O(1). O código não o
usa. Isso é um custo desnecessário e repetido, e é **consequência direta
da escolha de modelar a área como entidade**: a referência forte não pode
ser guardada (a entidade morre com o chunk), então guarda-se o id, e
resolver o id vira uma busca.

**Nota para o seu projeto:** você tem o problema simétrico e resolvido de
outro jeito — `ColonySavedData` guarda dados por UUID num registro global,
e `ColonyPos` é um valor, não uma referência ao mundo. A ADR-005 pagou
por si aqui.

---

## 7. Permissão e propriedade

```java
// AbstractWorkAreaEntity.java:186-189
public boolean canWorkHere(AbstractWorkerEntity worker) {
    return worker.isOwned()
        && (worker.getOwnerUUID().equals(this.getPlayerUUID())
            || getTeamAccess() && worker.getTeam() != null
               && this.getTeamStringID().equals(worker.getTeam().getName()));
}

// linhas 173-179
public boolean canPlayerSee(Player player) {
    boolean owner    = player.getUUID().equals(this.getPlayerUUID());
    boolean sameTeam = player.getTeam() != null && ...;
    boolean admin    = player.isCreative() && player.hasPermissions(2);
    return admin || owner || sameTeam;
}
```

Três níveis: dono, time (vanilla scoreboard team), admin. E a destruição
exige criativo + agachado + permissão 2 (linha 133-141) — uma área não
pode ser destruída por acidente.

Há ainda integração com claims de facção via
`ShouldWorkAreaOnlyBeInFactionClaim` no config.

**Não se aplica ao Village Colony.** No seu modelo a colônia não tem dono
— é a vila. Registrado para completude, e porque é a evidência mais clara
de que os dois mods respondem a perguntas diferentes.

---

## 8. Sobreposição de áreas

```java
// AbstractWorkAreaEntity.java:243-251
public static boolean isAreaOverlapping(Level level, AbstractWorkAreaEntity current, AABB target) {
    if (current instanceof BuildArea) return false;
    for (AbstractWorkAreaEntity other : level.getEntitiesOfClass(..., target.inflate(64))) {
        if (other == current || other instanceof BuildArea) continue;
        if (other.getArea().intersects(target)) return true;
    }
    return false;
}
```

Áreas de trabalho não podem se sobrepor — **exceto** as de construção, que
podem sobrepor qualquer coisa, inclusive umas às outras. Faz sentido: uma
obra acontece *em cima* de onde as coisas estão.

**Isto é diretamente relevante para a sua Fase 11.** O seu
`BuildSiteScanner` escolhe lote e a `BuildingRegistry` registra a
construção, e o §7 do `Project-State.md` diz que o teste é "o lote
seguinte não caindo em cima dela". A regra do Workers é: obra ignora
sobreposição na hora de nascer, mas o que ela **produz** ocupa. Vale
verificar que a sua exclusão de lote é contra a *construção registrada*, e
não contra a *obra em andamento* — são coisas diferentes, e confundi-las
faz o construtor recusar o próprio canteiro.
