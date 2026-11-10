package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O socorro da vila sem lote — Regra 222, ADR-049: depois de {@value #SWEEPS_BEFORE} voltas inteiras sem lote para a próxima obra, a
 * vila procura sozinha um lote a até {@value #REACH} blocos da borda da caixa, um lado por vez, e o pedido entra na fila do construtor
 * como o de {@code /vc mae obra} (Regra 213). Avisa o jogador e não espera aprovação. Lote que toca água é recusado.
 *
 * <p>Só conta a volta sem lote da casa, quando a vila inteira ficou sem lugar, e as obras seguintes formam um bairro: mesmo
 * lado, coladas na anterior — Regra 226.
 */
public final class MaeRescue {

    /** Quantas voltas sem lote antes do socorro: a primeira é da rua, que também abre lote (Regra 15). */
    static final int SWEEPS_BEFORE = 2;
    /** Até onde o lote vai, contado da borda da caixa da vila. */
    public static final int REACH = 24;
    /** Quantas plantas e quantos lotes cada passada confere: conferir lote custa, e isto roda no tique do servidor. */
    private static final int PLANS_TRIED = 2;
    private static final int LOTS_TRIED = 4;

    /** A última obra do socorro: o bairro cresce em volta dela, do mesmo lado (Regra 226). */
    private record Anchor(Side side, ColonyPos origin) {
    }

    private static final Map<UUID, Integer> SWEPT = new HashMap<>();
    private static final Map<UUID, Integer> NEXT_SIDE = new HashMap<>();
    private static final Map<UUID, Anchor> NEIGHBOURHOOD = new HashMap<>();

    static {
        ServerMemory.register(MaeRescue.class, () -> {
            SWEPT.clear();
            NEXT_SIDE.clear();
            NEIGHBOURHOOD.clear();
        });
    }

    private MaeRescue() {
    }

    /** A busca normal achou lote: a conta recomeça. */
    public static void foundALot(UUID colonyId) {
        SWEPT.remove(colonyId);
    }

    /**
     * A volta inteira terminou sem lote para estas plantas.
     *
     * @return o pedido, se o socorro abriu um nesta passada
     */
    public static Optional<MaeSites.Status> sweptWithoutLot(ServerWorld world, Colony colony, List<Blueprint> plans) {
        int swept = SWEPT.merge(colony.id(), 1, Integer::sum);

        if (swept < SWEEPS_BEFORE || colony.bounds().isEmpty() || !MaeBuildQueue.ofColony(world, colony.id()).isEmpty()) {
            return Optional.empty();
        }

        // O bairro: depois da primeira obra, as seguintes ficam do mesmo lado e o mais perto dela; o lado cheio passa a vez.
        Anchor anchor = NEIGHBOURHOOD.get(colony.id());
        Side side;

        if (anchor != null) {
            side = anchor.side();
        } else {
            Side[] sides = Side.values();
            int turn = NEXT_SIDE.merge(colony.id(), 1, Integer::sum) - 1;

            side = sides[Math.floorMod(turn, sides.length)];
        }

        long began = System.nanoTime();
        ColonyPos near = anchor != null ? anchor.origin() : colony.center();
        Optional<MaeSites.Status> started = openOnSide(world, colony, colony.bounds().get(), side, near, plans);

        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 colony={} result=rescue side={} found={} near={} ms={}",
                colony.id().toString().substring(0, 8), side, started.isPresent(), anchor != null ? "neighbourhood" : "center",
                (System.nanoTime() - began) / 1_000_000);

        if (started.isPresent()) {
            SWEPT.remove(colony.id());
            NEIGHBOURHOOD.put(colony.id(), new Anchor(side, com.villagecolony.fabric.adapter.MinecraftTypeAdapter.toColonyPos(
                    started.get().origin())));
            world.getPlayers().forEach(player -> player.sendMessage(notice(started.get()), false));
        } else {
            NEIGHBOURHOOD.remove(colony.id());
        }

        return started;
    }

    /** O aviso ao jogador: a obra não espera aprovação, e ele pode cancelar. */
    public static Text notice(MaeSites.Status status) {
        BlockPos at = status.origin();

        return Text.literal("A vila não achou lugar e vai construir " + status.model().path() + " em " + at.toShortString()
                + ". Para cancelar: /vc mae parar ou a tocha das almas.").formatted(Formatting.AQUA);
    }

    private static Optional<MaeSites.Status> openOnSide(
            ServerWorld world, Colony colony, VillageBounds box, Side side, ColonyPos near, List<Blueprint> plans) {
        for (Blueprint plan : plans.subList(0, Math.min(PLANS_TRIED, plans.size()))) {
            int quarterTurns = turnFacing(plan, facingTheVillage(side));

            for (BlockPos lot : lotsOnSide(world, near, box, side, plan.rotated(quarterTurns))) {
                try {
                    return Optional.of(MaeSites.start(world, colony.id(), plan.id(), lot, quarterTurns, null));
                } catch (IllegalStateException | IllegalArgumentException refused) {
                    if ("too_many_sites".equals(refused.getMessage())) {
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.empty();
    }

    /** Os lotes na faixa deste lado, os sem árvore primeiro e, entre eles, os mais perto de {@code center}: o centro ou o bairro. */
    private static List<BlockPos> lotsOnSide(ServerWorld world, ColonyPos center, VillageBounds box, Side side, Blueprint plan) {
        int bandMinX = side == Side.EAST ? box.maxX() + 1 : box.minX() - REACH;
        int bandMaxX = side == Side.WEST ? box.minX() - 1 : box.maxX() + REACH;
        int bandMinZ = side == Side.SOUTH ? box.maxZ() + 1 : box.minZ() - REACH;
        int bandMaxZ = side == Side.NORTH ? box.minZ() - 1 : box.maxZ() + REACH;
        // A leitura sobra além da faixa para ver a água que o anel de chão do lote tocaria.
        int margin = MaeSites.LOT_MARGIN;
        int minX = bandMinX - margin;
        int minZ = bandMinZ - margin;
        int width = bandMaxX - bandMinX + 1 + 2 * margin;
        int depth = bandMaxZ - bandMinZ + 1 + 2 * margin;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];

        MaeSites.readGround(world, minX, minZ, width, depth, heights, growth);
        closeAroundWater(world, minX, minZ, width, depth, heights, margin);

        var size = plan.size();
        List<FoundationLotSearch.Lot> lots = new ArrayList<>();

        for (var lot : FoundationLotSearch.candidates(heights, growth, width, depth, size.x(), size.z())) {
            int x = minX + lot.offsetX();
            int z = minZ + lot.offsetZ();

            if (x >= bandMinX && x + size.x() - 1 <= bandMaxX && z >= bandMinZ && z + size.z() - 1 <= bandMaxZ) {
                lots.add(lot);
            }
        }

        return lots.stream()
                .sorted(Comparator.comparing(FoundationLotSearch.Lot::clearing).thenComparingLong(lot -> {
                    long dx = (long) minX + lot.offsetX() + size.x() / 2 - center.x();
                    long dz = (long) minZ + lot.offsetZ() + size.z() / 2 - center.z();

                    return dx * dx + dz * dz;
                }))
                .limit(LOTS_TRIED)
                .map(lot -> new BlockPos(minX + lot.offsetX(), lot.floor(), minZ + lot.offsetZ()))
                .toList();
    }

    /** Coluna com fluido no topo fecha ela e o anel em volta: o lote automático não toca água nem lava (ADR-049). */
    private static void closeAroundWater(ServerWorld world, int minX, int minZ, int width, int depth, int[] heights, int margin) {
        int[] read = heights.clone();

        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int top = read[dz * width + dx];

                if (top != FoundationLotSearch.UNKNOWN
                        && !world.getFluidState(new BlockPos(minX + dx, top - 1, minZ + dz)).isEmpty()) {
                    for (int z = Math.max(0, dz - margin); z <= Math.min(depth - 1, dz + margin); z++) {
                        for (int x = Math.max(0, dx - margin); x <= Math.min(width - 1, dx + margin); x++) {
                            heights[z * width + x] = FoundationLotSearch.UNKNOWN;
                        }
                    }
                }
            }
        }
    }

    /** A porta olha para a vila: o lote ao norte da caixa abre para o sul. */
    static Side facingTheVillage(Side side) {
        return switch (side) {
            case NORTH -> Side.SOUTH;
            case SOUTH -> Side.NORTH;
            case EAST -> Side.WEST;
            case WEST -> Side.EAST;
        };
    }

    /** O giro que põe a porta neste lado; planta sem porta não gira. */
    private static int turnFacing(Blueprint plan, Side wanted) {
        for (int turn = 0; turn < 4; turn++) {
            if (plan.rotated(turn).doorSide().filter(wanted::equals).isPresent()) {
                return turn;
            }
        }

        return 0;
    }
}
