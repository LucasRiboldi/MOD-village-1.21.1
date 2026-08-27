# TODO

**Atualizado:** 2026-08-26, depois da sessão das 23:14 — a que abriu a
primeira mina e terminou a primeira casa sem o jogador encher baú.

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
476 testes unitários  ·  171 testes de jogo  ·  31 regras (2 emendas)  ·  9 ADRs
9 arquivos de código acima de 500 linhas  ·  6 de teste  (recontados em 08-26)
última sessão de jogo em 2026-08-26, 23:14  ·  18 minutos  ·  1 casa, 1 mina
```

---

## ✅ Resolvido

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
| **E30** | **A galeria engoliu os dois mineiros.** `could not reach the stone at {x=715, y=44, z=885} — task back to the queue`, os dois no mesmo tique, às 23:23:08, a 19 blocos de profundidade | **Visto em jogo, uma vez.** Depois disso ninguém minerou mais nos 10 minutos restantes. É o achado mais grave da sessão das 23:14: a mina abre, produz, e então se fecha sozinha. Sem diagnóstico — não se sabe se é o caminho de volta, o alcance, ou a descida do poço |
| **E31** | **O relatório da barreira afirma o que não mediu.** [`TestBarrier.report()`](src/main/java/com/villagecolony/fabric/work/TestBarrier.java) decide só por `SKIPPED.isEmpty()`, e não sabe se houve obra | **Visto em jogo, na sessão das 23:06.** Zero obras, zero projetos, e mesmo assim saiu `covered for nothing this session — Rule 28 can go`. Numa sessão sem construção a linha é vazia, e ela está marcada como *a notícia boa* no §"falta ver em jogo" — o conserto é exigir que **alguma peça tenha sido assentada** antes de a frase valer |
| **E21** | **`theStoneLeavesTheWorldAndReachesTheChest`** disse "a pedra não chegou ao baú" uma vez | Suspeita: custo de ler estrutura no tique. **Suspeita, não diagnóstico**. Não repetiu em 7 rodadas de 08-25 |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte | **Silêncio na sessão de 08-25** — nenhuma colônia trocou de estado três vezes em 42 minutos. É notícia boa e não é prova: nenhuma colônia da sessão foi abandonada |
| **E4** | `path held: no` e o aldeão chega assim mesmo | Provável, nunca verificado. Nenhuma linha dessas em 08-25 |
| **E3** | Sobra de colheita é perda de item | Conhecido e aceito. Nenhum baú encheu em 08-25 |

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
- **E aí a galeria o engoliu.** É o **E30**: os dois mineiros deram
  `could not reach the stone` no mesmo tique, e a mina não produziu mais
  nada nos dez minutos seguintes. **Abrir deixou de ser o gargalo;
  continuar passou a ser.**
- **A pedra de superfície continua sem prova** — nunca foi exercitada,
  porque a busca nunca precisou dela. A linha é
  `no miner surface stone work: ...`.

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
- **A varredura recomeça do centro a cada obra fechada, e custa 17
  passagens.** Medido em 08-26 pela decisão 8: 16.641 colunas, teto de
  1.024 por passagem, 30 s por ciclo — **8,5 minutos** entre uma casa e
  a próxima ter chance de nascer. Foi o que se viu às 23:25:22, logo
  depois da primeira casa pronta. O conserto é **no jeito de procurar**,
  não no volume.
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
| **1** | **A galeria continuando** | `Miner ... took` **depois** de um `could not reach the stone`. É o E30, e é o que mais falta: a mina abre, produz 43 blocos e se fecha sozinha |
| **2** | **O fundidor assando** | `Smelter ... made ...`. Não depende mais da mina — depende da **areia**, e é a cadeia 8 abaixo |
| **3** | **A cadeia da areia inteira** | meta de `SAND` → praia → vidro → vidraça. Em 08-25 parou em `looking for sand, 0 of 6`; em 08-26 nem começou, e mandou 3 `glass_pane` para a barreira |
| **4** | **A casa inteira sem a barreira** | `TEST BARRIER covered for nothing` **numa sessão que construiu**. Em 08-26 a casa subiu, mas com 19 peças da barreira — e a linha limpa da sessão anterior era o E31, não notícia boa |
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
