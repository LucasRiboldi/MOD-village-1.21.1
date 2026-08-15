package com.villagecolony.core.coordination;

import java.util.Objects;

/**
 * Por que uma profissão não trabalhou neste ciclo.
 *
 * <p>Existe porque o silêncio era indistinguível de trabalho
 * acontecendo. O E14 do §17 custou três sessões de jogo exatamente
 * assim: {@code ConstructionPlanner} tinha cinco saídas silenciosas e,
 * de fora, as cinco davam a mesma coisa — nada. Duas das três sessões
 * foram gastas construindo o instrumento, não achando o defeito.
 *
 * <p>A lição foi aprendida por uma fase só. O lenhador e o fabricante
 * continuavam se calando, e este tipo é o vocabulário que faltava para
 * que os três digam a mesma coisa do mesmo jeito.
 *
 * <p>É um {@code enum} e não uma frase por dois motivos. Frase se
 * compara por texto, e comparar por texto é o que faz um motivo mudar
 * de forma sem ninguém perceber; e um valor pode ser testado, contado e
 * — no dia em que houver tela — traduzido. O detalhe que muda a cada
 * ciclo continua sendo texto, e entra separado: ver
 * {@code IdleLog.record}.
 *
 * <p>Mora em {@code core.coordination} porque é a camada que já casa
 * tarefa com profissão e não pertence a nenhum domínio sozinho — a
 * mesma razão que a emenda da ADR-006 §6 deu para {@code WorkAssignment}
 * morar aqui.
 */
public enum IdleReason {

    /** A colônia não abriu tarefa desta capacidade. Não é erro. */
    NO_TASK("no task open for it"),

    /**
     * Há tarefa e ninguém a assumiu.
     *
     * <p>Diferente de {@link #NO_TASK}: aqui a colônia quis e não
     * conseguiu. Ou não há trabalhador com a capacidade, ou os que há já
     * estão ocupados com outra coisa.
     */
    NO_EXECUTOR("the task is open and no worker took it"),

    /** Já existe trabalho aberto desta natureza. O caso comum e bom. */
    ALREADY_OPEN("one is already open"),

    /** Não há trabalhador com a capacidade que a tarefa exige. */
    NO_WORKER("no worker in the village can do it"),

    /** O trabalhador não tem baú, e sem baú não há onde guardar. */
    NO_STORAGE("the worker has no chest"),

    /** O baú do trabalhador não tem espaço para o que a tarefa renderia. */
    STORAGE_FULL("the chest has no room left"),

    /**
     * A varredura terminou o raio inteiro e não achou alvo.
     *
     * <p>Separado de {@link #SWEEP_INCOMPLETE} porque a diferença entre
     * "não há" e "não terminei de olhar" foi a segunda metade do E14, e
     * afirmar o primeiro quando vale o segundo é o log mentindo
     * justamente no caso que ele existe para explicar.
     */
    NO_TARGET("nothing to work on in the whole radius"),

    /** A varredura não terminou: o orçamento de colunas do ciclo acabou. */
    SWEEP_INCOMPLETE("still sweeping — the budget ran out before an answer"),

    /** O jogo não tem o que a tarefa pede — datapack que saiu, versão que mudou. */
    NOT_IN_GAME("this game does not have what it asks for"),

    /** Falta o material, e a colônia não inventa recurso. */
    MISSING_MATERIAL("the colony has none of the material"),

    /** O lugar existe e não serve. */
    SITE_REFUSED("the lot found cannot be used");

    private final String message;

    IdleReason(String message) {
        this.message = message;
    }

    /**
     * A frase que vai para o log, sem o detalhe do momento.
     *
     * <p>Em inglês, como o resto do log deste projeto. O detalhe —
     * coordenada, contagem, nome de bloco — não entra aqui: ele muda a
     * cada ciclo, e um motivo que muda a cada ciclo faz a linha voltar
     * toda vez, o que derrota o registrador que só fala quando algo
     * muda.
     */
    public String message() {
        return message;
    }

    /** A frase com um detalhe do momento pendurado. */
    public String messageWith(String detail) {
        Objects.requireNonNull(detail, "detail");

        return detail.isBlank() ? message : message + " — " + detail;
    }
}
