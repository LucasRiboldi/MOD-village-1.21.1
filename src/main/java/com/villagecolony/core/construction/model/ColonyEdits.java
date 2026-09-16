package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * As mudanças de mundo que a própria colônia fez — 2026-09-16.
 *
 * <p><b>A enxurrada que isto corrige.</b> O log de 02:58 tinha
 * <b>19.193</b> linhas idênticas, quase metade de um arquivo de 7 MB:
 * <i>"Miner d492ef6b hit stone with nowhere to stand - the branch ends
 * here"</i>. Dez por segundo, um único mineiro, por trinta e dois minutos.
 *
 * <p><b>O laço.</b> O {@code PlayerWorldChangeHandler} reabre o ramal da
 * mina quando o mundo muda perto do túnel, e existe por um bom motivo: o
 * jogador abre caminho com a picareta dele, e a mina tem de retomar por
 * ali. Só que ele reagia a <b>qualquer</b> mudança, sem perguntar quem a
 * fez — e quem mais mexe no túnel é o próprio mineiro.
 *
 * <p>A cada pedra que a picareta tirava, o handler reabria o ramal com a
 * contagem de recusas <b>zerada</b>; o ramal caminhava até a mesma pedra
 * sem lugar de ficar de pé e fechava de novo. A mina <b>nunca desceu</b>
 * naquela sessão — {@code went one level deeper} não aparece uma única vez
 * —, que é a prova de que o ramal girava em vez de progredir.
 *
 * <p><b>A marca vale uma leitura.</b> Se ficasse, o jogador que depois
 * mexesse naquela mesma posição seria ignorado, e a mina nunca retomaria
 * por ali — trocaríamos uma enxurrada por um silêncio, que é pior.
 */
public final class ColonyEdits {

    /**
     * Quantas mudanças ficam esperando leitura.
     *
     * <p>Marca que ninguém leu é lixo em memória, e este projeto já pagou
     * por registro que só cresce. O número é folgado para o que se espera
     * — a colônia muda poucos blocos por tique —, e o descarte é do mais
     * antigo, que é o que tem menos chance de ainda ser perguntado.
     */
    public static final int MAX_PENDING = 512;

    /** Ordem de chegada, para o descarte tirar o mais antigo. */
    private static final Set<ColonyPos> OURS = new LinkedHashSet<>();

    private ColonyEdits() {
    }

    /**
     * A colônia acabou de mudar esta posição.
     *
     * <p>Chamado de onde a picareta, o machado ou o construtor mexem no
     * mundo, imediatamente antes ou depois da mudança.
     */
    public static void remember(ColonyPos at) {
        if (OURS.size() >= MAX_PENDING) {
            // O mais antigo sai. Uma marca que sobreviveu a quinhentas
            // outras já perdeu a chance de ser perguntada.
            java.util.Iterator<ColonyPos> oldest = OURS.iterator();

            oldest.next();
            oldest.remove();
        }

        OURS.add(at);
    }

    /**
     * Se esta mudança foi da colônia — e consome a marca.
     *
     * @return falso para a mudança do jogador, que é a que deve reabrir o
     *     ramal
     */
    public static boolean wasOurs(ColonyPos at) {
        return OURS.remove(at);
    }

    /** Quantas marcas esperam leitura. Para o teste. */
    public static int pending() {
        return OURS.size();
    }

    /** Esquece tudo. Ao parar o servidor, e para o teste. */
    public static void clearAll() {
        OURS.clear();
    }
}
