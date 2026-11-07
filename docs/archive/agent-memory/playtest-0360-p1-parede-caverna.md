---
name: playtest-0360-p1-parede-caverna
description: "Playtest 0.3.60 (Spark HNi26AzwWD, versão CONFIRMADA no log) — o P1 (reroute roomBeside) executa (REROUTED=10) mas NÃO atinge o aceite: NO_STANDING subiu a 35×, MINER Regra 51=60%. Causa: as recusas são uma PAREDE de caverna (cluster y67-68), não pedras isoladas; o vizinho também não tem pé e o desvio vira TOO_HIGH."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T04:34:05.420Z
---

Playtest da **0.3.60** (15-10, Spark `HNi26AzwWD`, 44 min; **versão confirmada no log**:
`villagecolony 0.3.60` — desta vez o JAR certo, ver [[jars-nao-instalados-deslocamento]]).

**O P1 executa mas NÃO atinge o aceite.** O reroute roda (`REROUTED`=10, "reroutes to the stone beside"
×10), mas:
- NO_STANDING_POSITION **subiu** a 35× (era 22×); TOO_HIGH 15×.
- MINER Regra 51 = **60%** (alvo <15%); `went one level deeper` = 0; 190 minerações, MINED 3,9/min.
- Um mineiro (`68eb770a`) fez 131 minerações com 6 reroutes; o outro (`0b1fa330`) só 42, preso.

**Causa (leitura das coordenadas):** as recusas NÃO são pedras isoladas — são uma **parede/teto de
caverna**. 50 das 59 recusas em **y67-68**, num cluster apertado `-128/-129, 67-68, 655-658` (e outro
em `-120, 63-65, 667-669`). Quando toda a região está sem pé, o `roomBeside` (que olha 1 bloco ao lado)
escolhe um vizinho **que também não tem pé** → o desvio troca NO_STANDING por **TOO_HIGH**:
`reroutes to -128,68,656 ... passes over -128,68,656 (TOO_HIGH)`.

**Conclusão:** o P1 (roomBeside, A1) é a ferramenta certa para **1 pedra emparedada**, errada para
**parede/caverna**. O conserto que falta é o A3 de verdade — quando uma REGIÃO está sem pé (N recusas
adjacentes), marcar a região e a galeria **contornar**, em vez de tentar abrir pé bloco a bloco.
Decisão pendente do autor (ver conversa 15-10).

O resto da vila segue com ócio de vila madura (MASON 85%, idle) — fases B1/B2. SHEPHERD 42% blocked e
SMELTER 42% espera são C4/P3.

**A3 FEITO (0.3.61, 15-10, commit 30f4490f):** o desvio não prossegue quando o alvo está num beco
(`MinerHands.rerouteWorthTryingAt` → `MineMarks.isInADeadEndAt`, ≥3 recusas na vizinhança), e
`MineVein.roomBeside` passa a respeitar `isUnreachableAround` (não só `isOutOfReach`). Numa parede,
deixa o contorno/curva agir; o desvio segue valendo para a pedra isolada. test 1523/0; runGametest
769/769; mutação (ignorar beco) derruba `aStoneInsideADeadEndDoesNotReroute`. JAR 0.3.61 instalado de
verdade (542A761B…). **Falta playtest de dia numa vila com caverna.**

**Descoberta reusável:** `MineMarks` já tinha a infra de "beco" (`isInADeadEndAt`, `regionRefusalsAt`,
`NEIGHBOURS_FOR_A_DEAD_END=3`, `isUnreachableAround`) — os 3 leitores por distância (veia/superfície/
areia) já a usavam; o cursor do túnel NÃO (tem a curva `BLOCKED_BEFORE_TURNING`). O P1 furava essa
defesa via roomBeside. A correção foi ligar o reroute à defesa que já existia, não criar nova.

Relacionado: [[mineiro-directpath-falso-caminho]], [[grupo-de-pedra-e-decisao]],
[[cenario-longe-da-arena-e-sorteado]], [[jars-nao-instalados-deslocamento]].
