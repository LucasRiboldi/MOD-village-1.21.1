package com.villagecolony.core.coordination;

import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ResourceType;

/**
 * Quantas mãos a colônia tem para cada material que lhe falta.
 *
 * <p><b>Existe para quebrar um silêncio</b> — 2026-09-09. Quando nenhuma
 * profissão registrada tem a capacidade que o material exige,
 * {@code ColonyCycle.requestMissing} pula o pedido e segue: abrir tarefa
 * que ninguém pode pegar a deixaria na fila para sempre, e a decisão de
 * pular está certa. O que estava errado era <b>pular calado</b>.
 *
 * <p>O sintoma em jogo é a linha {@code assigned 0 tasks (0 open)} sem
 * causa aparente, que é a mesma da roça que travou a vila em 09-09 — e
 * naquele caso custou uma hora de sessão e a queixa do autor de <i>não
 * ver trabalhador nenhum trabalhando</i>. Uma colônia sem fundidor
 * simplesmente nunca faz vidro, e nada no log dizia por quê.
 *
 * <p><b>Por que é uma interface e não uma chamada direta ao log.</b> O
 * {@code IdleLog} vive em {@code fabric.work}, e a ADR-006 §6 proíbe
 * {@code core} de importar {@code fabric} — regra com teste, o
 * {@code DependencyRuleTest}. O caminho é o mesmo que
 * {@code ColonyCycle.run} já usa para {@code hasStorage}: a camada de
 * cima passa o que sabe fazer, e a coordenação só a chama.
 *
 * <p><b>Recebe o número, e não só a ausência</b>, e isso não é
 * generalidade gratuita: o {@code IdleLog} registra <b>transições</b>, e
 * quem para de falar precisa ser mandado esquecer quando o trabalho
 * volta. Sem o caso {@code hands > 0}, uma colônia que perde o fundidor,
 * contrata outro e o perde de novo ficaria <b>muda na segunda vez</b> —
 * o motivo guardado ainda seria {@code NO_WORKER}. É exatamente o que o
 * {@code IdleLog.clear} existe para impedir.
 */
@FunctionalInterface
public interface ProductionHands {

    /** Não faz nada. O padrão de quem chama sem se interessar. */
    ProductionHands IGNORED = (resource, type, hands) -> {
    };

    /**
     * Diz quantos trabalhadores capazes a colônia tem para este material.
     *
     * @param resource o material que falta
     * @param type a tarefa que o produziria
     * @param hands quantos trabalhadores da colônia sabem fazê-la —
     *     <b>zero</b> quer dizer que o pedido não vai ser aberto
     */
    void counted(ResourceType resource, TaskType type, int hands);
}
