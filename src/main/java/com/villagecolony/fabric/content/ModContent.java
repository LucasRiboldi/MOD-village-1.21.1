package com.villagecolony.fabric.content;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.StonecutterBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Map;

/**
 * As bancadas exclusivas do mod — decisão do autor, 2026-10-13.
 *
 * <p>Réplicas funcionais das estações Vanilla de três profissões (carpinteiro,
 * pedreiro, ferreiro/fundidor) que o <b>jogador</b> fabrica para dobrar a produção
 * daquele ofício ({@link ModBenchBonus}). Emitem luz ({@link #LIGHT}). A colônia
 * nunca as fabrica nem coloca: não entram em nenhuma cadeia de produção.
 *
 * <p>Cada bancada é um POI próprio do mod ({@link PointOfInterestHelper}), <b>sem</b>
 * {@code VillagerProfession} associada — o Vanilla não emprega nela e a detecção de
 * vila (por POI de cama e sino) não é afetada. O fluxo do mod é quem a reivindica
 * ({@link com.villagecolony.fabric.work.CraftStation}).
 *
 * <p>Primeira infra de conteúdo registrado do mod; registrada cedo em
 * {@link VillageColonyMod#onInitialize()}.
 */
public final class ModContent {

    /** Luz que as bancadas do mod emitem (decisão do autor: nível 10). */
    public static final int LIGHT = 10;

    /** Até onde o POI de bancada é visível/buscável — a bancada fica na vila, perto do ofício. */
    private static final int POI_SEARCH_DISTANCE = 64;

    /** Um titular por bancada de cada vez. */
    private static final int POI_TICKET_COUNT = 1;

    public static final Block CARPENTER_BENCH =
            new CraftingTableBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).luminance(state -> LIGHT));

    public static final Block MASON_CUTTER =
            new StonecutterBlock(AbstractBlock.Settings.copy(Blocks.STONECUTTER).luminance(state -> LIGHT));

    // A forja não funde de verdade (a fundição é do mod, não da fornalha física), então é um
    // bloco simples com a forma da fornalha — sem BlockEntity para acertar.
    public static final Block BLACKSMITH_FORGE =
            new Block(AbstractBlock.Settings.copy(Blocks.BLAST_FURNACE).luminance(state -> LIGHT));

    // As cinco bancadas restantes — uma por profissão (decisão do autor, 2026-10-15). Como a forja,
    // são blocos simples com a forma da estação do ofício: não executam nada sozinhos (o trabalho é
    // do fluxo do mod), só servem de POI e dobram a produção daquele ofício ({@link ModBenchBonus}).
    public static final Block LUMBERJACK_BENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).luminance(state -> LIGHT));

    public static final Block FARMER_BENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).luminance(state -> LIGHT));

    public static final Block SHEPHERD_BENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).luminance(state -> LIGHT));

    public static final Block BUILDER_BENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).luminance(state -> LIGHT));

    public static final Block MINER_BENCH =
            new Block(AbstractBlock.Settings.copy(Blocks.STONE).luminance(state -> LIGHT));

    private static RegistryKey<PointOfInterestType> carpenterPoi;
    private static RegistryKey<PointOfInterestType> masonPoi;
    private static RegistryKey<PointOfInterestType> blacksmithPoi;
    private static RegistryKey<PointOfInterestType> lumberjackPoi;
    private static RegistryKey<PointOfInterestType> farmerPoi;
    private static RegistryKey<PointOfInterestType> shepherdPoi;
    private static RegistryKey<PointOfInterestType> builderPoi;
    private static RegistryKey<PointOfInterestType> minerPoi;

    private ModContent() {
    }

    /** Registra blocos, itens, POIs e os põe na aba do criativo. Chamado uma vez, cedo, no init. */
    public static void register() {
        registerBlock("carpenter_bench", CARPENTER_BENCH);
        registerBlock("mason_cutter", MASON_CUTTER);
        registerBlock("blacksmith_forge", BLACKSMITH_FORGE);
        registerBlock("lumberjack_bench", LUMBERJACK_BENCH);
        registerBlock("farmer_bench", FARMER_BENCH);
        registerBlock("shepherd_bench", SHEPHERD_BENCH);
        registerBlock("builder_bench", BUILDER_BENCH);
        registerBlock("miner_bench", MINER_BENCH);

        carpenterPoi = registerPoi("carpenter_bench_poi", CARPENTER_BENCH);
        masonPoi = registerPoi("mason_cutter_poi", MASON_CUTTER);
        blacksmithPoi = registerPoi("blacksmith_forge_poi", BLACKSMITH_FORGE);
        lumberjackPoi = registerPoi("lumberjack_bench_poi", LUMBERJACK_BENCH);
        farmerPoi = registerPoi("farmer_bench_poi", FARMER_BENCH);
        shepherdPoi = registerPoi("shepherd_bench_poi", SHEPHERD_BENCH);
        builderPoi = registerPoi("builder_bench_poi", BUILDER_BENCH);
        minerPoi = registerPoi("miner_bench_poi", MINER_BENCH);

        // As bancadas na aba Blocos Funcionais do criativo, ao lado das estações Vanilla
        // (crafting table, stonecutter, fornalha) — decisão do autor, 2026-10-13.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(CARPENTER_BENCH);
            entries.add(MASON_CUTTER);
            entries.add(BLACKSMITH_FORGE);
            entries.add(LUMBERJACK_BENCH);
            entries.add(FARMER_BENCH);
            entries.add(SHEPHERD_BENCH);
            entries.add(BUILDER_BENCH);
            entries.add(MINER_BENCH);
        });
    }

    private static void registerBlock(String path, Block block) {
        Identifier id = Identifier.of(VillageColonyMod.MOD_ID, path);

        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
    }

    private static RegistryKey<PointOfInterestType> registerPoi(String path, Block block) {
        Identifier id = Identifier.of(VillageColonyMod.MOD_ID, path);

        PointOfInterestHelper.register(id, POI_TICKET_COUNT, POI_SEARCH_DISTANCE, block);

        return RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, id);
    }

    /** O bloco da bancada do mod desta profissão, se ela tem uma. */
    public static Block benchBlock(ProfessionType profession) {
        return switch (profession) {
            case CARPENTER -> CARPENTER_BENCH;
            case MASON -> MASON_CUTTER;
            case SMELTER -> BLACKSMITH_FORGE;
            case LUMBERJACK -> LUMBERJACK_BENCH;
            case FARMER -> FARMER_BENCH;
            case SHEPHERD -> SHEPHERD_BENCH;
            case BUILDER -> BUILDER_BENCH;
            case MINER -> MINER_BENCH;
        };
    }

    /** O POI da bancada do mod desta profissão, se ela tem uma. */
    public static RegistryKey<PointOfInterestType> benchPoi(ProfessionType profession) {
        return switch (profession) {
            case CARPENTER -> carpenterPoi;
            case MASON -> masonPoi;
            case SMELTER -> blacksmithPoi;
            case LUMBERJACK -> lumberjackPoi;
            case FARMER -> farmerPoi;
            case SHEPHERD -> shepherdPoi;
            case BUILDER -> builderPoi;
            case MINER -> minerPoi;
        };
    }

    /** As profissões que têm bancada do mod e o bloco de cada uma — todas as oito. */
    public static Map<ProfessionType, Block> benches() {
        return Map.of(
                ProfessionType.CARPENTER, CARPENTER_BENCH,
                ProfessionType.MASON, MASON_CUTTER,
                ProfessionType.SMELTER, BLACKSMITH_FORGE,
                ProfessionType.LUMBERJACK, LUMBERJACK_BENCH,
                ProfessionType.FARMER, FARMER_BENCH,
                ProfessionType.SHEPHERD, SHEPHERD_BENCH,
                ProfessionType.BUILDER, BUILDER_BENCH,
                ProfessionType.MINER, MINER_BENCH);
    }
}
