package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.ColonyModels;
import com.villagecolony.fabric.integration.LotClearance;
import com.villagecolony.fabric.integration.LotTerraform;
import com.villagecolony.fabric.integration.MineHomeSiteSearch;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A casa do mineiro surge pronta ao lado da boca da mina na fundação da vila — Regra 260.
 *
 * <p>Quando a {@code BigHouseMOD} acaba de ser posta, a fundação abre a mina pela mesma escolha de boca do mineiro
 * ({@link MineOpening}), com o arco, e põe a casa do mineiro (Regra 175) no lote que {@link MineHomeSiteSearch} acha,
 * sem obra e sem material, registrada como {@link Building} terminado — como a {@code BigHouseMOD}. Com a casa
 * registrada, {@link MineHomePlans#isOwed} não abre obra para ela. Sem boca ou sem lote, nada é posto, e o mineiro e o
 * construtor seguem pelo caminho de sempre.
 */
public final class MinerHomeFoundation {

    /** A busca local anda por cursor, até {@code MAX_COLUMNS} colunas por chamada: o raio de 20 cabe em duas. */
    private static final int SEARCH_CALLS = 4;

    private MinerHomeFoundation() {
    }

    /** Abre a mina e ergue a casa do mineiro; verdadeiro quando a casa foi posta. Só para vila sem mina. */
    public static boolean ensure(ServerWorld world, Colony colony) {
        if (VillageColonyMod.MINES.of(colony.id()).isPresent()) {
            return false;
        }

        if (MineOpening.open(world, colony.id(), MinecraftTypeAdapter.toBlockPos(colony.center()),
                "foundation of colony " + colony.id()).isEmpty()) {
            return false;
        }

        List<Blueprint> plans = MineHomePlans.planFor(world, colony);
        Optional<BuildSiteScanner.Site> site = Optional.empty();

        for (int call = 0; call < SEARCH_CALLS && !plans.isEmpty() && site.isEmpty(); call++) {
            MineHomeSiteSearch.Result search = MineHomePlans.findSite(world, colony, plans);
            site = search.site();

            if (search.complete()) {
                break;
            }
        }

        if (site.isEmpty()) {
            VillageColonyMod.LOGGER.info("VC_MINE_HOME colony={}: no lot beside the mouth on foundation; "
                    + "the miner home stays a construction", colony.id());
            return false;
        }

        return place(world, colony, plans.getFirst(), site.get());
    }

    /** A planta virada para a porta do lote, posta inteira pelo molde do jogo. */
    private static boolean place(ServerWorld world, Colony colony, Blueprint plan, BuildSiteScanner.Site site) {
        Side road = MinecraftTypeAdapter.toSide(site.doorSide());
        int turns = plan.doorSide().map(door -> door.turnsTo(road)).orElse(0);
        Blueprint turned = PlanPlacement.turnedToTheRoad(plan, road);
        ColonyPos origin = turned.originFor(site.origin());

        ConstructionProject lot = ConstructionProject.plan(colony.id(), turned, origin);
        LotClearance.clearLightDecor(world, origin, turned.size());
        LotTerraform.level(world, lot);

        if (!stamp(world, plan, origin, turns)) {
            return false;
        }

        ColonyPos max = new ColonyPos(origin.x() + turned.size().x() - 1, origin.y() + turned.size().y() - 1,
                origin.z() + turned.size().z() - 1);
        VillageColonyMod.BUILDINGS.register(new Building(UUID.randomUUID(), colony.id(), plan.id(), origin, max, true));

        VillageColonyMod.LOGGER.info("VC_MINE_HOME colony={}: the miner home {} stands at {} on foundation",
                colony.id(), plan.id(), origin);

        return true;
    }

    /**
     * O molde da planta (o mesmo arquivo que o leitor da planta lê) girado {@code turns} quartos de volta, com o canto
     * de menor coordenada em {@code origin}.
     */
    static boolean stamp(ServerWorld world, Blueprint plan, ColonyPos origin, int turns) {
        Optional<StructureTemplate> template = world.getStructureTemplateManager()
                .getTemplate(MinecraftTypeAdapter.toIdentifier(ColonyModels.sourceFor(plan.id())));

        if (template.isEmpty()) {
            return false;
        }

        BlockPos corner = MinecraftTypeAdapter.toBlockPos(origin).add(pivotShift(template.get().getSize(), turns));
        StructurePlacementData placement = new StructurePlacementData()
                .setRotation(rotationOf(turns))
                .setPosition(BlockPos.ORIGIN)
                .setIgnoreEntities(true)
                .setInitializeMobs(false);

        return template.get().place(world, corner, corner, placement,
                Random.create(world.getSeed() ^ corner.asLong()), Block.NOTIFY_ALL);
    }

    /**
     * O giro do jogo é em volta da origem do molde; a planta girada fica no canto de menor coordenada
     * ({@link Blueprint#rotated}). O desvio leva um ao outro.
     */
    static BlockPos pivotShift(net.minecraft.util.math.Vec3i size, int turns) {
        int maxX = size.getX() - 1;
        int maxZ = size.getZ() - 1;

        return switch (Math.floorMod(turns, 4)) {
            case 1 -> new BlockPos(maxZ, 0, 0);
            case 2 -> new BlockPos(maxX, 0, maxZ);
            case 3 -> new BlockPos(0, 0, maxX);
            default -> BlockPos.ORIGIN;
        };
    }

    private static BlockRotation rotationOf(int turns) {
        return switch (Math.floorMod(turns, 4)) {
            case 1 -> BlockRotation.CLOCKWISE_90;
            case 2 -> BlockRotation.CLOCKWISE_180;
            case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }
}
