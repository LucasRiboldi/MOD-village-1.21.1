"""Regras de casa (Regra 369): cama, baú na parede, passagem, luz, piso inteiro e nenhum posto de trabalho do jogo."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
BIG = ROOT / "src/main/resources/data/villagecolony/structure/houses/big_house_mod.nbt"


def _rules():
    spec = importlib.util.spec_from_file_location("house_rules", ROOT / "scripts/houses/house_rules.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


rules = _rules()


def small_house(**extra):
    """Casa 5×4×5: piso de pedra na camada 0, paredes nas bordas, porta a oeste em (0, 1, 2), teto na camada 3."""
    blocks = {}
    for x in range(5):
        for z in range(5):
            blocks[(x, 0, z)] = ("stone", {})
            blocks[(x, 3, z)] = ("oak_planks", {})
            for y in (1, 2):
                if x in (0, 4) or z in (0, 4):
                    blocks[(x, y, z)] = ("cobblestone", {})
    blocks[(0, 1, 2)] = ("oak_door", {"half": "lower", "facing": "east"})
    blocks[(0, 2, 2)] = ("oak_door", {"half": "upper", "facing": "east"})
    blocks.update(extra)
    return rules.House((5, 4, 5), blocks)


# A cama: pé em (3,1,2), cabeceira em (3,1,1).
BED = {(3, 1, 2): ("white_bed", {"part": "foot", "facing": "north"}),
       (3, 1, 1): ("white_bed", {"part": "head", "facing": "north"})}
TORCH = {(2, 2, 1): ("wall_torch", {"facing": "south"})}


class HouseRulesTest(unittest.TestCase):

    def test_the_current_big_house_traps_a_villager(self):
        found = rules.problems(rules.load(str(BIG)))

        self.assertTrue(any(item.startswith("cama fora do caminho") for item in found), found)
        self.assertTrue(any(item.startswith("buraco no piso") for item in found), found)

    def test_a_chest_in_the_middle_of_the_room_is_loose(self):
        house = small_house()
        house.blocks.update(BED)
        house.blocks.update(TORCH)
        house.blocks[(2, 1, 2)] = ("chest", {"facing": "west"})

        self.assertTrue(any(item.startswith("baú solto") for item in rules.problems(house)), rules.problems(house))

    def test_a_vanilla_job_block_is_refused(self):
        house = small_house()
        house.blocks.update(BED)
        house.blocks.update(TORCH)
        house.blocks[(1, 1, 1)] = ("chest", {"facing": "south"})
        house.blocks[(1, 1, 3)] = ("composter", {})

        self.assertIn("posto de trabalho do jogo composter", rules.problems(house))

    def test_a_clean_small_house_passes(self):
        house = small_house()
        house.blocks.update(BED)
        house.blocks.update(TORCH)
        house.blocks[(1, 1, 1)] = ("chest", {"facing": "south"})

        self.assertEqual([], rules.problems(house))


if __name__ == "__main__":
    unittest.main()
