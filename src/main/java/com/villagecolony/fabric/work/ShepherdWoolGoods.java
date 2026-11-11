package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * A lã guardada vira peça de lã — Regra 240 (autor, 2026-11-11, opção P5): o pastor sem tarefa, com mais de
 * {@value #KEEP} lãs nos baús, faz tapete (2 lãs dão 3) e tinge lã branca com o corante que a vila tiver. A cor de cada
 * lote é sorteada entre as que dá para fazer agora; cada peça para em {@value ValueGoods#CAP}, e a lã nunca desce de
 * {@value #KEEP}. Sem corante nos baús, só a cor da lã que o rebanho deu.
 */
final class ShepherdWoolGoods {

    /** A lã que fica guardada: abaixo disto o pastor não gasta. */
    static final int KEEP = 16;

    private ShepherdWoolGoods() {
    }

    /** O que um lote fez: o produto e quantos. */
    record Made(Item product, int count) {
    }

    /** Um lote sorteado entre os que dá para fazer; vazio quando a lã guardada não passa de {@value #KEEP}. */
    static java.util.Optional<Made> step(ServerWorld world, List<ColonyPos> chests, Random random) {
        int wool = 0;

        for (DyeColor color : DyeColor.values()) {
            wool += ColonyChests.countIn(world, chests, item(color, "wool"));
        }

        List<Runnable> lots = new ArrayList<>();
        List<Made> results = new ArrayList<>();
        Item white = item(DyeColor.WHITE, "wool");

        for (DyeColor color : DyeColor.values()) {
            Item fleece = item(color, "wool");
            Item carpet = item(color, "carpet");
            Item dye = item(color, "dye");

            if (wool - 2 >= KEEP && ColonyChests.countIn(world, chests, fleece) >= 2
                    && ColonyChests.countIn(world, chests, carpet) + 3 <= ValueGoods.CAP) {
                results.add(new Made(carpet, 3));
                lots.add(() -> {
                    ColonyChests.withdraw(world, chests, fleece, 2);
                    ColonyChests.deposit(world, chests, carpet, 3);
                });
            }

            if (color != DyeColor.WHITE && wool > KEEP && ColonyChests.countIn(world, chests, dye) > 0
                    && ColonyChests.countIn(world, chests, white) > 0
                    && ColonyChests.countIn(world, chests, fleece) < ValueGoods.CAP) {
                results.add(new Made(fleece, 1));
                lots.add(() -> {
                    ColonyChests.withdraw(world, chests, white, 1);
                    ColonyChests.withdraw(world, chests, dye, 1);
                    ColonyChests.deposit(world, chests, fleece, 1);
                });
            }
        }

        if (lots.isEmpty()) {
            return java.util.Optional.empty();
        }

        int pick = random.nextInt(lots.size());

        lots.get(pick).run();

        return java.util.Optional.of(results.get(pick));
    }

    /** O item desta cor: {@code red_wool}, {@code red_carpet}, {@code red_dye}. */
    static Item item(DyeColor color, String kind) {
        return Registries.ITEM.get(Identifier.ofVanilla(color.getName() + "_" + kind));
    }
}
