# Architecture-Foundation.md

# Village Colony — Architecture Foundation

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir a arquitetura base do projeto Village Colony.

Este documento estabelece como os sistemas serão organizados, quais responsabilidades pertencem a cada módulo e como os componentes se comunicam.

A arquitetura deve permitir crescimento gradual sem exigir grandes alterações no núcleo do projeto.

---

# Princípio Arquitetural Principal

A arquitetura segue o conceito:

```
A Colônia decide.

As Tarefas organizam.

Os Aldeões executam.
```

Nenhum aldeão possui inteligência estratégica.

Toda decisão coletiva pertence à Colônia.

---

# Visão Geral

A arquitetura será dividida em camadas.

```
Minecraft / Fabric

↓

Integration Layer

↓

Colony Core

↓

Simulation Systems

↓

Villager Execution
```

---

# Estrutura de Módulos

## 1. Core

Responsável pela lógica independente do Minecraft.

Não conhece:

* blocos;
* entidades;
* mundo;
* Fabric.

Contém:

```
core/

├── colony
├── task
├── profession
├── resource
├── construction
└── storage
```

---

# Colony Module

Responsabilidade:

Representar uma vila organizada.

Entidade principal:

```
Colony
```

Responsável por:

* manter estado da vila;
* registrar aldeões;
* controlar profissões;
* controlar construções;
* gerar demandas.

Exemplo:

```
Colony

- villagers
- professions
- buildings
- resources
- tasks
```

---

# Task Module

Representa ações executáveis.

Toda atividade da colônia deve existir como uma tarefa.

Exemplos:

```
Collect Wood

Craft Planks

Build House
```

Estados:

```
AVAILABLE

↓

RESERVED

↓

EXECUTING

↓

COMPLETED

↓

CANCELLED
```

Responsabilidades:

* criar tarefas;
* controlar estado;
* atribuir executor;
* finalizar execução.

---

# Profession Module

Define capacidades dos aldeões.

Uma profissão informa:

* quais tarefas pode executar;
* quais ferramentas utiliza;
* quais recursos produz.

Não contém lógica de decisão.

Exemplo:

```
Lumberjack

Can:
- collect wood

Requires:
- wooden axe
```

---

# Resource Module

Controla os materiais conhecidos pela colônia.

Responsável por:

* identificar recursos;
* contar disponibilidade;
* registrar consumo;
* validar necessidades.

Categorias:

## Natural Resources

Exemplo:

```
Oak Log
Cobblestone
```

## Processed Resources

Exemplo:

```
Oak Planks
```

---

# Storage Module

Representa os locais onde recursos são armazenados.

No MVP:

Cada trabalhador possui um baú de estoque.

Exemplo:

```
Lumberjack

↓

Personal Storage Chest

↓

Oak Logs
```

O armazenamento possui:

* localização;
* proprietário;
* itens armazenados.

---

# Construction Module

Responsável pelos projetos de expansão.

Não decide construir.

Recebe projetos aprovados pela Colônia.

Responsabilidades:

* carregar estrutura Vanilla;
* calcular materiais;
* verificar recursos;
* criar tarefas de construção;
* registrar construção concluída.

---

# Fabric Integration Layer

Responsável pela comunicação com Minecraft.

Local:

```
fabric/
```

Contém:

```
fabric/

├── entities
├── events
├── world
├── blocks
├── structures
└── networking
```

---

# Entity Integration

Responsável por conectar aldeões Vanilla ao sistema.

Funções:

* detectar aldeões;
* identificar profissões;
* associar aldeão à Colônia;
* atribuir tarefas.

O aldeão continua utilizando sua IA Vanilla.

---

# World Integration

Responsável por:

* detectar vilas;
* acessar estruturas;
* verificar terreno;
* manipular blocos durante construção.

---

# Structure System

Usa estruturas Vanilla como fonte de construção.

Fluxo:

```
Minecraft Structure

↓

Construction Blueprint

↓

Material List

↓

Build Task

↓

Completed Building
```

---

# Building Model

Uma construção possui:

```
Building

- id
- structureType
- position
- rotation
- ownerColony
- state
```

Estados:

```
PLANNED

↓

BUILDING

↓

COMPLETED
```

---

# Infrastructure Registry

Toda construção registrada possui proteção.

Categorias:

```
Original Village

ou

Colony Infrastructure
```

Ambas são protegidas.

---

# Simulation Loop

A simulação funciona em ciclos.

```
Tick

↓

Update Colony

↓

Check Resources

↓

Generate Demands

↓

Create Tasks

↓

Assign Villagers

↓

Execute

↓

Save State
```

---

# Fluxo de Construção

```
Colony detects need

↓

Create Project

↓

Load Vanilla Structure

↓

Calculate Materials

↓

Search Worker Storage

↓

Create Build Task

↓

Builder Executes

↓

Register Infrastructure
```

---

# Comunicação Entre Sistemas

Os módulos nunca acessam diretamente outros módulos.

Comunicação deve ocorrer através de:

* interfaces;
* eventos;
* serviços.

Exemplo:

Correto:

```
Construction

↓

Task Request

↓

Task Manager
```

Incorreto:

```
Construction

↓

Modificar diretamente Villager
```

---

# Persistência

Toda informação criada pelo mod deve sobreviver ao fechamento do mundo.

Será armazenado:

## Colony

* localização;
* membros;
* estado.

## Villagers

* profissão atribuída;
* tarefas atuais.

## Buildings

* estruturas concluídas;
* proteção.

## Tasks

* tarefas pendentes.

---

# Regras de Desenvolvimento

Toda nova funcionalidade deve responder:

1. Qual módulo é responsável?
2. Pode funcionar sem Minecraft?
3. Existe uma interface definida?
4. Afeta sistemas existentes?
5. Precisa de um ADR?

---

# Limites do MVP

Não implementar:

* sistema de distritos;
* economia;
* múltiplas colônias;
* profissões avançadas;
* mineração complexa;
* IA personalizada.

---

# Objetivo Final da Arquitetura

Criar uma base onde uma pequena vila possa evoluir gradualmente para uma colônia complexa sem substituir as regras fundamentais do Minecraft.

A arquitetura deve permitir crescimento contínuo mantendo:

* simplicidade;
* compatibilidade;
* desempenho;
* manutenção fácil.
