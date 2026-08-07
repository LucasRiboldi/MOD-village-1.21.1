package com.villagecolony.core.type;

/**
 * Posição de bloco no mundo, livre de Minecraft.
 *
 * <p>Substitui {@code BlockPos}. A conversão acontece apenas na fronteira,
 * em {@code fabric.adapter.MinecraftTypeAdapter}. Ver ADR-005.
 *
 * @param x coordenada leste-oeste
 * @param y altura
 * @param z coordenada norte-sul
 */
public record ColonyPos(int x, int y, int z) {

    /**
     * Distância horizontal ao quadrado até outra posição.
     *
     * <p>Ignora {@code y} porque as distâncias da colônia são medidas no
     * plano: uma casa não fica longe por estar num morro.
     *
     * <p>Devolve o quadrado para evitar a raiz quadrada — comparar
     * distâncias não precisa dela, e este cálculo roda em laços de
     * detecção. Ver Performance-Rules.md.
     */
    public long horizontalDistanceSquared(ColonyPos other) {
        long dx = (long) x - other.x;
        long dz = (long) z - other.z;

        return dx * dx + dz * dz;
    }
}
