package com.villagecolony.fabric.network;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Solicitação de atualização enviada pelo cliente a um servidor autorizado. */
public record ProfessionPolicyUpdatePayload(ProfessionPolicySet policies) implements CustomPayload {
    public static final Id<ProfessionPolicyUpdatePayload> ID = new Id<>(
            Identifier.of(VillageColonyMod.MOD_ID, "profession_policies_update"));
    public static final PacketCodec<RegistryByteBuf, ProfessionPolicyUpdatePayload> CODEC = PacketCodec.of(
            (payload, buffer) -> ProfessionPolicyPayloadCodec.write(buffer, payload.policies),
            buffer -> new ProfessionPolicyUpdatePayload(ProfessionPolicyPayloadCodec.read(buffer)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
