package com.villagecolony;

import com.villagecolony.core.colony.service.ColonyService;
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

    /**
     * Registro das colônias em memória.
     *
     * <p>Campo estático por decisão explícita da ADR-006 §5: quando um
     * ponto de acesso global é necessário, ele vive aqui e não vira uma
     * camada de managers.
     *
     * <p><b>Limite do MVP:</b> existe um único registro, preenchido a
     * partir do Overworld. Servidor com múltiplos mundos ou troca de save
     * sem reiniciar o processo não são cobertos — por isso o registro é
     * esvaziado ao parar o servidor.
     */
    public static final ColonyService COLONIES = new ColonyService();

    @Override
    public void onInitialize() {
        ServerLifecycleHandler.register();

        LOGGER.info("[Village Colony] Mod initialized");
    }
}
