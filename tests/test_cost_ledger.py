import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "cost_ledger.py"
SPEC = importlib.util.spec_from_file_location("cost_ledger", SCRIPT)
cost_ledger = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(cost_ledger)

ONE = ("[12:00:00] [Server thread/INFO] (villagecolony) VC_COST version=1 colonies=1 detect_us=1000"
       " lifecycle_us=0 population_us=500 chests_us=2000 planner_us=8000 assign_us=300"
       " workers_us=700 other_us=500 total_us=13000")
FOUR = ONE.replace("colonies=1", "colonies=4").replace("planner_us=8000", "planner_us=20000")


class CostLedgerTest(unittest.TestCase):

    def test_reads_the_line_written_by_cycle_cost(self):
        samples = cost_ledger.parse([ONE, "linha qualquer", FOUR])

        self.assertEqual(2, len(samples))
        self.assertEqual(1, samples[0][0])
        self.assertEqual(8000, samples[0][1]["planner"])
        self.assertEqual(13000, samples[0][1]["total"])

    def test_groups_by_number_of_colonies_in_milliseconds(self):
        result, counts = cost_ledger.summarize(cost_ledger.parse([ONE, ONE, FOUR]))

        self.assertEqual(2, counts["1 colônia"])
        self.assertEqual(1, counts["4+ colônias"])
        self.assertAlmostEqual(8.0, result["1 colônia"]["planner"][0])
        self.assertAlmostEqual(20.0, result["4+ colônias"]["planner"][2])

    def test_a_log_without_samples_is_reported_not_silent(self):
        self.assertEqual([], cost_ledger.parse(["[12:00:00] nada aqui"]))


if __name__ == "__main__":
    unittest.main()
