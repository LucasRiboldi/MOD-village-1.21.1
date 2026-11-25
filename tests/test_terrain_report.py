import gzip
import importlib.util
import json
import pathlib
import sys
import tempfile
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parent.parent / "scripts"
sys.path.insert(0, str(SCRIPTS))
SPEC = importlib.util.spec_from_file_location("terrain_report", SCRIPTS / "terrain_report.py")
terrain_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(terrain_report)

SIZE = 12


def photo(label, heights, edits=None, day=0):
    """Uma foto 12×12 a partir de (0, 0): casa em x 2–3, z 2–3, porta ao sul em (2, 64, 4)."""
    y = [heights(x, z) for x in range(SIZE) for z in range(SIZE)]
    return {"version": 1, "label": label, "day": day, "minX": 0, "minZ": 0, "width": SIZE, "depth": SIZE,
            "center": [6, 65, 6], "y": y, "kind": [1] * len(y), "edit": edits or [0] * len(y),
            "target": [64] * len(y),
            "structures": [{"min": [2, 65, 2], "max": [3, 68, 3], "source": "colony", "blueprint": "house",
                            "finished": True}],
            "doors": [[2, 64, 4, 0]]}


def cliff(x, z):
    return 64 if x < 8 else 67


class TerrainReportTest(unittest.TestCase):
    """ADR-064: o relatório do terreno mede o acesso a pé e o que o jogador quis."""

    def test_the_cliff_stops_the_walk_and_counts_as_steps(self):
        snap = terrain_report.Snapshot(photo("start", cliff))
        reached = terrain_report.reach(snap)

        self.assertNotIn((9, 6), reached)
        self.assertIn((6, 6), reached)
        self.assertEqual(SIZE, terrain_report.steps(snap)["total"])

    def test_a_house_on_flat_ground_has_four_sides_and_its_door(self):
        snap = terrain_report.Snapshot(photo("start", cliff))
        access = terrain_report.structure_access(snap, terrain_report.reach(snap))

        self.assertEqual(1, len(access))
        self.assertEqual(4, len(access[0]["sides"]))
        self.assertTrue(access[0]["door"])

    def test_the_player_who_cuts_a_ramp_opened_a_passage(self):
        first = terrain_report.Snapshot(photo("start", cliff))
        ramp = lambda x, z: 65 if (x == 8 and z == 6) else 66 if (x == 9 and z == 6) else cliff(x, z)  # noqa: E731
        edits = [terrain_report.PLAYER if (x, z) in ((8, 6), (9, 6)) else 0 for x in range(SIZE) for z in range(SIZE)]
        last = terrain_report.Snapshot(photo("last", ramp, edits, day=2))
        it = terrain_report.intent(first, last, {(8, 6): 1}, {})
        by, volume = terrain_report.earthwork(first, last)

        self.assertEqual({"abriu passagem": 1}, dict(it["kinds"]))
        self.assertEqual(2, by["jogador"])
        self.assertEqual(3, volume["jogador"])

    def test_the_summary_reads_the_files_of_the_log_folder(self):
        with tempfile.TemporaryDirectory() as logs:
            for label, data in (("start", photo("start", cliff)), ("day3", photo("day3", lambda x, z: 64, day=3))):
                with gzip.open(pathlib.Path(logs) / f"villagecolony-terrain-ab12cd34-{label}.json.gz", "wt",
                               encoding="utf-8") as handle:
                    json.dump(data, handle)
            log = pathlib.Path(logs) / "latest.log"
            log.write_text("VC_PLAYER_EDIT version=1 action=broke block=air was=sand target=natural at=8, 67, 6\n",
                           encoding="utf-8")

            lines = terrain_report.summary_lines(logs, [str(log)])

        self.assertEqual(1, len(lines))
        self.assertIn("degraus >1 12 → 0", lines[0])
        self.assertIn("colunas do jogador 1", lines[0])


if __name__ == "__main__":
    unittest.main()
