import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "road_map.py"
SPEC = importlib.util.spec_from_file_location("road_map", SCRIPT)
road_map = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(road_map)


class NetworksTest(unittest.TestCase):

    def test_a_one_block_gap_still_links_the_street(self):
        # A rua pontilhada do jogo: caminho, um bloco de grama, caminho.
        marked = {(0, 0): (64, False), (2, 0): (64, False), (3, 0): (65, True)}

        self.assertEqual([3], [len(group) for group in road_map.networks(marked)])

    def test_a_far_piece_is_loose_and_the_biggest_network_comes_first(self):
        marked = {(0, 0): (64, False), (1, 0): (64, False), (2, 0): (64, True), (10, 10): (64, True)}

        groups = road_map.networks(marked)

        self.assertEqual([3, 1], [len(group) for group in groups])

    def test_a_cliff_splits_the_network(self):
        marked = {(0, 0): (64, False), (1, 0): (70, True)}

        self.assertEqual(2, len(road_map.networks(marked)))


class StreetsTest(unittest.TestCase):

    def test_a_new_worn_edge_next_to_a_path_counts_and_old_gravel_does_not(self):
        before = [[(64, "grass_block"), (64, "gravel"), (64, "grass_block")]]
        after = [[(64, "dirt_path"), (64, "gravel"), (64, "coarse_dirt")]]

        marked = road_map.streets(after, before)

        self.assertIn((0, 0), marked)
        self.assertNotIn((1, 0), marked, "o cascalho que já estava lá não é borda de rua")
        self.assertNotIn((2, 0), marked, "a terra grossa longe do caminho não é rua")


if __name__ == "__main__":
    unittest.main()
