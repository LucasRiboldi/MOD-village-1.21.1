package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A obra: este projeto, neste lugar, neste estado — TASK-032.
 */
class ConstructionProjectTest {

    private static final ResourceId HOUSE = ResourceId.vanilla("village/plains/houses/small_house");

    private static final ResourceId OTHER_HOUSE = new ResourceId("villagecolony", "hut");

    private static final ResourceId PLANKS = ResourceId.vanilla("oak_planks");

    private static final ResourceId COBBLE = ResourceId.vanilla("cobblestone");

    private static final ColonyPos ORIGIN = new ColonyPos(100, 64, 200);

    private ConstructionProject project;

    private static BlueprintBlock block(int x, int y, int z, ResourceId what) {
        return new BlueprintBlock(new ColonyPos(x, y, z), what);
    }

    @BeforeEach
    void setUp() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, COBBLE),
                block(1, 0, 0, COBBLE),
                block(0, 1, 0, PLANKS)));

        project = ConstructionProject.plan(UUID.randomUUID(), blueprint, ORIGIN);
    }

    @Test
    void aNewProjectIsPlannedAndUntouched() {
        assertEquals(ConstructionState.PLANNED, project.state());
        assertEquals(3, project.remainingCount());
        assertFalse(project.isFinished());
    }

    @Test
    void aRepairProjectKeepsBlocksAlreadyStanding() {
        Blueprint blueprint = Blueprint.of(HOUSE, List.of(
                block(0, 0, 0, COBBLE),
                block(1, 0, 0, COBBLE),
                block(0, 1, 0, PLANKS)));

        ConstructionProject repair = ConstructionProject.repair(
                UUID.randomUUID(),
                blueprint,
                ORIGIN,
                new HashSet<>(List.of(ORIGIN)));

        assertEquals(ConstructionState.BUILDING, repair.state());
        assertEquals(2, repair.remainingCount());
        assertFalse(repair.remaining().stream().anyMatch(
                block -> repair.worldPositionOf(block).equals(ORIGIN)));
    }

    @Test
    void theProjectSitsWhereItsOriginIs() {
        BlueprintBlock top = block(0, 1, 0, PLANKS);

        assertEquals(new ColonyPos(100, 65, 200), project.worldPositionOf(top));
    }

    @Test
    void theMaterialListCountsTheWholeHouse() {
        assertEquals(Map.of(COBBLE, 2, PLANKS, 1), project.materials());
    }

    /**
     * A lista sai na ordem da planta — 2026-09-09, o E4.
     *
     * <p>Ela saía de {@code Map.copyOf}, que devolve mapa imutável
     * <b>sem ordem</b> e embaralhado a cada execução da máquina virtual.
     * O {@code LinkedHashMap} que a monta existia para nada.
     *
     * <p>Parece detalhe e não é: {@code CraftingWork} percorre esta
     * lista e para no primeiro material que consegue produzir, então a
     * ordem <b>é</b> a prioridade dele. Sorteada, ela deixou a sessão de
     * 09-09 com treze lotes de escada e zero troncos descascados, com o
     * construtor parado esperando justamente o descascado.
     */
    @Test
    void theShoppingListKeepsTheBlueprintsOrder() {
        // Seis materiais, e não dois: a ordem de {@code Map.copyOf} é
        // sorteada, e com dois ela acertaria metade das vezes. Um teste
        // que passa por acaso em código quebrado não prova nada — com
        // seis, o acaso é uma vez em setecentas e vinte.
        List<ResourceId> planned = List.of(
                ResourceId.vanilla("stripped_oak_log"),
                PLANKS,
                COBBLE,
                ResourceId.vanilla("oak_stairs"),
                ResourceId.vanilla("oak_door"),
                ResourceId.vanilla("glass_pane"));

        List<BlueprintBlock> blocks = new ArrayList<>();

        for (int i = 0; i < planned.size(); i++) {
            blocks.add(block(i, 0, 0, planned.get(i)));
        }

        ConstructionProject ordered = ConstructionProject.plan(
                UUID.randomUUID(), Blueprint.of(HOUSE, blocks), ORIGIN);

        assertEquals(
                planned,
                List.copyOf(ordered.remainingMaterials().keySet()),
                "a ordem da planta se perdeu, e a prioridade do fabricante vira sorteio");
    }

    /**
     * E o primeiro da lista é o material do bloco que vem agora.
     *
     * <p><b>É o contrato de que o fabricante depende</b>, e é por isso
     * que ele não escreve prioridade nenhuma: ele percorre esta lista e
     * para no primeiro que consegue produzir, então basta a ordem chegar
     * inteira para ele atender primeiro o que trava a obra.
     *
     * <p>Fica travado aqui porque é uma ligação silenciosa — nada em
     * {@code ConstructionProject} diz que alguém depende desta ordem, e
     * foi assim que o {@code Map.copyOf} a apagou sem ninguém notar.
     */
    @Test
    void theFirstMaterialIsTheOneTheBuilderNeedsNext() {
        assertEquals(COBBLE, project.nextBlock().orElseThrow().block());
        assertEquals(COBBLE, List.copyOf(project.remainingMaterials().keySet()).get(0));

        project.markPlaced(block(0, 0, 0, COBBLE));
        project.markPlaced(block(1, 0, 0, COBBLE));

        assertEquals(
                PLANKS,
                project.nextBlock().orElseThrow().block(),
                "o bloco que vem mudou e a lista não acompanhou");

        assertEquals(
                PLANKS,
                List.copyOf(project.remainingMaterials().keySet()).get(0),
                "o fabricante deixaria de produzir justamente o que trava a obra");
    }

    /** Pedir de novo o que já está na parede mandaria cortar madeira à toa. */
    @Test
    void whatIsBuiltLeavesTheShoppingList() {
        project.markPlaced(block(0, 0, 0, COBBLE));

        assertEquals(Map.of(COBBLE, 1, PLANKS, 1), project.remainingMaterials());
        assertEquals(2, project.remainingCount());
    }

    @Test
    void placingEverythingFinishesTheProject() {
        for (BlueprintBlock block : project.remaining()) {
            assertTrue(project.markPlaced(block));
        }

        assertTrue(project.isFinished());
        assertTrue(project.nextBlock().isEmpty());
        assertEquals(Map.of(), project.remainingMaterials());
    }

    /** O construtor pode repetir um passo, e o bloco não conta duas vezes. */
    @Test
    void placingTheSameBlockTwiceCountsOnce() {
        BlueprintBlock first = block(0, 0, 0, COBBLE);

        assertTrue(project.markPlaced(first));
        assertFalse(project.markPlaced(first));
        assertEquals(2, project.remainingCount());
    }

    @Test
    void anUnsupportedPieceWaitsForItsSupportWithoutDisappearingFromTheProject() {
        BlueprintBlock first = block(0, 0, 0, COBBLE);
        ColonyPos position = project.worldPositionOf(first);

        project.defer(
                first,
                ConstructionOutcome.skipped(position, SkipReason.UNSUPPORTED),
                "support-before");

        assertEquals(3, project.remainingCount(), "a peça parcial não pode parecer construída");
        assertEquals(1, project.deferredPieces().size());
        assertEquals(
                COBBLE,
                project.nextBlock().orElseThrow().block(),
                "as outras peças apoiadas precisam poder continuar");
        assertEquals(Map.of(COBBLE, 1, PLANKS, 1), project.remainingMaterials(),
                "a peça parcial ainda existe, mas não deve gerar demanda antes de poder voltar");

        ConstructionProject.DeferredPiece deferred = project.deferredPieces().get(0);

        assertFalse(project.retryIfSupportChanged(deferred, "support-before"));
        assertTrue(project.retryIfSupportChanged(deferred, "support-after"));
        assertEquals(COBBLE, project.nextBlock().orElseThrow().block());
        assertTrue(project.deferredPieces().isEmpty());
    }

    // --- estados ---

    @Test
    void aProjectWalksThroughItsStates() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertEquals(ConstructionState.COMPLETED, project.state());
        assertFalse(project.state().isOpen());
    }

    /** Faltar material no meio da obra é normal, e se volta dele. */
    @Test
    void aProjectCanWaitForMaterialAndGoOn() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.WAITING_RESOURCES);
        project.moveTo(ConstructionState.BUILDING);

        assertEquals(ConstructionState.BUILDING, project.state());
    }

    /**
     * Não se volta para o começo.
     *
     * <p>Uma obra que voltasse a PLANNED teria blocos já colocados e um
     * plano que os ignora.
     */
    @Test
    void aProjectNeverGoesBackToPlanned() {
        project.moveTo(ConstructionState.PREPARING);

        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.PLANNED));
    }

    @Test
    void aFinishedProjectIsFinal() {
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);
        project.moveTo(ConstructionState.COMPLETED);

        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.BUILDING));
    }

    @Test
    void buildingCannotStartBeforePreparing() {
        assertThrows(IllegalStateException.class,
                () -> project.moveTo(ConstructionState.BUILDING));
    }

    /**
     * A obra sem um bloco de pé, de uma planta que não é mais o alvo,
     * sai da frente — e quem diz qual é o alvo é a colônia, não uma
     * planta escrita à mão aqui dentro.
     *
     * <p>A pergunta já esteve errada duas vezes pelo mesmo motivo: o
     * alvo foi escrito fixo no código. Em 2026-08-15 a Regra 13 trocou a
     * obra do MVP pela cabana e a colônia ficou presa à casa de planície
     * gravada no save; a correção daquele dia escreveu "o alvo é a
     * cabana", e a Regra 24 tornou isso falso de novo — em planície o
     * alvo voltou a ser a casa do jogo.
     */
    @Test
    void anUntouchedProjectIsSupersededByADifferentTarget() {
        assertTrue(project.isSupersededBy(OTHER_HOUSE));
    }

    @Test
    void aProjectOfTheCurrentTargetStays() {
        assertFalse(project.isSupersededBy(HOUSE));
    }

    /**
     * Com bloco de pé é o contrário: casa pela metade é do jogador, e
     * abandoná-la deixaria um esqueleto no mundo com o lote ocupado.
     */
    @Test
    void aProjectWithABlockStandingIsNeverSuperseded() {
        project.markPlaced(block(0, 0, 0, COBBLE));

        assertFalse(project.isSupersededBy(OTHER_HOUSE));
    }

    /**
     * <b>O canto do quadrado é o E46</b> — 2026-09-16.
     *
     * <p>Este teste não afirma que a conta está certa: afirma que ela
     * <b>discorda</b> de quem acha o lote, e é essa discordância que é o
     * defeito. O {@code BuildSiteScanner} varre em anéis quadrados e
     * aceita tudo com {@code max(|dx|,|dz|) <= radius}; este guarda
     * recusa pela reta. Uma obra no canto passa por um e morre no outro.
     *
     * <p>Fixa os números do playtest de 21:41 para o dia em que o C1 for
     * decidido: com a régua unificada em Chebyshev, o
     * {@code assertTrue} vira {@code assertFalse} e <b>este teste é quem
     * avisa</b> que o comportamento mudou de propósito.
     */
    @Test
    void theCornerOfTheSweptSquareIsOutOfReachByTheStraightLine() {
        ColonyPos centre = new ColonyPos(0, 64, 0);

        // O canto exato do quadrado que a varredura percorre: dentro dela
        // pela régua dos anéis, a 90,5 blocos pela reta.
        ColonyPos corner = new ColonyPos(64, 64, 64);

        assertEquals(64, Math.max(Math.abs(corner.x()), Math.abs(corner.z())));

        assertTrue(ConstructionReach.isOutOfReach(corner, centre, 64));
    }

    /**
     * E a beira reta continua dentro, pelas duas réguas — o contraste que
     * mostra que o problema é a diagonal, e não o raio.
     */
    @Test
    void theEdgeOfTheSweptSquareIsWithinReachByBothRulers() {
        ColonyPos centre = new ColonyPos(0, 64, 0);

        assertFalse(ConstructionReach.isOutOfReach(new ColonyPos(64, 64, 0), centre, 64));
    }

    // --- E46 / C4: quem responde "alcançável" é a rua, não o centro ---

    /**
     * <b>O caso do playtest de 2026-09-16 21:41.</b> A obra está a 77
     * blocos do centro — fora do raio por qualquer régua — e encostada na
     * rua que a vila acabou de calçar. Ela <b>fica</b>.
     *
     * <p>Antes deste conserto morria em trinta segundos, com 628 blocos
     * restantes de 628, duas vezes seguidas.
     */
    @Test
    void theWorkBesideTheRoadStaysEvenFarFromTheCentre() {
        ColonyPos centre = new ColonyPos(2495, 65, -3003);
        ColonyPos work = new ColonyPos(2456, 63, -2936);

        assertTrue(ConstructionReach.isOutOfReach(work, centre, 64),
                "o cenário precisa ser uma obra que a régua antiga largava");

        assertFalse(ConstructionReach.isOutOfReach(work, centre, 64, OptionalInt.of(1)));
    }

    /**
     * <b>E o defeito de 2026-09-15 continua pego</b> — é o ponto de não
     * afrouxar o guarda à toa.
     *
     * <p>Aquela obra ficou longe do centro <b>e</b> longe de qualquer rua,
     * segurando a vaga única por sete minutos e meio. Longe dos dois
     * continua sendo largada.
     */
    @Test
    void theWorkStrandedFarFromAnyRoadIsStillLetGo() {
        ColonyPos centre = new ColonyPos(637, 65, -2871);
        ColonyPos stranded = new ColonyPos(638, 65, -2793);

        assertTrue(ConstructionReach.isOutOfReach(
                stranded, centre, 64, OptionalInt.of(40)));
    }

    /**
     * Sem índice de ruas, o centro volta a valer.
     *
     * <p>Não saber onde estão as ruas não é o mesmo que não haver
     * nenhuma, e a colônia que ainda não varreu o raio decide como antes
     * deste conserto — nem mais frouxa, nem mais dura.
     */
    @Test
    void withoutARoadIndexTheCentreDecidesAsBefore() {
        ColonyPos centre = new ColonyPos(0, 64, 0);

        assertTrue(ConstructionReach.isOutOfReach(
                new ColonyPos(100, 64, 0), centre, 64, OptionalInt.empty()));

        assertFalse(ConstructionReach.isOutOfReach(
                new ColonyPos(30, 64, 0), centre, 64, OptionalInt.empty()));
    }

    /**
     * A borda da folga é inclusiva, e o bloco seguinte já é campo aberto.
     *
     * <p>Fixa {@code BESIDE_THE_ROAD} como contrato: mudá-lo é decisão, e
     * quem a tomar vê este teste cair.
     */
    @Test
    void theEdgeOfTheRoadsideToleranceIsInclusive() {
        ColonyPos centre = new ColonyPos(0, 64, 0);
        ColonyPos far = new ColonyPos(1000, 64, 1000);

        assertFalse(ConstructionReach.isOutOfReach(
                far, centre, 64, OptionalInt.of(ConstructionReach.BESIDE_THE_ROAD)));

        assertTrue(ConstructionReach.isOutOfReach(
                far, centre, 64, OptionalInt.of(ConstructionReach.BESIDE_THE_ROAD + 1)));
    }
}
