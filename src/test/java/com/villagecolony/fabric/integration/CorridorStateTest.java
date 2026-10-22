package com.villagecolony.fabric.integration;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O estado da busca do {@link Corridor} se acha no mapa de custo qualquer que seja a prioridade com que saiu
 * da fila — 0.3.89, crash no deserto: o estado ordenava pela prioridade ({@code Comparable}) e a igualdade
 * a ignorava; com muitos estados, o {@code HashMap} vira árvore e procura pela ordem, e o custo sumia.
 */
class CorridorStateTest {

    @Test
    void aStateIsFoundInTheCostMapWhateverItsPriority() throws Exception {
        Class<?> type = Class.forName(Corridor.class.getName() + "$State");
        Constructor<?> make = type.getDeclaredConstructor(
                BlockPos.class, Direction.class, int.class, int.class, int.class, int.class);
        Method withCost = type.getDeclaredMethod("withCost", int.class);

        make.setAccessible(true);
        withCost.setAccessible(true);

        Map<Object, Integer> cost = new HashMap<>();
        List<Object> states = new ArrayList<>();

        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 6; y++) {
                for (int z = 0; z < 12; z++) {
                    for (Direction way : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                        for (int turns = 0; turns <= Corridor.MAX_TURNS; turns++) {
                            Object state = make.newInstance(new BlockPos(x, y, z), way, turns, 1, 1, 0);

                            cost.put(state, states.size());
                            states.add(state);
                        }
                    }
                }
            }
        }

        // O hash do BlockPos se repete com y + 31 e z - 1: doze estados de hash igual no mesmo balde, que
        // o HashMap transforma em árvore.
        for (int k = 0; k < 12; k++) {
            Object state = make.newInstance(new BlockPos(5, 3 + 31 * k, 100 - k), Direction.NORTH, 0, 1, 1, 0);

            cost.put(state, states.size());
            states.add(state);
        }

        for (int i = 0; i < states.size(); i++) {
            Object polled = withCost.invoke(states.get(i), 1000 + i % 97);

            assertEquals(Integer.valueOf(i), cost.get(polled), "o custo do estado " + states.get(i) + " sumiu do mapa");
        }
    }
}
