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

    private static RegistryKey<PointOfInterestType> carpenterPoi;
    private static RegistryKey<PointOfInterestType> masonPoi;
    private static RegistryKey<PointOfInterestType> blacksmithPoi;

    private ModContent() {
    }

    /** Registra blocos, itens, POIs e os põe na aba do criativo. Chamado uma vez, cedo, no init. */
    public static void register() {
        registerBlock("carpenter_bench", CARPENTER_BENCH);
        registerBlock("mason_cutter", MASON_CUTTER);
        registerBlock("blacksmith_forge", BLACKSMITH_FORGE);

        carpenterPoi = registerPoi("carpenter_bench_poi", CARPENTER_BENCH);
        masonPoi = registerPoi("mason_cutter_poi", MASON_CUTTER);
        blacksmithPoi = registerPoi("blacksmith_forge_poi", BLACKSMITH_FORGE);

        // As bancadas na aba Blocos Funcionais do criativo, ao lado das estações Vanilla
        // (crafting table, stonecutter, fornalha) — decisão do autor, 2026-10-13.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(CARPENTER_BENCH);
            entries.add(MASON_CUTTER);
            entries.add(BLACKSMITH_FORGE);
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
            default -> null;
        };
    }

    /** O POI da bancada do mod desta profissão, se ela tem uma. */
    public static RegistryKey<PointOfInterestType> benchPoi(ProfessionType profession) {
        return switch (profession) {
            case CARPENTER -> carpenterPoi;
            case MASON -> masonPoi;
            case SMELTER -> blacksmithPoi;
            default -> null;
        };
    }

    /** As profissões que têm bancada do mod e o bloco de cada uma. */
    public static Map<ProfessionType, Block> benches() {
        return Map.of(
                ProfessionType.CARPENTER, CARPENTER_BENCH,
                ProfessionType.MASON, MASON_CUTTER,
                ProfessionType.SMELTER, BLACKSMITH_FORGE);
    }
}
