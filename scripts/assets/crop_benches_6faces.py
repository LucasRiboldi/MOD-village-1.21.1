#!/usr/bin/env python
"""Recorta as 6 faces distintas de cada mockup 1254px em texturas HD 64x64.

Grade 4 col x 3 linhas (+ rodapé). As 6 faces do cubo:
  up    <- TOPO       (linha 0, col 0)
  north <- FRENTE     (linha 0, col 1)
  west  <- LAT ESQ    (linha 0, col 2)
  east  <- LAT DIR    (linha 0, col 3)
  down  <- FUNDO      (linha 1, col 0)
  south <- PARTE SUP  (linha 1, col 1)
Cada bloco ganha um model com os 6 lados diferentes. Mapa mockup->bloco confirmado 15-10.
"""
from PIL import Image
import os, sys

IMGDIR = sys.argv[1]
OUT = sys.argv[2]
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

# face -> (col, row) na grade
FACES = {
    "up":    (0, 0),
    "north": (1, 0),
    "west":  (2, 0),
    "east":  (3, 0),
    "down":  (0, 1),
    "south": (1, 1),
}

for num, name in MAP.items():
    im = Image.open(os.path.join(IMGDIR, f"{num}.webp")).convert("RGBA")
    W, H = im.size
    col = W / 4.0
    padx = col * 0.05

    for face, (c, r) in FACES.items():
        x0 = c * col
        y0 = r * col
        # a face ocupa a largura da coluna e ~82% da altura da linha (resto é legenda)
        crop = im.crop((int(x0 + padx), int(y0 + padx), int(x0 + col - padx), int(y0 + col * 0.82)))
        crop.resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, f"{name}_{face}.png"))

    print("  ", name)

print("6 faces por bloco prontas")
