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
        return settle(start, natural, kinds, core, reach, minX, minZ, null, null, new java.util.HashMap<>(),
                new java.util.HashMap<>());
    }

    /**
     * O mesmo, e a frente de cada porta das peças, fora delas, fica fixa no piso da porta e entra em {@code doorFronts}
     * (Regra 303); sem moldes ({@code templates} nulo), nenhuma porta é lida.
     */
    static String settle(StructureStart start, int[][] natural, VillageSurface.Kind[][] kinds, VillageBlend.Area core,
                         VillageBlend.Area reach, int minX, int minZ,
                         net.minecraft.structure.StructureTemplateManager templates,
                         net.minecraft.registry.DynamicRegistryManager registries, java.util.Map<Long, Integer> doorFronts,
                         java.util.Map<Long, Integer> aprons) {
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
        List<java.util.Map<Long, Integer>> entries = new ArrayList<>();

        for (int i = 0; templates != null && i < rigid.size(); i++) {
            entries.add(DoorFronts.of(rigid.get(i).piece(), templates, registries));
        }

        // Regra 305: a casa (peça com porta) ganha um patamar de APRON colunas no piso; na disputa, o piso mais alto.
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).isEmpty()) {
                apron(rigid.get(i).footprint(), floorOf(rigid.get(i).piece()), natural, kinds, marked, minX, minZ, aprons);
            }
        }

        // A entrada de cada porta fica no piso, mesmo dentro da caixa do molde, e vale sobre o patamar (Regra 305).
        for (java.util.Map<Long, Integer> fronts : entries) {
            for (var front : fronts.entrySet()) {
                int x = com.villagecolony.core.construction.model.ColonyRoads.xOf(front.getKey()) - minX;
                int z = com.villagecolony.core.construction.model.ColonyRoads.zOf(front.getKey()) - minZ;

                if (x >= 0 && z >= 0 && x < natural.length && z < natural[0].length) {
                    natural[x][z] = front.getValue();
                    kinds[x][z] = VillageSurface.Kind.FIXED;
                    doorFronts.put(front.getKey(), front.getValue());
                    aprons.remove(front.getKey());
                    doors++;
                }
            }
        }

        return "\"moved\":" + moved + ",\"most\":" + most + ",\"doors\":" + doors + ",\"fixed\":[" + fixed + "]";
    }

    /** Quantas colunas de patamar no piso a casa ganha em volta da caixa (Regra 305). */
    static final int APRON = 2;

    /** O patamar em volta da pegada, no piso: fixo na conta e moldado até o piso; coluna de peça não entra. */
    private static void apron(VillageBlend.Area footprint, int floor, int[][] natural, VillageSurface.Kind[][] kinds,
                              boolean[][] marked, int minX, int minZ, java.util.Map<Long, Integer> aprons) {
        for (int x = footprint.minX() - APRON; x <= footprint.maxX() + APRON; x++) {
            for (int z = footprint.minZ() - APRON; z <= footprint.maxZ() + APRON; z++) {
                if (x < 0 || z < 0 || x >= natural.length || z >= natural[0].length || marked[x][z]) {
                    continue;
                }

                long column = com.villagecolony.core.construction.model.ColonyRoads.column(x + minX, z + minZ);
                Integer level = aprons.get(column);

                if (level == null || floor > level) {
                    aprons.put(column, floor);
                    natural[x][z] = floor;
                    kinds[x][z] = VillageSurface.Kind.FIXED;
                }
            }
        }
    }

    /** O piso da peça: o bloco de baixo do nível de chão dela. */
    private static int floorOf(PoolStructurePiece piece) {
        return piece.getBoundingBox().getMinY() + piece.getGroundLevelDelta() - 1;
    }

    private static int cells(VillageBlend.Area area) {
        return (area.maxX() - area.minX() + 1) * (area.maxZ() - area.minZ() + 1);
    }
}
