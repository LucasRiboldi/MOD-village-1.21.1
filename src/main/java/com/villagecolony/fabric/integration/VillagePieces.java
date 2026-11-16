package com.villagecolony.fabric.integration;

import com.villagecolony.core.construction.model.VillageBlend;
import com.villagecolony.core.construction.model.VillageSurface;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.math.BlockBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * As peças rígidas da vila (casas, postes, enfeites) assentadas no solo suavizado — Regra 296.
 *
 * <p>Antes de a vila ser posta, cada peça rígida desce ou sobe até o piso que {@link VillageBlend#settle} dá a ela, e o
 * piso entra na conta do terreno como coluna fixa. O deslocamento vai para a própria peça ({@code translate}), que o
 * jogo grava no chunk: depois de reiniciar, a conta refeita sobre o mesmo ruído dá deslocamento zero.
 */
final class VillagePieces {

    private VillagePieces() {
    }

    /** Uma peça rígida da vila: a peça do jogo, a pegada na grade e o piso. */
    private record Rigid(PoolStructurePiece piece, VillageBlend.Area footprint, int floor) {
    }

    /**
     * Assenta as peças rígidas, marca o piso delas como fixo em {@code natural} e {@code kinds}, e devolve o trecho do
     * {@code VC_GROUND} com as caixas fixas e quanto as peças andaram.
     */
    static String settle(StructureStart start, int[][] natural, VillageSurface.Kind[][] kinds, VillageBlend.Area core,
                         VillageBlend.Area reach, int minX, int minZ) {
        return settle(start, natural, kinds, core, reach, minX, minZ, null, null, new java.util.HashMap<>());
    }

    /**
     * O mesmo, e a frente de cada porta das peças, fora delas, fica fixa no piso da porta e entra em {@code doorFronts}
     * (Regra 303); sem moldes ({@code templates} nulo), nenhuma porta é lida.
     */
    static String settle(StructureStart start, int[][] natural, VillageSurface.Kind[][] kinds, VillageBlend.Area core,
                         VillageBlend.Area reach, int minX, int minZ,
                         net.minecraft.structure.StructureTemplateManager templates,
                         net.minecraft.registry.DynamicRegistryManager registries, java.util.Map<Long, Integer> doorFronts) {
        List<Rigid> rigid = new ArrayList<>();

        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof PoolStructurePiece pool
                    && pool.getPoolElement().getProjection() == StructurePool.Projection.RIGID) {
                BlockBox box = piece.getBoundingBox();
                rigid.add(new Rigid(pool, new VillageBlend.Area(box.getMinX() - minX, box.getMinZ() - minZ,
                        box.getMaxX() - minX, box.getMaxZ() - minZ), floorOf(pool)));
            }
        }

        int[] floors = VillageBlend.settle(natural, kinds, core, reach, rigid.stream().map(Rigid::footprint).toList(),
                rigid.stream().mapToInt(Rigid::floor).toArray());
        int moved = 0;
        int most = 0;

        for (int i = 0; i < rigid.size(); i++) {
            int shift = floors[i] - rigid.get(i).floor();

            if (shift != 0) {
                rigid.get(i).piece().translate(0, shift, 0);
                moved++;
                most = Math.max(most, Math.abs(shift));
            }
        }

        // A maior marca o chão primeiro; a de dentro dela (o aldeão do molde) não sobrescreve o piso da casa.
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < rigid.size(); i++) {
            order.add(i);
        }

        order.sort(Comparator.comparingInt((Integer i) -> -cells(rigid.get(i).footprint())).thenComparingInt(i -> i));
        boolean[][] marked = new boolean[natural.length][natural[0].length];
        StringBuilder fixed = new StringBuilder();

        for (int i : order) {
            VillageBlend.Area area = rigid.get(i).footprint();

            for (int x = area.minX(); x <= area.maxX(); x++) {
                for (int z = area.minZ(); z <= area.maxZ(); z++) {
                    if (!marked[x][z]) {
                        marked[x][z] = true;
                        natural[x][z] = floors[i];
                        kinds[x][z] = VillageSurface.Kind.FIXED;
                    }
                }
            }

            fixed.append(fixed.length() == 0 ? "" : ",").append('[').append(area.minX() + minX).append(',')
                    .append(area.minZ() + minZ).append(',').append(area.maxX() + minX).append(',')
                    .append(area.maxZ() + minZ).append(']');
        }

        int doors = 0;

        for (int i = 0; templates != null && i < rigid.size(); i++) {
            for (var front : DoorFronts.of(rigid.get(i).piece(), templates, registries).entrySet()) {
                int x = com.villagecolony.core.construction.model.ColonyRoads.xOf(front.getKey()) - minX;
                int z = com.villagecolony.core.construction.model.ColonyRoads.zOf(front.getKey()) - minZ;

                if (x >= 0 && z >= 0 && x < natural.length && z < natural[0].length && !marked[x][z]) {
                    natural[x][z] = front.getValue();
                    kinds[x][z] = VillageSurface.Kind.FIXED;
                    doorFronts.put(front.getKey(), front.getValue());
                    doors++;
                }
            }
        }

        return "\"moved\":" + moved + ",\"most\":" + most + ",\"doors\":" + doors + ",\"fixed\":[" + fixed + "]";
    }

    /** O piso da peça: o bloco de baixo do nível de chão dela. */
    private static int floorOf(PoolStructurePiece piece) {
        return piece.getBoundingBox().getMinY() + piece.getGroundLevelDelta() - 1;
    }

    private static int cells(VillageBlend.Area area) {
        return (area.maxX() - area.minX() + 1) * (area.maxZ() - area.minZ() + 1);
    }
}
