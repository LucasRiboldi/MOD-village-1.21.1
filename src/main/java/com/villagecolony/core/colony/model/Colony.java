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

    /**
     * A âncora da última varredura ancorada, e o que ela viu.
     *
     * <p>A sonda é a varredura que parte do centro da própria colônia,
     * repetida a cada ciclo. Ao contrário da posição do jogador, ela é o
     * mesmo ponto de um ciclo para o outro, e por isso duas leituras
     * dela são comparáveis.
     *
     * <p>Guardados fora de {@link #observedBeds} de propósito: são
     * atualizados a cada leitura da sonda, aceita ou recusada. Ligá-los
     * à observação aceita foi o defeito de 2026-08-07 — a âncora só
     * nascia numa aceitação, e nenhuma aceitação vinha enquanto a
     * colônia estivesse grande demais. Nada nunca encolhia.
     */
    private ColonyPos probeAnchor;

    private int probeBeds;

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

    public ColonyPos probeAnchor() {
        return probeAnchor;
    }

    public int probeBeds() {
        return probeBeds;
    }

    /**
     * Observação sem prova de completude e sem âncora. Ver
     * {@link #observe(ColonyPos, int, boolean, ColonyPos)}.
     */
    public boolean observe(ColonyPos center, int beds) {
        return observe(center, beds, false, null);
    }

    /** Observação com prova geométrica, sem âncora. */
    public boolean observe(ColonyPos center, int beds, boolean complete) {
        return observe(center, beds, complete, null);
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
     * <p>A colônia pode encolher — decisão do autor em 2026-08-07. Duas
     * coisas dão autoridade para baixar a contagem:
     *
     * <ul>
     *   <li>{@code complete}: a detecção provou que não cortou cama
     *       alguma. Rara em vila grande, e insuficiente sozinha — ver
     *       §15.
     *   <li>a sonda repetir a leitura: duas varreduras seguidas da mesma
     *       âncora vendo o mesmo tanto, ou menos. A sonda é o mesmo
     *       ponto de um ciclo para o outro, então suas leituras são
     *       comparáveis entre si, e uma leitura que se confirma não é
     *       acidente de posição.
     * </ul>
     *
     * <p>Só a sonda ancorada no centro da colônia traz {@code from}. A
     * varredura que parte do jogador vem sem âncora de propósito: um
     * jogador parado na borda da vila repetiria a mesma visão pobre
     * ciclo após ciclo, e ela se confirmaria — a deriva do §11 de volta.
     *
     * <p>Quem prova a completude é a detecção, não esta classe: o Core
     * não sabe o que é raio de busca nem chunk. Ver
     * {@code VillageDetector#evaluate}.
     *
     * @param complete se a observação provadamente não cortou cama
     *     alguma do cluster
     * @param from âncora da sonda; {@code null} para varredura que não é
     *     sonda, e nesse caso a observação nunca encolhe
     * @return true se o centro foi movido
     */
    public boolean observe(ColonyPos center, int beds, boolean complete, ColonyPos from) {
        Objects.requireNonNull(center, "center");

        // A leitura anterior da sonda também precisa estar abaixo da
        // contagem registrada. Sem isso, a primeira leitura menor já
        // passaria: a sonda que viu 38 e depois 33 confirmaria o 33
        // contra si mesma, e uma visão parcial isolada encolheria a
        // colônia.
        boolean confirmedByProbe = from != null
                && from.equals(probeAnchor)
                && beds <= probeBeds
                && probeBeds < observedBeds;

        if (from != null) {
            probeAnchor = from;
            probeBeds = beds;
        }

        if (beds < observedBeds && !complete && !confirmedByProbe) {
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
