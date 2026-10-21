#!/usr/bin/env python
"""Recorta as 6 faces de cada mockup 1254px do autor em texturas HD 64x64.

Os mockups são uma grade de 4 colunas x 3 linhas de painéis, cada um com a
legenda azul-clara logo abaixo. O recorte de 2026-10-15 usava proporções fixas
e pegou a legenda da linha de cima nas faces da segunda linha (as tarjas pretas
com "FRENTE"/"TOPO"). Agora as linhas saem das faixas de legenda achadas na
imagem, e cada painel é a caixa do que não é fundo preto dentro da sua célula.

Faces (2026-10-20):
  up    <- TOPO            (linha 0, col 0)
  north <- FRENTE          (linha 0, col 1)
  west  <- LATERAL ESQ     (linha 0, col 2)
  east  <- LATERAL DIR     (linha 0, col 3)
  down  <- FUNDO           (linha 1, col 0)
  south <- BORDA INFERIOR  (linha 1, col 3) — a PARTE SUPERIOR repetia o topo

Uso: python crop_benches_6faces.py <pasta-dos-mockups> <pasta-de-saída>
"""
from PIL import Image
import os
import sys

SIZE = 64

MAP = {
    "1": "lumberjack_bench",
    "2": "carpenter_bench",
    "3": "mason_cutter",
    "4": "shepherd_bench",
    "5": "builder_bench",
    "6": "miner_bench",
    "7": "blacksmith_forge",
}

FACES = {
    "up": (0, 0),
    "north": (0, 1),
    "west": (0, 2),
    "east": (0, 3),
    "down": (1, 0),
    "south": (1, 3),
}


def is_label(c):
    """A legenda é azul-clara sobre preto."""
    return c[2] > 170 and c[2] > c[0] + 40 and c[1] > 120


def label_bands(im):
    """As faixas de y em que há legenda, de cima para baixo."""
    w, h = im.size
    px = im.load()
    bands, start, last = [], None, None

    for y in range(h):
        hits = sum(1 for x in range(0, w, 2) if is_label(px[x, y]))

        if hits > 8:
            start = y if start is None else start
            last = y
        elif start is not None and y - last > 6:
            if last - start >= 6:
                bands.append((start, last))
            start = None

    return bands


def panel_box(im, x0, y0, x1, y1):
    """A caixa do painel dentro da célula: o que não é fundo preto nem legenda."""
    px = im.load()
    xs, ys = [], []

    for y in range(y0, y1):
        for x in range(x0, x1, 2):
            c = px[x, y]
            if sum(c) > 90 and not is_label(c):
                xs.append(x)
                ys.append(y)

    # 2 px para dentro: a borda do painel tem antisserrilhado com o preto.
    return min(xs) + 2, min(ys) + 2, max(xs) - 2, max(ys) - 2


def main(src, out):
    for num, name in MAP.items():
        im = Image.open(os.path.join(src, f"{num}.webp")).convert("RGB")
        w, _ = im.size
        bands = label_bands(im)
        # Linha 0: do topo até a 1ª legenda; linha 1: entre a 1ª e a 2ª.
        rows = [(0, bands[0][0] - 2), (bands[0][1] + 2, bands[1][0] - 2)]
        cell = w / 4.0

        for face, (row, col) in FACES.items():
            box = panel_box(im, int(col * cell), rows[row][0], int((col + 1) * cell), rows[row][1])
            im.crop(box).resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(out, f"{name}_{face}.png"))

        print("  ", name, "linhas", rows)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
