# Project-State.md

# Village Colony — Project State

**Status:** Em implementação — Fases 1 a 3 completas, Fase 4 em andamento
**Version:** 0.1.0
**Last Update:** 2026-08-07 — TASK-012 concluída
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
Fase 4 — Sistema de Trabalhadores
```

---

## Description

Fases 1 a 3 estão completas e verificadas dentro do jogo: o mod detecta
vilas, cria colônias, mantém sua identidade e persiste tudo entre
sessões.

A fase atual registra os aldeões como trabalhadores e vai atribuir-lhes
profissões de colônia.

---

## Concluído até aqui

```text
Fase 0   decisões de arquitetura        ADR-001 a ADR-006

Fase 1   núcleo da colônia              TASK-001 a TASK-006

Fase 2   persistência                   TASK-007 e TASK-008

Fase 3   detecção da vila               TASK-009 e TASK-010

Fase 4   trabalhadores                  TASK-011 e TASK-012
```

Detalhe por tarefa em §6. Histórico em §15.

---

# 4. Documentation Status

A documentação abaixo foi escrita antes do código e permanece a
referência do projeto. Onde código e documento divergirem, o conflito
está registrado em §9 ou numa emenda de ADR.

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

## MVP Goal e progresso

```text
Detectar vila          FEITO, verificado em jogo

↓

Registrar aldeões      FEITO, não verificado em jogo

↓

Organizar trabalhadores   em andamento (TASK-013, TASK-014)

↓

Coletar recursos       não iniciado

↓

Produzir materiais     não iniciado

↓

Construir expansão     não iniciado
```

Dois dos seis passos do MVP estão de pé.

---

# 6. Implementation Status

Uma linha por tarefa. O detalhe de cada uma está no Development Log (§15).

```text
Fase 0 — Decisões

  ADR-001 a ADR-006          aceitas
  ADR-003                    emendada três vezes (§10 da própria ADR)
  Version Matrix             fixada e validada por build

Fase 1 — Núcleo da Colônia

  TASK-001  projeto Fabric           feito, verificado em jogo
  TASK-002  identidade do mod        feito, verificado em jogo
  TASK-003  estrutura de pacotes     feito
  TASK-004  entrypoint e eventos     feito, verificado em jogo
  TASK-005  modelo Colony            feito
  TASK-006  ColonyService            feito

Fase 2 — Persistência

  TASK-007  ColonySavedData          feito, verificado em jogo
  TASK-008  teste de carregamento    feito, verificado em jogo

Fase 3 — Detecção da Vila

  TASK-009  detecção de vila         feito, verificado em jogo
  TASK-010  criação automática       feito, verificado em jogo

Fase 4 — Trabalhadores

  TASK-011  modelo Worker            feito
  TASK-012  VillagerScanner          feito, NÃO verificado em jogo
  TASK-013  ProfessionRegistry       não iniciado
  TASK-014  atribuição inicial       não iniciado

Fases 5 a 9

  TASK-015 em diante                 não iniciadas
```

---

## Código existente

```text
core/
  type/ColonyPos
  colony/model/      Colony, ColonyState, ColonyLifecycle, VillageCandidate
  colony/service/    ColonyService, VillageDetector
  worker/model/      Worker, ProfessionType
  worker/service/    WorkerService

fabric/
  adapter/           MinecraftTypeAdapter
  event/             ServerLifecycleHandler, VillageDetectionHandler
  integration/       VillageScanner, VillagerScanner

data/
  save/              ColonySavedData
```

Vazios por enquanto: `core/task`, `core/resource`, `core/storage`,
`core/construction`, `fabric/mixin`, `fabric/brain`.

---

## Testes

```text
106 testes, todos passando
```

Cobrem o Core (lógica pura) e a serialização NBT.

Não cobrem a fronteira com o Minecraft — é lá que moraram todos os
defeitos sérios desta fase. Ver §11.

---

## O que o mod faz hoje, em jogo

```text
detecta vilas plains por cluster de camas

cria colônias com identidade estável

move o centro só para observações mais completas

acorda e adormece colônias conforme os chunks

registra os aldeões como trabalhadores

persiste as colônias entre sessões
```

---

# 7. Next Development Step

## Decisão necessária antes de codificar

```text
Persistência de trabalhadores
```

`MVP-Tasks.md` não tem tarefa para isso, e é uma lacuna do plano.

Hoje os trabalhadores são redescobertos a cada sessão a partir dos
aldeões do mundo. Isso basta enquanto só há registro.

Deixa de bastar em TASK-014: profissão atribuída é decisão da colônia,
não existe no mundo Vanilla e sumiria ao fechar o mundo. Cada sessão
redistribuiria funções do zero.

Opções:

```text
estender ColonySavedData

criar WorkerSavedData
```

---

## Depois da decisão

```text
TASK-013 — ProfessionRegistry

TASK-014 — Atribuição inicial de profissões
```

---

# 8. Priority Queue

```text
1   decidir persistência de trabalhadores

2   TASK-013 — ProfessionRegistry

3   TASK-014 — atribuição inicial

4   verificar TASK-012 em jogo

5   Fase 5 — Sistema de Armazenamento (TASK-015+)
```

O item 4 não bloqueia os demais, mas quanto mais tarde, mais caro:
todos os defeitos graves desta fase apareceram só em jogo.

---

# 9. Known Limitations

## Regras aceitas e ainda não implementadas

```text
ColonyState.ABANDONED

  O valor existe; nada o atribui.

  ADR-003 §6 exige distinguir "vila deixou de ser viável" de
  "vila não foi observada". Hoje VillageScanner.scan devolve
  apenas clusters aprovados, então as duas situações são
  indistinguíveis.

  Exige o scanner reportar clusters reprovados. Tarefa própria.
```

```text
Aviso de colônias sobrepostas

  ADR-003 §5 manda registrar
  "[COLONY] Overlapping colonies detected"
  quando dois centros ficam a menos de 32 blocos.

  Não implementado.
```

```text
Loop de simulação

  ADR-002 define ACTIVE/DORMANT e o loop só roda para ACTIVE.

  O ciclo de vida já funciona; o loop não existe.
```

---

## Limites de escopo assumidos

```text
Só bioma PLAINS

  Cluster em outro bioma é ignorado. Não é erro (ADR-003 §5).
```

```text
Registro único, Overworld

  COLONIES e WORKERS são estáticos e não separam dimensão.

  Não é problema hoje porque PLAINS só existe no Overworld,
  mas a suposição está no código, não no tipo.
```

```text
Trabalhadores não são persistidos

  Ver §7.
```

```text
Fusão e divisão de vilas

  MVP não funde nem divide (ADR-003 §5).
```

---

## Limitação do ambiente de desenvolvimento

```text
Comandos que resolvem argumento de registro falham no
console do runServer:

  seed, locate structure, locate biome, execute if biome

  → "An unexpected error occurred", sem stack trace

Funcionam: say, list, stop, forceload
```

Não passa pelo código do mod — `seed` e `locate` são Vanilla puro.

Consequência prática: testes que dependem de localizar estrutura ou
sondar bioma precisam ser feitos no jogo real, não no `runServer`.

---

# 10. Pending Decisions

```text
1  Persistência de trabalhadores — ver §7

   Bloqueia TASK-014.
```

```text
2  Ícone e nome divergem

   A arte diz "Village++"; o mod é "Village Colony", id villagecolony.

   Decisão do autor em 2026-08-07: manter como está.

   Trocar o id quebraria saves — ele nomeia
   villagecolony_colonies.dat, o caminho do ícone e o logger.
```

```text
3  Fundo do ícone

   A arte veio sem canal alpha, fundo branco sólido.

   Não foi removido por chave de cor: a ovelha e as nuvens
   também são brancas e ficariam com buracos.

   Depende de recorte manual, se o autor quiser transparência.
```

---

# 11. Architectural Risks

## Risco confirmado — teste unitário não alcança a fronteira

Quatro defeitos sérios desta fase passaram por 100+ testes verdes:

```text
fabricloader >=0.19.3 exigido sem necessidade

  o mod não carregaria na instalação real do autor

ChunkPos.getStartPos() devolve y=0, getInCircle mede em 3D

  o gatilho de chunk nunca encontrou vila alguma

Centro oscilante entre observações parciais

  a vila chegou a trocar de UUID, violando ADR-003 §4

ColonyLifecycle sem escritor em produção

  toda colônia vinda do save ficaria DORMANT para sempre,
  e o loop de simulação a ignoraria em silêncio
```

Os dois primeiros só apareceram rodando o mod no jogo real.

O terceiro apareceu porque uma linha de log foi adicionada no commit
anterior — sem ela, o centro oscilaria em silêncio.

O quarto apareceu numa auditoria de `grep`, não em teste.

---

Consequência prática, a manter nas fases seguintes:

```text
depois de cada mudança que toque a camada fabric,
rodar no jogo real e ler o latest.log
```

```text
instrumentar antes de suspeitar

  a linha de log que expõe o defeito precisa existir
  antes de alguém desconfiar dele
```

---

## Armadilha de método já paga

```text
Trocar o jar com o jogo aberto não testa nada.
```

O Minecraft carrega mods na inicialização da JVM. Sair ao menu e
reentrar no mundo reusa o código em memória.

Duas sessões de teste foram desperdiçadas assim.

---

## Riscos ainda abertos

```text
Raio de detecção menor que a vila

  Mitigado por observedBeds, não eliminado.

  Uma vila muito grande pode nunca ser observada por inteiro.
```

```text
Dois clusters distintos a menos de 64 blocos

  São adotados como uma colônia só, em silêncio.

  ADR-003 §5 pede aviso; não implementado.
```

```text
Mixin ainda não escrito

  ADR-004 limita a superfície, mas nada foi injetado ainda.

  O risco de compatibilidade com outros mods de aldeão
  aparece só na Fase 9.
```

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

Fonte de verdade por assunto. Onde dois documentos discordarem, vale o
mais específico desta lista.

```text
O que o projeto é

  PROJECT_CONSTITUTION.md      princípios; §3 emendado por ADR-002

  MVP.md                       escopo do MVP

  README.md                    visão geral e status resumido


Arquitetura

  docs/decisions/ADR-001..006  decisões; ADR-003 tem emendas em §10

  Architecture-Foundation.md   camadas

  Data-Model.md                modelos e campos

  Class-Architecture.md        classes; layout delegado à ADR-006


Sistemas

  Simulation-Loop.md           ciclo da colônia (ainda não implementado)

  Profession-System.md         profissões

  Resource-System.md           recursos

  Storage-System.md            armazenamento

  Construction-System.md       construção

  Save-Data-System.md          persistência


Execução

  MVP-Tasks.md                 tarefas; ver ressalva abaixo

  docs/technical/Fabric-Version.md   versões fixadas

  docs/technical/Performance-Rules.md

  docs/technical/Testing-Strategy.md

  docs/technical/Debugging-Strategy.md

  docs/technical/Vanilla-Integration.md

  docs/technical/Project-State.md    este documento
```

---

## Ressalvas conhecidas nos documentos

```text
MVP-Tasks.md

  TASK-006 chama a classe de "ColonyManager".

  A ADR-006 §5 removeu manager como camada; a classe é ColonyService.


  TASK-008 lista "Encontrar vila" como passo, mas isso é TASK-009.

  A tarefa depende de outra que vem depois dela. Foi executada

  fora de ordem, por isso.


  Não existe tarefa de persistência de trabalhadores. Ver §7.
```

```text
Initial-Setup-Checklist.md §6 e Class-Architecture.md

  Continham layouts de pacote divergentes. Agora apontam para

  a ADR-006 em vez de repetir a estrutura.
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
