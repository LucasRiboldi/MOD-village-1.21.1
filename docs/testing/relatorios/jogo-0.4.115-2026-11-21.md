# Relatório do jogo — 2026-11-21 15:23

## Achados, do mais grave ao menos

1. BUILDER: REGRA 51 (37% ocioso+bloqueado)
2. BUILDER: TRAVAMENTO (13% bloqueado+encalhado)
3. CARPENTER: FLUXO (61% sem trabalhar)
4. CARPENTER: REGRA 51 (46% ocioso+bloqueado)
5. MASON: FLUXO (63% sem trabalhar)
6. MASON: REGRA 51 (41% ocioso+bloqueado)
7. MINER: FLUXO (44% sem trabalhar)
8. SHEPHERD: REGRA 51 (26% ocioso+bloqueado)
9. SMELTER: FLUXO (44% sem trabalhar)
10. SMELTER: REGRA 51 (29% ocioso+bloqueado)
11. aldeão 6e82ff92 (NONE) ficou 1 s sem cama
12. aldeão d75b122b (CARPENTER) ficou 6 s sem cama
13. item carrot: +0 por fora, 207 recusado no baú
14. item dirt: +0 por fora, 45 recusado no baú
15. item wheat_seeds: +0 por fora, 430 recusado no baú
16. item white_carpet: +0 por fora, 3 recusado no baú
17. item white_wool: +0 por fora, 58 recusado no baú
18. bloco birch_planks: DESCER
19. bloco blast_furnace: SUBIR
20. bloco bone_meal: SUBIR
21. bloco clay: FALTA CRU (coleta)
22. bloco clay_ball: FALTA CRU (coleta)
23. bloco cobblestone: FALTA CRU (coleta)
24. bloco cobblestone_slab: DESCER
25. bloco farmland: FALTA CRU (coleta)
26. bloco glass_pane: SUBIR
27. bloco gray_stained_glass_pane: DESCER
28. bloco light_gray_dye: SUBIR
29. bloco light_gray_stained_glass_pane: DESCER
30. bloco light_gray_wool: DESCER

… e mais 26.

## Tempo por ofício

```text
8 janelas de 5 min
BUILDER     work 45%  walk 18%  wait 0%  blocked 13%  idle 24%  stranded 0%  REGRA 51 (37% ocioso+bloqueado); TRAVAMENTO (13% bloqueado+encalhado)
CARPENTER   work 17%  walk 21%  wait 15%  blocked 0%  idle 46%  stranded 0%  REGRA 51 (46% ocioso+bloqueado); FLUXO (61% sem trabalhar)
FARMER      work 89%  walk 7%  wait 0%  blocked 1%  idle 3%  stranded 0%  
GROUNDSKEEPERwork 65%  walk 33%  wait 0%  blocked 0%  idle 2%  stranded 0%  
LUMBERJACK  work 13%  walk 73%  wait 0%  blocked 1%  idle 13%  stranded 0%  
MASON       work 14%  walk 23%  wait 22%  blocked 0%  idle 41%  stranded 0%  REGRA 51 (41% ocioso+bloqueado); FLUXO (63% sem trabalhar)
MINER       work 22%  walk 35%  wait 34%  blocked 0%  idle 10%  stranded 0%  FLUXO (44% sem trabalhar)
SHEPHERD    work 3%  walk 70%  wait 2%  blocked 10%  idle 16%  stranded 0%  REGRA 51 (26% ocioso+bloqueado)
SMELTER     work 16%  walk 41%  wait 15%  blocked 1%  idle 28%  stranded 0%  REGRA 51 (29% ocioso+bloqueado); FLUXO (44% sem trabalhar)
```

## Tarefas

```text
Tarefas pelo número (Regra 344): segundos desde que foi pega, mediana / p95

tipo                        qtd  feitas  soltas      começo     1ª peça         fim
-----------------------------------------------------------------------------------
CRAFT_STONE_MATERIAL         41      40       0       22/36       10/24       25/51
CRAFT_WOOD_MATERIAL          28      27       0      25/213        6/20      31/215
COLLECT_SURFACE_RESOURCE     18      16       2      11/119        5/15      11/119
CRAFT_METAL_MATERIAL         18      17       0       29/29        5/10       30/33
SMELT_MATERIAL               17      12       5       10/16         1/2       12/82
BUILD                         9       8       0         0/0       5/137     171/983
COLLECT_STONE                 3       1       2       86/86         5/5    300/2284
COLLECT_WOOD                  2       1       0         0/0         5/5   2425/2425
COLLECT_FOOD                  2       0       1         0/0         3/5       64/90
COLLECT_WOOL                  1       0       1         -/-         0/0   2650/2650
CRAFT_TEXTILE_MATERIAL        1       1       0       18/18         5/5       20/20

Por que foram soltas (os 3 motivos mais comuns de cada tipo):
  COLLECT_SURFACE_RESOURCE     2× nothing in the whole radius
  COLLECT_WOOL                 1× RESERVED
  COLLECT_FOOD                 1× could not reach the crop
  COLLECT_STONE                2× RESERVED
  SMELT_MATERIAL               2× none of N colony chests had minecraft:deepslate_iron_ore or minecraft:iron_ore or minecraf
  SMELT_MATERIAL               1× no sand in any chest — the smelter goes to gather it
  SMELT_MATERIAL               1× no raw for the furnace in any chest — the mason makes it; the smelter helps elsewhere
```

## Linha do tempo dos aldeões

```text
Linha do tempo por aldeão (Regra 344): parte de cada estado e os trechos parados mais longos

aldeão    ofício           work     walk     wait  blocked     idle stranded      off   (% do tempo medido)
-----------------------------------------------------------------------------------------------------------
cd8ea76a  BUILDER           22%       9%       0%       5%      11%       0%      16%
d75b122b  CARPENTER         10%       9%       7%       0%      22%       0%      15%
3fb4c09e  FARMER            42%       3%       0%       0%       1%       0%      16%
6e82ff92  GROUNDSKEEPER      21%      12%       0%       0%       0%       0%      28%
de2796f6  LUMBERJACK         3%      38%       0%       0%       6%       0%      15%
3074e209  MASON              7%      13%       9%       0%      19%       0%      15%
8a40378f  MINER             11%      17%      15%       0%       5%       0%      16%
139d8a27  NONE               0%       0%       0%       0%      20%       0%      31%
811729e9  NONE               0%       0%       0%       0%      25%       0%      31%
ae8c8839  NONE               0%       0%       0%       0%      33%       0%      28%
bd5fdc70  NONE               0%       0%       0%       0%      14%       0%      48%
08c5b3c7  SHEPHERD           0%      34%       1%       5%       8%       0%      15%
d80faa43  SMELTER            5%      22%       8%       0%      12%       0%      15%

Trechos parados mais longos:
  cd8ea76a  BUILDER         80s IDLE      em -4,64,12         destino=-4, 65, 15 | para WALK destino=-9, 66, 15
  cd8ea76a  BUILDER         73s IDLE      em 40,72,-48        | para BLOCKED BUILD:OAK_PLANKS
  cd8ea76a  BUILDER         65s IDLE      em -12,66,12        
  d75b122b  CARPENTER      432s IDLE      em -2,63,-19        
  d75b122b  CARPENTER       46s WAIT      em -48,69,-26       CRAFT_WOOD_MATERIAL:OAK_SLAB
  d75b122b  CARPENTER       45s IDLE      em 2,63,-26         | para WAIT CRAFT_WOOD_MATERIAL:DIRT
  3fb4c09e  FARMER          25s IDLE      em 1,64,-27         | para WORK destino=-1, 64, -26
  3fb4c09e  FARMER          20s IDLE      em -35,70,-33       destino=-42, 69, -35 | para WALK destino=-22, 69, -25
  3fb4c09e  FARMER          12s BLOCKED   em 0,65,-29         COLLECT_FOOD:WHEAT destino=-3, 64, -28 | para IDLE destino=7, 63, -27
  de2796f6  LUMBERJACK      61s IDLE      em -55,69,-15       destino=-39, 71, -21 | para WALK destino=-71, 71, -18
  de2796f6  LUMBERJACK      60s IDLE      em 5,63,61          | para WALK destino=-71, 70, -18
  de2796f6  LUMBERJACK      30s IDLE      em -57,68,-19       | para WALK destino=-71, 71, -18
  3074e209  MASON          269s IDLE      em -39,74,-20       
  3074e209  MASON          109s IDLE      em -41,69,-11       | para WALK destino=-4, 66, 22
  3074e209  MASON           68s IDLE      em -27,69,-25       | para WAIT CRAFT_STONE_MATERIAL:SAND
  8a40378f  MINER          119s IDLE      em 4,70,-69         destino=7, 69, -74 | para WALK destino=7, 69, -74
  8a40378f  MINER           91s WAIT      em -26,65,-62       COLLECT_STONE:COBBLESTONE | para WALK destino=-23, 65, -60
  8a40378f  MINER           53s WAIT      em 29,73,-51        COLLECT_STONE:COAL | para WALK destino=28, 73, -51
  139d8a27  NONE           500s IDLE      em 10,64,13         
  811729e9  NONE           500s IDLE      em 10,63,12         
  811729e9  NONE           174s IDLE      em 7,63,64          | para OFF destino=1, 63, 61
  ae8c8839  NONE           500s IDLE      em 2,63,15          destino=0, 64, 12 | para OFF destino=7, 66, 13
  ae8c8839  NONE           500s IDLE      em 9,63,13          
  bd5fdc70  NONE           211s IDLE      em -32,66,-13       destino=-32, 66, -13
  08c5b3c7  SHEPHERD       118s IDLE      em -3,73,33         | para WALK destino=-10, 71, 40
  08c5b3c7  SHEPHERD       108s BLOCKED   em 6,73,-51         HELP | para OFF destino=5, 65, -30
  08c5b3c7  SHEPHERD        91s IDLE      em -1,64,88         destino=13, 65, 85 | para WALK destino=-23, 66, 93
  d80faa43  SMELTER         61s IDLE      em -50,68,-35       
  d80faa43  SMELTER         60s IDLE      em 1,65,20          | para WALK destino=0, 66, 22
  d80faa43  SMELTER         46s WAIT      em 65,60,87         CRAFT_METAL_MATERIAL:DIRT
```

## Diário de ações

```text
Diário de ações: 50.6 min de jogo

profissão   aldeões  pegas feitas soltas cancel. conclusão  mediana  ações por minuto
--------------------------------------------------------------------------------------------------------------
BUILDER           1     10      8      1       0       80%     117s  PLACED 12.8, SET_ASIDE 2.5, STATE 0.9
CARPENTER         1     28     27      0       0       96%      31s  STATE 1.2, CRAFTED 3.6, PICKED_UP 0.1, CLEARED_STRAY 0.0
FARMER            1      3      0      1       1        0%        -  BONE_MEAL 25.4, HARVESTED 18.9, STATE 0.3, GAVE_UP 0.0
GROUNDSKEEPER       1      0      0      0       0         -        -  PAVED 4.1, LEVELLED 8.6, STATE 0.4, PICKED_UP 0.1
LUMBERJACK        1      4      1      2       0       25%    1165s  PICKED_UP 2.7, CLEARED_LEAF 0.9, FELLED_TREE 5.3, CLEARED_LEAVES 0.7
MASON             1     41     40      0       0       98%      25s  STATE 1.0, CRAFTED 2.5, PICKED_UP 0.0
MINER             1      6      1      4       0       17%      86s  MINED 11.6, STATE 0.9, PICKED_UP 0.2, MOVED_HOME 0.0
SHEPHERD          1      5      1      4       0       20%      20s  SHEARED 1.7, STATE 0.4, PICKED_UP 0.2, BUTCHERED 0.2
SMELTER           1     64     45     18       0       70%      14s  SMELTED 2.8, STATE 1.1, CRAFTED 1.2, CLEARED_LEAF 0.0

Por aldeão (mais tarefas soltas primeiro):
  d80faa43 SMELTER    feitas  45, soltas  18  SMELTED 140, STATE 56, CRAFTED 26  | soltou: SMELT_MATERIAL iron_ingot ×5, SMELT_MATERIAL glass ×5  | aberta: CRAFT_METAL_MATERIAL dirt: 105s, sem progresso 9s
  8a40378f MINER      feitas   1, soltas   4  MINED 569, STATE 47, PICKED_UP 8  | soltou: COLLECT_STONE coal ×3, COLLECT_STONE cobblestone ×1  | aberta: COLLECT_STONE cobblestone: 167s, sem progresso 5s
  08c5b3c7 SHEPHERD   feitas   1, soltas   4  SHEARED 41, STATE 19, PICKED_UP 9  | soltou: COLLECT_WOOL white_wool ×4
  de2796f6 LUMBERJACK feitas   1, soltas   2  PICKED_UP 110, CLEARED_LEAF 48, FELLED_TREE 43  | soltou: COLLECT_WOOD oak_log ×2  | aberta: COLLECT_WOOD oak_log: 531s, sem progresso 6s
  cd8ea76a BUILDER    feitas   8, soltas   1  PLACED 650, SET_ASIDE 129, STATE 47  | soltou: BUILD oak_planks ×1  | aberta: BUILD oak_planks: 137s, sem progresso 0s
  3fb4c09e FARMER     feitas   0, soltas   1  BONE_MEAL 1287, HARVESTED 555, STATE 15  | soltou: COLLECT_FOOD wheat ×1
  d75b122b CARPENTER  feitas  27, soltas   0  STATE 59, CRAFTED 29, PICKED_UP 3  | aberta: CRAFT_WOOD_MATERIAL oak_stairs: 137s, sem progresso 14s
  3074e209 MASON      feitas  40, soltas   0  STATE 50, CRAFTED 46, PICKED_UP 1  | aberta: CRAFT_STONE_MATERIAL sandstone: 159s, sem progresso 6s
  6e82ff92 GROUNDSKEEPER feitas   0, soltas   0  PAVED 207, LEVELLED 28, STATE 20

Motivos de recusa e desistência (os mais frequentes por profissão):
  BUILDER     129× SET_ASIDE: no material
  FARMER        1× GAVE_UP: stationary; farmer at N, N, N
  FARMER        1× TASK_RELEASED: could not reach the crop
  LUMBERJACK    1× TASK_RELEASED: has not moved a block in N work ticks
  LUMBERJACK    1× TASK_RELEASED: got no closer to the tree for N work ticks
  SHEPHERD      2× TASK_RELEASED: could not reach the sheep
  SHEPHERD      1× GAVE_UP: approach timeout; shepherd at N, N, N
  SHEPHERD      1× GAVE_UP: stationary; shepherd at N, N, N
  SMELTER       5× TASK_RELEASED: none of N colony chests had minecraft:deepslate_iron_ore or minecraft:iron_ore or minecraf
  SMELTER       5× TASK_RELEASED: no sand in any chest — the smelter goes to gather it
  SMELTER       5× TASK_RELEASED: no raw for the furnace in any chest — the mason makes it; the smelter helps elsewhere
  SMELTER       2× TASK_RELEASED: nothing in the whole radius
  SMELTER       1× TASK_RELEASED: no clay_ball in any chest — the smelter goes to gather it

Por código de bloqueio (Regra 112):
  BUILDER     129× NO_RESOURCE
  BUILDER       1× UNCLASSIFIED
  FARMER        1× UNCLASSIFIED
  FARMER        1× NO_PATH
  LUMBERJACK    1× NO_PROGRESS
  LUMBERJACK    1× NO_PATH
  MINER         4× TARGET_RESERVED
  SHEPHERD      2× UNCLASSIFIED
  SHEPHERD      2× NO_PATH
  SHEPHERD      2× TARGET_RESERVED
  SMELTER      13× NO_RESOURCE
  SMELTER       5× UNCLASSIFIED
Por item (Regra 344): feitas, e os desfechos mais comuns:
  stone                        feitas    72
        48× SMELTED: -
         3× CRAFTED: value goods
  oak_stairs                   feitas    21
        43× SET_ASIDE: no material
         4× CRAFTED: no logs left in the colony chests
         3× CRAFTED: made the oak_stairs it was asked
  cobblestone                  feitas     9
        25× SET_ASIDE: no material
         4× CRAFTED: nothing this workshop can make now
         3× CRAFTED: no logs left in the colony chests
  smooth_stone                 feitas    32
        32× SMELTED: -
  oak_planks                   feitas   123
        21× SET_ASIDE: no material
         4× CRAFTED: no logs left in the colony chests
         3× CRAFTED: no room in the personal chest for minecraft:oak_planks
  glass                        feitas    26
        26× SMELTED: -
  oak_log                      feitas     0
        22× SET_ASIDE: no material
  brick                        feitas    16
        16× SMELTED: -
  flower_pot                   feitas     2
        13× CRAFTED: nothing this workshop can make now
  smooth_sandstone             feitas    10
        10× SMELTED: -
  oak_trapdoor                 feitas     0
         8× SET_ASIDE: no material
  charcoal                     feitas     8
         8× SMELTED: -
  stone_bricks                 feitas    26
         4× CRAFTED: made the stone_bricks it was asked
         2× CRAFTED: nothing this workshop can make now
  torch                        feitas     1
         4× SET_ASIDE: no material
         1× CRAFTED: stocked N of torch
  sand                         feitas    32
         4× CRAFTED: crushed the sand it was asked
```

## Balanço dos itens

```text
Balanço dos itens (Regra 344): recusados e mudanças por fora primeiro

item                         dias  entrou    saiu  recusado  por fora  estoque
------------------------------------------------------------------------------
wheat_seeds                     3     205       8       430         0      197
carrot                          3     260      68       207         0      192
white_wool                      3     196       7        58         0      189
dirt                            3     799       0        45         0      799
white_carpet                    1       3       0         3         0        3
oak_planks                      3     747     558         0         0      189
cobblestone                     3     497     497         0         0        0
wheat                           3     371     363         0         0        8
oak_log                         3     298     259         0         0       39
stone                           3     283     184         0         0       99
oak_stairs                      3     208     192         0         0       16
grass_block                     3     397       0         0         0      397
sand                            3     232     148         0         0       84
bread                           3     121     125         0         0        2
stick                           3     127     110         0         0       17
stone_bricks                    3     152      57         0         0       95
oak_fence                       1      93      57         0         0       36
diorite                         3      65      65         0         0        0
gravel                          1      64      64         0         0        0
glass_pane                      3      65      58         0         0        7
birch_planks                    1      72      14         0         0       58
oak_sapling                     3      80       6         0         0       78
sandstone                       3      30      30         0         0        0
potato                          3      57       0         0         0       60
glass                           3      28      24         0         0        4
smooth_stone                    2      32      20         0         0       12
birch_log                       2      28      23         0         0        5
raw_copper                      2      24      22         0         0        2
prismarine_crystals             1      20      20         0         0        0
torch                           3      22      15         0         0        7
clay_ball                       1      16      16         0         0        0
prismarine_shard                1      16      16         0         0        0
brick                           1      16      14         0         0        2
light_gray_wool                 3      21       6         0         0       15
cobblestone_slab                1      24       0         0         0       24
smooth_stone_slab               1      24       0         0         0       24
cobblestone_stairs              3      13      10         0         0        3
oak_slab                        2      18       3         0         0       15
oak_door                        2      15       4         0         0       11
egg                             3      16       0         0         0       16
mutton                          2      12       4         0         0        8
white_stained_glass_pane        1      16       0         0         0       16
iron_nugget                     1       9       6         0         0        3
charcoal                        2       8       4         0         0        4
ladder                          1      12       0         0         0       12
oak_trapdoor                    3       6       6         0         0        0
black_wool                      2       7       4         0         0        3
smooth_sandstone                1      10       0         0         0       10
gray_stained_glass_pane         1       8       0         0         0        8
iron_ingot                      2       4       4         0         0        0
light_gray_stained_glass_pane    1       8       0         0         0        8
oak_fence_gate                  1       4       4         0         0        0
orange_stained_glass_pane       1       8       0         0         0        8
raw_iron                        1       4       4         0         0        0
sandstone_stairs                2       8       0         0         0        8
yellow_stained_glass_pane       1       8       0         0         0        8
cobblestone_wall                2       6       1         0         0        5
yellow_dye                      2       3       3         0         0        0
orange_dye                      3       3       2         0         0        1
white_dye                       3       3       2         0         0        1
apple                           2       4       0         0         0        5
bricks                          1       2       2         0         0        0
chicken                         2       2       2         0         0        0
chiseled_stone_bricks           1       4       0         0         0        4
cooked_mutton                   2       4       0         0         0        4
copper_ingot                    2       4       0         0         0        4
cracked_stone_bricks            1       4       0         0         0        4
gray_dye                        2       2       2         0         0        0
light_gray_dye                  3       2       2         0         0        0
polished_diorite                1       4       0         0         0        4
sea_lantern                     1       4       0         0         0        4
chain                           1       3       0         0         0        3
feather                         1       3       0         0         0        3
hay_block                       1       3       0         0         0        3
light_gray_carpet               1       3       0         0         0        3
purple_dye                      2       2       1         0         0        1
black_dye                       1       1       1         0         0        0
blast_furnace                   1       1       1         0         0        0
blue_dye                        1       1       1         0         0        0
blue_wool                       1       1       1         0         0        0
brown_dye                       1       1       1         0         0        0
brown_wool                      1       1       1         0         0        0
cooked_chicken                  1       2       0         0         0        2
cornflower                      1       2       0         0         0        2
cyan_dye                        1       1       1         0         0        0
cyan_wool                       1       1       1         0         0        0
flower_pot                      1       2       0         0         0        2
gray_wool                       1       1       1         0         0        0
green_dye                       1       1       1         0         0        0
green_wool                      1       1       1         0         0        0
light_blue_dye                  1       1       1         0         0        0
light_blue_wool                 1       1       1         0         0        0
lime_dye                        1       1       1         0         0        0
magenta_dye                     1       1       1         0         0        0
pink_dye                        1       1       1         0         0        0
raw_copper_block                1       2       0         0         0        2
red_dye                         1       1       1         0         0        0
stonecutter                     1       1       1         0         0        0
white_bed                       1       1       1         0         0        0
yellow_wool                     2       2       0         0         0        2
azure_bluet                     1       1       0         0         0        1
dandelion                       1       1       0         0         0        2
lime_wool                       1       1       0         0         0        1
magenta_wool                    1       1       0         0         0        1
oak_pressure_plate              1       1       0         0         0        1
orange_wool                     1       1       0         0         0        1
pink_wool                       1       1       0         0         0        1
purple_wool                     1       1       0         0         0        1
red_wool                        1       1       0         0         0        1
```

## Blocos e travas

```text
Blocos (Regra 342): os que pedem mudança primeiro

bloco                       classe      reserva     estoque  posto esperou surgiu   sugestão
--------------------------------------------------------------------------------------------
bone_meal                   FABRICADO         4         0-0      0       0   1408   SUBIR
white_wool                  VILA             12      27-189      0       0    134   DESCER
cobblestone                 CRU             256       0-186    159      25     54   FALTA CRU (coleta)
oak_log                     CRU             256        0-39     73      22     39   FALTA CRU (coleta)
oak_stairs                  VILA             12        0-72    192      43     12   SUBIR
oak_planks                  VILA             12       6-189     50      21     27   SUBIR
clay_ball                   CRU             256         0-0      0       0     16   FALTA CRU (coleta)
farmland                    CRU             256         0-0      0       0     12   FALTA CRU (coleta)
wheat                       CRU             256         0-0      0       0     12   FALTA CRU (coleta)
torch                       VILA             12         0-7      7       4      6   SUBIR
oak_trapdoor                VILA             12         0-2      6       8      0   SUBIR
clay                        CRU             256         0-0      4       0      4   FALTA CRU (coleta)
glass_pane                  VILA             12        7-16     10       1      1   SUBIR
oak_door                    VILA             12        0-11      4       2      0   SUBIR
blast_furnace               ESPECIAL          1         0-0      1       1      1   SUBIR
stonecutter                 ESPECIAL          1         0-0      1       1      1   SUBIR
white_bed                   VILA             12         0-0      1       1      0   SUBIR
light_gray_dye              FABRICADO         4         0-0      0       0      1   SUBIR
oak_sapling                 CRU             256        7-78      0       0      1   FALTA CRU (coleta)
orange_dye                  FABRICADO         4         0-0      0       0      1   SUBIR
water                       FABRICADO         4         0-0      0       0      1   SUBIR
white_dye                   FABRICADO         4         0-0      0       0      1   SUBIR
yellow_dye                  FABRICADO         4         0-0      0       0      1   SUBIR
birch_planks                FABRICADO         4       58-58      0       0      0   DESCER
cobblestone_slab            ESPECIAL          1       24-24      0       0      0   DESCER
gray_stained_glass_pane     FABRICADO         4         8-8      0       0      0   DESCER
light_gray_stained_glass_paneFABRICADO         4         8-8      0       0      0   DESCER
light_gray_wool             ESPECIAL          1        4-15      0       0      0   DESCER
orange_stained_glass_pane   ESPECIAL          1         8-8      0       0      0   DESCER
smooth_stone_slab           VILA             12       24-24      0       0      0   DESCER
white_stained_glass_pane    VILA             12       16-16      0       0      0   DESCER
oak_fence                   VILA             12       36-36     57       0      0   ok
stone_bricks                ESPECIAL          1       4-125     49       0      0   ok
cobblestone_stairs          VILA             12        0-11     10       0      0   ok
smooth_stone                VILA             12       12-12      8       0      0   ok
wall_torch                  FABRICADO         4         0-0      8       0      0   ok
oak_fence_gate              ESPECIAL          1         0-0      4       0      0   ok
oak_slab                    VILA             12       15-18      3       0      0   ok
bricks                      ESPECIAL          1         0-0      2       0      0   ok
cobblestone_wall            VILA             12         5-6      1       0      0   ok
azure_bluet                 CRU             256         1-1      0       0      0   ok
birch_log                   CRU             256         5-5      0       0      0   ok
black_wool                  FABRICADO         4         3-3      0       0      0   ok
carrot                      CRU             256     123-192      0       0      0   ok
chain                       FABRICADO         4         3-3      0       0      0   ok
chiseled_stone_bricks       FABRICADO         4         4-4      0       0      0   ok
cornflower                  CRU             256         2-2      0       0      0   ok
cracked_stone_bricks        FABRICADO         4         4-4      0       0      0   ok
dandelion                   CRU             256         2-2      0       0      0   ok
diorite                     CRU             256       42-65      0       0      0   ok
dirt                        CRU             256      54-799      0       0      0   ok
flower_pot                  FABRICADO         4         2-2      0       0      0   ok
glass                       FABRICADO         4         2-6      0       0      0   ok
grass_block                 CRU             256      18-397      0       0      0   ok
hay_block                   VILA             12         3-3      0       0      0   ok
ladder                      VILA             12       12-12      0       0      0   ok
light_gray_carpet           FABRICADO         4         3-3      0       0      0   ok
lime_wool                   FABRICADO         4         1-1      0       0      0   ok
magenta_wool                FABRICADO         4         1-1      0       0      0   ok
oak_pressure_plate          ESPECIAL          1         1-1      0       0      0   ok
orange_wool                 FABRICADO         4         1-1      0       0      0   ok
pink_wool                   FABRICADO         4         1-1      0       0      0   ok
polished_diorite            FABRICADO         4         4-4      0       0      0   ok
potato                      CRU             256       18-60      0       0      0   ok
purple_wool                 FABRICADO         4         1-1      0       0      0   ok
raw_copper_block            FABRICADO         4         2-2      0       0      0   ok
red_wool                    FABRICADO         4         1-1      0       0      0   ok
sand                        CRU             256       32-84      0       0      0   ok
sandstone_stairs            ESPECIAL          1         1-8      0       0      0   ok
sea_lantern                 FABRICADO         4         4-4      0       0      0   ok
smooth_sandstone            VILA             12       10-10      0       0      0   ok
stone                       CRU             256       80-99      0       0      0   ok
wheat_seeds                 CRU             256     170-197      0       0      0   ok
white_carpet                VILA             12         3-3      0       0      0   ok
yellow_stained_glass_pane   VILA             12         8-8      0       0      0   ok
yellow_wool                 VILA             12         1-2      0       0      0   ok

Travas de cru (Regra 343)

trava          limite  consultas  barradas  menor   sugestão
------------------------------------------------------------
bone_meal           4       1383      1383      0   SEGURA DEMAIS (descer)
mine_fill          16         47        25    -24   SEGURA DEMAIS (descer)
smelter_raw         4        563       561      0   SEGURA DEMAIS (descer)
upkeep             36          1         1     24   SEGURA DEMAIS (descer)
value_raw          11        177       123      0   SEGURA DEMAIS (descer)
wheat               8        557        58      1   ok
wool               16        124         0     19   NÃO AGE (pode sair)
```

## Interferência do jogador

```text
Interferência do jogador (Regra 345)

Para corrigir — o jogador insistiu:
  nenhuma reafirmação

O jogador desfez o mod (sistema, alvo):
  nada

O mod desfez o jogador (sistema, de → para):
  nada

Edições do jogador por alvo:
     5× broke em rua
     3× broke em vanilla
     3× placed em outro
     2× placed em vanilla
     2× placed em construcao
     1× broke em natural
```
