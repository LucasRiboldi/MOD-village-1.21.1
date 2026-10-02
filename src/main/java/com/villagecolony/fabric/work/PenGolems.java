package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

/**
 * O golem da vila também sai do curral — pedido do autor, 2026-10-02:
 * <i>"os golens da vila também devem poder sair pela porteira do curral e
 * saltar as cercas"</i>.
 *
 * <p>Quem mede e conduz é o {@link PenEscape}, o mesmo do aldeão. Esta classe
 * diz quais golens medir e por onde um corpo largo passa.
 *
 * <p><b>Só o golem da vila.</b> O que o jogador montou ({@code isPlayerCreated})
 * pode estar no curral de propósito, e fica.
 *
 * <p><b>Porteira simples não serve.</b> O golem tem 1,4 de largura e o vão de
 * uma porteira tem 1: ele não passa, nem no jogo sem o mod. Ele usa a porteira
 * dupla — as duas folhas abertas, andando pela junta entre elas — e, sem ela,
 * pula a cerca.
 */
final class PenGolems {

    /** Sem a caixa medida, a mesma régua da detecção de vila. */
    private static final int UNMEASURED_REACH = 64;

    /** Quanto acima e abaixo da caixa ainda conta: golem em cima de um telhado, golem no fosso. */
    private static final int HEIGHT_MARGIN = 4;

    private PenGolems() {
    }

    /** Mede os golens de cada vila atendida, na leva {@code group} de {@code groups}. */
    static void scan(ServerWorld world, long group, int groups) {
        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (!PenEscape.isWorking(world, colony.id())) {
                continue;
            }

            for (IronGolemEntity golem : world.getEntitiesByClass(IronGolemEntity.class, boxOf(colony),
                    golem -> golem.isAlive() && !golem.isPlayerCreated() && golem.getTarget() == null
                            && Math.floorMod(golem.getUuid().hashCode(), groups) == group)) {

                if (!PenEscape.isEscaping(golem.getUuid())) {
                    PenEscape.check(world, golem);
                }
            }
        }
    }

    static Box boxOf(Colony colony) {
        Optional<VillageBounds> measured = colony.bounds();

        if (measured.isPresent()) {
            VillageBounds box = measured.get();

            return new Box(box.minX(), box.minY() - HEIGHT_MARGIN, box.minZ(),
                    box.maxX() + 1, box.maxY() + 1 + HEIGHT_MARGIN, box.maxZ() + 1);
        }

        ColonyPos center = colony.center();

        return new Box(center.x() - UNMEASURED_REACH, center.y() - 16, center.z() - UNMEASURED_REACH,
                center.x() + UNMEASURED_REACH + 1, center.y() + 16, center.z() + UNMEASURED_REACH + 1);
    }

    /** Se o corpo não cabe no vão de uma porteira simples. */
    static boolean isWide(MobEntity mob) {
        return mob.getWidth() > 1.0F;
    }

    /**
     * A outra folha de uma porteira dupla: a porteira ao lado desta, na linha
     * da cerca. Vazio quando a porteira é simples.
     */
    static Optional<BlockPos> twinOf(ServerWorld world, FencedIn.Exit exit) {
        int dx = Integer.signum(exit.outside().getX() - exit.inside().getX());
        int dz = Integer.signum(exit.outside().getZ() - exit.inside().getZ());

        // Na linha da cerca: perpendicular ao rumo de dentro para fora.
        for (BlockPos side : List.of(exit.barrier().add(-dz, 0, dx), exit.barrier().add(dz, 0, -dx))) {
            if (world.getBlockState(side).isIn(BlockTags.FENCE_GATES)) {
                return Optional.of(side.toImmutable());
            }
        }

        return Optional.empty();
    }

    /** Meio bloco na direção da outra folha: a junta entre as duas. */
    static Vec3d towards(BlockPos gate, BlockPos twin) {
        return new Vec3d((twin.getX() - gate.getX()) * 0.5, 0.0, (twin.getZ() - gate.getZ()) * 0.5);
    }
}
