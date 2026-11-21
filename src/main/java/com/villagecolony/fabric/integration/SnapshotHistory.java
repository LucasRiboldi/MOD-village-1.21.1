package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Regra 346: linhas do log da colônia desde que o servidor abriu este mundo. */
public final class SnapshotHistory {

    private static final Map<UUID, SnapshotHistory> SESSION = new HashMap<>();

    static {
        ServerMemory.register(SnapshotHistory.class, SESSION::clear);
    }

    private String road = "Nenhum VC_ROAD_GRAPH registrado nesta sessão.";
    private final List<String> windows = new ArrayList<>();
    private final List<String> events = new ArrayList<>();

    private SnapshotHistory() {
    }

    public static void info(UUID colony, String pattern, Object... values) {
        String line = MessageFormatter.arrayFormat(pattern, values).getMessage();
        VillageColonyMod.LOGGER.info(line);
        remember(colony, line);
    }

    public static void warn(UUID colony, String pattern, Object... values) {
        String line = MessageFormatter.arrayFormat(pattern, values).getMessage();
        VillageColonyMod.LOGGER.warn(line);
        remember(colony, line);
    }

    public static void remember(UUID colony, String line) {
        SnapshotHistory history = SESSION.computeIfAbsent(colony, ignored -> new SnapshotHistory());
        if (line.startsWith("VC_ROAD_GRAPH ")) {
            history.road = line;
        } else if (line.startsWith("VC_TIME ")) {
            history.windows.add(line);
        } else {
            history.events.add(line);
        }
    }

    public static void append(UUID colony, Optional<String> partialWindow, StringBuilder text) {
        SnapshotHistory history = SESSION.getOrDefault(colony, new SnapshotHistory());
        text.append("== último VC_ROAD_GRAPH\n").append(history.road).append("\n\n");
        text.append("== VC_TIME da sessão (janelas completas e parcial atual)\n");
        history.windows.forEach(line -> text.append(line).append('\n'));
        partialWindow.ifPresent(line -> text.append(line).append(" partial=true\n"));
        text.append("\n== mortes, laços, suprimento e edições da sessão\n");
        history.events.forEach(line -> text.append(line).append('\n'));
        text.append('\n');
    }
}
