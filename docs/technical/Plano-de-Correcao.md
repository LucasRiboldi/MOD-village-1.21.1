# Plano de Correção — 2026-09-11

**Autor do plano: o autor do projeto.** Registrado aqui na íntegra em
2026-09-11, sobre o commit base `9385db6`, para que a execução não dependa
de ninguém lembrar do que foi combinado.

Este arquivo é o **roteiro**; a lista canônica do estado continua sendo o
[`TODO.md`](../../TODO.md). Onde os dois discordarem sobre *o que já foi
feito*, vale o TODO; sobre *o que fazer e em que ordem*, vale este.

---

## A régua (não negociável)

- **Ter teste verde ≠ estar pronto.** Todo conserto precisa ser visto em jogo.
- **Não tratar sintomas isoladamente.** Corrigir a cadeia:
  `descobrir → planejar → atribuir → executar → entregar → atualizar estado`.
- Cada etapa precisa de **motivo explícito de falha**, **cooldown quando
  necessário**, e **teste que reproduza o comportamento real**.
- **Nunca** aumentar raio, limite ou `tickLimit` só para fazer teste passar.
- Uma falha de **um** lote não significa "não existe lote".
- Uma falha de **uma** tarefa não significa "trabalhador burro".
- Não abrir profissão nova enquanto P0 não estiver verde.
- Não escrever teste de unidade onde o defeito é de integração — o
  `gauntlet-verifier` já reprovou duas entregas "prontas" que passavam na
  bateria e falhavam em jogo.

**Antes de começar:** rodar a bateria. Verde? seguir. Vermelha? parar e reportar.

---

## P0 — Bloqueadores

Um por vez, com teste antes de seguir.

### P0.1 — O planejador não acha lote

**Sintoma:** `no building work: nothing to work on in the whole radius`.
281 de 517 ciclos com `assigned 0 tasks`. Motivo nº 1:
`no carpenter work: no task open for it`, 67 vezes.

- Instrumentar a busca por obra: logar, **por candidato no raio**, o motivo
  da rejeição — fora de alcance, ocupado, terreno, recursos, planta, estado
  da obra, estado interno.
- O planejador deve avaliar **vários** candidatos, não encerrar no primeiro
  conjunto inválido.
- Transformar `nothing to work on` em *"rejeitei 12 lotes por X, Y, Z;
  candidato C válido"*.
- **Não aumentar o raio.** Primeiro descobrir por que rejeita.

**Teste:** colônia com recursos e área livre → pelo menos 1 lote válido.
**Critério:** log com motivo por candidato; nenhum `nothing to work on`
quando há lote válido.

### ~~P0.2~~ — A colônia lê 1 de 8 baús

❌ **VENCIDO NA PREMISSA — a varredura tinha lido os oito.** Conferido
pelo P0.0 em 2026-09-11, e é o achado que mais justifica a varredura.

A linha é montada assim, em `VillageDetectionHandler.logResources`:

```java
"Colony {} stores {} in {} of {} chests read{}",
    colony.id(), stock,
    resources.byChest().size(),   // baús COM CONTEÚDO
    survey.chestsRead(),          // baús LIDOS
```

Os dois números são **baús com conteúdo** e **baús lidos** — nunca uma
fração de cobertura. `in 1 of 8 chests read` quer dizer *"um baú tinha
alguma coisa, dos oito que li"*. O javadoc de `ChestSurvey` diz isso em
tantas palavras: *"`chestsRead` — baús alcançados, **incluindo os que
estavam vazios**"*. E quando a varredura **de fato** não alcança um baú,
a linha antiga ganhava o sufixo `(N unreachable, chunk unloaded)` — que
não aparece em nenhuma das duas medições citadas aqui.

Então o "piorou" de `5 of 16` → `1 of 8` também não é o que parece: são
menos baús registrados e menos baús com estoque, e não menos leitura.

**O que ficava pendurado nessa premissa:** `ColonyChestScanReport`,
`ColonyChestCache` com invalidação por evento, `CHESTS_PER_TICK = 4`,
`MAX_STALENESS_TICKS` e o log por ciclo. Nada disso tem defeito que o
justifique — o alarme que eles construiriam (`scanCompleted=false`) já
existe como `ChestSurvey.isPartial()`.

✅ **O que sobrou, e foi entregue:** a frase enganava, e enganou. Foi ela
que custou um bloqueador inteiro de plano. `ChestSurvey.coverage()` monta
a cobertura num lugar só, e **reserva a forma `X of Y chests read` para
cobertura de verdade** — varredura completa sai como `8 chests read, 1
with items`, que não se lê ao contrário. Cinco casos em
`ChestSurveyCoverageTest`.

É o mesmo *defeito-que-parece-número* do V5 que o javadoc do
`ChestSurvey` nomeia, cometido do lado de fora: não no que a varredura
mede, mas no que a frase deixa concluir.

**Sintoma original, para o registro:** `in 5 of 16 chests read` (09-10) →
`in 1 of 8 chests read` (11 set). **Piorou.**

- Criar `ColonyChestScanReport`: `chestsInRadius`, `chestsScanned`,
  `chestsSkipped`, `chestsFailed`, `itemsObserved`, `scanStartTick`,
  `scanEndTick`, `scanCompleted`.
- `scanCompleted=false` é o alarme. Envolver a varredura em `try/finally`.
- `ColonyChestCache` com invalidação **por evento** — bloco quebrado ou
  colocado, baú aberto ou fechado, trabalhador move item —, não por tique.
  `MAX_STALENESS_TICKS = 200–400` como válvula.
- Varredura em fila: `CHESTS_PER_TICK = 4`. Não ler tudo num tique.
- Log por ciclo:
  `[Colony/chest] cycle=N in=X scanned=Y skipped=Z failed=W items=V duration=Tt completed=B`
- **Não aumentar a frequência de leitura.** Se lê 1 de 8, ler 20×/min
  continua 1 de 8.

**Teste:** `chestsScanned == chestsInRadius` quando `scanCompleted=true`;
nenhuma linha `completed=false` com `scanned < in`.
**Critério:** 8 encontrados → 8 válidos → 8 lidos → 8 contabilizados.

### P0.3 — A cadeia mineiro → armazenamento → fundidor

✅ **CONFIRMADO, e a ruptura está achada — é estática.** O critério do
item era *"prova de que a cadeia está correta, **ou** prova de onde ela
quebra"*, e a segunda saiu por leitura em 2026-09-11, sem precisar de
sessão.

**A Regra 30 manda o minério que não é carvão para o baú da boca da
mina.** Esse baú é achado por geometria — `MineMouth.chestAt` procura um
baú encostado na entrada do poço — e **não é registro de ninguém**: o
único lugar do mod que cria `WorkerStorage` é `ChestScanner.scan`, que
procura baú ao redor da **cama** do aldeão. Mina não tem cama ao lado.

E tudo o que conta o estoque da colônia percorre baús de trabalhador:

| quem | de onde tira a lista |
|---|---|
| `ColonyChests.nearestFirst` | `WORKERS.ofColony` → `STORAGES.of(...)` |
| `ChestInventoryReader.survey` | os mesmos `WorkerStorage` |
| `SmelterWork.smeltOne` | percorre os trabalhadores direto |

Então o minério entra num baú que a contabilidade da colônia **não lê**.
O fundidor está certo, e está faminto ao lado do ferro — e a linha
`nothing in the colony chests to smelt` é verdadeira no pé da letra.

Isso também fecha a conta do P0.2: oito baús lidos com estoque em um só
é exatamente o que se espera quando o minério está fora da lista.

**Provado por** `SmelterGameTest.theOreInTheMineMouthChestIsInvisibleToTheColony`,
nas duas metades — que é mesmo ali que a Regra 30 deposita, pelo caminho
que o `MinerHaul.treasureChestFor` percorre, e que a colônia não o
enxerga estando ele com oito ferros dentro.

✅ **E a instrumentação que o item pedia foi entregue junto:** o motivo de
falha do fundidor passou a dizer o que mediu — `none of 3 colony chests
had minecraft:raw_iron to smelt`, ou `no colony chest to look in`. A
frase antiga era a mesma para "não há baú registrado", "há seis e estão
vazios" e "há seis e nenhum tem ferro".

⬜ **O conserto não está decidido, e é do autor.** São dois caminhos, e
eles não custam o mesmo: o baú da boca entra na lista da colônia, ou a
Regra 30 para de mandar minério para fora dela. O primeiro mexe em quem
lê; o segundo, em quem escreve — e a Regra 30 existe para o mineiro não
carregar minério montanha acima.

**Sintoma:** fundidor `nothing in the colony chests to smelt` (34×),
mineiro entregando 1 pedra em 30 minutos.

- **Não mexer na lógica do fundidor ainda.**
- Instrumentar a cadeia: quantos itens fundíveis existem nos baús no momento
  da falha?
- Correlacionar coleta, depósito, leitura do baú e decisão de fundição.
- Zero fundíveis → o fundidor está certo, e é sintoma do P0.2.
- Só alterar o fundidor se a cadeia anterior estiver comprovadamente correta.

**Critério:** prova de que a cadeia está correta, **ou** prova de onde ela quebra.

### ~~P0.4~~ — O lenhador corta a parede da própria vila

❌ **VENCIDO — o item pede o que entrou em 2026-09-09.** Conferido pelo
P0.0 em 2026-09-11, item por item:

| o que o plano pede | onde já está |
|---|---|
| conjunto de posições rejeitadas | `TreeMarks.REJECTED`, teto `MAX_REJECTED = 4096` |
| consultar antes de tentar | sim, e guarda o **grupo** inteiro, não o tronco |
| rejeitada N vezes → cooldown | `Refusal(since, count)` |
| **6.000 → 12.000 → 24.000 → 48.000** | `memoryFor`: `UNREACHABLE_MEMORY = 10 × 600 = 6000`, dobrando até `× MAX_MEMORY_FACTOR = 8` — **a escada exata** |
| invalidação, não lista negra | Regra 23, a marca envelhece sozinha |
| não persistir entre sessões | mapa estático |

Sobrou **"por trabalhador"**, e ele não deve ser feito: o conjunto é
global de propósito, e por trabalhador seria **pior** para o sintoma
medido — dois lenhadores redescobririam a mesma parede em separado,
dobrando as rejeições em vez de cortá-las.

**E a medida de 118 é posterior à escada.** O próprio `TODO.md` diz, na
linha de onde o número saiu: *"o castigo escalona (6.000→48.000 ticks) **e
funciona**, mas ele redescobre a mesma parede seis vezes"*. Com 28
posições e ~51.600 ticks de sessão, 118 rejeições é **o que a escada
prevê** — quatro reperguntas por posição, cada uma ao dobro do prazo
anterior.

⬜ **O que resta é outro item, e não este.** O custo não está no prazo,
está em a primeira recusa de cada parede pagar uma busca inteira. Atacar
isso pede medir a busca, e não mexer no cooldown — que é onde o plano
mandava mexer.

**Sintoma:** `Not a tree — N logs without a living canopy` sobre troncos de
casa. 118 rejeições sobre 28 posições; redescobre a mesma parede 6×.

- Conjunto de posições rejeitadas **por trabalhador**.
- Consultar antes de tentar. Rejeitada N vezes → cooldown.
- Cooldown progressivo: 6.000 → 12.000 → 24.000 → 48.000 ticks.
- Durante o cooldown a posição **não é candidata**.
- Invalidação, não lista negra permanente: bloco mudou, ambiente mudou, ou
  cooldown expirou.
- **Não persistir entre sessões.**

**Teste:** a mesma posição não é reavaliada antes do cooldown; redescobertas ≤ 2.
**Critério:** 118 rejeições caem para poucas.

### P0.5 — E3: perda de item por inventário cheio

✅ **CONFIRMADO e ENTREGUE em 2026-09-11.** Era a única das cinco
pendências de P0 que descrevia um defeito ainda vivo, e era literal.

O `MinerHaul.deposit` tentava o baú da boca da mina e depois o baú do
mineiro; o que sobrasse virava uma linha de WARN contando a perda — *"O
que não couber é perdido, e é o mesmo E3 do lenhador"*, dizia o javadoc.
Colônia com seis baús vazios a vinte blocos não mudava nada: a pergunta
nunca chegava a eles. O lenhador tinha ganhado `ColonyChests.ownFirst` em
2026-09-04, e o `TODO.md` registrava desde então que *"o mineiro continua
sem teto de inventário"*.

Agora o transbordo atravessa os baús da colônia pelo mesmo caminho do
lenhador — o baú do dono primeiro, o resto como transbordo —, e o WARN
passou a dizer quantos baús foram tentados. **Ainda se perde com a
colônia inteira cheia**, e aí a linha é a notícia certa: o jogador
precisa esvaziar alguma coisa e precisa poder descobrir isso.

**Provado por** `MinerOverflowGameTest.theHaulThatDoesNotFitGoesToAnotherColonyChest`,
com duas asserções de montagem — que a pedra saiu do mundo, e que o baú
do mineiro continua sem espaço até o fim. Sem elas o teste passaria por
motivo errado: mineiro que não cavou e mineiro que cavou e perdeu tudo
dariam o mesmo baú vazio.

- Coleta → inventário cheio → tenta depósito → falha → procura armazenamento
  válido → deposita → continua.
- Sem armazenamento: retorna a um conhecido; impossível → estado de espera.
- **Nunca destruir o item.**

**Critério:** o recurso é preservado.

---

## P1 — Estabilizar comportamento

Depende de P0 verde.

| | o quê |
|---|---|
| **P1.1** | **Implementar a ADR-009 §3.6** — a decisão mais importante. Abandonar tarefa que não progride, trocar de objetivo, voltar depois. Arquitetura `Task → Attempt → Progress?` — sim continua, não → motivo de falha → cooldown → outra tarefa. A Regra 28 vira **diagnóstico**, não inteligência definitiva. Vem antes do P1.2, porque o descanso depende do abandono funcionar |
| **P1.2** | **Honrar o descanso de quatro ciclos (E43).** `giveUp()` = desistir daquela tarefa; não pode recebê-la de volta por 4 ciclos. Sem outra: ocioso, e **ociosidade é aceitável**. Documentar no `WorkStall` qual das duas regras vale — *"só se aplica quando há alternativa"* ou *"estende-se se a única tarefa é a mesma"* |
| **P1.3** | **Mineiro longe do corredor: devolver a tarefa.** Nada de linha reta — NPC preso, atravessa montanha, cai em buraco. Estado explícito de *perdido*, independente do planejador que o levou lá. Cooldown para aquela ordem/localização |
| **P1.4** | **Lenhador: consertar o travamento**, não ressuscitar a mão emprestada. Comparar `assign()` do mineiro e do lenhador lado a lado antes de portar. Criar gancho de teste equivalente ao `shortenStallLimitTo`. GameTest ponta a ponta: força stall, abandona, entra em cooldown, escolhe outro objetivo |
| **P1.5** | ✅ **entregue em 2026-09-11, e o item estava pequeno.** A premissa era afinação de raio; o defeito era **cobertura**. O `CropPatch` tinha a própria espiral à mão, orçamento de 2.048 colunas e **sem cursor**: toda passagem recomeçava do centro, e o quadrado de raio 32 tem **4.225 colunas**. A varredura fechava o anel 22 e abortava no 23 — **48% da área prometida, sempre a mesma metade**. E como o `ConstructionPlanner` abre roça até `FarmerWork.reach()` = 32, **uma roça que a colônia mandou construir entre 23 e 32 blocos era invisível ao fazendeiro dela**. A escada 0–16 → 16–32 → 32–48 teria mudado o número da mentira, não a cobertura. Conserto: a espiral passou ao `RingSweep` (que retoma por anel **e** coluna, e era a quarta escrita à mão no projeto), o motivo passou a distinguir `SWEEP_INCOMPLETE` de `NO_TARGET`, e **volta inteira sem nada compra quatro ciclos de silêncio** — que é o cooldown que o item pedia |
| **P1.6** | **Aceitar espécies misturadas na casa.** Identidade de material vira **preferência**, não barreira |
| ~~**P1.7**~~ | ❌ **NÃO SE FAZ — refaria o defeito de 2026-09-10.** A premissa é que cem toras de cerejeira não deviam satisfazer "Oak Log × 20". Mas **elas satisfazem de verdade**: `INTERCHANGEABLE_IN_THE_WALL` já traz `WOOD`, `PLANKS`, `STONE` e `STRIPPED`, então a cerejeira substitui o carvalho na parede pela Regra 27, e o `MaterialChoice` descasca cerejeira para planta de carvalho desde 09-05. A conta por grupo e o construtor **concordam** — e é essa concordância que importa. O javadoc do `ResourceSubstitution` guarda a lição, que custou uma sessão: *"a colônia concluía que a meta estava cumprida e o mineiro não ia cavar, enquanto o construtor esperava pelo arenito. O defeito era a **discordância** entre a conta e o construtor, e não a substituição em si"*. Separar `WOOD` por espécie recria exatamente isso, do outro lado: a conta passaria a exigir carvalho enquanto a parede aceita cerejeira. **Este item também contradiz o P1.6** do próprio plano, que manda aceitar espécies misturadas — e o P1.6 é o que já está implementado |
| ~~**P1.8**~~ | ❌ **NÃO SE FAZ — o campo não está morto.** Conferido em 2026-09-11: `furniture()` é o **primeiro critério de ordenação** da obra em `StructureBlueprintReader`, e o javadoc de lá diz por quê — *"a ordem de baixo para cima garante o que está embaixo, e mobília depende do que está ao lado; só a casa inteira responde isso"*. Removê-lo faria a cama e a mesa serem assentadas junto com a parede. A premissa do item era que a morte da Regra 21 o tinha deixado sem dono; ele mudou de dono, e o dono novo é a ordem de construção |
| **P1.9** | **Regra 25: não mexer agora.** Só ganha sentido com múltiplas plantas, depois da §3.6 |
| **P1.10** | ✅ **feito em 2026-09-11.** `theNeighbourhoodDeadEndIsNotTheTunnelCursorsQuestion`: três recusas em posições **já cavadas** enchem o cubo em volta de uma fronteira **sem marca própria**. Duas asserções de montagem garantem que o cenário discrimina de verdade, e a mutação que o Verifier provou que passava pela bateria inteira agora derruba este teste e só ele |
| **P1.11** | ⚠️ **medido em 2026-09-11, e é maior que esta linha.** A arena não é só rasa: ela vive em **y=-58..-52** (medido no log da bateria), o mundo acaba em -64, e **`MineShaft.DEEPEST = -59`** já proíbe aprofundar nessa faixa. Um nível custa 20 blocos, então a sala 2 cairia em **-76** — fora do mundo, não só fora da arena. E a altura da arena é do runner de gametest, que o mod não controla: não há `vmArg` nem configuração no `build.gradle`. **O caminho que resta** é um gancho de teste que encurte a descida entre níveis, no molde do `sandRadius` e do `searchRadius` que o projeto já tem — com 4 blocos por nível a sala 2 cai em -60 e cabe. Não é afrouxar limite para passar; é encurtar geometria para caber. Mas mexe no `MineShaft`, que é Core e governa a forma da mina inteira, então é trabalho próprio e não uma linha |
| **P1.12** | ✅ **feito em 2026-09-11, com uma parte recusada.** `TestWorkers` dá os dois helpers com a diferença no nome, e um teste prova que ela é observável. Os 26 call-sites de mineiro e lenhador passaram a equipar — e a bateria seguiu verde, então **nenhum dependia da mão nua**. ❌ **A asserção defensiva no `assign()` não entra:** na produção o `assignMissing` roda antes do `equip`, no mesmo ciclo, então mão vazia ali é o desenho — recusá-la quebraria a contratação inteira |
| **P1.13** | **E41: teste de degradação longa.** 200+ ciclos, amostra por ciclo, falha **por tendência** e não por valor absoluto. Precisa de fonte de trabalho infinita (`TestWorkSource`), senão mede "nothing to work on" — que é o P0.1 — e não degradação. O `chestScanCompleted` vem do P0.2: as frentes andam juntas |

---

## P2 — Performance

**P2.1 — O ciclo da colônia em 112 ms** (limite: 50 ms).

Instrumentar **antes** de otimizar, por subsistema: planner, chest scan, task
assign, workers, persistence, other. A regra é *nem tudo em todo ciclo* —
planejamento de obras a cada X ciclos, scanner com cache incremental, seleção
de tarefas quando necessário, IA do trabalhador em frequência normal.

Só otimizar de verdade se passar de 100 ms ou o TPS cair.

---

## P3 — Confiabilidade

| | o quê |
|---|---|
| **P3.1** | Extrair `VillagerMigration`, `VillagerDataVersion` e `ColonyMigrationStats`. Comando `/colony debug migration [summary\|reset\|inspect\|migrate <radius>]` e `/colony debug chest scan` |
| **P3.2** | Teste de migração com NBT sintético: sem profissão + POI perto → `MANUFACTURER`; sem POI + `LastProfession` → usa a última; sem nada → `ColonyNeedsRehire`; já migrado → não toca; `DataVersion` gravada de volta. `PoiLookup` injetável |
| **P3.3** | Rodar o `gauntlet-verifier` depois de P0 e de P1 |

---

## Baixo — gatilhos, não frentes

1. Save antigo perde `MANUFACTURER` → só com bug report real.
2. Achados *medium* em `test_gauntlet.py` → só se o gauntlet for reescrito.
3. Ciclo de 81 ms → já coberto pelo P2.
4. Escada solta/retomada → é o E44, tratar junto com o P1.4.
5. Picareta de ouro vs diamante → documentar, não corrigir.
6. Falta de lint → só se a dívida passar de 20 arquivos acima de 500 linhas.

---

## Ordem de execução

```text
bateria verde
  ✅ P0.0  validar o plano contra o código          ← 2026-09-11, feito
  ✅ P0.2  vencido na premissa; a frase consertada
  ✅ P0.3  ruptura achada e instrumentada; conserto em aberto
  ✅ P0.4  vencido
  ✅ P0.5  entregue
  → uma sessão de jogo, que entrega o número do LotRefusals
     e confere P0.1-b, P0.1-c, P0.6 e P0.5
  → P0.7 terraplanagem, SE o número a justificar
  → PARAR E REPORTAR
  → P1.1 → P1.2 → P1.3 → P1.4 → P1.5 → P1.6 → P1.7 → P1.8
  → P1.10 → P1.11 → P1.12 → P1.13
  → P2.1
  → P3.1 → P3.2 → P3.3
  → gauntlet-verifier
```

**Um item por commit**, com mensagem no formato
`P0.1: <o que mudou> (<teste que prova>)`. Não agrupar P0.1 com P0.2.

---

## Regras finais

- Se um teste passa só por folga de `tickLimit`, **o teste está errado**.
- Se um teste mede mão nua, **o teste está errado**.
- Se um teste unitário cobre comportamento de integração, **o teste está errado**.
- Se foi preciso aumentar um limite para o teste passar, **o teste está errado**.
- Se o defeito some quando a frequência aumenta, **o defeito não sumiu**.
- Se não dá para reproduzir em jogo, **não foi consertado**.

---

## ⏭️ P0.0 — validar este plano contra o código

**Pedido do autor em 2026-09-11, e ele vem antes de tudo o que sobrou.**

No ciclo de 09-11 três itens deste plano caíram por leitura: o `furniture()`
que não estava morto, a asserção defensiva no `assign()` que quebraria a
contratação, e a reserva de tora por espécie que refaria a discordância de
09-10. Um quarto — o P1.11 — devolveu-se maior do que entrou.

**Os quatro têm a mesma forma.** Este plano foi escrito a partir do `TODO.md`,
e o `TODO.md` guarda pendências desde agosto. Algumas delas pararam de ser
verdade sem que ninguém as relesse — quatro linhas daquela lista já tinham sido
derrubadas por leitura antes, e o padrão se repetiu aqui.

**O que a varredura faz**, por item ainda aberto:

1. Achar no código o que o item afirma — a constante, o método, a linha.
2. Conferir se a afirmação ainda vale **hoje**, e não quando foi escrita.
3. Marcar o item como `confirmado`, `vencido` ou `maior do que parece`,
   com a evidência ao lado.

**O que ela não é:** não é refazer o plano. Os itens confirmados seguem na
ordem em que estão; o que muda é parar de descobrir a vencidura um a um, no
meio da implementação.

### ✅ O que a varredura devolveu — 2026-09-11

Quatro P0 abertos, e o placar foi **2 vencidos, 1 confirmado e entregue,
1 confirmado com a ruptura achada**:

| item | veredito | a evidência |
|---|---|---|
| P0.2 | ❌ **vencido na premissa** | os dois números da linha são *baús com conteúdo* e *baús lidos*; o sufixo `(N unreachable)` não aparece em nenhuma das medições citadas |
| P0.3 | ✅ **confirmado, ruptura achada** | o baú da boca da mina não é `WorkerStorage` de ninguém, e as três contagens da colônia só percorrem `WorkerStorage` |
| P0.4 | ❌ **vencido** | `memoryFor` dá 6.000→12.000→24.000→48.000 desde 09-09, e o `TODO.md` diz que funciona |
| P0.5 | ✅ **confirmado e entregue** | `MinerHaul.deposit` descartava o que não coubesse, com a colônia tendo espaço |

**A varredura se pagou.** O P0.2 sozinho era o item nº 2 do plano e
carregava cinco frentes — relatório de scan, cache por evento, varredura
em fila, válvula de obsolescência, log por ciclo. Nenhuma delas tinha
defeito que a justificasse. Implementá-las teria custado dias e mexido no
caminho mais quente do ciclo da colônia para corrigir uma leitura errada
de log.

**E o padrão que o P0.0 previa se repetiu inteiro.** Os dois vencidos
foram escritos a partir de sintomas de sessão que o código já tinha
respondido — o P0.4 em 09-09, dois dias antes de o plano ser escrito. A
lição não é sobre estes dois itens: é que **sintoma de log envelhece mais
devagar do que o código que o produz**, e uma lista feita de sintomas
precisa ser relida contra o código antes de virar trabalho.

**O que a varredura não fez:** os P1 não foram varridos item a item. Eles
dependem de P0 verde, e P0 verde ainda depende da sessão de jogo que o
P0.1-c e o P0.6 esperam. O P1.6 apareceu de passagem e está implementado
(`INTERCHANGEABLE_IN_THE_WALL`), como o próprio texto do P1.7 já dizia; o
P1.5 continua de pé (raio fixo de 32 no `FarmerWork`, sem busca
progressiva).

---

## Estado da execução

Atualizado a cada item entregue. `✅` só entra com bateria verde **e**
mutação conferida; a coluna *em jogo* é a que a régua cobra.

| item | estado | em jogo |
|---|---|---|
| P0.1 | ✅ entregue em 2026-09-11 — o índice de ruas voltou a valer para vila grande | ✅ **visto na sessão de 02:03**: a colônia planejou o açougue e abriu a obra |
| **P0.1-b** | ✅ **o caminho de terra não sai de baú.** A obra do P0.1 abriu e travou em `waiting for minecraft:dirt_path`, 30 vezes — o bloco não tem item, nasce de pá na grama. Uma linha no `isShapedFromTheGround`, que já cobria `farmland` e `water` pelo mesmo motivo | ❌ |
| **P0.1-c** | ✅ **a recusa de lote diz por quê.** `LotRefusals` conta os cinco motivos do `flatGroundAt` e o relatório de sessão os diz junto do `SweepLog`. É o P0.1 ao pé da letra, e é o número que decide a terraplanagem | ⬜ **espera sessão** |
| **P0.7** | ⬜ **terraplanagem da vila** — pesquisada em [`terraplanagem-da-vila.md`](../research/terraplanagem-da-vila.md), decidida (capacidade do construtor, teto de 4 blocos) e **represada de propósito**: abre com o número do P0.1-c, não com inferência | — |
| **P0.6** | ✅ **a enxurrada da areia calou.** Amortecedor de um ciclo no `IdleLog`, para quem pergunta por tique: motivo que oscila deixa de virar quatro linhas por segundo. O irmão da pedra de superfície foi junto | ⬜ **espera sessão** |
| ~~P0.6~~ | 🔴 ~~**novo, e é bloqueador de diagnóstico:** a busca de areia oscila entre *"ainda varrendo"* e *"varri tudo"* a cada passagem, e o `IdleLog` registra as duas. **4.389 linhas de 6.117 na sessão de 02:03** — 72% do log. A próxima sessão fica cega~~ | — |
| **P0.0** | ✅ **feito em 2026-09-11.** A varredura derrubou dois dos quatro P0 abertos e achou a ruptura do terceiro sem sessão. O placar: **1 confirmado e entregue, 1 confirmado com o conserto em aberto, 2 vencidos** | — |
| ~~P0.2~~ | ❌ **vencido na premissa** — `in 1 of 8 chests read` é *um baú com conteúdo, de oito lidos*. A varredura nunca pulou baú. ✅ O que sobrou foi a frase, e ela foi consertada: `ChestSurvey.coverage()`, 5 casos | — |
| P0.3 | ✅ **ruptura achada, e é estática** — a Regra 30 deposita minério no baú da boca da mina, que não é `WorkerStorage` de ninguém, e por isso está fora de `ColonyChests`, da varredura e do fundidor. Gametest nas duas metades. ✅ Instrumentação entregue | ⬜ **conserto em aberto, decisão do autor** |
| ~~P0.4~~ | ❌ **vencido** — a escada 6.000→48.000 que o item pede entrou em 2026-09-09 e o `TODO.md` diz que funciona. O que sobrava, "por trabalhador", pioraria o sintoma | — |
| P0.5 | ✅ **entregue em 2026-09-11** — o transbordo do mineiro atravessa os baús da colônia em vez de ser destruído. Era a única das cinco que ainda descrevia defeito vivo | ⬜ **espera sessão** |
| **P1.1** | ⚠️ **parece vencido — a §3.6 está no código.** `giveUp` faz `task.release()` com motivo explícito → cooldown na posição (`MineMarks.refuse`) → `stepAside` → `WorkerStrikes.gaveUp` → `Worker.rest(capability)`, e **as sete profissões passam pela mesma porta**. Falta a leitura cláusula a cláusula antes de riscar | — |
| **P1.2** | ⚠️ **parece vencido.** `Worker.REST_CYCLES = 4`, e o E43 está citado nominalmente: o descanso curto demais foi resolvido com `SHUN_CYCLES = 8` e escada até ×8. A pergunta que o item mandava documentar, o código respondeu | — |
| **P1.3** | ⬜ **precisa de leitura própria.** A desistência por distância existe (`"it got no closer than"`, `"it walked for"`) e o prazo de aproximação mudou em 09-11; *"estado explícito de perdido"* é outra coisa | — |
| **P1.4** | ⚠️ **parece vencido.** O guarda existe (`job.stall.stuck`, `TreeChoice.stallLimit`), o gancho de teste que o item manda criar já existe (`shortenStallLimitTo`) e o gametest ponta a ponta também (`LumberjackGameTest.theStallGuardReturnsTheTaskAndForgetsTheTree`) | — |
| **P1.5** | ✅ **entregue em 2026-09-11** — a busca do fazendeiro alcançava 22 dos 32 blocos prometidos; agora atravessa passagens pelo `RingSweep`, distingue "não achei" de "não terminei" e descansa depois de uma volta inteira | ⬜ **espera sessão** |
| P1.6 | ❌ já implementado — `INTERCHANGEABLE_IN_THE_WALL` | — |
| P1.9, P1.13 | ⬜ | — |
| P1.7 | ❌ não se faz — ver acima | — |
| P1.8 | ❌ não se faz — ver acima | — |
| P1.10 | ✅ 2026-09-11 | — |
| P1.11 | ⚠️ medido, e é maior que a linha — ver acima | — |
| P1.12 | ✅ 2026-09-11, com a asserção defensiva recusada | — |
| P2.1 | ⬜ | — |
| P3.1 – P3.3 | ⬜ | — |

**Bateria no fim do ciclo de 2026-09-11:** 753 unitários e 295 de gametest,
zero falhas, medidos pelos XML de relatório e pelo `runGametest`.

**Depois do P0.0, do que ele liberou e do P1.5:** **763 unitários e 298
de gametest**, zero falhas, com os XML mais novos que o fonte. Os dez
unitários novos são `ChestSurveyCoverageTest` (5) e `RingSweepResumeTest`
(5); os três de jogo são a prova da ruptura do P0.3, o transbordo do P0.5
e a retomada da varredura do P1.5.

**Mutação conferida nos três.** Com o `MinerHaul` voltando a depositar só
no baú do mineiro, **um** teste cai e é o do P0.5. Com o `coverage()`
voltando à forma ambígua, caem os cinco do P0.2 e nenhum outro. Com o
`CropPatch` voltando a varrer sempre do centro, **um** teste cai e é o
`FarmerGameTest.theFieldSweepResumesWhereTheBudgetStoppedIt`.

**Uma dívida que ficou pior, e fica dita.** O `FarmerWork` foi de 579
para 624 linhas, acima do teto de 500 do projeto — onde já estava antes.
O que dava para separar saiu para o `FieldRest` (100 linhas, uma pergunta
só, no corte que o `LumberjackWork` levou em 2026-08-20); o resto é o
motivo honesto dentro do `findWork`. O arquivo pede o mesmo corte em
quatro — procurar, andar, agir, guardar —, e isso é frente própria.
