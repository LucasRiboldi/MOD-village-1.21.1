package com.villagecolony.core.worker.model;

import com.villagecolony.core.type.Capability;

import java.util.EnumMap;
import java.util.Map;
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

    /**
     * Por quantas passagens da distribuição uma capacidade descansa.
     *
     * <p>A distribuição roda uma vez por ciclo da colônia, então contar
     * passagens é contar ciclos — e a conta não precisa de
     * {@code world.getTime()}, que o Core não conhece (ADR-005).
     *
     * <p>Quatro ciclos são dois minutos, que é a mesma ordem de grandeza
     * do guarda de travamento que põe a capacidade para descansar. Curto
     * de propósito: o descanso existe para desempatar a escolha da
     * passagem seguinte, e não para aposentar a profissão de ninguém.
     */
    public static final int REST_CYCLES = 4;

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

    /**
     * As capacidades que travaram para ele, e quantas passagens faltam.
     *
     * <p><b>Mora no trabalhador, e não num mapa estático</b>: é estado
     * dele, morre com ele, e não sobra atrás quando a colônia some. É a
     * diferença entre isto e o {@code TreeMarks}, que é da vila.
     *
     * <p><b>Não vai para o disco</b>, pelo mesmo argumento do
     * {@code blocked} da {@code Mine}: é a contagem de uma sessão, e não
     * um fato sobre o trabalhador. Reabrir o mundo já recusando o próprio
     * trabalho seria pior que a tentativa a mais que isso custa.
     */
    private final Map<Capability, Integer> resting = new EnumMap<>(Capability.class);

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

    /**
     * Esta capacidade acabou de travar para ele — ADR-010, 2026-09-02.
     *
     * <p><b>Travado não é ocioso.</b> A sessão de 2026-09-02 deixou dois
     * trabalhadores parados por dezesseis e por dois minutos, e nenhum
     * deles estava ocioso pela definição do {@code WorkAssignment}: os
     * dois tinham tarefa aberta. Quem sabe a diferença é o guarda de
     * travamento, e é ele quem chama isto.
     *
     * <p>Não é a árvore nem a pedra que descansa — disso já cuidam o
     * {@code TreeMarks} e o cursor da mina. É <b>este trabalhador
     * tentando este tipo de trabalho</b>.
     *
     * <p>Travar de novo renova o prazo inteiro: a segunda parede é prova
     * de que a primeira não foi azar.
     */
    public void rest(Capability capability) {
        resting.put(Objects.requireNonNull(capability, "capability"), REST_CYCLES);
    }

    /** Se esta capacidade ainda está de molho para ele. */
    public boolean isResting(Capability capability) {
        return resting.containsKey(Objects.requireNonNull(capability, "capability"));
    }

    /**
     * Passou uma distribuição, e os descansos andam com ela.
     *
     * <p>Chamado pela distribuição, e só para quem ela considera: quem
     * está com tarefa aberta não gasta descanso, porque não é dele que a
     * colônia precisa decidir agora.
     */
    public void aCycleWentBy() {
        resting.replaceAll((capability, left) -> left - 1);

        resting.values().removeIf(left -> left <= 0);
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
