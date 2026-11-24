package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.CropPatch;
import com.villagecolony.fabric.integration.Supplies;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O pastor alimenta pares com a comida que a espécie aceita (ADR-038 P2c e ADR-039 E2).
 * Com currais, as espécies de corte formam pares no mesmo recinto. A meta de adultos por
 * espécie soma todos os currais e sobe de 8 para 16 com pelo menos 32 itens de alimento
 * aceito nos baús (Regra 238). A ração da Regra 117 só atende a meta de 8.
 */
public final class ShepherdFlock {

    static {
        ServerMemory.register(ShepherdFlock.class, ShepherdFlock::clearAll);
    }

    /** Adultos de cada espécie a partir dos quais o pastor para de criar — autor, 2026-10-22 (eram 12). */
    public static final int FLOCK_TARGET = 8;

    /** Comida da espécie nos baús a partir da qual o rebanho cresce além da meta — Regra 238 (autor, 2026-11-11). */
    public static final int SURPLUS_FOOD = 32;

    /** Adultos de cada espécie com comida sobrando: casa com 16 animais por pastor (Regras 235 e 238). */
    public static final int SURPLUS_TARGET = 16;

    /** Entre uma rodada de comida e a seguinte da mesma colônia: um minuto. */
    static final int BETWEEN_PAIRS = 1_200;

    /** Quantos pares de cada espécie o pastor alimenta por rodada. */
    static final int PAIRS_PER_ROUND = 2;

    /** Quanto de uma comida um par come: os dois animais, um item cada. */
    public static final int PAIR_FOOD = 2;

    /**
     * Sementes que a criação deixa para o plantio — decisão do autor de 2026-10-22 ("mais trigo
     * para o rebanho"). No deserto (0.3.86) o pastor cevou galinha com semente de trigo e o fazendeiro,
     * sem ela, semeou beterraba: ovelha e vaca ficaram sem trigo.
     */
    static final int SEEDS_FOR_SOWING = 8;

    private static final int RADIUS = 32;
    static final double ACTION_REACH_SQUARED = 25.0;

    /** A ração de cada espécie quando o fazendeiro não produziu nada — Regra 117. */
    static final Map<EntityType<?>, Item> ANIMAL_FEED = Map.of(
            EntityType.SHEEP, Items.WHEAT, EntityType.COW, Items.WHEAT,
            EntityType.PIG, Items.CARROT, EntityType.CHICKEN, Items.WHEAT_SEEDS, EntityType.RABBIT, Items.CARROT);

    /**
     * As espécies que o pastor cria — Regra 239 (autor, 2026-11-11): todo animal do jogo que procria com comida, sem ser
     * domado antes. Cavalo, burro, lhama, lobo e gato pedem doma, e os do Nether não vivem na vila; ficam de fora.
     */
    static final List<EntityType<? extends AnimalEntity>> SPECIES = List.of(
            EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT, EntityType.MOOSHROOM,
            EntityType.GOAT, EntityType.CAMEL, EntityType.FOX, EntityType.PANDA, EntityType.TURTLE, EntityType.BEE,
            EntityType.FROG, EntityType.SNIFFER, EntityType.ARMADILLO);

    /** As espécies que o pastor leva ao curral e abate acima da reserva: a criação de corte (Regra 239). */
    static final List<EntityType<? extends AnimalEntity>> LIVESTOCK = List.of(
            EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT, EntityType.MOOSHROOM,
            EntityType.GOAT);

    /**
     * As comidas que esta espécie aceita, lidas do próprio jogo
     * ({@code AnimalEntity.isBreedingItem}) — regra do autor, 2026-10-12: o pastor
     * alimenta os animais com qualquer comida que haja nos baús da vila. Era uma lista
     * fixa de seis itens.
     */
    private static final Map<EntityType<?>, List<Item>> FOOD = new HashMap<>();

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ShepherdFlock() {
    }

    /**
     * Alimenta até dois pares por espécie quando um pastor alcança os dois animais.
     *
     * @return se algum par foi posto para procriar
     */
    public static boolean tend(ServerWorld world, Colony colony) {
        long now = world.getTime();
        Long last = LAST.get(colony.id());
        if (last != null && now - last < BETWEEN_PAIRS) {
            return false;
        }

        if (!hasShepherd(colony)) {
            return false;
        }

        LAST.put(colony.id(), now);

        Box area = areaOf(colony);
        PenFlock flock = PenFlock.read(world, colony);
        List<ColonyPos> chests = ColonyChests.nearestFirst(world, colony.id(), colony.center());
        boolean paired = false;
        Map<EntityType<?>, Integer> targets = new HashMap<>();

        for (EntityType<? extends AnimalEntity> species : SPECIES) {
            List<List<AnimalEntity>> groups = breedingGroups(world, species, area, flock);
            List<AnimalEntity> adults = groups.stream().flatMap(List::stream).toList();
            int target = creationTarget(world, species, adults, chests);
            targets.put(species, target);
            for (int pair = 0; pair < PAIRS_PER_ROUND; pair++) {
                if (!pairOf(world, colony, species, groups, adults.size(), target, chests, null)) {
                    break;
                }

                paired = true;
            }
        }

        // Abate um adulto por rodada no expediente, preservando as reservas da vila e do curral (Regras 68 e 194).
        if (com.villagecolony.core.coordination.WorkClock.isWorkTime(world.getTimeOfDay())) {
            FlockHarvest.step(world, colony, flock, chests, targets);
        }

        return paired;
    }

    /**
     * A comida de cria que a ceia da vila deixa no baú — D2, decisão do autor de 2026-10-21.
     *
     * <p>No deserto o pastor disse ~200 vezes "no food" com o rebanho pedindo par: a ceia
     * ({@link VillageMeals}) levava a cenoura, a batata e a beterraba de qualquer baú antes dele.
     * Com pastor na vila, cada espécie com par pronto e abaixo de {@link #FLOCK_TARGET} guarda um
     * par de cada comida que ela aceita.
     *
     * @return quanto de cada item a ceia não tira do baú; vazio sem pastor
     */
    public static Map<Item, Integer> breedingFoodToKeep(ServerWorld world, Colony colony) {
        if (!hasShepherd(colony)) {
            return Map.of();
        }

        Box area = areaOf(colony);
        PenFlock flock = PenFlock.read(world, colony);
        Map<Item, Integer> keep = new HashMap<>();

        for (EntityType<? extends AnimalEntity> species : SPECIES) {
            List<List<AnimalEntity>> groups = breedingGroups(world, species, area, flock);
            List<AnimalEntity> adults = groups.stream().flatMap(List::stream).toList();

            if (adults.size() >= FLOCK_TARGET || groups.stream().noneMatch(group -> group.size() >= 2)) {
                continue;
            }

            for (Item food : foodOf(species, adults.get(0))) {
                keep.merge(food, PAIR_FOOD, Math::max);
            }
        }

        return keep;
    }

    /** Se esta comida paga um par agora: semente só acima do que fica para o plantio. */
    static boolean mayFeed(ServerWorld world, List<ColonyPos> chests, Item food) {
        int keep = CropPatch.isSeed(food) ? SEEDS_FOR_SOWING : 0;

        return ColonyChests.countIn(world, chests, food) - PAIR_FOOD >= keep;
    }

    static boolean hasShepherd(Colony colony) {
        return VillageColonyMod.WORKERS.ofColony(colony.id()).stream()
                .anyMatch(worker -> worker.profession().filter(ProfessionType.SHEPHERD::equals).isPresent());
    }

    /** Regra 68: alimentar e abater exigem um pastor vivo a até cinco blocos do animal. */
    static VillagerEntity nearbyShepherd(ServerWorld world, Colony colony, AnimalEntity animal) {
        return nearbyShepherd(world, colony, animal, animal);
    }

    private static VillagerEntity nearbyShepherd(ServerWorld world, Colony colony, AnimalEntity first, AnimalEntity second) {
        for (var worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (worker.profession().filter(ProfessionType.SHEPHERD::equals).isPresent()
                    && world.getEntity(worker.villagerId()) instanceof VillagerEntity shepherd
                    && shepherd.isAlive() && reachesBoth(shepherd, first, second)) {
                return shepherd;
            }
        }
        return null;
    }

    /**
     * A lã faltou e nenhuma ovelha tem lã — E7, decisão do autor de 2026-10-08: só
     * procria sob <b>déficit real</b> do rebanho. Abaixo de {@link #FLOCK_TARGET}
     * ovelhas adultas, um par sai agora, fora do relógio de {@link #BETWEEN_PAIRS};
     * com o rebanho cheio, a lã volta pelo pasto e não se cria nada.
     *
     * @return se um par de ovelhas foi posto para procriar
     */
    static boolean breedForWool(ServerWorld world, UUID colonyId) {
        return VillageColonyMod.COLONIES.find(colonyId)
                .map(colony -> breedForWool(world, colony, areaOf(colony),
                        ColonyChests.nearestFirst(world, colony.id(), colony.center())))
                .orElse(false);
    }

    static boolean breedForWool(ServerWorld world, Colony colony, Box area, List<ColonyPos> chests) {
        List<List<AnimalEntity>> groups = breedingGroups(world, EntityType.SHEEP, area, PenFlock.read(world, colony));
        int sheep = groups.stream().mapToInt(List::size).sum();
        if (sheep >= FLOCK_TARGET) {
            VillageColonyMod.LOGGER.info(
                    "Colony {} — no wool and the flock is full ({} sheep): the wool grows back on its own",
                    colony.id().toString().substring(0, 8), sheep);
            return false;
        }

        return pairOf(world, colony, EntityType.SHEEP, groups, sheep, FLOCK_TARGET, chests, null);
    }

    /** A ajuda ociosa usa o mesmo rebanho, o próprio pastor e somente trigo dos baús. */
    static boolean feedSheep(ServerWorld world, Colony colony, VillagerEntity shepherd, List<ColonyPos> chests) {
        var groups = breedingGroups(world, EntityType.SHEEP, areaOf(colony), PenFlock.read(world, colony));
        int adults = groups.stream().mapToInt(List::size).sum();
        return pairOf(world, colony, EntityType.SHEEP, groups, adults, FLOCK_TARGET, chests, shepherd);
    }

    /**
     * Onde o pastor procura ovelha com lã: o raio dele e, com a vila medida,
     * a mesma área em que o rebanho é contado.
     *
     * <p>Só o raio em volta do centro deixava o curral de fora: na colônia
     * 33a6b9c4 a cerca fica 43 blocos ao norte do centro, o rebanho contava
     * 16 ovelhas "cheio" e o pastor dizia "nenhuma com lã em 32 blocos" com
     * 10 delas lanudas no curral.
     */
    static Box shearingArea(Colony colony, BlockPos center, int radius) {
        Box around = new Box(center).expand(radius);

        return colony.bounds().isPresent() ? around.union(areaOf(colony)) : around;
    }

    static Box areaOf(Colony colony) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        return colony.bounds()
                .map(box -> new Box(box.minX() - 10, box.minY() - 8, box.minZ() - 10,
                        box.maxX() + 11, box.maxY() + 8, box.maxZ() + 11))
                .orElseGet(() -> new Box(center).expand(RADIUS));
    }

    /** Criação de corte por curral. A criação anterior à obra e as demais espécies usam a área da vila. */
    private static List<List<AnimalEntity>> breedingGroups(ServerWorld world,
            EntityType<? extends AnimalEntity> species, Box area, PenFlock flock) {
        if (LIVESTOCK.contains(species) && !flock.pens().isEmpty()) {
            return flock.pens().stream().map(stock -> stock.adults(species)).toList();
        }
        return List.of(world.getEntitiesByType(species, area, animal -> animal.isAlive() && !animal.isBaby())
                .stream().map(AnimalEntity.class::cast).toList());
    }

    private static int creationTarget(ServerWorld world, EntityType<?> species,
            List<AnimalEntity> adults, List<ColonyPos> chests) {
        return adults.isEmpty() ? FLOCK_TARGET : targetFor(foodOf(species, adults.getFirst()).stream()
                .mapToInt(food -> ColonyChests.countIn(world, chests, food)).sum());
    }

    private record Pair(VillagerEntity shepherd, AnimalEntity first, AnimalEntity second) {
    }

    private static Pair readyPair(ServerWorld world, Colony colony, List<List<AnimalEntity>> groups,
            VillagerEntity helper) {
        for (List<AnimalEntity> group : groups) {
            List<AnimalEntity> ready = group.stream().filter(animal -> animal.getBreedingAge() == 0
                    && !animal.isInLove() && !ShepherdHerding.isHerded(animal.getUuid())).toList();
            for (int i = 0; i < ready.size(); i++) {
                AnimalEntity first = ready.get(i);
                for (int j = i + 1; j < ready.size(); j++) {
                    AnimalEntity second = ready.get(j);
                    if (first.squaredDistanceTo(second) > 64.0) {
                        continue;
                    }
                    if (helper != null) {
                        if (reachesBoth(helper, first, second)) {
                            return new Pair(helper, first, second);
                        }
                    } else {
                        VillagerEntity shepherd = nearbyShepherd(world, colony, first, second);
                        if (shepherd != null) {
                            return new Pair(shepherd, first, second);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean reachesBoth(VillagerEntity shepherd, AnimalEntity first, AnimalEntity second) {
        return shepherd.squaredDistanceTo(first) <= ACTION_REACH_SQUARED
                && shepherd.squaredDistanceTo(second) <= ACTION_REACH_SQUARED;
    }

    private static boolean pairOf(ServerWorld world, Colony colony, EntityType<? extends AnimalEntity> species,
            List<List<AnimalEntity>> groups, int adults, int target, List<ColonyPos> chests, VillagerEntity helper) {
        if (adults < 2 || adults >= target) {
            return false;
        }
        Pair pair = readyPair(world, colony, groups, helper);
        if (pair == null) {
            if (helper == null) {
                PenLog.breedingWait(colony, species, adults, target, "no_reachable_ready_pair");
            }
            return false;
        }

        List<Item> foods = helper == null ? foodOf(species, pair.first()) : List.of(Items.WHEAT);
        for (Item food : foods) {
            if (mayFeed(world, chests, food) && ColonyChests.withdraw(world, chests, food, PAIR_FOOD) == PAIR_FOOD) {
                feed(world, colony, pair, food, adults, target, "chest");
                return true;
            }
        }

        // Regra 117: a ração só completa a meta base e só entra quando a fazenda não entregou nada.
        Item ration = ANIMAL_FEED.get(species);
        if (helper != null || ration == null || adults >= FLOCK_TARGET
                || foods.stream().anyMatch(food -> ColonyChests.countIn(world, chests, food) > 0)) {
            if (helper == null) {
                PenLog.breedingWait(colony, species, adults, target, "no_available_food");
            }
            return false;
        }

        Supplies.record(SupplyException.ANIMAL_FEED, colony.id(), Registries.ITEM.getId(ration).getPath(), PAIR_FOOD);
        feed(world, colony, pair, ration, adults, target, "ration");
        return true;
    }

    private static void feed(ServerWorld world, Colony colony, Pair pair, Item food, int adults, int target,
            String source) {
        pair.first().lovePlayer(null);
        pair.second().lovePlayer(null);
        PenLog.fed(world, colony, pair.shepherd(), pair.first(), pair.second(), food, adults, target, source);
        ActionJournal.action(pair.shepherd().getUuid(), "FED", ActionJournal.idOf(food), PAIR_FOOD,
                pair.first().getBlockPos(), "the flock breeds");
    }

    /** A meta de adultos da espécie pela comida dela nos baús (Regra 238). */
    public static int targetFor(int food) {
        return food >= SURPLUS_FOOD ? SURPLUS_TARGET : FLOCK_TARGET;
    }

    /** Guardado: amarrado numa cerca, ou dentro de curral (cercado e com cerca perto). */
    static boolean isKept(ServerWorld world, AnimalEntity animal) {
        return animal.getLeashHolder() instanceof LeashKnotEntity || isPenned(world, animal);
    }

    /** Dentro de curral: cerca próxima e alcance suficiente para o modelo de 15×15 — ADR-046. */
    static boolean isPenned(ServerWorld world, AnimalEntity animal) {
        return FencedIn.isNearAFence(world, animal.getBlockPos())
                && FencedIn.check(world, animal.getBlockPos(), 2 * FencedIn.RADIUS).enclosed();
    }

    /** Toda comida do jogo que esta espécie aceita, guardada na primeira pergunta. */
    private static List<Item> foodOf(EntityType<?> species, AnimalEntity sample) {
        return FOOD.computeIfAbsent(species, type -> Registries.ITEM.stream()
                .filter(item -> item != Items.AIR && sample.isBreedingItem(new ItemStack(item)))
                .toList());
    }

    public static void clearAll() {
        LAST.clear();
        FOOD.clear();
    }
}
