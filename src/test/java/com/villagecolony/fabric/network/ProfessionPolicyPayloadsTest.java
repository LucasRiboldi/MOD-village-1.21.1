package com.villagecolony.fabric.network;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Os três pacotes da política levam o conjunto inteiro e voltam iguais pelo fio,
 * cada um com o seu canal — AUD-11. A validação do conteúdo é do
 * {@link ProfessionPolicyPayloadCodecTest}.
 */
class ProfessionPolicyPayloadsTest {

    private static RegistryByteBuf buffer() {
        return new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
    }

    /** Diferente do padrão em tudo: um decodificador que devolva o padrão não passa. */
    private static ProfessionPolicySet custom() {
        Map<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(ProfessionType.class);

        for (ProfessionType type : ProfessionType.values()) {
            policies.put(type, new ProfessionPolicy(false, 7, ProfessionPolicy.defaults().searchRadius()));
        }

        List<ProfessionType> order = new ArrayList<>(ProfessionPolicySet.defaults().hiringOrder());
        Collections.reverse(order);

        return new ProfessionPolicySet(policies, order);
    }

    private static void assertSamePolicies(ProfessionPolicySet sent, ProfessionPolicySet received) {
        assertEquals(sent.policies(), received.policies(), "as regras mudaram no caminho");
        assertEquals(sent.hiringOrder(), received.hiringOrder(), "a ordem de contratação mudou no caminho");
    }

    @Test
    void theUpdateCrossesTheWireWhole() {
        RegistryByteBuf wire = buffer();
        ProfessionPolicyUpdatePayload sent = new ProfessionPolicyUpdatePayload(custom());

        ProfessionPolicyUpdatePayload.CODEC.encode(wire, sent);
        ProfessionPolicyUpdatePayload received = ProfessionPolicyUpdatePayload.CODEC.decode(wire);

        assertSamePolicies(sent.policies(), received.policies());
        assertEquals(0, wire.readableBytes(), "sobrou byte no pacote de atualização");
        assertEquals("villagecolony:profession_policies_update", received.getId().id().toString());
    }

    @Test
    void theSyncCrossesTheWireWhole() {
        RegistryByteBuf wire = buffer();
        ProfessionPolicySyncPayload sent = new ProfessionPolicySyncPayload(custom());

        ProfessionPolicySyncPayload.CODEC.encode(wire, sent);
        ProfessionPolicySyncPayload received = ProfessionPolicySyncPayload.CODEC.decode(wire);

        assertSamePolicies(sent.policies(), received.policies());
        assertEquals(0, wire.readableBytes(), "sobrou byte no pacote de sincronia");
        assertEquals("villagecolony:profession_policies_sync", received.getId().id().toString());
    }

    @Test
    void theRequestIsEmpty() {
        RegistryByteBuf wire = buffer();

        ProfessionPolicyRequestPayload.CODEC.encode(wire, new ProfessionPolicyRequestPayload());

        assertEquals(0, wire.readableBytes(), "o pedido não deve levar conteúdo");
        assertEquals(new ProfessionPolicyRequestPayload(), ProfessionPolicyRequestPayload.CODEC.decode(wire));
        assertEquals("villagecolony:profession_policies_request",
                new ProfessionPolicyRequestPayload().getId().id().toString());
    }
}
