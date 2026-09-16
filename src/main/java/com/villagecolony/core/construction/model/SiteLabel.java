package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

import java.util.Map;
import java.util.Objects;

/**
 * A linha que flutua sobre o lote — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"precisa sinalizar um texto igual o nome
 * dos aldeoes mostrando o cone do material que falta, quantos tem em
 * estoque e quantos falta para a construcao finalizar"</i>.
 *
 * <p><b>O log tinha a resposta e ele não.</b> Na sessão de 23:41 a obra em
 * {@code 2503,63,-3031} repetia, a cada trinta segundos,
 * <i>"WAITING_RESOURCES, 382 blocks left, waiting for
 * minecraft:grass_block"</i> — com o estoque da colônia em
 * {@code {OAK_LOG=21, OAK_PLANKS=86, COBBLESTONE=96, DIRT=17}}, zero
 * grama. Dentro do jogo, nada disso aparecia: o autor via partículas de
 * fumaça e não tinha como saber o que a obra esperava.
 *
 * <p><b>Texto, e não ícone de item.</b> O pedido falava em ícone, e um
 * ícone exigiria renderização própria no cliente — mixin, networking,
 * entrypoint de cliente, e o mod deixaria de funcionar com cliente Vanilla
 * puro. O nome do bloco cabe na mesma placa, é lido de longe e não custa
 * nada disso. Ver {@code WorkerNameplate}, que escolheu assim pelo mesmo
 * motivo em 2026-08-08.
 *
 * <p><b>Um material por vez</b>, e é decisão: a obra pode estar esperando
 * cinco coisas, e listar todas viraria uma parede de texto sobre o lote. O
 * primeiro da lista é o que o construtor vai buscar em seguida, que é o
 * que destrava a obra.
 *
 * <p>Mora no Core e se afirma sem mundo, como {@link SiteOutline}: o que
 * costuma ter defeito aqui é a conta e o nome, não o desenho.
 */
public final class SiteLabel {

    /**
     * Por onde se reconhece uma placa de obra.
     *
     * <p>O {@code WorkerNameplate} só desfaz nome que o mod escreveu — a
     * Regra 3 aplicada ao nome do aldeão —, e ele o reconhece comparando
     * com os rótulos de profissão. A placa não é um deles, e sem esta
     * marca ela seria tomada por nome que o jogador deu: o construtor
     * ficaria com "Falta grass_block..." sobre a cabeça <b>para sempre</b>,
     * porque o mod se recusaria a mexer.
     *
     * <p>Público para o {@code WorkerNameplate} poder perguntar.
     */
    public static final String MARK = "Obra";

    private SiteLabel() {
    }

    /**
     * A linha da placa desta obra.
     *
     * @param missing o que ainda falta pôr, por bloco — normalmente
     *     {@code ConstructionProject.remainingMaterials()}
     * @param stock o que a colônia tem nos baús
     * @param blocksLeft quantos blocos faltam para a casa ficar pronta
     * @return uma linha curta, pronta para virar nome flutuante
     */
    public static String of(
            Map<ResourceId, Integer> missing,
            Map<ResourceId, Integer> stock,
            int blocksLeft) {

        Objects.requireNonNull(missing, "missing");
        Objects.requireNonNull(stock, "stock");

        if (missing.isEmpty()) {
            // Nada a esperar: a obra está andando, e o número que importa
            // é só quanto falta dela.
            return MARK + ": " + blocksLeft + " blocos";
        }

        Map.Entry<ResourceId, Integer> first = missing.entrySet().iterator().next();

        ResourceId material = first.getKey();

        // Zero aparece, e não some — ver o teste. Material ausente do
        // estoque é exatamente o caso que trava a obra, e uma placa que o
        // omitisse deixaria o autor sem saber se falta o material ou se a
        // placa não o conhece.
        int have = stock.getOrDefault(material, 0);

        // Só o path: "minecraft:" gasta dez caracteres dizendo o que o
        // jogador já sabe, e a placa é para ler de longe.
        return MARK + " · falta " + material.path() + ": " + have + "/" + first.getValue()
                + " · " + blocksLeft + " blocos";
    }
}
