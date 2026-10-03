package com.villagecolony.core.movement;

/**
 * O que uma posição do mundo é para quem anda, cava e põe bloco — ADR-025,
 * fase 2.
 *
 * <p>O planejador não lê bloco: lê isto. Quem traduz o mundo para cá é a
 * camada {@code fabric}, e é lá que mora a Regra 3 (nada da vila, da colônia
 * ou do jogador sai do lugar).
 */
public enum Cell {

    /** Vazio onde se passa e onde se pode pôr bloco: ar, capim, neve fina. */
    OPEN(true, false, false, true),

    /** Vazio onde se passa, mas que não se ocupa: tocha, escada de mão, placa. */
    PASSAGE(true, false, false, false),

    /** Terreno natural que se pisa e que a picareta tira. */
    ROCK(false, true, true, false),

    /** Sólido que se pisa e não se tira: bedrock, construção, baú. */
    FIRM(false, true, false, false),

    /** Areia e cascalho: pisa-se, mas tirar derruba o que está em cima. */
    LOOSE(false, true, false, false),

    /** Água e lava. Não se entra, não se pisa, e não se cava encostado. */
    FLUID(false, false, false, false),

    /** Tudo o mais: laje, cerca, magma, fogo, chunk descarregado. */
    BARRIER(false, false, false, false);

    private final boolean passable;

    private final boolean standable;

    private final boolean diggable;

    private final boolean placeable;

    Cell(boolean passable, boolean standable, boolean diggable, boolean placeable) {
        this.passable = passable;
        this.standable = standable;
        this.diggable = diggable;
        this.placeable = placeable;
    }

    /** Cabe o corpo do aldeão aqui sem mexer em nada. */
    public boolean passable() {
        return passable;
    }

    /** Serve de chão. */
    public boolean standable() {
        return standable;
    }

    /** A picareta pode abrir — ainda sujeito ao líquido em volta. */
    public boolean diggable() {
        return diggable;
    }

    /** Pode receber o bloco que o aldeão põe. */
    public boolean placeable() {
        return placeable;
    }
}
