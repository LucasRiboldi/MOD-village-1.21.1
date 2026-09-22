#!/usr/bin/env python3
"""Consolida bloqueios e repeticoes do Village Colony a partir de logs.

O servidor ja escreve diagnosticos com IdleLog, SweepLog e os relatorios de
cada oficio. Este utilitario nao adiciona estado ao mundo nem duplica essa
instrumentacao: ele le uma sessao encerrada e registra somente contagens por
assinatura. Assim o historico pode orientar a proxima investigacao sem expor
coordenadas, UUIDs ou linhas completas do log do jogador.

Uso:
    python scripts/analyze_village_log.py
    python scripts/analyze_village_log.py caminho/para/latest.log
    python scripts/analyze_village_log.py caminho/para/latest.log.gz \
        --history docs/technical/Log-Stall-History.json \
        --report docs/technical/Log-Stall-Statistics.md
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


DEFAULT_LOOP_THRESHOLD = 3
LOG_CANDIDATES = (
    Path(os.environ.get("APPDATA", "")) / ".minecraft" / "logs" / "latest.log",
    Path.home() / "AppData" / "Roaming" / ".minecraft" / "logs" / "latest.log",
    Path("run") / "logs" / "latest.log",
)


@dataclass(frozen=True)
class Signature:
    """Uma frase de producao e a area que deve ser investigada."""

    key: str
    where: str
    meaning: str
    pattern: re.Pattern[str]


@dataclass(frozen=True)
class Observation:
    """Contagem anonimizada de uma assinatura em uma sessao."""

    key: str
    where: str
    meaning: str
    occurrences: int
    loop_candidate: bool


SIGNATURES = (
    Signature(
        "miner_no_branch_work",
        "MineClaims / MinerWork",
        "mineiro sem frente de escavacao disponivel",
        re.compile(r"\bno miner branch work\b", re.IGNORECASE),
    ),
    Signature(
        "miner_no_standing_room",
        "MineDigging",
        "mina encontrou pedra sem patamar seguro para continuar",
        re.compile(r"\bhit stone with nowhere to stand\b", re.IGNORECASE),
    ),
    Signature(
        "construction_waiting_resources",
        "BuilderWork / WaitingWork",
        "obra aguarda recurso",
        re.compile(r"\bWAITING_RESOURCES\b", re.IGNORECASE),
    ),
    Signature(
        "builder_pathing_stalled",
        "BuilderApproach / BuilderWork",
        "construtor caminha sem alcancar o proximo bloco",
        re.compile(r"\bwalking for \d+ ticks without reaching the block\b", re.IGNORECASE),
    ),
    Signature(
        "surface_worker_unreachable",
        "SurfaceGatheringWork",
        "coletor de superficie nao alcancou o alvo",
        re.compile(r"\bunable to reach\b", re.IGNORECASE),
    ),
    Signature(
        "missing_profession",
        "ColonyCycle / ProductionHands",
        "pedido sem aldeao capaz na vila",
        re.compile(r"\bno worker in the village can do it\b", re.IGNORECASE),
    ),
    Signature(
        "site_sweep_budget_exhausted",
        "RingSweep / BuildSiteScanner",
        "varredura de lote ou alvo terminou a passagem sem resposta",
        re.compile(r"\bstill sweeping\b.*\bbudget ran out before an answer\b", re.IGNORECASE),
    ),
    Signature(
        "site_sweep_restarted",
        "SweepLog / BuildSiteScanner",
        "cursor de varredura reiniciou antes de concluir uma volta",
        re.compile(r"\b(?:restarted its sweep|the sweep starts over)\b", re.IGNORECASE),
    ),
)


def analyze_text(text: str, loop_threshold: int = DEFAULT_LOOP_THRESHOLD) -> dict[str, Observation]:
    """Conta cada assinatura por linha, sem preservar dados variaveis do jogador."""
    if loop_threshold < 2:
        raise ValueError("loop_threshold must be at least 2")

    counts = {signature.key: 0 for signature in SIGNATURES}
    for line in text.splitlines():
        for signature in SIGNATURES:
            if signature.pattern.search(line):
                counts[signature.key] += 1

    return {
        signature.key: Observation(
            key=signature.key,
            where=signature.where,
            meaning=signature.meaning,
            occurrences=counts[signature.key],
            loop_candidate=counts[signature.key] >= loop_threshold,
        )
        for signature in SIGNATURES
    }


def read_log(path: Path) -> str:
    """Le latest.log e archives gzip sem depender de ferramenta externa."""
    opener = gzip.open if path.suffix == ".gz" else open
    with opener(path, "rt", encoding="utf-8", errors="replace") as source:
        return source.read()


def timestamps(text: str) -> tuple[str | None, str | None]:
    found = re.findall(r"^\[(\d{2}:\d{2}:\d{2})\]", text, re.MULTILINE)
    return (found[0], found[-1]) if found else (None, None)


def observation_data(observations: Iterable[Observation]) -> dict[str, dict[str, object]]:
    return {
        item.key: {
            "where": item.where,
            "meaning": item.meaning,
            "occurrences": item.occurrences,
            "loop_candidate": item.loop_candidate,
        }
        for item in observations
    }


def read_history(path: Path) -> dict[str, object]:
    if not path.is_file():
        return {"schema": 1, "sessions": []}

    loaded = json.loads(path.read_text(encoding="utf-8"))
    if loaded.get("schema") != 1 or not isinstance(loaded.get("sessions"), list):
        raise ValueError(f"Unsupported stall history format: {path}")
    return loaded


def update_history(
        history: dict[str, object],
        log_path: Path,
        text: str,
        observations: dict[str, Observation]) -> dict[str, object]:
    """Substitui a mesma sessao pelo hash, sem guardar texto ou coordenadas."""
    started_at, ended_at = timestamps(text)
    digest = hashlib.sha256(log_path.read_bytes()).hexdigest()
    stat = log_path.stat()
    session = {
        "source_name": log_path.name,
        "sha256": digest,
        "bytes": stat.st_size,
        "modified_at": datetime.fromtimestamp(stat.st_mtime, timezone.utc).isoformat(),
        "started_at": started_at,
        "ended_at": ended_at,
        "lines": len(text.splitlines()),
        "observations": observation_data(observations.values()),
    }
    old_sessions = history["sessions"]
    assert isinstance(old_sessions, list)
    old_sessions[:] = [old for old in old_sessions if old.get("sha256") != digest]
    old_sessions.append(session)
    return history


def render_report(log_path: Path, history: dict[str, object], loop_threshold: int) -> str:
    """Gera Markdown com contagens da sessao mais recente e tendencia historica."""
    sessions = history["sessions"]
    assert isinstance(sessions, list) and sessions
    latest = sessions[-1]
    observations = latest["observations"]
    assert isinstance(observations, dict)

    rows = []
    candidates = []
    for signature in SIGNATURES:
        item = observations[signature.key]
        assert isinstance(item, dict)
        count = item["occurrences"]
        assert isinstance(count, int)
        state = "candidato a loop" if item["loop_candidate"] else "observado"
        rows.append(f"| `{signature.key}` | {item['where']} | {count} | {state} |")
        if item["loop_candidate"]:
            candidates.append((signature.key, item["where"], count))

    totals: dict[str, int] = {signature.key: 0 for signature in SIGNATURES}
    for session in sessions:
        current = session.get("observations", {})
        if not isinstance(current, dict):
            continue
        for key in totals:
            entry = current.get(key, {})
            if isinstance(entry, dict) and isinstance(entry.get("occurrences"), int):
                totals[key] += entry["occurrences"]

    candidate_text = (
        "\n".join(f"- `{key}`: {count} ocorrencias em {where}." for key, where, count in candidates)
        if candidates
        else "- Nenhuma assinatura atingiu o limiar nesta sessao."
    )
    total_rows = "\n".join(
        f"| `{signature.key}` | {totals[signature.key]} |"
        for signature in SIGNATURES
        if totals[signature.key]
    ) or "| Nenhuma assinatura observada | 0 |"

    return f"""# Estatistica de travamentos e repeticoes

**Gerado em:** {datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC')}
**Log analisado:** `{log_path.name}`
**Sessoes no historico:** {len(sessions)}
**Limiar de candidato a loop:** {loop_threshold} ocorrencias na mesma sessao

Este relatorio le diagnosticos que o mod ja escreve. Ele nao e um veredito de
defeito: uma assinatura isolada pode ser um estado normal. Ao atingir o limiar,
ela vira um candidato para reproducao, inspecao do contexto do log e GameTest.
O historico guarda somente contagens, horario, hash e nome do arquivo; nao
guarda UUIDs, coordenadas ou linhas cruas do mundo do jogador.

## Sessao atual

| Assinatura | Area responsavel | Ocorrencias | Leitura |
|---|---|---:|---|
{chr(10).join(rows)}

## Candidatos a investigacao

{candidate_text}

## Acumulado do historico

| Assinatura | Ocorrencias acumuladas |
|---|---:|
{total_rows}

## Proximo passo tecnico

1. Abrir o bloco de contexto de cada candidato no `latest.log` e separar estado
   normal de repeticao sem progresso.
2. Para cada repeticao confirmada, criar ou estabilizar um GameTest antes de
   modificar `MinerWork`, `BuildSiteScanner`, `WaitingWork` ou a profissao.
3. Rodar novamente este comando apos o playtest; o mesmo arquivo e deduplicado
   pelo SHA-256, entao o historico nao infla por releitura.
"""


def default_log() -> Path | None:
    return next((candidate for candidate in LOG_CANDIDATES if candidate.is_file()), None)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", nargs="?", type=Path, help="latest.log ou arquivo .gz")
    parser.add_argument(
        "--history",
        type=Path,
        default=Path("docs/technical/Log-Stall-History.json"),
        help="historico JSON versionado",
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=Path("docs/technical/Log-Stall-Statistics.md"),
        help="relatorio Markdown gerado",
    )
    parser.add_argument("--min-loop", type=int, default=DEFAULT_LOOP_THRESHOLD)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    log_path = args.log or default_log()
    if log_path is None or not log_path.is_file():
        print("Nao achei o log. Informe latest.log ou execute a partir de uma instalacao Minecraft.")
        return 2

    text = read_log(log_path)
    observations = analyze_text(text, args.min_loop)
    history = update_history(read_history(args.history), log_path, text, observations)
    args.history.parent.mkdir(parents=True, exist_ok=True)
    args.history.write_text(json.dumps(history, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(render_report(log_path, history, args.min_loop), encoding="utf-8")

    loops = [item for item in observations.values() if item.loop_candidate]
    print(f"Analisado: {log_path} ({len(text.splitlines())} linhas)")
    print(f"Candidatos a loop: {len(loops)}")
    for item in loops:
        print(f"  {item.key}: {item.occurrences} ({item.where})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
