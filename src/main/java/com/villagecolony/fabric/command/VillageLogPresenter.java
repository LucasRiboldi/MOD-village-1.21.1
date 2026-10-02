package com.villagecolony.fabric.command;

import com.villagecolony.core.construction.model.ConstructionPriority;
import com.villagecolony.core.telemetry.model.ActivityProfession;
import com.villagecolony.core.telemetry.model.ActivityState;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.telemetry.model.ActivityTraceEvent;
import com.villagecolony.core.telemetry.model.ControlledReason;
import com.villagecolony.core.telemetry.model.TargetKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converte o traço técnico da simulação em mensagens curtas para o jogador. */
final class VillageLogPresenter {

    private VillageLogPresenter() {
    }

    static List<String> entries(ActivityTrace trace, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }

        List<String> entries = new ArrayList<>();
        Map<ActivityProfession, ActivityTraceEvent> current = new LinkedHashMap<>();
        Map<ActivityProfession, ActivityTraceEvent> blockers = new LinkedHashMap<>();

        for (ActivityTraceEvent event : trace.newestFirst(ActivityTrace.CAPACITY)) {
            current.putIfAbsent(event.profession(), event);

            if (event.state() == ActivityState.ABANDONED
                    || event.state() == ActivityState.ERROR) {
                blockers.putIfAbsent(event.profession(), event);
            }
        }

        for (ActivityTraceEvent event : current.values()) {
            entries.add(describe(event));
            if (entries.size() == limit) {
                return List.copyOf(entries);
            }
        }

        for (Map.Entry<ActivityProfession, ActivityTraceEvent> entry : blockers.entrySet()) {
            ActivityTraceEvent latest = current.get(entry.getKey());

            if (entry.getValue().equals(latest)) {
                continue;
            }

            entries.add("[ÚLTIMO BLOQUEIO] " + profession(entry.getKey()) + ": "
                    + problemReason(entry.getValue().reason()) + ".");
            if (entries.size() == limit) {
                return List.copyOf(entries);
            }
        }

        if (entries.isEmpty()) {
            return List.of("Ainda não há atividades registradas nesta vila.");
        }

        return List.copyOf(entries);
    }

    /** Explica a prioridade atual da próxima obra sem expor a regra técnica. */
    static String constructionPriority(ConstructionPriority priority, int adults, int beds) {
        return switch (priority) {
            case HOUSING_DEFICIT -> "Próxima obra: moradia; faltam "
                    + (adults - beds) + " camas para os moradores.";
            case WORKSHOP -> "Próxima obra: a casa de um ofício que ainda não tem a sua.";
            case FIRST_HOUSE -> "Próxima obra: a primeira moradia da vila.";
            case ROTATION_NON_RESIDENTIAL ->
                    "Próxima obra: infraestrutura; as camas já atendem os moradores.";
            case ROTATION_HOUSE -> "Próxima obra: moradia; o rodízio voltou para casas.";
        };
    }

    private static String describe(ActivityTraceEvent event) {
        String profession = profession(event.profession());

        return switch (event.state()) {
            case IDLE -> "[ATIVO] " + profession + ": trabalhando em " + targetAction(event.target()) + ".";
            case WAITING -> "[AGUARDANDO] " + profession + ": "
                    + waitingReason(event.reason(), event.target()) + ".";
            case RECOVERED -> "[ATIVO] " + profession + ": "
                    + recoveredReason(event.target()) + ".";
            case ABANDONED, ERROR -> "[TRAVADO] " + profession + ": " + problemReason(event.reason()) + ".";
            case UNKNOWN -> "[ATENÇÃO] " + profession + ": o estado da atividade não foi reconhecido.";
        };
    }

    private static String profession(ActivityProfession profession) {
        return switch (profession) {
            case MINER -> "Mineiro";
            case LUMBERJACK -> "Lenhador";
            case MASON -> "Pedreiro";
            case SMELTER -> "Fundidor";
            case CARPENTER -> "Carpinteiro";
            case FARMER -> "Fazendeiro";
            case SHEPHERD -> "Pastor";
            case BUILDER -> "Construtor";
            case UNKNOWN -> "Profissional";
        };
    }

    private static String waitingReason(ControlledReason reason, TargetKind target) {
        if (reason == ControlledReason.WORK_STALLED && target == TargetKind.NONE) {
            return "preso no terreno; abrindo uma saida segura";
        }

        return switch (reason) {
            case NO_TARGET -> "procurando " + targetName(target) + " para " + targetVerb(target);
            case MISSING_MATERIAL -> "esperando material para continuar";
            case NO_STORAGE, STORAGE_FULL -> "esperando espaço livre em um baú";
            case NO_WORKER, NO_EXECUTOR -> "esperando um profissional disponível";
            case NO_TASK -> "esperando uma nova atividade";
            case SWEEP_INCOMPLETE -> "terminando de procurar uma área segura";
            default -> "esperando para continuar";
        };
    }

    private static String problemReason(ControlledReason reason) {
        return switch (reason) {
            case WORK_STALLED -> "a atividade parou por falta de progresso; a tarefa volta para a fila"
                    + " e ele descansa antes de tentar de novo";
            case STORAGE_FULL -> "não há espaço livre nos baús";
            case MISSING_MATERIAL -> "faltou material para concluir a atividade";
            default -> "a atividade precisou ser interrompida";
        };
    }

    private static String recoveredReason(TargetKind target) {
        if (target == TargetKind.NONE) {
            return "saiu do ponto preso e voltou a escala";
        }

        return "voltou a trabalhar em " + targetAction(target);
    }

    private static String targetAction(TargetKind target) {
        return switch (target) {
            case STONE -> "minerar pedra";
            case WOOD -> "cuidar de madeira";
            case CROP -> "cuidar da plantação";
            case ANIMAL -> "cuidar dos animais";
            case ORE -> "minerar minério";
            case CONSTRUCTION -> "construir";
            case STORAGE -> "organizar os baús";
            case MINE -> "trabalhar na mina";
            case NONE, UNKNOWN -> "sua atividade";
        };
    }

    private static String targetName(TargetKind target) {
        return switch (target) {
            case STONE -> "pedra";
            case WOOD -> "madeira";
            case CROP -> "plantação";
            case ANIMAL -> "animais";
            case ORE -> "minério";
            case CONSTRUCTION -> "uma obra";
            case STORAGE -> "um baú";
            case MINE -> "a mina";
            case NONE, UNKNOWN -> "um novo alvo";
        };
    }

    private static String targetVerb(TargetKind target) {
        return switch (target) {
            case STONE, ORE -> "minerar";
            case WOOD -> "coletar";
            case CROP -> "cuidar";
            case ANIMAL -> "atender";
            case CONSTRUCTION -> "continuar a obra";
            case STORAGE -> "organizar";
            case MINE -> "trabalhar";
            case NONE, UNKNOWN -> "continuar";
        };
    }
}
