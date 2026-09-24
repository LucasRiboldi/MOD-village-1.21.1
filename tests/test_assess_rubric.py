"""A regua da avaliacao tecnica e estavel: mesma medida, mesma nota."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts" / "assess"))

import rubric  # noqa: E402


def sample(**over):
    data = {
        "source": {"files": 100, "files_over_500": [], "method_lines_p90": 24, "method_lines_max": 90,
                   "pct_cc_over_10": 2.7, "cc_max": 26, "core_layer_violations": [],
                   "mutable_statics": 40, "mutable_statics_per_kloc": 4.0, "test_to_main_ratio": 1.2,
                   "comment_ratio": 0.98, "code_lines": 20000},
        "coverage": {"line_core_data": 91.0, "line_by_layer": {"fabric": 11.0}},
        "mutation": {"score": 77.5, "strength": 86.0},
        "gametests": [{"total": 433, "failed": 0}, {"total": 433, "failed": 0}],
        "static_analysis": {"warnings": 60},
        "process": {"practices": {k: True for k in "abcdefg"}},
        "game_performance": {"session_hours": 6.6, "observations": {"cycle_over_tick": 196}},
        "records": {"todo_open_critical": 4, "todo_open": 52},
    }
    data.update(over)
    return data


class RubricTest(unittest.TestCase):
    def test_ladder_picks_the_first_band_reached(self) -> None:
        self.assertEqual(4, rubric.ladder(90, (80, 65, 50, 35)))
        self.assertEqual(2, rubric.ladder(50, (80, 65, 50, 35)))
        self.assertEqual(0, rubric.ladder(10, (80, 65, 50, 35)))
        self.assertEqual(4, rubric.ladder(0, (0, 2, 5, 10), higher_is_better=False))
        self.assertEqual(1, rubric.ladder(29.7, (0, 5, 15, 40), higher_is_better=False))

    def test_the_same_measures_always_give_the_same_scorecard(self) -> None:
        first = rubric.score_all(sample())
        second = rubric.score_all(sample())
        self.assertEqual(first, second)
        self.assertEqual(len(rubric.CRITERIA), first["_overall"]["criteria_scored"])

    def test_one_failing_gametest_lowers_stability(self) -> None:
        scores = rubric.score_all(sample(gametests=[{"total": 433, "failed": 1}, {"total": 433, "failed": 0}]))
        self.assertEqual(2, scores["C09"]["score"])

    def test_a_missing_measure_is_not_scored_instead_of_scored_zero(self) -> None:
        scores = rubric.score_all(sample(game_performance={}))
        self.assertIsNone(scores["C13"]["score"])
        self.assertEqual(len(rubric.CRITERIA) - 1, scores["_overall"]["criteria_scored"])

    def test_grades(self) -> None:
        self.assertEqual("A", rubric.grade(3.6))
        self.assertEqual("B", rubric.grade(2.86))
        self.assertEqual("E", rubric.grade(1.0))


if __name__ == "__main__":
    unittest.main()
