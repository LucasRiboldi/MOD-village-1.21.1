# Reconciliação de regras — Village Colony

**Data:** 2026-10-06 · **Snapshot:** `main` `6022021e` · parte da
[auditoria de recuperação](PROJECT-RECOVERY-AUDIT.md).

Três camadas, nunca misturadas:

- **Intenção** — `PROJECT_CONSTITUTION.md`, `docs/RULES.md`, `docs/decisions/ADR-*`.
- **Implementação** — `src/main/java/com/villagecolony/` (abreviado `…/`).
- **Evidência** — teste que comprova; playtest quando o teste não basta.

Histórico (Development-Log, Project-State, Historico-*) **não** foi usado como
intenção.

## 1. Profissões

Enum `…/core/worker/model/ProfessionType.java:13-60`. Ferramentas em
`…/core/worker/service/ProfessionRegistry.java:35-50`.

| Profissão | Ferramenta | Faz | Classe |
|---|---|---|---|
| Lenhador | machado de ferro | madeira, replantio, viveiro | `LumberjackWork`, `TreeChoice`, `LumberjackNursery` |
| Minerador | picareta de ferro | pedra/minério, mina em escada, para em 256 por tipo | `MinerWork`, `MinerHaul.TYPE_CAP` `:29` |
| Pastor | tesoura | lã, procriação do rebanho | `ShepherdWork`, `ShepherdFlock:40` |
| Fundidor | pá de ferro com toque suave | fornalha, coleta de superfície | `SmelterWork`, `SurfaceGatheringWork` |
| Carpinteiro | — | peças de madeira | `CraftingWork` |
| Pedreiro | — | peças de pedra | `CraftingWork` |
| Agricultor | enxada de ferro | comida, terra, pão com trigo > 32 | `FarmerWork`, `FarmerBakery:26` |
| Construtor | — | coloca a obra | `BuilderWork` |

Aposentadas: `BREEDER`, `MANUFACTURER` — save antigo volta `null` e o aldeão é
recontratado (`ProfessionType.java:36-44`, ADR-021).

**Contratação** (`ProfessionAssigner.java:188-300`): (1) fundação
`FOUNDATION_ORDER` `:77-84` — 7 titulares, agricultor fora; (2) demanda
`HiringQuota.demandedVacancy` `:34` (Regra 38); (3) crescimento `GROWTH_ORDER`
`:54-62`, cota em `HiringQuota.targetCount` `:78-85` (até 8 com < 15 adultos;
depois lotes de 8 a cada 15); (4) política do jogador (ADR-030).

**Troca de profissão:** 3 desistências da mesma capacidade em 12 ciclos →
`giveUpProfession` (`Worker.java:143,153,370-374`); 4 ciclos sem ofício; evita o
ofício largado 8 ciclos, dobrando até 64. `VacancyEnforcer:31-55` tira a vaga
de quem não tem baú se há candidato com baú. Queda de população não demite.

**Ofício Vanilla:** removido de quem tem ofício do mod
(`VanillaProfessionGuard.filter:40` via `VillagerDataMixin:25`).

## 2. Ciclo de uma tarefa

```text
ColonyCycle.run (a cada 600 ticks)
  ResourceDemand.deficit ─► cancela pedido sem motivo ─► requestMissing (1 pedido por mão capaz)
  TaskState: AVAILABLE → RESERVED → EXECUTING → COMPLETED | CANCELLED
  Prioridade: SURVIVAL 10 < CONSTRUCTION_MATERIAL 20 < PRODUCTION 30 < CONSTRUCTION 40
  WorkAssignment.assign :121-145 → takeOneTask :256-289
  WorkEligibility.canReserve :37-39  (bloqueia: encalhado, descansando, sem baú)
  Entre ciclos: IdleHands → assignWithoutAClock :159
```

**Falha:** `WorkStall.LIMIT = 300` ticks parado no expediente
(`WorkStall.java:58`); `STALL_LIMIT = 2400` em `BuilderWork:103`,
`CraftingWork:111` → `WorkerStrikes.gaveUp:53` → `Worker.rest` (4 ciclos, conta
desistência). 2 congelamentos no mesmo ponto → encalhado
(`StrandedWorkers.java:44`). Obra sem material: `PatienceClock.CYCLES = 20`
(10 min) e abandono. Expediente: `WorkClock.isWorkTime` antes de `DUSK = 11000`;
chuva não pausa (Regra 18-e1).

## 3. Recursos

- Meta = guardado + espaço livre (`ColonyGoals.java:20-33`). Pisos: pedra 64
  `:90`, fundido 16 `:112`, mineral 64 `:113`, comida 64 `:127`, 8 de comida por
  cama `:149`.
- Madeira: metade fica em tora; obra pode converter além disso
  (`StockRules:82,110`, Regra 5-e1).
- Baús (Regra 45): `ColonyChests.nearestFirst:70-120` — fora da caixa = fora de
  alcance; exclui os 7 da BigHouseMOD (`VillageChests:174`), baús nomeados
  (`VillageChestRule.mayTake:57`) e baú privado de cama Vanilla.
- Cadeia recursiva: `CraftingLookup.billFor:174`, `ColonySupply`; substitutos
  em `ResourceSubstitution:126,151` e `MaterialChoice`.
- Falta material: obra em `WAITING_RESOURCES`; drops e corantes aparecem
  (`DropIngredients:26-42`); peça sem rota no bioma vai ao baú na 3ª tentativa
  (`BiomeConstructionSupply`, ADR-022); `TestBarrier` risca o que deixa passar.

## 4. Construção

- Ordem: `ConstructionPriority.decide:34-54` — déficit de moradia → oficina
  (Regra 49) → primeira casa → rodízio.
- Oficinas: `ConstructionOrder.WORKSHOPS:46-58`.
- Planta: decrescente por tamanho, primeira casa pela menor
  (`PlanOrdering.smallestFirst:77-90`); sorteio entre as que cabem
  (`SiteOpening:211`).
- Lote (Emenda 7): raio = ½ diagonal da caixa + 12, mínimo 64
  (`ConstructionPlanner:130-143`).
- Proteção: `BlockProtection.mayBreak:59` (jogador, colônia, vila original);
  exceções `mayDigOut:79`, `mayBuildOver:94`.
- Progresso salvo: origem, estado, peças adiadas; blocos relidos do mundo.
- Reparo: `BuildingRepairPlanner` só retoma obra abandonada.
- Cancelamento: tocha de alma no canteiro (`ConstructionCancellation:39`).

## 5. População

Um adulto por cama da BigHouseMOD, uma vez, na fundação
(`VillageFoundation.java:74`). Depois, procriação Vanilla com 12 de comida no
fim do expediente se sobrar cama (`VillageMeals`). **Sem teto** além das camas.

## 6. Detecção, caixa e foco

`VillageDetector:29-41`: ≥3 camas, ≥2 aldeões, raio 64, cluster 32.
`VillageBounds:31-46`: lado típico 144, margem 12. Foco: 5 min após o jogador
sair (`Colony.java:202,251`). `VillageFocus.isWorking:144` devolve `true` sem
jogador — existe para os GameTests.

## 7. Movimentação

`GoToWorkTargetTask` velocidade 0,5, `MAX_RUN_TIME 24000`; folga de chegada 2
(`WorkTargets:52`); desvio `DetourPlanner` raio 16, 2500 nós;
`DetourWalker.MAX_REPLANS = 3`; fuga `StrandedEscape`, `ClimbOut`, `PenEscape`,
`MineReturn`.

## 8. Persistência

Ver SYSTEM-MAP §7. Ponto crítico: o registro só é copiado no `SERVER_STOPPING`.

---

## 9. Matriz de reconciliação

Estados: CONFIRMADA · IMPLEMENTADA MAS NÃO COMPROVADA · PARCIAL · CONTRADITÓRIA
· DESATUALIZADA · NÃO IMPLEMENTADA · DUPLICADA · SEM EVIDÊNCIA.
"Playtest?" = o comportamento depende de jogo real para ser comprovado.

| # | Regra | Documentada | Implementada | Teste | Playtest? | Estado |
|---|---|---|---|---|---|---|
| 1 | Colher até os baús encherem | RULES:19 | `ColonyGoals:20-33` | ColonyGoalsTest | não | CONFIRMADA |
| 2 | Velocidade de ferro; troca pela melhor do baú | RULES:20-21 | `ActionTool:71`, `ToolUpgrade:94` | WorkerEquipmentGameTest | não | CONFIRMADA (javadoc `WorkerEquipment:36-41` desatualizado) |
| 3 | Nunca destruir vila/colônia/jogador | RULES:22, Constituição:118-132 | `BlockProtection:59` + exceções `:79,:94` | BlockProtection tests | sim | PARCIAL |
| 4 | Dois por profissão | RULES:24 | — (substituída por `HiringQuota`) | — | — | DESATUALIZADA (doc admite) |
| 5 | Tábua até metade; obra usa tudo | RULES:25-26 | `StockRules:82-110` | StockRules tests | não | CONFIRMADA |
| 6/15/17 | Rua primeiro, cresce, lote lateral | RULES:27,36,38; Constituição:259-270 | `RoadExtension`, `SiteOpening:211` | RoadExtensionGameTest | sim | CONFIRMADA |
| 8 | Baú ao lado de cada cama | RULES:29 ("só trabalhador") | `ChestPlacer:81`, `VanillaBedChests.ensure` | ChestPlacerGameTest | não | CONTRADITÓRIA |
| 11 | Uma de cada profissão | RULES:32 | `FOUNDATION_ORDER`, `GROWTH_ORDER` | ProfessionAssignerTest | não | PARCIAL (agricultor fora da fundação) |
| 18 | Expediente; chuva não pausa | RULES:39-40 | `WorkClock:33`, `WorkHours:46` | WorkHours tests | não | CONFIRMADA |
| 19 | Lote no nível da rua | RULES:41 | `LotLevel:114` | BuildSiteGameTest | sim | CONFIRMADA (ponteiro do doc errado) |
| 21/32 | Casa com cama, baú, lampião; mobília depois | RULES:43,58 ("✅ BuilderWork.furnish") | **não existe**; `TestBarrier:36`, `ColonyCycleRunner:~327`: "a Regra 21 morreu" | — | — | CONTRADITÓRIA |
| 25 | Maior planta que couber | RULES:47,116 ("inerte") | `PlanOrdering:56-90` ("continua valendo") | HousePlansTest | não | CONTRADITÓRIA |
| 27 / 27-e3 | Só catálogo do jogo; modelos próprios primeiro | RULES:49-52; Constituição:146-152 | `VillageStructures`, `ColonyModels:72` | StructureCoverageGameTest | não | CONTRADITÓRIA com a Constituição |
| 28 | Barreira de teste | RULES:53,87 | `TestBarrier` (produção) | BuilderGameTest | sim | CONFIRMADA (provisória, nome enganoso) |
| 29/30 | Mina; piso 64 de carvão e ferro | RULES:54-56 | `MineShaft`, `ColonyGoals:113` | MinerGameTest | sim | CONFIRMADA (javadoc `ColonyGoals:55-61` "ninguém minera" velho) |
| 34 | Lava nunca automática | RULES:60 | `BlockShaping:337` | — | não | IMPLEMENTADA MAS NÃO COMPROVADA |
| 35 | Carpinteiro titular | RULES:61 | `ProfessionAssigner:64-84` | ProfessionAssignerTest | não | CONTRADITÓRIA com ADR-021:17 e ADR-030:24-26 |
| 36 | Viveiro é do lenhador | RULES:62; ADR-020 §4 (agricultor e lenhador) | `LumberjackNursery` | LumberjackGameTest | sim | CONTRADITÓRIA com ADR-020 |
| 38 | Contratar pela demanda primeiro | RULES:64 | `HiringQuota:34`, `ProfessionAssigner:266-276` | ProfessionAssignerTest | não | CONFIRMADA |
| 39 | Pão acima de 32 trigos | RULES:65 | `FarmerBakery:26` | FarmerGameTest | não | CONFIRMADA |
| 41 | Drops automáticos | RULES:67, ADR-028 | `DropIngredients:26-42` | — | não | CONFIRMADA (contradiz Constituição §9) |
| 42 | Mineiro para em 256 por tipo | RULES:68 | `MinerHaul:29` | MinerGameTest | não | CONFIRMADA |
| 44 | Ofício do mod exclui o Vanilla | RULES:70, ADR-029:16 | `VanillaProfessionGuard:40-53` | — | sim | CONTRADITÓRIA com ADR-011:21,35 e Constituição §4 |
| 45 | Baús da colônia = baús da caixa | RULES:77 | `ColonyChests:70-120`, `VillageChests:151-200` | StorageGameTest | não | PARCIAL (exclusões não escritas na regra) |
| 48 | Recolher do chão só peça da obra | RULES:74 | `GroundPickup:34-78` | GroundPickup GameTest | sim | CONFIRMADA |
| 49 | Ordem das obras | RULES:75 | `ConstructionPriority:34-54`, `ConstructionOrder:46-58` | — | sim | IMPLEMENTADA MAS NÃO COMPROVADA |
| 50 | Medir tempo dos aldeões | RULES:76 | `WorkTime:40,162` | — | sim | CONFIRMADA |
| E6 | Caixa da vila, foco 5 min | ADR-003:699-744 | `VillageBounds:31-46`, `Colony:202,251` | VillageFocusTest | sim | CONFIRMADA |
| E7 | Raio ½ diagonal + 12, mín 64 | ADR-003:746-770 | `ConstructionPlanner:130-143` | — | sim | CONFIRMADA |
| E8 | Medida da vila pelas peças | só na branch `claude/corrigiveis-sem-jogo` | não na `main` | — | sim | NÃO IMPLEMENTADA na `main` |
| ADR-010 | Descanso vale em toda reserva | ADR-010:317-330 | `WorkEligibility:37-39` | WorkAssignmentTest | não | CONFIRMADA (RULES:134 "a decidir" está velho) |
| ADR-011 | Lotes de 15; construtor no crescimento | ADR-011:39-41,78-89 | `HiringQuota:83-85` | ProfessionAssignerTest | não | CONFIRMADA pela emenda (§2.3 `:60` velho) |
| ADR-030 | Política, ordem, teto, raio | ADR-030:13-45 | `ProfessionAssigner:237,264`, `HiringQuota:74` | — | não | PARCIAL / CONTRADITÓRIA |
| ADR-020 §1 | Reparo de construções registradas | ADR-020:20-23 | `BuildingRepairPlanner` | — | sim | PARCIAL |
| Const. §2 | Aldeão não decide estratégia | Constituição:59-71 | `ColonyCycle`, `WorkAssignment` | ColonyCycleTest | não | CONFIRMADA |
| Const. §9 | Nada surge sem origem | Constituição:166-174 | ver conflito C-01 | — | — | CONTRADITÓRIA |
| — | Skills, níveis, XP | não documentada como existente | — | — | — | NÃO EXISTE |

**Duplicadas no código:** cálculo de `searchRadius` com política em
`FarmerWork:169-173`, `ShepherdWork`, `TreeChoice:102-106`; `speedOf` em
`ActionTool:71` e `ToolUpgrade:194`.

## 10. Conflitos (decisão do autor necessária)

Formato: **Fonte A | Fonte B | Código | Decisão necessária.** Nenhum foi
resolvido nesta auditoria.

| ID | Fonte A | Fonte B | Código | Decisão |
|---|---|---|---|---|
| C-01 | Constituição §9 (:168-174): nada surge do nada | ADR-028 (09-30) **já retirou a regra**, mas a Constituição não foi emendada | cria ferramenta inicial (`WorkerEquipment:170`), pedregulho da fuga (`ClimbOut:407`), BigHouseMOD (`BigHouseFoundation:34-100`), aldeões (`VillageFoundation:74`), peça sem rota (`BiomeConstructionSupply`) | **RESOLVIDO 06-10 (D-04, ADR-034):** os quatro aceitos; Constituição 1.2.0 emendada |
| C-02 | RULES:43,58 "✅ feita" | — | `BuilderWork.furnish` não existe; código diz "a Regra 21 morreu" | marcar 21/32 como retiradas ou reabrir |
| C-03 | RULES:47,116 "Regra 25 inerte" | — | `PlanOrdering:56` "continua valendo" | qual vale |
| C-04 | ADR-030:24-26: carpinteiro fora da fundação, construtor fora do crescimento | ADR-011 emenda + Regra 35 | `FOUNDATION_ORDER` tem CARPENTER; `GROWTH_ORDER` tem BUILDER | **RESOLVIDO 06-10 (D-05, ADR-034 §5):** vale o código |
| C-05 | ADR-021:17, ADR-030:24: "seis" titulares | Regra 35 | 7 titulares; comentários ainda dizem "seis" (`VillageChests:~172`, `VillageFoundation:278`) | atualizar ADRs e comentários |
| C-06 | ADR-011:21,35 + Constituição §4 (:108-114): preservar ofício Vanilla | ADR-029 | ofício Vanilla é removido | **RESOLVIDO 06-10 (ADR-034 §2)** |
| C-07 | ADR-020 §4: viveiro de agricultor e lenhador | Regra 36: só lenhador | só lenhador | emendar ADR-020 |
| C-08 | RULES:134: E43 "falta decidir" | ADR-010 emenda 09-23 já decidiu | código segue a ADR | atualizar RULES |
| C-09 | RULES:29: baú "só de trabalhador" | — | `VanillaBedChests.ensure` cobre cama Vanilla | atualizar RULES |
| C-10 | Constituição §7 (:146-152): só templates Vanilla | Regra 27-e3, BigHouseMOD | `ColonyModels`, `big_house_mod.nbt` | **RESOLVIDO 06-10 (ADR-034 §4)** |
| C-11 | Constituição §5 (:130): nunca qualquer bloco da vila gerada | Regra 3 | `mayDigOut`/`mayBuildOver` liberam chão do bioma | **RESOLVIDO 06-10 (ADR-034 §3)** |
| C-12 | RULES.md:9: "onde o código discordar, a regra está certa" | RULES.md:158: "quando o código divergir, a tabela está errada" | — | escolher uma frase |
| C-13 | RULES aponta `BuilderWork.furnish`, `BuildSiteScanner.flatGroundAt`, `ColonyHut` | — | inexistente / em `LotLevel` / aposentado | corrigir ponteiros |
| C-14 | Comentários `ColonyGoals:55-61`, `WorkerEquipment:36-41` | Regras 29/30, 2-e1 | comportamento novo | corrigir comentários |
| C-15 | RULES.md remete o corpo das regras a "Project-State §18" | CLAUDE.md: Project-State é histórico | seção não encontrada | copiar o corpo para RULES.md |

**Padrão por trás dos conflitos:** as decisões novas viram ADR ou Regra, mas a
fonte antiga (Constituição, ADR anterior, comentário) não recebe a emenda.
Remédio do processo — não do código — no ROADMAP §6.
