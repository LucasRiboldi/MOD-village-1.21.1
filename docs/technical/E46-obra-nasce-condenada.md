# E46 — A obra que nasce condenada: quadrado para achar, círculo para manter

> Diagnóstico do playtest de **2026-09-16, 21:41–22:12** (37.041 linhas).
> A causa está **provada por aritmética e conferida no log**. Nada foi
> alterado no código; a correção é uma decisão pequena e de uma linha.

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
Logo, além da divergência de forma, **o scanner procurou fora do próprio
raio** — ou procurou a partir de um centro que não é o
`2495,-3003` usado no abandono.

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

⚠️ **Não confirmado:** de qual dos dois pontos o `BuildSiteScanner`
realmente partiu nesta sessão. O código lê `colony.center()`
(`ConstructionPlanner.java:353`), mas as coordenadas achadas só fazem
sentido a partir do anchor. **Essa é a primeira coisa a instrumentar.**

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

### C1 — As duas réguas têm de ser a mesma 🔴

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

**Prova.** Teste de unidade que põe a obra no canto do quadrado
(`dx = dz = radius`) e afirma que ela **não** é considerada fora de alcance.
A conta é pública e testável sem servidor — `isOutOfReach` é `static` e não
conhece Minecraft.

### C2 — Instrumentar de qual centro o lote foi escolhido 🔴

**Problema.** As obras achadas estão fora das **duas** contas medidas do
`colony.center()`, o que só se explica se o scanner partiu de outro ponto.
O log não diz de onde ele partiu.

**Correção.** A linha `planned …` passa a registrar o centro e a distância
usados na escolha. Uma linha, e ela responde a pergunta de vez.

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

**Nada foi alterado no código.** Esta análise é o único artefato.

**Ordem sugerida:** C2 (descobrir de onde o scanner parte — pode mudar o
diagnóstico de C1) → C1 (unificar a régua) → C3 (decisão à parte).

**Não verificado:** nenhum build, teste ou gametest foi executado nesta
sessão — foi leitura de log e código.

**Ponto de partida concreto:**

- `ConstructionProject.isOutOfReach` — a conta euclidiana — `src/main/java/com/villagecolony/core/construction/model/ConstructionProject.java:162`
- `ConstructionPlanner.plan` — o guarda que abandona — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:229`
- `ConstructionPlanner` — a busca do lote — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:353`
- `BuildSiteScanner.find` — os anéis quadrados — `src/main/java/com/villagecolony/fabric/integration/BuildSiteScanner.java:379`
- `ConstructionPlanner.withinTheFarmersReach` — Chebyshev, o precedente — `src/main/java/com/villagecolony/fabric/work/ConstructionPlanner.java:708`
- `WaitingWork.giveUp` — onde as duas saídas se encontram — `src/main/java/com/villagecolony/fabric/work/WaitingWork.java:168`
