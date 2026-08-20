package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.work.ConstructionPlanner;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * A obra atravessa o fechar do mundo.
 *
 * <p>O save guarda identidade, estrutura, lugar e estado — e <b>não</b> o
 * progresso. Quem sabe o que já está de pé é o mundo, e é a ele que a
 * sessão seguinte pergunta.
 *
 * <p>É a metade da persistência que só o mundo prova. A outra metade — o
 * NBT indo e voltando — mora em {@code ConstructionSaveTest}, onde não
 * precisa de servidor.
 *
 * <p><b>A obra destes testes fica no ar.</b> A origem é escolhida acima do
 * chão da arena, onde tudo é vazio: assim nenhum bloco de estrutura
 * vizinha coincide por acaso com um bloco da casa, e o que a contagem
 * mede é só o que este teste colocou. Ver {@link ColonyFixture}.
 */
public class ConstructionResumeGameTest implements FabricGameTest {

    /** Alto o bastante para o projeto cair sobre puro ar. */
    private static final BlockPos ORIGIN = new BlockPos(1, 4, 1);

    /**
     * Uma obra que volta do save é retomada, e não recomeçada do zero.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "resume_project")
    public void aSavedProjectComesBackFromTheWorld(TestContext context) {
        Blueprint house = StructureBlueprintReader
                .read(context.getWorld(), StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                .orElseThrow(() -> new AssertionError("o jogo não devolveu a casa"));

        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Um bloco da casa já de pé, exatamente onde o projeto o quer.
        BlueprintBlock first = house.blocks().get(0);

        Block material = MinecraftTypeAdapter.toBlock(first.block())
                .orElseThrow(() -> new AssertionError("bloco desconhecido: " + first.block()));

        ColonyPos where = new ColonyPos(
                origin.x() + first.offset().x(),
                origin.y() + first.offset().y(),
                origin.z() + first.offset().z());

        context.getWorld().setBlockState(
                MinecraftTypeAdapter.toBlockPos(where), material.getDefaultState());

        VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                UUID.randomUUID(),
                colony.id(),
                StructureBlueprintReader.PLAINS_SMALL_HOUSE,
                origin,
                ConstructionState.BUILDING));

        ConstructionPlanner.plan(context.getWorld(), colony);

        Optional<ConstructionProject> resumed =
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        context.assertTrue(resumed.isPresent(), "a obra do save não renasceu");

        context.assertTrue(
                resumed.get().origin().equals(origin),
                "a obra renasceu no lugar errado: " + resumed.get().origin());

        context.assertTrue(
                resumed.get().state() == ConstructionState.BUILDING,
                "a obra renasceu em " + resumed.get().state());

        // O bloco que já estava de pé não é pedido de novo. É a diferença
        // entre retomar e recomeçar.
        context.assertTrue(
                resumed.get().remainingCount() == house.blockCount() - 1,
                "esperava " + (house.blockCount() - 1) + " blocos a fazer, e faltam "
                        + resumed.get().remainingCount());

        owned.cleanUp();

        context.complete();
    }

    /**
     * Estrutura que o jogo não conhece mais não trava a colônia.
     *
     * <p>Datapack que saiu, versão que mudou. A obra é abandonada com uma
     * linha no log, e a colônia segue — em vez de tentar renascer a cada
     * ciclo, para sempre.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "resume_unknown")
    public void aProjectOfAnUnknownStructureIsDropped(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                UUID.randomUUID(),
                colony.id(),
                com.villagecolony.core.type.ResourceId.vanilla("village/plains/houses/gone"),
                origin,
                ConstructionState.BUILDING));

        ConstructionPlanner.plan(context.getWorld(), colony);

        context.assertTrue(
                VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).isEmpty(),
                "a obra de estrutura inexistente continua tentando renascer");

        owned.cleanUp();

        context.complete();
    }

    /**
     * Obra de planta antiga, sem um bloco de pé, sai da frente.
     *
     * <p>A Regra 13 trocou a obra do MVP pela cabana, e a colônia do
     * autor continuou presa à casa de planície gravada no save: quinze
     * ciclos de {@code waiting for minecraft:stripped_oak_log}, que
     * ninguém produz, e a cabana nunca chegou a ser planejada. {@code
     * plan} não abre obra nova enquanto houver uma aberta.
     *
     * <p>Nada se perde: são zero blocos de pé. O que se ganha é a
     * colônia voltando a construir.
     *
     * <p><b>O que este teste não alcança.</b> O alvo é perguntado à
     * colônia desde a Regra 24, e a resposta depende do bioma — em
     * planície ela é a própria casa de planície, e então nada seria
     * descartado aqui. Este caso só é "planta antiga" porque o bioma da
     * arena não é planície, e a arena tem bioma fixo. A regra em si
     * está presa fora do jogo, em {@code
     * ConstructionProjectTest#anUntouchedProjectIsSupersededByADifferentTarget},
     * onde o alvo é escolhido pelo teste.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "resume_stale_target")
    public void anUntouchedProjectOfTheOldTargetIsDropped(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillageColonyMod.CONSTRUCTIONS.registerPending(new ConstructionService.Pending(
                UUID.randomUUID(),
                colony.id(),
                StructureBlueprintReader.PLAINS_SMALL_HOUSE,
                origin,
                ConstructionState.WAITING_RESOURCES));

        ConstructionPlanner.plan(context.getWorld(), colony);

        context.assertTrue(
                VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id()).isEmpty(),
                "a obra antiga continua guardada");

        context.assertTrue(
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                        .map(open -> !open.blueprint().id().equals(
                                StructureBlueprintReader.PLAINS_SMALL_HOUSE))
                        .orElse(true),
                "a casa de planície voltou a ser aberta, e ela é impossível para esta colônia");

        owned.cleanUp();

        context.complete();
    }

    /**
     * A obra largada sai do registro e o lote continua tomado.
     *
     * <p>É a consequência do {@code PatienceClock}, afirmada sem
     * esperar os dez minutos dele: o relógio se prova fora do jogo, e o
     * que acontece quando ele estoura se prova aqui.
     *
     * <p><b>O que se ganha e o que se perde.</b> Ganha-se a colônia
     * viva: quem planeja não abre obra nova enquanto houver uma aberta,
     * e uma casa esperando pedregulho que o jogador nunca traz parava a
     * vila para sempre. Perde-se aquela casa, que fica pela metade — e
     * por isso o lote precisa continuar tomado, senão a colônia
     * planejaria por cima do que ela mesma levantou.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "give_up_stalled")
    public void aStalledProjectLeavesItsLotTaken(TestContext context) {
        ColonyPos origin = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(ORIGIN));

        Colony colony = Colony.create(UUID.randomUUID(), origin);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                ColonyHut.blueprint(ColonyHut.OAK_PLANKS, Side.NORTH),
                origin);

        VillageColonyMod.CONSTRUCTIONS.register(project);

        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.WAITING_RESOURCES);

        ConstructionPlanner.giveUp(colony, project);

        context.assertTrue(
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isEmpty(),
                "a obra largada continua aberta, e a colônia segue travada nela");

        context.assertTrue(
                VillageColonyMod.BUILDINGS.isColonyInfrastructure(origin),
                "o lote da casa pela metade ficou livre, e a colônia vai construir por cima");

        owned.cleanUp();

        VillageColonyMod.BUILDINGS.removeOfColony(colony.id());

        context.complete();
    }
}
