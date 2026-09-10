package com.villagecolony.fabric.work;

import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.BlockProtection;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Onde a mina pode abrir: o lugar da boca, e o chão que a sustenta.
 *
 * <p><b>Não confundir com {@code fabric.integration.MineMouth}</b>, que é
 * a boca <b>mobiliada</b> — lanterna, baú e arco, a Regra 30. Esta acha
 * <b>onde</b> ela cabe; aquela põe as coisas lá depois. O nome saiu no
 * paralelo do {@code BuildSiteScanner}, que responde a mesma pergunta
 * para a casa.
 *
 * <p><b>Saiu do {@code MineDigging} em 2026-09-10</b>, pela mesma dívida
 * que tirou o {@link MineRock}: aquele arquivo tinha <b>1.403 linhas</b>,
 * o pior do projeto, contra o limite de 500.
 *
 * <p><b>Refatoração pura</b>: nenhum destes quatro métodos mudou de
 * corpo, e as oito constantes vieram inteiras, com o javadoc que as
 * justifica. O que mudou foi a casa.
 *
 * <p><b>Por que a cadeia inteira veio junta.</b> Ela é fechada e tem uma
 * porta só: {@link #mouthOf} chama {@link #mouthWithin}, que chama
 * {@code surfaceAt}, que chama {@code surfaceOn} — e ninguém de fora
 * chamava nenhum dos três de baixo. As constantes vêm porque não servem
 * a mais nada: as duas passagens da busca, a janela de altura de cada
 * uma, a distância mínima e o desconto da diagonal são o vocabulário
 * desta pergunta e de nenhuma outra.
 *
 * <p><b>As duas passagens são a decisão 1 de 2026-08-26</b> — <i>mina sem
 * lugar: ela aceita uma boca ruim, e procura mais longe</i>. O que a
 * segunda passagem não relaxa é água em cima e a Regra 3: mina inundada
 * não é mina ruim, é mina quebrada.
 */
public final class MineSite {

    private MineSite() {
    }

    /** A que distância do centro a mina se abre — o fim da vila. */
    private static final int MINE_DISTANCE = 40;

    /**
     * A distância em vigor. É {@link #MINE_DISTANCE}, menos nos testes.
     *
     * <p>A bateria roda arenas lado a lado no mesmo mundo, e uma mina
     * aberta a quarenta blocos sai da arena dela e cava o cenário do
     * teste vizinho. Um teste que destrói o cenário de outro é pior que
     * um teste que não existe.
     *
     * <p><b>Veio do {@code MineDigging} em 2026-09-10, com a busca que a
     * usa.</b> Ela é o raio desta pergunta e de nenhuma outra — o
     * {@code MineDigging} só a lia para escrever o número numa linha de
     * log. Os ganchos de teste continuam lá, delegando, porque treze
     * chamadas os usam e mudá-las seria refatoração virando alteração.
     */
    private static int mineDistance = MINE_DISTANCE;

    /** A distância em vigor, para quem só quer dizê-la no log. */
    static int distance() {
        return mineDistance;
    }

    /** Aproxima a boca da mina. Só os testes precisam disso. */
    static void shortenTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Distance must be positive: " + blocks);
        }

        mineDistance = blocks;
    }

    /** Devolve a distância ao valor de jogo. */
    static void restore() {
        mineDistance = MINE_DISTANCE;
    }

    /**
     * As frações da distância que a busca tenta, em centésimos.
     *
     * <p>Cheia primeiro, que é a intenção do autor, e depois mais perto.
     * <b>Nunca mais longe:</b> "o fim da vila" é um teto, e a bateria
     * encurta essa distância para o mineiro não comer a pedra da arena
     * do lado.
     */
    private static final int[] REACHES = {100, 75, 50};

    /**
     * As frações que a segunda passagem tenta, quando a primeira falha.
     *
     * <p><b>Aqui a busca vai mais longe de propósito</b>, e é decisão do
     * autor em 2026-08-26: <i>ela aceita uma boca ruim, procura mais
     * longe</i>. "O fim da vila" deixa de ser teto quando a alternativa
     * é a colônia sem pedra.
     *
     * <p>Continua proporcional a {@link #mineDistance}, e não um número
     * solto: a bateria encurta essa distância para o mineiro não comer a
     * arena vizinha, e uma segunda passagem em blocos absolutos furaria
     * essa garantia.
     */
    private static final int[] FARTHER = {150, 200};

    /**
     * A janela de altura da boca ruim, para cima e para baixo.
     *
     * <p>Mais larga que {@link #LOOK_UP} e {@link #LOOK_DOWN}: a boca
     * boa é o fim da vila, no nível dela; a ruim aceita subir o morro ou
     * descer a depressão, porque a alternativa é não haver mina.
     *
     * <p><b>O que ela não relaxa:</b> água em cima e a Regra 3. Mina
     * inundada não é mina ruim, é mina quebrada; e peça de vila gerada
     * ou construção da colônia continua intocável em qualquer passagem.
     */
    private static final int POOR_UP = 12;

    /**
     * A janela de altura da boca ruim, para cima e para baixo.
     *
     * <p>Mais larga que {@link #LOOK_UP} e {@link #LOOK_DOWN}: a boca
     * boa é o fim da vila, no nível dela; a ruim aceita subir o morro ou
     * descer a depressão, porque a alternativa é não haver mina.
     *
     * <p><b>O que ela não relaxa:</b> água em cima e a Regra 3. Mina
     * inundada não é mina ruim, é mina quebrada; e peça de vila gerada
     * ou construção da colônia continua intocável em qualquer passagem.
     */


    private static final int POOR_DOWN = 24;

    /**
     * Quanto anda a diagonal, em centésimos da distância cheia.
     *
     * <p>Setenta, que é o cateto de um quadrado de hipotenusa cem. Assim
     * a boca na diagonal fica <b>à mesma distância</b> do centro que a
     * boca no eixo, e o teto de "o fim da vila" continua sendo teto.
     */
    private static final int DIAGONAL = 70;

    /** Mais perto que isto a escada desceria sob a própria vila. */
    private static final int NEAREST_MOUTH = 2;

    /**
     * Quanto acima do nível da vila a boca pode nascer.
     *
     * <p>Curto de propósito: a boca é <b>o fim da vila</b>, e não o topo
     * do morro ao lado. Foi por olhar oito para cima que a primeira
     * versão desta busca abriu uma mina seis blocos acima do centro, em
     * cima do piso da arena vizinha.
     */
    private static final int LOOK_UP = 3;

    /** E quanto abaixo, para a boca numa depressão. */
    private static final int LOOK_DOWN = 12;

    /**
     * A boca da mina: o fim da vila, na direção em que ela se abre.
     *
     * <p>É a frase do autor — <i>anda até o final da vila</i>. Longe o
     * bastante para a escada não descer sob as casas, perto o bastante
     * para o aldeão ir e voltar dentro do expediente.
     *
     * <p><b>Era uma coluna só, e por isso a mina nunca abriu.</b> Até
     * 2026-08-22 esta busca olhava exatamente um ponto — centro mais
     * quarenta blocos numa direção fixa — e desistia se ele não
     * servisse. Sem alternativa, sem nova tentativa e <b>sem uma linha
     * de log</b>: três sessões de jogo terminaram com {@code 0 mines} no
     * save e mineiros mudos com tarefa aberta.
     *
     * <p>Agora ela tenta <b>vinte e quatro colunas</b>: oito direções —
     * os quatro lados e as quatro diagonais entre eles —, em três
     * distâncias. Eram doze até 2026-08-25, e as quatro do eixo caíram
     * todas na água da mesma vila. A ordem é determinística e começa na intenção do autor
     * — o lado da colônia, na distância cheia —, e só depois encurta.
     * <b>Nunca vai mais longe</b> que a distância pedida: "o fim da
     * vila" é um teto, e a bateria encurta essa distância justamente
     * para o mineiro não comer a pedra da arena vizinha.
     *
     * <p>Pública para o teste de jogo, e é uma leitura sem efeito: nada
     * no mundo muda por perguntar onde a boca caberia.
     */
    public static Optional<BlockPos> mouthOf(
            ServerWorld world, BlockPos center, Side towards) {

        return mouthWithin(world, center, towards, REACHES, LOOK_UP, LOOK_DOWN)
                .or(() -> mouthWithin(world, center, towards, FARTHER, POOR_UP, POOR_DOWN));
    }

    /**
     * As oito direções, nestas distâncias, com esta janela de altura.
     *
     * <p>Chamada duas vezes: a primeira com a boca boa — o fim da vila,
     * no nível dela —, a segunda com a ruim, mais longe e menos exigente
     * quanto à altura. Decisão do autor em 2026-08-26.
     */
    private static Optional<BlockPos> mouthWithin(
            ServerWorld world, BlockPos center, Side towards,
            int[] reaches, int up, int down) {

        for (int part : reaches) {
            int away = Math.max(NEAREST_MOUTH, mineDistance * part / 100);

            int corner = Math.max(NEAREST_MOUTH, away * DIAGONAL / 100);

            Side side = towards;

            for (int turn = 0; turn < 4; turn++) {
                Side next = side.clockwise();

                // O eixo primeiro — é a intenção do autor, "anda até o
                // fim da vila" —, e a diagonal entre ele e o seguinte
                // logo depois. Oito por distância, e não quatro: em 08-25
                // as quatro do eixo caíram todas na água da mesma vila, e
                // a colônia ficou sem pedra por falta de amostra.
                Optional<BlockPos> found = surfaceAt(
                        world, center, side.offsetX() * away, side.offsetZ() * away, up, down);

                if (found.isPresent()) {
                    return found;
                }

                found = surfaceAt(
                        world,
                        center,
                        (side.offsetX() + next.offsetX()) * corner,
                        (side.offsetZ() + next.offsetZ()) * corner,
                        up,
                        down);

                if (found.isPresent()) {
                    return found;
                }

                side = next;
            }
        }

        return Optional.empty();
    }

    /**
     * O chão desta coluna, se ela servir de boca.
     *
     * <p><b>O topo sólido, e não o primeiro sólido.</b> A busca antiga
     * descia do centro mais quatro e devolvia o que encontrasse — numa
     * encosta, isso é o <b>miolo do morro</b>, e a boca nascia enterrada.
     * Aqui um bloco só vale se o que está sobre ele puder ser ocupado.
     *
     * <p><b>Nem debaixo d'água.</b> Água é substituível, então o leito do
     * lago passaria por superfície. A boca de uma mina dentro de um lago
     * é a mina inundada no primeiro degrau.
     *
     * <p>Vazio quando a coluna não serve — e vazio é "tente a próxima",
     * e não "desista", que era o defeito.
     */
    private static Optional<BlockPos> surfaceAt(
            ServerWorld world, BlockPos center, int dx, int dz, int up, int down) {

        int x = center.getX() + dx;
        int z = center.getZ() + dz;

        if (world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
            // Nunca forçar carregamento de dentro do ciclo — §11.
            return Optional.empty();
        }

        // Do nível da vila para fora, e não do céu para baixo. "O fim da
        // vila" é um lugar no chão dela: pegar o topo sólido da coluna
        // punha a boca em cima do que estivesse acima — numa arena de
        // bateria, o piso do teste vizinho; num mundo, o galho de uma
        // árvore ou a laje de um morro que a vila não ocupa.
        //
        // Desce primeiro: o chão costuma estar abaixo do marco do centro,
        // que é cama ou baú e fica um bloco acima dele.
        for (int step = 0; step <= Math.max(up, down); step++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int offset = step * sign;

                if (offset > up || offset < -down) {
                    continue;
                }

                int y = center.getY() + offset;

                Optional<BlockPos> found = surfaceOn(world, new BlockPos(x, y, z));

                if (found.isPresent()) {
                    return found;
                }

                if (step == 0) {
                    break;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Se esta posição é chão de verdade: sólida, com espaço livre em cima.
     *
     * <p><b>Nem debaixo d'água.</b> Água é substituível, então o leito do
     * lago passaria por superfície — e boca de mina dentro de um lago é
     * a mina inundada no primeiro degrau.
     *
     * <p>Vazio também quando o bloco é peça de vila gerada ou construção
     * da colônia: a Regra 3 vale para a boca como vale para o resto.
     */
    private static Optional<BlockPos> surfaceOn(ServerWorld world, BlockPos at) {
        if (!world.getBlockState(at).isSolidBlock(world, at)) {
            return Optional.empty();
        }

        BlockPos above = at.up();

        if (!world.getBlockState(above).isReplaceable()
                || !world.getFluidState(above).isEmpty()) {

            return Optional.empty();
        }

        if (BlockProtection.isVillageOriginal(world, at) || BlockProtection.isColonyBuilt(at)) {
            return Optional.empty();
        }

        return Optional.of(at);
    }
}
