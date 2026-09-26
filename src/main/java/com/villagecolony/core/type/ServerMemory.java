package com.villagecolony.core.type;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tudo o que o mod guarda em memória por servidor, e uma forma só de esquecer
 * — 2026-09-24, item 3 da avaliação técnica.
 *
 * <p><b>O defeito que isto fecha.</b> Havia 50 classes com {@code clearAll()} e
 * o ciclo de vida chamava 78 limpezas escritas à mão, em duas listas — ao abrir
 * e ao fechar o mundo — que já tinham divergido: a de fechar não limpava
 * {@code PlanRefusals} nem {@code PlayerWorldChangeHandler}, e o
 * {@code BiomeConstructionSupply.clearAll()} não era chamado por ninguém, de modo
 * que os relógios de "rota atrasada" de um mundo passavam para o próximo.
 * Toda classe nova com estado era mais uma linha a lembrar em dois lugares.
 *
 * <p><b>Agora cada classe se inscreve sozinha</b>, no próprio bloco estático:
 *
 * <pre>
 * static {
 *     ServerMemory.register(MinhaClasse.class, MinhaClasse::clearAll);
 * }
 * </pre>
 *
 * <p>Classe que nunca carregou não tem estado a esquecer; quando carregar,
 * se inscreve. O {@code ServerMemoryRegistrationTest} percorre as classes do
 * mod e reprova a que tem {@code clearAll()} sem se inscrever.
 *
 * <p>Os registros de domínio de {@code VillageColonyMod} (colônias,
 * trabalhadores, tarefas…) continuam limpos à parte pelo ciclo de vida: são
 * instâncias, e o ciclo de vida os repovoa do save logo em seguida.
 */
public final class ServerMemory {

    private static final Map<String, Runnable> RESETTERS = new LinkedHashMap<>();

    private ServerMemory() {
    }

    /** Inscreve o esquecimento de uma classe. Inscrever de novo substitui. */
    public static synchronized void register(Class<?> owner, Runnable reset) {
        RESETTERS.put(owner.getName(), reset);
    }

    /**
     * Esquece tudo o que está inscrito, na ordem de inscrição.
     *
     * <p><b>Uma limpeza pode inscrever outra</b> — 2026-09-25, visto em jogo.
     * Um {@code clearAll} que toca uma classe ainda não carregada dispara o
     * bloco estático dela, e ela se inscreve aqui no meio da volta. Percorrer
     * o mapa direto dava {@code ConcurrentModificationException} ao fechar o
     * mundo, e o resto da memória ficava sem limpar. Por isso a volta é sobre
     * uma cópia, repetida até ninguém novo aparecer; cada limpeza roda uma
     * vez.
     *
     * @return quantas classes foram limpas
     */
    public static synchronized int resetAll() {
        Set<String> done = new HashSet<>();
        boolean someoneNew = true;

        while (someoneNew) {
            someoneNew = false;

            for (Map.Entry<String, Runnable> entry : List.copyOf(RESETTERS.entrySet())) {
                if (done.add(entry.getKey())) {
                    entry.getValue().run();
                    someoneNew = true;
                }
            }
        }

        return done.size();
    }

    /** Os nomes das classes inscritas — para o teste de inscrição e o log. */
    public static synchronized Set<String> registered() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(RESETTERS.keySet()));
    }
}
