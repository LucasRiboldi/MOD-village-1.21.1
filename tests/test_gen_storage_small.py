import collections
import importlib.util
import pathlib
import unittest

import nbtlib

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "gen_storage_small.py"
SPEC = importlib.util.spec_from_file_location("gen_storage_small", SCRIPT)
gen_storage_small = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(gen_storage_small)


def names(structure):
    palette = structure["palette"]

    return collections.Counter(str(palette[int(block["state"])]["Name"]) for block in structure["blocks"])


class GenStorageSmallTest(unittest.TestCase):
    """O galpão do armazém pequeno — Regra 165."""

    def test_every_style_is_a_seven_by_seven_shed_with_eight_chests_and_a_door(self):
        for style in gen_storage_small.STYLES:
            with self.subTest(style=style):
                counted = names(gen_storage_small.structure(style))

                self.assertEqual([int(n) for n in gen_storage_small.structure(style)["size"]], [7, 5, 7])
                self.assertEqual(counted["minecraft:chest"], 8)
                self.assertEqual(counted[f"minecraft:{gen_storage_small.STYLES[style][4]}"], 2)

    def test_the_files_in_the_jar_are_what_the_script_makes(self):
        for style in gen_storage_small.STYLES:
            with self.subTest(style=style):
                saved = nbtlib.load(gen_storage_small.OUT / style / "storage_small.nbt")

                self.assertEqual(names(saved), names(gen_storage_small.structure(style)))


if __name__ == "__main__":
    unittest.main()
