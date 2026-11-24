"""Verifica os arquivos que o jogo carrega e a reprodução binária do gerador."""
import subprocess
import sys
import tempfile
import unittest
from itertools import product
from pathlib import Path

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/data/villagecolony/structure/colony"
THEMES = ("cattle", "sheep", "pig", "poultry", "horse", "camelid")
PLANTS = {"short_grass", "tall_grass", "fern", "dandelion", "poppy", "cornflower",
          "oxeye_daisy", "azure_bluet", "allium"}
REQUIRED = {
    "cattle": {"hay_block", "water_cauldron", "polished_andesite"},
    "sheep": {"hay_block", "water_cauldron", "white_wool", "loom"},
    "horse": {"hay_block", "water_cauldron", "smooth_stone_slab"},
    "camelid": {"hay_block", "water_cauldron", "terracotta", "blue_carpet"},
    "pig": {"carrots", "potatoes", "beetroots"},
    "poultry": {"carrots", "wheat", "hay_block", "spruce_slab"},
}


def filename(theme):
    return f"medium_animal_pen_{theme}.nbt"


class MediumAnimalPensTest(unittest.TestCase):
    def decode(self, theme):
        data = nbtlib.load(ASSETS / filename(theme))
        self.assertEqual(list(data["size"]), [12, 5, 17])
        self.assertEqual(data["DataVersion"], 3955)
        self.assertFalse(data["entities"])
        blocks = {}
        for entry in data["blocks"]:
            self.assertNotIn("nbt", entry)
            idx = int(entry["state"])
            self.assertTrue(0 <= idx < len(data["palette"]))
            state = data["palette"][idx]
            pos = tuple(map(int, entry["pos"]))
            self.assertNotIn(pos, blocks)
            name = str(state["Name"])
            self.assertTrue(name.startswith("minecraft:"))
            blocks[pos] = (name.removeprefix("minecraft:"), state.get("Properties", {}))
        self.assertEqual(set(blocks), set(product(range(12), range(5), range(17))))
        return blocks

    def test_enclosure_entrance_and_headroom(self):
        for theme in THEMES:
            with self.subTest(theme=theme):
                blocks = self.decode(theme)
                gates = [p for p, (n, _) in blocks.items() if n.endswith("_fence_gate")]
                self.assertEqual(gates, [(6, 1, 1)])
                self.assertEqual(blocks[gates[0]][1]["facing"], "north")
                self.assertEqual(blocks[gates[0]][1]["open"], "false")
                self.assertEqual(blocks[6, 0, 0][0], "dirt_path")
                for z, start in ((0, 1), (1, 2), (2, 1)):
                    self.assertTrue(all(blocks[6, y, z][0] == "air" for y in range(start, 5)))
                for x, z in product(range(12), range(1, 13)):
                    if x in (0, 11) or z in (1, 12):
                        if (x, z) != (6, 1):
                            self.assertTrue(blocks[x, 1, z][0].endswith("_fence"))
                    else:
                        self.assertEqual(blocks[x, 0, z][0], "grass_block")
                        for y in range(1, 5):
                            self.assertIn(blocks[x, y, z][0], PLANTS | {"air"})
                            if x in (1, 10) or z in (2, 11):
                                self.assertEqual(blocks[x, y, z][0], "air")
                self.assertTrue(all(blocks[x, y, 13][0] == "air"
                                    for x, y in product(range(12), range(1, 5))))

    def test_plants_lighting_and_theme(self):
        for theme in THEMES:
            with self.subTest(theme=theme):
                blocks = self.decode(theme)
                names = {name for name, _ in blocks.values()}
                self.assertTrue(REQUIRED[theme] <= names)
                self.assertFalse(any("leaves" in n or "sapling" in n for n in names))
                lights = [p for p, (n, _) in blocks.items() if n == "lantern"]
                for x, y, z in lights:
                    self.assertTrue(blocks[x, y-1, z][0].endswith("_fence"))
                for x, z in product(range(1, 11), range(2, 12)):
                    distance = min(abs(x-lx) + abs(1-ly) + abs(z-lz) for lx, ly, lz in lights)
                    self.assertLessEqual(distance, 10)
                meadow = [blocks[x, 1, z][0] for x, z in product(range(1, 11), range(2, 12))]
                self.assertTrue(10 <= sum(n in PLANTS for n in meadow) <= 25)
                self.assertTrue({"short_grass", "tall_grass", "fern"} <= set(meadow))
                self.assertTrue(set(meadow) & (PLANTS - {"short_grass", "tall_grass", "fern"}))
                sources = [(x, z) for (x, y, z), (n, _) in blocks.items() if n == "water" and y == 0]
                for (x, y, z), (name, props) in blocks.items():
                    if name in PLANTS:
                        if props.get("half") == "upper":
                            self.assertEqual(blocks[x, y-1, z][1]["half"], "lower")
                        else:
                            self.assertEqual(blocks[x, y-1, z][0], "grass_block")
                            if name == "tall_grass":
                                self.assertEqual(blocks[x, y+1, z][1]["half"], "upper")
                    if name in {"carrots", "potatoes", "beetroots", "wheat"}:
                        self.assertGreaterEqual(z, 14)
                        self.assertEqual(blocks[x, y-1, z][0], "farmland")
                        self.assertTrue(any(max(abs(x-wx), abs(z-wz)) <= 4 for wx, wz in sources))
                    if name == "water_cauldron":
                        self.assertEqual(props["level"], "3")
                    if name in REQUIRED[theme]:
                        self.assertGreaterEqual(z, 14)

    def test_reproducible_files(self):
        with tempfile.TemporaryDirectory() as folder:
            subprocess.run([sys.executable, str(ROOT / "scripts/gen_medium_animal_pens.py"),
                            "--output", folder], check=True)
            out = Path(folder)
            self.assertEqual({p.name for p in out.iterdir()}, {filename(t) for t in THEMES})
            for theme in THEMES:
                with self.subTest(theme=theme):
                    self.assertEqual((out / filename(theme)).read_bytes(),
                                     (ASSETS / filename(theme)).read_bytes())


if __name__ == "__main__":
    unittest.main()
