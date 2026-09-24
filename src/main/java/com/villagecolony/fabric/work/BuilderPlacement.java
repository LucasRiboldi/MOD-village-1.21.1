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

        BlockState state = BlockShaping.bedFacing(
                world, target, BlockShaping.facing(project, block, material.get().getDefaultState()));

        if (!world.getBlockState(target).isReplaceable()) {
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
            // é o vaso e a planta, e é deles que ele sai. Montar de graça
            // seria a colônia CRIANDO recurso, que a primeira regra de
            // arquitetura do Construction-System proíbe.
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
            Optional<String> chain = TestBarrier.chainFor(block.block());

            // <b>A barreira espera antes de riscar</b> — 2026-09-09. A
            // carência está em TestBarrier, e o que ela corrige é um
            // grito falso: 24 stripped_oak_log riscados na sessão de
            // 09-06 com cinquenta toras no baú, porque o construtor
            // riscava no mesmo tique em que via a falta e o fabricante
            // nunca teve o ciclo de descascar.
            //
            // Só o construtor começa a contar, e é por isso que a
            // chamada mora aqui: quem tentou tirar do baú foi ele.
            if (chain.isPresent()
                    && TestBarrier.graceExpired(
                            world.getTime(), project.id(), block.block())) {

                // <b>Barreira de teste</b> — a Regra 28, provisória por
                // declaração do autor: o bloco é riscado, e a casa fica
                // sem ele.
                //
                // Ela grita desde 2026-08-21, e o porquê está em
                // TestBarrier: a cadeia de cada uma das sete peças
                // fechou, e peça riscada deixou de ser o esperado para
                // virar notícia.
                TestBarrier.skip(project.id(), block.block(), chain.get());

                project.markPlaced(block);

                return true;
            }

            // Fora dessas quatro, <b>o construtor aguarda o bloco
            // específico de que precisa</b> — a Regra 27, aberta para
            // pedra em 2026-08-26 e inteira no resto.
            //
            // E a peça da barreira dentro da carência passa por aqui
            // também, de propósito: enquanto a barreira ainda espera,
            // ela é peça como qualquer outra e a Regra 27 vale inteira
            // para ela.
            //
            // O que impede a colônia de morrer esperando é o
            // PatienceClock: a obra sai da frente depois de vinte ciclos,
            // então a espera é do construtor e não da vila.
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

        // A única passagem em que uma peça de verdade encosta no mundo,
        // e é por isso que a conta da barreira sai daqui: as outras
        // quatro saídas de placeOne riscam o bloco, e riscado não é
        // assentado. Sem este número o relatório da sessão absolvia a
        // Regra 28 sem ter tido o que medir — o E31.
        TestBarrier.laidOne();

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
        for (ConstructionProject.DeferredPiece piece : project.deferredPieces()) {
            BlockPos target = MinecraftTypeAdapter.toBlockPos(piece.position());

            if (project.retryIfSupportChanged(piece, supportFingerprint(world, target))) {
                VillageColonyMod.LOGGER.info(
                        "Project {} retries {} at {} after its support changed",
                        project.id(),
                        piece.block(),
                        target.toShortString());
            }
        }
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
