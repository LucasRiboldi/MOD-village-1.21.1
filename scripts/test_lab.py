#!/usr/bin/env python3
"""Varredura manual de testes locais do Village Colony.

Uso: python scripts/test_lab.py {quick,fast,full} [--repeat N] [--dry-run]
     python scripts/test_lab.py --list
     python scripts/test_lab.py --resume <UTC-id>

Não agenda execução, não altera CI e não decide se uma regra de gameplay foi
cumprida. Ele apenas preserva evidência dos comandos Gradle já usados pelo
projeto. Falha que passa em outra repetição é UNSTABLE, nunca verde.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import shutil
import socket
import subprocess
import sys
import time
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REPORTS = ROOT / "reports" / "test-runs"
GRADLEW = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")

PROFILES = {
    "quick": [("unit", ["test"]), ("property", ["propertyTest"])],
    "fast": [
        ("unit", ["test"]),
        ("property", ["propertyTest"]),
        ("gametest", ["runGametest", "--rerun-tasks"]),
    ],
    "full": [
        ("unit", ["test"]),
        ("property", ["propertyTest"]),
        ("gametest", ["runGametest", "--rerun-tasks"]),
        ("gametest-audit", ["runGametest", "--rerun-tasks", "-PgametestAudit=only"]),
        ("mutation", ["pitest"]),
    ],
}

ARTIFACTS = (
    Path("build/test-results/test"),
    Path("build/test-results/propertyTest"),
    Path("build/gametest-report.xml"),
    Path("build/reports/jacoco/test"),
    Path("build/reports/jacoco/gametest"),
    Path("build/reports/pitest"),
)


def git(*args: str) -> str:
    completed = subprocess.run(["git", *args], cwd=ROOT, text=True,
                               capture_output=True, errors="replace")
    return completed.stdout.strip() if completed.returncode == 0 else ""


def command_version(command: list[str]) -> str:
    try:
        completed = subprocess.run(command, cwd=ROOT, text=True, capture_output=True,
                                   errors="replace", timeout=30)
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return "unavailable"
    return (completed.stdout or completed.stderr).strip().splitlines()[0]


def file_fingerprint(path: Path) -> tuple[int, int, str]:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    stat = path.stat()
    return stat.st_mtime_ns, stat.st_size, digest.hexdigest()


def artifact_fingerprint(path: Path) -> dict[str, tuple[int, int, str]] | None:
    if not path.exists():
        return None
    if path.is_file():
        return {".": file_fingerprint(path)}
    return {
        str(child.relative_to(path)).replace("\\", "/"): file_fingerprint(child)
        for child in path.rglob("*") if child.is_file()
    }


def snapshot_artifacts() -> dict[Path, dict[str, tuple[int, int, str]] | None]:
    return {path: artifact_fingerprint(ROOT / path) for path in ARTIFACTS}


def copy_changed_artifacts(
        before: dict[Path, dict[str, tuple[int, int, str]] | None], destination: Path) -> list[str]:
    copied = []
    for relative, previous_fingerprint in before.items():
        source = ROOT / relative
        if not source.exists() or artifact_fingerprint(source) == previous_fingerprint:
            continue
        target = destination / relative
        if source.is_dir():
            shutil.copytree(source, target, dirs_exist_ok=True)
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
        copied.append(str(relative).replace("\\", "/"))
    return copied


def run_suite(name: str, tasks: list[str], attempt: int, run_dir: Path) -> dict:
    suite_dir = run_dir / "suites"
    suite_dir.mkdir(exist_ok=True)
    label = f"attempt-{attempt:02d}-{name}"
    stdout_path = suite_dir / f"{label}.stdout.log"
    stderr_path = suite_dir / f"{label}.stderr.log"
    command = [str(GRADLEW), *tasks, "--console=plain"]
    before = snapshot_artifacts()
    started = time.monotonic()
    try:
        with stdout_path.open("w", encoding="utf-8") as stdout, stderr_path.open(
                "w", encoding="utf-8") as stderr:
            completed = subprocess.run(command, cwd=ROOT, stdout=stdout, stderr=stderr,
                                       text=True, errors="replace")
        exit_code = completed.returncode
        error = ""
    except FileNotFoundError as missing:
        exit_code = None
        error = f"command missing: {missing}"
        stderr_path.write_text(error + "\n", encoding="utf-8")
    elapsed = round(time.monotonic() - started, 1)
    status = "PASS" if exit_code == 0 else "FAIL" if exit_code is not None else "ERROR"
    artifacts = copy_changed_artifacts(before, run_dir / "artifacts" / label)
    return {
        "name": name,
        "attempt": attempt,
        "command": command,
        "status": status,
        "exit_code": exit_code,
        "seconds": elapsed,
        "error": error,
        "stdout": str(stdout_path.relative_to(run_dir)).replace("\\", "/"),
        "stderr": str(stderr_path.relative_to(run_dir)).replace("\\", "/"),
        "artifacts": artifacts,
    }


def overall_status(results: list[dict]) -> str:
    by_suite: dict[str, set[str]] = {}
    for result in results:
        by_suite.setdefault(result["name"], set()).add(result["status"])
    if any("PASS" in statuses and len(statuses) > 1 for statuses in by_suite.values()):
        return "UNSTABLE"
    return "PASS" if all(result["status"] == "PASS" for result in results) else "FAIL"


def write_summary(run_dir: Path, report: dict) -> None:
    (run_dir / "summary.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n",
                                           encoding="utf-8")
    counts = Counter(result["status"] for result in report["results"])
    duration = round(sum(result["seconds"] for result in report["results"]), 1)
    lines = [f"# Sentinela Local: {report['status']}", "",
             f"Perfil: `{report['profile']}`  ", f"Repetições: {report['repeat']}  ",
             f"Commit: `{report['metadata']['git_commit']}`  ",
             f"Duração das suites: {duration}s  ",
             "Status: " + ", ".join(f"{status} {count}" for status, count in sorted(counts.items())),
             "", "## Suites", ""]
    for result in report["results"]:
        lines.append(f"- {result['status']}: tentativa {result['attempt']}, `{result['name']}` "
                     f"({result['seconds']}s, saída {result['exit_code']})")
    lines.extend(["", "Logs e artefatos desta execução ficam neste diretório.", ""])
    (run_dir / "summary.md").write_text("\n".join(lines), encoding="utf-8")


def planned_suites(profile: str, repeat: int) -> list[tuple[int, str, list[str]]]:
    return [(attempt, name, tasks) for attempt in range(1, repeat + 1)
            for name, tasks in PROFILES[profile]]


def run_directory(run_id: str, parser: argparse.ArgumentParser) -> Path:
    if Path(run_id).name != run_id:
        parser.error("--resume accepts only a run directory name")
    run_dir = REPORTS / run_id
    if not (run_dir / "summary.json").is_file():
        parser.error(f"saved run not found: {run_dir}")
    return run_dir


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", choices=PROFILES, nargs="?")
    parser.add_argument("--repeat", type=int, default=1, metavar="N")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--list", action="store_true", help="list profiles without running Gradle")
    parser.add_argument("--resume", metavar="RUN_ID", help="continue a saved interrupted run")
    args = parser.parse_args(argv)
    if args.list:
        for profile, suites in PROFILES.items():
            print(f"{profile}: " + "; ".join(f"{name}={' '.join(tasks)}" for name, tasks in suites))
        return 0
    if bool(args.profile) == bool(args.resume):
        parser.error("provide one profile, or --resume RUN_ID")
    if args.repeat < 1:
        parser.error("--repeat must be at least 1")
    if args.profile and args.dry_run:
        for attempt, name, tasks in planned_suites(args.profile, args.repeat):
            print(f"attempt {attempt} {name}: {' '.join(tasks)}")
        return 0

    if args.resume:
        run_dir = run_directory(args.resume, parser)
        report = json.loads((run_dir / "summary.json").read_text(encoding="utf-8"))
        profile = report.get("profile")
        repeat = report.get("repeat")
        if profile not in PROFILES or not isinstance(repeat, int) or repeat < 1:
            parser.error("saved run has invalid profile or repeat")
        report.setdefault("results", [])
        report.setdefault("metadata", {})
        report["metadata"].setdefault("resumed_at", []).append(datetime.now(timezone.utc).isoformat())
        (run_dir / "metadata.json").write_text(
            json.dumps(report["metadata"], indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    else:
        profile = args.profile
        repeat = args.repeat
        run_id = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        run_dir = REPORTS / run_id
        suffix = 1
        while run_dir.exists():
            suffix += 1
            run_dir = REPORTS / f"{run_id}-{suffix}"
        run_dir.mkdir(parents=True)
        metadata = {
            "started_at": datetime.now(timezone.utc).isoformat(),
            "git_commit": git("rev-parse", "HEAD"),
            "git_dirty": bool(git("status", "--porcelain")),
            "java": command_version(["java", "-version"]),
            "gradle": command_version([str(GRADLEW), "--version"]),
            "host": socket.gethostname(),
            "platform": platform.platform(),
        }
        report = {"profile": profile, "repeat": repeat, "metadata": metadata,
                  "results": [], "status": "RUNNING"}
        (run_dir / "metadata.json").write_text(
            json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_summary(run_dir, report)

    suites = planned_suites(profile, repeat)
    completed = {(result.get("attempt"), result.get("name")) for result in report["results"]}
    pending = [(attempt, name, tasks) for attempt, name, tasks in suites
               if (attempt, name) not in completed]
    if args.dry_run:
        for attempt, name, tasks in pending:
            print(f"attempt {attempt} {name}: {' '.join(tasks)}")
        return 0

    for attempt, name, tasks in pending:
        report["results"].append(run_suite(name, tasks, attempt, run_dir))
        report["status"] = "RUNNING"
        write_summary(run_dir, report)
    report["status"] = overall_status(report["results"])
    write_summary(run_dir, report)
    print(f"{report['status']}: {run_dir.relative_to(ROOT)}")
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
