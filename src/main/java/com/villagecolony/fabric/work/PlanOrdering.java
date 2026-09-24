package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import com.villagecolony.fabric.integration.VillageBiomes;
import net.minecraft.server.world.ServerWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A ordem da lista de plantas: a menor primeiro enquanto não há casa, e as
 * recusadas fora sem esvaziar a lista — separado de {@link HousePlans} em
 * 2026-09-24, quando ele passou de 500 linhas.
 *
 * <p>São decisões puras sobre uma lista, e por isso testáveis sem mundo; ver
 * {@code HousePlansTest}. Os comentários vieram junto sem mudança.
 */
final class PlanOrdering {

    private PlanOrdering() {
    }

    /**
     * A menor planta na frente, enquanto a colônia não tem casa.
     *
     * <p><b>Decisão do autor, 2026-09-15:</b> <i>"dar preferência para a
     * primeira ser uma casa pequena"</i>.
     *
     * <p><b>O que o log de 09-15 mediu:</b> às 20:54:35 a colônia abriu
     * {@code plains_butcher_shop_2}, de 382 blocos, e sete minutos e meio
     * depois a obra continuava em <i>"382 blocks left"</i> — nenhum bloco
     * assentado — segurando a vaga única da colônia:
     * <i>"no building work: one is already open"</i>. Era a terceira sessão
     * seguida em que a maior planta do catálogo trava a vila <b>antes de a
     * primeira casa existir</b>.
     *
     * <p><b>A Regra 25 continua valendo, e ganha uma exceção de
     * arranque.</b> Ela manda levantar a maior planta que couber, e o
     * motivo dela é real: em 2026-08-20 exigir a casa grande em toda parte
     * fez a vila parar de crescer, com três cabanas de pé e o raio de 64
     * varrido sem resposta. Inverter a regra de vez faria a vila virar um
     * bairro de cabanas e as casas do jogo nunca subirem.
     *
     * <p>O que muda é só a <b>primeira</b>: sem nenhuma casa de pé, a
     * colônia começa pela planta que ela levanta sozinha, sem o jogador
     * guardar nada em baú — a mesma cabana que a {@link #plansFor} já
     * descreve como o fim da lista. Levantada essa, a Regra 25 volta
     * inteira, e a vila cresce como o autor decidiu em 08-20.
     *
     * <p><b>Reordena, não encurta.</b> As outras plantas continuam na
     * lista, atrás da menor: se a pequena não couber naquele lote, a
     * varredura desce para a seguinte em vez de a colônia ficar sem
     * resposta. Ver a Regra 25 — a escolha é por lote, não por vila.
     *
     * <p><b>Visível ao pacote para o teste</b>, pelo mesmo motivo que
     * {@link #without}: é decisão, e decisão se afirma sem mundo.
     */
    static List<Blueprint> smallestFirst(List<Blueprint> plans, boolean hasNoHouseYet) {
        if (!hasNoHouseYet || plans.size() < 2) {
            return plans;
        }

        List<Blueprint> reordered = new ArrayList<>(plans);

        // A ordem que chega é decrescente pela Regra 25, então a menor é a
        // última. Invertê-la por inteiro poria a segunda maior em segundo
        // lugar; o que o autor pediu é a menor NA FRENTE, e o resto como
        // estava — a Regra 25 intacta atrás dela.
        reordered.add(0, reordered.remove(reordered.size() - 1));

        return List.copyOf(reordered);
    }

    /**
     * A lista sem as plantas marcadas — e nunca vazia.
     *
     * <p><b>Separada de {@link #plansFor} porque é a decisão, e decisão se
     * afirma sem mundo.</b> Perguntar ao {@code PlanRefusals} varre baú;
     * escolher o que fica da lista não precisa de nada. Com as duas juntas,
     * o único teste possível seria de jogo — e a base já registrou o preço
     * de um filtro que nada exercitava: quando a divisão do fabricante
     * entrou, removido o {@code continue}, <b>701 unitários e 275 testes de
     * jogo continuavam verdes</b>.
     *
     * <p><b>Nunca devolve vazio tendo planta no catálogo.</b> Se todas
     * estiverem marcadas, vale a menor — a última, porque a ordem é
     * decrescente pela Regra 25. A alternativa é a vila parar de planejar
     * por completo, e a Regra 25 existe justamente para isso não acontecer.
     * Ela vai morrer esperando material de novo, e a linha de desistência
     * continua dizendo o que falta, que é melhor que silêncio.
     *
     * <p><b>Visível ao pacote para o teste.</b> {@code HousePlansTest}
     * afirma as duas coisas: que a marcada sai, e que a lista não fica
     * vazia.
     */
    static List<Blueprint> without(List<Blueprint> plans, Set<ResourceId> skipped) {
        List<Blueprint> offered = new ArrayList<>();

        for (Blueprint plan : plans) {
            if (!skipped.contains(plan.id())) {
                offered.add(plan);
            }
        }

        if (offered.isEmpty() && !plans.isEmpty()) {
            return List.of(plans.get(plans.size() - 1));
        }

        return List.copyOf(offered);
    }
}
