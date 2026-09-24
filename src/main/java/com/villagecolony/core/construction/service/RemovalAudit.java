package com.villagecolony.core.construction.service;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;

import java.util.Objects;
import java.util.Optional;

/**
 * A autorização explícita para tirar uma obra do registro — decisão
 * 10A, 2026-09-24.
 *
 * <p><b>O contrato já existia, só não era verificado.</b> O javadoc de
 * {@link ConstructionService#forget} sempre disse "quem chama é
 * responsável por guardar a caixa dela no registro de construções
 * antes" — e os dois chamadores reais, {@code ConstructionCancellation}
 * e {@code WaitingWork}, já cumpriam isso. Esta classe torna o contrato
 * verificável em compilação, em vez de só documentado: quem quiser
 * remover uma obra precisa construir um motivo, e o motivo precisa
 * concordar com o estado real do projeto.
 *
 * <p><b>Cada motivo autoriza um estado, e só ele.</b> Misturar os dois
 * seria o mesmo erro que {@code readState}/{@code readProfession}
 * existem para prevenir do outro lado — um valor usado fora do que ele
 * significa.
 */
public final class RemovalAudit {

    /** Por que uma obra sai do registro. */
    public enum Reason {
        /** O jogador cancelou com uma Tocha das Almas. Qualquer estado aberto. */
        PLAYER_CANCELLATION,

        /**
         * A colônia desistiu por paciência esgotada — {@code WaitingWork}.
         * Em {@code WAITING_RESOURCES} (esperou material demais) ou em
         * {@code BUILDING} (tinha tudo e não andou — o "fundo de poço"
         * de 2026-09-19, {@code givesUpIfItIsNotMoving}).
         */
        PATIENCE_ABANDONMENT,

        /** A obra terminou e vira infraestrutura. Só em {@code COMPLETED}. */
        COMPLETED_PROJECT_PURGE
    }

    private final Reason reason;

    private RemovalAudit(Reason reason) {
        this.reason = reason;
    }

    /** O jogador cancelou a obra com uma Tocha das Almas. */
    public static RemovalAudit playerCancellation() {
        return new RemovalAudit(Reason.PLAYER_CANCELLATION);
    }

    /** A colônia desistiu de esperar material. */
    public static RemovalAudit patienceAbandonment() {
        return new RemovalAudit(Reason.PATIENCE_ABANDONMENT);
    }

    /** A obra terminou. */
    public static RemovalAudit completedProjectPurge() {
        return new RemovalAudit(Reason.COMPLETED_PROJECT_PURGE);
    }

    /**
     * Nenhuma auditoria — o valor padrão de quem não construiu uma.
     *
     * <p>Nunca autoriza nada: {@link #allows} recusa sempre que o motivo
     * é ausente, mesmo com um projeto que existe e está aberto. É o
     * caso que {@code forgetRejectsActiveOrUnauditedProject} nomeia.
     */
    public static RemovalAudit absent() {
        return new RemovalAudit(null);
    }

    /**
     * Se este motivo autoriza remover este projeto.
     *
     * <p>Projeto ausente (id que não existe mais no registro) nunca é
     * autorizado — não há o que auditar contra um estado que não existe.
     */
    public boolean allows(Optional<ConstructionProject> project) {
        Objects.requireNonNull(project, "project");

        if (reason == null || project.isEmpty()) {
            return false;
        }

        ConstructionState state = project.get().state();

        return switch (reason) {
            case PLAYER_CANCELLATION -> state.isOpen();
            case PATIENCE_ABANDONMENT -> state == ConstructionState.WAITING_RESOURCES
                    || state == ConstructionState.BUILDING;
            case COMPLETED_PROJECT_PURGE -> state == ConstructionState.COMPLETED;
        };
    }

    public Optional<Reason> reason() {
        return Optional.ofNullable(reason);
    }
}
