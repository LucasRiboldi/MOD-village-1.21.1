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

**É a obra da colônia em vila de planície desde 2026-08-19**, por
decisão do autor. Nos outros biomas continua a cabana do mod.

A porta dela fica a um bloco da parede oeste — e o encaixe de rua do
gerador, o *jigsaw*, está do mesmo lado. A colônia descobre isso sozinha
(`Blueprint.doorSide`) e **gira a planta inteira** para a porta cair na
rua, que é a Regra 17.

**O que isso custa**, e é preciso dizer:

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

Sessenta e dois blocos pedem cadeias que a colônia não tem. Pela segunda
metade da Regra 13, isso não a torna impossível: o que a colônia não
produz, **o jogador guarda no baú**, e o construtor tira dali sem
perguntar de onde veio.

Mas a consequência é real e o autor a aceitou: **esta casa não sobe
sozinha** como a cabana subia. Sem pedregulho num baú, a obra fica em
`WAITING_RESOURCES` e o relatório diz o que falta, uma peça por vez.

A cama e a tocha são exceção — elas são **mobília** pela Regra 21, e
mobília não segura a obra. A casa termina sem elas e elas entram quando
o material aparecer. Pedregulho não: parede segura a obra, porque casa
sem parede não é casa.

---

## O catálogo de estruturas

`data/villagecolony/catalog/vanilla_structures.json` lista os **1.180
ids** de estrutura que o jogo 1.21.1 traz — vilas de cinco biomas,
fortalezas, cidades antigas, câmaras de provação, e o resto.

**Só nomes.** Nenhum byte de arquivo da Mojang mora no repositório: o
jogo já traz as estruturas, e o construtor as lê por id com o mesmo
`StructureBlueprintReader` que lê esta pasta. A lista foi gerada da
pasta `minecraft-assets_structure/`, que fica fora do controle de versão
pelo motivo escrito no `.gitignore`.

É a base para o passo seguinte, que o autor pediu para deixar preparado:
**o construtor escolhendo entre muitas estruturas** em vez de uma só. O
que falta para isso não é a lista — é o critério de escolha (que casa,
para qual vila, em que ordem) e a conta de materiais de cada uma.
