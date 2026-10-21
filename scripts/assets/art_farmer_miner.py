#!/usr/bin/env python
"""Arte das 6 faces do fazendeiro e do mineiro, que não têm mockup próprio (2026-10-20).

O fundo vem dos mockups do autor (a textura de tábuas do lenhador e a de pedra do
pedreiro, na terceira linha de painéis), com moldura e cantoneiras na paleta deles;
por cima, os objetos do ofício, desenhados a 64 px com contorno e luz de cima à
esquerda, como nos mockups. Cada face mostra uma parte do trabalho:

  fazendeiro: topo = composteira cheia; frente = feixe de trigo e foice; leste =
  enxada e regador; oeste = sacos de sementes; trás = cenouras e batatas; fundo = tábuas.
  mineiro: topo = picaretas cruzadas sobre minério; frente = boca de mina com trilho e
  carrinho; leste = minérios (carvão, ferro, ouro); oeste = lanterna e pá; trás = pilha
  de pedra; fundo = grade de ferro.

Uso: python art_farmer_miner.py <pasta-dos-mockups> <pasta-de-saída>
"""
from PIL import Image, ImageDraw
import os
import sys

S = 64

INK = (34, 22, 14)
GOLD, GOLD_L, GOLD_D = (214, 154, 33), (245, 200, 80), (150, 98, 18)
IRON, IRON_L, IRON_D = (110, 112, 120), (170, 172, 180), (60, 62, 70)
WOOD, WOOD_L, WOOD_D = (150, 98, 52), (190, 132, 74), (96, 60, 32)
WHEAT, WHEAT_L, WHEAT_D = (222, 182, 70), (246, 222, 120), (170, 128, 40)
GREEN, GREEN_L, GREEN_D = (78, 132, 48), (118, 176, 70), (48, 86, 30)
SOIL, SOIL_D = (92, 62, 38), (62, 40, 24)
STONE, STONE_L, STONE_D = (124, 124, 128), (160, 160, 164), (82, 82, 88)
COAL = (34, 34, 38)
ORE_IRON, ORE_GOLD = (216, 176, 140), (250, 214, 70)
ORANGE, ORANGE_L = (232, 118, 30), (252, 190, 80)
BURLAP, BURLAP_D = (196, 166, 112), (146, 118, 74)
CARROT, CARROT_L = (238, 128, 34), (252, 176, 80)
POTATO, POTATO_D = (196, 150, 84), (140, 100, 50)


def panel(src, num, row, col):
    """Um painel do mockup (achado pelas faixas de legenda, como em crop_benches_6faces)."""
    sys.path.insert(0, os.path.dirname(__file__))
    from crop_benches_6faces import label_bands, panel_box
    im = Image.open(os.path.join(src, f"{num}.webp")).convert("RGB")
    bands = label_bands(im)
    rows = [(0, bands[0][0] - 2), (bands[0][1] + 2, bands[1][0] - 2), (bands[1][1] + 2, bands[2][0] - 2)]
    cell = im.size[0] / 4.0
    box = panel_box(im, int(col * cell), rows[row][0], int((col + 1) * cell), rows[row][1])
    return im.crop(box).resize((S, S), Image.LANCZOS)


def framed(background, edge, corner):
    """Moldura de 4 px e cantoneiras douradas, como nos mockups."""
    im = background.copy()
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, S - 1, S - 1], outline=INK, width=1)
    d.rectangle([1, 1, S - 2, S - 2], outline=edge, width=3)
    for cx, cy in ((1, 1), (S - 11, 1), (1, S - 11), (S - 11, S - 11)):
        d.rectangle([cx, cy, cx + 9, cy + 9], fill=corner[0], outline=INK)
        d.rectangle([cx + 1, cy + 1, cx + 8, cy + 2], fill=corner[1])
        d.rectangle([cx + 4, cy + 4, cx + 5, cy + 5], fill=INK)
    return im


def poly(d, pts, fill, light=None):
    d.polygon(pts, fill=fill, outline=INK)
    if light:
        d.line(pts[:2], fill=light)


# ---- objetos -------------------------------------------------------------------

def wheat_sheaf(d, x, y, h=30):
    for i, dx in enumerate(range(-8, 9, 4)):
        d.line([(x + dx // 2, y + h), (x + dx, y + 8)], fill=WHEAT_D, width=2)
        for k in range(4):
            yy = y + 2 + k * 3
            d.ellipse([x + dx - 2, yy, x + dx + 2, yy + 4], fill=WHEAT if (i + k) % 2 else WHEAT_L, outline=WHEAT_D)
    d.rectangle([x - 6, y + h - 12, x + 6, y + h - 9], fill=GREEN_D, outline=INK)


def scythe(d, x, y):
    d.line([(x, y), (x + 4, y + 30)], fill=WOOD_D, width=3)
    d.line([(x, y), (x + 4, y + 30)], fill=WOOD_L, width=1)
    poly(d, [(x - 1, y), (x - 14, y + 4), (x - 18, y + 10), (x - 8, y + 6), (x + 2, y + 3)], IRON_L)


def hoe(d, x, y):
    d.line([(x, y + 32), (x + 16, y)], fill=WOOD_D, width=3)
    d.line([(x, y + 32), (x + 16, y)], fill=WOOD_L, width=1)
    poly(d, [(x + 12, y - 2), (x + 24, y - 2), (x + 24, y + 3), (x + 14, y + 4)], IRON_L)


def watering_can(d, x, y):
    poly(d, [(x, y + 6), (x + 18, y + 6), (x + 20, y + 22), (x - 2, y + 22)], IRON, IRON_L)
    d.arc([x + 2, y - 4, x + 16, y + 10], 180, 360, fill=INK, width=2)
    poly(d, [(x + 19, y + 10), (x + 30, y + 2), (x + 31, y + 5), (x + 21, y + 14)], IRON_D)
    for k in range(3):
        d.point((x + 30 + k, y + 7 + 2 * k), fill=(120, 170, 230))


def seed_sack(d, x, y, w=18, h=20, seed=GREEN_L):
    poly(d, [(x, y + 6), (x + w, y + 6), (x + w + 2, y + h), (x - 2, y + h)], BURLAP, (226, 200, 150))
    d.rectangle([x + 3, y + 2, x + w - 3, y + 6], fill=BURLAP_D, outline=INK)
    d.line([(x + 2, y + 6), (x + w - 2, y + 6)], fill=WOOD_D, width=2)
    for k in range(5):
        d.point((x + 4 + k * 3, y + 3 + k % 2), fill=seed)


def composter_top(d):
    d.rectangle([12, 12, 51, 51], fill=WOOD_D, outline=INK)
    d.rectangle([16, 16, 47, 47], fill=SOIL, outline=INK)
    for (px, py, c) in ((20, 20, SOIL_D), (30, 24, GREEN_D), (40, 30, SOIL_D), (24, 36, GREEN), (36, 40, SOIL_D),
                         (42, 20, GREEN_L), (18, 42, WHEAT_D), (28, 30, (230, 225, 210))):
        d.rectangle([px, py, px + 3, py + 2], fill=c)
    for k in range(3):
        d.line([(16, 22 + k * 10), (47, 22 + k * 10)], fill=WOOD, width=1)


def carrot(d, x, y):
    poly(d, [(x, y), (x + 6, y), (x + 3, y + 14)], CARROT, CARROT_L)
    d.line([(x + 3, y), (x + 1, y - 5)], fill=GREEN, width=2)
    d.line([(x + 3, y), (x + 6, y - 5)], fill=GREEN_L, width=2)


def potato(d, x, y):
    d.ellipse([x, y, x + 10, y + 8], fill=POTATO, outline=INK)
    d.point((x + 3, y + 3), fill=POTATO_D)
    d.point((x + 7, y + 5), fill=POTATO_D)


def pickaxe(d, x0, y0, x1, y1, head):
    d.line([(x0, y0), (x1, y1)], fill=WOOD_D, width=4)
    d.line([(x0, y0), (x1, y1)], fill=WOOD_L, width=1)
    hx, hy = x1, y1
    poly(d, [(hx - 12, hy + 4), (hx - 4, hy - 4), (hx + 4, hy - 4), (hx + 12, hy + 4), (hx + 4, hy - 1), (hx - 4, hy - 1)],
         head, IRON_L)


def ore_block(d, x, y, fleck, size=16):
    d.rectangle([x, y, x + size, y + size], fill=STONE, outline=INK)
    d.line([(x + 1, y + 1), (x + size - 1, y + 1)], fill=STONE_L)
    for (fx, fy) in ((3, 3), (9, 5), (5, 10), (11, 11)):
        d.rectangle([x + fx, y + fy, x + fx + 2, y + fy + 2], fill=fleck)


def lantern(d, x, y):
    d.rectangle([x + 2, y, x + 8, y + 3], fill=IRON_D, outline=INK)
    d.rectangle([x, y + 3, x + 10, y + 16], fill=IRON, outline=INK)
    d.rectangle([x + 2, y + 5, x + 8, y + 14], fill=ORANGE_L)
    d.rectangle([x + 4, y + 8, x + 6, y + 12], fill=(255, 240, 180))


def shovel(d, x, y):
    d.line([(x, y), (x, y + 26)], fill=WOOD_D, width=3)
    poly(d, [(x - 5, y + 24), (x + 5, y + 24), (x + 4, y + 34), (x, y + 37), (x - 4, y + 34)], IRON_L)


def mine_mouth(d):
    d.rectangle([14, 14, 49, 50], fill=(18, 14, 12), outline=INK)
    d.rectangle([12, 12, 51, 17], fill=WOOD, outline=INK)
    d.rectangle([12, 12, 17, 52], fill=WOOD, outline=INK)
    d.rectangle([46, 12, 51, 52], fill=WOOD, outline=INK)
    for k in range(4):  # trilho entrando na mina
        yy = 44 - k * 6
        d.line([(24 + k, yy), (39 - k, yy)], fill=WOOD_D, width=2)
    d.line([(24, 50), (29, 26)], fill=IRON_L, width=2)
    d.line([(39, 50), (34, 26)], fill=IRON_L, width=2)
    poly(d, [(22, 34), (41, 34), (39, 44), (24, 44)], IRON, IRON_L)  # carrinho
    d.rectangle([25, 31, 38, 35], fill=COAL)
    for cx in (26, 37):
        d.ellipse([cx - 2, 43, cx + 2, 47], fill=IRON_D, outline=INK)


def stone_pile(d):
    for (x, y) in ((12, 34), (28, 34), (44, 34), (20, 22), (36, 22), (28, 10)):
        d.rectangle([x, y, x + 13, y + 11], fill=STONE, outline=INK)
        d.line([(x + 1, y + 1), (x + 12, y + 1)], fill=STONE_L)
        d.point((x + 4, y + 6), fill=STONE_D)
        d.point((x + 9, y + 4), fill=STONE_D)


def iron_grate(d):
    d.rectangle([12, 12, 51, 51], fill=IRON_D, outline=INK)
    for k in range(5):
        x = 16 + k * 8
        d.rectangle([x, 14, x + 3, 49], fill=IRON, outline=INK)
        d.line([(x + 1, 15), (x + 1, 48)], fill=IRON_L)


def main(src, out):
    wood = panel(src, "1", 2, 0)   # textura de tábuas (lenhador)
    stone = panel(src, "3", 2, 1)  # textura de pedra (pedreiro)

    farmer = {}
    for face, draw in {
        "up": lambda d: composter_top(d),
        "north": lambda d: (wheat_sheaf(d, 26, 14), scythe(d, 46, 16)),
        "east": lambda d: (hoe(d, 12, 16), watering_can(d, 30, 26)),
        "west": lambda d: (seed_sack(d, 12, 20), seed_sack(d, 34, 26, seed=WHEAT_L)),
        "south": lambda d: ([carrot(d, 14 + k * 9, 20 + (k % 2) * 4) for k in range(4)],
                            [potato(d, 16 + k * 12, 40) for k in range(3)]),
    }.items():
        im = framed(wood, WOOD_D, (GOLD, GOLD_L))
        draw(ImageDraw.Draw(im))
        farmer[face] = im
    farmer["down"] = framed(wood, WOOD_D, (GOLD, GOLD_L))

    miner = {}
    for face, draw in {
        "up": lambda d: (ore_block(d, 24, 24, ORE_IRON), pickaxe(d, 12, 52, 40, 20, IRON_L),
                         pickaxe(d, 52, 52, 24, 20, (120, 210, 220))),
        "north": lambda d: mine_mouth(d),
        "east": lambda d: (ore_block(d, 12, 14, COAL), ore_block(d, 34, 14, ORE_IRON), ore_block(d, 23, 34, ORE_GOLD)),
        "west": lambda d: (lantern(d, 14, 18), shovel(d, 44, 14)),
        "south": lambda d: stone_pile(d),
        "down": lambda d: iron_grate(d),
    }.items():
        im = framed(stone, IRON_D, (GOLD, GOLD_L))
        draw(ImageDraw.Draw(im))
        miner[face] = im

    for name, faces in (("farmer_bench", farmer), ("miner_bench", miner)):
        for face, im in faces.items():
            im.save(os.path.join(out, f"{name}_{face}.png"))
        print("  ", name)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
