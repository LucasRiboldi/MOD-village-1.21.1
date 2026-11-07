---
name: village-colony-build-env
description: O build do Village Colony exige JVM 21 explícita — o JDK do PATH é Java 8
metadata: 
  node_type: memory
  type: project
  modified: 2026-08-15T15:19:00.643Z
---

O build do Village Colony só funciona com `JAVA_HOME` apontando para um
JDK 21. Verificado funcionando em 2026-08-15 via PowerShell:

```
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"; .\gradlew.bat build
```

Há também `C:\Users\lucas\.jdks\jdk-21.0.12+8` (Temurin, instalado em
08-06); qualquer um dos dois serve. O build completo leva ~10 s.

**Por quê:** o Java do PATH da máquina é JRE 1.8, e o único outro JDK era o
JBR 17 do IntelliJ. O Fabric Loom 1.17.18 exige JVM 21 para rodar o próprio
plugin Gradle — não apenas para a toolchain — então o `foojay-resolver`
declarado em `settings.gradle` não resolve sozinho. O JDK 21 (Temurin) foi
instalado em 2026-08-06 sob `~/.jdks` justamente para isso.

**Como aplicar:** sempre exportar `JAVA_HOME` antes de qualquer `gradlew`
neste projeto. Sem isso o build falha com "Dependency requires at least JVM
runtime version 21". Dentro do IntelliJ funciona sem ajuste, porque ele
detecta `~/.jdks` sozinho. Ver [[minecraft-test-install]].
