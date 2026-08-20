package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * A mina que o mineiro cava — a Regra 29, 2026-08-20.
 *
 * <p>O autor a descreveu por inteiro, e ela é geometria: o mineiro anda
 * até o fim da vila e desce cavando <b>em escada</b>, para poder voltar
 * a subir. Desce dez blocos, abre uma sala de sete por quatro no décimo,
 * desce mais dez por outro lado, abre outra sala no vigésimo, e dali em
 * diante recolhe na altura do aldeão mais um, sem fim.
 *
 * <pre>
 * lance 1     dez degraus, dois blocos de altura cada — os que o
 *             aldeão precisa para caber de pé
 * sala 1      sete por quatro no nível -10
 * lance 2     mais dez degraus, virando à direita: cavar reto para
 *             baixo daria um poço, e de poço não se sobe
 * sala 2      sete por quatro no nível -20
 * galeria     do nível -20 em diante, sem fim
 * </pre>
 *
 * <p><b>Por que a escada, e não o poço.</b> É a frase do autor: "de modo
 * que ele possa subir de volta". Um aldeão que cavasse reto para baixo
 * ficaria no fundo do buraco, e a colônia perderia um trabalhador por
 * causa do próprio trabalho.
 *
 * <p><b>Por que dois blocos de altura.</b> "na altura do aldeão mais 1"
 * — os pés e a cabeça. Um só e ele não passa; três e a mina custa
 * cinquenta por cento a mais de tempo para dar a mesma pedra.
 *
 * <p>Mora em {@code core} e não conhece Minecraft: é geometria pura, e
 * geometria se afirma sem subir servidor. Quem decide se um bloco
 * <i>pode</i> ser cavado — pedra do jogador, bedrock, a Regra 3 — é a
 * camada de fora.
 */
public record MineShaft(ColonyPos entry, Side descent, Side gallery) {

    /** Blocos por lance de escada. Dois lances até o fundo. */
    public static final int DESCENT = 10;

    /** O comprimento da sala, no sentido em que se descia. */
    public static final int ROOM_LONG = 7;

    /** A largura da sala, para o lado. */
    public static final int ROOM_WIDE = 4;

    /** Altura do aldeão mais um: os pés e a cabeça. */
    public static final int HEADROOM = 2;

    /** Quantas posições um lance de escada pede. */
    private static final int STAIR_BLOCKS = DESCENT * HEADROOM;

    /** Quantas posições uma sala pede. */
    private static final int ROOM_BLOCKS = ROOM_LONG * ROOM_WIDE * HEADROOM;

    /** Onde cada trecho começa, na ordem em que se cava. */
    private static final int ROOM_ONE = STAIR_BLOCKS;

    private static final int STAIR_TWO = ROOM_ONE + ROOM_BLOCKS;

    private static final int ROOM_TWO = STAIR_TWO + STAIR_BLOCKS;

    /** A partir daqui é galeria, e ela não acaba. */
    public static final int CARVED = ROOM_TWO + ROOM_BLOCKS;

    public MineShaft {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(descent, "descent");
        Objects.requireNonNull(gallery, "gallery");
    }

    /**
     * A mina que começa aqui, descendo para este lado.
     *
     * <p>O segundo lance vira à direita, e a galeria segue à direita de
     * novo. Duas curvas à direita afastam a galeria do lance de subida,
     * que é onde o aldeão anda.
     */
    public static MineShaft from(ColonyPos entry, Side descent) {
        return new MineShaft(entry, descent, descent.clockwise().clockwise());
    }

    /**
     * A mesma mina, com a galeria virada.
     *
     * <p>É a frase do autor: <i>"sempre que encontrar uma barreira que
     * impeça de realizar estas ações ele começa a recolher para outro
     * lado"</i>. Lava, bedrock, uma caverna — a galeria vira e segue.
     */
    public MineShaft turned() {
        return new MineShaft(entry, descent, gallery.clockwise());
    }

    /**
     * A posição de índice {@code i} na ordem de cavar.
     *
     * <p>Índice acima de {@link #CARVED} é galeria, e por isso não há
     * teto: a mina não acaba, e quem a interrompe é o expediente, a
     * paciência do jogador ou o fim do mundo.
     */
    public ColonyPos positionAt(int i) {
        if (i < ROOM_ONE) {
            return stair(entry, descent, i);
        }

        if (i < STAIR_TWO) {
            return room(landingOne(), descent, i - ROOM_ONE);
        }

        if (i < ROOM_TWO) {
            return stair(cornerOne(), descent.clockwise(), i - STAIR_TWO);
        }

        if (i < CARVED) {
            return room(landingTwo(), descent.clockwise(), i - ROOM_TWO);
        }

        return tunnel(i - CARVED);
    }

    /**
     * Um degrau: o bloco dos pés e o da cabeça, um passo adiante e um
     * abaixo do anterior.
     */
    private static ColonyPos stair(ColonyPos top, Side towards, int i) {
        int step = i / HEADROOM + 1;
        int layer = i % HEADROOM;

        return new ColonyPos(
                top.x() + towards.offsetX() * step,
                top.y() - step + 1 + layer,
                top.z() + towards.offsetZ() * step);
    }

    /** Onde o primeiro lance para: dez blocos abaixo da entrada. */
    private ColonyPos landingOne() {
        return new ColonyPos(
                entry.x() + descent.offsetX() * DESCENT,
                entry.y() - DESCENT,
                entry.z() + descent.offsetZ() * DESCENT);
    }

    /**
     * O canto da primeira sala de onde o segundo lance parte.
     *
     * <p><b>O canto, e não a ponta.</b> Partir da ponta punha os
     * primeiros degraus dentro da largura da sala — o teste da forma
     * pegou isso, e a sobreposição custaria ao aldeão bater a picareta
     * no ar oito vezes.
     */
    private ColonyPos cornerOne() {
        Side sideways = descent.clockwise();

        ColonyPos floor = landingOne();

        return new ColonyPos(
                floor.x() + descent.offsetX() * ROOM_LONG + sideways.offsetX() * (ROOM_WIDE - 1),
                floor.y(),
                floor.z() + descent.offsetZ() * ROOM_LONG + sideways.offsetZ() * (ROOM_WIDE - 1));
    }

    /** Onde o segundo lance para: vinte blocos abaixo da entrada. */
    private ColonyPos landingTwo() {
        Side towards = descent.clockwise();

        ColonyPos from = cornerOne();

        return new ColonyPos(
                from.x() + towards.offsetX() * DESCENT,
                from.y() - DESCENT,
                from.z() + towards.offsetZ() * DESCENT);
    }

    /**
     * Uma posição da sala: sete de fundo, quatro de largura, duas de alto.
     *
     * <p><b>Começa um bloco adiante do patamar</b>, e não sobre ele. O
     * último degrau abre exatamente os dois blocos que seriam o canto da
     * sala, e cavá-los de novo seria o aldeão batendo a picareta no ar.
     */
    private static ColonyPos room(ColonyPos floor, Side towards, int i) {
        int high = i % HEADROOM;
        int wide = i / HEADROOM % ROOM_WIDE;
        int deep = i / (HEADROOM * ROOM_WIDE) + 1;

        Side sideways = towards.clockwise();

        return new ColonyPos(
                floor.x() + towards.offsetX() * deep + sideways.offsetX() * wide,
                floor.y() + 1 + high,
                floor.z() + towards.offsetZ() * deep + sideways.offsetZ() * wide);
    }

    /**
     * A galeria sem fim, no nível da segunda sala.
     *
     * <p>Parte do canto oposto da sala para não recavá-la: a sala já
     * está aberta, e a galeria é o que vem depois dela.
     */
    private ColonyPos tunnel(int i) {
        int step = i / HEADROOM + 1;
        int high = i % HEADROOM;

        Side towards = descent.clockwise();

        ColonyPos floor = landingTwo();

        ColonyPos from = new ColonyPos(
                floor.x() + towards.offsetX() * ROOM_LONG,
                floor.y(),
                floor.z() + towards.offsetZ() * ROOM_LONG);

        return new ColonyPos(
                from.x() + gallery.offsetX() * step,
                from.y() + 1 + high,
                from.z() + gallery.offsetZ() * step);
    }
}
