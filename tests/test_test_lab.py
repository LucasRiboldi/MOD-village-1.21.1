"""Testes do Sentinela local sem iniciar Gradle."""

import importlib.util
import json
import os
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "test_lab.py"
SPEC = importlib.util.spec_from_file_location("test_lab", SCRIPT)
test_lab = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(test_lab)


class ArtifactCopyTest(unittest.TestCase):

    def test_copies_changed_same_size_file_even_when_timestamp_is_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            original_root = test_lab.ROOT
            test_lab.ROOT = pathlib.Path(directory)
            self.addCleanup(setattr, test_lab, "ROOT", original_root)
            report = test_lab.ROOT / "build" / "gametest-report.xml"
            report.parent.mkdir(parents=True)
            report.write_text("old", encoding="utf-8")
            before = test_lab.snapshot_artifacts()
            timestamp = report.stat().st_mtime_ns

            report.write_text("new", encoding="utf-8")
            os.utime(report, ns=(timestamp, timestamp))

            copied = test_lab.copy_changed_artifacts(before, test_lab.ROOT / "saved")

            self.assertEqual(["build/gametest-report.xml"], copied)
            self.assertEqual("new", (test_lab.ROOT / "saved" / "build" /
                                     "gametest-report.xml").read_text(encoding="utf-8"))


class ResumeMetadataTest(unittest.TestCase):

    def test_resume_records_time_in_metadata_file(self):
        with tempfile.TemporaryDirectory() as directory:
            original_root = test_lab.ROOT
            original_reports = test_lab.REPORTS
            test_lab.ROOT = pathlib.Path(directory)
            test_lab.REPORTS = test_lab.ROOT / "reports" / "test-runs"
            self.addCleanup(setattr, test_lab, "ROOT", original_root)
            self.addCleanup(setattr, test_lab, "REPORTS", original_reports)
            run_dir = test_lab.REPORTS / "interrupted"
            run_dir.mkdir(parents=True)
            metadata = {"git_commit": "abc"}
            report = {
                "profile": "quick", "repeat": 1, "metadata": metadata,
                "results": [
                    {"attempt": 1, "name": "unit", "status": "PASS", "seconds": 0,
                     "exit_code": 0},
                    {"attempt": 1, "name": "property", "status": "PASS", "seconds": 0,
                     "exit_code": 0},
                ], "status": "RUNNING",
            }
            (run_dir / "summary.json").write_text(json.dumps(report), encoding="utf-8")
            (run_dir / "metadata.json").write_text(json.dumps(metadata), encoding="utf-8")

            self.assertEqual(0, test_lab.main(["--resume", "interrupted"]))

            saved = json.loads((run_dir / "metadata.json").read_text(encoding="utf-8"))
            self.assertIn("resumed_at", saved)


class CoverageManifestTest(unittest.TestCase):

    def test_requires_a_test_and_minimum_profile_for_each_feature(self):
        with tempfile.TemporaryDirectory() as directory:
            manifest = pathlib.Path(directory) / "coverage.md"
            manifest.write_text("| feature | rule | test | profile |\n| --- | --- | --- | --- |\n"
                                "| watchdog | 1 | | quick |\n", encoding="utf-8")

            self.assertEqual(["coverage manifest line 3 needs test and profile"],
                             test_lab.coverage_errors(manifest))


if __name__ == "__main__":
    unittest.main()
