package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.WorkArea;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.data.save.MaeWorkStore;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.villagecolony.VillageColonyMod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import org.jspecify.annotations.Nullable;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Escrita supervisionada de edifícios pelo contrato {@code structure_v3}, contrato 1 — ADR-046.
 *
 * <p>Material exato dos baús (P2), cursor em ordem estrita (P3) e retirada antes da escrita (P4).
 * Um passo interrompido nunca se repete: a retomada classifica e bloqueia.
 */
final class MaeStructureExecution {
    static final String TYPE = "structure_v3";
    private static final int RETRY_TICKS = 20;
    /** Conferência de acesso em andamento; vive só na sessão e recomeça após recarga — ADR-046. */
    private static final Map<ServerWorld, Map<UUID, Check>> CHECKS = new WeakHashMap<>();

    static {
        com.villagecolony.core.type.ServerMemory.register(MaeStructureExecution.class, CHECKS::clear);
    }

    private record Check(ConstructionProject project, MaeStructureAccess.Survey survey) { }

    /** Origem do template e dos baús; o jogo usa a leitura MAE e os baús da colônia. */
    interface Sources {
        Optional<Blueprint> template(ServerWorld world, ResourceId id);

        List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from);

        Sources GAME = new Sources() {
            @Override public Optional<Blueprint> template(ServerWorld world, ResourceId id) {
                return StructureBlueprintReader.readForMae(world, id);
            }

            @Override public List<ColonyPos> chests(ServerWorld world, UUID colonyId, ColonyPos from) {
                return ColonyChests.nearestFirst(world, colonyId, from);
            }
        };
    }

    private MaeStructureExecution() { }

    static boolean is(NbtCompound job) {
        return TYPE.equals(job.getString("type"));
    }

    /**
     * O terreno da mesma obra precisa estar pronto: um job {@code terrain_v2} concluído ou, no terreno já plano,
     * um levantamento viável sem mudança cuja evidência ainda bate com o mundo — ADR-046, escolha do autor de
     * 2026-11-04. A evidência atual vira o ponto de partida.
     */
    static NbtCompound job(ServerWorld world, NbtCompound region, ConstructionProject project, int quarterTurns,
                           MaeStructureAccess.Selected access, MaeTerrainPreparation.@Nullable Prepared flat,
                           Sources sources) {
        if (!MaeTerrainExecution.TYPE.equals(region.getString("policy"))) {
            throw new IllegalStateException("terrain_policy_required");
        }
        WorkArea box = box(project);
        NbtCompound terrain = region.getCompound("job");
        NbtCompound link = new NbtCompound();
        if (flat != null) {
            if (!flat.changes().isEmpty() || !flat.terrain().feasible() || !flat.regionId().equals(region.getUuid("id"))
                    || !project.blueprint().id().equals(flat.lot().blueprint())
                    || !sameFootprint(new WorkArea(flat.lot().min(), flat.lot().max()), box)
                    || (region.contains("job") && !"complete".equals(terrain.getString("state")))
                    || !flat.evidence().equals(MaeGenerationEvidence.inspect(world,
                            MaeProtection.margin(flat.work()), false, 8))) {
                throw new IllegalStateException("terrain_job_required");
            }
            link.putBoolean("flat", true);
            link.put("sections", flat.evidence().copy());
        } else {
            if (!MaeTerrainExecution.TYPE.equals(terrain.getString("type")) || !"complete".equals(terrain.getString("state"))
                    || terrain.getBoolean("pendingWrite") || !terrain.containsUuid("id")
                    || !project.blueprint().id().toString().equals(terrain.getString("blueprint"))
                    || !sameFootprint(MaeWorldWriter.area(wrap(terrain.getCompound("footprint"))), box)) {
                throw new IllegalStateException("terrain_job_required");
            }
            link.putUuid("id", terrain.getUuid("id"));
            link.put("sections", terrain.getCompound("expectedSections").copy());
        }
        if (!region.getUuid("colony").equals(project.colonyId()) || !access.projectId().equals(project.id())) {
            throw new IllegalStateException("wrong_project_or_colony");
        }
        if (quarterTurns < 0 || quarterTurns > 3) throw new IllegalArgumentException("invalid_rotation");
        if (box.volume() > 4096 || !MaeWorldWriter.area(region).contains(MaeProtection.margin(box))) {
            throw new IllegalStateException("outside_work_area");
        }
        String hash = hash(project);
        if (!hash.equals(templateHash(world, project, quarterTurns, sources))) {
            throw new IllegalStateException("template_changed");
        }
        MaeProtection.inspectStructure(world, region.getUuid("colony"), MaeProtection.margin(box), Set.of());
        NbtCompound actual = MaeGenerationEvidence.inspect(world, MaeProtection.margin(box), false, 8);
        MaeWorldWriter.validateEvidence(world, MaeProtection.margin(box), actual, region.getCompound("ownedSections"));
        NbtList plan = new NbtList();
        for (var piece : MaeStructurePieces.ordered(project)) {
            NbtCompound step = step(world, piece);
            if (step != null) plan.add(step);
        }
        if (plan.isEmpty()) throw new IllegalArgumentException("empty_structure_plan");
        if (plan.size() + region.getList("history", NbtElement.COMPOUND_TYPE).size() > 1024) {
            throw new IllegalStateException("history_budget_exceeded");
        }
        NbtCompound chosen = new NbtCompound();
        chosen.putLong("opening", access.opening().asLong());
        chosen.putLong("support", access.support().asLong());
        NbtList gates = new NbtList();
        access.gates().stream().map(BlockPos::asLong).sorted().forEach(gate -> gates.add(NbtLong.of(gate)));
        chosen.put("gates", gates);
        chosen.putLong("goal", MinecraftTypeAdapter.toBlockPos(access.goal()).asLong());
        NbtList circulation = new NbtList();
        access.circulation().forEach(at -> circulation.add(NbtLong.of(MinecraftTypeAdapter.toBlockPos(at).asLong())));
        if (circulation.isEmpty()) throw new IllegalArgumentException("invalid_access_goal");
        chosen.put("circulation", circulation);
        NbtCompound job = new NbtCompound();
        job.putString("type", TYPE);
        job.putInt("contract", 1);
        job.putUuid("id", project.id());
        job.putString("blueprint", project.blueprint().id().toString());
        job.putString("template", hash);
        job.putInt("rotation", quarterTurns);
        job.putLong("origin", MinecraftTypeAdapter.toBlockPos(project.origin()).asLong());
        job.put("area", MaeWorldWriter.encode(box));
        job.put("terrain", link);
        job.put("access", chosen);
        job.put("plan", plan);
        job.putInt("cursor", 0);
        job.put("expectedSections", actual);
        job.putString("state", "approved");
        return job;
    }

    /** Contrato 1 só constrói em ar; peça já presente sai do plano, peça sem origem física recusa o modelo. */
    private static NbtCompound step(ServerWorld world, MaeStructurePieces.Piece piece) {
        if (!supported(piece)) throw new IllegalArgumentException("unsupported_structure_piece");
        NbtList parts = new NbtList();
        boolean present = true;
        for (int i = 0; i < piece.parts().size(); i++) {
            var entry = piece.parts().get(i);
            BlockState before = world.getBlockState(entry.getKey());
            BlockState after = entry.getValue();
            present &= before.equals(after);
            if (!before.isAir() && !before.equals(after)) throw new IllegalStateException("occupied_structure_target");
            NbtCompound part = new NbtCompound();
            part.putLong("position", entry.getKey().asLong());
            part.put("before", NbtHelper.fromBlockState(before));
            part.put("after", NbtHelper.fromBlockState(after));
            part.putString("role", i == 0 ? "main" : role(after));
            parts.add(part);
        }
        if (present) return null;
        Item item = piece.main().getBlock().asItem();
        NbtCompound material = new NbtCompound();
        material.putString("item", Registries.ITEM.getId(item).toString());
        material.putInt("count", piece.main().contains(Properties.SLAB_TYPE)
                && piece.main().get(Properties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1);
        NbtCompound step = new NbtCompound();
        step.put("parts", parts);
        step.put("material", material);
        return step;
    }

    /** Contrato 1: sem fluido, cultivo nem solo arado, e a peça principal tem item próprio — ADR-046 P2. */
    static boolean supported(MaeStructurePieces.Piece piece) {
        for (var part : piece.parts()) {
            BlockState after = part.getValue();
            if (!after.getFluidState().isEmpty() || after.getBlock() instanceof CropBlock
                    || after.getBlock() instanceof FarmlandBlock) return false;
        }
        return piece.main().isAir() || piece.main().getBlock().asItem() != Items.AIR;
    }

    private static String role(BlockState after) {
        return after.contains(Properties.BED_PART) ? "bed_head" : "upper_half";
    }

    static void validate(ServerWorld world, NbtCompound region, NbtCompound job) {
        if (!is(job) || job.getInt("contract") != 1 || !MaeTerrainExecution.TYPE.equals(region.getString("policy"))
                || job.getString("template").length() != 64 || !job.contains("rotation", NbtElement.INT_TYPE)
                || !job.contains("origin", NbtElement.LONG_TYPE) || !job.contains("access", NbtElement.COMPOUND_TYPE)
                || !job.contains("terrain", NbtElement.COMPOUND_TYPE)) {
            throw new IllegalStateException("unknown_structure_contract");
        }
        MaeProtection.inspectStructure(world, region.getUuid("colony"), MaeProtection.margin(MaeWorldWriter.area(job)),
                written(job));
    }

    /** Intenção, retirada, escrita e confirmação; falta de material espera sem intenção — ADR-046. */
    static boolean writeNext(ServerWorld world, NbtCompound region, NbtCompound job, MaeWorkStore store, Sources sources) {
        NbtCompound step = job.getList("plan", NbtElement.COMPOUND_TYPE).getCompound(job.getInt("cursor"));
        NbtList parts = step.getList("parts", NbtElement.COMPOUND_TYPE);
        WorkArea area = MaeWorldWriter.area(job);
        if (parts.isEmpty() || parts.size() > 2) throw new IllegalStateException("invalid_structure_step");
        for (int i = 0; i < parts.size(); i++) {
            NbtCompound part = parts.getCompound(i);
            BlockPos at = BlockPos.fromLong(part.getLong("position"));
            if (!area.contains(MinecraftTypeAdapter.toColonyPos(at))
                    || !world.getBlockState(at).equals(MaeWorldWriter.decodeState(part.getCompound("before")))) {
                throw new IllegalStateException("invalid_or_changed_target");
            }
        }
        NbtCompound material = step.getCompound("material");
        Identifier itemId = Identifier.tryParse(material.getString("item"));
        int count = material.getInt("count");
        if (itemId == null || !Registries.ITEM.containsId(itemId) || count < 1 || count > 2) {
            throw new IllegalStateException("invalid_material_contract");
        }
        Item item = Registries.ITEM.get(itemId);
        UUID colony = region.getUuid("colony");
        List<ColonyPos> chests = sources.chests(world, colony, MinecraftTypeAdapter.toColonyPos(
                BlockPos.fromLong(job.getLong("origin"))));
        if (ColonyChests.countIn(world, chests, item) < count) {
            job.putString("state", "waiting");
            job.putString("reason", "material:" + itemId);
            job.putLong("retryAt", world.getTime() + RETRY_TICKS);
            region.put("job", job);
            store.update(region);
            return false;
        }
        NbtCompound record = step.copy();
        record.putUuid("operation", job.getUuid("id"));
        record.putLong("tick", world.getTime());
        record.putString("result", "pending");
        record.putString("materials", "reserved");
        NbtList chestList = new NbtList();
        chests.forEach(chest -> chestList.add(NbtLong.of(MinecraftTypeAdapter.toBlockPos(chest).asLong())));
        record.put("chests", chestList);
        NbtList history = region.getList("history", NbtElement.COMPOUND_TYPE);
        if (history.size() >= 1024) throw new IllegalStateException("history_budget_exceeded");
        history.add(record);
        region.put("history", history);
        job.putString("state", "approved");
        job.remove("reason");
        job.remove("retryAt");
        job.putBoolean("pendingWrite", true);
        region.put("job", job);
        store.update(region);
        if (ColonyChests.withdraw(world, chests, item, count) != count) {
            throw new IllegalStateException("material_withdraw_failed");
        }
        for (int i = 0; i < parts.size(); i++) {
            NbtCompound part = parts.getCompound(i);
            world.setBlockState(BlockPos.fromLong(part.getLong("position")),
                    MaeWorldWriter.decodeState(part.getCompound("after")), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        for (int i = 0; i < parts.size(); i++) {
            NbtCompound part = parts.getCompound(i);
            if (!world.getBlockState(BlockPos.fromLong(part.getLong("position")))
                    .equals(MaeWorldWriter.decodeState(part.getCompound("after")))) {
                throw new IllegalStateException("write_not_confirmed");
            }
        }
        NbtCompound actual = MaeGenerationEvidence.inspect(world, MaeProtection.margin(area), false, 8);
        record.putString("result", "applied");
        record.putString("materials", "consumed");
        history.set(history.size() - 1, record);
        region.put("history", history);
        int cursor = job.getInt("cursor") + 1;
        job.putBoolean("pendingWrite", false);
        job.putInt("cursor", cursor);
        job.put("expectedSections", actual);
        NbtCompound owned = region.getCompound("ownedSections");
        for (String key : actual.getKeys()) owned.put(key, actual.get(key).copy());
        region.put("ownedSections", owned);
        // O acesso real ainda precisa ser conferido; até lá as seções seguem ocupadas — ADR-046.
        if (cursor == job.getList("plan", NbtElement.COMPOUND_TYPE).size()) job.putString("state", "built");
        region.put("job", job);
        store.update(region);
        return true;
    }

    static boolean runnable(ServerWorld world, NbtCompound job) {
        return "approved".equals(job.getString("state")) || "built".equals(job.getString("state"))
                || ("waiting".equals(job.getString("state")) && world.getTime() >= job.getLong("retryAt"));
    }

    /**
     * Obra escrita só conclui depois de o acesso passar nos blocos reais; falhou, bloqueia e não conclui.
     * Concluída, entra no registro de edifícios pelo mesmo {@code Building.of(project, true)} do fechamento
     * legado e libera as seções — ADR-046 item 10.
     */
    static void verify(ServerWorld world, NbtCompound region, NbtCompound job, MaeWorkStore store, Sources sources) {
        UUID regionId = region.getUuid("id");
        var checks = CHECKS.computeIfAbsent(world, ignored -> new HashMap<>());
        try {
            WorkArea area = MaeWorldWriter.area(job);
            MaeSectionLocks.check(world, store, regionId, MaeProtection.margin(area));
            validate(world, region, job);
            if (job.getInt("cursor") != job.getList("plan", NbtElement.COMPOUND_TYPE).size()) {
                throw new IllegalStateException("invalid_job_bounds_or_cursor");
            }
            if (!MaeGenerationEvidence.inspect(world, MaeProtection.margin(area), false, 8)
                    .equals(job.getCompound("expectedSections"))) {
                throw new IllegalStateException("world_changed_since_approval");
            }
            Check check = checks.get(regionId);
            if (check == null) {
                ConstructionProject project = rebuilt(world, region, job, sources);
                NbtCompound access = job.getCompound("access");
                List<ColonyPos> circulation = access.getList("circulation", NbtElement.LONG_TYPE).stream()
                        .map(at -> MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(((NbtLong) at).longValue()))).toList();
                check = new Check(project, MaeStructureAccess.begin(world, project, List.of(), circulation,
                        MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(access.getLong("goal"))), true));
                checks.put(regionId, check);
            }
            check.survey().runSlice();
            if (!check.survey().complete()) return;
            checks.remove(regionId);
            try {
                check.survey().result();
            } catch (IllegalStateException failed) {
                job.putString("detail", failed.getMessage() == null ? "unknown" : failed.getMessage());
                throw new IllegalStateException("access_not_verified");
            }
            VillageColonyMod.BUILDINGS.registerOrMerge(Building.of(check.project(), true));
            job.putString("state", "complete");
            region.put("job", job);
            store.update(region);
            VillageColonyMod.LOGGER.info("VC_MAE_STRUCTURE version=1 colony={} building={} at={} result=complete",
                    region.getUuid("colony").toString().substring(0, 8), job.getString("blueprint"),
                    BlockPos.fromLong(job.getLong("origin")).toShortString());
        } catch (IllegalStateException | IllegalArgumentException failure) {
            checks.remove(regionId);
            job.putString("state", "blocked");
            job.putString("reason", failure.getMessage() == null ? "invalid_payload" : failure.getMessage());
            region.put("job", job);
            store.update(region);
        }
    }

    /** O projeto refeito do template conferido; hash diferente recusa — a conferência não usa outra planta. */
    private static ConstructionProject rebuilt(ServerWorld world, NbtCompound region, NbtCompound job, Sources sources) {
        Identifier id = Identifier.tryParse(job.getString("blueprint"));
        if (id == null) throw new IllegalStateException("template_changed");
        var origin = MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(job.getLong("origin")));
        var project = sources.template(world, MinecraftTypeAdapter.toResourceId(id))
                .map(read -> ConstructionProject.plan(region.getUuid("colony"), read.rotated(job.getInt("rotation")), origin))
                .orElseThrow(() -> new IllegalStateException("template_changed"));
        if (!hash(project).equals(job.getString("template"))) throw new IllegalStateException("template_changed");
        return project;
    }

    /** Retomada de obra já escrita volta a {@code built} para conferir o acesso de novo; diz se tratou. */
    static boolean resume(ServerWorld world, NbtCompound region, NbtCompound job, MaeWorkStore store, Sources sources) {
        revalidate(world, region, job, store, sources);
        if (job.getInt("cursor") != job.getList("plan", NbtElement.COMPOUND_TYPE).size()) return false;
        validate(world, region, job);
        job.putString("state", "built");
        job.remove("reason");
        job.remove("detail");
        region.put("job", job);
        store.update(region);
        return true;
    }

    /** Retomada explícita: escrita interrompida bloqueia com causa, template diferente também. */
    static void revalidate(ServerWorld world, NbtCompound region, NbtCompound job, MaeWorkStore store, Sources sources) {
        String reason = null;
        if (job.getBoolean("pendingWrite")) {
            reason = interrupted(world, job);
        } else if (!job.getString("template").equals(templateHash(world, job, sources))) {
            reason = "template_changed";
        }
        if (reason != null) {
            job.putString("state", "blocked");
            job.putString("reason", reason);
            region.put("job", job);
            store.update(region);
            throw new IllegalStateException(reason);
        }
    }

    private static String interrupted(ServerWorld world, NbtCompound job) {
        NbtList parts = job.getList("plan", NbtElement.COMPOUND_TYPE).getCompound(job.getInt("cursor"))
                .getList("parts", NbtElement.COMPOUND_TYPE);
        int before = 0;
        int after = 0;
        for (int i = 0; i < parts.size(); i++) {
            NbtCompound part = parts.getCompound(i);
            BlockState actual = world.getBlockState(BlockPos.fromLong(part.getLong("position")));
            if (actual.equals(MaeWorldWriter.decodeState(part.getCompound("before")))) before++;
            else if (actual.equals(MaeWorldWriter.decodeState(part.getCompound("after")))) after++;
        }
        return parts.isEmpty() || before + after != parts.size() || (before > 0 && after > 0)
                ? "partial_parts" : "materials_uncertain";
    }

    private static Set<BlockPos> written(NbtCompound job) {
        Set<BlockPos> result = new HashSet<>();
        NbtList plan = job.getList("plan", NbtElement.COMPOUND_TYPE);
        int done = Math.min(job.getInt("cursor"), plan.size());
        for (int s = 0; s < done; s++) {
            NbtList parts = plan.getCompound(s).getList("parts", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < parts.size(); i++) result.add(BlockPos.fromLong(parts.getCompound(i).getLong("position")));
        }
        return result;
    }

    private static String templateHash(ServerWorld world, NbtCompound job, Sources sources) {
        Identifier id = Identifier.tryParse(job.getString("blueprint"));
        if (id == null) return "";
        var origin = MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(job.getLong("origin")));
        return sources.template(world, MinecraftTypeAdapter.toResourceId(id))
                .map(read -> hash(ConstructionProject.plan(UUID.randomUUID(), read.rotated(job.getInt("rotation")), origin)))
                .orElse("");
    }

    private static String templateHash(ServerWorld world, ConstructionProject project, int quarterTurns, Sources sources) {
        return sources.template(world, project.blueprint().id())
                .map(read -> hash(ConstructionProject.plan(project.colonyId(), read.rotated(quarterTurns), project.origin())))
                .orElse("");
    }

    /** SHA-256 das partes relativas à origem e dos estados; muda se o template carregado mudar. */
    private static String hash(ConstructionProject project) {
        BlockPos origin = MinecraftTypeAdapter.toBlockPos(project.origin());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        for (var piece : MaeStructurePieces.ordered(project)) {
            for (var part : piece.parts()) {
                BlockPos relative = part.getKey().subtract(origin);
                digest.update((relative.toShortString() + "=" + NbtHelper.fromBlockState(part.getValue()) + ";")
                        .getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) '|');
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static WorkArea box(ConstructionProject project) {
        Building lot = Building.of(project);
        return new WorkArea(lot.min(), lot.max());
    }

    private static boolean sameFootprint(WorkArea footprint, WorkArea box) {
        return footprint.min().x() == box.min().x() && footprint.max().x() == box.max().x()
                && footprint.min().z() == box.min().z() && footprint.max().z() == box.max().z();
    }

    private static NbtCompound wrap(NbtCompound area) {
        NbtCompound parent = new NbtCompound();
        parent.put("area", area);
        return parent;
    }
}
