# Catálogo das estruturas da colônia

> Gerado por `python scripts/structure_catalog.py` — não edite à mão; rode o
> script de novo depois de pôr um modelo nesta pasta. Ver `LEIAME.md`.

**Modelo do mod** = existe `colony/override/<caminho do jogo>.nbt`, e a colônia
constrói ele no lugar da estrutura do jogo.

## Casa de cada ofício

Arquivo: `colony/<estilo>/trade_<oficio>.nbt` (só aquele estilo) ou
`colony/trade_<oficio>.nbt` (todos os estilos). Com ele, a casa do ofício da
colônia vem antes das do jogo (Regra 49).

| Ofício | Arquivo | Todos os estilos | plains | desert | savanna | taiga | snowy | Casa do jogo que serve hoje |
|---|---|---|---|---|---|---|---|---|
| mineiro | `trade_miner.nbt` | — | — | — | — | — | — | tool_smith |
| lenhador | `trade_lumberjack.nbt` | — | — | — | — | — | — | nenhuma |
| pedreiro | `trade_mason.nbt` | — | — | — | — | — | — | mason |
| fundidor | `trade_smelter.nbt` | — | — | — | — | — | — | armorer, weaponsmith, weapon_smith |
| carpinteiro | `trade_carpenter.nbt` | — | — | — | — | — | — | fletcher |
| fazendeiro | `trade_farmer.nbt` | — | — | — | — | — | — | farm |
| pastor | `trade_shepherd.nbt` | — | — | — | — | — | — | shepherd, animal_pen |
| construtor | `trade_builder.nbt` | — | — | — | — | — | — | nenhuma |

## Estruturas do jogo que a colônia constrói — 143, 0 com modelo do mod

### plains (34)

| Estrutura | Tipo | Ofício | Modelo do mod |
|---|---|---|---|
| `village/plains/houses/plains_small_house_1` | moradia | — | — |
| `village/plains/houses/plains_small_house_2` | moradia | — | — |
| `village/plains/houses/plains_small_house_3` | moradia | — | — |
| `village/plains/houses/plains_small_house_4` | moradia | — | — |
| `village/plains/houses/plains_small_house_5` | moradia | — | — |
| `village/plains/houses/plains_small_house_6` | moradia | — | — |
| `village/plains/houses/plains_small_house_7` | moradia | — | — |
| `village/plains/houses/plains_small_house_8` | moradia | — | — |
| `village/plains/houses/plains_medium_house_1` | moradia | — | — |
| `village/plains/houses/plains_medium_house_2` | moradia | — | — |
| `village/plains/houses/plains_big_house_1` | moradia | — | — |
| `village/plains/houses/plains_armorer_house_1` | oficina | fundidor | — |
| `village/plains/houses/plains_butcher_shop_1` | oficina | — | — |
| `village/plains/houses/plains_butcher_shop_2` | oficina | — | — |
| `village/plains/houses/plains_cartographer_1` | oficina | — | — |
| `village/plains/houses/plains_fisher_cottage_1` | oficina | — | — |
| `village/plains/houses/plains_fletcher_house_1` | oficina | carpinteiro | — |
| `village/plains/houses/plains_library_1` | oficina | — | — |
| `village/plains/houses/plains_library_2` | oficina | — | — |
| `village/plains/houses/plains_masons_house_1` | oficina | pedreiro | — |
| `village/plains/houses/plains_shepherds_house_1` | oficina | pastor | — |
| `village/plains/houses/plains_tannery_1` | oficina | — | — |
| `village/plains/houses/plains_temple_3` | templo | — | — |
| `village/plains/houses/plains_temple_4` | templo | — | — |
| `village/plains/houses/plains_tool_smith_1` | oficina | mineiro | — |
| `village/plains/houses/plains_weaponsmith_1` | oficina | fundidor | — |
| `village/plains/houses/plains_small_farm_1` | roça | fazendeiro | — |
| `village/plains/houses/plains_large_farm_1` | roça | fazendeiro | — |
| `village/plains/houses/plains_animal_pen_1` | curral | pastor | — |
| `village/plains/houses/plains_animal_pen_2` | curral | pastor | — |
| `village/plains/houses/plains_animal_pen_3` | curral | pastor | — |
| `village/plains/houses/plains_stable_1` | estábulo | — | — |
| `village/plains/houses/plains_stable_2` | estábulo | — | — |
| `village/plains/plains_lamp_1` | poste | — | — |

### desert (29)

| Estrutura | Tipo | Ofício | Modelo do mod |
|---|---|---|---|
| `village/desert/houses/desert_small_house_1` | moradia | — | — |
| `village/desert/houses/desert_small_house_2` | moradia | — | — |
| `village/desert/houses/desert_small_house_3` | moradia | — | — |
| `village/desert/houses/desert_small_house_4` | moradia | — | — |
| `village/desert/houses/desert_small_house_5` | moradia | — | — |
| `village/desert/houses/desert_small_house_6` | moradia | — | — |
| `village/desert/houses/desert_small_house_7` | moradia | — | — |
| `village/desert/houses/desert_small_house_8` | moradia | — | — |
| `village/desert/houses/desert_medium_house_1` | moradia | — | — |
| `village/desert/houses/desert_medium_house_2` | moradia | — | — |
| `village/desert/houses/desert_armorer_1` | oficina | fundidor | — |
| `village/desert/houses/desert_butcher_shop_1` | oficina | — | — |
| `village/desert/houses/desert_cartographer_house_1` | oficina | — | — |
| `village/desert/houses/desert_fisher_1` | oficina | — | — |
| `village/desert/houses/desert_fletcher_house_1` | oficina | carpinteiro | — |
| `village/desert/houses/desert_library_1` | oficina | — | — |
| `village/desert/houses/desert_mason_1` | oficina | pedreiro | — |
| `village/desert/houses/desert_shepherd_house_1` | oficina | pastor | — |
| `village/desert/houses/desert_tannery_1` | oficina | — | — |
| `village/desert/houses/desert_temple_1` | templo | — | — |
| `village/desert/houses/desert_temple_2` | templo | — | — |
| `village/desert/houses/desert_tool_smith_1` | oficina | mineiro | — |
| `village/desert/houses/desert_weaponsmith_1` | oficina | fundidor | — |
| `village/desert/houses/desert_farm_1` | roça | fazendeiro | — |
| `village/desert/houses/desert_farm_2` | roça | fazendeiro | — |
| `village/desert/houses/desert_large_farm_1` | roça | fazendeiro | — |
| `village/desert/houses/desert_animal_pen_1` | curral | pastor | — |
| `village/desert/houses/desert_animal_pen_2` | curral | pastor | — |
| `village/desert/desert_lamp_1` | poste | — | — |

### savanna (28)

| Estrutura | Tipo | Ofício | Modelo do mod |
|---|---|---|---|
| `village/savanna/houses/savanna_small_house_1` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_2` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_3` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_4` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_5` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_6` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_7` | moradia | — | — |
| `village/savanna/houses/savanna_small_house_8` | moradia | — | — |
| `village/savanna/houses/savanna_medium_house_1` | moradia | — | — |
| `village/savanna/houses/savanna_medium_house_2` | moradia | — | — |
| `village/savanna/houses/savanna_armorer_1` | oficina | fundidor | — |
| `village/savanna/houses/savanna_butchers_shop_1` | oficina | — | — |
| `village/savanna/houses/savanna_cartographer_1` | oficina | — | — |
| `village/savanna/houses/savanna_fisher_cottage_1` | oficina | — | — |
| `village/savanna/houses/savanna_fletcher_house_1` | oficina | carpinteiro | — |
| `village/savanna/houses/savanna_library_1` | oficina | — | — |
| `village/savanna/houses/savanna_mason_1` | oficina | pedreiro | — |
| `village/savanna/houses/savanna_shepherd_1` | oficina | pastor | — |
| `village/savanna/houses/savanna_tannery_1` | oficina | — | — |
| `village/savanna/houses/savanna_temple_1` | templo | — | — |
| `village/savanna/houses/savanna_temple_2` | templo | — | — |
| `village/savanna/houses/savanna_tool_smith_1` | oficina | mineiro | — |
| `village/savanna/houses/savanna_weaponsmith_1` | oficina | fundidor | — |
| `village/savanna/houses/savanna_small_farm` | roça | fazendeiro | — |
| `village/savanna/houses/savanna_large_farm_1` | roça | fazendeiro | — |
| `village/savanna/houses/savanna_animal_pen_1` | curral | pastor | — |
| `village/savanna/houses/savanna_animal_pen_2` | curral | pastor | — |
| `village/savanna/savanna_lamp_post_01` | poste | — | — |

### taiga (26)

| Estrutura | Tipo | Ofício | Modelo do mod |
|---|---|---|---|
| `village/taiga/houses/taiga_small_house_1` | moradia | — | — |
| `village/taiga/houses/taiga_small_house_2` | moradia | — | — |
| `village/taiga/houses/taiga_small_house_3` | moradia | — | — |
| `village/taiga/houses/taiga_small_house_4` | moradia | — | — |
| `village/taiga/houses/taiga_small_house_5` | moradia | — | — |
| `village/taiga/houses/taiga_medium_house_1` | moradia | — | — |
| `village/taiga/houses/taiga_medium_house_2` | moradia | — | — |
| `village/taiga/houses/taiga_medium_house_3` | moradia | — | — |
| `village/taiga/houses/taiga_medium_house_4` | moradia | — | — |
| `village/taiga/houses/taiga_armorer_house_1` | oficina | fundidor | — |
| `village/taiga/houses/taiga_armorer_2` | oficina | fundidor | — |
| `village/taiga/houses/taiga_butcher_shop_1` | oficina | — | — |
| `village/taiga/houses/taiga_cartographer_house_1` | oficina | — | — |
| `village/taiga/houses/taiga_fisher_cottage_1` | oficina | — | — |
| `village/taiga/houses/taiga_fletcher_house_1` | oficina | carpinteiro | — |
| `village/taiga/houses/taiga_library_1` | oficina | — | — |
| `village/taiga/houses/taiga_masons_house_1` | oficina | pedreiro | — |
| `village/taiga/houses/taiga_shepherds_house_1` | oficina | pastor | — |
| `village/taiga/houses/taiga_tannery_1` | oficina | — | — |
| `village/taiga/houses/taiga_temple_1` | templo | — | — |
| `village/taiga/houses/taiga_tool_smith_1` | oficina | mineiro | — |
| `village/taiga/houses/taiga_weaponsmith_1` | oficina | fundidor | — |
| `village/taiga/houses/taiga_small_farm_1` | roça | fazendeiro | — |
| `village/taiga/houses/taiga_large_farm_1` | roça | fazendeiro | — |
| `village/taiga/houses/taiga_animal_pen_1` | curral | pastor | — |
| `village/taiga/taiga_lamp_post_1` | poste | — | — |

### snowy (26)

| Estrutura | Tipo | Ofício | Modelo do mod |
|---|---|---|---|
| `village/snowy/houses/snowy_small_house_1` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_2` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_3` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_4` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_5` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_6` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_7` | moradia | — | — |
| `village/snowy/houses/snowy_small_house_8` | moradia | — | — |
| `village/snowy/houses/snowy_medium_house_1` | moradia | — | — |
| `village/snowy/houses/snowy_medium_house_2` | moradia | — | — |
| `village/snowy/houses/snowy_armorer_house_1` | oficina | fundidor | — |
| `village/snowy/houses/snowy_butchers_shop_1` | oficina | — | — |
| `village/snowy/houses/snowy_cartographer_house_1` | oficina | — | — |
| `village/snowy/houses/snowy_fisher_cottage` | oficina | — | — |
| `village/snowy/houses/snowy_fletcher_house_1` | oficina | carpinteiro | — |
| `village/snowy/houses/snowy_library_1` | oficina | — | — |
| `village/snowy/houses/snowy_masons_house_1` | oficina | pedreiro | — |
| `village/snowy/houses/snowy_shepherds_house_1` | oficina | pastor | — |
| `village/snowy/houses/snowy_tannery_1` | oficina | — | — |
| `village/snowy/houses/snowy_temple_1` | templo | — | — |
| `village/snowy/houses/snowy_tool_smith_1` | oficina | mineiro | — |
| `village/snowy/houses/snowy_weapon_smith_1` | oficina | fundidor | — |
| `village/snowy/houses/snowy_farm_1` | roça | fazendeiro | — |
| `village/snowy/houses/snowy_animal_pen_1` | curral | pastor | — |
| `village/snowy/houses/snowy_animal_pen_2` | curral | pastor | — |
| `village/snowy/snowy_lamp_post_01` | poste | — | — |

## Estruturas que já são só da colônia

| Arquivo | Uso |
|---|---|
| `houses/big_house_mod.nbt` | a BigHouseMOD da fundação (Regra 35) |
| `houses/small_house.nbt` | casa pequena de teste |
