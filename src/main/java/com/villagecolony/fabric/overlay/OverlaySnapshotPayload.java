package com.villagecolony.fabric.overlay;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Snapshot visual, enviado pelo servidor a clientes que declararam suporte ao overlay. */
public record OverlaySnapshotPayload(
        List<WorkerEntry> workers, List<ConstructionEntry> constructions) implements CustomPayload {

    public static final Id<OverlaySnapshotPayload> ID = new Id<>(Identifier.of("villagecolony", "overlay_snapshot"));
    public static final PacketCodec<RegistryByteBuf, OverlaySnapshotPayload> CODEC = PacketCodec.of(
            OverlaySnapshotPayload::write, OverlaySnapshotPayload::read);

    public OverlaySnapshotPayload {
        workers = List.copyOf(workers);
        constructions = List.copyOf(constructions);
    }

    private static OverlaySnapshotPayload read(RegistryByteBuf buffer) {
        int size = buffer.readVarInt();
        List<WorkerEntry> workers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            workers.add(new WorkerEntry(buffer.readUuid(), buffer.readString()));
        }
        int constructionSize = buffer.readVarInt();
        List<ConstructionEntry> constructions = new ArrayList<>(constructionSize);
        for (int index = 0; index < constructionSize; index++) {
            constructions.add(new ConstructionEntry(
                    buffer.readUuid(), buffer.readString(), buffer.readString(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readString()));
        }
        return new OverlaySnapshotPayload(workers, constructions);
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarInt(workers.size());
        for (WorkerEntry worker : workers) {
            buffer.writeUuid(worker.id());
            buffer.writeString(worker.profession());
        }
        buffer.writeVarInt(constructions.size());
        for (ConstructionEntry construction : constructions) {
            buffer.writeUuid(construction.id());
            buffer.writeString(construction.blueprint());
            buffer.writeString(construction.state());
            buffer.writeDouble(construction.x());
            buffer.writeDouble(construction.y());
            buffer.writeDouble(construction.z());
            buffer.writeVarInt(construction.placed());
            buffer.writeVarInt(construction.total());
            buffer.writeString(construction.missingMaterial());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** O mínimo necessário para desenhar uma profissão sobre uma entidade já existente no cliente. */
    public record WorkerEntry(UUID id, String profession) {
    }

    /** Uma obra aberta, compactada para o HUD de mundo do cliente; x, y, z são os da placa da obra. */
    public record ConstructionEntry(
            UUID id, String blueprint, String state, double x, double y, double z,
            int placed, int total, String missingMaterial) {
    }
}
