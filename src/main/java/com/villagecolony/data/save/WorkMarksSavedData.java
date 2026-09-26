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

    static final String SUPPLY_WAITS = "supplyWaits";

    public static final PersistentState.Type<WorkMarksSavedData> TYPE = new PersistentState.Type<>(
            WorkMarksSavedData::new,
            WorkMarksSavedData::readNbt,
            null);

    /** Uma pedra recusada: onde, desde qual tique do mundo, e quantas vezes. */
    public record MineRefusal(int x, int y, int z, long since, int count) {
    }

    private final List<MineRefusal> mineRefusals = new ArrayList<>();

    /**
     * Desde quando cada peça de obra espera a rota do bioma entregar — chave
     * "colônia/item", valor em tiques do mundo. Ver
     * {@code BiomeConstructionSupply.waits}; 2026-09-26.
     */
    private final Map<String, Long> supplyWaits = new HashMap<>();

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

    /** Copia as esperas de rota em memória para cá e marca para gravação. */
    public void syncSupplyWaits(Map<String, Long> current) {
        supplyWaits.clear();
        supplyWaits.putAll(current);

        markDirty();
    }

    public Map<String, Long> supplyWaits() {
        return Map.copyOf(supplyWaits);
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

        NbtCompound waits = new NbtCompound();
        supplyWaits.forEach(waits::putLong);
        nbt.put(SUPPLY_WAITS, waits);

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

        NbtCompound waits = nbt.getCompound(SUPPLY_WAITS);

        for (String key : waits.getKeys()) {
            data.supplyWaits.put(key, waits.getLong(key));
        }

        return data;
    }
}
