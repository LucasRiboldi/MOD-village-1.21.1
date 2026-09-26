package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ChestDepositor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O encalhado cava a própria saída — E47, 2026-09-24, decisão do autor.
 *
 * <p><b>Por que cavar, e não teletransportar.</b> O autor pediu a
 * jogabilidade mais natural: o jogador vê o aldeão abrir uma escada no
 * barranco e subir, em vez de sumir de um lugar e aparecer em outro. A
 * saída é uma escada de um bloco de largura, um degrau por vez, na direção
 * do centro da vila.
 *
 * <p><b>O que ele pode cavar, e só isso:</b> terreno natural — pedra,
 * terra, areia, cascalho, argila, minério —, e sempre perguntando a
 * {@link BlockProtection}. Construção da vila, da colônia ou do jogador
 * reconhecível fica de pé; é a Regra 3 valendo também para quem está preso.
 *
 * <p><b>O que faz ele desistir de um rumo:</b> água ou lava encostada no
 * que ia sair (abrir o buraco inundaria a escada), e areia ou cascalho solto
 * logo acima do vão (cairia em cima dele). Sem rumo possível, ele fica onde
 * está e o log diz por quê — o jogador pode resgatá-lo.
 *
 * <p><b>Nada se perde.</b> O que sai do buraco vai para o baú dele; o que não
 * couber cai no chão como item, como faz o mineiro.
 */
public final class StrandedEscape {

    static {
        ServerMemory.register(StrandedEscape.class, StrandedEscape::clearAll);
    }

    /** Quantos degraus uma fuga cava antes de desistir. */
    static final int MAX_STEPS = 32;

    /** A fuga anda uma vez por segundo — o aldeão precisa de tempo para subir. */
    private static final int PASS_EVERY = 20;

    /** Quantas passagens parado, sem nada novo a cavar, antes de desistir. */
    private static final int STILL_PASSES = 15;

    /** A distância das oito colunas que dizem se ele já está no nível do terreno. */
    private static final int OUT_RING = 3;

    /** Quantas das oito colunas precisam estar no nível dele para contar como fora. */
    private static final int OUT_MIN_LEVEL = 3;

    private static final Map<UUID, BlockPos> LAST_FEET = new HashMap<>();

    private static final Map<UUID, Integer> STILL = new HashMap<>();

    private static final Set<UUID> HOPELESS = new HashSet<>();

    private StrandedEscape() {
    }

    /** Uma passagem por segundo, para todos os encalhados. */
    public static void tick(ServerWorld world) {
        // O desvio anda todo tique: cavar e pisar não esperam a passagem de
        // um segundo. Ver walkDetours.
        StrandedDetours.walk(world);

        if (world.getTime() % PASS_EVERY != 0) {
            return;
        }

        for (UUID workerId : StrandedWorkers.all()) {
            pass(world, workerId);
        }

        // Quem já saiu tampa a escada, um bloco por passagem — N10.
        EscapeBackfill.tick(world);
    }

    private static void pass(ServerWorld world, UUID workerId) {
        Optional<Worker> worker = VillageColonyMod.WORKERS.find(workerId);

        if (worker.isEmpty()) {
            StrandedWorkers.forget(workerId);
            forget(workerId);
            EscapeBackfill.forget(workerId);

            return;
        }

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager) || !villager.isAlive()) {
            return;
        }

        BlockPos feet = villager.getBlockPos();

        if (isOut(world, feet)) {
            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} is out at {} after {} steps — back in the work queue",
                    workerId.toString().substring(0, 8),
                    feet.toShortString(),
                    StrandedWorkers.stepsDug(workerId));

            StrandedWorkers.release(workerId);
            forget(workerId);
            EscapeBackfill.begin(workerId,
                    VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition).orElse(null));

            return;
        }

        if (HOPELESS.contains(workerId) || StrandedDetours.isWalking(workerId)
                || !WorkHours.isWorkTime(world, villager)) {
            return;
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(worker.get().colonyId());

        if (colony.isEmpty()) {
            return;
        }

        BlockPos home = MinecraftTypeAdapter.toBlockPos(colony.get().center());
        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(workerId);

        Optional<Step> step = planStep(world, feet, home);

        if (step.isEmpty() && StrandedDetours.begin(world, workerId, feet, home)) {
            return;
        }

        if (step.isEmpty() || StrandedWorkers.stepsDug(workerId) >= MAX_STEPS
                || stillFor(workerId, feet) >= STILL_PASSES) {

            giveUp(workerId, feet, step.isEmpty()
                    ? "no natural, dry way up toward the village"
                    : "it dug " + StrandedWorkers.stepsDug(workerId) + " steps and is still down");

            return;
        }

        if (!step.get().toBreak().isEmpty()) {
            dig(world, step.get(), storage.map(WorkerStorage::chestPosition).orElse(null), feet,
                    workerId);
            villager.swingHand(Hand.MAIN_HAND);
            StrandedWorkers.dugAStep(workerId);
            STILL.remove(workerId);

            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} dug a step at {} toward the village ({} of {})",
                    workerId.toString().substring(0, 8),
                    step.get().standAt().toShortString(),
                    StrandedWorkers.stepsDug(workerId),
                    MAX_STEPS);
        }

        // Ele sobe andando: o degrau é o destino, e o Brain faz o resto.
        WorkTargets.set(workerId, step.get().standAt(), 0);
    }

    /**
     * Cava um degrau a partir de {@code feet} e devolve onde o aldeão fica de
     * pé depois dele. Sem baú, o que sai cai no chão.
     *
     * <p>Pública para o teste de jogo: é a decisão inteira sem o aldeão
     * precisar andar, que a arena da bateria não garante.
     */
    public static Optional<BlockPos> digOneStep(
            ServerWorld world, BlockPos feet, BlockPos home) {

        Optional<Step> step = planStep(world, feet, home);

        step.ifPresent(found -> dig(world, found, null, feet, null));

        return step.map(Step::standAt);
    }

    /**
     * O mesmo degrau, com o que sai indo para {@code chest} e o vão
     * guardado para o tampão de {@code digger} — ver {@link EscapeBackfill}.
     */
    public static Optional<BlockPos> digOneStep(
            ServerWorld world, BlockPos feet, BlockPos home, UUID digger, ColonyPos chest) {

        Optional<Step> step = planStep(world, feet, home);

        step.ifPresent(found -> dig(world, found, chest, feet, digger));

        return step.map(Step::standAt);
    }

    /**
     * Se ele já está no nível do terreno em volta.
     *
     * <p>Olha oito colunas a {@value #OUT_RING} blocos: fora é quando ao
     * menos {@value #OUT_MIN_LEVEL} delas não sobem mais de um bloco acima
     * dos pés dele. No fundo de um poço todas sobem; na encosta, metade
     * desce. Coluna de chunk descarregado não conta.
     */
    public static boolean isOut(ServerWorld world, BlockPos feet) {
        int level = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                int x = feet.getX() + dx * OUT_RING;
                int z = feet.getZ() + dz * OUT_RING;

                if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

                if (top <= feet.getY() + 1) {
                    level++;
                }
            }
        }

        return level >= OUT_MIN_LEVEL;
    }

    /** O degrau: onde ele fica de pé, e os blocos que saem para isso. */
    record Step(BlockPos standAt, List<BlockPos> toBreak) {
    }

    /**
     * O próximo degrau, do rumo mais direto para a vila ao menos direto.
     *
     * <p>Um degrau pede três vãos livres: a cabeça dele ao pular
     * ({@code feet + 2}), e o corpo inteiro no degrau novo. O piso do degrau
     * é o bloco à frente dos pés, e ele fica — é nele que se pisa.
     */
    static Optional<Step> planStep(ServerWorld world, BlockPos feet, BlockPos home) {
        int dx = home.getX() - feet.getX();
        int dz = home.getZ() - feet.getZ();

        List<Direction> ways = new ArrayList<>(Direction.Type.HORIZONTAL.stream().toList());
        ways.sort(Comparator.comparingInt(
                (Direction way) -> -(way.getOffsetX() * dx + way.getOffsetZ() * dz)));

        for (Direction way : ways) {
            BlockPos floor = feet.offset(way);
            BlockPos standAt = floor.up();

            if (world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()) {
                // À frente não há onde pisar: ou é vão aberto, ou é queda.
                continue;
            }

            List<BlockPos> space = List.of(feet.up(2), standAt, standAt.up());
            List<BlockPos> toBreak = new ArrayList<>();
            boolean possible = true;

            for (BlockPos at : space) {
                BlockState state = world.getBlockState(at);

                if (state.getCollisionShape(world, at).isEmpty()
                        && state.getFluidState().isEmpty()) {
                    continue;
                }

                if (!mayDig(world, at, state)) {
                    possible = false;
                    break;
                }

                toBreak.add(at);
            }

            if (possible && isSafe(world, toBreak, space)) {
                return Optional.of(new Step(standAt, toBreak));
            }
        }

        return Optional.empty();
    }

    /** Terreno natural, que a proteção deixa quebrar. */
    private static boolean mayDig(ServerWorld world, BlockPos at, BlockState state) {
        if (!state.getFluidState().isEmpty() || world.getBlockEntity(at) != null) {
            return false;
        }

        return WorldTerrain.isNaturalGround(state) && BlockProtection.mayBreak(world, at, state);
    }

    /**
     * Abrir estes vãos não inunda a escada nem derruba areia na cabeça dele.
     */
    private static boolean isSafe(ServerWorld world, List<BlockPos> toBreak, List<BlockPos> space) {
        for (BlockPos at : toBreak) {
            for (Direction side : Direction.values()) {
                if (!world.getFluidState(at.offset(side)).isEmpty()) {
                    return false;
                }
            }
        }

        for (BlockPos at : space) {
            BlockPos above = at.up();

            if (!space.contains(above)
                    && world.getBlockState(above).getBlock() instanceof FallingBlock) {
                return false;
            }
        }

        return true;
    }

    /** Quebra os vãos do degrau e guarda o que saiu. */
    private static void dig(
            ServerWorld world, Step step,
            ColonyPos chest, BlockPos dropAt, UUID digger) {

        // Uma picareta de ferro na tabela de loot: pedra dá pedregulho, e
        // minério dá o que daria nas mãos do mineiro. Sem ferramenta a pedra
        // não dropa nada, e a fuga destruiria o que tira do caminho.
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);

        for (BlockPos at : step.toBreak()) {
            BlockState state = world.getBlockState(at);
            List<ItemStack> drops = Block.getDroppedStacks(state, world, at, null, null, tool);

            world.breakBlock(at, false);

            if (digger != null) {
                EscapeBackfill.dug(digger, at, drops);
            }

            for (ItemStack drop : drops) {
                int left = chest == null
                        ? drop.getCount()
                        : ChestDepositor.deposit(world, chest, drop.getItem(), drop.getCount());

                if (left > 0) {
                    world.spawnEntity(new ItemEntity(
                            world,
                            dropAt.getX() + 0.5,
                            dropAt.getY() + 0.5,
                            dropAt.getZ() + 0.5,
                            new ItemStack(drop.getItem(), left)));
                }
            }
        }
    }

    private static int stillFor(UUID workerId, BlockPos feet) {
        BlockPos before = LAST_FEET.put(workerId, feet.toImmutable());

        if (feet.equals(before)) {
            return STILL.merge(workerId, 1, Integer::sum);
        }

        STILL.remove(workerId);

        return 0;
    }

    private static void giveUp(UUID workerId, BlockPos feet, String why) {
        if (HOPELESS.add(workerId)) {
            WorkTargets.clear(workerId);

            VillageColonyMod.LOGGER.warn(
                    "Stranded worker {} cannot dig out of {} — {}; it stays out of the"
                            + " work queue until someone frees it",
                    workerId.toString().substring(0, 8),
                    feet.toShortString(),
                    why);
        }
    }

    private static void forget(UUID workerId) {
        LAST_FEET.remove(workerId);
        STILL.remove(workerId);
        HOPELESS.remove(workerId);
        StrandedDetours.forget(workerId);
        WorkTargets.clear(workerId);
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        LAST_FEET.clear();
        STILL.clear();
        HOPELESS.clear();
        StrandedDetours.clearAll();
        EscapeBackfill.clearAll();
    }
}
