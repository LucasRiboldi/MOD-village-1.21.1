package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Remove as placas de obra órfãs — 2026-09-30.
 *
 * <p><b>Visto em jogo:</b> uma placa ficou flutuando sobre o lote depois
 * que a obra deixou de existir. O {@link SiteMarker} só removia a placa que
 * estivesse carregada <i>e</i> que o mapa dele lembrasse, e duas portas
 * deixavam uma placa sem ninguém que a reconhecesse:
 *
 * <ul>
 *   <li>a obra fechar com o chunk da placa descarregado — o
 *       {@code clearStale} não acha a entidade e a esquece;</li>
 *   <li>o servidor reiniciar, que zera o mapa; a placa só era reencontrada
 *       por uma obra <b>aberta</b>, e a de uma obra que já não existe nunca
 *       mais.</li>
 * </ul>
 *
 * <p><b>A placa passa a dizer de qual obra é</b>, numa etiqueta de comando
 * que o jogo grava junto da entidade. Quando ela volta ao mundo, esta
 * classe pergunta à obra se ainda está aberta; se não, a placa sai. A placa
 * sem etiqueta — todas as que as versões anteriores puseram — não tem a quem
 * perguntar e também sai: se a obra dela existe, o {@link SiteMarker}
 * levanta uma nova, etiquetada, no segundo seguinte.
 *
 * <p>Separada do {@link SiteMarker} pelo teto de 500 linhas do projeto.
 */
public final class SiteSignJanitor {

    /**
     * A etiqueta que diz de qual obra a placa é, seguida do id da obra.
     *
     * <p>Só letras, dígitos, {@code _} e {@code -}: cabe num seletor
     * {@code tag=} sem aspas, se alguém precisar dela em comando.
     */
    static final String PROJECT_TAG_PREFIX = "villagecolony_site_project_";

    /**
     * As placas que entraram no mundo desde a última passagem.
     *
     * <p><b>Julgadas no tique, e não na hora do carregamento.</b> O mundo
     * carrega os chunks da origem antes do {@code SERVER_STARTED}, quando as
     * obras ainda não voltaram do save: julgada ali, toda placa seria órfã.
     * E descartar entidade de dentro do carregamento do chunk é mexer na
     * lista que o jogo está percorrendo.
     *
     * <p>Por isso também <b>não</b> se inscreve na {@code ServerMemory}, que
     * zera no {@code SERVER_STARTED} — depois de os chunks da origem já terem
     * enfileirado as placas deles. É limpa ao parar o servidor.
     */
    private static final Set<UUID> LOADED = new HashSet<>();

    /** O que fazer com uma placa que acabou de entrar no mundo. */
    enum Fate {
        /** É a placa da obra; fica, e o {@link SiteMarker} passa a conhecê-la. */
        KEEP,
        /** Órfã ou repetida: sai do mundo. */
        DISCARD
    }

    private SiteSignJanitor() {
    }

    /** Passa a ouvir as placas que entram no mundo. */
    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(SiteSignJanitor::onEntityLoad);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LOADED.clear());
    }

    /** Enfileira a placa nossa que acabou de carregar; ver {@link #LOADED}. */
    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (entity instanceof ArmorStandEntity stand && SiteMarker.isSign(stand)) {
            LOADED.add(stand.getUuid());
        }
    }

    /**
     * Julga as placas que carregaram desde a última passagem.
     *
     * <p>Chamada pelo {@link SiteMarker} uma vez por segundo, antes do
     * desenho — para ele não adotar uma placa órfã.
     */
    static void judgeLoaded(ServerWorld world) {
        if (LOADED.isEmpty()) {
            return;
        }

        for (UUID id : LOADED) {
            // Descarregou de novo antes da vez dela: volta à fila quando
            // carregar outra vez.
            if (!(world.getEntity(id) instanceof ArmorStandEntity sign) || !SiteMarker.isSign(sign)) {
                continue;
            }

            Optional<UUID> project = projectOf(sign);
            boolean open = project.map(SiteSignJanitor::isOpen).orElse(false);
            UUID known = project.map(SiteMarker.SIGNS::get).orElse(null);
            boolean knownLoaded = known != null && world.getEntity(known) instanceof ArmorStandEntity;

            if (judge(project, open, id, known, knownLoaded) == Fate.DISCARD) {
                sign.discard();
                VillageColonyMod.LOGGER.debug("Removed orphan site sign {} (project {})",
                        id, project.map(UUID::toString).orElse("unknown"));
                continue;
            }

            SiteMarker.SIGNS.put(project.orElseThrow(), id);
        }

        LOADED.clear();
    }

    /** A obra desta placa, pela etiqueta; vazio na placa sem dono. */
    static Optional<UUID> projectOf(ArmorStandEntity sign) {
        for (String tag : sign.getCommandTags()) {
            if (tag.startsWith(PROJECT_TAG_PREFIX)) {
                return parseProject(tag);
            }
        }

        return Optional.empty();
    }

    /** O id da obra numa etiqueta; vazio se ela não for legível. */
    static Optional<UUID> parseProject(String tag) {
        if (!tag.startsWith(PROJECT_TAG_PREFIX)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(tag.substring(PROJECT_TAG_PREFIX.length())));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    /**
     * Se a obra ainda está aberta, contando a que o save trouxe e ainda
     * não renasceu — ela renasce no primeiro ciclo da colônia, e a placa
     * dela não é órfã por isso.
     */
    private static boolean isOpen(UUID projectId) {
        if (VillageColonyMod.CONSTRUCTIONS.find(projectId)
                .filter(project -> project.state().isOpen())
                .isPresent()) {
            return true;
        }

        return VillageColonyMod.CONSTRUCTIONS.allPending().stream()
                .anyMatch(pending -> pending.id().equals(projectId) && pending.state().isOpen());
    }

    /**
     * O destino de uma placa que entrou no mundo — a regra, sem mundo.
     *
     * @param project a obra da etiqueta; vazio na placa sem dono, que as
     *     versões antes de 2026-09-30 punham
     * @param open se essa obra ainda está aberta
     * @param self o id desta placa
     * @param known a placa que o {@link SiteMarker} tem para a obra, ou null
     * @param knownLoaded se essa placa conhecida está carregada agora
     */
    static Fate judge(
            Optional<UUID> project, boolean open, UUID self, UUID known, boolean knownLoaded) {

        // Sem dono não há obra a quem perguntar. Se a obra existe, ela
        // levanta uma placa nova, etiquetada, no próximo segundo.
        if (project.isEmpty() || !open) {
            return Fate.DISCARD;
        }

        if (known == null || known.equals(self)) {
            return Fate.KEEP;
        }

        // Duas placas para a mesma obra: fica a que o SiteMarker já conhece
        // e está no mundo; na mesma leva, a primeira julgada.
        return knownLoaded ? Fate.DISCARD : Fate.KEEP;
    }
}
