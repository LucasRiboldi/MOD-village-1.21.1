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

## 2.6 Grupo G — o que a sessão de 08-15 (tarde) deixou aberto

44 minutos em jogo, sem exceção nem crash. 61 toras, 0 tábuas, 0 blocos.
Detalhe e evidência no Development-Log, entrada de 2026-08-15 tarde.

**Corrigido às 12:45:** G1 e G2 foram escritos a partir de duas sessões
que rodaram o jar de 01:09 — anterior ao `ensureTask` e ao guarda de
travamento. Verificado por dentro do jar. Nenhum dos dois é o defeito
que a linha original descrevia; os dois continuam **não verificados em
jogo**, que é coisa diferente. Ver Development-Log, entrada das 12:45.

| # | O que | Estado |
|---|---|---|
| G1 | **A obra nunca foi vista abrindo tarefa em jogo.** `ensureTask` entrou às 10:11 de 08-15 e nenhuma sessão o executou ainda | 🔒 bloqueia o MVP. **Não é regressão** — é código nunca exercitado. Jar novo instalado às 12:45; a próxima sessão é a primeira que o roda |
| G2 | **O guarda de travamento do lenhador nunca rodou em jogo.** Os 16 min congelados de 11:37 foram num jar sem `STALL_LIMIT` | 🔨 Verificar antes de investigar. Se travar de novo com o jar novo, `stall N/2400` na linha diz qual das três hipóteses vale |
| G3 | **O fabricante não fabrica** — `0/20 ticks` parado, some do relatório depois das 11:22 | Não investigado. A correção do baú (E16) pode ter removido a causa; verificar antes de investigar |
| G4 | **A colônia move o centro recusando encolher** — saiu de âncora de 6 camas para uma de 3 com `view not provably complete`, e deixou a obra 65 blocos atrás | 👤 espera decisão. É comportamento da ADR-003, não defeito declarado |
| G5 | **`BuilderWork.java` com 509 linhas** — passou do limite de 500 ao receber a instrumentação do G1 | 🔨 extrair os métodos de log para uma classe irmã |

## 2.7 Grupo H — as regras novas de 08-15

Enunciados em `Project-State.md` §18, Regras 7 a 11. A Regra 7 já está
feita. As três seguintes têm decisões em aberto que o enunciado não
resolve, e nenhuma delas deve começar antes de decidida — estão listadas
na seção de cada regra.

| # | Regra | Estado |
|---|---|---|
| ~~H1~~ | ~~**Regra 7** — o lenhador planta onde cortou~~ | ✅ feita em 08-15, dois testes de jogo pelo caminho do trabalhador |
| H2 | **Regra 8** — um baú ao lado de cada cama | 👤 espera decisão. A amarração cama–baú já existe; falta **criar** o baú. É a maior escrita no mundo do jogador que o mod já faria, e esbarra em "a colônia não cria recurso" |
| H3 | **Regra 9** — subir e descer para alcançar, e poder voltar | 👤 espera decisão: só navegação sem limite de altura, ou o aldeão põe e tira bloco para chegar? As duas leituras dão trabalhos muito diferentes. A metade "poder voltar" vale sozinha e não depende disso. Fecha o G2 |
| H4 | **Regra 10** — o construtor fabrica o que a expansão pede | 👤 espera decisão sobre baú comunitário e sobre onde termina o fabricante. As peças existem: `CraftingLookup`, `ChestWithdrawer`, `ManufacturerWork` |
| H5 | **Regra 11** — uma de cada profissão em cada vila | 🔨 mecanismo já pronto (`vacancy` devolve a mais escassa). Falta a garantia: vila pequena demais, e a dispensa podendo tirar o último de uma profissão. Nenhum teste afirma o piso |

## 2.5 Contagem

```text
375 testes unitários     lógica pura do Core e serialização NBT
 85 testes de jogo       a fronteira, num servidor sem cliente
```

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
| B3 | **TASK-044** — a fusão fica com **tudo** das duas: trabalhadores, baús e construções | 📐 exige **ADR nova** |
| B4 | **TASK-046 / E8** — o Core **aprende a falar de orientação**; escada, porta e cama saem como o arquivo manda | 📐 exige **ADR nova** — é a que emenda a ADR-005 |
| B5 | Ícone novo, entregue pelo autor | ⏳ falta o arquivo entrar em `assets/villagecolony/icon.png` |

## Grupo C — Não precisa de decisão nem de jogo 🔨

| # | O que | Origem |
|---|---|---|
| C1 | **TASK-043** — estender a estrada. A metade que falta da Regra 6: hoje a vila só constrói em beira de rua que já existe, e quando ela acabar a colônia para de crescer antes do que a regra permite. Pavimentar não custa material | §8 |
| C2 | **P2** — a metade estrutural da Regra 3: perguntar ao jogo quais blocos são peça de vila gerada | §8 |
| C3 | **P3** — o lado do cliente: nome sobre a cabeça, rachadura no bloco, braço balançando | §8 |

## Grupo D — Erros abertos

| # | Erro | Estado | Origem |
|---|---|---|---|
| D1 | **E3** — sobra de colheita é perda de item | Conhecido e aceito. A Regra 1 o torna raro por construção | §17 |
| D2 | **E4** — `path held: no` e o aldeão chega assim mesmo | **Provável, não verificado.** Se um dia ele parar de chegar, é o primeiro lugar a olhar | §17 |
| D3 | **E5** — colheita de outras espécies nunca vista em jogo | Só carvalho. O mangue é o mais provável de falhar primeiro | §17 |
| D4 | **E8** — orientação dos blocos | = B4. A metade das duas partes fechou em 08-15; a da orientação continua. Nunca visto, porque a Fase 10 nunca rodou | §17 |
| D5 | **E9** — colônia `ABANDONED` desmarcada no ciclo seguinte | **Provável, não investigado.** Hoje `ABANDONED` não muda nada, o que esconde o sintoma | §17 |
| ~~D6~~ | ~~**E11** — rodízio de profissão~~ | ✅ fechado em 08-15: a colônia conta baús distintos, e não candidatos | §17 |
| D7 | **E13** — hábito a criar: conferir a linha de carregamento do log antes de concluir do silêncio de uma fase | §17 |

## Grupo E — Dívidas de verificação e teste

| # | O que | Origem |
|---|---|---|
| E1 | **TASK-050 sem teste.** O guarda de travamento: 2.400 ticks contra uma bateria que roda em 5 s | §9 |
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
só bioma PLAINS                       aglomerado em outro bioma é
                                      ignorado (ADR-003 §5)

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

A FAZER, nesta ordem

  1  A1        a sessão do P1            🔒 bloqueia o MVP, e é o único
                                            que bloqueia
  2  B5        o ícone novo              ⏳ falta o arquivo entrar
  3  E9        investigar a oscilação    🔨 vem ANTES da B2
  4  B2        ABANDONED para de crescer 🔨 depois do E9
  5  C1        estender a estrada        🔨
  6  F3        Task.age                  🔨
  7  B4        ADR da orientação         📐 decisão tomada, ADR por
                                            escrever
  8  B3        ADR da fusão              📐 idem
  9  C2, C3    a Regra 3 estrutural, e o cliente
 10  F4        ItemRequest               só depois do MVP fechado
```

Nada dos itens 3 em diante muda o fato de que falta um passo do MVP, e
que o que falta é ver bloco ser posto.
