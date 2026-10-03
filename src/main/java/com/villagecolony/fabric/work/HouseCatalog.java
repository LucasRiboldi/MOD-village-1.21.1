package com.villagecolony.fabric.work;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.integration.VillageStructures;
import net.minecraft.server.world.ServerWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;

/**
 * As casas que o catálogo oferece para um estilo de vila — separado de
 * {@link HousePlans} em 2026-10-02 pelo teto de 500 linhas; o texto veio sem
 * mudança. A leitura das plantas continua no cache de {@link HousePlans#READ}.
 */
final class HouseCatalog {

    private HouseCatalog() {
    }

    /**
     * Quantas plantas a busca de lote experimenta por coluna.
     *
     * <p>A Regra 25 manda oferecer da maior para a menor, e a Regra 27
     * deu trinta e seis casas por bioma. Trinta e seis tamanhos por
     * coluna de estrada seria uma varredura trinta e seis vezes mais
     * cara, e a de hoje já leva dez minutos.
     *
     * <p>Quatro é o corte, e é generoso: os tamanhos são poucos e
     * repetidos — a maioria das casas de um bioma divide a mesma pegada.
     * O que se perde é a casa de tamanho raro num lote apertado, e o que
     * se ganha é a colônia continuar planejando dentro de um tique.
     */
    private static final int PLANS_OFFERED = 4;

    /**
     * O que esta vila pode levantar, da maior planta para a menor.
     *
     * <p><b>Só o que está no catálogo</b> — a Regra 27, e ela é imutável.
     * Até 2026-08-20 a colônia levantava uma cabana escrita em código,
     * criada pela Regra 13 porque a casa do jogo era impossível com o que
     * ela produzia. A resposta passou a ser outra: a casa do jogo pede
     * pedra, então a colônia aprendeu a minerar.
     *
     * <p>Tamanhos repetidos entram uma vez só. Oferecer duas casas da
     * mesma pegada faria a busca medir o mesmo lote duas vezes para dar a
     * mesma resposta.
     *
     * <p><b>Mas a irmã descartada não some</b> — 2026-09-18. O corte
     * acima é da <b>busca</b>, e só dela: quem mede lote não ganha nada
     * vendo duas casas 9×9. Quem <b>levanta</b> ganha tudo. Até hoje a
     * vila saía com a mesma estrutura sempre, e a causa era esta linha
     * jogando fora as sete outras {@code small_house} antes de qualquer
     * escolha. Ver {@link PlanPlacement#siblingsOf}, que as devolve ao planejador
     * depois de o lote estar achado — custo zero na varredura.
     */
    static List<Blueprint> catalogPlans(ServerWorld world, String style) {
        List<Blueprint> plans = new ArrayList<>();

        Set<ColonyPos> sizes = new HashSet<>();

        for (ResourceId id : VillageStructures.housesFor(style)) {
            if (!HousePlans.isHouse(id)) {
                continue;
            }

            Optional<Blueprint> house = HousePlans.READ.computeIfAbsent(
                    id, missing -> StructureBlueprintReader.read(world, missing));

            if (house.isEmpty()) {
                continue;
            }

            if (!HousePlans.hasBed(house.get())) {
                continue;
            }

            plans.add(house.get());
        }

        plans.sort(Comparator.comparingInt(HousePlans::volumeOf).reversed());

        List<Blueprint> offered = new ArrayList<>();

        for (Blueprint plan : plans) {
            if (!sizes.add(plan.size())) {
                continue;
            }

            offered.add(plan);

            if (offered.size() == PLANS_OFFERED) {
                break;
            }
        }

        return List.copyOf(offered);
    }
}
