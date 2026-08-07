package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public static final PersistentState.Type<ColonySavedData> TYPE = new PersistentState.Type<>(
            ColonySavedData::new,
            ColonySavedData::readNbt,
            null);

    private final List<Colony> colonies = new ArrayList<>();

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
     * Copia o registro em memória para cá e marca para gravação.
     *
     * <p>O Minecraft grava quando decide gravar; nosso papel é apenas
     * garantir que o conteúdo esteja correto e sinalizado como sujo.
     */
    public void sync(Collection<Colony> current) {
        colonies.clear();
        colonies.addAll(current);

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

        return data;
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
