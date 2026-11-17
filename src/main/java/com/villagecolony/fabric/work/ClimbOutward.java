package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.block.BlockState;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A subida rumo à borda da vila — Regra 312.
 *
 * <p>O mineiro que fez {@link BedTeleport#STUCK_ATTEMPTS} tentativas de se soltar no mesmo lugar não vai mais para a
 * cama: cava uma escada que se afasta do centro, mirando a borda mais perto da caixa da vila
 * ({@link MineReturn#homeFor}), para estragar o menos possível do terreno da vila. Nessa subida ele cava também a casca
 * da vila (só terreno natural e o que a {@code BlockProtection} deixa), e põe bloco onde falta piso: no degrau à
 * frente, ou debaixo dos pés quando só dá para subir na vertical. O bloco vem do que ele cavou, e sem isso do baú dele.
 * Sem degrau, sem pilar e sem passagem para o lado por {@link ClimbOut#TUNNEL_PASSES} passagens, vale o lugar seguro de
 * sempre ({@link BedTeleport#toSafety}).
 */
final class ClimbOutward {

    /** O que se põe quando o bolso está vazio, tirado do baú dele, nesta ordem. */
    private static final List<Item> FROM_CHEST = List.of(Items.COBBLESTONE, Items.DIRT, Items.COBBLED_DEEPSLATE);

    /** Sem a caixa da vila, a quantos blocos fica o ponto para onde ele se afasta do centro. */
    static final int AWAY = 16;

    /** Um passo: onde ele fica de pé, o que sai, e onde vai um bloco (o piso à frente, ou os pés no pilar). */
    record Move(BlockPos standAt, List<BlockPos> toBreak, Optional<BlockPos> place) {
    }

    private ClimbOutward() {
    }

    /** Entra na subida rumo à borda. */
    static void begin(ClimbOut.Climb climb, UUID workerId, BlockPos feet) {
        climb.mode = ClimbOut.Mode.OUTWARD;
        climb.noGain = 0;
        climb.outward = VillageColonyMod.WORKERS.find(workerId)
                .flatMap(worker -> VillageColonyMod.COLONIES.find(worker.colonyId()))
                .map(colony -> colony.bounds().isPresent() ? MineReturn.homeFor(colony, feet)
                        : awayFrom(MinecraftTypeAdapter.toBlockPos(colony.center()), feet))
                .orElse(feet.east(AWAY));
        BedTeleport.forgetAttempts(workerId);

        VillageColonyMod.LOGGER.info("Stranded worker {} digs its way out toward the village edge at {} from {} — {} attempts"
                        + " without leaving the place",
                ClimbOut.shortId(workerId), climb.outward.toShortString(), feet.toShortString(), BedTeleport.STUCK_ATTEMPTS);
    }

    /** Sem a caixa da vila medida: um ponto {@value #AWAY} blocos adiante, na direção que se afasta do centro. */
    static BlockPos awayFrom(BlockPos center, BlockPos feet) {
        int sx = Integer.signum(feet.getX() - center.getX());
        int sz = Integer.signum(feet.getZ() - center.getZ());

        return sx == 0 && sz == 0 ? feet.east(AWAY) : feet.add(sx * AWAY, 0, sz * AWAY);
    }

    /** Uma passagem da subida rumo à borda. */
    static void pass(ServerWorld world, VillagerEntity villager, ClimbOut.Climb climb, UUID workerId, BlockPos feet,
                     long now) {
        Optional<Item> block = blockAt(world, climb);
        Optional<Move> move = plan(world, feet, climb.outward, block.isPresent());

        if (move.isEmpty()) {
            if (climb.noGain >= ClimbOut.TUNNEL_PASSES) {
                BedTeleport.toSafety(world, villager, workerId, "boxed in on the way out toward the village edge");
            }

            return;
        }

        if (!move.get().toBreak().isEmpty()) {
            StrandedEscape.dig(world, new StrandedEscape.Step(move.get().standAt(), move.get().toBreak()), climb.chest,
                    feet, workerId);
        }

        if (move.get().place().isPresent() && block.isPresent()) {
            BlockPos at = move.get().place().get();

            if (at.equals(feet)) {
                villager.requestTeleport(feet.getX() + 0.5, feet.getY() + 1, feet.getZ() + 0.5);
            }

            world.setBlockState(at, ((BlockItem) block.get()).getBlock().getDefaultState());
            spend(world, climb, block.get());
        }

        villager.swingHand(Hand.MAIN_HAND);
        ClimbOut.moveTo(climb, move.get().standAt(), now);
    }

    /**
     * O passo rumo a {@code away}: primeiro o degrau (subindo um, com bloco no piso que falta), depois o pilar (bloco
     * debaixo dos pés), e por fim um passo para o lado sem subir.
     */
    static Optional<Move> plan(ServerWorld world, BlockPos feet, BlockPos away, boolean hasBlock) {
        int dx = away.getX() - feet.getX();
        int dz = away.getZ() - feet.getZ();
        List<Direction> ways = new ArrayList<>(Direction.Type.HORIZONTAL.stream().toList());
        ways.sort(Comparator.comparingInt((Direction way) -> -(way.getOffsetX() * dx + way.getOffsetZ() * dz)));

        for (Direction way : ways) {
            BlockPos floor = feet.offset(way);
            Optional<Move> step = toward(world, floor.up(), List.of(feet.up(2), floor.up(), floor.up(2)), floor, hasBlock);

            if (step.isPresent()) {
                return step;
            }
        }

        Optional<List<BlockPos>> head = clear(world, List.of(feet.up(2)));

        if (hasBlock && head.isPresent()) {
            return Optional.of(new Move(feet.up(), head.get(), Optional.of(feet)));
        }

        for (Direction way : ways) {
            BlockPos side = feet.offset(way);
            Optional<Move> step = toward(world, side, List.of(side, side.up()), side.down(), hasBlock);

            if (step.isPresent()) {
                return step;
            }
        }

        return Optional.empty();
    }

    private static Optional<Move> toward(ServerWorld world, BlockPos standAt, List<BlockPos> space, BlockPos floor,
                                         boolean hasBlock) {
        Optional<List<BlockPos>> toBreak = clear(world, space);

        if (toBreak.isEmpty()) {
            return Optional.empty();
        }

        if (!world.getBlockState(floor).getCollisionShape(world, floor).isEmpty()) {
            return Optional.of(new Move(standAt, toBreak.get(), Optional.empty()));
        }

        return hasBlock && mayPlace(world, floor)
                ? Optional.of(new Move(standAt, toBreak.get(), Optional.of(floor)))
                : Optional.empty();
    }

    /** O que sai para abrir o espaço, ou nada se algum bloco dele não pode sair ou se abrir é perigoso. */
    private static Optional<List<BlockPos>> clear(ServerWorld world, List<BlockPos> space) {
        List<BlockPos> toBreak = new ArrayList<>();

        for (BlockPos at : space) {
            BlockState state = world.getBlockState(at);

            if (!state.getFluidState().isEmpty()) {
                return Optional.empty();
            }

            if (state.getCollisionShape(world, at).isEmpty()) {
                continue;
            }

            if (!mayDig(world, at, state)) {
                return Optional.empty();
            }

            toBreak.add(at);
        }

        for (BlockPos at : space) {
            if (!space.contains(at.up()) && world.getBlockState(at.up()).getBlock() instanceof FallingBlock) {
                return Optional.empty();
            }
        }

        return Optional.of(toBreak);
    }

    /** Terreno natural que a proteção deixa sair, sem fluido encostado; a casca da vila também (Regra 312). */
    static boolean mayDig(ServerWorld world, BlockPos at, BlockState state) {
        if (world.getBlockEntity(at) != null
                || !(WorldTerrain.isNaturalGround(state)
                        || StrandedEscape.isRubbleUnderground(world, at, state))
                || !BlockProtection.mayDigOut(world, at, state)) {
            return false;
        }

        for (Direction side : Direction.values()) {
            if (!world.getFluidState(at.offset(side)).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static boolean mayPlace(ServerWorld world, BlockPos at) {
        BlockState state = world.getBlockState(at);

        return state.isReplaceable() && state.getFluidState().isEmpty()
                && !world.getFluidState(at.down()).isIn(FluidTags.LAVA);
    }

    /** O bloco que ele tem para pôr: o do bolso, e sem ele o do baú. */
    private static Optional<Item> blockAt(ServerWorld world, ClimbOut.Climb climb) {
        if (climb.pocket > 0 && climb.pocketItem instanceof BlockItem) {
            return Optional.of(climb.pocketItem);
        }

        ColonyPos chest = climb.chest;

        if (chest == null) {
            return Optional.empty();
        }

        return FROM_CHEST.stream()
                .filter(item -> ColonyChests.countIn(world, List.of(chest), item) > 0)
                .findFirst();
    }

    private static void spend(ServerWorld world, ClimbOut.Climb climb, Item block) {
        if (climb.pocket > 0 && climb.pocketItem == block) {
            climb.pocket--;
        } else if (climb.chest != null) {
            ColonyChests.withdraw(world, List.of(climb.chest), block, 1);
        }
    }
}
