"""Regra 379: a água do curral grande fica fora da reta do portão (autor 26-11, J6 do jogo da 0.4.145)."""
import pathlib
import unittest

import nbtlib

PEN = pathlib.Path(__file__).resolve().parent.parent / \
    "src/main/resources/data/villagecolony/structure/colony/large_animal_pen.nbt"


def blocks():
    data = nbtlib.load(str(PEN))
    palette = [str(entry["Name"]).split(":", 1)[-1] for entry in data["palette"]]
    return [(palette[int(b["state"])], tuple(int(v) for v in b["pos"])) for b in data["blocks"]]


class LargeAnimalPenTest(unittest.TestCase):

    def test_the_water_is_out_of_the_gate_line(self):
        found = blocks()
        gates = [pos for name, pos in found if name == "oak_fence_gate"]
        water = [pos for name, pos in found if name == "water"]

        self.assertEqual(1, len(water))
        for gate in gates:
            # Portão nos lados norte e sul: a reta dele corre em z, e a água fica 2 ou mais fora dela em x; nos lados
            # leste e oeste, o contrário.
            across = 0 if gate[2] in (0, 14) else 2
            self.assertGreaterEqual(abs(water[0][across] - gate[across]), 2,
                                    f"Regra 379: a água em {water[0]} está na reta do portão em {gate}")

    def test_the_water_still_waters_every_farmland(self):
        found = blocks()
        water = next(pos for name, pos in found if name == "water")
        farmland = [pos for name, pos in found if name == "farmland"]

        self.assertTrue(farmland)
        for pos in farmland:
            self.assertLessEqual(max(abs(pos[0] - water[0]), abs(pos[2] - water[2])), 4,
                                 f"a terra arada em {pos} ficou longe da água em {water}")


if __name__ == "__main__":
    unittest.main()
