package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SiteOutline;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * O contorno do lote, desenhado no mundo — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"adicionar um marcador visual, um efeito
 * que demonstre onde no terreno está o espaço alocado para a construção
 * escolhida"</i>. Três sessões seguidas ele entrou no jogo, não viu casa
 * nascendo, e o log dizia que a colônia planejava — sem marcador, <i>"não
 * achou lote"</i> e <i>"achou lote a setenta blocos"</i> são a mesma coisa
 * de dentro do jogo.
 *
 * <p><b>Fica no servidor</b>, e é o que torna isto barato e seguro:
 * {@code ServerWorld.spawnParticles} manda o pacote para quem está por
 * perto, e o cliente não precisa saber nada do mod. Nenhum entrypoint de
 * cliente, nenhum networking próprio, nenhuma travessia de thread — que é
 * a armadilha que este projeto já pagou duas vezes (§11).
 *
 * <p><b>A cor diz o estado</b>, e é o diagnóstico que o autor pediu de
 * fato: chama para a obra que está sendo construída, fumaça para a que
 * espera material. Quem olha o lote sabe se a casa está andando ou parada
 * sem abrir o log.
 */
public final class SiteMarker {

    /**
     * De quantos em quantos tiques o contorno pisca.
     *
     * <p>Vinte é uma vez por segundo. Partícula é pacote de rede por
     * posição: um lote 7x7 tem 24 colunas de borda, e desenhar a cada
     * tique seriam 480 pacotes por segundo por obra. Uma vez por segundo
     * o contorno fica visível e contínuo ao olho — as partículas vivem
     * mais que um tique — e custa 24.
     */
    private static final int EVERY_TICKS = 20;

    /**
     * A que distância o contorno é desenhado.
     *
     * <p>Mesma régua da vila. Mais longe que isto o jogador não vê
     * partícula de qualquer jeito, e desenhar seria pagar rede por nada.
     */
    private static final int WITHIN = 64;

    private static int tickCounter;

    private SiteMarker() {
    }

    /**
     * Desenha o contorno das obras abertas, uma vez por segundo.
     *
     * <p>Chamada do tique do servidor. Sai de graça na esmagadora maioria
     * dos tiques — é um contador e uma comparação.
     */
    public static void tick(ServerWorld world) {
        if (++tickCounter < EVERY_TICKS) {
            return;
        }

        tickCounter = 0;

        if (world.getPlayers().isEmpty()) {
            // Ninguém para ver. Partícula sem plateia é pacote jogado fora.
            return;
        }

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.state().isOpen()) {
                continue;
            }

            draw(world, project);
        }
    }

    /** O contorno de uma obra. */
    private static void draw(ServerWorld world, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        ColonyPos far = new ColonyPos(
                origin.x() + size.x() - 1,
                origin.y(),
                origin.z() + size.z() - 1);

        List<ColonyPos> border = SiteOutline.of(origin, far);

        if (noPlayerNear(world, origin)) {
            return;
        }

        // Chama para a obra que anda, fumaça para a que espera material —
        // ver o javadoc da classe. O estado é lido uma vez, fora do laço.
        boolean waiting = project.state() == ConstructionState.WAITING_RESOURCES;

        for (ColonyPos at : border) {
            BlockPos pos = MinecraftTypeAdapter.toBlockPos(at);

            // Meio bloco acima do piso: no piso a partícula nasce dentro
            // do chão e o jogador não a vê.
            world.spawnParticles(
                    waiting ? ParticleTypes.SMOKE : ParticleTypes.FLAME,
                    pos.getX() + 0.5,
                    pos.getY() + 1.1,
                    pos.getZ() + 0.5,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
        }
    }

    /**
     * Se não há jogador perto o bastante para ver.
     *
     * <p>Horizontal, como todo raio de vila neste projeto: o jogador no
     * fundo da mina continua sendo o jogador daquela vila.
     */
    private static boolean noPlayerNear(ServerWorld world, ColonyPos at) {
        return world.getPlayers().stream().noneMatch(player -> {
            long dx = (long) player.getBlockX() - at.x();
            long dz = (long) player.getBlockZ() - at.z();

            return dx * dx + dz * dz <= (long) WITHIN * WITHIN;
        });
    }

    /** Esquece o contador. Chamado ao parar o servidor. */
    public static void clearAll() {
        tickCounter = 0;
    }
}
