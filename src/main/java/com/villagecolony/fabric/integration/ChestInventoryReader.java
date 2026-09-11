package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Conta o que há nos baús dos trabalhadores.
 *
 * <p>Só lê. Nada aqui move, retira ou reorganiza item — o baú é do
 * jogador tanto quanto do aldeão, e o MVP não mexe no seu conteúdo. Ver
 * Storage-System.md §"Capacidade de Armazenamento".
 *
 * <p>Conta apenas os três recursos que a colônia acompanha; o resto do
 * inventário é ignorado, não apagado. Ver {@link ResourceType}.
 */
public final class ChestInventoryReader {

    private ChestInventoryReader() {
    }

    /**
     * O que há num baú.
     *
     * <p>Devolve vazio, e não erro, quando não há baú na posição: o
     * jogador pode tê-lo quebrado entre o registro e a leitura, e isso
     * é o "Storage Missing" de Storage-System.md §"Falhas", não uma
     * falha do mod.
     *
     * <p>Baú duplo conta só a metade registrada. Cada metade é uma block
     * entity com posição própria, e é uma delas que o trabalhador
     * reivindicou. Contar as duas faria a colônia enxergar o dobro
     * quando o outro lado fosse reivindicado por outro aldeão.
     *
     * <p>Chunk não carregado é pulado sem forçar carregamento, como em
     * {@link ChestScanner#findFreeChest} e pela mesma regra — ADR-002
     * §"o mod não segura chunk". Aqui a regra é mais que economia:
     * {@code World.getBlockEntity} carrega o chunk que faltar, e chamá-lo
     * de dentro do evento de carga de chunk trava a thread do servidor,
     * que passa a esperar por um chunk que só ela poderia produzir. Ver
     * §15, entrada de 2026-08-07.
     */
    public static ResourceTally read(ServerWorld world, BlockPos position) {
        WorldChunk chunk = chunkAt(world, position);

        if (chunk == null) {
            return ResourceTally.empty();
        }

        return readIn(chunk, position);
    }

    /**
     * O chunk de uma posição, ou {@code null} se ele não estiver
     * carregado.
     *
     * <p>Nunca força o carregamento. Ver a nota de {@link #read}: forçar
     * daqui trava a thread do servidor.
     */
    private static WorldChunk chunkAt(ServerWorld world, BlockPos position) {
        return world.getChunkManager()
                .getWorldChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    /** A leitura em si, com o chunk já em mãos. */
    private static ResourceTally readIn(WorldChunk chunk, BlockPos position) {
        if (!(chunk.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            return ResourceTally.empty();
        }

        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);

        for (int slot = 0; slot < chest.size(); slot++) {
            ItemStack stack = chest.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            MinecraftTypeAdapter.toResourceType(stack.getItem()).ifPresent(
                    type -> counts.merge(type, stack.getCount(), Integer::sum));
        }

        return ResourceTally.of(counts);
    }

    /** O que há no baú de um trabalhador, se ele tiver um. */
    public static ResourceTally readOf(
            ServerWorld world, UUID workerId, StorageRegistry storages) {

        return storages.of(workerId)
                .map(storage -> read(
                        world, MinecraftTypeAdapter.toBlockPos(storage.chestPosition())))
                .orElseGet(ResourceTally::empty);
    }

    /**
     * A soma dos baús de vários trabalhadores.
     *
     * <p>É a "visão agregada" de Resource-System.md §"Registro de
     * Recursos", calculada na hora a partir dos baús.
     *
     * <p>Calculada, e não guardada: o jogador pode tirar madeira do baú
     * a qualquer momento, e um total em cache estaria errado sem que
     * nada avisasse. Enquanto a contagem for barata — um punhado de
     * baús, dezenas de slots — vale pagar por ela.
     */
    public static ResourceTally readAll(
            ServerWorld world, Iterable<UUID> workerIds, StorageRegistry storages) {

        ResourceTally total = ResourceTally.empty();

        for (UUID workerId : workerIds) {
            total = total.plus(readOf(world, workerId, storages));
        }

        return total;
    }

    /**
     * O resultado de uma varredura de baús, com o que ela não conseguiu
     * olhar.
     *
     * <p>Existe porque {@link ColonyResources} sozinho não sabe dizer a
     * diferença entre um baú vazio e um baú que a colônia não pôde ler:
     * os dois somem da agregação. Enquanto a leitura forçava o
     * carregamento do chunk essa diferença não existia — todo baú
     * registrado era legível. Depois da correção de {@link #read} ela
     * passou a existir, e um baú fora de alcance vira estoque a menos
     * sem nada avisando.
     *
     * <p>É o risco que o V5 do §7 já apontava, agora com nome: nesta
     * camada o defeito aparece como número plausível, não como ausência.
     *
     * @param resources   o que foi lido, sem os baús vazios
     * @param chestsRead  baús alcançados, incluindo os que estavam vazios
     * @param chestsUnreachable baús registrados cujo chunk não está carregado
     */
    public record ChestSurvey(
            ColonyResources resources, int chestsRead, int chestsUnreachable) {

        /** Se a contagem está incompleta, e por isso não vale confiar nela. */
        public boolean isPartial() {
            return chestsUnreachable > 0;
        }

        /**
         * Quantos baús a colônia conhece: os lidos mais os que o chunk
         * não entregou.
         */
        public int chestsKnown() {
            return chestsRead + chestsUnreachable;
        }

        /**
         * Quantos dos baús lidos tinham alguma coisa dentro.
         *
         * <p>Baú vazio é lido e não entra aqui: a agregação descarta a
         * conta zerada. Quem quiser saber se a varredura <b>alcançou</b>
         * o baú pergunta a {@link #chestsRead()}, e são perguntas
         * diferentes — ver {@link #coverage()}.
         */
        public int chestsWithItems() {
            return resources.byChest().size();
        }

        /**
         * A cobertura desta varredura, numa frase que não se lê ao
         * contrário.
         *
         * <p><b>Existe por causa de um defeito de leitura que custou um
         * bloqueador inteiro.</b> A linha antiga saía como
         * {@code "stores {...} in 1 of 8 chests read"}, e os dois números
         * eram <i>baús com conteúdo</i> e <i>baús lidos</i>. Em 2026-09-11
         * ela foi lida como cobertura — <i>"a colônia lê 1 de 8 baús"</i> —
         * e virou o P0.2 do plano de correção, com varredura em fila,
         * cache por evento e relatório de scan pendurados numa premissa
         * que o log nunca afirmou. A varredura tinha lido os oito.
         *
         * <p>É o mesmo <i>defeito-que-parece-número</i> do V5 que o
         * javadoc de {@link ChestSurvey} nomeia, cometido do lado de fora:
         * não no que a varredura mede, mas no que a frase deixa concluir.
         *
         * <p>Então a forma {@code "X of Y chests read"} fica <b>reservada
         * para cobertura de verdade</b>, e só aparece quando algum baú
         * ficou sem ser lido. Varredura completa não tem "de": diz
         * {@code "8 chests read, 1 with items"}, e não há como ler isso
         * como oito baús dos quais um foi alcançado.
         */
        public String coverage() {
            String read = isPartial()
                    ? chestsRead + " of " + chestsKnown() + " chests read ("
                            + chestsUnreachable + " in unloaded chunks)"
                    : chestsRead + " chests read";

            return read + ", " + chestsWithItems() + " with items";
        }
    }

    /**
     * O estoque de uma colônia, dizendo também o que ficou fora do
     * alcance.
     *
     * <p>Preferir a {@link #readColony} quando a resposta for usada para
     * decidir alguma coisa: uma colônia que conclui "falta madeira"
     * porque metade dos baús estava descarregada mandaria um trabalhador
     * buscar o que ela já tem.
     *
     * <p><b>Recebe a lista pronta desde 2026-09-11</b>, e não mais os
     * trabalhadores — P0.3. Montá-la aqui, a partir do registro de
     * trabalhadores, era o que deixava o baú da boca da mina fora da
     * conta: ele não é registro de ninguém. Quem responde onde estão os
     * baús de uma colônia é o {@link ColonyChests}, num lugar só, para
     * que a conta e quem consome dela nunca olhem conjuntos diferentes.
     *
     * @param chests os baús da colônia, de {@code ColonyChests}
     */
    public static ChestSurvey survey(ServerWorld world, List<ColonyPos> chests) {
        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        int unreachable = 0;

        for (ColonyPos position : chests) {
            BlockPos blockPos = MinecraftTypeAdapter.toBlockPos(position);
            WorldChunk chunk = chunkAt(world, blockPos);

            if (chunk == null) {
                unreachable++;
                continue;
            }

            byChest.put(position, readIn(chunk, blockPos));
        }

        return new ChestSurvey(ColonyResources.of(byChest), byChest.size(), unreachable);
    }

    /** Atalho para somar tudo o que está registrado. */
    public static ResourceTally readEverything(ServerWorld world, StorageRegistry storages) {
        ResourceTally total = ResourceTally.empty();

        for (WorkerStorage storage : storages.all()) {
            total = total.plus(
                    read(world, MinecraftTypeAdapter.toBlockPos(storage.chestPosition())));
        }

        return total;
    }
}
