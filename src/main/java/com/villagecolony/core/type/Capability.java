package com.villagecolony.core.type;

/**
 * Uma ação que uma profissão sabe executar.
 *
 * <p>É o vocabulário entre tarefa e trabalhador: a tarefa declara a
 * capacidade que exige, a profissão declara as que possui, e a colônia
 * cruza as duas. Ver Profession-System.md §"Compatibilidade de Tarefas".
 *
 * <p>Mora em {@code core.type}, e não no domínio worker, exatamente por
 * ser esse vocabulário. A ADR-006 §6 proíbe um domínio do Core importar
 * outro: se ficasse em {@code core.worker.model}, o domínio task teria
 * de importá-lo para dizer de que precisa. Aqui, os dois lados dependem
 * do tipo compartilhado e nenhum depende do outro. Mesmo papel de
 * {@code ColonyPos}.
 *
 * <p>Existe como tipo próprio, e não como método de {@code ProfessionType},
 * porque duas profissões podem vir a compartilhar uma capacidade e uma
 * profissão nova não pode obrigar a mexer nas antigas. Ver
 * Profession-System.md §"Regras de Arquitetura".
 */
public enum Capability {

    /** Derrubar árvores e recolher a madeira. */
    COLLECT_WOOD,

    /** Tirar pedra do mundo: pedregulho, ou arenito no deserto. */
    COLLECT_STONE,

    /** Tosquiar ovelha. */
    COLLECT_WOOL,

    /** Fundir: areia em vidro, e o que mais a fornalha fizer. */
    SMELT_ITEMS,

    /** Transformar matéria-prima em material de construção. */
    CRAFT_ITEMS,

    /** Manter a plantação e o estoque de comida da colônia. */
    MAINTAIN_FOOD,

    /** Erguer as estruturas de uma expansão. */
    BUILD_STRUCTURE
}
