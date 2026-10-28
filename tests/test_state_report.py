import importlib.util
import json
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "state_report.py"
SPEC = importlib.util.spec_from_file_location("state_report", SCRIPT)
state_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(state_report)


def state(tick, villagers, buildings, fill, rule51, stuck=0, mspt=18.0):
    return {"schema": 1, "mod": "0.3.102", "kind": "state", "tick": tick, "colony": "3fc51129", "biome": "savanna",
            "mspt": mspt,
            "people": {"villagers": villagers, "employed": villagers - 2, "badges": {"STUCK": stuck}},
            "time": {"MASON": {"rule51": rule51}, "NONE": {"rule51": 100}},
            "stock": {"fill": fill}, "build": {"buildings": buildings}}


class StateReportTest(unittest.TestCase):

    def test_reads_world_jsonl_and_log_lines(self):
        with tempfile.TemporaryDirectory() as folder:
            jsonl = pathlib.Path(folder) / "2026-10-28.jsonl"
            jsonl.write_text(json.dumps(state(0, 10, 4, 40, 10)) + "\n"
                             + json.dumps({"kind": "note", "colony": "3fc51129", "text": "obra torta"}) + "\n",
                             encoding="utf-8")
            log = pathlib.Path(folder) / "latest.log"
            log.write_text("[01:00:00] [Server thread/INFO]: VC_STATE " + json.dumps(state(72_000, 14, 10, 80, 30, 2))
                           + "\nlinha qualquer\n", encoding="utf-8")

            records = state_report.records_from(jsonl) + state_report.records_from(log)

        self.assertEqual(3, len(records))
        summary = state_report.summarize(records)["3fc51129"]
        self.assertEqual(2, summary["samples"])
        self.assertEqual((10, 14), summary["villagers"])
        self.assertEqual(6.0, summary["builds_per_hour"])
        self.assertEqual((40, 80), summary["fill"])
        self.assertEqual(20, summary["rule51"]["MASON"])
        self.assertNotIn("NONE", summary["rule51"], "quem não tem ofício fica fora da Regra 51")
        self.assertEqual(["obra torta"], summary["notes"])

    def test_render_flags_trades_over_rule_51(self):
        summary = state_report.summarize([state(0, 10, 4, 40, 10), state(72_000, 10, 5, 50, 40)])
        text = "\n".join(state_report.render(summary))

        self.assertIn("acima de 15%: MASON", text)
        self.assertIn("1.0 por hora", text)

    def test_reads_the_village_diary(self):
        day = dict(state(24_000, 12, 6, 50, 10), kind="day", ofDay=3,
                   dayTime={"MASON": {"rule51": 40}, "NONE": {"rule51": 100}})
        records = state_report.records_from_lines([
            "[x] VC_STATE " + json.dumps(state(0, 10, 4, 40, 10)),
            "[x] VC_DAY " + json.dumps(day),
            "[x] VC_EVENT " + json.dumps({"kind": "event", "type": "teleport", "colony": "3fc51129"}),
            "[x] VC_EVENT " + json.dumps({"kind": "event", "type": "teleport", "colony": "3fc51129"}),
            "[x] VC_AUDIT " + json.dumps({"kind": "audit", "colony": "3fc51129", "problems": 1, "beds": 5,
                                          "bedsInTheWorld": 2}),
            "[x] VC_AUDIT " + json.dumps({"kind": "audit", "colony": "3fc51129", "problems": 0}),
        ])
        summary = state_report.summarize(records)["3fc51129"]

        self.assertEqual(1, summary["samples"], "o resumo do dia não é um retrato a mais")
        self.assertEqual({"teleport": 2}, summary["events"])
        self.assertEqual(1, len(summary["audits"]), "só a auditoria com problema aparece")
        self.assertIn("dia 3", summary["days"][0])
        self.assertIn("MASON 40%", summary["days"][0])
        self.assertNotIn("NONE", summary["days"][0])

if __name__ == "__main__":
    unittest.main()
