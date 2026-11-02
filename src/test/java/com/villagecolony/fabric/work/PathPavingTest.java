package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathPavingTest {

    @Test
    void onlyTheBuilderPavesPathLinks() {
        assertEquals(List.of(ProfessionType.BUILDER), PathPaving.PAVERS);
    }
}
