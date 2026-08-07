package com.villagecolony.core.colony.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.UUID;

/**
 * Uma vila organizada.
 *
 * <p>Modelo de dados: guarda estado e valida o que recebe. Não toma
 * decisões, não executa tarefas e não altera o mundo — isso pertence aos
 * services. Ver Data-Model.md e CODE-STANDARDS.md §5.
 *
 * <p>A colônia tem dois estados independentes:
 *
 * <ul>
 *   <li>{@link ColonyState} — o que ela está fazendo;
 *   <li>{@link ColonyLifecycle} — se está sendo simulada.
 * </ul>
 *
 * <p>Os campos {@code biomeType}, {@code workers}, {@code buildings} e
 * {@code tasks} previstos em Data-Model.md ainda não existem: dependem de
 * modelos criados em tarefas posteriores.
 */
public final class Colony {

    private final UUID id;

    /**
     * Move conforme camas são adicionadas ou removidas.
     *
     * <p>Não é final por exigência da ADR-003 §4: o centro acompanha a
     * vila, mas o {@link #id} nunca muda. Workers e Buildings ficam
     * ligados ao UUID, não à posição.
     */
    private ColonyPos center;

    private ColonyState state;

    private ColonyLifecycle lifecycle;

    /**
     * Quantas camas a melhor observação já vista continha.
     *
     * <p>Mede o quão completa foi a detecção que definiu o
     * {@link #center} atual — não o tamanho real da vila.
     *
     * <p>Existe porque nenhuma detecção enxerga a vila inteira: o raio de
     * busca é de 64 blocos e uma vila é maior que isso. Sem este número,
     * uma detecção de borda que vê 3 de 12 camas sobrescreve o centro
     * calculado a partir das 12.
     */
    private int observedBeds;

    private Colony(UUID id, ColonyPos center, ColonyState state, ColonyLifecycle lifecycle) {
        this.id = id;
        this.center = center;
        this.state = state;
        this.lifecycle = lifecycle;
    }

    /**
     * Cria uma colônia recém-detectada.
     *
     * <p>Nasce {@link ColonyState#STABLE} porque nenhuma demanda foi
     * avaliada ainda, e {@link ColonyLifecycle#ACTIVE} porque a detecção
     * só acontece com o chunk carregado.
     */
    public static Colony create(UUID id, ColonyPos center) {
        return new Colony(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(center, "center"),
                ColonyState.STABLE,
                ColonyLifecycle.ACTIVE);
    }

    /**
     * Reconstrói uma colônia a partir de dados salvos.
     *
     * <p>Diferente de {@link #create}, preserva os estados gravados em vez
     * de assumir os iniciais. Usado por {@code data.save} ao acordar uma
     * colônia. Ver ADR-002.
     */
    public static Colony restore(UUID id, ColonyPos center, ColonyState state, ColonyLifecycle lifecycle) {
        return new Colony(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(center, "center"),
                Objects.requireNonNull(state, "state"),
                Objects.requireNonNull(lifecycle, "lifecycle"));
    }

    public UUID id() {
        return id;
    }

    public ColonyPos center() {
        return center;
    }

    /**
     * Reposiciona o centro da colônia.
     *
     * <p>Chamado quando a detecção reavalia a vila e o conjunto de camas
     * mudou. A identidade da colônia não é afetada. Ver ADR-003 §4.
     */
    public void setCenter(ColonyPos center) {
        this.center = Objects.requireNonNull(center, "center");
    }

    public int observedBeds() {
        return observedBeds;
    }

    /**
     * Move o centro apenas se esta observação for ao menos tão completa
     * quanto a que definiu o centro atual.
     *
     * <p>Uma detecção que enxerga menos camas viu menos da vila, e não
     * tem autoridade para reposicionar o centro. Sem esta regra o centro
     * oscila entre observações parciais feitas de pontos diferentes.
     *
     * <p>Empate move: a vila pode mudar de lugar mantendo o mesmo número
     * de camas.
     *
     * @return true se o centro foi movido
     */
    public boolean observe(ColonyPos center, int beds) {
        Objects.requireNonNull(center, "center");

        if (beds < observedBeds) {
            return false;
        }

        boolean moved = !this.center.equals(center);

        this.center = center;
        this.observedBeds = beds;

        return moved;
    }

    public ColonyState state() {
        return state;
    }

    public void setState(ColonyState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public ColonyLifecycle lifecycle() {
        return lifecycle;
    }

    public void setLifecycle(ColonyLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    /** Atalho de leitura para o loop de simulação. Ver ADR-002. */
    public boolean isActive() {
        return lifecycle == ColonyLifecycle.ACTIVE;
    }

    /**
     * Duas colônias são a mesma quando têm o mesmo id.
     *
     * <p>Posição e estado mudam ao longo da vida da colônia; o id não.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        return other instanceof Colony colony && id.equals(colony.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Colony[id=" + id
                + ", center=" + center
                + ", state=" + state
                + ", lifecycle=" + lifecycle
                + "]";
    }
}
