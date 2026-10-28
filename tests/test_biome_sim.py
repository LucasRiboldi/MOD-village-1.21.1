import importlib.util
import json
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "biome_sim.py"
SPEC = importlib.util.spec_from_file_location("biome_sim", SCRIPT)
biome_sim = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(biome_sim)


class FakeRcon:
    def __init__(self):
        self.commands = []

    def run(self, command):
        self.commands.append(command)
        return ""


def state_line(min_x, min_z, max_x, max_z):
    box = {"minX": min_x, "minZ": min_z, "maxX": max_x, "maxZ": max_z}
    return "[x] [Server thread/INFO] (villagecolony) VC_STATE " + json.dumps({"kind": "state", "box": box}) + "\n"


class ForcedAreaTest(unittest.TestCase):

    def test_the_last_box_of_the_log_wins(self):
        with tempfile.TemporaryDirectory() as folder:
            log = pathlib.Path(folder) / "latest.log"
            log.write_text(state_line(0, 0, 10, 10) + "linha qualquer\n" + state_line(-50, -20, 130, 90),
                           encoding="utf-8")

            self.assertEqual((-50, -20, 130, 90), biome_sim.latest_box(log))

    def test_no_state_means_no_box(self):
        with tempfile.TemporaryDirectory() as folder:
            log = pathlib.Path(folder) / "latest.log"
            log.write_text("nada aqui\n", encoding="utf-8")

            self.assertIsNone(biome_sim.latest_box(log))

    def test_every_piece_fits_the_forceload_limit_and_together_they_cover_the_area(self):
        pieces = biome_sim.forced_pieces(-300, -200, 300, 250)
        covered = set()

        for x0, z0, x1, z1 in pieces:
            chunks = ((x1 >> 4) - (x0 >> 4) + 1) * ((z1 >> 4) - (z0 >> 4) + 1)
            self.assertLessEqual(chunks, biome_sim.MAX_FORCED_CHUNKS)
            covered |= {(cx, cz) for cx in range(x0 >> 4, (x1 >> 4) + 1) for cz in range(z0 >> 4, (z1 >> 4) + 1)}

        wanted = {(cx, cz) for cx in range(-300 >> 4, (300 >> 4) + 1) for cz in range(-200 >> 4, (250 >> 4) + 1)}
        self.assertEqual(wanted, covered)

    def test_a_box_that_grew_past_the_forced_area_is_forced_with_the_margin(self):
        with tempfile.TemporaryDirectory() as folder:
            log = pathlib.Path(folder) / "latest.log"
            log.write_text(state_line(-200, 0, 50, 60), encoding="utf-8")
            rcon = FakeRcon()

            grown = biome_sim.grow_forced(rcon, log, (-96, -96, 96, 96))

        self.assertEqual((-200 - biome_sim.FORCE_MARGIN, -96, 96, 96), grown)
        self.assertTrue(rcon.commands and all(c.startswith("forceload add") for c in rcon.commands))

    def test_a_box_inside_the_forced_area_forces_nothing(self):
        with tempfile.TemporaryDirectory() as folder:
            log = pathlib.Path(folder) / "latest.log"
            log.write_text(state_line(-20, -20, 20, 20), encoding="utf-8")
            rcon = FakeRcon()

            self.assertEqual((-96, -96, 96, 96), biome_sim.grow_forced(rcon, log, (-96, -96, 96, 96)))
            self.assertEqual([], rcon.commands)


if __name__ == "__main__":
    unittest.main()
