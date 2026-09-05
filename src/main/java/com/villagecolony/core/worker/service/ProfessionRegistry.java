package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.Profession;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.ToolType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * As profissões que existem, e o que cada uma sabe fazer.
 *
 * <p>Catálogo, não registro mutável: as quatro profissões do MVP são
 * fixas e não dependem de estado de partida. Por isso é estático e não
 * tem {@code clear} — nada aqui pertence a um mundo específico, ao
 * contrário de {@code ColonyService} e {@code WorkerService}. Ver
 * Profession-System.md §"Profissões do MVP".
 *
 * <p>Acrescentar Miner ou Fisher no futuro é acrescentar um valor a
 * {@link ProfessionType} e uma linha em {@link #define}, sem tocar nas
 * profissões existentes. Ver Profession-System.md §"Regras de
 * Arquitetura".
 */
public final class ProfessionRegistry {

    private static final Map<ProfessionType, Profession> PROFESSIONS =
            new EnumMap<>(ProfessionType.class);

    static {
        define(ProfessionType.LUMBERJACK, ToolType.IRON_AXE, Capability.COLLECT_WOOD);
        // <b>Madeira, e não diamante</b> — decisão do autor, 2026-09-04:
        // "todos trabalhadores começam com a ferramenta nível 1 de
        // madeira". Desfaz a de 08-27, que dava diamante ao mineiro
        // porque "vinte blocos de descida com picareta de madeira é uma
        // sessão inteira" — e continua sendo, com a diferença de que
        // agora existe saída: o ToolUpgrade troca pela melhor do baú
        // dele, e a colônia mesma põe picaretas lá. A descida lenta
        // deixou de ser um teto e virou o primeiro degrau.
        define(ProfessionType.MINER, ToolType.IRON_PICKAXE, Capability.COLLECT_STONE);
        define(ProfessionType.SHEPHERD, ToolType.SHEARS, Capability.COLLECT_WOOL);
        define(ProfessionType.SMELTER, ToolType.NONE, Capability.SMELT_ITEMS);
        define(ProfessionType.MANUFACTURER, ToolType.NONE, Capability.CRAFT_ITEMS);
        define(ProfessionType.FARMER, ToolType.IRON_HOE, Capability.MAINTAIN_FOOD);
        define(ProfessionType.BUILDER, ToolType.NONE, Capability.BUILD_STRUCTURE);
    }

    private ProfessionRegistry() {
    }

    private static void define(ProfessionType type, ToolType tool, Capability... capabilities) {
        PROFESSIONS.put(type, Profession.define(type, Set.of(capabilities), tool));
    }

    /**
     * A definição de uma profissão.
     *
     * <p>Não devolve {@code Optional}: todo valor de {@link ProfessionType}
     * tem definição, e a falta de uma é erro de programação — uma
     * profissão acrescentada ao enum sem entrada no catálogo. Falhar aqui
     * é melhor que devolver vazio e o chamador tratar como "sem função".
     */
    public static Profession of(ProfessionType type) {
        Objects.requireNonNull(type, "type");

        Profession profession = PROFESSIONS.get(type);

        if (profession == null) {
            throw new IllegalStateException("Profession not defined: " + type);
        }

        return profession;
    }

    /** Todas as profissões, na ordem de {@link ProfessionType}. */
    public static List<Profession> all() {
        return List.copyOf(PROFESSIONS.values());
    }

    /**
     * Quais profissões atendem a uma capacidade exigida por uma tarefa.
     *
     * <p>Devolve lista, e não uma profissão: hoje cada capacidade tem um
     * dono só, mas nada no modelo garante isso, e supor unicidade aqui
     * quebraria em silêncio quando duas profissões a compartilharem.
     */
    public static List<Profession> withCapability(Capability capability) {
        List<Profession> result = new ArrayList<>();

        for (Profession profession : PROFESSIONS.values()) {
            if (profession.canPerform(capability)) {
                result.add(profession);
            }
        }

        return List.copyOf(result);
    }
}
