package com.villagecolony.core.worker.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Um aldeão que pertence a uma colônia.
 *
 * <p>Modelo de dados: guarda estado e valida o que recebe. Não decide
 * profissão, não executa tarefas e não move o aldeão. Ver Data-Model.md
 * e CODE-STANDARDS.md §5.
 *
 * <p>O trabalhador não é uma entidade nova: {@link #villagerId()} aponta
 * para o {@code VillagerEntity} Vanilla, que continua existindo com sua
 * profissão e sua rotina. Ver PROJECT_CONSTITUTION.md §4.
 *
 * <p>A colônia é referenciada por id, e não por objeto, porque um domínio
 * do Core não importa outro. Ver ADR-006 §6.
 *
 * <p>Os campos {@code storageId}, {@code state} e {@code currentTask}
 * previstos em Data-Model.md ainda não existem: dependem dos sistemas de
 * armazenamento e de tarefas.
 */
public final class Worker {

    private final UUID villagerId;

    private final UUID colonyId;

    /**
     * Profissão de colônia, ou {@code null} enquanto não houver.
     *
     * <p>Registrar um aldeão e atribuir-lhe função são momentos
     * diferentes: a detecção registra todos os aldeões da vila, e só
     * depois a colônia decide quem faz o quê. Ver TASK-012 e TASK-013.
     */
    private ProfessionType profession;

    private Worker(UUID villagerId, UUID colonyId, ProfessionType profession) {
        this.villagerId = villagerId;
        this.colonyId = colonyId;
        this.profession = profession;
    }

    /**
     * Registra um aldeão recém-encontrado numa colônia.
     *
     * <p>Nasce sem profissão de colônia.
     */
    public static Worker register(UUID villagerId, UUID colonyId) {
        return new Worker(
                Objects.requireNonNull(villagerId, "villagerId"),
                Objects.requireNonNull(colonyId, "colonyId"),
                null);
    }

    /**
     * Reconstrói um trabalhador a partir de dados salvos.
     *
     * @param profession pode ser {@code null}, para quem ainda não tinha
     *     função quando o mundo foi fechado
     */
    public static Worker restore(UUID villagerId, UUID colonyId, ProfessionType profession) {
        return new Worker(
                Objects.requireNonNull(villagerId, "villagerId"),
                Objects.requireNonNull(colonyId, "colonyId"),
                profession);
    }

    /** Id do {@code VillagerEntity} Vanilla. É a identidade do trabalhador. */
    public UUID villagerId() {
        return villagerId;
    }

    public UUID colonyId() {
        return colonyId;
    }

    /** Vazio enquanto a colônia não tiver dado função a este aldeão. */
    public Optional<ProfessionType> profession() {
        return Optional.ofNullable(profession);
    }

    public boolean hasProfession() {
        return profession != null;
    }

    /**
     * Dá uma função ao trabalhador.
     *
     * <p>Substitui a anterior sem cerimônia: a colônia realoca conforme a
     * necessidade muda, e isso não é erro.
     */
    public void assign(ProfessionType profession) {
        this.profession = Objects.requireNonNull(profession, "profession");
    }

    /** Devolve o trabalhador ao estado sem função. */
    public void unassign() {
        this.profession = null;
    }

    public boolean belongsTo(UUID colonyId) {
        return this.colonyId.equals(colonyId);
    }

    /**
     * Dois trabalhadores são o mesmo quando apontam para o mesmo aldeão.
     *
     * <p>A profissão muda ao longo da vida; o aldeão não.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Worker worker && villagerId.equals(worker.villagerId);
    }

    @Override
    public int hashCode() {
        return villagerId.hashCode();
    }

    @Override
    public String toString() {
        return "Worker[villager=" + villagerId
                + ", colony=" + colonyId
                + ", profession=" + (profession == null ? "none" : profession)
                + "]";
    }
}
