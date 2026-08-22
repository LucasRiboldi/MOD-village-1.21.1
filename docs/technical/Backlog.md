# Backlog.md

# Village Colony — O que está feito e o que falta

**Atualizado:** 2026-08-15
**Natureza:** índice consolidado. **Este documento não decide nada.**

Ele junta num lugar só o que hoje está espalhado por cinco seções do
`Project-State.md` (§6 implementação, §8 fila, §9 limites, §10 decisões,
§17 erros), pela ressalva do `Construction-System.md` e pela fila da
`docs/workers-analysis/`.

Onde este documento e o `Project-State.md` discordarem, **vale o
Project-State**: é ele que se atualiza ao fim de cada sessão. Aqui a
regra é apontar, não repetir a razão de cada item — a razão mora na
seção de origem, e cada linha diz qual é.

---

# 1. Resumo

```text
FEITO e verificado em jogo        5 dos 6 passos do MVP
FEITO e coberto por teste          1 passo do MVP + 3 itens fora de fase
FALTA                             30 itens, em 8 grupos
                                  (as 5 decisões estão TOMADAS; o que
                                   falta delas é implementar)

do que falta, o que bloqueia       4 itens — todos do grupo A
o resto                           depois, e nesta ordem: B → C → F
```

---

# 2. Feito

## 2.1 Os seis passos do MVP

```text
detectar vila             ✅ verificado em jogo
registrar aldeões         ✅ verificado em jogo
organizar trabalhadores   ✅ verificado em jogo
coletar recursos          ✅ verificado em jogo
produzir materiais        ✅ verificado em jogo    2026-08-14
construir expansão        🧪 coberto por teste, nunca visto em jogo
```

## 2.2 Fases

```text
Fase 0   decisões de arquitetura       ✅ ADR-001 a ADR-006 aceitas
Fase 1   núcleo da colônia             ✅ verificado em jogo
Fase 2   persistência                  ✅ verificado em jogo
Fase 3   detecção da vila              ✅ verificado em jogo
Fase 4   trabalhadores                 ✅ verificado em jogo
Fase 5   armazenamento                 ✅ verificado em jogo
Fase 6   recursos                      ✅ verificado em jogo
Fase 7   tarefas                       ✅ verificado em jogo
Fase 8   primeiro trabalhador          ✅ verificado em jogo
Fase 9   fabricação                    ✅ verificado em jogo
Fase 10  construção                    🧪 coberta por teste
Fase 11  registro de infraestrutura    🧪 coberta por teste
Fase 12  testes do MVP                 🧪 TASK-038 a 040 feitas;
                                          041 e 042 exigem sessão
```

Detalhe por tarefa em `Project-State.md §6`.

## 2.3 Fora de fase, fechado

```text
item A    ColonyState.ABANDONED com escritor     🧪 08-13
item B    aviso de colônias sobrepostas          🧪 08-13
item C    ferramenta inicial do trabalhador      ✅ 08-14
TASK-045  proteção consulta construções          🧪 08-14
TASK-047  a grama do campo não reprova o lote    🧪 08-15
TASK-050  a tarefa volta à fila quando o
          trabalhador não chega                  ⚠️ 08-15, SEM TESTE
TASK-053  a obra ganha tarefa — o construtor      🧪 08-15, 3 testes,
          nunca teve o que fazer em jogo             os três rodados
                                                     contra a regra
                                                     desligada
TASK-052  a porta vira porta, e não duas          🧪 08-15, 2 testes de
          metades soltas — o F2 da fila                jogo, os dois
                                                       rodados contra a
                                                       regra desligada
TASK-051  o motivo de não trabalhar como valor   🧪 08-15 (IdleReason,
          — o F1 da fila do Workers                 IdleLog), 7 testes,
                                                    2 rodados contra a
                                                    regra desligada

extra     a Fase 10 diz por que não construiu    🧪 cinco motivos
extra     a varredura inacabada não é resposta   🧪 sweepPausedAt
extra     a análise do mod Workers               ✅ doze documentos
```

## 2.4 Erros fechados

```text
E1   a fila de tarefas não esvazia          08-11
E2   a colônia nunca encolhe                08-12
E6   o lenhador que não cortava             08-13
E7   proteção não via construções           08-14
E10  o fabricante lia o baú errado          08-14  ✅ em jogo
E12  "Equipped N workers" nunca apareceu    08-14  ✅ em jogo
E13  a sessão rodou um jar velho            08-14  (fica como hábito)
E16  tarefa de baú ia para quem não tinha   08-15  ✅ em jogo (diagnóstico)
D1   a marca do baú trocava de dono         08-12
D2   vaga de profissão entre vilas          não era defeito
D8   profissões estagnadas em 5             não era defeito: é MAX_PER_PROFESSION
--   travamento por tarefa RESERVED         08-12
```

**E14 saiu daqui.** Foi dado por fechado em 08-15 com base em teste, e a
sessão de jogo da tarde do mesmo dia mostrou que em jogo ele nunca
passou: 88 ciclos com `0 working` e `151 blocks left`, sem uma linha de
`opened a build task`. Ver o grupo G.

## 2.6 Grupo S — o que as sessões de jogo de 08-15 deixaram

Cinco sessões em jogo naquele dia, somando cerca de duas horas. Detalhe e
evidência no Development-Log. **Renomeado de "G" para "S" em 08-15:** já
existia um grupo G neste documento, vindo das ressalvas do
`Construction-System`, e os dois se sobrepunham.

| # | O que | Estado |
|---|---|---|
| ~~S1~~ | ~~**A obra nunca foi vista abrindo tarefa em jogo**~~ | ✅ **visto em 08-15, 13:12.** `opened a build task`, `1 working ... RESERVED by a1af0a01`, e a obra foi de 151 para 149 blocos — os dois primeiros blocos que este mod pôs no mundo |
| S2 | **O lenhador travado na mesma árvore** | 🟡 fechado dos dois lados no código: o guarda de travamento entrou no jar, e ao desistir ele **esquece a árvore**. Antes soltava a tarefa e a busca reescolhia a mesma. **Não reapareceu em jogo desde então** — mas também não foi visto disparando, porque nada travou |
| ~~S3~~ | ~~**O fabricante não fabrica**~~ | ✅ **não era defeito.** Na sessão das 22:01 os dois produziram 134 e 130 tábuas. Faltava tarefa aberta, e a correção do baú (E16) destravou |
| S4 | **A colônia move o centro recusando encolher** | 👤 espera decisão. A Regra 12 tirou do caminho a parte em que o centro novo era um ponto no ar; o desacoplamento entre contagem de camas e posição continua. Não se repetiu depois de 08-15 11:22 |
| S5 | **Arquivos acima de 500 linhas** | 🔨 `LumberjackWork` 1149, `VillageDetectionHandler` 901, `TreeHarvester` 651, `BuilderWork` 599, `ManufacturerWork` 510. Os três primeiros já estavam grandes; o ciclo de 08-15 piorou `LumberjackWork` e `BuilderWork` ao acrescentar instrumentação |

## 2.7 Grupo H — as regras novas de 08-15

Enunciados em `Project-State.md` §18, Regras 7 a 11. A Regra 7 já está
feita. As três seguintes têm decisões em aberto que o enunciado não
resolve, e nenhuma delas deve começar antes de decidida — estão listadas
na seção de cada regra.

| # | Regra | Estado |
|---|---|---|
| ~~H1~~ | ~~**Regra 7** — o lenhador planta onde cortou~~ | ✅ feita em 08-15, dois testes de jogo pelo caminho do trabalhador |
| H2 | **Regra 8** — um baú ao lado de cada cama | 🟡 **metade feita em 08-15.** `ChestPlacer` põe o baú quando a cama de um **trabalhador** não alcança nenhum, com 5 recusas e 6 testes de jogo. Falta a outra metade: cama de aldeão que não trabalha continua sem baú, porque `VillageCandidate` carrega contagem e não as posições das camas |
| ~~H3~~ | ~~**Regra 9** — subir e descer para alcançar, e poder voltar~~ | ✅ feita em 08-15. Árvore fora de alcance sai da escolha por 6.000 ticks, e quem a marca é o guarda de travamento. A checagem de caminho **antes** de escolher foi tentada e descartada: recusou seis árvores boas na bateria |
| H4 | **Regra 10** — o construtor fabrica o que a expansão pede | 🔨 **decidido em 08-15:** acesso a todos os baús, do mais próximo para o mais longe, acumulando até juntar a quantidade; e o construtor é fabricante dos blocos da obra. **Não começado** — é a maior das cinco. Peças existentes: `CraftingLookup`, `ChestWithdrawer`, `takeMaterial` |
| ~~H7~~ | ~~**Regra 13** — a obra do MVP é uma que a colônia consiga fazer~~ | ✅ feita em 08-15. `ColonyHut`: 72 blocos, 71 tábuas e 1 porta, tudo saindo de tronco. A casa do jogo pedia 66 blocos de cadeias que não existem |
| H5 | **Regra 11** — uma de cada profissão em cada vila | 🔨 mecanismo já pronto (`vacancy` devolve a mais escassa). Falta a garantia: vila pequena demais, e a dispensa podendo tirar o último de uma profissão. Nenhum teste afirma o piso |
| ~~H6~~ | ~~**Regra 12** — o centro fica em bloco que existe~~ | ✅ feita em 08-15. Era a média das camas, um ponto que podia cair no ar ou dentro do morro. Agora é a cama mais próxima da média. Quatro testes de unidade |

## 2.5 Contagem

```text
464 testes unitários     lógica pura do Core e serialização NBT
154 testes de jogo       a fronteira, num servidor sem cliente
```

> **A ADR-009 refez a fila de prioridades em 2026-08-22.** O §4 deste
> arquivo é anterior a ela; vale o `TODO.md`.
>
> **Este arquivo parou em 2026-08-15 e está sendo alcançado aos poucos.**
> Onde ele discordar do `TODO.md`, **vale o TODO** — a precedência foi
> invertida em 2026-08-22, porque a que existia mandava confiar no
> documento mais velho.

---

# 3. Falta

Legenda: **🔒 bloqueia o MVP** · **👤 espera decisão do autor** ·
**🔨 dá para fazer hoje**

## Grupo A — Bloqueia o MVP 🔒

Só sai com sessão de jogo. **É o único grupo que separa o MVP de
fechado.**

| # | O que | Origem |
|---|---|---|
| A1 | A casa subindo: a linha `planned … 151 blocks`, o lote escolhido, e a obra bloco a bloco | §8 P1 |
| A2 | Fase 11 em jogo: a casa pronta virando infraestrutura, e o lote seguinte não caindo em cima dela | §8 P1 |
| A3 | Itens A e B em jogo: `is now ABANDONED` ao demolir camas, e o aviso de sobreposição | §8 P1 |
| A4 | TASK-041 e TASK-042 — os dois testes do MVP que a bateria não alcança | §6, Fase 12 |

**A sessão precisa ter:** um construtor na vila, `/time set noon`, e
**pedra e vidro nos baús** — a colônia produz tábua e nada mais, e a
casa pede 43 de pedra. Sem isso a obra fica em `WAITING_RESOURCES`, que
é o comportamento certo e não uma casa.

O jar precisa ser trocado com o jogo fechado (§11), e a primeira coisa
a olhar no log é a linha de carregamento (E13).

## Grupo B — Decididas em 2026-08-15, à espera de implementação

**As cinco foram decididas.** O que falta é escrevê-las, e a ordem está
no §4. O enunciado de cada decisão está em `Project-State.md §10`.

| # | Decisão | Estado |
|---|---|---|
| ~~B1~~ | **TASK-049 / E11** — só dispensa quem não tem baú quando há baú livre **de verdade** para o substituto | ✅ **feita em 08-15**, 2 testes de jogo |
| B2 | **TASK-048** — a colônia `ABANDONED` **para de crescer** e continua colhendo | 🔨 **investigar o E9 antes.** Se a marca oscila, a decisão silencia colônia viva |
| B3 | **TASK-044** — a fusão fica com **tudo** das duas: trabalhadores, baús e construções | ✅ **ADR-007 escrita em 08-21**; falta implementar |
| B4 | **TASK-046 / E8** — o Core **aprende a falar de orientação**; escada, porta e cama saem como o arquivo manda | ✅ **ADR-008 escrita em 08-21**, forma (a); falta implementar |
| ~~B5~~ | Ícone novo, entregue pelo autor | ✅ **feito em 2026-08-21**, com o fundo recortado a partir da borda |

## Grupo C — Não precisa de decisão nem de jogo 🔨

| # | O que | Origem |
|---|---|---|
| ~~C1~~ | ✅ **feita em 2026-08-21** — é a Regra 15. **TASK-043** — estender a estrada. A metade que falta da Regra 6: hoje a vila só constrói em beira de rua que já existe, e quando ela acabar a colônia para de crescer antes do que a regra permite. Pavimentar não custa material | §8 |
| C2 | **P2** — a metade estrutural da Regra 3: perguntar ao jogo quais blocos são peça de vila gerada | §8 |
| C3 | **P3** — o lado do cliente: nome sobre a cabeça, rachadura no bloco, braço balançando | §8 |

## Grupo D — Erros abertos

| # | Erro | Estado | Origem |
|---|---|---|---|
| D1 | **E3** — sobra de colheita é perda de item | Conhecido e aceito. A Regra 1 o torna raro por construção | §17 |
| D2 | **E4** — `path held: no` e o aldeão chega assim mesmo | **Provável, não verificado.** Se um dia ele parar de chegar, é o primeiro lugar a olhar | §17 |
| D3 | **E5** — colheita de outras espécies nunca vista em jogo | Só carvalho. O mangue é o mais provável de falhar primeiro | §17 |
| D4 | **E8** — orientação dos blocos | = B4. A metade das duas partes fechou em 08-15; a da orientação continua. Nunca visto, porque a Fase 10 nunca rodou | §17 |
| D5 | **E9** — colônia `ABANDONED` desmarcada no ciclo seguinte | **Instrumentado em 2026-08-21** — `ColonyStateLog` diz de onde veio, o que a sonda viu nos dois sentidos, e nomeia quem trocou três vezes ou mais. Falta a sessão de jogo que responde | §17 |
| ~~D6~~ | ~~**E11** — rodízio de profissão~~ | ✅ fechado em 08-15: a colônia conta baús distintos, e não candidatos | §17 |
| ~~D8~~ | ~~**E17** — `SANDSTONE` e `COBBLESTONE` no mesmo grupo~~ | ✅ **fechado em 2026-08-22** pela ADR-009 §3.4: grupo classifica, substituição se declara. `ResourceSubstitution` | §17 |
| D9 | **E18** — a colônia cava arenito e a casa pede arenito **liso**; ninguém funde pedra | **Achado em jogo, 2026-08-22.** É o único elo entre a obra de deserto e a casa de pé | §17 |
| D7 | **E13** — hábito a criar: conferir a linha de carregamento do log antes de concluir do silêncio de uma fase | §17 |

## Grupo E — Dívidas de verificação e teste

| # | O que | Origem |
|---|---|---|
| ~~E1~~ | ~~**TASK-050 sem teste.** O guarda de travamento~~ | ✅ fechado em 08-15: o autor autorizou encurtar o limite para a bateria. `shortenStallLimitTo` / `restoreStallLimit`, e um teste que prova as duas coisas que o guarda faz — devolver a tarefa e esquecer a árvore |
| E2 | **Teste sem fase vermelha:** `anUnfinishedSweepIsNotAnAnswer` afirma propriedade que não existia antes dele | Dev-Log 08-15 |
| E3 | **WARN de 86 ms** — um ciclo passou de um tick, uma vez. Não se repetiu, não foi investigado | Dev-Log 08-15 |
| E4 | **Persistência entre sessões** — exige fechar e reabrir o mundo; a bateria roda um servidor só | §6 |
| E5 | **Tudo que só o cliente mostra** — fora do alcance da bateria por construção | §6 |

## Grupo F — Fila da análise do Workers

Nenhum destes é o MVP. Ordem por retorno.
Detalhe em `docs/workers-analysis/12-recommendations.md`.

| # | O que | Custo / risco |
|---|---|---|
| ~~F1~~ | ~~**`IdleReason`**~~ | ✅ **feito em 2026-08-15.** Ver §2.3 |
| ~~F2~~ | ~~**Blocos de duas partes**~~ | ✅ **feito em 2026-08-15.** A pista se confirmou: o Core não mudou uma linha |
| F3 | **`Task.age`** — envelhecimento contra inanição | Trivial. Um `int` e um incremento |
| F4 | **`ItemRequest`** — o trabalhador declara o que lhe falta | Médio / **médio**. Toca `Task`, que é o centro. Só depois do MVP |
| F5 | **`ProfessionSpec`** — parâmetros de profissão como dado | Baixo. Vira problema na quinta profissão |
| F6 | **Qualidade da casa** — cinco bits dizendo se ela é habitável | Médio. Fecha o ciclo da Fase 11 |

Menores, catalogados e sem tarefa própria: desempate por prontidão,
preferência pelo último baú, portão de notificação por dia de jogo,
prefetch de materiais.

## Grupo G — O documento descreve o que o código não faz

`Construction-System.md`, três divergências assumidas em 2026-08-14.

| # | O que |
|---|---|
| G1 | **§PREPARING** — onde o projeto pede ar (o interior dos cômodos), nada é escrito, e a grama fica **dentro** da casa. Cosmético |
| G2 | **§Estradas** — manda construir a rua antes da casa; o código só usa rua que já existe. É a C1 |
| G3 | **§Modelo de Construção** — prevê `rotation`; o modelo não tem. Girar só faz sentido quando houver rua com direção |

## Grupo H — Limites assumidos

**Não são tarefas.** São decisões que ficam, registradas para que
ninguém as descubra de novo como se fossem defeito.

```text
(vencido em 2026-08-20)               a paleta por bioma abriu taiga,
                                      savana, nevada e deserto; a arena
                                      da bateria é que continua PLAINS

registro único, Overworld             COLONIES e WORKERS são estáticos
                                      e não separam dimensão

profissão não muda depois de          ProfessionAssigner só preenche
atribuída                             vaga; realocar quem está vivo não
                                      pertence ao MVP

a ferramenta existe e ninguém a vê    o modelo de aldeão do Vanilla
                                      nunca monta HeldItemFeatureRenderer

o mixin no cérebro do aldeão          nunca foi testado junto com outro
                                      mod de aldeão

duas vilas encostadas disputam        até a fusão existir (B3)
trabalhador

raio de detecção menor que a vila     mitigado por observedBeds, não
                                      eliminado

seed/locate falham no runServer       Vanilla puro; testes que dependem
                                      deles vão para o jogo real
```

---

# 4. A ordem

```text
FEITO em 2026-08-15

  B1   o E11                    contar baús distintos, e não candidatos
  F1   IdleReason               o motivo de não trabalhar como valor
  F2   blocos de duas partes    a porta vira porta
  TASK-053                      a obra ganha tarefa — o defeito que a
                                sessão do P1 achou
  TASK-050                      a tarefa órfã volta para a fila

A FAZER, nesta ordem — revista em 2026-08-22

  1  A1        a sessão do P1            🔒 bloqueia o MVP, e é o único
                                            que bloqueia
  2  B4        implementar a orientação  🔨 ADR-008 escrita; é a que
                                            muda o que se vê
  3  E9        ler o que a instrumenta-  🔨 ColonyStateLog já mede;
               ção disser numa sessão       falta a sessão
  4  B2        ABANDONED para de crescer 🔨 depois do E9
  5  F3        Task.age                  🔨
  6  B3        implementar a fusão       🔨 ADR-007 escrita; depende de
                                            a construção rodar em jogo
  7  C2, C3    a Regra 3 estrutural, e o cliente

  FEITAS DESDE 08-15:  B1  B5  C1  D6  E1
 10  F4        ItemRequest               só depois do MVP fechado
```

Nada dos itens 3 em diante muda o fato de que falta um passo do MVP, e
que o que falta é ver bloco ser posto.
