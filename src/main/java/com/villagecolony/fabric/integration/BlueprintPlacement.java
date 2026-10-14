package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** Reconstitui uma planta lida do mundo na orientacao da rua atual. */
public final class BlueprintPlacement {

    private BlueprintPlacement() {
    }

    public static Optional<Blueprint> blueprintOf(
            ServerWorld world, UUID colonyId, ResourceId id, ColonyPos origin) {
        return StructureBlueprintReader.read(world, id)
                .map(FarmBlueprint::asBuilt)
                .map(blueprint -> blueprint.doorSide()
                        .map(door -> blueprint.rotated(
                                door.turnsTo(roadSideOf(world, colonyId, origin, blueprint))))
                        .orElse(blueprint));
    }

    /**
     * A planta da obra que volta do save, na orientação em que ela foi levantada.
     *
     * <p>O save guarda a planta e o lugar, não o giro, e {@link #blueprintOf} gira
     * pela rua de agora. Com o índice de ruas mudado entre uma sessão e outra, a
     * mesma casa voltava girada por cima dela mesma: no playtest 0.3.31 a
     * {@code plains_medium_house_1} com 8 peças por pôr voltou com 292, limpou 27
     * blocos do lote e pulou 120 peças "no caminho". Quem sabe o giro é o mundo: das
     * quatro orientações vale a que mais casa com o que já está de pé; no empate, a
     * da rua.
     */
    public static Optional<Blueprint> resumedBlueprintOf(
            ServerWorld world, UUID colonyId, ResourceId id, ColonyPos origin) {
        return blueprintOf(world, colonyId, id, origin).map(byRoad -> {
            Blueprint best = byRoad;
            int bestStanding = standing(world, byRoad, origin);

            for (int turns = 1; turns < 4; turns++) {
                Blueprint turned = byRoad.rotated(turns);
                int standing = standing(world, turned, origin);

                if (standing > bestStanding) {
                    best = turned;
                    bestStanding = standing;
                }
            }

            return best;
        });
    }

    /** Quantas peças desta planta, posta neste lugar, já estão de pé no mundo. */
    static int standing(ServerWorld world, Blueprint blueprint, ColonyPos origin) {
        int standing = 0;

        for (BlueprintBlock block : blueprint.blocks()) {
            BlockPos where = new BlockPos(
                    origin.x() + block.offset().x(), origin.y() + block.offset().y(), origin.z() + block.offset().z());
            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            if (expected.isPresent() && world.isChunkLoaded(where) && world.getBlockState(where).isOf(expected.get())) {
                standing++;
            }
        }

        return standing;
    }

    public static Side roadSideOf(
            ServerWorld world, UUID colonyId, ColonyPos origin, Blueprint blueprint) {
        return RoadIndex.roadSideOf(world, colonyId, origin, blueprint.size())
                .map(MinecraftTypeAdapter::toSide)
                .orElse(Side.NORTH);
    }
}
