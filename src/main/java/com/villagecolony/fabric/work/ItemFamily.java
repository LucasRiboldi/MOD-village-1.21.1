package com.villagecolony.fabric.work;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

/** A família de um item pelo nome, para a conta de demanda não descer em ciclo — ver {@link #same}. */
final class ItemFamily {

    private ItemFamily() {
    }

    /**
     * Se dois itens são a mesma coisa de outra cor ou outra madeira.
     *
     * <p><b>Existe porque a cama quase custou uma cama.</b> O jogo tem
     * mais de uma receita que dá {@code white_bed}, e uma delas é
     * <i>tingir</i>: {@code black_bed} mais corante branco. O livro
     * devolve a primeira que encontrar, e encontrou essa — a conta de
     * lã dava <b>zero</b>, e zero aqui é o pastor sem tarefa.
     *
     * <p>A regra que conserta é esta: <b>quem decompõe uma peça não pode
     * partir de outra peça da mesma família.</b> Para saber quanta lã
     * uma cama custa, uma receita que pede cama não serve, ainda que
     * exista e funcione na bancada.
     *
     * <p>Família é a última palavra do nome: {@code white_bed} e
     * {@code black_bed} são "bed"; {@code glass_pane} e {@code glass} não
     * são parentes. <b>O limite conhecido:</b> tronco descascado e tronco
     * são os dois "log", e uma conta que precisasse descer de um para o
     * outro daria zero. Nenhuma das quatro contas do {@link WorkMaterials} passa por
     * aí, e o dia em que uma passar vai aparecer como zero — que é o
     * mesmo sintoma, e agora com um lugar nomeado para procurar.
     */
    static boolean same(Item made, Item ingredient) {
        if (made == ingredient) {
            return true;
        }

        return lastWord(made).equals(lastWord(ingredient));
    }

    private static String lastWord(Item item) {
        String path = Registries.ITEM.getId(item).getPath();

        int underscore = path.lastIndexOf('_');

        return underscore < 0 ? path : path.substring(underscore + 1);
    }
}
