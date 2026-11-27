package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.EditLedger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * O degrau de fuga some — Regra 377 (autor 26-11, notas do jogo da 0.4.145, J4 🅱️: "depois de um dia os blocos postos
 * com o objetivo desta escadaria devem sumir, eles são usados como utilitário, mas não pode estragar o cenário"). O
 * pedregulho que o desvio põe ({@link DetourWalker}) sai depois de {@value #LIFETIME} tiques, um dia de jogo, se ainda
 * for pedregulho e ninguém estiver em cima ou dentro dele; o bloco que o jogador trocou fica. A lista vive na memória
 * do servidor: o degrau de antes de fechar o mundo fica.
 */
public final class TemporarySteps {

    /** Um dia de jogo. */
    static final long LIFETIME = 24_000;

    /** Uma varredura a cada dez segundos. */
    static final int EVERY = 200;

    /** O degrau posto → o tique em que foi posto. */
    private static final Map<BlockPos, Long> PLACED = new HashMap<>();

    static {
        ServerMemory.register(TemporarySteps.class, PLACED::clear);
    }

    private TemporarySteps() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();

            if (world.getTime() % EVERY == EVERY / 5 && !PLACED.isEmpty()) {
                sweep(world, world.getTime());
            }
        });
    }

    /** O desvio pôs este degrau agora. */
    static void placed(BlockPos at, long now) {
        PLACED.put(at.toImmutable(), now);
    }

    /** Tira os degraus de mais de um dia; diz quantos saíram. */
    static int sweep(ServerWorld world, long now) {
        int removed = 0;

        for (Iterator<Map.Entry<BlockPos, Long>> it = PLACED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> step = it.next();
            BlockPos at = step.getKey();

            if (now - step.getValue() <= LIFETIME || !world.isChunkLoaded(at)) {
                continue;
            }

            if (!world.getBlockState(at).isOf(Blocks.COBBLESTONE)) {
                it.remove();
                continue;
            }

            // Alguém em cima ou dentro: espera a próxima varredura, para não derrubar ninguém.
            if (!world.getEntitiesByClass(LivingEntity.class, new Box(at).stretch(0, 2, 0), entity -> true).isEmpty()) {
                continue;
            }

            world.setBlockState(at, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            EditLedger.mod(world, at, Blocks.AIR.getDefaultState(), "degrau_de_fuga"); // Regra 345
            it.remove();
            removed++;
        }

        if (removed > 0) {
            VillageColonyMod.LOGGER.info("VC_ESCAPE_STEPS removed={} left={} rule=377", removed, PLACED.size());
        }

        return removed;
    }

    static void forget(BlockPos at) {
        PLACED.remove(at);
    }
}
