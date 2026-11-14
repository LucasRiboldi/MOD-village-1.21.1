# 08 — Persistência

---

## 1. A resposta curta

**O Workers não tem persistência própria.** Verificado por grep em todo
`src/main/java`:

```text
SavedData              0 ocorrências
DimensionDataStorage   0 ocorrências
getDataStorage         0 ocorrências
Capability (Forge)     0 ocorrências
```

Tudo o que persiste é **NBT de entidade**, salvo pelo Minecraft junto com
o chunk. Nada mais.

---

## 2. O que o trabalhador guarda

```java
// AbstractWorkerEntity.java:245-259
public void addAdditionalSaveData(CompoundTag nbt) {
    super.addAdditionalSaveData(nbt);          // ← Recruits: dono, inventário,
                                               //   moral, followState, pagamento
    nbt.putInt("farmedItems", farmedItems);
    if(lastStorage  != null) nbt.putUUID("lastStorage",  lastStorage);
    if(homeAreaUUID != null) nbt.putUUID("homeAreaUUID", homeAreaUUID);
}
```

**Três campos.** Todo o resto do estado de um trabalhador — qual área ele
está atendendo, em que estado da máquina está, que pilha de blocos estava
consumindo, qual árvore estava derrubando, o que ele precisa
(`neededItems`) — **não é salvo**.

```java
// não persistem:
public State state;                 // no Goal
public BlockPos blockPos;           // alvo do momento
public Stack<Tree> stackOfTrees;    // trabalho pendente
public Tree currentTree;
public List<NeededItem> neededItems;
public XArea currentXArea;          // referência forte
```

---

## 3. O que a área guarda

```java
// AbstractWorkAreaEntity.java:89-104
playerUUID, playerName, isDone, isBeingWorkedOn,
width, height, depth, facing, teamStringID, time, teamAccess
```

Mais o específico de cada uma: `SEED_STACK` na `CropArea`,
`SAPLING_STACK`/`REPLANT`/`SHEAR_LEAVES`/`STRIP_LOGS` na `LumberArea`,
`STRUCTURE` (um `CompoundTag` inteiro!) na `BuildArea`, `RESIDENT_NAME` e
`ROOM_QUALITY` na `HomeArea`.

**As `Stack` de trabalho não são salvas em nenhuma delas.** São
reconstruídas pelo `scanXArea()` correspondente.

E `isBeingWorkedOn` **é** salvo (linha 94). Isso é um defeito: se o
servidor cair com um trabalhador atendendo a área, a área volta do save
marcada como ocupada, e o trabalhador que a atendia perdeu a referência
(`currentXArea` não persiste). A área fica reservada para ninguém.

O único remédio é o watchdog de distância
(`AbstractWorkerEntity:119-123`), e ele só dispara se **algum**
trabalhador tiver aquela área como `getCurrentWorkArea()` — o que, depois
do reload, nenhum tem. **A reserva órfã pós-reload não tem quem a
limpe.**

---

## 4. O que acontece em cada evento

| Evento | Consequência no Workers |
|---|---|
| mundo salva | NBT de entidade escrito com o chunk |
| jogo fecha | idem |
| chunk descarrega | trabalhador e áreas **somem da memória**. `currentXArea` vira referência morta; `storageMap` guarda `Container` inválido |
| chunk recarrega | tudo é reconstruído: `scanForTrees`, `scanStorageBlocks`, `scanRoomQuality`. O Goal recomeça em `SELECT_WORK_AREA` |
| servidor reinicia | idem, mais `isBeingWorkedOn` possivelmente órfão |
| trabalhador morre | `die()` libera área e casa (linhas 544-550); inventário cai no chão |
| trabalhador recriado | não há recriação. Morreu, acabou. O jogador compra outro |

### O chunk loading

`AbstractWorkerEntity extends AbstractChunkLoaderEntity` — **o
trabalhador carrega o próprio chunk**, e a implementação está no Recruits,
fora deste repositório.

Isso resolve o problema de raiz: o trabalhador continua existindo e
trabalhando longe do jogador. E é caro — cada trabalhador é um
force-load.

**A sua ADR-002 tomou a decisão oposta**, e o `ColonyLifecycle`
ACTIVE/DORMANT é o mecanismo. A colônia adormece quando os chunks saem;
nada é simulado sem o jogador. É a decisão certa para um mod de vila
(que pode ter muitas colônias) e a errada para um mod de peões (onde o
jogador quer que a mina continue funcionando enquanto ele está longe).

**Registro honesto:** a abordagem do Workers é mais divertida para o
jogador e muito mais cara. A sua é sustentável e implica que a colônia
não progride sem alguém por perto. Ambas são defensáveis; a sua está
alinhada com a ADR-001 e com `Performance-Rules.md`.

---

## 5. Estado global em `static`

Dois lugares, e ambos são bombas de multiplayer:

```java
// world/VillagerInviteRegistry.java:10
private static final Map<UUID, UUID> INVITES = new ConcurrentHashMap<>();
```

Estático, sem dimensão, sem servidor, **sem persistência**. Um convite de
mercador a aldeão desaparece no reload. Há um `clear()` — chamado no
`ServerStoppingEvent` (`VillagerEvents`), o que ao menos evita vazamento
entre mundos no cliente integrado.

```java
// WorkersMain.java:47-48
public static boolean isDynamicTreesInstalled;
public static boolean isFarmersDelightInstalled;
```

Essas duas são legítimas — a lista de mods não muda em execução.

**Nota para o seu §9:** você registra o mesmo tipo de suposição —
"`COLONIES` e `WORKERS` são estáticos e não separam dimensão". A
diferença é que os seus **são persistidos** (`ColonySavedData`) e o
`INVITES` não é. A sua dívida é de tipo; a do Workers é de dados
perdidos.

---

## 6. Persistência em disco: `StructureManager`

O único lugar que escreve arquivo:

```java
// world/StructureManager.java:98-127
File dir = new File(Minecraft.getInstance().gameDirectory, "workers/scan");
NbtIo.writeCompressed(root, file);
...
Path base = Path.of(Minecraft.getInstance().gameDirectory..., "workers", "scan");
```

O jogador seleciona uma região com uma `BuildArea`, escaneia, e a
estrutura vira um `.nbt` em `workers/scan/`. Depois pode mandar um
construtor erguê-la em outro lugar.

**Dois problemas graves:**

1. **`Minecraft.getInstance()` em código de mundo.** É classe de cliente.
   Num servidor dedicado isso é `NoClassDefFoundError`. Existe um caminho
   de servidor (`WorkersMain.onServerStarting` cria
   `serverDirectory/workers/scan/factions`), mas o `StructureManager`
   usa o do cliente. Não foi possível confirmar se `saveStructureToFile`
   só roda no cliente sem ler todos os chamadores — mas a assimetria
   entre os dois caminhos é evidente.

2. **`copyDefaultStructuresIfMissing` copia recursos do jar para o disco
   do jogador.** Funciona, e é a maneira que o mod tem de entregar
   presets editáveis. Custa: os arquivos ficam lá para sempre, e uma
   atualização do mod não os atualiza (`if (!Files.exists(destFile))`).

**Comparação:** o seu `StructureBlueprintReader` lê estrutura Vanilla do
próprio jogo, sem escrever nada em disco. É mais simples, mais alinhado
com a ADR-001 ("usar estruturas existentes como referência") e não tem
nenhum destes dois problemas. **Sua abordagem é melhor.**

O que o Workers tem e você não: o jogador pode **desenhar a própria
casa** e mandar construí-la. É uma funcionalidade, não uma arquitetura, e
está catalogada em `12 §5` como *COULD HAVE*.

---

## 7. Comparação com o Village Colony

| | Village Colony | Workers |
|---|---|---|
| Mecanismo | `ColonySavedData` (`PersistentState`) | NBT de entidade |
| Escopo | registro global por servidor | por entidade, por chunk |
| O que persiste | colônias, trabalhadores, baús, obras, construções | 3 campos + dados de área |
| Estado de trabalho | tarefa **não** persiste (decisão registrada) | não persiste (por omissão) |
| Progresso de obra | não persiste — **o mundo é a verdade** | não persiste |
| Sobrevive a chunk unload | sim | não |
| Sobrevive a restart | sim | parcialmente |
| Testado | `ColonySavedDataTest`, 263 linhas | nenhum |

**A decisão mais alinhada dos dois projetos, e cada um chegou nela
sozinho:** o progresso de uma obra não é gravado; ao retomar, cada bloco
cujo lugar já tem o bloco certo sai da lista.

Você escreveu isso em `Project-State.md §9`:

> quem sabe o que já está de pé é o mundo. Ao retomar, cada bloco do
> projeto cujo lugar já contém o bloco certo sai da lista — e a parede que
> o jogador derrubou entre sessões volta a ser pedida, que é a resposta
> certa.

O Workers faz o mesmo por `scanBreakArea()`/`statesMatch()`
(`BuildArea:317-326`), sem ter escrito por quê. **É a mesma regra,
alcançada por duas rotas.** Isso é a confirmação mais forte que a análise
produziu sobre uma decisão sua.

---

## 8. O que trazer

### Nível A

1. **Nada.** O modelo de persistência do Workers é o mínimo possível, e
   você já tem um estritamente melhor.

### Aprendizados (não código)

2. **Reserva persistida sem quem a libere é uma armadilha.**
   `isBeingWorkedOn` vai para o NBT e ninguém o limpa no carregamento.
   → **Verificar no seu:** `ColonySavedData` grava obra e construção. Se
   alguma delas guardar "trabalhador X está nisto" e o trabalhador não
   voltar, quem limpa? Você já registrou que "a marca de qual trabalhador
   estava construindo" fica fora do save — o que resolve isto por
   omissão. Vale garantir que continue assim.

3. **Estado global `static` sem persistência perde dados em silêncio.**
   O `INVITES` é o exemplo. Sua dívida análoga (registro único, sem
   dimensão) está registrada e é menor.

4. **O mundo como fonte de verdade do progresso** — confirmado por
   convergência.

5. **Não escrever em `getPersistentData()` de block entity Vanilla.**
   O `isOpened` do baú (`AbstractChestGoal:88`) fica no save do jogador
   para sempre, mesmo depois de o mod ser removido. Se você precisar
   marcar um baú, marque no seu próprio registro — que é o que o
   `StorageRegistry` já faz.
