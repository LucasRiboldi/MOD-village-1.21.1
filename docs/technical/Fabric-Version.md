# Fabric-Version.md

# Village Colony — Fabric Technical Specification

**Status:** Approved
**Minecraft Version:** 1.21.1
**Mod Loader:** Fabric
**Language:** Java

---

# 1. Purpose

Este documento define o ambiente técnico oficial do projeto Village Colony.

Nenhuma implementação deve utilizar versões diferentes sem atualização formal deste documento.

---

# 2. Minecraft Version

## Target Version

```text
Minecraft Java Edition 1.21.1
```

---

## Reason

A versão 1.21.1 foi escolhida por:

* estabilidade;
* suporte ao Fabric;
* disponibilidade de ferramentas;
* compatibilidade com desenvolvimento moderno.

---

# 3. Mod Loader

## Fabric

O projeto utiliza:

```text
Fabric Loader
```

---

## Reason

Fabric foi escolhido por:

* baixo overhead;
* arquitetura modular;
* rápida atualização;
* excelente integração com mods leves.

---

# 4. Required Dependencies

## Fabric API

Obrigatório:

```text
Fabric API
```

Responsável por:

* eventos;
* registros;
* compatibilidade;
* APIs comuns.

---

## No External Dependencies

O projeto não deve depender de:

* servidores externos;
* APIs online;
* bancos de dados;
* serviços pagos.

---

# 5. Programming Language

## Java

O código será desenvolvido em:

```text
Java 21
```

---

## Reason

Minecraft 1.20.5 e superiores exigem Java 21.

Não é uma preferência do projeto. É um requisito da versão alvo.

---

## Toolchain

O Gradle deve fixar:

```text
java.toolchain.languageVersion = 21
```

Nunca depender do JDK padrão da máquina.

---

# 5.1 Version Matrix

As versões abaixo devem ser preenchidas com os valores exatos
**antes** da TASK-001, consultando fabricmc.net.

Não usar "latest" em nenhuma delas.

---

```text
Minecraft:      1.21.1        (fixado)

Java:           21            (fixado)

Yarn:           1.21.1+build.?   ← confirmar

Fabric Loader:  0.16.?           ← confirmar

Fabric API:     ?+1.21.1         ← confirmar

Fabric Loom:    1.7-SNAPSHOT     ← confirmar
```

---

## Mappings

Decisão:

```text
Yarn
```

Motivo:

Toda a documentação do projeto usa nomenclatura Yarn
(`ServerWorld`, `BlockPos`, `VillagerEntity`).

Mojmap usaria nomes diferentes e invalidaria os documentos existentes.

---

## Regra

`§8` deste documento dizia "mappings oficiais compatíveis".

Isso era ambíguo: Yarn não é oficial, Mojmap é.

A ambiguidade fica resolvida aqui em favor de Yarn.

---

## Requirements

O código deve seguir:

* orientação a objetos;
* baixo acoplamento;
* separação de responsabilidades.

---

# 6. Development Environment

Recomendado:

```text
IntelliJ IDEA
```

---

Ferramentas:

* Gradle;
* Fabric Loom;
* Java Development Kit.

---

# 7. Project Build

Compilação:

```bash
./gradlew build
```

---

Execução de desenvolvimento:

```bash
./gradlew runClient
```

---

# 8. Mapping

O projeto utiliza os mappings oficiais compatíveis com Minecraft 1.21.1 Fabric.

Mappings devem permanecer consistentes durante o desenvolvimento.

---

# 9. Compatibility Target

O mod deve funcionar em:

## Singleplayer

Obrigatório.

---

## Dedicated Server

Obrigatório.

---

## Multiplayer

Compatível quando instalado corretamente.

---

# 10. Fabric Integration Rules

Classes Fabric podem acessar:

* ServerWorld;
* Entity;
* BlockEntity;
* Registry.

Porém:

Essas referências não podem entrar no Core.

---

# 11. Package Rule

Estrutura:

```text
com.villagecolony
```

Separação:

```text
core

fabric

data
```

---

# 12. Version Changes

Qualquer alteração:

* Minecraft version;
* Fabric Loader;
* Fabric API;

deve gerar atualização deste documento.

---

# Final Rule

O ambiente técnico deve permanecer estável.

Não atualizar versões apenas por disponibilidade.

Atualizações devem trazer benefício real ao projeto.
