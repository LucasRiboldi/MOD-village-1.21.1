package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.BlueprintAccess;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Fluxo supervisionado de uma obra MAE, na ordem do item 7 da ADR-046: levantamento do terreno pela saída
 * escolhida, prova do acesso com o terreno previsto, preparação, escrita da obra e prova real do acesso.
 *
 * <p>Vive só na sessão. O que sobrevive a uma recarga está nos jobs do writer, que bloqueiam sozinhos qualquer retomada
 * ambígua, e no pedido guardado na região ({@link MaeSiteRecord}), de onde {@link #resume} refaz o condutor na etapa do
 * job (Regra 187). Não é chamado pela fila automática — ADR-046, Consequências.
 */
public final class MaeStructureSite {
    /** Etapa atual; {@link #FAILED} guarda o motivo em {@link #failure()}. */
    public enum Phase { CLEARING, TERRAIN, ACCESS, TERRAIN_WRITE, STRUCTURE, ROAD, DONE, FAILED }

    private static final List<BlueprintAccess.Kind> KIND_ORDER = List.of(
            BlueprintAccess.Kind.DOOR, BlueprintAccess.Kind.GATE, BlueprintAccess.Kind.OPENING);

    private final ServerWorld world;
    private final MaeWorldWriter writer;
    private final UUID regionId;
    private final ConstructionProject project;
    private final int quarterTurns;
    private final ColonyPos entry;
    private final List<ColonyPos> surfaces;
    private final ColonyPos center;
    private MaeTerrainPreparation.@Nullable Survey terrain;
    /** A preparação de terreno veio de antes da recarga: ao fim dela, o levantamento recomeça. */
    private boolean resumedTerrain;
    private Phase phase = Phase.TERRAIN;
    private @Nullable String failure;
    private MaeStructureAccess.@Nullable Survey access;
    private MaeTerrainPreparation.@Nullable Prepared prepared;
    private MaeStructureAccess.@Nullable Selected selected;
    private boolean approved;
    private final boolean withRoad;
    private String note = "";

    private MaeStructureSite(ServerWorld world, MaeWorldWriter writer, UUID regionId, ConstructionProject project,
                             int quarterTurns, ColonyPos center, boolean withRoad) {
        this.center = center;
        this.withRoad = withRoad;
        this.world = world;
        this.writer = writer;
        this.regionId = regionId;
        this.project = project;
        this.quarterTurns = quarterTurns;
        this.entry = entry(project);
        this.surfaces = surfaces(project);
    }

    private void survey() {
        terrain = MaeTerrainPreparation.begin(world, writer, regionId, List.of(
                new MaeTerrainPreparation.Proposal(Building.of(project), surfaces, entry, null, 0)), center);
        phase = Phase.TERRAIN;
    }

    /**
     * Refaz o condutor na etapa do job depois de reiniciar: escrita sem confirmação fica parada e vira falha; pausado e
     * bloqueado passam pela retomada do writer, que revalida o mundo (ADR-044).
     */
    public static MaeStructureSite resume(ServerWorld world, MaeWorldWriter writer, UUID regionId, ConstructionProject project,
                                          int quarterTurns, ColonyPos center, boolean withRoad) {
        MaeStructureSite site = new MaeStructureSite(world, writer, regionId, project, quarterTurns, center, withRoad);

        try {
            site.continueFromJob();
        } catch (IllegalStateException | IllegalArgumentException refused) {
            site.fail("resume:" + (refused.getMessage() == null ? "invalid_payload" : refused.getMessage()));
        }

        return site;
    }

    private void continueFromJob() {
        NbtCompound job = job();
        String type = job.getString("type");
        String state = job.getString("state");

        if (job.getBoolean("pendingWrite") || job.getBoolean("pendingPublication")) {
            fail("resume:unconfirmed_write");
            return;
        }

        if (type.isEmpty() || ("complete".equals(state) && MaeTerrainExecution.TYPE.equals(type))) {
            survey();
            return;
        }

        if (List.of("paused", "blocked").contains(state)) {
            writer.resume(world, regionId);
            state = job().getString("state");
        }

        if (MaeStructureExecution.TYPE.equals(type)) {
            approved = true;
            phase = "complete".equals(state) ? (withRoad ? road() : Phase.DONE) : Phase.STRUCTURE;
        } else if (MaeTerrainExecution.TYPE.equals(type)) {
            resumedTerrain = true;
            phase = Phase.TERRAIN_WRITE;
        } else if (MaeRoadExecution.TYPE.equals(type)) {
            phase = "complete".equals(state) ? Phase.DONE : Phase.ROAD;
        } else {
            fail("resume:unknown_job");
        }
    }

    /** A região já reservada com {@code terrain_v2}; o projeto já orientado na rotação informada. */
    public static MaeStructureSite begin(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                                         ConstructionProject project, int quarterTurns, ColonyPos center) {
        MaeStructureSite site = new MaeStructureSite(world, writer, regionId, project, quarterTurns, center, false);

        site.survey();

        return site;
    }

    /** Igual, e ao concluir liga a entrada à rua certificada pelo {@code road_v2} — ADR-046. */
    public static MaeStructureSite beginWithRoad(ServerWorld world, MaeWorldWriter writer, UUID regionId,
                                                 ConstructionProject project, int quarterTurns, ColonyPos center) {
        MaeStructureSite site = new MaeStructureSite(world, writer, regionId, project, quarterTurns, center, true);

        site.survey();

        return site;
    }

    /**
     * O piso do lado de fora do vão escolhido: primeiro os que ficam até um bloco acima do chão da pegada (o
     * terreno só liga superfícies com degrau de um), depois o que sai da pegada em menos passos; empate vai para
     * porta, depois portão, depois vão aberto, depois a ordem do levantamento. A prova de acesso confirma.
     */
    static ColonyPos entry(ConstructionProject project) {
        Building lot = Building.of(project);
        BlockPos origin = MinecraftTypeAdapter.toBlockPos(project.origin());
        BlueprintAccess best = null;
        BlockPos bestFloor = null;
        int bestSteps = Integer.MAX_VALUE;
        int ground = lot.min().y() - 1;
        var ordered = MaeStructureAccess.sorted(project.blueprint().accesses()).stream()
                .sorted(Comparator.comparingInt((BlueprintAccess declared) -> KIND_ORDER.indexOf(declared.kind()))).toList();
        for (BlueprintAccess declared : ordered) {
            var first = declared.opening().getFirst();
            BlockPos at = origin.add(first.x(), first.y(), first.z());
            var direction = MinecraftTypeAdapter.toDirection(declared.outward());
            for (int step = 1; step <= 6; step++) {
                at = at.offset(direction);
                if (!lot.contains(MinecraftTypeAdapter.toColonyPos(at))) {
                    // Degrau de até um sobre o chão conta antes; vão mais alto só entra sem outro.
                    int rank = step + (at.down().getY() - ground > 1 ? 1000 : 0);
                    if (rank < bestSteps) {
                        best = declared;
                        bestSteps = rank;
                        bestFloor = at.down();
                    }
                    break;
                }
            }
        }
        if (best == null) throw new IllegalArgumentException("no_exterior_entry");
        return MinecraftTypeAdapter.toColonyPos(bestFloor);
    }

    /** A pegada inteira logo abaixo da origem e o piso da entrada; o levantamento decide se dá para prepará-las. */
    static List<ColonyPos> surfaces(ConstructionProject project) {
        Building lot = Building.of(project);
        var result = new ArrayList<ColonyPos>();
        int floor = lot.min().y() - 1;
        for (int x = lot.min().x(); x <= lot.max().x(); x++) {
            for (int z = lot.min().z(); z <= lot.max().z(); z++) result.add(new ColonyPos(x, floor, z));
        }
        result.add(entry(project));
        return List.copyOf(result);
    }

    /** Uma fatia da etapa atual; o orçamento de cada etapa é o do levantamento ou do writer que ela usa. */
    public void runSlice() {
        try {
            switch (phase) {
                case CLEARING -> { }
                case TERRAIN -> {
                    var survey = java.util.Objects.requireNonNull(terrain, "survey");
                    survey.runSlice();
                    if (!survey.complete()) return;
                    prepared = survey.result();
                    access = MaeStructureAccess.beginFor(world, project, prepared.changes(), surfaces, false);
                    phase = Phase.ACCESS;
                }
                case ACCESS -> {
                    access.runSlice();
                    if (!access.complete()) return;
                    selected = access.result();
                    if (!MinecraftTypeAdapter.toColonyPos(selected.support()).equals(entry)) {
                        fail("access:entry_mismatch");
                    } else if (prepared.changes().isEmpty()) {
                        phase = Phase.STRUCTURE;
                    } else {
                        writer.approveTerrain(world, prepared);
                        phase = Phase.TERRAIN_WRITE;
                    }
                }
                case TERRAIN_WRITE -> {
                    writer.runSlice(world, regionId);
                    String state = job().getString("state");
                    if ("blocked".equals(state)) fail("terrain:" + job().getString("reason"));
                    else if ("complete".equals(state) && resumedTerrain) {
                        resumedTerrain = false;
                        survey();
                    } else if ("complete".equals(state)) phase = Phase.STRUCTURE;
                }
                case STRUCTURE -> {
                    if (!approved) {
                        writer.approveStructure(world, regionId, project, quarterTurns, selected,
                                prepared.changes().isEmpty() ? prepared : null);
                        approved = true;
                        return;
                    }
                    writer.runSlice(world, regionId);
                    String state = job().getString("state");
                    if ("blocked".equals(state)) fail("structure:" + job().getString("reason"));
                    else if ("complete".equals(state)) phase = withRoad ? road() : Phase.DONE;
                }
                case ROAD -> {
                    writer.runSlice(world, regionId);
                    String state = job().getString("state");
                    // A obra já está concluída e registrada; rua que não fecha vira nota, não falha da obra.
                    if ("blocked".equals(state)) finish("road_blocked:" + job().getString("reason"));
                    else if ("complete".equals(state)) finish("road_linked");
                }
                case DONE, FAILED -> { }
            }
        } catch (IllegalStateException | IllegalArgumentException refused) {
            fail(phase.name().toLowerCase(Locale.ROOT) + ":"
                    + (refused.getMessage() == null ? "invalid_payload" : refused.getMessage()));
        }
    }

    /** Planeja e aprova a ligação; sem rua certificada ao alcance, a obra termina sem ela. */
    private Phase road() {
        var region = writer.store().region(regionId).orElseThrow();
        var plan = MaeStructureRoad.plan(world, region.getUuid("colony"), MaeWorldWriter.area(region),
                Building.of(project), MinecraftTypeAdapter.toBlockPos(entry));
        if (plan.isEmpty()) {
            note = "no_certified_road";
            return Phase.DONE;
        }
        try {
            writer.approveRoad(world, MaeRoadPreparation.prepare(world, writer, regionId,
                    plan.get().route(), plan.get().hub(), plan.get().connection()));
        } catch (IllegalStateException | IllegalArgumentException refused) {
            note = "road_refused:" + (refused.getMessage() == null ? "invalid_payload" : refused.getMessage());
            return Phase.DONE;
        }
        return Phase.ROAD;
    }

    private void finish(String reason) {
        note = reason;
        phase = Phase.DONE;
    }

    /** O que aconteceu com a ligação à rua, ou vazio sem ela. */
    public String note() { return note; }

    private NbtCompound job() {
        return writer.store().region(regionId).orElseThrow().getCompound("job");
    }

    private void fail(String reason) {
        failure = reason;
        phase = Phase.FAILED;
    }

    public Phase phase() { return phase; }
    public @Nullable String failure() { return failure; }

    /** O motivo da espera por material da obra aprovada, ou vazio fora dela. */
    public String waiting() {
        return phase == Phase.STRUCTURE && approved && "waiting".equals(job().getString("state"))
                ? job().getString("reason") : "";
    }
}
