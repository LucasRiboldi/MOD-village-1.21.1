package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import com.villagecolony.VillageColonyMod;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Diz, dentro do jogo, de quem é cada baú.
 *
 * <p>Pedido do autor em 2026-08-12. Até aqui a ligação entre trabalhador
 * e baú só existia no registro e no log: quem estivesse dentro do jogo
 * via um baú igual a qualquer outro, e a única forma de saber de quem
 * era passava por ler o arquivo de log.
 *
 * <p>A marca é um quadro com a ferramenta da profissão pregado no baú.
 * Vanilla puro, legível de longe e sem textura nova. O ícone diz a
 * profissão, e desde a regra de uma vaga por profissão a profissão é a
 * identidade: uma vila tem um lenhador, e o baú com o machado é o dele.
 *
 * <p><b>O que este código não faz.</b> Não quebra bloco, não move item e
 * não toca no que o jogador construiu. Só pendura sua marca em uma face livre.
 * Nos baús dos trabalhadores, prioriza a passagem interna em direção à porta
 * (N14 do playtest de 21-11-2026).
 */
public final class ChestMarker {

    static {
        ServerMemory.register(ChestMarker.class, ChestMarker::clearAll);
    }

    /**
     * O que a marca escreve no nome da entidade, para se reconhecer
     * depois.
     *
     * <p>Não aparece para o jogador: serve para o mod distinguir o
     * próprio quadro do quadro que o jogador pendurou ali. Sem isto, ou
     * o mod acrescentaria um quadro por ciclo, ou passaria a mexer em
     * decoração alheia.
     */
    private static final String TAG = "villagecolony:storage";
    private static final int DOOR_RADIUS = 6;

    /**
     * Os baús sobre os quais o log já reclamou de falta de espaço.
     *
     * <p>Em memória e descartável: ao reiniciar, a primeira tentativa
     * reclama de novo, o que é o comportamento certo — quem abriu o log
     * daquela sessão precisa ver o motivo.
     */
    private static final Set<BlockPos> complainedAbout = new HashSet<>();

    private ChestMarker() {
    }

    /** Esquece as reclamações, junto com o resto do estado em memória. */
    public static void clearAll() {
        complainedAbout.clear();
    }

    /**
     * Marca os baús destes trabalhadores, e corrige os que mudaram de
     * dono.
     *
     * <p>Chamável a cada ciclo: a marca acompanha o ofício e a passagem livre da moradia.
     *
     * @return quantas marcas foram postas ou corrigidas agora
     */
    public static int mark(
            ServerWorld world, Collection<Worker> workers, StorageLookup storages) {

        int marked = 0;

        for (Worker worker : workers) {
            Optional<ProfessionType> profession = worker.profession();

            if (profession.isEmpty()) {
                continue;
            }

            Optional<ColonyPos> chest = storages.chestOf(worker.villagerId());

            if (chest.isEmpty()) {
                continue;
            }

            if (markHomeAt(world, MinecraftTypeAdapter.toBlockPos(chest.get()), profession.get())) {
                marked++;
            }
        }

        return marked;
    }

    /** De onde sai a posição do baú de um trabalhador. */
    @FunctionalInterface
    public interface StorageLookup {

        Optional<ColonyPos> chestOf(java.util.UUID workerId);
    }

    /**
     * Marca um baú avulso, que não é de trabalhador nenhum.
     *
     * <p>Existe para a boca da mina — Regra 30, 2026-08-22. O baú de lá
     * é da <b>mina</b> e não de um mineiro: o segundo a descer usa o
     * mesmo, e ele não aparece em {@code StorageRegistry}. Sem isto a
     * marca só alcançaria baú com dono.
     */
    public static boolean markAt(ServerWorld world, BlockPos chest, ProfessionType profession) {
        return markOne(world, chest, profession, false);
    }

    /** Marca o baú da moradia, usando a passagem interna para orientar o quadro (N14). */
    public static boolean markHomeAt(ServerWorld world, BlockPos chest, ProfessionType profession) {
        return markOne(world, chest, profession, true);
    }

    /**
     * De que profissão é a marca deste baú, se ele tem uma.
     *
     * <p>Lida do mundo, como tudo nesta classe: o quadro é a verdade, e
     * o jogador que o arrancar libera o baú. Vazio quer dizer baú sem
     * dono — não quer dizer baú de todo mundo, e quem decide isso é
     * {@link #allows}.
     */
    public static Optional<ProfessionType> professionAt(ServerWorld world, BlockPos chest) {
        if (chunkAt(world, chest) == null) {
            return Optional.empty();
        }

        return existingMarkerAt(world, chest).flatMap(ChestMarker::professionOf);
    }

    /**
     * Se este baú pode ficar com quem exerce esta profissão — decisão do
     * autor, 2026-08-27.
     *
     * <p><b>O que estava errado.</b> O baú era escolhido pelo mais perto
     * da cama, por ordem de chegada, e profissão não entrava na conta.
     * Duas camas a dois blocos uma da outra bastavam: quem fosse
     * processado primeiro levava o baú do outro, e a marca ia atrás
     * dizendo que agora era dele. O autor viu em jogo — <i>"tem material
     * de lenhador indo para o baú do fazendeiro"</i>.
     *
     * <p><b>A marca já existia; o que faltava era ela valer.</b> Baú
     * marcado é de quem a marca diz, e quem não é daquela profissão
     * procura outro.
     *
     * <p><b>Quem ainda não tem profissão só pega baú sem marca.</b> Ele
     * não tem com o que casar, e deixá-lo passar tornaria a regra letra
     * morta — bastava reivindicar antes de ser contratado.
     *
     * <p><b>O que esta regra não faz</b>, e fica dito: ela não desfaz a
     * primeira escolha. Baú sem marca vai para o vizinho mais próximo
     * como sempre foi, e é a marca <i>daí em diante</i> que o prende.
     * Para mudar de dono, o jogador arranca o quadro.
     *
     * @param wants a profissão de quem quer o baú, vazia para quem ainda
     *     não tem nenhuma
     */
    public static boolean allows(
            ServerWorld world, BlockPos chest, Optional<ProfessionType> wants) {

        Optional<ProfessionType> owner = professionAt(world, chest);

        return owner.isEmpty() || owner.equals(wants);
    }

    /** A profissão que este ícone representa, se for um dos nossos. */
    private static Optional<ProfessionType> professionOf(ItemFrameEntity frame) {
        ItemStack held = frame.getHeldItemStack();

        for (ProfessionType profession : ProfessionType.values()) {
            if (held.isOf(itemOf(profession))) {
                return Optional.of(profession);
            }
        }

        return Optional.empty();
    }

    /**
     * Põe ou corrige a marca de um baú.
     *
     * @return true se algo mudou no mundo
     */
    private static boolean markOne(
            ServerWorld world, BlockPos chest, ProfessionType profession, boolean home) {
        if (chunkAt(world, chest) == null) {
            // Chunk descarregado. Nunca forçar carregamento de dentro do
            // ciclo do servidor — ver §11.
            return false;
        }

        ItemStack badge = badgeOf(profession);

        Optional<ItemFrameEntity> existing = existingMarkerAt(world, chest);

        if (home) {
            Optional<ItemFrameEntity> preferred = towardDoor(world, chest, existing);
            if (preferred.isPresent() && !preferred.equals(existing) && spawn(world, preferred.get(), badge)) {
                existing.ifPresent(ItemFrameEntity::discard);
                return true;
            }
        }

        if (existing.isPresent()) {
            ItemFrameEntity frame = existing.get();

            if (frame.getHeldItemStack().isOf(badge.getItem())
                    && label(profession).equals(frame.getHeldItemStack().getName().getString())) {
                return false;
            }

            // Reaproveita o quadro quando o ofício ou o nome legado mudou.
            frame.setHeldItemStack(badge);

            return true;
        }

        return place(world, chest, badge);
    }

    /**
     * Procura a porta mais perto por piso livre no mesmo nível, a partir das
     * faces que comportam um quadro (N14). A busca fica no entorno do baú e
     * só aceita a porta quando chega pelo lado interno indicado pelo bloco.
     */
    private static Optional<ItemFrameEntity> towardDoor(
            ServerWorld world, BlockPos chest, Optional<ItemFrameEntity> existing) {
        Queue<BadgeRoute> pending = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();

        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos front = chest.offset(facing);
            if (isPassage(world, front)) {
                existing.filter(frame -> frame.getHorizontalFacing() == facing)
                        .or(() -> availableFrame(world, chest, facing))
                        .ifPresent(frame -> {
                            seen.add(front);
                            pending.add(new BadgeRoute(front, frame));
                        });
            }
        }

        while (!pending.isEmpty()) {
            BadgeRoute route = pending.remove();
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos next = route.position().offset(direction);
                if (next.getManhattanDistance(chest) > DOOR_RADIUS || !world.isChunkLoaded(next)) {
                    continue;
                }
                BlockState state = world.getBlockState(next);
                if (state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                        && state.get(DoorBlock.FACING) == direction.getOpposite()) {
                    return Optional.of(route.frame());
                }
                if (seen.add(next) && isPassage(world, next)) {
                    pending.add(new BadgeRoute(next, route.frame()));
                }
            }
        }
        return Optional.empty();
    }

    private record BadgeRoute(BlockPos position, ItemFrameEntity frame) {
    }

    private static boolean isPassage(ServerWorld world, BlockPos pos) {
        BlockPos floor = pos.down();
        return world.isChunkLoaded(pos) && world.getBlockState(pos).isAir()
                && world.getBlockState(pos.up()).isAir()
                && world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP);
    }

    /**
     * Pendura o quadro na primeira face livre.
     *
     * <p>Só as quatro faces laterais. Em cima do baú fica a tampa, e um
     * quadro ali é a primeira coisa que o jogador acerta ao tentar abrir.
     */
    private static boolean place(ServerWorld world, BlockPos chest, ItemStack badge) {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            Optional<ItemFrameEntity> frame = availableFrame(world, chest, facing);
            if (frame.isPresent() && spawn(world, frame.get(), badge)) {
                return true;
            }
        }

        // Baú cercado por todos os lados. Fica sem marca, e o mod não
        // abre espaço na construção do jogador para pendurar a sua.
        //
        // Dito uma vez por baú, e não a cada ciclo: é uma situação
        // permanente até o jogador mexer na casa, e repeti-la a cada
        // trinta segundos encheria o log sem acrescentar nada. Mas
        // precisa ser dita ao menos uma vez — sem ela, "registrei cinco
        // baús e marquei dois" não tem explicação nenhuma no log.
        if (complainedAbout.add(chest.toImmutable())) {
            VillageColonyMod.LOGGER.info(
                    "Chest at {} has no free side for its badge — left unmarked",
                    chest.toShortString());
        }

        return false;
    }

    private static Optional<ItemFrameEntity> availableFrame(ServerWorld world, BlockPos chest, Direction facing) {
        BlockPos front = chest.offset(facing);
        WorldChunk chunk = chunkAt(world, front);
        if (chunk == null || !chunk.getBlockState(front).isAir()) {
            return Optional.empty();
        }
        ItemFrameEntity frame = new ItemFrameEntity(world, front, facing);
        return frame.canStayAttached() ? Optional.of(frame) : Optional.empty();
    }

    private static boolean spawn(ServerWorld world, ItemFrameEntity frame, ItemStack badge) {
        frame.setHeldItemStack(badge, false);
        frame.setInvulnerable(true);
        frame.setCustomName(Text.literal(TAG));
        return world.spawnEntity(frame);
    }

    /**
     * O quadro do mod colado neste baú, se já houver um.
     *
     * <p>Colado <b>neste</b> baú, e não perto dele. Até 2026-08-12 a
     * busca aceitava qualquer quadro do mod dentro da caixa, e perto
     * não é o mesmo que ser: dois baús reivindicados a dois blocos de
     * distância têm caixas que se cruzam, e o quadro pendurado no vão
     * entre eles cai dentro das duas.
     *
     * <p>O que isso custou na vila do autor: o baú do fabricante em
     * {@code 1118,70,727} e o do fazendeiro em {@code 1120,70,727}
     * disputavam o mesmo quadro, cada um repondo o seu ícone no ciclo
     * seguinte, para sempre — 30 em 30 segundos, um dos dois estava
     * sempre mentindo sobre de quem era o baú.
     *
     * <p>De qual baú o quadro é: {@code getAttachedBlockPos} devolve o
     * bloco de ar que o quadro ocupa — não a parede —, e é a parede que
     * responde. Ela é o bloco seguinte na direção contrária àquela para
     * a qual o quadro olha, que é como o próprio Vanilla a encontra em
     * {@code canStayAttached}.
     */
    private static Optional<ItemFrameEntity> existingMarkerAt(ServerWorld world, BlockPos chest) {
        Box around = new Box(chest).expand(1.0);

        List<ItemFrameEntity> frames = world.getEntitiesByClass(
                ItemFrameEntity.class, around, frame -> isOurs(frame, chest));

        return frames.isEmpty() ? Optional.empty() : Optional.of(frames.get(0));
    }

    /** Se este quadro é do mod <em>e</em> está pregado neste baú. */
    private static boolean isOurs(ItemFrameEntity frame, BlockPos chest) {
        Text name = frame.getCustomName();

        return name != null && TAG.equals(name.getString()) && chest.equals(wallOf(frame));
    }

    /** O bloco em que este quadro está pregado. */
    private static BlockPos wallOf(ItemFrameEntity frame) {
        return frame.getAttachedBlockPos().offset(frame.getHorizontalFacing().getOpposite());
    }

    /**
     * Tira a marca de um baú.
     *
     * <p>Chamado quando o trabalhador morre e o baú é liberado: um
     * machado pendurado num baú que já não é de ninguém mente para quem
     * está jogando.
     */
    public static boolean unmark(ServerWorld world, ColonyPos chest) {
        BlockPos position = MinecraftTypeAdapter.toBlockPos(chest);

        if (chunkAt(world, position) == null) {
            return false;
        }

        Optional<ItemFrameEntity> frame = existingMarkerAt(world, position);

        frame.ifPresent(found -> found.discard());

        return frame.isPresent();
    }

    /**
     * O ícone de cada profissão.
     *
     * <p>Ferramenta de ferro para quem tem ferramenta, e o objeto do
     * ofício para quem não tem. O nome no item é o que aparece ao passar
     * o mouse, e é ele que responde "de quem é este baú?" sem abrir o
     * log.
     */
    private static ItemStack badgeOf(ProfessionType profession) {
        ItemStack badge = new ItemStack(itemOf(profession));

        badge.set(DataComponentTypes.CUSTOM_NAME, Text.literal(label(profession)));

        return badge;
    }

    private static Item itemOf(ProfessionType profession) {
        return switch (profession) {
            case LUMBERJACK -> Items.IRON_AXE;
            case MINER -> Items.IRON_PICKAXE;
            case SHEPHERD -> Items.SHEARS;
            case SMELTER -> Items.FURNACE;
            case FARMER -> Items.IRON_HOE;
            case CARPENTER -> Items.CRAFTING_TABLE;
            case MASON -> Items.STONECUTTER;
            case BUILDER -> Items.BRICKS;
            case GROUNDSKEEPER -> Items.IRON_SHOVEL;
        };
    }

    /** O nome que aparece no item, em português, como o do aldeão. */
    private static String label(ProfessionType profession) {
        return switch (profession) {
            case LUMBERJACK -> "Baú do Lenhador";
            case MINER -> "Baú do Mineiro";
            case SHEPHERD -> "Baú do Pastor";
            case SMELTER -> "Baú do Fundidor";
            case FARMER -> "Baú do Fazendeiro";
            case CARPENTER -> "Baú do Carpinteiro";
            case MASON -> "Baú do Pedreiro";
            case BUILDER -> "Baú do Construtor";
            case GROUNDSKEEPER -> "Baú do Zelador";
        };
    }

    private static WorldChunk chunkAt(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
