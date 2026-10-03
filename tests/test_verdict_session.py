"""Testes de `scripts/verdict.py`, o veredito de uma sessão de jogo.

`scripts/test_verdict.py` já conferia que cada assinatura existe em
`src/main`, mas só rodava à mão — e a frase renomeada que ele pega é
justamente a que ninguém lembra de conferir. Aqui ele entra na bateria que o
CI roda (2026-10-02), junto com a leitura dos `.log.gz` do dia.

    python -m unittest discover -s tests
"""

from __future__ import annotations

import gzip
import importlib.util
import io
import sys
import tempfile
import time
import unittest
from contextlib import redirect_stdout
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

import verdict  # noqa: E402

# O script de assinaturas tem o mesmo nome de arquivo que um teste daqui
# teria; carregado pelo caminho, não colide com a descoberta do unittest.
_spec = importlib.util.spec_from_file_location(
    "verdict_signatures", ROOT / "scripts" / "test_verdict.py")
signatures = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(signatures)


class SignatureTest(unittest.TestCase):

    def test_every_signature_exists_and_every_verdict_is_reachable(self) -> None:
        out = io.StringIO()

        with redirect_stdout(out):
            status = signatures.main()

        self.assertEqual(0, status, out.getvalue())


class SessionTest(unittest.TestCase):

    def test_the_day_archives_come_before_the_latest_log(self) -> None:
        with tempfile.TemporaryDirectory() as folder:
            logs = Path(folder)
            latest = logs / "latest.log"
            latest.write_text("[20:00:00] noite: Worker a climbed to 1\n", encoding="utf-8")
            today = time.strftime("%Y-%m-%d", time.localtime(latest.stat().st_mtime))

            for name, line in ((f"{today}-2.log.gz", "segunda"), (f"{today}-1.log.gz", "primeira"),
                               ("2000-01-01-1.log.gz", "outro dia")):
                with gzip.open(logs / name, "wt", encoding="utf-8") as handle:
                    handle.write(f"[10:00:00] {line}\n")

            archives = verdict.same_day_archives(latest)
            text = verdict.read_session(latest, archives)

        self.assertEqual([f"{today}-1.log.gz", f"{today}-2.log.gz"], [a.name for a in archives])
        self.assertLess(text.index("primeira"), text.index("segunda"))
        self.assertLess(text.index("segunda"), text.index("noite"))
        self.assertNotIn("outro dia", text)

    def test_a_tenth_archive_sorts_after_the_ninth(self) -> None:
        with tempfile.TemporaryDirectory() as folder:
            logs = Path(folder)
            latest = logs / "latest.log"
            latest.write_text("", encoding="utf-8")
            today = time.strftime("%Y-%m-%d", time.localtime(latest.stat().st_mtime))

            for n in (10, 9):
                with gzip.open(logs / f"{today}-{n}.log.gz", "wt", encoding="utf-8") as handle:
                    handle.write("")

            names = [a.name for a in verdict.same_day_archives(latest)]

        self.assertEqual([f"{today}-9.log.gz", f"{today}-10.log.gz"], names)


if __name__ == "__main__":
    unittest.main()
