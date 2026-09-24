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
 * O arco, a lanterna e a luz da galeria de uma mina aberta — separado de
 * {@link MineDigging} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
final class MineFurnishing {

    private MineFurnishing() {
    }

    /**
     * A lanterna e o baú da boca, e a luz da galeria.
     *
     * <p>Chamada só ao abrir ou trocar de boca. Mina já conhecida recebe
     * apenas luz: o arco é construção inicial e, se o jogador o destruir,
     * não pode voltar em uma passagem posterior.
     *
     * <p><b>E a luz da galeria desde 2026-08-28</b>, que é da mesma
     * natureza: de graça, idempotente, e no que já está cavado.
     */
    static void furnishAndLight(ServerWorld world, Mine mine) {
        BlockPos mouth = MinecraftTypeAdapter.toBlockPos(mine.shaft().entry());

        MineMouth.Furnished furnished = MineMouth.furnish(
                world,
                mouth,
                MinecraftTypeAdapter.toDirection(mine.shaft().descent()),
                mine.archRaised());

        // <b>Marca o arco na passagem em que ele sobe</b> — 2026-09-11, e
        // <b>pelo arco, não pelo baú</b> desde 2026-09-12. A primeira
        // versão exigia {@code chest.isPresent()} aqui, e o
        // gauntlet-verifier provou o buraco: boca sem nenhum vizinho
        // livre nunca ganha baú, logo nunca marcava o arco, logo o arco
        // derrubado voltava para sempre — o defeito sobrevivendo num canto
        // dele. Os dois sinais são independentes, e o {@code Furnished}
        // existe para os manter assim.
        //
        // A linha sai <b>uma vez</b>, e não por ciclo, porque é aqui que o
        // falso vira verdadeiro. O autor pediu por ela depois de quebrar
        // um arco em jogo e o arco voltar: sem esta linha, um arco que
        // reaparecesse por outro motivo não deixaria pista nenhuma.
        //
        // <b>E ela não diz que o mod ergueu</b> — 2026-09-12. Dizia "got
        // its stone arch", e passou a mentir quando o critério virou
        // "posição firme, de quem quer que seja": boca cavada em rocha
        // intacta fecha o assunto sem o mod pôr uma pedra, e o log
        // reivindicava a obra. Num projeto em que log torto já custou
        // sessões de diagnóstico, "resolvido" é o que aconteceu.
        if (furnished.archRaisedNow()) {
            mine.archIsUp();

            VillageColonyMod.LOGGER.info(
                    "Mine mouth at {} has its stone arch settled — it will not be raised"
                            + " again, so breaking it is final",
                    mouth.toShortString());
        }

        // <b>E a boca não devolve mais baú</b> — decisão do autor,
        // 2026-09-15: <i>"retire o baú da boca da mina, use só o baú de
        // cada mineiro"</i>.
        //
        // Aqui havia a queixa de 2026-09-02 — "the mine mouth has no chest
        // and none could be placed" —, e o autor a viu de novo no log de
        // 09-15, a cada passagem. Ela foi embora com a causa: sem baú de
        // boca não há posição a procurar, e a falta deixou de ser falta.
        // O assunto é limpo uma vez para que a queixa antiga não fique
        // pendurada no IdleLog de quem carregar um save da véspera.
        IdleLog.clear(mine.colonyId(), MineDigging.CHEST_SUBJECT);

        lightMine(world, mine);
    }

    /** Ilumina apenas trechos já abertos; não reconstrói a entrada existente. */
    static void lightMine(ServerWorld world, Mine mine) {
        for (MineArm arm : mine.arms()) {
            MineLighting.light(world, mine, arm);
        }
    }
}
