# Ícones lisos das profissões: círculo de cor + símbolo branco, 512 px reduzidos a 64.
import sys, math
from PIL import Image, ImageDraw
OUT = sys.argv[1]
N = 512
W = (255, 255, 255, 255)
def base(color):
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.ellipse((16, 16, N-16, N-16), fill=(30, 30, 30, 255))
    d.ellipse((32, 32, N-32, N-32), fill=color)
    return im, d
def line(d, a, b, w): d.line([a, b], fill=W, width=w); [d.ellipse((p[0]-w/2, p[1]-w/2, p[0]+w/2, p[1]+w/2), fill=W) for p in (a, b)]
def builder(d):  # casa
    d.polygon([(256,110),(410,240),(102,240)], fill=W)
    d.rectangle((140,236,372,400), fill=W)
    d.rectangle((228,300,284,400), fill=COLOR[0])
def carpenter(d):  # serrote: lâmina larga, dentes embaixo, cabo à direita
    d.polygon([(110,200),(330,170),(330,290),(110,300)], fill=W)
    for i in range(8):
        x = 112 + i*27
        d.polygon([(x,298),(x+27,296),(x+13,330)], fill=W)
    d.rounded_rectangle((320,150,420,310), radius=30, fill=W)
    d.rounded_rectangle((350,195,392,265), radius=14, fill=COLOR[0])
def farmer(d):  # trigo
    line(d, (256,420), (256,150), 22)
    for k in range(4):
        y = 170 + k*55
        d.ellipse((190,y,250,y+50), fill=W); d.ellipse((262,y,322,y+50), fill=W)
    d.ellipse((228,110,284,170), fill=W)
def lumberjack(d):  # machado
    line(d, (150,420), (320,140), 34)
    d.polygon([(270,110),(410,120),(420,270),(320,240),(300,170)], fill=W)
def mason(d):  # tijolos
    w, h = 300, 70; x0, y0 = 106, 140
    for r in range(3):
        y = y0 + r*(h+16); off = 0 if r % 2 == 0 else 75
        xs = [x0, x0+off] if off else [x0]
        x = x0 + off - (150 if off else 0)
        while x < x0 + w:
            a = max(x, x0); b = min(x + 134, x0 + w)
            if b - a > 20: d.rounded_rectangle((a, y, b, y+h), radius=10, fill=W)
            x += 150
def miner(d):  # picareta
    line(d, (170,400), (330,170), 30)
    d.arc((120,90,420,330), start=200, end=340, fill=W, width=46)
def shepherd(d):  # ovelha
    for cx, cy in [(200,250),(260,220),(320,250),(230,300),(300,300)]:
        d.ellipse((cx-70, cy-60, cx+70, cy+60), fill=W)
    d.ellipse((350,200,430,290), fill=(30,30,30,255))
    for x in (200, 300): d.rectangle((x-12,340,x+12,410), fill=(30,30,30,255))
def smelter(d):  # chama de três línguas, miolo na cor do fundo
    d.ellipse((150,250,362,425), fill=W)
    d.polygon([(150,330),(170,200),(215,265)], fill=W)
    d.polygon([(190,300),(256,95),(322,300)], fill=W)
    d.polygon([(297,265),(342,190),(362,330)], fill=W)
    d.ellipse((205,320,307,410), fill=COLOR[0])
    d.polygon([(215,360),(256,255),(297,360)], fill=COLOR[0])
ICONS = {
    'builder': ((214,140,40,255), builder), 'carpenter': ((176,112,62,255), carpenter),
    'farmer': ((206,170,40,255), farmer), 'lumberjack': ((74,140,64,255), lumberjack),
    'mason': ((120,124,132,255), mason), 'miner': ((70,96,150,255), miner),
    'shepherd': ((150,110,180,255), shepherd), 'smelter': ((200,70,50,255), smelter),
}
COLOR=[None]
for name, (color, draw) in ICONS.items():
    COLOR[0]=color; im, d = base(color); draw(d)
    im.resize((64, 64), Image.LANCZOS).save(f'{OUT}/{name}.png')
print('ok')
