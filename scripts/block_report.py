"""O diário dos blocos (Regra 384): o que o jogador e cada sistema do mod mudaram, e onde os dois mexeram no mesmo bloco.

Uso: python scripts/block_report.py [arquivo .jsonl]  (sem argumento, o mais novo de %APPDATA%/.minecraft/logs)
"""
import collections
import glob
import json
import os
import sys


def latest():
    files = glob.glob(os.path.join(os.environ.get("APPDATA", ""), ".minecraft", "logs", "villagecolony-blocks-*.jsonl"))
    return max(files, key=os.path.getmtime) if files else None


def load(path):
    with open(path, encoding="utf-8") as handle:
        return [json.loads(line) for line in handle if line.strip()]


def summarize(rows):
    """Quem mudou quantos blocos, e os blocos que o jogador e o mod mudaram os dois, na ordem em que aconteceu."""
    by_who = collections.Counter(row["who"] for row in rows)
    touched = collections.defaultdict(list)
    for row in rows:
        touched[(row["x"], row["y"], row["z"])].append(row)
    both = {pos: events for pos, events in touched.items()
            if any(e["who"] == "player" for e in events) and any(e["who"].startswith("mod:") for e in events)}
    return {"by_who": dict(by_who), "both": both}


def lines(summary, limit=20):
    out = ["Blocos mudados (Regra 384)", ""]
    for who, count in sorted(summary["by_who"].items(), key=lambda item: -item[1]):
        out.append(f"  {count:6d}  {who}")
    out.append("")
    out.append(f"Blocos que o jogador e o mod mudaram os dois: {len(summary['both'])}")
    for pos, events in list(summary["both"].items())[:limit]:
        trail = " → ".join(f"{e['who']}:{e['now'].split(':')[-1]}" for e in events)
        out.append(f"  {pos[0]},{pos[1]},{pos[2]}  {trail}")
    return out


def main(argv):
    path = argv[1] if len(argv) > 1 else latest()
    if not path:
        print("nenhum diário de blocos achado")
        return 0
    print("\n".join(lines(summarize(load(path)))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
