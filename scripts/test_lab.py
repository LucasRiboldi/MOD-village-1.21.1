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
import html
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
from xml.etree import ElementTree

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


def coverage_errors(path: Path | None = None) -> list[str]:
    """Cada regra do Sentinela precisa apontar teste e perfil manual mínimo."""
    manifest = path or ROOT / "docs" / "testing" / "sentinela-local-coverage.md"
    if not manifest.is_file():
        return [f"coverage manifest missing: {manifest}"]
    rows = [line for line in manifest.read_text(encoding="utf-8").splitlines()
            if line.startswith("|") and "---" not in line]
    if len(rows) < 2:
        return ["coverage manifest has no feature rows"]
    errors = []
    for number, row in enumerate(rows[1:], start=3):
        cells = [cell.strip() for cell in row.strip("|").split("|")]
        if len(cells) < 4 or not cells[2] or not cells[3]:
            errors.append(f"coverage manifest line {number} needs test and profile")
    return errors


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


def failure_details(case: ElementTree.Element) -> tuple[str, str]:
    problem = case.find("failure")
    if problem is None:
        problem = case.find("error")
    if problem is None:
        return "", ""
    message = (problem.get("message") or problem.text or "test failure").strip().splitlines()[0]
    kind = problem.get("type") or problem.tag
    return f"{kind}: {message}", message


def test_cases_from_artifacts(destination: Path, name: str, attempt: int) -> list[dict]:
    """Lê XML JUnit copiado; relatório ausente nunca invalida a execução Gradle."""
    cases = []
    for xml_path in destination.rglob("*.xml"):
        try:
            root = ElementTree.parse(xml_path).getroot()
        except ElementTree.ParseError:
            continue
        for case in root.iter("testcase"):
            signature, message = failure_details(case)
            state = "FAIL" if case.find("failure") is not None else "ERROR" if case.find("error") is not None else "SKIPPED" if case.find("skipped") is not None else "PASS"
            try:
                seconds = round(float(case.get("time", "0")), 3)
            except ValueError:
                seconds = 0.0
            cases.append({
                "suite": name,
                "attempt": attempt,
                "class_name": case.get("classname", "unknown"),
                "name": case.get("name", "unknown"),
                "status": state,
                "seconds": seconds,
                "message": message,
                "signature": signature,
                "source": str(xml_path.relative_to(destination)).replace("\\", "/"),
            })
    return cases


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
    artifact_dir = run_dir / "artifacts" / label
    artifacts = copy_changed_artifacts(before, artifact_dir)
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
        "test_cases": test_cases_from_artifacts(artifact_dir, name, attempt),
    }


def overall_status(results: list[dict]) -> str:
    by_suite: dict[str, set[str]] = {}
    for result in results:
        by_suite.setdefault(result["name"], set()).add(result["status"])
    if any("PASS" in statuses and len(statuses) > 1 for statuses in by_suite.values()):
        return "UNSTABLE"
    return "PASS" if all(result["status"] == "PASS" for result in results) else "FAIL"


def report_history() -> list[dict]:
    history = []
    for summary in sorted(REPORTS.glob("*/summary.json"), reverse=True)[:50]:
        try:
            report = json.loads(summary.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        history.append({
            "id": summary.parent.name,
            "status": report.get("status", "UNKNOWN"),
            "profile": report.get("profile", "unknown"),
            "seconds": round(sum(item.get("seconds", 0) for item in report.get("results", [])), 1),
        })
    return history


def write_dashboard(run_dir: Path, report: dict) -> None:
    cases = [case for result in report["results"] for case in result.get("test_cases", [])]
    counts = Counter(case["status"] for case in cases)
    failures = Counter(case["signature"] for case in cases if case["signature"])
    slowest = sorted(cases, key=lambda case: case["seconds"], reverse=True)[:20]
    result_rows = "".join(
        "<tr><td>{}</td><td>{}</td><td>{}</td><td>{:.1f}s</td><td><a href=\"{}\">stdout</a> · <a href=\"{}\">stderr</a></td></tr>".format(
            item["attempt"], html.escape(item["name"]), item["status"], item["seconds"],
            html.escape(item["stdout"]), html.escape(item["stderr"]))
        for item in report["results"])
    failure_rows = "".join("<tr><td>{}</td><td>{}</td></tr>".format(count, html.escape(signature))
                           for signature, count in failures.most_common(20)) or "<tr><td colspan=\"2\">Nenhuma falha XML registrada.</td></tr>"
    slow_rows = "".join("<tr><td>{:.3f}s</td><td>{}</td><td>{}</td><td>{}</td></tr>".format(
        item["seconds"], html.escape(item["suite"]), html.escape(item["class_name"]), html.escape(item["name"]))
        for item in slowest) or "<tr><td colspan=\"4\">Nenhum XML de teste foi copiado.</td></tr>"
    history_rows = "".join("<tr><td>{}</td><td>{}</td><td>{}</td><td>{:.1f}s</td></tr>".format(
        html.escape(item["id"]), item["status"], html.escape(item["profile"]), item["seconds"])
        for item in report_history())
    bars = "".join("<div class=\"bar {}\" style=\"width:{}px\">{} {}</div>".format(
        status.lower(), max(8, count * 18), status, count) for status, count in sorted(counts.items())) or "<p>Sem XML de casos nesta execução.</p>"
    document = f"""<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\"><title>Sentinela Observatório</title>
<style>body{{font:15px Segoe UI,Arial,sans-serif;margin:24px;color:#17202a;background:#f7f9fb}}h1,h2{{margin-bottom:8px}}section{{background:white;padding:16px;margin:16px 0;border:1px solid #d9e1e8;border-radius:8px}}table{{border-collapse:collapse;width:100%}}th,td{{padding:7px;border-bottom:1px solid #e5e9ed;text-align:left}}.bar{{color:white;margin:5px;padding:5px;border-radius:3px}}.pass{{background:#248a3d}}.fail,.error{{background:#b83232}}.skipped{{background:#777}}.interrupted{{background:#9b6c00}}a{{color:#0759b7}}</style>
<h1>Sentinela Observatório: {html.escape(report['status'])}</h1><p>Perfil: <b>{html.escape(report['profile'])}</b> · Repetições: {report['repeat']} · Casos XML: {len(cases)}</p>
<section><h2>Resultados de casos</h2>{bars}</section>
<section><h2>Suites</h2><table><tr><th>Tentativa</th><th>Suite</th><th>Status</th><th>Duração</th><th>Logs</th></tr>{result_rows}</table></section>
<section><h2>Falhas agrupadas por assinatura</h2><table><tr><th>Ocorrências</th><th>Assinatura</th></tr>{failure_rows}</table></section>
<section><h2>20 casos mais lentos</h2><table><tr><th>Duração</th><th>Suite</th><th>Classe</th><th>Teste</th></tr>{slow_rows}</table></section>
<section><h2>Histórico local (até 50 execuções)</h2><table><tr><th>Execução</th><th>Status</th><th>Perfil</th><th>Duração</th></tr>{history_rows}</table></section>
</html>"""
    (run_dir / "dashboard.html").write_text(document, encoding="utf-8")


def write_summary(run_dir: Path, report: dict) -> None:
    (run_dir / "summary.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n",
                                           encoding="utf-8")
    counts = Counter(result["status"] for result in report["results"])
    duration = round(sum(result["seconds"] for result in report["results"]), 1)
    lines = [f"# Sentinela Local: {report['status']}", "",
             f"Perfil: `{report['profile']}`  ", f"Repetições: {report['repeat']}  ",
             f"Suites escolhidas: `{', '.join(report.get('only', ())) or 'todas do perfil'}`  ",
             f"Commit: `{report['metadata']['git_commit']}`  ",
             f"Duração das suites: {duration}s  ",
             "Status: " + ", ".join(f"{status} {count}" for status, count in sorted(counts.items())),
             "", "## Suites", ""]
    for result in report["results"]:
        lines.append(f"- {result['status']}: tentativa {result['attempt']}, `{result['name']}` "
                     f"({result['seconds']}s, saída {result['exit_code']})")
    lines.extend(["", "Logs e artefatos desta execução ficam neste diretório.",
                  "Painel visual: `dashboard.html`.", ""])
    (run_dir / "summary.md").write_text("\n".join(lines), encoding="utf-8")
    write_dashboard(run_dir, report)


def selected_suites(profile: str, only: set[str] | None, parser: argparse.ArgumentParser) -> list[tuple[str, list[str]]]:
    suites = PROFILES[profile]
    available = {name for name, _ in suites}
    if only is None:
        return suites
    unknown = only - available
    if unknown:
        parser.error("--only includes suites unavailable for profile {}: {}".format(profile, ", ".join(sorted(unknown))))
    chosen = [(name, tasks) for name, tasks in suites if name in only]
    if not chosen:
        parser.error("--only must select at least one suite")
    return chosen


def planned_suites(profile: str, repeat: int, only: set[str] | None, parser: argparse.ArgumentParser) -> list[tuple[int, str, list[str]]]:
    return [(attempt, name, tasks) for attempt in range(1, repeat + 1)
            for name, tasks in selected_suites(profile, only, parser)]


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
    parser.add_argument("--budget-minutes", type=int, default=0, metavar="MIN",
                        help="parar antes da próxima suite ao atingir MIN; 0 não limita")
    parser.add_argument("--only", metavar="SUITES",
                        help="suites do perfil, separadas por vírgula")
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
    if args.budget_minutes < 0:
        parser.error("--budget-minutes cannot be negative")
    if args.resume and args.only:
        parser.error("--only cannot change a saved run; resume its original selection")
    if args.profile:
        errors = coverage_errors()
        if errors:
            for error in errors:
                print(error, file=sys.stderr)
            return 2
    if args.profile and args.dry_run:
        only = set(filter(None, (args.only or "").split(","))) or None
        for attempt, name, tasks in planned_suites(args.profile, args.repeat, only, parser):
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
        only = set(report.get("only", [])) or None
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
        only = set(filter(None, (args.only or "").split(","))) or None
        selected_suites(profile, only, parser)
        report = {"profile": profile, "repeat": repeat, "only": sorted(only or ()), "metadata": metadata,
                  "results": [], "status": "RUNNING"}
        (run_dir / "metadata.json").write_text(
            json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        write_summary(run_dir, report)

    suites = planned_suites(profile, repeat, only, parser)
    completed = {(result.get("attempt"), result.get("name")) for result in report["results"]}
    pending = [(attempt, name, tasks) for attempt, name, tasks in suites
               if (attempt, name) not in completed]
    if args.dry_run:
        for attempt, name, tasks in pending:
            print(f"attempt {attempt} {name}: {' '.join(tasks)}")
        return 0

    deadline = time.monotonic() + args.budget_minutes * 60 if args.budget_minutes else None
    for attempt, name, tasks in pending:
        if deadline is not None and time.monotonic() >= deadline:
            report["status"] = "INTERRUPTED"
            report["interrupted_reason"] = f"time budget reached before attempt {attempt} {name}"
            write_summary(run_dir, report)
            print(f"INTERRUPTED: {run_dir.relative_to(ROOT)}")
            return 2
        report["results"].append(run_suite(name, tasks, attempt, run_dir))
        report["status"] = "RUNNING"
        write_summary(run_dir, report)
    report["status"] = overall_status(report["results"])
    write_summary(run_dir, report)
    print(f"{report['status']}: {run_dir.relative_to(ROOT)}")
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
