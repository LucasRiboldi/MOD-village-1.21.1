package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Grava e recarrega as colônias junto com o mundo.
 *
 * <p>Contém apenas serialização. Nenhuma regra de domínio mora aqui —
 * ver ADR-006 §5. Quem decide o que fazer com as colônias é
 * {@link ColonyService}.
 *
 * <p>Os dados ficam presos ao Overworld porque a colônia pertence ao
 * mundo, não ao jogador. Ver Save-Data-System.md.
 *
 * <p>Colônias e trabalhadores moram no mesmo arquivo de propósito
 * (TASK-012b): o trabalhador aponta para a colônia por id, e dois
 * arquivos separados poderiam ser gravados em momentos diferentes,
 * deixando trabalhador órfão apontando para colônia inexistente.
 */
public final class ColonySavedData extends PersistentState {

    /** Nome do arquivo em {@code data/}. Mudar isto invalida saves. */
    public static final String KEY = "villagecolony_colonies";

    private static final String COLONIES = "colonies";
    private static final String ID = "id";
    private static final String CENTER_X = "centerX";
    private static final String CENTER_Y = "centerY";
    private static final String CENTER_Z = "centerZ";
    private static final String STATE = "state";
    private static final String OBSERVED_BEDS = "observedBeds";

    private static final String WORKERS = "workers";
    private static final String VILLAGER_ID = "villagerId";
    private static final String COLONY_ID = "colonyId";
    private static final String PROFESSION = "profession";

    public static final PersistentState.Type<ColonySavedData> TYPE = new PersistentState.Type<>(
            ColonySavedData::new,
            ColonySavedData::readNbt,
            null);

    private final List<Colony> colonies = new ArrayList<>();

    private final List<Worker> workers = new ArrayList<>();

    private ColonySavedData() {
    }

    /**
     * Carrega os dados do mundo, criando-os vazios na primeira vez.
     *
     * <p>Este é o único ponto do mod que fala com o
     * {@code PersistentStateManager}.
     */
    public static ColonySavedData get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE, KEY);
    }

    /** Colônias lidas do disco. Vazio antes do primeiro save. */
    public List<Colony> colonies() {
        return List.copyOf(colonies);
    }

    /**
     * Trabalhadores lidos do disco, já sem os órfãos.
     *
     * <p>Vazio antes do primeiro save, e também em saves anteriores à
     * TASK-012b — nesse caso a varredura os reencontra, só sem profissão.
     */
    public List<Worker> workers() {
        return List.copyOf(workers);
    }

    /**
     * Copia os registros em memória para cá e marca para gravação.
     *
     * <p>O Minecraft grava quando decide gravar; nosso papel é apenas
     * garantir que o conteúdo esteja correto e sinalizado como sujo.
     *
     * <p>Os dois registros são copiados na mesma chamada porque são
     * gravados no mesmo arquivo: sincronizar um sem o outro produziria
     * exatamente o órfão que juntá-los evita.
     */
    public void sync(Collection<Colony> currentColonies, Collection<Worker> currentWorkers) {
        colonies.clear();
        colonies.addAll(currentColonies);

        workers.clear();
        workers.addAll(currentWorkers);

        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();

        for (Colony colony : colonies) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(ID, colony.id());
            entry.putInt(CENTER_X, colony.center().x());
            entry.putInt(CENTER_Y, colony.center().y());
            entry.putInt(CENTER_Z, colony.center().z());
            entry.putString(STATE, colony.state().name());
            entry.putInt(OBSERVED_BEDS, colony.observedBeds());

            list.add(entry);
        }

        nbt.put(COLONIES, list);

        NbtList workerList = new NbtList();

        for (Worker worker : workers) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(VILLAGER_ID, worker.villagerId());
            entry.putUuid(COLONY_ID, worker.colonyId());

            // Ausente, e não vazio, para quem ainda não tem função: a
            // leitura distingue "sem chave" de "chave desconhecida".
            worker.profession().ifPresent(p -> entry.putString(PROFESSION, p.name()));

            workerList.add(entry);
        }

        nbt.put(WORKERS, workerList);

        return nbt;
    }

    private static ColonySavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ColonySavedData data = new ColonySavedData();

        NbtList list = nbt.getList(COLONIES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(ID)) {
                continue;
            }

            UUID id = entry.getUuid(ID);

            ColonyPos center = new ColonyPos(
                    entry.getInt(CENTER_X),
                    entry.getInt(CENTER_Y),
                    entry.getInt(CENTER_Z));

            Colony colony = Colony.restore(id, center, readState(entry), ColonyLifecycle.DORMANT);

            // Save antigo não tem o campo; getInt devolve 0, que apenas
            // faz a primeira detecção da sessão valer. Autocorrige.
            colony.observe(center, entry.getInt(OBSERVED_BEDS));

            data.colonies.add(colony);
        }

        readWorkers(nbt, data);

        return data;
    }

    /**
     * Lê os trabalhadores, descartando os que apontam para colônia que
     * não veio no mesmo arquivo.
     *
     * <p>Um órfão não deveria existir — colônias e trabalhadores são
     * gravados juntos. Se existir, o save foi editado ou corrompido, e
     * um trabalhador de colônia inexistente seria invisível para sempre:
     * nenhuma colônia o listaria, e a varredura não o recriaria, porque
     * o villagerId já teria dono. Descartar deixa a varredura reencontrá-lo
     * e reatribuí-lo à colônia certa, ao custo da profissão que ele tinha.
     *
     * <p>Chamado depois das colônias, e depende disso.
     */
    private static void readWorkers(NbtCompound nbt, ColonySavedData data) {
        Set<UUID> knownColonies = new HashSet<>();

        for (Colony colony : data.colonies) {
            knownColonies.add(colony.id());
        }

        NbtList list = nbt.getList(WORKERS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(VILLAGER_ID) || !entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            data.workers.add(Worker.restore(
                    entry.getUuid(VILLAGER_ID),
                    colonyId,
                    readProfession(entry)));
        }
    }

    /**
     * Profissão ausente ou desconhecida vira "sem função".
     *
     * <p>Mesmo princípio de {@link #readState}: não derrubar o
     * carregamento do mundo. Aqui o custo é menor — a atribuição inicial
     * dá uma função nova ao aldeão no próximo ciclo (TASK-014).
     */
    private static ProfessionType readProfession(NbtCompound entry) {
        if (!entry.contains(PROFESSION, NbtElement.STRING_TYPE)) {
            return null;
        }

        String name = entry.getString(PROFESSION);

        for (ProfessionType profession : ProfessionType.values()) {
            if (profession.name().equals(name)) {
                return profession;
            }
        }

        return null;
    }

    /**
     * Um estado desconhecido no save não pode derrubar o carregamento do
     * mundo. Cair para STABLE perde a intenção da colônia, mas ela
     * reavalia o que fazer no próximo ciclo.
     */
    private static ColonyState readState(NbtCompound entry) {
        String name = entry.getString(STATE);

        for (ColonyState state : ColonyState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }

        return ColonyState.STABLE;
    }
}
