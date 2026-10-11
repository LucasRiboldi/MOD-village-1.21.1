package com.villagecolony.data.save;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * As regras de profissão do Mod Menu sobrevivem ao save, e um save ruim não as
 * troca todas pelo padrão — AUD-11.
 */
class ProfessionPolicySavedDataTest {

    private static final ProfessionPolicy MINER_RULE = new ProfessionPolicy(false, 3, ProfessionPolicy.AUTOMATIC_RADIUS);

    private static NbtCompound savedWithMinerRule(List<ProfessionType> order) {
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(ProfessionType.class);
        ProfessionPolicySet.defaults().policies().forEach(policies::put);
        policies.put(ProfessionType.MINER, MINER_RULE);
        ProfessionPolicySavedData data = ProfessionPolicySavedData.TYPE_DATA.constructor().get();
        data.replace(new ProfessionPolicySet(policies, order));
        return data.writeNbt(new NbtCompound(), null);
    }

    private static ProfessionPolicySet read(NbtCompound nbt) {
        return ProfessionPolicySavedData.TYPE_DATA.deserializer().apply(nbt, null).policies();
    }

    @Test
    void theRulesAndTheOrderSurviveTheSave() {
        List<ProfessionType> order = new ArrayList<>(ProfessionPolicySet.defaults().hiringOrder());
        Collections.reverse(order);

        ProfessionPolicySet loaded = read(savedWithMinerRule(order));

        assertEquals(MINER_RULE, loaded.policyOf(ProfessionType.MINER));
        assertEquals(order, loaded.hiringOrder());
    }

    @Test
    void anUnknownOrOutOfRangeEntryKeepsTheDefaultForThatProfessionOnly() {
        NbtCompound nbt = savedWithMinerRule(ProfessionPolicySet.defaults().hiringOrder());
        NbtList entries = nbt.getList("policies", 10);
        NbtCompound unknown = new NbtCompound();
        unknown.putString("type", "MANUFACTURER");
        entries.add(unknown);
        for (int i = 0; i < entries.size(); i++) {
            if ("LUMBERJACK".equals(entries.getCompound(i).getString("type"))) {
                entries.getCompound(i).putInt("maximumWorkers", ProfessionPolicy.MAXIMUM_WORKERS_LIMIT + 1);
            }
        }

        ProfessionPolicySet loaded = read(nbt);

        assertEquals(MINER_RULE, loaded.policyOf(ProfessionType.MINER));
        assertEquals(ProfessionPolicy.defaults(), loaded.policyOf(ProfessionType.LUMBERJACK));
    }

    /** Uma ordem estragada volta ao padrão, mas as regras de cada profissão ficam. */
    @Test
    void aBrokenHiringOrderKeepsTheRulesOfEachProfession() {
        NbtCompound nbt = savedWithMinerRule(ProfessionPolicySet.defaults().hiringOrder());
        NbtList repeated = new NbtList();
        repeated.add(NbtString.of("MINER"));
        repeated.add(NbtString.of("MINER"));
        nbt.put("order", repeated);

        ProfessionPolicySet loaded = read(nbt);

        assertEquals(MINER_RULE, loaded.policyOf(ProfessionType.MINER),
                "a ordem ruim apagou a regra do mineiro que o jogador configurou");
        assertEquals(ProfessionPolicySet.defaults().hiringOrder(), loaded.hiringOrder());
    }

    @Test
    void anEmptySaveGivesTheDefaults() {
        ProfessionPolicySet loaded = read(new NbtCompound());

        assertEquals(ProfessionPolicySet.defaults().policies(), loaded.policies());
        assertEquals(ProfessionPolicySet.defaults().hiringOrder(), loaded.hiringOrder());
    }
}
