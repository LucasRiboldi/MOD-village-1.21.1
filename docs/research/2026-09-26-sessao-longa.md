# Sessão longa de 2026-09-26 (03:22–08:14) — auditoria

**JAR:** `9EB559D1…9423` (conferido no `mods`). **Spark:** `ZXKtle85Nw`.
**Colônia:** `ede8c122` (-437, 64, 3529), a vila de planície, a sessão inteira
como vila foco. **Queixa do autor:** muitas horas de jogo e pouquíssima coisa
construída; um aldeão parado por não conseguir sair da mina (caminho com dois
blocos de altura).

Ferramentas: `session_audit.py` e `mine_bfs.py` (scratchpad da sessão), leitura
do save (`r.-1.6.mca`, `r.-1.7.mca`) e do NBT das casas do jogo.

## 1. Números da sessão (291 min)

| Medida | Valor |
|---|---|
| Obras terminadas | 1 casa do pastor (269 → 0 blocos, **03:23 → 05:20, ~2 h**) + 2 fechamentos de reparo com 0 blocos |
| Obras planejadas | 1 (`plains_medium_house_1`, 05:22) — **largada 1 min depois**, 0 blocos postos |
| Tempo sem nenhuma obra | **05:23 → 08:14 (2h51)** |
| Leituras do construtor | 137 BUILDING, 100 WAITING_RESOURCES (42% esperando) |
| Peças fabricadas do nada pela ADR-022 | **254** (87 tábuas, 60 escadas, 22 toras, 22 grama, 17 cercas, 15 terra…) |
| Lenhador com tarefa | **0 min** |
| Mineiro com tarefa | só a partir de **08:10** (4h48 parado) |
| Fundidor tentando o que não existe | **582** ("sem arenito/areia para fundir" em vila de planície) |
| Fazendeiro "varredura sem resposta" | 142 |
| Ciclos acima de um tique | 65 (planejador médio 69 ms, máx 214 ms) |
| TPS | 20 em todas as janelas; mod 0,16% da thread |

## 2. P0 — trabalhador sem baú nunca recebe tarefa

`[FATO]` Ao carregar, `Registered 5 storages in colony ede8c122 (5 total)`:
pastor, carpinteiro, construtor, fazendeiro e fundidor. **Lenhador, mineiro e
pedreiro ficaram sem baú.** `WorkAssignment.assign` exige `hasStorage`, então
as tarefas de madeira e pedra ficaram abertas (7–11 abertas, 1 atribuída por
ciclo) sem ninguém elegível.

`[FATO]` Às 08:10:47 o mineiro reivindicou baú (`claimed the chest at -449, 70,
3586`) e recebeu a tarefa de carvão no mesmo ciclo. O lenhador não ganhou baú
na sessão.

`[FATO]` O relatório mentia sobre a causa: `LumberjackReport` conta só tarefa
reservada ou em execução como "em curso", e dizia `no task open for it` quando
havia tarefa aberta e o lenhador sem baú.

`[INFERÊNCIA]` O baú é associado pela cama (`Storage claimed by X: bed … chest …
1 block apart`). Aldeão que troca de cama, ou dorme numa cama sem baú ao lado,
fica sem baú — e, desde a regra (b), o baú de cama só nasce na adoção da vila.

**Consequência em cadeia:** sem lenhador, a obra esperava madeira; a regra da
rota atrasada (ADR-022) fabricou 254 peças; a obra andou devagar (2 h para uma
casa) porque cada peça esperava os 10 ciclos da carência.

**Correção proposta:** trabalhador sem baú ganha um — pela regra (b) ao lado da
cama dele, ou um baú livre da colônia —, na hora e não só na adoção; e o
relatório passa a dizer "sem baú" em vez de "sem tarefa".

## 3. P0 — escolha de lote

`[FATO]` Balanço de `08:14:02`: **714.589 colunas recusadas, 1.151 aprovadas
(0,16%)**.

| Motivo | Recusas | % |
|---|---|---|
| chão dentro de área reservada de rua | 489.812 | 68,5 |
| não é solo natural | 83.668 | 11,7 |
| fora do nível da rua | 76.112 | 10,7 |
| sem chão na janela vertical | 41.581 | 5,8 |
| volume ocupado | 15.856 | 2,2 |
| peça de vila / do jogador | 7.417 | 1,0 |
| cama | 143 | 0,02 |

`[FATO]` A única obra planejada foi largada **um minuto depois**: o lote ficava
a 72 blocos do centro (raio 64) e o guarda de alcance disse "the nearest road
is not indexed yet". O planejador escolheu um lote que o guarda do mesmo
sistema recusa — duas réguas para a mesma pergunta. E o lote **ficou
reservado** com 0 blocos postos.

`[FATO]` Depois disso: 16× "índice de ruas descartado" (817 e 984 colunas por
rodada sem lote), 78× "nenhuma ponta de rua pavimentável".

**Correção proposta:** o planejador só aceita lote que o guarda de alcance
aceitaria (mesma régua); obra largada com 0 blocos libera o lote; e revisar a
reserva de rua, que sozinha recusa dois terços do que se olha.

## 4. P1 — trabalho repetido sem efeito

- **Fundidor** (582×): a meta de "tudo o que a fornalha faz" tem piso de 16 para
  cada item, inclusive arenito liso numa vila de planície; ele procura arenito e
  areia a cada ciclo. Proposta: meta só para o que tem matéria-prima com rota
  no bioma.
- **Fazendeiro** (142×): a varredura não termina dentro do orçamento e ele
  alterna "esperando varredura" / "recuperado".
- **ADR-022** (254 peças): efeito do P0 do baú; com o lenhador trabalhando, deve
  cair muito.

## 5. O aldeão preso na mina

`[FATO]` Busca de caminho no save ao fechar o jogo, com a regra do aldeão (sobe
1, cai até 3, e para subir precisa do bloco acima da cabeça livre): das 1.414
células andáveis da área da mina, **nenhuma da mina cavada (com tocha) deixa de
ter volta** à superfície. As 340 células sem volta são ravina e caverna
naturais (grama no fundo, sem tocha).

`[INFERÊNCIA]` O aldeão que o autor viu era, com boa chance, o mineiro **sem
tarefa** (sem baú, P0): trabalhador ocioso não passa por guarda de travamento
nem vira "encalhado", então nada o tira de onde parou e o log não diz nada. O
save final não reproduz o degrau de dois blocos.

`[VALIDAÇÃO NECESSÁRIA]` Coordenada (F3) ou captura do próximo caso.

## 6. O que a sessão não exercitou

- A casa na altura da rua: nenhuma casa nova foi construída (só a média,
  largada com 0 blocos).
- Lenhador sem árvore, lote de mudas: o lenhador nunca trabalhou.

## 7. Pedido do autor depois desta auditoria (26-09)

*"Todas profissões têm que trabalhar independente do pedido da obra, eles só dão
prioridade ao bloco solicitado pela obra. Peças fabricadas do nada pela regra de
peça sem rota devem ser somente para blocos de manufatura. Corrigir lenhador,
mineiro; rever como corrigir as colunas de lote recusadas; todos aldeões de
profissão devem ter um baú nascido destinado a cada um deles; revisar o projeto
para que as profissões sejam mais ativas."*

| # | Item | Estado | Onde / prova |
|---|---|---|---|
| 1 | Baú para todo aldeão de profissão | **feito** | `ChestSpawner` (ao lado da cama pela regra b; sem cama, perto do centro; 2 por varredura; baú quebrado é refeito); `ChestSpawnerGameTest` (3) |
| 2 | Vínculo aldeão→baú salvo com o mundo | **feito** | `WorkMarksSavedData.workerChests`; `WorkMarksSavedDataTest` |
| 3 | Peça pronta só para manufatura | **feito** | `BiomeConstructionSupply.isNatural` recusa recurso NATURAL, terreno, pedra de base, tronco, folha, muda e flor; `NaturalSupplyGameTest` |
| 4 | Lenhador e mineiro | **feito** pela causa: tinham tarefa aberta e nenhum baú (itens 1–2) |
| 5 | Profissões trabalhando sem depender da obra | **feito** para pastor (lã enquanto couber no baú dele) e fazendeiro (colheita além do piso da despensa); lenhador e mineiro já seguiam a regra | `StandingWork` (core), `StandingWorkTest` (4); prioridade da obra mantida (`CONSTRUCTION_MATERIAL`) |
| 6 | Fundidor sem trabalho inútil | **feito**: produto de fornalha sem matéria-prima no bioma sai das metas, salvo pedido da obra | `FurnaceReach`, `FurnaceReachGameTest` |
| 7 | Guarda de alcance largando obra ao lado da rua | **feito**: sem índice de ruas, a rua calçada colada no lote responde | `VillageRoad.besidePaving` — **sem teste dedicado ainda** |
| 8 | Obra com só chão enterrado não cedia lugar | **feito** (defeito introduzido pela camada da rua) | `ConstructionProject.hasBuiltAnything`, `ConstructionProjectBuiltTest` (3) |
| 9 | Obra largada sem nenhum bloco liberar o lote | **não feito — decisão do autor**: três GameTests antigos garantem que obra largada mantém o lote contra sobreposição (`thebuildthatplacesnothingletsgooftheslot`) | — |
| 10 | Colunas de lote recusadas (68,5% por rua reservada) | **analisado, não corrigido** | ver abaixo |
| 11 | Pedreiro e carpinteiro contínuos | **não feito**: dependem de insumo (pedra, tora) e hoje trabalham para a obra e para a meta de tábua | — |
| 12 | Aldeão ocioso preso abaixo do chão | **não feito** | resgatar como encalhado |

### 7.1 As colunas de lote recusadas — o que a análise mostra

`[FATO]` A reserva de rua (`RoadIndex.isReservedAgainstLots`) só recusa a coluna
que é calçamento **e** pertence à rede de ruas da colônia. Os 489.812 são pegadas
de casa candidatas atravessando ruas de verdade.

`[INFERÊNCIA]` O número alto é sintoma, não causa: a vila parou de crescer rua
(78× "nenhuma ponta de rua pavimentável", 16× índice descartado), e o lote só
nasce encostado em rua. Sem rua nova, as pegadas possíveis se esgotam e toda
volta da varredura reprova as mesmas colunas.

Próximos passos propostos, em ordem:
1. Medir por que as pontas de rua recusam calçamento (o log só diz "up to 12 were
   tried").
2. Aceitar lote a um ou dois blocos da rua, e não só encostado.
3. Contar recusa por lote, e não por coluna repetida a cada volta, para o número
   dizer quantos lugares de fato foram negados.
