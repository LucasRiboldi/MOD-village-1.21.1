package com.villagecolony.fabric.event;

import com.villagecolony.VillageColonyMod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Conta o trabalhador que o registro tem e o mundo não — 2026-09-02.
 *
 * <p><b>Isto mede, não conserta</b>, e é de propósito. A auditoria de
 * {@code docs/research/estado-que-sobrevive-ao-dono.md} achou um caminho de
 * perda de dono que nenhum evento cobre: o aldeão que some sem passar por
 * {@code AFTER_DEATH} nem por {@code MOB_CONVERSION} — removido por outro
 * mod, por comando em chunk descarregado, ou com a região do mundo perdida.
 * Ele fica registrado para sempre, segurando vaga de profissão e reserva de
 * baú, e atravessa o save.
 *
 * <p><b>Por que não se conserta agora.</b> A regra que impede o conserto
 * óbvio é <i>ausência não é morte</i>: podar por "não achei a entidade"
 * apagaria trabalhador legítimo toda vez que o jogador se afastasse. E o
 * gatilho é raro — aldeão não despawna sozinho, o que foi conferido no
 * fonte do 1.21.1 durante o E32 ({@code canImmediatelyDespawn} devolve
 * {@code false} incondicional). Consertar um defeito raro com uma poda
 * arriscada é trocar um problema conhecido por um pior.
 *
 * <p>Então primeiro se mede. Uma sessão com esta linha no log responde se
 * o fantasma existe de verdade; até lá, qualquer conserto seria escrito
 * contra uma suposição — que é exatamente o que o E32 ensinou a não fazer.
 *
 * <p><b>A ausência precisa ser persistente para valer notícia.</b> Um
 * aldeão pode estar fora da área carregada num tique e voltar no seguinte,
 * mesmo numa colônia ativa. Só depois de {@link #MISSES_BEFORE_NEWS}
 * passagens seguidas sem ser encontrado ele vira linha — e a linha sai
 * <b>uma vez</b>, porque a condição é permanente e repeti-la a cada ciclo
 * afogaria o log.
 */
public final class PhantomWorkerLog {

    /**
     * Quantas passagens seguidas sem achar o aldeão antes de noticiar.
     *
     * <p>O ciclo da colônia roda a cada trinta segundos, então três
     * passagens são um minuto e meio: longo o bastante para descartar
     * quem só andou para fora da área carregada, e curto o bastante para
     * caber numa sessão de jogo, que nesta bancada tem durado de dois a
     * catorze minutos.
     */
    public static final int MISSES_BEFORE_NEWS = 3;

    /** Quantas passagens seguidas cada trabalhador já sumiu. */
    private static final Map<UUID, Integer> MISSES = new LinkedHashMap<>();

    /** De quem a notícia já saiu, para ela não sair de novo. */
    private static final Set<UUID> ANNOUNCED = new java.util.LinkedHashSet<>();

    private PhantomWorkerLog() {
    }

    /**
     * Uma observação de um trabalhador, e o que ela decide.
     *
     * <p>Pura de propósito — sem mundo, sem entidade —, para que a regra
     * de quantas ausências viram notícia possa ser afirmada sem subir
     * servidor. Quem pergunta ao mundo é {@link #probe}.
     *
     * @param present se o aldeão foi encontrado nesta passagem
     * @return se esta observação é a que vira linha de log
     */
    public static boolean observe(UUID villagerId, boolean present) {
        if (present) {
            MISSES.remove(villagerId);
            ANNOUNCED.remove(villagerId);

            return false;
        }

        int misses = MISSES.merge(villagerId, 1, Integer::sum);

        return misses >= MISSES_BEFORE_NEWS && ANNOUNCED.add(villagerId);
    }

    /**
     * Pergunta ao mundo por cada trabalhador desta colônia, e noticia.
     *
     * <p>Chamada só para colônia <b>ativa</b>: numa dormente os chunks
     * estão descarregados, ninguém seria encontrado, e todos os
     * trabalhadores dela virariam fantasmas no log. É a mesma disciplina
     * que o {@code ColonyAbandonment} já impõe à sonda, e pelo mesmo
     * motivo — "não fui observada" não é "não existo mais".
     */
    public static void probe(net.minecraft.server.world.ServerWorld world, UUID colonyId) {
        for (var worker : VillageColonyMod.WORKERS.ofColony(colonyId)) {
            UUID villagerId = worker.villagerId();

            boolean present = world.getEntity(villagerId) != null;

            if (observe(villagerId, present)) {
                VillageColonyMod.LOGGER.warn(
                        "Worker {} of colony {} has been missing for {} cycles while the colony"
                                + " was active — it may be a phantom holding a profession slot"
                                + " and a chest. See docs/research/estado-que-sobrevive-ao-dono.md",
                        villagerId,
                        colonyId,
                        MISSES_BEFORE_NEWS);
            }
        }

        // A instrumentação do estado órfão não pode virar estado órfão: o
        // que sobra aqui de trabalhador que já não existe sai por
        // invariante, e não por evento — que é a lição que esta própria
        // auditoria registrou.
        Set<UUID> live = new java.util.LinkedHashSet<>();

        for (var worker : VillageColonyMod.WORKERS.all()) {
            live.add(worker.villagerId());
        }

        MISSES.keySet().retainAll(live);
        ANNOUNCED.retainAll(live);
    }

    /** Esquece as contagens, junto com o resto do estado em memória. */
    public static void clearAll() {
        MISSES.clear();
        ANNOUNCED.clear();
    }
}
