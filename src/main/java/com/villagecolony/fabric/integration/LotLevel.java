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
 * Se o chão de um lote está nivelado o bastante para a planta, com a janela de altura e a tolerância em relação à rua — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class LotLevel {

    private LotLevel() {
    }

    /**
     * Quanto acima do nível da colônia ainda se procura chão.
     *
     * <p>Apertado, e igual ao {@link #MAX_SLOPE} por coerência: um lote
     * mais alto que isso é morro, não continuação da vila. Se em jogo
     * ficar apertado demais, é uma constante — e o sintoma será claro,
     * "não achou lote" com terreno visivelmente bom em volta.
     *
     * <p>Também é o que mantém a busca fora do céu. A arena do gametest
     * é fechada por barreira oito blocos acima do chão, e uma janela
     * larga para cima encontrava o teto em vez do terreno.
     */
    static final int WINDOW_UP = BuildSiteScanner.MAX_SLOPE;

    /**
     * Quanto abaixo.
     *
     * <p>Mais folgado que para cima por dois motivos: o centro da
     * colônia vem das camas, que ficam no piso das casas, um ou dois
     * blocos acima da rua; e uma vila em encosta se estende morro
     * abaixo, não morro acima.
     */
    static final int WINDOW_DOWN = 8;

    /**
     * Quanto uma coluna do lote pode fugir do nível da rua.
     *
     * <p><b>Decisão do autor, 2026-09-15:</b> <i>"permitir somente 1 bloco
     * de desnivel da estrada"</i>.
     *
     * <p><b>A medição que autorizou a mudança.</b> A pesquisa de 09-11
     * ({@code docs/research/terraplanagem-da-vila.md} §8) registrou a regra
     * que o próprio autor impôs: <i>"Medir primeiro. Se a recusa por
     * desnível dominar, a inferência vira fato e a frente abre"</i>. Duas
     * sessões responderam, e o número é estável: <b>35,0%</b> e
     * <b>33,6%</b> das recusas de lote eram {@code OFF_ROAD_LEVEL},
     * a segunda maior causa atrás só da área de estrada.
     *
     * <p>A régua era <b>exata</b> — {@code ground.getY() != roadY} —, e num
     * terreno de planície ondulada isso reprova quase tudo. A Regra 19
     * mirava o lote <i>"dois blocos acima do caminho"</i>, a varanda sem
     * escada; um bloco é o degrau que um jogador sobe sem pensar, e o
     * Vanilla o trata assim em toda parte.
     *
     * <p><b>O que isto NÃO faz: mover terra.</b> A preparação do canteiro
     * tira planta e não aterra — ver {@code SitePreparation} —, então a
     * coluna um abaixo da rua fica com um vão de um bloco sob o piso, que
     * assenta em {@code roadY + 1}. O autor foi avisado e escolheu assim
     * para a vila voltar a crescer. Aterrar continua sendo a frente de
     * terraplanagem que a pesquisa desenhou (§6), e ela segue aberta.
     */
    static final int ROAD_LEVEL_TOLERANCE = 1;

    /**
     * Quanto da base precisa estar no nível exato da rua, em por cento.
     *
     * <p><b>Decisão do autor, 2026-09-19:</b> <i>"aceitar uma base da
     * construção que tenha mais de 90% dos blocos no mesmo nível
     * (tentando corrigir o fato de criar uma zona usando a altura de um
     * bloco porém todo resto da base estar acima do nível do solo,
     * construção fica voando)"</i>.
     *
     * <p>A {@link #ROAD_LEVEL_TOLERANCE} é <b>por coluna</b> e nada
     * exigia que as colunas concordassem entre si. Esta é a régua do
     * conjunto.
     *
     * <p><b>Medida ENTRE AS COLUNAS, e não contra a rua</b> — segunda
     * decisão do autor no mesmo dia, e ela é o que faz a regra
     * funcionar. Medir contra a rua reprovaria o lote inteiro um bloco
     * acima dela, que é o caso que o próprio autor mandou <b>aceitar</b>
     * em 09-15 e que o {@code oneBlockOffTheRoadLevelIsStillALot}
     * protege.
     *
     * <p>E a casa passa a assentar no nível da <b>base</b>, não no da
     * rua. Sem essa metade a regra não consertaria nada: um lote todo um
     * acima tem 100% das colunas no mesmo nível, passaria, e a casa
     * continuaria assentando em {@code roadY + 1} — voando sobre o
     * próprio terreno.
     */
    static final int LEVEL_BASE_PERCENT = 90;

    /**
     * A altura em que a casa assenta, se este lote servir.
     *
     * <p>Serve quando todas as colunas dele são chão natural, o desnível
     * cabe em {@link #MAX_SLOPE}, e nada ali é peça de vila ou coisa que
     * o jogador pôs — a Regra 3 vale para escolher lugar tanto quanto
     * para quebrar bloco. Construir por cima da casa de alguém seria a
     * pior forma de desobedecê-la.
     *
     * @return a altura do nível-base mais comum do lote. A casa assenta
     *     sobre esse nível para que a base aceite a tolerância de desnível
     *     sem transformar um bloco elevado em espaço livre
     */
    static Optional<Integer> flatGroundAt(
            ServerWorld world, UUID colonyId, int originX, int originZ, int aroundY,
            int roadY, ColonyPos size) {

        // Quantas colunas em cada altura de chão — a conta da regra dos
        // 90%, decisão do autor de 2026-09-19. O nível é medido ENTRE AS
        // COLUNAS, e não contra a rua: ver o portão depois do laço.
        Map<Integer, Integer> groundLevels = new HashMap<>();

        for (int dx = 0; dx < size.x(); dx++) {
            for (int dz = 0; dz < size.z(); dz++) {
                int x = originX + dx;
                int z = originZ + dz;

                Optional<BlockPos> found = LotGround.groundInColumn(world, x, z, aroundY);

                if (found.isEmpty()) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.NO_GROUND);

                    return Optional.empty();
                }

                BlockPos ground = found.get();

                // <b>Do mais barato para o mais caro</b> — 2026-09-15. A
                // ordem desta fila é decisão de custo, e não de regra:
                // toda pergunta aqui reprova o lote inteiro, então a
                // resposta final não depende de quem pergunta primeiro —
                // só o preço de chegar nela depende.
                //
                // <b>O que o log do autor mediu:</b> ciclos de 98, 57 e 54
                // ms, acima do tique de 50 ms, com o planejador levando 72
                // ms do pior; 192.448 recusas num ciclo, das quais 126.315
                // pela Regra 3. A Regra 3 era a SEGUNDA pergunta da fila e
                // é a mais cara de todas — {@code isVillageOriginal}
                // consulta o {@code StructureAccessor} —, enquanto a
                // comparação de dois inteiros da Regra 19, que respondeu
                // por 24.350 recusas, era a SEXTA. Toda coluna reprovada
                // pela régua da rua pagava a consulta de estrutura antes
                // de chegar à comparação que a reprovaria de graça.
                //
                // <b>A guarda barata do isVillageOriginal não salvava o
                // caso</b>, e é o que torna a troca valiosa: ela sai cedo
                // quando o bloco não tem referência de estrutura nenhuma,
                // e dentro de uma vila os blocos têm — que é justamente
                // onde a colônia procura lote.
                //
                // <b>O que muda no log</b>, sem nada mudar no jogo: uma
                // coluna reprovável por mais de um motivo passa a ser
                // contada pelo motivo mais barato. Espere a Regra 3 cair e
                // a régua da rua subir. Ver
                // {@code theCheapRefusalAnswersBeforeTheExpensiveOne}.

                // A Regra 19: no nível da rua, e não apenas plano entre
                // si. Um lote inteiro dois blocos acima do caminho é
                // plano e é uma varanda sem escada — a porta da Regra 17
                // daria para o alto de um degrau que ninguém sobe.
                //
                // Primeira da fila por ser a única que não lê o mundo: o
                // chão já está na mão, e a pergunta é a comparação de dois
                // inteiros.
                if (Math.abs(ground.getY() - roadY) > ROAD_LEVEL_TOLERANCE) {
                    // A contagem que decide a terraplanagem — 2026-09-11.
                    // Ver docs/research/terraplanagem-da-vila.md.
                    LotRefusals.refused(colonyId, LotRefusals.Reason.OFF_ROAD_LEVEL);

                    return Optional.empty();
                }

                // Uma leitura de bloco, logo depois da comparação gratuita
                // acima — P1.3, 2026-09-23. Fica antes da Regra 3 e da
                // reserva de rua porque uma cama é sempre peça de vila e a
                // categoria mais específica; conferir aqui, e não junto com
                // {@code NOT_NATURAL_GROUND}, é o que mantém
                // {@code bedAndRoadRefusalsAreIndependent} separando as
                // duas telemetrias em vez de uma engolir a outra.
                if (world.getBlockState(ground).getBlock() instanceof BedBlock) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.BED);

                    return Optional.empty();
                }

                // <b>E quantas colunas estão no nível EXATO da rua</b> —
                // decisão do autor, 2026-09-19. A tolerância acima aceita
                // um bloco de degrau por coluna, e a casa assenta em
                // {@code roadY + 1} venha o que vier: um lote em que
                // <i>toda</i> coluna está um abaixo é aceito e a casa sai
                // <b>voando</b>, com um vão de um bloco sob o piso
                // inteiro. Era limite conhecido — o javadoc do
                // {@link #ROAD_LEVEL_TOLERANCE} o descreve — e o autor o
                // viu em jogo.
                //
                // A conta fecha depois do laço: <i>"aceitar uma base que
                // tenha mais de 90% dos blocos no mesmo nível"</i>.
                groundLevels.merge(ground.getY(), 1, Integer::sum);

                // <b>E as três do meio ficam na ordem em que sempre
                // estiveram</b>, de propósito. Custam a mesma coisa — uma
                // consulta em memória, uma leitura de bloco, uma leitura
                // de bloco —, então trocá-las não compra desempenho e
                // <b>estraga o diagnóstico</b>: pôr o {@code isLotGround}
                // na frente fez o caminho de terra reservado como estrada
                // passar a ser recusado por "não é solo natural", porque
                // {@code dirt_path} não é cubo inteiro. A recusa continuava
                // certa e o log passava a mentir sobre o motivo. Foi o
                // {@code everyReservedRoadMaterialBlocksTheWholeFootprint}
                // que pegou, na primeira tentativa desta mudança.

                // Consulta em memória, ao registro de obras da colônia.
                if (BlockProtection.isColonyBuilt(ground)) {
                    VolumeSample.colonyBuilt();

                    LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

                    return Optional.empty();
                }

                // Uma leitura de bloco e uma consulta ao índice de ruas, e
                // a guarda do isPaving sai cedo no caso comum.
                //
                // <b>A reserva é da rua que a colônia calçou</b> — P1.0,
                // 2026-09-17. Era o RoadIndex.isRoadArea, que conta o calçamento
                // original da vila como reserva; numa vila gerada isso é
                // 54,8% das recusas, e o playtest de 42 minutos fechou com
                // 960.672 colunas testadas e zero aprovadas. Ver
                // RoadIndex.isReservedAgainstLots — a Regra 3 continua adiante, e
                // bloco original que não é calçamento segue intocável.
                if (RoadIndex.isReservedAgainstLots(world, colonyId, ground)) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.ROAD);

                    return Optional.empty();
                }

                // Uma leitura do bloco que já está na mão.
                if (!LotGround.isLotGround(world, ground)) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.NOT_NATURAL_GROUND);

                    return Optional.empty();
                }

                // A Regra 3, e a pergunta mais cara da fila: por último,
                // depois de as baratas terem tirado o que podiam.
                //
                // <b>Mas o chão do bioma não é peça de vila</b> — P1.3,
                // 2026-09-18, e o número que decidiu veio do jogo. Num
                // mundo só de deserto: 16.016 colunas, <b>zero
                // aprovadas</b>, 69% recusadas aqui — contra 19% na
                // planície. A amostra disse de que eram feitas:
                //
                //   5696 smooth_sandstone; 5403 sand; 1548 chest; 2 oak_log
                //
                // <b>87% é areia e arenito</b>, que no deserto é o terreno
                // em que a vila foi assentada — o gerador inclui o chão na
                // caixa da estrutura. Proibir construir sobre o chão do
                // bioma é proibir a colônia de trabalhar dentro da própria
                // vila, que é literalmente o que o javadoc de
                // {@code isVillageOriginal} diz que não deve acontecer.
                //
                // <b>A Regra 3 continua inteira para o que ela existe.</b>
                // Baú, tronco, porta, cama — peça posta pelo gerador —
                // seguem intocáveis; o que deixa de recusar é o solo
                // natural. É o mesmo movimento de 09-15 em
                // {@code isReservedAgainstLots}, que separou calçamento de
                // reserva e abriu 313 lotes na planície.
                if (BlockProtection.isVillageOriginal(world, ground)
                        && !LotGround.isBiomeGround(world, ground)) {

                    LotRefusals.refused(colonyId, LotRefusals.Reason.PROTECTED);

                    return Optional.empty();
                }

                // A Regra 22 é aplicada depois que o nível-base comum for
                // conhecido. Conferir a partir do chão de cada coluna faria
                // um degrau isolado parecer apoio e deixaria o bloco dentro
                // da caixa real da obra.
            }
        }

        // <b>A base tem de estar quase toda no mesmo nível</b> —
        // decisão do autor, 2026-09-19: <i>"aceitar uma base da
        // construção que tenha mais de 90% dos blocos no mesmo
        // nível"</i>.
        //
        // <b>O que isto conserta, visto em jogo.</b> A tolerância de um
        // bloco é <b>por coluna</b>, e nada exigia que as colunas
        // concordassem entre si: um lote em que todas estão um abaixo da
        // rua passa inteiro, e a casa assenta em {@code roadY + 1} —
        // <b>voando</b>, com um vão de um bloco sob o piso todo. O
        // javadoc do ROAD_LEVEL_TOLERANCE já descrevia o vão como limite
        // conhecido; o autor o viu e mandou fechar.
        //
        // Noventa por cento, e não cem: o degrau isolado é o que a
        // tolerância existe para aceitar, e exigir o lote perfeito
        // devolveria os 35% de recusa por desnível que a régua exata
        // produzia.
        int columns = size.x() * size.z();

        int mostCommon = groundLevels.values().stream().mapToInt(Integer::intValue).max()
                .orElse(0);

        if (mostCommon * 100 < columns * LEVEL_BASE_PERCENT) {
            LotRefusals.refused(colonyId, LotRefusals.Reason.OFF_ROAD_LEVEL);

            return Optional.empty();
        }

        // <b>E a casa assenta no nível da BASE, e não no da rua</b> — é a
        // outra metade da decisão, e sem ela a regra dos 90% não
        // consertaria nada: um lote inteiro um acima da rua tem 100% das
        // colunas no mesmo nível e passaria, e a casa continuaria
        // assentando em {@code roadY + 1} — <b>voando</b> sobre o próprio
        // terreno.
        int baseY = groundLevels.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(roadY);

        // A caixa real da obra começa no piso comum, não no chão de cada
        // coluna. Assim, degraus, blocos voando e restos de outra construção
        // dentro da pegada não podem ser confundidos com terreno de apoio.
        for (int dx = 0; dx < size.x(); dx++) {
            for (int dz = 0; dz < size.z(); dz++) {
                if (!LotClearance.isClearAbove(
                        world,
                        originX + dx,
                        originZ + dz,
                        baseY + 1,
                        Math.max(size.y(), BuildSiteScanner.VERTICAL_CLEARANCE))) {
                    LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

                    return Optional.empty();
                }
            }
        }

        // <b>E a caixa inteira não pode pisar em casa que já existe</b> —
        // 2026-09-19, visto em jogo: uma obra nova nasceu EM CIMA de uma
        // casa pronta, acavalando as duas.
        //
        // <b>Por que as perguntas de cima não pegaram.</b> A Regra 22
        // pergunta {@code isColonyBuilt(ground)} — <b>uma posição por
        // coluna</b>, a do chão encontrado — e a conferência de volume
        // começa <i>acima</i> dela. Um prédio cuja caixa cubra a coluna
        // em outra altura não é visto por nenhuma das duas: nem no ponto
        // do chão, nem na janela que começa depois dele.
        //
        // Aqui a pergunta é caixa contra caixa, que é a forma do que se
        // quer impedir. Uma vez por lote, e não por coluna — é o último
        // portão, e a essa altura só sobrou um candidato.
        ColonyPos floor = new ColonyPos(originX, baseY + 1, originZ);

        ColonyPos ceiling = new ColonyPos(
                originX + size.x() - 1,
                baseY + Math.max(size.y(), BuildSiteScanner.VERTICAL_CLEARANCE),
                originZ + size.z() - 1);

        if (LotClearance.overlapsVillageStructure(world, floor, ceiling)
                || VillageColonyMod.BUILDINGS.anythingBuiltInside(floor, ceiling)
                || LotClearance.overlapsFoundationFootprint(floor, ceiling)
                || LotClearance.overlapsConstructionSite(world, colonyId, floor, ceiling, null)) {

            LotRefusals.refused(colonyId, LotRefusals.Reason.OCCUPIED);

            return Optional.empty();
        }

        // <b>A pegada inteira passou</b> — 2026-09-17. É o par que
        // faltava ao LotRefusals: ele contava só o que some, e um
        // denominador sem numerador não diz se a vila está apertada ou
        // sem chão nenhum. Ver LotRefusals.accepted.
        LotRefusals.accepted(colonyId, columns);

        // O piso da casa vai sobre o chão, e não dentro dele — e o chão
        // é o da BASE, que é onde 90% das colunas estão. Era
        // {@code roadY + 1} até 2026-09-19, e era isso que fazia a casa
        // voar quando o lote inteiro estava acima da rua: o piso ficava
        // no nível do caminho e o terreno, mais alto, passava por baixo
        // dele.
        return Optional.of(baseY + 1);
    }
}
