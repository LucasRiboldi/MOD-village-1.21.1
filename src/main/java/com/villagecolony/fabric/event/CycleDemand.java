package com.villagecolony.fabric.event;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.coordination.WorkDemand;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageBiomes;
import com.villagecolony.fabric.work.ChestRelief;
import com.villagecolony.fabric.work.ConstructionDemand;
import com.villagecolony.fabric.work.HousePlans;
import com.villagecolony.fabric.work.WorkMaterials;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

/**
 * O que a obra aberta de uma colônia pede neste ciclo, na tábua, na pedra e nas peças desta vila — Regra 20,
 * ADR-039 item 4. Cada pedaço da conta é cobrado a uma subfase de {@link PlannerPhases} (VF-001 A).
 */
final class CycleDemand {

    /** A pedra desta vila e a demanda inteira da obra. */
    record Result(ResourceType stone, WorkDemand work) {
    }

    private CycleDemand() {
    }

    static Result of(ServerWorld overworld, Colony colony, PlannerPhases phases) {
        // A tábua da vila, e não sempre a de carvalho — a Regra 20. A
        // obra de uma colônia de taiga pede pinheiro, e perguntar por
        // carvalho devolveria zero: a meta perderia a demanda da obra e
        // cairia na metade do baú, que é a conta de quando não há obra.
        int planksForWork = phases.time(PlannerPhases.Sub.PLANKS, () -> ConstructionDemand.planksNeededBy(
                VillageBiomes.woodAt(overworld, colony.center())
                        .orElse(MinecraftTypeAdapter.toResourceId(Blocks.OAK_PLANKS)),
                colony)
                // E a tábua das escadas, lajes e cercas da obra (P7c).
                + com.villagecolony.fabric.work.WoodInPieces.planksIn(
                        overworld, ConstructionDemand.materialsNeededBy(colony)));

        // A pedra que a obra pede, e a pedra desta vila — 2026-08-20. No
        // deserto é arenito; perguntar por pedregulho daria zero, e a
        // vila voltaria a não construir por falta de meta.
        VillagePalette palette = phases.time(PlannerPhases.Sub.PALETTE,
                () -> HousePlans.paletteOf(overworld, colony.center()));
        phases.run(PlannerPhases.Sub.BIOME_PIECES,
                () -> com.villagecolony.fabric.work.BiomePieces.refresh(overworld, colony)); // ADR-039 item 4

        ResourceType stone = MinecraftTypeAdapter.toBlock(palette.stone())
                .flatMap(block -> MinecraftTypeAdapter.toResourceType(block.asItem()))
                .orElse(ResourceType.COBBLESTONE);

        // Por família desde 2026-08-22, e foi a vila de deserto que
        // cobrou: a casa dela é de arenito LISO, e perguntar pelo
        // arenito puro devolvia quase zero. Ver WorkMaterials.stone.
        // <b>E o bau que enche puxa a producao para a frente</b> —
        // decisao do autor, 2026-09-19. Sem isto a demanda so nasce de
        // obra aberta: na sessao de 12:09 o bau do mineiro encheu, 365
        // sandstone foram para o chao, e a obra esperava sandstone.
        // Ver ChestRelief.
        int stoneForWork = phases.time(PlannerPhases.Sub.STONE, () -> ChestRelief.stoneToAskFor(
                overworld, colony, WorkMaterials.stone(palette, colony)));

        // O que a obra pede em peça, traduzido para o que a colônia sabe
        // produzir. A casa não pede vidro, pede vidraça; não pede carvão,
        // pede tocha; não pede lã, pede cama. Perguntar pelo material
        // devolvia zero, e com zero ninguém recebia tarefa.
        //
        // <b>As quatro saem do mesmo lugar desde 2026-08-21</b>: a obra
        // aberta. A lã e o ferro vinham da passagem de mobília, que a
        // Regra 21 sustentava, e ela morreu.
        WorkDemand work = new WorkDemand(
                planksForWork,
                stone,
                stoneForWork,
                phases.time(PlannerPhases.Sub.WOOL, () -> WorkMaterials.wool(overworld, colony)),
                phases.time(PlannerPhases.Sub.GLASS, () -> WorkMaterials.glass(overworld, palette, colony)),
                phases.time(PlannerPhases.Sub.COAL, () -> WorkMaterials.coal(overworld, colony)),
                phases.time(PlannerPhases.Sub.IRON, () -> WorkMaterials.iron(overworld, colony)),
                phases.time(PlannerPhases.Sub.SMELTED, () -> WorkMaterials.smeltedNeeds(overworld, colony)),
                phases.time(PlannerPhases.Sub.SURFACE, () -> WorkMaterials.surfaceGatheredNeeds(overworld, colony)),
                phases.time(PlannerPhases.Sub.MINED, () -> WorkMaterials.minedNeeds(colony)),
                // A tora que a obra pede bruta: a meta não a conta como tábua (A-1).
                phases.time(PlannerPhases.Sub.RAW_WOOD, () -> ConstructionDemand.rawWoodNeededBy(colony)));

        return new Result(stone, work);
    }
}
