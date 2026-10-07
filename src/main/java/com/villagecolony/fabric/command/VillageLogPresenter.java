package com.villagecolony.fabric.command;

import com.villagecolony.core.colony.model.VillageHappiness;
import com.villagecolony.core.construction.model.ConstructionPriority;
import com.villagecolony.core.construction.model.MaterialRequest;
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

    /** Quanto tempo uma espera precisa ter para aparecer no /vc log: dois minutos. */
    static final long LONG_WAIT_MILLIS = 120_000;

    /** Quantas esperas o /vc log mostra. */
    static final int WAIT_LIMIT = 6;

    /**
     * O que a obra aberta espera agora, e por quê — ADR-035 §3.
     *
     * @param building o nome da planta, como {@code village/plains/houses/small_house_1}
     * @param nowTick o tique atual do mundo
     */
    static String materialRequest(String building, MaterialRequest request, long nowTick) {
        long seconds = request.waitingTicks(nowTick) / 20;
        String elapsed = seconds >= 120 ? (seconds / 60) + " min" : seconds + " s";
        String state = switch (request.state()) {
            case RESOLVING -> "em resolução";
            case DELIVERED -> "entregue";
            case NO_SOLUTION -> "sem solução";
        };

        return "[OBRA] " + building.substring(building.lastIndexOf('/') + 1) + " pede "
                + request.material().path() + " — " + state + ": " + request.reason() + " — há " + elapsed;
    }

    /**
     * As esperas de mais de dois minutos, com motivo e tempo — B-5, 2026-10-02:
     * <i>"[ESPERANDO] Fundidor: nada no raio inteiro (sand) — há 12 min"</i>.
     */
    static List<String> longWaits(List<com.villagecolony.fabric.work.IdleLog.Waiting> waits, long nowMillis) {
        List<String> lines = new ArrayList<>();

        for (com.villagecolony.fabric.work.IdleLog.Waiting wait : waits) {
            long minutes = (nowMillis - wait.sinceMillis()) / 60_000;

            if (nowMillis - wait.sinceMillis() < LONG_WAIT_MILLIS || lines.size() >= WAIT_LIMIT) {
                continue;
            }

            lines.add("[ESPERANDO] " + subjectName(wait.subject()) + ": " + idleReason(wait.reason())
                    + (wait.detail().isBlank() ? "" : " (" + wait.detail() + ")") + " — há " + minutes + " min");
        }

        return List.copyOf(lines);
    }

    private static String subjectName(String subject) {
        return switch (subject) {
            case "miner", "miner mine mouth", "miner mouth chest", "miner branch", "miner cut",
                    "miner surface stone", "miner sand" -> "Mineiro";
            case "lumberjack" -> "Lenhador";
            case "farmer" -> "Fazendeiro";
            case "shepherd" -> "Pastor";
            case "smelter", "surface gathering" -> "Fundidor";
            case "building" -> "Obra";
            case "carpenter" -> "Carpinteiro";
            case "mason" -> "Pedreiro";
            default -> subject;
        };
    }

    private static String idleReason(com.villagecolony.core.coordination.IdleReason reason) {
        return switch (reason) {
            case NO_TASK -> "sem tarefa aberta";
            case NO_EXECUTOR -> "tarefa aberta sem ninguém que a pegue";
            case ALREADY_OPEN -> "já há uma aberta";
            case NO_WORKER -> "ninguém na vila sabe fazer";
            case NO_STORAGE -> "o trabalhador não tem baú";
            case STORAGE_FULL -> "o baú está cheio";
            case NO_TARGET -> "nada a fazer no raio inteiro";
            case SWEEP_INCOMPLETE -> "ainda procurando";
            case NOT_IN_GAME -> "o jogo não tem o que se pede";
            case MISSING_MATERIAL -> "falta o material na colônia";
        };
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

    /** A felicidade da última reunião — ADR-036 item 20. */
    static String happiness(VillageHappiness happiness) {
        String mood = switch (happiness.mood()) {
            case HAPPY -> "feliz (mais filhos)";
            case CONTENT -> "satisfeita";
            case UNHAPPY -> "infeliz (nenhum filho novo)";
        };

        return "Felicidade: " + mood + " — " + happiness.foodPerAdult() + " de comida por adulto, "
                + happiness.spareBeds() + " camas sobrando, " + happiness.finishedBuildings() + " obras concluídas.";
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
