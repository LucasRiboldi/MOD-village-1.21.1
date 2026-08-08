package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro das colônias em memória.
 *
 * <p>Guarda quais colônias existem e permite encontrá-las. A lógica de
 * simulação pertence a {@code SimulationService}; a gravação em disco
 * pertence a {@code data.save}. Ver ADR-006 §5.
 *
 * <p>Não conhece Minecraft. Quem observa o mundo é a camada
 * {@code fabric}, que converte os tipos antes de chamar este service.
 *
 * <p><b>Thread safety:</b> nenhuma. Esta classe é acessada apenas pela
 * thread do servidor, que é única. Se algum dia for chamada de outra
 * thread, isto precisa mudar deliberadamente.
 */
public final class ColonyService {

    /**
     * Ordem de inserção preservada para que iterações e logs sejam
     * reproduzíveis — depurar simulação com ordem instável é sofrido.
     * Ver Debugging-Strategy.md.
     */
    private final Map<UUID, Colony> colonies = new LinkedHashMap<>();

    /**
     * Cria e registra uma colônia nova no centro informado.
     *
     * <p>Usado quando a detecção encontra uma vila ainda não conhecida.
     * Para recolocar uma colônia vinda do disco, use {@link #register}.
     */
    public Colony createColony(ColonyPos center) {
        Objects.requireNonNull(center, "center");

        Colony colony = Colony.create(UUID.randomUUID(), center);
        colonies.put(colony.id(), colony);

        return colony;
    }

    /**
     * Registra uma colônia já existente, tipicamente vinda do save.
     *
     * @throws IllegalStateException se o id já estiver registrado — isso
     *     indica save corrompido ou detecção duplicada, e sobrescrever em
     *     silêncio esconderia o defeito
     */
    public void register(Colony colony) {
        Objects.requireNonNull(colony, "colony");

        Colony existing = colonies.putIfAbsent(colony.id(), colony);

        if (existing != null) {
            throw new IllegalStateException("Colony already registered: " + colony.id());
        }
    }

    /**
     * Transforma uma vila detectada em colônia.
     *
     * <p>Se já existe colônia dentro de
     * {@link VillageDetector#DUPLICATE_DISTANCE} do centro detectado, ela
     * é a mesma vila: o centro é atualizado e o UUID preservado. Caso
     * contrário nasce uma colônia nova.
     *
     * <p>É isso que impede colônias duplicadas quando o jogador reentra
     * na área e a detecção roda de novo. Ver ADR-003 §4 e §6.
     *
     * @return a colônia resultante, nova ou atualizada
     */
    public Colony adopt(VillageCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");

        Optional<Colony> existing =
                findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE);

        Colony colony = existing.orElseGet(() -> createColony(candidate.center()));

        colony.observe(candidate.center(), candidate.bedCount(), candidate.complete());

        // Detectar exige chunk carregado. Uma colônia observada está,
        // por definição da ADR-002, sendo simulada — inclusive a que
        // acabou de ser lida do save como DORMANT.
        colony.setLifecycle(ColonyLifecycle.ACTIVE);

        return colony;
    }

    public Optional<Colony> find(UUID id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(colonies.get(id));
    }

    /**
     * Colônia mais próxima da posição, dentro do raio informado.
     *
     * <p>Responde "já existe uma colônia aqui?" antes de criar outra.
     *
     * <p>O raio é parâmetro e não constante: quem chama conhece o
     * contexto. Ver ADR-003.
     *
     * @param radius raio de busca em blocos; valores negativos não
     *     encontram nada
     */
    public Optional<Colony> findNearest(ColonyPos position, int radius) {
        Objects.requireNonNull(position, "position");

        if (radius < 0) {
            return Optional.empty();
        }

        long maxDistanceSquared = (long) radius * radius;

        Colony nearest = null;
        long nearestDistance = Long.MAX_VALUE;

        for (Colony colony : colonies.values()) {
            long distance = position.horizontalDistanceSquared(colony.center());

            if (distance <= maxDistanceSquared && distance < nearestDistance) {
                nearest = colony;
                nearestDistance = distance;
            }
        }

        return Optional.ofNullable(nearest);
    }

    /** Todas as colônias, em ordem de registro. Somente leitura. */
    public Collection<Colony> all() {
        return Collections.unmodifiableCollection(colonies.values());
    }

    /**
     * Remove uma colônia do registro.
     *
     * @return true se havia uma colônia com esse id
     */
    public boolean remove(UUID id) {
        if (id == null) {
            return false;
        }

        return colonies.remove(id) != null;
    }

    public int count() {
        return colonies.size();
    }

    /** Esvazia o registro. Usado ao descarregar o mundo. */
    public void clear() {
        colonies.clear();
    }
}
