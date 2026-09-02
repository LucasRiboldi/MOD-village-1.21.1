"""Testes de `scripts/graphify_relabel.py`.

`unittest` da biblioteca padrão, e não pytest, de propósito: este é um
projeto Java onde o Python é utilitário de manutenção. Um teste que roda com
`python -m unittest` roda em qualquer máquina que já tenha Python — e o
pytest coleta `TestCase` normalmente, então quem tiver pytest também o roda.

    python -m unittest discover -s tests

O que se testa aqui é a decisão, que é toda das funções puras: quem ganha o
nome, quem fica sem, e como se desempata. A escrita em disco e a regeneração
do relatório são costura em volta de bibliotecas de terceiros, e foram
verificadas de ponta a ponta contra o grafo real — o script reproduziu o
estado nomeado de 3.179 nós.
"""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
_spec = importlib.util.spec_from_file_location(
    "graphify_relabel", ROOT / "scripts" / "graphify_relabel.py"
)
relabel = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(relabel)


def node(node_id: str, community: int, label: str = "", curated: str | None = None) -> dict:
    n = {"id": node_id, "community": community, "label": label or node_id}

    if curated is not None:
        n["community_name"] = curated

    return n


def reference_of(*nodes: dict) -> dict:
    return {"nodes": list(nodes)}


class HubOf(unittest.TestCase):
    """O apelido usado quando o voto não decide."""

    def test_prefers_a_project_type_over_a_longer_external_one(self):
        # A regressão que motivou a correção: pegando só o rótulo mais longo,
        # uma comunidade de VillageDetector e WorkAssignment era apelidada de
        # `net.minecraft.block.entity.ChestBlockEntity`, que não descreve nada
        # do projeto e ainda é o nome de uma classe de terceiros.
        nodes = [
            node("a", 1, "net.minecraft.block.entity.ChestBlockEntity"),
            node("b", 1, "VillageDetector"),
        ]

        self.assertEqual(relabel.hub_of(nodes), "VillageDetector")

    def test_every_external_prefix_is_rejected(self):
        for external in (
            "net.minecraft.util.math.BlockPos",
            "org.junit.jupiter.api.Test",
            "java.util.concurrent.ConcurrentHashMap",
            "javax.annotation.Nullable",
            "com.google.common.collect.ImmutableList",
            "it.unimi.dsi.fastutil.longs.LongOpenHashSet",
        ):
            with self.subTest(external=external):
                nodes = [node("a", 1, external), node("b", 1, "Colony")]

                self.assertEqual(relabel.hub_of(nodes), "Colony")

    def test_prefers_a_type_over_a_method(self):
        nodes = [node("a", 1, ".aVeryLongMethodNameIndeed()"), node("b", 1, "Mine")]

        self.assertEqual(relabel.hub_of(nodes), "Mine")

    def test_falls_back_to_the_longest_when_nothing_belongs_to_the_project(self):
        # Sem candidato do projeto é melhor um nome externo que nome nenhum:
        # o operador ainda reconhece a comunidade pelo que ela toca.
        nodes = [node("a", 1, ".x()"), node("b", 1, "net.minecraft.util.Identifier")]

        self.assertEqual(relabel.hub_of(nodes), "net.minecraft.util.Identifier")

    def test_strips_the_java_suffix_the_leading_dot_and_the_argument_list(self):
        self.assertEqual(relabel.hub_of([node("a", 1, "ColonyPos.java")]), "ColonyPos")
        self.assertEqual(relabel.hub_of([node("a", 1, ".observe()")]), "observe")

    def test_an_empty_community_has_no_hub(self):
        self.assertEqual(relabel.hub_of([]), "")


class Vote(unittest.TestCase):
    """O nome curado que a maioria dos nós da comunidade já carregava."""

    def test_the_majority_name_wins_with_its_count_and_share(self):
        members = {1: [node("a", 1), node("b", 1), node("c", 1), node("d", 1)]}
        reference = reference_of(
            node("a", 9, curated="Colony Model"),
            node("b", 9, curated="Colony Model"),
            node("c", 9, curated="Colony Model"),
            node("d", 9, curated="Mine Digging State"),
        )

        name, count, share = relabel.vote(members, reference)[1]

        self.assertEqual(name, "Colony Model")
        self.assertEqual(count, 3)
        self.assertAlmostEqual(share, 0.75)

    def test_the_share_is_over_the_community_size_not_over_the_votes_cast(self):
        # Metade dos nós é nova e não votou. Se a fração fosse sobre os votos,
        # dois de dois daria 100% e a comunidade seria dada por decidida com
        # base em um quarto dos seus membros.
        members = {1: [node(c, 1) for c in "abcdefgh"]}
        reference = reference_of(
            node("a", 9, curated="Colony Model"), node("b", 9, curated="Colony Model")
        )

        _, count, share = relabel.vote(members, reference)[1]

        self.assertEqual(count, 2)
        self.assertAlmostEqual(share, 0.25)

    def test_a_community_with_no_node_in_the_reference_has_no_name(self):
        # É o caso de código novo: ConversionBoundaryTest não existia no grafo
        # anterior, e a comunidade dele tem de cair no colo do operador.
        members = {1: [node("novo", 1)]}

        self.assertEqual(relabel.vote(members, reference_of())[1], (None, 0, 0.0))

    def test_reference_nodes_without_a_curated_name_do_not_vote(self):
        members = {1: [node("a", 1), node("b", 1)]}
        reference = reference_of(
            node("a", 9, curated="Colony Model"), node("b", 9)  # sem community_name
        )

        name, count, _ = relabel.vote(members, reference)[1]

        self.assertEqual((name, count), ("Colony Model", 1))


class Resolve(unittest.TestCase):
    """Nome final por comunidade, e quem sobrou para a mão."""

    def test_a_clear_majority_keeps_the_curated_name(self):
        members = {1: [node("a", 1), node("b", 1)]}
        ballots = {1: ("Colony Model", 2, 1.0)}

        labels, undecided = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels, {1: "Colony Model"})
        self.assertEqual(undecided, [])

    def test_exactly_at_the_threshold_still_counts_as_decided(self):
        members = {1: [node("a", 1, "Colony"), node("b", 1, "Mine")]}
        ballots = {1: ("Colony Model", 1, 0.5)}

        labels, undecided = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels[1], "Colony Model")
        self.assertEqual(undecided, [])

    def test_below_the_threshold_falls_to_the_hub_and_is_reported(self):
        members = {1: [node("a", 1, "VillageDetector"), node("b", 1, ".run()")]}
        ballots = {1: ("Colony Model", 1, 0.3)}

        labels, undecided = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels[1], "VillageDetector")
        self.assertEqual(undecided, [1])

    def test_when_a_split_makes_two_communities_claim_one_name_the_bigger_keeps_it(self):
        # A comunidade antiga se partiu. A herdeira é a que levou mais nós
        # dela; a outra precisa continuar distinguível num relatório, e ganha
        # o próprio hub entre parênteses.
        members = {
            1: [node("a", 1, "Colony")],
            2: [node("b", 2, "ColonyAbandonment")],
        }
        ballots = {1: ("Colony Model", 9, 0.9), 2: ("Colony Model", 3, 0.6)}

        labels, _ = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels[1], "Colony Model")
        self.assertEqual(labels[2], "Colony Model (ColonyAbandonment)")

    def test_a_qualified_name_never_exceeds_sixty_characters(self):
        members = {
            1: [node("a", 1, "Colony")],
            2: [node("b", 2, "A" * 90)],
        }
        ballots = {1: ("Colony Model", 9, 0.9), 2: ("Colony Model", 3, 0.6)}

        labels, _ = relabel.resolve(members, ballots, {}, 0.5)

        self.assertLessEqual(len(labels[2]), 60)

    def test_an_override_wins_over_the_vote_and_is_not_reported_as_undecided(self):
        members = {1: [node("a", 1, "Colony")]}
        ballots = {1: ("Colony Model", 1, 1.0)}

        labels, undecided = relabel.resolve(members, ballots, {1: "Escolhido à mão"}, 0.5)

        self.assertEqual(labels[1], "Escolhido à mão")
        self.assertEqual(undecided, [])

    def test_an_override_also_rescues_a_community_the_vote_could_not_decide(self):
        members = {1: [node("a", 1, "ConversionBoundaryTest")]}
        ballots = {1: (None, 0, 0.0)}

        labels, undecided = relabel.resolve(members, ballots, {1: "Conversion Boundary Test"}, 0.5)

        self.assertEqual(labels[1], "Conversion Boundary Test")
        self.assertEqual(undecided, [])

    def test_a_community_with_no_usable_hub_falls_back_to_its_number(self):
        # Rótulos vazios são o único caso sem hub nenhum: `.x()` ainda vira
        # "x" depois da limpeza e serviria de apelido.
        members = {7: [{"id": "a", "community": 7, "label": ""}]}
        ballots = {7: (None, 0, 0.0)}

        labels, _ = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels[7], "Community 7")


class ParseOverrides(unittest.TestCase):
    """Chave por id de comunidade ou por id de nó."""

    def setUp(self):
        self.members = {
            3: [node("src_colony_colony", 3), node("src_colony_center", 3)],
            8: [node("src_mine_mine", 8)],
        }

    def test_a_numeric_key_names_that_community(self):
        resolved, unresolved = relabel.parse_overrides({"3": "Colony Model"}, self.members)

        self.assertEqual(resolved, {3: "Colony Model"})
        self.assertEqual(unresolved, [])

    def test_a_node_id_names_the_community_that_contains_it(self):
        # É o modo que sobrevive ao update: o id do nó é determinístico, o da
        # comunidade não.
        resolved, unresolved = relabel.parse_overrides(
            {"src_mine_mine": "Mine Digging State"}, self.members
        )

        self.assertEqual(resolved, {8: "Mine Digging State"})
        self.assertEqual(unresolved, [])

    def test_a_number_for_a_community_that_does_not_exist_is_reported_not_applied(self):
        resolved, unresolved = relabel.parse_overrides({"99": "Fantasma"}, self.members)

        self.assertEqual(resolved, {})
        self.assertEqual(unresolved, ["99"])

    def test_an_unknown_node_id_is_reported_not_applied(self):
        # Silêncio aqui seria o pior defeito possível do arquivo de overrides:
        # o operador acha que nomeou e o relatório sai com o apelido.
        resolved, unresolved = relabel.parse_overrides({"src_sumiu": "Nome"}, self.members)

        self.assertEqual(resolved, {})
        self.assertEqual(unresolved, ["src_sumiu"])


class TokenCost(unittest.TestCase):
    """O custo que o relatório é obrigado a mostrar."""

    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.addCleanup(self._dir.cleanup)
        self.path = Path(self._dir.name) / "cost.json"

    def test_reads_the_accumulated_cost_from_the_file(self):
        # A regressão: fixando zero aqui, o GRAPH_REPORT.md anunciava
        # "0 input · 0 output" para um grafo que custou 1.112.964 tokens de
        # subagente, e reescrevia esse zero a cada regeneração.
        self.path.write_text(
            json.dumps({"total_input_tokens": 1112964, "total_output_tokens": 12}),
            encoding="utf-8",
        )

        self.assertEqual(relabel.token_cost(self.path), {"input": 1112964, "output": 12})

    def test_a_missing_file_means_no_extraction_was_recorded(self):
        self.assertEqual(relabel.token_cost(self.path), {"input": 0, "output": 0})

    def test_a_corrupt_file_does_not_take_the_report_down_with_it(self):
        # Relatório sem custo é ruim; nenhum relatório é pior.
        self.path.write_text("{ isto nao e json", encoding="utf-8")

        self.assertEqual(relabel.token_cost(self.path), {"input": 0, "output": 0})

    def test_missing_keys_read_as_zero_rather_than_exploding(self):
        self.path.write_text(json.dumps({"runs": []}), encoding="utf-8")

        self.assertEqual(relabel.token_cost(self.path), {"input": 0, "output": 0})


class CommunityMembers(unittest.TestCase):
    def test_groups_the_nodes_by_community(self):
        graph = {"nodes": [node("a", 1), node("b", 2), node("c", 1)]}

        members = relabel.community_members(graph)

        self.assertEqual(sorted(members), [1, 2])
        self.assertEqual([n["id"] for n in members[1]], ["a", "c"])


class EndToEnd(unittest.TestCase):
    """Uma partida inteira, do voto ao nome final."""

    def test_the_vote_carries_the_curated_names_and_leaves_the_new_code_for_the_hand(self):
        members = {
            0: [node("a", 0), node("b", 0), node("c", 0)],   # 3/3 curados
            1: [node("d", 1), node("novo1", 1), node("novo2", 1)],  # 1/3, indeciso
            2: [node("novo3", 2, "ConversionBoundaryTest")],  # código novo
        }
        reference = reference_of(
            node("a", 9, curated="Colony Cycle Orchestration"),
            node("b", 9, curated="Colony Cycle Orchestration"),
            node("c", 9, curated="Colony Cycle Orchestration"),
            node("d", 9, curated="Mine Digging State"),
        )

        ballots = relabel.vote(members, reference)
        labels, undecided = relabel.resolve(members, ballots, {}, 0.5)

        self.assertEqual(labels[0], "Colony Cycle Orchestration")
        self.assertEqual(sorted(undecided), [1, 2])
        self.assertEqual(labels[2], "ConversionBoundaryTest")
        self.assertEqual(len(set(labels.values())), 3, "os nomes têm de sair distintos")


if __name__ == "__main__":
    unittest.main()
