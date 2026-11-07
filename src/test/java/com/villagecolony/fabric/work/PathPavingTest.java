package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathPavingTest {

    @Test
    void theBuilderTheMasonAndWhoHasNoTradePave() {
        assertEquals(
                List.of(Optional.of(ProfessionType.BUILDER), Optional.of(ProfessionType.MASON), Optional.empty()),
                PathPaving.PAVERS,
                "Regras 215 e 220");
    }
}
