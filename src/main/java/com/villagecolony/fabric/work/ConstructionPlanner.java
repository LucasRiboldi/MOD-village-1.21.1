package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

    /**
     * Como esta fase aparece na linha de {@link IdleLog}.
     *
     * <p>O motivo de não haver obra existe por causa da sessão de
     * 2026-08-14, à noite: a Fase 10 não produziu linha nenhuma — nem
     * obra, nem recusa — e havia cinco caminhos silenciosos por onde ela
     * podia ter saído. Do lado de fora, "não tem construtor", "o jogo não
     * tem essa casa" e "não há lote" eram o mesmo silêncio. É o §11: a
     * linha que expõe o defeito precisa existir antes de alguém
     * desconfiar dele.
     *
     * <p>A memória do último motivo, que morava aqui, virou
     * {@link IdleLog} em 2026-08-15 — o lenhador e o fabricante
     * precisavam da mesma regra.
     */
    private static final String SUBJECT = "building";

    private ConstructionPlanner() {
    }

    /**
     * Registra por que não houve obra, uma vez por motivo.
     *
     * <p>A memória do último motivo saiu daqui em 2026-08-15 e virou
     * {@link IdleLog}: o lenhador e o fabricante precisavam da mesma
     * regra, e ela estava escrita só aqui. O que ficou é a tradução das
     * cinco recusas desta fase para o vocabulário do {@link IdleReason}.
     *
     * @return sempre vazio, para servir de {@code return} das recusas
     */
    private static Optional<ConstructionProject> silent(
            Colony colony, IdleReason why, String detail) {

        IdleLog.record(colony.id(), SUBJECT, why, detail);

        return Optional.empty();
    }

    /** Esquece o motivo guardado. Chamado ao parar o servidor. */
    public static void clearAll() {
        IdleLog.clearAll();
    }

    /**
     * Garante que a obra aberta tenha uma tarefa por onde alguém a pegue.
     *
     * <p><b>É o defeito que a sessão de 2026-08-15 achou, e o último do
     * MVP.</b> A obra existia, o construtor existia com baú, e as duas
     * casas ficaram em {@code 151 blocks left} por cinco horas e quarenta
     * minutos com {@code 0 working}. A corrente arrebentava aqui: nada em
     * produção criava tarefa de construção.
     *
     * <p>{@code tasks.create} só era chamado de {@code ColonyCycle
     * .requestMissing}, que traduz falta de recurso em tarefa; e
     * {@code typeFor} só devolve {@code BUILD} para recurso de categoria
     * {@code CONSTRUCTION}, que <b>nenhum {@code ResourceType} tem</b>.
     * A tarefa de obra era estruturalmente impossível, e por isso
     * {@code BuilderWork} nunca teve o que fazer — em vila nenhuma.
     *
     * <p>Os 82 testes verdes não pegaram porque {@code BuilderGameTest}
     * criava a tarefa à mão. É o §11 pela segunda vez, com a mesma frase
     * que o E10 rendeu: a pergunta não é "este código funciona?", é
     * <em>"quem põe esta coisa aqui, em jogo?"</em>.
     *
     * <p>Quem põe passou a ser este método, e não o ciclo: a obra não é
     * uma falta de recurso — é um projeto aberto precisando de mão. O
     * ciclo continua dono do que nasce de falta, e a vida desta tarefa
     * pertence ao projeto. Ver {@code TaskType.isResourceRequest}.
     *
     * <p>Uma tarefa por vez, e só enquanto a obra está em
     * {@link ConstructionState#BUILDING}: em {@code WAITING_RESOURCES} não
     * há o que colocar, e abrir tarefa ali poria um construtor a andar
     * até um canteiro para não fazer nada.
     */
    private static void ensureTask(Colony colony, ConstructionProject project) {
        if (project.state() != ConstructionState.BUILDING) {
            return;
        }

        for (Task task : VillageColonyMod.TASKS.ofColony(colony.id())) {
            if (task.type() == TaskType.BUILD && task.isOpen()) {
                return;
            }
        }

        int blocks = project.remainingCount();

        if (blocks == 0) {
            // A obra acabou e ainda não foi encerrada. Quem a fecha é o
            // construtor ao pôr o último bloco; abrir tarefa para zero
            // blocos seria ocupar uma mão por nada — e Task.create
            // recusaria, com razão.
            return;
        }

        VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.BUILD,
                TaskPriority.CONSTRUCTION,
                // Nominal, e é seguro que seja: a tarefa de obra não é
                // pedido de recurso, e quem paga cada bloco é
                // BuilderWork.takeMaterial, lendo o projeto. O que este
                // campo carrega de útil é o número — quantos blocos
                // faltam —, que aparece no log.
                ResourceType.OAK_PLANKS,
                blocks);

        VillageColonyMod.LOGGER.info(
                "Colony {} opened a build task — {} blocks left of {}",
                colony.id(),
                blocks,
                project.blueprint().id());
    }

    /**
     * Decide, se for o caso, a próxima obra desta colônia.
     *
     * @return a obra recém-planejada, quando nasce uma agora
     */
    public static Optional<ConstructionProject> plan(ServerWorld world, Colony colony) {
        resume(world, colony);

        Optional<ConstructionProject> open = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id());

        if (open.isPresent()) {
            ensureTask(colony, open.get());

            return silent(colony, IdleReason.ALREADY_OPEN, "");
        }

        int builders = WorkAssignment.countCapableOf(
                colony.id(), TaskType.BUILD.required(), VillageColonyMod.WORKERS);

        if (builders == 0) {
            // Sem detalhe: a frase do motivo já diz "no worker in the
            // village can do it", e o assunto da linha já diz que o
            // trabalho é de construção. Repetir "no builder" aqui só
            // alonga a linha.
            return silent(colony, IdleReason.NO_WORKER, "");
        }

        Optional<Blueprint> blueprint = houseOf(world);

        if (blueprint.isEmpty()) {
            return silent(
                    colony,
                    IdleReason.NOT_IN_GAME,
                    "no " + StructureBlueprintReader.PLAINS_SMALL_HOUSE + " to read");
        }

        Optional<ColonyPos> site = BuildSiteScanner.find(
                world, colony.center(), VillageDetector.SEARCH_RADIUS, blueprint.get().size());

        if (site.isEmpty()) {
            // Duas respostas, e a diferença importa: uma diz que não há
            // lote, a outra diz que ninguém terminou de olhar. A linha
            // anterior dizia a primeira nos dois casos, e no segundo isso
            // era mentira — ver o E14 do §17.
            //
            // Sem o número do anel de propósito: IdleLog só registra
            // quando o motivo muda, e um anel diferente por ciclo faria a
            // linha voltar toda vez.
            return BuildSiteScanner.sweepPausedAt(colony.center()).isPresent()
                    ? silent(colony, IdleReason.SWEEP_INCOMPLETE, "looking for a lot")
                    : silent(
                            colony,
                            IdleReason.NO_TARGET,
                            "no free lot beside a road in the whole "
                                    + VillageDetector.SEARCH_RADIUS + "-block radius of "
                                    + colony.center() + " that fits "
                                    + blueprint.get().size());
        }

        if (VillageColonyMod.BUILDINGS.isColonyInfrastructure(site.get())) {
            // O lote caiu sobre casa que a própria colônia levantou. O
            // scanner não conhece o registro de construções — ele olha o
            // mundo, e uma casa de madeira sobre terra continua parecendo
            // terreno pelo topo do bloco. A próxima passagem tenta outro
            // anel.
            return silent(
                    colony,
                    IdleReason.SITE_REFUSED,
                    "the lot at " + site.get() + " falls on a colony building");
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

        ensureTask(colony, project);

        IdleLog.clear(colony.id(), SUBJECT);

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
     * <p>Roda dentro de {@link #plan}, antes de tudo: uma obra que voltou
     * do save é uma obra aberta, e planejar outra por cima dela abriria
     * dois canteiros na mesma vila.
     */
    private static void resume(ServerWorld world, Colony colony) {
        Optional<ConstructionService.Pending> pending =
                VillageColonyMod.CONSTRUCTIONS.pendingOf(colony.id());

        if (pending.isEmpty()) {
            return;
        }

        ConstructionService.Pending saved = pending.get();

        Optional<Blueprint> blueprint = StructureBlueprintReader.read(world, saved.blueprint());

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
                saved.id(), saved.colonyId(), blueprint.get(), saved.origin(), saved.state());

        int standing = 0;

        for (BlueprintBlock block : project.blueprint().blocks()) {
            BlockPos where = MinecraftTypeAdapter.toBlockPos(project.worldPositionOf(block));

            Optional<Block> expected = MinecraftTypeAdapter.toBlock(block.block());

            if (expected.isPresent() && world.getBlockState(where).isOf(expected.get())) {
                project.markPlaced(block);

                standing++;
            }
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
