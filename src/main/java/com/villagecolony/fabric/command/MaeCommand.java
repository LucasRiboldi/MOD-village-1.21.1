package com.villagecolony.fabric.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.MaeSites;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;
import java.util.Optional;

/**
 * Disparo supervisionado de uma obra MAE — ADR-046, escolha do autor de 2026-11-04.
 *
 * <ul>
 *   <li>{@code /vc mae obra <modelo>}: pegada centrada no jogador, rotação pela direção do olhar;</li>
 *   <li>{@code /vc mae obra <modelo> <rotação>}: a mesma, com a rotação 0–3 escolhida;</li>
 *   <li>{@code /vc mae obra <modelo> busca}: lote livre e nivelado perto do jogador;</li>
 *   <li>{@code /vc mae obra <modelo> fora}: lote fora da vila original, perto do jogador;</li>
 *   <li>{@code /vc mae soltar}: entrega à vila as regiões paradas (Regra 187);</li>
 *   <li>{@code /vc mae estado} e {@code /vc mae parar}: obras em andamento e interrupção;</li>
 *   <li>{@code /vc mae mostrar ligar|desligar}: o sinalizador dos caminhos guardados ({@code RoadMapSync});</li>
 *   <li>{@code /vc mae reler}: relê o caminho da vila agora ({@code RoadGraphRefresh}).</li>
 * </ul>
 *
 * <p>Operador apenas (nível 2). Reserva, terreno, material e provas de acesso seguem o writer; o comando
 * não contorna recusa nenhuma.
 */
public final class MaeCommand {
    /** Modelos já exercitados pelo fluxo inteiro; outros ids valem, e a leitura MAE decide. */
    private static final List<String> EXERCISED = List.of(
            "minecraft:village/plains/houses/plains_small_house_1",
            "minecraft:village/plains/houses/plains_masons_house_1",
            "minecraft:village/plains/houses/plains_animal_pen_1",
            "villagecolony:colony/storage_small");

    private static final SuggestionProvider<ServerCommandSource> MODELS =
            (context, builder) -> CommandSource.suggestMatching(EXERCISED, builder);

    private MaeCommand() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
                dispatcher.register(CommandManager.literal("vc")
                        .then(CommandManager.literal("mae")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("obra")
                                        .then(CommandManager.argument("modelo", IdentifierArgumentType.identifier())
                                                .suggests(MODELS)
                                                .executes(context -> build(context, null, Search.HERE))
                                                .then(CommandManager.literal("busca")
                                                        .executes(context -> build(context, null, Search.NEAR)))
                                                .then(CommandManager.literal("fora")
                                                        .executes(context -> build(context, null, Search.OUTSIDE)))
                                                .then(CommandManager.argument("rotacao", IntegerArgumentType.integer(0, 3))
                                                        .executes(context -> build(context,
                                                                IntegerArgumentType.getInteger(context, "rotacao"), Search.HERE)))))
                                .then(CommandManager.literal("estado").executes(MaeCommand::state))
                                .then(CommandManager.literal("parar").executes(MaeCommand::stop))
                                .then(CommandManager.literal("soltar").executes(MaeCommand::release))
                                .then(CommandManager.literal("reler").executes(MaeCommand::reread))
                                .then(CommandManager.literal("mostrar")
                                        .then(CommandManager.literal("ligar").executes(context -> show(context, true)))
                                        .then(CommandManager.literal("desligar")
                                                .executes(context -> show(context, false)))))));
    }

    private enum Search { HERE, NEAR, OUTSIDE }

    private static int build(CommandContext<ServerCommandSource> context, Integer rotation, Search search) {
        ServerCommandSource source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("A obra MAE é pedida por um jogador, na posição dele."));
            return 0;
        }
        if (!player.getServerWorld().getRegistryKey().equals(World.OVERWORLD)) {
            source.sendError(Text.literal("A obra MAE só vale no mundo principal."));
            return 0;
        }
        Optional<Colony> colony = VillageLogCommand.villageAtPlayerColumn(player,
                new ColonyPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
        if (colony.isEmpty()) {
            source.sendError(Text.literal("Nenhuma vila ativa perto de você."));
            return 0;
        }
        var model = MinecraftTypeAdapter.toResourceId(IdentifierArgumentType.getIdentifier(context, "modelo"));
        int quarterTurns = rotation != null ? rotation : facing(player);
        var world = player.getServerWorld();
        BlockPos feet = player.getBlockPos();
        try {
            var status = switch (search) {
                case NEAR -> MaeSites.startNear(world, colony.get().id(), model, feet, quarterTurns, player.getUuid());
                case OUTSIDE -> MaeSites.startOutside(world, colony.get().id(), model, feet, quarterTurns, player.getUuid());
                case HERE -> MaeSites.start(world, colony.get().id(), model,
                        MaeSites.centered(world, model, quarterTurns, feet), quarterTurns, player.getUuid());
            };
            source.sendFeedback(() -> MaeSites.message(status), true);
            return 1;
        } catch (IllegalStateException | IllegalArgumentException refused) {
            String why = refused.getMessage() == null ? "invalid_payload" : refused.getMessage();

            source.sendError(Text.literal("Obra MAE recusada: " + why + (why.endsWith("original_structure")
                    ? " — o MAE não edita a vila original; use \"fora\", perto da borda da vila" : "")));
            return 0;
        }
    }

    /** Norte 0, leste 1, sul 2, oeste 3: a obra gira junto com o olhar do jogador. */
    private static int facing(ServerPlayerEntity player) {
        return switch (player.getHorizontalFacing()) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static int release(CommandContext<ServerCommandSource> context) {
        int[] counts = MaeSites.releaseIdle(context.getSource().getWorld());

        context.getSource().sendFeedback(() -> Text.literal(counts[0] + " região(ões) MAE entregue(s) à vila"
                + (counts[1] > 0 ? "; " + counts[1] + " presa(s) por escrita sem confirmação" : "") + ".")
                .formatted(Formatting.YELLOW), false);

        return counts[0];
    }

    private static int reread(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        for (Colony colony : com.villagecolony.VillageColonyMod.COLONIES.all()) {
            com.villagecolony.fabric.work.RoadGraphRefresh.requestNow(colony.id());
        }

        source.sendFeedback(() -> Text.literal("Grafo de caminhos relido na próxima passada.").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int show(CommandContext<ServerCommandSource> context, boolean on) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("O sinalizador é de um jogador."));
            return 0;
        }

        com.villagecolony.fabric.overlay.RoadMapSync.set(player, on);
        source.sendFeedback(() -> Text.literal(on
                ? "Sinalizador ligado: linha amarela, nó rosa, entrada branca, nó central vermelho, ponta azul."
                : "Sinalizador desligado.").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int state(CommandContext<ServerCommandSource> context) {
        var active = MaeSites.active();
        if (active.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Nenhuma obra MAE em andamento.")
                    .formatted(Formatting.GRAY), false);
        }
        for (var status : active) context.getSource().sendFeedback(() -> MaeSites.message(status), false);
        return active.size();
    }

    private static int stop(CommandContext<ServerCommandSource> context) {
        int stopped = 0;
        for (var status : MaeSites.active()) if (MaeSites.stop(status.regionId())) stopped++;
        int count = stopped;
        context.getSource().sendFeedback(() -> Text.literal(count + " obra(s) MAE parada(s); jobs aprovados ficam pausados.")
                .formatted(Formatting.YELLOW), true);
        return count;
    }
}
