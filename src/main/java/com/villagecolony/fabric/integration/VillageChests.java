package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.model.VillageChestRule;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 *
 * <p><b>A vila é a caixa, e todo baú dentro dela conta</b> — 2026-10-01,
 * pedido do autor: <i>"aumentar a zona de varredura dos baús para analisar
 * todos baús dentro da vila, evitar baús fora de alcance"</i>. Com a vila
 * medida (ADR-003 Emenda 6), a varredura cobre a caixa inteira — e não um
 * raio de 64 em volta de um centro que oscila — e só ela: baú fora da caixa
 * está fora de alcance. Dentro da caixa vale o baú de dentro ou de fora de
 * casa: a regra "só dentro de casa" de 09-16 existia porque o raio solto
 * alcançava a vila vizinha e as arenas do teste, e a caixa já separa as duas.
 * Na altura, a mesma janela das camas da vila — o baú de masmorra embaixo
 * dela não é da vila. Vila ainda não medida continua no raio, só dentro de
 * casa.
 *
 * <p><b>Baú de trabalhador não é livre, de nenhuma colônia.</b> A lista
 * {@code known} só trazia os desta colônia, e o baú do trabalhador da
 * colônia vizinha entrava aqui como estoque público — nos testes de
 * carpinteiro e pedreiro, a 13 blocos um do outro, cada colônia lia e
 * gastava o baú da outra.
 */
public final class VillageChests {

    static {
        ServerMemory.register(VillageChests.class, VillageChests::clearAll);
    }

    /**
     * Por quantos tiques uma varredura vale — 2026-09-24.
     *
     * <p><b>Por que existe.</b> O perfil do spark de 2026-09-24 pôs o
     * {@code ColonyChests.nearestFirst} como o maior custo do mod: cada
     * chamada relia os nove por nove chunks em volta do centro e, para
     * cada baú, os blocos em volta da porta ({@code isIndoors},
     * {@code isPrivateBedChest}). E é chamado a cada bloco que o
     * construtor assenta.
     *
     * <p><b>Por que um segundo.</b> Não é número de playtest: é o teto do
     * que pode ficar velho. Baú posto ou quebrado invalida na hora (ver
     * {@link #register}); nome, BigHouse e chunk descarregado são
     * reconferidos a cada leitura. O que só este prazo cobre é o baú que
     * <i>vira</i> de dentro de casa — a obra terminou em volta dele — ou
     * deixa de ser baú de cama. Um segundo de atraso nisso não muda
     * decisão nenhuma, que o ciclo da colônia é de trinta.
     */
    static final int CACHE_TICKS = 20;

    /** Onde uma varredura foi feita: mundo, centro da colônia e a caixa, se medida. */
    private record Scan(RegistryKey<World> world, ColonyPos centre, @Nullable VillageBounds box) {
    }

    /** Os baús de dentro de casa que a varredura achou, e quando. */
    private record Cached(long at, long generation, List<BlockPos> chests) {
    }

    private static final Map<Scan, Cached> cache = new HashMap<>();

    /** Sobe a cada baú que entra ou sai do mundo; varredura anterior vira lixo. */
    private static long generation;

    private VillageChests() {
    }

    /**
     * Esquece a varredura quando um baú entra ou sai de qualquer mundo.
     *
     * <p>O Fabric dispara os dois eventos em {@code WorldChunk.setBlockEntity}
     * e {@code removeBlockEntity}: baú posto, baú quebrado e chunk que
     * carrega com baú dentro. Sem isto o baú que o jogador acabou de pôr
     * ficaria invisível até o prazo vencer, e o gametest que monta a arena
     * e já pergunta leria a arena de antes.
     */
    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(VillageChests::onChestChange);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(VillageChests::onChestChange);
    }

    private static void onChestChange(BlockEntity blockEntity, ServerWorld world) {
        if (blockEntity instanceof ChestBlockEntity) {
            generation++;
        }
    }

    public static void clearAll() {
        cache.clear();
        generation = 0;
    }

    /**
     * Os baús livres da vila: dentro da caixa dela, ou no raio em volta do
     * centro enquanto ela não foi medida.
     *
     * <p>Livre quer dizer: não é de trabalhador nenhum, desta colônia ou de
     * outra, e não foi nomeado pelo jogador.
     *
     * <p>A varredura cara sai de {@link #CACHE_TICKS}; o que é barato e
     * pode mudar a qualquer momento é conferido de novo aqui, baú por baú.
     *
     * @param known os que a colônia já conhece, para não repetir
     */
    public static List<ColonyPos> around(
            ServerWorld world, Colony colony, List<ColonyPos> known) {

        List<ColonyPos> found = new ArrayList<>();
        Set<ColonyPos> claimed = new HashSet<>();

        for (WorkerStorage storage : VillageColonyMod.STORAGES.all()) {
            claimed.add(storage.chestPosition());
        }

        for (BlockPos pos : villageChests(world, colony.center(), colony.bounds().orElse(null))) {
            WorldChunk chunk = world.getChunkManager()
                    .getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk == null) {
                // Descarregou depois da varredura — §11, nunca carregar.
                continue;
            }

            if (!(chunk.getBlockEntities().get(pos) instanceof ChestBlockEntity chest)) {
                continue;
            }

            ColonyPos at = MinecraftTypeAdapter.toColonyPos(pos);

            if (known.contains(at) || found.contains(at) || claimed.contains(at)) {
                continue;
            }

            // Os sete baús da BigHouseMOD pertencem aos moradores dela.
            // Sem este filtro, a casa nova vira estoque público antes que
            // a fundação termine de registrar os sete trabalhadores.
            if (BigHouseFoundation.containsHouseBlock(pos)) {
                continue;
            }

            if (!VillageChestRule.mayTake(nameOf(chest))) {
                // Nomeado pelo jogador: é dele, e a colônia passa longe.
                continue;
            }

            found.add(at);
        }

        return found;
    }

    /** A varredura cara, vinda do cache enquanto ele vale. */
    private static List<BlockPos> villageChests(ServerWorld world, ColonyPos centre, @Nullable VillageBounds box) {
        long now = world.getTime();
        Scan key = new Scan(world.getRegistryKey(), centre, box);
        Cached hit = cache.get(key);

        if (hit != null && hit.generation() == generation && now - hit.at() < CACHE_TICKS) {
            return hit.chests();
        }

        // O centro oscila entre oito posições, e cada uma vira chave nova.
        // Varrer o vencido aqui mantém o mapa do tamanho do último segundo.
        cache.values().removeIf(old -> old.generation() != generation
                || now - old.at() >= CACHE_TICKS);

        List<BlockPos> chests = box == null ? scan(world, centre) : scan(world, box);

        cache.put(key, new Cached(now, generation, List.copyOf(chests)));

        return chests;
    }

    private static List<BlockPos> scan(ServerWorld world, ColonyPos centre) {
        List<BlockPos> found = new ArrayList<>();

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

                collectFrom(world, chunk, centre, radius, found);
            }
        }

        return found;
    }

    /**
     * A vila medida: os chunks que a caixa cobre, e nela os baús na janela de
     * altura das camas — dentro ou fora de casa, que a caixa já é a vila.
     */
    private static List<BlockPos> scan(ServerWorld world, VillageBounds box) {
        List<BlockPos> found = new ArrayList<>();

        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);

                if (chunk == null) {
                    continue;
                }

                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();

                    if (entry.getValue() instanceof ChestBlockEntity && isInside(box, pos)
                            && !VanillaBedChests.isPrivateBedChest(world, pos)) {
                        found.add(pos.toImmutable());
                    }
                }
            }
        }

        return found;
    }

    /**
     * Se este baú está na vila medida: na coluna da caixa e na janela de
     * altura das camas dela. Fora disso ele está fora de alcance.
     */
    public static boolean isInside(VillageBounds box, BlockPos pos) {
        return box.containsColumn(pos.getX(), pos.getZ())
                && pos.getY() >= box.bedFloor() && pos.getY() <= box.bedCeiling();
    }

    /** O mesmo, para a posição que a colônia guarda. */
    public static boolean isInside(VillageBounds box, ColonyPos pos) {
        return box.containsColumn(pos.x(), pos.z())
                && pos.y() >= box.bedFloor() && pos.y() <= box.bedCeiling();
    }

    /**
     * Os baús deste chunk que caem no raio, estão dentro de casa e não são
     * baú de cama — as perguntas caras, que leem blocos em volta.
     */
    private static void collectFrom(
            ServerWorld world,
            WorldChunk chunk,
            ColonyPos centre,
            int radius,
            List<BlockPos> found) {

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            if (!(entry.getValue() instanceof ChestBlockEntity)) {
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

            if (VanillaBedChests.isPrivateBedChest(world, pos)) {
                continue;
            }

            // Cópia imutável: a chave do mapa do chunk pode ser mutável.
            found.add(pos.toImmutable());
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
