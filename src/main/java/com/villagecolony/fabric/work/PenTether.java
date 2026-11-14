package com.villagecolony.fabric.work;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import org.jspecify.annotations.Nullable;
import net.minecraft.util.math.BlockPos;

/**
 * A amarra do portão — Regra 284: enquanto atravessa o portão do curral, o animal fica preso a {@link #RANGE} blocos do
 * lugar de dentro. É o mesmo laço que a corda do jogo usa ({@code setPositionTarget}): o passeio e o pânico do animal só
 * escolhem destino dentro dele, e não o levam para longe do portão. A amarra solta quando ele entra, sai ou o pastor
 * desiste.
 */
final class PenTether {

    /** O raio da amarra, em blocos. */
    static final int RANGE = 2;

    private PenTether() {
    }

    /** Prende o animal a este lugar, se ainda não está preso a ele. */
    static void hold(AnimalEntity animal, BlockPos at) {
        if (!animal.hasPositionTarget() || !animal.getPositionTarget().equals(at)) {
            animal.setPositionTarget(at, RANGE);
        }
    }

    /** Solta a amarra do portão; não faz nada se não é um animal (já sumiu do mundo, por exemplo). */
    static void release(@Nullable Entity entity) {
        if (entity instanceof AnimalEntity animal) {
            animal.clearPositionTarget();
        }
    }
}
