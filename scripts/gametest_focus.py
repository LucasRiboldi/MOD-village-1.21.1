"""Limita a bateria de GameTest às classes pedidas: `runGametest` não aceita filtro por teste.

Uso:
  python scripts/gametest_focus.py com.villagecolony.fabric.work.DoorFrontsGameTest [outras classes]
  rm -f build/gametest-report.xml
  ./gradlew.bat runGametest --rerun-tasks -q
  python scripts/gametest_summary.py
  python scripts/gametest_focus.py restore

Sempre restaure antes de commitar: troca o `fabric-gametest` de src/gametest/resources/fabric.mod.json. A cópia fica em
build/gametest-focus-backup.json. Exige o nome completo da classe.
"""
import io
import json
import os
import re
import sys

FILE = "src/gametest/resources/fabric.mod.json"
BACKUP = "build/gametest-focus-backup.json"


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    if sys.argv[1] == "restore":
        text = json.load(io.open(BACKUP, encoding="utf-8"))
        io.open(FILE, "w", encoding="utf-8", newline="").write(text)
        print("restaurado")
        return
    text = io.open(FILE, encoding="utf-8", newline="").read()
    os.makedirs("build", exist_ok=True)
    json.dump(text, io.open(BACKUP, "w", encoding="utf-8"))
    items = ",\n".join('      "%s"' % name for name in sys.argv[1:])
    new, count = re.subn(r'"fabric-gametest"\s*:\s*\[.*?\]', '"fabric-gametest": [\n' + items + "\n    ]", text, count=1, flags=re.S)
    if count != 1:
        sys.exit("não achei fabric-gametest")
    io.open(FILE, "w", encoding="utf-8", newline="").write(new)
    print("focado em", len(sys.argv) - 1, "classes")


if __name__ == "__main__":
    main()
