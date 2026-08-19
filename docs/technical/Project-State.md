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
1  Fundo do ícone do mod

   A arte veio sem canal alpha, fundo branco sólido.

   Não foi removido por chave de cor: a ovelha e as nuvens
   também são brancas e ficariam com buracos.

   Depende de recorte manual, se o autor quiser transparência.
```

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
      NÃO IMPLEMENTADA. Exige ADR nova.

   4  a orientação dos blocos                          TASK-046
      → o Core APRENDE a falar de orientação. Escada, porta e cama
        saem como o arquivo manda, e não no padrão.
      NÃO IMPLEMENTADA. Exige ADR nova — é a que emenda a ADR-005.

   5  o fundo do ícone
      → arte nova entregue pelo autor em 2026-08-15.
      PENDENTE de o arquivo entrar em assets/villagecolony/icon.png.
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
Regra 11  uma de cada profissão em cada vila   08-15, já satisfeita pelo
                                               mecanismo — falta a garantia
Regra 12  o centro fica em bloco que existe    08-15, feita em 08-15
Regra 13  a obra do MVP é uma que a colônia    08-15, feita em 08-15
          consiga fazer
```

Duas previsões das primeiras se confirmaram e vale marcá-las: a fila que
não esvaziava — o E1 do §17 — morreu junto, e o lugar onde as duas
primeiras regras moram é de fato o mesmo.

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

O que falta não é o mecanismo, é a **garantia**:

```text
vila com menos de       quatro profissões não cabem em três aldeões.
quatro empregáveis      O piso vira "tantas quantas couberem", e isso
                        precisa estar escrito

a dispensa             `dismiss` tira a função de quem excede a vaga
                       e de quem não tem baú. Nada hoje a impede de
                       tirar o último de uma profissão — não foi
                       visto acontecer, e não foi verificado

nenhum teste           não há teste que afirme o piso. O que existe
afirma o piso          testa o teto
```

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
