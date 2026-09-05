package com.villagecolony.core.worker.model;

/**
 * Ferramenta inicial de uma profissão.
 *
 * <p>Tipo próprio do Core: nenhuma classe daqui conhece {@code Item} ou
 * {@code Items.WOODEN_AXE}. A conversão para o item Vanilla acontece na
 * fronteira, em {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * <p>Aqui está a ferramenta com que a profissão <b>começa</b>. A
 * evolução (madeira → pedra → ferro → diamante) deixou de ser fora do
 * MVP em 2026-09-04, e não mora neste enum: quem troca é
 * {@code fabric.integration.ToolUpgrade}, que mede pela velocidade que o
 * jogo dá e por isso não precisa de escada escrita. Ver
 * Profession-System.md §"Evolução das Ferramentas".
 */
public enum ToolType {

    /** A profissão trabalha de mãos vazias. */
    NONE,

    WOODEN_AXE,

    /**
     * Do mineiro — decisão do autor, 2026-09-04: <i>"todos trabalhadores
     * começam com a ferramenta nível 1 de madeira"</i>.
     *
     * <p>Foi dele até 08-27, virou diamante, e voltou. Ver
     * {@code ProfessionRegistry}, que é onde a escolha mora, e
     * {@code ToolUpgrade}, que é a saída: a descida de vinte blocos
     * continua lenta com madeira, só que agora ela é o primeiro degrau e
     * não o teto — o trabalhador troca pela melhor ferramenta que houver
     * no baú dele.
     */
    WOODEN_PICKAXE,

    /**
     * <b>Não é de profissão nenhuma desde 2026-09-04</b>, e fica.
     *
     * <p>Era do mineiro entre 08-27 e 09-04. O enum é a memória do que a
     * colônia já pôs em mão de aldeão, e não a lista do que ela entrega
     * hoje: é {@code WorkerEquipment.isProfessionTool} que a percorre
     * para saber se uma ferramenta é dela ou do jogador.
     *
     * <p>Tirá-la daqui faria a picareta de diamante que a colônia
     * entregou virar <b>item do jogador</b>, que a Regra 3 protege — e
     * ela ficaria naquela mão para sempre. É exatamente o defeito que
     * 2026-09-02 consertou com o pastor, e o javadoc daquele método
     * conta a história inteira.
     */
    DIAMOND_PICKAXE,

    /** Do pastor. Tesoura não tem grau, então é ela mesma. */
    SHEARS,

    WOODEN_HOE
}
