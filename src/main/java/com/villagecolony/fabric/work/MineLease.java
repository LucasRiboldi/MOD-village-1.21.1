package com.villagecolony.fabric.work;

import com.villagecolony.fabric.brain.WorkHours;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * A pedra é emprestada por um prazo curto, e só o progresso renova —
 * E44, 2026-09-10, decisão do autor.
 *
 * <p><b>Os dois guardas antigos têm o mesmo ponto cego, e a sessão das
 * 08:33 caiu exatamente nele.</b> O {@link WorkStall} pergunta <i>o
 * aldeão saiu do bloco?</i> — e quem anda em círculos sai, então ele
 * nunca dispara. O {@code MinerWork.STALL_LIMIT} pergunta <i>há quanto
 * tempo ele anda?</i> — e a resposta custa 2.400 tiques de expediente,
 * dois minutos de trabalho, antes de sair. Entre um e outro cabe um
 * mineiro caminhando o orçamento inteiro sem nunca se aproximar:
 *
 * <pre>
 * 08:33     4c4171a4 mira 2442,44,-1424 — out of reach, 9,9 blocos
 * 08:34:13  desiste: "walked for 2400 ticks of work time without arriving",
 *           com "19 blocks below it and unable to climb"
 * </pre>
 *
 * <p>Dezenove blocos abaixo do alvo, sem escada: aquele mineiro não ia
 * chegar no tique 1 nem no tique 2.400. A informação que decidia estava
 * pronta em quinze segundos, e o mod esperou dois minutos para usá-la —
 * duas vezes, porque a colônia tem dois mineiros e o segundo assume o
 * ramal.
 *
 * <p><b>A pergunta certa é a terceira: ele está chegando mais perto?</b>
 * É a mesma lição do {@code mineBlock} do AnimaFabric que o
 * {@link WorkStall} cita — <i>conferir que a ação surtiu efeito</i> —,
 * aplicada ao que a caminhada deveria produzir. Andar não é efeito;
 * encurtar a distância é.
 *
 * <p><b>A régua é o melhor de todos os tiques, e não o tique
 * anterior.</b> Contra o tique anterior, um mineiro que anda de verdade
 * seria punido: a navegação entrega passos de centésimos de bloco, e
 * quase nenhum passa da {@link #MARGIN} sozinho. Contra o melhor, quem
 * anda quatro blocos em quatro segundos renova dezesseis vezes no
 * caminho, e quem oscila entre dois blocos não renova nenhuma. Ver
 * {@code MineLeaseTest.creepingCloserBelowTheMarginIsNotProgress}.
 *
 * <p><b>Por que uma classe, e não um {@code double} no Job.</b> Pelo
 * mesmo motivo do {@link WorkStall}: são duas perguntas que precisam ser
 * feitas juntas — <i>é expediente?</i> e <i>ele encurtou?</i> —, e
 * soltas elas se separam. Fora do expediente a
 * {@code GoToWorkTargetTask} nem começa, e cobrar aproximação de quem
 * está proibido de andar é o E44 ao contrário: o mineiro perderia a
 * pedra dormindo.
 *
 * <p>Sem mapa estático e sem limpeza: cada {@code Job} tem o seu, e ele
 * morre junto com o trabalho.
 */
public final class MineLease {

    /**
     * Quantos tiques de expediente sem se aproximar antes de largar a pedra.
     *
     * <p>Quatrocentos — vinte segundos de trabalho —, o teto da faixa que
     * o autor pediu (200 a 400), e o teto de propósito. O preço de errar
     * não é simétrico: desistir cedo demais de uma pedra boa a manda para
     * o castigo do {@link MineMarks} por dez ciclos, e o mineiro que
     * estava contornando um morro perde um alvo que ele alcançaria. Vinte
     * segundos é folgado para o contorno — a essa altura ele já andou uns
     * sete blocos — e ainda assim <b>seis vezes</b> mais rápido que os
     * dois minutos que a sessão das 08:33 pagou duas vezes.
     */
    public static final int LIMIT = 400;

    /**
     * O prazo em vigor, que é o de cima fora da bateria.
     *
     * <p>Ver {@link #shortenTo}: é o mesmo gancho de
     * {@code MineDigging.shortenMineDistanceTo} e
     * {@code FarmerWork.shortenSearchTo}, e existe pelo mesmo motivo.
     */
    private static int limit = LIMIT;

    /**
     * Encurta o prazo. Só os testes precisam disso.
     *
     * <p><b>Não é conforto, é o que separa este teste do vizinho.</b> Uma
     * bateria de gametest roda os lotes em paralelo, e um teste de 480
     * tiques fica de pé enquanto dezenas de outros nascem e morrem ao
     * lado. Tudo o que ele mexer em estático — mapa de marcas, alcance
     * de varredura — fica mexido para eles também: foi assim que a
     * primeira versão do teste do E44 derrubou dois testes de marca que
     * passavam, em 2026-09-10. Com o prazo em quarenta, ele dura um
     * segundo e meio.
     *
     * <p>O que ele mede não muda: os outros dois guardas continuam em
     * 300 e 2.400, então quem fala continua sendo só o prazo.
     */
    public static void shortenTo(int ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Lease must be positive: " + ticks);
        }

        limit = ticks;
    }

    /** Devolve o prazo ao valor de jogo. */
    public static void restoreLimit() {
        limit = LIMIT;
    }

    /** O prazo em vigor agora. */
    public static int limit() {
        return limit;
    }

    /**
     * Quanto ele precisa encurtar para que conte como aproximação.
     *
     * <p>Um quarto de bloco. Abaixo disso é o tremor do caminhante que
     * se deu por chegado — o {@code MinerReach.ARRIVAL} é zero desde
     * 2026-08-29 justamente para ele fechar o último bloco, e fechar o
     * último bloco é oscilar em torno dele.
     */
    static final double MARGIN = 0.25;

    /** A menor distância já vista nesta pedra, ou NaN antes da primeira. */
    private double closest = Double.NaN;

    /** Há quantos tiques de expediente ele não bate a própria régua. */
    private int idle;

    /**
     * Mais um tique de aproximação, e se o prazo venceu.
     *
     * <p>Chamada no ramo em que o mineiro <b>deveria estar andando</b> —
     * fora de alcance, com destino posto. Ela mesma descarta o que não é
     * expediente, e por isso não há como esquecer.
     *
     * @return true quando ele não encurta a distância há {@link #limit()}
     *     tiques de expediente
     */
    public boolean outOfTime(ServerWorld world, VillagerEntity villager, BlockPos target) {
        if (!WorkHours.isWorkTime(world, villager)) {
            return false;
        }

        return ranOut(
                MinerReach.distanceTo(
                        villager.getX(), villager.getY(), villager.getZ(), target));
    }

    /**
     * O mesmo, pela distância que quem chama entrega.
     *
     * <p><b>Separado do mundo de propósito</b>, e pelo mesmo motivo que o
     * {@code MineMarks.refuseAt}: quatrocentos tiques de aproximação são
     * afirmáveis numa bateria de segundos, e subir um servidor para
     * afirmá-los não provaria mais nada. Ver {@code MineLeaseTest}.
     */
    boolean ranOut(double distance) {
        if (Double.isNaN(closest) || distance <= closest - MARGIN) {
            closest = distance;
            idle = 0;

            return false;
        }

        return ++idle >= limit;
    }

    /**
     * Pedra nova: a régua e a contagem recomeçam.
     *
     * <p><b>Aqui alvo novo É motivo</b>, e é a diferença desta classe
     * para o {@link WorkStall}, que aprendeu no E36 a não zerar por
     * troca de alvo. As duas estão certas porque as perguntas são
     * outras: <i>o aldeão está congelado?</i> não muda de resposta
     * quando o alvo muda, mas <i>ele está chegando mais perto</i>
     * <b>de quê</b> é uma pergunta sobre a pedra. Guardar a régua da
     * pedra anterior faria o mineiro desistir de uma pedra mais distante
     * sem ter dado um passo por ela.
     */
    void reset() {
        closest = Double.NaN;
        idle = 0;
    }

    /** Há quantos tiques ele não se aproxima. Para o relatório. */
    public int ticks() {
        return idle;
    }

    /** A menor distância que ele conseguiu nesta pedra, ou -1 antes da primeira. */
    public double closest() {
        return Double.isNaN(closest) ? -1 : closest;
    }

    /** Se o prazo já venceu. Para a bateria. */
    boolean expired() {
        return idle >= limit;
    }
}
