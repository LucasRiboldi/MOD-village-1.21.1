package com.villagecolony.fabric.work;

import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.fabric.integration.RingSweep;
import com.villagecolony.fabric.integration.SandPatch;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Varrer a superfície atrás de areia — 2026-08-20.
 *
 * <p><b>Por que a areia não desce a mina.</b> A Regra 29 mandou o mineiro
 * cavar fundo, e para pedra isso é certo: há pedra em toda parte abaixo
 * do chão. Areia é o contrário — praia, duna e margem de lago, e a vinte
 * blocos de profundidade não há nenhuma fora do deserto. Descer atrás
 * dela seria cavar vinte blocos para não achar.
 *
 * <p>A mesma profissão, dois caminhos, e quem decide é o recurso que a
 * tarefa pede. Este é o de cima; {@link MineDigging} é o de baixo.
 *
 * <p>É a espiral do {@link RingSweep}, e é ela que voltou a ter dono: a
 * varredura nasceu para o mineiro de afloramento que a mina aposentou no
 * mesmo dia, e ficou sem quem a chamasse.
 */
public final class SandGathering {

    /**
     * Até onde se procura areia em volta da vila.
     *
     * <p>É o raio da superfície, e é o mesmo que o mineiro de afloramento
     * usava antes de a Regra 29 mandá-lo descer.
     */
    private static final int SAND_RADIUS = 48;

    private static int sandRadius = SAND_RADIUS;

    /**
     * O assunto da busca de areia, separado do da mineração.
     *
     * <p>A chave do {@link IdleLog} inclui o assunto, e é de propósito:
     * um mineiro sem tarefa e um mineiro que não acha areia são dois
     * silêncios diferentes, e um calaria o outro se dividissem a chave.
     */
    private static final String SUBJECT = "miner sand";

    private SandGathering() {
    }

    /** Encurta a busca de areia. Só os testes precisam disso. */
    public static void shortenSandRadiusTo(int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + blocks);
        }

        sandRadius = blocks;
    }

    /** Devolve o raio ao valor de jogo. */
    public static void restoreSandRadius() {
        sandRadius = SAND_RADIUS;
    }

    /**
     * A próxima areia exposta em volta da vila.
     *
     * <p>Vazio não quer dizer "não há areia": pode ser o orçamento da
     * passagem acabando no meio do raio. Quem sabe a diferença é o
     * {@code RingSweep.pausedAt}, e ela vai para o log — dizer "não há"
     * quando se quer dizer "não terminei de olhar" é o log mentindo
     * justamente onde ele serve.
     */
    public static Optional<BlockPos> nextTarget(
            ServerWorld world, UUID workerId, UUID colonyId, BlockPos center) {

        Optional<BlockPos> found = RingSweep.around(
                workerId,
                center,
                sandRadius,
                // A areia entra pela mesma porta — E44, 2026-09-10. O
                // MinerWork.giveUp marca o alvo seja ele pedra ou areia,
                // e uma duna inalcançável tem exatamente a mesma forma de
                // laço. Ver MineMarks.
                column -> SandPatch.in(world, column, center.getY())
                        .filter(sand -> !MineMarks.isUnreachableAround(world, sand)));

        if (found.isEmpty()) {
            // Pelo IdleLog, e não direto no logger: uma varredura de raio
            // 48 são dez passagens, e dizer "não achei" em cada uma daria
            // duas linhas por segundo numa vila sem praia. Fala na
            // primeira vez e cala enquanto o motivo não mudar.
            // Pelo recordAt, e não pelo record — 2026-09-11. Este
            // método roda no laço de trabalho do mineiro, por tique, e o
            // motivo alterna por construção: toda volta da varredura
            // termina em NO_TARGET e a seguinte recomeça em
            // SWEEP_INCOMPLETE. A regra de transição sozinha deixou
            // 4.389 linhas num log de 6.117 na sessão das 02:03.
            IdleLog.recordAt(
                    colonyId,
                    SUBJECT,
                    RingSweep.pausedAt(workerId).isPresent()
                            ? IdleReason.SWEEP_INCOMPLETE
                            : IdleReason.NO_TARGET,
                    "sand within " + sandRadius + " blocks",
                    world.getTime());

            return Optional.empty();
        }

        IdleLog.clear(colonyId, SUBJECT);

        return found;
    }

    /**
     * Esquece onde a varredura deste mineiro parou.
     *
     * <p>Chamado quando ele desiste do bloco e quando some do mundo. Sem
     * isso a passagem seguinte reencontraria exatamente a mesma areia
     * inalcançável, que é a roda que a Regra 9 fechou do lado do
     * lenhador.
     */
    public static void forget(UUID workerId) {
        RingSweep.forget(workerId);
    }
}
