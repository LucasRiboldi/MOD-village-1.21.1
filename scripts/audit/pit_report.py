"""Placar do PIT e os mutantes sobreviventes mais relevantes.

Uso:
  python -I scripts/audit/pit_report.py <mutations.xml> [--threshold 85] [--top 20]
         [--out build/audit/pit.json]

Placar = mortos (KILLED, TIMED_OUT, MEMORY_ERROR) / todos, a mesma conta do
PIT e de scripts/assess. Ordena os sobreviventes por pacote crítico primeiro
(planejamento, construção, persistência), depois por classe com mais
sobreviventes. Sai com 1 se o placar ficar abaixo do limiar, e com 2 se o
XML não tiver mutação nenhuma (PIT que não rodou não passa).
"""
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from collections import Counter

KILLED = {'KILLED', 'TIMED_OUT', 'MEMORY_ERROR'}
CRITICAL = ('core.coordination', 'core.construction', 'data.save', 'core.worker', 'core.storage')


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument('xml')
    ap.add_argument('--threshold', type=float, default=85.0)
    ap.add_argument('--top', type=int, default=20)
    ap.add_argument('--out', default=None)
    args = ap.parse_args()

    muts = list(ET.parse(args.xml).getroot())
    if not muts:
        print('nenhuma mutação no XML: o PIT não rodou')
        return 2
    status = Counter(m.get('status') for m in muts)
    killed = sum(status[s] for s in KILLED)
    score = 100 * killed / len(muts)
    margin = killed - int(-(-args.threshold * len(muts) // 100))

    survivors = []
    for m in muts:
        if m.get('status') not in ('SURVIVED', 'NO_COVERAGE'):
            continue
        cls = m.findtext('mutatedClass', '')
        survivors.append({
            'status': m.get('status'),
            'class': cls.replace('com.villagecolony.', ''),
            'method': m.findtext('mutatedMethod', ''),
            'line': int(m.findtext('lineNumber', '0') or 0),
            'mutator': m.findtext('mutator', '').rsplit('.', 1)[-1],
            'description': m.findtext('description', ''),
        })
    per_class = Counter(s['class'] for s in survivors if s['status'] == 'SURVIVED')

    def rank(s):
        crit = 0 if s['class'].startswith(CRITICAL) else 1
        return (s['status'] != 'SURVIVED', crit, -per_class[s['class']], s['class'], s['line'])

    survivors.sort(key=rank)
    print(f'mutações {len(muts)}  mortas {killed}  placar {score:.2f}%  limiar {args.threshold}%  '
          f'folga {margin} mutantes')
    print('status:', dict(status))
    print('\nclasses com mais sobreviventes:')
    for cls, n in per_class.most_common(10):
        print(f'  {n:4}  {cls}')
    print(f'\ntop {args.top} sobreviventes (críticos primeiro):')
    for s in survivors[:args.top]:
        print(f"  {s['status']:11} {s['class']}.{s['method']}:{s['line']}  {s['mutator']}  {s['description']}")
    if args.out:
        with open(args.out, 'w', encoding='utf-8') as f:
            json.dump({'mutations': len(muts), 'killed': killed, 'score': round(score, 2),
                       'threshold': args.threshold, 'margin_mutants': margin, 'status': dict(status),
                       'per_class': per_class.most_common(), 'top': survivors[:args.top]},
                      f, indent=2, ensure_ascii=False)
    return 1 if score < args.threshold else 0


if __name__ == '__main__':
    sys.exit(main())
