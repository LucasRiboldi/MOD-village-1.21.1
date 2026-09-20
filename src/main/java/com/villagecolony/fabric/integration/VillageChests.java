package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.model.VillageChestRule;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Os baús que estão na vila e não são de ninguém — 2026-09-16.
 *
 * <p><b>Pedido do autor:</b> <i>"permitir que o recurso que falta possa
 * ser recolhido de qualquer baú que esteja na vila automaticamente"</i>.
 *
 * <p>Até aqui {@code ColonyChests} só conhecia os baús reivindicados por
 * um trabalhador, mais o da boca da mina. Um baú que o jogador pusesse na
 * vila, cheio do material que a obra espera, era invisível — e a obra
 * ficava em {@code WAITING_RESOURCES} do lado dele. O log de 01:19 mostrou
 * a biblioteca esperando {@code lectern} nessa situação.
 *
 * <p><b>O baú nomeado fica de fora</b>, e é a ressalva que o autor
 * escolheu. Ver {@link VillageChestRule}: nomear exige bigorna e é ato
 * deliberado, então é a saída de quem quer guardar as próprias coisas.
 *
 * <p><b>Por chunk, e não por bloco</b> — o mesmo caminho do
 * {@code ChestScanner}. Um raio de 64 são 16.641 colunas se percorrido
 * bloco a bloco; pelos {@code blockEntities} de cada chunk são nove
 * chunks e uma lista curta em cada. É a diferença entre caber no tique e
 * travar o servidor, que é erro que este projeto já cometeu duas vezes.
 *
 * <p>Chunk descarregado é pulado, nunca carregado à força — §11.
 */
public final class VillageChests {

    private VillageChests() {
    }

    /**
     * Os baús livres da vila, a partir do centro dela.
     *
     * <p>Livre quer dizer: não é de trabalhador nenhum (esses o
     * {@code ColonyChests} já conhece) e não foi nomeado pelo jogador.
     *
     * @param known os que a colônia já conhece, para não repetir
     */
    public static List<ColonyPos> around(
            ServerWorld world, ColonyPos centre, List<ColonyPos> known) {

        List<ColonyPos> found = new ArrayList<>();

        int radius = VillageDetector.SEARCH_RADIUS;

        int minChunkX = (centre.x() - radius) >> 4;
        int maxChunkX = (centre.x() + radius) >> 4;
        int minChunkZ = (centre.z() - radius) >> 4;
        int maxChunkZ = (centre.z() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);

                if (chunk == null) {
                    // Fora de memória. Pedir o chunk aqui forçaria
                    // carregamento dentro do tique — §11.
                    continue;
                }

                collectFrom(world, chunk, centre, radius, known, found);
            }
        }

        return found;
    }

    /** Os baús livres deste chunk que caem dentro do raio. */
    private static void collectFrom(
            ServerWorld world,
            WorldChunk chunk,
            ColonyPos centre,
            int radius,
            List<ColonyPos> known,
            List<ColonyPos> found) {

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (!(entry.getValue() instanceof ChestBlockEntity chest)) {
                continue;
            }

            BlockPos pos = entry.getKey();

            if (!isWithin(centre, pos, radius)) {
                continue;
            }

            if (!isIndoors(world, pos)) {
                // <b>"Na vila" quer dizer DENTRO de uma casa</b> — decisão
                // do autor, 2026-09-16. Um raio solto alcançava o baú da
                // vila vizinha e, no gametest, o das arenas ao lado: nove
                // casos quebraram dizendo "8 chests read" onde esperavam
                // dois, e um deles chegou a construir com o baú vazio
                // porque lia o baú de outro teste.
                //
                // Dentro de casa é o que o jogador entende por "na vila", e
                // é verificável: a peça de vila que o jogo gerou, ou a obra
                // que a colônia levantou. Baú no meio do campo fica de
                // fora, e o do acampamento dele também.
                continue;
            }

            ColonyPos at = MinecraftTypeAdapter.toColonyPos(pos);

            if (known.contains(at) || found.contains(at)) {
                continue;
            }

            // Os oito baús da BigHouseMOD pertencem aos moradores dela.
            // Sem este filtro, a casa nova vira estoque público antes que
            // a fundação termine de registrar os oito trabalhadores.
            if (BigHouseFoundation.containsHouseBlock(pos)) {
                continue;
            }

            if (!VillageChestRule.mayTake(nameOf(chest))) {
                // Nomeado pelo jogador: é dele, e a colônia passa longe.
                continue;
            }

            found.add(at);
        }
    }

    /**
     * O nome de exibição deste baú, se ele tem um.
     *
     * <p>{@code getCustomName} devolve nulo para o baú comum, e é
     * exatamente a distinção que a regra precisa — o nome padrão do jogo
     * só apareceria se perguntássemos por {@code getDisplayName}.
     */
    private static Optional<String> nameOf(ChestBlockEntity chest) {
        return Optional.ofNullable(chest.getCustomName()).map(name -> name.getString());
    }

    /**
     * Se este baú está dentro de alguma construção da vila.
     *
     * <p>As duas origens que a {@code BlockProtection} já distingue: a
     * peça que o jogo gerou com a vila, e a casa que a colônia levantou.
     * Reusa a mesma pergunta para não haver duas verdades sobre o que é
     * construção — a discordância que este projeto já pagou em 2026-09-10.
     */
    private static boolean isIndoors(ServerWorld world, BlockPos pos) {
        return BlockProtection.isVillageOriginal(world, pos)
                || BlockProtection.isColonyBuilt(pos);
    }

    /** Horizontal, como todo raio de vila neste projeto. */
    private static boolean isWithin(ColonyPos centre, BlockPos pos, int radius) {
        long dx = (long) pos.getX() - centre.x();
        long dz = (long) pos.getZ() - centre.z();

        return dx * dx + dz * dz <= (long) radius * radius;
    }
}
