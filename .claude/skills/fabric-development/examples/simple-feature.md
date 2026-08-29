# Exemplo — feature pequena (modo SMALL)

**Pedido:**

> "Adiciona um martelo de cobre ao mod."

Este exemplo existe tanto para mostrar o fluxo quanto para mostrar **o que não
fazer**: burocracia em tarefa pequena é desperdício, e é o erro mais comum de
quem acabou de aprender um processo.

---

## Classificar — 30 segundos

```text
TIPO      FEATURE, pequena
MODO      SMALL
SCOPE     um item, sem comportamento customizado
IMPACTO   registro + resources. Sem persistência, sem rede, sem Mixin.
```

**O que NÃO fazer aqui:**

```text
✗ feature-plan.md de trinta linhas para um item
✗ ItemFactory / ItemRegistry genérico
✗ pesquisa em docs/research/ (não há sistema Vanilla a compreender)
✗ system-contract.md
```

O modo SMALL existe justamente para isso: **identificar → implementar →
compilar → validar.**

## Verificar o ambiente — 10 segundos

```bash
grep -E "minecraft_version|yarn_mappings" gradle.properties
```

```text
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
```

Importa porque a assinatura de `Item.Settings` e a factory de `Identifier`
mudaram entre versões. Na dúvida:

```bash
javap -cp "$MC_JAR" net.minecraft.item.Item | head -5
```

## Implementar

```java
public final class ModItems {

    public static final Item MARTELO_DE_COBRE = registrar("martelo_de_cobre",
            new Item(new Item.Settings().maxDamage(180)));

    private static Item registrar(String path, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(MeuMod.MOD_ID, path), item);
    }

    /** Chamado do onInitialize — força a carga da classe. */
    public static void register() {
        MeuMod.LOGGER.debug("Registrando itens");
    }
}
```

E no entrypoint:

```java
@Override
public void onInitialize() {
    ModItems.register();
}
```

O `register()` aparentemente vazio **não é enfeite**: registro por inicialização
estática só roda quando a classe é carregada. Sem a chamada, nada acontece e o
item simplesmente não existe.

> **Por que não há `.maxCount(1)` aqui.** O pedido dizia "não empilha", e a
> tentação é escrever `.maxCount(1).maxDamage(180)`.
>
> `[FATO]` verificado no bytecode de `Item$Settings` em 1.21.1: `maxDamage(int)`
> já seta **três** componentes — `MAX_DAMAGE`, `DAMAGE = 0` e
> **`MAX_STACK_SIZE = 1`**. Item com durabilidade não empilha por construção.
>
> Escrever `.maxCount(1)` junto é redundante; escrever `.maxCount(64)` **depois**
> de `maxDamage` cria um item com durabilidade e pilha, que é um estado
> inconsistente. Confira antes de encadear:
>
> ```bash
> javap -c -p -cp "$MC_JAR" 'net.minecraft.item.Item$Settings' | sed -n '/maxDamage(int)/,/areturn/p'
> ```

```bash
./gradlew build
```

## Resources — a metade que costuma faltar

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

O código acima cobre três elos. O quarto:

```json
// assets/meumod/lang/en_us.json
{ "item.meumod.martelo_de_cobre": "Copper Hammer" }
```

```json
// assets/meumod/models/item/martelo_de_cobre.json
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "meumod:item/martelo_de_cobre" }
}
```

```text
assets/meumod/textures/item/martelo_de_cobre.png
```

E os dois que quase sempre são esquecidos:

```text
[ ] item group   — senão o item existe e é invisível no criativo
[ ] recipe       — senão só dá para obter por comando
```

## Validar

```bash
./gradlew build
./gradlew runClient
```

```text
[ ] o item aparece no criativo
[ ] o nome é "Copper Hammer", não "item.meumod.martelo_de_cobre"
[ ] a textura aparece
[ ] a receita funciona
```

```bash
./gradlew runServer
```

Rápido, e vale: pega registro condicional e classe de cliente vazada. Trinta
segundos que evitam um relatório de bug.

## Relato

> Adicionado `meumod:martelo_de_cobre` — item de mão, durabilidade 180.
>
> **Verificado rodando:** `build`, `runClient` (aparece no criativo, nome e
> textura corretos, receita funciona), `runServer` (sobe sem erro).
>
> **Não feito:** nenhum comportamento customizado — o pedido não mencionou. Se a
> ideia era que o martelo quebrasse blocos em área, isso é outra feature e muda o
> modo para FEATURE.

A última linha é o que transforma uma entrega literal numa entrega útil: o pedido
foi cumprido como escrito, e a ambiguidade foi devolvida em vez de resolvida em
silêncio.

---

## O que este exemplo demonstra

1. **Proporcionalidade.** Nem toda tarefa precisa de plano, contrato e pesquisa.
   Aplicar o processo grande aqui custaria mais que a feature.
2. **Resources são metade do trabalho** de conteúdo — e a metade esquecida.
3. **`runServer` vale mesmo em tarefa pequena.** É barato e pega uma classe
   inteira de erro.
4. **A ambiguidade foi declarada, não resolvida sozinha.**
