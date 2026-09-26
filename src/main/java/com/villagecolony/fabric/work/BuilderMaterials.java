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
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            return true;
        }

        Optional<Block> material = MinecraftTypeAdapter.toBlock(next.get().block());

        if (material.isEmpty()) {
            // Bloco que este jogo não conhece. BuilderPlacement.placeOne o risca e segue,
            // então acordar a obra é o certo — ela não vai travar nele.
            return true;
        }

        if (BlockShaping.isShapedFromTheGround(material.get().getDefaultState())) {
            // Estes blocos são formados no local ou não têm item próprio;
            // por isso não podem deixar a obra esperando por estoque.
            return true;
        }

        if (TestBarrier.willStrike(world.getTime(), project.id(), next.get().block())) {
            // Peça que a barreira risca nunca segura a obra: quando o
            // construtor chegar nela vai passar por cima, então dizer
            // "tem" aqui é dizer a verdade sobre o que vai acontecer.
            //
            // <b>Era {@code furniture()} até 2026-08-21</b>, e virou isto
            // no dia em que cama e lampião saíram da barreira. As duas
            // perguntas coincidiam enquanto a Regra 21 vivia; deixar a
            // antiga poria a obra a acordar dizendo que tem a cama,
            // tentar, falhar e dormir de novo — todo ciclo, para sempre.
            //
            // <b>E era {@code chainFor} até 2026-09-09</b>, que respondia
            // "tem" desde a primeira falta. Com a carência isso passou a
            // ser mentira durante cinco ciclos: a obra acordaria, o
            // construtor esperaria, e ela dormiria de novo — o mesmo laço
            // do parágrafo acima, pela porta nova. Agora a pergunta é
            // sobre a peça <b>de que a barreira já desistiu</b>; a que
            // ela ainda espera cai no teste de material logo abaixo e
            // segura a obra, que é o que faz a colônia ir produzi-la.
            return true;
        }

        // A mesma lista de takeMaterial, e por obrigação: uma pergunta
        // que dissesse "tem" e uma retirada que não achasse poriam a obra
        // a acordar e voltar a dormir todo ciclo.
        for (Item candidate : MaterialChoice.forBlock(material.get())) {
            if (ColonySupply.canProvide(
                    world, project.colonyId(), project.origin(), candidate)) {

                return true;
            }
        }

        return ensureConstructionMaterial(world, project, MaterialChoice.forBlock(material.get()));
    }

    /** Mantém no baú a peça que nenhuma profissão consegue produzir. */
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
     * Alternativas locais sempre ganham. Só três faltas de uma família sem
     * profissão capaz autorizam a peça preferida a aparecer no baú da obra.
     */
    static boolean ensureConstructionMaterial(
            ServerWorld world, ConstructionProject project, List<Item> choices) {

        if (choices.isEmpty()) {
            return false;
        }

        Item preferred = choices.getFirst();

        if (choices.stream().anyMatch(candidate ->
                BiomeConstructionSupply.hasProfessionRoute(world, candidate))) {
            return false;
        }

        if (!BiomeConstructionSupply.failedProfessionAttempt(project.colonyId(), preferred)) {
            return false;
        }

        return BiomeConstructionSupply.stockForConstruction(
                world, project.colonyId(), project.origin(), preferred);
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
