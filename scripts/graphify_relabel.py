#!/usr/bin/env python3
"""Transporta os nomes de comunidade curados para um grafo recém-atualizado.

`graphify update .` re-clusteriza. Os ids de comunidade mudam, e os nomes
escritos à mão são substituídos por nomes derivados do hub — `TaskService`
no lugar de `Colony Cycle Orchestration`. O `graphify label` não conserta
isso aqui: sem `GEMINI_API_KEY`/`GOOGLE_API_KEY` ele imprime
`no LLM backend configured; keeping Community N placeholders`, não produz
nada, e ainda sobrescreve o backup em `graphify-out/<data>/`.

**A pergunta que este script responde não precisa de LLM.** Uma comunidade
nova é quase sempre uma comunidade antiga com alguns nós a mais ou a menos.
Então o nome dela é o nome curado que a **maioria dos seus nós já
carregava** — uma votação, não um julgamento. Medido em 2026-09-02 contra o
grafo curado de 3.167 nós: 145 de 158 comunidades decidiram com pelo menos
50% de sobreposição. As 13 restantes é que precisam de alguém pensando, e o
`--dry-run` existe para mostrar exatamente quais são.

Uso típico, em dois passos:

    # 1. o que o voto decide sozinho, e o que sobra
    python scripts/graphify_relabel.py --reference graphify-out/curado/graph.json --dry-run

    # 2. depois de escrever os nomes que faltavam
    python scripts/graphify_relabel.py --reference graphify-out/curado/graph.json \\
        --overrides scripts/community_names.json

O `--reference` é o `graph.json` de antes do update — o `update` guarda um
em `graphify-out/<data>/`, e **sobrescreve esse diretório se rodar duas
vezes no mesmo dia**, então copie para outro nome antes da segunda rodada.

Ver a seção `## graphify` do `CLAUDE.md` na raiz.
"""

from __future__ import annotations

import argparse
import collections
import json
import subprocess
import sys
from pathlib import Path

DEFAULT_GRAPH = Path("graphify-out/graph.json")
DEFAULT_LABELS = Path("graphify-out/.graphify_labels.json")
DEFAULT_REPORT = Path("graphify-out/GRAPH_REPORT.md")
DEFAULT_COST = Path("graphify-out/cost.json")

# O `.sig` é assinado sobre os rótulos que ele acompanha. Reescrever os
# rótulos sem apagá-lo deixa os dois discordando em silêncio.
LABELS_SIG = Path("graphify-out/.graphify_labels.json.sig")


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def community_members(graph: dict) -> dict[int, list[dict]]:
    members: dict[int, list[dict]] = collections.defaultdict(list)

    for node in graph["nodes"]:
        members[node["community"]].append(node)

    return dict(members)


def hub_of(nodes: list[dict]) -> str:
    """O membro que melhor serve de apelido para a comunidade.

    Prefere um tipo do próprio projeto. Um método (`.observe()`) é curto
    demais para nomear o grupo, e um tipo externo totalmente qualificado
    (`net.minecraft.block.entity.ChestBlockEntity`) é longo sem descrever
    nada — a primeira versão disto usava só o rótulo mais longo e chamava
    de `ChestBlockEntity` uma comunidade de `VillageDetector`,
    `WorkAssignment` e `WorkHours`. Entre os candidatos do projeto, o mais
    longo, que costuma ser o tipo e não o membro.

    É heurística, e só vale quando o voto não decidiu — nunca para sobrepor
    um nome curado.
    """
    external = ("net.", "org.", "java.", "javax.", "com.google.", "it.unimi.")

    def own(node: dict) -> bool:
        label = node.get("label") or ""

        return bool(label) and not label.startswith(".") and not label.startswith(external)

    candidates = [n for n in nodes if own(n)] or nodes

    if not candidates:
        return ""

    best = max(candidates, key=lambda n: len(n.get("label") or ""))
    label = (best.get("label") or "").replace(".java", "").lstrip(".").split("(")[0]

    return label.strip()


def parse_overrides(
    raw: dict[str, str], members: dict[int, list[dict]]
) -> tuple[dict[int, str], list[str]]:
    """Aceita chave por id de comunidade ou por id de nó.

    **Prefira o id de nó.** O id de comunidade vale só para a clusterização
    que o produziu: o `update` seguinte reembaralha, e um arquivo de
    overrides chaveado por número passa a nomear a comunidade errada em
    silêncio, que é pior que não nomear. O id de nó é determinístico —
    `src_main_..._villagedetector_villagedetector` é o mesmo antes e depois
    —, então "a comunidade que contém este nó chama-se X" sobrevive ao
    reembaralhamento.
    """
    node_to_community = {n["id"]: cid for cid, nodes in members.items() for n in nodes}
    resolved: dict[int, str] = {}
    unresolved: list[str] = []

    for key, name in raw.items():
        if key.lstrip("-").isdigit():
            cid = int(key)

            if cid in members:
                resolved[cid] = name
            else:
                unresolved.append(key)
        elif key in node_to_community:
            resolved[node_to_community[key]] = name
        else:
            unresolved.append(key)

    return resolved, unresolved


def vote(members: dict[int, list[dict]], reference: dict) -> dict[int, tuple[str | None, int, float]]:
    """Para cada comunidade nova, o nome curado da maioria dos seus nós."""
    curated = {n["id"]: n.get("community_name") for n in reference["nodes"]}
    result = {}

    for cid, nodes in members.items():
        votes = collections.Counter(
            curated[n["id"]] for n in nodes if curated.get(n["id"])
        )

        if votes:
            name, count = votes.most_common(1)[0]
            result[cid] = (name, count, count / len(nodes))
        else:
            result[cid] = (None, 0, 0.0)

    return result


def resolve(
    members: dict[int, list[dict]],
    ballots: dict[int, tuple[str | None, int, float]],
    overrides: dict[int, str],
    threshold: float,
) -> tuple[dict[int, str], list[int]]:
    """Nomes finais, e a lista de comunidades que o voto não decidiu.

    Um nome curado pode vencer em duas comunidades quando a antiga se
    partiu. Nesse caso ele fica com a de maior sobreposição — a herdeira —
    e a outra recebe o hub como qualificador, para os nomes seguirem
    distinguíveis num relatório.
    """
    labels: dict[int, str] = {}
    undecided: list[int] = []

    claims: dict[str, list[tuple[int, int]]] = collections.defaultdict(list)

    for cid in members:
        if cid in overrides:
            labels[cid] = overrides[cid]
            continue

        name, count, share = ballots[cid]

        if name and share >= threshold:
            claims[name].append((count, cid))
        else:
            undecided.append(cid)

    for name, claimants in claims.items():
        claimants.sort(reverse=True)

        for rank, (_, cid) in enumerate(claimants):
            if rank == 0:
                labels[cid] = name
            else:
                hub = hub_of(members[cid])
                labels[cid] = f"{name} ({hub})"[:60] if hub else f"{name} #{cid}"

    for cid in undecided:
        hub = hub_of(members[cid])
        labels[cid] = (hub or f"Community {cid}")[:60]

    return labels, undecided


def token_cost(cost_path: Path = DEFAULT_COST) -> dict[str, int]:
    """O custo acumulado da extração, como o `cost.json` o registra.

    O relatório tem de mostrar o custo — é regra do graphify, e existe para o
    grafo não parecer de graça. A primeira versão desta função fixava zero, e
    o `GRAPH_REPORT.md` passou a anunciar `0 input · 0 output` para um grafo
    que custou 1.112.964 tokens de subagente. Não era estimativa errada: era
    o número certo sendo apagado a cada regeneração.

    Zero é a resposta honesta quando o arquivo não existe — aí não houve
    extração registrada — e não um palpite.
    """
    if not cost_path.is_file():
        return {"input": 0, "output": 0}

    try:
        cost = json.loads(cost_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {"input": 0, "output": 0}

    return {
        "input": int(cost.get("total_input_tokens", 0)),
        "output": int(cost.get("total_output_tokens", 0)),
    }


def regenerate_report(graph: dict, labels: dict[int, str], report_path: Path) -> None:
    """Refaz o GRAPH_REPORT.md com os nomes novos.

    Reconstrói o grafo em memória a partir do `graph.json` porque o
    `.graphify_extract.json` é apagado na limpeza do build. A detecção é
    remontada do que o próprio grafo sabe: o relatório só a usa para o
    resumo do corpus.
    """
    import networkx as nx

    from graphify.analyze import god_nodes, suggest_questions, surprising_connections
    from graphify.cluster import score_all
    from graphify.report import generate

    G = nx.Graph()

    for node in graph["nodes"]:
        G.add_node(node["id"], **{k: v for k, v in node.items() if k != "id"})

    for edge in graph.get("edges") or graph.get("links") or []:
        source, target = edge.get("source"), edge.get("target")

        if source in G and target in G:
            G.add_edge(
                source,
                target,
                **{k: v for k, v in edge.items() if k not in ("source", "target")},
            )

    communities = collections.defaultdict(list)

    for node in graph["nodes"]:
        communities[node["community"]].append(node["id"])

    communities = dict(communities)
    files = {n["source_file"] for n in graph["nodes"] if n.get("source_file")}
    detection = {
        "total_files": len(files),
        "total_words": 0,
        "files": {
            "code": [f for f in files if f.endswith((".java", ".json", ".gradle", ".py"))],
            "document": [f for f in files if f.endswith(".md")],
        },
        "skipped_sensitive": [],
    }

    report = generate(
        G,
        communities,
        score_all(G, communities),
        labels,
        god_nodes(G),
        surprising_connections(G, communities),
        detection,
        token_cost(),
        ".",
        suggested_questions=suggest_questions(G, communities, labels),
    )
    report_path.write_text(report, encoding="utf-8")


def main() -> int:
    # O console do Windows abre em cp1252, e rótulos do projeto trazem seta e
    # acento (`Tick → Update Colony`). Sem isto o script morre imprimindo o
    # relatório que acabou de calcular — e justamente na parte que o operador
    # precisa ler para escrever os --overrides.
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass

    parser = argparse.ArgumentParser(
        description="Transporta nomes de comunidade curados para o grafo atualizado.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument(
        "--reference",
        required=True,
        type=Path,
        help="graph.json de antes do update, com os nomes curados",
    )
    parser.add_argument("--graph", type=Path, default=DEFAULT_GRAPH)
    parser.add_argument("--labels", type=Path, default=DEFAULT_LABELS)
    parser.add_argument("--report", type=Path, default=DEFAULT_REPORT)
    parser.add_argument(
        "--overrides",
        type=Path,
        help='JSON {"<id da comunidade>": "Nome"} para as que o voto não decide',
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=0.5,
        help="fração mínima de nós que precisa concordar (padrão 0.5)",
    )
    parser.add_argument("--dry-run", action="store_true", help="mostra o plano, não grava")
    parser.add_argument("--no-report", action="store_true", help="não refaz o GRAPH_REPORT.md")
    parser.add_argument("--no-html", action="store_true", help="não refaz o graph.html")
    args = parser.parse_args()

    for path in (args.graph, args.reference):
        if not path.is_file():
            print(f"erro: {path} não existe", file=sys.stderr)
            return 1

    graph = load_json(args.graph)
    reference = load_json(args.reference)
    overrides = {}

    if args.overrides:
        if not args.overrides.is_file():
            print(f"erro: {args.overrides} não existe", file=sys.stderr)
            return 1

        raw_overrides = load_json(args.overrides)

    members = community_members(graph)

    if args.overrides:
        overrides, unresolved = parse_overrides(raw_overrides, members)

        if unresolved:
            print(
                f"aviso: {len(unresolved)} chave(s) de --overrides não casaram com "
                f"nenhuma comunidade nem nó deste grafo, e foram ignoradas:",
                file=sys.stderr,
            )

            for key in unresolved:
                print(f"  {key}", file=sys.stderr)

    ballots = vote(members, reference)
    labels, undecided = resolve(members, ballots, overrides, args.threshold)

    decided = len(members) - len(undecided) - len(overrides)
    print(
        f"{len(members)} comunidades: {decided} pelo voto, "
        f"{len(overrides)} por --overrides, {len(undecided)} sem maioria"
    )
    print(f"{len(set(labels.values()))} nomes distintos")

    if undecided:
        print(
            f"\nsem maioria de {args.threshold:.0%} — nomeie estas em --overrides "
            f"(por ora ficam com o hub):"
        )

        for cid in sorted(undecided, key=lambda c: -len(members[c])):
            name, _, share = ballots[cid]
            sample = ", ".join((n.get("label") or "") for n in members[cid][:6])
            print(f'  [{cid}] n={len(members[cid])} -> "{labels[cid]}"')
            print(f"        melhor voto: {name} ({share:.0%}) | {sample}")

    if args.dry_run:
        print("\n--dry-run: nada foi gravado.")
        return 0

    for node in graph["nodes"]:
        node["community_name"] = labels[node["community"]]

    args.graph.write_text(json.dumps(graph, ensure_ascii=False), encoding="utf-8")
    args.labels.write_text(
        json.dumps({str(k): v for k, v in labels.items()}, ensure_ascii=False),
        encoding="utf-8",
    )
    LABELS_SIG.unlink(missing_ok=True)
    print(f"\ngravado: {args.graph}, {args.labels}")

    if not args.no_report:
        regenerate_report(graph, labels, args.report)
        print(f"gravado: {args.report}")

    if not args.no_html:
        html = args.graph.parent / "graph.html"
        html.unlink(missing_ok=True)
        subprocess.run([sys.executable, "-m", "graphify", "export", "html"], check=True)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
