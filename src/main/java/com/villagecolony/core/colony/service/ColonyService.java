package com.villagecolony.core.colony.service;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.VillageCandidate;
import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

        colony.observe(
                candidate.center(),
                candidate.bedCount(),
                candidate.complete(),
                candidate.anchor());

        // Detectar exige chunk carregado. Uma colônia observada está,
        // por definição da ADR-002, sendo simulada — inclusive a que
        // acabou de ser lida do save como DORMANT.
        colony.setLifecycle(ColonyLifecycle.ACTIVE);

        return colony;
    }

    /**
     * Uma observação por colônia, dentre as de uma varredura só.
     *
     * <p>Uma varredura devolve um candidato por aglomerado de camas, e
     * dois aglomerados podem cair na mesma colônia:
     * {@link #adopt} considera a mesma vila tudo o que estiver a até
     * {@link VillageDetector#DUPLICATE_DISTANCE} do centro, enquanto
     * {@code VillageDetector.cluster} separa o que estiver a mais de
     * {@code CLUSTER_DISTANCE}. Entre 32 e 64 blocos existe a faixa em
     * que um punhado de camas é, ao mesmo tempo, outro aglomerado e a
     * mesma colônia.
     *
     * <p><b>Por que isto precisa existir.</b> A colônia só encolhe
     * quando a sonda repete a leitura — duas varreduras da mesma âncora
     * vendo o mesmo tanto. A regra pressupõe leituras de ciclos
     * sucessivos, e nada exigia que fossem sucessivas: dois candidatos
     * da mesma varredura chegam com a mesma âncora, o primeiro grava a
     * leitura e o segundo é confirmado por ela no mesmo tick. Uma vila
     * de 31 camas com cinco camas vizinhas desabava para 5, e o centro
     * pulava para o vizinho. Ver §17, E2, e a entrada de 2026-08-11.
     *
     * <p>Vence o maior. Ele é o que viu mais da vila, que é o mesmo
     * critério de {@code Colony#observe}: quem enxergou menos não tem
     * autoridade sobre quem enxergou mais.
     *
     * <p>Candidato que não cai em colônia conhecida passa sempre: cada
     * um vira uma colônia nova, e agrupá-los aqui faria a detecção
     * perder vila. Dois deles perto um do outro continuam sendo
     * resolvidos por {@link #adopt}, um depois do outro, como antes.
     *
     * <p>O agrupamento é calculado antes de qualquer adoção, e é por
     * isso que devolve uma lista em vez de adotar aqui mesmo: adotar
     * move centros, e um centro movido mudaria a resposta de
     * {@link #findNearest} para os candidatos seguintes da mesma
     * varredura.
     *
     * @return os candidatos que devem ser adotados, na ordem em que
     *     chegaram
     */
    public List<VillageCandidate> bestPerColony(Collection<VillageCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");

        Map<UUID, VillageCandidate> bestOf = new LinkedHashMap<>();
        List<VillageCandidate> unknown = new ArrayList<>();

        for (VillageCandidate candidate : candidates) {
            Optional<Colony> owner =
                    findNearest(candidate.center(), VillageDetector.DUPLICATE_DISTANCE);

            if (owner.isEmpty()) {
                unknown.add(candidate);

                continue;
            }

            bestOf.merge(
                    owner.get().id(),
                    candidate,
                    (kept, other) -> other.bedCount() > kept.bedCount() ? other : kept);
        }

        List<VillageCandidate> chosen = new ArrayList<>(bestOf.values());

        chosen.addAll(unknown);

        return List.copyOf(chosen);
    }

    /**
     * As colônias cujo centro está perto demais do desta.
     *
     * <p>ADR-003 §5: dois centros a menos de
     * {@link VillageDetector#OVERLAP_DISTANCE} blocos são uma
     * sobreposição, e o MVP registra o aviso sem fundir nada. Fundir
     * exige nova ADR.
     *
     * <p>Não é hipótese: com raio de varredura de 64 e anti-duplicata de
     * 64, dois aglomerados distintos a sessenta blocos viram duas
     * colônias que se enxergam, e um aldeão que mora numa pode acabar
     * registrado na outra. É o risco aberto do §11 do Project-State, e
     * até aqui ele acontecia em silêncio.
     *
     * <p>A colônia consultada nunca aparece no resultado — ela não se
     * sobrepõe a si mesma.
     *
     * @return as vizinhas sobrepostas, em ordem de registro; vazio no
     *     caso normal
     */
    public List<Colony> overlapping(Colony colony) {
        Objects.requireNonNull(colony, "colony");

        long maxDistanceSquared =
                (long) VillageDetector.OVERLAP_DISTANCE * VillageDetector.OVERLAP_DISTANCE;

        List<Colony> found = new ArrayList<>();

        for (Colony other : colonies.values()) {
            if (other.id().equals(colony.id())) {
                continue;
            }

            if (colony.center().horizontalDistanceSquared(other.center()) <= maxDistanceSquared) {
                found.add(other);
            }
        }

        return List.copyOf(found);
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
