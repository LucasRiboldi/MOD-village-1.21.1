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
 * Se o lote está livre: nada construído pela colônia, nenhuma obra aberta, nenhuma estrutura da vila e céu desimpedido — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class LotClearance {

    private LotClearance() {
    }

    /**
     * A coluna está livre da altura do piso até o teto da planta?
     *
     * <p>A Regra 22, de 2026-08-19. Até aqui o lote era julgado pelo
     * <b>chão</b>: a coluna respondia onde a casa assenta, e um único
     * bloco acima da janela reprovava. Isso deixava passar o que
     * estivesse dentro da caixa da casa e acima daquela janela — árvore
     * caída, cerca, poste, a quina de outra construção. A casa nascia
     * com aquilo dentro dela, e o construtor pulava os blocos ocupados
     * com {@code is in the way}.
     *
     * <p>Agora a pergunta é sobre o volume: cada coluna do lote, do piso
     * ao último nível da planta.
     *
     * <p><b>Planta não ocupa.</b> Grama alta, flor, samambaia e camada
     * de neve não reprovam o lote — quem constrói tira. É o outro lado
     * da mesma decisão do autor: recusar um lote de planície por causa
     * de um pé de margarida seria recusar a planície inteira.
     */
    /**
     * Se alguma obra <b>em andamento</b> ocupa esta caixa — 2026-09-19.
     *
     * <p><b>O buraco que isto fecha, e ele era meu.</b> O portão de caixa
     * contra caixa de 15:49 consultava só o {@code BUILDINGS}, que é o
     * registro das obras <b>terminadas</b>. Uma obra em andamento não
     * está nele — ela só entra quando fecha —, então um lote novo podia
     * nascer em cima dela.
     *
     * <p>E era o caso exato desta vila: a obra do {@code cut_sandstone}
     * ficou parada em {@code WAITING_RESOURCES} por quase meia hora,
     * ocupando o terreno e <b>invisível</b> para o portão.
     *
     * <p>O {@code BlockProtection.isOpenSite} já percorre as obras
     * abertas, mas responde por <b>ponto</b>; aqui a pergunta é da caixa,
     * que é a forma do que se quer impedir.
     */
    /**
     * Se esta obra ocupa o mesmo espaço de algo já construído —
     * 2026-09-19.
     *
     * <p>Existe para a <b>retomada</b>: o portão de caixa governa quem
     * escolhe lote, e a obra gravada no save não passa por ele. Um lote
     * ruim escolhido por uma versão anterior sobrevivia a todo conserto
     * do scanner e voltava a cada carregamento.
     *
     * <p>Ignora a própria obra, e é obrigatório: ela está no registro de
     * obras abertas quando esta pergunta é feita, e sem isso toda obra
     * se acusaria de pisar em si mesma.
     */
    public static boolean overlapsSomethingBuilt(
            ServerWorld world, ConstructionProject project) {
        Building box = Building.of(project);

        if (overlapsVillageStructure(world, box.min(), box.max())
                || hasOccupiedBlock(world, box)
                || VillageColonyMod.BUILDINGS.anythingBuiltInside(box.min(), box.max())
                || overlapsFoundationFootprint(box.min(), box.max())
                || overlapsConstructionSite(
                        world, project.colonyId(), box.min(), box.max(), project.id())) {
            return true;
        }

        return false;
    }

    /**
     * Se uma obra aberta, inclusive uma ainda pendente do save, ocupa a caixa.
     *
     * <p>Projetos pendentes existem antes de {@code resume}: o save guarda
     * apenas o id e a origem até o mundo estar carregado. Eles continuam
     * reservando a caixa nesse intervalo, ou uma nova zona pode nascer sobre
     * uma BigHouseMOD incompleta na entrada do jogo.
     */
    static boolean overlapsConstructionSite(
            ServerWorld world,
            UUID colonyId,
            ColonyPos min,
            ColonyPos max,
            UUID ignoredProjectId) {
        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.colonyId().equals(colonyId)
                    || !project.state().isOpen()
                    || project.id().equals(ignoredProjectId)) {
                continue;
            }

            if (intersects(min, max, Building.of(project))) {
                return true;
            }
        }

        for (ConstructionService.Pending pending : VillageColonyMod.CONSTRUCTIONS.allPending()) {
            if (!pending.colonyId().equals(colonyId) || pending.id().equals(ignoredProjectId)) {
                continue;
            }

            Optional<Blueprint> blueprint =
                    PlanPlacement.blueprintOf(world, pending.colonyId(), pending.blueprint(), pending.origin());

            if (blueprint.isEmpty()) {
                continue;
            }

            ColonyPos origin = pending.origin();
            ColonyPos pendingMax = new ColonyPos(
                    origin.x() + blueprint.get().size().x() - 1,
                    origin.y() + blueprint.get().size().y() - 1,
                    origin.z() + blueprint.get().size().z() - 1);
            Building site = new Building(
                    pending.id(), pending.colonyId(), pending.blueprint(), origin, pendingMax, false);

            if (intersects(min, max, site)) {
                return true;
            }
        }

        return false;
    }

    static boolean intersects(ColonyPos min, ColonyPos max, Building site) {
        if (!intersectsFootprint(min, max, site)) {
            return false;
        }

        return site.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD)
                || (min.y() <= site.max().y() && max.y() >= site.min().y());
    }

    /** A fundacao reserva todas as colunas da sua pegada, em qualquer altura. */
    static boolean overlapsFoundationFootprint(ColonyPos min, ColonyPos max) {
        return VillageColonyMod.BUILDINGS.all().stream()
                .filter(site -> site.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD))
                .anyMatch(site -> intersectsFootprint(min, max, site));
    }

    static boolean intersectsFootprint(ColonyPos min, ColonyPos max, Building site) {
        return min.x() <= site.max().x() && max.x() >= site.min().x()
                && min.z() <= site.max().z() && max.z() >= site.min().z();
    }

    /**
     * A caixa de uma obra não pode entrar na caixa de uma peça de vila.
     *
     * <p>{@code StructureAccessor#getStructureContaining} só responde para
     * blocos que a peça realmente colocou. O interior vazio de uma casa
     * continua dentro do {@code BlockBox} da peça, e é esse espaço que
     * também precisa ser protegido.
     */
    static boolean overlapsVillageStructure(
            ServerWorld world, ColonyPos min, ColonyPos max) {
        BlockBox candidate = new BlockBox(
                min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
        int minChunkX = min.x() >> 4;
        int maxChunkX = max.x() >> 4;
        int minChunkZ = min.z() >> 4;
        int maxChunkZ = max.z() >> 4;
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // A estrutura pode ter seu start em outra chunk. Consultar
                // somente getStructureStarts() da chunk candidata perde as
                // pecas referenciadas pela geracao da vila vizinha.
                for (StructureStart start : world.getStructureAccessor().getStructureStarts(
                        new ChunkPos(chunkX, chunkZ),
                        structure -> registry.getEntry(structure)
                                .isIn(StructureTags.VILLAGE))) {
                    if (!start.hasChildren()) {
                        continue;
                    }

                    if (start.getChildren().stream()
                            .map(piece -> piece.getBoundingBox())
                            .anyMatch(candidate::intersects)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Confere o volume físico de uma obra retomada, ignorando ar e blocos
     * que a preparação do canteiro pode remover.
     */
    static boolean hasOccupiedBlock(ServerWorld world, Building box) {
        for (int x = box.min().x(); x <= box.max().x(); x++) {
            for (int y = box.min().y(); y <= box.max().y(); y++) {
                for (int z = box.min().z(); z <= box.max().z(); z++) {
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

                    if (chunk == null) {
                        return true;
                    }

                    if (!LotGround.isNothing(chunk.getBlockState(new BlockPos(x, y, z)))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    static boolean isClearAbove(
            ServerWorld world, int x, int z, int floor, int height) {

        WorldChunk chunk = world.getChunkManager().getWorldChunk(x >> 4, z >> 4);

        if (chunk == null) {
            // Chunk descarregado: não dá para afirmar que está livre, e
            // afirmar sem saber é como a casa nasce dentro da árvore.
            return false;
        }

        for (int y = floor; y < floor + height; y++) {
            BlockPos at = new BlockPos(x, y, z);

            BlockState state = chunk.getBlockState(at);

            // O teto de barreira é inserido pelo GameTest fora da arena e
            // não representa um bloco do mundo. Ele não deve encurtar a
            // janela de 25 blocos que a produção precisa validar.
            if (!LotGround.isNothing(state) && !state.isOf(Blocks.BARRIER)) {
                // <b>Qual bloco barrou</b> — P1.6, 2026-09-19. A Regra 22
                // é 70% das recusas em duas sessões seguidas e não dizia
                // de que era feita. Aqui, e só aqui, porque este é o
                // bloco que parou a conferência. Ver VolumeSample.
                VolumeSample.inTheColumn(world, at);

                return false;
            }

            // E de quem é este vazio. A cabana da colônia é oca e não tem
            // piso: o miolo dela é grama original no nível da rua, com o
            // volume livre até o teto, e passa em todas as perguntas que
            // se fazem ao mundo — nenhuma delas pergunta de quem aquilo é.
            //
            // Visto em jogo em 2026-08-20, 01:54: a vila ofereceu como
            // lote o interior de uma cabana levantada na véspera. Quem
            // recusava era o planejador, depois da busca, e isso não
            // bastava: achar um lote apaga o cursor, então a passagem
            // seguinte recomeçava do centro e reencontrava o mesmo miolo.
            // A recusa precisa acontecer aqui dentro, onde a varredura
            // pode seguir para o anel seguinte.
            if (BlockProtection.isColonyBuilt(at)) {
                // Miolo da propria cabana: e a colonia cheia, nao coisa
                // no caminho. Separar as duas e o P1.6. Ver VolumeSample.
                VolumeSample.colonyBuilt();

                return false;
            }
        }

        return true;
    }
}
