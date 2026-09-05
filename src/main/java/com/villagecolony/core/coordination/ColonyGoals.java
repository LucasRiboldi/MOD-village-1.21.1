package com.villagecolony.core.coordination;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Quanto a colônia quer ter de cada recurso.
 *
 * <p>Decisão do autor em 2026-08-08, Regra 1: a colônia colhe até os
 * baús encherem. A meta deixou de ser um número escrito aqui e passou a
 * ser uma propriedade do mundo — o espaço que a colônia tem para
 * guardar.
 *
 * <p>A conta é uma só:
 *
 * <pre>{@code
 * meta = o que já está guardado + o que ainda cabe
 * }</pre>
 *
 * <p>e o déficit que {@code ResourceDemand} tira dela é exatamente o
 * espaço livre. Baú cheio dá meta igual ao estoque, déficit zero e
 * nenhuma tarefa nova — que é o que a regra pede, e o que faz a fila
 * parar de crescer para sempre. Ver §17, erro E1.
 *
 * <p>O número fixo anterior — 64 de madeira e 32 de pedra — foi o que
 * gerou o E1: uma meta constante que o lenhador demorava a atingir fazia
 * a colônia pedir de novo a cada ciclo.
 *
 * <p><b>A tábua entrou na Fase 9</b>, pela Regra 5 do §18, e com outra
 * conta: metade do que os baús comportam. A conta da Regra 1 não serve
 * para ela — um tronco vira quatro tábuas, então fabricar aumenta o
 * volume guardado, e "fabricar até encher" transformaria toda a madeira
 * da colônia em tábua e pararia a coleta junto.
 *
 * <p><b>A pedra saiu.</b> Ninguém minera no MVP, e
 * {@code ColonyCycle.typeFor} manda todo recurso NATURAL para
 * {@code COLLECT_WOOD}: a meta de pedra virava uma tarefa de coleta que
 * só o lenhador podia pegar, e ele derrubava árvore para atendê-la.
 * Volta quando existir minerador, e aí com o espaço dos baús pela mesma
 * conta.
 */
public final class ColonyGoals {

    /**
     * Quanta pedra a colônia mantém guardada mesmo sem obra — decisão do
     * autor, 2026-08-27.
     *
     * <p><b>Isto era zero até hoje</b>, e a razão estava escrita: <i>"a
     * tábua tem meta própria mesmo sem obra; pedra não — ninguém quer um
     * baú cheio de pedregulho por gosto"</i>. A objeção continua certa, e
     * é o próprio piso que a responde: alcançadas as 64, o déficit é zero
     * e nenhuma tarefa nova abre. Piso não é fome sem fim.
     *
     * <p><b>O que a regra antiga custava</b>, medido na sessão das
     * 21:06: dezenove ciclos, dois mineiros capazes, e uma linha só —
     * <i>"no miner work: no task open for it"</i>. A cadeia inteira:
     *
     * <pre>
     * sem lote livre  →  sem obra aberta  →  stoneForWork == 0
     *                 →  sem meta de pedra
     *                 →  sem tarefa de mineração
     *                 →  dois mineiros parados a sessão inteira
     * </pre>
     *
     * <p>E a obra depende de uma varredura que consumiu a sessão toda —
     * dezessete passagens. Sob demanda, na prática, o mineiro quase nunca
     * trabalhava; com piso, a pedra já está lá quando a obra abrir, em
     * vez de a obra esperar por ela.
     *
     * <p><b>Havia um segundo motivo, e ele expirou.</b> O texto antigo
     * dizia que {@code ColonyCycle.typeFor} mandava todo recurso natural
     * para coleta, e a meta de pedra virava tarefa que só o lenhador
     * podia pegar — ele derrubava árvore para atendê-la. Hoje
     * {@code typeFor} decide pela produção declarada: {@code MINED} vira
     * {@code COLLECT_STONE}, e quem a pega é o mineiro.
     *
     * <p><b>Sessenta e quatro</b>, uma pilha: a casa de deserto do
     * catálogo é feita de arenito liso aos sessenta, e uma pilha cobre
     * uma casa com sobra pequena.
     */
    public static final int STONE_FLOOR = 64;

    /**
     * Quanta comida a colônia mantém guardada — 2026-08-27.
     *
     * <p>Mesmo formato do piso da pedra, e pelo mesmo motivo: sem obra
     * nenhuma o fazendeiro não teria tarefa, e seria o mineiro das 21:06
     * de novo — capaz, com baú, e sem nada para fazer a sessão inteira.
     *
     * <p><b>Comida não depende de obra</b>, e é o que a separa da pedra:
     * a vila come todo dia. Uma pilha de trigo é o que a colônia guarda,
     * e o grupo {@code CROPS} faz batata e cenoura contarem para o mesmo
     * piso.
     */
    public static final int FOOD_FLOOR = 64;

    /**
     * Quantas tábuas saem de um tronco.
     *
     * <p>Convenção do jogo, e não conta desta classe: é o que a receita
     * de tábua rende, e o Core não pode perguntar ao livro de receitas
     * (ADR-005). Está aqui porque a reserva de tronco precisa comparar
     * duas medidas da mesma madeira — o que está em tora e o que já
     * virou tábua —, e sem um câmbio entre elas a comparação não existe.
     *
     * <p>Se alguma versão do jogo mudar o rendimento, o que sai errado é
     * a proporção da reserva, e não a colônia: o pior caso é ela guardar
     * tronco a mais.
     */
    public static final int PLANKS_PER_LOG = 4;

    private ColonyGoals() {
    }

    /**
     * Quantas toras ainda podem virar tábua sem furar a reserva.
     *
     * <p><b>A regra do autor</b>, 2026-09-05: <i>"converter somente
     * aproximadamente metade do estoque de troncos em tábuas e preservar
     * o restante como troncos"</i>. Vinte toras pedem dez conversões, e
     * as outras dez ficam — os {@code stripped_oak_log} da casa de
     * planície saem de tora, e não de tábua.
     *
     * <p><b>Mora aqui, e é chamada de dois lugares</b> — a meta, logo
     * abaixo, e o fabricante, que executa. Os dois têm de dizer o mesmo:
     * uma meta que parasse de pedir com um executor que continuasse
     * moendo seria a reserva existindo só no papel, que foi o defeito
     * original com outro nome. É o mesmo argumento que
     * {@code ColonySupply.canProvide} escreve para o par dele.
     *
     * <p>Zero quando a colônia já tem tábua bastante: {@code storedPlanks}
     * é convertido de volta a toras equivalentes, e o que se compara são
     * duas medidas da mesma madeira.
     *
     * @param logs quantas toras a colônia guarda, de qualquer espécie
     * @param storedPlanks quantas tábuas ela guarda, de qualquer espécie
     */
    public static int logsToConvert(int logs, int storedPlanks) {
        return Math.max(0, (logs - storedPlanks / PLANKS_PER_LOG) / 2);
    }

    /**
     * A meta desta colônia agora.
     *
     * <p>Recebe estoque e espaço porque os dois mudam a cada ciclo: o
     * jogador tira madeira do baú, quebra um baú, constrói outro. Uma
     * meta guardada envelheceria sem que nada avisasse.
     *
     * @param owned o que a colônia tem, tipicamente o total de
     *     {@code ColonyResources}. Contagem parcial produz meta a menos —
     *     quem chama já recusa decidir sobre leitura incompleta
     * @param woodRoom quantos troncos ainda cabem nos baús da colônia,
     *     medido na camada fabric. "Baús da colônia" são os baús dos
     *     trabalhadores registrados, que é o que {@code StorageRegistry}
     *     conhece; baú comunitário não existe ainda
     */
    public static Map<ResourceType, Integer> of(
            Colony colony, ResourceTally owned, int woodRoom) {

        return of(colony, owned, woodRoom, 0);
    }

    /**
     * @param plankRoom quantas tábuas ainda cabem nos baús da colônia.
     *     Entra na Regra 5 — ver §18 —, que responde "quanto fabricar":
     *     a meta é a da obra e, enquanto não houver obra, metade do que
     *     os baús comportam em tábua.
     *
     *     <p>Metade porque a resposta da Regra 1 não serve aqui: um
     *     tronco vira quatro tábuas, então fabricar aumenta o volume
     *     guardado. "Fabricar até encher" transformaria toda a madeira da
     *     colônia em tábua e pararia a coleta junto, porque é o baú cheio
     *     que faz o lenhador parar.
     */
    public static Map<ResourceType, Integer> of(
            Colony colony, ResourceTally owned, int woodRoom, int plankRoom) {

        return of(colony, owned, woodRoom, plankRoom, 0);
    }

    /**
     * @param planksForWork quantas tábuas a obra em curso ainda pede.
     *     Zero quando não há obra.
     *
     *     <p>É a segunda metade da Regra 5, escrita em 2026-08-13 e
     *     ligada agora que a Fase 10 existe: <b>o que a obra pede vira a
     *     meta</b>, e a metade do armazém deixa de ser teto para virar o
     *     lote de partida. Uma colônia sem obra fabrica até a metade e
     *     para; com obra, fabrica o que a casa consome.
     *
     *     <p>Substitui em vez de somar. Somar faria a colônia guardar
     *     meia despensa de tábua <em>além</em> da casa, e a Regra 1 já
     *     diz que o baú cheio é o que faz o lenhador parar — encher de
     *     tábua o espaço da madeira pararia a coleta que alimenta a
     *     própria obra.
     */
    public static Map<ResourceType, Integer> of(
            Colony colony, ResourceTally owned, int woodRoom, int plankRoom, int planksForWork) {

        return of(
                colony, owned, woodRoom, plankRoom, planksForWork,
                ResourceType.COBBLESTONE, 0, 0);
    }

    /**
     * O mesmo, mais a pedra que a obra pede — 2026-08-20.
     *
     * <p><b>Por que a pedra entra por fora.</b> O número vem de quem
     * sabe o que a obra ainda quer. O que mudou em 2026-08-27 foi o
     * <b>chão</b> dele: até ali, sem obra a pedra não era meta nenhuma —
     * ver {@link #STONE_FLOOR} para a sessão que desmentiu isso.
     *
     * <p>Qual pedra é decisão da paleta do bioma: pedregulho onde há
     * rocha, arenito no deserto. Perguntar sempre por pedregulho daria
     * zero no deserto, e a vila voltaria a não construir.
     *
     * <p>A lã entra pela mesma porta e pelo mesmo motivo: ninguém guarda
     * lã por gosto. Ela é meta quando <b>uma casa está sem cama</b>, e
     * três por cama é o que a receita do jogo pede.
     *
     * @param stone qual pedra esta vila usa
     * @param stoneForWork quanto dela a obra aberta ainda pede
     * @param woolForBeds quanta lã as casas sem cama ainda pedem
     */
    public static Map<ResourceType, Integer> of(
            Colony colony,
            ResourceTally owned,
            int woodRoom,
            int plankRoom,
            int planksForWork,
            ResourceType stone,
            int stoneForWork,
            int woolForBeds) {

        return of(
                colony, owned, woodRoom, plankRoom, planksForWork,
                stone, stoneForWork, woolForBeds, 0);
    }

    /**
     * O mesmo, mais o vidro que a obra pede — e a areia dele.
     *
     * <p><b>Duas metas de uma vez, e é o elo que faltava.</b> Até esta
     * linha o vidro nunca era meta: a colônia tinha um fundidor que sabia
     * fundir e nunca recebia tarefa, e a areia não tinha para quem ser
     * colhida. O único material da cadeia que ainda dependia do jogador.
     *
     * <p>A areia é <b>derivada</b>, e não recebida de fora: uma areia por
     * vidro, que é o que a fornalha do jogo faz. Mesmo espírito das três
     * lãs por cama — quando a conta é a receita do jogo e não uma medida
     * do mundo, ela cabe aqui.
     *
     * <p><b>E é a areia que falta, não a que a obra pede.</b> Pedir areia
     * pelo tamanho da janela ignoraria o vidro já fundido, e a colônia
     * continuaria raspando a praia com o baú cheio de vidro. Desconta-se
     * o que já está guardado, e a meta seca sozinha quando o fundidor
     * alcança a obra.
     *
     * @param glassForWork quanto vidro a obra aberta ainda consome, já
     *     com a vidraça decomposta pela receita. Ver {@code GlassDemand}
     */
    public static Map<ResourceType, Integer> of(
            Colony colony,
            ResourceTally owned,
            int woodRoom,
            int plankRoom,
            int planksForWork,
            ResourceType stone,
            int stoneForWork,
            int woolForBeds,
            int glassForWork) {

        return of(
                colony,
                owned,
                woodRoom,
                plankRoom,
                new WorkDemand(
                        planksForWork, stone, stoneForWork, woolForBeds, glassForWork,
                        0, 0, java.util.Map.of()));
    }

    /**
     * As metas desta colônia, dada a obra que ela tem aberta.
     *
     * <p>É a entrada de verdade; as outras existem para quem só olha uma
     * parte, e todas caem aqui. A demanda da obra virou um tipo em
     * 2026-08-21 — ver {@link WorkDemand}, e o porquê mora lá.
     */
    public static Map<ResourceType, Integer> of(
            Colony colony,
            ResourceTally owned,
            int woodRoom,
            int plankRoom,
            WorkDemand work) {

        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(owned, "owned");
        Objects.requireNonNull(work, "work");

        int planksForWork = work.planks();
        ResourceType stone = work.stone();
        int stoneForWork = work.stoneAmount();
        int woolForBeds = work.wool();
        int glassForWork = work.glass();

        if (woodRoom < 0) {
            throw new IllegalArgumentException("Negative storage room: " + woodRoom);
        }

        if (plankRoom < 0) {
            throw new IllegalArgumentException("Negative plank room: " + plankRoom);
        }

        // OAK_LOG responde pelo grupo inteiro: ResourceDemand compara a
        // meta de uma madeira com a soma de todas. Sessenta e quatro
        // troncos de abeto satisfazem esta linha tanto quanto os de
        // carvalho. Ver ResourceGroup e ResourceDemand.deficit.
        int wood = owned.amountOfGroup(ResourceGroup.WOOD) + woodRoom;

        int storedPlanks = owned.amountOfGroup(ResourceGroup.PLANKS);

        // A capacidade em tábua é o que já está guardado mais o que ainda
        // cabe. A meta é metade dela, e é isso que converge: cada peça
        // feita sobe o guardado e desce o que cabe, até os dois se
        // encontrarem na metade.
        //
        // Sem tronco guardado, porém, a meta é o que já se tem: não se
        // pede o que não há com que fazer. Sem esta linha, uma colônia
        // sem madeira abriria tarefa de fabricação a cada ciclo para o
        // fabricante encerrá-la no tick seguinte, por falta de material
        // — trabalho nenhum e uma linha de log por ciclo, que é o E1
        // voltando por outra porta.
        //
        // A obra, quando existe, manda: a meta é o que ela ainda pede.
        // O guarda de "sem tronco não se pede tábua" continua valendo
        // por cima dela — uma obra que exige quarenta tábuas sem madeira
        // na colônia abriria tarefa de fabricação a cada ciclo para o
        // fabricante encerrá-la no tick seguinte. A obra espera em
        // WAITING_RESOURCES, que é o estado previsto para isso, e quem
        // destrava é a meta de madeira acima.
        int logs = owned.amountOfGroup(ResourceGroup.WOOD);

        int appetite = planksForWork > 0 ? planksForWork : (storedPlanks + plankRoom) / 2;

        // <b>Metade da madeira fica em tora</b> — decisão do autor,
        // 2026-09-05: <i>"converter somente aproximadamente metade do
        // estoque de troncos em tábuas e preservar o restante como
        // troncos"</i>.
        //
        // <b>O que ela conserta.</b> Nada acima reservava tronco: o
        // portão era só "tem pelo menos um", e a meta pedia metade da
        // capacidade em tábua — que numa colônia com baús vazios é
        // praticamente tudo. O fabricante moía o estoque inteiro, e a
        // obra que precisa de tronco <b>direto</b> — os dezesseis
        // {@code stripped_oak_log} da casa de planície — ficava sem
        // matéria-prima. A sessão de 2026-09-04 terminou com 1.257
        // tábuas e 135 toras.
        //
        // <b>Por que a conta é esta, e não "metade do que há agora".</b>
        // Metade do estoque corrente encolhe junto com ele: vinte toras
        // viram dez, e no ciclo seguinte dez viram cinco. A reserva
        // esvaziaria o baú em degraus, só que mais devagar — Zenão, e não
        // reserva.
        //
        // O que fica parado é a <b>proporção</b>: a madeira da colônia é
        // contada inteira, tora e tábua na mesma moeda, e a meta é
        // empatá-las. Vinte toras e nenhuma tábua pedem dez conversões;
        // dez toras e quarenta tábuas — que é o mesmo estoque, depois —
        // já estão empatadas e não pedem nenhuma. O ponto de equilíbrio
        // não se move, e é ele que o autor chamou de metade.
        int planks = logs == 0
                ? storedPlanks
                : Math.min(
                        appetite,
                        storedPlanks + logsToConvert(logs, storedPlanks) * PLANKS_PER_LOG);

        Map<ResourceType, Integer> goals = new LinkedHashMap<>();

        goals.put(ResourceType.OAK_LOG, wood);
        goals.put(ResourceType.OAK_PLANKS, planks);

        // A pedra tem piso desde 2026-08-27 — ver STONE_FLOOR. A obra
        // manda quando pede mais; obra pequena não abaixa o estoque que
        // a colônia mantém para a casa seguinte.
        goals.put(stone, Math.max(stoneForWork, STONE_FLOOR));

        // A despensa — 2026-08-27. Qualquer lavoura conta, pelo grupo.
        goals.put(ResourceType.WHEAT, FOOD_FLOOR);

        if (woolForBeds > 0) {
            goals.put(ResourceType.WHITE_WOOL, woolForBeds);
        }

        if (glassForWork > 0) {
            goals.put(ResourceType.GLASS, glassForWork);

            int glassMissing = glassForWork - owned.amountOf(ResourceType.GLASS);

            if (glassMissing > 0) {
                goals.put(ResourceType.SAND, glassMissing);
            }
        }

        // O carvão da tocha — 2026-08-21. Direto, e sem o passo do meio
        // que o vidro tem: o mineiro traz o carvão pronto da galeria, e
        // não há fornalha entre a mina e a tocha.
        if (work.coal() > 0) {
            goals.put(ResourceType.COAL, work.coal());
        }

        // O que a obra pede e sai da FORNALHA — 2026-08-22, ADR-009.
        //
        // Genérico de propósito: quem entra aqui é todo material da obra
        // cuja produção declarada é SMELTED, e não uma lista de nomes. O
        // caso que o pediu foi o arenito liso — a casa de deserto do
        // catálogo é feita dele aos sessenta, e a colônia só sabia cavar
        // o cru. Material novo que saia de fornalha entra sozinho.
        //
        // O cru vem de cima: a meta de pedra já conta a família inteira,
        // e é dela que o mineiro tira o que a fornalha vai assar.
        work.smelted().forEach((made, amount) -> {
            if (amount > 0) {
                goals.put(made, amount);
            }
        });

        // O ferro do lampião — 2026-08-21, e com o passo do meio de volta:
        // o lingote é da fornalha, e o cru é da mina. Sem as duas metas o
        // fundidor sabia fundir ferro e nenhuma tarefa lhe chegava.
        if (work.iron() > 0) {
            goals.put(ResourceType.IRON_INGOT, work.iron());

            int ingotsMissing = work.iron() - owned.amountOf(ResourceType.IRON_INGOT);

            if (ingotsMissing > 0) {
                goals.put(ResourceType.RAW_IRON, ingotsMissing);
            }
        }

        return Map.copyOf(goals);
    }
}
