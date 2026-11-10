package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.ColonySavedData;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A obra MAE pedida pelo jogador — Regra 213, ADR-047: o lote é conferido uma vez e a obra entra na fila do construtor
 * ({@link MaeBuildQueue}), que a levanta como qualquer outra, bloco a bloco. A escolha do lote protege o que é da vila e do
 * jogador; durante a obra quem responde é o construtor (Regra 3), e o mundo vivo não a para.
 */
public final class MaeSites {
    /** Teto de obras pedidas à espera ou em andamento, por colônia. */
    static final int MAX_QUEUED = 4;
    /** Raio da busca de lote em volta do jogador. */
    static final int SEARCH_REACH = 24;
    /** Quantos lotes uma busca confere: cada um lê o volume inteiro. */
    private static final int LOTS_TRIED = 32;
    /** A folga conferida em volta do lote: o anel de chão da Regra 131. */
    static final int LOT_MARGIN = 3;
    /** A folga que a caixa da estrutura soma às peças da vila, e a da conferência. */
    private static final int OUTSIDE_CLEARANCE = 12 + LOT_MARGIN + 1;
    /** Até onde, além da folga, se procura lote fora da vila. */
    private static final int OUTSIDE_REACH = 32;
    /** De quanto em quanto tempo se soltam as regiões das versões em que o MAE escrevia a obra. */
    private static final int OLD_REGIONS_EVERY = 100;

    /** Em que pé está a obra pedida. */
    public enum Phase { CLEARING, QUEUED, BUILDING, DONE }

    /** Uma obra pedida, para o estado mostrado ao jogador. */
    public record Status(UUID id, ResourceId model, BlockPos origin, int quarterTurns, Phase phase, String detail) { }

    private MaeSites() { }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MaeSites::tick);
    }

    /** Obra com o piso pedido; recusa com o motivo da leitura ou da conferência do lote. */
    public static Status start(ServerWorld world, UUID colonyId, ResourceId model, BlockPos floor,
                               int quarterTurns, @Nullable UUID owner) {
        if (MaeClearing.isBusy()) throw new IllegalStateException("too_many_sites");
        return start(world, colonyId, model, floor, quarterTurns, owner, true);
    }

    private static Status start(ServerWorld world, UUID colonyId, ResourceId model, BlockPos floor,
                                int quarterTurns, @Nullable UUID owner, boolean mayClear) {
        long began = System.nanoTime();

        if (quarterTurns < 0 || quarterTurns > 3) throw new IllegalArgumentException("invalid_rotation");
        if (VillageColonyMod.COLONIES.find(colonyId).isEmpty()) throw new IllegalStateException("unknown_colony");
        if (MaeBuildQueue.ofColony(world, colonyId).size() >= MAX_QUEUED) throw new IllegalStateException("too_many_sites");

        Blueprint blueprint = blueprint(world, model, quarterTurns);
        ColonyPos origin = blueprint.originFor(MinecraftTypeAdapter.toColonyPos(floor));
        List<BlockPos> growth = inspectOrFill(world, colonyId, blueprint, origin, floor.getY(), owner);

        if (mayClear && !growth.isEmpty()) {
            return clearFirst(world, colonyId, model, floor, quarterTurns, owner, growth, lotArea(blueprint, colonyId, origin));
        }

        MaeBuildQueue.Request request = MaeBuildQueue.add(world, new MaeBuildQueue.Request(UUID.randomUUID(), colonyId, model,
                floor.toImmutable(), MinecraftTypeAdapter.toBlockPos(origin), quarterTurns, owner, false));

        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 colony={} model={} at={} rotation={} result=queued ms={}",
                colonyId.toString().substring(0, 8), model, floor.toShortString(), quarterTurns, (System.nanoTime() - began) / 1_000_000);

        return statusOf(request);
    }

    /**
     * O lote do jogador sobre água interna da vila: a água sai, e o lote é conferido de novo — Regra 227. A obra automática,
     * sem dono, não aterra água (ADR-049).
     */
    private static List<BlockPos> inspectOrFill(ServerWorld world, UUID colonyId, Blueprint blueprint, ColonyPos origin,
                                                int floorY, @Nullable UUID owner) {
        try {
            return inspect(world, colonyId, blueprint, origin, floorY, null);
        } catch (IllegalStateException refused) {
            var box = VillageColonyMod.COLONIES.find(colonyId).flatMap(com.villagecolony.core.colony.model.Colony::bounds);

            if (owner == null || box.isEmpty() || !("occupied".equals(refused.getMessage()) || "no_base".equals(refused.getMessage()))) {
                throw refused;
            }

            Building lot = Building.of(ConstructionProject.plan(colonyId, blueprint, origin));
            List<BlockPos> water = MaeWater.toFill(world, box.get(), lot.min(), new ColonyPos(lot.max().x(), floorY, lot.max().z()),
                    floorY);

            if (water.isEmpty()) {
                throw refused;
            }

            MaeWater.fill(world, water);
            VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 colony={} model={} at={} result=water_filled blocks={}",
                    colonyId.toString().substring(0, 8), blueprint.id(), MinecraftTypeAdapter.toBlockPos(origin).toShortString(),
                    water.size());

            return inspect(world, colonyId, blueprint, origin, floorY, null);
        }
    }

    /**
     * O lote serve? A régua é a do construtor (Regra 213): a mesma caixa da busca de lote, a mesma trava da base, e nada de
     * construção no volume. Devolve o que a limpeza tiraria dele; recusa com o motivo.
     */
    private static List<BlockPos> inspect(ServerWorld world, UUID colonyId, Blueprint blueprint, ColonyPos origin, int floorY,
                                          @Nullable UUID except) {
        WorkArea area = lotArea(blueprint, colonyId, origin);
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint, origin);
        Building lot = Building.of(project);

        for (int x = area.min().x() >> 4; x <= area.max().x() >> 4; x++) {
            for (int z = area.min().z() >> 4; z <= area.max().z() >> 4; z++) {
                if (world.getChunkManager().getWorldChunk(x, z) == null) {
                    throw new IllegalStateException("unknown_chunk");
                }
            }
        }

        for (MaeBuildQueue.Request other : MaeBuildQueue.all(world)) {
            if (!other.id().equals(except) && colonyId.equals(other.colonyId())
                    && area.intersects(lotArea(blueprint(world, other.model(), other.quarterTurns()), colonyId,
                    MinecraftTypeAdapter.toColonyPos(other.origin())))) {
                throw new IllegalStateException("overlapping_request");
            }
        }

        if (LotClearance.overlapsVillageStructure(world, lot.min(), lot.max())) {
            throw new IllegalStateException("original_structure");
        }

        if (VillageColonyMod.BUILDINGS.anythingBuiltInside(lot.min(), lot.max()) || LotClearance.overlapsFoundationFootprint(lot.min(), lot.max())
                || LotClearance.overlapsConstructionSite(world, colonyId, lot.min(), lot.max(), null)) {
            throw new IllegalStateException("occupied");
        }

        List<BlockPos> growth = MaeClearing.survey(world, area);

        if (growth.isEmpty()) {
            // Sem árvore no caminho, o resto do volume é construção de alguém (Regra 3); um degrau de chão a obra nivela.
            for (int x = lot.min().x(); x <= lot.max().x(); x++) {
                for (int z = lot.min().z(); z <= lot.max().z(); z++) {
                    if (!LotClearance.isClearAbove(world, x, z, floorY + 1, lot.max().y() - floorY)) {
                        throw new IllegalStateException("occupied");
                    }
                }
            }

            FoundationPreparation.refusal(world, project).ifPresent(why -> {
                throw new IllegalStateException("no_base");
            });
        }

        return growth;
    }

    /** O lote com a folga do anel de chão: é o que a obra toca. */
    static WorkArea lotArea(Blueprint blueprint, UUID colonyId, ColonyPos origin) {
        Building lot = Building.of(ConstructionProject.plan(colonyId, blueprint, origin));

        return new WorkArea(new ColonyPos(lot.min().x() - LOT_MARGIN, lot.min().y() - LOT_MARGIN, lot.min().z() - LOT_MARGIN),
                new ColonyPos(lot.max().x() + LOT_MARGIN, lot.max().y() + LOT_MARGIN, lot.max().z() + LOT_MARGIN));
    }

    /** Limpa a região agora; a obra entra na fila depois da espera, em {@link #startAfterClearing} — Regra 200. */
    private static Status clearFirst(ServerWorld world, UUID colonyId, ResourceId model, BlockPos floor, int quarterTurns,
                                     @Nullable UUID owner, List<BlockPos> growth, WorkArea area) {
        MaeClearing.clear(world, growth);
        MaeClearing.schedule(new MaeClearing.Pending(world, colonyId, model, floor.toImmutable(),
                quarterTurns, owner, area, MaeClearing.readyAt(world)));
        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 colony={} model={} at={} rotation={} result=clearing blocks={}",
                colonyId.toString().substring(0, 8), model, floor.toShortString(), quarterTurns, growth.size());

        return new Status(UUID.randomUUID(), model, floor.toImmutable(), quarterTurns, Phase.CLEARING, growth.size() + " blocos");
    }

    private static void startAfterClearing(MinecraftServer server, MaeClearing.Pending due) {
        Text outcome;

        try {
            outcome = message(start(due.world(), due.colonyId(), due.model(), due.origin(), due.quarterTurns(), due.owner(), false));
        } catch (IllegalStateException | IllegalArgumentException refused) {
            VillageColonyMod.LOGGER.warn("VC_MAE_SITE version=2 model={} at={} result=refused_after_clearing detail={}",
                    due.model(), due.origin().toShortString(), refused.getMessage());
            outcome = Text.literal("Obra MAE recusada depois da limpeza: " + refused.getMessage()).formatted(Formatting.RED);
        }

        tell(server, due.owner(), outcome);
    }

    /** O dono da obra lê a linha, se estiver no jogo. */
    public static void tell(MinecraftServer server, @Nullable UUID owner, Text line) {
        if (owner != null) {
            var player = server.getPlayerManager().getPlayer(owner);

            if (player != null) {
                player.sendMessage(line, false);
            }
        }
    }

    /**
     * Busca um lote perto do jogador pelo mesmo mapa de alturas da casa fundadora (desnível até
     * {@link FoundationLotSearch#MAX_SPREAD}) e pede a obra no primeiro que serve.
     */
    public static Status startNear(ServerWorld world, UUID colonyId, ResourceId model, BlockPos near,
                                   int quarterTurns, @Nullable UUID owner) {
        return firstThatServes(world, colonyId, model, lotsNear(world, model, near, quarterTurns), quarterTurns, owner, "no_lot");
    }

    /** O primeiro lote que serve; sem nenhum, a recusa traz a conta de cada motivo (Regra 226). */
    private static Status firstThatServes(ServerWorld world, UUID colonyId, ResourceId model, List<BlockPos> lots,
                                          int quarterTurns, @Nullable UUID owner, String nothing) {
        long began = System.nanoTime();
        java.util.Map<String, Integer> reasons = new java.util.LinkedHashMap<>();

        for (BlockPos lot : lots.subList(0, Math.min(LOTS_TRIED, lots.size()))) {
            try {
                return start(world, colonyId, model, lot, quarterTurns, owner);
            } catch (IllegalStateException | IllegalArgumentException refused) {
                if ("too_many_sites".equals(refused.getMessage())) throw refused;
                reasons.merge(refused.getMessage() == null ? "invalid_payload" : refused.getMessage(), 1, Integer::sum);
            }
        }

        String summary = "no_lot:" + (reasons.isEmpty() ? nothing : refusals(reasons));

        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 colony={} model={} result=refused detail={} ms={}",
                colonyId.toString().substring(0, 8), model, summary, (System.nanoTime() - began) / 1_000_000);

        throw new IllegalStateException(summary);
    }

    /** Cada motivo com quantos lotes recusou, o mais comum primeiro: {@code occupied×5,no_base×2}. */
    static String refusals(java.util.Map<String, Integer> reasons) {
        return reasons.entrySet().stream()
                .sorted((a, b) -> a.getValue().equals(b.getValue()) ? a.getKey().compareTo(b.getKey()) : Integer.compare(b.getValue(), a.getValue()))
                .map(entry -> entry.getKey() + "×" + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
    }

    /** O lote sem árvore primeiro; a limpeza só quando não há outro (Regra 200). */
    private static List<BlockPos> lotsNear(ServerWorld world, ResourceId model, BlockPos near, int quarterTurns) {
        var size = blueprint(world, model, quarterTurns).size();
        int width = 2 * SEARCH_REACH + size.x();
        int depth = 2 * SEARCH_REACH + size.z();
        int minX = near.getX() - SEARCH_REACH - size.x() / 2;
        int minZ = near.getZ() - SEARCH_REACH - size.z() / 2;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];

        readGround(world, minX, minZ, width, depth, heights, growth);

        List<FoundationLotSearch.Lot> candidates = new ArrayList<>(
                FoundationLotSearch.candidates(heights, growth, width, depth, size.x(), size.z()));

        candidates.sort(java.util.Comparator.comparing(FoundationLotSearch.Lot::clearing));

        return candidates.stream().limit(LOTS_TRIED)
                .map(lot -> new BlockPos(minX + lot.offsetX(), lot.floor(), minZ + lot.offsetZ())).toList();
    }

    static void readGround(ServerWorld world, int minX, int minZ, int width, int depth, int[] heights, boolean[] growth) {
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int x = minX + dx;
                int z = minZ + dz;
                boolean loaded = world.isChunkLoaded(x >> 4, z >> 4);

                heights[dz * width + dx] = loaded ? LotGrowth.groundTop(world, x, z) : FoundationLotSearch.UNKNOWN;
                growth[dz * width + dx] = loaded && LotGrowth.hasGrowth(world, x, z);
            }
        }
    }

    /**
     * Lote na faixa fora da vila gerada, perto do jogador — plano mestre §3.3: o MAE não edita a vila original. Só lê chunk
     * carregado.
     */
    public static Status startOutside(ServerWorld world, UUID colonyId, ResourceId model, BlockPos near,
                                      int quarterTurns, @Nullable UUID owner) {
        ColonyPos center = VillageColonyMod.COLONIES.find(colonyId)
                .orElseThrow(() -> new IllegalStateException("unknown_colony")).center();
        var box = com.villagecolony.fabric.world.GeneratedVillages.boundsAt(world, center.x(), center.z())
                .orElseThrow(() -> new IllegalStateException("no_generated_village"));
        var size = blueprint(world, model, quarterTurns).size();
        int clear = OUTSIDE_CLEARANCE;
        int minX = box.minX() - clear - OUTSIDE_REACH;
        int minZ = box.minZ() - clear - OUTSIDE_REACH;
        int width = box.maxX() - box.minX() + 2 * (clear + OUTSIDE_REACH) + 1;
        int depth = box.maxZ() - box.minZ() + 2 * (clear + OUTSIDE_REACH) + 1;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];

        readGround(world, minX, minZ, width, depth, heights, growth);

        List<BlockPos> lots = new ArrayList<>();
        java.util.Set<BlockPos> toClear = new java.util.HashSet<>();

        for (var lot : FoundationLotSearch.candidates(heights, growth, width, depth, size.x(), size.z())) {
            int x = minX + lot.offsetX();
            int z = minZ + lot.offsetZ();
            boolean outside = x + size.x() - 1 < box.minX() - clear || x > box.maxX() + clear
                    || z + size.z() - 1 < box.minZ() - clear || z > box.maxZ() + clear;

            if (outside) {
                BlockPos at = new BlockPos(x, lot.floor(), z);

                lots.add(at);

                if (lot.clearing()) {
                    toClear.add(at);
                }
            }
        }

        lots.sort(java.util.Comparator.<BlockPos, Boolean>comparing(toClear::contains)
                .thenComparingDouble(lot -> lot.getSquaredDistance(near)));

        return firstThatServes(world, colonyId, model, lots, quarterTurns, owner, "no_lot_outside_the_village");
    }

    /**
     * O lote mudou desde o pedido e a obra não abre nele: a mesma obra vai para outro lote perto, para sempre terminar — Regra 213.
     *
     * @return se achou outro lote
     */
    public static boolean move(ServerWorld world, MaeBuildQueue.Request request) {
        for (BlockPos lot : lotsNear(world, request.model(), request.floor(), request.quarterTurns())) {
            if (!lot.equals(request.floor())) {
                try {
                    Blueprint blueprint = blueprint(world, request.model(), request.quarterTurns());
                    ColonyPos origin = blueprint.originFor(MinecraftTypeAdapter.toColonyPos(lot));

                    if (inspect(world, request.colonyId(), blueprint, origin, lot.getY(), request.id()).isEmpty()) {
                        MaeBuildQueue.movedTo(world, request.id(), lot, MinecraftTypeAdapter.toBlockPos(origin));
                        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} result=moved to={}",
                                request.model(), request.floor().toShortString(), lot.toShortString());

                        return true;
                    }
                } catch (IllegalStateException | IllegalArgumentException ignored) {
                    // Este lote não serve: a busca segue para o próximo.
                }
            }
        }

        return false;
    }

    /** Origem que deixa o jogador no meio da pegada, na altura dos pés. */
    public static BlockPos centered(ServerWorld world, ResourceId model, int quarterTurns, BlockPos feet) {
        var size = blueprint(world, model, quarterTurns).size();
        return feet.add(-size.x() / 2, 0, -size.z() / 2);
    }

    /** A planta pedida, no giro pedido. */
    public static Blueprint blueprint(ServerWorld world, ResourceId model, int quarterTurns) {
        try {
            return StructureBlueprintReader.read(world, model)
                    .orElseThrow(() -> new IllegalArgumentException("unknown_model")).rotated(quarterTurns);
        } catch (RuntimeException failure) {
            if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) throw failure;
            throw new IllegalArgumentException("unknown_model", failure);
        }
    }

    /** O pedido sai da fila. */
    public static boolean stop(ServerWorld world, UUID id) {
        boolean known = MaeBuildQueue.all(world).stream().anyMatch(request -> request.id().equals(id));

        MaeBuildQueue.remove(world, id);

        return known;
    }

    /** A obra que uma versão anterior deixou numa região vira pedido, e as regiões saem do save — ADR-047. */
    static void retireOldRegions(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        var store = ColonySavedData.get(server).maeWork();

        if (!store.supported() || store.regions().isEmpty()) return;

        List<net.minecraft.nbt.NbtCompound> waiting = new ArrayList<>();

        for (var region : store.regions()) {
            var record = MaeSiteRecord.of(region);

            if (record.isEmpty()) {
                continue;
            }

            boolean alive = region.containsUuid("colony") && VillageColonyMod.COLONIES.find(region.getUuid("colony")).isPresent();

            if (!(alive && requeue(world, region.getUuid("colony"), record.get())) && !clearRuin(world, record.get())) {
                waiting.add(region);
            }
        }

        store.keepRegions(waiting);
    }

    private static boolean requeue(ServerWorld world, UUID colonyId, MaeSiteRecord record) {
        try {
            Blueprint blueprint = blueprint(world, record.model(), record.quarterTurns());

            MaeBuildQueue.add(world, new MaeBuildQueue.Request(UUID.randomUUID(), colonyId, record.model(), record.origin(),
                    record.origin(), record.quarterTurns(), record.owner(), false));
            VillageColonyMod.LOGGER.info("VC_MAE_SITE version=2 model={} at={} result=requeued blocks={}", record.model(),
                    record.origin().toShortString(), blueprint.blockCount());

            return true;
        } catch (IllegalStateException | IllegalArgumentException refused) {
            VillageColonyMod.LOGGER.warn("VC_MAE_SITE version=2 model={} at={} result=not_requeued detail={}", record.model(),
                    record.origin().toShortString(), refused.getMessage());

            return false;
        }
    }

    /** A obra antiga que não volta para a fila sai do mundo; falso enquanto o chunk dela não está carregado (Regra 228). */
    private static boolean clearRuin(ServerWorld world, MaeSiteRecord record) {
        Blueprint blueprint;

        try {
            blueprint = blueprint(world, record.model(), record.quarterTurns());
        } catch (IllegalStateException | IllegalArgumentException unreadable) {
            // Sem a planta não há como saber o que é da obra: a região sai, e o mundo fica como está.
            return true;
        }

        if (!MaeRuins.loaded(world, blueprint, record.origin())) {
            return false;
        }

        MaeRuins.clear(world, blueprint, record.origin());

        return true;
    }

    public static List<Status> active(ServerWorld world) {
        return MaeBuildQueue.all(world).stream().map(MaeSites::statusOf).toList();
    }

    static void tick(MinecraftServer server) {
        MaeClearing.takeDue(server.getOverworld().getTime()).ifPresent(due -> startAfterClearing(server, due));

        if (server.getTicks() % OLD_REGIONS_EVERY == 0) {
            retireOldRegions(server);
        }
    }

    /** A linha que o jogador lê: modelo, posição, etapa e motivo. */
    public static Text message(Status status) {
        String text = "MAE " + status.model() + " em " + status.origin().toShortString()
                + " (rotação " + status.quarterTurns() + "): " + status.phase().name().toLowerCase(java.util.Locale.ROOT)
                + (status.detail().isEmpty() ? "" : " — " + status.detail());

        return Text.literal(text).formatted(status.phase() == Phase.DONE ? Formatting.GREEN : Formatting.YELLOW);
    }

    public static Status statusOf(MaeBuildQueue.Request request) {
        return new Status(request.id(), request.model(), request.floor(), request.quarterTurns(),
                request.opened() ? Phase.BUILDING : Phase.QUEUED, request.opened() ? "com o construtor" : "próxima do construtor");
    }
}
