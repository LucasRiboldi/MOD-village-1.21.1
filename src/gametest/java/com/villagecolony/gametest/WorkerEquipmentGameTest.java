package com.villagecolony.gametest;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.WorkerEquipment;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
}
