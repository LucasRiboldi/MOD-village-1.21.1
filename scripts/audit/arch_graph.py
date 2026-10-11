"""Grafo de pacotes do mod a partir do bytecode (saída do jdeps).

Lê `jdeps -verbose:package -filter:none` e responde três perguntas que o
ArchUnit não responde de uma vez:

1. há ciclo entre pacotes do mod? (componentes fortemente conexos)
2. algum pacote `core` depende de pacote fora do `core`?
3. algum pacote `core` depende de Minecraft ou Fabric?

Uso: python -I scripts/audit/arch_graph.py <saida-do-jdeps.txt> <relatorio.json>
Saída 1 se houver dependência do core para fora (regra de camadas, ADR de
camadas); ciclos só são relatados (dívida, não quebra de regra).
"""
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict

EDGE = re.compile(r'^\s+(com\.villagecolony\S*)\s+->\s+(\S+)\s+(\S+)')
OWN = 'com.villagecolony'
EXTERNAL_FORBIDDEN = ('net.minecraft', 'net.fabricmc', 'com.mojang')


def parse(path: str) -> dict[str, set[str]]:
    graph: dict[str, set[str]] = defaultdict(set)
    with open(path, encoding='utf-8', errors='replace') as f:
        for line in f:
            m = EDGE.match(line)
            if m and m.group(1) != m.group(2):
                graph[m.group(1)].add(m.group(2))
    return graph


def strongly_connected(graph: dict[str, set[str]]) -> list[list[str]]:
    """Tarjan iterativo; devolve só componentes com mais de um pacote."""
    index: dict[str, int] = {}
    low: dict[str, int] = {}
    on_stack: set[str] = set()
    stack: list[str] = []
    result: list[list[str]] = []
    counter = 0
    nodes = set(graph) | {t for ts in graph.values() for t in ts if t.startswith(OWN)}
    for root in sorted(nodes):
        if root in index:
            continue
        work = [(root, iter(sorted(t for t in graph.get(root, ()) if t.startswith(OWN))))]
        index[root] = low[root] = counter
        counter += 1
        stack.append(root)
        on_stack.add(root)
        while work:
            node, it = work[-1]
            advanced = False
            for nxt in it:
                if nxt not in index:
                    index[nxt] = low[nxt] = counter
                    counter += 1
                    stack.append(nxt)
                    on_stack.add(nxt)
                    work.append((nxt, iter(sorted(t for t in graph.get(nxt, ()) if t.startswith(OWN)))))
                    advanced = True
                    break
                if nxt in on_stack:
                    low[node] = min(low[node], index[nxt])
            if advanced:
                continue
            work.pop()
            if work:
                low[work[-1][0]] = min(low[work[-1][0]], low[node])
            if low[node] == index[node]:
                comp = []
                while True:
                    w = stack.pop()
                    on_stack.discard(w)
                    comp.append(w)
                    if w == node:
                        break
                if len(comp) > 1:
                    result.append(sorted(comp))
    return result


def main() -> int:
    if len(sys.argv) != 3 or sys.argv[1] in ('-h', '--help'):
        print(__doc__)
        return 0 if len(sys.argv) > 1 and sys.argv[1] in ('-h', '--help') else 2
    graph = parse(sys.argv[1])
    own_pkgs = sorted(p for p in graph if p.startswith(OWN))
    core = [p for p in own_pkgs if p.startswith(OWN + '.core')]
    core_out = sorted({(p, t) for p in core for t in graph[p]
                       if t.startswith(OWN) and not t.startswith(OWN + '.core')})
    core_ext = sorted({(p, t) for p in core for t in graph[p] if t.startswith(EXTERNAL_FORBIDDEN)})
    cycles = strongly_connected(graph)
    report = {
        'packages': len(own_pkgs),
        'edges': sum(len([t for t in ts if t.startswith(OWN)]) for ts in graph.values()),
        'core_packages': len(core),
        'core_to_non_core': [f'{a} -> {b}' for a, b in core_out],
        'core_to_minecraft_or_fabric': [f'{a} -> {b}' for a, b in core_ext],
        'cycles': cycles,
    }
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)
    print(f"pacotes={report['packages']} arestas={report['edges']} core={report['core_packages']}")
    print(f"core->fora do core: {len(core_out)}  core->Minecraft/Fabric: {len(core_ext)}  ciclos: {len(cycles)}")
    for c in cycles:
        print('  ciclo:', ' <-> '.join(x.replace(OWN + '.', '') for x in c))
    for a, b in core_out + core_ext:
        print('  VIOLA:', a, '->', b)
    return 1 if (core_out or core_ext) else 0


if __name__ == '__main__':
    sys.exit(main())
