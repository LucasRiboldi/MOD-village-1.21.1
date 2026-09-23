package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.Locale;
import java.util.Objects;

/**
 * Registra transições de trabalho em formato estável para análise offline.
 *
 * <p>As linhas não carregam UUID, coordenada, nome de bloco ou texto livre.
 * O {@code analyze_village_log.py} consolida somente os quatro campos de
 * classificação, sem transformar o log do jogador em estado da colônia.</p>
 */
public final class ActivityLog {

    private ActivityLog() {
    }

    /** Registra que uma atividade entrou em espera. */
    public static void waiting(String subject, IdleReason reason) {
        record(professionFor(subject), activityFor(subject), "WAITING", reason.name());
    }

    /** Registra que uma atividade anteriormente observada voltou a progredir. */
    public static void recovered(String subject, IdleReason reason) {
        record(professionFor(subject), activityFor(subject), "RECOVERED", reason.name());
    }

    /** Registra que o detector de travamento abandonou a atividade do trabalhador. */
    public static void abandoned(ProfessionType profession, String activity) {
        Objects.requireNonNull(profession, "profession");
        record(profession.name(), normalized(activity), "ABANDONED", "WORK_STALLED");
    }

    /** Registra a falha operacional que levou ao abandono controlado da tarefa. */
    public static void failed(ProfessionType profession, String activity) {
        Objects.requireNonNull(profession, "profession");
        record(profession.name(), normalized(activity), "ERROR", "WORK_STALLED");
    }

    private static void record(String profession, String activity, String outcome, String reason) {
        VillageColonyMod.LOGGER.info(
                "VC_ACTIVITY version=1 profession={} activity={} outcome={} reason={}",
                profession, activity, outcome, reason);
    }

    private static String professionFor(String subject) {
        String value = normalized(subject);
        if (value.contains("MIN") || value.contains("SURFACE") || value.contains("COLLECT")) {
            return ProfessionType.MINER.name();
        }
        if (value.contains("LUMBER") || value.contains("TREE")) {
            return ProfessionType.LUMBERJACK.name();
        }
        if (value.contains("MASON")) {
            return ProfessionType.MASON.name();
        }
        if (value.contains("SMELT")) {
            return ProfessionType.SMELTER.name();
        }
        if (value.contains("CARPENTER")) {
            return ProfessionType.CARPENTER.name();
        }
        if (value.contains("FARM")) {
            return ProfessionType.FARMER.name();
        }
        if (value.contains("SHEPHERD")) {
            return ProfessionType.SHEPHERD.name();
        }
        if (value.contains("BUILD")) {
            return ProfessionType.BUILDER.name();
        }
        return "COLONY";
    }

    private static String activityFor(String subject) {
        String value = normalized(subject);
        if (value.contains("MIN") || value.contains("SURFACE") || value.contains("COLLECT")) {
            return "MINING";
        }
        if (value.contains("LUMBER") || value.contains("TREE")) {
            return "HARVESTING";
        }
        if (value.contains("MASON") || value.contains("CARPENTER")) {
            return "CRAFTING";
        }
        if (value.contains("SMELT")) {
            return "SMELTING";
        }
        if (value.contains("FARM")) {
            return "FARMING";
        }
        if (value.contains("SHEPHERD")) {
            return "SHEPHERDING";
        }
        if (value.contains("BUILD")) {
            return "BUILDING";
        }
        return "COORDINATION";
    }

    private static String normalized(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }
}
