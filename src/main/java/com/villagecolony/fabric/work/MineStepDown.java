package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.MineFlooding;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A descida cavada — Regra 336, ADR-061. O mineiro só pisa no degrau já aberto; o próximo, ele cava de pé no último,
 * no formato da escada da mina (Regra 119): {@value #LANES} pistas de largura, à esquerda de quem desce, por
 * {@value #HEADROOM} de altura, um bloco à frente e um abaixo, cortado de cima para baixo. Vai rumo à pedra pelo eixo
 * mais longo; em cima dela, desce em caracol, virando à direita a cada {@value #FLIGHT} degraus. Com degrau cavado,
 * nunca vira à esquerda nem volta, porque ali ficam as pistas abertas. O degrau sem chão embaixo (vão de caverna)
 * ganha piso de pedra, só dentro da montanha (Regra 71). Lado com bloco protegido, líquido ou bloco que segura líquido
 * não se cava: tenta a direita.
 */
final class MineStepDown {

    enum Status { DIGGING, DONE, FAILED }

    static final int LANES = MineShaft.STAIR_LANES;

    static final int HEADROOM = MineShaft.STAIR_HEADROOM;

    static final int FLIGHT = MineShaft.HELIX_SIDE;

    /** A até esta distância na horizontal da pedra, ele desce em caracol em vez de ir na direção dela. */
    static final int ALIGNED = 2;

    /** Tiques para pisar no degrau aberto. */
    static final int STEP_TICKS = 100;

    private static final ItemStack LOOT_TOOL = new ItemStack(Items.IRON_PICKAXE);

    /** Um degrau planejado: o lado, onde os pés ficam, as células a abrir (de cima para baixo) e o chão das pistas. */
    private record Step(Direction side, BlockPos next, List<BlockPos> cells, List<BlockPos> floors) {
    }

    private final BlockPos aim;

    private BlockPos feet;

    private Direction side;

    private int inFlight;

    @Nullable
    private Step step;

    @Nullable
    private BlockPos breaking;

    private int progress;

    private int required;

    private int ticksOnStep;

    private int steps;

    private int dug;

    private int placed;

    private String why = "";

    private MineStepDown(BlockPos feet, BlockPos aim, Direction side) {
        this.feet = feet.toImmutable();
        this.aim = aim.toImmutable();
        this.side = side;
    }

    /**
     * A descida cavada de {@code feet} rumo a {@code aim}, começando por {@code preferred}.
     *
     * @return vazio quando ele já está na altura da pedra ou nenhum lado deixa cavar o degrau
     */
    static Optional<MineStepDown> start(ServerWorld world, BlockPos feet, BlockPos aim, Direction preferred) {
        MineStepDown descent = new MineStepDown(feet, aim,
                preferred.getAxis().isHorizontal() ? preferred : Direction.EAST);

        if (descent.arrived()) {
            return Optional.empty();
        }

        descent.step = descent.plan(world);

        return descent.step == null ? Optional.empty() : Optional.of(descent);
    }

    /** O degrau que ele não alcança andando: o alvo é o próprio pé, fica embaixo dele ou dentro da rocha. */
    static boolean cannotWalkTo(ServerWorld world, BlockPos feet, BlockPos target) {
        boolean underFoot = Math.abs(target.getX() - feet.getX()) <= 1 && Math.abs(target.getZ() - feet.getZ()) <= 1
                && target.getY() <= feet.getY();

        return underFoot || (world.isChunkLoaded(target) && !BuilderApproach.standable(world, target));
    }

    /** O lado de quem desce rumo a {@code aim} a partir de {@code feet}: o eixo mais longo. */
    static Direction heading(BlockPos feet, BlockPos aim, Direction otherwise) {
        int dx = aim.getX() - feet.getX();
        int dz = aim.getZ() - feet.getZ();

        if (Math.max(Math.abs(dx), Math.abs(dz)) <= ALIGNED) {
            return otherwise;
        }

        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }

        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Um tique: cava a próxima célula do degrau, põe o piso que falta ou leva o mineiro até ele. */
    Status tick(ServerWorld world, VillagerEntity villager, @Nullable ColonyPos chest) {
        if (step == null) {
            if (arrived()) {
                return Status.DONE;
            }

            step = plan(world);

            if (step == null) {
                return fail("no side lets it dig the next step down from " + feet.toShortString());
            }
        }

        for (BlockPos cell : step.cells()) {
            if (!world.getBlockState(cell).isAir()) {
                return dig(world, villager, cell, chest);
            }
        }

        for (BlockPos floor : step.floors()) {
            if (needsFloor(world, floor)) {
                return floor(world, villager, floor, chest);
            }
        }

        if (onTheStep(villager.getBlockPos())) {
            feet = step.next();
            side = step.side();
            inFlight++;
            steps++;
            step = null;
            ticksOnStep = 0;

            return arrived() || MinerWork.isWithinReach(villager, aim) ? Status.DONE : Status.DIGGING;
        }

        if (++ticksOnStep > STEP_TICKS) {
            return fail("it did not step down onto " + step.next().toShortString() + " in " + STEP_TICKS + " ticks");
        }

        WorkTargets.set(villager.getUuid(), step.next(), 0);

        return Status.DIGGING;
    }

    /** De pé no degrau novo, em qualquer das pistas: o degrau conta pela pista de dentro, a de quem guia. */
    private boolean onTheStep(BlockPos at) {
        if (at.getY() != step.next().getY()) {
            return false;
        }

        Direction across = step.side().rotateYCounterclockwise();

        for (int lane = 0; lane < LANES; lane++) {
            if (at.equals(step.next().offset(across, lane))) {
                return true;
            }
        }

        return false;
    }

    private boolean arrived() {
        return feet.getY() <= aim.getY() + 1;
    }

    /** O próximo degrau: rumo à pedra, ou, em cima dela, o caracol; sem saída, os lados à direita, nunca o de volta. */
    @Nullable
    private Step plan(ServerWorld world) {
        boolean aligned = Math.max(Math.abs(aim.getX() - feet.getX()), Math.abs(aim.getZ() - feet.getZ())) <= ALIGNED;

        if (aligned && inFlight >= FLIGHT) {
            side = side.rotateYClockwise();
            inFlight = 0;
        }

        // Com degrau cavado, só em frente ou à direita: à esquerda e para trás ficam as pistas já abertas.
        List<Direction> sides = steps == 0
                ? List.of(heading(feet, aim, side), side, side.rotateYClockwise(), side.getOpposite(),
                        side.rotateYCounterclockwise())
                : heading(feet, aim, side) == side.rotateYClockwise()
                        ? List.of(side.rotateYClockwise(), side)
                        : List.of(side, side.rotateYClockwise());

        for (Direction trying : sides) {
            Optional<Step> candidate = build(world, trying);

            if (candidate.isPresent()) {
                if (trying != side) {
                    inFlight = 0;
                }

                return candidate.get();
            }
        }

        return null;
    }

    private Optional<Step> build(ServerWorld world, Direction toward) {
        BlockPos next = feet.offset(toward).down();
        // As pistas ficam à esquerda de quem desce, como no caracol (Regra 119): a virada à direita entra em rocha nova.
        Direction across = toward.rotateYCounterclockwise();
        List<BlockPos> cells = new ArrayList<>(LANES * HEADROOM);
        List<BlockPos> floors = new ArrayList<>(LANES);

        for (int height = HEADROOM - 1; height >= 0; height--) {
            for (int lane = 0; lane < LANES; lane++) {
                BlockPos cell = next.offset(across, lane).up(height);

                if (!mayOpen(world, cell)) {
                    return Optional.empty();
                }

                cells.add(cell);
            }
        }

        for (int lane = 0; lane < LANES; lane++) {
            BlockPos floor = next.offset(across, lane).down();
            BlockState state = world.getBlockState(floor);

            if (!state.getFluidState().isEmpty() || (lane == 0 && needsFloor(world, floor) && !mayFloor(world, floor))) {
                return Optional.empty();
            }

            floors.add(floor);
        }

        return Optional.of(new Step(toward, next, cells, floors));
    }

    private static boolean mayOpen(ServerWorld world, BlockPos cell) {
        if (!world.isChunkLoaded(cell)) {
            return false;
        }

        BlockState state = world.getBlockState(cell);

        if (state.isAir()) {
            return true;
        }

        return state.getFluidState().isEmpty() && BlockProtection.mayBreak(world, cell, state)
                && !MineFlooding.holdsBackFluid(world, cell);
    }

    private static boolean needsFloor(ServerWorld world, BlockPos floor) {
        return !world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP);
    }

    /** Piso só onde o vão é vazio e fica dentro da montanha (Regra 71): ao céu aberto, nenhum bloco. */
    private static boolean mayFloor(ServerWorld world, BlockPos floor) {
        BlockState state = world.getBlockState(floor);

        return state.isReplaceable() && state.getFluidState().isEmpty()
                && floor.getY() < world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, floor.getX(), floor.getZ());
    }

    private Status dig(ServerWorld world, VillagerEntity villager, BlockPos at, @Nullable ColonyPos chest) {
        BlockState state = world.getBlockState(at);

        if (!at.equals(breaking)) {
            if (!mayOpen(world, at)) {
                step = null;

                return Status.DIGGING;
            }

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

            return Status.DIGGING;
        }

        world.setBlockBreakingInfo(villager.getId(), at, -1);

        List<ItemStack> drops = Block.getDroppedStacks(state, world, at, null, null, LOOT_TOOL);

        ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(at));
        world.removeBlock(at, false);
        MineFlooding.seal(world, at);

        for (ItemStack drop : drops) {
            int left = chest == null
                    ? drop.getCount()
                    : ChestDepositor.deposit(world, chest, drop.getItem(), drop.getCount());

            if (left > 0) {
                world.spawnEntity(new ItemEntity(world, villager.getX(), villager.getY() + 0.5, villager.getZ(),
                        new ItemStack(drop.getItem(), left)));
            }
        }

        breaking = null;
        ticksOnStep = 0;
        dug++;

        return Status.DIGGING;
    }

    private Status floor(ServerWorld world, VillagerEntity villager, BlockPos at, @Nullable ColonyPos chest) {
        if (!mayFloor(world, at)) {
            // A pista do lado sem chão ao céu aberto fica como está; a do meio já foi conferida no plano.
            step = new Step(step.side(), step.next(), step.cells(),
                    step.floors().stream().filter(floor -> !floor.equals(at)).toList());

            return Status.DIGGING;
        }

        // A pedra cavada volta como piso; sem ela no baú, a pedra é criada (A-5, decisão do autor de 2026-10-02).
        if (chest != null) {
            ChestWithdrawer.withdraw(world, chest, Items.COBBLESTONE, 1);
        }

        ColonyEdits.remember(MinecraftTypeAdapter.toColonyPos(at));
        world.setBlockState(at, Blocks.COBBLESTONE.getDefaultState());
        villager.swingHand(Hand.MAIN_HAND);
        ticksOnStep = 0;
        placed++;

        return Status.DIGGING;
    }

    private Status fail(String reason) {
        why = reason;

        return Status.FAILED;
    }

    String why() {
        return why;
    }

    BlockPos feet() {
        return feet;
    }

    int steps() {
        return steps;
    }

    int dug() {
        return dug;
    }

    int placed() {
        return placed;
    }
}
