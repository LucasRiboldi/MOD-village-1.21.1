package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.gametest.ColonyFixture;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

/**
 * Cada estrutura do mod sai na versão do bioma da vila — Regra 262: um arquivo serve todos os biomas, e a madeira e a
 * pedra dele trocam pelas da vila ao erguer. O que já está de pé, de outra madeira, continua reconhecido.
 */
public class BiomeBlueprintsGameTest implements FabricGameTest {

    private static final ResourceId LARGE_PEN = new ResourceId("villagecolony", "colony/large_animal_pen");

    private static final ResourceId PLAINS_STORAGE = new ResourceId("villagecolony", "colony/plains/storage_small");

    /** O arco do deserto sai da madeira da vila do deserto, e não do pinheiro do arquivo; o arco antigo segue de pé. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_blueprints", tickLimit = 40)
    public void theDesertArchIsOfTheVillageWoodAndStillStands(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos mouth = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(1_000_000, 0, 3_300_000);

        village(context, mouth, "minecraft:desert");

        context.waitAndRun(3, () -> {
            try {
                desertArch(context, world, mouth);
            } finally {
                clear(world, mouth);
            }

            context.complete();
        });
    }

    private static void desertArch(TestContext context, ServerWorld world, BlockPos mouth) {
        context.assertTrue(BiomeBlueprints.swapAt(world, MinecraftTypeAdapter.toColonyPos(mouth)).stone()
                .equals(com.villagecolony.core.construction.model.VillagePalette.SANDSTONE), "a vila não é de deserto");
        MineGate.raise(world, mouth, Direction.SOUTH);
        List<BlockPos> arch = MineGate.positions(mouth, Direction.SOUTH);

        context.assertTrue(arch.stream().noneMatch(at -> world.getBlockState(at).isOf(Blocks.STRIPPED_SPRUCE_LOG)),
                "o arco do deserto saiu de pinheiro");
        context.assertTrue(arch.stream().anyMatch(at -> world.getBlockState(at).isOf(Blocks.STRIPPED_OAK_LOG)),
                "o arco do deserto não tem a madeira da vila");
        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(),
                "o arco trocado não é reconhecido");

        // O arco de antes da regra, de pinheiro, continua sendo o arco desta boca.
        for (BlockPos at : arch) {
            BlockState state = world.getBlockState(at);

            if (state.isOf(Blocks.STRIPPED_OAK_LOG)) {
                world.setBlockState(at, Blocks.STRIPPED_SPRUCE_LOG.getStateWithProperties(state), Block.FORCE_STATE);
            }
        }

        context.assertTrue(MineGate.standingAtMouth(world, mouth, Direction.SOUTH).isPresent(),
                "o arco antigo de pinheiro deixou de ser reconhecido");
    }

    /** O curral grande da taiga pede cerca de pinheiro; o curral de carvalho já de pé conta como de pé. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "biome_blueprints", tickLimit = 40)
    public void theTaigaLargePenHasSpruceFencesAndTheOakOneStillStands(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos center = context.getAbsolutePos(new BlockPos(1, 1, 1)).add(1_000_000, 0, 3_400_000);

        village(context, center, "minecraft:taiga");

        context.waitAndRun(3, () -> {
            ColonyFixture fixture = ColonyFixture.colonyAtWorld(center);
            Colony colony = fixture.colony();
            ColonyPos origin = MinecraftTypeAdapter.toColonyPos(center.add(4, 0, 4));

            try {
                Blueprint pen = BlueprintPlacement.blueprintOf(world, colony.id(), LARGE_PEN, origin).orElseThrow();
                List<String> blocks = pen.blocks().stream().map(block -> block.block().path()).toList();

                context.assertTrue(blocks.contains("spruce_fence") && blocks.contains("spruce_fence_gate"),
                        "o curral grande da taiga não tem cerca de pinheiro: " + pen.materials());
                context.assertTrue(blocks.stream().noneMatch(path -> path.startsWith("oak_")),
                        "o curral grande da taiga ainda pede carvalho: " + pen.materials());

                // O arquivo do estilo fica como está: o armazém da planície segue de carvalho na taiga.
                Blueprint storage = BiomeBlueprints.styledAt(world, colony.center(),
                        StructureBlueprintReader.read(world, PLAINS_STORAGE).orElseThrow());
                context.assertTrue(storage.blocks().stream().anyMatch(block -> block.block().path().equals("oak_planks")),
                        "o arquivo do estilo da planície foi trocado");

                // O curral erguido antes, de carvalho: cada cerca dele faz o papel da cerca de pinheiro da planta.
                int fences = 0;
                for (BlueprintBlock block : pen.blocks()) {
                    if (block.block().path().equals("spruce_fence")) {
                        world.setBlockState(MinecraftTypeAdapter.toBlockPos(new ColonyPos(origin.x() + block.offset().x(),
                                origin.y() + block.offset().y(), origin.z() + block.offset().z())),
                                Blocks.OAK_FENCE.getDefaultState(), Block.FORCE_STATE);
                        fences++;
                    }
                }

                int standing = BlueprintPlacement.standing(world, pen, origin);
                context.assertTrue(fences > 0 && standing >= fences,
                        "o curral de carvalho conta " + standing + " de " + fences + " cercas de pé");
            } finally {
                fixture.cleanUp();
                clear(world, center);
            }

            context.complete();
        });
    }

    /** Chão plano e três camas nele: é pelo bioma das camas que a vila sabe o estilo (VillageStyle). */
    private static void village(TestContext context, BlockPos center, String biome) {
        ServerWorld world = context.getWorld();

        // Presos na memória até o fim: no servidor de teste o chunk sem ticket sai e volta com o bioma gerado.
        forced(world, center, true);

        for (BlockPos at : BlockPos.iterate(center.add(-24, -2, -24), center.add(24, 10, 24))) {
            world.setBlockState(at, at.getY() < center.getY() ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(),
                    Block.FORCE_STATE);
        }

        for (int dx : new int[] {-12, 0, 12}) {
            BlockPos foot = center.add(dx, 0, -14);

            world.setBlockState(foot, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH));
            world.setBlockState(foot.north(), Blocks.RED_BED.getDefaultState()
                    .with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
        }

        biome(world, center, biome);
    }

    private static void forced(ServerWorld world, BlockPos center, boolean forced) {
        for (int cx = (center.getX() - 24) >> 4; cx <= (center.getX() + 24) >> 4; cx++) {
            for (int cz = (center.getZ() - 24) >> 4; cz <= (center.getZ() + 24) >> 4; cz++) {
                world.setChunkForced(cx, cz, forced);
            }
        }
    }

    /** O bioma pedido em volta do centro. */
    private static void biome(ServerWorld world, BlockPos center, String biome) {
        // O /fillbiome recusa mais de 32.768 blocos: a área cobre as camas e o centro, que é o que o estilo lê.
        BlockPos from = center.add(-16, -4, -16);
        BlockPos to = center.add(16, 8, 16);

        try {
            world.getServer().getCommandManager().getDispatcher().execute(
                    "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " "
                            + to.getX() + " " + to.getY() + " " + to.getZ() + " " + biome,
                    world.getServer().getCommandSource().withSilent());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException failed) {
            throw new AssertionError("fillbiome: " + failed.getMessage());
        }
    }

    private static void clear(ServerWorld world, BlockPos center) {
        for (BlockPos at : BlockPos.iterate(center.add(-24, -2, -24), center.add(24, 10, 24))) {
            world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.FORCE_STATE | Block.SKIP_DROPS);
        }

        forced(world, center, false);
    }
}
