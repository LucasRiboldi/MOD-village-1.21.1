package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.world.GeneratedVillages;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * De que bioma é o estilo desta vila — decisão do autor, 2026-10-20.
 *
 * <p>Até aqui o bioma era lido num ponto só, o centro da colônia, e a bancada de simulação mostrou
 * o custo: a vila de neve de 19 camas tinha o centro num pedaço de floresta, a de taiga à beira de
 * um oceano frio, e as duas eram ignoradas. Agora, nesta ordem:
 *
 * <ol>
 *   <li>a vila que o jogo gerou ali ({@code village_snowy} → planície nevada, ...);
 *   <li>sem vila gerada (feita pelo jogador), o bioma atendido mais comum entre as camas;
 *   <li>sem nenhum bioma atendido, planície.
 * </ol>
 *
 * <p>Toda vila é atendida, em qualquer bioma: o bioma decide o estilo, não se ela existe.
 */
public final class VillageStyle {

    static {
        ServerMemory.register(VillageStyle.class, VillageStyle::clearAll);
    }

    /** Quanto a resposta vale: a vila não muda de estilo de um minuto para o outro. */
    static final long VALID_TICKS = 1_200;

    private static final Map<String, RegistryKey<Biome>> OF_GENERATED = Map.of(
            "minecraft:village_plains", BiomeKeys.PLAINS,
            "minecraft:village_desert", BiomeKeys.DESERT,
            "minecraft:village_savanna", BiomeKeys.SAVANNA,
            "minecraft:village_snowy", BiomeKeys.SNOWY_PLAINS,
            "minecraft:village_taiga", BiomeKeys.TAIGA);

    private record Answer(RegistryKey<Biome> biome, long until) {
    }

    private static final Map<Long, Answer> CACHE = new HashMap<>();

    private VillageStyle() {
    }

    /** O bioma que dá o estilo da vila que está neste lugar. Nunca vazio. */
    public static RegistryKey<Biome> biomeAt(ServerWorld world, ColonyPos where) {
        BlockPos at = MinecraftTypeAdapter.toBlockPos(where);
        long chunk = ChunkPos.toLong(at.getX() >> 4, at.getZ() >> 4);
        long now = world.getTime();
        Answer known = CACHE.get(chunk);

        if (known != null && known.until() > now) {
            return known.biome();
        }

        RegistryKey<Biome> biome = resolve(world, at);

        // O centro anda; a resposta vencida de onde ele esteve não fica para sempre.
        if (CACHE.size() >= 64) {
            CACHE.values().removeIf(answer -> answer.until() <= now);
        }

        CACHE.put(chunk, new Answer(biome, now + VALID_TICKS));

        return biome;
    }

    private static RegistryKey<Biome> resolve(ServerWorld world, BlockPos at) {
        Optional<RegistryKey<Biome>> generated = GeneratedVillages.structureAt(world, at.getX(), at.getZ())
                .map(id -> OF_GENERATED.get(id.toString()));

        if (generated.isPresent()) {
            return generated.get();
        }

        List<RegistryKey<Biome>> bedBiomes = world.getPointOfInterestStorage()
                .getInSquare(type -> type.matchesKey(PointOfInterestTypes.HOME), at,
                        VillageDetector.SEARCH_RADIUS, PointOfInterestStorage.OccupationStatus.ANY)
                .map(bed -> world.getBiome(bed.getPos()).getKey())
                .flatMap(Optional::stream)
                .toList();

        return majority(bedBiomes, biome -> VillageBiomes.woodFor(biome).isPresent(),
                Comparator.comparing(biome -> biome.getValue().toString()))
                .orElse(BiomeKeys.PLAINS);
    }

    /**
     * O mais comum entre os aceitos; empate vai para o primeiro na ordem dada, para a resposta não
     * depender da ordem em que as camas foram lidas.
     */
    static <K> Optional<K> majority(List<K> seen, Predicate<K> accepted, Comparator<K> tieBreak) {
        Map<K, Integer> counts = new HashMap<>();

        for (K each : seen) {
            if (accepted.test(each)) {
                counts.merge(each, 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .min(Comparator.<Map.Entry<K, Integer>>comparingInt(entry -> -entry.getValue())
                        .thenComparing(Map.Entry::getKey, tieBreak))
                .map(Map.Entry::getKey);
    }

    static void clearAll() {
        CACHE.clear();
    }
}
