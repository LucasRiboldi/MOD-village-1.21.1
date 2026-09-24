#!/usr/bin/env python3
"""Coleta as metricas da avaliacao tecnica e aplica a regua de notas.

Metodologia: docs/technical/avaliacao/METODOLOGIA.md. Este script e a parte
reproduzivel dela: mede, pontua com os limiares da metodologia e grava um
retrato comparavel com os anteriores.

Uso:
    python scripts/assess/assess_project.py --run            # roda build, PIT e GameTest antes
    python scripts/assess/assess_project.py                  # so le os artefatos ja gerados
    python scripts/assess/assess_project.py --gametest-runs 3 --run
    python scripts/assess/assess_project.py --compare A.json B.json

Saida: docs/technical/avaliacao/<data>-<commit>/metricas.json e scorecard.md
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import java_metrics as jm  # noqa: E402
from rubric import CRITERIA, grade, score_all  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs' / 'technical' / 'avaliacao'
LOGS = ROOT / 'build' / 'assess'
GRADLEW = str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew'))


def sh(*args: str) -> str:
    return subprocess.run(list(args), cwd=ROOT, capture_output=True, text=True,
                          encoding='utf-8', errors='replace').stdout.strip()


def gradle(log: str, *tasks: str) -> int:
    env = dict(os.environ)
    env.setdefault('JAVA_HOME', r'C:\Program Files\Java\jdk-21.0.12')
    LOGS.mkdir(parents=True, exist_ok=True)
    with open(LOGS / log, 'w', encoding='utf-8') as out:
        return subprocess.run([GRADLEW, *tasks], cwd=ROOT, env=env, stdout=out,
                              stderr=subprocess.STDOUT).returncode


def run_everything(gametest_runs: int) -> dict:
    codes = {'build': gradle('build.log', 'build', '--rerun-tasks'),
             'pitest': gradle('pitest.log', 'pitest')}
    runs = []
    for i in range(gametest_runs):
        gradle(f'gametest-{i + 1}.log', 'runGametest', '--rerun-tasks')
        runs.append(gametest_summary(LOGS / f'gametest-{i + 1}.log'))
    return {'exit_codes': codes, 'gametest_runs': runs}


def gametest_summary(log: Path) -> dict:
    text = log.read_text(encoding='utf-8', errors='replace') if log.exists() else ''
    total = re.search(r'(\d+) GAME TESTS COMPLETE IN ([\d.]+) (s|min)', text)
    failed = re.search(r'(\d+) required tests failed', text)
    names = sorted(set(re.findall(r'invoking test method (\w+) in', text)))
    secs = None
    if total:
        secs = float(total.group(2)) * (60 if total.group(3) == 'min' else 1)
    return {'total': int(total.group(1)) if total else None,
            'failed': int(failed.group(1)) if failed else 0,
            'seconds': secs, 'failing_tests': names}


def unit_tests() -> dict:
    tests = fails = 0
    secs = 0.0
    for f in glob.glob(str(ROOT / 'build/test-results/test/*.xml')):
        s = ET.parse(f).getroot()
        tests += int(s.get('tests', 0))
        fails += int(s.get('failures', 0)) + int(s.get('errors', 0))
        secs += float(s.get('time', 0))
    return {'tests': tests, 'failures': fails, 'seconds': round(secs, 1)}


def jacoco() -> dict:
    f = ROOT / 'build/reports/jacoco/test/jacocoTestReport.xml'
    if not f.exists():
        return {}
    root = ET.parse(f).getroot()
    result = {}
    for c in root.findall('counter'):
        missed, covered = int(c.get('missed')), int(c.get('covered'))
        result[c.get('type').lower()] = round(100 * covered / max(1, missed + covered), 1)
    # Por camada: o teste unitario e o instrumento do core e do data; a camada
    # fabric e exercida pelo GameTest, que o JaCoCo desta configuracao nao mede.
    layers: dict[str, list[int]] = {}
    for pkg in root.findall('package'):
        parts = pkg.get('name').split('/')
        layer = parts[2] if len(parts) > 2 else 'root'
        for c in pkg.findall('counter'):
            if c.get('type') == 'LINE':
                acc = layers.setdefault(layer, [0, 0])
                acc[0] += int(c.get('missed'))
                acc[1] += int(c.get('covered'))
    result['line_by_layer'] = {k: round(100 * cov / max(1, mis + cov), 1) for k, (mis, cov) in layers.items()}
    core_data = [layers.get(k, [0, 0]) for k in ('core', 'data')]
    missed, covered = sum(x[0] for x in core_data), sum(x[1] for x in core_data)
    result['line_core_data'] = round(100 * covered / max(1, missed + covered), 1)
    return result


def pitest() -> dict:
    f = ROOT / 'build/reports/pitest/mutations.xml'
    if not f.exists():
        return {}
    status = [m.get('status') for m in ET.parse(f).getroot()]
    total = len(status)
    killed = sum(s in ('KILLED', 'TIMED_OUT', 'MEMORY_ERROR') for s in status)
    no_cov = status.count('NO_COVERAGE')
    covered = total - no_cov
    return {'mutations': total, 'killed': killed, 'survived': status.count('SURVIVED'),
            'no_coverage': no_cov, 'score': round(100 * killed / max(1, total), 1),
            'strength': round(100 * killed / max(1, covered), 1)}


def error_prone() -> dict:
    log = LOGS / 'build.log'
    if not log.exists():
        return {}
    text = log.read_text(encoding='utf-8', errors='replace').replace('\n', '')
    kinds: dict[str, int] = {}
    for k in re.findall(r'warning:\s*\[(\w+)\]', text):
        kinds[k] = kinds.get(k, 0) + 1
    return {'warnings': sum(kinds.values()), 'by_kind': dict(sorted(kinds.items(), key=lambda e: -e[1]))}


def source_metrics() -> dict:
    main = jm.scan(ROOT, 'src/main/java')
    tests = jm.scan(ROOT, 'src/test/java') + jm.scan(ROOT, 'src/gametest/java')
    methods = [m for f in main for m in f.methods]
    sizes = [m.lines for m in methods]
    cc = [m.complexity for m in methods]
    code = sum(f.code_lines for f in main)
    by_layer: dict[str, dict] = {}
    for f in main:
        d = by_layer.setdefault(f.layer, {'files': 0, 'code_lines': 0})
        d['files'] += 1
        d['code_lines'] += f.code_lines
    violations = []
    for f in main:
        if f.layer == 'core':
            bad = [i for i in f.imports if i.startswith(('net.minecraft', 'net.fabricmc',
                                                          'com.villagecolony.fabric', 'com.villagecolony.data'))]
            violations += [f'{f.path} -> {b}' for b in bad]
    worst = sorted(((m.complexity, m.lines, f.path, m.name) for f in main for m in f.methods), reverse=True)[:10]
    return {
        'files': len(main), 'lines': sum(f.lines for f in main), 'code_lines': code,
        'comment_lines': sum(f.comment_lines for f in main),
        'comment_ratio': round(sum(f.comment_lines for f in main) / max(1, code), 2),
        'files_over_500': [f'{f.path} ({f.lines})' for f in main if f.lines > 500],
        'max_file_lines': max(f.lines for f in main),
        'methods': len(methods), 'method_lines_p50': jm.percentile(sizes, .5),
        'method_lines_p90': jm.percentile(sizes, .9), 'method_lines_max': max(sizes),
        'methods_over_50_lines': sum(s > 50 for s in sizes),
        'cc_mean': round(sum(cc) / max(1, len(cc)), 2), 'cc_p90': jm.percentile(cc, .9),
        'cc_max': max(cc), 'methods_cc_over_10': sum(c > 10 for c in cc),
        'methods_cc_over_20': sum(c > 20 for c in cc),
        'pct_cc_over_10': round(100 * sum(c > 10 for c in cc) / max(1, len(cc)), 1),
        'mutable_statics': sum(f.mutable_statics for f in main),
        'mutable_statics_per_kloc': round(1000 * sum(f.mutable_statics for f in main) / max(1, code), 2),
        'layers': by_layer, 'core_layer_violations': violations,
        'most_complex': [{'cc': c, 'lines': l, 'file': p, 'method': n} for c, l, p, n in worst],
        'test_files': len(tests), 'test_code_lines': sum(f.code_lines for f in tests),
        'test_to_main_ratio': round(sum(f.code_lines for f in tests) / max(1, code), 2),
    }


def records() -> dict:
    todo = (ROOT / 'TODO.md').read_text(encoding='utf-8', errors='replace')
    open_items = re.findall(r'^\s*- \[ \]\s*(\S+)', todo, re.M)
    return {'adrs': len(glob.glob(str(ROOT / 'docs/decisions/ADR-*.md'))),
            'docs_md': len(glob.glob(str(ROOT / 'docs/**/*.md'), recursive=True)),
            'todo_open': len(open_items),
            'todo_open_critical': sum(1 for i in open_items if '🔴' in i),
            'todo_done': len(re.findall(r'^\s*- \[x\]', todo, re.M)),
            'has_readme': (ROOT / 'README.md').exists(),
            'ci_workflow': (ROOT / '.github/workflows/ci.yml').exists()}


def process() -> dict:
    ci = (ROOT / '.github/workflows/ci.yml').read_text(encoding='utf-8') if (ROOT / '.github/workflows/ci.yml').exists() else ''
    build = (ROOT / 'build.gradle').read_text(encoding='utf-8')
    practices = {'ci': bool(ci), 'unit_tests_in_ci': 'gradlew build' in ci,
                 'gametests_in_ci': 'runGametest' in ci, 'static_analysis': 'errorprone' in build,
                 'mutation_testing': 'pitest' in build, 'coverage': 'jacoco' in build,
                 'commit_hooks': (ROOT / 'scripts/hooks/commit_gate.py').exists()}
    subjects = sh('git', 'log', '-300', '--format=%s').splitlines()
    kinds: dict[str, int] = {}
    for s in subjects:
        k = (re.match(r'(\w+)[:(]', s) or re.match(r'(E\d+|N\d+|P\d)', s))
        key = k.group(1) if k else 'outro'
        kinds[key] = kinds.get(key, 0) + 1
    return {'practices': practices,
            'commits_total': int(sh('git', 'rev-list', '--count', 'HEAD') or 0),
            'commits_30_days': int(sh('git', 'rev-list', '--count', '--since=30.days', 'HEAD') or 0),
            'last_300_by_prefix': dict(sorted(kinds.items(), key=lambda e: -e[1])[:12])}


def game_performance() -> dict:
    f = ROOT / 'docs/technical/Log-Stall-History.json'
    if not f.exists():
        return {}
    session = json.loads(f.read_text(encoding='utf-8'))['sessions'][-1]
    obs = {k: v['occurrences'] for k, v in session['observations'].items()}
    hours = None
    if session.get('started_at') and session.get('ended_at'):
        a = datetime.strptime(session['started_at'], '%H:%M:%S')
        b = datetime.strptime(session['ended_at'], '%H:%M:%S')
        hours = round(((b - a).seconds or 1) / 3600, 2)
    return {'session_hours': hours, 'observations': obs, 'lines': session.get('lines')}


def collect(args) -> dict:
    ran = run_everything(args.gametest_runs) if args.run else {}
    runs = ran.get('gametest_runs') or [gametest_summary(p) for p in sorted(LOGS.glob('gametest-*.log'))]
    data = {
        'schema': 1,
        'generated_at': datetime.now(timezone.utc).isoformat(timespec='seconds'),
        'commit': sh('git', 'rev-parse', '--short', 'HEAD'),
        'branch': sh('git', 'rev-parse', '--abbrev-ref', 'HEAD'),
        'source': source_metrics(), 'unit_tests': unit_tests(), 'gametests': runs,
        'coverage': jacoco(), 'mutation': pitest(), 'static_analysis': error_prone(),
        'records': records(), 'process': process(), 'game_performance': game_performance(),
        'exit_codes': ran.get('exit_codes', {}),
    }
    data['scores'] = score_all(data)
    return data


def scorecard(data: dict) -> str:
    rows = [f"| {c['id']} | {c['name']} | {data['scores'][c['id']]['value']} | "
            f"**{data['scores'][c['id']]['score']}** / 4 |" for c in CRITERIA]
    total = data['scores']['_overall']
    return (f"# Scorecard — {data['generated_at'][:10]} ({data['commit']})\n\n"
            f"Gerado por `scripts/assess/assess_project.py`; régua em `METODOLOGIA.md`.\n\n"
            f"| # | Critério | Medida | Nota |\n|---|---|---|---|\n" + '\n'.join(rows) +
            f"\n\n**Média: {total['mean']} / 4 — conceito {total['grade']}**\n")


def compare(a: Path, b: Path) -> str:
    x, y = json.loads(a.read_text(encoding='utf-8')), json.loads(b.read_text(encoding='utf-8'))
    lines = [f"| Critério | {x['commit']} | {y['commit']} | Δ |", '|---|---|---|---|']
    for c in CRITERIA:
        sa = x['scores'].get(c['id'], {}).get('score')
        sb = y['scores'].get(c['id'], {}).get('score')
        delta = f'{sb - sa:+d}' if sa is not None and sb is not None else '—'
        lines.append(f"| {c['name']} | {sa} | {sb} | {delta} |")
    ma, mb = x['scores']['_overall']['mean'], y['scores']['_overall']['mean']
    lines.append(f"| **Média** | {ma} | {mb} | {mb - ma:+.2f} |")
    return '\n'.join(lines)


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--run', action='store_true', help='roda build, pitest e runGametest antes de medir')
    p.add_argument('--gametest-runs', type=int, default=1)
    p.add_argument('--compare', nargs=2, type=Path, metavar=('ANTES', 'DEPOIS'))
    args = p.parse_args()
    if args.compare:
        print(compare(*args.compare))
        return 0
    data = collect(args)
    folder = OUT / f"{data['generated_at'][:10]}-{data['commit']}"
    folder.mkdir(parents=True, exist_ok=True)
    (folder / 'metricas.json').write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    (folder / 'scorecard.md').write_text(scorecard(data), encoding='utf-8')
    print(scorecard(data))
    print(f'gravado em {folder.relative_to(ROOT)}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
