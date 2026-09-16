package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * As colunas da borda do lote — o marcador visual, 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"adicionar um marcador visual, um efeito
 * que demonstre onde no terreno está o espaço alocado para a construção
 * escolhida"</i>.
 *
 * <p>Ele entrou no jogo três sessões seguidas sem ver casa nascendo, e o
 * log dizia que a colônia planejava. Sem o marcador, <i>"não achou
 * lote"</i> e <i>"achou lote a setenta blocos daqui"</i> são a mesma coisa
 * vista de dentro do jogo — e foi exatamente essa confusão que custou a
 * sessão de 21:02, quando a obra estava aberta e inalcançável.
 *
 * <p><b>A geometria mora no Core de propósito.</b> Desenhar partícula
 * precisa de servidor; decidir <b>onde</b> desenhar é aritmética de dois
 * cantos, e é o que costuma ter defeito — um contorno que erra o canto
 * marca o lote errado e manda o autor procurar no lugar errado. Aqui ela
 * se afirma sem mundo, e {@code SiteOutlineTest} a afirma.
 *
 * <p><b>Só a borda, e só na altura do piso.</b> São duas decisões, e as
 * duas servem ao mesmo fim: o autor quer ver <b>o terreno</b>. Preencher o
 * miolo esconderia o chão que ele foi olhar, e subir até o telhado viraria
 * uma parede opaca em volta do lote.
 */
public final class SiteOutline {

    private SiteOutline() {
    }

    /**
     * As colunas da borda do retângulo entre estes dois cantos.
     *
     * <p>A altura de saída é a do canto {@code min} — o piso do lote. Os
     * cantos podem vir trocados: o que importa é o retângulo que eles
     * descrevem, e quem chama nem sempre sabe qual é o menor.
     *
     * @return uma coluna por posição de borda, sem repetição, do canto
     *     mínimo em diante
     */
    public static List<ColonyPos> of(ColonyPos one, ColonyPos other) {
        Objects.requireNonNull(one, "one");
        Objects.requireNonNull(other, "other");

        int minX = Math.min(one.x(), other.x());
        int maxX = Math.max(one.x(), other.x());
        int minZ = Math.min(one.z(), other.z());
        int maxZ = Math.max(one.z(), other.z());

        // O piso, e não a altura da casa — ver o javadoc da classe.
        int y = Math.min(one.y(), other.y());

        List<ColonyPos> border = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // A borda é quem toca alguma das quatro faces. O miolo
                // fica de fora, e é o que mantém o terreno à vista.
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    border.add(new ColonyPos(x, y, z));
                }
            }
        }

        return List.copyOf(border);
    }
}
