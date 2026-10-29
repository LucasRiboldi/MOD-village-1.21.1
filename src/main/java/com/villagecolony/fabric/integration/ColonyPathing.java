package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.Optional;

/**
 * Como o aldeão da colônia escolhe o trajeto — autor, 2026-10-28, depois do jogo da 0.3.104 (4 de 7 teleportes
 * noturnos saíram do meio do lago, e o aldeão cortava pelo gramado). Muda só o custo dos nós da navegação do jogo:
 *
 * <ul>
 *   <li><b>Água:</b> o nó de água custa {@link #WATER_COST} (o Vanilla cobra 8). Ele nada só quando a volta a pé
 *   é muito mais longa ou não existe — escolha "nadar só se não houver volta".</li>
 *   <li><b>Rua:</b> dentro da caixa da vila, o nó andável sem chão de rua embaixo vira {@link #OFF_STREET}, que
 *   custa {@link #OFF_STREET_COST}. A rua ganha sempre que o desvio por ela não passa de cerca de 2 vezes a
 *   linha reta; perto do alvo ele sai da rua normalmente.</li>
 * </ul>
 *
 * <p>Os custos vão no próprio aldeão ({@code MobEntity.setPathfindingPenalty}), na primeira busca dele. Quem não é
 * da colônia anda como no Vanilla.
 */
public final class ColonyPathing {

    /**
     * O custo de cada nó de água para o aldeão da colônia. Alto de propósito: a busca do jogo pesa a distância até o
     * alvo 1,5 vez ({@code PathNodeNavigator}) e aceita caminho até 1,5 vez mais caro que o melhor; com 24, cruzar
     * 2 blocos (70) ainda vencia uma volta de 47. Com 48, cruzar 2 custa 118 e a volta de até ~78 ganha.
     */
    public static final float WATER_COST = 48.0F;

    /**
     * O tipo do nó de gramado dentro da vila: o perigo cauteloso, que o Vanilla só usa para dar custo (0 por padrão)
     * e que nenhum bloco da vila produz.
     */
    public static final PathNodeType OFF_STREET = PathNodeType.DAMAGE_CAUTIOUS;

    /** O custo de cada nó fora da rua, dentro da caixa da vila. */
    public static final float OFF_STREET_COST = 1.0F;

    private ColonyPathing() {
    }

    /**
     * O tipo do nó para este bicho: o mesmo, ou {@link #OFF_STREET} quando é aldeão da colônia, o nó é andável, fica
     * dentro da caixa da vila dele e o chão embaixo não é de rua. Prepara os custos do aldeão na passagem.
     */
    public static PathNodeType forWalker(BlockView world, MobEntity mob, int x, int y, int z, PathNodeType type) {
        if (!(mob instanceof VillagerEntity)) {
            return type;
        }

        Optional<Worker> worker = VillageColonyMod.WORKERS.find(mob.getUuid());

        if (worker.isEmpty()) {
            return type;
        }

        prepare(mob);

        if (type != PathNodeType.WALKABLE || !insideTheVillage(worker.get(), x, z)) {
            return type;
        }

        BlockState floor = world.getBlockState(new BlockPos(x, y - 1, z));

        return VillageRoad.isPathBlock(floor) || StreetSurface.isStreetMaterial(floor) ? type : OFF_STREET;
    }

    /** Os custos do aldeão da colônia; só escreve quando ainda não estão lá. */
    static void prepare(MobEntity mob) {
        if (mob.getPathfindingPenalty(PathNodeType.WATER) != WATER_COST) {
            mob.setPathfindingPenalty(PathNodeType.WATER, WATER_COST);
        }

        if (mob.getPathfindingPenalty(OFF_STREET) != OFF_STREET_COST) {
            mob.setPathfindingPenalty(OFF_STREET, OFF_STREET_COST);
        }
    }

    private static boolean insideTheVillage(Worker worker, int x, int z) {
        return VillageColonyMod.COLONIES.find(worker.colonyId())
                .flatMap(Colony::bounds)
                .filter(box -> inside(box, x, z))
                .isPresent();
    }

    private static boolean inside(VillageBounds box, int x, int z) {
        return x >= box.minX() && x <= box.maxX() && z >= box.minZ() && z <= box.maxZ();
    }
}
