import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "player_report.py"
SPEC = importlib.util.spec_from_file_location("player_report", SCRIPT)
player_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(player_report)

LOG = [
    "VC_PLAYER_EDIT version=1 action=broke block=air was=oak_planks target=construcao at=10, 64, 5",
    "VC_PLAYER_INSIST version=1 system=obra block=air target=construcao count=1 gap=400 at=10, 64, 5",
    "VC_PLAYER_INSIST version=1 system=obra block=air target=construcao count=2 gap=300 at=10, 64, 5",
    "VC_PLAYER_UNDO version=1 system=calcada block=grass_block was=dirt_path target=rua gap=120 at=4, 63, 4",
    "VC_MOD_OVERRIDE version=1 system=rua block=dirt_path was=oak_fence gap=200 at=6, 64, 6",
]


class PlayerReportTest(unittest.TestCase):
    """Regra 345: o conflito com o jogador e a reafirmação, que vira item a corrigir."""

    def test_an_insistence_twice_in_the_same_place_is_serious(self):
        fixes = player_report.to_fix(player_report.collect(LOG))
        self.assertEqual(1, len(fixes))
        serious, text = fixes[0]
        self.assertTrue(serious)
        self.assertIn("refez 2×", text)
        self.assertIn("'obra'", text)

    def test_both_sides_of_the_conflict_are_counted(self):
        data = player_report.collect(LOG)
        self.assertEqual(1, data["undo"][("calcada", "rua")])
        self.assertEqual(1, data["override"][("rua", "oak_fence→dirt_path")])
        self.assertEqual(1, data["edits"][("construcao", "broke")])

    def test_legacy_grass_noise_does_not_hide_a_named_or_unknown_conflict(self):
        data = player_report.collect(LOG + [
            "VC_MOD_OVERRIDE version=1 system=mod?natural block=grass_block was=dirt_path gap=12 at=1, 64, 1",
            "VC_PLAYER_INSIST version=1 system=mod?natural block=dirt_path target=natural count=1 gap=5 at=1, 64, 1",
            "VC_MOD_OVERRIDE version=1 system=mod?natural block=dirt was=grass_block gap=12 at=2, 64, 1",
            "VC_PLAYER_UNDO version=1 system=mod?natural block=air was=dirt target=natural gap=5 at=2, 64, 1",
            "VC_MOD_OVERRIDE version=1 system=nivelamento block=grass_block was=dirt gap=12 at=3, 64, 1",
            "VC_MOD_OVERRIDE version=1 system=mod?outro block=air was=grass_block gap=12 at=4, 64, 1",
        ])
        self.assertNotIn(("mod?natural", "dirt_path→grass_block"), data["override"])
        self.assertNotIn(("mod?natural", "natural"), data["undo"])
        self.assertEqual({"obra": 2}, data["insist_by_system"])
        self.assertEqual(1, data["override"][("nivelamento", "dirt→grass_block")])
        self.assertEqual(1, data["override"][("mod?outro", "grass_block→air")])
