package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.construction.model.MineShaft;
import com.villagecolony.core.construction.service.MineRecovery;
import com.villagecolony.core.type.ColonyPos;
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Quando a mina não deixa trabalhar: pedra inalcançável, veio de água, picareta que não tirou nada, e o rumo novo ou a culpa na boca — separado de
 * {@link MineDigging} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class MineTrouble {

    private MineTrouble() {
    }

    /**
     * O mineiro desistiu desta pedra — 2026-08-27.
     *
     * <p>Devolve a posição ao cursor da galeria, quando ela é de lá.
     * Sem isto o cursor passava por cima dela: o mod marchava pela ordem
     * de cavar enquanto o mundo continuava rocha maciça, e três sessões
     * seguidas terminaram com zero blocos e a galeria intacta.
     *
     * <p>Silencioso quando a pedra não era do túnel — veio, areia, ou a
     * posição que o outro mineiro já ultrapassou. Ver
     * {@link Mine#holdPositionAt}.
     */
    public static void couldNotReach(UUID colonyId, BlockPos stone) {
        VillageColonyMod.MINES.of(colonyId).ifPresent(mine -> {
            ColonyPos at = MinecraftTypeAdapter.toColonyPos(stone);

            // <b>Perguntado aos quatro ramais</b> — 2026-09-04, e sem
            // saber qual foi. O holdPositionAt já se defende: ele só
            // recua o cursor que acabou de entregar <b>esta</b> posição,
            // e os outros três respondem não. Era essa mesma guarda que
            // impedia dois mineiros na mesma escada de recuarem o cursor
            // duas vezes por um bloco.
            boolean kept = false;
            boolean dropped = false;

            for (MineArm arm : mine.arms()) {
                kept |= arm.holdPositionAt(at);

                if (arm.vein().filter(at::equals).isPresent()) {
                    arm.veinExhausted();

                    dropped = true;
                }
            }

            if (kept) {
                VillageColonyMod.LOGGER.info(
                        "The gallery keeps its place at {} — it was not dug",
                        stone.toShortString());
            }

            // <b>E a veia se larga</b> — 2026-09-03. O javadoc acima diz
            // que este método é silencioso quando a pedra é do veio, e
            // era: o cursor do túnel recuava e a veia ficava.
            //
            // Só que a veia mora no Mine, que é da colônia, e o
            // MineVein.followingTheVein roda ANTES do túnel a cada passagem. Um
            // minério que o mineiro não alcançou era servido de volta na
            // passagem seguinte, ao mesmo mineiro e ao que herdasse a
            // escada pelo MineClaims.stepAside — um laço fechado, sem
            // saída, para a colônia inteira.
            //
            // A guarda do MineVein.followingTheVein evita quase todos os casos na
            // entrada; esta é a que fecha o resto, porque nem toda
            // desistência é por falta de lugar: chunk descarregado,
            // caminho que a navegação não traçou, o jogador tapando o
            // buraco. Desistir da veia devolve o mineiro ao túnel, que é
            // contínuo por construção.
            if (dropped) {
                VillageColonyMod.LOGGER.info(
                        "The vein at {} is dropped — the miner could not reach it",
                        stone.toShortString());
            }
        });
    }

    /**
     * A picareta abriu um veio de água, e a galeria vira — 2026-09-03.
     *
     * <p><b>Decisão do autor:</b> <i>"colocar um bloco no lugar para
     * encerrar o fluxo da água e seguir por outro caminho"</i>. O bloco é
     * do {@link MineFlooding}; a outra metade — <i>seguir por outro
     * caminho</i> — é esta.
     *
     * <p>Vai pela mesma porta do bedrock e da pedra sem onde pisar, que é
     * a frase do autor de 2026-08-21: <i>sempre que encontrar uma barreira
     * que impeça de realizar estas ações ele começa a recolher para outro
     * lado</i>. Um lençol de água é exatamente isso — a diferença é que
     * esta barreira <b>persegue</b> quem a ignora, porque escorre.
     *
     * <p><b>Vira na hora, e não depois de oito recusas.</b> O
     * {@code blockedAgain} existe para não virar a galeria por causa de um
     * bloco duro solto no meio do caminho; água não é solta. Insistir na
     * mesma direção é cavar de volta para dentro do lençol, e o preço de
     * errar é a mina inundada.
     */
    public static void flooded(UUID colonyId, UUID workerId, BlockPos at) {
        VillageColonyMod.MINES.of(colonyId).ifPresent(mine -> {
            // <b>O ramal de quem cavou</b> — 2026-09-04. A água é do
            // lugar em que a picareta bateu, e só aquele rumo entra nela;
            // encerrar os quatro tiraria três mineiros de frentes que
            // estão secas.
            OptionalInt claimed =
                    MineClaims.claimArm(colonyId, workerId, mine.branchesOpenNow());

            if (claimed.isEmpty()) {
                return;
            }

            mine.arm(claimed.getAsInt()).finish();

            MineClaims.releaseArm(colonyId, workerId);

            mine.deepenIfEveryArmIsDone();

            VillageColonyMod.LOGGER.info(
                    "The branch turns away from the water at {}", at.toShortString());
        });
    }

    /**
     * A picareta tirou um bloco: a curva deste ramal recomeça —
     * 2026-09-11.
     *
     * <p><b>Irmã do {@link #flooded}, e pelo mesmo motivo</b>: quem sabe
     * que o bloco saiu do mundo é o {@code MinerWork}, e quem guarda a
     * contagem é o braço. O que muda em relação ao que havia antes é
     * <i>quando</i> a conta volta a zero — era ao servir a pedra, e
     * passa a ser ao quebrá-la.
     *
     * <p>A distinção é o conserto inteiro. Servir é uma aposta: o
     * cursor escolhe a posição e só depois se descobre se o mineiro
     * chega nela. Zerar na aposta apagava justamente a prova que a curva
     * existe para juntar — a de que esta frente não está rendendo —, e
     * bastava uma pedra nova sem marca por passagem para a contagem
     * nunca passar de zero. Quebrar não é aposta: é o ramal rendendo, e
     * aí a curva deve mesmo recomeçar.
     *
     * <p>Sem reserva de ramal, como o {@link #armOf}: quem acabou de
     * cavar já tem o seu, e pedir outro tiraria a última frente livre de
     * quem ia cavar nela.
     */
    /**
     * A mina girou em falso: troca a hélice, ou a boca — E45, 2026-09-16.
     *
     * <p><b>A decisão do autor, 2026-09-16:</b> girar primeiro, mudar a
     * boca só quando as quatro hélices falharem. Girar é barato e reusa
     * {@code MineShaft.rerouted()}, que já existe para o fundo da mina;
     * trocar a boca abandona o poço iniciado, e isso se paga só depois de
     * o rumo estar descartado.
     *
     * <p><b>O laço que isto fecha.</b> Com a boca emparedada, o ramal
     * fechava em oito recusas, era solto, e a passagem seguinte o servia
     * no mesmo cursor: 17.518 vezes em trinta e um minutos, dez por
     * segundo, sem uma pedra sair do mundo. Nenhum guarda pegava porque
     * todos eram zerados pelo próprio caminho de falha — ver
     * {@code Mine.turnsWithoutAPickaxe}.
     *
     * <p><b>E sem boca nova a mina fica onde está.</b> Esquecer a mina
     * sem ter onde recriá-la deixaria a colônia sem mina nenhuma e sem
     * nada dizendo por quê; o mineiro cai no {@code exposedStone}, que é
     * a rede de segurança que já existe, e a passagem seguinte tenta de
     * novo.
     */
    static void rerouteOrBlameTheMouth(
            ServerWorld world, UUID colonyId, Mine mine, BlockPos center) {

        // A decisão é pura e mora em MineRecovery — decisão 3B,
        // 2026-09-24. Este método só executa o efeito que ela escolheu:
        // MineDigging é quem tem o ServerWorld e a MineSite, não quem
        // decide se ainda vale girar a hélice.
        MineRecovery.Decision decision = MineRecovery.recover(mine);

        if (decision == MineRecovery.Decision.NO_ACTION) {
            // O guarda que chama este método já testou turnedWithoutAPickaxe(),
            // então isto nunca deveria acontecer — mas nada aqui edita o
            // mundo, e uma decisão inesperada não pode travar a mina.
            return;
        }

        if (decision == MineRecovery.Decision.REROUTE) {
            MineShaft before = mine.shaft();

            mine.reroute();

            VillageColonyMod.LOGGER.info(
                    "Mine {} turned in place {} times without a pickaxe — turning the helix"
                            + " from {} to {} (helix {} of {})",
                    colonyId,
                    Mine.TURNS_BEFORE_REROUTING,
                    before.descent(),
                    mine.shaft().descent(),
                    mine.helicesTried(),
                    Mine.HELICES_BEFORE_BLAMING_THE_MOUTH);

            return;
        }

        Side descent = MineCuts.sideOf(colonyId);

        Optional<BlockPos> mouth = MineSite.mouthOf(world, center, descent);

        if (mouth.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Mine {} tried all {} helices and found no better mouth within {} blocks"
                            + " of {} — the miner falls back to exposed stone",
                    colonyId,
                    Mine.HELICES_BEFORE_BLAMING_THE_MOUTH,
                    MineSite.distance(),
                    center.toShortString());

            return;
        }

        VillageColonyMod.LOGGER.info(
                "Mine {} tried all {} helices without a pickaxe — the mouth is the problem,"
                        + " and the mine starts over at {}",
                colonyId,
                Mine.HELICES_BEFORE_BLAMING_THE_MOUTH,
                mouth.get().toShortString());

        VillageColonyMod.MINES.removeOfColony(colonyId);

        Mine replacement = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), descent));

        MineFurnishing.furnishAndLight(world, replacement);
    }

    /**
     * O fundo não reaproveita a mesma abertura: a mina acabada é esquecida
     * e o próximo ciclo começa no lado oposto da vila.
     */
    static void abandonAtBottom(
            ServerWorld world, UUID colonyId, Mine mine, BlockPos center) {

        Side opposite = mine.shaft().descent().opposite();
        Optional<BlockPos> mouth = MineSite.mouthOnSide(world, center, opposite);

        if (mouth.isEmpty()) {
            VillageColonyMod.LOGGER.warn(
                    "Mine {} reached the world bottom; waiting for an opposite mouth before replacing it",
                    colonyId);
            return;
        }

        VillageColonyMod.MINES.removeOfColony(colonyId);

        Mine replacement = VillageColonyMod.MINES.open(
                colonyId,
                MineShaft.from(MinecraftTypeAdapter.toColonyPos(mouth.get()), opposite));

        MineFurnishing.furnishAndLight(world, replacement);

        VillageColonyMod.LOGGER.info(
                "Mine {} reached the world bottom and moved from {} to opposite mouth {}",
                colonyId,
                MinecraftTypeAdapter.toBlockPos(mine.entry()).toShortString(),
                mouth.get().toShortString());
    }

    public static void pickaxeTook(UUID colonyId, UUID workerId) {
        MineDigging.armOf(colonyId, workerId).ifPresent(MineArm::digging);

        // <b>E a mina inteira sai da desconfiança</b> — E45, 2026-09-16.
        // O contador do braço é apagado por finish() e por restartAt, que
        // é justamente o caminho do laço; o da mina só sai daqui, porque
        // só aqui houve prova de que o desenho se cava. Ver
        // Mine.turnsWithoutAPickaxe.
        VillageColonyMod.MINES.of(colonyId).ifPresent(Mine::pickaxeTook);
    }
}
