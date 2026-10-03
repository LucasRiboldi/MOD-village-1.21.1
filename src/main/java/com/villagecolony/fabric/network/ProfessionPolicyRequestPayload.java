package com.villagecolony.fabric.network;

import com.villagecolony.VillageColonyMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Pedido do cliente pelo instantâneo atual, feito ao abrir a configuração. */
public record ProfessionPolicyRequestPayload() implements CustomPayload {
    public static final Id<ProfessionPolicyRequestPayload> ID = new Id<>(
            Identifier.of(VillageColonyMod.MOD_ID, "profession_policies_request"));
    public static final PacketCodec<RegistryByteBuf, ProfessionPolicyRequestPayload> CODEC = PacketCodec.of(
            (payload, buffer) -> { }, buffer -> new ProfessionPolicyRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
