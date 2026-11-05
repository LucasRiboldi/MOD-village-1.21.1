package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * O pulo e o nado do aldeão — autor, 2026-11-04.
 *
 * <ul>
 *   <li><b>Pulo:</b> a altura padrão do jogo. O maior ({@link #JUMP_GAIN}) só com obstáculo: o próximo trecho do
 *   caminho sobe mais que um bloco ou passa por cerca, ou ele bate em algo andando, está encalhado ou sai de um
 *   curral.</li>
 *   <li><b>Água:</b> com mais de {@value #WATER_AHEAD_MIN} blocos de água à frente, anda 5× mais rápido que no
 *   Vanilla, pelo atributo de eficiência na água, para todo aldeão.</li>
 * </ul>
 */
public final class VillagerMoves {

    private static final int JUMP_EVERY = 2;

    private static final int WATER_EVERY = 5;

    /** Quantos trechos do caminho à frente contam para o pulo maior: o atual e o seguinte. */
    private static final int NODES_AHEAD = 2;

    /** O degrau que o pulo padrão vence: até 1 bloco, pois ele sobe 1,25. */
    private static final int NORMAL_RISE = 1;

    /**
     * Quanto o pulo maior ganha: de 0,42 para 0,62, uns 2,2 blocos de altura — o bastante para o degrau de
     * {@code ColonyPathing.STEP} e para a cerca, que tem 1,5.
     */
    static final double JUMP_GAIN = 0.2;

    private static final Identifier JUMP_ID = Identifier.of("villagecolony", "obstacle_jump");

    /** "Mais de 5 blocos à frente": 5 não basta. */
    static final int WATER_AHEAD_MIN = 6;

    /**
     * A eficiência na água no teto do atributo. Fora do chão o jogo a corta pela metade: inércia 0,673 e aceleração
     * 0,16 contra 0,8 e 0,02 do Vanilla, cerca de 5× na velocidade em que o aldeão anda (0,3).
     */
    static final double WATER_EFFICIENCY = 1.0;

    private static final Identifier WATER_ID = Identifier.of("villagecolony", "open_water");

    private VillagerMoves() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!world.getRegistryKey().equals(World.OVERWORLD)) {
                return;
            }

            if (world.getTime() % JUMP_EVERY == 0) {
                for (Worker worker : VillageColonyMod.WORKERS.all()) {
                    if (world.getEntity(worker.villagerId()) instanceof VillagerEntity villager) {
                        setJump(villager, needsHighJump(world, villager));
                    }
                }
            }

            if (world.getTime() % WATER_EVERY == 0) {
                for (VillagerEntity villager : world.getEntitiesByType(EntityType.VILLAGER, VillagerEntity::isAlive)) {
                    setWater(villager, villager.isTouchingWater() && hasOpenWaterAhead(world, villager));
                }
            }
        });
    }

    /** Se este aldeão precisa do pulo maior agora. */
    static boolean needsHighJump(ServerWorld world, VillagerEntity villager) {
        if (StrandedWorkers.isStranded(villager.getUuid()) || PenEscape.isEscaping(villager.getUuid())) {
            return true;
        }

        Path path = villager.getNavigation().getCurrentPath();

        if (path == null || path.isFinished()) {
            return false;
        }

        if (villager.horizontalCollision) {
            return true;
        }

        BlockPos feet = villager.getBlockPos();
        int last = Math.min(path.getCurrentNodeIndex() + NODES_AHEAD, path.getLength());

        for (int index = path.getCurrentNodeIndex(); index < last; index++) {
            BlockPos node = path.getNodePos(index);

            if (node.getY() - feet.getY() > NORMAL_RISE || standsOnTallBlock(world, node)) {
                return true;
            }
        }

        return false;
    }

    /** O chão do nó passa de um bloco de altura: a cerca, o muro, o portão fechado. */
    private static boolean standsOnTallBlock(ServerWorld world, BlockPos node) {
        BlockPos under = node.down();
        BlockState state = world.getBlockState(under);
        var shape = state.getCollisionShape(world, under);

        return !shape.isEmpty() && shape.getMax(net.minecraft.util.math.Direction.Axis.Y) > 1.0;
    }

    /** Se há mais de 5 blocos de água seguidos na direção em que ele anda ou olha. */
    static boolean hasOpenWaterAhead(ServerWorld world, VillagerEntity villager) {
        Vec3d way = villager.getVelocity().horizontalLengthSquared() > 1.0E-4
                ? villager.getVelocity().multiply(1, 0, 1).normalize()
                : Vec3d.fromPolar(0, villager.getBodyYaw());
        Vec3d feet = villager.getPos();

        for (int step = 1; step <= WATER_AHEAD_MIN; step++) {
            BlockPos at = BlockPos.ofFloored(feet.add(way.multiply(step)));

            if (!world.isChunkLoaded(at) || !world.getFluidState(at).isIn(FluidTags.WATER)) {
                return false;
            }
        }

        return true;
    }

    private static void setJump(VillagerEntity villager, boolean high) {
        set(villager.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH), JUMP_ID, JUMP_GAIN, high);
    }

    private static void setWater(VillagerEntity villager, boolean fast) {
        set(villager.getAttributeInstance(EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY),
                WATER_ID, WATER_EFFICIENCY, fast);
    }

    /** Liga ou desliga o modificador; só escreve quando muda. */
    private static void set(EntityAttributeInstance attribute, Identifier id, double value, boolean on) {
        if (attribute == null || attribute.hasModifier(id) == on) {
            return;
        }

        if (on) {
            attribute.addTemporaryModifier(new EntityAttributeModifier(id, value,
                    EntityAttributeModifier.Operation.ADD_VALUE));
        } else {
            attribute.removeModifier(id);
        }
    }
}
