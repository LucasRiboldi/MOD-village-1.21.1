package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.construction.model.VillagePalette;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Onde há vila, e de que madeira ela é — a Regra 20.
 *
 * <p>Duas perguntas que são a mesma tabela. Até 2026-08-19 o mod
 * atendia só planície: {@code VillageScanner} descartava em silêncio
 * todo aglomerado fora de PLAINS, e a colônia construía sempre em
 * carvalho porque não havia outra coisa a construir.
 *
 * <p>A tabela é dos biomas em que o jogo <b>gera vila</b>, mais as
 * variantes vizinhas em que uma vila pode se estender. Estar aqui é o
 * que faz o mod aceitar a vila; a madeira que acompanha é o estilo dela.
 *
 * <p><b>Por que uma tabela, e não uma leitura do mundo.</b> Dava para
 * olhar as árvores em volta e deduzir a espécie. Seria mais esperto e
 * seria pior: vila de planície com um bosque de bétula ao lado passaria
 * a construir em bétula, e duas colônias do mesmo bioma sairiam
 * diferentes conforme o que nasceu perto. A tabela é previsível, e vila
 * é uma estrutura do jogo com estilo definido por bioma — a mesma
 * escolha que o gerador faz.
 *
 * <p><b>O deserto é honesto sobre si.</b> Ele está na tabela porque o
 * jogo gera vila lá, e a colônia nasce, contrata e conta recurso. O que
 * ela não faz é construir: não há árvore no deserto, e o lenhador não
 * tem o que cortar. A casa sai em carvalho no dia em que o jogador
 * guardar madeira num baú.
 */
public final class VillageBiomes {

    private static final ResourceId OAK = plank("oak");

    private static final ResourceId SPRUCE = plank("spruce");

    private static final ResourceId ACACIA = plank("acacia");

    /**
     * De que madeira é a vila de cada bioma.
     *
     * <p>Ordem de leitura, não de precedência: a consulta é por chave.
     */
    private static final Map<RegistryKey<Biome>, ResourceId> WOOD = wood();

    private VillageBiomes() {
    }

    private static Map<RegistryKey<Biome>, ResourceId> wood() {
        Map<RegistryKey<Biome>, ResourceId> table = new LinkedHashMap<>();

        // Planície e suas vizinhas: a vila de carvalho, e o único caso
        // que o mod atendia antes desta regra.
        table.put(BiomeKeys.PLAINS, OAK);
        table.put(BiomeKeys.SUNFLOWER_PLAINS, OAK);
        table.put(BiomeKeys.MEADOW, OAK);

        // Taiga e as nevadas: pinheiro, que é a madeira das duas.
        table.put(BiomeKeys.TAIGA, SPRUCE);
        table.put(BiomeKeys.SNOWY_TAIGA, SPRUCE);
        table.put(BiomeKeys.OLD_GROWTH_PINE_TAIGA, SPRUCE);
        table.put(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA, SPRUCE);
        table.put(BiomeKeys.SNOWY_PLAINS, SPRUCE);

        // Savana: acácia.
        table.put(BiomeKeys.SAVANNA, ACACIA);
        table.put(BiomeKeys.SAVANNA_PLATEAU, ACACIA);

        // Deserto: sem árvore. A madeira aqui é a de reserva, para
        // quando o jogador guardar tronco no baú; a parede da casa é
        // arenito, e quem decide isso é a paleta.
        table.put(BiomeKeys.DESERT, OAK);

        return Map.copyOf(table);
    }

    /**
     * A madeira da vila deste bioma.
     *
     * <p>Vazio quer dizer que o jogo não gera vila aqui, e é assim que
     * a varredura decide se o aglomerado vira colônia. Não é recusa: a
     * vila que exista fora desta lista continua lá, viva, e o mod é que
     * não a atende — ver ADR-003 §5.
     */
    /**
     * A paleta desta vila — a Regra 20 dita por inteiro, 2026-08-20.
     *
     * <p>Até aqui o estilo do bioma era a espécie da madeira e nada
     * mais, e por isso o deserto ficava de fora: a vila nascia,
     * contratava e nunca construía. Agora o bioma responde também de que
     * é a parede, e no deserto ela é arenito — que o mineiro tira da
     * duna ao lado.
     */
    public static Optional<VillagePalette> paletteFor(RegistryKey<Biome> biome) {
        if (BiomeKeys.DESERT.equals(biome)) {
            return Optional.of(VillagePalette.ofSandstone());
        }

        return woodFor(biome).map(VillagePalette::ofWood);
    }

    /** A paleta da vila que está neste lugar. */
    public static Optional<VillagePalette> paletteAt(ServerWorld world, ColonyPos where) {
        return world.getBiome(MinecraftTypeAdapter.toBlockPos(where))
                .getKey()
                .flatMap(VillageBiomes::paletteFor);
    }

    public static Optional<ResourceId> woodFor(RegistryKey<Biome> biome) {
        return Optional.ofNullable(WOOD.get(biome));
    }

    /** O mesmo, perguntando ao mundo qual é o bioma deste ponto. */
    public static Optional<ResourceId> woodAt(ServerWorld world, ColonyPos where) {
        return world.getBiome(MinecraftTypeAdapter.toBlockPos(where))
                .getKey()
                .flatMap(VillageBiomes::woodFor);
    }

    /** Se o mod atende vila neste ponto. */
    public static boolean hasVillages(ServerWorld world, ColonyPos where) {
        return woodAt(world, where).isPresent();
    }

    private static ResourceId plank(String species) {
        return new ResourceId(ResourceId.VANILLA, species + "_planks");
    }
}
