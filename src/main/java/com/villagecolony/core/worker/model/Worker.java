package com.villagecolony.core.worker.model;

import com.villagecolony.core.type.Capability;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Um aldeão que pertence a uma colônia.
 *
 * <p>Modelo de dados: guarda estado e valida o que recebe. Não decide
 * profissão, não executa tarefas e não move o aldeão. Ver Data-Model.md
 * e CODE-STANDARDS.md §5.
 *
 * <p>O trabalhador não é uma entidade nova: {@link #villagerId()} aponta
 * para o {@code VillagerEntity} Vanilla, que continua existindo com sua
 * profissão e sua rotina. Ver PROJECT_CONSTITUTION.md §4.
 *
 * <p>A colônia é referenciada por id, e não por objeto, porque um domínio
 * do Core não importa outro. Ver ADR-006 §6.
 *
 * <p>Os campos {@code storageId}, {@code state} e {@code currentTask}
 * previstos em Data-Model.md ainda não existem: dependem dos sistemas de
 * armazenamento e de tarefas.
 */
public final class Worker {

    /**
     * Por quantas passagens da distribuição uma capacidade descansa.
     *
     * <p>A distribuição roda uma vez por ciclo da colônia, então contar
     * passagens é contar ciclos — e a conta não precisa de
     * {@code world.getTime()}, que o Core não conhece (ADR-005).
     *
     * <p>Quatro ciclos são dois minutos, que é a mesma ordem de grandeza
     * do guarda de travamento que põe a capacidade para descansar. Curto
     * de propósito: o descanso existe para desempatar a escolha da
     * passagem seguinte, e não para aposentar a profissão de ninguém.
     */
    public static final int REST_CYCLES = 4;

    /**
     * Por quantas passagens o ofício abandonado fica de fora, na primeira
     * desistência.
     *
     * <p>Oito, e não os quatro do {@link #REST_CYCLES}: o descanso de
     * capacidade desempata a escolha da passagem seguinte, e este tira o
     * trabalhador de um ofício inteiro. Curto demais e ele volta antes de
     * a colônia ter mudado de estado; era esse o defeito do descanso, que
     * numa colônia de uma tarefa só não dura um ciclo (E43).
     */
    public static final int SHUN_CYCLES = 8;

    /** Quantas vezes o castigo do ofício pode dobrar. */
    private static final int MAX_SHUN_DOUBLINGS = 3;

    /** E o teto, em múltiplos do prazo base: sessenta e quatro passagens. */
    private static final int MAX_SHUN_FACTOR = 8;

    /**
     * Por quantas passagens se lembra <b>quantas vezes</b> o ofício
     * falhou.
     *
     * <p>Mais longo que o maior castigo, e é o que faz a escada existir:
     * se a contagem morresse junto com o castigo, toda desistência seria
     * a primeira e o prazo nunca passaria de oito passagens — que é o
     * laço com outro nome. Mesma razão do {@code TreeMarks.TALLY_MEMORY}.
     */
    private static final int TALLY_CYCLES = 2 * SHUN_CYCLES * MAX_SHUN_FACTOR;

    private final UUID villagerId;

    private final UUID colonyId;

    /**
     * Profissão de colônia, ou {@code null} enquanto não houver.
     *
     * <p>Registrar um aldeão e atribuir-lhe função são momentos
     * diferentes: a detecção registra todos os aldeões da vila, e só
     * depois a colônia decide quem faz o quê. Ver TASK-012 e TASK-013.
     */
    private ProfessionType profession;

    /**
     * As capacidades que travaram para ele, e quantas passagens faltam.
     *
     * <p><b>Mora no trabalhador, e não num mapa estático</b>: é estado
     * dele, morre com ele, e não sobra atrás quando a colônia some. É a
     * diferença entre isto e o {@code TreeMarks}, que é da vila.
     *
     * <p><b>Não vai para o disco</b>, pelo mesmo argumento do
     * {@code blocked} da {@code Mine}: é a contagem de uma sessão, e não
     * um fato sobre o trabalhador. Reabrir o mundo já recusando o próprio
     * trabalho seria pior que a tentativa a mais que isso custa.
     */
    private final Map<Capability, Integer> resting = new EnumMap<>(Capability.class);

    /**
     * Quantas desistências seguidas bastam para ele trocar de ofício.
     *
     * <p>Três, e não uma: desistir uma vez é o caso comum e saudável — a
     * árvore atrás do rio, a pedra emparedada —, e o {@link #rest} já dá
     * a resposta certa para ele. Três dentro da janela é outra coisa: é a
     * capacidade inteira sendo impossível nesta colônia, e nenhuma escolha
     * de alvo conserta isso.
     */
    private static final int STRIKES_BEFORE_GIVING_UP = 3;

    /**
     * Por quantas passagens uma desistência conta para a soma.
     *
     * <p>Doze — três vezes o {@link #REST_CYCLES}, com folga para as três
     * desistências caberem. Sem janela, três azares espalhados por uma
     * hora tirariam o trabalhador do ofício; com ela, o que conta é
     * teimosia dentro de um intervalo curto.
     */
    private static final int STRIKE_MEMORY = 3 * REST_CYCLES;

    /** As desistências recentes, por capacidade. */
    private final Map<Capability, Integer> strikes = new EnumMap<>(Capability.class);

    /** E por quantas passagens cada soma ainda vale. */
    private final Map<Capability, Integer> strikesLeft = new EnumMap<>(Capability.class);

    /** Os ofícios de que ele desistiu, e quantas passagens faltam. */
    private final Map<ProfessionType, Integer> shunned =
            new EnumMap<>(ProfessionType.class);

    /** Quantas vezes cada ofício já o derrubou. Sobrevive ao castigo. */
    private final Map<ProfessionType, Integer> tally = new EnumMap<>(ProfessionType.class);

    /** E por quantas passagens ainda se lembra dessa contagem. */
    private final Map<ProfessionType, Integer> tallyLeft =
            new EnumMap<>(ProfessionType.class);

    private Worker(UUID villagerId, UUID colonyId, ProfessionType profession) {
        this.villagerId = villagerId;
        this.colonyId = colonyId;
        this.profession = profession;
    }

    /**
     * Registra um aldeão recém-encontrado numa colônia.
     *
     * <p>Nasce sem profissão de colônia.
     */
    public static Worker register(UUID villagerId, UUID colonyId) {
        return new Worker(
                Objects.requireNonNull(villagerId, "villagerId"),
                Objects.requireNonNull(colonyId, "colonyId"),
                null);
    }

    /**
     * Reconstrói um trabalhador a partir de dados salvos.
     *
     * @param profession pode ser {@code null}, para quem ainda não tinha
     *     função quando o mundo foi fechado
     */
    public static Worker restore(UUID villagerId, UUID colonyId, ProfessionType profession) {
        return new Worker(
                Objects.requireNonNull(villagerId, "villagerId"),
                Objects.requireNonNull(colonyId, "colonyId"),
                profession);
    }

    /** Id do {@code VillagerEntity} Vanilla. É a identidade do trabalhador. */
    public UUID villagerId() {
        return villagerId;
    }

    public UUID colonyId() {
        return colonyId;
    }

    /** Vazio enquanto a colônia não tiver dado função a este aldeão. */
    public Optional<ProfessionType> profession() {
        return Optional.ofNullable(profession);
    }

    public boolean hasProfession() {
        return profession != null;
    }

    /**
     * Dá uma função ao trabalhador.
     *
     * <p>Substitui a anterior sem cerimônia: a colônia realoca conforme a
     * necessidade muda, e isso não é erro.
     */
    public void assign(ProfessionType profession) {
        this.profession = Objects.requireNonNull(profession, "profession");
    }

    /** Devolve o trabalhador ao estado sem função. */
    public void unassign() {
        this.profession = null;
    }

    /**
     * Ele desiste do ofício — a linha de reserva, decisão do autor de
     * 2026-09-10.
     *
     * <p><b>Devolver o posto não basta, e é o achado que fez esta peça
     * existir.</b> O {@link com.villagecolony.core.worker.service.ProfessionAssigner}
     * escolhe a profissão de <b>menor contagem</b> na colônia; um mineiro
     * que devolve o posto acaba de abrir a própria vaga, e é ele mesmo o
     * mais escasso no ciclo seguinte. Sem a marca abaixo, a reavaliação
     * de ofício é um {@link #rest} caro, que ainda por cima escreve no
     * save.
     *
     * <p>Por isso desistir <b>marca o ofício</b>, e a marca sobe escada:
     * a segunda desistência é prova de que a primeira não foi azar. É a
     * mesma forma do {@code TreeMarks} e do {@code MineMarks}, contada em
     * passagens da distribuição em vez de tiques — o Core não conhece
     * {@code world.getTime()} (ADR-005).
     *
     * <p>Silencioso para quem não tem ofício: um aldeão sem função
     * chamado por engano não deve ganhar castigo por isso.
     */
    public void giveUpProfession() {
        if (profession == null) {
            return;
        }

        ProfessionType failed = profession;

        int failures = tally.merge(failed, 1, Integer::sum);

        shunned.put(failed, shunCyclesFor(failures));
        tallyLeft.put(failed, TALLY_CYCLES);

        this.profession = null;
    }

    /**
     * Se este ofício está de fora para ele agora.
     *
     * <p>Quem pergunta é a atribuição de profissão, e é a única coisa que
     * separa a linha de reserva de um {@code rest} caro.
     */
    public boolean isShunning(ProfessionType profession) {
        return shunned.containsKey(Objects.requireNonNull(profession, "profession"));
    }

    /**
     * Por quantas passagens um ofício fica de fora depois de tantas
     * desistências.
     *
     * <p>Oito passagens na primeira — quatro minutos, a ordem de grandeza
     * do guarda de travamento que o derrubou —, dobrando até três vezes,
     * com teto de oito vezes o prazo base. Além disso ele para de
     * crescer: castigo sem teto viraria aposentadoria, e o jogador que
     * conserta o terreno tem de ver o mod mudar de ideia.
     */
    static int shunCyclesFor(int failures) {
        if (failures <= 0) {
            return 0;
        }

        int doubled = SHUN_CYCLES << Math.min(failures - 1, MAX_SHUN_DOUBLINGS);

        return Math.min(doubled, SHUN_CYCLES * MAX_SHUN_FACTOR);
    }

    /**
     * Esta capacidade acabou de travar para ele — ADR-010, 2026-09-02.
     *
     * <p><b>Travado não é ocioso.</b> A sessão de 2026-09-02 deixou dois
     * trabalhadores parados por dezesseis e por dois minutos, e nenhum
     * deles estava ocioso pela definição do {@code WorkAssignment}: os
     * dois tinham tarefa aberta. Quem sabe a diferença é o guarda de
     * travamento, e é ele quem chama isto.
     *
     * <p>Não é a árvore nem a pedra que descansa — disso já cuidam o
     * {@code TreeMarks} e o cursor da mina. É <b>este trabalhador
     * tentando este tipo de trabalho</b>.
     *
     * <p>Travar de novo renova o prazo inteiro: a segunda parede é prova
     * de que a primeira não foi azar.
     */
    public void rest(Capability capability) {
        resting.put(Objects.requireNonNull(capability, "capability"), REST_CYCLES);

        // <b>E é aqui que o verificador escala</b> — 2026-09-10, decisão
        // do autor. Esta chamada é a porta por onde toda profissão avisa
        // que desistiu, e por isso é o único lugar do projeto que vê o
        // trabalhador teimar sem precisar saber de mina, árvore ou roça.
        //
        // Descansar a capacidade resolve o caso de UMA parede: a passagem
        // seguinte prefere outra coisa. O que ele não resolve é a colônia
        // em que a capacidade inteira é impossível — a mina sem pedra
        // alcançável, a roça fora do raio —, e aí o trabalhador volta à
        // mesma parede a cada quatro passagens para sempre. Três
        // desistências dentro da janela são prova disso, e a resposta é
        // trocar de ofício. Ver giveUpProfession.
        int strike = strikes.merge(capability, 1, Integer::sum);

        strikesLeft.put(capability, STRIKE_MEMORY);

        if (strike >= STRIKES_BEFORE_GIVING_UP) {
            strikes.remove(capability);
            strikesLeft.remove(capability);

            giveUpProfession();
        }
    }

    /**
     * Quantas desistências ele acumulou nesta capacidade. Para a bateria
     * e para o relatório.
     */
    public int strikesOn(Capability capability) {
        return strikes.getOrDefault(Objects.requireNonNull(capability, "capability"), 0);
    }

    /** Se esta capacidade ainda está de molho para ele. */
    public boolean isResting(Capability capability) {
        return resting.containsKey(Objects.requireNonNull(capability, "capability"));
    }

    /**
     * Passou uma distribuição, e os descansos andam com ela.
     *
     * <p>Chamado pela distribuição, e só para quem ela considera: quem
     * está com tarefa aberta não gasta descanso, porque não é dele que a
     * colônia precisa decidir agora.
     */
    public void aCycleWentBy() {
        resting.replaceAll((capability, left) -> left - 1);

        resting.values().removeIf(left -> left <= 0);

        // E as desistências recentes esquecem-se pela janela delas: o que
        // tira o trabalhador do ofício é teimosia num intervalo curto, e
        // não três azares espalhados por uma hora.
        strikesLeft.replaceAll((capability, left) -> left - 1);

        strikesLeft.entrySet().removeIf(entry -> {
            if (entry.getValue() > 0) {
                return false;
            }

            strikes.remove(entry.getKey());

            return true;
        });

        // E os ofícios abandonados andam no mesmo relógio — 2026-09-10.
        // Dois prazos, e é de propósito: o castigo é curto e a contagem
        // é longa. Se ela vencesse junto, a escada nunca subiria.
        shunned.replaceAll((profession, left) -> left - 1);

        shunned.values().removeIf(left -> left <= 0);

        tallyLeft.replaceAll((profession, left) -> left - 1);

        tallyLeft.entrySet().removeIf(entry -> {
            if (entry.getValue() > 0) {
                return false;
            }

            tally.remove(entry.getKey());

            return true;
        });
    }

    public boolean belongsTo(UUID colonyId) {
        return this.colonyId.equals(colonyId);
    }

    /**
     * Dois trabalhadores são o mesmo quando apontam para o mesmo aldeão.
     *
     * <p>A profissão muda ao longo da vida; o aldeão não.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Worker worker && villagerId.equals(worker.villagerId);
    }

    @Override
    public int hashCode() {
        return villagerId.hashCode();
    }

    @Override
    public String toString() {
        return "Worker[villager=" + villagerId
                + ", colony=" + colonyId
                + ", profession=" + (profession == null ? "none" : profession)
                + "]";
    }
}
