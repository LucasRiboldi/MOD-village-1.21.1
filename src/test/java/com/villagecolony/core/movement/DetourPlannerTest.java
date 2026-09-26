package com.villagecolony.core.movement;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os cenários do desvio — ADR-025, fase 2. */
class DetourPlannerTest {

    /** Uma grade: uma regra para o fundo e posições escritas à mão por cima. */
    static final class Grid implements Terrain {

        private final Function<ColonyPos, Cell> background;

        private final Map<ColonyPos, Cell> cells = new HashMap<>();

        Grid(Function<ColonyPos, Cell> background) {
            this.background = background;
        }

        /** Campo aberto: chão de rocha em y = 0, ar de y = 1 para cima. */
        static Grid field() {
            return new Grid(pos -> pos.y() <= 0 ? Cell.ROCK : Cell.OPEN);
        }

        /** Rocha maciça em toda parte. */
        static Grid solid(Cell cell) {
            return new Grid(pos -> cell);
        }

        Grid set(int x, int y, int z, Cell cell) {
            cells.put(new ColonyPos(x, y, z), cell);
            return this;
        }

        /** Abre os pés e a cabeça de uma posição. */
        Grid room(int x, int y, int z) {
            return set(x, y, z, Cell.OPEN).set(x, y + 1, z, Cell.OPEN);
        }

        @Override
        public Cell at(ColonyPos pos) {
            return cells.getOrDefault(pos, background.apply(pos));
        }
    }

    private static final ColonyPos START = new ColonyPos(0, 1, 0);

    private static Optional<DetourPlanner.Detour> planTo(Grid grid, ColonyPos goal) {
        return DetourPlanner.plan(grid, START, goal, goal::equals, Set.of(), false);
    }

    /** As ações de cada passo, lidas contra o terreno como está. */
    private static List<DetourMoves.Actions> actionsOf(Grid grid, DetourPlanner.Detour detour) {
        java.util.ArrayList<DetourMoves.Actions> out = new java.util.ArrayList<>();
        ColonyPos from = START;

        for (ColonyPos step : detour.steps()) {
            out.add(DetourMoves.actions(grid, from, step, Set.of()).orElseThrow());
            from = step;
        }

        return out;
    }

    @Test
    void inAnOpenFieldItJustWalks() {
        DetourPlanner.Detour detour = planTo(Grid.field(), new ColonyPos(5, 1, 0)).orElseThrow();

        assertTrue(detour.reachesGoal());
        assertEquals(5, detour.steps().size());
        assertEquals(5 * DetourMoves.WALK, detour.cost());
        assertEquals(new ColonyPos(5, 1, 0), detour.steps().get(4));
    }

    @Test
    void throughSolidRockItDigsTheBodyOfEveryStep() {
        Grid grid = Grid.solid(Cell.ROCK).room(0, 1, 0);

        DetourPlanner.Detour detour = planTo(grid, new ColonyPos(3, 1, 0)).orElseThrow();

        assertEquals(3, detour.steps().size());
        assertEquals(3 * DetourMoves.WALK + 6 * DetourMoves.DIG, detour.cost());

        List<ColonyPos> dug = actionsOf(grid, detour).get(0).dig();
        assertEquals(List.of(new ColonyPos(1, 1, 0), new ColonyPos(1, 2, 0)), dug);
    }

    /** Um vão fundo no corredor vira ponte, e ninguém desce no buraco. */
    @Test
    void aPitInTheCorridorIsBridged() {
        Grid grid = Grid.solid(Cell.FIRM);

        for (int x = 0; x <= 6; x++) {
            grid.room(x, 1, 0);
        }

        for (int x = 3; x <= 4; x++) {
            grid.set(x, 0, 0, Cell.OPEN).set(x, -1, 0, Cell.OPEN);
        }

        DetourPlanner.Detour detour = planTo(grid, new ColonyPos(6, 1, 0)).orElseThrow();

        assertTrue(detour.steps().stream().allMatch(step -> step.y() == 1), "desceu no buraco");

        List<ColonyPos> placed = actionsOf(grid, detour).stream()
                .flatMap(actions -> actions.place().stream()).toList();

        assertEquals(List.of(new ColonyPos(3, 0, 0), new ColonyPos(4, 0, 0)), placed);
    }

    /** O pedido do autor: rocha encostada em água não se cava — a galeria contorna. */
    @Test
    void rockTouchingWaterIsNeverDug() {
        Grid grid = Grid.solid(Cell.ROCK).room(0, 1, 0).set(2, 3, 0, Cell.FLUID);

        DetourPlanner.Detour detour = planTo(grid, new ColonyPos(4, 1, 0)).orElseThrow();

        assertFalse(detour.steps().contains(new ColonyPos(2, 1, 0)),
                "passou por (2,1,0), o que exige abrir (2,2,0), colado na água");

        ColonyPos from = START;

        for (ColonyPos step : detour.steps()) {
            for (ColonyPos dug : DetourMoves.actions(grid, from, step, Set.of()).orElseThrow().dig()) {
                assertTrue(DetourMoves.sides(dug).stream().noneMatch(side -> grid.at(side) == Cell.FLUID),
                        "cavou " + dug + " encostado em líquido");
            }

            from = step;
        }
    }

    /**
     * Rio de um bloco, sem volta dentro do raio e com chão que não se cava. O
     * desvio atravessa por uma ponte erguida um nível acima da água — o que um
     * jogador faz —, sem nunca pisar no líquido nem entrar nele.
     */
    @Test
    void waterIsNeverStoodOnNorEntered() {
        Grid grid = new Grid(pos -> pos.y() <= 0 ? Cell.FIRM : Cell.OPEN);

        for (int z = -20; z <= 20; z++) {
            grid.set(2, 0, z, Cell.FLUID);
        }

        DetourPlanner.Detour detour = planTo(grid, new ColonyPos(4, 1, 0)).orElseThrow();

        for (ColonyPos step : detour.steps()) {
            assertTrue(grid.at(step) != Cell.FLUID && grid.at(DetourMoves.up(step, 1)) != Cell.FLUID,
                    "entrou na água em " + step);
            assertTrue(grid.at(DetourMoves.up(step, -1)) != Cell.FLUID, "pisou na água em " + step);
        }

        assertTrue(detour.steps().contains(new ColonyPos(2, 2, 0)), "não atravessou por cima");
    }

    /** Líquido também não serve de apoio para pôr bloco: sem ar sobre ele, não há ponte. */
    @Test
    void aRiverUnderALowCeilingCannotBeCrossed() {
        Grid grid = new Grid(pos -> pos.y() <= 0 || pos.y() >= 3 ? Cell.FIRM : Cell.OPEN);

        for (int z = -20; z <= 20; z++) {
            grid.set(2, 0, z, Cell.FLUID);
        }

        assertTrue(planTo(grid, new ColonyPos(4, 1, 0)).isEmpty(), "atravessou o rio");

        DetourPlanner.Detour partial = DetourPlanner.plan(
                grid, START, new ColonyPos(4, 1, 0), new ColonyPos(4, 1, 0)::equals, Set.of(), true)
                .orElseThrow();

        assertFalse(partial.reachesGoal());
        assertTrue(partial.steps().stream().allMatch(step -> step.x() <= 1), "o trecho entrou no rio");
    }

    @Test
    void aCellToKeepIsNeitherDugNorWalkedThrough() {
        Grid grid = Grid.solid(Cell.ROCK).room(0, 1, 0);
        ColonyPos stone = new ColonyPos(2, 1, 0);

        DetourPlanner.Detour detour = DetourPlanner.plan(
                grid, START, new ColonyPos(4, 1, 0), new ColonyPos(4, 1, 0)::equals, Set.of(stone), false)
                .orElseThrow();

        assertFalse(detour.steps().contains(stone));
        assertFalse(detour.steps().contains(new ColonyPos(2, 0, 0)), "a cabeça passou pela pedra");
    }

    /** Contornar um pilar custa dois passos; cavá-lo custaria duas picaretadas. */
    @Test
    void walkingRoundIsCheaperThanDigging() {
        Grid grid = Grid.field().set(2, 1, 0, Cell.ROCK).set(2, 2, 0, Cell.ROCK);

        DetourPlanner.Detour detour = planTo(grid, new ColonyPos(4, 1, 0)).orElseThrow();

        assertFalse(detour.steps().contains(new ColonyPos(2, 1, 0)));
        assertEquals(6 * DetourMoves.WALK, detour.cost());
    }

    @Test
    void aGoalOutsideTheRadiusIsNotReachedButTheWayIsStarted() {
        ColonyPos far = new ColonyPos(DetourPlanner.RADIUS + 4, 1, 0);

        assertTrue(planTo(Grid.field(), far).isEmpty());

        DetourPlanner.Detour partial = DetourPlanner.plan(
                Grid.field(), START, far, far::equals, Set.of(), true).orElseThrow();

        assertFalse(partial.reachesGoal());
        assertEquals(new ColonyPos(DetourPlanner.RADIUS, 1, 0),
                partial.steps().get(partial.steps().size() - 1));
    }

    @Test
    void theSearchStopsAtItsBudget() {
        // Um poço fechado sem saída: toda posição aberta é visitada até o teto.
        Grid grid = Grid.solid(Cell.FIRM);

        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                grid.room(x, 1, z);
            }
        }

        ColonyPos unreachable = new ColonyPos(0, 30, 0);

        assertTrue(planTo(grid, unreachable).isEmpty());

        DetourPlanner.Detour partial = DetourPlanner.plan(
                grid, START, unreachable, unreachable::equals, Set.of(), true)
                .orElse(null);

        assertTrue(partial == null || partial.nodes() <= DetourPlanner.MAX_NODES);
    }

    @Test
    void aStepUpDigsTheHeadroomAboveTheClimber() {
        Grid grid = Grid.solid(Cell.ROCK).room(0, 1, 0);

        DetourMoves.Actions up = DetourMoves.actions(
                grid, START, new ColonyPos(1, 2, 0), Set.of()).orElseThrow();

        assertEquals(List.of(new ColonyPos(0, 3, 0), new ColonyPos(1, 2, 0), new ColonyPos(1, 3, 0)),
                up.dig());
        assertEquals(DetourMoves.ASCEND + 3 * DetourMoves.DIG, up.cost());
    }

    @Test
    void aStepDownClearsTheEdgeAndPlacesTheFloorItLacks() {
        Grid grid = Grid.field().set(1, 0, 0, Cell.OPEN).set(1, -1, 0, Cell.OPEN);

        DetourMoves.Actions down = DetourMoves.actions(
                grid, START, new ColonyPos(1, 0, 0), Set.of()).orElseThrow();

        assertTrue(down.dig().isEmpty());
        assertEquals(List.of(new ColonyPos(1, -1, 0)), down.place());
        assertEquals(DetourMoves.DESCEND + DetourMoves.PLACE, down.cost());
    }

    @Test
    void onlyNeighbouringStepsAreMoves() {
        Grid grid = Grid.field();

        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(2, 1, 0), Set.of()).isEmpty());
        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(1, 3, 0), Set.of()).isEmpty());
        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(1, 1, 1), Set.of()).isEmpty());
        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(1, 1, 0), Set.of()).isPresent());
    }

    @Test
    void rockUnderSandIsNotDug() {
        Grid grid = Grid.solid(Cell.ROCK).set(0, 2, 0, Cell.LOOSE);

        assertFalse(DetourMoves.mayDig(grid, new ColonyPos(0, 1, 0)));
        assertTrue(DetourMoves.mayDig(grid, new ColonyPos(0, 3, 0)));
        assertFalse(DetourMoves.mayDig(grid, new ColonyPos(0, 2, 0)), "areia não é rocha que se cava");
    }

    @Test
    void aPassageIsWalkedButNotBuiltOn() {
        // Tocha no chão da frente: passa-se por ela, mas não se põe bloco no lugar dela.
        Grid grid = Grid.field().set(1, 0, 0, Cell.PASSAGE);

        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(1, 1, 0), Set.of()).isEmpty());

        grid.set(1, 0, 0, Cell.ROCK).set(1, 1, 0, Cell.PASSAGE);
        assertTrue(DetourMoves.actions(grid, START, new ColonyPos(1, 1, 0), Set.of()).orElseThrow()
                .dig().isEmpty());
    }

    // --- contas diretas, 2026-09-25: o PIT mostrou que os cenários não as fixavam ---

    @Test
    void theSixSidesAreTheSixFaces() {
        assertEquals(
                Set.of(new ColonyPos(6, 3, -2), new ColonyPos(4, 3, -2),
                        new ColonyPos(5, 4, -2), new ColonyPos(5, 2, -2),
                        new ColonyPos(5, 3, -1), new ColonyPos(5, 3, -3)),
                Set.copyOf(DetourMoves.sides(new ColonyPos(5, 3, -2))));
    }

    @Test
    void theEstimateCountsTheDominantAxisInSteps() {
        assertEquals(14, DetourPlanner.estimate(new ColonyPos(1, 5, 2), new ColonyPos(4, 1, -2)));
        assertEquals(16, DetourPlanner.estimate(new ColonyPos(3, 2, 7), new ColonyPos(4, 10, 6)));
    }

    @Test
    void theRadiusHoldsOnEveryAxisAndBothSides() {
        ColonyPos start = new ColonyPos(10, 10, 10);
        int r = DetourPlanner.RADIUS;

        assertTrue(DetourPlanner.withinRadius(start, new ColonyPos(10 + r, 10 - r, 10 + r)));
        assertTrue(DetourPlanner.withinRadius(start, new ColonyPos(10 - r, 10 + r, 10 - r)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(11 + r, 10, 10)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(9 - r, 10, 10)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(10, 11 + r, 10)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(10, 9 - r, 10)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(10, 10, 11 + r)));
        assertFalse(DetourPlanner.withinRadius(start, new ColonyPos(10, 10, 9 - r)));
    }

    /** Num campo onde tudo se alcança e nada é o objetivo, a busca para exatamente no teto. */
    @Test
    void anUnreachableGoalSpendsExactlyTheBudget() {
        ColonyPos nowhere = new ColonyPos(0, 1, 40);

        DetourPlanner.Detour partial = DetourPlanner.plan(
                Grid.field(), START, nowhere, at -> false, Set.of(), true).orElseThrow();

        assertEquals(DetourPlanner.MAX_NODES, partial.nodes());
    }
}
