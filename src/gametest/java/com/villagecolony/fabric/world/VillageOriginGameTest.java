package com.villagecolony.fabric.world;

import com.villagecolony.fabric.integration.VillageOrigin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.BuriedTreasureGenerator;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.FlatChunkGeneratorLayer;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/** Origem pertence ao start e sobrevive ao NBT sem marcar legado — ADR-043. */
public class VillageOriginGameTest implements FabricGameTest {
    private static final String KEY = "villagecolony:origin";

    /** Invoca o callsite Vanilla transformado, sem executar features ou escrever terreno — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 20)
    public void realTrySetStructureStartMarksOnlyTheCreatedStart(TestContext context)
            throws ReflectiveOperationException {
        var world = context.getWorld();
        var registries = world.getRegistryManager();
        var biomes = registries.get(RegistryKeys.BIOME);
        var config = new FlatChunkGeneratorConfig(Optional.empty(),
                biomes.getEntry(BiomeKeys.PLAINS).orElseThrow(), List.of());
        config.getLayers().add(new FlatChunkGeneratorLayer(1, Blocks.BEDROCK));
        config.getLayers().add(new FlatChunkGeneratorLayer(3, Blocks.DIRT));
        config.updateLayerBlocks();
        var generator = new FlatChunkGenerator(config);
        ChunkPos pos = new ChunkPos(0, 0);
        var chunk = new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, world, biomes, null);
        var structures = registries.get(RegistryKeys.STRUCTURE);
        var village = structures.getEntry(structures.get(Identifier.ofVanilla("village_plains")));
        var noise = world.getChunkManager().getNoiseConfig();
        var templates = world.getStructureTemplateManager();

        // Controle: criar peças fora do hook não fornece prova de origem — ADR-043.
        StructureStart control = village.value().createStructureStart(registries, generator,
                generator.getBiomeSource(), noise, templates, 0L, pos, 0, chunk,
                village.value().getValidBiomes()::contains);
        context.assertTrue(control.hasChildren(), "fixture Vanilla não gerou vila válida");
        context.assertFalse(VillageOrigin.isCurrent(control), "criação fora do hook marcou origem");

        var method = ChunkGenerator.class.getDeclaredMethod("trySetStructureStart",
                StructureSet.WeightedEntry.class, StructureAccessor.class, DynamicRegistryManager.class,
                NoiseConfig.class, StructureTemplateManager.class, long.class, Chunk.class,
                ChunkPos.class, ChunkSectionPos.class);
        method.setAccessible(true);
        boolean created = (boolean) method.invoke(generator, StructureSet.createEntry(village),
                world.getStructureAccessor(), registries, noise, templates, 0L, chunk, pos,
                ChunkSectionPos.from(chunk));
        context.assertTrue(created, "trySetStructureStart não criou vila válida");
        StructureStart installed = chunk.getStructureStart(village.value());
        context.assertTrue(installed != null && installed.hasChildren(), "hook não instalou start no holder");
        context.assertTrue(VillageOrigin.isCurrent(installed), "callsite real não marcou origem");
        NbtCompound encoded = installed.toNbt(StructureContext.from(world), pos);
        context.assertTrue(VillageOrigin.isCurrent(load(context, roundTrip(context, encoded))),
                "origem criada no hook não persistiu em duas leituras");
        context.complete();
    }

    /** Descoberta consulta start carregado do disco, sem produzir origem retroativa — ADR-043. */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void discoveringLegacyVillageDoesNotMarkIt(TestContext context) {
        var world = context.getWorld();
        NbtCompound encoded = fixture(context);
        StructureStart legacy = load(context, encoded);
        ChunkPos pos = legacy.getPos();
        var chunk = world.getChunk(pos.x, pos.z);
        var startsBefore = new HashMap<>(chunk.getStructureStarts());
        var referencesBefore = new HashMap<>(chunk.getStructureReferences());
        try {
            chunk.setStructureStart(legacy.getStructure(), legacy);
            var references = new HashMap<>(referencesBefore);
            var self = new LongOpenHashSet();
            self.add(pos.toLong());
            references.put(legacy.getStructure(), self);
            chunk.setStructureReferences(references);
            for (int attempt = 0; attempt < 2; attempt++) {
                context.assertTrue(GeneratedVillages.villagesReaching(world, pos.x, pos.z).contains(legacy),
                        "fixture legada não foi realmente descoberta");
                context.assertFalse(VillageOrigin.isCurrent(legacy), "descoberta marcou vila legada");
                context.assertTrue(encoded.equals(legacy.toNbt(StructureContext.from(world), pos)),
                        "descoberta alterou NBT legado");
            }
        } finally {
            chunk.setStructureStarts(startsBefore);
            chunk.setStructureReferences(referencesBefore);
        }
        context.complete();
    }

    private static NbtCompound fixture(TestContext context) {
        var world = context.getWorld();
        BlockPos position = context.getAbsolutePos(new BlockPos(2, 2, 2));
        var structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE)
                .get(Identifier.ofVanilla("village_plains"));
        StructureStart start = new StructureStart(structure, new ChunkPos(position), 0,
                new StructurePiecesList(List.of(new BuriedTreasureGenerator.Piece(position))));
        context.assertTrue(start.hasChildren(), "fixture sem peças");
        return start.toNbt(StructureContext.from(world), start.getPos());
    }

    private static NbtCompound roundTrip(TestContext context, NbtCompound encoded) {
        StructureStart loaded = load(context, encoded);
        return loaded.toNbt(StructureContext.from(context.getWorld()), loaded.getPos());
    }

    private static StructureStart load(TestContext context, NbtCompound encoded) {
        var world = context.getWorld();
        StructureContext structures = StructureContext.from(world);
        StructureStart loaded = StructureStart.fromNbt(structures, encoded, world.getSeed());
        context.assertTrue(loaded != null && loaded != StructureStart.DEFAULT && loaded.hasChildren(),
                "start válido perdido na leitura");
        return loaded;
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void legacyStartStaysUnmarked(TestContext context) {
        NbtCompound encoded = fixture(context);
        context.assertFalse(encoded.contains(KEY), "construtor marcou origem legada");
        context.assertFalse(roundTrip(context, encoded).contains(KEY), "leitura marcou origem legada");
        context.assertFalse(VillageOrigin.isCurrent(load(context, encoded)), "legado habilitou política nova");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void currentOriginSurvivesTwoRoundTrips(TestContext context) {
        NbtCompound encoded = fixture(context);
        NbtCompound origin = new NbtCompound();
        origin.putInt("version", 1);
        origin.putString("policy", "hierarchical_roads_v1");
        encoded.put(KEY, origin.copy());
        NbtCompound saved = roundTrip(context, roundTrip(context, encoded));
        context.assertTrue(origin.equals(saved.get(KEY)), "origem atual perdida ou alterada");
        context.assertTrue(VillageOrigin.isCurrent(load(context, saved)), "origem atual não reconhecida");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void futureOriginRemainsOpaque(TestContext context) {
        NbtCompound encoded = fixture(context);
        NbtCompound origin = new NbtCompound();
        origin.putInt("version", 999);
        origin.putString("policy", "future_policy");
        NbtCompound unknown = new NbtCompound();
        unknown.putIntArray("unknown", new int[]{4, 7, 11});
        origin.put("extra", unknown);
        encoded.put(KEY, origin.copy());
        NbtCompound saved = roundTrip(context, roundTrip(context, encoded));
        context.assertTrue(origin.equals(saved.get(KEY)), "payload futuro perdido ou interpretado");
        StructureStart loaded = load(context, saved);
        context.assertFalse(VillageOrigin.isCurrent(loaded), "origem futura habilitou política atual");
        VillageOrigin.markCreated(loaded, context.getWorld().getRegistryManager());
        context.assertTrue(origin.equals(((VillageOrigin) (Object) loaded).villagecolony$getOrigin()),
                "marcação repetida sobrescreveu payload futuro");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void createdVillageMarkerIsIdempotentAndCopiesPayload(TestContext context) {
        StructureStart start = load(context, fixture(context));
        VillageOrigin.markCreated(start, context.getWorld().getRegistryManager());
        context.assertTrue(VillageOrigin.isCurrent(start), "vila criada não marcada");
        VillageOrigin origin = (VillageOrigin) (Object) start;
        NbtCompound first = (NbtCompound) origin.villagecolony$getOrigin();
        VillageOrigin.markCreated(start, context.getWorld().getRegistryManager());
        context.assertTrue(first.equals(origin.villagecolony$getOrigin()), "marcação não idempotente");
        first.putInt("version", 999);
        context.assertTrue(VillageOrigin.isCurrent(start), "getter expôs payload mutável");
        origin.villagecolony$setOrigin(first);
        first.putInt("version", 1);
        context.assertFalse(VillageOrigin.isCurrent(start), "setter guardou alias mutável");
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_origin", tickLimit = 5)
    public void malformedOriginStaysOpaqueAndDisabled(TestContext context) {
        NbtCompound encoded = fixture(context);
        NbtCompound origin = new NbtCompound();
        origin.putString("version", "1");
        origin.putString("policy", "hierarchical_roads_v1");
        encoded.put(KEY, origin);
        NbtCompound saved = roundTrip(context, roundTrip(context, encoded));
        context.assertTrue(origin.equals(saved.get(KEY)), "payload inválido alterado");
        context.assertFalse(VillageOrigin.isCurrent(load(context, saved)), "tipo inválido habilitou política");
        encoded.putString(KEY, "opaque_future_format");
        saved = roundTrip(context, roundTrip(context, encoded));
        context.assertTrue(encoded.get(KEY).equals(saved.get(KEY)), "formato não composto perdido");
        context.assertFalse(VillageOrigin.isCurrent(load(context, saved)), "formato inválido habilitou política");
        context.complete();
    }
}
