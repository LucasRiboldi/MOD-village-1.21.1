package com.villagecolony;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada do mod.
 *
 * <p>Responsabilidade: inicializar o mod e registrar eventos.
 *
 * <p>Esta classe nunca cria sistemas de colônia, controla aldeões ou
 * executa lógica de simulação. Ver docs/technical/Initial-Setup-Checklist.md §7.
 */
public class VillageColonyMod implements ModInitializer {

    public static final String MOD_ID = "villagecolony";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[Village Colony] Mod initialized");
    }
}
