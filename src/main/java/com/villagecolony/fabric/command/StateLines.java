package com.villagecolony.fabric.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O retrato da vila ({@code ColonyState}) em linhas para o chat — {@code /vc bioma}, {@code /vc log} e
 * {@code /vc dados} mostram o mesmo registro que vai para o arquivo de análise (pedido do autor, 2026-10-28).
 * Puro: recebe o JSON, devolve o texto.
 */
final class StateLines {

    private StateLines() {
    }

    /** O retrato inteiro, uma linha por assunto. */
    static List<String> of(JsonObject state) {
        List<String> lines = new ArrayList<>();
        JsonObject people = object(state, "people");
        JsonObject stock = object(state, "stock");
        JsonObject build = object(state, "build");
        JsonObject mine = object(state, "mine");
        JsonObject village = object(state, "village");

        lines.add("Dia " + number(state, "day") + " (" + clock(number(state, "daytime")) + "), servidor a "
                + text(state, "mspt") + " ms por tique");
        lines.add("Aldeões " + number(people, "villagers") + " (" + number(people, "employed") + " com ofício), camas "
                + number(people, "beds") + ", encalhados " + number(people, "stranded"));
        lines.add("Ofícios: " + pairs(object(people, "trades")));
        lines.add("Agora: " + badges(object(people, "badges"))
                + (people.has("stuck") && people.getAsJsonArray("stuck").size() > 0
                        ? " — travados: " + people.getAsJsonArray("stuck") : ""));
        lines.add("Baús " + number(stock, "chests") + (stock.has("fill") ? ", " + number(stock, "fill") + "% cheios" : "")
                + (stock.has("needsStorage") && stock.get("needsStorage").getAsBoolean() ? " — pede armazém" : "")
                + "; estoque " + groups(stock));
        lines.add("Obras prontas " + number(build, "buildings") + "; aberta: "
                + (build.has("open") ? text(build, "open") + " (" + text(build, "state") + ", faltam "
                        + number(build, "left") + ", de lado " + number(build, "deferred") + ")" : "nenhuma")
                + "; lotes aceitos " + number(build, "lotsOk"));
        lines.add("Tarefas abertas: " + pairs(object(object(state, "tasks"), "open")) + " ("
                + number(object(state, "tasks"), "held") + " com dono)");
        lines.add(mine.has("entryY") ? "Mina: nível em y=" + number(mine, "entryY") + ", " + number(mine, "levels")
                + " nível(is) abaixo da boca, " + number(mine, "cut") + " posições cavadas" : "Mina: nenhuma");
        lines.add("Ruas " + number(village, "roads") + " colunas; bancadas: "
                + (village.has("benches") && village.getAsJsonArray("benches").size() > 0
                        ? village.getAsJsonArray("benches").toString().replace("\"", "") : "nenhuma")
                + "; animais " + pairs(object(village, "animals")));

        return lines;
    }

    private static String badges(JsonObject badges) {
        Map<String, String> names = Map.of("DUTY", "no ofício", "HELP", "ajudando", "STUCK", "travados",
                "IDLE", "à toa", "OFF", "fora do expediente");

        return badges.entrySet().stream()
                .map(entry -> entry.getValue().getAsInt() + " " + names.getOrDefault(entry.getKey(), entry.getKey()))
                .collect(Collectors.joining(", "));
    }

    private static String groups(JsonObject stock) {
        return stock.entrySet().stream()
                .filter(entry -> entry.getKey().equals(entry.getKey().toUpperCase(java.util.Locale.ROOT))
                        && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber())
                .map(entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT) + " " + entry.getValue().getAsInt())
                .collect(Collectors.joining(", "));
    }

    private static String pairs(JsonObject counts) {
        return counts.size() == 0 ? "-" : counts.entrySet().stream()
                .map(entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT) + " " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    static String clock(long daytime) {
        long hours = (daytime / 1000 + 6) % 24;
        long minutes = (daytime % 1000) * 60 / 1000;

        return String.format(java.util.Locale.ROOT, "%02d:%02d", hours, minutes);
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement child = parent.get(key);

        return child != null && child.isJsonObject() ? child.getAsJsonObject() : new JsonObject();
    }

    private static long number(JsonObject parent, String key) {
        JsonElement child = parent.get(key);

        return child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isNumber() ? child.getAsLong() : 0;
    }

    private static String text(JsonObject parent, String key) {
        JsonElement child = parent.get(key);

        return child == null ? "?" : child.getAsString();
    }
}
