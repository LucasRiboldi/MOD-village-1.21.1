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

    def test_progress_signals_are_counted_but_never_loop_candidates(self) -> None:
        line = "[00:00:01] [Server thread/INFO]: Worker 1234abcd finished — the house is up"
        summary = analyze_village_log.analyze_text("\n".join([line] * 5))

        self.assertEqual(5, summary["house_finished"].occurrences)
        self.assertFalse(summary["house_finished"].loop_candidate,
                         "casa pronta repetida e progresso, nao travamento")

    def test_counts_the_stranded_worker_and_server_health(self) -> None:
        summary = analyze_village_log.analyze_text(
            "\n".join(
                [
                    "[03:10:00] [Server thread/INFO]: Worker 9a8b is stranded at -202, 62, -937 — frozen twice on the same spot;",
                    "[03:10:20] [Server thread/WARN]: Stranded worker 9a8b cannot dig out of -202, 62, -937 — no natural, dry way up",
                    "[03:11:00] [Server thread/INFO]: Stranded worker 9a8b is out at -200, 66, -937 after 4 steps — back in the work queue",
                    "[03:12:00] [Server thread/WARN]: Can't keep up! Is the server overloaded? Running 2034ms or 40 ticks behind",
                    "[03:13:00] [Server thread/ERROR]: Something broke",
                    "[03:14:00] [Server thread/INFO]: Miner 77aa took 0 from -10, 40, 5 — 12 this task",
                ]
            )
        )

        self.assertEqual(1, summary["worker_stranded"].occurrences)
        self.assertEqual(1, summary["stranded_cannot_dig_out"].occurrences)
        self.assertEqual(1, summary["stranded_freed"].occurrences)
        self.assertEqual(1, summary["server_overloaded"].occurrences)
        self.assertEqual(1, summary["log_error_line"].occurrences)
        self.assertEqual(1, summary["miner_chest_full"].occurrences)

    def test_ranks_the_pieces_the_builds_waited_for(self) -> None:
        text = "\n".join(
            ["... WAITING_RESOURCES ... waiting for minecraft:oak_stairs"] * 3
            + ["... WAITING_RESOURCES ... waiting for minecraft:glass_pane"]
        )

        self.assertEqual(
            {"minecraft:oak_stairs": 3, "minecraft:glass_pane": 1},
            analyze_village_log.analyze_waited_items(text),
        )


if __name__ == "__main__":
    unittest.main()
