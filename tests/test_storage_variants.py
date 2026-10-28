import collections
import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "storage_variants.py"
SPEC = importlib.util.spec_from_file_location("storage_variants", SCRIPT)
storage_variants = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(storage_variants)


def names(structure):
    palette = structure["palette"]

    return collections.Counter(str(palette[int(block["state"])]["Name"]) for block in structure["blocks"])


class StorageVariantsTest(unittest.TestCase):
    """O armazém de cada bioma — Regra 115."""

    def test_the_files_in_the_jar_are_what_the_script_makes(self):
        self.assertEqual(storage_variants.main(["--check"]), 0)

    def test_no_variant_keeps_leaves_trapdoors_or_cobwebs(self):
        for path, mapping in storage_variants.targets():
            with self.subTest(path=path):
                self.assertFalse(storage_variants.REMOVED & set(names(storage_variants.variant(mapping))))

    def test_no_variant_asks_for_deepslate_or_calcite(self):
        for style, mapping in storage_variants.STYLES.items():
            with self.subTest(style=style):
                found = [name for name in names(storage_variants.variant(mapping))
                         if "deepslate" in name or "calcite" in name or "ender_chest" in name]
                self.assertEqual(found, [])

    def test_the_desert_storage_has_no_wood(self):
        found = [name for name in names(storage_variants.variant(storage_variants.STYLES["desert"]))
                 if any(wood in name for wood in ("spruce", "oak", "acacia"))]
        self.assertEqual(found, [])

    def test_stairs_keep_their_facing(self):
        structure = storage_variants.variant(storage_variants.STYLES["plains"])
        stairs = [entry for entry in structure["palette"] if str(entry["Name"]).endswith("_stairs")]

        self.assertTrue(stairs)
        self.assertTrue(all("facing" in entry.get("Properties", {}) for entry in stairs))


if __name__ == "__main__":
    unittest.main()
