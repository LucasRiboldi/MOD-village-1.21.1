package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A linha diz de quem é a mão emprestada — ADR-010, 2026-09-02.
 *
 * <p><b>É o E31 outra vez, e antes de custar sessão.</b> As duas linhas
 * de relatório — a do lenhador e a do mineiro — são montadas a partir
 * das <b>tarefas</b>, e não da profissão de quem as pegou. Isso é o que
 * faz a mão emprestada funcionar sem nenhum outro conserto: o mineiro
 * que pegou tarefa de madeira já é conduzido pelo {@code LumberjackWork}
 * e já aparece na linha dos lenhadores.
 *
 * <p>E é também o que faria a linha mentir. Um mineiro contado como
 * lenhador manda o autor procurar defeito onde não há: a colônia tem
 * dois lenhadores e a linha mostraria três.
 *
 * <p>Relatório que afirma o que não mediu é pior que relatório que cala.
 */
class LentHandTest {

    @Test
    void aWorkerDoingHisOwnJobIsNotMarked() {
        assertEquals(
                "",
                LentHand.mark(Optional.of(ProfessionType.LUMBERJACK), ProfessionType.LUMBERJACK));
    }

    @Test
    void aWorkerDoingSomeoneElsesJobIsMarked() {
        String mark = LentHand.mark(Optional.of(ProfessionType.MINER), ProfessionType.LUMBERJACK);

        assertTrue(mark.contains("MINER"), "a linha não diz de onde veio a mão: " + mark);
        assertTrue(mark.contains("lending a hand"), "a linha não diz que é emprestada: " + mark);
    }

    /**
     * Sem profissão não há empréstimo a anunciar.
     *
     * <p>Bebê e nitwit passam por aqui, e a distribuição já os pula. Uma
     * marca neles seria ruído sobre alguém que não está trabalhando.
     */
    @Test
    void aWorkerWithoutAProfessionIsNotMarked() {
        assertEquals("", LentHand.mark(Optional.empty(), ProfessionType.LUMBERJACK));
    }
}
