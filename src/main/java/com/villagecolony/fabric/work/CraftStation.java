package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.content.ModContent;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Onde o artesão trabalha — ADR-038 P3c: diante da bancada do ofício, se a vila
 * tem uma (carpinteiro: bancada de trabalho perto da casa; pedreiro: cortador de
 * pedra da vila); sem bancada, em volta do sino; sem sino, no baú, como antes.
 * O material sai de todos os baús e o produto vai direto ao baú da profissão
 * ({@link CraftingSteps}): ele não carrega nada.
 */
final class CraftStation {

    static {
        ServerMemory.register(CraftStation.class, CraftStation::clearAll);
    }

    /** Quanto a escolha vale antes de olhar o mundo de novo. */
    static final int REFRESH_TICKS = 6_000;

    /** Até onde da casa (do baú) procurar a bancada do carpinteiro. */
    static final int BENCH_RADIUS = 16;

    /** Até onde procurar cortador de pedra e sino pelo registro de pontos do jogo. */
    static final int POI_RADIUS = 64;

    /**
     * Ticks de expediente que o artesão insiste em caminhar até a bancada antes de desistir e fabricar
     * onde está — a salvaguarda da regra de 2026-10-15. Reconcilia com a de 2026-10-11 ("fabrica onde
     * está; a caminhada até a bancada soltava a tarefa 'stuck on the way'"): agora ele vai até a
     * bancada, mas uma bancada inalcançável (ex.: do outro lado de um desnível na montanha) deixa de
     * travar — passado o prazo, fabrica no lugar até o fim da tarefa. 10 s a 20 tps.
     */
    static final int APPROACH_BUDGET = 200;

    /**
     * A ida até a bancada numa tarefa — regra do autor, 2026-10-15.
     *
     * <p>A bancada é procurada <b>uma vez</b>, quando o artesão pega a tarefa ({@link #benchFor}
     * custa uma consulta de POI de raio 64 e, para o carpinteiro, uma varredura de ~9.800 blocos;
     * fazê-la a cada tique era o defeito G2 da varredura de 2026-10-20). Se ele não chega dentro do
     * {@link #APPROACH_BUDGET}, desiste da bancada e fabrica onde está <b>até o fim da tarefa</b> — o
     * defeito G1 era voltar a caminhar a cada prazo, o que derrubava a produção. Na tarefa seguinte, com
     * um {@code Approach} novo, ele tenta a bancada de novo.
     */
    static final class Approach {

        private final @Nullable BlockPos bench;

        private int ticks;

        private boolean gaveUp;

        /** @param bench a bancada desta tarefa, ou {@code null} quando não há nenhuma ao alcance */
        Approach(@Nullable BlockPos bench) {
            this.bench = bench == null ? null : bench.toImmutable();
        }

        /** A bancada desta tarefa, ou {@code null} quando ele fabrica onde está. */
        @Nullable BlockPos bench() {
            return gaveUp ? null : bench;
        }

        /**
         * Um tique de expediente: se ele fabrica onde está agora.
         *
         * @param atBench se ele já está ao lado da bancada
         */
        boolean craftHere(boolean atBench) {
            if (bench == null || gaveUp || atBench) {
                return true;
            }

            if (++ticks >= APPROACH_BUDGET) {
                gaveUp = true;

                return true;
            }

            return false;
        }
    }

    private record Choice(BlockPos spot, long at) {
    }

    private static final Map<UUID, Choice> CHOSEN = new HashMap<>();

    private CraftStation() {
    }

    static void clearAll() {
        CHOSEN.clear();
    }

    /** Onde este artesão fica para trabalhar; {@code chest} é o baú da profissão dele. */
    static BlockPos spotFor(ServerWorld world, UUID workerId, BlockPos chest) {
        Choice known = CHOSEN.get(workerId);

        if (known != null && world.getTime() - known.at() < REFRESH_TICKS) {
            return known.spot();
        }

        Optional<ProfessionType> profession = VillageColonyMod.WORKERS.find(workerId).flatMap(Worker::profession);
        BlockPos spot = bench(world, profession.orElse(null), chest)
                .or(() -> bell(world, chest))
                .orElse(chest);

        CHOSEN.put(workerId, new Choice(spot.toImmutable(), world.getTime()));

        return spot;
    }

    /**
     * A bancada do mod desta profissão, se a vila tem uma perto — carpinteiro, pedreiro
     * ou ferreiro/fundidor ({@link ModContent}). É a estação preferida: usá-la dobra a
     * produção ({@link ModBenchBonus}). Vazio para profissão sem bancada do mod.
     */
    static Optional<BlockPos> modBench(ServerWorld world, ProfessionType profession, BlockPos near) {
        if (profession == null || ModContent.benchPoi(profession) == null) {
            return Optional.empty();
        }

        var poi = ModContent.benchPoi(profession);

        return world.getPointOfInterestStorage().getNearestPosition(
                entry -> entry.matchesKey(poi), near, POI_RADIUS,
                PointOfInterestStorage.OccupationStatus.ANY);
    }

    private static Optional<BlockPos> bench(ServerWorld world, ProfessionType profession, BlockPos chest) {
        // A bancada do mod vem primeiro: é a estação que dobra a produção (2026-10-13).
        Optional<BlockPos> mod = modBench(world, profession, chest);

        if (mod.isPresent()) {
            return mod;
        }

        if (profession == ProfessionType.MASON) {
            return world.getPointOfInterestStorage().getNearestPosition(
                    entry -> entry.matchesKey(PointOfInterestTypes.MASON), chest, POI_RADIUS,
                    PointOfInterestStorage.OccupationStatus.ANY);
        }

        if (profession != ProfessionType.CARPENTER) {
            return Optional.empty();
        }

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos at : BlockPos.iterate(chest.add(-BENCH_RADIUS, -4, -BENCH_RADIUS),
                chest.add(BENCH_RADIUS, 4, BENCH_RADIUS))) {
            WorldChunk chunk = world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4);

            if (chunk != null && chunk.getBlockState(at).isOf(Blocks.CRAFTING_TABLE)) {
                double distance = at.getSquaredDistance(chest);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at.toImmutable();
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private static Optional<BlockPos> bell(ServerWorld world, BlockPos chest) {
        return world.getPointOfInterestStorage().getNearestPosition(
                entry -> entry.matchesKey(PointOfInterestTypes.MEETING), chest, POI_RADIUS,
                PointOfInterestStorage.OccupationStatus.ANY);
    }

    /**
     * A bancada em que este artesão deve trabalhar: a do mod primeiro (dobra a produção), depois a
     * estação Vanilla do ofício, e vazio quando não há nenhuma — aí ele fabrica onde está (trabalho
     * normal). É o {@code mod → vanilla → normal} do fluxo de bancada (regra do autor, 2026-10-15).
     *
     * <p><b>Cara</b>: uma consulta de POI de raio 64 e, para o carpinteiro sem bancada do mod, uma
     * varredura de ~9.800 blocos. Chamar uma vez por tarefa, pelo {@link Approach} — nunca por tique.
     */
    static Optional<BlockPos> benchFor(ServerWorld world, UUID workerId, BlockPos chest) {
        Optional<ProfessionType> profession = VillageColonyMod.WORKERS.find(workerId).flatMap(Worker::profession);

        return bench(world, profession.orElse(null), chest);
    }
}
