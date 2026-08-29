package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * A luz da galeria, e não só a da boca — 2026-08-28.
 *
 * <p><b>O que faltava.</b> A Regra 30 põe uma lanterna na boca da mina e
 * mais nada. Vinte blocos abaixo dela a escada e a galeria ficam com luz
 * zero, que é a condição exata de criatura nascer — <b>dentro</b> da
 * mina, ao lado de um aldeão desarmado. A sessão de 2026-08-26 nem a
 * lanterna da boca conseguiu: saiu {@code lantern at nowhere it fits}.
 *
 * <p><b>Uma tocha por passagem, e sempre atrás do mineiro.</b> Não se
 * cava para iluminar: a tocha vai no alto de uma passagem que ele já
 * abriu, de {@link #SPACING} em {@link #SPACING} posições da ordem de
 * cavar. Fora disso ela seria obra, e obra é do construtor.
 *
 * <p><b>Não custa material</b>, pela mesma razão que a mobília da boca:
 * a escada, as salas e a galeria são cavadas e ninguém paga por elas.
 * Cobrar tocha aqui faria a mina ficar escura até a colônia ter carvão —
 * e carvão vem da mina.
 *
 * <p><b>E o mineiro não cava a própria luz.</b> É a lição de 2026-08-27,
 * quando o lampião da boca caiu no primeiro degrau e ele o cavou: uma
 * posição com luz é <b>espaço aberto</b>, e não rocha. Quem responde isso
 * é {@link #isLight}, e é por ela que {@code MineDigging} pula a tocha em
 * vez de tratá-la como frente de galeria.
 */
public final class MineLighting {

    /**
     * De quantas em quantas posições da ordem de cavar nasce uma tocha.
     *
     * <p>A tocha do jogo acende 14, e a luz cai um por bloco: a sete
     * blocos dela ainda sobram 7, e criatura hostil precisa de <b>zero</b>
     * para nascer. Oito posições deixam o trecho inteiro aceso com folga,
     * e é o espaçamento que um jogador usa por hábito.
     *
     * <p>Posição, e não bloco de distância: um degrau da escada são três
     * posições, e a galeria são duas. O trecho iluminado é mais curto que
     * oito blocos em toda parte, o que erra para o lado seguro.
     */
    public static final int SPACING = 8;

    /**
     * Quantas posições para trás a passagem olha à procura de trabalho.
     *
     * <p>A mina não tem fim, e varrer a ordem inteira toda passagem seria
     * um custo que cresce com a sessão. Olhar os três pontos mais
     * recentes basta: o que ficou para trás ou já tem tocha, ou não
     * coube, e no segundo caso a posição seguinte cobre o trecho.
     */
    private static final int LOOK_BACK = SPACING * 3;

    /**
     * Quanto o cursor precisa ter andado além do ponto antes de acender.
     *
     * <p><b>Sem esta folga a tocha nasce cedo demais.</b> O cursor da
     * mina conta posição <i>entregue</i>, e não bloco <i>cavado</i>: a
     * que está em curso ainda é rocha, e a coluna aberta pela metade
     * <b>parece</b> ter teto — o bloco de cima só não é ar porque a
     * picareta não chegou nele.
     *
     * <p>A bateria pegou isso duas vezes seguidas, e a segunda foi mais
     * fina que a primeira: o E33 falhou com <i>"o degrau saiu sem altura
     * para o aldeão passar"</i>, porque a camada da cabeça tinha virado
     * tocha antes de a terceira ser cavada.
     *
     * <p>Um espaçamento inteiro para trás resolve com folga larga: quando
     * o cursor chega ao ponto seguinte, o anterior está cavado há oito
     * posições. Custa acender um trecho atrasado, e atrás do mineiro é
     * exatamente onde a luz precisa estar — ele acabou de sair de lá, e
     * é por lá que ele volta.
     */
    private static final int BEHIND = SPACING;

    /**
     * Quanto se sobe à procura do teto da passagem.
     *
     * <p>Um a menos que a altura do degrau da escada: da posição mais
     * baixa da coluna até a mais alta são dois passos, e a galeria tem
     * um só.
     */
    private static final int LOOK_UP = MineShaft.STAIR_HEADROOM - 1;

    private MineLighting() {
    }

    /**
     * A posição da ordem de cavar que pede tocha, dado o que já abriu.
     *
     * <p>O mais recente múltiplo do espaçamento entre as posições
     * abertas — que são {@code 0} até {@code cut - 1}.
     *
     * @param cut quantas posições da ordem já foram percorridas
     * @return o índice, ou {@code -1} quando ainda não há nenhum
     */
    public static int spotFor(int cut) {
        if (cut <= 0) {
            return -1;
        }

        return (cut - 1) / SPACING * SPACING;
    }

    /**
     * Se este bloco é luz solta na passagem, e não rocha.
     *
     * <p><b>Perguntado ao jogo, e não a uma lista de nomes</b> — a regra
     * de ouro da ADR-009. Duas condições, e as duas importam: acende, e
     * não fecha a passagem. Tocha, lanterna e vela entram; magma e pedra
     * luminosa são bloco sólido e continuam sendo pedra a cavar.
     *
     * <p>Vale para a luz que o jogador pôs também, e é de propósito: uma
     * posição com tocha está aberta, quem quer que a tenha aberto, e a
     * Regra 3 manda não mexer no que é dele.
     */
    public static boolean isLight(ServerWorld world, BlockPos at, BlockState state) {
        return state.getLuminance() > 0 && !state.isSolidBlock(world, at);
    }

    /**
     * Uma tocha, se a passagem pedir e couber.
     *
     * <p>Idempotente e barata: o ponto mais recente já aceso faz a
     * passagem andar para trás, e três pontos depois ela desiste. Chamada
     * a cada passagem em que a mina existe, como a mobília da boca.
     *
     * @return onde a tocha foi posta, ou vazio quando não havia o que
     *     fazer — nada aberto, tudo aceso, ou nenhum apoio
     */
    public static Optional<BlockPos> light(ServerWorld world, Mine mine) {
        // De propósito atrás do cursor — ver BEHIND.
        int newest = spotFor(mine.cut()) - BEHIND;

        for (int spot = newest; spot >= 0 && spot > newest - LOOK_BACK; spot -= SPACING) {
            BlockPos at = MinecraftTypeAdapter.toBlockPos(mine.shaft().positionAt(spot));

            if (world.getChunkManager().getWorldChunk(at.getX() >> 4, at.getZ() >> 4) == null) {
                // Nunca forçar carregamento de dentro do ciclo — §11.
                return Optional.empty();
            }

            if (!world.getBlockState(at).isAir()) {
                // Ainda por cavar, ou já acesa. Nos dois casos não é aqui.
                continue;
            }

            Optional<BlockPos> lit = placeTorch(world, at);

            if (lit.isPresent()) {
                VillageColonyMod.LOGGER.info(
                        "Mine {} lit the gallery at {} — position {} of the dig order",
                        mine.colonyId(),
                        lit.get().toShortString(),
                        spot);

                return lit;
            }
        }

        return Optional.empty();
    }

    /**
     * A tocha no alto da passagem, e nunca no chão dela.
     *
     * <p><b>O chão é degrau, e degrau se pisa.</b> A primeira versão
     * punha a tocha na posição da ordem de cavar, que na escada é o
     * bloco dos pés — e a bateria pegou na hora: o E33 passou a falhar
     * com <i>"o primeiro degrau da escada continua fechado"</i>, porque
     * o degrau tinha virado tocha. Numa escada de um bloco de largura
     * todo chão é caminho, e não sobra nenhum.
     *
     * <p>Então ela sobe até o teto da coluna e exige <b>ar embaixo</b>:
     * é o que garante que o bloco escolhido não é piso de ninguém. Na
     * escada isso é a terceira camada, que é folga pura — o aldeão ocupa
     * duas. Na galeria, de duas de altura, é a da cabeça: custa um lugar
     * de ficar de pé a cada {@link #SPACING} posições, e o
     * {@code approachTo} procura num cubo de raio quatro, então sempre
     * há outro.
     *
     * <p>Tocha de parede, e não de chão, pela mesma razão: no alto não
     * há em que se apoiar de baixo. A rocha da lateral é que a segura, e
     * numa passagem de um bloco de largura ela está sempre lá.
     *
     * <p><b>Coluna ainda só aberta no chão não recebe nada.</b> O teto
     * dela são as posições seguintes da ordem, e a passagem volta aqui
     * quando elas saírem — é para isso que o {@link #LOOK_BACK} olha
     * para trás.
     */
    private static Optional<BlockPos> placeTorch(ServerWorld world, BlockPos from) {
        BlockPos at = from;

        for (int up = 0; up < LOOK_UP; up++) {
            BlockPos above = at.up();
            BlockState overhead = world.getBlockState(above);

            if (isLight(world, above, overhead)) {
                // <b>A coluna já está acesa</b>, e sem esta linha ela
                // ganharia uma segunda tocha um bloco abaixo da primeira:
                // a subida parava embaixo dela — tocha não é ar — e o
                // lugar tinha ar embaixo, que era o único teste. A
                // bateria pegou de novo pelo E33, e desta vez pela
                // camada da cabeça.
                return Optional.empty();
            }

            if (!overhead.isAir()) {
                // O teto da passagem.
                break;
            }

            at = above;
        }

        if (!world.getBlockState(at).isAir() || !world.getBlockState(at.down()).isAir()) {
            // Ou já tem coisa aqui, ou aqui é o piso da passagem.
            return Optional.empty();
        }

        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockState torch =
                    Blocks.WALL_TORCH.getDefaultState().with(Properties.HORIZONTAL_FACING, facing);

            if (torch.canPlaceAt(world, at)) {
                world.setBlockState(at, torch);

                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

}
