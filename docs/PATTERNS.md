# PATTERNS.md

Assinaturas de defeito do Village Colony. **Sintoma → causa provável →
onde olhar.**

Consulte antes de investigar. Cada entrada vem com o defeito que a pagou.
Se encontrar uma assinatura nova, adicione aqui ao encerrar a sessão.

---

## Como usar

1. Você tem um sintoma (linha de log, comportamento estranho, teste
   falhando).
2. Procure a assinatura mais parecida abaixo.
3. Vá direto ao "onde olhar" — não releia o log histórico inteiro.

---

## Ciclo e tempo

### Vila nova sem portal/mina; mineiro fica em `looking for stone, 0 of 64`

**Causa confirmada no escalonador:** a cota de uma busca é global. Um
trabalho sem alvo que vem primeiro no mapa podia gastar a busca todo tique,
mesmo quando não encontrava pedra; os demais mineiros nunca tentavam abrir
sua mina. A ordem estável tornava o bloqueio reproduzível, mas não o evitava.

**Onde olhar:** `MinerWork.tick`, `startNextStone` e
`lastSearchWorker`. A busca gira entre candidatos sem alvo e mantém o limite
de uma por tique. O log de 2026-09-13 é compatível com o defeito nas vilas
novas; confirmar no jogo se o portal aparece e a escavação começa.

---

### Vários trabalhadores diferentes presos na mesma coordenada

**Causa:** armadilha fixa no terreno — poço, ravina ou caverna aberta de
onde a navegação Vanilla não sai. No playtest de 2026-09-24, três
construtores em `-211, 66, -954` e quatro em `-196, 66, -949`, e um
pedreiro por 3h30 em `-202, 62, -937`. O guarda devolvia a tarefa, mas
nada tirava o aldeão de lá, e ele era escalado de novo (E47).

**Como achar:** `grep -oE "the worker is at [-0-9]+, [-0-9]+, [-0-9]+"`
contado com `sort | uniq -c`; coordenada repetida por trabalhadores
distintos é o terreno, não o aldeão.

**Onde olhar:** `StrandedWorkers` (dois congelamentos no mesmo ponto) e
`StrandedEscape` (a escada de saída). Sinais: `is stranded at`,
`dug a step`, `is out at`, `cannot dig out`. Antes de culpar o mod,
confira se alguma profissão cavou ali — no caso de 09-24 nenhuma cavou.

---

### `stall N/2400` subindo, `still 0/300` cravado

**Causa:** o aldeão **gira no próprio eixo**. Ele se move, então o guarda
de imobilidade (300) não conta. Ele não chega, então só o guarda de
travamento (2400) o solta — dois minutos depois.

**Onde olhar:** `MinerReach.IN_THE_PASSAGE`, `BuilderApproach.standable`.
Foi o R-006 (mineiro girando na superfície em cima da mina).

---

### `assigned 0 tasks` com recursos nos baús, por muitos ciclos

**Causa:** uma obra aberta está travada e **segurando o lote**. Sem obra,
não há pedido de material; sem pedido, ninguém trabalha.

**Onde olhar:** `ConstructionPlanner.ensureTask` — verificar se a tarefa
BUILD existe e está `isOpen()`. Foi o E14 (R-004).

### Obra aberta, `WAITING_RESOURCES` repetindo o mesmo item

**Causa provável:** o item pedido pela planta não tem rota da demanda até
um produtor executável. O projeto pode estar selecionado e a construção
corretamente bloqueada; acrescentar planos não resolve a ausência do
fornecedor.

**Onde olhar:** `ConstructionProject.remainingMaterials`,
`WorkMaterials` por `Production`, `ColonyGoals`, `ColonyCycle.typeFor` e a
ação física do executor. No log de 2026-09-14, a casa abriu com 382 blocos,
colocou dois e esperou `minecraft:dirt`; `DIRT` não existia no catálogo nem
no coletor de superfície. A correção precisa provar a cadeia inteira e o
depósito, não somente que o item é reconhecido pelo adaptador.

---

### Guardas não disparam, mas o trabalhador está preso

**Causa possível:** os contadores só andam em **expediente**.
`WorkHours.isWorkTime` é a porta. Fora de `WORK..9000`, o guarda não
conta — correto, mas a sessão parece travada.

**Onde olhar:** `WorkHours`. Conferir a hora no log (`work time` ou
`off hours`).

---

### Teste gametest passa em algumas rodadas e falha em outras

**Causa possível:** fase do `tickCounter` do servidor. O ciclo da colônia
(600 ticks) re-reserva a tarefa que o teste acabou de ver liberada.

**Onde olhar:** `VillageDetectionHandler.tickCounter`. Foi o KF-001.
O KF-001 foi fechado no teste: o ciclo re-reservava a tarefa antes da
asserção. Não atribua esse caso ao orçamento de busca. A quota de uma busca
por tick continua sendo global e pode limitar vazão em produção; só a altere
com medição de contenção/fairness entre jobs.

---

## Teste vs. jogo

### Bateria verde, jogo não funciona

**Causa:** o teste modela um mundo que o jogo não produz.

Pergunte: **"quem põe esta coisa aqui, em jogo?"** Se a resposta for
"ninguém", o teste está medindo uma fantasia.

**Onde olhar:** a fixture do teste. Foi o E10 (76 verdes, 0 tábuas — todos
punham o tronco no baú do fabricante, e ninguém põe tronco lá).

---

### Uma linha de log afirma coisa que o código não mediu

**Causa:** a linha conclui em vez de relatar. Ex: dizia "não há lote"
quando o que ela sabia era "não terminei de olhar".

**Onde olhar:** `ConstructionPlanner.silent`, `BuildSiteScanner.sweepPausedAt`.
Foi o E14 segunda metade.

---

### Uma linha de log usa um número que o código não tem

**Causa:** a linha imprimiu uma constante em vez do contador.
Ex: `made no progress for 2400 work ticks` quando o contador estava em 60.

**Onde olhar:** o construtor da frase. Foi a linha de desistência do
lenhador (`TreeChoice.giveUp`).

---

## Reserva e dono

### Dois trabalhadores no mesmo alvo, ou um só pega e o outro espera

**Causa:** falta reserva, ou a reserva está frouxa.

**Onde olhar:** `TreeClaims`, `MineClaims`, `Task.reserveFor`. A regra:
**a coisa disputada é que tem dono** — não a tarefa.

O fim de um job tambem encerra sua reserva: `MinerWork.tick` limpa
`WorkTargets` e libera `MineClaims` no mesmo tique. A regressao
`MinerWorkLifecycleTest.aClosedJobReleasesItsMineClaimOnTheNextTick` protege
esse contrato; a poda posterior por `retainOnly(JOBS.keySet())` e apenas a
rede de seguranca, nao o mecanismo normal de liberacao.

---

### Alvo ruim é servido para sempre

**Causa:** falta **escada de recusas**. Sem ela, toda recusa volta a ser
a primeira e o prazo nunca cresce.

**Onde olhar:** `TreeMarks.memoryFor` e `MineMarks.memoryFor`. A mina já tem
escada de recusas e filtros na busca e na fronteira da galeria; E44 aguarda
validação em jogo. Se o mineiro ainda ficar preso, registre o alvo e o ramo
antes de concluir que a causa continua sendo ausência de prazo.

---

### Estado que sobrevive ao dono

**Causa:** a limpeza depende de **evento** em vez de **invariante**. Quando
o evento não dispara (aldeão some sem `AFTER_DEATH`), o estado fica.

**Onde olhar:** `docs/research/estado-que-sobrevive-ao-dono.md`.
`MineClaims.retainAll(working)` é o modelo. `WORKERS` não tem essa
conferência — trabalhador fantasma.

---

## Ordem e prioridade

### `no miner branch work` repete no limite da mina

**Causa:** se todos os braços acabam no nível mais fundo, girar apenas a
galeria não muda a hélice que o mineiro precisa atravessar. O mesmo bloqueio
volta a ser servido.

**Onde olhar:** `Mine.deepenIfEveryArmIsDone`, `MineShaft.rerouted` e
`MineSave.SHAPE_VERSION`. A rotação só deve acontecer após todos os braços
terminarem; ao mudar a ordem, o save reinicia cursores e mantém a boca.
ADR-013 registra a decisão. Ainda exige validação em jogo.

### Uma lista de materiais vira prioridade por acaso

**Causa:** `Map.copyOf` devolve mapa **sem ordem**, embaralhada a cada
execução. Onde a ordem define prioridade, é bug.

**Onde olhar:** `ConstructionProject.remainingMaterials`. Foi o R-005
(a prioridade do fabricante virou sorteio).

**Correção:** `Collections.unmodifiableMap`.

---

### A conta diz "tenho" e o construtor espera

**Causa:** **discordância** entre a conta e o consumidor. Não é a
substituição em si.

**Onde olhar:** `ResourceSubstitution` (javadoc), `ResourceDemand.available`
vs. `MaterialChoice`. Foi o defeito de 2026-08-22 (pedregulho respondia
por arenito na conta, e o construtor exigia arenito).

**Regra:** a conta e o construtor precisam **concordar**.

---

## Minecraft / API

### Chunk travando ou terreno parando de carregar

**Causa:** `world.getBlockEntity` chamado de dentro do evento de chunk.
Força carregamento e a thread trava.

**Onde olhar:** `ChestInventoryReader.read`, qualquer scanner.
Correção: `getChunkManager().getWorldChunk(...)` + checar `null`.

---

### Alvo que o aldeão não alcança, mesmo estando perto

**Causa:** alvo sólido entregue à navegação. O Vanilla **sobe** o alvo até
sair da rocha — dentro de uma mina, isso é a superfície.

**Onde olhar:** `MinerReach.legTowards`, `BuilderApproach.standable`.
Foi o E32 (o mineiro ia para a superfície em vez de entrar na escada).

---

### Alvo no ar entregue à navegação

**Causa:** o Vanilla **abaixa** o alvo até o chão da coluna. Não é bug —
é o que ele faz. Mas se o alvo era para ser em cima da pedra, o
comportamento é o oposto.

**Onde olhar:** mesma família do E32. `MinerWork.approachTo`.

---

## Bateria e teste

### Um teste que passava de repente falha, e ninguém mexeu nele

**Causa provável:** teste depende de relógio compartilhado, ou de ordem
de registro, ou de chave estática não limpa.

**Onde olhar:** `ColonyFixture`, `WorkStall`, `COLONIES`, `WORKERS`.
Regra: o teste limpa em `finally`, nunca depois da asserção.

---

### Um teste afirma sobre contagem global e falha por contaminação

**Causa:** a bateria é **concorrente**. Estruturas de vizinhos ficam a
menos de 64 blocos.

**Onde olhar:** a asserção. Deve ser sobre **posições específicas** que o
teste plantou, nunca sobre contagem global.

---

### Um teste novo passa, mas não prova nada

**Causa:** o cenário não reproduz a condição que o defeito precisa.

**Regra dura:** rode o teste novo contra a **regra desligada**. Se ele
passar, não está afirmando — está acompanhando.

---

## Quando o defeito é grande

### Mineiro disponível, mas nenhuma tarefa aberta

**Causa a verificar:** metas de carvão e ferro bruto dependiam de uma
obra. Pedregulho acima do piso podia deixar a profissão sem demanda,
mesmo com reservas minerais vazias.

**Onde olhar:** `ColonyGoals.MINERAL_FLOOR`, `ResourceDemand.deficit` e
log `no miner work: no task open`. Confirme estoque e meta antes de
atribuir a ociosidade à geometria da mina.

### Lenhador remove tronco de uma estrutura

**Causa:** planejamento ou execução de `TreeHarvester` ignorou
`BlockProtection`; copa viva compartilhada fazia troncos estruturais
parecerem parte da árvore.

**Onde olhar:** a proteção deve recusar a árvore inteira no plano e ser
revalidada em cada quebra. O jogo não distingue autoria de troncos
colocados manualmente; estruturas Vanilla registradas e construções da
colônia são reconhecíveis, mas construções manuais sem marca são uma
limitação explícita.

### Casa sobe com barreira de teste

**Causa:** a cadeia de produção não entregou a peça antes de o
`PatienceClock` desistir.

**Onde olhar:** `TestBarrier` (a soma no fim da sessão), `CraftingWork.stock`
vs. `ColonySupply.take`, `ManufacturerWork.produceForWork` (só roda dentro
de tarefa).

---

### Trabalhador passa a sessão inteira parado, e o log não explica

**Causa:** o motivo de ociosidade é silencioso ou é o mesmo para causas
diferentes.

**Onde olhar:** `IdleLog`, `IdleReason`. A regra: **motivo de não
trabalhar é um valor, não um silêncio**.

---

## Não é defeito (mas parece)

### `in 1 of 8 chests read`

Não é cobertura de leitura. É **um baú com conteúdo, de oito lidos**.
`ChestSurvey.coverage()` já formata.

### `Equipped N workers` não aparece

Provável: as colônias vieram do save já equipadas, e a linha só imprime
quando `N > 0`.

### O ciclo levou 86 ms uma vez

Pico de carregamento, não regressão. Não investigar sem ver repetir.

### `/time set day` não põe os aldeões para trabalhar

Não é bug. `day` = 1000, antes da janela `WORK` (2000..9000). Use
`/time set noon`.

---

### Edição do jogador faz a descoberta de lotes recomeçar

**Causa confirmada:** cada alteração efetiva de bloco perto da colônia
chamava a invalidação ampla do scanner, que descartava o cursor da varredura
incremental e os candidatos parciais já encontrados. Uma sequência de obras
do jogador podia impedir a busca de terminar.

**Onde olhar:** `PlayerWorldChangeHandler` e `BuildSiteScanner`. A reconciliação
atualiza somente a coluna de estrada afetada, preserva a passagem parcial e
reinicia o cursor que consulta as estradas. GameTests:
`playerEditKeepsAnIncrementalSweepCursor`,
`reconcilingAPlayerRoadChangeUpdatesTheIndex` e
`removingAPlayerRoadRemovesOnlyThatIndexedColumn`. Isso melhora a descoberta
de lotes, mas não prova retomada de construção em jogo.

---

## Como adicionar uma assinatura nova

Ao encerrar uma sessão, se você investigou algo por mais de 30 minutos:

```markdown
### <frase curta que descreve o sintoma>

**Causa:** <o que era, sem jargão>

**Onde olhar:** <arquivo, método, ou doc>. <Defeito de referência>.
