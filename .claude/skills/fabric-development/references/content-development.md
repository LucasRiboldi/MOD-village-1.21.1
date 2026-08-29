# Blocos, itens, block entities e inventário

O conteúdo básico de um mod. A regra que atravessa tudo:

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

Faltando o último elo, o jogo carrega e o conteúdo aparece quebrado. Ver
`registration.md` e `datagen-and-resources.md`.

> As assinaturas de `Settings`, `AbstractBlock.Settings` e construtores mudam
> entre versões. Confirme na sua antes de copiar qualquer exemplo:
> `javap -cp "$MC_JAR" net.minecraft.item.Item`

---

## Itens

```java
public static final Item MARTELO =
        Registry.register(Registries.ITEM,
                Identifier.of(MOD_ID, "martelo"),
                new Item(new Item.Settings().maxCount(1)));
```

O que costuma faltar depois:

```text
[ ] lang           senão aparece "item.meumod.martelo"
[ ] modelo         assets/meumod/models/item/martelo.json
[ ] textura
[ ] tags           se deve contar como ferramenta, combustível, etc.
[ ] item group     senão não aparece em nenhuma aba do criativo
```

Comportamento customizado: estenda `Item` e sobrescreva o que precisa (`use`,
`useOnBlock`, `inventoryTick`). **Cuidado com `inventoryTick`**: roda para cada
stack no inventário de cada jogador, todo tick.

## Blocos

```java
public static final Block FORJA =
        Registry.register(Registries.BLOCK,
                Identifier.of(MOD_ID, "forja"),
                new Block(AbstractBlock.Settings.create().strength(3.0f)));

// quase sempre você também quer o item do bloco
public static final Item FORJA_ITEM =
        Registry.register(Registries.ITEM,
                Identifier.of(MOD_ID, "forja"),
                new BlockItem(FORJA, new Item.Settings()));
```

Bloco sem `BlockItem` existe no mundo mas não pode ser obtido nem colocado — é
uma das omissões mais comuns.

```text
[ ] blockstate     assets/meumod/blockstates/forja.json
[ ] modelo de bloco + modelo de item
[ ] textura
[ ] loot table     data/meumod/loot_table/blocks/forja.json — senão não dropa nada
[ ] tags de mineração  senão qualquer ferramenta serve, ou nenhuma
```

### Block state antes de BlockEntity

Se o estado tem poucos valores discretos — ligado/desligado, orientação, nível —
ele cabe no **block state**, que é mais barato, sincroniza sozinho e persiste
sozinho.

```java
public static final BooleanProperty ACESO = BooleanProperty.of("aceso");
```

Cada propriedade multiplica o número de estados possíveis, então mantenha poucas.
Mas prefira isto a criar uma BlockEntity só para guardar um booleano.

## BlockEntity — só quando necessário

**Antes de criar, responda:**

```text
O bloco precisa mesmo de estado?          → se não, não crie
Precisa de tick?                          → tick custa
Precisa de inventário?
Precisa de serialização?
Precisa sincronizar com o cliente?
Cabe num BLOCK STATE?                     → se cabe, use block state
```

BlockEntity criada por conveniência é custo permanente: memória por posição,
serialização, e tick se você registrar um.

### Criar

```java
public static final BlockEntityType<ForjaBlockEntity> FORJA_BE =
        Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "forja"),
                BlockEntityType.Builder.create(ForjaBlockEntity::new, ModBlocks.FORJA).build());
```

O bloco precisa existir antes — respeite a ordem de registro.

```java
public class ForjaBlockEntity extends BlockEntity {

    private int progresso;

    public ForjaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FORJA_BE, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("progresso", progresso);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        progresso = nbt.getInt("progresso");
    }

    public void setProgresso(int valor) {
        this.progresso = valor;
        markDirty();                    // ← sem isto, não é salvo
    }
}
```

> `RegistryWrapper.WrapperLookup` nas assinaturas é **1.20.5+**. Em versões
> anteriores a forma é outra — confirme com `javap`.

**`markDirty()` depois de mudar.** Sem ele o jogo não grava, e a perda é
silenciosa: o estado parece certo até o restart.

### Tick

```java
public static void tick(World world, BlockPos pos, BlockState state, ForjaBlockEntity be) {
    if (world.isClient()) return;
    if (world.getTime() % 20 != 0) return;    // 1×/s em vez de 20×/s
    ...
}
```

Antes de registrar um ticker, pergunte: **por que precisa rodar todo tick?** Pode
ser periódico, reativo ou sob demanda? Ver `performance.md`.

### Sincronizar com o cliente

BlockEntity **não** sincroniza sozinha. Se o cliente precisa ver o estado,
implemente `toUpdatePacket` / `toInitialChunkDataNbt`. Se o dado só afeta lógica,
não sincronize — é tráfego de graça. Ver `networking.md`.

## Inventário

```text
Precisa mesmo?                    → muita coisa se resolve sem inventário
Quantos slots?
O jogador interage?               → precisa de ScreenHandler + tela no cliente
Automação (hopper) interage?      → considere a Transfer API
Persiste?                         → NBT da block entity
```

Se for para automação, `fabric-transfer-api-v1` é o mecanismo previsto e convive
melhor com outros mods do que uma implementação própria.

Tela é **client-side**: `ScreenHandler` é comum, `Screen` é cliente. A tela nunca
decide — ela envia intenção e o servidor valida. Ver `client-server.md`.

## A ordem de construção

Funciona melhor nesta ordem, compilando entre as etapas:

```text
1. bloco/item registrado         → build → aparece no jogo (sem textura)
2. resources                     → build → aparece direito
3. block state, se houver
4. BlockEntity + persistência    → build → save/load funciona
5. lógica
6. sincronização, se necessária
7. inventário/tela, se necessário
```

**Resources cedo (etapa 2), não no fim.** Ver o bloco no jogo já na segunda etapa
valida o registro inteiro de uma vez.

## Erros frequentes

```text
[ ] bloco sem BlockItem
[ ] bloco sem loot table → não dropa
[ ] sem lang → "block.meumod.forja"
[ ] sem modelo/textura → cubo preto e rosa
[ ] BlockEntity criada para o que cabia em block state
[ ] markDirty esquecido → estado some no restart
[ ] tick registrado sem necessidade
[ ] tela decidindo em vez de pedir
[ ] BlockEntity registrada antes do bloco
```
