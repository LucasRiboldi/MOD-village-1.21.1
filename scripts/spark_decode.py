"""Resume um perfil do Spark baixado: TPS, tempo por tique, picos e os métodos do mod que mais pesam.

Uso:
  curl -s -o build/spark.bin https://spark-usercontent.lucko.me/<id>
  python -I scripts/spark_decode.py build/spark.bin

O <id> é o fim do link spark.lucko.me/<id>. O arquivo vem da internet: rode com -I e só leia. O tempo de cada método
é inclusivo, ou seja, soma o dos métodos que ele chama.
"""
import collections
import struct
import sys


def varint(b, i):
    value = shift = 0
    while True:
        c = b[i]
        i += 1
        value |= (c & 0x7F) << shift
        shift += 7
        if c < 0x80:
            return value, i


def fields(b):
    i = 0
    found = []
    while i < len(b):
        key, i = varint(b, i)
        number, wire = key >> 3, key & 7
        if wire == 0:
            value, i = varint(b, i)
        elif wire == 2:
            size, i = varint(b, i)
            value = b[i:i + size]
            i += size
        elif wire == 1:
            value = b[i:i + 8]
            i += 8
        elif wire == 5:
            value = b[i:i + 4]
            i += 4
        else:
            raise ValueError("wire %d" % wire)
        found.append((number, value))
    return found


def windows(top):
    rows = []
    for number, window in top:
        if number != 7:
            continue
        row = {}
        for inner, value in fields(window):
            if inner == 2:
                for key, raw in fields(value):
                    row[key] = struct.unpack("<d", raw)[0] if isinstance(raw, bytes) and len(raw) == 8 else raw
        rows.append(row)
    return rows


def server_methods(top):
    for number, thread in top:
        if number != 2:
            continue
        parts = fields(thread)
        name = [v for f, v in parts if f == 1][0].decode("utf8", "replace")
        if "Server thread" not in name:
            continue
        info = []
        for f, value in parts:
            if f != 3:
                continue
            cls = method = ""
            total = 0.0
            for inner, raw in fields(value):
                if inner == 3 and isinstance(raw, bytes):
                    cls = raw.decode("utf8", "replace")
                if inner == 4 and isinstance(raw, bytes):
                    method = raw.decode("utf8", "replace")
                if inner == 8:
                    total += sum(struct.unpack("<%dd" % (len(raw) // 8), raw))
            info.append((cls, method, total))
        return info
    return []


def main():
    top = fields(open(sys.argv[1], "rb").read())
    rows = windows(top)
    if rows:
        tps = [r[4] for r in rows if 4 in r]
        mspt = [r[5] for r in rows if 5 in r]
        print("janelas %d, TPS mínimo %.2f, médio %.2f, ms por tique médio %.1f, máximo %.1f"
              % (len(rows), min(tps), sum(tps) / len(tps), sum(mspt) / len(mspt), max(r.get(6, 0) for r in rows)))
        peaks = sorted(rows, key=lambda r: -r.get(6, 0))[:3]
        print("picos (ms, janela):", [(round(r.get(6, 0)), rows.index(r)) for r in peaks])
    info = server_methods(top)
    if info:
        root = max(total for _, _, total in info)
        mod = collections.Counter()
        for cls, method, total in info:
            if "villagecolony" in cls:
                key = cls.split(".")[-1] + "." + method
                mod[key] = max(mod[key], total)
        print("% do Server thread, métodos do mod:")
        for key, total in mod.most_common(20):
            print("%6.2f %s" % (100 * total / root, key))


if __name__ == "__main__":
    main()
