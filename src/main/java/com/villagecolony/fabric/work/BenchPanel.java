package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.integration.BenchPresence;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * O painel da bancada — Regra 132 (autor, 2026-10-27): shift+clique numa bancada do mod mostra no chat
 * os titulares daquele ofício na vila, o estado de cada um com a cor do nome (Regra 129), a tarefa, há
 * quanto tempo esperam (Regra 128), o que falta à obra (construtor) e as passivas que a bancada liga.
 */
public final class BenchPanel {

    private BenchPanel() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!player.isSneaking() || !(world instanceof ServerWorld server)) {
                return ActionResult.PASS;
            }

            BlockPos at = hit.getBlockPos();
            Optional<ProfessionType> profession = BenchPresence.professionOf(world.getBlockState(at).getBlock());

            if (profession.isEmpty()) {
                return ActionResult.PASS;
            }

            lines(server, at, profession.get()).forEach(line -> player.sendMessage(line, false));

            return ActionResult.SUCCESS;
        });
    }

    /** As linhas do painel desta bancada. */
    static List<Text> lines(ServerWorld world, BlockPos at, ProfessionType profession) {
        String trade = profession.name().toLowerCase(Locale.ROOT);
        Text name = Text.translatable("text.villagecolony.profession." + trade);
        Optional<Colony> colony = BenchPresence.colonyAt(at);

        if (colony.isEmpty()) {
            return List.of(Text.translatable("text.villagecolony.panel.no_colony", name).formatted(Formatting.GRAY));
        }

        List<Worker> holders = VillageColonyMod.WORKERS.ofColony(colony.get().id()).stream()
                .filter(worker -> worker.profession().filter(profession::equals).isPresent())
                .toList();
        List<Text> lines = new java.util.ArrayList<>();

        lines.add(Text.translatable("text.villagecolony.panel.header", name, holders.size()).formatted(Formatting.GOLD));

        for (Worker worker : holders) {
            lines.add(workerLine(worker));
        }

        if (profession == ProfessionType.BUILDER) {
            VillageColonyMod.CONSTRUCTIONS.openOf(colony.get().id()).ifPresent(project -> lines.add(
                    Text.translatable("text.villagecolony.panel.build", project.blueprint().id().path(),
                            project.remainingMaterials().values().stream().mapToInt(Integer::intValue).sum())
                            .formatted(Formatting.GRAY)));
        }

        lines.add(Text.translatable("text.villagecolony.passives." + trade).formatted(Formatting.DARK_AQUA));

        return lines;
    }

    private static Text workerLine(Worker worker) {
        WorkTime.Badge badge = WorkTime.badgeOf(worker.villagerId());
        List<Task> tasks = VillageColonyMod.TASKS.assignedTo(worker.villagerId());
        MutableText line = Text.literal("• " + worker.villagerId().toString().substring(0, 8) + " ")
                .append(Text.translatable("text.villagecolony.badge." + badge.name().toLowerCase(Locale.ROOT))
                        .formatted(colorOf(badge)));

        if (!tasks.isEmpty()) {
            Task task = tasks.getFirst();
            line.append(Text.literal(" · " + task.type() + " " + task.targetResource() + " ×" + task.amount())
                    .formatted(Formatting.WHITE));
        }

        int waiting = IdleWatch.waitingOf(worker.villagerId());

        if (waiting > 0) {
            line.append(Text.translatable("text.villagecolony.panel.waiting", waiting, IdleWatch.WAIT_LIMIT)
                    .formatted(Formatting.YELLOW));
        }

        return line;
    }

    static Formatting colorOf(WorkTime.Badge badge) {
        return switch (badge) {
            case DUTY -> Formatting.GOLD;
            case HELP -> Formatting.AQUA;
            case STUCK -> Formatting.RED;
            case IDLE -> Formatting.WHITE;
            case OFF -> Formatting.GRAY;
        };
    }
}
