package com.villagecolony.fabric.brain;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Para onde a colônia quer que cada aldeão vá.
 *
 * <p>É a única coisa que o trabalho da colônia diz ao {@code Brain}: um
 * destino por aldeão, posto pelo ciclo e lido pela
 * {@link GoToWorkTargetTask} enquanto existir. Sem destino, o aldeão é
 * Vanilla — a task nem começa, e nada da agenda dele muda.
 *
 * <p><b>Por que não é uma memória customizada.</b> A ADR-004 §5 previa
 * {@code villagecolony:task_target} como {@code MemoryModuleType}. Um
 * Brain só guarda memórias declaradas na lista estática
 * {@code VillagerEntity.MEMORY_MODULES}: {@code Brain.setMemory} ignora
 * em silêncio qualquer tipo que não esteja lá. Registrar a memória
 * exigiria um segundo mixin, sobre um campo estático, para reescrever
 * uma lista imutável de Vanilla — mais superfície de conflito com outros
 * mods de aldeão do que a ADR-004 §7 aceita, e para guardar um dado que
 * não precisa ser salvo no aldeão. O destino é intenção do momento, como
 * a própria tarefa, que também não é persistida.
 *
 * <p>Consequência assumida: o destino se perde ao reiniciar o servidor.
 * O ciclo seguinte o repõe, porque a tarefa continua na fila.
 */
public final class WorkTargets {

    /**
     * A folga com que a navegação se dá por chegada, quando quem põe o
     * destino não pede outra.
     *
     * <p>Dois blocos, e o número é do <b>lenhador</b>: o destino dele é
     * a própria árvore, o braço é maior que isso, e parar dois antes é
     * parar dentro do alcance — andar até encostar no tronco seria pior.
     *
     * <p><b>Não serve para todo mundo, e a sessão de 2026-08-28 mostrou
     * a quem não serve.</b> Quando o destino já é o lugar exato de ficar
     * de pé — o caso do mineiro —, parar dois antes é <b>não chegar</b>.
     * Ver {@code MinerReach.ARRIVAL}.
     */
    public static final int DEFAULT_ARRIVAL = 2;

    /**
     * Um destino e a folga com que ele conta como alcançado.
     *
     * @param at para onde ir
     * @param arrival a que distância a navegação pode parar e ainda
     *     dizer que chegou
     */
    private record Destination(BlockPos at, int arrival) {
    }

    /**
     * Escrito pelo ciclo da colônia, lido pela thread do servidor no
     * tick do Brain. São a mesma thread hoje; o mapa concorrente é para
     * que deixar de ser não vire uma corrida silenciosa.
     */
    private static final Map<UUID, Destination> TARGETS = new ConcurrentHashMap<>();

    private WorkTargets() {
    }

    /** Manda este aldeão até aqui. Repor o mesmo destino é barato. */
    public static void set(UUID villagerId, BlockPos target) {
        set(villagerId, target, DEFAULT_ARRIVAL);
    }

    /**
     * O mesmo, dizendo de quanta folga este destino aguenta.
     *
     * <p><b>Quem sabe de quanto precisa é quem põe o destino</b>, e não
     * a task que anda: para o lenhador o destino é a árvore e sobra
     * braço; para o mineiro o destino já é onde ele tem de ficar de pé,
     * e o que sobra é nada.
     */
    public static void set(UUID villagerId, BlockPos target, int arrival) {
        TARGETS.put(villagerId, new Destination(target.toImmutable(), arrival));
    }

    public static Optional<BlockPos> of(UUID villagerId) {
        return Optional.ofNullable(TARGETS.get(villagerId)).map(Destination::at);
    }

    /**
     * Com que folga este aldeão conta como chegado.
     *
     * <p>{@link #DEFAULT_ARRIVAL} para quem não tem destino: a pergunta
     * não tem sentido ali, e devolver a folga de casa é o que não muda
     * comportamento nenhum.
     */
    public static int arrivalOf(UUID villagerId) {
        Destination destination = TARGETS.get(villagerId);

        return destination == null ? DEFAULT_ARRIVAL : destination.arrival();
    }

    /**
     * Cede o aldeão de volta à agenda Vanilla.
     *
     * <p>Chamado quando a tarefa termina, é solta ou o trabalhador
     * morre. É a "cessão imediata" da ADR-004 §5.
     */
    public static void clear(UUID villagerId) {
        TARGETS.remove(villagerId);
    }

    /** Ao parar o servidor, junto com o resto do estado em memória. */
    public static void clearAll() {
        TARGETS.clear();
    }
}
