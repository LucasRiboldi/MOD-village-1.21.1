package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.BuildSiteScanner;
import com.villagecolony.fabric.work.MineRock;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reavalia dados derivados quando o jogador realmente altera blocos. */
public final class PlayerWorldChangeHandler {

    private static final int OBSERVED_POSITIONS_LIMIT = 8192;
    private static final int MINE_NEIGHBOR_DISTANCE = 1;
    private static final int BUILD_VERTICAL_RANGE = 16;

    private static final Map<World, LinkedHashMap<BlockPos, BlockState>> pending = new HashMap<>();

    private PlayerWorldChangeHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(PlayerWorldChangeHandler::beforeUseBlock);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) {
                worldChanged(serverWorld, pos, serverWorld.getBlockState(pos));
            }
        });
        ServerTickEvents.END_WORLD_TICK.register(PlayerWorldChangeHandler::afterWorldTick);
    }

    private static ActionResult beforeUseBlock(
            PlayerEntity player,
            World world,
            Hand hand,
            BlockHitResult hit) {
        if (!(world instanceof ServerWorld serverWorld) || player.isSpectator()) {
            return ActionResult.PASS;
        }

        LinkedHashMap<BlockPos, BlockState> changes = pending.computeIfAbsent(
                world, ignored -> new LinkedHashMap<>());
        BlockPos center = hit.getBlockPos();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);

                    if (changes.size() >= OBSERVED_POSITIONS_LIMIT && !changes.containsKey(pos)) {
                        continue;
                    }

                    if (serverWorld.isChunkLoaded(pos)) {
                        changes.putIfAbsent(pos.toImmutable(), serverWorld.getBlockState(pos));
                    }
                }
            }
        }

        return ActionResult.PASS;
    }

    private static void afterWorldTick(ServerWorld world) {
        LinkedHashMap<BlockPos, BlockState> changes = pending.remove(world);

        if (changes == null) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : changes.entrySet()) {
            BlockPos pos = entry.getKey();

            if (!world.isChunkLoaded(pos) || entry.getValue().equals(world.getBlockState(pos))) {
                continue;
            }

            worldChanged(world, pos, world.getBlockState(pos));
        }
    }

    private static void worldChanged(ServerWorld world, BlockPos changed, BlockState after) {
        ColonyPos changedPos = MinecraftTypeAdapter.toColonyPos(changed);

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            ColonyPos center = colony.center();

            if (Math.abs((long) center.x() - changedPos.x()) <= VillageDetector.SEARCH_RADIUS
                    && Math.abs((long) center.z() - changedPos.z()) <= VillageDetector.SEARCH_RADIUS
                    && Math.abs((long) center.y() - changedPos.y()) <= BUILD_VERTICAL_RANGE) {
                BuildSiteScanner.reconcileWorldChange(colony.id(), world, changed, center);
            }
        }

        // <b>E a mudança da própria colônia não reabre nada</b> —
        // 2026-09-16. Esta porta existe para o jogador: ele abre caminho e
        // a mina retoma por ali. Reagir à picareta do próprio mineiro era o
        // laço que encheu o log de 02:58 com 19.193 linhas iguais, dez por
        // segundo, com a mina sem descer uma vez. Ver ColonyEdits — a marca
        // vale uma leitura, para o jogador nunca ficar ignorado.
        if (ColonyEdits.wasOurs(changedPos)) {
            return;
        }

        if (MineRock.isOpenSpace(world, changed, after)) {
            for (Mine mine : VillageColonyMod.MINES.all()) {
                reopenAffectedArms(mine, changedPos);
            }
        }
    }

    private static void reopenAffectedArms(Mine mine, ColonyPos changed) {
        for (MineArm arm : mine.arms()) {
            for (int index = 0; index < arm.cut(); index++) {
                ColonyPos tunnel = arm.shaft().positionAt(index);

                if (near(tunnel, changed)) {
                    arm.reopenFrom(index);
                    break;
                }
            }
        }
    }

    private static boolean near(ColonyPos tunnel, ColonyPos changed) {
        return Math.abs((long) tunnel.x() - changed.x()) <= MINE_NEIGHBOR_DISTANCE
                && Math.abs((long) tunnel.y() - changed.y()) <= MINE_NEIGHBOR_DISTANCE
                && Math.abs((long) tunnel.z() - changed.z()) <= MINE_NEIGHBOR_DISTANCE;
    }

    public static void clearAll() {
        pending.clear();
    }
}
