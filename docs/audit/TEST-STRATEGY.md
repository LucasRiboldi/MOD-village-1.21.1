# Estratégia de testes — Village Colony

**Data:** 2026-10-06 · **Snapshot:** `main` `6022021e` · parte da
[auditoria de recuperação](PROJECT-RECOVERY-AUDIT.md).
**Nada foi executado.** Contagens por `grep`; custo por leitura de `tickLimit`.

## 1. O que existe (contado)

| Tipo | Quantidade | Onde |
|---|---:|---|
| `@Test` JUnit | 1.293 (core 867, fabric 335, data/save 75, architecture 16) | `src/test/java` |
| `@Property` jqwik | 1 (`HiringProperties`) | idem, roda em `propertyTest` |
| `@ParameterizedTest` | **0** | — |
| `@GameTest` | 564 em 91 classes | `src/gametest/java` |
| Python | 88 (gauntlet 46, graphify 28, log 9, rubrica 5) | `tests/` |
| `@Disabled`/`@Ignore` | 0 | — |

As 91 classes de GameTest estão **todas** em `src/gametest/resources/fabric.mod.json`
(nenhuma silenciosa hoje), mas nada trava isso: depende de disciplina.

**CI** (`.github/workflows/ci.yml`, 1 job, 45 min): `git diff --check` →
Python → `gradlew build --rerun-tasks` (unitários, propriedade, ArchUnit, Error
Prone/NullAway `-Werror`, PMD só relatório) → `runGametest --rerun-tasks` →
`pitest` (limiar 85%, **só `core`**). Nenhum `continue-on-error`. JaCoCo só
relatório, sem piso.

**Custo dos GameTests:** nenhum gigante por tempo (máx `tickLimit` 800,
`ConstructionSupplyAuditGameTest:53`; 119 com 20 ticks). O custo é volume e
setup manual. 4 classes = 37%: `MinerGameTest` 85 (5.890 linhas),
`BuildSiteGameTest` 49, `LumberjackGameTest` 44, `BuilderGameTest` 31.
Tempo real de execução: NÃO CONFIRMADO (relatório não lido).

## 2. Classificação por grupo

| Grupo | Ação | Motivo |
|---|---|---|
| `core/coordination` (ColonyCycle, ColonyGoals, WorkAssignment, StockRules) | **KEEP** | regra pura, barata, sob PIT |
| `core/colony/service` (VillageDetector, PartialObservation, ColonyMerge) | **KEEP** | detecção; exceção: `VillageDetectorTest:315 constantsMatchTheDecisionRecord` → DELETE-CANDIDATE (só repete constantes) |
| `core` models (Task, Worker, ResourceTally…) | KEEP / CONSOLIDATE | muitos testes de getter/estado; grau de redundância NÃO CONFIRMADO |
| `data/save/*` (75) | **KEEP** | única proteção do formato e da migração |
| ArchUnit (5 classes) | **KEEP** | contrato de arquitetura; reduzir os congelados com o tempo |
| `CycleCostTest`, `PlanningBudgetTest`, `SectorBudgetTest` | **KEEP** | único contrato de custo |
| `fabric/work/*Test` de claims/marks/leases (~40) | **CONSOLIDATE** | mesmo formato; parametrizar |
| Testes de string de log (`SiteLabelTest:49-66`, ActivityLog, IdleLog, VillageLogPresenter…; 31 `contains(...)`) | **DELETE-CANDIDATE** os que só checam texto; **DEFER** o resto | detalhe de implementação |
| GameTests de detecção/ciclo/fundação | **KEEP** | integração real |
| `MinerGameTest` + 10 classes `Mine*` | **CONSOLIDATE** | famílias copiadas: arco (`:1427-1674`), contagem de minério (`:1950-1988`), lugar de apoio (`:2075-2248`). Dividir e parametrizar |
| `LumberjackGameTest` | **CONSOLIDATE** | família da copa (`:579-761`, 8 testes); manter os de encalhe (`:1255`, `:1465`) |
| `BuildSiteGameTest` | **CONSOLIDATE** parcial | variantes de terreno (`:65-1334`) parametrizáveis |
| `WorkerEquipmentGameTest`, `CraftingGameTest`, `WorkMaterialsGameTest` | **REWRITE** p/ unitário onde a lógica não depende do mundo | NÃO CONFIRMADO onde mora a lógica |
| `ColonyEnduranceGameTest` | **KEEP** | 200 ciclos sem vazamento |
| Blueprint/estrutura (`BigHouseModBlueprint`, `StructureCoverage`, `VillageStructures`) | **DEFER** | viram teste de dados sem servidor |
| Python gauntlet/graphify | **DEFER** | protegem ferramenta, não o produto |
| Aparência em jogo (iluminação, painel, ritmo dia/noite) | **PLAYTEST ONLY** | — |

**Nenhum teste é apagado antes de P0-01 (integração das linhas) e de uma
rodada verde da bateria completa.**

## 3. Regras críticas × proteção

| Regra | Risco | Teste atual | Tipo | Valor | Confiança | Ação |
|---|---|---|---|---|---|---|
| Detecção de vila | alto | VillageDetectorTest (24), ColonyDetectionGameTest | U + G | alto | alta | KEEP |
| Ciclo da colônia | alto | ColonyCycleTest (24), ColonyCycleGameTest, Endurance | U + G | alto | alta | KEEP |
| Metas de recurso | alto | ColonyGoalsTest (54) + PIT | U | alto | alta | KEEP |
| Escolha de lote | médio | BuildSiteGameTest (~12 relevantes) | G | médio | média | CONSOLIDATE |
| **Construção completa** | alto | BuilderGameTest:1389 (casa pronta vira infraestrutura) | G | — | baixa | **FALTA fim-a-fim** (NÃO CONFIRMADO, conferir BuilderGameTest) |
| Mineração | médio | MinerGameTest (85) | G | médio | média | CONSOLIDATE |
| Lenhador | médio | LumberjackGameTest (44) | G | médio | média | CONSOLIDATE |
| Agricultura | médio | FarmerGameTest, FarmPlanGameTest | G | médio | média | KEEP |
| **Persistência após reload** | **alto** | data/save (NBT em memória), ConstructionResumeGameTest (mesmo servidor) | U + G | médio | **baixa** | **FALTA** (ver P0-02) |
| Encalhe | alto | StrandedEscapeGameTest, StrandedWorkersTest | U + G | alto | alta | KEEP |
| **Duas colônias simultâneas** | médio | só ColonyCycleTest:456 (unitário) | U | baixo | baixa | **FALTA GameTest** |
| Performance | médio | contas de orçamento, não medida | U | baixo | baixa | PLAYTEST + Spark |

## 4. Testes faltantes (só estes — nada para "aumentar cobertura")

| ID | Teste | Hipótese que prova | Tipo |
|---|---|---|---|
| T-01 | Registro → `sync` → novo `ColonySavedData` lido do NBT → mesmo registro | "o que está na memória sobrevive a recarregar" (pré-requisito de P0-02) | unitário/integração |
| T-02 | Gravação periódica acontece sem `SERVER_STOPPING` | "crash não apaga a colônia" (prova a correção de P0-02) | GameTest |
| T-03 | Vila nova → BigHouseMOD → primeira casa inteira | "a promessa central do mod funciona sem jogador" | GameTest longo (1 só) |
| T-04 | Duas colônias vizinhas trabalhando com baús e minas próximos | "uma colônia não toca a outra" | GameTest |
| T-05 | Paridade `@GameTest` × `fabric.mod.json` | "nenhuma classe some da bateria em silêncio" | unitário barato (junto de `ModMetadataTest`) |

## 5. Pirâmide proposta

```text
           ▲  PLAYTEST + Spark + time_ledger   (comportamento emergente, desempenho)
          ▲▲  GameTest de cenário (poucos)     T-03, T-04, encalhe, endurance
        ▲▲▲▲  GameTest focado                   uma regra que depende do mundo
    ▲▲▲▲▲▲▲▲  Unitário core + ArchUnit + PIT    toda regra que cabe fora do Minecraft
```

Regra de escolha (a "escada de verificação" do pedido do autor):

| Nível | Quando |
|---|---|
| 0 — grep / compilação / teste único | mudança de doc, renome, import |
| 1 — unitário | regra que cabe sem Minecraft |
| 2 — GameTest focado | regra que depende de bloco, entidade, inventário |
| 3 — bateria completa (2 rodadas, ver `bateria-gametest-instavel`) | linha de base, fechar P0, antes de release |
| 4 — playtest | pathfinding, tempo, chunks, várias profissões, economia real, desempenho |

**Teste verde ≠ comportamento visto em jogo.** Todo item deve dizer qual dos
dois tem: `AUTOMATICAMENTE COMPROVADO` ou `AINDA PRECISA DE PLAYTEST`.

## 6. Ordem

1. T-05 (barato, trava o que já está certo).
2. T-01 e T-02 junto com a correção de P0-02.
3. T-04 e T-03 depois da integração das linhas (P0-01).
4. Consolidação de MinerGameTest/LumberjackGameTest/BuildSiteGameTest —
   **só depois** de uma rodada verde estável, um arquivo por commit, com
   contagem antes/depois e PIT intacto.
5. Remover os DELETE-CANDIDATE por último, com aprovação.
