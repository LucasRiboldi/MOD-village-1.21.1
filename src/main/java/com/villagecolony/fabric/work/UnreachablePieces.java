package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.SupplyException;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.integration.VillageStyle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * A peça que o construtor não alcança surge no lugar — decisão do autor, 2026-10-20.
 *
 * <p>A peça posta de lado por alcance ({@code SkipReason.UNREACHABLE}) voltava a ser tentada para
 * sempre, e quando era a última a obra nunca fechava: na bancada da taiga, a casa subiu 305 blocos e
 * ficou 44 ciclos em "1 blocks left", segurando a vaga única de obra da vila. Agora, sem mais nada a
 * pôr e depois de {@link #TRIES} vezes de lado, ela surge no lugar — como a horta e o chão moldado já
 * surgem — e o caso vai para {@code <pasta do jogo>/villagecolony/unreachable-pieces.jsonl}, para o
 * acesso ser corrigido depois (o mesmo espírito do registro de faltas, {@code SupplyGapLedger}).
 */
final class UnreachablePieces {

    /** Quantas vezes de lado por alcance antes de surgir. */
    static final int TRIES = 3;

    private static boolean failed;

    private UnreachablePieces() {
    }

    /** Se esta peça de lado por alcance já esperou o bastante. */
    static boolean isDue(ConstructionProject project, ConstructionProject.DeferredPiece piece) {
        return project.deferralsAt(piece.position()) >= TRIES;
    }

    /**
     * Assenta a peça no lugar, sem o construtor, e a risca da obra. Bloco que já está ali, ou que não é
     * substituível (peça de vila, coisa do jogador), não é sobrescrito: a peça é riscada como a obra já
     * faz com o que está no caminho.
     */
    static void surge(ServerWorld world, ConstructionProject project, ConstructionProject.DeferredPiece piece) {
        surge(world, project, piece, SupplyException.UNREACHABLE_PIECE);
    }

    /** O mesmo, com a exceção de suprimento que autoriza a peça a surgir (Regras 86 e 113). */
    static void surge(ServerWorld world, ConstructionProject project, ConstructionProject.DeferredPiece piece,
            SupplyException why) {
        Optional<BlueprintBlock> block = project.remaining().stream()
                .filter(candidate -> project.worldPositionOf(candidate).equals(piece.position()))
                .findFirst();
        Optional<Block> material = MinecraftTypeAdapter.toBlock(piece.block());

        if (block.isEmpty() || material.isEmpty()) {
            return;
        }

        BlockPos target = MinecraftTypeAdapter.toBlockPos(piece.position());
        BlockState now = world.getBlockState(target);
        boolean placed = false;

        if (!now.isOf(material.get()) && now.isReplaceable()) {
            BlockState state = BuilderPlacement.shaped(world, project, block.get(), material.get(), target);
            world.setBlockState(target, state, Block.NOTIFY_ALL);
            BlockShaping.placeSecondHalf(world, target, state);
            placed = true;
            com.villagecolony.fabric.integration.Supplies.record(why, project.colonyId(),
                    piece.block().path(), 1);
        }

        project.markPlaced(block.get());
        VillageColonyMod.LOGGER.warn("VC_UNREACHABLE version=1 project={} piece={} at={} tries={} placed={} why={} rule={}",
                project.id(), piece.block(), target.toShortString(), project.deferralsAt(piece.position()), placed,
                why, why.rule());
        record(world, project, piece, target, placed);
    }

    private static void record(ServerWorld world, ConstructionProject project, ConstructionProject.DeferredPiece piece,
            BlockPos target, boolean placed) {
        Path file = world.getServer().getRunDirectory().resolve("villagecolony").resolve("unreachable-pieces.jsonl");
        String style = VillageColonyMod.COLONIES.find(project.colonyId())
                .map(colony -> VillageStyle.biomeAt(world, colony.center()).getValue().toString())
                .orElse("?");
        String version = FabricLoader.getInstance().getModContainer("villagecolony")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
        String line = "{\"time\":\"" + OffsetDateTime.now(ZoneId.systemDefault()) + "\""
                + ",\"version\":\"" + version + "\""
                + ",\"colony\":\"" + project.colonyId() + "\""
                + ",\"style\":\"" + style + "\""
                + ",\"blueprint\":\"" + project.blueprint().id() + "\""
                + ",\"piece\":\"" + piece.block() + "\""
                + ",\"x\":" + target.getX() + ",\"y\":" + target.getY() + ",\"z\":" + target.getZ()
                + ",\"tries\":" + project.deferralsAt(piece.position())
                + ",\"placed\":" + placed + "}\n";

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            if (!failed) {
                failed = true;
                VillageColonyMod.LOGGER.warn("The unreachable piece ledger is off — cannot write {}", file, failure);
            }
        }
    }
}
