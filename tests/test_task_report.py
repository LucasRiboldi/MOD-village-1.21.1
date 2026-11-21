import importlib.util
import pathlib
import sys
import unittest

SCRIPTS = pathlib.Path(__file__).resolve().parent.parent / "scripts"
sys.path.insert(0, str(SCRIPTS))
SPEC = importlib.util.spec_from_file_location("task_report", SCRIPTS / "task_report.py")
task_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(task_report)

LINES = [
    '{"t":0,"worker":"aa","prof":"MINER","action":"TASK_TAKEN","task":"COLLECT_STONE","id":"t1","what":"cobblestone"}',
    '{"t":40,"worker":"aa","prof":"MINER","action":"TASK_STARTED","task":"COLLECT_STONE","id":"t1"}',
    '{"t":100,"worker":"aa","prof":"MINER","action":"MINED","id":"t1","what":"cobblestone","n":3}',
    '{"t":400,"worker":"aa","prof":"MINER","action":"TASK_DONE","task":"COLLECT_STONE","id":"t1","ticks":400}',
    '{"t":500,"worker":"bb","prof":"SMELTER","action":"TASK_TAKEN","task":"SMELT_MATERIAL","id":"t2"}',
    '{"t":900,"worker":"bb","prof":"SMELTER","action":"TASK_RELEASED","task":"SMELT_MATERIAL","id":"t2",'
    '"detail":"none of 3 colony chests had iron"}',
    '{"t":950,"worker":"bb","prof":"SMELTER","action":"PICKED_UP","what":"sand"}',
]


class TaskReportTest(unittest.TestCase):
    """Regra 344: cada tarefa seguida pelo número, do pedido ao fim."""

    def test_each_task_is_traced_from_taken_to_its_end(self):
        tasks = task_report.trace(task_report.parse(LINES))
        self.assertEqual({"t1", "t2"}, set(tasks))
        self.assertEqual(40, tasks["t1"]["started"])
        self.assertEqual(100, tasks["t1"]["first"])
        self.assertEqual("TASK_DONE", tasks["t1"]["outcome"])
        self.assertEqual("TASK_RELEASED", tasks["t2"]["outcome"])

    def test_the_summary_counts_and_times_by_type(self):
        by_type = task_report.summarize(task_report.trace(task_report.parse(LINES)))
        stone = by_type["COLLECT_STONE"]
        self.assertEqual(1, stone["done"])
        self.assertEqual([100], stone["to_first"])
        self.assertEqual([400], stone["to_end"])
        self.assertEqual(1, by_type["SMELT_MATERIAL"]["released"])
        self.assertIn("none of N colony chests had iron", by_type["SMELT_MATERIAL"]["reasons"])
