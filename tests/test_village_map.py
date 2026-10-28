import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "village_map.py"
SPEC = importlib.util.spec_from_file_location("village_map", SCRIPT)
village_map = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(village_map)


class UnpackTest(unittest.TestCase):

    def test_values_do_not_span_longs(self):
        # 9 bits: 7 valores por long, o resto do long fica sem uso (formato desde a 1.16).
        first = sum(v << (i * 9) for i, v in enumerate([1, 2, 3, 4, 5, 6, 7]))
        second = 300

        self.assertEqual([1, 2, 3, 4, 5, 6, 7, 300], village_map.unpack([first, second], 9, 8))

    def test_negative_longs_are_read_as_unsigned(self):
        self.assertEqual([15] * 16, village_map.unpack([-1], 4, 16))


class ColorTest(unittest.TestCase):

    def test_exact_name_wins_over_the_family(self):
        self.assertEqual(village_map.COLORS["dirt_path"], village_map.color_of("minecraft:dirt_path"))

    def test_family_by_word(self):
        self.assertEqual(village_map.color_of("minecraft:oak_leaves"), village_map.color_of("minecraft:spruce_leaves"))

    def test_mod_blocks_stand_out(self):
        self.assertEqual((240, 120, 30), village_map.color_of("villagecolony:colony_torch"))


if __name__ == "__main__":
    unittest.main()
