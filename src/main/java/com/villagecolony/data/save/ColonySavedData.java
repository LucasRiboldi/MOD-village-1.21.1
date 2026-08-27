package com.villagecolony.data.save;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyRoads;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.service.ConstructionService;
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
import java.util.HashSet;
import java.util.List;
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

    private static final String COLONIES = "colonies";
    private static final String ID = "id";
    private static final String CENTER_X = "centerX";
    private static final String CENTER_Y = "centerY";
    private static final String CENTER_Z = "centerZ";
    private static final String STATE = "state";
    private static final String OBSERVED_BEDS = "observedBeds";

    private static final String WORKERS = "workers";
    private static final String VILLAGER_ID = "villagerId";
    private static final String COLONY_ID = "colonyId";
    private static final String PROFESSION = "profession";

    private static final String PROJECTS = "projects";
    private static final String BUILDINGS = "buildings";
    private static final String BLUEPRINT = "blueprint";
    private static final String ORIGIN_X = "originX";
    private static final String ORIGIN_Y = "originY";
    private static final String ORIGIN_Z = "originZ";
    private static final String MIN_X = "minX";
    private static final String MIN_Y = "minY";
    private static final String MIN_Z = "minZ";
    private static final String MAX_X = "maxX";
    private static final String MAX_Y = "maxY";
    private static final String MAX_Z = "maxZ";

    /** As peças da Regra 21 que esta casa já recebeu, uma vez cada. */

    public static final PersistentState.Type<ColonySavedData> TYPE = new PersistentState.Type<>(
            ColonySavedData::new,
            ColonySavedData::readNbt,
            null);

    private final List<Colony> colonies = new ArrayList<>();

    private final List<Worker> workers = new ArrayList<>();

    /**
     * As obras em andamento, e o que a colônia já levantou.
     *
     * <p>Entraram no mesmo arquivo das colônias pelo mesmo motivo que os
     * trabalhadores: gravar em arquivos separados permitiria construção
     * órfã, apontando para colônia que não foi gravada, sem transação que
     * mantivesse os dois em sincronia.
     */
    private final List<ConstructionService.Pending> projects = new ArrayList<>();

    private final List<Building> buildings = new ArrayList<>();

    /**
     * A mina de cada colônia — Regra 29.
     *
     * <p>No mesmo arquivo pelo mesmo motivo de sempre: a mina aponta
     * para a colônia por id, e mina órfã seria escada de dono nenhum.
     */
    private final List<Mine> mines = new ArrayList<>();

    /**
     * As ruas que cada colônia já mediu — 2026-08-27.
     *
     * <p>No mesmo arquivo pelo mesmo motivo de sempre: o índice aponta
     * para a colônia por id, e índice órfão seria mapa de vila nenhuma.
     */
    private final List<ColonyRoads> roads = new ArrayList<>();

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

            buildingList.add(entry);
        }

        nbt.put(BUILDINGS, buildingList);

        MineSave.write(nbt, mines);
        RoadIndexSave.write(nbt, roads);

        return nbt;
    }

    private static ColonySavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ColonySavedData data = new ColonySavedData();

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

            Colony colony = Colony.restore(id, center, readState(entry), ColonyLifecycle.DORMANT);

            // Save antigo não tem o campo; getInt devolve 0, que apenas
            // faz a primeira detecção da sessão valer. Autocorrige.
            colony.observe(center, entry.getInt(OBSERVED_BEDS));

            data.colonies.add(colony);
        }

        readWorkers(nbt, data);
        readConstruction(nbt, data);

        return data;
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
    private static void readConstruction(NbtCompound nbt, ColonySavedData data) {
        Set<UUID> knownColonies = new HashSet<>();

        for (Colony colony : data.colonies) {
            knownColonies.add(colony.id());
        }

        data.mines.addAll(MineSave.read(nbt, knownColonies));
        data.roads.addAll(RoadIndexSave.read(nbt, knownColonies));

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
                    readConstructionState(entry)));
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
                    new ColonyPos(entry.getInt(MAX_X), entry.getInt(MAX_Y), entry.getInt(MAX_Z))));
        }
    }

    /**
     * O estado gravado da obra.
     *
     * <p>Estado desconhecido — de uma versão futura, ou de save editado —
     * vira BUILDING, que é o estado de onde a obra continua sozinha. Cair
     * em COMPLETED apagaria do registro uma casa pela metade; cair em
     * PLANNED a faria esperar por uma preparação que já aconteceu.
     */
    private static ConstructionState readConstructionState(NbtCompound entry) {
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
    private static void readWorkers(NbtCompound nbt, ColonySavedData data) {
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
     */
    private static ProfessionType readProfession(NbtCompound entry) {
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
    private static ColonyState readState(NbtCompound entry) {
        String name = entry.getString(STATE);

        for (ColonyState state : ColonyState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }

        return ColonyState.STABLE;
    }
}
