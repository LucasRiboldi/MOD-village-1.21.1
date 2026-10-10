package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O ajudante no tempo ocioso recolhe do chão o que a obra espera — B-1 e
 * Regra 48, decisões do autor de 2026-10-02: <i>"itens no chão somente são
 * recolhidos se forem itens em falta, na lista da obra"</i>.
 *
 * <p><b>Quem.</b> Um trabalhador da colônia sem tarefa, no expediente, solto
 * (nem encalhado, nem saindo de curral). A reanálise de 02-10 mediu o pastor,
 * o pedreiro e o fundidor parados por desenho; esta é a primeira coisa que eles
 * fazem nesse tempo.
 *
 * <p><b>O quê.</b> Só item caído que seja peça que falta à obra aberta da
 * colônia, dentro da caixa da vila (ou a {@link #REACH} blocos da obra, sem
 * caixa). O resto fica no chão. Vai para o baú da colônia mais perto da obra —
 * é de lá que o construtor tira.
 *
 * <p><b>Como.</b> Pelo destino de trabalho, como qualquer ofício; ao chegar a
 * {@link #PICK_UP} blocos do item, ele o guarda. Um ajudante por item, e no
 * máximo {@link #HELPERS} por colônia.
 */
public final class GroundPickup {

    static {
        ServerMemory.register(GroundPickup.class, GroundPickup::clearAll);
    }

    /** Sem caixa medida, até onde da obra se procura. */
    static final int REACH = 32;

    /** A que distância do item ele o pega. */
    static final double PICK_UP = 1.8;

    /** Quantos ajudantes por colônia ao mesmo tempo. */
    static final int HELPERS = 2;

    /** Quanto tempo ele tem para chegar ao item. */
    static final long GIVE_UP_AFTER = 600;

    private record Pickup(UUID colonyId, UUID item, long since) {
    }

    private static final Map<UUID, Pickup> HELPING = new HashMap<>();

    /** Item que nenhum baú da colônia aceitou: fica de lado por um tempo. */
    private static final Map<UUID, Long> NO_ROOM = new HashMap<>();

    /** Quanto tempo o item sem baú fica de lado. */
    static final long NO_ROOM_FOR = 6_000;

    private GroundPickup() {
    }

    /** Se este trabalhador está recolhendo para a obra agora. */
    public static boolean isHelping(UUID workerId) {
        return HELPING.containsKey(workerId);
    }

    /** Uma passagem (uma vez por segundo): conduz quem já ajuda e chama ajuda nova. */
    static void pass(ServerWorld world) {
        long now = world.getTime();

        for (UUID workerId : List.copyOf(HELPING.keySet())) {
            drive(world, workerId, HELPING.get(workerId), now);
        }

        for (Colony colony : List.copyOf(VillageColonyMod.COLONIES.all())) {
            if (PenEscape.isWorking(world, colony.id())) {
                callHelp(world, colony, now);
            }
        }
    }

    private static void callHelp(ServerWorld world, Colony colony, long now) {
        long helping = HELPING.values().stream().filter(pickup -> pickup.colonyId().equals(colony.id())).count();

        if (helping >= HELPERS) {
            return;
        }

        ConstructionProject project = VillageColonyMod.CONSTRUCTIONS.openOf(colony.id()).orElse(null);

        if (project == null) {
            return;
        }

        Set<Item> wanted = itemsOf(project);

        if (wanted.isEmpty()) {
            return;
        }

        Set<UUID> taken = new HashSet<>();
        HELPING.values().forEach(pickup -> taken.add(pickup.item()));
        NO_ROOM.entrySet().removeIf(entry -> now - entry.getValue() >= NO_ROOM_FOR);
        taken.addAll(NO_ROOM.keySet());

        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, searchBox(colony, project),
                entity -> entity.isAlive() && wanted.contains(entity.getStack().getItem())
                        && !taken.contains(entity.getUuid()));

        for (ItemEntity item : items) {
            VillagerEntity helper = idleHelperNear(world, colony, item);

            if (helper == null) {
                return;
            }

            HELPING.put(helper.getUuid(), new Pickup(colony.id(), item.getUuid(), now));
            WorkTargets.set(helper.getUuid(), item.getBlockPos(), 1);

            VillageColonyMod.LOGGER.info("Worker {} goes to pick up {} x{} at {} — the build is waiting for it",
                    helper.getUuid().toString().substring(0, 8), Registries.ITEM.getId(item.getStack().getItem()),
                    item.getStack().getCount(), item.getBlockPos().toShortString());

            if (++helping >= HELPERS) {
                return;
            }
        }
    }

    private static void drive(ServerWorld world, UUID workerId, Pickup pickup, long now) {
        if (!(world.getEntity(workerId) instanceof VillagerEntity helper)
                || !(world.getEntity(pickup.item()) instanceof ItemEntity item) || !item.isAlive()
                || !VillageColonyMod.TASKS.assignedTo(workerId).isEmpty()
                || now - pickup.since() > GIVE_UP_AFTER) {
            stop(workerId);

            return;
        }

        if (helper.squaredDistanceTo(item) > PICK_UP * PICK_UP) {
            WorkTargets.set(workerId, item.getBlockPos(), 1);

            return;
        }

        ConstructionProject project = VillageColonyMod.CONSTRUCTIONS.openOf(pickup.colonyId()).orElse(null);
        BlockPos near = project == null ? helper.getBlockPos() : MinecraftTypeAdapter.toBlockPos(project.origin());

        int left = item.getStack().getCount();

        for (ColonyPos chest : ColonyChests.nearestFirst(world, pickup.colonyId(),
                MinecraftTypeAdapter.toColonyPos(near))) {
            left = ChestDepositor.deposit(world, chest, item.getStack().getItem(), left);

            if (left == 0) {
                break;
            }
        }

        int stored = item.getStack().getCount() - left;

        if (stored == 0) {
            // Nenhum baú da colônia aceitou: o item fica de lado, senão o
            // ajudante seria chamado de novo a cada segundo para nada.
            NO_ROOM.put(item.getUuid(), now);
            VillageColonyMod.LOGGER.info("No colony chest had room for {} at {} — it stays on the ground for now",
                    Registries.ITEM.getId(item.getStack().getItem()), item.getBlockPos().toShortString());
            stop(workerId);

            return;
        }

        if (left == 0) {
            item.discard();
        } else {
            item.getStack().setCount(left);
        }

        helper.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        VillageColonyMod.LOGGER.info("Worker {} picked up {} x{} for the build — into the colony chest",
                workerId.toString().substring(0, 8), Registries.ITEM.getId(item.getStack().getItem()), stored);

        stop(workerId);
    }

    private static void stop(UUID workerId) {
        HELPING.remove(workerId);
        WorkTargets.clear(workerId);
    }

    /** As peças que faltam à obra, como itens. */
    static Set<Item> itemsOf(ConstructionProject project) {
        Set<Item> items = new HashSet<>();

        for (ResourceId material : project.remainingMaterials().keySet()) {
            addKnownItem(items, material);
        }

        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            addKnownItem(items, piece.block());
        }

        return items;
    }

    private static void addKnownItem(Set<Item> items, ResourceId material) {
        Item item = Registries.ITEM.get(Identifier.of(material.namespace(), material.path()));

        if (item != Items.AIR) {
            items.add(item);
        }
    }

    private static Box searchBox(Colony colony, ConstructionProject project) {
        return colony.bounds()
                .map(box -> new Box(box.minX(), box.minY() - 4, box.minZ(), box.maxX() + 1, box.maxY() + 5,
                        box.maxZ() + 1))
                .orElseGet(() -> new Box(MinecraftTypeAdapter.toBlockPos(project.origin())).expand(REACH));
    }

    /** O trabalhador sem tarefa mais perto do item, no expediente e solto. */
    private static VillagerEntity idleHelperNear(ServerWorld world, Colony colony, ItemEntity item) {
        VillagerEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            UUID id = worker.villagerId();

            if (HELPING.containsKey(id) || !VillageColonyMod.TASKS.assignedTo(id).isEmpty()
                    || StrandedWorkers.isStranded(id) || PenEscape.isEscaping(id)
                    || !(world.getEntity(id) instanceof VillagerEntity villager) || !villager.isAlive()
                    || villager.isSleeping() || !WorkHours.isWorkTime(world, villager)) {
                continue;
            }

            double distance = villager.squaredDistanceTo(item);

            if (distance < bestDistance) {
                best = villager;
                bestDistance = distance;
            }
        }

        return best;
    }

    static void clearAll() {
        HELPING.clear();
        NO_ROOM.clear();
    }
}
