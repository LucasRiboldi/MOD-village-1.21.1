# 12 — Recomendações

---

## 1. A recomendação que vem antes de todas

**Não mude nada agora.**

O seu MVP tem um passo faltando: ver a casa subir (`Project-State.md §7`
e §8, P1). Quatro sessões de jogo foram gastas desentupindo o caminho até
a obra, e bloco nenhum foi visto sendo posto.

Nada nesta análise é mais urgente que isso. Uma refatoração inspirada por
um mod que você não vai copiar, feita antes de o MVP fechar, troca uma
dívida conhecida por uma desconhecida.

**A ordem certa:**

```text
1. a sessão de jogo do P1 — a casa subindo
2. o E11 decidido (TASK-049)
3. só então, os itens desta página
```

O que segue está escrito para quando esse momento chegar.

---

## 2. A arquitetura recomendada

Ela é a **sua**, com quatro acréscimos. O briefing pediu uma proposta
adaptada ao código real, e o código real não pede reorganização: pede
peças que faltam.

```text
core/                                    (Java puro, sem Minecraft)
 │
 ├── type/          ColonyPos, Capability, ResourceId, ResourceType,
 │                  ResourceCategory, ResourceGroup, ToolType
 │                  + [NOVO] ItemRequest            ← §3.2
 │
 ├── colony/        Colony, ColonyState, ColonyLifecycle,
 │                  ColonyService, VillageDetector, ColonyAbandonment
 │
 ├── worker/        Worker, Profession, ProfessionType,
 │                  WorkerService, ProfessionRegistry, ProfessionAssigner
 │                  + [NOVO] ProfessionSpec         ← §3.4
 │
 ├── task/          Task, TaskState, TaskType, TaskPriority, TaskService
 │                  + [NOVO] Task.age               ← §3.3
 │                  + [NOVO] Task.needs             ← §3.2
 │
 ├── construction/  Blueprint, BlueprintBlock, ConstructionProject,
 │                  ConstructionState, Building,
 │                  ConstructionService, BuildingRegistry
 │                  + [NOVO] BlueprintBlock.pairedWith  ← §3.5
 │
 ├── storage/       WorkerStorage, StorageRegistry
 ├── resource/      ResourceTally, ColonyResources, ResourceDemand
 │
 └── coordination/  ColonyCycle, ColonyGoals, WorkAssignment
                    + [NOVO] WorkAssignment: desempate por prontidão ← §3.6
                    + [NOVO] IdleReason (enum)          ← §3.1

fabric/                                  (a fronteira)
 ├── adapter/       MinecraftTypeAdapter   ItemRequest → Item/TagKey
 ├── brain/         ColonyBrainInitializer, GoToWorkTargetTask, WorkHours,
 │                  WorkTargets
 ├── event/         ServerLifecycleHandler, VillageDetectionHandler,
 │                  VillagerLifecycleHandler
 ├── integration/   os 17 scanners e escritores de mundo
 ├── work/          LumberjackWork, ManufacturerWork, BuilderWork,
 │                  ConstructionPlanner
 └── mixin/         VillagerEntityMixin

data/save/          ColonySavedData
```

**Nenhum pacote novo. Nenhuma camada nova. Nenhuma ADR nova** — com uma
exceção possível, o §3.5, e mesmo ela provavelmente não precisa.

---

## 3. Os seis acréscimos, em ordem de retorno

### 3.1 `IdleReason` — o motivo de não trabalhar como valor

**Origem:** os 11 `ERROR_*` do Workers (`09 §A1`).
**Custo:** baixo. **Risco:** nenhum. **Retorno:** o maior da lista.

Hoje `ConstructionPlanner.silent` diz por que a Fase 10 não construiu,
com cinco motivos separados. O lenhador e o fabricante não têm
equivalente.

Proposta: um `enum IdleReason` em `core/coordination`, e cada `*Work`
devolvendo-o em vez de um `boolean` ou de um silêncio:

```text
NO_TASK                 nada aberto para esta capacidade
OUTSIDE_WORK_HOURS      fora da janela WORK da agenda Vanilla
NO_TARGET_FOUND         varredura terminou e não achou alvo
SWEEP_INCOMPLETE        varredura não terminou   ← você já separou isto
NO_STORAGE              sem baú adotado
STORAGE_FULL            baú sem espaço
MISSING_TOOL            falta ferramenta
MISSING_MATERIAL        falta insumo
UNREACHABLE             alvo existe e não se chega
```

Cada um vira uma linha de log distinta. **É o instrumento que o §11 diz
que precisa existir antes de alguém desconfiar do defeito.**

**Por que primeiro:** porque as sessões de jogo do P1 e do E11 vão gerar
perguntas, e este é o único item que torna as respostas legíveis.

### 3.2 `ItemRequest` — o trabalhador declara o que lhe falta

**Origem:** `NeededItem` (`09 §A2`, `06 §2`).
**Custo:** médio. **Risco:** médio. **Retorno:** alto, e crescente.

Hoje um `*Work` sem o que precisa não tem como pedir. Não dói porque
`WorkerEquipment` entrega a ferramenta na atribuição e a colheita não
consome nada. Vai doer quando: a ferramenta quebrar, o construtor precisar
de pedra que não está no baú dele, ou uma profissão nova exigir insumo.

Proposta, **como dado e não como predicado** — a lição do `06 §2.3`:

```java
// core/type/ItemRequest.java
public record ItemRequest(ResourceId what, int amount, boolean blocking) {}
```

`Task` ganha `List<ItemRequest> needs()`. `MinecraftTypeAdapter` traduz
`ResourceId` para `Item` na fronteira, que é o que ele já faz
(`toItem`). O `ChestWithdrawer` já sabe tirar item do baú.

Dois níveis, como no Workers: `blocking = true` impede o trabalho;
`false` é oportunista.

**Onde não copiar:** nada de `Predicate<ItemStack>`, nada de varrer
`BuiltInRegistries.ITEM`. A ADR-005 já resolveu esse problema — o
`ResourceId` existe exatamente para isso.

### 3.3 `Task.age` — envelhecimento contra inanição

**Origem:** `AbstractWorkAreaEntity.time` (`09 §A3`).
**Custo:** trivial — um `int` e um incremento por ciclo.

`WorkAssignment` casa por `TaskPriority`, que é fixa. Duas tarefas de
mesma prioridade têm ordem arbitrária **e estável**: a mesma sempre
ganha. Um contador de ciclos desde a criação, somado ao desempate, faz a
tarefa esquecida subir sozinha.

Isso também é meia resposta ao E11: hoje a vaga gira porque a colônia
reavalia do zero a cada ciclo. Um critério que **acumula** dá memória ao
sistema sem persistir nada.

### 3.4 `ProfessionSpec` — parâmetros de profissão como dado

**Origem:** a ausência disso no Workers (`07 §5`).
**Custo:** baixo. **Risco:** nenhum.

Hoje `SEARCH_RADIUS = 64` mora dentro de `LumberjackWork`, e cada `*Work`
tem as suas constantes. Isso está **certo** enquanto são três profissões;
vira problema na quinta.

Proposta: um record no Core com raio de busca, teto por tarefa,
ferramenta exigida e capacidade, registrado no `ProfessionRegistry`.
`ProfessionAssigner` e `WorkAssignment` leem de lá.

Não é urgente. É o que impede que a extensibilidade que você já tem se
perca por acúmulo de constantes espalhadas.

### 3.5 Blocos de duas partes — possivelmente sem a TASK-046

**Origem:** `BuilderWorkGoal` + `BuildArea.findPairedMultiBlockPos`
(`09 §B4`).
**Custo:** médio. **Risco:** médio. **Retorno:** fecha o E8.

Este é o achado mais concreto da análise para o seu backlog.

O seu §17 registra o E8 assim: o `Blueprint` guarda o nome do bloco e
descarta o estado; porta e cama viram duas metades independentes. E a
TASK-046 diz que a correção **exige decisão de arquitetura** — levar
`BlockState` ao Core contra a ADR-005, ou inventar uma linguagem de
propriedades lá dentro.

**O Workers resolve sem nenhuma das duas.** A solução dele tem duas
partes:

1. **Colocar por último.** `stackToPlaceMultiBlock` é uma pilha separada,
   consumida depois que todo o resto está de pé. A metade de cima da porta
   nunca é posta antes de haver parede.
2. **Colocar o par junto.** `findPairedMultiBlockPos(primaryPos)` acha a
   outra metade **no projeto**, e as duas vão ao mundo na mesma operação —
   deixando o próprio `Block.setPlacedBy`/`onPlace` do Vanilla resolver a
   propriedade que as liga.

O que o Core precisa saber é apenas: *este `BlueprintBlock` é de duas
partes, e o par dele é aquele*. Isso é **um booleano e uma referência de
posição** — dado puro, dentro da ADR-005. Nenhum `BlockState` atravessa a
fronteira.

**Ressalva honesta:** não verifiquei se o `setBlockState` da 1.21.1
resolve a ligação sozinho quando as duas metades são escritas na ordem
certa, nem se `StructureBlueprintReader` já preserva a informação de par.
Isso precisa de leitura do seu código e de um gametest. **É uma pista
forte, não uma solução pronta.** Mas se ela se confirmar, a TASK-046 deixa
de precisar de ADR e vira implementação.

### 3.6 Desempate por prontidão em `WorkAssignment`

**Origem:** `isWorkerPerfectCandidate` (`09 §A4`).
**Custo:** baixo.

Hoje `Capability` é binária: pode ou não pode. Com desempate por
distância ao alvo, ferramenta em mão e espaço no baú, "pode fazer" vira "é
o melhor para fazer". Não muda estrutura nenhuma — muda o comparador.

---

## 4. Cinco verificações que valem uma hora cada

Não são funcionalidades. São defeitos possíveis, encontrados por analogia,
que só aparecem em save real.

```text
V1  baú duplo contado duas vezes?
    ChestScanner / ChestInventoryReader.
    O Vanilla devolve o mesmo CompoundContainer para as duas metades.
    Contar duas vezes muda o que ResourceDemand acha que falta.
    → 06 §3, 09 §A6

V2  baú com bloco em cima conta como estoque?
    O trabalhador não consegue abri-lo. StorageArea exige
    stateAbove.isAir(). Uma linha.
    → 09 §A7

V3  TreeScanner testa PERSISTENT, ou só conectividade?
    Folha decorativa colocada pelo jogador tem PERSISTENT=true e
    DISTANCE baixa. Só a conectividade não distingue casa de árvore.
    → 09 §B1

V4  tarefa EXECUTING cujo executor não chega — quem a libera?
    A morte libera (VillagerLifecycleHandler). O aldeão vivo que
    ficou preso, ou que o jogador levou embora, não.
    → 09 §A5

V5  BuilderWork coloca em ordem de camada?
    Se coloca em ordem de lista, uma parede pode ser escrita antes
    do que a sustenta.
    → 09 §B3
```

**Todas as cinco são verificações, não mudanças.** Se o código já estiver
certo, o custo foi ler; e vale escrever um teste que trave a resposta.

---

## 5. Ideias para o mod — classificadas

Pedido do §18 do briefing. Classificação segundo o que o **Village
Colony** quer ser (`PROJECT_CONSTITUTION`, `MVP.md`), não segundo o que o
Workers faz.

### MUST HAVE

```text
motivo de ociosidade nomeado        §3.1 — instrumento, não recurso
pedido de item pelo trabalhador     §3.2 — antideadlock
liberação de tarefa órfã            V4
```

### SHOULD HAVE

```text
envelhecimento de tarefa            §3.3
desempate por prontidão             §3.4/§3.6
blocos de duas partes               §3.5 — fecha o E8
qualidade da casa construída        bitmask de HomeArea: a colônia
                                    saber se a casa que ergueu serve
rodízio de profissão resolvido      E11 / TASK-049 — decisão sua
```

### COULD HAVE

```text
status do trabalhador legível       o que ele está fazendo agora,
                                    em vez de só linhas de log
progressão / experiência            o Workers não tem; é ideia sua
estatísticas de produção            quanto a colônia produziu por dia
consumo de alimento pela colônia    fecha o ciclo do fabricante
estrada usada de fato               bônus de custo em bloco de caminho —
                                    exige mixin de navegação, ADR-004
rede logística entre baús           o courier, quando houver armazém
                                    central
casa desenhada pelo jogador         StructureManager, com outra
                                    implementação
```

### NICE TO HAVE

```text
moral                               o Workers tem, e não faz nada
horários por profissão              a agenda Vanilla já basta
GUI de colônia                      P3 é mais urgente
grupos e permissões                 não se aplica: a colônia não tem dono
pagamento / upkeep                  contra a ADR-001 (economia artificial)
```

**As duas últimas são recusas, não adiamentos.** Dono, pagamento e
facções são a espinha do Workers e são **incompatíveis** com o
PROJECT_CONSTITUTION §4. Não entram nunca.

---

## 6. Roadmap

As doze fases do §21 do briefing não se aplicam: elas descrevem construir
um mod do zero, e o seu está na Fase 12. O roadmap real é este.

### Fase 0 — Fechar o MVP (bloqueante)

```text
arquivos     nenhum. É sessão de jogo
depende de   um construtor na vila, /time set noon, pedra e vidro nos baús
risco        as vilas do autor podem não ter lote de verdade
conclusão    "planned ... 151 blocks" e a casa subindo bloco a bloco
```

### Fase 1 — Instrumento (`IdleReason`)

```text
arquivos     core/coordination/IdleReason.java (novo)
             fabric/work/LumberjackWork, ManufacturerWork, BuilderWork
             ConstructionPlanner (generalizar o silent)
depende de   nada
risco        baixo — só acrescenta
conclusão    toda linha "não trabalhou" diz por quê, e um teste de
             unidade cobre a tabela de motivos
```

### Fase 2 — Verificações V1 a V5

```text
arquivos     ChestScanner, ChestInventoryReader, TreeScanner,
             BuilderWork, WorkAssignment
depende de   Fase 1 (o log fica legível antes)
risco        baixo. Pode não haver defeito nenhum
conclusão    cada verificação vira um gametest que trava a resposta,
             rodado contra a regra desligada (§11)
```

### Fase 3 — Envelhecimento e desempate

```text
arquivos     core/task/model/Task.java (+age)
             core/coordination/WorkAssignment.java
depende de   Fase 1
risco        baixo. Muda ordem de atribuição — pode mexer em testes
             que afirmam sobre ordem
conclusão    teste de unidade: a tarefa mais velha de mesma prioridade
             é atendida primeiro
```

### Fase 4 — `ItemRequest`

```text
arquivos     core/type/ItemRequest.java (novo)
             core/task/model/Task.java
             fabric/adapter/MinecraftTypeAdapter
             fabric/work/* e fabric/integration/ChestWithdrawer
depende de   Fases 1 e 3
risco        MÉDIO — toca Task, que é o centro. Fazer depois do MVP,
             nunca antes
conclusão    um trabalhador sem ferramenta pede, recebe e volta a
             trabalhar; coberto por gametest
```

### Fase 5 — Blocos de duas partes (E8)

```text
arquivos     core/construction/model/BlueprintBlock (+ marca de par)
             fabric/integration/StructureBlueprintReader
             fabric/work/BuilderWork
depende de   Fase 0 verificada em jogo — não faz sentido consertar
             a porta de uma casa que nunca subiu
risco        MÉDIO. Se a ligação não resolver sozinha, volta a exigir
             a decisão da TASK-046
conclusão    a porta da casa de planície fica inteira, visto em jogo
```

### Fase 6 — `ProfessionSpec`

```text
arquivos     core/worker/model/ProfessionSpec.java (novo)
             ProfessionRegistry, os três *Work
depende de   Fase 4
risco        baixo
conclusão    nenhuma constante de profissão fora do registro
```

### Fase 7 — Qualidade da casa

```text
arquivos     core/construction/model/Building (+qualidade)
             fabric/integration/ (varredura nova)
depende de   Fase 11 do MVP verificada em jogo
risco        baixo, custo por varredura
conclusão    a colônia sabe se a casa que ergueu é habitável
```

**Ordem:** 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7. As Fases 1, 2 e 3 são baratas e
independentes entre si; a 4 é a única que mexe no centro.

---

## 7. Riscos técnicos

```text
R1  mexer em Task antes do MVP fechar
    Task é o centro do Core, e 366 testes dependem dele. A Fase 4
    entra depois do P1, sem exceção.
    Mitigação: a ordem do §6.

R2  o E8 não se resolver sozinho
    A hipótese do §3.5 não foi verificada. Se a ligação de porta e
    cama não acontecer ao escrever as duas metades em sequência, a
    TASK-046 volta a exigir ADR.
    Mitigação: gametest antes de mexer no Blueprint.

R3  o envelhecimento quebrar testes de ordem
    Alguns dos 366 testes podem afirmar sobre ordem de atribuição.
    Mitigação: rodar a bateria antes de decidir a forma do desempate.

R4  ItemRequest virar um sistema
    O NeededItem do Workers tem 128 linhas e uma varredura de
    registro dentro. É fácil crescer.
    Mitigação: record de três campos, e nada mais até doer.

R5  a análise virar pauta em vez de referência
    São 21 itens catalogados. Nenhum deles é o MVP.
    Mitigação: §1 desta página.

R6  workers-maingit/ ser commitada
    Redistribuição de obra All Rights Reserved num repositório
    público. Hoje a pasta está untracked, e um `git add .` a inclui.
    Mitigação: .gitignore, agora. Ver 11 §4, item 7.
```

---

## 8. O próximo passo recomendado

Nesta ordem, e só esta:

```text
1. .gitignore ganha workers-maingit/
   É a única ação desta análise que não pode esperar. Risco jurídico
   real, custo de uma linha.

2. A sessão de jogo do P1 — a casa subindo.
   Nada nesta pasta é mais importante.

3. Decidir o E11 (TASK-049).

4. Fase 1 do §6: IdleReason.
   É o item de maior retorno, o mais barato, e o que torna todas as
   sessões seguintes mais legíveis.
```

Os itens 2 e 3 já estavam no seu `Project-State.md §8` antes desta
análise. **Ela não mudou a sua prioridade — confirmou-a**, e encheu a
fila do que vem depois.
