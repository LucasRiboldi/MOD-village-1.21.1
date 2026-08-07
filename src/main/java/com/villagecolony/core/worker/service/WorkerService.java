package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.Worker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro dos trabalhadores em memória.
 *
 * <p>Guarda quais aldeões pertencem a quais colônias. Quem lê o mundo é
 * {@code fabric.integration.VillagerScanner}; quem decide profissão será
 * o sistema de profissões. Ver ADR-006 §5.
 *
 * <p>As colônias são referenciadas por id: este domínio não importa o
 * domínio colony. Ver ADR-006 §6.
 *
 * <p><b>Thread safety:</b> nenhuma. Acessado apenas pela thread do
 * servidor, que é única.
 */
public final class WorkerService {

    /** Ordem de inserção preservada para logs e iterações reproduzíveis. */
    private final Map<UUID, Worker> workers = new LinkedHashMap<>();

    /**
     * Registra um aldeão, ou devolve o registro que já existia.
     *
     * <p>Idempotente de propósito: a detecção roda a cada ciclo e vê os
     * mesmos aldeões de novo. Reencontrar alguém não pode apagar a
     * profissão que ele já recebeu.
     *
     * @return o trabalhador, novo ou preexistente
     */
    public Worker register(UUID villagerId, UUID colonyId) {
        Objects.requireNonNull(villagerId, "villagerId");
        Objects.requireNonNull(colonyId, "colonyId");

        Worker existing = workers.get(villagerId);

        if (existing != null) {
            return existing;
        }

        Worker worker = Worker.register(villagerId, colonyId);
        workers.put(villagerId, worker);

        return worker;
    }

    public Optional<Worker> find(UUID villagerId) {
        if (villagerId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(workers.get(villagerId));
    }

    public boolean isRegistered(UUID villagerId) {
        return villagerId != null && workers.containsKey(villagerId);
    }

    /** Trabalhadores de uma colônia, em ordem de registro. */
    public List<Worker> ofColony(UUID colonyId) {
        Objects.requireNonNull(colonyId, "colonyId");

        List<Worker> result = new ArrayList<>();

        for (Worker worker : workers.values()) {
            if (worker.belongsTo(colonyId)) {
                result.add(worker);
            }
        }

        return List.copyOf(result);
    }

    public int countOfColony(UUID colonyId) {
        return ofColony(colonyId).size();
    }

    /** Todos os trabalhadores. Somente leitura. */
    public Collection<Worker> all() {
        return Collections.unmodifiableCollection(workers.values());
    }

    /**
     * Remove um trabalhador do registro.
     *
     * <p>Existe para quando houver como provar que o aldeão morreu. Não é
     * chamado por ausência na varredura: um aldeão fora do raio não está
     * morto, apenas não foi visto.
     */
    public boolean remove(UUID villagerId) {
        return villagerId != null && workers.remove(villagerId) != null;
    }

    public int count() {
        return workers.size();
    }

    /** Esvazia o registro. Usado ao descarregar o mundo. */
    public void clear() {
        workers.clear();
    }
}
