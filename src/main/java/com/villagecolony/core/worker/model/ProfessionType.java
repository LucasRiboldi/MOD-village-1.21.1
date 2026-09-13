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

    /** Extrai recursos minerais e materiais do terreno. */
    MINER,

    /** Derruba árvores e replanta. Produz madeira. */
    LUMBERJACK,

    /** Transforma materiais minerais em blocos de construção. */
    MASON,

    /**
     * Funde o que a fornalha funde — 2026-08-20.
     *
     * <p>Areia vira vidro, e vidro vira vidraça. É a exceção honesta que
     * a Regra 10 registrou em 08-18: a vidraça pedia fundir, e a colônia
     * não fundia.
     */
    SMELTER,

    /**
     * O carpinteiro: transforma madeira em material de construção.
     *
     * <p><b>Era o {@code MANUFACTURER}, e a divisão é de 2026-09-10</b>,
     * a pedido do autor. O fabricante fazia os dois ofícios — a tábua e a
     * pedra lavrada — em um arquivo de 639 linhas, acima do limite de
     * 500 que este projeto se impôs.
     *
     * <p><b>Save antigo não quebra.</b> {@code ColonySavedData}
     * devolve {@code null} para profissão que não reconhece, e o aldeão é
     * recontratado no ciclo seguinte: quem estava gravado como
     * {@code MANUFACTURER} volta sem função e ganha uma nova. Perde-se a
     * atribuição, não o mundo.
     */
    CARPENTER,

    /** Cuida das plantações. */
    FARMER,

    /** Produz materiais de origem animal. */
    BREEDER,

    /** Compatibilidade com saves anteriores; novas vagas usam BREEDER. */
    SHEPHERD,

    /** Compatibilidade com saves anteriores; construção agora é uma tarefa. */
    BUILDER
}
