#!/usr/bin/env python
"""Recorta as faces dos mockups 1254px do autor em texturas HD 64x64.

Cada mockup tem 12 faces em grade 4 col x 3 linhas (+ rodapé). Usamos as 3 da
primeira linha: TOPO (col0) -> _top, FRENTE (col1) -> _front, LAT ESQ (col2) -> _side.
Mapa mockup -> bancada confirmado pelos títulos (15-10).
"""
from PIL import Image
import os, sys

IMGDIR = sys.argv[1]
OUT = sys.argv[2]
SIZE = 64

# img -> nome do bloco (sem sufixo). FARMER fica no 16px (sem mockup próprio).
MAP = {
    "1": "lumberjack_bench",
    "2": "carpenter_bench",
    "3": "mason_cutter",
    "4": "shepherd_bench",
    "5": "builder_bench",
    "6": "miner_bench",     # Mesa de Trabalho genérica
    "7": "blacksmith_forge",
}

# mason_cutter usa top/side/bottom/saw; os demais top/front/side.
for num, name in MAP.items():
    im = Image.open(os.path.join(IMGDIR, f"{num}.webp")).convert("RGBA")
    W, H = im.size
    col = W / 4.0
    padx = col * 0.05
    # a face ocupa a largura da coluna e ~82% da altura da linha (resto é legenda)
    def face(c):
        x0 = c * col
        return im.crop((int(x0 + padx), int(padx), int(x0 + col - padx), int(col * 0.82)))

    faces = {"top": face(0), "front": face(1), "side": face(2)}
    for suf, f in faces.items():
        f.resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, f"{name}_{suf}.png"))

    # mason_cutter tem 4 texturas: top, side, bottom, saw. Mapeia front->saw, e gera bottom do FUNDO.
    if name == "mason_cutter":
        faces["front"].resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, "mason_cutter_saw.png"))
        # FUNDO fica na linha 2, col 0
        fundo = im.crop((int(padx), int(col + padx), int(col - padx), int(col * 1.82)))
        fundo.resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, "mason_cutter_bottom.png"))
    print("  ", name)

print("recorte HD pronto")
