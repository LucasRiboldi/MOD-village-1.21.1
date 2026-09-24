package com.villagecolony.core.colony.model;

/**
 * A profissão de um trabalhador, vista de {@code core.colony} — decisão
 * 11A, 2026-09-24.
 *
 * <p><b>Por que não é {@code core.worker.model.ProfessionType}.</b>
 * {@code DependencyRuleTest.coreDomainsDoNotImportEachOther} proíbe
 * {@code core/colony} de importar {@code core/worker}: são dois
 * domínios do {@code core}, e a única saída compartilhada é
 * {@code core/type} — onde {@code ProfessionType} não mora. Espelhar os
 * mesmos oito valores aqui é o mesmo preço que
 * {@code core.telemetry.model.ActivityProfession} já paga pelo mesmo
 * motivo — ver o javadoc dela.
 *
 * <p>Quem monta {@link VillageInventory} (em {@code fabric/}, que
 * enxerga os dois lados) faz a tradução.
 */
public enum ColonyProfession {
    MINER,
    LUMBERJACK,
    MASON,
    SMELTER,
    CARPENTER,
    FARMER,
    SHEPHERD,
    BUILDER
}
