"""As ferramentas de leitura do save e do Spark, sem depender do jogo."""
import os
import struct
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))

import save_read  # noqa: E402
import spark_decode  # noqa: E402


def named(tag, name, payload):
    raw = name.encode()
    return bytes([tag]) + struct.pack(">H", len(raw)) + raw + payload


def test_parse_nbt_reads_nested_values():
    inner = named(8, "Name", struct.pack(">H", 5) + b"stone") + named(3, "Y", struct.pack(">i", -4))
    items = bytes([3]) + struct.pack(">i", 2) + struct.pack(">ii", 7, 8)
    array = struct.pack(">i", 2) + struct.pack(">ii", 1, 2)
    root = named(10, "", named(10, "block", inner + b"\x00") + named(9, "list", items) + named(11, "ints", array) + b"\x00")

    assert save_read.parse_nbt(root) == {"block": {"Name": "stone", "Y": -4}, "list": [7, 8], "ints": (1, 2)}


def test_road_of_lists_roles_and_line_columns():
    graph = {"nodes": [{"position": (1, 64, 2), "roles": ["ENTRANCE", "ENDPOINT"]}],
             "edges": [{"columns": (1, 64, 2, 2, 65, 2)}]}

    nodes, line = save_read.road_of(graph)

    assert nodes == {(1, 64, 2): ["ENDPOINT", "ENTRANCE"]}
    assert line == {(1, 2): 64, (2, 2): 65}


def test_spark_fields_reads_varint_and_bytes():
    message = bytes([0x08, 0x96, 0x01, 0x12, 0x02]) + b"ok"

    assert spark_decode.fields(message) == [(1, 150), (2, b"ok")]


def test_spark_windows_reads_the_tick_numbers():
    tps = struct.pack("<d", 20.0)
    inner = bytes([(4 << 3) | 1]) + tps
    window = bytes([(2 << 3) | 2, len(inner)]) + inner

    assert spark_decode.windows([(7, window)]) == [{4: 20.0}]
