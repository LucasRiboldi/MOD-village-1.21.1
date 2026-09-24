#!/usr/bin/env python3
"""Manifesto de evidência de release — decisão 10A, 2026-09-24.

Automatiza a comparação manual de três hashes SHA-256 (build/libs,
downloads, mods instalados) que STATE.md já documentava fazer à mão
antes de cada entrega: "As três cópias foram comparadas depois de
fechar o cliente Minecraft."

Não copia nem publica artefato — só relata evidência. Com --dry-run,
imprime o manifesto (commit atual, os três hashes, timestamp, caminhos)
e sai com código zero quando os três hashes concordam entre si e todos
os arquivos existem; sai com código não-zero e mensagem explícita em
qualquer outro caso.

TODO (fora do escopo desta entrega — ver Task 13 do plano de
confiabilidade operacional): incluir o resultado real de
`./gradlew.bat test` e `./gradlew.bat runGametest --rerun-tasks` no
manifesto, e a versão de esquema do save (SaveMigration.CURRENT,
MineSave.SHAPE_VERSION), para que o manifesto ateste evidência de
verificação e não só identidade de artefato.
"""

import argparse
import hashlib
import io
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

# O console do Windows nem sempre usa UTF-8 por padrão, e este arquivo
# usa travessão e acentos nas mensagens. Reconfigurar aqui, e não pedir
# ao usuário para trocar o codepage, é o que mantém a saída legível em
# qualquer terminal que rode o script — Git Bash, PowerShell ou cmd.
for _stream in (sys.stdout, sys.stderr):
    if isinstance(_stream, io.TextIOWrapper):
        _stream.reconfigure(encoding="utf-8")


def sha256_of(path: Path) -> str:
    """O SHA-256 do arquivo, lido em blocos — nunca o arquivo inteiro
    em memória de uma vez, para caber num JAR grande sem custo."""
    digest = hashlib.sha256()

    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)

    return digest.hexdigest().upper()


def current_commit() -> str:
    """O commit atual, ou 'unknown' se o diretório não for um repositório git.

    Nunca falha o manifesto por isto: um artefato pode ser gerado fora
    de um checkout git (ex.: de um zip extraído), e a evidência de hash
    continua válida mesmo sem o commit.
    """
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            capture_output=True,
            text=True,
            check=True,
            timeout=10,
        )
        return result.stdout.strip()
    except (subprocess.SubprocessError, FileNotFoundError, OSError):
        return "unknown"


def build_manifest(jar: Path, downloads: Path, mods: Path) -> dict:
    """Reúne a evidência dos três artefatos, ou levanta com a primeira
    falta encontrada — sem calcular o hash do que falta."""
    missing = [
        str(path)
        for label, path in (("--jar", jar), ("--downloads", downloads), ("--mods", mods))
        if not path.is_file()
    ]

    if missing:
        raise SystemExit(
            "release manifest failed: artifact not found — "
            + ", ".join(missing)
        )

    hashes = {
        "jar": sha256_of(jar),
        "downloads": sha256_of(downloads),
        "mods": sha256_of(mods),
    }

    if len(set(hashes.values())) != 1:
        lines = "\n".join(f"  {name}: {value}" for name, value in hashes.items())
        raise SystemExit(
            "release manifest failed: artifact hash mismatch\n" + lines
        )

    return {
        "commit": current_commit(),
        "timestamp": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "sha256": next(iter(hashes.values())),
        "jar": str(jar),
        "downloads": str(downloads),
        "mods": str(mods),
    }


def print_manifest(manifest: dict) -> None:
    print("release manifest — all three artifacts match")
    print(f"  commit:    {manifest['commit']}")
    print(f"  timestamp: {manifest['timestamp']}")
    print(f"  sha256:    {manifest['sha256']}")
    print(f"  jar:       {manifest['jar']}")
    print(f"  downloads: {manifest['downloads']}")
    print(f"  mods:      {manifest['mods']}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Compara o SHA-256 do JAR de build, do download publicado e do "
            "JAR instalado, e imprime um manifesto de evidência de release. "
            "Falha (código não-zero) se algum artefato faltar ou se os "
            "hashes não baterem entre si. Nunca copia nem publica nada."
        )
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help=(
            "Confere os três artefatos e imprime o manifesto sem alterar "
            "nada. É o único modo suportado nesta entrega — o script "
            "nunca escreve, então --dry-run é sempre implícito, mas a "
            "flag é exigida para deixar a intenção explícita no comando."
        ),
    )
    parser.add_argument("--jar", required=True, type=Path, help="o JAR de build/libs")
    parser.add_argument(
        "--downloads", required=True, type=Path, help="o JAR publicado em downloads/"
    )
    parser.add_argument(
        "--mods", required=True, type=Path, help="o JAR instalado em %%APPDATA%%\\.minecraft\\mods"
    )

    args = parser.parse_args(argv)

    if not args.dry_run:
        raise SystemExit(
            "release manifest failed: only --dry-run is supported in this entry"
        )

    manifest = build_manifest(args.jar, args.downloads, args.mods)
    print_manifest(manifest)

    return 0


if __name__ == "__main__":
    sys.exit(main())
