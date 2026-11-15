package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A plantação grande trabalha — Regra 289:
 *
 * <ul>
 *   <li>Ao fechar a obra da plantação grande, todo canteiro sai semeado, alternando trigo, cenoura, batata e
 *   beterraba (cultivo moldado no local, Regra 69).</li>
 *   <li>Sem semente em baú nenhum, e com canteiro vazio na roça do fazendeiro, {@link #SURGE} sementes da lavoura que a
 *   vila tem menos surgem no baú dele ({@code SEED_FOR_THE_FARM}); de novo, só depois de {@link #AGAIN}.</li>
 *   <li>A plantação grande tem {@link #FARMERS} vagas de dono ({@link FarmOwners#slots}).</li>
 * </ul>
 */
final class LargeFarmSeeds {

    static {
        ServerMemory.register(LargeFarmSeeds.class, LargeFarmSeeds::clearAll);
    }

    /** Quantos fazendeiros cuidam da plantação grande. */
    static final int FARMERS = 2;

    /** Quantas sementes surgem de cada vez. */
    static final int SURGE = 16;

    /** O intervalo mínimo entre duas sementes surgidas para o mesmo fazendeiro: um minuto de jogo. */
    static final long AGAIN = 1_200;

    /** As sementes da vila, na ordem do desempate. */
    private static final List<Item> SEEDS = List.of(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS);

    /** As lavouras da obra semeada, em faixas diagonais. */
    private static final List<Block> CROPS = List.of(Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS);

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private LargeFarmSeeds() {
    }

    static void clearAll() {
        LAST.clear();
    }

    /**
     * Faz a semente surgir no baú {@code chest} do fazendeiro quando nenhum baú da vila tem semente e a roça dele tem
     * canteiro vazio.
     *
     * @return se a semente surgiu
     */
    static boolean surgeIfOut(ServerWorld world, UUID colonyId, UUID farmer, ColonyPos chest) {
        Long last = LAST.get(farmer);

        if (last != null && world.getTime() - last < AGAIN) {
            return false;
        }

        Optional<BlockBox> farm = FarmOwners.farmOf(farmer);

        if (farm.isEmpty() || FarmerChores.seedChest(world, colonyId, chest).isPresent()
                || OwnFarm.work(world, farm.get(), true, false)
                        .filter(found -> found.chore() == FarmerWork.Chore.SOW).isEmpty()) {
            return false;
        }

        Item seed = scarcest(world, colonyId, chest);
        int surged = SURGE - ChestDepositor.deposit(world, chest, seed, SURGE);

        if (surged <= 0) {
            return false;
        }

        LAST.put(farmer, world.getTime());
        Supplies.record(SupplyException.SEED_FOR_THE_FARM, colonyId, Registries.ITEM.getId(seed).getPath(), surged);
        VillageColonyMod.LOGGER.info("Farmer {} found no seed in any chest — {} {} surged in its chest (Regra 289)",
                farmer.toString().substring(0, 8), surged, Registries.ITEM.getId(seed).getPath());

        return true;
    }

    /** A semente da lavoura que a vila tem menos nos baús; no empate, a primeira de {@link #SEEDS}. */
    private static Item scarcest(ServerWorld world, UUID colonyId, ColonyPos chest) {
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colonyId, chest);
        Item best = SEEDS.get(0);
        int fewest = Integer.MAX_VALUE;

        for (Item seed : SEEDS) {
            int have = ColonyChests.countIn(world, chests, FarmerChores.cropOf(seed));

            if (have < fewest) {
                best = seed;
                fewest = have;
            }
        }

        return best;
    }

    /** Semeia a plantação grande que acabou de fechar e declara o cultivo moldado (Regra 69). */
    static void sowAtCompletion(ServerWorld world, ConstructionProject project) {
        BlockBox box = ColonyFarms.boxOf(com.villagecolony.core.construction.model.Building.of(project, true));
        int sown = sowAtCompletion(world, project.blueprint().id(), box);

        if (sown > 0) {
            Supplies.record(SupplyException.SHAPED_GROUND, project.colonyId(), "crop", sown);
            VillageColonyMod.LOGGER.info("Colony {} handed the large farm at {} sown — {} plots (Regra 289)",
                    project.colonyId().toString().substring(0, 8), project.origin(), sown);
        }
    }

    /**
     * Planta em todo canteiro vazio de {@code box}, se a obra é a plantação grande.
     *
     * @return quantos canteiros foram semeados
     */
    static int sowAtCompletion(ServerWorld world, ResourceId blueprint, BlockBox box) {
        if (!LargeFarmPlans.MODEL.equals(blueprint)) {
            return 0;
        }

        int sown = 0;

        for (BlockPos at : BlockPos.iterate(box.getMinX(), box.getMinY() - 1, box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ())) {
            if (CropPatch.isEmptyPlot(world, at)) {
                Block crop = CROPS.get(Math.floorMod(at.getX() + at.getZ(), CROPS.size()));
                world.setBlockState(at.up(), crop.getDefaultState(), Block.NOTIFY_ALL);
                sown++;
            }
        }

        return sown;
    }
}
