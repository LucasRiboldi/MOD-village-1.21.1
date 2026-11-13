# 06 — Inventário e logística

O subsistema mais maduro do Workers, e o que tem mais a oferecer ao
Village Colony. Aqui os dois projetos resolvem o **mesmo** problema.

---

## 1. O inventário do trabalhador

`SimpleContainer` herdado do Recruits. A convenção que importa:

```java
// AbstractWorkerEntity.java:131, 198, 210, 214, 526, 620
for(int i = 6; i < inventory.getContainerSize(); i++)
```

**Os slots 0-5 são reservados** — equipamento, armadura, comida. O
trabalho só toca de 6 em diante. O número aparece cru em seis lugares
diferentes, sem constante nomeada. Convenção correta, execução ruim.

O que a base oferece:

```java
canAddItem(ItemStack)                  cabe?
addItem(ItemStack)                     empilha primeiro, depois slot vazio
hasFreeInvSlot()
getMatchingItem(Predicate<ItemStack>)
countMatchingItems(Predicate)
countMatchingStacks(Predicate)
switchMainHandItem(Predicate)          troca o que está na mão
wantsToKeep(ItemStack)                 comida com nutrição > 4 nunca sai
```

`switchMainHandItem` (linha 519) é uma peça pequena e boa: troca o item da
mão principal pelo primeiro do inventário que casa com o predicado, e o
antigo vai para o slot de onde saiu o novo. É o que faz o lenhador pegar
machado, tesoura e osso conforme a etapa.

**No Village Colony isso não tem equivalente e não precisa ter.** O
aldeão Vanilla não tem inventário de trabalho útil (8 slots, usados pela
IA de fazendeiro) e o item na mão **não é renderizado** — você já
registrou isso em `Project-State.md §9` sobre o `WorkerEquipment`. O
inventário do seu trabalhador é o baú dele, e essa é a decisão mais
alinhada com a ADR-001 ("o mundo é a fonte de verdade").

---

## 2. `NeededItem` — o mecanismo central

`world/NeededItem.java`, 128 linhas. É a peça mais reaproveitável do mod
inteiro, em conceito.

```java
public class NeededItem {
    public final Predicate<ItemStack> matcher;   // o que serve
    public int count;                            // quanto
    public final boolean required;               // bloqueia o trabalho?
    @Nullable public final Object sourceKey;     // quem pediu
    @Nullable private Object cachedMatchKey;     // identidade do pedido
}
```

### 2.1 O ciclo de vida

```text
Goal de profissão descobre que falta X
        │
        ▼
worker.addNeededItem(new NeededItem(pred, n, required, areaUUID))
        │  ← funde com pedido igual da MESMA fonte (max, não soma)
        ▼
worker.needsToGetItems()  vira true   (algum required == true)
        │
        ▼
GetNeededItemsFromStorage.canUse()    vira true
        │
        ▼
o trabalhador vai ao armazém e volta com o item
        │
        ▼
NeededItem.applyToNeededItems(stack, list)  abate a contagem;
                                            chega a zero → remove da lista
```

Este é o **mecanismo antideadlock** do Workers. O Goal que descobre a
falta não trava e não falha: ele **declara** a falta e devolve o controle.
Outro Goal, de prioridade maior, resolve.

### 2.2 O `required`

```java
// AbstractWorkerEntity.java:414-416
public boolean needsToGetItems() {
    return neededItems.stream().anyMatch(neededItem -> neededItem.required);
}
```

Dois níveis de pedido:

* `required = true` → **bloqueia**: machado, picareta, tesoura. Sem isso o
  trabalho não anda.
* `required = false` → **oportunista**: osso de farinha para a muda. Se
  passar pelo baú, pega; se não, tanto faz.

Só o `required` dispara a viagem. Distinção simples e certa.

E há um detalhe fino: em `LumberjackWorkGoal`, o osso vai para uma lista
**local** do Goal (`this.neededItems.add`, linha 129) e só é transferido
ao trabalhador no estado `DONE` (linhas 279-284). O machado vai
**direto** para o trabalhador (`lumberjack.addNeededItem`, linha 214),
com um comentário explicando que ir pela lista local travava o Goal no
estado de preparo. É a correção do deadlock, e a diferença de urgência
está codificada em *para onde* o pedido vai.

### 2.3 O problema do `Predicate` como identidade

```java
// NeededItem.java:28-32
// Lambdas can't be reliably compared, so we use this derived key for
// equality checks instead.

// linhas 87-96
private Item tryExtractSingleItemFromMatcher() {
    for (Item item : BuiltInRegistries.ITEM) {           // ← varre o registro
        if (matcher.test(new ItemStack(item))) return item;
    }
    return null;
}
```

Para saber se dois pedidos são o mesmo, ele **varre o registro de itens
inteiro** procurando o primeiro que o predicado aceita. Cacheado, roda uma
vez por `NeededItem`. Mas o registro tem milhares de itens em modpack, e
`new ItemStack(item)` aloca em cada iteração.

É engenhoso e é errado. **A causa raiz é a escolha de `Predicate` como
tipo do pedido.** Um pedido deveria ser um dado (item ou tag + contagem),
não uma função. Predicado é conveniente na escrita e impede comparar,
serializar e logar.

**Isto é uma lição direta para o seu projeto.** O seu `ResourceId` /
`ResourceType` / `ResourceGroup` já são dados — comparáveis, serializáveis
e independentes do Minecraft (ADR-005). O `MinecraftTypeAdapter` faz a
tradução na fronteira. **Você já está do lado certo desta escolha; a
análise só confirma o custo do lado errado.**

### 2.4 A fusão por fonte

```java
// AbstractWorkerEntity.java:495-506
// Same item + same source -> merge by taking the higher count instead of
// appending a duplicate. Different sources stay separate so e.g. two
// crop fields each asking for 8 bone meal accumulate to 16.
```

Pedido igual da mesma fonte = `max`, não soma (senão um Goal que pede a
cada tick acumularia para sempre). Fontes diferentes ficam separadas
(senão dois campos pedindo 8 viram 8, e não 16).

Regra pequena, aprendida na prática, e correta.

---

## 3. Armazenamento: `StorageArea`

```java
// entities/workarea/StorageArea.java:58-75
public void scanStorageBlocks(){
    storageMap.clear();
    BlockPos.betweenClosedStream(area).forEach(pos -> {
        BlockState stateAbove = level.getBlockState(pos.above());
        if(stateAbove.isAir()){                                  // ← acessível
            Container container = getContainer(pos);
            if(container != null && !isAlreadyMapped(storageMap, container)) {
                storageMap.put(pos.immutable(), container);
            }
        }
    });
}
```

Três decisões dentro de dez linhas:

1. **`stateAbove.isAir()`** — só conta baú que pode ser aberto. Baú com
   bloco em cima não entra no mapa.
2. **`isAlreadyMapped`** — o *double chest*. `ChestBlock.getContainer`
   devolve um `CompoundContainer` para as duas metades; sem essa
   verificação, o baú duplo entraria duas vezes e a contagem dobraria.
   ```java
   // AbstractWorkAreaEntity.java:253-260
   public static boolean isSameContainer(Container a, Container b) {
       if (a instanceof CompoundContainer ccA && b instanceof CompoundContainer ccB) {
           return (ccA.container1 == ccB.container1 && ccA.container2 == ccB.container2)
               || (ccA.container1 == ccB.container2 && ccA.container2 == ccB.container1);
       }
       return a == b;
   }
   ```
   Compara as duas ordens de par. **Detalhe que só se descobre em jogo.**
3. `Map<BlockPos, Container>` — guarda a **referência** ao container, não
   a posição. Isso é rápido e é perigoso: a referência vira lixo se o
   chunk descarregar. O código lida com isso checando `null` no uso
   (`DepositItemsToStorage:111-114`) e removendo a entrada. Remendo, não
   solução.

**Comparação com o seu `ChestScanner`:** você adota baú por **linha livre
desde a cama, no mesmo nível** (`Project-State.md §10`, decisão de
2026-08-08), e marca o baú adotado com um quadro
(`ChestMarker`). São critérios de propriedade diferentes — o Workers
delimita por caixa desenhada pelo jogador, você delimita por alcance e
visada a partir da cama.

**Mas o `stateAbove.isAir()` e o `isSameContainer` são ortogonais ao
critério de propriedade.** Vale conferir se o seu `ChestScanner` /
`ChestInventoryReader` tratam baú duplo sem dupla contagem, e se
consideram baú bloqueado por cima. São dois defeitos que não aparecem em
teste e aparecem no primeiro save real.

### O grande armazém

`StorageArea` implementa `Container` diretamente, com um TODO honesto:

```java
// StorageArea.java:142
//TODO: REMOVE ONCE RECRUITS HAS UPDATED OTHERWISE UPKEEP ON STORAGE DOES NOT WORK
```

Adaptador de compatibilidade para o mod-base. Dívida técnica declarada.

---

## 4. Depósito e retirada

Duas máquinas de estado quase simétricas, `DepositItemsToStorage` (281) e
`GetNeededItemsFromStorage` (330), sobre `AbstractChestGoal` (154).

### O que dispara o depósito

```java
// AbstractWorkerEntity.java:315-317
public boolean needsToDeposit() {
    return forcedDeposit || farmedItems > 128;
}
```

Um contador de itens colhidos, e um limiar de 128 (dois stacks). Simples,
e é a decisão que a sua **TASK-026 cancelou** em 2026-08-08 — porque no
seu mod a madeira vai direto para o baú, e o trabalhador nunca acumula.

**A sua decisão é melhor para o seu modelo.** Onde cada trabalhador tem
um baú próprio a poucos blocos, a viagem de retorno não paga. O Workers
precisa dela porque o armazém é um lugar distinto e distante.

### A busca do baú certo — o pré-scan

```java
// GetNeededItemsFromStorage.java:76-82
// Pre-scan: only target chests that actually contain a needed item, so the
// worker walks straight to the right chest instead of opening every chest.
for(Map.Entry<BlockPos, Container> entry : storageArea.storageMap.entrySet()){
    if(containerHasNeededItem(entry.getValue())){
        this.blockPosStack.push(entry.getKey());
    }
}
```

Lê **todos** os inventários em memória antes de andar, e só põe na fila os
baús que têm algo. Custo: uma varredura de inventários. Ganho: o
trabalhador não abre 20 baús com animação de 20 ticks cada.

Note o que isso é, em termos de projeto: **é onisciência sobre o
conteúdo, disfarçada de busca**. O trabalhador "sabe" onde está o item
antes de olhar. É uma troca deliberada de realismo por desempenho, e o seu
`ChestInventoryReader` faz a mesma coisa quando conta o estoque da
colônia inteira.

### A animação como parte do estado

```text
OPEN_CHEST  → interactChest(true), espera 40 ticks
DEPOSIT     → move os itens
CLOSE_CHEST → interactChest(false), espera 20 ticks
```

O baú abre de verdade, com som e animação (`blockEvent`), e o trabalhador
espera dois segundos. Isso não tem função mecânica nenhuma — é **só para
o jogador ver**. E é 30% do código dos dois Goals.

Vale registrar como *game feel*: o custo de fazer o trabalho parecer
trabalho. O seu P3 (§8 do `Project-State.md` — "o lado do cliente: nome,
rachadura e braço") é a mesma categoria de esforço.

### O bug do ordenamento

```java
// DepositItemsToStorage.java:94-95, e idêntico em GetNeededItems:100-101
blockPosStack.sort(Comparator.comparing(pos -> pos.getCenter().distanceToSqr(worker.position())));
blockPosStack.sort(Comparator.reverseOrder());     // ← desfaz o de cima
```

A segunda ordenação usa a ordem natural de `BlockPos` e **descarta**
completamente a ordenação por distância feita na linha anterior. O
trabalhador não vai ao baú mais próximo; vai ao de maior coordenada.

Provavelmente a intenção era `.reversed()` sobre o primeiro comparador,
para que o `pop()` (que tira do fim) pegasse o mais próximo. É um bug
real, presente nos dois Goals. **Bom exemplo de por que ordenar uma
`Stack` que se consome por `pop()` é armadilha.**

---

## 5. O Courier — a rede logística

`CourierEntity` + `CourierWorkGoal` (619) + `CourierRoute` +
`CourierAction`. É o sistema mais elaborado do mod, e as notas de release
mostram que foi o mais problemático (3 das 4 versões o citam).

### O modelo

```text
CourierRoute (envolve uma RecruitsRoute por UUID)
  └── List<CourierWaypoint>
        ├── BlockPos position
        ├── String displayName
        └── List<CourierAction>   (máximo 8)
              ├── ActionType
              ├── ItemStack template (o filtro)
              ├── SourceType: CHEST | STORAGE | MARKET | KITCHEN
              └── count / time
```

### As nove ações

```java
// world/CourierAction.java:10-22
TAKE       tirar até N de um item específico
PUT        pôr até N de um item específico
TAKE_ANY   tirar TODOS de um tipo, sem teto
PUT_ANY    pôr todos os que o courier carrega
TAKE_ALL   tirar tudo até encher o inventário
PUT_ALL    esvaziar o inventário inteiro no destino
PUT_FILL   encher o DESTINO até N          (mede no destino)
TAKE_FILL  encher o PRÓPRIO inventário até N (mede em si)
WAIT       esperar N tempo
```

`PUT_FILL` e `TAKE_FILL` são a diferença entre "mova 10" e "mantenha 10
lá". A primeira é uma ordem; a segunda é uma **política**. É a distinção
que separa um sistema de transporte de um sistema de reposição, e é a
razão de haver nove ações em vez de quatro.

Isso é o modelo mental de um *logistics network* (AE2, Refined Storage,
Create) trazido para peões. Bem pensado.

### O `canGoHomeNow`

```java
// AbstractWorkerEntity.java:431-440
/**
 * Safety hook for WorkerGoHomeGoal: lets a worker delay going home at night
 * until it is in a safe position. Default is always safe; the courier
 * overrides this so it first returns to the start of its route before
 * heading home (otherwise it could get stranded far away and fail to path back).
 */
public boolean canGoHomeNow() { return true; }
```

Um veto por profissão a um Goal genérico, com o motivo escrito. É a forma
certa de resolver "a regra geral não serve para um caso": não é
`if (worker instanceof CourierEntity)` no Goal — é um método que a
subclasse sobrescreve.

**Padrão de extensão limpo, e o único do mod inteiro.** Vale nota.

---

## 6. Upkeep — comida, pagamento, equipamento

`RecruitStorageUpkeepGoal` (327). Estende o Goal do Recruits e só muda o
caso "o alvo de upkeep é um `StorageArea`":

```java
// linhas 46-51
OPEN_TIME       = 16;  // ticks parado no baú aberto
REACH_SQR       = 9;   // ~3 blocos
FOOD_BUDGET     = 4;   // no máximo 4 comidas por rodada
MAX_CHESTS      = 16;  // teto de baús por rodada
```

E o mesmo pré-scan, com o mesmo raciocínio:

```java
// linhas 109-115
// Only queue chests that actually hold something the recruit would take for upkeep.
```

**Os quatro orçamentos são a parte a aprender.** Todo laço que percorre o
mundo tem um teto explícito e nomeado. É o mesmo princípio do seu
`Performance-Rules.md`, escrito como constante em vez de como documento.

O `isUpkeepItem` (linha 263) é um bom exemplo de predicado de domínio
legível:

```java
if (recruit.canEatItemStack(stack)) return true;          // comida
if (stack.is(currency))             return true;          // pagamento
if (recruit.wantsToPickUp(stack)) {
    if (recruit.canEquipItem(stack)) return true;         // arma/armadura
    if (recruit instanceof IRangedRecruit && stack.is(ItemTags.ARROWS)) return true;
}
```

---

## 7. O fluxo completo do briefing, respondido

```text
Worker → Inventory → Work → Produced Items → Storage
```

No Workers:

```text
1. Goal descobre que falta ferramenta/material
        → addNeededItem(required=true)
2. GetNeededItemsFromStorage assume
        → escolhe StorageArea (a última usada tem preferência: lastStorage)
        → pré-scan de conteúdo
        → anda, abre, tira, fecha
        → applyToNeededItems abate a contagem
3. Goal de profissão retoma, trabalha
        → drops caem no chão
        → aiStep() recolhe num raio de 5.5 blocos (linha 107-116)
        → farmedItems++
4. farmedItems > 128
        → DepositItemsToStorage assume
        → escolhe armazém, escolhe baú, abre, deposita, fecha
        → farmedItems = 0; lastStorage = area.getUUID()
5. de volta ao passo 1
```

O `lastStorage` (`AbstractChestGoal:122-124`) é uma boa micro-decisão: se
o último armazém usado ainda está por perto, **ignora todos os outros** e
vai direto nele. Fidelidade de local, sem custo.

---

## 8. Aproveitamento para o Village Colony

### Nível A — reimplementar o conceito

1. **`NeededItem` como dado, não como predicado.** O eixo "o trabalhador
   declara o que lhe falta em vez de falhar" é o que impede deadlock. Seu
   `Task` não tem esse eixo. Ver `12 §3.2`.
2. **`required` vs. oportunista.** Dois níveis de urgência, e só o
   primeiro dispara viagem.
3. **Fusão por fonte com `max`.** Evita crescimento infinito de pedido
   repetido — que é a forma do seu E1 no domínio de itens.
4. **`isSameContainer` para baú duplo.** Verificar o `ChestScanner`.
5. **`stateAbove.isAir()`** — baú inacessível não conta como estoque.
6. **Orçamentos nomeados em todo laço sobre o mundo.**

### Nível B — adaptar

7. **`lastStorage`** — preferir o último baú usado. Barato, e reduz
   caminhada.
8. **Pré-scan de conteúdo antes de andar.** Você já faz o equivalente no
   `ChestInventoryReader`.
9. **`canGoHomeNow()`** — veto por profissão a uma regra geral, como
   ponto de extensão nomeado.

### Nível D — não trazer

10. **`Predicate<ItemStack>` como identidade de pedido.** Custa varredura
    do registro e impede comparar/serializar. Sua ADR-005 já proíbe.
11. **Guardar `Container` em mapa de longa vida.** Referência que apodrece
    com o chunk.
12. **A dupla ordenação da `Stack`.** É um bug.
13. **Escrever em `getPersistentData()` de `ChestBlockEntity` Vanilla.**
    Sujeira permanente no save do jogador.
