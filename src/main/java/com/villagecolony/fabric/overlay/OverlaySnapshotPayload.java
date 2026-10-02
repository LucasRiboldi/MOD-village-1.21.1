package com.villagecolony.fabric.overlay;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Snapshot visual, enviado pelo servidor a clientes que declararam suporte ao overlay. */
public record OverlaySnapshotPayload(List<WorkerEntry> workers) implements CustomPayload {

    public static final Id<OverlaySnapshotPayload> ID = new Id<>(Identifier.of("villagecolony", "overlay_snapshot"));
    public static final PacketCodec<RegistryByteBuf, OverlaySnapshotPayload> CODEC = PacketCodec.of(
            OverlaySnapshotPayload::write, OverlaySnapshotPayload::read);

    public OverlaySnapshotPayload {
        workers = List.copyOf(workers);
    }

    private static OverlaySnapshotPayload read(RegistryByteBuf buffer) {
        int size = buffer.readVarInt();
        List<WorkerEntry> workers = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            workers.add(new WorkerEntry(buffer.readUuid(), buffer.readString()));
        }
        return new OverlaySnapshotPayload(workers);
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarInt(workers.size());
        for (WorkerEntry worker : workers) {
            buffer.writeUuid(worker.id());
            buffer.writeString(worker.profession());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** O mínimo necessário para desenhar uma profissão sobre uma entidade já existente no cliente. */
    public record WorkerEntry(UUID id, String profession) {
    }
}
