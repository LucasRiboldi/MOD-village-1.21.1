package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.ToolType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.worker.service.ProfessionRegistry;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Põe a ferramenta da profissão na mão do trabalhador.
 *
 * <p>Profession-System.md §"Ferramentas das Profissões": o trabalhador
 * recebe automaticamente a ferramenta inicial. {@code ToolType} existia
 * desde a Fase 4 e cada profissão já declarava a sua, mas nada convertia
 * aquilo em item — a regra estava aceita em documento e não acontecia.
 *
 * <p><b>O que isto não é.</b> A ferramenta não muda a velocidade de
 * trabalho. A Regra 2 do autor diz que o trabalhador quebra um bloco no
 * tempo de um jogador <em>com ferramenta de ferro</em>, e é isso que
 * {@code LumberjackWork} usa. Dar-lhe um machado de madeira e passar a
 * medir por ele tornaria a colheita mais lenta do que a regra manda —
 * seria trocar uma regra do autor por uma consequência de implementação.
 *
 * <p><b>O que o jogador vê: nada, hoje.</b> Verificado no jarro mapeado
 * da 1.21.1 — {@code VillagerResemblingModel} implementa apenas
 * {@code ModelWithHead} e {@code ModelWithHat}, nunca
 * {@code ModelWithArms}, e {@code VillagerEntityRenderer} não monta
 * {@code HeldItemFeatureRenderer}. O modelo de aldeão do Vanilla não tem
 * onde pendurar um item. Quem mostra a profissão continua sendo o nome
 * sobre a cabeça e o quadro no baú.
 *
 * <p>Fica registrado assim mesmo porque é o que o documento pede, porque
 * persiste no NBT do aldeão e porque é o lugar de onde a Fase 10 vai
 * puxar a ferramenta do construtor. Um zumbi-aldeão, esse sim, mostra o
 * que tem na mão.
 */
public final class WorkerEquipment {

    /**
     * Nada do que a colônia cria vira drop.
     *
     * <p>A ferramenta é dada do nada — não sai de baú nem de receita. Se
     * ela caísse com a morte do aldeão, a colônia viraria uma fonte de
     * itens: bastaria matar trabalhadores para colher machados. Zero de
     * chance de queda fecha isso.
     */
    private static final float NEVER_DROPS = 0.0f;

    private WorkerEquipment() {
    }

    /**
     * Faz a mão do aldeão combinar com a profissão dele.
     *
     * <p><b>O que o jogador pôs ali fica onde está.</b> Um diamante, uma
     * flor, uma espada: a colônia não toma a mão de ninguém. Ela só mexe
     * no que ela mesma dá — as ferramentas de profissão, e é o
     * {@link #isProfessionTool} que as reconhece.
     *
     * <p><b>Era só "preencher mão vazia", e isso não se conserta
     * sozinho</b> — 2026-08-29, visto em jogo: <i>"mineiro e pastor
     * segurando picareta"</i>. Quem esvazia a mão é o {@link #unequip},
     * que roda quando o trabalhador perde a função; só que ele depende de
     * o aldeão estar <b>carregado no mundo</b>, e {@code world.getEntity}
     * devolve nulo em chunk descarregado. Falhando uma vez, a ferramenta
     * errada nunca mais era corrigida: a colônia recontratava o aldeão
     * noutra profissão e esta passagem via a mão ocupada e seguia.
     *
     * <p>A regra passou a ser a <b>invariante</b> em vez do momento —
     * <i>a mão combina com a profissão</i> —, e por isso ela se corrige
     * na passagem seguinte em vez de depender de uma remoção acontecer
     * na hora certa.
     *
     * <p>Quem já está com a ferramenta certa não é tocado, e é o que
     * impede a mão de alguém de ser reescrita trinta vezes por minuto.
     *
     * @return quantas ferramentas foram <b>entregues</b> agora. Ferramenta
     *     devolvida por profissão de mãos livres não conta: ela não é
     *     entrega, e somá-las faria a linha do ciclo dizer que a colônia
     *     equipou alguém quando ela desequipou
     */
    public static int equip(ServerWorld world, Collection<Worker> workers) {
        int equipped = 0;

        for (Worker worker : workers) {
            Optional<ProfessionType> profession = worker.profession();

            if (profession.isEmpty()) {
                continue;
            }

            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }

            Optional<Item> tool = MinecraftTypeAdapter.toItem(
                    ProfessionRegistry.of(profession.get()).requiredTool());

            ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

            if (upgrade(world, worker, profession.get(), villager, held, tool)) {
                equipped++;

                continue;
            }

            if (tool.isPresent() && held.isOf(tool.get())) {
                continue;
            }

            if (!held.isEmpty() && !isProfessionTool(held)) {
                // Do jogador. A Regra 3 vale para a mão do aldeão também.
                continue;
            }

            if (!held.isEmpty()) {
                VillageColonyMod.LOGGER.info(
                        "Worker {} is a {} and was holding {} — the colony takes it back",
                        worker.villagerId(),
                        profession.get(),
                        held.getItem());
            }

            if (tool.isEmpty()) {
                // Mãos livres, e a mão não está livre.
                villager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

                continue;
            }

            villager.equipStack(EquipmentSlot.MAINHAND, new ItemStack(tool.get()));
            villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, NEVER_DROPS);

            equipped++;
        }

        return equipped;
    }

    /**
     * Troca a ferramenta da mão pela melhor do baú do trabalhador —
     * decisão do autor, 2026-09-04.
     *
     * <p><b>A frase dele:</b> <i>"se houver uma ferramenta de qualidade
     * maior dentro do seu baú o trabalhador troca pela que está
     * usando"</i>. Quem julga "maior" é o {@link ToolUpgrade}, e ele
     * julga pela velocidade que o jogo mede.
     *
     * <p><b>Vem antes de tudo, inclusive de "a mão é do jogador".</b> A
     * Regra 3 impede a colônia de <i>tomar</i> o que o jogador deu, e
     * nada aqui toma: o que sai da mão volta para o baú, a menos que
     * seja ferramenta que a própria colônia entregou — essa veio do
     * nada e volta ao nada, que é a mesma conta do
     * {@link #NEVER_DROPS}. O aldeão fica com a melhor das duas, e o
     * jogador não perde item nenhum.
     *
     * <p><b>Baú cheio cancela a troca.</b> Devolver a ferramenta antiga
     * é parte da troca, e não um passo depois dela: fazer a metade que
     * equipa sem a metade que devolve seria a colônia destruindo o que
     * o jogador pôs no baú do aldeão. Se não cabe, não troca — e a
     * passagem seguinte tenta de novo.
     *
     * @return se a mão mudou agora
     */
    private static boolean upgrade(
            ServerWorld world,
            Worker worker,
            ProfessionType profession,
            VillagerEntity villager,
            ItemStack held,
            Optional<Item> starter) {

        Optional<WorkerStorage> storage = VillageColonyMod.STORAGES.of(worker.villagerId());

        if (storage.isEmpty()) {
            return false;
        }

        ItemStack floor = starter.map(ItemStack::new).orElse(ItemStack.EMPTY);

        Optional<ItemStack> better = ToolUpgrade.betterThan(
                world, profession, held, floor, storage.get().chestPosition());

        if (better.isEmpty()) {
            return false;
        }

        ItemStack take = better.get();
        boolean givesBack = !held.isEmpty() && !isProfessionTool(held);

        if (givesBack
                && ChestDepositor.freeSpaceFor(world, storage.get().chestPosition(),
                        held.getItem()) < 1) {

            return false;
        }

        if (ChestWithdrawer.takeOne(world, storage.get().chestPosition(), take.getItem()) < 1) {
            return false;
        }

        if (givesBack) {
            ChestDepositor.deposit(
                    world, storage.get().chestPosition(), held.getItem(), held.getCount());
        }

        villager.equipStack(EquipmentSlot.MAINHAND, take);
        villager.setEquipmentDropChance(EquipmentSlot.MAINHAND, NEVER_DROPS);

        VillageColonyMod.LOGGER.info(
                "Worker {} is a {} and traded {} for the {} in its chest",
                worker.villagerId(),
                profession,
                held.isEmpty() ? "empty hands" : held.getItem(),
                take.getItem());

        return true;
    }

    /**
     * Tira a ferramenta da mão de quem perdeu a função.
     *
     * <p>Contraparte de {@link #equip}, pelo mesmo motivo que a marca do
     * baú sai quando o trabalhador é dispensado: um machado na mão de
     * quem já não é lenhador mente para quem está jogando.
     *
     * <p>Só retira ferramenta de profissão. O que o jogador tenha posto
     * ali continua onde está — a colônia devolve o que deu, e nada mais.
     */
    public static void unequip(ServerWorld world, UUID villagerId) {
        if (!(world.getEntity(villagerId) instanceof VillagerEntity villager)) {
            return;
        }

        ItemStack held = villager.getEquippedStack(EquipmentSlot.MAINHAND);

        if (held.isEmpty() || !isProfessionTool(held)) {
            return;
        }

        villager.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    /**
     * Se este item é ferramenta que a colônia entrega — ou já entregou.
     *
     * <p><b>A pergunta mudou em 2026-09-02</b>, e o que a mudou foi um
     * pastor com picareta na mão. Ela era <i>"isto é ferramenta de alguma
     * profissão?"</i>, feita ao registro de <b>hoje</b>. O mineiro usou
     * {@code WOODEN_PICKAXE} até a picareta dele virar diamante, e no dia
     * seguinte àquela troca a picareta de madeira que a própria colônia
     * tinha entregado deixou de ser reconhecida: virou <b>item do
     * jogador</b>, que a Regra 3 protege, e ficou naquela mão para
     * sempre. O log não dizia nada porque o caminho que a mantinha é o
     * que não escreve linha.
     *
     * <p>Quem responde agora é o {@link ToolType} inteiro, que é a
     * memória do que a colônia já pôs em mão de aldeão — inclusive o que
     * nenhuma profissão pede mais. O que o jogador deu continua dele:
     * picareta de pedra, de ferro, espada, pão, nada disso está aqui.
     */
    private static boolean isProfessionTool(ItemStack held) {
        for (ToolType tool : ToolType.values()) {
            Optional<Item> item = MinecraftTypeAdapter.toItem(tool);

            if (item.isPresent() && held.isOf(item.get())) {
                return true;
            }
        }

        return false;
    }
}
