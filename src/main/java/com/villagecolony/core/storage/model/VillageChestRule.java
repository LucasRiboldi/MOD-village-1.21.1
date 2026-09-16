package com.villagecolony.core.storage.model;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Se a colônia pode usar este baú da vila — 2026-09-16.
 *
 * <p><b>Pedido do autor:</b> <i>"permitir que o recurso que falta possa
 * ser recolhido de qualquer baú que esteja na vila automaticamente"</i>,
 * com a ressalva que ele escolheu: <b>menos os marcados</b>.
 *
 * <p>Até aqui a colônia só enxergava os baús que reivindicou para um
 * trabalhador, mais o da boca da mina — ver {@code ColonyChests}. Um baú
 * que o jogador pusesse na vila, cheio do material que a obra espera, era
 * invisível, e a obra ficava em {@code WAITING_RESOURCES} do lado dele. O
 * log de 01:19 mostrou isso com a biblioteca esperando {@code lectern}.
 *
 * <p><b>O baú privado é o que o jogador NOMEIA.</b> Nomear exige bigorna
 * e é ato deliberado — ninguém nomeia um baú por acidente —, e é simétrico
 * ao que o mod já faz com aldeão desde 2026-08-08: nome dado à mão é
 * intocável. O jogador que quiser guardar as próprias coisas dá um nome ao
 * baú, e a colônia passa longe.
 *
 * <p><b>Qualquer nome serve</b>, e não uma palavra mágica. Exigir a
 * palavra certa faria o jogador perder material por erro de digitação, e o
 * erro só apareceria depois de a colônia já ter levado.
 */
public final class VillageChestRule {

    /**
     * Os nomes que o próprio jogo dá, e que não são nome de ninguém.
     *
     * <p>Um baú comum responde "Chest" quando perguntado pelo nome de
     * exibição, e tratá-lo como nomeado tornaria a regra inútil: nenhum
     * baú da vila seria usado, e o pedido do autor viraria letra morta.
     *
     * <p>Em minúsculas, e conferido assim: o jogo devolve o nome na língua
     * do servidor, e o autor joga em português.
     */
    private static final Set<String> DEFAULT_NAMES = Set.of(
            "chest", "baú", "bau",
            "large chest", "baú grande", "bau grande",
            "trapped chest", "baú armadilhado", "bau armadilhado",
            "barrel", "barril");

    private VillageChestRule() {
    }

    /**
     * Se a colônia pode contar e retirar deste baú.
     *
     * @param name o nome de exibição do baú, vazio quando ele não tem
     *     nenhum
     */
    public static boolean mayTake(Optional<String> name) {
        if (name.isEmpty()) {
            return true;
        }

        String written = name.get().trim();

        if (written.isEmpty()) {
            return true;
        }

        return DEFAULT_NAMES.contains(written.toLowerCase(Locale.ROOT));
    }
}
