package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.TreeHarvester;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * As árvores que já têm dono.
 *
 * <p>Saiu de {@code LumberjackWork} em 2026-08-20, quando ele passou de
 * mil e duzentas linhas. É a coordenação entre lenhadores, e nada mais:
 * dois deles indo ao mesmo tronco é trabalho perdido e um deles
 * chegando a um bloco que já caiu.
 *
 * <p>Reserva-se a <b>árvore inteira</b> quando ela é escolhida, e não
 * bloco a bloco enquanto cai. A diferença importa: reservar aos poucos
 * deixaria o segundo lenhador escolher a mesma árvore pelo topo, que a
 * busca ainda não teria marcado.
 *
 * <p>Separado de {@link TreeMarks} de propósito, e a diferença é de
 * natureza. A reserva é sobre <b>agora</b> e sai quando a árvore acaba;
 * a marca é sobre o <b>mundo</b> e vence pelo relógio.
 */
public final class TreeClaims {

    private TreeClaims() {
    }

    /** Solta todas as reservas. Chamado ao parar o servidor. */
    public static void clearAll() {
        CLAIMED.clear();
    }

    /** Se este tronco já é de alguém. */
    static boolean isTaken(BlockPos log) {
        return CLAIMED.contains(log);
    }

    /**
     * Os troncos que já têm dono.
     *
     * <p>A colônia passou a abrir uma tarefa por lenhador, e a busca por
     * árvore parte sempre do centro e é determinística: sem isto, todos
     * receberiam a mesma árvore, um só a derrubaria e os outros ficariam
     * em volta de um toco.
     *
     * <p>Guarda os troncos do plano, e não a árvore como um ponto: a
     * busca devolve tronco, então comparar tronco com tronco é exato e
     * custa uma consulta de conjunto. As folhas ficam de fora porque a
     * busca nunca as devolve.
     */
    private static final Set<BlockPos> CLAIMED = new HashSet<>();

    /**
     * Reserva os troncos desta árvore para quem vai derrubá-la.
     *
     * <p>Só o tronco. A busca por árvore nunca devolve folha, então
     * reservar a copa não impediria colisão nenhuma e faria o conjunto
     * crescer sete vezes à toa.
     */
    static void claim(TreeHarvester.Plan plan) {
        for (int i = 0; i < plan.logs(); i++) {
            CLAIMED.add(plan.blocks().get(i));
        }
    }

    static void unclaim(TreeHarvester.Plan plan) {
        if (plan == null) {
            return;
        }

        for (int i = 0; i < plan.logs(); i++) {
            CLAIMED.remove(plan.blocks().get(i));
        }
    }

}
