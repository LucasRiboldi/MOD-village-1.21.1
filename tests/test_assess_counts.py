"""As contas da régua v2 medem o que dizem medir — AUD-07."""

from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts" / "assess"))

import assess_project  # noqa: E402
import java_metrics  # noqa: E402


class PhysicalLinesTest(unittest.TestCase):
    """C01 conta linhas como o `wc -l` e o gauntlet_checks.py contam."""

    def test_the_final_newline_is_not_an_extra_line(self) -> None:
        self.assertEqual(java_metrics.physical_lines("a\nb\n"), 2)

    def test_a_file_without_final_newline_counts_its_last_line(self) -> None:
        self.assertEqual(java_metrics.physical_lines("a\nb"), 2)

    def test_a_500_line_file_is_not_over_the_ceiling(self) -> None:
        self.assertEqual(java_metrics.physical_lines("x\n" * 500), 500)


class TodoCountsTest(unittest.TestCase):
    """C14 conta o item aberto marcado na linha ou no título da seção."""

    TODO = """# TODO

Prioridade: 🔴 bloqueia · 🟠 importante.

## 🔴 Playtest da 0.3.19

- [ ] medir o lenhador
- [x] já feito
- [ ] medir o construtor

## 🟡 Melhorias

- [ ] 🔴 marcado na própria linha
- [ ] item comum
"""

    def test_items_under_a_red_section_are_critical(self) -> None:
        open_items, critical = assess_project.todo_counts(self.TODO)
        self.assertEqual(open_items, 4)
        self.assertEqual(critical, 3)

    def test_a_red_mark_in_the_preamble_does_not_make_items_critical(self) -> None:
        text = "Prioridade: 🔴 bloqueia\n\n## 🟢 Futuro\n\n- [ ] algo\n"
        self.assertEqual(assess_project.todo_counts(text), (1, 0))

    def test_no_items_no_debt(self) -> None:
        self.assertEqual(assess_project.todo_counts("# TODO\n"), (0, 0))


if __name__ == "__main__":
    unittest.main()
