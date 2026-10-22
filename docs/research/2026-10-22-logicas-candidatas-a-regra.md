# Lógicas do mod que podem virar regra — 2026-10-22

**Pedido do autor:** levantar todas as lógicas do mod, comparar com `docs/RULES.md` e apontar as que poderiam
ser registradas como regra, para ajudar a controlar as ações do mod.

## Como foi feito

- No código de `src/main`, procurei as classes cujo javadoc registra uma decisão do autor ("decisão do
  autor", "pedido do autor", "regra do autor", "escolha do autor") e cruzei com a coluna "Onde" do
  `RULES.md`, que lista as classes de cada regra.
- Saíram 136 classes que nenhuma regra nomeia. Tirei as que só dividem arquivo ou só medem (relatórios,
  diários, leitura de baú, separações de 500 linhas). Ficaram **33 lógicas de comportamento**: o autor
  decidiu cada uma, elas estão no código e nenhuma tem número de regra.
- **Cruzamento ao contrário:**
  - O código cita a Regra 52, que faltava na tabela. Era a uma roça por fazendeiro, substituída pela 67;
    a posse da roça continua. Agora está em "Regras revogadas".
  - A emenda da Regra 43 ainda dizia "≤ 5 animais", que a 72 trocou pela lotação pelo tamanho. Corrigido.

**Registradas como Regras 75 a 107**, escolha do autor de 2026-10-22, na ordem desta lista (A1 = 75 … G1 =
107). Cada linha traz a classe e a data da decisão. O enunciado resume o javadoc, e o comportamento já está no
código; registrar só dá número, para que logs, testes e comentários possam citá-la.

## A. Movimento e encalhe

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| A1 | **Movimentação natural:** só o mineiro remodela o terreno para subir (degrau, túnel); ninguém põe bloco debaixo dos pés, e não há escada fixa | `ClimbPolicy` | 10-15, 10-21 |
| A2 | **O encalhado nunca desiste:** larga o ofício e sobe até a superfície; a escada da fuga é tampada depois que ele sai | `ClimbOut`, `StrandedEscape`, `EscapeBackfill` | E47 10-01, N10 09-24 |
| A3 | **Cama como último recurso:** preso 5 vezes no mesmo lugar, ou sem caminho para casa à noite, vai para a cama | `BedTeleport`, `NightHome` | ADR-036 item 15 |
| A4 | **Caminhada longa por pernas:** trajeto de 50 a 130 blocos vira etapas na superfície | `WalkLegs` | A7 10-08 |

## B. Mina

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| B1 | **A pedra é emprestada:** o alvo do mineiro vale por um prazo curto, e só o progresso renova | `MineLease` | E44 09-10 |
| B2 | **Só vira alvo o bloco com posição de trabalho válida e alcançável** | `MiningTarget` | E1 10-08 |
| B3 | **Ramal com dono:** cada ramal é reservado a um mineiro, com o estado dito pelo nome | `MineBranch`, `MineArm` | E2 10-08, 09-04 |
| B4 | **O mineiro memoriza a entrada da mina** e chega por ela, por etapas, até o nível de agora | `MineEntrance`, `MineApproachPath` | 10-08, 10-14 |
| B5 | **Espera a areia assentar** antes de voltar a cavar | `MineSettling` | 09-19 |
| B6 | **Tapa e escora com o bloco do bioma** (arenito no deserto) | `MineFill` | 10-20 |
| B7 | **O ferro descansa onde a mina não o tem:** o piso de ferro cru não força busca sem veio | `IronRest` | 10-21 |

## C. Obra e lote

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| C1 | **A peça que o construtor não alcança surge no lugar** | `UnreachablePieces` | 10-20 |
| C2 | **Peça equivalente:** sem a exata e sem tag do jogo, vale a equivalente da tabela | `EquivalentPieces` | N3 09-24 |
| C3 | **Terracota, feno e argila aparecem quando a obra pede** | `AppearingPieces` | 10-21 |
| C4 | **Degrau na frente da porta** do lote que fica um acima da rua | `DoorStep` | 09-30 |
| C5 | **Terraplanagem do lote:** corta até 5 e aterra até 2 do chão do bioma, com um corredor de 2 em volta da casa | `LotTerraform` | 10-20, 10-21 |
| C6 | **Casa fundadora:** primeiro o lote plano exato, depois o aplainável; a vegetação do lote sai | `FoundationLotSearch`, `LotGrowth` | 10-20 (G1) |
| C7 | **Deserto:** a areia da planta vira arenito (emenda à Regra 27), e o chão da vila de areia não desaba | `DesertSand`, `DesertGround` | 09-19, 10-21 |
| C8 | **Obra abandonada tem retomada limitada** antes de a vila aceitar obra nova | `BuildingRepairPlanner` | — |
| C9 | **Faixa de construção:** o lote novo nasce ao lado da rua; muda, flor alta ou arbusto crescendo ali tiram o lote da faixa | `ConstructionZone` | 10-11 |

## D. Produção e estoque

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| D1 | **Todo ofício trabalha independente do pedido** (produção contínua até os tetos) | `StandingWork` | 09-26 |
| D2 | **Todo trabalhador de ofício tem um baú nascido para ele** (emenda à Regra 8) | `ChestSpawner` | 09-26 |
| D3 | **Baú cheio chama os outros ofícios**, e os 10 últimos espaços dos baús da colônia ficam sempre livres | `ChestRelief`, `ProfessionChestOverflow` | 09-19, ADR-036 item 9 |
| D4 | **O que a vila provou não ter no alcance não é mais buscado**, e o raio varrido vazio espera | `KnownAbsent`, `EmptySweeps` | 10-20, F-1 10-02 |
| D5 | **O alcance da coleta cresce com a vila** | `GatheringReach` | N11 09-24 |
| D6 | **Autossuficiência:** britador do pedreiro (pedra → areia), janela sem vidro, compostor do fazendeiro, pedreiro faz o insumo da fornalha, fundidor sem cru solta a fornalha, areia de beira d'água, argila | `Crusher`, `WindowEquivalent`, `FarmerCompost`, `FurnaceFeed`, `SmelterGathers`, `SandNearWater`, `ClayPatch` | 09-19 a 10-21 |
| D7 | **Receita de tingir peça pronta é ignorada** | `RecolorRecipes` | 09-26 |

## E. Animais, plantas e jogador

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| E1 | **Toda vila nasce com 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos** | `VillageFarmAnimals` | 10-20 |
| E2 | **Grama ao pé do poste da ovelha** amarrada, e **o golem sai do curral** pela porteira | `PostGrass`, `PenGolems` | 10-20, 10-02 |
| E3 | **O que o jogador tirou não volta:** árvore, muda ou flor quebrada pelo jogador não é replantada naquela coluna | `PlayerClearedPlants` | 10-11 |

## F. Colônia

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| F1 | **Duas colônias da mesma vila viram uma**, e a identidade é a vila gerada, não o centro | `ColonyMergeTrigger`, `ColonyIdentity` | ADR-007, E51 09-30 |
| F2 | **A vila é uma caixa** medida pela vila gerada, e não um raio | `VillageBounds`, `VillageMeasure` | ADR-003 Emenda 6, 09-30 |

## G. O que o jogador vê

| # | Lógica | Onde | Decisão |
|---|---|---|---|
| G1 | **Marcas no mundo:** contorno de chamas no lote, área da vila, dono de cada baú, nome do aldeão no painel do ofício | `SiteMarker`, `VillageMarker`, `ChestMarker`, `WorkerNameplateMixin` | 08-12 a 10-08 |

## O que fica fora

- **Divisões técnicas:** `MinerSteps`, `MineCuts`, `RoadPaving` e afins, separados por passarem de 500 linhas.
- **Medição:** `CraftingReport`, `ColonyStateLog`, `TaskTransitions`, `SupplyGapLedger`, `BiomeCheck`.
- **Modelo e infraestrutura:** `Worker`, `Capability`, `ServerLifecycleHandler`, `StructureBlueprintReader`.

Elas executam regras, mas não são decisões de comportamento por si.
