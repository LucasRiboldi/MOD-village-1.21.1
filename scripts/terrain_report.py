"""Relatório do terreno da vila (ADR-064): compara a primeira foto do terreno com a última.

Lê as fotos `villagecolony-terrain-<colônia>-<rótulo>.json.gz` da pasta de logs do jogo e as linhas `VC_PLAYER_EDIT` e
`VC_PLAYER_UNDO` do log. Diz a parte da vila que se anda a pé do centro com degrau de até 1, os degraus maiores que 1,
quantos lados de cada estrutura se alcançam, as portas que não se alcançam, a terra que cada um mexeu e o que o jogador
quis em cada coluna que mexeu. Desenha o mapa visto de cima.

Uso:
    python scripts/terrain_report.py                      # pasta de logs do TLauncher, colônia mais recente
    python scripts/terrain_report.py --logs <pasta> --colony 5bbe35c7 --out reports/terrain
"""

import argparse
import gzip
import json
import os
import re
from collections import Counter, deque
from pathlib import Path

NO_HEIGHT = -32768
KIND_UNKNOWN, KIND_NATURAL, KIND_PATH, KIND_WATER, KIND_COLONY, KIND_OTHER = range(6)
PLAYER, MOD, WORLD = 1, 2, 4
FILE = re.compile(r"villagecolony-terrain-([0-9a-f]{8})-(start|day(\d+)|last)\.json\.gz$")
EDIT = re.compile(r"VC_PLAYER_EDIT version=1 action=(\w+) block=(\S+) was=(\S+) target=(\S+) at=(-?\d+), (-?\d+), (-?\d+)")
UNDO = re.compile(r"VC_PLAYER_UNDO version=1 system=(\S+) .*? at=(-?\d+), (-?\d+), (-?\d+)")
SIDES = ("norte", "sul", "oeste", "leste")


class Snapshot:
    """Uma foto: as listas da foto viram grades [x][z] da caixa com margem."""

    def __init__(self, data):
        self.data = data
        self.label = data["label"]
        self.day = data.get("day", 0)
        self.min_x = data["minX"]
        self.min_z = data["minZ"]
        self.width = data["width"]
        self.depth = data["depth"]
        self.center = data["center"]
        self.y = data["y"]
        self.kind = data["kind"]
        self.edit = data.get("edit", [0] * len(self.y))
        self.target = data.get("target", [NO_HEIGHT] * len(self.y))
        self.structures = data.get("structures", [])
        self.doors = data.get("doors", [])
        self.footprint = self._footprint()

    def index(self, x, z):
        dx, dz = x - self.min_x, z - self.min_z
        if 0 <= dx < self.width and 0 <= dz < self.depth:
            return dx * self.depth + dz
        return None

    def at(self, x, z):
        i = self.index(x, z)
        return None if i is None or self.y[i] == NO_HEIGHT else self.y[i]

    def _footprint(self):
        cells = set()
        for s in self.structures:
            for x in range(s["min"][0], s["max"][0] + 1):
                for z in range(s["min"][2], s["max"][2] + 1):
                    cells.add((x, z))
        return cells

    def walkable(self, x, z):
        i = self.index(x, z)
        return (i is not None and self.y[i] != NO_HEIGHT and self.kind[i] in (KIND_NATURAL, KIND_PATH, KIND_OTHER)
                and (x, z) not in self.footprint)


def load(path):
    with gzip.open(path, "rt", encoding="utf-8") as handle:
        return Snapshot(json.load(handle))


def find(logs, colony=None):
    """As fotos por colônia: {colônia: {rótulo: caminho}}; a colônia pedida ou todas."""
    found = {}
    for name in os.listdir(logs):
        match = FILE.match(name)
        if match and (colony is None or match.group(1).startswith(colony)):
            found.setdefault(match.group(1), {})[match.group(2)] = Path(logs) / name
    return found


def first_and_last(files):
    """A foto do começo e a mais recente: `last`, ou o maior dia."""
    days = sorted((int(label[3:]), path) for label, path in files.items() if label.startswith("day"))
    first = files.get("start") or (days[0][1] if days else files.get("last"))
    last = files.get("last") or (days[-1][1] if days else first)
    return first, last


def neighbours(x, z):
    return ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1))


def reach(snap):
    """As colunas que se andam a pé a partir do centro, com degrau de até 1."""
    cx, cz = snap.center[0], snap.center[2]
    start = None
    for radius in range(0, 12):
        ring = [(cx + dx, cz + dz) for dx in range(-radius, radius + 1) for dz in range(-radius, radius + 1)
                if max(abs(dx), abs(dz)) == radius]
        start = next((c for c in ring if snap.walkable(*c)), None)
        if start:
            break
    if start is None:
        return set()
    seen = {start}
    open_ = deque([start])
    while open_:
        x, z = open_.popleft()
        here = snap.at(x, z)
        for nxt in neighbours(x, z):
            if nxt not in seen and snap.walkable(*nxt) and abs(snap.at(*nxt) - here) <= 1:
                seen.add(nxt)
                open_.append(nxt)
    return seen


def steps(snap):
    """Degraus maiores que 1 entre colunas que se andam: no total, nos caminhos e a até 2 de uma porta."""
    doors = {(d[0], d[2]) for d in snap.doors}
    near_door = {(x + dx, z + dz) for x, z in doors for dx in range(-2, 3) for dz in range(-2, 3)}
    total = path = door = 0
    for dx in range(snap.width):
        for dz in range(snap.depth):
            x, z = snap.min_x + dx, snap.min_z + dz
            if not snap.walkable(x, z):
                continue
            for nxt in ((x + 1, z), (x, z + 1)):
                if snap.walkable(*nxt) and abs(snap.at(*nxt) - snap.at(x, z)) > 1:
                    total += 1
                    i, j = snap.index(x, z), snap.index(*nxt)
                    path += snap.kind[i] == KIND_PATH or snap.kind[j] == KIND_PATH
                    door += (x, z) in near_door or nxt in near_door
    return {"total": total, "path": path, "door": door}


def structure_access(snap, reached):
    """Por estrutura pronta: quantos lados se alcançam a pé (degrau de até 1 da entrada) e se a porta se alcança."""
    result = []
    for index, s in enumerate(snap.structures):
        if not s.get("finished", True):
            continue
        own = [d for d in snap.doors if d[3] == index]
        level = min(d[1] for d in own) if own else s["min"][1] - 1
        (x0, _, z0), (x1, _, z1) = s["min"], s["max"]
        rings = {"norte": [(x, z0 - 1) for x in range(x0, x1 + 1)], "sul": [(x, z1 + 1) for x in range(x0, x1 + 1)],
                 "oeste": [(x0 - 1, z) for z in range(z0, z1 + 1)], "leste": [(x1 + 1, z) for z in range(z0, z1 + 1)]}
        open_sides = [side for side in SIDES
                      if any(c in reached and abs(snap.at(*c) - level) <= 1 for c in rings[side])]
        door_ok = not own or any((d[0], d[2]) in reached
                                 or any(n in reached and abs(snap.at(*n) - d[1]) <= 1 for n in neighbours(d[0], d[2]))
                                 for d in own)
        result.append({"index": index, "source": s["source"], "blueprint": s.get("blueprint", ""), "min": s["min"],
                       "sides": open_sides, "door": door_ok})
    return result


def edits_from_logs(paths, snap):
    """As colunas que o jogador mexeu dentro da foto, e os sistemas que ele desfez em cada uma."""
    player, undo = Counter(), {}
    for path in paths:
        with open(path, encoding="utf-8", errors="replace") as handle:
            for line in handle:
                m = EDIT.search(line)
                if m and snap.index(int(m.group(5)), int(m.group(7))) is not None:
                    player[(int(m.group(5)), int(m.group(7)))] += 1
                u = UNDO.search(line)
                if u and snap.index(int(u.group(2)), int(u.group(4))) is not None:
                    undo[(int(u.group(2)), int(u.group(4)))] = u.group(1)
    return player, undo


def local_step(snap, x, z):
    here = snap.at(x, z)
    if here is None:
        return 0
    return max([abs(snap.at(*n) - here) for n in neighbours(x, z) if snap.at(*n) is not None] or [0])


def intent(first, last, player, undo, reached_first=None, reached_last=None):
    """O que o jogador quis em cada coluna que mexeu, pela foto do começo e a última: a coluna que passou a se alcançar a
    pé do centro é passagem aberta; a que deixou de se alcançar, passagem fechada."""
    reached_first = reach(first) if reached_first is None else reached_first
    reached_last = reach(last) if reached_last is None else reached_last
    kinds = Counter()
    after = Counter()
    door_distance = Counter()
    undone = Counter()
    plan_gap = []
    doors = [(d[0], d[2]) for d in last.doors]
    for (x, z) in player:
        before, now = local_step(first, x, z), local_step(last, x, z)
        y0, y1 = first.at(x, z), last.at(x, z)
        if (x, z) in undo:
            kinds["desfez " + undo[(x, z)]] += 1
            if y0 is not None and y1 is not None:
                undone[(undo[(x, z)], y1 - y0)] += 1
        elif (x, z) in reached_last and (x, z) not in reached_first:
            kinds["abriu passagem"] += 1
        elif (x, z) in reached_first and (x, z) not in reached_last:
            kinds["fechou passagem"] += 1
        elif before <= 1 < now:
            kinds["fez desnível"] += 1
        elif y0 is not None and y1 is not None and y0 != y1 and now <= before:
            kinds["aplainou"] += 1
        else:
            kinds["outro"] += 1
        after["até 1" if now <= 1 else "2" if now == 2 else "3 ou mais"] += 1
        if doors:
            d = min(abs(x - a) + abs(z - b) for a, b in doors)
            door_distance["0–1" if d <= 1 else "2–3" if d <= 3 else "4–6" if d <= 6 else "7+"] += 1
        i = last.index(x, z)
        if i is not None and last.target[i] != NO_HEIGHT and y1 is not None:
            plan_gap.append(abs(y1 - last.target[i]))
    return {"kinds": kinds, "after": after, "door_distance": door_distance, "undone": undone, "plan_gap": plan_gap}


def earthwork(first, last):
    """Colunas que mudaram de altura e o volume, por quem mexeu (o que a última foto sabe)."""
    by = Counter()
    volume = Counter()
    for i, b in enumerate(last.y):
        x, z = last.min_x + i // last.depth, last.min_z + i % last.depth
        a = first.at(x, z)
        if a is None or b == NO_HEIGHT or a == b:
            continue
        flags = last.edit[i]
        who = "jogador" if flags & PLAYER else "mod" if flags & MOD else "mundo" if flags & WORLD else "sem dono"
        by[who] += 1
        volume[who] += abs(b - a)
    return by, volume


def draw(snap, reached, player, out):
    """O mapa visto de cima: altura em cinza, água azul, estrutura escura, degrau >1 vermelho, jogador magenta, mod
    verde, porta amarela (vermelha se não se alcança)."""
    try:
        from PIL import Image
    except ImportError:
        return None
    scale = 3
    known = [v for v in snap.y if v != NO_HEIGHT]
    low, high = (min(known), max(known)) if known else (0, 1)
    image = Image.new("RGB", (snap.width * scale, snap.depth * scale), (0, 0, 0))
    pixels = image.load()
    for dx in range(snap.width):
        for dz in range(snap.depth):
            x, z = snap.min_x + dx, snap.min_z + dz
            i = snap.index(x, z)
            h = snap.y[i]
            if h == NO_HEIGHT:
                color = (0, 0, 0)
            elif snap.kind[i] == KIND_WATER:
                color = (40, 90, 200)
            elif (x, z) in snap.footprint:
                color = (60, 50, 45)
            else:
                g = int(60 + 170 * (h - low) / max(1, high - low))
                color = (g, g, g) if (x, z) in reached else (g, int(g * 0.8), int(g * 0.6))
                if snap.kind[i] == KIND_PATH:
                    color = (200, 170, 110)
            if (x, z) in player:
                color = (220, 40, 200)
            elif snap.edit[i] & MOD:
                color = (color[0] // 2, min(255, color[1] + 80), color[2] // 2)
            if snap.walkable(x, z) and any(snap.walkable(*n) and abs(snap.at(*n) - h) > 1 for n in neighbours(x, z)):
                color = (230, 30, 30)
            for px in range(scale):
                for pz in range(scale):
                    pixels[dx * scale + px, dz * scale + pz] = color
    for d in snap.doors:
        i = snap.index(d[0], d[2])
        if i is not None:
            color = (250, 220, 0) if (d[0], d[2]) in reached else (255, 0, 0)
            for px in range(-1, scale + 1):
                for pz in range(-1, scale + 1):
                    px_, pz_ = (d[0] - snap.min_x) * scale + px, (d[2] - snap.min_z) * scale + pz
                    if 0 <= px_ < snap.width * scale and 0 <= pz_ < snap.depth * scale:
                        pixels[px_, pz_] = color
    image.save(out)
    return out


def summarize(first, last, logs_for_edits):
    """As medidas das duas fotos, num dicionário (o game_report.py usa o resumo)."""
    reached_first, reached_last = reach(first), reach(last)
    walk_last = sum(1 for dx in range(last.width) for dz in range(last.depth)
                    if last.walkable(last.min_x + dx, last.min_z + dz))
    walk_first = sum(1 for dx in range(first.width) for dz in range(first.depth)
                     if first.walkable(first.min_x + dx, first.min_z + dz))
    player, undo = edits_from_logs(logs_for_edits, last)
    return {
        "first": first, "last": last, "reached_last": reached_last,
        "reach": (len(reached_first), walk_first, len(reached_last), walk_last),
        "steps": (steps(first), steps(last)),
        "access": structure_access(last, reached_last),
        "earthwork": earthwork(first, last),
        "player": player,
        "intent": intent(first, last, player, undo, reached_first, reached_last),
    }


def pct(part, whole):
    return f"{100 * part / whole:.0f}%" if whole else "—"


def markdown(colony, m, image):
    first, last = m["first"], m["last"]
    rf, wf, rl, wl = m["reach"]
    sf, sl = m["steps"]
    lines = [f"# Terreno da colônia {colony} (ADR-064)", "",
             f"Fotos: `{first.label}` (dia {first.day}) e `{last.label}` (dia {last.day}); {last.width}×{last.depth} colunas.",
             "", "## Acesso a pé do centro, com degrau de até 1", "",
             "| | começo | agora |", "|---|---|---|",
             f"| colunas alcançadas | {rf} de {wf} ({pct(rf, wf)}) | {rl} de {wl} ({pct(rl, wl)}) |",
             f"| degraus maiores que 1 | {sf['total']} | {sl['total']} |",
             f"| … em caminho | {sf['path']} | {sl['path']} |",
             f"| … a até 2 de uma porta | {sf['door']} | {sl['door']} |", ""]
    access = m["access"]
    by_sides = Counter(len(a["sides"]) for a in access)
    lines += ["## Estruturas", "", f"{len(access)} estruturas prontas; lados alcançados: "
              + ", ".join(f"{n} lados: {by_sides[n]}" for n in range(5)) + ".", ""]
    blocked = [a for a in access if not a["door"] or len(a["sides"]) < 2]
    if blocked:
        lines += ["| estrutura | canto | lados | porta alcançada |", "|---|---|---|---|"]
        for a in blocked[:30]:
            lines.append(f"| {a['source']} {a['blueprint']} | {a['min'][0]},{a['min'][1]},{a['min'][2]} | "
                         f"{', '.join(a['sides']) or 'nenhum'} | {'sim' if a['door'] else 'não'} |")
        lines.append("")
    by, volume = m["earthwork"]
    lines += ["## Terra mexida desde o começo", "", "| quem | colunas | blocos de altura |", "|---|---|---|"]
    lines += [f"| {who} | {by[who]} | {volume[who]} |" for who in ("mod", "jogador", "mundo", "sem dono") if by[who]]
    it = m["intent"]
    lines += ["", f"## O que o jogador quis ({len(m['player'])} colunas mexidas)", ""]
    lines += [f"- {kind}: {count}" for kind, count in it["kinds"].most_common()]
    if it["after"]:
        lines.append("- degrau que ficou em volta: " + ", ".join(f"{k}: {v}" for k, v in sorted(it["after"].items())))
    if it["door_distance"]:
        lines.append("- distância da porta mais perto: "
                     + ", ".join(f"{k}: {it['door_distance'][k]}" for k in ("0–1", "2–3", "4–6", "7+")))
    if it["undone"]:
        lines.append("- o que ele desfez (sistema, altura que ficou menos a do começo): "
                     + ", ".join(f"{s} {d:+d}: {n}" for (s, d), n in it["undone"].most_common(12)))
    if it["plan_gap"]:
        gap = it["plan_gap"]
        lines.append(f"- distância do alvo do plano nas colunas dele: média {sum(gap) / len(gap):.1f}, "
                     f"no alvo {sum(1 for g in gap if g == 0)} de {len(gap)}")
    if image:
        lines += ["", f"Mapa: `{image}`"]
    return "\n".join(lines) + "\n"


def summary_lines(logs, log_paths):
    """O resumo para o game_report.py: uma linha por colônia com foto."""
    out = []
    for colony, files in sorted(find(logs).items()):
        first, last = first_and_last(files)
        if not first or not last:
            continue
        m = summarize(load(first), load(last), log_paths)
        rf, wf, rl, wl = m["reach"]
        sides = Counter(len(a["sides"]) for a in m["access"])
        out.append(f"{colony}: a pé {pct(rf, wf)} → {pct(rl, wl)}; degraus >1 {m['steps'][0]['total']} → "
                   f"{m['steps'][1]['total']}; estruturas com menos de 2 lados {sides[0] + sides[1]} de "
                   f"{len(m['access'])}; colunas do jogador {len(m['player'])}")
    return out


def main():
    default_logs = Path(os.environ.get("APPDATA", ".")) / ".minecraft" / "logs"
    parser = argparse.ArgumentParser(description="Relatório do terreno da vila (ADR-064).")
    parser.add_argument("--logs", default=str(default_logs))
    parser.add_argument("--colony")
    parser.add_argument("--log", nargs="*", help="logs com VC_PLAYER_EDIT; padrão: latest.log da pasta")
    parser.add_argument("--out", default="reports/terrain")
    args = parser.parse_args()
    found = find(args.logs, args.colony)
    if not found:
        raise SystemExit("nenhuma foto do terreno em " + args.logs)
    logs_for_edits = args.log if args.log is not None else [p for p in [Path(args.logs) / "latest.log"] if p.exists()]
    os.makedirs(args.out, exist_ok=True)
    colony = max(found, key=lambda c: max(os.path.getmtime(p) for p in found[c].values()))
    first, last = first_and_last(found[colony])
    m = summarize(load(first), load(last), logs_for_edits)
    image = draw(m["last"], m["reached_last"], m["player"], os.path.join(args.out, f"{colony}-{m['last'].label}.png"))
    text = markdown(colony, m, image)
    path = os.path.join(args.out, f"{colony}-{m['last'].label}.md")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(text)
    print(text)
    print("salvo em", path)


if __name__ == "__main__":
    main()
