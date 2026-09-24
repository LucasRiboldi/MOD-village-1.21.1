package com.villagecolony.core.telemetry.model;

/**
 * A profissão de um evento de traço — decisão 7B, 2026-09-24.
 *
 * <p><b>Por que não é {@code core.worker.model.ProfessionType}.</b>
 * {@code DependencyRuleTest.coreDomainsDoNotImportEachOther} proíbe
 * {@code core/telemetry} de importar {@code core/worker}: são dois
 * domínios do {@code core}, e a única saída compartilhada é
 * {@code core/type} — onde {@code ProfessionType} não mora. Espelhar os
 * mesmos oito valores aqui é o preço de manter o traço testável sem
 * violar a ADR-006 §6; quem grava o evento (em {@code fabric/}, que
 * enxerga os dois domínios) faz a tradução.
 *
 * <p>{@code UNKNOWN} existe para todo enum salvo em NBT — Regra do
 * projeto: um valor de uma versão futura, ou um save editado à mão,
 * nunca pode impedir o carregamento do mundo.
 */
public enum ActivityProfession {
    MINER,
    LUMBERJACK,
    MASON,
    SMELTER,
    CARPENTER,
    FARMER,
    SHEPHERD,
    BUILDER,
    UNKNOWN
}
