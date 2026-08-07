package com.villagecolony;

import com.villagecolony.fabric.event.ServerLifecycleHandler;
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
 *
 * <p>O registro de cada evento vive em {@code fabric.event}. Aqui existe
 * apenas a chamada, para que adicionar um evento não signifique alterar
 * esta classe.
 */
public class VillageColonyMod implements ModInitializer {

    public static final String MOD_ID = "villagecolony";

    /**
     * Logger compartilhado do mod.
     *
     * <p>Nomeado com o mod id para que toda linha do log seja atribuída a
     * {@code (villagecolony)} e possa ser filtrada.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleHandler.register();

        LOGGER.info("[Village Colony] Mod initialized");
    }
}
