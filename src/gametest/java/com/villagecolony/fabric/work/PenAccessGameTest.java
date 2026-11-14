package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.List;
import java.util.UUID;

/** Quatro entradas físicas continuam sendo um único curral — ADR-046. */
public final class PenAccessGameTest implements FabricGameTest {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_four_access", tickLimit = 40)
    public void theShepherdChoosesTheNearGateOfOnePhysicalPen(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 60, 3));
        floor(context, origin, 6);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) {
            if (x == 0 || x == 6 || z == 0 || z == 6) {
                context.getWorld().setBlockState(origin.add(x, 1, z), Blocks.OAK_FENCE.getDefaultState(), 3);
            }
        }
        for (var side : Direction.Type.HORIZONTAL) {
            var gate = origin.add(3, 1, 3).offset(side, 3);
            context.getWorld().setBlockState(gate, Blocks.OAK_FENCE_GATE.getDefaultState()
                    .with(FenceGateBlock.FACING, side), 3);
        }
        var colony = colony(origin, 6);
        try {
            context.assertTrue(ShepherdPens.pensOf(context.getWorld(), colony).size() == 1, "quatro portões multiplicaram currais");
            for (var side : Direction.Type.HORIZONTAL) {
                var from = origin.add(3, 1, 3).offset(side, 5);
                var selected = ShepherdPens.withRoom(context.getWorld(), colony, from).orElseThrow();
                context.assertTrue(selected.gate().equals(origin.add(3, 1, 3).offset(side, 3)),
                        "pastor perdeu o portão próximo no lado " + side);
                context.assertTrue(ShepherdPens.capacityOf(selected) == 6, "entradas multiplicaram capacidade");
            }
            var north = origin.add(3, 1, -1);
            context.getWorld().setBlockState(north.up(), Blocks.STONE.getDefaultState(), 3);
            var selected = ShepherdPens.withRoom(context.getWorld(), colony, north).orElseThrow();
            context.assertTrue(!selected.gate().equals(north.south()), "portão exterior obstruído foi escolhido");
            context.getWorld().setBlockState(north.up(), Blocks.AIR.getDefaultState(), 3);
            context.getWorld().setBlockState(north.south().up(), Blocks.STONE.getDefaultState(), 3);
            selected = ShepherdPens.withRoom(context.getWorld(), colony, north).orElseThrow();
            context.assertTrue(!selected.gate().equals(north.south()), "portão com cabeça obstruída foi escolhido");
            context.assertTrue(ShepherdPens.forSpecies(context.getWorld(), colony,
                    net.minecraft.entity.EntityType.COW, north, selected, false).isEmpty(),
                    "outro portão do mesmo curral foi tratado como destino diferente");
        } finally {
            ShepherdPens.clearAll();
            for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(9, 4, 9))) {
                context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_large_access", tickLimit = 40)
    public void theActualLargeTemplateIsRecognizedFromItsSide(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 90, 3));
        floor(context, origin, 14);
        var colony = colony(origin, 14);
        var plan = StructureBlueprintReader.read(context.getWorld(), PenPlans.MODEL).orElseThrow();
        var project = com.villagecolony.core.construction.model.ConstructionProject.plan(colony.id(), plan,
                MinecraftTypeAdapter.toColonyPos(origin));
        for (var piece : plan.blocks()) {
            context.getWorld().setBlockState(origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z()),
                    com.villagecolony.fabric.integration.PaletteStates.planned(piece), 18);
        }
        var pens = ShepherdPens.scan(context.getWorld(), colony.bounds().orElseThrow());
        context.assertTrue(pens.size() == 1, "o curral físico 15x15 não foi reconhecido: " + pens.stream()
                .map(pen -> pen.gate().subtract(origin) + ":" + pen.reach().size() + ":" + pen.inside().subtract(origin)).toList());
        context.assertTrue(pens.getFirst().reach().contains(origin.add(13, 1, 13)), "canto distante ficou fora do cercado");
        var cow = net.minecraft.entity.EntityType.COW.create(context.getWorld());
        context.assertTrue(cow != null, "vaca da fixture ausente");
        cow.setAiDisabled(true);
        cow.refreshPositionAndAngles(origin.getX() + 13.5, origin.getY() + 1, origin.getZ() + 13.5, 0, 0);
        context.getWorld().spawnEntity(cow);
        var animals = new java.util.ArrayList<net.minecraft.entity.passive.CowEntity>();
        animals.add(cow);
        var building = com.villagecolony.core.construction.model.Building.of(project, true);
        com.villagecolony.VillageColonyMod.BUILDINGS.register(building);
        try {
            context.assertTrue(ShepherdFlock.isPenned(context.getWorld(), cow), "animal no canto do curral foi tratado como solto");
            context.assertTrue(ShepherdPens.animalsIn(context.getWorld(), pens.getFirst()) == 1,
                    "animal no canto não entrou na contagem");
            var pen = ShepherdPens.pensOf(context.getWorld(), colony).getFirst();
            context.assertTrue(ShepherdPens.capacityOf(pen) == 20 && pen.accesses().size() == 4,
                    "registro perdeu capacidade ou acessos do curral único");
            for (var at : pen.reach().stream().filter(at -> context.getWorld().getBlockState(at.down()).isOf(Blocks.GRASS_BLOCK))
                    .filter(at -> !at.equals(cow.getBlockPos())).sorted(java.util.Comparator.comparingLong(BlockPos::asLong))
                    .limit(19).toList()) {
                var animal = net.minecraft.entity.EntityType.COW.create(context.getWorld());
                context.assertTrue(animal != null, "vaca da fixture ausente");
                animal.setAiDisabled(true);
                animal.refreshPositionAndAngles(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
                context.getWorld().spawnEntity(animal);
                animals.add(animal);
            }
            context.assertTrue(ShepherdPens.animalsIn(context.getWorld(), pen) == 20
                    && !ShepherdPens.hasRoom(context.getWorld(), pen), "quatro entradas multiplicaram a lotação total de 20");
            // A vaca já está dentro: a entrega abre o portão principal e ele fecha assim que fica livre.
            ShepherdPens.startIn(context.getWorld(), pen, cow);
            context.assertTrue(ShepherdPens.isInside(pen, cow), "vaca do canto não conta como dentro");
            FencedIn.closeWhenClear(pen.gate());
            FencedIn.tick(context.getWorld());
            context.assertTrue(pen.accesses().stream().noneMatch(access ->
                    context.getWorld().getBlockState(access.gate()).get(FenceGateBlock.OPEN)), "portão ficou aberto após entrega");
            context.assertTrue(animals.stream().allMatch(animal -> pen.reach().contains(animal.getBlockPos())),
                    "a entrega controlada retirou animais do cercado");
        } finally {
            animals.forEach(net.minecraft.entity.passive.CowEntity::discard);
            com.villagecolony.VillageColonyMod.BUILDINGS.remove(building.id());
            ShepherdPens.clearAll();
            FencedIn.clearAll();
            for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(17, 4, 17))) {
                context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
            }
        }
        context.complete();
    }

    /** Um animal entra andando por cada um dos quatro portões do curral grande MAE, e cada portão fecha atrás. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_large_walk", tickLimit = 1200)
    public void animalsWalkInThroughEachOfTheFourGates(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 90, 3));
        // Animal que anda precisa de chunk com tique de entidade; a grade de teste não garante isso.
        forceChunks(context, origin, true);
        floor(context, origin, 14);
        var colony = colony(origin, 14);
        var plan = StructureBlueprintReader.read(context.getWorld(), PenPlans.MODEL).orElseThrow();
        for (var piece : plan.blocks()) {
            context.getWorld().setBlockState(origin.add(piece.offset().x(), piece.offset().y(), piece.offset().z()),
                    com.villagecolony.fabric.integration.PaletteStates.planned(piece), 18);
        }
        var pen = ShepherdPens.scan(context.getWorld(), colony.bounds().orElseThrow()).getFirst();
        var cows = new java.util.ArrayList<net.minecraft.entity.passive.CowEntity>();
        var escapes = new java.util.LinkedHashMap<Integer, String>();
        try {
            context.assertTrue(pen.accesses().size() == 4, "curral MAE sem os quatro acessos: " + pen.accesses().size());
            walkEach(context, origin, pen, pen.accesses(), 0, ShepherdPens.WALK_LIMIT, cows, escapes);
        } catch (RuntimeException | AssertionError failure) {
            clearLargeWalk(context, origin, cows);
            throw failure;
        }
    }

    private static void walkEach(TestContext context, BlockPos origin, ShepherdPens.Pen pen,
                                 List<ShepherdPens.Access> accesses, int index, int left,
                                 java.util.List<net.minecraft.entity.passive.CowEntity> cows,
                                 java.util.Map<Integer, String> escapes) {
        try {
            var world = context.getWorld();
            FencedIn.tick(world);
            watchEscapes(context, origin, pen, accesses, index, cows, escapes);
            if (index == accesses.size()) {
                context.assertTrue(cows.stream().allMatch(cow -> pen.reach().contains(cow.getBlockPos())),
                        "um animal saiu depois de entrar: " + escapes.values());
                context.assertTrue(accesses.stream().noneMatch(access -> world.getBlockState(access.gate()).get(FenceGateBlock.OPEN)),
                        "portão ficou aberto depois das entradas");
                clearLargeWalk(context, origin, cows);
                context.complete();
                return;
            }
            var access = accesses.get(index);
            var through = new ShepherdPens.Pen(access.gate(), access.inside(), access.outside(), pen.reach(), true);
            if (cows.size() == index) {
                var cow = net.minecraft.entity.EntityType.COW.create(world);
                context.assertTrue(cow != null, "vaca da fixture ausente");
                cow.refreshPositionAndAngles(access.outside().getX() + .5, access.outside().getY(), access.outside().getZ() + .5, 0, 0);
                world.spawnEntity(cow);
                cows.add(cow);
                ShepherdPens.startIn(world, through, cow);
            }
            var cow = cows.get(index);
            if (ShepherdPens.isInside(through, cow)) {
                FencedIn.closeWhenClear(access.gate());
                FencedIn.tick(world);
                context.waitAndRun(1, () -> walkEach(context, origin, pen, accesses, index + 1, ShepherdPens.WALK_LIMIT, cows,
                        escapes));
                return;
            }
            context.assertTrue(left > 0, "a vaca não entrou andando pelo portão " + access.gate().subtract(origin).toShortString()
                    + ": " + cow.getBlockPos().subtract(origin).toShortString() + " alvo=" + ShepherdPens.deeper(through).subtract(origin).toShortString()
                    + "/" + world.getBlockState(ShepherdPens.deeper(through)) + " dentro=" + access.inside().subtract(origin).toShortString()
                    + "/" + world.getBlockState(access.inside()) + " portão=" + world.getBlockState(access.gate())
                    + " parada=" + cow.getNavigation().isIdle() + " caminho=" + cow.getNavigation().getCurrentPath()
                    + " chão=" + cow.isOnGround() + " busca=" + cow.getNavigation().findPathTo(ShepherdPens.deeper(through), 0)
                    + " piso=" + world.getBlockState(ShepherdPens.deeper(through).down()) + "/" + world.getBlockState(access.inside().down())
                    + " fora=" + world.getBlockState(access.outside().down()) + " fugidas=" + escapes.values());
            ShepherdPens.walkThrough(world, through, cow, ShepherdPens.deeper(through));
            context.waitAndRun(1, () -> walkEach(context, origin, pen, accesses, index, left - 1, cows, escapes));
        } catch (RuntimeException | AssertionError failure) {
            clearLargeWalk(context, origin, cows);
            throw failure;
        }
    }

    /**
     * VF-010 A, só diagnóstico: anota, no primeiro tique em que cada vaca já entrada aparece fora do chão do curral, qual
     * vaca é, por qual portão entrou, onde está, o portão mais perto dela e se cada portão estava aberto naquele tique.
     */
    private static void watchEscapes(TestContext context, BlockPos origin, ShepherdPens.Pen pen,
                                     List<ShepherdPens.Access> accesses, int entered,
                                     java.util.List<net.minecraft.entity.passive.CowEntity> cows,
                                     java.util.Map<Integer, String> escapes) {
        var world = context.getWorld();
        for (int i = 0; i < Math.min(entered, cows.size()); i++) {
            var cow = cows.get(i);
            if (escapes.containsKey(i) || pen.reach().contains(cow.getBlockPos())) {
                continue;
            }
            var nearest = accesses.stream()
                    .min(java.util.Comparator.comparingDouble(access -> access.gate().getSquaredDistance(cow.getPos())))
                    .orElseThrow();
            var gates = new StringBuilder();
            for (var access : accesses) {
                gates.append(' ').append(access.gate().subtract(origin).toShortString()).append('=').append(gateState(world, access.gate()));
            }
            escapes.put(i, "vaca " + i + " (entrou pelo portão " + accesses.get(i).gate().subtract(origin).toShortString()
                    + ") fora no tique " + world.getTime() + " em " + cow.getBlockPos().subtract(origin).toShortString()
                    + " viva=" + cow.isAlive() + ", portão mais perto " + nearest.gate().subtract(origin).toShortString()
                    + " " + gateState(world, nearest.gate()) + "; portões:" + gates);
        }
    }

    private static String gateState(net.minecraft.server.world.ServerWorld world, BlockPos gate) {
        var state = world.getBlockState(gate);
        return state.contains(FenceGateBlock.OPEN) ? (state.get(FenceGateBlock.OPEN) ? "aberto" : "fechado") : state.toString();
    }

    private static void forceChunks(TestContext context, BlockPos origin, boolean force) {
        for (int x = (origin.getX() - 3) >> 4; x <= (origin.getX() + 17) >> 4; x++) {
            for (int z = (origin.getZ() - 3) >> 4; z <= (origin.getZ() + 17) >> 4; z++) {
                context.getWorld().setChunkForced(x, z, force);
            }
        }
    }

    private static void clearLargeWalk(TestContext context, BlockPos origin, java.util.List<net.minecraft.entity.passive.CowEntity> cows) {
        forceChunks(context, origin, false);
        cows.forEach(net.minecraft.entity.Entity::discard);
        ShepherdPens.clearAll();
        for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(17, 4, 17))) {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    /** Regra 196: o portão com rua na saída é acesso; o caminho não tem a face de cima cheia. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_path_access", tickLimit = 20)
    public void aGateWithAPathOutsideStillCounts(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 70, 3));
        var colony = penWithOneGate(context, origin);
        try {
            context.getWorld().setBlockState(origin.add(7, 0, 3), Blocks.DIRT_PATH.getDefaultState(), 18);
            ShepherdPens.clearAll();
            context.assertTrue(ShepherdPens.withRoom(context.getWorld(), colony, origin.add(8, 1, 3)).isPresent(),
                    "o portão com caminho na saída foi descartado");
        } finally {
            cleanPen(context, origin);
        }
        context.complete();
    }

    /** Regra 196: o amarrado também vai para o curral, e o nó vazio sai da cerca. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_tied", tickLimit = 40)
    public void aTiedAnimalGoesToThePen(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 76, 3));
        var colony = penWithOneGate(context, origin);
        runway(context, origin, 2);
        var post = origin.add(9, 1, 1);
        context.getWorld().setBlockState(post, Blocks.OAK_FENCE.getDefaultState(), 18);
        var shepherd = com.villagecolony.gametest.ColonyFixture.spawnAtWorld(context, net.minecraft.entity.EntityType.VILLAGER, origin.add(8, 1, 2));
        var cow = com.villagecolony.gametest.ColonyFixture.spawnAtWorld(context, net.minecraft.entity.EntityType.COW, origin.add(8, 1, 1));
        cow.setAiDisabled(true);
        context.getWorld().setTimeOfDay(1_000);
        context.waitAndRun(10, () -> {
            try {
                // Fora da caixa do teste o chunk pode ainda não tiquear, e a navegação só sai de quem está no chão.
                shepherd.setOnGround(true);
                var knot = net.minecraft.entity.decoration.LeashKnotEntity.getOrCreate(context.getWorld(), post);
                shepherd.getNavigation().stop();
                cow.attachLeash(knot, true);
                var herd = ShepherdHerding.start(context.getWorld(), colony, shepherd);
                context.assertTrue(herd.isPresent() && herd.get().animal.equals(cow.getUuid()), "o amarrado não foi escolhido:"
                        + " herd=" + herd.isPresent() + " penned=" + ShepherdFlock.isPenned(context.getWorld(), cow)
                        + " room=" + ShepherdPens.withRoom(context.getWorld(), colony, shepherd.getBlockPos()).isPresent()
                        + " reach=" + ShepherdPens.forSpecies(context.getWorld(), colony, cow.getType(), shepherd, null, false).isPresent()
                        + " shepherd=" + shepherd.getBlockPos() + " ground=" + shepherd.isOnGround()
                        + " cow=" + cow.getBlockPos());
                context.assertTrue(herd.get().pen != null, "o amarrado não foi para o curral");
                ShepherdHerding.step(context.getWorld(), shepherd.getUuid(), herd.get());
                context.assertTrue(cow.getLeashHolder() == shepherd, "o pastor não pegou a corda");
                context.assertTrue(knot.isRemoved(), "o nó vazio ficou na cerca");
            } finally {
                cow.discard();
                shepherd.discard();
                cleanPen(context, origin);
            }
            context.complete();
        });
    }

    /** Regra 196: curral com lugar que agora não se alcança não vira poste. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "pen_no_post", tickLimit = 40)
    public void aPenWithRoomMeansNoPost(TestContext context) {
        var origin = context.getAbsolutePos(new BlockPos(3, 84, 3));
        var colony = penWithOneGate(context, origin);
        runway(context, origin, 5);
        for (var at : BlockPos.iterate(origin.add(7, 1, 0), origin.add(9, 3, 2))) {
            context.getWorld().setBlockState(at, Blocks.STONE.getDefaultState(), 18);
        }
        for (var at : BlockPos.iterate(origin.add(8, 1, 1), origin.add(8, 2, 1))) {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
        var shepherd = com.villagecolony.gametest.ColonyFixture.spawnAtWorld(context, net.minecraft.entity.EntityType.VILLAGER, origin.add(8, 1, 1));
        var cow = com.villagecolony.gametest.ColonyFixture.spawnAtWorld(context, net.minecraft.entity.EntityType.COW, origin.add(8, 1, 5));
        cow.setAiDisabled(true);
        context.waitAndRun(10, () -> {
            try {
                // O mesmo aqui: sem isto o caminho falharia por não estar no chão, e o teste passaria sem medir nada.
                shepherd.setOnGround(true);
                context.assertTrue(ShepherdHerding.start(context.getWorld(), colony, shepherd).isEmpty(),
                        "o pastor preso levaria o animal para um poste, com curral livre na vila");
            } finally {
                cow.discard();
                shepherd.discard();
                cleanPen(context, origin);
            }
            context.complete();
        });
    }

    private static Colony penWithOneGate(TestContext context, BlockPos origin) {
        // O animal e o pastor ficam fora da caixa do teste, e chunk sem tíquete não os faz tiquear.
        forceChunks(context, origin, true);
        floor(context, origin, 6);
        for (int x = 0; x <= 6; x++) for (int z = 0; z <= 6; z++) {
            if (x == 0 || x == 6 || z == 0 || z == 6) {
                context.getWorld().setBlockState(origin.add(x, 1, z), Blocks.OAK_FENCE.getDefaultState(), 3);
            }
        }
        context.getWorld().setBlockState(origin.add(6, 1, 3), Blocks.OAK_FENCE_GATE.getDefaultState()
                .with(FenceGateBlock.FACING, Direction.EAST), 3);
        // A caixa cobre o curral e a pista em volta, onde ficam o poste e o pastor: só o que está nela é recolhido
        // (Regra 283).
        return colony(origin, 10);
    }

    /** Sem chão que leve para longe, o animal na ilha da arena conta como cercado ({@code FencedIn}). */
    private static void runway(TestContext context, BlockPos origin, int z) {
        for (int x = 10; x <= 36; x++) {
            context.getWorld().setBlockState(origin.add(x, 0, z), Blocks.GRASS_BLOCK.getDefaultState(), 18);
        }
    }

    private static void cleanPen(TestContext context, BlockPos origin) {
        ShepherdPens.clearAll();
        forceChunks(context, origin, false);
        for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(36, 4, 9))) {
            context.getWorld().setBlockState(at, Blocks.AIR.getDefaultState(), 18);
        }
    }

    private static Colony colony(BlockPos origin, int size) {
        var colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin));
        colony.measure(new VillageBounds(origin.getX(), origin.getY() - 2, origin.getZ(),
                origin.getX() + size, origin.getY() + 4, origin.getZ() + size));
        return colony;
    }

    private static void floor(TestContext context, BlockPos origin, int size) {
        for (var at : BlockPos.iterate(origin.add(-3, 0, -3), origin.add(size + 3, 4, size + 3))) {
            context.getWorld().setBlockState(at, at.getY() == origin.getY()
                    ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(), 18);
        }
    }
}
