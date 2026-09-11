package com.villagecolony.gametest;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.worker.model.ProfessionType;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.fabric.integration.WorkerEquipment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/**
 * Os dois jeitos de pôr um trabalhador na arena, e a diferença entre
 * eles — P1.12, 2026-09-11.
 *
 * <p><b>A diferença está no nome de propósito.</b> Havia um jeito só,
 * repetido em sessenta e quatro lugares: {@code register} seguido de
 * {@code assign}. Ele cria um aldeão <b>de mãos vazias</b>, e o nome
 * {@code assign} não conta isso — parece que atribuir a profissão
 * entrega o ofício inteiro, e entrega só metade.
 *
 * <p><b>Por que a metade que falta importa.</b> Desde 2026-09-04 o
 * {@code BlockBreakTime} pergunta <b>à mão do aldeão</b> quanto tempo um
 * bloco leva para cair; antes ele respondia por uma constante. Teste que
 * mede quebra sem equipar mede a <b>mão nua</b>, que no jogo real nunca
 * acontece — a colônia equipa todo trabalhador no mesmo ciclo em que o
 * contrata.
 *
 * <p>Esses testes não estão errados por isso: eles passam. O problema é
 * <b>por que</b> passam — por folga no {@code tickLimit}, e não por
 * medirem o que prometem. No dia em que a folga acabar, eles quebram
 * todos juntos e ninguém vai saber por quê.
 *
 * <p><b>E a produção continua entregando mão vazia</b>, o que não é
 * defeito: o {@code VillageDetectionHandler} chama
 * {@code ProfessionAssigner.assignMissing} e só depois
 * {@code WorkerEquipment.equip}, no mesmo ciclo. Uma asserção defensiva
 * dentro do {@code Worker.assign} recusando mão vazia — como a primeira
 * versão do P1.12 pedia — <b>quebraria a contratação inteira</b>, porque
 * ali a mão está vazia por desenho. O lugar de exigir ferramenta é o
 * teste que depende dela, e é o que esta classe dá.
 */
final class TestWorkers {

    private TestWorkers() {
    }

    /**
     * Um trabalhador com a profissão e <b>sem ferramenta nenhuma</b>.
     *
     * <p>É o que os sessenta e quatro lugares faziam, agora com o nome
     * dizendo o que é. Serve para todo teste cujo assunto não é tempo de
     * quebra: reserva de baú, escolha de alvo, contagem de tarefa,
     * persistência, nome, ociosidade.
     */
    static Worker createWorker(
            TestContext context, UUID colonyId, ProfessionType profession, BlockPos at) {

        VillagerEntity villager = context.spawnEntity(EntityType.VILLAGER, at);

        villager.setBreedingAge(0);

        Worker worker = VillageColonyMod.WORKERS.register(villager.getUuid(), colonyId);

        worker.assign(profession);

        return worker;
    }

    /**
     * O mesmo, e <b>com a ferramenta na mão</b> — pela mesma porta que a
     * colônia usa.
     *
     * <p>Use este sempre que o teste medir <b>tempo</b>: quantos tiques
     * um bloco leva, quantos blocos saem dentro do {@code tickLimit},
     * se a tarefa fecha antes do limite. Em todos esses o
     * {@code BlockBreakTime} pergunta à mão, e a mão nua responde outra
     * coisa.
     *
     * <p>Equipa pelo {@link WorkerEquipment#equip}, e não pondo o item à
     * força: assim o teste exercita a mesma escolha de ferramenta que a
     * colônia faz em jogo, inclusive a troca pela melhor do baú.
     */
    static Worker createEquippedWorker(
            TestContext context, UUID colonyId, ProfessionType profession, BlockPos at) {

        Worker worker = createWorker(context, colonyId, profession, at);

        WorkerEquipment.equip(context.getWorld(), List.of(worker));

        return worker;
    }
}
