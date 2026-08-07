# Class-Architecture.md

# Village Colony — Class Architecture

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir a arquitetura das classes Java do projeto Village Colony.

Este documento determina:

* classes principais;
* responsabilidades;
* dependências;
* fluxo de comunicação.

---

# Princípio Arquitetural

Nenhuma classe deve possuir responsabilidades de camadas diferentes.

Exemplo:

Errado:

```java
Colony.java

- detectar aldeões
- colocar blocos
- salvar mundo
- criar tarefas
```

Correto:

```text
Colony

↓

ColonyManager

↓

Fabric Adapter
```

---

# Estrutura Geral

A fonte única de verdade sobre o layout de pacotes é:

```text
docs/decisions/ADR-006-Package-Layout.md
```

Este documento não repete a estrutura.

---

Duas mudanças da ADR-006 afetam o restante deste arquivo:

```text
Agrupamento por domínio dentro da camada,

não por camada.
```

```text
A camada "manager" deixou de existir.

Service contém a lógica e o registro em memória.

data/save contém apenas serialização.
```

Onde este documento ainda descrever `ColonyManager` como camada,
vale a ADR-006 §5.

---

# Camada Model

Responsável apenas por dados.

Não possui lógica Minecraft.

---

# Colony.java

Local:

```text
core/model/colony
```

Responsabilidade:

Representar uma colônia.

---

Dados:

```java
UUID id;

BlockPos center;

ColonyState state;

List<Worker> workers;

List<Building> buildings;
```

---

Não faz:

* detectar vila;
* criar tarefas;
* salvar dados.

---

# Worker.java

Local:

```text
core/model/worker
```

Representa um trabalhador.

---

Dados:

```java
UUID villagerUUID;

ProfessionType profession;

WorkerState state;

UUID storageId;

UUID currentTask;
```

---

Não faz:

* movimentação;
* coleta;
* construção.

---

# Storage.java

Local:

```text
core/model/storage
```

Representa um armazenamento.

---

Dados:

```java
UUID id;

BlockPos position;

UUID ownerWorker;
```

---

Não faz:

* abrir baú;
* retirar item.

---

# Resource.java

Local:

```text
core/model/resource
```

Representa um recurso.

---

Dados:

```java
Identifier itemId;

ResourceCategory category;

int quantity;
```

---

# Task.java

Local:

```text
core/model/task
```

Representa uma tarefa.

---

Dados:

```java
UUID id;

TaskType type;

TaskStatus status;

ProfessionType requiredProfession;

UUID assignedWorker;
```

---

# Building.java

Local:

```text
core/model/construction
```

Representa uma construção concluída.

---

Dados:

```java
UUID id;

StructureType type;

BlockPos position;

BuildingStatus status;
```

---

# ConstructionProject.java

Representa uma construção planejada.

---

Dados:

```java
UUID id;

Blueprint blueprint;

List<ResourceRequirement> materials;

ConstructionStatus status;
```

---

# Camada Service

Responsável pelo comportamento.

---

# ColonyService.java

Responsabilidade:

Controlar operações da colônia.

Funções:

```java
createColony()

updateColony()

addWorker()

removeWorker()
```

---

Não conhece:

* VillagerEntity;
* Minecraft Server.

---

# SimulationService.java

Responsabilidade:

Executar o ciclo da colônia.

Fluxo:

```text
tick()

↓

observe

↓

evaluate

↓

generate tasks
```

---

# TaskService.java

Responsabilidade:

Gerenciar tarefas.

Funções:

```java
createTask()

assignTask()

completeTask()

cancelTask()
```

---

# ResourceService.java

Responsabilidade:

Gerenciar recursos.

Funções:

```java
scanResources()

calculateNeeds()

checkAvailability()
```

---

# StorageService.java

Responsabilidade:

Controlar registros de armazenamento.

Funções:

```java
registerStorage()

findResources()

reserveItems()
```

---

# ProfessionService.java

Responsabilidade:

Controlar profissões.

Funções:

```java
assignProfession()

checkCapability()
```

---

# ConstructionService.java

Responsabilidade:

Controlar construções.

Funções:

```java
createProject()

calculateMaterials()

completeBuilding()
```

---

# WorkerService.java

Responsabilidade:

Relacionar aldeões reais aos trabalhadores.

Funções:

```java
registerWorker()

updateWorker()

assignTask()
```

---

# Camada Manager

Responsável por acesso global aos sistemas.

---

# ColonyManager.java

Responsabilidade:

Ponto principal de acesso às colônias.

Exemplo:

```java
getColony(UUID id)

getColonyAt(BlockPos pos)
```

---

# TaskManager.java

Mantém tarefas ativas.

---

# ResourceManager.java

Mantém registros de recursos.

---

# StorageManager.java

Mantém registros de baús.

---

# Data Layer

Responsável por persistência.

---

# ColonySavedData.java

Responsabilidade:

Salvar e carregar dados do mundo.

Salva:

```text
Colonies

Workers

Buildings

Tasks
```

---

# Fabric Layer

Responsável pela comunicação com Minecraft.

---

# VillageScanner.java

Local:

```text
fabric/integration
```

Responsabilidade:

Encontrar vilas Vanilla.

Entrada:

```java
ServerWorld
```

Saída:

```java
Colony
```

---

# VillagerAdapter.java

Responsabilidade:

Converter aldeão Minecraft em Worker.

Exemplo:

```text
VillagerEntity

↓

Worker
```

---

# ChestAdapter.java

Responsabilidade:

Ler baús reais.

Entrada:

```java
BlockEntity
```

Saída:

```text
Storage data
```

---

# BlockPlacementAdapter.java

Responsabilidade:

Construção no mundo.

Funções:

```java
placeBlock()

removeNaturalBlock()
```

---

# Event Handlers

---

# ServerTickHandler.java

Executa:

```text
Minecraft Tick

↓

SimulationService
```

---

# WorldLoadHandler.java

Executa:

```text
World Load

↓

Load Saved Data
```

---

# WorldSaveHandler.java

Executa:

```text
World Save

↓

Save Colony Data
```

---

# Dependências

Fluxo permitido:

```text
Fabric

↓

Services

↓

Models
```

---

Fluxo proibido:

```text
Model

↓

Fabric
```

---

# Exemplo de Execução

## Coleta de Madeira

```text
ServerTickHandler

↓

SimulationService

↓

TaskService

↓

WorkerService

↓

VillagerAdapter

↓

Minecraft World
```

---

## Construção

```text
ConstructionService

↓

TaskService

↓

WorkerService

↓

Builder Worker

↓

BlockPlacementAdapter
```

---

# Classes Futuras

Não criar agora:

```text
TransportService

MarketService

DistrictService

DefenseService

DiplomacyService
```

---

# Primeira Implementação Real

A ordem das primeiras classes será:

## 1

```text
VillageColonyMod
```

---

## 2

```text
Colony
```

---

## 3

```text
ColonyManager
```

---

## 4

```text
ColonySavedData
```

---

## 5

```text
SimulationService
```

---

## 6

```text
VillagerScanner
```

---

# Objetivo Final

A arquitetura deve permitir que:

```text
Minecraft

↓

Fabric Adapter

↓

Services

↓

Models

↓

Colony Simulation
```

evolua de uma pequena vila automática para uma colônia complexa sem reescrever o núcleo.
