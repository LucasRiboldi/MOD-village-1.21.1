package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide quando uma colônia deixou de ter vila.
 *
 * <p>ADR-003 §6: um aglomerado que deixa de atender à validação não apaga
 * a colônia — apagá-la destruiria o registro de Buildings e violaria o
 * PROJECT_CONSTITUTION.md §10. A colônia fica marcada, e o estado é
 * {@link ColonyState#ABANDONED}.
 *
 * <p><b>Quem tem autoridade para dizer isso.</b> Só a sonda ancorada no
 * centro da própria colônia, e só enquanto ela está sendo simulada. É a
 * mesma disciplina do encolhimento — ver {@code Colony#observe} —, e pelo
 * mesmo motivo: uma colônia dormente tem os chunks descarregados, sua
 * varredura não acharia cama alguma, e toda vila longe do jogador seria
 * declarada morta. "Não fui observada" não é "não existo mais", e essa
 * confusão é justamente o que a ADR-003 §6 manda evitar.
 *
 * <p><b>Por que não exige confirmação em dois ciclos</b>, ao contrário do
 * encolhimento: o veredito não destrói nada e se desfaz sozinho. A
 * primeira varredura que voltar a enxergar vila devolve a colônia a
 * {@link ColonyState#STABLE}. O encolhimento precisa de repetição porque
 * ele <em>perde</em> informação — a contagem menor sobrescreve a maior, e
 * não há como voltar atrás.
 *
 * <p>Puro: recebe o que a varredura viu e devolve o estado, sem tocar no
 * mundo nem no registro. Quem aplica é a camada fabric.
 */
public final class ColonyAbandonment {

    private static final long VIABLE_DISTANCE_SQUARED =
            (long) VillageDetector.DUPLICATE_DISTANCE * VillageDetector.DUPLICATE_DISTANCE;

    private ColonyAbandonment() {
    }

    /**
     * O estado novo desta colônia, quando ele muda.
     *
     * <p>Devolve vazio no caso comum — a colônia continua como estava —
     * para que quem chama saiba quando vale uma linha de log. Uma vila
     * viva produziria uma linha por ciclo, para sempre.
     *
     * @param colony a colônia sondada
     * @param probedFrom o ponto de onde a sonda partiu, que é o centro da
     *     colônia no momento em que a varredura começou. Vem por
     *     parâmetro porque a adoção pode ter movido o centro desde então,
     *     e a pergunta é sobre o que a sonda enxergou de onde estava
     * @param accepted os aglomerados que a varredura aprovou como vila
     * @param sawIgnoredCluster se algum aglomerado aprovado foi
     *     descartado por estar fora de PLAINS. Ele suprime o veredito:
     *     bioma recusado é limite do MVP, não vila morta (ADR-003 §5), e
     *     um centro que caminhou para a borda do bioma condenaria uma
     *     vila cheia de gente
     * @return o estado a aplicar, ou vazio se nada muda
     */
    public static Optional<ColonyState> judge(
            Colony colony,
            ColonyPos probedFrom,
            Collection<VillageCandidate> accepted,
            boolean sawIgnoredCluster) {

        Objects.requireNonNull(colony, "colony");
        Objects.requireNonNull(probedFrom, "probedFrom");
        Objects.requireNonNull(accepted, "accepted");

        if (sees(probedFrom, accepted)) {
            // Vila de volta. Volta a STABLE, e não ao que ela era antes:
            // o estado anterior descrevia uma decisão tomada sobre uma
            // vila que, no meio, deixou de existir. STABLE é o mesmo
            // ponto de partida de uma colônia recém-criada.
            return colony.state() == ColonyState.ABANDONED
                    ? Optional.of(ColonyState.STABLE)
                    : Optional.empty();
        }

        if (sawIgnoredCluster || colony.state() == ColonyState.ABANDONED) {
            return Optional.empty();
        }

        return Optional.of(ColonyState.ABANDONED);
    }

    /**
     * Se algum dos aglomerados aprovados é esta colônia.
     *
     * <p>Mesmo raio que {@code ColonyService#adopt} usa para decidir que
     * dois centros são a mesma vila. Tem de ser o mesmo: um candidato que
     * seria adotado por esta colônia não pode, ao mesmo tempo, deixá-la
     * sem vila.
     *
     * <p>Limite conhecido: com duas colônias sobrepostas, o candidato
     * pode ser adotado pela vizinha e ainda assim contar como vila desta.
     * É o lado seguro do erro — deixar de marcar ABANDONED é preferível a
     * marcar por engano —, e sobreposição já rende aviso próprio (ADR-003
     * §5).
     */
    private static boolean sees(ColonyPos probedFrom, Collection<VillageCandidate> accepted) {
        for (VillageCandidate candidate : accepted) {
            if (candidate.center().horizontalDistanceSquared(probedFrom) <= VIABLE_DISTANCE_SQUARED) {
                return true;
            }
        }

        return false;
    }
}
