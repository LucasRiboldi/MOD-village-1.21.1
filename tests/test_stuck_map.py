import importlib.util
import json
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "stuck_map.py"
SPEC = importlib.util.spec_from_file_location("stuck_map", SCRIPT)
stuck_map = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(stuck_map)


def event(**fields):
    return "[x] [Server thread/INFO] (villagecolony) VC_EVENT " + json.dumps(fields)


class StuckMapTest(unittest.TestCase):

    def test_teleports_are_read_from_the_village_diary(self):
        lines = [event(kind="event", type="teleport", **{"from": "120, 64, -35"}, why="stuck with no path home"),
                 event(kind="event", type="beds", before=3, now=4),
                 "linha qualquer"]

        self.assertEqual([(120, -35, "teleporte", "stuck with no path home")], stuck_map.teleports_from(lines))

    def test_strandings_are_read_from_the_action_journal(self):
        lines = [json.dumps({"action": "STRANDED", "prof": "SHEPHERD", "x": 10, "y": 64, "z": 5, "detail": "fenced in"}),
                 json.dumps({"action": "PLACED", "x": 1, "z": 1}),
                 "não é json"]

        self.assertEqual([(10, 5, "SHEPHERD", "fenced in")], stuck_map.strandings_from(lines))

    def test_the_cell_with_most_cases_comes_first(self):
        cases = [(10, 5, "A", ""), (11, 6, "A", ""), (9, 7, "B", ""), (100, 100, "C", "")]

        result = stuck_map.spots(cases, 4)

        self.assertEqual((8, 4), result[0][0])
        self.assertEqual(3, len(result[0][1]))
        self.assertEqual(1, len(result[1][1]))

    def test_without_a_log_it_reads_the_game_logs_folder(self):
        import os
        import tempfile

        with tempfile.TemporaryDirectory() as appdata:
            logs = pathlib.Path(appdata) / ".minecraft" / "logs"
            logs.mkdir(parents=True)
            (logs / "villagecolony-actions-20261028-010000.jsonl").write_text("", encoding="utf-8")
            (logs / "villagecolony-actions-20261028-020000.jsonl").write_text("", encoding="utf-8")
            old = os.environ.get("APPDATA")
            os.environ["APPDATA"] = appdata

            try:
                log, journals = stuck_map.game_logs()
            finally:
                if old is not None:
                    os.environ["APPDATA"] = old

        self.assertEqual(str(logs / "latest.log"), log)
        self.assertEqual([str(logs / "villagecolony-actions-20261028-020000.jsonl")], journals)


if __name__ == "__main__":
    unittest.main()
