package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * A colônia decide construir — TASK-033.
 *
 * <p>Uma obra por colônia de cada vez, e só quando há quem a execute, e
 * só onde a Regra 6 deixa. Não põe bloco algum: quem põe é
 * {@link BuilderWork}.
 *
 * <p><b>A torneira por último.</b> Não se abre obra sem construtor na
 * vila. Uma obra sem executor ficaria aberta para sempre, e a meta de
 * tábua da Regra 5 passaria a apontar para uma casa que ninguém levanta
 * — o fabricante encheria os baús de tábua por causa de um canteiro
 * fantasma. É a lição do §11, e é a mesma ordem que a Fase 9 seguiu.
 *
 * <p><b>Quando a vila para de crescer.</b> Por regra, nunca: o autor
 * decidiu em 2026-08-14 que se constrói enquanto houver material e
 * espaço. O freio é o mundo — só há obra onde há lote livre encostado em
 * rua, e é {@code BuildSiteScanner} quem responde isso.
 */
public final class ConstructionPlanner {

    /**
     * O projeto lido do jogo, guardado depois da primeira leitura.
     *
     * <p>Ler um template é abrir e decodificar um arquivo, e a casa não
     * muda entre um ciclo e o outro. Sem esta memória, cada ciclo de cada
     * colônia pagaria a leitura inteira.
     *
     * <p>Estático e sem limpeza: é um objeto imutável de algumas centenas
     * de blocos, e o mesmo para todo mundo. Trocar de mundo não o
     * invalida — a casa de planície é a mesma em qualquer save.
     */
    private static Blueprint house;

    private ConstructionPlanner() {
    }

    /**
     * Decide, se for o caso, a próxima obra desta colônia.
     *
     * @return a obra recém-planejada, quando nasce uma agora
     */
    public static Optional<ConstructionProject> plan(ServerWorld world, Colony colony) {
        if (VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).isPresent()) {
            return Optional.empty();
        }

        int builders = WorkAssignment.countCapableOf(
                colony.id(), TaskType.BUILD.required(), VillageColonyMod.WORKERS);

        if (builders == 0) {
            return Optional.empty();
        }

        Optional<Blueprint> blueprint = houseOf(world);

        if (blueprint.isEmpty()) {
            return Optional.empty();
        }

        Optional<ColonyPos> site = BuildSiteScanner.find(
                world, colony.center(), VillageDetector.SEARCH_RADIUS, blueprint.get().size());

        if (site.isEmpty()) {
            return Optional.empty();
        }

        if (VillageColonyMod.BUILDINGS.isColonyInfrastructure(site.get())) {
            // O lote caiu sobre casa que a própria colônia levantou. O
            // scanner não conhece o registro de construções — ele olha o
            // mundo, e uma casa de madeira sobre terra continua parecendo
            // terreno pelo topo do bloco. A próxima passagem tenta outro
            // anel.
            return Optional.empty();
        }

        ConstructionProject project =
                ConstructionProject.plan(colony.id(), blueprint.get(), site.get());

        VillageColonyMod.CONSTRUCTIONS.register(project);

        // PREPARING passa direto, e é honesto dizer por quê: o lote só é
        // aceito quando não há nada em cima dele — BuildSiteScanner
        // reprova a coluna que tenha qualquer bloco acima da janela. A
        // limpeza de terreno prevista em Construction-System.md §PREPARING
        // não tem o que limpar, e implementá-la agora seria escrever
        // código para um caso que a escolha do lote já excluiu.
        project.moveTo(ConstructionState.PREPARING);
        project.moveTo(ConstructionState.BUILDING);

        VillageColonyMod.LOGGER.info(
                "Colony {} planned {} at {} — {} blocks, {} builders",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                project.blueprint().blockCount(),
                builders);

        return Optional.of(project);
    }

    /**
     * Quantas tábuas a obra em curso ainda pede.
     *
     * <p>É o número que a Regra 5 usa para substituir a metade do
     * armazém. Zero quando não há obra — e aí volta a valer a metade.
     */
    public static int planksNeededBy(ResourceId planks, Colony colony) {
        return VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(project -> project.remainingMaterials().getOrDefault(planks, 0))
                .orElse(0);
    }

    /** Esquece o projeto guardado. Só os testes precisam disso. */
    public static void forgetBlueprint() {
        house = null;
    }

    private static Optional<Blueprint> houseOf(ServerWorld world) {
        if (house == null) {
            house = StructureBlueprintReader
                    .read(world, StructureBlueprintReader.PLAINS_SMALL_HOUSE)
                    .orElse(null);
        }

        return Optional.ofNullable(house);
    }
}
