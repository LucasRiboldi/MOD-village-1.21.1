package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BenchPresence;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Set;

/**
 * As passivas que a bancada faz só por estar na vila — Regra 132 (autor, 2026-10-27). Uma passagem por
 * segundo, cada efeito no seu ritmo, só nas vilas ativas com jogador perto:
 * <ul>
 *   <li><b>carpinteiro e fundidor:</b> repara, a até {@link #REPAIR_REACH}, a ferramenta (carpinteiro) e a
 *       armadura (fundidor) de quem está perto, um ponto por segundo;</li>
 *   <li><b>lenhador:</b> mudas a até {@link #GROVE_REACH} recebem um passo de crescimento;</li>
 *   <li><b>fazendeiro:</b> a roça a até {@link #FIELD_REACH} cresce um passo a mais; a sobra dos baús
 *       (semente acima da reserva, mudas e folhas) vira farinha de osso, {@link #COMPOST_PER_MEAL} por uma;</li>
 *   <li><b>pastor:</b> a até {@link #FLOCK_REACH}, um par abaixo da meta cruza sozinho a cada 10 min, a
 *       ovelha tosquiada recupera a lã mais depressa e o que os animais derrubam vai aos baús;</li>
 *   <li><b>construtor:</b> a obra a até {@link #BUILD_REACH} assenta um bloco a mais por minuto
 *       ({@link #hurry}).</li>
 * </ul>
 */
public final class BenchPassives {

    static final int REPAIR_REACH = 8;
    static final int GROVE_REACH = 32;
    static final int FIELD_REACH = 24;
    static final int FLOCK_REACH = 36;
    static final int BUILD_REACH = 48;

    /** Quantas sobras viram uma farinha de osso, como na composteira (em média ~7). */
    static final int COMPOST_PER_MEAL = 7;

    /** Colunas sorteadas por passagem, em volta de cada bancada, para crescimento. */
    static final int SAMPLES = 24;

    static final Set<Item> ANIMAL_DROPS = Set.of(Items.FEATHER, Items.EGG, Items.LEATHER, Items.RABBIT_HIDE,
            Items.RABBIT_FOOT, Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN, Items.RABBIT);

    private BenchPassives() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long now = world.getTime();

            if (now % 20 != 0) {
                return;
            }

            for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
                if (colony.isActive() && colony.isAttended(now)) {
                    pass(world, colony, now);
                }
            }
        });
    }

    static void pass(ServerWorld world, Colony colony, long now) {
        repair(world, colony, ProfessionType.CARPENTER, false);
        repair(world, colony, ProfessionType.SMELTER, true);

        if (now % 600 == 0) {
            grow(world, colony, ProfessionType.LUMBERJACK, GROVE_REACH, true);
            grow(world, colony, ProfessionType.FARMER, FIELD_REACH, false);
            woolBack(world, colony);
        }

        if (now % 100 == 0) {
            gatherDrops(world, colony);
        }

        if (now % 1200 == 0) {
            compost(world, colony);
            hurryTheBuild(world, colony);
        }

        if (now % 12_000 == 0) {
            breed(world, colony);
        }
    }

    /** Um ponto por segundo na ferramenta (ou na armadura) de quem está perto da bancada. */
    static void repair(ServerWorld world, Colony colony, ProfessionType profession, boolean armor) {
        for (BlockPos bench : BenchPresence.of(world, colony, profession)) {
            for (LivingEntity living : world.getEntitiesByClass(LivingEntity.class, new Box(bench).expand(REPAIR_REACH),
                    LivingEntity::isAlive)) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    mend(living.getEquippedStack(slot), armor);
                }

                if (living instanceof net.minecraft.entity.player.PlayerEntity player) {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        mend(player.getInventory().getStack(i), armor);
                    }
                }
            }
        }
    }

    static void mend(ItemStack stack, boolean armor) {
        if (stack.isEmpty() || !stack.isDamaged() || (stack.getItem() instanceof ArmorItem) != armor) {
            return;
        }

        stack.setDamage(stack.getDamage() - 1);
    }

    /** Um passo de crescimento em mudas (lenhador) ou em plantações (fazendeiro) sorteadas perto da bancada. */
    static int grow(ServerWorld world, Colony colony, ProfessionType profession, int reach, boolean saplings) {
        int grown = 0;

        for (BlockPos bench : BenchPresence.of(world, colony, profession)) {
            for (int sample = 0; sample < SAMPLES; sample++) {
                int x = bench.getX() + world.getRandom().nextBetween(-reach, reach);
                int z = bench.getZ() + world.getRandom().nextBetween(-reach, reach);

                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));

                for (BlockPos at : List.of(top, top.down())) {
                    if (growAt(world, at, saplings)) {
                        grown++;
                        break;
                    }
                }
            }
        }

        return grown;
    }

    static boolean growAt(ServerWorld world, BlockPos at, boolean saplings) {
        BlockState state = world.getBlockState(at);

        if (saplings && state.isIn(BlockTags.SAPLINGS) && state.getBlock() instanceof Fertilizable plant
                && plant.isFertilizable(world, at, state)) {
            plant.grow(world, world.getRandom(), at, state);
            return true;
        }

        if (!saplings && state.getBlock() instanceof CropBlock crop && !crop.isMature(state)) {
            world.setBlockState(at, crop.withAge(crop.getAge(state) + 1), net.minecraft.block.Block.NOTIFY_LISTENERS);
            return true;
        }

        return false;
    }

    /** A sobra dos baús vira farinha de osso, como na composteira. */
    static int compost(ServerWorld world, Colony colony) {
        if (!BenchPresence.has(world, colony, ProfessionType.FARMER)) {
            return 0;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        int spare = 0;

        for (Item seed : List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS)) {
            int above = ColonyChests.countIn(world, chests, seed) - 4 * ShepherdFlock.SEEDS_FOR_SOWING;

            if (above >= COMPOST_PER_MEAL) {
                spare += ColonyChests.withdraw(world, chests, seed, above / COMPOST_PER_MEAL * COMPOST_PER_MEAL);
            }
        }

        for (Item leaf : net.minecraft.registry.Registries.ITEM) {
            if (leaf.getDefaultStack().isIn(ItemTags.LEAVES)) {
                int have = ColonyChests.countIn(world, chests, leaf);

                if (have >= COMPOST_PER_MEAL) {
                    spare += ColonyChests.withdraw(world, chests, leaf, have / COMPOST_PER_MEAL * COMPOST_PER_MEAL);
                }
            }
        }

        int meal = spare / COMPOST_PER_MEAL;

        if (meal > 0 && !chests.isEmpty()) {
            ChestDepositor.deposit(world, chests.getFirst(), Items.BONE_MEAL, meal);
            VillageColonyMod.LOGGER.info("Colony {} composted {} spare items into {} bone meal (Regra 132)",
                    colony.id().toString().substring(0, 8), spare, meal);
        }

        return meal;
    }

    /** A ovelha tosquiada perto da bancada do pastor recupera a lã mais depressa. */
    static void woolBack(ServerWorld world, Colony colony) {
        for (BlockPos bench : BenchPresence.of(world, colony, ProfessionType.SHEPHERD)) {
            for (SheepEntity sheep : world.getEntitiesByClass(SheepEntity.class, new Box(bench).expand(FLOCK_REACH),
                    SheepEntity::isSheared)) {
                if (world.getRandom().nextInt(4) == 0) {
                    sheep.setSheared(false);
                }
            }
        }
    }

    /** O que os animais derrubam perto da bancada do pastor vai para os baús. */
    static void gatherDrops(ServerWorld world, Colony colony) {
        List<BlockPos> benches = BenchPresence.of(world, colony, ProfessionType.SHEPHERD);

        if (benches.isEmpty()) {
            return;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        for (BlockPos bench : benches) {
            for (ItemEntity drop : world.getEntitiesByClass(ItemEntity.class, new Box(bench).expand(FLOCK_REACH),
                    entity -> entity.isAlive() && isAnimalDrop(entity.getStack()))) {
                ItemStack stack = drop.getStack();
                int left = stack.getCount();

                for (ColonyPos chest : chests) {
                    if (left <= 0) {
                        break;
                    }

                    left = ChestDepositor.deposit(world, chest, stack.getItem(), left);
                }

                if (left <= 0) {
                    drop.discard();
                } else {
                    stack.setCount(left);
                }
            }
        }
    }

    static boolean isAnimalDrop(ItemStack stack) {
        return ANIMAL_DROPS.contains(stack.getItem()) || stack.isIn(ItemTags.WOOL);
    }

    /** Abaixo da meta, um par perto da bancada do pastor cruza sozinho, sem gastar comida. */
    static boolean breed(ServerWorld world, Colony colony) {
        for (BlockPos bench : BenchPresence.of(world, colony, ProfessionType.SHEPHERD)) {
            for (var species : ShepherdFlock.SPECIES) {
                List<? extends AnimalEntity> adults = world.getEntitiesByType(species, new Box(bench).expand(FLOCK_REACH),
                        animal -> animal.isAlive() && !animal.isBaby());

                if (adults.size() >= ShepherdFlock.FLOCK_TARGET) {
                    continue;
                }

                List<? extends AnimalEntity> ready = adults.stream()
                        .filter(animal -> animal.getBreedingAge() == 0 && !animal.isInLove()).limit(2).toList();

                if (ready.size() == 2) {
                    ready.forEach(animal -> animal.lovePlayer(null));
                    VillageColonyMod.LOGGER.info("Colony {} — a pair of {} bred by the shepherd bench (Regra 132)",
                            colony.id().toString().substring(0, 8), species.getUntranslatedName());
                    return true;
                }
            }
        }

        return false;
    }

    /** A obra a até {@link #BUILD_REACH} da bancada do construtor assenta um bloco a mais por minuto. */
    static void hurryTheBuild(ServerWorld world, Colony colony) {
        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).ifPresent(project -> {
            BlockPos origin = MinecraftTypeAdapter.toBlockPos(project.origin());

            if (BenchPresence.near(world, colony, ProfessionType.BUILDER, origin, BUILD_REACH)) {
                hurry(project.id());
            }
        });
    }

    /** O construtor desta obra põe o bloco da vez no tique seguinte, se estiver ao alcance dele. */
    static void hurry(java.util.UUID projectId) {
        BuilderWork.JOBS.values().stream()
                .filter(job -> job.projectId.equals(projectId))
                .forEach(job -> job.progress = Math.max(job.progress, BuilderWork.TICKS_PER_BLOCK - 1));
    }
}
