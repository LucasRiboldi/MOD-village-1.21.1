---
name: minecraft-villager-systems
description: >-
  Domínio dos aldeões do Minecraft Java Edition — Brain, Memory, Sensor,
  Activity, Behavior/Task, POI, profissão, local de trabalho, Schedule, trades,
  gossip, reprodução, pathfinding, vila e raids. Use sempre que a tarefa tocar
  aldeões, vilas ou o sistema de IA por Brain: criar profissão, criar local de
  trabalho, mudar o trabalho do aldeão, fazer aldeão coletar recurso, fugir de
  inimigo, trabalhar em máquina, alterar trades, mexer em reprodução ou gossip,
  depurar aldeão parado, ou investigar lag de vila grande. Dispare também em
  pedidos que não citam "aldeão" mas caem aqui — "quero um NPC que trabalhe",
  "mineiro automático", "sistema de colônia", "POI", "job site", "villager".
  Trabalha com minecraft-code-research (investiga) e fabric-development
  (implementa). Vale em português ou inglês.
---

# Minecraft Villager Systems

## O aldeão não é uma entidade com funções

Este é o erro que produz toda arquitetura frágil de mod de aldeão:

```text
✗  Villager = Entity + alguns métodos
```

O modelo real:

```text
                        VILLAGER
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
       DATA              BRAIN             WORLD
         │                 │                 │
    Type                  │            ┌─────┴─────┐
    Profession    ┌───────┼───────┐    │           │
    Level         │       │       │   POI       VILLAGE
    Experience  MEMORY ACTIVITY SENSOR │           │
    Age           │       │       │  Job Site   Gossip
                  └───────┼───────┘  Bed        Reputation
                          │          Meeting    Population
                        TASKS                   Breeding
                          │                     Threats
                       SCHEDULE
```

**Cada camada é modificável separadamente.** A habilidade central desta skill é
descobrir **qual camada mexer** — e não mexer nas outras.

Quase todo pedido de "muda o comportamento do aldeão" é resolvido acrescentando
uma peça numa camada, não reescrevendo o sistema.

## As três skills

```text
minecraft-code-research   →   minecraft-villager-systems   →   fabric-development
   "como isso funciona?"        "onde isso se encaixa?"          "como implementar?"
```

Esta skill responde **onde encaixar**. Ela não repete a pesquisa (a primeira faz
isso) nem a implementação (a terceira). Quando faltar fato sobre o Vanilla, ela
**para e aciona a pesquisa** em vez de inventar API.

## Antes de qualquer coisa: as 20 perguntas

Antes de implementar qualquer sistema de aldeão, responda mentalmente. Cada
"não sei" é um gatilho para `minecraft-code-research`.

```text
 1. O que o aldeão SABE?
 2. COMO ele sabe?              (qual sensor)
 3. ONDE isso é armazenado?     (qual memória)
 4. Quem DECIDE?                (Brain + Activity)
 5. Quem EXECUTA?               (qual task)
 6. Qual ACTIVITY está ativa?
 7. Qual TASK executa?
 8. Qual SENSOR fornece a percepção?
 9. Qual POI está envolvido?
10. Qual PROFISSÃO está envolvida?
11. Qual SCHEDULE controla o momento?
12. Qual estado precisa PERSISTIR?
13. O que acontece se FALHAR?
14. O que acontece com VÁRIOS aldeões?
15. Qual o custo de PERFORMANCE?
16. Qual parte é do VANILLA?
17. Qual parte é do NOSSO mod?
18. Precisamos MESMO de Mixin?
19. Qual é a MENOR implementação correta?
20. QUEM É O DONO deste estado, e o que o limpa quando o dono some?
```

As perguntas 1–5 são a espinha. Se você não sabe responder qual **memória**
representa o estado, provavelmente está prestes a guardar estado de IA num
`static Map` — e isso é o anti-padrão número um deste domínio.

**A 20 é a que ninguém faz, e ela tem nome: *estado que sobrevive ao dono*.**
Ferramenta que sobrevive à profissão, destino que sobrevive à tarefa, reserva
que sobrevive ao reservante, trabalhador que sobrevive ao aldeão. O erro nunca
é esquecer de limpar — é **pendurar a limpeza num momento** em vez de conferir
uma invariante. Momento falha: o aldeão está em chunk descarregado, o evento
não dispara, a tarefa termina bem e não passa pela desistência.

Some por mais caminhos do que se supõe — e dois deles estão nas regras deste
arquivo, logo abaixo: **zumbificação não é morte** e **ausência não é morte**.
Para cada estado que você criar, escreva a lista de caminhos pelos quais o
dono some, e o que limpa em cada um. Se a lista tiver um caminho sem limpeza,
prefira a invariante conferida a cada passagem — é o que
`references/multi-villager-systems.md` já manda fazer com reserva, e vale
igual para estado por aldeão.

## O fluxo da decisão

```text
MUNDO → SENSOR → MEMORY → ACTIVITY → TASK → AÇÃO
```

Cada peça tem uma responsabilidade e **não invade a vizinha**:

| Peça | Faz | Nunca faz |
|---|---|---|
| **Sensor** | percebe e escreve memória | executa ação de gameplay |
| **Memory** | guarda conhecimento com validade | serve de banco de dados permanente |
| **Activity** | define o modo atual | é escolhida por você — quem escolhe é a `Schedule` |
| **Task** | age | faz varredura enorme de mundo |
| **Schedule** | mapeia horário → Activity | é substituída por `if (getTimeOfDay())` |

Sensor que age e task que percebe são os dois anti-padrões que mais aparecem.

## Fatos verificados — MC 1.21.1

Confirmados por `javap` sobre `minecraft-merged` 1.21.1 / yarn `1.21.1+build.3`.
**Verifique na sua versão antes de usar** — o método está em
`references/vanilla-extension-points.md`.

**`VillagerProfession` é um `record`:**

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

> **Não há trades no record.** Profissão e comércio são sistemas separados —
> mudar um não exige tocar no outro. É a confusão mais comum deste domínio.

**`PointOfInterestType` é um `record`:**

```java
PointOfInterestType(Set<BlockState> blockStates, int ticketCount, int searchDistance)
```

`ticketCount` = quantos aldeões podem reivindicar. `searchDistance` = de que
distância podem reivindicar.

**Memórias-chave** — repare no tipo:

```java
MemoryModuleType<GlobalPos>  HOME · JOB_SITE · POTENTIAL_JOB_SITE · MEETING_POINT
MemoryModuleType<List<GlobalPos>>  SECONDARY_JOB_SITE
MemoryModuleType<WalkTarget>  WALK_TARGET
MemoryModuleType<LookTarget>  LOOK_TARGET
MemoryModuleType<BlockPos>    NEAREST_BED
MemoryModuleType<Long>        CANT_REACH_WALK_TARGET_SINCE
```

> `JOB_SITE` e `HOME` são **`GlobalPos`** (dimensão + posição), não `BlockPos`.
> Tratar como `BlockPos` compila em alguns caminhos e erra entre dimensões.

**Activities:** `CORE · IDLE · WORK · PLAY · REST · MEET · PANIC · PRE_RAID ·
RAID · HIDE · FIGHT · CELEBRATE · AVOID · ADMIRE_ITEM · RIDE`

**Schedules:** `EMPTY · SIMPLE · VILLAGER_BABY · VILLAGER_DEFAULT`, com a
constante `WORK_TIME`.

## Regras do domínio

**Quem escolhe a Activity é a `Schedule`.** Uma Activity que a Schedule não
conhece **nunca é escolhida**. Registrar uma Activity nova quase nunca é o
caminho — ver `references/brain-system.md`.

**`setTaskList` acrescenta, não substitui.** Nenhuma task Vanilla é removida.
Você não precisa remover nada.

**Quem manda no caminho é `WALK_TARGET`.** `getNavigation().startMovingTo` é
sobrescrito pelo cérebro no mesmo tick.

**Zumbificação não é morte.** Passa por `MOB_CONVERSION`, não por `AFTER_DEATH` —
e é o caso mais comum de perder um aldeão.

**Ausência não é morte.** Aldeão fora do raio ou em chunk descarregado não morreu;
só não foi visto.

**O aldeão não existe sozinho.** Toda feature precisa dizer se é individual, por
POI, por vila, por chunk ou por mundo.

## A pergunta que evita metade dos erros

Antes de criar uma profissão, pergunte:

> **Isto precisa mesmo ser uma profissão, ou é só uma nova capacidade
> comportamental?**

Profissão traz junto: POI, local de trabalho, reivindicação, Schedule, trades,
níveis, XP, som, resources. Um aldeão que foge de um inimigo específico não
precisa de nada disso — precisa de **um sensor, uma memória e uma task**.

Criar profissão quando bastava comportamento é o erro mais caro deste domínio,
porque ele só aparece depois de todo o aparato estar escrito.

Ver `examples/guard-villager-decision.md`.

## Roteamento

### Workflows — o passo a passo

| Tarefa | Workflow |
|---|---|
| Entender por que o aldeão faz o que faz | `workflows/analyze-villager-behavior.md` |
| Novo comportamento (task) | `workflows/add-behavior.md` |
| Nova memória ou sensor | `workflows/add-memory-or-sensor.md` |
| Nova Activity (leia antes de decidir) | `workflows/add-activity.md` |
| Nova profissão | `workflows/add-profession.md` |
| Novo local de trabalho / POI | `workflows/add-job-site.md` |
| Alterar comportamento Vanilla (trabalho, trades, schedule, gossip, reprodução) | `workflows/modify-vanilla-behavior.md` |
| Aldeão parado, indo ao lugar errado, ignorando ordem | `workflows/villager-debugging.md` |
| **Auditar estado que sobrevive ao dono** (pergunta 20) | `references/villager-lifecycle.md` para os caminhos de perda de dono, e `references/multi-villager-systems.md` para a forma da invariante |
| Lag com vila grande | `workflows/villager-performance.md` |
| Garantir que nada quebrou | `workflows/villager-regression.md` |

### Referências — o domínio

| Assunto | Leia |
|---|---|
| O modelo mental completo, as camadas | `references/villager-architecture.md` |
| Nascer, tickar, morrer, converter, bebê | `references/villager-lifecycle.md` |
| Brain, memória, sensor, activity, task, schedule | `references/brain-system.md` |
| POI e locais de trabalho, reivindicação | `references/poi-and-job-sites.md` |
| Profissões, níveis, XP | `references/professions.md` |
| Trabalho, descanso, encontro, horário | `references/work-and-schedules.md` |
| Comércio | `references/trading.md` |
| Gossip e reputação | `references/gossip-and-reputation.md` |
| Reprodução e comida | `references/breeding-and-food.md` |
| Vila, população, raids, ameaça | `references/village-and-raids.md` |
| Caminho, navegação, custo | `references/pathfinding-and-movement.md` |
| Dados do aldeão, NBT, o que persiste | `references/villager-data.md` |
| Coleta de recursos, biomas, máquina de estados | `references/resource-gathering.md` |
| Vários aldeões, estado compartilhado, conflito | `references/multi-villager-systems.md` |
| Onde encaixar sem quebrar o Vanilla | `references/vanilla-extension-points.md` |
| Custo com muitos aldeões | `references/villager-performance.md` |
| Diagnosticar IA | `references/villager-debugging.md` |
| O que não fazer | `references/anti-patterns.md` |
| Projetos de referência e `PesquisaFabricMOD/` | `references/reference-projects.md` |

### Templates

| Vou produzir | Use |
|---|---|
| Plano de feature de aldeão | `templates/villager-feature-plan.md` |
| Plano de comportamento (task) | `templates/behavior-plan.md` |
| Plano de memória | `templates/memory-plan.md` |
| Plano de sensor | `templates/sensor-plan.md` |
| Plano de Activity | `templates/activity-plan.md` |
| Plano de profissão | `templates/profession-plan.md` |
| Plano de local de trabalho | `templates/job-site-plan.md` |
| Plano de trades | `templates/trade-plan.md` |
| Contrato de sistema de aldeão | `templates/villager-system-contract.md` |
| Relatório de depuração de IA | `templates/ai-debug-report.md` |

### Checklists

| Antes de | Rode |
|---|---|
| Dar uma feature de aldeão por pronta | `checklists/villager-feature.md` |
| Mexer em Brain, memória ou sensor | `checklists/brain-memory-sensor.md` |
| Adicionar comportamento ou Activity | `checklists/behavior-activity.md` |
| Registrar uma profissão | `checklists/profession.md` |
| Registrar um local de trabalho | `checklists/job-site.md` |
| Alterar comércio | `checklists/trading.md` |
| Dizer que o estado sobrevive | `checklists/persistence.md` |
| Dizer que funciona em multiplayer | `checklists/multiplayer.md` |
| Dizer que aguenta uma vila grande | `checklists/performance.md` |

### Exemplos

| Situação | Exemplo |
|---|---|
| Comportamento novo, sem profissão | `examples/custom-behavior.md` |
| Profissão completa | `examples/custom-profession.md` |
| Bloco de trabalho + POI | `examples/custom-job-site.md` |
| "Aldeão mineiro" — coleta de recursos | `examples/resource-gathering-villager.md` |
| **"Isto precisa ser profissão?"** | `examples/guard-villager-decision.md` |
| Sistema completo, ponta a ponta | `examples/complete-villager-system.md` |

## Regra contra os dois excessos

```text
✗  10 abstrações para o que era 1 task + 1 memória
✗  simplificar artificialmente um sistema que é complexo de verdade
```

A pergunta correta:

> **Qual é a menor arquitetura que preserva corretamente o modelo mental do
> Minecraft?**

Preservar o modelo mental importa: uma implementação que guarda estado de IA fora
das memórias "funciona" e depois briga com o Vanilla em cada caso de borda —
salvamento, chunk, morte, conversão, raid.

## Conhecimento vivo

Quando descobrir algo sobre aldeões que não estava documentado — "esta Activity
tem esta prioridade", "este POI é reivindicado assim", "este Behavior depende
desta Memory" — **registre** em `docs/research/` ou na base de conhecimento do
projeto.

Descoberta feita durante implementação custou o bug. Deixá-la só na conversa é
jogá-la fora, e a próxima sessão paga de novo.
