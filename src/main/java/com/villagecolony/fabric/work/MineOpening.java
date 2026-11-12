package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Escolhe a boca e abre a mina da colônia, com o arco — a mesma escolha para o mineiro ({@link MineDigging}) e para a
 * fundação da vila ({@link MinerHomeFoundation}, Regra 260). A boca nasce dentro da caixa, longe da água (ADR-038 P3b);
 * com a caixa medida, só na faixa da borda (Regra 53).
 */
final class MineOpening {

    private MineOpening() {
    }

    /**
     * Abre a mina agora. Vazio quando nenhuma coluna serve de boca, e o motivo fica no {@link IdleLog}.
     *
     * @param opener quem abre, só para o log: o id do mineiro, ou a fundação
     */
    static Optional<Mine> open(ServerWorld world, UUID colonyId, BlockPos center, String opener) {
        Side descent = MineCuts.sideOf(colonyId);

        // A boca nasce dentro da caixa, longe da água (ADR-038 P3b).
        Optional<MineEdge.Choice> edge = MineEdge.inside(world, colonyId, center, null);

        if (edge.isPresent()) {
            descent = edge.get().side();
        }

        // Com a caixa medida, a boca só nasce na faixa da borda (Regra 53).
        Optional<VillageBounds> box = MineEdge.boxOf(colonyId);
        Optional<BlockPos> mouth = edge.isPresent()
                ? Optional.of(edge.get().mouth())
                : box.isPresent() ? Optional.empty() : MineSite.mouthOf(world, center, descent);
        Optional<WaterMineAccess.Route> waterAccess = Optional.empty();

        if (mouth.isEmpty()) {
            waterAccess = WaterMineAccess.find(world, center, descent)
                    .filter(route -> box.isEmpty() || MineEdge.nearTheEdge(box.get(), route.entry()));

            if (waterAccess.isPresent() && waterAccess.get().place(world)) {
                mouth = Optional.of(waterAccess.get().entry());
                VillageColonyMod.LOGGER.info(
                        "Miner {} opens a sealed water mine access at {}",
                        opener,
                        mouth.get().toShortString());
            }
        }

        if (mouth.isEmpty()) {
            // Sem esta linha o mineiro ficava "looking for stone" para sempre e nada dizia que a mina sequer tinha
            // onde nascer. Assunto próprio porque MinerWork.run limpa o dele quando há tarefa aberta.
            IdleLog.record(
                    colonyId,
                    MineDigging.MOUTH_SUBJECT,
                    IdleReason.NO_TARGET,
                    box.isPresent()
                            ? "no column near the edge of the village box can hold a mine mouth"
                            : "no column within " + MineSite.distance() + " blocks of " + center.toShortString()
                                    + " can hold a mine mouth — tried 8 directions at 5 distances,"
                                    + " the last two settling for a poor one");

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MineDigging.MOUTH_SUBJECT);

        Mine opened = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), descent));

        // A boca de superfície fica na memória: o nível desce, a entrada não (MineEntrance).
        MineEntrance.opened(colonyId, opened.entry());

        VillageColonyMod.LOGGER.info(
                "Miner {} opens a mine at {} - a {} step spiral then four {} step branches",
                opener,
                mouth.get().toShortString(),
                MineShaft.DESCENT,
                MineShaft.ARM_STAIRS);

        if (waterAccess.isPresent()) {
            // A saída inferior está dentro da rota selada. O arco normal ocuparia o corredor de três pistas, então
            // esta mina só recebe a iluminação das galerias já abertas.
            MineFurnishing.lightMine(world, opened);
        } else {
            MineFurnishing.furnishAndLight(world, opened);
        }

        return Optional.of(opened);
    }
}
