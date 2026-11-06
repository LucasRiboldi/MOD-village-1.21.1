package com.villagecolony.fabric.overlay;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * O que a vila guarda como caminho, para o sinalizador de desenvolvimento ({@code /vc mae mostrar}): as linhas do
 * grafo de ruas e os marcadores dos nós e do índice. Lista vazia apaga o desenho no cliente.
 */
public record RoadMapPayload(List<Line> lines, List<Marker> markers) implements CustomPayload {

    public static final Id<RoadMapPayload> ID = new Id<>(Identifier.of("villagecolony", "road_map"));
    public static final PacketCodec<RegistryByteBuf, RoadMapPayload> CODEC = PacketCodec.of(
            RoadMapPayload::write, RoadMapPayload::read);

    public RoadMapPayload {
        lines = List.copyOf(lines);
        markers = List.copyOf(markers);
    }

    private static RoadMapPayload read(RegistryByteBuf buffer) {
        int lineCount = buffer.readVarInt();
        List<Line> lines = new ArrayList<>(lineCount);

        for (int index = 0; index < lineCount; index++) {
            int color = buffer.readInt();
            int size = buffer.readVarInt();
            List<Long> points = new ArrayList<>(size);

            for (int point = 0; point < size; point++) {
                points.add(buffer.readLong());
            }

            lines.add(new Line(color, points));
        }

        int markerCount = buffer.readVarInt();
        List<Marker> markers = new ArrayList<>(markerCount);

        for (int index = 0; index < markerCount; index++) {
            markers.add(new Marker(buffer.readInt(), buffer.readLong()));
        }

        return new RoadMapPayload(lines, markers);
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeVarInt(lines.size());

        for (Line line : lines) {
            buffer.writeInt(line.color());
            buffer.writeVarInt(line.points().size());
            line.points().forEach(buffer::writeLong);
        }

        buffer.writeVarInt(markers.size());

        for (Marker marker : markers) {
            buffer.writeInt(marker.color());
            buffer.writeLong(marker.position());
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Uma linha pelo chão: a cor (RGB) e os pontos ({@code BlockPos.asLong}), na ordem. */
    public record Line(int color, List<Long> points) {
        public Line {
            points = List.copyOf(points);
        }
    }

    /** Um bloco marcado: a cor (RGB) e a posição ({@code BlockPos.asLong}). */
    public record Marker(int color, long position) {
    }
}
