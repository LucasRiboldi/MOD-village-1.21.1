package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;

import net.minecraft.block.Block;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Quando e onde o lenhador planta a árvore do viveiro — 2026-09-19;
 * do fazendeiro para o lenhador em 2026-09-30, decisão do autor.
 *
 * <p>A habilidade é do autor e está descrita em {@link TreeNursery}; o
 * que mora aqui é o <b>ritmo</b> e o <b>lugar</b>, que são as duas
 * decisões que fazem a diferença entre uma vila com árvores e um deserto
 * coberto de terra enraizada.
 *
 * <p><b>Na borda, e não no meio.</b> O pedido é <i>"no limite da vila"</i>,
 * e há razão prática: árvore no miolo tomaria lote de casa, e o
 * {@code BuildSiteScanner} passaria a recusar por volume ocupado — que foi
 * 70% das recusas no P1.6. A borda <b>não</b> está livre de obra: a busca de
 * lote vai até 64 do centro, e a casa do pastor do playtest de 2026-09-30
 * nasceu a 50. Quem impede o rebento no espaço de uma obra é
 * {@code TreeNursery.isSpotForANursery}.
 *
 * <p><b>Dois momentos, com intervalo.</b> O lenhador planta um lote
 * quando não acha árvore ao alcance ({@link #plantBatchIfItIsTime}) e uma
 * muda depois de cada árvore que derruba inteira ({@link #plantIfItIsTime}).
 * Sem freio ele plantaria a cada passagem até a borda inteira virar
 * viveiro.
 */
public final class LumberjackNursery {

    static {
        ServerMemory.register(LumberjackNursery.class, LumberjackNursery::clearAll);
    }

    /**
     * Quantos tiques entre um plantio e o seguinte.
     *
     * <p>Seis mil — cinco minutos. Uma árvore leva mais que isso para
     * crescer, então plantar mais rápido só encheria a borda de rebentos
     * que ainda não são madeira.
     */
    public static final int BETWEEN_PLANTINGS = 6_000;

    /** Meta-base de árvores vivas mantidas pelo viveiro de cada vila. */
    public static final int TARGET_TREES = 10;

    /** Mínimo adicional de árvores por lenhador ativo na vila. */
    public static final int MIN_TREES_PER_LUMBERJACK = 5;

    /**
     * A que distância do centro o viveiro fica.
     *
     * <p>Dentro do alcance do lenhador — ele procura árvore em volta do
     * centro —, e fora do miolo onde as obras nascem. Plantar além do
     * alcance dele seria plantar para ninguém colher.
     */
    static final int EDGE = 56;

    /** Limite interno para não plantar dentro do miolo da vila. */
    private static final int INNER_EDGE = 48;

    /**
     * O anel em volta do baú do lenhador, na altura dele, tentado antes da borda quando o baú fica mais de
     * {@code NEAR_CHEST_HEIGHT + 1} acima ou abaixo do centro (a vila em duas alturas) — deserto jogado,
     * 2026-10-21: o lenhador mora na parte baixa da vila (y63) e as mudas da borda ficaram 11 a 20
     * blocos acima; ele andou até 56 blocos por árvore e cortou 12 toras na sessão.
     */
    static final int NEAR_CHEST_MIN = 6;

    static final int NEAR_CHEST_MAX = 14;

    /** Quanto acima ou abaixo do baú o chão do viveiro perto dele pode estar. */
    private static final int NEAR_CHEST_HEIGHT = 3;

    /** Resolução do anel de candidatos, percorrido do mais distante ao centro. */
    private static final int DIRECTIONS = 48;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private LumberjackNursery() {
    }

    /** Se já passou tempo bastante desde o último plantio desta colônia. */
    public static boolean isTime(UUID colonyId, long now) {
        Long last = LAST.get(colonyId);

        return last == null || now - last >= BETWEEN_PLANTINGS;
    }

    /**
     * Planta uma árvore na borda, se for hora e houver lugar.
     *
     * @return {@code true} se uma árvore nasceu agora
     */
    public static boolean plantIfItIsTime(ServerWorld world, UUID colonyId, BlockPos centre) {
        return plant(world, colonyId, centre, 1) > 0;
    }

    private static int plant(ServerWorld world, UUID colonyId, BlockPos centre, int wanted) {
        // Primeiro o bosque de três mudas perto da vila (pedido do autor, 2026-10-08).
        VillageGrove.tend(world, colonyId);

        if (!isTime(colonyId, world.getTime())) {
            return 0;
        }

        double towards = towardsTheLumberjack(colonyId, centre);

        Optional<Block> sapling =
                TreeNursery.saplingFor(world, MinecraftTypeAdapter.toColonyPos(centre));

        if (sapling.isEmpty()) {
            // Bioma sem madeira declarada: não é vila que o mod atende, e
            // inventar uma espécie aqui seria escolher por conta própria.
            return 0;
        }

        Optional<BlockPos> chest = lumberjackChest(colonyId);
        int room = Math.min(wanted, targetTrees(colonyId) - countNurseries(world, centre,
                chest.map(at -> Math.min(centre.getY() - 8, at.getY() - NEAR_CHEST_HEIGHT - 1)).orElse(centre.getY() - 8)));

        if (room <= 0) {
            // <b>Cheio também marca a hora</b> — spark de 2026-09-26. Com os
            // dez viveiros de pé, cada chamada recontava ~166 mil blocos e
            // não guardava nada; o lenhador sem árvore chama
            // o tempo todo, e a conta virou o terceiro maior custo do mod.
            // Cheio agora espera o mesmo intervalo de quem plantou.
            LAST.put(colonyId, world.getTime());

            return 0;
        }

        int planted = 0;

        while (planted < room) {
            // O anel do baú só na vila em duas alturas: o baú longe da altura do centro.
            Optional<BlockPos> spot = chest.filter(at -> Math.abs(at.getY() - centre.getY()) > NEAR_CHEST_HEIGHT + 1)
                    .flatMap(at -> spotNearTheChest(world, colonyId, at))
                    .or(() -> spotOnTheEdge(world, centre, towards));

            // O lugar já foi conferido pela busca (borda ou anel do baú).
            if (spot.isEmpty() || !TreeNursery.plantAt(world, spot.get(), sapling.get())) {
                break;
            }

            planted++;
            com.villagecolony.fabric.integration.VillageTrees.rememberSapling(colonyId, spot.get().up());

            VillageColonyMod.LOGGER.info(
                    "Colony {} — the lumberjack planted {} on rooted dirt at {}, near its chest or at the village edge",
                    colonyId.toString().substring(0, 8),
                    TreeNursery.idOf(sapling.get()),
                    spot.get().toShortString());
        }

        // Sem ponto livre, o lenhador sem arvore voltaria aqui a cada tick e
        // repetiria a contagem cara do viveiro. A tentativa vale pelo mesmo
        // intervalo de quem plantou ou encontrou a borda ja cheia.
        LAST.put(colonyId, world.getTime());

        return planted;
    }

    /**
     * Quantas mudas o lenhador sem árvore pede de uma vez — sessão de
     * 2026-09-26. A vila de planície sem árvore natural esperou uma muda a cada
     * cinco minutos, e o lenhador cortou 15 toras em 33 minutos. Com quatro, o
     * teto de dez fecha em três plantios.
     */
    public static final int BATCH = 4;

    /**
     * O plantio do lenhador que não achou árvore: até {@link #BATCH} mudas,
     * no mesmo ritmo e no mesmo teto do plantio avulso.
     *
     * @return quantas mudas nasceram agora
     */
    public static int plantBatchIfItIsTime(ServerWorld world, UUID colonyId, BlockPos centre) {
        return plant(world, colonyId, centre, BATCH);
    }

    /**
     * Meta da vila: conserva o bosque inicial e sobe para cinco árvores por
     * lenhador quando a equipe cresce.
     *
     * <p>A meta-base também mantém os plantios explícitos e as arenas de teste
     * estáveis. O despacho automático, por sua vez, só chama o viveiro quando
     * há ao menos um lenhador registrado em {@link #hasLumberjack(UUID)}.
     */
    static int targetTrees(UUID colonyId) {
        int lumberjacks = (int) VillageColonyMod.WORKERS.ofColony(colonyId).stream()
                .filter(worker -> worker.profession()
                        .filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals)
                        .isPresent())
                .count();

        return targetTreesFor(lumberjacks);
    }

    static int targetTreesFor(int lumberjacks) {
        return Math.max(TARGET_TREES, Math.max(0, lumberjacks) * MIN_TREES_PER_LUMBERJACK);
    }

    /** Evita a varredura do viveiro em ciclos que não têm lenhador. */
    static boolean hasLumberjack(UUID colonyId) {
        return VillageColonyMod.WORKERS.ofColony(colonyId).stream()
                .anyMatch(worker -> worker.profession()
                        .filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals)
                        .isPresent());
    }

    /**
     * Um ponto na borda que sirva de viveiro.
     *
     * <p>Anda pelo anel externo para dentro. Assim a árvore fica o mais
     * longe possível do centro sem sair do alcance do lenhador; só usa uma
     * distância menor quando a borda está ocupada ou inacessível.
     */
    private static Optional<BlockPos> spotOnTheEdge(ServerWorld world, BlockPos centre, double towards) {
        for (int radius = EDGE; radius >= INNER_EDGE; radius--) {
            for (int turn = 0; turn < DIRECTIONS; turn++) {
                // Do lado da borda mais perto do baú do lenhador para fora — A-7,
                // 2026-10-02: o viveiro continua na borda, mas no ponto dela de
                // onde o lenhador anda menos.
                int step = (turn % 2 == 0 ? turn / 2 : DIRECTIONS - (turn + 1) / 2);
                double angle = towards + 2 * Math.PI * step / DIRECTIONS;

                int x = centre.getX() + (int) Math.round(radius * Math.cos(angle));
                int z = centre.getZ() + (int) Math.round(radius * Math.sin(angle));

                Optional<BlockPos> ground = groundAt(world, x, z, centre.getY());

                if (ground.isPresent() && TreeNursery.isSpotForANursery(world, ground.get())) {
                    return ground;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Um viveiro no anel em volta do baú do lenhador, com o chão na altura do baú e dentro da caixa da
     * vila — fora dela a muda cai no terreno de outro (na bateria, no cenário vizinho).
     */
    static Optional<BlockPos> spotNearTheChest(ServerWorld world, UUID colonyId, BlockPos chest) {
        Optional<com.villagecolony.core.colony.model.VillageBounds> box =
                VillageColonyMod.COLONIES.find(colonyId).flatMap(com.villagecolony.core.colony.model.Colony::bounds);

        if (box.isEmpty()) {
            return Optional.empty();
        }

        for (int radius = NEAR_CHEST_MIN; radius <= NEAR_CHEST_MAX; radius++) {
            for (int turn = 0; turn < DIRECTIONS; turn++) {
                double angle = 2 * Math.PI * turn / DIRECTIONS;
                int x = chest.getX() + (int) Math.round(radius * Math.cos(angle));
                int z = chest.getZ() + (int) Math.round(radius * Math.sin(angle));

                if (x < box.get().minX() || x > box.get().maxX() || z < box.get().minZ() || z > box.get().maxZ()
                        || world.getChunkManager().getWorldChunk(x >> 4, z >> 4) == null) {
                    continue;
                }

                for (int y = chest.getY() + NEAR_CHEST_HEIGHT; y >= chest.getY() - NEAR_CHEST_HEIGHT; y--) {
                    BlockPos at = new BlockPos(x, y, z);

                    if (!world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()) {
                        if (TreeNursery.isSpotForANursery(world, at) || isOpenNaturalGround(world, at)) {
                            return Optional.of(at);
                        }

                        break;
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Chão natural da vila gerada, a céu aberto — deserto jogado, 2026-10-21: em volta do baú do
     * lenhador havia 77 lugares de areia, todos dentro das peças da vila e por isso recusados pelo
     * viveiro de sempre. Aqui vale a areia ou a terra (o chão que o gerador assentou, como na
     * BlockProtection.mayBuildOver), nunca dentro de casa, nem na faixa da rua, nem em obra.
     */
    static boolean isOpenNaturalGround(ServerWorld world, BlockPos ground) {
        net.minecraft.block.BlockState state = world.getBlockState(ground);

        return (state.isIn(BlockTags.SAND) || state.isIn(BlockTags.DIRT))
                && !state.isOf(TreeNursery.BED)
                && world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, ground.getX(), ground.getZ())
                        == ground.getY() + 1
                && !com.villagecolony.fabric.integration.PlayerClearedPlants.isCleared(ground)
                && !com.villagecolony.fabric.integration.ConstructionZone.nearRoad(world, ground)
                && !com.villagecolony.fabric.integration.BlockProtection.isColonyBuilt(ground)
                && !com.villagecolony.fabric.integration.BlockProtection.isColonyBuilt(ground.up());
    }

    /** O ângulo, a partir do centro, do baú do lenhador da colônia; zero sem lenhador com baú. */
    private static double towardsTheLumberjack(UUID colonyId, BlockPos centre) {
        return lumberjackChest(colonyId)
                .map(at -> Math.atan2(at.getZ() - centre.getZ(), at.getX() - centre.getX()))
                .orElse(0.0);
    }

    /** O baú do primeiro lenhador da colônia que tem um. */
    private static Optional<BlockPos> lumberjackChest(UUID colonyId) {
        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            if (worker.profession().filter(com.villagecolony.core.worker.model.ProfessionType.LUMBERJACK::equals)
                    .isEmpty()) {
                continue;
            }

            var chest = VillageColonyMod.STORAGES.of(worker.villagerId());

            if (chest.isPresent()) {
                return Optional.of(MinecraftTypeAdapter.toBlockPos(chest.get().chestPosition()));
            }
        }

        return Optional.empty();
    }

    /** Conta marcadores de viveiro que ainda têm muda ou árvore em cima. */
    private static int countNurseries(ServerWorld world, BlockPos centre, int lowest) {
        int count = 0;

        for (int x = centre.getX() - EDGE; x <= centre.getX() + EDGE; x++) {
            for (int z = centre.getZ() - EDGE; z <= centre.getZ() + EDGE; z++) {
                for (int y = centre.getY() + 4; y >= lowest; y--) {
                    BlockPos ground = new BlockPos(x, y, z);

                    if (!world.getBlockState(ground).isOf(TreeNursery.BED)
                            || (!world.getBlockState(ground.up()).isIn(BlockTags.SAPLINGS)
                            && !world.getBlockState(ground.up()).isIn(BlockTags.LOGS))) {
                        continue;
                    }

                    count++;
                    break;
                }
            }
        }

        return count;
    }

    /**
     * O chão nesta coluna, perto da altura da vila.
     *
     * <p>A janela é a mesma do {@code SandPatch} e pela mesma razão: a
     * vila não planta no alto do morro que a olha de cima nem no fundo do
     * desfiladeiro.
     */
    private static Optional<BlockPos> groundAt(ServerWorld world, int x, int z, int centreY) {
        for (int y = centreY + 4; y >= centreY - 8; y--) {
            BlockPos at = new BlockPos(x, y, z);

            if (!world.getBlockState(at).isAir() && world.getBlockState(at.up()).isAir()) {
                return Optional.of(at);
            }
        }

        return Optional.empty();
    }

    /** Marca que esta colônia plantou agora. Para a bateria. */
    public static void remember(UUID colonyId, long now) {
        LAST.put(colonyId, now);
    }

    /** Esquece o ritmo guardado. Chamado ao parar o servidor. */
    public static void clearAll() {
        LAST.clear();
    }
}
