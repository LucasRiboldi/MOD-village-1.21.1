package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Varredura de superfície que acompanha a forma atual da vila.
 *
 * <p>Primeiro percorre a borda da caixa, depois cada retângulo interno até o
 * centro. Só então abre retângulos concêntricos do lado de fora. O cursor é
 * por trabalhador e guarda a coluna exata seguinte, portanto uma passagem
 * limitada não relê o que a anterior já examinou.
 */
public final class VillageSpiralSweep {

    static {
        ServerMemory.register(VillageSpiralSweep.class, VillageSpiralSweep::clearAll);
    }

    /** Mesmo teto de leitura de mundo da varredura genérica. */
    public static final int MAX_COLUMNS = 1024;

    private static final Map<UUID, Cursor> CURSORS = new HashMap<>();

    private enum Phase {
        INSIDE,
        OUTSIDE
    }

    private record Cursor(
            VillageBounds bounds, int y, int outerReach, Phase phase, int layer, int offset) {

        private boolean matches(VillageBounds expectedBounds, int expectedY, int expectedOuterReach) {
            return bounds.equals(expectedBounds) && y == expectedY && outerReach == expectedOuterReach;
        }

        private Cursor atOffset(int nextOffset) {
            return new Cursor(bounds, y, outerReach, phase, layer, nextOffset);
        }

        private Cursor atLayer(Phase nextPhase, int nextLayer) {
            return new Cursor(bounds, y, outerReach, nextPhase, nextLayer, 0);
        }
    }

    private record Rectangle(int minX, int minZ, int maxX, int maxZ) {
        private int width() {
            return maxX - minX + 1;
        }

        private int depth() {
            return maxZ - minZ + 1;
        }

        private int perimeterSize() {
            if (width() == 1) {
                return depth();
            }
            if (depth() == 1) {
                return width();
            }
            return 2 * width() + 2 * depth() - 4;
        }

        private BlockPos at(int y, int offset) {
            if (width() == 1) {
                return new BlockPos(minX, y, minZ + offset);
            }
            if (depth() == 1) {
                return new BlockPos(minX + offset, y, minZ);
            }

            if (offset < width()) {
                return new BlockPos(minX + offset, y, minZ);
            }
            offset -= width();

            if (offset < depth() - 1) {
                return new BlockPos(maxX, y, minZ + 1 + offset);
            }
            offset -= depth() - 1;

            if (offset < width() - 1) {
                return new BlockPos(maxX - 1 - offset, y, maxZ);
            }
            offset -= width() - 1;

            return new BlockPos(minX, y, maxZ - 1 - offset);
        }
    }

    private VillageSpiralSweep() {
    }

    /**
     * Procura a primeira coluna aceita, retomando após o teto por passagem.
     *
     * @param y altura transportada na coordenada; a leitura de cada coluna
     *     escolhe a superfície efetiva
     * @param outerReach quantos anéis além da caixa atual podem ser buscados
     * @param worth filtro aritmético barato, aplicado antes do orçamento
     */
    public static <T> Optional<T> next(
            UUID owner,
            VillageBounds bounds,
            int y,
            int outerReach,
            Predicate<BlockPos> worth,
            Function<BlockPos, Optional<T>> test) {

        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(worth, "worth");
        Objects.requireNonNull(test, "test");

        if (outerReach < 0) {
            throw new IllegalArgumentException("outer reach must not be negative: " + outerReach);
        }

        Cursor cursor = CURSORS.get(owner);
        if (cursor == null || !cursor.matches(bounds, y, outerReach)) {
            cursor = new Cursor(bounds, y, outerReach, Phase.INSIDE, 0, 0);
        }

        int looked = 0;

        while (true) {
            Rectangle rectangle = rectangle(cursor);
            if (rectangle == null) {
                CURSORS.remove(owner);
                return Optional.empty();
            }

            if (cursor.offset() >= rectangle.perimeterSize()) {
                cursor = nextLayer(cursor);
                continue;
            }

            BlockPos column = rectangle.at(cursor.y(), cursor.offset());

            if (!worth.test(column)) {
                cursor = cursor.atOffset(cursor.offset() + 1);
                continue;
            }

            if (looked >= MAX_COLUMNS) {
                CURSORS.put(owner, cursor);
                return Optional.empty();
            }

            looked++;
            cursor = cursor.atOffset(cursor.offset() + 1);

            Optional<T> found = test.apply(column);
            if (found.isPresent()) {
                CURSORS.remove(owner);
                return found;
            }
        }
    }

    private static Rectangle rectangle(Cursor cursor) {
        VillageBounds bounds = cursor.bounds();

        if (cursor.phase() == Phase.INSIDE) {
            int minX = bounds.minX() + cursor.layer();
            int minZ = bounds.minZ() + cursor.layer();
            int maxX = bounds.maxX() - cursor.layer();
            int maxZ = bounds.maxZ() - cursor.layer();

            return minX <= maxX && minZ <= maxZ
                    ? new Rectangle(minX, minZ, maxX, maxZ)
                    : null;
        }

        if (cursor.layer() > cursor.outerReach()) {
            return null;
        }

        return new Rectangle(
                bounds.minX() - cursor.layer(), bounds.minZ() - cursor.layer(),
                bounds.maxX() + cursor.layer(), bounds.maxZ() + cursor.layer());
    }

    private static Cursor nextLayer(Cursor cursor) {
        if (cursor.phase() == Phase.INSIDE) {
            int next = cursor.layer() + 1;
            int layers = (Math.min(cursor.bounds().sizeX(), cursor.bounds().sizeZ()) + 1) / 2;

            return next < layers
                    ? cursor.atLayer(Phase.INSIDE, next)
                    : cursor.atLayer(Phase.OUTSIDE, 1);
        }

        return cursor.atLayer(Phase.OUTSIDE, cursor.layer() + 1);
    }

    /** Onde a busca parou por falta de orçamento. */
    public static Optional<Integer> pausedAt(UUID owner) {
        return Optional.ofNullable(CURSORS.get(owner)).map(Cursor::layer);
    }

    /** Esquece a busca de um trabalhador. */
    public static void forget(UUID owner) {
        CURSORS.remove(owner);
    }

    /** Esquece todos os cursores ao parar o servidor. */
    public static void clearAll() {
        CURSORS.clear();
    }
}
