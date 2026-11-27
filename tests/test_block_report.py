"""Regra 384: o relatório do diário dos blocos acha o bloco que o jogador e o mod mudaram os dois."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
spec = importlib.util.spec_from_file_location("block_report", ROOT / "scripts/block_report.py")
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class BlockReportTest(unittest.TestCase):

    def test_a_block_changed_by_both_is_found(self):
        rows = [
            {"t": 1, "who": "mod:nivelamento", "x": 1, "y": 64, "z": 1, "now": "minecraft:dirt"},
            {"t": 2, "who": "player", "x": 1, "y": 64, "z": 1, "was": "minecraft:dirt", "now": "minecraft:air"},
            {"t": 3, "who": "player", "x": 9, "y": 64, "z": 9, "was": "minecraft:dirt", "now": "minecraft:air"},
        ]
        summary = report.summarize(rows)

        self.assertEqual({"mod:nivelamento": 1, "player": 2}, summary["by_who"])
        self.assertEqual([(1, 64, 1)], list(summary["both"]))
        self.assertIn("mod:nivelamento:dirt → player:air", "\n".join(report.lines(summary)))


if __name__ == "__main__":
    unittest.main()
