# Regras e testes

Gerado por `python scripts/rule_coverage.py`; não editar à mão. Toda suite do Sentinela Local roda todos os testes das classes
abaixo: a pergunta aqui é se existe teste para a regra, não se ele rodou. Citar não prova que o teste mede a regra, e usar a
classe prova menos ainda.

- **Cita a regra:** 151
- **Só usa a classe indicada:** 94
- **Sem nenhum teste:** 7
- **Fora de vigor:** 5

| # | regra | grau | testes |
| --- | --- | --- | --- |
| 1 | Colher até os baús encherem | cita | ChestPlacerGameTest, ColonyCycleGameTest, ColonyCycleTest |
| 2 | Colher no tempo de um jogador com ferro | cita | LumberjackGameTest, ProfessionRegistryTest |
| 3 | Nunca destruir construções da vila original, da colônia ou do jogador | cita | ArchitectureRulesTest, BuildSiteGameTest, BuilderGameTest |
| 4 | Dois trabalhadores por profissão | fora de vigor |  |
| 5 | Fabricar até metade do armazém | cita | ColonyGoalsTest, CraftingGameTest |
| 6 | Estrada primeiro, casa ligada a ela | cita | BuildSiteGameTest |
| 7 | O lenhador planta onde cortou | cita | FarmerGameTest, ShepherdGameTest |
| 8 | Um baú ao lado de cada cama — em toda casa (da vila e construída), encostado na parede, n… | cita | ChestPlacerGameTest |
| 9 | Subir e descer para alcançar, e poder voltar | cita | LumberjackGameTest |
| 10 | O construtor fabrica o que a expansão pede | cita | BuilderGameTest, HouseBillOfMaterialsGameTest, SmelterGameTest |
| 11 | Uma de cada profissão em cada vila | cita | ProfessionFloorTest |
| 12 | O centro fica em bloco que existe | classe | BedWindowGameTest, ClusterRejectionTest, ColonyAbandonmentTest |
| 13 | A obra é uma que a colônia consiga fazer | cita | BuildSiteGameTest, ConstructionProjectTest, ConstructionResumeGameTest |
| 14 | O construtor alcança o alto da obra — desde a ADR-037 B1, de qualquer lugar da zona da ob… | cita | BuilderGameTest |
| 15 | A estrada cresce com a vila: a ponta pede as colunas ao grafo, e o aldeão calça (Regra 21… | cita | BuildSiteGameTest, RoadExtensionGameTest |
| 16 | Cada casa com espaço em volta | classe | BigHouseFoundationGameTest, BuildSiteGameTest, ColonyRoadsTest |
| 17 | A casa com uma lateral na estrada | cita | BlueprintDoorSideTest, BlueprintRotationTest, BuildSiteGameTest |
| 18 | O dia inteiro é expediente | cita | BuildProgressGameTest, WorkClockTest, WorkHoursGameTest |
| 19 | O lote fica no nível da estrada | cita | BuildSiteGameTest, LotTerraformGameTest |
| 20 | Cada vila constrói no estilo do bioma | cita | VillageBiomesGameTest, VillagePaletteTest |
| 21 | Toda casa nasce com cama, baú e lampião — resolvida em 10-22 (C-02, autor): cama, porta e… | cita | BuilderGameTest, ConstructionSaveTest, HouseBillOfMaterialsGameTest |
| 22 | O lote é livre no volume | cita | BuildSiteGameTest, VolumeSampleGameTest |
| 23 | O que já foi analisado se analisa de novo | cita | RingSweepResumeTest, TreeMarksTest |
| 24 | A vila de planície levanta a casa do jogo | cita | ConstructionProjectTest, ConstructionResumeGameTest, MinerGameTest |
| 25 | ~~A maior planta que couber no lote~~ | fora de vigor |  |
| 26 | Cadeia de produção, e paleta por bioma | classe | ActionJournalGameTest, AdvanceStockTest, BiomePiecesGameTest |
| 27 | Só o catálogo do jogo, e o construtor aguarda | cita | BuilderGameTest, CraftingWorkFamilyTest, DesertSandTest |
| 28 | ~~Barreira de teste: casa pequena, mobília dispensada~~ | fora de vigor |  |
| 29 | A mina em escada, duas salas, galeria sem fim | cita | MinerGameTest |
| 30 | O mineiro recolhe tudo, e a boca tem endereço | cita | MinerGameTest, SmelterGameTest |
| 31 | O fazendeiro planta o que tem e colhe o que está pronto | classe | ColonyFixture, CropPatchKnownFieldGameTest, CropRegistryGameTest |
| 32 | ~~Móveis e cama entram depois da casa pronta~~ — substituída em 10-22 pela 21 (C-02, auto… | fora de vigor |  |
| 33 | Pedido de commit+push também atualiza o JAR local | nenhum |  |
| 34 | A lava nunca surge automática | classe | BuilderGameTest, ChainRootsGameTest, ConstructionSupplyAuditGameTest |
| 35 | Carpinteiro titular na BigHouseMOD, cama e baú à direita da porta, corredor livre até a e… | cita | BigHouseModBlueprintGameTest |
| 36 | O viveiro (terra enraizada + rebento) é do lenhador | classe | DesertFixesGameTest, LumberjackNurseryTargetTest, TreeNurseryGameTest |
| 37 | Cada aldeão usa a ferramenta de ferro apropriada para a ação | classe | BetterToolGameTest, LumberjackGameTest, ProfessionReviewGameTest |
| 38 | Contratar primeiro a profissão de que a demanda depende, depois a lista | classe | BigHouseModBlueprintGameTest, ColonyCycleTest, ColonyDetectionGameTest |
| 39 | Comida é feita pelo fazendeiro (pão do trigo acima de 32) | classe | ProfessionReviewGameTest, VillageMealsGameTest |
| 40 | Piso de fundido só com cadeia do cru | cita | BuilderMaterialsGameTest |
| 41 | Corante, linha, pó de osso e drops de inimigo e animal aparecem automáticos | classe | BuilderGameTest, BuilderMaterialsGameTest, ConstructionStallGameTest |
| 42 | Mineiro para de guardar um tipo aos 256 no baú (fora o pedido) | classe | MinerHaulCapGameTest, MinerOverflowGameTest, PublicChestsGameTest |
| 43 | O pastor faz o rebanho procriar | classe | FlockSurplusGameTest, FlockWheatGameTest, PenAccessGameTest |
| 44 | Ofício do mod exclui ofício Vanilla | cita | VillagerRulesGameTest |
| 45 | Os baús da colônia são todos os da vila, e só eles: com a vila medida, todo baú livre den… | cita | GroundPickupGameTest |
| 46 | A fome não influencia o trabalho: a comida decide só a procriação (Vanilla) | classe | VillageMealsGameTest |
| 47 | O aldeão não envelhece nem morre de velhice | nenhum |  |
| 48 | Todo aldeão da colônia recolhe qualquer item caído a até 5 blocos ao passar e o guarda em… | cita | GroundPickupGameTest, MinerOverflowGameTest |
| 49 | A ordem das obras (consolida 55 e 60, 1A de 13-10): faltando cama, casa; senão a oficina… | cita | ConstructionOrderTest, ConstructionPriorityTest |
| 50 | Toda verificação mede o tempo dos aldeões — trabalhando, andando, bloqueado, ocioso, enca… | cita | WorkTimeTest |
| 51 | Peça que falta aparece na 4ª tentativa sem sucesso, no baú da profissão que tentou (colet… | cita | BuilderMaterialsGameTest, test_state_report, test_time_ledger |
| 53 | A boca da mina só nasce na faixa da borda da caixa da vila (até 8 blocos da linha, dentro… | classe | MineEdgeGameTest, MineFluidGameTest, MineLeaseGameTest |
| 54 | Mina em rede (ADR-042): passada a escadaria e o salão, cada nível é uma grade de nós a ca… | classe | MineAccessGameTest, MineReturnGameTest, MineReturnTest |
| 55 | Roça grande primeiro; fundidor ocioso adianta o cru; lenhador ocioso planta mudas até 10… | cita | GrowingPiecesGameTest |
| 56 | Expansão do terreno: a caixa cresce só com rua e obra, nunca com faixa vazia anexada (0.3… | cita | PublicChestPlacementGameTest, VillageExpansionGameTest, VillageExpansionTest |
| 57 | O pastor põe no máximo um poste por casa e um por cercado (nenhum poste novo a até 6 bloc… | classe | EntrancesAtTheDoorGameTest, PenAccessGameTest, ShepherdPensGameTest |
| 58 | Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto por tipo: o exc… | classe | MinerHaulCapGameTest, MinerOverflowGameTest, PublicChestPlacementGameTest |
| 59 | Ajudam a obra só carpinteiro e pedreiro (fazendeiro e pastor não); o carpinteiro guarda u… | classe | BuilderPavingGameTest, ConstructionZoneGameTest, GraphOnlyPathsGameTest |
| 60 | O tipo de obra que passa uma volta inteira da varredura sem lote fica 30 min fora da vez;… | classe | BigHouseFoundationGameTest, ColonyModelsGameTest, ConstructionOrderTest |
| 61 | A colônia só é dada como abandonada sem vila à vista do centro nem dentro da caixa, e com… | classe | ColonyAbandonmentTest, test_graphify_relabel |
| 62 | A vila trabalha com o jogador dentro da caixa ou a até 48 blocos dela (VillageFocus.PERIM… | classe | AbandonedColonyGameTest, ColonyBoundsTest, DrawnFromTest |
| 63 | Socorro contra parada com prazo único: o ofício que anda sem chegar ao alvo há 1 minuto d… | classe | WorkRescueTest |
| 64 | As ruas da vila são medidas como rede de caminhos conectados a partir do centro (de bloco… | classe | GraphOnlyPathsGameTest, RoadNetworkGameTest |
| 65 | Bancadas exclusivas do mod: três blocos próprios (carpenter_bench, mason_cutter, blacksmi… | classe | BenchApproachTest, BenchPassivesGameTest, BenchUseGameTest |
| 66 | Cercado no nível da rua: estrutura cuja camada y=0 é só solo natural (grama, terra, areia… | classe | StreetLayerTest |
| 67 | Plantação por população: uma roça a cada 15 adultos, no mínimo uma desde a fundação; ao p… | cita | ConstructionPriorityTest, FarmOwnersTest, FarmPlanGameTest |
| 68 | O pastor aproveita o rebanho (manutenção do ofício, não toca a obra): somente um pastor v… | classe | CarpetFamilyGameTest, ChainRootsGameTest, CraftingLookupGameTest |
| 69 | A horta da casa surge sem gastar recurso: os blocos de chão da planta que não têm item ne… | cita | GardenPiecesGameTest |
| 70 | Forma do caminho: um bloco de largura, reto a partir da porta, virando só em ângulo reto.… | fora de vigor |  |
| 71 | O mineiro não constrói para fora da montanha: o desvio dele não põe bloco ao céu aberto (… | cita | DetourCostsTest, MinerCautionGameTest |
| 72 | Um curral por espécie: animais iguais ficam no mesmo curral; com mais de um curral na vil… | cita | PenModelGameTest, ShepherdPensGameTest |
| 73 | Caminho de areia: a vila do deserto tem caminho próprio — cópia do caminho de terra do Va… | classe | BenchPassivesGameTest, BenchUseGameTest, BiomeSiteGameTest |
| 74 | A cama mais perto do trabalho: cada aldeão dorme na cama da vila mais perto de onde traba… | cita | BedChoiceGameTest, BedPlanTest, WalksTest |
| 75 | Movimentação natural: só o mineiro remodela o terreno para subir (degrau, túnel); ninguém… | classe | ClimbReshapeTest |
| 76 | O encalhado nunca desiste: larga o ofício e sobe até a superfície; a escada da fuga é tam… | classe | ClimbOutGameTest, ClimbReshapeTest, FencedInGameTest |
| 77 | Cama como último recurso: preso 5 vezes no mesmo lugar, ou sem caminho para casa à noite,… | classe | BedTeleportGameTest, NightHomeRangeGameTest |
| 78 | Caminhada longa por etapas: um trajeto de 50 a 130 blocos vira etapas na superfície | classe | WalkLegsGameTest |
| 79 | A pedra é emprestada: o alvo do mineiro vale por um prazo curto, e só o progresso renova | classe | MineDescentLeaseGameTest, MineLeaseGameTest, MineLeaseTest |
| 80 | Só vira alvo o bloco com posição de trabalho válida e alcançável | classe | MinerPicksTest, MinerRerouteTest, MiningTargetGameTest |
| 81 | Ramal com dono: cada ramal da mina é reservado a um mineiro, com o estado dito pelo nome | classe | MineBranchTest, MineRampGameTest, MineRegistryTest |
| 82 | O mineiro memoriza a entrada da mina e chega por ela, por etapas, até o nível de agora | classe | IdleFixesGameTest, MineAccessGameTest, MineApproachProgressTest |
| 83 | Espera a areia assentar antes de voltar a cavar | classe | MineGravityGameTest, MineSettlingGameTest |
| 84 | Tapa e escora com o bloco do bioma (arenito no deserto) | classe | BiomeCoverageGameTest, MineGravityGameTest |
| 85 | O ferro descansa onde a mina não o tem: o piso de ferro cru não força busca sem veio | classe | DesertGroundGameTest |
| 86 | A peça que o construtor não alcança surge no lugar | classe | UnreachablePiecesGameTest |
| 87 | Peça equivalente: sem a exata e sem tag do jogo, vale a equivalente da tabela | classe | EquivalentPiecesTest |
| 88 | Terracota, feno, argila, gelo e neve aparecem quando a obra pede (gelo, gelo compactado e… | classe | BuilderGameTest, DesertFixesGameTest, GardenPiecesGameTest |
| 89 | Degrau na frente da porta do lote que fica um bloco acima da rua | classe | FoundationPreparationGameTest |
| 90 | Terraplanagem do lote: no legado, corta até 5 e aterra até 2 blocos do chão do bioma, com… | classe | BuildSiteGameTest, DuneCutGameTest, LotTerraformGameTest |
| 91 | Casa fundadora: primeiro o lote plano exato, depois o aplainável; a vegetação do lote sai | classe | BiomeCoverageGameTest, BiomeSiteGameTest, ColonyDetectionGameTest |
| 92 | Deserto: a areia da planta vira arenito (emenda à Regra 27), e o chão da vila de areia nã… | classe | DesertGroundGameTest, DesertSandTest, PlanAffordabilityTest |
| 93 | Obra abandonada tem retomada limitada antes de a vila aceitar obra nova | classe | FarmPlanGameTest, FoundationRepairGameTest |
| 94 | Faixa de construção: o lote novo nasce ao lado da rua; muda, flor alta ou arbusto crescen… | cita | VillagerRulesGameTest |
| 95 | Todo ofício trabalha independente do pedido (produção contínua até os tetos) | classe | StandingWorkTest |
| 96 | Todo trabalhador de ofício tem um baú nascido para ele (emenda à Regra 8) | classe | ChestSpawnerGameTest |
| 97 | Baú cheio chama os outros ofícios, e os 10 últimos espaços dos baús da colônia ficam semp… | classe | IdleFixesGameTest, ProfessionChestOverflowGameTest, ProfessionChestSpreadGameTest |
| 98 | O que a vila provou não ter no alcance não é mais buscado, e o raio varrido vazio espera | classe | EmptySweepsTest, KnownAbsentTest, LocateFallbackGameTest |
| 99 | O alcance da coleta cresce com a vila | classe | GatheringReachTest |
| 100 | Autossuficiência: britador do pedreiro (pedra → areia), janela sem vidro, compostor do fa… | classe | BuilderMaterialsGameTest, CrusherGameTest, FarmerCompostGameTest |
| 101 | Receita de tingir peça pronta é ignorada | classe | RecolorRecipesTest |
| 102 | Toda vila nasce com 2 ovelhas, 2 galinhas, 2 vacas e 2 porcos | classe | VillageFarmAnimalsGameTest |
| 103 | Grama ao pé do poste da ovelha amarrada, e o golem sai do curral pela porteira | classe | DesertFixesGameTest, PostGrassGameTest |
| 104 | O que o jogador tirou não volta: árvore, muda ou flor quebrada pelo jogador não é replant… | classe | ConstructionZoneGameTest |
| 105 | Duas colônias da mesma vila viram uma, e a identidade é a vila gerada, não o centro | classe | ColonyIdentityTest, ColonyMergeTriggerGameTest |
| 106 | A vila é uma caixa medida pela vila gerada, e não um raio | classe | BedChoiceGameTest, BuilderPavingGameTest, CarpenterWoodGateGameTest |
| 107 | Marcas no mundo: contorno de chamas no lote, área da vila, dono de cada baú, nome do alde… | classe | BuildSiteGameTest, ChestMarkerGameTest, CraftingLookupGameTest |
| 108 | A Tocha das Almas cancela a obra: uma tocha das almas (também a de parede) posta pelo jog… | cita | LotLightDecorGameTest, MaeSitesGameTest |
| 109 | A casa fundadora BigHouseMOD: toda vila adotada a recebe uma única vez — cópia editada da… | classe | BedChoiceGameTest, BigHouseFoundationGameTest, ColonyDetectionGameTest |
| 110 | Nada que a colônia corta some (R-059 do estudo v2, autor 10-22): o que a terraplanagem do… | cita | CuttingsGameTest, LotTerraformGameTest |
| 111 | Suprimento excepcional só pela lista declarada (R-080 do estudo v2, autor 10-22): o que s… | cita | SupplyExceptionTest, VillageFarmAnimalsGameTest |
| 112 | Vocabulário único de bloqueio (R-052 do estudo v2, autor 10-22): toda soltura, desistênci… | cita | BlockCodeTest |
| 113 | A obra parada só em peças de lado não prende a vila (autor 10-22, playtest do deserto: o… | cita | BuildProgressGameTest, PieceRescueTest, StuckPiecesGameTest |
| 114 | Ninguém é posto onde o corpo não cabe, e ninguém fica preso para sempre (autor 10-27, jog… | cita | BedTeleportGameTest, PenLiftGameTest |
| 115 | O armazém do mod sobe do chão e é feito do material do bioma (autor 10-27: o armazém nasc… | cita | ColonyModelsGameTest, ConstructionOrderTest, StreetLevelGameTest |
| 116 | A argila aparece na terceira busca vazia, em todo bioma (autor 10-27, jogo da 0.3.93: na… | cita | LocateFallbackGameTest |
| 117 | O pastor recebe ração quando o fazendeiro não produziu (autor 10-27, jogo da 0.3.93: "the… | cita | ShepherdWoolDeficitGameTest |
| 118 | No deserto o bosque do lenhador é o dobro e se espalha pelas bordas (autor 10-27, jogo da… | classe | VillageGroveGameTest |
| 119 | A mina desce em caracol, abre salão e desce de novo pelos quatro cantos, e o caminho que… | cita | BlueprintDoorSideTest, MineEntranceGameTest, MineSecondLevelTest |
| 120 | A tarefa do lenhador fecha concluída quando o alcance esvazia (autor 10-27, jogo da 0.3.9… | cita | TreeFellingGameTest |
| 121 | A roça conhecida responde primeiro, sempre (autor 10-27, jogo da 0.3.93: 622 "still sweep… | cita | CropRegistryGameTest |
| 122 | O curral é do pastor (autor 10-27, jogo da 0.3.93: 217 entradas em curral na planície e o… | cita | PenFloorsGameTest |
| 123 | A rede de caminhos da vila: toda porta se liga à rede pelo grafo, com o trajeto mais curt… | cita | PathNetworkGameTest, VillagePathPlannerTest |
| 124 | Construção nova nasce a no mínimo 3 blocos de qualquer construção (autor 10-27, D1): o lo… | cita | PathNetworkGameTest |
| 125 | Quem assenta o caminho (autor 10-27, D6): o pedreiro, o carpinteiro e o fundidor sem tare… | classe | GraphOnlyPathsGameTest, GroundKeepersTest, PathNetworkGameTest |
| 126 | Peça colorida sai branca (autor 10-27, item 5; jogo da 0.3.93: 64 "cama sem lã" e lã pret… | classe | BigHouseFoundationGameTest, BigHouseModBlueprintGameTest, BlueprintReaderGameTest |
| 127 | Ajudas de quem está sem tarefa do ofício, cada uma com um ofício fixo (autor 10-27, item… | cita | ColonyTorchesGameTest |
| 128 | Nenhuma profissão espera mais de 5 minutos por nada (autor 10-27, emenda 11-02): 300 s se… | cita | IdleWatchGameTest |
| 129 | O nome sobre a cabeça tem a cor do estado (autor 10-27): laranja no ofício, azul em ajuda… | classe | IdleRoutineGameTest, IdleWatchGameTest, LargeFarmGameTest |
| 130 | Cinco animais no mesmo poste pedem curral, com prioridade (autor 10-27): o curral do esti… | cita | CrowdedPostGameTest |
| 131 | O chão em volta da obra nova fica na altura da rua (autor 10-27): na abertura, um anel de… | cita | DuneCutGameTest, SiteSurroundingsGameTest |
| 132 | As bancadas do mod têm painel e passivas (autor 10-27): shift+clique mostra titulares, es… | cita | BenchPassivesGameTest |
| 133 | Estoque cheio vira obra e produto (autor 10-28, jogo da 0.3.99: lenhador 74%, pedreiro 48… | cita | ItemStockGameTest |
| 134 | O adulto que sobra vai ao ofício que a vila espera (autor 10-28, jogo da 0.3.99: 6 de 18… | classe | BigHouseModBlueprintGameTest, ColonyCycleTest, ColonyDetectionGameTest |
| 135 | Tarefa só abre quando pode render (autor 10-28, jogo da 0.3.99): a ordem de estoque de pe… | classe | IdleFixesGameTest, ItemStockGameTest, ProfessionBalanceGameTest |
| 136 | O fazendeiro alcança como o jogador (autor 10-28, jogo da 0.3.99: roça 2 blocos acima, a… | classe | ColonyFixture, FarmPlanGameTest, FarmerGameTest |
| 137 | A escada da mina recomeça do topo (autor 10-28, jogo da 0.3.99: o mineiro passou 30 min n… | classe | IdleFixesGameTest |
| 138 | A rua da colônia é só caminho e é da vila (autor 10-28; depois do jogo da 0.3.104: "volta… | classe | BlockProtectionGameTest, BuildSiteGameTest, LumberjackGameTest |
| 139 | O lote sai do mapa de interesse (autor 10-28, jogo da 0.3.102: casa a 140 blocos, 24 acim… | cita | LotScoreMemoryTest |
| 140 | A rua estica pela ponta mais perto do centro, com um bloco de largura (Regra 186), pedind… | cita | RoadExtensionGameTest |
| 141 | O centro da vila não muda (autor 10-28, taiga simulada: o curral sem lote foi a 98 blocos… | cita | ColonyBoundsTest, ColonySavedDataTest, ConstructionPlannerReachTest |
| 142 | À noite só o preso vai à cama (autor 10-28, taiga simulada: 114 teleportes, um por aldeão… | cita | BedTeleportGameTest, NightHomeRangeGameTest |
| 143 | O baú público enche inteiro (autor 10-28, taiga simulada: 27 vezes o lenhador largou tora… | classe | BuildProgressGameTest, BuildReserveGameTest, BuilderGameTest |
| 144 | Alvo de coleta inalcançável sai da memória (autor 10-28, taiga simulada: 3 fazendeiros vo… | cita | SurfaceHitsTest |
| 145 | As camas da vila são as da caixa (autor 10-28, taiga simulada: a sonda de 64 blocos via p… | cita | ColonyBoundsTest, VillageDiaryGameTest |
| 146 | Fundidor e pedreiro produzem mais (autor 10-28, taiga simulada: fundidor 80% e pedreiro 6… | cita | IdleFixesGameTest |
| 147 | O diário da vila bate com o jogo (autor 10-28): cada fato vira VC_EVENT (obra aberta e fe… | cita | VillageDiaryGameTest |
| 148 | A obra nunca fica largada meio pronta (autor 10-28, taiga simulada: casa em "0 blocks lef… | cita | BuildClosingGameTest |
| 149 | Rua sem falha: falha de até 2 blocos de chão natural numa linha reta entre duas colunas d… | cita | HouseSurroundingsGameTest, VillageHelpersGameTest |
| 150 | O pastor tosquia por cima da cerca (autor 10-28, diagnóstico da 0.3.103: 45 pastores pres… | classe | ColonyFixture, ShepherdGameTest |
| 151 | O aldeão da colônia prefere a rua e evita a água (autor 10-28, jogo da 0.3.104: 4 de 7 te… | classe | StreetRouteGameTest, StreetRouteTest, WaterTerrainControlGameTest |
| 152 | O solo da vila nasce suave na geração (autor 10-29: crateras e montanhas no meio das vila… | classe | VillageBlendTest, VillageGroundGameTest, VillageSurfaceTest |
| 153 | Mais ajudas de quem está sem tarefa (autor 10-28, O1/O3/O4/O8; pedido de novo em 10-29),… | classe | GroundSmoothingGameTest, IdleFixesGameTest, ItemStockGameTest |
| 154 | Lenhador, fundidor e fazendeiro: os ajustes do O5–O7 (autor 10-28; pedido de novo em 10-2… | classe | ChestPlacerGameTest, IdleFixesGameTest, ItemStockGameTest |
| 155 | A boca da mina ganha o portão do autor (10-29, estrutura mine_entrance.nbt, 9×7×5): no lu… | classe | MineLandingGameTest, MinerGameTest, SmelterGameTest |
| 156 | O pedreiro alisa o chão da vila (autor 10-29, 11-06): sem tarefa, uma vez por minuto ele… | classe | GroundSmoothingGameTest, VillageBlendTest, VillageHelpersGameTest |
| 157 | O mineiro não quebra o que segura algo solto (autor 10-29, jogo da 0.3.108: o cascalho pe… | classe | MineGravityGameTest, MineSettlingGameTest, MinerGameTest |
| 158 | Sem cama no andar de baixo da casa de escada de mão (autor 10-29, jogo da 0.3.108: 9 de 2… | classe | BedChoiceGameTest, BedPlanTest, StructureEntrancesGameTest |
| 159 | O aldeão da colônia pula 2 blocos e por cima da cerca (autor 10-29, depois do jogo da 0.3… | cita | LumberjackGameTest, PenEscapeGameTest |
| 160 | O construtor trabalha do chão, e quem fica num telhado desce (autor 10-29, jogo da 0.3.10… | classe | BuildZoneGameTest, BuilderApproachGameTest, BuilderGameTest |
| 161 | Comer enche a vida (autor 10-29): todo aldeão que come (a comida do inventário, ou para c… | cita | VillagerRulesGameTest |
| 162 | O miolo do cercado fica livre (autor 10-29, jogo da 0.3.110: o cercado nasceu com blocos… | classe | GroundSmoothingGameTest |
| 163 | Roça por fazendeiro e pela maior das contas (autor 10-30): a vila abre uma roça para cada… | cita | ConstructionPriorityTest, FarmOwnersTest |
| 164 | Casa de reserva respeita teto de camas (autor 10-30): sem outro tipo com lote, a casa de… | cita | ConstructionPriorityTest, LotTypeRefusalsGameTest |
| 165 | Armazém pequeno antes do grande (autor 10-30): quando a vila precisa de armazenamento, ab… | cita | StoragePriorityGameTest, test_gen_storage_small |
| 166 | Transição em volta do lote: após cortar o terreno para a obra, o corredor de seis colunas… | classe | BuildSiteGameTest, DuneCutGameTest, LotTerraformGameTest |
| 167 | Cuidado físico do rebanho: só pastor vivo a até 5 blocos alimenta ou abate; a passiva da… | classe | FlockHarvestGameTest, FlockHarvestTest, FlockSurplusGameTest |
| 168 | Um curral grande por vila nova: a vila gerada após esta regra constrói no máximo um curra… | classe | CrowdedPostGameTest, EntrancesAtTheDoorGameTest, PenAccessGameTest |
| 169 | Passagem livre: aldeão da colônia quebra aos poucos as folhas naturais que bloqueiam seu… | classe | LeafPathsGameTest |
| 170 | Lote sem árvores: ao escolher terreno para qualquer estrutura, remove a árvore natural in… | classe | BiomeCoverageGameTest, BiomeSiteGameTest, FoundationPreparationGameTest |
| 171 | Pulo do aldeão (autor 11-04): a altura padrão do jogo; o maior só com obstáculo, cerca, e… | cita | VillagerRulesGameTest |
| 172 | Água aberta (autor 11-04): com mais de 5 blocos de água à frente, todo aldeão anda cerca… | cita | VillagerRulesGameTest |
| 173 | Zona da obra limpa (autor 11-04): árvore, tronco, folha e flor saem em todas as alturas,… | cita | TreeClearingGameTest |
| 174 | Ponte (autor 11-04): sem caminho a seco na zona da vila, o construtor atravessa a água, a… | cita | BridgeDeckGameTest |
| 175 | Casa do mineiro: ver Regra 211. A casa e a boca da mina entram na rede pelo grafo (Regra… | classe | EntrancesAtTheDoorGameTest, GraphOnlyPathsGameTest, MineHomePlansTest |
| 176 | Sinalizador dos caminhos (autor 11-04): /vc mae mostrar ligar\ | cita | RoadMapSyncGameTest |
| 177 | Grafo como mapa de trilhos (autor 11-05): uma linha contínua por corredor, no bloco centr… | cita | RoadLinesTest |
| 178 | Acesso livre (autor 11-05): o primeiro bloco fora de toda porta e portão é caminho; portã… | cita | BuildingGapsGameTest, EntrancesAtTheDoorGameTest, RoadLinesTest |
| 179 | Zona da obra limpa até o céu (autor 11-05): tudo que é natural e desprotegido na pegada,… | classe | FoundationPreparationGameTest, TreeClearingGameTest |
| 180 | Caminho coberto não é caminho (autor 11-05; emendada pela Regra 231): bloco de caminho se… | cita | RoadRulesGameTest |
| 181 | Pulo confere o teto (jogo 11-05): o caminho do aldeão da colônia só sobe onde o corpo cab… | cita | VillagerRulesGameTest |
| 182 | A mina cresce por dentro (autor 11-05): fim de escada abre ramal a partir dos salões que… | cita | WaterMineAccessGameTest |
| 183 | Anel de caminho (autor 11-05): o chão em volta do chafariz é caminho, sem quina fixa no g… | cita | RoadRulesGameTest |
| 184 | Quina é entrada (autor 11-05): da terceira releitura em diante, a quina da plantação mais… | cita | EntrancesAtTheDoorGameTest, RoadRulesGameTest |
| 185 | A ponta cresce (autor 11-05, emendada 11-10): por dia, as 4 pontas, uma por lado, avançam… | cita | RoadRulesGameTest |
| 186 | Rua de um bloco (autor 11-05): o caminho a até 3 blocos da linha que não é linha nem fren… | cita | EntrancesAtTheDoorGameTest, RetiredSpursTest, RoadLinesTest |
| 187 | Revogada (autor 11-06, ADR-047). A obra pedida não reserva região | classe | LotTypeRefusalsGameTest, MaeFinishGameTest, MaeRescueGameTest |
| 188 | Pedido só do que falta (jogo 11-05): o pedido de peça de obra não abre quando os baús cob… | classe | CraftFamilyGameTest, CraftingGameTest, FurnaceFeedGameTest |
| 189 | Madeira (autor 11-05): dentro da zona de prioridade, o lenhador pega a árvore conhecida m… | classe | LumberjackGameTest, LumberjackSearchGameTest, ProfessionBalanceGameTest |
| 190 | Cru na reserva (jogo 11-05): com o cru nos baús e a reserva impedindo assar, o fundidor l… | classe | CrusherGameTest, FurnaceFeedGameTest, SmelterGathersGameTest |
| 191 | Pedido sem resultado espera (autor 11-05): 3 fechamentos seguidos sem peça seguram o pedi… | cita | TaskLoopGuardTest |
| 192 | Tora é do carpinteiro (autor 11-05): sem peça a fazer, os outros ofícios fecham a tarefa… | classe | ChainRootsGameTest, ColonyFixture, ConstructionProjectTest |
| 193 | Mineiro alarga o nível (autor 11-05): quem espera ramal cava em volta do salão, até 6 ané… | cita | MinerGameTest |
| 194 | Coleta no expediente (autor 11-05): abate do pastor e item do chão só no expediente; emen… | classe | FlockSurplusGameTest, FlockWheatGameTest, GardenPiecesGameTest |
| 195 | Reunião do fim do dia (autor 11-05): o expediente acaba em 10000 tiques; até 11000 os ald… | cita | ShepherdGameTest, WorkClockTest |
| 196 | Curral antes do poste (autor 11-05): o pastor leva o animal solto ou amarrado ao curral c… | cita | PenAccessGameTest, ShepherdGameTest |
| 197 | Folha no caminho se corta (autor 11-05): todo aldeão da vila com destino, parado ao lado… | cita | LeafPassageGameTest |
| 198 | Lenhador no interior da vila (autor 11-05): a árvore é escolhida por faixas de 32 blocos… | cita | TreeNearestGameTest |
| 199 | Sem muda perto de estrutura (autor 11-05): árvore a até 3 blocos de casa, obra ou peça da… | cita | TreeReplantingGameTest |
| 200 | A obra pedida limpa o próprio lote (autor 11-05): árvore natural, folha e mato alto não r… | cita | MaeSitesGameTest |
| 201 | Revogada (autor 11-06, ADR-047). A régua do lote é a do construtor (Regra 213) | nenhum |  |
| 202 | Revogada (autor 11-06, ADR-047). A obra pedida não usa a prova por seção | nenhum |  |
| 203 | Revogada (autor 11-06, ADR-047). A água entra pelo construtor | nenhum |  |
| 204 | Revogada (autor 11-06, ADR-047). A peça da obra é posta pelo construtor | nenhum |  |
| 205 | Revogada (autor 11-06, ADR-047). A obra pedida não reserva região e não falha | nenhum |  |
| 206 | As pontas do grafo são os lados da vila (autor 11-05, emendada 11-10): fica a ponta mais… | cita | RoadLinesTest, RoadMapSyncGameTest, RoadRulesGameTest |
| 207 | Toda porta térrea é entrada (autor 11-05): o marcador branco fica no primeiro bloco em fr… | cita | DoorFrontsGameTest |
| 208 | Plantação sem anel (autor 11-06): o chão em volta da plantação não é pedido como caminho,… | cita | RoadLinesTest, RoadRulesGameTest |
| 209 | Porta de quintal fechado (autor 11-06): a caixa da peça pode incluir o quintal. Porta cuj… | cita | DoorFrontsGameTest |
| 210 | Toda estrutura onde se entra tem entrada (autor 11-06, 11-07): toda estrutura da vila ger… | cita | StructureEntrancesGameTest |
| 211 | Casa do mineiro na boca em uso (autor 11-06): a casa do mineiro fica ao lado da boca da s… | cita | StructureEntrancesGameTest |
| 212 | Sem tocha sozinha, vela não trava obra (autor 11-06): nenhum aldeão põe tocha da colônia… | cita | ArchitectureRulesTest, LotLightDecorGameTest |
| 213 | A obra MAE é a próxima do construtor (autor 11-06, ADR-047): a obra pedida entra na fila… | cita | MaePatienceGameTest, MaeSitesGameTest |
| 214 | Todo caminho é do grafo, em toda vila (autor 11-06, ADR-048): quem quer caminho só pede a… | cita | GraphOnlyPathsGameTest, PlannedPathsTest, RoadExtensionGameTest |
| 215 | O pedreiro ocioso ajuda no caminho e no chão (autor 11-06): sem tarefa do ofício, ele cal… | cita | GraphOnlyPathsGameTest |
| 216 | A rede é o grafo (autor 11-06, ADR-048): quem pergunta por onde se anda, o que está perto… | cita | GraphOnlyPathsGameTest |
| 217 | A ceia alimenta com o trigo de qualquer baú (autor 11-06): com fazendeiro na vila, a ceia… | cita | VillageMealsGameTest |
| 218 | A vila continua construindo (autor 11-06, muda a decisão de 10-12 de não repetir): com um… | cita | ConstructionOrderTest, VillageKeepsBuildingGameTest |
| 219 | Rotina à vista de quem está sem tarefa (autor 11-06): no expediente, depois de um minuto… | cita | IdleRoutineGameTest |
| 220 | Quem não tem ofício trabalha na vila (autor 11-06, exceção à regra temporária de 11-02):… | cita | GraphOnlyPathsGameTest |
| 221 | A vila gasta o que produz (autor 11-06): depois da ceia, cada adulto come 1 ponto de comi… | cita | VillageConsumesGameTest, VillageMealsGameTest |
| 222 | O socorro do MAE (autor 11-06, ADR-049): depois de 2 voltas inteiras sem lote para a próx… | cita | MaePatienceGameTest, MaeRescueGameTest |
| 223 | Branco só onde se entra (autor 11-07, desenho sobre o jogo da 0.4.42): o marcador fica co… | cita | DoorFrontsGameTest, EntrancesAtTheDoorGameTest, RetiredSpursTest |
| 224 | Todo caminho conta e liga em reta (autor 11-10, ADR-050): o grafo lê o caminho da caixa e… | cita | GraphStabilityGameTest, GraphStabilityTest, RoadLinesTest |
| 225 | O trecho que ficou trava (autor 11-10, ADR-050): coluna que passa 10 dias de jogo na linh… | cita | GraphStabilityGameTest, GraphStabilityTest, RoadGraphStoreTest |
| 226 | Socorro da vila inteira, em bairro (autor 11-10, ADR-051): só a volta sem lote da casa co… | cita | LotTypeRefusalsGameTest, MaeFinishGameTest, MaePatienceGameTest |
| 227 | Água interna sob a obra pedida (autor 11-10, ADR-051): a obra do jogador aterra a água da… | cita | MaeFinishGameTest |
| 228 | Ruína de save antigo sai (autor 11-10, ADR-051): a obra MAE que uma versão anterior deixo… | cita | MaeFinishGameTest |
| 229 | As pontas levam a vila para fora (autor 11-10, ADR-052): uma ponta azul por lado da caixa… | cita | RoadLinesTest, RoadRulesGameTest |
| 230 | A zona alcança a ponta (autor 11-10, ADR-052): a ponta calçada que sai 7 blocos ou mais d… | cita | RoadRulesGameTest |
| 231 | Caminho sob laje e ponta na extremidade (autor 11-10, sobre o jogo da 0.4.47): o grafo lê… | cita | RoadRulesGameTest, RoadTipsTest |
| 232 | A obra manda no artesão (autor 11-10, sobre a análise docs/audit/2026-11-10-qualidade-das… | cita | ProfessionBalanceGameTest |
| 233 | A vaga segue a falta (autor 11-10): com comida em falta, o fazendeiro vem primeiro na lis… | cita | ColonyCycleTest, ProfessionDemandTest |
| 234 | O trecho travado sai verde (autor 11-11): no sinalizador dos caminhos (/vc mae mostrar li… | cita | RoadMapSyncGameTest |
| 235 | Pastor pelo rebanho (autor 11-11, opção P1 de docs/audit/2026-11-11-atividade-das-profiss… | cita | ProfessionDemandTest |
| 236 | Pedreira de superfície no deserto (autor 11-11, opção M1): na vila de deserto, o mineiro… | cita | DesertQuarryGameTest |
| 237 | Composteira pela roça (autor 11-11, opção F2): o fazendeiro sem colheita nem canteiro com… | cita | FarmerCompostGameTest |
| 238 | Rebanho cresce com comida sobrando (autor 11-11, opção P2): com 32 ou mais da comida da e… | cita | FlockSurplusGameTest |
| 239 | Todo animal que procria com comida (autor 11-11, opção P4): o pastor cria ovelha, vaca, p… | cita | FlockSurplusGameTest |
| 240 | Lã guardada vira peça de lã (autor 11-11, opção P5): o pastor sem tarefa, com mais de 16… | cita | ShepherdWoolGoodsGameTest |
| 241 | Plantação grande do deserto (autor 11-11): a plantação do mod tem 16x16, borda de tora co… | cita | BuilderGameTest, LargeFarmGameTest |
| 242 | Quem arruma o chão (autor 11-11): nenhum bloco de caminho muda sozinho. Pedreiro, carpint… | cita | BuildingGapsGameTest, GraphOnlyPathsGameTest, GroundKeepersTest |
| 243 | Baú de qualquer lugar (autor 11-11, opção T4 de docs/audit/2026-11-11-jogo-0453.md): todo… | cita | VillageChestReachGameTest |
| 244 | Fora da simulação não congela (autor 11-11, opção T1c de docs/audit/2026-11-11-jogo-0453.… | cita | PinnedOnFlatGameTest |
| 245 | Cama longe vem para perto (autor 11-11, opção T3b): na troca de camas do amanhecer (Regra… | cita | BedPlanTest |
| 246 | Arrumar o chão é trabalho (autor 11-11, opção O2a): quem nivela, tapa buraco ou devolve a… | cita | WorkTimeTest |
| 247 | Construtor sem obra arruma o chão (autor 11-11, opção O1b): o construtor sem obra aberta,… | cita | GroundSmoothingGameTest |
| 248 | Um terço da madeira fica em tora (autor 11-11, opção O2c): o carpinteiro converte em tábu… | cita | ColonyGoalsTest |
| 249 | Fundição sem cru chama o pedreiro (autor 11-11, opção O2b-1): a cada ciclo, a tarefa de f… | cita | FurnaceFeedGameTest |
| 250 | A rua nasce no chão novo (autor 11-11, opção A): na geração, a coluna que o solo da vila… | cita | VillageGroundGameTest |
| 251 | Flor e muda não seguram a coluna (autor 12-11, opção A): na geração, a varredura do topo… | cita | VillageGroundGameTest |
| 252 | Acessório é enfeite (autor 12-11, opção A): peça accessory da vila gerada (plains_accesso… | cita | VillageGroundGameTest |
| 253 | O curral leva terra no solo (autor 12-11): o piso de grama da planta de curral sobre pedr… | cita | StreetLevelGameTest |
| 254 | Patamar nas dobras da escada da mina (autor 12-11, ADR-053): cada dobra do caracol ganha… | cita | MineLandingGameTest, MineSaveTest, MineShaftLayoutTest |
| 255 | Animal na corda vai para o curral (autor 12-11, opções A e B): com curral na vila, o past… | cita | ShepherdTetherGameTest |
| 256 | O arco da mina e a árvore com folha (autor 12-11, opções 1 e 2): a boca da mina ganha um… | cita | LumberjackGameTest, MineLandingGameTest, MinerGameTest |
| 257 | Areia e argila sem reserva de cru (autor 12-11, solução 3 da reanálise dos logs): areia,… | cita | RawStoneReserveTest |
| 258 | O adiantamento lê todas as estruturas da vila (autor 12-11, solução 2 da reanálise dos lo… | cita | BiomePiecesGameTest |
