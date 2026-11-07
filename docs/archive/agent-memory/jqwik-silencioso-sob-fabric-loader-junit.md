---
name: jqwik-silencioso-sob-fabric-loader-junit
description: "Com fabric-loader-junit no classpath, o jqwik não executa nenhuma @Property e o test fica verde; roda na tarefa propertyTest"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-30T20:19:17.135Z
---

Com o `fabric-loader-junit` no classpath do `test`, o listener dele carrega as
classes de teste pelo Knot, e o motor do jqwik não reconhece `@Property` vinda
de outro carregador: **nenhuma propriedade roda, e a suíte fica verde**
("No tests found" só aparece com filtro `--tests`). Visto em 2026-09-30.

Correção no build.gradle: tarefa `propertyTest` (no `check`) com o classpath do
teste **sem** `fabric-loader-junit` e `includeEngines 'jqwik'`. É o mesmo corte
que o PIT já fazia.

Junto, do mesmo dia: o NullAway em `JSpecifyMode` não roda no JDK 21 da Oracle
(pede JDK 22+ ou `-XDaddTypeAnnotationsToSymbol`, que ela não aceita); o
projeto usa o modo padrão com `OnlyNullMarked`. E o NullAway 0.12 quebra com o
Error Prone 2.50 (`NoClassDefFoundError DescendantOf`); use 0.14+.

**Why:** a regra do projeto é "suíte verde que não mediu nada é pior que
vermelha" — igual à [[gametest-registro-silencioso]].
**How to apply:** propriedade nova vai para uma classe jqwik e é conferida
em `build/test-results/propertyTest`, nunca em `test`. Antes de confiar numa
propriedade nova, plante uma mutação e veja o `propertyTest` reprovar.
