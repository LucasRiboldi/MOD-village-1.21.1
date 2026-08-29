# Registro de conteúdo

Registrar é dar identidade: um `Identifier` estável aponta para um objeto, e é
esse id que aparece em saves, comandos, packets e datapacks.

Também é o **extension point mais barato que existe**. Muita coisa que parece
exigir Mixin exige, na verdade, uma entrada nova num registro.

## A cadeia — os quatro elos

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

Os quatro, ou o conteúdo não está pronto. Faltando o último, o jogo carrega, o
bloco existe, e aparece como cubo preto e rosa chamado `block.meumod.foo` —
código certo, entrega incompleta.

## Como registrar

```java
public final class ModItems {

    public static final Item MARTELO = registrar("martelo", new Item(new Item.Settings()));

    private static Item registrar(String path, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MeuMod.MOD_ID, path), item);
    }

    /** Chamado do onInitialize. Existe para forçar a carga da classe. */
    public static void register() {
        MeuMod.LOGGER.debug("Registrando itens");
    }
}
```

> A factory de `Identifier` e a assinatura de `Item.Settings` **mudaram entre
> versões** (1.21 trocou o construtor por `Identifier.of`). Confirme na sua:
> `javap -cp "$MC_JAR" net.minecraft.util.Identifier | head`

O método `register()` aparentemente vazio não é enfeite: registro por
inicialização estática só roda quando a classe é **carregada**. Sem uma chamada
explícita, nada acontece e o conteúdo simplesmente não existe.

## As três regras

Todas com a mesma raiz: **os ids precisam ser iguais em toda execução e nos dois
lados.**

**1. No entrypoint.** Antes do freeze.

**2. Incondicional.**

```java
// ✗ ids divergentes entre cliente e servidor → conexão cai no handshake
if (config.recursoAtivado) { Registry.register(...); }

// ✓ registra sempre; a config decide o COMPORTAMENTO, não a existência
Registry.register(...);
```

**3. Determinístico.**

```java
// ✗ ordem instável → ids numéricos mudam → saves antigos apontam para outra coisa
for (String nome : conjuntoNaoOrdenado) { registrar(nome); }

// ✓ ordem fixa
for (String nome : List.of("a", "b", "c")) { registrar(nome); }
```

## Namespace

```java
Identifier.of(MeuMod.MOD_ID, "martelo")   // meumod:martelo
```

**Nunca use `minecraft:` para conteúdo próprio** — colide com o Vanilla e com
todo mundo. Use a constante do mod, não a string literal repetida: id montado por
concatenação em vinte lugares é vinte chances de erro de digitação que só aparece
em runtime.

## Registros estáticos e dinâmicos

| | Estáticos | Dinâmicos |
|---|---|---|
| Exemplos | `ITEM`, `BLOCK`, `ENTITY_TYPE`, `BLOCK_ENTITY_TYPE`, `POINT_OF_INTEREST_TYPE`, `MEMORY_MODULE_TYPE`, `SENSOR_TYPE`, `VILLAGER_PROFESSION`, `SOUND_EVENT`, `SCREEN_HANDLER` | biomas, features, estruturas, encantamentos (1.21+), tipos de dano |
| Existem | no boot | **por mundo**, do datapack ativo |
| Estende-se com | `Registry.register` no entrypoint | JSON de datapack |

Registro dinâmico **não está disponível no `onInitialize`** — nenhum mundo foi
carregado. Código que assume isso falha.

## Registros que evitam Mixin

Estes transformam "preciso injetar" em "preciso registrar":

```text
POINT_OF_INTEREST_TYPE   local de trabalho, cama, sino
VILLAGER_PROFESSION      profissão
MEMORY_MODULE_TYPE       conhecimento do Brain
SENSOR_TYPE              percepção
ACTIVITY / SCHEDULE      modo e horário
ENTITY_TYPE / BLOCK_ENTITY_TYPE
```

Registrar uma memória ou POI novo não toca em nada do Vanilla e convive com
outros mods. É o degrau 2 da escada — verifique antes de subir.

## Quanta organização

**Poucos objetos:** registre direto, sem classe dedicada. `ModItems` com dois
itens é cerimônia.

**Muitos objetos:** uma classe por categoria — `ModItems`, `ModBlocks`,
`ModEntities`, `ModSounds`. O ganho é auditabilidade: um lugar para conferir o
que existe.

**O que evitar em qualquer tamanho:** registros espalhados por vinte arquivos.
Ninguém consegue responder "o que este mod registra?" sem ler o projeto inteiro.

A arquitetura cresce com o projeto. Ver `project-architecture.md`.

## Dependências entre registros

Ordem importa quando um depende do outro:

```text
BLOCK  →  BLOCK_ENTITY_TYPE (precisa do bloco)
BLOCK  →  BlockItem         (precisa do bloco)
ENTITY_TYPE → atributos → renderer (cliente)
```

`EntityType` sem atributos registrados **crasha no spawn**; sem renderer no lado
cliente, some ou crasha ao aparecer. São três registros distintos.

## Sincronização

Registros do mod precisam existir dos dois lados com os mesmos ids. A Fabric
cuida disso (`fabric-registry-sync-v0`) para os registros suportados, mas:

- id que existe só num lado derruba a conexão
- remover conteúdo de um save existente deixa referências órfãs
- `environment` no `fabric.mod.json` declara o que o mod assume

## Checklist

```text
[ ] no entrypoint, antes do freeze
[ ] incondicional
[ ] determinístico
[ ] namespace próprio
[ ] a classe é efetivamente carregada
[ ] dependências entre registros respeitadas
[ ] atributos registrados (entidade)
[ ] renderer registrado no lado cliente (entidade/block entity)
[ ] RESOURCES: lang, modelo, textura, loot table, recipe, tags
```

Detalhamento em `checklists/registration.md`. Plano em `templates/registry-plan.md`.
