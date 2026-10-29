import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "ground_report.py"
SPEC = importlib.util.spec_from_file_location("ground_report", SCRIPT)
ground_report = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ground_report)


def flat(size, y, block="grass_block"):
    return [[(y, block) for _ in range(size)] for _ in range(size)]


class GroundReportTest(unittest.TestCase):

    def test_flat_ground_has_no_steps(self):
        result = ground_report.measure(flat(5, 64))

        self.assertEqual(0, result["degraus"])
        self.assertEqual(40, result["pares"])

    def test_a_cliff_and_a_hole_are_counted(self):
        grid = flat(5, 64)
        grid[2][2] = (60, "dirt")
        grid[0][4] = (70, "stone")

        result = ground_report.measure(grid)

        self.assertEqual(1, result["buracos"])
        self.assertEqual(6, result["maior"])
        self.assertEqual(6, result["degraus"])

    def test_a_wall_is_a_step_of_four_or_more(self):
        grid = flat(4, 64)
        grid[0][3] = (67, "stone")
        grid[3][3] = (68, "stone")

        result = ground_report.measure(grid)

        self.assertEqual(2, result["paredoes"], "só o par com degrau 4, nos dois vizinhos do canto")
        self.assertEqual(4, result["maior"])

    def test_ground_over_a_gap_near_the_top_hangs(self):
        self.assertEqual((True, False), ground_report.hangs(["stone", "air", "air", "dirt", "stone"]))
        self.assertEqual((False, False), ground_report.hangs(["grass_block", "dirt", "stone", "stone"]))
        self.assertEqual((False, True), ground_report.hangs(["oak_leaves", "oak_log", "air", "grass_block", "dirt"]))
        self.assertEqual((False, False), ground_report.hangs(["oak_leaves", "oak_log", "grass_block", "dirt"]))

    def test_a_cave_deep_under_the_top_is_not_hanging(self):
        column = ["grass_block"] + ["stone"] * (ground_report.HANG_DEPTH + 2) + ["air"]

        self.assertEqual((False, False), ground_report.hangs(column))

    def test_buildings_are_not_ground(self):
        grid = flat(3, 64)
        grid[1][1] = (75, "oak_planks")

        self.assertEqual(0, ground_report.measure(grid)["degraus"])


if __name__ == "__main__":
    unittest.main()
