# ADR-005-Core-Type-Isolation.md

# Architecture Decision Record 005

# Village Colony Core Type Isolation

**Status:** Proposed — awaiting approval
**Date:** 2026-08-06
**Decision Type:** Architecture / Code Structure
**Blocks:** Phase 2 — Core Models

---

# 1. Context

A regra e a especificação se contradizem.

---

## A regra

`claude/CLAUDE.md §6`:

> Modelos nunca devem conhecer:
>
> VillagerEntity, ServerWorld, BlockEntity, Fabric API

`Architecture-Foundation.md`:

> Core não conhece blocos, entidades, mundo, Fabric.

---

## A especificação

`Data-Model.md` e `Class-Architecture.md` definem:

```java
Colony   { BlockPos center; }

Storage  { BlockPos position; }

Building { BlockPos position; Rotation rotation; }

Resource { Identifier itemId; }
```

---

## O problema

```text
net.minecraft.util.math.BlockPos

net.minecraft.util.Identifier

net.minecraft.util.BlockRotation
```

Os três são Minecraft.

O Core, como especificado, importa Minecraft na primeira linha.

**A regra é violada pelo documento que a define.**

---

## Por que isso importa

Não é purismo.

`Testing-Strategy.md §3` promete testes unitários do Core sem Minecraft.

Com `BlockPos` nos modelos, rodar um teste de `ColonyService` exige
classpath completo do Minecraft remapeado.

A promessa se torna falsa.

---

# 2. Decision

Criar **tipos de valor próprios** no Core.

Converter na camada adapter.

---

# 3. Tipos do Core

Todos imutáveis. Todos `record`.

---

## ColonyPos

```java
public record ColonyPos(int x, int y, int z) { }
```

Substitui:

```text
BlockPos
```

---

## ResourceId

```java
public record ResourceId(String namespace, String path) { }
```

Substitui:

```text
Identifier
```

Exemplo:

```text
ResourceId("minecraft", "oak_log")
```

---

## ColonyRotation

```java
public enum ColonyRotation {
    NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90
}
```

Substitui:

```text
BlockRotation
```

Os quatro valores espelham o Vanilla intencionalmente.

---

# 4. Conversão

Local:

```text
com.villagecolony.fabric.adapter
```

Classe:

```text
MinecraftTypeAdapter
```

---

Responsabilidade:

```text
BlockPos   <-> ColonyPos

Identifier <-> ResourceId

BlockRotation <-> ColonyRotation
```

---

Regra:

A conversão acontece **apenas** na fronteira.

Correto:

```text
ServerWorld

↓

MinecraftTypeAdapter

↓

Core Service
```

Incorreto:

```text
Core Service

↓

BlockPos
```

---

# 5. Custo

```text
3 tipos de valor

1 classe de conversão
```

Aproximadamente 60 linhas.

---

# 6. Retorno

* testes unitários reais, sem Minecraft no classpath;
* Core sobrevive a mudanças de versão do Minecraft;
* `BlockPos` mudou de pacote entre versões — o Core ficaria imune;
* a regra declarada passa a ser verdadeira.

---

# 7. Objeção considerada

> Isso é boilerplate desnecessário.

Resposta:

`PROJECT_CONSTITUTION.md §11` proíbe arquitetura especulativa.

Esta não é especulativa: resolve um problema que **já existe**
e está documentado em dois documentos aprovados.

O custo é de 60 linhas escritas uma única vez.

O custo de não fazer é perder a testabilidade do Core inteiro.

---

# 8. Regra de verificação

O Core deve compilar em um source set **sem dependência de Minecraft**.

Essa é a definição operacional de "Core independente".

Se não compilar isolado, a regra foi violada.

---

# 9. Consequences

---

## Data-Model.md

Substituir os tipos nas definições de campo.

---

## Class-Architecture.md

Idem.

---

## CODE-STANDARDS.md

Adicionar seção sobre a fronteira de conversão.

---

## Testing-Strategy.md

Passa a ser executável como escrito.

---

# 10. Final Statement

O Core não deve saber que o Minecraft existe.

Se souber, ele não é um Core — é um plugin.
