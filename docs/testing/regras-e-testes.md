# Regras e testes

Gerado por `python scripts/rule_coverage.py`; não editar à mão. Toda suite do Sentinela Local roda todos os testes das classes
abaixo: a pergunta aqui é se existe teste para a regra, não se ele rodou. Citar não prova que o teste mede a regra, e usar a
classe prova menos ainda.

- **Cita a regra:** 285
- **Só usa a classe indicada:** 85
- **Sem nenhum teste:** 2
- **Fora de vigor:** 14

| # | regra | grau | testes |
| --- | --- | --- | --- |
| 1 | Colher até os baús encherem | cita | ChestPlacerGameTest, ColonyCycleGameTest, ColonyCycleTest |
| 2 | Colher no tempo de um jogador com ferro | cita | LumberjackGameTest, ProfessionRegistryTest |
| 3 | Nunca destruir construções da vila original, da colônia ou do jogador | cita | ArchitectureRulesTest, BuildSiteGameTest, BuilderGameTest |
| 4 | Dois trabalhadores por profissão | fora de vigor |  |
| 5 | Fabricar até metade do armazém | fora de vigor |  |
| 6 | Estrada primeiro, casa ligada a ela | cita | BuildSiteGameTest |
| 7 | O lenhador planta onde cortou | cita | FarmerGameTest, ShepherdGameTest |
| 8 | Um baú ao lado de cada cama — em toda casa (da vila e construída), encostado na parede, n… | cita | ChestPlacerGameTest |
| 9 | Subir e descer para alcançar, e poder voltar | cita | LumberjackGameTest |
| 10 | O construtor fabrica o que a expansão pede | cita | BuilderGameTest, HouseBillOfMaterialsGameTest, SmelterGameTest |
| 11 | Uma de cada profissão em cada vila | cita | ProfessionFloorTest |
| 12 | O centro fica em bloco que existe | cita | test_rule_coverage |
| 13 | A obra é uma que a colônia consiga fazer | cita | BuildSiteGameTest, ConstructionProjectTest, ConstructionResumeGameTest |
| 14 | O construtor alcança o alto da obra — desde a ADR-037 B1, de qualquer lugar da zona da ob… | cita | BuilderGameTest |
| 15 | A estrada cresce com a vila: a ponta pede as colunas ao grafo, e o aldeão calça (Regra 21… | cita | BuildSiteGameTest, RoadExtensionGameTest |
| 16 | Cada casa com espaço em volta | classe | BigHouseFoundationGameTest, BuildSiteGameTest, ColonyRoadsTest |
| 17 | A casa com uma lateral na estrada. Em moradias novas, priorizar a porta voltada ao centro… | cita | BlueprintDoorSideTest, BlueprintRotationTest, BuildSiteGameTest |
| 18 | O dia inteiro é expediente. Durante o expediente, adultos com ofício na colônia não inici… | cita | BuildProgressGameTest, WorkClockTest, WorkHoursGameTest |
| 19 | O lote fica no nível da estrada | cita | BuildSiteGameTest, LotTerraformGameTest |
| 20 | Cada vila constrói no estilo do bioma | cita | VillageBiomesGameTest, VillagePaletteTest |
| 21 | Toda casa nasce com cama, baú e lampião — resolvida em 10-22 (C-02, autor): cama, porta e… | cita | BuilderGameTest, ConstructionSaveTest, HouseBillOfMaterialsGameTest |
| 22 | O lote é livre no volume | cita | BuildSiteGameTest, VolumeSampleGameTest |
| 23 | O que já foi analisado se analisa de novo | cita | RingSweepResumeTest, TreeMarksTest |
| 24 | A vila de planície levanta a casa do jogo | cita | ConstructionProjectTest, ConstructionResumeGameTest, MinerGameTest |
| 25 | ~~A maior planta que couber no lote~~ | fora de vigor |  |
| 26 | Cadeia de produção, e paleta por bioma | classe | ActionJournalGameTest, AdvanceStockTest, BiomeBlockSwapTest |
| 27 | Só o catálogo do jogo, e o construtor aguarda | cita | BuilderGameTest, CraftingWorkFamilyTest, DesertSandTest |
| 28 | ~~Barreira de teste: casa pequena, mobília dispensada~~ | fora de vigor |  |
| 29 | A mina em escada, duas salas, galeria sem fim | cita | MinerGameTest |
| 30 | O mineiro recolhe tudo, e a boca tem endereço | cita | MinerGameTest, SmelterGameTest |
| 31 | O fazendeiro planta o que tem e colhe o que está pronto | classe | ActionJournalGameTest, BoneMealRoundGameTest, ColonyFixture |
| 32 | ~~Móveis e cama entram depois da casa pronta~~ — substituída em 10-22 pela 21 (C-02, auto… | fora de vigor |  |
| 33 | Pedido de commit+push também atualiza o JAR local | nenhum |  |
| 34 | A lava nunca surge automática | classe | BuilderGameTest, ChainRootsGameTest, ConstructionSupplyAuditGameTest |
| 35 | Carpinteiro titular na BigHouseMOD, cama e baú à direita da porta, corredor livre até a e… | cita | BigHouseModBlueprintGameTest |
| 36 | O viveiro (terra enraizada + rebento) é do lenhador | classe | DesertFixesGameTest, LumberjackNurseryTargetTest, TreeNurseryGameTest |
| 37 | Cada aldeão usa a ferramenta de ferro apropriada para a ação | classe | BetterToolGameTest, LumberjackGameTest, ProfessionReviewGameTest |
| 38 | Contratar primeiro a profissão de que a demanda depende, depois a lista | cita | HiringProperties |
| 39 | Comida é feita pelo fazendeiro (pão do trigo acima de 32) | classe | ProfessionReviewGameTest, VillageMealsGameTest |
| 40 | Piso de fundido só com cadeia do cru | cita | BuilderMaterialsGameTest |
| 41 | Corante, linha, pó de osso e drops de inimigo e animal aparecem automáticos | classe | BuilderGameTest, BuilderMaterialsGameTest, ConstructionStallGameTest |
| 42 | Mineiro para de guardar um tipo aos 256 no baú (fora o pedido) | classe | MinerHaulCapGameTest, MinerOverflowGameTest, PublicChestsGameTest |
| 43 | O pastor faz o rebanho procriar | classe | FlockHarvestGameTest, FlockHarvestTest, FlockSurplusGameTest |
| 44 | Ofício do mod exclui ofício Vanilla | cita | VillagerRulesGameTest |
| 45 | Os baús da colônia são todos os da vila, e só eles: com a vila medida, todo baú livre den… | cita | GroundPickupGameTest |
| 46 | A fome não influencia o trabalho: a comida decide só a procriação (Vanilla) | classe | VillageMealsGameTest |
| 47 | O aldeão não envelhece nem morre de velhice | nenhum |  |
| 48 | Todo aldeão da colônia recolhe qualquer item caído a até 5 blocos ao passar e o guarda em… | cita | GroundPickupGameTest, MinerOverflowGameTest |
| 49 | A ordem das obras (consolida 55 e 60, 1A de 13-10): faltando cama, casa; senão a oficina… | cita | ConstructionOrderTest, ConstructionPriorityTest |
| 50 | Toda verificação mede o tempo dos aldeões — trabalhando, andando, bloqueado, ocioso, enca… | cita | WorkTimeTest |
| 51 | Peça que falta aparece na 4ª tentativa sem sucesso, no baú da profissão que tentou (colet… | cita | BuilderMaterialsGameTest, test_state_report, test_time_ledger |
| 52 | ~~Uma roça por fazendeiro~~ | fora de vigor |  |
| 53 | A boca da mina só nasce na faixa da borda da caixa da vila (até 8 blocos da linha, dentro… | classe | MineEdgeGameTest, MineFluidGameTest, MineLeaseGameTest |
| 54 | Mina em rede (ADR-042): passada a escadaria e o salão, cada nível é uma grade de nós a ca… | classe | MineAccessGameTest, MineReturnGameTest, MineReturnTest |
| 55 | Roça grande primeiro; fundidor ocioso adianta o cru; lenhador ocioso planta mudas até 10… | cita | GrowingPiecesGameTest |
| 56 | Expansão do terreno: a caixa cresce só com rua e obra, nunca com faixa vazia anexada (0.3… | cita | PublicChestPlacementGameTest, VillageExpansionGameTest, VillageExpansionTest |
| 57 | O pastor põe no máximo um poste por casa e um por cercado (nenhum poste novo a até 6 bloc… | classe | EntrancesAtTheDoorGameTest, FarmRoundGameTest, PenAccessGameTest |
| 58 | Madeira e pedra cruas podem passar dos limites do baú do ofício e do teto por tipo: o exc… | classe | MinerHaulCapGameTest, MinerOverflowGameTest, PublicChestPlacementGameTest |
| 59 | Ajudam a obra só carpinteiro e pedreiro (fazendeiro e pastor não); o carpinteiro guarda u… | classe | BuilderPavingGameTest, ConstructionZoneGameTest, GraphOnlyPathsGameTest |
| 60 | O tipo de obra que passa uma volta inteira da varredura sem lote fica 30 min fora da vez;… | classe | BigHouseFoundationGameTest, ColonyModelsGameTest, ConstructionOrderTest |
| 61 | A colônia só é dada como abandonada sem vila à vista do centro nem dentro da caixa, e com… | classe | ColonyAbandonmentTest, GroundPickupGameTest, test_graphify_relabel |
| 62 | A vila trabalha com o jogador dentro da caixa ou a até 48 blocos dela (VillageFocus.PERIM… | classe | AbandonedColonyGameTest, ColonyBoundsTest, DrawnFromTest |
| 63 | Socorro contra parada com prazo único: o ofício que anda sem chegar ao alvo há 1 minuto d… | classe | WorkRescueTest |
| 64 | As ruas da vila são medidas como rede de caminhos conectados a partir do centro (de bloco… | classe | BuildSiteGameTest, ColonyMergeTriggerGameTest, GraphOnlyPathsGameTest |
| 65 | Bancadas exclusivas do mod: três blocos próprios (carpenter_bench, mason_cutter, blacksmi… | classe | BenchApproachTest, BenchPassivesGameTest, BenchUseGameTest |
| 66 | Cercado no nível da rua: estrutura cuja camada y=0 é só solo natural (grama, terra, areia… | classe | StreetLayerTest |
| 67 | Plantação por população: uma roça a cada 15 adultos, no mínimo uma desde a fundação; ao p… | cita | ConstructionPriorityTest, FarmOwnersTest, FarmPlanGameTest |
| 68 | O pastor aproveita o rebanho (manutenção do ofício, não toca a obra): somente um pastor v… | cita | FlockHarvestTest |
| 69 | A horta da casa surge sem gastar recurso: os blocos de chão da planta que não têm item ne… | cita | GardenPiecesGameTest |
| 70 | Forma do caminho: um bloco de largura, reto a partir da porta, virando só em ângulo reto.… | fora de vigor |  |
| 71 | O mineiro não constrói para fora da montanha: o desvio dele não põe bloco ao céu aberto (… | cita | DetourCostsTest, MinerCautionGameTest |
| 72 | Destino e contagem do rebanho: o curral grande misto tem prioridade e aceita todas as esp… | cita | PenModelGameTest, ShepherdRoundGameTest |
| 73 | Caminho de areia: a vila do deserto tem caminho próprio — cópia do caminho de terra do Va… | classe | BenchPassivesGameTest, BenchUseGameTest, BiomeSiteGameTest |
| 74 | A cama mais perto do trabalho: cada aldeão dorme na cama da vila mais perto de onde traba… | cita | BedChoiceGameTest, BedPlanTest, WalksTest |
| 75 | Movimentação natural: só o mineiro remodela o terreno para subir (degrau, túnel); ninguém… | classe | ClimbReshapeTest |
| 76 | O encalhado nunca desiste: larga o ofício e sobe até a superfície; a escada da fuga é tam… | classe | ClimbOutGameTest, ClimbOutwardGameTest, ClimbReshapeTest |
| 77 | Cama como último recurso: preso 5 vezes no mesmo lugar, ou sem caminho para casa à noite,… | classe | BedTeleportGameTest, ClimbOutwardGameTest, NightHomeRangeGameTest |
| 78 | Caminhada longa por etapas: um trajeto de 50 a 130 blocos vira etapas na superfície | classe | WalkLegsGameTest |
| 79 | A pedra é emprestada: o alvo do mineiro vale por um prazo curto, e só o progresso renova | classe | MineDescentLeaseGameTest, MineLeaseGameTest, MineLeaseTest |
| 80 | Só vira alvo o bloco com posição de trabalho válida e alcançável | classe | MinerPicksTest, MinerRerouteTest, MiningTargetGameTest |
| 81 | Ramal com dono: cada ramal da mina é reservado a um mineiro, com o estado dito pelo nome | classe | MineBranchTest, MineRampGameTest, MineRegistryTest |
| 82 | O mineiro memoriza a entrada da mina e chega por ela, por etapas, até o nível de agora | classe | IdleFixesGameTest, MineAccessGameTest, MineApproachProgressTest |
| 83 | Espera a areia assentar antes de voltar a cavar | classe | MineGravityGameTest, MineLooseGameTest, MineSettlingGameTest |
| 84 | Tapa e escora com o bloco do bioma (arenito no deserto) | classe | BiomeCoverageGameTest, MineGravityGameTest |
| 85 | O ferro descansa onde a mina não o tem: o piso de ferro cru não força busca sem veio | classe | DesertGroundGameTest |
| 86 | A peça que o construtor não alcança surge no lugar | classe | PenModelGameTest, UnreachablePiecesGameTest |
| 87 | Peça equivalente: sem a exata e sem tag do jogo, vale a equivalente da tabela | classe | EquivalentPiecesTest |
| 88 | Terracota, feno, argila, gelo e neve aparecem quando a obra pede (a Regra 352 retira a te… | classe | BuilderGameTest, DesertFixesGameTest, GardenPiecesGameTest |
| 89 | Degrau na frente da porta do lote que fica um bloco acima da rua | classe | FoundationPreparationGameTest |
| 90 | Terraplanagem do lote: no legado, corta até 5 e aterra até 2 blocos do chão do bioma, com… | classe | BuildSiteGameTest, DuneCutGameTest, LotEdgeGameTest |
| 91 | Casa fundadora: primeiro o lote plano exato, depois o aplainável; a vegetação do lote sai | classe | BigHouseLevelGameTest, BiomeCoverageGameTest, BiomeSiteGameTest |
| 92 | Deserto: a areia da planta vira arenito (emenda à Regra 27), e o chão da vila de areia nã… | classe | DesertGroundGameTest, DesertSandTest |
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
| 106 | A vila é uma caixa medida pela vila gerada, e não um raio | classe | BedChoiceGameTest, BigHouseEntranceGameTest, BiomeSoilGameTest |
| 107 | Marcas no mundo: contorno de chamas no lote, área da vila, dono de cada baú, nome do alde… | classe | BuildSiteGameTest, ChestMarkerGameTest, CraftingLookupGameTest |
| 108 | A Tocha das Almas cancela a obra: uma tocha das almas (também a de parede) posta pelo jog… | cita | LotLightDecorGameTest, MaeSitesGameTest |
| 109 | A casa fundadora BigHouseMOD: toda vila adotada a recebe uma única vez — cópia editada da… | classe | BedChoiceGameTest, BigHouseFoundationGameTest, BigHouseLevelGameTest |
| 110 | Nada que a colônia corta some (R-059 do estudo v2, autor 10-22): o que a terraplanagem do… | cita | CuttingsGameTest, LotTerraformGameTest |
| 111 | Suprimento excepcional só pela lista declarada (R-080 do estudo v2, autor 10-22): o que s… | cita | SupplyExceptionTest, VillageFarmAnimalsGameTest |
| 112 | Vocabulário único de bloqueio (R-052 do estudo v2, autor 10-22): toda soltura, desistênci… | cita | BlockCodeTest |
| 113 | A obra parada só em peças de lado não prende a vila (autor 10-22, playtest do deserto: o… | cita | BuildProgressGameTest, PieceRescueTest, StuckPiecesGameTest |
| 114 | Ninguém é posto onde o corpo não cabe, e ninguém fica preso para sempre (autor 10-27, jog… | cita | BedTeleportGameTest, PenLiftGameTest |
| 115 | O armazém do mod sobe do chão e é feito do material do bioma (autor 10-27: o armazém nasc… | cita | ColonyModelsGameTest, ConstructionOrderTest, StreetLevelGameTest |
| 116 | A argila aparece na terceira busca vazia, em todo bioma (autor 10-27, jogo da 0.3.93: na… | cita | LocateFallbackGameTest |
| 117 | O pastor recebe ração quando o fazendeiro não produziu (autor 10-27, jogo da 0.3.93: "the… | cita | ShepherdWoolDeficitGameTest |
| 118 | No deserto o bosque do lenhador é o dobro e se espalha pelas bordas (autor 10-27, jogo da… | classe | LargeFarmGameTest, VillageGroveGameTest |
| 119 | A mina desce em caracol, abre salão e desce de novo pelos quatro cantos, e o caminho que… | cita | BlueprintDoorSideTest, MineEntranceGameTest, MineSecondLevelTest |
| 120 | A tarefa do lenhador fecha concluída quando o alcance esvazia (autor 10-27, jogo da 0.3.9… | cita | TreeFellingGameTest |
| 121 | A roça conhecida responde primeiro, sempre (autor 10-27, jogo da 0.3.93: 622 "still sweep… | cita | CropRegistryGameTest |
| 122 | O curral é do pastor (autor 10-27, jogo da 0.3.93: 217 entradas em curral na planície e o… | cita | PenFloorsGameTest |
| 123 | A rede de caminhos da vila: toda porta se liga à rede pelo grafo, com o trajeto mais curt… | cita | PathNetworkGameTest, VillagePathPlannerTest |
| 124 | Construção nova nasce a no mínimo 3 blocos de qualquer construção (autor 10-27, D1): o lo… | cita | PathNetworkGameTest |
| 125 | Quem assenta o caminho (autor 10-27, D6): o pedreiro, o carpinteiro e o fundidor sem tare… | classe | GraphOnlyPathsGameTest, GroundKeeperGameTest, GroundKeeperPavingGameTest |
| 126 | Peça colorida sai branca (autor 10-27, item 5; jogo da 0.3.93: 64 "cama sem lã" e lã pret… | classe | BigHouseEntranceGameTest, BigHouseFoundationGameTest, BigHouseModBlueprintGameTest |
| 127 | Ajudas de quem está sem tarefa do ofício, cada uma com um ofício fixo (autor 10-27, item… | cita | ColonyTorchesGameTest |
| 128 | Nenhuma profissão espera mais de 5 minutos por nada (autor 10-27, emenda 11-02): 300 s se… | cita | IdleWatchGameTest |
| 129 | O nome sobre a cabeça tem a cor do estado (autor 10-27): laranja no ofício, azul em ajuda… | classe | IdleRoutineGameTest, IdleWatchGameTest, LargeFarmGameTest |
| 130 | Cinco animais no mesmo poste pedem curral, com prioridade (autor 10-27): o curral do esti… | cita | CrowdedPostGameTest |
| 131 | O chão em volta da obra nova fica na altura da rua (autor 10-27; o anel e a rampa viraram… | cita | DuneCutGameTest |
| 132 | As bancadas do mod têm painel e passivas (autor 10-27): shift+clique mostra titulares, es… | cita | BenchPassivesGameTest |
| 133 | Estoque cheio vira obra e produto (autor 10-28, jogo da 0.3.99: lenhador 74%, pedreiro 48… | classe | ColonyCycleTest, HiringProperties, IdleFixesGameTest |
| 134 | O adulto que sobra vai ao ofício que a vila espera (autor 10-28, jogo da 0.3.99: 6 de 18… | fora de vigor |  |
| 135 | Tarefa só abre quando pode render (autor 10-28, jogo da 0.3.99): a ordem de estoque de pe… | classe | IdleFixesGameTest, ItemStockGameTest, ProfessionBalanceGameTest |
| 136 | O fazendeiro alcança como o jogador (autor 10-28, jogo da 0.3.99: roça 2 blocos acima, a… | classe | ActionJournalGameTest, BoneMealRoundGameTest, ColonyFixture |
| 137 | A escada da mina recomeça do topo (autor 10-28, jogo da 0.3.99: o mineiro passou 30 min n… | classe | IdleFixesGameTest |
| 138 | A rua da colônia é só caminho e é da vila (autor 10-28; depois do jogo da 0.3.104: "volta… | classe | BlockProtectionGameTest, BuildSiteGameTest, GraphFloorGameTest |
| 139 | O lote sai do mapa de interesse (autor 10-28, jogo da 0.3.102: casa a 140 blocos, 24 acim… | cita | LotScoreMemoryTest |
| 140 | A rua estica pela ponta mais perto do centro, com um bloco de largura (Regra 186), pedind… | cita | RoadExtensionGameTest |
| 141 | O centro da vila não muda (autor 10-28, taiga simulada: o curral sem lote foi a 98 blocos… | cita | ColonyBoundsTest, ColonySavedDataTest, ConstructionPlannerReachTest |
| 142 | À noite só o preso vai à cama (autor 10-28, taiga simulada: 114 teleportes, um por aldeão… | cita | BedTeleportGameTest, NightHomeRangeGameTest |
| 143 | O baú público enche inteiro (autor 10-28, taiga simulada: 27 vezes o lenhador largou tora… | classe | BlockLedgerGameTest, BoneMealRoundGameTest, BuildProgressGameTest |
| 144 | Alvo de coleta inalcançável sai da memória (autor 10-28, taiga simulada: 3 fazendeiros vo… | cita | SurfaceHitsTest |
| 145 | As camas da vila são as da caixa (autor 10-28, taiga simulada: a sonda de 64 blocos via p… | cita | ColonyBoundsTest, VillageDiaryGameTest |
| 146 | Fundidor e pedreiro produzem mais (autor 10-28, taiga simulada: fundidor 80% e pedreiro 6… | cita | IdleFixesGameTest |
| 147 | O diário da vila bate com o jogo (autor 10-28): cada fato vira VC_EVENT (obra aberta e fe… | cita | VillageDiaryGameTest |
| 148 | A obra nunca fica largada meio pronta (autor 10-28, taiga simulada: casa em "0 blocks lef… | cita | BuildClosingGameTest |
| 149 | Rua sem falha: falha de até 2 blocos de chão natural numa linha reta entre duas colunas d… | cita | HouseSurroundingsGameTest, VillageHelpersGameTest |
| 150 | O pastor só tosquia junto da ovelha (autor 21-11, N05 do jogo da 0.4.115; substitui o alc… | cita | ShepherdGameTest |
| 151 | O aldeão da colônia prefere a rua e evita a água (autor 10-28, jogo da 0.3.104: 4 de 7 te… | cita | ColonyMergeTriggerGameTest, StreetRouteGameTest |
| 152 | O solo da vila nasce suave na geração (autor 10-29: crateras e montanhas no meio das vila… | classe | ClearAbovePieceGameTest, VillageBlendTest, VillageCoreTest |
| 153 | Mais ajudas de quem está sem tarefa (autor 10-28, O1/O3/O4/O8; pedido de novo em 10-29),… | classe | GroundSmoothingGameTest, IdleFixesGameTest, ItemStockGameTest |
| 154 | Lenhador, fundidor e fazendeiro: os ajustes do O5–O7 (autor 10-28; pedido de novo em 10-2… | cita | VillageGroveGameTest |
| 155 | A boca da mina ganha o portão do autor (10-29, estrutura mine_entrance.nbt, 9×7×5): no lu… | classe | BiomeBlueprintsGameTest, EntrancesAtTheDoorGameTest, MineHomeWayGameTest |
| 156 | O pedreiro alisa o chão da vila (autor 10-29, 11-06): sem tarefa, uma vez por minuto ele… | classe | BiomeSoilGameTest, FallingSandGuardGameTest, GroundSmoothingGameTest |
| 157 | O mineiro não quebra o que segura algo solto (autor 10-29, jogo da 0.3.108: o cascalho pe… | classe | MineGravityGameTest, MineLooseGameTest, MineSettlingGameTest |
| 158 | Sem cama no andar de baixo da casa de escada de mão (autor 10-29, jogo da 0.3.108: 9 de 2… | classe | BedChoiceGameTest, BedPlanTest, BiomeBlueprintsGameTest |
| 159 | O aldeão da colônia pula 2 blocos e por cima da cerca (autor 10-29, depois do jogo da 0.3… | cita | LumberjackGameTest, PenEscapeGameTest |
| 160 | O construtor trabalha do chão, e quem fica num telhado desce. Da zona da obra (pegada + 4… | cita | StreetRouteGameTest |
| 161 | Comer enche a vida (autor 10-29): todo aldeão que come (a comida do inventário, ou para c… | cita | VillagerRulesGameTest |
| 162 | O miolo do cercado fica livre (autor 10-29, jogo da 0.3.110: o cercado nasceu com blocos… | classe | BiomeSoilGameTest, FallingSandGuardGameTest, GroundSmoothingGameTest |
| 163 | Roça por fazendeiro e pela maior das contas (autor 10-30): a vila abre uma roça para cada… | cita | ConstructionPriorityTest, FarmOwnersTest |
| 164 | Casa de reserva respeita teto de camas (autor 10-30): sem outro tipo com lote, a casa de… | cita | ConstructionPriorityTest, LotTypeRefusalsGameTest |
| 165 | Armazém pequeno antes do grande (autor 10-30): quando a vila precisa de armazenamento, ab… | cita | StoragePriorityGameTest, test_gen_storage_small |
| 166 | Transição em volta do lote: após cortar o terreno para a obra, o corredor de seis colunas… | classe | BuildSiteGameTest, DuneCutGameTest, LotEdgeGameTest |
| 167 | Cuidado físico do rebanho: só pastor vivo a até 5 blocos alimenta ou abate; a passiva da… | cita | ShepherdHerdingGameTest |
| 168 | Currais da colônia: um grande misto prioritário por vila e seis modelos médios no rodízio… | cita | PenModelGameTest |
| 169 | Passagem livre: aldeão da colônia quebra aos poucos as folhas naturais que bloqueiam seu… | classe | LeafPathsGameTest |
| 170 | Lote sem árvores: ao escolher terreno para qualquer estrutura, remove a árvore natural in… | classe | BiomeCoverageGameTest, BiomeSiteGameTest, FoundationPreparationGameTest |
| 171 | Pulo do aldeão (autor 11-04; pulo padrão primeiro desde a 0.4.125): a altura padrão do jo… | cita | PenEscapeGameTest, VillagerRulesGameTest |
| 172 | Água aberta (autor 11-04): com mais de 5 blocos de água à frente, todo aldeão anda cerca… | cita | VillagerRulesGameTest |
| 173 | Zona da obra limpa (autor 11-04): árvore, tronco, folha e flor saem em todas as alturas,… | cita | TreeClearingGameTest |
| 174 | Ponte (autor 11-04): sem caminho a seco na zona da vila, o construtor atravessa a água, a… | fora de vigor |  |
| 175 | Casa do mineiro: ver Regra 211. A casa e a boca da mina entram na rede pelo grafo (Regra… | classe | EntrancesAtTheDoorGameTest, GraphOnlyPathsGameTest, GroundKeeperGameTest |
| 176 | Sinalizador dos caminhos (autor 11-04): /vc mae mostrar ligar\ | cita | RoadMapSyncGameTest |
| 177 | Grafo como mapa de trilhos (autor 11-05): uma linha contínua por corredor, no bloco centr… | cita | EntrancesAtTheDoorGameTest, RoadLinesTest |
| 178 | Acesso livre (autor 11-05): o primeiro bloco fora de toda porta e portão é caminho; portã… | cita | BuildingGapsGameTest, EntrancesAtTheDoorGameTest, RoadLinesTest |
| 179 | Zona da obra limpa até o céu (autor 11-05): tudo que é natural e desprotegido na pegada,… | classe | FoundationPreparationGameTest, TreeAndLeafGameTest, TreeClearingGameTest |
| 180 | Caminho coberto não é caminho (autor 11-05; emendada pela Regra 231): bloco de caminho se… | cita | RoadRulesGameTest |
| 181 | Pulo confere o teto (jogo 11-05): o caminho do aldeão da colônia só sobe onde o corpo cab… | cita | VillagerRulesGameTest |
| 182 | A mina cresce por dentro (autor 11-05): fim de escada abre ramal a partir dos salões que… | cita | WaterMineAccessGameTest |
| 183 | Anel de caminho (autor 11-05): o chão em volta do chafariz é caminho, sem quina fixa no g… | cita | RoadRulesGameTest |
| 184 | Quina é entrada (autor 11-05): da terceira releitura em diante, a quina da plantação mais… | classe | EntrancesAtTheDoorGameTest, GraphFloorGameTest, RoadRulesGameTest |
| 185 | A ponta cresce (autor 11-05, emendada 11-10): por dia, as 4 pontas, uma por lado, avançam… | cita | RoadRulesGameTest |
| 186 | Rua de um bloco (autor 11-05; a rua da vila gerada fica, Regra 316): o caminho a até 3 bl… | cita | EntrancesAtTheDoorGameTest, RetiredSpursTest, RoadLinesTest |
| 187 | Revogada (autor 11-06, ADR-047). A obra pedida não reserva região | classe | LotTypeRefusalsGameTest, MaeFinishGameTest, MaeRescueGameTest |
| 188 | Pedido só do que falta (jogo 11-05): o pedido de peça de obra não abre quando os baús cob… | classe | CraftFamilyGameTest, CraftingGameTest, FurnaceFeedGameTest |
| 189 | Madeira (autor 11-05): dentro da zona de prioridade, o lenhador pega a árvore conhecida m… | cita | ProfessionDemandTest |
| 190 | Cru na reserva (jogo 11-05): com o cru nos baús e a reserva impedindo assar, o fundidor l… | classe | CrusherGameTest, FurnaceFeedGameTest, SmelterGathersGameTest |
| 191 | Pedido sem resultado espera (autor 11-05): 3 fechamentos seguidos sem peça seguram o pedi… | cita | TaskLoopGuardTest |
| 192 | Tora é do carpinteiro (autor 11-05): sem peça a fazer, os outros ofícios fecham a tarefa… | classe | ChainRootsGameTest, ColonyFixture, ConstructionProjectTest |
| 193 | Mineiro alarga o nível (autor 11-05): quem espera ramal cava em volta do salão, até 6 ané… | cita | MinerGameTest |
| 194 | Coleta no expediente (autor 11-05): abate do pastor e item do chão só no expediente; emen… | classe | FlockHarvestGameTest, FlockHarvestTest, FlockSurplusGameTest |
| 195 | Reunião do fim do dia (autor 11-05): o expediente acaba em 10000 tiques; até 11000 os ald… | cita | ShepherdGameTest, WorkClockTest |
| 196 | Curral antes do poste (autor 11-05): o pastor leva o animal solto ou amarrado ao curral c… | cita | PenAccessGameTest, ShepherdGameTest |
| 197 | Folha no caminho se corta (autor 11-05): todo aldeão da vila com destino, parado ao lado… | cita | LeafPassageGameTest |
| 198 | Lenhador no interior da vila (autor 11-05): a árvore é escolhida por faixas de 32 blocos… | cita | TreeNearestGameTest |
| 199 | Sem muda perto de estrutura (autor 11-05): árvore a até 3 blocos de casa, obra ou peça da… | cita | TreeReplantingGameTest |
| 200 | A obra pedida limpa o próprio lote (autor 11-05): árvore natural, folha e mato alto não r… | cita | MaeSitesGameTest |
| 201 | Revogada (autor 11-06, ADR-047). A régua do lote é a do construtor (Regra 213) | fora de vigor |  |
| 202 | Revogada (autor 11-06, ADR-047). A obra pedida não usa a prova por seção | fora de vigor |  |
| 203 | Revogada (autor 11-06, ADR-047). A água entra pelo construtor | fora de vigor |  |
| 204 | Revogada (autor 11-06, ADR-047). A peça da obra é posta pelo construtor | fora de vigor |  |
| 205 | Revogada (autor 11-06, ADR-047). A obra pedida não reserva região e não falha | fora de vigor |  |
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
| 223 | Branco só onde se entra (o nó passou para o primeiro chão fora da estrutura pela Regra 30… | cita | DoorFrontsGameTest, EntrancesAtTheDoorGameTest, LampPostPlanTest |
| 224 | Todo caminho conta e liga em reta (autor 11-10, ADR-050): o grafo lê o caminho da caixa e… | cita | GraphStabilityGameTest, GraphStabilityTest, RoadLinesTest |
| 225 | O trecho que ficou trava (a trava passou a ser por aresta de rosa a rosa pela Regra 306;… | cita | GraphStabilityGameTest, GraphStabilityTest, RoadGraphStoreTest |
| 226 | Socorro da vila inteira, em bairro (autor 11-10, ADR-051): só a volta sem lote da casa co… | cita | LotTypeRefusalsGameTest, MaeFinishGameTest, MaePatienceGameTest |
| 227 | Água interna sob a obra pedida (autor 11-10, ADR-051): a obra do jogador aterra a água da… | cita | MaeFinishGameTest |
| 228 | Ruína de save antigo sai (autor 11-10, ADR-051): a obra MAE que uma versão anterior deixo… | cita | MaeFinishGameTest |
| 229 | As pontas levam a vila para fora (autor 11-10, ADR-052): uma ponta azul por lado da caixa… | cita | RoadLinesTest, RoadRulesGameTest |
| 230 | A zona alcança a ponta (autor 11-10, ADR-052): a ponta calçada que sai 7 blocos ou mais d… | cita | RoadRulesGameTest |
| 231 | Caminho sob laje e ponta na extremidade (autor 11-10, sobre o jogo da 0.4.47): o grafo lê… | cita | RoadRulesGameTest, RoadTipsTest |
| 232 | A obra manda no artesão (autor 11-10, sobre a análise docs/audit/2026-11-10-qualidade-das… | cita | ProfessionBalanceGameTest |
| 233 | A vaga segue a falta (autor 11-10): com comida em falta, o fazendeiro vem primeiro na lis… | cita | ColonyCycleTest, ProfessionDemandTest |
| 234 | O trecho travado sai verde (só aresta travada pela Regra 306 sai verde; autor 11-11): no… | cita | RoadMapSyncGameTest |
| 235 | Pastor pelo rebanho (autor 11-11, opção P1 de docs/audit/2026-11-11-atividade-das-profiss… | cita | ProfessionDemandTest |
| 236 | Pedreira de superfície no deserto (autor 11-11, opção M1): na vila de deserto, o mineiro… | cita | DesertQuarryGameTest |
| 237 | Composteira pela roça (autor 11-11, opção F2): o fazendeiro sem colheita, canteiro nem la… | cita | FarmerCompostGameTest |
| 238 | Rebanho cresce com comida sobrando (autor 11-11, opção P2): com 32 ou mais da comida da e… | cita | FlockHarvestTest, FlockSurplusGameTest |
| 239 | Todo animal que procria com comida (autor 11-11, opção P4): o pastor cria ovelha, vaca, p… | cita | FlockSurplusGameTest |
| 240 | Lã guardada vira peça de lã (autor 11-11, opção P5): o pastor sem tarefa, com mais de 16… | cita | ShepherdWoolGoodsGameTest |
| 241 | Plantação grande do deserto (autor 11-11): a plantação do mod tem 16x16, borda de tora co… | cita | BuilderGameTest, LargeFarmGameTest |
| 242 | Quem arruma o chão (autor 11-11; a sobra da vila gerada fica, Regra 316): nenhum bloco de… | cita | BuildingGapsGameTest, GraphOnlyPathsGameTest, GroundKeepersTest |
| 243 | Baú de qualquer lugar (autor 11-11, opção T4 de docs/audit/2026-11-11-jogo-0453.md): todo… | cita | VillageChestReachGameTest |
| 244 | Fora da simulação não congela (autor 11-11, opção T1c de docs/audit/2026-11-11-jogo-0453.… | cita | PinnedOnFlatGameTest |
| 245 | Cama longe vem para perto (autor 11-11, opção T3b): na troca de camas do amanhecer (Regra… | cita | BedPlanTest |
| 246 | Arrumar o chão é trabalho (autor 11-11, opção O2a): quem nivela, tapa buraco ou devolve a… | cita | WorkTimeTest |
| 247 | Construtor sem obra arruma o chão (autor 11-11, opção O1b): o construtor sem obra aberta,… | cita | GroundSmoothingGameTest |
| 248 | Um terço da madeira fica em tora (autor 11-11, opção O2c): o carpinteiro converte em tábu… | cita | ColonyGoalsTest, RawWithoutReserveTest |
| 249 | Fundição sem cru chama o pedreiro (autor 11-11, opção O2b-1): a cada ciclo, a tarefa de f… | cita | FurnaceFeedGameTest |
| 250 | A rua nasce no chão novo (autor 11-11, opção A): na geração, a coluna que o solo da vila… | cita | VillageGroundGameTest |
| 251 | Flor e muda não seguram a coluna (autor 12-11, opção A): na geração, a varredura do topo… | cita | VillageGroundGameTest |
| 252 | Acessório é enfeite (autor 12-11, opção A): peça accessory da vila gerada (plains_accesso… | cita | LampPostPlanTest, VillageGroundGameTest |
| 253 | O curral leva terra no solo (autor 12-11): o piso de grama da planta de curral sobre pedr… | cita | PenModelGameTest, StreetLevelGameTest |
| 254 | Patamar nas dobras da escada da mina (autor 12-11, ADR-053): cada dobra do caracol ganha… | cita | MineLandingGameTest, MineSaveTest, MineShaftLayoutTest |
| 255 | Animal na corda vai para o curral (autor 12-11, opções A e B): com curral na vila, o past… | cita | ShepherdTetherGameTest |
| 256 | O arco da mina e a árvore com folha (autor 12-11, opções 1 e 2): a boca da mina ganha um… | cita | LumberjackGameTest, MineLandingGameTest, MinerGameTest |
| 257 | Areia e argila sem reserva de cru (autor 12-11, solução 3 da reanálise dos logs): areia,… | cita | RawWithoutReserveTest |
| 258 | O adiantamento lê todas as estruturas da vila (autor 12-11, solução 2 da reanálise dos lo… | cita | BiomePiecesGameTest |
| 259 | Um aldeão de cada profissão na vila nova (autor 12-11; as camas da casa também contam os… | cita | VillageFoundationGameTest |
| 260 | Casa do mineiro na fundação (autor 12-11): quando a BigHouseMOD acaba de ser posta, a fun… | cita | MinerHomeFoundationGameTest |
| 261 | Poste de luz nas ruas fixas (autor 12-11, opção B): base de 1 bloco de pedra do bioma (ar… | cita | LampPostGameTest, LampPostPlanTest |
| 262 | Cada estrutura do mod na versão do bioma (autor 12-11, troca automática): ao erguer uma e… | cita | BiomeBlockSwapTest, BiomeBlueprintsGameTest |
| 263 | O fundidor funde onde está (autor 13-11, jogo da 0.4.68): a fundição não exige que o fund… | cita | ClayLoopGameTest, SmelterGameTest |
| 264 | O grafo nunca passa sobre a água (autor 13-11, jogo da 0.4.68): nenhuma linha do grafo de… | cita | EntrancesAtTheDoorGameTest, RoadLinesTest |
| 265 | O grafo não atravessa estrutura (autor 13-11, jogo da 0.4.68, opção "parede e casas Vanil… | cita | GraphStabilityGameTest |
| 266 | Uma casa do mineiro basta (autor 13-11, jogo da 0.4.68): com uma casa do mineiro pronta n… | cita | StructureEntrancesGameTest |
| 267 | Plantação grande em todo bioma (autor 13-11, jogo da 0.4.68: fazendeiro da planície com 0… | cita | LargeFarmGameTest |
| 268 | O bosque acha o chão em qualquer altura (autor 13-11, jogo da 0.4.68: 0 mudas plantadas,… | cita | VillageGroveGameTest |
| 269 | O prazo do pastor pela distância (rumo a curral, sem prazo pela Regra 309; autor 13-11, j… | cita | ShepherdHerdingGameTest |
| 270 | Sem ofício fora das análises (autor 13-11, ADR-055 item 7): o aldeão sem ofício do mod nã… | cita | WorkTimeSwingGameTest, test_action_report, test_time_ledger |
| 271 | A muda do bosque surge se faltar no estoque, também no deserto (autor 13-11, ADR-055 item… | cita | VillageGroveGameTest |
| 272 | Corante das flores (autor 13-11, ADR-055 item 11): no ócio do ofício, o pastor colhe uma… | cita | ShepherdDyesGameTest |
| 273 | Um construtor por vila (autor 13-11, ADR-055): a vila tem uma obra aberta por vez, então… | cita | ProfessionGrowthTest, ProfessionPolicySetTest, ProfessionShunTest |
| 274 | A obra conta como trabalho do construtor (autor 13-11, ADR-055 item 14): sem trabalho med… | cita | BedChoiceGameTest |
| 275 | Soleira de casa não é curral (autor 13-11, jogo da 0.4.75): o que se alcança passando por… | cita | FencedInGameTest, PenEscapeOutTest |
| 276 | Alçapão em pé se pula e se evita (autor 13-11, jogo da 0.4.79): o alçapão aberto prende c… | cita | FencedInGameTest |
| 277 | O lenhador encosta na árvore (autor 13-11, jogo da 0.4.79; revoga o corte de longe de 202… | cita | TreeChoiceSpeciesGameTest, TrunkStandGameTest |
| 278 | Água só sem caminho seco (autor 13-11, jogo da 0.4.75; muda a decisão de 2026-11-01): for… | cita | ColonyPathingGameTest, MinerCautionGameTest |
| 279 | Solto é quem saiu do lugar (autor 14-11, jogo da 0.4.75): o encalhado por congelar duas v… | cita | StrandedWorkersTest |
| 280 | Árvore no caminho não trava o calçamento (autor 14-11, jogo da 0.4.75): a árvore (tronco… | cita | LeafPathsGameTest, PathTreeClearingGameTest |
| 281 | Tronco posto sai no log (autor 13-11, jogo da 0.4.79): todo tronco que o mod põe no mundo… | cita | LeafPathsGameTest |
| 282 | Só a rede reserva chão contra lote (autor 14-11, jogo da 0.4.79): com a rede publicada pe… | cita | BuildSiteGameTest |
| 283 | O pastor recolhe só dentro da caixa da vila (autor 14-11, opção B da varredura dos GameTe… | cita | PenAccessGameTest, ShepherdGameTest, ShepherdHerdingGameTest |
| 284 | O animal atravessa o portão amarrado (autor 14-11, VF-010, opção "prender ao portão"): en… | cita | PenTetherGameTest |
| 285 | Aldeão preso na água sai (autor 14-11, jogo da 0.4.84: vários ofícios presos na água): o… | cita | WaterRescueGameTest |
| 286 | O mineiro novo dorme junto da mina (autor 14-11, jogo da 0.4.84: a cama ao lado da mina n… | cita | BedChoiceGameTest |
| 287 | O fazendeiro entra na plantação de borda de tora (autor 15-11, jogo da 0.4.85: o fazendei… | cita | ColonyPathingGameTest, IdleFixesGameTest |
| 288 | Nenhuma estrutura da vila nasce sobre água (autor 15-11, jogo da 0.4.86: casas e lampião… | cita | VillageGroundGameTest |
| 289 | A plantação grande trabalha (autor 15-11, jogo da 0.4.86: 167 canteiros arados e 5 planta… | cita | FarmerTillLoopGameTest, LargeFarmSeedsGameTest |
| 290 | Tronco inteiro e folha no caminho (autor 15-11, G7 da ADR-056; jogo da 0.4.85: "sobrou um… | cita | TreeAndLeafGameTest, WaterTerrainControlGameTest |
| 291 | O grafo pisa na tábua de rua e na frente de toda porta (autor 15-11, G1 e G5 da ADR-056;… | cita | GraphFloorGameTest |
| 292 | Sem torre de minério e sem ar protegido no alisamento (autor 15-11, G3 da ADR-056, escolh… | cita | OreTowerGameTest, VillageGroundGameTest |
| 293 | O pátio da boca da mina (autor 15-11, G4 da ADR-056; o nó de entrada passou para sob o ar… | cita | MineYardGameTest |
| 294 | Os ociosos tiram a sobra da vila (autor 15-11, G6 da ADR-056; jogo da 0.4.85: bloco solto… | cita | StrayBlocksGameTest |
| 295 | A ligação do grafo não some na montanha (autor 15-11, jogo da 0.4.88, opção 3). O grafo o… | cita | EntrancesAtTheDoorGameTest, GraphFloorGameTest, RoadLinesTest |
| 296 | As estruturas da vila nascem no solo suavizado (autor 15-11, jogo da 0.4.88 em montanha,… | cita | VillageBlendTest, VillagePiecesGameTest |
| 297 | O chão em volta da obra nova é suavizado como o da vila (autor 15-11, jogo da 0.4.88 em m… | cita | SiteSurroundingsGameTest |
| 298 | A vila nova nasce enxuta (autor 15-11, jogo da 0.4.88, opção C corrigida: 9 aldeões da vi… | cita | HiringProperties, IdleFixesGameTest, ProfessionDemandTest |
| 299 | Entrega do rebanho. O abate preserva animais na guia ou em coleta. Na entrega, abre a por… | cita | FlockHarvestGameTest, ShepherdHerdingGameTest |
| 300 | A porta e as partes soltas entram no grafo (autor 16-11, jogo da 0.4.89, opção C com o ro… | cita | RoadLinesTest |
| 301 | A argila aparece na hora pedida (só para obra e com teto de 64 guardadas pela Regra 308;… | cita | ClayLoopGameTest, ClayOnRequestGameTest |
| 302 | O nó da mina fica sob o arco (autor 16-11, jogo da 0.4.89, opção A; muda o nó de entrada… | cita | MineNodeGameTest, MineYardGameTest |
| 303 | A frente da porta nasce no piso dela (a entrada passou para a primeira coluna livre e gan… | cita | DoorFrontsGameTest |
| 304 | Casa da vila não fica sem solo (autor 16-11, jogo da 0.4.89, opção A+B: casa suspensa sob… | cita | FallingSandGuardGameTest, UnderPieceFillGameTest |
| 305 | A entrada da casa e o patamar em volta ficam no piso (autor 17-11, jogo da 0.4.90, opção… | cita | DoorFrontsGameTest, EntrancesAtTheDoorGameTest, VillagePiecesGameTest |
| 306 | Só a aresta inteira de rosa a rosa fica fixa (autor 17-11, jogo da 0.4.90, opção B: fixed… | cita | GraphStabilityGameTest, LampPostGameTest, RoadMapSyncGameTest |
| 307 | Telhado não é chão do grafo (autor 17-11, jogo da 0.4.90, opção A: cinco das seis portas… | cita | GraphFloorGameTest |
| 308 | A fundição para na quantidade pedida, e a argila só surge para a obra (autor 17-11, jogo… | cita | ClayLoopGameTest, ClayOnRequestGameTest |
| 309 | O pastor leva o animal até o curral, seja qual for a distância (autor 17-11, jogo da 0.4.… | cita | ShepherdHerdingGameTest |
| 310 | O fazendeiro põe pó de osso em cada pé, uma volta por vez (autor 17-11, jogo da 0.4.90, o… | cita | BoneMealRoundGameTest, FarmerGameTest |
| 311 | O pedreiro mói a areia antes de a obra pedir (autor 17-11, jogo da 0.4.90, opção B: 525 p… | cita | CrusherGameTest |
| 312 | O mineiro preso sobe rumo à borda da vila (autor 17-11, jogo da 0.4.90: o mineiro ficou p… | cita | ClimbOutwardGameTest |
| 313 | Depois do degrau da porta, a entrada fica no chão (autor 17-11, jogo da 0.4.91, opção 1A:… | cita | DoorFrontsGameTest, VillagePiecesGameTest |
| 314 | A casa do mod nasce com caminho na porta (autor 17-11, jogo da 0.4.91, opção 2A: a porta… | cita | DoorPathsGameTest |
| 315 | O fazendeiro semeia a terra comum até o fim, e o alvo recusado fica de lado (autor 18-11,… | cita | FarmerSetAsideTest, FarmerTillLoopGameTest |
| 316 | A rua da vila gerada não é sobra (autor 18-11, estudo do grafo §3.4, corte do laço: a pas… | cita | RoadRulesGameTest |
| 317 | O log conta as mudanças do grafo no dia (autor 18-11, estudo do grafo §8 e §9 passo 2): a… | cita | RoadGraphChangesTest |
| 318 | Três modos do grafo, um ligado por vez (autor 18-11, ADR-057): o grafo de ruas é montado… | cita | RoadGraphModeTest, RoadMapRunsTest |
| 319 | Modo regioes: o grafo refeito só onde mudou (autor 18-11, ADR-057, modo 2; emendada no me… | cita | RoadRegionEventsGameTest, RoadRegionsTest |
| 320 | Modo vizinhanca: o grafo nasce das entradas, liga todas e cresce (autor 18-11, ADR-057, m… | cita | NeighborhoodRoadsTest |
| 321 | A porta do jogador é entrada do grafo (autor 18-11, para todos os modos do grafo; cumpre… | cita | PlayerDoorsGameTest |
| 322 | Só se calça a aresta estável (autor 18-11, para todos os modos do grafo, junto da ideia d… | cita | EntrancesAtTheDoorGameTest, RoadStabilityTest |
| 323 | O terreno da vila inteira arrumado pelos aldeões (autor 18-11, escolha "a vila inteira":… | cita | GradePlanTest, TerrainDefectsTest, TerrainQueueGameTest |
| 324 | O lenhador encosta no carvalho de copa baixa (autor 18-11, escolha "tirar a folha"): quan… | cita | TrunkStandGameTest |
| 325 | A plantação grande arada e semeada inteira (autor 18-11, escolha "ler o molde e semear es… | cita | LargeFarmBedsGameTest |
| 326 | A árvore sai inteira, com galho e tronco ligados (autor 19-11, jogo da 0.4.93, escolha "t… | cita | PathTreeClearingGameTest |
| 327 | O curral é um lugar (autor 19-11, jogo da 0.4.93 em planície, ADR-058, pacote B): na leit… | cita | ShepherdRoundGameTest |
| 328 | O pastor desiste do desgarrado que não alcança (autor 19-11, ADR-058; emenda a Regra 309)… | cita | ShepherdRoundGameTest |
| 329 | A ronda do pastor (autor 19-11, ADR-058; emenda a Regra 72): uma vez por dia, no expedien… | cita | ShepherdRoundGameTest |
| 330 | O Zelador, dono do chão da vila (autor 19-11, ADR-059, pacote B; nome "Zelador", sem banc… | cita | GraphOnlyPathsGameTest, GroundKeepersTest, GroundSmoothingGameTest |
| 331 | O caminho atravessa o feno (autor 19-11, ADR-059): no cálculo do grafo, o fardo de feno s… | cita | GroundKeeperGameTest |
| 332 | O pasto do curral não é roça (autor 19-11, jogo da 0.4.93 em planície, ADR-060; emenda a… | cita | FarmRoundGameTest |
| 333 | A ronda da roça (autor 19-11, ADR-060, pacote B; emenda a Regra 267): sem tarefa de comid… | cita | FarmRoundGameTest |
| 334 | O chão que o jogador mexeu fica como ele deixou (autor 20-11, jogo da 0.4.93 em planície,… | cita | PlayerGroundGameTest |
| 335 | O grafo nunca busca porta do andar de cima (autor 20-11, jogo da 0.4.93 em planície: a en… | cita | BigHouseEntranceGameTest, DoorFrontsGameTest |
| 336 | A descida cavada (autor 20-11, jogo da 0.4.93 em planície, escolha A3 da pesquisa docs/re… | cita | MineStepDownGameTest |
| 337 | Nada solto em cima da peça da vila (autor 20-11, jogo da 0.4.93 em montanha: pedra, terra… | cita | ClearAbovePieceGameTest |
| 338 | A BigHouseMOD nasce na altura das casas (autor 20-11, jogo da 0.4.93 em montanha, escolha… | cita | BigHouseLevelGameTest, FoundationLotSearchTest |
| 339 | O ofício não segura pedido impossível (autor 20-11, jogo da 0.4.93 em planície, opções O1… | cita | CraftBarrierTest, SmelterReleaseGameTest, WaitReasonsTest |
| 340 | A reserva por classe de bloco (autor 20-11, fase 2 do plano de ofícios e reservas, ADR-06… | cita | AdvanceStockTest, BlockReserveTest, ColonyGoalsTest |
| 341 | O contratado perde o ofício Vanilla na hora (autor 20-11: viu um trabalhador do mod ganha… | cita | VanillaTradeAfterHireGameTest |
| 342 | O registro de blocos (autor 20-11, fase 3 do plano de ofícios e reservas, ADR-062): por c… | cita | BlockLedgerGameTest, BlockLedgerTest, test_stock_report |
| 343 | A medida das travas de cru (autor 20-11, fase 4 do plano de ofícios e reservas, ADR-062):… | cita | KeepLedgerTest, test_stock_report |
| 344 | A telemetria dos aldeões e dos itens (autor 20-11, plano docs/research/2026-11-20-telemet… | cita | ItemFlowGameTest, TaskTraceGameTest, VillagerDayGameTest |
| 345 | O conflito com o jogador e a reafirmação (autor 20-11, telemetria F1 a F3b do plano docs/… | cita | EditLedgerGameTest, test_player_report |
| 346 | Salvar todas as análises de uma vez (autor 20-11): /vc salvar, comando de jogador, reúne… | cita | SnapshotCommandGameTest |
| 347 | O miolo da vila segue a forma das peças (autor 20-11, jogo da 0.4.110: "terreno quadrado… | cita | VillageCoreTest |
| 348 | Uma descida por arco (autor 20-11, jogo da 0.4.111: muitas aberturas de mina em volta do… | cita | MineDigLimitTest, MineLooseGameTest, MineOneDescentTest |
| 349 | Menos matéria-prima retida nos baús (autor 21-11, análise A8, ADR-062): a reserva de pó d… | cita | BoneMealRoundGameTest, IdleFixesGameTest, SmelterAdvanceGameTest |
| 350 | A terra excedente fica no chão (autor 21-11, análise A7, exceção à Regra 48): ao passar p… | cita | GroundPickupGameTest |
| 351 | Grama e caminho na telemetria (autor 21-11, análise A9, emenda à Regra 345): sem autor de… | cita | EditLedgerGameTest, MineFloorGameTest, SiteSurroundingsGameTest |
| 352 | Tijolos de pedra quando falta argila (autor 21-11, análise A6, emenda à Regra 88): sem ar… | cita | BiomeBlueprintsGameTest, ItemStockGameTest |
| 353 | Tochas da colônia na mina (autor 21-11, análise A2, exceção à Regra 212): o mineiro insta… | cita | ArchitectureRulesTest, MineLandingGameTest |
| 354 | Cinco mudas por lenhador no bosque perto da vila (autor 21-11, análise A3a): o lenhador u… | cita | VillageGroveGameTest |
| 355 | O pastor que tosquia dentro do cercado está trabalhando (autor 25-11, escolha C do N05):… | cita | ShepherdPenGraceGameTest |
| 356 | O chão que o Zelador mexe não estraga a vila (autor 25-11, jogo da 0.4.136 no deserto, es… | cita | FallingSandGuardGameTest |
| 357 | O plano de alturas da vila (autor 25-11, ADR-063, G5 do jogo da 0.4.136, decisões 🅲: "a v… | cita | GradeWorkGameTest, ProfessionAssignerTest |
| 358 | Calçar não tira a vez do resto do chão (autor 25-11, jogo da 0.4.139, escolha E1 🅰️: o Ze… | cita | GroundKeeperPavingGameTest |
| 359 | A boca da mina nasce no chão aberto e alcançado a pé (autor 25-11, jogo da 0.4.139 no des… | cita | MineMouthInsideGameTest, MineMouthSurfaceGameTest |
| 360 | O nó de entrada fica na frente da porta, e o da plantação no meio do lado (autor 25-11, n… | cita | DoorFrontsGameTest, EntrancesAtTheDoorGameTest, FallingSandGuardGameTest |
| 361 | A foto do terreno da vila (autor 25-11, ADR-064, programa 🅲: medições profundas do estado… | cita | TerrainSnapshotGameTest |
| 362 | Bloco de trabalho não é telhado, e a descida não vira laço (autor 25-11, nota do jogo da… | cita | RoofDescentGameTest |
| 363 | O aterro usa o chão do bioma (autor 25-11, G1 🅲 do jogo da 0.4.136 e notas do jogo da 0.4… | cita | BiomeSoilGameTest |
| 364 | Só o Zelador muda o terreno, e só o chão natural (autor 25-11, nota do jogo da 0.4.141, e… | cita | StreetKeptGameTest |
| 365 | O viveiro só nasce a céu aberto (autor 25-11, nota do jogo da 0.4.143, escolha H3 🅰️: "nã… | cita | NurserySkyGameTest |
| 366 | A porta que olha para fora não leva o nó para trás da casa (autor 25-11, notas do jogo da… | cita | DoorFrontsGameTest |
| 367 | Toda entrada de construção se liga ao grafo sem esperar a vez (autor 25-11, notas do jogo… | cita | RoadLinesDoorsTest |
| 368 | Sem vala em volta da obra, e a cova funda perto das construções é enchida (autor 25-11, n… | cita | OpenSiteRingGameTest |
| 369 | A casa grande é andável por dentro (autor 26-11, H5 do jogo da 0.4.143, o pastor preso no… | cita | BigHouseModBlueprintGameTest, DoorPathsGameTest, StreetLevelGameTest |
| 370 | Baú novo não tira o acesso a uma cama (autor 26-11, H5 do jogo da 0.4.143: "todos baú est… | cita | ChestPassageGameTest |
| 371 | A entrada da mina e as portas perto dela entram no grafo (autor 26-11, notas do jogo da 0… | cita | MineYardGameTest |
| 372 | A coleira da cama (autor 26-11, notas do jogo da 0.4.145, escolha J1 🅱️ com o raio contad… | cita | HomeLeashGameTest |
| 373 | A plantação sem lote perto espera a busca da casa terminar (autor 26-11, notas do jogo da… | cita | FarmPostponeTest, FirstBuildsTest |
| 374 | Curral grande e plantação grande primeiro, e uma plantação a cada 10 obras (autor 26-11,… | cita | FirstBuildsTest |
| 375 | O log diz por que os lotes foram recusados (autor 26-11, notas do jogo da 0.4.145, escolh… | cita | LotRefusalsDayTest |
| 376 | O construtor mora perto da obra (autor 26-11, notas do jogo da 0.4.145, J7: "o construtor… | cita | BuilderHomeGameTest |
| 377 | O degrau de fuga some em um dia (autor 26-11, notas do jogo da 0.4.145, escolha J4 🅱️: "i… | cita | TemporaryStepsGameTest |
| 378 | O lenhador leva o galho solto e a copa (autor 26-11, notas do jogo da 0.4.145, escolha J5… | cita | LumberjackGameTest, TreeLeftoversGameTest |
| 379 | A água do curral grande fora da reta dos portões (autor 26-11, nota do jogo da 0.4.145, J… | cita | test_large_animal_pen |
| 380 | A vila nasce com animais e não abate antes de 6 (autor 26-11, notas do jogo da 0.4.145, e… | cita | FlockHarvestTest, FoundationAnimalsGameTest |
| 381 | Areia e cacto aparecem sob pedido (autor 26-11, K1 do jogo da 0.4.148: "areia e cacto dev… | cita | ClayOnRequestGameTest |
| 382 | A casa do fazendeiro perto da plantação grande (autor 26-11, K2 do jogo da 0.4.148: "adic… | cita | FarmerHomeTest |
| 383 | O ofício não troca, e a troca é dita (autor 26-11, K3 🅲 do jogo da 0.4.148: "nenhum aldeã… | cita | ProfessionLockTest, ProfessionShunTest, WorkerTest |
| 384 | O diário dos blocos (autor 26-11, K3 do jogo da 0.4.148: "uma função para registrar cada… | cita | BlockJournalTest, test_block_report |
| 385 | Muda com espaço (autor 26-11, K4 🅱️ do jogo da 0.4.148: "rebentos não devem ser plantados… | cita | NurserySkyGameTest |
| 386 | O caminho da mina à casa do mineiro (nota do jogo da 0.4.148, K5 🅱️: "o caminho do arco d… | cita | MineHomeWayGameTest, MineHomeWayTest |
