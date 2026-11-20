import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "flow_report.py"
SPEC = importlib.util.spec_from_file_location("flow_report", SCRIPT)
flow_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(flow_report)


class FlowReportTest(unittest.TestCase):
    """Regra 344, telemetria T3: o balanço dos itens soma os dias e mostra o que mudou por fora."""

    LOG = [
        "VC_FLOW version=1 day=1 items=1 oak_planks=0,0,0,?,40,?",
        "VC_FLOW version=1 day=2 items=2 oak_planks=10,3,2,40,52,5 cobblestone=64,0,0,0,64,0",
    ]

    def test_the_days_are_summed_by_item(self):
        items = flow_report.collect(self.LOG)
        planks = items["oak_planks"]
        self.assertEqual(2, planks["days"])
        self.assertEqual(10, planks["in"])
        self.assertEqual(3, planks["out"])
        self.assertEqual(2, planks["refused"])
        self.assertEqual(5, planks["outside"], "o primeiro dia não tem ontem e não soma")
        self.assertEqual(52, planks["stock"])
        self.assertEqual(0, items["cobblestone"]["outside"])
