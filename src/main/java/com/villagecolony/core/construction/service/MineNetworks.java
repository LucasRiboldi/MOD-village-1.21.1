package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.MineNetwork;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O nível em rede de cada mina — ADR-042. Um por colônia: o do salão de agora. Quando a
 * mina desce, o salão muda e o nível novo nasce aqui; os de cima ficam no mundo,
 * ligados pela escadaria.
 */
public final class MineNetworks {

    static {
        ServerMemory.register(MineNetworks.class, MineNetworks::clearAll);
    }

    private static final Map<UUID, MineNetwork> BY_COLONY = new HashMap<>();

    private MineNetworks() {
    }

    /** O nível deste salão: o guardado, se for o mesmo salão; senão um nível novo. */
    public static MineNetwork forLevel(UUID colonyId, ColonyPos hub) {
        MineNetwork known = BY_COLONY.get(colonyId);

        if (known != null && known.hub().equals(hub)) {
            return known;
        }

        MineNetwork fresh = MineNetwork.open(hub);
        BY_COLONY.put(colonyId, fresh);

        return fresh;
    }

    /** O nível de agora, se a mina já chegou à fase das galerias. */
    public static Optional<MineNetwork> of(UUID colonyId) {
        return Optional.ofNullable(BY_COLONY.get(colonyId));
    }

    /** O nível que voltou do save. */
    public static void restore(UUID colonyId, MineNetwork network) {
        BY_COLONY.put(colonyId, network);
    }

    public static void forget(UUID colonyId) {
        BY_COLONY.remove(colonyId);
    }

    public static void clearAll() {
        BY_COLONY.clear();
    }
}
