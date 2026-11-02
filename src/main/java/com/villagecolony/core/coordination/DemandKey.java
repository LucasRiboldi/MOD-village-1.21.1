package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;

import java.util.Objects;
import java.util.UUID;

/** Identifica uma demanda que falhou dentro de uma colônia. */
public record DemandKey(UUID colonyId, TaskType type, ResourceType resource) {

    public DemandKey {
        Objects.requireNonNull(colonyId, "colonyId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resource, "resource");
    }
}
