package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.integration.WorkMemoryKeys;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A roça de cada fazendeiro — Regra 52 (autor, 2026-10-08).
 *
 * <p>Cada fazendeiro sem roça recebe a roça de pé sem dono mais perto do baú
 * dele; o dono fica até deixar o ofício ou a roça deixar de existir. Gravado
 * com as memórias de trabalho ({@link WorkMemory}).
 */
public final class FarmOwners {

    static {
        ServerMemory.register(FarmOwners.class, FarmOwners::clearAll);
    }

    private static final Map<UUID, BlockBox> OWNED = new HashMap<>();

    private FarmOwners() {
    }

    /** A roça deste fazendeiro, se ele tem uma. */
    public static Optional<BlockBox> farmOf(UUID farmer) {
        return Optional.ofNullable(OWNED.get(farmer));
    }

    /** Confere os donos desta colônia e dá roça a quem está sem. Uma vez por ciclo. */
    public static void assign(ServerWorld world, Colony colony) {
        List<BlockBox> standing = slots(ColonyFarms.of(world, colony));

        List<UUID> farmers = VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .filter(worker -> worker.profession().filter(ProfessionType.FARMER::equals).isPresent())
                .map(worker -> worker.villagerId())
                .sorted()
                .toList();

        Map<UUID, ColonyPos> chests = new HashMap<>();

        for (UUID farmer : farmers) {
            chests.put(farmer, VillageColonyMod.STORAGES.of(farmer)
                    .map(WorkerStorage::chestPosition)
                    .orElse(colony.center()));
        }

        Map<UUID, BlockBox> next = pair(farmers, chests, standing, OWNED);

        for (Map.Entry<UUID, BlockBox> change : next.entrySet()) {
            if (!change.getValue().equals(OWNED.get(change.getKey()))) {
                BlockBox farm = change.getValue();
                VillageColonyMod.LOGGER.info("Farmer {} owns the farm at {}, {}, {} -> {}, {}, {}",
                        change.getKey().toString().substring(0, 8),
                        farm.getMinX(), farm.getMinY(), farm.getMinZ(),
                        farm.getMaxX(), farm.getMaxY(), farm.getMaxZ());
            }
        }

        // Quem desta colônia perdeu o ofício ou a roça sai; os das outras ficam.
        Set<UUID> ofColony = new HashSet<>(VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .map(worker -> worker.villagerId()).toList());
        OWNED.keySet().removeIf(ofColony::contains);
        OWNED.putAll(next);
    }

    /**
     * A divisão: quem já tem roça de pé fica com ela; os demais, em ordem, pegam
     * a livre mais perto do baú. Sem efeito colateral, para o teste.
     *
     * @param current os donos de antes (de todas as colônias)
     * @return o dono de cada roça desta colônia
     */
    static Map<UUID, BlockBox> pair(
            List<UUID> farmers, Map<UUID, ColonyPos> chests, List<BlockBox> standing,
            Map<UUID, BlockBox> current) {

        Map<UUID, BlockBox> owners = new HashMap<>();
        List<BlockBox> free = new ArrayList<>(standing);

        for (UUID farmer : farmers) {
            BlockBox kept = current.get(farmer);

            if (kept != null && free.remove(kept)) {
                owners.put(farmer, kept);
            }
        }

        for (UUID farmer : farmers) {
            if (owners.containsKey(farmer) || free.isEmpty()) {
                continue;
            }

            ColonyPos chest = chests.get(farmer);
            BlockBox nearest = free.stream()
                    .min(Comparator.comparingLong(box -> distanceSquared(box, chest)))
                    .orElseThrow();

            free.remove(nearest);
            owners.put(farmer, nearest);
        }

        return owners;
    }

    /**
     * As vagas de dono: uma por roça de pé, e {@link LargeFarmSeeds#FARMERS} na plantação grande (Regra 289).
     */
    static List<BlockBox> slots(List<ColonyFarms.Farm> farms) {
        List<BlockBox> slots = new ArrayList<>();

        for (ColonyFarms.Farm farm : farms) {
            if (!farm.standing()) {
                continue;
            }

            int places = LargeFarmPlans.MODEL.equals(farm.plan()) ? LargeFarmSeeds.FARMERS : 1;

            for (int place = 0; place < places; place++) {
                slots.add(farm.box());
            }
        }

        return slots;
    }

    /** Dá esta roça a este fazendeiro fora da conta do ciclo; a conta seguinte a confere. */
    static void own(UUID farmer, BlockBox farm) {
        OWNED.put(farmer, farm);
    }

    /**
     * Regra 289: se este lugar fica numa roça que já tem {@link LargeFarmSeeds#FARMERS} donos, e nenhum deles é este
     * fazendeiro — o terceiro não trabalha na plantação grande.
     */
    static boolean isFullFor(UUID farmer, BlockPos at) {
        BlockBox mine = OWNED.get(farmer);

        if (mine != null && covers(mine, at)) {
            return false;
        }

        long owners = OWNED.entrySet().stream()
                .filter(owned -> !owned.getKey().equals(farmer) && covers(owned.getValue(), at))
                .count();

        return owners >= LargeFarmSeeds.FARMERS;
    }

    /** Se outro dono da mesma roça vem antes deste na ordem: ele varre a roça do canto oposto (Regra 289). */
    static boolean isSecondOwner(UUID farmer) {
        BlockBox mine = OWNED.get(farmer);

        return mine != null && OWNED.entrySet().stream()
                .anyMatch(owned -> owned.getValue().equals(mine) && owned.getKey().compareTo(farmer) < 0);
    }

    private static boolean covers(BlockBox box, BlockPos at) {
        return at.getX() >= box.getMinX() && at.getX() <= box.getMaxX()
                && at.getZ() >= box.getMinZ() && at.getZ() <= box.getMaxZ();
    }

    private static long distanceSquared(BlockBox box, ColonyPos at) {
        long dx = Math.max(0, Math.max(box.getMinX() - at.x(), at.x() - box.getMaxX()));
        long dz = Math.max(0, Math.max(box.getMinZ() - at.z(), at.z() - box.getMaxZ()));

        return dx * dx + dz * dz;
    }

    /** O fazendeiro saiu da colônia ou morreu. */
    public static void forget(UUID farmer) {
        OWNED.remove(farmer);
    }

    public static void clearAll() {
        OWNED.clear();
    }

    static NbtCompound save() {
        NbtCompound nbt = new NbtCompound();

        OWNED.forEach((farmer, box) -> nbt.putIntArray(farmer.toString(), new int[] {
            box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()}));

        return nbt;
    }

    static void load(NbtCompound nbt) {
        for (String key : nbt.getKeys()) {
            UUID farmer = WorkMemoryKeys.uuid(key);
            int[] box = nbt.getIntArray(key);

            if (farmer != null && box.length == 6) {
                OWNED.put(farmer, new BlockBox(box[0], box[1], box[2], box[3], box[4], box[5]));
            }
        }
    }
}
