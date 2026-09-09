package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.Capability;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.TreeHarvester;
import com.villagecolony.fabric.integration.TreeScanner;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Qual árvore agora, e quando desistir dela.
 *
 * <p>Saiu de {@code LumberjackWork} em 2026-08-20, quando ele passou de
 * mil e duzentas linhas. As duas metades estão juntas porque são a
 * mesma decisão de dois lados: escolher é olhar o que sobrou depois das
 * recusas, e desistir é o que produz a recusa seguinte.
 *
 * <p>Quem responde "esta árvore está fora" é {@link TreeMarks}; quem
 * responde "esta já é de alguém" é {@link TreeClaims}. Aqui fica só a
 * ordem em que as perguntas são feitas, e o que se faz com o não.
 *
 * <p>O guarda de travamento é a Regra 9 fechando: um lenhador que anda
 * sem chegar não está trabalhando, e devolver a tarefa sem marcar a
 * árvore faria a busca reencontrar exatamente a mesma no ciclo
 * seguinte — a roda que custou a sessão de 2026-08-18.
 */
public final class TreeChoice {

    /**
     * Quantos ticks de trabalho sem nenhum avanço antes de desistir.
     *
     * <p>Quatro ciclos da colônia. É muito de propósito: derrubar um
     * tronco leva dez ticks, e atravessar o raio de busca a pé leva bem
     * menos que isto. Um lenhador que passou dois minutos de horário de
     * trabalho sem quebrar um bloco nem começar uma árvore não está
     * demorando — está preso.
     *
     * <p>Existe porque a tarefa reservada não tinha como voltar para a
     * fila enquanto o trabalhador estivesse vivo. A morte e a
     * zumbificação liberam, por {@code VillagerLifecycleHandler}; o
     * aldeão vivo que o jogador levou de barco, que caiu num buraco ou
     * que ficou do lado errado de uma parede, não. A vaga da profissão
     * ficava ocupada por alguém que nunca chegaria, e a colônia não
     * abria pedido novo porque, para ela, aquele pedido tinha dono.
     *
     * <p>O relógio só corre em horário de trabalho e com o aldeão
     * carregado — ver {@link #step}. Uma noite inteira não é
     * travamento, e chunk descarregado é a colônia dormindo.
     */
    static final int STALL_LIMIT = 4 * VillageDetector.CYCLE_TICKS;

    /**
     * O limite em vigor. É {@link #STALL_LIMIT}, menos nos testes.
     *
     * <p>Existe por decisão do autor em 2026-08-15, e a razão é o E1 do
     * grupo E: 2.400 ticks são dois minutos de relógio, contra uma
     * bateria que roda inteira em vinte e cinco segundos. O guarda de
     * travamento nunca teve teste por isso — e desde a Regra 9 ele carrega
     * também a marcação de árvore fora de alcance, que é o que fecha o
     * G2. Dois comportamentos sem cobertura no mesmo lugar.
     *
     * <p>É código de produção existindo para teste, e isso se paga com
     * limites: só a bateria mexe aqui, sempre devolvendo ao padrão por
     * {@link #restoreStallLimit()}, e o valor de jogo continua sendo o
     * único que o mod usa sozinho.
     */
    static int stallLimit = STALL_LIMIT;

    /** Encurta o relógio de travamento. Só os testes precisam disso. */
    public static void shortenStallLimitTo(int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("stall limit must be positive: " + ticks);
        }

        stallLimit = ticks;
    }

    /** Devolve o relógio ao valor de jogo. */
    public static void restoreStallLimit() {
        stallLimit = STALL_LIMIT;
    }

    private TreeChoice() {
    }

    /**
     * Escolhe a próxima árvore, ou encerra a tarefa.
     *
     * <p>É aqui que a Regra 1 fecha o ciclo: enquanto couber madeira no
     * baú, há árvore nova; quando não couber, a tarefa termina. A
     * colônia reavalia no ciclo seguinte e só pede de novo se o espaço
     * tiver voltado — o jogador esvaziou o baú, um baú novo entrou no
     * registro.
     */
    static LumberjackWork.Outcome startNextTree(
            ServerWorld world,
            VillagerEntity villager,
            LumberjackWork.Job job,
            WorkerStorage storage,
            boolean maySearch) {

        if (job.plan != null) {
            // A árvore anterior acabou de descer. Fechar antes de
            // procurar outra: é a ordem pedida pelo autor — derrubar,
            // recolher, e só então replantar.
            TreeHarvester.finish(world, job.plan);

            TreeClaims.unclaim(job.plan);

            VillageColonyMod.LOGGER.info(
                    "Worker {} finished the tree at {} — {} logs and {} leaves,"
                            + " {} logs this task",
                    villager.getUuid(),
                    job.plan.base().toShortString(),
                    job.plan.logs(),
                    job.plan.leaves(),
                    job.collected);

            job.plan = null;
        }

        if (!maySearch) {
            return LumberjackWork.Outcome.WORKED;
        }

        Optional<BlockPos> tree = TreeScanner.findNearestLog(
                world,
                job.center,
                LumberjackWork.SEARCH_RADIUS,
                log -> !TreeClaims.isTaken(log)
                        && !TreeMarks.isRejected(world, log)
                        && !TreeMarks.isOutOfReach(world, log));

        if (tree.isEmpty()) {
            // Nenhuma árvore ao alcance. Não é motivo para encerrar: a
            // floresta cresce, e a muda replantada volta a ser árvore.
            return LumberjackWork.Outcome.SEARCHED;
        }

        // Perguntar antes de derrubar. O tronco sai do mundo sem drop,
        // então madeira que não coubesse no baú seria madeira destruída:
        // a árvore sumiria e a colônia não ficaria com nada. Recolher
        // todos os recursos da árvore começa em não derrubar a árvore
        // que não se pode recolher.
        //
        // A conta é a do tronco, que é certa: um bloco, um item. O que a
        // folha dá é sorteado na hora — muda, maçã, graveto, ou nada — e
        // não dá para perguntar de antemão. São poucos itens, e o espaço
        // conferido para o tronco inteiro sobra para eles.
        List<BlockPos> trunkGroup = TreeHarvester.trunkOf(world, tree.get());

        // A conta é da colônia, e não do baú deste lenhador. Foi a
        // correção de 2026-09-04: o baú próprio assoreia de vara e maçã
        // — que nenhum ResourceGroup cobre e nenhum trabalhador retira —,
        // e o espaço dele só desce. Ao chegar a zero, esta guarda
        // encerrava a tarefa antes de a primeira árvore cair; a colônia
        // reabria a tarefa no ciclo seguinte, e o lenhador passou
        // cinquenta e nove ciclos nascendo e morrendo sem derrubar nada,
        // com a obra parada esperando a madeira.
        //
        // Medir aqui o que `TreeFelling.deposit` vai usar lá é o ponto:
        // guarda e depósito discordando sobre onde a madeira cabe é o
        // que destruía o tronco.
        List<ColonyPos> chests =
                ColonyChests.ownFirst(job.task.colonyId(), storage.chestPosition());

        int room = ColonyChests.freeSpaceForGroup(world, chests, ResourceGroup.WOOD);

        if (room < trunkGroup.size()) {
            TreeFelling.finishTask(job, villager.getUuid(), storage, room);

            return LumberjackWork.Outcome.DONE;
        }

        TreeHarvester.Plan plan = TreeHarvester.plan(world, tree.get());

        if (plan.isEmpty()) {
            // Não é árvore: a regra da copa recusou. Recusar em silêncio
            // e sair daqui faria a busca reencontrar este mesmo tronco no
            // ciclo seguinte, e no seguinte — ver REJECTED.
            TreeMarks.reject(world, trunkGroup);

            return LumberjackWork.Outcome.SEARCHED;
        }

        TreeClaims.claim(plan);

        job.plan = plan;
        job.index = 0;
        job.progress = 0;
        job.required = 0;
        job.stalled = 0;

        if (job.task.state() == TaskState.RESERVED) {
            job.task.start();
        }

        walkTo(villager, plan.base());

        return LumberjackWork.Outcome.SEARCHED;
    }

    /**
     * Ele parou de andar, e há quantos tiques — o guarda de imobilidade.
     *
     * <p>Método, e não string montada no lugar da chamada, pelo motivo
     * que o {@code footingIn} do mineiro já registrou em 2026-09-05: o
     * que não é função o teste reimplementa, e aí passa a medir a cópia.
     * Aqui o que se afirma é justamente que o <b>número é o contador</b>,
     * e não a constante.
     */
    static String motionless(int ticks) {
        return "has not moved a block in " + ticks + " work ticks";
    }

    /** Ele andou e não chegou, e há quantos tiques — o guarda de travamento. */
    static String noProgress(int ticks) {
        return "made no progress for " + ticks + " work ticks";
    }

    /**
     * Qual dos dois guardas falou, e com que contador — 2026-09-09.
     *
     * <p><b>A escolha é que precisa ser função, e não só a frase.</b> Um
     * teste que só exercita {@link #motionless} e {@link #noProgress}
     * mede a interpolação, e o defeito de 09-05 não estava lá: estava no
     * <b>argumento</b>, que era {@link #stallLimit}. Com os dois ramos
     * aqui dentro, a asserção enxerga o que o chamador não pode mais
     * escolher errado — passar a constante deixou de ser uma opção
     * porque a constante não é parâmetro.
     *
     * @param motionless se quem devolveu a tarefa foi o guarda de
     *     imobilidade, que fala aos {@code WorkStall.LIMIT}
     * @param stillTicks há quantos tiques ele não sai do bloco
     * @param stalledTicks há quantos tiques ele anda sem chegar
     */
    static String reasonFor(boolean motionless, int stillTicks, int stalledTicks) {
        return motionless ? motionless(stillTicks) : noProgress(stalledTicks);
    }

    /**
     * Devolve à fila a tarefa de um lenhador que parou de andar.
     *
     * <p>Mesmo desfecho do trabalhador sem baú, e pelo mesmo motivo: a
     * tarefa continua fazendo sentido, e quem não a fará é este
     * trabalhador. Solta o tronco reservado, apaga o destino para o
     * aldeão voltar à agenda Vanilla, e diz o que houve — este caminho
     * em silêncio seria indistinguível de trabalho acontecendo, que é a
     * forma que o E1 assume toda vez que reaparece.
     *
     * <p><b>E o motivo vem de quem desistiu</b> — 2026-09-09. A linha
     * imprimia {@link #stallLimit}, que é a <b>constante</b>: ela dizia
     * <i>"made no progress for 2400 work ticks"</i> em desistências
     * separadas por 600 tiques, e a mesma frase saía para os dois
     * guardas — o de imobilidade, que fala aos 300, e o de travamento,
     * que fala aos 2.400. Oito vezes de diferença com o mesmo texto.
     * Quase mandou procurar o defeito no guarda em vez de no castigo,
     * na sessão de 09-05.
     *
     * <p>É o molde que o mineiro já usa: {@code MinerWork.giveUp} recebe
     * o motivo pronto de quem o chamou, com o número que aquele guarda
     * de fato contou.
     *
     * @param why o que aconteceu, com o contador de verdade — não a
     *     constante do limite
     */
    static LumberjackWork.Outcome giveUp(
            ServerWorld world, LumberjackWork.Job job, UUID workerId, String why) {

        job.task.release();

        WorkTargets.clear(workerId);

        VillageColonyMod.LOGGER.info(
                "Worker {} {}{} — wood task returned to the queue",
                LumberjackReport.shortId(workerId),
                why,
                job.plan == null
                        ? " while looking for a tree"
                        : " on the tree at " + job.plan.base().toShortString());

        if (job.plan != null) {
            // A outra metade da Regra 9, e o que fecha o G2. Soltar a
            // tarefa sem esquecer a árvore troca de trabalhador e não de
            // problema: a busca é determinística a partir do centro, essa
            // árvore continua sendo a mais próxima, e o substituto anda
            // até ela para travar no mesmo lugar.
            TreeMarks.markUnreachable(world, job.plan.base());
        }

        // E a madeira descansa para ele — ADR-010. Esquecer a árvore
        // manda o lenhador para a seguinte; quando é a floresta inteira
        // que não se alcança, a seguinte também não vai render, e o que
        // faz a colônia continuar produzindo é ele mudar de foco.
        VillageColonyMod.WORKERS.find(workerId)
                .ifPresent(worker -> worker.rest(Capability.COLLECT_WOOD));

        return LumberjackWork.Outcome.DONE;
    }

    /**
     * Manda o aldeão andar até a árvore.
     *
     * <p>Escrever o destino em {@link WorkTargets} e deixar a
     * {@code GoToWorkTargetTask} conduzir. A versão anterior chamava
     * {@code getNavigation().startMovingTo} daqui e o aldeão nunca
     * chegou: o cérebro Vanilla reescrevia o destino no mesmo tick,
     * seguindo a agenda dele. Quem manda no caminho é o Brain, então o
     * pedido passou a ser feito na língua dele.
     *
     * <p>Reposto a cada tick enquanto ele estiver a caminho. Não é
     * desperdício: a leitura de 2026-08-08 mostrou o {@code WALK_TARGET}
     * já descartado pelo Vanilla no instante em que o aldeão chegava, e
     * é a reposição — não a primeira escrita — que faz o caminho
     * acontecer. Ver §17, E4.
     */
    static void walkTo(VillagerEntity villager, BlockPos tree) {
        WorkTargets.set(villager.getUuid(), tree);
    }
}
