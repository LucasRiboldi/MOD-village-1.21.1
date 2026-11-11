package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ResourceId;

/** Classifica plantas sem depender da camada Fabric que as carrega. */
public final class BlueprintKind {

    private static final String COLONY_NAMESPACE = "villagecolony";
    private static final String COLONY_FOLDER = "colony/";

    /** Na pasta do mod, roça é a planta que termina assim: a plantação grande (Regra 241). */
    private static final String COLONY_FARM_SUFFIX = "_farm";

    private BlueprintKind() {
    }

    /**
     * Se a planta é uma roça, e não a casa de ofício do fazendeiro. Na pasta {@code colony/} do mod, só a planta que termina
     * em {@value #COLONY_FARM_SUFFIX} é roça: a plantação grande constrói a base no lugar do chão, como a roça da vila
     * (Regra 241).
     */
    public static boolean isFarm(ResourceId id) {
        if (id == null || !id.path().contains("farm")) {
            return false;
        }

        boolean colonyPlan = COLONY_NAMESPACE.equals(id.namespace()) && id.path().startsWith(COLONY_FOLDER);

        return !colonyPlan || id.path().endsWith(COLONY_FARM_SUFFIX);
    }
}
