package com.villagecolony.core.coordination;

import java.util.Arrays;
import java.util.Optional;

/**
 * A lista declarada do que pode surgir sem que uma profissão o obtenha — Regra 111 (R-080 do estudo v2,
 * autor de 2026-10-22). Fora desta lista nada surge: cada exceção diz a regra que a autoriza e o que ela pode
 * fazer aparecer, e cada vez que acontece fica uma linha {@code VC_SUPPLY} no log, contada por colônia e
 * exceção, para que o suprimento excepcional não vire economia infinita sem ninguém ver.
 */
public enum SupplyException {

    MISSING_PIECE(51, "a peça que a obra pede, na 4ª tentativa sem sucesso, no baú da profissão que tentou"),
    AUTOMATIC_DROP(41, "corante, linha, pó de osso e drops de bicho e inimigo que a receita pede"),
    SHAPED_GROUND(69, "canteiro, água, caminho e cultivo da planta, moldados no local"),
    UNREACHABLE_PIECE(86, "a peça que o construtor não alcança, posta no lugar"),
    APPEARING_PIECE(88, "terracota, fardo de feno e argila que a vila não tem, postos no lugar"),
    FOUNDATION_GROUND(90, "o chão do bioma que aterra o lote e apoia cama, porta e baú (Regra 21)"),
    CHEST_FOR_A_WORKER(96, "o baú ao lado da cama do trabalhador de ofício que não tem um"),
    FOUNDING_ANIMALS(102, "duas ovelhas, duas galinhas, duas vacas e dois porcos, uma vez, na fundação"),
    GRASS_FOR_THE_SHEEP(103, "o bloco de grama ao pé do poste ou no curral da ovelha"),
    STUCK_PIECE(113, "a peça de lado da obra sem mais nada a pôr, depois de 4 tentativas, posta no lugar"),
    ANIMAL_FEED(117, "a ração do par que o pastor vai criar, quando os baús não têm comida do fazendeiro"),
    WAITED_TOO_LONG(128, "o produto da tarefa largada duas vezes em 10 minutos, no baú de quem a largou");

    private final int rule;

    private final String what;

    SupplyException(int rule, String what) {
        this.rule = rule;
        this.what = what;
    }

    /** A regra que autoriza esta exceção. */
    public int rule() {
        return rule;
    }

    /** O que ela pode fazer surgir. */
    public String what() {
        return what;
    }

    /** A exceção desta regra, se a regra autoriza alguma. */
    public static Optional<SupplyException> ofRule(int rule) {
        return Arrays.stream(values()).filter(exception -> exception.rule == rule).findFirst();
    }
}
