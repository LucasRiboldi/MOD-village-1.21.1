package com.villagecolony.fabric.integration;

import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Registra como trabalhadores os aldeões vivos de uma colônia.
 *
 * <p>Detecta aldeões dentro de uma colônia — não confundir com
 * {@link VillageScanner}, que detecta vilas. A ADR-003 §7 exige que os
 * dois nomes não se misturem.
 *
 * <p>Nunca busca aldeões no mundo inteiro: a caixa parte do centro da
 * colônia, conforme Performance-Rules.md §5.
 */
public final class VillagerScanner {

    private VillagerScanner() {
    }

    /**
     * Varre os arredores do centro da colônia e registra quem encontrar.
     *
     * <p>Usa o mesmo raio da detecção de vila: é o alcance que a ADR-003
     * já define como "os arredores desta vila", e inventar um segundo
     * número aqui só criaria duas noções de perto.
     *
     * <p>Aldeões bebês são registrados. Eles crescem, e a colônia perde
     * menos ao já conhecê-los do que ao redescobri-los depois.
     *
     * <p>O baú de cada aldeão é procurado na mesma passagem: são os
     * mesmos aldeões, e uma segunda consulta de entidades por ciclo só
     * para isso seria desperdício. Quem já tem baú não é revisitado.
     *
     * @return quantos trabalhadores e quantos baús foram registrados
     *     agora — zero e zero quando nada mudou
     */
    public static ScanResult scan(
            ServerWorld world,
            Colony colony,
            WorkerService workers,
            StorageRegistry storages) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());

        Box area = Box.of(
                center.toCenterPos(),
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0);

        int registered = 0;
        int storagesFound = 0;

        Set<UUID> employable = new HashSet<>();

        for (VillagerEntity villager
                : world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)) {

            if (!workers.isRegistered(villager.getUuid())) {
                workers.register(villager.getUuid(), colony.id());
                registered++;
            }

            if (canWork(villager)) {
                employable.add(villager.getUuid());
            }

            // Baú é de quem trabalha.
            //
            // Antes de 2026-08-12 todo aldeão reivindicava um, e a vila
            // do autor mostrou o que isso custa: treze baús presos, dos
            // quais só dois pertenciam a alguém com função. O fazendeiro
            // e o construtor não conseguiam reivindicar nenhum, porque os
            // vizinhos desempregados tinham chegado primeiro.
            //
            // Fora do if de registro: o aldeão pode já ser conhecido e
            // ainda não ter baú, seja porque o jogador o construiu
            // depois, seja porque o chunk dele não estava carregado no
            // ciclo anterior, seja porque ele acabou de receber função.
            if (isEmployed(workers, villager.getUuid())
                    && ChestScanner.scan(world, villager, storages).isPresent()) {

                storagesFound++;
            }
        }

        return new ScanResult(registered, storagesFound, Set.copyOf(employable));
    }

    /**
     * Se este aldeão tem função na colônia.
     *
     * <p>A função vem de um passo posterior a esta varredura, então quem
     * acabou de ser registrado só reivindica baú no ciclo seguinte. Um
     * ciclo de atraso é barato; deixar os quarenta desempregados
     * reivindicarem primeiro não era.
     */
    private static boolean isEmployed(WorkerService workers, UUID villagerId) {
        return workers.find(villagerId).filter(Worker::hasProfession).isPresent();
    }

    /**
     * Se este aldeão pode receber uma função de colônia.
     *
     * <p>Bebê não trabalha. Ele é registrado — a colônia não perde nada
     * ao já conhecê-lo — mas dar-lhe um machado seria absurdo em jogo, e
     * ele ainda ocuparia a vaga de lenhador que um adulto deveria ter.
     * Profession-System.md §"Nascimento de Novos Aldeões" é explícito:
     * a função vem quando surge um aldeão <em>adulto</em>. Ao crescer,
     * ele passa a ser elegível sozinho, no ciclo seguinte.
     *
     * <p>Nitwit também não. O Vanilla nunca lhe dá emprego, e o jogador
     * que reconhece o casaco verde espera que ele continue inútil. Uma
     * colônia que o pusesse a construir contrariaria a expectativa que o
     * próprio jogo criou — e o PROJECT_CONSTITUTION §4 manda respeitar
     * o comportamento Vanilla do aldeão.
     */
    private static boolean canWork(VillagerEntity villager) {
        return !villager.isBaby()
                && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT;
    }

    /**
     * O que uma varredura viu.
     *
     * <p>{@code employable} são os aldeões prontos para receber função —
     * não os que a têm. Vazio é comum: uma vila só de bebês existe.
     */
    public record ScanResult(
            int registeredWorkers, int registeredStorages, Set<UUID> employable) {

        public boolean changedNothing() {
            return registeredWorkers == 0 && registeredStorages == 0;
        }
    }
}
