package com.villagecolony.fabric.work;

import com.villagecolony.core.colony.model.Colony;
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

    private FarmPlans() {
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
    }

    /** Se esta planta é uma roça, e não uma casa. */
    public static boolean isFarm(ResourceId id) {
        return id.path().contains("farm");
    }
}
