package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.SweepPersistence;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.UUID;

/**
 * A busca de luz da rua não carrega chunk — PROJECT_AUDIT.md, R2/T2. Ler um bloco de chunk descarregado
 * ({@code World.getBlockState} → {@code getChunk(x, z, FULL)}) carrega o chunk na hora, na thread do servidor.
 * O cenário fica longe das arenas: um chunk carregado sozinho, e a busca saindo pela borda dele para o vizinho
 * descarregado.
 */
public class StreetLightChunkGameTest implements FabricGameTest {

    /** Longe das arenas da bateria, onde nada mais carrega chunk. */
    private static final int FAR = 30_000;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_light_chunk", tickLimit = 20)
    public void aStreetLightNeverLoadsAChunk(TestContext context) {
        ServerWorld world = context.getWorld();
        int offset = (int) (Math.abs(context.getAbsolutePos(BlockPos.ORIGIN).asLong()) % 64) * 4;
        ChunkPos home = new ChunkPos(FAR + offset, FAR);
        ChunkPos east = new ChunkPos(home.x + 1, home.z);

        context.assertTrue(!world.isChunkLoaded(east.x, east.z), "o vizinho já estava carregado: o teste não mede nada");

        // Só o chunk de casa: o vizinho fica em geração parcial, que não conta como carregado.
        world.getChunk(home.x, home.z);
        context.assertTrue(world.isChunkLoaded(home.x, home.z), "o chunk de casa devia estar carregado");
        context.assertTrue(!world.isChunkLoaded(east.x, east.z), "carregar a casa carregou o vizinho");

        // A coluna de rua na borda leste: a busca de 6 blocos entra no vizinho.
        BlockPos street = new BlockPos(home.getEndX(), world.getSeaLevel(), home.getCenterZ());

        VillageHelpers.lightNear(world, street);

        context.assertTrue(!world.isChunkLoaded(east.x, east.z),
                "a busca de luz carregou o chunk vizinho " + east + " a partir da rua em " + street.toShortString());
        context.complete();
    }

    /**
     * O lugar da lanterna também: o lado da rua no chunk vizinho descarregado, ou na borda dele, é pulado — pôr a
     * lanterna avisa os vizinhos, e o aviso os leria. A lanterna vai para um lado dentro da área carregada.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "street_light_chunk", tickLimit = 20)
    public void theLanternSpotNeverLoadsAChunk(TestContext context) {
        ServerWorld world = context.getWorld();
        int offset = (int) (Math.abs(context.getAbsolutePos(BlockPos.ORIGIN).asLong()) % 64) * 4;
        ChunkPos home = new ChunkPos(FAR + offset, FAR + 8);
        ChunkPos east = new ChunkPos(home.x + 1, home.z);

        context.assertTrue(!world.isChunkLoaded(east.x, east.z), "o vizinho já estava carregado: o teste não mede nada");
        world.getChunk(home.x, home.z);
        context.assertTrue(!world.isChunkLoaded(east.x, east.z), "carregar a casa carregou o vizinho");

        int x = home.getEndX();
        int z = home.getCenterZ();
        BlockPos above = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        // O lado norte é tirado da escolha: a busca segue para o leste, que fica no vizinho descarregado. Sem aviso
        // aos vizinhos nem atualização de forma: na borda do chunk, os dois leriam o vizinho e o carregariam.
        world.setBlockState(above.north(), Blocks.STONE.getDefaultState(),
                net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
        context.assertTrue(!world.isChunkLoaded(east.x, east.z), "o cenário carregou o vizinho");

        UUID colonyId = UUID.randomUUID();
        Colony colony = Colony.create(colonyId, MinecraftTypeAdapter.toColonyPos(above));
        SweepPersistence.restore(new ColonyRoads(colonyId, colony.center(), List.of(ColonyRoads.column(x, z))));
        VillagerEntity smelter = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));

        try {
            int lit = VillageHelpers.lightTheStreet(world, colony, smelter);

            context.assertTrue(!world.isChunkLoaded(east.x, east.z),
                    "o lugar da lanterna carregou o chunk vizinho " + east + " a partir da rua em " + above.toShortString());
            // O sul também fica na borda do chunk: a lanterna vai para o oeste, com os quatro vizinhos carregados.
            context.assertTrue(lit == 1 && world.getBlockState(above.west()).isOf(Blocks.LANTERN),
                    "a lanterna devia ir para o lado oeste: " + world.getBlockState(above.west()));
        } finally {
            BuildSiteScanner.clear(colonyId);
        }

        context.complete();
    }
}
