package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.event.VillageFocus;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
 * logo acima do vão (cairia em cima dele). Desistir de um rumo não é
 * desistir de sair: sem degrau, o {@link ClimbOut} sobe em pilar ou abre
 * túnel para o lado, e recomeça — 2026-10-01, pedido do autor: <i>"nunca
 * ficar preso"</i>.
 *
 * <p><b>Nada se perde.</b> O que sai do buraco vai para o baú dele; o que não
 * couber cai no chão como item, como faz o mineiro.
 */
public final class StrandedEscape {

    static {
        ServerMemory.register(StrandedEscape.class, StrandedEscape::clearAll);
    }

    /** A fuga anda uma vez por segundo — o aldeão precisa de tempo para subir. */
    private static final int PASS_EVERY = 20;

    /** A distância das oito colunas que dizem se ele já está no nível do terreno. */
    private static final int OUT_RING = 3;

    /** Quantas das oito colunas precisam estar no nível dele para contar como fora. */
    private static final int OUT_MIN_LEVEL = 3;

    private StrandedEscape() {
    }

    /** Uma passagem por segundo, para todos os encalhados. */
    public static void tick(ServerWorld world) {
        // Quem está num curral sai pelo portão ou pula a cerca, sem esperar
        // ser marcado encalhado — E52, 2026-10-01. Ver PenEscape.
        PenEscape.tick(world);

        // Quem foi marcado larga o trabalho, fora do laço do ofício que o
        // marcou; e quem sobe pula e pisa a cada tique — ver ClimbOut.
        StrandedWorkers.dropMarkedJobs();
        ClimbOut.tick(world);

        if (world.getTime() % PASS_EVERY != 0) {
            return;
        }

        // O rastro de quem anda solto: o caminho de volta, se ele encalhar.
        MineReturn.record(world);

        // E o tempo de cada um — Regra 50. Ver WorkTime.
        WorkTime.sample(world);

        // Quem está à toa recolhe do chão o que a obra espera — B-1, Regra 48.
        GroundPickup.pass(world);

        // A vila com jogador dentro não espera o ciclo para continuar a busca
        // de lote — estudo de 01-10, §7-A. Ver SweepCadence.
        SweepCadence.pass(world);

        for (UUID workerId : StrandedWorkers.all()) {
            pass(world, workerId);
        }

        // Quem já saiu tampa a escada, um bloco por passagem — N10.
        EscapeBackfill.tick(world);

        // E o portão aberto para sair do cercado fecha de novo — E52.
        FencedIn.tick(world);
    }

    static void pass(ServerWorld world, UUID workerId) {
        Optional<Worker> worker = VillageColonyMod.WORKERS.find(workerId);

        if (worker.isEmpty()) {
            StrandedWorkers.forget(workerId);
            forget(workerId);
            EscapeBackfill.forget(workerId);
            MineReturn.forget(workerId);

            return;
        }

        if (!VillageFocus.isWorking(world, worker.get().colonyId())) {
            return;
        }

        if (!(world.getEntity(workerId) instanceof VillagerEntity villager) || !villager.isAlive()) {
            return;
        }

        BlockPos feet = villager.getBlockPos();

        // <b>No nível do chão não é o mesmo que solto</b> — E52, 2026-10-01.
        // Dentro de um curral ele está no nível do terreno em volta, e a fuga
        // o devolvia à escala "after 0 steps", de volta à cerca, por horas.
        // Enquanto ele sai do curral, a fuga espera; quando sair, esta mesma
        // passagem o vê fora e o devolve à escala.
        if (PenEscape.check(world, villager)) {
            return;
        }

        if (isOut(world, feet)) {
            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} is out at {} after {} steps — back in the work queue",
                    workerId.toString().substring(0, 8),
                    feet.toShortString(),
                    StrandedWorkers.stepsDug(workerId));

            StrandedWorkers.release(workerId);
            ClimbOut.finish(world, villager, workerId);
            MineReturn.finish(workerId);
            forget(workerId);
            EscapeBackfill.begin(workerId,
                    VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition).orElse(null));

            return;
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(worker.get().colonyId());

        if (colony.isEmpty()) {
            return;
        }

        // A qualquer hora: sair do buraco não é trabalho, e preso ele não
        // chega à cama. Ver ClimbOut — primeiro o caminho por onde desceu
        // (MineReturn); sem ele, escada, pilar ou túnel, mirando a borda da
        // vila e não o centro dela, sem desistir.
        ClimbOut.pass(world, villager, workerId, MineReturn.homeFor(colony.get(), feet),
                VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition).orElse(null));
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
    static boolean mayDig(ServerWorld world, BlockPos at, BlockState state) {
        if (!state.getFluidState().isEmpty() || world.getBlockEntity(at) != null) {
            return false;
        }

        // A casca da vila não se fura subindo — pedido do autor, 2026-10-02.
        return (WorldTerrain.isNaturalGround(state) || isRubbleUnderground(world, at, state))
                && BlockProtection.mayDigOut(world, at, state)
                && !MineReturn.isVillageShell(world, at);
    }

    /** Quantos blocos abaixo da superfície o pedregulho conta como entulho da mina. */
    static final int RUBBLE_DEPTH = 4;

    /**
     * Pedregulho no subsolo — playtest de 2026-10-02.
     *
     * <p>O mineiro 199ad062 subiu 29 níveis e parou 15 minutos em y=39,
     * "boxed in", debaixo de um pedregulho que a própria colônia pôs na mina
     * (o aterro e o pilar são de pedregulho). Pedregulho não é terreno natural,
     * e a fuga não o quebrava. Debaixo da terra ele é entulho; perto da
     * superfície pode ser parede de alguém, e fica.
     */
    static boolean isRubbleUnderground(ServerWorld world, BlockPos at, BlockState state) {
        boolean rubble = state.isOf(net.minecraft.block.Blocks.COBBLESTONE)
                || state.isOf(net.minecraft.block.Blocks.COBBLED_DEEPSLATE)
                || state.isOf(net.minecraft.block.Blocks.MOSSY_COBBLESTONE);

        return rubble
                && world.getTopY(Heightmap.Type.WORLD_SURFACE, at.getX(), at.getZ()) - at.getY() > RUBBLE_DEPTH;
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
    static void dig(
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

            for (ItemStack dug : drops) {
                // O que serve de bloco fica com quem sobe, para o pilar.
                ItemStack drop = ClimbOut.keep(digger, dug);

                if (drop.isEmpty()) {
                    continue;
                }

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

    private static void forget(UUID workerId) {
        ClimbOut.forget(workerId);
        WorkTargets.clear(workerId);
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        ClimbOut.clearAll();
        EscapeBackfill.clearAll();
    }
}
