package com.villagecolony.fabric.network;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * O pacote de política é a única escrita que o cliente manda ao servidor — AUD-11.
 *
 * <p>O que chega precisa ser um conjunto completo e válido, ou é recusado
 * inteiro: profissão faltando, repetida, desconhecida, prioridade incompleta
 * e valor fora dos limites de {@link ProfessionPolicy}.
 */
class ProfessionPolicyPayloadCodecTest {

    private static RegistryByteBuf buffer() {
        return new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    }

    /** Escreve um pacote à mão: cada entrada é {nome, ligado, máximo, raio}. */
    private static RegistryByteBuf packet(List<Object[]> entries, List<String> order) {
        RegistryByteBuf buffer = buffer();
        buffer.writeVarInt(entries.size());
        for (Object[] entry : entries) {
            buffer.writeString((String) entry[0]);
            buffer.writeBoolean((Boolean) entry[1]);
            buffer.writeVarInt((Integer) entry[2]);
            buffer.writeVarInt((Integer) entry[3]);
        }
        buffer.writeVarInt(order.size());
        order.forEach(buffer::writeString);
        return buffer;
    }

    private static List<Object[]> validEntries() {
        List<Object[]> entries = new ArrayList<>();
        for (ProfessionType type : ProfessionType.values()) {
            entries.add(new Object[] {type.name(), true, 0, ProfessionPolicy.AUTOMATIC_RADIUS});
        }
        return entries;
    }

    private static List<String> validOrder() {
        return ProfessionPolicySet.defaults().hiringOrder().stream().map(Enum::name).toList();
    }

    @Test
    void aWrittenPolicyReadsBackTheSame() {
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(ProfessionType.class);
        ProfessionPolicySet.defaults().policies().forEach(policies::put);
        policies.put(ProfessionType.MINER, new ProfessionPolicy(false, 3, ProfessionPolicy.AUTOMATIC_RADIUS));
        List<ProfessionType> order = new ArrayList<>(ProfessionPolicySet.defaults().hiringOrder());
        java.util.Collections.reverse(order);
        ProfessionPolicySet sent = new ProfessionPolicySet(policies, order);

        RegistryByteBuf buffer = buffer();
        ProfessionPolicyPayloadCodec.write(buffer, sent);
        ProfessionPolicySet received = ProfessionPolicyPayloadCodec.read(buffer);

        assertEquals(sent.policies(), received.policies());
        assertEquals(sent.hiringOrder(), received.hiringOrder());
    }

    @Test
    void aPacketWithTooFewProfessionsIsRefused() {
        List<Object[]> entries = validEntries();
        entries.remove(entries.size() - 1);

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(entries, validOrder())));
    }

    @Test
    void aRepeatedProfessionIsRefused() {
        List<Object[]> entries = validEntries();
        entries.set(1, entries.get(0));

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(entries, validOrder())));
    }

    @Test
    void anUnknownProfessionIsRefused() {
        List<Object[]> entries = validEntries();
        entries.set(0, new Object[] {"MANUFACTURER", true, 0, ProfessionPolicy.AUTOMATIC_RADIUS});

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(entries, validOrder())));
    }

    @Test
    void anIncompleteOrRepeatedHiringOrderIsRefused() {
        List<String> shorter = new ArrayList<>(validOrder());
        shorter.remove(0);
        List<String> repeated = new ArrayList<>(validOrder());
        repeated.set(1, repeated.get(0));

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(validEntries(), shorter)));
        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(validEntries(), repeated)));
    }

    @Test
    void valuesOutsideTheLimitsAreRefused() {
        List<Object[]> tooMany = validEntries();
        tooMany.set(0, new Object[] {tooMany.get(0)[0], true, ProfessionPolicy.MAXIMUM_WORKERS_LIMIT + 1,
                ProfessionPolicy.AUTOMATIC_RADIUS});
        List<Object[]> tinyRadius = validEntries();
        tinyRadius.set(0, new Object[] {tinyRadius.get(0)[0], true, 0, 5});

        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(tooMany, validOrder())));
        assertThrows(IllegalArgumentException.class,
                () -> ProfessionPolicyPayloadCodec.read(packet(tinyRadius, validOrder())));
    }
}
