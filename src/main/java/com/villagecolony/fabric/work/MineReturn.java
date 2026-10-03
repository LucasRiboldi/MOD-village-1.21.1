package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.model.VillageBounds;
import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.Terrain;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * O encalhado volta por onde desceu — pedido do autor, 2026-10-02.
 *
 * <p><b>O que se via.</b> Na sessão das 01:36, três mineiros desceram pela
 * passagem da mina até y=10, não alcançaram a pedra lá embaixo (caverna com
 * lava), congelaram e foram dados como encalhados — sete vezes. A fuga subia
 * reto e saía no meio da vila, em {@code -240, 65, 384}, furando o chão dela.
 * O caminho de volta existia: ele tinha acabado de descer por ele.
 *
 * <p><b>O pedido.</b> <i>"Ele não pode quebrar os blocos base da vila ao
 * subir; ele precisa criar caminhos de retorno que encontrem corredores e
 * escadas que já existam, para que possa subir por onde desceu (…) tirar e
 * colocar bloco em busca de retornar ao caminho já criado."</i>
 *
 * <p><b>Como.</b>
 * <ol>
 *   <li><b>Rastro.</b> A cada passagem (um segundo), cada trabalhador abaixo do
 *       nível do terreno guarda onde pisou. Na superfície o rastro recomeça:
 *       ele é sempre o caminho da boca da mina até onde ele está.
 *   <li><b>Refazer o rastro.</b> Encalhado, ele volta pelo rastro em trechos de
 *       {@link #SEGMENT} pisadas, pelo {@link DetourPlanner} — andar é o mais
 *       barato, então ele segue o corredor; cava ou põe bloco só para
 *       reencontrá-lo.
 *   <li><b>A casca da vila.</b> Dentro da caixa da vila, os {@link #SHELL_DEPTH}
 *       blocos de cima do terreno não se cavam nem recebem bloco — nem no
 *       retorno, nem na subida de reserva do {@link ClimbOut}.
 * </ol>
 *
 * <p>Sem rastro (mundo recém-carregado) ou sem volta, a subida de sempre segue,
 * mirando a borda da caixa em vez do centro da vila, e o retorno é tentado de
 * novo a cada {@link #RETRY_PASSES} passagens.
 */
public final class MineReturn {

    static {
        ServerMemory.register(MineReturn.class, MineReturn::clearAll);
    }

    /** Quantos blocos de cima do terreno da vila são intocáveis para quem sobe. */
    static final int SHELL_DEPTH = 4;

    /** Quantas pisadas do rastro cada trecho da volta cobre. */
    static final int SEGMENT = 10;

    /** O tamanho máximo do rastro guardado. */
    static final int MAX_TRAIL = 2_048;

    /** Passagens até tentar de novo um retorno que falhou. */
    static final int RETRY_PASSES = 30;

    /** Trechos seguidos sem avançar no rastro antes de dar o retorno como falho. */
    private static final int MAX_STALLS = 3;

    /** A margem fora da caixa para onde a subida de reserva mira. */
    private static final int OUTSIDE_MARGIN = 4;

    private static final class Trail {

        final List<BlockPos> cells = new ArrayList<>();

        final Map<BlockPos, Integer> index = new HashMap<>();

        void reset(BlockPos entrance) {
            cells.clear();
            index.clear();
            add(entrance);
        }

        void add(BlockPos cell) {
            Integer seen = index.get(cell);

            if (seen != null) {
                // Voltou a um lugar já pisado: o laço some do rastro.
                for (int i = cells.size() - 1; i > seen; i--) {
                    index.remove(cells.remove(i));
                }

                return;
            }

            if (cells.size() < MAX_TRAIL) {
                index.put(cell, cells.size());
                cells.add(cell);
            }
        }
    }

    private static final class Retrace {

        final List<BlockPos> cells;

        final Map<BlockPos, Integer> index;

        /** A pisada do rastro mais perto da boca já alcançada. */
        int at;

        DetourWalker walker;

        int stalls;

        int cooldown;

        Retrace(Trail trail) {
            this.cells = List.copyOf(trail.cells);
            this.index = Map.copyOf(trail.index);
            this.at = cells.size() - 1;
        }
    }

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();

    private static final Map<UUID, Retrace> RETRACES = new HashMap<>();

    private MineReturn() {
    }

    /** Uma passagem: cada trabalhador solto, carregado e de pé deixa a pisada no rastro. */
    static void record(ServerWorld world) {
        for (Worker worker : List.copyOf(VillageColonyMod.WORKERS.all())) {
            UUID id = worker.villagerId();

            if (StrandedWorkers.isStranded(id) || !(world.getEntity(id) instanceof VillagerEntity villager)
                    || !villager.isOnGround()) {
                continue;
            }

            walked(id, villager.getBlockPos().toImmutable(), StrandedEscape.isOut(world, villager.getBlockPos()));
        }
    }

    /** Uma pisada: na superfície, o rastro recomeça dela. */
    static void walked(UUID id, BlockPos feet, boolean atTheSurface) {
        Trail trail = TRAILS.computeIfAbsent(id, ignored -> new Trail());

        if (atTheSurface || trail.cells.isEmpty()) {
            trail.reset(feet);
        } else {
            trail.add(feet);
        }
    }

    /**
     * Uma passagem do encalhado.
     *
     * @return se o retorno cuida dele agora; falso deixa a subida de reserva agir
     */
    static boolean pass(ServerWorld world, VillagerEntity villager, UUID id) {
        Retrace retrace = RETRACES.get(id);

        if (retrace == null) {
            Trail trail = TRAILS.get(id);

            if (trail == null || trail.cells.size() < 2) {
                return false;
            }

            retrace = new Retrace(trail);
            RETRACES.put(id, retrace);

            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} goes back the way it came — {} steps of trail up to the mine entrance at {}",
                    shortId(id), retrace.cells.size(), retrace.cells.get(0).toShortString());
        }

        if (retrace.cooldown > 0) {
            retrace.cooldown--;

            return false;
        }

        if (retrace.walker != null) {
            return true;
        }

        BlockPos feet = villager.getBlockPos();
        Integer here = retrace.index.get(feet);

        if (here != null) {
            retrace.at = Math.min(retrace.at, here);
        }

        if (retrace.at == 0) {
            // Na boca da mina e ainda não "fora": a subida de reserva termina o caminho.
            return false;
        }

        int aim = Math.max(0, retrace.at - SEGMENT);
        Set<BlockPos> goals = new HashSet<>(retrace.cells.subList(0, aim + 1));

        Optional<DetourWalker> walker = DetourWalker.plan(world, id, feet, retrace.cells.get(aim),
                goals::contains, Set.of(), true, MineReturn::sealedTerrain, true);

        if (walker.isEmpty()) {
            fail(id, retrace, "no way back to the trail near " + retrace.cells.get(aim).toShortString());

            return false;
        }

        retrace.walker = walker.get();

        return true;
    }

    /** Um tique do trecho em curso. */
    static void drive(ServerWorld world, VillagerEntity villager, UUID id, ColonyPos chest) {
        Retrace retrace = RETRACES.get(id);

        if (retrace == null || retrace.walker == null) {
            return;
        }

        DetourWalker.Status status = retrace.walker.tick(world, villager, chest);

        if (status == DetourWalker.Status.WALKING) {
            return;
        }

        DetourWalker done = retrace.walker;

        retrace.walker = null;

        if (status == DetourWalker.Status.FAILED) {
            fail(id, retrace, done.why());

            return;
        }

        Integer reached = retrace.index.get(villager.getBlockPos());

        if (reached != null && reached < retrace.at) {
            retrace.at = reached;
            retrace.stalls = 0;

            VillageColonyMod.LOGGER.info(
                    "Stranded worker {} is back on its trail at {} — {} steps from the mine entrance ({} dug, {} placed)",
                    shortId(id), villager.getBlockPos().toShortString(), reached, done.dug(), done.placed());
        } else if (++retrace.stalls >= MAX_STALLS) {
            fail(id, retrace, MAX_STALLS + " stretches without getting closer to the entrance");
        }
    }

    /** Se um trecho do retorno está andando agora. */
    static boolean isDriving(UUID id) {
        Retrace retrace = RETRACES.get(id);

        return retrace != null && retrace.walker != null;
    }

    /** Em que pé está o retorno, para o teste dizer onde parou. */
    static String stateOf(UUID id) {
        Retrace retrace = RETRACES.get(id);

        return retrace == null ? "none"
                : "at step " + retrace.at + " of " + retrace.cells.size()
                        + (retrace.walker != null ? ", walking" : "")
                        + (retrace.cooldown > 0 ? ", waiting " + retrace.cooldown : "");
    }

    private static void fail(UUID id, Retrace retrace, String why) {
        retrace.walker = null;
        retrace.stalls = 0;
        retrace.cooldown = RETRY_PASSES;

        VillageColonyMod.LOGGER.info(
                "Stranded worker {} cannot go back the way it came for now ({}) — climbing meanwhile,"
                        + " trying again in {} s",
                shortId(id), why, RETRY_PASSES);
    }

    /**
     * Se esta posição é da casca da vila: dentro da caixa medida de uma
     * colônia, nos {@link #SHELL_DEPTH} blocos de cima do terreno, ou acima
     * dele.
     */
    static boolean isVillageShell(ServerWorld world, BlockPos pos) {
        for (Colony colony : VillageColonyMod.COLONIES.all()) {
            Optional<VillageBounds> bounds = colony.bounds();

            if (bounds.isEmpty() || !insideColumn(bounds.get(), pos)) {
                continue;
            }

            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());

            if (pos.getY() >= top - SHELL_DEPTH) {
                return true;
            }
        }

        return false;
    }

    private static boolean insideColumn(VillageBounds box, BlockPos pos) {
        return pos.getX() >= box.minX() && pos.getX() <= box.maxX()
                && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ();
    }

    /** O mundo do retorno: a casca da vila não se cava nem recebe bloco. */
    static Terrain sealedTerrain(ServerWorld world) {
        WorldTerrain base = new WorldTerrain(world);
        Map<ColonyPos, Cell> seen = new HashMap<>();

        return pos -> seen.computeIfAbsent(pos, at -> {
            Cell cell = base.at(at);

            if ((cell == Cell.ROCK || cell == Cell.OPEN)
                    && isVillageShell(world, MinecraftTypeAdapter.toBlockPos(at))) {
                return cell == Cell.ROCK ? Cell.FIRM : Cell.PASSAGE;
            }

            return cell;
        });
    }

    /**
     * Para onde a subida de reserva mira: com a caixa medida, para fora dela
     * pela borda mais perto — subir na direção do centro saía no meio da vila.
     */
    static BlockPos homeFor(Colony colony, BlockPos feet) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(colony.center());
        Optional<VillageBounds> bounds = colony.bounds();

        if (bounds.isEmpty()) {
            return center;
        }

        VillageBounds box = bounds.get();

        if (!insideColumn(box, feet)) {
            // Já fora da caixa: para longe dela.
            return feet.add(Integer.signum(feet.getX() - center.getX()) * 16, 0,
                    Integer.signum(feet.getZ() - center.getZ()) * 16);
        }

        int west = feet.getX() - box.minX();
        int east = box.maxX() - feet.getX();
        int north = feet.getZ() - box.minZ();
        int south = box.maxZ() - feet.getZ();
        int nearest = Math.min(Math.min(west, east), Math.min(north, south));

        if (nearest == west) {
            return new BlockPos(box.minX() - OUTSIDE_MARGIN, feet.getY(), feet.getZ());
        }

        if (nearest == east) {
            return new BlockPos(box.maxX() + OUTSIDE_MARGIN, feet.getY(), feet.getZ());
        }

        return nearest == north
                ? new BlockPos(feet.getX(), feet.getY(), box.minZ() - OUTSIDE_MARGIN)
                : new BlockPos(feet.getX(), feet.getY(), box.maxZ() + OUTSIDE_MARGIN);
    }

    /**
     * Os rastros de agora, em posições compactadas, para o save — F-3,
     * 2026-10-02. Só o rastro; o retorno em curso recomeça do rastro ao carregar.
     */
    public static Map<UUID, long[]> snapshot() {
        Map<UUID, long[]> out = new HashMap<>();

        TRAILS.forEach((id, trail) -> {
            if (trail.cells.size() >= 2) {
                out.put(id, trail.cells.stream().mapToLong(BlockPos::asLong).toArray());
            }
        });

        return out;
    }

    /** Os rastros que voltam do save, na ordem em que foram pisados. */
    public static void restore(Map<UUID, long[]> saved) {
        saved.forEach((id, cells) -> {
            Trail trail = new Trail();

            for (long cell : cells) {
                trail.add(BlockPos.fromLong(cell));
            }

            if (trail.cells.size() >= 2) {
                TRAILS.put(id, trail);
            }
        });
    }

    /** Ele saiu: o retorno acaba, e o rastro recomeça da superfície. */
    static void finish(UUID id) {
        RETRACES.remove(id);
        TRAILS.remove(id);
    }

    static void forget(UUID id) {
        RETRACES.remove(id);
        TRAILS.remove(id);
    }

    static void clearAll() {
        RETRACES.clear();
        TRAILS.clear();
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
