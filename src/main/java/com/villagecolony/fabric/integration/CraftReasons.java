package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.type.ColonyPos;

import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Por que a peça não foi fabricada — 2026-09-19.
 *
 * <p><b>A pergunta que a sessão de 16:10 abriu.</b> A obra parou <b>22
 * vezes</b> esperando {@code cut_sandstone} com <b>190 arenitos</b> no
 * baú, e o {@code ColonySupply.craft} devolvia {@code false} <b>em
 * silêncio</b>. Três causas moravam nesse silêncio, e elas pedem
 * consertos opostos:
 *
 * <pre>
 * NO_RECIPE      o jogo não tem receita para isto. O conserto é a
 *                planta ou a lista de materiais, nunca a produção.
 * SHORT          a receita existe e falta ingrediente. O conserto é
 *                mandar alguém buscar o que falta.
 * </pre>
 *
 * <p>Do lado de fora as duas são "o fabricante não fez". Distinguir é a
 * diferença entre mexer na cadeia de produção e mexer na planta — e
 * escolher errado custa uma sessão.
 *
 * <p><b>É o mesmo movimento que decidiu o P1.3 e o P1.6</b>, e os dois
 * custaram dias antes de alguém instrumentar: o {@code ProtectionSample}
 * separou <i>"não há chão"</i> de <i>"faltou varredura"</i>, e o
 * {@code VolumeSample} disse de que a Regra 22 era feita.
 *
 * <p><b>Uma linha por item, e não por tentativa.</b> O fabricante repete
 * a pergunta a cada passagem; uma linha por vez afogaria o log e mudaria
 * o que se está medindo.
 */
public final class CraftReasons {

    static {
        ServerMemory.register(CraftReasons.class, CraftReasons::clearAll);
    }

    /** Quantos itens distintos lembrar antes de esquecer tudo. */
    private static final int MAX_ITEMS = 64;

    /** O que já foi dito, para não repetir a cada passagem. */
    private static final Map<Item, String> SAID = new HashMap<>();

    private CraftReasons() {
    }

    /**
     * Diz por que este item não saiu, uma vez por motivo.
     *
     * @param bill a receita que o livro devolveu, vazia quando não há
     */
    public static void couldNotMake(
            Item item,
            Optional<CraftingLookup.Bill> bill,
            ServerWorld world,
            List<ColonyPos> chests) {

        // <b>Vazio NÃO quer dizer "sem receita"</b> — 2026-09-19, e esta
        // foi a primeira coisa que a própria instrumentação pegou: ela
        // disse <i>"this game has no recipe for cut_sandstone"</i> num
        // jogo que tem a receita. O {@code billFor} recebe o predicado
        // do baú, então ele devolve vazio tanto quando <b>não há
        // receita</b> quanto quando <b>o ingrediente não está no
        // baú</b> — que são as duas causas que esta classe existe para
        // separar.
        //
        // A pergunta certa é ao livro <b>sem</b> filtro: se ele responde,
        // a receita existe e o que falta é material.
        Optional<CraftingLookup.Bill> anyRecipe =
                CraftingLookup.billFor(world, item, anything -> true);

        String why;

        if (anyRecipe.isEmpty()) {
            // <b>E "sem receita" ainda era impreciso</b> — 2026-09-19, na
            // segunda leitura da própria instrumentação. O
            // {@code billFor} percorre <b>bancada e cortador</b>, e não a
            // fornalha: para o arenito liso, que é assado, ele não acha
            // nada e a linha saía dizendo que o jogo não tem receita.
            //
            // O jogo tem — ela é de fornalha, e quem a executa é o
            // fundidor por outro caminho. Dizer isso é a diferença entre
            // "a planta pede o impossível" e "a peça é de outra
            // profissão".
            why = CraftingLookup.smeltingInputsFor(world, item).isEmpty()
                    ? "no bench or stonecutter recipe, and the furnace makes none either"
                    : "it comes from the furnace, not the bench — the smelter owns it";
        } else if (bill.isPresent()) {
            why = shortOf(bill.get(), world, chests);
        } else {
            why = shortOf(anyRecipe.get(), world, chests);
        }

        if (why.equals(SAID.get(item))) {
            // Mesmo motivo do que já foi dito: calar é o certo, senão a
            // linha vira enxurrada.
            return;
        }

        if (SAID.size() >= MAX_ITEMS) {
            SAID.clear();
        }

        SAID.put(item, why);

        VillageColonyMod.LOGGER.info("The colony could not make {} — {}", item, why);
    }

    /** Quais ingredientes faltam, e quanto de cada um. */
    private static String shortOf(
            CraftingLookup.Bill bill, ServerWorld world, List<ColonyPos> chests) {

        StringBuilder said = new StringBuilder();

        for (Map.Entry<Item, Integer> part : bill.ingredients().entrySet()) {
            int has = ColonyChests.countIn(world, chests, part.getKey());

            if (has >= part.getValue()) {
                continue;
            }

            if (said.length() > 0) {
                said.append("; ");
            }

            said.append("needs ").append(part.getValue()).append(' ')
                    .append(part.getKey()).append(" and has ").append(has);
        }

        return said.length() == 0
                ? "the recipe closes but the tally disagrees — look at the chest reading"
                : said.toString();
    }

    /** Esquece o que foi dito. Chamado ao parar o servidor. */
    public static void clearAll() {
        SAID.clear();
    }
}
