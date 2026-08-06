# Fabric-Implementation-Plan.md

# Village Colony — Fabric Implementation Plan

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir a estrutura inicial de implementação do mod utilizando:

* Minecraft 1.21.1
* Fabric Loader
* Fabric API
* Java

Este documento define:

* organização do código;
* responsabilidades dos pacotes;
* pontos de integração;
* ordem de implementação.

---

# Princípio Fundamental

A implementação segue a separação:

```text
Minecraft / Fabric

↓

Adapter Layer

↓

Colony Core

↓

Simulation Logic
```

Minecraft fornece o mundo.

A Colônia fornece a lógica.

---

# Estrutura do Projeto

Estrutura inicial:

```text
village-colony/

├── src/main/java/
│
│   └── com/villagecolony/
│
│       ├── VillageColonyMod.java
│       │
│       ├── core/
│       │   ├── colony/
│       │   ├── task/
│       │   ├── profession/
│       │   ├── resource/
│       │   ├── storage/
│       │   └── construction/
│       │
│       ├── fabric/
│       │   ├── entity/
│       │   ├── world/
│       │   ├── event/
│       │   ├── block/
│       │   └── structure/
│       │
│       └── data/
│           └── save/
│
└── src/main/resources/
```

---

# Módulo Core

O Core não conhece:

* Minecraft;
* Fabric;
* entidades;
* blocos;
* inventários.

Ele trabalha apenas com modelos.

---

# Core Packages

## colony

Responsável pela Colônia.

Classes principais:

```text
Colony

ColonyManager

ColonyState
```

Responsabilidades:

* criar colônias;
* atualizar estado;
* gerenciar membros.

---

## task

Sistema de tarefas.

Classes:

```text
Task

TaskManager

TaskStatus
```

Responsabilidades:

* criar tarefas;
* atribuir trabalhadores;
* controlar estados.

---

## profession

Sistema de profissões.

Classes:

```text
Profession

ProfessionRegistry

WorkerRole
```

Responsabilidades:

* registrar capacidades;
* validar tarefas.

---

## resource

Sistema de recursos.

Classes:

```text
Resource

ResourceRegistry

ResourceRequirement
```

Responsabilidades:

* controlar necessidades;
* calcular déficit.

---

## storage

Sistema de armazenamento.

Classes:

```text
Storage

StorageRegistry
```

Responsabilidades:

* registrar baús;
* localizar recursos.

---

## construction

Sistema de construção.

Classes:

```text
ConstructionProject

Building

Blueprint
```

Responsabilidades:

* controlar projetos;
* acompanhar progresso.

---

# Camada Fabric

A camada Fabric conecta o Core ao Minecraft.

---

# Inicialização

Classe:

```java
VillageColonyMod
```

Responsável por:

* iniciar registros;
* carregar dados;
* registrar eventos.

Não deve conter lógica da colônia.

---

# Eventos Fabric Necessários

## Server Tick Event

Uso:

Atualização da simulação.

Fluxo:

```text
Server Tick

↓

Colony Tick

↓

Simulation Loop
```

---

## Server World Load

Uso:

Carregar dados persistentes.

Fluxo:

```text
World Loaded

↓

Load Colony Data

↓

Initialize Managers
```

---

## Server World Save

Uso:

Salvar estado.

Fluxo:

```text
World Save

↓

Serialize Colony Data
```

---

## Entity Events

Uso:

Monitorar aldeões.

Exemplos:

* nascimento;
* morte;
* carregamento.

---

# Integração com Aldeões

A camada Fabric será responsável por:

* localizar VillagerEntity;
* identificar profissão Vanilla;
* associar UUID;
* executar tarefas.

O Core nunca acessa VillagerEntity.

---

# Exemplo de Separação

Correto:

```text
FabricVillagerAdapter

↓

Worker

↓

Task
```

Incorreto:

```text
Colony

↓

VillagerEntity
```

---

# Persistência

Implementação inicial:

Minecraft Persistent State.

Responsável:

```text
data/save/
```

Dados:

* colônias;
* trabalhadores;
* construções;
* tarefas.

---

# Estrutura de Dados Inicial

Exemplo:

```text
ColonySavedData

├── Colonies
│
├── Workers
│
├── Buildings
│
└── Tasks
```

---

# Primeiras Classes a Criar

Ordem recomendada:

## 1. Mod Initialization

```text
VillageColonyMod
```

Objetivo:

Projeto Fabric inicia corretamente.

---

## 2. Colony Model

```text
Colony
```

Objetivo:

Criar entidade principal.

---

## 3. Colony Manager

```text
ColonyManager
```

Objetivo:

Gerenciar colônias existentes.

---

## 4. Villager Scanner

```text
VillagerScanner
```

Objetivo:

Encontrar aldeões.

---

## 5. Simulation Loop

```text
ColonyTickHandler
```

Objetivo:

Executar ciclos.

---

## 6. Resource Registry

```text
ResourceManager
```

Objetivo:

Contar recursos.

---

## 7. Task System

```text
TaskManager
```

Objetivo:

Criar e controlar tarefas.

---

# Ordem de Desenvolvimento do MVP

## Fase 1 — Fundação

Implementar:

* projeto Fabric;
* carregamento;
* persistência;
* Colony.

Resultado:

```text
Mod inicia

↓

Colony criada
```

---

## Fase 2 — Observação

Implementar:

* detectar vila;
* detectar aldeões;
* registrar profissões.

Resultado:

```text
Colony conhece seus habitantes
```

---

## Fase 3 — Recursos

Implementar:

* detectar baús;
* registrar recursos.

Resultado:

```text
Colony conhece seus estoques
```

---

## Fase 4 — Tarefas

Implementar:

* criar tarefas;
* atribuir aldeões.

Resultado:

```text
Aldeões recebem trabalhos
```

---

## Fase 5 — Construção

Implementar:

* Blueprint;
* materiais;
* Builder.

Resultado:

```text
Nova casa construída
```

---

# Regras de Código

## Não criar classes gigantes.

Evitar:

```text
VillageColonyManager.java
```

com milhares de linhas.

---

## Uma responsabilidade por classe.

---

## Core independente.

---

## Fabric somente adapta.

---

## Documentar decisões importantes com ADR.

---

# Critério de Sucesso

A primeira versão técnica estará correta quando:

```text
Minecraft inicia

↓

Fabric carrega

↓

Uma vila é encontrada

↓

Uma Colony é criada

↓

A simulação executa

↓

Dados sobrevivem ao salvar mundo
```

Esse é o primeiro marco antes de adicionar qualquer comportamento de trabalho.
