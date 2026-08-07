# Project-State.md

# Village Colony — Project State

**Status:** Documentation Complete / Stage 0 Complete / Implementation Unblocked
**Version:** 0.1.0 Planning Phase
**Last Update:** 2026-08-06 — Stage 0 concluído
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

Vanilla-Integration.md
```

---

# Architecture Decisions

Status:

```text
ACCEPTED
```

Documentos:

```text
docs/decisions/

ADR-001-Core-Principles.md          (Accepted)

ADR-002-Chunk-Loading-Strategy.md   (Accepted)

ADR-003-Village-Detection.md        (Accepted)

ADR-004-Mixin-Policy.md             (Accepted)

ADR-005-Core-Type-Isolation.md      (Accepted)

ADR-006-Package-Layout.md           (Accepted)
```

As seis ADRs foram aceitas em 2026-08-06.

Nenhuma decisão de arquitetura permanece em aberto.

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
DONE — TASK-001 (2026-08-06)
```

Criado:

```text
build.gradle

settings.gradle

gradle.properties

gradle/wrapper/          (Gradle 9.6.1)

src/main/java/com/villagecolony/VillageColonyMod.java

src/main/resources/fabric.mod.json
```

Verificado:

```text
./gradlew build  → BUILD SUCCESSFUL

build/libs/village-colony-0.1.0.jar

fabric.mod.json válido, ${version} expandido para 0.1.0
```

```text
./gradlew runClient  → jogo abriu

FabricLoader: Loading 56 mods

  - villagecolony 0.1.0

[villagecolony] [Village Colony] Mod initialized

Nenhuma exceção no log.
```

Requisito de ambiente descoberto:

```text
Loom 1.17.18 exige JVM 21 para rodar o Gradle,

não apenas para a toolchain.

JDK instalado: Temurin 21.0.12 em ~/.jdks
```

---

## Mod Identity

Status:

```text
DONE — TASK-002 (2026-08-06)
```

```text
id           villagecolony

name         Village Colony

version      0.1.0

license      MIT

environment  *
```

Verificado: `FabricLoader` lista `villagecolony 0.1.0`.

Pendente: `icon` (decisão de arte, não técnica).

---

## Package Structure

Status:

```text
DONE — TASK-003 (2026-08-06)
```

Árvore criada conforme `ADR-006 §3`.

28 pacotes, cada um com `package-info.java` documentando sua função.

Nota: `package-info.java` sem anotações não gera `.class`.

Os pacotes existem no repositório e no source jar, não no jar remapado.

Isso é esperado — eles passam a existir no jar quando receberem código.

---

## Mod Entry Point

Status:

```text
DONE — TASK-004 (2026-08-06)
```

```text
VillageColonyMod

  MOD_ID, LOGGER compartilhado

  chama ServerLifecycleHandler.register()

fabric/event/ServerLifecycleHandler

  SERVER_STARTED

  SERVER_STOPPING
```

`VillageColonyMod` permanece sem lógica, conforme
`Initial-Setup-Checklist.md §7`.

O registro de cada evento vive em `fabric/event`, para que adicionar
um evento não signifique alterar a classe principal.

---

Verificado:

```text
build passa

runClient carrega o mod, registro não lança
```

---

Verificado em servidor real (2026-08-06):

```text
Loaded 0 colonies    SERVER_STARTED

Saved 0 colonies     SERVER_STOPPING
```

O EULA foi aceito pelo autor no ambiente de desenvolvimento.

`run/` não é versionado; cada máquina precisa aceitar o seu.

---

## Core Models

Status:

```text
IN PROGRESS — Colony feito (TASK-005, 2026-08-06)
```

Feito:

```text
core/type/ColonyPos

core/colony/model/Colony

core/colony/model/ColonyState

core/colony/model/ColonyLifecycle

core/colony/service/ColonyService
```

Verificado:

```text
35 unit tests passando

Core sem import de net.minecraft (ADR-006 §6)

Nenhum domínio do core importa outro domínio
```

Planejado:

```text
Worker

Task

Resource

Storage

Building
```

Tipos da ADR-005 ainda não criados, por falta de uso:

```text
ResourceId

ColonyRotation
```

---

## Persistence

Status:

```text
DONE — TASK-007 (2026-08-06)
```

Feito:

```text
data/save/ColonySavedData      PersistentState do Overworld

VillageColonyMod.COLONIES      registro global (ADR-006 §5)

ServerLifecycleHandler         carrega e grava
```

Gravado:

```text
id, centerX/Y/Z, state
```

Não gravado:

```text
lifecycle       derivado do chunk, volta sempre DORMANT

villageType     campo não existe no modelo Colony

creationTime    campo não existe no modelo Colony
```

---

Verificado:

```text
43 tests passando (8 de serialização)

Round-trip NBT: id, posição e state sobrevivem

Coordenadas negativas sobrevivem

Toda colônia volta DORMANT

state desconhecido cai para STABLE, não lança

entrada sem id é ignorada, não lança
```

Verificado em servidor real:

```text
./gradlew runServer

  Loaded 0 colonies

  Saved 0 colonies

run/world/data/villagecolony_colonies.dat  gravado
```

---

Não verificado:

```text
Uma colônia com dados atravessando

fechar e reabrir o mundo de verdade.
```

Motivo: nada cria colônia ainda. A detecção é TASK-009.

O que resta sem cobertura é apenas a costura:

```text
ColonyService → ColonySavedData → disco → ColonyService
```

As duas pontas estão testadas. O trecho do meio é o que TASK-008 fecha.

---

## Village Detection

Status:

```text
DONE — TASK-009 (2026-08-06)
```

Feito:

```text
core/colony/model/VillageCandidate

core/colony/service/VillageDetector    cluster + validação + centro

fabric/adapter/MinecraftTypeAdapter    BlockPos <-> ColonyPos

fabric/integration/VillageScanner      POI, aldeões, bioma
```

A lógica pura ficou no Core; o scanner apenas lê o mundo e converte.

---

Verificado:

```text
59 tests passando (16 de detecção)

transitividade do cluster

fronteira exata de 32 blocos

sino tem prioridade sobre a média

coordenadas extremas não estouram

constantes conferem com ADR-003 §8
```

---

---

## Colony Creation

Status:

```text
DONE — TASK-010 (2026-08-06)
```

Feito:

```text
Colony.setCenter               centro móvel, ADR-003 §4

ColonyService.adopt            anti-duplicata, ADR-003 §6

fabric/event/VillageDetectionHandler   os dois gatilhos
```

Gatilhos, conforme ADR-003 §3:

```text
CHUNK_LOAD         só se o chunk contiver POI de cama

END_SERVER_TICK    a cada 600 ticks, em torno dos jogadores
```

A checagem barata (`getInChunk`) vem antes da cara (`getInCircle`):
a maioria dos chunks carregados é descartada sem custo.

---

Verificado:

```text
68 tests passando (9 de adoção)

anti-duplicata em ambos os lados da fronteira de 64

centro move, UUID permanece
```

---

Verificado em jogo (2026-08-06, instalação TLauncher):

```text
Loaded 0 colonies

Colony created at ColonyPos[x=1109, y=64, z=730] with 3 beds

Saved 1 colonies
```

Detecção, adoção e gravação funcionam sobre uma vila plains real.

---

---

## TASK-008 — Testar Carregamento

Status:

```text
DONE — 2026-08-06
```

Segunda sessão, mesmo mundo:

```text
Loaded 1 colonies

(4min30s dentro da vila, ~9 ciclos de detecção)

Saved 1 colonies
```

O round-trip completo está fechado:

```text
detectar → salvar → fechar → abrir → colônia permanece
```

Anti-duplicata confirmado fora do teste: o jogador permaneceu na vila
por nove ciclos e a contagem não subiu.

Nenhum erro no log.

---

## Bug encontrado pelo teste em jogo

O gatilho de `CHUNK_LOAD` nunca encontrava vila.

Causa:

```text
ChunkPos.getStartPos()  →  BlockPos(startX, 0, startZ)

getInCircle             →  distância em três dimensões
```

Partindo de y=0, uma cama em y=64 já consome os 64 blocos de raio
antes de qualquer deslocamento horizontal.

A detecção observada veio do ciclo de 600 ticks, ancorado no jogador —
esse sempre esteve na altura certa.

Correção: ancorar no POI de cama encontrado pelo `getInChunk`, que já
era consultado ali.

Nenhum teste unitário pegaria isto: o defeito está na fronteira com o
Minecraft, não na lógica do Core.

---

## Cegueira de log — corrigida

A segunda sessão não conseguiu confirmar a correção acima, porque o mod
só logava **criação** de colônia.

Uma vila já conhecida era reavaliada em silêncio, e o log não distinguia:

```text
detecção rodou e a vila já era conhecida

detecção nunca rodou
```

Foi exatamente essa cegueira que permitiu ao gatilho de chunk ficar
quebrado sem ninguém perceber.

Correção: logar também quando o centro se move.

Reavaliação que não muda nada segue silenciosa, então não vira spam por
ciclo — jogador parado não gera linha.

---

## Gatilho de CHUNK_LOAD — confirmado

Sessão de 23:36:

```text
23:36:31  Loaded 1 colonies

23:36:33  Colony ... moved ...
```

Dois segundos. O ciclo de 600 ticks levaria trinta.

A correção funciona.

---

## Lacuna da ADR-003 — centro oscilante

O mesmo log revelou o centro alternando entre duas posições a 29
blocos uma da outra:

```text
1096,68,742 → 1109,64,730   12 camas

1109,64,730 → 1080,64,733    3 camas

1080,64,733 → 1109,64,730   12 camas

1109,64,730 → 1080,64,733    3 camas
```

Mesma colônia, mesmo UUID — a identificação estava certa.

---

Causa:

A ADR-003 §4 define o centro como a média das camas do cluster, e a
§6 manda atualizar a colônia existente. Mas o §3 limita a coleta a 64
blocos.

Uma vila é maior que 64 blocos. Logo **nenhuma detecção enxerga a vila
inteira**, e cada gatilho produz um cluster diferente.

Como estava implementado, a última detecção sempre vencia: um gatilho
de borda que via 3 de 12 camas arrastava o centro.

---

Correção adotada:

```text
Colony.observedBeds

  quantas camas a melhor observação já vista continha
```

```text
Colony.observe(center, beds)

  move o centro apenas se beds >= observedBeds
```

Empate move, porque a vila pode mudar de lugar mantendo o número de
camas.

O campo é persistido; save antigo lê 0 e autocorrige na primeira
detecção da sessão.

---

```text
PENDENTE: emendar ADR-003 registrando que a completude da observação

          decide a autoridade sobre o centro.
```

---

Verificado em jogo (2026-08-06, 23:50, jar corrigido):

```text
created at 885,920   12 camas

moved to  870,923    13 camas

moved to  878,922    15 camas

moved to  876,919    21 camas
```

Contagem monotonicamente crescente. O centro converge em vez de
oscilar.

Comparar com a sessão anterior, mesmo cenário:

```text
12 → 3 → 12 → 3 → 3
```

Uma colônia, um UUID, estável nas quatro observações.

---

---

## 2026-08-07 — Correções de revisão

Defeito latente corrigido:

```text
setLifecycle nunca era chamado em produção.
```

Toda colônia lida do save ficava `DORMANT` para sempre, mesmo sendo
observada pela detecção a cada ciclo.

Como a ADR-002 manda o loop rodar só para colônias `ACTIVE`, a
simulação da Fase 4 ignoraria silenciosamente toda colônia persistida.

Correção:

```text
adopt()            observar acorda a colônia

updateLifecycles() varredura por ciclo, via shouldTick(ChunkPos)
```

Sem a varredura, o oposto ocorreria: colônia visitada uma vez ficaria
`ACTIVE` a sessão inteira, a milhares de blocos do jogador.

---

Lacuna deixada em aberto, deliberadamente:

```text
ColonyState.ABANDONED existe e nada o atribui.
```

Ver ADR-003 §10, Emenda 1. Exige o scanner reportar clusters
reprovados, o que é tarefa própria e não cabia numa revisão.

---

Emendas de ADR-003 redigidas: três, em `docs/decisions/ADR-003 §10`.

---

Nota de método: trocar o jar com o jogo aberto não testa nada.

O Minecraft carrega mods na inicialização da JVM. Sair ao menu e
reentrar no mundo reusa o código em memória.

Duas sessões foram desperdiçadas assim antes de perceber.

Este é o segundo desvio de ADR-003 registrado aqui. O primeiro é
`DORMANT → ABANDONED`.

---

## Observação sobre a instrumentação

A cegueira de log foi corrigida na sessão anterior, e o defeito
apareceu na sessão seguinte.

Sem a linha `moved`, o centro estaria oscilando em silêncio.

---

## Limitação do console do runServer

Comandos vanilla que dependem de argumento de registro ou produzem
texto rico falham no console deste ambiente:

```text
seed                  → An unexpected error occurred

locate structure ...  → An unexpected error occurred

execute if biome ...  → An unexpected error occurred
```

Funcionam normalmente:

```text
say    list    stop    forceload
```

Nenhum stack trace é registrado no log.

Isso **não passa pelo código do mod** — `seed` e `locate` são vanilla
puro. É limitação do ambiente, não defeito do Village Colony.

Consequência: não foi possível localizar uma vila plains nem sondar
bioma por comando, que era o roteiro planejado para TASK-008.

---

## Caminho alternativo de teste

O jar foi instalado na instalação real do autor:

```text
C:\Users\lucas\AppData\Roaming\.minecraft\mods
```

Perfil `Fabric 1.21.1` existe.

**Falta a Fabric API nessa instalação** — sem ela o mod não carrega,
porque `fabric.mod.json` a declara como dependência.

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

```text
TASK-008 — Testar Carregamento
```

Fase 2, adiada até a detecção existir.

---

## Reason

Fase 3 concluída em 2026-08-06.

A detecção cria colônias. Agora há o que salvar.

---

## Objective — TASK-008

O round-trip completo, que era a única costura de persistência sem
cobertura:

```text
detectar → salvar → fechar → abrir → colônia permanece
```

Roteiro sem interface gráfica:

```text
locate structure minecraft:village_plains

forceload add <x> <z>

conferir "Colony created"

stop

reabrir e conferir "Loaded 1 colonies"
```

---

## Depois

Fase 4 — Sistema de Trabalhadores, a partir de TASK-011.

---

## Pendência de decisão do autor

```text
Ícone do mod
```

---

## Emendas de ADR — resolvidas

```text
ADR-003 §10 — três emendas registradas
```

Nenhum desvio de ADR permanece sem documento.

---

## Antes disso — histórico

```text
TASK-007 — Criar Colony Saved Data
```

Fase 2 — Persistência.

---

## Reason

Fase 1 concluída em 2026-08-06.

O registro de colônias existe em memória e está coberto por testes.

Nada sobrevive a fechar o mundo.

---

## Objective — TASK-007

```text
data/save/ColonySavedData
```

Salvar e recarregar:

```text
id

posição

state

lifecycle
```

---

## Restrições

```text
ADR-006 §5   data/save contém apenas serialização,

             nunca lógica de domínio

ADR-002      o estado salvo deve bastar para retomar

             sem perda

ADR-005      ColonyPos é convertido na fronteira,

             não gravado como BlockPos
```

Ao recarregar, usar `ColonyService.register()`, que já rejeita
id duplicado.

---

## Ponto de integração

`ServerLifecycleHandler` já tem os dois ganchos onde isso encaixa:

```text
SERVER_STARTED   → carregar

SERVER_STOPPING  → garantir gravação
```

Hoje eles apenas logam.

---

## Pendência de decisão do autor

```text
Ícone do mod
```

`fabric.mod.json` não declara `icon`.

Escolher a arte é decisão de identidade visual, não técnica.

Sem `icon` o Fabric usa o ícone padrão. Nada quebra.

---

## Ambiente de build

O Gradle precisa rodar sobre JVM 21.

Loom 1.17.18 exige isso para o próprio plugin, não apenas
para a toolchain.

```text
JAVA_HOME=~/.jdks/jdk-21.0.12+8
```

Instalado em 2026-08-06.

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
DONE — Accepted 2026-08-06
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
DONE — Accepted 2026-08-06
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
DONE — Accepted 2026-08-06
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
DONE — Accepted 2026-08-06
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
DONE — Accepted 2026-08-06
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
DONE — 2026-08-06
```

Matriz fixada em `Fabric-Version.md §5.1`.

Validação prática ocorre na TASK-001.

---

## Priority 0.7

Correções de consistência na documentação.

Status:

```text
DONE — 2026-08-06
```

Corrigido:

```text
PROJECT_CONSTITUTION.md §3

  alinhado com ADR-002

  autonomia = independência de comandos do jogador

  não de chunks carregados
```

Escopo: apenas a contradição §3 vs ADR-002, identificada na auditoria.

Nenhuma outra inconsistência foi auditada nesta passagem.

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

## Entry 003

Data:

2026-08-06

Ação:

Redação das decisões do Stage 0.

Criado:

```text
ADR-002-Chunk-Loading-Strategy.md

ADR-003-Village-Detection.md

ADR-004-Mixin-Policy.md

ADR-005-Core-Type-Isolation.md

ADR-006-Package-Layout.md

docs/technical/Vanilla-Integration.md

LICENSE

.gitignore
```

Atualizado:

```text
README.md          — licença MIT

Fabric-Version.md  — Java 21, Yarn, Version Matrix
```

Estado:

Todas as ADRs em `Proposed`.

Nenhuma aceita.

Resultado:

Implementação permanece bloqueada.

Aguardando aprovação.

---

## 2026-08-06 — Stage 0 concluído

Aceito:

```text
ADR-002  Hibernação com estado persistente e retomada

ADR-003  Detecção por cluster de POIs de cama ocupados

ADR-004  Mixin com superfície mínima e declarada

ADR-005  Tipos de valor próprios no Core (records imutáveis)

ADR-006  Domínio dentro da camada
```

Atualizado:

```text
PROJECT_CONSTITUTION.md   §3 alinhado com ADR-002

Fabric-Version.md         Version Matrix fixada
```

Versões fixadas:

```text
Gradle 9.6.1

Loom 1.17.18

Yarn 1.21.1+build.3

Loader 0.19.3

Fabric API 0.116.15+1.21.1
```

Não verificado:

```text
A matriz não foi executada.

Nenhum build rodou.

A validação acontece na TASK-001.
```

Resultado:

```text
Implementação desbloqueada.

Próximo passo: TASK-001.
```

---

## 2026-08-06 — TASK-001 concluída

Criado:

```text
build.gradle

settings.gradle

gradle.properties

gradle/wrapper/      (Gradle 9.6.1)

gradlew / gradlew.bat

.gitattributes

src/main/java/com/villagecolony/VillageColonyMod.java

src/main/resources/fabric.mod.json
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → mod carregado, sem exceções
```

Descoberto:

```text
Loom 1.17.18 exige JVM 21 para rodar o Gradle.

O foojay-resolver cobre a toolchain, não o JVM do Gradle.

Temurin 21.0.12 instalado em ~/.jdks
```

Corrigido:

```text
Initial-Setup-Checklist.md §6

  layout antigo substituído por referência à ADR-006
```

Resultado:

```text
Version Matrix validada na prática.

Nenhum valor precisou de correção.
```

---

## 2026-08-06 — TASK-002 e TASK-003 concluídas

TASK-002 — Identidade do mod:

```text
fabric.mod.json completo

description, contact.homepage, sources, issues

license MIT
```

TASK-003 — Estrutura de pacotes:

```text
28 pacotes conforme ADR-006 §3

cada um com package-info.java
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → villagecolony 0.1.0 na lista de mods

                       sem exceções
```

Corrigido — ADR-006 §7 exigia fonte única:

```text
README.md §11

Class-Architecture.md

Fabric-Implementation-Plan.md
```

Os três repetiam layouts divergentes.

Agora apontam para a ADR-006.

Divergências encontradas:

```text
core/profession/  →  core/worker/

camada manager    →  não existe mais

core/type/        →  estava ausente
```

Pendente:

```text
icon do mod — decisão do autor
```

---

## 2026-08-06 — TASK-004 concluída

Criado:

```text
fabric/event/ServerLifecycleHandler.java
```

Alterado:

```text
VillageColonyMod  → LOGGER compartilhado, chama register()
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → mod carregado, sem exceções
```

Não verificado:

```text
Os handlers de SERVER_STARTED e SERVER_STOPPING nunca executaram.
```

`runServer` para no EULA:

```text
run/eula.txt  →  eula=false
```

Aceitar o EULA é decisão do autor, não do agente.

Pendência aberta até que um servidor rode.

---

## 2026-08-06 — TASK-005 concluída

Criado:

```text
core/type/ColonyPos                  record, ADR-005

core/colony/model/ColonyState        STABLE, PRODUCTION, EXPANSION

core/colony/model/ColonyLifecycle    ACTIVE, DORMANT

core/colony/model/Colony
```

Infraestrutura:

```text
JUnit 5.12.2 configurado em build.gradle
```

Verificado:

```text
13 unit tests passando

Core sem net.minecraft

Nenhum domínio do core importa outro
```

Conflito de documentos resolvido:

```text
Data-Model.md    state = STABLE | PRODUCTION | EXPANSION

ADR-002          state = ACTIVE | DORMANT
```

Não era conflito. São dois eixos independentes:

```text
ColonyState      o que a colônia faz

ColonyLifecycle  se a colônia é simulada
```

Uma colônia DORMANT conserva seu ColonyState e retoma nele.

Coberto pelo teste `goingDormantPreservesState`.

Corrigido em `Data-Model.md`:

```text
centerPosition   BlockPos → ColonyPos   (ADR-005)

lifecycle        campo adicionado
```

---

## 2026-08-06 — TASK-006 concluída

Criado:

```text
core/colony/service/ColonyService
```

Nome divergente de `MVP-Tasks.md`:

```text
MVP-Tasks.md   ColonyManager

ADR-006 §5     manager não existe como camada
```

Vale a ADR. A classe é `ColonyService`.

Operações:

```text
createColony    detecção encontrou vila nova

register        recolocação vinda do save

find            por id

findNearest     por posição, com raio explícito

all             leitura, ordem de registro

remove / clear / count
```

Verificado:

```text
35 unit tests passando

Core sem net.minecraft
```

Decisões registradas no código (TASK-006):

```text
register duplicado lança em vez de sobrescrever

  sobrescrever esconderia save corrompido

raio de findNearest é parâmetro, não constante

  quem chama conhece o contexto (ADR-003)

LinkedHashMap para ordem de iteração estável

  ordem instável dificulta depurar simulação

sem thread safety — thread única do servidor

  documentado na classe
```

Fase 1 encerrada.

---

## 2026-08-06 — TASK-007 escrita, não verificada

Criado:

```text
data/save/ColonySavedData
```

Alterado:

```text
VillageColonyMod        campo COLONIES (ADR-006 §5)

ServerLifecycleHandler  carrega no start, grava no stop
```

API confirmada contra o jar mapeado, não de memória:

```text
PersistentState.Type<T>(Supplier, BiFunction, DataFixTypes)

writeNbt(NbtCompound, RegistryWrapper.WrapperLookup)

server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, key)
```

Decisão — `lifecycle` não é persistido:

```text
É estado derivado do carregamento de chunk.

Ao abrir o mundo nada está carregado.

Toda colônia volta DORMANT.
```

Persistir `ACTIVE` marcaria como simulável uma colônia cujo chunk não
existe em memória.

Contraria o que eu havia planejado na entrada anterior deste log, onde
`lifecycle` constava entre os campos a salvar. A leitura da ADR-002
mostrou que estava errado.

Decisão — estado desconhecido no save:

```text
Cai para STABLE em vez de lançar.

Não impedir o jogador de abrir o mundo.
```

Verificado:

```text
43 tests passando

runServer: Loaded 0 / Saved 0 colonies

villagecolony_colonies.dat gravado no mundo
```

O EULA foi aceito pelo autor, desbloqueando também a verificação
pendente da TASK-004.

---

## 2026-08-06 — Ordem do MVP-Tasks tem uma inversão

`TASK-008 — Testar Carregamento` exige:

```text
Criar mundo → Encontrar vila → Salvar → Fechar → Abrir
```

"Encontrar vila" é `TASK-009`.

A TASK-008 depende de uma tarefa que vem depois dela.

Consequência prática: o round-trip com dados reais só pode ser
provado após a detecção existir.

Enquanto isso, a serialização foi coberta por teste direto de NBT,
sem servidor.

---

## 2026-08-06 — TASK-009 concluída

Criado:

```text
core/colony/model/VillageCandidate

core/colony/service/VillageDetector

fabric/adapter/MinecraftTypeAdapter

fabric/integration/VillageScanner
```

Divisão adotada:

```text
Core      decide o que é vila (puro, testável)

Fabric    lê POI, conta aldeões, checa bioma
```

APIs confirmadas com `javap` no jar mapeado:

```text
PointOfInterestStorage.getInCircle(Predicate, BlockPos, int, OccupationStatus)

PointOfInterestTypes.HOME / MEETING  são RegistryKey

WorldView.getBiome(BlockPos) → RegistryEntry<Biome>

EntityView.getEntitiesByClass(Class, Box, Predicate)
```

---

## Conflito entre ADRs — resolvido com desvio

ADR-003 §7 manda `ColonyState` ganhar `DORMANT`, dizendo estar
"alinhado com ADR-002".

Não está:

```text
ADR-002   DORMANT = chunk descarregado

ADR-003   DORMANT = vila sem população
```

Condições diferentes. Uma vila abandonada com o jogador ao lado
atende a segunda e não a primeira.

`ColonyLifecycle` já usa `DORMANT` no sentido da ADR-002.

Decisão: o valor de `ColonyState` chama-se `ABANDONED`.

Motivo: dois `DORMANT` com significados distintos no mesmo objeto
seriam uma armadilha para quem lê o código depois.

```text
PENDENTE: emendar ADR-003 §7 registrando a troca.
```

Este é um desvio do texto de uma ADR aceita, e está aqui para ser
revisto pelo autor — não para passar despercebido.

---

## Escolhas de implementação

```text
Distância de cluster é horizontal

  a cama do sótão é da mesma casa
```

```text
Média das camas somada em long

  64 camas em coordenada extrema estouram int
```

```text
Camas com OccupationStatus.ANY

  vila que perdeu aldeões ainda é vila;

  quem decide isso é a validação
```

```text
Caixa de contagem derivada das camas

  buscar aldeões no mundo inteiro é proibido

  por Performance-Rules.md §5
```

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
