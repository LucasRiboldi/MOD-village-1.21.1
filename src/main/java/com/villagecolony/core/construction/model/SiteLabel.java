package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A linha que flutua sobre o lote — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"precisa sinalizar um texto igual o nome
 * dos aldeoes mostrando o cone do material que falta, quantos tem em
 * estoque e quantos falta para a construcao finalizar"</i>.
 *
 * <p><b>O log tinha a resposta e ele não.</b> Na sessão de 23:41 a obra em
 * {@code 2503,63,-3031} repetia, a cada trinta segundos,
 * <i>"WAITING_RESOURCES, 382 blocks left, waiting for
 * minecraft:grass_block"</i> — com o estoque da colônia em
 * {@code {OAK_LOG=21, OAK_PLANKS=86, COBBLESTONE=96, DIRT=17}}, zero
 * grama. Dentro do jogo, nada disso aparecia: o autor via partículas de
 * fumaça e não tinha como saber o que a obra esperava.
 *
 * <p><b>Texto, e não ícone de item.</b> O pedido falava em ícone, e um
 * ícone exigiria renderização própria no cliente — mixin, networking,
 * entrypoint de cliente, e o mod deixaria de funcionar com cliente Vanilla
 * puro. O nome do bloco cabe na mesma placa, é lido de longe e não custa
 * nada disso. Ver {@code WorkerNameplate}, que escolheu assim pelo mesmo
 * motivo em 2026-08-08.
 *
 * <p><b>Um material por vez</b>, e é decisão: a obra pode estar esperando
 * cinco coisas, e listar todas viraria uma parede de texto sobre o lote. O
 * primeiro da lista é o que o construtor vai buscar em seguida, que é o
 * que destrava a obra.
 *
 * <p>Mora no Core e se afirma sem mundo, como {@link SiteOutline}: o que
 * costuma ter defeito aqui é a conta e o nome, não o desenho.
 */
public final class SiteLabel {

    /**
     * Por onde se reconhece uma placa de obra.
     *
     * <p>O {@code WorkerNameplate} só desfaz nome que o mod escreveu — a
     * Regra 3 aplicada ao nome do aldeão —, e ele o reconhece comparando
     * com os rótulos de profissão. A placa não é um deles, e sem esta
     * marca ela seria tomada por nome que o jogador deu: o construtor
     * ficaria com "Falta grass_block..." sobre a cabeça <b>para sempre</b>,
     * porque o mod se recusaria a mexer.
     *
     * <p>Público para o {@code WorkerNameplate} poder perguntar.
     */
    public static final String MARK = "Obra";

    private SiteLabel() {
    }

    /**
     * A linha da placa desta obra.
     *
     * @param missing o que ainda falta pôr, por bloco — normalmente
     *     {@code ConstructionProject.remainingMaterials()}
     * @param stock o que a colônia tem nos baús
     * @param blocksLeft quantos blocos faltam para a casa ficar pronta
     * @return uma linha curta, pronta para virar nome flutuante
     */
    public static String of(
            Map<ResourceId, Integer> missing,
            Map<ResourceId, Integer> stock,
            int blocksLeft) {

        return of(missing, stock, blocksLeft, ResourceId::path);
    }

    /**
     * O mesmo, com o nome do bloco na língua do jogo — 2026-09-17.
     *
     * <p><b>Pedido do autor:</b> <i>"deve estar escrito com o nome dos
     * blocos em português, sinalizando quantos tem nos estoques e quantos
     * faltam na estrutura"</i>. A conta de estoque e falta já estava aqui
     * desde 09-15; o que faltava era o nome — a placa dizia
     * {@code grass_block} onde o jogo diz <i>Bloco de Grama</i>.
     *
     * <p><b>Por que uma função, e não uma tabela aqui dentro.</b> Quem
     * sabe traduzir é o próprio jogo, por {@code Block.getName()}, e essa
     * chamada é do {@code fabric} — este arquivo é {@code core} e não
     * conhece Minecraft, pela ADR-005. Escrever uma tabela de nomes aqui
     * seria refazer, com risco de errar e de envelhecer, o que o Vanilla
     * já mantém traduzido em toda língua. É o mesmo argumento do
     * {@code MaterialChoice} ao preferir as tags do jogo a uma lista de
     * espécies.
     *
     * <p>O {@code fabric} passa {@code Block::getName}; os testes passam o
     * que quiserem, e é por isso que a regra continua afirmável sem
     * servidor.
     *
     * @param naming como escrever o nome de um material para o jogador.
     *     Recebe o id e devolve o nome na língua do jogo
     */
    public static String of(
            Map<ResourceId, Integer> missing,
            Map<ResourceId, Integer> stock,
            int blocksLeft,
            Function<ResourceId, String> naming) {

        return of(missing, stock, blocksLeft, naming, Optional.empty());
    }

    /**
     * O mesmo, dizendo o bloco em que o construtor parou — 2026-09-25.
     *
     * <p><b>Visto em jogo.</b> A placa mostrava o <i>primeiro</i> material
     * restante da planta, estivesse ele em falta ou não. No templo de 25-09
     * a obra parou por falta de tocha — a vila não tinha carvão — com 34
     * pedregulhos no baú, e a placa podia anunciar o pedregulho. Quem trava
     * a obra é o <b>próximo bloco</b> da ordem: o construtor para nele
     * quando o material não está nos baús, e é ele que o log chama de
     * {@code waiting for}.
     *
     * <p>A ordem da escolha: o material do próximo bloco, se os baús não
     * têm o que a obra pede dele; senão, o primeiro material que de fato
     * está em falta; e, sem nada em falta, só a contagem de blocos — a obra
     * está andando.
     *
     * @param next o material do próximo bloco que o construtor vai pôr
     */
    public static String of(
            Map<ResourceId, Integer> missing,
            Map<ResourceId, Integer> stock,
            int blocksLeft,
            Function<ResourceId, String> naming,
            Optional<ResourceId> next) {

        return of(missing, stock, blocksLeft, naming, next, 0);
    }

    /**
     * O mesmo, contando as peças que esperam apoio — 2026-09-25.
     *
     * <p>Visto em jogo: com as nove peças do templo adiadas, a placa dizia só
     * "Obra: 9 blocos", e parecia que a obra tinha voltado atrás. A peça
     * adiada não pede material, então não entra na conta da falta; sem esta
     * linha ela sumia da placa junto com o motivo.
     *
     * @param waitingForSupport quantas peças restantes esperam apoio físico
     */
    public static String of(
            Map<ResourceId, Integer> missing,
            Map<ResourceId, Integer> stock,
            int blocksLeft,
            Function<ResourceId, String> naming,
            Optional<ResourceId> next,
            int waitingForSupport) {

        Objects.requireNonNull(missing, "missing");
        Objects.requireNonNull(stock, "stock");
        Objects.requireNonNull(naming, "naming");
        Objects.requireNonNull(next, "next");

        Optional<ResourceId> lacking = next
                .filter(material -> isShort(material, missing, stock))
                .or(() -> missing.keySet().stream()
                        .filter(material -> isShort(material, missing, stock))
                        .findFirst());

        String waiting = waitingForSupport > 0 ? " · " + waitingForSupport + " sem apoio" : "";

        if (lacking.isEmpty()) {
            // Nada a esperar: a obra está andando, e o número que importa
            // é só quanto falta dela.
            return MARK + ": " + blocksLeft + " blocos" + waiting;
        }

        ResourceId material = lacking.get();

        // Zero aparece, e não some — ver o teste. Material ausente do
        // estoque é exatamente o caso que trava a obra, e uma placa que o
        // omitisse deixaria o autor sem saber se falta o material ou se a
        // placa não o conhece.
        int have = stock.getOrDefault(material, 0);

        String name = naming.apply(material);

        // Nome vazio seria placa muda: cai no id, que é feio e informa.
        if (name == null || name.isBlank()) {
            name = material.path();
        }

        return MARK + " · falta " + name + ": " + have + "/" + missing.get(material)
                + " · " + blocksLeft + " blocos" + waiting;
    }

    /** Se os baús têm menos deste material do que a obra ainda pede dele. */
    private static boolean isShort(
            ResourceId material, Map<ResourceId, Integer> missing, Map<ResourceId, Integer> stock) {

        Integer needed = missing.get(material);

        return needed != null && stock.getOrDefault(material, 0) < needed;
    }
}
