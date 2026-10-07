package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Se um bloco é peça da planta de uma construção da colônia — estudo de
 * 01-10, §9b-A.
 *
 * <p>O encalhado fechado por blocos protegidos tentava para sempre. A saída
 * escolhida: <b>quebrar só a peça da própria colônia</b>, e deixar o reparo
 * ({@link BuildingRepairPlanner}) reconstruí-la. A pergunta não pode ser a
 * caixa da construção ({@code BlockProtection.isColonyBuilt}): ela também
 * cobre o bloco que o jogador pôs dentro da casa, e esse a Regra 3 não deixa
 * tocar. A resposta é a planta: só é peça da colônia o bloco que está
 * exatamente onde a planta põe aquele bloco — o mesmo critério com que o
 * reparo conta o que está de pé.
 *
 * <p>Custo: lê a planta da construção. Só é perguntado quando o encalhado já
 * se viu fechado, que é raro.
 */
final class ColonyPieces {

    private ColonyPieces() {
    }

    static boolean isPlannedPiece(ServerWorld world, BlockPos pos) {
        ColonyPos at = MinecraftTypeAdapter.toColonyPos(pos);
        Optional<Building> building = VillageColonyMod.BUILDINGS.at(at);

        if (building.isEmpty()) {
            return false;
        }

        ColonyPos origin = building.get().min();
        Optional<Blueprint> blueprint = PlanPlacement.blueprintOf(
                world, building.get().colonyId(), building.get().blueprint(), origin);

        if (blueprint.isEmpty()) {
            return false;
        }

        ColonyPos offset = new ColonyPos(at.x() - origin.x(), at.y() - origin.y(), at.z() - origin.z());

        for (BlueprintBlock block : blueprint.get().blocks()) {
            if (!block.offset().equals(offset)) {
                continue;
            }

            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            return expected.isPresent() && world.getBlockState(pos).isOf(expected.get());
        }

        return false;
    }
}
