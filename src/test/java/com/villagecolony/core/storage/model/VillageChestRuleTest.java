package com.villagecolony.core.storage.model;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quais baús da vila a colônia pode usar — 2026-09-16.
 *
 * <p><b>Pedido do autor:</b> <i>"permitir que o recurso que falta possa
 * ser recolhido de qualquer baú que esteja na vila automaticamente"</i>,
 * com a ressalva que ele escolheu: <b>menos os marcados</b>.
 *
 * <p>Até aqui a colônia só enxergava os baús que ela mesma reivindicou
 * para um trabalhador, mais o da boca da mina. Um baú que o jogador
 * colocasse na vila, cheio do material que a obra espera, era invisível —
 * e a obra ficava em {@code WAITING_RESOURCES} do lado dele.
 *
 * <p><b>O baú privado é o que o jogador NOMEIA.</b> Nomear um baú exige
 * bigorna e é um ato deliberado; ninguém nomeia um baú por acidente. E é
 * simétrico ao que o mod já faz com aldeão: nome dado à mão é intocável —
 * ver {@code WorkerNameplate} e a Regra 3.
 *
 * <p>A decisão mora no Core e se afirma sem mundo: ler o nome de um baú
 * precisa de servidor, decidir o que o nome significa não.
 */
class VillageChestRuleTest {

    /** Baú comum, sem nome: é da vila, e a colônia usa. */
    @Test
    void anUnnamedChestBelongsToTheVillage() {
        assertTrue(
                VillageChestRule.mayTake(Optional.empty()),
                "um baú sem nome ficou fora da vila, e é o caso comum");
    }

    /**
     * Baú nomeado é do jogador, e a colônia não toca.
     *
     * <p>É a saída que ele pediu para guardar as próprias coisas.
     */
    @Test
    void aNamedChestIsPrivate() {
        assertFalse(
                VillageChestRule.mayTake(Optional.of("Minhas coisas")),
                "a colônia levou de um baú que o jogador nomeou");
    }

    /**
     * Qualquer nome serve, e não uma palavra mágica.
     *
     * <p>Exigir a palavra certa faria o jogador perder material por erro
     * de digitação, e o erro só apareceria depois de a colônia já ter
     * levado.
     */
    @Test
    void anyNameMakesItPrivate() {
        assertFalse(VillageChestRule.mayTake(Optional.of("privado")));
        assertFalse(VillageChestRule.mayTake(Optional.of("x")));
        assertFalse(VillageChestRule.mayTake(Optional.of("Ferramentas")));
    }

    /**
     * O nome padrão do jogo não conta como nome.
     *
     * <p>Um baú comum responde {@code "Chest"} — ou {@code "Baú"} — quando
     * perguntado, e tratá-lo como nomeado tornaria a regra inútil: nenhum
     * baú da vila seria usado.
     */
    @Test
    void theVanillaDefaultNameIsNotAName() {
        assertTrue(VillageChestRule.mayTake(Optional.of("Chest")));
        assertTrue(VillageChestRule.mayTake(Optional.of("Baú")));
        assertTrue(VillageChestRule.mayTake(Optional.of("Large Chest")));
    }

    /** Nome só de espaços é nome vazio, e não tranca o baú. */
    @Test
    void blankIsNotAName() {
        assertTrue(VillageChestRule.mayTake(Optional.of("   ")));
    }
}
