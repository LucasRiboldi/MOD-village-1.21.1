# Mapa do sistema — Village Colony

**Data:** 2026-10-06 · **Snapshot:** `main` `6022021e` · parte da
[auditoria de recuperação](PROJECT-RECOVERY-AUDIT.md).
Caminhos relativos a `src/main/java/com/villagecolony/` salvo indicação.
Linhas são aproximadas onde marcadas com `~`.

## 1. Tamanho

| Área | Arquivos | Linhas |
|---|---:|---:|
| `src/main/java` | 375 | 66.214 |
| `src/test/java` | 162 | 25.909 |
| `src/gametest/java` | 94 | 34.035 |
| Markdown (sem build/run/worktrees) | ≈400 | — |

Maior arquivo de produção: `StructureBlueprintReader` (568 linhas). As linhas
são dominadas por javadoc; em código efetivo os maiores são
`SurfaceGatheringWork` (321), `PenEscape` (274), `ClimbOut` (273).
**O tamanho de classe não é o problema; o acoplamento é** (§5).

Pacotes de produção mais povoados: `fabric/work` 104, `fabric/integration` 79,
`core/coordination` 21, `core/construction/model` 21, `fabric/event` 17.

## 2. Pontos de entrada

| Item | Onde | O que faz |
|---|---|---|
| `main` | `VillageColonyMod.java:183-195` | registra handlers, payload de overlay, comando, rede |
| bloco `static` | `VillageColonyMod.java:165-170` | ganchos `BUILDINGS.whenRegistered`, `CONSTRUCTIONS.whenOpened` (caixa da vila cresce) |
| `client` | `client/VillageColonyClient.java` | política no cliente, receptor de overlay, 2 renderizadores |
| Mod Menu | `client/modmenu/VillageColonyModMenu.java:7` | telas de política de profissão (opcional, ADR-030) |
| Mixins (2) | `fabric/mixin/VillagerEntityMixin.java:29` (`@Inject initBrain TAIL`), `VillagerDataMixin.java:25` (`@ModifyVariable setVillagerData`) | instala a tarefa de ir ao trabalho no Brain; filtra ofício Vanilla. **Sem `@Overwrite`, `@Redirect`, `@Accessor`.** |
| Eventos servidor | `VillageDetectionHandler.java:136-137`; `PlayerWorldChangeHandler.java:49-55`; `ServerLifecycleHandler.java:43-44`; `VillagerLifecycleHandler.java:46-47`; `integration/VillageChests.java:124-125`; `integration/SiteSignJanitor.java:78-79`; `overlay/OverlaySync.java:23` | |
| Comando | `fabric/command/VillageLogCommand.java:31-37` | só `/vc log` |
| Rede | `fabric/network/ProfessionPolicyNetworking.java:20-28` | S2C política e overlay; C2S pedido e atualização de política |
| Registries | nenhum `Registry.register` | o mod não adiciona item, bloco nem entidade |
| Persistência | `data/save/ColonySavedData.java:173`, `WorkMarksSavedData.java:88`, `ProfessionPolicySavedData.java:38` | 3 `PersistentState` |
| Config | não há arquivo | só a `ProfessionPolicy` (Mod Menu); tudo o mais é constante |
| Recursos | `resources/data/villagecolony/structure/houses/*.nbt`, `catalog/vanilla_structures.json` | |

## 3. O ciclo da colônia (real)

Só roda em `END_SERVER_TICK` e só com jogador no Overworld
(`VillageDetectionHandler.java:311-316`). Por colônia, só com jogador dentro da
caixa da vila ou há ≤ 5 min (`Colony.ATTENTION_TICKS = 6000`, `Colony.java:202`).

```text
END_SERVER_TICK → VillageDetectionHandler.onServerTick (~:311)
 ├─ cada 20 ticks: VillageFocus.attend
 ├─ TODO tick (tickActiveServer ~:344):
 │    drainOnePending           (fila de camas do CHUNK_LOAD, ≤256, 1 por tick)
 │    Miner/Smelter/SurfaceGathering/Shepherd/Farmer/Lumberjack/Crafting/BuilderWork.tick
 │    StrandedEscape.tick → PenEscape, ClimbOut, MineReturn.record, WorkTime.sample
 │    VillageMeals.tick (20t) · SiteMarker.tick (20t)
 │    tickCounter < 600 ? fim
 │  ══ CICLO LONGO (600 ticks = 30 s, VillageDetector.CYCLE_TICKS :57) ══
 │    [DETECT]    VillageAdoption.detectAround (cada jogador)
 │    [LIFECYCLE] VillageAdoption.updateLifecycles
 │    [DETECT]    detectFromColonyCenters + ColonyMergeTrigger
 │    ColonyCycleRunner.runColonyCycles (~:131)
 │      PlannerTurns.chooseFrom  (quem planeja neste ciclo: 1..8 colônias)
 │      para cada colônia ativa e vigiada: runCycleOf (~:191)
 │        [POPULATION] VillageForest.plantForPopulation
 │        [CHESTS]     ColonyChests.nearestFirst → ColonyChestSurvey.advance (8 baús/ciclo)
 │        [PLANNER]    SweepDeadline.within(15 ms, ConstructionPlanner.plan)  ← único com prazo
 │                     ConstructionDemand · ChestRelief · WorkMaterials → WorkDemand
 │        [ASSIGN]     core ColonyCycle.run :135
 │                       ResourceDemand.deficit → cancelSatisfied → requestMissing
 │                       → TaskService.create → WorkAssignment.assign
 │        [WORKERS]    runOngoingWork (~:416): *Work.run, WaitingWork, CraftingWork.run
 │      PlannerTurns.observeCost · purgeClosed · purgeFinished
 │    reportIfSlow: loga só se ≥ 50 ms (TICK_MILLIS :112, :443)
 └─ cada 20 ticks: IdleHands.assignNow
```

A **contratação não está no ciclo**: acontece na adoção/registro
(`VillageAdoption` → `VillagerRegistration.registerVillagers` →
`ProfessionAssigner.assignMissing` `VillagerRegistration.java:151`,
`VacancyEnforcer` `:241`).

**Movimento:** Brain Vanilla + `fabric/brain/GoToWorkTargetTask` (instalada pelo
mixin) via `WalkTarget`. O mod **não chama `findPathTo`** em lugar nenhum; fuga
usa `getMoveControl().moveTo` direto (`ClimbOut:430`, `DetourWalker:215`,
`PenMoves:137`).

## 4. Inventário de sistemas

Complexidade: A alta, M média, B baixa. Estado: ativo salvo indicação.

| Sistema | Classes principais | O que faz | Depende de | Testes principais | Cx |
|---|---|---|---|---|---|
| Detecção / caixa da vila | `core/colony/service/VillageDetector:26`, `integration/VillageScanner:29`, `event/VillageAdoption:86`, `ColonyMergeTrigger:47`, `core/colony/model/VillageBounds` | camas (POI HOME, raio 64, ≥3 camas, ≥2 aldeões) → adota, funde, abandona | ColonyService, POI | VillageDetectorTest, ColonyDetectionGameTest | A |
| Fundação | `integration/VillageFoundation:43`, `BigHouseFoundation`, `ChestPlacer:31`, `VanillaBedChests` | baú por cama, BigHouseMOD colocada de uma vez | Registration | VillageFoundationGameTest | M |
| População / contratação | `event/VillagerRegistration:85`, `core/worker/service/{ProfessionAssigner:35, HiringQuota:23, VacancyEnforcer:26}` | fundação (7 titulares), demanda, crescimento em lotes de 15 adultos | WORKERS, STORAGES | ProfessionAssignerTest (27) | A |
| Profissões e política | `core/worker/model/ProfessionType:13-60`, `ProfessionRegistry:35-50`, `network/*`, `client/modmenu/*`, `integration/VanillaProfessionGuard` | 8 profissões, ferramenta, capacidades, política do jogador | SavedData | — | M |
| Tarefas / coordenação | `core/coordination/{ColonyCycle:40, ColonyGoals, WorkAssignment:38}`, `core/task/service/TaskService:36`, `event/{IdleHands, PlannerTurns, VillageFocus}` | metas → pedidos → reserva | core puro | ColonyGoalsTest (54), ColonyCycleTest, WorkAssignmentTest | A |
| Recursos / baús | `integration/{ColonyChestSurvey:28, ChestDepositor, ChestWithdrawer, ColonyChests, VillageChests, ColonySupply}`, `core/storage/*`, `work/ChestRelief` | leitura fatiada dos baús, depósito, retirada | BlockEntity | StorageGameTest | A |
| Fabricação / fundição | `work/{CraftingWork:73, SmelterWork:61, SmelterFallback}`, `integration/{CraftingLookup, FurnaceReach}` | receitas Vanilla, cadeia recursiva | RecipeManager | CraftingGameTest, SmelterGameTest | A |
| Blueprints | `integration/StructureBlueprintReader`, `work/HousePlans`, `integration/{VillageStructures, ColonyModels}` | lê NBT próprio e catálogo Vanilla | resources | HousePlansTest | A |
| Lote / planejador | `work/ConstructionPlanner:79` (`plan` :270), `integration/{BuildSiteScanner:92, LotLevel, LotGround, LotClearance}`, `work/{PlanOrdering, SiteOpening}` | retomar → reparo → plano → rua → lote | `SweepDeadline` | BuildSiteGameTest (49) | A |
| Construtor / reparo | `work/{BuilderWork:82, BuilderPlacement, BuilderMaterials, WaitingWork, ConstructionResume, BuildingRepairPlanner:36, TestBarrier:48}` | coloca blocos, espera, desiste | CONSTRUCTIONS | BuilderGameTest (31), ConstructionResumeGameTest | A |
| Ruas | `integration/{RoadExtension:52, RoadPaving, RoadIndex, RoadsideSites}`, `work/RoadGrowthPlanning` | rua cresce com a vila | Planner | RoadExtensionGameTest | A |
| Mineração | ≈30 classes `work/Mine*`/`Miner*`, `integration/{MineFlooding, MineLighting, OreVein}`, `core/construction/{Mine, MineRegistry}` | poço em escada, galerias, veios | MINES | MinerGameTest (85), MineRegistryTest | A |
| Agricultura | `work/{FarmerWork:65, FarmerBakery, FarmPlans}`, `integration/CropPatch` | colhe, replanta, pão | — | FarmerGameTest, FarmPlanGameTest | M |
| Lenhador | `work/{LumberjackWork:77, TreeChoice, TreeFelling, LumberjackNursery}`, `integration/{TreeScanner, VillageForest}` | corta, replanta, viveiro | — | LumberjackGameTest (44) | A |
| Pastor | `work/{ShepherdWork:58, ShepherdFlock}` | tosquia, procria | — | — | M |
| Coleta de superfície | `work/{SurfaceGatheringWork, SandGathering, EmptySweeps}` | areia, argila, terra | RingSweep | SurfaceGatheringGameTest | A |
| Movimento / encalhe | `work/{StrandedEscape:56, ClimbOut:65, PenEscape:52, MineReturn:58, DetourWalker:55}`, `core/movement/DetourPlanner`, `brain/GoToWorkTargetTask:31` | cava, sobe, sai do curral, refaz rastro | Brain | StrandedEscapeGameTest, DetourPlannerTest | A |
| Telemetria | `work/WorkTime:45` (`VC_TIME` :162), `event/CycleCost`, `integration/SweepLog`, `scripts/time_ledger.py`, `scripts/analyze_village_log.py` | tempo dos aldeões, custo do ciclo | — | CycleCostTest | M |
| Overlays | `overlay/OverlaySync`, `client/*OverlayRenderer` | painéis de profissão/obra | rede | — | M |
| Persistência | `data/save/*` | ver §7 | — | data/save (75) | M |
| Skills / níveis / XP | — | **NÃO EXISTE** (grep `skill|experience|xp|levelUp` vazio) | — | — | — |

## 5. Dependências

**Direção (CONFIRMADO):** `core/` não importa `net.minecraft`, `net.fabricmc`,
`com.villagecolony.fabric` nem `data`. `data/` não importa `fabric`. Regras
ArchUnit em `src/test/java/com/villagecolony/architecture/ArchitectureRulesTest.java`:

| Regra | Linha | Modo |
|---|---|---|
| core não toca Minecraft/Fabric/fabric.. | :60 | estrita |
| `fabric.(*)` sem ciclo de pacote | :77 | **congelada — 400 linhas, 10 ciclos** (`archunit_store/bd1afc07…`) |
| `core.(*)` sem ciclo | :87 | estrita |
| mixin só delega | :94 | estrita |
| estático mutável registra `ServerMemory` | :118 | **congelada — 10 classes** (`archunit_store/9f6005cc…`) |

`archunit.properties`: `allowStoreUpdate=true`, `refreeze=false` — violação
nova **reprova**; só a consertada sai do registro. O risco de "aceitar
regressão sem aviso" levantado numa frente desta auditoria foi **refutado**.

**Acoplamento** (imports internos): `ColonyCycleRunner` 66,
`VillageDetectionHandler` 56, `VillageAdoption` 56, `VillagerRegistration` 55,
`ConstructionPlanner` 30. As quatro primeiras herdaram o bloco de imports de
uma classe antiga dividida: 33/88, 43/77, 54/77 e 47/76 imports sem uso
(contagem mínima).

```text
              VillageColonyMod (8 singletons: COLONIES WORKERS STORAGES TASKS
                                CONSTRUCTIONS BUILDINGS MINES ACTIVITY_TRACES)
                     ▲ usados por quase todo fabric/*
fabric/event ──► fabric/work ◄──► fabric/integration ◄──► fabric/adapter   (ciclos congelados)
     │                 │                   │
     └─────────────────┴──────► core/ (puro, sem ciclo) ◄── data/save
```

**Estado global:** 8 singletons em `VillageColonyMod.java:69-161`; ≈240
coleções estáticas; 79 arquivos chamam `ServerMemory.register`;
`ServerMemory.resetAll` em `ServerLifecycleHandler.java:66,264`. Estado
estático também em `VillageDetectionHandler.java:120-130` (`pending`,
`tickCounter`).

**Classes que sabem demais:**

| Classe | Mistura |
|---|---|
| `ColonyCycleRunner` | orquestra + lê baús + monta `WorkDemand` com 10 consultas ao mundo (~:263-293) + prazo + regra de reserva BUILD (`canReserveTask` ~:427) + log + estado estático |
| `VillageAdoption` / `VillagerRegistration` / `VillageDetectionHandler` | detecção + adoção + abandono + ciclo de vida + avisos; registro + contratação + perda de vaga + log de baús |
| `ConstructionPlanner.plan` | planeja **e** dispara efeitos no mundo (desistência, reparo, rua) |
| `WaitingWork` | pedido ao artesão + acordar obra + decisão de desistir + log |
| `HousePlans` | catálogo + prioridade + paleta + contagem de camas + 5 caches estáticos |

## 6. Performance

**Gancho único** de tick (ver §3). Medidas existentes:

| Fonte | Data | Resultado |
|---|---|---|
| Spark `wIEM9zz90l` | 25-09 | TPS 20, MSPT mediano 13-15 ms; mod 4,1% dos ticks lentos; topo `ColonyChests.nearestFirst` |
| Spark `1HTwcUdP2F` | 25-09 | mod 4,1% → 0,3% após correção |
| Spark `KbkZcE27Uu` | 27-09 | TPS 20, MSPT p95 12,8 ms; `TreeChoice` 10,6%, `FarmerNursery` 9,5% |
| Spark 21h | 30-09 | `TreeScanner.findNearestLog` = **73% do custo do mod** (`docs/technical/Identidade-da-Vila-2026-09-30.md:164`) |
| Spark `jPsGP2hsPo`, `LhqqBh973A` | 01-10, 02-10 | TPS 20, mod 1,7% |
| `PlanningBudget.java:7-12` | 24-09 | 196 ciclos > 50 ms em 6,7 h; planejador 91%, média 255 ms, máx 554 ms (antes do prazo de 15 ms) |

**Orçamentos:** só o planejador tem prazo (15 ms por chamada,
`PlanningBudget.DEADLINE_MS`) e cota adaptativa (1–8 colônias, alvo 20 ms).
População, baús, `ColonyCycle.run`, `CraftingWork.run`, `WaitingWork` e
`runOngoingWork` rodam para **todas** as colônias vigiadas, sem prazo.

**Varreduras:**

| Varredura | Raio / volume | Limite |
|---|---|---|
| Lote (`BuildSiteScanner`) | anéis ≥ 64 | 1024 colunas ou prazo |
| Árvore (`TreeScanner`) | raio 64, índice 256 troncos | 1024 colunas, **sem prazo de relógio** |
| Água/areia (`SandNearWater`) | raio 48 (~9.400 colunas × 2) | recheck 6.000 ticks |
| Camas (`VillageScanner.collectBeds`) | POI raio 64 | por chunk 1/tick |
| População (`VillagerScanner`) | `getEntitiesByClass` 128×128 | por ciclo por colônia, sem limite |
| Baús | 8 por ciclo; cache 20 ticks | ok |

**Top 5 suspeitos (e a medida que decide):**

1. Lenhador sem árvore (`TreeScanner`/`TreeChoice`/`FarmerNursery`) — Spark com
   lenhador numa vila sem floresta; confirma se > 30% do mod.
2. Ciclo sem prazo fora do planejador — breakdown `CycleCost` por fase no log
   `Colony cycle took`; confirma se a fase maior não for `planner`.
3. `getEntitiesByClass` 128×128 por colônia — fase `population` no breakdown.
4. Planejador com muitas colônias — `Planner turns` preso em 1-2 com
   `last cycle spent > 20 ms`.
5. Pathfinding Vanilla de encalhados — Spark `--only-ticks-over 50` em
   `PathNavigation`; `time_ledger.py` com bloqueio+encalhe > 10%.

**Lacunas de medida:** não há custo por colônia nem por `*Work.tick`; o log do
ciclo só aparece ≥ 50 ms; os perfis de 30-09 em diante só registram TPS e % do
mod. **Não otimize nada antes de preencher estas lacunas.**

## 7. Persistência

| Arquivo | Conteúdo | Quando grava |
|---|---|---|
| `villagecolony_colonies` (`ColonySavedData`) | colônias, workers, projetos (planta, origem, estado, peças adiadas), buildings, minas, índice de ruas, cursores de varredura, traços | **só em `SERVER_STOPPING`** (`ServerLifecycleHandler.java:44,205`) |
| `villagecolony_marks` (`WorkMarksSavedData`) | recusas da mina, tentativas de suprimento, rastros, baús de trabalhador | **só em `SERVER_STOPPING`** (`:216-231`) |
| `ProfessionPolicySavedData` | política do Mod Menu | a cada mudança (`markDirty` `:47`) |

**Não salvo:** tarefas (`TaskService`), descanso/desistências do aldeão
(`Worker.java:111-122`), encalhe, caches de baú. **Recomposto do mundo:**
blocos já colocados da obra, baús da vila, camas, ofício Vanilla.

`sync` é o único caminho entre o registro em memória e o `PersistentState`, e
só é chamado no `SERVER_STOPPING` (grep: único chamador). O autosave do
Minecraft grava o `PersistentState` — mas ele só contém o que foi copiado no
último fechamento. Ver P0-02 no ROADMAP.

## 8. Código morto e restos

| Item | Evidência | Estado |
|---|---|---|
| `core/coordination/ColonyFocus` | 0 referências em `src/main` fora de si; papel feito por `fabric/event/VillageFocus` | morto em produção |
| `fabric/work/PlanAffordability` | 0 referências em `src/main` | morto em produção |
| `fabric/work/MinerProbe`, `EnduranceReport` | só testes usam | utilitário de teste em `src/main` |
| Imports `ServerTickEvents`/`ServerChunkEvents` | sem uso em `ColonyCycleRunner:69-70`, `VillageAdoption:59-60`, `VillagerRegistration:58-59` | resto da divisão de classe |
| `TODO`/`FIXME`/`@Deprecated` | nenhum marcador real em produção | ok |

NÃO CONFIRMADO: classes citadas só em `{@link}` contam como referência no
grep, então pode haver mais código morto.
