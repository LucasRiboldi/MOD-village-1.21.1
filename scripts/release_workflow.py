#!/usr/bin/env python3
"""Ferramentas de entrega: FECHAR-RAPIDO e REGISTRO-RELEASE.

Os comandos validam evidência e documentos, mas nunca editam README, STATE ou
CHANGELOG automaticamente. Decisões editoriais continuam revisáveis no PR.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import re
import subprocess
import sys
from datetime import date
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

for stream in (sys.stdout, sys.stderr):
    if isinstance(stream, io.TextIOWrapper):
        stream.reconfigure(encoding="utf-8")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def version_from_gradle() -> str:
    match = re.search(r"^mod_version=(.+)$", read(ROOT / "gradle.properties"), re.MULTILINE)
    if not match:
        raise ValueError("mod_version ausente em gradle.properties")
    return match.group(1).strip()


def command(*args: str) -> str:
    result = subprocess.run(args, cwd=ROOT, text=True, capture_output=True, check=False)
    if result.returncode:
        raise ValueError(f"comando falhou: {' '.join(args)}\n{result.stderr.strip()}")
    return result.stdout.strip()


def required_documents(version: str) -> list[tuple[str, bool]]:
    changelog = read(ROOT / "CHANGELOG.md")
    readme = read(ROOT / "README.md")
    state = read(ROOT / "STATE.md")
    return [
        ("CHANGELOG contém a versão", f"## [{version}]" in changelog),
        ("README contém a versão", version in readme),
        ("STATE contém a versão", version in state),
    ]


def write_output(text: str, output: Path | None) -> None:
    if output is None:
        print(text)
        return
    target = output if output.is_absolute() else ROOT / output
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text + "\n", encoding="utf-8")
    print(f"Relatório gravado em {target.relative_to(ROOT)}")


def fechar_rapido(args: argparse.Namespace) -> int:
    version = args.version or version_from_gradle()
    checks = required_documents(version)
    rules = subprocess.run(
        [sys.executable, "scripts/rules_to_readme.py", "--check"],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    checks.append(("Regras geradas do README", rules.returncode == 0))
    branch = command("git", "branch", "--show-current")
    dirty = command("git", "status", "--porcelain")
    lines = [f"# FECHAR-RAPIDO — {version}", "", f"- Data: {date.today().isoformat()}", f"- Branch: `{branch}`"]
    lines.append(f"- Árvore local: {'alterada' if dirty else 'limpa'}")
    lines.extend(f"- [{'x' if ok else ' '}] {label}" for label, ok in checks)
    lines.extend(["", "## Próximos passos", "", "1. Revise o diff e abra o PR.", "2. Aguarde o CI completo do PR.", "3. Rode `REGISTRO-RELEASE` após registrar a evidência da versão."])
    write_output("\n".join(lines), args.output)
    return 0 if all(ok for _, ok in checks) and branch != "main" else 1


def record_path(version: str) -> Path:
    return ROOT / "docs" / "releases" / f"v{version}.md"


def registro_release(args: argparse.Namespace) -> int:
    version = args.version or version_from_gradle()
    record = record_path(version)
    if not record.is_file():
        raise ValueError(f"registro obrigatório ausente: {record.relative_to(ROOT)}")
    source = read(record)
    required = ("## Mudanças", "## Evidências", "## Playtest", "## Pendências")
    missing = [heading for heading in required if heading not in source]
    if missing:
        raise ValueError("registro incompleto: " + ", ".join(missing))
    if re.search(r"\bPENDENTE\b", source, re.IGNORECASE):
        raise ValueError("registro ainda contém PENDENTE; complete a evidência antes de publicar")
    commit = args.commit or command("git", "rev-parse", "HEAD")
    sha = args.sha
    if args.jar:
        sha = hashlib.sha256((ROOT / args.jar).read_bytes()).hexdigest().upper()
    if not sha:
        raise ValueError("informe --sha ou --jar")
    text = "\n".join([
        f"# Evidência de release v{version}",
        "",
        f"- Commit: `{commit}`",
        f"- SHA-256: `{sha.upper()}`",
        f"- Registro humano: [`docs/releases/v{version}.md`](https://github.com/LucasRiboldi/MOD-village-1.21.1/blob/main/docs/releases/v{version}.md)",
        "",
        source.strip(),
    ])
    write_output(text, args.output)
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    fast = subparsers.add_parser("fechar-rapido", help="FECHAR-RAPIDO: pré-voo de PR")
    fast.add_argument("--version", help="versão a conferir; padrão: gradle.properties")
    fast.add_argument("--output", type=Path, help="arquivo Markdown opcional")
    fast.set_defaults(handler=fechar_rapido)
    record = subparsers.add_parser("registro-release", help="REGISTRO-RELEASE: evidência para release")
    record.add_argument("--version", help="versão a registrar; padrão: gradle.properties")
    record.add_argument("--commit", help="commit da entrega; padrão: HEAD")
    record.add_argument("--sha", help="SHA-256 do JAR")
    record.add_argument("--jar", type=Path, help="JAR para calcular SHA-256")
    record.add_argument("--output", type=Path, help="arquivo Markdown opcional")
    record.set_defaults(handler=registro_release)
    args = parser.parse_args()
    try:
        return args.handler(args)
    except ValueError as error:
        print(f"ERRO: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
