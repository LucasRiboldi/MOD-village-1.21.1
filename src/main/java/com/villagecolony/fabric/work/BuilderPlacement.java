package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.BuilderWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.construction.model.Building;
import com.villagecolony.core.construction.model.BlueprintBlock;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.ConstructionOutcome;
import com.villagecolony.core.construction.model.SkipReason;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.ChestWithdrawer;
import com.villagecolony.fabric.integration.ColonySupply;
import com.villagecolony.fabric.integration.BiomeConstructionSupply;
import net.minecraft.block.Block;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Assentar um bloco da planta, e voltar às peças adiadas quando o apoio delas
 * mudou — separado de {@link BuilderWork} em 2026-09-24, quando ele passou de
 * 500 linhas. Os comentários vieram junto sem mudança.
 */
public final class BuilderPlacement {

    private BuilderPlacement() {
    }

    /**
     * Põe um bloco, se houver material e lugar.
     *
     * @return false quando a obra não pode continuar agora
     */
    static boolean placeOne(
            ServerWorld world,
            ConstructionProject project,
            Job job,
            UUID workerId,
            BlueprintBlock block,
            BlockPos target) {

        Optional<Block> material = MinecraftTypeAdapter.toBlock(block.block());

        if (material.isEmpty()) {
            // O jogo não conhece este bloco — datapack que saiu, versão
            // que mudou. Riscar é melhor que travar a obra para sempre.
            VillageColonyMod.LOGGER.warn(
                    "Project {} asks for {}, which this game does not have — skipped",
                    project.id(),
                    block.block());

            project.markPlaced(block);

            return true;
        }

        if (BlockShaping.isNeverPlaced(material.get().getDefaultState())) {
            // A lava da planta não surge sozinha — 2026-09-30. A posição
            // fica vazia e a obra segue.
            VillageColonyMod.LOGGER.info(
                    "Project {} leaves {} empty — lava is never placed by the colony",
                    project.id(),
                    target.toShortString());

            project.markPlaced(block);

            return true;
        }

        // O bloco da planta já está no lugar — 2026-10-03. Com a terra da base
        // sendo construída, terra sobre terra viraria cavar e repor; aqui ela
        // conta como assentada, sem tirar material do baú.
        if (world.getBlockState(target).isOf(material.get())) {
            project.markPlaced(block);

            return true;
        }

        BlockState state = shaped(world, project, block, material.get(), target);

        if (!world.getBlockState(target).isReplaceable()
                && !BuriedPieces.mayReplaceGround(world, project.blueprint(), block, target)
                && !BlockShaping.tillsTheGround(world, state, target)) {
            // Já tem coisa ali, e não é grama alta: pode ser peça de
            // vila, pode ser construção do jogador. A Regra 3 manda não
            // mexer, e a obra segue sem este bloco.
            VillageColonyMod.LOGGER.info(
                    "Project {} skips {} — {} is in the way",
                    project.id(),
                    target,
                    world.getBlockState(target).getBlock());

            project.markPlaced(block);

            return true;
        }

        if (!state.canPlaceAt(world, target) && project.failsForTheLastTime(block)) {
            // Quinta falha ao pôr: a peça é pulada e o erro fica no log — ADR-036 item 6.
            VillageColonyMod.LOGGER.warn(
                    "VC_SUPPLY_ERROR version=1 piece={} profession=BUILDER attempts={} reason=placement failed at {}",
                    block.block(), ConstructionProject.PLACEMENT_FAILURES_BEFORE_SKIP, target.toShortString());
            project.markPlaced(block);

            return true;
        }

        if (!state.canPlaceAt(world, target)) {
            // Tocha sem parede, porta sem chão. Não é bloco colocado e
            // também não é uma obra que deva acordar outro construtor a
            // cada ciclo: fica pendente até a leitura do apoio mudar.
            ConstructionOutcome.Skipped outcome = ConstructionOutcome.skipped(
                    project.worldPositionOf(block), SkipReason.UNSUPPORTED);
            project.defer(block, outcome, supportFingerprint(world, target));

            VillageColonyMod.LOGGER.info(
                    "Project {} defers {} at {} — nothing holds it",
                    project.id(),
                    block.block(),
                    target.toShortString());

            BuilderWork.finish(job, workerId, "the next piece has no physical support at " + target.toShortString());

            return false;
        }

        if (PottedPlant.isPotted(state)) {
            // <b>O vaso com planta é montado, e pago</b> — 2026-09-19. O
            // bloco não tem item e ninguém o traria; o que a colônia tem
            // é o vaso e a planta, e é deles que ele sai: a cadeia existe
            // dentro da colônia, então ela é usada (ADR-028).
            //
            // Espera pelos dois como esperaria por qualquer material: a
            // Regra 27 continua valendo, só que sobre os ingredientes em
            // vez de sobre um item que não existe.
            if (!BlockShaping.assemblePotted(world, project, job, workerId, block, target, state)) {
                return false;
            }

            return true;
        }

        if (BlockShaping.isShapedFromTheGround(state)) {
            // A colônia molda no local apenas blocos sem item de inventário
            // utilizável (canteiro, água, caminho e cultivos). Terra comum
            // tem item e deve sair fisicamente do estoque.
            //
            // Quem abre um canteiro move o chão que já está ali. A regra
            // vale para qualquer obra porque depende do bloco, não da planta.
            world.setBlockState(target, state, Block.NOTIFY_ALL);

            project.markPlaced(block);

            return true;
        }

        Optional<Item> taken = BuilderMaterials.takeMaterial(world, project, material.get());

        if (taken.isEmpty()) {
            // Falta de material: a peça espera de lado e a obra segue pelas
            // outras (autor, 2026-10-08 — o celeiro parou 817 blocos por um
            // funil). Volta quando o material chega (reconsiderDeferredPieces).
            if (project.hasAnotherPieceThan(block)) {
                project.deferForMaterial(block);

                VillageColonyMod.LOGGER.info(
                        "Project {} sets {} at {} aside — not in the colony chests yet; it goes on"
                                + " with the next piece",
                        project.id(), block.block(), target.toShortString());

                return true;
            }

            // Era a última: o construtor aguarda, e o suprimento a faz aparecer
            // na quarta tentativa (ADR-036 item 6).
            BuilderMaterials.waitForResources(project, job, workerId, block);

            return false;
        }

        // O que se assenta é o que saiu do baú, e não o que a planta
        // pediu: a colônia não inventa matéria. O substituto veste o
        // estado da planta no que os dois tiverem em comum — senão uma
        // viga deitada trocada de espécie sairia em pé.
        BlockState placed = MaterialChoice.isExact(material.get(), taken.get())
                ? state
                : MaterialChoice.dressedLike(state, taken.get());

        world.setBlockState(target, placed, Block.NOTIFY_ALL);

        BlockShaping.placeSecondHalf(world, target, placed);

        project.markPlaced(block);

        job.placed++;

        // O gesto de assentar — e o sinal de "trabalhando" do WorkTime (Regra 50).
        if (world.getEntity(workerId) instanceof VillagerEntity builder) {
            builder.swingHand(Hand.MAIN_HAND);
        }

        // A única passagem em que uma peça de verdade encosta no mundo,
        // e é por isso que a conta da barreira sai daqui: as outras
        // quatro saídas de placeOne riscam o bloco, e riscado não é
        // assentado. Sem este número o relatório da sessão absolvia a
        // Regra 28 sem ter tido o que medir — o E31.
        return true;
    }

    /**
     * Reabre peças parciais apenas quando a leitura local que as impediu
     * de ser colocadas mudou.
     *
     * <p>A assinatura cobre o alvo e seus seis vizinhos. É a pequena
     * vizinhança que {@link BlockState#canPlaceAt} consulta para apoios
     * usuais, como chão, parede e teto, sem gravar estado do Minecraft no
     * core.
     */
    public static void reconsiderDeferredPieces(ServerWorld world, ConstructionProject project) {
        // Sem mais nada a pôr, as peças que esperam material voltam todas: a
        // obra passa a esperar por elas como antes.
        boolean nothingElse = project.nextBlock().isEmpty();

        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            BlockPos target = MinecraftTypeAdapter.toBlockPos(piece.position());

            if (piece.reason() == SkipReason.WAITING_MATERIAL) {
                if ((nothingElse || BuilderMaterials.isInTheChests(world, project, piece.block()))
                        && project.retry(piece)) {
                    VillageColonyMod.LOGGER.info("Project {} retries {} at {} — {}",
                            project.id(), piece.block(), target.toShortString(),
                            nothingElse ? "nothing else is left to place" : "the material arrived");
                }

                continue;
            }

            if (project.retryIfSupportChanged(piece, supportFingerprint(world, target))) {
                VillageColonyMod.LOGGER.info(
                        "Project {} retries {} at {} after its support changed",
                        project.id(),
                        piece.block(),
                        target.toShortString());

                continue;
            }

            // <b>E quando ela já cabe</b> — 2026-09-25, visto em jogo. As nove
            // peças do templo foram adiadas por uma versão que não apoiava a
            // peça de parede na parede que existe; a vizinhança delas nunca ia
            // mudar, e a obra ficou aberta por três sessões. A pergunta é a
            // mesma que o construtor faz ao assentar — ver shaped.
            if (fitsNow(world, project, piece, target) && project.retry(piece)) {
                VillageColonyMod.LOGGER.info(
                        "Project {} retries {} at {} — it fits against the wall now",
                        project.id(),
                        piece.block(),
                        target.toShortString());
            }
        }
    }

    /**
     * O estado que o construtor assenta neste lugar: eixo, metade e formato
     * da planta (ADR-036 19), a direção dela, a cama pela cabeceira, e a peça
     * de parede apoiada na parede que existe.
     */
    static BlockState shaped(
            ServerWorld world,
            ConstructionProject project,
            BlueprintBlock block,
            Block material,
            BlockPos target) {

        BlockState planned = BlockShaping.withPlanStates(block, material.getDefaultState());
        BlockState state = BlockShaping.bedFacing(world, target, BlockShaping.facing(project, block, planned));

        return BlockShaping.leanOnAWall(world, target, state);
    }

    /** Se a peça adiada já se sustenta no lugar dela. */
    private static boolean fitsNow(
            ServerWorld world,
            ConstructionProject project,
            ConstructionProject.DeferredPiece piece,
            BlockPos target) {

        Optional<BlueprintBlock> block = project.remaining().stream()
                .filter(candidate -> candidate.block().equals(piece.block())
                        && project.worldPositionOf(candidate).equals(piece.position()))
                .findFirst();

        Optional<Block> material = MinecraftTypeAdapter.toBlock(piece.block());

        return block.isPresent()
                && material.isPresent()
                && world.getBlockState(target).isReplaceable()
                && shaped(world, project, block.get(), material.get(), target).canPlaceAt(world, target);
    }

    /** Uma representação estável do alvo e de cada bloco que pode apoiá-lo. */
    static String supportFingerprint(ServerWorld world, BlockPos target) {
        StringBuilder fingerprint = new StringBuilder(
                world.getBlockState(target).toString());

        for (Direction direction : Direction.values()) {
            fingerprint.append('|')
                    .append(direction.getName())
                    .append('=')
                    .append(world.getBlockState(target.offset(direction)));
        }

        return fingerprint.toString();
    }
}
