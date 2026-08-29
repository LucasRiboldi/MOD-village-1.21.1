# Exemplo — bloco com estado, tick e inventário

**Pedido:**

> "Quero uma forja que funde minério em lingote ao longo do tempo, mostrando o
> progresso."

Este exemplo mostra o modo FEATURE, e principalmente **as três perguntas que
decidem a arquitetura antes de qualquer classe**.

---

## 1. O bloco precisa mesmo de BlockEntity?

A pergunta não é retórica. BlockEntity custa memória por posição, serialização e
tick.

```text
Precisa de estado?              sim — progresso da fundição
Cabe num BLOCK STATE?           ← a pergunta decisiva
```

Block state é mais barato, sincroniza sozinho e persiste sozinho. Ele caberia se
o estado fosse ligado/desligado ou um nível de 0 a 3.

Aqui: o progresso é contínuo **e** há inventário. Não cabe.

> `[DECISÃO]` BlockEntity é necessária. Mas o **estado visual** (acesa/apagada)
> vai num block state, não na BlockEntity — porque assim sincroniza de graça.

Essa divisão é o ponto mais importante deste exemplo: **nem todo estado do bloco
precisa morar no mesmo lugar.**

## 2. Quem é dono de quê?

| Estado | Dono | Mecanismo | Por quê |
|---|---|---|---|
| acesa / apagada | bloco | **block state** | o cliente precisa ver; sincroniza sozinho |
| progresso | BlockEntity | NBT | precisa sobreviver ao save |
| inventário | BlockEntity | NBT | idem |
| progresso **visível na tela** | BlockEntity | sync explícito | ver passo 5 |

## 3. Precisa de tick?

```text
Por que precisa rodar todo tick?
```

Fundição avança com o tempo — mas não precisa de resolução de 1/20 de segundo.

> `[DECISÃO]` Tick a cada 20 ticks (1×/s). O progresso avança em passos de 1
> segundo; o jogador não percebe a diferença e o custo cai por 20.

---

## Implementação, passo a passo

### Passo 1 — bloco e registro

```java
public static final Block FORJA = Registry.register(Registries.BLOCK,
        Identifier.of(MOD_ID, "forja"),
        new ForjaBlock(AbstractBlock.Settings.create().strength(3.0f)));

public static final Item FORJA_ITEM = Registry.register(Registries.ITEM,
        Identifier.of(MOD_ID, "forja"),
        new BlockItem(FORJA, new Item.Settings()));
```

```bash
./gradlew build
```

### Passo 2 — resources, cedo

```text
assets/meumod/lang/en_us.json          "block.meumod.forja": "Forge"
assets/meumod/blockstates/forja.json   dois modelos: acesa e apagada
assets/meumod/models/block/forja*.json
assets/meumod/models/item/forja.json
assets/meumod/textures/block/forja*.png
data/meumod/loot_table/blocks/forja.json
data/minecraft/tags/block/mineable/pickaxe.json
```

```bash
./gradlew runClient
```

**Ver o bloco no jogo já aqui valida o registro inteiro de uma vez.** Deixar
resources para o fim é o erro que faz a feature "estar pronta" e aparecer como
cubo preto e rosa.

### Passo 3 — block state

```java
public static final BooleanProperty ACESA = BooleanProperty.of("acesa");

@Override
protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
    builder.add(ACESA);
}
```

Sincroniza sozinho. Zero código de rede para o visual.

### Passo 4 — BlockEntity e persistência

```java
public static final BlockEntityType<ForjaBlockEntity> FORJA_BE =
        Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "forja"),
                BlockEntityType.Builder.create(ForjaBlockEntity::new, ModBlocks.FORJA).build());
```

O bloco precisa existir antes — respeite a ordem.

```java
public class ForjaBlockEntity extends BlockEntity {

    private int progresso;

    public ForjaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FORJA_BE, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("versao", 1);          // ← migração futura
        nbt.putInt("progresso", progresso);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        progresso = nbt.getInt("progresso");
    }

    public void avancar() {
        progresso++;
        markDirty();                       // ← sem isto, some no restart
    }
}
```

> `RegistryWrapper.WrapperLookup` é **1.20.5+**. Confirme com `javap` antes de
> copiar.

**Testar persistência AGORA, não no fim:**

```text
colocar a forja → deixar progredir → FECHAR o mundo → REABRIR → o progresso voltou
```

Se não voltou, é `markDirty` ou o par escrita/leitura. Descobrir isso agora custa
minutos; descobrir depois de mais dez arquivos custa a tarde.

### Passo 5 — tick

```java
public static void tick(World world, BlockPos pos, BlockState state, ForjaBlockEntity be) {
    if (world.isClient()) return;
    if (world.getTime() % 20 != 0) return;

    if (!be.temTrabalho()) return;
    be.avancar();

    if (be.terminou()) {
        be.produzir();
        world.setBlockState(pos, state.with(ForjaBlock.ACESA, false));
    }
}
```

Duas saídas antecipadas antes de qualquer trabalho: lado errado e tick errado.
É o padrão que mantém o custo perto de zero na maior parte dos ticks.

### Passo 6 — o progresso na tela

Aqui aparece a distinção que o passo 1 preparou:

- **acesa/apagada** já chega ao cliente pelo block state — de graça.
- **o número do progresso** só existe na BlockEntity, que **não sincroniza
  sozinha**.

Duas opções honestas:

| Opção | Custo | Quando |
|---|---|---|
| `toUpdatePacket` na block entity | sync a cada mudança, para todos por perto | se o progresso é visível **no mundo** |
| `ScreenHandler` com propriedade | sync só para quem está com a tela aberta | se o progresso só aparece **na tela** |

> `[DECISÃO]` A segunda. O progresso só é visível na tela, então sincronizá-lo
> para todo mundo por perto seria tráfego desperdiçado.

Ver `references/networking.md`.

---

## Validar

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew runGametest
```

```text
[ ] o bloco aparece, com nome e textura
[ ] dropa ao quebrar
[ ] a fundição avança
[ ] o visual muda (block state)
[ ] o progresso aparece na tela
[ ] FECHAR e REABRIR o mundo → progresso preservado
[ ] servidor dedicado + cliente conectando → tudo acima continua
[ ] quebrar o bloco no meio da fundição → sem crash, itens dropam
```

O penúltimo é o que separa "funciona" de "funciona em multiplayer". O último é o
edge case que a implementação feliz esquece.

---

## O que este exemplo demonstra

1. **"Precisa de BlockEntity?" é uma pergunta real** — e a resposta pode ser não.
2. **Nem todo estado do bloco mora no mesmo lugar.** Dividir entre block state e
   BlockEntity resolveu a sincronização visual sem uma linha de rede.
3. **Tick com frequência justificada**, com saídas antecipadas baratas.
4. **Persistência testada no passo 4**, não no fim.
5. **Resources no passo 2**, validando o registro inteiro cedo.
6. **A escolha de sincronização foi decidida pelo uso**, não pelo hábito.
