package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Os cercados da vila, para o pastor — regra do autor, 2026-10-11: <i>"o pastor deve
 * levar os animais para dentro dos cercados como prioridade; se houver mais de 5
 * animais dentro de um cercado ele tenta outro cercado; não havendo cercados ele
 * prende a corda nos lugares mais próximos; se não houver poste para prender animal
 * ele anda até ao lado de sua casa, coloca um poste e prende o animal"</i>.
 *
 * <p>Cercado é o espaço fechado do lado de dentro de um portão de cerca
 * ({@link FencedIn#check}: o animal não chega a {@link FencedIn#RADIUS} blocos andando).
 * Os portões saem da superfície da caixa da vila, relida a cada {@link #RESCAN} tiques.
 */
final class ShepherdPens {

    static {
        ServerMemory.register(ShepherdPens.class, ShepherdPens::clearAll);
    }

    /** Mais que isto num cercado, e o pastor tenta outro. */
    static final int MAX_ANIMALS = 5;

    /** Releitura dos portões: cinco minutos. */
    static final long RESCAN = 6_000;

    /** Além da borda da caixa da vila, até onde procurar portão. */
    static final int MARGIN = 8;

    /** Até onde da casa procurar o lugar do poste novo. */
    static final int POST_RADIUS = 6;

    /** As cercas que viram poste, na ordem em que saem do baú. */
    static final List<Item> FENCES = List.of(Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE,
            Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE, Items.MANGROVE_FENCE, Items.CHERRY_FENCE);

    /**
     * Um cercado.
     *
     * @param gate o portão
     * @param inside o chão de dentro, encostado no portão
     * @param outside o chão de fora, de onde o pastor entrega o animal
     * @param reach o chão de dentro inteiro
     */
    record Pen(BlockPos gate, BlockPos inside, BlockPos outside, Set<BlockPos> reach) {
    }

    private static final Map<UUID, List<Pen>> PENS = new HashMap<>();

    private static final Map<UUID, Long> READ_AT = new HashMap<>();

    private ShepherdPens() {
    }

    static void clearAll() {
        PENS.clear();
        READ_AT.clear();
    }

    /** O cercado com lugar (até {@link #MAX_ANIMALS} animais) mais perto de {@code from}. */
    static Optional<Pen> withRoom(ServerWorld world, Colony colony, BlockPos from) {
        return pensOf(world, colony).stream()
                .filter(pen -> animalsIn(world, pen) <= MAX_ANIMALS)
                .min(Comparator.comparingDouble(pen -> pen.outside().getSquaredDistance(from)));
    }

    /** Os cercados desta colônia, relidos a cada {@link #RESCAN}. */
    static List<Pen> pensOf(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long read = READ_AT.get(colony.id());

        if (read == null || now - read >= RESCAN) {
            READ_AT.put(colony.id(), now);
            PENS.put(colony.id(), colony.bounds().map(box -> scan(world, box)).orElse(List.of()));
        }

        return PENS.getOrDefault(colony.id(), List.of());
    }

    /** Os portões na superfície da caixa e o cercado de dentro de cada um. */
    static List<Pen> scan(ServerWorld world, VillageBounds box) {
        List<Pen> pens = new ArrayList<>();

        for (int x = box.minX() - MARGIN; x <= box.maxX() + MARGIN; x++) {
            for (int z = box.minZ() - MARGIN; z <= box.maxZ() + MARGIN; z++) {
                if (!world.isChunkLoaded(new BlockPos(x, 0, z))) {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);

                for (int y = top - 1; y >= top - 3; y--) {
                    BlockPos at = new BlockPos(x, y, z);

                    if (world.getBlockState(at).getBlock() instanceof FenceGateBlock) {
                        penBehind(world, at, pens).ifPresent(pens::add);
                    }
                }
            }
        }

        return List.copyOf(pens);
    }

    /**
     * O cercado do lado de dentro deste portão, se for um e ainda não estiver na lista.
     * Com os dois lados fechados (portão entre dois cercados), o de dentro é o menor.
     */
    static Optional<Pen> penBehind(ServerWorld world, BlockPos gate, List<Pen> known) {
        Pen best = null;

        for (Direction way : Direction.Type.HORIZONTAL) {
            BlockPos inside = gate.offset(way);

            if (known.stream().anyMatch(pen -> pen.reach().contains(inside))) {
                return Optional.empty();
            }

            FencedIn.Result pen = FencedIn.check(world, inside);

            if (pen.enclosed() && pen.reach().size() >= 4
                    && (best == null || pen.reach().size() < best.reach().size())) {
                best = new Pen(gate.toImmutable(), inside.toImmutable(),
                        gate.offset(way.getOpposite()).toImmutable(), pen.reach());
            }
        }

        return Optional.ofNullable(best);
    }

    /** Quantos animais estão no chão de dentro deste cercado. */
    static int animalsIn(ServerWorld world, Pen pen) {
        return world.getEntitiesByClass(AnimalEntity.class, new Box(pen.inside()).expand(FencedIn.RADIUS),
                animal -> animal.isAlive() && pen.reach().contains(animal.getBlockPos())).size();
    }

    /** Põe o animal para dentro: abre o portão, ele entra, o portão fecha. */
    static void bringIn(ServerWorld world, Pen pen, AnimalEntity animal) {
        FencedIn.open(world, pen.gate());
        animal.detachLeash(true, false);
        animal.requestTeleport(pen.inside().getX() + 0.5, pen.inside().getY(), pen.inside().getZ() + 0.5);
        FencedIn.closeWhenClear(pen.gate());
    }

    /**
     * O lugar do poste novo ao lado da casa: chão firme, céu aberto (fora da casa), a
     * até {@link #POST_RADIUS} blocos, o mais perto. Só se há cerca nos baús.
     *
     * <p>Um poste por casa e um por cercado — regra do autor, 2026-10-12: com um poste
     * solto a até {@link #POST_RADIUS} da casa, ou a até {@link #POST_RADIUS} do lugar
     * novo (o lado de um cercado), não se põe outro.
     */
    static Optional<BlockPos> postBesideHome(ServerWorld world, UUID colonyId, BlockPos home) {
        if (fenceInChests(world, colonyId, home).isEmpty() || hasLonePostNear(world, home)) {
            return Optional.empty();
        }

        for (BlockPos at : BlockPos.iterateOutwards(home, POST_RADIUS, 2, POST_RADIUS)) {
            BlockState below = world.getBlockState(at.down());

            if (world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()
                    && below.isSolidBlock(world, at.down()) && !below.isOf(Blocks.DIRT_PATH)
                    && world.isSkyVisible(at) && !hasLonePostNear(world, at)) {
                return Optional.of(at.toImmutable());
            }
        }

        return Optional.empty();
    }

    /** Se há um poste solto (cerca sem cerca ao lado) a até {@link #POST_RADIUS} daqui. */
    static boolean hasLonePostNear(ServerWorld world, BlockPos near) {
        for (BlockPos at : BlockPos.iterateOutwards(near, POST_RADIUS, 2, POST_RADIUS)) {
            if (isLonePost(world, at)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLonePost(ServerWorld world, BlockPos at) {
        if (!world.getBlockState(at).isIn(net.minecraft.registry.tag.BlockTags.FENCES)) {
            return false;
        }

        for (net.minecraft.util.math.Direction side : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockState next = world.getBlockState(at.offset(side));

            if (next.isIn(net.minecraft.registry.tag.BlockTags.FENCES)
                    || next.isIn(net.minecraft.registry.tag.BlockTags.FENCE_GATES)
                    || next.isIn(net.minecraft.registry.tag.BlockTags.WALLS)) {
                return false;
            }
        }

        return true;
    }

    /** Põe o poste: uma cerca sai do baú e vira bloco. */
    static boolean placePost(ServerWorld world, UUID colonyId, BlockPos at) {
        if (!world.getBlockState(at).isAir()) {
            return world.getBlockState(at).isIn(net.minecraft.registry.tag.BlockTags.FENCES);
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, MinecraftTypeAdapter.toColonyPos(at));
        Optional<Item> fence = fenceInChests(world, colonyId, at);

        if (fence.isEmpty() || ColonyChests.withdraw(world, chests, fence.get(), 1) == 0) {
            return false;
        }

        world.setBlockState(at, Block.getBlockFromItem(fence.get()).getDefaultState());

        return true;
    }

    private static Optional<Item> fenceInChests(ServerWorld world, UUID colonyId, BlockPos near) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, MinecraftTypeAdapter.toColonyPos(near));

        return FENCES.stream().filter(fence -> ColonyChests.countIn(world, chests, fence) > 0).findFirst();
    }
}
