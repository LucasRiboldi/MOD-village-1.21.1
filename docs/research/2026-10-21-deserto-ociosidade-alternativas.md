# Ociosidade e bloqueios no deserto — causas medidas e alternativas (2026-10-21)

> Playtest do autor, 0.3.85 (confirmado no log: `villagecolony 0.3.85`), 15:31–16:32, mundo de deserto
> (vila 153,-494). Spark `IAXTyE0LXM`. Relatório da sessão:
> [`docs/testing/relatorios/deserto-2026-10-21.md`](../testing/relatorios/deserto-2026-10-21.md) (nota 6,1).
> Só pesquisa: nada foi alterado no código. As linhas de código citadas são de `src/main/java/com/villagecolony/`.

## 0. Resumo

| # | Problema | Causa medida | Alternativa recomendada | Decisão do autor? |
|---|---|---|---|---|
| P1 | Construtor 72% ocioso; carpinteiro e pedreiro param em cascata | **não existe lote** ao lado das ruas; a rua que entra na faixa anexada para em 1 bloco | A1 telemetria da rua → A2 rua da faixa com alternativas | A5 e A6 sim |
| P2 | Fundidor espera 52%; arenito liso surge 3× | a rota "mina" do arenito pula a receita areia → arenito | B1 fundidor pede arenito ao pedreiro quando há areia | não |
| P3 | Mineiro c37e298f 15% encalhado | o ponto de trabalho escolhido fica a 4,12 blocos da pedra (alcance 4) | C1 conferir alcance 3D no `judge` | não |
| P4 | Pastor 92% ocioso | metade é medição (o trato dos animais não conta); o resto é falta de comida | D1 contar o trato; D2 reservar comida de cria | D2 sim |
| P5 | Fazendeiro 40% esperando | colheita guardada no baú dele; descanso de 2 min | E1 medir antes | — |
| P6 | Lenhador 93% ocioso | deserto quase sem árvore; o plantio exige terra | F1/F2 | sim (Regra 59) |
| P7 | 7 de 16 sem ofício | cota de 9 vagas para 16 adultos (lote de 15) | G1 | sim |

**Primeiro passo recomendado: A1 + B1 + C1.** Os três são pequenos, não pedem decisão e atacam as três
maiores perdas medidas.

## 1. O que a sessão mediu (e o que eu tinha lido errado)

- **Desempenho ok:** TPS 20 nos 60 minutos; MSPT mediano de 5 a 8 ms; picos de 315/440 ms só na carga do
  mundo. O mod ocupa ~0,8% da thread do servidor.
- **Obras:** 3 em 57 minutos (casas pequenas 2, 5 e 3). Houve obra aberta só ~23 minutos; nos outros ~34 não
  havia lote.
- Linha `sweep:` do fim da sessão: *472 planner runs, 472 passes over 29646 columns, 472 answered by the index
  — 0 restarts, 0 complete rounds*.

Correções de leitura, que mudam o que vale atacar:

1. **A busca de lote não está sem tempo.** Ela roda ~63 colunas por passada (`SweepDeadline.MIN_COLUMNS=64`,
   prazo de 15 ms em `PlanningBudget.DEADLINE_MS`), percorre o índice de ruas (440–558 colunas) a cada ~30 s e
   o descarta depois de 4 voltas vazias (`SweepState.EMPTY_ROUNDS_BEFORE_DROP=4`). "Budget ran out" é o estado
   normal de uma busca em andamento. **A resposta "sem lote" é verdadeira.** Aumentar o prazo não criaria lote.
2. **As 621 mil recusas por "não é solo natural" são tentativas, não colunas** (`LotRefusals.java:123`). Elas
   batem em laje e escada de telhado, tocha e plantação porque `LotGround.groundInColumn` pega o bloco mais alto
   da janela. Isso custa leitura, mas não é o que impede a obra.
3. **Não há laço do lenhador nem do fazendeiro.** O 243/253 do diário é o tamanho da cota da tarefa (campo
   `n`); cada um pegou e soltou a tarefa uma vez.
4. **A espera depois de cada bloco minerado não é a causa do mineiro:** os 578 blocos esperaram 1 tique cada.
5. **O pastor trabalhou:** 39 procriações e 26 abates, feitos em `ShepherdFlock.tend`, fora de tarefa. Por
   isso o `WorkTime` os conta como ócio (`WorkTime.java:157-159`).

## 2. P1 — sem lote, sem obra (a raiz da cascata)

### O que o log mostra

- Recusas: 220 mil sem chão na janela; 215 mil rua reservada; 148 mil volume ocupado; **115 mil fora do
  nível da rua, de −3 a −8** (dunas além do `MAX_FILL=2`); 621 mil topo não sólido.
- Construtor de ofício, açougue, pescador, templo e curral: *"found no lot … in a whole sweep — it tries
  another kind of building for 30 minutes"*, duas vezes cada. Só as casinhas pequenas acham lugar.
- **Faixa anexada perdida.** Às 15:35 a vila anexou ao SUL (*SOUTH 423/440 flat, 0 water, 0 built*) e
  começou a rua para dentro da faixa (`StripRoad.lead`). A rua cresceu **1 bloco** (*1 of 16 on this
  stretch*) e parou sem registrar o motivo. Às 16:05 (`ANNEX_EVERY` = 30 min) a vila tentou de novo, um bloco
  adiante, com o mesmo resultado. Uma faixa quase toda plana não deu lote nenhum em uma hora.
- Enquanto isso, a Regra 15 calçou 1 a 5 blocos a cada 30 s **dentro** da vila (rua entre casas), e nenhum
  lote nasceu ao lado desses trechos.

### Por que a rua da faixa para

`RoadExtension.keepGrowing` (`RoadExtension.java:301-338`) chama `RoadPaving.pave` com uma
`PavingRefusals` nova e a descarta. Se `pave` devolve vazio, a ponta sai de castigo e o crescimento termina
(`GROWING.remove`) **sem log do motivo**. Os motivos possíveis estão em `RoadPaving.refusalAt` (:171) e
`groundNear` (:208):

- degrau maior que 1 (duna);
- bloco original da vila que não é chão de bioma;
- obra da colônia no caminho;
- chão não calçável;
- algo em cima.

Não dá para saber qual sem medir.

### Alternativas

| | Alternativa | Ganho esperado | Risco |
|---|---|---|---|
| **A1** | Logar o motivo quando a rua para (`keepGrowing` e `StripRoad.lead` registram a `PavingRefusals`) | decide entre A2, A3 e A5 com dado | nenhum; só log |
| **A2** | Rua da faixa com alternativas: se o primeiro passo recusa, tentar as outras pontas dentro da `BAND` de 24 e a ponta seguinte da mesma rua, como a `RoadExtension` já faz com 12 pontas (`RoadExtension.java:75-90`), em vez de esperar 30 min pelo próximo anexo | a faixa plana passa a dar lote | baixo; a decisão de 2026-08-25 ("mais de um candidato") é a mesma |
| **A3** | Terraplanagem fase 2: aterrar mais que 2 (`LotTerraform.MAX_FILL=2`; o corte já vai a 5) | ataca as 115 mil recusas de −3 a −8 | médio; aterro alto muda o visual e gasta areia ou arenito; plano em `docs/research/terraplanagem-da-vila.md` |
| A4 | Pular, antes de ler o chão, as colunas dentro das caixas de `BUILDINGS` e das obras abertas (`LotLevel.java:112-130`) | menos leitura por passada (desempenho) | médio; **não cria lote**, então não resolve P1 |
| A5 | Lote junto com a rua na faixa anexada: planejar o lote na faixa e a rua até ele, em vez de esperar a rua nascer | resolve a faixa de vez | alto; muda a regra "o lote só nasce encostado em rua" (Regra 56 D); **decisão do autor** |
| A6 | Encurtar `ANNEX_EVERY` (30 min) | mais faixas | alto; foi o que soltou 27 aldeões na 0.3.42 por deriva do centro (memória `abandono-por-deriva-do-centro`). **Não recomendo** |

Fora daqui: o cursor do índice é zerado sem condição a cada edição do jogador
(`RoadIndex.java:352`); `ColonyRoads.columns` é `List` com `contains` O(n). Nenhum dos dois apareceu como
causa nesta sessão (0 recomeços). Ficam como desempenho, não como defeito.

## 3. P2 — o arenito liso surge em vez de ser feito

### A cadeia, medida

- O construtor pede `smooth_sandstone`. Ela vem da fornalha (`ResourceType.SMOOTH_SANDSTONE = SMELTED`), e
  `CraftingLookup.smeltingInputsFor` aceita só `sandstone` como insumo (`SmelterWork.java:204`).
- O fundidor parou 41× com *"none of N colony chests had minecraft:sandstone to smelt"*
  (`SmelterWork.java:403`). Depois de 2 faltas (`SmelterFallback.MISSES_BEFORE_FALLBACK`), ele procura outra
  coisa para fundir, e o `SmelterGathers.handOff` só sabe pedir **areia** ao pedreiro (`Crusher.askTheMason`).
- **Onde a cadeia quebra:** `SupplyRoutes.inBiome` (`SupplyRoutes.java:38-43`) aceita primeiro a rota direta
  do bioma. `SANDSTONE` é `MINED` e está na paleta do deserto, então a rota "mina" está sempre "disponível" e a
  receita 4 areia → 1 arenito (`billFor`, :57-68) nunca é considerada. O dono do arenito é o mineiro
  (`ColonyCycle.typeFor`: MINED → COLLECT_STONE). Ele recebeu essa tarefa uma vez e a soltou para cavar ferro.
- Concorrência: o pedreiro fez 32 `cut_sandstone`, que consome o mesmo arenito cru (`MasonStock.java:21-22`).
- Resultado: o vigia G2 vê "rota aberta + tarefa aberta" (`ConstructionSupplyDispatches.java:176-179`) e faz
  o arenito liso **surgir** no baú do construtor 3 vezes. A obra anda, mas o registro de faltas mascara a causa.

### Alternativas

| | Alternativa | Risco |
|---|---|---|
| **B1** | Quando falta o insumo da fornalha e ele tem receita a partir de algo que está nos baús (aqui, areia acima da reserva de vidro `SAND_RESERVE=16`), o fundidor pede o insumo ao ofício da família (pedreiro, `CraftFamily` → CRAFT_STONE), no mesmo molde do `Crusher.askTheMason`. Generaliza o `handOff` por receita, não por nome | baixo a médio; respeita a ADR-009 (decide pela receita, não pelo nome); cuidado para não gastar a areia do vidro |
| B2 | `SupplyRoutes`: quando a rota direta falha N vezes (Regra das 3 buscas), devolver também a rota por receita | médio; mexe na previsão de todos os biomas |
| B3 | Reservar arenito cru para o fundidor antes do `cut_sandstone` (`StockRules.rawToKeep`) | médio; mexe em todo material de fornalha (Regra 27) |
| B4 | Só corrigir a razão no registro do G2: "fundidor sem insumo" no lugar de "STALLED" | baixo; só honestidade do registro, combina com B1 |

## 4. P3 — mineiro que chega e não alcança

Linha do tempo do c37e298f (os outros 501 blocos foram do outro mineiro):

- 15:55:48 e de novo às 16:17:39: *"reroutes to the stone beside at 95,53,-506 to open a footing"*.
- Ele chega **exatamente** ao destino 98,55,-508 e o relatório segue dizendo *"4,1 blocks away (out of
  reach … walking to 98,55,-508)"*. Depois de 300 tiques parado, desiste, e na segunda vez encalha.
- Δ = (3, 2, 2), distância 4,12 > `MinerReach.REACH = 4`. O ponto de trabalho que
  `MiningTarget.judge` devolve (`MinerHands.reroutedBeside`, :374-386) **não passa no teste de alcance 3D**.
  É a mesma forma do defeito de 08-28 descrito no próprio `MinerReach` (folga de chegada contra alcance).
- O mesmo desvio foi repetido 22 minutos depois: não há marca para "este desvio já falhou".
- A boca da mina fica em y84, numa duna (`117,84,-503`), com o nível em y64 e a vila em y63. O mineiro sobe e
  desce a duna para trabalhar e se perde nela: dois teleportes "no path home at night", um de y87.

| | Alternativa | Risco |
|---|---|---|
| **C1** | O `judge` só aceita ponto de trabalho a ≤ `REACH` em 3D da pedra (ou o relatório de alcance usa a mesma régua que o `judge`); GameTest com o Δ (3,2,2) e mutação | baixo |
| C2 | Desvio recusado entra no `MineMarks.refuse` como qualquer alvo | baixo |
| C3 | Preferir a boca da mina na altura da vila quando há escolha (`Mine.entry()`) | médio; a boca é deduzida (memória `mineiro-boca-lenhador-faixas-m2`) |

Também achado: `MinerSteps.java:200-224` (log "waited N ticks for the sand") é código morto, porque
`settling>0` já retorna em :96-112. Limpar junto com C1.

## 5. P4 — pastor

- Procriou 39 vezes e abateu 26 animais; o `WorkTime` conta tudo isso como ócio.
- Mesmo assim faltou comida: ~200 *"no food"* (4 espécies × 1 por minuto, `ShepherdFlock.java:202`). O
  estoque público tinha `WHEAT=1`, porque o fazendeiro guarda a colheita no próprio baú
  (`FarmerChores.java:288-299`) e as refeições da vila tiram beterraba, cenoura e batata de qualquer baú
  (`VillageMeals.java:62-63`, :151-171), sem reserva para os animais.

| | Alternativa | Risco |
|---|---|---|
| **D1** | Contar `tend` (alimentar, procriar, abater) como trabalho no `WorkTime` | baixo; muda o número da Regra 50, mas o deixa honesto |
| D2 | Reservar 2 de comida de cria por espécie antes do `VillageMeals.feed` | médio; ADR-037 R1 (refeições); **decisão do autor** |
| D3 | Uma linha "no food" por rodada, não por espécie | baixo; só log |

## 6. P5 — fazendeiro (40% de espera)

- 39 colheitas e 13× *"still sweeping … nothing ripe"*. A espera é a planta crescendo mais o descanso de
  2 min depois de uma varredura vazia (`FieldRest.QUIET_TICKS`).
- Já existem alternativas: lavrar canteiro novo (`FarmerTill`) e farinha de osso (`FarmerCompost`). Armadilha:
  a composteira consome a mesma semente que o replantio exige (`FarmerCompost.java:51`,
  `FarmerChores.java:91`).
- **E1:** antes de mexer, medir quanto da espera é o `FieldRest`. Ajudar a obra ou o pastor é proibido pela
  Regra 59.

## 7. P6 e P7 — lenhador e sem ofício

- **Lenhador:** cortou 3 árvores numa hora, o que é esperado no deserto. O `OutskirtsPlanting` exige chão com
  tag DIRT (`OutskirtsPlanting.java:166`), então no deserto quase não planta.
  - F1: muda só onde há terra, com log dizendo que o deserto não tem onde plantar. Risco baixo.
  - F2: lenhador ajuda a obra ou cuida de cacto (`CactusPatch` existe). **Decisão do autor**: a Regra 59 lista
    como ajudantes só o carpinteiro e o pedreiro.
- **Sem ofício (7 de 16):** a cota é `(adultos/15)×8 + min(adultos%15, 8)` = 9 vagas para 16 adultos
  (`HiringQuota.java:78-105`, `ADULTS_PER_BATCH=15`). A Regra 51 deixa quem não tem ofício fora da conta.
  - G1: sem ofício vira ajudante de obra (A10 do estudo de 10-08). **Decisão do autor.**

## 8. Observabilidade (miúdos que enganaram a leitura)

- O carpinteiro "fabricou dirt": é o alvo nominal das ordens de estoque (`ItemStock.NOMINAL = DIRT`,
  `ItemStock.java:70`; `CraftsmanRequest.java:142`). O log mostra o alvo, não o item. Não achei a origem dos 18
  "cobblestone".
- O `action_report.py` mostra "1 pega / 1 solta" e o diário tem `n` = cota da tarefa. Ler o `n` como
  contagem me fez anunciar um laço que não existia.
- Os 92% e 93% de ócio de pastor e lenhador dependem da régua do `WorkTime` (D1).

## 9. Aplicado na 0.3.86 (escolha do autor, 2026-10-21: A1, C1, B1, D2)

| Item | Commit | O que mudou |
|---|---|---|
| C1 | `e38a5e47` | `MinerReach.isWithinReachStanding`: alcança também do centro do bloco em que pisa; o relatório usa a mesma conta |
| A1 | `91de43ca` | `RoadExtension.keepGrowing` publica as `PavingRefusals` e escreve *"stopped growing the road … — motivos"* |
| B1 | `e002ba46` | `FurnaceFeed`: sem o cru da fornalha, pedido ao pedreiro pela receita com material dos baús, sem tirar da obra aberta |
| D2 | `25db9392` | `ShepherdFlock.breedingFoodToKeep`: com pastor, a ceia deixa um par de cada comida do rebanho com par pronto (emenda na ADR-037 R1) |

Achado ao implementar o B1: o "cobblestone" que o carpinteiro "fabricava" (§8) é o alvo nominal do
pedido de peça de obra (`CraftsmanRequest.NOMINAL = COBBLESTONE`), assim como o "dirt" é o da ordem de
estoque (`ItemStock.NOMINAL`).

Bateria inteira (832 GameTests, 3 rodadas nesta branch): caíram só o `CraftingGameTest.theCycleOpens…`
(KF-006, conhecido) e o `MinerDirectPathGameTest.theMinerWalksStraight…` (KF-007: a navegação do jogo
não acha caminho; isolado 6/6). A bisseção em 22 baterias está na KF-007
(`docs/behavioral-tests/known-failures.md`): a queda aparece com o arranjo de 830+ testes, já sem o D2,
e nenhuma das quatro mudanças toca a navegação.

## 10. Jogo da 0.3.86 (21-10, 20:37–21:08, Spark `j7jTrVIaPQ`)

Versão confirmada no log (`villagecolony 0.3.86`). Sessão de 28 min, a metade da anterior. Relatório:
[`docs/testing/relatorios/deserto-2026-10-21-0386.md`](../testing/relatorios/deserto-2026-10-21-0386.md) (nota 3,7).

**Desempenho:** TPS 20, salvo os 3 minutos de carga (8,6). MSPT mediano 6–8 ms. O mod ocupa ~1% da
thread. As entidades subiram de 206 para 335 em 20:43–20:49 e voltaram a ~215: são as folhas das 20
árvores que o lenhador derrubou levando só as toras ("0 leaves"), apodrecendo e soltando itens, que
somem em 5 min. Sem efeito no MSPT.

**O que as mudanças fizeram:**

| Item | Resultado em jogo |
|---|---|
| A1 | **Funciona.** A rua da faixa anexada para em *"0 of 16 blocks — something above (sandstone)"*: a duna sobe 2+ blocos logo na entrada da faixa. Nas outras pontas: arenito tratado como "not natural ground" (9×), água (4×), duna (vários), tapete da vila (Regra 3) |
| C1 | Assinatura sumiu: 25 "fora de alcance", **0** parados no próprio destino (eram 2). Prova fraca: houve só 1 desvio |
| B1 | **Funciona.** Pedreiro fez 34 arenitos em 13 pedidos (moeu 127 areias de pedra); o fundidor fundiu 144 arenitos lisos (eram 32 em 57 min). Custo: o fundidor solta e repega a fornalha a cada ~30 s (60 "leaves the furnace") |
| D2 | **Não exercido:** nenhuma ceia na sessão ("shared supper" 0) |

**Achados novos:**

1. **Mineiros 71% encalhados** (eram 15%), e **não vem do C1**: os dois param a caminho da boca da mina,
   no alto da duna (y84–87, vila em y63–80), e o resgate entra num vaivém: o `ClimbOut` não acha degrau →
   túnel; o túnel anda `TUNNEL_LENGTH` blocos por caminho aberto e devolve o modo a degrau sem log
   (`ClimbReshape.java:88`); a direção se inverte (x=110 ↔ 113), e o `BedTeleport.attempt` zera a conta
   porque ele "saiu" de 1,5 bloco (`BedTeleport.java:53`). 34d462b6 ficou 12 min assim (216 trocas) e
   c37e298f 17 min (200 trocas), até a cama pela noite. Correção candidata: contar a tentativa contra a
   posição de partida do episódio, com raio maior que o túnel, ou contra a melhor distância até a casa.
2. **A rua recusa arenito dentro da vila gerada**: `VillageRoad.isPavableGround` aceita ali só grama,
   terra e areia, por decisão (Regra 3). Hipótese, não medida: o chão firme da 0.3.84 (`DesertGround`,
   18.609 blocos de areia viraram arenito sob a superfície) expõe arenito onde a areia de cima sai, e a
   rua o lê como peça original. Para medir: marcar no log se o bloco recusado é um dos firmados.
3. **Pastor:** procriar até 12 e abater acima de 8 (`FlockHarvest`) é a "faixa de colheita" decidida em
   14-10; o comentário diz que "um não desfaz o outro", mas toda comida de cria vira carne (36 abates em
   28 min) e o "no food" segue (porco 22×, ovelha e vaca 26× cada) com trigo 1 e beterraba 2–3 no
   estoque. A reserva do D2 vai alimentar esse ciclo.
4. **Obra:** 0 lote aprovado em 28 min. A sessão anterior teve 2 em 57 min, então é o mesmo ritmo raro,
   não regressão.

## 11. Jogo da 0.3.87 (22-10, 01:48–02:20, Spark `CnNU79zqZ5`) e a 0.3.88

Versão confirmada (`villagecolony 0.3.87`). Relatório:
[`docs/testing/relatorios/deserto-2026-10-22-0387.md`](../testing/relatorios/deserto-2026-10-22-0387.md)
(nota 4,9; a anterior foi 3,7). TPS 20 o tempo todo, MSPT mediano 6,7 ms, mod em ~1,2% da thread.

| Profissão | Medido | Leitura |
|---|---|---|
| Mineiro | encalhe **7%** (era 71%); 34d462b6 cavou 551 blocos | o vaivém da cama acabou |
| Construtor | 71% ócio; 1 obra (casa média, 138 blocos) | lote raro, como antes |
| Fundidor | 72% ócio, 1% trabalho | estoque cheio (arenito liso 256, arenito 355): vila madura, não defeito |
| Fazendeiro | semeou trigo 5 e beterraba 5 (era 4 contra 27) | o trigo pela semente mais rara funciona; a roça produz pouco (16 colheitas) |
| Pastor | 83% ócio, 32 abates | sem comida (trigo 1, beterraba 1); D2 ainda sem ceia na sessão |
| Lenhador | 94% ócio | deserto quase sem árvore |
| Pedreiro | dentro da Regra 51 | — |

**Achados:**

1. **O lote na faixa (A5) nunca disparou.** Às 01:57 uma casa achou lote perto de outra rua, e o
   `RoadExtension.lotFound` limpou o `PENDING` que o `StripLot` usava. Mesmo assim os lotes aprovados
   subiram de 3.082 para 12.327 sem nenhum caminho, e nada dizia o motivo. Corrigido em `70a345a2`
   (`STRIP_AWAITS` próprio da faixa, e uma linha de telemetria no fim de cada volta sem lote).
2. **O mineiro construía caminho para fora da duna.** Um desvio teve "0 dug, 8 placed", a caminho da
   entrada da mina em 82,94,-434, que ficou inalcançável a sessão inteira. Duas regras do autor resolvem:
   - `62501caf`: nada de bloco ao céu aberto no desvio do mineiro, e ele desce antes de ir de lado (Regra 71);
   - `dd668591`: acesso cavado da vila até o nível, só cavando, quando a entrada não se alcança a pé.
3. **Caminhos em mancha e em escada diagonal.** A regra dos três lados do `BuilderPaving` fechava cantos,
   e o `WorkPath` trocava de eixo a cada passo. O corredor da Regra 70 (`aff29065`) troca os dois.

## 12. Jogo da 0.3.88 (22-10, 08:12–08:49, Spark `YFIroI1U1x`) e a 0.3.89

A versão foi confirmada no log (`villagecolony 0.3.88`). O relatório está em
[`docs/testing/relatorios/deserto-2026-10-22-0388.md`](../testing/relatorios/deserto-2026-10-22-0388.md)
(nota 4,0).

- **Desempenho:** TPS 20. Os picos de 110–126 ms a cada 5 min são o salvamento do jogo. `StripLot` e
  `Corridor` somam 0,09% da thread.
- **Alcance:** `VC_REACH` 28/28, então o `MineAccess` não disparou.

O save do mundo foi lido offline (nbtlib) para medir o terreno.

| Profissão | Medido | Causa | Feito na 0.3.89 |
|---|---|---|---|
| Fazendeiro | 0 de 11 tarefas; "could not reach the crop" 10× | parava a (2,1,2) da planta (distância² 9) e o alcance era estrito | alcance ≤ 3 com a borda (`FarmerWork.reaches`, `FarmerStand` até 9) |
| Mineiro 34d462b6 | 7 desistências e 12 min parado em 96,63,-481 | margem de 12 blocos entre o lago e o paredão: a pé não há saída, nadando bastam 3 blocos, e a água era proibida | escolha do autor: a proibição vale só debaixo da terra (`MinerCaution`, OCEAN_FLOOR) |
| Construtor e todos os produtores | ócio de 70% a 99%; 2 obras | **sem lote**: a faixa sul é um platô 23 blocos acima da rua (440 portas, 228 lotes que cabem, nenhum corredor) | escolha do autor: o corredor corta degraus, num vale de 3 (`Corridor` maxCut 12, `CorridorCut`; a melhor rampa reta tem 24 blocos e corte de 12) |
| Mineiro c37e298f | 971 blocos, 67 tetos escorados | — | — |

Pedidos do autor na mesma rodada, todos feitos na 0.3.89:

- **Regra 72** (um curral por espécie).
- **Rebanho 8/6.**
- **Lotação pelo tamanho:** 1 animal a cada 4 blocos de chão.
- **Curral novo com todos cheios** (`PenPlans`).
- **Caminho de areia** exclusivo do deserto (Regra 73).

## 13. Jogo da 0.3.90 (22-10, 12:52–13:20, Spark `LYMx4yHjKk`) e a 0.3.91

A versão foi confirmada no log. O relatório está em
[`docs/testing/relatorios/deserto-2026-10-22-0390.md`](../testing/relatorios/deserto-2026-10-22-0390.md)
(**nota 7,6**; a anterior foi 4,0). Não houve crash.

- **A faixa sul deu lote, pela primeira vez.** Às 12:58:44 abriu um curral no platô, a y93. O corredor tem
  32 blocos e cortou 486 blocos da encosta.
  - No save, a coluna do caminho é `sand_path`, desce 1 bloco por vez de y93 a y67 e passa num vale de 3.
- **O custo foi alto.** Das 12:54 às 12:58 houve 7 "Can't keep up", de 2,6 a 3,7 s, um a cada volta pela
  faixa, e o planejador chegou a 3.160 ms (`cost_ledger`).
  - A busca com corte relia a mesma coluna para cada combinação de direção, curvas e retos, e não tinha teto
    de tempo.
  - Corrigido na 0.3.91: a leitura da coluna é guardada uma vez por busca, a busca para em 50 ms, e o
    `StripLot` para entre um tamanho de casa e outro e retoma dali.
- **Profissões:**
  - construtor 48% de trabalho (era 7%), 441 blocos postos;
  - pedreiro 23% (era 2%);
  - lenhador 11%, com 19 árvores;
  - o fazendeiro não desistiu nenhuma vez por alcance.
- **Abertos:**
  - **Mineiros:** 36% do expediente andando. A cama fica na vila alta (y80–83), a mina a y55, depois do
    lago, e eles desistiram 9 vezes a 32–61 blocos do alvo.
  - **Armazém:** o `storage_majest` pede ardósia funda (deepslate), calcita, folha de acácia e madeira de
    pinheiro, que o deserto não tem (55 peças deixadas de lado).
  - **Espera alta:** fazendeiro 42% e fundidor 41%.

## 14. Ordem sugerida

1. **A1** (log da rua que para) + **C1** (alcance do `judge`) + **B1** (fundidor pede arenito ao pedreiro).
   Pequenos, sem decisão, um GameTest com mutação cada.
2. Jogar ≥30 min no mesmo mundo e comparar com esta sessão: obras por hora, ócio do construtor,
   `VC_UNREACHABLE`/G2 do arenito, encalhe do mineiro.
3. Com o motivo da rua em mãos: **A2** (rua da faixa com alternativas) ou **A3** (aterro maior).
4. **D1** (medição do pastor), para que o próximo `time_ledger` meça o que é ócio de verdade.
5. Decisões do autor: A5 (lote com rua na faixa), D2 (reserva de comida), F2/G1 (quem ajuda a obra).
