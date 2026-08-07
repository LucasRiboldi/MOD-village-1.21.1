package com.villagecolony.core.worker.service;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Dá função aos trabalhadores que ainda não têm nenhuma.
 *
 * <p>Responde à pergunta "de quem esta colônia mais precisa agora?" e
 * atribui. Ver Profession-System.md §"Seleção de Profissão" e
 * §"Nascimento de Novos Aldeões".
 *
 * <p>Lógica pura: recebe o registro, não lê o mundo. Quem descobre que há
 * aldeão novo é {@code fabric.integration.VillagerScanner}.
 *
 * <p>Não decide prioridade de tarefa nem realoca quem já trabalha —
 * apenas preenche vaga. Realocação conforme a necessidade muda é da
 * colônia, e não pertence ao MVP.
 */
public final class ProfessionAssigner {

    private ProfessionAssigner() {
    }

    /**
     * De qual profissão a colônia mais precisa, dado quem ela já tem.
     *
     * <p>Escolhe a de menor contagem, o que cobre as quatro funções antes
     * de duplicar qualquer uma — a necessidade mínima do
     * Profession-System.md: seis aldeões, um de cada.
     *
     * <p>Empate resolvido pela ordem de declaração de
     * {@link ProfessionType}, que é a ordem da cadeia produtiva do MVP:
     * lenhador antes de fabricante, fabricante antes de construtor. Numa
     * colônia recém-detectada todas as contagens são zero, então é essa
     * ordem que decide as primeiras quatro atribuições — e começar pelo
     * construtor, sem madeira nem material, daria um trabalhador sem o
     * que fazer.
     */
    public static ProfessionType mostNeeded(Collection<Worker> colonyWorkers) {
        Objects.requireNonNull(colonyWorkers, "colonyWorkers");

        Map<ProfessionType, Integer> counts = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            counts.put(type, 0);
        }

        for (Worker worker : colonyWorkers) {
            worker.profession().ifPresent(type -> counts.merge(type, 1, Integer::sum));
        }

        ProfessionType scarcest = ProfessionType.values()[0];

        for (ProfessionType type : ProfessionType.values()) {
            if (counts.get(type) < counts.get(scarcest)) {
                scarcest = type;
            }
        }

        return scarcest;
    }

    /**
     * Dá função a todos os trabalhadores sem função de uma colônia.
     *
     * <p>Recalcula a cada atribuição, e não uma vez para o lote: atribuir
     * quatro aldeões de uma colônia vazia de uma só vez daria quatro
     * lenhadores, porque a contagem seria a mesma para todos.
     *
     * <p>Chamável a cada ciclo. Quem já tem função é ignorado, então
     * rodar de novo sem aldeão novo não faz nada.
     *
     * @return quantos receberam função agora
     */
    public static int assignMissing(WorkerService workers, UUID colonyId) {
        Objects.requireNonNull(workers, "workers");
        Objects.requireNonNull(colonyId, "colonyId");

        int assigned = 0;

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession()) {
                continue;
            }

            worker.assign(mostNeeded(workers.ofColony(colonyId)));
            assigned++;
        }

        return assigned;
    }
}
