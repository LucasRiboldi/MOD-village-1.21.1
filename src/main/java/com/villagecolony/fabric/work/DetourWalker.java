package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.movement.DetourMoves;
import com.villagecolony.core.movement.DetourPlanner;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.MineFlooding;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Anda um desvio do {@link DetourPlanner}, cavando e pondo bloco — ADR-025,
 * fase 2.
 *
 * <p><b>Um passo de cada vez, e cada passo perguntado de novo.</b> Antes de
 * cada vizinho, as ações vêm do {@link DetourMoves} contra o mundo de agora: o
 * que o planejador viu pode ter mudado (água que chegou, jogador que pôs
 * bloco). Passo que deixou de ser possível encerra o desvio, e quem o pediu
 * decide o que fazer.
 *
 * <p><b>Primeiro abre, depois põe, depois anda.</b> Cavar leva o tempo da
 * ferramenta, como a picareta do mineiro ({@link BlockBreakTime}); pôr é um
 * tique. Andar é a navegação Vanilla, um bloco por vez — o vizinho já aberto e
 * com chão é o caminho mais curto que ela conhece.
 *
 * <p><b>Nada se perde, e o material vem da colônia.</b> O que sai vai para o
 * baú do trabalhador (ou cai no chão, se não couber); o pedregulho que se põe
 * sai do mesmo baú. Sem pedregulho no baú, a ponte não se faz e o desvio
 * encerra com o motivo escrito.
 */
public final class DetourWalker {

    /** O que o passo deste tique deu. */
    public enum Status { WALKING, DONE, FAILED }

    /** Quanto se espera o aldeão chegar ao vizinho já aberto e com chão. */
    static final int STEP_TICKS = 100;

    private static final ItemStack LOOT_TOOL = new ItemStack(Items.IRON_PICKAXE);

    private final UUID workerId;

    private final List<ColonyPos> steps;

    private final Set<ColonyPos> keep;

    private final boolean reachesGoal;

    private ColonyPos from;

    private int index;

    private BlockPos breaking;

    private int progress;

    private int required;

    private int ticksOnStep;

    private int dug;

    private int placed;

    private String why = "";

    private DetourWalker(UUID workerId, ColonyPos from, DetourPlanner.Detour detour, Set<ColonyPos> keep) {
        this.workerId = workerId;
        this.from = from;
        this.steps = detour.steps();
        this.keep = keep;
        this.reachesGoal = detour.reachesGoal();
    }

    /**
     * Planeja o desvio de {@code feet} até algum lugar que satisfaça {@code goal}.
     *
     * @param keep blocos que o desvio não cava nem ocupa
     * @param partial se aceita o trecho que mais se aproxima quando não chega
     */
    public static Optional<DetourWalker> plan(
            ServerWorld world, UUID workerId, BlockPos feet, BlockPos toward,
            Predicate<BlockPos> goal, Set<BlockPos> keep, boolean partial) {

        ColonyPos start = MinecraftTypeAdapter.toColonyPos(feet);
        Set<ColonyPos> kept = keep.stream().map(MinecraftTypeAdapter::toColonyPos).collect(Collectors.toSet());

        return DetourPlanner.plan(
                        new WorldTerrain(world),
                        start,
                        MinecraftTypeAdapter.toColonyPos(toward),
                        at -> goal.test(MinecraftTypeAdapter.toBlockPos(at)),
                        kept,
                        partial)
                .filter(detour -> !detour.steps().isEmpty())
                .map(detour -> new DetourWalker(workerId, start, detour, kept));
    }

    /** Um tique do desvio. */
    public Status tick(ServerWorld world, VillagerEntity villager, ColonyPos chest) {
        if (index >= steps.size()) {
            return Status.DONE;
        }

        ColonyPos step = steps.get(index);

        if (MinecraftTypeAdapter.toColonyPos(villager.getBlockPos()).equals(step)) {
            from = step;
            index++;
            ticksOnStep = 0;

            return index >= steps.size() ? Status.DONE : Status.WALKING;
        }

        Optional<DetourMoves.Actions> actions =
                DetourMoves.actions(new WorldTerrain(world), from, step, keep);

        if (actions.isEmpty()) {
            return fail("the step to " + step.x() + ", " + step.y() + ", " + step.z()
                    + " is no longer possible");
        }

        if (!actions.get().dig().isEmpty()) {
            return dig(world, villager, MinecraftTypeAdapter.toBlockPos(actions.get().dig().get(0)), chest);
        }

        if (!actions.get().place().isEmpty()) {
            return place(world, villager, MinecraftTypeAdapter.toBlockPos(actions.get().place().get(0)), chest);
        }

        if (++ticksOnStep > STEP_TICKS) {
            return fail("it did not step onto " + step.x() + ", " + step.y() + ", " + step.z()
                    + " in " + STEP_TICKS + " ticks");
        }

        WorkTargets.set(workerId, MinecraftTypeAdapter.toBlockPos(step), 0);

        return Status.WALKING;
    }

    private Status dig(ServerWorld world, VillagerEntity villager, BlockPos at, ColonyPos chest) {
        BlockState state = world.getBlockState(at);

        if (!at.equals(breaking)) {
            breaking = at.toImmutable();
            progress = 0;
            required = BlockBreakTime.ticksFor(world, at, state, villager);
        }

        if (required == BlockBreakTime.NEVER) {
            return fail("it cannot break " + at.toShortString() + " with what it holds");
        }

        progress++;

        if (progress % MinerWork.SWING_INTERVAL == 1) {
            villager.swingHand(Hand.MAIN_HAND);
        }

        if (progress < required) {
            world.setBlockBreakingInfo(villager.getId(), at, progress * MinerWork.BREAKING_STAGES / required);

            return Status.WALKING;
        }

        world.setBlockBreakingInfo(villager.getId(), at, -1);

        List<ItemStack> drops = Block.getDroppedStacks(state, world, at, null, null, LOOT_TOOL);

        ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(at));
        world.removeBlock(at, false);

        // A rede do MineFlooding: o passo só cava o que não encosta em líquido,
        // mas a água pode ter chegado entre a pergunta e a última batida.
        MineFlooding.seal(world, at);

        for (ItemStack drop : drops) {
            int left = chest == null
                    ? drop.getCount()
                    : ChestDepositor.deposit(world, chest, drop.getItem(), drop.getCount());

            if (left > 0) {
                world.spawnEntity(new ItemEntity(
                        world, villager.getX(), villager.getY() + 0.5, villager.getZ(),
                        new ItemStack(drop.getItem(), left)));
            }
        }

        breaking = null;
        ticksOnStep = 0;
        dug++;

        return Status.WALKING;
    }

    private Status place(ServerWorld world, VillagerEntity villager, BlockPos at, ColonyPos chest) {
        BlockState block = Blocks.COBBLESTONE.getDefaultState();

        if (!world.canPlace(block, at, ShapeContext.absent())) {
            // Alguém de pé no vão. Espera, no mesmo prazo de quem espera andar.
            return ++ticksOnStep > STEP_TICKS
                    ? fail("something stood in " + at.toShortString() + " for " + STEP_TICKS + " ticks")
                    : Status.WALKING;
        }

        if (chest == null || ChestWithdrawer.withdraw(world, chest, Items.COBBLESTONE, 1) < 1) {
            return fail("no cobblestone in its chest to place at " + at.toShortString());
        }

        ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(at));
        world.setBlockState(at, block);
        villager.swingHand(Hand.MAIN_HAND);

        ticksOnStep = 0;
        placed++;

        return Status.WALKING;
    }

    private Status fail(String reason) {
        why = reason;

        return Status.FAILED;
    }

    /** Por que o desvio parou, quando parou. */
    public String why() {
        return why;
    }

    public int steps() {
        return steps.size();
    }

    /** Os pés planejados, passo a passo — para o log e para o teste. */
    public String path() {
        return steps.stream().map(step -> step.x() + "," + step.y() + "," + step.z())
                .collect(Collectors.joining(" > "));
    }

    public boolean reachesGoal() {
        return reachesGoal;
    }

    public int dug() {
        return dug;
    }

    public int placed() {
        return placed;
    }
}
