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
 * O lote logo ao lado de um trecho de rua, com a porta voltada para ela — separado de
 * {@link BuildSiteScanner} em 2026-09-24, quando ele passou de 2.000 linhas.
 * Os comentários vieram junto sem mudança.
 */
public final class RoadsideSites {

    private RoadsideSites() {
    }

    /**
     * Um lote encostado <b>nestas</b> colunas, sem varrer o raio — E26.
     *
     * <p>Existe por causa de uma conta que a sessão de 2026-08-25 tornou
     * concreta: uma resposta de lote custa dezessete ciclos, oito minutos
     * e meio. Quando a Regra 15 acaba de calçar um trecho, a colônia
     * <b>sabe</b> onde nasceu beira nova — e mandá-la redescobrir isso
     * varrendo o raio inteiro é pagar oito minutos por uma informação
     * que ela tem na mão.
     *
     * <p>Não substitui a varredura: ela continua sendo quem acha lote em
     * vila que ainda tem beira livre, e quem anota as pontas de rua. Isto
     * é o atalho do caso em que a colônia acabou de criar a beira.
     *
     * @param road as colunas recém-calçadas, na ordem em que saíram
     */
    public static Optional<Site> findBeside(
            ServerWorld world, UUID colonyId, ColonyPos center,
            List<ColonyPos> plans, List<ColonyPos> road) {

        BlockPos from = MinecraftTypeAdapter.toBlockPos(center);

        for (ColonyPos column : road) {
            Optional<Site> site = siteBesideRoadAt(
                    world, colonyId, from, column.x(), column.z(), from.getY(), plans);

            if (site.isPresent()) {
                // Achou: a varredura em curso perde o sentido, e o cursor
                // dela sai para a próxima começar limpa.
                SweepState.SWEEPS.remove(colonyId);

                return site;
            }
        }

        return Optional.empty();
    }

    /**
     * Se esta coluna é estrada, o lote livre ao lado dela.
     *
     * <p>Testa as quatro direções na ordem do enum, e a primeira que
     * servir vence. Não há critério melhor no MVP: as quatro são
     * igualmente boas, e escolher por sorteio faria a mesma vila crescer
     * diferente a cada sessão, o que é ruim de depurar.
     */
    static Optional<Site> siteBesideRoadAt(
            ServerWorld world, UUID colonyId, BlockPos center,
            int x, int z, int aroundY, List<ColonyPos> plans) {

        Optional<BlockPos> ground = LotGround.groundInColumn(world, x, z, aroundY);

        // Material oficial so vira rua quando pertence a ROAD_AREA.
        if (ground.isEmpty() || !RoadIndex.isRoadArea(world, colonyId, ground.get())) {
            return Optional.empty();
        }

        // A Regra 15 pega carona aqui — 2026-08-21. Esta coluna é rua, e
        // esta varredura é a única que passa por todas elas. Perguntar
        // agora se ela é ponta custa algumas leituras nas poucas colunas
        // calçadas; perguntar depois custaria o raio inteiro de novo.
        RoadExtension.consider(world, colonyId, ground.get(), center);

        // Esta coluna é rua, e esta varredura é a única que passa por
        // todas elas — o índice se enche exatamente aqui, e de graça.
        Set<Long> building = SweepState.BUILDING.get(colonyId);

        if (building != null) {
            building.add(ColonyRoads.column(x, z));
        }

        // A altura da rua, que a Regra 19 usa como régua do lote.
        int roadY = ground.get().getY();

        for (ColonyPos size : plans) {
            Optional<Site> site = siteFor(world, colonyId, x, z, aroundY, roadY, size);

            if (site.isPresent()) {
                return site;
            }
        }

        return Optional.empty();
    }

    /** O lote desta planta ao lado desta rua, se houver. */
    static Optional<Site> siteFor(
            ServerWorld world, UUID colonyId, int x, int z, int aroundY, int roadY,
            ColonyPos size) {

        for (Direction side : Direction.Type.HORIZONTAL) {
            // O lote começa no bloco seguinte à estrada — encostado nela,
            // que é a decisão 1.
            int lotX = x + side.getOffsetX();
            int lotZ = z + side.getOffsetZ();

            // A casa se estende para longe da estrada, e não por cima
            // dela: partindo da beira, o canto do lote recua meia casa
            // nos eixos que não são o da direção.
            int originX = side.getOffsetX() < 0 ? lotX - size.x() + 1 : lotX;
            int originZ = side.getOffsetZ() < 0 ? lotZ - size.z() + 1 : lotZ;

            Optional<Integer> floor =
                    LotLevel.flatGroundAt(world, colonyId, originX, originZ, aroundY, roadY, size);

            if (floor.isPresent()) {
                // A rua fica do lado oposto àquele para onde o lote
                // cresceu: `side` aponta da rua para o lote, e a porta
                // olha de volta para ela.
                return Optional.of(new Site(
                        new ColonyPos(originX, floor.get(), originZ),
                        side.getOpposite(),
                        size));
            }
        }

        return Optional.empty();
    }
}
