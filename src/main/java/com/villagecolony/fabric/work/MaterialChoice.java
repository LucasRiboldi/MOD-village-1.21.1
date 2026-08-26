package com.villagecolony.fabric.work;

import com.villagecolony.core.resource.service.ResourceSubstitution;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.core.type.Substitution;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * O que o construtor pode assentar no lugar do bloco pedido — 2026-08-26.
 *
 * <p><b>A Regra 27 abriu para pedra, e este arquivo é a abertura.</b> Ela
 * é imutável desde 2026-08-20 e manda o construtor <i>aguardar a
 * existência do específico tipo de bloco que ele precisa</i>. O autor a
 * emendou em 08-26 com três palavras — <i>abre para pedra só</i> — e o
 * resto dela continua de pé: fora da pedra, o construtor espera o exato.
 *
 * <p><b>Só {@link Substitution#ALTERNATIVE} entra aqui</b>, e é essa a
 * diferença entre os dois níveis do meio. {@code ACCEPTABLE} conta para
 * a meta da colônia — abeto responde por carvalho quando a pergunta é
 * "tenho tronco?" — e não vai para a parede. {@code ALTERNATIVE} vai.
 *
 * <p><b>O preferido primeiro, sempre.</b> A casa sai com o bloco certo
 * enquanto ele existir; o substituto entra quando o certo acabou. É a
 * frase da ADR-009 §3.10: <i>deserto prefere arenito; isso não quer
 * dizer que só possa arenito</i>.
 *
 * <p><b>Por que isto não ressuscita o defeito de 2026-08-22.</b> Naquele
 * dia a conta aceitava pedregulho por arenito e o construtor não: a
 * colônia declarava a meta cumprida, o mineiro não ia cavar, e a obra
 * dormia esperando o que ninguém buscaria. O defeito era a
 * <b>discordância</b>. Esta classe existe para desfazê-la — o que a
 * conta aceita é o que a parede aceita, e as duas leem a mesma tabela.
 */
public final class MaterialChoice {

    private MaterialChoice() {
    }

    /**
     * Os itens que servem para este bloco, do preferido ao último.
     *
     * <p>Nunca vazio: o primeiro é sempre o item do próprio bloco, mesmo
     * quando a colônia não acompanha aquele recurso — e é o caso da
     * esmagadora maioria deles.
     *
     * <p>A ordem sai de {@code ResourceSubstitution.byPreference}, e não
     * daqui: duas ordens diferentes seriam duas políticas.
     */
    public static List<Item> forBlock(Block wanted) {
        Item exact = wanted.asItem();

        List<Item> order = new ArrayList<>();

        order.add(exact);

        Optional<ResourceType> resource = MinecraftTypeAdapter.toResourceType(exact);

        if (resource.isEmpty()) {
            // Bloco que a colônia não conta — escada, porta, vidraça. Não
            // há substituição declarada para o que não é recurso.
            return List.copyOf(order);
        }

        for (ResourceType candidate : ResourceSubstitution.byPreference(resource.get())) {
            if (ResourceSubstitution.levelOf(resource.get(), candidate)
                    != Substitution.ALTERNATIVE) {

                // PREFERRED é o próprio, e já entrou. ACCEPTABLE conta
                // para a meta e não vai para a parede — a Regra 27 só
                // abriu para o nível de baixo.
                continue;
            }

            MinecraftTypeAdapter.toItem(candidate).ifPresent(item -> {
                if (!order.contains(item)) {
                    order.add(item);
                }
            });
        }

        return List.copyOf(order);
    }

    /**
     * Se este item é o bloco que a planta pediu, e não um substituto.
     *
     * <p>Serve a quem vai assentar: o bloco da planta carrega estado —
     * o lado da escada, a metade da porta — e o substituto não tem esse
     * estado para carregar. Ver {@code BuilderWork.placeOne}.
     */
    public static boolean isExact(Block wanted, Item taken) {
        return wanted.asItem() == taken;
    }
}
