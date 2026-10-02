package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O encalhado sobe até a superfície, e nunca desiste — 2026-10-01.
 *
 * <p><b>O que falhou.</b> Na sessão das 09:16 o mineiro 199ad062 cavou os 32
 * degraus da fuga e parou a y=56, quinze abaixo da superfície, "until someone
 * frees it". Os degraus eram contados ao cavar, não ao subir: o aldeão
 * dependia da navegação Vanilla para pisar em cada degrau, e o trabalho de
 * mineiro, que seguia correndo ao lado, o puxava de volta (ver
 * {@link WorkerJobs}). Metade dos degraus foi cavada sem ele sair do lugar.
 *
 * <p><b>Três jeitos de subir, um depois do outro</b>, trocando quando o atual
 * passa {@link #NO_GAIN_PASSES} passagens sem ganhar altura:
 *
 * <ol>
 *   <li><b>escada</b> — o degrau do {@link StrandedEscape#planStep}, na direção
 *       da vila, e ele pisa nele;
 *   <li><b>pilar</b> — quebra o bloco acima da cabeça, pula e põe um bloco
 *       debaixo dos pés, como um jogador. O bloco é o que ele acabou de cavar;
 *       sem nada no bolso, cava um da parede ao lado, e por último usa
 *       pedregulho do baú;
 *   <li><b>túnel</b> — quando o que está acima não pode ser quebrado
 *       (construção, rocha-mãe, lava, areia que cairia nele), abre
 *       {@link #TUNNEL_LENGTH} blocos para o lado e tenta subir dali.
 * </ol>
 *
 * <p><b>Nunca desiste.</b> Esgotados os três, recomeça pela escada. Só o que
 * a {@code BlockProtection} protege não é quebrado (Regra 3); um aldeão
 * fechado por todos os lados nisso fica tentando, e o log diz onde, a cada
 * minuto. Nenhum teletransporte (decisão do autor no E47).
 *
 * <p><b>Os movimentos são dele, não da navegação.</b> Pisar no degrau e
 * entrar no túnel vão pelo controle de movimento; a navegação Vanilla fica
 * parada enquanto ele sobe — ela dava "não alcança" para o bloco ao lado.
 */
final class ClimbOut {

    static {
        ServerMemory.register(ClimbOut.class, ClimbOut::clearAll);
    }

    enum Mode { STAIRS, PILLAR, TUNNEL }

    /** Passagens (uma por segundo) sem subir antes de trocar de jeito. */
    static final int NO_GAIN_PASSES = 6;

    /** Quantos blocos o túnel anda para o lado antes de tentar subir de novo. */
    static final int TUNNEL_LENGTH = 3;

    /**
     * Quanto o túnel tem antes de dar lugar a outro jeito: até três passagens
     * por bloco (cavar os pés, cavar a cabeça, entrar). Com o prazo da escada
     * (6), o túnel era cortado no segundo bloco e nunca saía de baixo da vila
     * — sessão de 01-10, 23:36, 1.246 trocas de jeito em 41 minutos.
     */
    static final int TUNNEL_PASSES = TUNNEL_LENGTH * 3 + 2;

    /** Quantos blocos ele carrega para pôr debaixo dos pés. */
    private static final int POCKET_MAX = 16;

    /** O pulo do pilar: o topo fica a 1,7 bloco, e o bloco entra a 1. */
    private static final double JUMP_UP = 0.5;

    /** Quanto tempo ele tem para pisar no degrau ou entrar no túnel. */
    private static final int MOVE_TICKS = 40;

    private static final double MOVE_SPEED = 0.6;

    /** De quanto em quanto tempo o log repete que ele está fechado. */
    private static final long WARN_EVERY = 1_200;

    /** Uma subida em andamento. */
    static final class Climb {

        Mode mode = Mode.STAIRS;

        int bestY;

        int startY;

        int noGain;

        Item pocketItem;

        int pocket;

        BlockPos moveTo;

        long moveSince;

        BlockPos jumpFrom;

        long jumpedAt;

        Direction tunnelWay;

        int tunnelLeft;

        long warnedAt = Long.MIN_VALUE / 2;

        ColonyPos chest;

        /** Quantos níveis cada jeito subiu, na ordem de {@link Mode}. */
        final Map<Mode, Integer> upBy = new EnumMap<>(Mode.class);

        Climb(int y) {
            this.bestY = y;
            this.startY = y;
        }
    }

    private static final Map<UUID, Climb> CLIMBS = new HashMap<>();

    private ClimbOut() {
    }

    /** A cada tique: o pulo do pilar e o passo para o degrau ou o túnel. */
    static void tick(ServerWorld world) {
        if (CLIMBS.isEmpty()) {
            return;
        }

        long now = world.getTime();

        for (Map.Entry<UUID, Climb> entry : CLIMBS.entrySet()) {
            if (!(world.getEntity(entry.getKey()) instanceof VillagerEntity villager) || !villager.isAlive()
                    || PenEscape.isEscaping(entry.getKey())) {
                continue;
            }

            Climb climb = entry.getValue();

            // Parado enquanto sobe: a navegação Vanilla e este controle
            // disputariam o mesmo aldeão.
            villager.getBrain().forget(MemoryModuleType.WALK_TARGET);
            villager.getNavigation().stop();

            if (climb.jumpFrom != null) {
                airborne(world, villager, entry.getKey(), climb, now);
            } else if (climb.moveTo != null) {
                step(villager, climb, now);
            }
        }
    }

    /** Uma passagem: escolhe e faz a próxima ação da subida. */
    static void pass(ServerWorld world, VillagerEntity villager, UUID workerId, BlockPos home, ColonyPos chest) {
        BlockPos feet = villager.getBlockPos();
        Climb climb = CLIMBS.computeIfAbsent(workerId, id -> new Climb(feet.getY()));
        long now = world.getTime();

        climb.chest = chest;

        if (climb.jumpFrom != null || climb.moveTo != null || !villager.isOnGround()) {
            return;
        }

        if (feet.getY() > climb.bestY) {
            climb.upBy.merge(climb.mode, feet.getY() - climb.bestY, Integer::sum);
            climb.bestY = feet.getY();
            climb.noGain = 0;

            VillageColonyMod.LOGGER.info("Stranded worker {} climbed to {} by {} — {} level(s) up",
                    shortId(workerId), feet.toShortString(), climb.mode, climb.bestY - climb.startY);
        } else if (++climb.noGain >= (climb.mode == Mode.TUNNEL ? TUNNEL_PASSES : NO_GAIN_PASSES)) {
            switchMode(workerId, climb, feet, next(climb.mode), "no height gained in " + climb.noGain + " s");
        }

        switch (climb.mode) {
            case STAIRS -> stairs(world, villager, climb, workerId, feet, home, now);
            case PILLAR -> pillar(world, villager, climb, workerId, feet, now);
            case TUNNEL -> tunnel(world, villager, climb, workerId, feet, home, now);
        }
    }

    /**
     * O que sai do buraco e serve de bloco fica com ele, para o pilar.
     * Chamado pelo {@link StrandedEscape} ao cavar; devolve o que vai para o baú.
     */
    static ItemStack keep(UUID digger, ItemStack drop) {
        Climb climb = digger == null ? null : CLIMBS.get(digger);

        if (climb == null || !ClimbTerrain.isBuildingBlock(drop.getItem())
                || (climb.pocket > 0 && climb.pocketItem != drop.getItem())) {
            return drop;
        }

        int take = Math.min(drop.getCount(), POCKET_MAX - climb.pocket);

        if (take <= 0) {
            return drop;
        }

        climb.pocketItem = drop.getItem();
        climb.pocket += take;

        return drop.copyWithCount(drop.getCount() - take);
    }

    /** Em que pé está a subida, para o teste dizer onde parou. */
    static String stateOf(UUID workerId) {
        Climb climb = CLIMBS.get(workerId);

        return climb == null ? "none"
                : climb.mode + " best y " + climb.bestY + ", pocket " + climb.pocket + ", no gain " + climb.noGain
                        + ", up by stairs/pillar/tunnel " + climb.upBy.getOrDefault(Mode.STAIRS, 0) + "/"
                        + climb.upBy.getOrDefault(Mode.PILLAR, 0) + "/" + climb.upBy.getOrDefault(Mode.TUNNEL, 0);
    }

    /** Ele saiu: o que sobrou no bolso cai a seus pés, e a subida acaba. */
    static void finish(ServerWorld world, VillagerEntity villager, UUID workerId) {
        Climb climb = CLIMBS.remove(workerId);

        if (climb != null && climb.pocket > 0) {
            world.spawnEntity(new ItemEntity(world, villager.getX(), villager.getY() + 0.5, villager.getZ(),
                    new ItemStack(climb.pocketItem, climb.pocket)));
        }
    }

    static void forget(UUID workerId) {
        CLIMBS.remove(workerId);
    }

    static void clearAll() {
        CLIMBS.clear();
    }

    private static void stairs(ServerWorld world, VillagerEntity villager, Climb climb, UUID workerId,
            BlockPos feet, BlockPos home, long now) {

        Optional<StrandedEscape.Step> step = StrandedEscape.planStep(world, feet, home);

        if (step.isEmpty()) {
            switchMode(workerId, climb, feet, Mode.PILLAR, "no natural, dry step toward the village");
            pillar(world, villager, climb, workerId, feet, now);

            return;
        }

        if (!step.get().toBreak().isEmpty()) {
            StrandedEscape.dig(world, step.get(), climb.chest, feet, workerId);
            villager.swingHand(Hand.MAIN_HAND);
        }

        moveTo(climb, step.get().standAt(), now);
    }

    private static void pillar(ServerWorld world, VillagerEntity villager, Climb climb, UUID workerId,
            BlockPos feet, long now) {

        // Espaço para o pulo: a cabeça sobe até feet + 3.
        for (BlockPos above : List.of(feet.up(2), feet.up(3))) {
            if (ClimbTerrain.isOpen(world, above)) {
                continue;
            }

            if (ClimbTerrain.mayBreak(world, above)) {
                breakOne(world, villager, climb, workerId, above);
            } else if (above.equals(feet.up(2))) {
                switchMode(workerId, climb, feet, Mode.TUNNEL, "it may not break " + describe(world, above));
            } else {
                // O de cima de tudo fica: o pulo alcança 1 bloco mesmo assim.
                continue;
            }

            return;
        }

        if (climb.pocket == 0) {
            Optional<BlockPos> wall = ClimbTerrain.wallWithBlock(world, feet);

            if (wall.isPresent()) {
                breakOne(world, villager, climb, workerId, wall.get());

                return;
            }
        }

        villager.setVelocity(0.0, JUMP_UP, 0.0);
        villager.velocityModified = true;
        climb.jumpFrom = feet.toImmutable();
        climb.jumpedAt = now;
    }

    private static void tunnel(ServerWorld world, VillagerEntity villager, Climb climb, UUID workerId,
            BlockPos feet, BlockPos home, long now) {

        if (climb.tunnelWay == null) {
            climb.tunnelWay = ClimbTerrain.tunnelWay(world, feet, home).orElse(null);
            climb.tunnelLeft = TUNNEL_LENGTH;

            if (climb.tunnelWay == null) {
                if (now - climb.warnedAt >= WARN_EVERY) {
                    climb.warnedAt = now;
                    VillageColonyMod.LOGGER.warn(
                            "Stranded worker {} is boxed in at {} by blocks it may not break — it keeps"
                                    + " trying every few seconds",
                            shortId(workerId), feet.toShortString());
                }

                climb.mode = Mode.STAIRS;
                climb.noGain = 0;

                return;
            }
        }

        BlockPos ahead = feet.offset(climb.tunnelWay);

        for (BlockPos cell : List.of(ahead, ahead.up())) {
            if (!ClimbTerrain.isOpen(world, cell)) {
                if (ClimbTerrain.mayBreak(world, cell)) {
                    breakOne(world, villager, climb, workerId, cell);
                } else {
                    climb.tunnelWay = null;
                }

                return;
            }
        }

        moveTo(climb, ahead, now);

        if (--climb.tunnelLeft <= 0) {
            climb.tunnelWay = null;
            climb.mode = Mode.STAIRS;
            climb.noGain = 0;
        }
    }

    private static void airborne(ServerWorld world, VillagerEntity villager, UUID workerId, Climb climb, long now) {
        if (villager.getY() >= climb.jumpFrom.getY() + 1.0 && place(world, workerId, climb, climb.jumpFrom)) {
            climb.jumpFrom = null;

            return;
        }

        if (villager.isOnGround() && now - climb.jumpedAt > 4) {
            // Pousou sem o bloco: a próxima passagem vê que não subiu.
            climb.jumpFrom = null;

            return;
        }

        // Sobe reto, sem sair de cima do vão.
        Vec3d velocity = villager.getVelocity();
        Vec3d center = Vec3d.ofBottomCenter(climb.jumpFrom);

        villager.setVelocity((center.x - villager.getX()) * 0.3, velocity.y, (center.z - villager.getZ()) * 0.3);
        villager.velocityModified = true;
    }

    private static boolean place(ServerWorld world, UUID workerId, Climb climb, BlockPos at) {
        if (!world.getBlockState(at).isReplaceable()
                || !world.getEntitiesByClass(LivingEntity.class, new Box(at), LivingEntity::isAlive).isEmpty()) {
            return false;
        }

        boolean placed;

        if (climb.pocket > 0) {
            world.setBlockState(at, Block.getBlockFromItem(climb.pocketItem).getDefaultState());
            climb.pocket--;
            placed = true;
        } else {
            placed = climb.chest != null
                    && EscapeBackfill.placeFromChest(world, workerId, climb.chest, at, Items.COBBLESTONE);
        }

        if (placed) {
            ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(at));
        }

        return placed;
    }

    private static void step(VillagerEntity villager, Climb climb, long now) {
        Vec3d target = Vec3d.ofBottomCenter(climb.moveTo);
        double dx = target.x - villager.getX();
        double dz = target.z - villager.getZ();

        boolean arrived = dx * dx + dz * dz <= 0.35 * 0.35 && Math.abs(villager.getY() - target.y) < 0.5;

        if (arrived || now - climb.moveSince > MOVE_TICKS) {
            climb.moveTo = null;

            return;
        }

        villager.getMoveControl().moveTo(target.x, target.y, target.z, MOVE_SPEED);
    }

    private static void moveTo(Climb climb, BlockPos to, long now) {
        climb.moveTo = to.toImmutable();
        climb.moveSince = now;
    }

    private static void breakOne(ServerWorld world, VillagerEntity villager, Climb climb, UUID workerId, BlockPos at) {
        StrandedEscape.dig(world, new StrandedEscape.Step(at, List.of(at.toImmutable())), climb.chest,
                villager.getBlockPos(), workerId);
        villager.swingHand(Hand.MAIN_HAND);
    }

    private static Mode next(Mode mode) {
        return switch (mode) {
            case STAIRS -> Mode.PILLAR;
            case PILLAR -> Mode.TUNNEL;
            case TUNNEL -> Mode.STAIRS;
        };
    }

    private static void switchMode(UUID workerId, Climb climb, BlockPos feet, Mode to, String why) {
        VillageColonyMod.LOGGER.info("Stranded worker {} switches from {} to {} at {} — {}",
                shortId(workerId), climb.mode, to, feet.toShortString(), why);

        climb.mode = to;
        climb.noGain = 0;
        climb.tunnelWay = null;
    }

    private static String describe(ServerWorld world, BlockPos at) {
        return world.getBlockState(at).getBlock().getTranslationKey() + " at " + at.toShortString();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
