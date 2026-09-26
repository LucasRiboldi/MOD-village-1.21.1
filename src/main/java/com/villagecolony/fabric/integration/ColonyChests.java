package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Os baús de uma colônia, do mais perto para o mais longe — a Regra 10.
 *
 * <p>A decisão do autor, de 2026-08-15: o construtor tem acesso a
 * qualquer baú da vila, <b>começa pelo mais próximo</b> e vai abrindo o
 * seguinte enquanto não juntar a quantidade de que precisa.
 *
 * <p>Nenhuma das duas metades existia. Quem percorria os baús —
 * {@code BuilderMaterials.takeMaterial} e {@code CraftingWork.convertOne}
 * — usava a ordem em que {@code WORKERS.ofColony} devolve os
 * trabalhadores, que não é distância nenhuma: é a ordem de registro. E
 * os dois desistiam no primeiro baú que não tivesse tudo, de modo que
 * três tábuas num baú e três em outro eram seis tábuas que a colônia
 * tinha e não conseguia usar.
 *
 * <p>O baú é registrado por trabalhador, e é por isso que a lista sai do
 * registro de trabalhadores. Trabalhador sem baú simplesmente não
 * contribui.
 */
public final class ColonyChests {

    private ColonyChests() {
    }

    /**
     * Os baús desta colônia, ordenados pela distância até {@code from}.
     *
     * <p>A distância é a do quadrado, sem raiz: comparar quadrados
     * ordena igual e não paga a raiz por baú a cada consulta.
     *
     * <p>Empate é resolvido pela posição, e não deixado ao acaso: duas
     * colônias com o mesmo mapa precisam crescer igual entre sessões,
     * senão o relatório de uma não explica a outra.
     */
    public static List<ColonyPos> nearestFirst(
            ServerWorld world, UUID colonyId, ColonyPos from) {

        List<ColonyPos> chests = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (storage.isPresent() && !chests.contains(storage.get().chestPosition())) {
                chests.add(storage.get().chestPosition());
            }
        }

        addMineMouth(world, colonyId, chests);

        // <b>E os baús que estão na vila e não são de ninguém</b> —
        // 2026-09-16, decisão do autor: <i>"permitir que o recurso que
        // falta possa ser recolhido de qualquer baú que esteja na vila
        // automaticamente"</i>.
        //
        // O log de 01:19 mostrou a biblioteca parada esperando lectern; um
        // baú do jogador com o material dentro da vila era invisível para a
        // colônia, porque esta lista só tinha os baús reivindicados por
        // trabalhador e o da boca da mina.
        //
        // Baú nomeado fica de fora — ver VillageChestRule. Entram por
        // último de propósito: a ordenação abaixo é por distância, então a
        // posição na lista não os privilegia nem os prejudica, e o baú do
        // próprio trabalhador continua sendo o primeiro quando é o mais
        // perto.
        VillageColonyMod.COLONIES.find(colonyId)
                .ifPresent(colony ->
                        chests.addAll(VillageChests.around(world, colony.center(), chests)));

        chests.sort(Comparator
                .comparingLong((ColonyPos chest) -> squaredDistance(chest, from))
                .thenComparingInt(ColonyPos::x)
                .thenComparingInt(ColonyPos::y)
                .thenComparingInt(ColonyPos::z));

        return chests;
    }

    /**
     * Quanta pedra ainda cabe nos baús dos mineiros desta colônia — 2026-09-25.
     *
     * <p>É onde o mineiro descarrega (o baú da boca da mina fica com o
     * minério primeiro, e o que sobra vai para o dele). Baú em chunk
     * descarregado conta zero: sem ler, não se promete espaço.
     */
    public static int minersRoom(ServerWorld world, UUID colonyId) {
        return roomOf(world, colonyId, ProfessionType.MINER, ResourceGroup.STONE);
    }

    /**
     * Quanto deste grupo ainda cabe nos baús de uma profissão — 2026-09-26.
     * Ver {@code StandingWork}: o pastor e o fazendeiro trabalham enquanto o
     * baú deles tiver espaço, como o mineiro e o lenhador.
     */
    public static int roomOf(
            ServerWorld world, UUID colonyId, ProfessionType profession, ResourceGroup group) {

        List<ColonyPos> chests = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession().filter(profession::equals).isEmpty()) {
                continue;
            }

            VillageColonyMod.STORAGES.of(worker.villagerId())
                    .ifPresent(storage -> chests.add(storage.chestPosition()));
        }

        if (chests.isEmpty()) {
            return 0;
        }

        return ChestInventoryReader.survey(world, chests, group).freeSpaceForGroup(group);
    }

    /** Quanto deste item a colônia tem, somando todos os baús. */
    public static int countIn(ServerWorld world, List<ColonyPos> chests, Item item) {
        int found = 0;

        for (ColonyPos chest : chests) {
            found += ChestWithdrawer.countIn(world, chest, item);
        }

        return found;
    }

    /**
     * Tira este item dos baús, somando entre eles até juntar a
     * quantidade.
     *
     * <p><b>Confira o total antes de chamar.</b> Este método tira o que
     * encontra e devolve quanto tirou; se a colônia não tiver o
     * bastante, o que saiu já saiu. Quem precisa da quantidade inteira
     * ou de nada pergunta a {@link #countIn} primeiro — é o que a
     * fabricação faz, porque tirar material para uma feitura que não
     * acontece é destruir o que é do jogador.
     *
     * @return quanto foi tirado, entre zero e {@code amount}
     */
    public static int withdraw(
            ServerWorld world, List<ColonyPos> chests, Item item, int amount) {

        int taken = 0;

        for (ColonyPos chest : chests) {
            if (taken >= amount) {
                break;
            }

            taken += ChestWithdrawer.withdraw(world, chest, item, amount - taken);
        }

        return taken;
    }

    /**
     * O primeiro baú, a partir do mais próximo, em que este item cabe.
     *
     * <p>Vazio quando não cabe em nenhum. Fabricar antes de saber onde
     * guardar é a mesma armadilha do {@code convertOne}: o ingrediente
     * já foi gasto, e o resultado não tem para onde ir.
     */
    public static Optional<ColonyPos> firstWithRoomFor(
            ServerWorld world, List<ColonyPos> chests, Item item, int amount) {

        for (ColonyPos chest : chests) {
            if (ChestDepositor.freeSpaceFor(world, chest, item) >= amount) {
                return Optional.of(chest);
            }
        }

        return Optional.empty();
    }

    /**
     * Guarda ao longo dos baús, e devolve o que não coube em nenhum.
     *
     * <p>O espelho de {@link #withdraw}, e pelo mesmo motivo. A retirada
     * passou a percorrer a colônia inteira em 2026-08-14, quando a
     * sessão mostrou o fabricante encerrando por "sem tronco" com 134
     * troncos guardados a dois baús de distância. O depósito ficou para
     * trás, e a sessão de 2026-09-04 cobrou o outro lado: o baú do
     * lenhador assoreou de vara — que nenhum grupo de recurso cobre e
     * nada no mod retira —, o espaço chegou a zero, e a partir daí cada
     * tronco derrubado era um tronco destruído.
     *
     * <p><b>Sobra é sobra, e não perda.</b> Quem chamou decide: o
     * lenhador registra e para, porque a essa altura o jogador precisa
     * esvaziar alguma coisa e precisa poder descobrir isso.
     *
     * @return quantos itens não couberam em baú nenhum, entre zero e
     *     {@code amount}
     */
    public static int deposit(
            ServerWorld world, List<ColonyPos> chests, Item item, int amount) {

        int remaining = amount;

        for (ColonyPos chest : chests) {
            if (remaining <= 0) {
                break;
            }

            remaining = ChestDepositor.deposit(world, chest, item, remaining);
        }

        return remaining;
    }

    /**
     * Quanto ainda cabe de um grupo, somando os baús da lista.
     *
     * <p>Existe para a pergunta ser feita onde a resposta será usada. A
     * guarda que decide se vale derrubar a árvore e o depósito que
     * guarda o tronco precisam medir o mesmo lugar — quando mediam
     * lugares diferentes, a guarda dizia "não cabe" sobre um baú e o
     * depósito destruía a colheita no outro. Foi o defeito de
     * 2026-09-04.
     */
    public static int freeSpaceForGroup(
            ServerWorld world, List<ColonyPos> chests, ResourceGroup group) {

        int room = 0;

        for (ColonyPos chest : chests) {
            room += ChestDepositor.freeSpaceForGroup(world, chest, group);
        }

        return room;
    }

    /**
     * Os baús da colônia com o do próprio trabalhador na frente.
     *
     * <p>A Regra 10 ordena por distância, e para quem procura material
     * isso é o certo. Para quem guarda não é: o baú do trabalhador é
     * dele, e a colheita ir para lá é o que faz o relatório de um
     * lenhador falar do lenhador. O resto da colônia é o transbordo, e
     * só isso.
     *
     * <p>Baú próprio que não está registrado na colônia entra assim
     * mesmo — o trabalhador pode ter sido despachado antes de o registro
     * alcançá-lo, e perder a colheita por causa disso seria trocar um
     * defeito por outro.
     */
    public static List<ColonyPos> ownFirst(
            ServerWorld world, UUID colonyId, ColonyPos own) {

        List<ColonyPos> chests = new ArrayList<>();

        chests.add(own);

        for (ColonyPos chest : nearestFirst(world, colonyId, own)) {
            if (!chest.equals(own)) {
                chests.add(chest);
            }
        }

        return chests;
    }

    /**
     * O baú da boca da mina, que não é registro de trabalhador nenhum.
     *
     * <p><b>A ruptura que o P0.3 achou em 2026-09-11.</b> A Regra 30
     * manda o minério que não é carvão para o baú da boca da mina, e esse
     * baú é achado por geometria — {@link MineMouth#chestAt} procura um
     * baú encostado na entrada do poço. O único lugar do mod que cria
     * {@code WorkerStorage} é o {@code ChestScanner.scan}, que procura
     * baú ao redor da <b>cama</b> do aldeão, e mina não tem cama ao lado.
     *
     * <p>Então o minério entrava num baú que a contabilidade da colônia
     * não lia: o fundidor dizia {@code nothing in the colony chests to
     * smelt} <b>34 vezes</b> na sessão de 09-04, com o mineiro cavando, e
     * estava certo ao pé da letra.
     *
     * <p><b>Consertou-se o lado de quem lê, e não o de quem escreve.</b> A
     * Regra 30 é decisão do autor de 2026-08-22 com motivo escrito — o
     * mineiro não carrega minério montanha acima —, e revogá-la para
     * fazer a conta fechar trocaria um defeito de contabilidade por um de
     * desenho.
     *
     * <p><b>A Regra 30 foi revogada em 2026-09-15</b>, e esta leitura
     * <b>fica</b>. O autor mandou parar de <i>depositar</i> na boca — ver
     * {@code MinerHaul.treasureChestFor} —, e nada foi removido do mundo:
     * o baú que a colônia já pôs ali continua de pé, com todo o minério
     * que a Regra 30 mandou para lá enquanto vigorou. Parar de lê-lo
     * apagaria esse estoque da contabilidade e devolveria exatamente o
     * defeito que esta função nasceu para corrigir — o fundidor dizendo
     * {@code nothing in the colony chests to smelt} com o ferro a dez
     * blocos dali.
     *
     * <p>Ela deixa de receber depósito novo e vira fonte que só drena, que
     * é o fim certo para um baú aposentado. Quando o jogador o quebrar,
     * {@link MineMouth#chestAt} deixa de achá-lo e esta chamada volta a
     * ser silenciosa — sem nada a fazer no código.
     *
     * <p><b>E entra aqui, num lugar só</b>, de propósito. Contar num
     * conjunto e consumir de outro é a discordância que o javadoc do
     * {@code ResourceSubstitution} guarda de 2026-09-10: <i>"a colônia
     * concluía que a meta estava cumprida e o mineiro não ia cavar,
     * enquanto o construtor esperava pelo arenito"</i>. Meia correção
     * aqui seria pior que nenhuma.
     *
     * <p>Perguntado ao mundo a cada chamada, e não guardado: o jogador
     * quebra o baú quando quer, e uma posição guardada envelheceria
     * calada. São oito leituras de bloco, com o chunk conferido antes.
     */
    private static void addMineMouth(
            ServerWorld world, UUID colonyId, List<ColonyPos> chests) {

        VillageColonyMod.MINES.of(colonyId)
                .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()))
                .flatMap(mouth -> MineMouth.chestAt(world, mouth))
                .map(MinecraftTypeAdapter::toColonyPos)
                .filter(chest -> !chests.contains(chest))
                .ifPresent(chests::add);
    }

    private static long squaredDistance(ColonyPos chest, ColonyPos from) {
        long dx = (long) chest.x() - from.x();
        long dy = (long) chest.y() - from.y();
        long dz = (long) chest.z() - from.z();

        return dx * dx + dy * dy + dz * dz;
    }
}
