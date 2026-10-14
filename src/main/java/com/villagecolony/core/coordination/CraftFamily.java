package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.worker.model.ProfessionType;

import java.util.List;

/**
 * Quem fabrica cada peça, pelo material — regra do autor, 2026-10-11: madeira é do
 * carpinteiro; pedra, barro, terracota e fornalha, do pedreiro; metal, vidro,
 * lampião, gelo e neve, do fundidor; lã colorida, tapete, cama, estandarte e
 * corante, do pastor; papel, livro, farinha de osso e feno, do fazendeiro.
 *
 * <p>Antes o carpinteiro era o ofício-padrão de tudo que a bancada faz e ficava
 * com barra de ferro, cama, corante e fornalha ({@code StructureCoverageGameTest},
 * 103 de 187 itens). A pergunta é por nome, o idioma da base (a cama já era
 * {@code endsWith("_bed")}): as peças de obra quase nunca são recurso declarado.
 */
public final class CraftFamily {

    /** As tarefas de fabricação, na ordem em que as oficinas são despachadas. */
    public static final List<TaskType> TYPES = List.of(
            TaskType.CRAFT_WOOD_MATERIAL, TaskType.CRAFT_STONE_MATERIAL, TaskType.CRAFT_METAL_MATERIAL,
            TaskType.CRAFT_TEXTILE_MATERIAL, TaskType.CRAFT_FARM_MATERIAL);

    /** As marcas da pedra; {@code sandstone} entra por {@code stone}. */
    static final List<String> MASONRY = List.of(
            "stone", "cobble", "brick", "granite", "diorite", "andesite", "deepslate", "tuff", "quartz",
            "terracotta", "basalt", "calcite", "furnace", "smoker", "clay", "flower_pot");

    static final List<String> METAL = List.of(
            "iron", "gold", "copper", "lantern", "chain", "cauldron", "anvil", "hopper", "bucket", "glass",
            "coal_block", "ice", "snow");

    static final List<String> TEXTILE_ENDINGS = List.of("_wool", "_carpet", "_bed", "_banner", "_dye");

    static final List<String> FARM = List.of(
            "paper", "book", "bone_meal", "bone_block", "hay_block", "bread", "cake", "pumpkin_pie", "jack_o_lantern");

    private CraftFamily() {
    }

    /** A tarefa de fabricação desta peça, pelo caminho do nome ({@code oak_stairs}). */
    public static TaskType of(String path) {
        if (path.contains("redstone")) {
            // A tocha de redstone tem "stone" no nome e não é alvenaria.
            return TaskType.CRAFT_WOOD_MATERIAL;
        }

        if (path.contains("bookshelf") || path.equals("lectern")) {
            // Madeira com livro: o grosso é tábua.
            return TaskType.CRAFT_WOOD_MATERIAL;
        }

        if (path.equals("leather") || TEXTILE_ENDINGS.stream().anyMatch(path::endsWith)) {
            return TaskType.CRAFT_TEXTILE_MATERIAL;
        }

        if (FARM.stream().anyMatch(path::contains)) {
            return TaskType.CRAFT_FARM_MATERIAL;
        }

        if (METAL.stream().anyMatch(path::contains)) {
            return TaskType.CRAFT_METAL_MATERIAL;
        }

        if (MASONRY.stream().anyMatch(path::contains)) {
            return TaskType.CRAFT_STONE_MATERIAL;
        }

        return TaskType.CRAFT_WOOD_MATERIAL;
    }

    /** O ofício que fabrica pela tarefa {@code type}. */
    public static ProfessionType craftsmanOf(TaskType type) {
        return switch (type) {
            case CRAFT_STONE_MATERIAL -> ProfessionType.MASON;
            case CRAFT_METAL_MATERIAL -> ProfessionType.SMELTER;
            case CRAFT_TEXTILE_MATERIAL -> ProfessionType.SHEPHERD;
            case CRAFT_FARM_MATERIAL -> ProfessionType.FARMER;
            default -> ProfessionType.CARPENTER;
        };
    }

    /** Como a oficina aparece no log ({@code IdleLog}, pedido de peça). */
    public static String subjectOf(TaskType type) {
        return switch (type) {
            case CRAFT_STONE_MATERIAL -> "mason";
            case CRAFT_METAL_MATERIAL -> "smelter workshop";
            case CRAFT_TEXTILE_MATERIAL -> "shepherd workshop";
            case CRAFT_FARM_MATERIAL -> "farmer workshop";
            default -> "carpenter";
        };
    }

    /** Se a tarefa é de fabricação, de qualquer família. */
    public static boolean isCrafting(TaskType type) {
        return TYPES.contains(type);
    }
}
