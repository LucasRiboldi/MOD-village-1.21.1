package com.villagecolony.fabric.work;

import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineLighting;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * O vocabulário da rocha: o que a picareta abre e o que ela deixa em paz.
 *
 * <p><b>Saiu do {@code MineDigging} em 2026-09-10</b>, e a razão é a
 * dívida que o encerramento do ciclo mediu: aquele arquivo tinha
 * <b>1.403 linhas</b>, o pior do projeto, contra o limite de 500 que
 * este repositório se impôs.
 *
 * <p><b>Refatoração pura</b>: nenhum destes quatro métodos mudou de
 * corpo. O que mudou foi a casa e a visibilidade — eram privados de um
 * arquivo que fazia oito coisas, e passam a ser a resposta pública de um
 * arquivo que faz uma.
 *
 * <p><b>Por que estes quatro andam juntos, e não é arrumação.</b> Eles
 * respondem a mesma pergunta em quatro alturas — <i>o que há neste
 * bloco?</i> — e já eram obrigados a concordar entre si: o
 * {@code nextCut} pula o que {@link #isOpenSpace} diz estar aberto, e o
 * {@code isStillClosed} recua o cursor até onde ele diz que a rocha
 * começa. Em 2026-09-05 os dois lados existiam como <b>cópias da mesma
 * lista</b>, a tentativa de proteger a escada do jogador mexeu num só, e
 * a mina emudeceu. Manter a lista em um lugar é o conserto daquele dia;
 * este arquivo é o lugar.
 */
public final class MineRock {

    private MineRock() {
    }

    /**
     * Se a picareta não tem o que fazer nesta posição da ordem —
     * 2026-09-05.
     *
     * <p><b>Uma pergunta, dois donos.</b> O {@link #nextCut} pula o que
     * já está aberto e o {@link #isStillClosed} recua o cursor até onde
     * a rocha começa, e <b>os dois têm de concordar</b>: posição que a
     * escolha do alvo pula não pode ser a fronteira, senão o cursor
     * recua até ela toda passagem. Eles concordavam por cópia — a mesma
     * lista escrita duas vezes —, e foi por aí que a tentativa de
     * 2026-09-05 de proteger a escada do jogador saiu pela culatra:
     * mexeu num lado só e a mina emudeceu. Agora a lista é uma.
     *
     * <p><b>E o que o jogador constrói entra aqui</b> — pedido do autor:
     * <i>"corrigir a escada que o player constrói, ou qualquer caminho
     * que o próprio player cria dentro da mina"</i>. O caminho dele é
     * <b>espaço aberto</b>, e não obstáculo: a mina passa por ele como
     * passa pelo corredor que ela mesma cavou. Tratá-lo como bloqueio
     * faria a galeria virar depois de alguns degraus, e o corredor do
     * jogador acabaria fechando o ramal.
     *
     * <p>É a mesma porta por onde a tocha da própria mina já passava
     * desde 08-28, e pelo mesmo motivo.
     */
    public static boolean isOpenSpace(ServerWorld world, BlockPos at, BlockState state) {
        return state.isAir()
                || !state.getFluidState().isEmpty()
                || MineLighting.isLight(world, at, state)
                || !isRock(world, at, state);
    }

    /**
     * Se este bloco é rocha, e não coisa que alguém pôs ali.
     *
     * <p><b>O Minecraft não guarda quem pôs cada bloco</b> — está dito no
     * cabeçalho do {@code BlockProtection}, e continua verdade. O que ele
     * guarda é a <b>forma</b> e a <b>ferramenta</b>, e as duas juntas
     * bastam para o que a mina precisa saber:
     *
     * <ul>
     *   <li><b>cubo cheio</b> — degrau, laje, escada de mão, tocha,
     *       trilho, porta, alçapão, placa e cerca já não são. É a
     *       gramática inteira de um caminho feito à mão, e nenhuma
     *       linha precisa nomeá-la;
     *   <li><b>picareta ou pá</b> — o jogo diz com que se quebra cada
     *       bloco, e o mineiro só tira o que sai com as duas. Tábua, lã
     *       e baú caem fora sem serem citados;
     *   <li><b>tijolo de pedra não</b> — é a única família de cubo cheio
     *       que sai na picareta e que <b>nenhuma caverna gera</b>. É
     *       dela que o autor fez a escada: {@code digging Escadas de
     *       Tijolos de Pedra}.
     * </ul>
     *
     * <p><b>O que ela erra, e para que lado.</b> Um piso de tijolo de
     * barro ou de pedra polida ainda passa por rocha. Errar para este
     * lado é o certo: a mina que para de cavar é pior que a mina que
     * abre um bloco a mais — foi o que a tentativa desfeita provou.
     *
     * <p>E o que é natural e não é cubo cheio — estalactite, ametista,
     * teia, líquen — a mina simplesmente contorna. Nenhum deles fecha
     * passagem.
     */
    public static boolean isRock(ServerWorld world, BlockPos at, BlockState state) {
        return state.isFullCube(world, at)
                && (state.isIn(BlockTags.PICKAXE_MINEABLE)
                        || state.isIn(BlockTags.SHOVEL_MINEABLE))
                && !state.isIn(BlockTags.STONE_BRICKS);
    }

    /** Rocha que a picareta abre: nem espaço aberto, nem o que a Regra 3 protege. */
    public static boolean isDiggableRock(ServerWorld world, BlockPos at) {
        return canDig(world, at) && !isOpenSpace(world, at, world.getBlockState(at));
    }

    /**
     * Se este bloco pode ser cavado — a Regra 3 e o impossível.
     *
     * <p>A mesma pergunta que {@link #nextCut} faz na sua volta, aqui
     * porque o veio precisa saber se consegue abrir a saída antes de
     * descer. Bedrock, lava e o que é da vila respondem não.
     */
    public static boolean canDig(ServerWorld world, BlockPos at) {
        if (!world.isInBuildLimit(at)) {
            return false;
        }

        BlockState state = world.getBlockState(at);

        if (!state.getFluidState().isEmpty()) {
            return false;
        }

        if (MineLighting.isLight(world, at, state)) {
            // A luz da mina não se cava, e o findTheFrontier a pula por
            // aqui: uma tocha na ordem de cavar seria "fronteira" para
            // sempre, e o cursor recuaria até ela toda passagem.
            return false;
        }

        return state.getHardness(world, at) >= 0
                && !BlockProtection.isVillageOriginal(world, at)
                && !BlockProtection.isColonyBuilt(at);
    }
}
