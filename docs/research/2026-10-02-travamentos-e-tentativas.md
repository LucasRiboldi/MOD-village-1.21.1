# Travamentos e tentativas perdidas — playtest de 2026-10-02

> Pedido do autor: *"o /vc log mostra fazendeiro travado, verifique o porquê,
> e demais profissões; abra uma análise para identificar os travamentos e
> possíveis testes sem sucesso ou perda de tempo com muitas tentativas"*.
>
> Fonte: `.minecraft/logs/latest.log` (00:43–01:04, JAR `17ceb450`), Spark
> `LhqqBh973A` (16 min, TPS 20, MSPT mediano 9–10 ms, mod 1,7%) e o save
> `Novo mundo` lido com `blocks.py`.

---

## 1. Resumo

| # | Profissão | O que o log mostra | Veredito | Estado |
|---|---|---|---|---|
| 1 | Fazendeiro | `/vc log` diz `[TRAVADO]`; o log diz 11 colheitas, 2 semeaduras, pão assado | **falso travamento**: o traço só recebia desistência | corrigido |
| 2 | Construtor | 132 `skips … grass_block/dirt is in the way` em 3 obras | **defeito real**: o piso e a base da casa nunca eram assentados | corrigido |
| 3 | Mineiro | 15 min `boxed in` em y=39, 932 trocas de modo | **defeito real**: pedregulho em cima da cabeça | corrigido |
| 4 | Lenhador | 124 `Not a tree` em 41 posições, 3 rodadas | tentativa perdida: tora de casa da vila re-examinada | pendente (🟡) |
| 5 | Fundidor | 38 `no smelter surface work … sand` em 20 min | tentativa perdida: o raio inteiro varrido a cada ~30 s sem areia | pendente (🟠) |
| 6 | Fazendeiro | 8 `still sweeping — the budget ran out` | varredura que não termina no prazo; a colheita segue | pendente (🟡) |
| 7 | Construtor | 16 peças cobertas pela `TEST BARRIER` (10 `glass_pane`, 6 `wall_torch`) | cadeia sem início: sem areia, sem vidro | ligado ao 5 |
| 8 | Pastor, pedreiro, carpinteiro, fundidor | `[ATIVO] saiu do ponto preso` | linha antiga do traço, igual ao 1 | corrigido com o 1 |

---

## 2. Os três defeitos corrigidos

### 2.1 Fazendeiro "travado" que trabalha

- `/vc log` (00:49 e 01:00) mostrava `[TRAVADO] Fazendeiro`. No mesmo
  intervalo o log tem `Farmer … harvested` 11 vezes, `sowed` 2 vezes e
  `baked 2 bread`.
- O traço salvo (`ActivityTrace`, 16.384 eventos, gravado no save) só recebia
  **desistência** (`WorkerStrikes.gaveUp`) e **saída de ponto preso**
  (`StrandedWorkers`). O trabalho que dá certo nunca entrava.
- A última linha do fazendeiro era de **01-10 às 09:22**: `could not reach
  the crop at -241, 72, 422`, quando o curral o prendeu (E52, já corrigido).
  O `/vc log` mostra a última linha por profissão, e ela ficaria lá para
  sempre.
- **Correção:** `WorkerStrikes.worked`. Quando um trabalhador conclui uma
  tarefa (colheita, semeadura, tosquia, fundição, fabricação, mineração,
  corte, coleta, obra) e a última linha da profissão é uma interrupção, grava
  `RECOVERED`. São uma linha por interrupção, não uma por tarefa.
- **Teste:** `ActivityTraceTest.endsStoppedReadsOnlyTheNewestLineOfThatProfession`.

### 2.2 Obra sem piso: "a base foi retirada"

- **No save:** a casa `58405bf6` (x −272..−264, z 399..409) tem paredes em
  y=66 apoiadas direto na grama. Em y=65, onde vai o piso, há só grama e
  terra.
- **No log:** `Project 58405bf6 skips … grass_block is in the way` 90 vezes,
  `50805a31` (a roça) 35 vezes, `9054c669` 7 vezes.
- **Causa:** `BuriedPieces.mayReplaceGround` perguntava a
  `BlockProtection.mayBreak`. Essa pergunta protege a **obra aberta**, e a
  posição do piso está dentro da própria obra. A resposta era sempre "não",
  e o construtor riscava a peça.
- **Por que nenhum teste pegou:** o GameTest da regra
  (`theStreetFloorReplacesNaturalGroundOnly`) não registrava a obra. Sem obra
  registrada, a proteção não se aplicava e o teste passava.
- **Correção:**
  - `BlockProtection.mayBuildOver` é a mesma regra sem a pergunta da obra
    aberta. Casa pronta, bloco do jogador e peça da vila continuam
    intocáveis.
  - Pedido do autor do mesmo dia ("só construir a base"): a base abaixo da
    rua que não é solo (pedregulho, tábua, tora) também é construída,
    tomando o lugar do terreno. `Blueprint.isBuried` passou a valer só para
    solo, e `Blueprint.isBase` é novo.
- **Teste:** `StreetLevelGameTest.theBaseReplacesTheGroundInsideItsOwnOpenSite`,
  com a obra registrada e uma condição de controle que falha se a proteção
  não cobre a posição.
- **Não retroage:** as peças já riscadas das casas prontas contam como
  assentadas. A casa `58405bf6` continua sem piso até ser reparada ou
  refeita.

### 2.3 Mineiro preso sob pedregulho

- `199ad062` desceu para a mina, congelou em y=10, foi dado como encalhado e
  subiu 29 níveis (escada, depois pilar). Parou em `-242, 39, 393`: em cima
  da cabeça havia **pedregulho** (`cobblestone at -242, 41, 393`, 465 vezes).
- O pedregulho veio da própria colônia: o aterro da fuga e o desvio do
  mineiro usam pedregulho. A fuga só cavava terreno natural.
- **Saídas que falharam:**
  - escada: rumo à vila há água em y=43–44;
  - pilar: o pedregulho em cima;
  - túnel: os quatro lados sem chão.
  - Resultado: `boxed in` por 15 minutos.
- **Correção:** `StrandedEscape.isRubbleUnderground`. Pedregulho, pedregulho
  de ardósia e pedregulho com musgo a mais de 4 blocos abaixo da superfície
  são entulho, e a fuga os cava. Perto da superfície podem ser parede de
  alguém, e ficam.
- **Teste:** `ClimbOutGameTest.cobblestoneDeepUnderTheGroundIsRubbleTheEscapeMayDig`.

---

## 3. Tentativas perdidas ainda abertas

### 3.1 🟠 Fundidor varre o raio inteiro atrás de areia que não existe

- **Sinal:** `no smelter surface work: nothing to work on in the whole radius
  — sand`, 38 vezes em 20 min (uma a cada ~30 s), e `Smelter … stopped — none
  of N colony chests had sand`, 5 vezes.
- **Custo (Spark):** `SurfaceGatheringWork.findTarget` → `SandPatch.in` =
  **0,58%** do servidor. É o segundo maior custo do mod, atrás só do
  lenhador.
- **Efeito:** sem areia não há vidro, e o `glass_pane` da obra só sai pela
  `TEST BARRIER` (10 peças). A cadeia não tem começo neste bioma.
- **Proposta:**
  - espera crescente depois de uma varredura vazia (5 → 10 → 20 min), como o
    lenhador já faz com a árvore recusada;
  - zerar a espera quando a obra pedir vidro de novo;
  - registrar no `/vc log` "não há areia no raio".

### 3.2 🟡 Lenhador re-examina as toras das casas da vila

- **Sinal:** `Not a tree at …` 124 vezes, em 41 posições (casas e postes da
  vila), em três rodadas: 00:45, 00:50 e 01:00. A espera dobra (6.000 →
  12.000 → 24.000 tiques), mas a rodada recomeça.
- **Custo (Spark):** `TreeScanner.findNearestLog` = **0,69%**. Ele não anda
  até lá; o custo é só de leitura.
- **Proposta:** descartar já na varredura a coluna dentro de peça da vila ou
  de obra da colônia (`BlockProtection`), antes de medir a copa. Ou deixar a
  terceira recusa valer pela sessão.

### 3.3 🟡 Varredura do fazendeiro sem terminar

- **Sinal:** `still sweeping — the budget ran out before an answer`, 8 vezes,
  seguido de `RECOVERED` 20 s depois.
- Não trava: a colheita acontece. É só a volta inteira que não cabe no prazo
  de um tique.
- **Proposta:** medir antes de mexer (ver `docs/technical/Fluidez`, F1).

### 3.4 Observado, sem ação

- **Construtor** `walking for N ticks without reaching the block`, 5 vezes:
  a caminhada até a obra depois de carregar o mundo, até 224 tiques. Depois
  disso ele chega.
- **Colônias vizinhas** `has no safe lot for BigHouseMOD`, 10 vezes (uma por
  vila). A adoção é adiada, como previsto.
- **Pico do ciclo** (`Colony cycle took` 72–346 ms, 5 vezes): fase
  `population` (F13) e o primeiro ciclo depois de carregar. Igual a 01-10.

---

## 4. Pedidos do autor desta sessão

| Pedido | Feito | Teste |
|---|---|---|
| Golem sai do curral pela porteira e pula a cerca | `PenGolems` + `PenEscape` genérico. Porteira **dupla** pela junta das folhas; na simples ele **pula**, porque 1,4 de largura não cabe em 1 bloco, nem no jogo sem o mod. Só o golem da vila, não o do jogador | `PenGolemGameTest` (3 cenários) |
| Desfazer o "sem base" e construir a base original | Opção escolhida: "só construir a base". O piso fica na rua, e a base da planta entra no lugar do terreno | `BlueprintStreetLayerTest`, `StreetLevelGameTest` |
| Entrada 2 acima da rua → escada de madeira | O degrau de cima do `DoorStep` é `oak_stairs` subindo para a casa; embaixo, solo | `FoundationPreparationGameTest` (2 cenários) |
| Plantação acima da rua, para receber a água | A roça perde a camada da rua: a camada da lavoura e do canal assenta um acima do chão | `FarmPlanGameTest.theFarmSitsAboveTheStreet` |

## 5. O que o próximo jogo precisa mostrar

- **Obra nova:** sem `is in the way` de grama ou terra no piso; casa com
  pedregulho ou tábua no chão.
- **Roça nova:** a lavoura um bloco acima do terreno, com a água no canal.
- **`/vc log`:** o fazendeiro como `[ATIVO] … voltou a trabalhar em cuidar da
  plantação`.
- **Mineiro sob a mina:** `climbed to …` até `is out at`, sem `boxed in` por
  pedregulho.
- **Golem no curral:** `Golem … is fenced in` seguido de `Golem … is out of
  the pen`.
