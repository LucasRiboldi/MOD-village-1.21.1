package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.worker.model.Worker;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.block.BedBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Uma placa externa por moradia, reconciliada pelas camas atribuídas. */
public final class HomeDisplays {
    private static final String HOME_SIGN = "villagecolony:home_sign";
    private static final String HOUSE = "villagecolony:home/";
    private static final String COLONY = "villagecolony:colony/";
    private static final AttachmentType<NbtCompound> RESIDENTS = AttachmentRegistry.createPersistent(
            Identifier.of("villagecolony", "home_sign_residents"), NbtCompound.CODEC);
    private static final double SEARCH_RADIUS = VillageDetector.SEARCH_RADIUS + 16.0;
    private static final AffineTransformation SIGN_SCALE =
            new AffineTransformation(null, null, new Vector3f(0.58f), null);

    private HomeDisplays() {
    }

    /** Registra o tipo antes de carregar as entidades do mundo. */
    public static AttachmentType<NbtCompound> register() {
        return RESIDENTS;
    }

    public static void reconcile(ServerWorld world, UUID colony, BlockPos center, Collection<Worker> workers) {
        Box area = new Box(center).expand(SEARCH_RADIUS);
        String owner = COLONY + colony;
        Set<String> registered = new HashSet<>();
        Set<String> observed = new HashSet<>();
        Map<String, Residence> homes = new HashMap<>();

        for (Worker worker : workers) {
            String resident = worker.villagerId().toString();
            registered.add(resident);
            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }
            var bed = Homes.assignedBed(world, villager);
            if (bed.isEmpty()) {
                observed.add(resident);
                continue;
            }
            if (!world.isChunkLoaded(bed.get())) {
                continue;
            }
            if (!(world.getBlockState(bed.get()).getBlock() instanceof BedBlock)) {
                observed.add(resident);
                continue;
            }
            var house = Homes.containing(world, colony, bed.get());
            if (house.isEmpty()) {
                continue;
            }
            observed.add(resident);
            String key = houseKey(house.get());
            homes.computeIfAbsent(key, ignored -> new Residence(house.get(), new TreeMap<>()))
                    .residents().put(resident, label(worker, villager));
        }

        for (TextDisplayEntity sign : world.getEntitiesByClass(TextDisplayEntity.class, area,
                entity -> entity.getCommandTags().contains(HOME_SIGN))) {
            String key = sign.getCommandTags().stream().filter(tag -> tag.startsWith(HOUSE)).findFirst().orElse("");
            Residence home = homes.remove(key);
            if (home == null && !sign.getCommandTags().contains(owner)) {
                continue;
            }
            var residents = home == null ? new TreeMap<String, String>() : home.residents();
            var previous = sign.getAttached(RESIDENTS);
            if (previous != null) {
                for (String id : previous.getKeys()) {
                    if (registered.contains(id) && !observed.contains(id)) {
                        residents.putIfAbsent(id, previous.getString(id));
                    }
                }
            }
            if (residents.isEmpty()) {
                sign.discard();
                continue;
            }
            if (!sign.getCommandTags().contains(owner)) {
                sign.getCommandTags().stream().filter(tag -> tag.startsWith(COLONY)).toList()
                        .forEach(sign::removeCommandTag);
                sign.addCommandTag(owner);
            }
            update(sign, residents);
        }

        for (var entry : homes.entrySet()) {
            Facade facade = findFacade(world, entry.getValue().box());
            if (facade != null && area.contains(facade.position().toCenterPos())) {
                create(world, owner, entry.getKey(), facade, entry.getValue().residents());
            }
        }
    }

    private static String houseKey(BlockBox house) {
        return HOUSE + new BlockPos(house.getMinX(), house.getMinY(), house.getMinZ()).asLong();
    }

    private static String label(Worker worker, VillagerEntity villager) {
        String role = worker.profession().map(WorkerNameplate::nameFor).orElse("");
        if (villager.hasCustomName()) {
            String name = villager.getName().getString().replace('\n', ' ');
            return role.isEmpty() ? name : name + " · " + role;
        }
        return role.isEmpty() ? "Morador " + worker.villagerId().toString().substring(0, 4) : role;
    }

    private static Facade findFacade(ServerWorld world, BlockBox house) {
        BlockPos min = new BlockPos(house.getMinX(), house.getMinY(), house.getMinZ());
        BlockPos max = new BlockPos(house.getMaxX(), Math.min(house.getMaxY(), min.getY() + 3), house.getMaxZ());
        for (BlockPos door : BlockPos.iterate(min, max)) {
            var state = world.getBlockState(door);
            if (!(state.getBlock() instanceof DoorBlock) || state.get(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }
            Direction front = state.get(DoorBlock.FACING).getOpposite();
            if (!leadsOutside(world, house, door, front)) {
                front = front.getOpposite();
                if (!leadsOutside(world, house, door, front)) {
                    continue;
                }
            }
            for (Direction side : new Direction[] {front.rotateYClockwise(), front.rotateYCounterclockwise()}) {
                BlockPos support = door.up().offset(side);
                BlockPos at = support.offset(front);
                if (house.contains(support) && world.getBlockState(at).isAir()
                        && world.getBlockState(support).isSideSolidFullSquare(world, support, front)) {
                    return new Facade(at, front);
                }
            }
        }
        return null;
    }

    private static boolean leadsOutside(ServerWorld world, BlockBox house, BlockPos door, Direction front) {
        for (int distance = 1; distance <= 16; distance++) {
            BlockPos at = door.offset(front, distance);
            if (!world.isChunkLoaded(at) || !world.getBlockState(at.up()).getCollisionShape(world, at.up()).isEmpty()) {
                return false;
            }
            if (!house.contains(at)) {
                return true;
            }
        }
        return false;
    }

    private static void create(ServerWorld world, String owner, String house, Facade facade,
                               Map<String, String> residents) {
        TextDisplayEntity sign = new TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        sign.addCommandTag(HOME_SIGN);
        sign.addCommandTag(owner);
        sign.addCommandTag(house);
        sign.setTransformation(SIGN_SCALE);
        sign.setBackground(0xFF513820);
        sign.setLineWidth(105);
        sign.setViewRange(0.4f);
        sign.setDisplayWidth(2.0f);
        sign.setDisplayHeight(2.0f);
        var anchor = facade.position().toCenterPos().offset(facade.front().getOpposite(), 0.47);
        sign.refreshPositionAndAngles(anchor.x, anchor.y - 0.35, anchor.z, facade.front().asRotation(), 0);
        update(sign, residents);
        if (world.spawnEntity(sign)) {
            VillageColonyMod.LOGGER.info("VC_HOME_SIGN at={} residents={}",
                    facade.position().toShortString(), residents.size());
        }
    }

    private static void update(TextDisplayEntity sign, Map<String, String> residents) {
        NbtCompound data = new NbtCompound();
        residents.forEach(data::putString);
        if (!data.equals(sign.getAttached(RESIDENTS))) {
            sign.setAttached(RESIDENTS, data);
        }
        Text text = Text.literal(String.join("\n", residents.values()));
        if (!text.equals(sign.getText())) {
            sign.setText(text);
        }
    }

    private record Residence(BlockBox box, Map<String, String> residents) {
    }

    private record Facade(BlockPos position, Direction front) {
    }
}
