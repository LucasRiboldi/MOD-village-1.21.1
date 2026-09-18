package com.villagecolony.fabric.integration;

import com.villagecolony.VillageColonyMod;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * O que a Regra 3 está de fato protegendo — P1.3, 2026-09-18.
 *
 * <p><b>A pergunta que o playtest do deserto abriu.</b> Num mundo só de
 * deserto a colônia varreu o raio inteiro — 16.641 colunas, uma rodada
 * completa — e aprovou <b>zero</b> lotes. Das 11.284 recusas,
 * <b>7.821 (69%)</b> vieram da Regra 3, contra 19% na planície. O mod
 * nunca tinha sido medido fora da planície.
 *
 * <p><b>Por que a suspeita precisa de medida, e não de leitura de
 * código.</b> O javadoc de {@code BlockProtection.isVillageOriginal}
 * afirma que a verificação é <i>"por peça, e não pela caixa da vila
 * inteira"</i>, e o de {@code BlockProtectionGameTest} fala em
 * <i>"a caixa da vila"</i> — os dois se contradizem, e nenhum teste
 * decide: a arena de gametest é vazia e <b>não gera vila</b>, então
 * {@code isVillageOriginal} nunca foi exercitada com estrutura real.
 * Ler o código do jogo decompilado responderia o que o método faz; só o
 * jogo responde o que ele <b>devolve naquele terreno</b>.
 *
 * <p><b>O que esta amostra decide.</b> Se os blocos protegidos forem
 * {@code sand} e {@code sandstone}, a Regra 3 está protegendo o chão do
 * deserto — areia que é terreno, não construção — e o conserto é
 * distinguir peça de terreno. Se forem {@code cobblestone},
 * {@code oak_planks} e afins, ela está protegendo casa de verdade e o
 * gargalo é outro: a vila é densa e não sobra espaço.
 *
 * <p><b>Uma amostra, não um log por bloco.</b> São 7.821 recusas em meia
 * hora; uma linha por bloco afogaria o log e mudaria o que se está
 * medindo. Conta por tipo de bloco e diz o resumo junto com o
 * {@code LotRefusals}, que é onde quem investiga já está olhando.
 */
public final class ProtectionSample {

    private ProtectionSample() {
    }

    /**
     * Quantos tipos distintos guardar antes de parar de aprender.
     *
     * <p>Um mundo com datapack pode ter muitos blocos; o que interessa é
     * a cauda pesada — os dois ou três tipos que respondem pela maioria.
     * Trinta é folgado para isso e não cresce sem teto.
     */
    private static final int MAX_KINDS = 30;

    /** Quantos blocos de cada tipo a Regra 3 protegeu. */
    private static final Map<String, Integer> SEEN = new HashMap<>();

    /** A Regra 3 protegeu este bloco. */
    public static void saw(ServerWorld world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();

        String name = Registries.BLOCK.getId(block).getPath();

        if (SEEN.size() >= MAX_KINDS && !SEEN.containsKey(name)) {
            return;
        }

        SEEN.merge(name, 1, Integer::sum);
    }

    /**
     * Diz o que foi protegido, da maior contagem para a menor.
     *
     * <p>Sai junto com o relatório de recusas de lote, e só quando houve
     * alguma: linha de amostra vazia é ruído.
     */
    public static void report() {
        if (SEEN.isEmpty()) {
            return;
        }

        StringBuilder said = new StringBuilder();

        SEEN.entrySet().stream()
                .sorted((one, other) -> other.getValue() - one.getValue())
                .forEach(entry -> {
                    if (said.length() > 0) {
                        said.append("; ");
                    }

                    said.append(entry.getValue()).append(' ').append(entry.getKey());
                });

        VillageColonyMod.LOGGER.info("Rule 3 protected these blocks — {}", said);
    }

    /** Esquece a amostra. Chamado ao parar o servidor. */
    public static void clearAll() {
        SEEN.clear();
    }
}
