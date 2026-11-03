package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import java.util.Objects;

/** Volume inclusivo de região ou lote supervisionado; não implica autorização — ADR-044. */
public record WorkArea(ColonyPos min, ColonyPos max) {
    public WorkArea {
        Objects.requireNonNull(min);
        Objects.requireNonNull(max);
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("inverted work area");
        }
        if (Math.abs((long) min.x()) > 29_999_000 || Math.abs((long) max.x()) > 29_999_000
                || Math.abs((long) min.z()) > 29_999_000 || Math.abs((long) max.z()) > 29_999_000
                || min.y() < -2048 || max.y() > 2047) {
            throw new IllegalArgumentException("work area outside supported coordinates");
        }
    }

    public boolean contains(ColonyPos pos) {
        return pos.x() >= min.x() && pos.x() <= max.x()
                && pos.y() >= min.y() && pos.y() <= max.y()
                && pos.z() >= min.z() && pos.z() <= max.z();
    }

    public boolean contains(WorkArea other) {
        return contains(other.min) && contains(other.max);
    }

    public boolean intersects(WorkArea other) {
        return min.x() <= other.max.x() && max.x() >= other.min.x()
                && min.y() <= other.max.y() && max.y() >= other.min.y()
                && min.z() <= other.max.z() && max.z() >= other.min.z();
    }

    public long volume() {
        return Math.multiplyExact(Math.multiplyExact((long) max.x() - min.x() + 1,
                (long) max.y() - min.y() + 1), (long) max.z() - min.z() + 1);
    }
}
