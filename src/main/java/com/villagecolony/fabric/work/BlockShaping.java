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
 * A forma do bloco no mundo: virado para onde a planta manda, a cama inteira,
 * o vaso montado e o que nasce do chão em vez de vir do baú — separado de
 * {@link BuilderWork} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class BlockShaping {

    private BlockShaping() {
    }

    /**
     * A peça de parede se apoia na parede que existe — 2026-09-25, visto em
     * jogo.
     *
     * <p><b>O defeito.</b> A planta não guarda a direção do bloco, e
     * {@link #facing} só vira as peças da borda da caixa. No miolo da casa a
     * escada de mão e a tocha de parede ficavam no estado padrão, viradas
     * para o norte e procurando apoio ao sul. No templo de planície de 25-09
     * havia pedregulho a oeste, ao norte ou a leste de cada uma, e ar ao sul:
     * o jogo recusava as nove, elas eram adiadas, e a obra nunca fechava.
     *
     * <p><b>A regra.</b> Se a direção deduzida já se sustenta, ela fica — a
     * borda da caixa e qualquer peça que já dava certo não mudam. Senão, a
     * primeira das quatro direções, numa ordem fixa, em que o jogo aceita a
     * peça. Sem nenhuma, devolve a de antes, e a peça é adiada como era.
     *
     * <p>A direção de verdade está no arquivo da estrutura e o leitor a
     * descarta; guardá-la na planta é o conserto da aparência, e fica
     * registrado no TODO. Este aqui é o que garante que a obra fecha.
     */
    static BlockState leanOnAWall(ServerWorld world, BlockPos target, BlockState state) {
        if (!state.contains(Properties.HORIZONTAL_FACING) || state.canPlaceAt(world, target)) {
            return state;
        }

        for (Direction facing : WALL_ORDER) {
            BlockState turned = state.with(Properties.HORIZONTAL_FACING, facing);

            if (turned.canPlaceAt(world, target)) {
                return turned;
            }
        }

        return state;
    }

    /** Ordem fixa: a mesma parede dá a mesma direção em toda sessão. */
    private static final Direction[] WALL_ORDER = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    /**
     * Vira o bloco de parede para fora da casa — a Regra 17.
     *
     * <p>Até 2026-08-19 a porta saía no estado padrão da planta, que
     * olha sempre para o mesmo lado: a casa podia ter a porta na parede
     * da rua e a folha abrindo para o lado errado. Agora a orientação
     * sai da <b>geometria</b>, e não de um campo gravado.
     *
     * <p>A conta: o bloco está numa das quatro paredes; a parede diz
     * para onde é fora; e a face do bloco olha para <b>dentro</b>, que é
     * como o jogo grava a porta que um jogador põe da rua — ele está do
     * lado de fora, olhando para a casa, e a porta guarda a direção do
     * olhar dele.
     *
     * <p>Só blocos de parede. Bloco do miolo — a cama, o baú da Regra 21
     * — não tem "fora" e fica com o estado padrão até ter regra própria.
     *
     * <p>Deduzir em vez de gravar tem uma vantagem que vale dizer: a
     * obra que volta do save não precisa que o save saiba disso. A
     * planta remontada já traz a porta na parede certa, e a face sai
     * dela.
     */
    static BlockState facing(
            ConstructionProject project, BlueprintBlock block, BlockState state) {

        if (!state.contains(Properties.HORIZONTAL_FACING)) {
            return state;
        }

        if (state.contains(Properties.BED_PART)) {
            // O javadoc acima sempre disse que a cama fica de fora, e ela
            // não ficava: basta a planta pôr a cama encostada na parede
            // para esta regra virá-la para o meio da casa. Quem decide a
            // cama é o bedFacing, que pergunta onde a cabeceira cabe.
            return state;
        }

        ColonyPos size = project.blueprint().size();
        ColonyPos at = block.offset();

        Direction outward = null;

        if (at.x() == 0) {
            outward = Direction.WEST;
        } else if (at.x() == size.x() - 1) {
            outward = Direction.EAST;
        } else if (at.z() == 0) {
            outward = Direction.NORTH;
        } else if (at.z() == size.z() - 1) {
            outward = Direction.SOUTH;
        }

        if (outward == null) {
            return state;
        }

        return state.with(Properties.HORIZONTAL_FACING, outward.getOpposite());
    }

    /**
     * Para que lado a cama cabe — 2026-08-29.
     *
     * <p><b>Visto em jogo</b>, e o log diz na letra o que aconteceu:
     *
     * <pre>
     * Could not finish the two-part block at 769, 64, 935
     *     — Block{minecraft:cobblestone} is in the way
     * </pre>
     *
     * <p>A planta guarda o nome do bloco e não o estado (ADR-005), então
     * a cama saía no <b>padrão</b>, que olha para o norte. Na casa de
     * planície o norte da cama é a parede: a cabeceira não coube, e
     * sobrou meia cama — <i>"aparece somente a metade da cama e na
     * direção errada"</i>.
     *
     * <p>A saída é perguntar ao mundo em vez de ao arquivo, e é a Regra
     * 32 que a torna possível: com a mobília entrando depois da casa
     * pronta, a parede já está lá para ser vista.
     *
     * <p><b>Encostada na parede quando dá</b>, que é onde uma cama fica
     * numa casa: entre os lados livres, ganha aquele cujo bloco seguinte
     * é sólido. Sem nenhum assim, qualquer lado livre serve; sem nenhum
     * livre, fica o que estava — e aí o {@code placeSecondHalf} diz no
     * log que a cabeceira não coube, como já dizia.
     *
     * <p><b>A orientação fiel ao arquivo continua sendo a ADR-008</b>,
     * decidida e por escrever, e vale para o tronco e o degrau também.
     * Isto aqui é mais simples e mais urgente: cama que cabe.
     */
    static BlockState bedFacing(ServerWorld world, BlockPos foot, BlockState state) {
        if (!state.contains(Properties.BED_PART) || !state.contains(Properties.HORIZONTAL_FACING)) {
            return state;
        }

        Direction anywhere = null;

        for (Direction side : Direction.Type.HORIZONTAL) {
            if (!world.getBlockState(foot.offset(side)).isReplaceable()) {
                continue;
            }

            if (!world.getBlockState(foot.offset(side, 2)).isReplaceable()) {
                return state.with(Properties.HORIZONTAL_FACING, side);
            }

            if (anywhere == null) {
                anywhere = side;
            }
        }

        return anywhere == null ? state : state.with(Properties.HORIZONTAL_FACING, anywhere);
    }

    /**
     * Completa um bloco que ocupa dois lugares.
     *
     * <p>É o E8 do §17, e a outra ponta de
     * {@code StructureBlueprintReader.isSecondHalf}: o projeto guarda uma
     * porta só, e é aqui que ela vira duas metades <b>ligadas</b> em vez
     * de dois blocos independentes no estado padrão.
     *
     * <p>Escrever a segunda metade em vez de deixar o jogo fazê-lo é
     * deliberado. {@code Block.onPlaced} faria isso, e faria também tudo
     * o mais que a colocação por jogador dispara — som, evento, lógica de
     * item. O construtor não é um jogador com uma porta na mão; ele está
     * montando uma casa a partir de um arquivo, e o que ele precisa é da
     * propriedade que liga as duas metades.
     *
     * <p>Só escreve onde há lugar. A metade de cima cai sobre o que o
     * projeto já pôs no andar de cima em nenhum caso — a leitura descarta
     * aquela posição —, mas o mundo é do jogador e pode ter qualquer
     * coisa ali. A Regra 3 vale aqui como vale no resto da obra: nada
     * substitui o que não é substituível.
     *
     * <p>Bloco de uma parte só passa direto: {@code contains} responde
     * não, e nada acontece.
     */
    public static void placeSecondHalf(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.contains(Properties.DOUBLE_BLOCK_HALF)) {
            put(world, pos.up(), state.with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));

            return;
        }

        if (state.contains(Properties.BED_PART) && state.contains(Properties.HORIZONTAL_FACING)) {
            // A cabeceira vai para onde o estado padrão aponta, e não
            // para onde o arquivo dizia: a orientação é a metade do E8
            // que continua aberta (TASK-046). Uma cama virada para o
            // norte numa casa que a queria virada para o leste continua
            // sendo uma cama — dois pés lado a lado não eram.
            put(
                    world,
                    pos.offset(state.get(Properties.HORIZONTAL_FACING)),
                    state.with(Properties.BED_PART, BedPart.HEAD));
        }
    }

    /** Escreve, se o lugar aceitar. */
    static void put(ServerWorld world, BlockPos pos, BlockState state) {
        if (!world.getBlockState(pos).isReplaceable()) {
            VillageColonyMod.LOGGER.info(
                    "Could not finish the two-part block at {} — {} is in the way",
                    pos.toShortString(),
                    world.getBlockState(pos).getBlock());

            return;
        }

        world.setBlockState(pos, state, Block.NOTIFY_ALL);
    }

    /**
     * Se este bloco a colônia molda do chão em vez de tirar do baú.
     *
     * <p>São blocos que se formam no local ou não têm item próprio para
     * o estoque. Terra, grama, terra grossa e barro têm itens e continuam
     * sendo materiais físicos de construção.
     *
     * <p>O {@code dirt_path} permanece nesta lista porque não tem item
     * próprio; terra comum não entra aqui, pois pode ser fornecida pelo
     * baú como qualquer outro material de construção.
     */
    public static boolean isShapedFromTheGround(BlockState state) {
        return state.isOf(Blocks.FARMLAND)
                || state.isOf(Blocks.WATER)
                || state.isOf(Blocks.DIRT_PATH)
                || state.getBlock() instanceof CropBlock
                || hasNoItemOfItsOwn(state);
    }

    /**
     * Bloco que <b>não tem item</b> — e por isso ninguém pode trazê-lo.
     *
     * <p><b>O que isto conserta, medido na sessão de 14:47.</b> A obra
     * parou em {@code waiting for minecraft:potted_cactus} com 148 blocos
     * por pôr, e ia esperar <b>para sempre</b>: no Minecraft o vaso com
     * cacto só existe como <i>bloco</i> — o jogador põe o vaso e planta o
     * cacto nele —, e {@code Items.POTTED_CACTUS} não existe. A colônia
     * esperava um item que <b>não pode existir</b>.
     *
     * <p>E não era só ele. As casas do catálogo pedem oito blocos assim:
     * os cinco vasos com planta, {@code water}, {@code lava} e
     * {@code water_cauldron}.
     *
     * <p><b>Pergunta ao jogo em vez de crescer a lista acima</b> —
     * ADR-009. A lista era quatro nomes escritos à mão, e cada bloco novo
     * sem item pedia mais um; {@code asItem()} devolve <b>ar</b>
     * exatamente quando não há item, e essa é a pergunta que importa.
     * Quem sabe é o jogo.
     *
     * <p>O bloco entra montado, como já entram o canteiro e o caminho de
     * terra: é a mesma decisão de 2026-08-27, aplicada à sua própria
     * definição.
     */
    static boolean hasNoItemOfItsOwn(BlockState state) {
        return state.getBlock().asItem() == Items.AIR;
    }

    /**
     * Monta o vaso com planta do vaso e da planta que a colônia tem.
     *
     * <p>Os dois saem do baú <b>antes</b> de o bloco entrar no mundo, que
     * é a mesma ordem de todo material: sem ingrediente não há bloco.
     *
     * <p><b>O vaso primeiro, e a planta só se o vaso saiu.</b> Tirar a
     * planta e falhar no vaso gastaria a planta sem pôr nada — o defeito
     * que o lenhador teve em 09-04 com outro nome.
     *
     * @return {@code false} quando falta ingrediente e a obra espera
     */
    static boolean assemblePotted(
            ServerWorld world,
            ConstructionProject project,
            Job job,
            UUID workerId,
            BlueprintBlock block,
            BlockPos target,
            BlockState state) {

        Optional<Item> plant = PottedPlant.plantOf(state.getBlock());

        if (!BuilderMaterials.hasOrStocksConstructionMaterial(
                world, project, PottedPlant.pot())) {

            BuilderMaterials.waitForResources(project, job, workerId, block);

            return false;
        }

        if (plant.isPresent()
                && !BuilderMaterials.hasOrStocksConstructionMaterial(world, project, plant.get())) {

            BuilderMaterials.waitForResources(project, job, workerId, block);

            return false;
        }

        // Os dois estão lá: agora sim se gasta, e na ordem em que se
        // conferiu.
        ColonySupply.take(world, project.colonyId(), project.origin(), PottedPlant.pot());

        plant.ifPresent(item ->
                ColonySupply.take(world, project.colonyId(), project.origin(), item));

        world.setBlockState(target, state, Block.NOTIFY_ALL);

        project.markPlaced(block);

        job.placed++;

        return true;
    }
}
