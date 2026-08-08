# ADR-006-Package-Layout.md

# Architecture Decision Record 006

# Village Colony Package Layout

**Status:** Accepted
**Date:** 2026-08-06
**Accepted:** 2026-08-06
**Decision Type:** Architecture / Code Structure
**Blocks:** TASK-003

---

# 1. Context

Quatro documentos aprovados definem quatro layouts diferentes.

---

## README.md §11

```text
core/models
core/services
core/managers

fabric/adapters
fabric/events
fabric/integration
```

Plural.

---

## Class-Architecture.md

```text
core/model
core/service
core/manager
```

Singular.

---

## Initial-Setup-Checklist.md §6

```text
core/model
core/service
core/manager
```

Singular. Igual ao anterior.

---

## Fabric-Implementation-Plan.md

```text
core/colony
core/task
core/profession
core/resource
core/storage
core/construction

fabric/entity
fabric/world
fabric/event
fabric/block
fabric/structure
```

Por domínio.

---

## O conflito real

São **dois eixos de organização incompatíveis**:

```text
Agrupar por camada

versus

Agrupar por domínio
```

Mais divergência singular/plural.

---

# 2. Decision

Adotar **domínio dentro da camada**.

---

# 3. Estrutura

```text
com.villagecolony

├── VillageColonyMod.java
│
├── core
│   │
│   ├── type
│   │   ├── ColonyPos
│   │   ├── ResourceId
│   │   └── ColonyRotation
│   │
│   ├── colony
│   │   ├── model
│   │   └── service
│   │
│   ├── worker
│   │   ├── model
│   │   └── service
│   │
│   ├── task
│   │   ├── model
│   │   └── service
│   │
│   ├── resource
│   │   ├── model
│   │   └── service
│   │
│   ├── storage
│   │   ├── model
│   │   └── service
│   │
│   └── construction
│       ├── model
│       └── service
│
├── fabric
│   ├── adapter
│   ├── event
│   ├── integration
│   ├── mixin
│   └── brain
│
└── data
    └── save
```

Singular em todos os níveis.

---

# 4. Justificativa

---

## Modularidade

`PROJECT_CONSTITUTION.md §13` exige que adicionar uma profissão ou recurso
exija modificação mínima do código existente.

---

Com agrupamento por camada:

```text
Adicionar "Miner"

↓

Tocar core/model/

Tocar core/service/

Tocar core/manager/
```

Feature espalhada por três pastas.

---

Com agrupamento por domínio:

```text
Adicionar "Miner"

↓

Tocar core/worker/
```

Um diretório.

---

## Legibilidade

`core/model/` com oito classes de domínios diferentes não comunica nada.

`core/construction/model/` comunica exatamente o que contém.

---

## Coesão

`CODE-STANDARDS.md §4` e `PROJECT_CONSTITUTION.md §15` pedem alta coesão
e baixo acoplamento.

Domínio dentro de camada entrega as duas.

---

# 5. Decisões acessórias

---

## Singular

```text
model    não    models

service  não    services
```

Motivo: um pacote descreve o **tipo** do que contém, não a quantidade.

Consistente com a convenção Java.

---

## Camada manager removida

`Class-Architecture.md` define `manager` como "acesso global aos sistemas".

Na prática `ColonyManager` e `ColonyService` se sobrepõem.

---

Resolução:

```text
Service

Contém lógica e mantém o registro em memória.
```

```text
data/save

Contém apenas serialização.
```

`ColonyManager` deixa de existir como camada.

Se um ponto de acesso global for necessário, ele é um campo estático em
`VillageColonyMod`, não uma camada.

Isso resolve a ambiguidade `ColonyManager` × `ColonySavedData` levantada
na auditoria.

---

## core/type

Existe por causa da ADR-005.

Contém apenas os tipos de valor livres de Minecraft.

---

## fabric/brain

Existe por causa da ADR-004.

Contém `Activity`, `MemoryModuleType` e tasks de Brain.

Separado de `mixin` porque mixin não contém lógica.

---

# 6. Regra de dependência

Permanece conforme `claude/CLAUDE.md §6`.

Permitido:

```text
fabric  →  core

data    →  core
```

Proibido:

```text
core  →  fabric

core  →  net.minecraft
```

---

Regra adicional:

Um domínio do core **não importa** outro domínio do core diretamente.

Comunicação por interface ou serviço, conforme
`Architecture-Foundation.md`.

---

Emenda de 2026-08-08 — `core/coordination`:

```text
core/coordination  →  qualquer domínio do core

qualquer domínio    ↛  outro domínio      (segue proibido)
core/coordination   ↛  fabric, data       (segue proibido)
```

A TASK-023 precisa casar tarefa com profissão, e isso exige ler
`core.task` e `core.worker` na mesma linha de código. Não havia lugar
legítimo para ela: `core/type` é para tipos de valor, e `fabric/` faria
regra de colônia morar na camada de integração — a única sem um teste
de unidade sequer, e onde todos os defeitos da semana apareceram.

`core/coordination` é uma camada acima dos domínios, não um domínio.
Não guarda estado próprio: recebe os serviços dos domínios e os
combina.

A emenda é estreita de propósito. Um domínio continua sem poder
importar outro; o que se abre é um pacote nomeado, e só ele.
`DependencyRuleTest` passa a conhecer a exceção — a regra continua
sendo teste, não confiança.

---

# 7. Consequences

Documentos a atualizar:

```text
README.md §11

Class-Architecture.md

Fabric-Implementation-Plan.md

Initial-Setup-Checklist.md §6
```

Todos devem apontar para esta ADR em vez de repetir a estrutura.

Fonte única de verdade.

---

# 8. Final Statement

Uma feature deve caber em um diretório.

Se ela se espalha, a estrutura está errada.
