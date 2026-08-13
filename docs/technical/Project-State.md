# Project-State.md

# Village Colony — Project State

**Status:** Em implementação — Fases 1 a 8 completas e verificadas em jogo
**Version:** 0.1.0
**Last Update:** 2026-08-12 — documento consolidado: o log saiu para
`Development-Log.md`, e o que ficou aqui é o estado
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
Fase 8 encerrada e verificada em jogo — a próxima é a Fase 9
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

O que muda a partir daqui é a natureza da Fase 9: até agora a colônia
recolhe o que o mundo já tem. Fabricar é transformar o que ela guardou, e
é a primeira vez que o estoque vai diminuir por decisão do mod.

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

todas verificadas em jogo
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

Produzir materiais        não iniciado — Fase 9

↓

Construir expansão        não iniciado — Fase 10
```

Quatro dos seis passos do MVP estão feitos e verificados em jogo. Os dois
que faltam são as duas fases seguintes, e a segunda depende da primeira.

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

Fase 9 em diante                     não iniciadas
```

---

## Código existente

```text
core/
  type/              ColonyPos, Capability, ResourceType,
                     ResourceCategory, ResourceGroup
  colony/model/      Colony, ColonyState, ColonyLifecycle,
                     VillageCandidate
  colony/service/    ColonyService, VillageDetector
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
                     TreeScanner, TreeHarvester, TreeSpecies,
                     BlockBreakTime, WorkerNameplate
  mixin/             VillagerEntityMixin
  work/              LumberjackWork

data/
  save/              ColonySavedData
```

Vazio por enquanto: `core/construction`.

---

## Testes

```text
284 testes unitários     lógica pura do Core e serialização NBT
 45 testes de jogo       a fronteira com o Minecraft, num servidor
                         sem cliente (./gradlew runGametest)
```

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

## Fase 9 — Fabricação

```text
TASK-027  implementar o Manufacturer      capacidade CRAFT_ITEMS
TASK-028  integrar o Recipe Manager       receitas Vanilla
TASK-029  produzir Oak Planks             tronco → tábua → baú
```

O que já está pronto para ser usado: `TaskType.CRAFT_MATERIAL` existe e
`ColonyCycle.typeFor` já manda recurso PROCESSED para ele; o
`ProfessionRegistry` já tem o Manufacturer com a capacidade; o estoque
conta por tipo e agrupa por `ResourceGroup`; e o trabalho contínuo do
lenhador — plano feito uma vez, um passo por tick — é o molde do que a
fabricação vai precisar.

O que a Fase 9 vai exigir decidir:

```text
onde se fabrica          bancada da vila? o próprio baú? o Vanilla
                         exige bancada para receita 3x3

o que se consome         fabricar tira do baú. Hoje nada tira, e o
                         estoque só cresce — nenhum código do mod
                         jamais removeu item de um baú
```

Quanto produzir já tem resposta: §18, Regra 5 — a meta é a da obra, e
enquanto não houver obra, metade do espaço de armazenamento em tábua.

---

## Antes disso, e sem depender dela

O §8 lista o que está aberto hoje. Nada ali bloqueia a Fase 9, e as duas
primeiras dependem só de uma sessão de jogo do autor.

---

# 8. Priority Queue

Situação em 2026-08-12, fim da noite.

---

## Precisa de uma sessão de jogo do autor

```text
P1   a copa e o cursor da busca, em jogo
```

Duas regras entraram em 2026-08-12 à noite, verdes em gametest e nunca
vistas em jogo:

```text
tronco sem copa viva não é árvore — a casa de tronco da vila precisa
continuar de pé depois de vários ciclos com lenhador ao lado dela

a busca continua de onde parou — a colônia cuja floresta começa depois
do décimo sexto bloco precisa passar a ter linha de corte
```

```text
P1b  a caixa da vila protege, em jogo
```

A Regra 3 do §18 — nunca destruir bloco da vila original — pergunta ao
próprio jogo quais blocos são peça de estrutura gerada. O mundo do
gametest não tem vila gerada, então essa metade da regra só pode ser
vista no save do autor. O que o teste alcança é o caminho sem estrutura
nenhuma, que é o que roda a cada colheita.

```text
P2   o lado do cliente
```

Nome sobre a cabeça, rachadura no bloco e braço balançando: as três
coisas que a Regra 2 acrescentou para o trabalho ser visível, e nenhuma
tem prova. O servidor dedicado não as mostra — precisa de alguém no
teclado.

---

## Precisa de decisão do autor

```text
nada em aberto
```

O P3 — quanto fabricar — foi resolvido em 2026-08-13 por delegação do
autor. A regra está no §18, Regra 5, e é a mais fácil de derrubar deste
capítulo: o enunciado vale até ele dizer outra coisa.

---

## Não precisa de decisão nem de jogo

```text
A   ColonyState.ABANDONED

    O valor existe e nada o atribui. Exige o scanner reportar
    aglomerado reprovado, para distinguir "vila deixou de ser
    viável" de "vila não foi observada". Ver §9 e ADR-003 §6.

B   aviso de colônias sobrepostas

    ADR-003 §5 manda registrar quando dois centros ficam a menos de
    32 blocos. Não implementado.

C   ferramenta inicial do trabalhador

    Profession-System.md diz que o trabalhador recebe a ferramenta ao
    assumir a função. ToolType existe e a profissão a declara; falta o
    adaptador ToolType → Item.
```

---

## Fechado em 2026-08-12

```text
E1   fila de tarefas que não esvazia    Regra 1 + purgeClosed
E2   colônia que nunca encolhe          só a sonda própria escreve
D1   marca trocando de dono             o quadro é do baú em que
                                        está pregado
D2   vaga de profissão entre vilas      não havia furo; a linha
                                        de log é que mentia

item A do §8 anterior                   morte, zumbificação,
                                        encolhimento e déficit
                                        cobertos por gametest
item B do §8 anterior                   este documento, consolidado

travamento por tarefa RESERVED          encontrado pelo teste rodado
                                        contra a regra desligada
```

---

## Depois disso

```text
Fase 9 — Fabricação
Fase 10 — Construção
```

A construção traz junto a decisão já registrada em §10: duas vilas viram
uma quando um bloco de uma encostar no bloco da outra.

---

# 9. Known Limitations

## Regras aceitas e ainda não implementadas

```text
ColonyState.ABANDONED

  O valor existe; nada o atribui.

  ADR-003 §6 exige distinguir "vila deixou de ser viável" de
  "vila não foi observada". Hoje VillageScanner.scan devolve
  apenas aglomerados aprovados, então as duas situações são
  indistinguíveis.

  Exige o scanner reportar aglomerado reprovado. Tarefa própria.
```

```text
Aviso de colônias sobrepostas

  ADR-003 §5 manda registrar
  "[COLONY] Overlapping colonies detected"
  quando dois centros ficam a menos de 32 blocos.

  Não implementado.
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
Ferramenta inicial não é entregue

  Profession-System.md diz que o trabalhador recebe a
  ferramenta ao assumir a função. ToolType existe e a
  profissão a declara, mas nada põe o item na mão do aldeão.

  Depende do adaptador ToolType → Item, não escrito.
```

```text
Nada tira item de baú

  O mod só deposita. Nenhum caminho do código remove item de um
  baú, e a Fase 9 é a primeira que precisa disso.
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
1  Quanto fabricar — Fase 9

   A Regra 1 respondeu "quanto colher" com o espaço dos baús.
   Fabricar não tem resposta equivalente: aplicar a mesma regra
   à tábua transformaria toda a madeira da colônia em tábua.

   Ver §7 e §8, P3.
```

```text
2  Fundo do ícone do mod

   A arte veio sem canal alpha, fundo branco sólido.

   Não foi removido por chave de cor: a ovelha e as nuvens
   também são brancas e ficariam com buracos.

   Depende de recorte manual, se o autor quiser transparência.
```

---

## Decididas e registradas

```text
Ícone e nome divergem                    2026-08-07

   A arte diz "Village++"; o mod é "Village Colony", id
   villagecolony. Decisão: manter como está — trocar o id
   quebraria saves, porque ele nomeia o arquivo .dat, o
   caminho do ícone e o logger.
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
Dois aglomerados distintos a menos de 64 blocos

  São adotados como uma colônia só, em silêncio.

  ADR-003 §5 pede aviso; não implementado.
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

  MVP-Tasks.md                 tarefas; ver ressalva abaixo

  docs/technical/Fabric-Version.md   versões fixadas

  docs/technical/Performance-Rules.md

  docs/technical/Testing-Strategy.md

  docs/technical/Debugging-Strategy.md

  docs/technical/Vanilla-Integration.md

  docs/technical/Project-State.md    este documento — o estado

  docs/technical/Development-Log.md  o histórico, por data
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


  A persistência de trabalhadores não constava do plano original.

  Acrescentada como TASK-012b em 2026-08-07, fora da numeração
  sequencial para não renumerar as tarefas seguintes.


  A TASK-026 — depositar de 32 em 32 — foi cancelada em 2026-08-08.

  A madeira vai direto para o baú. Ver §10.
```

```text
Initial-Setup-Checklist.md §6 e Class-Architecture.md

  Continham layouts de pacote divergentes. Agora apontam para

  a ADR-006 em vez de repetir a estrutura.
```

---

# 14. Session Resume Template

Ao retomar, começar pelo §8. Ele diz o que depende de uma sessão de jogo,
o que depende de decisão do autor e o que não depende de nenhum dos dois.

Depois dele, o §17 diz o que se sabe estar quebrado.

Ao encerrar uma sessão, atualizar:

```text
§3   Current Phase        se a fase mudou

§6   Implementation        a linha da tarefa, e a contagem de testes

§8   Priority Queue        o que entrou, o que saiu

§17  Erros conhecidos      o que se descobriu, sem inventar causa

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

### E6 — O lenhador que não cortava — explicação provável, sem confirmação

No servidor dedicado de 2026-08-12 pela manhã, seis lenhadores receberam
tarefa em onze ciclos e nenhuma árvore caiu. Na sessão das 07:48 do mesmo
dia dois deles cortaram — 22 e 7 toras —, e não se soube o que mudou.

A explicação provável apareceu à noite: a busca por árvore gastava suas
mil colunas no anel 16 e recomeçava do centro a cada ciclo, então uma
colônia cuja floresta comece depois disso nunca achava nada. A busca
passou a continuar de onde parou.

Provável, não confirmado. O que confirma é uma sessão com linha de corte
na colônia de `1109,730`. Ver §8, P1.

---

## Fechados, mantidos por rastreabilidade

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

### O que isso exige na hora de implementar

```text
ColonyGoals          ganha a linha da tábua, medida em capacidade
                     e não em contagem

ChestDepositor       já sabe medir espaço livre por grupo; falta
                     medir capacidade total

nada disso entra     antes de existir quem execute CRAFT_MATERIAL.
antes do fabricante  Tarefa aberta sem executor possível fica
                     reservada para sempre — é o que o §11 ensina
```

