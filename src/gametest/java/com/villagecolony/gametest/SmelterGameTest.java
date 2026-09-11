package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskPriority;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Side;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestInventoryReader;
import com.villagecolony.fabric.integration.ColonyChests;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.work.SmelterWork;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * O fundidor — 2026-08-20.
 *
 * <p>A exceção honesta da Regra 10, resolvida: a vidraça pedia vidro,
 * vidro pedia fundir, e a colônia não fundia. Ficou escrito em 08-18 que
 * "enquanto não houver forno, o vidro é material que o jogador guarda no
 * baú".
 */
public class SmelterGameTest implements FabricGameTest {

    private static final BlockPos CHEST = new BlockPos(2, 2, 2);

    private static final BlockPos STAND = new BlockPos(3, 2, 3);

    /**
     * A areia do baú vira vidro no mesmo baú.
     *
     * <p>Duas metades, como em toda transformação deste mod: a areia sai
     * e o vidro entra. Uma que saísse sem a outra entrar seria a colônia
     * destruindo material do jogador.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter",
            tickLimit = 200)
    public void theSandInTheChestBecomesGlass(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.SAND, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.GLASS,
                4);

        task.reserveFor(villager.getUuid());

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            int glass = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.GLASS);

            int sand = ChestInventoryReader
                    .read(world, context.getAbsolutePos(CHEST))
                    .amountOf(ResourceType.SAND);

            try {
                context.assertTrue(glass > 0, "a areia não virou vidro nenhum");

                context.assertTrue(
                        sand < 4,
                        "o vidro apareceu e a areia continua inteira — matéria do nada");
            } finally {
                owned.cleanUp();

            }

            context.complete();
        });
    }

    /**
     * O ferro cru vira lingote na mesma fornalha — 2026-08-21.
     *
     * <p>O fundidor só conhecia areia. Uma tarefa de fundir ferro o faria
     * tirar a <b>areia</b> do baú, porque o cru estava escrito no código
     * e não vinha da tarefa — e a areia da vidraça queimaria para não dar
     * lingote nenhum.
     *
     * <p>Agora o cru sai do que a tarefa pede, e é a única tabela desse
     * tipo no mod: duas linhas, as duas que a colônia consome. O que a
     * fornalha <b>devolve</b> continua sendo pergunta ao livro do jogo.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_iron",
            tickLimit = 200)
    public void theRawIronInTheChestBecomesAnIngot(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        ChestDepositor.deposit(world, chest, Items.RAW_IRON, 4);

        // A areia entra junto, e é o ponto: se o fundidor voltar a olhar
        // só para o grupo da areia, ele a queima e o ferro fica intacto.
        ChestDepositor.deposit(world, chest, Items.SAND, 4);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.IRON_INGOT,
                4);

        task.reserveFor(villager.getUuid());

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            var inChest = ChestInventoryReader.read(world, context.getAbsolutePos(CHEST));

            try {
                context.assertTrue(
                        inChest.amountOf(ResourceType.IRON_INGOT) > 0,
                        "o ferro cru não virou lingote nenhum");

                context.assertTrue(
                        inChest.amountOf(ResourceType.RAW_IRON) < 4,
                        "o lingote apareceu e o cru continua inteiro — matéria do nada");

                context.assertTrue(
                        inChest.amountOf(ResourceType.SAND) == 4,
                        "o fundidor queimou a areia numa tarefa de ferro");
            } finally {
                owned.cleanUp();

            }

            context.complete();
        });
    }

    /** Os lingotes nos dois baús do cenário: o do fundidor e o da boca. */
    private static int totalIronIngots(
            ServerWorld world, TestContext context, BlockPos mouthChest) {

        return ChestInventoryReader.read(world, context.getAbsolutePos(CHEST))
                        .amountOf(ResourceType.IRON_INGOT)
                + ChestInventoryReader.read(world, context.getAbsolutePos(mouthChest))
                        .amountOf(ResourceType.IRON_INGOT);
    }

    /**
     * <b>A cadeia mineiro → armazenamento → fundidor, remendada</b> —
     * P0.3, 2026-09-11.
     *
     * <p><b>Este teste nasceu provando o defeito e passou a provar o
     * conserto</b>, no mesmo dia. A primeira versão afirmava que a
     * colônia <i>não</i> enxergava o minério da boca da mina, e trazia um
     * recado para quem o visse falhar: <i>"a ruptura não é mais esta, e
     * este teste precisa ser relido"</i>. Foi o que aconteceu.
     *
     * <p>Na sessão de 09-04 o fundidor disse
     * {@code nothing in the colony chests to smelt} <b>34 vezes</b>, com
     * o mineiro cavando. O plano de correção mandou instrumentar a cadeia
     * antes de mexer no fundidor, e a instrumentação não foi precisa: a
     * ruptura é estática e está aqui.
     *
     * <p><b>A Regra 30 manda o minério que não é carvão para o baú da
     * boca da mina.</b> Esse baú é achado por geometria —
     * {@code MineMouth.chestAt} procura um baú encostado na entrada —, e
     * <b>não é registro de ninguém</b>. O único lugar que cria
     * {@code WorkerStorage} é {@code ChestScanner.scan}, que procura baú
     * ao redor da <b>cama</b> do aldeão; mina não tem cama ao lado.
     *
     * <p>E tudo o que contava o estoque da colônia — {@code ColonyChests},
     * {@code ChestInventoryReader.survey} e este fundidor — percorria
     * baús de <b>trabalhador</b>. O minério entrava num baú que a
     * contabilidade não lia: o fundidor estava certo, e faminto ao lado
     * do ferro.
     *
     * <p><b>O conserto foi do lado de quem lê.</b> A Regra 30 é decisão
     * do autor de 2026-08-22 com motivo escrito — o mineiro não carrega
     * minério montanha acima —, e revogá-la para a conta fechar trocaria
     * um defeito de contabilidade por um de desenho. Então o
     * {@code ColonyChests} passou a ser a <b>única</b> resposta a "onde
     * estão os baús desta colônia", com o da boca da mina entre eles, e
     * os três que montavam a própria lista passaram a perguntar a ele.
     *
     * <p><b>Os três juntos, e não um por vez.</b> Contar num conjunto e
     * consumir de outro é a discordância que o javadoc do
     * {@code ResourceSubstitution} guarda de 2026-09-10 — <i>"a colônia
     * concluía que a meta estava cumprida e o mineiro não ia cavar,
     * enquanto o construtor esperava pelo arenito"</i>. Meia correção
     * aqui seria pior que nenhuma.
     *
     * <p>Este teste fixa as duas metades, agora do lado certo. Que é
     * mesmo ali que o minério cai, pelo caminho que o
     * {@code MinerHaul.treasureChestFor} percorre — registro da mina,
     * entrada do poço, baú encostado. E que a colônia <b>o enxerga</b>,
     * conta o ferro e funde.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "smelter_mine_mouth",
            tickLimit = 200)
    public void theOreInTheMineMouthChestIsCountedAndSmelted(TestContext context) {
        ServerWorld world = context.getWorld();

        context.setBlockState(new BlockPos(3, 1, 3), Blocks.DIRT.getDefaultState());
        context.setBlockState(CHEST, Blocks.CHEST.getDefaultState());

        ColonyPos chest = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(CHEST));

        // A boca da mina, longe da cama, e o baú encostado nela. É o
        // arranjo que a Regra 30 pressupõe.
        BlockPos mouth = new BlockPos(5, 2, 5);
        BlockPos mouthChest = mouth.east();

        context.setBlockState(mouthChest, Blocks.CHEST.getDefaultState());

        ColonyPos treasure =
                MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(mouthChest));

        // Oito ferros crus: o que meia hora de mineiro entrega, e mais do
        // que a tarefa pede. Nada aqui é questão de quantidade.
        ChestDepositor.deposit(world, treasure, Items.RAW_IRON, 8);

        Colony colony = Colony.create(UUID.randomUUID(), chest);

        VillageColonyMod.COLONIES.register(colony);

        ColonyFixture owned = ColonyFixture.create().owning(colony);

        VillageColonyMod.MINES.restore(Mine.restore(
                colony.id(),
                MineShaft.from(
                        MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(mouth)),
                        Side.NORTH),
                0));

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, STAND);
        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colony.id());
        worker.assign(ProfessionType.SMELTER);

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), chest));

        owned.owning(villager.getUuid());

        Task task = VillageColonyMod.TASKS.create(
                colony.id(),
                TaskType.SMELT_MATERIAL,
                TaskPriority.PRODUCTION,
                ResourceType.IRON_INGOT,
                4);

        task.reserveFor(villager.getUuid());

        // Montagem, e antes de o fundidor rodar: o minério está lá de
        // verdade. Aferir isto no fim seria medir o contrário do que se
        // quer — a partir do conserto o cru sai do baú, que é o ponto.
        context.assertTrue(
                ChestInventoryReader
                        .read(world, context.getAbsolutePos(mouthChest))
                        .amountOf(ResourceType.RAW_IRON) == 8,
                "o cenário não pôs o ferro no baú da boca");

        SmelterWork.run(world, colony);

        context.runAtTick(150, () -> {
            try {
                // Primeira metade: é este o baú que o mineiro alimenta.
                // O caminho é o do MinerHaul.treasureChestFor — mina da
                // colônia, entrada do poço, baú encostado.
                BlockPos reached = VillageColonyMod.MINES.of(colony.id())
                        .map(mine -> MinecraftTypeAdapter.toBlockPos(mine.shaft().entry()))
                        .flatMap(entry -> MineMouth.chestAt(world, entry))
                        .orElse(null);

                context.assertTrue(
                        context.getAbsolutePos(mouthChest).equals(reached),
                        "a Regra 30 não chega a este baú — o cenário não prova nada");

                // Segunda metade: a colônia o enxerga, pela mesma lista
                // que o ciclo usa. Montar uma lista à parte aqui faria o
                // teste medir um caminho que a produção não percorre.
                ChestInventoryReader.ChestSurvey survey = ChestInventoryReader.survey(
                        world,
                        ColonyChests.nearestFirst(world, colony.id(), colony.center()));

                context.assertTrue(
                        survey.resources().amountOf(ResourceType.RAW_IRON) > 0,
                        "a varredura não achou o ferro da boca da mina: "
                                + survey.coverage());

                // E o baú da boca entra na cobertura, e não só no total:
                // um baú somado sem ser contado voltaria a ser o
                // defeito-que-parece-número do P0.2.
                context.assertTrue(
                        survey.chestsRead() == 2,
                        "esperava dois baús lidos — o do fundidor e o da boca —, deu "
                                + survey.coverage());

                // E o fundidor come: o cru saiu da boca e o lingote entrou.
                context.assertTrue(
                        ChestInventoryReader
                                .read(world, context.getAbsolutePos(mouthChest))
                                .amountOf(ResourceType.RAW_IRON) < 8,
                        "o fundidor não tocou no ferro da boca da mina");

                context.assertTrue(
                        totalIronIngots(world, context, mouthChest) > 0,
                        "o cru saiu e não virou lingote nenhum — matéria perdida");
            } finally {
                owned.cleanUp();
            }

            context.complete();
        });
    }
}
