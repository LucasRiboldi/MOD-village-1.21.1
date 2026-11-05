package com.villagecolony.fabric.integration;

import com.villagecolony.data.save.MaeWorkStore;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.UUID;

/**
 * A região MAE que já cumpriu o papel volta para a vila — Regra 187, emenda da ADR-045 item 10 (autor, 2026-11-05).
 *
 * <p>Enquanto há obra, a região é exclusiva. Entregue, ela para de bloquear o caminho comum, a releitura do grafo e a
 * redescoberta de ruas, e não recebe outra obra. O histórico fica no save; nada é desfeito.
 */
final class MaeHandover {

    private static final String HANDED_OVER = "handedOver";

    private MaeHandover() {
    }

    static boolean isHandedOver(NbtCompound region) {
        return region.getBoolean(HANDED_OVER);
    }

    static void handOver(MaeWorkStore store, UUID regionId) {
        store.region(regionId).ifPresent(region -> {
            region.putBoolean(HANDED_OVER, true);
            region.remove(MaeSiteRecord.KEY);
            store.update(region);
        });
    }

    /** Nenhuma escrita registrada e nenhum job em curso: entregar não muda nada no mundo. */
    static boolean untouched(NbtCompound region) {
        return appliedChanges(region) == 0 && !hasUnconfirmedWork(region)
                && !"approved".equals(region.getCompound("job").getString("state"));
    }

    /** Escrita ou publicação sem confirmação: só a análise solta esta região (ADR-044). */
    static boolean hasUnconfirmedWork(NbtCompound region) {
        NbtCompound job = region.getCompound("job");

        return job.getBoolean("pendingWrite") || job.getBoolean("pendingPublication");
    }

    /** Quantas escritas reais a região registrou. */
    static int appliedChanges(NbtCompound region) {
        int applied = 0;

        for (NbtElement element : region.getList("history", NbtElement.COMPOUND_TYPE)) {
            if ("applied".equals(((NbtCompound) element).getString("result"))) {
                applied++;
            }
        }

        return applied;
    }
}
