package com.villagecolony.core.storage.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Uma fotografia do estoque físico da colônia, com reserva por ciclo —
 * decisão 4B, 2026-09-24. Ver
 * {@code docs/decisions/ADR-024-physical-warehouse-index.md}.
 *
 * <p><b>Derivado, nunca fonte de verdade.</b> O mundo é quem sabe quanto
 * a colônia tem; este índice é uma leitura de um instante, e
 * {@link #invalidate} existe justamente para que ninguém confunda uma
 * reserva desta fotografia com estoque real depois que ela envelhece.
 *
 * <p><b>Sem estoque virtual.</b> Todo recurso que este índice conhece
 * veio de um baú físico reconhecido; a única exceção documentada é a
 * ADR-022 — uma peça final sintetizada quando nenhuma alternativa tem
 * rota local, e ela entra como qualquer outro depósito físico antes de
 * chegar aqui, não como atalho deste índice.
 *
 * <p><b>Mutável de propósito</b>, como {@code Colony}/{@code Mine}: uma
 * fotografia reconstruída a cada reserva pagaria o mapa inteiro por
 * pedido, dentro do mesmo ciclo em que ela é válida.
 */
public final class WarehouseIndex {

    private final Map<ResourceId, Integer> quantities;

    private final boolean complete;

    private final Map<ResourceId, Integer> reserved = new HashMap<>();

    private WarehouseIndex(Map<ResourceId, Integer> quantities, boolean complete) {
        this.quantities = Map.copyOf(quantities);
        this.complete = complete;
    }

    /** Uma fotografia que cobriu todo o baú reconhecido da colônia. */
    public static WarehouseIndex complete(Map<ResourceId, Integer> quantities) {
        return new WarehouseIndex(quantities, true);
    }

    /**
     * Uma fotografia que não cobriu tudo — algum baú reconhecido estava
     * em chunk descarregado. Toda reserva sobre ela é recusada com
     * {@link SupplyBlockReason#SNAPSHOT_INCOMPLETE}, mesmo que o recurso
     * pedido pareça sobrar no que foi lido: o que faltou ler pode ser
     * exatamente o que decidiria.
     */
    public static WarehouseIndex incomplete(Map<ResourceId, Integer> quantities) {
        return new WarehouseIndex(quantities, false);
    }

    public boolean isComplete() {
        return complete;
    }

    /** Quanto deste recurso a fotografia viu, sem descontar reserva. */
    public int quantityOf(ResourceId resourceId) {
        return quantities.getOrDefault(resourceId, 0);
    }

    /**
     * Reserva este pedido agora, sem olhar prioridade — quem chama
     * primeiro reserva primeiro. Ver {@link #reserveBatch} para a
     * resolução que respeita {@link SupplyPriority}.
     */
    public Reservation reserve(SupplyRequest request) {
        Objects.requireNonNull(request, "request");

        if (!complete) {
            return Reservation.blocked(SupplyBlockReason.SNAPSHOT_INCOMPLETE);
        }

        int available = quantityOf(request.resourceId())
                - reserved.getOrDefault(request.resourceId(), 0);

        if (available < request.amount()) {
            return Reservation.blocked(SupplyBlockReason.INSUFFICIENT_PHYSICAL_STOCK);
        }

        reserved.merge(request.resourceId(), request.amount(), Integer::sum);

        return Reservation.reserved();
    }

    /**
     * Resolve vários pedidos na ordem de {@link SupplyPriority}: uma
     * obra ativa nunca perde estoque escasso para um objetivo de estoque
     * geral, não importa em que ordem os dois chegaram na lista.
     *
     * <p>Empates de prioridade mantêm a ordem em que chegaram na lista —
     * {@code sorted} é estável, e reordenar dentro do mesmo nível não
     * tem regra que o justifique.
     */
    public Map<SupplyRequest, Reservation> reserveBatch(List<SupplyRequest> requests) {
        Objects.requireNonNull(requests, "requests");

        Map<SupplyRequest, Reservation> results = new LinkedHashMap<>();

        requests.stream()
                .sorted(Comparator.comparingInt(request -> request.priority().ordinal()))
                .forEach(request -> results.put(request, reserve(request)));

        return results;
    }

    /**
     * Esquece toda reserva desta fotografia, sem alterar o estoque que
     * ela viu. Chamado quando o ciclo termina ou a fotografia é
     * reconstruída — reserva de um ciclo anterior nunca deve sobreviver
     * para o próximo.
     */
    public void invalidate() {
        reserved.clear();
    }

    /** O resultado de uma tentativa de {@link #reserve}. */
    public record Reservation(SupplyRequestStatus status, Optional<SupplyBlockReason> reason) {

        public Reservation {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(reason, "reason");

            if (status == SupplyRequestStatus.BLOCKED && reason.isEmpty()) {
                throw new IllegalArgumentException("a blocked reservation must carry a reason");
            }

            if (status != SupplyRequestStatus.BLOCKED && reason.isPresent()) {
                throw new IllegalArgumentException("only a blocked reservation carries a reason");
            }
        }

        static Reservation reserved() {
            return new Reservation(SupplyRequestStatus.RESERVED, Optional.empty());
        }

        static Reservation blocked(SupplyBlockReason reason) {
            return new Reservation(SupplyRequestStatus.BLOCKED, Optional.of(reason));
        }
    }
}
