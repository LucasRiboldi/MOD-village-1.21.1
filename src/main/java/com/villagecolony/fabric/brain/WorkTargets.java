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
     * Escrito pelo ciclo da colônia, lido pela thread do servidor no
     * tick do Brain. São a mesma thread hoje; o mapa concorrente é para
     * que deixar de ser não vire uma corrida silenciosa.
     */
    private static final Map<UUID, BlockPos> TARGETS = new ConcurrentHashMap<>();

    private WorkTargets() {
    }

    /** Manda este aldeão até aqui. Repor o mesmo destino é barato. */
    public static void set(UUID villagerId, BlockPos target) {
        TARGETS.put(villagerId, target.toImmutable());
    }

    public static Optional<BlockPos> of(UUID villagerId) {
        return Optional.ofNullable(TARGETS.get(villagerId));
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
