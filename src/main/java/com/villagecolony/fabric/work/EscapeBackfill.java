package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A escada da fuga é tampada depois que ele sai — N10, 2026-09-24.
 *
 * <p><b>Decisão do autor:</b> <i>"tampar a escada"</i>. A fuga do E47 abre
 * um túnel em degraus no terreno natural, e ele ficava aberto: uma
 * cicatriz no barranco e, pior, um buraco novo para outro aldeão cair — a
 * mesma armadilha de terreno que prendeu o primeiro.
 *
 * <p><b>Com o que saiu dele.</b> Cada vão cavado guarda o item que a
 * picareta tirou dali — pedregulho da pedra, terra da terra, areia da
 * areia. O tampão tira esse item do baú do trabalhador e o assenta de
 * volta. Se o item não está mais no baú, aquele vão fica aberto: o mod não
 * cria bloco para tapar buraco.
 *
 * <p><b>Um bloco por passagem</b>, do fundo para cima, como quem joga a
 * terra de volta com a pá. E nunca onde há alguém: vão ocupado por
 * criatura fica aberto, para não enterrar ninguém.
 */
public final class EscapeBackfill {

    static {
        ServerMemory.register(EscapeBackfill.class, EscapeBackfill::clearAll);
    }

    /** Um vão cavado e o item com que ele se tampa. */
    private record Hole(BlockPos at, Item refill) {
    }

    /** Um tampão em andamento: os vãos que faltam e o baú de onde sai o material. */
    private record Job(UUID workerId, Deque<Hole> holes, ColonyPos chest) {
    }

    private static final Map<UUID, List<Hole>> DUG = new HashMap<>();

    private static final List<Job> PENDING = new ArrayList<>();

    private EscapeBackfill() {
    }

    /** Guarda o vão que acabou de sair, e com que item ele volta. */
    static void dug(UUID workerId, BlockPos at, List<net.minecraft.item.ItemStack> drops) {
        Optional<Item> refill = drops.stream()
                .map(net.minecraft.item.ItemStack::getItem)
                .filter(item -> item instanceof BlockItem)
                .findFirst();

        refill.ifPresent(item -> DUG.computeIfAbsent(workerId, id -> new ArrayList<>())
                .add(new Hole(at.toImmutable(), item)));
    }

    /**
     * Ele saiu: começa o tampão, do fundo para cima.
     *
     * <p>Público para o teste de jogo, que cava sem um aldeão de verdade.
     */
    public static void begin(UUID workerId, ColonyPos chest) {
        List<Hole> holes = DUG.remove(workerId);

        if (holes == null || holes.isEmpty() || chest == null) {
            return;
        }

        Deque<Hole> order = new ArrayDeque<>();

        holes.stream()
                .sorted((a, b) -> Integer.compare(a.at().getY(), b.at().getY()))
                .forEach(order::addLast);

        PENDING.add(new Job(workerId, order, chest));
    }

    /** Um bloco por tampão em andamento. Chamado a cada passagem da fuga. */
    public static void tick(ServerWorld world) {
        Iterator<Job> jobs = PENDING.iterator();

        while (jobs.hasNext()) {
            Job job = jobs.next();
            Hole hole = job.holes().pollFirst();

            if (hole != null) {
                fill(world, job, hole);
            }

            if (job.holes().isEmpty()) {
                jobs.remove();

                VillageColonyMod.LOGGER.info(
                        "Stranded worker {} finished backfilling its escape stairs",
                        job.workerId().toString().substring(0, 8));
            }
        }
    }

    private static void fill(ServerWorld world, Job job, Hole hole) {
        BlockState now = world.getBlockState(hole.at());

        if (!now.isAir()) {
            // Alguém já pôs algo aqui — o jogador, a água, a areia que caiu.
            return;
        }

        if (!world.getEntitiesByClass(LivingEntity.class, new Box(hole.at()), LivingEntity::isAlive)
                .isEmpty()) {
            return;
        }

        if (ChestWithdrawer.withdraw(world, job.chest(), hole.refill(), 1) == 0) {
            return;
        }

        world.setBlockState(
                hole.at(), Block.getBlockFromItem(hole.refill()).getDefaultState(), Block.NOTIFY_ALL);
    }

    /** Esquece um trabalhador que saiu do registro. */
    static void forget(UUID workerId) {
        DUG.remove(workerId);
        PENDING.removeIf(job -> job.workerId().equals(workerId));
    }

    /** Esquece tudo. Chamado ao abrir e ao parar o servidor. */
    public static void clearAll() {
        DUG.clear();
        PENDING.clear();
    }
}
