# Exemplo — organizar registros conforme o projeto cresce

**Situação:**

> O mod começou com dois itens registrados no `onInitialize`. Agora tem 14 itens,
> 9 blocos, 3 block entities, 2 tipos de entidade e uma profissão de aldeão —
> tudo espalhado.

A pergunta não é "qual é a estrutura certa". É **"qual estrutura este projeto
merece hoje?"**

---

## Os três estágios

Nenhum é errado; cada um é certo num tamanho.

### Estágio 1 — poucos objetos: direto no entrypoint

```java
@Override
public void onInitialize() {
    Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "moeda"), new Item(new Item.Settings()));
}
```

Com dois ou três objetos, `ModItems` é **cerimônia**. Não crie.

### Estágio 2 — uma classe por categoria

É onde este projeto está agora.

```java
public final class ModItems {
    public static final Item MOEDA   = registrar("moeda", new Item(new Item.Settings()));
    public static final Item MARTELO = registrar("martelo", new Item(new Item.Settings().maxCount(1)));

    private static Item registrar(String path, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MeuMod.MOD_ID, path), item);
    }

    public static void register() { MeuMod.LOGGER.debug("Registrando itens"); }
}
```

```java
@Override
public void onInitialize() {
    ModBlocks.register();      // ← antes dos itens: BlockItem precisa do bloco
    ModItems.register();
    ModBlockEntities.register();
    ModEntities.register();
    ModVillagers.register();
}
```

**O ganho é auditabilidade:** existe um lugar para responder "o que este mod
registra?".

### Estágio 3 — por domínio

Quando o mod passa a ter sistemas com identidade própria:

```text
mod/
├── registry/          ModItems, ModBlocks, ModEntities…
├── villager/
│   ├── profession/    registro da profissão
│   └── ai/
├── mining/
└── client/
```

**Não pule para o estágio 3 cedo.** Reorganizar antes da hora é trabalho sem
ganho, e a estrutura por domínio só ajuda quando os domínios existem.

## Ordem de registro — onde dá errado

```java
// ✗ BlockItem antes do bloco → NPE no boot
public static final Item FORJA_ITEM = registrar("forja", new BlockItem(ModBlocks.FORJA, ...));

// ✓ o bloco existe primeiro
ModBlocks.register();
ModItems.register();
```

As dependências reais:

```text
BLOCK        → BLOCK_ENTITY_TYPE   (o builder precisa do bloco)
BLOCK        → BlockItem
ENTITY_TYPE  → atributos           (sem isto, crash no spawn)
ENTITY_TYPE  → renderer (cliente)  (sem isto, some ou crasha)
```

## As três regras, aplicadas

```java
// ✗ ids divergentes entre cliente e servidor → conexão cai no handshake
public static void register() {
    if (Config.recursoAvancado) {
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "avancado"), new Item(...));
    }
}

// ✓ registra sempre; a config decide o COMPORTAMENTO
public static final Item AVANCADO = registrar("avancado", new Item(...));
```

```java
// ✗ ordem instável → ids numéricos mudam entre execuções → saves corrompem
for (String nome : configuracoesLidas.keySet()) { registrar(nome, ...); }

// ✓ ordem fixa
for (String nome : List.of("cobre", "estanho", "bronze")) { registrar(nome, ...); }
```

## Registros que evitam Mixin

O caso da profissão de aldeão é ilustrativo. A intuição é "preciso injetar em
`VillagerProfession`". A realidade:

```java
public final class ModVillagers {

    public static final PointOfInterestType POI_FORJA = registrarPoi("forja", ModBlocks.FORJA);

    public static final VillagerProfession FERREIRO = registrarProfissao("ferreiro", POI_FORJA);
    ...
}
```

> Confirme as assinaturas na sua versão — a API de POI e profissão mudou entre
> releases: `javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession`

**Isso é o degrau 2 da escada**, não toca em nada do Vanilla e convive com outros
mods. O mesmo vale para `MEMORY_MODULE_TYPE`, `SENSOR_TYPE`, `ACTIVITY` e
`SCHEDULE`.

Antes de escrever Mixin para qualquer coisa relacionada a aldeão, **verifique se
é um registro**.

## Verificar

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

```text
[ ] tudo aparece no criativo
[ ] nada de "block.meumod.foo" na tela
[ ] nada de cubo preto e rosa
[ ] os blocos dropam algo
[ ] o servidor dedicado sobe
[ ] cliente conecta ao servidor dedicado    ← pega registro condicional
```

O último é o teste que a maioria pula, e é o único que pega ids divergentes.

---

## O que este exemplo demonstra

1. **A estrutura cresce com o projeto.** Os três estágios são todos corretos, em
   tamanhos diferentes.
2. **Ordem de registro é dependência real**, não estilo.
3. **Registro condicional e ordem instável quebram multiplayer e saves** — e não
   aparecem em nenhum teste local.
4. **Muita coisa que parece exigir Mixin é um registro.** POI, profissão, memória
   e sensor são todos degrau 2.
