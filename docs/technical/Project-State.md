# Project-State.md

# Village Colony — Project State

**Status:** Em implementação — Fases 1 a 3 completas, Fases 4 e 5
escritas e não verificadas em jogo
**Version:** 0.1.0
**Last Update:** 2026-08-08 — o aldeão anda até a árvore; ver §8
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
Fase 8 — Primeiro Trabalhador Funcional (escrita)
```

---

## Description

As Fases 1 a 7 estão completas e verificadas em jogo. O mod detecta
vilas, cria colônias, mantém sua identidade entre sessões, registra
aldeões como trabalhadores, dá função a cada um, encontra o baú da casa,
conta o que ele guarda, calcula o que falta e distribui tarefas a quem
sabe executá-las.

A dívida de verificação que dominou 2026-08-07 foi paga. O roteiro V1 a
V7 do §7 foi respondido inteiro, e as três decisões que travavam a
Fase 8 — coordenação, loop de simulação e propriedade do baú — foram
decididas e implementadas.

A camada fabric deixou de ser cega: `./gradlew runGametest` sobe um
servidor sem cliente e cobre seis casos. O primeiro defeito de produção
encontrado por máquina neste projeto veio dela, horas depois de existir.

O que a Fase 8 traz é uma virada de natureza: até aqui o mod só **lê** o
mundo. A partir da TASK-024 ele **escreve** — derruba árvore e recolhe
item. Bloco quebrado por engano é dano no save do jogador, diferente de
tudo que se errou até agora.

---

## Concluído até aqui

```text
Fase 0   decisões de arquitetura        ADR-001 a ADR-006
                                        ADR-003 e ADR-006 emendadas

Fase 1   núcleo da colônia              TASK-001 a TASK-006

Fase 2   persistência                   TASK-007 e TASK-008

Fase 3   detecção da vila               TASK-009 e TASK-010

Fase 4   trabalhadores                  TASK-011 a TASK-014
                                        verificada em jogo

Fase 5   armazenamento                  TASK-015 a TASK-017
                                        verificada em jogo

Fase 6   recursos                       TASK-018 a TASK-020
                                        verificada em jogo

Fase 7   tarefas                        TASK-021 a TASK-023
                                        verificada em jogo

Fase 8   primeiro trabalhador           TASK-024 e TASK-025
                                        coberta por gametest;
                                        o aldeão andar até a árvore
                                        passou a ser task no Brain,
                                        verde em gametest e ainda
                                        não vista em jogo (§8)
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
Detectar vila             FEITO, verificado em jogo

↓

Registrar aldeões         FEITO, verificado em jogo

↓

Organizar trabalhadores   FEITO, verificado em jogo

↓

Coletar recursos          FEITO, não verificado em jogo

↓

Produzir materiais        não iniciado

↓

Construir expansão        não iniciado
```

Três dos seis passos do MVP estão feitos e verificados em jogo. O
quarto é a Fase 8, e é onde o mod passa a escrever no mundo.

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

Fase 6 — Recursos

  TASK-018  visão agregada           feito (ColonyResources)
  TASK-019  verificação de déficit   feito (ResourceDemand)
  TASK-020  integrar com simulação   feito (ColonyCycle),
                                     verificado em jogo

Fase 7 — Tarefas

  TASK-021  modelo Task              feito (Task, TaskState,
                                     TaskType, TaskPriority)
  TASK-022  TaskService              feito
  TASK-023  associar a profissões    feito (WorkAssignment),
                                     verificado em jogo

Fase 8 — Primeiro Trabalhador

  TASK-024  capacidade do lenhador   feito (LumberjackWork),
                                     coberto por gametest
  TASK-025  coleta de madeira        feito (TreeScanner,
                                     TreeHarvester, ChestDepositor),
                                     coberto por gametest;
                                     bloqueado em jogo pelo
                                     movimento do aldeão (§8)

  extra     nome sobre a cabeça      feito (WorkerNameplate),
                                     verificado em jogo

Fase 9

  TASK-024 em diante                 não iniciadas
```

As TASK-018 e TASK-019 são lógica pura e estão cobertas por teste.
Não trazem dívida de verificação em jogo: não leem o mundo. O que as
alimenta — `ChestInventoryReader` — é que ainda não rodou lá.

---

## Código existente

```text
core/
  type/              ColonyPos, Capability,
                     ResourceType, ResourceCategory
  colony/model/      Colony, ColonyState, ColonyLifecycle, VillageCandidate
  colony/service/    ColonyService, VillageDetector
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
  event/             ServerLifecycleHandler, VillageDetectionHandler,
                     VillagerLifecycleHandler
  integration/       VillageScanner, VillagerScanner, ChestScanner,
                     ChestInventoryReader

data/
  save/              ColonySavedData
```

Vazios por enquanto: `core/construction`, `fabric/mixin`,
`fabric/brain`.

---

## Testes

```text
217 testes, todos passando
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

dá função a quem não tem, cobrindo as quatro antes de repetir

persiste colônias e trabalhadores entre sessões
```

As três últimas linhas nunca rodaram em jogo.

---

# 7. Next Development Step

## Fase 8 — o primeiro trabalhador que age

A TASK-024 e a TASK-025 fazem o lenhador andar até a árvore, quebrar o
bloco e recolher a madeira.

Tudo o que vem antes está pronto e verificado: existe colônia, existe
trabalhador com profissão, existe baú com estoque contado, existe fila
de tarefas e existe quem as distribua. A tarefa `COLLECT_WOOD` já é
criada e reservada por um lenhador a cada ciclo — falta o lenhador
fazer alguma coisa com ela.

---

### O que muda de natureza aqui

```text
até a Fase 7   o mod lê o mundo

da Fase 8      o mod escreve no mundo
```

Bloco quebrado por engano é dano permanente no save do jogador. Nenhum
defeito desta semana teve essa propriedade: baú contado errado é número
errado num log, e centro deslocado se corrige sozinho no ciclo seguinte.

Isso muda o que "verificar" significa. Um gametest que derruba árvore
roda num mundo descartável; uma sessão de jogo do autor, não.

---

### Decisões de regra que a Fase 8 exige

Nenhuma é de implementação, e nenhuma foi tomada:

```text
o que o lenhador pode quebrar

  só tronco? tronco e folha? replanta muda?
  árvore que o jogador plantou é diferente da
  que nasceu com o mundo?

o que acontece com o item

  vai para o baú do trabalhador, fica no chão,
  ou some e vira número no estoque?

até onde ele anda

  raio a partir do centro da colônia, e o que
  fazer quando não há árvore dentro dele
```

A terceira tem custo de desempenho: procurar árvore é varredura de
blocos, e Performance-Rules.md §5 e §6 já proíbem o caminho ingênuo.

---

### O que já está pronto para ser usado

```text
Task com COLLECT_WOOD, estados e executor
WorkAssignment reservando por capacidade
ColonyCycle chamando tudo isso a cada 600 ticks
ChestInventoryReader para depositar depois
runGametest para verificar sem sessão de jogo
```

---

# 8. Priority Queue

Situação em 2026-08-08, fim da sessão.

---

## Bloqueio da Fase 8 — implementado em 2026-08-08

```text
P1   o lenhador não chega à árvore     task no Brain, escrita
```

A causa era a esperada: `LumberjackWork` pedia o caminho por
`getNavigation().startMovingTo`, e o cérebro Vanilla reescrevia o
destino no mesmo tick, seguindo a agenda dele. Quem manda no caminho do
aldeão em 1.21.1 é a memória `WALK_TARGET` — e as tasks Vanilla de
movimento só começam quando ela está vazia. Manter a memória posta
enquanto houver destino é o que segura o aldeão no caminho.

O que existe agora:

```text
VillagerEntityMixin        @Inject TAIL em initBrain, só delega
ColonyBrainInitializer     põe a task em CORE, prioridade 5
GoToWorkTargetTask         escreve WALK_TARGET enquanto houver destino
WorkTargets                UUID → BlockPos, posto pelo ciclo
```

A task só age com destino posto e só no horário de trabalho da agenda
Vanilla; fora dele o aldeão dorme, come e socializa como sempre. Pânico
e incursão vêm antes. Quando a tarefa termina, é solta ou o trabalhador
morre, o destino é apagado e o aldeão volta à rotina no mesmo tick — a
cessão imediata da ADR-004 §5.

Dois pontos da ADR-004 §5 mudaram de lugar na implementação: a task vive
em CORE em vez de numa Activity própria, e o destino vive num mapa do
mod em vez de numa memória customizada. Motivo de cada um em
ADR-004 §11.

**Verificado por gametest, não em jogo.** O teste `lumber_walk` tica o
mundo com um aldeão dentro e falha se ele não se aproximar — foi
conferido desligando a task: com ela desligada o teste falha. É a
primeira vez que um teste deste projeto pega este defeito; os doze
anteriores nunca tocaram o cérebro do aldeão. Falta a sessão de jogo do
autor e a linha `felled` no log.

---

## Precisa de decisão do autor

```text
P2   meta de estoque         antes da Fase 9
```

`ColonyGoals` devolve número fixo — 64 de madeira, 32 de pedra. A meta
real sai do que a expansão pretende construir. Está isolado numa classe
e a assinatura já recebe a colônia.

---

## Não precisa de decisão

```text
A   estender o gametest para o que ainda não cobre

    hoje: detecção, profissão, baú, parede, nível e
    contagem — seis casos.

    falta: morte e zumbificação do trabalhador,
    encolhimento da colônia, e o ciclo gerando tarefa
    a partir de déficit.

    O V3 continua fora do alcance: persistência exige
    fechar e reabrir o mundo, e o gametest roda um
    servidor só.
```

```text
B   consolidar este documento

    passou de 4100 linhas. As consolidações anteriores
    aconteceram com 2534 e 2662, e o Development Log já
    tem mais de trinta entradas.

    A regra do §16 pergunta se quem abre o projeto hoje
    sabe onde as coisas estão. Hoje ainda sabe, pelas
    seções 1 a 14; o §15 é que virou arquivo.
```

---

## Fechado nesta sessão

```text
verificação das Fases 4 a 7      V1 a V7 respondidos
P2 camada de coordenação         core/coordination
P3 loop de simulação             ColonyCycle
P4 propriedade do baú            linha livre cama–baú
item A do §8 anterior            runGametest existe
```

---

## Depois disso

```text
Fase 9 — Expansão
```

É onde a meta de estoque deixa de ser constante e passa a sair do que a
colônia pretende construir.

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
O baú do jogador pode ser reivindicado

  ChestScanner pega o baú livre mais próximo da cama, e não
  tem como saber de quem ele é. Jogador que constrói sua base
  dentro da vila terá baús adotados por aldeões.

  O dano tem dois estágios, e o primeiro já começou:

    agora   o estoque do jogador conta como da colônia.
            ColonyResources soma o baú dele, e ResourceDemand
            conclui que não falta nada com base em madeira
            que não é da colônia. Invisível para o jogador.

    Fase 8  o trabalhador deposita produção no baú dele.
            Aí fica visível, e irritante.

  Precisa de decisão antes da Fase 8, e quanto antes melhor:
  a partir da Fase 7 as tarefas serão geradas a partir de
  números que podem estar contaminados. Não há sinal confiável
  de propriedade no Vanilla; as saídas prováveis são exigir
  que o baú esteja dentro da mesma casa que a cama, ou deixar
  o jogador marcar o baú de alguma forma.

  Em 2026-08-08 a altura passou a ser exigida: o baú tem de
  estar no mesmo nível da cama, com um bloco de folga. Isso
  fecha o andar de cima, não a casa do lado — parede não é
  altura, e a distância continua atravessando qualquer uma.

  Ainda em 2026-08-08 o P4 foi decidido: linha livre entre a
  cama e o baú. Parede desqualifica, e isso cobre tanto o baú
  do vizinho quanto o do jogador. Escrito e não visto em jogo.
```

```text
Profissão não muda depois de atribuída

  ProfessionAssigner só preenche vaga. Realocar conforme a
  necessidade da colônia muda — e liberar a função de quem
  morreu — não pertence ao MVP.

  A morte já libera a vaga — ver VillagerLifecycleHandler.
  O que falta é realocar quem está vivo quando a necessidade
  da colônia muda.
```

```text
Ferramenta inicial não é entregue

  Profession-System.md diz que o trabalhador recebe a
  ferramenta ao assumir a função. ToolType existe e a
  profissão a declara, mas nada põe o item na mão do aldeão.

  Depende do adaptador ToolType -> Item, não escrito.
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
1  Movimento do trabalhador — Fase 8

   O cérebro Vanilla do aldeão tem agenda própria e pode
   sobrescrever o destino que LumberjackWork pede. O
   caminho correto é uma task no Brain, que é mudança
   maior e mexe em como o aldeão se comporta fora do
   trabalho.

   Hoje o pedido é repetido a cada ciclo. Se o aldeão não
   chegar, a linha "felled" não aparece — e é isso que
   uma sessão de jogo vai dizer.
```

```text
1  Meta de estoque — Fase 9

   ColonyGoals devolve 64 de madeira e 32 de pedra para
   toda colônia. Resource-System.md fala em metas mínimas
   e não diz de onde vêm; a resposta é a expansão.

   Isolado numa classe, e a assinatura já recebe a
   colônia.
```

---

### A colheita, decidida em 2026-08-08 e verificada em jogo

O lenhador chegou à árvore e derrubou: `felled 6 logs at 1120, 64, 669`,
às 05:32:44. É a primeira derrubada do projeto em jogo, e fecha o
bloqueio da Fase 8.

Com ela o autor fechou a regra da colheita, que agora tem quatro partes
em ordem:

```text
derrubar a árvore inteira    troncos ligados, até o teto de 24

recolher tudo                o baú é consultado antes; árvore que
                             não cabe fica de pé, porque tronco sai
                             do mundo sem drop e seria destruído

só então replantar           tronco cortado no teto não replanta;
                             a muda entra quando o último cair

abrir a coluna acima         folha na coluna da muda sai da frente,
                             senão ela nunca vira árvore
```

A quarta parte abre uma exceção na regra "folha não é alvo", e a
exceção é estreita de propósito: uma coluna de um bloco de largura,
oito de altura, e a limpeza para no primeiro bloco que não seja folha —
um telhado do jogador acima da árvore encerra ali. Dois testes de jogo
guardam os dois lados, e o teste antigo da folha passou a pôr a folha ao
lado do tronco, que é o caso que a regra sempre quis proteger.

---

Resolvidas em 2026-08-08, mantidas aqui por rastreabilidade:

```text
camada de coordenação   → core/coordination, ADR-006 emendada
loop de simulação       → ColonyCycle
propriedade do baú      → linha livre entre cama e baú

regras do lenhador      → só oak_log, replanta muda;
                          madeira direto para o baú;
                          raio 64 do centro
```

---

```text
3  Ícone e nome divergem

   A arte diz "Village++"; o mod é "Village Colony", id villagecolony.

   Decisão do autor em 2026-08-07: manter como está.

   Trocar o id quebraria saves — ele nomeia
   villagecolony_colonies.dat, o caminho do ícone e o logger.
```

```text
4  Fundo do ícone

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

World.getBlockEntity chamado de dentro do evento de chunk

  a thread do servidor travou; o terreno parou de carregar

Duas premissas erradas sobre o mundo, na regra de encolhimento

  margem de 32 blocos numa vila maior que isso;
  e colônia que nasce do save, não criada na hora

getBlockState forçando chunk, de novo, na Fase 8

  o mesmo erro de dois dias antes, no primeiro código
  que escreve no mundo. O gametest não podia pegá-lo:
  a estrutura de teste está toda carregada
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


  A persistência de trabalhadores não constava do plano original.

  Acrescentada como TASK-012b em 2026-08-07, fora da numeração
  sequencial para não renumerar as tarefas seguintes. Ver §7.
```

```text
Initial-Setup-Checklist.md §6 e Class-Architecture.md

  Continham layouts de pacote divergentes. Agora apontam para

  a ADR-006 em vez de repetir a estrutura.
```

---

# 14. Session Resume Template

Ao retomar em 2026-08-07 ou depois, começar por §8. Ela diz o que
depende do jogo, o que depende de decisão e o que não depende de
nenhum dos dois.

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

## 2026-08-07 — Persistência de trabalhadores decidida

Decisão:

```text
Estender ColonySavedData
```

Recusado: `WorkerSavedData` em arquivo próprio.

Motivo: o `Worker` referencia a colônia por `colonyId`. Dois arquivos
separados permitiriam um trabalhador órfão apontando para uma colônia
que não foi gravada, e não há transação que mantenha os dois em
sincronia. Um só `PersistentState` também evita dobrar o custo de
versionamento futuro.

Registrado:

```text
MVP-Tasks.md   TASK-012b, entre a 012 e a 013
```

A numeração com sufixo evita renumerar da TASK-013 em diante, o que
invalidaria as referências já espalhadas pelos documentos.

Nenhum código foi escrito nesta entrada.

---

## 2026-08-07 — Verificação em jogo adiada a pedido do autor

A fila do §8 punha a verificação da TASK-012 antes de escrever mais
código. O autor não podia rodar o jogo no momento e pediu para seguir.

Consequência aceita: TASK-012b, 013 e 014 foram escritas sobre uma
fronteira nunca exercitada. Registrado aqui para que a origem da dívida
não se perca.

---

## 2026-08-07 — TASK-012b concluída

Criado:

```text
WorkerService.restore        recoloca trabalhador vindo do save
```

Alterado:

```text
ColonySavedData          lista "workers" ao lado de "colonies"

  sync(colonies, workers)  — assinatura mudou, os dois juntos

ServerLifecycleHandler   carrega e grava os trabalhadores
```

Decisão — órfão é descartado na leitura:

```text
Trabalhador cuja colônia não veio no mesmo arquivo é ignorado.
```

Ele não deveria existir, já que os dois são gravados juntos. Se
existir, mantê-lo seria pior que perdê-lo: nenhuma colônia o listaria e
a varredura não o recriaria, porque o `villagerId` já teria dono. Ficaria
invisível para sempre. Descartado, a varredura o reencontra e o reatribui
à colônia certa — ao custo da profissão que ele tinha.

Decisão — profissão ausente ou desconhecida vira "sem função":

```text
Mesmo princípio de readState: não derrubar o mundo.
```

Aqui o custo é menor que no estado da colônia — a TASK-014 dá uma função
nova no próximo ciclo.

Decisão — `restore` lança em duplicata, `register` não:

```text
register  idempotente   a varredura repete de propósito

restore   lança         villagerId repetido no save esconderia
                        qual profissão venceu
```

Verificado:

```text
116 testes passando

./gradlew build → BUILD SUCCESSFUL
```

Não verificado:

```text
O round-trip com mundo real. Nenhum save foi aberto e fechado.
```

---

## 2026-08-07 — TASK-013 concluída

Criado:

```text
core/worker/model/Capability          COLLECT_WOOD, CRAFT_ITEMS,
                                      MAINTAIN_FOOD, BUILD_STRUCTURE

core/worker/model/ToolType            NONE, WOODEN_AXE, WOODEN_HOE

core/worker/model/Profession          definição imutável

core/worker/service/ProfessionRegistry   catálogo das quatro
```

Decisão — `Capability` é tipo próprio, não método de `ProfessionType`:

```text
Duas profissões podem vir a compartilhar uma capacidade.
```

Profession-System.md exige que profissão nova não obrigue a mexer nas
antigas.

Decisão — `ToolType` em vez de `Item`:

```text
ADR-005: o Core não conhece net.minecraft.
```

A conversão para o item Vanilla entra no `MinecraftTypeAdapter` quando
houver quem entregue a ferramenta. Hoje não há.

Decisão — o registro é estático e não tem `clear`:

```text
As quatro profissões são fixas e não pertencem a um mundo.
```

Difere de `ColonyService` e `WorkerService`, que são estado de partida.

Decisão — `of` lança em vez de devolver `Optional`:

```text
Todo ProfessionType tem definição.
```

A falta de uma é profissão acrescentada ao enum sem entrada no catálogo
— erro de programação, não ausência legítima. Há teste que trava isso.

Não implementado:

```text
allowedTasks, previsto em Profession-System.md
```

Depende de `core/task`, hoje vazio. A ligação já é possível pelo outro
lado: a tarefa declara a `Capability` que exige e `canPerform` responde.

---

## 2026-08-07 — TASK-014 concluída

Criado:

```text
core/worker/service/ProfessionAssigner
```

Alterado:

```text
VillageDetectionHandler.registerVillagers

  atribui depois de registrar
```

Regra adotada — sempre a função mais escassa da colônia:

```text
Cobre as quatro antes de duplicar qualquer uma.
```

É a necessidade mínima do Profession-System.md: seis aldeões, um de cada.

Decisão — empate resolvido pela ordem de `ProfessionType`:

```text
LUMBERJACK, MANUFACTURER, FARMER, BUILDER
```

Numa colônia recém-detectada todas as contagens são zero, então é essa
ordem que decide as primeiras quatro atribuições. Ela é a cadeia
produtiva do MVP: começar pelo construtor, sem madeira nem material,
daria um trabalhador sem o que fazer.

Decisão — recontar a cada atribuição, não uma vez por lote:

```text
Quatro aldeões de uma colônia vazia veriam a mesma contagem
e virariam quatro lenhadores.
```

Há teste para isso.

Decisão — a atribuição roda mesmo sem aldeão novo:

```text
Save anterior à TASK-012b traz trabalhadores sem função.
```

Eles precisam receber uma sem depender de alguém nascer.

Verificado:

```text
135 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — TASK-015 e TASK-016 concluídas

Criado:

```text
core/storage/model/WorkerStorage        workerId + posição do baú

core/storage/service/StorageRegistry    quem tem baú, e onde

fabric/integration/ChestScanner         cama -> baú próximo
```

Alterado:

```text
VillagerScanner          procura o baú na mesma passagem

  scan devolve ScanResult, não int

VillageColonyMod         campo STORAGES

ServerLifecycleHandler   esvazia STORAGES junto com os demais

VillageDetectionHandler  loga os baús registrados
```

APIs confirmadas com `javap` no jar mapeado, não de memória:

```text
MemoryModuleType.HOME                     -> GlobalPos

Brain.getOptionalRegisteredMemory(...)

GlobalPos.dimension() / pos()

WorldChunk.getBlockEntities()             -> Map<BlockPos, BlockEntity>

ServerChunkManager.getWorldChunk(x, z)    null se não carregado
```

Decisão — a busca parte das block entities do chunk, não do cubo:

```text
Percorrer o cubo de raio 6 custaria 2197 getBlockEntity
por aldeão sem baú, a cada ciclo.
```

Contra Performance-Rules.md §6. Uma casa tem um punhado de block
entities, e é sobre esse punhado que se itera. O raio vira um filtro,
não um laço.

Decisão — a cama é a casa:

```text
MemoryModuleType.HOME já é a cama do aldeão.
```

É o mesmo POI que a ADR-003 usa para achar a vila, então não há uma
segunda noção de "casa" no código.

Decisão — baú já reivindicado é pulado:

```text
Storage-System.md §"Proteção".
```

Sem isso, dois aldeões do mesmo cômodo partilhariam um baú e cada um
contaria o estoque do outro como seu.

Decisão — `register` substitui, ao contrário de colônia e trabalhador:

```text
O baú registrado pode ter sido quebrado.
```

Reencontrar o dono com outro baú é a recuperação prevista em
Storage-System.md §"Falhas". Recusar prenderia o trabalhador a um baú
que não existe mais.

Decisão — o registro de baús não é persistido:

```text
A posição do baú existe no mundo e é redescoberta.
```

Difere da profissão, que só existe na cabeça do mod. Salvar manteria
uma segunda verdade que envelheceria assim que o jogador quebrasse o
baú.

Decisão — a busca não força carregamento de chunk:

```text
Chunk não carregado é pulado.
```

ADR-002. O baú lá será encontrado no ciclo em que o chunk estiver
carregado.

Não implementado:

```text
"Storage Missing" de Storage-System.md §"Falhas"
```

`StorageRegistry.remove` existe e nada o chama: falta detectar que o
baú registrado sumiu. Enquanto isso, um baú quebrado continua no
registro até o dono achar outro.

Verificado:

```text
148 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — TASK-017 concluída, Fase 5 encerrada

Criado:

```text
core/resource/model/ResourceCategory    NATURAL, PROCESSED, CONSTRUCTION

core/resource/model/ResourceType        OAK_LOG, OAK_PLANKS, COBBLESTONE

core/resource/model/ResourceTally       contagem imutável, somável

fabric/integration/ChestInventoryReader lê os baús
```

Alterado:

```text
MinecraftTypeAdapter      Item -> ResourceType

VillageDetectionHandler   loga o estoque quando um baú novo entra
```

APIs confirmadas com `javap` no jar mapeado:

```text
Inventory.size() / getStack(int)

ItemStack.getItem() / getCount() / isEmpty()

Items.OAK_LOG / OAK_PLANKS / COBBLESTONE
```

Decisão — o tipo do recurso é do Core, o item é da fronteira:

```text
ADR-005. ResourceType não conhece Item.
```

A conversão é uma comparação por identidade no `MinecraftTypeAdapter`:
`Items.OAK_LOG` é singleton do registro, a mesma instância para todo
stack de carvalho do servidor.

Decisão — `ResourceTally` é imutável:

```text
Uma contagem é a fotografia de um momento.
```

O baú muda o tempo todo. Um objeto que se atualizasse sozinho não teria
como dizer de quando é o número que carrega. Somar duas contagens
produz uma terceira, e é assim que os baús viram um total.

Decisão — zero e ausente são a mesma coisa:

```text
of() descarta os zeros.
```

Sem isso, duas contagens que dizem o mesmo não seriam iguais — uma com
`OAK_LOG=0`, outra sem a chave.

Decisão — o total é calculado, não guardado:

```text
O jogador pode esvaziar o baú a qualquer momento.
```

Um total em cache estaria errado sem que nada avisasse. Enquanto a
contagem for barata — um punhado de baús, dezenas de slots — vale
pagar por ela. Quando deixar de ser, o cache precisará de invalidação
por evento, não de um temporizador.

Decisão — baú duplo conta só a metade registrada:

```text
Cada metade é uma block entity com posição própria.
```

Foi uma delas que o trabalhador reivindicou. Contar as duas faria a
colônia enxergar o dobro quando o outro lado fosse reivindicado por
outro aldeão.

Decisão — o log sai só quando um baú novo entra no registro:

```text
O conteúdo muda a cada baú que o jogador abre.
```

Logar por ciclo encheria o arquivo sem dizer nada. Sem nenhuma linha, a
contagem seria invisível em jogo — e o §11 existe porque defeitos desta
camada só aparecem lá.

Nada aqui escreve no baú. O MVP lê; mover item é da Fase 6.

Verificado:

```text
161 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — Duas correções de jogabilidade

Nenhuma das duas veio de tarefa do plano. Vieram de olhar o que já
existia como jogo, e não como código.

---

### Bebê e nitwit não recebem mais função

Sintoma:

```text
ProfessionAssigner dava função a todo trabalhador sem função.
```

Um bebê virava lenhador. Além de absurdo em jogo, ele ocupava a vaga:
`mostNeeded` contava a função como preenchida, e o adulto seguinte
virava fazendeiro numa colônia sem ninguém cortando madeira.

O nitwit tinha o mesmo problema com um agravante. O Vanilla nunca lhe
dá emprego, e o jogador que reconhece o casaco verde espera que ele
continue inútil — PROJECT_CONSTITUTION §4 manda respeitar o
comportamento Vanilla do aldeão.

Correção:

```text
VillagerScanner decide quem pode trabalhar

  !isBaby() && profissão != NITWIT

ScanResult carrega os aptos

assignMissing recebe o conjunto
```

A decisão fica na camada fabric porque é ela que enxerga a entidade. O
Core continua puro: recebe um conjunto de ids e não pergunta por quê.

A contagem de necessidade continua olhando a colônia inteira. Um
lenhador é um lenhador esteja ele à vista ou não — filtrar a contagem
pelos aptos faria a colônia recontratar funções que já tem toda vez que
alguém saísse do raio.

O bebê é registrado como antes. Ao crescer, torna-se elegível sozinho,
no ciclo seguinte, sem nada que trate o caso.

---

### Aldeão morto ou zumbificado deixa de ser trabalhador

Sintoma:

```text
WorkerService.remove existia e nada o chamava.
```

Uma colônia que perdesse o lenhador numa noite de zumbis continuaria
achando que tinha um, para sempre. A vaga nunca reabria. O baú do morto
ficava reservado para sempre, e nenhum outro aldeão podia usá-lo.

Correção:

```text
fabric/event/VillagerLifecycleHandler

  AFTER_DEATH       morreu

  MOB_CONVERSION    virou zumbi
```

Os dois eventos, não só a morte: aldeão mordido por zumbi é
<em>convertido</em>, não morto, então `AFTER_DEATH` nunca dispara — e é
justamente o caso mais comum de perder um trabalhador em jogo.

Remove o trabalhador e o baú dele juntos. Um baú reservado para quem
não existe mais é um baú perdido para a colônia.

Só o evento serve como prova. Ausência na varredura não serve, e é por
isso que `remove` tinha ficado sem quem o chamasse: um aldeão fora do
raio, ou num chunk descarregado, não está morto — apenas não foi visto.

Consequência aceita:

```text
Zumbi curado volta com identidade nova.
```

Será registrado do zero e receberá a função de que a colônia mais
precisar, não necessariamente a que tinha. Preservar a antiga exigiria
rastrear a conversão nos dois sentidos.

---

Não corrigido, e por quê:

```text
O baú do jogador pode ser reivindicado — ver §9.
```

Hoje é inofensivo, porque nada move item. Vira problema real na Fase 6,
e a saída depende de decisão do autor: não há sinal de propriedade no
Vanilla para o mod se apoiar.

Verificado:

```text
164 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Ambas dependem do jogo para valer. Ver §7.
```

---

## 2026-08-07 — TASK-018 e TASK-019; TASK-020 bloqueada

Teste em jogo adiado outra vez a pedido do autor. O §7 foi reescrito
como roteiro de verificação — sete itens, V1 a V7, com o método e a
ordem — para que a sessão de teste não dependa de reconstruir contexto
de memória. Seguiu-se pelo que não precisa do jogo.

Criado:

```text
core/resource/model/ColonyResources     total + repartição por baú

core/resource/service/ResourceDemand    déficit
```

Alterado:

```text
ChestInventoryReader.readColony         agrega guardando a origem

VillageDetectionHandler                 loga o número de baús
```

Decisão — o nome não é `ResourceRegistry`, como em MVP-Tasks.md:

```text
ColonyResources é imutável.
```

Registro sugere algo que se mantém e se atualiza; isto é uma leitura
datada. Prometer atualidade que não se tem seria pior que o nome
diferente. Mesma precedência de `ColonyManager` → `ColonyService`
na TASK-006.

Decisão — a repartição por baú é guardada, não só o total:

```text
O trabalhador vai ao baú, não ao total.
```

Saber que a colônia tem 64 troncos não diz a ninguém para onde andar.

Decisão — baú vazio não entra na agregação:

```text
Senão "três baús com madeira" contaria baús sem madeira.
```

Decisão — déficit não lista o que não falta:

```text
Recurso em dia fica fora do mapa, não entra com zero.
```

Um mapa que lista o que não falta obriga todo chamador a filtrar, e o
primeiro que esquecer vai gerar trabalho para buscar nada.

Decisão — sobra é déficit zero, não negativo:

```text
Ter 100 com meta de 64 é déficit 0, não -36.
```

O excedente é outra pergunta. Misturá-lo faria uma soma de déficits
cancelar falta de um recurso com sobra de outro.

---

### TASK-020 não foi feita, e não por falta de tempo

`MVP-Tasks.md`: "A Colônia deve saber o que possui e o que falta."

Ela já pode responder às duas. O que falta é **onde perguntar**. O loop
de simulação da ADR-002 e do Simulation-Loop.md nunca foi escrito —
§9 registra isso desde a Fase 4 — então não existe ciclo de colônia em
que encaixar a consulta.

Pendurá-la no `VillageDetectionHandler` seria errado: aquilo é
detecção, não simulação. A colônia passaria a pensar sobre recursos
apenas quando um jogador passasse perto o bastante para disparar a
detecção.

Falta também de onde vêm as metas de estoque. Resource-System.md
§"Necessidade de Recursos" fala em "metas mínimas" e dá um exemplo,
mas nada diz quem as define. Provavelmente saem do que a expansão
pretende construir, que é a Fase 9.

É uma lacuna de plano do mesmo tipo da que produziu a TASK-012b, e está
em §10 aguardando decisão do autor.

Verificado:

```text
184 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

---

## 2026-08-07 — TASK-021 e TASK-022; TASK-023 parcial

Criado:

```text
core/task/model/TaskState      AVAILABLE, RESERVED, EXECUTING,
                               COMPLETED, CANCELLED

core/task/model/TaskPriority   SURVIVAL, PRODUCTION, CONSTRUCTION

core/task/model/TaskType       COLLECT_WOOD, CRAFT_MATERIAL, BUILD

core/task/model/Task

core/task/service/TaskService
```

Movido — e este é o ponto que mais mexeu no que já existia:

```text
core/worker/model/Capability       -> core/type/Capability

core/resource/model/ResourceType   -> core/type/ResourceType

core/resource/model/ResourceCategory -> core/type/ResourceCategory
```

Motivo: a ADR-006 §6 proíbe um domínio do Core importar outro. A tarefa
precisa dizer de que capacidade precisa e sobre qual recurso age. Com
`Capability` dentro de `core.worker` e `ResourceType` dentro de
`core.resource`, o domínio task teria de importar os dois — violação
direta.

Os três são vocabulário compartilhado, não regra de um domínio: a
tarefa declara a capacidade, a profissão declara as que tem, e nenhuma
conhece a outra. É o mesmo papel de `ColonyPos`, que já morava ali.

Verificado por varredura: nenhum domínio do Core importa outro.

---

Decisão — `TaskService`, não `TaskManager` como em MVP-Tasks.md:

```text
ADR-006 §5 removeu manager como camada.
```

Mesma decisão da TASK-006, onde `ColonyManager` virou `ColonyService`.

Decisão — existe `CANCELLED`, que MVP-Tasks.md não lista:

```text
Simulation-Loop.md exige o caso.
```

Aldeão morreu, construção removida, recurso dispensado. Sem estado
próprio, a tarefa cancelada teria de ser apagada — e apagar perde a
diferença entre "foi feita" e "deixou de fazer sentido", que é o que a
colônia precisa saber ao reavaliar.

Decisão — `release` é diferente de `cancel`:

```text
release  perdeu o executor, a tarefa continua valendo

cancel   a tarefa deixou de valer
```

Quando o lenhador morre, a colônia ainda precisa de madeira. Cancelar
ali faria a demanda sumir junto com quem a atenderia.

Decisão — reservar o que já está reservado lança:

```text
Simulation-Loop.md: uma tarefa tem um executor só.
```

Substituir em silêncio poria dois aldeões a cortar a mesma árvore, cada
um contando a madeira do outro como sua.

Decisão — `availableFor` já vem ordenada:

```text
A prioridade é regra da colônia, não de quem chama.
```

Deixar cada chamador ordenar abriria espaço para dois pontos do código
escolherem tarefas em ordens diferentes. Empate mantém ordem de
criação, e é disso que depende `sort` ser estável.

Decisão — `purgeClosed` existe:

```text
Sem limpeza o registro cresce para sempre.
```

Uma colônia produzindo por horas acumula milhares de tarefas
concluídas que ninguém consulta.

Decisão — tarefas não são persistidas:

```text
Uma tarefa é intenção do momento.
```

Retomá-la numa sessão em que o mundo mudou faria o aldeão ir cortar
uma árvore que o jogador já derrubou.

---

### O registro de tarefas foi ligado, mesmo vazio

`VillageColonyMod.TASKS` existe e nada cria tarefas: a geração de
demanda é o passo 4 do Simulation-Loop.md, que depende do loop
não escrito.

Foi ligado assim mesmo ao `VillagerLifecycleHandler`, que agora devolve
à fila as tarefas de quem morreu, e ao `ServerLifecycleHandler`, que o
esvazia. Custou três linhas, e o precedente pesou: `WorkerService.remove`
ficou sem chamador até alguém notar que colônia nenhuma reabria vaga.

---

### TASK-023 ficou pela metade, e não por falta de tempo

Os dois lados da associação existem e estão testados:

```text
TaskType declara a Capability

ProfessionRegistry.withCapability diz quem a tem

TaskService.nextFor acha a tarefa de uma capacidade
```

Falta quem junte os dois em tempo de execução. Esse código importaria
`core.task` e `core.worker` juntos, e não há hoje lugar legítimo para
ele — a ADR-006 §6 fecha os domínios, `core/type` é para tipos de valor
e `fabric/` seria regra de colônia morando na camada de integração.

Não criei a camada por conta própria. É decisão de arquitetura, e as
seis ADRs existentes foram todas aprovadas antes de virar código. Está
em §10.

Verificado:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft

Nenhum domínio do Core importa outro (varredura)
```

---

## 2026-08-07 — Sessão encerrada, pendências registradas

Nenhum código nesta entrada. O §8 foi reescrito como ponto de retomada
e passou a separar três coisas que antes estavam misturadas numa fila
só:

```text
o que precisa do jogo         P1

o que precisa de decisão      P2, P3, P4

o que não precisa de nenhum   A, B, C
```

A separação importa porque o projeto passou três sessões seguidas
adiando o item que precisa do jogo, e a fila antiga não deixava óbvio
que havia trabalho disponível sem ele.

Levantadas e não iniciadas, por decisão do autor de encerrar a sessão:

```text
A  Fabric Game Test cobriria V1, V2, V4, V5, V6 e V7

   O fabric-gametest-api-v1 já vem no Fabric API do projeto —
   confirmado no cache de dependências. Falta configurar
   runGametest, que não existe entre as tasks do Gradle hoje.

   Exigiria uma costura no VillageDetectionHandler, porque a
   detecção só é disparada por chunk e por ciclo de ticks.
   Mexer em produção para viabilizar teste é decisão que não
   se toma sozinho.

B  A ADR-006 §6 vira teste em vez de grep manual

C  Este documento passou de 2662 linhas
```

Estado ao encerrar:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

Core sem net.minecraft; nenhum domínio importa outro

9 commits à frente de origin/main, agora empurrados
```

---

## 2026-08-07 — Primeiro defeito de fronteira encontrado em jogo

A sessão de verificação começou e não chegou ao roteiro: o jogo travou o
carregamento de terreno depois de alguns minutos de caminhada.

Antes disso, uma sessão foi perdida por método. O jar instalado em
`.minecraft/mods/` era das 09:41 e continha apenas até a TASK-012 —
conferido abrindo o jar, sem `Profession`, `ChestScanner` nem `Task`
dentro. Nada das Fases 4 e 5 podia aparecer no log porque nada das
Fases 4 e 5 estava rodando. A armadilha do §11 tem agora uma irmã:

```text
Jar velho na pasta de mods não testa nada.
Conferir o conteúdo do jar, não a data do arquivo.
```

O que aquela sessão provou, ainda assim, do V1:

```text
Colony created at ColonyPos[x=886, y=69, z=922] with 26 beds
Colony ... moved to ... with 27 beds
Registered 2 villagers in colony c18264c9 (8 total)
Registered 6 villagers in colony c18264c9 (14 total)
Colony c18264c9 is now DORMANT
Saved 2 colonies with 30 workers
```

Detecção, movimentação de centro, registro incremental sem repetir os
mesmos aldeões, DORMANT ao afastar e gravação ao sair. Falta conferir
se `N` bate com a contagem real da vila, que só se vê em jogo.

---

### O travamento

Com o jar correto, o terreno parou de carregar. Não houve crash, não
houve linha de log — o `latest.log` ficou em zero byte, com o buffer
presa na thread travada. O defeito não se denunciou por log nenhum.

Quem o expôs foi um thread dump do processo, com `jstack`:

```text
"Server thread" ... TIMED_WAITING (parking)
  at net.minecraft.class_1937.method_8321          getBlockEntity
  at ChestInventoryReader.read(ChestInventoryReader.java:49)
  at ChestInventoryReader.readColony(...:116)
  at VillageDetectionHandler.logResources(...:171)
  at VillageDetectionHandler.registerVillagers(...:143)
  at VillageDetectionHandler.detectAround(...:235)
  at VillageDetectionHandler.onChunkLoad(...:78)
  at ServerChunkEvents.lambda$static$0(ServerChunkEvents.java:44)
```

A pilha diz tudo: `onChunkLoad` roda dentro do pipeline de carga de
chunk, na própria thread do servidor. Dali, `World.getBlockEntity`
carrega o chunk que faltar, e a thread passa a esperar por um chunk que
só ela mesma poderia produzir. Ela para, e com ela para todo o
carregamento de terreno.

Correção: `ChestInventoryReader.read` passou a buscar o chunk por
`getChunkManager().getWorldChunk(...)` e a devolver vazio quando ele não
está carregado — exatamente o que o `ChestScanner.findFreeChest` já
fazia, pela ADR-002 §"o mod não segura chunk". Os dois lados da leitura
de baú agora seguem a mesma regra.

Consequência aceita: baú em chunk descarregado não entra na contagem.
É o comportamento correto — a colônia não enxerga o que não está
carregado — mas cai no risco que o V5 já apontava, o de um defeito que
aparece como número plausível em vez de ausência.

---

### O que isto custou e o que ensina

```text
o defeito não estava no código de domínio

  217 testes verdes, e nenhum deles poderia pegá-lo:
  o erro é a thread errada chamando o método certo
```

```text
"instrumentar antes de suspeitar" não bastou aqui

  a thread travou antes de escrever a linha; o log
  ficou em zero byte. Contra travamento, a ferramenta
  é o thread dump, não o log
```

Isto reforça o item A do §8 — o Fabric Game Test rodaria a detecção num
servidor de verdade e teria travado o build, e não a tarde do autor.

Estado ao registrar:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

roteiro V1 a V7 ainda por rodar; a sessão travou antes
```

---

## 2026-08-07 — Fases 4 e 5 verificadas em jogo

A dívida de três sessões foi paga. Seis dos sete pontos do roteiro V1 a
V7 têm agora evidência de jogo real, colhida em duas sessões seguidas
depois da correção do travamento.

---

### V3 — Persistência: CONFIRMADO

```text
[22:14:03] Loaded 2 colonies with 16 workers
```

As duas leituras anteriores diziam `0 workers` e quase foram tratadas
como defeito. Não eram: liam o save das 21:52, escrito pelo jar velho.
O primeiro save escrito pelo jar atual voltou inteiro.

Nenhuma linha `Assigned` seguiu o carregamento — as funções vieram do
save e a atribuição não as desfez, que é o contrato da TASK-012b.

---

### V2 — Atribuição: CONFIRMADO

Lido dentro do `villagecolony_colonies.dat`, não só pelo log:

```text
4 LUMBERJACK · 4 MANUFACTURER · 4 FARMER · 3 BUILDER
o primeiro é LUMBERJACK
```

Rodízio entre as quatro funções, uma linha por lote novo, silêncio ao
reencontrar os mesmos aldeões.

---

### V5 — Contagem de estoque: CONFIRMADO

```text
[22:18:23] stores {OAK_LOG=128} across 2 chests
[22:20:53] stores {OAK_LOG=448, OAK_PLANKS=64} across 8 chests
```

Antes disto, toda linha dizia `nothing tracked across 0 chests`. Isso
era o esperado e não defeito: a colônia acompanha três itens, e baú de
vila vanilla tem trigo, semente e esmeralda.

Ressalva encontrada aqui e corrigida logo depois: a linha
`nothing tracked across 0 chests` não distinguia "baú vazio" de "baú em
chunk que não consegui ler". Ver a entrada seguinte.

---

### V7 — Morte: CONFIRMADO

```text
[22:21:37] Villager ... died, message: 'Saqueador atingiu Aldeão'
[22:21:37] Worker fa196508 died — profession freed, storage released
```

Não foi provocado: um saqueador matou o aldeão durante a sessão. A
contagem seguiu correta — 22 trabalhadores, menos o morto, mais quatro
registrados depois, deu 25.

A zumbificação continua sem exercício, e é o caminho mais comum em
jogo. Exige dificuldade normal ou acima.

---

### V1 e V6 — Indício, não confirmação

O registro é incremental e não repete aldeão já conhecido, ao longo de
três sessões. Falta o único critério que o log não dá: se `N` bate com
a contagem real de aldeões da vila.

Do V6, o indício é bom e aparece várias vezes:

```text
Registered 4 villagers ... / Assigned 3 professions
```

Alguém foi registrado e não empregado, que é o comportamento correto
para bebê e nitwit. O save fecha com 25 trabalhadores e 23 profissões.
Falta esperar um bebê crescer e receber função sozinho.

---

### V4 — Não verificado

Se cada aldeão pegou o baú da própria casa e não o do vizinho, o log
não diz. É inspeção visual, e continua pendente.

---

Estado ao registrar:

```text
V2, V3, V5, V7(morte)   confirmados em jogo
V1, V6                  indício forte, critério final pendente
V4                      não verificado
V7(zumbi)               não exercido

217 testes passando; ./gradlew build → BUILD SUCCESSFUL
```

---

## 2026-08-07 — Duas melhorias que a verificação cobrou

Nenhuma tarefa do MVP. As duas saíram do que a sessão de verificação
mostrou, e as duas fecham buracos de observação, não de funcionalidade.

---

### A contagem de baús passou a dizer o que não conseguiu ler

A correção do travamento criou um caso que antes não existia: baú
registrado cujo chunk não está carregado. `ColonyResources` descarta baú
vazio na agregação, então esse baú e um baú vazio saíam pelo mesmo cano
— sumiam.

O log dizia a mesma coisa nos dois casos:

```text
Colony ... stores nothing tracked across 0 chests
```

"Nenhum baú tem madeira" e "não consegui ler baú nenhum" com o mesmo
texto. É o defeito-que-parece-número que o V5 do §7 já antecipava, e
agora com uma causa concreta atrás dele.

`ChestInventoryReader.survey` devolve `ChestSurvey`: o que foi lido,
quantos baús foram alcançados — vazios inclusive — e quantos ficaram
fora de alcance. A linha passou a ser:

```text
Colony ... stores {OAK_LOG=448} in 8 of 13 chests read
Colony ... stores nothing tracked in 0 of 4 chests read (9 unreachable, chunk unloaded)
```

`readColony` continua existindo e devolvendo só o agregado. O javadoc
de `survey` diz quando preferir uma à outra: decisão de colônia tomada
sobre contagem parcial mandaria um trabalhador buscar o que ela já tem.

Fica registrado que isto é observação, não correção do risco. A colônia
ainda não *usa* `isPartial()` para se recusar a decidir — quando o loop
de simulação existir (§10 item 2), é onde essa recusa mora.

---

### A regra de dependência da ADR-006 §6 virou teste

Era o item B do §8. A regra "nenhum domínio do core importa outro" era
conferida por `grep`, à mão — o que some no dia em que alguém esquecer
de rodar. Ela já tinha cobrado uma vez, obrigando a mover `Capability`
e `ResourceType` para `core/type` na Fase 7.

`DependencyRuleTest` lê os fontes de `core/` e trava três coisas:

```text
core não importa net.minecraft nem net.fabricmc

core não importa fabric nem data

nenhum domínio do core importa outro, exceto core/type
```

Lê fonte, e não bytecode, de propósito: a regra é sobre `import`, que é
o que o autor escreve e o que a ADR proíbe. O bytecode já perdeu a
diferença entre um import e um nome qualificado.

O teste foi verificado ao contrário antes de entrar. Um arquivo
temporário em `core/colony/model` importando `Worker` e `BlockPos` fez
os dois testes certos falharem, com a mensagem apontando arquivo e
import:

```text
ADR-006 §6 — nenhum domínio do core importa outro. Violações:
  .../TempViolation.java (colony) importa com.villagecolony.core.worker.model.Worker
```

O quarto teste, `theScanReachesTheSource`, existe porque uma varredura
que não acha arquivo nenhum passa sempre — e passaria calada se o
caminho relativo quebrasse.

---

Estado ao registrar:

```text
221 testes passando (eram 217)

./gradlew build → BUILD SUCCESSFUL
```

---

## 2026-08-07 — A colônia pode encolher

Decisão do autor, tomada a partir de uma pergunta levantada na sessão de
verificação. `observedBeds` só crescia: uma vila que perdesse camas —
zumbis destroem, o jogador derruba — ficaria com o centro congelado no
lugar antigo para sempre, porque nenhuma observação futura alcançaria a
marca antiga. A partir da Fase 8 isso mandaria trabalhador andar até um
centro que não existe mais.

---

### O que não podia ser desfeito junto

A regra que impedia o encolhimento é a mesma que impede a oscilação do
§11, e essa oscilação chegou a custar o UUID de uma vila. Baixar a
guarda por completo devolveria o defeito.

O que separa os dois casos é *autoridade*: uma observação que viu menos
camas ou viu menos da vila, ou a vila encolheu de fato, e o log não
distinguia as duas.

---

### A prova de completude

Uma observação é completa quando provadamente não cortou cama alguma:

```text
toda cama de um cluster está a no máximo CLUSTER_DISTANCE
de outra cama dele — é a definição de cluster

logo, se toda cama vista está a até
SEARCH_RADIUS - CLUSTER_DISTANCE do gatilho,
qualquer cama ligada a elas cairia dentro do raio
e teria sido coletada

64 - 32 = 32 blocos de margem
```

Dentro da margem, "vi menos camas" só pode significar que a vila
encolheu. Fora dela, a resposta é "não sei", e o seguro é continuar
recusando.

`VillageCandidate` ganhou `complete`, `Colony.observe` ganhou o terceiro
argumento, e quem prova é a detecção — o Core não sabe o que é raio de
busca. `VillageScanner` passa o gatilho, que já tinha em mãos.

Limite conhecido e aceito: a prova mede na horizontal, como a
clusterização. Uma cama muito acima ou abaixo das outras entra no
cluster e poderia cair fora da esfera de busca. Vila Vanilla é de
superfície, e o erro possível é a colônia deixar de encolher — nunca
encolher errado.

---

### Como foi verificado

Teste antes de código. Os testes novos não compilavam contra a API
antiga, que é o vermelho legítimo aqui.

Depois de verdes, a regra foi revertida à mão dentro de `observe` para
confirmar que os testes a sustentam:

```text
if (beds < observedBeds) {        ← sem o && !complete

  completeViewMayShrinkTheColony        FALHOU
  completeViewMayShrinkAndMoveTheCenter FALHOU
  observeReportsMovementWhenShrinking   FALHOU

  os 12 restantes seguiram passando
```

Os doze que continuaram verdes importam tanto quanto os três que
falharam: são eles que garantem que a oscilação não voltou junto.

---

Estado ao registrar:

```text
231 testes passando (eram 221)

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo: nenhuma vila encolheu ainda
```

---

## 2026-08-07 — A prova geométrica não servia, e o jogo mostrou

Camas foram destruídas em jogo e a colônia não encolheu. O
`observedBeds` seguiu em 38, conferido dentro do `.dat`.

O log não sabia dizer por quê: a linha `moved` só sai quando o centro
muda, e encolher sem mover o centro é silencioso. Duas causas possíveis,
com correções diferentes — a regra recusou a observação menor, ou
observação menor nenhuma chegou.

Instrumentar antes de suspeitar. A linha nova saiu na sessão seguinte:

```text
[23:13:21] saw 32 beds, keeping 38 — view not provably complete
[23:13:49] saw  3 beds, keeping 38 — view not provably complete
[23:13:49] saw 33 beds, keeping 38 — view not provably complete
[23:14:21] saw 33 beds, keeping 38 — view not provably complete
[23:15:58] saw 32 beds, keeping 38 — view not provably complete
```

Cinco observações da vila encolhida, cinco recusas. A de 3 camas está
correta — é visão de borda. As de 32 e 33 é que deviam ter passado.

---

### O defeito era do critério, não do jogo

A prova exige toda cama a até 32 blocos do gatilho. Esta vila é maior
que isso, e por isso nenhuma observação real ali jamais se prova
completa. A regra estava correta no papel e inalcançável na prática.

Os testes não pegaram porque usam clusters de três camas a dez blocos de
distância — passavam com folga. O critério só funcionava em vila
pequena, e não havia teste com vila do tamanho das de verdade.

---

### A mesma janela

Decisão do autor entre três mecanismos. Escolhido: a colônia guarda de
onde veio a melhor observação, e uma varredura do mesmo ponto vendo
menos camas tem autoridade para encolher.

```text
Colony.observedFrom      âncora da melhor observação

VillageCandidate.anchor  de onde a varredura partiu

ciclo longo varre também a partir do centro de
cada colônia ativa — âncora estável entre ciclos
```

A posição do jogador nunca se repete entre ciclos; o centro da colônia
sim. Sem essa segunda varredura a âncora nunca casaria e o mecanismo
não dispararia nunca — o mesmo erro da prova geométrica.

Roda depois de `updateLifecycles`, para não varrer colônia dormente:
sem chunk carregado a varredura não acharia cama alguma e a colônia se
veria vazia.

Custo: uma consulta de POI por colônia ativa a cada ciclo de 30s. O
limite de Performance-Rules.md §5 continua respeitado — busca por raio
em torno de um ponto, nunca pelo mundo.

A prova geométrica ficou. É rara, mas é a única que serve na primeira
observação, quando ainda não há âncora com que comparar.

---

### Como foi verificado

Teste antes de código, e o primeiro deles é o caso real: vila de 38
camas, observação de 33 da mesma âncora, encolhe.

Duas mutações confirmaram que os testes sustentam as duas decisões:

```text
sem a comparação de âncora

  sameAnchorSeeingFewerBedsShrinksTheColony  FALHOU
  aBetterViewMovesTheAnchorToo               FALHOU

âncora atualizada mesmo na recusa

  aWorseViewDoesNotBecomeTheNewAnchor        FALHOU
```

A segunda mutação é a que importa mais: ela é exatamente a deriva do
§11 voltando por outro caminho, e agora existe teste que a barra.

---

Estado ao registrar:

```text
238 testes passando (eram 231)

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo: a vila encolhida ainda não encolheu a colônia
```

---

## 2026-08-07 — A âncora que nunca nascia

Segunda tentativa, segunda recusa em jogo. A sonda do centro estava
rodando — o log passou a trazer duas linhas por ciclo, uma de cada
varredura — mas a colônia seguiu em 38:

```text
[23:26:03] saw 33 beds, keeping 38   ← jogador
[23:26:03] saw 33 beds, keeping 38   ← sonda
[23:26:33] saw 33 beds, keeping 38
[23:26:33] saw 33 beds, keeping 38
```

O impasse estava no meu código, e é do tipo que teste de unidade não
encontra porque depende do estado com que a colônia nasce:

```text
a colônia vem do save com observedBeds = 38
e âncora nula

a âncora só era gravada numa observação ACEITA

nenhuma observação é aceita enquanto 33 < 38

logo a âncora nunca nasce, e nada nunca encolhe
```

Os testes anteriores não pegaram porque todos partiam de uma colônia
criada na hora, cuja primeira observação é sempre aceita e já deixava a
âncora pronta. Nenhum partia de colônia carregada do save.

---

### A sonda passou a ter memória própria

`probeAnchor` e `probeBeds` são gravados a cada leitura da sonda, aceita
ou recusada. Deixaram de ser um efeito da observação aceita e viraram o
que sempre deveriam ter sido: o registro do que a sonda viu da última
vez.

A regra de encolhimento ficou:

```text
mesma âncora da leitura anterior
   e  a leitura de agora não é maior que aquela
   e  aquela já estava abaixo da contagem registrada
```

A terceira condição é a que exige repetição. Sem ela, a sonda que viu 38
e depois 33 confirmaria o 33 contra si mesma, e uma visão parcial
isolada encolheria a colônia — dois testes falharam exatamente nisso
antes de a condição existir.

---

### Só a sonda tem âncora

A varredura que parte do jogador e a que parte do chunk carregado passam
`anchor` nulo. Não é detalhe: um jogador parado na borda da vila repete
a mesma visão pobre ciclo após ciclo, e ela se confirmaria sozinha. A
deriva do §11 entraria pela porta que abrimos para o encolhimento.

`VillageScanner.scan` recebe `isProbe` e retira a âncora quando a
varredura não é sonda. A prova de completude continua valendo nas duas,
porque ela não depende de repetição.

---

Estado ao registrar:

```text
233 testes passando

  eram 238; sete testes da âncora antiga foram
  substituídos por oito da sonda, e o total caiu
  porque a regra antiga tinha caso que não existe mais

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo, pela terceira vez
```

---

## 2026-08-07 — A colônia encolheu em jogo

Terceira tentativa, e desta vez funcionou. A vila tinha 38 camas
registradas e 33 reais desde que camas foram destruídas.

```text
[23:36:42] saw 33 beds, keeping 38    ← sonda registra
[23:36:42] saw 33 beds, keeping 38

           (a sonda confirmou 33 aqui)

[23:38:42] saw  3 beds, keeping 33    ← já é 33
[23:38:42] saw 13 beds, keeping 33
[23:39:12] saw 25 beds, keeping 33
```

Confirmado fora do log, dentro do `.dat`: `observedBeds` passou de 38
para 33 e sobreviveu ao salvamento.

As três linhas depois do encolhimento são a outra metade da prova.
Visões de 3, 13 e 25 camas foram recusadas contra os 33 — a guarda
contra a deriva do §11 continua de pé enquanto o encolhimento funciona.
As duas coisas conviviam mal em teoria e convivem bem em jogo.

---

### O que custou

Três sessões de jogo do autor e duas correções minhas, ambas do mesmo
tipo: código que passava em 230+ testes e não funcionava na primeira
vez que encontrou o mundo real.

```text
prova geométrica    correta e inalcançável
                    margem de 32 blocos, vila maior que isso

âncora da sonda     nascia só numa observação aceita,
                    e nenhuma vinha enquanto a colônia
                    estivesse grande demais
```

Nenhum dos dois é erro de lógica. Os dois são erro de *premissa sobre o
mundo* — o tamanho de uma vila real, e o estado com que uma colônia
começa a sessão.

---

Estado ao registrar:

```text
233 testes passando

./gradlew build → BUILD SUCCESSFUL

encolhimento verificado em jogo
```

---

## 2026-08-07 — V7 fechado; o V4 era inverificável

A zumbificação apareceu em jogo, e distinta da morte:

```text
Worker 0bdd1c8a was converted — profession freed
Worker fc3c2162 was converted — profession freed
Worker 457439b1 was converted — profession freed, storage released
```

Os dois desfechos convivem no mesmo ataque — na mesma investida
apareceram sete linhas de `died` e três de `was converted`. A diferença
entre "morreu" e "virou zumbi" é o caminho mais comum em jogo e o que o
§7 marcava como o mais fácil de deixar passar. Está exercido.

A terceira linha mostra a outra metade: `storage released` só sai para
quem tinha baú. Quem não tinha, não solta.

Com isto o V7 fecha inteiro. Restava o V4.

---

### O V4 não falhou: ele não podia ser respondido

"Cada aldeão pegou o baú da sua casa, não o do vizinho." O autor foi
verificar em jogo e a resposta foi que não dá para saber.

```text
Registered 7 storages in colony ... (7 total)
```

Quantos, e nada mais. Nem qual baú, nem de quem, nem onde. Não existe
UI, e o baú reivindicado é igual a qualquer outro. O critério estava no
roteiro desde o começo e nenhuma sessão poderia tê-lo respondido.

É o mesmo padrão da linha `nothing tracked across 0 chests` e da linha
que faltava no encolhimento: um critério de verificação escrito sem
que existisse a instrumentação para satisfazê-lo. Três vezes no mesmo
dia.

`ChestScanner` passou a registrar cada reivindicação:

```text
Storage claimed by <uuid>: bed 1109,68,730 chest 1112,68,731 (3,3 blocks apart)
```

São as coordenadas de ir até lá e abrir o baú, que é a verificação que
o V4 pede. Uma linha por baú reivindicado, e o baú de um aldeão é
procurado uma vez só enquanto ele o tiver.

---

Estado ao registrar:

```text
V2, V3, V5, V7      confirmados em jogo
encolhimento        confirmado em jogo
V1, V6              indício forte, critério final pendente
V4                  instrumentado, ainda por verificar

233 testes passando; build verde
```

---

## 2026-08-08 — V4 respondido: um baú estava noutro andar

A instrumentação da véspera respondeu na primeira sessão. Dezesseis
baús reivindicados, com cama, baú e distância:

```text
13 de 16    1,4 a 2,4 blocos    mesmo cômodo
 3 de 16    ~5 blocos           suspeitos
```

O autor foi conferir os três em jogo. Um estava errado, e de um jeito
que a distância não denunciava:

```text
7ae2b8d3   cama 1068,65,735   baú 1068,70,735   5,0 blocos
```

Mesmo x, mesmo z, cinco blocos acima. Distância no espaço não conhece
teto: o baú estava dentro do raio de seis e noutro andar.

---

### A regra

Decisão do autor: o baú tem de estar no mesmo nível da cama.

```text
MAX_LEVEL_DIFFERENCE = 1
```

Um bloco de folga, e não zero, porque chão de vila vanilla tem degrau —
casa com piso 68 de um lado e 69 do outro é comum. Os números da sessão
sustentam a escolha: quinze dos dezesseis baús estavam a zero ou um
bloco de altura da cama; só o do outro andar estava a cinco.

A vertical saiu do cubo de busca e virou limite próprio. O horizontal
continua em seis.

Nada a migrar: o registro de baús não é persistido, é refeito a cada
sessão. A reivindicação errada simplesmente não se repete.

---

### O que a regra não resolve

Os outros dois suspeitos — camas em z=714 com baús em z=719 — não são
de altura. Se estiverem errados, é parede no meio, e altura não
distingue parede.

```text
o baú do vizinho continua alcançável
```

Isso é o item P4 do §8, a propriedade do baú, que já estava na fila e
segue lá. A regra de hoje fecha o andar de cima; a casa do lado
continua aberta.

---

Estado ao registrar:

```text
V1 a V7             respondidos; V4 gerou correção
encolhimento        confirmado em jogo

233 testes passando; build verde

a regra de nível ainda não foi vista em jogo
```

---

## 2026-08-08 — P2 e P3 decididas; a colônia passou a pensar

Duas decisões do autor destravaram as TASK-020 e TASK-023, paradas
desde a Fase 6.

---

### P2 — `core/coordination`

O código que casa tarefa com profissão precisa de `core.task` e
`core.worker` na mesma linha, e a ADR-006 §6 proíbe domínio importar
domínio. As saídas eram três; a escolhida foi um pacote próprio no core,
acima dos domínios.

A emenda é estreita: só `core/coordination` pode importar domínios, e
domínio nenhum passa a importar outro. `DependencyRuleTest` ganhou a
exceção e um teste para a direção contrária — coordenação lê domínio,
domínio não lê coordenação, ou a regra viraria um ciclo com nome bonito.

A exceção foi verificada por mutação: sem ela, `WorkAssignment` aparece
como violação da regra.

Descartada a alternativa de deixar em `fabric/`, que não exigiria emenda
nenhuma: é a única camada sem um teste de unidade sequer, e é onde todos
os defeitos desta semana apareceram.

---

### TASK-023 — quem faz o quê

`WorkAssignment` percorre os trabalhadores ociosos, lê a profissão de
cada um e reserva a tarefa mais urgente que ele saiba fazer.

```text
uma tarefa por trabalhador, um trabalhador por tarefa

  a Fase 8 vai mandar o aldeão andar até o local,
  e quem tivesse duas andaria para dois lugares

ocioso é quem não tem tarefa aberta

  não quem está parado: a caminho da árvore
  continua ocupado
```

Percorre as capacidades da profissão, não só a primeira: pedreiro que
também carrega madeira pega madeira quando não há o que construir.

---

### P3 — o loop de simulação existe

`ColonyCycle` é o ciclo da ADR-002, e roda uma vez por colônia ACTIVE a
cada `CYCLE_TICKS`:

```text
comparar o que tem com o que quer
pedir o que falta, retirar o pedido sem motivo
entregar os pedidos a quem sabe atendê-los
```

Decisões que o ciclo carrega, cada uma com um teste:

```text
um pedido por recurso, não um por ciclo

  30s de ciclo com falta permanente encheria a fila
  sem limite

pedido cancelado quando a falta acaba

  o jogador enche o baú, e o pedido perde o motivo

tarefa já iniciada não é cancelada

  a contagem muda o tempo todo, e ninguém é
  interrompido a meio caminho da árvore
```

---

### O que o ciclo se recusa a fazer

A colônia não decide sobre contagem parcial:

```text
if (survey.isPartial()) return;
```

Baú em chunk descarregado sai da soma sem avisar. Uma colônia que
concluísse "falta madeira" com metade dos baús fora de alcance mandaria
um trabalhador buscar o que ela já tem. O `ChestSurvey` de ontem existia
para dar o aviso; hoje ele tem quem o ouça.

---

### O provisório assumido

`ColonyGoals` devolve meta fixa para toda colônia: 64 de madeira, 32 de
pedra. Resource-System.md fala em metas mínimas e não diz de onde vêm; a
resposta real é a Fase 9, quando a meta sair do que a expansão pretende
construir.

Está isolado numa classe só, e a assinatura já recebe a colônia — trocar
por meta de obra não toca em `ColonyCycle`.

---

Estado ao registrar:

```text
257 testes passando (eram 233)

./gradlew build → BUILD SUCCESSFUL

nada disto foi visto em jogo ainda
```

---

## 2026-08-08 — P4 decidida: parede define propriedade

A última das três decisões do §8. Não havia critério nenhum de
propriedade: o `ChestScanner` pegava o baú livre mais próximo da cama e
não tinha como saber de quem ele era.

Sobravam dois casos que a distância não separa, e eles são o mesmo caso:

```text
o baú do vizinho    casa geminada, cama de um lado da parede
                    e baú do outro

o baú do jogador    base construída encostada na vila, sem
                    sinal no Vanilla que diga que é sua
```

Escolha do autor entre três saídas: linha livre entre a cama e o baú.

```text
dá para ir da cama ao baú sem atravessar bloco?

  sim  → mesmo cômodo, o baú é da casa
  não  → parede no meio, não é da colônia
```

Uma regra resolve os dois, porque nenhum dos dois tem sinal próprio no
Vanilla e os dois têm parede.

Descartadas: marcar o baú à mão, que inventa regra de jogo que o MVP não
tinha; e adotar só o que já existia na vila, que quebra "o jogador
constrói para a colônia".

---

### Como

Um traço do centro da cama ao centro do baú,
`RaycastContext.ShapeType.COLLIDER`. Bater no próprio baú é chegar — ele
é sólido e é o alvo. Bater na própria cama é sair, porque o traço começa
dentro dela.

Custo: um traço por baú candidato, e só quando o aldeão não tem baú.
Para quem tem, a busca inteira nem começa.

---

### O que isto piora, e por que ainda assim vale

Um baú na volta de um corredor em L está no mesmo cômodo e não tem linha
livre. Ele deixa de ser reivindicado.

É o erro na direção certa: a colônia deixa de adotar um baú que era
dela, em vez de adotar um que não era. O primeiro é um aldeão sem baú; o
segundo é a colônia contando o estoque do jogador e, a partir de ontem,
gerando tarefa com base nele.

Também custa mais para o aldeão que não acha baú nenhum: a busca se
repete a cada ciclo, e agora com um traço por candidato. São poucos baús
num raio de seis.

---

### Urgência que apareceu ontem

Enquanto nada consumia a contagem, a contaminação era inerte. Com o
`ColonyCycle` ligado, o número errado passou a **gerar tarefa**: a
colônia que soma o baú do jogador conclui que não falta madeira e não
pede nada, ou o contrário.

O §9 descrevia esse primeiro estágio como "invisível para o jogador".
Deixou de ser.

---

Estado ao registrar:

```text
257 testes passando; build verde

as três decisões do §8 estão fechadas

P4 é código de fronteira e não tem teste — a camada
fabric segue sem nenhum. Precisa de jogo
```

---

## 2026-08-08 — A colônia pensou em jogo, e o P4 fechou

Uma sessão, duas verificações, nenhuma correção necessária. Primeira vez
na semana.

---

### O loop de simulação funcionou

Linha inédita, no primeiro ciclo:

```text
Colony 0c2771b0 assigned 1 tasks (0 open)
Colony 9a5afa23 assigned 2 tasks (0 open)
```

Os números fecham com a meta de `ColonyGoals` — 64 de madeira, 32 de
pedra — e é isso que prova que a cadeia inteira está ligada, não só que
o código rodou:

```text
0c2771b0   stores {OAK_LOG=192}     madeira satisfeita,
                                    falta pedra      → 1 tarefa

9a5afa23   stores nothing tracked   falta as duas    → 2 tarefas
```

`(0 open)` diz que toda tarefa criada foi reservada no mesmo ciclo por
um trabalhador com a capacidade certa. TASK-020 e TASK-023 confirmadas
em jogo.

---

### O P4 fez o que devia

Quinze baús reivindicados, contra dezesseis antes das duas regras:

```text
14 de 15    entre 1,4 e 2,8 blocos
 1 de 15    a 5,4 blocos
```

Os dois casos que sobravam foram tratados por regras diferentes, e dá
para ver cada uma agindo:

```text
1068,65,735 → 1068,70,735    sumiu pela regra de nível

1130,69,714 → 1130,69,719    sumiu pela regra de parede
```

O segundo é o que importa: era um dos dois pares geminados de z=714 para
z=719, e a parede o barrou enquanto deixava o vizinho passar. A regra
não é um filtro cego de distância.

Sobrou um a 5,4 blocos, `1130,69,714 → 1132,69,719`, e ele não era
denunciável pelo log — cinco blocos tanto pode ser sala aberta quanto
casa do lado. O autor foi ao lugar e confirmou que dá para andar de um
ao outro sem sair da casa.

A reivindicação está correta. O V4 do §7 — "cada aldeão pegou o baú da
sua casa, não o do vizinho" — está respondido, e afirmativamente.

---

### O que isto encerra

```text
Fases 4 a 7    escritas, verificadas em jogo

V1 a V7        respondidos

§8  P2, P3, P4 decididos e implementados
```

A Fase 8 não tem mais bloqueio. TASK-024 e TASK-025 são o lenhador que
anda até a árvore, quebra o bloco e traz a madeira — a primeira vez que
o mod vai escrever no mundo em vez de só lê-lo.

---

Estado ao registrar:

```text
257 testes passando; build verde

nada pendente sem verificação
```

---

## 2026-08-08 — A camada fabric ganhou o primeiro teste

Item A do §8, aprovado pelo autor. `./gradlew runGametest` sobe um
servidor sem cliente, monta a vila, afirma e falha o build.

```text
All 2 required tests passed :)
```

Dois testes, ambos cobrindo o caminho que nenhum teste de unidade
alcança:

```text
aVillageBecomesAColony              POI vira detecção vira colônia

villagersBecomeWorkersWithAProfession   e vira trabalhador com função
```

São carregados: com `MIN_VILLAGERS` mutado para 99, os dois falham.

---

### A costura

`VillageDetectionHandler.runCycleNow` é o único código do mod que existe
por causa de teste. Faz o que o ciclo longo faz, na mesma ordem —
detectar, atualizar lifecycle, sondar do centro, simular. Se divergir do
`onServerTick`, o teste passa a verificar um caminho que o jogo não
percorre, que é pior do que não ter teste.

Em jogo nada a chama.

---

### Três premissas erradas, todas descobertas rodando

O padrão da semana se repetiu, com a diferença de que desta vez o
retorno levou segundos em vez de uma sessão do autor.

```text
meia cama não vira POI

  o primeiro teste plantava um bloco de cama só. O POI
  HOME nasce da cabeceira, e sem POI não há vila

o bioma não era problema

  a suspeita era que o mundo de teste fosse void e a
  detecção recusasse. A diagnose respondeu:
  minecraft:plains

o mundo é partilhado

  o teste negativo plantou duas camas e a diagnose achou
  cinco POIs no raio: as estruturas dos outros testes
  ficam a menos de 64 blocos
```

A mensagem de falha só respondeu isso porque carrega uma diagnose — POIs
no raio, bioma e posição absoluta. Sem ela, "esperava 1, achei 0"
mandaria adivinhar entre as três.

---

### O que não cabe aqui

O caso negativo — "camas de menos não são vila" — foi tentado e
descartado com prova. "Não existe colônia" é propriedade global, e
nenhum teste pode afirmá-la num mundo que todos partilham. Separar em
batches não resolve: os blocos permanecem entre eles.

Está coberto onde cabe, em `VillageDetectorTest`, que é onde a regra
vive.

O V3 continua humano: persistência exige fechar e reabrir o mundo, e o
gametest roda um servidor só.

Baú e estoque ainda não têm teste de jogo. São o próximo alvo natural, e
agora custam minutos em vez de sessão.

---

Estado ao registrar:

```text
257 testes de unidade + 2 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 2 required tests passed
```

---

## 2026-08-08 — O teste de jogo pagou na primeira hora

Quatro testes novos cobrindo baú e estoque, e o primeiro defeito de
produção encontrado por máquina neste projeto.

```text
All 6 required tests passed :)
```

---

### A regra de parede era um nada

A regra do P4, escrita e dada como verificada horas antes, não filtrava
coisa alguma.

O traço partia do centro da cama. A cama é sólida: ele batia nela mesma
no primeiro passo, e a regra tratava esse acerto como "cheguei". O
resultado era `true` para qualquer baú, parede incluída.

O teste plantou cama, parede de pedra e baú atrás dela, e o baú foi
reivindicado. A sonda dentro do teste apontou o culpado:

```text
traço: BLOCK em -11912041, -59, -2021780   ← a própria cama
```

Correção: o traço parte de um bloco acima da cama, onde estaria a cabeça
de quem levanta. Chegar passou a ser bater no próprio baú, ou não bater
em nada.

---

### Uma conclusão de ontem estava errada

Na sessão em jogo eu afirmei que a regra de parede tinha barrado um dos
dois pares geminados — o `4b09f9cb`, de `1130,714` para `1130,719`, que
sumiu da lista.

Não foi a regra. A regra não filtrava nada. Aquele baú sumiu porque foi
reivindicado por uma cama mais perto: `1ef9c30b`, de `1130,717`, está a
dois blocos dele contra cinco.

A evidência era compatível com a explicação e não a sustentava. Fica
registrado porque o erro não foi de código: foi de leitura.

---

### O que os quatro testes cobrem

```text
aChestInTheSameRoomIsClaimed        o caminho feliz
aChestBehindAWallIsNotClaimed       a regra do P4
aChestOnAnotherLevelIsNotClaimed    a regra de nível
theColonyCountsWhatTheChestHolds    o V5, com número afirmado
```

Todas as afirmações são ancoradas na posição do baú plantado, via
`StorageRegistry.isTaken`. Contagem global não vale aqui, e é o mesmo
motivo de §15 mais acima: o mundo é um só e as estruturas ficam a menos
de 64 blocos. "Este baú tem dono" e "este baú não tem" são locais e
valem.

A memória `HOME` do aldeão é escrita à mão. Em jogo o cérebro dele
reivindica a cama sozinho, o que leva tempo e depende do ciclo dele;
o que estes testes verificam é o que o mod faz depois de existir casa.

---

### O que isto significa para o método

```text
o defeito estava em produção desde o commit e6ac113

a sessão de jogo que o "verificou" não podia pegá-lo:
o sintoma era um baú a mais reivindicado num canto da
vila, indistinguível de um baú legítimo

o teste pegou em segundos, e disse onde
```

É o argumento do item A por inteiro, e agora com um caso concreto.

---

Estado ao registrar:

```text
257 testes de unidade + 6 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 6 required tests passed

a correção da regra de parede não foi vista em jogo
```

---

## 2026-08-08 — Fecho da sessão

O que foi feito, na ordem em que aconteceu:

```text
paga a dívida de verificação das Fases 4 e 5
  V1 a V7 respondidos; V4 gerou correção

quatro defeitos de fronteira achados e corrigidos
  travamento da thread do servidor
  prova geométrica inalcançável
  âncora da sonda que nunca nascia
  baú noutro andar

decidido e implementado o encolhimento da colônia

decididas e implementadas as três do §8
  P2 core/coordination, ADR-006 emendada
  P3 ColonyCycle, o loop da ADR-002
  P4 linha livre entre cama e baú

item A do §8: runGametest existe, com seis casos

quinto defeito, achado por máquina
  a regra de parede não filtrava nada
```

Fases 4 a 7 fechadas e verificadas em jogo. §3, §5, §6, §7, §8 e §10
reescritos para descrever o projeto de hoje.

---

### Duas correções a coisas que eu afirmei

```text
"a regra de parede barrou o par 1130,714 → 1130,719"

  não barrou. A regra não filtrava nada. O baú foi para
  uma cama mais perto, a dois blocos contra cinco.

contagem de testes num commit

  informei 251; eram 246. Corrigido no commit seguinte.
```

A primeira é a que importa: a evidência era compatível com a explicação
e não a sustentava, e foi apresentada como se sustentasse.

---

### O que fica para a próxima sessão

```text
decisão   regras do lenhador — §7 e §10 item 1

trabalho  TASK-024 e TASK-025, a Fase 8

dívida    gametest para morte, zumbificação,
          encolhimento e geração de tarefa

dívida    consolidar este documento, item B do §8
```

---

Estado ao encerrar:

```text
257 testes de unidade + 6 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 6 required tests passed

nada escrito e não verificado, salvo a correção da
regra de parede, que tem gametest e não tem jogo
```

---

## 2026-08-08 — Fase 8: o mod passou a escrever no mundo

TASK-024 e TASK-025. A virada de natureza do projeto: até aqui tudo
lia, agora derruba árvore e guarda madeira.

---

### As regras, decididas pelo autor

```text
alvo     só oak_log. Folha, terra e qualquer outro
         bloco ficam. Ao terminar a árvore, planta
         muda na base

item     vai direto para o baú do trabalhador, sem
         passar por item no chão

alcance  64 blocos do centro da colônia
```

O item foi a decisão menos óbvia. Item no chão despawna em cinco
minutos, cai n'água, e outro mob o pega — a contagem da colônia passaria
a mentir sem nada avisar, que é a família de defeito que mais custou
nesta semana.

O alcance ficou em 64, e não nos 32 que eu recomendei: é o mesmo raio da
detecção de vila.

---

### O que foi escrito

```text
TreeScanner      acha o carvalho mais próximo do centro

TreeHarvester    derruba os troncos ligados e replanta

ChestDepositor   guarda no baú o que couber, devolve o resto

LumberjackWork   o passo de trabalho por ciclo: achar,
                 andar, derrubar, guardar
```

`TreeScanner` não varre volume. Raio 64 em três dimensões são milhões de
blocos, e Performance-Rules.md §5 e §6 proíbem esse caminho: ele percorre
colunas em espiral a partir do centro, usa o mapa de altura para saber
onde está a superfície, e para no teto de 4096 colunas. Parar no teto é
"não achei perto", e o ciclo seguinte tenta de novo.

`TreeHarvester` tem teto de 24 troncos por árvore. Carvalho comum tem
entre quatro e sete; o teto existe para o carvalho gigante e para a casa
de tronco que o jogador tenha encostado numa árvore.

---

### Seis testes de jogo, e os negativos importam mais

```text
fellingTakesTheWholeTrunk        derruba os quatro troncos
fellingReplantsASapling          muda no lugar da base
fellingLeavesTheLeavesAlone      folha fica
fellingIgnoresOtherWoods         bétula fica
theWoodGoesIntoTheChest          madeira entra e é contada
theSearchFindsATreeNearby        a varredura acha
```

Os dois do meio são os que protegem a construção de quem joga.
Verificados por mutação: com o `TreeHarvester` tornado guloso —
aceitando folha e bétula — os dois falham, e só eles.

---

### O limite conhecido

O movimento. O cérebro Vanilla do aldeão tem agenda própria e pode
sobrescrever o destino que `LumberjackWork` pede. O caminho correto é
uma task no `Brain`, que é mudança maior e mexe no comportamento dele
fora do trabalho.

Hoje o pedido é repetido a cada ciclo, e a derrubada só acontece a três
blocos da árvore. Se o aldeão não chegar, a linha `felled` não aparece —
é o que uma sessão de jogo vai dizer, e é a única parte da Fase 8 que os
testes não conseguem responder.

Está em §10 como decisão pendente.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

a derrubada nunca rodou em jogo
```

---

## 2026-08-08 — A Fase 8 quebrou o jogo, e o diagnóstico ficou incompleto

Primeira sessão com a Fase 8 instalada: o terreno quase não carregou e
os aldeões ficaram parados. O jogo foi fechado antes de eu poder tirar
um thread dump, e o `latest.log` parou às 03:32:23 com o buffer não
descarregado — a mesma assinatura do travamento de 2026-08-07.

```text
[03:32:23] Storage claimed by e8f56d2b ...
[03:32:23] Storage claimed by a60c4f43 ...
            (nada mais)
```

**Não sei qual dos três defeitos abaixo causou o quê.** O que se sabe é
que os três existiam, e que os três são do tipo que já travou a thread
antes.

---

### O que estava errado

```text
1  TreeHarvester lia e escrevia com world.getBlockState

   O mesmo erro de 2026-08-07, repetido no primeiro
   código que escreve no mundo. Um tronco na borda de
   chunk faz o vizinho cair em chunk descarregado, e a
   leitura o carrega à força — gerando terreno dentro do
   laço, na thread do servidor.

   Agora todo acesso passa por getWorldChunk com nulo
   checado, e o que não está carregado é pulado.

2  findVillager varria uma caixa de 128 blocos de lado

   Por tarefa, por ciclo. O servidor já indexa entidade
   por UUID: world.getEntity(uuid) responde direto.

3  a espiral iterava o miolo para descartá-lo

   Para olhar quatro mil colunas, o laço percorria mais
   de um milhão de posições. O salto agora pula o miolo,
   e o teto caiu de 4096 para 1024 colunas.
```

---

### O que isto custa admitir

Os doze testes de jogo passaram antes, durante e depois. Nenhum deles
mede tempo nem toca em chunk descarregado: a estrutura do gametest é
pequena e está inteiramente carregada.

```text
o gametest cobre comportamento, não custo

o gametest cobre o mundo montado, não o mundo real
```

É o mesmo limite que o §11 registra para o teste de unidade, um nível
acima. Uma bancada que não pode falhar por lentidão não protege contra
lentidão.

---

### O que fica em aberto

A correção não foi verificada em jogo, e o diagnóstico não foi
confirmado — foi deduzido do código. Se o travamento voltar, o caminho
é o mesmo que funcionou antes: manter o jogo aberto e tirar um thread
dump com `jstack`, que aponta a linha exata.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

a Fase 8 travou o jogo uma vez; a correção não foi vista
```

---

## 2026-08-08 — A correção do travamento funcionou; nome sobre a cabeça

Sessão de dois minutos com a Fase 8 corrigida:

```text
[03:38:44] assigned 1 tasks (0 open)
[03:39:14] assigned 1 tasks (0 open)
[03:39:44] assigned 1 tasks (0 open)
[03:40:12] Saved 3 colonies with 45 workers
```

Ciclos a cada trinta segundos, mundo salvo, saída limpa. O travamento
não voltou, e as três correções ficam confirmadas — sem saber, ainda,
qual das três era a causa.

Nenhuma linha `felled`. O lenhador não chegou à árvore, que é o limite
registrado em §10: o cérebro Vanilla tem agenda própria e sobrescreve o
destino pedido.

---

### Identificar o trabalhador em jogo

Pedido do autor: pôr uma skin no lenhador para reconhecê-lo. A skin
enviada era de jogador, no layout 64×64 de player, e aldeão usa modelo e
UV próprios — aplicá-la daria textura embaralhada.

Mais de fundo: a profissão é nossa e existe só no servidor. Textura por
profissão exigiria mixin de renderização no cliente, sincronização por
rede e ADR nova, já que a ADR-004 permite um mixin só. O mod deixaria de
funcionar em cliente Vanilla.

Escolha do autor entre três: nome sobre a cabeça.

```text
LUMBERJACK    → Lenhador
MANUFACTURER  → Fabricante
FARMER        → Fazendeiro
BUILDER       → Construtor
```

`WorkerNameplate` nunca sobrescreve nome existente: aldeão batizado com
etiqueta pelo jogador continua com o nome dele. Trabalhador sem
profissão fica sem nome — bebê e nitwit são o caso comum, e chamá-los de
trabalhador diria algo falso.

Texto literal e não chave de tradução: o mod roda no servidor e o
cliente pode ser Vanilla puro, que mostraria a chave crua.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo; build verde

travamento da Fase 8: corrigido e confirmado em jogo
derrubada de árvore: nunca aconteceu em jogo
nomes: escritos, não vistos
```

---

## 2026-08-08 — Os nomes apareceram; o que está pronto e o que falta

```text
[03:49:16] Named 1 workers in colony 0c2771b0
[03:49:16] Named 7 workers in colony 0c2771b0
[03:49:44] Named 7 workers in colony 9a5afa23
[03:49:44] Named 23 workers in colony 0c2771b0
```

Quarenta e quatro trabalhadores nomeados, confirmados em jogo pelo
autor. Vêm em lotes porque a nomeação acompanha a detecção, que enxerga
a vila por partes.

---

### O que está pronto e verificado em jogo

```text
detecção de vila, identidade e persistência
registro de aldeões como trabalhadores
atribuição de profissão, com rodízio
descoberta do baú da casa, com regra de nível e de parede
contagem de estoque, com aviso de leitura parcial
encolhimento da colônia por sonda repetida
ciclo de simulação: déficit vira tarefa
distribuição de tarefa por capacidade
nome da profissão sobre a cabeça
```

---

### O que está escrito e nunca aconteceu em jogo

```text
nada da Fase 8
```

A Fase 8 fechou em 2026-08-08 às 05:32:44, com a linha
`Worker e8f56d2b felled 6 logs at 1120, 64, 669`. A tarefa nasce, é
reservada, o aldeão anda até a árvore, derruba, e a madeira entra no
baú — tudo visto em jogo, não só em teste.

A Fase 8 está coberta por dez testes de jogo, e quatro deles existem
para provar o que o lenhador **não** quebra.

---

### O que falta, em ordem

```text
1  meta de estoque real              hoje é constante; sai da Fase 9

2  gametest para o que falta         morte, zumbificação, encolhimento,
                                     e o ciclo gerando tarefa

3  consolidar este documento         passou de 4200 linhas
```

O item 2 tem uma ressalva que a Fase 8 deixou clara: gametest cobre
comportamento, não custo. O travamento que quebrou o jogo do autor
passou por doze testes verdes.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

uma única coisa escrita e não vista em jogo: a derrubada
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

---

## 2026-08-08 — O lenhador derrubou; a colheita ganhou ordem

```text
[05:32:14] Worker e8f56d2b heading to the tree at 1120, 64, 669
           — 12 blocks away, work time: yes, path held: no, doing: idle
[05:32:44] Worker e8f56d2b felled 6 logs at 1120, 64, 669
```

Trinta segundos entre uma linha e outra: um ciclo. O bloqueio da Fase 8
caiu.

O caminho até aqui passou por três coisas, e só a primeira era a
prevista:

A task no Brain. `startMovingTo` era um pedido na língua errada; quem
manda no caminho do aldeão é a memória `WALK_TARGET`, e as tasks Vanilla
de movimento só começam com ela vazia.

O travamento ao carregar o mapa, que não tinha nada a ver com o Brain — o
jar que travou era anterior a ele. A detecção rodava inteira dentro do
evento de chunk carregado, uma vez por chunk, e uma vila de trinta camas
ocupa dezenas deles. Ao abrir o mundo, centenas chegam no mesmo tick.
Agora o gatilho enfileira e o tick drena uma varredura.

E duas rodadas de instrumentação, porque as duas primeiras sessões não
souberam dizer o que tinha acontecido. A primeira linha só falava quando
a árvore mudava, e deu uma linha e sete ciclos de silêncio. A segunda
fala a cada ciclo e diz distância, horário de trabalho, se a memória de
caminho sobreviveu e que Activity o Brain escolheu — e foi ela que
mostrou `work time: yes` e a chegada.

Detalhe que a linha revelou e que vale guardar: `path held: no`. No
instante da leitura o Vanilla já tinha descartado o `WALK_TARGET`, e o
aldeão chegou mesmo assim. A task repõe no tick seguinte, e é a
reposição — não a primeira escrita — que faz o caminho acontecer.

---

Com a derrubada em jogo, o autor fechou a regra da colheita. Ver §10.

O que mudou de comportamento: a árvore que não cabe no baú fica de pé,
em vez de virar madeira destruída; o tronco cortado no teto de 24 não
replanta, e a muda espera o último tronco cair; e a coluna acima da muda
é aberta de folha, senão ela nunca vira árvore.

A quarta regra abre exceção em "folha não é alvo". O teste antigo dessa
regra punha a folha exatamente na coluna da muda — ou seja, ele
guardava as duas coisas ao mesmo tempo e agora tinha de escolher. A
folha passou para o lado do tronco, que é o caso que a regra sempre quis
proteger: construção do jogador feita de folha.

---

Estado ao registrar:

```text
257 testes de unidade + 16 de jogo; build verde

Fase 8: fechada e verificada em jogo
travamento ao carregar: corrigido e confirmado em jogo
regra da colheita: escrita e coberta por teste, não vista em jogo
```

---

## 2026-08-08 — Todas as árvores, e a copa junto

Pedido do autor, com três decisões tomadas na hora:

```text
folhas       quebrar as folhas da árvore derrubada e
             recolher o que elas dropam

contagem     tipo próprio por madeira, somando numa
             categoria para efeito de meta

alcance      Overworld inteiro; Nether e bambu ficam na
             tabela, prontos para ligar
```

O centro da mudança é `TreeSpecies`: uma linha por árvore, e cada linha
diz tronco, folha, muda e recurso. Acrescentar uma árvore é acrescentar
uma linha — e o `MinecraftTypeAdapter` lê a mesma tabela, então a
contagem no estoque passa a existir sozinha.

Não é a tag `minecraft:logs` de propósito. A tag inclui tronco
descascado e bloco de madeira, que são material de construção do
jogador, não árvore, e não diz que muda replanta o quê.

Nether e bambu ficaram de fora com motivo, não por esquecimento: caule
carmesim e distorcido não têm muda, têm fungo, e o fungo só vira árvore
em nylium e com farinha de osso. Bambu não tem muda nenhuma — cresce da
própria base, e derrubá-lo inteiro impede que se reponha. Os dois pedem
um campo a mais na tabela, e ele entra quando houver colônia num bioma
que os tenha.

---

O grupo de recursos é a parte que muda o comportamento da colônia, e
não só do lenhador. `ResourceGroup.WOOD` faz o déficit somar as oito
madeiras: quem tem o baú cheio de bétula deixa de mandar buscar
carvalho. O estoque continua sabendo o tipo de cada tronco — é o
déficit que soma.

Isso resolve metade do que a sessão de 05:26 mostrou: a tarefa que
nascia a cada ciclo, para sempre. A outra metade é a meta ser constante,
que continua sendo a Fase 9.

---

A colheita agora devolve `Harvest` em vez de um número: quantos troncos,
quantas folhas, e os itens já somados por tipo. Os drops saem da mesma
tabela de loot que o jogo consultaria — é dela que vêm a muda de vez em
quando, a maçã do carvalho e o graveto. Repetir essas probabilidades no
mod seria inventar uma segunda verdade sobre o que uma árvore dá.

A copa é achada a partir dos troncos, nunca de um raio, e para a seis
blocos de qualquer tronco. Sem esse limite, copas encostadas ligariam
uma árvore à vizinha e derrubar uma levaria a copa de meia floresta.

O teste antigo "folha não é alvo" tinha de mudar de novo, e desta vez de
premissa: a copa agora vem junto. O que sobrou dele são três testes com
o que a regra ainda protege — folha longe do tronco, folha de outra
espécie, e o teto que não é folha.

---

Estado ao registrar:

```text
260 testes de unidade + 20 de jogo; build verde

Fase 8: fechada e verificada em jogo
todas as árvores, copa e drops: escritos e cobertos por teste,
não vistos em jogo
```
