package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Pedido do autor de 2026-10-08: o lenhador planta seis mudas perto da vila, onde
 * não atrapalham obra futura — fora da caixa da vila, longe da rua (o lote novo
 * nasce ao lado dela) e afastadas entre si.
 *
 * <p>Sessenta acima do piso: o patamar de grama tem 25 de lado, mais que a arena,
 * e lá em cima não há cenário de ninguém.
 */
public class VillageGroveGameTest implements FabricGameTest {

    private static final int Y = 60;

    /**
     * A rua, do lado leste — por onde a busca começa quando não há lenhador: seis
     * além da borda da caixa. Do lado norte ela não era testada (a primeira versão
     * sobreviveu à mutação).
     */
    private static final int PATH_X = 11;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 100)
    public void threeSaplingsGrowNearTheVillageAwayFromTheRoad(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = -9; x <= 15; x++) {
            for (int z = -9; z <= 15; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)),
                        x == PATH_X ? Blocks.DIRT_PATH.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(1, Y - 5, 1));
        BlockPos high = context.getAbsolutePos(new BlockPos(5, Y + 5, 5));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);
        int pathX = context.getAbsolutePos(new BlockPos(PATH_X, Y, 0)).getX();

        try {
            int planted = VillageGrove.tendNow(world, colony);
            List<BlockPos> grove = VillageGrove.of(colony.id());

            context.assertTrue(planted == 6 && grove.size() == 6,
                    "o bosque não ficou com seis mudas: " + planted + " " + grove);

            for (BlockPos spot : grove) {
                context.assertTrue(!box.containsColumn(spot.getX(), spot.getZ(), VillageGrove.NEAR - 1),
                        "uma muda nasceu dentro da vila: " + spot);
                context.assertTrue(box.containsColumn(spot.getX(), spot.getZ(), 6),
                        "uma muda nasceu a mais de 6 da vila (O5: o bosque perto): " + spot);
                context.assertTrue(Math.abs(spot.getX() - pathX) >= VillageGrove.ROAD_CLEARANCE,
                        "uma muda nasceu ao lado da rua, onde os lotes nascem: " + spot);
                // A farinha de osso pode já ter feito a muda virar árvore.
                context.assertTrue(world.getBlockState(spot.up()).isIn(net.minecraft.registry.tag.BlockTags.SAPLINGS)
                                || world.getBlockState(spot.up()).isIn(net.minecraft.registry.tag.BlockTags.LOGS),
                        "não há muda nem árvore em " + spot);

                for (BlockPos other : grove) {
                    context.assertTrue(other.equals(spot)
                                    || other.getSquaredDistance(spot) >= VillageGrove.SPACING * VillageGrove.SPACING,
                            "duas mudas coladas: " + spot + " e " + other);
                }
            }

            context.assertTrue(VillageGrove.tendNow(world, colony) == 0,
                    "com o bosque cheio o lenhador plantou mais");

            // P7a, bosque-fazenda: a árvore madura do bosque é a escolhida, e o
            // bosque tem seis mudas por lenhador.
            BlockPos grown = grove.get(0).up();
            world.setBlockState(grown, Blocks.OAK_LOG.getDefaultState());

            context.assertTrue(VillageGrove.matureTree(colony.id(), grove.get(0), log -> true, world)
                            .filter(grown::equals).isPresent(),
                    "o tronco que cresceu no bosque não foi o escolhido");

            for (int i = 0; i < 2; i++) {
                UUID lumberjack = UUID.randomUUID();
                VillageColonyMod.WORKERS.register(lumberjack, colony.id())
                        .assign(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK);
                owned.owning(lumberjack);
            }

            context.assertTrue(VillageGrove.sizeFor(colony.id()) == 2 * VillageGrove.SIZE,
                    "dois lenhadores, e o bosque não dobrou: " + VillageGrove.sizeFor(colony.id()));
        } finally {
            for (int x = -9; x <= 15; x++) {
                for (int z = -9; z <= 15; z++) {
                    for (int up = 1; up <= 10; up++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + up, z)), Blocks.AIR.getDefaultState());
                    }
                    world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.AIR.getDefaultState());
                }
            }

            owned.cleanUp();
            VillageGrove.clearAll();
        }

        context.complete();
    }

    /**
     * A árvore cortada longe não é replantada lá: a muda vai para o bosque perto da
     * vila, que cresce até o dobro — regra do autor, 2026-10-11.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 20)
    public void theSaplingOfAFarTreeGoesToTheGrove(TestContext context) {
        BlockPos low = context.getAbsolutePos(new BlockPos(1, Y - 5, 1));
        BlockPos high = context.getAbsolutePos(new BlockPos(5, Y + 5, 5));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            int before = VillageGrove.sizeFor(colony.id());
            BlockPos near = high.add(VillageGrove.NEAR, 0, 0);
            BlockPos far = high.add(80, 0, 0);

            context.assertFalse(VillageGrove.takesTheSapling(context.getWorld(), colony.id(), near),
                    "a árvore do bosque é replantada no lugar");
            context.assertTrue(VillageGrove.takesTheSapling(context.getWorld(), colony.id(), far),
                    "a muda da árvore longe devia ir para o bosque");
            context.assertTrue(VillageGrove.sizeFor(colony.id()) == before + 1,
                    "o bosque não cresceu com a muda: " + VillageGrove.sizeFor(colony.id()));

            for (int i = 0; i < 3 * VillageGrove.SIZE; i++) {
                VillageGrove.takesTheSapling(context.getWorld(), colony.id(), far);
            }

            context.assertTrue(VillageGrove.sizeFor(colony.id()) == 2 * before,
                    "o bosque passou do dobro: " + VillageGrove.sizeFor(colony.id()));
        } finally {
            owned.cleanUp();
            VillageGrove.clearAll();
        }

        context.complete();
    }

    /** Uma em cada dez mudas do bosque é de outra espécie, e nunca a do bioma (autor, 2026-10-27). */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove", tickLimit = 20)
    public void oneSaplingInTenIsOfAnotherSpecies(TestContext context) {
        UUID colonyId = UUID.randomUUID();
        int others = 0;

        for (int i = 1; i <= 3 * VillageGrove.OTHER_SPECIES_EVERY; i++) {
            net.minecraft.block.Block chosen = VillageGrove.speciesFor(context.getWorld(), colonyId, Blocks.ACACIA_SAPLING);

            if (chosen != Blocks.ACACIA_SAPLING) {
                others++;
                context.assertTrue(i % VillageGrove.OTHER_SPECIES_EVERY == 0, "outra espécie fora da vez, na muda " + i);
                context.assertTrue(VillageGrove.OTHER_SPECIES.contains(chosen), "espécie fora da lista: " + chosen);
            }
        }

        context.assertTrue(others == 3, "em 30 mudas deviam vir 3 de outra espécie, vieram " + others);
        context.complete();
    }

    /**
     * Regra 268: o bosque acha o chão em qualquer altura. A vila fica num vale e a borda dela, 9 blocos acima do centro:
     * a busca antiga olhava só de 4 acima a 8 abaixo do centro e não plantava nada (jogo da 0.4.68).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove_high_edge", tickLimit = 100)
    public void theGroveFindsGroundHighAboveTheCentre(TestContext context) {
        ServerWorld world = context.getWorld();
        int high = Y + 9;

        for (int x = -9; x <= 15; x++) {
            for (int z = -9; z <= 15; z++) {
                boolean inside = x >= 1 && x <= 5 && z >= 1 && z <= 5;

                world.setBlockState(context.getAbsolutePos(new BlockPos(x, inside ? Y : high, z)), Blocks.GRASS_BLOCK.getDefaultState());
            }
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(1, Y - 5, 1));
        BlockPos top = context.getAbsolutePos(new BlockPos(5, Y + 5, 5));
        VillageBounds box = new VillageBounds(low.getX(), low.getY(), low.getZ(), top.getX(), top.getY(), top.getZ());
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            int planted = VillageGrove.tendNow(world, colony);

            context.assertTrue(planted > 0, "Regra 268: a borda alta da vila não recebeu nenhuma muda");
            context.assertTrue(VillageGrove.of(colony.id()).stream().allMatch(spot -> spot.getY() == context.getAbsolutePos(
                    new BlockPos(0, high, 0)).getY()), "Regra 268: muda fora do chão da borda: " + VillageGrove.of(colony.id()));
        } finally {
            for (int x = -9; x <= 15; x++) {
                for (int z = -9; z <= 15; z++) {
                    for (int up = 0; up <= 20; up++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + up, z)), Blocks.AIR.getDefaultState());
                    }
                }
            }

            owned.cleanUp();
            VillageGrove.clearAll();
        }

        context.complete();
    }

    /**
     * Regra 271: a muda do bosque surge (no deserto também, pela madeira de reserva da vila), plantada em terra enraizada,
     * e entra na conta do que surge ({@code VC_SUPPLY}).
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_grove_supply", tickLimit = 100)
    public void theGroveSaplingAppearsOnRootedDirtAndIsDeclared(TestContext context) {
        ServerWorld world = context.getWorld();

        for (int x = -9; x <= 15; x++) {
            for (int z = -9; z <= 15; z++) {
                world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y, z)), Blocks.SAND.getDefaultState());
            }
        }

        BlockPos low = context.getAbsolutePos(new BlockPos(1, Y - 5, 1));
        BlockPos top = context.getAbsolutePos(new BlockPos(5, Y + 5, 5));
        Colony colony = Colony.create(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(3, Y + 1, 3))));

        colony.measure(new VillageBounds(low.getX(), low.getY(), low.getZ(), top.getX(), top.getY(), top.getZ()));
        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        try {
            context.assertTrue(com.villagecolony.fabric.integration.VillageBiomes
                            .woodFor(net.minecraft.world.biome.BiomeKeys.DESERT).isPresent(),
                    "Regra 271: o deserto ficou sem madeira de reserva, e o bosque dele sem muda");

            int planted = VillageGrove.tendNow(world, colony);
            List<BlockPos> grove = VillageGrove.of(colony.id());

            context.assertTrue(planted > 0, "o bosque na areia não recebeu muda");
            context.assertTrue(grove.stream().allMatch(spot -> world.getBlockState(spot).isOf(TreeNursery.BED)),
                    "Regra 271: muda fora da terra enraizada: " + grove);
            context.assertTrue(com.villagecolony.fabric.integration.Supplies.sessionCount(colony.id(),
                            com.villagecolony.core.coordination.SupplyException.valueOf("GROVE_SAPLING")) == planted,
                    "Regra 271: a muda que surgiu não entrou no VC_SUPPLY");
        } finally {
            for (int x = -9; x <= 15; x++) {
                for (int z = -9; z <= 15; z++) {
                    for (int up = 0; up <= 12; up++) {
                        world.setBlockState(context.getAbsolutePos(new BlockPos(x, Y + up, z)), Blocks.AIR.getDefaultState());
                    }
                }
            }

            owned.cleanUp();
            VillageGrove.clearAll();
        }

        context.complete();
    }
}
