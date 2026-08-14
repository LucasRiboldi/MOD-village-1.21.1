package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;

import java.util.Objects;

/**
 * Um bloco de um projeto: onde vai, e o que é.
 *
 * <p>A posição é <b>relativa</b> à origem do projeto, e é o que permite
 * ao mesmo {@link Blueprint} servir para toda casa que a colônia
 * levantar. Somar a origem escolhida é trabalho de quem executa a obra.
 *
 * <p>Guarda o bloco por nome ({@link ResourceId}) e não por estado
 * completo. O que se perde com isso é a orientação: uma escada olhando
 * para o norte e outra para o sul são o mesmo {@code oak_stairs} aqui
 * dentro. A perda é conhecida e assumida no MVP — a casa sai de pé, com
 * degrau apontando para o padrão. Guardar estado exigiria ou levar
 * {@code BlockState} para dentro do Core, contra a ADR-005, ou inventar
 * uma linguagem de propriedades no Core, que é trabalho de outra fase.
 *
 * @param offset posição relativa à origem do projeto
 * @param block o bloco a colocar ali
 */
public record BlueprintBlock(ColonyPos offset, ResourceId block) {

    public BlueprintBlock {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(block, "block");
    }
}
