package com.villagecolony.core.construction.model;

/**
 * Em que pé está uma obra.
 *
 * <p>Os cinco estados de Construction-System.md, na ordem em que
 * acontecem. Só se anda para a frente: uma obra que voltasse de BUILDING
 * para PLANNED teria blocos já colocados e um plano que os ignora.
 *
 * <p>A exceção é {@link #WAITING_RESOURCES}, que se alcança de
 * {@link #PREPARING} e de onde se volta a {@link #BUILDING} quando o
 * material aparece. Faltar material no meio da obra é normal — o baú é
 * do jogador tanto quanto da colônia.
 */
public enum ConstructionState {

    /** A colônia decidiu construir. Nenhum bloco foi alterado. */
    PLANNED,

    /** O local está sendo limpo do que é natural. */
    PREPARING,

    /** Falta material. A colônia pede o que falta e espera. */
    WAITING_RESOURCES,

    /** O construtor está pondo blocos. */
    BUILDING,

    /** Todos os blocos foram postos. Vira infraestrutura da colônia. */
    COMPLETED;

    /**
     * Se daqui se pode ir para lá.
     *
     * <p>A regra vive no enum porque é dele: quem a espalhasse pelos
     * chamadores acabaria com dois caminhos discordando, e um deles
     * deixaria uma obra parada num estado de que ninguém a tira.
     */
    public boolean canBecome(ConstructionState next) {
        return switch (this) {
            case PLANNED -> next == PREPARING;
            case PREPARING -> next == WAITING_RESOURCES || next == BUILDING;
            case WAITING_RESOURCES -> next == BUILDING;
            case BUILDING -> next == WAITING_RESOURCES || next == COMPLETED;
            case COMPLETED -> false;
        };
    }

    /** Se a obra ainda vai mexer no mundo. */
    public boolean isOpen() {
        return this != COMPLETED;
    }
}
