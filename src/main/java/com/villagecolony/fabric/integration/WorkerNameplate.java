package com.villagecolony.fabric.integration;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/**
 * Põe o nome da profissão sobre a cabeça do trabalhador.
 *
 * <p>A colônia atribui função, e até aqui isso era invisível: dois
 * aldeões idênticos, um lenhador e um fazendeiro, e nada no mundo dizia
 * qual era qual. Quem quisesse saber lia o log e comparava UUID.
 *
 * <p>Escolhido pelo autor em 2026-08-08 entre três caminhos. Os outros
 * dois eram vestir o aldeão com roupa de profissão Vanilla, que muda
 * trocas e estação de trabalho, e textura própria, que exigiria mixin de
 * renderização, sincronização por rede e ADR nova — o mod deixaria de
 * funcionar só no servidor.
 *
 * <p>Texto literal, e não {@code Text.translatable}: o mod roda no
 * servidor e o cliente pode ser Vanilla puro. Chave de tradução chegaria
 * como {@code villagecolony.profession.lumberjack} na tela de quem não
 * tem o mod instalado.
 */
public final class WorkerNameplate {

    private WorkerNameplate() {
    }

    /**
     * Nomeia os trabalhadores desta colônia que ainda não têm nome.
     *
     * <p>Nunca sobrescreve nome que já existe. Aldeão batizado com
     * etiqueta pelo jogador continua com o nome que ele deu — o mod não
     * tem direito sobre isso, e perder um nome dado à mão é o tipo de
     * dano que não se desfaz.
     *
     * <p>Trabalhador sem profissão fica sem nome. Bebê e nitwit são o
     * caso comum, e nomeá-los de "trabalhador" diria algo falso.
     *
     * @return quantos nomes foram postos agora
     */
    public static int label(ServerWorld world, Collection<Worker> workers) {
        int labelled = 0;

        for (Worker worker : workers) {
            if (worker.profession().isEmpty()) {
                continue;
            }

            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }

            Text current = nameOf(villager);

            if (current != null && !isColonyLabel(current)) {
                // Nome que o jogador deu. A Regra 3 vale aqui como vale
                // na mão do aldeão.
                continue;
            }

            Text label = labelFor(worker.profession().get());

            if (current != null && current.getString().equals(label.getString())
                    && sameColour(current, label)) {

                // Já está certo, e reescrevê-lo toda passagem seria mexer
                // no nome de um aldeão trinta vezes por minuto.
                continue;
            }

            villager.setCustomName(label);
            villager.setCustomNameVisible(true);

            labelled++;
        }

        return labelled;
    }

    private static Text nameOf(Entity entity) {
        return entity.getCustomName();
    }

    /**
     * Se este nome é um dos que a colônia mesma põe.
     *
     * <p>Mesma pergunta — e pelo mesmo motivo — que o
     * {@code WorkerEquipment.isProfessionTool} faz da ferramenta: sem
     * ela, o nome que a colônia escreveu ontem vira <b>nome do
     * jogador</b> hoje, e a Regra 3 o protege para sempre.
     *
     * <p><b>Foi o que a cor pediu</b>, em 2026-09-05. O nome só era posto
     * quando não havia nenhum, então numa vila já batizada a cor não
     * apareceria em ninguém — a colônia inteira ficaria com os nomes
     * brancos de antes, e o autor veria a mudança não acontecer.
     *
     * <p>Compara o <b>texto</b>, e não o estilo: é o mesmo nome com ou
     * sem cor, e é isso que permite pintar o que já estava escrito.
     */
    private static boolean isColonyLabel(Text name) {
        String written = name.getString();

        for (ProfessionType profession : ProfessionType.values()) {
            if (labelFor(profession).getString().equals(written)) {
                return true;
            }
        }

        return false;
    }

    /** Se os dois já estão da mesma cor. */
    private static boolean sameColour(Text current, Text label) {
        return java.util.Objects.equals(
                current.getStyle().getColor(), label.getStyle().getColor());
    }

    /**
     * O nome que vai aparecer.
     *
     * <p>Em português porque é a língua do jogo do autor, e porque texto
     * literal não passa pelo sistema de tradução — ver a nota da classe.
     */
    private static Text labelFor(ProfessionType profession) {
        return Text.literal(nameFor(profession)).formatted(colourOf(profession));
    }

    private static String nameFor(ProfessionType profession) {
        return switch (profession) {
            case LUMBERJACK -> "Lenhador";
            case MINER -> "Mineiro";
            case SHEPHERD -> "Pastor";
            case SMELTER -> "Fundidor";
            case CARPENTER -> "Carpinteiro";
            case MASON -> "Pedreiro";
            case FARMER -> "Fazendeiro";
            case BUILDER -> "Construtor";
        };
    }

    /**
     * A cor de cada profissão — decisão do autor, 2026-09-05: <i>"coloque
     * um nome colorido para cada profissão"</i>.
     *
     * <p>Cada uma puxa do material que ela traz, porque é o que o jogador
     * já associa a ela sem precisar decorar tabela: folha para quem corta
     * árvore, pedra para quem cava, lã para quem tosquia, fogo para quem
     * funde, tábua para quem fabrica, lavoura para quem planta.
     *
     * <p>O construtor é o único sem material próprio — ele assenta o dos
     * outros —, e por isso fica com a cor que sobra e não se confunde com
     * nenhuma das seis.
     *
     * <p><b>Sete cores distintas, e é o requisito.</b> Nome colorido que
     * se confunde com o do vizinho não diz profissão nenhuma: as duas
     * verdes são clara e escura, e as duas quentes são vermelho e
     * dourado.
     */
    private static Formatting colourOf(ProfessionType profession) {
        return switch (profession) {
            case LUMBERJACK -> Formatting.DARK_GREEN;
            case MINER -> Formatting.GRAY;
            case SHEPHERD -> Formatting.WHITE;
            case SMELTER -> Formatting.RED;
            case CARPENTER -> Formatting.GOLD;
            case MASON -> Formatting.DARK_PURPLE;
            case FARMER -> Formatting.YELLOW;
            case BUILDER -> Formatting.AQUA;
        };
    }
}
