package com.villagecolony.fabric.work;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;

import java.util.Map;
import java.util.UUID;

/**
 * A linha que o construtor deixa no log.
 *
 * <p>Saiu de {@code BuilderWork} em 2026-08-22, no mesmo corte que
 * separou {@link BuilderApproach}. Contar o que está acontecendo é um
 * assunto próprio, e este projeto já o trata assim para o lenhador
 * ({@code LumberjackReport}) e para o mineiro ({@code MinerReport}) — o
 * construtor era o último com a linha morando dentro do trabalho.
 *
 * <p>O corte é por responsabilidade, e não por contagem — ADR-009 §6.
 */
final class BuilderReport {

    private BuilderReport() {
    }

    /**
     * Uma linha por ciclo, quando há obra.
     *
     * <p>Existe pelo mesmo motivo da linha do lenhador: sem ela, "a obra
     * não anda" e "não há obra" são indistinguíveis no log, e foi essa
     * cegueira que custou as sessões do §11.
     */
    static void report(
            Colony colony,
            ConstructionProject project,
            int builders,
            String queue,
            String waiting) {

        VillageColonyMod.LOGGER.info(
                "Colony {} builders: {} working, {} at {}, {} blocks left — {}{}",
                colony.id(),
                builders,
                project.state(),
                project.origin(),
                project.remainingCount(),
                queue,
                waiting);
    }

    /**
     * Há quanto tempo este construtor anda sem alcançar o bloco.
     *
     * <p>Vazio enquanto ele está pondo bloco — a linha já é longa.
     *
     * <p>É a segunda metade da Regra 14, e é o que faltou na sessão de
     * 2026-08-18. A obra parou na altura do telhado e o relatório dizia
     * apenas {@code BUILDING ... 1 blocks left}: do lado de fora, o
     * construtor que não alcança e o construtor que trabalha devagar
     * eram a mesma linha. O lenhador já tinha essa distinção desde a
     * Regra 9; o construtor não.
     */
    static String walking(BuilderWork.Job job) {
        if (job.stalled == 0) {
            return "";
        }

        return ", walking for " + job.stalled + " ticks without reaching the block";
    }

    /**
     * O que a obra dormindo está esperando.
     *
     * <p>Vazio quando ela não está dormindo — a linha já é longa.
     *
     * <p>Existe por causa da sessão das 21:29 de 2026-08-15, a primeira
     * a rodar {@code wakeIfSupplied}. Ele se comportou como devia e não
     * acordou nada, porque o material do próximo bloco não estava em baú
     * algum. Só que o log dizia apenas {@code WAITING_RESOURCES ... no
     * build task}, e daí não sai a pergunta seguinte: <b>esperando o
     * quê?</b>
     *
     * <p>A resposta muda tudo. Se falta tábua, a colônia fabrica e a
     * casa anda sozinha. Se falta pedregulho ou vidro, ninguém nesta
     * vila produz aquilo — e a obra não está lenta, está impossível.
     * Dois estados idênticos no log, e correções que não se parecem.
     */
    static String waitingFor(ConstructionProject project) {
        if (project.state() != ConstructionState.WAITING_RESOURCES) {
            return "";
        }

        return project.nextBlock()
                .map(block -> ", waiting for " + block.block())
                .orElse(", waiting with nothing left to place");
    }
}
