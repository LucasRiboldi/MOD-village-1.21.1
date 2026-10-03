package com.villagecolony.fabric.network;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionPolicySet;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Instantâneo autoritativo da política enviado pelo servidor. */
public record ProfessionPolicySyncPayload(ProfessionPolicySet policies) implements CustomPayload {
    public static final Id<ProfessionPolicySyncPayload> ID = new Id<>(
            Identifier.of(VillageColonyMod.MOD_ID, "profession_policies_sync"));
    public static final PacketCodec<RegistryByteBuf, ProfessionPolicySyncPayload> CODEC = PacketCodec.of(
            (payload, buffer) -> ProfessionPolicyPayloadCodec.write(buffer, payload.policies),
            buffer -> new ProfessionPolicySyncPayload(ProfessionPolicyPayloadCodec.read(buffer)));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
