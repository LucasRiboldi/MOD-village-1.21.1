package com.villagecolony.fabric.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.LotRefusals;
import com.villagecolony.fabric.integration.SupplyGapLedger;
import com.villagecolony.fabric.integration.VillageStyle;
import com.villagecolony.fabric.work.StrandedWorkers;
import com.villagecolony.fabric.work.WorkTime;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * O boletim da vila no bioma — pedido do autor, 2026-10-20: entrar num mundo de um bioma só e varrer
 * os problemas daquele bioma. Ver {@code docs/testing/roteiro-por-bioma.md}.
 *
 * <ul>
 *   <li>{@code VC_BIOME}: a cada 5 minutos, uma linha por colônia atendida — o retrato da vila no
 *       bioma (estilo, nascimento, obras, o que barra o lote, faltas, encalhes, Regra 51);
 *   <li>{@code /vc bioma}: o mesmo retrato no chat, da vila em que o jogador está;
 *   <li>{@code /vc bioma nota <texto>}: a observação do jogador vai para o log ({@code VC_BIOME_NOTE}),
 *       com posição e bioma, e entra no relatório de {@code scripts/biome_report.py}.
 * </ul>
 */
public final class BiomeCheck {

    /** Cinco minutos, o mesmo passo do {@code VC_TIME}. */
    static final long EVERY = 6_000;

    /** O teto da Regra 51: ocioso + bloqueado + encalhado no expediente. */
    static final int RULE_51 = 15;

    private BiomeCheck() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % EVERY == 0) {
                logAll(server.getOverworld());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(CommandManager.literal("vc")
                        .then(CommandManager.literal("bioma")
                                .executes(BiomeCheck::show)
                                .then(CommandManager.literal("nota")
                                        .then(CommandManager.argument("texto", StringArgumentType.greedyString())
                                                .executes(BiomeCheck::note))))));
    }

    /** O retrato de uma colônia no bioma. */
    record Snapshot(String colony, String biome, String style, int beds, int villagers, int employed,
            int buildings, String project, int lotsAccepted, String refusals, int gaps, int stranded,
            String overRule51) {

        String line(String version) {
            return "VC_BIOME version=1 mod=" + version + " colony=" + colony + " biome=" + biome + " style=" + style
                    + " beds=" + beds + " villagers=" + villagers + " employed=" + employed
                    + " buildings=" + buildings + " project=" + project + " lots_ok=" + lotsAccepted
                    + " refusals=" + refusals + " gaps=" + gaps + " stranded=" + stranded
                    + " over51=" + overRule51;
        }
    }

    static void logAll(ServerWorld world) {
        long now = world.getTime();

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (colony.canWork() && colony.isAttended(now)) {
                VillageColonyMod.LOGGER.info(snapshot(world, colony).line(version()));
            }
        }
    }

    static Snapshot snapshot(ServerWorld world, Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        var workers = VillageColonyMod.WORKERS.ofColony(colony.id());
        int employed = (int) workers.stream().filter(worker -> worker.profession().isPresent()).count();
        int stranded = (int) workers.stream().filter(worker -> StrandedWorkers.isStranded(worker.villagerId())).count();

        String project = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id())
                .map(open -> open.blueprint().id().path().replaceAll(".*/", "") + ":" + open.state()
                        + ":" + open.remaining().size())
                .orElse("none");

        List<String> refusals = new ArrayList<>();
        java.util.Arrays.stream(LotRefusals.Reason.values())
                .filter(reason -> LotRefusals.countOf(colony.id(), reason) > 0)
                .sorted(Comparator.comparingInt((LotRefusals.Reason reason) -> -LotRefusals.countOf(colony.id(), reason)))
                .limit(3)
                .forEach(reason -> refusals.add(reason.name() + ":" + LotRefusals.countOf(colony.id(), reason)));

        List<String> over = new ArrayList<>();
        for (Map.Entry<String, Map<WorkTime.State, Integer>> job : WorkTime.lastWindowOf(colony.id()).entrySet()) {
            int share = WorkTime.idleRuleShare(job.getValue());

            if (!"NONE".equals(job.getKey()) && share > RULE_51) {
                over.add(job.getKey() + ":" + share);
            }
        }

        return new Snapshot(colony.id().toString().substring(0, 8),
                pathOf(world.getBiome(center).getKey().map(key -> key.getValue().toString()).orElse("?")),
                pathOf(VillageStyle.biomeAt(world, colony.center()).getValue().toString()),
                colony.observedBeds(), workers.size(), employed,
                VillageColonyMod.BUILDINGS.ofColony(colony.id()).size(), project,
                LotRefusals.acceptedIn(colony.id()),
                refusals.isEmpty() ? "-" : String.join(",", refusals),
                SupplyGapLedger.sessionCountOf(colony.id()), stranded,
                over.isEmpty() ? "-" : String.join(",", over));
    }

    private static int show(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Optional<Colony> colony = colonyOf(source);

        if (colony.isEmpty()) {
            source.sendError(Text.literal("Village Colony: nenhuma vila perto de você neste mundo."));
            return 0;
        }

        Snapshot shot = snapshot(source.getWorld(), colony.get());
        List<String> lines = List.of(
                "Bioma " + shot.biome() + " — estilo da vila " + shot.style(),
                "Camas " + shot.beds() + ", aldeões " + shot.villagers() + " (" + shot.employed() + " com ofício), "
                        + shot.stranded() + " encalhados",
                "Construções " + shot.buildings() + "; obra aberta: " + shot.project(),
                "Lotes aprovados " + shot.lotsAccepted() + "; mais recusas: " + shot.refusals(),
                "Peças que surgiram sem rota nesta sessão: " + shot.gaps(),
                "Acima da Regra 51 (ócio > " + RULE_51 + "%): " + shot.overRule51());

        source.sendFeedback(() -> Text.literal("Village Colony — boletim do bioma").formatted(Formatting.GOLD), false);
        lines.forEach(line -> source.sendFeedback(() -> Text.literal(line).formatted(Formatting.WHITE), false));
        source.sendFeedback(() -> Text.literal("Anote problemas com /vc bioma nota <texto>; depois, "
                + "python scripts/biome_report.py").formatted(Formatting.GRAY), false);
        VillageColonyMod.LOGGER.info(shot.line(version()));

        return 1;
    }

    private static int note(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String text = StringArgumentType.getString(context, "texto").replace('"', '\'');
        BlockPos at = BlockPos.ofFloored(source.getPosition());
        String biome = pathOf(source.getWorld().getBiome(at).getKey().map(key -> key.getValue().toString()).orElse("?"));
        String colony = colonyOf(source).map(found -> found.id().toString().substring(0, 8)).orElse("-");

        VillageColonyMod.LOGGER.info("VC_BIOME_NOTE version=1 mod={} colony={} biome={} x={} y={} z={} text=\"{}\"",
                version(), colony, biome, at.getX(), at.getY(), at.getZ(), text);
        source.sendFeedback(() -> Text.literal("Nota registrada em " + biome + " (" + at.toShortString() + ")")
                .formatted(Formatting.GREEN), false);

        return 1;
    }

    private static Optional<Colony> colonyOf(ServerCommandSource source) {
        if (!(source.getEntity() instanceof ServerPlayerEntity player) || player.getWorld().getRegistryKey() != World.OVERWORLD) {
            return Optional.empty();
        }

        ColonyPos at = MinecraftTypeAdapter.toColonyPos(player.getBlockPos());

        return VillageLogCommand.villageAtPlayerColumn(player, at);
    }

    private static String pathOf(String id) {
        return id.replace("minecraft:", "");
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer("villagecolony")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
