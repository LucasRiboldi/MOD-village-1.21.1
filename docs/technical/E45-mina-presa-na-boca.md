# E45 — A mina presa na boca

> Análise do playtest de **2026-09-16, 03:44–08:23** (log `latest.log`,
> 341.196 linhas). Escrito para ser executado numa sessão seguinte: a
> causa está provada, a correção está proposta, nada foi alterado no
> código ainda.
>
> **Revisado em 2026-09-16, 22:30**, contra um segundo playtest
> (21:41–22:12, 37.041 linhas) que **reproduziu o defeito**. A revisão
> **corrigiu o mecanismo da §3**: a primeira versão atribuía o laço ao
> reinício por `deepenIfEveryOpenArmIsDone`, e o log **refuta** isso. A
> causa real está provada abaixo, e **C2 e C3 mudaram por causa dela**.

---

## 1. O que aconteceu

A colônia rodou **4h40 e não concluiu uma única construção**. O mineiro
girou em falso a sessão inteira, do primeiro minuto ao último.

| Medida | Valor |
|---|---|
| Linhas do log | 341.196 |
| `hit stone with nowhere to stand` | 166.559 |
| `no miner branch work` | 166.558 |
| **Soma das duas** | **97,6% do log** |
| Cadência | **10/s, constantes, por 16.657 s sem interrupção** |
| `Miner … took` (pedra quebrada) | **0** |
| `went one level deeper` | **0** |
| Construções concluídas | **0** |

O log traz o par sempre na mesma ordem, em rodízio pelos três mineiros:

```text
[04:27:04] Miner 9e7fd8b4 hit stone with nowhere to stand - the branch ends here
[04:27:04] COLONY: no branch work
[04:27:04] Miner d492ef6b hit stone with nowhere to stand - the branch ends here
[04:27:04] COLONY: no branch work
[04:27:04] Miner fe9a0ec2 hit stone with nowhere to stand - the branch ends here
```

## 2. O custo real — não é o log

O log é o sintoma barulhento. O prejuízo foi a vila parar de construir:

- **Cobblestone: 411 → 10**, e nunca reposto. Quem repõe é o mineiro.
- **Builder parou 46 vezes** por `no minecraft:cobblestone`, 57 por `dirt`.
- A obra em `2442,64,-2972` chegou a **12 blocos restantes** e morreu.

É o padrão de `casa-nao-sobe-e-fome-nao-travamento` por outra porta: **o
mineiro travado mata o builder**.

## 3. O mecanismo, tique a tique

Provado por leitura do código e **medido no log do playtest de 21:41**.
Vale enquanto `Mine.branchesOpenNow() == 1`, isto é, enquanto o poço não
foi cavado.

O laço inteiro cabe numa passagem só, e se repete dez vezes por segundo:

1. Mineiro A pede braço. `MineClaims.claimArm` dá o braço 0 — `taken[0] = A`,
   e `open.test(0)` é `true` porque `done == false`.
2. `nextCut` roda até `CUTS_PER_SEARCH = 64` posições **na mesma passagem**.
   A boca está emparedada: `nowhereToStand` é verdade, e cada volta do laço
   chama `blockedAgain`. Na **oitava** (`BLOCKED_BEFORE_TURNING = 8`) vem
   `finish()` — `done = true` — e o `break`. → **log `hit stone`**, uma linha
   por passagem.
3. Mineiro B pede na mesma passagem. `open.test(0)` agora é `false`, e
   `claimArm` **solta** o braço (`taken[0] = null`) sem achar outro: com
   `branchesOpenNow() == 1`, não há segundo braço a oferecer. → **log
   `no branch work`**, com `0 digger(s) in 1 open branch(es)`.
4. Na passagem seguinte o braço 0 volta a ser servido, **com o cursor onde
   estava**, contra o mesmo bloco emparedado. Volta ao passo 1.

### A aritmética que fecha o caso

- `CARVED = HELIX_SIDE(5) × STAIR_HEADROOM(3) × STAIR_LANES(2) × HELIX_FLIGHTS(4) = 120`.
  O poço só é considerado aberto — e `branchesOpenNow()` só passa de 1 para 4 —
  depois de **120** posições vencidas.
- `BLOCKED_BEFORE_TURNING = 8` contra `CUTS_PER_SEARCH = 64`: o braço **fecha
  em toda passagem**, sempre antes de o orçamento de busca acabar.
- **Nenhuma posição é vencida num universo que exige 120.** A mina não tinha
  como abrir o poço; o laço era inevitável, não azar.

### Por que o reinício **não** é o culpado — e o log prova

A primeira versão desta análise dizia que `deepenIfEveryOpenArmIsDone`
fechava os quatro braços e `restartAt` devolvia o cursor à boca. **É falso, e
o log de 21:41 refuta em dois números.**

Na superfície (`y ≈ 64`), `levelFloor()` é `64 − DESCENT(20) = 44`, e
`DEEPEST` é `−59` — portanto `mayDeepen()` é **verdadeiro**. Se aquele
caminho fosse tomado, `deepenIfEveryArmIsDone` desceria de fato, retornaria
`true`, e `MineDigging` escreveria `went one level deeper`.

| Frase | Esperado se o reinício fosse a causa | Medido |
|---|---|---|
| `went one level deeper` | ~17.500 | **0** |
| `no miner branch work` | ~0 (o braço reabriria) | **17.517** |
| `The gallery really ends at` | — | **1** |

`no branch work` só é escrito **depois** de `deepenIfEveryOpenArmIsDone()`
retornar `false`. Ele aparece 17.517 vezes: o reinício **nunca aconteceu**.

Pelo mesmo motivo, `MineFrontier.findTheFrontier` também não é o culpado —
ele escreveria `The gallery really ends at` a cada reposição, e escreveu
**uma vez em toda a sessão**.

**O braço não precisa ser reaberto por ninguém.** Ele é servido de novo na
passagem seguinte simplesmente porque `claimArm` o soltou no passo 3, e
`done` volta a ser consultado num braço que o próximo `claimArm` reocupa.
O que se repete não é um reinício: é a **mesma passagem inteira**, de graça,
dez vezes por segundo.

### O que continua mudo

O laço não deixa **uma linha própria**. As duas frases que ele emite dizem
"este braço acabou" e "não há trabalho" — as duas são estados normais. Nada
no log diz *"a mina serviu a mesma posição pela milésima vez sem uma
picareta"*, e é por isso que o defeito só apareceu pelo efeito colateral.

## 4. Cinco hipóteses testadas e descartadas

Ficam registradas para não serem repetidas. As duas últimas foram
descartadas na revisão de 22:30 — e a quarta era a tese da própria
primeira versão.

- **Não é o conserto do `ColonyEdits`** (2026-09-16, `MinerWork.java:704`).
  Ele só age depois que a picareta pega. Com **zero** quebras, esse guarda
  nunca chegou a rodar.
- **Não é o lenhador via `PlayerWorldChangeHandler.reopenFrom`.** Ele
  derrubou ~65 árvores/hora, cerca de uma a cada 55 s. **Uma causa de 1/55 s
  não sustenta um efeito de 10/s.** E `MINE_NEIGHBOR_DISTANCE` é **1**: só
  bloco colado ao túnel reabre um braço.
- **Não é o ramo do fundo da mina** (`!shaft.mayDeepen()` → `rerouted()`).
  Todos os Y do log estão entre **59 e 65** — superfície. `DEEPEST` é `-59`,
  ~120 blocos abaixo. Esse ramo nunca foi tomado, e o teste que o cobre
  (`theDeepestLevelRotatesInsteadOfRepeatingTheSameBlockedPattern`) está
  correto e continua valendo.
- **Não é o reinício por `deepenIfEveryOpenArmIsDone` + `restartAt`** — era
  a tese da primeira versão. `went one level deeper` aparece **0** vezes e
  `no miner branch work` **17.517**; a segunda frase só é escrita quando o
  reinício **não** ocorreu. Ver §3.
- **Não é `MineFrontier.findTheFrontier` repondo o cursor.** Ele registra
  `The gallery really ends at` a cada reposição: **1** linha na sessão
  inteira, contra 17.518 recusas.

## 5. O defeito de fundo: guarda que o caminho de falha zera

Durante **4h40 inteiras** os três contadores de segurança do mineiro
ficaram parados em zero:

```text
stall 0/2400, still 0/300, adrift 0/400
```

Nenhum chegou perto de disparar. O caminho de falha zera os contadores no
mesmo tique em que falha, de modo que nenhum limite é alcançável.

**Guarda que o caminho de falha zera não é guarda.** Esta é a terceira volta
do mesmo laço — 19.193 linhas em 02:58 de 09-16, nove desistências em 09-11,
agora 166.559. Cada conserto anterior fechou **uma porta específica**
(`arm.digging()` cedo demais; a picareta vista como edição do jogador). O
contador que deveria pegar *qualquer* porta nunca disparou em nenhuma das
três.

---

## 6. Correção proposta

Quatro itens. Os três primeiros são independentes e podem entrar juntos; o
quarto é decisão do autor.

### C1 — O braço servido sem picareta não pode ser mudo 🔴

**Problema.** O laço não emite uma linha própria: as duas frases que ele
escreve — "o braço acabou", "não há trabalho" — descrevem estados normais.
17.518 repetições e nenhuma delas diz que **nada progrediu**.

**Correção.** Contar, na `Mine`, as passagens em que o braço foi servido e
fechado **sem uma única quebra**, e registrar uma linha quando esse contador
passa de um patamar (ex.: a cada 100), com a posição em que o cursor está
parado. O que se registra é a **falta de progresso**, não o reinício — que,
como a §3 mostra, não acontece.

**Prova.** Teste que serve o braço N vezes sem nunca chamar `pickaxeTook` e
afirma que o evento foi comunicado (contador ou callback observável no core,
sem depender do logger do Fabric).

### C2 — Repetição sem progresso tem de custar 🔴

**Problema.** `finish()` zera `blocked`, e o braço é reocupado na passagem
seguinte no mesmo `cut`. Nada lembra, entre passagens, que a anterior não
rendeu pedra.

⚠️ **Mudou na revisão de 22:30.** A versão anterior propunha um contador
"que `restartAt` não zera". Isso não resolve: `restartAt` **não roda** neste
laço. O contador precisa sobreviver ao **`finish()`**, que é quem o apaga
hoje — e é a `Mine`, não o `MineArm`, que deve guardá-lo, porque o braço é
solto e reocupado a cada passagem.

**Correção.** Contador de **passagens servidas sem picareta**, guardado na
`Mine`, que **só** `MineDigging.pickaxeTook` zera — mesmo dono do zeramento
adotado em 2026-09-11, pelo mesmo motivo: quem zera é o bloco saindo do
mundo, não o servir da posição. Ao passar de um limite, a mina para de servir
aquele desenho e toma a saída de C4.

**Prova.** Teste que serve e fecha o braço N+1 vezes sem nunca chamar
`pickaxeTook` e afirma que a mina deixou de devolver a mesma posição.

### C3 — ~~`everyOpenArmIsDone` não pode aceitar 1 de 4 como "todos"~~ 🟢 retirado

⚠️ **Retirado na revisão de 22:30.** A premissa era que
`deepenIfEveryOpenArmIsDone` fechava os outros três braços e disparava o
reinício. O log refuta: esse caminho **nunca é alcançado** (`went one level
deeper` = 0; ver §3 e §4).

Mexer em `everyOpenArmIsDone` não tocaria neste defeito, e **arriscaria o
limbo de 2026-09-04** ("não posso entregar / não posso descer") que aquela
lógica existe para evitar. Fica como não-fazer registrado.

### C4 — Decisão do autor: o que fazer quando a boca é intransponível 🔴

> **Promovido a 🔴 na revisão de 22:30.** Com C3 retirado e C2 reduzido a
> uma rede de segurança, **C4 é a correção de verdade**: enquanto a boca for
> intransponível, nenhum contador faz a mina cavar.

**O caso.** O poço exige 120 posições; a primeira é emparedada e não há
onde o aldeão fique de pé. Hoje a resposta é repetir para sempre.

Três saídas possíveis, e **a escolha é sua**:

| Saída | O que faz | Preço |
|---|---|---|
| **(a) Mudar a boca** | `MineSite.mouthOf` procura outra coluna e a mina renasce ali | Abandona o poço iniciado; boca pode migrar |
| **(b) Girar a hélice no mesmo nível** | Reusa `MineShaft.rerouted()`, que já existe para o fundo | Mantém a boca; pode girar em vão se o impedimento for a boca |
| **(c) Desistir por um prazo** | A mina dorme N tiques e o mineiro cai no `exposedStone` | A colônia raspa pedra exposta e sobrevive, mas não cava |

**Recomendação:** **(b) primeiro, (a) como escalada** — girar é barato e
reusa código já testado; se as quatro hélices falharem, a boca é que está
ruim, e aí (a) se justifica. (c) fica como rede de segurança de C2, para a
colônia nunca ficar sem pedra nenhuma enquanto isso se resolve.

---

## 7. O que falta — estado para a próxima sessão

**Nada foi alterado no código.** O repositório está como estava ao fim do
ciclo de 2026-09-15; esta análise e sua revisão são os únicos artefatos
novos.

**Ordem sugerida (revisada em 22:30):** **C4** (a correção de verdade —
decisão + implementação) → C1 (torna o laço visível) → C2 (rede de
segurança). **C3 foi retirado.**

**Antes de implementar C4, o autor precisa escolher** entre (a), (b) e (c).

**Por que a boca está emparedada.** `nowhereToStand` é
`MinerWork.approachTo(world, at).equals(at)` — não há vizinho onde um
aldeão caiba. No primeiro degrau da escada, dentro da rocha maciça, isso é
**estrutural**, não azar de terreno: é um argumento a favor de (b)/(a) e
contra (c).

**Não verificado nesta sessão:** nenhum build, teste ou gametest foi
executado — as duas sessões foram de leitura de log e código. Os números de
`STATE.md` (884 unitários, 335/335 GameTests) são do ciclo anterior e
**não** foram reconfirmados aqui.

**Ponto de partida concreto** (caminhos corrigidos em 22:30 — `MineClaims`
e `MineDigging` estão em `fabric/work/`, não em `core/construction/model/`):

- `MineDigging.nextCut` — o laço, onde o braço fecha — `src/main/java/com/villagecolony/fabric/work/MineDigging.java:825`
- `MineDigging.nextTarget` — `src/main/java/com/villagecolony/fabric/work/MineDigging.java:134`
- `MineClaims.claimArm` — solta o braço fechado — `src/main/java/com/villagecolony/fabric/work/MineClaims.java:126`
- `MineArm.blockedAgain` / `finish` — `src/main/java/com/villagecolony/core/construction/model/MineArm.java:153`
- `Mine.branchesOpenNow` — `src/main/java/com/villagecolony/core/construction/model/Mine.java:208`
- `Mine.deepenIfEveryOpenArmIsDone` — **nunca alcançado neste defeito** — `src/main/java/com/villagecolony/core/construction/model/Mine.java:298`

---

## 8. Playtest de 2026-09-16, 21:41–22:12 — reproduzido

Segunda sessão do autor, 31 minutos, log de 37.041 linhas. **O defeito é o
mesmo, e a cadência é idêntica:** 10 recusas por segundo, sem uma
interrupção, do minuto 2 ao fim.

| Medida | 03:44 (4h40) | 21:41 (31 min) |
|---|---|---|
| `hit stone with nowhere to stand` | 166.559 | **17.518** |
| `no miner branch work` | 166.558 | **17.517** |
| `Miner … took` (pedra quebrada) | 0 | **0** |
| `went one level deeper` | 0 | **0** |
| Construções concluídas | 0 | **0** |
| `stall / still / adrift` | 0 / 0 / 0 | **0 / 0 / 0** |

O que a segunda sessão acrescenta — e que provou a causa — é a instrumentação
da linha de recusa, que agora diz **`0 digger(s) in 1 open branch(es)`**, nas
17.517 vezes, sem exceção. É a medida direta de `branchesOpenNow() == 1`.

**O relato do autor bate com o log:** "construções pararam e mineiro não foi
visto". O mineiro não é visto porque nunca sai da boca.

### 8.1 E as construções param por um segundo defeito, não pelo mineiro 🔴

**Isto é novo, e é independente do E45.** Em 03:44 o builder parava por falta
de cobblestone; em 21:41 **não é isso**. A biblioteca foi planejada **duas
vezes**, e as duas morreram **sem um único bloco posto**:

```text
[21:47:06] opened a build task — 628 blocks left of .../plains_library_1
[21:47:06] planned .../plains_library_1 at ColonyPos[x=2456, y=63, z=-2936]
[21:47:35] Builder f1962d31 stopped — the project is closed     ← 29 s depois
[21:50:35] planned .../plains_library_1 at ColonyPos[x=2439, y=63, z=-2932]
[21:51:06] Builder f1962d31 stopped — the project is closed     ← 31 s depois
```

Nas duas vezes: **628 blocos restantes de 628** — nada foi construído.

**O que já se sabe.** `BuilderWork.step` escreve essa frase quando
`CONSTRUCTIONS.find(projectId)` volta vazio **ou** o projeto deixou de estar
`isOpen()` ([`BuilderWork.java:224`](../../src/main/java/com/villagecolony/fabric/work/BuilderWork.java)).

**O que já foi descartado:** não é o `isSupersededBy` do
`ConstructionPlanner` — aquele caminho registra `drops the untouched`, e essa
frase aparece **0** vezes.

**Suspeita a investigar primeiro:** `ConstructionService.forget` /
`removeIf(!isOpen)` ([`ConstructionService.java:206`](../../src/main/java/com/villagecolony/core/construction/service/ConstructionService.java))
— quem remove o projeto do registro enquanto o builder ainda o segura. O
`find` vazio e o `!isOpen` **são frases indistinguíveis no log de hoje**, e
separá-las é o primeiro passo.

⚠️ **Este defeito merece número próprio (E46) e um diagnóstico próprio.**
Corrigir o E45 sozinho **não** faz a vila construir.

### 8.2 Dois achados de fundo, fora do mineiro 🟠

- **36 colônias ativas.** `Colony cycle took 486 ms` no pior caso, 40 ciclos
  acima de um tique do servidor. O playtest carrega muito mais colônia do que
  os cenários de teste, e o custo do ciclo cresce com ela.
- **A vila do defeito é uma só** (`9da5460c`). As outras 35 não planejam nem
  constroem — `assigned 0 tasks (0 open)` —, o que é o padrão de
  `roca-sem-lote-trava-a-vila` e vale conferir se é esperado.
