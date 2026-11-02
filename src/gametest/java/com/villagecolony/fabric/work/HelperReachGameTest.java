package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Só o construtor executa obra. */
public class HelperReachGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials", tickLimit = 20)
    public void onlyTheBuilderCanTakeOrStartTheBuild(TestContext context) {
        UUID colony = UUID.randomUUID();
        VillagerEntity shepherdEntity = context.spawnEntity(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        VillagerEntity builderEntity = context.spawnEntity(EntityType.VILLAGER, new BlockPos(2, 2, 1));
        Worker shepherd = VillageColonyMod.WORKERS.register(shepherdEntity.getUuid(), colony);
        shepherd.assign(ProfessionType.SHEPHERD);
        Worker builder = VillageColonyMod.WORKERS.register(builderEntity.getUuid(), colony);
        builder.assign(ProfessionType.BUILDER);
        ColonyPos here = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(1, 2, 1)));

        try {
            ConstructionProject near = siteAt(colony, new ColonyPos(here.x() + 20, here.y(), here.z()));
            ConstructionProject far = siteAt(colony, new ColonyPos(here.x() + 40, here.y(), here.z()));

            context.assertFalse(HelperReach.mayTake(context.getWorld(), shepherd, near),
                    "o pastor assumiu obra do construtor");
            context.assertFalse(HelperReach.mayTake(context.getWorld(), shepherd, far),
                    "o pastor distante assumiu obra do construtor");
            context.assertTrue(HelperReach.mayTake(context.getWorld(), builder, far),
                    "o construtor pega a obra de qualquer distância");
            context.assertTrue(HelperReach.stallLimit(builder.villagerId()) == BuilderWork.STALL_LIMIT,
                    "o construtor não recebeu prazo inteiro");

            context.assertFalse(HelperReach.mayStart(context.getWorld(), shepherd, near),
                    "o pastor começou obra");
            context.assertTrue(HelperReach.mayStart(context.getWorld(), builder, far),
                    "o construtor começa a obra de qualquer distância");
            builder.assign(ProfessionType.SHEPHERD);
            context.assertFalse(HelperReach.mayStart(context.getWorld(), shepherd, near),
                    "sem construtor, outro ofício começou obra");
        } finally {
            VillageColonyMod.WORKERS.remove(shepherd.villagerId());
            VillageColonyMod.WORKERS.remove(builder.villagerId());
            shepherdEntity.discard();
            builderEntity.discard();
        }

        context.complete();
    }

    /** Com só a peça de lado faltando, só o construtor segura a obra e espera. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials", tickLimit = 20)
    public void theBuilderHoldsTheBuildWhileThePieceIsMissing(TestContext context) {
        UUID colony = UUID.randomUUID();
        UUID shepherd = UUID.randomUUID();
        UUID builder = UUID.randomUUID();
        VillageColonyMod.WORKERS.register(shepherd, colony).assign(ProfessionType.SHEPHERD);
        VillageColonyMod.WORKERS.register(builder, colony).assign(ProfessionType.BUILDER);
        ConstructionProject project = siteAt(colony, new ColonyPos(0, 64, 0));
        project.deferForMaterial(project.nextBlock().orElseThrow());

        try {
            context.assertTrue(project.nextBlock().isEmpty() && !project.deferredPieces().isEmpty(),
                    "a única peça devia estar de lado");
            context.assertTrue(BuildWait.holds(context.getWorld(), project, builder) && BuildWait.isWaiting(builder),
                    "o construtor devia segurar a obra esperando a peça");
            context.assertFalse(BuildWait.holds(context.getWorld(), project, shepherd),
                    "o ajudante não segura a obra parada");
        } finally {
            BuildWait.stop(builder);
            VillageColonyMod.WORKERS.remove(shepherd);
            VillageColonyMod.WORKERS.remove(builder);
        }

        context.complete();
    }

    private static ConstructionProject siteAt(UUID colony, ColonyPos origin) {
        ConstructionProject project = ConstructionProject.plan(colony,
                Blueprint.of(ResourceId.vanilla("test_window"),
                        List.of(new BlueprintBlock(new ColonyPos(0, 0, 0), ResourceId.vanilla("glass_pane")))),
                origin);
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        return project;
    }

    /**
     * A última peça de lado por alcance volta à fila — playtest de 2026-10-11: a escada do
     * telhado ficou de lado 25 minutos e a obra ficou sem tarefa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "builder_materials", tickLimit = 20)
    public void theLastUnreachablePieceComesBack(TestContext context) {
        ConstructionProject project = siteAt(UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(new BlockPos(2, 2, 2))));
        com.villagecolony.core.construction.model.BlueprintBlock piece = project.nextBlock().orElseThrow();
        BlockPos at = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(piece));

        // O lugar dela ocupado, como a terra do playtest: o "já cabe" responde não, e o
        // apoio gravado é o de agora — não muda.
        context.getWorld().setBlockState(at, net.minecraft.block.Blocks.STONE.getDefaultState());

        try {
            project.defer(piece, com.villagecolony.core.construction.model.ConstructionOutcome.skipped(
                            project.worldPositionOf(piece), com.villagecolony.core.construction.model.SkipReason.UNREACHABLE),
                    BuilderPlacement.supportFingerprint(context.getWorld(), at));

            context.assertTrue(project.nextBlock().isEmpty(), "a peça devia estar de lado");

            BuilderPlacement.reconsiderDeferredPieces(context.getWorld(), project);

            context.assertTrue(project.nextBlock().isPresent(),
                    "sem mais nada a pôr, a peça de lado por alcance devia voltar à fila");
        } finally {
            context.getWorld().setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState());
        }

        context.complete();
    }
}
