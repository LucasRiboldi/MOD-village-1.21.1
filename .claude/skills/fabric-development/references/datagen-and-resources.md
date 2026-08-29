# Datagen e resources

**Código não está completo até os resources existirem.** A cadeia:

```text
JAVA OBJECT → REGISTRY → IDENTIFIER → RESOURCE
```

Faltando o último elo, o jogo carrega e o conteúdo aparece quebrado — cubo preto
e rosa chamado `block.meumod.forja`, que não dropa nada. Código certo, entrega
incompleta.

## Os dois pacotes

| | `assets/` | `data/` |
|---|---|---|
| Lado | **cliente** | **servidor** |
| Contém | modelos, texturas, blockstates, lang, sons | recipes, loot tables, tags, advancements, worldgen |
| Recarrega | F3+T | `/reload` |

Erro comum: pôr algo em `assets/` esperando que o servidor leia (não lê), ou em
`data/` esperando que o cliente use (não usa).

## O que cada conteúdo exige

**Item**

```text
assets/meumod/lang/en_us.json          "item.meumod.martelo": "Hammer"
assets/meumod/models/item/martelo.json
assets/meumod/textures/item/martelo.png
data/meumod/tags/…                     se deve contar como ferramenta, combustível…
```

**Bloco**

```text
assets/meumod/lang/en_us.json
assets/meumod/blockstates/forja.json
assets/meumod/models/block/forja.json
assets/meumod/models/item/forja.json    ← o item do bloco, esquecido com frequência
assets/meumod/textures/block/forja.png
data/meumod/loot_table/blocks/forja.json   ← sem isto, não dropa nada
data/minecraft/tags/block/mineable/pickaxe.json  ← ferramenta correta
```

**Entidade**

```text
assets/meumod/lang/en_us.json          "entity.meumod.meu_mob"
assets/meumod/textures/entity/…
+ renderer registrado no entrypoint client
```

> Os nomes de pasta mudaram entre versões (`recipe` vs `recipes`, `loot_table` vs
> `loot_tables`). **Confirme na sua:**
> ```bash
> unzip -l ~/.gradle/caches/fabric-loom/1.21.1/minecraft-server.jar | grep "data/minecraft/" | head
> ```

## Tags — o mecanismo mais subestimado

Tags são listas de ids que o Vanilla consulta em vez de checar tipo. São
**aditivas entre datapacks**:

```json
{ "replace": false, "values": ["meumod:minerio_estanho", "#c:ores/tin"] }
```

`"replace": false` é o que garante a convivência. `true` apaga o que os outros
puseram — quase nunca é o que você quer.

Isso resolve, **sem Java**: "meu bloco é minerável com picareta", "meu item conta
como madeira", "meu mob queima ao sol". Antes de escrever código para um
comportamento condicional, **veja se a condição é uma tag**.

## Datagen — quando vale

A Fabric gera esses JSONs a partir de código
(`fabric-data-generation-api-v1`).

**Vale quando:**

```text
[ ] o arquivo é repetitivo (vinte blocos com o mesmo padrão de modelo)
[ ] o dado é derivado do código (loot table = "dropa a si mesmo")
[ ] datagen reduz erro de digitação em id
[ ] código e dado precisam ficar em sincronia
```

**Não vale quando:**

```text
[ ] são três arquivos escritos uma vez
[ ] cada arquivo é único e artesanal
[ ] você está adotando datagen por moda
```

O custo do datagen é real: um sourceset a mais, um passo de build, e uma
indireção entre o que você escreve e o que o jogo lê. Para um mod pequeno, isso
supera o ganho.

## Se usar datagen

```bash
./gradlew runDatagen
```

O gerado vai para `src/main/generated/` (ou o que o build definir).

**Arquivo gerado não se edita à mão.** O próximo datagen sobrescreve, e a edição
some sem aviso. Se você precisa mudar, mude o gerador.

```text
[ ] o gerado está versionado no git? (decisão do projeto — as duas são válidas)
[ ] o build roda datagen, ou o dev roda manualmente?
[ ] arquivos gerados estão claramente separados dos escritos à mão
```

## Lang

```json
{
  "item.meumod.martelo": "Hammer",
  "block.meumod.forja": "Forge",
  "entity.meumod.meu_mob": "My Mob",
  "itemGroup.meumod.geral": "My Mod"
}
```

**Toda chave de lang é contrato:** renomear apaga a tradução, inclusive as que
outras pessoas contribuíram.

O sintoma de lang faltando é o próprio id aparecendo na tela — fácil de ver, e
fácil de deixar passar quando só se testa em criativo procurando pelo ícone.

## Verificar antes de entregar

```bash
./gradlew build
unzip -l build/libs/*.jar | grep -E "lang/|models/|blockstates/|loot_table|tags/"
```

E em jogo:

```text
[ ] todo item/bloco tem nome traduzido (nada de "block.meumod.foo")
[ ] todo bloco tem textura (nada de cubo preto e rosa)
[ ] todo bloco dropa alguma coisa
[ ] a ferramenta correta funciona
[ ] recipes aparecem no livro de receitas
[ ] o conteúdo aparece em alguma aba do criativo
```

O último é esquecido com frequência: conteúdo registrado sem item group existe,
funciona, e é invisível para o jogador.

## Checklist

```text
[ ] lang para todo conteúdo registrado
[ ] modelo + textura para item e bloco
[ ] blockstate para bloco
[ ] loot table para bloco
[ ] tags de mineração
[ ] recipe, se aplicável
[ ] item group
[ ] renderer no cliente, para entidade/block entity
[ ] caminhos conferidos NA SUA VERSÃO
[ ] resources presentes no jar final
```
