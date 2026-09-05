package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.Side;

import java.util.Objects;

/**
 * A mina que o mineiro cava — a Regra 29, 2026-08-20.
 *
 * <p>O autor a descreveu por inteiro, e ela é geometria: o mineiro anda
 * até o fim da vila e desce cavando <b>em escada</b>, para poder voltar
 * a subir. Desce dez blocos, abre uma sala de sete por quatro no décimo,
 * desce mais dez por outro lado, abre outra sala no vigésimo, e dali em
 * diante recolhe na altura do aldeão mais um, sem fim.
 *
 * <pre>
 * lance 1     dez degraus, três blocos de altura cada — dois para o
 *             aldeão caber, e o terceiro para ele passar
 * sala 1      sete por quatro no nível -10
 * lance 2     mais dez degraus, virando à direita: cavar reto para
 *             baixo daria um poço, e de poço não se sobe
 * sala 2      sete por quatro no nível -20
 * galeria     do nível -20 em diante, sem fim
 * </pre>
 *
 * <p><b>Por que a escada, e não o poço.</b> É a frase do autor: "de modo
 * que ele possa subir de volta". Um aldeão que cavasse reto para baixo
 * ficaria no fundo do buraco, e a colônia perderia um trabalhador por
 * causa do próprio trabalho.
 *
 * <p><b>Por que dois blocos de altura na galeria.</b> "na altura do
 * aldeão mais 1" — os pés e a cabeça. Um só e ele não passa; três e a
 * mina custa cinquenta por cento a mais de tempo para dar a mesma pedra.
 *
 * <p><b>E por que três na escada.</b> Porque descer é andar antes de
 * cair, e quem anda leva a cabeça junto — ver {@link #STAIR_HEADROOM} e
 * a sessão de 2026-08-27.
 *
 * <p>Mora em {@code core} e não conhece Minecraft: é geometria pura, e
 * geometria se afirma sem subir servidor. Quem decide se um bloco
 * <i>pode</i> ser cavado — pedra do jogador, bedrock, a Regra 3 — é a
 * camada de fora.
 */
public record MineShaft(ColonyPos entry, Side descent, Side gallery) {

    /** Blocos por lance de escada. Dois lances até o fundo. */
    public static final int DESCENT = 10;

    /** O comprimento da sala, no sentido em que se descia. */
    public static final int ROOM_LONG = 7;

    /** A largura da sala, para o lado. */
    public static final int ROOM_WIDE = 4;

    /** Altura do aldeão mais um: os pés e a cabeça. */
    public static final int HEADROOM = 2;

    /**
     * Quanto um degrau abre — três, e não dois. Visto em jogo em
     * 2026-08-27.
     *
     * <p>Dois é quanto o aldeão ocupa <b>parado</b>. Descer um degrau
     * não é cair: é andar para a frente no mesmo nível e só então cair,
     * e nesse instante a cabeça dele está um bloco acima do teto do
     * degrau seguinte.
     *
     * <pre>
     * degrau s      abre y, y+1      pés em y, cabeça em y+1
     * degrau s+1    abre y-1, y      a cabeça bate em y+1, maciço
     * </pre>
     *
     * <p>Com dois, o mineiro parava no primeiro degrau e batia a
     * picareta no ar — a mina só descia porque o jogador abria o caminho
     * na mão. A frase dele: <i>"o mineiro precisa quebrar mais um bloco
     * na sua frente para poder descer a escada"</i>.
     *
     * <p><b>Custa vinte blocos na mina inteira</b>, dez por lance. A
     * objeção que a galeria carrega — <i>três e a mina custa cinquenta
     * por cento a mais</i> — vale para ela, que é plana e continua com
     * dois. Escada plana não existe.
     */
    public static final int STAIR_HEADROOM = 3;

    /** Quantas posições um lance de escada pede. */
    private static final int STAIR_BLOCKS = DESCENT * STAIR_HEADROOM;

    /** Quantas posições uma sala pede. */
    private static final int ROOM_BLOCKS = ROOM_LONG * ROOM_WIDE * HEADROOM;

    /** Onde cada trecho começa, na ordem em que se cava. */
    private static final int ROOM_ONE = STAIR_BLOCKS;

    private static final int STAIR_TWO = ROOM_ONE + ROOM_BLOCKS;

    private static final int ROOM_TWO = STAIR_TWO + STAIR_BLOCKS;

    /** A partir daqui é galeria, e ela não acaba. */
    public static final int CARVED = ROOM_TWO + ROOM_BLOCKS;

    public MineShaft {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(descent, "descent");
        Objects.requireNonNull(gallery, "gallery");
    }

    /**
     * A mina que começa aqui, descendo para este lado.
     *
     * <p>O segundo lance vira à direita, e a galeria segue à direita de
     * novo. Duas curvas à direita afastam a galeria do lance de subida,
     * que é onde o aldeão anda.
     */
    public static MineShaft from(ColonyPos entry, Side descent) {
        return new MineShaft(entry, descent, descent.clockwise().clockwise());
    }

    /**
     * A mesma mina, com a galeria virada.
     *
     * <p>É a frase do autor: <i>"sempre que encontrar uma barreira que
     * impeça de realizar estas ações ele começa a recolher para outro
     * lado"</i>. Lava, bedrock, uma caverna — a galeria vira e segue.
     */
    public MineShaft turned() {
        return new MineShaft(entry, descent, gallery.clockwise());
    }

    /**
     * O nível mais fundo que a mina procura — 2026-09-02.
     *
     * <p>É o pico do diamante em 1.21, e não o fundo do mundo: abaixo
     * dele a geração cai, e a rocha-mãe começa cinco blocos depois.
     * Parar aqui é parar onde há mais o que achar.
     */
    public static final int DEEPEST = -59;

    /**
     * O poço do nível seguinte, que começa onde a galeria deste está.
     *
     * <p><b>A profundidade cresce aos poucos</b> — 2026-09-02, e a forma
     * é a do MineColonies, onde a mina desce um nível a cada nível do
     * prédio. Aqui quem manda é a galeria ter fechado o círculo: quatro
     * curvas e ela voltou à direção em que começou, tendo dado a volta
     * no nível. Ver {@code Mine.turn}.
     *
     * <p><b>Por que isso importa.</b> A sessão de 2026-09-02 trabalhou
     * em {@code y=44}, e o pico do diamante é {@code y=-59}: cem blocos
     * acima do que se estava procurando. Uma mina que não desce não tem
     * como achar minério melhor, por mais que se conserte a busca.
     *
     * <p>Mesma descida e mesma galeria: o que muda é a altura de onde
     * ela recomeça. Cada nível custa duas descidas, que são vinte
     * blocos.
     */
    public MineShaft deepened() {
        return new MineShaft(landingTwo(), descent, gallery);
    }

    /** Se ainda há nível abaixo deste, sem passar do {@link #DEEPEST}. */
    public boolean mayDeepen() {
        return deepened().landingTwo().y() >= DEEPEST;
    }

    /**
     * A posição de índice {@code i} na ordem de cavar.
     *
     * <p>Índice acima de {@link #CARVED} é galeria, e por isso não há
     * teto: a mina não acaba, e quem a interrompe é o expediente, a
     * paciência do jogador ou o fim do mundo.
     */
    public ColonyPos positionAt(int i) {
        if (i < ROOM_ONE) {
            return stair(entry, descent, i);
        }

        if (i < STAIR_TWO) {
            return room(landingOne(), descent, i - ROOM_ONE);
        }

        if (i < ROOM_TWO) {
            return stair(cornerOne(), descent.clockwise(), i - STAIR_TWO);
        }

        if (i < CARVED) {
            return room(landingTwo(), descent.clockwise(), i - ROOM_TWO);
        }

        return tunnel(i - CARVED);
    }

    /**
     * Um degrau: o bloco dos pés e o da cabeça, um passo adiante e um
     * abaixo do anterior.
     */
    private static ColonyPos stair(ColonyPos top, Side towards, int i) {
        int step = i / STAIR_HEADROOM + 1;
        int layer = i % STAIR_HEADROOM;

        return new ColonyPos(
                top.x() + towards.offsetX() * step,
                top.y() - step + 1 + layer,
                top.z() + towards.offsetZ() * step);
    }

    /** Onde o primeiro lance para: dez blocos abaixo da entrada. */
    private ColonyPos landingOne() {
        return new ColonyPos(
                entry.x() + descent.offsetX() * DESCENT,
                entry.y() - DESCENT,
                entry.z() + descent.offsetZ() * DESCENT);
    }

    /**
     * O canto da primeira sala de onde o segundo lance parte.
     *
     * <p><b>O canto, e não a ponta.</b> Partir da ponta punha os
     * primeiros degraus dentro da largura da sala — o teste da forma
     * pegou isso, e a sobreposição custaria ao aldeão bater a picareta
     * no ar oito vezes.
     */
    private ColonyPos cornerOne() {
        Side sideways = descent.clockwise();

        ColonyPos floor = landingOne();

        return new ColonyPos(
                floor.x() + descent.offsetX() * ROOM_LONG + sideways.offsetX() * (ROOM_WIDE - 1),
                floor.y(),
                floor.z() + descent.offsetZ() * ROOM_LONG + sideways.offsetZ() * (ROOM_WIDE - 1));
    }

    /** Onde o segundo lance para: vinte blocos abaixo da entrada. */
    private ColonyPos landingTwo() {
        Side towards = descent.clockwise();

        ColonyPos from = cornerOne();

        return new ColonyPos(
                from.x() + towards.offsetX() * DESCENT,
                from.y() - DESCENT,
                from.z() + towards.offsetZ() * DESCENT);
    }

    /**
     * Uma posição da sala: sete de fundo, quatro de largura, duas de alto.
     *
     * <p><b>Começa um bloco adiante do patamar</b>, e não sobre ele. O
     * último degrau abre os blocos que seriam o canto da sala, e cavá-los
     * de novo seria o aldeão batendo a picareta no ar.
     */
    private static ColonyPos room(ColonyPos floor, Side towards, int i) {
        int high = i % HEADROOM;
        int wide = i / HEADROOM % ROOM_WIDE;
        int deep = i / (HEADROOM * ROOM_WIDE) + 1;

        Side sideways = towards.clockwise();

        return new ColonyPos(
                floor.x() + towards.offsetX() * deep + sideways.offsetX() * wide,
                floor.y() + 1 + high,
                floor.z() + towards.offsetZ() * deep + sideways.offsetZ() * wide);
    }

    /**
     * Quantas colunas de corredor antes de cada bolsão — 2026-09-03.
     *
     * <p>Oito, que é a distância entre duas tochas: o bolsão cai onde a
     * luz já chega.
     */
    public static final int RUN = 8;

    /** Quanto o bolsão avança ao lado do corredor, em colunas. */
    public static final int POCKET_LONG = 3;

    /** E quanto ele entra na parede. */
    public static final int POCKET_WIDE = 2;

    private static final int RUN_BLOCKS = RUN * HEADROOM;

    private static final int POCKET_BLOCKS = POCKET_LONG * POCKET_WIDE * HEADROOM;

    /**
     * O ciclo da galeria: um trecho de corredor e o bolsão dele.
     *
     * <p><b>Fixo, e é o que mantém a conta em O(1).</b> A posição de
     * índice {@code i} tem de sair de uma fórmula, e não de somar o
     * caminho desde a boca: o {@code MinerReach.legTowards} percorre até
     * duas mil posições <b>todo tique</b>, e uma ordem que precisasse ser
     * acumulada custaria isso ao quadrado.
     *
     * <p>Por isso o bolsão é periódico e o que varia é de que <b>lado</b>
     * ele fica — ver {@link #pocketSide}. Sorteio que mudasse o tamanho
     * mudaria o passo do ciclo, e o passo do ciclo é o que fecha a
     * fórmula.
     */
    private static final int GALLERY_CYCLE = RUN_BLOCKS + POCKET_BLOCKS;

    /**
     * Quantas colunas a galeria avança antes de virar — decisão do
     * autor, 2026-09-04.
     *
     * <p><b>A frase dele:</b> <i>"o mineiro deve priorizar o perímetro da
     * vila"</i>, e a forma escolhida foi um teto de raio a partir da
     * boca.
     *
     * <p><b>O que ela conserta está medido.</b> Na sessão de 2026-09-04,
     * às 21:03, o mineiro estava em {@code 1456,44,87} e a ordem de cavar
     * apontava para {@code 1454,44,158}: <b>70,7 blocos</b>, {@code out
     * of reach}, {@code 0/0 ticks}. A galeria não tinha teto — o
     * {@code cycle} do {@link #tunnel} cresce sem fim —, e nem virar
     * resolvia: {@code Mine.turn} trocava a direção e <b>guardava o
     * cursor</b>, então a curva punha o aldeão à mesma distância, noutro
     * rumo.
     *
     * <p>Vinte e quatro colunas são três trechos de {@link #RUN} com os
     * bolsões deles. Somadas à sala e à descida, põem a frente mais
     * distante a cerca de quarenta blocos da boca — dentro do que a perna
     * do mineiro percorre num expediente.
     *
     * <p>Com {@link Mine#TURNS_PER_LEVEL} curvas, o nível vira um anel de
     * quatro braços em volta do poço, e só então a mina desce. É a forma
     * que o autor pediu: nem uma reta sem fim, nem sorteio — um perímetro.
     */
    public static final int ARM = 24;

    /**
     * Se este índice da ordem já passou do fim do braço.
     *
     * <p>Índice, e não distância medida no mundo: o corredor sai reto da
     * sala, então contar colunas <b>é</b> medir o raio, e sem custo. O
     * {@code MinerReach.legTowards} percorre até duas mil posições por
     * tique, e uma pergunta que precisasse de raiz quadrada estaria nesse
     * laço.
     */
    public boolean beyondTheArm(int i) {
        return i >= CARVED && (i - CARVED) / GALLERY_CYCLE >= ARM / RUN;
    }

    /**
     * A galeria: corredor com bolsões, e não um túnel reto sem fim —
     * decisão do autor, 2026-09-03.
     *
     * <p>A frase dele: <i>"o caminho de mineração pode ser de modo mais
     * aleatório em bolsões e não uma linha reta"</i>.
     *
     * <p>Parte do canto oposto da segunda sala para não recavá-la: a sala
     * já está aberta, e a galeria é o que vem depois dela.
     *
     * <p><b>O corredor continua reto, e isso é de propósito.</b> Ele é o
     * caminho de volta do aldeão, e é dele que o {@code legTowards}
     * depende — <i>a ordem de cavar É um corredor contínuo a partir da
     * boca</i>. Fazer a espinha serpentear poria dois blocos em diagonal,
     * e de diagonal a navegação não passa sem que os cantos estejam
     * abertos: é o E34 pela porta de trás.
     *
     * <p><b>O bolsão fica pendurado ao lado dela.</b> Cada bloco dele
     * encosta no corredor ou no bloco anterior do próprio bolsão, então a
     * contiguidade continua valendo — o que muda é que a mina passa a ter
     * câmaras, e não um cano de um bloco de largura.
     *
     * <p>Ganha-se mais que a aparência: parede exposta é onde
     * {@code OreVein.beside} enxerga minério, e um bolsão de três por dois
     * mostra <b>doze</b> paredes novas onde o corredor mostraria duas.
     */
    private ColonyPos tunnel(int i) {
        int cycle = i / GALLERY_CYCLE;
        int within = i % GALLERY_CYCLE;

        int base = cycle * RUN;

        if (within < RUN_BLOCKS) {
            return at(base + within / HEADROOM + 1, 0, within % HEADROOM);
        }

        int j = within - RUN_BLOCKS;

        int deep = j / (POCKET_LONG * HEADROOM) + 1;
        int rest = j % (POCKET_LONG * HEADROOM);

        // As últimas colunas do trecho, e não as primeiras: o bolsão se
        // abre quando o corredor já passou por ele.
        return at(base + RUN - rest / HEADROOM, pocketSide(cycle) * deep, rest % HEADROOM);
    }

    /**
     * Uma posição da galeria: quantas colunas adiante, quanto de lado, e
     * qual das duas alturas.
     */
    private ColonyPos at(int step, int lane, int high) {
        Side towards = descent.clockwise();

        ColonyPos floor = landingTwo();

        ColonyPos from = new ColonyPos(
                floor.x() + towards.offsetX() * ROOM_LONG,
                floor.y(),
                floor.z() + towards.offsetZ() * ROOM_LONG);

        Side sideways = gallery.clockwise();

        return new ColonyPos(
                from.x() + gallery.offsetX() * step + sideways.offsetX() * lane,
                from.y() + 1 + high,
                from.z() + gallery.offsetZ() * step + sideways.offsetZ() * lane);
    }

    /**
     * De que lado do corredor este bolsão se abre: {@code -1} ou
     * {@code +1}.
     *
     * <p><b>O "aleatório" do pedido, e ele não pode ser sorteio.</b> A
     * ordem de cavar é indexada por um cursor gravado no save, então
     * {@code positionAt} tem de responder a mesma coisa hoje e depois de
     * reiniciar o servidor. Um {@code Random} daria uma mina diferente a
     * cada carregamento, e o cursor passaria a apontar para outro lugar.
     *
     * <p>Então é ruído: função pura da boca da mina, do lado da galeria e
     * do número do ciclo. Duas colônias cavam minas diferentes, a mesma
     * colônia cava a mesma mina sempre, e as quatro direções da galeria
     * não repetem o desenho uma da outra.
     */
    private int pocketSide(int cycle) {
        int noise = entry.x() * 73_856_093 ^ entry.y() * 19_349_663 ^ entry.z() * 83_492_791;

        noise = noise * 31 + gallery.ordinal();
        noise = noise * 31 + cycle;

        noise ^= noise >>> 15;
        noise *= 0x2c1b3c6d;
        noise ^= noise >>> 13;

        return (noise & 1) == 0 ? -1 : 1;
    }
}
