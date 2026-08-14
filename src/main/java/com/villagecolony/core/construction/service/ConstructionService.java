package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.ConstructionProject;

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
 * As obras que existem — TASK-033.
 *
 * <p>Registro em memória, como {@code ColonyService} e {@code TaskService}.
 * Não decide construir, não escolhe lugar e não coloca bloco: guarda o
 * que foi decidido e responde quem pergunta. ADR-006 §5.
 *
 * <p><b>Uma obra por colônia de cada vez.</b> É a decisão que mantém a
 * Fase 10 honesta com a Regra 5: a meta de tábua é "o que a obra pede", e
 * duas obras abertas dariam duas respostas para a mesma pergunta. Também
 * é o que impede a colônia de abrir dez canteiros e não terminar nenhum —
 * o mesmo defeito da fila que não esvaziava, em outra roupa (§17, E1).
 *
 * <p><b>Thread safety:</b> nenhuma, como todo o resto do Core. Só a
 * thread do servidor toca aqui.
 */
public final class ConstructionService {

    /** Ordem de inserção, para log e iteração reproduzíveis. */
    private final Map<UUID, ConstructionProject> projects = new LinkedHashMap<>();

    /**
     * Registra uma obra nova.
     *
     * @throws IllegalStateException se a colônia já tem obra aberta. É
     *     erro de programação, não condição do mundo: quem planeja deve
     *     perguntar antes com {@link #openOf}
     */
    public void register(ConstructionProject project) {
        Objects.requireNonNull(project, "project");

        if (openOf(project.colonyId()).isPresent()) {
            throw new IllegalStateException(
                    "Colony already has an open project: " + project.colonyId());
        }

        projects.put(project.id(), project);
    }

    /** A obra em andamento desta colônia, se houver. */
    public Optional<ConstructionProject> openOf(UUID colonyId) {
        if (colonyId == null) {
            return Optional.empty();
        }

        for (ConstructionProject project : projects.values()) {
            if (project.colonyId().equals(colonyId) && project.state().isOpen()) {
                return Optional.of(project);
            }
        }

        return Optional.empty();
    }

    public Optional<ConstructionProject> find(UUID projectId) {
        if (projectId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(projects.get(projectId));
    }

    /** Todas as obras desta colônia, terminadas inclusive. */
    public List<ConstructionProject> ofColony(UUID colonyId) {
        List<ConstructionProject> found = new ArrayList<>();

        if (colonyId == null) {
            return found;
        }

        for (ConstructionProject project : projects.values()) {
            if (project.colonyId().equals(colonyId)) {
                found.add(project);
            }
        }

        return found;
    }

    /** Todas as obras, em ordem de registro. Somente leitura. */
    public Collection<ConstructionProject> all() {
        return Collections.unmodifiableCollection(projects.values());
    }

    /**
     * Tira do registro as obras terminadas.
     *
     * <p>Chamado ao fim do ciclo, como {@code TaskService.purgeClosed} —
     * e existe pelo mesmo motivo: obra é objeto em memória, e sem alguém
     * que a remova o registro só cresce. A casa continua de pé; o que sai
     * é o papel do canteiro.
     *
     * <p>Quem guarda que a casa existe é o registro de construções da
     * Fase 11, não este.
     *
     * @return quantas saíram
     */
    public int purgeFinished() {
        int before = projects.size();

        projects.values().removeIf(project -> !project.state().isOpen());

        return before - projects.size();
    }

    public int count() {
        return projects.size();
    }

    /** Esvazia o registro. Usado ao descarregar o mundo. */
    public void clear() {
        projects.clear();
    }
}
