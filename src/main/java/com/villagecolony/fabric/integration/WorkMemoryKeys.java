package com.villagecolony.fabric.integration;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/** As chaves do save das memórias de trabalho — ADR-039 C. Chave estragada é ignorada. */
public final class WorkMemoryKeys {

    private WorkMemoryKeys() {
    }

    /** O identificador escrito na chave, ou nulo se ela não for um. */
    public static @Nullable UUID uuid(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }
}
