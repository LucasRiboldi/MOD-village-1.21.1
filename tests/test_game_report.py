import importlib.util
import pathlib
import sys
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parent.parent / "scripts"
sys.path.insert(0, str(SCRIPTS))
SPEC = importlib.util.spec_from_file_location("game_report", SCRIPTS / "game_report.py")
game_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(game_report)

LOG = [
    "VC_TIME version=1 colony=ab12cd34 window=300s SMELTER[s=300 work=20 walk=10 wait=10 blocked=10 idle=50 stranded=0]",
    "VC_VILLAGER_DEATH version=1 colony=ab12cd34 worker=aa11bb22 prof=MINER cause=fall at=10, 40, 5",
    "VC_VILLAGER_DAY version=1 colony=ab12cd34 day=3 villagers=1 aa11bb22=MINER,9000,0,100,300,12",
    "VC_FLOW version=1 day=3 items=1 oak_planks=10,3,0,40,52,5",
    "VC_VANILLA_JOB version=1 action=stripped worker=cc33dd44 trade=farmer",
]


class GameReportTest(unittest.TestCase):
    """Regra 344, telemetria T6: os achados mais graves primeiro, num relatório só."""

    def test_the_findings_come_in_order_of_weight(self):
        found = [text for _rank, text in game_report.findings(LOG, [])]
        self.assertTrue(all(text.startswith("SMELTER: ") for text in found[:2]), found)
        self.assertTrue(any(text.startswith("SMELTER: REGRA 51") for text in found), found)
        self.assertTrue(any("morreu: fall" in text for text in found), found)
        self.assertTrue(any("300 s sem cama" in text for text in found), found)
        self.assertTrue(any("oak_planks: +5 por fora" in text for text in found), found)
        self.assertEqual("ofício Vanilla farmer stripped 1×", found[-1])

    def test_the_report_has_the_findings_and_every_section(self):
        report = game_report.build(LOG, [], "2026-11-20 20:00")
        self.assertIn("## Achados, do mais grave ao menos", report)
        for title in ("Tempo por ofício", "Tarefas", "Linha do tempo dos aldeões", "Balanço dos itens",
                      "Blocos e travas"):
            self.assertIn("## " + title, report)
