# Análise data-driven

Uma parte grande do Minecraft **não é código** — é JSON carregado de datapacks e
resource packs. Isso importa porque o degrau 3 da escada de extensão (configuração
data-driven) resolve muito mais casos do que a maioria supõe, e sem uma linha de
Java.

Pergunta a fazer sempre: **isso precisa mesmo de Java, ou é um JSON?**

## Os dois pacotes

| | Data pack | Resource pack |
|---|---|---|
| Vive em | `data/<namespace>/` | `assets/<namespace>/` |
| Lado | **servidor** | **cliente** |
| Contém | recipes, loot tables, tags, advancements, funções, worldgen, dimensões | modelos, texturas, blockstates, sons, lang |
| Recarrega com | `/reload` | F3+T |

Um mod entrega os dois dentro do jar. Erro comum: pôr algo em `assets/` esperando
que o servidor leia (não lê), ou em `data/` esperando que o cliente use (não usa).

## O que é data-driven em 1.21.x

```text
data/<ns>/recipe/            receitas
data/<ns>/loot_table/        drops de bloco, mob, baú
data/<ns>/tags/              tags de bloco, item, entidade, bioma, fluido...
data/<ns>/advancement/       progresso
data/<ns>/enchantment/       encantamentos (viraram data-driven em 1.21)
data/<ns>/worldgen/          biomas, features, estruturas
data/<ns>/damage_type/       tipos de dano
data/<ns>/function/          funções de comando

assets/<ns>/models/          modelos de bloco e item
assets/<ns>/blockstates/     estado → modelo
assets/<ns>/textures/
assets/<ns>/lang/            traduções
assets/<ns>/sounds.json
```

Os nomes de pasta mudaram entre versões (singular vs. plural: `recipe` vs.
`recipes`, `loot_table` vs. `loot_tables`). **Confirme na sua versão** olhando o
jar do Minecraft ou um mod que funcione:

```bash
unzip -l ~/.gradle/caches/fabric-loom/1.21.1/minecraft-server.jar | grep "data/minecraft/" | head -20
```

## Tags — o extension point mais subestimado

Tags são listas de ids que o Vanilla consulta em vez de checar tipo. Elas são
**aditivas entre datapacks**: seu mod acrescenta à tag sem substituir o conteúdo
de ninguém.

```json
{ "replace": false, "values": ["mymod:copper_ore", "#c:ores/tin"] }
```

`"replace": false` é o que garante a convivência. `true` apaga o que os outros
puseram — quase nunca é o que você quer.

Isso resolve, sem Java, casos como "meu bloco deve ser minerável com picareta",
"meu item conta como madeira", "meu mob deve queimar ao sol". Antes de escrever
Mixin para mudar um comportamento condicional, **veja se a condição é uma tag**.

## NBT e Data Components

Mudança importante em **1.20.5**: itens deixaram de usar NBT solto e passaram a
Data Components — mapa tipado de componentes, com codec e sincronização.

| | NBT | Data Components |
|---|---|---|
| Onde ainda vale | entidade, block entity, `PersistentState`, mundo | **item stacks** |
| Tipagem | fraca, string-based | forte, com codec |
| Versão | sempre | 1.20.5+ |

Consequência prática: **todo tutorial de "NBT customizado em item" anterior a
1.20.5 está errado para você.** Verifique antes de seguir.

Ao analisar dados de item, pergunte:

```text
É componente? Qual? É Vanilla ou customizado?
Tem codec? Como serializa?
É sincronizado para o cliente?
O que acontece com um item salvo antes da mudança?
```

## Como o dado chega ao jogo

```text
JSON no jar/datapack
    ↓  ResourceManager carrega no start e no /reload
Registro dinâmico ou lista interna
    ↓  servidor sincroniza o que o cliente precisa
Consumo em código
```

Dois pontos que geram bug:

1. **Registros dinâmicos** (worldgen, encantamentos, tipos de dano) são carregados
   **por mundo**, não no start do jogo. Eles dependem do datapack ativo naquele
   save. Código que assume disponibilidade no `onInitialize` falha.
2. **Sincronização.** Se o cliente precisa do dado e ele não é sincronizado,
   aparece dessincronia — item sem nome, bloco sem textura, receita invisível.

## Roteiro de análise

Para cada sistema data-driven:

```text
ONDE O DADO É DEFINIDO?     caminho e formato
COMO É CARREGADO?           reload, por mundo, no boot
QUEM CONSOME?               a classe que lê
PODE SER MODIFICADO?        por datapack de terceiros?
É ADITIVO ou SUBSTITUTIVO?  tags são aditivas; loot tables não
É PERSISTENTE?
É SINCRONIZADO?
CLIENT OU SERVER?
```

Localizar quem consome:

```bash
grep -rn "TagKey\|RegistryKeys.LOOT_TABLE\|RecipeType" /tmp/mcsrc/net/minecraft | head -20
```

## Substituição e conflito

- **Tags** — aditivas. Baixo risco.
- **Loot tables / recipes** — o mesmo id é **substituído** por quem carregar
  depois. Dois mods mexendo na mesma loot table Vanilla: um perde silenciosamente.
- **Worldgen** — substituição parcial gera mundos inconsistentes entre saves.

Isso é material de `compatibility-analysis.md`: sobrescrever um arquivo Vanilla é
um `[RISCO]` a declarar, mesmo quando funciona no seu teste.

## Datagen

A Fabric API gera esses JSONs a partir de código (`fabric-data-generation-api-v1`),
o que elimina erro de digitação e mantém código e dado em sincronia.

Ao analisar um mod, ache o gerado antes de estranhar a ausência:

```bash
find . -path "*generated*" -name "*.json" | head
grep -rn "fabric-datagen\|DataGeneratorEntrypoint" src/ | head
```

Arquivo em `src/main/generated/` **não se edita à mão** — o próximo datagen
sobrescreve.

## A pergunta que fecha

Antes de concluir "precisa de Java":

```text
[ ] Existe tag que já controla isso?
[ ] Existe loot table / recipe que expressa isso?
[ ] Existe registro dinâmico com entrada nova?
[ ] O comportamento é condicional a algo que é dado, não código?
```

Quatro "não" verificados autorizam subir a escada. Quatro "não sei" não.
