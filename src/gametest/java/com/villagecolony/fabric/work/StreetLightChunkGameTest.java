package com.villagecolony.fabric.work;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

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
}
