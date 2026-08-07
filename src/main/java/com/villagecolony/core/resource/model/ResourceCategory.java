package com.villagecolony.core.resource.model;

/**
 * De onde um recurso vem.
 *
 * <p>Ver Resource-System.md §"Categorias". A categoria é o que liga o
 * recurso à profissão que o produz — madeira bruta vem do lenhador,
 * tábua vem do fabricante — sem que uma conheça a outra.
 */
public enum ResourceCategory {

    /** Obtido direto do mundo. */
    NATURAL,

    /** Criado por receita Vanilla a partir de outro recurso. */
    PROCESSED,

    /** Exigido por uma construção. */
    CONSTRUCTION
}
