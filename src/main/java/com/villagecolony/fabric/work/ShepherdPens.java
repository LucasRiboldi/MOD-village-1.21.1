package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

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
 * ele anda até ao lado de sua casa, coloca um poste e prende o animal"</i>. Os 5 fixos viraram a
 * lotação pelo tamanho do curral ({@link #FLOOR_PER_ANIMAL}, autor, 2026-10-22).
 *
 * <p>Cercado é o espaço fechado do lado de dentro de um portão de cerca
 * ({@link FencedIn#check}: o animal não chega a {@link FencedIn#RADIUS} blocos andando).
 * Os portões saem da superfície da caixa da vila, relida a cada {@link #RESCAN} tiques.
 */
final class ShepherdPens {

    static {
        ServerMemory.register(ShepherdPens.class, ShepherdPens::clearAll);
    }

    /**
     * Blocos de chão do cercado por animal — autor, 2026-10-22: a lotação é pelo tamanho do curral, e não
     * um número fixo. O cercado de 5×5 por dentro leva 6.
     */
    static final int FLOOR_PER_ANIMAL = 4;

    /** Releitura dos portões: cinco minutos. */
    static final long RESCAN = 6_000;

    /** Além da borda da caixa da vila, até onde procurar portão. */
    static final int MARGIN = 8;

    /** Até onde da casa procurar o lugar do poste novo. */
    static final int POST_RADIUS = 6;

    /** As cercas que viram poste, na ordem em que saem do baú. */
    static final List<Item> FENCES = List.of(Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE,
            Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE, Items.MANGROVE_FENCE, Items.CHERRY_FENCE);

    record Access(BlockPos gate, BlockPos inside, BlockPos outside) { }

    /**
     * Um cercado.
     *
     * @param gate o portão
     * @param inside o chão de dentro, encostado no portão
     * @param outside o chão de fora, de onde o pastor entrega o animal
     * @param reach o chão de dentro inteiro
     */
    record Pen(BlockPos gate, BlockPos inside, BlockPos outside, Set<BlockPos> reach, boolean large,
               List<Access> accesses) {
        Pen(BlockPos gate, BlockPos inside, BlockPos outside, Set<BlockPos> reach, boolean large) {
            this(gate, inside, outside, Set.copyOf(reach), large, List.of(new Access(gate, inside, outside)));
        }
        Pen {
            reach = Set.copyOf(reach);
            accesses = List.copyOf(accesses);
        }
        Pen at(Access access) {
            return new Pen(access.gate(), access.inside(), access.outside(), reach, large, accesses);
        }
    }

    private static final Map<UUID, List<Pen>> PENS = new HashMap<>();

    private static final Map<UUID, Long> READ_AT = new HashMap<>();

    /** Da última varredura: portões que não fecham um cercado, para o log. */
    private static List<BlockPos> openGates = List.of();

    private ShepherdPens() {
    }

    static void clearAll() {
        PENS.clear();
        READ_AT.clear();
    }

    /** O cercado com lugar ({@link #hasRoom}) mais perto de {@code from}. */
    static Optional<Pen> withRoom(ServerWorld world, Colony colony, BlockPos from) {
        return pensOf(world, colony).stream()
                .filter(pen -> hasRoom(world, pen))
                .flatMap(pen -> nearestAccess(world, pen, from, access -> true).stream())
                .min(Comparator.comparingDouble(pen -> pen.outside().getSquaredDistance(from)));
    }

    /** Curral grande registrado leva 20; cercados legados usam área de chão. */
    static int capacityOf(Pen pen) {
        return pen.large() ? 20 : Math.max(1, pen.reach().size() / FLOOR_PER_ANIMAL);
    }

    static boolean hasRoom(ServerWorld world, Pen pen) {
        return animalsIn(world, pen) < capacityOf(pen);
    }

    /**
     * Se a vila tem cercado e todos estão cheios — autor, 2026-10-22: aí um curral novo entra na fila de
     * obras ({@link PenPlans}).
     */
    static boolean allFull(ServerWorld world, Colony colony) {
        List<Pen> pens = pensOf(world, colony);

        return !pens.isEmpty() && pens.stream().noneMatch(pen -> hasRoom(world, pen));
    }

    /**
     * O cercado para um animal desta espécie — Regra 72, autor de 2026-10-22: <i>"deixar animais iguais no
     * mesmo curral; se tiver mais de um curral na vila, colocar um tipo de animal em um curral e outro em
     * outro"</i>. Entre os que têm lugar, na ordem: o que só tem animais desta espécie; o vazio (que passa a
     * ser dela); o misto que já a tem; qualquer um. No empate, o mais perto de {@code from}. Com um curral
     * só, todos vão para ele, como antes.
     *
     * @param except o cercado de onde o animal está saindo, ou nulo
     * @param onlyOwn só o cercado desta espécie ou o vazio — a separação dos currais mistos
     */
    static Optional<Pen> forSpecies(ServerWorld world, Colony colony, EntityType<?> species, BlockPos from,
            @Nullable Pen except, boolean onlyOwn) {
        return forSpecies(world, colony, species, from, except, onlyOwn, access -> true);
    }

    /** A navegação existente comprova o trajeto do pastor até cada entrada candidata — ADR-046. */
    static Optional<Pen> forSpecies(ServerWorld world, Colony colony, EntityType<?> species,
            net.minecraft.entity.passive.VillagerEntity shepherd, @Nullable Pen except, boolean onlyOwn) {
        return forSpecies(world, colony, species, shepherd.getBlockPos(), except, onlyOwn,
                access -> canReach(shepherd, access));
    }

    static Optional<Pen> reachableFrom(ServerWorld world, Pen pen, net.minecraft.entity.passive.VillagerEntity shepherd) {
        return nearestAccess(world, pen, shepherd.getBlockPos(), access -> canReach(shepherd, access));
    }

    static boolean canReach(net.minecraft.entity.passive.VillagerEntity shepherd, Access access) {
        // Além do alcance da navegação não há o que comprovar: ele vai por pernas (WalkLegs), e o prazo da coleta responde.
        if (!shepherd.getBlockPos().isWithinDistance(access.outside(), WalkLegs.LEG)) {
            return true;
        }

        var path = shepherd.getNavigation().findPathTo(access.outside(), 0);
        return path != null && path.reachesTarget();
    }

    private static Optional<Pen> forSpecies(ServerWorld world, Colony colony, EntityType<?> species, BlockPos from,
            @Nullable Pen except, boolean onlyOwn, java.util.function.Predicate<Access> reachable) {

        Pen best = null;
        int bestRank = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;

        for (Pen group : pensOf(world, colony)) {
            if ((except != null && group.reach().equals(except.reach())) || !hasRoom(world, group)) {
                continue;
            }
            var selected = nearestAccess(world, group, from, reachable);
            if (selected.isEmpty()) continue;
            Pen pen = selected.get();

            int rank = rankFor(speciesIn(world, pen), species);

            if (onlyOwn && rank > OWN_OR_EMPTY) {
                continue;
            }

            double distance = pen.outside().getSquaredDistance(from);

            if (rank < bestRank || (rank == bestRank && distance < bestDistance)) {
                best = pen;
                bestRank = rank;
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    static Optional<Pen> nearestAccess(ServerWorld world, Pen pen, BlockPos from,
            java.util.function.Predicate<Access> reachable) {
        return pen.accesses().stream().filter(access -> world.isChunkLoaded(access.gate())
                        && world.getBlockState(access.gate()).getBlock() instanceof FenceGateBlock
                        && world.getBlockState(access.gate().up()).getFluidState().isEmpty()
                        && world.getBlockState(access.gate().up()).getCollisionShape(world, access.gate().up()).isEmpty()
                        && standing(world, access.inside()) && standing(world, access.outside()))
                .sorted(Comparator.comparingDouble((Access access) -> access.outside().getSquaredDistance(from))
                        .thenComparingLong(access -> access.gate().asLong()))
                .filter(reachable).findFirst().map(pen::at);
    }

    private static boolean standing(ServerWorld world, BlockPos feet) {
        return world.isChunkLoaded(feet) && isFloor(world, feet.down())
                && world.getBlockState(feet).getFluidState().isEmpty() && world.getBlockState(feet.up()).getFluidState().isEmpty()
                && world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty();
    }

    /** O caminho tem 15/16 de altura e por isso não tem a face de cima cheia, mas é chão: a rua chega ao portão — Regra 196. */
    private static boolean isFloor(ServerWorld world, BlockPos floor) {
        BlockState state = world.getBlockState(floor);

        return state.isOf(Blocks.DIRT_PATH) || state.isSideSolidFullSquare(world, floor, Direction.UP);
    }

    /** A caixa acompanha todo o chão, inclusive cantos além de 12 blocos do portão — ADR-046. */
    static Box areaOf(Pen pen) {
        if (pen.reach().isEmpty()) return new Box(pen.inside());
        var xs = pen.reach().stream().mapToInt(BlockPos::getX).summaryStatistics();
        var ys = pen.reach().stream().mapToInt(BlockPos::getY).summaryStatistics();
        var zs = pen.reach().stream().mapToInt(BlockPos::getZ).summaryStatistics();
        return new Box(xs.getMin(), ys.getMin(), zs.getMin(), xs.getMax() + 1, ys.getMax() + 2, zs.getMax() + 1);
    }

    /** O pior posto que ainda conta como "o curral da espécie": só ela, ou vazio. */
    static final int OWN_OR_EMPTY = 1;

    /** 0 só desta espécie; 1 vazio; 2 misto com ela; 3 sem ela. */
    static int rankFor(Map<EntityType<?>, Integer> inside, EntityType<?> species) {
        if (inside.isEmpty()) {
            return 1;
        }

        if (inside.containsKey(species)) {
            return inside.size() == 1 ? 0 : 2;
        }

        return 3;
    }

    /** Quantos de cada espécie estão no chão de dentro deste cercado. */
    static Map<EntityType<?>, Integer> speciesIn(ServerWorld world, Pen pen) {
        Map<EntityType<?>, Integer> count = new HashMap<>();

        for (AnimalEntity animal : world.getEntitiesByClass(AnimalEntity.class,
                areaOf(pen),
                animal -> animal.isAlive() && pen.reach().contains(animal.getBlockPos()))) {
            count.merge(animal.getType(), 1, Integer::sum);
        }

        return count;
    }

    /** Quanto o animal tem para atravessar o portão andando antes de a coleta desistir. */
    static final int WALK_LIMIT = 200;

    /**
     * O animal atravessa o portão andando, sem teleporte — escolha do autor de 2026-11-04 (MAE, item 5).
     * O portão é aberto pelo pastor (a navegação não abre portão) e fica aberto enquanto o animal passa.
     */
    static void walkThrough(ServerWorld world, Pen pen, AnimalEntity animal, BlockPos target) {
        FencedIn.open(world, pen.gate());
        // O passeio da própria IA toma a navegação; ela volta para o portão a cada passo — MAE, item 5.
        if (!animal.getNavigation().isIdle() && target.equals(animal.getNavigation().getTargetPos())) return;
        // A navegação dá por chegada a um bloco do alvo e o deixa no portão; o último passo é direto (PenMoves).
        if (animal.getBlockPos().isWithinDistance(target, 2.5)) {
            animal.getMoveControl().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        } else {
            animal.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
        }
    }

    /** Já está no chão de dentro deste cercado e não encosta mais no portão, que assim pode fechar. */
    static boolean isInside(Pen pen, AnimalEntity animal) {
        return pen.reach().contains(animal.getBlockPos())
                && !animal.getBoundingBox().intersects(new net.minecraft.util.math.Box(pen.gate()));
    }

    /** Uma célula além da primeira de dentro, na direção do portão para dentro, para o animal liberar o portão. */
    static BlockPos deeper(Pen pen) {
        BlockPos step = pen.inside().add(pen.inside().subtract(pen.gate()));
        return pen.reach().contains(step) ? step : pen.inside();
    }

    /** Já saiu do cercado e está junto do lado de fora do portão. */
    static boolean isOutside(Pen pen, AnimalEntity animal) {
        return !pen.reach().contains(animal.getBlockPos())
                && animal.getBlockPos().isWithinDistance(pen.outside(), 2);
    }

    /** Os cercados desta colônia, relidos a cada {@link #RESCAN}. */
    static List<Pen> pensOf(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long read = READ_AT.get(colony.id());

        if (read == null || now - read >= RESCAN) {
            READ_AT.put(colony.id(), now);
            PENS.put(colony.id(), colony.bounds().map(box -> scan(world, box).stream()
                    .map(pen -> markLarge(colony, pen)).toList()).orElse(List.of()));
            // O chão de dentro sai do caminho de quem não é pastor (Regra 122).
            com.villagecolony.fabric.integration.PenFloors.update(colony.id(),
                    PENS.get(colony.id()).stream().map(Pen::reach).toList());
            PenLog.write(world, colony, PENS.getOrDefault(colony.id(), List.of()), openGates);
        }

        return PENS.getOrDefault(colony.id(), List.of());
    }

    /** Os portões na superfície da caixa e o cercado de dentro de cada um. */
    static List<Pen> scan(ServerWorld world, VillageBounds box) {
        List<Pen> pens = new ArrayList<>();
        List<BlockPos> open = new ArrayList<>();

        for (int x = box.minX() - MARGIN; x <= box.maxX() + MARGIN; x++) {
            for (int z = box.minZ() - MARGIN; z <= box.maxZ() + MARGIN; z++) {
                if (!world.isChunkLoaded(new BlockPos(x, 0, z))) {
                    continue;
                }

                int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);

                for (int y = top - 1; y >= top - 3; y--) {
                    BlockPos at = new BlockPos(x, y, z);

                    if (world.getBlockState(at).getBlock() instanceof FenceGateBlock) {
                        Optional<Pen> behind = penBehind(world, at, pens);

                        if (behind.isEmpty()) {
                            open.add(at);
                        }

                        behind.ifPresent(found -> {
                            for (int i = 0; i < pens.size(); i++) {
                                Pen existing = pens.get(i);
                                if (!existing.reach().equals(found.reach())) continue;
                                var accesses = new ArrayList<>(existing.accesses());
                                if (!accesses.contains(found.accesses().getFirst())) accesses.add(found.accesses().getFirst());
                                pens.set(i, new Pen(existing.gate(), existing.inside(), existing.outside(), existing.reach(),
                                        existing.large(), accesses));
                                return;
                            }
                            pens.add(found);
                        });
                    }
                }
            }
        }

        openGates = List.copyOf(open);

        return List.copyOf(pens);
    }

    /**
     * O cercado do lado de dentro deste portão; a varredura agrupa acessos pela área real (ADR-046).
     * Com os dois lados fechados (portão entre dois cercados), o de dentro é o menor.
     */
    static Optional<Pen> penBehind(ServerWorld world, BlockPos gate, List<Pen> known) {
        Pen best = null;
        var facing = world.getBlockState(gate).get(FenceGateBlock.FACING);

        for (Direction way : Direction.Type.HORIZONTAL) {
            if (way.getAxis() != facing.getAxis()) continue;
            BlockPos inside = gate.offset(way);
            if (!standing(world, inside)) continue;

            var existing = known.stream().filter(pen -> pen.reach().contains(inside)).findFirst();
            if (existing.isPresent()) {
                return Optional.of(new Pen(gate.toImmutable(), inside.toImmutable(),
                        gate.offset(way.getOpposite()).toImmutable(), existing.get().reach(), existing.get().large()));
            }

            FencedIn.Result pen = FencedIn.check(world, inside, 2 * FencedIn.RADIUS);

            if (pen.enclosed() && pen.reach().size() >= 4
                    && (best == null || pen.reach().size() < best.reach().size())) {
                best = new Pen(gate.toImmutable(), inside.toImmutable(),
                        gate.offset(way.getOpposite()).toImmutable(), pen.reach(), false);
            }
        }

        return Optional.ofNullable(best);
    }

    private static Pen markLarge(Colony colony, Pen pen) {
        boolean large = VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                .anyMatch(building -> PenPlans.isLargePen(building.blueprint())
                        && building.contains(MinecraftTypeAdapter.toColonyPos(pen.gate())));
        return large ? new Pen(pen.gate(), pen.inside(), pen.outside(), pen.reach(), true, pen.accesses()) : pen;
    }

    /** Quantos animais estão no chão de dentro deste cercado. */
    static int animalsIn(ServerWorld world, Pen pen) {
        return world.getEntitiesByClass(AnimalEntity.class, areaOf(pen),
                animal -> animal.isAlive() && pen.reach().contains(animal.getBlockPos())).size();
    }

    /** Solta a corda diante do portão aberto: daqui o animal entra andando ({@link #walkThrough}). */
    static void startIn(ServerWorld world, Pen pen, AnimalEntity animal) {
        animal.detachLeash(true, false);
        animal.getNavigation().stop();
        walkThrough(world, pen, animal, deeper(pen));
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
                    && below.isSolidBlock(world, at.down()) && !com.villagecolony.fabric.integration.VillageRoad.isPathBlock(below)
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
