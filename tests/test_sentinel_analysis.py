"""Testes do analisador local do Sentinela, sem iniciar Gradle."""

import importlib.util
import json
import pathlib
import tempfile
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "sentinel_analysis.py"
SPEC = importlib.util.spec_from_file_location("sentinel_analysis", SCRIPT)
sentinel_analysis = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sentinel_analysis)
GUI_SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "Sentinela" / "sentinela_gui.py"
GUI_SPEC = importlib.util.spec_from_file_location("sentinela_gui", GUI_SCRIPT)
sentinela_gui = importlib.util.module_from_spec(GUI_SPEC)
GUI_SPEC.loader.exec_module(sentinela_gui)


class SentinelaAnalysisTest(unittest.TestCase):

    def test_gui_analysis_command_never_calls_gradle(self):
        command = sentinela_gui.analysis_command()

        self.assertEqual(str(sentinela_gui.ANALYSER), command[1])
        self.assertNotIn("gradlew", " ".join(command).lower())

    def test_groups_compile_failure_and_marks_later_pass_as_supersession(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            runs = root / "test-runs"
            first = runs / "20260101T000000Z"
            second = runs / "20260101T010000Z"
            first.mkdir(parents=True)
            second.mkdir(parents=True)
            log = "Task :compileJava FAILED\nBuilderWork.java:17: error: cannot find symbol\n"
            (first / "unit.stderr.log").write_text(log, encoding="utf-8")
            (first / "summary.json").write_text(json.dumps({
                "status": "FAIL", "results": [
                    {"name": "unit", "attempt": 1, "status": "FAIL", "seconds": 2,
                     "stderr": "unit.stderr.log", "test_cases": []},
                    {"name": "property", "attempt": 1, "status": "FAIL", "seconds": 2,
                     "stderr": "unit.stderr.log", "test_cases": []},
                ],
            }), encoding="utf-8")
            (second / "summary.json").write_text(json.dumps({
                "status": "PASS", "results": [
                    {"name": "unit", "attempt": 1, "status": "PASS", "seconds": 1,
                     "test_cases": []},
                ],
            }), encoding="utf-8")

            report = sentinel_analysis.analyse_reports(runs, root / "analysis")

            compile_findings = [item for item in report["findings"]
                                if item["category"] == "COMPILATION"]
            self.assertEqual(1, len(compile_findings))
            self.assertEqual(2, compile_findings[0]["occurrences"])
            self.assertEqual("20260101T010000Z", compile_findings[0]["superseded_by"])

    def test_reports_bad_json_and_missing_xml_without_stopping_analysis(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            runs = root / "test-runs"
            invalid = runs / "20260101T000000Z"
            incomplete = runs / "20260101T010000Z"
            invalid.mkdir(parents=True)
            incomplete.mkdir(parents=True)
            (invalid / "summary.json").write_text("{not json", encoding="utf-8")
            (incomplete / "summary.json").write_text(json.dumps({
                "status": "RUNNING", "results": [
                    {"name": "gametest", "attempt": 1, "status": "PASS", "seconds": 1,
                     "test_cases": []},
                ],
            }), encoding="utf-8")

            report = sentinel_analysis.analyse_reports(runs, root / "analysis")

            categories = {item["category"] for item in report["findings"]}
            self.assertTrue({"EVIDENCE_GAP", "INCOMPLETE"}.issubset(categories))
            output = pathlib.Path(report["output_dir"])
            self.assertTrue((output / "analysis.json").is_file())
            self.assertTrue((output / "analysis.md").is_file())
            self.assertTrue((output / "dashboard.html").is_file())

    def test_summarises_saved_suite_metrics_when_available(self):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            run = root / "test-runs" / "20260101T000000Z"
            run.mkdir(parents=True)
            (run / "summary.json").write_text(json.dumps({"status": "PASS", "results": []}), encoding="utf-8")
            (run / "metrics.json").write_text(json.dumps({"suites": [{
                "name": "unit", "metrics": {"samples": [
                    {"cpu_percent": 20.0, "ram_used_mib": 100.0, "java_working_set_mib": 50.0},
                    {"cpu_percent": 40.0, "ram_used_mib": 120.0, "java_working_set_mib": 60.0},
                ]},
            }]}), encoding="utf-8")

            report = sentinel_analysis.analyse_reports(root / "test-runs", root / "analysis")

            self.assertEqual(40.0, report["metrics"]["peak_cpu_percent"])
            self.assertEqual(120.0, report["metrics"]["peak_ram_used_mib"])


if __name__ == "__main__":
    unittest.main()
