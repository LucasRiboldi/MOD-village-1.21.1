# E46 — A obra que nasce condenada: a estrada leva o lote para fora do raio

> Diagnóstico do playtest de **2026-09-16, 21:41–22:12** (37.041 linhas).
>
> **Atualizado em 2026-09-16, depois de executar o C2**, que mudou a causa
> raiz. A primeira versão culpava a divergência quadrado/círculo (§2); ela é
> real, mas **não foi o que matou estas obras**. A causa é o **índice de
> ruas sem teto de raio** (§2-bis).
>
> **O C2 está implementado e verificado** (build + 886 testes, 0 falhas).
> **C4, C1 e C3 seguem abertos e esperam decisão do autor.**

---

## 1. O que aconteceu

A biblioteca foi planejada **duas vezes**, em lugares diferentes, e as duas
morreram em cerca de trinta segundos **sem um único bloco posto**.

```text
[21:47:06] planned .../plains_library_1 at ColonyPos[x=2456, y=63, z=-2936] — 628 blocks
[21:47:35] lets go of .../plains_library_1 at ColonyPos[x=2456, y=63, z=-2936]
           — the village centre moved to ColonyPos[x=2495, y=65, z=-3003],
             and the work is now outside the 64-block radius
[21:47:35] Builder f1962d31 stopped — the project is closed

[21:50:35] planned .../plains_library_1 at ColonyPos[x=2439, y=63, z=-2932] — 628 blocks
[21:51:05] lets go of .../plains_library_1 at ColonyPos[x=2439, y=63, z=-2932]  (mesma frase)
[21:51:06] Builder f1962d31 stopped — the project is closed
```

Nas duas vezes: **628 blocos restantes de 628**. A colônia gastou o ciclo
inteiro a planejar, preparar o terreno (`cleared 23 plants off the site`) e
abrir tarefa, para desistir no ciclo seguinte.

## 2. A causa: duas réguas diferentes para a mesma distância

**Quem acha o lote mede em quadrado. Quem o mantém mede em círculo.**

| Quem | Onde | Conta |
|---|---|---|
| Acha o lote | `BuildSiteScanner.find` — `for (int ring = 0; ring <= radius; ring++)` | **Chebyshev**: `max(|dx|,|dz|) ≤ 64` |
| Larga a obra | `ConstructionProject.isOutOfReach` — `dx*dx + dz*dz > radius*radius` | **Euclidiana**: `√(dx²+dz²) ≤ 64` |

Os dois recebem **o mesmo** `colony.center()` e **o mesmo** `searchRadius`
(64) — ver `ConstructionPlanner.java:353` e `:229`. Não há divergência de
centro nem de raio. A divergência é a **forma**.

O quadrado de lado 128 contém o círculo de raio 64 e sobra: o canto chega a
`√(64² + 64²) ≈ 90,5` blocos. **Toda obra que cai entre o círculo e o
quadrado é aprovada pelo scanner e abandonada pelo guarda no ciclo
seguinte** — e a área dessa faixa é `1 − π/4`, isto é, **21% do quadrado
varrido**.

### A aritmética dos dois casos do log

| Obra | Distância do centro `2495,-3003` | Chebyshev | Euclidiana | Resultado |
|---|---|---|---|---|
| `2456,63,-2936` | dx=39, dz=67 | **67** > 64 ✗ | **77,5** > 64 ✗ | abandonada |
| `2439,63,-2932` | dx=56, dz=71 | **71** > 64 ✗ | **90,4** > 64 ✗ | abandonada |

⚠️ **E aqui o diagnóstico aperta mais.** As duas obras estão fora **das
duas** réguas. O scanner não deveria tê-las achado nem pela conta dele.

✅ **Resolvido pelo C2, em 2026-09-16 — e a resposta é a §2-bis.**

## 2-bis. A causa raiz: o índice de ruas não tem teto de raio

**Executado como C2 em 2026-09-16.** A pergunta era de qual centro o scanner
parte. A resposta estava no relatório de fim de sessão que o `SweepLog` já
escrevia, e ninguém tinha lido:

```text
Colony 9da5460c sweep: 40 planner runs, 6 passes over 6144 columns,
  40 answered by the index — 0 restarts (0 by drift, farthest 0 blocks),
  0 complete rounds
```

**40 de 40 consultas foram respondidas pelo índice de ruas**, e o centro
**não derivou uma vez** (`0 by drift`). O caminho dos anéis — o único que
tem `ring <= radius` — **não rodou para decidir esses lotes**.

### Os dois caminhos, e só um tem teto

`BuildSiteScanner.find` bifurca logo no começo:

| Caminho | Quando | Teto de raio |
|---|---|---|
| `findAmongRoads` | há índice de ruas (o caso normal, depois da 1ª volta) | **nenhum** — percorre a lista inteira |
| anéis (`for ring…`) | não há índice | `ring <= radius` |

`findAmongRoads` sequer **recebia** o raio, e nem `siteBesideRoadAt` nem
`siteFor` o consultam. E o índice cresce sem teto: `BuildSiteScanner.remember`
acrescenta **qualquer** coluna de rua nova, e o log traz 31 linhas de
`extended the road N blocks …`.

**O mecanismo completo, então:**

1. A vila calça estrada para fora do raio de 64 — 31 vezes nesta sessão.
2. `remember` absorve cada coluna nova no índice, sem perguntar a distância.
3. `findAmongRoads` percorre o índice inteiro e serve um lote de lá.
4. O guarda de alcance, que **tem** raio, larga a obra no ciclo seguinte.

**O lote nasce de onde a estrada chegou, e não de onde o centro alcança.**

### O que isso muda no diagnóstico

A divergência quadrado/círculo da §2 é **real e continua valendo** — 21% da
área varrida cai nela. Mas **não foi ela que matou estas duas obras**: elas
estavam a 67 e 71 blocos **em quadrado**, fora das duas réguas. Unificar as
contas (C1) **não teria salvado nenhuma das duas**.

São dois defeitos na mesma família, e a ordem de correção se inverte:

- **O que matou o playtest:** o índice sem teto (C4, novo).
- **A armadilha latente:** as duas réguas (C1) — que ainda condena 21% dos
  lotes quando o índice for corrigido.

## 3. A pista do segundo centro

O log traz **dois** pontos para a mesma colônia `9da5460c`:

| Ponto | Aparições | De onde vem |
|---|---|---|
| `2495, 65, -3003` | 2 (no abandono) | `colony.center()` |
| `2448, 64, -2942` | 59 (`from anchor …`) | `candidate.center()` da detecção |

Medidas do **anchor** `2448,-2942`, as duas obras estão **dentro** do raio,
e com folga:

| Obra | Distância do anchor | Dentro de 64? |
|---|---|---|
| `2456,-2936` | **10,0** | sim |
| `2439,-2932` | **13,5** | sim |

**É o padrão `obra-orfa-por-deriva-do-centro`, mas ao contrário:** não é o
centro que oscila entre ciclos — são **duas noções de centro coexistindo**,
uma usada para achar e outra para manter.

A detecção registrou **59 vezes** `saw N beds at 2448,-2942 from anchor …,
keeping N — view not provably complete`: ela **viu** o aglomerado real de
camas e **recusou** mover o centro, porque a regra de completude não deixou.
O `colony.center()` ficou em `2495,-3003`, a 77 blocos de onde a vila de
fato está.

✅ **Respondido pelo C2, e a suspeita estava errada.** O scanner **parte de
`colony.center()`**, como o código diz. As coordenadas achadas não vinham de
outro centro: vinham do **índice de ruas**, que não tem teto de raio — ver
§2-bis. O `0 by drift` do `SweepLog` fecha a questão.

**Mas o fato desta seção continua de pé, e é o C3:** o centro da colônia está
a 77 blocos do aglomerado real de camas, e a detecção recusa movê-lo. Isso
não causou o E46, e continua sendo uma vila que mede tudo de um ponto onde
ela não está.

## 4. O que já foi descartado

- **Não é falta de material.** No playtest de 03:44 era; aqui não. As obras
  morreram com 628/628 e o estoque tinha 3.290 tábuas.
- **Não é o relógio de paciência** (`PatienceClock`/`WaitingWork.giveUpIfStalled`).
  Ele só conta para `WAITING_RESOURCES`, estado que **não aparece uma vez no
  log**; e a paciência é de 20 ciclos = **10 minutos**, contra os **29
  segundos** observados. A frase dele — `gives up on` — sai da **mesma**
  chamada `giveUp`, e foi o que primeiro confundiu: as duas saídas escrevem
  frases diferentes, mas as duas passam por `giveUp`.
- **Não é o `isSupersededBy` do planejador.** Escreveria `drops the
  untouched`: **0** ocorrências.
- **Não é `ConstructionService.forget` chamado de outro lugar.** O único
  chamador é `WaitingWork:190`, dentro de `giveUp`.
- **Não é o mineiro (E45).** Independente: as obras morrem por posição, não
  por falta de pedra.

## 5. Correção proposta

### C1 — As duas réguas têm de ser a mesma 🟡 (rebaixado pelo C4)

⚠️ **Rebaixado de 🔴 para 🟡 em 2026-09-16, depois do C4.** O círculo deixou
de governar o caminho normal: com índice de ruas, quem decide é a distância
até a rua, em **quadrado**. A régua euclidiana só é consultada agora no caso
sem índice — colônia que ainda não varreu o raio, ou que perdeu o índice por
deriva.

A divergência **continua existindo** nesse caso residual, e por isso o item
fica aberto. Mas os 21% já não descrevem a vila em operação normal, e o
`assertTrue` que o C2 deixou fixado continua valendo: ele guarda a
sobrecarga de dois pontos, que não mudou.

**Problema.** Achar em quadrado e manter em círculo condena 21% dos lotes
que o próprio scanner aprova.

**Correção.** Uma conta só, e a escolha é do autor:

| Saída | O que faz | Preço |
|---|---|---|
| **(a) Guarda passa a Chebyshev** | `isOutOfReach` usa `max(|dx|,|dz|) > radius` | Igual ao scanner e ao `withinTheFarmersReach`, que **já** usa Chebyshev (`ConstructionPlanner.java:711`). Aceita obra a 90 blocos na diagonal |
| **(b) Scanner passa a euclidiano** | O anel pula colunas fora do círculo | Vila fica redonda; 21% menos lote candidato, e a varredura encurta |

**Recomendação: (a).** O mod já mede alcance em quadrado em dois lugares
(`withinTheFarmersReach` e o próprio scanner), e `VillageDetector` varre em
quadrado. O círculo em `isOutOfReach` é o **forasteiro** — e trocá-lo alinha
três réguas de uma vez, em uma linha.

**Prova.** ✅ **Os dois testes já existem**, escritos junto com o C2 em
`ConstructionProjectTest`:

- `theCornerOfTheSweptSquareIsOutOfReachByTheStraightLine` — fixa a
  divergência de hoje: o canto do quadrado varrido (`64,64`) é recusado pela
  reta. **Quando o C1 for decidido em favor de (a), este `assertTrue` vira
  `assertFalse`** — e é ele quem avisa que o comportamento mudou de propósito.
- `theEdgeOfTheSweptSquareIsWithinReachByBothRulers` — a beira reta passa
  pelas duas réguas, mostrando que o problema é a diagonal e não o raio.

A conta é testável sem servidor: `isOutOfReach` é `static` e não conhece
Minecraft.

### C2 — Instrumentar de qual centro o lote foi escolhido ✅ feito

**Entregue em 2026-09-16.** Duas linhas, e elas respondem a pergunta em
qualquer log futuro:

- `ConstructionPlanner` — a linha `planned …` agora termina com
  `Measured from <centro>: N blocks square, N blocks straight, and the
  radius is N`. As duas contas lado a lado, porque é a divergência entre
  elas que condena a obra.
- `BuildSiteScanner.warnIfBeyondTheRadius` — um `WARN` quando o índice de
  ruas serve um lote de fora do raio: *"the road index served a lot at …
  from outside the sweep … The sweep would never have offered it"*.
  **Avisa e não corrige** — recusar a coluna muda onde a vila constrói, e
  isso é o C4.

**Verificado:** `build` passou; **886 testes unitários, 0 falhas**, conferido
no XML de `build/test-results`. Dois testes novos em
`ConstructionProjectTest` fixam a divergência das réguas — ver C1.

### C4 — Quem responde "alcançável" é a rua, não o centro ✅ feito

**Decisão do autor em 2026-09-16: a saída (c)** — o scanner está certo, e
quem estava errado era o guarda.

**O argumento que decidiu.** A estrada **foi projetada para sair do raio**.
`RoadExtension.consider` ordena as pontas candidatas da **mais distante para
a mais perto**, e o comentário no código diz por quê: *"a rua cresce pela
ponta, e não pelo meio"*. Pôr teto no scanner seria remendar o sintoma de
uma intenção deliberada. **O raio de 64 é da detecção de vila, não um limite
de crescimento.**

**O que mudou.** O guarda deixou de medir do centro e passou a medir da
**rede de ruas**:

- `ColonyRoads.blocksToTheNearestRoad(at)` — a distância em quadrado até a
  rua mais próxima do índice. Devolve **vazio** quando não há índice, e isso
  é diferente de "infinitamente longe".
- `ConstructionProject.isOutOfReach(origin, centre, radius, toTheRoad)` — a
  sobrecarga nova. Com índice, larga a obra que está a mais de
  `BESIDE_THE_ROAD` (**16**) blocos de qualquer rua. Sem índice, **cai na
  régua antiga** — não saber onde estão as ruas não é o mesmo que não haver
  nenhuma, e largar obra por ignorância era o defeito.
- `BuildSiteScanner.roadsOf(colonyId)` — o acessor que faltava.
- A linha do abandono agora diz a distância até a rua, em vez de culpar o
  centro por ter se movido.

**Por que 16.** A casa nasce encostada na rua, mas o *canto de origem* da
planta grande (13×11) fica a treze blocos dela. Dezesseis é esse número com
margem, e é deliberadamente apertado: o que se quer excluir é a obra **solta
no campo**, não a obra na ponta da estrada.

**Prova — 10 testes novos, e o que importa é o par:**

- `theWorkBesideTheRoadStaysEvenFarFromTheCentre` — o caso exato do playtest
  (obra a 77 blocos do centro, encostada na rua) **fica**. O teste afirma
  antes que a régua antiga a largava, para não passar por acidente.
- `theWorkStrandedFarFromAnyRoadIsStillLetGo` — **o defeito de 2026-09-15
  continua pego**: a obra de `638,65,-2793`, longe do centro *e* de qualquer
  rua, continua sendo largada. É o que impede este conserto de virar um
  afrouxamento.
- `withoutARoadIndexTheCentreDecidesAsBefore`, `theEdgeOfTheRoadsideToleranceIsInclusive`,
  e seis em `ColonyRoadsTest` para a conta da distância.

**Verificado:** `build` passou; **896 unitários, 0 falhas** (XML conferido);
**335 GameTests, 4 rodadas verdes em 5** — a falha é o KF-002, alheio a isto.
⚠️ **Sem playtest.**

### C3 — O centro que a detecção recusa mover 🟠

**Problema.** 59 linhas de `view not provably complete`: a detecção vê o
aglomerado de camas em `2448,-2942` e mantém o centro em `2495,-3003`, a 77
blocos. Mesmo com C1 aplicado, a colônia mede tudo — lote, roça, estrada —
a partir de um ponto onde a vila não está.

⚠️ **Não é para corrigir junto.** A regra de completude existe para não
encolher a colônia por leitura pobre (E2, 2026-08-07), e afrouxá-la sem
cuidado ressuscita aquele defeito. Fica registrado como investigação
própria.

---

## 6. Estado para a próxima sessão

**O C2 foi implementado; C1, C3 e C4 seguem abertos e esperam decisão.**

**Ordem revisada, depois do que o C2 achou:** **C4** (o índice sem teto — é
o que matou o playtest) → **C1** (unificar a régua — a armadilha latente dos
21%) → C3 (o centro que a detecção recusa mover, investigação própria).

**Verificado nesta sessão:**

- `./gradlew build` — **passou**
- `./gradlew test` — **886 testes, 0 falhas, 0 erros**, conferido no XML de
  `build/test-results/test`, e não só na saída do Gradle
- Um teste de arquitetura (`ConversionBoundaryTest`) pegou uma violação da
  ADR-005 §4 na primeira tentativa — conversão de tipo fora do
  `MinecraftTypeAdapter` — e foi corrigida antes de o commit sair

**Não verificado:** **nenhum gametest foi executado**, e **nada foi visto em
jogo**. A instrumentação do C2 é código novo que ainda não rodou num
servidor — o próximo playtest é quem confirma que as duas linhas saem como
esperado.

**Ponto de partida concreto:**

- `BuildSiteScanner.findAmongRoads` — **o caminho sem teto de raio, a causa** — `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java:634`
- `BuildSiteScanner.remember` — quem enche o índice sem perguntar distância — `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java:745`
- `BuildSiteScanner.warnIfBeyondTheRadius` — o aviso do C2 — `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java:849`
- `ConstructionProject.isOutOfReach` — a conta euclidiana — `src/main/java/com/villagecolony/core/construction/model/ConstructionProject.java:162`
- `ConstructionPlanner.plan` — o guarda que abandona — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:229`
- `ConstructionPlanner` — a busca do lote — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:353`
- `BuildSiteScanner.find` — os anéis quadrados — `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java:379`
- `ConstructionPlanner.withinTheFarmersReach` — Chebyshev, o precedente — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:708`
- `WaitingWork.giveUp` — onde as duas saídas se encontram — `src/main/java/com/villagecolony/fabric/work/WaitingWork.java:168`
