package com.villagecolony.fabric.integration;

import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

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

            if (nameOf(villager) != null) {
                continue;
            }

            villager.setCustomName(Text.literal(labelFor(worker.profession().get())));
            villager.setCustomNameVisible(true);

            labelled++;
        }

        return labelled;
    }

    private static Text nameOf(Entity entity) {
        return entity.getCustomName();
    }

    /**
     * O nome que vai aparecer.
     *
     * <p>Em português porque é a língua do jogo do autor, e porque texto
     * literal não passa pelo sistema de tradução — ver a nota da classe.
     */
    private static String labelFor(ProfessionType profession) {
        return switch (profession) {
            case LUMBERJACK -> "Lenhador";
            case MANUFACTURER -> "Fabricante";
            case FARMER -> "Fazendeiro";
            case BUILDER -> "Construtor";
        };
    }
}
