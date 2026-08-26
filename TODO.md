# TODO

**Atualizado:** 2026-08-26, depois de duas sessões de jogo e do ciclo que
tirou os becos sem saída que elas revelaram.

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
472 testes unitários  ·  165 testes de jogo  ·  30 regras  ·  9 ADRs
7 arquivos de código acima de 500 linhas  ·  5 de teste
2 sessões de jogo em 2026-08-25  ·  6 commits desde a última
```

---

## ✅ Resolvido

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
| **E20** | **`theStallGuardReturnsTheTaskAndForgetsTheTree` instável** | **Voltou a reproduzir em 08-25: 3 falhas em 7 rodadas.** Antes disso eram 12 rodadas limpas, e é essa alternância que o torna difícil. Duas hipóteses já caíram: o relógio compartilhado do mundo (as três horas usadas estão todas dentro do expediente) e o limite global de travamento (só um teste o mexe) |
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
saindo da frente** · casa terminada uma vez, em 08-19, com baús que o
jogador encheu.

### Nível 1 — a raiz do material *(com alternativa, e não visto)*

- **A mina não abriu na sessão de 08-25**, e não foi por silêncio: as
  doze colunas foram tentadas e nenhuma serviu. Duas respostas entraram
  em 08-26 — **vinte e quatro colunas** em vez de doze, e **pedra de
  superfície** quando nem elas servem.
- A **decisão 1 deixou de travar**: sem boca a colônia agora raspa
  afloramento em vez de ficar sem pedra. O que a decisão ainda decide é
  o caso raro em que não há nem boca nem afloramento — aí a vila
  continua sem pedra, e a pergunta é se ela declara `BLOCKED` e muda de
  objetivo, que é Nível 4.
- **Nada disso foi visto em jogo.** A linha a procurar é
  `Miner ... opens a mine at`, e a alternativa aparece como
  `no miner surface stone work: ...` quando também falha.

### Nível 2 — material processado *(feito, e passando fome)*

- **E18 fechado em 08-22**, e pelo caminho genérico que a ADR pediu:
  `Production` declarada no recurso, e o fundidor perguntando ao livro
  de receitas do jogo. **Nenhuma sessão viu isso rodar** — a linha a
  procurar é `Smelter ... made minecraft:smooth_sandstone`.
- 08-25 mostrou que o elo **não está quebrado, está sem entrada**: o
  fundidor apareceu vivo e olhando o baú quinze vezes, sempre com
  `Smelter ... stopped — nothing in the colony chests to smelt`. Ele
  espera o Nível 1, e não uma correção sua.

### Nível 3 — a obra termina sem o jogador

- Casa nunca terminou com material que a colônia mesma fez. Em 08-25 a
  obra de 149 blocos pôs **um** e morreu esperando pedregulho:
  `WAITING_RESOURCES ... waiting for minecraft:cobblestone` por dez
  minutos, até o `PatienceClock`.
- **A barreira de teste risca antes de a cadeia ter chance.** Ela pulou
  `stripped_oak_log` às 21:56:15; o fabricante descascou o primeiro
  tronco às 22:00:19 — quatro minutos depois. A peça foi riscada de uma
  cadeia que **funcionava**, só não a tempo. Enquanto a Regra 28 valer,
  a soma da sessão superestima o que está quebrado.
- **E21** — instrumentar antes de corrigir.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
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
| 🔴 | **`ResourceSubstitution` é binária; a ADR-009 §3.10 pede quatro níveis** (`PREFERRED / ACCEPTABLE / ALTERNATIVE / FORBIDDEN`). A versão binária conserta o defeito e **não** implementa a política |
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **`ChestWithdrawer.takeGroup` ainda usa grupo como equivalência.** Hoje é inócuo — só o fundidor o chama, com `SAND` e `IRON`, que têm um membro só. É o resto do buraco |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |
| 🟡 | **ADR-009 §17 (população por capacidade) vs o vanilla**, que controla o *breeding*. O mod não tem como segurar população |
| ✅ | **ADR-009 §14 vs Regra 27 — resolvida.** O propósito da estrutura sai do nome, e o catálogo do jogo já os tem. Nenhuma contradição |

---

## 👤 Decisões que faltam, na ordem em que travam

| | Decisão | Trava |
|---|---|---|
| 1 | **Mina sem lugar:** quando não há nem boca nem afloramento, a vila aceita boca ruim, tenta outro raio, ou declara `BLOCKED` e faz outra coisa? | **Deixou de travar em 08-26** — a pedra de superfície cobre o caso comum. O que sobra é o terreno em que nem ela serve |
| 2 | **Substituição:** fica binária ou vira os quatro níveis da ADR? `cut_sandstone` serve no lugar de `smooth_sandstone`? | já não trava o Nível 2 — a fornalha faz o liso. Trava a **variedade** |
| 3 | **Regra 28:** sai quando o planejador souber desistir, ou antes? | Nível 4 |
| 4 | **Regra 25:** morre ou volta? Hoje é lógica morta | limpeza |
| 5 | **Água e comida:** o mod planta e coloca água, ou fazenda fica fora do escopo? | Nível 6 e a alternativa de planície |
| 6 | **População:** o mod controla, ou aceita o *breeding* do vanilla? | ADR-009 §17 |
| 7 | **Fusão de colônias:** qual UUID e o teto de profissão já estão decididos na ADR-007 — falta só escrever | Nível 6 |
| 8 | **O custo da busca de lote (E26):** a vila cheia responde a cada nove minutos. Sobe o teto de colunas e aceita o tique mais pesado; varre só o que pode ser beira de rua; ou aceita que vila cheia cresce devagar? | Quanto tempo uma vila leva para abrir obra nova |
| 9 | **O cursor da varredura sobrevive ao centro se mover** — é decisão de 08-19, com teste (`theSweepSurvivesTheCenterMoving`) e motivo escrito: a âncora trocava a cada trinta segundos e zerava a busca. **A ADR-003 Emenda 4 mudou essa premissa** — hoje o centro anda pela sonda, e raramente. O preço atual: depois de um movimento de centro, a varredura retomada pula os anéis de dentro do centro **novo**, que é onde o lote é mais provável. Reverter mexe numa decisão testada, e por isso não foi feito | A vila que acabou de mover o centro |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto. Riscado é o que a sessão de
**2026-08-25** viu acontecer.

| | O que | A linha que prova |
|---|---|---|
| **1** | **A mina abrindo** | `Miner ... opens a mine at ...`. Em 08-25 saiu a outra linha, com doze colunas; agora são vinte e quatro. Se falhar de novo, a linha nova a procurar é `no miner surface stone work` — e o mineiro deve estar cavando afloramento mesmo assim |
| **1** | **A rua crescendo e a casa nascendo junto** | `extended the road N blocks ...` seguido de `planned ... at ...` **no mesmo ciclo**. Era o que custava oito minutos, e é a correção mais visível do ciclo de 08-26 |
| **2** | **O mineiro cavando** | `Miner ... took` de um arenito. Depende de 1 |
| **3** | **O fundidor assando pedra** | `Smelter ... made minecraft:smooth_sandstone`. Em 08-25 ele passou a sessão inteira em `nothing in the colony chests to smelt` — depende de 1 |
| **4** | **A boca mobiliada** | `Mine mouth at ... furnished — miner chest at ..., lantern at ...`. Depende de 1 |
| **4** | **A casa de deserto subindo** | `blocks left` caindo de 113. Nenhuma sessão em deserto ainda |
| **5** | **A barreira de teste limpa** | `TEST BARRIER covered for nothing` é a notícia boa. Em 08-25 deu `covered for 1: 1x stripped_oak_log` — e por atraso, não por cadeia quebrada |
| **6** | **A casa esperando a cama**, e o pastor tosquiando por causa disso | `WAITING_RESOURCES` por `white_bed`. Em 08-25 a obra esperou por **pedregulho** e morreu antes de chegar na cama |
| **7** | ~~**O fabricante descascando**~~ · falta pastor e fundidor | ✅ `stripped a oak_log`, 08-25. Faltam `sheared` e `made minecraft:glass` |
| **8** | **A cadeia da areia inteira** | meta de `SAND` → praia → vidro → vidraça. Em 08-25 parou no começo: `looking for sand, 0 of 6` |
| **9** | ~~**O E9**~~ | ✅ silêncio no relatório de 08-25 — mas nenhuma colônia da sessão chegou a ser abandonada, então o caso não foi exercitado |
| **10** | **Fechar e reabrir o mundo com mina aberta** | O save de 08-25 trouxe `1 mines` e devolveu `1 mines`, mas ninguém cavou: o **registro** sobrevive, a **galeria retomada** continua sem prova |
| **11** | **A correção do E22** | Um construtor morrer no meio da obra e a vila continuar de pé. Corrigido em 08-25, e a correção não foi vista em jogo |
| **12** | **A pedra de superfície** | `Miner ... took minecraft:stone from ...` sem nenhuma linha de mina antes. É a alternativa que tira a vila do beco quando a boca não nasce |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície —
taiga, savana, nevada e deserto nunca rodaram, e a sessão de 08-25
também foi em planície. E fechar e reabrir o mundo **com um mineiro
cavando** nunca foi feito (dívida E4 do Backlog).
