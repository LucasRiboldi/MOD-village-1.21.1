package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintKind;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.ColonyModels;
import com.villagecolony.fabric.integration.FarmBlueprint;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A roça padrão da vila, lida do catálogo do próprio jogo — decisão do
 * autor, 2026-09-05.
 *
 * <p><b>A frase dele:</b> <i>"precisam construir o espaço de plantação
 * padrão e idêntico aos que já vêm na vila do Minecraft"</i>, e
 * <i>"precisam de um espaço livre dentro da vila e não colado em outra
 * estrutura para criar o espaço de plantio"</i>.
 *
 * <p><b>O que ela substitui.</b> Até esta data quem abria roça era o
 * fazendeiro, arando toda terra hidratada que a varredura achasse. A
 * sessão das 20:30 deixou a medida no chão: <b>trinta e quatro blocos
 * arados em trinta e três posições distintas, espalhados por catorze
 * blocos de vila</b> — quadradinhos soltos entre as casas.
 *
 * <p><b>As duas exigências saem de graça do que já existe</b>, e é o
 * ponto desta classe ser pequena:
 *
 * <ul>
 *   <li><i>padrão e idêntica à da vila</i> — a roça já está no catálogo
 *       do jogo, ao lado das casas. Ver {@link VillageStructures#farmsFor};
 *   <li><i>lote livre, não colado em outra estrutura</i> — é exatamente
 *       o que a busca de lote da casa já garante, e a roça passa pela
 *       mesma porta. Nenhuma regra nova de espaçamento foi escrita.
 * </ul>
 *
 * <p><b>A menor primeiro</b>, ao contrário das casas (cuja ordem não
 * favorece tamanho desde a ADR-036): uma roça pequena que nasce alimenta
 * mais que uma grande que nunca cabe.
 */
public final class FarmPlans {

    static {
        ServerMemory.register(FarmPlans.class, FarmPlans::clearAll);
    }

    /** As plantas lidas, por id. Ler um template não é barato. */
    private static final Map<ResourceId, Optional<Blueprint>> READ = new HashMap<>();

    /*
     * Quantas roças a vila levanta: uma por fazendeiro, contando as que a vila
     * gerada já tinha — Regra 52 (autor, 2026-10-08). Era uma a cada vinte
     * aldeões, e a conta não sabia quantos fazendeiros havia. Ver ColonyFarms.
     */

    private FarmPlans() {
    }

    /**
     * As colônias que tentaram roça e não acharam lote ao alcance do
     * fazendeiro, e desde quando — 2026-09-09.
     */
    private static final Map<UUID, Long> POSTPONED = new HashMap<>();

    /**
     * Quanto tempo a roça sai da frente depois de não caber.
     *
     * <p>Uma rodada dá à casa ou oficina a oportunidade de assumir o lote.
     * Vinte rodadas deixavam a vila dez minutos sem projeto mesmo tendo
     * materiais e trabalhadores disponíveis; a próxima tentativa de roça
     * continua preservando a cota e o alcance do fazendeiro.
     */
    private static final int POSTPONE_TICKS = VillageDetector.CYCLE_TICKS;

    /**
     * A roça espera, e as casas passam à frente — 2026-09-09.
     *
     * <p><b>Uma roça que não cabia parava a vila inteira.</b> A sessão de
     * 09-09 mediu a colônia {@code 634bf5cc} presa nisto por uma hora:
     * <b>108 passagens do planejador, 108 respondidas pelo índice</b>, e
     * nenhuma obra aberta. O lote livre mais perto estava fora do alcance
     * do fazendeiro, e a recusa daquele lote encerrava a passagem — sem
     * nunca tentar uma casa.
     *
     * <p>O efeito era a vila parada de verdade, e não só sem roça: sem
     * obra não há pedido de tábua nem de pedra, então o construtor e o
     * mineiro ficavam sem tarefa e o autor viu <b>78 de 108 ciclos com
     * "assigned 0 tasks (0 open)"</b>. A queixa dele foi exatamente essa
     * — <i>"não vi os trabalhadores trabalhando"</i>.
     *
     * <p>A cota continua de pé: a roça não foi cancelada, só cedeu a vez.
     * Passado o prazo ela volta a ser tentada, e a vila que cresceu no
     * meio-tempo pode ter aberto o lote que faltava.
     */
    static boolean postponed(UUID colonyId, long now) {
        Long since = POSTPONED.get(colonyId);

        if (since == null) {
            return false;
        }

        if (now - since >= POSTPONE_TICKS) {
            POSTPONED.remove(colonyId);

            return false;
        }

        return true;
    }

    /** A roça desta colônia cede a vez — ver {@link #postponed}. */
    static void postpone(UUID colonyId, long now) {
        POSTPONED.put(colonyId, now);
    }

    /**
     * As roças que esta vila pode levantar, da menor para a maior.
     *
     * <p>Vazio quer dizer catálogo ausente ou estilo sem roça, e quem
     * chama trata: sem planta não há o que construir, e inventar uma
     * para preencher o silêncio é o que a Regra 27 proíbe.
     */
    public static List<Blueprint> plansFor(ServerWorld world, Colony colony) {
        String style = HousePlans.paletteOf(world, colony.center()).style();

        List<Blueprint> plans = new ArrayList<>();

        for (ResourceId id : VillageStructures.farmsFor(style)) {
            READ.computeIfAbsent(id, missing -> StructureBlueprintReader.read(world, missing))
                    .map(FarmPlans::withoutTheCrops)
                    .ifPresent(plans::add);
        }

        plans.sort(Comparator.comparingInt(FarmPlans::volumeOf));

        return List.copyOf(plans);
    }

    /** A roça sem a lavoura — ver {@link FarmBlueprint#withoutTheCrops}. */
    static Blueprint withoutTheCrops(Blueprint farm) {
        return FarmBlueprint.withoutTheCrops(farm);
    }

    private static int volumeOf(Blueprint plan) {
        return plan.size().x() * plan.size().y() * plan.size().z();
    }

    /** Esquece as plantas lidas. Chamado ao parar o servidor. */
    public static void clearAll() {
        READ.clear();

        POSTPONED.clear();
    }

    /** Se esta planta é uma roça, e não uma casa. */
    public static boolean isFarm(ResourceId id) {
        return BlueprintKind.isFarm(id);
    }
}
