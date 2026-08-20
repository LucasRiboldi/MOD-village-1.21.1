package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.Blueprint;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.ColonyHut;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.StructureBlueprintReader;
import com.villagecolony.fabric.work.HouseFurnishing;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * A mobília da Regra 21: cama, baú e lampião entram depois que a casa
 * está de pé, quando houver material.
 *
 * <p><b>Por que este arquivo nasceu.</b> A Regra 21 entrou em
 * 2026-08-19 sem nenhum teste do caminho que põe a peça — só do que
 * decide não segurar a obra por ela. Na sessão daquela noite a mesma
 * casa foi "mobiliada" oito vezes: o baú entrou sete, a cama quatro, o
 * lampião quatro, e cada repetição gastou uma peça do baú da colônia.
 */
public class HouseFurnishingGameTest implements FabricGameTest {

    /**
     * O baú da colônia, no alto e fora do caminho.
     *
     * <p>A casa de planície é 7x7 e ocupa a arena inteira no plano. O
     * baú sobe para não disputar posição com peça de mobília nenhuma —
     * nem com o chão que o teste monta.
     */
    private static final BlockPos CHEST = new BlockPos(6, 5, 6);

    /** O canto da casa. A de planície vai de 0 a 6, que é toda a arena. */
    private static final BlockPos HOUSE = new BlockPos(0, 2, 0);

    /**
     * A mobília entra uma vez, e a passagem seguinte não gasta mais nada.
     *
     * <p>É o defeito da sessão de 2026-08-19, 23:41: as três peças
     * repostas no mesmo ciclo, na mesma casa, depois de já terem
     * entrado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_furnishing")
    public void theFurnitureGoesInOnceAndStaysIn(TestContext context) {
        Fixture fixture = setUp(context);

        HouseFurnishing.run(context.getWorld(), fixture.colony);

        int chestsAfterFirst = countIn(context, fixture.chest, Items.CHEST);
        int bedsAfterFirst = countIn(context, fixture.chest, Items.WHITE_BED);
        int lanternsAfterFirst = countIn(context, fixture.chest, Items.LANTERN);

        context.assertTrue(
                chestsAfterFirst == 2 && bedsAfterFirst == 2 && lanternsAfterFirst == 2,
                "a primeira passagem devia gastar uma de cada, e gastou "
                        + (3 - chestsAfterFirst) + " baús, "
                        + (3 - bedsAfterFirst) + " camas, "
                        + (3 - lanternsAfterFirst) + " lampiões");

        HouseFurnishing.run(context.getWorld(), fixture.colony);

        context.assertTrue(
                countIn(context, fixture.chest, Items.CHEST) == chestsAfterFirst,
                "a segunda passagem repôs o baú que já estava lá");

        context.assertTrue(
                countIn(context, fixture.chest, Items.WHITE_BED) == bedsAfterFirst,
                "a segunda passagem repôs a cama que já estava lá");

        context.assertTrue(
                countIn(context, fixture.chest, Items.LANTERN) == lanternsAfterFirst,
                "a segunda passagem repôs o lampião que já estava lá");

        fixture.owned.cleanUp();

        VillageColonyMod.BUILDINGS.removeOfColony(fixture.colony.id());


        context.complete();
    }

    /**
     * Peça que sumiu não é reposta a cada ciclo.
     *
     * <p><b>O defeito, e por que o teste não pergunta a causa.</b> Na
     * sessão de 2026-08-19 a mesma casa recebeu baú sete vezes, cama
     * quatro e lampião quatro — e às 23:41:05 as três de uma vez, tendo
     * as três entrado antes. O que tirou os blocos do mundo não está no
     * log e não é do mod: creeper, o jogador com uma picareta, qualquer
     * coisa. Este teste não tenta adivinhar — ele tira as três peças e
     * afirma o que o mod deve fazer em seguida.
     *
     * <p>Repor sem limite custa uma peça do baú a cada trinta segundos,
     * para sempre, e sem uma linha dizendo por quê. E quando quem tirou
     * foi o jogador, repor é escrever por cima da escolha dele — a
     * Regra 3.
     *
     * <p><b>E a marca não vence.</b> Regra do autor, 2026-08-20, depois
     * de ver a mobília voltar sozinha: peça destruída não volta. A
     * primeira versão desta correção deixava a marca envelhecer em dez
     * ciclos, como a recusa da Regra 23 — e cinco minutos depois a cama
     * reaparecia na casa de quem a tinha desfeito.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_furnishing_gone")
    public void aPieceThatVanishesIsNotReplacedEveryCycle(TestContext context) {
        Fixture fixture = setUp(context);

        HouseFurnishing.run(context.getWorld(), fixture.colony);

        int chests = countIn(context, fixture.chest, Items.CHEST);
        int beds = countIn(context, fixture.chest, Items.WHITE_BED);
        int lanterns = countIn(context, fixture.chest, Items.LANTERN);

        // Alguém tirou as três. Quem, o mod não sabe — e não precisa.
        for (BlueprintBlock piece : ColonyHut.furnishings()) {
            context.setBlockState(
                    HOUSE.add(piece.offset().x(), piece.offset().y(), piece.offset().z()),
                    Blocks.AIR.getDefaultState());
        }

        HouseFurnishing.run(context.getWorld(), fixture.colony);

        context.assertTrue(
                countIn(context, fixture.chest, Items.CHEST) == chests
                        && countIn(context, fixture.chest, Items.WHITE_BED) == beds
                        && countIn(context, fixture.chest, Items.LANTERN) == lanterns,
                "a colônia repôs a mobília que sumiu, e vai repor a cada ciclo para sempre");

        fixture.owned.cleanUp();

        VillageColonyMod.BUILDINGS.removeOfColony(fixture.colony.id());


        context.complete();
    }

    /**
     * A casa lida do jogo também recebe a mobília que lhe falta.
     *
     * <p><b>O buraco que este teste fecha.</b> A Regra 21 nasceu para a
     * cabana, e {@code run} pulava tudo que não fosse ela. O construtor
     * marca cama e tocha da casa de planície como mobília e as pula com
     * {@code finishes without minecraft:white_bed} quando falta
     * material — e ninguém voltava para pô-las.
     *
     * <p>O efeito era de sistema, e não cosmético: casa sem cama não
     * vira aldeão, aldeão é quem trabalha, e a casa que a colônia passou
     * a preferir em 2026-08-20 era justamente a que nunca ganhava cama.
     * O laço da vila — casa, cama, aldeão, trabalhador, casa — ficava
     * aberto exatamente onde ele devia fechar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "house_furnishing_read")
    public void aHouseReadFromTheGameGetsItsFurnitureToo(TestContext context) {
        Fixture fixture = setUp(context, StructureBlueprintReader.SMALL_HOUSE);

        int before = countIn(context, fixture.chest, Items.WHITE_BED);

        HouseFurnishing.run(context.getWorld(), fixture.colony);

        context.assertTrue(
                countIn(context, fixture.chest, Items.WHITE_BED) < before,
                "a casa do jogo não recebeu cama nenhuma — a Regra 21 continua só da cabana");

        fixture.owned.cleanUp();

        VillageColonyMod.BUILDINGS.removeOfColony(fixture.colony.id());

        context.complete();
    }

    private record Fixture(Colony colony, ColonyPos chest, ColonyFixture owned) {
    }

    private static Fixture setUp(TestContext context) {
        return setUp(context, ColonyHut.ID);
    }

    private static Fixture setUp(TestContext context, ResourceId plan) {
        ServerWorld world = context.getWorld();

        // Chão sólido sob a casa inteira: a cama e o lampião precisam de
        // em que se apoiar, e sem isso o teste mediria a regra errada.
        for (int dx = 0; dx <= 6; dx++) {
            for (int dz = 0; dz <= 6; dz++) {
                context.setBlockState(HOUSE.add(dx, -1, dz), Blocks.STONE.getDefaultState());
            }
        }

        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // Três de cada: uma para entrar, e duas para a repetição ter de
        // onde tirar. Um baú com uma só esconderia o defeito atrás de
        // "acabou o material".
        ChestDepositor.deposit(world, chest, Items.CHEST, 3);
        ChestDepositor.deposit(world, chest, Items.WHITE_BED, 3);
        ChestDepositor.deposit(world, chest, Items.LANTERN, 3);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        // Os baús da colônia saem dos trabalhadores, e não do mundo.
        UUID keeper = UUID.randomUUID();

        VillageColonyMod.WORKERS.register(keeper, colony.id());
        VillageColonyMod.STORAGES.register(WorkerStorage.of(keeper, chest));

        owned.owning(keeper);

        Blueprint blueprint = ColonyHut.ID.equals(plan)
                ? ColonyHut.blueprint(ColonyHut.OAK_PLANKS, Side.NORTH)
                : StructureBlueprintReader.read(context.getWorld(), plan).orElseThrow();

        ConstructionProject project = ConstructionProject.plan(
                colony.id(),
                blueprint,
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(HOUSE)));

        VillageColonyMod.BUILDINGS.register(Building.of(project));

        return new Fixture(colony, chest, owned);
    }

    private static int countIn(TestContext context, ColonyPos chest, Item item) {
        BlockPos pos = MinecraftTypeAdapter.toBlockPos(chest);

        if (!(context.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity inventory)) {
            return -1;
        }

        int found = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isOf(item)) {
                found += stack.getCount();
            }
        }

        return found;
    }
}
