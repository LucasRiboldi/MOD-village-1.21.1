package com.villagecolony.fabric.work;

import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.Heightmap;

/**
 * O mineiro não entra na água debaixo da terra — ADR-025 fase 1, emendada pela Regra 71.
 *
 * <p>Na tabela do Vanilla (1.21.1) água custa 8 e não é proibida: com a volta seca longa, a
 * navegação atravessa nadando. Na mina, é o aldeão entrando no veio que a picareta abriu — lá a
 * água é -1, como a lava. Ao ar livre ele nada como qualquer aldeão, pelo custo do Vanilla: a
 * margem de lago cercada por paredão só se deixa nadando.
 *
 * <p>"Ao ar livre" é nada sólido acima dos pés: o mapa de altura {@code OCEAN_FLOOR} ignora a água,
 * então o mineiro que já nada no lago continua ao ar livre.
 *
 * <p>API pública do {@code MobEntity}, sem Mixin. A penalidade vive na entidade e não é salva, por
 * isso é reaplicada a cada passo — o mundo recarregado volta com o valor padrão.
 *
 * <p>{@code WATER_BORDER} fica como está: é a margem, chão seco ao lado da água, e proibi-la
 * fecharia os túneis que o {@code MineFlooding} acabou de vedar.
 */
public final class MinerCaution {

    private static final float NEVER = -1.0F;

    private MinerCaution() {
    }

    public static void keepOutOfWater(VillagerEntity villager) {
        float wanted = isUnderTheSky(villager) ? PathNodeType.WATER.getDefaultPenalty() : NEVER;

        if (villager.getPathfindingPenalty(PathNodeType.WATER) != wanted) {
            villager.setPathfindingPenalty(PathNodeType.WATER, wanted);
        }
    }

    static boolean isUnderTheSky(VillagerEntity villager) {
        return villager.getBlockY()
                >= villager.getWorld().getTopY(Heightmap.Type.OCEAN_FLOOR, villager.getBlockX(), villager.getBlockZ());
    }
}
