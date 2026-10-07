package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonySupply;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import java.util.Map;
import java.util.Optional;

/**
 * A obra pede ao artesão a peça que não é recurso declarado — P1.1
 * (2026-09-17) e A-3 (2026-10-02). Separado de {@link WaitingWork} em
 * 2026-10-02 pelo teto de 500 linhas; o texto veio sem mudança.
 */
public final class CraftsmanRequest {

    private CraftsmanRequest() {
    }

    /**
     * A peça que a obra espera não é recurso, e por isso ninguém a fazia
     * — P1.1, 2026-09-17.
     *
     * <p><b>O defeito, medido.</b> Playtest de 2026-09-17 às 08:43: a
     * biblioteca ficou oito minutos em {@code WAITING_RESOURCES} com
     * <b>628 de 628</b> blocos, esperando {@code cobblestone_stairs}, e a
     * colônia tinha <b>69 pedregulhos</b> no baú, um pedreiro com
     * cortador de pedra e a receita do próprio jogo. O log do mesmo ciclo
     * dizia {@code no mason work: no task open for it}.
     *
     * <p><b>Por que ninguém abria a tarefa.</b> Quem as cria é
     * {@code ColonyCycle.requestMissing}, e ele itera
     * {@code Map<ResourceType, Integer>}: o pedido nasce de um recurso
     * <b>declarado</b>, e {@code cobblestone_stairs} não é um deles.
     * Escada, porta, cerca e alçapão são peças da planta, não recursos
     * que a colônia conta — então some do planejador de tarefas, por
     * mais pedregulho que haja.
     *
     * <p><b>Por que não bastava declarar o recurso.</b> Seria a terceira
     * vez: {@code SMOOTH_STONE_SLAB} e {@code STONE_BRICKS} entraram
     * assim, e a ADR-009 §2 chama isso pelo nome — <i>"uma solução que
     * pede mais um nome a cada material novo deve ser questionada"</i>.
     * A próxima peça repetiria o defeito. Decisão do autor em 09-17: que
     * a obra abra a tarefa <b>pelo que ela pede</b>.
     *
     * <p><b>O que decide se dá para fazer é o jogo, e não uma lista.</b>
     * {@code ColonySupply.canProvide} consulta o {@code CraftingLookup},
     * que lê as receitas reais e desce até {@code RECIPE_DEPTH} níveis —
     * peça nova entra sozinha, que é a ADR-009 aplicada. Aqui não se
     * fabrica nada: só se abre o pedido, e quem lavra é
     * {@code CraftingWork}, que já sabe achar a peça pela obra.
     *
     * <p><b>Uma tarefa por vez, e não uma por ciclo.</b> A obra fica em
     * espera por muitos ciclos, e abrir um pedido a cada um encheria a
     * fila com a mesma peça — que é o defeito da fila que não esvaziava
     * (§17, E1), por outra porta.
     */
    public static void askForWhatTheWorkIsWaitingOn(ServerWorld world, Colony colony) {
        VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .filter(project -> project.state() == ConstructionState.WAITING_RESOURCES)
                .ifPresent(project -> project.remainingMaterials().keySet()  // a lista inteira (A-3, 10-02)
                        .forEach(wanted -> askTheCraftsmanFor(world, project, wanted)));
    }

    /**
     * Abre antecipadamente, pela lista inteira da obra (A-3), a tarefa da peça que a
     * cadeia de recursos não declara. A peça continua sendo feita apenas
     * quando os ingredientes já existem fisicamente nos baús.
     */
    public static void askBeforeTheWorkWaits(ServerWorld world, ConstructionProject project) {
        if (project.state() != ConstructionState.BUILDING) {
            return;
        }

        project.remainingMaterials().keySet().forEach(wanted -> askTheCraftsmanFor(world, project, wanted)); // A-3
    }

    private static boolean askTheCraftsmanFor(
            ServerWorld world, ConstructionProject project, ResourceId wanted) {

        Optional<Item> piece = MinecraftTypeAdapter.toBlock(wanted).map(Block::asItem);

        if (piece.isEmpty()) {
            return false;
        }

        // Já é recurso declarado: o ColonyCycle cuida dele, e abrir aqui
        // seria um segundo pedido pela mesma coisa.
        if (MinecraftTypeAdapter.toResourceType(piece.get()).isPresent()) {
            return false;
        }

        TaskType type = CraftingWork.isMasonry(wanted)
                ? TaskType.CRAFT_STONE_MATERIAL
                : TaskType.CRAFT_WOOD_MATERIAL;

        for (Task task : VillageColonyMod.TASKS.ofColony(project.colonyId())) {
            if (task.type() == type && task.isOpen()) {
                return false;
            }
        }

        if (WorkAssignment.countCapableOf(
                project.colonyId(), type.required(), VillageColonyMod.WORKERS) == 0) {

            // Ninguém sabe lavrar isto. Abrir a tarefa a deixaria na fila
            // para sempre, sem executor possível — mesma razão do
            // ColonyCycle.requestMissing.
            return false;
        }

        if (!ColonySupply.canProvide(
                world, project.colonyId(), project.origin(), piece.get())) {

            // A colônia não tem como fazer: falta ingrediente, e quem o
            // traz é o pedido de recurso, não o fabricante.
            return false;
        }

        VillageColonyMod.TASKS.create(
                project.colonyId(),
                type,
                TaskPriority.CONSTRUCTION_MATERIAL,
                // Nominal, como na tarefa de obra: o pedido não é de
                // recurso, e quem acha a peça é CraftingWork lendo a
                // própria obra. Ver ConstructionPlanner.ensureTask.
                ResourceType.COBBLESTONE,
                1);

        VillageColonyMod.LOGGER.info(
                "Colony {} asks the {} for construction piece {}",
                project.colonyId(),
                type == TaskType.CRAFT_STONE_MATERIAL ? "mason" : "carpenter",
                wanted);

        return true;
    }
}
