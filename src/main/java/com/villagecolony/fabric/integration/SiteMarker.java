package com.villagecolony.fabric.integration;

import com.villagecolony.core.type.ServerMemory;
import com.villagecolony.VillageColonyMod;
import com.villagecolony.core.construction.model.ConstructionProject;
import com.villagecolony.core.construction.model.ConstructionState;
import com.villagecolony.core.construction.model.SiteLabel;
import com.villagecolony.core.construction.model.SiteOutline;
import com.villagecolony.core.resource.model.ResourceTally;
import com.villagecolony.core.type.ColonyPos;
import com.villagecolony.core.type.ResourceId;
import com.villagecolony.fabric.adapter.MinecraftTypeAdapter;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O contorno do lote, desenhado no mundo — 2026-09-15.
 *
 * <p><b>Pedido do autor:</b> <i>"adicionar um marcador visual, um efeito
 * que demonstre onde no terreno está o espaço alocado para a construção
 * escolhida"</i>. Três sessões seguidas ele entrou no jogo, não viu casa
 * nascendo, e o log dizia que a colônia planejava — sem marcador, <i>"não
 * achou lote"</i> e <i>"achou lote a setenta blocos"</i> são a mesma coisa
 * de dentro do jogo.
 *
 * <p><b>Fica no servidor</b>, e é o que torna isto barato e seguro:
 * {@code ServerWorld.spawnParticles} manda o pacote para quem está por
 * perto, e o cliente não precisa saber nada do mod. Nenhum entrypoint de
 * cliente, nenhum networking próprio, nenhuma travessia de thread — que é
 * a armadilha que este projeto já pagou duas vezes (§11).
 *
 * <p><b>A cor diz o estado</b>, e é o diagnóstico que o autor pediu de
 * fato: chama para a obra que está sendo construída, fumaça para a que
 * espera material. Quem olha o lote sabe se a casa está andando ou parada
 * sem abrir o log.
 */
public final class SiteMarker {

    static {
        ServerMemory.register(SiteMarker.class, SiteMarker::clearAll);
    }

    /**
     * De quantos em quantos tiques o contorno pisca.
     *
     * <p>Vinte é uma vez por segundo. Partícula é pacote de rede por
     * posição: um lote 7x7 tem 24 colunas de borda, e desenhar a cada
     * tique seriam 480 pacotes por segundo por obra. Uma vez por segundo
     * o contorno fica visível e contínuo ao olho — as partículas vivem
     * mais que um tique — e custa 24.
     */
    private static final int EVERY_TICKS = 20;

    /**
     * A que distância o contorno é desenhado.
     *
     * <p>Mesma régua da vila. Mais longe que isto o jogador não vê
     * partícula de qualquer jeito, e desenhar seria pagar rede por nada.
     */
    private static final int WITHIN = 64;

    private static int tickCounter;

    /**
     * O estoque que o último ciclo de cada colônia leu.
     *
     * <p><b>Guardado em vez de relido</b>, e é o que torna a placa barata:
     * contar baú é varredura de inventário, e a placa desenha uma vez por
     * segundo contra os trinta segundos do ciclo. Reler aqui multiplicaria
     * por trinta o custo que o {@code CycleCost} mede como {@code chests}.
     *
     * <p>O número pode estar até trinta segundos velho, e isso é honesto
     * para o que a placa responde — <i>"a colônia tem o material?"</i> não
     * muda de segundo em segundo. Quem precisa do número exato é o
     * construtor, e ele lê o baú na hora.
     */
    private static final Map<UUID, ResourceTally> STOCK = new HashMap<>();

    /**
     * A etiqueta que marca um suporte de armadura como placa nossa.
     *
     * <p>Sem ela, um suporte que o jogador tenha posto sobre o lote seria
     * tomado por placa da colônia — e reescrito, ou removido ao fim da
     * obra. A Regra 3 aplicada à decoração dele.
     */
    private static final String SIGN_TAG = "villagecolony_site_sign";

    /** A placa de cada obra, para achá-la de volta e removê-la no fim. */
    private static final Map<UUID, UUID> SIGNS = new HashMap<>();

    private SiteMarker() {
    }

    /**
     * Guarda o que esta colônia tem, para a placa poder dizer.
     *
     * <p>Chamado do ciclo, que já leu os baús — ver {@link #STOCK}.
     */
    public static void remember(UUID colonyId, ResourceTally stock) {
        STOCK.put(colonyId, stock);
    }

    /**
     * Desenha o contorno das obras abertas, uma vez por segundo.
     *
     * <p>Chamada do tique do servidor. Sai de graça na esmagadora maioria
     * dos tiques — é um contador e uma comparação.
     */
    public static void tick(ServerWorld world) {
        if (++tickCounter < EVERY_TICKS) {
            return;
        }

        tickCounter = 0;

        // As placas de obra fechada saem primeiro, e saem mesmo sem
        // ninguém por perto: entidade órfã no save não espera plateia.
        clearStale(world);

        if (world.getPlayers().isEmpty()) {
            // Ninguém para ver. Partícula sem plateia é pacote jogado fora.
            return;
        }

        for (ConstructionProject project : VillageColonyMod.CONSTRUCTIONS.all()) {
            if (!project.state().isOpen()) {
                continue;
            }

            draw(world, project);
        }
    }

    /** O contorno de uma obra. */
    private static void draw(ServerWorld world, ConstructionProject project) {
        ColonyPos origin = project.origin();
        ColonyPos size = project.blueprint().size();

        ColonyPos far = new ColonyPos(
                origin.x() + size.x() - 1,
                origin.y(),
                origin.z() + size.z() - 1);

        List<ColonyPos> border = SiteOutline.of(origin, far);

        if (noPlayerNear(world, origin)) {
            return;
        }

        // Chama para a obra que anda, fumaça para a que espera material —
        // ver o javadoc da classe. O estado é lido uma vez, fora do laço.
        boolean waiting = project.state() == ConstructionState.WAITING_RESOURCES;

        label(world, project, origin, size);

        for (ColonyPos at : border) {
            BlockPos pos = MinecraftTypeAdapter.toBlockPos(at);

            // Meio bloco acima do piso: no piso a partícula nasce dentro
            // do chão e o jogador não a vê.
            world.spawnParticles(
                    waiting ? ParticleTypes.SMOKE : ParticleTypes.FLAME,
                    pos.getX() + 0.5,
                    pos.getY() + 1.1,
                    pos.getZ() + 0.5,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0);
        }
    }

    /**
     * Quantos blocos acima do topo da planta a placa flutua — 2026-09-24.
     *
     * <p><b>Decisão do autor (N7):</b> <i>"o da do lote precisa ficar 5
     * blocos acima da altura da construção para facilitar a leitura"</i>.
     * A versão de 09-18 a punha a 2,5 do chão: legível de perto, mas
     * dentro da parede que subia, e escondida atrás dela de longe. Acima
     * do telhado ela se lê de qualquer ponto da vila, e a obra não a cobre
     * em nenhuma altura.
     */
    static final int LABEL_ABOVE_TOP = 5;

    /**
     * A placa que flutua sobre o lote — 2026-09-16.
     *
     * <p><b>Correção de rumo.</b> A primeira versão, de 09-15, pôs a linha
     * no nome do <b>construtor</b>, e o autor a recusou: <i>"os itens que
     * faltam da obra deve ficar flutuando no espaço da construção e não no
     * lugar do nome do trabalhador"</i>. Ele está certo — o nome do
     * trabalhador diz o ofício dele, e sobrescrevê-lo trocava uma
     * informação por outra em vez de somar.
     *
     * <p><b>Um suporte de armadura invisível</b>, marcado, sobre o centro
     * do lote. A escolha foi entre ele e o {@code TextDisplayEntity} do
     * 1.21: o segundo é feito para isto, mas o texto dele só se escreve por
     * NBT — não há setter — e montar NBT à mão para uma placa é mais
     * frágil do que o {@code setCustomName} que o mod já usa desde 08-08.
     *
     * <p><b>O lixo no save era o medo, e ele é tratado</b>: a placa é
     * procurada antes de ser criada, reusada enquanto a obra existe, e
     * removida assim que ela fecha — ver {@link #clearStale}. Um suporte
     * que sobrevivesse ao fim da obra seria entidade órfã no mundo do
     * jogador, e disso o projeto já tem cicatriz.
     */
    private static void label(
            ServerWorld world, ConstructionProject project, ColonyPos origin, ColonyPos size) {

        ResourceTally stock = STOCK.get(project.colonyId());

        String line = SiteLabel.of(
                project.remainingMaterials(),
                stock == null ? Map.of() : stock.idCounts(),
                project.remainingCount(),
                SiteMarker::nameOf,
                // O bloco em que o construtor para quando falta material —
                // 2026-09-25; ver SiteLabel.of.
                project.nextBlock().map(block -> block.block()),
                project.deferredPieces().size());

        // O centro do lote, cinco blocos acima do topo da planta — N7,
        // 2026-09-24; ver LABEL_ABOVE_TOP.
        double x = origin.x() + size.x() / 2.0;
        double y = labelY(origin, size);
        double z = origin.z() + size.z() / 2.0;

        ArmorStandEntity sign = findSign(world, project, x, y, z);

        if (sign == null) {
            return;
        }

        Text text = Text.literal(line).formatted(Formatting.AQUA);

        if (!text.getString().equals(sign.getCustomName() == null
                ? "" : sign.getCustomName().getString())) {

            // Reescrever a cada segundo mandaria pacote de metadado para
            // todo cliente perto sem nada ter mudado.
            sign.setCustomName(text);
        }
    }

    /**
     * O nome do bloco na língua do jogo — 2026-09-17.
     *
     * <p><b>Quem traduz é o Vanilla</b>, por {@code Block.getName()}: a
     * placa dizia {@code grass_block} onde o jogo diz <i>Bloco de
     * Grama</i>. O mod já fazia assim em três lugares — o
     * {@code MinerReport} escreve "Terra" e "Pedra" no log desde 09-03 —,
     * e esta linha só leva o mesmo padrão para a placa.
     *
     * <p><b>Nada de tabela própria.</b> Escrever os nomes aqui seria
     * refazer o que o jogo mantém traduzido em toda língua, com risco de
     * errar e de envelhecer; e quebraria em espanhol, inglês ou qualquer
     * idioma que o jogador escolher. Ver {@code SiteLabel.of}, que recebe
     * esta função justamente para o {@code core} não precisar conhecer
     * Minecraft.
     *
     * <p>Bloco que este jogo não conhece devolve vazio, e o
     * {@code SiteLabel} cai no id — feio, e informa.
     */
    private static String nameOf(ResourceId material) {
        return MinecraftTypeAdapter.toBlock(material)
                .map(block -> block.getName().getString())
                .orElse("");
    }

    /**
     * A placa desta obra: a que já existe, ou uma nova.
     *
     * <p>Procurada pela caixa em volta do ponto, e reconhecida pela marca
     * de {@link SiteLabel}: sem a marca, um suporte de armadura que o
     * jogador tenha posto ali viraria placa da colônia.
     */
    private static ArmorStandEntity findSign(
            ServerWorld world, ConstructionProject project, double x, double y, double z) {

        // A placa já conhecida é levada para a altura certa, em vez de
        // procurada pela caixa: uma placa da regra antiga, a 2,5 do chão,
        // ficaria fora da caixa e viraria órfã ao lado da nova.
        UUID known = SIGNS.get(project.id());

        if (known != null && world.getEntity(known) instanceof ArmorStandEntity sign
                && isSign(sign)) {
            if (sign.getX() != x || sign.getY() != y || sign.getZ() != z) {
                sign.refreshPositionAfterTeleport(x, y, z);
            }

            return sign;
        }

        // A placa de uma sessão anterior: a coluna inteira do lote, e não
        // só a caixa em volta do ponto, pela mesma razão.
        Box column = new Box(x - 1.5, y - LABEL_ABOVE_TOP - 64, z - 1.5, x + 1.5, y + 1.5, z + 1.5);

        for (ArmorStandEntity found
                : world.getEntitiesByClass(ArmorStandEntity.class, column, SiteMarker::isSign)) {

            SIGNS.put(project.id(), found.getUuid());
            found.refreshPositionAfterTeleport(x, y, z);

            return found;
        }

        return raiseSign(world, x, y, z);
    }

    /** A altura da placa: o topo da planta mais {@link #LABEL_ABOVE_TOP}. */
    static double labelY(ColonyPos origin, ColonyPos size) {
        return origin.y() + size.y() + LABEL_ABOVE_TOP;
    }

    /** Um suporte novo, invisível e sem colisão, só para carregar o nome. */
    private static ArmorStandEntity raiseSign(
            ServerWorld world, double x, double y, double z) {

        ArmorStandEntity sign = EntityType.ARMOR_STAND.create(world);

        if (sign == null) {
            return null;
        }

        sign.setPosition(x, y, z);

        // Invisível, sem gravidade, sem colisão e sem braços: o que o
        // jogador vê é só o nome flutuando. Marcado, para ser encontrado
        // de novo e removido depois.
        sign.setInvisible(true);
        sign.setNoGravity(true);
        sign.setInvulnerable(true);
        sign.setSilent(true);
        sign.setCustomNameVisible(true);
        sign.addCommandTag(SIGN_TAG);

        world.spawnEntity(sign);

        return sign;
    }

    /** Se este suporte é uma placa nossa, e não decoração do jogador. */
    private static boolean isSign(ArmorStandEntity candidate) {
        return candidate.getCommandTags().contains(SIGN_TAG);
    }

    /**
     * Tira as placas de obra que já fecharam — 2026-09-16.
     *
     * <p>É a metade que impede o lixo no save. Sem ela, cada casa terminada
     * deixaria um suporte de armadura invisível de pé para sempre, com o
     * último recado congelado.
     */
    private static void clearStale(ServerWorld world) {
        SIGNS.entrySet().removeIf(entry -> {
            boolean open = VillageColonyMod.CONSTRUCTIONS.find(entry.getKey())
                    .filter(project -> project.state().isOpen())
                    .isPresent();

            if (open) {
                return false;
            }

            if (world.getEntity(entry.getValue()) instanceof ArmorStandEntity sign) {
                sign.discard();
            }

            return true;
        });
    }

    /**
     * Se não há jogador perto o bastante para ver.
     *
     * <p>Horizontal, como todo raio de vila neste projeto: o jogador no
     * fundo da mina continua sendo o jogador daquela vila.
     */
    private static boolean noPlayerNear(ServerWorld world, ColonyPos at) {
        return world.getPlayers().stream().noneMatch(player -> {
            long dx = (long) player.getBlockX() - at.x();
            long dz = (long) player.getBlockZ() - at.z();

            return dx * dx + dz * dz <= (long) WITHIN * WITHIN;
        });
    }

    /** Esquece o contador. Chamado ao parar o servidor. */
    public static void clearAll() {
        tickCounter = 0;

        STOCK.clear();
        SIGNS.clear();
    }
}
