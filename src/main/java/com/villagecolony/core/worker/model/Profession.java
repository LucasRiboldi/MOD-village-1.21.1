package com.villagecolony.core.worker.model;

import com.villagecolony.core.type.Capability;

import java.util.Objects;
import java.util.Set;

/**
 * O que uma profissão de colônia é: o que sabe fazer e com que ferramenta.
 *
 * <p>Imutável. Uma profissão é definição, não estado: quem muda ao longo
 * da partida é o {@link Worker}, que aponta para uma delas. Ver
 * Data-Model.md e ADR-005.
 *
 * <p>Não cria tarefas, não tem prioridade e não conhece outra profissão.
 * Ver Profession-System.md §"Regras de Arquitetura".
 *
 * <p>O campo {@code allowedTasks} previsto em Profession-System.md §"Modelo
 * de Profissão" ainda não existe: depende do sistema de tarefas, que é
 * {@code core/task}, hoje vazio. A ligação entre tarefa e profissão já é
 * possível pelo outro lado — a tarefa declara a {@link Capability} que
 * exige, e {@link #canPerform} responde.
 */
public final class Profession {

    private final ProfessionType type;

    private final Set<Capability> capabilities;

    private final ToolType requiredTool;

    /**
     * Define uma profissão.
     *
     * <p>Quem chama isto é o catálogo, {@code ProfessionRegistry}. Não
     * há razão para construir uma profissão em outro lugar: elas são
     * fixas, e uma segunda instância do mesmo tipo com capacidades
     * diferentes seria uma armadilha.
     */
    public static Profession define(
            ProfessionType type, Set<Capability> capabilities, ToolType requiredTool) {
        return new Profession(type, capabilities, requiredTool);
    }

    private Profession(ProfessionType type, Set<Capability> capabilities, ToolType requiredTool) {
        this.type = Objects.requireNonNull(type, "type");
        this.requiredTool = Objects.requireNonNull(requiredTool, "requiredTool");

        Objects.requireNonNull(capabilities, "capabilities");

        if (capabilities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Profession without capability does nothing: " + type);
        }

        this.capabilities = Set.copyOf(capabilities);
    }

    public ProfessionType type() {
        return type;
    }

    /** Somente leitura. */
    public Set<Capability> capabilities() {
        return capabilities;
    }

    /**
     * Resposta à pergunta que a colônia faz antes de dar uma tarefa.
     *
     * <p>Ver Profession-System.md §"Compatibilidade de Tarefas".
     */
    public boolean canPerform(Capability capability) {
        return capability != null && capabilities.contains(capability);
    }

    /**
     * Ferramenta que o trabalhador recebe ao assumir a função.
     *
     * <p>{@link ToolType#NONE} para quem trabalha de mãos vazias — é
     * resposta legítima, não ausência de resposta, por isso não é
     * {@code Optional}.
     */
    public ToolType requiredTool() {
        return requiredTool;
    }

    public boolean needsTool() {
        return requiredTool != ToolType.NONE;
    }

    /** A profissão é identificada pelo seu tipo; há uma de cada. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Profession profession && type == profession.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "Profession[" + type
                + ", tool=" + requiredTool
                + ", capabilities=" + capabilities
                + "]";
    }
}
