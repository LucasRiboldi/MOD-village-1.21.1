"""Testes de `scripts/gauntlet.py`.

Mesma escolha de `test_graphify_relabel.py`: `unittest` da biblioteca
padrão, porque este é um projeto Java onde o Python é utilitário.

    python -m unittest discover -s tests

O que se testa aqui é **a decisão do gate**, que é onde um erro custa
caro: um gate que promove BLOCKED a PASS é pior que não ter gate nenhum,
porque passa a assinar embaixo. A execução dos comandos do Gradle é
costura em volta de `subprocess` e foi verificada de ponta a ponta contra
o projeto real — a bateria inteira, verde e vermelha.
"""

from __future__ import annotations

import importlib
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Dois módulos desde 2026-09-09: `gauntlet_checks` roda as camadas,
# `gauntlet` decide. Importados pelo caminho, e não por pacote, porque
# `scripts/` não é um — é a mesma escolha de `test_graphify_relabel.py`.
sys.path.insert(0, str(ROOT / "scripts"))

layers = importlib.import_module("gauntlet_checks")
gauntlet = importlib.import_module("gauntlet")


class ProbeCase(unittest.TestCase):
    """Quem precisa de um arquivo novo de verdade para afirmar alguma coisa.

    Achado do gauntlet-verifier na iteração 4: teste que percorre os
    arquivos não-rastreados do repositório passa por vacuidade quando a
    árvore está limpa. A sonda é montada aqui e desmontada no fim.
    """

    #: Fora de `build/`, que é ignorado: o git precisa enxergá-la.
    PROBE_DIR = ROOT / "tests" / "_gauntlet_probe"

    def probe(self, body: str) -> str:
        self.PROBE_DIR.mkdir(exist_ok=True)
        self.addCleanup(shutil.rmtree, self.PROBE_DIR, ignore_errors=True)

        (self.PROBE_DIR / "Sonda.java").write_text(body, encoding="utf-8")

        return "tests/_gauntlet_probe/Sonda.java"


class GateTest(unittest.TestCase):
    """O Quality Gate: três estados, e nenhuma promoção silenciosa."""

    def test_everything_passing_is_a_pass(self):
        checks = {"typecheck": {"status": "PASS"}, "unit_tests": {"status": "PASS"}}

        self.assertEqual("PASS", gauntlet.gate(checks, 1, 5))

    def test_one_failure_fails_the_whole_gate(self):
        checks = {"typecheck": {"status": "PASS"}, "unit_tests": {"status": "FAIL"}}

        self.assertEqual("FAIL", gauntlet.gate(checks, 1, 5))

    def test_blocked_never_becomes_pass(self):
        """A regra 9 do pedido, e a que mais tenta se desfazer sozinha."""
        checks = {"typecheck": {"status": "PASS"},
                  "unit_tests": {"status": "BLOCKED"}}

        self.assertEqual("BLOCKED", gauntlet.gate(checks, 1, 5))

    def test_failure_wins_over_blocked(self):
        """Falha conhecida é notícia melhor que ambiente incerto."""
        checks = {"a": {"status": "BLOCKED"}, "b": {"status": "FAIL"}}

        self.assertEqual("FAIL", gauntlet.gate(checks, 1, 5))

    def test_a_layer_that_did_not_run_is_not_a_pass(self):
        """Sem `--deep` a bateria de gametest não roda, e sem ela não há PASS."""
        checks = {"unit_tests": {"status": "PASS"},
                  "integration_tests": {"status": "NOT_RUN"}}

        self.assertEqual("BLOCKED", gauntlet.gate(checks, 1, 5))

    def test_not_applicable_does_not_block(self):
        """Lint que não existe neste projeto não é impedimento."""
        checks = {"lint": {"status": "NOT_APPLICABLE"},
                  "unit_tests": {"status": "PASS"}}

        self.assertEqual("PASS", gauntlet.gate(checks, 1, 5))

    def test_the_iteration_limit_blocks_even_with_everything_green(self):
        """O limite é teto do laço, e não sugestão."""
        checks = {"unit_tests": {"status": "PASS"}}

        self.assertEqual("BLOCKED", gauntlet.gate(checks, 6, 5))
        self.assertEqual("PASS", gauntlet.gate(checks, 5, 5))


class ReportLayerTest(unittest.TestCase):
    """A leitura do XML, que é de onde vem a contagem de testes."""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.path = Path(self.dir.name) / "report.xml"
        self.addCleanup(self.dir.cleanup)

    def write(self, xml: str) -> Path:
        self.path.write_text(xml, encoding="utf-8")

        return self.path

    def test_counts_come_from_the_xml_and_not_from_stdout(self):
        report = self.write(
            '<testsuite><testcase classname="a" name="um"/>'
            '<testcase classname="a" name="dois"/></testsuite>')

        totals = layers.junit_totals([report])

        self.assertEqual(2, totals["total"])
        self.assertEqual(0, totals["failed"])

    def test_a_failure_is_named(self):
        report = self.write(
            '<testsuite><testcase classname="a" name="quebrado">'
            '<failure message="x"/></testcase></testsuite>')

        totals = layers.junit_totals([report])

        self.assertEqual(1, totals["failed"])
        self.assertEqual(["a.quebrado"], totals["failures"])

    def test_nested_suites_are_read(self):
        """O relatório do gametest aninha `testsuite` dentro de `testsuite`."""
        report = self.write(
            '<testsuite><testsuite><testcase classname="g" name="um"/>'
            '</testsuite></testsuite>')

        self.assertEqual(1, layers.junit_totals([report])["total"])

    def test_a_report_older_than_the_code_is_blocked(self):
        """Tarefa UP-TO-DATE não reescreve o XML, e o verde seria de ontem."""
        report = self.write('<testsuite><testcase classname="a" name="um"/></testsuite>')

        layer = layers.report_layer(
            {"exit_code": 0}, [report], report.stat().st_mtime + 60)

        self.assertEqual("BLOCKED", layer["status"])
        self.assertIn("mais antigo", layer["reason"])

    def test_a_fresh_report_with_exit_zero_passes(self):
        report = self.write('<testsuite><testcase classname="a" name="um"/></testsuite>')

        layer = layers.report_layer({"exit_code": 0}, [report], 0.0)

        self.assertEqual("PASS", layer["status"])

    def test_exit_zero_with_a_failed_case_is_still_a_failure(self):
        """Se o relatório acusa e o comando não, ganha o relatório."""
        report = self.write(
            '<testsuite><testcase classname="a" name="x"><error/></testcase></testsuite>')

        layer = layers.report_layer({"exit_code": 0}, [report], 0.0)

        self.assertEqual("FAIL", layer["status"])

    def test_an_empty_report_is_not_a_green_battery(self):
        """Achado do gauntlet-verifier, iteração 1: zero caso não é PASS."""
        report = self.write("<testsuite></testsuite>")

        layer = layers.report_layer({"exit_code": 0}, [report], 0.0)

        self.assertEqual("BLOCKED", layer["status"])
        self.assertIn("nada foi executado", layer["reason"])

    def test_no_report_at_all_is_blocked(self):
        layer = layers.report_layer({"exit_code": 0}, [self.path], 0.0)

        self.assertEqual("BLOCKED", layer["status"])


class SourceMtimeTest(unittest.TestCase):
    """Qual alteração invalida qual bateria."""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)

    def test_a_python_only_change_does_not_stale_the_java_report(self):
        """Falso bloqueio é ruído, e ruído ensina a ignorar o gate."""
        self.assertEqual(0.0, layers.newest_source_mtime(["scripts/gauntlet.py"]))

    def test_a_java_change_counts(self):
        java = "src/main/java/com/villagecolony/fabric/work/MinerWork.java"

        self.assertGreater(layers.newest_source_mtime([java]), 0.0)

    def test_a_resource_under_src_counts_even_without_a_java_suffix(self):
        """A bateria de gametest lê estrutura e catálogo de `src/`."""
        found = [f for f in layers.git(["ls-files", "src/main/resources"]).split()
                 if f.endswith(".json")]

        if not found:
            self.skipTest("o projeto não tem recurso .json em src/main/resources")

        self.assertGreater(layers.newest_source_mtime([found[0]]), 0.0)

    def test_a_directory_entry_is_skipped_and_does_not_explode(self):
        """`git status` lista diretório novo, e diretório não tem mtime útil."""
        self.assertEqual(0.0, layers.newest_source_mtime([".claude/agents/"]))


class ChangedFilesTest(ProbeCase):
    """O que o `git status` mostra, e o que ele esconde."""

    def test_a_new_file_inside_a_new_directory_is_listed(self):
        """Achado do gauntlet-verifier, iteração 1.

        Sem `--untracked-files=all` o git colapsa o diretório novo no
        nome da pasta, e o `.java` de dentro nunca é visto — a guarda de
        relatório velho é pulada e um XML de ontem passa por bateria de
        hoje.

        <b>Com sonda criada aqui</b> — iteração 4 — porque a versão
        anterior só afirmava que nenhum caminho terminava em barra, o que
        é verdade de graça em árvore limpa.
        """
        probe = self.probe("class Sonda {}\n")

        listed = layers.changed_files("HEAD")

        self.assertIn(probe, listed)
        self.assertEqual([], [f for f in listed if f.endswith("/")])

    def test_the_probe_also_moves_the_java_clock(self):
        """E um `.java` novo invalida o relatório de Java, como deve."""
        probe = self.probe("class Sonda {}\n")

        self.assertGreater(layers.newest_source_mtime([probe]), 0.0)

    def test_a_rename_is_listed_by_its_new_name(self):
        self.assertEqual("src/novo/Nome.java",
                         layers.parse_status_line("R  src/velho/Nome.java -> src/novo/Nome.java"))

    def test_a_path_with_spaces_loses_the_quotes(self):
        self.assertEqual("docs/um dois.md",
                         layers.parse_status_line('?? "docs/um dois.md"'))

    def test_a_plain_modification_is_the_path_itself(self):
        self.assertEqual("scripts/gauntlet.py",
                         layers.parse_status_line(" M scripts/gauntlet.py"))

    def test_a_line_too_short_to_carry_a_path_is_ignored(self):
        self.assertIsNone(layers.parse_status_line("?? "))
        self.assertIsNone(layers.parse_status_line(""))


class SecurityLayerTest(unittest.TestCase):
    """Ela não aprova: diz se alguém precisa olhar."""

    def test_an_untouched_area_is_not_applicable(self):
        layer = layers.security_layer(["src/main/java/.../MinerReport.java"])

        self.assertEqual("NOT_APPLICABLE", layer["status"])

    def test_touching_persistence_asks_for_a_human_sentence(self):
        layer = layers.security_layer(["src/main/java/com/villagecolony/data/save/MineSave.java"])

        self.assertEqual("REVIEW_REQUIRED", layer["status"])

    def test_review_required_is_not_a_pass(self):
        """E por isso ela bloqueia o gate em vez de passar batido."""
        checks = {"security": layers.security_layer(["data/save/X.java"])}

        self.assertEqual("BLOCKED", gauntlet.gate(checks, 1, 5))

    def test_the_verifier_promotes_it_only_by_writing_what_it_checked(self):
        """A única promoção do laço, e ela deixa rastro."""
        layer = layers.security_layer(
            ["data/save/X.java"], "li o round-trip do NBT e a versão de forma")

        self.assertEqual("PASS", layer["status"])
        self.assertIn("round-trip", layer["reviewed_by_verifier"])
        self.assertEqual("PASS", gauntlet.gate({"security": layer}, 1, 5))

    def test_an_empty_sentence_promotes_nothing(self):
        layer = layers.security_layer(["data/save/X.java"], "   ")

        self.assertEqual("REVIEW_REQUIRED", layer["status"])


class UntrackedBlindnessTest(ProbeCase):
    """O failures[2] da iteração 1, escrito como teste.

    `git diff` não contém arquivo não-rastreado, e a camada `scope`
    varria só o diff: um `@Disabled` dentro de arquivo NOVO — que é o que
    toda entrega nova mais produz — não era lido por ninguém, e a camada
    saía PASS com achado nenhum.
    """

    def test_a_disabled_annotation_only_counts_inside_java(self):
        """A anotação citada em prosa não é anotação.

        A primeira versão acusava a fonte do próprio mecanismo com
        severidade `critical` — a única que bloqueia. Um portão que
        reprova a si mesmo por falar sobre o que procura não sobrevive.
        """
        java = layers.marker_findings([("A.java", "    @Disabled")])
        prosa = layers.marker_findings([("guia.md", "| `@Disabled`, `@Ignore` |")])
        script = layers.marker_findings([("x.py", '"@Disabled/@Ignore — desligar"')])

        self.assertEqual("critical", java[0]["severity"])
        self.assertEqual([], prosa)
        self.assertEqual([], script)

    def test_the_diff_line_knows_which_file_it_came_from(self):
        diff = "\n".join([
            "--- a/A.java",
            "+++ b/src/A.java",
            "+    @Disabled",
            "--- a/b.md",
            "+++ b/docs/b.md",
            "+ fala de @Disabled",
        ])

        added = layers.diff_added(diff)

        self.assertEqual([("src/A.java", "    @Disabled"),
                          ("docs/b.md", " fala de @Disabled")], added)

        found = layers.marker_findings(added)

        self.assertEqual(1, len(found), found)
        self.assertIn("src/A.java", found[0]["evidence"])

    def test_a_disabled_test_in_a_brand_new_file_is_caught(self):
        found = layers.marker_findings([
            ("src/test/java/Novo.java", "    @Disabled"),
        ])

        self.assertEqual(1, len(found))
        self.assertEqual("critical", found[0]["severity"])
        self.assertIn("Novo.java", found[0]["evidence"])

    def test_the_finding_says_which_file_it_came_from(self):
        found = layers.marker_findings([("scripts/novo.py", "# TODO: depois")])

        self.assertIn("scripts/novo.py", found[0]["evidence"])

    def test_a_line_outside_any_file_header_keeps_a_neutral_origin(self):
        found = layers.marker_findings([("diff", "// TODO: depois")])

        self.assertTrue(found[0]["evidence"].startswith("diff:"))

    def test_a_java_file_in_a_brand_new_directory_reaches_the_scan(self):
        """A sonda é criada, e não esperada.

        <b>Achado do gauntlet-verifier na iteração 4.</b> A primeira
        versão deste teste percorria os arquivos não-rastreados que o
        repositório tivesse: em árvore limpa a lista é vazia, o laço não
        roda, e o teste passa sem afirmar nada. Um teste que vira vácuo
        quando o repositório está arrumado é pior que ausente, porque
        conta como cobertura.

        Aqui o caso é montado — diretório novo, que é justamente o que o
        `git status` padrão colapsa — e desmontado no fim.
        """
        probe = self.probe("class Sonda {\n    @Disabled\n    void x() {}\n}\n")

        self.assertIn(probe, layers.untracked_files())

        lines = layers.scanned_lines([])

        self.assertIn(probe, {origin for origin, _ in lines})

        critical = [f for f in layers.marker_findings(lines)
                    if f["severity"] == "critical" and probe in f["evidence"]]

        self.assertEqual(1, len(critical),
                         "o @Disabled do arquivo novo não foi visto")

    def test_the_scope_layer_fails_on_that_probe(self):
        """E a camada inteira reprova, e não só a função de dentro."""
        self.probe("class Sonda {\n    @Disabled\n    void x() {}\n}\n")

        self.assertEqual("FAIL", layers.scope_layer("HEAD")["status"])


class MentionIsNotAPendingMarkerTest(unittest.TestCase):
    """A expressão apertada em 2026-09-09.

    A versão larga acusava a própria fonte do mecanismo: estes arquivos
    citam os marcadores para descrevê-los, e achado de mentira ensina a
    ignorar o relatório.
    """

    def test_a_real_marker_is_still_caught(self):
        for line in ("// TODO: arrumar", "// TODO arrumar", "# FIXME urgente"):
            self.assertTrue(layers.UNFINISHED.search(line), line)

    def test_a_marker_inside_a_regex_alternation_is_a_mention(self):
        self.assertIsNone(
            layers.UNFINISHED.search('re.compile(r"(TODO|FIXME|XXX|HACK)")'))

    def test_a_marker_inside_a_string_literal_is_a_mention(self):
        self.assertIsNone(
            layers.UNFINISHED.search('for marker in ("TODO", "FIXME"):'))

    def test_a_marker_inside_backticks_is_a_mention(self):
        self.assertIsNone(layers.UNFINISHED.search("| `TODO`, `FIXME`, mock |"))

    def test_the_mechanism_does_not_accuse_its_own_sources(self):
        """O caso que motivou o aperto, medido nos arquivos de verdade."""
        # `tests/test_gauntlet.py` fica de fora, e não por conveniência:
        # um teste que prova o detector **tem** de conter o que ele
        # detecta. Nas execuções de verdade ele aparece com três achados
        # `medium`, e eles estão certos — a linha realmente traz o
        # marcador. `medium` não bloqueia; o que não podia acontecer era
        # o `critical` da anotação, e esse a regra de "só em .java"
        # resolveu.
        for name in ("scripts/gauntlet.py", "scripts/gauntlet_checks.py",
                     ".claude/agents/gauntlet-verifier.md",
                     ".claude/commands/gauntlet.md"):
            path = ROOT / name

            if not path.exists():
                continue

            lines = path.read_text(encoding="utf-8").splitlines()
            found = layers.marker_findings([(name, line) for line in lines])

            self.assertEqual([], found, name + " acusou a si mesmo: " + str(found))


class ScopeFindingsTest(unittest.TestCase):
    """Os padrões que o diff traz sem ser pedido."""

    def test_a_disabled_test_is_caught(self):
        self.assertTrue(layers.DISABLED.search("    @Disabled(\"depois\")"))
        self.assertTrue(layers.DISABLED.search("@Ignore"))

    def test_a_word_containing_disabled_is_not_the_annotation(self):
        self.assertIsNone(layers.DISABLED.search("boolean disabled = false;"))

    def test_pending_work_markers_are_caught(self):
        for marker in ("TODO", "FIXME", "XXX", "HACK"):
            self.assertTrue(layers.UNFINISHED.search(f"// {marker}: depois"), marker)

    def test_a_word_that_merely_contains_a_marker_is_left_alone(self):
        self.assertIsNone(layers.UNFINISHED.search("int todos = 3;"))


if __name__ == "__main__":
    unittest.main()
