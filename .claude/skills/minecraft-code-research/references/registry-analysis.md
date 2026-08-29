# Análise de registries

Registry é como o Minecraft dá identidade a coisas: um `Identifier` estável
aponta para um objeto, e é esse id que aparece em saves, comandos, packets e
datapacks.

Para pesquisa, o que importa é: **registrar é o extension point mais barato que
existe.** Muita coisa que parece exigir Mixin exige, na verdade, uma entrada nova
num registro.

## O modelo

```text
REGISTRY → IDENTIFIER → OBJECT → BOOTSTRAP → REFERENCE
```

```java
Identifier id = Identifier.of("mymod", "copper_hammer");   // confira a factory na sua versão
Registry.register(Registries.ITEM, id, new Item(settings));
```

O `Identifier` tem namespace e path. **Namespace é a sua identidade** — usar
`minecraft:` para conteúdo próprio colide com o Vanilla e com todo mundo.

## Registros estáticos vs. dinâmicos

Distinção que decide onde e quando registrar:

| | Estáticos | Dinâmicos |
|---|---|---|
| Exemplos | `ITEM`, `BLOCK`, `ENTITY_TYPE`, `BLOCK_ENTITY_TYPE`, `POINT_OF_INTEREST_TYPE`, `MEMORY_MODULE_TYPE`, `SENSOR_TYPE`, `VILLAGER_PROFESSION`, `SOUND_EVENT` | biomas, features, estruturas, encantamentos (1.21+), tipos de dano |
| Quando existem | no boot do jogo | **por mundo**, do datapack ativo |
| Como se estende | `Registry.register` no entrypoint | JSON no datapack |
| Congela? | sim, depois do boot | recarrega no `/reload` |

Consequência: código que assume um registro dinâmico disponível no
`onInitialize` está errado — naquele momento nenhum mundo foi carregado.

## Janela de registro

```text
FABRIC LOADER → ENTRYPOINT (onInitialize) → REGISTRO → freeze → mundo → jogo
```

Registro **no entrypoint**, sempre. Três regras, e as três têm a mesma raiz — os
ids precisam ser iguais em toda execução e nos dois lados:

1. **Incondicional.** Nada de `if (configAtivada) register(...)`. Config diferente
   entre cliente e servidor → ids diferentes → a conexão cai com mismatch.
2. **Determinístico.** Mesma ordem toda vez. Ordem instável (iterar um `HashSet`)
   produz ids numéricos diferentes entre execuções, e saves antigos passam a
   apontar para a coisa errada.
3. **Antes do freeze.** Registrar depois lança exceção.

Detalhe de Java que morde: registro por inicialização estática de classe só roda
quando a classe é **carregada**. Se ninguém a toca, nada registra. Por isso os
mods costumam ter uma chamada explícita no entrypoint.

## Registros que importam para IA e aldeões

Estes são exatamente os que transformam "preciso de Mixin" em "preciso de uma
entrada":

```text
POINT_OF_INTEREST_TYPE   local de trabalho, cama, sino
VILLAGER_PROFESSION      profissão
MEMORY_MODULE_TYPE       conhecimento do Brain
SENSOR_TYPE              percepção
ACTIVITY                 modo do Brain
SCHEDULE                 horário
ENTITY_TYPE / BLOCK_ENTITY_TYPE
```

Registrar uma memória ou um POI novo não toca em nada do Vanilla e convive com
outros mods. Ver `ai-brain-analysis.md` para o que fazer com elas depois.

## Sincronização com o cliente

Registros do mod precisam existir dos dois lados com os mesmos ids. A Fabric API
cuida disso (`fabric-registry-sync-v0`) para os registros suportados, mas:

- o cliente precisa ter o mod instalado, salvo para conteúdo server-side puro
- id que existe só num lado derruba a conexão no handshake
- remover conteúdo de um save existente deixa referências órfãs

Ao analisar um mod, `"environment"` no `fabric.mod.json` diz o que ele assume.

## O que investigar

```text
ONDE registra?          classe e método
QUANDO?                 entrypoint, evento, estático
COMO é acessado?        constante, lookup por id, tag
CICLO DE VIDA           estático ou dinâmico
DEPENDÊNCIAS            precisa de outro registro antes?
DATAGEN relacionado     gera JSON a partir disto?
RECURSOS associados     modelo, textura, lang, loot table
```

O último ponto é a fonte de um bug clássico: **objeto registrado sem recurso**.
O jogo carrega, o bloco existe, e aparece como cubo preto e rosa com nome
`block.mymod.foo`. O código estava certo e incompleto.

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

Os quatro elos, ou o conteúdo não está pronto.

## Localizar registros num projeto

```bash
grep -rn "Registry.register\|Registries\." src/main/java | head -30
grep -rn "Identifier.of\|new Identifier" src/main/java | head -20
```

Um projeto com registros espalhados por vinte arquivos é mais difícil de auditar
que um com uma classe por categoria — anote isso como observação de arquitetura,
não como defeito automático: com poucos objetos, a classe dedicada é cerimônia.

## Sinais

**Bom:** registro no entrypoint, incondicional, namespace próprio, id derivado de
constante, recursos presentes.

**Ruim:** registro dentro de evento de mundo; id montado por concatenação
condicional; namespace `minecraft`; registro em bloco estático sem chamada
garantida; conteúdo sem lang nem modelo.
