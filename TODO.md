# TODO

**Atualizado:** 2026-09-04. **Sessão de jogo de 43 minutos** (00:00–00:44),
sem crash e sem exception do mod — e com noventa e nove `WARN` que
contavam uma história ruim. Cinco ciclos saíram dela, todos de defeito
encontrado no log: o **lenhador que morria no próprio baú** (silo sem
dreno, 24 troncos destruídos), o **impasse do poço** de 25 minutos
(recusa gastava a busca de quem tinha a escada), a **perna do mineiro**
que nunca olhava para o destino, o **ciclo da colônia que pulava calado**
com baú fora de alcance, e o **teste instável** — melhorado, não curado.

O achado que sobrou é maior que todos eles e abriu o **E36**: os dois
guardas de travamento são zerados a cada alvo novo, em seis profissões, e
é isso que deixava os mineiros travados exibindo `stall 0/2400, still
0/300` por vinte e cinco minutos. **Nenhum dos cinco consertos foi visto
em jogo.**

**Antes disso — 2026-08-29, madrugada.** **Sessão de jogo à 01:19** — e ela
rodou com o jar de ontem, porque a troca do arquivo tinha falhado com o
jogo aberto. Mesmo assim mostrou dois defeitos novos, os dois de
**estado que sobrevive ao seu dono**: ferramenta de profissão que não é
trocada, e destino de trabalho que não é solto. Antes deles, o **E35
fechou** — o segundo mineiro
oscilava na fronteira da perna, e a descida passou a ser dada pela ordem
de cavar, um passo por vez. Antes dele, a mesma sessão respondeu quatro
perguntas de uma vez, e três delas com a causa exata no
log ou no arquivo do Vanilla: o buraco no meio do chão, a cama pela
metade, e — a nona tentativa — **por que o mineiro não desce**. Ele
descia; parava dois blocos antes de chegar.

Antes dela, no mesmo ciclo: o E31 e o relatório do mineiro barrado
calaram, a escada virou de **um mineiro só**, e a galeria passou a
**acender atrás do mineiro**.

O **plano depois do MVP** — a economia inteira, as profissões que faltam
e as cinco fases de crescimento — vive em
[`Village-Economy.md`](docs/technical/Village-Economy.md). Este arquivo
continua sendo o que está aberto **agora**.

Este arquivo é a **lista canônica**. Onde ele discordar do
[`Backlog.md`](docs/technical/Backlog.md) ou do
[`Project-State.md`](docs/technical/Project-State.md), vale este — os
dois pararam em 2026-08-15 e estão sendo alcançados aos poucos.

O enunciado das regras está em `Project-State.md §18`; a arquitetura de
destino, na
[`ADR-009`](docs/decisions/ADR-009-Autonomous-Village-Evolution.md).

**A distinção que este arquivo respeita:** *tem teste* e *foi visto
funcionando em jogo* são coisas diferentes, e estão separadas em toda
lista abaixo.

```text
618 testes unitários  ·  238 testes de jogo  ·  32 regras (2 emendas)  ·  9 ADRs
9 arquivos de código acima de 500 linhas  ·  6 de teste  (recontados em 08-26)
última sessão de jogo em 2026-09-04  ·  6 consertos, nenhum visto em jogo ainda
```

> A contagem de jogo era 176 aqui e **175** no `runGametest`. Recontado
> em 08-27 por `@GameTest`: são 175, e o número deste arquivo estava um
> acima.
>
> **Recontado de novo em 09-02, e escorregou para o outro lado.** Este
> arquivo dizia 217 de jogo e 558 de unidade; o `runGametest` diz *"All
> **218** required tests passed"* e o `build` fecha **565** unitários,
> zero falhas. O de jogo estava um **abaixo**, e o de unidade quatro —
> antes ainda dos três deste ciclo. Quem conta é o runner.

---

## 🎮 A próxima sessão

O gargalo do projeto **não é mais código**. A lista de conferência de uma
sessão — o que preparar, o que olhar e em que ordem, e o que cada linha de
log significa — vive em
[`docs/proxima-sessao.md`](docs/proxima-sessao.md).

**Seis consertos esperam jogo, e nenhum foi visto rodar.** Em ordem do
que olhar primeiro, porque é a ordem em que um falha esconde o outro:

1. **O lenhador entrega madeira?** A linha a caçar é
   `filled the chest — N logs collected`. Se o `N` for zero ciclo após
   ciclo, o transbordo não pegou. E `Colony ... had no room mid-harvest`
   só deve aparecer com a colônia **inteira** cheia — se aparecer cedo, o
   assoreamento (E38) chegou antes do esperado.
2. **A obra anda sem a barreira?** O relatório de encerramento diz
   `TEST BARRIER covered for N of M pieces`. Na sessão de 09-04 foram
   **47 de 169**. Se cair, a cadeia de madeira passou a entregar.
3. **Os mineiros se revezam na escada?** `waiting for the shaft` pode
   aparecer — o que não pode é ficar. Se o mesmo par ficar 20 minutos, o
   conserto não pegou.
4. **O mineiro sai da mina para buscar areia?** É o
   `digging Areia at ... y=62` com ele lá embaixo. Ele agora deve subir.
5. **Alguma colônia cala?** A linha nova é
   `no cycle work: the chest count came in partial`. Se ela aparecer, a
   colônia estava parada e agora se sabe por quê.
6. **O guarda de imobilidade morde?** A linha é
   `has not moved a block in N ticks of work time`, e o relatório do
   ciclo passa a mostrar `still` subindo em vez de `still 0/300`. Um
   trabalhador congelado agora é devolvido em quinze segundos, e não em
   dois minutos — se `still` continuar cravado em zero com trabalhador
   parado, o E36 não era tudo.

**E o estoque agora sai a cada ciclo em que muda** — é a série que faltava
para responder "a colônia tinha material?" sem adivinhar.

**A pergunta do save antigo continua aberta:** a mina de antes do bolsão
tem o cursor apontando para outra forma de galeria. O `findTheFrontier`
deve recuar sozinho; ver se recua.

---

## 📒 A sessão de 2026-09-04 — o inventário

Cinco ciclos num dia, todos abertos por **uma sessão de jogo de 43
minutos** (`latest.log`, 1.443 linhas, 00:00–00:44). Zero crash e zero
exception do mod. Noventa e nove `WARN`, e eles contavam uma história
ruim com uma causa só na raiz.

**O estado em que a sessão terminou:** 6 colônias salvas com 78
trabalhadores, mas só **uma** ciclou (`c4706b63`); 169 peças assentadas,
das quais **47 pagas pela barreira de teste** — 28% da obra era falsa; 3
lotes vazados com casa pela metade; 25 itens do jogador destruídos.

### O que a sessão mostrou, em ordem de dano

| # | O que era | A medida |
|---|---|---|
| 1 | **O baú do lenhador é um silo sem dreno.** Vara, maçã e muda não são `ResourceType` nenhum: nada as retira, e cada uma ocupa um slot para sempre | 59 ciclos de `filled the chest — 0 logs collected`; 24 `oak_log` destruídos |
| 2 | **Guarda e depósito mediam lugares diferentes** — a meta da Regra 1 é da colônia, `TreeChoice` media um baú e `TreeFelling.deposit` guardava num baú | a obra parou 20 ciclos esperando madeira |
| 3 | **Recusa no portão da escada gastava a busca do tique** | 25 min de `waiting for the shaft`, 1 pedra em meia hora |
| 4 | **`legTowards` nunca olhava para onde o mineiro ia** — andava sempre para a frente na ordem de cavar | 17 `unable to climb`; 7 deles com alvo de areia em y=62 |
| 5 | **O ciclo da colônia pulava calado** com varredura parcial de baús | nenhuma linha no log; nada no jogo |
| 6 | **O estoque só ia ao log quando um baú novo entrava** | último retrato às 00:08, 35 min de escuro |

### Arquivos alterados (10)

| Arquivo | O que mudou |
|---|---|
| `fabric/integration/ColonyChests.java` | `deposit`, `ownFirst` e `freeSpaceForGroup` sobre lista — espelhos de `withdraw` e `nearestFirst` |
| `fabric/work/TreeChoice.java` · `TreeFelling.java` | Guarda e depósito passam a percorrer a mesma lista: baú próprio na frente, colônia atrás |
| `fabric/work/MineClaims.java` | `heldByOther`, que pergunta sem reservar |
| `fabric/work/MinerWork.java` | Recusa não gasta busca; `JOBS` deixou de ser `HashMap` |
| `fabric/work/MinerReach.java` | A perna ganhou direção e `orderIndexNear`; três estados de destino |
| `core/coordination/IdleReason.java` | `COUNT_PARTIAL` |
| `fabric/event/VillageDetectionHandler.java` | O ciclo parcial fala; o estoque sai a cada ciclo, sem repetir |
| `gametest/{Lumberjack,Miner,Storage}GameTest.java` · `test/{MineClaims,MinerLeg}Test.java` | 5 de jogo e 6 unitários novos |

### Erros meus, neste ciclo — e eles custaram tempo

| # | O que eu fiz | Como se resolveu |
|---|---|---|
| 1 | **Dois testes passaram por motivo errado.** Pus o mineiro fora da escada; ele caía no atalho da boca, cujo `y` é maior, e a asserção "subiu" passava sozinha | Só apareceu ao ler `MineShaft.positionAt`. Aceitar aquele verde teria commitado conserto sem prova |
| 2 | **Afirmei "mecanismo provado"** sobre o relógio do mundo tendo só suficiência, não necessidade | O pino de relógio não consertou o teste instável. Corrigido em voz alta na hora |
| 3 | **Pinei o relógio em 14 testes preventivamente**, sem nenhum deles ter sido observado falhando | Quebrou um vizinho em 3 de 3 rodadas. Desfeito; ficou só o do teste que de fato falhava |
| 4 | **`setAiDisabled(true)` e cercar o aldeão** — as duas curas "óbvias" para congelá-lo | As duas vazam: o aldeão sem IA sobrevive à limpeza da arena, e `PERCH.up()` é y=8, fora da arena |
| 5 | **Levantei suspeita sobre a leitura de baús que estava errada** | `7 of 14 chests read` são 7 com recurso entre 14 **alcançados**. O código já separava "vazio" de "inalcançável" |

### Contagens

```text
618 testes unitários  ·  237 testes de jogo  ·  0 falhas na rodada final
10 arquivos alterados  ·  5 commits
14f4fb6 · c5a08c0 · bdb6390 · b2e4fc4 · 84f0a10
```

> **A bateria não é determinística, e isso enfraquece os "0 falhas".**
> `aFrozenMinerGivesUpLongBeforeTheStallGuard` passou **7 de 8** rodadas
> seguidas depois do conserto; antes era 1 falha em 3. É melhora medida,
> não cura.

### O que este dia NÃO provou

- **Nenhum dos cinco consertos foi visto em jogo.** Todos têm teste com
  fase vermelha conferida, e nenhuma sessão rodou depois deles
- A linha de log do ciclo parcial — forjar chunk descarregado dentro da
  arena não dá. O teste fixa a **precondição**, não a linha
- O transbordo do lenhador com a colônia inteira cheia

---

## 📒 A sessão de 2026-09-03 — o inventário

Quatro ciclos num dia, e a primeira sessão de jogo em que o mineiro
trabalhou. Este bloco é o resumo; cada ciclo tem sua entrada datada
abaixo, com o porquê.

### Arquivos criados (2)

| Arquivo | O que é |
|---|---|
| `fabric/integration/MineFlooding.java` | Tapa a nascente que a picareta abriu. Pedregulho na **face** de onde vem o líquido, nunca na pedra cavada |
| `fabric/work/WorkStall.java` | Os dois contadores do guarda num lugar só — *é expediente?* e *ele saiu do lugar?* |

### Arquivos alterados (14)

| Arquivo | O que mudou |
|---|---|
| `core/construction/model/MineShaft.java` | A galeria abre bolsões de 3×2×2 a cada 8 colunas; lado por ruído determinístico |
| `fabric/integration/OreVein.java` | `rarityOf` e a escolha do mais raro entre as seis faces |
| `fabric/work/MineDigging.java` | Guarda de emparedada estendida ao minério; `couldNotReach` larga a veia; `flooded` vira a galeria |
| `fabric/work/MinerWork.java` | `approachTo` com saída antecipada; detector de imobilidade; vedação ao quebrar |
| `fabric/work/MinerReach.java` | `APPROACH_OFFSETS` ordenado por distância |
| `fabric/work/MinerReport.java` | A linha ganhou `still N/300` ao lado de `stall N/2400` |
| `fabric/work/ShepherdWork.java` | **Gate de expediente** (era o defeito) + detector + `stallOf` |
| `fabric/work/{Builder,Lumberjack,Farmer,Manufacturer}Work.java` | Detector de imobilidade |
| `gametest/MinerGameTest.java` · `gametest/ShepherdGameTest.java` | 9 testes de jogo novos |
| `test/…/MineShaftTest.java` · `test/…/MinerReachTest.java` | 8 testes unitários novos |
| `TODO.md` · `README.md` | Registro e lista pública |

### Erros encontrados e corrigidos (5)

| # | Erro | Como aparecia |
|---|---|---|
| 1 | **A guarda de emparedada não valia para o minério** | `nextCut` conferia a posição do túnel e devolvia **outro bloco** — o minério colado nela, sem conferência. O mineiro andava para dentro da parede |
| 2 | **Veio inalcançável era servido para sempre** | A veia mora no `Mine`, que é da colônia, e `couldNotReach` só recua o cursor do túnel. Laço fechado, sem saída, para os dois mineiros |
| 3 | **`approachTo` custava até 38 mil leituras de bloco por tique** | O javadoc dizia "uma vez por pedra"; deixou de ser verdade em 09-02, quando a guarda o pôs dentro do laço de 64 candidatos |
| 4 | **O guarda cobrava 2 minutos para notar o óbvio** | Contava tique *andando* e nunca perguntava se o aldeão andou. Congelado é a assinatura de **toda** sessão registrada |
| 5 | **O pastor contava a noite** | Única profissão sem gate de expediente. O mesmo defeito de 08-26 do mineiro, ainda aberto noutro lugar |

### Melhorias (5)

| # | Melhoria | Pedido por |
|---|---|---|
| 1 | Detector de imobilidade: 15s em vez de 2 min, com o motivo no log | Achado na pesquisa (AnimaFabric, mc_aiplayer) |
| 2 | Água e lava tapadas na hora, galeria desviada | Autor |
| 3 | Minério mais raro tem prioridade | Autor |
| 4 | Galeria com bolsões em vez de linha reta | Autor |
| 5 | O detector virou peça das sete profissões | Pergunta do autor |

### Contagens

```text
612 testes unitários  ·  233 testes de jogo  ·  0 falhas
+2 arquivos  ·  14 alterados  ·  ~1.700 linhas
4 commits: ec31503 · eec09b0 · 245492e · fee3c17
```

### O que este dia NÃO provou

- O detector em jogo fora do mineiro — mesma peça, com teste, sem sessão
- A mina de **save antigo** com a galeria de forma nova
- A decisão do A\*, que continua em aberto e agora tem instrumento para
  ser decidida com evidência: `still` e `stall` lado a lado no log dizem
  se o gargalo é navegação

---

## ✅ Resolvido

### 2026-09-04 — o lenhador parou de morrer no próprio baú

O baú dele é um **silo sem dreno**: a folha derruba vara, maçã e muda,
nenhuma é `ResourceType`, nada as retira, e cada uma ocupa um slot para
sempre. O espaço de madeira só desce, e ao chegar a zero o lenhador morre
em definitivo — 59 ciclos de `filled the chest — 0 logs collected, 0 more
would fit` e 24 troncos destruídos no meio da colheita.

A causa mecânica era guarda e depósito medindo lugares diferentes: a meta
da Regra 1 é da colônia inteira, `TreeChoice` media **um** baú e
`TreeFelling.deposit` guardava em **um** baú. Os dois passaram a percorrer
a mesma lista — o próprio na frente, a colônia atrás —, que é a regra que
a retirada já segue desde 08-14. Transbordar não move o assoreamento de
lugar: tronco tem consumidor em qualquer baú, vara não tem em nenhum.
`ColonyChests` ganhou `deposit`, `ownFirst` e `freeSpaceForGroup`. Dois
testes de jogo, fase vermelha conferida (`o lenhador não derrubou nada`).
**O assoreamento continua de pé** — ver 🔴 abaixo. Commit `14f4fb6`.

### 2026-09-04 — quem espera a escada parou de gastar a busca de quem a tem

O orçamento é de **uma busca por tique para a colônia**, e quem vinha
antes no mapa a gastava — inclusive quem ia ser recusado no portão do
`claim` sem varrer coluna nenhuma. O dono ficava sem passagem, e é a
passagem dele que solta a mina quando ele não acha pedra: por isso a saída
de 09-02 nunca chegava a rodar. Vinte e cinco minutos de
`waiting for the shaft`, uma pedra em meia hora, e os dois guardas em zero.

Recusa não é busca. `MineClaims.heldByOther` pergunta sem reservar, e
`JOBS` deixou de ser `HashMap` — a ordem de hash decidia qual mineiro
monopolizava o orçamento, e qual era mudava a cada sessão. Impasse que não
se reproduz não se conserta; é o princípio que `nearestFirst` já aplica.
Um teste de jogo com a ordem **escolhida**, fase vermelha conferida.
Commit `c5a08c0`.

### 2026-09-04 — a perna do mineiro passou a olhar para onde ele vai

`legTowards` recebia o destino e **não o usava** fora do atalho de oito
blocos: `stepAlongTheShaft` andava sempre para a frente na ordem de cavar.
Acertava por acidente ao entrar para cavar fundo, e errava sempre que o
alvo estava atrás ou em cima. Dos 17 `unable to climb` da sessão, **7 eram
areia em y=62** — alvo de superfície roteado pela galeria, porque
`mineOf(job)` devolve a mina para qualquer tarefa de mineiro. Ele varreu a
galeria até o fim e nunca saiu; as linhas de granito vêm depois disso, com
ele já perdido em z=110.

O destino ganhou **três** estados, e o do meio é o que quase passou
batido: na ordem → anda até o índice, para frente ou para trás; fora dela e
**abaixo** → é a pedra que a frente não alcançou, desce como antes; fora e
não abaixo → é a superfície, sai pela boca. Sem o caso do meio o conserto
quebrava a descida — foi o que `fromInsideTheShaftTheLegKeepsGoingDown`
pegou. Três testes unitários, fase vermelha conferida. Commit `bdb6390`.

### 2026-09-04 — a colônia que decide não decidir passa a dizer isso

`runCycleOf` pula o ciclo inteiro quando a varredura de baús vem parcial —
decisão certa desde 08-07 — e pulava em **silêncio absoluto**. Uma colônia
com um único baú em chunk descarregado não faz nada, ciclo após ciclo, sem
uma linha no log. É o mesmo formato do defeito que custou três sessões na
boca da mina, e a mesma cura: `IdleLog` com `COUNT_PARTIAL`, dizendo
quantos baús ficaram fora.

Junto, o estoque voltou ao log: `logResources` só rodava quando um baú novo
entrava no registro, e a sessão teve o último retrato às 00:08 e 35 minutos
de escuro **justamente enquanto a obra parava por falta de material**.
Agora sai a cada ciclo, reaproveitando a varredura, e só quando muda.

Nada em `src/test` ou `src/gametest` mencionava `isPartial` ou
`ChestSurvey`: um baú a quatro milhões de blocos passou a provar que ele
conta como **inalcançável**, e não como vazio. Commit `b2e4fc4`.

### 2026-09-04 — o guarda de imobilidade para de esquecer que ele não andou

**E36.** `startNextStone`, `findCrop`, `findSheep` e os três `release`
zeravam o `WorkStall` ao pegar e ao largar alvo. A pergunta que esse
guarda faz — *o aldeão saiu do bloco?* — **não tem nada a ver com qual é
o alvo**: quem estava congelado continua congelado depois de a pedra à
frente dele sumir. Zerar por alvo novo deixava imune quem troca de alvo
com frequência, que é a forma de erro que a pergunta 20 da skill de
aldeões já nomeia — *pendurar a limpeza num momento em vez de conferir
uma invariante*.

**E o conserto não é invenção nova.** Construtor e fabricante nunca
tiveram este defeito porque sempre zeraram no **ramo em que trabalham**,
e não ao pegar alvo. Mineiro, fazendeiro e pastor passaram a fazer o
mesmo: sobram dois motivos para zerar, e os dois são prova de que ele não
está congelado — *ele andou* (o `WorkStall` vê sozinho) e *ele
trabalhou*. O contador de 2.400 **continua por alvo**, porque é isso que
ele mede: andei demais até *este* alvo.

`theStillnessGuardSurvivesTheTargetChanging` congela o mineiro por 100
passagens e então o jogador cava a pedra que era o alvo. Fase vermelha
conferida, e com medida: *o contador caiu de 99 para 0*. Verde em 3 de 3
rodadas depois.

> **O E36 não fechou o E37, e a suspeita registrada estava errada.** Ver
> 🔴 abaixo: o relatório da falha mostra `stall 3/2400, still 2/300` em
> 360 tiques **com os resets já removidos**. O que congela os contadores
> naquele teste é outra coisa.

### 2026-09-04 — o teste instável se afirma em passagens, não em tiques

`aFrozenMinerGivesUpLongBeforeTheStallGuard` afirmava no tique 360 que o
detector de imobilidade (300) já tinha devolvido a tarefa. Mas o guarda não
conta tique de servidor: conta **passagem** do mineiro pelo ramo "andando,
fora de alcance". A mensagem de falha passou a trazer o relatório do ciclo,
e foi ela que mediu: `stall 219/2400` com novecentos tiques gastos — um em
cada quatro. O teste dá as passagens à mão agora, no mesmo tique.

**Não está curado:** 7 de 8 rodadas, contra 1 falha em 3 antes. Commit
`84f0a10`.


### 2026-09-03 — o detector de imobilidade saiu do mineiro e virou das sete

Pergunta do autor: *"a técnica utilizada para configurar o mineiro se
aplica às outras profissões?"* Sim — e conferindo o código apareceu um
defeito aberto.

#### O que já era compartilhado

O esqueleto do mineiro **é o do lenhador**, e está dito no javadoc dele.
Despacho por ciclo, trabalho por tique, `Job` por trabalhador, guarda de
travamento. Junto já eram comuns o `IdleLog`, o `WorkTargets`, o
`BuilderApproach.standable`, o par `TreeClaims`/`MineClaims` e um
`Report` por profissão. O mineiro não inventou a técnica — é onde ela foi
mais longe.

#### O defeito: o pastor contava a noite

**Era a única profissão que andava sem gate de expediente.** Mineiro,
lenhador e fazendeiro gateiam por `WorkHours`; construtor e fabricante
saem cedo do tique inteiro quando não é hora. O pastor contava tudo.

É o defeito de 2026-08-26 do mineiro, ainda aberto noutro lugar: o
contador foi de 886 a 2086 com o relatório dizendo `off hours`, e metade
do orçamento queimou com o aldeão dormindo. Fora da hora a
`GoToWorkTargetTask` nem começa — ele está **proibido** de andar, e o
guarda existe para punir quem anda sem chegar.

> **Duas profissões foram acusadas por engano e absolvidas.** A primeira
> leitura apontou construtor e fabricante também, por não achar
> `isWorkTime` perto do contador. Os dois têm um `return` cedo no tique,
> acima. Os comentários deles são honestos, e a acusação é que estava
> errada — registrado porque grep perto do símbolo não é leitura do fluxo.

#### E o detector virou peça compartilhada

`WorkStall` — os dois contadores num lugar só, e o mineiro passou a usá-lo
também, para não haver duas implementações.

| | |
|---|---|
| **Quem ganhou** | construtor, lenhador, fazendeiro, pastor e fabricante. O fundidor não anda |
| **O que muda** | 2 minutos cegos viram 15 segundos com o motivo no log, em cinco profissões |
| **Por que classe e não campo no `Job`** | são duas perguntas que precisam ser feitas **juntas** — *é expediente?* e *ele saiu do lugar?* Soltas elas se separam, e o pastor é a prova: a primeira ficou por conta de cada profissão lembrar, e ele esqueceu |
| **Sem mapa estático** | cada `Job` tem o seu, e ele morre com o trabalho. Morte, zumbificação e dispensa já derrubam o `Job` |

| | |
|---|---|
| **Fase vermelha conferida** | sim: sem o gate cai `shepherdgametest.theStallGuardDoesNotCountOutsideWorkHours`, e só ele |
| **Verificações que rodaram** | `./gradlew build` (**612** unitários, 0 falhas) e `runGametest --rerun-tasks` (**233** de jogo, todos passaram) |
| **O que este ciclo NÃO provou** | o detector em jogo nas outras seis. No mineiro ele foi visto; nas demais é a mesma peça, no mesmo ramo, mas sem sessão |

---

### 2026-09-03, à noite — três pedidos do autor, depois da primeira sessão boa

**A sessão aconteceu, e ele gostou.** É a primeira vez que o mineiro é
visto trabalhando desde que os consertos começaram a empilhar. Os três
pedidos abaixo saíram dela.

#### 1. Saiu água: tapa e segue por outro caminho

> *"quando quebrar uma pedra e sair água por ali ele deve rapidamente
> colocar um bloco no lugar para encerrar o fluxo da água e seguir por
> outro caminho"*

Sem isto a galeria inunda, e inundada ela não é só ruim — ela **deixa de
existir**: `standable` pede dois blocos livres sobre sólido, e coluna de
água não é livre para quem anda. Com lava é pior que perder a mina, porque
`BlockBreakTime` não sabe de dano e o caminho mais curto até a pedra
atravessa o que matou o aldeão.

| | |
|---|---|
| **Onde o bloco vai** | na **face de onde o líquido vem**, e nunca na pedra recém-cavada — isso seria desfazer o trabalho e refazê-lo na passagem seguinte, para sempre |
| **Com o quê** | pedregulho: sólido, é o que o mineiro tira o dia inteiro, e sem estado a acertar (tocha de parede já custou uma rodada por causa disso) |
| **A Regra 3 vale** | água dentro do poço da vila é da vila |
| **E não se lembra de nada** | a vedação não é gravada. Se a ordem voltar a passar por ali ele cava, a água volta, ele veda e a galeria vira de novo — e quatro curvas descem um nível. Laço que se resolve descendo é laço que fecha |
| **A galeria vira na hora** | e não depois de oito recusas como o bedrock: água não é bloco duro solto, é barreira que **persegue** quem a ignora |

#### 2. O minério mais raro ganha

> *"deve sempre priorizar os minerais diferentes e mais raros"*

`OreVein.beside` devolvia **a primeira das seis faces**, e o
`Direction.values()` começa em `DOWN`. **Carvão colado no chão ganhava do
diamante colado na parede**, toda vez.

A ordem é por **etiqueta**, e não por nome de bloco — é o que preserva o
que a troca de 08-27 comprou: ardósia, outra versão e datapack entram
sozinhos. Mas é julgamento, e está escrito como julgamento: *o que é
minério* é fato do jogo, *qual é mais raro* não é, e não há etiqueta de
raridade nem dado do bloco que sirva de substituto.

```
escombros · diamante · esmeralda · ouro · lápis · redstone ·
quartzo · ferro · [minério desconhecido] · cobre · carvão
```

#### 3. Bolsões, e não uma linha reta

> *"o caminho de mineração pode ser de modo mais aleatório em bolsões e
> não uma linha reta"*

| | |
|---|---|
| **O que mudou** | a cada 8 colunas de corredor a galeria abre um bolsão de 3×2×2 ao lado |
| **O corredor continua reto, de propósito** | ele é o caminho de volta do aldeão e é dele que o `legTowards` depende. Serpentear a espinha poria dois blocos em **diagonal**, e de diagonal a navegação não passa sem os cantos abertos — é o E34 pela porta de trás |
| **O bolsão fica pendurado ao lado** | cada bloco encosta no corredor ou no anterior do próprio bolsão, então a contiguidade que o `findTheFrontier` assume continua valendo |
| **Ganha-se mais que aparência** | parede exposta é onde `OreVein.beside` enxerga minério. Um bolsão mostra **doze** paredes novas onde o corredor mostraria duas |
| **O "aleatório" não é sorteio** | o cursor é um **índice gravado no save**: `Random` daria uma mina diferente a cada carregamento e o cursor apontaria para rocha maciça. É ruído — função pura da boca, do lado da galeria e do número do ciclo |
| **E o ciclo é fixo** | porque `legTowards` percorre até 2.000 posições **todo tique**: uma ordem que precisasse ser acumulada custaria isso ao quadrado. O que varia é de que **lado** o bolsão abre |

| | |
|---|---|
| **Verificações que rodaram** | `./gradlew build` (**612** unitários, 0 falhas) e `runGametest --rerun-tasks` (**232** de jogo, todos passaram) |
| **Testes novos** | 5 de geometria (bolsão, espinha reta, contiguidade, determinismo entre cargas, minas diferentes) e 5 de jogo (vedar, não vedar rocha seca, virar, raro ganha, ordem da raridade) |
| **Fase vermelha** | **não conferida neste ciclo** — são três funcionalidades novas, e os testes delas falhariam por não compilar, não por medir. Os 227 anteriores continuam verdes, que é o que protege o que já existia |
| **O que este ciclo NÃO provou** | nada em jogo. Em especial a **mina antiga do save**: a ordem da galeria mudou de forma, então o cursor gravado aponta para outro lugar. O `findTheFrontier` lê o mundo e recua sozinho, que é exatamente o caso para o qual ele foi escrito — mas isso é raciocínio, e não sessão |

---

### 2026-09-03 — o mineiro percebe que está parado

**O guarda de travamento conta tique de expediente *andando até a pedra*,
e nunca pergunta se o aldeão andou.** Um mineiro travado paga os 2.400
inteiros — dois minutos de expediente — antes de a tarefa voltar para a
fila.

E travado é a assinatura de **toda** sessão registrada. Não "andando
devagar", não "quase lá": parado no mesmo bloco, com destino posto.

```text
he is at 718, 44, 878, walking to 718, 44, 878       (E35)
he is at 756, 44, 878, walking to 758, 44, 878       (folga de chegada)
757, 42, 877, oito leituras seguidas em seis minutos (o poço)
```

É por isso que *"seis vezes a mesma frase, dois minutos de expediente
cada, e zero pedra em dezessete minutos"* — o orçamento inteiro da sessão
cabe em oito desistências.

| | |
|---|---|
| **O detector** | `STILL_LIMIT`, 300 tiques de expediente sem mudar de bloco. **Oito vezes** mais rápido que o guarda de cima, que continua existindo como teto para quem anda sem chegar — oscilar entre dois blocos mexe o contador novo e não escapa do antigo |
| **Por que 300 e não menos** | aldeão para de verdade: porta, outro aldeão na passagem, recálculo de rota. Quinze segundos é folgado para todos esses |
| **De onde veio** | é o `mineBlock` do AnimaFabric — *reports failure if the block is not broken* — aplicado ao passo anterior: **conferir que a ação surtiu efeito**, em vez de esperar o orçamento acabar |
| **E ele não conta fora do expediente** | a mesma armadilha que o contador irmão caiu em 08-26, quando foi de 886 a 2086 com o relatório dizendo `off hours`. Tem teste próprio |
| **A linha do relatório** | ganhou `still N/300` ao lado de `stall N/2400`. Os dois juntos separam as duas frases que a sessão precisa distinguir: *andando devagar* tem `still` perto de zero, *travado* tem os dois subindo juntos |
| **A desistência diz qual dos dois foi** | `gave up the stone at X — it has not moved a block in N ticks of work time` |

| | |
|---|---|
| **Fase vermelha conferida** | sim, na bateria: sem o detector cai `aFrozenMinerGivesUpLongBeforeTheStallGuard`, e só ele |
| **`theStillnessGuardDoesNotCountOutsideWorkHours`** | é guarda-corpo, não fase vermelha — fica verde nos dois lados |
| **Verificações que rodaram** | `./gradlew build` (607 unitários, 0 falhas) e `runGametest --rerun-tasks` (**227** de jogo, todos passaram) |
| **O que este ciclo NÃO provou** | que o mineiro passa a produzir. Ele passa a **falhar rápido e dizer por quê** — o que muda é o custo do erro e a legibilidade do log, não a causa dele |

---

### 2026-09-03 — a guarda de emparedada não valia para o minério

A guarda de 2026-09-02 ensinou o `nextCut` a recusar pedra sem lugar de
onde bater. Ela conferia **a posição do túnel** — e o método devolve
**outro bloco** logo abaixo: o minério colado nela. Esse nunca passou por
conferência nenhuma, e o `followingTheVein`, que roda **antes** do túnel a
cada passagem, também não conferia o dele.

É o pior lugar possível para o vazamento: o minério é o que justifica a
galeria existir.

#### E ele fechava um laço, não só perdia uma passagem

A veia mora no `Mine`, que é da colônia. O `couldNotReach` recua o cursor
do **túnel** e diz por escrito que é *"silencioso quando a pedra não era
do túnel — veio, areia"*. Então um minério inalcançável era servido de
volta na passagem seguinte, ao mesmo mineiro **e ao que herdasse a escada
pelo `MineClaims.stepAside`**:

```text
mira o minério -> anda 2 min de expediente contra a rocha
-> o guarda devolve a tarefa -> a passagem seguinte mira o mesmo minério
```

Sem saída, para a colônia inteira. É a forma exata de *"dezessete minutos,
zero pedra"*.

| | |
|---|---|
| **A guarda de entrada** | `nowhereToStand` passou a ser uma pergunta só, num lugar só, e toda posição que vira alvo passa por ela: a do túnel, o minério colado nela, o minério da veia e o degrau de volta |
| **O quebra-laço** | `couldNotReach` larga a veia quando a pedra largada **é** ela. A guarda de entrada pega quase tudo; esta pega o que não é falta de lugar — chunk descarregado, caminho que a navegação não traçou, o jogador tapando o buraco |
| **Sem minério alcançável** | devolve-se a posição do túnel, que já passou pela guarda, e a veia **não** é lembrada. Aberto o túnel, a passagem seguinte reencontra o minério pelo lado de onde se alcança |

#### E pagar por ela ficou barato

O `approachTo` varria o cubo de raio quatro inteiro — 728 posições, umas
600 leituras de bloco — para ficar com o vizinho mais perto. O javadoc se
defendia dizendo que rodava *uma vez por pedra*. **Deixou de rodar em
09-02**, quando a guarda o chamou de dentro do laço do `nextCut`: até 64
posições por passagem, uma passagem por tique. Até **38 mil leituras por
tique**, e este ciclo ainda estendeu a guarda ao minério.

As posições agora vêm prontas e ordenadas por distância em
`MinerReach.APPROACH_OFFSETS`, e a resposta é a primeira que servir. Num
corredor o vizinho colado responde na primeira leitura; a varredura
inteira só é paga quando a resposta é *não há lugar nenhum* — o caso em
que ela vale.

O bloco devolvido é **o mesmo de antes**, e há teste que afirma isso
refazendo o laço antigo e comparando a lista inteira.

| | |
|---|---|
| **Fase vermelha conferida** | sim, na bateria: sem os dois guardas caem `aVeinWithNowhereToStandIsDropped` e `givingUpOnTheOreDropsTheVein`, e só esses dois |
| **`givingUpOnAnotherStoneKeepsTheVein`** | é guarda-corpo, não fase vermelha: ele fica verde nos dois lados, e existe para impedir que o quebra-laço largue veia boa |
| **Verificações que rodaram** | `./gradlew build` (607 unitários, 0 falhas) e `runGametest --rerun-tasks` (**225** de jogo, todos passaram) |
| **O que este ciclo NÃO provou** | nada disso foi visto em jogo. Continua valendo o parágrafo de cima: os consertos do mineiro estão empilhados sem sessão |

> **As contagens deste arquivo estavam as duas erradas, de novo.** Ele
> dizia 218 de jogo e 565 unitários. O baseline real era **222** de jogo
> (contado por `@GameTest` no `HEAD`), e o `build` com `--rerun-tasks`
> fecha **607** unitários — o 565 era contagem parcial de build
> incremental. Com os três deste ciclo, 225 de jogo.

---

### 2026-09-02, à tarde — duas decisões do autor, e o que cada uma virou

Saíram da auditoria de *estado que sobrevive ao dono*
([`docs/research/estado-que-sobrevive-ao-dono.md`](docs/research/estado-que-sobrevive-ao-dono.md)),
e as duas foram escolhidas pelo autor entre alternativas com custo
diferente.

#### A colônia abandonada parou de pagar a varredura

`runColonyCycles` filtra por `isActive()`, que é **chunk carregado**, e não
por estado. Colônia `ABANDONED` rodava o ciclo inteiro — inclusive o
`ConstructionPlanner`, que carrega a varredura de lote (**teto de 1.024
colunas por passagem**) e o crescimento de rua. Sem vila, nada disso compra
nada, e o orçamento de varredura é justamente o que o §"o que falta ver em
jogo" chama de decisivo.

**Só o planejamento foi cortado, e não o ciclo.** O trabalhador de uma
colônia abandonada continua sendo cuidado — tarefa, depósito, dispensa. A
marca de abandono **oscila** (é o E9), e pular o ciclo inteiro faria o
aldeão trabalhar aos soluços. Pular só o planejamento custa, no pior caso,
um ciclo de atraso para quem foi marcado por engano.

A regra mora no Core (`ColonyAbandonment.plansConstruction`), onde a de
abandono já morava; na camada fabric ficou só a aplicação.

#### E o trabalhador fantasma passou a ser medido, não consertado

A auditoria achou um caminho de perda de dono que **nenhum evento cobre**:
o aldeão que some sem `AFTER_DEATH` nem `MOB_CONVERSION` fica registrado
para sempre, segurando vaga de profissão e reserva de baú, e atravessa o
save.

**Não foi consertado de propósito.** *Ausência não é morte* — podar por
"não achei a entidade" apagaria trabalhador legítimo sempre que o jogador
se afastasse. E o gatilho é raro: aldeão não despawna sozinho, o que foi
conferido no fonte durante o E32.

Entrou `PhantomWorkerLog`, que conta ausências seguidas de trabalhador em
colônia **ativa** e noticia na terceira — uma vez, porque a condição é
permanente. A próxima sessão diz se o fantasma existe de verdade; até lá,
qualquer conserto seria escrito contra suposição.

> A instrumentação do estado órfão não podia virar estado órfão: o que
> sobra nela sai por **invariante** (`retainAll` contra os trabalhadores
> vivos), e não por evento. É a lição da própria auditoria aplicada a ela
> mesma.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **575 unitários, 0 falhas**; `runGametest` → **219 de 219** |
| **Fase vermelha conferida** | em `plansConstruction`. As cinco afirmações do `PhantomWorkerLog` são de regra nova, não de conserto |
| **O que não foi verificado** | sessão de jogo. Nenhuma das duas foi vista rodando, e a segunda **existe justamente para ser vista** |

### 2026-09-02 — a fronteira de conversão passou a ser conferida

Não veio de sessão de jogo: veio de olhar o grafo do projeto. `ColonyPos`
é o nó mais conectado do mod — 389 arestas, ponte entre 51 comunidades —
e a pergunta era se a ADR-005 tinha funcionado ou se ele tinha virado
god object.

**Funcionou, e o susto era falso.** `ColonyPos` é um `record` de três
campos com **um** método; das 389 arestas, 203 são de teste. Grau alto
num tipo de valor é uso, não responsabilidade acumulada. E o `core` não
importa Minecraft em lugar nenhum — o `DependencyRuleTest` já garantia.

**O que estava errado era uma frase.** A ADR-005 §4 e o Javadoc do
`MinecraftTypeAdapter` afirmam que a conversão acontece *apenas* ali.
`MinerReach` tinha um `at(ColonyPos)` privado que refazia o `toBlockPos`
inteiro — a única cópia fora da fronteira, e nada a impedia de virar
duas.

| | |
|---|---|
| `MinerReach.at()` | Passou a delegar a `MinecraftTypeAdapter.toBlockPos`. Comportamento idêntico — o método copiado era linha por linha o mesmo |
| `ConversionBoundaryTest` | 3 casos novos. Proíbe montar um tipo a partir dos três acessores **puros** e do **mesmo** receptor do outro, fora de `fabric.adapter` |

**O que a regra deliberadamente não proíbe.** Chamar
`MinecraftTypeAdapter.toBlockPos` — 23 arquivos fazem, e é para isso que
a fronteira existe. E posição **derivada**: `new BlockPos(origin.x() +
dx, ...)` é aritmética de deslocamento, não conversão, e não há
`toBlockPos` que a substitua. Proibi-la só faria a mesma conta ser
escrita pior.

**O terceiro caso é controle positivo**, e é o que dá valor aos outros
dois: uma regra que passa quando não acha nada fica verde para sempre no
dia em que o detector cega. Esse caso exige que o padrão **ache** a
conversão dentro do próprio adaptador.

#### E o grafo que apontou o defeito quase se estragou sozinho

`graphify update .` — o comando que o `CLAUDE.md` manda rodar após mexer
em código — levava o grafo de **3.167 para 5.422 nós**, e passar de 5.000
derruba o `graph.html` para visão agregada, que perde o detalhe por nó.

**A primeira causa que levantei estava errada.** O `update` reescreve
`graphify-out/.graphify_root` de caminho absoluto para `.`, e eu tomei
isso por causa. Rodar de novo com a raiz restaurada deu **exatamente o
mesmo** 5.422. Correlação, não causa.

**A causa medida, pelo diff dos conjuntos de nós:** dos 2.256 nós
somados, **2.243 vinham de `.claude/skills/`**. Dez eram o código deste
ciclo e dois o `CLAUDE.md`. A exclusão do `.claude/`, escolhida no começo
do build, valia só para aquela execução — o `update` re-detecta a árvore
inteira. O sinal que fecha: ele re-extraiu **4 arquivos** e ainda assim
inflou. O volume vinha da re-detecção, não da re-extração.

Corrigido com um `.graphifyignore` na raiz excluindo `.claude/`. Ele é
lido depois do `.gitignore` e só exclui mais, o que é necessário aqui
porque o `.gitignore` **reabre** `.claude/skills/` para versionar as
skills. Verificado: o `update` seguinte deu **3.179 nós** — os 12 a mais
são o código deste ciclo, que é crescimento legítimo.

**E o `update` deixou de custar os nomes curados.** Ele re-clusteriza e
troca os 169 rótulos escritos à mão por nomes de hub — o `.graphifyignore`
resolveu a inflação, não isto. Até aqui havia duas saídas ruins: restaurar
o backup, que devolve os nomes e joga fora o código novo, ou refazer o
transporte à mão toda vez.

`scripts/graphify_relabel.py` transporta os nomes por **votação**: cada
comunidade nova fica com o nome curado que a maioria dos seus nós já
carregava. Determinístico, sem LLM — a pergunta não pede julgamento quando
a maioria concorda. **143 de 158 decidem sozinhas**; o `--dry-run` lista as
que sobram com amostra de membros, e `scripts/community_names.json` guarda
os 15 nomes escolhidos à mão.

Esse arquivo é chaveado por **id de nó, não de comunidade**. O id de
comunidade só vale para a clusterização que o produziu; o de nó é
determinístico. Chaveado por número, o arquivo passaria a nomear a
comunidade errada em silêncio depois do `update` seguinte.

| defeitos encontrados testando, e corrigidos | |
|---|---|
| `UnicodeEncodeError` no console cp1252 | Morria ao imprimir rótulo com seta (`Tick → Update Colony`), e justamente na lista que o operador precisa ler para nomear o que faltou |
| Apelido pegava o rótulo mais longo | Chamava de `net.minecraft.block.entity.ChestBlockEntity` uma comunidade de `VillageDetector`, `WorkAssignment` e `WorkHours`. Agora prefere tipo do projeto |
| **O relatório anunciava custo zero** | `regenerate_report` fixava `0 input · 0 output` para um grafo que custou **1.112.964 tokens**, e reescrevia esse zero a cada regeneração. Contra a regra do próprio graphify, que manda sempre mostrar o custo. Passou a ler o `cost.json` |

`tests/test_graphify_relabel.py` cobre a decisão com **28 casos**
(`python -m unittest discover -s tests`, stdlib, fora do `gradlew build` de
propósito — build de projeto Java não deve exigir Python). Conferidos por
mutação: revertendo as correções, 9 casos falham.

Nada disso entra no histórico: `graphify-out/` é gitignorado desde
`faa39c0`.

### 2026-08-29, madrugada — duas coisas que sobreviviam ao dono

**Sessão de jogo à 01:19**, e ela rodou com o **jar de 28/08 00:54** — a
troca do arquivo falhou porque o Minecraft estava aberto segurando o
jar, e o `.jar.new` ficou ao lado sem ser aplicado. Confirmado no log:
zero ocorrências de `lit the gallery`, `waiting for the shaft` e
`pieces were laid`, e a frase antiga da barreira.

**Nenhum dos consertos do dia estava rodando.** Ainda assim ela mostrou
dois defeitos que nada tinham a ver com eles.

#### O pastor com picareta

> *"mineiro e pastor segurando picareta"*

Pastor com picareta de diamante é uma vila que mente sobre quem faz o
quê — e ela ficava assim **para sempre**.

O `equip` só preenchia **mão vazia**. Quem esvazia a mão é o `unequip`,
que roda quando o trabalhador perde a função — e ele depende de o aldeão
estar **carregado no mundo**, porque `world.getEntity` devolve nulo em
chunk descarregado e ele sai sem fazer nada. Falhando **uma vez**, a
ferramenta errada nunca mais era corrigida: a colônia recontratava o
aldeão noutra profissão, o `equip` via a mão ocupada, e seguia.

**Uma ponta que só funciona se disparar na hora certa não é
invariante.** A regra passou a ser *a mão combina com a profissão*,
conferida a cada passagem. O que o jogador pôs ali continua onde está —
a colônia só mexe no que ela mesma dá.

#### E os dois rodando no mesmo lugar

> *"os dois rodando no mesmo lugar, bug certamente"*

Era. **O destino sobrevivia ao trabalho que o criou.**

Toda profissão larga o trabalho da mesma forma quando a tarefa deixa de
estar aberta — um `removeIf` sobre o mapa de trabalhos — e **nenhuma
delas soltava o destino junto**. O `WorkTargets` só era limpo na
dispensa do trabalhador e nas desistências de cada trabalho; **tarefa
que termina bem não passa por nenhum dos dois**.

E destino que fica é destino que manda: o `GoToWorkTargetTask` roda
enquanto houver um, e **não expira**. O aldeão passa o resto do
expediente sendo empurrado para o último lugar onde trabalhou — a ovelha
que já foi tosquiada, a pedra que já caiu. De fora, ele fica rodando ali.

Seis profissões consertadas, e a sétima conferida: o fundidor não tem
destino nenhum, funde sem sair do lugar.

> **As duas são a mesma forma de defeito**, e vale nomeá-la: *estado que
> sobrevive ao dono*. Ferramenta que sobrevive à profissão, destino que
> sobrevive à tarefa. Nos dois casos havia uma limpeza — e ela dependia
> de um momento em vez de ser uma invariante conferida.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **558 unitários, 0 falhas**; `runGametest` → **217 de 217** |
| **Fase vermelha conferida** | nos dois. E o teste do destino falhou primeiro por **exceção** e não pela afirmação — `Task.complete` exige `EXECUTING`, e completar uma reservada morria antes de medir. Vermelho que não é o vermelho que se quer não conta |
| **O que a sessão não viu** | nada. Ela rodou o jar de ontem — os consertos do mineiro continuam com **zero sessões** |

### 2026-08-29, à noite — o E35, e o instrumento que o escondia

**Nenhuma sessão nova.** Os dois consertos saíram de reler o log da
sessão de 08-28 com os números na mão.

#### O E35: a perna tinha duas pontas e nada no meio

O segundo mineiro passou a sessão inteira em volta da boca da mina. A
mina daquela colônia veio do save, com a boca em `732, 63, 898`, e as
posições dele estão **dos dois lados** da fronteira da perna:

```text
740, 65, 895  ->  8,77 da boca   FORA da perna  -> mandado à boca
739, 65, 896  ->  7,55 da boca   DENTRO         -> mandado à pedra
741, 63, 898  ->  9,00 da boca   FORA           -> mandado à boca
```

Longe, o destino era a boca; perto da boca, o destino virava **a pedra**
— vinte blocos abaixo, do outro lado da rocha. A navegação não traça
esse caminho, devolve caminho parcial, ele deriva, sai dos oito blocos, e
o destino volta a ser a boca. Para sempre.

**A descida tem vinte blocos e a perna tem oito: são três passos, e o
sistema só sabia dar dois.**

Agora quem dá o passo é a **ordem de cavar**. Ela é um corredor contínuo
a partir da boca — tudo o que vem antes da frente já está aberto —, e o
passo seguinte é o ponto mais avançado dela que ainda caiba numa perna,
contado de onde ele está. Um degrau de cada vez, e sem fronteira nenhuma
para oscilar em volta.

> **A busca é contígua de propósito.** A ordem dobra: a escada desce para
> um lado, a sala se abre, o segundo lance vira, a galeria corre para
> outro. Um ponto avançado pode passar **perto** dele por fora da rocha
> sem que haja caminho — pegar *"o último que estiver a oito blocos"*
> mandaria o aldeão atravessar parede.

**E um teste afirmava exatamente o defeito.** O
`atTheMouthHeAimsForTheStone` foi escrito em 08-28 para impedir que ele
ficasse parado na entrada. A intenção estava certa e a afirmação, errada.

#### O relatório dizia para onde ele deveria ir, não para onde foi

E foi ele que quase impediu o diagnóstico acima. A linha **recomputava**
o destino — chamava `approachTo` de novo na hora de escrever — em vez de
ler o que o aldeão recebeu. Enquanto os dois coincidem ninguém percebe;
eles deixam de coincidir exatamente quando a perna manda o mineiro à
boca, que é o caso do E35.

É a terceira vez que este projeto paga pela mesma coisa — E30, E31, e
agora esta. **Instrumento que reporta o que recalculou, e não o que
aconteceu.** Sai mais barato junto: `approachTo` são umas seiscentas
leituras de bloco por mineiro por ciclo, gastas para reimprimir um dado
que já estava guardado.

A frase de desistência também separou duas perguntas que misturava: *para
onde ele foi mandado* é o destino da task, e *onde haveria de ficar de
pé* é o `approachTo`. Ela imprimia a segunda como se fosse a primeira.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **558 unitários, 0 falhas**; `runGametest` → **213 de 213** |
| **Fase vermelha conferida** | nos dois. Recolocada a regra de duas pontas, cai só o teste da boca; e o conserto do relatório derrubou `aStoneWithNowhereToStandSaysSo`, que é o teste que guarda a segunda pergunta |
| **O que continua sem prova** | sessão. O mineiro tem agora **três** consertos empilhados sem uma única sessão que os veja: a folga de chegada, a reserva de mina e a perna pela ordem de cavar |

### 2026-08-29 — a sessão que respondeu quatro perguntas

**Sessão de jogo em 2026-08-28, 23:06 às 23:20.** A casa subiu — 149
planejados, **127 assentados** —, a mina abriu, e os dois mineiros
travaram. O autor entrou e olhou, e o que ele viu tinha causa exata em
três dos quatro casos.

#### O mineiro descia, e parava dois blocos antes de chegar

**É a nona tentativa no mesmo sintoma, e a primeira com a conta
fechada.** Pela primeira vez o log pegou um mineiro **dentro** da mina —
y=44 é a galeria:

```text
digging Pedra at 760, 44, 878, 4,2 blocks away
  (out of reach, he is at 756, 44, 878, walking to 758, 44, 878),
  0/0 ticks, stall 2140/2400
```

**Exatamente dois blocos do destino**, parado seiscentos tiques. Dois é
o `COMPLETION_RANGE` do `GoToWorkTargetTask`: a navegação **se deu por
chegada** e parou, o mod continuou dizendo *"fora de alcance"*, e ele
moeu os últimos dois blocos até o guarda devolver a tarefa. É o
*"rodando na escada e não desce"* que o autor descreveu.

**Duas contas certas que não compunham.** O `approachTo` escolhe um
lugar **dentro** do braço — 758 está a 2,0 da pedra, e o braço é 4. O
caminhante parava até dois **antes** desse lugar. Somadas: 4,2, fora do
braço para sempre, sem que nenhuma das duas estivesse errada sozinha.

A folga de dois foi escrita para o **lenhador**, e ali ela é certa: o
destino dele *é* a árvore, e parar dois antes é parar dentro do alcance.
Para o mineiro o destino já é o lugar exato de ficar de pé.

| | |
|---|---|
| **O conserto** | a folga deixou de ser constante da task e passou a ser **do destino**. Padrão continua dois; o mineiro pede um |
| **Por que um e não zero** | exigir o bloco exato faria a navegação perseguir uma casa decimal, e o guarda devolveria a tarefa por outro motivo. Um deixa o pior caso em 3,0 de uma pedra a 4 |
| **Fase vermelha** | com a folga em dois, `arrivingAtTheEdgeOfTheWalkStillReachesTheStone` cai |

#### O bloco central do chão era um encaixe do gerador

A frase do autor foi *"falta um bloco central no chão"*, e ela é exata.
O piso da casa de planície é um quadrado de nove tábuas, e a **do meio**
é um `jigsaw` no arquivo do Vanilla:

```text
camada y=0, vista de cima

  cobblestone  cobblestone  cobblestone
  cobblestone  <JIGSAW>     cobblestone      <- o buraco
  cobblestone  cobblestone  cobblestone
```

O leitor tratava encaixe como **andaime do gerador** e o descartava
junto com o ar. Mas encaixe não é andaime: bloco de estrutura não vira
nada e o jogo o apaga, enquanto o encaixe carrega no próprio arquivo o
`final_state` — o bloco em que ele se transforma quando a vila é gerada.
Do encaixe do meio sai `oak_planks`; do que fica na porta, o **degrau da
entrada**, que também faltava.

**E o conserto desenterrou um teste que passava por acidente.** O
`aSavedProjectComesBackFromTheWorld` punha de pé o primeiro bloco da
planta **lida do arquivo** e esperava que a obra retomada o
reconhecesse — mas a obra que volta do save é **girada para a rua**
antes de medir o mundo. Enquanto a caixa da casa era quadrada, o
primeiro bloco caía sobre si mesmo ao girar e era do mesmo material, e a
conta fechava. O degrau da entrada alargou a caixa num eixo só, o giro
deixou de ser inócuo, e o acidente apareceu — que é literalmente o que o
javadoc de `blueprintOf` avisava que aconteceria.

#### A cama, e a Regra 32

*"Aparece somente a metade da cama e na direção errada."* O log diz por
quê, na letra:

```text
Could not finish the two-part block at 769, 64, 935
    — Block{minecraft:cobblestone} is in the way
```

A planta guarda o **nome** do bloco e não o estado (ADR-005), então a
cama saía no padrão, que olha para o norte. No arquivo ela olha para
**leste**. Na casa de planície o norte da cama é a parede: a cabeceira
não coube, e sobrou meia cama.

O autor pediu a regra junto com o defeito — *"criar uma regra para
adicionar os móveis e cama depois da casa pronta"* —, e ela é a
**Regra 32**, enunciada em `Project-State.md`. Ela resolve os dois de
uma vez: as **três tochas de parede** riscadas com `nothing holds it`
vinham antes da parede que as segura, e a cama era decidida contra um
pedregulho que ainda não estava lá.

Com a casa de pé, a cama passou a **perguntar ao mundo** para que lado
cabe — encostada na parede quando dá. E a Regra 17 passou a deixar a
cama em paz: o javadoc dela sempre disse que a cama ficava de fora, e
não ficava.

> **O que isto não faz** é orientação fiel ao arquivo. A cama da planta
> olha para leste e a que sobe olha para onde couber. Isso é a
> **ADR-008**, decidida e por escrever, e vale para o tronco e o degrau
> junto.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **556 unitários, 0 falhas**; `runGametest` → **211 de 211** |
| **Fase vermelha conferida** | nos quatro consertos |
| **O que a sessão não respondeu** | nada disto foi visto em jogo depois do conserto. São quatro causas com prova de código e **zero sessões** |

### 2026-08-28, à noite — dois relatórios que afirmavam o que não mediram

**Nenhuma sessão de jogo neste ciclo.** É trabalho de bateria, e os dois
achados são da mesma família: uma linha de log que conclui mais do que
os dados dela sustentam.

#### O E31 — a barreira absolvia a Regra 28 sem ter medido nada

Sessão de 2026-08-26, 23:06: zero obras, zero projetos, nenhum bloco
assentado — a vila passou os dois minutos varrendo. E o servidor parou
dizendo `TEST BARRIER covered for nothing this session — every piece
came from the colony's own chests. Rule 28 can go.`

A frase é uma **conclusão sobre a Regra 28**, e a soma que a sustentava
só sabia contar o que **foi riscado**. Numa sessão sem obra a barreira
não é exercitada uma vez: o silêncio dela não é notícia boa, é ausência
de notícia — e estava marcada como a notícia boa neste arquivo, que é
onde a mentira custava.

Nasceu a conta do outro lado. O veredito tem três estados:

| | |
|---|---|
| `NOTHING_BUILT` | nada subiu — a barreira não teve o que medir, e não absolve ninguém |
| `COVERED_FOR_NOTHING` | subiu, e nada precisou ser riscado. A única forma da notícia boa |
| `COVERED` | a barreira trabalhou, e a lista diz em quê |

A chamada sai da **única** passagem de `placeOne` em que uma peça encosta
no mundo; as outras quatro riscam o bloco, e riscado não é assentado.

#### Um mineiro por mina — a pendência do Nível 1

Sessão de 2026-08-26, 23:23:08: os dois mineiros deram `could not reach
the stone` **no mesmo tique**. Havia reserva, e ela era da **tarefa** —
o `ColonyCycle` abre uma por recurso pedido, pedregulho e carvão são
dois, e nada na cadeia falava da mina.

Mas a mina é uma coisa só: o cursor da galeria mora no `Mine`, e os dois
recebiam **a mesma posição na mesma passagem**. Pior que trabalho
perdido — `could not reach` recua o cursor, e ele recuava duas vezes por
um bloco.

É o que o [`TreeClaims`](src/main/java/com/villagecolony/fabric/work/TreeClaims.java)
resolveu do lado do lenhador, e a resposta é a mesma: **a coisa disputada
é que tem dono**. Ali é a árvore, aqui é a mina. Quem não é o dono sai de
`nextTarget` sem alvo, volta a perguntar na passagem seguinte, e herda a
mina no ciclo em que o dono largar o trabalho.

**A reserva não vaza, e é por construção.** Nem todo fim de trabalho
passa pelo mesmo lugar — morte, zumbificação, dispensa, tarefa devolvida
pelo guarda —, e mina trancada por um aldeão que já não existe é pior
que o defeito que ela conserta. A conferência roda a cada ciclo contra
os trabalhos abertos.

**O que ela não reserva** é a pedra de superfície: aquela busca é por
mineiro, cada um com seu cursor de espiral. O que é um só é a escada.

#### E o relatório do mineiro parou de dizer que procura quando está barrado

A linha de quem não tem alvo era `looking for stone` nos dois casos.
Uma sessão inteira do segundo mineiro "procurando" mandaria o autor
investigar a busca, que está certa. Agora ela diz
`waiting for the shaft — <id> is in it`.

#### E a galeria passou a acender

Só a boca tinha lanterna, e **nem sempre**: a sessão de 08-26 saiu com
`lantern at nowhere it fits`. Vinte blocos abaixo dela a escada e a
galeria ficam com **luz zero**, que é a condição exata de criatura
nascer — dentro da mina, ao lado de um aldeão desarmado.

Uma tocha por passagem, de oito em oito posições da ordem de cavar, e só
no que já está aberto: fora disso seria obra, e obra é do construtor.
Não custa material, pela mesma razão que a mobília da boca — cobrar
tocha faria a mina ficar escura até a colônia ter carvão, e carvão vem
da mina.

**A bateria cobrou três coisas, e as três eram de verdade:**

| | O que quebrou | O que ela ensinou |
|---|---|---|
| A tocha ia na posição da ordem, que na escada é o **bloco dos pés** | o E33 falhou com *"o primeiro degrau da escada continua fechado"* | numa escada de um bloco de largura **todo chão é caminho**. A tocha subiu para o teto da passagem, e virou tocha de parede porque no alto não há em que se apoiar de baixo |
| O cursor conta posição **entregue**, não bloco **cavado** | o E33 falhou de novo, agora com *"o degrau saiu sem altura para o aldeão passar"* — a camada da cabeça virou tocha antes de a terceira ser cavada | coluna aberta pela metade **parece** ter teto. A luz passou a ficar um espaçamento inteiro atrás do cursor |
| A subida parava **embaixo** da tocha que já estava lá | a mesma coluna acendia duas vezes, uma no teto e outra na cabeça | tocha não é ar, e "tem ar embaixo" não bastava. A subida passou a reconhecer luz e desistir |

**E o mineiro não cava a própria luz** — a lição do lampião do primeiro
degrau, de 08-27. Uma posição com luz é **espaço aberto**, e não rocha:
sem isso o `findTheFrontier` recuaria até a tocha toda passagem, e a mina
nunca mais passaria dali. A pergunta é feita ao jogo e não a uma lista de
nomes — *acende e não fecha a passagem* —, então magma e pedra luminosa
são bloco sólido e continuam sendo pedra a cavar.

#### Limpeza do caminho

`MinerWork.approachTo` tinha **dois javadocs em sequência**, e o
primeiro descrevia a busca por ordem de faces que a busca por distância
aposentou em 08-27 — documentação que contradizia o código. Junto saiu
`APPROACHES`, a constante daquela ordem, declarada e **lida por
ninguém**.

| | |
|---|---|
| **Fase vermelha conferida** | nos três, em rodadas separadas. Sem `laidOne`, a parede sobe e o teste de jogo cai; sem a reserva, os dois testes de mina caem; sem o `isLight` caem os dois da tocha, e sem a chamada cai o da galeria acesa |
| **Verificado rodando** | `gradlew build` → **549 unitários, 0 falhas**; `runGametest` → **208 de 208** |
| **O que este ciclo não provou** | nada disso foi visto em jogo. São dois relatórios honestos e uma escada com dono — o mineiro continua sem ter sido visto cavando |

### 2026-08-28 — o E33 fechou na bateria: faltava a arena ser uma mina

**O teste que faltava, e é por isso que a bateria ficava verde com o
jogo quebrado.** Todos os testes do mineiro montavam um **piso de terra
plano** e plantavam uma pedra nele:

```java
private static void ground(TestContext context) {
    for (int x = 0; x <= 7; x++)
        for (int z = 0; z <= 7; z++)
            setBlockState(new BlockPos(x, 1, z), DIRT);   // um piso, e só
}
```

Numa arena assim **não há escada, não há teto, não há degrau diagonal e
não há frente de galeria** — nada do que o mundo de verdade tem, e nada
do que quebrou sete sessões. Todo defeito destes dois dias vivia
exatamente no que a arena não tinha.

**Três testes novos, em rocha maciça:**

| | |
|---|---|
| `theMinerDigsTheStaircaseThroughSolidRock` | ele tira os blocos do primeiro degrau, **com a altura de passagem**, e a pedra chega ao baú |
| `theMinerWalksDownTheStaircaseAsItDigs` | o degrau 4 fica a **4,7 blocos da boca**, fora do braço de quatro. Se sai, ele andou escada abaixo |
| `aMineWhoseCursorRanAheadDigsAgain` | a mina entra com a fronteira do save adiantada e **nada aberto** — a forma exata do defeito em jogo — e se conserta sozinha |

**Os testes mordem, e isso foi conferido.** Desligando o
`findTheFrontier`, `aMineWhoseCursorRanAheadDigsAgain` cai. A bateria
rodou **três vezes seguidas** com 202 de 202, porque teste de tempo
mente numa rodada só.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **530 unitários, 0 falhas**; `runGametest` → **202 de 202, três rodadas** |
| **O que ainda não é prova** | a arena tem oito blocos e a mina de verdade desce vinte. O que se provou é que **a mecânica funciona numa escada real**; a distância continua sem teste |
| **Estado do E33** | ✅ fechado na bateria · 🔒 **falta a sessão de jogo** |

### 2026-08-28 — a frente da galeria passou a ser lida do mundo

**A perna funcionou, e o log mostra:** de `734, 66, 878` na superfície
para `725, 57, 898` — que é, exatamente, o **degrau 7** da escada. Ele
entrou na mina pela primeira vez.

**E parou lá, mirando isto:**

```text
the miner is at 725, 57, 898, 24,5 blocks away;
the stone at 732, 45, 878 is Lanterna
```

**Uma lanterna.** O autor tinha cavado à mão até a galeria dias antes —
*"tive que cavar até lá"* — e iluminado o próprio túnel. Aquele buraco
não se liga à escada do mod por lugar nenhum.

**O recuo de ontem parava cedo demais.** Ele voltava até achar uma
posição de onde desse para bater, e o túnel do jogador oferece
exatamente isso. Os dois mineiros ficaram no fim da escada de verdade
mirando um bolsão do outro lado da rocha.

**A frente de verdade é a primeira posição ainda fechada na ordem de
cavar.** Ela é conectada por construção — tudo o que vem antes já está
aberto, e a ordem é um caminho contínuo a partir da boca. Nenhum buraco
solto engana a conta.

Lida do mundo, e não lembrada: a mesma escolha que o baú da boca e a
marca do baú já faziam. **O número gravado no save deixa de poder
mentir.**

| | |
|---|---|
| **Fase vermelha** | `theFrontierIsWhereverTheCursorIsToldToGo`, `theFrontierStaysInsideTheOrder` |
| **Um teste antigo virou do avesso, e por verdade** | `theMineTheSaveBroughtIsNotDugAgain` afirmava que a fronteira gravada era **obedecida** — e foi isso que quebrou a mina do autor. Passou a afirmar que ela é **conferida**. Não custa: o que já está aberto é pulado com uma leitura de bloco, não com uma picareta |
| **Verificado rodando** | `gradlew build` → **530 unitários, 0 falhas**; `runGametest` → **199 de 199** |
| **Ainda não visto em jogo** | um bloco cavado. Sete sessões |

### 2026-08-28 — ele estava na superfície, e agora entra pela boca

**O instrumento respondeu inteiro**, e foi a primeira vez que se soube
onde ele estava:

```text
the miner is at 734, 66, 878, 20,5 blocks away;
it was walking to 732, 46, 878, which is not standable;
the stone at 735, 45, 878 is Pedra
```

**Y 66 é a superfície.** Ele estava vinte e um blocos em linha reta
**acima** da galeria, em cima do chão, mirando uma pedra no fundo da
mina. A navegação recebe um destino a vinte blocos atravessando rocha
maciça, devolve caminho parcial, e ele estaciona no ponto mais próximo
que consegue — bem ali em cima.

É o sintoma do MineColonies palavra por palavra, e o remendo do jogador
é o mesmo que o autor fez: cavar até lá.

**A perna que faltava.** Não se pede à navegação um caminho que ela não
sabe traçar: pede-se a **boca da mina**, que fica na superfície e a que
se chega andando. De dentro dela a escada é um corredor. Chegando à
boca, o destino passa a ser a pedra — sem essa segunda metade ele
trocaria um travamento por outro, parado na entrada para sempre.

### 2026-08-28 — e havia duas definições de "cabe um aldeão aqui"

A mesma linha pegou as duas discordando: **quem escolheu o lugar achou
que cabia, quem relatou achou que não.** A do mineiro pedia *qualquer
coisa que não fosse ar* embaixo — água, lava, folha servem —, e a do
construtor pede **chão sólido**.

É a falha que a distância já tinha tido anteontem: duas contas para a
mesma pergunta, e o log podendo contradizer a decisão. Uma conta só, e é
a do construtor.

| | |
|---|---|
| **Fase vermelha** | `theChosenSpotHoldsAVillager` — *"escolheu o bolsão sobre água"*; `MinerLegTest` inteiro |
| **Confirmado em jogo** | o recuo da galeria funcionou: `The gallery backs up from 736, 44, 878 — there is nowhere to stand to dig it` |
| **Verificado rodando** | `gradlew build` → **528 unitários, 0 falhas**; `runGametest` → **199 de 199** |
| **Ainda não visto em jogo** | um bloco cavado. Seis sessões, zero |
| **O fazendeiro** | a corrente está viva e relatando — *"no ripe crop within 32 blocks"*. Não há lavoura madura na vila agora; não é defeito, é o mundo |

### 2026-08-27 — o fazendeiro era só uma etiqueta, e agora as sete trabalham

**Das sete profissões, era a única sem trabalho.** A colônia lhe dava
enxada, baú e placa com o nome, e nunca mais falava com ele — lenhador,
mineiro, pastor, fundidor, fabricante e construtor buscam e guardam
desde a Fase 10; ele ficava parado no meio deles.

**Faltava a corrente inteira**, e não só o comportamento:

```text
ResourceType.WHEAT/CARROT/POTATO/BEETROOT   não existiam
Production.FARMED                            não existia
ResourceGroup.CROPS                          não existia
TaskType.COLLECT_FOOD                        não existia
a meta de comida                             não existia
FarmerWork                                   não existia
```

A `Capability.MAINTAIN_FOOD` existia desde a Fase 7 **e nenhuma tarefa a
pedia** — capacidade sem tarefa é um aldeão com enxada e sem lavoura.

**O que ele faz:** acha a lavoura madura mais perto do centro da vila,
anda até ela, colhe, **replanta com a semente da própria colheita** e
guarda o resto no seu baú. É a Regra 7 do lenhador aplicada onde ela
nasceu — colher sem replantar deixaria a vila com um campo de terra
arada vazia e uma refeição só.

**Quem diz se está madura é o bloco**, não uma lista de nomes:
`CropBlock.isMature` vale para as quatro do jogo e para o que um
datapack plantar depois. Mesmo caminho que o minério tomou de manhã.

**Um defeito que só o teste de ponta a ponta pegaria.** O fazendeiro
colhia, replantava, guardava no baú — e a colônia lia **zero**. O
`MinecraftTypeAdapter` não sabia nomear trigo, e o estoque só conta o
que ele nomeia: a meta de comida nunca cairia, e o trabalho aconteceria
para sempre sem valer nada.

| | |
|---|---|
| **Fase vermelha** | `FarmerChainTest` (núcleo) e `FarmerGameTest` (mundo), os dois inteiros sem compilar |
| **Verificado rodando** | `gradlew build` → **524 unitários, 0 falhas**; `runGametest` → **198 de 198** |
| **Ainda não visto em jogo** | o fazendeiro colhendo numa vila de verdade |
| **Limite assumido** | ele colhe o que **já** está plantado. Não ara terra nova nem planta em campo vazio — é o "básico" que o autor pediu, e arar é o ciclo seguinte |

### 2026-08-27 — o degrau seguinte é diagonal, e a busca só olhava faces

**A causa das cinco sessões sem cavar**, e ela estava na geometria da
própria Regra 29. Um degrau anda um para a frente e um para baixo:

```text
posição 0 (degrau 1)   (1, 64, 0)   onde ele está de pé
posição 3 (degrau 2)   (2, 63, 0)   o alvo — DIAGONAL
```

Os dois não encostam por **nenhuma face**. O `approachTo` olhava as seis
faces e, desde a manhã, um bloco abaixo de cada uma — nenhuma alcança
uma diagonal. Ele caía no *"fica a própria pedra"* já no **segundo
degrau** e mandava o aldeão para dentro da rocha, que a navegação não
cumpre.

**E o aldeão alcançava o tempo todo.** De pé no degrau 1 ele está a
**1,1 bloco** do centro do degrau 2, e o braço dele é 4. O lugar
existia; a busca é que não sabia procurá-lo.

**Explica por que algumas sessões cavaram e outras não.** A galeria é
reta, e blocos consecutivos dela *encostam* — os onze blocos da sessão
das 22:23 foram todos de galeria. A escada e a frente do túnel nunca
saíram.

A busca passou a ser por **distância** dentro do alcance, e não por
ordem de face. Custa umas seiscentas leituras de bloco, e por isso o
resultado é guardado no `Job`: uma vez por pedra, e não a cada tique
enquanto ele caminha.

### 2026-08-27 — e a galeria aprendeu a recuar

O conserto acima não bastaria sozinho para a mina **que já existe**: o
cursor marchou dezenas de blocos por dentro da rocha antes de a marcha
ser consertada, e essa posição está gravada no save. De lá nada é
alcançável, e a mina ficaria presa para sempre.

Recuar funciona porque a ordem de cavar é um caminho **para fora da
boca**: a posição anterior está sempre mais perto do que já está aberto.
Para na primeira de onde dá para bater — e para também no ar, que é o
que impede o vaivém.

### 2026-08-27 — a pesquisa, e o que ela mostrou

O autor pediu para procurar projetos de aldeão que ajudassem a entender.
O **MineColonies** tem o mesmo sintoma registrado, com as mesmas
palavras — [issue #4297](https://github.com/ldtteam/minecolonies/issues/4297):

> *o mineiro fica parado na superfície acima do alvo, numa mina grande;
> sem bloqueio e sem falta de item. Cavar direto para baixo à mão
> resolve até ele precisar voltar.*

É a mesma coisa que o autor fez — *"tive que cavar até lá"*. E a resposta
deles foi **trocar a navegação inteira** por um A\* próprio, em pool de
threads, com cache de chunks. Cedo demais para este projeto; o que se
aproveita é a disciplina que aquilo impõe, e que aqui faltava: **nunca
mandar o trabalhador para um lugar de onde ele não consegue trabalhar.**

| | |
|---|---|
| **Fase vermelha** | `theNextStairStepIsReachedFromTheOneBefore` — *"o destino virou a própria pedra"*; `theGalleryCanBackUpToWhereItReallyEnds` |
| **Dois falsos vermelhos no caminho** | a arena de teste tinha superfície e chão dentro do alcance de 4, e a busca nova os achava — **e estava certa em achar**. A rocha do teste ficou funda e larga o bastante para a premissa ser verdadeira |
| **Verificado rodando** | `gradlew build` → **519 unitários, 0 falhas**; `runGametest` → **194 de 194** |
| **Sem teste de jogo** | o recuo da galeria. Um poço de vinte blocos não cabe numa arena de oito; o mecanismo (`Mine.backUp`) tem teste de unidade, o laço que lê o mundo não |

### 2026-08-27 — o mineiro anda, e a linha de cada ciclo passou a dizer onde ele está

**O fato novo da sessão das 23:18**, e ele derruba o que eu vinha
supondo:

```text
23:20:24   93,9 blocks away   (off hours)
23:20:54   30,4 blocks away   (work time)
23:21:24   20,5 blocks away   (work time)
```

**Ele anda.** Noventa e quatro para vinte em um minuto de expediente. As
sessões anteriores mostravam distância congelada e eu li isso como
"navegação não funciona" — funciona, e por dezenas de blocos. O que as
outras sessões mostravam era ele **parado depois de chegar perto**, não
incapaz de sair do lugar.

**E a sessão passou sem responder nada**, por um defeito do instrumento:
onde ele está e para onde foi mandado saíam só na frase de
**desistência**, e ela sai depois de 2400 tiques de expediente. A sessão
durou três minutos, o guarda parou em 1177, e a única linha capaz de
responder nunca chegou a ser escrita.

O estado que interessa é o do **travamento**, não o do fim dele. As duas
informações passaram para a linha de cada ciclo, e só quando está fora
de alcance — linha curta quando está tudo bem.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **517 unitários, 0 falhas**; `runGametest` → **192 de 192** |
| **O número que se repete** | **21,5** — dois mineiros, duas sessões diferentes, param exatamente aí. Não é coincidência de distância, é um lugar |

### 2026-08-27 — o cursor da galeria marchava por dentro da rocha

**Quem fechou o caso foi o autor, a pé.** A frase dele: *"fui olhar,
tive que cavar até lá e não tinha nada, era um minério"*. O mod dizia
estar abrindo a galeria havia três sessões, e no mundo estava **rocha
maciça**.

```java
public ColonyPos nextPosition() {
    return shaft.positionAt(cut++);   // avança SEMPRE
}
```

Quando o mineiro não conseguia chegar na pedra, a tarefa voltava para a
fila — e a posição ficava para trás. O cursor marchava pela ordem de
cavar, coluna após coluna, enquanto o túnel continuava fechado. Daí os
alvos avançarem (`731,45 → 732,45 → 733,44`) com **zero** blocos
cavados, e daí metade das colunas "sumirem": elas nunca foram puladas
por serem ar, foram puladas por terem sido abandonadas.

**O conserto já existia no arquivo.** O `holdPosition` faz exatamente
isto quando a picareta desvia para o minério, e o comentário dele
descreve o sintoma do autor palavra por palavra — *"o túnel ficaria com
um bloco no meio para sempre"*. Ninguém o chamava na desistência.

A posição vai por parâmetro porque a mina é da colônia e **dois mineiros
a partilham**: desandar às cegas devolveria o cursor por cima do bloco
que o outro acabou de pegar.

| | |
|---|---|
| **Fase vermelha** | `aStoneThatCouldNotBeReachedIsOfferedAgain`, `onlyTheLastHandedOutPositionRollsBack` |
| **Verificado rodando** | `gradlew build` → **517 unitários, 0 falhas**; `runGametest` → **191 de 191** |
| **O que isto NÃO resolve** | o mineiro continua sem conseguir chegar na pedra. Este conserto impede o mod de **mentir** sobre ter cavado; ele não faz o aldeão andar. A galeria vai insistir no mesmo bloco em vez de fingir que passou por ele |

### 2026-08-27 — três sessões investigando por que o mineiro não anda

**Terceira sessão seguida sem um bloco cavado.** O conserto do
`approachTo` — que era um defeito real, e continua consertado — **não
resolveu**:

```text
digging Pedra at 732, 45, 878, 21,5 blocks away (out of reach), stall 2399/2400
digging Pedra at 733, 44, 878, 22,5 blocks away (out of reach), stall 2398/2400
```

Distâncias congeladas, `0/0 ticks` (o `mine` nunca foi chamado nem uma
vez), guarda subindo a taxa cheia em horário de trabalho. **Ele não
anda.**

**O que ficou sabido**, e vale registrar porque estreita a busca:

- não é horário — o guarda só conta em expediente e conta a taxa cheia;
- não é o alcance — as distâncias são 21 e 22, não 4,x;
- **metade da galeria está sendo pulada como já aberta.** Os alvos
  avançam de coluna em coluna sem nada ser cavado, e o `nextCut` só pula
  o que é ar ou fluido. Ou a galeria entrou numa caverna, ou está
  alagada.

**Distância sozinha não escolhe** entre aldeão longe demais para a
navegação, destino que a navegação não cumpre, túnel alagado, e aldeão
do outro lado de uma parede. As quatro têm correções diferentes, e a
frase de desistência passou a dizer as três coisas que escolhem: onde
ele está, para onde foi mandado, e o que há lá.

Molde do `BuilderApproach.whyNotReached`, que existe pela mesma razão do
lado do construtor desde 08-22.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **515 unitários, 0 falhas**; `runGametest` → **191 de 191** |
| **A frase que decide** | *"the stone itself (no free neighbour to stand on)"* — quer dizer que o aldeão foi mandado para dentro da rocha. Se aparecer, o `approachTo` ainda tem buraco; se não, o problema é navegação ou terreno |

## ✅ Resolvido

### 2026-08-27 — o bloco de cima da galeria não tinha onde se ficar de pé

**A sessão das 22:38 não cavou um bloco**, e o relatório consertado
apontou o lugar no primeiro uso:

```text
digging Pedra at 729, 45, 878, 7,9 blocks away (out of reach), stall 1938/2400
```

**7,9 congelado em oito relatórios seguidos.** Ele não andava — e o
número parado, que a versão anterior não sabia mostrar, é o que
transformou "aparentemente travou" em endereço.

**A geometria.** A galeria é de dois de altura, e o alvo era o bloco
**de cima** da coluna da frente. O `approachTo` olhava só as seis faces:

```text
atrás, mesma altura   ar, mas o teto acima é pedra — não se fica de pé
embaixo               ar, mas o de cima é o próprio alvo, maciço
os outros quatro      rocha
```

Nenhuma servia, e o método caía no *"fica a própria pedra"* — mandar o
aldeão para dentro da rocha, que a navegação não cumpre. Ele ficava onde
estava até o guarda devolver a tarefa. **Todo bloco de cima da galeria
caía nisso**, ou seja metade dela.

O lugar existia o tempo todo: **atrás e um abaixo**, o chão do túnel, a
um metro e oito do alvo. Diagonal, e por isso invisível para as seis
faces. Agora cada face é tentada também um bloco abaixo.

| | |
|---|---|
| **Fase vermelha** | `theTopBlockOfTheGalleryHasSomewhereToStand` — *"o destino virou a própria pedra"* |
| **Um falso verde no caminho** | a primeira arena do teste não tinha teto, e o `approachTo` achava lugar **em cima** da coluna — passava sem provar nada. Numa mina de verdade aquilo é rocha. A arena virou rocha maciça com um túnel cavado nela |
| **Verificado rodando** | `gradlew build` → **515 unitários, 0 falhas**; `runGametest` → **189 de 189** |
| **Ainda não visto em jogo** | a galeria andando sem travar. É a próxima sessão |

### 2026-08-27 — o relatório do mineiro media uma coisa e o alcance media outra

**A sessão das 22:19.** A galeria andou cinco colunas em quatro segundos
— `722,44,878` a `727,45,878`, dois blocos por coluna, exatamente o
túnel de dois de altura — e então os dois mineiros travaram:

```text
digging Pedra at 728, 44, 878, 4 blocks away, 5/6 ticks, stall 2219/2400
```

Quatro blocos, e o alcance é quatro: parecia que ele estava no lugar
certo e não batia. **As duas frases mediam coisas diferentes.** O
relatório usava `getBlockPos()`, que é inteiro, e ainda truncava a raiz;
o alcance usa a posição real do aldeão. Qualquer distância entre 4,0 e
4,99 aparecia como *"4 blocks away"* e estava **fora** de alcance.

Instrumento que mente é pior que instrumento nenhum: aquele mandou
procurar o defeito onde ele não estava. Agora é uma conta só, com uma
casa decimal e `(out of reach)` por extenso — parado perto e parado
longe têm correções diferentes.

A geometria saiu para `MinerReach`, classe própria: `MinerWork` não
carrega fora do jogo, e três subtrações e uma raiz se afirmam sem subir
servidor.

### 2026-08-27 — todo tipo de minério, perguntado ao jogo

Decisão do autor: *"ele deve minerar todo tipo de minério"*. Havia uma
lista de dezesseis nomes escrita no `OreVein`, e ela era a regra de ouro
da ADR-009 sendo desobedecida — cada minério novo pedia uma linha, e até
alguém escrevê-la o mineiro passava por cima dele como se fosse pedra.

Quem responde agora é `c:ores`, a etiqueta que o próprio jogo mantém —
mesmo caminho da Regra 27.

| | |
|---|---|
| **Fase vermelha** | `MinerReachTest` inteiro (18 erros de compilação); `everyKindOfOreCounts` |
| **Verificado rodando** | `gradlew build` → **515 unitários, 0 falhas**; `runGametest` → **187 de 187** |
| **O que a troca de etiqueta NÃO conserta** | a lista antiga já cobria todo minério do Overworld em 1.21. Quem ganha é mundo com datapack e a versão seguinte — **não a sessão de hoje** |
| **E o travamento continua aberto** | saber que ele está a 4,7 e não a 4 não diz *por que* ele não anda os 0,7 que faltam. É a próxima sessão que responde |

### 2026-08-27 — o veio que desce abre o degrau antes

Decisão do autor, e a frase dele: *"o mineiro deve sempre manter um
local que consiga escapar para voltar, ou que destrua bloco para poder
subir"*. Escolhida a segunda — abrir o bloco.

**A escada já era subível; o veio não.** A Regra 29 abre três blocos por
degrau desde 08-27, e sobe-se por ela na mesma geometria em que se
desce. O veio não tem geometria: `OreVein.beside` olha as seis faces, e
**a de baixo é a primeira da lista**. Minério empilhado abre um poço de
um bloco de largura, e de poço não se sobe — o aldeão não pula dois.

**Qual bloco falta é sempre o mesmo:** o teto do nível de onde ele veio.
Subir um degrau pede dois blocos de ar no destino; o de baixo já é o
minério recém-tirado, o de cima é este. Aberto ele, a subida se faz um
degrau de cada vez até a boca do poço.

Degrau que não se abre — bedrock, lava, casa da vila — **encerra o
veio**: a colônia prefere perder o minério a perder o mineiro, e a
escada volta a mandar. Veio que anda de lado não paga nada: o custo é do
que desce, e só dele.

| | |
|---|---|
| **Fase vermelha** | `aVeinGoingDownOpensTheStepFirst` e `aStepThatCannotBeOpenedEndsTheVein` |
| **Verificado rodando** | `gradlew build` → **510 unitários, 0 falhas**; `runGametest` → **185 de 185** |
| **Não observado em jogo** | o poço nunca chegou a ser visto acontecendo. É conserto de leitura de código, e está dito |

### 2026-08-27 — o lampião estava no primeiro degrau, e o mineiro cavava o próprio lampião

**Três coisas se confirmaram em jogo na sessão das 21:39**, e as três
eram ciclos anteriores esperando prova:

```text
miners: 68f4dcde looking for stone, wants cobblestone, 0 of 26   ← o piso de pedra
Mine mouth at 732, 63, 898 got its lantern at 731, 63, 898       ← o lampião idempotente
took 1 from 715,47,888 · 715,46,887 · 715,45,886 · 715,44,885    ← a escada de três
```

A última é a mais importante: um bloco para baixo e um para o lado por
degrau, com mais de um bloco por coluna. **É a escada da Regra 29 sendo
cavada**, e é a primeira vez que ela aparece num log.

**E o defeito que isso desenterrou.** O primeiro degrau é
`mouth.offset(descent)`, na mesma altura da boca — e o `freeSpotNear` o
tratava como qualquer outro vizinho. O lampião foi parar exatamente ali:

```text
miners: 68f4dcde digging Lanterna at 731, 63, 898, 48 blocks away, stall 2399/2400
Miner 68f4dcde could not reach the stone at 731, 63, 898 — task back to the queue
```

O mineiro recebeu ordem de cavar a própria lanterna, e queimou o guarda
inteiro nela. **Pior: desde que a mobília virou idempotente (o ciclo
anterior), o mod a reporia toda passagem** — põe, o mineiro quebra, põe
de novo. A mobília saiu da coluna da descida; sobram três lados, e três
bastam para duas peças.

| | |
|---|---|
| **Fase vermelha** | `theMouthFurnitureStaysOutOfTheStaircase` |
| **Verificado rodando** | `gradlew build` → **510 unitários, 0 falhas**; `runGametest` → **181 de 181** |
| **O que eu errei no caminho** | li a diagonal `715,47,888 → 715,46,887` como um veio descendo e escrevi um conserto para ela. Era a **escada**, funcionando. Os dois testes daquele conserto caíram por afirmarem uma geometria que o `OreVein` não produz, e a metade especulativa foi revertida |

### 2026-08-27 — a pedra ganhou piso, e o mineiro deixou de esperar obra

**A regra era o contrário até hoje**, e estava escrita: *"a tábua tem
meta própria mesmo sem obra; pedra não — ninguém quer um baú cheio de
pedregulho por gosto"*. A objeção continua certa, e é o próprio piso que
a responde: alcançadas as 64, o déficit é zero e nenhuma tarefa nova
abre. **Piso não é fome sem fim.**

**O que a regra antiga custava**, medido na sessão das 21:06 — dezenove
ciclos, dois mineiros capazes, e uma linha só, no primeiro ciclo:

```text
no miner work: no task open for it — 2 able to mine
```

O `IdleLog` registra transições, então essa linha vale a sessão inteira.
A cadeia:

```text
sem lote livre  →  sem obra aberta  →  stoneForWork == 0
                →  sem meta de pedra  →  sem tarefa de mineração
                →  dois mineiros parados dezenove ciclos
```

E a obra dependia de uma varredura que consumiu a sessão toda. **Sob
demanda, na prática, o mineiro quase nunca trabalhava.**

**O segundo motivo do texto antigo expirou, e isso precisou ser
conferido antes de inverter a regra.** Ele dizia que `ColonyCycle.typeFor`
mandava todo recurso natural para coleta, e a meta de pedra virava tarefa
que só o lenhador podia pegar — *ele derrubava árvore para atendê-la*.
Hoje `typeFor` decide pela produção declarada: `MINED` vira
`COLLECT_STONE`, e quem a pega é o mineiro.

**Sessenta e quatro**, uma pilha: a casa de deserto do catálogo é de
arenito liso aos sessenta. A obra manda quando pede mais; obra pequena
não abaixa o estoque guardado para a casa seguinte.

| | |
|---|---|
| **Fase vermelha** | `stoneIsAGoalEvenWithNoWorkOpen` e mais quatro — a constante `STONE_FLOOR` não existia |
| **Um teste antigo caiu, e por verdade** | `aFullChestAsksForNothing` afirmava que baú cheio não pede **nada**, e agora a colônia quer pedra mesmo cheia. Estreitado à madeira, que é do que a Regra 1 fala |
| **Verificado rodando** | `gradlew build` → **510 unitários, 0 falhas**; `runGametest` → **180 de 180** |
| **Limitação assumida** | o piso de pedra **não** é limitado pelo espaço do armazém. Nunca foi: o que a obra pedia já ignorava o armazém, como a lã, o vidro e o carvão ignoram |
| **Ainda não visto em jogo** | o mineiro descendo. A escada de três blocos continua sem verificação — nesta sessão ele nunca chegou a receber tarefa |

### 2026-08-27 — a varredura pela metade também atravessa o fechar do mundo

O 🔴 que a própria medição abriu, três entradas acima. Gravar o índice
não bastava: ele só nasce de uma volta **completa**, e a sessão das
20:22 parou em **14 de 17**. As catorze passagens foram para o lixo, e
uma vila grande podia repetir isso sessão após sessão sem nunca guardar
nada.

**A armadilha, e é o que quase virou um defeito novo.** O cursor sozinho
não é meio conserto. Retomar no anel 40 sem as ruas que os anéis 0 a 39
acharam faria a volta terminar com meia lista e chamá-la de índice
completo — e a colônia passaria a perguntar só a ela, sem nunca mais
varrer para descobrir o que faltava. **Um índice que mente sobre ter
visto tudo é pior que índice nenhum.**

Por isso o cursor carrega o que já foi achado, e por isso ele **não**
aparece em `roadIndexSize`: meia volta não é índice, e essa diferença é
a única coisa que separa um atalho de uma mentira. Está afirmado em
`whatTheHalfSweepFoundIsNotAnIndexYet`.

Os dois nunca coexistem para a mesma colônia, e não por acaso: o índice
é construído no mesmo instante em que o cursor é apagado.

| | |
|---|---|
| **Fase vermelha** | `SweepCursorSaveTest` inteiro sem compilar — 42 erros |
| **Verificado rodando** | `gradlew build` → **506 unitários, 0 falhas**; `runGametest` → **180 de 180**, com `paused sweeps` nas duas linhas de log |
| **O que ele aceita de envelhecimento** | anel já varrido não é reolhado, e entre sessões o jogador pode ter mudado o mundo ali. É o mesmo trato que a varredura já fazia dentro de uma sessão — 8,5 minutos —, agora esticado. Quando a volta completa, a seguinte recomeça do centro |

### 2026-08-27 — o baú marcado passou a ser de quem a marca diz

**Visto em jogo:** *"tem material de lenhador indo para o baú do
fazendeiro"*. O baú era escolhido pelo **mais perto da cama**, por ordem
de chegada, e profissão não entrava na conta. O log da sessão mostra o
mecanismo inteiro:

```text
MINER    9532fc7a: bed 755,64,918  chest 753,64,920  (2,8 blocos)
SHEPHERD f638379c: bed 753,64,918  chest 754,64,918  (1,0 bloco)
```

Duas camas a dois blocos uma da outra, e quem fosse processado primeiro
levava o baú do outro.

**Decisão do autor: a marca vale.** Ela já existia desde 08-12 — um
quadro com a ferramenta da profissão pregado no baú — e era escrita a
cada ciclo. O que faltava era o mod **lê-la de volta**: era decoração, e
virou regra. Baú marcado só serve a quem a marca diz; quem ainda não tem
profissão só pega baú sem marca, senão bastaria reivindicar antes de ser
contratado.

**Por que isso não trava a vila.** A Regra 8 continua atrás: quando não
há baú livre ao alcance da cama, o `ChestPlacer` põe um novo — e baú novo
nasce sem marca.

| | |
|---|---|
| **Fase vermelha** | `aMarkedChestIsOnlyFreeForItsOwnProfession`, `theMarkAnswersWhichProfessionOwnsTheChest` |
| **Verificado rodando** | `gradlew build` → **499 unitários, 0 falhas**; `runGametest` → **180 de 180** |
| **O que ela NÃO faz**, e está dito no código | não desfaz a primeira escolha. Baú **sem** marca continua indo para o vizinho mais próximo, e é a marca *daí em diante* que o prende. Para trocar de dono, o jogador arranca o quadro |

### 2026-08-27 — a escada da mina não dava para descer, e a boca ficava sem lanterna

Os dois vistos em jogo pelo autor, na sessão das 20:22.

**A escada.** *"O mineiro precisa quebrar mais um bloco na sua frente
para poder descer a escada."* O degrau abria **dois** blocos — que é
quanto o aldeão ocupa **parado**. Descer não é cair: é andar para a
frente no mesmo nível e só então cair, e nesse instante a cabeça está um
bloco acima do teto do degrau seguinte.

```text
degrau s      abre y, y+1      pés em y, cabeça em y+1
degrau s+1    abre y-1, y      a cabeça bate em y+1, maciço
```

O mineiro parava no primeiro degrau e batia a picareta no ar. A mina só
descia porque o jogador abria o caminho na mão. Agora a escada abre
**três** — a galeria continua com dois, porque ela é plana.

**A migração que isso obrigou.** `cut` é um índice na ordem de cavar, e a
ordem mudou: o primeiro lance foi de 20 posições para 30. Save antigo
volta com a fronteira no **zero** — já aberto é pulado de graça, 64 por
passagem, e o que passa a ser cavado é só o que faltava. É o conserto da
escada que já está no mundo, não só das próximas.

**A lanterna.** *"Faltou o lampião na entrada da mina, eu mesmo botei."*
A mobília saía toda de dentro do mesmo `if`: quem já tinha baú voltava na
primeira linha, e a lanterna nunca chegava a ser tentada. Duas bocas
comuns caíam aí — a mina de save anterior à Regra 30, e aquela em que a
primeira tentativa achou lugar para o baú e não para a lanterna. As duas
peças passaram a ser conferidas separadamente, e a que o **jogador** pôs
conta como posta.

| | |
|---|---|
| **Fase vermelha** | `theVillagerWalksDownWithoutDiggingAgain`; `aMineFromBeforeTheTallerStairStartsOver`; `aMouthThatAlreadyHasAChestStillGetsItsLantern` |
| **Verificado rodando** | `gradlew test` → **499 unitários, 0 falhas**; `runGametest` → **177 de 177** |
| **Ainda não visto em jogo** | o mineiro descendo sozinho. É a próxima sessão |

### 2026-08-27 — a varredura não reinicia: a sessão é que acaba antes

**O instrumento respondeu na primeira sessão em que rodou.** Colônia
`56c5b68d`, sessão das 20:22:

```text
14 planner runs, 14 passes over 14336 columns, 0 answered by the index
— 1 restarts (0 by drift, farthest 0 blocks), 0 complete rounds
```

Nenhuma das duas hipóteses. A varredura **avança perfeitamente** — 14
passagens, 14.336 colunas, uma passagem por ciclo, zero deriva de centro,
um único reinício (o normal, o primeiro da sessão). Ela precisa de 17 e
chegou a 14. **Faltaram três ciclos — noventa segundos.**

> E isso muda a conclusão do ciclo anterior. O índice só nasce quando uma
> volta **completa**, e essa sessão parou em 14/17 — então salvou
> `0 road indexes`, e as 14 passagens foram jogadas fora. **O cursor
> também precisa atravessar o disco**, e não só o índice. A entrada de
> 08-27 dizia *"é ele que vale gravar, não o cursor"*; a medição desmente.

A colônia `ca8966a6` deu o outro caso, e a linha nova o nomeou: *14 of 14
planner runs gave up before reaching the sweep* — é a vila sem
trabalhador capaz, e o culpado não é a varredura.

### 2026-08-27 — a varredura passou a dizer se reinicia ou se ninguém a chama

**A sessão das 19:11 não respondeu, e mostrou por quê.** Vinte e seis
ciclos de colônia, uma única linha de construção — *"still sweeping — the
budget ran out before an answer"* — e nenhuma volta completa. Dezessete
passagens bastariam para as 16.641 colunas. Duas explicações cabiam no
mesmo silêncio:

1. a varredura **reinicia** — o centro anda mais que os 20 blocos do
   `CENTER_DRIFT`, o cursor é jogado fora, e ela recomeça do centro sem
   nunca chegar ao fim;
2. a varredura **não é chamada** — o planejador desiste antes do
   `BuildSiteScanner`, e o ciclo passa sem gastar passagem.

O `IdleLog` não as separa, e por um motivo bom: ele registra
**transições**, e um ciclo em que o planejador nem roda não tem transição
nenhuma. O silêncio das duas é idêntico.

`SweepLog` tem os dois números que fecham a conta — quantas vezes o
planejador **rodou** e quantas passagens de fato **correram**:

```text
26 rodadas, 26 passagens, 26 reinícios   →  a varredura reinicia
26 rodadas,  8 passagens,  1 reinício    →  o planejador desiste antes
```

E a deriva fala na hora, não só na soma: pelo javadoc do `CENTER_DRIFT`
ela deveria ser rara — três movimentos em treze minutos em 08-25. Se a
linha sair a cada trinta segundos, **a enxurrada é o achado**.

| | |
|---|---|
| **Fase vermelha** | `SweepLogTest` inteiro sem compilar — a classe não existia |
| **Um defeito achado pela própria bateria** | a primeira rodada imprimiu `-1 never reached the sweep`: os testes de jogo chamam o scanner direto, sem planejador, e a subtração fica negativa por construção. O relatório passou a calar onde a conta não fecha, em vez de imprimir absurdo |
| **E um segundo** | o aviso saía para colônia com **um** ciclo — cinquenta avisos numa bateria. Ganhou piso de evidência (`RUNS_BEFORE_JUDGING`) |
| **Verificado rodando** | `gradlew build` → **496 unitários, 0 falhas**; `runGametest` → **175 de 175**, e a linha de deriva apareceu de verdade (*"the center moved 40 blocks"*) |
| **Ainda não respondido** | qual das duas causas é a de 19:11. Isso é a **próxima sessão de jogo**, não este ciclo |

### 2026-08-27 — o índice de ruas atravessa o fechar do mundo

A alavanca que a entrada da varredura tinha nomeado, e o item que
destravava a verificação de todo o resto: **enquanto o índice fosse de
sessão, nada do que foi consertado em 08-27 chegava a ser exercitado em
jogo.** As duas sessões curtas do dia anterior morreram na primeira
varredura porque a primeira varredura era sempre a primeira *daquela
sessão* — 16.641 colunas, mil por passagem, dezessete ciclos, 8,5
minutos antes da colônia saber onde procurar lote.

O que se grava é o **índice**, e não o cursor: são as 698 colunas que a
varredura completa achou, e não o lugar onde ela parou no meio.

**Por que é seguro gravar uma leitura do mundo.** Porque ela não é
acreditada. Cada coluna do índice é reperguntada ao mundo quando
visitada — `siteBesideRoadAt` já o fazia —, e o centro de onde a medida
saiu vai junto, para que um índice velho demais seja jogado fora em vez
de mentir. Grava-se o **caminho até a pergunta**, não a resposta.

O molde é o do `MineSave`, arquivo por agregado: `RoadIndexSave` faz o
NBT, `ColonyRoads` é o registro que atravessa (e passou a ser o único
dono do empacotamento `x`/`z` num `long` — a mesma conta era feita dos
dois lados do disco, e duas cópias que discordassem na ordem dos bits
devolveriam um índice embaralhado, com ruas onde não há).

| | |
|---|---|
| **Fase vermelha** | `RoadIndexSaveTest` inteiro sem compilar — `ColonySavedData.roads()`, `BuildSiteScanner.saved()` e `restore()` não existiam |
| **A fronteira recusa** | índice vazio (*"varri tudo e não achei rua"* pararia a colônia para sempre), entrada sem dono, colônia desconhecida, e índice maior que o orçamento de uma passagem |
| **Verificado rodando** | `gradlew build` → **484 unitários, 0 falhas**; `runGametest` → **175 de 175** |
| **Ainda não visto em jogo** | a sessão que prova o ganho — abrir o mundo e a colônia já achar lote sem varrer — **não foi jogada**. O `runGametest` grava `0 road indexes` porque nenhuma colônia de teste completa o raio |

### 2026-08-27 — o guarda parou de contar a noite, e o mineiro pegou a picareta certa

Os dois saíram de uma **conferência de plano externo** contra o código: o
autor trouxe um prompt de implementação de "Mineiro autônomo", e a
comparação com o que existe achou duas discordâncias reais.

**A porta de expediente.** `GoToWorkTargetTask` só anda em horário de
trabalho — fora dele o aldeão dorme, come e socializa. Mas o
`MinerWork` contava os tiques do guarda de travamento de qualquer jeito,
e o guarda existe para punir **quem anda sem chegar**, não quem está
proibido de andar. A sessão de 08-26 pagou: o contador foi de 886 a 2086
com o relatório dizendo `off hours`, metade do orçamento queimada com o
aldeão dormindo. O `STALL_LIMIT` promete *"tiques de expediente"* no
javadoc e o código contava todos.

O mineiro era o **único** trabalhador sem essa porta — lenhador,
construtor e fabricante já a tinham. O conserto é o molde do lenhador,
que esta classe segue por decisão.

**A picareta.** O catálogo entregava `WOODEN_PICKAXE` e o `MinerWork`
calculava o tempo de quebra com `DIAMOND_PICKAXE`: o aldeão minerava na
velocidade do diamante **segurando uma picareta de madeira**. Qual dos
dois manda estava escrito no javadoc do `TOOL` — *"o autor pediu
diamante para o mineiro"* —, então quem estava errado era o catálogo.

| | |
|---|---|
| **Fase vermelha** | `theStallGuardDoesNotCountOutsideWorkHours` — *"o guarda contou 59 tiques fora do expediente"*; `theMinerHoldsThePickaxeItMinesWith` — *"expected DIAMOND_PICKAXE but was WOODEN_PICKAXE"* |
| **Como se testa a noite sem mexer no relógio** | com **criança**: `WorkHours` diz não para bebê sem depender da hora. Mexer no relógio do mundo é global e vaza para os testes vizinhos — a interferência que já custou um ciclo a esta bateria |
| **Verificado rodando** | `gradlew test --rerun` → **477 unitários, 0 falhas**; `runGametest` → **176 de 176, quatro rodadas seguidas** |

### 2026-08-27 — as ruas ficam indexadas, e a varredura cabe num ciclo

**A medição veio antes do código.** Lendo o save do mundo do autor
direto — um leitor de Anvil+NBT escrito para isto —, das **16.641**
colunas do quadrado de raio 64 só **698** eram calçamento:

| Centro | Colunas de `dirt_path` | % do quadrado |
|---|---|---|
| 772, 898 | 698 | **4,19%** |
| 764, 926 | 770 | **4,63%** |
| 720, 908 (a obra) | 558 | **3,35%** |

O teto da varredura é 1.024 colunas por passagem, e **698 cabem numa
só**. Perguntar só às ruas tira a varredura de dezessete ciclos para um
— de 8,5 minutos para trinta segundos.

**A ideia natural foi medida e reprovada.** Seguir o traçado a partir de
uma semente parecia óbvio, e a conectividade das mesmas 698 colunas diz
que não:

```text
vizinhança direta          14 componentes   maior = 421 de 698  (60%)
tolerando 1 bloco de vão    7 componentes   maior = 508 de 698  (73%)
```

**As ruas da vila são catorze pedaços soltos.** Um alastramento acharia
60% delas, e os 40% de fora podem ser exatamente onde está o único lote
livre — a família do E14, a colônia dizendo "não há lote" com lote
existindo.

**O índice não corre esse risco porque só nasce completo:** ele é
promovido no único ponto do código em que o raio inteiro foi visitado.
Coluna que deixou de ser rua é reconferida ao ser visitada — o
`siteBesideRoadAt` já pergunta —, e rua nova entra por `remember`,
chamado de dentro do `pave` da Regra 15. Sem esse gancho o índice
envelheceria no pior momento: a rua cresce justamente quando não houve
lote, e o lote novo nasce encostado no que acabou de ser calçado.

**Limite assumido, e ele é real:** índice maior que o orçamento não vira
índice — vila com mais de 1.024 colunas de rua continua varrendo o
quadrado, que é o que ela já fazia. E **rua feita à mão pelo jogador
fica invisível** até o centro andar mais de 20 blocos e a medida ser
refeita.

| | |
|---|---|
| **Fase vermelha** | `theCompletedSweepLeavesTheRoadColumnsIndexed` — *"terminou o raio e não guardou a única coluna de rua que achou"* |
| **Verificado rodando** | `gradlew test --rerun` → **476 unitários, 0 falhas**; `runGametest` → **174 de 174, seis rodadas seguidas** |
| **Não visto em jogo** | o ganho é de relógio, e só uma sessão o mostra |

### 2026-08-27 — a varredura para de recomeçar do centro a cada casa

Achar lote apagava o cursor, e a varredura seguinte recomeçava do zero.
O centro custa **dezessete passagens** — 16.641 colunas, 1.024 por vez,
30 s por ciclo, **8,5 minutos** entre uma casa e a próxima ter chance de
nascer. É o que a sessão de 08-26 mostrou às 23:25:22: recomeço do zero
logo depois da primeira casa, e nenhuma segunda obra em oito minutos.

Agora o cursor **fica**, uma coluna adiante. É a decisão 8 aplicada como
ela foi escrita — *o conserto é no jeito de procurar, não no volume* —, e
o teto de mil colunas não subiu: o ciclo já avisa 95 ms com ele.

**Por que retomar é o certo e não só o barato:** os anéis de perto
acabaram de responder não, e agora estão **mais** ocupados, porque a casa
nova está neles. Quando o raio acaba o cursor sai sozinho e a próxima
recomeça do centro, que é onde a vila muda e o lote de ontem pode
existir.

| | |
|---|---|
| **Fase vermelha** | `theSweepKeepsItsPlaceAfterFindingALot` — *"a varredura esqueceu onde estava depois de achar o lote"* |
| **Verificado rodando** | `gradlew test --rerun` → **476 unitários, 0 falhas**; `runGametest` → **173 de 173, cinco rodadas seguidas** |

**O que este conserto NÃO resolve, e é o mais importante do achado.** As
sessões das 23:06 e das 01:33 morreram na **primeira** varredura, e essa
continua custando os 8,5 minutos inteiros. O motivo apareceu enquanto eu
conferia o efeito real:

> **O cursor é de sessão.** `SWEEPS` é um `HashMap` estático, limpo em
> `ServerLifecycleHandler` ao subir e ao parar, e **nunca gravado em
> disco**. Toda vez que o jogador entra no mundo, a colônia recomeça a
> varredura do centro.

Por isso as duas sessões curtas morreram "na primeira varredura": é a
primeira **daquela sessão**, e é sempre.

**A alavanca seguinte mudou de nome no mesmo dia:** com o índice de ruas,
o que vale gravar em disco é **ele**, e não o cursor. Ver a entrada acima.

### 2026-08-27 — o E30 fechou, e desenterrou o vizinho dele

**A causa raiz era o alcance.** `MinerWork.isWithinReach` media `dx` e
`dz` e nunca `dy`: o mineiro cavava a mina inteira **de pé na
superfície**, furando o chão para baixo, e nunca entrava nela. A sessão
de 08-26 o pegou em flagrante — `digging Pedra at 721, 54, 897, **9
blocks away**, 1/6 ticks`, picareta em movimento a nove blocos do bloco.
Funcionava enquanto a escada descia debaixo dele, e morria quando a
galeria corria na horizontal.

A Regra 29 pedia o contrário, por escrito: *"desce cavando em escada,
para poder voltar a subir"*, com degraus de dois blocos **"os que o
aldeão precisa para caber de pé"**. A escada foi desenhada para ser
andada e nunca tinha sido andada.

| | |
|---|---|
| **Fase vermelha** | `theMinerGoesDownToTheStoneInsteadOfDiggingItFromAbove`, **vermelho 6 de 6** — determinístico. A mensagem: *"quebrou a pedra de 6 blocos de distância, e o braço dele tem 4"* |
| **Conserto** | `dy` entra na conta |
| **Verificado rodando** | `gradlew test --rerun` → **476 unitários, 0 falhas**; `runGametest` → **172 de 172, dez rodadas seguidas** |

**E aí apareceu o segundo defeito, que o primeiro escondia.** Com o
alcance honesto, o mineiro passou a precisar **chegar** no bloco — e
`WorkTargets` o mandava para dentro da rocha. Bloco sólido nunca é
alcançável: a navegação devolve caminho parcial e ele estaciona onde
parou. Uma sonda temporária de uma rodada vermelha mostrou o que
faltava:

```text
alvo 2931813,-61 | aldeao 2931820,-58 | dist 7,9
vizinhos: up=ar(PISAVEL)  ·  os outros cinco sólidos
```

O alvo **tinha** vizinho pisável, e ninguém apontava para ele. Nasceu o
`approachTo`: os quatro lados primeiro — entrar no túnel é como se anda
numa mina —, depois em cima, que é o degrau recém-aberto, e por último
embaixo.

**O que foi medido, e o que não fecha.** A instabilidade que o alcance
3D criou no `theStoneLeavesTheWorldAndReachesTheChest`:

| Arranjo | Rodadas | Vermelhas |
|---|---|---|
| Alcance no plano (antes) | 6 | 0 |
| + alcance 3D | 19 | **8 — 42%** |
| + `approachTo` | 10 | **2 — 20%** |
| + boca fixada no teste | 10 | **0** |

Uma hipótese caiu no caminho e fica registrada: **não era orçamento de
tiques.** Com 1100 em vez de 320 o vermelho continuou, 2 em 6.

A última linha da tabela **não é conserto de produção, é conserto de
teste**: aquele teste não fixava a boca, então o lado da descida saía do
UUID sorteado da colônia e a sorte decidia o resultado. O teste irmão da
galeria já fixava a boca, e pelo motivo escrito lá: *"um teste não pode
depender de sorte para saber onde a escada passa"*. Enquanto o alcance
era desonesto isso não aparecia — ele furava de longe e a pedra caía de
qualquer geometria.

**O que sobrou está no E32, e não se afirma consertado.**

### 2026-08-26 — o E23 tinha cinco bocas, e três nunca tinham falado

O nome ofuscado voltou ao log da sessão das 23:14 — **97 ocorrências de
`class_2338{...}`**, todas do mineiro. Não é regressão: a correção de
08-26 arrumou os caminhos que **já tinham aparecido em log**, e os do
mineiro nunca tinham rodado numa sessão porque **nenhuma mina tinha
aberto**. A primeira mina da história do mod acendeu três linhas mudas
de uma vez.

O varredor foi o próprio histórico de logs — todas as sessões
arquivadas, agrupadas por forma de mensagem. Ele achou **cinco** bocas,
e duas eram do construtor:

| Onde | A linha |
|---|---|
| [`MineDigging:333`](src/main/java/com/villagecolony/fabric/work/MineDigging.java) | `opens a mine at` |
| [`MinerWork:358`](src/main/java/com/villagecolony/fabric/work/MinerWork.java) | `took {} from` |
| [`MinerWork:385`](src/main/java/com/villagecolony/fabric/work/MinerWork.java) | `could not reach the stone at` |
| [`BuilderWork:317`](src/main/java/com/villagecolony/fabric/work/BuilderWork.java) | `skips {} at — nothing holds it` |
| [`BuilderWork:476`](src/main/java/com/villagecolony/fabric/work/BuilderWork.java) | `Could not finish the two-part block at` |

O conserto é o `.toShortString()` que
[`MineMouth:80`](src/main/java/com/villagecolony/fabric/integration/MineMouth.java)
já usava — foi por isso que a linha da boca mobiliada saiu legível na
mesma sessão em que as outras três saíram ofuscadas.

**Nulidade conferida antes de encadear:** as três do mineiro só são
alcançadas depois do `job.target == null` ter retornado, e `giveUp` vem
de dentro do ramo que já leu `getBlockState(job.target)`.

| | |
|---|---|
| **Verificado rodando** | `gradlew build` → **476 unitários, 0 falhas**; `gradlew runGametest` → **All 171 required tests passed** |
| **Teste novo** | **nenhum** — é formatação de log, e o oráculo é o log da próxima sessão de mina |

**O que fica em aberto:** nada garante que a sexta boca não exista num
caminho que ainda não rodou. O varredor é repetível e está no histórico
desta sessão — vale repassá-lo depois de toda sessão que estreie um
caminho novo.

### 2026-08-26, 23:14 — a mina abriu, e a casa terminou

Dezoito minutos, e **quatro itens da lista "falta ver em jogo" caíram
juntos**. A sessão anterior, de dois minutos, não tinha chegado nem à
primeira varredura de lote; esta passou por tudo em cinco minutos:

```text
[23:20:17] planned .../plains_small_house_1 at 720, 63, 908 — 149 blocks, 2 builders
[23:20:18] Miner 68f4dcde opens a mine at {x=732, y=63, z=898} - down 10 then 10 more
[23:20:18] Mine mouth at 732, 63, 898 furnished — miner chest at 732, 63, 899
[23:25:14] finished .../plains_small_house_1 — 127 blocks placed, now colony infrastructure
[23:33:07] Saved 4 colonies with 41 workers, 1 buildings, 1 mines and 0 open projects
```

| # | O que se viu | Detalhe |
|---|---|---|
| **1** | **A mina abrindo** | e **sem** passar pela pedra de superfície: as vinte e quatro colunas de perto acharam lugar na primeira passagem |
| **2** | **O mineiro cavando** | 43 blocos numa tarefa só, descendo até y≈44 |
| **4** | **A boca mobiliada** | `miner chest at 732, 63, 899`, e `lantern at nowhere it fits` |
| **7** | **O pastor tosquiando** | `sheared 3 of gray_wool — 42 this task`, e 28 no outro |

**O que a casa não prova, e não se afirma.** Ela terminou **com a
barreira segurando dezenove peças**:

```text
[23:33:07] TEST BARRIER covered for 19 this session:
               16x stripped_oak_log — the manufacturer's stripping
                3x glass_pane — the miner's sand and the smelter's glass
```

E é o padrão de 08-25 de novo, com números melhores: a barreira riscou
`stripped_oak_log` entre 23:20:25 e 23:23:53, e os fabricantes
entregaram `6 pieces`, `10 pieces` e `4 pieces` a partir de **23:27:04**
— depois de a casa ter fechado, às 23:25:14. **A cadeia não está
quebrada; ela chega tarde.** *A casa feita inteira com material da
própria colônia* continua sem prova, e é o item do Nível 3 que fica de
pé.

### 2026-08-26 — o guarda passou a provar as duas metades da Regra 9

O javadoc de `theStallGuardReturnsTheTaskAndForgetsTheTree` prometia duas
provas — a tarefa volta à fila **e** a árvore é esquecida — e o teste só
fazia a primeira. O nome do método já dizia `AndForgetsTheTree`.

A segunda é o que fecha o G2: soltar a tarefa sem esquecer a árvore
**troca de trabalhador e não de problema**, porque a busca é
determinística e aquela árvore continua sendo a mais próxima.
`theTreeMarkedOutOfReachIsSkipped` já provava o **filtro**, marcando à
mão; ninguém provava que o **guarda marca**.

| | |
|---|---|
| **Custo em produção** | uma linha, e é só visibilidade — `TreeMarks.isOutOfReach` de package-private a `public`, mesmo precedente de `forgetUnreachable()` |
| **Fase vermelha** | **conferida** — desligando a marcação em `TreeChoice.giveUp`, a bateria acusa **uma** falha, a nova. As outras duas afirmações continuam passando, então a nova isola mesmo a segunda metade |

Com esta, as três pendências anotadas no ciclo fecharam: o E20, a limpeza
depois do assert, e a lacuna do javadoc.

### 2026-08-26 — a limpeza passou a rodar quando a afirmação cai

`context.assertTrue` lança, e a limpeza vinha depois dela: uma afirmação
que caísse deixava trabalhador vivo, colônia registrada e estático
alterado **para o resto da bateria** — e o sintoma aparecia em outro
teste. Terceiro canal de interferência da bateria, conhecido desde
08-19 e nunca fechado.

**44 dos 46 pontos de limpeza** estavam depois de uma afirmação. Agora
são `try/finally`. O `RoadExtensionGameTest` já estava certo, e o outro é
um auxiliar sem afirmação.

Junto: o `ColonyFixture` esquecia três profissões e o
`VillagerLifecycleHandler`, que ele espelha, esquece seis. Mineiro,
fundidor e pastor entraram, e as chamadas à mão saíram.

| | |
|---|---|
| **Prova** | uma afirmação quebrada de propósito, e o `finally` logou `worker ainda registrado? false` |
| **Depois** | 476 unitários e 171 de jogo, zero falhas · 0 afirmações dentro de `finally` |
| **Produção alterada** | **nenhuma** — tudo em `src/gametest` |

**O que não foi medido, e não se afirma:** a intenção era contar a
cascata antes e depois. Deu uma falha nos dois arranjos — o teste
escolhido deixou de contaminar ninguém por causa da própria correção do
E20. Está provado que **a limpeza executa**; o ganho contra cascata
continua sendo argumento estrutural, não número.

### 2026-08-26 — o E20 fechou, e o mod não tinha culpa

**O guarda de travamento sempre funcionou.** A instrumentação de uma
rodada vermelha mostrou o que ninguém tinha olhado: a tarefa voltava à
fila no tique 61, o job encerrava, e ela ficava `AVAILABLE` até o tique
240. **No 260 aparecia `RESERVED` de novo, com job novo** — e a
afirmação era feita no 300, tarde demais.

Quem a reservava era o **ciclo longo**: `onServerTick` tem contador
estático do processo e varre todas as colônias registradas a cada 600
ticks. A fase em que cada teste pega esse contador é arbitrária, e era
daí que vinha a alternância de uma rodada em quatro.

**Não era a árvore marcada** — a marca vale 6.000 ticks e estava de pé. O
despacho achava **outra** árvore: o raio de busca alcança a arena dos
testes vizinhos, que rodam concorrentes.

O conserto é **uma linha a menos**: a colônia do teste não entra mais em
`COLONIES`. Nada ali precisava dela — `run` recebe a colônia na mão, e
`LumberjackWork` não tem uma única referência a `COLONIES`.

| | |
|---|---|
| **Antes** | 3 vermelhas em 15 rodadas em 08-26; 3 em 7 em 08-25 |
| **Depois** | **20 rodadas, zero vermelhas** |
| **Código de produção alterado** | **nenhum** — o defeito era do teste |

Uma hipótese caiu no caminho, e fica registrada: forçar o ciclo no tique
200 **não** reproduziu (3 rodadas verdes). Cedo demais — naquele
instante nenhuma árvore vizinha estava ao alcance.

### 2026-08-26 — a sessão que mostrou o gargalo seguinte

Vinte e três minutos, sem crash, no mundo novo. **Duas correções do dia
anterior apareceram funcionando em jogo:**

| | O que | A linha |
|---|---|---|
| **E27 — a rua saiu do beco** | A mesma colônia `56c5b68d` que em 08-25 ouviu `BLOCKED` e não cresceu | `[03:11:57] extended the road 1 blocks west from 766, 62, 961` |
| **E23 — o nome ofuscado sumiu** | `from 766, 62, 961`, e não `class_2338{...}`. Zero ocorrências na sessão — **e a sessão de 23:14 mostrou por quê: nenhuma linha do mineiro tinha rodado.** Ver a entrada das cinco bocas, no topo | a mesma linha |

**E ela mediu o gargalo seguinte**, que virou o E29: a rua rendeu **um**
bloco, e um bloco não abre lote para casa de sete por sete. A colônia
voltou a varrer.

**O que continua sem prova:** o E22 não foi exercitado (nenhum construtor
morreu), o E24 não teve sintoma para mostrar, e a mina não chegou a ser
perguntada — sem obra não há demanda de pedra.

### 2026-08-25 — duas sessões, e os becos sem saída que elas mostraram

A primeira sessão desde 08-22 durou 42 minutos e **terminou em crash**. A
segunda, num mundo novo, mostrou a vila viva e parada: rua crescendo,
lote nunca achado, mineiro sem nada para cavar.

**O padrão que as duas revelaram, e que este ciclo atacou:** em três
lugares diferentes o mod tinha **um candidato só** — uma ponta de rua,
uma boca de mina, uma fonte de pedra. Candidato único é a vila parando
para sempre quando o terreno não colabora, porque o critério é
determinístico e o mundo não muda sozinho. Todos ganharam alternativa, e
toda recusa passou a envelhecer.

Nenhuma dessas correções foi vista em jogo.

**Seis coisas vistas pela primeira vez numa vila:**

| | O que | A linha, com hora |
|---|---|---|
| **O fabricante descasca tronco** | a peça que a barreira de teste vinha riscando | `[22:00:19] Manufacturer dcf51c87 stripped a oak_log into stripped_oak_log` |
| **A bancada desce dois degraus** | tábua feita porque a escada precisa, e a porta feita da tábua | `[22:00:17] The colony made minecraft:oak_planks because minecraft:oak_stairs needs it` · `made 3 minecraft:oak_door out of {minecraft:oak_planks=6}` |
| **A obra parada sai da frente** | vinte ciclos, e o lote fica tomado | `[22:06:42] gives up on ...plains_small_house_1 — 148 blocks never came in 20 cycles` |
| **A rua cresce com a vila** — Regra 15 | assentou 3 dos 5 e parou no bloco que recusou, como manda o `pave` | `[21:34:15] extended the road 3 blocks east from ...` |
| **O mineiro tem voz**, uma linha por ciclo | a correção de 08-22, funcionando | `miners: cef3e01d looking for stone, work time, wants cobblestone, 0 of 43 so far` |
| **O fracasso da boca da mina fala** | a linha que faltava e custou três sessões | `no miner mine mouth work: ... no column within 40 blocks of ... — tried 4 sides at 3 distances` |

**E sete erros fecharam:**

| | O que | Prova |
|---|---|---|
| **E22 — o crash** | Um construtor morto deixava o trabalho no registro, e vinte ciclos depois a obra fechava e mandava devolver à fila uma tarefa que já estava nela. Duas correções: `Task.isHeld()` passou a ser a pergunta que autoriza o `release`, e `BuilderWork.forget` entrou no `VillagerLifecycleHandler`, onde faltava entre os seis | 2 testes unitários, 1 teste de jogo. **Corrigido, não visto em jogo** |
| **E25 — a varredura re-perguntava** | O cursor guardava só o **anel**, e a passagem seguinte recomeçava do primeiro bloco dele — a casca de um anel de raio 64 tem 512 colunas. Agora guarda a coluna, e o raio custa as 17 passagens que a aritmética pede, e não 19 | teste de jogo, **fase vermelha conferida**: com o cursor antigo ele acusa 18 passagens contra 17 |
| **E27 — a rua com uma tentativa só** | `consider` guardava a ponta mais distante e nenhuma outra; a que recusasse parava a vila para sempre, porque o critério é determinístico e o mundo não muda. Agora são doze candidatas, tentadas uma a uma, e a que recusa fica de castigo dez ciclos | teste de jogo, **fase vermelha conferida** |
| **E26 — oito minutos por lote** | A varredura termina, e o número é que era o problema: 17 ciclos por resposta. Agora a rua que a colônia acaba de calçar já é testada na mesma passagem — ela sabe onde criou beira nova e não precisa redescobrir | teste de jogo. **O teto de mil colunas por passagem ficou como está**, e vira a decisão 8 |
| **E24 — a cama reperguntada para sempre** | Cama sem vão para baú era olhada a cada ciclo, sem fim. Agora fica de castigo dez ciclos e volta sozinha — Regra 23 | teste de jogo, **fase vermelha conferida** |
| **E23 — nome ofuscado no log** | `class_2338{...}` saía na linha da rua em produção | `toShortString()` |
| **A mina sem lugar** | Eram doze colunas, e as doze caíram na mesma água. Agora são vinte e quatro — oito direções, três distâncias — e, quando nem elas servem, a pedra vem de afloramento na superfície: o caminho que a Regra 29 aposentou, de volta como alternativa | 2 testes de jogo, **fase vermelha conferida** |
| **E29 — a rua rendia um bloco por varredura** | Calçar consumia a ponta e a esquecia: a colônia voltava a varrer o raio inteiro para, 8m30 depois, calçar mais um. **Medido em jogo em 08-26, às 03:11:57** — `extended the road 1 blocks west`, e um bloco de beira não abre lote para casa de 7×7. Agora a ponta que rendeu é retomada na passagem seguinte, até 16 blocos ou até parar de render | 2 testes de jogo, **fase vermelha conferida** |
| **E5 — colheita de outras espécies** | Era "só carvalho, nunca visto em jogo". A sessão derrubou e replantou cinco: `Planted a ACACIA / SPRUCE / BIRCH / JUNGLE / OAK sapling` | **visto em jogo** |

### 2026-08-22 — as sessões de jogo e o que elas cobraram

| | O que | Prova |
|---|---|---|
| **E18 — quem funde pedra** | Duas exceções nominais saíram: `typeFor` deixou de ter lista de nomes, e o fundidor pergunta ao livro de receitas do jogo. Nasceu `Production` | 6 testes |
| **E19 — o corte por responsabilidade** | `MinerWork` 511→436 (de volta ao teto), `BuilderWork` 838→625. Três classes, duas em molde que já existia | a bateria |
| **Nível 1 — a mina tem onde nascer** | Era uma coluna só, sem alternativa e sem log. Agora são doze — quatro lados por três distâncias —, a boca é superfície e não miolo de morro, e o fracasso tem voz | teste de jogo, fase vermelha conferida |
| **O mineiro tem voz** | `MinerWork` não tinha linha por ciclo. O lenhador ganhou a dele em 08-12, o construtor em 08-18 | teste de jogo |
| **Regra 30 — a boca mobiliada** | Lanterna de um lado, baú do outro; minério menos carvão fica lá até lotar | 2 testes de jogo |
| **Grupo não é equivalência** | `ResourceSubstitution`: o padrão é não substituir, e a substituição se declara | 6 testes unitários |
| **A pedra é contada por família** | A casa de deserto pede 93 de arenito e a conta enxergava 5 | teste de jogo |
| **O construtor pisa onde cabe** | `footOf` mandava o aldeão para dentro da duna | **visto em jogo:** `WAITING_RESOURCES` |
| **A colônia procura aldeão onde viu as camas** | Com o centro parado pela Emenda 4, ela olhava o lugar antigo | **visto em jogo** |

### 2026-08-21 — o ciclo das nove decisões

| | O que |
|---|---|
| **ADR-003 Emenda 4** | O centro da colônia só anda pela sonda. **Visto em jogo:** 4 movimentos, todos convergindo, 23 varreduras do jogador recusadas |
| **Regra 21 morta** | A mobília não é mais reposta; a demanda de lã e ferro passou para a obra |
| **Regra 28 grita** | Cada peça riscada nomeia a cadeia que falhou, e a sessão termina com a soma |
| **A cabana e a paleta apagadas** | `ColonyHut` saiu; `VillagePalette` ficou com estilo, pedra e vidro |
| **ADR-007 e ADR-008** | Fusão e orientação, decididas por escrito. **Nenhuma implementada** |
| **O ícone** | Fundo recortado a partir da borda, e não por chave de cor |
| **O `NOTICE`** | O arquivo da Mojang declarado em vez de escondido |
| **E9 instrumentado** | A transição de estado diz de onde veio e o que a sonda viu |
| **A receita da cama** | O livro devolvia o tingimento, e a conta de lã dava zero |

<details>
<summary>Ciclos anteriores — 08-19 a 08-21</summary>

Regras 25, 26, 27, 28, 29 · mineiro, pastor, fundidor · paleta por bioma
· vila de deserto reconhecendo a própria rua · a rua que cresce · a mina
no save · a cadeia da areia, do carvão e do ferro · Regra 11 · obra
parada que sai da frente · casa do jogo girada para a rua · lote
conferido no volume · árvore grande deixando de ser recusada.

</details>

---

## 🔴 Erros abertos

| | Erro | Estado |
|---|---|---|
| **E36** | **Os dois guardas eram zerados a cada alvo novo.** `startNextStone`, `findCrop`, `findSheep` e os três `release` faziam `job.stall.reset()` ao trocar de alvo, e quem troca de alvo com frequência ficava **imune** ao detector de imobilidade (300) | ✅ **Fechado em 09-04.** Zerar passou a ser no ramo em que a profissão trabalha — onde `BuilderWork` e `ManufacturerWork` sempre zeraram, e por isso os dois nunca tiveram o defeito. **Eram três profissões, não seis:** o construtor e o fabricante já estavam certos, e o lenhador não zera em lugar nenhum — ver **E39**. O contador de 2.400 continua por alvo de propósito. `theStillnessGuardSurvivesTheTargetChanging`, fase vermelha conferida (*caiu de 99 para 0*) |
| **E37** | **`aFrozenMinerGivesUpLongBeforeTheStallGuard` continua instável** — 1 falha em 3 rodadas medidas em 09-04, depois do E36 | ⚙️ **A suspeita anterior está morta, e foi medida.** Este arquivo dizia *"o que sobra é o E36: cada troca de alvo zera o contador"*. Com os resets **já removidos**, a falha voltou com `stall 3/2400, still 2/300` em 360 tiques — três passagens contadas de trezentas e sessenta. Os dois contadores são fechados por `WorkHours.isWorkTime`, e o `still` também zera quando o aldeão **muda de bloco**: o relatório mostra ele em y=-53 andando para y=-58, ou seja **o mineiro daquele teste não está congelado**. O próximo ciclo precisa de um instrumento que conte as passagens de expediente, e não de mais uma suspeita |
| **E38** | **O baú do trabalhador assoreia e nada o esvazia.** Vara, maçã e muda não são `ResourceType`, nenhum trabalhador as retira, e cada uma ocupa um slot para sempre | ⚙️ **Metade fechada em 09-04.** O transbordo para a colônia tirou o lenhador do buraco e parou a destruição de item, mas **não move o assoreamento de lugar**: baú que só enche acaba cheio, e agora demora mais para chegar lá. Dar a esses itens consumidor ou descarte é **decisão de projeto** e está registrada no javadoc de `TreeFelling.deposit`, não decidida por conta própria |
| **E39** | **O lenhador é o único que cobra o guarda de imobilidade enquanto trabalha.** `LumberjackWork:325` chama `stuck()` **antes** da conferência de alcance, ao contrário das outras seis profissões | ⚙️ **Achado ao ler, em 09-04, e não observado falhando.** O javadoc do `WorkStall` diz o contrário por escrito — *"chamá-la enquanto ele trabalha puniria quem está parado de propósito"* —, e um lenhador parado numa árvore que leve mais de 300 tiques de expediente é devolvido à fila por estar trabalhando. **O conserto foi escrito e revertido de propósito:** mudar uma quarta profissão sem ter visto nenhuma falhar é o erro nº 3 desta mesma lista, de 09-04. Quer o ciclo e o teste dele |
| ~~**E33**~~ | ~~O mineiro não cavou um bloco em sete sessões~~ | ✅ **Fechado na bateria em 08-28.** Três testes em rocha maciça provam que ele cava a escada, desce cavando, e conserta a fronteira adiantada do save. Faltava a arena ser uma mina — todas as outras eram um piso de terra plano. **Falta ver em jogo** |
| **E33-a** | **O mineiro desce, cava, e então trava.** Na sessão de 08-28, 23:19, ele estava **na galeria** (y=44) com **108 pedras** já trazidas, e parou | ⚙️ **Causa encontrada em 08-29, e é aritmética:** ele parava a **exatamente dois blocos** do lugar escolhido, porque dois era a folga com que a navegação se dá por chegada. `approachTo` escolhia um lugar a 2,0 da pedra; somada a folga, 4,2 — e o braço é 4. Duas contas certas que não compunham. A folga passou a ser do destino: o mineiro pede um. **Nenhuma sessão viu o conserto** — é o nono no mesmo sintoma, e o primeiro com a conta fechada em cima de um mineiro que já estava lá dentro |
| **E34** | **Túnel cavado pelo jogador confunde a frente da galeria.** Um bolsão iluminado, desligado da escada, parecia frente | ⚙️ **Fechado nos caminhos conhecidos em 09-02, e o mod continua sem distinguir — de propósito.** A frente já era lida em ordem desde 08-28. O que faltava era a **perna**: o conserto do E32, na mesma manhã, fez o laço pular o que não fosse pisável e seguir somando adiante, o que **reabriu a forma exata do E34** — bastava um vão aberto coincidir com um índice mais avançado para o passo saltar a parede. Agora o laço para na primeira posição que **não se atravessa**. A pergunta deixou de ser *quem cavou* e passou a ser *dá para chegar lá a partir daqui*, que é a que importa — e vale igual para caverna natural. `theLegStopsAtTheWallInsteadOfJumpingToAPocketBehindIt`, fase vermelha conferida. **Nenhuma sessão viu** |
| ~~**E35**~~ | ~~Mineiro na superfície não caminha até a mina~~ | ✅ **Fechado em 08-29.** Ele não estava parado: estava **oscilando** na fronteira dos oito blocos da perna — 8,77 da boca ele era mandado à boca, 7,55 ele era mandado à pedra vinte blocos abaixo. A descida passou a ser dada pela ordem de cavar, um passo por vez. Ver a entrada no topo. **Falta a sessão** |
| **E32** | **O mineiro não entra na própria escada quando começa do lado errado.** Vizinho pisável existe, o `approachTo` aponta para ele, e o aldeão continua estacionado a 4 blocos com `0/0 ticks` | ⚙️ **Causa encontrada em 09-02, e a suspeita registrada estava errada.** A navegação **não** recusa buraco de um bloco: o `MobNavigation.findPathTo` do 1.21.1 (fonte lido) **abaixa** alvo no ar até o chão, mas **sobe** alvo sólido até sair da rocha — e dentro de uma mina isso é a superfície. Quem entregava alvo ruim era o mod: a perna devolvia um bloco cru da **ordem de cavar**, que é lista de blocos a cavar e não de lugares onde se fica de pé (2 de cada 3 posições da escada são peito e cabeça), e `cut` conta posição **entregue**, não cavada. `legTowards` nem recebia o mundo — não tinha como perguntar. Passou a filtrar por `BuilderApproach.standable`, a mesma conta do `approachTo`. Dois testes de unidade e **um de jogo em rocha maciça** (`theLegNeverAimsAtRockNobodyDug`), fase vermelha conferida nos três. **Nenhuma sessão viu o conserto.** Ver [`docs/research/E32-miner-walk-target.md`](docs/research/E32-miner-walk-target.md) |
| ~~**E30**~~ | ~~A galeria engoliu os dois mineiros~~ | ✅ **Fechado em 08-27** — era o alcance medido no plano. Ver a entrada no topo |
| ~~**E31**~~ | ~~O relatório da barreira afirma o que não mediu~~ | ✅ **Fechado em 08-28.** A soma passou a contar a peça assentada, e o veredito tem três estados: sessão sem obra sai como `NOTHING_BUILT` e **não absolve** a Regra 28. Cinco unitários e um de jogo, fase vermelha conferida. Ver a entrada no topo |
| **E21** | **`theStoneLeavesTheWorldAndReachesTheChest`** disse "a pedra não chegou ao baú" uma vez | Suspeita: custo de ler estrutura no tique. **Suspeita, não diagnóstico**. Não repetiu em 7 rodadas de 08-25 |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte | **Silêncio na sessão de 08-25** — nenhuma colônia trocou de estado três vezes em 42 minutos. É notícia boa e não é prova: nenhuma colônia da sessão foi abandonada |
| **E4** | `path held: no` e o aldeão chega assim mesmo | Provável, nunca verificado. Nenhuma linha dessas em 08-25 |
| **E3** | Sobra de colheita é perda de item | ⚙️ **Metade fechada em 09-04.** O lenhador deixou de destruir: o que não cabe no baú dele vai para outro da colônia, e só se perde com a colônia inteira cheia. **O mineiro continua sem teto de inventário** — ver a pendência do Nível 1. E em 08-25 nenhum baú encheu; em 09-04 encheu, e custou 24 troncos antes do conserto |

---

## 🟠 O que a sessão de 2026-09-04 deixou aberto

Achados da leitura do log que **não** foram atacados neste ciclo. Todos
têm medida, e nenhum tem conserto.

| | O que é | A medida na sessão |
|---|---|---|
| 🟠 | **O lenhador rejeita as paredes da própria vila.** `Not a tree — N logs without a living canopy` sobre troncos de casa | **118 rejeições sobre 28 posições**, todas em x 1436–1506, y 63–65, dentro da vila. O castigo escalona (6.000→48.000 ticks) e funciona, mas ele redescobre a mesma parede seis vezes. **Nenhuma recusa veio com o número 24**, então a regressão de 08-19 não voltou |
| 🟠 | **O agricultor fica ocioso quase o tempo todo.** `no ripe crop within 32 blocks of the village` | **86 ciclos**. Houve 15 colheitas com replantio, e depois disso o raio de 32 não alcança mais nada maduro. É afinação, não defeito — mas 86 de 81 ciclos é a profissão inteira parada |
| 🟠 | **O fundidor não tem o que fundir.** `nothing in the colony chests to smelt` | **34 vezes**, com o minerador entregando 1 pedra em meia hora. Deve seguir o E36 |
| 🟠 | **Mineiro longe do corredor não tem resgate.** Sem posição da ordem a uma perna dele, não há passo a dar e a boca continua sendo a resposta — 19 blocos acima e 30 de distância, que a navegação não cumpre | O conserto de 09-04 ataca **o que o levava para lá** (o alvo de areia), não o resgate de quem já está. Decisão de projeto: caminhar em linha reta aceitando trecho não navegável, ou devolver a tarefa e deixar a rotina Vanilla trazê-lo |
| 🟡 | **Caso vizinho do impasse do poço.** Se o **dono** for despachado primeiro e a galeria estiver esgotada, ele solta a escada, gasta a busca e a retoma no tique seguinte — o outro nunca entra | Não é o travamento da sessão (a colônia não congela, o dono trabalha). O remédio seria trocar o `release` de "não achei pedra" por `stepAside`, que já existe — mas muda a semântica documentada dele e faria mineiro sozinho perder uma passagem por rodada |
| 🟡 | **`Colony cycle took 81 ms`**, acima de um tique de servidor | Uma ocorrência, com 6 colônias e 78 trabalhadores salvos. Escala mal |
| 🟡 | **Só uma das 6 colônias ciclou.** As outras 5 foram salvas e nunca apareceram no log | Provavelmente chunks descarregados, e o comportamento correto. Mas agora que o ciclo parcial **fala** (`COUNT_PARTIAL`), a próxima sessão distingue as duas coisas |

---

## 🟠 O ciclo de 2026-09-04, à noite — o que ele deixou aberto

Quatro pedidos do autor entraram; **três foram feitos, um não**. Nenhum
dos três foi visto em jogo.

| | O que é | Estado |
|---|---|---|
| 🔴 | **Ramais da mesma escada, um por mineiro.** Decisão do autor: os mineiros deixam de esperar a vez e cada um ganha um braço próprio a partir do poço | **Não feito, e é o próximo ciclo.** O bloqueio tem nome: `Mine.cut` é um cursor só, e ele é compartilhado com o `MineLighting.spotFor` e com a perna do `MinerReach`. Ramal por mineiro exige cursor por braço e a mudança se espalha por essas três. A migração sai de graça — `MineSave.SHAPE_VERSION` já faz a mina voltar ao primeiro degrau quando a geometria muda, e basta ir a 3 |
| 🟠 | **Os 49 `assign()` que criam trabalhador de mãos vazias.** Só `WorkerEquipmentGameTest` chama `equip`; três testes de `MinerGameTest` passaram a chamar neste ciclo | Passam hoje por folga no `tickLimit`, não por estarem certos. Desde que o `BlockBreakTime` pergunta à mão, teste que mede tempo sem equipar mede a mão nua |
| 🟠 | **`MineDigging` tem 1.078 linhas** e passou o `VillageDetectionHandler` (983) como o pior arquivo do projeto | A lista de arquivos acima de 500 linhas, mais abaixo, ainda não o traz |
| 🟡 | **A picareta de ouro vence a de diamante** — 12 de velocidade contra 8 | É o Vanilla, e aqui não tem o defeito que o compensa: ferramenta de trabalhador não se gasta. Fica dito porque vai parecer defeito quando aparecer em jogo |

---

## 🟠 Pendências, por nível de progressão lógica

A ordem é de dependência: cada nível precisa do anterior de pé.

### Nível 0 — o que já roda em jogo

Detecção · identidade estável · aldeões e profissões · baús · lenhador
**em cinco espécies** · fabricante, **inclusive descascando e fazendo o
que a obra pede dois degraus abaixo** · construtor **chegando ao bloco**
· centro parado pela sonda · **a rua crescendo** · **a obra parada
saindo da frente** · **a mina abrindo e sendo mobiliada** · **o mineiro
cavando** · **o pastor tosquiando** · casa terminada duas vezes: em
08-19 com baús que o jogador encheu, e em 08-26 sem ele — com dezenove
peças da barreira.

### Nível 1 — a raiz do material *(aberta, e ela se fecha sozinha)*

- **A mina abriu em 2026-08-26, 23:20:18** —
  `Miner 68f4dcde opens a mine at {x=732, y=63, z=898} - down 10 then 10
  more`, seguida de `Mine mouth ... furnished`. A busca acertou **na
  primeira camada**: as vinte e quatro colunas de perto bastaram, sem
  precisar da boca ruim nem do afloramento.
- **O mineiro cavou** — 43 blocos numa tarefa só, descendo até y≈44.
- **A galeria o engoliu, e o E30 fechou em 08-27**: o alcance era medido
  no plano, e o mineiro cavava de pé lá em cima sem nunca entrar na
  mina. Agora ele desce, com teste.
- **Continuar ainda não está provado em jogo.** O conserto tem teste e
  **nenhuma sessão o viu rodar**. E o **E32** ficou de pé: em 20% das
  geometrias o aldeão não entra na própria escada.
- **A pedra de superfície continua sem prova** — nunca foi exercitada,
  porque a busca nunca precisou dela. A linha é
  `no miner surface stone work: ...`.

#### Vindas da conferência do plano externo — 2026-08-27

Um prompt de implementação de "Mineiro autônomo" foi comparado, item a
item, com o que existe. A maior parte já estava feita; **quatro coisas
não estavam**, e todas têm sintoma já observado em log. Ficam aqui, no
nível a que pertencem, e não como plano à parte.

| | O que falta | O sintoma que já apareceu |
|---|---|---|
| 🟠 | **Inventário cheio dispara retorno.** Hoje o mineiro deposita e segue; não há teto nem volta por lotação | É onde mora o **E3** — sobra de colheita é perda de item |
| ✅ | ~~**Um mineiro por mina.**~~ **Feito em 08-28** — a mina passou a ter dono, no molde do `TreeClaims`. Quem não é o dono sai sem alvo e herda a escada quando o dono largar o trabalho; a reserva não vaza porque é conferida a cada ciclo contra os trabalhos abertos. Oito unitários e dois de jogo, fase vermelha conferida | Sessão de 08-26, 23:23:08 |
| ✅ | ~~**Iluminação além da boca.**~~ **Feita em 08-28** — uma tocha de parede por passagem, no alto do que já foi cavado, de oito em oito posições da ordem. A luz fica um espaçamento **atrás** do cursor, que é onde ela precisa estar: o mineiro acabou de sair de lá, e é por lá que ele volta. Seis unitários e três de jogo | Sessão de 08-26, 23:20:18 |
| 🟢 | **Estoque-alvo além da conta da obra.** Hoje a colônia só quer o que a obra pede. Um piso mínimo de ferro e carvão é ideia legítima — e é **Nível 5**, o motor da ADR-009, não agora | nenhum; é desenho |

**O que do plano foi deliberadamente recusado**, para não voltar como
sugestão nova: caverna natural como estratégia de mineração (§9B) —
o **E32** diz que o aldeão não entra nem na própria escada de dois
blocos; máquina de estados própria do mineiro (§17) — criaria uma
segunda arquitetura de trabalho ao lado das tarefas que as outras cinco
profissões usam; sistema de configuração externo (§24) — as constantes
moram no código com o javadoc que as justifica; e Nether, Deep Dark e
risco por bioma (§22, §23) — o Nível 1 ainda não foi visto em jogo.

### Nível 2 — material processado *(feito, e ainda passando fome)*

- **E18 fechado em 08-22**, e pelo caminho genérico que a ADR pediu:
  `Production` declarada no recurso, e o fundidor perguntando ao livro
  de receitas do jogo. **Nenhuma sessão viu isso rodar** — a linha a
  procurar é `Smelter ... made minecraft:smooth_sandstone`.
- 08-25 mostrou que o elo **não está quebrado, está sem entrada**: o
  fundidor apareceu vivo e olhando o baú quinze vezes, sempre com
  `Smelter ... stopped — nothing in the colony chests to smelt`.
- **08-26 mostrou que ele espera a coisa errada.** A mina abriu e ele
  continuou com a mesma linha, porque o que lhe falta é **areia**, e a
  cadeia da areia não começou — foram dela os 3 `glass_pane` que caíram
  na barreira. O fundidor não espera o Nível 1 inteiro: espera **a
  areia**, e isso é uma pendência com nome próprio agora.

### Nível 3 — a obra termina sem o jogador

- **A casa terminou sozinha em 2026-08-26, 23:25:14** — 149 blocos
  planejados, **127 assentados**, em 4 min 57 s, e virou infraestrutura
  da colônia. É a primeira vez sem o jogador encher baú. Em 08-25 a
  mesma obra tinha posto **um** bloco e morrido esperando pedregulho.
- **Mas dezenove peças foram da barreira**, não da colônia: 16
  `stripped_oak_log` e 3 `glass_pane`. *Casa feita inteira com material
  da própria colônia* **continua sem prova**, e é o que resta deste
  nível.
- **A barreira risca antes de a cadeia ter chance, e agora com número.**
  Em 08-26 ela pulou `stripped_oak_log` entre 23:20:25 e 23:23:53; os
  fabricantes entregaram a partir de **23:27:04** — depois de a casa ter
  fechado. Mesmo padrão de 08-25, e a conclusão é a mesma: **a cadeia
  funciona e chega tarde**. Enquanto a Regra 28 valer, a soma da sessão
  superestima o que está quebrado.
- **E21** — instrumentar antes de corrigir.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
- ✅ **A varredura recomeçar a cada obra fechada** — resolvido em 08-27,
  o cursor fica onde achou o lote.
- ✅ **Perguntar só às ruas** — resolvido em 08-27 pelo índice: 698
  colunas em vez de 16.641, e a varredura cabe numa passagem.
- ✅ **O cursor da varredura** — resolvido em 08-27, e **confirmado em
  jogo** na sessão das 21:06: 17 passagens, 16.641 colunas, uma volta
  completa e `1 road indexes` gravado. A próxima sessão abre com o índice
  na mão.
- ✅ **O veio que desce** — resolvido em 08-27: ele abre o degrau antes,
  e desiste do minério quando o degrau não pode ser aberto.
- 🟠 **O mineiro cavando de verdade ainda não foi visto em jogo.** A
  causa foi encontrada e consertada — degrau diagonal invisível para a
  busca de faces —, e a galeria aprendeu a recuar para desfazer o
  estrago que já está no save. **Falta a sessão que confirme.**
- 🟠 **Mineiro longe demais não caminha até a mina.** Na mesma sessão, o
  segundo mineiro passou tudo a `51,4 blocks away (out of reach)`, parado
  na vila. O alcance da navegação de aldeão não cobre a descida inteira,
  e o guarda só devolve a tarefa depois de 2400 tiques. Não investigado.
- 🟡 **O piso de pedra ignora o espaço do armazém.** Baú cheio continua
  pedindo pedra. Vale para lã, vidro e carvão também — é a família toda
  de metas de demanda, e nenhuma delas tem `room`.
- 🟡 **`theMinerWithWorkLeavesALineInTheLog` não devolve a distância de
  mina.** Achado em 08-28 lendo a bateria: ele chama
  `MineDigging.shortenMineDistanceTo(2)` e não tem `restoreMineDistance`
  no `finally`, então a constante fica em 2 para o resto da bateria.
  Todos os outros testes que a encurtam a devolvem. **Não corrigido** —
  não é deste ciclo, e mexer nela sem rodar a bateria inteira atrás é
  trocar um risco por outro. É da mesma família da interferência entre
  testes de jogo que já custou rodada.
- 🟡 **`ColonySavedData.sync` tem sete parâmetros**, numa cadeia de
  sobrecargas 2→4→5→6→7. Cada agregado novo alonga a corrente. Não
  incomoda ainda; o dia em que incomodar, o conserto é um tipo que
  carregue o conjunto.
- ✅ **A varredura não reinicia** — medido, não suposto: 1 reinício em 14
  passagens, zero por deriva de centro.
- 🟠 **Baú sem marca ainda vai para o vizinho mais próximo.** A marca
  prende o baú depois da primeira escolha, não antes dela. Se a primeira
  errar, quem conserta é o jogador arrancando o quadro. A saída seria a
  opção 2 daquela conversa — limitar a busca à casa da cama, apertando o
  `isInTheSameRoom` que já existe.
- 🟡 **Rua feita à mão pelo jogador fica invisível ao índice** até o
  centro andar mais de 20 blocos. A rua que a colônia mesma calça entra
  na hora, por `remember`.
- **A Regra 28 filtra o catálogo para `*_small_house_1`** — a mesma
  barreira que torna o teste possível impede escolher outro objetivo.
- **O catálogo do jogo já tem as alternativas**, e isso está confirmado:
  `farm`, `large_farm`, `animal_pen`, `armorer`, `mason`, `tannery`,
  `tool_smith`, `library`, `medium_house`, `big_house`. O propósito sai
  do **nome** — nenhum `.nbt` novo, nenhum byte da Mojang.
- `plains_small_farm_1` = terra, terra arada, tronco, água, trigo. **A
  vila de planície poderia construir isso hoje.**

### Nível 5 — o motor da ADR-009

`VillageProfile` · inventário de território · escassez e distância ·
orçamento de recursos · detecção de dependência circular · reserva
mínima de sobrevivência · objetivos graduais. **Nada disso existe.**

### Nível 6 — o que nem modelo tem

Comida · água · o fazendeiro (tem enxada e baú desde a Fase 4 e nunca
teve código) · população por capacidade · defesa · especialização ·
comércio entre vilas.

### Fora dos níveis — dívida que não bloqueia

- **7 arquivos de código acima de 500 linhas**, e 5 de teste.
  `VillageDetectionHandler` é o pior com 983, e o corte dele é o próximo:

```text
983  VillageDetectionHandler      621  BuildSiteScanner
724  TreeHarvester                565  ConstructionPlanner
639  ManufacturerWork             502  ColonySavedData
625  BuilderWork

1571 LumberjackGameTest           754  MinerGameTest
975  BuilderGameTest              670  BuildSiteGameTest
                                  504  ProfessionAssignerTest
```

- **ADR-008** (orientação) e **ADR-007** (fusão), decididas e por escrever.
- **Regra 16** — distância mínima e máxima entre construções.
- **O ícone** — 1,95 MB num jar de 2,29 MB.
- **Cenário de teste por bioma.** A planície escondeu **duas vezes** que
  o deserto estava quebrado.
- **O `Development-Log`** parou em 08-15. Quarenta e seis commits e três
  sessões de jogo não estão nele.

---

## ⚠️ Incompatibilidades — o que se contradiz hoje

| | O que |
|---|---|
| ✅ | **Os quatro níveis da ADR-009 §3.10 — resolvida em 08-26.** A política está escrita, a ordem funciona, e a Regra 27 abriu para pedra pela Emenda 1: `ALTERNATIVE` é o nível que o construtor assenta. Fora da pedra, `ACCEPTABLE` conta para a meta e não vai para a parede |
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **`ChestWithdrawer.takeGroup` ainda usa grupo como equivalência.** Hoje é inócuo — só o fundidor o chama, com `SAND` e `IRON`, que têm um membro só. É o resto do buraco |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |
| ✅ | **ADR-009 §17 (população por capacidade) vs o vanilla — resolvida por decisão em 08-26.** O jogo controla o *breeding* e o mod não tem como segurá-lo. A §17 **não cabe**, e fica registrada como ideia recusada em vez de pendência aberta |
| ✅ | **ADR-009 §14 vs Regra 27 — resolvida.** O propósito da estrutura sai do nome, e o catálogo do jogo já os tem. Nenhuma contradição |

---

## ✅ Decisões tomadas em 2026-08-26

Oito das nove foram respondidas de uma vez. **Seis não precisaram de
código** — são escolha registrada, e valem a partir de agora. **Duas
viraram trabalho** e já estão feitas, com teste e fase vermelha
conferida: a boca ruim da mina, e a varredura que recomeça quando o
centro anda muito.

| | Decisão | O que ela manda fazer |
|---|---|---|
| **3** | **A Regra 28 sai quando o planejador souber desistir de um objetivo** — e não antes | Vira dependência com nome: a barreira de uma casa por bioma **só cai depois do Nível 4**. Enquanto o planejador perseguir uma obra só e não souber trocar de alvo, a vila continua levantando a casa pequena do bioma dela. A **Regra 25** — a maior planta que couber — fica dormindo junto, e acorda no mesmo dia |
| **4** | **A Regra 25 volta** — por consequência da 3, e não por decisão própria | "A maior planta que couber no lote" precisa de mais de uma planta, e é a Regra 28 que impede isso. Respondida a 3, esta se responde sozinha: a 25 fica dormindo e acorda no dia em que a 28 sair. **Nada a apagar** — se o autor discordar, é só dizer |
| **1** | **Mina sem lugar: ela aceita uma boca ruim, e procura mais longe** | Nasceu a segunda passagem da busca: quando as vinte e quatro colunas de perto falham, ela tenta 150% e 200% da distância, com janela de altura mais larga — aceita subir o morro ou descer a depressão. **O que ela não relaxa:** água em cima e a Regra 3. Mina inundada não é mina ruim, é mina quebrada. Feito em 08-26, com teste |
| **6** | **A população fica com o vanilla, e a ideia original não cabe** | O jogo controla o *breeding*, e o mod não tem como segurá-lo. A ADR-009 §17 — população por capacidade — passa de pendência a **contradição declarada**: fica escrito que não cabe, em vez de esperar por uma implementação que não existe |
| **8** | **O custo da busca de lote fica como está** — e se mede na próxima sessão | **Medido em 08-26, e a condição bateu.** Na sessão das 23:06 a vila passou os 2 min inteiros em `still sweeping — the budget ran out before an answer`: raio 64 são **16.641 colunas**, o teto é **1.024 por passagem**, e a 30 s por ciclo isso dá **17 passagens ≈ 8,5 min** só para a primeira varredura — o número que o comentário de `ConstructionPlanner` já dizia. Na sessão das 23:14 ela teve tempo e achou lote; mas às 23:25:22, logo depois da casa pronta, **recomeçou do zero** e não nasceu segunda obra nos 8 min restantes. Pela própria decisão, o conserto é **no jeito de procurar, não no volume**, e virou pendência do Nível 4. Ciclo agora em **88 ms** |
| **9** | **A varredura recomeça quando o centro andar mais de 20 blocos** | *Movimento pequeno não atrapalha; movimento grande justifica recomeçar.* O cursor passa a guardar de que centro os anéis foram medidos. Os três movimentos da sessão de 08-25 foram todos abaixo de 20 e teriam mantido o cursor — a decisão de 08-19 continua valendo para eles. Feito em 08-26, com teste |
| **2** | **A substituição vira os quatro níveis da ADR-009 §3.10** | `PREFERRED / ACCEPTABLE / ALTERNATIVE / FORBIDDEN`, com ordem de preferência. O padrão não mudou de comportamento — o "não" virou `FORBIDDEN` e o "sim" virou `ACCEPTABLE`. O que nasceu foi a **ordem**, que é a diferença entre aceitar e preferir. Feito em 08-26, com 3 testes. **`ALTERNATIVE` está vazio, e há um teste que impede enchê-lo** — ver a decisão pendente abaixo |
| **10** | **A Regra 27 abre para as três famílias de construção** — Emendas 1 e 2, e elas mexem numa regra marcada imutável | O construtor passa a assentar qualquer membro declarado da mesma família — madeira, tábua e pedra — no lugar do bloco que a planta pede; fora delas continua aguardando o exato. É a frase do autor: *alternativas de recursos para todas as construções dos biomas*. A de taiga levanta com abeto, a de savana com acácia, a de deserto com pedregulho. O que não afrouxou: o material sai do baú antes, **o que se assenta é o que saiu do baú**, o preferido vem primeiro, e é dentro da família. Feito em 08-26, com 2 testes de jogo e fase vermelha conferida |
| **E28 — a madeira tinha a discordância da pedra** | `OAK_PLANKS` respondia pelo grupo na conta e o construtor exigia a espécie: vila cercada de bétula declarava a meta cumprida e a casa esperava carvalho até o `PatienceClock` desistir. Achado ao escrever a Emenda 1, corrigido pela Emenda 2 | teste unitário e de jogo, **fase vermelha conferida** |
| **5** | **Agricultura está dentro do escopo, e virou a Regra 31** | *O fazendeiro planta qualquer semente que possuir ou tenha no baú, coloca a água, colhe o que está pronto e guarda no próprio baú.* Enunciado em `Project-State.md`. **Deliberadamente não implementada agora**: profissão nova inteira não entra antes de a cadeia atual ser vista funcionando até o fim numa vila |

---

## 👤 Decisões que faltam, na ordem em que travam

**Nenhuma pendente.** As nove que existiam foram respondidas em
2026-08-26, e a décima nasceu e fechou no mesmo dia.

| | Decisão | Trava |
|---|---|---|
| ~~7~~ | ~~**Fusão de colônias**~~ — **não é decisão.** UUID sobrevivente e teto de profissão estão decididos na ADR-007 desde 08-21; falta implementar | trabalho, não pergunta |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto. Riscado é o que já foi visto
acontecer, com a sessão que viu.

| | O que | A linha que prova |
|---|---|---|
| **1** | **O mineiro descendo a escada** | `Miner ... took` com o aldeão **dentro** da mina. O E30 fechou em 08-27 e **nenhuma sessão viu o conserto**. Se ele voltar a estacionar com `0/0 ticks`, é o **E32** |
| **1** | **A varredura acabando num ciclo** | `no building work: still sweeping` aparecendo **uma vez** e não a sessão inteira, seguido de `planned ... at`. É o índice de ruas de 08-27, e é o que decide se dá para ver qualquer outra coisa |
| **2** | **O mineiro parando à noite** | o contador de `stall` **congelado** enquanto o relatório diz `off hours`. Em 08-26 ele foi de 886 a 2086 dormindo |
| **2** | **A picareta de diamante na mão** | o mineiro segurando diamante, e não madeira. Cosmético e de velocidade ao mesmo tempo |
| **2** | **O fundidor assando** | `Smelter ... made ...`. Não depende mais da mina — depende da **areia**, e é a cadeia 8 abaixo |
| **3** | **A cadeia da areia inteira** | meta de `SAND` → praia → vidro → vidraça. Em 08-25 parou em `looking for sand, 0 of 6`; em 08-26 nem começou, e mandou 3 `glass_pane` para a barreira |
| **4** | **A casa inteira sem a barreira** | `TEST BARRIER covered for nothing` — e desde 08-28 a frase **só sai numa sessão que assentou peça**, então ela já não pode ser o E31 outra vez. Em 08-26 a casa subiu com 19 peças da barreira |
| **5** | **A rua crescendo e a casa nascendo junto** | `extended the road N blocks ...` seguido de `planned ... at ...` **no mesmo ciclo**. Em 08-26 a casa nasceu sem a rua crescer: a varredura achou lote sozinha |
| **6** | **A casa de deserto subindo** | `blocks left` caindo de 113. Nenhuma sessão em deserto ainda |
| **7** | **A casa esperando a cama** | `WAITING_RESOURCES` por `white_bed`. Em 08-25 a obra esperou **pedregulho** e morreu antes da cama; em 08-26 fechou sem esperar nada |
| **8** | **Fechar e reabrir o mundo com mina aberta** | O save de 08-26 devolveu `1 mines`. O **registro** sobrevive; a **galeria retomada** continua sem prova, e agora tem o E30 pela frente |
| **9** | **A correção do E22** | Um construtor morrer no meio da obra e a vila continuar de pé. Corrigido em 08-25, e a correção não foi vista em jogo |
| **10** | **A pedra de superfície** | `no miner surface stone work` seguido de `Miner ... took minecraft:stone`. Em 08-26 a busca acertou na primeira camada e **nunca chegou nesta** |
| ~~1~~ | ~~**A mina abrindo**~~ | ✅ `opens a mine at {x=732, y=63, z=898}`, 08-26 23:20:18 — e na primeira camada da busca |
| ~~2~~ | ~~**O mineiro cavando**~~ | ✅ 43 blocos numa tarefa, até y≈44, 08-26 |
| ~~4~~ | ~~**A boca mobiliada**~~ | ✅ `furnished — miner chest at 732, 63, 899`, 08-26. A lanterna não coube: `lantern at nowhere it fits` |
| ~~7~~ | ~~**O fabricante descascando e o pastor tosquiando**~~ | ✅ `stripped a oak_log` em 08-25; `sheared 3 of gray_wool — 42 this task` em 08-26. Falta só o fundidor |
| ~~9~~ | ~~**O E9**~~ | ✅ silêncio no relatório de 08-25 — mas nenhuma colônia da sessão chegou a ser abandonada, então o caso não foi exercitado |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície —
taiga, savana, nevada e deserto nunca rodaram, e todas as sessões até
hoje foram em planície. E fechar e reabrir o mundo **com um mineiro
cavando** nunca foi feito (dívida E4 do Backlog).
