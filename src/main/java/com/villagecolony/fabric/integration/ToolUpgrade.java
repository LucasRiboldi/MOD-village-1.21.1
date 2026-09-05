package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * A ferramenta melhor que está no baú do trabalhador — 2026-09-04.
 *
 * <p><b>A regra do autor:</b> <i>"todos trabalhadores começam com a
 * ferramenta nível 1 de madeira, se houver uma ferramenta de qualidade
 * maior dentro do seu baú o trabalhador troca pela que está usando"</i>.
 *
 * <p>Junto com ela veio a outra metade — <i>"cada trabalhador deve
 * coletar o recurso na velocidade de sua ferramenta"</i> —, e é a
 * segunda que dá a esta classe o critério da primeira. <b>Qualidade
 * maior é velocidade maior</b>, e velocidade é coisa que o jogo mede:
 * {@code ItemStack.getMiningSpeedMultiplier}. Nenhuma escada de madeira
 * → pedra → ferro → diamante está escrita aqui, porque escrevê-la seria
 * repetir, com risco de errar, uma tabela que o Vanilla já mantém — e
 * envelheceria no primeiro material novo, de outra versão ou de mod.
 *
 * <p><b>A família da ferramenta sai de graça disso.</b> Não é preciso
 * perguntar "isto é picareta?": basta medir contra a pedra. Machado em
 * pedra vale 1,0, como a mão vazia; picareta de madeira vale 2,0. Uma
 * ferramenta que não serve à profissão nunca é mais rápida que a que
 * serve, e por isso nunca ganha — sem uma linha de código para dizê-lo.
 *
 * <p><b>Uma consequência que é do Vanilla e fica dita:</b> a picareta de
 * ouro é mais rápida que a de diamante — 12 contra 8 —, e vai ganhar
 * dela. No jogo isso é um mau negócio porque o ouro se gasta em poucos
 * blocos; <b>aqui não se gasta</b>: nenhum ponto deste mod chama
 * {@code damage} numa ferramenta de trabalhador. A regra do autor é a
 * velocidade, o ouro é o mais veloz, e o defeito que tornaria isso ruim
 * não existe nesta colônia.
 */
public final class ToolUpgrade {

    /**
     * O bloco em que cada profissão mede a ferramenta dela.
     *
     * <p>É julgamento, e está escrito como julgamento — mas é o
     * <b>menor</b> julgamento que resolve o problema: uma pergunta por
     * profissão, e a resposta a todas as ferramentas de todos os
     * materiais vem do jogo. Cada bloco é o que aquele trabalhador de
     * fato quebra o dia inteiro.
     *
     * <p>Profissão que não aparece aqui não troca de ferramenta. São as
     * de mãos livres — construtor, fundidor, fabricante —, e o pastor:
     * tesoura não tem grau, então não há por que medir.
     */
    private static final Map<ProfessionType, Block> PROOF =
            new EnumMap<>(ProfessionType.class);

    static {
        PROOF.put(ProfessionType.MINER, Blocks.STONE);
        PROOF.put(ProfessionType.LUMBERJACK, Blocks.OAK_LOG);
        PROOF.put(ProfessionType.FARMER, Blocks.HAY_BLOCK);
    }

    private ToolUpgrade() {
    }

    /**
     * A melhor ferramenta do baú, se ela superar o que já está em uso.
     *
     * <p>A barra é o <b>maior</b> entre o que ele segura e o que a
     * profissão entrega de início. Sem o segundo, um aldeão de mão vazia
     * — o {@code WorkerEquipment} falha calado em chunk descarregado —
     * aceitaria qualquer coisa do baú como melhora sobre a mão nua.
     *
     * <p>Empate não troca. Duas picaretas de pedra são a mesma picareta
     * de pedra, e trocar uma pela outra a cada ciclo de colônia seria
     * mexer na mão de um aldeão trinta vezes por minuto para nada — o
     * mesmo defeito que o {@code equip} já evita quando a mão está certa.
     *
     * @return uma <b>cópia de um item só</b>, ainda dentro do baú. Quem
     *     chamar é que decide tirá-la de lá; esta leitura não muda nada
     *     no mundo
     */
    public static Optional<ItemStack> betterThan(
            ServerWorld world,
            ProfessionType profession,
            ItemStack held,
            ItemStack starter,
            ColonyPos chest) {

        Block proof = PROOF.get(profession);

        if (proof == null) {
            return Optional.empty();
        }

        ChestBlockEntity inventory = ChestWithdrawer.chestAt(world, chest);

        if (inventory == null) {
            // Chunk descarregado. Não é erro, e não é "não há nada lá":
            // é não saber, e não saber não troca ferramenta.
            return Optional.empty();
        }

        BlockState state = proof.getDefaultState();

        float bar = Math.max(speedOf(held, state), speedOf(starter, state));

        ItemStack best = null;
        float bestSpeed = bar;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack candidate = inventory.getStack(slot);

            if (candidate.isEmpty()) {
                continue;
            }

            float speed = speedOf(candidate, state);

            if (speed <= bestSpeed) {
                continue;
            }

            best = candidate;
            bestSpeed = speed;
        }

        return best == null ? Optional.empty() : Optional.of(best.copyWithCount(1));
    }

    /**
     * Quão depressa esta pilha quebra este bloco.
     *
     * <p>Pilha vazia é a mão nua, e o jogo já responde 1,0 por ela — não
     * há caso especial a escrever.
     */
    private static float speedOf(ItemStack stack, BlockState state) {
        return stack.getMiningSpeedMultiplier(state);
    }
}
