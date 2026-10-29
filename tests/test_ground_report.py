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

    def test_buildings_are_not_ground(self):
        grid = flat(3, 64)
        grid[1][1] = (75, "oak_planks")

        self.assertEqual(0, ground_report.measure(grid)["degraus"])


if __name__ == "__main__":
    unittest.main()
