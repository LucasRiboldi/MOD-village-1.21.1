import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "action_report.py"
SPEC = importlib.util.spec_from_file_location("action_report", SCRIPT)
action_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(action_report)

LINES = [
    '{"t":0,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"TASK_TAKEN",'
    '"task":"COLLECT_STONE","what":"cobblestone","n":32,"detail":"AVAILABLE"}',
    '{"t":100,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"MINED",'
    '"what":"COBBLESTONE","n":3,"x":1,"y":2,"z":3}',
    '{"t":1200,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"TASK_RELEASED",'
    '"task":"COLLECT_STONE","what":"cobblestone","n":32,"ticks":1200,"detail":"EXECUTING"}',
    '{"t":1300,"colony":"33a6b9c4","worker":"9353ad2c","prof":"BUILDER","action":"TASK_TAKEN",'
    '"task":"BUILD","what":"oak_planks","n":1}',
    '{"t":2400,"colony":"33a6b9c4","worker":"9353ad2c","prof":"BUILDER","action":"TASK_DONE",'
    '"task":"BUILD","what":"oak_planks","n":1,"ticks":1100}',
    '{"t":1250,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"TARGET_REJECTED",'
    '"what":"stone","x":-751,"y":31,"z":-942,"detail":"TOO_HIGH"}',
    '{"t":1260,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"TARGET_REJECTED",'
    '"what":"stone","x":-750,"y":31,"z":-942,"detail":"TOO_HIGH"}',
    '{"t":2400,"colony":"33a6b9c4","worker":"9353ad2c","prof":"BUILDER","action":"PLA',
]


class ActionReportTest(unittest.TestCase):

    def test_a_broken_last_line_is_skipped(self):
        self.assertEqual(7, len(action_report.parse(LINES)))

    def test_counts_tasks_and_actions_per_profession(self):
        by_prof, by_worker, minutes = action_report.summarize(action_report.parse(LINES))

        self.assertEqual(2.0, minutes)
        self.assertEqual(1, by_prof["MINER"]["events"]["TASK_RELEASED"])
        self.assertEqual(3, by_prof["MINER"]["amount"]["MINED"])
        self.assertEqual([1100], by_prof["BUILDER"]["done_ticks"])
        self.assertEqual(1, by_worker["cc99a53b"]["released_tasks"]["COLLECT_STONE cobblestone"])

    def test_the_rejection_reasons_are_counted_per_profession(self):
        by_prof, _, _ = action_report.summarize(action_report.parse(LINES))

        self.assertEqual(2, by_prof["MINER"]["reasons"]["TARGET_REJECTED: TOO_HIGH"])
        text = action_report.render(*action_report.summarize(action_report.parse(LINES)))
        self.assertIn("2× TARGET_REJECTED: TOO_HIGH", text)

    def test_a_release_with_a_reason_is_counted_and_a_bare_state_is_not(self):
        lines = LINES[:-1] + [
            '{"t":1300,"colony":"33a6b9c4","worker":"cc99a53b","prof":"MINER","action":"TASK_RELEASED",'
            '"task":"COLLECT_STONE","what":"cobblestone","n":32,'
            '"detail":"no room in the chests for what it mined"}',
        ]
        by_prof, _, _ = action_report.summarize(action_report.parse(lines))

        self.assertEqual(1, by_prof["MINER"]["reasons"]["TASK_RELEASED: no room in the chests for what it mined"])
        self.assertNotIn("TASK_RELEASED: EXECUTING", by_prof["MINER"]["reasons"])

    def test_numbers_inside_a_reason_do_not_split_it(self):
        self.assertEqual(action_report.reason_of("it got no closer than 6,9 blocks in 400 ticks"),
                         action_report.reason_of("it got no closer than 7,4 blocks in 400 ticks"))

    def test_the_report_names_the_completion_rate(self):
        text = action_report.render(*action_report.summarize(action_report.parse(LINES)))

        self.assertIn("BUILDER", text)
        self.assertIn("100%", text)
        self.assertIn("0%", text)


    def test_the_block_codes_are_counted_per_profession(self):
        lines = [
            '{"t":0,"worker":"a","prof":"MINER","action":"GAVE_UP","detail":"has not moved","code":"NO_PROGRESS"}',
            '{"t":5,"worker":"a","prof":"MINER","action":"STRANDED","detail":"frozen twice","code":"NO_PROGRESS"}',
            '{"t":9,"worker":"b","prof":"FARMER","action":"TASK_RELEASED","detail":"could not reach the crop",'
            '"code":"NO_PATH"}',
        ]
        by_prof, by_worker, minutes = action_report.summarize(action_report.parse(lines))

        self.assertEqual(2, by_prof["MINER"]["codes"]["NO_PROGRESS"])
        self.assertEqual(1, by_prof["FARMER"]["codes"]["NO_PATH"])
        self.assertIn("Por código de bloqueio", action_report.render(by_prof, by_worker, minutes))

if __name__ == "__main__":
    unittest.main()
