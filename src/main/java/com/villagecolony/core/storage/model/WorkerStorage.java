package com.villagecolony.core.storage.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.UUID;

/**
 * O baú de um trabalhador.
 *
 * <p>O recurso pertence à colônia, mas fica fisicamente guardado no baú
 * de quem o produziu. Ver Storage-System.md §"Princípio Fundamental".
 *
 * <p>Registro, não criação: o mod não constrói casas nem baús. O baú já
 * tem de existir na casa do aldeão, e aqui só se anota onde ele está.
 * Ver Storage-System.md §"Criação do Baú".
 *
 * <p>Imutável. Um trabalhador que troque de baú recebe outro registro —
 * mudar a posição em silêncio esconderia que o baú anterior sumiu.
 *
 * <p>O trabalhador é referenciado por id, e não por objeto: um domínio
 * do Core não importa outro. Ver ADR-006 §6. É também por isso que
 * {@code ownerProfession}, previsto em Storage-System.md §"Associação
 * Trabalhador → Baú", não está aqui: a profissão é do trabalhador, e
 * copiá-la criaria uma segunda verdade que envelheceria na primeira
 * realocação.
 */
public final class WorkerStorage {

    private final UUID workerId;

    private final ColonyPos chestPosition;

    private WorkerStorage(UUID workerId, ColonyPos chestPosition) {
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.chestPosition = Objects.requireNonNull(chestPosition, "chestPosition");
    }

    public static WorkerStorage of(UUID workerId, ColonyPos chestPosition) {
        return new WorkerStorage(workerId, chestPosition);
    }

    /** Id do trabalhador dono do baú. É o {@code villagerId} do Vanilla. */
    public UUID workerId() {
        return workerId;
    }

    public ColonyPos chestPosition() {
        return chestPosition;
    }

    public boolean isAt(ColonyPos position) {
        return chestPosition.equals(position);
    }

    /** Um trabalhador tem um baú; o baú é identificado por seu dono. */
    @Override
    public boolean equals(Object other) {
        return other instanceof WorkerStorage storage && workerId.equals(storage.workerId);
    }

    @Override
    public int hashCode() {
        return workerId.hashCode();
    }

    @Override
    public String toString() {
        return "WorkerStorage[worker=" + workerId + ", chest=" + chestPosition + "]";
    }
}
