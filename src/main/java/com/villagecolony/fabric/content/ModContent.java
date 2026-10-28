package com.villagecolony.fabric.content;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
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
 * <p>Uma bancada por profissão, que o <b>jogador</b> fabrica para dobrar a produção daquele
 * ofício ({@link com.villagecolony.fabric.work.ModBenchBonus}) e que funciona para ele como a
 * estação Vanilla do ofício: mesa de trabalho (carpinteiro, e os sem equivalente: lenhador,
 * construtor, mineiro), cortador de pedra (pedreiro), fornalha (fundidor), tear (pastor) e
 * composteira (fazendeiro) — decisão do autor, 2026-10-20. Emitem luz ({@link #LIGHT}). A colônia
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

    /**
     * Luz que as bancadas do mod emitem — nível 14, o da tocha (decisão do autor, 2026-10-15).
     * Acima do limiar que impede o spawn de mobs hostis (luz de bloco ≥ 1 já basta no 1.21, mas 14
     * ilumina o ambiente como uma tocha e mantém a área segura).
     */
    public static final int LIGHT = 14;

    /** Até onde o POI de bancada é visível/buscável — a bancada fica na vila, perto do ofício. */
    private static final int POI_SEARCH_DISTANCE = 64;

    /** Um titular por bancada de cada vez. */
    private static final int POI_TICKET_COUNT = 1;

    // Cada bancada funciona como a estação Vanilla do seu ofício, e as sem equivalente como a mesa de
    // trabalho — decisão do autor, 2026-10-20. Todas são cubos cheios. As telas são próprias
    // (BenchCraftingBlock e irmãos): as do Vanilla só ficam abertas diante do bloco Vanilla exato.
    public static final Block CARPENTER_BENCH =
            new BenchCraftingBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).luminance(state -> LIGHT));

    public static final Block MASON_CUTTER =
            new BenchStonecutterBlock(AbstractBlock.Settings.copy(Blocks.STONECUTTER).luminance(state -> LIGHT));

    public static final Block BLACKSMITH_FORGE =
            new BenchForgeBlock(AbstractBlock.Settings.copy(Blocks.FURNACE).luminance(state -> LIGHT));

    public static final Block LUMBERJACK_BENCH =
            new BenchCraftingBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).luminance(state -> LIGHT));

    public static final Block FARMER_BENCH =
            new BenchComposterBlock(AbstractBlock.Settings.copy(Blocks.COMPOSTER).luminance(state -> LIGHT));

    public static final Block SHEPHERD_BENCH =
            new BenchLoomBlock(AbstractBlock.Settings.copy(Blocks.LOOM).luminance(state -> LIGHT));

    public static final Block BUILDER_BENCH =
            new BenchCraftingBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).luminance(state -> LIGHT));

    public static final Block MINER_BENCH =
            new BenchCraftingBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).luminance(state -> LIGHT));

    /** O caminho de areia, exclusivo da vila do deserto — autor, 2026-10-22 ({@link SandPathBlock}). */
    public static final Block SAND_PATH =
            new SandPathBlock(AbstractBlock.Settings.copy(Blocks.DIRT_PATH).mapColor(net.minecraft.block.MapColor.PALE_YELLOW));

    /** A tocha da colônia — Regra 127 ({@link ColonyTorchBlock}): luz 15, chama laranja. */
    public static final Block COLONY_TORCH = new ColonyTorchBlock(
            AbstractBlock.Settings.copy(Blocks.TORCH).luminance(state -> 15));

    /** A mesma tocha presa na parede: o par TORCH/WALL_TORCH do jogo, com um item só. */
    public static final Block COLONY_WALL_TORCH = new ColonyWallTorchBlock(
            AbstractBlock.Settings.copy(Blocks.WALL_TORCH).luminance(state -> 15).dropsLike(COLONY_TORCH));

    private static RegistryKey<PointOfInterestType> colonyTorchPoi;

    private static BlockEntityType<BenchForgeBlockEntity> forgeEntity;

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
        registerBlock("sand_path", SAND_PATH);
        registerTorch();

        // A pá faz o caminho de areia como faz o de terra (receita escolhida pelo autor, 2026-10-27).
        net.fabricmc.fabric.api.registry.FlattenableBlockRegistry.register(Blocks.SAND, SAND_PATH.getDefaultState());

        // A forja é fornalha de verdade: tipo de BlockEntity próprio (o FURNACE do Vanilla só aceita o
        // bloco Vanilla).
        forgeEntity = Registry.register(Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(VillageColonyMod.MOD_ID, "blacksmith_forge"),
                BlockEntityType.Builder.create(BenchForgeBlockEntity::new, BLACKSMITH_FORGE).build(null));

        carpenterPoi = registerPoi("carpenter_bench_poi", CARPENTER_BENCH);
        masonPoi = registerPoi("mason_cutter_poi", MASON_CUTTER);
        blacksmithPoi = registerPoi("blacksmith_forge_poi", BLACKSMITH_FORGE);
        lumberjackPoi = registerPoi("lumberjack_bench_poi", LUMBERJACK_BENCH);
        farmerPoi = registerPoi("farmer_bench_poi", FARMER_BENCH);
        shepherdPoi = registerPoi("shepherd_bench_poi", SHEPHERD_BENCH);
        builderPoi = registerPoi("builder_bench_poi", BUILDER_BENCH);
        minerPoi = registerPoi("miner_bench_poi", MINER_BENCH);
        colonyTorchPoi = registerPoi("colony_torch_poi", COLONY_TORCH, COLONY_WALL_TORCH);

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

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries ->
                entries.addAfter(net.minecraft.item.Items.DIRT_PATH, SAND_PATH));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries ->
                entries.addAfter(net.minecraft.item.Items.TORCH, COLONY_TORCH));
    }

    /** O ponto de interesse da tocha da colônia: onde as tochas estão, para o bloqueio de monstros. */
    public static RegistryKey<PointOfInterestType> colonyTorchPoi() {
        return colonyTorchPoi;
    }

    /** O tipo de BlockEntity da forja do fundidor; registrado em {@link #register()}. */
    public static BlockEntityType<BenchForgeBlockEntity> forgeEntity() {
        return forgeEntity;
    }

    private static void registerBlock(String path, Block block) {
        Identifier id = Identifier.of(VillageColonyMod.MOD_ID, path);

        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
    }

    /** A tocha de chão e a de parede, com o item que escolhe entre as duas pela face clicada, como a do jogo. */
    private static void registerTorch() {
        Registry.register(Registries.BLOCK, Identifier.of(VillageColonyMod.MOD_ID, "colony_torch"), COLONY_TORCH);
        Registry.register(Registries.BLOCK, Identifier.of(VillageColonyMod.MOD_ID, "colony_wall_torch"), COLONY_WALL_TORCH);
        Registry.register(Registries.ITEM, Identifier.of(VillageColonyMod.MOD_ID, "colony_torch"),
                new net.minecraft.item.VerticallyAttachableBlockItem(COLONY_TORCH, COLONY_WALL_TORCH,
                        new Item.Settings(), net.minecraft.util.math.Direction.DOWN));
    }

    private static RegistryKey<PointOfInterestType> registerPoi(String path, Block... blocks) {
        Identifier id = Identifier.of(VillageColonyMod.MOD_ID, path);

        PointOfInterestHelper.register(id, POI_TICKET_COUNT, POI_SEARCH_DISTANCE, blocks);

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
