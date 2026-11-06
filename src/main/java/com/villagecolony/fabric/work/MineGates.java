package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/**
 * Os portões de mina de pé em volta da vila, lidos do mundo pela varredura do grafo — Regra 210: o portão entra no grafo como
 * uma porta, sem bloco novo e sem estado em disco. Portão derrubado sai sozinho. A entrada é o primeiro chão do lado de onde se
 * chega, fora da folga da escada, que o caminho não pode aterrar.
 */
final class MineGates {

    static {
        ServerMemory.register(MineGates.class, MineGates::clearAll);
    }

    private static final Map<UUID, Set<MineGate.Standing>> KNOWN = new HashMap<>();

    private MineGates() {
    }

    /** Confere o topo de uma coluna. */
    static void look(ServerWorld world, UUID colonyId, BlockPos top, BlockState state) {
        if (MineGate.isPeak(state)) {
            MineGate.standingAt(world, top)
                    .ifPresent(gate -> KNOWN.computeIfAbsent(colonyId, ignored -> new HashSet<>()).add(gate));
        }
    }

    /** O chão em frente a cada portão de pé. */
    static Set<ColonyPos> fronts(ServerWorld world, UUID colonyId, WorldPathTerrain terrain) {
        Set<ColonyPos> fronts = new HashSet<>();
        Set<MineGate.Standing> known = KNOWN.computeIfAbsent(colonyId, ignored -> new HashSet<>());

        known.removeIf(gate -> !MineGate.stands(world, gate));

        for (MineGate.Standing gate : known) {
            for (int blocks = 1; blocks <= WorldPathTerrain.MINE_CLEARANCE + 2; blocks++) {
                BlockPos out = MineGate.approach(gate.mouth(), gate.descent(), blocks);
                OptionalInt ground = terrain.groundY(out.getX(), out.getZ(), gate.mouth().getY());

                if (ground.isPresent() && !terrain.blocked(out.getX(), out.getZ())) {
                    fronts.add(new ColonyPos(out.getX(), ground.getAsInt(), out.getZ()));
                    break;
                }
            }
        }

        return fronts;
    }

    static void clearAll() {
        KNOWN.clear();
    }
}
