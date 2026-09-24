package com.villagecolony.fabric.work;

import com.villagecolony.fabric.work.MinerWork.Job;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.construction.model.ColonyEdits;
import com.villagecolony.core.construction.model.Mine;
import com.villagecolony.core.construction.model.MineArm;
import com.villagecolony.core.coordination.IdleReason;
import com.villagecolony.core.coordination.WorkAssignment;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.task.model.Task;
import com.villagecolony.core.task.model.TaskState;
import com.villagecolony.core.task.model.TaskType;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceGroup;
import com.villagecolony.core.type.ResourceType;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import com.villagecolony.fabric.brain.WorkHours;
import com.villagecolony.fabric.brain.WorkTargets;
import com.villagecolony.fabric.integration.BlockBreakTime;
import com.villagecolony.fabric.integration.ChestDepositor;
import com.villagecolony.fabric.integration.MineFlooding;
import com.villagecolony.fabric.integration.OreVein;
import com.villagecolony.fabric.integration.MineMouth;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * O que se lê de fora sobre o mineiro, para o log e para os testes: guarda, imobilidade, deriva, alvo e colheita — separado de
 * {@link MinerWork} em 2026-09-24, quando ele passou de 500 linhas. Os
 * comentários vieram junto sem mudança.
 */
public final class MinerProbe {

    private MinerProbe() {
    }

    /** Quantos mineiros estão com trabalho aberto agora. */
    public static int activeJobs() {
        return MinerWork.JOBS.size();
    }

    /**
     * Quantos tiques este mineiro já andou sem chegar na pedra.
     *
     * <p>Não é estado novo — é o contador do guarda de travamento, lido
     * de fora, como o {@code BuildSiteScanner.sweepPausedAt}. Existe
     * porque a pergunta que ele responde não tem outro observável: o
     * guarda só fala quando estoura, e o defeito era ele <b>contar</b>
     * quando não devia.
     */
    public static int stallOf(UUID workerId) {
        Job job = MinerWork.JOBS.get(workerId);

        return job == null ? 0 : job.stalled;
    }

    /**
     * Há quantos tiques de expediente este mineiro não sai do bloco.
     *
     * <p>Pelo mesmo motivo do {@link #stallOf}: o guarda só fala quando
     * estoura, e a pergunta que ele responde — <i>ele está andando?</i> —
     * não tem outro observável de fora.
     */
    public static int stillnessOf(UUID workerId) {
        Job job = MinerWork.JOBS.get(workerId);

        return job == null ? 0 : job.stall.ticks();
    }

    /**
     * Há quantos tiques de expediente este mineiro não encurta a distância.
     *
     * <p>E qual pedra ele mira — {@link #targetOf}. As duas juntas são o
     * que a bateria do E44 precisa saber: a de cima prova que o prazo
     * venceu, e a de baixo diz de qual posição estamos falando quando se
     * pergunta se o segundo mineiro recebeu a mesma.
     */
    public static int adriftOf(UUID workerId) {
        Job job = MinerWork.JOBS.get(workerId);

        return job == null ? 0 : job.lease.ticks();
    }

    /**
     * A pedra que este mineiro mira agora, se ele mira alguma.
     *
     * <p>Pelo mesmo motivo do {@link #stallOf}: de fora não há outro
     * observável: o alvo nasce e morre dentro do {@code Job}, e a única
     * pista era o texto do relatório — que é para ler, e não para
     * afirmar contra.
     */
    public static Optional<BlockPos> targetOf(UUID workerId) {
        Job job = MinerWork.JOBS.get(workerId);

        return Optional.ofNullable(job == null ? null : job.target);
    }

    /** Quanta pedra este mineiro já trouxe nesta tarefa. */
    public static int collectedBy(UUID workerId) {
        Job job = MinerWork.JOBS.get(workerId);

        return job == null ? 0 : job.collected;
    }
}
