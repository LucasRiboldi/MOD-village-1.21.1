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
 * <p><b>Escorar antes, esperar depois</b> — pedido do autor, 2026-10-20.
 * Antes de quebrar, o bloco que cai logo acima do alvo é compactado no
 * lugar ({@link #shoreUp}): a areia vira arenito, e a coluna inteira fica
 * apoiada. Não gasta bloco da colônia — é a mesma areia, endurecida —, e a
 * frente não se fecha atrás dele. Na coleta de areia não escora: ali a
 * areia que desce é o que ele quer. O que não se compacta (areia suspeita,
 * pó de concreto) segue caindo, e a espera abaixo continua valendo.
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
     * Compacta o bloco que cai logo acima do alvo, antes de o alvo sair — pedido do autor,
     * 2026-10-20.
     *
     * <p>O bloco que cai só despenca no tique agendado depois de perder o apoio; trocado no mesmo
     * tique, ele nunca solta, e o resto da coluna fica em cima do arenito. Não toca o que é da
     * vila gerada, da colônia ou do jogador ({@code BlockProtection.mayBreak}).
     *
     * @return se escorou
     */
    public static boolean shoreUp(ServerWorld world, BlockPos target) {
        BlockPos above = target.up();
        net.minecraft.block.BlockState state = world.getBlockState(above);
        java.util.Optional<net.minecraft.block.Block> firm = com.villagecolony.fabric.world.MineFill.compacted(state);

        if (firm.isEmpty() || !com.villagecolony.fabric.integration.BlockProtection.mayBreak(world, above, state)) {
            return false;
        }

        world.setBlockState(above, firm.get().getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS);

        return true;
    }

    /** Quantos blocos acima do alvo o mineiro olha, pelo ar, procurando o que cairia nele. */
    static final int LOOSE_REACH = 8;

    /**
     * Se o alvo segura, acima dele e pelo ar, um bloco que cai, água ou lava — o mineiro não o quebra
     * (autor, 2026-10-29). O bloco que cai logo acima não conta: ele é escorado antes de o alvo sair
     * ({@link #shoreUp}); o que não se compacta conta.
     */
    public static boolean looseAbove(ServerWorld world, BlockPos target) {
        for (int dy = 1; dy <= LOOSE_REACH; dy++) {
            BlockPos at = target.up(dy);
            net.minecraft.block.BlockState state = world.getBlockState(at);

            if (!state.getFluidState().isEmpty()) {
                return true;
            }

            if (state.getBlock() instanceof net.minecraft.block.FallingBlock) {
                return dy > 1 || com.villagecolony.fabric.world.MineFill.compacted(state).isEmpty()
                        || !com.villagecolony.fabric.integration.BlockProtection.mayBreak(world, at, state);
            }

            if (!state.isAir() && !state.isReplaceable()) {
                return false;
            }
        }

        return false;
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
