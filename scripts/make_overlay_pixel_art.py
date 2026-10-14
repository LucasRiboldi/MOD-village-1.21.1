# Arte em pixel do overlay: moldura medieval (9 partes) e ícones 16x16 com contorno.
# Uso: python scripts/make_overlay_pixel_art.py <pasta de saída>
# A saída espelha textures/gui/overlays/ (ui/, professions/, construction/).
# Os ícones são desenhados em 16x16 e ampliados por vizinho mais próximo, para o
# pixel ficar nítido no jogo.
import os
import sys

from PIL import Image

OUT = sys.argv[1]

PALETTE = {
    '.': None,
    'k': (27, 20, 16, 255),     # contorno
    'w': (176, 124, 66, 255),   # madeira clara
    'W': (118, 76, 38, 255),    # madeira escura
    'i': (222, 226, 232, 255),  # ferro claro
    'I': (150, 156, 166, 255),  # ferro
    'D': (92, 98, 108, 255),    # ferro escuro
    'y': (238, 204, 92, 255),   # trigo
    'Y': (180, 140, 48, 255),   # trigo escuro
    'g': (96, 168, 72, 255),    # folha
    'G': (56, 112, 48, 255),    # folha escura
    'r': (186, 84, 58, 255),    # tijolo
    'R': (124, 50, 36, 255),    # tijolo escuro
    'm': (214, 202, 178, 255),  # argamassa
    's': (150, 150, 150, 255),  # pedra
    'S': (98, 98, 98, 255),     # pedra escura
    'o': (255, 150, 40, 255),   # brasa
    'f': (255, 222, 80, 255),   # chama
    'c': (244, 240, 230, 255),  # lã
    'C': (200, 194, 180, 255),  # lã sombra
    'h': (170, 60, 44, 255),    # telhado
    'H': (116, 38, 28, 255),    # telhado escuro
    'e': (92, 196, 92, 255),    # verde do certo
    'E': (40, 120, 40, 255),
    'a': (255, 196, 40, 255),   # âmbar do aviso
    'A': (196, 120, 20, 255),
}

ICONS = {
    # Mineiro: picareta de ferro.
    'professions/miner': [
        "................",
        "....iiiiiii.....",
        "...iIIIIIIIi....",
        "..iI...w...Ii...",
        "..I....w....I...",
        "......w.....I...",
        "......w.....D...",
        ".....w..........",
        ".....w..........",
        "....w...........",
        "....w...........",
        "...w............",
        "...w............",
        "..W.............",
        "................",
        "................",
    ],
    # Lenhador: machado.
    'professions/lumberjack': [
        "................",
        "...........w....",
        ".....iiIIDw.....",
        "....iiIIIDw.....",
        "....iIIIDw......",
        "....iIIDDw......",
        ".....IDDw.......",
        "........w.......",
        ".......w........",
        ".......w........",
        "......w.........",
        "......w.........",
        ".....w..........",
        ".....W..........",
        "................",
        "................",
    ],
    # Fazendeiro: feixe de trigo.
    'professions/farmer': [
        "................",
        ".......y........",
        "......yYy.......",
        ".....y.y.y......",
        "....yYyYyYy.....",
        ".....yYyYy......",
        "....y.yYy.y.....",
        ".....yYyYy......",
        "......yYy.......",
        ".......Y........",
        "......YYY.......",
        ".....WwwwW......",
        ".......Y........",
        "......Y.Y.......",
        ".....Y...Y......",
        "................",
    ],
    # Pastor: novelo de lã.
    'professions/shepherd': [
        "................",
        "................",
        ".....cccccc.....",
        "....cCccccCc....",
        "...cCcCCCccCc...",
        "...ccCcccCccc...",
        "..cCcccCcccCcc..",
        "..ccCcccCcccCc..",
        "..cCcCcccCcCcc..",
        "...ccCcccCccc...",
        "...cCccCcccCc...",
        "....cCccccCc....",
        ".....cccccc.....",
        "..........CC....",
        "...........C....",
        "................",
    ],
    # Fundidor: fornalha acesa.
    'professions/smelter': [
        "................",
        "..ssssssssssss..",
        "..sSsssSsssSss..",
        "..ssssssssssss..",
        "..sSSSSSSSSSSs..",
        "..sS........Ss..",
        "..sS..f.....Ss..",
        "..sS.ofo..f.Ss..",
        "..sSoffoofofSs..",
        "..sSooooooooSs..",
        "..sSSSSSSSSSSs..",
        "..ssssssssssss..",
        "..sSsssSsssSss..",
        "..ssssssssssss..",
        "................",
        "................",
    ],
    # Carpinteiro: serrote.
    'professions/carpenter': [
        "................",
        "................",
        "...........WW...",
        "..........WwwW..",
        "..iiiiiiiiWw.W..",
        "..iIIIIIIIWw.W..",
        "..iIIIIIIIWwwW..",
        "..IIIIIIIIIWW...",
        "..D.D.D.D.D.....",
        "................",
        "................",
        "...WWWWWWWWWW...",
        "...wwwwwwwwww...",
        "...WWWWWWWWWW...",
        "................",
        "................",
    ],
    # Pedreiro: parede de tijolos.
    'professions/mason': [
        "................",
        "................",
        "..rrrrmrrrrrmr..",
        "..RRRRmRRRRRmR..",
        "..mmmmmmmmmmmm..",
        "..rrmrrrrrmrrr..",
        "..RRmRRRRRmRRR..",
        "..mmmmmmmmmmmm..",
        "..rrrrmrrrrrmr..",
        "..RRRRmRRRRRmR..",
        "..mmmmmmmmmmmm..",
        "..rrmrrrrrmrrr..",
        "..RRmRRRRRmRRR..",
        "................",
        "................",
        "................",
    ],
    # Construtor: martelo.
    'professions/builder': [
        "................",
        "................",
        "....DIIIIIi.....",
        "...DIIIIIIii....",
        "...DIIIIIIii....",
        "....DDDwDDD.....",
        ".......w........",
        ".......w........",
        ".......w........",
        ".......W........",
        ".......w........",
        ".......w........",
        ".......W........",
        ".......W........",
        "................",
        "................",
    ],
    # Obra em andamento: casa com andaime.
    'construction/building': [
        "................",
        ".......hh.......",
        "......hhHh......",
        ".....hhHHhh.....",
        "....hhH..HHh....",
        "...hhH....HHh...",
        "..HHH......HHH..",
        "...w.ssss.w.....",
        "...wWsSsSWw.....",
        "...w.ssss.w.....",
        "...wWsS.sWw.....",
        "...w.s..s.w.....",
        "...wWsS.sWw.....",
        "..WWWWWWWWWWW...",
        "................",
        "................",
    ],
    # Obra pronta: casa com o certo.
    'construction/completed': [
        "................",
        ".......hh.......",
        "......hhHh......",
        ".....hhHHhh.....",
        "....hhHHHHHh....",
        "...hhHHHHHHHh...",
        "..HHHHHHHHHHHH..",
        "...ssssssssss...",
        "...sSsww.sSss...",
        "...ssswW.sssse..",
        "...sSsww.sSsee..",
        "...sssww.sseE...",
        "...sSswWeeeE....",
        "...ssssseeE.....",
        ".........E......",
        "................",
    ],
    # Obra esperando material: caixa com o aviso.
    'construction/waiting_material': [
        "................",
        "..........aa....",
        ".........aAAa...",
        ".........aAAa...",
        "..WWWWWW.aAAa...",
        "..WwwwwW.aAAa...",
        "..WwWWwW..aa....",
        "..WwwwwW..AA....",
        "..WWWWWW........",
        "..WwwwwW........",
        "..WwWWwW........",
        "..WwwwwW........",
        "..WWWWWW........",
        "................",
        "................",
        "................",
    ],
}


def outlined(grid):
    """Os pixels do desenho e um contorno escuro em volta, nas quatro direções."""
    h, w = len(grid), len(grid[0])
    filled = [[grid[y][x] != '.' for x in range(w)] for y in range(h)]
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            ch = grid[y][x]
            if ch != '.':
                img.putpixel((x, y), PALETTE[ch])
            elif any(0 <= y + dy < h and 0 <= x + dx < w and filled[y + dy][x + dx]
                     for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), PALETTE['k'])
    return img


def save(img, name, size):
    path = os.path.join(OUT, name + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.resize((size, size), Image.NEAREST).save(path)


def panel():
    """A moldura 64x32 com borda de 5 px: contorno, três tábuas de madeira, filete de
    latão; cantoneira de ferro com rebite nos quatro cantos; miolo de couro escuro.

    O jogo desenha a borda em 5 unidades de texto (OverlayDrawing.BORDER_DRAWN): o pixel
    da moldura fica do tamanho do pixel da fonte. As faixas de cima e de baixo só têm
    linhas horizontais e as dos lados só verticais: elas esticam, e linha na direção do
    esticar continua linha.
    """
    W, H, B = 64, 32, 5
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    out = (27, 18, 10, 255)
    wood = [(128, 86, 46, 255), (104, 66, 34, 255), (84, 52, 27, 255)]
    trim = (204, 158, 70, 255)
    inner = (40, 28, 19, 236)
    for y in range(H):
        for x in range(W):
            edge = min(x, y, W - 1 - x, H - 1 - y)
            if edge == 0:
                c = out
            elif edge < B - 1:
                c = wood[edge - 1]
            elif edge == B - 1:
                c = trim
            else:
                c = inner
            img.putpixel((x, y), c)
    iron, iron_hi, iron_lo, rivet = (112, 116, 124, 255), (176, 182, 192, 255), (68, 72, 80, 255), (236, 238, 242, 255)
    for cx, cy in ((0, 0), (W - B, 0), (0, H - B), (W - B, H - B)):
        for dy in range(B):
            for dx in range(B):
                lx = dx if cx == 0 else B - 1 - dx
                ly = dy if cy == 0 else B - 1 - dy
                if lx == 0 or ly == 0:
                    c = out
                elif lx == 1 or ly == 1:
                    c = iron_hi
                elif lx == B - 1 or ly == B - 1:
                    c = iron_lo
                else:
                    c = iron
                img.putpixel((cx + dx, cy + dy), c)
        img.putpixel((cx + 2, cy + 2), rivet)
    return img


def main():
    for name, grid in ICONS.items():
        assert len(grid) == 16 and all(len(row) == 16 for row in grid), name
        save(outlined(grid), name, 64)
    path = os.path.join(OUT, 'ui', 'panel_background.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    panel().save(path)


if __name__ == '__main__':
    main()
