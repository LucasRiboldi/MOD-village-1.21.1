package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * A ferramenta chega à mão de quem tem função.
 *
 * <p>Profession-System.md manda entregá-la desde a Fase 4, e até
 * 2026-08-13 nada entregava: {@code ToolType} existia, a profissão o
 * declarava, e faltava a conversão para item.
 *
 * <p>Mora no gametest e não no teste de unidade porque é fronteira: o
 * Core não conhece {@code ItemStack}, e todos os defeitos sérios deste
 * projeto moraram exatamente aqui. Ver Project-State §11.
 *
 * <p>O que ele <b>não</b> prova: que alguém veja a ferramenta. O modelo
 * de aldeão do Vanilla não tem braço que segure item — ver
 * {@code WorkerEquipment}.
 */
public class WorkerEquipmentGameTest implements FabricGameTest {

    private static final int BEDS = VillageDetector.MIN_BEDS;

    /**
     * Cada trabalhador segura a ferramenta da profissão dele — e quem
     * trabalha de mãos livres continua de mãos livres.
     *
     * <p>Afirma por profissão, e não "existe um lenhador com machado":
     * quais funções são distribuídas depende da ordem de preenchimento
     * das vagas, e um teste que dependesse dela quebraria no dia em que a
     * Regra 4 mudasse de número.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void everyWorkerHoldsTheToolOfItsProfession(TestContext context) {
        BlockPos anchor = new BlockPos(1, 1, 1);

        placeBeds(context, anchor, BEDS);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        Colony colony = colonyOf(context, anchor).orElseThrow(
                () -> new AssertionError("nenhuma colônia nasceu destas camas"));

        int checked = 0;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            Optional<ProfessionType> profession = worker.profession();

            if (profession.isEmpty()) {
                continue;
            }

            if (!(context.getWorld().getEntity(worker.villagerId())
                    instanceof VillagerEntity villager)) {

                continue;
            }

            Optional<Item> expected = MinecraftTypeAdapter.toItem(
                    ProfessionRegistry.of(profession.get()).requiredTool());

            ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

            if (expected.isEmpty()) {
                context.assertTrue(
                        held.isEmpty(),
                        profession.get() + " trabalha de mãos livres e está segurando " + held);
            } else {
                context.assertTrue(
                        held.isOf(expected.get()),
                        profession.get() + " deveria segurar " + expected.get()
                                + " e segura " + (held.isEmpty() ? "nada" : held.getItem()));
            }

            checked++;
        }

        context.assertTrue(
                checked > 0,
                "nenhum trabalhador com profissão para conferir — a atribuição não rodou");

        forget(context, anchor);

        context.complete();
    }

    /**
     * A ferramenta não vira fonte de itens.
     *
     * <p>Ela é criada do nada, não sai de baú nem de receita. Se caísse
     * com a morte do aldeão, matar trabalhadores seria uma forma de
     * colher machados.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theToolNeverDrops(TestContext context) {
        BlockPos anchor = new BlockPos(1, 1, 1);

        placeBeds(context, anchor, BEDS);
        spawnVillagers(context, anchor, VillageDetector.MIN_VILLAGERS);

        runCycle(context, anchor);

        Colony colony = colonyOf(context, anchor).orElseThrow(
                () -> new AssertionError("nenhuma colônia nasceu destas camas"));

        VillagerEntity armed = null;
        Item tool = null;

        for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
            if (!(context.getWorld().getEntity(worker.villagerId())
                    instanceof VillagerEntity villager)) {

                continue;
            }

            ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

            if (!held.isEmpty()) {
                armed = villager;
                tool = held.getItem();

                break;
            }
        }

        context.assertTrue(
                armed != null,
                "nenhum trabalhador armado para matar — o teste não afirmaria nada");

        // Pela morte, e não pela chance de queda: o número é protegido em
        // MobEntity, e o que importa não é o campo e sim o que sobra no
        // chão. Um mod que devolvesse a ferramenta por outro caminho
        // passaria por uma leitura de campo e não passa por esta.
        Vec3d where = armed.getPos();

        armed.kill();

        Box around = new Box(where, where).expand(DROP_SEARCH);

        for (ItemEntity dropped : context.getWorld()
                .getEntitiesByClass(ItemEntity.class, around, entity -> true)) {

            context.assertTrue(
                    !dropped.getStack().isOf(tool),
                    "a ferramenta caiu no chão com a morte do trabalhador — a colônia"
                            + " virou fonte de " + tool);
        }

        forget(context, anchor);

        context.complete();
    }

    /** Quanto olhar em volta do corpo, à procura do que caiu. */
    private static final double DROP_SEARCH = 3.0;

    // ----------------------------------------------------------------
    //
    // O que não mora aqui: a colônia abandonada, do item A da mesma
    // sessão. A regra precisa que a sonda não ache vila alguma ao redor
    // do centro, e no mundo do gametest as estruturas vizinhas ficam a
    // menos de 64 blocos — as camas delas entram no raio e a vila deste
    // teste nunca some. É o mesmo motivo pelo qual "camas de menos não
    // são vila" e o encolhimento não moram no gametest; ver os
    // comentários de ColonyDetectionGameTest.
    //
    // A regra está coberta onde ela cabe: ColonyAbandonmentTest, no Core.

    private static Optional<Colony> colonyOf(TestContext context, BlockPos anchor) {
        return VillageColonyMod.COLONIES.findNearest(
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(anchor)),
                VillageDetector.DUPLICATE_DISTANCE);
    }

    private static void forget(TestContext context, BlockPos anchor) {
        colonyOf(context, anchor).ifPresent(colony -> {
            ColonyFixture owned = ColonyFixture.create().owning(colony);

            for (Worker worker : VillageColonyMod.WORKERS.ofColony(colony.id())) {
                owned.owning(worker.villagerId());
            }

            owned.cleanUp();
        });
    }

    private static void placeBeds(TestContext context, BlockPos anchor, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos head = anchor.add((i % 4) * 2, 0, (i / 4) * 3);

            context.setBlockState(head, Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.HEAD)
                    .with(BedBlock.FACING, Direction.NORTH));

            context.setBlockState(head.offset(Direction.SOUTH), Blocks.WHITE_BED.getDefaultState()
                    .with(BedBlock.PART, BedPart.FOOT)
                    .with(BedBlock.FACING, Direction.NORTH));
        }
    }

    private static void spawnVillagers(TestContext context, BlockPos anchor, int count) {
        for (int i = 0; i < count; i++) {
            VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, anchor.add(i, 0, 2));

            villager.setBreedingAge(0);
        }
    }

    private static void runCycle(TestContext context, BlockPos anchor) {
        ServerWorld world = context.getWorld();

        VillageDetectionHandler.runCycleNow(world, context.getAbsolutePos(anchor));
    }
}
