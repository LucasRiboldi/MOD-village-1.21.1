package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As marcas de trabalho que precisam sobreviver ao carregar o mundo —
 * 2026-09-25, ADR-025.
 *
 * <p><b>Por que existe.</b> O mineiro voltava à mesma pedra inalcançável
 * (553, 39, 158 no mundo do autor) em todas as sessões: a marca que afasta a
 * pedra recusada ({@code MineMarks}) vivia só em memória. O prazo dela é
 * contado em tiques do mundo, que o jogo salva, então guardar a marca basta
 * para ela continuar valendo depois de carregar.
 *
 * <p><b>Por que um arquivo à parte.</b> O {@link ColonySavedData} é o estado
 * das colônias e já está no teto de tamanho do projeto; marca de trabalho é
 * outra natureza de dado — de vida curta, com teto próprio, que pode ser
 * perdida sem estragar colônia nenhuma. Mesmo {@code PersistentStateManager},
 * outra chave.
 */
public final class WorkMarksSavedData extends PersistentState {

    public static final String KEY = "villagecolony_marks";

    static final String MINE_REFUSALS = "mineRefusals";

    static final String SUPPLY_ATTEMPTS = "supplyAttempts";

    static final String STORAGES = "workerChests";

    /**
     * O baú de cada trabalhador — decisão do autor, 2026-09-26. Era refeito a
     * cada carregamento a partir da cama do momento, e aldeão troca de cama:
     * na sessão longa daquele dia o lenhador, o mineiro e o pedreiro voltaram
     * sem baú e passaram 4h45 sem tarefa.
     */
    public record WorkerChest(java.util.UUID worker, int x, int y, int z) {
    }

    private final List<WorkerChest> workerChests = new ArrayList<>();

    public static final PersistentState.Type<WorkMarksSavedData> TYPE = new PersistentState.Type<>(
            WorkMarksSavedData::new,
            WorkMarksSavedData::readNbt,
            null);

    /** Uma pedra recusada: onde, desde qual tique do mundo, e quantas vezes. */
    public record MineRefusal(int x, int y, int z, long since, int count) {
    }

    private final List<MineRefusal> mineRefusals = new ArrayList<>();

    /**
     * Tentativas de uma peça sem rota profissional, por chave "colônia/item".
     */
    private final Map<String, Integer> supplyAttempts = new HashMap<>();

    public static WorkMarksSavedData get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE, KEY);
    }

    /** Copia as marcas em memória para cá e marca para gravação. */
    public void sync(Collection<MineRefusal> currentMineRefusals) {
        mineRefusals.clear();
        mineRefusals.addAll(currentMineRefusals);

        markDirty();
    }

    public List<MineRefusal> mineRefusals() {
        return List.copyOf(mineRefusals);
    }

    /** Copia as tentativas de suprimento em memória para cá e marca para gravação. */
    public void syncSupplyAttempts(Map<String, Integer> current) {
        supplyAttempts.clear();
        supplyAttempts.putAll(current);

        markDirty();
    }

    public Map<String, Integer> supplyAttempts() {
        return Map.copyOf(supplyAttempts);
    }

    /** Copia o baú de cada trabalhador para cá e marca para gravação. */
    public void syncWorkerChests(Collection<WorkerChest> current) {
        workerChests.clear();
        workerChests.addAll(current);

        markDirty();
    }

    public List<WorkerChest> workerChests() {
        return List.copyOf(workerChests);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList list = new NbtList();

        for (MineRefusal refusal : mineRefusals) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("x", refusal.x());
            entry.putInt("y", refusal.y());
            entry.putInt("z", refusal.z());
            entry.putLong("since", refusal.since());
            entry.putInt("count", refusal.count());
            list.add(entry);
        }

        nbt.put(MINE_REFUSALS, list);

        NbtCompound attempts = new NbtCompound();
        supplyAttempts.forEach(attempts::putInt);
        nbt.put(SUPPLY_ATTEMPTS, attempts);

        NbtList chests = new NbtList();

        for (WorkerChest chest : workerChests) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("worker", chest.worker());
            entry.putInt("x", chest.x());
            entry.putInt("y", chest.y());
            entry.putInt("z", chest.z());
            chests.add(entry);
        }

        nbt.put(STORAGES, chests);

        return nbt;
    }

    public static WorkMarksSavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        WorkMarksSavedData data = new WorkMarksSavedData();

        NbtList list = nbt.getList(MINE_REFUSALS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);

            // Contagem zero é marca que não recusa nada: entrada estragada,
            // e descartá-la é a leitura mais segura.
            if (entry.getInt("count") <= 0) {
                continue;
            }

            data.mineRefusals.add(new MineRefusal(
                    entry.getInt("x"),
                    entry.getInt("y"),
                    entry.getInt("z"),
                    entry.getLong("since"),
                    entry.getInt("count")));
        }

        NbtList chests = nbt.getList(STORAGES, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < chests.size(); i++) {
            NbtCompound entry = chests.getCompound(i);

            if (entry.containsUuid("worker")) {
                data.workerChests.add(new WorkerChest(
                        entry.getUuid("worker"), entry.getInt("x"), entry.getInt("y"), entry.getInt("z")));
            }
        }

        NbtCompound attempts = nbt.getCompound(SUPPLY_ATTEMPTS);

        for (String key : attempts.getKeys()) {
            data.supplyAttempts.put(key, attempts.getInt(key));
        }

        return data;
    }
}
