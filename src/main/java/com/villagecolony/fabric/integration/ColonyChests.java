package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.worker.model.Worker;
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
 * {@code BuilderWork.takeMaterial} e {@code ManufacturerWork.convertOne}
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
    public static List<ColonyPos> nearestFirst(UUID colonyId, ColonyPos from) {
        List<ColonyPos> chests = new ArrayList<>();

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (storage.isPresent() && !chests.contains(storage.get().chestPosition())) {
                chests.add(storage.get().chestPosition());
            }
        }

        chests.sort(Comparator
                .comparingLong((ColonyPos chest) -> squaredDistance(chest, from))
                .thenComparingInt(ColonyPos::x)
                .thenComparingInt(ColonyPos::y)
                .thenComparingInt(ColonyPos::z));

        return chests;
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
    public static List<ColonyPos> ownFirst(UUID colonyId, ColonyPos own) {
        List<ColonyPos> chests = new ArrayList<>();

        chests.add(own);

        for (ColonyPos chest : nearestFirst(colonyId, own)) {
            if (!chest.equals(own)) {
                chests.add(chest);
            }
        }

        return chests;
    }

    private static long squaredDistance(ColonyPos chest, ColonyPos from) {
        long dx = (long) chest.x() - from.x();
        long dy = (long) chest.y() - from.y();
        long dz = (long) chest.z() - from.z();

        return dx * dx + dy * dy + dz * dz;
    }
}
