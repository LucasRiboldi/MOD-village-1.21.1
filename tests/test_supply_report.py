import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "supply_report.py"
SPEC = importlib.util.spec_from_file_location("supply_report", SCRIPT)
supply_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(supply_report)

LOG = [
    "[12:00:00] [Server thread/INFO]: VC_SUPPLY version=1 exception=APPEARING_PIECE rule=88 colony=a828fd3e"
    " item=lime_terracotta count=1",
    "[12:00:01] [Server thread/INFO]: VC_SUPPLY version=1 exception=APPEARING_PIECE rule=88 colony=a828fd3e"
    " item=lime_terracotta count=1",
    "[12:00:02] [Server thread/INFO]: VC_SUPPLY version=1 exception=FOUNDATION_GROUND rule=90 colony=a828fd3e"
    " item=lot fill count=12",
    "[12:00:03] [Server thread/INFO]: uma linha qualquer",
]


class SupplyReportTest(unittest.TestCase):

    def test_reads_the_line_written_by_supplies(self):
        rows = supply_report.parse(LOG)

        self.assertEqual(3, len(rows))
        self.assertEqual(("FOUNDATION_GROUND", 90, "a828fd3e", "lot fill", 12), rows[2])

    def test_sums_per_exception_and_item(self):
        totals = supply_report.summarize(supply_report.parse(LOG))

        self.assertEqual(2, totals["APPEARING_PIECE"][1])
        self.assertEqual(2, totals["APPEARING_PIECE"][2]["lime_terracotta"])
        self.assertEqual(12, totals["FOUNDATION_GROUND"][1])

    def test_the_report_puts_the_biggest_first_and_says_when_there_is_none(self):
        text = supply_report.render(supply_report.summarize(supply_report.parse(LOG)))

        self.assertLess(text.index("FOUNDATION_GROUND"), text.index("APPEARING_PIECE"))
        self.assertIn("Nenhum suprimento", supply_report.render(supply_report.summarize([])))


if __name__ == "__main__":
    unittest.main()
