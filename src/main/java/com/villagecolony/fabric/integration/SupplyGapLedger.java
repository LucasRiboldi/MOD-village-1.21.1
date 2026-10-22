package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Toda peça que apareceu num baú sem uma profissão a ter obtido — pedido do autor, 2026-10-20 (G2):
 * <i>"guardar os erros desta função sempre para no futuro todos itens terem uma rota própria de
 * serem adquiridos"</i>.
 *
 * <p>Uma linha JSON por caso em {@code <pasta do jogo>/villagecolony/supply-gaps.jsonl}, fora de
 * {@code logs/} de propósito: o arquivo só cresce, atravessa sessões, versões e biomas, e limpar os
 * logs não o apaga. {@code scripts/supply_gaps.py} o resume como a lista de itens que ainda pedem
 * rota própria.
 */
public final class SupplyGapLedger {

    /** Sem rota no bioma: nenhuma profissão obtém a peça aqui. */
    public static final String NO_ROUTE = "NO_ROUTE";

    /** A rota existe no papel, mas nenhuma tarefa aberta a executa. */
    public static final String NO_TASK = "NO_TASK";

    /** A rota tem tarefa e não avançou: nem chegou ao baú, nem a obra assentou. */
    public static final String STALLED = "STALLED";

    /** As buscas no mundo não acharam a peça (regra das 4 buscas). */
    public static final String NOT_FOUND = "NOT_FOUND";

    private static boolean failed;

    static {
        com.villagecolony.core.type.ServerMemory.register(SupplyGapLedger.class, SupplyGapLedger::clearAll);
    }

    /** Quantas peças surgiram em cada colônia nesta sessão — o boletim do bioma conta. */
    private static final java.util.Map<UUID, Integer> SESSION = new java.util.HashMap<>();

    private SupplyGapLedger() {
    }

    /**
     * A falta de suprimento — ADR-036 item 6: a linha {@code VC_SUPPLY_ERROR} no log, para o
     * playtest contar, e o caso no registro permanente.
     */
    public static void report(ServerWorld world, UUID colonyId, Item item, int count,
            ProfessionType profession, int attempts, String reason, String why) {
        VillageColonyMod.LOGGER.warn("VC_SUPPLY_ERROR version=2 piece={} profession={} attempts={} gap={} reason={}",
                Registries.ITEM.getId(item), profession, attempts, reason, why);
        record(world, colonyId, item, count, profession, reason, why);
        Supplies.record(com.villagecolony.core.coordination.SupplyException.MISSING_PIECE, colonyId,
                Registries.ITEM.getId(item).getPath(), count);
    }

    /** Grava um caso. Falha de disco vira um aviso, uma vez: a obra não pode parar por isso. */
    public static void record(ServerWorld world, UUID colonyId, Item item, int count,
            ProfessionType profession, String reason, String detail) {
        SESSION.merge(colonyId, 1, Integer::sum);
        Path file = world.getServer().getRunDirectory().resolve("villagecolony").resolve("supply-gaps.jsonl");
        String line = "{\"time\":\"" + OffsetDateTime.now(ZoneId.systemDefault()) + "\""
                + ",\"version\":\"" + version() + "\""
                + ",\"colony\":\"" + colonyId + "\""
                + ",\"style\":\"" + styleOf(world, colonyId) + "\""
                + ",\"item\":\"" + Registries.ITEM.getId(item) + "\""
                + ",\"count\":" + count
                + ",\"profession\":\"" + profession + "\""
                + ",\"reason\":\"" + reason + "\""
                + ",\"detail\":\"" + detail.replace("\"", "'") + "\"}\n";

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            if (!failed) {
                failed = true;
                VillageColonyMod.LOGGER.warn("The supply gap ledger is off — cannot write {}", file, failure);
            }
        }
    }

    /** Quantas peças surgiram nesta colônia nesta sessão. */
    public static int sessionCountOf(UUID colonyId) {
        return SESSION.getOrDefault(colonyId, 0);
    }

    static void clearAll() {
        SESSION.clear();
    }

    private static String styleOf(ServerWorld world, UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> VillageStyle.biomeAt(world, colony.center()).getValue().toString())
                .orElse("?");
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("villagecolony")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
