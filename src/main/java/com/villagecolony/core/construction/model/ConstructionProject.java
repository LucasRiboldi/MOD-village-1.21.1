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

    /**
     * Uma obra que volta do save.
     *
     * <p>Diferente de {@link #plan}, preserva a identidade e o estado
     * gravados — como {@code Colony.restore} faz com a colônia.
     *
     * <p><b>O que ela não traz de volta é o progresso</b>, e isso é
     * decisão, não esquecimento: quem sabe o que já está construído é o
     * mundo. Quem restaura risca da lista os blocos que já estão de pé,
     * comparando com o que há em cada posição — ver
     * {@code ConstructionPlanner.resume}.
     *
     * <p>Sai mais barato no save e sai mais <b>certo</b>: uma parede que
     * o jogador derrubou entre uma sessão e outra volta para a lista, e a
     * colônia a levanta de novo. Uma lista de posições gravada teria
     * jurado que ela estava lá.
     */
    public static ConstructionProject restore(
            UUID id,
            UUID colonyId,
            Blueprint blueprint,
            ColonyPos origin,
            ConstructionState state) {

        ConstructionProject project = new ConstructionProject(
                Objects.requireNonNull(id, "id"),
                Objects.requireNonNull(colonyId, "colonyId"),
                Objects.requireNonNull(blueprint, "blueprint"),
                Objects.requireNonNull(origin, "origin"));

        project.state = Objects.requireNonNull(state, "state");

        return project;
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
     * Se esta obra sai da frente para dar lugar ao alvo de hoje.
     *
     * <p>Duas condições, e as duas importam. A planta precisa ser
     * <b>outra</b> que não o alvo atual da colônia, e não pode haver um
     * único bloco de pé. Sem bloco posto nada se perde ao abandoná-la, e
     * o que se ganha é a colônia voltando a construir: quem planeja não
     * abre obra nova enquanto houver uma aberta.
     *
     * <p>Com bloco de pé é o contrário. Casa pela metade é do jogador, e
     * abandoná-la deixaria um esqueleto no mundo com o lote ocupado —
     * essa continua de onde parou, seja qual for o alvo.
     *
     * <p><b>O alvo vem de fora, e é o ponto desta assinatura.</b> Ele já
     * foi escrito fixo aqui dentro duas vezes, e nas duas envelheceu: a
     * Regra 13 fez da cabana o alvo em 2026-08-15, e a Regra 24 devolveu
     * a casa do jogo às vilas de planície em 2026-08-19. Uma obra não
     * tem como saber o que a colônia dela constrói hoje — quem sabe é a
     * colônia, e por isso ela é quem responde.
     */
    public boolean isSupersededBy(ResourceId target) {
        Objects.requireNonNull(target, "target");

        return remaining.size() == blueprint.blockCount() && !blueprint.id().equals(target);
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
