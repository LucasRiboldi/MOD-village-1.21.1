package com.villagecolony.fabric.work;

import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.VillagerEntity;

/**
 * O mineiro não entra na água — 2026-09-25, ADR-025 fase 1.
 *
 * <p>Na tabela do Vanilla (1.21.1) água custa 8 e não é proibida: com a volta
 * seca longa, a navegação atravessa nadando. Na mina, é o aldeão entrando no
 * veio que a picareta abriu. O autor pediu "evitar entrar em água ou lava";
 * lava já é -1 no Vanilla, água passa a ser também, só para o mineiro.
 *
 * <p>API pública do {@code MobEntity}, sem Mixin. A penalidade vive na
 * entidade e não é salva, por isso é reaplicada a cada passo em vez de uma
 * vez ao contratar — o mundo recarregado volta com o valor padrão.
 *
 * <p>{@code WATER_BORDER} fica como está: é a margem, chão seco ao lado da
 * água, e proibi-la fecharia os túneis que o {@code MineFlooding} acabou de
 * vedar.
 */
public final class MinerCaution {

    private static final float NEVER = -1.0F;

    private MinerCaution() {
    }

    public static void keepOutOfWater(VillagerEntity villager) {
        if (villager.getPathfindingPenalty(PathNodeType.WATER) != NEVER) {
            villager.setPathfindingPenalty(PathNodeType.WATER, NEVER);
        }
    }
}
