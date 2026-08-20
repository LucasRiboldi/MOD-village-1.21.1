package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Uma construção que a colônia levantou — TASK-036.
 *
 * <p>O que fica depois que a obra termina. O canteiro sai do registro; a
 * casa não.
 *
 * <p><b>Por caixa, e não por bloco.</b> A TASK-036 pede posição, tipo e
 * colônia, e a caixa responde às três perguntas que dependem disto: a
 * proteção ("este bloco é da colônia?"), a fusão de vilas decidida em
 * 2026-08-12 ("um bloco de uma encostou no da outra?") e o registro de
 * infraestrutura permanente do PROJECT_CONSTITUTION.md §10. Guardar as
 * cento e cinquenta posições de cada casa daria as mesmas respostas por
 * um preço muito maior em memória e em disco.
 *
 * <p>O que a caixa perde: o vazio dentro da casa fica dentro dela. Uma
 * árvore que nascesse no meio do quarto seria "da colônia" para efeito de
 * proteção. É o lado seguro do erro — protege demais, nunca de menos.
 *
 * @param id identidade própria, para o dia em que uma construção puder
 *     ser derrubada ou reformada
 * @param colonyId de quem ela é
 * @param blueprint que projeto a originou
 * @param min canto de menor coordenada, inclusive
 * @param max canto de maior coordenada, inclusive
 */
public record Building(
        UUID id, UUID colonyId, ResourceId blueprint, ColonyPos min, ColonyPos max,
        Set<ResourceId> furnished) {

    public Building {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(blueprint, "blueprint");
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        Objects.requireNonNull(furnished, "furnished");

        if (max.x() < min.x() || max.y() < min.y() || max.z() < min.z()) {
            throw new IllegalArgumentException("Inverted bounds: " + min + " to " + max);
        }

        furnished = Set.copyOf(furnished);
    }

    /** Uma casa que ainda não recebeu peça nenhuma. */
    public Building(
            UUID id, UUID colonyId, ResourceId blueprint, ColonyPos min, ColonyPos max) {

        this(id, colonyId, blueprint, min, max, Set.of());
    }

    /**
     * Se esta peça já entrou nesta casa alguma vez.
     *
     * <p><b>Alguma vez, e não "está lá agora".</b> A diferença é a regra
     * do autor de 2026-08-20: peça destruída não volta. Perguntar ao
     * mundo responde "não está lá" tanto para a cama que nunca entrou
     * quanto para a que o jogador desfez com uma picareta, e tratar as
     * duas igual põe a colônia a repor o que alguém tirou de propósito.
     *
     * <p>Por isso isto mora na construção, e não num mapa em memória: a
     * casa sobrevive ao servidor parar, e a resposta precisa sobreviver
     * junto. Uma marca esquecida no boot faria a mobília voltar do nada
     * na sessão seguinte, que é o mesmo defeito com prazo mais longo.
     */
    public boolean wasFurnishedWith(ResourceId piece) {
        return furnished.contains(piece);
    }

    /** Esta casa, agora com esta peça na conta do que já recebeu. */
    public Building withFurnished(ResourceId piece) {
        Objects.requireNonNull(piece, "piece");

        Set<ResourceId> next = new LinkedHashSet<>(furnished);

        next.add(piece);

        return new Building(id, colonyId, blueprint, min, max, next);
    }

    /**
     * A construção que uma obra terminada deixou.
     *
     * <p>A caixa vem do projeto e da origem, e não dos blocos de fato
     * colocados: um bloco que o construtor tenha pulado — a tocha sem
     * parede, por exemplo — continua sendo parte da casa para efeito de
     * proteção. Do contrário a casa teria buracos por onde outra obra
     * poderia passar.
     */
    public static Building of(ConstructionProject project) {
        Objects.requireNonNull(project, "project");

        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        return new Building(
                UUID.randomUUID(),
                project.colonyId(),
                project.blueprint().id(),
                origin,
                new ColonyPos(
                        origin.x() + size.x() - 1,
                        origin.y() + size.y() - 1,
                        origin.z() + size.z() - 1));
    }

    /** Se esta posição está dentro da construção. */
    public boolean contains(ColonyPos pos) {
        Objects.requireNonNull(pos, "pos");

        return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }

    /**
     * Se esta construção encosta na outra.
     *
     * <p>É a pergunta da fusão de vilas: duas viram uma quando um bloco
     * de uma encostar no bloco da outra. "Encostar" inclui tocar-se pela
     * face, e por isso a comparação é com folga de um bloco.
     */
    public boolean touches(Building other) {
        Objects.requireNonNull(other, "other");

        return min.x() - 1 <= other.max.x() && max.x() + 1 >= other.min.x()
                && min.y() - 1 <= other.max.y() && max.y() + 1 >= other.min.y()
                && min.z() - 1 <= other.max.z() && max.z() + 1 >= other.min.z();
    }
}
