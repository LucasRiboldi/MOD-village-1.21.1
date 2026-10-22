import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parent.parent / "scripts" / "rules_to_readme.py"
SPEC = importlib.util.spec_from_file_location("rules_to_readme", SCRIPT)
rules_to_readme = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(rules_to_readme)

RULES = """# RULES.md

Introdução.

---

## Regras ativas

| # | Enunciado |
|---|---|
| 1 | Colher até os baús encherem |

---

## Regras revogadas

A 25 foi desfeita.

---

## Como ler esta tabela

Não vai para o README.
"""


class RulesToReadmeTest(unittest.TestCase):

    def test_the_block_runs_from_the_active_rules_to_before_how_to_read(self):
        block = rules_to_readme.rules_block(RULES)

        self.assertTrue(block.startswith("### Regras ativas"))
        self.assertIn("| 1 | Colher até os baús encherem |", block)
        self.assertIn("### Regras revogadas", block)
        self.assertNotIn("Como ler", block)
        self.assertFalse(block.endswith("---"), "o separador final do RULES.md não entra")

    def test_only_the_text_between_the_markers_is_replaced(self):
        readme = ("# Village Colony\n\n## Regras do mod\n\nantes\n\n" + rules_to_readme.START
                  + "\nvelho\n" + rules_to_readme.END + "\n\n## Instalação\n")

        rendered = rules_to_readme.render(readme, "### Regras ativas\n\nnovo")

        self.assertIn("antes", rendered)
        self.assertIn("novo", rendered)
        self.assertNotIn("velho", rendered)
        self.assertTrue(rendered.endswith(rules_to_readme.END + "\n\n## Instalação\n"))

    def test_the_readme_of_the_project_is_in_step_with_rules_md(self):
        readme = rules_to_readme.README.read_text(encoding="utf-8")
        rules = rules_to_readme.RULES.read_text(encoding="utf-8")

        self.assertEqual(readme, rules_to_readme.render(readme, rules_to_readme.rules_block(rules)),
                         "README desatualizado: rode python scripts/rules_to_readme.py")


if __name__ == "__main__":
    unittest.main()
