package com.villagecolony.fabric.integration;

import com.villagecolony.fabric.integration.SweepState.RoadScan;
import com.villagecolony.fabric.integration.SweepState.Sweep;
import com.villagecolony.fabric.integration.BuildSiteScanner.Site;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.ScanRefusalReason;
import com.villagecolony.core.coordination.ScanReport;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.PlanPlacement;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * O chão de uma coluna, e se ele é chão natural do bioma e serve de lote — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class LotGround {

    private LotGround() {
    }

    /**
     * O bloco de chão no alto desta coluna, dentro da janela da vila.
     *
     * <p><b>Não usa o mapa de alturas</b>, e a razão foi medida: a arena
     * do gametest é fechada por barreiras, e {@code MOTION_BLOCKING}
     * devolve o teto de barreira — oito blocos acima da grama. Num mundo
     * de verdade o mapa daria a superfície e estaria certo; num mundo
     * fechado dá o teto, e o código que confiasse nele procuraria lote
     * dentro da laje.
     *
     * <p>A janela também é uma decisão, e é a quarta desta fase: o lote
     * tem de estar entre {@link CactusPatch#WINDOW_UP} acima e {@link CactusPatch#WINDOW_DOWN}
     * abaixo do nível do centro da colônia. Vila não constrói no alto do
     * morro que a olha de cima, e a janela é o que torna a busca barata
     * — uma coluna custa poucas leituras, e não uma varredura do céu ao
     * bedrock.
     */
    static Optional<BlockPos> groundInColumn(
            ServerWorld world, int x, int z, int aroundY) {

        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            // Chunk descarregado. Pedir por ele aqui forçaria
            // carregamento dentro do tick — o defeito que travou o
            // servidor duas vezes neste projeto (§11).
            return Optional.empty();
        }

        // Qualquer coisa acima da janela reprova a coluna inteira, e não
        // é detalhe: sem esta pergunta a janela *recorta* o morro. Uma
        // torre de quatro blocos era lida como dois — a altura do teto
        // da janela — e um lote com desnível de quatro passava pelo
        // limite de dois. A casa nasceria enfiada na encosta. Achado
        // pelo gametest do desnível, em 2026-08-14.
        //
        // Efeito colateral assumido: lote com árvore em cima é recusado,
        // porque o tronco está acima da janela. Conservador de
        // propósito — a colônia procura outro lugar em vez de derrubar
        // o que não planejou.
        if (!isNothing(chunk.getBlockState(new BlockPos(x, aroundY + LotLevel.WINDOW_UP + 1, z)))) {
            return Optional.empty();
        }

        for (int y = aroundY + LotLevel.WINDOW_UP; y >= aroundY - LotLevel.WINDOW_DOWN; y--) {
            BlockPos pos = new BlockPos(x, y, z);

            if (!isNothing(chunk.getBlockState(pos))) {
                return Optional.of(pos);
            }
        }

        return Optional.empty();
    }

    /**
     * Se este bloco não conta como obstáculo para achar o chão.
     *
     * <p>Ar, e a cobertura do campo: grama, samambaia, flor, camada de
     * neve. A TASK-047, e o motivo dela está numa sessão inteira.
     *
     * <p>Em 2026-08-15, 00:42, duas colônias varreram o raio de 64 blocos
     * até o fim, duas vezes cada, e não acharam um lote — em duas vilas
     * de planície rodeadas de campo aberto. A causa: este laço devolvia o
     * bloco mais alto que não fosse ar, e em planície esse bloco é o tufo
     * de grama. {@code flatGroundAt} então recusava a coluna, porque tufo
     * não é chão. Um lote de sete por sete precisa das quarenta e nove
     * colunas limpas, e em planície nenhuma está.
     *
     * <p>Construction-System.md §PREPARING sempre mandou limpar grama,
     * flor e neve. O código pulava esse estado alegando que o lote só é
     * aceito quando não há nada em cima dele — e a alegação era verdadeira
     * e era exatamente o defeito.
     *
     * <p><b>Folha fica de fora, e é decisão.</b> O documento a lista, mas
     * aceitar folha como nada faria a colônia escolher lote debaixo de
     * copa — e a casa nasceria dentro da árvore. O guarda da janela pega
     * o tronco, não a copa baixa. Conservador de propósito, como a recusa
     * de lote com árvore em cima logo acima.
     *
     * <p>Quem limpa é o próprio construtor, sem código novo: ele escreve
     * o bloco no lugar, e o que estava ali sai. O que sobra é a moita
     * dentro de cômodo cujo projeto pede ar — o projeto não escreve nada
     * ali, e a grama fica. É cosmético e está registrado no §13.
     */
    static boolean isNothing(BlockState state) {
        return state.isAir()
                || state.isReplaceable()
                || state.isIn(BlockTags.REPLACEABLE)
                || state.isIn(BlockTags.SMALL_FLOWERS);
    }

    /**
     * Se dá para assentar uma casa sobre este bloco.
     *
     * <p>Chão de vila, e não qualquer bloco sólido: pedra à mostra,
     * madeira e lã são, respectivamente, montanha, casa e casa de
     * alguém. O caminho de terra fica de fora de propósito — a casa
     * encosta na rua, não sobe em cima dela.
     */
    static boolean isNaturalGround(BlockState state) {
        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.PODZOL)
                || state.isIn(BlockTags.SAND);
    }

    /**
     * Se este bloco é o chão do bioma, e não peça que o gerador pôs —
     * P1.3, 2026-09-18.
     *
     * <p><b>O número que decidiu.</b> Playtest num mundo só de deserto:
     * 16.016 colunas, <b>zero aprovadas</b>, 69% recusadas pela Regra 3
     * contra 19% na planície. A amostra de
     * {@code ProtectionSample} disse de que eram feitas:
     *
     * <pre>
     * 5696 smooth_sandstone;  5403 sand;  1548 chest;  2 oak_log
     * </pre>
     *
     * <p><b>87% é areia e arenito</b> — o terreno em que a vila foi
     * assentada. O gerador de vilas inclui o chão na caixa da estrutura,
     * e no deserto esse chão é quase tudo que existe. Proibir construir
     * sobre ele é proibir a colônia de trabalhar dentro da própria vila,
     * que é justamente o que o javadoc de
     * {@code BlockProtection.isVillageOriginal} diz que não deve
     * acontecer.
     *
     * <p><b>Por que não bastava {@link #isNaturalGround}.</b> Ele já
     * aceita areia pela etiqueta {@code SAND}, e <b>não</b> aceita
     * {@code smooth_sandstone}, que é 45% da amostra: arenito liso é
     * pedra, e para quem procura chão de lote ele continua não sendo
     * solo. A pergunta aqui é outra — <i>isto é peça de construção ou é
     * o material de que este bioma é feito?</i> — e por isso o predicado
     * é próprio em vez de um alargamento daquele, que mudaria a resposta
     * de quem pergunta "dá para assentar casa aqui?".
     *
     * <p><b>Pela etiqueta do jogo onde existe etiqueta</b> — a ADR-009.
     * {@code BASE_STONE_OVERWORLD} traz pedra, granito, diorito e
     * andesito; {@code TERRACOTTA} cobre o barro cozido dos ermos;
     * {@code SAND} e {@code DIRT} vêm de {@link #isNaturalGround}.
     *
     * <p><b>O arenito é a exceção, e ela é do jogo e não deste mod:</b>
     * <b>não existe</b> etiqueta de arenito em 1.21.1 — conferido no
     * {@code BlockTags} do jar mapeado, que tem {@code SAND},
     * {@code TERRACOTTA} e {@code BASE_STONE_OVERWORLD} e nenhuma
     * família de {@code sandstone}. Os seis nomes ficam escritos porque
     * a alternativa seria pior: casar por substring {@code "sandstone"}
     * pegaria escada, laje e muro de arenito, que <b>são</b> peça de
     * construção — exatamente o que esta pergunta precisa continuar
     * recusando.
     *
     * <p><b>A Regra 3 continua inteira para o que ela existe:</b> baú,
     * tronco, porta, cama — peça posta pelo gerador — seguem intocáveis.
     */
    public static boolean isBiomeGround(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        return isNaturalGround(state)
                || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isIn(BlockTags.TERRACOTTA)
                || state.isOf(Blocks.SANDSTONE)
                || state.isOf(Blocks.SMOOTH_SANDSTONE)
                || state.isOf(Blocks.CUT_SANDSTONE)
                || state.isOf(Blocks.RED_SANDSTONE)
                || state.isOf(Blocks.SMOOTH_RED_SANDSTONE)
                || state.isOf(Blocks.CUT_RED_SANDSTONE)
                || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.SNOW_BLOCK)
                || state.isOf(Blocks.PACKED_ICE);
    }

    /**
     * Se a coluna sólida pode sustentar um lote no P0.7.
     *
     * <p>A composição não identifica origem: terreno natural e preparo do
     * jogador são elegíveis. Proteção, construção existente, {@code ROAD_AREA},
     * nível e volume já foram verificados antes desta pergunta.
     */
    static boolean isLotGround(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isSolidBlock(world, pos);
    }
}
