package com.villagecolony.data.save;

import static com.villagecolony.data.save.ColonySavedData.*;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ColonySweepCursor;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.telemetry.model.ActivityTrace;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.colony.model.ColonyLifecycle;
import com.villagecolony.core.colony.model.ColonyState;
import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A leitura do save, peça por peça: obras, construções, trabalhadores e
 * estados — separado de {@link ColonySavedData} em 2026-09-24, quando ele
 * passou de 500 linhas.
 *
 * <p>O {@code ColonySavedData} continua sendo o dono do formato (chaves,
 * escrita e migração); aqui fica só a tradução de volta do NBT, com as mesmas
 * regras de descarte de dono desconhecido. Os comentários vieram junto sem
 * mudança.
 */
final class ColonySaveReader {

    private ColonySaveReader() {
    }

    /**
     * Lê obras e construções, descartando as de colônia desconhecida.
     *
     * <p>Mesma regra dos trabalhadores, e pelo mesmo motivo: uma casa de
     * colônia inexistente seria protegida para sempre por um dono que
     * ninguém acha.
     *
     * <p>Save anterior a 2026-08-14 não tem as chaves, e {@code getList}
     * devolve lista vazia. Autocorrige: perde-se a memória de casas
     * construídas antes desta versão, que é exatamente o que a versão
     * anterior perdia toda vez.
     */
    static void readConstruction(NbtCompound nbt, ColonySavedData data) {
        Set<UUID> knownColonies = new HashSet<>();

        for (Colony colony : data.colonies) {
            knownColonies.add(colony.id());
        }

        data.mines.addAll(MineSave.read(nbt, knownColonies));
        data.roads.addAll(RoadIndexSave.read(nbt, knownColonies));
        data.sweeps.addAll(SweepCursorSave.read(nbt, knownColonies));
        data.activityTraces.putAll(ActivityTraceSave.read(nbt, knownColonies));

        NbtList projectList = nbt.getList(PROJECTS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < projectList.size(); i++) {
            NbtCompound entry = projectList.getCompound(i);

            if (!entry.containsUuid(ID) || !entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            data.projects.add(new ConstructionService.Pending(
                    entry.getUuid(ID),
                    colonyId,
                    ResourceId.parse(entry.getString(BLUEPRINT)),
                    new ColonyPos(
                            entry.getInt(ORIGIN_X),
                            entry.getInt(ORIGIN_Y),
                            entry.getInt(ORIGIN_Z)),
                    readConstructionState(entry),
                    readDeferredPieces(entry)));
        }

        NbtList buildingList = nbt.getList(BUILDINGS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < buildingList.size(); i++) {
            NbtCompound entry = buildingList.getCompound(i);

            if (!entry.containsUuid(ID) || !entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            ResourceId blueprint = ResourceId.parse(entry.getString(BLUEPRINT));

            data.buildings.add(new Building(
                    entry.getUuid(ID),
                    colonyId,
                    blueprint,
                    new ColonyPos(entry.getInt(MIN_X), entry.getInt(MIN_Y), entry.getInt(MIN_Z)),
                    new ColonyPos(entry.getInt(MAX_X), entry.getInt(MAX_Y), entry.getInt(MAX_Z)),
                    !entry.contains(FINISHED) || entry.getBoolean(FINISHED)));
        }
    }

    /**
     * Lê a memória mínima de uma peça sem apoio.
     *
     * <p>É opcional para que saves anteriores continuem válidos. Entrada
     * inválida é descartada: ela só regula uma nova tentativa, nunca diz
     * que um bloco existe no mundo.
     */
    static List<ConstructionProject.DeferredPiece> readDeferredPieces(NbtCompound project) {
        List<ConstructionProject.DeferredPiece> pieces = new ArrayList<>();
        NbtList saved = project.getList(DEFERRED_PIECES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < saved.size(); i++) {
            NbtCompound entry = saved.getCompound(i);

            if (!entry.contains(DEFERRED_BLOCK, NbtElement.STRING_TYPE)
                    || !entry.contains(DEFERRED_REASON, NbtElement.STRING_TYPE)
                    || !entry.contains(SUPPORT_FINGERPRINT, NbtElement.STRING_TYPE)) {
                continue;
            }

            SkipReason reason = readSkipReason(entry.getString(DEFERRED_REASON));
            String fingerprint = entry.getString(SUPPORT_FINGERPRINT);

            if (reason == null || fingerprint.isBlank()) {
                continue;
            }

            pieces.add(new ConstructionProject.DeferredPiece(
                    new ColonyPos(
                            entry.getInt(POSITION_X),
                            entry.getInt(POSITION_Y),
                            entry.getInt(POSITION_Z)),
                    ResourceId.parse(entry.getString(DEFERRED_BLOCK)),
                    reason,
                    fingerprint));
        }

        return pieces;
    }

    static SkipReason readSkipReason(String name) {
        for (SkipReason reason : SkipReason.values()) {
            if (reason.name().equals(name)) {
                return reason;
            }
        }

        return null;
    }

    /**
     * O estado gravado da obra.
     *
     * <p>Estado desconhecido — de uma versão futura, ou de save editado —
     * vira BUILDING, que é o estado de onde a obra continua sozinha. Cair
     * em COMPLETED apagaria do registro uma casa pela metade; cair em
     * PLANNED a faria esperar por uma preparação que já aconteceu.
     */
    static ConstructionState readConstructionState(NbtCompound entry) {
        String name = entry.getString(STATE);

        for (ConstructionState state : ConstructionState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }

        return ConstructionState.BUILDING;
    }

    /**
     * Lê os trabalhadores, descartando os que apontam para colônia que
     * não veio no mesmo arquivo.
     *
     * <p>Um órfão não deveria existir — colônias e trabalhadores são
     * gravados juntos. Se existir, o save foi editado ou corrompido, e
     * um trabalhador de colônia inexistente seria invisível para sempre:
     * nenhuma colônia o listaria, e a varredura não o recriaria, porque
     * o villagerId já teria dono. Descartar deixa a varredura reencontrá-lo
     * e reatribuí-lo à colônia certa, ao custo da profissão que ele tinha.
     *
     * <p>Chamado depois das colônias, e depende disso.
     */
    static void readWorkers(NbtCompound nbt, ColonySavedData data) {
        Set<UUID> knownColonies = new HashSet<>();

        for (Colony colony : data.colonies) {
            knownColonies.add(colony.id());
        }

        NbtList list = nbt.getList(WORKERS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(VILLAGER_ID) || !entry.containsUuid(COLONY_ID)) {
                continue;
            }

            UUID colonyId = entry.getUuid(COLONY_ID);

            if (!knownColonies.contains(colonyId)) {
                continue;
            }

            data.workers.add(Worker.restore(
                    entry.getUuid(VILLAGER_ID),
                    colonyId,
                    readProfession(entry)));
        }
    }

    /**
     * Profissão ausente ou desconhecida vira "sem função".
     *
     * <p>Mesmo princípio de {@link #readState}: não derrubar o
     * carregamento do mundo. Aqui o custo é menor — a atribuição inicial
     * dá uma função nova ao aldeão no próximo ciclo (TASK-014).
     *
     * <p><b>{@code BREEDER} não chega mais até aqui</b> — desde
     * {@link SaveMigration}, 2026-09-24, ele já virou {@code SHEPHERD}
     * antes de {@link ColonySavedData#readNbt} montar esta lista. O laço abaixo
     * continua sendo a última linha de defesa contra um enum removido
     * do código ou um save editado à mão.
     */
    static ProfessionType readProfession(NbtCompound entry) {
        if (!entry.contains(PROFESSION, NbtElement.STRING_TYPE)) {
            return null;
        }

        String name = entry.getString(PROFESSION);

        for (ProfessionType profession : ProfessionType.values()) {
            if (profession.name().equals(name)) {
                return profession;
            }
        }

        return null;
    }

    /**
     * Um estado desconhecido no save não pode derrubar o carregamento do
     * mundo. Cair para STABLE perde a intenção da colônia, mas ela
     * reavalia o que fazer no próximo ciclo.
     */
    static ColonyState readState(NbtCompound entry) {
        String name = entry.getString(STATE);

        for (ColonyState state : ColonyState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }

        return ColonyState.STABLE;
    }
}
