package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
 * não toca no que o jogador construiu. Ele só acrescenta um quadro numa
 * face vazia; se não houver face vazia, o baú fica sem marca e isso não é
 * erro — é o mod não abrindo espaço na construção de quem joga.
 */
public final class ChestMarker {

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
     * <p>Chamável a cada ciclo: baú já marcado com a profissão certa não
     * gera nada. Quem trocou de profissão tem o item do quadro trocado,
     * em vez de ganhar um segundo quadro.
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

            if (markOne(world, MinecraftTypeAdapter.toBlockPos(chest.get()), profession.get())) {
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
        return markOne(world, chest, profession);
    }

    /**
     * Põe ou corrige a marca de um baú.
     *
     * @return true se algo mudou no mundo
     */
    private static boolean markOne(ServerWorld world, BlockPos chest, ProfessionType profession) {
        if (chunkAt(world, chest) == null) {
            // Chunk descarregado. Nunca forçar carregamento de dentro do
            // ciclo do servidor — ver §11.
            return false;
        }

        ItemStack badge = badgeOf(profession);

        Optional<ItemFrameEntity> existing = existingMarkerAt(world, chest);

        if (existing.isPresent()) {
            ItemFrameEntity frame = existing.get();

            if (frame.getHeldItemStack().isOf(badge.getItem())) {
                return false;
            }

            // Mesmo baú, outra profissão: troca o ícone em vez de
            // pendurar um segundo quadro.
            frame.setHeldItemStack(badge);

            return true;
        }

        return place(world, chest, badge);
    }

    /**
     * Pendura o quadro na primeira face livre.
     *
     * <p>Só as quatro faces laterais. Em cima do baú fica a tampa, e um
     * quadro ali é a primeira coisa que o jogador acerta ao tentar abrir.
     */
    private static boolean place(ServerWorld world, BlockPos chest, ItemStack badge) {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos front = chest.offset(facing);
            WorldChunk chunk = chunkAt(world, front);

            if (chunk == null || !chunk.getBlockState(front).isAir()) {
                continue;
            }

            ItemFrameEntity frame = new ItemFrameEntity(world, front, facing);

            if (!frame.canStayAttached()) {
                continue;
            }

            frame.setHeldItemStack(badge, false);
            frame.setInvulnerable(true);
            frame.setCustomName(Text.literal(TAG));

            world.spawnEntity(frame);

            return true;
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
            case MANUFACTURER -> Items.CRAFTING_TABLE;
            case BUILDER -> Items.BRICKS;
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
            case MANUFACTURER -> "Baú do Fabricante";
            case BUILDER -> "Baú do Construtor";
        };
    }

    private static WorldChunk chunkAt(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
