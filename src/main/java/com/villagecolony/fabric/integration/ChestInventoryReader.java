package com.villagecolony.fabric.integration;

import com.villagecolony.core.resource.model.ColonyResources;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Conta o que há nos baús dos trabalhadores.
 *
 * <p>Só lê. Nada aqui move, retira ou reorganiza item — o baú é do
 * jogador tanto quanto do aldeão, e o MVP não mexe no seu conteúdo. Ver
 * Storage-System.md §"Capacidade de Armazenamento".
 *
 * <p>Preserva a identidade de todo item no estoque e mantém a contagem
 * tipada para os recursos que já participam das cadeias de trabalho.
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

        return inspectIn(chunk, position, Set.of()).resources();
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

    /**
     * A leitura em si, com o chunk já em mãos.
     *
     * <p>Quando o ciclo pede capacidade, a contagem e o espaço livre saem
     * da mesma passagem pelos slots. Isso não é cache entre ciclos: a cada
     * fotografia o inventário do mundo continua sendo lido de novo.
     */
    private static ChestContents inspectIn(
            WorldChunk chunk, BlockPos position, Set<ResourceGroup> capacityGroups) {
        if (!(chunk.getBlockEntity(position) instanceof ChestBlockEntity chest)) {
            return ChestContents.empty(capacityGroups);
        }

        Map<ResourceType, Integer> counts = new EnumMap<>(ResourceType.class);
        Map<ResourceId, Integer> idCounts = new LinkedHashMap<>();
        Map<ResourceGroup, Integer> freeSpace = emptyCapacityFor(capacityGroups);

        for (int slot = 0; slot < chest.size(); slot++) {
            ItemStack stack = chest.getStack(slot);

            if (stack.isEmpty()) {
                for (Map.Entry<ResourceGroup, Integer> entry : freeSpace.entrySet()) {
                    entry.setValue(entry.getValue() + emptySlotCapacity());
                }
                continue;
            }

            idCounts.merge(
                    MinecraftTypeAdapter.toResourceId(stack.getItem()),
                    stack.getCount(),
                    Integer::sum);
            MinecraftTypeAdapter.toResourceType(stack.getItem()).ifPresent(type -> {
                counts.merge(type, stack.getCount(), Integer::sum);
                if (freeSpace.containsKey(type.group())) {
                    freeSpace.merge(
                            type.group(),
                            stack.getMaxCount() - stack.getCount(),
                            Integer::sum);
                }
            });
        }

        return new ChestContents(ResourceTally.of(counts, idCounts), freeSpace);
    }

    private static Map<ResourceGroup, Integer> emptyCapacityFor(
            Set<ResourceGroup> capacityGroups) {

        Map<ResourceGroup, Integer> freeSpace = new EnumMap<>(ResourceGroup.class);

        for (ResourceGroup group : capacityGroups) {
            freeSpace.put(group, 0);
        }

        return freeSpace;
    }

    /** A capacidade de um slot vazio para os recursos físicos da colônia. */
    private static int emptySlotCapacity() {
        return Items.OAK_LOG.getDefaultStack().getMaxCount();
    }

    private record ChestContents(
            ResourceTally resources, Map<ResourceGroup, Integer> freeSpaceByGroup) {

        private static ChestContents empty(Set<ResourceGroup> capacityGroups) {
            return new ChestContents(ResourceTally.empty(), emptyCapacityFor(capacityGroups));
        }
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
     * @param freeSpaceByGroup espaço dos grupos pedidos para esta fotografia
     * @param chestsRead  baús alcançados, incluindo os que estavam vazios
     * @param chestsUnreachable baús registrados cujo chunk não está carregado
     */
    public record ChestSurvey(
            ColonyResources resources,
            Map<ResourceGroup, Integer> freeSpaceByGroup,
            int chestsRead,
            int chestsUnreachable) {

        public ChestSurvey {
            freeSpaceByGroup = Map.copyOf(freeSpaceByGroup);
        }

        /** Construtor mantido para fotografias que só precisam do estoque. */
        public ChestSurvey(ColonyResources resources, int chestsRead, int chestsUnreachable) {
            this(resources, Map.of(), chestsRead, chestsUnreachable);
        }

        /**
         * Espaço do grupo pedido para esta fotografia.
         *
         * <p>Grupo que não foi solicitado na varredura não tem espaço
         * calculado e devolve zero; quem decide uma meta deve pedi-lo em
         * {@link ChestInventoryReader#survey(ServerWorld, List, ResourceGroup...)}.
         */
        public int freeSpaceForGroup(ResourceGroup group) {
            return freeSpaceByGroup.getOrDefault(group, 0);
        }

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
        return survey(world, chests, new ResourceGroup[0]);
    }

    /**
     * Fotografia de estoque e espaço livre para os grupos que vão decidir
     * neste ciclo.
     *
     * <p>Os grupos são explícitos para não transformar uma otimização de
     * duas metas em nova varredura para toda categoria conhecida.
     */
    public static ChestSurvey survey(
            ServerWorld world, List<ColonyPos> chests, ResourceGroup... capacityGroups) {

        Set<ResourceGroup> requestedGroups = EnumSet.noneOf(ResourceGroup.class);

        for (ResourceGroup group : capacityGroups) {
            requestedGroups.add(group);
        }

        Map<ColonyPos, ResourceTally> byChest = new LinkedHashMap<>();
        Map<ResourceGroup, Integer> freeSpace = emptyCapacityFor(requestedGroups);
        int unreachable = 0;

        for (ColonyPos position : chests) {
            BlockPos blockPos = MinecraftTypeAdapter.toBlockPos(position);
            WorldChunk chunk = chunkAt(world, blockPos);

            if (chunk == null) {
                unreachable++;
                continue;
            }

            ChestContents contents = inspectIn(chunk, blockPos, requestedGroups);
            byChest.put(position, contents.resources());

            for (Map.Entry<ResourceGroup, Integer> entry : contents.freeSpaceByGroup().entrySet()) {
                freeSpace.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return new ChestSurvey(
                ColonyResources.of(byChest), freeSpace, byChest.size(), unreachable);
    }
}
