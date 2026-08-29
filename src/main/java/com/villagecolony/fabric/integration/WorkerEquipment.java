package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
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

    /** Se este item é ferramenta que alguma profissão entrega. */
    private static boolean isProfessionTool(ItemStack held) {
        for (ProfessionType type : ProfessionType.values()) {
            Optional<Item> tool = MinecraftTypeAdapter.toItem(
                    ProfessionRegistry.of(type).requiredTool());

            if (tool.isPresent() && held.isOf(tool.get())) {
                return true;
            }
        }

        return false;
    }
}
