package com.villagecolony.fabric.work;

import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * O mineiro espera a areia assentar antes de voltar a cavar — 2026-09-19.
 *
 * <p><b>O pedido do autor, e o que ele conserta.</b> Areia e cascalho caem.
 * Quando o mineiro tira o bloco de baixo, a coluna inteira desce — e por
 * alguns tiques aquele espaço não é nem pedra nem ar: é
 * {@code FallingBlockEntity}, uma entidade no meio do caminho. Cavar
 * durante a queda é cavar no escuro:
 *
 * <pre>
 * o alvo escolhido já não existe    ele caiu um bloco abaixo
 * o buraco se reenche sozinho       a areia de cima ocupa o que se abriu
 * o mineiro bate no ar              e a picareta não rende nada
 * </pre>
 *
 * <p>No deserto isso é a regra, não a exceção: a mina inteira é cavada sob
 * duna. É o mesmo tipo de defeito que a mina já conhece da água — o
 * líquido corre por tique e o {@code MineFlooding} o tapa antes de sair de
 * perto —, e a resposta aqui é a simétrica: <b>parar, deixar cair, e então
 * olhar de novo</b>.
 *
 * <p><b>Por que esperar em vez de escorar.</b> Escorar gastaria bloco da
 * colônia para segurar uma duna que vai cair de qualquer jeito, e a areia
 * que desce <b>é minério que ele queria</b> — ela chega sozinha até a mão
 * dele. Esperar é de graça e ainda entrega o material.
 *
 * <p><b>Por que a entidade, e não o bloco.</b> Perguntar ao mundo se há
 * areia acima responde <i>"vai cair"</i>; perguntar pelas entidades
 * responde <i>"está caindo agora"</i>, que é a pergunta certa. Quem sabe é
 * o jogo — ADR-009: o {@code FallingBlockEntity} existe exatamente entre o
 * bloco sair e o bloco pousar, e some sozinho quando assenta. Não há
 * contador para acertar nem tabela de blocos que caem para manter.
 */
public final class MineSettling {

    /**
     * O raio, em blocos, em volta do alvo onde uma queda interessa.
     *
     * <p>Três porque a areia não cai só na vertical: ela escorrega para os
     * lados ao pousar, e uma coluna vizinha desabando ocupa o alvo tanto
     * quanto a de cima. Mais que isso pararia o mineiro por causa de uma
     * queda do outro lado da galeria, que não é problema dele.
     */
    private static final int AROUND = 3;

    /**
     * Quantos tiques ele espera antes de desistir e cavar assim mesmo.
     *
     * <p><b>Guarda contra a espera eterna</b>, e ela é necessária: areia
     * caindo sobre água faz entidade que não assenta no lugar, e um
     * gerador de areia de jogador cairia para sempre. Cem tiques são cinco
     * segundos — tempo de sobra para uma duna assentar, e curto demais
     * para a colônia notar.
     *
     * <p>É a mesma escolha do {@code PatienceClock}: a espera é do
     * trabalhador, nunca da vila.
     */
    public static final int PATIENCE = 100;

    private MineSettling() {
    }

    /**
     * Se ainda há bloco caindo em volta deste alvo.
     *
     * <p>Devolver {@code true} significa <b>não cave neste tique</b>.
     */
    public static boolean stillFalling(ServerWorld world, BlockPos target) {
        Box around = new Box(target).expand(AROUND);

        return !world.getEntitiesByType(EntityType.FALLING_BLOCK, around, entity -> true)
                .isEmpty();
    }

    /**
     * O que o mineiro faz neste tique: esperar, ou seguir.
     *
     * <p>A conta de paciência é do chamador — ela mora no {@code Job}, que
     * é quem sobrevive entre tiques.
     *
     * @return {@code true} se ele deve <b>parar</b> e esperar mais um tique
     */
    public static boolean waits(ServerWorld world, BlockPos target, int waited) {
        if (waited >= PATIENCE) {
            // Esperou demais: cava assim mesmo. Uma queda infinita não
            // pode aposentar o mineiro.
            return false;
        }

        return stillFalling(world, target);
    }
}
