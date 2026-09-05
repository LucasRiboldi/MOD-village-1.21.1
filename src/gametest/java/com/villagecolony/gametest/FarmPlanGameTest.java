package com.villagecolony.gametest;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;
import java.util.Optional;

/**
 * A roça que a colônia levanta é a do jogo, e ela sai vazia.
 *
 * <p>Decisão do autor, 2026-09-05: <i>"precisam construir o espaço de
 * plantação padrão e idêntico aos que já vêm na vila do Minecraft"</i>.
 *
 * <p>Até essa data quem abria roça era o fazendeiro, arando toda terra
 * hidratada que achasse: a sessão das 20:30 deixou <b>trinta e quatro
 * blocos arados em trinta e três posições distintas</b>, espalhados por
 * catorze blocos de vila.
 */
public class FarmPlanGameTest implements FabricGameTest {

    /** A roça de planície, lida do catálogo do jogo. */
    private static Optional<Blueprint> plainsFarm(TestContext context) {
        List<ResourceId> farms = VillageStructures.farmsFor("plains");

        return farms.isEmpty()
                ? Optional.empty()
                : StructureBlueprintReader.read(context.getWorld(), farms.get(0));
    }

    /**
     * A planta é mesmo uma roça: ela traz terra arada.
     *
     * <p>É a afirmação de que o catálogo entregou o que se pediu, e não
     * uma casa cujo nome por acaso contém {@code farm}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void theVillageFarmPlanCarriesFarmland(TestContext context) {
        Blueprint farm = plainsFarm(context).orElse(null);

        context.assertTrue(farm != null, "o catálogo não devolveu roça de planície nenhuma");

        ResourceId farmland = MinecraftTypeAdapter.toResourceId(Blocks.FARMLAND);

        context.assertTrue(
                farm.blocks().stream().anyMatch(block -> block.block().equals(farmland)),
                "a planta da roça não tem um canteiro sequer");

        context.complete();
    }

    /**
     * <b>E o canteiro nasce vazio</b> — a obra faz o espaço, o fazendeiro
     * planta.
     *
     * <p>É a divisão que o autor descreveu, e ela também evita a obra
     * parada: cobrar trigo, cenoura, batata e beterraba do baú faria a
     * roça esperar semente que a colônia talvez não tenha, que é o
     * defeito do vão do teto de 09-04 noutro lugar.
     *
     * <p>A planta do jogo <b>tem</b> lavoura — este teste só vale porque
     * o {@code FarmPlans} a tira. Sem essa retirada ele reprova.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "farm_plan")
    public void thePlantedCropsAreLeftToTheFarmer(TestContext context) {
        Blueprint raw = plainsFarm(context).orElse(null);

        context.assertTrue(raw != null, "o catálogo não devolveu roça de planície nenhuma");

        context.assertTrue(
                raw.blocks().stream().anyMatch(FarmPlanGameTest::isCrop),
                "a roça do jogo veio sem lavoura, e aí este teste não mede nada");

        Colony colony = Colony.create(
                java.util.UUID.randomUUID(),
                MinecraftTypeAdapter.toColonyPos(
                        context.getAbsolutePos(new net.minecraft.util.math.BlockPos(1, 2, 1))));

        List<Blueprint> plans = com.villagecolony.fabric.work.FarmPlans
                .plansFor(context.getWorld(), colony);

        context.assertTrue(!plans.isEmpty(), "a colônia não recebeu planta de roça nenhuma");

        for (Blueprint plan : plans) {
            context.assertTrue(
                    plan.blocks().stream().noneMatch(FarmPlanGameTest::isCrop),
                    "a planta da roça saiu com lavoura plantada, e a obra vai esperar semente");
        }

        context.complete();
    }

    private static boolean isCrop(BlueprintBlock block) {
        return MinecraftTypeAdapter.toBlock(block.block())
                .map(found -> found instanceof CropBlock)
                .orElse(false);
    }
}
