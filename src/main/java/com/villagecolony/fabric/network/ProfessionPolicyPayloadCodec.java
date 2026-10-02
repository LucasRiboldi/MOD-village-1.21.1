package com.villagecolony.fabric.network;

import com.villagecolony.core.worker.model.ProfessionPolicy;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.network.RegistryByteBuf;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Serialização limitada da política para os pacotes de jogo. */
final class ProfessionPolicyPayloadCodec {

    private ProfessionPolicyPayloadCodec() {
    }

    static void write(RegistryByteBuf buffer, ProfessionPolicySet policies) {
        buffer.writeVarInt(ProfessionType.values().length);
        for (ProfessionType type : ProfessionType.values()) {
            ProfessionPolicy policy = policies.policyOf(type);
            buffer.writeString(type.name());
            buffer.writeBoolean(policy.enabled());
            buffer.writeVarInt(policy.maximumWorkers());
            buffer.writeVarInt(policy.searchRadius());
        }
        buffer.writeVarInt(policies.hiringOrder().size());
        for (ProfessionType type : policies.hiringOrder()) {
            buffer.writeString(type.name());
        }
    }

    static ProfessionPolicySet read(RegistryByteBuf buffer) {
        int expectedTypes = ProfessionType.values().length;
        if (buffer.readVarInt() != expectedTypes) {
            throw new IllegalArgumentException("Pacote de política com quantidade inválida de profissões");
        }
        EnumMap<ProfessionType, ProfessionPolicy> policies = new EnumMap<>(ProfessionType.class);
        for (int index = 0; index < expectedTypes; index++) {
            ProfessionType type = readType(buffer);
            if (policies.put(type, new ProfessionPolicy(buffer.readBoolean(), buffer.readVarInt(),
                    buffer.readVarInt())) != null) {
                throw new IllegalArgumentException("Pacote de política repete a profissão " + type);
            }
        }
        if (buffer.readVarInt() != expectedTypes) {
            throw new IllegalArgumentException("Pacote de política com prioridade incompleta");
        }
        List<ProfessionType> order = new ArrayList<>(expectedTypes);
        for (int index = 0; index < expectedTypes; index++) {
            ProfessionType type = readType(buffer);
            if (order.contains(type)) {
                throw new IllegalArgumentException("Pacote de política repete prioridade para " + type);
            }
            order.add(type);
        }
        return new ProfessionPolicySet(policies, order);
    }

    private static ProfessionType readType(RegistryByteBuf buffer) {
        try {
            return ProfessionType.valueOf(buffer.readString(32));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Pacote de política contém profissão desconhecida", exception);
        }
    }
}
