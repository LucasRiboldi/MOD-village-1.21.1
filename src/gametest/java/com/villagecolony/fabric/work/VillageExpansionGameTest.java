package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.coordination.VillageExpansion;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/** A vila cheia anexa a faixa do lado de chão bom — Regra 56 C. */
public class VillageExpansionGameTest implements FabricGameTest {

    /** Alto o bastante para o chão da prova ser o topo de cada coluna. */
    private static final int HIGH = 40;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_expansion", tickLimit = 20)
    public void aFullVillageAnnexesTheFlatDrySide(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(0, HIGH, 0));
        ColonyPos center = MinecraftTypeAdapter.toColonyPos(origin.add(4, 1, 4));
        Colony colony = Colony.create(UUID.randomUUID(), center);
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageBounds box = new VillageBounds(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 8, origin.getY() + 6, origin.getZ() + 8);
        int strip = VillageExpansion.STRIP;
        BlockPos from = origin.add(-strip, 0, -strip);
        BlockPos to = origin.add(8 + strip, 1, 8 + strip);

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);
        VillageExpansion.lotFound(colony.id());

        try {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, at.getY() == origin.getY() ? Blocks.STONE.getDefaultState()
                        : Blocks.AIR.getDefaultState(), 2);
            }

            // Água sobre o chão ao norte, ao sul e a oeste; o leste fica plano e seco.
            for (Side wet : new Side[] {Side.NORTH, Side.SOUTH, Side.WEST}) {
                VillageBounds band = VillageExpansion.stripOf(box, wet);

                for (int x = band.minX(); x <= band.maxX(); x++) {
                    for (int z = band.minZ(); z <= band.maxZ(); z++) {
                        world.setBlockState(new BlockPos(x, origin.getY() + 1, z), Blocks.WATER.getDefaultState(), 2);
                    }
                }
            }

            // Uma rua de oeste a leste no meio da vila, que acaba na borda leste.
            for (int x = 0; x <= 8; x++) {
                world.setBlockState(origin.add(x, 0, 4), Blocks.DIRT_PATH.getDefaultState(), 2);
            }

            VillageExpansion.SideSurvey east = VillageExpansionSurvey.survey(world, colony, box, Side.EAST);
            VillageExpansion.SideSurvey west = VillageExpansionSurvey.survey(world, colony, box, Side.WEST);

            context.assertTrue(east.columns() > 0 && east.flat() == east.columns() && east.water() == 0,
                    "o leste plano e seco não mediu assim: " + east);
            context.assertTrue(west.water() == west.columns(), "o oeste alagado não mediu água: " + west);

            Optional<Side> annexed = Optional.empty();

            for (int sweep = 1; sweep <= VillageExpansion.FULL_AFTER; sweep++) {
                context.assertTrue(annexed.isEmpty(), "a vila anexou antes de encher");
                annexed = VillageExpansionSurvey.sweptWithoutLot(world, colony);
            }

            context.assertTrue(annexed.equals(Optional.of(Side.EAST)), "anexou " + annexed + " em vez do leste");
            context.assertTrue(colony.bounds().orElseThrow().maxX() >= box.maxX() + strip,
                    "a caixa não ganhou a faixa: " + colony.bounds());
            context.assertTrue(VillageExpansion.sideOf(colony.id()).equals(Optional.of(Side.EAST)),
                    "a rua não ficou apontada para o leste");
            context.assertTrue(com.villagecolony.fabric.integration.RoadExtension.isGrowing(colony.id()),
                    "a vila anexou sem puxar a rua para dentro da faixa");

            // Um anexo a cada meia hora (0.3.42: quatro em cinco minutos soltaram a vila).
            int width = colony.bounds().orElseThrow().sizeX();

            for (int sweep = 0; sweep < 3 * VillageExpansion.FULL_AGAIN_AFTER; sweep++) {
                context.assertTrue(VillageExpansionSurvey.sweptWithoutLot(world, colony).isEmpty(),
                        "anexou de novo na mesma meia hora");
            }

            context.assertTrue(colony.bounds().orElseThrow().sizeX() == width, "a caixa cresceu de novo");
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
            }

            VillageExpansion.lotFound(colony.id());
            com.villagecolony.fabric.integration.RoadExtension.lotFound(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }

    /** Sem rua da vila que alcance a faixa, a vila não anexa: faixa sem rua não dá lote. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_expansion_no_road", tickLimit = 20)
    public void withoutARoadToReachTheStripTheVillageDoesNotAnnex(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos origin = context.getAbsolutePos(new BlockPos(0, HIGH, 0));
        Colony colony = Colony.create(UUID.randomUUID(), MinecraftTypeAdapter.toColonyPos(origin.add(4, 1, 4)));
        ColonyFixture owned = ColonyFixture.create().owning(colony);
        VillageBounds box = new VillageBounds(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + 8, origin.getY() + 6, origin.getZ() + 8);
        int strip = VillageExpansion.STRIP;
        BlockPos from = origin.add(-strip, 0, -strip);
        BlockPos to = origin.add(8 + strip, 1, 8 + strip);

        colony.measure(box);
        VillageColonyMod.COLONIES.register(colony);
        VillageExpansion.lotFound(colony.id());

        try {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, at.getY() == origin.getY() ? Blocks.STONE.getDefaultState()
                        : Blocks.AIR.getDefaultState(), 2);
            }

            // A colônia que a detecção funda na bateria pode encostar na faixa e vetar os lados:
            // a pergunta é sobre a rua, então ela sai do registro só durante a pergunta.
            VillageBounds area = new VillageBounds(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
            java.util.List<Colony> others = VillageColonyMod.COLONIES.all().stream()
                    .filter(other -> !other.id().equals(colony.id()))
                    .filter(other -> other.bounds().map(b -> b.touches(area, 0)).orElse(false))
                    .toList();
            java.util.List<VillageExpansion.SideSurvey> sides = new java.util.ArrayList<>();
            Optional<Side> annexed;

            others.forEach(other -> VillageColonyMod.COLONIES.remove(other.id()));

            try {
                for (Side side : Side.values()) {
                    sides.add(VillageExpansionSurvey.survey(world, colony, box, side));
                }

                annexed = VillageExpansionSurvey.sweptWithoutLot(world, colony);
            } finally {
                others.forEach(VillageColonyMod.COLONIES::register);
            }

            // Há lado bom para anexar: só a falta de rua pode impedir o anexo.
            context.assertFalse(VillageExpansion.ranked(sides).isEmpty(), "nenhum lado utilizável: " + sides);
            context.assertTrue(annexed.isEmpty(), "a vila anexou uma faixa que nenhuma rua alcança");
            context.assertTrue(colony.bounds().orElseThrow().equals(box), "a caixa cresceu sem rua: " + colony.bounds());
        } finally {
            for (BlockPos at : BlockPos.iterate(from, to)) {
                world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
            }

            VillageExpansion.lotFound(colony.id());
            owned.cleanUp();
        }

        context.complete();
    }
}
