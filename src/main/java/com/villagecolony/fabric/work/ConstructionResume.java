package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.LotClearance;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionReach;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.VillageRoad;
import com.villagecolony.fabric.integration.RoadExtension;
import com.villagecolony.fabric.integration.SweepLog;
import com.villagecolony.fabric.integration.SitePreparation;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A obra que volta do save: reconstruída do mundo, conferida contra a planta e
 * reaberta ou descartada — separado de {@link ConstructionPlanner} em
 * 2026-09-24, quando ele passou de 500 linhas. Os comentários vieram junto sem
 * mudança.
 */
final class ConstructionResume {

    private ConstructionResume() {
    }

    /**
     * Faz renascer a obra que o save trouxe.
     *
     * <p>O save guarda identidade, estrutura, lugar e estado — e não o
     * progresso. **Quem sabe o que já está de pé é o mundo**, e é a ele
     * que se pergunta: cada bloco do projeto cujo lugar já contém o bloco
     * certo sai da lista.
     *
     * <p>Sai mais barato no disco e sai mais certo. Uma lista de posições
     * gravada juraria que a parede está lá; se o jogador a derrubou entre
     * uma sessão e outra, a colônia a levanta de novo — e essa é a
     * resposta que se quer.
     *
     * <p>Custa uma leitura de bloco por peça do projeto, uma vez por
     * colônia por sessão. Cento e cinquenta leituras de vetor no primeiro
     * ciclo, e nada depois.
     *
     * <p>Roda dentro de {@link ConstructionPlanner#plan}, antes de tudo: uma obra que voltou
     * do save é uma obra aberta, e planejar outra por cima dela abriria
     * dois canteiros na mesma vila.
     */
    static void resume(ServerWorld world, Colony colony) {
        Optional<ConstructionService.Pending> pending =
                VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id());

        if (pending.isEmpty()) {
            return;
        }

        ConstructionService.Pending saved = pending.get();

        if (saved.blueprint().equals(StructureBlueprintReader.BIG_HOUSE_MOD)
                && VillageColonyMod.BUILDINGS.ofColony(colony.id()).stream()
                        .anyMatch(building -> building.finished()
                                && building.blueprint().equals(saved.blueprint())
                                && building.min().equals(saved.origin()))) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} drops saved BigHouseMOD repair at {} — foundation already stands",
                    colony.id(), saved.origin());
            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());
            return;
        }

        // <b>A obra abandonada que voltou aberta espera a vez dela</b> —
        // 2026-09-25, decisão do autor. O reparo antigo reabria a obra largada
        // antes do rodízio, e o save guardou uma delas aberta: o templo de
        // z=211 da vila do autor. Retomá-la aqui levantaria o mesmo templo na
        // vez da casa. A caixa abandonada continua no registro, com o lote
        // reservado, e o reparo a devolve quando for a vez do tipo — ver
        // HousePlans.isTurnOf.
        List<Building> buildings = VillageColonyMod.BUILDINGS.ofColony(colony.id());

        boolean abandoned = buildings.stream().anyMatch(building -> !building.finished()
                && building.blueprint().equals(saved.blueprint())
                && building.min().equals(saved.origin()));

        if (abandoned && !HousePlans.isTurnOf(
                buildings,
                VillageColonyMod.WORKERS.countOfColony(colony.id()),
                colony.observedBeds(),
                saved.blueprint())) {

            VillageColonyMod.LOGGER.info(
                    "Colony {} keeps the abandoned {} at {} waiting — it is not its turn,"
                            + " and the lot stays taken",
                    colony.id(), saved.blueprint(), saved.origin());

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        Optional<Blueprint> blueprint = PlanPlacement.blueprintOf(
                world, colony.id(), saved.blueprint(), saved.origin());

        if (blueprint.isEmpty()) {
            // O jogo não conhece mais essa estrutura — datapack que saiu,
            // versão que mudou. Desistir da obra é melhor que tentar a
            // cada ciclo: a casa pela metade fica no mundo, e o lote
            // ocupado impede a colônia de construir por cima dela.
            VillageColonyMod.LOGGER.warn(
                    "Colony {} had a project of {}, which this game no longer has — dropped",
                    colony.id(),
                    saved.blueprint());

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        ConstructionProject project = ConstructionProject.restore(
                saved.id(),
                saved.colonyId(),
                blueprint.get(),
                saved.origin(),
                saved.state(),
                saved.deferredPieces());

        // O jogador pode ter plantado no canteiro entre uma sessão e
        // outra — a Regra 23: o que já foi olhado se olha de novo.
        SitePreparation.clear(world, project);

        int standing = 0;

        for (BlueprintBlock block : project.blueprint().blocks()) {
            BlockPos where = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            if (expected.isPresent() && world.getBlockState(where).isOf(expected.get())) {
                project.markPlaced(block);

                standing++;
            }
        }

        // <b>Obra gravada em cima de outra construção é largada</b> —
        // 2026-09-19, visto em jogo. O portão de caixa de 15:49 governa
        // quem ESCOLHE lote; a retomada não passava por ele, então um
        // lote ruim escolhido por uma versão anterior sobrevivia a todo
        // conserto do scanner e voltava a cada carregamento do save.
        //
        // <b>Só a intocada</b>, e a razão é a mesma que o descarte
        // abaixo já escreve: casa pela metade é do jogador, e abandoná-la
        // deixaria um esqueleto no mundo com o lote ocupado. Com zero
        // blocos de pé não se perde nada.
        if (standing == 0 && LotClearance.overlapsSomethingBuilt(world, project)) {
            VillageColonyMod.LOGGER.warn(
                    "Colony {} drops the saved {} at {} — it sits inside something that is"
                            + " already built, and no block of it stands yet",
                    colony.id(),
                    project.blueprint().id(),
                    project.origin());

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        Optional<ResourceId> target = HousePlans.plansForNext(world, colony).stream()
                .map(Blueprint::id)
                .findFirst();

        // A obra salva só é substituída quando ainda não assentou nenhum
        // bloco e já não pertence à vez atual da sequência. Uma construção
        // iniciada continua sendo do jogador, mesmo que a próxima família
        // tenha mudado desde o último carregamento.
        if (target.isPresent() && project.isSupersededBy(target.get())) {
            // Obra de uma planta que não é mais o alvo, e sem um bloco de
            // pé. Nada se perde ao abandoná-la — e mantê-la trava a
            // colônia para sempre, porque `plan` não abre obra nova
            // enquanto houver uma aberta.
            //
            // Foi o que a sessão das 22:01 de 2026-08-15 mostrou. A Regra
            // 13 trocou a obra do MVP pela cabana, e a colônia continuou
            // presa à casa de planície gravada no save: quinze ciclos de
            // "waiting for minecraft:stripped_oak_log", que ninguém
            // produz. A cabana nunca chegou a ser planejada.
            //
            // **E aconteceu de novo, ao contrário.** A correção daquele
            // dia perguntava se a planta era a cabana, com o id escrito
            // no código. A Regra 24 devolveu a casa do jogo às vilas de
            // planície em 2026-08-19, e a pergunta passou a proteger
            // exatamente a obra que devia sair: a cabana gravada no save
            // era retomada, e o alvo novo nunca chegava a ser planejado.
            // Por isso o alvo agora é perguntado à colônia — `houseFor` é
            // a mesma resposta que `plan` usa uma linha abaixo.
            //
            // Com bloco de pé é o contrário: casa pela metade é do
            // jogador, e abandoná-la deixaria um esqueleto no mundo com o
            // lote ocupado. Essa continua de onde parou.
            VillageColonyMod.LOGGER.info(
                    "Colony {} drops the untouched {} — the target is now {}",
                    colony.id(),
                    project.blueprint().id(),
                    target.get());

            VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

            return;
        }

        VillageColonyMod.CONSTRUCTIONS.register(project);
        VillageColonyMod.CONSTRUCTIONS.dropPending(colony.id());

        VillageColonyMod.LOGGER.info(
                "Colony {} resumed {} at {} — {} blocks already standing, {} to go",
                colony.id(),
                project.blueprint().id(),
                project.origin(),
                standing,
                project.remainingCount());
    }
}
