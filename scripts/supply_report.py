"""Resume o suprimento excepcional de uma sessão — Regra 111.

Lê as linhas VC_SUPPLY do log do jogo (uma por vez em que algo surgiu pela lista declarada,
SupplyException) e soma por exceção e por item, para ver se alguma exceção virou fonte de recurso.

Uso:
    python scripts/supply_report.py                 # logs/latest.log da instalação de teste
    python scripts/supply_report.py caminho.log ...
"""

import os
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

LINE = re.compile(r"VC_SUPPLY version=1 exception=(\w+) rule=(\d+) colony=(\S+) item=(.+?) count=(\d+)\s*$")


def default_logs():
    appdata = os.environ.get("APPDATA")
    if not appdata:
        return []
    log = Path(appdata) / ".minecraft" / "logs" / "latest.log"
    return [log] if log.exists() else []


def parse(lines):
    """[(exceção, regra, colônia, item, quantidade)] das linhas VC_SUPPLY."""
    rows = []
    for line in lines:
        match = LINE.search(line)
        if match:
            exception, rule, colony, item, count = match.groups()
            rows.append((exception, int(rule), colony, item, int(count)))
    return rows


def summarize(rows):
    """{exceção: (regra, total, Counter por item)}."""
    totals = defaultdict(lambda: [0, 0, Counter()])
    for exception, rule, _colony, item, count in rows:
        entry = totals[exception]
        entry[0] = rule
        entry[1] += count
        entry[2][item] += count
    return totals


def render(totals):
    if not totals:
        return "Nenhum suprimento excepcional nesta sessão (sem linhas VC_SUPPLY)."
    out = ["Suprimento excepcional (Regra 111)", ""]
    for exception, (rule, total, items) in sorted(totals.items(), key=lambda kv: -kv[1][1]):
        top = ", ".join(f"{item} ×{n}" for item, n in items.most_common(5))
        out.append(f"  {exception:<20} Regra {rule:<4} {total:>6}  {top}")
    return "\n".join(out)


def main(argv):
    paths = [Path(p) for p in argv[1:]] or default_logs()
    if not paths:
        print("Nenhum log encontrado.")
        return 1
    lines = []
    for path in paths:
        lines.extend(path.read_text(encoding="utf-8", errors="replace").splitlines())
    print(render(summarize(parse(lines))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
