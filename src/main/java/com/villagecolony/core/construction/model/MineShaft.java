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

    /**
     * Quantos degraus cada lance do caracol dá antes de virar — decisão
     * do autor, 2026-09-05: <i>"o caminho que o mineiro cava deve ser
     * espiral circular"</i>.
     *
     * <p><b>A descida virou um caracol.</b> Eram dois lances retos de dez
     * com uma sala em cada patamar; agora são quatro lances de cinco,
     * cada um virando à direita do anterior. Quatro curvas fecham a
     * volta, e a escada <b>volta à coluna da boca</b> vinte blocos
     * abaixo — que é a mesma profundidade de nível de antes, no lugar de
     * um rastro de vinte blocos de comprimento.
     *
     * <p><b>O que se ganha é alcance.</b> A boca ficava a vinte blocos
     * horizontais do fundo do nível, e essa distância entrava inteira na
     * caminhada do mineiro toda vez que ele voltava para depositar —
     * era ela que punha a frente a setenta blocos em 2026-09-04. Um
     * caracol de cinco por cinco não tem rastro: o fundo fica <b>debaixo
     * da boca</b>.
     *
     * <p>E ele resolve de graça o que a escada reta pedia por escrito: um
     * degrau de caracol tem parede dos dois lados o tempo todo, então
     * nunca há o vão de onde o aldeão não alcança nada.
     */
    public static final int HELIX_SIDE = 5;

    /** Quantos lances o caracol dá por nível — quatro é a volta inteira. */
    public static final int HELIX_FLIGHTS = 4;

    /** Quanto o caracol desce por nível. Vinte, como os dois lances de antes. */
    public static final int DESCENT = HELIX_SIDE * HELIX_FLIGHTS;

    /**
     * Quanto a galeria e as salas abrem de altura — três desde
     * 2026-09-05, e é decisão do autor: <i>"adicionar um bloco na altura
     * da mina cavada"</i>.
     *
     * <p>Eram dois, que é exatamente o que um aldeão ocupa parado. A
     * escada já abria três desde 08-27, pelo motivo que o
     * {@link #STAIR_HEADROOM} conta — descer não é cair —, e o corredor
     * plano ficou sendo o único lugar da mina onde ele anda com a cabeça
     * raspando o teto.
     *
     * <p><b>Custa, e a conta é a que estava escrita ali:</b> a galeria é
     * o trecho que não acaba, e cinquenta por cento a mais de altura é
     * cinquenta por cento a mais de picareta por coluna. O autor pediu
     * sabendo — a mina é lugar de aldeão trabalhar, e não um cano.
     */
    public static final int HEADROOM = 3;

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

    /**
     * Quantas pistas a escada tem — duas, desde 2026-09-05.
     *
     * <p>Decisão do autor: <i>"a mina deve descer sempre em escadas
     * duplas para o aldeão descer e subir sem se atrapalharem"</i>.
     *
     * <p>Um corredor de uma coluna é uma via de mão única: dois aldeões
     * em sentidos opostos se empurram, e o de baixo perde a descida que
     * acabou de fazer. Com duas colunas lado a lado cada um tem por onde
     * passar, e a navegação do jogo resolve o desvio sozinha — não é
     * preciso mão nem regra dizendo quem sobe por qual.
     *
     * <p>A segunda pista sai para o lado do rumo da descida, que é o
     * mesmo lado por onde a sala se abre.
     */
    public static final int STAIR_LANES = 2;

    /** Quantas posições um lance do caracol pede. */
    private static final int FLIGHT_BLOCKS = HELIX_SIDE * STAIR_HEADROOM * STAIR_LANES;

    /** A partir daqui é galeria, e ela não acaba. */
    public static final int CARVED = FLIGHT_BLOCKS * HELIX_FLIGHTS;



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
        return new MineShaft(levelFloor(), descent, gallery);
    }

    /** Se ainda há nível abaixo deste, sem passar do {@link #DEEPEST}. */
    public boolean mayDeepen() {
        return deepened().levelFloor().y() >= DEEPEST;
    }

    /**
     * A posição de índice {@code i} na ordem de cavar.
     *
     * <p>Índice acima de {@link #CARVED} é galeria, e por isso não há
     * teto: a mina não acaba, e quem a interrompe é o expediente, a
     * paciência do jogador ou o fim do mundo.
     */
    public ColonyPos positionAt(int i) {
        if (i < CARVED) {
            return helix(i);
        }

        return tunnel(i - CARVED);
    }

    /**
     * Um degrau do caracol — 2026-09-05.
     *
     * <p>Quatro lances por volta, cada um virando à direita do anterior,
     * e cada um usando o mesmo degrau de sempre: duas pistas, três de
     * altura, um bloco adiante e um abaixo. A curva é a única coisa nova.
     */
    private ColonyPos helix(int i) {
        int flight = i / FLIGHT_BLOCKS;

        return stair(cornerOf(flight), facingOn(flight), i % FLIGHT_BLOCKS);
    }

    /**
     * De onde parte o lance de número {@code flight}.
     *
     * <p><b>Fórmula, e não soma do caminho.</b> O
     * {@code MinerReach.legTowards} percorre até duas mil posições todo
     * tique, e uma ordem que precisasse ser acumulada custaria isso ao
     * quadrado — é a mesma razão que já mantinha a galeria periódica.
     *
     * <p>E ela fecha porque o caracol é periódico em quatro: os dois
     * primeiros lances afastam, os dois seguintes trazem de volta, e ao
     * fim da volta o x e o z são os da boca outra vez. Só o y desce.
     */
    private ColonyPos cornerOf(int flight) {
        Side first = descent;
        Side second = descent.clockwise();

        int dx = 0;
        int dz = 0;

        // P[0]=nada, P[1]=primeiro, P[2]=primeiro+segundo, P[3]=segundo.
        if (flight % HELIX_FLIGHTS >= 1) {
            dx += first.offsetX();
            dz += first.offsetZ();
        }

        if (flight % HELIX_FLIGHTS >= 2) {
            dx += second.offsetX();
            dz += second.offsetZ();
        }

        if (flight % HELIX_FLIGHTS == 3) {
            dx -= first.offsetX();
            dz -= first.offsetZ();
        }

        return new ColonyPos(
                entry.x() + dx * HELIX_SIDE,
                entry.y() - flight * HELIX_SIDE,
                entry.z() + dz * HELIX_SIDE);
    }

    /** Para que lado o lance de número {@code flight} desce. */
    private Side facingOn(int flight) {
        Side towards = descent;

        for (int turn = 0; turn < flight % HELIX_FLIGHTS; turn++) {
            towards = towards.clockwise();
        }

        return towards;
    }

    /**
     * Um degrau: as camadas de uma pista, um passo adiante e um abaixo
     * do anterior — e depois as da pista ao lado.
     *
     * <p>As duas pistas do mesmo degrau vêm <b>juntas</b> na ordem, e não
     * uma escada inteira depois da outra: assim o mineiro abre o degrau
     * completo antes de descer para o seguinte, e nunca fica com meia
     * largura aberta debaixo do pé. Ver {@link #STAIR_LANES}.
     */
    private static ColonyPos stair(ColonyPos top, Side towards, int i) {
        int perStep = STAIR_HEADROOM * STAIR_LANES;

        int step = i / perStep + 1;
        int within = i % perStep;

        int lane = within / STAIR_HEADROOM;
        int layer = within % STAIR_HEADROOM;

        // <b>Para a esquerda, e é o caracol que manda</b> — 2026-09-05. O
        // lance seguinte vira à direita, então uma segunda pista à
        // direita cairia dentro dele: a última posição de um lance e a
        // primeira do outro seriam a mesma coluna, e o mineiro bateria a
        // picareta em bloco já aberto duas vezes por curva. À esquerda
        // ela sai por trás da curva, onde ninguém mais cava.
        Side sideways = towards.clockwise().opposite();

        return new ColonyPos(
                top.x() + towards.offsetX() * step + sideways.offsetX() * lane,
                top.y() - step + 1 + layer,
                top.z() + towards.offsetZ() * step + sideways.offsetZ() * lane);
    }

    /**
     * O chão do nível: debaixo da boca, {@link #DESCENT} blocos abaixo.
     *
     * <p><b>Debaixo da boca, e é o ponto do caracol.</b> A escada reta
     * deixava o fundo vinte blocos <b>de lado</b>, e essa distância
     * entrava na caminhada do mineiro toda vez que ele subia para
     * depositar. Quatro curvas fecham a volta e o x e o z voltam a ser os
     * da entrada — a galeria do nível nasce embaixo de quem a mandou
     * cavar.
     */
    private ColonyPos levelFloor() {
        return cornerOf(HELIX_FLIGHTS);
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
        ColonyPos floor = levelFloor();

        Side sideways = gallery.clockwise();

        // <b>A galeria nasce no chão do caracol</b>, e não a alguns
        // blocos dele — 2026-09-05. A sala de sete por quatro fazia a
        // ligação na forma velha; sem ela, começar longe deixaria o
        // corredor solto dentro da rocha, sem tocar a escada por lugar
        // nenhum. As primeiras colunas atravessam a pegada do caracol, e
        // as que já estiverem abertas o nextCut pula de graça.
        return new ColonyPos(
                floor.x() + gallery.offsetX() * step + sideways.offsetX() * lane,
                floor.y() + 1 + high,
                floor.z() + gallery.offsetZ() * step + sideways.offsetZ() * lane);
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
