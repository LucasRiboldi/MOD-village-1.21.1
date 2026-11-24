package com.villagecolony.fabric.work;

import com.villagecolony.core.type.ResourceId;
import net.minecraft.entity.EntityType;

import java.util.Locale;
import java.util.Optional;

/** O modelo registrado define quais animais compartilham cada curral médio. */
public enum MediumPenType {
    CATTLE, SHEEP, PIG, POULTRY, HORSE, CAMELID;

    public ResourceId model() {
        return new ResourceId("villagecolony", "colony/medium_animal_pen_" + name().toLowerCase(Locale.ROOT));
    }

    boolean accepts(EntityType<?> animal) {
        return switch (this) {
            case CATTLE -> animal == EntityType.COW || animal == EntityType.MOOSHROOM;
            case SHEEP -> animal == EntityType.SHEEP;
            case PIG -> animal == EntityType.PIG;
            case POULTRY -> animal == EntityType.CHICKEN || animal == EntityType.RABBIT;
            case HORSE -> animal == EntityType.HORSE;
            case CAMELID -> animal == EntityType.LLAMA || animal == EntityType.GOAT;
        };
    }

    static Optional<MediumPenType> fromModel(ResourceId id) {
        for (var type : values()) {
            if (type.model().equals(id)) return Optional.of(type);
        }
        return Optional.empty();
    }
}
