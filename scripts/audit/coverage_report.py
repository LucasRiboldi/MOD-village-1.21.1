"""Cobertura de linha por pacote a partir do XML do JaCoCo.

Uso:
  python -I scripts/audit/coverage_report.py <jacoco.xml> [--min 60] [--critical core,data]
         [--out build/audit/coverage.json]

Imprime a cobertura de cada pacote, marca os que ficam abaixo do mínimo e sai
com 1 se algum pacote *crítico* (prefixo relativo a com/villagecolony) ficar
abaixo dele. Pacote não crítico abaixo do mínimo só é relatado: a camada
`fabric` é medida pela bateria de jogo, não pelo unitário.
"""
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET

PREFIX = 'com/villagecolony/'


def line_counter(node) -> tuple[int, int]:
    for c in node.findall('counter'):
        if c.get('type') == 'LINE':
            return int(c.get('missed')), int(c.get('covered'))
    return 0, 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('xml')
    ap.add_argument('--min', type=float, default=60.0)
    ap.add_argument('--critical', default='core,data')
    ap.add_argument('--out', default=None)
    args = ap.parse_args()

    root = ET.parse(args.xml).getroot()
    critical = tuple(p.strip() for p in args.critical.split(',') if p.strip())
    rows = []
    for pkg in root.findall('package'):
        name = pkg.get('name')
        missed, covered = line_counter(pkg)
        total = missed + covered
        if total == 0:
            continue
        short = (name[len(PREFIX):] if name.startswith(PREFIX) else name).replace('/', '.')
        rows.append({'package': short, 'lines': total,
                     'covered': covered, 'pct': round(100 * covered / total, 1),
                     'critical': short.startswith(critical)})
    rows.sort(key=lambda r: r['pct'])

    failing = [r for r in rows if r['critical'] and r['pct'] < args.min]
    print(f"{'pacote':45} {'linhas':>7} {'cob%':>6}  crítico")
    for r in rows:
        flag = ' <' if r['pct'] < args.min else ''
        print(f"{r['package']:45} {r['lines']:>7} {r['pct']:>6}  {'sim' if r['critical'] else '-'}{flag}")
    print(f"\nabaixo de {args.min}%: {sum(r['pct'] < args.min for r in rows)} pacotes; "
          f"críticos abaixo: {len(failing)}")
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            json.dump({'min': args.min, 'critical_prefixes': critical, 'packages': rows,
                       'critical_failing': [r['package'] for r in failing]}, f, indent=2)
    return 1 if failing else 0


if __name__ == '__main__':
    sys.exit(main())
