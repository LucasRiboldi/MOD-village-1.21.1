package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * A picareta abriu um veio de água — 2026-09-03.
 *
 * <p><b>Decisão do autor:</b> <i>"quando quebrar uma pedra e sair água
 * por ali ele deve rapidamente colocar um bloco no lugar para encerrar o
 * fluxo da água e seguir por outro caminho"</i>.
 *
 * <p>É o que um jogador faz, e pelo mesmo motivo. Sem isto a galeria
 * inunda: a água corre pelo túnel aberto, desce a escada, e a mina passa
 * a ser um lugar onde o aldeão não fica de pé — {@code standable} pede
 * dois blocos livres sobre bloco sólido, e coluna de água não é livre
 * para quem anda. A mina não fica difícil; ela deixa de existir, e o
 * mineiro passa a devolver tarefa em toda passagem.
 *
 * <p>Com lava é pior que perder a mina: {@code BlockBreakTime} não sabe
 * de dano, o aldeão anda até a pedra pelo caminho mais curto, e o
 * caminho mais curto atravessa o que o matou.
 *
 * <p><b>Onde o bloco vai.</b> Não na pedra recém-cavada — isso seria
 * desfazer o trabalho e recomeçá-lo na passagem seguinte, para sempre. Vai
 * <b>na face de onde o líquido vem</b>, que é a mesma escolha do jogador:
 * tapa-se o buraco, e a água que já entrou escorre sozinha por não ter
 * mais nascente.
 *
 * <p><b>E não se lembra de nada.</b> A vedação não é gravada, e é de
 * propósito — a mesma escolha do baú da boca e da frente da galeria, que
 * são lidos do mundo. Se a ordem de cavar voltar a passar por aqui, ele
 * cava a vedação, a água volta, ele veda de novo e a galeria vira de
 * novo: cada volta custa uma picareta e uma curva, e quatro curvas descem
 * um nível. Um laço que se resolve descendo é um laço que se fecha —
 * caro seria o contrário, gravar posição de bloco no save para um caso
 * que o mundo já sabe responder.
 */
public final class MineFlooding {

    /**
     * O que se põe no buraco.
     *
     * <p>Pedregulho, que é o que o mineiro tira o dia inteiro e o que um
     * jogador usaria. Sólido, barato, e sem estado nenhum a acertar —
     * bloco com orientação ou com apoio traria a discussão que a tocha de
     * parede trouxe em 2026-08-27.
     */
    private static final BlockState SEAL = Blocks.COBBLESTONE.getDefaultState();

    private MineFlooding() {
    }

    /**
     * Tapa as nascentes que a picareta acabou de abrir nesta posição.
     *
     * <p>As seis faces, e só elas: é o que se vê da pedra que saiu. O
     * líquido a dois blocos daqui não entrou por este buraco, e tapá-lo
     * seria o mineiro cimentando o lago inteiro.
     *
     * <p><b>A Regra 3 vale aqui como vale para a picareta</b>, e é o
     * mesmo argumento do {@code OreVein.beside}: o que é da vila gerada e
     * o que é da colônia não se toca. Água dentro do poço da vila é dela.
     *
     * @param dug a posição de onde o bloco saiu
     * @return quantas faces foram tapadas — zero quando não havia líquido
     */
    public static int seal(ServerWorld world, BlockPos dug) {
        int sealed = 0;

        for (Direction face : Direction.values()) {
            BlockPos at = dug.offset(face);

            if (!world.isInBuildLimit(at)) {
                continue;
            }

            if (world.getBlockState(at).getFluidState().isEmpty()) {
                continue;
            }

            if (BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {
                continue;
            }

            world.setBlockState(at, SEAL);

            sealed++;
        }

        if (sealed > 0) {
            VillageColonyMod.LOGGER.info(
                    "The mine sealed {} face(s) at {} — the pick opened a flow",
                    sealed,
                    dug.toShortString());
        }

        return sealed;
    }
}
