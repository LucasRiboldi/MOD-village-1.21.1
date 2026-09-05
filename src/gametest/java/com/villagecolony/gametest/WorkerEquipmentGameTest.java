package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorkerEquipment;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A ferramenta chega à mão de quem tem função.
 *
 * <p>Profession-System.md manda entregá-la desde a Fase 4, e até
 * 2026-08-13 nada entregava: {@code ToolType} existia, a profissão o
 * declarava, e faltava a conversão para item.
 *
 * <p>Mora no gametest e não no teste de unidade porque é fronteira: o
 * Core não conhece {@code ItemStack}.
 *
 * <p><b>Nenhum destes testes usa o registro do mod.</b> A primeira versão
 * afirmava sobre os trabalhadores da colônia mais próxima, e chegava a
 * matar um deles — e numa bateria concorrente essa colônia é
 * compartilhada com as estruturas vizinhas. O teste matava o aldeão de
 * outro teste, que é exatamente o que {@link ColonyFixture} proíbe, e
 * falhava sozinho quando a vaga de lenhador tinha ido para um aldeão que
 * não era dele. Aqui os {@code Worker} são construídos na hora, apontando
 * para aldeões que este teste mesmo criou.
 *
 * <p>O que ele <b>não</b> prova: que alguém veja a ferramenta. O modelo
 * de aldeão do Vanilla não tem braço que segure item — ver
 * {@code WorkerEquipment}.
 */
public class WorkerEquipmentGameTest implements FabricGameTest {

    /** Quanto olhar em volta do corpo, à procura do que caiu. */
    private static final double DROP_SEARCH = 3.0;

    /**
     * Cada profissão recebe a ferramenta que declara — e quem trabalha
     * de mãos livres continua de mãos livres.
     *
     * <p>Percorre as quatro, e não a que a colônia calhou de atribuir:
     * assim o teste continua valendo no dia em que a ordem de
     * preenchimento das vagas mudar.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void eachProfessionGetsTheToolItDeclares(TestContext context) {
        UUID colony = UUID.randomUUID();

        int i = 0;

        for (ProfessionType profession : ProfessionType.values()) {
            VillagerEntity villager = spawn(context, new BlockPos(1 + i++, 1, 1));

            Worker worker = Worker.restore(villager.getUuid(), colony, profession);

            WorkerEquipment.equip(context.getWorld(), List.of(worker));

            Optional<Item> expected = MinecraftTypeAdapter.toItem(
                    ProfessionRegistry.of(profession).requiredTool());

            ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

            if (expected.isEmpty()) {
                context.assertTrue(
                        held.isEmpty(),
                        profession + " trabalha de mãos livres e está segurando " + held);
            } else {
                context.assertTrue(
                        held.isOf(expected.get()),
                        profession + " deveria segurar " + expected.get()
                                + " e segura " + (held.isEmpty() ? "nada" : held.getItem()));
            }
        }

        context.complete();
    }

    /**
     * O que o jogador pôs na mão do aldeão fica onde está.
     *
     * <p>Sobrescrever a cada ciclo apagaria a mão de alguém trinta vezes
     * por minuto.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void whatTheVillagerAlreadyHoldsIsKept(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.item.Items.DIAMOND));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.LUMBERJACK);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(net.minecraft.item.Items.DIAMOND),
                "a colônia tomou a mão do aldeão");

        context.complete();
    }

    /**
     * Quem perde a função devolve a ferramenta — e só a da profissão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theToolGoesBackWhenTheJobDoes(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.LUMBERJACK);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                !villager.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty(),
                "o lenhador não chegou a receber ferramenta");

        WorkerEquipment.unequip(context.getWorld(), villager.getUuid());

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty(),
                "a ferramenta ficou na mão de quem já não é lenhador");

        context.complete();
    }

    /**
     * A ferramenta não vira fonte de itens.
     *
     * <p>Ela é criada do nada, não sai de baú nem de receita. Se caísse
     * com a morte do aldeão, matar trabalhadores seria uma forma de
     * colher machados.
     *
     * <p>Pela morte, e não pela chance de queda: o número é protegido em
     * {@code MobEntity}, e o que importa não é o campo e sim o que sobra
     * no chão.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theToolNeverDrops(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.LUMBERJACK);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

        context.assertTrue(!held.isEmpty(), "o lenhador não chegou a receber ferramenta");

        Item tool = held.getItem();
        Vec3d where = villager.getPos();

        villager.kill();

        Box around = new Box(where, where).expand(DROP_SEARCH);

        for (ItemEntity dropped : context.getWorld()
                .getEntitiesByClass(ItemEntity.class, around, entity -> true)) {

            context.assertTrue(
                    !dropped.getStack().isOf(tool),
                    "a ferramenta caiu no chão com a morte do trabalhador — a colônia"
                            + " virou fonte de " + tool);
        }

        context.complete();
    }

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

    /**
     * Um aldeão adulto deste teste.
     *
     * <p>Adulto porque bebê não recebe profissão, e a ferramenta segue a
     * profissão.
     */
    private static VillagerEntity spawn(TestContext context, BlockPos where) {
        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, where);

        villager.setBreedingAge(0);

        return villager;
    }

    /**
     * Ferramenta de outra profissão é trocada pela certa — 2026-08-29.
     *
     * <p><b>Visto em jogo.</b> A frase do autor foi <i>"mineiro e pastor
     * segurando picareta"</i>. Pastor com picareta de diamante na mão é
     * uma vila que mente sobre quem faz o quê — e ela ficava assim para
     * sempre.
     *
     * <p><b>Por que não se conserta sozinho.</b> O {@link
     * WorkerEquipment#equip} só preenchia <b>mão vazia</b>, e quem
     * esvazia a mão é o {@code unequip}, que roda quando o trabalhador
     * perde a função. Só que o {@code unequip} depende de o aldeão estar
     * <b>carregado no mundo</b>: {@code world.getEntity} devolve nulo em
     * chunk descarregado, e ele sai sem fazer nada. Falhando uma vez, a
     * ferramenta errada nunca mais é corrigida — a colônia recontrata o
     * aldeão noutra profissão e o {@code equip} vê a mão ocupada e passa
     * direto.
     *
     * <p>A regra passou a ser a invariante em vez do momento: <b>a mão
     * combina com a profissão</b>, e quem não combina é trocado na
     * passagem seguinte. Não depende mais de uma remoção acontecer na
     * hora certa.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theToolOfAnotherProfessionIsSwappedForTheRightOne(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        // A picareta do mineiro, na mão de quem virou pastor.
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.SHEPHERD);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

        context.assertTrue(
                held.isOf(Items.SHEARS),
                "o pastor continua com " + (held.isEmpty() ? "nada" : held.getItem())
                        + " na mão");

        context.complete();
    }

    /**
     * E quem perde a função de mãos livres devolve a ferramenta velha.
     *
     * <p>A outra ponta da mesma invariante: o fundidor trabalha de mãos
     * livres, e uma picareta na mão dele mente igual.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void aFreeHandedProfessionGivesTheOldToolBack(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.SMELTER);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty(),
                "o fundidor trabalha de mãos livres e continua com a picareta");

        context.complete();
    }

    /**
     * A ferramenta que a colônia deu numa versão antiga também volta —
     * 2026-09-02.
     *
     * <p><b>Visto em jogo, e o log provou por silêncio.</b> O autor viu o
     * pastor com picareta na mão; a linha {@code was holding ... — the
     * colony takes it back} não saiu uma vez na sessão inteira. Picareta
     * de diamante teria saído — ela é a ferramenta do mineiro de hoje e
     * {@code isProfessionTool} a reconhece. Logo não era de diamante.
     *
     * <p><b>Era de madeira.</b> O mineiro usou {@code WOODEN_PICKAXE} até
     * o commit {@code 5227a2d}, e o {@code ToolType} ainda a tem, sem
     * profissão nenhuma pedindo por ela. A invariante perguntava <i>"isto
     * é ferramenta de alguma profissão?"</i> ao registro de hoje, e a
     * resposta virou não — então a picareta que a própria colônia
     * entregou passou a ser tratada como <b>item do jogador</b>, que a
     * Regra 3 protege. Para sempre.
     *
     * <p>A pergunta certa é <i>"isto é ferramenta que a colônia já
     * entregou?"</i>, e quem a responde é o {@code ToolType} inteiro, que
     * é a memória dessas entregas. Picareta de pedra ou de ferro continua
     * sendo do jogador — ver {@link #whatTheVillagerAlreadyHoldsIsKept}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theToolOfAnOlderVersionIsAlsoTakenBack(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        // A picareta que o mineiro usava antes de a dele virar diamante.
        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_PICKAXE));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.SHEPHERD);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

        context.assertTrue(
                held.isOf(Items.SHEARS),
                "o pastor continua com " + (held.isEmpty() ? "nada" : held.getItem())
                        + " na mão");

        context.complete();
    }

    /**
     * A ferramenta certa não é entregue duas vezes.
     *
     * <p>A conta de entregas é o que o ciclo registra, e ela não pode
     * subir trinta vezes por minuto por causa de quem já está equipado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theRightToolIsNotHandedTwice(TestContext context) {
        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        Worker worker = Worker.restore(
                villager.getUuid(), UUID.randomUUID(), ProfessionType.SHEPHERD);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                WorkerEquipment.equip(context.getWorld(), List.of(worker)) == 0,
                "a colônia entregou a mesma tesoura de novo");

        context.complete();
    }

    /**
     * A picareta melhor do baú vai para a mão — decisão do autor,
     * 2026-09-04.
     *
     * <p><b>A frase dele:</b> <i>"se houver uma ferramenta de qualidade
     * maior dentro do seu baú o trabalhador troca pela que está
     * usando"</i>. É o outro degrau da mesma decisão que fez todo
     * trabalhador começar de madeira: sem a troca, começar de madeira
     * seria um teto em vez de um começo.
     *
     * <p>Ninguém escreveu aqui que ferro é melhor que madeira. Quem diz
     * é o jogo, medindo as duas contra a pedra — ver {@code ToolUpgrade}.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theBetterPickaxeInTheChestReachesTheHand(TestContext context) {
        BlockPos chest = new BlockPos(2, 1, 2);

        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        Worker worker = storedMiner(context, villager, chest);

        putInChest(context, chest, new ItemStack(Items.IRON_PICKAXE));

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.IRON_PICKAXE),
                "a mão ficou com "
                        + villager.getEquippedStack(EquipmentSlot.MAINHAND).getItem()
                        + " — a picareta de ferro do baú não subiu");

        context.assertTrue(
                countIn(context, chest, Items.IRON_PICKAXE) == 0,
                "a picareta de ferro continua no baú: a colônia duplicou o item");

        context.complete();
    }

    /**
     * E a de madeira que ela substituiu não vira lixo no baú.
     *
     * <p>Ela veio do nada — é a mesma conta do {@code NEVER_DROPS} —, e
     * devolvê-la faria da colônia uma fábrica: um degrau acima por ciclo,
     * uma picareta de madeira por ciclo dentro do baú.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void theColonysOwnToolDoesNotPileUpInTheChest(TestContext context) {
        BlockPos chest = new BlockPos(2, 1, 2);

        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_PICKAXE));

        Worker worker = storedMiner(context, villager, chest);

        putInChest(context, chest, new ItemStack(Items.DIAMOND_PICKAXE));

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                countIn(context, chest, Items.WOODEN_PICKAXE) == 0,
                "a picareta de madeira da colônia voltou para o baú");

        context.complete();
    }

    /**
     * O que o jogador pôs na mão volta ao baú, e não some.
     *
     * <p>A Regra 3 diz que a colônia não toma o que é do jogador. A troca
     * não toma: ela devolve. Sem esta metade, dar uma ferramenta melhor
     * ao aldeão seria o jogador perder a que ele mesmo tinha dado.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void whatThePlayerGaveComesBackWhenSomethingBetterArrives(TestContext context) {
        BlockPos chest = new BlockPos(2, 1, 2);

        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_PICKAXE));

        Worker worker = storedMiner(context, villager, chest);

        putInChest(context, chest, new ItemStack(Items.DIAMOND_PICKAXE));

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.DIAMOND_PICKAXE),
                "o diamante do baú não subiu para a mão");

        context.assertTrue(
                countIn(context, chest, Items.STONE_PICKAXE) == 1,
                "a picareta de pedra do jogador não voltou ao baú — a colônia a destruiu");

        context.complete();
    }

    /**
     * Ferramenta de outra família não é melhora, por mais cara que seja.
     *
     * <p>Um machado de diamante no baú do mineiro continua sendo um
     * machado. Esta é a prova de que a escolha vem da velocidade contra
     * a pedra e não do material: ninguém escreveu "picareta" em lugar
     * nenhum, e mesmo assim o machado perde.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "worker_equipment")
    public void aDiamondAxeIsNoUpgradeForTheMiner(TestContext context) {
        BlockPos chest = new BlockPos(2, 1, 2);

        VillagerEntity villager = spawn(context, new BlockPos(1, 1, 1));

        Worker worker = storedMiner(context, villager, chest);

        putInChest(context, chest, new ItemStack(Items.DIAMOND_AXE));

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        context.assertTrue(
                villager.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.WOODEN_PICKAXE),
                "a mão ficou com "
                        + villager.getEquippedStack(EquipmentSlot.MAINHAND).getItem()
                        + " — o machado de diamante passou por picareta");

        context.assertTrue(
                countIn(context, chest, Items.DIAMOND_AXE) == 1,
                "o machado saiu do baú do mineiro");

        context.complete();
    }

    /** Um mineiro com baú registrado, que é o que a troca exige. */
    private static Worker storedMiner(
            TestContext context, VillagerEntity villager, BlockPos chest) {

        context.setBlockState(chest, Blocks.CHEST.getDefaultState());

        ColonyPos position = MinecraftTypeAdapter.toColonyPos(context.getAbsolutePos(chest));

        VillageColonyMod.STORAGES.register(WorkerStorage.of(villager.getUuid(), position));

        return Worker.restore(villager.getUuid(), UUID.randomUUID(), ProfessionType.MINER);
    }

    private static void putInChest(TestContext context, BlockPos chest, ItemStack stack) {
        if (context.getWorld().getBlockEntity(context.getAbsolutePos(chest))
                instanceof ChestBlockEntity inventory) {

            inventory.setStack(0, stack);
            inventory.markDirty();
        }
    }

    private static int countIn(TestContext context, BlockPos chest, Item item) {
        if (!(context.getWorld().getBlockEntity(context.getAbsolutePos(chest))
                instanceof ChestBlockEntity inventory)) {

            return 0;
        }

        int found = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isOf(item)) {
                found += inventory.getStack(slot).getCount();
            }
        }

        return found;
    }
}
