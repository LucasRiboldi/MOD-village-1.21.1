package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.CraftingWork.Job;
import com.villagecolony.VillageColonyMod;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.coordination.ColonyGoals;
import com.villagecolony.core.coordination.StockRules;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Production;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.CraftingLookup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O que a bancada faz de fato: produzir a peça que a obra pede, descascar a
 * tora e converter a madeira — separado de {@link CraftingWork} em
 * 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>O {@code CraftingWork} cuida do ciclo do trabalhador (tarefa, passo,
 * guarda); estas são as receitas executadas. Os comentários vieram junto sem
 * mudança.
 */
final class CraftingSteps {

    private CraftingSteps() {
    }

    /**
     * Faz a primeira peça que a obra pede e a colônia não tem.
     *
     * <p>Pedido do autor em 2026-08-20: <i>"o fabricante deve descascar
     * stripped_oak, criar tocha e a vidraça, se os materiais necessários
     * existirem nos baús da vila"</i>.
     *
     * <p>São dois caminhos, e a diferença entre eles é do jogo e não do
     * mod:
     *
     * <pre>
     * descascar   não é receita de bancada — é machado no tronco. Sai
     *             de uma conversão nominal: oak_log vira
     *             stripped_oak_log, e o nome basta porque é convenção
     *             do próprio jogo
     *
     * montar      tocha e vidraça são receitas de verdade, e quem as
     *             conhece é o livro do jogo. CraftingLookup.billFor
     *             procura pelo RESULTADO, que é a pergunta certa aqui
     * </pre>
     *
     * @return se fez alguma coisa nesta passagem
     */
    static boolean produceForWork(ServerWorld world, Job job, UUID workerId) {
        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());

        if (colony.isEmpty()) {
            return false;
        }

        Optional<ConstructionProject> open =
                VillageColonyMod.CONSTRUCTIONS.openOf(colony.get().id());

        if (open.isEmpty()) {
            return false;
        }

        // <b>Este laço para no primeiro material que ele consegue
        // produzir, então a ordem é a prioridade do fabricante</b> — e
        // ela não era escolhida. {@code remainingMaterials} devolvia
        // mapa sem ordem até 2026-09-09, e o fabricante atendia o que o
        // sorteio mandasse.
        //
        // A sessão de 09-09 mediu o preço: treze lotes de escada, sete
        // de tábua, uma porta, uma vidraça — e <b>zero troncos
        // descascados</b>, com 59 toras no baú e o construtor parado em
        // "waiting for minecraft:stripped_oak_log". Descascar é o único
        // material que só sai daqui, e era o único que nunca ganhava a
        // vez; as dezesseis peças riscadas naquela casa foram todas ela.
        //
        // Agora a ordem é a da planta, e o primeiro material da lista é
        // o do bloco em que o construtor está parado — {@code nextBlock}
        // é a primeira posição que falta, e é dela que a lista começa a
        // ser contada. Não há prioridade a escrever aqui: basta a ordem
        // chegar inteira.
        boolean masonry = job.task.type() == TaskType.CRAFT_STONE_MATERIAL;

        List<ColonyPos> chests =
                ColonyChests.nearestFirst(world, colony.get().id(), colony.get().center());

        for (Map.Entry<ResourceId, Integer> remaining
                : open.get().remainingMaterials().entrySet()) {
            ResourceId wanted = remaining.getKey();

            // Cada oficina lavra a sua família — 2026-09-09. Sem esta
            // linha a divisão do fabricante seria só de nome: os dois
            // percorreriam a mesma lista e fariam a mesma peça, e o
            // segundo chegaria sempre para achar o trabalho feito.
            if (CraftingWork.isMasonry(wanted) != masonry) {
                continue;
            }

            Optional<Item> item = MinecraftTypeAdapter.toBlock(wanted).map(Block::asItem);

            if (item.isEmpty()) {
                continue;
            }

            if (ColonyChests.countIn(world, chests, item.get()) >= remaining.getValue()) {
                // A colônia já cobre toda a demanda restante desta peça.
                continue;
            }

            if (strip(world, colony.get(), wanted, workerId)
                    || stockFor(world, colony.get(), workerId, item.get(), remaining.getValue())) {

                return true;
            }
        }

        return false;
    }

    /**
     * A peça da própria tarefa, quando a obra não pede nada — ADR-039 D1. A meta
     * de adiantamento pede escada, laje, tijolo; sem este passo a tarefa caía na
     * conversão de tora em tábua e a peça nunca saía. O alvo fica fixo desde a
     * primeira passagem (estoque mais a quantidade da tarefa): entre um ciclo e
     * outro ela não passa da meta.
     *
     * @return vazio quando a tarefa é de tábua, tora ou viga (segue o caminho
     *     antigo); senão, se a peça foi feita ou já está no alvo
     */
    static Optional<Boolean> produceForGoal(ServerWorld world, Job job, UUID workerId) {
        ResourceType piece = job.task.targetResource();
        boolean crafted = piece.production() == Production.CRAFTED_WOOD
                || piece.production() == Production.CRAFTED_STONE;

        if (!crafted || piece.group() == ResourceGroup.PLANKS || piece.group() == ResourceGroup.STRIPPED) {
            return Optional.empty();
        }

        Optional<Colony> colony = VillageColonyMod.COLONIES.find(job.task.colonyId());
        Optional<Item> item = MinecraftTypeAdapter.toItem(piece);

        if (colony.isEmpty() || item.isEmpty()) {
            return Optional.empty();
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.get().id(), colony.get().center());
        int have = ColonyChests.countIn(world, chests, item.get());

        if (job.goalTarget < 0) {
            job.goalTarget = have + job.task.amount();
        }

        if (have >= job.goalTarget) {
            CraftingWork.finish(job, workerId, "made the " + piece.name().toLowerCase(java.util.Locale.ROOT) + " it was asked");

            return Optional.of(false);
        }

        if (!stockFor(world, colony.get(), workerId, item.get(), job.goalTarget)) {
            return Optional.empty();
        }

        job.crafted++;

        return Optional.of(true);
    }

    /** Faz a peça com material de toda a vila e a guarda no baú da profissão (ADR-038 P3c). */
    private static boolean stockFor(ServerWorld world, Colony colony, UUID workerId, Item item, int target) {
        Optional<ColonyPos> own = VillageColonyMod.STORAGES.of(workerId).map(WorkerStorage::chestPosition);

        return own.isPresent()
                ? ColonySupply.stockToward(world, colony.id(), colony.center(), item, target, own.get())
                : ColonySupply.stockToward(world, colony.id(), colony.center(), item, target);
    }

    /**
     * Descasca um tronco, se a colônia tiver um e a obra quiser o pelado.
     *
     * <p>Não passa pelo livro de receitas porque não é receita: no jogo,
     * descascar é passar o machado. O que se aproveita é o nome —
     * {@code stripped_oak_log} sai de {@code oak_log} —, e essa
     * convenção vale para as nove madeiras.
     */
    static boolean strip(
            ServerWorld world, Colony colony, ResourceId wanted, UUID workerId) {

        if (!wanted.path().startsWith("stripped_")) {
            return false;
        }

        Optional<Block> asked = MinecraftTypeAdapter.toBlock(wanted);

        if (asked.isEmpty()) {
            return false;
        }

        // <b>A espécie que a colônia tem, e não a que a planta escreveu</b>
        // — 2026-09-05. Era {@code oak_log} pelo nome, e a queixa do autor
        // foi <i>"na construção da casa não está sendo utilizado tronco e
        // está ficando vazio"</i>: a sessão de 09-05 riscou dezessete
        // {@code stripped_oak_log} tendo <b>295 toras de cerejeira</b> no
        // baú, e não descascou uma única vez.
        //
        // A ordem é a do MaterialChoice, então a preferida continua sendo
        // a da planta — o carvalho primeiro, e a cerejeira quando não há
        // carvalho. É a mesma lista que o construtor vai consultar na
        // hora de assentar, e as duas têm de concordar: descascar uma
        // espécie que a parede recusasse seria gastar tora à toa.
        for (Item naked : MaterialChoice.forBlock(asked.get())) {
            if (stripOne(world, colony, naked, workerId)) {
                return true;
            }
        }

        return false;
    }

    /** Descasca a tora desta espécie, se ela estiver em algum baú. */
    static boolean stripOne(
            ServerWorld world, Colony colony, Item naked, UUID workerId) {

        ResourceId id = MinecraftTypeAdapter.toResourceId(Block.getBlockFromItem(naked));

        String path = id.path();

        if (!path.startsWith("stripped_")) {
            return false;
        }

        ResourceId bark = new ResourceId(
                id.namespace(), path.substring("stripped_".length()));

        Optional<Item> log = MinecraftTypeAdapter.toBlock(bark).map(Block::asItem);

        if (log.isEmpty()) {
            return false;
        }

        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());

        Optional<ColonyPos> output = VillageColonyMod.STORAGES.of(workerId)
                .map(WorkerStorage::chestPosition);

        if (output.isEmpty()) {
            return false;
        }

        ColonyPos source = null;

        for (ColonyPos chest : chests) {
            if (ChestWithdrawer.withdraw(world, chest, log.get(), 1) > 0) {
                source = chest;

                break;
            }
        }

        if (source == null) {
            return false;
        }

        if (ChestDepositor.freeSpaceFor(world, output.get(), naked) < 1) {
            ChestDepositor.deposit(world, source, log.get(), 1);

            return false;
        }

        ChestDepositor.deposit(world, output.get(), naked, 1);

        VillageColonyMod.LOGGER.info(
                "Carpenter {} stripped a {} into {}", workerId, bark.path(), path);

        return true;
    }

    /**
     * Tira um tronco dos baús da colônia, faz a tábua, devolve ao baú de
     * onde o tronco saiu.
     *
     * <p>Nesta ordem e no mesmo tick. A conferência de espaço vem antes
     * da retirada: tirar o tronco e descobrir depois que a tábua não cabe
     * seria destruir o tronco do jogador, que é exatamente o defeito que
     * o E3 registra do outro lado.
     *
     * <p><b>De qualquer baú da colônia, e não só do próprio.</b> Até
     * 2026-08-14 era só do próprio, por uma intenção que o mundo não
     * sustenta: quem colhe deposita no baú <em>dele</em>, e nada nunca põe
     * tronco no baú de um fabricante. A sessão daquele dia mostrou o
     * resultado — dezessete tarefas encerradas com "no logs left in the
     * chest", zero tábuas, e 134 troncos guardados na colônia. A meta da
     * Regra 5 se mede na colônia inteira ({@code ColonyGoals} soma o
     * {@code ResourceTally} dela); o executor media um baú só, e a
     * discordância entre os dois é que abria tarefa por ciclo para
     * encerrá-la no tick seguinte.
     *
     * <p>A tábua volta para o baú de onde o tronco veio <b>quando cabe</b>,
     * e para o mais próximo que couber quando não — 2026-09-05.
     *
     * <p><b>Esta linha dizia outra coisa, e a outra coisa era falsa.</b>
     * Ela garantia que <i>"o lugar aberto pela retirada é o lugar onde a
     * peça cabe"</i>, e o mundo não sustenta isso: tirar uma tora de uma
     * pilha de quarenta <b>não abre slot nenhum</b>, e quatro tábuas
     * voltam. Com o baú cheio a peça era destruída e só sobrava um WARN.
     *
     * <p><b>A sessão de 2026-09-05, às 20:03, contou 118 delas em quinze
     * minutos</b> — cento e dezoito toras moídas e jogadas fora, com o
     * estoque de tábua subindo cento e trinta e oito no mesmo período.
     * Perdeu-se perto de metade da produção do fabricante.
     *
     * <p>Agora ele pergunta <b>antes</b> de moer, como o descascar ao
     * lado já perguntava: sem lugar para a tábua, a tora volta inteira e
     * a tarefa encerra dizendo por quê. É o mesmo E3 do lenhador, e a
     * mesma resposta.
     */
    static boolean convertOne(ServerWorld world, Job job, UUID workerId) {
        if (!halfTheWoodMayStillBeConverted(world, job.task.colonyId())) {
            CraftingWork.finish(job, workerId, "half of the colony's wood stays in logs");

            return false;
        }

        Optional<ColonyPos> output = VillageColonyMod.STORAGES.of(workerId)
                .map(WorkerStorage::chestPosition);

        if (output.isEmpty()) {
            CraftingWork.finish(job, workerId, "no personal chest for crafted output");

            return false;
        }

        ColonyPos chest = null;
        List<ItemStack> logs = List.of();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(job.task.colonyId())) {
            Optional<WorkerStorage> owned = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (owned.isEmpty()) {
                continue;
            }

            ColonyPos candidate = owned.get().chestPosition();

            logs = ChestWithdrawer.withdrawGroup(world, candidate, ResourceGroup.WOOD, 1);

            if (!logs.isEmpty()) {
                chest = candidate;

                break;
            }
        }

        if (logs.isEmpty()) {
            CraftingWork.finish(job, workerId, "no logs left in the colony chests");

            return false;
        }

        ItemStack log = logs.get(0);

        Optional<ItemStack> planks = CraftingLookup.resultOfOne(world, log);

        if (planks.isEmpty()) {
            // O jogo não conhece receita para este tronco sozinho. Devolve
            // o que tirou: o item é do jogador, e some se ninguém o puser
            // de volta.
            ChestDepositor.deposit(world, chest, log.getItem(), log.getCount());

            CraftingWork.finish(job, workerId, "nothing to make out of " + log.getItem());

            return false;
        }

        ItemStack result = planks.get();

        if (ChestDepositor.freeSpaceFor(world, output.get(), result.getItem()) < result.getCount()) {
            ChestDepositor.deposit(world, chest, log.getItem(), log.getCount());

            CraftingWork.finish(job, workerId, "no room in the personal chest for " + result.getItem());

            return false;
        }

        ChestDepositor.deposit(world, output.get(), result.getItem(), result.getCount());

        job.crafted++;

        return true;
    }

    /**
     * A colônia ainda pode moer tora, ou a reserva já bateu — 2026-09-05.
     *
     * <p><b>Por que o portão é aqui, e não só na meta.</b> A meta de
     * tábua do {@link ColonyGoals} passou a respeitar a reserva no mesmo
     * dia, e sozinha ela não segura nada: o {@code ColonyCycle} só
     * cancela tarefa <b>ainda disponível</b> — <i>"quem já começou
     * termina"</i> —, e esta aqui converte a cada vinte tiques até não
     * sobrar tronco. Um fabricante que pegasse a tarefa antes de a meta
     * baixar moeria o estoque inteiro dentro do mesmo ciclo, e a reserva
     * existiria só no papel.
     *
     * <p><b>Lido dos baús na hora</b>, pelo argumento que o próprio
     * {@code ChestInventoryReader} escreve: o jogador tira madeira do baú
     * quando quer, e um total em cache erraria sem avisar. É a mesma
     * varredura que este método já faria logo abaixo para achar o tronco.
     *
     * <p>Baú que não pôde ser lido conta como vazio, e isso é conservador
     * na direção certa: menos madeira vista é menos conversão liberada,
     * e o pior caso é a colônia guardar tora a mais.
     */
    static boolean halfTheWoodMayStillBeConverted(ServerWorld world, UUID colonyId) {
        // <b>Os mesmos baús da meta</b> — playtest de 2026-10-03. A meta lê
        // todos os baús da vila (VillageInventoryObserver, Regra 45) e este
        // portão lia só os de trabalhador: tora num baú sem dono abria tarefa
        // de tábua que o carpinteiro fechava com 0 peça — 12 vezes em meia
        // hora, e 603 linhas de "stays in logs" desde 30-09. A tora também
        // sai de qualquer baú da vila (convertOne), então é esta a conta.
        ResourceTally owned = VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> ChestInventoryReader.survey(
                        world, ColonyChests.nearestFirst(world, colonyId, colony.center())).resources().total())
                .orElseGet(ResourceTally::empty);

        Map<ResourceId, Integer> stillNeeded = VillageColonyMod.CONSTRUCTIONS.openOf(colonyId)
                .map(ConstructionProject::remainingMaterials)
                .orElseGet(Map::of);

        return StockRules.logsThatMayBeConverted(
                owned.amountOfGroup(ResourceGroup.WOOD),
                owned.amountOfGroup(ResourceGroup.PLANKS),
                stillNeeded) > 0;
    }
}
