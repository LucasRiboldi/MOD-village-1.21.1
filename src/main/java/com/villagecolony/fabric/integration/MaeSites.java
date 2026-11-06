package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Obras MAE supervisionadas disparadas por comando: reserva a região, conduz um {@link MaeStructureSite} por
 * tique e avisa quem pediu quando termina ou recusa — ADR-046.
 *
 * <p>Vive só na sessão; após recarga, o que foi aprovado fica nos jobs do writer, que bloqueiam retomada
 * ambígua. Não é a fila automática: cada obra nasce de um pedido explícito.
 */
public final class MaeSites {
    /** Teto de obras supervisionadas simultâneas; o orçamento de escrita já é global por tique. */
    static final int MAX_ACTIVE = 4;
    /** Raio da busca de lote em volta do jogador. */
    static final int SEARCH_REACH = 24;
    private static final int SLICES_PER_TICK = 3;
    /** Folga horizontal da região: entrada, margem do terreno e um trecho de rua até a rede. */
    static final int REGION_REACH = 12;
    /** A folga que a caixa da estrutura soma às peças da vila. */
    private static final int STRUCTURE_MARGIN = 12;
    /** Até onde, além da folga, se procura lote fora da vila. */
    private static final int OUTSIDE_REACH = 32;
    /** De quanto em quanto tempo se procura obra interrompida com o chão já carregado. */
    private static final int RESUME_EVERY = 100;
    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();

    static {
        com.villagecolony.core.type.ServerMemory.register(MaeSites.class, ACTIVE::clear);
    }

    private record Entry(ServerWorld world, MaeWorldWriter writer, ResourceId model, BlockPos origin,
                         int quarterTurns, @Nullable UUID owner, MaeStructureSite site) { }

    /** Uma obra em andamento, para o estado mostrado ao jogador. */
    public record Status(UUID regionId, ResourceId model, BlockPos origin, int quarterTurns,
                         MaeStructureSite.Phase phase, String detail) { }

    private MaeSites() { }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MaeSites::tick);
    }

    /** Obra com a origem pedida; recusa com o motivo da leitura, da reserva ou do levantamento. */
    public static Status start(ServerWorld world, UUID colonyId, ResourceId model, BlockPos origin,
                               int quarterTurns, @Nullable UUID owner) {
        return start(world, MaeWorldWriter.of(world), colonyId, model, origin, quarterTurns, owner);
    }

    static Status start(ServerWorld world, MaeWorldWriter writer, UUID colonyId, ResourceId model, BlockPos origin,
                        int quarterTurns, @Nullable UUID owner) {
        if (MaeClearing.isBusy()) throw new IllegalStateException("too_many_sites");
        return start(world, writer, colonyId, model, origin, quarterTurns, owner, true);
    }

    private static Status start(ServerWorld world, MaeWorldWriter writer, UUID colonyId, ResourceId model, BlockPos origin,
                                int quarterTurns, @Nullable UUID owner, boolean mayClear) {
        if (ACTIVE.size() >= MAX_ACTIVE) throw new IllegalStateException("too_many_sites");
        if (quarterTurns < 0 || quarterTurns > 3) throw new IllegalArgumentException("invalid_rotation");
        ColonyPos center = VillageColonyMod.COLONIES.find(colonyId)
                .orElseThrow(() -> new IllegalStateException("unknown_colony")).center();
        ConstructionProject project = ConstructionProject.plan(colonyId, blueprint(world, model, quarterTurns),
                MinecraftTypeAdapter.toColonyPos(origin));
        java.util.Optional<UUID> reused = reusable(world, writer, colonyId, project);

        if (reused.isEmpty() && mayClear) {
            WorkArea area = regionFor(project);
            List<BlockPos> growth = MaeClearing.survey(world, writer, colonyId, area);

            if (!growth.isEmpty()) {
                return clearFirst(world, writer, colonyId, model, origin, quarterTurns, owner, area, growth);
            }
        }

        UUID regionId = reused.orElseGet(() -> writer.reserveTerrain(world, colonyId, regionFor(project)));
        MaeStructureSite site;

        try {
            // Em vila nova a ligação é do grafo, com o construtor; o road_v2 fica para a vila que não tem releitura.
            site = NewVillage.is(world, colonyId)
                    ? MaeStructureSite.begin(world, writer, regionId, project, quarterTurns, center)
                    : MaeStructureSite.beginWithRoad(world, writer, regionId, project, quarterTurns, center);
        } catch (IllegalStateException | IllegalArgumentException refused) {
            // Reserva nova sem nenhuma escrita não pode ficar bloqueando a dimensão.
            if (reused.isEmpty()) {
                MaeHandover.handOver(writer.store(), regionId);
            }

            throw refused;
        }

        new MaeSiteRecord(model, origin.toImmutable(), quarterTurns, owner).writeTo(writer.store(), regionId);
        ACTIVE.put(regionId, new Entry(world, writer, model, origin.toImmutable(), quarterTurns, owner, site));
        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=1 colony={} model={} at={} rotation={} result=started",
                colonyId.toString().substring(0, 8), model, origin.toShortString(), quarterTurns);
        return status(regionId, ACTIVE.get(regionId));
    }

    /** Limpa a região agora; a obra começa depois da espera, em {@link #startAfterClearing} — Regra 200. */
    private static Status clearFirst(ServerWorld world, MaeWorldWriter writer, UUID colonyId, ResourceId model, BlockPos origin,
                                     int quarterTurns, @Nullable UUID owner, WorkArea area, List<BlockPos> growth) {
        MaeClearing.clear(world, growth);
        MaeGenerationEvidence.rebase(world, area);
        MaeClearing.schedule(new MaeClearing.Pending(world, writer, colonyId, model, origin.toImmutable(), quarterTurns, owner,
                area, MaeClearing.readyAt(world)));
        VillageColonyMod.LOGGER.info("VC_MAE_SITE version=1 colony={} model={} at={} rotation={} result=clearing blocks={}",
                colonyId.toString().substring(0, 8), model, origin.toShortString(), quarterTurns, growth.size());
        return new Status(UUID.randomUUID(), model, origin.toImmutable(), quarterTurns, MaeStructureSite.Phase.CLEARING,
                growth.size() + " blocos");
    }

    private static void startAfterClearing(MinecraftServer server, MaeClearing.Pending due) {
        Text outcome;

        MaeGenerationEvidence.rebase(due.world(), due.area());

        try {
            outcome = message(start(due.world(), due.writer(), due.colonyId(), due.model(), due.origin(), due.quarterTurns(),
                    due.owner(), false));
        } catch (IllegalStateException | IllegalArgumentException refused) {
            VillageColonyMod.LOGGER.warn("VC_MAE_SITE version=1 model={} at={} result=refused_after_clearing detail={}",
                    due.model(), due.origin().toShortString(), refused.getMessage());
            outcome = Text.literal("Obra MAE recusada depois da limpeza: " + refused.getMessage()).formatted(Formatting.RED);
        }

        if (due.owner() != null) {
            var player = server.getPlayerManager().getPlayer(due.owner());
            if (player != null) player.sendMessage(outcome, false);
        }
    }

    /**
     * Busca um lote perto do jogador pelo mesmo mapa de alturas da casa fundadora (desnível até
     * {@link FoundationLotSearch#MAX_SPREAD}) e começa no primeiro cuja reserva é aceita.
     */
    public static Status startNear(ServerWorld world, UUID colonyId, ResourceId model, BlockPos near,
                                   int quarterTurns, @Nullable UUID owner) {
        return startNear(world, MaeWorldWriter.of(world), colonyId, model, near, quarterTurns, owner);
    }

    static Status startNear(ServerWorld world, MaeWorldWriter writer, UUID colonyId, ResourceId model, BlockPos near,
                            int quarterTurns, @Nullable UUID owner) {
        var size = blueprint(world, model, quarterTurns).size();
        int width = 2 * SEARCH_REACH + size.x();
        int depth = 2 * SEARCH_REACH + size.z();
        int minX = near.getX() - SEARCH_REACH - size.x() / 2;
        int minZ = near.getZ() - SEARCH_REACH - size.z() / 2;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];
        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int x = minX + dx;
                int z = minZ + dz;
                boolean loaded = world.isChunkLoaded(x >> 4, z >> 4);
                heights[dz * width + dx] = loaded ? LotGrowth.groundTop(world, x, z) : FoundationLotSearch.UNKNOWN;
                growth[dz * width + dx] = loaded && LotGrowth.hasGrowth(world, x, z);
            }
        }
        String last = "no_lot";
        int tried = 0;
        // O lote sem árvore primeiro; a limpeza só quando não há outro (Regra 200).
        List<FoundationLotSearch.Lot> candidates = new ArrayList<>(
                FoundationLotSearch.candidates(heights, growth, width, depth, size.x(), size.z()));
        candidates.sort(java.util.Comparator.comparing(FoundationLotSearch.Lot::clearing));

        for (var lot : candidates) {
            if (tried >= 32) break;
            tried++;
            try {
                return start(world, writer, colonyId, model,
                        new BlockPos(minX + lot.offsetX(), lot.floor(), minZ + lot.offsetZ()), quarterTurns, owner);
            } catch (IllegalStateException | IllegalArgumentException refused) {
                if ("too_many_sites".equals(refused.getMessage())) throw refused;
                last = refused.getMessage() == null ? "invalid_payload" : refused.getMessage();
            }
        }
        throw new IllegalStateException("no_lot:" + last);
    }

    /**
     * Lote na faixa fora da vila gerada, perto do jogador — plano mestre §3.3: o MAE não edita a vila original, e a
     * caixa dela, com a folga da estrutura e a da região, recusaria qualquer lote de dentro. Só lê chunk carregado.
     */
    public static Status startOutside(ServerWorld world, UUID colonyId, ResourceId model, BlockPos near,
                                      int quarterTurns, @Nullable UUID owner) {
        ColonyPos center = VillageColonyMod.COLONIES.find(colonyId)
                .orElseThrow(() -> new IllegalStateException("unknown_colony")).center();
        var box = com.villagecolony.fabric.world.GeneratedVillages.boundsAt(world, center.x(), center.z())
                .orElseThrow(() -> new IllegalStateException("no_generated_village"));
        MaeWorldWriter writer = MaeWorldWriter.of(world);
        var size = blueprint(world, model, quarterTurns).size();
        int clear = STRUCTURE_MARGIN + REGION_REACH + 1;
        int minX = box.minX() - clear - OUTSIDE_REACH;
        int minZ = box.minZ() - clear - OUTSIDE_REACH;
        int width = box.maxX() - box.minX() + 2 * (clear + OUTSIDE_REACH) + 1;
        int depth = box.maxZ() - box.minZ() + 2 * (clear + OUTSIDE_REACH) + 1;
        int[] heights = new int[width * depth];
        boolean[] growth = new boolean[width * depth];

        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int x = minX + dx;
                int z = minZ + dz;
                boolean loaded = world.isChunkLoaded(x >> 4, z >> 4);

                heights[dz * width + dx] = loaded ? LotGrowth.groundTop(world, x, z) : FoundationLotSearch.UNKNOWN;
                growth[dz * width + dx] = loaded && LotGrowth.hasGrowth(world, x, z);
            }
        }

        List<BlockPos> lots = new java.util.ArrayList<>();
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

        // O lote sem árvore primeiro; a limpeza só quando não há outro (Regra 200).
        lots.sort(java.util.Comparator.<BlockPos, Boolean>comparing(toClear::contains)
                .thenComparingDouble(lot -> lot.getSquaredDistance(near)));

        String last = lots.isEmpty() ? "no_lot_outside_the_village" : "no_lot";

        for (BlockPos lot : lots.subList(0, Math.min(32, lots.size()))) {
            try {
                return start(world, writer, colonyId, model, lot, quarterTurns, owner);
            } catch (IllegalStateException | IllegalArgumentException refused) {
                if ("too_many_sites".equals(refused.getMessage())) throw refused;
                last = refused.getMessage() == null ? "invalid_payload" : refused.getMessage();
            }
        }

        throw new IllegalStateException("no_lot:" + last);
    }

    /** Origem que deixa o jogador no meio da pegada, na altura dos pés. */
    public static BlockPos centered(ServerWorld world, ResourceId model, int quarterTurns, BlockPos feet) {
        var size = blueprint(world, model, quarterTurns).size();
        return feet.add(-size.x() / 2, 0, -size.z() / 2);
    }

    /**
     * Região já reservada da mesma colônia, ociosa e que comporta a obra com folga de terreno: as ruas MAE só
     * se ligam dentro de uma região, então a obra seguinte entra nela em vez de reservar outra ao lado.
     */
    static java.util.Optional<UUID> reusable(ServerWorld world, MaeWorldWriter writer, UUID colonyId,
                                             ConstructionProject project) {
        Building lot = Building.of(project);
        WorkArea needed = new WorkArea(new ColonyPos(lot.min().x() - 5, lot.min().y() - 7, lot.min().z() - 5),
                new ColonyPos(lot.max().x() + 5, lot.max().y() + 5, lot.max().z() + 5));
        String dimension = world.getRegistryKey().getValue().toString();
        for (var region : writer.store().regions()) {
            if (!region.containsUuid("id") || ACTIVE.containsKey(region.getUuid("id"))
                    || !region.containsUuid("colony") || !colonyId.equals(region.getUuid("colony"))
                    || !dimension.equals(region.getString("dimension")) || MaeHandover.isHandedOver(region)
                    || !"terrain_v2".equals(region.getString("policy"))) continue;
            var job = region.getCompound("job");
            boolean idle = !region.contains("job") || ("complete".equals(job.getString("state"))
                    && !job.getBoolean("pendingWrite") && !job.getBoolean("pendingPublication"));
            if (idle && MaeWorldWriter.area(region).contains(needed)) return java.util.Optional.of(region.getUuid("id"));
        }
        return java.util.Optional.empty();
    }

    /** A região reservada cobre pegada, entrada, apoio, margem do terreno e a ligação à rua. */
    static WorkArea regionFor(ConstructionProject project) {
        Building lot = Building.of(project);
        return new WorkArea(new ColonyPos(lot.min().x() - REGION_REACH, lot.min().y() - 7, lot.min().z() - REGION_REACH),
                new ColonyPos(lot.max().x() + REGION_REACH, lot.max().y() + 5, lot.max().z() + REGION_REACH));
    }

    private static Blueprint blueprint(ServerWorld world, ResourceId model, int quarterTurns) {
        try {
            return StructureBlueprintReader.readForMae(world, model)
                    .orElseThrow(() -> new IllegalArgumentException("unknown_model")).rotated(quarterTurns);
        } catch (RuntimeException failure) {
            if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) throw failure;
            throw new IllegalArgumentException("unknown_model", failure);
        }
    }

    /** Para a obra: pausa o job aprovado no writer e deixa de conduzi-la; nada é desfeito. */
    public static boolean stop(UUID regionId) {
        Entry entry = ACTIVE.remove(regionId);
        if (entry == null) return false;
        try {
            entry.writer().pause(entry.world(), regionId);
        } catch (IllegalStateException | IllegalArgumentException ignored) {
            // Sem job aprovado não há o que pausar; a reserva continua, sem escrita.
        }
        // Parada a pedido não volta sozinha ao reiniciar.
        MaeSiteRecord.clear(entry.writer().store(), regionId);
        return true;
    }

    /**
     * Entrega as regiões paradas desta dimensão ao pedido do autor; a de escrita sem confirmação fica (ADR-044).
     *
     * @return quantas foram entregues e quantas ficaram, nessa ordem
     */
    public static int[] releaseIdle(ServerWorld world) {
        MaeWorldWriter writer = MaeWorldWriter.of(world);
        String dimension = world.getRegistryKey().getValue().toString();
        int released = 0;
        int kept = 0;

        for (var region : writer.store().regions()) {
            if (!region.containsUuid("id") || ACTIVE.containsKey(region.getUuid("id"))
                    || !dimension.equals(region.getString("dimension")) || MaeHandover.isHandedOver(region)) continue;

            if (MaeHandover.hasUnconfirmedWork(region)) {
                kept++;
            } else {
                MaeHandover.handOver(writer.store(), region.getUuid("id"));
                released++;
            }
        }

        return new int[] {released, kept};
    }

    /** Refaz a obra que o reinício interrompeu, assim que o chão dela estiver carregado (Regra 187). */
    private static void resumeInterrupted(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        MaeWorldWriter writer = MaeWorldWriter.of(world);
        String dimension = world.getRegistryKey().getValue().toString();

        if (!writer.store().supported()) return;

        for (var region : writer.store().regions()) {
            if (ACTIVE.size() >= MAX_ACTIVE) return;
            if (!region.containsUuid("id") || !region.containsUuid("colony") || ACTIVE.containsKey(region.getUuid("id"))
                    || !dimension.equals(region.getString("dimension")) || MaeHandover.isHandedOver(region)) continue;

            var record = MaeSiteRecord.of(region);

            if (record.isPresent() && groundIsLoaded(world, region)) {
                resume(world, writer, region.getUuid("id"), region.getUuid("colony"), record.get());
            }
        }
    }

    private static boolean groundIsLoaded(ServerWorld world, net.minecraft.nbt.NbtCompound region) {
        try {
            WorkArea area = MaeWorldWriter.area(region);

            return world.isChunkLoaded(area.min().x() >> 4, area.min().z() >> 4)
                    && world.isChunkLoaded(area.max().x() >> 4, area.max().z() >> 4);
        } catch (IllegalStateException | IllegalArgumentException malformed) {
            return false;
        }
    }

    private static void resume(ServerWorld world, MaeWorldWriter writer, UUID regionId, UUID colonyId, MaeSiteRecord record) {
        var colony = VillageColonyMod.COLONIES.find(colonyId);

        if (colony.isEmpty()) {
            return;
        }

        try {
            ConstructionProject project = ConstructionProject.plan(colonyId, blueprint(world, record.model(), record.quarterTurns()),
                    MinecraftTypeAdapter.toColonyPos(record.origin()));
            MaeStructureSite site = MaeStructureSite.resume(world, writer, regionId, project, record.quarterTurns(),
                    colony.get().center(), !NewVillage.is(world, colonyId));

            ACTIVE.put(regionId, new Entry(world, writer, record.model(), record.origin(), record.quarterTurns(), record.owner(), site));
            VillageColonyMod.LOGGER.info("VC_MAE_SITE version=1 colony={} model={} at={} rotation={} result=resumed phase={}",
                    colonyId.toString().substring(0, 8), record.model(), record.origin().toShortString(), record.quarterTurns(),
                    site.phase());
        } catch (IllegalStateException | IllegalArgumentException refused) {
            MaeSiteRecord.clear(writer.store(), regionId);
            VillageColonyMod.LOGGER.warn("VC_MAE_SITE version=1 colony={} at={} result=not_resumed detail={}",
                    colonyId.toString().substring(0, 8), record.origin().toShortString(), refused.getMessage());
        }
    }

    public static List<Status> active() {
        var result = new ArrayList<Status>();
        ACTIVE.forEach((id, entry) -> result.add(status(id, entry)));
        return result;
    }

    static void tick(MinecraftServer server) {
        MaeClearing.takeDue(server.getOverworld().getTime()).ifPresent(due -> startAfterClearing(server, due));

        if (server.getTicks() % RESUME_EVERY == 0) {
            resumeInterrupted(server);
        }

        Iterator<Map.Entry<UUID, Entry>> each = ACTIVE.entrySet().iterator();
        while (each.hasNext()) {
            var item = each.next();
            Entry entry = item.getValue();
            for (int i = 0; i < SLICES_PER_TICK; i++) entry.site().runSlice();
            var phase = entry.site().phase();
            if (phase != MaeStructureSite.Phase.DONE && phase != MaeStructureSite.Phase.FAILED) continue;
            each.remove();
            boolean handedOver = close(item.getKey(), entry, phase);
            int applied = entry.writer().store().region(item.getKey()).map(MaeHandover::appliedChanges).orElse(0);
            Status status = closedStatus(item.getKey(), entry, handedOver);
            VillageColonyMod.LOGGER.info("VC_MAE_SITE version=1 model={} at={} rotation={} result={} detail={} applied={} handedOver={}",
                    entry.model(), entry.origin().toShortString(), entry.quarterTurns(),
                    phase.name().toLowerCase(java.util.Locale.ROOT), status.detail(), applied, handedOver);
            if (entry.owner() != null) {
                var player = server.getPlayerManager().getPlayer(entry.owner());
                if (player != null) player.sendMessage(message(status), false);
            }
        }
    }

    /**
     * Fim da obra: pronta em vila nova, ou falha sem escrita por confirmar, a região volta para a vila, e o que já foi
     * escrito fica como está (Regra 205); o resto fica reservado.
     *
     * @return se a região foi entregue
     */
    private static boolean close(UUID regionId, Entry entry, MaeStructureSite.Phase phase) {
        var store = entry.writer().store();
        var region = store.region(regionId);
        boolean newVillage = region.filter(found -> found.containsUuid("colony"))
                .map(found -> NewVillage.is(entry.world(), found.getUuid("colony"))).orElse(false);
        boolean handOver = phase == MaeStructureSite.Phase.DONE ? newVillage
                : region.filter(found -> !MaeHandover.hasUnconfirmedWork(found)).isPresent();

        if (handOver) {
            MaeHandover.handOver(store, regionId);
        } else {
            MaeSiteRecord.clear(store, regionId);
        }

        return handOver;
    }

    /** Sem nota de rua, a obra entregue diz quem liga a porta. */
    private static Status closedStatus(UUID regionId, Entry entry, boolean handedOver) {
        Status status = status(regionId, entry);

        return status.phase() == MaeStructureSite.Phase.DONE && handedOver && status.detail().isEmpty()
                ? new Status(status.regionId(), status.model(), status.origin(), status.quarterTurns(), status.phase(), "road_by_graph")
                : status;
    }

    /** A linha que o jogador lê: modelo, posição, etapa e motivo. */
    public static Text message(Status status) {
        boolean failed = status.phase() == MaeStructureSite.Phase.FAILED;
        String text = "MAE " + status.model() + " em " + status.origin().toShortString()
                + " (rotação " + status.quarterTurns() + "): " + status.phase().name().toLowerCase(java.util.Locale.ROOT)
                + (status.detail().isEmpty() ? "" : " — " + status.detail());
        return Text.literal(text).formatted(failed ? Formatting.RED
                : status.phase() == MaeStructureSite.Phase.DONE ? Formatting.GREEN : Formatting.YELLOW);
    }

    private static Status status(UUID regionId, Entry entry) {
        var site = entry.site();
        String detail = site.failure() != null ? site.failure() : !site.waiting().isEmpty() ? site.waiting() : site.note();
        return new Status(regionId, entry.model(), entry.origin(), entry.quarterTurns(), site.phase(), detail);
    }
}
