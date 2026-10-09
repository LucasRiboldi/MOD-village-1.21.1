package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineRecovery;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.core.type.Side;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.BlockProtection;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.MineLighting;
import com.villagecolony.fabric.integration.MineMouth;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.StonePatch;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * A pedra exposta na superfície e o veio seguido pelo braço da mina, com a subida de volta e o patamar para ficar de pé — separado de
 * {@link MineDigging} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
final class MineVein {

    static {
        ServerMemory.register(MineVein.class, MineVein::clearAll);
    }

    /** Quantos minérios já cavados o rastro do veio lembra, por ramal. */
    static final int TRAIL_MAX = 256;

    /** Uma busca vazia inteira por pedra exposta descansa antes de repetir. */
    private static final int EMPTY_SURFACE_COOLDOWN = 100;

    /**
     * O rastro do veio de cada ramal — ADR-036 item 23, o veio inteiro: quando
     * o último minério não tem vizinho, volta-se pelo rastro atrás das
     * ramificações. Em memória, como o veio.
     */
    private static final IdentityHashMap<MineArm, Deque<BlockPos>> TRAILS = new IdentityHashMap<>();

    private MineVein() {
    }

    static void clearAll() {
        TRAILS.clear();
    }

    /**
     * O veio e o rastro de cada ramal, para o save — ADR-039 C. O ramal é
     * recriado ao carregar, então a chave é {@code colônia|índice do ramal}.
     */
    static net.minecraft.nbt.NbtCompound save() {
        net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();

        for (com.villagecolony.core.construction.model.Mine mine : VillageColonyMod.MINES.all()) {
            for (int index = 0; index < mine.arms().size(); index++) {
                MineArm arm = mine.arm(index);
                net.minecraft.nbt.NbtCompound entry = new net.minecraft.nbt.NbtCompound();

                arm.vein().ifPresent(ore -> entry.putLong("vein", MinecraftTypeAdapter.toBlockPos(ore).asLong()));
                Deque<BlockPos> trail = TRAILS.get(arm);

                if (trail != null && !trail.isEmpty()) {
                    entry.putLongArray("trail", trail.stream().mapToLong(BlockPos::asLong).toArray());
                }

                if (!entry.isEmpty()) {
                    nbt.put(mine.colonyId() + "|" + index, entry);
                }
            }
        }

        return nbt;
    }

    static void load(net.minecraft.nbt.NbtCompound nbt) {
        for (String key : nbt.getKeys()) {
            int bar = key.indexOf('|');
            java.util.UUID colony = bar > 0
                    ? com.villagecolony.fabric.integration.WorkMemoryKeys.uuid(key.substring(0, bar)) : null;
            String tail = bar > 0 ? key.substring(bar + 1) : "";
            int index = tail.length() == 1 && Character.isDigit(tail.charAt(0)) ? tail.charAt(0) - '0' : -1;
            Optional<com.villagecolony.core.construction.model.Mine> mine = VillageColonyMod.MINES.of(colony);

            if (index < 0 || mine.isEmpty() || index >= mine.get().arms().size()) {
                continue;
            }

            MineArm arm = mine.get().arm(index);
            net.minecraft.nbt.NbtCompound entry = nbt.getCompound(key);

            if (entry.contains("vein")) {
                arm.followVein(MinecraftTypeAdapter.toColonyPos(BlockPos.fromLong(entry.getLong("vein"))));
            }

            Deque<BlockPos> trail = new ArrayDeque<>();

            for (long at : entry.getLongArray("trail")) {
                trail.addLast(BlockPos.fromLong(at));
            }

            if (!trail.isEmpty()) {
                TRAILS.put(arm, trail);
            }
        }
    }

    /** Um veio novo, achado pelo túnel: o rastro recomeça. */
    static void startVein(MineArm arm, ColonyPos ore) {
        TRAILS.remove(arm);
        arm.followVein(ore);
    }

    private static Optional<BlockPos> exhausted(MineArm arm) {
        arm.veinExhausted();
        TRAILS.remove(arm);

        return Optional.empty();
    }

    /**
     * A pedra de superfície, quando a mina não tem onde nascer.
     *
     * <p><b>É alternativa, e não substituto.</b> A escada continua sendo
     * o caminho: é ela que traz carvão e ferro, e ela rende mais. Isto só
     * roda quando as vinte e quatro colunas da boca falharam — e o que
     * ele evita é o que a sessão de 2026-08-25 mostrou: uma vila cercada
     * de água ficou sem pedra nenhuma, a obra morreu de fome esperando
     * pedregulho, e a colônia parou de crescer por causa do terreno em
     * volta.
     *
     * <p>Mesma espiral da areia, mesmo teto por passagem, e a distinção
     * que o log precisa: "não terminei de olhar" não é "não há".
     */
    static Optional<BlockPos> exposedStone(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {
        return exposedStone(world, workerId, colonyId, center, java.util.Set.of());
    }

    /** O mesmo, fora das células que a mina planeja cavar — quem espera ramal não invade o poço. */
    static Optional<BlockPos> exposedStone(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center, java.util.Set<ColonyPos> mineCells) {

        Optional<BlockPos> found = RingSweep.aroundWithCooldown(
                workerId,
                RingSweep.Scan.GENERAL,
                center,
                MineDigging.surfaceRadius,
                column -> true,
                // <b>E a marca vale aqui também</b> — E44, 2026-09-10, e
                // este era o buraco que o verificador achou: o giveUp
                // marca TODA pedra largada, inclusive a de superfície,
                // mas só o lado da escada perguntava pela marca. Uma
                // pedra exposta do outro lado da água reproduzia o E44
                // inteiro numa colônia sem boca de mina — mesmo alvo,
                // mesma desistência, todo ciclo.
                column -> StonePatch.in(world, column, center.getY())
                        .filter(stone -> !mineCells.contains(MinecraftTypeAdapter.toColonyPos(stone)))
                        .filter(stone -> !MineMarks.isUnreachableAround(world, stone))
                        .filter(stone -> !MineFlooding.holdsBackFluid(world, stone)),
                world.getTime(),
                EMPTY_SURFACE_COOLDOWN);

        if (found.isEmpty()) {
            // Pelo recordAt, como a areia — 2026-09-11. Este é o irmão
            // da busca de areia e tem o mesmo desenho: roda por tique, e
            // a varredura em anéis alterna pausada e completa a cada
            // volta. Ele não apareceu na enxurrada das 02:03 porque
            // aquela colônia tinha boca de mina — o que é sorte, e não
            // defesa.
            IdleLog.recordAt(
                    colonyId,
                    MineDigging.SURFACE_SUBJECT,
                    RingSweep.pausedAt(workerId).isPresent()
                            ? IdleReason.SWEEP_INCOMPLETE
                            : IdleReason.NO_TARGET,
                    "no mine mouth, and no exposed stone within "
                            + MineDigging.surfaceRadius + " blocks either",
                    world.getTime());

            return Optional.empty();
        }

        IdleLog.clear(colonyId, MineDigging.SURFACE_SUBJECT);

        return found;
    }

    /**
     * O minério colado no que acabou de sair, se a veia continuar.
     *
     * <p><b>A veia manda no túnel.</b> Minério não vem sozinho, e voltar
     * para a escada com metade da veia aberta faria o aldeão andar até lá
     * outra vez na passagem seguinte. Enquanto houver minério ao lado do
     * último, é ele o alvo.
     *
     * <p>Quando acabar, a memória da veia sai e o túnel volta a mandar —
     * senão o mineiro reperguntaria por ela a cada passagem, para sempre.
     */
    static Optional<BlockPos> followingTheVein(ServerWorld world, Mine mine, MineArm arm) {
        Optional<BlockPos> from = arm.vein().map(MinecraftTypeAdapter::toBlockPos);

        if (from.isEmpty()) {
            return Optional.empty();
        }

        Optional<BlockPos> more = OreVein.beside(world, from.get());
        Deque<BlockPos> trail = TRAILS.get(arm);

        // O veio inteiro — ADR-036 23: sem vizinho aqui, a ramificação que
        // ficou para trás.
        while (more.isEmpty() && trail != null && !trail.isEmpty()) {
            from = Optional.of(trail.pop());
            more = OreVein.beside(world, from.get());
        }

        if (more.isEmpty()) {
            return exhausted(arm);
        }

        // A mesma guarda do MineCuts.nextCut, e aqui ela é a que fecha o laço —
        // 2026-09-03. Este método roda ANTES do túnel a cada passagem, e
        // a veia mora no Mine, que é da colônia: um minério sem lugar de
        // onde bater era servido de novo, e de novo, e ao mineiro
        // seguinte também. O MineTrouble.couldNotReach não alcançava o caso — ele só
        // recua o cursor do túnel, e diz por escrito que é "silencioso
        // quando a pedra não era do túnel — veio, areia".
        //
        // O resultado em jogo era a colônia inteira parada num bolsão de
        // carvão dentro da rocha: dezessete minutos, zero pedra.
        //
        // Desistir da veia é a saída barata, e é a que o stepBackUp já
        // escolhe logo abaixo — <i>a colônia prefere perder o minério a
        // perder o mineiro</i>. O túnel volta a mandar, e ele reabre o
        // caminho até este mesmo minério pelo lado de onde se alcança.
        //
        // <b>E a pedra de castigo entra por esta mesma porta</b> — E44,
        // 2026-09-10. O MineTrouble.couldNotReach larga a veia quando a pedra
        // recusada É a veia; o que ele não alcança é o minério VIZINHO
        // que já recusou noutra passagem, e é ele que este método serve.
        // Sem esta linha o laço voltaria pelo lado do minério, que é
        // justamente por onde ele voltou em 2026-09-03.
        if (WaterMineAccess.protects(world, mine, more.get())
                || nowhereToStand(world, more.get())
                || MineFlooding.holdsBackFluid(world, more.get())
                || MineMarks.isUnreachableAround(world, more.get())) {
            return exhausted(arm);
        }

        if (more.get().getY() < from.get().getY()) {
            Optional<BlockPos> step = stepBackUp(world, mine, from.get());

            if (step.isEmpty()) {
                // Sem degrau possível não se desce. A colônia prefere
                // perder o minério a perder o mineiro — a escada volta a
                // mandar, e ela é subível por construção.
                return exhausted(arm);
            }

            if (!step.get().equals(from.get())) {
                // E o degrau é alvo como qualquer outro: se não há de
                // onde bater nele, ele trava a veia do mesmo jeito que o
                // minério travaria — 2026-09-03.
                if (nowhereToStand(world, step.get())
                        || MineFlooding.holdsBackFluid(world, step.get())) {
                    return exhausted(arm);
                }

                // O degrau primeiro, e o veio NÃO avança: a passagem
                // seguinte acha o mesmo minério com a saída pronta.
                return step;
            }
        }

        Deque<BlockPos> path = TRAILS.computeIfAbsent(arm, ignored -> new ArrayDeque<>());

        path.push(from.get().toImmutable());

        if (path.size() > TRAIL_MAX) {
            path.removeLast();
        }

        arm.followVein(MinecraftTypeAdapter.toColonyPos(more.get()));

        return more;
    }

    /**
     * O bloco que falta abrir para se voltar de um degrau abaixo —
     * decisão do autor, 2026-08-27.
     *
     * <p><b>Por que o veio precisa disto e a escada não.</b> A escada da
     * Regra 29 abre três blocos por degrau desde 08-27, e sobe-se por
     * ela na mesma geometria em que se desce. O veio não tem geometria:
     * {@link OreVein#beside} olha as seis faces, e a de baixo é a
     * primeira da lista. Minério empilhado abre um poço de um bloco de
     * largura, e de poço não se sobe — o aldeão não pula dois.
     *
     * <p><b>Qual bloco falta é sempre o mesmo:</b> o teto do nível de
     * onde ele veio. Subir um degrau pede dois blocos de ar no nível de
     * destino, e o de baixo já é o minério recém-tirado; o de cima é
     * este. Com ele aberto, a subida se faz um degrau de cada vez até a
     * boca do poço.
     *
     * @param from o minério de onde o veio parte — o nível ao qual o
     *     mineiro precisa conseguir voltar
     * @return o bloco a abrir; o próprio {@code from} quando já dá para
     *     subir; vazio quando não há degrau possível e portanto não se
     *     deve descer
     */
    static Optional<BlockPos> stepBackUp(ServerWorld world, Mine mine, BlockPos from) {
        BlockPos ceiling = from.up();

        if (world.getBlockState(ceiling).isAir()) {
            return Optional.of(from);
        }

        // Rocha, e não só "cavável": uma laje que o jogador pôs de teto
        // passa no canDig e não é degrau nenhum — 2026-09-05.
        return MineRock.isDiggableRock(world, mine, ceiling)
                ? Optional.of(ceiling)
                : Optional.empty();
    }

    /**
     * Se não há de onde bater nesta pedra — 2026-09-03.
     *
     * <p><b>Uma pergunta só, num lugar só.</b> O
     * {@link MinerApproach#approachTo} devolve <i>a própria pedra</i> quando
     * não acha vizinho onde um aldeão caiba de pé, e essa igualdade é a
     * resposta — escrita à mão em três lugares, ela seria a próxima a
     * discordar de si mesma, que é a falha que o {@code standable} já
     * teve em 2026-08-28.
     *
     * <p>Toda posição que vira alvo do mineiro passa por aqui: a do
     * túnel, a do minério colado nela, o minério da veia e o degrau de
     * volta. Alvo que não passa é alvo que custa dois minutos de
     * expediente e devolve a tarefa.
     *
     * <p>Barato desde que as posições de aproximação vêm ordenadas por
     * distância — ver {@link MinerReach#APPROACH_OFFSETS}. A varredura
     * completa só é paga quando a resposta é <b>sim</b>.
     */
    static boolean nowhereToStand(ServerWorld world, BlockPos at) {
        return MinerApproach.approachTo(world, at).equals(at);
    }

    /**
     * A pedra ao lado de uma emparedada que o mineiro alcança: cavada, ela abre
     * lugar de pé para a outra — ADR-038 P6, degrau em vez de encerrar o ramal.
     */
    static Optional<BlockPos> roomBeside(ServerWorld world, Mine mine, BlockPos walled) {
        for (net.minecraft.util.math.Direction way : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos beside = walled.offset(way);

            // Os pés e depois a cabeça: o lugar de pé precisa de dois de altura.
            for (BlockPos open : new BlockPos[] {beside, beside.up()}) {
                if (MineRock.isDiggableRock(world, mine, open)
                        && !MineFlooding.holdsBackFluid(world, open)
                        && !MineMarks.isOutOfReach(world, open)
                        && !nowhereToStand(world, open)) {
                    return Optional.of(open.toImmutable());
                }

                if (!world.getBlockState(open).isAir()) {
                    break;
                }
            }
        }

        return Optional.empty();
    }
}
