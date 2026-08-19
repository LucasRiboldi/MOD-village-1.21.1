# Os schemas das construções da colônia

As plantas que o construtor sabe levantar, no formato do **bloco de
estrutura** do próprio Minecraft (`.nbt`).

## Onde isto fica, e por que aqui

`data/villagecolony/structure/<caminho>.nbt` é o caminho de dados do
1.21.1 — pasta `structure`, no singular, como o próprio jogo usa em
`data/minecraft/structure/village/plains/houses/`. Estar aqui faz o
`StructureTemplateManager` do servidor achar o arquivo sozinho, sem
código de carregamento nenhum: o mod pergunta por
`villagecolony:houses/small_house` e o jogo entrega.

É o mesmo caminho por onde um datapack entraria. Quem quiser trocar uma
planta sem recompilar o mod põe um arquivo de mesmo nome num datapack, e
ele vence.

## Como fazer um schema novo

1. No jogo, com um **bloco de estrutura** (`/give @s structure_block`),
   marque a área e salve.
2. O arquivo sai em `saves/<mundo>/generated/minecraft/structure/`.
3. Copie para cá com um nome curto e descritivo.
4. Registre o id em `StructureBlueprintReader`, e some a lista de
   materiais dele com `HouseBillOfMaterialsGameTest` **antes** de mandar
   a colônia construí-lo.

## O que o leitor descarta do arquivo

Ver `StructureBlueprintReader`. Em resumo: o ar não vira bloco a pôr;
blocos de estrutura e de jigsaw do gerador ficam de fora, porque pô-los
deixaria bloco de comando na vila do jogador; e a metade de cima de
porta e cama é descartada, porque quem a completa é o construtor.

## O que há aqui hoje

### `small_house.nbt`

A casa pequena de vila de planície do próprio jogo
(`plains_small_house_1`, DataVersion 3952), 7 × 7 × 7.

**Ela ainda não é a obra da colônia**, e a razão está na Regra 13:

```text
149 blocos, 8 tipos
 49 oak_stairs        tábua      ✅ desde a Regra 10, o construtor fabrica
 43 cobblestone       minerar    ❌
 33 oak_planks        tronco     ✅
 16 stripped_oak_log  machado    ❌ não é receita, é uso de ferramenta
  3 glass_pane        fundir     ❌
  3 wall_torch        carvão     ❌
  1 white_bed         lã         ❌ (não trava — é mobília, Regra 21)
  1 oak_door          tábua      ✅
```

Sessenta e cinco blocos pedem cadeias que a colônia não tem. Pela
segunda metade da Regra 13, isso não a torna impossível: o que a colônia
não produz, **o jogador guarda no baú**, e o construtor tira dali sem
perguntar de onde veio. O que ela exige é essa combinação, e por isso
trocar a obra padrão é decisão do autor, não consequência de o arquivo
estar nesta pasta.
