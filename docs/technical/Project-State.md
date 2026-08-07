# Project-State.md

# Village Colony — Project State

**Status:** Em implementação — Fases 1 a 3 completas, Fases 4 e 5
escritas e não verificadas em jogo
**Version:** 0.1.0
**Last Update:** 2026-08-07 — TASK-018 e TASK-019; TASK-020 bloqueada
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
Fase 6 — Sistema de Recursos
```

---

## Description

Fases 1 a 3 estão completas e verificadas dentro do jogo: o mod detecta
vilas, cria colônias, mantém sua identidade e persiste tudo entre
sessões.

A Fase 4 está escrita por inteiro — registro, persistência, catálogo de
profissões e atribuição inicial — e **nenhuma parte dela foi verificada
em jogo**. A Fase 5 está fechada sobre ela: baús registrados e
inventário contado.

Sete tarefas e duas correções foram escritas sem passar pelo jogo. É a
maior dívida aberta do projeto; o §7 lista o que verificar e o §11
explica por que ela é cara.

A Fase 6 avançou no que não depende do jogo — a colônia já sabe somar
o que tem e calcular o que falta. A TASK-020, que ligaria isso ao ciclo
da colônia, está bloqueada pelo loop de simulação, que nunca foi
escrito. Ver §10.

---

## Concluído até aqui

```text
Fase 0   decisões de arquitetura        ADR-001 a ADR-006

Fase 1   núcleo da colônia              TASK-001 a TASK-006

Fase 2   persistência                   TASK-007 e TASK-008

Fase 3   detecção da vila               TASK-009 e TASK-010

Fase 4   trabalhadores                  TASK-011 a TASK-014
                                        escrita, não verificada em jogo

Fase 5   armazenamento                  TASK-015 a TASK-017
                                        escrita, não verificada em jogo

Fase 6   recursos                       TASK-018 e TASK-019
                                        lógica pura, coberta por teste
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

Organizar trabalhadores   FEITO, não verificado em jogo

↓

Coletar recursos       não iniciado

↓

Produzir materiais     não iniciado

↓

Construir expansão     não iniciado
```

Três dos seis passos do MVP estão escritos; um deles verificado.

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
  TASK-012b persistir trabalhadores  feito, NÃO verificado em jogo
  TASK-013  ProfessionRegistry       feito, NÃO verificado em jogo
  TASK-014  atribuição inicial       feito, NÃO verificado em jogo

Fase 5 — Armazenamento

  TASK-015  detecção de baú          feito, NÃO verificado em jogo
  TASK-016  StorageRegistry          feito, NÃO verificado em jogo
  TASK-017  ler inventário           feito, NÃO verificado em jogo

Fase 6 — Recursos

  TASK-018  visão agregada           feito (ColonyResources)
  TASK-019  verificação de déficit   feito (ResourceDemand)
  TASK-020  integrar com simulação   BLOQUEADA — ver §10

Fases 7 a 9

  TASK-021 em diante                 não iniciadas
```

As TASK-018 e TASK-019 são lógica pura e estão cobertas por teste.
Não trazem dívida de verificação em jogo: não leem o mundo. O que as
alimenta — `ChestInventoryReader` — é que ainda não rodou lá.

---

## Código existente

```text
core/
  type/ColonyPos
  colony/model/      Colony, ColonyState, ColonyLifecycle, VillageCandidate
  colony/service/    ColonyService, VillageDetector
  worker/model/      Worker, ProfessionType, Profession,
                     Capability, ToolType
  worker/service/    WorkerService, ProfessionRegistry,
                     ProfessionAssigner
  storage/model/     WorkerStorage
  storage/service/   StorageRegistry
  resource/model/    ResourceType, ResourceCategory, ResourceTally,
                     ColonyResources
  resource/service/  ResourceDemand

fabric/
  adapter/           MinecraftTypeAdapter
  event/             ServerLifecycleHandler, VillageDetectionHandler,
                     VillagerLifecycleHandler
  integration/       VillageScanner, VillagerScanner, ChestScanner,
                     ChestInventoryReader

data/
  save/              ColonySavedData
```

Vazios por enquanto: `core/task`, `core/construction`,
`fabric/mixin`, `fabric/brain`.

---

## Testes

```text
184 testes, todos passando
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

## Verificar as Fases 4 e 5 em jogo

Nada das Fases 4 e 5 rodou no jogo real. São sete tarefas e duas
correções empilhadas sobre código de fronteira nunca exercitado, e o
§11 mostra que é exatamente aí que os defeitos desta camada moram.

O autor não pôde testar em 2026-08-07 e pediu para seguir. Esta seção
é o registro do que ficou devendo, para que a sessão de teste, quando
vier, não dependa de reconstruir o contexto de memória.

---

### Método

```text
1  ./gradlew build

2  jar de build/libs/ numa instância Fabric 1.21.1

3  INICIAR O JOGO DO ZERO
```

Trocar o jar com o jogo aberto não testa nada: o Minecraft carrega mods
na inicialização da JVM, e sair ao menu e reentrar reusa o código em
memória. Duas sessões já foram desperdiçadas assim — ver §11.

Mundo de teste: vila plains, com o jogador parado perto tempo bastante
para o ciclo longo rodar mais de uma vez.

---

### V1 — Registro de aldeões (TASK-012)

```text
Registered N villagers in colony ...
```

```text
N bate com os aldeões da vila

não repete a cada ciclo com os mesmos aldeões

bebês entram na conta
```

Nunca verificado, e é a base de tudo o que vem depois.

---

### V2 — Atribuição de profissão (TASK-014)

```text
Assigned N professions in colony ...
```

```text
aparece uma vez, não a cada ciclo

quatro aldeões produzem quatro funções distintas

o primeiro é LUMBERJACK
```

---

### V3 — Persistência (TASK-012b)

```text
Loaded N colonies with M workers
```

```text
M > 0 ao reabrir o mundo

as funções são as mesmas de antes de fechar
```

O mais barato de todos: fechar o mundo e reabrir. Fazer primeiro.

---

### V4 — Registro de baús (TASK-015 e TASK-016)

```text
Registered N storages in colony ...
```

```text
cada aldeão pegou o baú da sua casa, não o do vizinho

dois aldeões do mesmo cômodo não pegaram o mesmo baú

um baú construído depois é encontrado no ciclo seguinte
```

---

### V5 — Contagem de estoque (TASK-017)

```text
Colony ... stores {OAK_LOG=N, ...}
```

```text
o número bate com o que está dentro do baú

conferir ABRINDO o baú, não confiar no log

item fora dos três acompanhados não aparece — é o esperado
```

Atenção especial: este é o primeiro ponto em que um defeito aparece
como valor e não como ausência. Se o V4 tiver associado o baú errado,
o número aqui sairá plausível. O log não vai denunciar.

---

### V6 — Bebê e nitwit não trabalham (correção)

```text
Sem linha de log própria. Verificar pelo comportamento.
```

```text
vila com bebê: o total de "Assigned" é menor que o de aldeões

nitwit (casaco verde) não recebe função

bebê crescido recebe função no ciclo seguinte, sozinho
```

O terceiro caso é o mais demorado e o mais fácil de esquecer: exige
esperar um bebê crescer, ou usar ovo de spawn e crescer com trigo.

---

### V7 — Morte e zumbificação (correção)

```text
Worker ... died — profession freed
Worker ... was converted — profession freed, storage released
```

```text
matar um aldeão com função: a linha aparece

zumbificar um aldeão: a linha aparece com "was converted"

  este é o caminho comum em jogo e o que NÃO passa por morte

depois da perda, o próximo aldeão recebe a função que vagou

o baú do morto pode ser reivindicado por outro
```

A zumbificação exige dificuldade normal ou acima; em fácil o aldeão
morre em vez de converter, e o caso mais importante não seria exercido.

---

### O que fazer com o resultado

Defeito encontrado vira entrada em §15 com a linha de log que o
denunciou, antes de qualquer correção. O §11 existe porque foi assim
que os quatro defeitos anteriores foram entendidos.

---

## Decisão registrada — persistência de trabalhadores

```text
Estender ColonySavedData
```

Decidido em 2026-08-07. A alternativa era um `WorkerSavedData` próprio.

Hoje os trabalhadores são redescobertos a cada sessão a partir dos
aldeões do mundo. Isso basta enquanto só há registro.

Deixa de bastar em TASK-014: profissão atribuída é decisão da colônia,
não existe no mundo Vanilla e sumiria ao fechar o mundo. Cada sessão
redistribuiria funções do zero.

Motivo da escolha:

```text
worker referencia a colônia por colonyId

  dois arquivos permitiriam worker órfão apontando
  para colônia não gravada, sem transação que
  mantivesse os dois em sincronia

um só PersistentState

  um segundo arquivo dobraria o custo de
  versionamento futuro sem ganho no MVP

ServerLifecycleHandler já tem os pontos de start/stop

  não precisa de um segundo par
```

Registrada como `TASK-012b` em `MVP-Tasks.md`.

---

# 8. Priority Queue

```text
1   verificar as Fases 4 e 5 em jogo — ver §7

2   decidir o loop de simulação — desbloqueia TASK-020 (§10)

3   Fase 7 — Sistema de Tarefas (TASK-021+)
```

A verificação foi adiada a pedido do autor em 2026-08-07, e o código
seguiu sem ela. A dívida cresceu de uma tarefa para sete: quando um
defeito de fronteira aparecer, ele estará em algum ponto de
`VillagerScanner`, `ChestScanner`, `ChestInventoryReader`,
`ColonySavedData` ou `VillageDetectionHandler`, sem o log intermediário
que teria dito qual.

A contagem da TASK-017 depende do registro da TASK-015 estar certo. Se
o `ChestScanner` associar o baú do vizinho, o total sairá plausível e
errado — nada no log vai denunciar, porque o número existe e é um
número. É o primeiro ponto do projeto em que um defeito deixa de
aparecer como ausência e passa a aparecer como valor.

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
1  TASK-020 está bloqueada pelo loop de simulação

   "A Colônia deve saber o que possui e o que falta."

   Ela já pode: ColonyResources responde o primeiro,
   ResourceDemand o segundo. Falta onde perguntar.

   O loop de ADR-002 e Simulation-Loop.md nunca foi
   escrito — §9 registra isso desde a Fase 4. Não há
   ciclo de colônia em que encaixar a consulta, e o
   VillageDetectionHandler é detecção, não simulação:
   pendurar a decisão de recursos nele faria a colônia
   pensar só quando alguém passasse perto.

   Falta também a meta de estoque. Resource-System.md
   §"Necessidade de Recursos" fala em "metas mínimas"
   e dá um exemplo, mas nada define de onde elas vêm.
   No MVP elas provavelmente saem do que a expansão
   pretende construir — que é a Fase 9.

   Decisão do autor: escrever o loop antes da TASK-020,
   ou fixar metas constantes e ligar ao ciclo de detecção
   como paliativo.
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

# 16. Definition of Project Progress

O projeto avança somente quando:

* código funciona;
* testes passam;
* documentação acompanha.

---

# Final State Rule

O Project-State deve sempre responder:

> "Se um desenvolvedor abrir este projeto hoje, ele sabe exatamente onde estamos e qual é o próximo passo?"
