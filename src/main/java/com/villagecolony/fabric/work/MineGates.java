package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.MineGate;
import com.villagecolony.fabric.integration.WorldPathTerrain;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** O chão em frente a cada portão de pé; sem portão nenhum (Regra 3), a borda da folga da boca, do lado do centro. */
    static Set<ColonyPos> fronts(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        UUID colonyId = colony.id();
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

        if (known.isEmpty()) {
            mouthFront(world, colony, terrain).ifPresent(fronts::add);
        }

        return fronts;
    }

    private static Optional<ColonyPos> mouthFront(ServerWorld world, Colony colony, WorldPathTerrain terrain) {
        var mine = VillageColonyMod.MINES.of(colony.id());

        if (mine.isEmpty()) {
            return Optional.empty();
        }

        ColonyPos mouth = MineEntrance.surfaceOf(world, colony.id(), mine.get());
        List<Side> sides = new ArrayList<>(List.of(Side.values()));

        sides.sort(Comparator.comparingInt((Side side) -> -(side.offsetX() * (colony.center().x() - mouth.x())
                + side.offsetZ() * (colony.center().z() - mouth.z()))));

        for (Side side : sides) {
            int x = mouth.x() + side.offsetX() * (WorldPathTerrain.MINE_CLEARANCE + 1);
            int z = mouth.z() + side.offsetZ() * (WorldPathTerrain.MINE_CLEARANCE + 1);
            OptionalInt ground = terrain.groundY(x, z, mouth.y());

            if (ground.isPresent() && !terrain.blocked(x, z)) {
                return Optional.of(new ColonyPos(x, ground.getAsInt(), z));
            }
        }

        return Optional.empty();
    }

    static void clearAll() {
        KNOWN.clear();
    }
}
