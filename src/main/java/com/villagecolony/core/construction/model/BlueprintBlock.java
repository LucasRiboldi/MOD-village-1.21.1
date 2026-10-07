package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.type.Side;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Um bloco de um projeto: onde vai, e o que é.
 *
 * <p>A posição é <b>relativa</b> à origem do projeto, e é o que permite
 * ao mesmo {@link Blueprint} servir para toda casa que a colônia
 * levantar. Somar a origem escolhida é trabalho de quem executa a obra.
 *
 * <p>Guarda o bloco por nome ({@link ResourceId}), um dos quatro lados
 * horizontais (ADR-008) e, desde a ADR-036 item 19, eixo, metade e formato
 * ({@link ShapeStates}). As demais propriedades continuam do Minecraft.
 *
 * @param offset posição relativa à origem do projeto
 * @param block o bloco a colocar ali
 * @param states eixo, metade e formato que a planta pede ({@link ShapeStates})
 */
public record BlueprintBlock(
        ColonyPos offset, ResourceId block, boolean furniture, Optional<Side> facing,
        Map<String, String> states) {

    /** Sem eixo, metade nem formato declarados. */
    public BlueprintBlock(ColonyPos offset, ResourceId block, boolean furniture, Optional<Side> facing) {
        this(offset, block, furniture, facing, Map.of());
    }

    /** Compatibilidade para plantas que nao declaram orientacao. */
    public BlueprintBlock(ColonyPos offset, ResourceId block, boolean furniture) {
        this(offset, block, furniture, Optional.empty());
    }

    /**
     * Um bloco de estrutura, que é o caso comum.
     *
     * <p>Parede, teto e porta: sem eles não há casa, e a obra espera
     * pelo material deles.
     */
    public BlueprintBlock(ColonyPos offset, ResourceId block) {
        this(offset, block, false, Optional.empty());
    }

    /**
     * Um bloco de mobília — a Regra 21.
     *
     * <p>Cama, baú e lampião. A diferença com a estrutura é o que
     * acontece quando falta material: a obra <b>não espera</b> por
     * mobília. Ela termina sem, e a peça entra depois, quando o material
     * aparecer num baú.
     *
     * <p>A razão é a Regra 13. Dos três, a colônia só sabe fazer o baú:
     * a cama pede lã e o lampião pede ferro, e nenhum aldeão deste mod
     * tosquia ou minera. Exigi-los para dar a casa por pronta faria
     * nenhuma casa terminar, e a vila pararia de crescer — que é
     * exatamente o travamento que a Regra 13 corrigiu.
     */
    public static BlueprintBlock furniture(ColonyPos offset, ResourceId block) {
        return new BlueprintBlock(offset, block, true, Optional.empty());
    }

    public BlueprintBlock {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(facing, "facing");
        states = Map.copyOf(Objects.requireNonNull(states, "states"));
    }
}
