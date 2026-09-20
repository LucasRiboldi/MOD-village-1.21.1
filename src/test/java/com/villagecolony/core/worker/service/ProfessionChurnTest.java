package com.villagecolony.core.worker.service;

import com.villagecolony.core.type.Capability;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O rodízio de ofícios — sessão de jogo de 2026-09-19.
 *
 * <p><b>O que o log mostrou.</b> Um trabalhador
 * ({@code 5afa6bca}) largou <b>seis ofícios diferentes</b> em 41 minutos,
 * e voltou a {@code COLLECT_STONE} três vezes e a
 * {@code BUILD_STRUCTURE} três vezes. Nenhum dos dois é o que a linha de
 * reserva promete: o castigo do primeiro deveria estar de pé quando ele
 * voltou.
 *
 * <p><b>Por que o {@link ProfessionShunTest} não pegou.</b> Lá o
 * trabalhador larga o ofício e <b>fica parado</b> enquanto os ciclos
 * passam — {@code cyclesGoBy} só anda o relógio. Em jogo ele é
 * recontratado <b>no mesmo ciclo</b>, trava de novo no ofício novo, e
 * desiste outra vez antes de o primeiro castigo ter andado. A cobertura
 * media o castigo em repouso, e o defeito mora no movimento.
 */
class ProfessionChurnTest {

    private static final UUID COLONY = UUID.randomUUID();

    private WorkerService workers;

    @BeforeEach
    void setUp() {
        workers = new WorkerService();
    }

    private Worker aWorker() {
        UUID villagerId = UUID.randomUUID();

        workers.register(villagerId, COLONY);

        return workers.find(villagerId).orElseThrow();
    }

    private Set<UUID> everyone() {
        Set<UUID> ids = new HashSet<>();

        for (Worker worker : workers.all()) {
            ids.add(worker.villagerId());
        }

        return ids;
    }

    /**
     * A capacidade que cada ofício exerce, para travá-lo como o jogo
     * travou.
     */
    private static Capability capabilityOf(ProfessionType type) {
        return switch (type) {
            case MINER -> Capability.COLLECT_STONE;
            case LUMBERJACK -> Capability.COLLECT_WOOD;
            case MASON, SMELTER -> Capability.CRAFT_STONE;
            default -> Capability.BUILD_STRUCTURE;
        };
    }

    /**
     * <b>A colônia impossível.</b> É o deserto do log: a mina sem pedra
     * alcançável e o lote sem posição de pé. Todo ofício trava, e o
     * trabalhador desiste de cada um ao terceiro tropeço.
     *
     * <p>O que se mede é o <b>número de ofícios queimados</b>. A linha de
     * reserva promete escada: oito passagens no primeiro castigo,
     * dobrando. Um trabalhador não pode atravessar os sete ofícios da
     * colônia antes de o primeiro castigo vencer.
     */
    @Test
    void theWorkerDoesNotBurnThroughEveryTradeBeforeTheFirstPunishmentEnds() {
        Worker worker = aWorker();

        Set<ProfessionType> burned = new HashSet<>();

        // Menos passagens que o primeiro castigo: dentro desta janela o
        // ofício que ele largou primeiro AINDA está de fora, e ele não
        // deveria ter tido tempo de queimar a colônia inteira.
        for (int cycle = 0; cycle < Worker.SHUN_CYCLES; cycle++) {
            ProfessionAssigner.assignMissing(workers, COLONY, everyone(), 30);

            worker.profession().ifPresent(burned::add);

            // Ele trava no ofício novo, como travou em jogo: três
            // desistências na janela e ele larga.
            worker.profession().ifPresent(type -> {
                Capability capability = capabilityOf(type);

                worker.rest(capability);
                worker.rest(capability);
                worker.rest(capability);
            });

            worker.aCycleWentBy();
        }

        assertTrue(
                burned.size() < ProfessionAssigner.PRODUCER_ORDER.size(),
                "ele passou por TODOS os " + burned.size() + " ofícios da colônia"
                        + " dentro do primeiro castigo — a linha de reserva virou"
                        + " rodízio, e é o que a sessão de 09-19 filmou");
    }
}
