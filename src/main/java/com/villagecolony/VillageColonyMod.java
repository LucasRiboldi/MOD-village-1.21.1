package com.villagecolony;

import com.villagecolony.core.colony.service.ColonyService;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.construction.service.BuildingRegistry;
import com.villagecolony.core.construction.service.ConstructionService;
import com.villagecolony.core.task.service.TaskService;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.event.ServerLifecycleHandler;
import com.villagecolony.fabric.event.VillageDetectionHandler;
import com.villagecolony.fabric.event.VillagerLifecycleHandler;
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

    /**
     * Registro dos trabalhadores em memória.
     *
     * <p>Mesma justificativa de {@link #COLONIES}: ADR-006 §5 permite o
     * campo estático em vez de uma camada de managers.
     *
     * <p>Persistido junto com as colônias desde a TASK-012b: a profissão
     * atribuída é decisão do mod e não existe no mundo Vanilla, então
     * redescobrir os aldeões deixou de bastar.
     */
    public static final WorkerService WORKERS = new WorkerService();

    /**
     * Registro dos baús dos trabalhadores em memória.
     *
     * <p>Mesma justificativa de {@link #COLONIES}: ADR-006 §5 permite o
     * campo estático em vez de uma camada de managers.
     *
     * <p>Não é persistido. Ao contrário da profissão, a posição do baú
     * existe no mundo e é redescoberta pela varredura a cada sessão —
     * salvar seria manter uma segunda verdade que envelheceria assim que
     * o jogador quebrasse o baú.
     */
    public static final StorageRegistry STORAGES = new StorageRegistry();

    /**
     * Tarefas abertas da partida.
     *
     * <p>Mesma justificativa de {@link #COLONIES}: ADR-006 §5 permite o
     * campo estático em vez de uma camada de managers.
     *
     * <p>Vazio hoje, e não por engano: nada cria tarefas ainda. A geração
     * de demanda é o passo 4 do Simulation-Loop.md, que depende do loop
     * de simulação — não escrito. Ver §10 do Project-State.
     *
     * <p>Existe assim mesmo porque o caminho da morte do trabalhador já
     * passa por aqui, e ligá-lo agora custa três linhas. Deixar para
     * depois é como {@code WorkerService.remove} ficou sem chamador até
     * alguém notar que colônia nenhuma reabria vaga.
     *
     * <p>Não é persistido. Uma tarefa é intenção do momento; retomá-la
     * numa sessão em que o mundo mudou faria o aldeão ir cortar uma
     * árvore que o jogador já derrubou.
     */
    public static final TaskService TASKS = new TaskService();

    /**
     * As obras em andamento — Fase 10.
     *
     * <p>Uma por colônia de cada vez. O canteiro sai daqui quando a casa
     * fica pronta; o que fica é {@link #BUILDINGS}.
     *
     * <p>Não é persistido. Uma obra interrompida por fechar o mundo
     * volta a ser planejada na sessão seguinte, e a casa meio construída
     * do mundo é lote ocupado — o scanner recusa terreno com bloco em
     * cima. Persistir obra é trabalho da Fase 11 e não do MVP.
     */
    public static final ConstructionService CONSTRUCTIONS = new ConstructionService();

    /**
     * O que a colônia levantou — Fase 11.
     *
     * <p>Sobrevive à obra e responde "este bloco é da colônia?", que é o
     * que a proteção, a fusão de vilas e a escolha do próximo lote
     * precisam saber.
     *
     * <p>Também não é persistido, e isso <b>custa</b>: ao reabrir o
     * mundo, a colônia esquece que a casa é dela. A casa continua de pé —
     * o mundo é que guarda blocos —, mas a proteção some. Está registrado
     * em Project-State §9.
     */
    public static final BuildingRegistry BUILDINGS = new BuildingRegistry();

    @Override
    public void onInitialize() {
        ServerLifecycleHandler.register();
        VillageDetectionHandler.register();
        VillagerLifecycleHandler.register();

        LOGGER.info("[Village Colony] Mod initialized");
    }
}
