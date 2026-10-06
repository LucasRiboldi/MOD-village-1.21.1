package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

/** Classifica plantas sem depender da camada Fabric que as carrega. */
public final class BlueprintKind {

    private static final String COLONY_NAMESPACE = "villagecolony";
    private static final String COLONY_FOLDER = "colony/";

    private BlueprintKind() {
    }

    /** Se a planta e uma roca, e nao a casa de oficio do fazendeiro. */
    public static boolean isFarm(ResourceId id) {
        return id != null
                && id.path().contains("farm")
                && !(COLONY_NAMESPACE.equals(id.namespace()) && id.path().startsWith(COLONY_FOLDER));
    }
}
