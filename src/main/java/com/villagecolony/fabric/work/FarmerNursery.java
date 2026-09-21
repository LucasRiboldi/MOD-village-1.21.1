package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quando e onde o fazendeiro planta a árvore — 2026-09-19.
 *
 * <p>A habilidade é do autor e está descrita em {@link TreeNursery}; o
 * que mora aqui é o <b>ritmo</b> e o <b>lugar</b>, que são as duas
 * decisões que fazem a diferença entre uma vila com árvores e um deserto
 * coberto de terra enraizada.
 *
 * <p><b>Na borda, e não no meio.</b> O pedido é <i>"no limite da vila"</i>,
 * e há razão prática: árvore no miolo tomaria lote de casa, e o
 * {@code BuildSiteScanner} passaria a recusar por volume ocupado — que foi
 * 70% das recusas no P1.6. A borda é onde não há obra disputando.
 *
 * <p><b>Uma por vez, com intervalo.</b> O fazendeiro chega aqui toda vez
 * que varre o raio e não acha lavoura, o que numa vila sem roça é
 * <b>sempre</b>. Sem freio ele plantaria uma árvore por passagem até a
 * borda inteira virar viveiro.
 */
public final class FarmerNursery {

    /**
     * Quantos tiques entre um plantio e o seguinte.
     *
     * <p>Seis mil — cinco minutos. Uma árvore leva mais que isso para
     * crescer, então plantar mais rápido só encheria a borda de rebentos
     * que ainda não são madeira.
     */
    public static final int BETWEEN_PLANTINGS = 6_000;

    /** Meta de árvores vivas mantidas pelo viveiro de cada vila. */
    public static final int TARGET_TREES = 10;

    /**
     * A que distância do centro o viveiro fica.
     *
     * <p>Dentro do alcance do lenhador — ele procura árvore em volta do
     * centro —, e fora do miolo onde as obras nascem. Plantar além do
     * alcance dele seria plantar para ninguém colher.
     */
    static final int EDGE = 28;

    /** Limite interno para não plantar dentro do miolo da vila. */
    private static final int INNER_EDGE = 20;

    /** Resolução do anel de candidatos, percorrido do mais distante ao centro. */
    private static final int DIRECTIONS = 48;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private FarmerNursery() {
    }

    /** Se já passou tempo bastante desde o último plantio desta colônia. */
    public static boolean isTime(UUID colonyId, long now) {
        Long last = LAST.get(colonyId);

        return last == null || now - last >= BETWEEN_PLANTINGS;
    }

    /**
     * Planta uma árvore na borda, se for hora e houver lugar.
     *
     * @return {@code true} se uma árvore nasceu agora
     */
    public static boolean plantIfItIsTime(ServerWorld world, UUID colonyId, BlockPos centre) {
        if (!isTime(colonyId, world.getTime())) {
            return false;
        }

        if (countNurseries(world, centre) >= TARGET_TREES) {
            return false;
        }

        Optional<Block> sapling =
                TreeNursery.saplingFor(world, MinecraftTypeAdapter.toColonyPos(centre));

        if (sapling.isEmpty()) {
            // Bioma sem madeira declarada: não é vila que o mod atende, e
            // inventar uma espécie aqui seria escolher por conta própria.
            return false;
        }

        Optional<BlockPos> spot = spotOnTheEdge(world, centre);

        if (spot.isEmpty()) {
            return false;
        }

        if (!TreeNursery.plant(world, spot.get(), sapling.get())) {
            return false;
        }

        LAST.put(colonyId, world.getTime());

        VillageColonyMod.LOGGER.info(
                "Colony {} — the farmer planted {} on rooted dirt at {}, at the village edge",
                colonyId.toString().substring(0, 8),
                TreeNursery.idOf(sapling.get()),
                spot.get().toShortString());

        return true;
    }

    /**
     * Um ponto na borda que sirva de viveiro.
     *
     * <p>Anda pelo anel externo para dentro. Assim a árvore fica o mais
     * longe possível do centro sem sair do alcance do lenhador; só usa uma
     * distância menor quando a borda está ocupada ou inacessível.
     */
    private static Optional<BlockPos> spotOnTheEdge(ServerWorld world, BlockPos centre) {
        for (int radius = EDGE; radius >= INNER_EDGE; radius--) {
            for (int step = 0; step < DIRECTIONS; step++) {
                double angle = 2 * Math.PI * step / DIRECTIONS;

                int x = centre.getX() + (int) Math.round(radius * Math.cos(angle));
                int z = centre.getZ() + (int) Math.round(radius * Math.sin(angle));

                Optional<BlockPos> ground = groundAt(world, x, z, centre.getY());

                if (ground.isPresent() && TreeNursery.isSpotForANursery(world, ground.get())) {
                    return ground;
                }
            }
        }

        return Optional.empty();
    }

    /** Conta marcadores de viveiro que ainda têm muda ou árvore em cima. */
    private static int countNurseries(ServerWorld world, BlockPos centre) {
        int count = 0;

        for (int x = centre.getX() - EDGE; x <= centre.getX() + EDGE; x++) {
            for (int z = centre.getZ() - EDGE; z <= centre.getZ() + EDGE; z++) {
                for (int y = centre.getY() + 4; y >= centre.getY() - 8; y--) {
                    BlockPos ground = new BlockPos(x, y, z);

                    if (!world.getBlockState(ground).isOf(TreeNursery.BED)
                            || (!world.getBlockState(ground.up()).isIn(BlockTags.SAPLINGS)
                            && !world.getBlockState(ground.up()).isIn(BlockTags.LOGS))) {
                        continue;
                    }

                    count++;
                    break;
                }
            }
        }

        return count;
    }

    /**
     * O chão nesta coluna, perto da altura da vila.
     *
     * <p>A janela é a mesma do {@code SandPatch} e pela mesma razão: a
     * vila não planta no alto do morro que a olha de cima nem no fundo do
     * desfiladeiro.
     */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z, int centreY) {
        for (int y = centreY + 4; y >= centreY - 8; y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (!world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** Marca que esta colônia plantou agora. Para a bateria. */
    public static void remember(UUID colonyId, long now) {
        LAST.put(colonyId, now);
    }

    /** Esquece o ritmo guardado. Chamado ao parar o servidor. */
    public static void clearAll() {
        LAST.clear();
    }
}
