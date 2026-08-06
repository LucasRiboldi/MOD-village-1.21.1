# Project-State.md

# Village Colony — Project State

**Status:** Documentation Complete / Technical Audit Complete / Implementation Blocked
**Version:** 0.1.0 Planning Phase
**Last Update:** 2026-08-06 — Technical Audit
**Repository:** https://github.com/LucasRiboldi/MOD-village-1.21.1

---

# 1. Purpose

Este documento representa o estado atual do projeto Village Colony.

Ele deve ser atualizado continuamente durante o desenvolvimento.

Sua função é manter uma visão rápida do progresso sem substituir os documentos técnicos.

---

# 2. Project Identity

## Name

```text
Village Colony
```

---

## Target

Minecraft Java Edition:

```text
1.21.1
```

---

## Mod Loader

```text
Fabric
```

---

## Language

```text
Java
```

---

# 3. Current Phase

## Current Stage

```text
Phase 0 — Project Preparation
```

---

## Description

A fase atual consiste na preparação completa da documentação, arquitetura e regras de desenvolvimento antes da geração do código.

---

# 4. Development Status

## Completed

## Project Definition

Status:

```text
DONE
```

Concluído:

* visão do projeto;
* filosofia;
* objetivos;
* limites de escopo.

Documentos:

```text
PROJECT_CONSTITUTION.md

README.md
```

---

# Architecture

Status:

```text
DONE
```

Concluído:

* arquitetura em camadas;
* modelos;
* serviços;
* adaptadores.

Documentos:

```text
Architecture-Foundation.md

Data-Model.md

Class-Architecture.md
```

---

# Systems Design

Status:

```text
DONE
```

Concluído:

* simulação;
* profissões;
* recursos;
* armazenamento;
* construção;
* persistência.

Documentos:

```text
Simulation-Loop.md

Profession-System.md

Resource-System.md

Storage-System.md

Construction-System.md

Save-Data-System.md
```

---

# Development Control

Status:

```text
DONE
```

Concluído:

* regras Claude Code;
* padrões de código;
* fluxo de desenvolvimento.

Arquivos:

```text
claude/

CLAUDE.md

DEVELOPMENT-RULES.md

IMPLEMENTATION-ORDER.md

CODE-STANDARDS.md
```

---

# Technical Documentation

Status:

```text
DONE
```

Concluído:

```text
Fabric-Version.md

Performance-Rules.md

Testing-Strategy.md

Debugging-Strategy.md

Development-Workflow.md
```

---

# 5. Current MVP Status

## MVP Version

```text
Not Started
```

---

## MVP Goal

Criar uma vila Vanilla capaz de:

```text
Detectar vila

↓

Registrar aldeões

↓

Organizar trabalhadores

↓

Coletar recursos

↓

Produzir materiais

↓

Construir expansão
```

---

# 6. Current Implementation Status

## Fabric Project

Status:

```text
NOT STARTED
```

---

## Core Models

Status:

```text
NOT STARTED
```

Planejado:

```text
Colony

Worker

Task

Resource

Storage

Building
```

---

## Persistence

Status:

```text
NOT STARTED
```

Planejado:

```text
ColonySavedData
```

---

## Village Detection

Status:

```text
NOT STARTED
```

Planejado:

```text
VillageScanner
```

---

## Worker System

Status:

```text
NOT STARTED
```

Planejado:

```text
WorkerService

VillagerAdapter
```

---

## Resource System

Status:

```text
NOT STARTED
```

---

## Construction System

Status:

```text
NOT STARTED
```

---

# 7. Next Development Step

## Task

Criar `ADR-002-Chunk-Loading-Strategy.md`.

---

## Reason

A auditoria técnica identificou que criar o projeto Fabric agora significaria
implementar Simulation, Resource e Storage sobre uma decisão não tomada.

Ver §10 — Decision 1.

---

## Objective

Definir o comportamento da colônia quando nenhum jogador está próximo.

---

## Expected Result

```text
Estratégia escolhida

↓

Impacto documentado em Simulation-Loop

↓

Impacto documentado em Resource-System

↓

Impacto documentado em Storage-System
```

---

## Blocked

```text
Criar projeto Fabric

Criar Core Models

Criar ColonySavedData
```

permanecem bloqueados até o Stage 0 ser concluído.

---

# 8. Current Priority Queue

A auditoria técnica identificou cinco bloqueadores que precedem qualquer código.

A fila abaixo reflete essa ordem.

---

## Stage 0 — Decisions (no code)

---

## Priority 0.1

Criar:

```text
ADR-002-Chunk-Loading-Strategy.md
```

Resolver: simulação autônoma quando nenhum jogador está próximo.

Status:

```text
PENDING — BLOCKER
```

---

## Priority 0.2

Criar:

```text
ADR-003-Village-Detection.md
```

Resolver: algoritmo de detecção de vila (cluster de POIs).

Status:

```text
PENDING — BLOCKER
```

---

## Priority 0.3

Criar:

```text
ADR-004-Mixin-Policy.md

Vanilla-Integration.md
```

Resolver: injeção de comportamento no Brain do aldeão.

Status:

```text
PENDING — BLOCKER
```

---

## Priority 0.4

Criar:

```text
ADR-005-Core-Type-Isolation.md
```

Resolver: Core independente de Minecraft.

Status:

```text
PENDING — BLOCKER
```

---

## Priority 0.5

Criar:

```text
ADR-006-Package-Layout.md
```

Resolver: três layouts de pacote conflitantes.

Status:

```text
PENDING — BLOCKER
```

---

## Priority 0.6

Atualizar:

```text
Fabric-Version.md
```

Fixar versões exatas: Java, Loom, Loader, Fabric API, mappings.

Status:

```text
PENDING
```

---

## Priority 0.7

Correções de consistência na documentação.

Status:

```text
PENDING
```

---

## Stage 1 — Foundation (após Stage 0)

---

## Priority 1.1

Criar estrutura Gradle Fabric.

Status:

```text
BLOCKED BY STAGE 0
```

---

## Priority 1.2

Criar entrypoint do mod.

Status:

```text
BLOCKED BY STAGE 0
```

---

## Priority 1.3

Criar pacotes conforme ADR-006.

Status:

```text
BLOCKED BY STAGE 0
```

---

## Priority 1.4

Criar primeiros modelos:

```text
Colony

Worker
```

Status:

```text
BLOCKED BY STAGE 0
```

---

# 9. Known Limitations

Atualmente:

* nenhum código existe;
* nenhum mundo de teste existe;
* nenhuma integração Minecraft existe;
* cinco decisões arquiteturais bloqueiam o início da implementação.

---

# 10. Pending Decisions

Cinco decisões críticas pendentes.

---

## Decision 1 — Chunk Loading

Conflito:

```text
Constituição §3

"a colônia funciona sem jogador próximo"

versus

Save-Data-System

"recursos são lidos dos baús reais"
```

Sem jogador, o chunk está descarregado e o baú não é acessível.

Opções:

* chunk ticket / forceload;
* simulação offline aproximada;
* hibernação da colônia.

Todas possuem custo. Nenhuma foi escolhida.

Impacto:

Define o design de Simulation, Resource e Storage.

---

## Decision 2 — Village Detection

Minecraft não possui objeto `Village`.

Vila é emergente:

```text
POIs (cama, workstation, sino)

+

VillagerEntity Brain memories
```

`locateStructure` encontra a estrutura gerada, não a vila viva.

Impacto:

TASK-009, Phase 4, v0.2.

---

## Decision 3 — Mixin Policy

Aldeões Vanilla não sabem quebrar nem colocar blocos.

O villager 1.21.1 usa Brain/Activity/Schedule, não Goal.

A Fabric API não expõe injeção pública no Brain.

Requer Mixin em `VillagerEntity`.

Nenhum documento menciona Mixin.

Impacto:

Toda a Phase 9 em diante. Compatibilidade com outros mods.

---

## Decision 4 — Core Type Isolation

`CLAUDE.md §6` proíbe Minecraft no Core.

`Data-Model.md` define modelos com:

```text
BlockPos

Identifier

Rotation
```

que são `net.minecraft.*`.

A regra é violada pela própria especificação.

Impacto:

Testabilidade unitária prometida em Testing-Strategy §3.

---

## Decision 5 — Package Layout

Três layouts conflitantes:

```text
README §11

Class-Architecture

Fabric-Implementation-Plan
```

Impacto:

TASK-003.

---

# 11. Architectural Risks

## Risk: Complexidade excessiva

Controle:

Manter MVP pequeno.

---

## Risk: Substituir Vanilla

Controle:

Seguir ADR-001.

---

## Risk: Performance

Controle:

Seguir Performance-Rules.md.

---

## Risk: Chunks descarregados

Controle:

ADR-002 pendente.

---

## Risk: Detecção de vila sem API

Controle:

ADR-003 pendente.

---

## Risk: Mixin no Brain do aldeão

Controle:

ADR-004 pendente.

---

## Risk: Saves quebrados na primeira migração

Sem campo `dataVersion` no NBT.

Controle:

Adicionar no primeiro save implementado.

---

## Risk: Processamento de template subestimado

Templates Vanilla contêm jigsaw blocks, structure void e data blocks.

Não contêm fundação nem terraplanagem.

Conversão `BlockState` → `Item` não é 1:1.

Controle:

Detalhar em Construction-System antes da Phase 11.

---

# 12. Current Development Rules

Sempre:

Antes de implementar:

```text
Ler documentação

↓

Planejar

↓

Executar pequena alteração

↓

Testar
```

---

# 13. Active Documents

Documentos principais:

```text
PROJECT_CONSTITUTION.md

MVP.md

Architecture-Foundation.md

Class-Architecture.md

Development-Roadmap.md
```

---

# 14. Session Resume Template

Ao iniciar uma nova sessão:

Atualizar:

```text
Current Phase:

Completed:

Working On:

Next Step:

Problems:
```

---

# 15. Development Log

## Entry 001

Data:

Initial setup.

Estado:

Toda documentação base criada.

Resultado:

Projeto pronto para iniciar implementação Fabric.

---

## Entry 002

Data:

2026-08-06

Ação:

Auditoria técnica completa dos 24 documentos.

Estado:

Documentação coerente em visão e filosofia.

Encontrado:

* 5 bloqueadores arquiteturais;
* 4 problemas de severidade alta;
* 5 de severidade média;
* 6 de severidade baixa.

Correções aplicadas:

* extensões `.md.txt` renomeadas para `.md`;
* repositório Git remoto registrado.

Resultado:

Implementação bloqueada até conclusão do Stage 0.

O próximo passo NÃO é criar o projeto Fabric.

O próximo passo é ADR-002.

---

# 16. Definition of Project Progress

O projeto avança somente quando:

* código funciona;
* testes passam;
* documentação acompanha.

---

# Final State Rule

O Project-State deve sempre responder:

> "Se um desenvolvedor abrir este projeto hoje, ele sabe exatamente onde estamos e qual é o próximo passo?"
