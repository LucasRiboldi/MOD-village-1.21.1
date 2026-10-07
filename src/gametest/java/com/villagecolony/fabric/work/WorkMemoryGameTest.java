package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.coordination.AdvanceStock;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.integration.VillageTrees;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.UUID;

/**
 * As memórias escolhidas pelo autor vão ao save e voltam dele (ADR-039 C). A
 * colônia A é gravada; no texto do save o identificador dela vira o de B; B
 * carrega e grava de novo. Assim o teste não apaga a memória global dos
 * cenários que rodam junto.
 */
public class WorkMemoryGameTest implements FabricGameTest {

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "work_hours", tickLimit = 20)
    public void everyChosenMemoryGoesToTheSaveAndComesBack(TestContext context) throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID worker = UUID.randomUUID();
        UUID shepherdA = UUID.randomUUID();
        UUID shepherdB = UUID.randomUUID();
        BlockPos base = context.getAbsolutePos(new BlockPos(1, 1, 1)).up(80);
        ColonyPos entry = new ColonyPos(base.getX(), base.getY(), base.getZ());
        Mine mineA = VillageColonyMod.MINES.open(a, MineShaft.from(entry, Side.NORTH));
        Mine mineB = VillageColonyMod.MINES.open(b, MineShaft.from(entry, Side.NORTH));
        ColonyPos ore = new ColonyPos(base.getX() + 3, base.getY() - 5, base.getZ());

        try {
            VillageTrees.rememberTree(a, base.east(2));
            VillageTrees.rememberSapling(a, base.east(4));
            SurfaceHits.remember(a, ResourceType.SAND, base.south(3));
            MineVein.startVein(mineA.arm(1), ore);
            MineClaims.claimArm(a, worker, 1);
            AdvanceStock.addTo(new HashMap<>(), a, "carpenter", AdvanceStock.CARPENTER_PIECES, piece -> 0);

            NbtCompound herd = new NbtCompound();
            NbtCompound one = new NbtCompound();
            one.putUuid("animal", UUID.randomUUID());
            one.putLong("fence", base.asLong());
            one.putString("phase", "LEAD");
            one.putLong("since", 7L);
            herd.put(shepherdA.toString(), one);
            ShepherdHerding.load(herd);

            String saved = WorkMemory.save().toString()
                    .replace(a.toString(), b.toString())
                    .replace(shepherdA.toString(), shepherdB.toString());

            WorkMemory.load(StringNbtReader.parse(saved));

            NbtCompound again = WorkMemory.save();

            context.assertTrue(VillageTrees.knownTrees(b) == 1 && VillageTrees.knownSaplings(b) == 1,
                    "as árvores e mudas não voltaram: " + VillageTrees.knownTrees(b) + "/" + VillageTrees.knownSaplings(b));
            context.assertTrue(again.getCompound("surface").contains(b + "|SAND"), "a coluna de areia não voltou");
            context.assertTrue(mineB.arm(1).vein().filter(ore::equals).isPresent(), "o veio do ramal 1 não voltou");
            context.assertTrue(MineClaims.armAlreadyHeld(b, worker).isPresent(), "o ramal reservado não voltou");
            context.assertTrue(AdvanceStock.snapshot().stream()
                            .anyMatch(turn -> turn.colonyId().equals(b) && turn.lane().equals("carpenter")),
                    "a vez do adiantamento não voltou");
            context.assertTrue(ShepherdHerding.phaseOf(shepherdB).filter(ShepherdHerding.Phase.LEAD::equals).isPresent(),
                    "a coleta do pastor não voltou");
            context.assertTrue(again.getList("advance", NbtElement.COMPOUND_TYPE).size() >= 2,
                    "a seção do adiantamento saiu vazia");
        } finally {
            MineClaims.releaseArm(a, worker);
            MineClaims.releaseArm(b, worker);
            VillageColonyMod.MINES.removeOfColony(a);
            VillageColonyMod.MINES.removeOfColony(b);
        }

        context.complete();
    }
}
