package com.villagecolony.fabric.integration;

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
     * Entrega a ferramenta a quem tem função e ainda não a recebeu.
     *
     * <p>Nunca substitui o que o aldeão já segura. Um item posto ali pelo
     * jogador, ou pelo próprio mod num ciclo anterior, fica onde está —
     * sobrescrever a cada ciclo apagaria a mão de alguém trinta vezes por
     * minuto.
     *
     * <p>Trabalhador sem profissão fica sem ferramenta, e profissão de
     * mãos livres também: {@code ToolType.NONE} não vira item.
     *
     * @return quantas ferramentas foram entregues agora
     */
    public static int equip(ServerWorld world, Collection<Worker> workers) {
        int equipped = 0;

        for (Worker worker : workers) {
            Optional<ProfessionType> profession = worker.profession();

            if (profession.isEmpty()) {
                continue;
            }

            Optional<Item> tool = MinecraftTypeAdapter.toItem(
                    ProfessionRegistry.of(profession.get()).requiredTool());

            if (tool.isEmpty()) {
                continue;
            }

            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }

            if (!villager.getEquippedStack(EquipmentSlot.MAINHAND).isEmpty()) {
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
