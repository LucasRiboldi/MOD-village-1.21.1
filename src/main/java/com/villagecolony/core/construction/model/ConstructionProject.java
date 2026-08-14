package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma obra: este projeto, neste lugar, neste estado.
 *
 * <p>{@link Blueprint} diz o que construir; este diz onde, e quanto já
 * foi feito. O modelo é o de Construction-System.md §"Modelo de
 * Construção", menos {@code rotation} — o MVP levanta a casa como ela
 * está no arquivo, e girar é decisão que só faz sentido quando houver
 * estrada com direção.
 *
 * <p>Modelo de dados: guarda estado e valida o que recebe. Não escolhe
 * onde construir, não lê o mundo e não coloca bloco algum — isso é dos
 * services e da camada fabric. CODE-STANDARDS.md §5.
 *
 * <p><b>O progresso é por posição, e não por contagem.</b> Um contador
 * "faltam 43" não sobrevive ao construtor que pula um bloco porque ele
 * ainda não pode ser posto — a porta antes da parede, a tocha antes do
 * bloco em que ela se apoia. Guardando quais posições já foram feitas, o
 * construtor pode tentar em qualquer ordem e voltar depois.
 */
public final class ConstructionProject {

    private final UUID id;

    private final UUID colonyId;

    private final Blueprint blueprint;

    /**
     * O canto do projeto no mundo.
     *
     * <p>Toda posição do blueprint é somada a esta. Não muda: mover uma
     * obra em curso deixaria metade da casa no lugar antigo.
     */
    private final ColonyPos origin;

    /** As posições do projeto que ainda não foram construídas. */
    private final List<BlueprintBlock> remaining;

    private ConstructionState state;

    private ConstructionProject(
            UUID id, UUID colonyId, Blueprint blueprint, ColonyPos origin) {

        this.id = id;
        this.colonyId = colonyId;
        this.blueprint = blueprint;
        this.origin = origin;
        this.remaining = new ArrayList<>(blueprint.blocks());
        this.state = ConstructionState.PLANNED;
    }

    /** Uma obra recém-decidida, com nada feito ainda. */
    public static ConstructionProject plan(UUID colonyId, Blueprint blueprint, ColonyPos origin) {
        return new ConstructionProject(
                UUID.randomUUID(),
                Objects.requireNonNull(colonyId, "colonyId"),
                Objects.requireNonNull(blueprint, "blueprint"),
                Objects.requireNonNull(origin, "origin"));
    }

    public UUID id() {
        return id;
    }

    public UUID colonyId() {
        return colonyId;
    }

    public Blueprint blueprint() {
        return blueprint;
    }

    public ColonyPos origin() {
        return origin;
    }

    public ConstructionState state() {
        return state;
    }

    /**
     * Move a obra para o estado seguinte.
     *
     * @throws IllegalStateException se o caminho não existe. Estourar é
     *     melhor que ignorar em silêncio: uma obra que ficasse parada
     *     num estado errado nunca mais sairia dele, e ninguém saberia
     *     por quê
     */
    public void moveTo(ConstructionState next) {
        Objects.requireNonNull(next, "next");

        if (!state.canBecome(next)) {
            throw new IllegalStateException("Cannot go from " + state + " to " + next);
        }

        state = next;
    }

    /**
     * Tudo o que a obra pede, do começo ao fim.
     *
     * <p>Não desconta o que já foi posto. Para saber o que ainda falta
     * comprar, use {@link #remainingMaterials}.
     */
    public Map<ResourceId, Integer> materials() {
        return blueprint.materials();
    }

    /**
     * O que ainda falta pôr, contado por tipo de bloco — TASK-032.
     *
     * <p>É esta a lista que vira demanda: pedir de novo o que já está na
     * parede mandaria o lenhador cortar madeira que a casa não vai
     * consumir.
     */
    public Map<ResourceId, Integer> remainingMaterials() {
        Map<ResourceId, Integer> tally = new LinkedHashMap<>();

        for (BlueprintBlock block : remaining) {
            tally.merge(block.block(), 1, Integer::sum);
        }

        return Map.copyOf(tally);
    }

    /** Quantos blocos ainda faltam. */
    public int remainingCount() {
        return remaining.size();
    }

    /** As posições que faltam, na ordem do projeto. Somente leitura. */
    public List<BlueprintBlock> remaining() {
        return List.copyOf(remaining);
    }

    /**
     * Onde vai este bloco, no mundo.
     *
     * <p>A soma que transforma projeto em obra. Mora aqui e em nenhum
     * outro lugar: espalhada pelos chamadores, uma casa acabaria com
     * metade dos blocos deslocados.
     */
    public ColonyPos worldPositionOf(BlueprintBlock block) {
        Objects.requireNonNull(block, "block");

        return new ColonyPos(
                origin.x() + block.offset().x(),
                origin.y() + block.offset().y(),
                origin.z() + block.offset().z());
    }

    /**
     * Risca um bloco da lista.
     *
     * @return true se ele ainda estava por fazer. False para bloco que
     *     já tinha sido posto — chamar duas vezes é possível quando o
     *     construtor repete um passo, e não pode contar duas
     */
    public boolean markPlaced(BlueprintBlock block) {
        Objects.requireNonNull(block, "block");

        return remaining.remove(block);
    }

    /** Se não falta mais nada a pôr. */
    public boolean isFinished() {
        return remaining.isEmpty();
    }

    /**
     * O primeiro bloco que ainda falta, se falta algum.
     *
     * <p>Quem constrói pode pular este e tentar outro: um bloco que
     * ainda não pode ser posto — a tocha antes da parede — continua na
     * lista para a próxima passagem.
     */
    public Optional<BlueprintBlock> nextBlock() {
        return remaining.isEmpty() ? Optional.empty() : Optional.of(remaining.get(0));
    }

    @Override
    public String toString() {
        return "ConstructionProject[id=" + id
                + ", colony=" + colonyId
                + ", blueprint=" + blueprint.id()
                + ", origin=" + origin
                + ", state=" + state
                + ", remaining=" + remaining.size()
                + "]";
    }
}
