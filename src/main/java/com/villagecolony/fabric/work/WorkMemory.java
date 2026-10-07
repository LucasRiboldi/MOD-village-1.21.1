package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.AdvanceStock;
import com.villagecolony.fabric.integration.VillageTrees;
import com.villagecolony.fabric.integration.WorkMemoryKeys;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * As memórias de trabalho que o autor escolheu levar ao save — ADR-039 C:
 * árvores e mudas, colunas de coleta, veio e rastro, ramais reservados, vez do
 * adiantamento e coletas do pastor. Uma seção por memória; seção ausente ou
 * estragada só volta vazia, como antes do save existir.
 */
public final class WorkMemory {

    private WorkMemory() {
    }

    /** Tudo o que se guarda, numa peça só. */
    public static NbtCompound save() {
        NbtCompound nbt = new NbtCompound();

        nbt.put("trees", VillageTrees.save());
        nbt.put("surface", SurfaceHits.save());
        nbt.put("veins", MineVein.save());
        nbt.put("arms", MineClaims.save());
        nbt.put("advance", advance());
        nbt.put("herds", ShepherdHerding.save());

        return nbt;
    }

    /** Devolve o que foi guardado. Chamado depois de as minas voltarem. */
    public static void load(NbtCompound nbt) {
        VillageTrees.load(nbt.getCompound("trees"));
        SurfaceHits.load(nbt.getCompound("surface"));
        MineVein.load(nbt.getCompound("veins"));
        MineClaims.load(nbt.getCompound("arms"));
        AdvanceStock.restore(turns(nbt.getList("advance", NbtElement.COMPOUND_TYPE)));
        ShepherdHerding.load(nbt.getCompound("herds"));
    }

    private static NbtList advance() {
        NbtList list = new NbtList();

        for (AdvanceStock.Turn turn : AdvanceStock.snapshot()) {
            NbtCompound entry = new NbtCompound();

            entry.putString("colony", turn.colonyId().toString());
            entry.putString("lane", turn.lane());
            entry.putInt("index", turn.index());
            entry.putInt("target", turn.target());
            entry.putInt("stalled", turn.stalled());
            list.add(entry);
        }

        return list;
    }

    private static List<AdvanceStock.Turn> turns(NbtList list) {
        List<AdvanceStock.Turn> turns = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            UUID colony = WorkMemoryKeys.uuid(entry.getString("colony"));

            if (colony != null && !entry.getString("lane").isEmpty()) {
                turns.add(new AdvanceStock.Turn(colony, entry.getString("lane"),
                        entry.getInt("index"), entry.getInt("target"), entry.getInt("stalled")));
            }
        }

        return turns;
    }
}
