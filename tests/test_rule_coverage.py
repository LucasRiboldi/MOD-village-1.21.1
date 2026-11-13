import importlib.util
import pathlib
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "rule_coverage.py"
SPEC = importlib.util.spec_from_file_location("rule_coverage", SCRIPT)
rule_coverage = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(rule_coverage)

RULES = """| N | Regra | Data | Estado | Onde |
|---|---|---|---|---|
| 10 | **Ativa com estado** (autor) | 08-18 | ✅ feita | `Smelter` |
| 11 | **Antiga** (autor) | 08-18 | ❌ revogada pela Regra 12 | `Smelter` |
| 201 | **Revogada** (autor 11-06, ADR-047). A régua do lote é a do construtor | 11-05 |
| 202 | **Nova regra curta** (autor). Ela substitui o modo antigo de calçar | 11-05 |
"""


class RuleCoverageTest(unittest.TestCase):

    def grades(self, tests):
        with tempfile.TemporaryDirectory() as folder:
            path = pathlib.Path(folder) / "RULES.md"
            path.write_text(RULES, encoding="utf-8")
            rules = rule_coverage.read_rules(path)
        return {row[0]: row[3] for row in rule_coverage.classify(rules, tests)}

    def test_a_revoked_rule_without_state_column_is_out_of_force(self):
        grades = self.grades({})

        self.assertEqual("fora de vigor", grades[201], "a revogada de duas células contou como sem teste")
        self.assertEqual("fora de vigor", grades[11])

    def test_a_short_active_rule_is_not_taken_for_retired(self):
        grades = self.grades({"FooTest": "// Regra 202 e Regra 10"})

        self.assertEqual("cita", grades[202], "a regra ativa que só cita 'substitui' saiu de vigor")
        self.assertEqual("cita", grades[10])

    def test_a_short_active_rule_without_test_has_none(self):
        self.assertEqual("nenhum", self.grades({})[202])


if __name__ == "__main__":
    unittest.main()
