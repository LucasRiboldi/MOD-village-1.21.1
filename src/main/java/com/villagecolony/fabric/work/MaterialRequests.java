package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.MaterialRequest;
import com.villagecolony.core.construction.model.MaterialRequest.Source;
import com.villagecolony.core.construction.model.MaterialRequest.State;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O pedido de material atual de cada obra — ADR-035 §3, fase 1.
 *
 * <p>Uma entrada por obra: a próxima peça que ela pede e o que a colônia está
 * fazendo por ela. Quem grava é {@link BuilderMaterials#hasMaterialForNextBlock};
 * quem lê é o {@code /vc log}. Só observa: nenhuma decisão de suprimento passa
 * por aqui.
 */
public final class MaterialRequests {

    /** Teto: obra encerrada sai sozinha quando o registro passa disto. */
    static final int LIMIT = 256;

    private static final Map<UUID, MaterialRequest> BY_PROJECT = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, MaterialRequest> eldest) {
            return size() > LIMIT;
        }
    };

    static {
        ServerMemory.register(MaterialRequests.class, BY_PROJECT::clear);
    }

    private MaterialRequests() {
    }

    /** Registra a situação da peça; a mesma situação mantém o tique de início. */
    static MaterialRequest record(UUID projectId, ResourceId material, State state, Source source, long tick) {
        return BY_PROJECT.merge(projectId, MaterialRequest.start(material, state, source, tick),
                (old, fresh) -> old.updatedTo(material, state, source, tick));
    }

    /** A obra não espera peça nenhuma: acabou, ou a próxima se forma no local. */
    static void clear(UUID projectId) {
        BY_PROJECT.remove(projectId);
    }

    public static Optional<MaterialRequest> of(UUID projectId) {
        return Optional.ofNullable(BY_PROJECT.get(projectId));
    }
}
