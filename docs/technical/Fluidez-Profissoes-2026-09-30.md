# Fluidez das profissões — levantamento de 2026-09-30

> Pedido do autor: varrer as profissões, achar o que mais trava a evolução da
> vila e propor soluções para ela andar sem pausas nem interrupções.
> Este documento é o levantamento. **Nada aqui foi implementado**, exceto o
> que a seção 6 marca como feito no mesmo dia.

## 1. Fontes e método

| Fonte | O que deu |
|---|---|
| `latest.log` da sessão de 30-09, 02:45–03:13 (28 min, JAR `8BA7B5B8…`) | todas as contagens abaixo |
| `latest.log` da sessão de 30-09, 01:38–01:59 (JAR `D0512A8E…`) | comparação do custo do ciclo antes da busca pelo prazo |
| save `Novo mundo` (leitura de região) | geometria do lote da casa do pastor |
| código (`grep` das constantes de tempo, leitura dos pontos de decisão) | os tempos de cada ação |

As contagens saíram de um script de leitura do log: estados `VC_ACTIVITY`
por profissão, linhas `no X work`, paradas e desistências, e a distribuição
de `Colony cycle took`. Onde algo é hipótese, está escrito "hipótese".

## 2. A sessão em números

- **Uma obra em 28 minutos.** A casa do pastor de `e79a3177` abriu às 02:48:05
  e fechou às 03:07:44 (19 min, 150 blocos). Depois dela, nenhuma obra nova.
- **Dos 19 minutos da obra, cerca de 12 foram espera por peça fabricada:**

  | Espera | De | Até | Duração | Motivo no log |
  |---|---|---|---|---|
  | tear (`loom`) | 02:49:50 | 02:57:35 | 7 min 45 s | `needs 2 minecraft:string and has 0`; entregue após "three failed profession attempts" |
  | vidraça (`glass_pane`) | 03:00:09 | 03:04:26 | 4 min 17 s | `needs 6 minecraft:glass and has 0`; fundidor sem areia |

- **O ciclo de colônia passou de um tique em 50 de ~56 ciclos.** Mediana
  125 ms, p90 165 ms, máximo 497 ms. Por fase: baús mediana 85 ms (p90 112),
  planejador mediana 28 ms (p90 60, máx 178).
- **Fundidor: 193 paradas** por falta de argila (158), areia (32), pedregulho
  e pedra; 112 delas num só minuto (02:47).
- **Fazendeiro: 26 vezes `still sweeping`** alternando com `NO_TARGET`.
- **48 linhas `could not make`**, a maioria as 16 cores de vidraça tingida.
- **101 linhas `Not a tree`** sobre troncos das casas da vila gerada.
- **Distribuição de tarefas:** 150 rodadas, 307 tarefas entregues e, somadas,
  518 tarefas que continuaram abertas ao fim da rodada.

## 3. O que mais trava, em ordem

1. **A vila não acha lugar para a próxima obra** (interrupção total). Depois da
   casa, `e79a3177` terminou o raio sem lote, e as 24 pontas de rua recusaram:
   ~10 por `village-original block (grass_block)` e ~9 por
   `not natural ground (dirt_path)`. A `3c358029` passou a sessão inteira
   assim. Nas recusas de lote, 49% são "área reservada de rua" e 37% "fora da
   altura da rua". Três colônias (`3c358029`, `e79a3177`, `2fffd4b9`) dividem a
   mesma vila em ~80 blocos, e a fusão (ADR-007) não existe.
2. **A obra para esperando peça que a vila não sabe obter.** Tear precisa de
   linha (não há fonte); vidraça precisa de vidro, que precisa de areia, que
   ninguém foi mandado buscar (`no surface gathering work: no task open`).
3. **Tudo anda no passo do ciclo de 30 s.** A tarefa só é distribuída dentro
   do ciclo (`ColonyCycle.java:153`); quem termina ou desiste no meio espera
   até 30 s. Os castigos também contam ciclos: descanso de 4 ciclos (2 min),
   ponta de rua recusada 10 ciclos (5 min), paciência de 20 ciclos (10 min),
   3 tentativas antes de estocar peça, roça adiada 20 ciclos.
4. **O ciclo é pesado e concentrado.** Um pico de 125 ms a cada 30 s é um
   engasgo de dois a três tiques visível. O maior custo é o levantamento de
   baús, e desde a mudança B o planejador também pesa.
5. **Metade do dia sem trabalho.** `WorkClock` só aceita tique < 11.000
   (46% do dia, ~9 de 20 min). É decisão de projeto, e natural, mas explica
   parte das pausas percebidas.

## 4. Erros levantados

| # | Erro | Evidência | Área | Gravidade |
|---|---|---|---|---|
| F1 | **Regressão minha (mudança B, `291187e`):** o planejador subiu de 1–2 ms para 28 ms de mediana e 178 ms de máximo, mesmo com `Planner turns 1`. O prazo de 15 ms só cobre a varredura; rua, índice e abertura de obra ficam fora dele | `Colony cycle took … planner 96 ms`, `Planner turns 2 -> 1 (… 96 ms …)` | `ColonyCycleRunner`, `SweepDeadline` | 🔴 |
| F2 | Fundidor em laço: pega tarefa de fundir sem a matéria-prima existir, para, e pega de novo | 193 paradas; 112 em 02:47 | `SmelterWork`, criação de tarefa | 🔴 |
| F3 | Ponta de rua trata `dirt_path` de outra colônia (ou solto) como chão proibido; a rua não segue nem por cima | `not natural ground (dirt_path)` ×9–11 por tentativa | `RoadIndex.isRoadArea`, `RoadPaving` | 🟠 |
| F4 | Ponta de rua da vila original bate em grama dentro da caixa da peça de rua (Regra 3) e nunca avança | `village-original block (grass_block)` ×8–12 | `BlockProtection.isVillageOriginal` | 🟠 decisão |
| F5 | Peça sem cadeia no mundo (tear ← linha) só chega depois de 3 tentativas falhas | 7 min 45 s de espera | `BiomeConstructionSupply.ATTEMPTS_BEFORE_STOCKING` | 🟠 |
| F6 | A demanda por vidro não vira tarefa de coletar areia | `no surface gathering work: no task open`; fundidor `no sand` ×32 | `ColonyGoals`, `SandGathering` | 🟠 |
| F7 | Busca do fazendeiro não fecha a volta e oscila com `NO_TARGET` | 26 `SWEEP_INCOMPLETE` | `FarmerWork`, `RingSweep` | 🟡 |
| F8 | Cadeia de tingimento testa as 16 cores de vidraça a cada falta | 48 `could not make`; às 03:00:09, 16 cores tentadas de uma vez | `CraftReasons`, receita de peça | 🟡 |
| F9 | Carpinteiro para a cada poucos itens pela reserva de metade da madeira em tora, e roda 0 peças | 12 paradas, 5 com 0 peças | `CraftingWork`, `StockRules` | 🟡 |
| F10 | Tarefa distribuída só uma vez por ciclo | até 30 s de trabalhador parado por tarefa | `ColonyCycle`, `WorkAssignment` | 🟠 |
| F11 | Ruído: `Not a tree` sobre tronco de casa da vila, 101 linhas | log | `TreeScanner` | 🟢 |
| F12 | Três colônias sobrepostas disputam lote e rua; fusão não implementada | centros observados entre ~20 e ~70 blocos um do outro, que oscilam entre âncoras | ADR-007 | 🟠 decisão |
| F13 | Levantamento de baús ainda é o maior custo do ciclo | `chests` mediana 85 ms | `ColonyChestSurvey` | 🟠 |
| F14 | Trabalho parado (`WORK_STALLED`) em pastor (2), lenhador (1), mineiro (1) | `VC_ACTIVITY … WORK_STALLED` | ofícios | 🟡 |

## 5. Soluções propostas

### 5.1 Tirar o ciclo do caminho (F10, item 3)

- **Distribuir tarefa quando o trabalhador fica livre**, e não só no ciclo:
  ao terminar ou desistir, o próprio trabalho chama uma distribuição de um
  trabalhador só. Hipótese a confirmar no código: `WorkAssignment.assign`
  aceita uma lista de trabalhadores, então chamá-lo com um só deve bastar; a
  lista de tarefas já está em memória.
- **Contar castigos em tiques, e não em ciclos**, para que a espera seja a
  pretendida e não "até o próximo ciclo". Não muda a duração; muda a
  granularidade.
- Esforço médio; risco médio (mexe no Core; pede teste de unidade e PIT).

### 5.2 A próxima obra (F3, F4, F12, item 1)

- **F3, sem decisão:** `dirt_path` à frente da ponta, mesmo de outra colônia,
  é rua existente: seguir por cima, como já se faz com a rua da própria
  colônia. Nenhum bloco muda de dono nem é destruído.
- **F4, decisão do autor:** a Regra 3 protege a grama dentro da caixa da peça
  de rua. Opções: (a) proteger só blocos da peça que não sejam chão natural;
  (b) deixar a rua partir apenas de pontas fora das peças originais.
- **F12, decisão do autor:** implementar a ADR-007 ou, mais simples, impedir
  que uma segunda colônia nasça a menos de 2×64 blocos de outra.
- **Instrumentar o "fora da altura da rua"** (37% das recusas) com a
  diferença de altura, como já foi feito para as pontas de rua.

### 5.3 A obra não para por peça (F5, F6, F8, item 2)

- **Decidir a obtenibilidade da planta ao escolhê-la**: descer a receita até
  a folha (a lição de `cadeia-de-producao-recursiva`) e, se uma folha não
  tem rota no bioma, entregar a peça logo, ou escolher outra planta, em vez de
  esperar três tentativas.
- **Propagar a demanda:** vidraça pedida → vidro → areia → tarefa de coleta
  de areia aberta no mesmo ciclo.
- **Tingimento:** tentar só a cor que a planta pede, não as dezesseis.
- **Pedir a próxima peça antes**: a antecipação de 28-09 existe para uma
  peça; estendê-la à lista de peças artesanais da obra inteira ao abrir.

### 5.4 Ofícios em laço (F2, F7, F9, F14)

- **Fundidor:** só criar tarefa de fundir quando a matéria-prima existe no
  baú ou há coleta aberta para ela; caso contrário marcar espera com prazo.
- **Fazendeiro:** investigar por que a volta não fecha (hipótese: raio 32 com
  orçamento pequeno por tique e cursor reiniciado); instrumentar antes.
- **Carpinteiro:** a regra "metade da madeira em tora" corta a produção em
  lotes de 2–9 peças; considerar reservar tora só para a obra aberta, que já
  é contada (a correção de 28-09), e liberar o resto.

### 5.5 Custo do ciclo (F1, F13, item 4)

- **F1, primeiro:** pôr o prazo em volta do planejamento inteiro, ou devolver
  um teto de colunas generoso (por exemplo 4×1.024) mesmo com prazo, e medir.
- **F13:** o levantamento de baús já é fatiado em 8 por ciclo; espalhar a
  leitura pelos tiques entre ciclos tiraria o pico sem mudar o total.

### 5.6 A noite (item 5)

- Decisão do autor: ofícios de oficina (carpinteiro, pedreiro, fundidor)
  trabalhando à noite dentro de casa manteriam a cadeia andando enquanto
  lenhador, mineiro e construtor dormem.

## 6. Feito no mesmo dia

- Construtor fora do alcance (`15882dd`) — **confirmado em jogo**: a casa do
  pastor fechou às 03:07:44.
- Busca de lote pelo prazo (`291187e`) — funcionou (6 voltas em 21 passagens
  contra 1 em 18), mas gerou a regressão F1.
- Motivo da ponta de rua recusada (`914b657`) — funcionou; é dele que saíram
  F3 e F4.
- Rebento não nasce no espaço de obra (`1eb6352`) — viveiro do fazendeiro e
  replantio do lenhador; não visto em jogo.

## 7. Ordem recomendada

1. F1 (regressão, custo) — pequeno e mede o risco da mudança B.
2. F2 (fundidor em laço) e F3 (rua segue por `dirt_path`) — pequenos, sem decisão.
3. F10 (tarefa ao ficar livre) — o maior ganho de fluidez; pede ADR curta.
4. F5/F6/F8 (obra sem espera de peça).
5. Decisões do autor: F4, F12, noite.

## 8. Revarredura de F1 a F14 — 2026-09-30, depois das correções

Todas as correções abaixo têm GameTest ou teste unitário e passaram na
bateria completa (517/517 GameTests, unitários verdes, build sem avisos).
**Nenhuma foi vista em jogo**; o JAR com elas não foi instalado.

| # | Situação | O que foi feito | Commit |
|---|---|---|---|
| F1 | ✅ atendido | a volta pelo índice de ruas obedece ao prazo de 15 ms, como a varredura | `22b40bf` |
| F2 | ✅ atendido | fundidor: 5 paradas pelo mesmo pedido e ele funde uma peça que a obra aberta (ou as plantas da vila) vai usar, depois tenta o pedido de novo | `6852179` |
| F3 | ✅ atendido | a ponta de rua segue por `dirt_path` que já existe, de qualquer colônia | `182a1f5` |
| F4 | ✅ atendido | chão do bioma dentro de peça da vila pode ser calçado; planta à frente da ponta sai. **Sem GameTest da parte da vila original** (a bateria não gera vila) | `a71b4da` |
| F5 | ✅ atendido | peça sem rota no bioma chega na primeira falta (eram três) | `e58208c` |
| F6 | 🟡 parcial | a rota passou a olhar o bioma, então a vidraça na planície é entregue na primeira falta. **A colônia ainda não produz vidro na planície**: areia só é coletável em deserto (`isSurfaceResource`), e a demanda vidraça → vidro → areia não abre coleta | `e58208c` |
| F7 | ❌ não atendido | busca do fazendeiro que não fecha (26 `SWEEP_INCOMPLETE`) | — |
| F8 | ❌ não atendido | tingimento tenta as 16 cores de vidraça | — |
| F9 | ❌ não atendido | carpinteiro para pela reserva de metade da madeira em tora | — |
| F10 | ✅ atendido | quem fica livre recebe tarefa em até 1 s, sem gastar o relógio dos descansos | `cd22243` |
| F11 | ❌ não atendido | 101 linhas `Not a tree` sobre tronco de casa da vila | — |
| F12 | ✅ atendido | colônias que se tocam ou ocupam o mesmo espaço viram uma (ADR-007 implementada e emendada). **Sem GameTest do critério da vila gerada** | `daa5d31` |
| F13 | 🟡 parcial | baús lidos no máximo uma vez por minuto; bosque sem lugar espera 6.000 tiques; fase `population` separada no log. `minersRoom` e `roomOf` ainda leem baús a cada ciclo; custo não medido | `e00ee15` |
| F14 | ❌ não atendido | `WORK_STALLED` de pastor, lenhador e mineiro | — |

### Pedidos da mesma rodada que não eram itens F

| Pedido | Situação | Commit |
|---|---|---|
| Rebento não nasce no espaço de obra | ✅ viveiro do fazendeiro e replantio do lenhador | `1eb6352` |
| Lote com metade da base abaixo da rua | ✅ aterro de até 3 camadas (era 1); 3 é provisório, a medir | `560d546` |
| Lote que não cabe por causa da rua | ✅ desliza ao longo da rua; recuar 1–2 blocos foi tentado e retirado (quebrava a Regra 17) | `560d546` |
| Rua cresce para fora da vila | ✅ ver F3 e F4 | `182a1f5`, `a71b4da` |
| Noite | ✅ decisão: sem trabalho à noite, reação como no Vanilla; nada mudou | — |

### Tear e vidraça: alguma profissão consegue sozinha?

| Peça | Receita | Quem faz cada etapa | Sozinha? |
|---|---|---|---|
| Tear (`loom`) | 2 linhas + 2 tábuas, bancada | tábuas: lenhador → carpinteiro. **Linha: ninguém** — não há recurso nem receita que a colônia alcance (vem de aranha ou teia) | **Não.** Agora chega pela entrega de peça sem rota, na primeira falta |
| Vidraça (`glass_pane`) | 6 vidros, bancada | vidro: fundidor, a partir de areia; areia: coleta de superfície com a pá do fundidor, **só em deserto** | **Na planície, não**: entrega na primeira falta. **No deserto, sim**, pela cadeia areia → vidro → vidraça |

O pedido da peça de obra vai ao artesão (`asks the carpenter for
construction piece`); a receita vem do livro do próprio jogo.
