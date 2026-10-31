package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.StreetRoute;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.HashSet;
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
 *   <li><b>Trajeto longo:</b> acima de {@link StreetRoute#LONG} blocos ele vai pela rede de ruas, trecho a trecho
 *   ({@link #legToward}, chamado da {@code MoveToTargetTask} do jogo).</li>
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

    /** Até quantos blocos acima e abaixo do aldeão se procura o chão do trecho. */
    static final int STAND_SEARCH = 12;

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

    /**
     * Para onde o aldeão anda agora quando o cérebro do jogo o manda a {@code target}: o próximo trecho pela rede de
     * ruas no trajeto longo ({@link StreetRoute}), de pé no chão da coluna; o próprio destino quando ele não é da
     * colônia, o trajeto é curto, a rua não serve ou o chunk do trecho não está carregado. A cada trecho percorrido o
     * cérebro pede de novo, e recebe o seguinte.
     */
    public static BlockPos legToward(MobEntity mob, BlockPos target) {
        if (!(mob instanceof VillagerEntity) || !(mob.getWorld() instanceof ServerWorld world)) {
            return target;
        }

        Optional<Long> next = VillageColonyMod.WORKERS.find(mob.getUuid())
                .flatMap(worker -> RoadIndex.roadsOf(worker.colonyId()))
                .flatMap(roads -> StreetRoute.nextWaypoint(new HashSet<>(roads.columns()),
                        mob.getBlockX(), mob.getBlockZ(), target.getX(), target.getZ()));

        if (next.isEmpty()) {
            return target;
        }

        int x = ColonyRoads.xOf(next.get());
        int z = ColonyRoads.zOf(next.get());

        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return target;
        }

        return standingOn(world, x, z, mob.getBlockY()).orElse(target);
    }

    /**
     * Onde se fica de pé nesta coluna, o mais perto da altura {@code aroundY}: chão firme e 2 livres em cima. Pela
     * altura dele e não pelo topo do mundo — a rua debaixo de uma ponte ou de um beiral também é rua.
     */
    private static Optional<BlockPos> standingOn(ServerWorld world, int x, int z, int aroundY) {
        for (int step = 0; step <= 2 * STAND_SEARCH; step++) {
            // 0, -1, +1, -2, +2...: o chão mais perto da altura dele ganha.
            int y = aroundY + (step % 2 == 0 ? step / 2 : -(step + 1) / 2);
            BlockPos feet = new BlockPos(x, y, z);

            if (!world.getBlockState(feet.down()).getCollisionShape(world, feet.down()).isEmpty()
                    && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                    && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()) {
                return Optional.of(feet);
            }
        }

        return Optional.empty();
    }

    /** Os custos do aldeão da colônia; só escreve quando ainda não estão lá. */
    static void prepare(MobEntity mob) {
        if (mob.getPathfindingPenalty(PathNodeType.WATER) != WATER_COST) {
            mob.setPathfindingPenalty(PathNodeType.WATER, WATER_COST);
        }

        if (mob.getPathfindingPenalty(OFF_STREET) != OFF_STREET_COST) {
            mob.setPathfindingPenalty(OFF_STREET, OFF_STREET_COST);
        }

        // Pula 2 blocos e por cima da cerca — autor, 2026-10-29 ("pulo de verdade").
        if (mob.getNavigation() instanceof net.minecraft.entity.ai.pathing.MobNavigation walking) {
            walking.setCanWalkOverFences(true);
        }

        var jump = mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_JUMP_STRENGTH);

        if (jump != null && !jump.hasModifier(JUMP_ID)) {
            jump.addTemporaryModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(JUMP_ID, JUMP_GAIN,
                    net.minecraft.entity.attribute.EntityAttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** O degrau que o caminho do aldeão da colônia sobe de um nó para o outro (o Vanilla, 1 para o aldeão). */
    public static final float STEP = 2.0f;

    /**
     * Quanto o pulo do aldeão da colônia ganha: de 0,42 para 0,62, uns 2,2 blocos de altura — o bastante para o
     * degrau de {@link #STEP} e para a cerca, que tem 1,5.
     */
    static final double JUMP_GAIN = 0.2;

    static final net.minecraft.util.Identifier JUMP_ID = net.minecraft.util.Identifier.of("villagecolony", "colony_jump");

    /**
     * A altura de passo que o construtor de caminho do jogo usa para este bicho: {@link #STEP} para o aldeão da
     * colônia (o caminho aceita subir 2 blocos e o topo da cerca; o controle de movimento do jogo pula sempre que o
     * próximo nó fica acima do passo do corpo, que não muda); a do jogo para os outros.
     */
    public static float stepFor(MobEntity mob, float vanilla) {
        return mob instanceof VillagerEntity && VillageColonyMod.WORKERS.find(mob.getUuid()).isPresent()
                ? Math.max(vanilla, STEP) : vanilla;
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
