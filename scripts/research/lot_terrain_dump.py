import sys, zlib, io, struct, math, json
import nbtlib
REG = sys.argv[1]; X0,X1,Z0,Z1 = map(int, sys.argv[2:6]); OUT = sys.argv[6]
def chunk(rx, rz, cx, cz):
    path = f"{REG}/r.{rx}.{rz}.mca"
    with open(path,'rb') as f:
        i = (cx & 31) + (cz & 31)*32
        f.seek(i*4); e = f.read(4); off = (e[0]<<16|e[1]<<8|e[2])*4096
        if off == 0: return None
        f.seek(off); ln, comp = struct.unpack('>IB', f.read(5)); data = f.read(ln-1)
    raw = zlib.decompress(data)
    return nbtlib.File.parse(io.BytesIO(raw))
def unpack(longs, bits, n):
    per = 64//bits; mask = (1<<bits)-1; out=[]
    for l in longs:
        v = int(l) & 0xFFFFFFFFFFFFFFFF
        for k in range(per):
            out.append((v>>(k*bits))&mask)
            if len(out)==n: return out
    return out
top = {}
for cx in range(X0>>4, (X1>>4)+1):
    for cz in range(Z0>>4, (Z1>>4)+1):
        c = chunk(cx>>5, cz>>5, cx, cz)
        if c is None: continue
        hm = unpack(c['Heightmaps']['MOTION_BLOCKING_NO_LEAVES'], 9, 256)
        secs = {int(s['Y']): s for s in c['sections'] if 'block_states' in s}
        cache = {}
        for lz in range(16):
            for lx in range(16):
                y = hm[lz*16+lx] - 64 - 1
                sy = y >> 4
                s = secs.get(sy)
                name='?'
                if s is not None:
                    bs = s['block_states']; pal = [str(p['Name']).replace('minecraft:','') for p in bs['palette']]
                    if 'data' not in bs: name = pal[0]
                    else:
                        if sy not in cache:
                            bits = max(4, math.ceil(math.log2(len(pal))))
                            cache[sy] = unpack(bs['data'], bits, 4096)
                        name = pal[cache[sy][((y&15)*16+lz)*16+lx]]
                top[f"{cx*16+lx},{cz*16+lz}"] = [y, name]
json.dump(top, open(OUT,'w'))
print(len(top))
