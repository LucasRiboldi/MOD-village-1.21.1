# O plano de crescimento, conferido contra o código

**2026-09-12.** O autor trouxe uma especificação de ~20 sistemas para a vila
crescer sozinha, e pediu: *"analise o que deve ser útil implementar do código
a seguir priorizando a ordem e quantidade de recurso necessária para vila
sempre se manter crescendo"*.

Este documento é essa análise. **Não é um plano de implementação** — é o
confronto entre o que a especificação pede e o que o mod já faz, ordenado pelo
que de fato destrava crescimento.

A conclusão, antes dos detalhes: **a especificação descreve um mod que em boa
parte já existe, e nenhum dos sistemas novos que ela pede é o que está
travando a vila hoje.** Um laço fechado trava a obra, e quebrá-lo é uma condição a mais em quem cria tarefa.

---

## 1. O que a sessão de 2026-09-12 mediu

Sessão de 43 minutos (09:24–10:07), jar `2f4b9ce8`, colônia
`e23c52eb`. O relato foi *"não vi nenhuma construção nascendo"*.

### A obra morreu esperando uma peça que o mod sabe fazer

```
09:29–09:36  WAITING_RESOURCES at 2387,64,-2093, 369 blocks left
             — waiting for minecraft:smooth_stone_slab
09:36:03     gives up — 369 blocks never came in 20 cycles.
             The half-built house and its lot stay taken
```

E ao lado, no mesmo instante:

```
Colony stores {OAK_LOG=36, BIRCH_LOG=45, CHERRY_LOG=21,
               CHERRY_PLANKS=40, COBBLESTONE=119, POTATO=3, COAL=6}
Smelter stopped — none of 11 colony chests had
                  minecraft:sand or minecraft:red_sand to smelt
```

**Cento e dezenove pedregulhos parados, e o fundidor procurando areia.**

A causa é um **laço fechado**, e o mod já tem quase tudo para quebrá-lo:

1. `plains_butcher_shop_2` pede `smooth_stone_slab`.
2. **Laje não é `ResourceType`, e isso é de propósito.** A javadoc de
   `CraftingWork.MASONRY` diz por escrito: *"a maior parte destas peças —
   escada, laje, muro — não é recurso declarado, e é por isso que o
   `produceForWork` existe"*. O caminho certo existe: `produceForWork` lê a
   obra aberta (`CONSTRUCTIONS.openOf`) e produz o que ela espera, sem passar
   por `ResourceType`. E `smooth_stone_slab` cai na família do **pedreiro**,
   porque o nome contém `stone`.
3. **Mas `produceForWork` só roda dentro de uma tarefa.** E a tarefa de
   alvenaria nasce da **meta de estoque** de `STONE_BRICKS` — que estava
   satisfeita. Sem meta, sem tarefa; sem tarefa, o método que sabe fazer a
   laje nunca é chamado.
4. O log diz isso com todas as letras, e já dizia em 09-10:
   `no mason work: no task open for it — 2 able to`.
5. Em paralelo, o fundidor é dirigido pela meta
   (`CraftingLookup.smeltingInputsFor`) e fica procurando **areia** enquanto
   119 pedregulhos esperam.
6. O construtor espera 20 ciclos e desiste. **O lote fica ocupado para
   sempre.**

**Isto é o "nenhuma construção nascendo", e não é buraco de lista: é falta de
veículo.** O `TODO.md` de 09-10 já havia nomeado exatamente isso — *"A causa
não é o descascar: é o veículo. O `produceForWork` só roda dentro de uma
tarefa"* — e a consequência não havia sido ligada à obra que morre.

**A correção mínima:** abrir tarefa de ofício quando a **obra aberta** espera
uma peça daquela família, independente da meta de estoque. Uma condição a mais
em quem cria tarefa, não um sistema novo. É isso que destrava o pedreiro, o
carpinteiro **e** a obra, de uma vez.

### O que NÃO era defeito

O log tem um silêncio de 31 minutos depois das 09:36, e ele é **correto**: as
linhas dizem `off hours`. A vila não trabalha à noite, e o mod não polui o log
enquanto isso. A obra nova (`383 blocks`) foi planejada às 09:36, a noite
entrou, e a sessão terminou às 10:07 — ela não teve expediente nenhum para
progredir. O `walking for 177 ticks` da última linha é o construtor indo para
um lote a 106 blocos, o que são ~9 segundos de caminhada.

### O gargalo de lote ficou pior, e é o mesmo de ontem

```
lot refusals: 9388 candidates turned down
  6527 (69,5%)  the ground there is not natural soil
  1155          the ground is not at street level
  1120          village-original or player-placed (Regra 3)
   402          no ground in the vertical window
   184          something stands inside the house's volume
```

`21 of 24 planner runs gave up before reaching the sweep`, e o aviso do
`SweepLog`: *"the sweep is not why"*.

---

## 2. O que a especificação pede e o mod já faz

Esta seção existe para **não reimplementar**. Cada linha foi conferida no
código, não suposta.

| o que a especificação pede | estado | onde |
|---|---|---|
| Partir de vila vanilla, sem construir abrigo/depósito/praça iniciais | ✅ **é o desenho do mod** | `VillageDetector` acha vila existente; nada é semeado |
| Reserva global que obra normal não consome | ✅ **e maior que o pedido** | `ColonyGoals.STONE_FLOOR = 64`, `FOOD_FLOOR = 64` (a spec pedia 32) |
| Baú individual por trabalhador, não depósito geral | ✅ | `WorkerStorage`, um por `workerId` |
| Jogador põe ferramenta melhor no baú → aldeão detecta, compara e equipa | ✅ | a ferramenta inicial é de ferro, e *"havendo uma melhor dentro do baú dele, o trabalhador troca"* — quem julga "melhor" é o próprio jogo, medindo velocidade contra o bloco daquela profissão |
| Ferramenta não deve ser construção obrigatória | ✅ | nenhuma oficina é pré-requisito de nada |
| Ferramenta afeta produtividade | ✅ | `BlockBreakTime.ticksFor` usa a ferramenta na mão |
| Estruturas vanilla por bioma, com variantes tratadas como a mesma função | ✅ | catálogo do próprio jogo — `Village style plains has 36 houses in the game catalog`; `VillageBiomesGameTest` cobre os conjuntos |
| Não tratar `small_house_1/2/3` como construções diferentes | ✅ | as plantas vêm do catálogo por estilo, não enumeradas à mão |
| Registrar o **motivo** de "nothing to work on" em vez de enfileirar obra aleatória | ✅ **e melhor que a spec** | `IdleLog` com enum de motivo, `LotRefusals` com cinco motivos contados, `SweepLog` que separa *"a varredura não rodou"* de *"a varredura não achou"* |
| Prioridade de tarefa | ✅ parcial | `TaskPriority` existe; o que falta é ela ser derivada de gargalo (§3) |
| Estrada como coisa real, não decoração | ✅ parcial | `RoadExtension` faz a rua crescer; ela ainda não é **decisão logística** |
| Não construir decoração para "completar" a vila | ✅ | o planejador só abre casa do catálogo; não há passe decorativo |

**Treze dos itens da especificação já estão de pé.** Implementá-los de novo
custaria semanas e não moveria a vila um bloco.

---

## 3. O que falta, em ordem de quanto destrava crescimento

A ordem é por **impacto medido**, não pela ordem da especificação. O custo em
recurso está ao lado porque foi o que o autor pediu.

### 🔴 P0 — O ofício que sabe fazer a peça nunca é chamado

**O defeito medido acima.** O método que sabe produzir a laje existe e está
correto; ele só nunca é chamado, porque a tarefa que o carrega nasce de meta de
estoque e não da obra.

**Por que é o primeiro:** é a diferença entre a vila construir e não
construir. Todos os outros itens desta lista pressupõem obra acontecendo. E
destrava **três** coisas com uma condição: o pedreiro (que nunca trabalhou uma
vez), o carpinteiro (travado pelo mesmo veículo) e a obra.

**O que fazer, do menor para o maior:**

1. **Abrir tarefa de ofício a partir da obra aberta**, e não só da meta de
   estoque: se a obra espera uma peça da família do pedreiro ou do
   carpinteiro, há trabalho para ele — mesmo com o estoque satisfeito.
2. **Separar as duas frases do construtor.** Hoje `no smooth_stone_slab in the
   colony chests` cobre dois casos diferentes: *acabou* e *ninguém nunca fez*.
   O segundo é o que matou esta obra, e ele soa como o primeiro.
3. **Devolver o lote da obra abandonada** — *"the half-built house and its lot
   stay taken"* retira terreno de circulação a cada fracasso.

**Custo de recurso:** zero. É condição e contabilidade. A colônia já tem 119
pedregulhos, 102 toras e 40 tábuas paradas.

### 🔴 P0.7 — Setenta por cento dos lotes são recusados por serem pedra

`isNaturalGround` aceita grama, terra, terra grossa, podzol e areia. **Pedra
não entra**, por decisão registrada (*"pedra à mostra é montanha"*). A vila do
autor é rochosa: **6527 de 9388 recusas**.

**É decisão do autor**, porque toca a Regra 3 e a Regra 19, e porque
[a separação de grupos de pedra já desfez uma regra antes](../TODO.md).
Três caminhos, e o custo muda muito entre eles:

| caminho | custo em recurso | risco |
|---|---|---|
| aceitar pedra como chão de lote | **zero** | casa nasce em afloramento, e pode ficar esquisita |
| terraplanar o lote antes de construir | terra ou o próprio bloco do chão; ~1 bloco por coluna fora de nível | mexe no mundo do jogador; mais código |
| ampliar o raio de busca de lote | zero em material, custo de tique | a casa nasce longe, e piora a logística |

**Recomendação:** aceitar pedra é a intervenção menor que resolve o gargalo —
e é literalmente o que a especificação manda fazer (*"qual é a menor e mais
eficiente intervenção que resolve esse gargalo?"*).

### 🟠 P1 — O gargalo não é calculado, e é o coração da especificação

Hoje a meta nasce da **conta da obra**: o que a planta pede, menos o que há no
baú. Não existe noção de produção contra consumo.

O sintoma é o que o site do projeto já mostra: **o pedreiro nunca trabalhou uma
única vez**, e o carpinteiro tem o mesmo veículo travado — com 15 tábuas no
estoque a meta está satisfeita e nenhuma tarefa abre. Duas profissões inteiras
paradas por desenho, não por defeito.

**O que a especificação pede e vale a pena:** um estado econômico por recurso
com `current / production / consumption / deficit`, e o gargalo derivado dele.
**O que não vale:** as doze categorias dela. Começar com **quatro** —
`FOOD`, `WOOD`, `STONE`, `HOUSING` — já muda a decisão do planejador.

**Custo de recurso:** zero. É contabilidade sobre dados que o mod já lê (ele já
varre os baús a cada ciclo).

**Ganho:** o `POTATO=3` do log é fome real que nenhuma meta enxerga hoje.

### 🟠 P2 — Camas contra população não decide habitação

A especificação está certa no princípio: *se aldeões ≥ camas, a vila está no
limite habitacional*. O mod **conta camas** (`VillageDetector`,
`ClusterRejection`) mas usa isso para **detectar vila**, não para decidir obra.

**Custo de uma casa pequena,** pela especificação e pelo que as plantas pedem:
madeira 48, pedra 16, vidro 4, porta 1, cama 1 — **somado à reserva**, então a
colônia precisa de 48+64 de madeira e 16+64 de pedra antes de abrir a obra.
Vale medir contra as plantas reais em vez de fixar o número: `butcher_shop_2`
pede **383 blocos**, oito vezes a conta da casa pequena.

**Recomendação forte:** preferir a **menor planta** que resolve o gargalo. A
colônia escolheu duas vezes seguidas um `butcher_shop` de 369 e 383 blocos, e
morreu nas duas. Uma casa pequena de 90 blocos teria subido com o que havia no
baú.

### 🟡 P3 — A porta que fica aberta à noite

Relato do autor: *"o último aldeão a entrar na sua casa para dormir deve sempre
garantir que a porta esteja fechada"*.

**Não é conserto — é funcionalidade nova.** O mod conhece porta apenas como
orientação de planta (`Blueprint.doorSide`, para saber de que lado fica a rua);
**não há nada no código que abra ou feche porta**.

Vanilla já fecha portas por conta própria de dia, pelo `VillagerEntity` e pela
`OpenDoorsTask` do Brain — o que não acontece é garantia ao anoitecer. O lugar
natural é uma varredura por casa no fim do expediente.

**Custo de recurso:** zero. **Valor de crescimento:** zero — isso é imersão e
segurança contra mob, não crescimento. Por isso está em P3 e não antes, mesmo
tendo sido pedido.

### 🟢 P4 — O que a especificação pede e eu recomendo **não** fazer agora

| item | por que esperar |
|---|---|
| Estruturas de profissão vanilla (Armorer, Weaponsmith, Fletcher…) | a própria especificação as classifica como condicionais, e nenhuma profissão do mod as consome |
| Expansão territorial, defesa, grandes fazendas | dependem de obra acontecendo, que é o P0 |
| Doze categorias de estado econômico | quatro resolvem a decisão; doze são manutenção |
| Utility score formal | com quatro recursos, a comparação direta basta |
| Entregar "arquivos completos" a cada mudança | este projeto tem limite de 500 linhas por arquivo e revisão por diff; arquivo inteiro a cada alteração é ruído, não rastreabilidade |
| Reescrever o que já existe (§2) | treze itens; custo alto e ganho zero |

---

## 4. A ordem recomendada, em uma linha cada

1. **Dar veículo ao `produceForWork`** — tarefa de ofício vinda da obra, não só da meta. Destrava pedreiro, carpinteiro e obra de uma vez. Custo zero.
2. **Decidir o chão de lote** (pedra sim ou não) — 70% das recusas. Decisão do autor.
3. **Devolver lote de obra abandonada** — cada fracasso hoje é terreno perdido.
4. **Preferir a planta menor** que resolve o gargalo — a colônia escolhe obras de 383 blocos e morre nelas.
5. **Quatro recursos com produção × consumo**, e o gargalo derivado — destrava o pedreiro e o carpinteiro, e enxerga o `POTATO=3`.
6. **Camas contra população** como gatilho de habitação.
7. **A porta ao anoitecer** — imersão, custo zero, nenhum efeito em crescimento.

Os cinco primeiros não pedem **um único bloco novo de recurso**: são lista,
contabilidade e escolha. A vila tem 119 pedregulhos, 102 toras e 40 tábuas
paradas agora.

---

## 5. O que esta análise não fez

- **Contei os materiais que as obras esperaram em vão**, em todos os logs
  disponíveis: é **um só**, `smooth_stone_slab`, 36 vezes. `ResourceType` tem
  39 valores e nenhuma laje — e isso é de propósito, não buraco. Não varri o
  catálogo inteiro de plantas, então outras peças podem ter o mesmo destino.
- **Não medi o custo real de cada planta** — só os dois `butcher_shop` que o
  log nomeia (369 e 383 blocos).
- **Não verifiquei os conjuntos de Desert, Savanna, Taiga e Snowy** em jogo. O
  `VillageBiomesGameTest` cobre a seleção; nenhuma sessão os viu construir.
- **Não implementei nada.** O pedido era análise, e a decisão dos itens 2 e 4
  é do autor.
