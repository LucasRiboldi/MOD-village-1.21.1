package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.Side;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O traçado do caminho — Regra 123, rente ao chão: degrau de um, aterro só de buraco de um, corte de um, rede. */
class VillagePathPlannerTest {

    /** Terreno de mentira: altura por coluna, colunas proibidas e água; tudo fora de |x|,|z| ≤ 40 é proibido. */
    private static final class Ground implements PathTerrain {

        private final IntBinaryOperator height;
        private final Set<Long> blocked = new HashSet<>();
        private final Set<Long> water = new HashSet<>();

        Ground(IntBinaryOperator height) {
            this.height = height;
        }

        Ground block(int x, int z) {
            blocked.add(key(x, z));
            return this;
        }

        Ground water(int x, int z) {
            water.add(key(x, z));
            return this;
        }

        @Override
        public OptionalInt groundY(int x, int z, int nearY) {
            return water.contains(key(x, z)) ? OptionalInt.empty() : OptionalInt.of(height.applyAsInt(x, z));
        }

        @Override
        public boolean blocked(int x, int z) {
            return Math.abs(x) > 40 || Math.abs(z) > 40 || blocked.contains(key(x, z));
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static VillagePathPlanner.Goal to(int gx, int gz) {
        return new VillagePathPlanner.Goal() {
            @Override
            public boolean reached(int x, int floorY, int z) {
                return x == gx && z == gz;
            }

            @Override
            public int estimate(int x, int z) {
                return Math.abs(x - gx) + Math.abs(z - gz);
            }
        };
    }

    @Test
    void onFlatGroundThePathIsStraight() {
        List<PathStep> path = VillagePathPlanner.plan(new Ground((x, z) -> 0), 0, 0, 0, Side.EAST, to(10, 0))
                .orElseThrow();

        assertEquals(10, path.size());
        assertTrue(path.stream().allMatch(step -> step.z() == 0 && step.floorY() == 0 && step.heading() == Side.EAST));
    }

    /**
     * O buraco fundo de ponta a ponta não é aterrado: sem volta, a ligação não sai (autor, 28-10). A vala
     * de 3 ainda passa — corta um em cada margem e cobre um no fundo, tudo abaixo do chão em volta —, a de
     * 5 não.
     */
    @Test
    void aDeepDitchAcrossTheWholeWayIsNoWay() {
        Ground shallow = new Ground((x, z) -> x == 5 ? -3 : 0);
        List<PathStep> across = VillagePathPlanner.plan(shallow, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();

        assertSafe(shallow, across, 0);
        assertTrue(across.stream().allMatch(step -> step.floorY() <= 0), "o caminho subiu acima do chão: " + across);

        assertTrue(VillagePathPlanner.plan(new Ground((x, z) -> x == 5 ? -5 : 0), 0, 0, 0, Side.EAST, to(10, 0))
                .isEmpty(), "o caminho atravessou uma vala de 5 — teria de levantar terra");
    }

    /** O buraco de um bloco é coberto rente ao chão em volta, e nunca nasce trecho elevado. */
    @Test
    void aOneBlockHoleIsCoveredLevelWithTheGroundAround() {
        Ground ground = new Ground((x, z) -> x == 5 ? -1 : 0);

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();

        assertSafe(ground, path, 0);
        assertTrue(path.stream().allMatch(step -> step.fill() <= 1));
    }

    /** No chão plano nada fica acima do terreno: o piso é sempre o próprio chão. */
    @Test
    void onFlatGroundNothingRisesAboveIt() {
        List<PathStep> path = VillagePathPlanner.plan(new Ground((x, z) -> 0), 0, 0, 0, Side.EAST, to(10, 6))
                .orElseThrow();

        assertTrue(path.stream().allMatch(step -> step.fill() == 0 && step.cut() == 0 && step.floorY() == 0),
                "caminho fora do chão em terreno plano: " + path);
    }

    /** O passo aterrado só existe entre vizinhos no mesmo nível ou acima — nunca um trecho elevado. */
    @Test
    void theHoleRuleRefusesARaisedRun() {
        assertTrue(VillagePathPlanner.fitsTheHoleRule(false, 0, 1), "buraco entre iguais");
        assertTrue(VillagePathPlanner.fitsTheHoleRule(false, -1, 1), "buraco depois de descer");
        assertFalse(VillagePathPlanner.fitsTheHoleRule(false, 1, 1), "subir para um aterro é trecho elevado");
        assertFalse(VillagePathPlanner.fitsTheHoleRule(true, 0, 1), "dois aterros seguidos");
        assertFalse(VillagePathPlanner.fitsTheHoleRule(true, -1, 0), "descer depois do aterro: não era buraco");
        assertTrue(VillagePathPlanner.fitsTheHoleRule(true, 0, 0), "sair do buraco no mesmo nível");
    }

    @Test
    void aCliffIsClimbedByTheRampBesideItAndNotByCutting() {
        // Penhasco de 3 em x = 5 em toda a frente, menos uma rampa de 1 por bloco em z 20..24.
        Ground ground = new Ground((x, z) -> x < 5 ? 0
                : z >= 20 && z <= 24 && x <= 7 ? Math.min(3, x - 4) : 3);

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();

        assertSafe(ground, path, 0);
        assertEquals(3, path.getLast().floorY());
    }

    @Test
    void aSteepBumpIsCutAtMostOneBlock() {
        Ground ground = new Ground((x, z) -> x == 5 || x == 6 ? 2 : 0);

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();

        assertSafe(ground, path, 0);
        assertTrue(path.stream().allMatch(step -> step.cut() <= VillagePathPlanner.MAX_CUT));
    }

    /** No vão estreito o caminho estreita ali, sem tocar a construção dos lados (autor, 28-10). */
    @Test
    void aNarrowGapNarrowsThePath() {
        Ground ground = new Ground((x, z) -> 0);

        for (int z = -40; z <= 40; z++) {
            if (z != 0) {
                ground.block(5, z);
            }
        }

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();
        PathStep inTheGap = path.stream().filter(step -> step.x() == 5).findFirst().orElseThrow();

        assertEquals(0, inTheGap.z(), "o eixo devia passar no vão");
        assertFalse(VillagePathPlanner.laneFits(ground, inTheGap.sideX(-1), inTheGap.sideZ(-1), 0),
                "a coluna de lado no vão é construção e fica de fora");
    }

    @Test
    void waterIsNeverCrossed() {
        Ground ground = new Ground((x, z) -> 0);

        for (int z = -40; z <= 40; z++) {
            if (z < 15 || z > 19) {
                ground.water(5, z);
            }
        }

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.EAST, to(10, 0)).orElseThrow();

        assertTrue(path.stream().filter(step -> step.x() == 5).allMatch(step -> step.z() >= 15 && step.z() <= 19),
                "o caminho atravessou a água: " + path);
    }

    @Test
    void aNewPathJoinsTheNearestPieceOfTheNetwork() {
        Ground ground = new Ground((x, z) -> 0);
        Set<Long> network = new HashSet<>();

        for (int x = -40; x <= 40; x++) {
            network.add(key(x, 6));
        }

        VillagePathPlanner.Goal joinTheNetwork = new VillagePathPlanner.Goal() {
            @Override
            public boolean reached(int x, int floorY, int z) {
                return network.contains(key(x, z));
            }

            @Override
            public int estimate(int x, int z) {
                return Math.abs(z - 6);
            }
        };

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 0, Side.SOUTH, joinTheNetwork).orElseThrow();

        assertEquals(6, path.size(), "devia ir reto até a rede, seis passos ao sul");
        assertEquals(6, path.getLast().z());
    }

    /**
     * Caminho não corre colado ao lado de outro: ou se junta a ele, ou se afasta — autor, 27-10 ("unir-se
     * como um grande caminho, e não infinitos distintos").
     */
    @Test
    void aPathNeverRunsAlongsideAnotherOne() {
        Ground ground = new Ground((x, z) -> 0);
        Set<Long> network = new HashSet<>();

        for (int x = 4; x <= 16; x++) {
            for (int z = 3; z <= 5; z++) {
                network.add(key(x, z));
            }
        }

        VillagePathPlanner.Goal along = new VillagePathPlanner.Goal() {
            @Override
            public boolean reached(int x, int floorY, int z) {
                return x == 20 && z == 2;
            }

            @Override
            public int estimate(int x, int z) {
                return Math.abs(x - 20) + Math.abs(z - 2);
            }

            @Override
            public boolean isPath(int x, int floorY, int z) {
                return network.contains(key(x, z));
            }
        };

        List<PathStep> path = VillagePathPlanner.plan(ground, 0, 0, 2, Side.EAST, along).orElseThrow();

        for (PathStep step : path) {
            boolean onIt = network.contains(key(step.x(), step.z()));
            boolean touches = network.contains(key(step.sideX(-1), step.sideZ(-1)))
                    || network.contains(key(step.sideX(1), step.sideZ(1)));

            assertTrue(onIt || !touches, "o caminho correu colado ao lado do outro em " + step);
        }
    }

    /** Em terreno acidentado sorteado, todo traçado achado cumpre as regras de segurança. */
    @Test
    void everyPathOnRoughGroundIsSafe() {
        Random random = new Random(27);
        Map<Long, Integer> heights = new HashMap<>();
        Ground ground = new Ground((x, z) -> heights.computeIfAbsent(key(x / 3, z / 3), k -> random.nextInt(5)));
        int found = 0;

        for (int trial = 0; trial < 20; trial++) {
            int gx = random.nextInt(30) - 15;
            int gz = random.nextInt(30) - 15;
            int startY = ground.groundY(0, 0, 0).getAsInt();
            Optional<List<PathStep>> path = VillagePathPlanner.plan(ground, 0, startY, 0, Side.NORTH, to(gx, gz));

            if (path.isPresent() && !path.get().isEmpty()) {
                assertSafe(ground, path.get(), startY);
                found++;
            }
        }

        assertTrue(found >= 10, "poucos traçados achados no terreno sorteado: " + found);
    }

    private static void assertSafe(PathTerrain terrain, List<PathStep> path, int startY) {
        int lastY = startY;
        PathStep last = null;

        for (PathStep step : path) {
            assertTrue(Math.abs(step.floorY() - lastY) <= VillagePathPlanner.MAX_STEP,
                    "degrau maior que um em " + step);

            if (last != null) {
                assertEquals(1, Math.abs(step.x() - last.x()) + Math.abs(step.z() - last.z()), "passo que pula coluna");
            }

            int ground = terrain.groundY(step.x(), step.z(), step.floorY()).orElseThrow();

            assertFalse(terrain.blocked(step.x(), step.z()), "eixo em coluna proibida em " + step);
            assertTrue(step.floorY() - ground <= VillagePathPlanner.MAX_FILL, "aterro fundo demais em " + step);
            assertTrue(ground - step.floorY() <= VillagePathPlanner.MAX_CUT, "corte fundo demais em " + step);

            if (last != null && last.fill() > 0) {
                assertTrue(step.floorY() >= last.floorY() && step.fill() == 0,
                        "trecho elevado depois do buraco em " + step);
            }

            if (step.fill() > 0) {
                assertTrue(step.floorY() <= lastY, "subiu para um aterro em " + step);
            }

            lastY = step.floorY();
            last = step;
        }
    }
}
