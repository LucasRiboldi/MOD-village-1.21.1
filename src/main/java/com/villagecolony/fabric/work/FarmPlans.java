package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.block.CropBlock;
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
 * <p><b>A menor primeiro</b>, ao contrário das casas. A Regra 25 oferece
 * a casa da maior para a menor porque casa grande é mais moradia; a roça
 * grande no lugar da pequena é só um lote que não coube — e uma roça
 * pequena que nasce alimenta mais que uma grande que nunca cabe.
 */
public final class FarmPlans {

    /** As plantas lidas, por id. Ler um template não é barato. */
    private static final Map<ResourceId, Optional<Blueprint>> READ = new HashMap<>();

    /**
     * Quantos aldeões cada roça alimenta — decisão do autor, 2026-09-05,
     * <b>revista por ele em 2026-09-10</b>: <i>"a vila deve ter uma
     * plantacao a cada 20 aldoes na vila"</i>. Eram quinze, e a sessão
     * das 22:57 mostrou lavoura demais para o tamanho da vila.
     *
     * <p>O pedido original de 09-05 era <i>"1/15 avos da quantidade de
     * aldeões"</i>; a forma da regra não mudou, só o divisor.
     *
     * <p><b>O que ela substitui é o defeito da véspera.</b> O pedido de
     * roça vinha do fazendeiro — <i>varri o raio e não achei campo</i> —,
     * e um pedido assim não tem teto: enquanto ele não achasse lavoura a
     * colônia levantava roça, e a sessão das 21:17 mostrou <b>duas em
     * quatro minutos</b>, a caminho de encher o mapa. Uma cota fecha
     * isso por construção — quinze aldeões, uma roça, e ponto.
     *
     * <p>E ela é a medida certa por outra razão: roça existe para
     * alimentar gente, então quem manda no tamanho da lavoura é o tamanho
     * da vila, e não o humor da varredura do fazendeiro.
     */
    public static final int VILLAGERS_PER_FARM = 20;

    private FarmPlans() {
    }

    /**
     * Se esta colônia ainda deve uma roça à própria população.
     *
     * <p>Divisão inteira, que é a frase do autor ao pé da letra:
     * dezenove aldeões não pedem roça nenhuma, vinte pedem a primeira, e
     * a segunda só com quarenta.
     *
     * <p><b>Conta as roças que a colônia levantou</b>, e não as que a
     * vila já tinha. É o que o {@code BuildingRegistry} sabe responder
     * sem varrer o mundo — e erra para o lado seguro: uma vila que nasceu
     * com roça ganha um pouco mais de lavoura do que a conta pede, e
     * lavoura a mais é comida a mais.
     */
    public static boolean owedToThePopulation(UUID colonyId) {
        int villagers = VillageColonyMod.WORKERS.countOfColony(colonyId);

        int built = 0;

        for (Building building : VillageColonyMod.BUILDINGS.ofColony(colonyId)) {
            if (isFarm(building.blueprint())) {
                built++;
            }
        }

        return built < villagers / VILLAGERS_PER_FARM;
    }

    /**
     * As colônias que tentaram roça e não acharam lote ao alcance do
     * fazendeiro, e desde quando — 2026-09-09.
     */
    private static final Map<UUID, Long> POSTPONED = new HashMap<>();

    /**
     * Quanto tempo a roça sai da frente depois de não caber.
     *
     * <p>Vinte ciclos, o mesmo fôlego do {@code PatienceClock}: tempo de
     * uma casa subir e mudar o desenho da vila, que é o que pode abrir o
     * lote perto que faltava.
     */
    private static final int POSTPONE_TICKS = 20 * VillageDetector.CYCLE_TICKS;

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

    /**
     * A mesma roça, sem a lavoura plantada — 2026-09-05.
     *
     * <p><b>A obra faz o canteiro; quem planta é o fazendeiro.</b> É a
     * divisão que o autor descreveu, e ela também resolve um problema
     * prático: a lavoura da planta é trigo, cenoura, batata e beterraba,
     * e cobrá-las do baú faria a roça parar esperando semente que a
     * colônia talvez não tenha — a obra parada esperando material é o
     * defeito de 09-04, e não vale repeti-lo para plantar.
     *
     * <p>O canteiro sai arado e vazio, que é exatamente o alvo do ofício
     * {@code SOW}: na passagem seguinte o fazendeiro semeia o que houver
     * no baú dele, e daí em diante colhe e replanta.
     */
    private static Blueprint withoutTheCrops(Blueprint farm) {
        List<BlueprintBlock> kept = new ArrayList<>();

        for (BlueprintBlock block : farm.blocks()) {
            if (!isCrop(block.block())) {
                kept.add(block);
            }
        }

        return kept.size() == farm.blocks().size() ? farm : Blueprint.of(farm.id(), kept);
    }

    /**
     * Se este bloco é lavoura.
     *
     * <p>Pergunta ao bloco, e não a uma lista de nomes: {@code CropBlock}
     * é a resposta do próprio jogo, e vale para trigo, cenoura, batata,
     * beterraba e para o que um datapack plantar depois. É a mesma
     * escolha que o {@code CropPatch} faz desde 2026-08-27.
     */
    private static boolean isCrop(ResourceId block) {
        return MinecraftTypeAdapter.toBlock(block)
                .map(found -> found instanceof CropBlock)
                .orElse(false);
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
        return id.path().contains("farm");
    }
}
