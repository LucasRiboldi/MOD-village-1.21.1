package com.villagecolony.fabric.world;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O que o mundo diz da vila sem o mod perguntar ao resto do mod — ADR-003
 * Emenda 6, 2026-09-30.
 *
 * <p>Pacote à parte e de propósito: as quatro partes da camada Fabric
 * ({@code adapter}, {@code integration}, {@code event}, {@code work}) estão
 * em ciclo congelado no ArchUnit, e qualquer chamada nova entre elas reprova.
 * Esta classe só depende do jogo e do Core, e por isso todas podem usá-la.
 * Recebe coordenada em inteiro, sem passar pelo adaptador.
 */
public final class GeneratedVillages {

    private GeneratedVillages() {
    }

    /**
     * As vilas cuja caixa do jogo (as peças + 12) alcança este chunk. A referência da vila vai mais longe — até
     * {@code VillageGround.REACH}, para o solo da transição ser moldado (autor, 2026-10-29) —, mas o que o mod
     * pergunta da vila (medida, casas, estilo, fusão de colônias) continua com o alcance do jogo.
     */
    public static List<StructureStart> villagesReaching(ServerWorld world, int chunkX, int chunkZ) {
        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

        return world.getStructureAccessor().getStructureStarts(chunk,
                        structure -> registry.getEntry(structure).isIn(StructureTags.VILLAGE)).stream()
                .filter(start -> reaches(start, chunk))
                .toList();
    }

    /** Se a caixa do jogo desta vila (as peças + 12) toca o chunk. */
    public static boolean reaches(StructureStart start, ChunkPos chunk) {
        return start.hasChildren() && start.getBoundingBox().intersectsXZ(
                chunk.getStartX(), chunk.getStartZ(), chunk.getEndX(), chunk.getEndZ());
    }

    /** As casas de dois andares ligados só por escada de mão (Vanilla): o aldeão fica preso na escada. */
    static final List<String> LADDER_HOUSES = List.of("/houses/plains_small_house_5]");

    /** Até que altura, acima do pé da peça, vai o andar de baixo dela (as camas da planta ficam em y 2). */
    static final int LOWER_FLOOR = 4;

    /**
     * O andar de baixo das casas de dois andares da vila que alcança esta coluna — jogo da 0.3.108: 9 dos 21
     * encalhes e teleportes foram junto da escada de mão da {@code plains_small_house_5}, ao lado das camas.
     * A colônia não dá ao aldeão cama ali (autor, 2026-10-29).
     */
    public static List<BlockBox> ladderLowerFloorsAt(ServerWorld world, int x, int z) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return List.of();
        }

        List<BlockBox> floors = new ArrayList<>();

        for (StructureStart start : villagesReaching(world, x >> 4, z >> 4)) {
            for (var piece : start.getChildren()) {
                if (piece instanceof PoolStructurePiece pool
                        && LADDER_HOUSES.stream().anyMatch(pool.getPoolElement().toString()::contains)) {
                    BlockBox box = piece.getBoundingBox();
                    floors.add(new BlockBox(box.getMinX(), box.getMinY(), box.getMinZ(),
                            box.getMaxX(), box.getMinY() + LOWER_FLOOR, box.getMaxZ()));
                }
            }
        }

        return floors;
    }

    /**
     * A caixa da vila gerada que alcança esta coluna, se houver e o chunk
     * estiver carregado — pedir um chunk descarregado o carregaria no tique.
     */
    public static Optional<VillageBounds> boundsAt(ServerWorld world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return Optional.empty();
        }

        VillageBounds found = null;

        for (StructureStart start : villagesReaching(world, chunkX, chunkZ)) {
            if (!start.hasChildren()) {
                continue;
            }

            // As peças, e não a caixa da estrutura — 2026-10-03, autor: "o
            // tamanho original do Vanilla". A caixa da estrutura soma 12 blocos
            // de folga em cada lado (a vila do autor: 166 × 140 contra 142 ×
            // 116 das peças).
            for (var piece : start.getChildren()) {
                BlockBox box = piece.getBoundingBox();
                VillageBounds bounds = new VillageBounds(
                        box.getMinX(), box.getMinY(), box.getMinZ(),
                        box.getMaxX(), box.getMaxY(), box.getMaxZ());

                found = found == null ? bounds : found.union(bounds);
            }
        }

        return Optional.ofNullable(found);
    }

    /**
     * As caixas das casas da vila gerada que alcança esta coluna — as peças {@code village/<estilo>/houses/...}, se o
     * chunk estiver carregado. Servem para ligar a porta das casas originais à rua (autor, 2026-10-28).
     */
    public static List<BlockBox> housesAt(ServerWorld world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return List.of();
        }

        List<BlockBox> houses = new ArrayList<>();

        for (StructureStart start : villagesReaching(world, chunkX, chunkZ)) {
            for (var piece : start.getChildren()) {
                if (piece instanceof PoolStructurePiece pool && pool.getPoolElement().toString().contains("/houses/")) {
                    houses.add(piece.getBoundingBox());
                }
            }
        }

        return houses;
    }

    /**
     * Qual vila o jogo gerou nesta coluna ({@code minecraft:village_snowy}, ...), se houver e o
     * chunk estiver carregado. É ela que dá o estilo da colônia — decisão do autor, 2026-10-20.
     */
    public static Optional<Identifier> structureAt(ServerWorld world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return Optional.empty();
        }

        var registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (StructureStart start : villagesReaching(world, chunkX, chunkZ)) {
            if (start.hasChildren()) {
                return Optional.ofNullable(registry.getId(start.getStructure()));
            }
        }

        return Optional.empty();
    }

    /**
     * A janela de altura das camas em volta desta coluna: a da vila medida que
     * a contém (com a folga de identidade), ou a da descoberta.
     *
     * @return {mínimo, máximo}
     */
    public static int[] bedWindowAround(ServerWorld world, int x, int z) {
        VillageBounds known = null;

        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Optional<VillageBounds> bounds = colony.bounds();

            if (bounds.isPresent()
                    && bounds.get().containsColumn(x, z, VillageBounds.IDENTITY_MARGIN)) {
                known = known == null ? bounds.get() : known.union(bounds.get());
            }
        }

        return known != null
                ? new int[] {known.bedFloor(), known.bedCeiling()}
                : bedWindowAt(world, x, z);
    }

    /**
     * A janela de altura em que uma cama pode ser de vila, quando ainda não se
     * sabe de que vila — a descoberta.
     *
     * <p>Com vila gerada no lugar, é a faixa dela; sem, vai de 16 abaixo a
     * {@link VillageBounds#BEDS_ABOVE} acima da superfície da coluna. Quem
     * está numa caverna ou numa Trial Chamber continua procurando pela
     * superfície acima dele, e não em volta de si.
     *
     * @return {mínimo, máximo}
     */
    public static int[] bedWindowAt(ServerWorld world, int x, int z) {
        Optional<VillageBounds> generated = boundsAt(world, x, z);

        if (generated.isPresent()) {
            return new int[] {generated.get().bedFloor(), generated.get().bedCeiling()};
        }

        int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

        return new int[] {surface - 16, surface + VillageBounds.BEDS_ABOVE};
    }
}
