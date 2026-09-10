package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * A pedra que o mineiro não alcança fica de fora por um prazo — E44,
 * 2026-09-10.
 *
 * <p><b>O cursor da galeria segurava a posição para sempre.</b> O
 * {@code couldNotReach} devolve ao cursor a pedra que ninguém cavou, e
 * isso está certo: pular por uma desistência deixou <b>três sessões
 * seguidas com a galeria intacta</b> em 2026-08-27. O que faltava era o
 * outro lado — segurar <i>sem prazo</i> é um laço, e a sessão de
 * 2026-09-10 às 08:33 mediu o laço inteiro:
 *
 * <pre>
 * 08:33     4c4171a4 mira 2442,44,-1424 — out of reach, 9,9 blocos
 * 08:34:13  desiste: "walked for 2400 ticks of work time without arriving",
 *           com "19 blocks below it and unable to climb"
 * 08:34:43  d5f6de43 assume o ramal e recebe A MESMA pedra 2442,44,-1424
 * 08:36:17  desiste com a mesma frase
 * </pre>
 *
 * <p>Cada volta custa os 2.400 tiques de expediente do guarda de
 * travamento, e a colônia tem <b>dois</b> mineiros: eles se revezam no
 * mesmo alvo impossível e ficam enfileirados no mesmo túnel, que foi
 * exatamente a queixa do autor. A pedra não muda entre uma volta e outra,
 * e nada no mod aprendia isso.
 *
 * <p><b>A forma é a do lenhador</b>, e é decisão registrada: o
 * {@link TreeMarks} resolveu o mesmo laço em 2026-09-02 com uma escada de
 * prazos que cresce a cada recusa e uma contagem que sobrevive ao
 * castigo. Os números aqui são os mesmos de lá, de propósito — a segunda
 * recusa é prova de que a primeira não foi azar, e vale mais que ela.
 * Estão repetidos, e não importados, porque as duas marcas respondem a
 * mundos diferentes: a árvore some quando alguém a derruba, a pedra fica
 * no lugar até uma picareta chegar nela.
 *
 * <p><b>Quem lê esta marca são dois, e têm de concordar</b> — é a regra
 * que o {@code MineFrontier.isStillClosed} já traz escrita: <i>"uma
 * posição que o nextCut vai pular não pode ser a fronteira, senão o
 * cursor recua até ela toda passagem"</i>. Marcar num lado só troca este
 * defeito por outro maior, e é por isso que o par
 * {@code MineDigging.nextCut} / {@code MineFrontier.isStillClosed}
 * pergunta aqui.
 *
 * <p><b>E são quatro leitores, não dois</b> — achado do
 * {@code gauntlet-verifier} em 2026-09-10, e ele estava certo: o
 * {@code MinerWork.giveUp} marca <b>toda</b> pedra largada, e a primeira
 * versão desta classe só tinha ensinado o lado da escada a perguntar. Uma
 * colônia sem boca de mina viável cai no {@code exposedStone}, e uma
 * pedra exposta do outro lado da água reproduzia o E44 inteiro — mesmo
 * alvo, mesma desistência, todo ciclo. O mesmo valia para a areia. Os
 * dois passaram a filtrar por aqui.
 *
 * <p><b>O prazo é de mão dupla em três dos quatro, e vale saber qual é o
 * quarto.</b> A veia, a pedra de superfície e a areia procuram por
 * <b>proximidade</b>: vencido o castigo, o alvo volta a ser candidato
 * sozinho, e é aí que <i>o jogador constrói a rampa e o mod muda de
 * ideia</i> acontece de verdade.
 *
 * <p>O cursor do túnel é de mão única, e isto é consequência de um
 * desenho anterior, não descuido: o {@code frontierWhereRockBegins} só
 * chama de frente a posição fechada cuja <b>seguinte</b> também está
 * fechada — regra de 2026-09-02, feita para o cursor não recuar 83 passos
 * até um resto solto dentro do túnel. Uma pedra pulada por castigo cujo
 * vizinho seguinte foi cavado é exatamente um resto solto, e o prazo
 * vencer não a traz de volta: ela fica.
 *
 * <p><b>O preço disso é um bloco não cavado no túnel</b>, e ele é aceito
 * de propósito. A pedra ficou para trás porque <i>ninguém alcançava</i>
 * — o trecho já não era caminhável para aquele mineiro, e deixá-la sólida
 * não piora o que existe. Fazer diferente pediria devolver ao
 * {@code frontierWhereRockBegins} a noção de resto solto que ele foi
 * desenhado para perder, e isso troca este preço pelo laço de recuo que
 * custou a sessão de 09-02. Se um dia doer em jogo, o lugar de mexer é
 * lá — e o teste que fixa o comportamento de hoje é o
 * {@code theSkippedStoneStaysBehindOnceTheGalleryMovedPast}.
 */
public final class MineMarks {

    private MineMarks() {
    }

    /** Quando esta pedra recusou pela última vez, e quantas vezes já recusou. */
    private record Refusal(long since, int count) {
    }

    /**
     * As pedras que um mineiro não conseguiu alcançar, por posição.
     *
     * <p>Da colônia inteira, e não de um mineiro: é isso que impede o
     * revezamento que a sessão das 08:33 mediu. Marca por trabalhador
     * deixaria o segundo mineiro herdar a armadilha inteira do primeiro.
     */
    private static final Map<BlockPos, Refusal> REFUSED = new HashMap<>();

    /**
     * Por quantos tiques uma pedra fica de fora na primeira recusa.
     *
     * <p>Dez ciclos da colônia, cinco minutos — o mesmo prazo base do
     * {@link TreeMarks}.
     *
     * <p><b>E o prazo não vale igual nos quatro leitores</b> — achado do
     * {@code gauntlet-verifier} em 2026-09-10, e a primeira versão desta
     * classe prometia o que não entrega. Ver o parágrafo <i>"O prazo é de
     * mão dupla em três dos quatro"</i> no javadoc da classe: nos três
     * que procuram por proximidade — veia, pedra de superfície e areia —
     * vencer o prazo devolve o alvo de verdade, e é lá que a promessa de
     * <i>"o jogador constrói a rampa e o mod muda de ideia"</i> se cumpre.
     * No cursor do túnel, não.
     */
    private static final int REFUSAL_MEMORY = 10 * VillageDetector.CYCLE_TICKS;

    /** Quantas vezes o prazo pode dobrar. Além disso ele para de crescer. */
    private static final int MAX_DOUBLINGS = 3;

    /** E o teto em múltiplos do prazo base: oitenta ciclos, mais de uma hora. */
    private static final int MAX_MEMORY_FACTOR = 8;

    /**
     * Por quanto tempo se lembra <b>quantas vezes</b> a pedra recusou.
     *
     * <p>Mais longo que o maior castigo, e é o que faz a escada existir:
     * se a contagem morresse junto com o castigo, a pedra voltaria a ser
     * ré primária a cada volta e o prazo nunca passaria de dez ciclos —
     * que é o laço, com outro nome.
     */
    private static final int TALLY_MEMORY = 2 * REFUSAL_MEMORY * MAX_MEMORY_FACTOR;

    /**
     * Teto do mapa, para o servidor que vive dias.
     *
     * <p>O {@link #forgetStaleMarksAt} varre por prazo e é quem esvazia
     * isto no uso normal. Este teto é o que sobra para o caso em que
     * ninguém pergunta há muito tempo — uma mina abandonada, a colônia
     * descarregada — e é a mesma defesa que o {@code TreeMarks.REJECTED}
     * tem.
     */
    private static final int MAX_REFUSED = 4096;

    /**
     * Quanto tempo fica de fora a pedra que já recusou tantas vezes.
     *
     * @param refusals quantas recusas seguidas esta posição acumulou
     */
    static long memoryFor(int refusals) {
        if (refusals <= 0) {
            return 0;
        }

        long doubled = (long) REFUSAL_MEMORY << Math.min(refusals - 1, MAX_DOUBLINGS);

        return Math.min(doubled, (long) REFUSAL_MEMORY * MAX_MEMORY_FACTOR);
    }

    /** Marca esta pedra como inalcançável por ora. */
    public static void refuse(ServerWorld world, BlockPos stone) {
        refuseAt(world.getTime(), stone);
    }

    /**
     * O mesmo, pelo relógio que quem chama entrega.
     *
     * <p><b>Separado do mundo de propósito</b>, e pelo mesmo motivo que o
     * {@code TreeMarks.rejectAt}: a bateria não avança o relógio do mundo
     * — o castigo mais curto é de 6.000 tiques, e nenhum {@code tickLimit}
     * de gametest chega perto —, então a única forma de provar "a segunda
     * recusa dura mais que a primeira" é entregar o relógio.
     */
    static void refuseAt(long now, BlockPos stone) {
        forgetStaleMarksAt(now);

        Refusal before = REFUSED.get(stone);
        int count = before == null ? 1 : before.count() + 1;

        REFUSED.put(stone.toImmutable(), new Refusal(now, count));

        VillageColonyMod.LOGGER.info(
                "The stone at {} is out of reach — refused {} times now,"
                        + " the gallery skips it for {} ticks",
                stone.toShortString(),
                count,
                memoryFor(count));
    }

    /** Se esta pedra ainda está de castigo. */
    public static boolean isOutOfReach(ServerWorld world, BlockPos stone) {
        return isOutOfReachAt(world.getTime(), stone);
    }

    /**
     * O mesmo, pelo relógio que quem chama entrega.
     *
     * <p>A entrada fica no mapa mesmo depois de o castigo vencer, e é de
     * propósito: ela carrega a contagem, que é o que faz o castigo
     * seguinte ser maior. Quem a apaga é {@link #forgetStaleMarksAt}.
     */
    static boolean isOutOfReachAt(long now, BlockPos stone) {
        Refusal refusal = REFUSED.get(stone);

        if (refusal == null) {
            return false;
        }

        return now - refusal.since() < memoryFor(refusal.count());
    }

    /** A picareta pegou nesta pedra: ela deixa de ser suspeita. */
    public static void dug(BlockPos stone) {
        REFUSED.remove(stone);
    }

    /** Tira do registro as marcas cuja contagem já venceu. */
    static void forgetStaleMarksAt(long now) {
        REFUSED.values().removeIf(refusal -> now - refusal.since() >= TALLY_MEMORY);

        if (REFUSED.size() >= MAX_REFUSED) {
            REFUSED.clear();
        }
    }

    /**
     * Esquece tudo. Só os testes precisam disso.
     *
     * <p>O mapa é estático e vive enquanto o servidor viver, o que em
     * jogo é o certo — a colônia não deve reaprender a cada ciclo que não
     * chega naquela pedra. Numa bateria é o contrário: as áreas de teste
     * são reaproveitadas, e uma posição marcada por um teste reaparece
     * como rocha boa no seguinte.
     */
    public static void clearAll() {
        REFUSED.clear();
    }

    /** Quantas posições estão no registro agora. Para a bateria. */
    static int size() {
        return REFUSED.size();
    }
}
