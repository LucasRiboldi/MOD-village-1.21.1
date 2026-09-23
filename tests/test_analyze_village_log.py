"""Regressoes do relatorio de travamentos extraido de logs do Minecraft."""

from __future__ import annotations

import sys
import json
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import analyze_village_log  # noqa: E402


class AnalyzeVillageLogTest(unittest.TestCase):
    def test_counts_anonymized_activity_transitions_per_profession(self) -> None:
        activities = analyze_village_log.analyze_activities(
            "\n".join(
                [
                    "[00:00:01] [Server thread/INFO]: VC_ACTIVITY version=1 profession=MINER activity=MINING outcome=WAITING reason=NO_TASK",
                    "[00:00:02] [Server thread/INFO]: VC_ACTIVITY version=1 profession=MINER activity=MINING outcome=WAITING reason=NO_TASK",
                    "[00:00:03] [Server thread/INFO]: VC_ACTIVITY version=1 profession=MINER activity=MINING outcome=RECOVERED reason=NO_TASK",
                    "[00:00:04] [Server thread/INFO]: VC_ACTIVITY version=1 profession=BUILDER activity=BUILDING outcome=ERROR reason=WORK_STALLED",
                    "[00:00:05] [Server thread/INFO]: VC_ACTIVITY version=1 profession=BUILDER activity=BUILDING outcome=ABANDONED reason=WORK_STALLED",
                ]
            )
        )

        self.assertEqual(2, activities[("MINER", "MINING", "WAITING", "NO_TASK")].occurrences)
        self.assertEqual(1, activities[("MINER", "MINING", "RECOVERED", "NO_TASK")].occurrences)
        self.assertEqual(1, activities[("BUILDER", "BUILDING", "ERROR", "WORK_STALLED")].occurrences)
        self.assertEqual(1, activities[("BUILDER", "BUILDING", "ABANDONED", "WORK_STALLED")].occurrences)

    def test_migrates_stall_history_without_inventing_old_activities(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "history.json"
            path.write_text(json.dumps({"schema": 1, "sessions": [{"observations": {}}]}), encoding="utf-8")

            history = analyze_village_log.read_history(path)

        self.assertEqual(2, history["schema"])
        self.assertEqual([], history["sessions"][0]["activities"])

    def test_counts_known_stalls_and_marks_only_repetition_as_loop(self) -> None:
        summary = analyze_village_log.analyze_text(
            "\n".join(
                [
                    "[00:00:01] [Server thread/INFO]: Colony a — no miner branch work: nothing to work on",
                    "[00:00:02] [Server thread/INFO]: Colony a — no miner branch work: nothing to work on",
                    "[00:00:03] [Server thread/INFO]: Colony a — no miner branch work: nothing to work on",
                    "[00:00:04] [Server thread/INFO]: Colony a builders: 0 working, WAITING_RESOURCES at ColonyPos[x=1, y=2, z=3], 12 blocks left — no build task, waiting for minecraft:cobblestone",
                    "[00:00:05] [Server thread/INFO]: Colony a — no collect_stone work: no worker in the village can do it — COBBLESTONE needs COLLECT_STONE",
                ]
            )
        )

        self.assertEqual(3, summary["miner_no_branch_work"].occurrences)
        self.assertTrue(summary["miner_no_branch_work"].loop_candidate)
        self.assertEqual(1, summary["construction_waiting_resources"].occurrences)
        self.assertFalse(summary["construction_waiting_resources"].loop_candidate)
        self.assertEqual(1, summary["missing_profession"].occurrences)

    def test_normalizes_variable_coordinates_without_recording_them(self) -> None:
        summary = analyze_village_log.analyze_text(
            "\n".join(
                [
                    "[00:00:01] [Server thread/INFO]: Colony aaa builders: 0 working, WAITING_RESOURCES at ColonyPos[x=1, y=2, z=3], 12 blocks left — no build task, waiting for minecraft:cobblestone",
                    "[00:00:02] [Server thread/INFO]: Colony bbb builders: 0 working, WAITING_RESOURCES at ColonyPos[x=-99, y=70, z=400], 4 blocks left — no build task, waiting for minecraft:oak_stairs",
                    "[00:00:03] [Server thread/INFO]: Colony ccc builders: 0 working, WAITING_RESOURCES at ColonyPos[x=8, y=9, z=10], 2 blocks left — no build task, waiting for minecraft:glass",
                ]
            )
        )

        waiting = summary["construction_waiting_resources"]
        self.assertEqual(3, waiting.occurrences)
        self.assertTrue(waiting.loop_candidate)
        self.assertEqual("obra aguarda recurso", waiting.meaning)


if __name__ == "__main__":
    unittest.main()
