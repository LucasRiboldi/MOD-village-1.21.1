# Project-State.md

# Village Colony — Project State

**Status:** MVP escrito por inteiro — Fases 1 a 9 verificadas em jogo,
10 e 11 só por teste
**Version:** 0.1.0
**Last Update:** 2026-08-15, mais tarde — uma sessão de leitura. O mod
Workers foi lido por inteiro e virou `docs/workers-analysis/`; a pasta
dele saiu do alcance do git; e a tarefa reservada ganhou como voltar
para a fila quando o trabalhador vivo não chega. A casa ainda não subiu

Antes disso, na madrugada: quatro sessões de jogo em duas noites. A
Fase 9 e o item C ficaram verificados em jogo; o E10, o E13 e o E14
fecharam, com a TASK-045 e a TASK-047 junto; e o rodízio de profissão
(E11) espera decisão do autor
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
Fases 9, 10 e 11 escritas e cobertas por teste — falta vê-las em jogo
```

---

## Description

O mod detecta vilas, cria colônias com identidade estável entre sessões,
registra aldeões como trabalhadores, dá função a cada um, acha e marca o
baú de cada trabalhador, conta o que ele guarda, decide o que falta,
abre tarefas e as entrega. O lenhador anda até a árvore, derruba um
bloco por vez no tempo de um jogador com machado de ferro, deposita no
baú e replanta.

A Fase 8 fechou em jogo em 2026-08-08, com a primeira derrubada, e o
trabalho contínuo da Regra 2 entrou em 2026-08-11. Em 2026-08-12 o mod
rodou pela primeira vez num servidor dedicado sobre uma cópia do save do
autor, e no fim daquele dia dois lenhadores da vila dele estavam
cortando — 22 e 7 toras.

A Fase 9 entrou em 2026-08-13: o fabricante tira tronco do baú, faz
tábua pela receita do próprio jogo e devolve ao mesmo baú. É a primeira
vez que o mod **diminui** o que o jogador tem — até aqui a colônia só
somava.

Em 2026-08-14, à noite, ela rodou em jogo pela primeira vez e **não
fabricou nada**: dezessete tarefas encerradas por falta de tronco, com
134 troncos guardados na colônia. O fabricante lia o próprio baú, e
quem colhe deposita no baú dele — é o E10 do §17.

Corrigido e **verificado em jogo na mesma noite**, às 23:41:

```text
Colony 0c2771b0 stores {... ACACIA_LOG=22, OAK_PLANKS=256,
                        ACACIA_PLANKS=20} in 5 of 8 chests read
```

Vinte tábuas de acácia onde não havia nenhuma três minutos antes,
`manufacturers:` com peças subindo, e nenhuma tarefa encerrada por
falta de tronco. **A Fase 9 está verificada em jogo.**

Em 2026-08-14 a colônia passou a **construir**. O construtor levanta a
casa de planície do próprio jogo, um bloco por segundo, tirando cada peça
do baú antes de pô-la no mundo — e a casa pronta vira infraestrutura da
colônia. É o sexto e último passo do MVP, e o primeiro trabalho do mod
que acrescenta bloco em vez de tirar. Nada disso foi visto em jogo.

Ainda em 2026-08-13, mais tarde, saíram as três dívidas que não
dependiam de decisão nem de sessão de jogo — os itens A, B e C do §8:
`ColonyState.ABANDONED` ganhou quem o atribua, colônias sobrepostas
passaram a avisar, e o trabalhador recebe a ferramenta da profissão.
Nenhuma das três foi vista em jogo.

---

## Concluído até aqui

```text
Fase 0   decisões de arquitetura        ADR-001 a ADR-006
                                        ADR-003 e ADR-004 emendadas

Fase 1   núcleo da colônia              TASK-001 a TASK-006
Fase 2   persistência                   TASK-007 e TASK-008
Fase 3   detecção da vila               TASK-009 e TASK-010
Fase 4   trabalhadores                  TASK-011 a TASK-014
Fase 5   armazenamento                  TASK-015 a TASK-017
Fase 6   recursos                       TASK-018 a TASK-020
Fase 7   tarefas                        TASK-021 a TASK-023
Fase 8   primeiro trabalhador           TASK-024 e TASK-025
                                        verificada em jogo

Fase 9   fabricação                     TASK-027 a TASK-029
                                        coberta por teste, não vista
                                        em jogo

Fase 10  construção                     TASK-030 a TASK-035
Fase 11  registro de infraestrutura     TASK-036 e TASK-037
                                        cobertas por teste, não vistas
                                        em jogo
```

Detalhe por tarefa em §6. Histórico em `Development-Log.md`.

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
Detectar vila             FEITO, verificado em jogo

↓

Registrar aldeões         FEITO, verificado em jogo

↓

Organizar trabalhadores   FEITO, verificado em jogo

↓

Coletar recursos          FEITO, verificado em jogo

↓

Produzir materiais        FEITO, verificado em jogo em 2026-08-14

↓

Construir expansão        FEITO, não verificado em jogo
```

**Os seis passos do MVP estão escritos.** Cinco estão verificados em
jogo. Falta um: a casa subindo.

E o que separa esse último de verificado não é mais "rodar uma sessão".
A sessão de 23:41 rodou o jar certo, com construtor na vila e tábua nos
baús, e a Fase 10 não abriu obra nenhuma. Por qual dos cinco motivos,
ninguém sabe — nenhum deles escrevia linha. A instrumentação entrou na
mesma noite, e é ela que a próxima sessão vai ler. Ver §8, P1.

---

# 6. Implementation Status

Uma linha por tarefa. O detalhe de cada uma está no Development Log.

```text
Fase 0 — Decisões

  ADR-001 a ADR-006          aceitas
  ADR-003                    emendada três vezes
  ADR-004                    emendada na Fase 8 (§11 da própria ADR)
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
  TASK-012  VillagerScanner          feito, verificado em jogo
  TASK-012b persistir trabalhadores  feito, verificado em jogo
  TASK-013  ProfessionRegistry       feito, verificado em jogo
  TASK-014  atribuição inicial       feito, verificado em jogo
                                     e por gametest

Fase 5 — Armazenamento

  TASK-015  detecção de baú          feito, verificado em jogo
                                     e por gametest
  TASK-016  StorageRegistry          feito, verificado em jogo
  TASK-017  ler inventário           feito, verificado em jogo
                                     e por gametest

  extra     marca do baú             feito (ChestMarker), verificada
                                     em jogo pelo autor

Fase 6 — Recursos

  TASK-018  visão agregada           feito (ColonyResources)
  TASK-019  verificação de déficit   feito (ResourceDemand)
  TASK-020  integrar com simulação   feito (ColonyCycle), verificado
                                     em jogo e por gametest

Fase 7 — Tarefas

  TASK-021  modelo Task              feito
  TASK-022  TaskService              feito
  TASK-023  associar a profissões    feito (WorkAssignment),
                                     verificado em jogo

Fase 8 — Primeiro Trabalhador

  TASK-024  capacidade do lenhador   feito (LumberjackWork)
  TASK-025  coleta de madeira        feito, verificado em jogo em
                                     2026-08-08

  extra     movimento pelo Brain     feito (ADR-004 §11)
  extra     nome sobre a cabeça      feito (WorkerNameplate)
  extra     Regra 1 e Regra 2        feitas (§18), cobertas por
                                     gametest e vistas em jogo em parte

  TASK-026  depositar de 32 em 32    cancelada: a madeira vai direto
                                     para o baú, por decisão de
                                     2026-08-08. Ver §10

Fase 9 — Fabricação

  TASK-027  implementar o Manufacturer   feito (ManufacturerWork)
  TASK-028  integrar o Recipe Manager    feito (CraftingLookup)
  TASK-029  produzir tábua               feito, coberto por teste de
                                         jogo, não visto em jogo

  extra     tirar item do baú            feito (ChestWithdrawer) —
                                         o mod nunca tinha feito isso
  extra     as oito tábuas               feitas (TreeSpecies,
                                         ResourceGroup.PLANKS)

Fora de fase — regras aceitas em documento e nunca implementadas

  item A    ColonyState.ABANDONED        feito (ClusterRejection,
                                         ColonyAbandonment), coberto
                                         por teste de unidade, não
                                         visto em jogo

  item B    aviso de sobreposição        feito (ColonyService.overlapping),
                                         coberto por teste de unidade,
                                         não visto em jogo

  item C    ferramenta inicial           feito (MinecraftTypeAdapter.toItem,
                                         WorkerEquipment), coberto por
                                         teste de jogo, não visto em jogo

Fase 10 — Construção                     começada em 2026-08-14

  Regra 6   como a vila cresce           decidida pelo autor (§18)

  TASK-030  criar o Blueprint            feito (Blueprint,
                                         BlueprintBlock, ResourceId)
  TASK-031  ler estrutura Vanilla        feito (StructureBlueprintReader),
                                         coberto por teste de jogo
  TASK-032  calcular materiais           feito (ConstructionProject,
                                         ConstructionState) — e a Regra 5
                                         ligada em ColonyGoals
  TASK-033  criar a Build Task           feito (ConstructionService,
                                         ConstructionPlanner)
  TASK-034  implementar o Builder        feito (BuilderWork)
  TASK-035  colocar blocos               feito, coberto por teste de
                                         jogo, não visto em jogo

Fase 11 — Registro de Infraestrutura     feita junto da TASK-035

  TASK-036  Building Registry            feito (Building, BuildingRegistry)
  TASK-037  marcar blocos da colônia     feito — por caixa da construção,
                                         não por bloco

  extra     escolher o lote              feito (BuildSiteScanner),
                                         coberto por teste de jogo

Nada das Fases 10 e 11 foi visto em jogo.

Fora de fase — fechado em 2026-08-14, à noite, a partir do log da sessão

  TASK-045  proteção consulta construções   feito
                                     (BlockProtection.isColonyBuilt),
                                     coberto por teste de jogo e rodado
                                     contra a regra desligada

  E10       o fabricante lê os baús da   feito
            colônia, e não só o seu       (ManufacturerWork.convertOne),
                                     coberto por teste de jogo e
                                     **verificado em jogo** em 08-14

  TASK-047  a cobertura do campo não     feito
            reprova o lote               (BuildSiteScanner.isNothing),
                                     coberto por teste de jogo. É o que
                                     fechou o E14, e destravou a Fase 10

  extra     a Fase 10 diz por que não    feito
            construiu                    (ConstructionPlanner.silent),
                                     cinco motivos separados, e a
                                     varredura inacabada separada da
                                     resposta (BuildSiteScanner
                                     .sweepPausedAt)

Fora de fase — 2026-08-15, da leitura do mod Workers

  TASK-050  a tarefa volta para a fila   feito (LumberjackWork,
            quando o trabalhador não     ManufacturerWork, BuilderWork:
            chega                        contador de ticks sem avanço,
                                         quatro ciclos, só em horário de
                                         trabalho e com o aldeão
                                         carregado). NÃO tem teste
                                         próprio: o limite é de 2400
                                         ticks e a bateria inteira roda
                                         em cinco segundos

  TASK-049  contar baús, e não candidatos  feito
                                         (ChestScanner.freeChestFor,
                                         ScanResult.freeChests). Fecha o
                                         E11: a colônia só dispensa quem
                                         não tem baú quando há baú livre
                                         de verdade. Decisão do autor de
                                         2026-08-15, opção A. Dois testes
                                         de jogo, o da ligação rodado
                                         contra a regra desligada

  TASK-053  a obra ganha tarefa            feito
                                         (ConstructionPlanner.ensureTask;
                                         TaskType.isResourceRequest
                                         protege a tarefa de obra das
                                         duas armadilhas do ciclo).

                                         É o defeito que a sessão de
                                         2026-08-15 achou: nada em
                                         produção criava tarefa BUILD, e
                                         o construtor nunca teve o que
                                         fazer em vila nenhuma. Três
                                         testes, os três rodados contra a
                                         regra desligada

  TASK-052  a porta vira porta, e não      feito
            duas metades soltas            (StructureBlueprintReader
                                         .isSecondHalf descarta a metade
                                         de cima na leitura;
                                         BuilderWork.placeSecondHalf
                                         escreve a outra com a
                                         propriedade que as liga). Dois
                                         testes de jogo, os dois rodados
                                         contra a regra desligada.

                                         Fecha metade do E8 **sem ADR
                                         nova**: o Core não mudou uma
                                         linha, porque quem sabe o que é
                                         "metade de cima" é a fronteira.
                                         Conserta junto uma conta que
                                         ninguém tinha notado — a porta
                                         custava duas portas ao baú

  TASK-051  o motivo de não trabalhar      feito (IdleReason no Core,
            como valor                     IdleLog na fronteira). As três
                                         profissões falam o mesmo
                                         vocabulário, e o log registra a
                                         mudança de motivo em vez do
                                         estado. Sete testes de unidade,
                                         dois rodados contra a regra
                                         desligada. É o F1 da fila da
                                         workers-analysis

  extra     a análise do Workers         doze documentos em
                                         docs/workers-analysis/, e a
                                         pasta workers-maingit/ no
                                         .gitignore — a licença dela é
                                         All Rights Reserved

Fase 12 — Testes do MVP                  TASK-038 a 040 cobertas por
                                         gametest; TASK-041 e 042
                                         exigem sessão de jogo
```

---

## Código existente

```text
core/
  type/              ColonyPos, Capability, ResourceType,
                     ResourceCategory, ResourceGroup
  colony/model/      Colony, ColonyState, ColonyLifecycle,
                     VillageCandidate, ClusterRejection
  colony/service/    ColonyService, VillageDetector, ColonyAbandonment
  construction/model/ Blueprint, BlueprintBlock, ConstructionProject,
                     ConstructionState, Building
  construction/service/ ConstructionService, BuildingRegistry
                     (as duas persistidas em ColonySavedData)
  coordination/      ColonyCycle, ColonyGoals, WorkAssignment
  worker/model/      Worker, ProfessionType, Profession, ToolType
  worker/service/    WorkerService, ProfessionRegistry,
                     ProfessionAssigner
  storage/model/     WorkerStorage
  storage/service/   StorageRegistry
  resource/model/    ResourceTally, ColonyResources
  resource/service/  ResourceDemand
  task/model/        Task, TaskState, TaskType, TaskPriority
  task/service/      TaskService

fabric/
  adapter/           MinecraftTypeAdapter
  brain/             ColonyBrainInitializer, GoToWorkTargetTask,
                     WorkHours, WorkTargets
  event/             ServerLifecycleHandler, VillageDetectionHandler,
                     VillagerLifecycleHandler
  integration/       VillageScanner, VillagerScanner, ChestScanner,
                     ChestInventoryReader, ChestDepositor, ChestMarker,
                     ChestWithdrawer, CraftingLookup, BlockProtection,
                     TreeScanner, TreeHarvester, TreeSpecies,
                     BlockBreakTime, WorkerNameplate, WorkerEquipment,
                     StructureBlueprintReader, BuildSiteScanner
  work/              LumberjackWork, ManufacturerWork, BuilderWork,
                     ConstructionPlanner
  mixin/             VillagerEntityMixin
data/
  save/              ColonySavedData
```

Nenhum pacote vazio: `core/construction` foi preenchido em 2026-08-14.

---

## Testes

```text
375 testes unitários     lógica pura do Core e serialização NBT
 85 testes de jogo       a fronteira com o Minecraft, num servidor
                         sem cliente (./gradlew runGametest)
```

Sete entraram em 2026-08-15 com o `IdleReason`: afirmam que dois
silêncios diferentes produzem linhas diferentes, que é a propriedade
inteira daquele tipo. Dois deles foram rodados contra a regra
desligada — duplicando a frase de um motivo — e falharam sozinhos.

Quatro entraram entre 2026-08-14 à noite e 08-15 de madrugada: o tronco
no baú do lenhador virando tábua (E10), a casa da colônia protegida
(E7), a varredura inacabada que não é resposta (E14) e o campo de grama
que não reprova o lote (TASK-047).

Três dos quatro foram rodados contra a regra desligada e falharam
sozinhos. O da varredura não teve fase vermelha: ele afirma uma
propriedade de `sweepPausedAt`, que não existia antes dele.

Os testes de jogo rodam **concorrentes**: um teste que atravessa ticks
continua vivo enquanto os batches seguintes começam. Nenhum deles pode
apagar registro global nem afirmar sobre contagem global — ver
`ColonyFixture`, e o comentário no lugar onde ficava o teste de
encolhimento.

O gametest existe porque o teste unitário não alcança a fronteira, e foi
lá que moraram todos os defeitos sérios deste projeto. Ver §11.

O que ele ainda não cobre: persistência entre sessões — exige fechar e
reabrir o mundo, e a bateria roda um servidor só — e tudo o que só o
cliente mostra.

---

## O que o mod faz hoje, em jogo

```text
detecta vilas plains por aglomerado de camas

cria colônias com identidade estável, e as reconhece entre sessões

move o centro só para observações mais completas, e encolhe quando a
sonda da própria colônia confirma a leitura menor

acorda e adormece colônias conforme os chunks

registra os aldeões como trabalhadores e dá função a quem não tem

acha o baú de cada trabalhador por linha livre desde a cama, e prega
nele um quadro com o ícone da profissão

conta o estoque, mede o espaço livre e decide o que falta

abre uma tarefa por mão capaz, e as entrega

o lenhador anda até a árvore pelo Brain, derruba um bloco por vez no
tempo de um jogador com machado de ferro, deposita e replanta

libera vaga, baú e tarefa quando o trabalhador morre ou é zumbificado
```

---

# 7. Next Development Step

## Ver a casa subir

Não há mais fase por escrever, e a Fase 9 saiu da lista em 2026-08-14:
o fabricante foi visto trabalhando em jogo. Sobra o sexto passo.

```text
1  a Fase 10 em jogo       a linha "planned", o lote escolhido, e a casa
                           subindo bloco a bloco. A casa de planície são
                           151 blocos, um por segundo

2  a Fase 11 em jogo       a casa pronta virando infraestrutura, e o
                           lote seguinte não caindo em cima dela
```

Quatro sessões em 2026-08-14 e 15 foram gastas chegando até aqui, e
nenhuma delas viu bloco ser posto. O que elas fecharam foi o caminho
até a obra: o fabricante lendo o baú certo (E10), o jar certo (E13), a
Fase 10 dizendo por que não construía (E14 primeira metade) e a grama
do campo deixando de reprovar todo lote (E14, TASK-047).

**O que ainda não se sabe:** se as vilas do autor têm lote de verdade.
A grama deixou de recusar; que sobre lugar é outra pergunta, e ela
depende do que a vila tem de espaço livre encostado em rua.

O que a sessão precisa ter: um construtor na vila, `/time set noon`, e
**pedra e vidro nos baús** — a colônia produz tábua e nada mais, e a
casa pede 43 de pedra. Sem isso a obra fica em WAITING_RESOURCES, que é
o comportamento certo e não uma casa.

Se voltar `planned no building`, a linha agora diz qual dos cinco
motivos é.

---

## Depois disso, e nesta ordem

```text
estender a estrada              hoje a vila só constrói em beira de rua
                                que já existe. Quando ela acabar, a
                                colônia para de crescer antes do que a
                                Regra 6 permite

TASK-042                        o teste de persistência do MVP, que a
                                bateria não alcança
```

---

# 8. Priority Queue

Situação em 2026-08-15, depois de quatro sessões de jogo.

---

## Precisa de uma sessão de jogo do autor

É o único item que bloqueia o MVP.

```text
P1  a casa subindo

    Fase 10   a linha "planned", o lote escolhido, e a casa subindo
              bloco a bloco. São 151 blocos, um por segundo

    Fase 11   a casa pronta virando infraestrutura, e o lote seguinte
              não caindo em cima dela

    itens A e B      a linha "is now ABANDONED" ao demolir camas, e o
                     aviso de sobreposição se o mundo der o acaso
```

O que a sessão precisa ter: um construtor na vila, `/time set noon`, e
**pedra e vidro nos baús** — a colônia produz tábua e nada mais, e a
casa pede 43 de pedra. Sem isso a obra fica em WAITING_RESOURCES, que é
o comportamento certo e não uma casa.

Saíram desta fila em 2026-08-14 e 15, todos vistos em jogo:

```text
Fase 9      "manufacturers:" com peças subindo, e vinte tábuas de
            acácia onde não havia nenhuma

item C      "Equipped 4 workers" nas duas colônias
```

```text
P2  a metade estrutural da Regra 3

    O mod perguntar ao jogo quais blocos são peça de vila gerada. O
    mundo do gametest não tem vila gerada, e em jogo quem tem
    protegido é a regra da copa.

P3  o lado do cliente

    Nome sobre a cabeça, rachadura no bloco e braço balançando. O log
    não os mostra.
```

---

## Não precisa de decisão nem de jogo

```text
TASK-043  estender a estrada

          A metade que falta da Regra 6. A intenção já está
          registrada — um trecho por casa —, e pavimentar não custa
          material.
```

---

## Precisa de decisão do autor

```text
TASK-049  quando dispensar quem não conseguiu baú

          É o E11 do §17, visto em jogo em 2026-08-14: a colônia
          dispensa o trabalhador sem baú "em favor de quem consegue",
          e o substituto também não consegue — nove vezes em
          dezesseis minutos, uma por ciclo.

          A correção mexe na Regra 4, e por isso espera: dispensar só
          faz sentido se o substituto puder de fato conseguir baú, e
          decidir isso antes de atribuir é regra, não implementação.

TASK-048  o que uma colônia ABANDONED deixa de fazer

          Hoje: nada. Ela é marcada e continua sendo simulada.

TASK-044  a fusão de vilas

          Decidida em 2026-08-12 e destravada em 2026-08-14, quando o
          registro de construções passou a existir. Exige ADR nova.

TASK-046  a orientação dos blocos

          É o E8 do §17, e a decisão é de arquitetura: levar
          BlockState para o Core contra a ADR-005, ou inventar uma
          linguagem de propriedades lá dentro.
```

---

## Fechado em 2026-08-13 e 2026-08-14

```text
itens A, B e C do §8 anterior       ColonyState.ABANDONED com escritor,
                                    aviso de sobreposição, ferramenta
                                    do trabalhador

Fase 10 inteira                     TASK-030 a 035
Fase 11 inteira                     TASK-036 e 037

persistir obra e construção         a que era a dívida mais cara do
                                    projeto, aberta e fechada no mesmo
                                    dia

teste flaky de profissão            a afirmação passou a dizer a regra
                                    em vez da população
```

---

# 9. Known Limitations

## Regras aceitas e ainda não implementadas

```text
ColonyState.ABANDONED            implementado em 2026-08-13

  VillageScanner.survey passou a devolver também o que recusou, e
  ColonyAbandonment decide sobre isso. Só a sonda ancorada no
  centro da própria colônia, e só enquanto ela está ACTIVE — que é
  o que separa "a vila acabou" de "ninguém olhou", exatamente o que
  a ADR-003 §6 pede.

  O que a colônia abandonada ainda não faz: nada. Ela é marcada,
  gravada no save e nada muda de comportamento. Parar de simular
  uma vila morta é decisão que não foi tomada — e ABANDONED com
  jogador ao lado continua ACTIVE, então não é o lifecycle que
  resolve.
```

```text
Aviso de colônias sobrepostas    implementado em 2026-08-13

  ColonyService.overlapping responde quem está a menos de 32
  blocos, e a detecção avisa uma vez por par por sessão.

  O aviso não funde nada — fundir exige nova ADR, e o critério
  dela já está decidido: um bloco de uma encostando no da outra.
  Ver abaixo.
```

```text
Fusão de vilas por construção

  Decidida em 2026-08-12 e registrada em §10: duas vilas viram
  uma quando um bloco de uma encostar no bloco da outra.

  Depende da construção existir. Enquanto não existir, duas vilas
  encostadas podem disputar trabalhador — cada uma legítima pela
  sua colônia.
```

---

## Limites de escopo assumidos

```text
Só bioma PLAINS

  Aglomerado em outro bioma é ignorado. Não é erro (ADR-003 §5).
```

```text
Registro único, Overworld

  COLONIES e WORKERS são estáticos e não separam dimensão.

  Não é problema hoje porque PLAINS só existe no Overworld,
  mas a suposição está no código, não no tipo.
```

```text
Profissão não muda depois de atribuída

  ProfessionAssigner só preenche vaga, até duas por profissão
  (§18, Regra 4).

  A morte e a zumbificação já liberam a vaga — ver
  VillagerLifecycleHandler, coberto por gametest desde 2026-08-12.
  O que falta é realocar quem está vivo quando a necessidade da
  colônia muda, e isso não pertence ao MVP.
```

```text
Ferramenta inicial            entregue desde 2026-08-13

  WorkerEquipment põe a ferramenta da profissão na mão do
  trabalhador, e a tira de quem perde a função.

  O limite que fica: ninguém a vê. O modelo de aldeão do Vanilla
  implementa ModelWithHead e ModelWithHat, nunca ModelWithArms, e
  VillagerEntityRenderer não monta HeldItemFeatureRenderer —
  conferido no jarro mapeado da 1.21.1. O item existe no NBT do
  aldeão e não na tela de ninguém.

  E não muda velocidade de trabalho: a Regra 2 fixou a colheita no
  tempo de um machado de ferro, e LumberjackWork continua medindo
  por ele de propósito.
```

```text
Obra e construção         gravadas desde 2026-08-14

  As duas vão para o mesmo arquivo das colônias.

  O progresso da obra não é gravado, e isso é decisão: quem sabe o
  que já está de pé é o mundo. Ao retomar, cada bloco do projeto
  cujo lugar já contém o bloco certo sai da lista — e a parede que
  o jogador derrubou entre sessões volta a ser pedida, que é a
  resposta certa.

  O que continua fora do save: as tarefas, que são intenção do
  momento, e a marca de qual trabalhador estava construindo.
```

```text
O guarda de travamento não tem teste      2026-08-15

  TASK-050 solta a tarefa do trabalhador que passa quatro ciclos em
  horário de trabalho sem avançar. O caminho é o certo e não foi
  exercitado: alcançá-lo num gametest custaria 2400 ticks, contra os
  cinco segundos da bateria inteira.

  O que existe no lugar é a linha de log — quantos ticks, e onde o
  trabalhador estava. A sessão de jogo do P1 é a primeira chance de
  vê-la, e o esperado é **não** vê-la: se ela aparecer numa vila
  normal, o limite está curto ou alguma condição de horário está
  errada.
```

```text
Nada tira item de baú         resolvido na Fase 9

  ChestWithdrawer existe desde 2026-08-13 e é o único caminho que
  remove item de um baú. Fica registrado porque a frase valeu até
  a Fase 8 e aparece em comentários daquela época.
```

```text
O baú do jogador dentro da vila

  Resolvido em parte, e vale saber até onde. Desde 2026-08-08 o
  baú só é adotado com linha livre entre a cama e ele, no mesmo
  nível — parede desqualifica, e isso cobre tanto o baú do vizinho
  quanto o do jogador que construa fora da casa.

  O que continua possível: o jogador que puser um baú dentro da
  casa de um aldeão, ao alcance da cama, terá esse baú adotado. A
  partir de 2026-08-12 ele ao menos avisa — o baú adotado ganha um
  quadro com o ícone da profissão do dono.
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

## Em aberto

```text
nenhuma
```

A última em aberto era o fundo do ícone, e ela fechou em 2026-08-21.
Ver abaixo.

---

## Decididas e registradas

```text
As cinco decisões de 2026-08-15

   Tomadas de uma vez, para irem sendo desenvolvidas aos poucos. A
   ordem de execução é a do Backlog.md §4; o que cada uma decide está
   abaixo, com a tarefa que a implementa.

   1  quando dispensar quem não conseguiu baú          TASK-049
      → só se dispensa quando há baú livre DE VERDADE para o
        substituto. Contar candidatos não é contar baús.
      FEITA em 2026-08-15. Fecha o E11.

   2  o que uma colônia ABANDONED deixa de fazer       TASK-048
      → ela para de CRESCER, e continua colhendo. Sem obra nova; o
        trabalho que já existe segue.
      NÃO IMPLEMENTADA. Ver a ressalva do E9 abaixo.

   3  o que acontece quando duas vilas viram uma       TASK-044
      → a colônia resultante fica com TUDO: trabalhadores, baús e
        construções das duas. Confirma o que o autor já enunciara em
        2026-08-12 — a fusão não reduz trabalhadores.
      ADR-007 ESCRITA em 2026-08-21, e ela responde as três perguntas
      que faltavam: sobrevive o UUID da MAIOR; o teto de profissão é
      violado e PERMANECE violado; o centro é o do sobrevivente.
      NÃO IMPLEMENTADA.

   4  a orientação dos blocos                          TASK-046
      → o Core APRENDE a falar de orientação. Escada, porta e cama
        saem como o arquivo manda, e não no padrão.
      ADR-008 ESCRITA em 2026-08-21, forma (a): Side de quatro
      direções no BlueprintBlock, tradução na fronteira. Emenda a
      ADR-005, e o precedente está dito lá.
      NÃO IMPLEMENTADA.

   5  o fundo do ícone
      → arte nova entregue pelo autor, e instalada em 2026-08-21:
        1254x1254, o distintivo com o aldeão, a casa e o nome.
      FEITA em 2026-08-21. O fundo branco saiu por preenchimento a
      partir da borda, e não por chave de cor — e essa distinção era
      o problema inteiro. Chave de cor abriria buracos na roupa do
      aldeão e no reboco da casa, que também são brancos. O
      preenchimento só alcança o branco LIGADO À BORDA: 39,1% da
      imagem saiu, e nenhum branco de dentro do desenho foi tocado.
```

```text
Ressalva da decisão 2, e ela é a razão de a tarefa esperar

   O E9 registra que a marca ABANDONED oscila no ambiente de teste, e
   ninguém investigou por quê. "Parar de crescer" só é seguro se a
   marca estiver certa: numa colônia viva marcada por engano, a decisão
   silencia a construção sem que nada avise.

   Investigar o E9 é mais barato que implementar a decisão e descobrir
   isso depois. É o passo que vem antes da TASK-048.
```

```text
Ícone e nome divergem                    2026-08-07,
                                         resolvido em 2026-08-14

   A arte dizia "Village++"; o mod é "Village Colony", id
   villagecolony. A decisão de 08-07 foi manter o id — trocá-lo
   quebraria saves, porque ele nomeia o arquivo .dat, o caminho
   do ícone e o logger.

   Em 2026-08-14 a divergência saiu pelo outro lado: o nome foi
   removido da arte. A placa de madeira ficou lisa, reconstruída
   com a própria textura da tábua, e o galhardete abaixo dela
   continua onde estava. O id não foi tocado.
```

```text
A regra da colheita                      2026-08-08

   derrubar a árvore inteira, troncos ligados, até o teto de 24;
   recolher tudo, com o baú consultado antes — árvore que não
   cabe fica de pé, porque tronco sai do mundo sem drop;
   só então replantar; e abrir a coluna acima da muda, que é uma
   exceção estreita à regra "folha não é alvo".

   Em 2026-08-12 ganhou uma quinta parte: tronco sem copa viva
   não é árvore, é construção.
```

```text
As duas regras do autor                  2026-08-08

   Colher até os baús encherem, e colher no tempo de um jogador
   com ferramenta de ferro. Enunciado em §18, implementadas em
   2026-08-11.

   Substituíram a meta constante de ColonyGoals, que era o que
   mantinha a fila de tarefas crescendo para sempre (E1).
```

```text
Propriedade do baú                       2026-08-08

   Linha livre entre a cama e o baú, no mesmo nível. Parede
   desqualifica. Ver §9.
```

```text
Depositar de 32 em 32 — cancelada        2026-08-08

   A TASK-026 previa o trabalhador voltar para casa a cada 32
   troncos. A madeira vai direto para o baú do trabalhador, e a
   tarefa perdeu o motivo. Registrada aqui porque MVP-Tasks.md
   ainda a lista.
```

```text
Quando duas vilas viram uma              2026-08-12

   Não é distância. Duas vilas viram uma quando um bloco de uma
   encostar no bloco da outra — quando a construção as junta. A
   fusão não reduz trabalhadores: a vila resultante fica com os
   de ambas.

   Nada disto está implementado; depende da construção existir.
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

World.getBlockEntity chamado de dentro do evento de chunk

  a thread do servidor travou; o terreno parou de carregar

Duas premissas erradas sobre o mundo, na regra de encolhimento

  margem de 32 blocos numa vila maior que isso;
  e colônia que nasce do save, não criada na hora

getBlockState forçando chunk, de novo, na Fase 8

  o mesmo erro de dois dias antes, no primeiro código
  que escreve no mundo. O gametest não podia pegá-lo:
  a estrutura de teste está toda carregada

Task.complete numa tarefa RESERVED, na Regra 1

  exceção dentro do tick do servidor, e o mundo cai.
  É o fim normal da regra — baú quase cheio, pedido do
  tamanho do espaço que sobrou, primeira árvore maior
  que ele. Passou por 284 testes de unidade e 44 de
  jogo verdes
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

```text
testar a partir do estado com que a sessão começa

  toda sessão depois da primeira lê a colônia do save.
  Um teste que sempre cria a colônia na hora exercita
  um estado que quase nunca acontece — foi assim que a
  âncora que nunca nascia passou por 238 testes verdes
```

```text
o teste alcançar a fronteira não basta — ele
precisa modelar o mundo que acontece

  os quatro testes da Fase 9 punham o tronco no baú
  do próprio fabricante, que é um estado que o jogo
  nunca produz sozinho: quem colhe deposita no baú
  dele. Setenta e seis testes de jogo verdes, e a
  primeira sessão real rendeu zero tábuas. Ver o E10
  do §17

  a pergunta que o teste tem de responder não é
  "este código funciona?", é "quem põe esta coisa
  aqui, em jogo?"
```

```text
usar número de vila de verdade nos testes

  a prova geométrica passava com cluster de três camas
  a dez blocos, e falhava em toda vila real. Vila do
  save tinha 38 camas
```

```text
rodar o teste novo contra a regra desligada

  não é cerimônia: é o que separa um teste que afirma
  de um que acompanha. Em 2026-08-12 esse passo achou
  um travamento de servidor que a bateria verde não
  achava — o teste falho expôs um caminho que o teste
  passando nunca percorria
```

```text
no gametest, colônia longe da estrutura é dormente

  a sonda e o ciclo só rodam para colônia ACTIVE, e
  ACTIVE quer dizer chunk ticando. Sem jogador, quem
  segura o chunk é forceload — e o pedido só vale no
  tick seguinte
```

```text
critério de verificação exige instrumentação que o
satisfaça, escrita junto com ele

  três critérios do roteiro V1–V7 eram inverificáveis
  como escritos: o estoque não distinguia baú vazio de
  ilegível, o encolhimento não tinha linha nenhuma, e o
  V4 pedia saber de quem era cada baú sem que nada
  dissesse isso
```

---

## Armadilhas de método já pagas

```text
Trocar o jar com o jogo aberto não testa nada.
```

O Minecraft carrega mods na inicialização da JVM. Sair ao menu e
reentrar no mundo reusa o código em memória.

Duas sessões de teste foram desperdiçadas assim.

---

```text
/time set day não é hora de trabalho do aldeão.
```

A agenda Vanilla do aldeão põe WORK no tique **2000** e o tira em 9000.
`day` é 1000 — antes da janela. Duas sessões de 2026-08-13 morreram
assim, uma inteira e outra no meio: o autor pôs 1000 e o lenhador que
estava cortando parou.

O que serve: `/time set noon`, que é 6000, ou `/time set 2000`. E olhar
a própria linha do lenhador, que diz `work time` ou `off hours` — ela
existe para responder isso sem adivinhação.

---

## Riscos ainda abertos

```text
Raio de detecção menor que a vila

  Mitigado por observedBeds, não eliminado.

  Uma vila muito grande pode nunca ser observada por inteiro.
```

```text
Dois aglomerados distintos a menos de 64 blocos

  São adotados como uma colônia só.

  Desde 2026-08-13 não é mais em silêncio: dois centros a menos
  de 32 blocos rendem o aviso da ADR-003 §5. O que o aviso não
  faz é resolver — para isso é preciso a fusão, que depende da
  construção existir.
```

```text
Duas vilas encostadas disputam trabalhador

  A vaga de profissão vale por colônia do registro, não
  por vila física. Com dois centros a 61 blocos e raio de
  varredura 64, um aldeão que mora numa pode estar
  registrado na outra.

  Quem fecha isso é a fusão por construção, decidida em
  2026-08-12 e ainda não implementada. Ver §10.
```

```text
Mixin no cérebro do aldeão

  VillagerEntityMixin injeta no TAIL de initBrain e só
  delega — a superfície mínima que a ADR-004 exige. Ainda
  assim é o cérebro do aldeão, e outro mod que mexa nele
  é o candidato natural a conflito.

  Nunca foi testado junto com outro mod de aldeão.
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

  docs/decisions/ADR-001..006  decisões; ADR-003 e ADR-004 têm emendas

  Architecture-Foundation.md   camadas

  Data-Model.md                modelos e campos

  Class-Architecture.md        classes; layout delegado à ADR-006


Sistemas

  Simulation-Loop.md           ciclo da colônia; implementado em
                               core/coordination/ColonyCycle

  Profession-System.md         profissões

  Resource-System.md           recursos

  Storage-System.md            armazenamento

  Construction-System.md       construção (Fase 10, não iniciada)

  Save-Data-System.md          persistência


Execução

  MVP-Tasks.md                 tarefas, com o estado de cada uma;
                               o detalhe por tarefa fica no §6 deste
                               documento

  docs/technical/Fabric-Version.md   versões fixadas

  docs/technical/Performance-Rules.md

  docs/technical/Testing-Strategy.md

  docs/technical/Debugging-Strategy.md

  docs/technical/Vanilla-Integration.md

  docs/technical/Project-State.md    este documento — o estado

  docs/technical/Development-Log.md  o histórico, por data

  docs/technical/Backlog.md    o que está feito e o que falta, num
                               índice só. Não decide nada: onde ele e
                               este documento discordarem, vale este


Referência externa

  docs/workers-analysis/       a engenharia reversa do mod Workers,
                               em doze documentos. É estudo, não
                               decisão: nada ali obriga este projeto a
                               nada, e o §12 de lá diz por que o MVP
                               vem antes de qualquer item da lista.

                               A pasta workers-maingit/ que o originou
                               é All Rights Reserved e está no
                               .gitignore. Não commitar.
```

---

## Ressalvas conhecidas nos documentos

```text
MVP-Tasks.md

  As quatro ressalvas que viviam aqui — o nome ColonyManager, a
  TASK-008 fora de ordem, a TASK-012b fora da numeração e a
  TASK-026 cancelada — foram para dentro do próprio arquivo em
  2026-08-13, cada uma junto da tarefa a que pertence.

  Aquele documento passou a carregar o estado de cada tarefa.
  Onde ele e este §6 discordarem, vale este: é o que se atualiza
  ao fim de cada sessão.
```

```text
Construction-System.md

  Três coisas que o documento descreve e o código não faz, todas
  conhecidas e assumidas em 2026-08-14:

  §PREPARING descreve limpeza de grama, flor, folha e neve. A TASK-047
  fechou em 2026-08-15, e fechou pelo outro lado: em vez de um estado
  que limpa, o scanner passou a não enxergar a cobertura do campo como
  obstáculo, e quem a remove é o construtor ao escrever o bloco no
  lugar.

  O que fica de dívida, e é cosmético: onde o projeto pede ar — o
  interior dos cômodos — nada é escrito, e a grama fica dentro da casa.

  Folha continua fora, por decisão de 2026-08-15: aceitá-la faria a
  colônia escolher lote debaixo de copa.

  §Estradas manda construir a rua antes da casa. O código só usa rua
  que já existe, e não constrói rua nenhuma — o que obedece à ordem
  ("nunca casa isolada") por um caminho mais estreito. Ver TASK-043.

  §"Modelo de Construção" prevê `rotation`. O modelo não tem: o MVP
  levanta a casa como ela está no arquivo, e girar só faz sentido
  quando houver rua com direção.
```

```text
Initial-Setup-Checklist.md §6 e Class-Architecture.md

  Continham layouts de pacote divergentes. Agora apontam para

  a ADR-006 em vez de repetir a estrutura.
```

---

# 14. Onde retomar

## O estado em 2026-08-15, de madrugada

```text
Fases 1 a 9    completas e verificadas em jogo
Fases 10 e 11  escritas e cobertas por teste. O caminho até a obra
               foi desentupido em quatro sessões, e bloco nenhum
               foi visto sendo posto
itens A e B    cobertos por teste, nunca vistos em jogo
item C         verificado em jogo em 08-14
E11            rodízio de profissão, visto em jogo, à espera de
               decisão do autor (TASK-049)

366 testes unitários + 80 de jogo, verdes
árvore limpa, tudo empurrado para origin/main
```

O que as quatro sessões fecharam, em ordem: o fabricante lia o baú
errado (E10); o jar rodado era velho (E13); a Fase 10 não dizia por que
não construía (E14, primeira metade); a linha que passou a dizer
afirmava mais do que sabia (E14, segunda metade); e a grama do campo
reprovava todo lote de planície (TASK-047, que fechou o E14).

---

## As três coisas que esperam, em ordem

```text
1  ver a casa subir               §8, P1

   É o último passo do MVP, e o único que nunca rodou. O que olhar:

     "planned ... — 151 blocks, N builders"
     "builders: 1 working, BUILDING at ..., N blocks left"
     e depois a casa virando infraestrutura

   O que a sessão precisa ter: um construtor na vila, /time set
   noon, e **pedra e vidro nos baús** — a colônia produz tábua e
   nada mais, e a casa pede 43 de pedra. Sem isso a obra fica em
   WAITING_RESOURCES, que é o comportamento certo e não uma casa.

   Se voltar "planned no building", a linha diz qual dos cinco
   motivos é. O que ninguém sabe ainda é se as vilas do autor têm
   lote de verdade — a grama deixou de recusar, e sobrar lugar é
   outra pergunta.

   O jar precisa ser trocado com o jogo fechado. Ver §11.

2  decidir o E11                  §8, TASK-049

   O rodízio de profissão mexe na Regra 4 e espera o autor.

3  o que ficou da Fase 8          §8, P2 e P3

   A metade estrutural da Regra 3 e o lado do cliente: nome,
   rachadura e braço.
```

---

## Como rodar, sem procurar

```text
build e testes     JAVA_HOME="$HOME/.jdks/jdk-21.0.12+8" ./gradlew build

testes de jogo     JAVA_HOME="$HOME/.jdks/jdk-21.0.12+8" ./gradlew runGametest

jar para jogar     build/libs/village-colony-0.1.0.jar
                   → %APPDATA%/.minecraft/mods/
```

O PATH da máquina tem Java 8; sem o `JAVA_HOME` explícito o Loom recusa.

Trocar o jar com o jogo aberto não testa nada, e `/time set day` não é
horário de trabalho do aldeão — as duas armadilhas estão no §11.

---

## Ao encerrar uma sessão, atualizar

```text
§3   Current Phase        se a fase mudou

§6   Implementation        a linha da tarefa, e a contagem de testes

§8   Priority Queue        o que entrou, o que saiu

§17  Erros conhecidos      o que se descobriu, sem inventar causa

§18  Regras vigentes       regra nova do autor, com a data

Development-Log.md         a entrada da sessão, no fim do arquivo
```

---

# 15. Development Log

O log cronológico mora em `docs/technical/Development-Log.md`.

Saiu daqui em 2026-08-12: eram mais de 4700 linhas, quatro quintos deste
documento, e o §16 pergunta se quem abre o projeto hoje sabe onde as
coisas estão. As entradas continuam identificadas pela data, e as
referências a "a entrada de §15 de 2026-08-07" espalhadas pelo código
continuam válidas — é lá que elas estão.

Entrada nova vai para o fim daquele arquivo. Este documento guarda o
estado; aquele guarda como se chegou nele.

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

---

# 17. Erros conhecidos

> **A lista viva mora no [`TODO.md`](../../TODO.md)** desde 2026-08-22,
> organizada por nível de progressão lógica: resolvidos, erros abertos,
> pendências por nível, incompatibilidades e decisões que faltam. Esta
> seção guarda o histórico e o detalhe de cada erro; onde as duas
> discordarem, vale o TODO.
>
> Quatro nasceram em 2026-08-22, das três sessões de jogo, e dois
> fecharam no mesmo dia:
>
> ```text
> E18  ninguém funde pedra          ✅ fechado — Production declarada
>                                      no recurso, e o fundidor
>                                      perguntando ao livro de receitas
> E19  MinerWork cruzou o teto      ✅ fechado — corte por
>                                      responsabilidade: BuilderApproach,
>                                      BuilderReport, MinerHaul
> E20  o guarda de travamento       ⚠️ ABERTO. Não reproduzido em 12
>                                      rodadas, e isso não é diagnóstico
> E21  a pedra que não chegou       ⚠️ ABERTO. Instrumentar primeiro
> ```
>
> **O E20 merece a ressalva por escrito.** Duas hipóteses caíram na
> investigação de 08-22: o relógio compartilhado do mundo — que não
> explica nada, porque as três horas usadas pelos testes estão todas
> abaixo do `DUSK` — e o limite global de travamento, que só um
> teste mexe. O que se sabe é que doze rodadas não o viram.

Registrados com o que se sabe, e sem inventar causa para o que não foi
investigado.

O texto integral de cada um, com a investigação que o fechou, está em
`Development-Log.md`, na data indicada.

---

## Abertos

### E3 — Sobra de colheita é perda de item

Se o baú encher no meio da colheita, o que não coube é registrado em
WARN e perdido: o bloco já saiu do mundo e o item não vira drop no chão,
por decisão do autor.

O espaço é conferido antes de derrubar, e a conferência cobre o tronco
inteiro — que é a parte determinística. O que a folha dá é sorteado na
hora e não dá para prever. São poucos itens e o espaço do tronco sobra
para eles, mas a perda é possível.

A Regra 1 do §18 torna isto raro por construção: um trabalhador que só
colhe quando há espaço quase nunca chega ao limite no meio.

---

### E4 — `path held: no` e o aldeão chega assim mesmo

```text
[05:32:14] heading to the tree — 12 blocks away, work time: yes,
           path held: no, doing: idle
[05:32:44] felled 6 logs
```

No instante da leitura, a memória `WALK_TARGET` já tinha sido descartada
pelo Vanilla, e o aldeão chegou. A explicação provável é que a reposição
a cada tick — e não a primeira escrita — é o que faz o caminho
acontecer.

Provável, não verificado. Fica registrado porque, se um dia o aldeão
parar de chegar, esta linha é o primeiro lugar a olhar.

---

### E5 — Colheita de outras espécies nunca aconteceu em jogo

Só a derrubada de carvalho foi vista em jogo, em 2026-08-08 às 05:32:44.
As outras sete espécies da tabela existem em código; o gametest exercita
carvalho e bétula, e nenhuma outra tem caso próprio.

O mangue é o mais provável de falhar primeiro: o que se replanta ali é
propágulo, e ele quer lama ou água rasa. A regra confia em `canPlaceAt`,
que é a resposta certa, mas isso nunca foi visto acontecendo.

---

### E8 — A orientação dos blocos (metade fechada em 2026-08-15)

O `Blueprint` guarda o nome do bloco e descarta o estado. Isso custava
duas coisas, e elas se separaram.

**As duas partes — fechado.** Porta e cama ocupam dois blocos ligados
por uma propriedade, e o mod punha duas metades independentes no estado
padrão: duas metades de baixo empilhadas, dois pés de cama lado a lado.
E cobrava duas peças do baú por um item.

Resolvido na leitura e na escrita, e **sem a ADR que esta entrada
supunha necessária**: a metade de cima é descartada por
`StructureBlueprintReader.isSecondHalf`, e `BuilderWork.placeSecondHalf`
escreve a outra com a propriedade que as liga. O Core não mudou uma
linha — quem sabe o que é "metade de cima" é a fronteira, que é onde
esse conhecimento sempre pertenceu.

**A orientação — aberto.** Escada e porta continuam saindo no padrão, e
a cabeceira da cama vai para onde o estado padrão aponta e não para onde
o arquivo dizia. Nunca foi visto em jogo, porque a Fase 10 nunca rodou.

Tarefa própria: TASK-046, agora só sobre orientação. A decisão que ela
pede continua sendo a mesma — levar `BlockState` para o Core contra a
ADR-005, ou inventar uma linguagem de propriedades lá dentro —, e agora
ela vale por menos: uma casa com escada virada para o lado errado é
feia; uma casa com a porta partida não era casa.

---

### E9 — Colônia marcada ABANDONED e desmarcada no ciclo seguinte

Observado na bateria de gametest em 2026-08-14:

```text
Colony 20c744a6 is inhabited again — now STABLE
```

Precedido, em algum ciclo anterior, da marcação de abandono. A colônia do
mundo de teste oscila entre ABANDONED e STABLE.

A explicação provável é o mundo do gametest: as vilas dos testes se
sobrepõem, e a sonda de uma colônia às vezes não alcança as camas que a
definiram. **Provável, não investigado.** Em mundo de verdade não há
motivo conhecido para isso acontecer.

Fica registrado porque, se em jogo aparecer a mesma oscilação, esta linha
é o primeiro lugar a olhar — e porque hoje ABANDONED não muda nada, o que
esconderia o sintoma (ver TASK-048).

---

### E15 — O fabricante não fabricou numa sessão de 5h40m

Observado na sessão de 2026-08-15: **zero** linhas `manufacturers:` em
cinco horas e quarenta minutos, com três fabricantes de baú
reivindicado e cerca de 2.900 tábuas guardadas na colônia.

A meta da Regra 5, sem obra ligada, é metade do que os baús comportam em
tábua. Com 2.900 guardadas é possível que a meta já estivesse satisfeita
e que a ausência seja o comportamento certo — a Regra 5 mandando parar.

**Não investigado, e não se inventa causa.** Fica registrado com o que
se sabe, e é o próximo lugar a olhar depois que a casa subir: a mesma
sessão que provar a Fase 10 vai dizer se o fabricante volta a trabalhar
quando a obra passar a puxar a meta.

---

### E11 — Rodízio de profissão a cada ciclo (fechado em 2026-08-15)

**Fechado.** A causa era de contagem, e estava escrita no javadoc do
próprio `ChestScanner.hasFreeChest` desde que ele nasceu: *"é uma
preferência, não uma promessa: dois candidatos podem enxergar o mesmo
baú livre, e só um fica com ele."*

Quem contava contava **candidatos**. Três aldeões do mesmo cômodo
olhando para um baú davam três dispensas, uma reivindicação e dois
trabalhadores novos sem baú — e no ciclo seguinte, a mesma troca. Foram
689 vezes na sessão de 5h40m de 2026-08-15.

`freeChestFor` passou a dizer **qual** baú, e a varredura conta baús
distintos. Não visto em jogo: o que a próxima sessão deve mostrar é o
silêncio.

O texto original fica abaixo, porque o caminho é a parte que ensina.

---

### E11 — como era, antes de fechar

Observado na sessão de 2026-08-14, na colônia `9a5afa23`: nove
dispensas em dezesseis minutos, uma por ciclo, cada uma seguida de
`Assigned 1 professions`.

```text
Colony 9a5afa23 dismissed 1 workers (1 of them had no chest and lost
the job to someone who can get one) — at most 2 of each profession
Assigned 1 professions in colony 9a5afa23
```

A colônia dispensa quem não conseguiu baú "em favor de quem consegue",
atribui a vaga a outro, e o outro também não consegue baú — e no ciclo
seguinte a mesma troca acontece de novo. O fabricante `fb3640ae`
apareceu como `no chest` do começo ao fim da sessão, então quem gira é
a vaga ao lado dele, não ele.

Não trava nada e não perde item. O que custa é trabalho jogado fora por
ciclo, uma linha de log por ciclo, e trabalhador trocando de função sem
que nada no mundo tenha mudado — que é a forma do E1 visto de um
terceiro ângulo.

**A correção mexe na Regra 4**, e por isso não foi feita: dispensar
quem não tem baú só faz sentido se o substituto puder conseguir um, e
saber isso antes de atribuir é decisão de regra, não de implementação.
Tarefa própria: TASK-049, em §8, sob decisão do autor.

---

### E14 — A Fase 10 não abria obra: a grama do campo (fechado)

Fechado em 2026-08-15, 01:08, depois de três sessões. O texto abaixo
fica inteiro porque o caminho é a parte que ensina — duas das três
sessões foram gastas com o instrumento, não com o defeito.

**A resposta final.** A varredura completa o raio e não acha, e o
motivo é `groundInColumn`: ela devolvia o bloco mais alto que não fosse
ar, e em planície esse bloco é o tufo de grama. `flatGroundAt` recusava
a coluna porque tufo não é chão. Um lote de sete por sete precisa das
quarenta e nove colunas limpas, e em planície nenhuma está.

`Construction-System.md §PREPARING` sempre mandou limpar grama, flor e
neve. O código pulava o estado alegando que o lote só é aceito quando
não há nada em cima dele — a alegação era verdadeira, e era o defeito.
Era a TASK-047, registrada no §13 por outro lado e sem ninguém saber
que ela bloqueava a Fase 10 inteira.

Corrigido: `isNothing` trata ar, cobertura do campo e camada de neve
como nada. Folha fica de fora por decisão — aceitá-la faria a colônia
escolher lote debaixo de copa.

**A obra ainda não foi vista subindo.** O que se sabe é que o lote
deixou de ser recusado por grama; que exista lote de verdade nas vilas
do autor é a próxima sessão que diz.

---

### E14 — como era, antes de fechar

Sessão de 2026-08-14, 23:41, com o jar certo — `Loaded 3 colonies with
80 workers, 0 buildings and 0 projects to resume`. Nove minutos, três
colônias, quatro construtores com baú, 256 tábuas de carvalho guardadas.

Nenhuma linha da Fase 10. Nem obra, nem recusa, nem canteiro.

`ConstructionPlanner.plan` roda todo ciclo e tinha **cinco** saídas
silenciosas: obra já aberta, sem construtor, o jogo sem a casa, sem
lote, e lote sobre construção da colônia. Do lado de fora as cinco
davam o mesmo silêncio.

O que já se sabe eliminar sem sessão nova: construtor existe — quatro
`BUILDER ... claimed the chest` no mesmo log —, e a leitura da casa de
planície tem gametest verde desde a TASK-031.

**O suspeito provável é o lote**, e ele tem motivo documentado: o §7
registra que a vila só constrói em beira de rua que já existe. Provável,
não verificado — e é exatamente o tipo de conclusão que este projeto não
aceita sem a linha que a prove.

Instrumentado em 2026-08-14, à noite. **Respondido em 2026-08-15,
00:28**, no primeiro ciclo da sessão seguinte:

```text
Colony 0c2771b0 planned no building — no free lot beside a road
within 64 blocks of ColonyPos[x=1109, y=64, z=730] that fits
ColonyPos[x=7, y=7, z=7]
```

Nas duas colônias. Dos cinco caminhos, é o do lote — como se suspeitava,
e agora não é mais suspeita.

**Mas a linha afirmava mais do que sabia**, e isso é a segunda metade
do E14. `BuildSiteScanner.find` devolve vazio em dois casos diferentes:
varreu o raio inteiro sem achar, ou o teto de colunas daquele ciclo
estourou no meio. A mensagem dizia o primeiro nos dois.

A conta: raio 64 são dezesseis mil colunas, mil por chamada, dezessete
ciclos para uma volta. A sessão teve quatorze. **Nenhuma das duas
colônias tinha varrido raio nenhum inteiro** quando o log afirmou que
não havia lote. A conta deixou de ser conta e virou teste em
`anUnfinishedSweepIsNotAnAnswer`.

Corrigido na mesma noite: `sweepPausedAt` lê o cursor que já existia, e
a mensagem passa a separar "não há lote" de "não terminei de olhar".

Respondido em 2026-08-15, 00:42, numa sessão de vinte minutos:

```text
00:42:59  still sweeping for a lot — the ring budget ran out
00:53:35  no free lot beside a road in the whole 64-block radius
00:54:05  still sweeping for a lot
01:03:05  no free lot beside a road in the whole 64-block radius
```

Duas varreduras completas por colônia, e a terceira já em curso. **A
varredura termina e não acha.** A causa está no topo desta entrada.

---

### E13 — A sessão rodou um jar velho, e ninguém percebeu na hora

O jar em `%APPDATA%/.minecraft/mods/` na sessão de 2026-08-14 era de
**08-13, 08:55** — anterior às Fases 10 e 11 e aos itens A, B e C.

Como se sabe, sem depender de data de arquivo: a linha de encerramento
da sessão foi

```text
Saved 3 colonies with 80 workers
```

e o código atual escreve

```text
Saved {} colonies with {} workers, {} buildings and {} open projects
```

O formato mudou quando a persistência da obra entrou, em 08-14. A
linha antiga é a prova de que o código de 08-14 não estava rodando.

**O que isso invalida e o que não invalida.** Não invalida o E10: o
fabricante é código de 08-13, está no jar velho e no novo sem mudança,
e o defeito é real — a correção continua de pé. Invalida a conclusão
que se ia tirar do silêncio das Fases 10 e 11: não houve linha
"planned" nem construtor trabalhando porque **esse código não estava
lá**, e não por falta de material. Nada se aprendeu sobre elas.

O §11 já registra "trocar o jar com o jogo aberto não testa nada".
Esta é a variante mais simples e mais fácil de cometer: o jar não foi
trocado. E o log **diz** qual versão está rodando, na linha de
carregamento — que é o primeiro lugar a olhar antes de concluir
qualquer coisa do silêncio de uma fase.

Fechado do lado prático em 2026-08-14 à noite: o jar novo foi
instalado. Fica aberto como hábito a criar — conferir a linha de
carregamento antes de ler o resto do log.

---

### E12 — `Equipped N workers` nunca apareceu em jogo

A linha existe e roda na bateria de gametest (`Equipped 2 workers in
colony ...`). Na sessão de 2026-08-14, com 80 trabalhadores em três
colônias, ela não apareceu nenhuma vez — nem `Named N workers`.

A explicação era o E13: o jar daquela sessão era de 08-13, 08:55, e a
ferramenta inicial entrou mais tarde naquele mesmo dia. O código não
estava lá.

**Fechado em 2026-08-14, 23:41**, com o jar certo:

```text
Equipped 4 workers in colony 9a5afa23
Equipped 4 workers in colony 0c2771b0
```

O item C do §8 está verificado em jogo. `Named` continua sem aparecer,
e a explicação provável é que as colônias vieram do save já nomeadas —
provável, não verificado, e sem consequência conhecida.

Fica registrado porque é exatamente o item C do §8 — a ferramenta
inicial — que continua sem ter sido visto em jogo, e porque a próxima
sessão pode confirmá-lo de graça: basta um aldeão ganhar profissão
nova.

---

## Fechados, mantidos por rastreabilidade

```text
E10 o fabricante lia o baú errado           corrigido em 2026-08-14

    A Fase 9 rodou em jogo pela primeira vez e não fabricou nada:
    dezessete tarefas encerradas com "no logs left in the chest",
    zero tábuas, com 134 troncos guardados na colônia e dois
    lenhadores derrubando o tempo todo.

    Nem o executor nem a torneira estavam errados sozinhos — eles
    discordavam sobre onde fica o estoque. ColonyGoals mede a meta
    da Regra 5 no ResourceTally da colônia inteira; convertOne
    tirava o tronco do baú do próprio fabricante. Como quem colhe
    deposita no baú dele, o baú de um fabricante nunca recebe
    tronco: a meta abria tarefa por ciclo para o executor encerrá-la
    no tick seguinte — o E1 pela porta que o comentário do
    ColonyGoals previu e não fechou.

    A retirada passou a ser da colônia, como já era a do construtor,
    e a tábua volta ao baú de onde o tronco saiu.

    Por que 76 testes verdes não pegaram: todos punham o tronco no
    baú do fabricante, que é um estado que o jogo nunca produz
    sozinho. A lição do §11 ganha uma linha — não basta o teste
    alcançar a fronteira, ele precisa modelar o mundo que acontece.
```

```text
E7  a proteção não consultava as construções  corrigido em 2026-08-14

    BlockProtection passou a perguntar a
    BuildingRegistry.isColonyInfrastructure. São três agora os que
    não se quebram: bloco de vila gerada, bloco do jogador, e bloco
    de casa que a colônia levantou. TASK-045, fechada.

    Não corrigiu dano observado — o mod só quebra árvore, e a regra
    da copa já a separa de construção. Fechou o furo antes da
    primeira demolição de outra natureza.
```


```text
E1  a fila de tarefas não esvazia          corrigido em 2026-08-11

    Meta constante gerava tarefa por ciclo, para sempre, e nada
    removia tarefa encerrada. Fechado pelas duas metades: a Regra 1
    tirou a meta constante, e purgeClosed — que existia desde a
    Fase 7 sem quem o chamasse — passou a ser chamado ao fim de
    cada ciclo.
```

```text
E2  a colônia nunca encolhe                corrigido em 2026-08-12

    Duas investigações erraram a causa antes da terceira acertar.
    A colônia guardava uma âncora de sonda só, e recebia leituras
    de duas: a sonda da vila vizinha apagava a da própria entre um
    ciclo e o seguinte, e a repetição nunca se confirmava.

    Agora só a sonda ancorada no centro desta colônia escreve na
    memória dela. Coberto por gametest desde 2026-08-12.
```

```text
D1  a marca do baú trocava de dono         corrigido em 2026-08-12

    A busca por quadro existente aceitava qualquer quadro dentro de
    uma caixa expandida, e dois baús a dois blocos compartilhavam o
    vão entre eles. Agora se pergunta em que bloco o quadro está
    pregado.
```

```text
D2  vaga de profissão entre vilas          não era defeito

    A linha de log atribuía o trabalhador à colônia que varreu, e
    não à do registro. A regra nunca teve furo; a linha é que
    mentia. O que sobra é o limite registrado em §11: duas vilas
    encostadas disputam trabalhador até a fusão existir.
```

```text
E6  o lenhador que não cortava             corrigido em 2026-08-13

    Eram duas causas somadas. A busca gastava suas mil colunas no
    anel 16 e recomeçava do centro, então floresta mais longe que
    isso era inalcançável — corrigido com o cursor de anel. E a
    regra da copa, ao recusar a construção mais próxima do centro,
    fazia a busca reencontrar a mesma construção para sempre —
    corrigido anotando o grupo recusado.

    Verificado em jogo: a colônia 1109,730 passou a derrubar, e as
    árvores dela estão nos anéis 20 a 25.
```

```text
--  travamento por tarefa RESERVED         corrigido em 2026-08-12

    Task.complete exige EXECUTING, e o lenhador que encerrava a
    tarefa antes de escolher a primeira árvore — porque ela não
    cabia no baú — lançava dentro do tick do servidor. Achado pelo
    teste rodado contra a Regra 1 desligada, não em jogo.
```

---

# 18. Regras vigentes do autor

As regras do autor sobre o comportamento da colônia, na ordem em que
foram decididas. O enunciado fica como foi dito; o que a implementação
de fato fez, e onde divergiu, está na entrada do Development Log da data
correspondente.

```text
Regra 1   colher até os baús encherem          08-08, feita em 08-11
Regra 2   colher no tempo de um jogador        08-08, feita em 08-11
Regra 3   o que nunca se destrói               08-13, feita em 08-13
Regra 4   dois trabalhadores por profissão     08-13, feita em 08-13
Regra 5   quanto fabricar                      08-13, feita em 08-13
Regra 6   como a vila cresce                   08-14, em implementação
Regra 7   o lenhador planta onde cortou        08-15, feita em 08-15
Regra 8   um baú ao lado de cada cama          08-15, a implementar
Regra 9   subir e descer para alcançar, e      08-15, a implementar
          poder voltar
Regra 10  o construtor fabrica o que a         08-15, a implementar
          expansão pede
Regra 11  uma de cada profissão em cada vila   08-15, fechada em 08-21.
                                               mecanismo — falta a garantia
Regra 12  o centro fica em bloco que existe    08-15, feita em 08-15
Regra 13  a obra do MVP é uma que a colônia    08-15, feita em 08-15
          consiga fazer
Regra 14  o construtor alcança o alto da obra   08-18, feita em 08-18
Regra 15  a estrada cresce com a vila           08-18, feita em 08-21.
Regra 16  espaço em volta de cada casa          08-18, meia feita
Regra 17  a casa se abre para a estrada         08-19, feita em 08-19
Regra 18  o dia inteiro é expediente            08-19, feita em 08-19
Regra 19  o lote fica no nível da estrada       08-19, feita em 08-19
Regra 20  cada vila constrói no estilo do seu   08-19, feita em 08-19
          bioma                                 e ampliada pela 26
Regra 21  toda casa nasce com cama, baú e       08-19, feita em 08-19
          lampião                               e ampliada em 08-20
Regra 22  o lote é livre no volume              08-19, feita em 08-19
Regra 23  o que já foi analisado se reanalisa   08-19, feita em 08-19
Regra 24  a vila de planície levanta a casa     08-19, feita em 08-19
          do jogo
Regra 25  a maior planta que couber no lote     08-20, feita em 08-20
Regra 26  a cadeia de produção, e a paleta      08-20, feita em parte
          de cada vila                          — ver o que ela não fecha
Regra 27  só o catálogo do jogo, e o            08-20, IMUTÁVEL, feita
          construtor aguarda o bloco            em 08-20. Desfaz partes
                                                das Regras 13, 21 e 25.
                                                EMENDA 1 em 08-26: abre
                                                para pedra só. EMENDA 2
                                                no mesmo dia: e para a
                                                madeira junto
Regra 28  a casa pequena do bioma, e a obra     08-20, PROVISÓRIA por
          não espera por porta, cama,           decisão do autor. Estreita
          lampião e baú                         a 27 enquanto se testa
                                                — ampliada no mesmo dia
                                                com tronco descascado,
                                                tocha e vidraça
Regra 29  a mina em escada, duas salas e a      08-20, feita em 08-20.
          galeria sem fim                       Substitui o mineiro de
                                                superfície do mesmo dia

Regra 30  o mineiro recolhe tudo o que cava;    08-22, feita em 08-22.
          a boca da mina ganha lanterna e um    Amplia a 29
          baú de minério, e ele transborda
          para o baú do mineiro

Regra 31  o fazendeiro planta o que tiver no    08-26, ENUNCIADA e por
          baú, põe a água, colhe o que está     escrever. Decisão do
          pronto e guarda no próprio baú        autor na mesma data
```

Duas previsões das primeiras se confirmaram e vale marcá-las: a fila que
não esvaziava — o E1 do §17 — morreu junto, e o lugar onde as duas
primeiras regras moram é de fato o mesmo.

---

## Regra 31 — o fazendeiro planta o que tem, e colhe o que está pronto

```text
o fazendeiro planta qualquer semente que possuir ou que esteja no baú
dele

ele coloca a água que a plantação precisa

colhe as plantações prontas

e guarda a colheita no próprio baú
```

Enunciada pelo autor em 2026-08-26, e **deliberadamente não implementada
ainda**. A decisão veio junto com a ordem: uma profissão nova inteira não
entra antes de a cadeia atual — mina, fundição, casa terminada — ter sido
vista funcionando até o fim numa vila.

**O que esta regra decide, e que estava em aberto desde a Fase 4:**
agricultura está **dentro** do escopo do mod. O fazendeiro tem nome,
enxada e baú desde então, e nunca teve trabalho; a pergunta "o mod planta
ou fazenda fica de fora?" fica respondida — planta.

**Quatro coisas que o enunciado já resolve**, e que não precisam voltar
ao autor:

1. **Qual semente.** A que houver — não há lista escrita no código, e
   por isso trigo, cenoura, batata e beterraba entram pela mesma porta.
   É a mesma disciplina da Regra 27: quem responde é o jogo, e não uma
   tabela daqui.
2. **De onde ela vem.** Do baú do fazendeiro, como o tronco vem do baú
   do fabricante. A colônia não inventa semente.
3. **A água é responsabilidade dele.** Terra arada seca não dá colheita,
   e por isso pôr água faz parte do trabalho, e não do cenário.
4. **Onde a colheita para.** No baú dele. É o mesmo caminho de todas as
   outras profissões, e é o que faz a comida virar recurso contável da
   colônia.

**O que o enunciado ainda não diz**, e vai precisar de decisão quando
chegar a hora de escrever: onde fica a lavoura — canteiro que a colônia
levanta, ou terra arada que já existe na vila gerada. A
`plains_small_farm_1` do catálogo do jogo é a resposta mais barata, e
ela depende da Regra 28 sair.

---

## Regra 1 — colher até os baús da colônia encherem

```text
o trabalhador colhe enquanto houver espaço nos baús da colônia

quando os baús enchem, ele para

quando o jogador tira alguma coisa e abre espaço, ele volta a colher
```

O que isso substitui: hoje a colônia quer 64 de madeira e 32 de pedra,
por um número fixo em `ColonyGoals`, e o comentário da própria classe já
dizia que a resposta real viria da expansão. A regra do autor é outra e
é melhor: a meta deixa de ser um número inventado e passa a ser uma
propriedade do mundo — o espaço que a colônia tem para guardar.

O que muda em código:

```text
ColonyGoals          deixa de devolver constante; a meta vira
                     "cabe mais?" em vez de "quanto?"

ResourceDemand       o déficit passa a ser espaço livre, não
                     diferença de contagem

ChestInventoryReader já sabe ler os baús; falta somar o espaço
                     livre, que ChestDepositor.freeSpaceFor já
                     calcula para um baú só

ColonyCycle          para de gerar tarefa quando não há espaço, e
                     volta a gerar quando houver
```

Efeito colateral bom: isso encerra o item 3 do que falta. A fila não
esvazia hoje justamente porque a colônia nunca se dá por satisfeita.

Ponto que precisará de decisão na hora de implementar: "os baús da
colônia" são os baús dos trabalhadores registrados, que é o que
`StorageRegistry` conhece hoje. Um baú comunitário da colônia não
existe ainda.

---

## Regra 2 — colher na velocidade de um jogador com ferramenta de ferro

```text
o trabalhador leva para quebrar um bloco o mesmo tempo que um
jogador levaria com ferramenta de ferro
```

Hoje a árvore inteira cai num tick, dentro do ciclo de 600. Uma árvore
de seis troncos e oitenta folhas desaparece no mesmo instante, o que é
visível e errado — e também é um pico de custo dentro de um tick, que é
o tipo de coisa que já travou este projeto duas vezes.

A conta do Vanilla, para bloco com a ferramenta certa:

```text
tempo em segundos = dureza × 1,5 ÷ velocidade da ferramenta

machado de ferro: velocidade 6
tronco:           dureza 2      → 0,5 s = 10 ticks
folha:            dureza 0,2    → machado não é a ferramenta certa
                                  da folha; a conta muda, e o valor
                                  precisa sair da própria fórmula do
                                  jogo, não de um número escrito aqui
```

O caminho provável: o trabalho do lenhador sai do ciclo de 600 ticks e
passa a ter um passo por tick, com um bloco em progresso e um contador.
Isso é o mesmo lugar onde a Regra 1 vai morar — as duas são a mesma
mudança vista de dois lados: quanto colher, e em quanto tempo.

Regra que não pode ser esquecida na implementação: o custo por tick tem
de continuar cabendo num tick. Um contador por trabalhador é barato; uma
varredura por trabalhador por tick não é.

---

---

## Regra 3 — o que o trabalhador nunca destrói

```text
nunca um bloco da vila original

nunca um bloco colocado pelo jogador
```

**A árvore é a exceção, e é a única.** O lenhador derruba árvore onde a
achar, inclusive dentro dos limites que o jogo registra para a vila —
sem isso não haveria colheita, porque vila de planície nasce cercada de
carvalho e boa parte dele cai dentro desses limites.

**Árvore que nasceu junto com a vila é derrubável como qualquer outra**
— perguntado ao autor em 2026-08-13 e confirmado por ele. A exceção é da
árvore, não do lugar onde ela está: estar dentro da vila não muda o que
um bloco é.

O que protege a casa não é esta regra, é a da copa — tronco sem folha
viva não é árvore.

---

### O que o mundo consegue responder, e o que não consegue

```text
vila original      o jogo guarda, por chunk, as peças de cada
                   estrutura gerada: a casa, o poço, a rua, o
                   lampião. A pergunta é por peça, não pela caixa
                   da vila inteira — a caixa cobre o campo aberto
                   entre as casas, e proibir o campo aberto
                   proibiria a colônia de trabalhar em casa

colocado pelo      o Minecraft não guarda quem pôs cada bloco. A
jogador            única marca é a folha: colocada à mão vem
                   persistent, nascida de árvore não
```

Para tudo o mais, o mod não tem como saber — e por isso a proteção real
é a inversa, e não mora numa lista de proibições: **o trabalhador só
quebra o que consegue provar ser floresta.** A regra da copa é o que faz
esse trabalho hoje.

Vila construída pelo jogador não tem estrutura registrada, e a primeira
pergunta responde "não" para ela. Não é buraco: é a segunda metade da
regra que a cobre, pela via inversa.

---

### Onde isto vive

`fabric/integration/BlockProtection` é a porta única. Hoje quem passa por
ela é só a limpeza da coluna da muda — o único bloco que a colheita
quebra sem que ele seja da árvore que ela planejou.

A porta existe para as fases seguintes: fabricar e construir vão tocar no
mundo, e a pergunta "posso quebrar isto?" tem de ser feita num lugar só.

---

## Regra 4 — dois trabalhadores de cada profissão

```text
a vila começa com dois lenhadores, dois fabricantes, dois
fazendeiros e dois construtores

os demais aldeões continuam os que já eram
```

Era um de cada desde 2026-08-12, e antes disso a vaga era ilimitada — a
vila de 43 aldeões do autor acabou com seis lenhadores disputando tarefa
a cada ciclo.

Oito trabalhadores numa vila de quarenta continua sendo uma minoria
empregada, e é esse o ponto: a vila continua sendo a vila do jogador, com
a colônia dentro dela.

A colônia cobre as quatro funções antes de dobrar qualquer uma. Preencher
por ordem daria dois lenhadores antes do primeiro fabricante, e uma vila
com dois lenhadores e nenhum construtor é pior do que uma com um de cada.

O teto vale também para save antigo: quem excede perde a função no
primeiro ciclo, devolve o baú e volta a ser candidato à próxima vaga que
abrir.

---

## Regra 5 — quanto fabricar

**Decidida por delegação em 2026-08-13**, a pedido do autor ("resolve o
P3"). É a única regra deste capítulo que não saiu da cabeça dele, e por
isso é a mais fácil de derrubar: o enunciado abaixo vale até ele dizer
outra coisa.

```text
a meta de tábua é o que a obra pede

enquanto não houver obra, o fabricante enche metade do espaço de
armazenamento da colônia com tábua, e para
```

---

### Por que não foi a mesma resposta da Regra 1

A Regra 1 respondeu "quanto colher" com o espaço dos baús. A mesma
resposta para a tábua se destrói sozinha:

```text
um tronco vira quatro tábuas

fabricar aumenta o volume guardado, não diminui

"fabricar até encher" transformaria toda a madeira da colônia em
tábua e pararia a coleta junto — o baú cheio é o que faz o lenhador
parar
```

Metade e metade mantém as duas coisas vivas: o lenhador tem para onde
colher, e o fabricante tem o que fazer. E a metade é medida no mundo — a
capacidade dos baús que a colônia tem —, não é uma quantidade inventada.

Quando a Fase 10 trouxer a obra, a demanda dela substitui o teto: o que a
obra pede vira a meta, e a metade deixa de ser teto e passa a ser só o
lote de partida.

---

### O que a implementação de 2026-08-13 fez

```text
ColonyGoals          a meta de tábua é (guardado + o que cabe) / 2,
                     e é zero enquanto não houver tronco guardado:
                     não se pede o que não há com que fazer

ManufacturerWork     tira do baú, fabrica pela receita do jogo e
                     devolve — tudo no mesmo tick, para nada ficar
                     na mão de ninguém

a torneira por       a meta entrou depois do executor. Tarefa aberta
último               sem quem a execute fica reservada para sempre,
                     e é o que o §11 ensina
```

A parte da obra continua para a Fase 10: quando ela existir, o que ela
pedir substitui a metade.


---

## Regra 6 — como a vila cresce

**Decidida pelo autor em 2026-08-14**, respondendo às três perguntas que
o §7 guardava desde a abertura da Fase 10, mais a confirmação da Regra 5.

```text
o que se constrói    a casa de planície do próprio jogo, lida da
                     estrutura Vanilla

onde se constrói     estrada primeiro, casa ligada a ela. Nunca casa
                     isolada com estrada depois

quando para          não para por regra: constrói enquanto houver
                     material e espaço

a Regra 5            confirmada. Quando a obra existir, o que ela pede
                     vira a meta de tábua; a metade do armazém deixa de
                     ser teto e passa a ser o lote de partida
```

---

### O que cada uma escolhe, e o que descarta

**A casa é do jogo, não do mod.** É a mesma escolha que a Fase 9 fez com
a receita: perguntar ao Minecraft em vez de escrever a resposta. A casa
combina com a vila porque é a casa da vila. O projeto próprio foi
descartado por criar uma segunda fonte de verdade e uma casa que destoa.

**A estrada vem primeiro**, como Construction-System.md §"Estradas" já
mandava. É a opção cara — exige achar a estrada existente e saber
estendê-la — e foi escolhida sabendo disso, contra a alternativa de pôr a
casa no primeiro terreno plano. O que se compra com o custo é o
crescimento que parece Vanilla, em vez de casas espalhadas.

**Nada faz a vila parar de crescer**, e o autor escolheu assim depois de
ver as duas alternativas: um teto de casas seria número inventado, do
tipo que a Regra 1 veio substituir, e "até todo aldeão ter cama" foi
descartado.

Fica registrado o que isto significa, sem suavizar: a colônia não tem
critério de parada próprio. O freio é o mundo — só se constrói onde a
estrada alcança e o terreno deixa —, e ele é um limite real mas não é um
número. Se um dia a vila crescer demais, é esta regra que muda, e o
lugar de mexer é a escolha do lugar, não o executor.

---

### O que a Fase 10 ainda vai ter de decidir sozinha

Coisas que a regra não responde e a implementação não pode adiar:

```text
a que distância da estrada     "ligada a ela" não diz encostada nem
                               a três blocos

quanto de estrada por vez      um trecho por casa? até onde o terreno
                               deixar?

terreno que não é plano        aplainar é destruir bloco natural, o
                               que a Regra 3 permite — mas até que
                               altura de desnível vale aplainar em vez
                               de procurar outro lugar
```

Estas serão decididas na implementação e anotadas no Development Log da
data, como as anteriores. Nenhuma delas troca o enunciado acima.

---

## Regra 7 — o lenhador planta onde cortou

```text
o lenhador sempre planta uma árvore no lugar onde ele cortou
```

Feita em 08-15. O replantio já existia e era preguiçoso: morava em
`startNextTree` e só acontecia quando o lenhador ia procurar a árvore
seguinte. Quem derrubasse uma árvore e perdesse o trabalho antes disso
deixava o toco sem muda — tarefa cancelada, baú fora do registro, guarda
de travamento. `closePlan` passou a fechar o plano em toda saída, e a
conta é a dos troncos: derrubado o último, o lugar é chão livre e a muda
entra.

Ver o Development Log de 08-15 e os dois testes de jogo pelo caminho do
trabalhador.

---

## Regra 8 — um baú ao lado de cada cama

```text
toda vila gerada pelo Minecraft ganha um baú ao lado de cada cama

cada aldeão fica vinculado a uma cama

e ao baú mais perto da sua cama
```

**Metade disto já existe.** `ChestScanner` amarra o aldeão ao baú mais
próximo da cama dele, dentro de um raio de 6, e a linha `Storage claimed
by X: bed ... chest ... (2,0 blocks apart)` é essa amarração acontecendo.
O que não existe é **criar** o baú quando não há nenhum — hoje o aldeão
sem baú por perto simplesmente fica sem baú, que foi o E16 de 08-15.

O que muda em código:

```text
ChestScanner       já acha o mais próximo da cama; nada a mudar
                   na escolha

um lugar novo      quem põe o baú que falta. A vila é varrida por
                   cama na detecção; é ali que a falta aparece

StorageRegistry    já registra o par trabalhador–baú
```

Decisões que a implementação precisa tomar, e que **não estão no
enunciado**:

**Decidido pelo autor em 08-15: o baú é exceção de geração.** Ele
aparece do nada, e a justificativa é que não é produção da colônia — é
completar o que o Minecraft gerou incompleto. A regra "a colônia não
cria recurso" continua valendo para tudo o mais: o que a obra consome
sai de baú, e nada do que o trabalhador produz vem do nada.

Isto precisa entrar no `Construction-System.md` §Regras de Arquitetura
como exceção nomeada, e não ficar só aqui — regra com exceção não escrita
é regra que se perde.

O que a implementação ainda decide:

```text
onde é "ao lado"        qual dos vizinhos da cama, e em que altura.
                        Precisa ser livre, válido e alcançável

vilas já geradas        "toda cidade gerada" lê-se como todas,
                        inclusive as que já estão no mundo. Então é
                        na detecção, e não na geração

cama sem espaço         o que fazer quando nenhum vizinho serve
```

Esta é a maior escrita no mundo do jogador que o mod já faria: um baú
por cama, em toda vila detectada. A ressalva do `Construction-System.md`
— bloco posto no lugar errado é dano que ninguém desfaz — vale aqui com
mais força do que na obra, porque a obra é um lote escolhido vazio e
isto é dentro da casa de alguém.

---

## Regra 9 — subir e descer para alcançar, e poder voltar

```text
na busca de recurso o aldeão sobe e desce quantos blocos forem
necessários para alcançar o recurso

de maneira que, ao ir, ele possa voltar
```

O que isso mira: o lenhador que para a sete blocos da árvore e não
chega. Foi visto em 08-15, dezesseis minutos parado, e é o G2 do
Backlog.

Hoje quem manda no caminho é `GoToWorkTargetTask`, escrevendo
`WALK_TARGET` na memória do cérebro Vanilla. Quem calcula o caminho é o
Vanilla, com o limite de passo dele — e árvore em encosta, em cima de
morro ou do outro lado de um barranco fica fora de alcance sem que nada
no log diga isso.

**Decidido pelo autor em 08-15: só navegação.** O aldeão anda por onde o
terreno deixa — sobe encosta, desce degrau, dá a volta. Não põe nem tira
bloco para chegar. Árvore inalcançável a pé deixa de ser alvo, e ele
procura outra.

A leitura recusada foi a outra: o aldeão pôr degrau, andaime ou escada
para alcançar. Ela faria o lenhador escrever no mundo — o que hoje só o
construtor faz — e reabriria a Regra 3.

Com isso o trabalho fica assim:

```text
antes de aceitar o alvo   conferir que existe caminho até ele, e de
                          volta. É a segunda metade do enunciado:
                          "ao ir, poder voltar" — não se jogar num
                          desnível que não sobe

alvo sem caminho          sai da escolha e o lenhador procura outro,
                          como já acontece com o tronco sem copa.
                          Não pode ser recusa permanente: o jogador
                          constrói ponte, e a árvore volta a valer

custo                     um cálculo de caminho é caro. Cabe no
                          orçamento de SEARCHES_PER_TICK, que já é
                          de uma busca por tick no servidor inteiro
```

Fecha o G2 do Backlog — o lenhador parado a sete blocos da árvore por
dezesseis minutos, em 08-15.

---

## Regra 10 — o construtor fabrica o que a expansão pede

```text
além de erguer a expansão, o construtor fabrica os itens que ela
pede — uma cama nova, um baú, uma tocha

se houver recurso em qualquer baú da vila, ele busca, fabrica o item
e o deixa guardado no baú

de onde ele é retirado na hora de pôr na estrutura
```

O que já existe: `CraftingLookup` sabe ler receita Vanilla, o
`ManufacturerWork` já fabrica tábua a partir de tronco, e
`ChestWithdrawer` já tira material de baú para o construtor pôr bloco.
As peças estão todas no lugar; o que falta é a cadeia.

**Decidido pelo autor em 2026-08-15**, as duas pontas:

```text
de qual baú ele tira    de todos. O construtor tem acesso a qualquer
                        baú da vila. Começa pelo mais próximo e vai
                        abrindo para o seguinte enquanto não juntar a
                        quantidade de que precisa

quem fabrica            o construtor é fabricante. Ele mesmo faz o
                        craft dos blocos que a construção pede — junta
                        o material dos baús, do mais perto para o mais
                        longe, e só fabrica quando tem a quantidade
```

Note o que isso muda em relação ao que existe. `takeMaterial` já
percorre todos os baús da colônia, mas na ordem em que
{@code WORKERS.ofColony} devolve — que não é distância. Passa a ser por
proximidade, e passa a **acumular**: tirar três de um baú e cinco de
outro para juntar oito é o caso normal, não a exceção.

E o construtor deixa de só consumir. Quando falta o bloco pronto mas
sobra o ingrediente, é ele quem fabrica — e não o MANUFACTURER, que
continua com a cadeia de produção geral da colônia.

O que a implementação ainda decide:

```text
em qual baú guarda      o do próprio construtor, ou o mais perto da
                        obra

quais itens             cama, baú e tocha estão no enunciado. A lista
                        cresce com o que a expansão pedir, e sai da
                        planta — não de uma lista fixa

receita de quê          quando falta bloco e falta ingrediente, até
                        onde descer na cadeia
```

---

## Regra 11 — uma de cada profissão em cada vila

```text
cada vila tem ao menos um aldeão de cada profissão do mod

LUMBERJACK, MANUFACTURER, FARMER, BUILDER
```

É o piso; a Regra 4 — dois por profissão — é o teto.

**Já satisfeita pelo mecanismo.** `ProfessionAssigner.vacancy` devolve a
profissão mais escassa que ainda tem vaga, e não a primeira da lista,
justamente para cobrir as quatro antes de dobrar qualquer uma. O
comentário do método já diz isso desde 08-13: "uma vila com dois
lenhadores e nenhum construtor é pior do que uma com um de cada".

As três ressalvas de 08-15, e o que 2026-08-21 fez com cada uma:

```text
vila com menos          ESCRITO. Sete profissões não cabem em três
empregáveis que         aldeões. O piso vira "tantas quantas
profissões              couberem", e o que a regra exige então é que
                        as três sejam três funções DIFERENTES —
                        dobrar uma antes de cobrir as outras é a vila
                        com dois lenhadores e nenhum construtor

a dispensa pode         VERIFICADO, e a resposta é sim: pedindo três
tirar o último          trocas a uma vila de um trabalhador por
                        função, três funções ficam vazias. A dispensa
                        não conhece o piso; ela conhece o número de
                        trocas que lhe pedem

nenhum teste            SETE TESTES, em ProfessionFloorTest. O teto
afirma o piso           continua com os seus, em ProfessionAssignerTest
```

**Onde a garantia mora, e por que ela mudou de lugar.** Quem segura a
Regra 11 não é a dispensa: é o número que a varredura passa a ela. Uma
troca precisa de duas coisas — um baú livre, que é a decisão do E11, e
**alguém para ocupá-lo**, que é este piso. O código passava a contagem de
baús livres, e ela funcionava porque cada baú livre entra na lista junto
com o candidato que o alcançou: nunca há mais baú que candidato.

Funcionava **por acidente do jeito de contar**. Agora tem nome —
`ScanResult.substitutes`, o mínimo entre os dois — e o dia em que os baús
livres passarem a ser contados de outra maneira, essa linha é o que
impede a vila de perder o último lenhador.

---

## Regra 12 — o centro fica em bloco que existe

```text
a posição do centro da colônia é marcada na horizontal e na vertical
dos blocos existentes
```

Decidida em 2026-08-15, e feita no mesmo dia.

Até aqui o centro era a **média** das posições das camas — um ponto
calculado que não precisa coincidir com coisa alguma. Três camas em L
têm média num lugar onde não há cama; camas em dois andares dão um `y`
no meio do forro. A colônia media distância a partir dali: a âncora que
deveria ser a coisa mais estável do sistema era a que menos existia.

A média continua sendo o alvo — ela é o que descreve onde a vila está. O
que mudou é que o centro passou a ser a **cama mais próxima dela**, e
cama é bloco: tem horizontal e tem vertical, as duas do mundo real.

O sino continua mandando quando existe, e pelo mesmo motivo: sino também
é bloco que existe.

```text
onde vale     o centro da colônia, em VillageDetector.evaluate

onde não      a recusa de aglomerado continua com a média crua. Ela
              não é âncora, é um "onde isto estava" — nada mede
              distância a partir dela
```

Quatro testes de unidade. Não fecha o G4 sozinho — a colônia ainda pode
mudar de centro recusando encolher —, mas tira do caminho a parte em que
o centro novo era um ponto no ar.

---

## Regra 13 — a obra do MVP é uma que a colônia consiga fazer

```text
a obra que o MVP precisa provar é feita só do que a colônia produz

o que a colônia não produz, o jogador guarda no baú — e o construtor
usa dali, sem distinguir de onde veio
```

Decidida em 2026-08-15 sobre a lista de compras da casa de planície,
medida na bateria naquele dia:

```text
149 blocos, 8 tipos            72 blocos, 2 tipos
 49 oak_stairs      tábua       71 oak_planks   tábua
 43 cobblestone     minerar      1 oak_door     tábua
 33 oak_planks      tábua
 16 stripped_oak    machado
  3 glass_pane      fundir
  3 wall_torch      carvão
  1 white_bed       lã
  1 oak_door        tábua

a casa do jogo                 a cabana da colônia
```

Sessenta e seis dos 149 blocos da casa pedem cadeias que este mod não
tem — minerar, fundir, tosquiar, descascar. A colônia parava no primeiro
pedregulho e ficava em `WAITING_RESOURCES` para sempre. Não por defeito:
a meta era impossível, e nenhuma quantidade de lenhador mudaria isso.

**As duas metades da decisão.**

A primeira: {@code ColonyHut} passa a ser a obra do MVP. Tábua e porta,
as duas saindo de tronco. A colônia levanta a cabana do começo ao fim
sem o jogador guardar nada — e é isso que faltava para o sexto passo do
MVP poder ser visto acontecendo.

A segunda: o que a colônia não produz **não vira profissão nova**. Vira
material que o jogador guarda no baú. `takeMaterial` já lê qualquer baú
da colônia e não pergunta de onde o item veio, então isso já funciona —
o que faltava era a colônia **dizer** o que está esperando, e é o que a
linha `waiting for X` do relatório do construtor passou a fazer.

A casa do jogo continua sendo o alvo bonito. O dia em que a colônia
minerar e fundir, ela volta — e o que muda hoje é só qual obra o MVP
precisa provar.

```text
o teste que segura     theColonyCanMakeEverythingTheHutIsMadeOf
                       afirma que a cabana só pede o que sai de
                       tronco. Pôr pedregulho nela derruba a bateria
                       antes de a sessão de jogo descobrir
```

---

## Regra 14 — o construtor alcança o alto da obra

```text
o construtor põe bloco em qualquer altura da obra, bastando estar
encostado na área de construção
```

Vista em jogo em 2026-08-18: parte da casa subiu, e parou. O motivo
está em `BuilderWork.REACH`, que vale 5 e é medido por
`isWithinDistance` — uma **esfera**. Um bloco a oito de altura fica
fora dela ainda que o construtor esteja com o pé no lote, e a obra
morre na altura do telhado sem nenhuma linha dizendo por quê.

A esfera é a forma errada de perguntar. O que importa é ele estar
**no lote**, não a que altura está o bloco:

```text
horizontal    continua valendo o alcance de braço — ele precisa
              estar em cima, ou colado, da coluna onde vai pôr

vertical      sem limite dentro da obra. Da fundação ao último
              bloco da planta, de pé no chão do lote

o que isso    o construtor não voa, não sobe andaime e não empilha
não é         bloco para subir. Ele fica no chão e a planta sobe.
              A Regra 3 continua fechada: ele só escreve o que a
              planta manda
```

O par disso é a linha: alvo recusado por alcance tem de aparecer no
relatório do construtor, como a árvore fora de alcance da Regra 9
aparece no do lenhador. Foi o silêncio que custou a sessão de 08-18.

**Feita em 2026-08-18**, nas três pontas:

```text
o alcance          `isWithinReach` mede só o plano — dx² + dz² ≤ 25.
                   A altura saiu da conta

para onde anda     `footOf` manda o construtor ao pé da coluna, na
                   altura da origem do projeto. Andar até o bloco em
                   si só servia enquanto a obra era rasa: mandá-lo a
                   uma posição no ar é pedir caminho que não existe,
                   e ele ficaria parado até o guarda devolver a
                   tarefa — a mesma roda por outra porta

a linha            `walking for N ticks without reaching the block`
                   no relatório do construtor, enquanto ele não
                   alcança

o teste que        theBuilderReachesTheTopOfTheWorkFromTheGround —
segura             uma torre de seis tábuas, com a do topo a cinco
                   de altura. Rodado contra a regra desligada: as
                   três primeiras sobem e a quarta nunca
```

---

## Regra 10, elaborada em 2026-08-18 — quem fabrica o quê

O enunciado de 08-15 continua de pé; o que 08-18 acrescenta é a
**divisão do trabalho**, que estava por decidir.

```text
o FABRICANTE faz o que a casa leva nas aberturas e dentro
    porta, janela (vidraça), cama, baú
    fabrica e guarda no baú, como já faz com tábua

o CONSTRUTOR faz o bloco estrutural que falta na hora
    tábua, escada, e o que mais a planta pedir
    junta o material dos baús e fabrica ali
```

A leitura: o fabricante trabalha por **estoque** — a colônia sempre
tem porta, cama e baú prontos, haja obra ou não. O construtor trabalha
por **falta** — quando o próximo bloco da planta não está em baú
nenhum, ele desce um degrau na receita e fabrica.

Isso não desfaz "o construtor é fabricante", de 08-15: ele continua
fabricando. O que mudou é que os quatro itens de mobília saíram da
conta dele e viraram meta permanente do fabricante, do mesmo jeito que
a tábua é hoje.

**A busca de material, que era a outra metade de 08-15:**

```text
ordem dos baús      do mais próximo da obra para o mais longe.
                    Hoje `takeMaterial` percorre na ordem de
                    `WORKERS.ofColony`, que não é distância

acumular            tirar 3 de um baú e 5 de outro para juntar 8 é
                    o caso normal. Hoje ele desiste no primeiro baú
                    que não tem tudo

quando fabricar     só com a quantidade inteira dos ingredientes em
                    mãos. Fabricar pela metade tranca o material num
                    item que não serve

onde guarda         o baú mais próximo da obra. O do próprio
                    construtor não serve: ele anda, e o baú não

até onde desce      um degrau por vez. Falta escada e sobra tábua:
na receita          fabrica. Falta escada e falta tábua: fabrica
                    tábua se houver tronco, e senão declara a falta
                    (`waiting for X`) em vez de tentar a cadeia toda
```

**A vidraça é a exceção honesta:** ela pede fundir, e a colônia não
funde (Regra 13). Enquanto não houver forno, o vidro é material que o
jogador guarda no baú — o fabricante monta a vidraça a partir dele,
mas não faz o vidro.

---

**A metade do construtor foi feita em 2026-08-19.** É a que destrava a
cabana, e a sessão de 2026-08-18 mostrou por quê: o relatório repetia
`waiting for minecraft:oak_door` com **154 tábuas guardadas**. A colônia
tinha tudo de que precisava e parava a um bloco do fim.

```text
a pergunta que      `CraftingLookup.billFor` procura a receita pelo
faltava             RESULTADO, e não pelo ingrediente. `resultOfOne`
                    respondia "o que sai deste tronco?"; ninguém
                    sabia perguntar "o que faz uma porta?"

o ingrediente é     a receita da porta aceita tábua de qualquer
escolhido pelo      madeira. A casa da grade vira um item concreto
que existe          consultando os baús — escolher a madeira que a
                    colônia não tem produziria uma lista de compras
                    impossível

os baús, enfim      `ColonyChests.nearestFirst` ordena por distância
por distância       até a obra. A ordem antiga era a de registro dos
                    trabalhadores, que não é distância nenhuma

e acumulando        `ColonyChests.withdraw` soma entre baús. Três
                    tábuas num baú e três em outro eram seis tábuas
                    que a colônia tinha e não conseguia usar

três conferências   receita conhecida com todos os ingredientes em
antes de gastar     baú; quantidade inteira de cada um; e lugar onde
                    o resultado caiba. Cada uma evita destruir
                    material do jogador — fabricar pela metade tranca
                    material num item que não serve, e fabricar sem
                    onde guardar gasta o ingrediente à toa

ter do que se faz   `hasMaterialForNextBlock` passou a contar a
conta como ter      fabricação. Sem isso a obra que dormisse por
                    falta de porta continuaria dormindo com o baú
                    cheio de tábua, porque quem a acorda pergunta ali
```

```text
os testes que      theBuilderMakesTheDoorTheWorkIsWaitingFor — baú com
seguram            seis tábuas, projeto de uma porta: a porta entra no
                   mundo, a tábua sai do baú, e as duas portas que
                   sobram da receita ficam guardadas

                   theMaterialIsGatheredFromMoreThanOneChest — três
                   tábuas em cada um de dois baús, e a porta sobe
```

**O que continua por fazer desta regra:** a metade do fabricante — porta,
janela, cama e baú por estoque, independentemente de haver obra. Ela
precisa de algo que a tarefa hoje não sabe dizer: *qual item*. A tarefa
carrega um `ResourceType`, que é a lista fechada do que a colônia conta,
e porta não está nela — nem deve estar, porque a lista sai da planta. É
o `ItemRequest` do backlog, e ele toca `Task`, que é o centro.

---

## Regra 15 — a estrada cresce com a vila

```text
quando não houver mais lote livre encostado em rua, o construtor
estende a rua — e o lote novo nasce na beira do trecho novo
```

Era a pendência declarada no cabeçalho de `BuildSiteScanner` desde
08-14: "a vila cresce enquanto houver beira de rua livre, e para
quando não houver". É esse "para" que esta regra tira.

```text
quem estende        o construtor, e é obra como outra qualquer —
                    abre projeto, consome material, aparece no
                    relatório

de onde             da ponta de rua mais distante do centro.
                    Estrada que cresce pelo meio racha a vila

quanto por vez      um trecho curto por casa. Rua que cresce
                    sozinha vira rua sem nada em volta — é a
                    decisão 2 de 08-14, e ela continua valendo

o que é o trecho    caminho de terra (`DIRT_PATH`) no nível do
                    chão, seguindo o eixo da rua que ele prolonga,
                    e parando onde o desnível passa de MAX_SLOPE

quando não dá       ponta de rua contra encosta, água ou lote
                    ocupado: tenta a ponta seguinte. Sem nenhuma
                    ponta boa, a vila para — e agora diz por quê
```

Ordem que não pode inverter: **estrada primeiro, casa ligada a ela**.
A regra do autor de 08-14 sobrevive inteira — o que muda é que a
estrada passou a ser algo que a colônia produz, e não só algo que ela
encontra.

**Feita em 2026-08-21**, e com duas escolhas que valem registro.

**A ponta sai da varredura que já acontece.** Procurar a ponta da rua
numa varredura própria custaria o raio de 64 inteiro — dezessete
passagens de mil colunas, oito minutos e meio de relógio — logo depois
da varredura de lote que acabou de falhar percorrendo exatamente as
mesmas colunas. Então quem acha a ponta é a busca de lote: ela já visita
cada coluna e já pergunta se aquilo é rua, e o que se acrescenta é uma
pergunta a mais nas poucas colunas que **são**. A ponta mais distante
fica guardada para quando a varredura terminar sem lote.

**Calçar não custa material**, como o Backlog decidiu, e não abre
projeto. O `Project-State` de 08-18 dizia "abre projeto, consome
material"; o Backlog dizia o contrário, e o TODO manda o Backlog vencer.
O que a regra ganha com isso é simplicidade: a colônia não fica
esperando terra num baú para poder crescer.

**O ritmo em jogo, e é lento de propósito:** a rua só cresce quando a
varredura de 64 blocos **termina** sem lote — dezessete ciclos, cerca de
oito minutos e meio. É o preço de a regra só agir quando a vila
realmente não tem mais onde construir.

---

## Regra 16 — cada casa com espaço em volta, e nem longe demais

```text
a casa nova nasce a uma distância boa das que já existem: nem tão
perto que atrapalhe a obra, nem tão longe que se solte da vila

e o lote precisa estar livre na largura, na altura e na profundidade
```

Hoje o `BuildSiteScanner` pergunta duas coisas do lote — se encosta em
rua, e se o chão é plano dentro da janela. Não pergunta o que há em
volta, nem o que há **em cima**. Daí sair casa colada em casa, e casa
com a copa de uma árvore dentro do telhado.

**As duas distâncias:**

```text
mínima     um corredor livre entre a parede nova e a parede mais
           próxima que já existe. Menos que isso e o construtor
           não passa em volta da própria obra

máxima     a casa nova encosta na malha da vila. Passar disso é
           fundar um bairro solto, e a Regra 12 já mostrou o que
           acontece quando o centro e a obra se separam — 65
           blocos, em 08-15
```

**O volume, e não a área.** O lote deixa de ser um retângulo de chão e
passa a ser uma caixa:

```text
largura e         o tamanho da planta, mais a margem do corredor
profundidade      em cada lado

altura            a altura da planta, mais um bloco de folga. E
                  livre de verdade: hoje a coluna reprova o que
                  estiver acima da janela, mas por acidente da
                  busca de chão, não por regra

o que ocupa       bloco sólido, tronco, folha, água, e construção
                  da vila. Grama, flor e neve não ocupam — é o
                  mesmo `isNothing` da TASK-047
```

Os números — quanto é o corredor, quanto é "encostar na malha" —
ficam para a implementação medir em jogo. O que a regra fixa é que
existem os dois limites, e que a pergunta é sobre volume.

---

## Regra 17 — a casa sempre com uma lateral na estrada

```text
toda construção tem uma de suas laterais voltada para a estrada, e é
por essa lateral que ela se abre
```

A metade da posição já existe: `siteBesideRoadAt` põe o lote colado na
rua e o faz crescer para longe dela. O que falta é a **orientação** —
hoje escada e porta saem no estado padrão da planta, olhando para onde
a planta olhava quando foi lida.

```text
a lateral da rua    é a face do lote que toca o `DIRT_PATH`. Já é
                    conhecida: é a `Direction` que
                    `siteBesideRoadAt` escolheu, e que hoje é
                    descartada depois de calcular o canto

a porta             na lateral da rua, virada para fora. Casa cuja
                    porta dá no mato é casa em que ninguém entra

a planta gira       os blocos com face — porta, escada, cama,
inteira             placa — giram junto com o lote. Girar a porta e
                    deixar a escada é pior que não girar nada

o que grava         a direção escolhida vira parte do projeto
                    salvo. Obra retomada depois de sair do mundo
                    precisa girar igual, senão a casa sai meio
                    torta
```

Fecha o item "orientação de blocos" do TODO, que estava solto em 🟡
sem dono nem critério: o critério é a rua.

---

## Regra 18 — o dia inteiro é expediente

```text
enquanto houver sol, o trabalhador está buscando recurso ou
trabalhando

a última hora de luz é dele para voltar para casa; a noite é para
dormir
```

Decidida e feita em 2026-08-19, sobre o que a sessão de 2026-08-18
mostrou: a colônia parava com o sol alto, e nada no relatório explicava.

**A causa, medida no jar de 1.21.1.** Quem respondia "é hora de
trabalhar?" era a `Schedule` do Vanilla, e a resposta dela é curta:

```text
villager_default        10  IDLE
                      2000  WORK     ← o expediente começava aqui
                      9000  MEET     ← e acabava aqui
                     11000  IDLE
                     12000  REST
```

São **7.000 tiques de trabalho num dia de 24.000**. Pior: o dia claro
vai até 12.000, então havia **3.000 tiques de sol** — um quarto da luz
— em que a colônia inteira parava de colher, de fabricar e de
construir, porque o aldeão tinha ido conversar no sino.

As linhas do relatório de 08-18 mostram isso acontecendo:

```text
23:55:15  lumberjacks: ... off hours,  stall 137/2400
23:55:45  lumberjacks: ... work time,  stall 195/2400
23:58:15  lumberjacks: ... work time,  stall 1789/2400
23:58:45  lumberjacks: ... off hours,  stall 2282/2400   ← e congela
00:00:45  lumberjacks: ... off hours,  stall 2282/2400
```

**O efeito colateral que ninguém tinha visto.** O guarda de travamento
da Regra 9 só conta durante o expediente — de propósito, porque quem
não pode andar não está preso. Com uma janela de 7.000 tiques, o
contador chegou a 2.282 de 2.400 e o expediente acabou. A árvore
inalcançável nunca foi marcada, e o lenhador voltava a ela no dia
seguinte. **A Regra 9 não fechava em jogo por causa da janela**, e
nenhum teste podia ter pego isso: a bateria roda em vinte e cinco
segundos, e o defeito é de escala de dia.

**O que passa a valer:**

```text
a janela           do amanhecer (0) ao anoitecer (11.000). São 11.000
                   de 12.000 tiques de luz, contra 7.000 antes

por que 11.000     é onde o Vanilla troca MEET por IDLE, uma hora
                   antes de mandar dormir. A última hora de luz fica
                   para voltar para casa: trabalhar até o escuro
                   deixaria o aldeão no mato quando os monstros
                   nascem, e a colônia perderia trabalhador por causa
                   da própria regra

criança            não trabalha. A Schedule do bebê não tem WORK em
                   hora nenhuma e dava isso de graça; ao deixar de
                   perguntar a ela, a colônia passa a dizer isso por
                   conta própria

pânico e           continuam vindo antes. Sino tocando ou incursão, o
esconderijo        trabalho espera
```

**Onde a regra mora.** O relógio — a janela pura — é
`core.coordination.WorkClock`, sem Minecraft nenhum. `WorkHours`, na
camada Fabric, acrescenta só o que depende do aldeão: criança, pânico,
esconderijo.

A separação não é cerimônia, e o motivo é uma armadilha real: a hora
do mundo é **global**, e a bateria roda testes concorrentes. A primeira
versão desta regra tinha um teste de jogo que virava a noite para
afirmar "à noite ninguém trabalha" — e derrubou três testes de lenhador
que rodavam junto. A janela se afirma fora do jogo; dentro do jogo só
se afirmam horas que estão **dentro** do expediente.

```text
os testes que      WorkClockTest — amanhecer, manhã, tarde, a borda
seguram            do anoitecer, a noite, e o relógio acumulado de
                   dez dias

                   WorkHoursGameTest — a tarde é expediente com um
                   aldeão de verdade, e criança não trabalha
```

**O que esta regra não resolve**, e ficou visível no mesmo log:

```text
o construtor       parado em `waiting for minecraft:oak_door` com 154
                   tábuas no baú. Ninguém fabrica a porta — é a
                   Regra 10, e ela continua por fazer

o lenhador em      o guarda agora alcança o limite, e a árvore
encosta            inalcançável passa a ser marcada. Se isso basta
                   para a vila do jogador, só a próxima sessão diz

o centro           trocando de âncora a cada 30 segundos entre 49
oscilando          camas e 7. É a ADR-003, e espera decisão do autor
```

---

## Regra 19 — o lote fica no nível da estrada

```text
a casa assenta no mesmo nível da rua em que ela encosta

de modo que se entre e se saia dela a pé, sem degrau que impeça
```

Decidida em 2026-08-19. É a condição que faltava para a Regra 17 servir
de alguma coisa: porta virada para a rua com dois blocos de degrau na
frente é porta que ninguém atravessa.

Hoje `flatGroundAt` aceita desnível de até `MAX_SLOPE` dentro do lote e
não compara nada com a rua. O lote pode ficar dois acima do caminho, e a
casa nasce numa varanda sem escada.

```text
o que passa a      todas as colunas do lote no mesmo nível, e esse
valer               nível igual ao do bloco de rua em que o lote
                    encosta. O piso da casa fica então na mesma altura
                    do topo da rua, que é por onde se anda

o que isso custa    lote perto de encosta passa a ser recusado. É
                    conservador de propósito: recusar é procurar
                    outro lugar, e aceitar é uma casa em que ninguém
                    entra

o que isso não é    nivelar o terreno. A colônia não terraplana — ela
                    escolhe onde já está plano. Terraplanar é escrever
                    no mundo do jogador fora da planta, e a Regra 3
                    não deixa
```

---

## Regra 20 — cada vila constrói no estilo do seu bioma

```text
a casa que a colônia levanta é feita da madeira da vila a que ela
pertence
```

Decidida em 2026-08-19, com a segunda metade explícita: **o mod passa a
aceitar vila fora da planície**. Sem isso a regra não teria o que variar
— `VillageScanner` descarta em silêncio todo aglomerado que não esteja
em PLAINS, e o estilo por bioma seria uma escolha entre uma opção só.

```text
a madeira sai       planície e vila de savana usam carvalho; taiga e
do bioma            vila nevada, pinheiro; savana, acácia. A cabana
                    deixa de ter `oak_planks` escrito nela e passa a
                    perguntar qual madeira é a desta colônia

por que madeira,    porque é o que a colônia sabe fazer. Copiar a casa
e não a casa        que o jogo gera para cada bioma foi considerado e
do jogo             recusado: a de planície pede 43 pedregulhos, 3
                    vidros e uma cama de lã, e nenhum aldeão deste mod
                    minera, funde ou tosquia. É a Regra 13, e ela
                    continua valendo

de onde sai a       do bioma do centro, perguntado ao mundo na hora
espécie             de planejar a obra. Não é gravada na colônia, e a
                    razão é que o receio não se sustentou: o ciclo só
                    roda para colônia ACTIVE, e ACTIVE quer dizer chunk
                    carregado. Guardar exigiria mexer no save para
                    responder o que o mundo já responde certo

deserto             vila de deserto não tem árvore. A colônia nasce,
                    contrata e conta recurso; construir, só quando o
                    jogador guardar madeira no baú ou quando existir
                    outra fonte. Isso é limite conhecido, e o
                    relatório precisa dizê-lo em vez de calar
```

---

## Regra 21 — toda casa nasce com cama, baú e lampião

```text
cada casa construída tem dentro, no mínimo, uma cama, um baú e um
lampião
```

Decidida em 2026-08-19, junto com a resposta para o que fazer quando
falta material — e essa metade é a que importa.

**O problema.** Dos três, a colônia só sabe fazer o baú:

```text
baú        8 tábuas                    ✅ sai de tronco
cama       3 tábuas + 3 lãs            ❌ lã pede tosquia
lampião    8 pepitas de ferro + tocha  ❌ ferro pede minerar e fundir
tocha      graveto + carvão            ❌ carvão pede minerar
```

Exigir os três para dar a casa por pronta reproduziria exatamente o
travamento que a Regra 13 corrigiu em 08-15: sem lã e sem ferro em baú,
nenhuma casa terminaria e a vila pararia de crescer.

**Decidido pelo autor: a casa termina, e a mobília entra quando houver
material.**

```text
a estrutura         paredes, teto, porta e baú. O baú é feito pela
termina sozinha     colônia, então a casa nasce com ele

cama e lampião      ficam como pendência da casa, não da obra. Assim
                    que aparecer lã ou ferro em qualquer baú da
                    colônia, o construtor os põe

a vila não para     obra terminada libera o lote seguinte. Uma casa
                    sem cama continua sendo uma casa, e continua
                    contando para a vila

o relatório diz     por casa, o que ainda falta lá dentro. Sem essa
o que falta         linha, "casa sem cama" e "casa que a colônia
                    esqueceu" são o mesmo silêncio — é o §11 de novo
```

A cama tem um efeito de segunda ordem que vale escrever: cama nova é
aldeão novo, e aldeão novo é trabalhador. A vila que ganha camas cresce
sozinha, que é o ciclo desenhado no README desde o começo.

---

## O teto de colheita decidindo o que é árvore — 2026-08-19

O autor relatou, depois da sessão de 02:10: *"o texto do lenhador não é
verdade, tem vários tipos de árvores que não foram cortadas"*. Estava
certo, e a causa não era a que parecia.

**O que a lista de espécies tinha.** As oito do Overworld —
`OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, CHERRY, MANGROVE`. Nenhuma
faltava, e acrescentar espécie não teria mudado nada. Nether e bambu
continuam de fora com motivo escrito na própria tabela.

**Onde a recusa nascia.** No log:

```text
Not a tree at 836, 100, -3429 — 24 logs without a living canopy
Not a tree at 836, 100, -3423 — 24 logs without a living canopy
Not a tree at 845, 102, -3427 — 24 logs without a living canopy
```

Vinte e quatro é `MAX_LOGS`, o teto de colheita. **Quando o número da
recusa é exatamente o limite, o limite é a causa.**

`connectedLogs` percorre o tronco em largura a partir da base e para no
teto; a copa era procurada a partir <b>desse grupo já cortado</b>. Num
abeto gigante ou num carvalho-escuro, 24 troncos são os seis níveis de
baixo de um tronco de vinte e tantos, e a copa fica muito acima do que o
grupo alcançou. Sem folha no grupo, a árvore virava "não é árvore" — e a
recusa é permanente, então aquela árvore saía da vida da colônia.

Há ironia no comentário que já estava lá: em 2026-08-12 alguém corrigiu
a *ordem* — a copa passou a ser procurada antes de o teto reprovar a
árvore — e escreveu que "era justamente a construção grande que escapava
do teste". A ordem foi corrigida; o **grupo** continuou sendo o
truncado.

**A correção: são duas perguntas, e agora têm dois limites.**

```text
MAX_LOGS = 24            o teto de trabalho. Continua cortando a
                         colheita em pedaços, e o pedaço que desce
                         são os troncos mais baixos

CANOPY_SEARCH_LOGS = 256 só para responder "existe copa viva ligada a
                         este tronco?". Não derruba nada
```

O custo é uma travessia maior, e ela acontece na escolha da árvore, que
é limitada a uma por tick no servidor inteiro.

**O que não podia cair junto** é a regra que protege a construção do
jogador: pilar de tronco sem copa continua não sendo árvore. Os dois
casos estão presos por teste, e o primeiro foi rodado com a correção
desligada — recusa a árvore alta, como em jogo.

```text
os testes que    aTreeTallerThanTheHarvestCeilingIsStillATree
seguram          aTallBareTrunkIsStillNotATree
```

---

## Regra 22 — o lote é livre no volume, e planta não conta

```text
não pode haver bloco no espaço interno da casa

se houver bloco acima da linha de base, dentro do espaço onde a casa
vai, aquele espaço não serve

flor e mato não impedem: quem constrói tira
```

Pedida pelo autor em 2026-08-19, depois de ver casa nascendo com coisa
dentro.

**O que estava errado.** O lote era julgado pelo **chão**. A coluna
respondia onde a casa assenta, e havia uma única pergunta sobre o alto —
um bloco acima da janela de busca reprovava a coluna. A janela tem dois
blocos; a casa tem sete. Tudo o que estivesse entre um e outro passava
despercebido, e a casa subia em volta do obstáculo: o construtor punha o
que dava e pulava o resto com `is in the way`.

**O que passa a valer:** cada coluna do lote, do piso ao último nível da
planta, precisa estar livre. É a pergunta sobre o volume que a Regra 16
já anunciava e que só agora tem código.

**A outra metade, e ela é do mesmo pedido:** planta não ocupa. Grama
alta, samambaia, flor e camada de neve não reprovam lote nenhum — e por
não reprovarem, precisam de alguém que as tire. Esse alguém é o estado
`PREPARING`, que existia no documento desde o começo e passava em branco.

```text
o que a preparação  o que o jogo considera substituível: mato, flor,
tira                neve. Bloco sólido não, e não por acidente — a
                    Regra 3 manda não tocar no que é do jogador, e um
                    lote com bloco sólido dentro nem devia ter sido
                    escolhido

sem drop            a flor some, não vira item no chão. É o que
                    acontece quando um jogador põe bloco sobre grama
                    alta. Fazer cair encheria o canteiro de entulho
                    que ninguém recolhe

quando              ao abrir a obra, e de novo ao ela voltar do save —
                    o jogador pode ter plantado no meio (Regra 23)
```

```text
os testes que    aBlockInsideTheHouseRefusesTheLot — com uma casa
seguram          ALTA, porque com casa baixa a janela de chão cobre o
                 volume por acidente e o teste passa sem a regra. A
                 primeira versão dele passou assim

                 flowersInsideTheHouseDoNotRefuseTheLot
```

---

## Regra 23 — o que já foi analisado se analisa de novo

```text
vale para o mod inteiro: espaço já analisado pode ser reanalisado,
porque o jogador modifica a vila
```

Pedida pelo autor em 2026-08-19, e ela derruba um argumento que estava
escrito no código com todas as letras. `LumberjackWork.REJECTED` dizia:

> *Construção não vira árvore, então a recusa não envelhece — o que
> envelhece é o mundo, e para isso basta reiniciar.*

O argumento está errado pelo lado do jogador. Ele derruba a parede,
planta uma muda ao lado do pilar, deixa a copa crescer sobre o tronco
que descascou. **O mundo muda, e o mod ficava com uma opinião de trinta
minutos atrás** — até o servidor reiniciar.

Agora a recusa envelhece, como já envelhecia a marca de árvore fora de
alcance: guarda quando nasceu e esquece sozinha, em dez ciclos da
colônia.

```text
o que já obedecia    UNREACHABLE, com prazo desde a Regra 9
                     o cursor da busca de lote, que recomeça do centro
                     o cursor da busca de árvore

o que passou a       REJECTED, o "não é árvore"
obedecer             a limpeza do canteiro, refeita ao voltar do save
```

O princípio, para quem escrever o próximo cache: **marca que não vence é
uma afirmação sobre o futuro do mundo do jogador, e o mod não tem como
fazer nenhuma.**

---

## Regra 24 — a vila de planície levanta a casa do jogo

```text
o modelo de casa construído em vila de planície é a casa pequena
padrão do Minecraft (plains_small_house_1)
```

Decidida pelo autor em 2026-08-19, e ela desfaz metade da Regra 13.

A Regra 13 tinha trocado a obra do MVP pela cabana do mod porque a casa
do jogo era **impossível**: 66 dos 149 blocos dela pedem minerar,
fundir, tosquiar e descascar, e a colônia parava no primeiro pedregulho.
O que mudou desde então não foi a colônia — foi a segunda metade da
própria Regra 13 ficar utilizável: *o que a colônia não produz, o
jogador guarda no baú*, e a Regra 10 deu ao construtor o acesso a
qualquer baú da vila, com fabricação do que faltar.

```text
o que a colônia faz    49 escadas (da tábua, pela Regra 10)
sozinha                33 tábuas
                        1 porta

o que você guarda      43 pedregulhos
no baú                 16 troncos descascados
                        3 vidraças

o que não segura        1 cama, 3 tochas — mobília, Regra 21
a obra
```

**A consequência, dita antes de acontecer:** esta casa **não sobe
sozinha** como a cabana subia. Sem pedregulho num baú, a obra fica em
`WAITING_RESOURCES` e o relatório diz o que falta. Isso é o modo de jogo
que o autor escolheu, não um defeito — mas é uma troca, e quem abrir o
log daqui a um mês precisa achar isto escrito.

**A Regra 17 sobreviveu, e não de graça.** A cabana é um quadrado e
resolvia a porta mudando duas coordenadas. A casa do jogo tem a porta
onde o arquivo a pôs — a um bloco da parede oeste, com o *jigsaw* de
encaixe de rua do mesmo lado. A única forma de virá-la para a rua é
**girar a planta inteira**, e é o que passa a acontecer:

```text
Blueprint.doorSide()   a parede mais perto da porta. Acerta a casa de
                       planície, e a conta é a mesma para qualquer
                       planta com porta

Blueprint.rotated(n)   giro em quartos de volta, com a caixa
                       acompanhando: 5x3 vira 3x5. A ordem dos blocos
                       é preservada, porque é ela que faz a parede
                       subir antes do teto

Side.turnsTo(outro)    quantos quartos de volta daqui até lá
```

A obra que volta do save é girada de novo, e o lado sai do mundo — não
do save. Um campo gravado poderia discordar do terreno depois de o
jogador mexer nele; o caminho de terra ao lado, não.

**A mobília precisou ser reconhecida.** A Regra 21 nasceu para a cabana,
onde a lista era escrita à mão. Numa planta lida de arquivo o leitor
marca cama, tocha, lanterna e baú como mobília — e **só isso**.
Pedregulho, vidraça e tronco descascado seguram a obra, e é assim que
tem de ser: são parede, e casa sem parede não é casa.

---

## O catálogo de estruturas — 2026-08-19

O autor baixou a pasta `minecraft-assets_structure/` com **1.180
arquivos** de estrutura do jogo, 6,5 MB, e pediu que ela fosse a base de
dados de construções possíveis do construtor — "para implementar no
futuro".

**O que entrou no repositório:** só os **nomes**, em
`data/villagecolony/catalog/vanilla_structures.json` (51 KB). Nenhum byte
de arquivo da Mojang.

**Por quê.** O mod não precisa dos arquivos: o jogo já os traz, e
`StructureBlueprintReader` os lê por id — foi assim que a casa de
planície foi medida em 08-15, antes de qualquer cópia existir. E o
repositório é público, então commitar assets da Mojang é decisão
jurídica, não técnica. O `.gitignore` já tinha o precedente exato, com a
pasta do mod Workers.

**O que falta para o construtor escolher entre muitas** não é a lista. É
o critério — que casa, para qual vila, em que ordem — e a conta de
materiais de cada uma, que hoje só existe sob demanda. A ferramenta para
isso já está escrita: `Blueprint.materials()`, que o teste de lista de
compras usa.

---

## Regra 25 — a colônia levanta a maior planta que couber no lote

```text
onde a casa grande couber, é ela que sobe; onde não couber, sobe a que
cabe — e a escolha é por lote, não por vila
```

Decidida em 2026-08-20, e o motivo está num log. A vila varreu o raio de
64 inteiro e respondeu:

```text
no free lot beside a road in the whole 64-block radius of
[-6810, 98, -5054] that fits ColonyPos[x=7, y=7, z=7]
```

Na mesma vila em que **três cabanas já estavam de pé**. A casa de
planície pede 49 colunas no nível exato da rua, fora das peças da vila
gerada e com sete blocos livres acima; a cabana pede 25. Exigir a grande
em toda parte transformou a Regra 24 num travamento: a colônia parou de
crescer.

```text
por lote, e não     a casa de planície continua subindo onde há
por vila            espaço para ela. Rebaixar a vila inteira porque um
                    canto é apertado seria perder a Regra 24 para
                    salvar o crescimento

a cabana fecha      é a única planta que a colônia levanta sem o
a lista sempre      jogador guardar nada em baú. Enquanto ela couber
                    em algum lugar, a vila continua crescendo

uma varredura só    as plantas dividem o mesmo teto de mil colunas.
                    Coluna que não é estrada é recusada antes de olhar
                    planta nenhuma, que é a esmagadora maioria
```

É a Regra 13 outra vez — construir o que a colônia consegue —, agora
sobre **espaço** em vez de material.

---

## Regra 26 — a cadeia de produção, e a paleta de cada vila

```text
a colônia produz o que a casa dela pede, e o que ela pede depende do
bioma em que a vila está
```

Decidida em 2026-08-20. É a Regra 20 dita por inteiro e a segunda metade
da Regra 13 finalmente exercida.

**O que estava errado.** A Regra 20 dizia que cada vila constrói no
estilo do seu bioma, e o estilo era **uma coisa só**: a espécie da
madeira. Isso bastava enquanto a colônia só sabia derrubar árvore, e
deixava o deserto de fora — a vila nascia, contratava, contava recurso e
não construía nunca.

**A paleta.** Cada bioma responde de que a vila é feita, por inteiro:

```text
estilo      parede            porta        pedra
carvalho    oak_planks        oak_door     cobblestone
pinheiro    spruce_planks     spruce_door  cobblestone
acácia      acacia_planks     acacia_door  cobblestone
arenito     sandstone         —            sandstone
```

**O deserto não tem porta, e é decisão.** A porta sai de tábua, tábua sai
de tronco, e ali não há tronco. Exigi-la deixaria a casa em
`WAITING_RESOURCES` para sempre — o travamento que a Regra 13 corrigiu. A
cabana do deserto tem o vão, e quem quiser pendura a porta.

**As três profissões novas**, e o que cada uma destrava:

```text
MINER      pedra do mundo: pedregulho onde há rocha, arenito no
           deserto. Destrava os 43 pedregulhos da casa de planície e
           a vila de deserto inteira

SHEPHERD   lã, e a ovelha continua viva. Destrava a cama — e a cama
           é aldeão novo, que é trabalhador novo. É o elo que faltava
           no laço da vila

SMELTER    areia em vidro, pela receita de fornalha do jogo. Resolve
           a exceção que a Regra 10 registrou em 08-18 como
           impossível
```

**O que a Regra 3 exige aqui, e é mais que para a árvore.** A vila gerada
e as casas do jogador são feitas do mesmo material que o mineiro procura.
Ele só toca pedra **exposta e de ninguém** — nem peça de vila, nem
construção da colônia. Um mineiro sem essa porta derrubaria a igreja no
primeiro ciclo.

**Sem forno no mundo, e é decisão.** O fundidor transforma o que está no
baú, como o fabricante transforma tronco em tábua sem bancada. Pôr forno
de verdade seria escrever bloco fora da planta.

**A areia foi fechada no mesmo dia**, e a decomposição que ela pedia
coube num passo. A casa de planície não pede vidro: pede **três
vidraças**, e perguntar ao projeto quanto vidro falta devolvia zero — com
zero a colônia nunca abria tarefa de fundição, o fundidor ficava parado e
a areia não tinha para quem ser colhida. Agora a vidraça é decomposta
pela receita do próprio jogo (seis vidros dão dezesseis vidraças), o vidro
vira meta, e a areia sai dele: uma por vidro, descontando o que já está
fundido. Ver `GlassDemand`.

**E areia não desce a mina.** A Regra 29 mandou o mineiro cavar fundo, e
para pedra está certo — há pedra em toda parte abaixo do chão. Areia mora
na praia, na duna e na margem do lago, e a vinte blocos de profundidade
não há nenhuma fora do deserto. A mesma profissão, dois caminhos, e quem
decide é o recurso que a tarefa pede. O caminho de superfície devolveu
função ao `RingSweep`, que tinha ficado sem quem o chamasse.

**O minério entrou em 2026-08-21**, e com ele a mina deixou de ser só
uma fonte de pedregulho. O mineiro reconhece carvão e ferro — as duas
variantes, comum e de ardósia, porque a segunda sala fica no nível −20 —
e **segue a veia**: minério não vem sozinho, e voltar para o túnel com
metade dela aberta faria o aldeão andar até lá outra vez na passagem
seguinte. O minério colado na parede vem antes da parede, e a posição do
túnel espera a passagem seguinte em vez de se perder.

**Só carvão e ferro**, e é decisão: são os dois que alguma receita da
colônia consome. Ouro, cobre e redstone encheriam o baú, e baú cheio faz
a Regra 1 parar a coleta do que falta.

**O que esta regra NÃO fecha**, e está dito para não passar por pronto:

```text
tronco           não é receita de bancada: é machado no tronco. É o
descascado       último material da casa de planície que a colônia
                 não faz

ferro            o lampião pede ferro, e ferro pede minerar fundo e
                 fundir. O fundidor já sabe fundir; falta o mineiro
                 descer

o buraco         o lenhador replanta o que corta (Regra 7). O mineiro
                 não tem equivalente, porque pedra não cresce, e a
                 vila vai ficando com covas rasas em volta
```

**A Regra 4 mudou de tamanho sem mudar de enunciado.** Dois trabalhadores
por profissão, com sete profissões, são catorze vagas por colônia — eram
oito. A Regra 11, que garante o piso de uma de cada, ficou
proporcionalmente maior, e foi fechada em 2026-08-21: sete testes, e a
garantia com nome próprio.

---

## Regra 27 — o construtor só levanta o que está no catálogo do jogo

```text
todas as estruturas que o construtor de cada bioma poderá construir
estão na pasta "minecraft-assets_structure"

não criar estruturas que não estão na lista

o construtor aguarda a existência do específico tipo de bloco que ele
precisa para continuar a construir
```

### Emenda 1 — abre para pedra só, 2026-08-26

```text
o construtor pode assentar pedra declarada no lugar da pedra que a
planta pede

fora da pedra, ele continua aguardando o bloco específico
```

**Três palavras do autor**, e elas mudam uma regra marcada como
imutável: *abre para pedra só*. O resto da Regra 27 continua inteiro —
só o catálogo do jogo, e o construtor aguardando o bloco exato de tudo o
que não for pedra.

**Por que a emenda foi pedida.** A ADR-009 §3.10 quer variedade —
*deserto prefere arenito; isso não quer dizer que só possa arenito* — e
a decisão 2 do autor, no mesmo dia, mandou implementar os quatro níveis
de substituição. Ao escrever, apareceu o impedimento: enquanto o
construtor exigisse o bloco exato, declarar substituição não daria
variedade nenhuma. Daria **travamento**.

**O defeito que essa emenda evita, e é o mesmo de 2026-08-22.** Naquele
dia, pedregulho respondia por arenito na conta da colônia e não no
construtor. A vila de deserto com o baú cheio de pedregulho declarava a
meta de arenito cumprida, o mineiro não ia cavar, e a obra dormia
esperando um bloco que ninguém buscaria.

**O defeito era a discordância**, e não a substituição. A correção de
08-22 desfez a discordância pelo lado da conta: pedregulho deixou de
responder por arenito. A emenda de 08-26 a desfaz pelo outro lado: a
conta aceita, e a parede também. As duas correções são a mesma frase
dita de dois jeitos — **a conta e o construtor precisam concordar**.

**O que a emenda não afrouxa:**

```text
o material continua saindo do baú antes de o bloco entrar no mundo

o que se assenta é o que saiu do baú, e não o que a planta pediu —
a colônia não inventa matéria

o preferido vem primeiro: a casa sai com o bloco certo enquanto ele
existir, e o substituto entra quando o certo acabou

fora do grupo da pedra, nada substitui nada na parede
```

**A madeira ficou de fora, e o autor sabe.** Ela é `ACCEPTABLE`: conta
para a meta da colônia e o construtor continua exigindo a espécie que a
planta pede. A mesma discordância mora ali — uma colônia com duzentas
tábuas de bétula e nenhuma de carvalho declara a meta cumprida enquanto
a casa espera carvalho. Está registrado no `TODO.md` e não foi mexido
porque a decisão foi *pedra só*.

**Onde isso vive no código:** `Substitution.ALTERNATIVE` é o nível que o
construtor assenta, `MaterialChoice` é a lista que ele consulta, e
`ResourceSubstitutionTest.theWallOnlyEverAcceptsStone` é o guarda que
impede a emenda de crescer sem decisão.

### Emenda 2 — e a madeira junto, 2026-08-26

```text
o construtor pode assentar, no lugar do bloco que a planta pede,
qualquer membro declarado da mesma família de construção

as famílias são três: madeira, tábua e pedra

fora delas, ele continua aguardando o bloco específico
```

**A Emenda 1 tinha meio dia de idade quando esta a alargou**, e o motivo
está no E28: ao escrever a abertura da pedra, apareceu que **a madeira
tinha exatamente a mesma discordância**, e ninguém a tinha visto.

`OAK_PLANKS` responde pelo grupo inteiro na conta da colônia — é
deliberado, e está escrito em `ColonyGoals` desde sempre: *sessenta e
quatro troncos de abeto satisfazem esta linha tanto quanto os de
carvalho*. Só que o construtor exigia a espécie que a planta pede. Uma
colônia com duzentas tábuas de bétula e nenhuma de carvalho **declarava
a meta cumprida enquanto a casa esperava carvalho**, e a obra dormia até
o `PatienceClock` desistir dela.

**Por que ninguém viu.** Em planície o lenhador corta carvalho, e a casa
de planície pede carvalho. O defeito precisa de uma vila cercada de
outra espécie — e a sessão de 2026-08-25 já mostrava baús com bétula,
abeto, selva, acácia, cerejeira e mangue.

**O que a emenda dá, e é a frase do autor:** *alternativas de recursos
para todas as construções dos biomas*. A vila de taiga levanta a casa
dela com o abeto que tem em volta; a de savana, com acácia; a de
deserto, com o pedregulho que sobrou. Nenhuma delas depende mais de a
floresta ao lado ter a espécie exata que o arquivo de estrutura da
Mojang escolheu.

**O que continua valendo, e não mudou de uma emenda para a outra:**

```text
o material sai do baú antes de o bloco entrar no mundo

o que se assenta é o que saiu do baú — a colônia não inventa matéria

o preferido vem primeiro: a casa sai da espécie certa enquanto ela
existir

dentro da família, e só nela: tronco não vira tábua por substituição.
Vira por receita, e quem faz isso é o fabricante

fora das três famílias, nada substitui nada na parede. Areia, carvão,
ferro e vidro alimentam receita, e trocá-los mudaria o que a colônia
produz, não a cara da casa
```

**Um cuidado que a madeira exigiu e a pedra não.** Tronco tem eixo;
tábua e pedra não têm nada. Um substituto assentado no estado padrão
poria em pé a viga que a planta queria deitada. Por isso o substituto
**veste as propriedades da planta** no que os dois tiverem em comum —
`MaterialChoice.dressedLike`.

**E fica dito que isso ainda não muda nada em jogo:** o
`BlueprintBlock` não carrega propriedade nenhuma, e o construtor assenta
tudo no estado padrão mais o giro da porta. O eixo do tronco já se perde
hoje, com substituição ou sem — é a **ADR-008**, decidida e por
escrever. A garantia existe para o dia em que ela entrar.

**`ACCEPTABLE` ficou sem ninguém.** Hoje tudo o que uma exigência aceita,
o construtor assenta. O nível fica no enum porque a distinção continua
fazendo sentido: um recurso que a colônia conte junto e não possa
assentar é coisa que ainda pode existir.

---

**Regra imutável, dita pelo autor em 2026-08-20.** Ela desfaz decisões
anteriores, e vale registrar quais, porque são decisões que este mesmo
documento defendeu:

```text
Regra 13    criou a cabana do mod — escrita em código, cinco por cinco
            — porque a casa do jogo era impossível com o que a colônia
            produzia. A cabana deixa de ser levantada

Regra 21    mandava a casa terminar sem cama e sem lampião, e a peça
            entrar depois. A obra passa a esperar por elas

Regra 25    continua valendo, e agora escolhe entre as casas do
            catálogo em vez de escolher entre casa do jogo e cabana
```

**O que tornou isso possível**, e é do mesmo dia: a Regra 26 deu à
colônia mineiro, pastor e fundidor. A Regra 13 recusou a casa do jogo
porque 66 dos 149 blocos dela pediam minerar, fundir, tosquiar e
descascar. A resposta agora não é trocar a casa — é a colônia aprender.

```text
o que a lista tem     plains 36 casas    savanna 31    desert 28
                      taiga  27 casas    snowy   30

de onde ela sai       data/villagecolony/catalog/vanilla_structures.json,
                      o índice da pasta, que entrou em 08-19 — só os
                      nomes, nenhum byte de arquivo da Mojang. Os
                      arquivos o jogo já traz

o que fica de fora    as variantes zumbi. São as mesmas casas em ruína,
                      e uma colônia que as levantasse estaria
                      construindo a própria decadência
```

**A cabana continua no código, e não é contradição.** Save antigo tem
cabana pela metade e construção registrada, e apagá-la faria a colônia
planejar por cima do que ela mesma levantou. Ela é **retomável e nunca
mais oferecida** — a regra proíbe criar, não proíbe terminar o que já
está de pé.

**A espera do construtor, e o que a torna suportável.** Aguardar o bloco
específico, sem exceção, reabriria o travamento que a Regra 13 corrigiu
— não fosse o `PatienceClock` da mesma semana, que tira a obra da frente
depois de vinte ciclos. **A espera é do construtor, não da vila:** ele
espera pela peça, e a colônia vai planejar outra coisa.

**O que isto custa, dito sem suavizar:**

```text
tocha           pede graveto e carvão, e a colônia não minera carvão.
                Casa de vila tem tochas, então casa de vila espera pelo
                jogador — ou fica pela metade quando a paciência acabar

ferro           mesmo caso, pelo lampião

tronco          mesmo caso. É machado no tronco, e ninguém descasca
descascado

o corte de       a busca de lote experimenta quatro tamanhos por coluna,
quatro plantas   e não trinta e seis. Casa de tamanho raro num lote
                 apertado deixa de ser tentada — o preço de a varredura
                 caber num tique
```

---

## Regra 28 — a barreira de teste, e ela é provisória por decisão

```text
enquanto este projeto não estiver formalmente acabado:

  a única estrutura que o construtor pode construir é a casa pequena
  do seu bioma — em planície, plains_small_house_1

  a construção não espera pela porta, cama, lampião e baú se eles não
  estiverem dentro de algum baú da vila
```

Dita pelo autor em 2026-08-20, e **ele a declarou temporária na própria
frase**: *"esta barreira de teste vai sumir quando eu definir no
futuro"*. Está registrada aqui como regra porque vale hoje, e marcada
como barreira porque não vale amanhã.

**Por que ela ajuda.** Vinte e oito a trinta e seis casas por bioma é
variedade demais para depurar: cada uma pede materiais diferentes, e uma
sessão que falha não diz se falhou pela regra nova ou pela casa
sorteada. Uma casa por bioma torna toda sessão comparável com a
anterior.

**Onde ela mora, e como sai:**

```text
a casa única    VillageStructures.ONLY_WHILE_TESTING. Apagar o campo e
                a linha que o usa devolve a pasta inteira, que é a
                Regra 27

as quatro       BuilderWork.isSkippableWhileTesting. Apagar o método e
peças           o bloco que o chama devolve a espera sem exceção
```

**Por que são essas quatro peças e não outras.** Porta, cama, lampião e
baú dependem de cadeia que a colônia ainda não fecha, e as quatro são
peças que a casa dispensa **sem ficar com buraco na parede**. Pedra,
tábua e vidraça não entram na lista: sem elas a casa tem furo, e furo é
a Regra 22 ao contrário.

**O que continua travando a casa de planície, medido e não suposto.** A
lista de materiais dela é de 149 blocos em 8 tipos:

A medição original, do dia em que a barreira nasceu:

```text
 49 oak_stairs          a colônia faz, de tábua
 43 cobblestone         o mineiro traz
 33 oak_planks          o fabricante faz
  1 white_bed           dispensada pela barreira
  1 oak_door            dispensada pela barreira

 16 stripped_oak_log    NINGUÉM DESCASCA — a obra espera
  3 wall_torch          pede carvão, e ninguém minera carvão
  3 glass_pane          o fundidor faz vidro de areia, e NINGUÉM COLHE
                        AREIA
```

**Duas das três fecharam no mesmo dia**, e o que sobrou é uma só:

```text
 16 stripped_oak_log    ✅ o fabricante descasca, por conversão nominal
  3 glass_pane          ✅ a vidraça vira vidro pela receita, o vidro
                        vira meta, e o mineiro colhe a areia dele
  3 wall_torch          ✅ o carvão sai da mina, e o graveto sai da
                        tábua — a bancada desce dois degraus
```

**As três fecharam**, e a última fechou por um motivo diferente das
outras duas: não faltava material, faltava **profundidade de receita**.
Até 2026-08-21 a colônia só montava o que pudesse montar com <b>todos</b>
os ingredientes já no baú, e a tocha pede carvão e graveto. O carvão a
mina passou a dar; o graveto caía das folhas por sorteio, e ninguém o
fazia de tábua — a colônia ficava com carvão, tábua e nenhuma tocha.

`ColonySupply` agora desce **dois degraus** atrás do que falta. Dois, e
não um, porque é o que o lampião pede: pepita, que sai do lingote; e
tocha, que sai de carvão e graveto.

**A casa de planície não depende mais do jogador em nenhum material.**
Isso está afirmado por teste e **não foi visto em jogo**.

**E o lampião fechou junto**, pelo mesmo degrau. Ele é mobília da Regra
21 e não passa pela obra: quem sabe quantas casas estão sem ele é a
passagem que as mobiliaria, e ela relatava **um** número — a lã. Agora
relata dois, e o segundo desce dois degraus até o lingote, porque o
lampião pede pepita e a pepita é que sai do lingote. O lingote vira meta
da fornalha, o minério cru vira meta da mina, e o fundidor que aprendeu a
fundir ferro pela manhã passou a receber tarefa.

**Nada disto foi visto em jogo**, e a frase vale para as duas que
fecharam: elas estão cobertas por teste e nenhuma rodou numa vila de
verdade.

Isso está dito aqui, e não descoberto na próxima sessão, porque é
exatamente o tipo de coisa que custa uma sessão inteira quando fica
implícita.

---

## Regra 30 — o mineiro recolhe tudo, e a boca da mina tem endereço

**Enunciada pelo autor em 2026-08-22, e implementada no mesmo dia.**

```text
o mineiro vai atrás de recurso normalmente, e recolhe
TUDO o que cavar

onde ele decide começar a cavar aparecem duas coisas:
  uma lanterna de um lado do buraco
  um baú marcado como do mineiro do outro

esse baú guarda MINÉRIO, e só

quando ele lotar, o minério passa a ir para o baú
principal do mineiro
```

**O que é minério, decidido pelo autor no mesmo dia:** cobre, ferro,
ouro, redstone, lápis, esmeralda e diamante — **todo minério menos
carvão**. Pedra, terra e carvão vão direto para o baú do mineiro na
vila, que é de onde a obra e a fornalha tiram o que consomem; mandar
carvão para o fundo da mina seria afastá-lo de quem o usa.

**O que ela muda na 29.** O `OreVein` reconhecia dois minérios — carvão
e ferro — e agora reconhece os oito, nas duas variantes de ardósia.
Seguir só dois era o mineiro passando ao lado de diamante sem ver.

**Não custa material**, e é a mesma decisão que a mina inteira já
carrega: a escada, as duas salas e a galeria são cavadas e ninguém paga
por elas. Cobrar aqui faria a mina não abrir até a colônia ter lanterna
— e lanterna pede ferro, que vem da mina.

**Sem estado novo em disco.** Onde está o baú é lido do mundo: dos
vizinhos da boca, o que for um baú é ele. Gravar a posição seria uma
segunda verdade que o jogador desfaz com uma picareta.

**Onde ela mora:** `MineMouth`, `OreVein.isTreasure` e
`MinerWork.treasureChestFor`.

---

## Regra 29 — a mina, e o mineiro que desce por ela

```text
o mineiro anda até o final da vila e começa a cavar com sua picareta
de diamante para baixo em formato de escada, de modo que ele possa
subir de volta

desce 10 blocos; no andar do 10° bloco recolhe uma área de 7x4

depois, em outra direção da descida, cava mais 10 blocos para baixo
com a mesma regra da escada; no andar do 20° recolhe outra área de 7x4

sempre que encontrar uma barreira que impeça de realizar estas ações
ele começa a recolher para outro lado

na camada 20 ele começa a recolher na altura do aldeão mais 1
infinitamente
```

Dita pelo autor em 2026-08-20, e ela **substitui** o mineiro de
superfície do mesmo dia — aquele procurava pedra exposta em volta da
vila, e a mina é outra coisa.

**Por que a escada, e não o poço.** É a frase do autor: *"de modo que ele
possa subir de volta"*. Um aldeão que cavasse reto para baixo ficaria no
fundo do buraco, e a colônia perderia um trabalhador por causa do próprio
trabalho.

**Por que dois blocos de altura.** *"na altura do aldeão mais 1"* — os pés
e a cabeça. Um só e ele não passa; três e a mina custa cinquenta por
cento a mais de tempo para dar a mesma pedra.

```text
lance 1     dez degraus, dois blocos cada          20 posições
sala 1      sete por quatro no nível -10           56 posições
lance 2     mais dez degraus, virando à direita    20 posições
sala 2      sete por quatro no nível -20           56 posições
galeria     do nível -20 em diante                 sem fim
```

**A forma mora em `core`**, e é geometria pura: `MineShaft` não conhece
Minecraft, e por isso as cento e cinquenta e duas posições da parte
cavada se afirmam em milissegundos em vez de uma sessão. Quem decide se
um bloco <i>pode</i> ser cavado — bedrock, a vila do jogo, a casa da
colônia — é a camada de fora.

**Duas correções que o teste da forma pegou**, e as duas custariam
tempo do aldeão em jogo:

```text
a sala começava   o último degrau abre exatamente os dois blocos que
sobre o patamar   seriam o canto dela, e cavá-los de novo é bater a
                  picareta no ar

o segundo lance   partia da ponta da sala e entrava na largura dela,
partia do lugar   sobrepondo oito posições. Passou a partir do canto
errado
```

**A Regra 3 tem duas portas aqui**, e é de propósito: a boca da mina não
se abre sobre vila gerada nem sobre casa da colônia, e cada posição
cavada é conferida de novo. Qualquer uma das duas basta para o teste
passar — foi preciso desligar as duas para vê-lo falhar, e isso está
registrado porque um dia alguém vai mexer numa e achar que a outra não
existe.

**A picareta é de diamante**, por decisão do autor. São vinte blocos de
descida antes de a mina render alguma coisa, e com picareta de madeira
isso é uma sessão inteira.

**O lado para onde a mina abre sai do identificador da colônia.** Duas
colônias vizinhas cavam para lados diferentes, e a mesma colônia cava
sempre para o mesmo lado entre sessões — o aldeão não perde a mina que
abriu ontem.

**A mina é da colônia, e é gravada — 2026-08-20, no mesmo dia.** Sete
campos no save: a colônia dona, a boca em três coordenadas, o lado da
descida, o lado da galeria e a fronteira já cavada. Uma mina por colônia,
e não uma por mineiro: o segundo a descer continua a mesma escada.

**O que o limite era, e por que ele não era pequeno.** A regra nasceu com
a mina em memória, e o texto de então dizia que reabrir custava "uma
varredura de índices, não uma escavação". Estava certo pela metade:

```text
a boca        reprocurada pelo primeiro bloco sólido na coluna do fim
              da vila — e esse bloco tinha sido cavado. A busca descia
              mais, achava outro, e a colônia ganhava uma segunda
              escada alguns blocos abaixo da de ontem

a fronteira   voltava a zero, e a varredura de índices era de graça
              por posição e crescia com a profundidade: a galeria não
              acaba, e a conta acompanha

a galeria     reabria virada para a lava que já a tinha feito virar, e
              o mineiro batia oito vezes na mesma barreira para virar
              de novo
```

**O lado da descida continua saindo do identificador da colônia**, e
agora é redundância de propósito: mesmo que o save se perca, a mina nova
abre para o mesmo lado da antiga.

---

# 19. O ciclo de 2026-08-20, registrado

Dezenove commits num dia, e o ciclo que mais mudou a natureza do mod
desde a Fase 10. Fica aqui o que foi feito, o que não foi, e o que
passou a se contradizer.

## O que mudou de natureza

**A colônia deixou de depender do jogador para os materiais.** Até
08-19 ela produzia tábua e nada mais; o que a casa pedia de pedra,
vidro, lã e ferro era o jogador que guardava no baú. Hoje ela minera,
tosquia, funde e descasca.

**O mod deixou de inventar casa.** A cabana escrita em código — criada
pela Regra 13 porque a casa do jogo era impossível — foi aposentada pela
Regra 27. O que mudou não foi a casa: foi a colônia aprender a fazer o
que a casa pede.

## O que ficou por fazer, e é sabido

```text
o fazendeiro     tem nome, enxada e baú desde a Fase 4, e nenhum
                 trabalho. É a última profissão do modelo sem código
```

## O que falta testar

**Nada deste ciclo foi visto em jogo.** É a maior dívida aberta, e ela
cresce a cada commit: dezenove mudanças cobertas por teste e nenhuma
rodada numa vila de verdade. A lista ordenada do que olhar está no
`TODO.md`.

Sem teste próprio, e a bateria só prova que nada quebrou:

```text
o fabricante descascando, montando tocha e vidraça
a retomada da mina depois de o servidor parar
```

Coberto, mas com limite conhecido: **a arena da bateria tem bioma fixo**,
então a aceitação de vila de taiga, savana, nevada ou deserto nunca
rodou.

## Os conflitos que este ciclo criou

Registrados porque dívida calada é dívida que ninguém paga. O
detalhamento está no `TODO.md`.

```text
1  três regras sobre a mesma pergunta — a 21, a 27 e a 28 discordam
   sobre a obra esperar pela mobília. Vale a 28, que é provisória

2  VillagePalette ficou quase sem uso: wall() e door() só servem à
   cabana aposentada

3  small_house.nbt é cópia de arquivo da Mojang num repositório
   público, e desde a Regra 27 a produção não o lê. O §catálogo
   afirmava em 08-19 que nenhum byte da Mojang tinha entrado, e isso
   deixou de ser verdade no mesmo dia

4  a Regra 25 está inerte enquanto a 28 valer: escolher a maior planta
   precisa de mais de uma planta

5  RingSweep ficou sem quem o chame — a mina substituiu o único usuário

6  a cabana do mod continua no código, e é deliberado: save antigo tem
   cabana pela metade. Mas é uma estrutura que a Regra 27 proíbe criar,
   morando no código que a proíbe
```

## O que o ciclo prova sobre o método

Três defeitos deste ciclo vieram de **sessões de jogo** e nenhum teste
os teria pego: o alvo escrito no código, o cursor guardado pela posição,
e o miolo oco oferecido como lote. Dois vieram de **testes** e nenhuma
sessão os teria mostrado: o servidor caindo por chunk descarregado, e as
duas sobreposições da geometria da mina.

As duas coisas são necessárias, e o projeto continua sem poder trocar
uma pela outra.

---

# 20. O checkpoint de 2026-08-26

Não é um ciclo de desenvolvimento: é uma auditoria do estado real antes
de enviar treze commits que estavam parados no `main` local. O que segue
foi **medido nesta máquina**, e não lembrado.

## O que foi rodado, e o que deu

```text
./gradlew clean build          BUILD SUCCESSFUL   11 tarefas executadas
testes unitários               476 / 476          0 falhas, 0 ignorados
./gradlew runGametest          171 / 171          bateria verde
compileGametestJava            compila
```

Os 476 saem dos 42 XML de `build/test-results/test`, somados — não da
contagem do console, que agrupa. A primeira execução de `build` veio
inteira `UP-TO-DATE` e não provava nada; **o número acima é de um
`clean`.**

## O que a repetição mostrou, e uma execução só teria escondido

A bateria foi rodada **oito vezes**. Seis verdes, **duas vermelhas**, e
as duas no mesmo teste:

```text
lumberjackgametest.thestallguardreturnsthetaskandforgetsthetree
  "o guarda não devolveu a tarefa à fila — ela está em EXECUTING"
```

É o **E20**, e ele continua aberto. Uma execução só teria dito "171
passaram" e o checkpoint teria mentido por omissão — o erro aparece em
uma rodada a cada quatro.

**O diagnóstico saiu do arquivo, não de hipótese nova.** O teste
registra a colônia em `COLONIES` (`LumberjackGameTest.java:978`) e faz a
afirmação no tique fixo 300 (`:1005`). Entre a devolução da tarefa e o
tique 300 cabe o ciclo de 600 ticks, que reserva de novo a tarefa
disponível: o guarda funciona, e o teste mede um estado passageiro tarde
demais. A instrumentação que provou isso é de **2026-08-19** e está no
ramo `claude/inspiring-torvalds-bdc8c3` — que ficou **21 mil linhas
atrás** do `main` e não serve para merge. O que se aproveita dele é o
diagnóstico.

## O sinal que a bateria deu sobre a Regra 28

```text
TEST BARRIER covered for nothing this session —
every piece came from the colony's own chests. Rule 28 can go.
```

É a linha que o `TODO.md` pedia no item 5 do que falta ver. **Ela saiu
na arena da bateria, não numa sessão de jogo** — bioma fixo de planície
e fixtures montadas. Conta como notícia boa e **não** como a prova que o
item pede, que continua devendo uma vila de verdade. E a decisão 3 de
08-26 já disse que a Regra 28 só cai depois do Nível 4, então nada muda
por causa desta linha.

## O que a auditoria achou de discordante

```text
1  Backlog §S5 estava vencido em todos os números — corrigido neste
   commit. LumberjackWork saiu da lista (1149 → 455, corte do E19) e
   quatro arquivos entraram desde 08-15, trazidos pela mina e pela
   estrada

2  small_house.nbt continua rastreado num repositório público, e é
   o conflito 3 do §19. Mudou uma coisa desde lá: a produção não o lê
   mais, mas a bateria lê — theModsOwnSchemaLoads o carrega. Tirá-lo
   não é apagar arquivo morto, é trabalho com teste junto

3  o ramo claude/inspiring-torvalds-bdc8c3 está no remoto e 21 mil
   linhas atrás. Ninguém o mencionava em documento nenhum

4  o Gradle avisa que o build usa recurso incompatível com o Gradle 10.
   Sai do Loom, não dos scripts do projeto. Não quebra nada hoje
```

## O que este checkpoint não fez

**Nenhuma correção de comportamento.** O E20 tem causa e endereço e
continua aberto de propósito: consertar teste intermitente pede fase
vermelha conferida e muitas rodadas, e isso é ciclo próprio, não
rodapé de auditoria.

O que entrou foi documentação: o §S5 do `Backlog`, a linha do E20 no
`TODO.md`, e esta seção.
