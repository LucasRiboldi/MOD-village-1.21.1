# 09 — Padrões reutilizáveis

Classificação pedida no §10 e a matriz do §17 do briefing.

**Regra que vale para toda esta página:** o Workers é *All Rights
Reserved* (ver `11-license-analysis.md`). Nenhuma linha entra. Tudo aqui é
**conceito**, e "Reimplementar" significa escrever do zero a partir da
descrição, não traduzir o arquivo.

---

## A — Altamente reutilizável

Conceitos que melhoram o Village Colony diretamente, com pouco atrito
arquitetural.

### A1 — Motivos de falha nomeados

**Onde:** `DepositItemsToStorage` e `GetNeededItemsFromStorage`, 11
estados `ERROR_*`.

Cada modo de não-trabalho tem um nome, e o nome vira mensagem. Não existe
"não deu certo".

**Por quê para você:** o E14 custou três sessões de jogo porque a Fase 10
dizia "planned no building" sem dizer qual dos cinco motivos. Você
consertou no ponto (`ConstructionPlanner.silent`); falta generalizar para
lenhador e fabricante. **É o item de maior retorno da análise inteira.**

### A2 — Pedido de item declarado, em vez de falha

**Onde:** `NeededItem` + `needsToGetItems()` + `GetNeededItemsFromStorage`.

O trabalhador que descobre que lhe falta algo não trava e não desiste:
registra a falta, e outro mecanismo a atende.

**Por quê para você:** seu `Task` não tem esse eixo. Hoje não dói porque
`WorkerEquipment` entrega a ferramenta na atribuição, e a colheita não
consome nada. Dói no primeiro dia em que a ferramenta quebrar, ou em que
uma tarefa exigir material que a colônia não tem no baú certo.

**Adaptação obrigatória:** `ResourceId`/`ResourceType`, nunca
`Predicate<ItemStack>`. Ver `06 §2.3`.

### A3 — Envelhecimento na escolha de alvo

**Onde:** `AbstractWorkAreaEntity.time` + `priority += area.time`.

O alvo não atendido ganha prioridade com o tempo. Impede inanição sem
escalonador.

**Por quê para você:** `WorkAssignment` casa trabalhador com tarefa por
`TaskPriority`, que é fixa. Duas tarefas de mesma prioridade têm ordem
arbitrária e estável — a segunda pode nunca ser atendida. Um contador de
ciclos-desde-a-criação em `Task` resolve, e é um `int`.

### A4 — Pontuação por prontidão

**Onde:** `isWorkerPerfectCandidate(worker)`, vale 10 dos ~14 pontos.

O trabalhador que **já carrega** o que a tarefa exige é preferido para
ela.

**Por quê para você:** hoje `WorkAssignment` casa por `Capability`, que é
binária. Com equipamento, distância ao alvo e baú com espaço, "pode fazer"
vira "é o melhor para fazer" — sem mudar a estrutura, só o critério de
desempate.

### A5 — Watchdog de reserva órfã

**Onde:** `AbstractWorkerEntity:119-123` — se o trabalhador está a >31
blocos da área que reservou, a reserva cai.

**Por quê para você:** `Task.release()` cobre a morte
(`VillagerLifecycleHandler`). Não cobre o aldeão **vivo** que foi embora,
foi levado, ou está preso. Uma tarefa `EXECUTING` cujo executor não chega
nunca é uma tarefa perdida para sempre.

### A6 — Baú duplo contado uma vez

**Onde:** `isSameContainer` / `isAlreadyMapped`
(`AbstractWorkAreaEntity:253-267`), comparando as duas ordens do par de
`CompoundContainer`.

**Por quê para você:** contagem dobrada de estoque muda o que
`ResourceDemand` acha que falta. É invisível em gametest com baú simples e
aparece no primeiro save real com baú duplo. **Verificar
`ChestScanner`/`ChestInventoryReader`.**

### A7 — Baú inacessível não é estoque

**Onde:** `scanStorageBlocks()` exige `stateAbove.isAir()`.

**Por quê para você:** o mesmo motivo. E é uma linha.

### A8 — Orçamento explícito em todo laço sobre o mundo

**Onde:** `MAX_CHESTS = 16`, `FOOD_BUDGET = 4`, `OPEN_TIME = 16`,
`WATER_CROSS_MAX_PROBE = 4`, `PREFETCH_MATERIAL_BUDGET = 4`.

**Por quê para você:** o `Performance-Rules.md` diz o princípio; o Workers
o escreve como constante nomeada ao lado do laço. Seu
`BuildSiteScanner.sweepPausedAt` já é isso. Generalizar.

---

## B — Adaptável

Boas ideias que precisam de remodelagem para caber na sua arquitetura.

### B1 — Validação de árvore por folha viva com duas propriedades

**Onde:** `hasNaturalLeavesConnected` — `!PERSISTENT` **e** `DISTANCE < 7`.

Você já tem a regra ("tronco sem copa viva é construção", 2026-08-12). O
que vale conferir é se o seu `TreeScanner` testa `PERSISTENT` — é o que
separa folha natural de folha decorativa posta pelo jogador. A `DISTANCE`
sozinha não distingue.

### B2 — Qualidade de cômodo como bitmask + BFS de fechamento

**Onde:** `HomeArea.scanRoomQuality` (5 bits) + `checkEnclosed()` (BFS de
fora para dentro).

**Adaptação:** a Fase 11 registra a casa como infraestrutura. Uma métrica
de "esta casa está habitável" fecha o ciclo — a colônia saberia se o que
construiu **serviu**. Cinco bits e uma varredura, rodando na conclusão da
obra e não por tick.

**Custo real:** ~1.500 posições para um cômodo 7×5×4. Aceitável uma vez.

### B3 — Construção camada por camada

**Onde:** `BuilderWorkGoal:204-217` — calcula `minBuildHeight` e só coloca
blocos daquele Y; quando esvazia, recalcula.

**Por quê:** garante que a parede tenha em que se apoiar. Se o seu
`BuilderWork` coloca em ordem de lista, vale considerar — é a diferença
entre uma casa e blocos flutuantes que quebram.

### B4 — Multiblocos numa segunda passada

**Onde:** `stackToPlaceMultiBlock` — porta, cama e afins ficam para
`PREPARE_PLACE_MULTIBLOCK`, **depois** de todo o resto estar de pé.

**Por quê para você: isto endereça o seu E8 diretamente.** Blocos de duas
partes saem soltos porque são colocados como duas metades independentes.
A resposta do Workers não é guardar o `BlockState` — é **colocar por
último, e colocar o par junto**, com `findPairedMultiBlockPos`
(`BuildArea:403-416`).

Isso pode resolver o E8 **sem** a decisão de arquitetura da TASK-046 (sem
levar `BlockState` para o Core, contra a ADR-005). O Core precisaria
apenas de uma marca "este `BlueprintBlock` é de duas partes, e o par é
aquele" — que é um dado, não um `BlockState`. **Ver `12 §3.5`.**

### B5 — Prefetch de materiais

**Onde:** `PREFETCH_MATERIAL_BUDGET = 4` — o construtor busca 4 materiais
distintos por viagem, priorizando os da camada atual.

**Adaptação:** no seu modelo o construtor tira do baú da colônia, e a
distância é menor. Vale se aparecer viagem de ida e volta por bloco.

### B6 — Preferência pelo último armazém (`lastStorage`)

Barato, reduz caminhada, e no seu caso quase de graça: o trabalhador já
tem baú próprio.

### B7 — Portão de notificação por dia de jogo

**Onde:** `canNotifyOwner()` — uma mensagem por dia, reaberta por
interação.

**Adaptação:** você não fala com jogador; você escreve no log. Mas o E11
produziu nove linhas idênticas em dezesseis minutos. Um portão por
`(colônia, tipo de aviso, dia)` deixaria o log legível sem esconder
mudança de estado.

### B8 — Ponto de extensão nomeado para exceção de profissão

**Onde:** `canGoHomeNow()` — o courier veta a ida para casa até voltar ao
início da rota.

**Adaptação:** quando uma regra geral sua precisar de exceção por
profissão, um método sobrescrevível com o motivo no javadoc é melhor que
um `if` sobre `ProfessionType` dentro da regra.

---

## C — Apenas referência

Boas ideias que não valem o custo hoje.

* **C1 — Courier e rede logística.** Nove tipos de ação, rota com
  waypoints, `PUT_FILL`/`TAKE_FILL`. É um mod dentro do mod. A distinção
  ordem-vs-política vale guardar para quando houver múltiplos armazéns.
* **C2 — Escaneamento de estrutura pelo jogador.** Funcionalidade boa
  (`StructureManager`), implementação com dois defeitos (`08 §6`). Se um
  dia o jogador for desenhar casas, fazer diferente.
* **C3 — Prioridade `goalSelector` por inteiro.** Você usa Brain; não se
  aplica.
* **C4 — Animação de baú abrindo com espera de 40 ticks.** Puro *game
  feel*. Entra no mesmo balde do seu P3.
* **C5 — Bônus de custo em bloco de caminho.** Faria os aldeões usarem a
  estrada da TASK-043. Exige mixin em navegação, contra a ADR-004. Ideia
  boa, porta fechada.
* **C6 — Sistema de moral.** `tickMorale` sobe/desce por ter casa. Só
  gera mensagem; não afeta trabalho. Ideia com potencial, execução vazia.

---

## D — Não utilizar

* **D1 — Goal como máquina de 17 estados.** É o padrão do mod inteiro, é
  intestável, e o seu `Task` já é melhor.
* **D2 — Estado do trabalho morando na área de trabalho.** Acoplamento
  total, `Stack` pública mutável compartilhada.
* **D3 — `Predicate<ItemStack>` como identidade de pedido.** Força
  varredura do registro de itens. Contra a ADR-005.
* **D4 — Reserva por `boolean` público sem dono.** Sem transação, sem
  liberação garantida, órfã depois do reload.
* **D5 — `static` mutável capturando estado de instância**
  (`WorkerPathNavigation.pathfinderSupplier`). Bug.
* **D6 — Dupla ordenação de `Stack` que descarta a primeira**
  (`DepositItemsToStorage:94-95`). Bug, em dois arquivos.
* **D7 — Escrever em `getPersistentData()` de block entity Vanilla.**
  Sujeira permanente no save.
* **D8 — `Minecraft.getInstance()` em código de lógica de mundo.**
  Quebra em servidor dedicado.
* **D9 — Guardar `Container` em mapa de longa vida.** Referência que
  apodrece com o chunk.
* **D10 — Flood fill com cubo 8×8×8 por tronco, sem cache.** 512
  `getBlockState` por tronco, num tick.
* **D11 — `enum` com índice numérico manual e serializado**
  (`WorkAreaTypes`). Não pode ser reordenado sem quebrar saves.
* **D12 — Resolver UUID de entidade varrendo cubo de 512 blocos**
  (`findHomeAreaByUUID`), existindo `getEntity(UUID)`.
* **D13 — Herança de mod de terceiros como base da entidade.** O Workers
  não existe sem o Recruits. Você não tem esse acoplamento e não deve
  criar.
* **D14 — Zero testes.** Não é um padrão; é a ausência de um. Registrado
  porque explica boa parte do resto.

---

## Matriz de aproveitamento

| Conceito | Arquivo origem (workers-maingit/src/main/java/com/talhanation/workers/…) | Utilidade | Complexidade | Risco | Recomendação |
|---|---|---|---|---|---|
| Motivos de falha nomeados | `entities/ai/DepositItemsToStorage.java`, `GetNeededItemsFromStorage.java` | **Alta** | Baixa | Baixo | **Reimplementar** |
| Pedido de item declarado (`NeededItem`) | `world/NeededItem.java` | **Alta** | Média | Médio | **Adaptar** (como dado) |
| Envelhecimento de alvo | `entities/workarea/AbstractWorkAreaEntity.java:110`, `ai/LumberjackWorkGoal.java:543` | **Alta** | Baixa | Baixo | **Reimplementar** |
| Pontuação por prontidão | `entities/workarea/LumberArea.java:89` | Alta | Baixa | Baixo | **Reimplementar** |
| Watchdog de reserva órfã | `entities/AbstractWorkerEntity.java:119` | **Alta** | Baixa | Baixo | **Reimplementar** |
| Baú duplo contado uma vez | `entities/workarea/AbstractWorkAreaEntity.java:253` | **Alta** | Baixa | Baixo | **Verificar e corrigir** |
| Baú inacessível não é estoque | `entities/workarea/StorageArea.java:65` | Média | Baixa | Baixo | **Reimplementar** |
| Orçamento nomeado por laço | `entities/ai/RecruitStorageUpkeepGoal.java:46` | Alta | Baixa | Baixo | **Generalizar** |
| Multiblocos em segunda passada | `entities/ai/BuilderWorkGoal.java:276-294`, `workarea/BuildArea.java:403` | **Alta** | Média | Médio | **Adaptar** (fecha o E8) |
| Construção camada por camada | `entities/ai/BuilderWorkGoal.java:204` | Alta | Baixa | Baixo | **Verificar** |
| Folha viva com `PERSISTENT` | `entities/workarea/LumberArea.java:232` | Média | Baixa | Baixo | **Verificar** |
| Qualidade de cômodo (bitmask+BFS) | `entities/workarea/HomeArea.java:257,308` | Média | Média | Baixo | **Adaptar** (Fase 11+) |
| Prefetch de materiais | `entities/ai/BuilderWorkGoal.java:230-263` | Média | Média | Baixo | **Estudar** |
| Preferência por último armazém | `entities/ai/AbstractChestGoal.java:122` | Baixa | Baixa | Baixo | **Adaptar** |
| Portão de notificação por dia | `entities/AbstractWorkerEntity.java:418` | Média | Baixa | Baixo | **Adaptar** (para log) |
| Extensão por método sobrescrevível | `entities/AbstractWorkerEntity.java:431` | Média | Baixa | Baixo | **Adotar como estilo** |
| Vetor unitário por `Direction` | `entities/workarea/MiningArea.java:104` | Média | Baixa | Baixo | **Adotar como estilo** |
| Duas métricas: guiar ≠ decidir | `entities/ai/navigation/WorkersAsyncPathfinder.java:126,218` | Média | Média | Baixo | **Estudar** |
| Orçamento adaptativo por estagnação | `.../WorkersAsyncPathfinder.java:112-187` | Média | Alta | Médio | **Estudar** |
| Custo por medida, não por limiar | `.../WorkersNodeEvaluator.java:236` | Média | Média | Baixo | **Estudar** |
| Cache de posição já avaliada | `.../WorkersNodeEvaluator.java:132` | Média | Baixa | Baixo | **Reimplementar** |
| Rede logística (courier) | `entities/ai/CourierWorkGoal.java`, `world/CourierAction.java` | Baixa hoje | Alta | Alto | **Referência** |
| Escaneamento de estrutura | `world/StructureManager.java` | Baixa | Alta | Alto | **Referência** |
| Moral | `entities/AbstractWorkerEntity.java:575` | Baixa | Baixa | Baixo | **Referência** |
| Goal-máquina de 17 estados | todos os `*WorkGoal` | — | — | Alto | **Não usar** |
| Estado na área de trabalho | `entities/workarea/*` | — | — | Alto | **Não usar** |
| `Predicate` como identidade | `world/NeededItem.java:88` | — | — | Alto | **Não usar** |
| Reserva por booleano público | `entities/workarea/AbstractWorkAreaEntity.java:49` | — | — | Alto | **Não usar** |
| Herdar de mod de terceiros | `entities/AbstractWorkerEntity.java:50` | — | — | Alto | **Não usar** |
