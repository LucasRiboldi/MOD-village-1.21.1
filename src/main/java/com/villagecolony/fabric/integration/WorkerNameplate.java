package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.SiteLabel;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Collection;

/**
 * Limpa nomes antigos de profissão para o overlay ocupar sozinho esse lugar.
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
 * <p>A profissão não mora mais no {@code customName}: quem tem o mod vê a
 * placa pixelada do cliente, com o texto dentro do fundo e o ícone à direita.
 * Este serviço ficou só como faxina de saves antigos, para tirar nomes que a
 * colônia escreveu antes dessa decisão sem apagar nome dado pelo jogador.
 */
public final class WorkerNameplate {

    private WorkerNameplate() {
    }

    /**
     * Remove dos trabalhadores os nomes de profissão escritos pelo mod.
     *
     * <p>Nunca sobrescreve nome que já existe. Aldeão batizado com
     * etiqueta pelo jogador continua com o nome que ele deu — o mod não
     * tem direito sobre isso, e perder um nome dado à mão é o tipo de
     * dano que não se desfaz.
     *
     * <p>Trabalhador sem profissão fica sem nome. Bebê e nitwit são o
     * caso comum, e nomeá-los de "trabalhador" diria algo falso.
     *
     * <p><b>E quem PERDEU a profissão perde o nome junto</b> — 2026-09-11,
     * e até esta data o nome mentia. O laço abaixo <i>pulava</i> quem não
     * tinha profissão, o que resolve o bebê — ele nunca teve nome — e
     * deixa intacto o caso que importa: o trabalhador que largou o
     * ofício continuava com a plaquinha do ofício que largou.
     *
     * <p>E ficar sem função não é acidente, é <b>projeto</b>: o
     * {@code ProfessionAssigner.vacancyFor} diz que, esgotados os ofícios
     * que ele não está evitando, <i>"ele fica sem função por algumas
     * passagens, que é o piso desta linha"</i>. Some a isso o
     * {@code WorkerStrikes}, que desde 09-10 tira do ofício quem desiste
     * três vezes, e o resultado é o que o autor viu na sessão de 00:04:
     * <b>aldeões parados com nome de profissão, aglomerados na mina</b>,
     * cinco deles tendo largado o ofício ali mesmo.
     *
     * <p>A plaquinha é a única coisa que o jogador tem para saber quem é
     * quem, e um nome que mente é pior que nome nenhum: ele faz procurar
     * defeito no mineiro que não existe mais.
     *
     * @return quantos nomes foram tirados ou escondidos agora
     */
    public static int label(ServerWorld world, Collection<Worker> workers) {
        int labelled = 0;

        for (Worker worker : workers) {
            if (!(world.getEntity(worker.villagerId()) instanceof VillagerEntity villager)) {
                continue;
            }

            Text current = nameOf(villager);

            if (current != null && !isColonyLabel(current)) {
                // Nome que o jogador deu. A Regra 3 vale aqui como vale
                // na mão do aldeão — e vale também para tirar: o mod só
                // desfaz o que o mod escreveu.
                continue;
            }

            if (current != null) {
                // A profissão agora pertence ao overlay. O nome Vanilla só
                // conserva nomes do jogador; rótulos antigos do mod somem.
                villager.setCustomName(null);
                villager.setCustomNameVisible(false);
                labelled++;
                continue;
            }

            if (villager.isCustomNameVisible()) {
                villager.setCustomNameVisible(false);
                labelled++;
            }
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

        // <b>A placa da obra passou por aqui</b>, entre 09-15 e 09-16. Ela
        // morava no nome do construtor, e o autor a recusou: <i>"deve ficar
        // flutuando no espaço da construção e não no lugar do nome do
        // trabalhador"</i>. Hoje ela é um suporte de armadura sobre o lote
        // — ver SiteMarker —, e o nome do trabalhador voltou a dizer só o
        // ofício dele.
        //
        // A linha fica como reconhecimento de nome antigo: quem carregar um
        // save feito naquela janela tem construtores com a placa na cabeça,
        // e sem isto o mod se recusaria a desfazê-la para sempre.
        if (written.startsWith(SiteLabel.MARK)) {
            return true;
        }

        if ("Criador".equals(written)) {
            return true;
        }

        for (ProfessionType profession : ProfessionType.values()) {
            if (nameFor(profession).equals(written)) {
                return true;
            }
        }

        return false;
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
}
