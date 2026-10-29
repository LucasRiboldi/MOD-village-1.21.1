package com.villagecolony.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.gen.StructureTerrainAdaptation;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.List;

/**
 * A vila do Minecraft nasce em terreno plano — autor, 2026-10-28, depois do jogo da 0.3.104 ("o smooth do terreno
 * não foi visto, vila criada no meio da montanha"), escolha "só suavizar na geração". O mod troca o
 * {@code terrain_adaptation} das cinco vilas de {@code beard_thin} para {@code beard_box}: o terreno é aterrado
 * embaixo e cortado em cima por toda a altura da caixa de cada peça ({@code StructureWeightSampler.sample}), como na
 * Cidade Antiga. Vale só para chunks gerados depois.
 */
public class VillageTerrainGameTest implements FabricGameTest {

    private static final List<RegistryKey<Structure>> VILLAGES = List.of(StructureKeys.VILLAGE_PLAINS,
            StructureKeys.VILLAGE_DESERT, StructureKeys.VILLAGE_SAVANNA, StructureKeys.VILLAGE_SNOWY,
            StructureKeys.VILLAGE_TAIGA);

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "village_terrain", tickLimit = 5)
    public void everyVillageFlattensTheGroundUnderItsPieces(TestContext context) {
        Registry<Structure> structures = context.getWorld().getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (RegistryKey<Structure> village : VILLAGES) {
            Structure structure = structures.get(village);

            context.assertTrue(structure != null, "a vila " + village.getValue() + " não está no registro");
            context.assertTrue(structure.getTerrainAdaptation() == StructureTerrainAdaptation.BEARD_BOX,
                    village.getValue() + " gera com " + structure.getTerrainAdaptation() + ", não beard_box");
        }

        context.complete();
    }
}
