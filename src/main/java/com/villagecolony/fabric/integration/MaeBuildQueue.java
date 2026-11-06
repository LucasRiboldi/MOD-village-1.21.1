package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.data.save.MaeWorkStore;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A fila de obras pedidas pelo jogador — Regra 213: a primeira é a próxima obra do construtor, e o pedido fica aqui até a obra
 * terminar. Gravada no save: a obra pedida sobrevive ao reinício.
 */
public final class MaeBuildQueue {

    /**
     * Uma obra pedida.
     *
     * @param floor o piso pedido, onde se pisa
     * @param origin a origem do projeto: o piso menos a camada enterrada da planta
     * @param opened se a obra já está com o construtor
     */
    public record Request(UUID id, UUID colonyId, ResourceId model, BlockPos floor, BlockPos origin, int quarterTurns,
                          @Nullable UUID owner, boolean opened) {

        NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();

            nbt.putUuid("id", id);
            nbt.putUuid("colony", colonyId);
            nbt.putString("namespace", model.namespace());
            nbt.putString("path", model.path());
            nbt.putLong("floor", floor.asLong());
            nbt.putLong("origin", origin.asLong());
            nbt.putInt("rotation", quarterTurns);
            nbt.putBoolean("opened", opened);

            if (owner != null) {
                nbt.putUuid("owner", owner);
            }

            return nbt;
        }

        static Request of(NbtCompound nbt) {
            return new Request(nbt.getUuid("id"), nbt.getUuid("colony"), new ResourceId(nbt.getString("namespace"), nbt.getString("path")),
                    BlockPos.fromLong(nbt.getLong("floor")), BlockPos.fromLong(nbt.getLong("origin")), nbt.getInt("rotation"),
                    nbt.containsUuid("owner") ? nbt.getUuid("owner") : null, nbt.getBoolean("opened"));
        }
    }

    private MaeBuildQueue() {
    }

    private static MaeWorkStore store(ServerWorld world) {
        return ColonySavedData.get(world.getServer()).maeWork();
    }

    public static List<Request> all(ServerWorld world) {
        return store(world).requests().stream().map(Request::of).toList();
    }

    public static List<Request> ofColony(ServerWorld world, UUID colonyId) {
        return all(world).stream().filter(request -> colonyId.equals(request.colonyId())).toList();
    }

    static Request add(ServerWorld world, Request request) {
        edit(world, requests -> requests.add(request));

        return request;
    }

    /** O pedido saiu da fila: obra pronta, parada ou cancelada. */
    public static void remove(ServerWorld world, UUID id) {
        edit(world, requests -> requests.removeIf(request -> request.id().equals(id)));
    }

    public static void opened(ServerWorld world, UUID id, boolean opened) {
        replace(world, id, request -> new Request(request.id(), request.colonyId(), request.model(), request.floor(), request.origin(),
                request.quarterTurns(), request.owner(), opened));
    }

    static void movedTo(ServerWorld world, UUID id, BlockPos floor, BlockPos origin) {
        replace(world, id, request -> new Request(request.id(), request.colonyId(), request.model(), floor, origin,
                request.quarterTurns(), request.owner(), false));
    }

    private static void replace(ServerWorld world, UUID id, java.util.function.UnaryOperator<Request> change) {
        edit(world, requests -> requests.replaceAll(request -> request.id().equals(id) ? change.apply(request) : request));
    }

    private static void edit(ServerWorld world, Consumer<List<Request>> change) {
        List<Request> requests = new ArrayList<>(all(world));

        change.accept(requests);
        store(world).setRequests(requests.stream().map(Request::toNbt).toList());
    }

    /** Se esta obra aberta nasceu de um pedido: ela não é largada, nem por distância nem por espera. */
    public static boolean isRequested(ServerWorld world, ConstructionProject project) {
        return ofColony(world, project.colonyId()).stream()
                .anyMatch(request -> request.opened() && sameColumn(request.origin(), project));
    }

    private static boolean sameColumn(BlockPos origin, ConstructionProject project) {
        return origin.getX() == project.origin().x() && origin.getY() == project.origin().y() && origin.getZ() == project.origin().z();
    }

    /** O jogador cancelou a obra aberta neste lugar (Regra 108): o pedido sai junto, para ela não voltar. */
    public static void dropAt(ServerWorld world, UUID colonyId, int x, int y, int z) {
        edit(world, requests -> requests.removeIf(request -> colonyId.equals(request.colonyId()) && request.origin().getX() == x
                && request.origin().getY() == y && request.origin().getZ() == z));
    }

    /** Se o pedido é o da obra aberta neste projeto. */
    public static boolean isOf(Request request, ConstructionProject project) {
        return request.opened() && request.colonyId().equals(project.colonyId()) && sameColumn(request.origin(), project);
    }
}
