package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.storage.model.WorkerStorage;
import com.villagecolony.core.colony.model.Colony;
import com.villagecolony.core.colony.service.VillageDetector;
import com.villagecolony.core.storage.service.StorageRegistry;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.worker.model.Worker;
import com.villagecolony.core.worker.service.ProfessionAssigner;
import com.villagecolony.core.worker.service.WorkerService;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Registra como trabalhadores os aldeões vivos de uma colônia.
 *
 * <p>Detecta aldeões dentro de uma colônia — não confundir com
 * {@link VillageScanner}, que detecta vilas. A ADR-003 §7 exige que os
 * dois nomes não se misturem.
 *
 * <p>Nunca busca aldeões no mundo inteiro: a caixa parte do centro da
 * colônia, conforme Performance-Rules.md §5.
 */
public final class VillagerScanner {

    private VillagerScanner() {
    }

    /**
     * Varre os arredores do centro da colônia e registra quem encontrar.
     *
     * <p>Usa o mesmo raio da detecção de vila: é o alcance que a ADR-003
     * já define como "os arredores desta vila", e inventar um segundo
     * número aqui só criaria duas noções de perto.
     *
     * <p>Aldeões bebês são registrados. Eles crescem, e a colônia perde
     * menos ao já conhecê-los do que ao redescobri-los depois.
     *
     * <p>O baú de cada aldeão é procurado na mesma passagem: são os
     * mesmos aldeões, e uma segunda consulta de entidades por ciclo só
     * para isso seria desperdício. Quem já tem baú não é revisitado.
     *
     * @return quantos trabalhadores e quantos baús foram registrados
     *     agora — zero e zero quando nada mudou
     */
    public static ScanResult scan(
            ServerWorld world,
            Colony colony,
            WorkerService workers,
            StorageRegistry storages) {

        return scan(world, colony, colony.center(), workers, storages);
    }

    /**
     * O mesmo, a partir de onde as camas foram vistas — 2026-08-22.
     *
     * <p><b>Por que a origem passou a ser um parâmetro.</b> Até aqui a
     * busca partia sempre do centro da colônia, e isso valia enquanto o
     * centro perseguia a última observação. A Emenda 4 da ADR-003 parou
     * o centro: ele só anda numa leitura da sonda.
     *
     * <p>O efeito era este: uma colônia que adotasse um aglomerado a
     * dezenas de blocos do próprio centro ficava dono dele <b>e não
     * enxergava um aldeão sequer ali</b> — a caixa de busca continuava
     * centrada no lugar antigo. A vila crescia para um lado e a colônia
     * procurava gente no outro.
     *
     * <p>Quem adota passa a dizer <b>de onde</b> veio a observação. O
     * centro continua parado, que é a decisão; o registro segue as camas,
     * que é onde a gente está.
     */
    public static ScanResult scan(
            ServerWorld world,
            Colony colony,
            ColonyPos around,
            WorkerService workers,
            StorageRegistry storages) {
        BlockPos center = MinecraftTypeAdapter.toBlockPos(around);

        Box area = Box.of(
                center.toCenterPos(),
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0,
                VillageDetector.SEARCH_RADIUS * 2.0);

        int registered = 0;
        int storagesFound = 0;

        Set<UUID> employable = new HashSet<>();
        Set<UUID> equippable = new HashSet<>();

        // Os baús distintos que os candidatos conseguiriam, e não os
        // candidatos. Dois aldeões do mesmo cômodo enxergam o MESMO baú,
        // e contá-los como dois é o E11 do §17 — ver
        // ChestScanner.freeChestFor.
        Set<ColonyPos> freeChests = new HashSet<>();

        // Perguntar quem consegue baú custa uma varredura por candidato,
        // e só serve quando a resposta muda alguma coisa: quando há vaga
        // aberta, ou quando alguém está ocupando uma sem baú — e nesse
        // caso a resposta decide se ele perde a vaga para quem consegue.
        boolean hiring = ProfessionAssigner.vacancy(workers.ofColony(colony.id())).isPresent()
                || hasEmployedWithoutStorage(workers, colony.id(), storages);

        for (VillagerEntity villager
                : world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive)) {

            if (!workers.isRegistered(villager.getUuid())) {
                workers.register(villager.getUuid(), colony.id());
                registered++;
            }

            if (canWork(villager)) {
                employable.add(villager.getUuid());

                if (hiring && !isEmployed(workers, villager.getUuid())) {
                    ChestScanner.freeChestFor(world, villager, storages)
                            .ifPresent(chest -> {
                                equippable.add(villager.getUuid());
                                freeChests.add(chest);
                            });
                }
            }

            // Baú é de quem trabalha.
            //
            // Antes de 2026-08-12 todo aldeão reivindicava um, e a vila
            // do autor mostrou o que isso custa: treze baús presos, dos
            // quais só dois pertenciam a alguém com função. O fazendeiro
            // e o construtor não conseguiam reivindicar nenhum, porque os
            // vizinhos desempregados tinham chegado primeiro.
            //
            // Fora do if de registro: o aldeão pode já ser conhecido e
            // ainda não ter baú, seja porque o jogador o construiu
            // depois, seja porque o chunk dele não estava carregado no
            // ciclo anterior, seja porque ele acabou de receber função.
            if (isEmployed(workers, villager.getUuid())) {
                Optional<WorkerStorage> claimed =
                        ChestScanner.scan(world, villager, storages);

                if (claimed.isPresent()) {
                    storagesFound++;

                    announce(workers, villager.getUuid(), claimed.get());
                }
            }
        }

        return new ScanResult(
                registered,
                storagesFound,
                Set.copyOf(employable),
                Set.copyOf(equippable),
                Set.copyOf(freeChests));
    }

    /**
     * Diz qual profissão ficou com qual baú.
     *
     * <p>A linha antiga contava — "Registered 5 storages" — e não dizia
     * de quem nem onde. Em 2026-08-12 isso deixou duas perguntas sem
     * resposta na mesma sessão: por que uma vila com quatro vagas
     * registrou cinco baús, e por que três deles não ganharam marca.
     * Nenhuma das duas dá para responder a partir de um número.
     *
     * <p>É o mesmo remédio que fechou o E2: o número sozinho não diz
     * nada, o número com o lugar diz tudo. E aqui ele custa uma linha por
     * baú reivindicado, que acontece uma vez na vida de cada baú.
     *
     * <p>A colônia sai do <b>trabalhador</b>, e não de quem varreu. Até
     * 2026-08-12 saía de quem varreu, e isso fez a linha atribuir dono
     * errado: com dois centros a 61 blocos e raio de varredura 64, um
     * aldeão da colônia vizinha entra nesta caixa, e quem varreu
     * primeiro assinava embaixo. A linha chegou a mostrar dois
     * fabricantes na mesma colônia, sugerindo furo na regra de uma vaga
     * por profissão — a regra estava certa, a linha é que dizia o nome
     * errado.
     */
    private static void announce(
            WorkerService workers, UUID villagerId, WorkerStorage storage) {

        Optional<Worker> worker = workers.find(villagerId);

        VillageColonyMod.LOGGER.info(
                "Colony {} — {} {} claimed the chest at {}",
                worker.map(Worker::colonyId).map(Object::toString).orElse("unknown"),
                worker.flatMap(Worker::profession)
                        .map(Object::toString)
                        .orElse("worker"),
                villagerId.toString().substring(0, 8),
                storage.chestPosition());
    }

    /**
     * Se este aldeão tem função na colônia.
     *
     * <p>A função vem de um passo posterior a esta varredura, então quem
     * acabou de ser registrado só reivindica baú no ciclo seguinte. Um
     * ciclo de atraso é barato; deixar os quarenta desempregados
     * reivindicarem primeiro não era.
     */
    private static boolean isEmployed(WorkerService workers, UUID villagerId) {
        return workers.find(villagerId).filter(Worker::hasProfession).isPresent();
    }

    /**
     * Se este aldeão pode receber uma função de colônia.
     *
     * <p>Bebê não trabalha. Ele é registrado — a colônia não perde nada
     * ao já conhecê-lo — mas dar-lhe um machado seria absurdo em jogo, e
     * ele ainda ocuparia a vaga de lenhador que um adulto deveria ter.
     * Profession-System.md §"Nascimento de Novos Aldeões" é explícito:
     * a função vem quando surge um aldeão <em>adulto</em>. Ao crescer,
     * ele passa a ser elegível sozinho, no ciclo seguinte.
     *
     * <p>Nitwit também não. O Vanilla nunca lhe dá emprego, e o jogador
     * que reconhece o casaco verde espera que ele continue inútil. Uma
     * colônia que o pusesse a construir contrariaria a expectativa que o
     * próprio jogo criou — e o PROJECT_CONSTITUTION §4 manda respeitar
     * o comportamento Vanilla do aldeão.
     */
    /** Alguém desta colônia tem função e não tem onde guardar. */
    private static boolean hasEmployedWithoutStorage(
            WorkerService workers, java.util.UUID colonyId, StorageRegistry storages) {

        for (Worker worker : workers.ofColony(colonyId)) {
            if (worker.hasProfession() && !storages.hasStorage(worker.villagerId())) {
                return true;
            }
        }

        return false;
    }

    private static boolean canWork(VillagerEntity villager) {
        return !villager.isBaby()
                && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT;
    }

    /**
     * O que uma varredura viu.
     *
     * <p>{@code employable} são os aldeões prontos para receber função —
     * não os que a têm. Vazio é comum: uma vila só de bebês existe.
     *
     * <p>{@code equippable} são, dentre eles, os que conseguiriam um baú.
     * É subconjunto de {@code employable} e só é preenchido quando a
     * colônia tem vaga aberta — perguntar custa uma varredura de baús por
     * candidato, e depois dos primeiros ciclos não há vaga nenhuma.
     */
    /**
     * @param freeChests os baús <b>distintos</b> que os {@code equippable}
     *     conseguiriam. Menor que {@code equippable} sempre que dois
     *     candidatos olharem para o mesmo baú, que é o caso comum de dois
     *     aldeões do mesmo cômodo — e é o número que decide quantas
     *     dispensas cabem. Ver o E11 do §17
     */
    public record ScanResult(
            int registeredWorkers,
            int registeredStorages,
            Set<UUID> employable,
            Set<UUID> equippable,
            Set<ColonyPos> freeChests) {

        public boolean changedNothing() {
            return registeredWorkers == 0 && registeredStorages == 0;
        }

        /**
         * Quantas trocas de vaga cabem nesta passagem — a Regra 11.
         *
         * <p>Uma troca precisa de duas coisas: <b>um baú livre</b>, que é
         * a decisão do E11 de 08-15, e <b>alguém para ocupá-lo</b>, que é
         * o piso da Regra 11. Faltando qualquer uma, a troca deixaria uma
         * função vazia sem ninguém para reassumi-la no mesmo ciclo.
         *
         * <p><b>Hoje o mínimo não muda nada</b>, e é de propósito que ele
         * esteja escrito: cada baú livre entra na lista junto com o
         * candidato que o alcançou, então nunca há mais baú que
         * candidato. A garantia da Regra 11 vinha daí — de um acidente do
         * jeito de contar, e não de uma frase. Um dia em que os baús
         * livres passem a ser contados de outra maneira, esta linha é o
         * que impede a vila de perder o último lenhador.
         *
         * <p>A dispensa não conhece o piso; ela conhece o número que
         * recebe. É este.
         */
        public int substitutes() {
            return Math.min(freeChests.size(), equippable.size());
        }
    }
}
