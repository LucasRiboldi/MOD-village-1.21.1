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
 * A próxima pedra a cortar na mina, na ordem da escada, das salas e da galeria — separado de
 * {@link MineDigging} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class MineCuts {

    private MineCuts() {
    }

    /**
     * A primeira posição desta passagem que valha a picareta.
     *
     * <p>As já abertas são puladas de graça, e as impossíveis contam para
     * a curva da galeria.
     */
    static Optional<BlockPos> nextCut(
            ServerWorld world, UUID workerId, Mine mine, MineArm arm) {
        MineFrontier.findTheFrontier(world, mine, arm);

        for (int look = 0; look < MineDigging.CUTS_PER_SEARCH; look++) {
            if (arm.reachedTheEndOfTheArm()) {
                // <b>O teto de raio do autor</b> — 2026-09-04: "o mineiro
                // deve priorizar o perímetro da vila". Ver MineShaft.ARM.
                //
                // O ramal acaba aqui, e não vira: virar era o jeito de um
                // cursor só visitar os quatro rumos em sequência. Agora
                // cada rumo é de um mineiro, e quem troca é ele.
                arm.finish();

                break;
            }

            BlockPos at = MinecraftTypeAdapter.toBlockPos(arm.nextPosition());

            if (MineMouth.isPortalBlock(
                    MinecraftTypeAdapter.toBlockPos(mine.entry()),
                    MinecraftTypeAdapter.toDirection(mine.shaft().descent()),
                    at)) {
                continue;
            }

            if (!world.isInBuildLimit(at)) {
                arm.finish();

                break;
            }

            BlockState state = world.getBlockState(at);

            if (MineRock.isOpenSpace(world, at, state)) {
                // Já aberto, ou água e lava. Nenhum dos dois se cava.
                //
                // <b>E a tocha da própria mina</b> — 2026-08-28. Uma
                // posição com luz é espaço aberto, não rocha: sem isto
                // o mineiro cavaria a luz que acabou de pôr, que é o
                // defeito do lampião no primeiro degrau de 08-27 de
                // volta pela porta da frente.
                //
                // <b>E o degrau que o jogador construiu</b> — 2026-09-05.
                // A lista mora no isOpenSpace, que é a mesma que o
                // isStillClosed usa: recuo do cursor e escolha do alvo
                // não podem discordar.
                continue;
            }

            if (state.getHardness(world, at) < 0
                    || BlockProtection.isVillageOriginal(world, at)
                    || BlockProtection.isColonyBuilt(at)) {

                // Bedrock, casa da vila, casa da colônia. A Regra 3 e o
                // impossível, pela mesma porta.
                if (arm.blockedAgain(MineDigging.BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} hit something it cannot dig - the branch ends here",
                            workerId);

                    break;
                }

                continue;
            }

            if (MineFlooding.holdsBackFluid(world, at)) {
                // <b>Pedra que segura líquido fica</b> — pedido do autor,
                // 2026-09-25: "não deixar o mineiro quebrar o bloco que tem
                // líquido atrás". Conta para a curva como o bedrock: a
                // galeria contorna a nascente em vez de abri-la.
                if (arm.blockedAgain(MineDigging.BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} keeps finding stone that holds back water or lava"
                                    + " - the branch ends here",
                            workerId);

                    break;
                }

                continue;
            }

            if (MineVein.nowhereToStand(world, at)) {
                // <b>Emparedada: não há vizinho onde um aldeão caiba</b> —
                // 2026-09-02. O approachTo devolve a própria pedra quando
                // não acha lugar de ficar de pé, e o javadoc dele
                // delegava o caso ao guarda de travamento. A sessão das
                // 21:44 mostrou o preço: seis vezes a mesma frase — "the
                // place to stand is the stone itself (no free neighbour
                // to stand on)" —, dois minutos de expediente cada, e
                // zero pedra em dezessete minutos.
                //
                // Impossível de trabalhar é impossível, e vai pela mesma
                // porta do bedrock: conta para a curva, e a galeria
                // contorna o vão em vez de mirar para dentro dele.
                if (arm.blockedAgain(MineDigging.BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} hit stone with nowhere to stand - the branch ends here",
                            workerId);

                    break;
                }

                continue;
            }

            if (MineMarks.isOutOfReach(world, at)) {
                // <b>Já cobrou o preço e não foi alcançada</b> — E44,
                // 2026-09-10. Um mineiro andou os 2.400 tiques de
                // expediente até aqui e não chegou; enquanto o prazo
                // corre, a picareta vai adiante em vez de o ramal inteiro
                // parar nesta pedra e os dois mineiros se revezarem nela.
                //
                // <b>Conta para a curva</b>, e é a defesa que a lição de
                // 2026-08-27 exige: se a galeria toda for inalcançável,
                // pular uma a uma marcharia pela ordem de cavar com o
                // mundo intacto, que é exatamente o defeito que o
                // MineTrouble.couldNotReach existe para impedir. Contando, o ramal
                // acaba depois de MineDigging.BLOCKED_BEFORE_TURNING recusas e a mina
                // desce um nível — que é resposta, e não marcha.
                if (arm.blockedAgain(MineDigging.BLOCKED_BEFORE_TURNING)) {
                    VillageColonyMod.LOGGER.info(
                            "Miner {} keeps finding stone it cannot reach"
                                    + " - the branch ends here",
                            workerId);

                    break;
                }

                continue;
            }

            // <b>E aqui NÃO se zera a curva</b> — 2026-09-11. Esta linha
            // era {@code arm.digging()}, e ela zerava a contagem de
            // recusas ao <i>servir</i> a pedra, não ao quebrá-la. É a
            // razão de a curva nunca virar: o cursor pulava as marcadas
            // contando 1, 2, 3, achava a primeira sem marca, zerava, e o
            // mineiro ia falhar nela. Na passagem seguinte a conta
            // recomeçava do zero, e assim para sempre — a sessão de
            // 2026-09-11 às 00:04 mostrou nove desistências em oito
            // minutos sem um único "went one level deeper", e a colônia
            // respondendo "no miner branch work" cinco vezes.
            //
            // Quem zera agora é a picareta, de dentro do MinerWork,
            // quando o bloco de verdade sai do mundo. Ver MineTrouble.pickaxeTook.

            // O minério da parede vem antes da parede — 2026-08-21. Um
            // túnel de dois blocos de altura mostra o que está colado
            // nele, e passar direto era o mineiro trazendo pedregulho de
            // uma galeria cheia de carvão.
            Optional<BlockPos> ore = OreVein.isOre(state)
                    ? Optional.of(at)
                    : OreVein.beside(world, at);

            // <b>E o minério da parede também precisa ter onde se ficar de
            // pé</b> — 2026-09-03. A guarda de emparedada acima conferia a
            // posição do túnel e devolvia <b>outro bloco</b>: o minério
            // colado nela, que nunca passou por conferência nenhuma. A
            // colônia mira o carvão dentro da rocha, o approachTo devolve
            // "o próprio minério", o mineiro anda para dentro da parede, e
            // dois minutos depois o guarda de travamento devolve a tarefa.
            //
            // É exatamente o defeito que 09-02 fechou, vazando pela porta
            // do minério — e pela pior delas, porque é o minério que
            // justifica a galeria existir.
            //
            // Sem lugar de onde bater, a parede vale mais que o minério
            // atrás dela: devolve-se a posição do túnel, que já passou pela
            // guarda. O veio NÃO é lembrado — lembrar um minério
            // inalcançável é o que faz o MineVein.followingTheVein reservi-lo para
            // sempre. Aberto o túnel, a passagem seguinte o reencontra, e
            // aí com lugar de onde bater.
            // <b>E a marca do minério da parede vale aqui</b> — 2026-09-15.
            // O autor viu em jogo: <i>"os mineiros estavam parados no fundo
            // da mina em local que não chegaram escavando"</i>, e o log
            // mostrou a mesma pedra servida três vezes em dois minutos —
            // 665,32,-2866, com 6.000 tiques de castigo já escritos na
            // primeira desistência, e o mineiro demitido do ofício na
            // terceira.
            //
            // O furo era de porta, não de marca: a guarda do E44 acima
            // pergunta pelo `at`, que é a posição do TÚNEL. O minério
            // colado nela sai por esta linha, e só passava pelo
            // MineVein.nowhereToStand — de modo que o giveUp escrevia a marca e
            // ninguém a lia. Enquanto o prazo corre, a picareta vai
            // adiante, como já vai para a posição do túnel.
            //
            // Devolve o `at` em vez de pular a passagem inteira: a parede
            // ainda vale a picareta, e abri-la é justamente o que dá ao
            // minério um lado de onde se alcance — a mesma saída que o
            // ramo do `nowhereToStand` escolhe, e pelo mesmo motivo.
            if (ore.isEmpty()
                    || (!ore.get().equals(at)
                            && (MineVein.nowhereToStand(world, ore.get())
                                    || MineFlooding.holdsBackFluid(world, ore.get())
                                    || MineMarks.isOutOfReach(world, ore.get())))) {

                return Optional.of(at);
            }

            arm.followVein(MinecraftTypeAdapter.toColonyPos(ore.get()));

            if (!ore.get().equals(at)) {
                // A posição do túnel não foi cavada, e não pode ser
                // perdida: sem isto o cursor passaria por cima dela e o
                // túnel ficaria com um bloco no meio para sempre.
                arm.holdPosition();
            }

            return ore;
        }

        return Optional.empty();
    }


    /**
     * Para que lado esta colônia abre a mina.
     *
     * <p>Sai do identificador da colônia, e é de propósito: duas colônias
     * vizinhas cavam para lados diferentes. Desde que a mina é gravada
     * isto virou redundância — e continua valendo a pena: save perdido, a
     * mina nova abre para o mesmo lado da antiga.
     */
    static Side sideOf(UUID colonyId) {
        return Side.values()[Math.floorMod(colonyId.hashCode(), Side.values().length)];
    }
}
