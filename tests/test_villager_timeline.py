import importlib.util
import pathlib
import sys
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parent.parent / "scripts"
sys.path.insert(0, str(SCRIPTS))
SPEC = importlib.util.spec_from_file_location("villager_timeline", SCRIPTS / "villager_timeline.py")
villager_timeline = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(villager_timeline)

LINES = [
    '{"t":600,"worker":"aa","prof":"SMELTER","action":"STATE","what":"wait","x":5,"y":64,"z":9,"ticks":600,'
    '"detail":"from WORK"}',
    '{"t":2600,"worker":"aa","prof":"SMELTER","action":"STATE","what":"work","x":5,"y":64,"z":9,"ticks":2000,'
    '"detail":"from WAIT SMELT_MATERIAL:IRON_INGOT"}',
    '{"t":100,"worker":"aa","prof":"SMELTER","action":"SMELTED","what":"glass"}',
]


class VillagerTimelineTest(unittest.TestCase):
    """Regra 344, telemetria T2: o dia de cada aldeão pelas mudanças de estado."""

    def test_each_villager_sums_the_states_and_keeps_the_long_stops(self):
        villagers = villager_timeline.stretches(villager_timeline.parse(LINES))
        smelter = villagers["aa"]
        self.assertEqual("SMELTER", smelter["prof"])
        self.assertEqual(30, smelter["seconds"]["WORK"])
        self.assertEqual(100, smelter["seconds"]["WAIT"])
        self.assertEqual([(100.0, "WAIT", "SMELT_MATERIAL:IRON_INGOT", (5, 64, 9))], smelter["stops"])
