package com.villagecolony.core.storage.service;

import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Quais trabalhadores têm baú, e onde.
 *
 * <p>Registro em memória, no mesmo molde de {@code WorkerService}: quem
 * lê o mundo é {@code fabric.integration.ChestScanner}. Ver ADR-006 §5.
 *
 * <p>Os trabalhadores são referenciados por id: este domínio não importa
 * o domínio worker. Ver ADR-006 §6.
 *
 * <p><b>Thread safety:</b> nenhuma. Acessado apenas pela thread do
 * servidor, que é única.
 */
public final class StorageRegistry {

    /** Ordem de inserção preservada para logs reproduzíveis. */
    private final Map<UUID, WorkerStorage> storages = new LinkedHashMap<>();

    /**
     * Anota o baú de um trabalhador, substituindo o anterior.
     *
     * <p>Substituir é o comportamento certo aqui, ao contrário do que
     * vale para colônia e trabalhador: o baú registrado pode ter sido
     * quebrado, e reencontrar o dono com outro baú é o caminho normal de
     * recuperação previsto em Storage-System.md §"Falhas". Recusar
     * deixaria o trabalhador preso a um baú que não existe mais.
     *
     * @return o registro anterior, se havia um
     */
    public Optional<WorkerStorage> register(WorkerStorage storage) {
        Objects.requireNonNull(storage, "storage");

        return Optional.ofNullable(storages.put(storage.workerId(), storage));
    }

    public Optional<WorkerStorage> of(UUID workerId) {
        if (workerId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(storages.get(workerId));
    }

    public boolean hasStorage(UUID workerId) {
        return workerId != null && storages.containsKey(workerId);
    }

    /**
     * Se algum trabalhador já reivindicou esta posição.
     *
     * <p>Storage-System.md §"Proteção": o baú registrado pertence ao seu
     * trabalhador. Sem esta pergunta, dois aldeões da mesma casa
     * partilhariam um baú e cada um contaria o estoque do outro como seu.
     */
    public boolean isTaken(ColonyPos position) {
        if (position == null) {
            return false;
        }

        for (WorkerStorage storage : storages.values()) {
            if (storage.isAt(position)) {
                return true;
            }
        }

        return false;
    }

    /** Todos os armazenamentos. Somente leitura. */
    public Collection<WorkerStorage> all() {
        return Collections.unmodifiableCollection(storages.values());
    }

    /**
     * Esquece o baú de um trabalhador.
     *
     * <p>Para quando houver como provar que o baú sumiu — o
     * "Storage Missing" de Storage-System.md §"Falhas".
     */
    public boolean remove(UUID workerId) {
        return workerId != null && storages.remove(workerId) != null;
    }

    public int count() {
        return storages.size();
    }

    /** Esvazia o registro. Usado ao descarregar o mundo. */
    public void clear() {
        storages.clear();
    }
}
