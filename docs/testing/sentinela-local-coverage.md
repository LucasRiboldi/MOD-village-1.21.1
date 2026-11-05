# Cobertura do Sentinela Local

Mapa manual entre regra, prova automatizada e menor perfil do Sentinela Local.
`scripts/test_lab.py` recusa executar quando uma linha não aponta teste e perfil.
Ele não agenda testes e não altera CI. O cruzamento de todas as regras com os testes
está em [`regras-e-testes.md`](regras-e-testes.md).

| funcionalidade | regra | teste | perfil mínimo |
| --- | --- | --- | --- |
| Progresso de tarefa | Aldeão não insiste mais de 5 min sem ação útil; libera tarefa e tenta suprimento uma vez | `DemandRetriesTest`, `IdleWatchGameTest.usefulProgressResetsWalkingDeadline` | fast |
| Relatório de ações | Tarefa aberta informa idade e tempo sem progresso pelo diário existente | `ActionReportTest.test_open_task_reports_elapsed_and_time_without_progress` | quick |
| Validação de cobertura | Toda mudança funcional declara teste e perfil mínimo | `CoverageManifestTest.test_requires_a_test_and_minimum_profile_for_each_feature` | quick |
| Reserva ociosa do fundidor | Sem obra, somente lingotes de minério ficam em reserva até 64 | `SmelterAdvanceGameTest.anIdleSmelterKeepsAvailableIngotsAt64` | fast |
| Frente autônoma da mina | Sem demanda de obra, mineiro produz no máximo 64 pedras por dia; escadaria e frente continuam protegidas pelos GameTests de mina | `MineSupplyTest`, `MinerGameTest.theMinerDoesNotDigThePlayersStaircase` | fast |
| Profissões fixas | Falta de baú ou tarefa travada não remove, troca ou desvia profissão; só construtor inicia obra ou caminho | `ProfessionAssignerTest.aChestlessWorkerKeepsTheJobWhenSomeoneElseCanGetAChest`, `ProfessionFloorTest.dismissalNeverRemovesAProfession`, `HelperReachGameTest.onlyTheBuilderCanTakeOrStartTheBuild`, `BuilderPavingGameTest.anIdleCarpenterDoesNotPaveTheRoad`, `PathPavingTest.onlyTheBuilderPavesPathLinks` | fast |
| Curral grande | Cada vila nova recebe no máximo um curral grande com capacidade 20, água, 12 cultivos, grama e tochas; cercados legados continuam utilizáveis | `PenPlansTest.onlyTheLargePenBlocksAnotherLargePen`, `PenModelGameTest.theLargePenHasWaterCropsGrassGateAndPostTorches`, `ShepherdPensGameTest.aRegisteredLargePenHoldsTwentyAnimals` | fast |
| Casa do mineiro | Vila nova planeja uma moradia junto à boca da mina, sem duplicar uma casa próxima | `MineHomePlansTest.recognizesOnlyAHouseNearTheMineMouth` | fast |
| Grafo de ruas | Vila nova usa somente colunas fisicamente revalidadas no RoadGraph; vila antiga continua no índice legado | `RoadGraphMigrationGameTest`, `RoadGraphActivationTest.exposesEveryConfirmedGraphColumn` | fast |
