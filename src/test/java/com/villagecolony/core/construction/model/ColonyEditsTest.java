package com.villagecolony.core.construction.model;

import com.villagecolony.core.type.ColonyPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O que a própria colônia mudou no mundo — 2026-09-16.
 *
 * <p><b>A enxurrada que isto corrige.</b> O log de 02:58 tinha
 * <b>19.193</b> linhas idênticas — quase metade do arquivo, 7 MB:
 *
 * <pre>
 * Miner d492ef6b hit stone with nowhere to stand - the branch ends here
 * </pre>
 *
 * <p>Dez por segundo, um único mineiro, por trinta e dois minutos.
 *
 * <p><b>O laço.</b> O {@code PlayerWorldChangeHandler} reabre o ramal da
 * mina quando o mundo muda perto do túnel — existe para o jogador poder
 * abrir caminho e a mina retomar. Só que ele reage a <b>qualquer</b>
 * mudança, sem perguntar quem a fez. O próprio mineiro quebra pedra no
 * túnel; o handler via a mudança, reabria o ramal com a contagem de
 * recusas zerada; o ramal batia na mesma pedra sem lugar de ficar de pé e
 * fechava de novo. Para sempre.
 *
 * <p>A mina <b>nunca desceu</b> naquela sessão — {@code went one level
 * deeper} não aparece uma vez —, que é a prova de que o ramal não estava
 * progredindo: estava girando.
 */
class ColonyEditsTest {

    private static final ColonyPos SOMEWHERE = new ColonyPos(10, 40, -20);

    @BeforeEach
    void clear() {
        ColonyEdits.clearAll();
    }

    /** A mudança que ninguém anunciou é do jogador, e reabre o ramal. */
    @Test
    void anUnannouncedChangeIsThePlayers() {
        assertFalse(
                ColonyEdits.wasOurs(SOMEWHERE),
                "uma mudança que a colônia não fez passou por dela — o jogador abre"
                        + " caminho e a mina tem de retomar");
    }

    /** A que a colônia anunciou é dela, e não reabre nada. */
    @Test
    void anAnnouncedChangeIsOurs() {
        ColonyEdits.remember(SOMEWHERE);

        assertTrue(
                ColonyEdits.wasOurs(SOMEWHERE),
                "a picareta do mineiro passou por edição do jogador — é o laço de"
                        + " 19.193 linhas");
    }

    /**
     * A marca vale uma vez só.
     *
     * <p>Se ficasse, o jogador que depois mexesse naquela mesma posição
     * seria ignorado — e a mina nunca retomaria por ali. A pergunta é
     * feita uma vez por mudança, então consumir é o certo.
     */
    @Test
    void theMarkIsSpentWhenRead() {
        ColonyEdits.remember(SOMEWHERE);

        assertTrue(ColonyEdits.wasOurs(SOMEWHERE));

        assertFalse(
                ColonyEdits.wasOurs(SOMEWHERE),
                "a marca sobreviveu à leitura, e o jogador ficaria ignorado ali para"
                        + " sempre");
    }

    /** Uma posição não fala pela vizinha. */
    @Test
    void eachPositionAnswersForItself() {
        ColonyEdits.remember(SOMEWHERE);

        assertFalse(ColonyEdits.wasOurs(new ColonyPos(11, 40, -20)));
    }

    /**
     * O registro não cresce sem teto.
     *
     * <p>Marca que ninguém leu é lixo em memória — e este mod já pagou
     * por registro que só cresce.
     */
    @Test
    void theRegisterDoesNotGrowWithoutBound() {
        for (int i = 0; i < ColonyEdits.MAX_PENDING * 3; i++) {
            ColonyEdits.remember(new ColonyPos(i, 0, 0));
        }

        assertTrue(
                ColonyEdits.pending() <= ColonyEdits.MAX_PENDING,
                "o registro passou do teto: " + ColonyEdits.pending());
    }
}
