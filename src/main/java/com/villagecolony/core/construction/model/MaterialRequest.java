package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

/**
 * O que uma obra espera agora, de onde a colônia tenta tirar e desde quando —
 * ADR-035 §3, fase 1.
 *
 * <p>Antes, a pergunta "esta obra tem a próxima peça?" devolvia só sim ou não,
 * e o motivo se perdia em cada ramo do suprimento. Esta fase só <b>registra</b>
 * a decisão que os caminhos de hoje já tomam; não muda nenhuma. Decidir pela
 * cadeia de fontes é a fase 2, que espera playtest.
 *
 * @param material a peça que a obra pede
 * @param state em que pé o pedido está
 * @param source de onde a colônia tenta tirar a peça
 * @param sinceTick o tique em que o pedido entrou nesta situação
 */
public record MaterialRequest(ResourceId material, State state, Source source, long sinceTick) {

    /** Em que pé o pedido está. */
    public enum State {

        /** Alguém da colônia pode trazer a peça; ela ainda não chegou. */
        RESOLVING,

        /** A peça está ao alcance da obra. */
        DELIVERED,

        /** Nenhum caminho de hoje vai trazer a peça. */
        NO_SOLUTION
    }

    /** De onde a colônia tenta tirar a peça. */
    public enum Source {

        /** Os baús da colônia (Regra 45). */
        CHEST,

        /** Uma profissão que obtém a peça no bioma. */
        PROFESSION,

        /** Um artesão, com os ingredientes sem rota postos no baú dele. */
        CRAFTSMAN,

        /** Aparece no baú da profissão depois de quatro tentativas (ADR-036 item 6). */
        STOCKED,

        /** Nenhuma fonte: a peça não tem item conhecido. */
        NONE
    }

    public static MaterialRequest start(ResourceId material, State state, Source source, long tick) {
        return new MaterialRequest(material, state, source, tick);
    }

    /** A obra pode seguir com a peça. */
    public boolean delivered() {
        return state == State.DELIVERED;
    }

    /**
     * O pedido depois de mais uma consulta. A mesma situação mantém o tique de
     * início, para que o tempo de espera conte desde a primeira vez.
     */
    public MaterialRequest updatedTo(ResourceId material, State state, Source source, long tick) {
        if (this.material.equals(material) && this.state == state && this.source == source) {
            return this;
        }

        return new MaterialRequest(material, state, source, tick);
    }

    public long waitingTicks(long now) {
        return Math.max(0, now - sinceTick);
    }

    /** O motivo em uma frase, para o {@code /vc log}. */
    public String reason() {
        return switch (state) {
            case DELIVERED -> source == Source.STOCKED
                    ? "apareceu no baú depois de quatro tentativas sem rota"
                    : "está no baú da colônia";
            case RESOLVING -> switch (source) {
                case CRAFTSMAN -> "os ingredientes estão no baú do artesão; falta ele fabricar";
                case STOCKED -> "sem rota no bioma; aparece no baú na quarta tentativa";
                default -> "há rota no bioma e ainda não chegou ao baú — a cadeia diz qual elo falta";
            };
            case NO_SOLUTION -> switch (source) {
                case STOCKED -> "sem rota, e o baú do construtor não aceitou a peça";
                default -> "a peça não tem item que o jogo conheça";
            };
        };
    }
}
