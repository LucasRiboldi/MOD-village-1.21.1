"""Compara os números escritos nos documentos com os números medidos.

Fontes medidas (precisam existir; rode antes check-tests-green.sh e o PIT):
  - unitários: soma de `tests` em build/test-results/test/*.xml
  - GameTests: maior "All N required tests passed" em build/audit/**/gametest*.log
    ou, na falta, os testcases de build/gametest-report.xml
  - Python: `python -m unittest discover -s tests` (rodado aqui, ~1 s)
  - PIT: build/reports/pitest/mutations.xml
  - versão: mod_version do gradle.properties

Afirmações lidas: README.md (contagens e badge de versão), STATE.md (linha
"JAR atual" e teto de 150 linhas que o próprio arquivo declara) e o topo do
CHANGELOG.md.

Uso: python -I -X utf8 scripts/audit/docs_drift.py [--out build/audit/docs-drift.json]
Saída 1 se houver qualquer divergência.
"""
from __future__ import annotations

import argparse
import glob
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read(rel: str) -> str:
    p = ROOT / rel
    return p.read_text(encoding='utf-8', errors='replace') if p.exists() else ''


def measured() -> dict:
    m: dict = {}
    xmls = glob.glob(str(ROOT / 'build/test-results/test/*.xml'))
    if xmls:
        m['unit'] = sum(int(ET.parse(x).getroot().get('tests')) for x in xmls)
    counts = []
    for log in glob.glob(str(ROOT / 'build/audit/**/gametest*.log'), recursive=True):
        counts += [int(n) for n in re.findall(r'All (\d+) required tests passed',
                                               Path(log).read_text(encoding='utf-8', errors='replace'))]
    if counts:
        m['gametest'] = max(counts)
    elif (ROOT / 'build/gametest-report.xml').exists():
        m['gametest'] = len(ET.parse(ROOT / 'build/gametest-report.xml').getroot().findall('.//testcase'))
    out = subprocess.run([sys.executable, '-I', '-m', 'unittest', 'discover', '-s', 'tests'],
                         cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace')
    ran = re.search(r'Ran (\d+) tests', out.stderr + out.stdout)
    if ran:
        m['python'] = int(ran.group(1))
    pit = ROOT / 'build/reports/pitest/mutations.xml'
    if pit.exists():
        st = [x.get('status') for x in ET.parse(pit).getroot()]
        killed = sum(s in ('KILLED', 'TIMED_OUT', 'MEMORY_ERROR') for s in st)
        m['pit'] = round(100 * killed / max(1, len(st)), 2)
        m['pit_counts'] = f'{killed}/{len(st)}'
    v = re.search(r'^mod_version=(\S+)', read('gradle.properties'), re.M)
    if v:
        m['version'] = v.group(1)
    return m


def claims() -> list[tuple[str, str, str, str]]:
    """(documento, o que, valor escrito, trecho)."""
    found = []
    readme = read('README.md')
    for kind, pat in (('unit', r'(\d[\d.]*) testes unitários'), ('python', r'(\d+) testes Python'),
                      ('gametest', r'(\d+) GameTests'), ('pit', r'mortas\s*\((\d+[,.]\d+)%')):
        for mt in re.finditer(pat, readme):
            line = readme[:mt.start()].count('\n') + 1
            found.append(('README.md:%d' % line, kind, mt.group(1).replace('.', '').replace(',', '.')
                          if kind != 'pit' else mt.group(1).replace(',', '.'), mt.group(0)))
    badge = re.search(r'Vers%C3%A3o-([\d.]+)', readme)
    if badge:
        found.append(('README.md (badge)', 'version', badge.group(1), badge.group(0)))
    state = read('STATE.md')
    jar = re.search(r'JAR atual: `village-colony-([\d.]+)\.jar`', state)
    if jar:
        found.append(('STATE.md (JAR atual)', 'version', jar.group(1), jar.group(0)))
    top = re.search(r'^## \[?v?([\d.]+)', read('CHANGELOG.md'), re.M)
    if top:
        found.append(('CHANGELOG.md (topo)', 'version', top.group(1), top.group(0)))
    return found


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('--out', default=str(ROOT / 'build/audit/docs-drift.json'))
    args = ap.parse_args()
    m = measured()
    rows = []
    for doc, kind, written, snippet in claims():
        real = m.get(kind)
        if real is None:
            rows.append((doc, kind, written, 'sem medida', 'não verificado'))
            continue
        ok = (abs(float(written) - float(real)) < 0.05) if kind == 'pit' else str(written) == str(real)
        rows.append((doc, kind, written, real, 'ok' if ok else 'DIVERGE'))
    state_lines = read('STATE.md').count('\n')
    rows.append(('STATE.md', 'linhas (teto declarado 150)', '150', state_lines,
                 'ok' if state_lines <= 150 else 'DIVERGE'))
    print('medido:', json.dumps(m, ensure_ascii=False))
    print(f"{'documento':28} {'o que':28} {'escrito':>9} {'medido':>9}  veredito")
    for doc, kind, w, r, v in rows:
        print(f'{doc:28} {kind:28} {str(w):>9} {str(r):>9}  {v}')
    Path(args.out).parent.mkdir(parents=True, exist_ok=True)
    Path(args.out).write_text(json.dumps({'measured': m, 'rows': rows}, ensure_ascii=False, indent=2),
                              encoding='utf-8')
    return 1 if any(r[4] == 'DIVERGE' for r in rows) else 0


if __name__ == '__main__':
    sys.exit(main())
