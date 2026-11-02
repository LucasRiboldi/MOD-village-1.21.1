#!/usr/bin/env python3
"""Analisa evidências já gravadas pelo Sentinela Local, sem iniciar Gradle."""

from __future__ import annotations

import argparse
import html
import json
import re
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_RUNS = ROOT / "reports" / "test-runs"
DEFAULT_OUTPUT = ROOT / "reports" / "sentinel-analysis"


def _read_text(run_dir: Path, relative: str) -> str:
    path = run_dir / relative
    try:
        return path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def _signature(result: dict, run_dir: Path) -> tuple[str, str]:
    text = "\n".join(_read_text(run_dir, result.get(key, "")) for key in ("stdout", "stderr"))
    if "compileJava" in text or "compileGametestJava" in text or re.search(r"\.java:\d+: error:", text):
        error = re.search(r"error:\s*([^\n]+)", text)
        return "COMPILATION", "compile: " + (error.group(1).strip() if error else "Gradle Java compilation failed")
    failures = [case for case in result.get("test_cases", []) if case.get("status") in {"FAIL", "ERROR"}]
    if failures:
        case = failures[0]
        return "TEST", "test: {}.{}".format(case.get("class_name", "unknown"), case.get("name", "unknown"))
    if result.get("status") == "ERROR":
        return "INFRASTRUCTURE", result.get("error") or "suite process failed to start"
    return "INFRASTRUCTURE", "suite failed without classified evidence"


def _output_dir(output_root: Path) -> Path:
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    destination = output_root / stamp
    suffix = 1
    while destination.exists():
        suffix += 1
        destination = output_root / f"{stamp}-{suffix}"
    destination.mkdir(parents=True)
    return destination


def _write_outputs(destination: Path, report: dict) -> None:
    (destination / "analysis.json").write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    lines = ["# Análise do Sentinela Local", "", f"Execuções lidas: {report['summary']['runs_read']}",
             f"Achados: {len(report['findings'])}", "", "## Métricas locais", "",
             f"Amostras: {report['metrics']['samples']}; pico CPU: {report['metrics']['peak_cpu_percent']}; "
             f"pico RAM: {report['metrics']['peak_ram_used_mib']} MiB.", "", "## Achados", ""]
    for item in report["findings"]:
        suffix = f"; superado por {item['superseded_by']}" if item.get("superseded_by") else ""
        lines.append(f"- **{item['category']}** ({item['occurrences']}): {item['signature']}{suffix}")
    if not report["findings"]:
        lines.append("- Nenhuma anomalia encontrada nas evidências disponíveis.")
    lines.extend(["", "## Limites", "", "- A análise não inicia Gradle nem confirma comportamento visual no jogo.",
                  "- Sem XML copiado, o resultado Gradle é preservado, mas casos individuais ficam sem evidência.", ""])
    (destination / "analysis.md").write_text("\n".join(lines), encoding="utf-8")
    rows = "".join("<tr><td>{}</td><td>{}</td><td>{}</td><td>{}</td></tr>".format(
        html.escape(item["category"]), item["occurrences"], html.escape(item["signature"]),
        html.escape(item.get("superseded_by") or "—")) for item in report["findings"])
    document = """<!doctype html><meta charset=\"utf-8\"><title>Análise Sentinela</title>
<style>body{{font:15px Segoe UI,Arial;margin:24px;background:#f7f9fb;color:#17202a}}table{{border-collapse:collapse;width:100%;background:white}}th,td{{padding:8px;border:1px solid #d9e1e8;text-align:left}}</style>
<h1>Análise Sentinela Local</h1><p>Execuções lidas: {runs}; achados: {findings}.</p>
<p>Amostras locais: {samples}; pico CPU: {peak_cpu}; pico RAM: {peak_ram} MiB.</p>
<table><tr><th>Categoria</th><th>Ocorrências</th><th>Assinatura</th><th>Superado por</th></tr>{rows}</table>
<p>Este painel lê evidências locais. Não inicia Gradle.</p>""".format(
        runs=report["summary"]["runs_read"], findings=len(report["findings"]),
        samples=report["metrics"]["samples"], peak_cpu=report["metrics"]["peak_cpu_percent"],
        peak_ram=report["metrics"]["peak_ram_used_mib"], rows=rows or "<tr><td colspan=\"4\">Sem achados.</td></tr>")
    (destination / "dashboard.html").write_text(document, encoding="utf-8")


def analyse_reports(runs_root: Path, output_root: Path) -> dict:
    """Lê relatórios imutáveis e gera uma análise nova em ``output_root``."""
    findings: dict[tuple[str, str], dict] = {}
    runs_read = 0
    passing_suites: list[tuple[str, str]] = []
    metric_values = {"cpu_percent": [], "ram_used_mib": [], "java_working_set_mib": []}
    for run_dir in sorted(path for path in runs_root.iterdir() if path.is_dir()) if runs_root.exists() else ():
        summary = run_dir / "summary.json"
        if not summary.is_file():
            continue
        try:
            report = json.loads(summary.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            key = ("EVIDENCE_GAP", "invalid summary.json")
            findings.setdefault(key, {"category": key[0], "signature": key[1], "occurrences": 0, "runs": [], "superseded_by": None})
            findings[key]["occurrences"] += 1
            findings[key]["runs"].append(run_dir.name)
            continue
        runs_read += 1
        try:
            saved_metrics = json.loads((run_dir / "metrics.json").read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            saved_metrics = {}
        for suite in saved_metrics.get("suites", []):
            for sample in suite.get("metrics", {}).get("samples", []):
                for key, values in metric_values.items():
                    if isinstance(sample.get(key), (int, float)):
                        values.append(float(sample[key]))
        if report.get("status") == "RUNNING":
            key = ("INCOMPLETE", "run marked RUNNING; live process state is unavailable from saved evidence")
            findings.setdefault(key, {"category": key[0], "signature": key[1], "occurrences": 0, "runs": [], "superseded_by": None})
            findings[key]["occurrences"] += 1
            findings[key]["runs"].append(run_dir.name)
        for result in report.get("results", []):
            name = result.get("name", "unknown")
            if result.get("status") == "PASS":
                passing_suites.append((run_dir.name, name))
            elif result.get("status") in {"FAIL", "ERROR"}:
                category, signature = _signature(result, run_dir)
                key = (category, signature)
                findings.setdefault(key, {"category": category, "signature": signature, "occurrences": 0, "runs": [], "suites": [], "superseded_by": None})
                findings[key]["occurrences"] += 1
                findings[key]["runs"].append(run_dir.name)
                findings[key]["suites"].append(name)
            if not result.get("test_cases"):
                key = ("EVIDENCE_GAP", "no copied XML test cases for suite " + name)
                findings.setdefault(key, {"category": key[0], "signature": key[1], "occurrences": 0, "runs": [], "superseded_by": None})
                findings[key]["occurrences"] += 1
                findings[key]["runs"].append(run_dir.name)
    for finding in findings.values():
        suites = set(finding.get("suites", ()))
        if suites:
            later = [run_id for run_id, suite in passing_suites
                     if suite in suites and run_id > max(finding["runs"])]
            if later:
                finding["superseded_by"] = min(later)
    destination = _output_dir(output_root)
    report = {"generated_at": datetime.now(timezone.utc).isoformat(), "output_dir": str(destination),
              "summary": {"runs_read": runs_read, "findings_by_category": dict(Counter(item["category"] for item in findings.values()))},
              "metrics": {"samples": len(metric_values["cpu_percent"]),
                          "peak_cpu_percent": max(metric_values["cpu_percent"], default=None),
                          "peak_ram_used_mib": max(metric_values["ram_used_mib"], default=None),
                          "peak_java_working_set_mib": max(metric_values["java_working_set_mib"], default=None)},
              "findings": sorted(findings.values(), key=lambda item: (item["category"], item["signature"]))}
    _write_outputs(destination, report)
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runs-root", type=Path, default=DEFAULT_RUNS)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args(argv)
    report = analyse_reports(args.runs_root, args.output_root)
    print(f"Análise criada: {report['output_dir']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
