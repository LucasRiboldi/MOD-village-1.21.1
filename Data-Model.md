# Data-Model.md

# Village Colony — Data Model

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir os modelos de dados fundamentais do projeto Village Colony.

Este documento descreve:

* entidades principais;
* atributos;
* relacionamentos;
* estados;
* responsabilidades.

---

# Princípio Fundamental

Os modelos representam dados.

Eles não:

* tomam decisões;
* executam tarefas;
* alteram o mundo;
* controlam aldeões.

A lógica pertence aos sistemas.

---

# Modelo Geral

```text
Minecraft World

        |
        |
        v

    Colony

        |
        |
 +------+------+-------+

 |      |      |       |

Worker Task Resource Building

        |

     Storage
```

---

# Colony

Representa uma vila organizada.

É a entidade principal do sistema.

---

## Estrutura

```text
Colony

- id
- centerPosition
- biomeType
- state
- lifecycle
- workers
- buildings
- tasks
```

---

## Campos

### id

Tipo:

```java
UUID
```

Identificador único.

---

### centerPosition

Tipo:

```java
ColonyPos
```

Localização principal da vila.

`BlockPos` constava aqui antes da ADR-005.

O Core não conhece tipos do Minecraft; a conversão acontece em
`fabric.adapter.MinecraftTypeAdapter`.

---

### biomeType

Tipo:

```java
BiomeType
```

MVP:

```text
PLAINS
```

---

### state

Tipo:

```java
ColonyState
```

Valores:

```text
STABLE

PRODUCTION

EXPANSION
```

Descreve **o que a colônia está fazendo**.

---

### lifecycle

Tipo:

```java
ColonyLifecycle
```

Valores:

```text
ACTIVE

DORMANT
```

Descreve **se a colônia está sendo simulada**. Introduzido pela ADR-002.

---

Os dois estados são independentes.

`ACTIVE` e `DORMANT` dependem apenas do carregamento de chunk.

Uma colônia `DORMANT` conserva o `ColonyState` em que parou e retoma
nele ao acordar.

Combinação válida:

```text
lifecycle = DORMANT

state     = EXPANSION
```

Significa: a colônia estava construindo e a construção continua de onde
parou quando o chunk voltar.

---

# Colony Relationships

Uma Colônia possui:

```text
1 Colony

N Workers

N Buildings

N Tasks
```

---

# Worker

Representa um aldeão pertencente à colônia.

---

## Estrutura

```text
Worker

- villagerUUID
- colonyId
- profession
- storageId
- state
- currentTask
```

---

## Campos

### villagerUUID

Tipo:

```java
UUID
```

Referência da entidade Vanilla.

---

### colonyId

Tipo:

```java
UUID
```

Colônia pertencente.

---

### profession

Tipo:

```java
ProfessionType
```

Valores MVP:

```text
LUMBERJACK

MANUFACTURER

FARMER

BUILDER
```

---

### storageId

Tipo:

```java
UUID
```

Baú pessoal associado.

---

### state

Tipo:

```text
WorkerState
```

Estados:

```text
AVAILABLE

BUSY

RETURNING

IDLE
```

---

### currentTask

Tipo:

```java
UUID?
```

Tarefa atual.

Pode ser nulo.

---

# Storage

Representa o baú pessoal do trabalhador.

---

## Estrutura

```text
Storage

- id
- ownerWorker
- position
- status
```

---

## Campos

### id

Tipo:

```java
UUID
```

---

### ownerWorker

Tipo:

```java
UUID
```

Trabalhador proprietário.

---

### position

Tipo:

```java
BlockPos
```

Localização do baú.

---

### status

Tipo:

```text
StorageStatus
```

Valores:

```text
ACTIVE

MISSING
```

---

# Resource

Representa um recurso conhecido pela colônia.

---

## Estrutura

```text
Resource

- itemId
- category
- quantity
- locations
```

---

## Campos

### itemId

Tipo:

```java
Identifier
```

Exemplo:

```text
minecraft:oak_log
```

---

### category

Tipo:

```text
ResourceCategory
```

Valores:

```text
NATURAL

PROCESSED

CONSTRUCTION
```

---

### quantity

Tipo:

```java
int
```

Quantidade conhecida.

---

### locations

Tipo:

```text
List<StorageReference>
```

Locais onde existe.

---

# Task

Representa uma ação que deve ser executada.

---

## Estrutura

```text
Task

- id
- type
- priority
- requiredProfession
- assignedWorker
- status
```

---

## Campos

### type

Tipo:

```text
TaskType
```

MVP:

```text
COLLECT_RESOURCE

CRAFT_ITEM

BUILD_STRUCTURE
```

---

### priority

Tipo:

```java
int
```

Maior valor = maior prioridade.

---

### requiredProfession

Tipo:

```text
ProfessionType
```

---

### assignedWorker

Tipo:

```java
UUID?
```

---

### status

Tipo:

```text
TaskStatus
```

Valores:

```text
AVAILABLE

RESERVED

EXECUTING

COMPLETED

CANCELLED
```

---

# Building

Representa uma construção pertencente à colônia.

---

## Estrutura

```text
Building

- id
- colonyId
- structureType
- position
- rotation
- status
```

---

## Campos

### structureType

Tipo:

```text
StructureType
```

MVP:

```text
PLAINS_SMALL_HOUSE
```

---

### position

Tipo:

```java
BlockPos
```

---

### rotation

Tipo:

```text
Rotation
```

---

### status

Tipo:

```text
BuildingStatus
```

Valores:

```text
PLANNED

BUILDING

COMPLETED
```

---

# ConstructionProject

Representa uma construção antes de existir.

---

## Estrutura

```text
ConstructionProject

- id
- blueprint
- materials
- progress
- status
```

---

# Blueprint

Modelo da estrutura.

---

## Estrutura

```text
Blueprint

- id
- size
- blocks
```

---

# BlueprintBlock

Representa um bloco da construção.

---

## Estrutura

```text
BlueprintBlock

- relativePosition
- blockType
```

---

# ResourceRequirement

Representa materiais necessários.

---

## Estrutura

```text
ResourceRequirement

- resourceId
- requiredAmount
- availableAmount
```

---

# Enums Principais

---

# ProfessionType

```text
LUMBERJACK

MANUFACTURER

FARMER

BUILDER
```

---

# TaskType

```text
COLLECT_RESOURCE

CRAFT_ITEM

BUILD_STRUCTURE
```

---

# WorkerState

```text
AVAILABLE

BUSY

RETURNING

IDLE
```

---

# ColonyState

```text
STABLE

PRODUCTION

EXPANSION
```

---

# Relacionamentos

Modelo completo:

```text
Colony

 |
 +-- Worker

 |      |
 |      +-- Storage

 |
 +-- Task

 |
 +-- Building

 |
 +-- Resource
```

---

# Regras de Integridade

## Worker

Sempre pertence a uma Colony.

---

## Storage

Sempre pertence a um Worker.

---

## Task

Pode existir sem Worker.

Exemplo:

```text
Task AVAILABLE
```

---

## Building

Sempre pertence a uma Colony.

---

## Resource

Pertence à visão da Colony.

---

# Dados Temporários

Não devem ser salvos:

* caminho do aldeão;
* posição atual;
* animação;
* alvo atual;
* comportamento Vanilla.

---

# Dados Persistentes

Devem ser salvos:

* Colony;
* Worker;
* Storage referência;
* Building;
* Task pendente.

---

# Objetivo Final

Criar um modelo simples e extensível onde:

```text
Colony

organiza

↓

Workers

executam

↓

Tasks

consomem

↓

Resources

para criar

↓

Buildings
```

Este modelo deve suportar crescimento futuro sem alterar o núcleo do MVP.
