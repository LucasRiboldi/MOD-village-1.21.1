"""As camadas do Gauntlet Loop: rodar e ler o que aconteceu.

Metade de baixo do mecanismo — a que executa comando de verdade e lê
relatório de verdade. A decisão (PASS/FAIL/BLOCKED, teto de iterações,
livro-razão) mora em `gauntlet.py`, e a separação não é enfeite: são as
duas coisas que o pedido do Gauntlet manda manter separadas, e este
projeto pede arquivo abaixo de 500 linhas. Juntas elas passavam de 580 —
o próprio gate acusava.

Nada aqui julga requisito. Julgar é do `gauntlet-verifier`.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

LEDGER_DIR = ROOT / "build" / "gauntlet"

DEFAULT_MAX_ITERATIONS = 5

#: O que invalida um relatório da bateria de Java.
#:
#: Só o que entra naquela compilação. A primeira versão desta lista tinha
#: `.py` dentro, e um ajuste em `scripts/gauntlet.py` fazia a camada de
#: unitários sair BLOCKED com o relatório de Java intacto — falso
#: bloqueio é ruído, e ruído acaba ensinando a ignorar o gate.
JAVA_SOURCE = (".java", ".gradle", ".properties")

#: Um teste desligado é a forma mais barata de fingir verde.
DISABLED = re.compile(r"@(Disabled|Ignore)\b")

#: Trabalho por fazer que entrou junto com a entrega.
#:
#: Apertada em 2026-09-09: a versao larga acusava a si mesma. Este
#: arquivo, os testes dele e os dois `.claude/` CITAM os marcadores
#: para descreve-los, e achado de mentira ensina a ignorar o relatorio.
#: Marcador entre aspas, entre crases ou dentro de uma alternancia de
#: expressao regular e mencao, nao pendencia.
UNFINISHED = re.compile(r"""(?<![\w"'`])(TODO|FIXME|XXX|HACK)\b(?![\w"'`|)])""")

#: O que da para ler linha a linha a procura de marcador.
TEXT_SUFFIXES = (".java", ".py", ".md", ".json", ".gradle", ".properties", ".txt")

#: Regra do CLAUDE.md deste projeto.
MAX_FILE_LINES = 500

SENSITIVE = ("data/save", "persistent", "Nbt", "Save", "Protection", "networking")


def run(command: list[str], timeout: int) -> dict:
    """Executa e devolve o que aconteceu — nunca o que se esperava."""
    started = time.time()

    try:
        done = subprocess.run(
            command,
            cwd=ROOT,
            capture_output=True,
            text=True,
            errors="replace",
            timeout=timeout,
        )
    except FileNotFoundError as missing:
        return {"command": " ".join(command), "exit_code": None,
                "error": f"comando ausente: {missing}", "seconds": 0.0,
                "stdout": "", "stderr": ""}
    except subprocess.TimeoutExpired:
        return {"command": " ".join(command), "exit_code": None,
                "error": f"estourou {timeout}s", "seconds": float(timeout),
                "stdout": "", "stderr": ""}

    return {
        "command": " ".join(command),
        "exit_code": done.returncode,
        "seconds": round(time.time() - started, 1),
        "stdout": done.stdout[-4000:],
        "stderr": done.stderr[-4000:],
    }


def gradle(tasks: list[str]) -> list[str]:
    """O wrapper do projeto, no nome que o sistema operacional entende.

    <b>Caminho absoluto, e não `./gradlew`.</b> O `cwd=` do `subprocess`
    muda o diretório do processo filho, mas quem resolve o nome do
    executável é o sistema — e no Windows ele resolve contra o diretório
    do processo PAI. A primeira execução deste script devolveu
    `comando ausente: [WinError 2]`, e o gate a reportou como BLOCKED em
    vez de verde, que é o comportamento que ele existe para ter.
    """
    launcher = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")

    return [str(launcher), *tasks, "--console=plain"]


def git(args: list[str]) -> str:
    done = subprocess.run(["git", *args], cwd=ROOT, capture_output=True,
                          text=True, errors="replace")

    return done.stdout if done.returncode == 0 else ""


def changed_files(base: str) -> list[str]:
    """Os arquivos que esta entrega tocou, rastreados ou não.

    <b>`--untracked-files=all`, e não o padrão</b> — achado do
    `gauntlet-verifier` na iteração 1 de 2026-09-09. O `git status`
    normal <b>colapsa um diretório novo no nome da pasta</b>: uma classe
    de teste nova dentro de um pacote novo aparecia como
    `src/test/java/.../novopacote/` e nenhum `.java` era visto. Sem
    arquivo Java visto, {@code newest_source_mtime} devolvia 0.0, a
    guarda de relatório velho era pulada, e um XML de ontem passava por
    bateria de hoje. É o verde falso que este script existe para impedir,
    entrando pela porta dos fundos.
    """
    names = set()

    for line in git(["diff", "--name-only", base]).splitlines():
        if line.strip():
            names.add(line.strip())

    for line in git(["status", "--porcelain", "--untracked-files=all"]).splitlines():
        path = parse_status_line(line)

        if path:
            names.add(path)

    return sorted(names)


def untracked_files() -> list[str]:
    """Só o que o git ainda não conhece.

    <b>`git diff` não mostra arquivo não-rastreado</b> — achado do
    `gauntlet-verifier` na iteração 1 de 2026-09-09, e o mais sério dos
    três. A camada `scope` varria só o diff, então um `@Disabled` dentro
    de arquivo NOVO — que é o que toda entrega nova mais produz — não era
    lido por ninguém, e a camada saía PASS sem achado nenhum. Medido
    naquela entrega: cinco dos nove caminhos nunca foram abertos.
    """
    names = []

    for line in git(["status", "--porcelain", "--untracked-files=all"]).splitlines():
        if line.startswith("??"):
            path = parse_status_line(line)

            if path:
                names.append(path)

    return names


def marker_findings(lines):
    """Os marcadores que estas linhas trazem.

    Recebe pares ``(origem, texto)``: a origem é o arquivo, e ela decide
    junto. <b>`@Disabled` só é anotação dentro de `.java`</b> — em
    Markdown, Python ou prosa é menção, e a primeira versão desta função
    acusava a própria fonte do mecanismo com severidade `critical`, que é
    a única que bloqueia o gate. Um portão que reprova a si mesmo por
    falar sobre o que ele procura não sobrevive à segunda semana.
    """
    findings = []

    for origin, line in lines:
        if origin.endswith(".java") and DISABLED.search(line):
            findings.append({
                "severity": "critical",
                "rule": "teste desligado",
                "evidence": origin + ": " + line.strip()[:160],
                "description": "@Disabled/@Ignore — desligar um teste é fingir "
                               "verde, não obtê-lo",
            })

        if UNFINISHED.search(line):
            findings.append({
                "severity": "medium",
                "rule": "trabalho por fazer",
                "evidence": origin + ": " + line.strip()[:160],
                "description": "marcador de pendência entrou junto com a entrega",
            })

    return findings


def diff_added(diff: str) -> list[tuple[str, str]]:
    """As linhas acrescentadas, cada uma sabendo de que arquivo veio.

    O cabeçalho ``+++ b/caminho`` do diff unificado é quem diz. Sem ele a
    linha chegava como ``("diff", texto)`` e nenhuma regra podia depender
    do tipo do arquivo — e a de anotação depende.
    """
    lines = []
    current = "diff"

    for line in diff.splitlines():
        if line.startswith("+++ "):
            path = line[4:].strip()
            current = path[2:] if path.startswith("b/") else path

            continue

        if line.startswith("+"):
            lines.append((current, line[1:]))

    return lines


def scanned_lines(added: list[tuple[str, str]]) -> list[tuple[str, str]]:
    """As linhas do diff, mais os arquivos novos por inteiro."""
    lines = list(added)

    for name in untracked_files():
        path = ROOT / name

        if path.suffix not in TEXT_SUFFIXES or not path.exists():
            continue

        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            lines.append((name, line))

    return lines


def parse_status_line(line: str) -> str | None:
    """O caminho que uma linha do `git status --porcelain` nomeia.

    Função à parte porque é a única parte disto que dá para afirmar sem
    um repositório de mentira: renomeado vem como `antigo -> novo`, e o
    que interessa é o novo; caminho com espaço vem entre aspas.
    """
    if len(line) <= 3:
        return None

    path = line[3:].strip()

    if " -> " in path:
        path = path.split(" -> ", 1)[1].strip()

    return path.strip('"') or None


def newest_source_mtime(files: list[str], suffixes=JAVA_SOURCE) -> float:
    """A alteração mais recente que afeta ESTA bateria.

    Recurso dentro de `src/` conta junto: a bateria de gametest lê
    estrutura e catálogo de lá, e um `.json` novo muda o que ela mede sem
    passar pelo compilador.
    """
    newest = 0.0

    for name in files:
        path = ROOT / name

        if not path.exists() or path.is_dir():
            continue

        if path.suffix in suffixes or name.startswith("src/"):
            newest = max(newest, path.stat().st_mtime)

    return newest


def scope_layer(base: str) -> dict:
    """A camada barata: o diff, e o que ele traz sem ser pedido."""
    files = changed_files(base)
    findings = []

    diff = git(["diff", "--unified=0", base])

    added = diff_added(diff)
    removed = [line[1:] for line in diff.splitlines()
               if line.startswith("-") and not line.startswith("---")]

    findings.extend(marker_findings(scanned_lines(added)))

    dropped = sum(1 for line in removed if "assert" in line.lower())
    gained = sum(1 for _, line in added if "assert" in line.lower())

    if dropped > gained:
        findings.append({
            "severity": "high",
            "rule": "afirmação removida",
            "evidence": f"{dropped} linhas com assert saíram, {gained} entraram",
            "description": "a entrega tirou mais afirmação do que pôs — confira "
                           "se um teste deixou de medir o que media",
        })

    for name in files:
        path = ROOT / name

        if path.suffix == ".java" and path.exists():
            lines = len(path.read_text(encoding="utf-8", errors="replace").splitlines())

            if lines > MAX_FILE_LINES:
                findings.append({
                    "severity": "low",
                    "rule": f"arquivo acima de {MAX_FILE_LINES} linhas",
                    "evidence": f"{name}: {lines} linhas",
                    "description": "regra do CLAUDE.md. Arquivo que já estava "
                                   "grande não é regressão desta entrega, mas "
                                   "fica dito",
                })

    blocking = [f for f in findings if f["severity"] == "critical"]

    return {
        "status": "FAIL" if blocking else "PASS",
        "files": files,
        "findings": findings,
    }


def junit_totals(paths: list[Path]) -> dict:
    """Contagem lida do XML, e não do que o Gradle imprimiu na tela."""
    total = failed = skipped = 0
    names: list[str] = []
    newest = 0.0

    for path in paths:
        newest = max(newest, path.stat().st_mtime)

        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue

        for case in root.iter("testcase"):
            total += 1

            broken = (case.find("failure") is not None
                      or case.find("error") is not None)

            if broken:
                failed += 1
                names.append(f"{case.get('classname', '?')}.{case.get('name', '?')}")
            elif case.find("skipped") is not None:
                skipped += 1

    return {"total": total, "failed": failed, "skipped": skipped,
            "failures": names[:40], "report_mtime": newest}


def report_layer(result: dict, reports: list[Path], source_mtime: float) -> dict:
    """Junta o que o comando devolveu com o que o relatório diz.

    <b>Relatório mais velho que o código é BLOCKED, e não PASS.</b> Tarefa
    UP-TO-DATE do Gradle não reescreve o XML: sem esta conferência, uma
    bateria que não rodou passaria por bateria verde — que é exatamente o
    verde falso que este script existe para impedir.
    """
    present = [p for p in reports if p.exists()]

    if not present:
        return {"status": "BLOCKED", "reason": "nenhum relatório foi escrito",
                "run": result}

    totals = junit_totals(present)

    if totals["total"] == 0:
        # Zero caso executado não é bateria verde: é bateria que não
        # aconteceu. Sem isto, um relatório vazio — filtro que não casou
        # com nada, tarefa que abortou antes de começar — sairia PASS
        # com exit code 0.
        return {"status": "BLOCKED",
                "reason": "o relatório não tem um único caso — nada foi executado",
                "run": result, "totals": totals}

    if source_mtime and totals["report_mtime"] < source_mtime:
        return {"status": "BLOCKED",
                "reason": "o relatório é mais antigo que o código alterado — "
                          "esta camada não rodou de verdade",
                "run": result, "totals": totals}

    if result["exit_code"] is None:
        return {"status": "BLOCKED", "reason": result.get("error", "não executou"),
                "run": result, "totals": totals}

    passing = result["exit_code"] == 0 and totals["failed"] == 0

    return {"status": "PASS" if passing else "FAIL", "run": result, "totals": totals}


def plain_layer(result: dict) -> dict:
    if result["exit_code"] is None:
        return {"status": "BLOCKED", "reason": result.get("error", "não executou"),
                "run": result}

    return {"status": "PASS" if result["exit_code"] == 0 else "FAIL", "run": result}


def security_layer(files: list[str], reviewed: str = "") -> dict:
    """Não aprova nada sozinha: diz se alguém precisa olhar.

    Este mod não tem autenticação, pagamento nem dado de terceiro. O que
    ele tem de parecido é persistência e proteção de bloco — estragar
    qualquer um dos dois custa o mundo do jogador.

    <b>A única promoção que o Verifier pode fazer, e ela é registrada.</b>
    Em todo o resto ele só rebaixa. Aqui não dá para ser assim: uma
    entrega que toca persistência ficaria BLOCKED para sempre, porque
    nenhum `subprocess` sabe dizer se o save continua legível. A saída é
    a promoção existir, ser explícita e ficar escrita — `--security-reviewed`
    grava no relatório <b>o que</b> foi conferido. Frase vazia não promove.
    """
    touched = [f for f in files if any(mark in f for mark in SENSITIVE)]

    if not touched:
        return {"status": "NOT_APPLICABLE",
                "reason": "o diff não toca persistência nem proteção de bloco"}

    if reviewed.strip():
        return {"status": "PASS", "files": touched,
                "reviewed_by_verifier": reviewed.strip(),
                "reason": "promovido pelo Verifier, com o que ele conferiu escrito "
                          "aqui — é a única promoção que o laço permite"}

    return {"status": "REVIEW_REQUIRED", "files": touched,
            "reason": "persistência ou proteção mudou — o Verifier tem de dizer "
                      "por escrito o que conferiu (--security-reviewed)"}


