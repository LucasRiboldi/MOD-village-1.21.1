package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonySupply;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A planta que a colônia já tentou e não conseguiu levantar — 2026-09-12.
 *
 * <p><b>O laço que ela corta.</b> A Regra 25 oferece da maior planta para
 * a menor, e a escolha é determinística: a primeira que couber no lote. Na
 * sessão de 2026-09-12 isso virou um laço infinito de obras mortas — a
 * colônia escolheu {@code plains_butcher_shop_2} duas vezes seguidas, ficou
 * vinte ciclos esperando {@code smooth_stone_slab}, desistiu nas duas, e
 * teria escolhido a mesma casa na terceira. O autor resumiu o que via:
 * <i>"não vi nenhuma construção nascendo"</i>.
 *
 * <p>A laje está a <b>três</b> degraus de receita — pedregulho, pedra,
 * pedra lisa, laje — e o {@code ColonySupply.RECIPE_DEPTH} é dois. Não era
 * escassez: aquela casa era <b>impossível</b> para aquela colônia, e
 * continuaria sendo para sempre.
 *
 * <p><b>Por condição, e não por prazo.</b> É a diferença que o
 * {@code WorkStall} aprendeu a custo: pendurar a limpeza num momento, em
 * vez de conferir uma invariante, dá marca que expira sozinha e defeito que
 * volta. Aqui a pergunta é <i>a colônia já consegue o que faltou?</i> —
 * então a planta volta à lista no instante em que a resposta mudar, e não
 * quando um relógio mandar. Um fundidor novo, um baú cheio, uma entrega do
 * jogador: qualquer um desfaz a marca.
 *
 * <p><b>Não substitui a Regra 25, desce nela.</b> A ordem continua da maior
 * para a menor; marcar a que falhou faz a lista descer um degrau em vez de
 * reoferecer a mesma casa. É o que o autor pediu — <i>preferir a planta
 * menor que resolve o gargalo</i> — sem trocar a regra dele por uma
 * inversão cega, que faria a vila nunca mais tentar casa grande.
 *
 * <p>Memória de sessão, como o {@code TreeMarks}: o save não leva. Colônia
 * que reabre tenta a casa grande outra vez, gasta um ciclo de paciência e
 * aprende de novo — barato, e a alternativa é gravar no disco uma resposta
 * que o mundo muda sem avisar.
 */
public final class PlanRefusals {

    /**
     * O que faltou à planta que cada colônia largou.
     *
     * <p>Duas chaves, e a de dentro é o <b>id da planta</b>: a colônia
     * pode largar mais de uma — a casa grande e a média podem pedir a
     * mesma laje —, e uma marca só faria a segunda tentativa apagar a
     * primeira.
     *
     * <p>{@code LinkedHashMap} por dentro porque a ordem da lista de
     * materiais é a prioridade do fabricante em todo o resto desta base;
     * aqui ela não decide nada, e manter a ordem evita que a linha de log
     * mude de uma execução para a outra sem nada ter mudado.
     */
    private static final Map<UUID, Map<ResourceId, ResourceId>> MISSING = new HashMap<>();

    private PlanRefusals() {
    }

    /** Esquece tudo. Chamado ao parar o servidor. */
    public static void clearAll() {
        MISSING.clear();
    }

    /**
     * Esta colônia largou esta planta, e por falta de quê.
     *
     * <p>Guarda <b>um</b> material, e não o mapa inteiro: o que decide se a
     * planta volta é haver <b>alguma</b> coisa que a colônia não alcança, e
     * o primeiro que faltou serve de prova. Guardar todos faria a pergunta
     * de volta custar uma varredura de baú por item.
     *
     * @param missing o que a obra esperava e não veio. Vazio não marca
     *     nada — obra largada por outro motivo não condena a planta
     */
    public static void refused(UUID colonyId, ResourceId plan, Map<ResourceId, Integer> missing) {
        if (missing.isEmpty()) {
            return;
        }

        ResourceId first = missing.keySet().iterator().next();

        MISSING.computeIfAbsent(colonyId, id -> new LinkedHashMap<>()).put(plan, first);
    }

    /**
     * Se esta colônia deve pular esta planta agora.
     *
     * <p>Pula enquanto o material que a derrubou continuar fora de alcance.
     * <b>A marca se desfaz sozinha</b> quando o {@code ColonySupply}
     * passar a dizer que sim — e desfazer é apagar, para a pergunta não ser
     * refeita a cada ciclo depois de respondida.
     *
     * @param near de onde se procura baú. A colônia, normalmente
     */
    public static boolean skip(
            ServerWorld world, UUID colonyId, ColonyPos near, ResourceId plan) {

        Map<ResourceId, ResourceId> marks = MISSING.get(colonyId);

        if (marks == null) {
            return false;
        }

        ResourceId missing = marks.get(plan);

        if (missing == null) {
            return false;
        }

        // Pelo bloco, e não pelo ResourceType: o que a obra espera é peça
        // de planta — laje, escada, muro —, e a maior parte delas não é
        // recurso declarado. É a mesma razão por que o produceForWork
        // existe. Ver CraftingWork.MASONRY.
        Optional<Item> item = MinecraftTypeAdapter.toBlock(missing).map(Block::asItem);

        if (item.isEmpty()) {
            // Material que este jogo não tem não vira alcançável nunca, e
            // insistir nele é o laço de novo.
            return true;
        }

        if (!ColonySupply.canProvide(world, colonyId, near, item.get())) {
            return true;
        }

        marks.remove(plan);

        VillageColonyMod.LOGGER.info(
                "Colony {} can reach {} now — {} is back on the list",
                colonyId,
                missing,
                plan);

        return false;
    }

    /** Quais plantas esta colônia está pulando. Para o relatório. */
    public static Set<ResourceId> skipped(UUID colonyId) {
        Map<ResourceId, ResourceId> marks = MISSING.get(colonyId);

        return marks == null ? Set.of() : Set.copyOf(marks.keySet());
    }
}
