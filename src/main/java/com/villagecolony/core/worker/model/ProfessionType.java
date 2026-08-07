package com.villagecolony.core.worker.model;

/**
 * Profissão de colônia de um trabalhador.
 *
 * <p>É uma camada adicional sobre a profissão Vanilla, não um
 * substituto: o aldeão mantém sua profissão, sua estação de trabalho e
 * sua rotina. Ver Profession-System.md.
 *
 * <p>Aqui existe apenas o tipo. Ferramenta exigida, capacidades e tarefas
 * permitidas são o sistema de profissões, que é TASK-013.
 */
public enum ProfessionType {

    /** Derruba árvores e replanta. Produz madeira. */
    LUMBERJACK,

    /** Transforma matéria-prima em material de construção. */
    MANUFACTURER,

    /** Cuida das plantações. */
    FARMER,

    /** Constrói as expansões da vila. */
    BUILDER
}
