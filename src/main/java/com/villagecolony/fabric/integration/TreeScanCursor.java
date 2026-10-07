package com.villagecolony.fabric.integration;

/** Posicao exata da proxima coluna na espiral quadrada da busca de arvores. */
record TreeScanCursor(int ring, int dx, int dz) {

    record Offset(int dx, int dz) {
    }

    static TreeScanCursor start() {
        return new TreeScanCursor(0, 0, 0);
    }

    Offset offset() {
        return new Offset(dx, dz);
    }

    TreeScanCursor advance() {
        if (ring == 0) {
            return new TreeScanCursor(1, -1, -1);
        }

        if (dx == -ring && dz < ring) {
            return new TreeScanCursor(ring, dx, dz + 1);
        }

        if (dx < ring) {
            if (dz == -ring) {
                return new TreeScanCursor(ring, dx, ring);
            }

            return new TreeScanCursor(ring, dx + 1, -ring);
        }

        if (dz < ring) {
            return new TreeScanCursor(ring, dx, dz + 1);
        }

        int next = ring + 1;

        return new TreeScanCursor(next, -next, -next);
    }
}
