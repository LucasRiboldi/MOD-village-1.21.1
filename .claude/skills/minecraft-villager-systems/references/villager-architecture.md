# Arquitetura do aldeão

O mapa completo. Sua utilidade prática é uma só: **descobrir em qual camada mexer**
— e não mexer nas outras.

## As seis camadas

### 1. Identidade

```text
Villager
├── VillagerType         o bioma de origem (aparência)
├── VillagerProfession   o que ele faz
├── Level                nível de comércio (1–5)
├── Experience           progresso para o próximo nível
└── Age                  bebê ou adulto
```

`[FATO]` 1.21.1: vivem em `net.minecraft.village.VillagerData`, exposto por
`VillagerDataContainer`. `VillagerType` e `VillagerProfession` são registros
separados.

### 2. Estado

```text
State
├── Brain          memórias + activities + sensores + tasks
├── Inventory      o que ele carrega
├── Gossips        o que ele "acha" de cada jogador
├── Offers         as trocas disponíveis
└── flags          dormindo, em pânico, disposto a reproduzir
```

### 3. Percepção — sensores

O aldeão só sabe o que um **sensor** escreveu numa **memória**. Ele não "olha o
mundo" quando precisa: ele consulta o que já foi percebido.

Isso é a chave da performance do sistema — e a razão de sensores terem
frequência própria.

### 4. Decisão — Brain

```text
MEMÓRIAS  →  qual ACTIVITY está ativa  →  quais TASKS podem rodar
```

O aldeão nunca decide "estrategicamente". Ele responde ao estado das memórias
dentro do modo em que está.

### 5. Ação — tasks

Andar, olhar, trabalhar, comerciar, dormir, socializar, procurar comida, fugir,
interagir. **Só a task age.**

### 6. Mundo

```text
Village
├── POIs           cama · local de trabalho · sino
├── População
├── Gossip / reputação
├── Ponto de encontro
├── Ameaças
└── Raids
```

O aldeão pertence ao mundo, não ao jogador. É por isso que a maior parte do
estado dele mora no mundo — em POIs e no próprio bloco — e não numa estrutura do
seu mod.

## O fluxo, de ponta a ponta

```text
MUNDO
  ↓  sensor observa (com frequência própria)
MEMORY
  ↓  Brain lê
ACTIVITY               ← escolhida pela Schedule, não por você
  ↓  filtra quais tasks podem rodar
TASK
  ↓  age
WALK_TARGET / interação / mudança de bloco
  ↓
MUNDO muda → o sensor percebe na próxima passagem
```

O ciclo se fecha. Mods que quebram esse ciclo — escrevendo direto na navegação,
guardando estado fora das memórias — funcionam no caso feliz e brigam com o
Vanilla em cada caso de borda.

## Separação de responsabilidades

Use como **modelo mental**, não como obrigação de criar uma classe por conceito:

| Conceito | Responde |
|---|---|
| **Profession** | identidade — o que ele é |
| **POI** | lugar e reivindicação — onde |
| **Activity** | modo — o que ele está fazendo agora, em geral |
| **Sensor** | percepção — o que ele nota |
| **Memory** | conhecimento e estado — o que ele sabe |
| **Task** | ação — o que ele faz |
| **Schedule** | tempo — quando |
| **Brain** | coordenação |

**A confusão mais cara:** achar que `Profession` controla o comportamento. Ela
não controla — ela dá identidade, e determina quais POIs ele pode reivindicar.
O comportamento é da task.

`[FATO]` 1.21.1: o record `VillagerProfession` contém `id`, os dois predicados de
workstation, `gatherableItems`, `secondaryJobSites` e `workSound`. **Não contém
tasks, nem trades, nem schedule.**

## Em qual camada mexer

A tabela que resolve a maior parte dos pedidos:

| Pedido | Camada |
|---|---|
| "ele deve notar X" | **sensor** + memória |
| "ele deve lembrar de X" | **memória** |
| "ele deve fazer X" | **task** |
| "ele deve fazer X só de dia" | task + consulta ao **schedule/activity** |
| "ele deve trabalhar no bloco Y" | **POI** + profissão |
| "ele deve ser um novo tipo de trabalhador" | **profissão** (+ POI + task) |
| "ele deve vender Z" | **trades** — não mexa na profissão |
| "ele deve fugir de W" | sensor + memória + task — **não é profissão** |
| "ele deve ir para lá" | memória `WALK_TARGET` |
| "vários aldeões devem cooperar" | estado por **vila**, não por aldeão |

Repare quantas linhas **não** exigem profissão. Ver
`examples/guard-villager-decision.md`.

## O que é do Vanilla e o que é seu

Antes de escrever, separe:

```text
VANILLA já faz          → use (degrau 1)
VANILLA aceita registro → registre (degrau 2: POI, memória, sensor, profissão)
VANILLA não cobre       → task própria + memória própria (degraus 6–8)
VANILLA precisa mudar   → Mixin mínimo (degraus 9–11), justificado
```

`references/vanilla-extension-points.md` traz o mapa completo, e é a página a ler
antes de decidir arquitetura.

## Onde o estado mora — a decisão que define tudo

| Estado | Onde deve morar | Por quê |
|---|---|---|
| profissão atribuída pelo mod | dado do aldeão / save do mod | não existe no Vanilla |
| local de trabalho | **memória `JOB_SITE`** + POI | o Vanilla já gerencia |
| destino atual | **memória `WALK_TARGET`** | é o mecanismo Vanilla |
| alvo escolhido pelo mod | memória própria registrada | com validade |
| posição de um baú | **redescoberta** do mundo | existe no mundo |
| reserva de recurso entre aldeões | estado por **vila** | é coordenação |

> **Guardar estado de IA num `static Map<UUID, ...>` é o anti-padrão número um
> deste domínio.** Ele não persiste, não expira, não é limpo na morte, vaza entre
> saves e briga com o Brain. Memória registrada resolve os cinco.
