package com.villagecolony.fabric.integration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O cursor da busca de arvores nao repete colunas entre passagens. */
class TreeScanCursorTest {

    @Test
    void aPassResumesAtTheExactNextColumnInsideTheRing() {
        TreeScanCursor uninterrupted = TreeScanCursor.start();
        List<TreeScanCursor.Offset> expected = new ArrayList<>();

        for (int column = 0; column < 1_040; column++) {
            expected.add(uninterrupted.offset());
            uninterrupted = uninterrupted.advance();
        }

        TreeScanCursor firstPass = TreeScanCursor.start();

        for (int column = 0; column < 1_024; column++) {
            firstPass = firstPass.advance();
        }

        List<TreeScanCursor.Offset> resumed = new ArrayList<>();

        for (int column = 0; column < 16; column++) {
            resumed.add(firstPass.offset());
            firstPass = firstPass.advance();
        }

        assertEquals(expected.subList(1_024, 1_040), resumed);
        assertEquals(16, firstPass.ring(), "1.024 colunas terminam no meio do anel 16");
    }

    @Test
    void theOrderRemainsTheSameSquareSpiral() {
        TreeScanCursor cursor = TreeScanCursor.start();
        List<TreeScanCursor.Offset> offsets = new ArrayList<>();

        for (int column = 0; column < 9; column++) {
            offsets.add(cursor.offset());
            cursor = cursor.advance();
        }

        assertEquals(List.of(
                new TreeScanCursor.Offset(0, 0),
                new TreeScanCursor.Offset(-1, -1),
                new TreeScanCursor.Offset(-1, 0),
                new TreeScanCursor.Offset(-1, 1),
                new TreeScanCursor.Offset(0, -1),
                new TreeScanCursor.Offset(0, 1),
                new TreeScanCursor.Offset(1, -1),
                new TreeScanCursor.Offset(1, 0),
                new TreeScanCursor.Offset(1, 1)), offsets);
    }
}
