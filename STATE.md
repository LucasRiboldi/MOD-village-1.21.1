# STATE — 2026-09-16

> Arquivo de estado vivo. Sobrescreve, não acumula.
> Se passar de 150 linhas, algo está errado — P0 não está fechando.

---

## P0 — bloqueadores

Um por vez, teste antes de seguir. Nada mais entra antes de fechar.

| Item | Descrição | Estado |
|---|---|---|
| P0.1 | O planejador não acha lote | ✅ entregue 09-11, **visto em jogo** |
| P0.1-b | O caminho de terra não sai de baú | ✅ **VERIFICADO no log de 09-18** (`extended the road`) |
| P0.1-c | A recusa de lote diz por quê | ✅ **VERIFICADO no log de 09-18** (`lot refusals:`, 2×) |
| P0.3 | Mineiro → armazenamento → fundidor | ✅ **VERIFICADO no log de 09-18** (184 fundições) |
| P0.5 | Perda de item por inventário cheio (E3) | ⬜ **NÃO EXERCITADO** — nenhum baú encheu em 09-18 |
| P0.6 | A enxurrada da areia calou | ✅ entregue 09-11, **espera sessão** |
| P0.7 | Elegibilidade simplificada de lotes | ✅ **VERIFICADO no log de 09-18** (`survived every check`) |
| **P0.8** | **A mina fica presa na boca (E45)** | ✅ **corrigido e CONFIRMADO EM JOGO 09-17** |
| **P1.0** | **Nenhum lote aprovado — a vila não planeja obra** | ✅ **abriu chão 09-17: 313 colunas, obra planejada** |
| **P1.1** | **A obra espera peça que ninguém fabrica** | 🔴 **REFUTADO em 09-18** — `OAK_PLANKS needs CRAFT_WOOD`, 2× |
| **P0.9** | **A obra nasce condenada: a estrada leva o lote para fora (E46)** | ✅ **VERIFICADO em 09-18** — obra planejada, nenhuma largada |

**Dois bloqueadores, e são independentes.** O playtest de 09-16 21:41–22:12
**reproduziu o E45** e revelou o E46. Corrigir só o E45 **não** faz a vila
construir.

**P0.8 — E45, a mina presa na boca.** 17.518 recusas em 31 min, 10/s, **zero
pedra quebrada**, idêntico ao playtest de 03:44 (166.559). A causa foi
**corrigida na revisão de 22:30**: não é o reinício por
`deepenIfEveryOpenArmIsDone` — esse caminho **nunca é alcançado**
(`went one level deeper` = 0 contra `no branch work` = 17.517). O laço é a
**mesma passagem repetida**: o braço fecha em 8 recusas
(`BLOCKED_BEFORE_TURNING`) dentro de um orçamento de 64 (`CUTS_PER_SEARCH`),
`claimArm` o solta, e a passagem seguinte o reocupa no mesmo `cut`.
**Corrigido em 09-16 — decisão do autor: girar a hélice, mudar a boca se as
quatro falharem.** C2 e C4 fecharam juntos, porque um é a medida e o outro a
saída. `Mine.turnsWithoutAPickaxe` conta as voltas em que o ramal fechou sem
uma pedra sair, **mora na mina e não no braço** (o braço é solto e reocupado
a cada passagem), e **só a picareta o zera**. Cheias 3 voltas, a mina gira a
hélice com o `rerouted()` que já existia; esgotadas as 4 hélices — o giro
completo —, pede boca nova. Sem boca melhor, a mina fica e o mineiro cai no
`exposedStone`.

**Guarda que o caminho de falha zera não é guarda**, e há teste que afirma
isso: `theTurnCounterSurvivesTheBranchClosingAndRestarting` fecha e reinicia
os ramais e exige que a conta sobreviva. Ver
[`docs/technical/E45-mina-presa-na-boca.md`](docs/technical/E45-mina-presa-na-boca.md).

**P0.9 — E46, a obra nasce condenada.** O **C2 foi executado**, e ele mudou
a causa raiz. A suspeita inicial — divergência quadrado/círculo — é real mas
**não matou estas obras**: elas estavam fora das **duas** réguas.

**A causa é o índice de ruas sem teto de raio.** O `SweepLog` já dizia, e
ninguém tinha lido: `40 planner runs, 40 answered by the index — 0 by
drift`. O caminho `findAmongRoads` percorre o índice inteiro e **não
consulta o raio** (nem o recebia); `remember` acrescenta **qualquer** rua
nova, e a vila calçou estrada 31 vezes nesta sessão. **O lote nasce de onde
a estrada chegou, não de onde o centro alcança.**

**Entregue e verificado:** a linha `planned …` agora diz o centro e as duas
distâncias (quadrado e reta), e o scanner emite `WARN` quando o índice serve
lote de fora do raio. `build` passou; **886 unitários, 0 falhas**, conferido
no XML.

**GameTests: 335 por rodada, e a bateria é instável — 13 verdes em 16
rodadas.** As 3 falhas são o **KF-002**, aberto hoje: o cenário perde o
aldeão entre `spawnEntity` e o tique 1, e a asserção que cai é a do próprio
cenário, antes do comportamento sob teste. **É pré-existente e isso foi
medido:** 6 rodadas no commit anterior (`e209e1f`, só documentação) deram 1
falha. Mesmo sintoma do AUD-001. Ver
[`docs/behavioral-tests/known-failures.md`](docs/behavioral-tests/known-failures.md).

Confirmado que a contagem é honesta: **335 anotações `@GameTest` no código e
34 classes registradas** no `fabric.mod.json` do gametest — nenhuma classe
sumiu da bateria.

⚠️ **Nada foi visto em jogo** — a instrumentação é código novo que ainda não
rodou num servidor de verdade.

**C4 corrigido em 09-16 — decisão do autor: o scanner estava certo, e o
guarda errado.** A estrada é projetada para sair do raio (`RoadExtension`
ordena as pontas da **mais distante** para a mais perto), então o raio de 64
é da **detecção de vila**, não um limite de crescimento. O guarda passou a
medir da **rede de ruas**: larga a obra a mais de 16 blocos de qualquer rua,
e **sem índice cai na régua antiga** — não saber onde estão as ruas não é o
mesmo que não haver nenhuma.

**O defeito de 09-15 continua pego**, e há teste que o afirma: obra longe do
centro **e** de qualquer rua continua sendo largada. É o que impede o
conserto de ser um afrouxamento.

**Duas decisões seguem abertas:** **C1** — rebaixado a 🟡, porque o círculo
só governa agora o caso sem índice — e **C3** (o centro a 77 blocos do
aglomerado de camas, que não causou o E46). Ver
[`docs/technical/E46-obra-nasce-condenada.md`](docs/technical/E46-obra-nasce-condenada.md).

**P0.7 — politica aplicada em 2026-09-15.** Piso solido disponivel e candidato a lote; pedra, gravilha e terracota nao sao recusadas pela composicao. Estrada exige material oficial mais `ROAD_AREA`; gravilha ou terracota fora da reserva continuam elegiveis. O scanner nao terraplana nem muda o mundo. Ver ADR-017.

**Lote 2 — continuidade do mineiro, primeira fatia integrada em 2026-09-15.** Ao encerrar a tarefa, `MinerWork.tick` agora libera a claim do ramal no mesmo tique em que remove o job. `MinerWorkLifecycleTest.aClosedJobReleasesItsMineClaimOnTheNextTick` prova que nem o job nem a claim sobrevivem. Isto corrige apenas a limpeza de claim; alvo inalcançavel, ramo bloqueado, veio exaurido, fluido e retomada apos backoff continuam na matriz aberta de recuperacao.

**JAR atual 0.3.0 — gerado em 2026-09-17, 17:33.** Acumula **sete
entregas sem playtest**: o afrouxamento da reserva de estrada (P1.0), a
tarefa aberta pela peça que a obra espera (P1.1), a água recusada na escolha
do alvo (P1.2) e o cortador de pedra (B1).

**O que procurar no log do próximo playtest, em ordem de importância:**

1. **A casa sobe?** É a pergunta que fecha P1.0 + P1.1 juntos. Se a
   biblioteca passar de 628/628, os dois funcionaram.
2. **`which is flooded` deve sumir ou quase.** Eram **30** em 28 minutos.
   Se continuar alto, o P1.2 não pegou o caso real.
3. `Colony … lot columns: N survived every check` — era **313** na última
   sessão, contra 0 antes.
4. **`The mine sealed N face(s)`** deve continuar aparecendo: é o outro
   caso da água, e ele não foi tocado.

⚠️ **Jogue 15–20 minutos sem fechar** — a varredura precisa de ~17 ciclos e
o relatório de lote só sai ao parar o servidor.

**Verificado:** `build` passou; **902 unitários, 0 falhas** (XML conferido);
**340 GameTests** (+2: `aFloodedSpotIsNeverAPlaceToStand` e
`theStonecutterMakesStairsFromOneCobblestone`), **4 rodadas verdes em 5** —
a falha é o KF-002, pré-existente e alheio. Conferido dentro do JAR que
`isDry`, `cutFor` e `cheaperOf` estão nas classes compiladas.

Copiado para `downloads/` e `%APPDATA%/.minecraft/mods/`; SHA-256 nas três
cópias: `D49961B2209BD121D1DABBEE229AFE05F1BB2A1B768CB3D3C17EFF5ECD560594`
(o anterior era `530B209A…`).

*JAR anterior, para referência: `build`, 884 unitários e 335/335 GameTests;
incluía P0.7, a limpeza imediata da claim, as duas correções do playtest de
09-15 (toco órfão e minério recusado), a retirada do baú da boca da mina e
as duas otimizações do planejador.*

---

## ✅ P1.5 — O índice de ruas era um beco sem saída (corrigido 09-19)

**O conserto:** o índice que dá **quatro voltas seguidas** sem um único
lote é descartado, e a passagem seguinte varre o chão de novo. A conta
morre assim que um lote aparece.

**Uma volta vazia NÃO derruba**, e essa metade é o conserto de um furo que
a bateria pegou: a primeira versão descartava na primeira volta, e dois
testes caíram — `removingAPlayerRoadRemovesOnlyThatIndexedColumn` e
`theCompletedSweepLeavesTheRoadColumnsIndexed`. Eles estavam certos: a vila
muda, o jogador abre espaço, e o lote de ontem existe amanhã. O que não é
normal é a **repetição**.

**Conferido por mutação nas duas metades:** nunca descartar falha 1 teste,
descartar na primeira volta falha 4.

### ✅ CONFIRMADO EM JOGO — sessão 02:19→02:47 de 09-19

A cadeia inteira saiu no log, em ordem e com um minuto de intervalo:

```text
02:22:33  dropped its road index — 5 columns asked over 4 rounds
          and no lot came of any. The next pass sweeps the ground again
02:23:33  planned minecraft:village/desert/houses/desert_small_house_6
          at [-344, 71, 584] — 322 blocks, 7 builders
```

E a varredura voltou a existir, que era o sintoma:

| | antes | depois |
|---|---|---|
| `sweep` | 32 runs, **0 passes over 0 columns** | 11 runs, **7 passes over 6.407 columns** |
| lotes aprovados | 0 | **30 survived every check** (5% de 646) |

O beco sem saída está desfeito: o índice foi descartado depois de quatro
voltas vazias, a varredura correu, e a casa foi planejada na passagem
seguinte.

---

### O diagnóstico original

**A assinatura, medida em duas sessões seguidas:**

```text
sweep: 32 planner runs, 0 passes over 0 columns, 32 answered by the index
       — 0 restarts, 0 complete rounds
```

**O laço, lido no código:**

```text
1. há índice          -> findAmongRoads, e o return é INCONDICIONAL
2. o índice se esgota -> Optional.empty()
3. o chamador já deu return  -> a varredura NUNCA roda
4. o índice NÃO é invalidado por ter falhado
5. passagem seguinte: volta ao 1
```

`BuildSiteScanner` linha 366: `if (roads != null) return findAmongRoads(...)`.
O índice só é descartado quando **o centro se muda** (`rebasedTo`) ou
quando **uma coluna é consumida** — nunca por ter dado a volta completa
sem achar lote.

**Onde a volta completa é detectável:** linha 734,
`ROAD_CURSOR.remove(colonyId)`, com o comentário *"Perguntou a todas:
aqui, e só aqui"*. É a única saída do laço que significa **índice
esgotado**, e é diferente de parar no meio por orçamento
(`MAX_COLUMNS`), que é legítimo e deve continuar voltando ao índice.

**Por que descartar é o certo, e está escrito na própria base:** o javadoc
do `ROADS` diz que o índice *"só nasce de uma varredura que visitou o raio
inteiro"* — ele **promete cobertura completa**. Dar a volta inteira sem
lote é a promessa falhando, e a resposta é refazer a varredura, não
reperguntar à mesma lista para sempre.

**E a outra saída também está fechada.** A Regra 15 manda a rua crescer
quando não há lote, e nesta sessão ela disse `found no road end it may
pave` **12 vezes**. A vila está com as duas saídas bloqueadas ao mesmo
tempo, e é por isso que o gargalo migrou para *"volume ocupado"* (39,7%)
sem nunca chegar a varrer chão novo.

⚠️ **Investigado, não corrigido.** O conserto é pequeno — descartar o
índice no ponto da volta completa — mas mexe na porta de entrada de todo
planejamento de obra, e a régua de orçamento (`MAX_COLUMNS`) precisa
continuar distinguível do esgotamento. Decisão do autor.

---

## ✅ A obra gravada no save também passa pelo portão — 19:29→19:57

### O acavalamento voltou, e eu sei por quê

**A obra não foi escolhida nesta sessão** — o log prova: zero varreduras,
zero recusas, zero `planned`. Ela foi **retomada**:

```text
resumed desert_fletcher_house_1 — 2 blocks already standing, 238 to go
```

**O portão de caixa de 15:49 governa quem ESCOLHE lote. A retomada não
passava por ele** — então um lote ruim escolhido por uma versão anterior
sobrevivia a todo conserto do scanner e voltava a cada carregamento do
save.

**Entregue:** a retomada confere a caixa contra o que já está de pé, e
larga a obra **intocada** que pisa em outra. Só a intocada — casa pela
metade é do jogador, e abandoná-la deixaria esqueleto no mundo com o lote
ocupado, que é a mesma razão que o descarte por planta superada já
escrevia.

### E a obra parou por falta de madeira, não por defeito

A linha nova disse em uma leitura:

```text
could not make minecraft:fletching_table — needs 4 minecraft:oak_planks and has 0
```

A colônia tem **zero madeira** — estoque só de pedra e areia —, o
lenhador passou a sessão `looking for a tree` e **nenhuma árvore foi
derrubada**. É uma vila de deserto sem floresta ao alcance.

**O viveiro plantou 4 mudas de carvalho** nesta sessão. Elas precisam
crescer; é a plantação de 09-19 funcionando, mas madeira leva tempo.

### E a reserva do cru está visível no estoque

`SANDSTONE=51` e `SMOOTH_SANDSTONE=51` — exatamente empatados, que é o
ponto de equilíbrio da regra.

**Verificado:** build verde, **947 unitários** (+4), **370 gametests**,
2 de 3 rodadas (a falha é a instabilidade conhecida, não determinística).
Mutação nas duas pontas do portão — cego e sem altura.

---

## ✅ CONFIRMADO EM JOGO — 18:42→19:11

**As quatro entregas funcionaram, e cada uma tem linha própria no log.**

| entrega | prova |
|---|---|
| um pouco de cada | **68× `stone`** e **20× `smooth_sandstone`** na mesma sessão |
| arenito cortado | **`made 4 minecraft:cut_sandstone`** — o bloco que travava horas |
| reserva do cru | estoque com `SANDSTONE=28` **e** `SAND=18` **e** `CUT_SANDSTONE=2` convivendo |
| mensagem de craft | `needs 4 minecraft:sand and has 0` — nomeia o que falta |
| acavalamento | `49 the colony had already built there` |
| regra dos 90% | `310 the ground is not at street level` |

**E a obra saiu de `WAITING_RESOURCES` para `BUILDING`** — 282 → 240
blocos. Três obras planejadas na sessão.

### Um retoque que a própria instrumentação cobrou

`smooth_sandstone — this game has no recipe for it` era **impreciso**: o
`billFor` percorre **bancada e cortador**, não a fornalha. O jogo tem a
receita; ela é de fornalha, e quem a executa é o fundidor por outro
caminho.

A linha passa a distinguir <i>"vem da fornalha, e o fundidor é o dono"</i>
de <i>"nem bancada nem fornalha fazem"</i> — a diferença entre *a planta
pede o impossível* e *a peça é de outra profissão*.

**Verificado:** build verde, **943 unitários**, **370 gametests**.

---

## ✅ A fornalha mantém um pouco de cada — 2026-09-19, decisão do autor

> *"o fundidor deve assar um pouco de cada para ter todos tipos de
> blocos, e focar em um quando solicitado pela obra"*

**A lacuna que isto fecha.** A meta de fornalha nascia **só** da obra
aberta: sem obra pedindo, o fundidor não assava nada, e a primeira casa a
pedir arenito liso esperava a fornalha começar do zero. Com obra pedindo,
ele assava **aquilo** até o estoque virar um tipo só — o defeito de
17:15, com 162 lisos e nenhum cru.

**O conserto tem a forma que a pedra já usava desde 08-27:**
`Math.max(piso, o que a obra pede)`. Piso de **16** — é "um pouco de
cada", não uma despensa — e a obra passa por cima quando pede mais.

**Vidro e ferro ficam de fora**, e a bateria cobrou isso: os dois já têm
meta própria que puxa o cru por trás — a de vidro abre meta de **areia**
pelo que falta. Pôr piso neles fazia a colônia pedir areia sem obra
nenhuma querendo vidraça, e **cinco testes** disseram isso na primeira
tentativa. O pedido é sobre bloco de construção; vidro e ferro são peça
de janela e de ferramenta.

**Um teste antigo foi substituído, não consertado:**
`nothingToSmeltOpensNoGoal` protegia o desenho velho — meta de fornalha
só de obra aberta —, que é exatamente o que esta decisão muda. Virou
`theFurnaceKeepsALittleOfEachWithoutAnyWork`, e ele afirma **as duas
metades**: o piso sem obra, e a obra mandando quando pede mais.

**E a mutação pegou um teste meu que não media nada:** eu comparava o
resultado com `SMELTED_FLOOR`, então com o piso em **zero** os dois lados
se moviam juntos e a afirmação passava. Agora afirma contra um número.

**Verificado:** build verde, **943 unitários**, **370 gametests**, duas
rodadas. Mutação nas duas metades — piso desligado e piso ganhando da
obra.

⚠️ **Espera playtest.**

---

## ✅ P1.8 resolvido — o fundidor comia o arenito do pedreiro (09-19, 17:15→17:44)

**A instrumentação de 17:06 pagou na primeira sessão** — e a primeira
coisa que ela pegou foi um erro **meu**:

```text
could not make minecraft:cut_sandstone — this game has no recipe for it
```

**Era falso.** O `billFor` recebe o predicado do baú, então devolve vazio
tanto por *não haver receita* quanto por *faltar ingrediente* — as duas
causas que a classe existia para separar. Consertado: pergunta ao livro
**sem filtro** antes de acusar o jogo.

### A causa real, no estoque

```text
SMOOTH_SANDSTONE=162 ... e NENHUM sandstone cru
waiting for minecraft:cut_sandstone   ← 39×
```

**O fundidor assou o estoque inteiro.** O arenito cortado sai do **cru**,
e não sobrou nenhum.

**O conserto é a regra de 09-05 aplicada à pedra:** metade do cru fica
sem ser assado, para o pedreiro lavrar. `ColonyGoals.rawToKeep`.

**E a conta saiu errada na primeira versão**, com o próprio teste a
cobrando: `(raw - processed) / 2` reserva **zero** num estoque empatado —
40 crus e 40 lisos liberavam os 40. A reserva é metade do **total**.

### E o acavalamento: o portão só via obra TERMINADA

O portão de caixa de 15:49 consultava `BUILDINGS`, que é o registro das
obras **prontas**. A obra em andamento não está nele — e era exatamente o
caso: a obra do `cut_sandstone` ficou meia hora parada ocupando o
terreno, **invisível** para o portão. Agora as obras abertas entram na
mesma pergunta.

**Verificado:** build verde, **942 unitários** (+4), **370 gametests**,
duas rodadas. Mutação na reserva.

⚠️ **O cenário do acavalamento ficou sem gametest**, e isso está dito
porque é dívida: toda versão que escrevi derrubou o
`ColonyDetectionGameTest` de forma **determinística** (3 em 3), e isolei
que o código novo passa nas duas rodadas **sem** ele. O teste mexe em
registros compartilhados e precisa de um isolamento que ainda não achei.

---

## 🔴 P1.8 — O pedreiro nunca recebe tarefa de arenito (achado 09-19, 16:10→17:06)

**As duas correções de 15:49 seguraram:** nenhuma obra acavalada, nenhuma
casa voando, e o cacto chegou ao baú (`CACTUS=3`).

**Mas a obra passou 55 minutos parada** em `waiting for
minecraft:cut_sandstone` — 22 vezes —, com **190 arenitos no baú**.

### O que o log prova

```text
SANDSTONE=190, SMOOTH_SANDSTONE_SLAB=6, SANDSTONE_WALL=6
no minecraft:cut_sandstone in the colony chests   ← 24×
```

Tarefas abertas na sessão inteira: **18 BUILD, 3 COLLECT_STONE, 1
COLLECT_WOOD**. **Zero `CRAFT_STONE_MATERIAL`** — o pedreiro nunca
recebeu tarefa, então `produceForWork` nunca rodou.

**E a lousa mente por omissão:** `ColonySupply.craft` devolvia `false`
**em silêncio**, e três causas moravam nesse silêncio — *não há receita*,
*falta ingrediente*, *não cabe no baú* — que pedem consertos opostos.

### Entregue: a lousa passa a dizer

`CraftReasons` nomeia a causa, uma linha por item e por motivo. É o mesmo
movimento que decidiu o P1.3 e o P1.6 numa leitura.

**E o que já dá para afirmar sem jogo:** a receita de `cut_sandstone`
**existe** e **fecha só com arenito** — há gametest afirmando as duas
coisas. Então o conserto é a **produção**, não a planta.

### O que NÃO é o defeito

**As obras não estão sendo desistidas.** Nenhum construtor largou obra, e
o projeto sobreviveu aos 55 minutos. O que se vê é a obra parada em
`WAITING_RESOURCES` para sempre.

Quem desistiu foi o **mineiro**: 3 largaram `COLLECT_STONE` por não
alcançar a pedra a y=37 — outro defeito, e ele não é a causa desta
parada, porque arenito não falta.

⚠️ **Espera playtest** — a próxima sessão dirá a causa numa linha.

---

## ✅ A obra acavalada e a casa voando — 2026-09-19, 15:21→15:49

**O cacto funcionou:** `potted_cactus` sumiu da lista de espera.

### Dois defeitos vistos em jogo, e eles tinham a mesma raiz

**1. A obra nova nasceu em cima de uma casa pronta.** A Regra 22
consultava `isColonyBuilt(ground)` — **uma posição por coluna**, a do
chão encontrado — e a conferência de volume começava *acima* dela. Um
prédio cuja caixa cobrisse a coluna em outra altura escapava das duas.

O conserto pergunta **caixa contra caixa**, que é a forma do que se quer
impedir. Uma vez por lote, no último portão.

**2. A casa ficava voando.** A `ROAD_LEVEL_TOLERANCE` é **por coluna**, e
nada exigia que as colunas concordassem entre si — o javadoc dela já
descrevia o vão como limite conhecido.

> *"aceitar uma base da construção que tenha mais de 90% dos blocos no
> mesmo nível"*

**E a segunda decisão do autor é o que faz a regra funcionar:** medir o
nível **entre as colunas**, não contra a rua, e assentar a casa no nível
da **base**. Medir contra a rua reprovaria o lote inteiro um acima dela —
o caso que o autor mandou aceitar em 09-15 e que o
`oneBlockOffTheRoadLevelIsStillALot` protege. E sem mover o piso, um lote
todo um acima teria 100% no mesmo nível, passaria, e continuaria voando.

**Verificado:** build verde, **938 unitários**, **369 gametests** (+2),
duas rodadas. Mutação nas três pontas — incluindo a que devolve o voo.

**A bateria pegou o conflito antes de mim:** a primeira versão media
contra a rua e derrubou o teste de 09-15. Os dois estavam certos, e a
pergunta foi para o autor em vez de eu escolher.

⚠️ **Espera playtest.**

---

## ✅ A economia do cacto — 2026-09-19, 14:47→14:59

**O P1.4 foi confirmado em jogo:** `no mason work` caiu para **zero**, e
as variantes novas saem por demanda — `sandstone_wall`,
`smooth_sandstone_slab`, `cut_sandstone` 3×.

### As três perguntas do autor: não, não e não

Não havia **uma linha** de cacto ou vaso no mod. E a obra parou em
`waiting for minecraft:potted_cactus` com 148 blocos por pôr.

**Ela ia esperar para sempre:** `potted_cactus` **não tem item** — no
Minecraft o vaso com cacto só existe como bloco. A colônia esperava algo
que não pode existir. São **oito** blocos assim no catálogo: os cinco
vasos, `water`, `lava` e `water_cauldron`.

### Duas metades independentes

**1. Bloco sem item é montado, não esperado.** Pergunta ao jogo
(`asItem() == AIR`) em vez de crescer a lista de quatro nomes escritos à
mão — ADR-009. Destrava os oito de uma vez.

**2. E é montado do que a colônia TEM.** Montar de graça seria a colônia
criando recurso, que a primeira regra do Construction-System proíbe. A
cadeia inteira, toda com receita do jogo:

```text
argila (colhida) → tijolo (fornalha) → vaso (bancada, 3 tijolos)
cacto  (colhido, só o topo — deixar a base É o replantio)
vaso + cacto → potted_cactus, montado no lugar
```

**O replantio não é um método:** cortar só o topo e deixar a base faz o
cacto crescer de novo dali. É a Regra 7 do lenhador aplicada a outra
planta.

**Verificado:** build verde, **938 unitários**, **367 gametests** (+2),
duas rodadas. Mutação nas duas metades.

**A mutação cobrou de novo:** desligar `hasNoItemOfItsOwn` deixava os 366
verdes — o conserto do defeito real não tinha teste. Escrevi o cenário da
obra de um bloco de lava, e aí quebra.

**E um teste-guarda me cobrou a declaração:**
`theFurnaceMakesGlassIngotsSmoothSandstoneAndStone` falhou porque `BRICK`
entrou na fornalha. É o serviço dele — a lista da fornalha não muda em
silêncio.

⚠️ **Espera playtest.**

---

## ✅ A CASA SUBIU — 2026-09-19, 14:01→14:22

**`Builder stopped — the house is up`.** A primeira casa terminada depois
de toda a sequência de consertos.

| o que foi pedido verificar | resposta |
|---|---|
| arenito recolhido? | **sim** — itens no chão de 660 → 254 |
| alguém transforma? | **sim** — 287 `smooth_sandstone`, 17 `cut_sandstone`, 15 escadas |
| viveiro do fazendeiro | **3 árvores** plantadas em terra enraizada na borda |
| cama de qualquer cor | **funcionou** — `made 1 minecraft:white_bed`, e `green_bed` sumiu do log |

O rebento saiu **carvalho** num deserto, e está certo: `VillageBiomes`
declara `DESERT → OAK`, que é a mesma tabela que decide a tábua da obra.

## 🔴 P1.4 RESOLVIDO — o pedreiro não tinha material declarado

A obra seguinte parou em `waiting for minecraft:cut_sandstone` com **178
blocos** por pôr, e o log dizia:

```text
no mason work: no task open for it — 1 able to
assigned 0 tasks (2 open)   ← 12 vezes
```

**A causa é a frase que o `STONE_BRICKS` já escrevia:** *profissão sem
material declarado nunca recebe pedido*. Das **dez** variantes de arenito
que a casa do deserto pede, só **duas** eram `ResourceType` — `SANDSTONE`
e `SMOOTH_SANDSTONE`. As outras oito não tinham entrada: sem meta, sem
tarefa, e o pedreiro olhando.

Os 9 `smooth_sandstone_stairs` daquela sessão vieram do caminho
oportunista do fabricante, não de demanda — por isso a produção era fina
e irregular enquanto a obra esperava.

**Entregue:** as sete variantes declaradas como `CRAFTED_STONE` (o
pedreiro), mais um **mapeamento por nome** no adaptador — sem ele a
declaração seria invisível para o jogo, que é pior que não declarar
porque *parece* conserto.

**Verificado:** build verde, **938 unitários** (+3), **365 gametests**
(+1), duas rodadas. Mutação nas duas metades.

**E a bateria me corrigiu de novo:** meu gametest novo criou o batch
`desert_masonry` e o `ColonyDetectionGameTest` passou a falhar — *"esperava
ao menos 30 trabalhadores, achei 24"*. Isolei: os 364 passam com **todo**
o código novo e **sem** a anotação nova. Aquele cenário conta aldeões
registrados num orçamento de três passagens, e um batch a mais muda o
escalonamento. A afirmação foi para um batch que já existia, e a
classificação virou teste unitário, que não precisava de mundo nenhum.

⚠️ **Espera playtest.**

---

## ✅ Quatro entregas do autor — 2026-09-19, 13:41

### O P1.7 funcionou, e a obra chegou perto do fim

| | 12:28 | 13:41 |
|---|---|---|
| itens no chão | 660 | **254** |
| obra | 318 blocos faltando | **12** |
| `pulling ... forward` | — | **20×** |

**E a transformação do arenito está acontecendo:** 287 `smooth_sandstone`,
17 `cut_sandstone`, 15 `smooth_sandstone_stairs`.

### As quatro regras novas

**1. Cama de qualquer cor.** A obra parou a **12 blocos do fim** esperando
`green_bed` — 17× —, numa vila cujo colono produz lã **branca**. Entra
pela mesma porta das peças de madeira (`ItemTags.BEDS`), então demanda e
parede concordam por construção.

**2. Madeira de qualquer espécie: já existia** desde 09-05, e a medição
mostrou que o problema era outro — a obra esperava `jungle_door` num
deserto com **dez toras ao todo**. Não falta substituição; **falta
árvore**.

**3. A primeira obra de toda vila é uma casa.** A cota de roça é por
população, então uma vila que nasce com gente bastante abria a roça antes
da primeira casa.

**4. O viveiro do fazendeiro.** Terra enraizada + rebento na borda, a
espécie saindo do bioma pela mesma tabela que decide a tábua da obra.
Plantado quando ele varre o raio e não acha lavoura, com intervalo de
cinco minutos.

**Verificado:** build verde, **935 unitários**, **364 gametests** (+7),
duas rodadas. Mutação nas três regras novas.

**Um teste antigo caiu e estava certo:**
`theHouseGoesUpAfterTheFarmStepsAside` montava o impasse com a colônia
sem prédio nenhum — exatamente o caso que a regra nova manda ser casa. O
cenário ganhou o prédio anterior; o que ele mede continua igual.

**E a mutação achou um buraco meu:** a regra da casa-primeiro passou sem
teste que a medisse — desligá-la deixava os 363 verdes. Escrevi o
cenário irmão, e aí a mutação quebra.

⚠️ **Espera playtest.**

---

## 🔴 P1.7 — O baú cheio destrói o que a obra espera (achado 09-19, 12:09→12:28)

**A sessão em que a vila finalmente construiu, e parou por outro motivo.**
107 lotes aprovados (22%), **2 obras planejadas** — e a obra travou em
`WAITING_RESOURCES ... waiting for minecraft:sandstone`, com **318 de 323
blocos** por pôr.

**Ao mesmo tempo, 365 sandstone eram jogados no chão.**

```text
Miner chest at [-1364, 63, 1410] is full — dropped 1 of minecraft:sandstone
```

660 itens destruídos, 668 avisos de baú cheio. **Um** baú, com vinte
livres ao lado.

**A assimetria é a raiz:** o construtor **LÊ** todos os 21 baús
(`ColonySupply.take`), o mineiro só **ESCREVE** no próprio.

**E o fundidor era sintoma, não causa.** A linha
`none of 21 colony chests had sandstone to smelt` estava **certa**: a obra
pede `smooth_sandstone`, que é `SMELTED` a partir de `sandstone` — e o
sandstone estava no chão. Um conserto resolve os dois.

### ✅ O conserto: puxar, não transbordar (decisão do autor)

> *"quando o baú da profissão passar da metade do preenchimento, as outras
> profissões devem forçar a criação de itens que futuramente serão
> utilizados para criar as estruturas do bioma"*

**É melhor que as três opções que eu tinha levantado.** Transbordar para o
baú do vizinho resolve o sintoma e adia a causa — o baú seguinte enche
também. Puxar ataca a causa: a matéria vira peça, o baú esvazia porque
quatro areias viram um arenito, e a obra encontra pronto o que ia esperar.

**A lacuna que ele fecha:** a demanda hoje só nasce de **obra aberta**.
Sem obra — ou com ela parada esperando —, ninguém produz nada, e a colônia
só começa a fabricar depois que a obra pede, que é tarde demais.

Mede **vaga ocupada, não item guardado**: um baú com 27 pilhas de uma
unidade está cheio, e medir por item diria que estava vazio.

**Verificado:** build verde, **935 unitários** (+4), **357 gametests**
(+3). Mutação nas três: borda `>=` em vez de `>`, nunca chamar, e medir
item em vez de vaga.

⚠️ **Espera playtest.**

---

## 🟠 P1.6 — O gargalo mudou de dono: Regra 22 sem amostra (09-19, 10:36→11:02)

**O P1.5 está funcionando** e isso está medido: 39 passagens sobre
**39.163 colunas**, índice descartado 2×, 2 voltas completas. A varredura
voltou a existir.

**Mas a vila não planejou obra nenhuma** — `0 survived every check` de 532
candidatas. E a recusa dominante agora é outra:

| recusa | quantas | % |
|---|---|---|
| **algo dentro do volume (Regra 22)** | **371** | **70%** |
| janela vertical | 65 | 12% |
| Regra 3 | 50 | 9% |
| nível de rua | 46 | 9% |

**A Regra 22 é 70% das recusas e não diz de que é feita.** É a mesma
cegueira que o `ProtectionSample` desfez para a Regra 3 em 09-18 — e ali
a amostra decidiu o P1.3 numa leitura. Sem ela, "algo está no caminho"
pode ser cacto, pode ser a própria casa vizinha, pode ser mato: três
consertos opostos.

### ✅ A amostra foi construída (09-19)

`VolumeSample` separa as **duas causas** que dividiam a linha, e elas
pedem consertos opostos:

| causa | o que significa | o conserto |
|---|---|---|
| `COLONY_BUILT` | a própria colônia já construiu ali | procurar mais longe / crescer a rua |
| `IN_THE_COLUMN` | tem bloco físico na coluna | depende **do bloco**, e agora ele é nomeado |

A linha nova sai junto do `LotRefusals`:

```text
Rule 22 turned lots down — N the colony had already built there,
                           M had something in the column — 120 cactus; 45 sandstone; ...
```

Conta **por posição, não por visita** — a lição de 09-18, em que o
`ProtectionSample` relatou "1548 chest" numa vila de três camas porque a
varredura repassa a mesma coluna a cada ciclo.

### ✅ RESPONDIDO EM JOGO — 12:09→12:28

```text
Rule 22 turned lots down — 10 the colony had already built there,
                           0 had something in the column
```

**E a Regra 22 deixou de ser o gargalo:** de 70% das recusas para **2,7%**
(10 de 376). A vila aprovou **107 lotes (22%)** e planejou **2 obras**.
Nenhuma das 10 foi bloco no caminho — era a colônia achando a própria
obra, que é o comportamento correto.

A amostra fez o trabalho dela: respondeu numa leitura e saiu da frente.

---

## ✅ Duas entregas do deserto — 2026-09-19, pedido do autor

### A areia da planta vira arenito

**Emenda à Regra 27, decidida pelo autor** — a terceira (pedra em 08-26,
espécie de madeira em 09-05, areia agora). As casas do deserto pedem
`sand` **junto com nove variantes de arenito**; a areia é o punhado do
piso e da moldura, e ela **cai** — casa entregue com areia na parede
desaba no primeiro vizinho quebrado.

**A troca é na PLANTA, e essa foi a decisão de projeto que quase saiu
errada.** Trocar só no `BuilderWork` faria a obra **pedir areia** ao baú e
**assentar arenito**, porque a demanda sai de `remainingMaterials()`, que
conta o bloco da planta. Conta e parede discordando é exatamente o defeito
de **2026-08-22**. Trocando no único ponto em que a planta é montada
(`StructureBlueprintReader`), as três pontas voltam a falar do mesmo
bloco.

Fora do deserto **nada muda**: a areia da praia de uma vila de planície é
da planta dela.

### O mineiro espera a areia assentar

Areia e cascalho caem; enquanto caem, aquele espaço não é pedra nem ar —
é `FallingBlockEntity`. Cavar durante a queda é cavar no escuro: o alvo
desce, o buraco se reenche, a picareta bate no ar. No deserto isso é a
regra, não a exceção.

**Decisão do autor:** espera assentar e **recalcula o alvo** — o que caiu
ocupou o lugar, e insistir na pedra de antes seria bater onde não há mais
nada. A areia que desce é minério que chega sozinho até a mão dele.

Pergunta pela **entidade**, não pelo bloco: *"está caindo agora"* em vez
de *"vai cair"*. Paciência de 100 tiques impede que uma queda infinita
(areia sobre água, gerador de jogador) aposente o mineiro.

**Verificado:** `build` verde, **931 unitários** (+5), **352 gametests**
(+3), **duas rodadas verdes**. Mutação nas duas direções de cada um: não
trocar / trocar em todo estilo; nunca esperar / esperar para sempre —
esta última é a que produziria um mineiro que nunca mais cava.

⚠️ **Espera playtest.**

---

## ✅ O leitor de log mentia em três itens de uma vez (corrigido 09-19)

**O falso positivo:** a assinatura de refutação `"0 survived every check"`
casava dentro de `"30 survived every check"` — a linha de **sucesso** da
sessão. `str.count` não sabe onde começa um número, então o `0` do `30`
servia de assinatura.

**Três itens saíram REFUTADO** — P0.7, P1.0 e P1.3 dividem essa refutação —
na sessão em que a vila planejou casa. O veredito honesto é **0 refutados**.

É o pior tipo de erro de ferramenta, porque o número errado era plausível:
eu quase fui investigar três regressões que nunca existiram. E não era só o
30 — qualquer contagem terminada em zero (10, 20, 100) dispararia a mesma
refutação para sempre.

**O conserto:** assinatura que começa com dígito só casa em início de
número (`(?<![0-9])`). O texto continua literal, que é o que o
`test_verdict.py` confere contra `src/main`.

**Mutação nas duas metades:** voltar ao `str.count` falha o teste do falso
positivo; cegar o casamento falha o teste de que o zero **de verdade** ainda
refuta. Sem a segunda metade, "nunca casar nada" passaria — e a ferramenta
ficaria cega em vez de afinada.

---

## 🔴 P1.4 — O pedreiro existe e não tem tarefa (achado 09-19)

**A investigação do pedreiro terminou, e a resposta não era a que eu
supunha.** A linha `hiring` saiu quando o autor andou pela vila:

```text
Colony 9c9e465f hiring — MASON at target x61; SMELTER at target x61;
                         CARPENTER at target x61
no mason work: no task open for it — 1 able to
Colony 9c9e465f — MASON b06ae217 claimed the chest at -324, 66, 600
```

**O pedreiro existe, tem baú, e está apto.** `at target` queria dizer *"a
vaga já está preenchida"*, não *"a vaga não abre"*. O que falta é
**tarefa** — e tarefa de pedreiro nasce de obra aberta, que é o P1.3
seguinte.

**Minha hipótese de 09-18 estava errada** e ficou registrada como tal: eu
disse "a vila não tem pedreiro". Tem. O caminho `cut_sandstone → pedreiro`
está inteiro; quem não fecha é `obra → tarefa`.

---

## ✅ P1.3 confirmado, e o gargalo mudou de dono — 2026-09-19, 1 hora

**A correção da Regra 3 funcionou, e o número é direto:**

| | Regra 3 | total de recusas |
|---|---|---|
| 09-18 (antes) | **69%** — 7.821 | 11.284 |
| 09-19 (depois) | **5,5%** — 2.774 | 50.568 |

A amostra caiu junto, de 12.651 blocos protegidos para **235**:

```text
Rule 3 protected these blocks — 105 smooth_sandstone; 88 sand;
                                34 sandstone; 4 chest; 2 hay_block
```

O que sobra ali é chão residual mais **peça de verdade** — baú e fardo de
feno —, que é exatamente o que a Regra 3 deve proteger.

### O novo topo, e ele não é o mesmo defeito

```text
20073 (39,7%)  something stands inside the house's volume   <- Regra 22
14716 (29,1%)  the ground is inside a reserved road area
 8589 (17,0%)  the ground is not at street level
 4008 ( 7,9%)  no ground in the village's vertical window
 2774 ( 5,5%)  Rule 3
```

**Zero lotes aprovados em 50.568 candidatas** — a vila continua sem
construir, por outra razão.

⚠️ **Não investigado, e há uma pista forte:** `348 planner runs, 0 passes
over 0 columns, 348 answered by the index — 0 complete rounds`. A colônia
**nunca varreu o terreno** nesta sessão: todas as 348 passagens foram
respondidas pelo índice de ruas, que oferece só as colunas de beira de rua
já conhecidas. As 50.568 recusas saem desse conjunto estreito, e num
deserto plano com a vila apertada contra as casas existentes o volume da
planta quase sempre esbarra em alguma coisa.

**A linha `hiring` não saiu** — o caminho de contratação não rodou em
nenhuma das 348 passagens, então a investigação do pedreiro continua sem
resposta. Isso é informação: `Assigned N professions` também não apareceu,
e as duas ausências juntas dizem que a colônia não tentou contratar
ninguém nesta hora.

---

## A casa do deserto começou e parou — 2026-09-18, 20:40

**Veredito da sessão: 8 verificados, 0 refutados.** A obra andou de 323 para
**256 blocos** — 67 assentados, a primeira casa a subir no deserto.

### Por que parou: a vila tem UM trabalhador

```text
21x  stopped — no minecraft:cut_sandstone in the colony chests
 7x  stopped — no minecraft:sand in the colony chests
 3x  no miner work: no worker in the village can do it
```

**Não é falha de cobertura.** O levantamento diz `nobody-makes-these: none`,
e os donos existem: `cut_sandstone` é do **pedreiro**, `sand` da
**superfície**, `sandstone` do **pedreiro** — todo o arenito tem quem o
faça. O que falta é **gente**: `Equipped 1 workers`, cinco vezes. A colônia
tem um aldeão, ele virou construtor, e não sobrou ninguém para minerar.

**É o P1.2 outra vez, noutro material** — `OAK_PLANKS needs CRAFT_WOOD` na
planície, `cut_sandstone` no deserto. A causa comum é população, não
catálogo.

### A placa invisível: estava no telhado

**Causa medida, não suposta.** A placa ficava em `origin.y + size.y` — o
**topo** da planta. As alturas do deserto, medidas pelo levantamento:

```text
desert_medium_house_2  11x8x7     <- a obra desta sessão
desert_small_house_6   6x18x5     <- dezoito de altura
desert_small_house_4   5x5x5
```

A placa da obra nasceu **8 blocos acima do chão**, fora do campo de visão de
quem está ao lado dela. Corrigido para `LABEL_HEIGHT = 2.5` — acima da
cabeça do jogador, abaixo de todo telhado do catálogo. O nome flutuante
atravessa bloco, então não precisa de céu aberto para ser lido.

⚠️ **Espera playtest.**

---

## ✅ P1.3 — No deserto a Regra 3 trancava a vila (CONFIRMADO EM JOGO 09-18)

**Playtest de 20:21, mesma colônia `9c9e465f` das duas sessões que deram
zero:**

```text
planned minecraft:village/desert/houses/desert_small_house_6
  at ColonyPos[x=-342, y=70, z=594] — 322 blocks, 4 builders,
  drawn from 1 of that footprint
opened a build task — 322 blocks left
```

**Mesmo mundo, mesma vila, mesmo centro.** Antes: 16.016 colunas, zero
aprovadas, nenhuma obra. Depois: obra planejada e aberta. A comparação é
controlada — só o jar mudou.

**E a linha do sorteio apareceu pela primeira vez:** `drawn from 1 of that
footprint`. Um, e é o esperado — a colônia não tem casa de pé, então o
`smallestFirst` põe a menor pegada na frente e nesse tamanho o deserto tem
uma planta só. A instrumentação de 09-18 funciona; a variedade entre irmãs
ainda espera uma vila com casa terminada.

⚠️ **O que esta sessão NÃO mostrou:** a obra subindo. Foram 4 minutos e ela
nasceu no fim — `322 blocks left` é o estado inicial, não progresso. Falta
ver bloco assentado no deserto.

---

### Como estava antes da correção

**A amostra decidiu, e a suspeita estava certa.** Playtest de 19:45 com a
instrumentação nova:

```text
Rule 3 protected these blocks — 5696 smooth_sandstone; 5403 sand;
                                1548 chest; 2 oak_log; 2 oak_leaves
```

**87% é areia e arenito** — o terreno em que a vila foi assentada. O gerador
inclui o chão na caixa da estrutura, e no deserto esse chão é quase tudo.

**O conserto:** `BuildSiteScanner.isBiomeGround` separa chão de peça, e a
Regra 3 deixa de recusar solo natural. É o mesmo movimento de 09-15 em
`isReservedAgainstLots`, que separou calçamento de reserva e abriu 313 lotes
na planície. Baú, tronco, porta, cama e **escada/laje/muro de arenito**
seguem intocáveis — arenito inteiro é chão, arenito lavrado é construção.

**Conferido por mutação nas duas metades:** soltar tudo falha 2 testes,
segurar tudo falha 1. Um teste que afirmasse só uma metade passaria com a
proteção inteira apagada.

⚠️ **Espera playtest** — a correção não foi vista em jogo.

---

### O diagnóstico original (19:01–19:33)

**Sessão do autor num mundo só de deserto, 32 min.** O mod detectou o bioma
certo — `Village style desert has 28 houses` —, varreu **o raio inteiro** e
**nenhuma obra nasceu**.

```text
lot columns: 0 survived every check, 11284 were turned down
  7821 (69%)  village-original or player-placed, and Rule 3 protects it
  2316 (21%)  the ground is inside a reserved road area
   930 ( 8%)  the ground is not at street level
   217 ( 2%)  no ground in the village's vertical window
sweep: 47 planner runs, 17 passes over 16641 columns, 1 complete rounds
```

**Não é falta de orçamento de varredura**, e essa distinção importa porque
foi a dúvida que atrasou o P1.0: **1 rodada completa** sobre 16.641 colunas.
A varredura terminou e a resposta foi zero.

**A Regra 3 recusa 3,6× mais no deserto que na planície:**

| | Regra 3 | reserva de estrada | aprovadas |
|---|---|---|---|
| planície (09-18 01:43) | 19% | 53% | 313 |
| **deserto (09-18 19:01)** | **69%** | 21% | **0** |

**A causa suspeita** é `BlockProtection.isVillageOriginal`, que pergunta
`structureContains` — se o bloco está dentro da **caixa delimitadora** da
estrutura da vila. No deserto as casas são baixas e espalhadas, então a caixa
cobre boa parte da areia **vazia** entre elas, e areia vazia não é construção
de ninguém. ⚠️ **Suspeita, não medida:** falta confirmar que os 7.821 caem em
chão vazio dentro da caixa, e não em casa de verdade.

**O que isto ensina sobre o P1.0.** O afrouxamento de 09-15 atacou a reserva
de estrada, que era o topo na planície, e funcionou lá (0 → 313). No deserto o
topo é outro, e o conserto anterior não alcança — **cada bioma tem seu
gargalo**, e o mod só foi medido em planície.

---

## O veredito de sessão — 2026-09-18

**O gargalo não era medir, era ler.** A instrumentação já é rica — `SweepLog`,
`LotRefusals.accepted`, o motivo de parada de cada trabalhador. O que faltava
é que ninguém lia o log depois da sessão: a frase do P1.1 estava lá **23
vezes** e ninguém a tinha visto, e o E46 ficou dias escondido numa linha já
escrita.

`scripts/verdict.py` fecha o laço. Joga-se, roda-se o script, e ele responde
por item: **VERIFICADO**, **REFUTADO** ou **NÃO EXERCITADO**. O terceiro é o
que não existia, e é o mais útil: diz o que a próxima sessão precisa cobrir,
em vez de deixar o item envelhecendo numa fila indistinta.

`scripts/test_verdict.py` confere que cada assinatura existe em `src/main` —
uma assinatura errada faria o item sair como "não exercitado" para sempre, que
é o pior veredito possível porque parece inofensivo.

**A fila de 19 encolheu na primeira rodada:** 7 verificados, 1 refutado, 2 não
exercitados — sem jogar nada, contra o log que já existia.

**Duas assinaturas minhas estavam erradas, e o exame as pegou:**

- **P1.1** usava `no worker in the village can do it`, que sai 2× por profissão
  numa vila que simplesmente não tem aquele trabalhador. "Não há pedreiro aqui"
  não é "a peça não tem quem faça". Corrigida para citar a capacidade de
  fabricação.
- **E45** usava `is out of reach` como refutação, e pedra inalcançável é rotina
  de mineração. O defeito era a **ausência** de `went one level deeper`, não a
  presença de recusas. Com 29 descidas medidas, o item está **verificado**.

---

## 🔴 P1.2 — Tábua sem carpinteiro (achado pelo veredito, 09-18)

`OAK_PLANKS needs CRAFT_WOOD`, 2× na sessão de 01:43–02:25. A obra espera
tábua e a vila não tem quem a fabrique. **É o P1.1 refutado**: a correção de
09-17 tratou a peça que não é `ResourceType`, e este é outro caminho — o
recurso existe, a capacidade não está na vila.

Não investigado. O próximo passo é descobrir se é falta de aldeão livre, de
baú, ou se o `ProfessionAssigner` não está criando carpinteiro.

---

## A vila levantava sempre a mesma casa — 2026-09-18

**O autor viu em jogo:** *"está criando sempre a mesma estrutura, porém
precisa alterar mais, criar casas"*.

**A causa não era o sorteio, era o que sobrava para sortear.** O sorteio
entre plantas de mesma pegada existe desde 09-09 e está em
`ConstructionPlanner.open` — mas ele nunca teve mais de uma opção. A lista
que chega ali vem de `HousePlans.catalogPlans`, que corta as pegadas
repetidas (`sizes.add(plan.size())`) e depois corta em `PLANS_OFFERED = 4`.
Das **36 peças de planície** sobravam **4**, uma por pegada, e o planejador
levanta a `get(0)`. As oito `small_house` do jogo colapsavam em **uma**.
Sortear entre plantas de tamanhos todos distintos, filtradas pelo tamanho
do lote, é sortear entre uma — a mesma casa, toda passagem, toda sessão,
toda vila do mesmo bioma.

**O corte é legítimo e ficou.** Ele serve à busca de lote, que o comentário
do `PLANS_OFFERED` registra em dez minutos. O que mudou é **quando** as
irmãs voltam: `HousePlans.siblingsOf` é chamada **depois** de o lote estar
achado, com a pegada já conhecida e a medição encerrada. A varredura não
ficou um byte mais cara.

**E a pasta `houses` não é só casa.** O gerador do jogo põe ali tudo que um
lote pode receber — cerca de bicho, ponto de encontro, templo, estábulo, a
peça decorativa da planície. Decisão do autor: a colônia levanta
**moradia**. `HousePlans.isDwelling` filtra por substring e não por lista de
nomes, porque os nomes do jogo não têm convenção entre biomas
(`butcher_shop`/`butchers_shop`, `mason_1`/`masons_house_1`,
`weaponsmith`/`weapon_smith`). Medido nos cinco estilos: planície 36→24,
taiga 27→22, savana 31→23, nevada 30→25, deserto 28→21 — **nenhuma moradia
cai**. A roça sai da lista de casas e **continua no catálogo**, porque
`FarmPlans.farmsFor` lê da mesma pasta.

**Um defeito achado na própria correção, antes de commitar.** A primeira
versão de `siblingsOf` casava a pegada só pelo eixo do arquivo, e quem
chama compara o tamanho **depois** do giro da Regra 17. Numa planta
retangular isso fazia duas coisas erradas: trazia a irmã para ser
descartada logo adiante, e — pior — escondia do sorteio a irmã que **só
cabe girada**. A variedade voltaria a morrer justamente nas plantas
retangulares. Corrigido por `fitsEitherWay`, que casa os dois eixos do chão
e exige a altura igual, com unitário próprio
(`theSiblingThatOnlyFitsRotatedStillCounts`).

**O que foi verificado rodando:** `gradlew test` — **912 unitários, 0
falhas, 0 erros**, `HousePlansTest` de 11 para 15; `gradlew runGametest` —
**343 gametests, todos passaram**, `village_catalog` de 5 para 7. Uma rodada
da bateria falhou uma vez e as **quatro seguintes passaram**, consistente
com a instabilidade já registrada da bateria, não com este ciclo.

**O que NÃO foi verificado:** nada disto foi visto em jogo. Os testes provam
que as irmãs existem no catálogo real e que o filtro não come moradia; eles
**não** provam que a vila agora levanta casas visivelmente diferentes ao
longo de uma sessão. Isso é playtest, e é o próximo passo. O
`smallestFirst` **não foi tocado** — decisão do autor de manter a exceção de
arranque de 09-15 —, então enquanto nenhuma casa **terminar** a colônia
continua começando pela menor pegada; a variedade nova aparece **dentro**
dessa pegada.

---

## A placa da obra em português — 2026-09-17

Pedido do autor: *"o identificador visual dos elementos que faltam para a
estrutura ser construída deve estar escrito com o nome dos blocos em
português, sinalizando quantos tem nos estoques e quantos faltam"*.

**Metade já existia.** A placa flutuante sobre o lote (`SiteMarker` +
`SiteLabel`, de 09-15/09-16) já mostrava estoque e falta. O que faltava era
o **nome**: ela dizia `grass_block` onde o jogo diz *Bloco de Grama*.

**Agora:** `SiteLabel.of` recebe uma função de nomes, e o `SiteMarker` passa
`Block.getName()` — a tradução do próprio Vanilla. Nada de tabela própria,
que envelheceria e quebraria em outro idioma.

```text
antes:  Obra · falta grass_block: 17/64 · 382 blocos
agora:  Obra · falta Bloco de Grama: 17/64 · 382 blocos
```

⚠️ **A função entra por parâmetro por causa da ADR-005:** `SiteLabel` é
`core` e não conhece Minecraft. O `fabric` passa a tradução; os testes
passam o que quiserem, e a regra segue afirmável sem servidor.

**Prova em dois níveis:** 3 unitários para a regra, e **1 gametest** que
pergunta ao Vanilla de verdade — porque regra provada só com nome de
mentira não prova que o jogo traduz.

`[FATO]` O log do autor já traz `Pedra`, `Terra` e `Andesito` pela mesma
chamada, o que confirma que o cliente dele está em português.

---

## Melhorias de 2026-09-17 — água na escolha do alvo, e o cortador de pedra

Duas implementadas, as duas saídas da pesquisa em
[`opcoes-de-melhoria-das-profissoes.md`](research/opcoes-de-melhoria-das-profissoes.md).

**P1.2 — água (30 travamentos medidos).** `BuilderApproach.isDry` entrou no
`standable`, o predicado que **mineiro e construtor compartilham**. Água tem
caixa de colisão vazia e passava por `passable`; agora a coluna alagada é
recusada **na escolha**, e não depois de 300 tiques de caminhada.

⚠️ **O conserto de 2026-09-03 continua, e é outro caso.** `MineFlooding.seal`
tapa a fonte que a **picareta** abriu — agiu 5 vezes no log, com acerto. O
que ele não alcança é a água que já estava lá: a picareta nunca chega a
bater. Um cobre 1 caso em 6; juntos cobrem os dois.

**B1 — cortador de pedra.** `CraftingLookup.cutFor` lê
`RecipeType.STONECUTTING`, e `cheaperOf` escolhe a receita que gasta menos
**por peça**. A escada de pedregulho cai de **6→4** para **1→1**.

⚠️ **Duas versões minhas foram corrigidas pela medição, e ficam registradas:**
o `isDry` perguntava também pela camada da cabeça e reprovou um teste do
mineiro (4 verdes em 4 no baseline provaram que era regressão, não flaky); e
o cortador entrou primeiro como *segunda pergunta*, o que não economizava
nada porque a bancada responde antes.

**Verificado:** build; **902 unitários, 0 falhas**; **340 GameTests
(+2), 4 rodadas verdes em 5** — a falha é o KF-002, pré-existente.

---

## Ciclo de 2026-09-17 — P1.1 corrigido e revisão completa do projeto

**P1.1 corrigido** (`4cee104`): a obra agora abre a tarefa do fabricante
**pela peça que espera**. A causa era `ColonyCycle.requestMissing` iterar
`Map<ResourceType,Integer>` — e escada, porta e cerca não são recursos
contados, então sumiam do planejador por mais pedregulho que houvesse.
Declarar mais um `ResourceType` seria a terceira vez, e a ADR-009 §2 chama
isso pelo nome.

⚠️ **O ponto de chamada foi escolhido depois de uma tentativa errada, e
vale o registro:** primeiro pus no `wakeIfSupplied`, dentro do
`ConstructionPlanner` — e o gametest falhou. A instrumentação temporária
mostrou que o método **nunca era chamado**: o planejador só roda para as
colônias da vez no rodízio, oito por ciclo. Obra parada esperando escada não
pode depender de sorteio. A chamada foi para o ciclo por colônia.

**Revisão completa do projeto**, pedida pelo autor:
[`docs/technical/Revisao-2026-09-17.md`](docs/technical/Revisao-2026-09-17.md).
391 arquivos `.md`, 40.838 linhas, 23 parados desde 08-08. Três documentos
descrevem classes que **nunca existiram** (`ColonyManager`, `TaskManager`,
`BuildingStatus`). O `Development-Log.md` ganhou cabeçalho de histórico; os
outros dois já tinham.

**As oito profissões passam** — 338 GameTests, 5 rodadas verdes em 5. Mas a
cobertura é desigual: **79 testes para o mineiro, 2 para o pastor.** Quatro
estão provadas em jogo (mineiro, lenhador, carpinteiro, fazendeiro); as
outras quatro passam nos testes sem terem tido oportunidade no playtest.

---

## Playtest de 2026-09-17, 08:23–08:51 — o afrouxamento abriu chão, e a obra travou noutro ponto

28 minutos. **O P1.0 andou**, e o número mostra:

```text
lot columns: 313 survived every check, 370716 were turned down
```

**313 colunas aprovadas, contra zero na sessão anterior.** A reserva de
estrada caiu de 54,8% para 49,6% das recusas, e **uma obra foi planejada** —
a primeira em quatro playtests.

### Mas ela não subiu um bloco, e a causa é outra 🔴 P1.1

```text
08:43:36  planned plains_library_1 at 2498,64,-2981 — 628 blocks
08:44:36  WAITING_RESOURCES … waiting for minecraft:cobblestone_stairs
          (e assim por 8 minutos, 628 de 628)
Builder … stopped — no minecraft:cobblestone_stairs in the colony chests
```

**A colônia tem 69 `COBBLESTONE` no baú e ninguém os transforma em
`cobblestone_stairs`.** É o laço fechado que
[`analise-plano-crescimento.md`](../analise-plano-crescimento.md) descreveu
em 09-12 por outra porta — a obra pede uma peça que o mod sabe fazer, e a
tarefa de fabricá-la não abre.

O log ainda traz, no mesmo ciclo:

```text
no collect_stone work: no worker in the village can do it — COBBLESTONE needs COLLECT_STONE
no craft_wood_material work: no worker in the village can do it — OAK_PLANKS needs CRAFT_WOOD
```

⚠️ **Não é falta de material: é falta de quem transforme.** O estoque tinha
4.258 tábuas, 1.686 toras e 69 pedregulhos.

### E a água trava o mineiro — confirmado 🔴 P1.2

O autor: *"quando aldeão encontra água ou lava ele se perde e trava"*. **O
log concorda, e a assinatura é exata:**

```text
Miner … gave up the stone at 2455,20,-2999 — it has not moved a block in
300 ticks … the place to stand is 2455,21,-2999, which is flooded
```

**15 desistências** por `which is flooded`, e `flooded` aparece 30 vezes.
O guarda de imobilidade dispara (300 tiques parado) e devolve a tarefa — o
aldeão não afoga, mas perde 300 tiques toda vez.

**O que já existe:** `MineDigging.flooded` vira o ramal na hora
(`The branch turns away from the water`, 5 ocorrências). **O que falta é o
que o autor pediu:** tapar o bloco que verte e seguir por outro caminho, em
vez de ficar parado até o guarda expirar. Lava não apareceu nesta sessão
(`lava` = 0).

---

## Playtest de 2026-09-17, 00:34–01:17 — a instrumentação respondeu: zero

42 minutos, 2.093 linhas. **A linha nova deu a resposta que a sessão curta
não podia dar:**

```text
Colony 9da5460c lot columns: 0 survived every check, 960672 were turned down — 0%
```

**Zero colunas aprovadas em quase um milhão testadas.** Não é orçamento de
varredura: **a vila não tem um palmo de chão que sirva.** A distribuição é
idêntica à da sessão de 6 minutos (54,8% / 18,5%), então não foi amostra
pequena.

| Recusa | % |
|---|---|
| **reserva de estrada** | **54,8%** |
| Regra 3 protege | 18,5% |
| fora do nível da rua | 9,1% |
| algo no volume da casa | 6,2% |
| sem chão na janela | 5,9% |
| chão não natural | 5,5% |

**E a vila está num círculo fechado:** sem lote, a regra manda estender a
estrada; a estrada não consegue crescer (`none of the road ends this colony
can see may be paved`, 23 tentativas). Sem estrada nova, não há lote novo.

**O mineiro segue vivo:** `Miner … took` = 5, `turning the helix` = 8,
`tried all 4 helices` = 2. O guarda girou e trocou a boca em vez de travar.

### O afrouxamento, decidido pelo autor em 09-17

A reserva de estrada passa a valer **só para a rua que a colônia calçou**.
`BuildSiteScanner.isReservedAgainstLots` é nova e substitui `isRoadArea`
**apenas na recusa de lote**.

⚠️ **Por que uma função nova, e não mudar a que havia.** `isRoadArea` é
usada em **dois sentidos opostos**: em três lugares responde *"isto é rua, a
casa pode encostar aqui"* e num quarto *"isto é reserva, não construa"*.
Afrouxar a única função afrouxaria as duas, e a colônia deixaria de
reconhecer as ruas Vanilla — que é justamente por onde ela cresce.

**A Regra 3 continua inteira:** quem protege a vila do jogador é a recusa
`PROTECTED`, uma pergunta adiante. Bloco original que não é calçamento segue
intocável.

⚠️ **Expectativa honesta, e ela é menor que os 54,8%.** `dirt_path` não é
bloco sólido cheio, então o calçamento de terra continuará caindo em
`NOT_NATURAL_GROUND` logo adiante. Quem passa a poder virar lote é o
calçamento de **gravel e terracotta**. **Quanto disso existe na vila do
playtest, não sei** — o próximo log é que dirá, e a linha `lot columns` mede
exatamente isso.

---

## Playtest de 2026-09-17, 00:09–00:17 — E45 CONFIRMADO CORRIGIDO, e um terceiro defeito

Log: 1.317 linhas, 8 minutos. Relato do autor: *"não vi as zonas de
construção marcadas, não vi casa crescendo"*.

**O E45 está morto, e os números provam:**

| Medida | 21:41 (antes) | 00:09 (agora) |
|---|---|---|
| `Miner … took` — pedra quebrada | **0** | **37** ✅ |
| `hit stone with nowhere to stand` | 17.518 | **15** |
| `turning the helix` — o guarda novo | — | **4** |

O guarda girou a hélice quatro vezes em vez de travar, e o mineiro passou a
produzir. **É o primeiro playtest desde 09-15 com pedra saindo do mundo.**

**Mas o E46 não pôde ser testado.** A colônia rodou 6 minutos, o planejador
pediu lote 8 vezes e **nenhuma obra chegou a ser planejada** — `planned` = 0,
`opened a build task` = 0. O E46 conserta a obra que morre *depois* de
nascer; aqui nenhuma nasceu. Por isso não houve zona marcada nem casa.

**P1.0 — o terceiro defeito, e ele é anterior aos outros dois.**

```text
no building work: still sweeping — the budget ran out before an answer
sweep: 8 planner runs, 2055 columns, 0 complete rounds
lot refusals: 174912 candidates turned down
  92972 (53%) the ground is inside a reserved road area
  33597 (19%) village-original or player-placed, Rule 3 protects it
  17051 (10%) the ground is not at street level
```

**53% das recusas são a reserva de estrada**, e a causa está em
`BuildSiteScanner.isRoadArea`: ela trata **todo caminho original da vila
Vanilla** como reserva (`BlockProtection.isVillageOriginal`). Numa vila
gerada, o calçamento é justamente onde há chão plano. Com a Regra 3 somada,
**72% do território está bloqueado por proteção**.

⚠️ **Duas leituras possíveis, e a sessão foi curta demais para separá-las:**
a varredura precisa de ~17 ciclos (8 min) para fechar uma volta e teve 6, e
`0 complete rounds` diz que ela nunca terminou. Pode ser orçamento, pode ser
falta de chão. **Ver [[lento-nao-e-travado]]** — já errei este diagnóstico
antes.

**Instrumentado em 09-17 para decidir sem chutar:** `LotRefusals.accepted`
conta as colunas que sobrevivem a **todas** as recusas, e o relatório agora
sai com as duas linhas:

```text
Colony … lot refusals: N candidates turned down — …
Colony … lot columns: N survived every check, N were turned down — N% of what was looked at
```

**Zero aceitas** ⇒ a vila não tem um palmo livre, e alguma recusa precisa
afrouxar. **Poucas aceitas** ⇒ há chão, e o problema é o orçamento da
varredura. São consertos opostos, e o número decide qual.

---

## Playtest de 2026-09-16, 21:41–22:12 — E45 reproduzido, E46 descoberto

Log: `%APPDATA%/.minecraft/logs/latest.log`, 37.041 linhas, 31 minutos.

**O E45 reproduziu com a mesma assinatura:** 17.518 `hit stone with nowhere
to stand`, 10 por segundo sem interrupção, **zero** `Miner … took`, **zero**
`went one level deeper`, e `stall/still/adrift` em **0/0/0** a sessão
inteira. A linha de recusa agora traz a medida que fechou o caso:
**`0 digger(s) in 1 open branch(es)`**, nas 17.517 vezes.

**E a causa da primeira análise estava errada.** O reinício por
`deepenIfEveryOpenArmIsDone` + `restartAt` **nunca acontece** — se
acontecesse, `went one level deeper` apareceria (na superfície `mayDeepen()`
é verdadeiro), e `no branch work` não apareceria. O laço é a **mesma
passagem repetida**, não um reinício. `MineFrontier` também foi descartado:
`The gallery really ends at` aparece **1** vez contra 17.518 recusas.

**E46 — a obra fecha sozinha, e é outro defeito.** Biblioteca planejada duas
vezes, duas posições, as duas mortas em ~30 s com **628/628 blocos**. Em
03:44 o builder parava por falta de cobblestone; aqui **não é falta de
material** — é o projeto sumindo debaixo do builder. Ver §8.1 do documento.

**Dois achados de fundo:** 36 colônias ativas, `Colony cycle took` até
**486 ms** (40 ciclos acima de um tique); e só a colônia `9da5460c` planeja
— as outras 35 dão `assigned 0 tasks (0 open)`.

---

## Playtest de 2026-09-16, 03:44–08:23 — a mina presa na boca (P0.8)

Log: `latest.log`, 341.196 linhas, JAR 0.3.0.

**97,6% do log é um laço só:** 166.559 `hit stone with nowhere to stand` mais
166.558 `no miner branch work`, **10 por segundo, por 16.657 segundos sem uma
interrupção**. E os dois números que fecham o caso: **zero** `Miner … took` —
nenhuma pedra saiu do mundo — e **zero** `went one level deeper`.

**O custo não é o log, é a vila parada:** cobblestone 411 → 10 sem reposição,
builder parado 46 vezes por falta dele, a obra em `2442,64,-2972` morta com
**12 blocos restantes**, e **zero construções concluídas em 4h40**.

**A causa.** Com o poço ainda não cavado, `branchesOpenNow()` vale 1, e o
braço 0 sozinho satisfaz `everyOpenArmIsDone()`. Ele fecha na oitava recusa,
`deepenIfEveryOpenArmIsDone` fecha os outros três — **que nunca foram
trabalhados** — e `restartAt` devolve `cut = 0`: o cursor volta à boca, no
mesmo bloco emparedado. A aritmética torna o laço inevitável: **o poço exige
120 posições (`CARVED`) e o cursor nunca passa de 8 (`BLOCKED_BEFORE_TURNING`)**.

**Duas hipóteses descartadas, registradas para não voltarem:** não é o conserto
do `ColonyEdits` (só age depois que a picareta pega, e ela nunca pegou); não é
o lenhador via `reopenFrom` (~1 árvore/55 s não sustenta 10/s); não é o ramo do
fundo da mina (todos os Y do log entre 59 e 65, superfície).

**E o defeito de fundo:** `stall 0/2400`, `still 0/300`, `adrift 0/400` ficaram
**zerados as 4h40** — o caminho de falha zera os contadores no mesmo tique em
que falha. **Guarda que o caminho de falha zera não é guarda**, e esta é a
terceira volta do mesmo laço (19.193 linhas em 02:58; nove desistências em
09-11; 166.559 agora).

Análise completa, correção em quatro itens e o que falta decidir:
[`docs/technical/E45-mina-presa-na-boca.md`](docs/technical/E45-mina-presa-na-boca.md).

---

## Playtest de 2026-09-16, 03:31 — a casa acavalou na roça da vila

Log: `latest.log`, 03:19-03:31, JAR `df0c704d`.

**Relato do autor:** *"a segunda construcao acavalou em cima de uma fazenda da
vila, entao a verificacao do local para construir deve ter dado erro"*. Ele
estava certo, e a causa era **ordem de operacoes**, nao o filtro de solo.

**O que o log mostrou.** O acougue foi planejado em `2503,63,-3045`, limpou
plantas **ali**, e dezoito segundos depois riscou **onze** posicoes de
`farmland` em `z=-3031..-3033` — **catorze blocos** alem da origem, numa planta
de onze de profundidade. Uma caixa continua nao faz isso.

**A causa: a planta era girada DEPOIS de o lote ser aprovado.** O filtro em
`ConstructionPlanner.open` comparava `plan.size()` com `site.size()`, e so
entao a Regra 17 girava a planta para a porta olhar a rua. Num retangulo o giro
de 90° **troca os eixos**: a casa 13×11 aprovada num lote 13×11 virava 11×13 e
ocupava treze blocos de profundidade onde onze foram verificados. O excedente
caia em terreno que ninguem olhou — no caso, a comida da vila virando piso de
casa.

**Duas correcoes, porque o furo tinha dois lados:**

1. `open` gira **antes** de filtrar, entao o filtro ve a pegada que a obra vai
   de fato ocupar.
2. `sizesOf` passa ao scanner **as duas orientacoes**. Sem isso, o filtro
   corrigido nao acharia planta nenhuma para um lote 13×11 quando todas viram
   11×13, e a vila deixaria de construir **em silencio** — troca de um defeito
   visivel por um mudo.

**Hipotese descartada pelo caminho, e fica registrada:** suspeitei que
`isLotGround` aceitasse `farmland` por ela ser solida. Escrevi o teste, e ele
passou **sem patch** — `farmland` nao e `isSolidBlock`, e o scanner ja a
recusava por `NOT_NATURAL_GROUND`. Os dois GameTests ficaram como regressao.

**Verificacao:** `build` verde com **884 unitarios** e **335/335** GameTests
(2 novos), bateria repetida ate **duas rodadas limpas seguidas** — a primeira
teve o AUD-001 conhecido, sem relacao com construcao.

---

## Playtest de 2026-09-16, 02:58 — a casa sobe, e tres defeitos

Log: `latest.log`, 02:23-02:58, JAR `9f479423`. **A casa esta sendo
construida** — a obra foi de 61 para 37 blocos restantes. Nenhuma terminou.

**1. Porta por ultimo (`BuildOrder`).** Pedido do autor. O construtor
**risca** o que nao tem apoio (*"skips ... nothing holds it"*), e a porta fica
na base da parede: a ordem de baixo para cima a colocava **antes** do batente,
e ela era perdida — a casa terminava sem porta. Agora sao tres grupos:
estrutura, porta, mobilia. A porta vem antes da mobilia porque a cama decide a
cabeceira contra a casa pronta, e o vao da porta precisa ja existir.

A ordem saiu do leitor de estrutura para o Core: era decisao sem teste proprio,
porque o leitor precisa de servidor para rodar.

**2. O construtor travava ao subir na obra (`ClimbLimit`).** Pedido do autor:
*"nao fazer uma diferenca maior de 2 blocos"*. O log mostra por que:

```
on the way to 2515, 67, -3054
the worker is at 2506, 68, -3051
the lot floor at 2515, 63, -3054 is Pedregulho
```

Ele estava em **y=68**, em cima da propria obra, e `footOf` mandava sempre ao
**piso do lote, y=63** — cinco blocos abaixo. Um aldeao desce degrau de um; a
navegacao Vanilla nao anda para uma queda dessas, e ele ficava parado ate o
guarda devolver a tarefa. Agora, quando o pe da coluna esta longe demais na
vertical, o destino vira o **patamar** a dois blocos, e a passagem seguinte o
leva mais um degrau.

**Andaime nao foi preciso.** O alcance do construtor ja ignora a vertical de
proposito — ele constroi do chao ao telhado —, entao o problema nunca foi
alcancar: era o **caminho de volta**. Fica registrado porque o autor pediu as
duas saidas, e a segunda so se justificaria se a primeira nao bastasse.

**3. A enxurrada de 19.193 linhas (`ColonyEdits`).** O log tinha **7 MB**, e
quase metade era a mesma linha: *"Miner d492ef6b hit stone with nowhere to
stand"*, dez por segundo, um unico mineiro, por trinta e dois minutos.

`PlayerWorldChangeHandler` reabre o ramal da mina quando o mundo muda perto do
tunel — existe para o **jogador** abrir caminho. Mas reagia a qualquer
mudanca, sem perguntar quem a fez, e quem mais mexe no tunel e o proprio
mineiro: cada pedra tirada reabria o ramal com a contagem de recusas zerada, o
ramal batia na mesma pedra sem lugar de ficar de pe, e fechava de novo. A mina
**nunca desceu** naquela sessao — `went one level deeper` nao aparece uma vez.

Agora a colonia anuncia a propria picareta, e a marca **vale uma leitura**: se
ficasse, o jogador que depois mexesse ali seria ignorado para sempre, e
trocariamos a enxurrada por um silencio pior.

**4. Lenhador construindo NAO e defeito.** O autor perguntou; a ADR-011 decidiu
que *"qualquer produtor pode construir temporariamente sem trocar de
profissao"* (`WorkAssignment.canPerform`). O nome sobre a cabeca continua
dizendo o oficio dele, que e o correto.

**5. Orientacao — decisao do autor: corrigir por bloco, nao por arquitetura.**
A ADR-005 decidiu que `Blueprint` **nao** guarda `BlockState`, e hoje o mod
infere a direcao pela posicao na caixa (`BuilderWork.facing`): acerta parede,
erra escada e tronco deitado. O autor escolheu tratar caso a caso. **Fica
pendente ate ele dizer quais blocos viu virados errado** — sem isso eu estaria
adivinhando qual bloco corrigir.

**Verificacao:** `build` verde com **882 unitarios** (18 novos) e **333/333**
GameTests, bateria repetida **tres vezes, todas limpas**.

---

## 2026-09-16 — baus da vila e o gargalo real da cadeia

**A MEDICAO QUE MUDOU O ESCOPO.** O autor pediu que *"tudo que falta para a
construcao entre na cadeia produtiva"*. Antes de abrir a frente, medi o que as
obras esperaram na semana:

| material | esperas | a colonia sabe fazer? |
|---|---|---|
| `dirt` | 93 | **sim** |
| `grass_block` | 42 | **sim** |
| `lectern` | 19 | nao |
| `smooth_stone_slab` | 16 | sim |

**78% do problema e material que a colonia JA sabe coletar** e mesmo assim
falta. O `lectern` e 12%, e a cadeia dele exige couro (matar vaca) e papel
(cana) — duas profissoes que o mod nao tem. O autor escolheu atacar os 78%
primeiro.

**1. A varredura pagava por um circulo para usar um cone.** A coleta de terra e
grama procura **fora** da vila, num cone de 90° (Regra 3), mas `RingSweep`
varre um **circulo** de raio 48: 9.409 colunas contra orcamento de 1.024. Das
26 falhas de coleta no log, **24** eram *"still sweeping — the budget ran out —
dirt"*. Tres quartos de cada passagem eram gastos para descartar.

`RingSweep.around` ganhou uma sobrecarga com filtro barato, aplicado **antes**
do orcamento. O contrato e explicito: o filtro e aritmetica de coordenada,
nunca leitura de mundo — senao so moveria o custo de lugar.

**2. Baus da vila (`VillageChests`).** Decisao do autor: *"permitir que o
recurso que falta possa ser recolhido de qualquer bau que esteja na vila
automaticamente"*, com a ressalva **menos os marcados**.

O bau privado e o que o **jogador nomeia** (`VillageChestRule`): nomear exige
bigorna e e ato deliberado, e e simetrico ao que o mod ja faz com aldeao desde
08-08 — nome dado a mao e intocavel. Nome padrao do jogo ("Chest", "Baú") nao
conta como nome, senao a regra seria letra morta.

**"Na vila" quer dizer DENTRO de uma casa**, e esta restricao veio de nove
gametests quebrados. Um raio solto de 64 alcancava o bau da vila vizinha e, no
gametest, o das arenas ao lado: *"8 chests read"* onde se esperavam dois, e um
caso chegou a **construir com o bau vazio** porque lia o bau de outro teste. O
mesmo risco existe em jogo com duas vilas proximas. Agora vale o bau dentro da
caixa de uma construcao — peca de vila gerada ou obra da colonia —, reusando a
pergunta que `BlockProtection` ja faz.

**Verificacao:** `build` verde com **864 unitarios** (7 novos) e **333/333**
GameTests, bateria repetida **tres vezes, todas limpas**.

**Pendente e dito por inteiro:** o `lectern` continua sem cadeia. A frente de
couro/papel nao foi aberta, e enquanto nao for, a biblioteca nao sobe.

---

## Playtest de 2026-09-16, 01:19 — a casa subiu, e o lenhador a derrubou

Log: `latest.log`, 00:57-01:19, colonia `9da5460c`, JAR `37f943fc`.

**Progresso real:** a casa pequena de 64 blocos abriu as 00:58:21
(`plains_small_farm_1`) e o construtor chegou a assentar — de 64 para 55
blocos restantes. Depois uma biblioteca de 628 blocos foi planejada. A
preferencia pela planta menor e o desnivel de 1 bloco funcionaram.

**1. O DEFEITO GRAVE: o canteiro em obra nao era protegido.** Relato do autor:
*"confundiu colocar tronco com recolher tronco e plantar arvore"*. O log tem
as duas linhas na **mesma coordenada**, as 00:59:51:

```
lumberjacks: 4661287a walking — tree at 2503, 63, -3035, block 1 of 1
builders:    1 working, BUILDING at ColonyPos[x=2503, y=63, z=-3035]
```

O construtor assentava tronco na parede e o lenhador ia colhe-lo como arvore
— `block 1 of 1`, um tronco solto. **A casa caia enquanto subia.** Em seguida:
*"No OAK sapling at 2509,63,-3033 — Block{minecraft:oak_log} is in the way"*,
o lenhador tentando replantar no meio do canteiro.

**A causa era de momento, nao de regra.** `BlockProtection.isColonyBuilt`
pergunta ao registro de `Building`, e um `Building` so nasce quando a obra
**termina** — ou e abandonada. A obra **em andamento** nao estava em lugar
nenhum que a protecao consultasse, entao o canteiro ficava desprotegido
justamente durante as horas em que ha material solto nele. E o material solto
de uma casa de planicie e tronco de carvalho, que e o que o lenhador procura.

**Nao e regressao da decisao de 09-15** (lenhador so recolhe tronco): ele
cortava tronco de canteiro desde sempre. Ficou visivel agora porque a casa
finalmente chegou a subir. As duas perguntas juntas cobrem a vida inteira de
uma casa: obra aberta pelo registro de construcoes, casa pronta pelo de
construcoes levantadas.

**2. A placa saiu do nome do trabalhador.** Correcao de rumo pedida pelo
autor: *"os itens que faltam da obra deve ficar flutuando no espaco da
construcao e nao no lugar do nome do trabalhador"*. Ele esta certo — o nome do
trabalhador diz o oficio dele, e sobrescreve-lo trocava uma informacao por
outra.

Agora e um **suporte de armadura invisivel** sobre o centro do lote, acima do
teto da planta. A escolha foi entre ele e o `TextDisplayEntity` do 1.21: o
segundo e feito para isso, mas o texto so se escreve por NBT — nao ha setter —,
e montar NBT a mao e mais fragil que o `setCustomName` que o mod ja usa.

**O lixo no save era o medo, e esta tratado:** a placa e marcada com etiqueta
propria (suporte do jogador nao vira placa da colonia), procurada antes de ser
criada, e **removida assim que a obra fecha** (`clearStale`, que roda mesmo sem
jogador perto). `WorkerNameplate` ainda reconhece a marca antiga, para desfazer
a placa na cabeca de quem carregar um save da janela de 09-15.

**Verificacao:** `build` verde com **857 unitarios** e **333/333** GameTests
(1 novo), bateria repetida **duas vezes, ambas limpas**. **Pendente: validacao
em jogo.**

---

## Playtest de 2026-09-15, 23:41 — desnivel, vila perto e a placa da obra

Log: `latest.log`, 23:15-23:41, JAR `5c034262`.

**O marcador funcionou, e o log explica a duvida do autor** (*"nao vi bordas
de particulas"* seguido de *"vi as particulas"*): a unica obra da sessao abriu
numa vila **nova** (`9da5460c`, em `2503,63,-3031`) e ficou em
`WAITING_RESOURCES` esperando `grass_block` — entao o contorno era de
**fumaca**, nao de chama. O estoque daquela colonia era
`{OAK_LOG=21, OAK_PLANKS=86, COBBLESTONE=96, DIRT=17}`: zero grama.

**1. Desnivel de 1 bloco (`ROAD_LEVEL_TOLERANCE = 1`).** Decisao do autor:
*"permitir somente 1 bloco de desnivel da estrada"*. A regua era **exata**, e
o desnivel respondia por 35,0% e 33,6% das recusas em duas sessoes — a segunda
maior causa. Isto cumpre a condicao que o proprio autor impos em 09-11 na
pesquisa de terraplanagem §8.

**Um defeito que a mudanca expos:** `isClearAbove` contava o volume sempre a
partir de `roadY + 1`, o que estava certo enquanto toda coluna tinha o chao em
`roadY`. Com a tolerancia, a coluna um acima tem o proprio chao em `roadY + 1`
— e a pergunta lia esse chao como obstrucao, recusando por `OCCUPIED` o lote
que a regua acabara de aprovar. Agora conta a partir do chao **daquela**
coluna.

**O que NAO mudou:** a preparacao do canteiro nao aterra (so tira planta), e o
autor foi avisado. A coluna um abaixo fica com vao de um bloco sob o piso.
Aterrar continua sendo a frente de terraplanagem.

**2. Vila longe nao trabalha (`WORKING_DISTANCE = 128`).** Decisao do autor:
*"nao trabalhar nas vilas que o jogador nao esta perto"*. O log mostrava
**seis colonias** reportando atividade no mesmo periodo. Para o **ciclo
inteiro**, nao so o planejamento. O dobro do raio da vila, para a colonia nao
congelar quando o autor anda pela borda ou desce a mina — o centro oscila entre
oito posicoes, como 21:50 mediu.

`runCycleNow` (costura de gametest) **nao** aplica o filtro: o gametest nao tem
jogador, e tres casos de ciclo caiam em silencio dizendo "abriu 0 tarefas".
Quem afirma a regra e o caminho de producao.

**3. Placa da obra.** Pedido do autor: *"um texto igual o nome dos aldeoes
mostrando o material que falta, quantos tem em estoque e quantos falta"*.
`SiteLabel` (Core, testavel) monta a linha; quem a carrega e o **nome flutuante
do construtor** — o mesmo mecanismo do `WorkerNameplate`, e literalmente "igual
o nome dos aldeoes".

- Formato: `Obra · falta grass_block: 0/64 · 382 blocos`.
- **Texto, nao icone de item:** icone exigiria render no cliente (mixin,
  networking, entrypoint), e o mod deixaria de funcionar com cliente Vanilla —
  a mesma escolha que o `WorkerNameplate` fez em 08-08.
- **Nao usa ArmorStand:** seria entidade nova que persiste no save do jogador.
- **Um material por vez**, senao vira parede de texto sobre o lote.
- O estoque vem do que o ciclo **ja leu** (`SiteMarker.remember`); reler no
  tique da placa multiplicaria por trinta o custo da fase `chests`.

**Armadilha evitada:** `WorkerNameplate` so desfaz nome que o mod escreveu, e
a placa nao e rotulo de profissao — sem `SiteLabel.MARK`, o construtor ficaria
com a placa na cabeca **para sempre**.

**Verificacao:** `build` verde com **857 unitarios** (5 novos do `SiteLabel`) e
**332/332** GameTests, repetida ate duas rodadas limpas seguidas (a primeira
teve o AUD-001 conhecido). **Pendente: validacao em jogo dos tres.**

---

## Playtest de 2026-09-15, 22:54 — marcador, lenhador e a medicao do terreno

Log: `latest.log`, 22:43-22:54, colonia `111d6ee5`, JAR `79d9774c`.

**A prioridade da colonia observada funcionou:** 20 passagens do planejador
contra 16 na sessao anterior. A rua cresceu (22:47:09, 3 blocos oeste).

**1. Lenhador so recolhe tronco.** Decisao do autor: *"os lenhadores devem se
focar apenas em recolher todos troncos, nao devem recolher as folhas"*. Era o
contrario desde 2026-08-08. A conta explica: uma copa de carvalho tem ~80
folhas contra 6 troncos, e cada bloco custa os mesmos tiques de picareta — o
lenhador passava a maior parte do expediente quebrando folha.

**A copa continua sendo PROCURADA**, e isso e essencial: ela e a unica coisa
que separa arvore de construcao (`isNaturalLeaf`). O que muda e que ela nao
entra mais na colheita. A folha que sobra decai sozinha pelo Vanilla, e
`clearAbove` continua abrindo a coluna da muda.

**2. Marcador visual do lote.** Pedido do autor: *"um efeito que demonstre onde
no terreno esta o espaco alocado para a construcao escolhida"*. `SiteOutline`
(Core, sem mundo, testavel) da as colunas da borda; `SiteMarker` (Fabric)
desenha com `spawnParticles` do **servidor** — sem entrypoint de cliente, sem
networking proprio, sem travessia de thread.

- **Chama** = obra construindo; **fumaca** = obra esperando material.
- So a borda (o miolo esconderia o terreno) e so na altura do piso (subir ate
  o telhado viraria parede opaca).
- Uma vez por segundo (`EVERY_TICKS = 20`), so com jogador em 64 blocos.

**3. A MEDICAO QUE O AUTOR EXIGIU EM 09-11 ESTA PRONTA.** A pesquisa
`docs/research/terraplanagem-da-vila.md` §8 registrou a decisao dele: *"Medir
primeiro. Se a recusa por desnivel dominar, a inferencia vira fato e a frente
abre."*

| sessao | total de recusas | area de estrada | fora do nivel | soma |
|---|---|---|---|---|
| 21:50 | 187.152 | 46,7% | 35,0% | **81,7%** |
| 22:54 | 279.648 | 47,6% | 33,6% | **81,2%** |

**A condicao foi satisfeita, e o numero e estavel entre sessoes.** A frente de
terraplanagem esta autorizada pelo criterio do proprio autor; falta ele decidir
abri-la. A pesquisa ja tem o desenho (§6), as decisoes (§8: capacidade do
construtor, 4 blocos de desnivel) e os tres pontos de contato
(`BuildSiteScanner.flatGroundAt`, `RoadExtension.pave`, `SitePreparation`).

**Verificacao:** `build` verde com **852 unitarios** (5 novos do `SiteOutline`)
e **330/330** GameTests, bateria repetida **tres vezes, todas limpas**.
**Pendente: validacao em jogo do marcador e do lenhador.**

---

## Playtest de 2026-09-15, 21:50 — "nenhuma casa crescendo"

Log: `latest.log`, 21:30-21:50, colonia `111d6ee5`, JAR `1c362b8b`.

**A correcao da obra orfa funcionou:** as 21:42:41 o acougue foi liberado com
a linha nova (`lets go of ... outside the 64-block radius`).

**DIAGNOSTICO CORRIGIDO NO MEIO DO CAMINHO.** A primeira leitura do log foi
**errada** e esta registrada aqui porque custou uma tentativa de patch. Eu
concluira "impasse fechado: a rua nao cresce", e escrevi um patch que largava
o indice de ruas ao fim da volta vazia. **Onze GameTests quebraram** e
mostraram o porque: a Regra 15 (crescer a rua) roda exatamente nesse ponto — o
fim da volta sem lote — e o patch consumia o momento dela. Tudo foi revertido.

**O que o log diz de verdade:** a rua **cresce**. Tres extensoes em 20
minutos, as 21:45 (2 blocos oeste), 21:46 (3 norte) e 21:47 (5 sul). O sistema
funciona; esta lento demais para se ver.

**A causa da lentidao e a fila que eu criei na vespera.** A colonia teve vez do
planejador **16 vezes em 20 minutos**, porque `PlannerTurns` reparte 29
colonias em oito por ciclo. E **28 daquelas 29 estavam dormentes** — o log
registra uma unica colonia reportando atividade. A fila gastava a vez com quem
nao tinha o que fazer, enquanto a colonia observada esperava quatro ciclos.

**Correcao 1 — a colonia que o jogador ve fura a fila.** `PlannerTurns` ganhou
a sobrecarga `chooseFrom(active, watched)`, e `VillageDetectionHandler`
calcula `watched` pelo mesmo `SEARCH_RADIUS` da vila. **A cota nao muda**: as
observadas ocupam vagas dela, nao vagas a mais — alargar o orcamento
devolveria o pico de 214 ms pela porta dos fundos. O cursor do rodizio avanca
so pelas que entraram por rodizio, entao ninguem fica para tras.

**Correcao 2 — obra abandonada deixa de contar como casa.** Defeito **meu**, da
vespera: `smallestFirst` perguntava se `BUILDINGS.ofColony()` estava vazio, e
`WaitingWork.giveUp` registra a obra largada como `Building` para o lote nao
parecer livre. O save do mundo tem **56 buildings e zero** `house is up`, entao
a preferencia pela planta pequena **nunca dispararia**. `Building` ganhou o
campo `finished`, e so `BuilderWork.complete` o marca verdadeiro.

**Save retrocompativel:** a chave `finished` ausente vale **terminada**. O save
anterior a 09-15 nao a tem, e nele quase toda construcao e casa de verdade —
o registro so passou a receber obra abandonada em 09-12. Ler as antigas como
inacabadas faria toda vila ja construida voltar a preferir a planta pequena. O
preco e a obra abandonada de um save antigo contar como casa naquela colonia,
e isso se apaga na primeira casa que ela terminar.

**Verificacao:** `build` verde com **847 unitarios** (7 novos) e **330/330**
GameTests, bateria repetida **tres vezes, todas limpas**. **Pendente: validacao
em jogo.**

**Proxima frente decidida pelo autor:** menu do mod comecando por comando
(`/vc log`, `/vc watch <profissao>`), nao por tela. Ver a pesquisa na secao
seguinte.

---

## Playtest de 2026-09-15, 21:02 — nenhuma construcao nascendo

Log: `%APPDATA%/.minecraft/logs/latest.log`, 20:44-21:02, colonia `111d6ee5`.

**As otimizacoes do planejador funcionaram.** A Regra 3 caiu de 126.315 para
28.333 recusas, e o aviso `Colony cycle took` saiu de varios por minuto para
**um** na sessao inteira (214 ms as 20:45:35, no arranque). O gargalo do tique
deixou de ser o problema.

**A causa de nada nascer eram outras duas, e ambas foram corrigidas.**

**1. A obra ficou fora do raio quando o centro da vila derivou.** As 20:54:35 a
colonia abriu `plains_butcher_shop_2` em `638,65,-2793` — 382 blocos — e sete
minutos e meio depois continuava em *"382 blocks left"*, sem **um unico bloco
assentado**, segurando a vaga unica: *"no building work: one is already open"*.
Nao era falta de material: o estoque tinha 2.743 tabuas e 621 pedregulhos.

A aritmetica esta no proprio log. O centro da vila **nao e estavel** — a mesma
sessao registrou oito centros, de `616,-2863` a `640,-2891`, porque ele e
recalculado das camas vistas e camas entram e saem de chunk carregado. A obra
nasceu com o centro em `625,-2854`, a **62,4** blocos: dentro do raio de 64. O
centro que prevaleceu, `637,-2871`, a deixa a **78** — fora do raio, e portanto
fora do alcance de qualquer trabalhador.

Ninguem reclamava porque o relogio de paciencia so conta para
`WAITING_RESOURCES`, e esta obra estava em `BUILDING`; o passo do construtor sai
em silencio quando o aldeao nao esta em chunk carregado. Obra viva,
inalcancavel, calada e ocupando a vaga unica, as quatro coisas ao mesmo tempo.

`ConstructionProject.isOutOfReach` (Core, sem mundo) responde a pergunta, e o
planejador larga a obra antes do relogio de paciencia. **A planta nao leva a
culpa:** `WaitingWork.giveUp` ganhou o parametro `blamePlan`, porque marcar a
casa pelo primeiro material restante acusaria um item inocente — nao faltou
material nenhum. A casa pela metade e o lote continuam ocupados, como no
abandono por paciencia.

**2. A primeira casa da colonia passa a ser a menor.** Decisao do autor:
*"dar preferencia para a primeira ser uma casa pequena"*. Era a terceira sessao
seguida em que a maior planta do catalogo trava a vila antes de a primeira casa
existir. `HousePlans.smallestFirst` poe a menor na frente **enquanto a colonia
nao tem nenhuma construcao de pe**; levantada a primeira, a Regra 25 volta
inteira. A lista e **reordenada, nao encurtada**: se a pequena nao couber
naquele lote, a varredura desce para a seguinte.

A Regra 25 (2026-08-20) continua valendo e o motivo dela segue real — exigir a
casa grande em toda parte fez a vila parar de crescer. O que entrou e uma
excecao de **arranque**, e nao uma inversao: inverter de vez faria a vila virar
um bairro de cabanas.

**Verificacao:** `build` verde com **840 unitarios** (7 novos) e **330/330**
GameTests. Bateria repetida **tres vezes**: duas limpas e uma com o AUD-001
conhecido (`smeltergathers...` / *"fundidor criado no setor nao esta registrado
no ServerWorld"*), que nao tem relacao com construcao. **Pendente: validacao em
jogo das duas.**

---

## Playtest de 2026-09-15 — dois defeitos vistos e corrigidos

Log: `%APPDATA%/.minecraft/logs/latest.log`, 19:41–19:49, colonia `111d6ee5`.

**1. Lenhador deixava toco de 3 a 4 troncos de pe.** Relato do autor:
*"lenhador esta deixando 3 a 4 blocos de troncos sem cortar, todos troncos
devem ser cortados e replantar"*. O log mediu: **114** recusas `Not a tree`,
com a moda exata em **4 troncos** (34 dos 114). O lenhador **nao** abandonava
arvore no meio — as 18 que comecou, terminou, e o guarda de imobilidade de
09-12 segurou (stall 0–4). Os tocos nunca foram cortados: a regra da copa
viva (`TreeHarvester.plan`) recusa tronco sem folha `PERSISTENT=false` ao
alcance, e o tronco que **perdeu** a copa para o decaimento — o que sobra ao
lado de uma arvore derrubada — virava recusa permanente, com o castigo
crescendo a cada volta. Sem plano, tambem nunca era replantado.
**Decisao do autor (09-15):** toco sem copa cai quando o grupo e pequeno e
nao e protegido. `BARE_TRUNK_LIMIT = 8`, e o numero saiu do log: os 114
grupos se separam num vale limpo entre 7 e 10 — 93 tem de 1 a 7 troncos, os
21 restantes vao de 10 a 57. Grupo acima do limite continua intocado, que e a
Regra 3. Nao se usou `BlockProtection` como criterio porque ela nao responde
por tronco: o Vanilla marca quem pos uma **folha**, nao um tronco, entao a
cabana que o jogador ergueu em terreno livre ela nao ve. **Limite conhecido:**
cabana de ate 8 troncos, a mao, em terreno livre, entra no corte.

**2. Mineiro parado no fundo da mina.** Relato do autor: *"os mineiros
estavam parados no fundo da mina em local que nao chegaram escavando"*. O log
mostrou `c8c33662` recebendo **a mesma pedra tres vezes** — cobre em
665,32,-2866 — com 6.000 tiques de castigo ja escritos na primeira
desistencia, e o trabalhador demitido do oficio na terceira
(`gave up COLLECT_STONE once too often`). O furo era de **porta, nao de
marca**: a guarda do E44 em `MineDigging.nextCut` pergunta
`MineMarks.isOutOfReach` para a posicao do **tunel**; o minerio **colado** na
parede e servido por outra linha, passando so pelo `nowhereToStand`. O
`giveUp` escrevia a marca e ninguem a lia. Corrigido: o minerio da parede
passa pela mesma marca, e a passagem devolve a posicao do tunel — abrir a
parede e o que da ao minerio um lado de onde se alcance.

Os dois tiveram GameTest **vermelho antes do patch**, com a mensagem de
falha citando o relato. `build` verde (828 unitarios) e 329/329 GameTests.
**Pendente: validacao em jogo dos dois.**

**3. Regra 30 revogada — a boca da mina nao ganha mais bau.** Decisao do
autor, 2026-09-15: *"retire o bau da boca da mina, use so o bau de cada
mineiro"*. Era: minerio que nao fosse carvao ia para o bau da boca
(`MinerHaul.treasureChestFor`), o resto para o bau do mineiro. Passa a ser um
destino so. `MineMouth.furnish` nao poe mais bau; a lanterna fica, porque e
peca do arco e nunca dependeu do bau — a linha de log *"the mouth lantern
waits on the chest"* descrevia uma dependencia que ja nao existia, e a queixa
*"has no chest and none could be placed"* que o autor viu no log sumiu com a
causa.

**O que NAO mudou, de proposito:** `ColonyChests.addMineMouth` continua
**lendo** o bau da boca. O bau que a colonia ja pos em saves anteriores fica
de pe com todo o minerio que a Regra 30 mandou para la; parar de le-lo
apagaria esse estoque da contabilidade e devolveria o defeito que o P0.3
corrigiu em 09-11 — o fundidor dizendo `nothing in the colony chests to
smelt` com o ferro a dez blocos. Ele vira fonte que so drena. Quando o
jogador o quebrar, `MineMouth.chestAt` deixa de acha-lo e a leitura fica
silenciosa sozinha.

**4. Planejador: as duas otimizacoes que o autor escolheu (09-15).** O log
mediu ciclos de 98, 57 e 54 ms contra o orcamento de 50 ms do tique, com o
planejador levando 72 ms do pior deles, 29 colonias e 192.448 recusas de lote
num ciclo.

*Opcao 1 — a recusa barata responde antes da cara.* Das 192.448 recusas,
126.315 eram a Regra 3, a pergunta mais cara da fila
(`BlockProtection.isVillageOriginal` consulta o `StructureAccessor`), e ela
rodava em **segundo** de sete; a comparacao de dois inteiros da Regra 19, com
24.350 recusas, rodava em **sexto**. Toda coluna reprovada pela regua da rua
pagava a consulta de estrutura antes de chegar a comparacao que a reprovaria
de graca. A guarda barata do `isVillageOriginal` nao salvava o caso: ela sai
cedo quando o bloco nao tem referencia de estrutura, e dentro de uma vila os
blocos tem — que e onde a colonia procura lote.

Foram movidos **so os dois extremos**: `OFF_ROAD_LEVEL` para o topo e
`isVillageOriginal` para o fim. As tres do meio ficaram na ordem original de
proposito — custam o mesmo (uma consulta em memoria, duas leituras de bloco),
entao trocar nao compra desempenho e estraga o diagnostico: a primeira
tentativa pos `isLotGround` na frente e o `dirt_path` reservado como estrada
passou a ser recusado por "nao e solo natural", porque caminho de terra nao e
cubo inteiro. A recusa continuava certa e o log passava a mentir sobre o
motivo; quem pegou foi o
`everyReservedRoadMaterialBlocksTheWholeFootprint`.

*Opcao 3 — a vez de planejar e repartida.* Nada limitava quantas colonias
decidiam obra por ciclo: o laco percorria as 29, e o custo era a soma delas. A
varredura ja tinha teto **por colonia** (1.024 colunas por passagem) e faltava
o teto **global**. `PlannerTurns` da a vez a `PER_CYCLE = 8` colonias por
ciclo, em rodizio que retoma de onde parou. O numero saiu da medicao: 72 ms
para 29 colonias sao ~2,5 ms cada, e oito devolvem a fase para perto de 20 ms.

**O preco, dito por inteiro:** num mundo de 29 colonias cada uma passa a
decidir obra a cada quatro ciclos — dois minutos, e nao trinta segundos. A
construcao fica mais lenta onde ha muitas colonias, que e onde o tique
estourava. Com oito colonias ou menos nada muda. So o planejamento espera a
vez; trabalhador, bau e tarefa continuam andando todo ciclo para todas.

**Ao ler o log depois disto:** os numeros de recusa mudam de caixa sem que
nada de comportamento tenha mudado — espere a Regra 3 cair muito e a regua da
rua subir. O sinal de sucesso e a linha `Colony cycle took N ms` parar de
aparecer.

**Ainda nao verificado em jogo.** O ganho foi deduzido da medicao do log e da
ordem dos predicados; nenhum dos dois foi cronometrado em partida. Gametest
nao mede milissegundos de forma confiavel — o teste novo
(`theCheapRefusalAnswersBeforeTheExpensiveOne`) afirma a **ordem**, que e a
otimizacao, pela atribuicao da recusa.

Tres GameTests que afirmavam a Regra 30 foram convertidos:
`theMineMouthGetsALanternAndAChest` virou
`theMineMouthGetsALanternAndNoChest`, e a cauda do bau saiu de
`anArchTheOwnerBrokeIsNotRaisedAgain` e de
`theMouthFurnitureStaysOutOfTheStaircase`.

**Verificacao desta sessao:** `build` verde com **833 unitarios** (5 novos do
`PlannerTurnsTest`) e **330/330 GameTests**. A bateria foi repetida **tres
vezes** por causa da instabilidade conhecida: na primeira rodada caiu
`smeltergathersgrassoutsidetheprotectedvillageradius` com *"fundidor criado no
setor nao esta registrado no ServerWorld"* — que e o AUD-001 ja registrado —, e
as duas rodadas seguintes passaram limpas. Nao e regressao desta sessao, e o
AUD-001 continua aberto.

---

## Sessão de 2026-09-14

**Distribuição após a emenda ADR-012:** o JAR 0.3.0 foi reconstruído e
copiado para `downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256 nas três
cópias (`build/libs/`, distribuição e launcher):
`EF0138BE7180FC47FB905C42EF7F64A8A31FE68CFCFCBCE4C07CAF72DB467231`.
`build` e 324/324 GameTests passaram; falta a validação visual das obras e da
mineração no mundo do jogador.

**Correção local de continuidade do mineiro:** quando `MinerWork.tick` remove
um job cuja tarefa foi encerrada, ele agora libera a claim do ramal no mesmo
tique. O teste foi escrito vermelho antes do patch; depois, `build` e
324/324 GameTests passaram. O JAR em `downloads/` e no launcher não foi
atualizado nesta sessão; a observação em jogo continua pendente.

**AUD-001 — falha de CI reavaliada, ainda aberta:** o artefato do run Linux
`34814235426` no commit `2a0a4b7` registra a asserção
`fundidor criado no setor não está registrado no ServerWorld` no tick 1. A
suíte local fresca passa 324/324; desligar temporariamente a descoberta de
terra faz falhar somente `o fundidor não removeu a terra do setor externo
escolhido`, provando que a coleta é exercitada. A troca experimental para
`TestContext.spawnEntity` também falhou na asserção de registro e foi
revertida. Sem causa determinística, não houve aumento de timeout nem patch
de comportamento; falta executar o job Linux em uma revisão publicada.

## Sessão de 2026-09-13

**Distribuição desta sessão:** JAR 0.3.0 copiado de `build/libs/` para
`downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256:
`E41063395BFC20A8D5D3182A9732B6FAF1E7265E96446E8909F32A623B1BE1D9`.
`build` e 316/316 GameTests passaram; validação visual continua pendente.

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3` — em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu no jogo:**

- Casa parada no meio.
- Mineiro invisível/parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a `STABLE` — reduz o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A carpintaria fabricava composteiras, mas usava a porta de consumo (`ColonySupply.take`) e retirava do baú a peça que acabou de produzir. Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`.

**Auditoria técnica de 09-13:** a casa média do log parou por uma exigência falsa de `minecraft:structure_void`; o leitor de blueprint agora ignora esse marcador. O teste novo falhou antes da correção e passou depois. Foram aprovados 798 unitários, 312 GameTests e 74 testes Python. A pequena fazenda antiga do log retomou com 45 blocos, mas não há identidade suficiente para afirmar que era a casa do relato. A casa média corrigida ainda aguarda validação em jogo.

**Reinvestigação após o playtest:** um GameTest reproduziu a colocação de
`minecraft:dirt` sem estoque: `BuilderWork` tratava toda a tag `DIRT` como
material moldado no local. A exceção foi limitada a farmland, água,
`dirt_path` e cultivos. Os 313 GameTests e `build` passaram; a nova regra
aguarda validação em jogo. O log anterior carregou o JAR antigo e não
registra bloco/posição da terra observada, então a causa está confirmada
no código, mas não pode ser atribuída com certeza àquela posição específica.

## Sessão de 2026-09-14 — obra esperando terra

O log mais recente mostra que o planejador **abriu** `plains_butcher_shop_2`
com 382 blocos às 00:50:15. A obra assentou dois blocos e então ficou em
`WAITING_RESOURCES`, com 380 restantes, aguardando `minecraft:dirt` até
01:01:15. Os 18 baús lidos tinham `GRASS_BLOCK=33`, mas nenhum `DIRT`; o
fundidor não tinha rota para coletar terra. Portanto, a ausência visual de
construções neste teste decorre de uma obra ativa bloqueada por insumo, não
de falha de seleção do planejador. Um aviso posterior de alcance do construtor
é secundário e precisa de nova medição depois que a terra chegar.

`DIRT` agora é recurso `SURFACE_GATHERED`, tem conversão do item Vanilla e
entra na mesma coleta externa e protegida por setor usada para `grass_block`.
GameTests cobrem catálogo, atribuição ao coletor, coleta real de terra fora do
raio protegido e depósito no baú pessoal. `build` e 320/320 GameTests passaram.
**Pendente:** instalar o JAR atualizado e validar em jogo se o fundidor coleta
terra, se a obra retoma além dos 380 blocos e se o aviso de alcance reaparece.

**Reanálise do playtest de 09-14:** o JAR do launcher tinha SHA diferente do
`build/libs/`; o log veio do artefato anterior à correção de terra. Nele,
construções abriram mas pararam esperando `dirt`; três mineiros estavam aptos,
mas sem tarefa, porque havia 3 carvões e não existiam metas de carvão/ferro
sem obra. Lenhadores também ignoravam `BlockProtection` durante a derrubada.
Correções atuais: piso de 64 carvão + 64 minério bruto (obra soma ao piso) e
proteção de árvores no plano e em cada quebra. Construções Vanilla e da colônia
são cobertas; troncos manuais sem marca não têm autoria recuperável pelo jogo.
`build` passou, 322/322 GameTests passaram, e o JAR foi copiado para
`downloads/` e o launcher. SHA-256 nas três cópias:
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.
**Pendente apenas validação em jogo** da construção, mineração e preservação
estrutural; troncos manuais sem marca seguem como limite conhecido.

**ADR-016 / Lote 1 em andamento (09-14):** `ResourceTally`,
`ColonyResources` e a leitura de baús agora preservam contagens por `ResourceId`
para itens fora de `ResourceType`, mantendo a visão tipada existente. `build`
verde e 317/317 GameTests; ainda não há metas/tarefas nem executor genérico para
esses IDs, portanto a cobertura de materiais continua pendente. Validação em
mundo de desenvolvimento ainda não realizada. JAR 0.3.0 distribuído em
`downloads/` e `%APPDATA%/.minecraft/mods/`, com cliente fechado; SHA-256 nas
três cópias: `F41920D7DA1FBEADA94D4F886F5047F7A3242F850011DE7DFA2F4A9CAE28DB77`.

O mineiro fechava a frente sem espaço para ficar e reabria a mesma hélice no
fundo. E45 foi resolvido em 2026-09-13 pela ADR-013: no limite, a rota gira
sem mover a boca; saves v4 reiniciam os cursores e mantêm o arco. `build`
passou com 808 testes unitários e `runGametest` com 314/314; confirmação
visual ainda pendente.

**Playtest de 2026-09-13, após o JAR anterior:** nenhuma construção visível;
mineiros sem atividade percebida; baús de profissões misturando produção.
No log, a obra assentou só três peças e parou por falta de grama/pedregulho;
mineiros ficaram sem espaço para ficar em pé e produziram zero; lenhadores
e agricultores registraram colheitas, e fundidores pararam repetidamente
por falta de areia. Revisão dos depósitos confirmou saídas em baús de
colegas: agora a produção de mineiro, lenhador, fundidor, carpinteiro e
pedreiro vai ao baú pessoal; insumos continuam compartilhados. Se não há
espaço, transformações devolvem o insumo e drops de mineração/derrubada
permanecem no mundo. `build` e 313 GameTests passaram. **Aguardar validação
em jogo.** Próximo lote: causa da mina sem espaço para ficar em pé e
construção bloqueada por estoque/retomada.

**Releitura após alterações do jogador (lote aplicado em 2026-09-13):**
interações que realmente mudam blocos e quebras invalidam os índices/cursor
de construção das colônias próximas; a varredura limitada relê estradas e
terreno no mundo. Abrir espaço sobre trecho já percorrido da mina reabre o
braço desde o primeiro ponto afetado e limpa seu bloqueio transitório.
`build` e 314 GameTests passaram, inclusive a reindexação após estrada nova.
Pendente confirmar em jogo que casa e mina retomam no mundo do autor. Isto
não cria novas galerias nem corrige geometria E45. Baús registrados e o da
boca da mina já são consultados ao vivo; baú arbitrário continua dependendo
de vínculo de armazenamento.

**ADR-011 — profissões e crescimento:** implementadas as sete funções
produtoras (Mineiro, Lenhador, Pedreiro, Fundidor, Carpinteiro, Agricultor,
Criador) e as vagas por população adulta: 1 de cada aos 15, 2 de cada aos
30, 3º Mineiro aos 31 e 3º Lenhador aos 32. Nitwits contam para a população,
mas não são contratados; bebês só contam quando adultos. `BUILDER` e
`SHEPHERD` permanecem compatíveis com saves, e Pastor legado conta na cota
de Criador. Qualquer produtor pode construir temporariamente sem trocar de
profissão. Build, testes unitários e 313 GameTests passaram; **aguarda
verificação em jogo**.

---

## P1 — em fila (depende de P0 verde)

| Item | Descrição | Decisão |
|---|---|---|
| E44 | Recusa de alvos inalcançáveis | ✅ escada em `MineMarks`; **aguarda validação em jogo** |
| E43 | O descanso de 4 ciclos é anulado no ciclo seguinte | **autor** |
| E41 | Nada mede degradação ao longo de muitos ciclos | — |
| KF-001 | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard` | ✅ causa no teste corrigida; quota global é risco separado de vazão |

---

## Última verificação em jogo

| Data | O que foi visto |
|---|---|
| **2026-09-13** | casa parada, mineiro parado, lenhadores e fazendeiros trabalhando |
| **2026-09-12** | casa morreu esperando `smooth_stone_slab`; mineiro não desceu |
| **2026-09-11** | casa aberta (P0.1 visto); obra travou em `dirt_path` |

**Pendente de ver em jogo:**

- P0.1-b, P0.1-c, P0.3, P0.5, P0.6 — entregues em 09-11, **nunca vistos**.
- As quatro correções de 09-13 — **nunca vistas**.
- Arco da mina não volta depois de quebrado (`Mine.archRaised`).
- Lenhador corta todo o tronco sem deixar sobra (E39, fechado 09-12).

---

## Bateria

Última medição: **810 unitários**, **314 GameTests** e **74 testes Python**;
zero falhas nos dois primeiros nesta sessão. Python não foi reexecutado.

**Diagnóstico do playtest/log de 09-13:** uma tarefa de areia foi criada para
vidro, mas `MinerWork` gravava no Job a pedra da paleta da vila. O log mostra
`0 of 3` apesar de areia transportada; corrigido para o Job acompanhar o
recurso da tarefa. Antes disso, ele completou 64/64 pedregulhos; depois a
busca de areia não encontrou bloco num raio de 48. `MinerWork.tick` remove
Jobs concluídos sem uma etapa explícita de volta à boca da mina; retorno
segue aberto. A obra esperou `minecraft:grass_block` com 382 blocos restantes
e desistiu mantendo o lote ocupado; nenhuma profissão fornece esse recurso
no catálogo atual. Agricultores não acharam plantio maduro/lote vazio em 32
blocos. Carpinteiro, fundidor, pedreiro e pastor reportaram sem tarefa aberta;
lenhadores foram vistos cortando e aumentando a produção. `build`: 810
unitários; GameTests: 314/314.

---

## Decisões que esperam o autor

1. **E43 — o descanso de 4 ciclos é anulado no ciclo seguinte.** A 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador quando a colônia não tem outro trabalho da profissão dele.

---

## Onde o projeto está

MVP previamente verificado em jogo; o playtest de 09-13 contradiz o estado da mina: o minerador repete uma frente bloqueada no limite. O falso material `structure_void` da casa média foi corrigido e ainda precisa de verificação visual. O log mostra uma obra antiga retomada, mas não prova que era a casa do relato. Lenhadores e fazendeiros foram vistos funcionando pelo autor.

**O gargalo recorrente é verificação em jogo e decisões do autor**, embora
o playtest ainda revele defeitos de código, como a fome de buscas da mina
corrigida nesta sessão. Cada item entregue acumula dívida de "não visto em
jogo", e a fila cresce mais rápido do que drena.

**Próximo passo natural:** sessao de jogo para validar o portal da mina em
vila nova, a rota E45, a casa e a politica P0.7 no mundo do autor. E43 segue
como decisao do autor; E44 aguarda validacao em jogo.

**Nova vila sem portal da mina (09-13):** o log mostra mineiros das vilas
novas repetindo `looking for stone, 0 of 64`, sem linha de abertura. A causa
no código era a cota global de uma busca por tique presa ao primeiro
trabalho sem alvo. O rodízio foi corrigido e passou em `build` (809
unitários) e 314 GameTests; portal visível e início da escavação **aguardam
validação em jogo**.

**Coleta superficial do fundidor (09-13):** pá de ferro com Toque Suave I;
areia para a cadeia do vidro e `grass_block` apenas quando uma obra aberta
precisa dele. A grama é buscada estritamente além de 64 blocos, no setor
cardinal mais distante das peças de estruturas de vila conhecidas em chunks
carregados; nenhum chunk é forçado e o raio de busca de trabalho continua 48.
`build` e 315/315 GameTests passaram. **Aguardam validação em jogo** a coleta,
o baú pessoal do fundidor e a preservação visual das estruturas. JAR de
`downloads/` e launcher não atualizado nesta tarefa.

**Distribuição híbrida e pedra lisa (09-13):** faltas da obra aberta recebem
`CONSTRUCTION_MATERIAL` antes das tarefas de estoque; trabalhadores restantes
mantêm `PRODUCTION`. O pedreiro reconhece `smooth_stone_slab`, e a demanda
deriva a quantidade de `smooth_stone` da receita Vanilla. `build` e 316/316
GameTests passaram; aguarda validação visual. ADR-015 registra a escolha C,
sem teto numérico arbitrário de reserva. **Ainda não implementado:** tarefas,
contagem e estoque de blocos arbitrários, nem o fallback genérico para
fundidor/criador; o catálogo de recursos atual é enum fechado. Ver TODO.

**Plano de continuidade e construção (09-14):** plano por lotes em
`docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md`.
Lote 1 aplicado: ADR-012 reconcilia a coluna editada pelo jogador, preserva
a varredura parcial e reinicia apenas o cursor de consulta de ruas. `build` e
324/324 GameTests passaram; **sem confirmação em jogo**. Próximo lote para
revisão: continuidade do mineiro; depois diversidade de estruturas entre
construtores, estratégias distintas para avaliar lotes e revalidação após
falhas repetidas.
