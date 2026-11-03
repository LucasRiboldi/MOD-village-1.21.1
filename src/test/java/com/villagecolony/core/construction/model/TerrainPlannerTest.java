package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerrainPlannerTest {
    private static final WorkArea AREA = new WorkArea(new ColonyPos(-8, 56, -8), new ColonyPos(8, 72, 8));
    private static final WorkArea FOOTPRINT = new WorkArea(new ColonyPos(0, 64, 0), new ColonyPos(1, 64, 0));

    private TerrainPlanner.Column column(int x, int z, int originalY, int finalY) {
        return new TerrainPlanner.Column(new ColonyPos(x, originalY, z), finalY, 2, 2, true, false, 0, 0);
    }

    private TerrainPlan plan(List<TerrainPlanner.Column> columns) {
        var candidate = new TerrainPlanner.Candidate(new ColonyPos(0, 64, 0), FOOTPRINT,
                new ColonyPos(0, 64, 0), null, columns, List.of(), 0);
        return TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)),
                new TerrainPlanner.Request(AREA, new ColonyPos(0, 64, 0)));
    }

    @Test
    void unchangedConnectedGroundIsValidWithoutAuthorizingWrites() {
        var result = plan(List.of(column(0, 0, 64, 64), column(1, 0, 64, 64)));
        assertTrue(result.feasible());
        assertEquals(0, result.changedPositions());
        assertEquals(0, result.earthwork());
        assertEquals("external_access_unverified", result.reason());
    }

    @Test
    void cutAndFillAcceptTwoButRejectThree() {
        assertTrue(plan(List.of(column(0, 0, 66, 64), column(1, 0, 62, 64))).feasible());
        assertEquals("cut_fill_limit", plan(List.of(column(0, 0, 67, 64))).reason());
        assertEquals("cut_fill_limit", plan(List.of(column(0, 0, 61, 64))).reason());
    }

    @Test
    void supportMustHaveTwoLayersAndFillMustBeAnOpenDepression() {
        var weak = new TerrainPlanner.Column(new ColonyPos(0, 64, 0), 64, 1, 2, true, false, 0, 0);
        assertEquals("insufficient_support", plan(List.of(weak)).reason());
        var cave = new TerrainPlanner.Column(new ColonyPos(0, 63, 0), 64, 2, 2, false, false, 0, 0);
        assertEquals("unsafe_fill", plan(List.of(cave)).reason());
    }

    @Test
    void entireSurfaceMustBeConnectedWithoutDiagonalShortcuts() {
        assertEquals("disconnected_surface", plan(List.of(column(0, 0, 64, 64), column(2, 0, 64, 64))).reason());
        assertEquals("disconnected_surface", plan(List.of(column(0, 0, 64, 64), column(1, 1, 64, 64))).reason());
        assertEquals("disconnected_surface", plan(List.of(column(0, 0, 64, 64), column(1, 0, 66, 66))).reason());
    }

    @Test
    void normalStepRequiresHeadroomAndVerifiedPassage() {
        assertTrue(plan(List.of(column(0, 0, 64, 64), column(1, 0, 65, 65))).feasible());
        var low = new TerrainPlanner.Column(new ColonyPos(1, 65, 0), 65, 2, 1, true, false, 0, 0);
        assertEquals("clearance", plan(List.of(column(0, 0, 64, 64), low)).reason());
        var blocked = new TerrainPlanner.Candidate(new ColonyPos(0, 64, 0), FOOTPRINT,
                new ColonyPos(0, 64, 0), null, List.of(column(0, 0, 64, 64), column(1, 0, 65, 65)),
                List.of(new TerrainPlanner.Barrier(new ColonyPos(0, 64, 0), new ColonyPos(1, 65, 0))), 0);
        assertEquals("disconnected_surface", TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(blocked)),
                new TerrainPlanner.Request(AREA, new ColonyPos(0, 64, 0))).reason());
    }

    @Test
    void supportClearanceAndTransitionMustFitAuthorizedVolume() {
        var narrow = new TerrainPlanner.Request(new WorkArea(new ColonyPos(0, 63, 0), new ColonyPos(1, 66, 0)),
                new ColonyPos(0, 64, 0));
        var candidate = new TerrainPlanner.Candidate(new ColonyPos(0, 64, 0), FOOTPRINT,
                new ColonyPos(0, 64, 0), null, List.of(column(0, 0, 64, 64)), List.of(), 0);
        assertEquals("outside_work_area", TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)), narrow).reason());
        assertEquals("transition_limit", plan(List.of(column(0, 0, 64, 64), column(8, 0, 64, 64))).reason());
    }

    @Test
    void protectedColumnsAndMissingEntryAreRejected() {
        var protectedColumn = new TerrainPlanner.Column(new ColonyPos(0, 64, 0), 64, 2, 2, true, true, 0, 0);
        assertEquals("protected_column", plan(List.of(protectedColumn)).reason());
        assertEquals("missing_entry", plan(List.of(column(1, 0, 64, 64))).reason());
    }

    @Test
    void limitsAreExplicitAndImmutable() {
        assertEquals(2, TerrainPlanner.MAX_CUT_FILL);
        assertEquals(2, TerrainPlanner.SUPPORT_LAYERS);
        assertEquals(6, TerrainPlanner.MAX_TRANSITION);
        assertEquals(1, TerrainPlanner.MAX_STEP);
        assertEquals(2, TerrainPlanner.CLEARANCE);
        assertEquals(8, TerrainPlanner.MAX_CANDIDATES);
        var columns = new java.util.ArrayList<>(List.of(column(0, 0, 64, 64)));
        var candidate = new TerrainPlanner.Candidate(new ColonyPos(0, 64, 0), FOOTPRINT,
                new ColonyPos(0, 64, 0), null, columns, List.of(), 0);
        columns.clear();
        assertEquals(1, candidate.columns().size());
        assertThrows(UnsupportedOperationException.class, () -> candidate.columns().clear());
    }

    private TerrainPlanner.Candidate candidate(int x, int originalY, ColonyPos anchor) {
        var origin = new ColonyPos(x, 64, 0);
        return new TerrainPlanner.Candidate(origin, new WorkArea(origin, new ColonyPos(x + 1, 64, 0)),
                origin, anchor, List.of(column(x, 0, originalY, 64), column(x + 1, 0, 64, 64)), List.of(), 0);
    }

    private TerrainPlan choose(List<TerrainPlanner.Candidate> candidates) {
        return TerrainPlanner.plan(new TerrainPlanner.Surface(candidates),
                new TerrainPlanner.Request(AREA, new ColonyPos(0, 64, 0)));
    }

    @Test
    void readOnlyAnchorRequiresCardinalContactAndNormalHeight() {
        var anchor = new ColonyPos(-1, 65, 0);
        var accepted = choose(List.of(candidate(0, 64, anchor)));
        assertTrue(accepted.feasible());
        assertEquals("planned_access_to_anchor", accepted.reason());
        assertEquals(0, accepted.changedPositions());
        assertEquals("incompatible_anchor", choose(List.of(candidate(0, 64, new ColonyPos(-1, 66, 0)))).reason());
        assertEquals("incompatible_anchor", choose(List.of(candidate(0, 64, new ColonyPos(-1, 64, -1)))).reason());
        assertEquals("anchor_write", choose(List.of(candidate(0, 64, new ColonyPos(0, 64, 0)))).reason());
    }

    @Test
    void selectionPrefersFewerChangesBeforeLotScoreAndBreaksTiesDeterministically() {
        assertEquals(4, choose(List.of(candidate(0, 65, null), candidate(4, 64, null))).candidate().origin().x());
        var left = candidate(-2, 64, null);
        var right = candidate(0, 64, null);
        assertEquals(-2, choose(List.of(right, left)).candidate().origin().x());
        assertEquals(-2, choose(List.of(left, right)).candidate().origin().x());
    }

    @Test
    void candidateEnumerationNeverSilentlyTruncatesTheRequest() {
        assertTrue(choose(java.util.Collections.nCopies(8, candidate(0, 64, null))).feasible());
        assertEquals("candidate_budget", choose(java.util.Collections.nCopies(9, candidate(0, 64, null))).reason());
        assertEquals("candidate_budget", choose(List.of()).reason());
    }

    @Test
    void transitionAcceptsExactlySixColumnsAndRejectsSeven() {
        var columns = java.util.stream.IntStream.rangeClosed(0, 7).mapToObj(x -> column(x, 0, 64, 64)).toList();
        assertTrue(plan(columns).feasible());
        assertEquals("transition_limit", plan(java.util.stream.IntStream.rangeClosed(0, 8)
                .mapToObj(x -> column(x, 0, 64, 64)).toList()).reason());
    }

    @Test
    void explicitCorridorExtendsTransitionOriginWithoutGrantingArbitrarySurface() {
        var corridor = java.util.stream.IntStream.rangeClosed(2, 8)
                .mapToObj(x -> new ColonyPos(x, 64, 0)).toList();
        var columns = java.util.stream.IntStream.rangeClosed(0, 8).mapToObj(x -> column(x, 0, 64, 64)).toList();
        var candidate = new TerrainPlanner.Candidate(new ColonyPos(0, 64, 0), FOOTPRINT,
                new ColonyPos(0, 64, 0), null, columns, List.of(), corridor, 8);
        assertTrue(choose(List.of(candidate)).feasible());
        var missing = new TerrainPlanner.Candidate(candidate.origin(), FOOTPRINT, candidate.entry(), null,
                columns, List.of(), List.of(new ColonyPos(8, 64, 1)), 8);
        assertEquals("invalid_corridor", choose(List.of(missing)).reason());
    }

    @Test
    void missingFootprintAndDuplicateColumnNeverPassAsCompletePlans() {
        assertEquals("incomplete_footprint", plan(List.of(column(0, 0, 64, 64))).reason());
        assertEquals("duplicate_column", plan(List.of(column(0, 0, 64, 64), column(0, 0, 64, 64))).reason());
    }

    @Test
    void changedPositionsIncludePlantsAndSupportRepairsAndRespectHistoryBudget() {
        var changed = new TerrainPlanner.Column(new ColonyPos(0, 66, 0), 64, 2, 2, true, false, 3, 1);
        var result = plan(List.of(changed, column(1, 0, 64, 64)));
        assertTrue(result.feasible());
        assertEquals(6, result.changedPositions());
        assertEquals(2, result.earthwork());
        assertEquals(2, result.maximumDelta());
        assertEquals(1, result.plants());
        var oversized = new TerrainPlanner.Column(new ColonyPos(0, 64, 0), 64, 2, 2, true, false, 1025, 0);
        assertEquals("work_budget", plan(List.of(oversized, column(1, 0, 64, 64))).reason());
    }

    @Test
    void plantsReplacedByFillRemainCountedWithoutDuplicatingPositions() {
        var filled = new TerrainPlanner.Column(new ColonyPos(0, 62, 0), 64, 2, 2, true, false, 3, 1, 1);
        var result = plan(List.of(filled, column(1, 0, 64, 64)));
        assertTrue(result.feasible());
        assertEquals(5, result.changedPositions());
        assertEquals(2, result.earthwork());
        assertEquals(1, result.plants());
        assertThrows(IllegalArgumentException.class, () ->
                new TerrainPlanner.Column(new ColonyPos(0, 63, 0), 64, 2, 2, true, false, 0, 2, 2));
        assertThrows(IllegalArgumentException.class, () ->
                new TerrainPlanner.Column(new ColonyPos(0, 62, 0), 64, 2, 2, true, false, 0, 1, 2));
    }

    @Test
    void surfaceCanConnectAlongBothAxesAtNegativeCoordinates() {
        var origin = new ColonyPos(-2, 64, -2);
        var footprint = new WorkArea(origin, new ColonyPos(-1, 64, -1));
        var columns = List.of(column(-2, -2, 64, 64), column(-1, -2, 64, 64),
                column(-2, -1, 64, 64), column(-1, -1, 64, 64));
        var candidate = new TerrainPlanner.Candidate(origin, footprint, origin, new ColonyPos(-2, 64, -3),
                columns, List.of(), 0);
        assertTrue(choose(List.of(candidate)).feasible());
        var reverseBarrier = new TerrainPlanner.Barrier(new ColonyPos(-2, 64, -3), origin);
        var blocked = new TerrainPlanner.Candidate(origin, footprint, origin, candidate.anchor(),
                columns, List.of(reverseBarrier), 0);
        assertEquals("incompatible_anchor", choose(List.of(blocked)).reason());
    }

    @Test
    void changeBudgetIncludesItsExactLimit() {
        var atLimit = new TerrainPlanner.Column(new ColonyPos(0, 64, 0), 64, 2, 2, true, false, 1024, 0);
        var result = plan(List.of(atLimit, column(1, 0, 64, 64)));
        assertTrue(result.feasible());
        assertEquals(1024, result.changedPositions());
    }

    @Test
    void clearanceCannotExtendAboveAuthorizedArea() {
        var candidate = candidate(0, 64, null);
        var exact = new TerrainPlanner.Request(new WorkArea(new ColonyPos(0, 62, 0), new ColonyPos(1, 66, 0)),
                new ColonyPos(0, 64, 0));
        var tooLow = new TerrainPlanner.Request(new WorkArea(new ColonyPos(0, 62, 0), new ColonyPos(1, 65, 0)),
                new ColonyPos(0, 64, 0));
        assertTrue(TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)), exact).feasible());
        assertEquals("outside_work_area", TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)), tooLow).reason());
    }

    @Test
    void workVolumeIncludesWidthHeightAndDepth() {
        var area = new WorkArea(new ColonyPos(0, 62, 0), new ColonyPos(40, 66, 30));
        var request = new TerrainPlanner.Request(area, new ColonyPos(0, 64, 0));
        for (int width : new int[]{32, 33}) {
            var columns = new java.util.ArrayList<TerrainPlanner.Column>();
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < 25; z++) columns.add(column(x, z, 64, 64));
            }
            var origin = new ColonyPos(0, 64, 0);
            var footprint = new WorkArea(origin, new ColonyPos(width - 1, 64, 24));
            var candidate = new TerrainPlanner.Candidate(origin, footprint, origin, null, columns, List.of(), 0);
            var result = TerrainPlanner.plan(new TerrainPlanner.Surface(List.of(candidate)), request);
            if (width == 32) assertTrue(result.feasible());
            else assertEquals("work_budget", result.reason());
        }
    }
}
