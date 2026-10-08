import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "time_ledger.py"
SPEC = importlib.util.spec_from_file_location("time_ledger", SCRIPT)
time_ledger = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(time_ledger)


def shares(work=0, walk=0, wait=0, blocked=0, idle=0, stranded=0):
    return {"work": work, "walk": walk, "wait": wait, "blocked": blocked, "idle": idle, "stranded": stranded}


class Rule51Test(unittest.TestCase):
    """Regra 51 (2026-10-08): ocioso + bloqueado + encalhado acima de 15% reprova."""

    def test_idle_plus_blocked_above_fifteen_fails(self):
        said = time_ledger.verdict("LUMBERJACK", shares(work=10, walk=44, idle=15, blocked=31))
        self.assertTrue(any(s.startswith("REGRA 51 (46%") for s in said), said)

    def test_fifteen_exactly_passes(self):
        said = time_ledger.verdict("MASON", shares(work=40, walk=45, idle=10, blocked=5))
        self.assertFalse(any(s.startswith("REGRA 51") for s in said), said)

    def test_waiting_is_an_alert_not_rule_51(self):
        said = time_ledger.verdict("CARPENTER", shares(work=11, walk=28, wait=52, idle=8, blocked=1))
        self.assertIn("ESPERA (52%)", said)
        self.assertFalse(any(s.startswith("REGRA 51") for s in said), said)

    def test_the_unemployed_are_outside_the_rule(self):
        said = time_ledger.verdict("NONE", shares(idle=100))
        self.assertFalse(any(s.startswith("REGRA 51") for s in said), said)


if __name__ == "__main__":
    unittest.main()
