package com.villagecolony.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras de arquitetura como teste, lidas do bytecode — ArchUnit, 2026-09-30.
 * Ver docs/research/2026-09-30-avaliacao-profissional-de-codigo.md §3.
 *
 * <p><b>O que o {@code DependencyRuleTest} não alcança.</b> Aquele lê
 * {@code import} no fonte, que é o certo para a ADR-006; este lê as classes
 * compiladas, e pega o nome totalmente qualificado, o ciclo entre pacotes e o
 * que o mixin chama de fato.
 *
 * <p><b>Congeladas.</b> As regras que o código de hoje já viola são
 * {@link FreezingArchRule}: as violações existentes ficam gravadas em
 * {@code src/test/resources/archunit_store} (versionado) e só uma violação
 * <b>nova</b> reprova. Quando alguém conserta uma, o registro encolhe
 * sozinho — ver {@code archunit.properties}.
 */
class ArchitectureRulesTest {

    private static final Path CLASSES = Path.of("build", "classes", "java", "main");

    private static JavaClasses mod;

    @BeforeAll
    static void importTheMod() {
        assertTrue(Files.isDirectory(CLASSES),
                "classes do mod não encontradas em " + CLASSES.toAbsolutePath());

        mod = new ClassFileImporter().importPath(CLASSES);

        assertTrue(mod.size() > 300,
                "o importador achou só " + mod.size() + " classes — o caminho mudou?");
    }

    /** ADR-006 §6: o core não conhece Minecraft, nem por nome qualificado. */
    @Test
    void theCoreDoesNotTouchMinecraftOrFabric() {
        ArchRule rule = noClasses().that().resideInAPackage("com.villagecolony.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "net.minecraft..", "net.fabricmc..", "com.villagecolony.fabric..")
                .because("o core é a lógica pura, testável sem o jogo (ADR-006)");

        rule.check(mod);
    }

    /**
     * Sem ciclo novo entre os pacotes da camada fabric.
     *
     * <p>Em 2026-09-30 havia quatro: work ↔ integration, work ↔ event,
     * integration ↔ event e adapter ↔ integration. Estão congelados; desfazê-los
     * é trabalho contínuo, e o registro mostra quanto falta.
     */
    @Test
    void theFabricLayerGrowsNoNewPackageCycle() {
        ArchRule rule = slices().matching("com.villagecolony.fabric.(*)..")
                .should().beFreeOfCycles();

        FreezingArchRule.freeze(rule).check(mod);
    }

    /** Idem para o core, que hoje não tem ciclo: fica sem congelar. */
    @Test
    void theCoreHasNoPackageCycle() {
        slices().matching("com.villagecolony.core.(*)..")
                .should().beFreeOfCycles()
                .check(mod);
    }

    /** ADR-004 §4, Regra 3: o mixin só delega. */
    @Test
    void aMixinOnlyDelegates() {
        ArchRule rule = classes().that().resideInAPackage("com.villagecolony.fabric.mixin..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "com.villagecolony.fabric.mixin..",
                        "com.villagecolony.fabric.brain..",
                        "com.villagecolony.fabric.integration..",
                        "net.minecraft..",
                        "org.spongepowered..",
                        "com.llamalad7..",
                        "java..")
                .because("a decisão mora fora do mixin (ADR-004 §4, Regra 3)");

        rule.check(mod);
    }

    /**
     * Coleção estática mutável é memória de servidor, e memória de servidor se
     * inscreve no {@code ServerMemory} — ou sobrevive de um mundo para o outro.
     *
     * <p>O {@code ServerMemoryRegistrationTest} só alcança quem declara
     * {@code clearAll()}; esta regra pega também quem guarda um mapa estático
     * e nunca declarou como esquecê-lo.
     */
    @Test
    void aStaticMutableCollectionIsServerMemory() {
        ArchRule rule = classes().that().resideInAPackage("com.villagecolony..")
                .should(registerServerMemoryWhenTheyKeepStaticCollections())
                .because("estado estático sobrevive à troca de mundo (ServerMemory)");

        FreezingArchRule.freeze(rule).check(mod);
    }

    private static final Set<String> MUTABLE_COLLECTIONS = Set.of(
            "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap",
            "java.util.EnumMap", "java.util.WeakHashMap", "java.util.IdentityHashMap",
            "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet",
            "java.util.ArrayList", "java.util.LinkedList", "java.util.ArrayDeque",
            "java.util.concurrent.ConcurrentHashMap");

    private static ArchCondition<JavaClass> registerServerMemoryWhenTheyKeepStaticCollections() {
        return new ArchCondition<>("register ServerMemory when they keep static mutable collections") {
            @Override
            public void check(JavaClass type, ConditionEvents events) {
                if (type.getStaticInitializer().isEmpty()
                        || type.getPackageName().startsWith("com.villagecolony.fabric.mixin")) {
                    return;
                }

                boolean keeps = false;

                for (JavaConstructorCall call : type.getStaticInitializer().get().getConstructorCallsFromSelf()) {
                    if (MUTABLE_COLLECTIONS.contains(call.getTargetOwner().getName())) {
                        keeps = true;
                        break;
                    }
                }

                if (!keeps) {
                    return;
                }

                boolean registers = false;

                for (JavaMethodCall call : type.getStaticInitializer().get().getMethodCallsFromSelf()) {
                    if (call.getTargetOwner().getName().equals("com.villagecolony.core.type.ServerMemory")
                            && call.getName().equals("register")) {
                        registers = true;
                        break;
                    }
                }

                if (!registers) {
                    events.add(SimpleConditionEvent.violated(type, type.getName()
                            + " keeps a static mutable collection and does not register ServerMemory"));
                }
            }
        };
    }
}
