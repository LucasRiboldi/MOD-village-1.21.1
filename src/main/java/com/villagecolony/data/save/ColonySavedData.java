package com.villagecolony.data.save;

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
 * Grava e recarrega as colônias junto com o mundo.
 *
 * <p>Contém apenas serialização. Nenhuma regra de domínio mora aqui —
 * ver ADR-006 §5. Quem decide o que fazer com as colônias é
 * {@link ColonyService}.
 *
 * <p>Os dados ficam presos ao Overworld porque a colônia pertence ao
 * mundo, não ao jogador. Ver Save-Data-System.md.
 *
 * <p>Colônias e trabalhadores moram no mesmo arquivo de propósito
 * (TASK-012b): o trabalhador aponta para a colônia por id, e dois
 * arquivos separados poderiam ser gravados em momentos diferentes,
 * deixando trabalhador órfão apontando para colônia inexistente.
 */
public final class ColonySavedData extends PersistentState {

    /** Nome do arquivo em {@code data/}. Mudar isto invalida saves. */
    public static final String KEY = "villagecolony_colonies";

    static final String COLONIES = "colonies";
    static final String ID = "id";
    static final String CENTER_X = "centerX";
    static final String CENTER_Y = "centerY";
    static final String CENTER_Z = "centerZ";
    static final String STATE = "state";
    static final String OBSERVED_BEDS = "observedBeds";

    static final String WORKERS = "workers";
    static final String VILLAGER_ID = "villagerId";
    static final String COLONY_ID = "colonyId";
    static final String PROFESSION = "profession";

    static final String PROJECTS = "projects";
    static final String BUILDINGS = "buildings";
    static final String BLUEPRINT = "blueprint";
    static final String ORIGIN_X = "originX";
    static final String ORIGIN_Y = "originY";
    static final String ORIGIN_Z = "originZ";
    static final String DEFERRED_PIECES = "deferredPieces";
    static final String POSITION_X = "x";
    static final String POSITION_Y = "y";
    static final String POSITION_Z = "z";
    static final String DEFERRED_BLOCK = "block";
    static final String DEFERRED_REASON = "reason";
    static final String SUPPORT_FINGERPRINT = "supportFingerprint";
    static final String MIN_X = "minX";
    static final String MIN_Y = "minY";
    static final String MIN_Z = "minZ";
    static final String MAX_X = "maxX";
    static final String MAX_Y = "maxY";
    static final String MAX_Z = "maxZ";

    /**
     * Se esta construção é uma casa terminada — 2026-09-15.
     *
     * <p><b>Ausente vale terminada</b>, e é decisão. O save anterior a esta
     * data não guardava o campo, e nele a esmagadora maioria das
     * construções É casa levantada — o registro só passou a receber obra
     * abandonada em 09-12, com o {@code PatienceClock}. Ler as antigas como
     * inacabadas faria toda vila já construída voltar a preferir a planta
     * pequena, que é o oposto da Regra 25.
     *
     * <p>O preço é a obra abandonada de um save antigo continuar contando
     * como casa naquela colônia. É um erro que se apaga sozinho na primeira
     * casa que ela terminar, e menor que o outro.
     */
    static final String FINISHED = "finished";

    /** As peças da Regra 21 que esta casa já recebeu, uma vez cada. */

    public static final PersistentState.Type<ColonySavedData> TYPE = new PersistentState.Type<>(
            ColonySavedData::new,
            ColonySavedData::readNbt,
            null);

    final List<Colony> colonies = new ArrayList<>();

    final List<Worker> workers = new ArrayList<>();

    /**
     * As obras em andamento, e o que a colônia já levantou.
     *
     * <p>Entraram no mesmo arquivo das colônias pelo mesmo motivo que os
     * trabalhadores: gravar em arquivos separados permitiria construção
     * órfã, apontando para colônia que não foi gravada, sem transação que
     * mantivesse os dois em sincronia.
     */
    final List<ConstructionService.Pending> projects = new ArrayList<>();

    final List<Building> buildings = new ArrayList<>();

    /**
     * A mina de cada colônia — Regra 29.
     *
     * <p>No mesmo arquivo pelo mesmo motivo de sempre: a mina aponta
     * para a colônia por id, e mina órfã seria escada de dono nenhum.
     */
    final List<Mine> mines = new ArrayList<>();

    /**
     * As ruas que cada colônia já mediu — 2026-08-27.
     *
     * <p>No mesmo arquivo pelo mesmo motivo de sempre: o índice aponta
     * para a colônia por id, e índice órfão seria mapa de vila nenhuma.
     */
    final List<ColonyRoads> roads = new ArrayList<>();

    /**
     * A varredura que ficou no meio — 2026-08-27.
     *
     * <p>O outro lado do {@link #roads}: aquele guarda a volta
     * terminada, este a que não chegou ao fim. Nunca os dois para a
     * mesma colônia.
     */
    final List<ColonySweepCursor> sweeps = new ArrayList<>();

    /**
     * O traço circular de atividade de cada colônia — decisão 7B,
     * 2026-09-24.
     *
     * <p>No mesmo arquivo pelo mesmo motivo de sempre: um traço órfão
     * seria histórico de vila nenhuma.
     */
    final Map<UUID, ActivityTrace> activityTraces = new HashMap<>();

    private ColonySavedData() {
    }

    /**
     * Carrega os dados do mundo, criando-os vazios na primeira vez.
     *
     * <p>Este é o único ponto do mod que fala com o
     * {@code PersistentStateManager}.
     */
    public static ColonySavedData get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE, KEY);
    }

    /** Colônias lidas do disco. Vazio antes do primeiro save. */
    public List<Colony> colonies() {
        return List.copyOf(colonies);
    }

    /**
     * Trabalhadores lidos do disco, já sem os órfãos.
     *
     * <p>Vazio antes do primeiro save, e também em saves anteriores à
     * TASK-012b — nesse caso a varredura os reencontra, só sem profissão.
     */
    public List<Worker> workers() {
        return List.copyOf(workers);
    }

    /**
     * Copia os registros em memória para cá e marca para gravação.
     *
     * <p>O Minecraft grava quando decide gravar; nosso papel é apenas
     * garantir que o conteúdo esteja correto e sinalizado como sujo.
     *
     * <p>Os dois registros são copiados na mesma chamada porque são
     * gravados no mesmo arquivo: sincronizar um sem o outro produziria
     * exatamente o órfão que juntá-los evita.
     */
    public void sync(Collection<Colony> currentColonies, Collection<Worker> currentWorkers) {
        sync(currentColonies, currentWorkers, List.of(), List.of());
    }

    /**
     * O mesmo, gravando também as obras em andamento e as construções.
     *
     * @param currentProjects as obras em andamento, já reduzidas ao que
     *     se grava: identidade, estrutura, lugar e estado. O progresso
     *     não vai para o disco — quem sabe o que está de pé é o mundo.
     *     Ver {@code ConstructionProject.restore}
     * @param currentBuildings o que a colônia levantou. É este que dói
     *     perder: sem ele a colônia reabre o mundo sem saber que a casa
     *     é dela, e a proteção do PROJECT_CONSTITUTION.md §10 some
     */
    public void sync(
            Collection<Colony> currentColonies,
            Collection<Worker> currentWorkers,
            Collection<ConstructionService.Pending> currentProjects,
            Collection<Building> currentBuildings) {

        sync(currentColonies, currentWorkers, currentProjects, currentBuildings, List.of());
    }

    /**
     * O mesmo, gravando também a mina de cada colônia.
     *
     * @param currentMines a mina de cada colônia — a boca, o lado da
     *     descida, o lado da galeria e a fronteira já cavada. É a única
     *     parte do trabalho do mineiro que o mundo <b>não</b> guarda: os
     *     túneis abertos ficam onde estão, mas nada neles diz onde a
     *     escada começa nem até onde a picareta chegou
     */
    public void sync(
            Collection<Colony> currentColonies,
            Collection<Worker> currentWorkers,
            Collection<ConstructionService.Pending> currentProjects,
            Collection<Building> currentBuildings,
            Collection<Mine> currentMines) {

        sync(currentColonies, currentWorkers, currentProjects, currentBuildings,
                currentMines, List.of());
    }

    /**
     * O mesmo, gravando também o índice de ruas.
     *
     * @param currentRoads o índice de ruas de cada colônia. É a resposta
     *     cara de uma pergunta barata de reconferir: montá-lo custa
     *     varrer 16.641 colunas em dezessete ciclos, e cada coluna dele
     *     volta a ser perguntada ao mundo quando visitada
     */
    public void sync(
            Collection<Colony> currentColonies,
            Collection<Worker> currentWorkers,
            Collection<ConstructionService.Pending> currentProjects,
            Collection<Building> currentBuildings,
            Collection<Mine> currentMines,
            Collection<ColonyRoads> currentRoads) {

        sync(currentColonies, currentWorkers, currentProjects, currentBuildings,
                currentMines, currentRoads, List.of());
    }

    /**
     * O mesmo, gravando também a varredura que parou no meio.
     *
     * @param currentSweeps a varredura de cada colônia que parou no meio
     *     do raio, com o que ela já achou. Medido em 2026-08-27: uma
     *     sessão de catorze passagens das dezessete necessárias gravava
     *     <b>nada</b>, porque o índice só nasce de uma volta completa
     */
    public void sync(
            Collection<Colony> currentColonies,
            Collection<Worker> currentWorkers,
            Collection<ConstructionService.Pending> currentProjects,
            Collection<Building> currentBuildings,
            Collection<Mine> currentMines,
            Collection<ColonyRoads> currentRoads,
            Collection<ColonySweepCursor> currentSweeps) {

        sync(currentColonies, currentWorkers, currentProjects, currentBuildings,
                currentMines, currentRoads, currentSweeps, Map.of());
    }

    /**
     * O mesmo, gravando também o traço de atividade.
     *
     * @param currentActivityTraces o traço circular de atividade de cada
     *     colônia — decisão 7B, 2026-09-24. Sem UUID de coordenada, sem
     *     texto livre: seis campos por evento, no máximo
     *     {@link ActivityTrace#CAPACITY} por colônia
     */
    public void sync(
            Collection<Colony> currentColonies,
            Collection<Worker> currentWorkers,
            Collection<ConstructionService.Pending> currentProjects,
            Collection<Building> currentBuildings,
            Collection<Mine> currentMines,
            Collection<ColonyRoads> currentRoads,
            Collection<ColonySweepCursor> currentSweeps,
            Map<UUID, ActivityTrace> currentActivityTraces) {

        colonies.clear();
        colonies.addAll(currentColonies);

        workers.clear();
        workers.addAll(currentWorkers);

        projects.clear();
        projects.addAll(currentProjects);

        buildings.clear();
        buildings.addAll(currentBuildings);

        mines.clear();
        mines.addAll(currentMines);

        roads.clear();
        roads.addAll(currentRoads);

        sweeps.clear();
        sweeps.addAll(currentSweeps);

        activityTraces.clear();
        activityTraces.putAll(currentActivityTraces);

        markDirty();
    }

    /** As obras que o save trouxe, esperando o mundo. */
    public List<ConstructionService.Pending> projects() {
        return List.copyOf(projects);
    }

    /** O que a colônia levantou, segundo o save. */
    public List<Building> buildings() {
        return List.copyOf(buildings);
    }

    /** As minas que o save trouxe, uma por colônia. */
    public List<Mine> mines() {
        return List.copyOf(mines);
    }

    /** Os índices de rua que o save trouxe, um por colônia. */
    public List<ColonyRoads> roads() {
        return List.copyOf(roads);
    }

    /** As varreduras que o save trouxe pela metade, uma por colônia. */
    public List<ColonySweepCursor> sweeps() {
        return List.copyOf(sweeps);
    }

    /** O traço circular de atividade de cada colônia, uma por colônia. */
    public Map<UUID, ActivityTrace> activityTraces() {
        return Map.copyOf(activityTraces);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();

        for (Colony colony : colonies) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(ID, colony.id());
            entry.putInt(CENTER_X, colony.center().x());
            entry.putInt(CENTER_Y, colony.center().y());
            entry.putInt(CENTER_Z, colony.center().z());
            entry.putString(STATE, colony.state().name());
            entry.putInt(OBSERVED_BEDS, colony.observedBeds());

            list.add(entry);
        }

        nbt.put(COLONIES, list);

        NbtList workerList = new NbtList();

        for (Worker worker : workers) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(VILLAGER_ID, worker.villagerId());
            entry.putUuid(COLONY_ID, worker.colonyId());

            // Ausente, e não vazio, para quem ainda não tem função: a
            // leitura distingue "sem chave" de "chave desconhecida".
            worker.profession().ifPresent(p -> entry.putString(PROFESSION, p.name()));

            workerList.add(entry);
        }

        nbt.put(WORKERS, workerList);

        NbtList projectList = new NbtList();

        for (ConstructionService.Pending project : projects) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(ID, project.id());
            entry.putUuid(COLONY_ID, project.colonyId());
            entry.putString(BLUEPRINT, project.blueprint().toString());
            entry.putInt(ORIGIN_X, project.origin().x());
            entry.putInt(ORIGIN_Y, project.origin().y());
            entry.putInt(ORIGIN_Z, project.origin().z());
            entry.putString(STATE, project.state().name());

            NbtList deferred = new NbtList();

            for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
                NbtCompound savedPiece = new NbtCompound();

                savedPiece.putInt(POSITION_X, piece.position().x());
                savedPiece.putInt(POSITION_Y, piece.position().y());
                savedPiece.putInt(POSITION_Z, piece.position().z());
                savedPiece.putString(DEFERRED_BLOCK, piece.block().toString());
                savedPiece.putString(DEFERRED_REASON, piece.reason().name());
                savedPiece.putString(SUPPORT_FINGERPRINT, piece.supportFingerprint());
                deferred.add(savedPiece);
            }

            entry.put(DEFERRED_PIECES, deferred);

            projectList.add(entry);
        }

        nbt.put(PROJECTS, projectList);

        NbtList buildingList = new NbtList();

        for (Building building : buildings) {
            NbtCompound entry = new NbtCompound();

            entry.putUuid(ID, building.id());
            entry.putUuid(COLONY_ID, building.colonyId());
            entry.putString(BLUEPRINT, building.blueprint().toString());
            entry.putInt(MIN_X, building.min().x());
            entry.putInt(MIN_Y, building.min().y());
            entry.putInt(MIN_Z, building.min().z());
            entry.putInt(MAX_X, building.max().x());
            entry.putInt(MAX_Y, building.max().y());
            entry.putInt(MAX_Z, building.max().z());
            entry.putBoolean(FINISHED, building.finished());

            buildingList.add(entry);
        }

        nbt.put(BUILDINGS, buildingList);

        MineSave.write(nbt, mines);
        RoadIndexSave.write(nbt, roads);
        SweepCursorSave.write(nbt, sweeps);
        ActivityTraceSave.write(nbt, activityTraces);

        return nbt;
    }

    private static ColonySavedData readNbt(NbtCompound rawNbt, RegistryWrapper.WrapperLookup registries) {
        ColonySavedData data = new ColonySavedData();

        // A migração roda antes de qualquer leitor olhar um valor —
        // decisão 8A, 2026-09-24. Ela nunca cria colônia, obra ou bloco;
        // só normaliza o NBT para a forma que os leitores abaixo esperam.
        NbtCompound nbt = SaveMigration.migrate(rawNbt);

        NbtList list = nbt.getList(COLONIES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            if (!entry.containsUuid(ID)) {
                continue;
            }

            UUID id = entry.getUuid(ID);

            ColonyPos center = new ColonyPos(
                    entry.getInt(CENTER_X),
                    entry.getInt(CENTER_Y),
                    entry.getInt(CENTER_Z));

            Colony colony = Colony.restore(id, center, ColonySaveReader.readState(entry), ColonyLifecycle.DORMANT);

            // Save antigo não tem o campo; getInt devolve 0, que apenas
            // faz a primeira detecção da sessão valer. Autocorrige.
            colony.observe(center, entry.getInt(OBSERVED_BEDS));

            data.colonies.add(colony);
        }

        ColonySaveReader.readWorkers(nbt, data);
        ColonySaveReader.readConstruction(nbt, data);

        return data;
    }

}
