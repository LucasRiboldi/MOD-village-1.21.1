package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.BooleanSupplier;

/**
 * O que o mundo prova em volta da colônia, para a rota de suprimento — "rota na receita não é rota no
 * mundo" (playtest de 2026-09-26). Cada prova só é calculada se a rota perguntar.
 *
 * <ul>
 *   <li>areia de beira d'água ao alcance ({@link SandNearWater}, P2a, 2026-10-11);
 *   <li>árvore ao alcance ({@link WoodNearby}) — o deserto não tem madeira pela regra do bioma, mas o
 *       bosque fundacional e o viveiro plantam árvore lá, e a tocha e a cama dependiam dela (autor,
 *       2026-10-20: rota própria para cada item do registro de faltas);
 *   <li>argila ao alcance ({@link ClayNearby}) — a terracota do deserto, pelo mesmo motivo.
 * </ul>
 */
public record WorldEvidence(BooleanSupplier sandNearWater, BooleanSupplier woodNearby, BooleanSupplier clayNearby) {

    /** Sem mundo para provar nada: a rota é só a do bioma (a auditoria dos estilos usa esta). */
    public static final WorldEvidence NONE = new WorldEvidence(() -> false, () -> false, () -> false);

    /** As provas em volta desta colônia, cada uma guardada pela busca que a faz. */
    public static WorldEvidence around(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        int reach = com.villagecolony.core.coordination.GatheringReach.radius(colony.observedBeds(), SandNearWater.RADIUS);

        return new WorldEvidence(
                () -> SandNearWater.around(world, colony.id(), center, reach),
                () -> WoodNearby.around(world, colony.id(), center, reach),
                () -> ClayNearby.around(world, colony.id(), center, reach));
    }
}
