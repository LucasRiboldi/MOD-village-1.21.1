package com.villagecolony.fabric.world;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ServerMemory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O chão firme da vila de areia — decisão do autor, 2026-10-21.
 *
 * <p>No deserto, tirar um bloco do chão da vila (o mineiro, o construtor, a obra) fazia a areia de
 * cima cair e o chão desmoronar. Agora, em cada coluna da caixa da vila cuja superfície é areia, as
 * {@link #LAYERS} camadas logo abaixo dela, se forem areia, viram arenito (a areia vermelha vira arenito
 * vermelho): a superfície continua areia de verdade — o visual não muda — e passa a assentar em bloco
 * que não cai. Telhado, chão de casa e rua não são areia e ficam de fora.
 *
 * <p>Andando aos poucos ({@link #COLUMNS_PER_PASS} colunas por segundo), e de novo a cada dia de jogo
 * ou quando a caixa da vila cresce: chunk que não estava carregado entra na volta seguinte.
 */
public final class DesertGround {

    static {
        ServerMemory.register(DesertGround.class, DesertGround::clearAll);
    }

    /** Quantas camadas abaixo da superfície viram arenito. */
    public static final int LAYERS = 2;

    /** Colunas por passagem (uma passagem por segundo). */
    static final int COLUMNS_PER_PASS = 512;

    /** De quanto em quanto a vila inteira é revista: um dia de jogo. */
    static final long AGAIN_AFTER = 24_000;

    private static final class Sweep {

        final VillageBounds box;

        int next;

        long finishedAt = Long.MIN_VALUE;

        int firmed;

        Sweep(VillageBounds box) {
            this.box = box;
        }
    }

    private static final Map<UUID, Sweep> SWEEPS = new HashMap<>();

    private DesertGround() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % 20 == 0) {
                tick(world);
            }
        });
    }

    private static void tick(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            Optional<VillageBounds> box = colony.bounds();

            if (!colony.isActive() || !colony.isAttended(now) || box.isEmpty()) {
                continue;
            }

            Sweep sweep = SWEEPS.get(colony.id());

            if (sweep == null || !sweep.box.equals(box.get())
                    || (sweep.finishedAt != Long.MIN_VALUE && now - sweep.finishedAt >= AGAIN_AFTER)) {
                sweep = new Sweep(box.get());
                SWEEPS.put(colony.id(), sweep);
            }

            if (sweep.finishedAt == Long.MIN_VALUE) {
                pass(world, colony.id(), sweep, now);
            }
        }
    }

    private static void pass(ServerWorld world, UUID colonyId, Sweep sweep, long now) {
        VillageBounds box = sweep.box;
        int width = box.maxX() - box.minX() + 1;
        int columns = width * (box.maxZ() - box.minZ() + 1);
        int stop = Math.min(columns, sweep.next + COLUMNS_PER_PASS);

        for (; sweep.next < stop; sweep.next++) {
            sweep.firmed += firmColumn(world, box.minX() + sweep.next % width, box.minZ() + sweep.next / width);
        }

        if (sweep.next >= columns) {
            sweep.finishedAt = now;

            if (sweep.firmed > 0) {
                VillageColonyMod.LOGGER.info("Colony {} firmed the sand under its ground — {} blocks became sandstone",
                        colonyId.toString().substring(0, 8), sweep.firmed);
            }
        }
    }

    /**
     * A coluna em {@code (x, z)}: com areia na superfície, as camadas de baixo que são areia viram
     * arenito.
     *
     * @return quantos blocos viraram arenito
     */
    public static int firmColumn(ServerWorld world, int x, int z) {
        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            return 0;
        }

        BlockPos surface = new BlockPos(x, chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

        if (firmFor(chunk.getBlockState(surface)).isEmpty()) {
            return 0;
        }

        int firmed = 0;

        for (int depth = 1; depth <= LAYERS; depth++) {
            BlockPos below = surface.down(depth);
            Optional<BlockState> firm = firmFor(chunk.getBlockState(below));

            if (firm.isPresent()) {
                world.setBlockState(below, firm.get(), Block.NOTIFY_LISTENERS);
                firmed++;
            }
        }

        return firmed;
    }

    /** O bloco que não cai no lugar desta areia: arenito, ou arenito vermelho. */
    static Optional<BlockState> firmFor(BlockState state) {
        if (state.isOf(Blocks.SAND)) {
            return Optional.of(Blocks.SANDSTONE.getDefaultState());
        }

        if (state.isOf(Blocks.RED_SAND)) {
            return Optional.of(Blocks.RED_SANDSTONE.getDefaultState());
        }

        return Optional.empty();
    }

    static void clearAll() {
        SWEEPS.clear();
    }
}
