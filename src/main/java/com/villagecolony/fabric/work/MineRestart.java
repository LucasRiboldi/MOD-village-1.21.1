package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A escada recomeça do topo — Regra 119, escolha do autor (2026-10-28). No jogo da 0.3.99 o mineiro passou
 * 30 minutos na boca, 30 blocos acima do nível da mina, sem conseguir descer: a escada entre a superfície e
 * o nível de agora era de antes da mina em caracol, e o roteiro de descida ({@link MineEntrance}) o mandava
 * para dentro da rocha.
 *
 * <p>Com {@link #STRIKES} desistências seguidas em {@link #WINDOW} tiques, de um mineiro a
 * {@link #ABOVE} blocos ou mais acima da entrada do nível, a mina recomeça na mesma boca de superfície com o
 * cursor no primeiro degrau, virando para o rumo seguinte; o que já está aberto passa depressa. Boca nova nunca.
 */
final class MineRestart {

    static {
        ServerMemory.register(MineRestart.class, MineRestart::clearAll);
    }

    /** Quanto acima da entrada do nível o mineiro precisa estar para contar como "não consegue descer". */
    static final int ABOVE = 8;

    /** Quantas desistências seguidas assim recomeçam a escada. */
    static final int STRIKES = 2;

    /** Em quanto tempo elas precisam acontecer: dez minutos. */
    static final long WINDOW = 12_000;

    private record Strikes(int count, long first) {
    }

    private static final Map<UUID, Strikes> STRUCK = new HashMap<>();

    private MineRestart() {
    }

    /** Uma desistência deste mineiro; recomeça a escada quando a conta fecha. */
    static boolean afterGiveUp(ServerWorld world, UUID colonyId, UUID workerId) {
        Optional<Mine> mine = VillageColonyMod.MINES.of(colonyId);

        if (mine.isEmpty() || !(world.getEntity(workerId) instanceof VillagerEntity miner)) {
            return false;
        }

        ColonyPos entry = mine.get().shaft().entry();

        if (miner.getBlockY() - entry.y() < ABOVE) {
            STRUCK.remove(colonyId);
            return false;
        }

        long now = world.getTime();
        Strikes before = STRUCK.get(colonyId);
        Strikes counted = before == null || now - before.first() > WINDOW
                ? new Strikes(1, now) : new Strikes(before.count() + 1, before.first());

        if (counted.count() < STRIKES) {
            STRUCK.put(colonyId, counted);
            return false;
        }

        STRUCK.remove(colonyId);

        return restart(world, colonyId, mine.get());
    }

    /**
     * A mina recomeça na <b>mesma</b> boca de superfície, cursor no primeiro degrau, virando para o outro lado — autor,
     * 2026-10-29 (jogo da 0.3.109: a mina abriu uma boca nova em 3072,73,1281 e voltou a 0 níveis): nunca uma boca
     * nova; a primeira descida é a mesma e o caracol segue no rumo seguinte, para os níveis irem para outro lado. Sem
     * a boca lembrada na superfície, a mina fica como está.
     */
    static boolean restart(ServerWorld world, UUID colonyId, Mine mine) {
        ColonyPos entry = mine.shaft().entry();
        ColonyPos mouth = MineEntrance.surfaceOf(world, colonyId, mine);

        if (mouth.y() - entry.y() < ABOVE || !atTheSurface(world, mouth)) {
            VillageColonyMod.LOGGER.info("Mine {} keeps its stair — no first mouth at the surface to go back to"
                    + " (a new mouth is never opened)", colonyId.toString().substring(0, 8));
            return false;
        }

        VillageColonyMod.MINES.removeOfColony(colonyId);

        Mine fresh = VillageColonyMod.MINES.open(colonyId, MineShaft.from(mouth, mine.shaft().descent().clockwise()));

        MineEntrance.opened(colonyId, fresh.entry());
        MineFurnishing.furnishAndLight(world, fresh);
        VillageColonyMod.LOGGER.info("Mine {} goes back to its first mouth at {} and turns {} — the miners could not get"
                        + " down to the level at y={} (Regra 119)", colonyId.toString().substring(0, 8),
                MinecraftTypeAdapter.toBlockPos(mouth).toShortString(), fresh.shaft().descent(), entry.y());

        return true;
    }

    private static BlockPos surfaceAbove(ServerWorld world, ColonyPos at) {
        return world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(at.x(), 0, at.z())).down();
    }

    /** No chão da superfície: até 3 blocos do topo da coluna. */
    static boolean atTheSurface(ServerWorld world, ColonyPos at) {
        return Math.abs(surfaceAbove(world, at).getY() - at.y()) <= 3;
    }

    static void clearAll() {
        STRUCK.clear();
    }
}
