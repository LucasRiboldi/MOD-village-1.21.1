package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.Objects;
import java.util.Optional;

/**
 * O resultado de uma tentativa de construção.
 *
 * <p>Uma peça sem apoio não é uma peça colocada. Ela pode liberar o
 * construtor sem sumir da planta, para que o mundo decida quando a
 * tentativa deve voltar a existir.
 */
public sealed interface ConstructionOutcome
        permits ConstructionOutcome.Placed, ConstructionOutcome.Skipped, ConstructionOutcome.Blocked {

    /** Quantas peças físicas foram assentadas nesta tentativa. */
    int placedCount();

    /** Se esta tentativa encerra a tarefa de construção do trabalhador. */
    boolean releasesProjectSlot();

    /** O motivo observável, quando a tentativa não colocou uma peça. */
    Optional<SkipReason> reason();

    static Placed placed() {
        return new Placed();
    }

    static Skipped skipped(ColonyPos position, SkipReason reason) {
        return new Skipped(position, reason);
    }

    static Blocked blocked(SkipReason reason) {
        return new Blocked(reason);
    }

    /** Uma peça foi realmente colocada no mundo. */
    record Placed() implements ConstructionOutcome {
        @Override
        public int placedCount() {
            return 1;
        }

        @Override
        public boolean releasesProjectSlot() {
            return false;
        }

        @Override
        public Optional<SkipReason> reason() {
            return Optional.empty();
        }
    }

    /** Uma peça continua pendente porque o mundo ainda não a suporta. */
    record Skipped(ColonyPos position, SkipReason skipReason) implements ConstructionOutcome {
        public Skipped {
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(skipReason, "skipReason");
        }

        @Override
        public int placedCount() {
            return 0;
        }

        @Override
        public boolean releasesProjectSlot() {
            return true;
        }

        @Override
        public Optional<SkipReason> reason() {
            return Optional.of(skipReason);
        }
    }

    /** A construção está bloqueada, mas não há uma peça parcial a registrar. */
    record Blocked(SkipReason skipReason) implements ConstructionOutcome {
        public Blocked {
            Objects.requireNonNull(skipReason, "skipReason");
        }

        @Override
        public int placedCount() {
            return 0;
        }

        @Override
        public boolean releasesProjectSlot() {
            return true;
        }

        @Override
        public Optional<SkipReason> reason() {
            return Optional.of(skipReason);
        }
    }
}
