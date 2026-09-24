package com.villagecolony.data.save;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

/**
 * Transforma o NBT de {@link ColonySavedData} para a forma atual, antes de
 * qualquer leitor decidir sozinho o que fazer com um valor legado —
 * decisão 8A, 2026-09-24.
 *
 * <p><b>Não é dona da mina.</b> {@code MineSave} já tem seu próprio
 * {@code SHAPE_VERSION}, testado em produção, cuja regra é "versão
 * diferente descarta a fronteira e recomeça do zero": é um descarte
 * deliberado, não uma tradução de forma antiga para nova, e por isso
 * continua fora daqui. Ver o javadoc de {@code MineSave.SHAPE_VERSION}.
 *
 * <p><b>O que esta classe resolve.</b> Antes dela, cada leitor de
 * {@code ColonySavedData} decidia sozinho o que fazer com um valor
 * desconhecido ou legado — {@code readProfession} traduzia
 * {@code BREEDER} para {@code SHEPHERD}, {@code readState} caía para
 * {@code STABLE}, e assim por diante. Isso funciona, mas espalha a
 * pergunta "que versão é esta?" pelo arquivo inteiro. {@code migrate}
 * concentra as transformações que dependem de <b>versão</b>, e não de
 * um valor pontual desconhecido: a primeira é {@code BREEDER}, que só
 * existe em saves anteriores ao Pastor e nunca mais será escrito de novo.
 *
 * <p><b>Nunca edita o mundo.</b> Só o {@code NbtCompound} em memória; nenhum
 * bloco, nenhuma entidade, nenhum {@code ServerWorld}.
 */
public final class SaveMigration {

    private static final String SAVE_VERSION = "saveVersion";
    private static final String WORKERS = "workers";
    private static final String PROFESSION = "profession";
    private static final String LEGACY_BREEDER = "BREEDER";
    private static final String SHEPHERD = "SHEPHERD";

    /**
     * A versão atual do esquema.
     *
     * <p><b>Um, desde 2026-09-24.</b> A primeira transformação registrada
     * é v0 → v1: normalizar {@code BREEDER} para {@code SHEPHERD} nos
     * trabalhadores, antes de {@code readProfession} nem chegar a olhar
     * o valor.
     */
    static final int CURRENT = 1;

    private SaveMigration() {
    }

    /**
     * Devolve uma cópia migrada para {@link #CURRENT}.
     *
     * <p>Save sem a chave {@code saveVersion} é tratado como versão zero —
     * todo save anterior a esta entrega. Save de uma versão <b>futura</b>
     * (maior que {@link #CURRENT}) não regride: o laço não roda, e o
     * valor gravado permanece o que já era. É a mesma régua que
     * {@code ConstructionState}/{@code ColonyState} usam para valor
     * desconhecido — preservar, nunca truncar.
     */
    public static NbtCompound migrate(NbtCompound source) {
        NbtCompound target = source.copy();

        int from = target.getInt(SAVE_VERSION);

        for (int v = from; v < CURRENT; v++) {
            apply(v, target);
        }

        if (from < CURRENT) {
            target.putInt(SAVE_VERSION, CURRENT);
        }

        return target;
    }

    /** Aplica a transformação que leva da versão {@code fromVersion} à seguinte. */
    private static void apply(int fromVersion, NbtCompound target) {
        switch (fromVersion) {
            case 0 -> migrateBreederToShepherd(target);
            default -> throw new IllegalStateException(
                    "no migration registered from saveVersion " + fromVersion);
        }
    }

    /**
     * v0 → v1: o Pastor sucedeu o Criador — 2026-09-19. Todo save mais
     * antigo grava {@code BREEDER}, e {@code SHEPHERD} é o único destino
     * correto; nenhum outro valor é alterado.
     */
    private static void migrateBreederToShepherd(NbtCompound target) {
        if (!target.contains(WORKERS, NbtElement.LIST_TYPE)) {
            return;
        }

        NbtList workers = target.getList(WORKERS, NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < workers.size(); i++) {
            NbtCompound worker = workers.getCompound(i);

            if (LEGACY_BREEDER.equals(worker.getString(PROFESSION))) {
                worker.putString(PROFESSION, SHEPHERD);
            }
        }
    }
}
