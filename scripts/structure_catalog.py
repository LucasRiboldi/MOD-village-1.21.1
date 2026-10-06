#!/usr/bin/env python3
"""Gera o CATALOGO.md da pasta de modelos da colônia — 2026-10-02.

Lista, por estilo de vila, tudo o que a colônia pode construir hoje (a lista
``ALLOWED`` do ``VillageStructures``) e diz se já existe modelo próprio para
cada estrutura, e para a casa de cada ofício.

Uso (da raiz do projeto):
    python scripts/structure_catalog.py

Reescreve ``src/main/resources/data/villagecolony/structure/colony/CATALOGO.md``.
Rode de novo sempre que um modelo for adicionado.
"""

import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "src/main/java/com/villagecolony/fabric/integration/VillageStructures.java")
MODELS = os.path.join(ROOT, "src/main/resources/data/villagecolony/structure")
FOLDER = os.path.join(MODELS, "colony")
OUT = os.path.join(FOLDER, "CATALOGO.md")

PROFESSIONS = ["MINER", "LUMBERJACK", "MASON", "SMELTER", "CARPENTER", "FARMER", "SHEPHERD", "BUILDER"]
NAMES = {"MINER": "mineiro", "LUMBERJACK": "lenhador", "MASON": "pedreiro", "SMELTER": "fundidor",
         "CARPENTER": "carpinteiro", "FARMER": "fazendeiro", "SHEPHERD": "pastor", "BUILDER": "construtor"}
NAMED_PROFESSION_MODELS = {"barn_majest": "SHEPHERD", "storage_majest": "BUILDER"}

# Ofício de cada casa do jogo — ConstructionOrder.WORKSHOPS.
WORKSHOPS = [("FARMER", ["farm"]), ("SHEPHERD", ["shepherd", "animal_pen"]), ("MASON", ["mason"]),
             ("SMELTER", ["armorer", "weaponsmith", "weapon_smith"]), ("CARPENTER", ["fletcher"]),
             ("MINER", ["tool_smith"])]

KINDS = [("lamp", "poste"), ("animal_pen", "curral"), ("stable", "estábulo"), ("farm", "roça"),
         ("temple", "templo"), ("meeting_point", "praça"), ("accessory", "acessório")]
SHOPS = ["armorer", "butcher", "cartographer", "fisher", "fletcher", "library", "mason", "shepherd",
         "tannery", "tool_smith", "weaponsmith", "weapon_smith"]


def allowed():
    text = open(JAVA, encoding="utf-8").read()
    block = text[text.index("ALLOWED = Map.of("):]
    block = block[:block.index("));") + 3]
    styles = {}
    for style, body in re.findall(r'"(\w+)", List\.of\((.*?)\)', block, re.S):
        styles[style] = re.findall(r'"(village/[^"]+)"', body)
    return styles


def kind_of(path):
    name = path.rsplit("/", 1)[-1]
    for key, label in KINDS:
        if key in name:
            return label
    if any(shop in name for shop in SHOPS):
        return "oficina"
    return "moradia"


def profession_of(path):
    name = path.rsplit("/", 1)[-1]
    for profession, keys in WORKSHOPS:
        if any(key in name for key in keys):
            return NAMES[profession]
    return "—"


def exists(model_path):
    return os.path.exists(os.path.join(MODELS, model_path + ".nbt"))


def main():
    styles = allowed()
    lines = [
        "# Catálogo das estruturas da colônia",
        "",
        "> Gerado por `python scripts/structure_catalog.py` — não edite à mão; rode o",
        "> script de novo depois de pôr um modelo nesta pasta. Ver `LEIAME.md`.",
        "",
        "**Modelo do mod** = existe `colony/override/<caminho do jogo>.nbt`, e a colônia",
        "constrói ele no lugar da estrutura do jogo.",
        "",
        "## Casa de cada ofício",
        "",
        "Arquivo: `colony/<estilo>/trade_<oficio>.nbt` (só aquele estilo) ou",
        "`colony/trade_<oficio>.nbt` (todos os estilos). Com ele, a casa do ofício da",
        "colônia vem antes das do jogo (Regra 49).",
        "",
        "| Ofício | Arquivo | Todos os estilos | " + " | ".join(styles) + " | Casa do jogo que serve hoje |",
        "|---|---|---|" + "---|" * len(styles) + "---|",
    ]
    for profession in PROFESSIONS:
        name = "trade_" + profession.lower()
        general = "✅" if exists("colony/" + name) else "—"
        per_style = ["✅" if exists(f"colony/{style}/{name}") else "—" for style in styles]
        vanilla = next((", ".join(keys) for p, keys in WORKSHOPS if p == profession), "nenhuma")
        lines.append(f"| {NAMES[profession]} | `{name}.nbt` | {general} | " + " | ".join(per_style)
                     + f" | {vanilla} |")

    lines += ["", "## Modelos nomeados de profissão", "",
              "| Arquivo | Profissão | Existe |",
              "|---|---|---|"]
    for model, profession in NAMED_PROFESSION_MODELS.items():
        mark = "✅" if exists("colony/" + model) else "—"
        lines.append(f"| `{model}.nbt` | {NAMES[profession]} | {mark} |")

    total = sum(len(paths) for paths in styles.values())
    modeled = sum(1 for paths in styles.values() for p in paths if exists("colony/override/" + p))
    lines += ["", f"## Estruturas do jogo que a colônia constrói — {total}, {modeled} com modelo do mod", ""]

    for style, paths in styles.items():
        lines += [f"### {style} ({len(paths)})", "", "| Estrutura | Tipo | Ofício | Modelo do mod |", "|---|---|---|---|"]
        for path in paths:
            mark = "✅" if exists("colony/override/" + path) else "—"
            lines.append(f"| `{path}` | {kind_of(path)} | {profession_of(path)} | {mark} |")
        lines.append("")

    lines += ["## Estruturas que já são só da colônia", "",
              "| Arquivo | Uso |", "|---|---|",
              "| `houses/big_house_mod.nbt` | a BigHouseMOD da fundação (Regra 35) |",
              "| `houses/small_house.nbt` | casa pequena de teste |", ""]

    os.makedirs(FOLDER, exist_ok=True)
    with open(OUT, "w", encoding="utf-8", newline="\n") as out:
        out.write("\n".join(lines))
    named = sum(exists("colony/" + model) for model in NAMED_PROFESSION_MODELS)
    print(f"{OUT}: {total} estruturas do jogo em {len(styles)} estilos, {modeled} com modelo do mod, {named} nomeados")


if __name__ == "__main__":
    main()
