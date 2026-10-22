package com.villagecolony.core.coordination;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * O vocabulário único de bloqueio — Regra 112 (R-052 do estudo v2, autor de 2026-10-22). Cada ofício diz em
 * frase livre por que soltou, adiou ou desistiu; o diário grava também o código, e o relatório agrupa por ele,
 * de modo que "parado 300 tiques" do mineiro e "made no progress" do lenhador contam juntos como
 * {@link #NO_PROGRESS}.
 *
 * <p>A tradução da frase para o código ({@link #classify}) é por trecho, na ordem da tabela: o mais específico
 * primeiro. Frase nova que não casa com nenhum fica {@link #UNCLASSIFIED}, e é sinal para acrescentar o trecho.
 */
public enum BlockCode {

    NO_RESOURCE,
    RESOURCE_INACCESSIBLE,
    NO_PATH,
    SITE_BLOCKED,
    NO_VALID_SITE,
    NO_RECIPE,
    TOOL_MISSING,
    CHEST_FULL,
    TARGET_RESERVED,
    PROTECTED_BLOCK,
    NO_PROGRESS,
    UNSUPPORTED_PIECE,
    WORLD_UNAVAILABLE,
    /** O trabalhador não tem baú — além da lista mínima do v2. */
    NO_STORAGE,
    UNCLASSIFIED;

    /** Os trechos de frase de cada código, do mais específico para o mais geral. */
    private static final List<Map.Entry<String, BlockCode>> PHRASES = List.of(
            Map.entry("nothing holds", UNSUPPORTED_PIECE),
            Map.entry("no physical support", UNSUPPORTED_PIECE),
            Map.entry("unsupported", UNSUPPORTED_PIECE),
            Map.entry("no room to walk", SITE_BLOCKED),
            Map.entry("not loaded", WORLD_UNAVAILABLE),
            Map.entry("unloaded", WORLD_UNAVAILABLE),
            Map.entry("no room", CHEST_FULL),
            Map.entry("chest is full", CHEST_FULL),
            Map.entry("full chest", CHEST_FULL),
            Map.entry("storage_full", CHEST_FULL),
            Map.entry("no chest", NO_STORAGE),
            Map.entry("no_storage", NO_STORAGE),
            Map.entry("village-original", PROTECTED_BLOCK),
            Map.entry("colony-built", PROTECTED_BLOCK),
            Map.entry("protected", PROTECTED_BLOCK),
            Map.entry("in the way", PROTECTED_BLOCK),
            Map.entry("no lot", NO_VALID_SITE),
            Map.entry("no safe lot", NO_VALID_SITE),
            Map.entry("no valid site", NO_VALID_SITE),
            Map.entry("no_standing_position", SITE_BLOCKED),
            Map.entry("something above", SITE_BLOCKED),
            Map.entry("the lot itself", SITE_BLOCKED),
            Map.entry("no recipe", NO_RECIPE),
            Map.entry("could not make", NO_RECIPE),
            Map.entry("does not have", NO_RECIPE),
            Map.entry("not_in_game", NO_RECIPE),
            Map.entry("no pickaxe", TOOL_MISSING),
            Map.entry("no tool", TOOL_MISSING),
            Map.entry("reserved", TARGET_RESERVED),
            Map.entry("taken by", TARGET_RESERVED),
            Map.entry("already_open", TARGET_RESERVED),
            Map.entry("too_high", RESOURCE_INACCESSIBLE),
            Map.entry("out of reach", RESOURCE_INACCESSIBLE),
            Map.entry("unreachable", RESOURCE_INACCESSIBLE),
            Map.entry("could not reach", NO_PATH),
            Map.entry("unable to reach", NO_PATH),
            Map.entry("got no closer", NO_PATH),
            Map.entry("no detour", NO_PATH),
            Map.entry("no path", NO_PATH),
            Map.entry("has not moved", NO_PROGRESS),
            Map.entry("made no progress", NO_PROGRESS),
            Map.entry("frozen", NO_PROGRESS),
            Map.entry("no material", NO_RESOURCE),
            Map.entry("missing_material", NO_RESOURCE),
            Map.entry("has none", NO_RESOURCE),
            Map.entry("no_target", NO_RESOURCE),
            Map.entry("nothing to work on", NO_RESOURCE));

    /** O código desta frase de motivo; {@link #UNCLASSIFIED} quando nenhum trecho casa. */
    public static BlockCode classify(String reason) {
        String text = reason.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, BlockCode> phrase : PHRASES) {
            if (text.contains(phrase.getKey())) {
                return phrase.getValue();
            }
        }

        return UNCLASSIFIED;
    }
}
