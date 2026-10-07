"""Roda a bateria de GameTests N vezes e resume: passou, o que caiu, e quantas
fusões de colônia houve (deve ser 0 desde a correção do KF-003).

Uso: python scripts/gametest_battery.py 30
Precisa de JAVA_HOME apontando para o JDK 21.
"""
import collections
import pathlib
import re
import subprocess
import sys
import tempfile

GRADLEW = "gradlew.bat" if sys.platform == "win32" else "./gradlew"


def run_once(log: pathlib.Path) -> tuple[str, list[str], int]:
    with log.open("w", encoding="utf-8", errors="replace") as out:
        subprocess.run([GRADLEW, "runGametest", "--rerun-tasks", "-q"], stdout=out, stderr=subprocess.STDOUT,
                       shell=sys.platform == "win32")
    text = log.read_text(encoding="utf-8", errors="replace")
    result = re.findall(r"All \d+ required tests passed|\d+ required tests failed", text)
    failed = re.findall(r"\(Minecraft\)    - (\S+)", text)
    return (result[-1] if result else "sem resultado"), failed, text.count("absorbed colony")


def main() -> None:
    runs = int(sys.argv[1]) if len(sys.argv) > 1 else 10
    falls = collections.Counter()
    clean = 0
    with tempfile.TemporaryDirectory() as tmp:
        for i in range(1, runs + 1):
            result, failed, merges = run_once(pathlib.Path(tmp) / f"run{i}.log")
            clean += not failed
            falls.update(failed)
            print(f"{i:>3}: {result}  fusões={merges}  {' '.join(failed)}", flush=True)
    print(f"\n{clean}/{runs} baterias limpas")
    for name, count in falls.most_common():
        print(f"  {count:>3}x {name}")


if __name__ == "__main__":
    main()
