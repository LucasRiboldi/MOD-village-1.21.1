package com.villagecolony.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.work.BlockLedger;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;

/**
 * {@code /vc blocos} — Regra 342: a tabela do registro de blocos da vila em volta do jogador, agora. Os mais movimentados
 * no dia primeiro (posto pela obra, esperado, surgido), e em vermelho o que está abaixo da reserva.
 */
public final class BlockCommand {

    /** Quantas linhas cabem no chat. */
    static final int LINES = 15;

    private BlockCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("vc")
                .then(CommandManager.literal("blocos").executes(BlockCommand::show)));
    }

    private static int show(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
            source.sendError(Text.literal("Este comando só pode ser usado por um jogador."));
            return 0;
        }

        Optional<Colony> colony = VillageLogCommand.villageAtPlayerColumn(player,
                new ColonyPos(player.getBlockX(), player.getBlockY(), player.getBlockZ()));

        if (colony.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Village Colony: nenhuma vila ativa está perto de você.")
                    .formatted(Formatting.YELLOW), false);
            return 1;
        }

        List<BlockLedger.Row> rows = BlockLedger.rows(player.getServerWorld(), colony.get());

        source.sendFeedback(() -> Text.literal("Blocos da vila (" + rows.size() + "): classe, reserva, estoque,"
                + " posto, esperou, surgiu").formatted(Formatting.AQUA, Formatting.BOLD), false);

        for (BlockLedger.Row row : rows.subList(0, Math.min(LINES, rows.size()))) {
            String line = row.key() + "  " + row.stockClass() + "  reserva " + row.reserve() + "  estoque "
                    + row.stock() + "  posto " + row.used() + "  esperou " + row.waited() + "  surgiu " + row.surged();

            source.sendFeedback(() -> Text.literal(line).formatted(
                    row.stock() < row.reserve() ? Formatting.RED : Formatting.GRAY), false);
        }

        return 1;
    }
}
