package com.villagecolony.fabric.work;

import com.villagecolony.core.worker.model.ProfessionType;

import java.util.Optional;

/**
 * Quem está fazendo trabalho que não é da profissão dele — ADR-010.
 *
 * <p><b>As duas linhas de relatório são montadas a partir das tarefas</b>,
 * e não da profissão de quem as pegou. É o que faz a mão emprestada
 * funcionar sem nenhum outro conserto — o mineiro que pegou tarefa de
 * madeira já é conduzido pelo {@link LumberjackWork} e já aparece na
 * linha dos lenhadores — e é também o que faria a linha mentir.
 *
 * <p>Uma colônia com dois lenhadores mostraria três, e o autor iria
 * procurar defeito na atribuição de profissão, que está certa. É o E31
 * aplicado antes de custar sessão: relatório que afirma o que não mediu
 * é pior que relatório que cala.
 *
 * <p>Sem estado e sem mundo: recebe o que o chamador já tem em mãos, e
 * por isso a bateria de unidade a alcança sem subir servidor.
 */
public final class LentHand {

    private LentHand() {
    }

    /**
     * O que se acrescenta ao nome de quem está com a mão emprestada.
     *
     * <p>Vazio — e não uma frase — quando não há nada a dizer: a linha do
     * ciclo já é longa, e o caso comum é o trabalhador na própria
     * profissão.
     *
     * @param profession a profissão de quem pegou a tarefa
     * @param doing a profissão de quem normalmente faria este trabalho
     */
    public static String mark(Optional<ProfessionType> profession, ProfessionType doing) {
        if (profession.isEmpty() || profession.get() == doing) {
            return "";
        }

        return " (" + profession.get() + " lending a hand)";
    }
}
