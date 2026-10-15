#!/usr/bin/env python
"""Gera texturas 16x16 (pixel-art à mão) para as bancadas das 8 profissões.

Cada bancada tem _top, _front, _side. Inspirado nos mockups do autor: paleta e
motivos (serra, machado, bigorna, lã listrada, planta da casa, forja, trigo,
picareta). Estilo Minecraft: bordas escuras, poucas cores, sombreamento simples.
"""
from PIL import Image
import os

OUT = os.environ.get("OUT_DIR", ".")
S = 16

# ---- paletas (RGBA) ---------------------------------------------------------
def c(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

TRANSPARENT = (0, 0, 0, 0)

# madeiras
OAK_D = c("4a3119"); OAK = c("6b4423"); OAK_L = c("8b5a2b"); OAK_LL = c("a9712f")
DARK_D = c("2e1d0e"); DARK = c("43301a"); DARK_L = c("5a4426")
PLANK = c("b5823d"); PLANK_L = c("c89b55")
# metais
IRON_D = c("4a4a52"); IRON = c("6b6b74"); IRON_L = c("9a9aa2"); IRON_LL = c("c4c4cc")
GOLD_D = c("9a6b12"); GOLD = c("d49a1e"); GOLD_L = c("f0c23a")
# pedra
STONE_D = c("5a5a5e"); STONE = c("858589"); STONE_L = c("aeaeb2")
# fogo / energia
EMBER_D = c("8a1e00"); EMBER = c("d94a10"); EMBER_L = c("f58a1e"); EMBER_LL = c("ffd24a")
# lã / tecido
WOOL = c("e8e0d0"); WOOL_D = c("c9bfa8"); RED = c("a83232"); RED_L = c("c64a4a")
# verde (folha)
LEAF = c("4a7a2a"); LEAF_L = c("6ba03a")
# corda
ROPE = c("b8945a"); ROPE_D = c("8a6e3e")
# papel (planta)
PAPER = c("d8cba0"); PAPER_L = c("e8ddba"); INK = c("6b5a3a")
# trigo
WHEAT = c("d9b84a"); WHEAT_L = c("ecd36a")


def img():
    return Image.new("RGBA", (S, S), TRANSPARENT)


def px(im, x, y, col):
    if 0 <= x < S and 0 <= y < S:
        im.putpixel((x, y), col)


def rect(im, x0, y0, x1, y1, col):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(im, x, y, col)


def border(im, col):
    for i in range(S):
        px(im, i, 0, col); px(im, i, S - 1, col)
        px(im, 0, i, col); px(im, S - 1, i, col)


def plank_bg(im, d, m, l):
    """Fundo de madeira em tábuas horizontais com veios."""
    rows = [(0, 3, m), (4, 7, l), (8, 11, m), (12, 15, l)]
    for y0, y1, base in rows:
        rect(im, 0, y0, S - 1, y1, base)
    # linhas de junção escuras
    for y in (3, 7, 11):
        rect(im, 0, y, S - 1, y, d)
    # veios sutis
    for (x, y) in [(2, 1), (9, 1), (5, 5), (12, 6), (3, 9), (11, 10), (6, 13), (13, 14)]:
        px(im, x, y, d)


def frame(im, cd, cl):
    """Cantoneiras nos 4 cantos + borda, estilo caixa reforçada."""
    border(im, cd)
    # cantos (2x2) claros
    for (cx, cy) in [(1, 1), (S - 3, 1), (1, S - 3), (S - 3, S - 3)]:
        rect(im, cx, cy, cx + 1, cy + 1, cl)


def save(im, name):
    im.save(os.path.join(OUT, name + ".png"))
    print("  ", name)


# ---- LENHADOR (lumberjack): tronco + machado, serragem ----------------------
def lumberjack():
    # TOP: anel de tronco no centro + machado
    t = img(); plank_bg(t, OAK_D, OAK, OAK_L); frame(t, OAK_D, GOLD)
    # anéis do tronco (centro)
    rings = [(7, 7, 8, 8, PLANK_L), (6, 6, 9, 9, PLANK), (5, 5, 10, 10, OAK_L)]
    for x0, y0, x1, y1, col in reversed(rings):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                if x in (x0, x1) or y in (y0, y1):
                    px(t, x, y, col)
    px(t, 7, 7, PLANK_L); px(t, 8, 8, PLANK_L)
    # cabo do machado (diagonal) + lâmina
    for i in range(4):
        px(t, 11 + i, 11 - i, OAK_LL)
    rect(t, 12, 5, 13, 7, IRON_L); px(t, 11, 6, IRON)
    save(t, "lumberjack_bench_top")

    # FRONT: serra circular + machado encostado
    f = img(); plank_bg(f, OAK_D, OAK, OAK_L); frame(f, OAK_D, GOLD)
    # serra circular (disco)
    cx, cy, r = 6, 7, 3
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(f, x, y, IRON)
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            if r * r - 2 <= (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(f, x, y, IRON_LL)  # dentes
    px(f, cx, cy, GOLD)
    # machado à direita
    for i in range(5):
        px(f, 11, 5 + i, OAK_LL)
    rect(f, 10, 4, 12, 5, IRON_L)
    save(f, "lumberjack_bench_front")

    # SIDE: corda enrolada + machado + toras
    s = img(); plank_bg(s, OAK_D, OAK, OAK_L); frame(s, OAK_D, GOLD)
    # corda (laço) à esquerda
    for (x, y) in [(3, 4), (4, 3), (5, 4), (5, 5), (4, 6), (3, 5)]:
        px(s, x, y, ROPE)
    # toras empilhadas embaixo
    rect(s, 2, 12, 13, 13, PLANK); px(s, 3, 12, OAK_L); px(s, 8, 12, OAK_L)
    # machado à direita
    for i in range(5):
        px(s, 10, 4 + i, OAK_LL)
    rect(s, 9, 3, 11, 4, IRON_L)
    save(s, "lumberjack_bench_side")


# ---- FUNDIDOR (smelter / forja): já existe blacksmith_forge — atualiza ------
def smelter():
    # FRONT: boca da fornalha com fogo
    f = img(); rect(f, 0, 0, S - 1, S - 1, STONE); frame(f, STONE_D, GOLD)
    # arco da boca
    rect(f, 4, 6, 11, 13, STONE_D)
    rect(f, 5, 8, 10, 13, EMBER_D)
    rect(f, 5, 10, 10, 13, EMBER)
    for (x, y) in [(6, 9), (8, 8), (9, 10), (7, 11), (6, 12), (9, 12)]:
        px(f, x, y, EMBER_L)
    px(f, 7, 9, EMBER_LL); px(f, 8, 11, EMBER_LL)
    # barras de calor no topo
    rect(f, 4, 3, 11, 3, EMBER)
    save(f, "blacksmith_forge_front")

    # TOP: placa de metal com fendas de brasa
    t = img(); rect(t, 0, 0, S - 1, S - 1, STONE); frame(t, STONE_D, GOLD)
    rect(t, 4, 4, 11, 11, IRON); frame_inner = [(5, 7), (8, 5), (10, 9), (6, 10)]
    rect(t, 6, 6, 9, 9, IRON_D)
    rect(t, 7, 6, 8, 9, EMBER)  # fenda central brilhante
    px(t, 7, 7, EMBER_L); px(t, 8, 8, EMBER_LL)
    save(t, "blacksmith_forge_top")

    # SIDE: parede de pedra+metal com fresta de brasa vertical
    s = img(); rect(s, 0, 0, S - 1, S - 1, STONE); frame(s, STONE_D, GOLD)
    # blocos de pedra
    for (x0, y0) in [(2, 3), (8, 3), (4, 8), (9, 9)]:
        rect(s, x0, y0, x0 + 3, y0 + 2, STONE_L)
        rect(s, x0, y0, x0 + 3, y0, STONE_D)
    # fresta de brasa vertical
    rect(s, 12, 4, 12, 11, EMBER)
    px(s, 12, 6, EMBER_L); px(s, 12, 9, EMBER_LL)
    save(s, "blacksmith_forge_side")


# ---- FAZENDEIRO (farmer): trigo + foice ------------------------------------
def farmer():
    # TOP: feixes de trigo em terra arada
    t = img(); rect(t, 0, 0, S - 1, S - 1, DARK); frame(t, DARK_D, OAK_L)
    # sulcos de terra
    for y in (5, 8, 11):
        rect(t, 2, y, 13, y, DARK_D)
    # espigas de trigo
    for x in (4, 7, 10):
        for i in range(4):
            px(t, x, 4 + i, WHEAT if i % 2 else WHEAT_L)
        px(t, x - 1, 4, WHEAT_L); px(t, x + 1, 4, WHEAT_L)
    save(t, "farmer_bench_top")

    # FRONT: feixe de trigo amarrado + foice
    f = img(); plank_bg(f, DARK_D, DARK, DARK_L); frame(f, DARK_D, OAK_L)
    # feixe de trigo
    for x in range(5, 10):
        for y in range(3, 11):
            if (x + y) % 2 == 0:
                px(f, x, y, WHEAT)
            else:
                px(f, x, y, WHEAT_L)
    rect(f, 5, 7, 9, 7, ROPE_D)  # amarra
    # lâmina da foice (curva) à direita
    for (x, y) in [(11, 4), (12, 5), (12, 6), (11, 7), (10, 8)]:
        px(f, x, y, IRON_L)
    save(f, "farmer_bench_front")

    # SIDE: enxada + cesto
    s = img(); plank_bg(s, DARK_D, DARK, DARK_L); frame(s, DARK_D, OAK_L)
    # cabo da enxada
    for i in range(6):
        px(s, 4 + i, 3 + i, OAK_LL)
    rect(s, 9, 3, 11, 4, IRON_L)  # lâmina
    # cesto embaixo
    rect(s, 3, 11, 9, 13, ROPE)
    for x in range(3, 10, 2):
        rect(s, x, 11, x, 13, ROPE_D)
    save(s, "farmer_bench_side")


# ---- PASTOR (shepherd): lã listrada + tesoura ------------------------------
def shepherd():
    # TOP: lã branca com faixa vermelha + tesoura
    t = img(); plank_bg(t, OAK_D, OAK, OAK_L); frame(t, OAK_D, IRON)
    # manta de lã
    rect(t, 3, 3, 12, 12, WOOL)
    for (x, y) in [(4, 4), (10, 5), (6, 10), (11, 11), (5, 8)]:
        px(t, x, y, WOOL_D)  # textura de lã
    rect(t, 3, 7, 12, 8, RED)  # faixa vermelha
    # tesoura (X)
    for i in range(4):
        px(t, 6 + i, 5 + i, IRON_L); px(t, 9 - i, 5 + i, IRON_L)
    save(t, "shepherd_bench_top")

    # FRONT: lã pendurada + lanterna
    f = img(); plank_bg(f, OAK_D, OAK, OAK_L); frame(f, OAK_D, IRON)
    # lã pendurada (topo)
    rect(f, 7, 2, 13, 6, WOOL)
    rect(f, 7, 4, 13, 4, RED)
    for (x, y) in [(8, 3), (11, 5), (9, 6)]:
        px(f, x, y, WOOL_D)
    # lanterna à esquerda
    rect(f, 3, 4, 5, 7, IRON); rect(f, 3, 5, 5, 6, EMBER_L); px(f, 4, 3, IRON_L)
    # laço de corda
    for (x, y) in [(4, 10), (5, 11), (4, 12), (3, 11)]:
        px(f, x, y, ROPE)
    save(f, "shepherd_bench_front")

    # SIDE: lã crua no balde + corda
    s = img(); plank_bg(s, OAK_D, OAK, OAK_L); frame(s, OAK_D, IRON)
    # balde com lã
    rect(s, 8, 8, 12, 13, OAK_L); rect(s, 8, 8, 12, 8, IRON_L)
    rect(s, 9, 6, 11, 8, WOOL); px(s, 9, 7, WOOL_D)
    # corda laço à esquerda
    for (x, y) in [(4, 5), (5, 4), (6, 5), (5, 6), (4, 6)]:
        px(s, x, y, ROPE)
    save(s, "shepherd_bench_side")


# ---- CONSTRUTOR (builder): planta da casa + nível/esquadro -----------------
def builder():
    # TOP: planta (papel) com desenho de casa + esquadro
    t = img(); plank_bg(t, OAK_D, OAK, OAK_L); frame(t, DARK_D, GOLD)
    rect(t, 3, 3, 11, 11, PAPER)
    rect(t, 3, 3, 11, 3, PAPER_L)
    # casinha na planta
    for (x, y) in [(6, 6), (7, 5), (8, 6)]:
        px(t, x, y, INK)  # telhado
    rect(t, 6, 7, 8, 9, INK)
    px(t, 7, 8, PAPER)  # porta
    # esquadro dourado (L)
    rect(t, 10, 10, 13, 10, GOLD); rect(t, 13, 10, 13, 13, GOLD)
    save(t, "builder_bench_top")

    # FRONT: planta + lanterna + ferramentas
    f = img(); plank_bg(f, OAK_D, OAK, OAK_L); frame(f, DARK_D, GOLD)
    rect(f, 6, 3, 12, 9, PAPER)
    for (x, y) in [(8, 5), (9, 4), (10, 5)]:
        px(f, x, y, INK)
    rect(f, 8, 6, 10, 8, INK); px(f, 9, 7, PAPER)
    # lanterna
    rect(f, 3, 4, 5, 7, IRON); rect(f, 3, 5, 5, 6, EMBER_L)
    # régua dourada embaixo
    rect(f, 3, 12, 11, 12, GOLD)
    for x in range(3, 12, 2):
        px(f, x, 12, GOLD_D)
    save(f, "builder_bench_front")

    # SIDE: martelo + colher de pedreiro + nível
    s = img(); plank_bg(s, OAK_D, OAK, OAK_L); frame(s, DARK_D, GOLD)
    # martelo
    for i in range(5):
        px(s, 4, 5 + i, OAK_LL)
    rect(s, 3, 3, 6, 4, IRON_L)
    # colher de pedreiro (triângulo)
    for (x, y) in [(10, 4), (9, 5), (10, 5), (11, 5), (9, 6), (10, 6), (11, 6), (10, 7)]:
        px(s, x, y, IRON)
    for i in range(3):
        px(s, 10, 8 + i, OAK_LL)  # cabo
    # nível (barra com bolha)
    rect(s, 3, 12, 12, 13, OAK_L); px(s, 7, 12, LEAF_L); px(s, 8, 12, LEAF_L)
    save(s, "builder_bench_side")


# ---- MINER: picareta + minério (sem mockup; coerente com o resto) ----------
def miner():
    # TOP: pedra com veios de minério + picareta
    t = img(); rect(t, 0, 0, S - 1, S - 1, STONE); frame(t, STONE_D, GOLD)
    # blocos de pedra
    for (x0, y0) in [(2, 2), (8, 3), (3, 9), (9, 9)]:
        rect(t, x0, y0, x0 + 3, y0 + 2, STONE_L)
        rect(t, x0, y0, x0 + 3, y0, STONE_D)
    # veios de minério (brasa/ferro)
    for (x, y) in [(5, 5), (11, 6), (6, 11)]:
        px(t, x, y, EMBER_L); px(t, x + 1, y, GOLD_L)
    # picareta diagonal
    for i in range(5):
        px(t, 5 + i, 11 - i, OAK_LL)
    rect(t, 9, 5, 11, 6, IRON_L); px(t, 8, 6, IRON); px(t, 12, 6, IRON)
    save(t, "miner_bench_top")

    # FRONT: picareta cruzada + minério
    f = img(); rect(f, 0, 0, S - 1, S - 1, STONE); frame(f, STONE_D, GOLD)
    # duas picaretas cruzadas
    for i in range(7):
        px(f, 4 + i, 4 + i, OAK_LL); px(f, 11 - i, 4 + i, DARK_L)
    rect(f, 3, 3, 5, 4, IRON_L); rect(f, 10, 3, 12, 4, IRON_L)
    # minério embaixo
    for (x, y) in [(6, 12), (9, 12), (7, 13)]:
        px(f, x, y, EMBER_L); px(f, x, y - 1, GOLD_L)
    save(f, "miner_bench_front")

    # SIDE: lanterna de mina + trilho/pedra
    s = img(); rect(s, 0, 0, S - 1, S - 1, STONE); frame(s, STONE_D, GOLD)
    # lanterna
    rect(s, 4, 4, 6, 8, IRON); rect(s, 4, 5, 6, 7, EMBER_L); px(s, 5, 3, IRON_L)
    # blocos
    for (x0, y0) in [(9, 4), (9, 9), (3, 11)]:
        rect(s, x0, y0, x0 + 3, y0 + 2, STONE_L); rect(s, x0, y0, x0 + 3, y0, STONE_D)
    px(s, 10, 5, GOLD_L); px(s, 11, 10, EMBER_L)
    save(s, "miner_bench_side")


# ---- CARPENTER / MASON: atualiza as existentes com o estilo dos mockups -----
def carpenter():
    # TOP: grade de crafting dourada (anime bench) + serra
    t = img(); plank_bg(t, DARK_D, DARK, DARK_L); frame(t, DARK_D, GOLD)
    # grade 3x3 central
    rect(t, 5, 5, 10, 10, OAK_L)
    for gx in (5, 7, 9):
        for gy in (5, 7, 9):
            px(t, gx, gy, DARK_D); px(t, gx + 1, gy, DARK)
    rect(t, 5, 5, 10, 5, GOLD_L); rect(t, 5, 5, 5, 10, GOLD_L)
    save(t, "carpenter_bench_top")

    # FRONT: serra circular no alto + bancada
    f = img(); plank_bg(f, DARK_D, DARK, DARK_L); frame(f, DARK_D, GOLD)
    cx, cy, r = 8, 5, 2
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                px(f, x, y, IRON_L)
    px(f, cx, cy, GOLD)
    rect(f, 3, 9, 12, 10, OAK_L)  # tampo
    rect(f, 4, 11, 5, 13, DARK)  # perna
    rect(f, 10, 11, 11, 13, DARK)
    save(f, "carpenter_bench_front")

    # SIDE: ferramentas penduradas (martelo, formão)
    s = img(); plank_bg(s, DARK_D, DARK, DARK_L); frame(s, DARK_D, GOLD)
    for i in range(5):
        px(s, 5, 4 + i, OAK_LL)
    rect(s, 4, 3, 6, 4, IRON_L)  # martelo
    rect(s, 9, 3, 10, 9, IRON)   # formão
    px(s, 9, 9, IRON_LL)
    save(s, "carpenter_bench_side")


def mason():
    # mason_cutter tem 4 texturas: top, side, bottom, saw
    # TOP: pedra sintética com fenda de energia (futurista)
    t = img(); rect(t, 0, 0, S - 1, S - 1, IRON_D); frame(t, c("202024"), GOLD)
    rect(t, 3, 3, 12, 12, STONE_L)
    # rachaduras
    for (x, y) in [(5, 4), (6, 5), (7, 6), (9, 8), (10, 10)]:
        px(t, x, y, STONE_D)
    rect(t, 7, 3, 8, 12, EMBER)  # fenda de energia vertical
    px(t, 7, 6, EMBER_LL); px(t, 8, 9, EMBER_L)
    save(t, "mason_cutter_top")

    # SIDE: metal tecnológico com barras de energia
    s = img(); rect(s, 0, 0, S - 1, S - 1, IRON); frame(s, c("202024"), GOLD)
    for x in (4, 7, 10):
        rect(s, x, 4, x, 11, EMBER)
        px(s, x, 6, EMBER_L); px(s, x, 9, EMBER_LL)
    rect(s, 2, 2, 3, 3, GOLD); rect(s, 12, 2, 13, 3, GOLD)
    save(s, "mason_cutter_side")

    # BOTTOM: placa simples de metal
    b = img(); rect(b, 0, 0, S - 1, S - 1, IRON_D); frame(b, c("202024"), IRON)
    rect(b, 4, 4, 11, 11, IRON)
    for (x, y) in [(5, 5), (10, 5), (5, 10), (10, 10)]:
        px(b, x, y, GOLD)
    save(b, "mason_cutter_bottom")

    # SAW: disco de serra (o topo giratório)
    w = img(); rect(w, 0, 0, S - 1, S - 1, IRON); frame(w, c("202024"), GOLD)
    cx, cy, r = 8, 8, 5
    for x in range(S):
        for y in range(S):
            d2 = (x - cx) ** 2 + (y - cy) ** 2
            if d2 <= r * r:
                px(w, x, y, IRON_L if d2 > (r - 2) ** 2 else STONE)
            if r * r - 3 <= d2 <= r * r:
                px(w, x, y, IRON_LL)  # dentes
    px(w, cx, cy, GOLD); px(w, cx - 1, cy, GOLD_L); px(w, cx + 1, cy, GOLD_D)
    save(w, "mason_cutter_saw")


print("Gerando texturas em", OUT)
lumberjack(); smelter(); farmer(); shepherd(); builder(); miner()
carpenter(); mason()
print("pronto")
