package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.BuilderWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.MaterialRequest.Source;
import com.villagecolony.core.construction.model.MaterialRequest.State;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O material da obra: tirar do baú o bloco ou um substituto, dizer se a
 * próxima peça tem material, e esperar quando não tem — separado de
 * {@link BuilderWork} em 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>É onde a ADR-022 (peça sem rota nasce no baú) e o N3 (equivalente
 * antes) encontram o construtor. Os comentários vieram junto sem mudança.
 */
public final class BuilderMaterials {
    /**
     * Peças de rota só teórica: depois de três faltas aparecem no baú do construtor,
     * mesmo com a cadeia existindo no papel. A terracota (argila que nunca chega) e o
     * fardo de feno — pedido do autor, 2026-10-08: a obra pôs o fardo de lado 93 vezes
     * em 22 min, cada um custa nove trigos e a roça não dava conta. Lampião e
     * corrente também (mesmo pedido, depois): ferro do mineiro, e a obra ficou 20 min
     * parada nas últimas 99 peças, todas lampião e corrente. Funil e trilho pelo
     * mesmo motivo, e o vaso pela argila: o celeiro ficou 40 min parado num funil.
     */
    private static final Set<Item> STOCKED_AFTER_MISSES = Set.of(
            Items.HAY_BLOCK,
            Items.LANTERN, Items.SOUL_LANTERN, Items.CHAIN,
            Items.HOPPER, Items.RAIL, Items.FLOWER_POT,
            Items.TERRACOTTA,
            Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
            Items.LIGHT_BLUE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
            Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA, Items.LIGHT_GRAY_TERRACOTTA,
            Items.CYAN_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
            Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA, Items.RED_TERRACOTTA,
            Items.BLACK_TERRACOTTA);

    private BuilderMaterials() {
    }

    /**
     * Tira do baú o primeiro material que servir, e diz qual foi.
     *
     * <p>Do preferido ao último — {@link MaterialChoice}. Fora da pedra a
     * lista tem um item só, que é a Regra 27 valendo inteira.
     *
     * @return o item que saiu do baú, ou vazio quando nenhum servia
     */
    static Optional<Item> takeMaterial(
            ServerWorld world, ConstructionProject project, Block wanted) {

        for (Item candidate : MaterialChoice.forBlock(wanted)) {
            if (ColonySupply.take(world, project.colonyId(), project.origin(), candidate)) {
                return Optional.of(candidate);
            }
        }

        if (ensureConstructionMaterial(world, project, MaterialChoice.forBlock(wanted))) {
            for (Item candidate : MaterialChoice.forBlock(wanted)) {
                if (ColonySupply.take(world, project.colonyId(), project.origin(), candidate)) {
                    return Optional.of(candidate);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * O material do próximo bloco já está em algum baú da colônia?
     *
     * <p>Pergunta sem tirar nada, e existe para {@code ConstructionPlanner}
     * poder tirar a obra de {@code WAITING_RESOURCES}. A varredura é a
     * mesma de {@link #takeMaterial} — todos os baús de todos os
     * trabalhadores da colônia — porque as duas precisam concordar: uma
     * que dissesse "tem" e outra que não achasse poria a obra a acordar e
     * voltar a dormir todo ciclo.
     */
    public static boolean hasMaterialForNextBlock(
            ServerWorld world, ConstructionProject project) {

        Optional<BlueprintBlock> next = project.nextBlock();

        if (next.isEmpty()) {
            // Nada a pôr: a obra acabou e quem a fecha é o construtor.
            MaterialRequests.clear(project.id());
            return true;
        }

        ResourceId piece = next.get().block();
        Optional<Block> material = MinecraftTypeAdapter.toBlock(piece);

        if (material.isEmpty()) {
            // Bloco que este jogo não conhece. BuilderPlacement.placeOne o risca e segue,
            // então acordar a obra é o certo — ela não vai travar nele.
            MaterialRequests.record(project.id(), piece, State.NO_SOLUTION, Source.NONE, world.getTime());
            return true;
        }

        if (BlockShaping.isShapedFromTheGround(material.get().getDefaultState())
                || BlockShaping.isNeverPlaced(material.get().getDefaultState())) {
            // Estes blocos são formados no local ou não têm item próprio;
            // por isso não podem deixar a obra esperando por estoque.
            MaterialRequests.clear(project.id());
            return true;
        }

        // A mesma lista de takeMaterial, e por obrigação: uma pergunta
        // que dissesse "tem" e uma retirada que não achasse poriam a obra
        // a acordar e voltar a dormir todo ciclo.
        for (Item candidate : MaterialChoice.forBlock(material.get())) {
            if (ColonySupply.canProvide(
                    world, project.colonyId(), project.origin(), candidate)) {

                MaterialRequests.record(project.id(), piece, State.DELIVERED, Source.CHEST, world.getTime());
                return true;
            }
        }

        // O pedido da próxima peça fica registrado com o motivo — ADR-035 §3.
        // Só aqui: o prepareAhead pergunta por peças futuras e não pode
        // sobrescrever o que a obra espera agora.
        Supply supply = supply(world, project, MaterialChoice.forBlock(material.get()));
        MaterialRequests.record(project.id(), piece, supply.state(), supply.source(), world.getTime());

        return supply.state() == State.DELIVERED;
    }

    /** Se algum baú da colônia já tem o material desta peça — só pergunta, não tira. */
    static boolean isInTheChests(ServerWorld world, ConstructionProject project, ResourceId piece) {
        return MinecraftTypeAdapter.toBlock(piece)
                .map(block -> MaterialChoice.forBlock(block).stream().anyMatch(item ->
                        ColonySupply.canProvide(world, project.colonyId(), project.origin(), item)))
                .orElse(false);
    }

    /** Quantas peças diferentes, além da próxima, contam tentativa por ciclo. */
    static final int AHEAD = 8;

    /**
     * As tentativas contam para as peças que faltam, não só para a próxima —
     * playtest de 2026-10-03.
     *
     * <p>A obra parava numa peça sem rota (papoula), esperava as três
     * tentativas, e só então batia na seguinte (grama, cerca, dente-de-leão,
     * terracota): um minuto cada, em fila. Aqui cada peça que falta ganha sua
     * tentativa no mesmo ciclo, e as que não têm rota aparecem juntas. A
     * próxima peça fica de fora: quem conta a dela é
     * {@link #hasMaterialForNextBlock}, e contar duas vezes encurtaria a espera.
     */
    static void prepareAhead(ServerWorld world, ConstructionProject project) {
        Optional<ResourceId> next = project.nextBlock().map(BlueprintBlock::block);
        int asked = 0;

        for (ResourceId id : project.remainingMaterials().keySet()) {
            if (asked >= AHEAD) {
                return;
            }

            Optional<Block> block = MinecraftTypeAdapter.toBlock(id);

            if (next.filter(id::equals).isPresent() || block.isEmpty()
                    || BlockShaping.isShapedFromTheGround(block.get().getDefaultState())
                    || BlockShaping.isNeverPlaced(block.get().getDefaultState())) {
                continue;
            }

            List<Item> choices = MaterialChoice.forBlock(block.get());

            if (choices.stream().anyMatch(item ->
                    ColonySupply.canProvide(world, project.colonyId(), project.origin(), item))) {
                continue;
            }

            asked++;
            ensureConstructionMaterial(world, project, choices);
        }
    }

    /** Mantém a obra abastecida quando a rota local não chega a entregar a peça. */
    static boolean hasOrStocksConstructionMaterial(
            ServerWorld world, ConstructionProject project, Item item) {

        if (ColonySupply.canProvide(world, project.colonyId(), project.origin(), item)) {
            // O ofício entregou: uma falta futura volta à primeira tentativa.
            BiomeConstructionSupply.routeDelivered(project.colonyId(), item);

            return true;
        }

        return ensureConstructionMaterial(world, project, List.of(item));
    }

    /**
     * Alternativas locais sempre ganham. Três faltas da mesma peça distinguem
     * uma rota que de fato entregou de uma rota apenas teórica. Receita de
     * bancada continua recebendo seus ingredientes ausentes; peça de fornalha
     * ou sem receita aparece no baú da obra quando a rota não chegou.
     */
    static boolean ensureConstructionMaterial(
            ServerWorld world, ConstructionProject project, List<Item> choices) {

        return supply(world, project, choices).state() == State.DELIVERED;
    }

    /** O estado e a fonte de uma decisão de suprimento — ADR-035 §3. */
    private record Supply(State state, Source source) {
    }

    private static final Supply NO_ITEM = new Supply(State.NO_SOLUTION, Source.NONE);

    private static final Supply BY_PROFESSION = new Supply(State.RESOLVING, Source.PROFESSION);

    private static final Supply COUNTING_TO_STOCK = new Supply(State.RESOLVING, Source.STOCKED);

    /**
     * A mesma decisão de sempre, agora dizendo por quê: só {@code DELIVERED}
     * deixa a obra seguir, como o {@code true} de antes.
     */
    private static Supply supply(ServerWorld world, ConstructionProject project, List<Item> choices) {
        if (choices.isEmpty()) {
            return NO_ITEM;
        }

        Item preferred = choices.getFirst();

        boolean terracotta = isStockedAfterMisses(preferred);
        boolean routeExists = choices.stream().anyMatch(candidate ->
                BiomeConstructionSupply.hasRouteInBiome(world, project.colonyId(), candidate));

        if (!terracotta && routeExists) {
            return BY_PROFESSION;
        }

        if (!BiomeConstructionSupply.failedProfessionAttempt(project.colonyId(), preferred)) {
            // Sem rota no bioma (ou terracota de rota só teórica): conta a
            // tentativa; na terceira a peça aparece no baú.
            return COUNTING_TO_STOCK;
        }

        // A família de terracota colorida parece ter rota por receita de
        // recoloração, mas isto não prova que o corante e a terracota neutra
        // chegaram a um artesão. Três faltas do bloco concreto encerram essa
        // rota apenas teórica sem alterar a prioridade das alternativas nas
        // duas primeiras tentativas.
        Optional<CraftingLookup.Bill> bill = CraftingLookup.billFor(world, preferred, any -> true);

        // A rota do bioma e a entrega são fatos diferentes. Terracota, por
        // exemplo, pode ter argila em teoria, mas a coleta pode nunca chegar
        // ao baú da obra. Depois da terceira falta, uma peça de manufatura
        // sem receita de bancada deixa de travar a construção.
        if (terracotta || bill.isEmpty()) {
            return BiomeConstructionSupply.stockForConstruction(
                    world, project.colonyId(), project.origin(), preferred)
                    ? new Supply(State.DELIVERED, Source.STOCKED)
                    : new Supply(State.NO_SOLUTION, Source.STOCKED);
        }

        if (routeExists) {
            return BY_PROFESSION;
        }

        // O tear pede linha: as linhas aparecem no baú do carpinteiro, e ele
        // faz o tear. A peça ainda não está no baú, então a obra espera.
        ProfessionType craftsman = CraftingWork.isMasonry(MinecraftTypeAdapter.toResourceId(preferred))
                ? ProfessionType.MASON
                : ProfessionType.CARPENTER;

        for (Map.Entry<Item, Integer> ingredient : bill.get().ingredients().entrySet()) {
            if (!BiomeConstructionSupply.hasRouteInBiome(
                    world, project.colonyId(), ingredient.getKey())) {

                BiomeConstructionSupply.stockForCraftsman(
                        world, project.colonyId(), project.origin(),
                        ingredient.getKey(), ingredient.getValue(), craftsman);
            }
        }

        return new Supply(State.RESOLVING, Source.CRAFTSMAN);
    }

    static boolean isStockedAfterMisses(Item item) {
        return STOCKED_AFTER_MISSES.contains(item);
    }

    /**
     * Falta material: a obra espera, e a tarefa volta para a fila.
     *
     * <p>WAITING_RESOURCES é estado previsto (Construction-System.md), e
     * não defeito. Quem destrava é o ciclo da colônia, que vê a falta e
     * pede o que falta — e o jogador, que estoca o que a colônia não
     * produz.
     *
     * <p>Até 2026-08-15 essa frase descrevia uma intenção que nenhum
     * código cumpria: a única transição para {@code BUILDING} estava na
     * criação do projeto, e {@code ensureTask} não abre tarefa fora de
     * {@code BUILDING}. Na prática isto era estado terminal — a obra da
     * sessão das 19:44 estava parada em 149 blocos com 52 tábuas no baú.
     * Quem destrava de verdade é {@code ConstructionPlanner.plan}, com
     * {@link #hasMaterialForNextBlock}.
     */
    static void waitForResources(
            ConstructionProject project, Job job, UUID workerId, BlueprintBlock block) {

        if (project.state() == ConstructionState.BUILDING) {
            project.moveTo(ConstructionState.WAITING_RESOURCES);
        }

        BuilderWork.finish(job, workerId, "no " + block.block() + " in the colony chests");
    }
}
