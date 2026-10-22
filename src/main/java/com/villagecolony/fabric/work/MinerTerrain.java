package com.villagecolony.fabric.work;

import com.villagecolony.core.movement.Cell;
import com.villagecolony.core.movement.Terrain;
import com.villagecolony.core.type.ColonyPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;

import java.util.function.Function;

/**
 * O mundo como o mineiro o vê para se locomover — regra do autor de 2026-10-22: <i>"deve ser evitada a
 * construção de caminhos (adicionando blocos em formato de labirinto) para fora da montanha"</i>.
 *
 * <p>O vazio exposto ao céu (na altura da superfície da coluna ou acima) continua passável, mas deixa
 * de receber bloco ({@link Cell#OPEN} vira {@link Cell#PASSAGE}): o desvio não põe piso no ar para
 * atravessar vão ou subir duna. Dentro da montanha — túnel, galeria, caverna — o piso que falta ainda
 * se tapa. A outra metade da regra (descer antes de ir de lado) é o custo do desvio do mineiro,
 * {@link com.villagecolony.core.movement.DetourMoves.Costs#MINER}.
 */
final class MinerTerrain implements Terrain {

    private final ServerWorld world;

    private final Terrain base;

    private MinerTerrain(ServerWorld world, Terrain base) {
        this.world = world;
        this.base = base;
    }

    /** O terreno de sempre ({@link WorldTerrain}) com a regra do mineiro. */
    static MinerTerrain of(ServerWorld world) {
        return new MinerTerrain(world, new WorldTerrain(world));
    }

    /**
     * Só cavar: nenhum vazio recebe bloco, nem dentro da montanha — o acesso da vila até a mina
     * ({@link MineAccess}, decisão do autor de 2026-10-22).
     */
    static Terrain digOnly(ServerWorld world) {
        Terrain base = new WorldTerrain(world);

        return pos -> {
            Cell cell = base.at(pos);

            return cell == Cell.OPEN ? Cell.PASSAGE : cell;
        };
    }

    /** Qualquer terreno, com a regra do mineiro por cima. */
    static Function<ServerWorld, Terrain> over(Function<ServerWorld, Terrain> base) {
        return world -> new MinerTerrain(world, base.apply(world));
    }

    @Override
    public Cell at(ColonyPos pos) {
        Cell cell = base.at(pos);

        return cell == Cell.OPEN && isOutsideTheMountain(pos) ? Cell.PASSAGE : cell;
    }

    /** Céu aberto: nada sólido acima desta posição na coluna. */
    boolean isOutsideTheMountain(ColonyPos pos) {
        return pos.y() >= world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.x(), pos.z());
    }
}
