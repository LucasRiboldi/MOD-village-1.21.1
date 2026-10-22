package com.villagecolony.core.coordination;

import com.villagecolony.core.type.ColonyPos;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleBiFunction;

/**
 * A cama mais perto do trabalho — Regra 74, autor de 2026-10-22: <i>"fazer a troca da cama destinada dos
 * aldeões para a cama que exista mais próxima do local onde ele trabalha, para não precisar caminhar
 * longas distâncias"</i>.
 *
 * <p>Escolhas do autor: o trabalho é onde o aldeão trabalhou de fato; a cama ocupada só troca de dono se a
 * soma das duas caminhadas cai e nenhum dos dois passa a andar mais; a vila reavalia uma vez por dia e só
 * muda quem ganha ao menos {@link #MIN_GAIN} blocos. Quem anda mais escolhe primeiro, e cada aldeão muda no
 * máximo uma vez por passada.
 */
public final class BedPlan {

    /** Quantos blocos mais perto do trabalho a cama nova precisa ficar para valer a mudança. */
    public static final int MIN_GAIN = 16;

    private BedPlan() {
    }

    /**
     * Quem dorme numa cama da vila.
     *
     * @param work o centro do trabalho dele; nulo para quem não tem ofício ou ainda não trabalhou
     * @param movable se pode mudar agora — quem dorme fica onde está, e a cama dele não se oferece
     */
    public record Sleeper(UUID id, ColonyPos bed, @Nullable ColonyPos work, boolean movable) {

        public Sleeper {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(bed, "bed");
        }
    }

    /** Uma mudança de cama. */
    public record Move(UUID id, ColonyPos from, ColonyPos to) {
    }

    /** A distância em linha reta, com a altura: subir e descer é caminhada também. */
    public static double distance(ColonyPos a, ColonyPos b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * As mudanças desta passada: cada uma para cama livre, ou a troca com o dono quando ele não perde.
     *
     * @param freeBeds as camas da vila sem dono
     * @return as mudanças; numa troca, as duas
     */
    public static List<Move> plan(Collection<Sleeper> sleepers, Collection<ColonyPos> freeBeds) {
        return plan(sleepers, freeBeds, BedPlan::distance);
    }

    /**
     * O mesmo, medindo com {@code walk} — a caminhada real do trabalho até a cama, quando o lado do jogo a
     * conhece (R-148 do estudo v2); infinita para a cama a que não se chega a pé.
     */
    public static List<Move> plan(Collection<Sleeper> sleepers, Collection<ColonyPos> freeBeds,
            ToDoubleBiFunction<ColonyPos, ColonyPos> walk) {
        Map<ColonyPos, Sleeper> owners = new HashMap<>();

        for (Sleeper sleeper : sleepers) {
            owners.put(sleeper.bed(), sleeper);
        }

        Set<ColonyPos> free = new LinkedHashSet<>(freeBeds);
        free.removeAll(owners.keySet());

        Set<UUID> moved = new HashSet<>();
        List<Move> moves = new ArrayList<>();

        List<Sleeper> walkers = sleepers.stream()
                .filter(sleeper -> sleeper.movable() && sleeper.work() != null)
                .sorted(Comparator.comparingDouble(
                        (Sleeper sleeper) -> walk.applyAsDouble(Objects.requireNonNull(sleeper.work()), sleeper.bed()))
                        .reversed())
                .toList();

        for (Sleeper walker : walkers) {
            if (moved.contains(walker.id())) {
                continue;
            }

            ColonyPos work = Objects.requireNonNull(walker.work());
            ColonyPos home = walker.bed();
            double today = walk.applyAsDouble(work, home);
            List<ColonyPos> candidates = new ArrayList<>(free);

            for (Sleeper owner : owners.values()) {
                if (!owner.id().equals(walker.id()) && owner.movable() && !moved.contains(owner.id())) {
                    candidates.add(owner.bed());
                }
            }

            candidates.sort(Comparator.comparingDouble((ColonyPos bed) -> walk.applyAsDouble(work, bed)));

            for (ColonyPos bed : candidates) {
                if (walk.applyAsDouble(work, bed) > today - MIN_GAIN) {
                    break;
                }

                Sleeper owner = owners.get(bed);

                if (owner == null) {
                    free.remove(bed);
                    free.add(home);
                    owners.remove(home);
                    owners.put(bed, walker);
                    moves.add(new Move(walker.id(), home, bed));
                    moved.add(walker.id());
                    break;
                }

                // O dono só cede a cama se não passa a andar mais: sem ofício, qualquer cama serve.
                if (owner.work() != null
                        && walk.applyAsDouble(owner.work(), home) > walk.applyAsDouble(owner.work(), bed)) {
                    continue;
                }

                owners.put(bed, walker);
                owners.put(home, owner);
                moves.add(new Move(walker.id(), home, bed));
                moves.add(new Move(owner.id(), bed, home));
                moved.add(walker.id());
                moved.add(owner.id());
                break;
            }
        }

        return List.copyOf(moves);
    }
}
