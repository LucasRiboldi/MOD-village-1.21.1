import importlib.util
import json
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "sim_gate.py"
SPEC = importlib.util.spec_from_file_location("sim_gate", SCRIPT)
sim_gate = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sim_gate)

LOG = """\
[00:00:01] [Server thread/INFO]: VC_TIME version=1 colony=aaaa window=300s MINER[s=300 work=50 walk=10 wait=10 blocked=20 idle=10 stranded=0] NONE[s=300 work=0 walk=0 wait=0 blocked=0 idle=100 stranded=0]
[00:00:02] [Server thread/INFO]: VC_TIME version=1 colony=aaaa window=300s FARMER[s=300 work=30 walk=10 wait=20 blocked=0 idle=30 stranded=10]
[00:00:03] [Server thread/INFO]: VC_ROAD_GRAPH colony=aaaa nodes=10 edges=9 toPave=0 isolated=1 offLine=5 capped=0 fixed=0 refused=0 loose=part:1@1,2,3
[00:00:04] [Server thread/INFO]: VC_ROAD_GRAPH colony=aaaa nodes=10 edges=9 toPave=0 isolated=1 offLine=5 capped=0 fixed=0 refused=0 loose=part:1@1,2,3
[00:00:05] [Server thread/INFO]: VC_ROAD_GRAPH colony=aaaa nodes=11 edges=10 toPave=0 isolated=0 offLine=5 capped=0 fixed=0 refused=0 loose=door@4,5,6;part:2@7,8,9
[00:00:06] [Server thread/INFO]: VC_SUPPLY version=1 exception=CLAY_ON_REQUEST rule=301 colony=aaaa item=clay count=16
[00:00:07] [Server thread/INFO]: VC_SUPPLY version=1 exception=BONE_MEAL_FOR_THE_FARM rule=310 colony=aaaa item=bone_meal count=16
[00:00:08] [Server thread/INFO]: Colony aaaa finished plains_small_house_1 at 1, 2, 3
[00:00:09] [Server thread/INFO]: Worker 0f86d566 is stranded at 96, 92, 311 — below its way
"""


class MeasureTest(unittest.TestCase):
    """O perfil longo do Sentinela (autor, 2026-11-18): os números saem do log da bancada."""

    def report(self, log=LOG, signals="alvo=plains dias=2 semente=20261020\n"):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = pathlib.Path(directory.name)
        (path / "latest.log").write_text(log, encoding="utf-8")
        (path / "signals.txt").write_text(signals, encoding="utf-8")

        return path

    def test_time_shares_split_no_task_from_stuck_and_leave_out_no_trade(self):
        metrics = sim_gate.measure(str(self.report()))

        self.assertEqual(set(metrics["por_profissao"]), {"MINER", "FARMER"})
        self.assertEqual(metrics["trabalho"], 40.0)
        self.assertEqual(metrics["travado"], 15.0)  # mineiro 20, fazendeiro 0 + 10
        self.assertEqual(metrics["sem_tarefa"], 20.0)
        self.assertEqual(metrics["espera"], 15.0)

    def test_graph_changes_count_only_reads_that_differ(self):
        metrics = sim_gate.measure(str(self.report()))

        self.assertEqual(metrics["grafo_dia"], 0.5)  # uma mudança em 2 dias
        self.assertEqual(metrics["soltas"], 2)

    def test_supply_builds_and_strandings_per_day(self):
        metrics = sim_gate.measure(str(self.report()))

        self.assertEqual(metrics["surgidos"], {"BONE_MEAL_FOR_THE_FARM": 16, "CLAY_ON_REQUEST": 16})
        self.assertEqual(metrics["surgidos_dia"], 16.0)
        self.assertEqual(metrics["obras_dia"], 0.5)
        self.assertEqual(metrics["encalhes_dia"], 0.5)

    def test_no_log_is_no_measure(self):
        with tempfile.TemporaryDirectory() as directory:
            self.assertIsNone(sim_gate.measure(directory))


class CompareTest(unittest.TestCase):

    def test_worse_beyond_slack_is_pior_and_within_is_igual(self):
        base = {"trabalho": 40.0, "travado": 10.0, "grafo_dia": 5.0}
        now = {"trabalho": 30.0, "travado": 11.0, "grafo_dia": 1.0}
        verdicts = {name: verdict for name, _, _, verdict in sim_gate.compare(now, base)}

        self.assertEqual(verdicts["trabalho"], "pior")
        self.assertEqual(verdicts["travado"], "igual")
        self.assertEqual(verdicts["grafo_dia"], "melhor")
        self.assertEqual(verdicts["sem_tarefa"], "info")

    def test_base_from_another_seed_or_length_is_not_comparable(self):
        self.assertTrue(sim_gate.comparable({"semente": "1", "dias": 3}, {"semente": "1", "dias": 3}))
        self.assertFalse(sim_gate.comparable({"semente": "1", "dias": 3}, {"semente": "2", "dias": 3}))
        self.assertFalse(sim_gate.comparable({"semente": "1", "dias": 3}, {"semente": "1", "dias": 2}))
        self.assertFalse(sim_gate.comparable({"semente": "1", "dias": 3}, None))


class MainTest(unittest.TestCase):

    def test_writes_baseline_then_fails_only_when_a_number_gets_worse(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            target = root / "reports" / "plains"
            target.mkdir(parents=True)
            (target / "latest.log").write_text(LOG, encoding="utf-8")
            (target / "signals.txt").write_text("dias=2 semente=7\n", encoding="utf-8")
            baseline = root / "base.json"
            args = ["plains", "--reports", str(root / "reports"), "--baseline", str(baseline)]

            self.assertEqual(sim_gate.main(args + ["--write-baseline"]), 0)
            self.assertIn("plains", json.loads(baseline.read_text(encoding="utf-8")))
            self.assertEqual(sim_gate.main(args), 0)

            worse = LOG.replace("work=50 walk=10 wait=10 blocked=20", "work=10 walk=10 wait=10 blocked=60")
            (target / "latest.log").write_text(worse, encoding="utf-8")

            self.assertEqual(sim_gate.main(args), 1)


if __name__ == "__main__":
    unittest.main()
