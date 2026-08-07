package com.villagecolony.core.worker.model;

/**
 * Uma ação que uma profissão sabe executar.
 *
 * <p>É o vocabulário entre tarefa e trabalhador: a tarefa declara a
 * capacidade que exige, a profissão declara as que possui, e a colônia
 * cruza as duas. Ver Profession-System.md §"Compatibilidade de Tarefas".
 *
 * <p>Existe como tipo próprio, e não como método de {@link ProfessionType},
 * porque duas profissões podem vir a compartilhar uma capacidade e uma
 * profissão nova não pode obrigar a mexer nas antigas. Ver
 * Profession-System.md §"Regras de Arquitetura".
 */
public enum Capability {

    /** Derrubar árvores e recolher a madeira. */
    COLLECT_WOOD,

    /** Transformar matéria-prima em material de construção. */
    CRAFT_ITEMS,

    /** Manter a plantação e o estoque de comida da colônia. */
    MAINTAIN_FOOD,

    /** Erguer as estruturas de uma expansão. */
    BUILD_STRUCTURE
}
