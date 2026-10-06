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

---

## 7. Análise de custo e organização — medida em 2026-10-06

**Base:** branch `integra/linhas-2026-10-06` (`852f3a54`). Diferente das seções
1–6, aqui os números são **medidos**: `build --rerun-tasks --profile`, XML de
`build/test-results`, os logs das duas rodadas de `runGametest --rerun-tasks`
(timestamps de cada lote) e as etapas da última execução do CI
(`gh run view 37465893589`). Nenhum teste foi alterado.

### 7.1 Onde vai o tempo

**Local, `build --rerun-tasks` = 60 s**

| Tarefa | Tempo | Observação |
|---|---:|---|
| `compileJava` | 22,3 s | Error Prone + NullAway ≈ 15 s (70%): medido 22,6 s com × 6,6 s sem, 2 rodadas cada |
| `test` | 21,3 s | 1.325 casos; ver abaixo |
| `pmdMain` | 7,6 s | só relatório (`ignoreFailures = true`) |
| `remapSourcesJar` | 7,0 s | empacotamento, irrelevante no ciclo de desenvolvimento |
| `propertyTest` | 4,1 s | 1 propriedade (sobe o fabric-loader-junit) |
| `compileTestJava` / `compileGametestJava` | 3,7 / 3,1 s | |
| `remapJar` / `jacocoTestReport` | 2,9 / 2,3 s | |

**Unitários:** 15,9 s somados nas classes, mas **1.302 dos 1.325 casos levam
menos de 10 ms**. Duas classes fazem 14,2 s: `ServerMemoryRegistrationTest`
8,1 s (sobe o `Bootstrap` do Minecraft e inicializa todas as classes do mod) e
`ArchitectureRulesTest` 6,1 s (importa o classpath no ArchUnit). Parte dos
8,1 s é o `Bootstrap`, que só 3 testes usam e que a JVM paga uma vez — quanto
sobraria tirando-o é **SUSPEITO, não medido**.

**Local, `runGametest --rerun-tasks` ≈ 2 min 15 s a 2 min 30 s**

| Fase | Tempo |
|---|---:|
| Gradle (compilação, JaCoCo, remap) até o servidor | ≈ 30–45 s |
| Subida do servidor até o 1º lote | 14–15 s |
| 292 lotes, 610 testes | 90 s |
| ↳ **um lote: `construction_supply` (1 teste)** | **64 s (71% dos 90 s)** |
| ↳ os outros 291 lotes, 609 testes | ≈ 26 s |

O lote é `ConstructionSupplyAuditGameTest.everyBuildableVillageBlockHasABiomeSupplyDecision`:
lê de uma vez os NBTs de 5 biomas e pergunta o suprimento de cada bloco,
travando o servidor 64 s. É **auditoria de dados**, não comportamento de
aldeão, e o resultado só muda quando mudam estruturas, catálogo ou
`BiomeConstructionSupply`.

**CI, um push ≈ 6 min 30 s**

| Etapa | Tempo |
|---|---:|
| Build + unitários | 76 s |
| GameTests | 115 s |
| **PIT (mutação, só `core`)** | **165 s (42%)** |
| resto | ≈ 20 s |

Três das últimas seis execuções da linha Codex falharam em GameTests (04-10 e
05-10); as duas de hoje passaram. A causa das falhas antigas não foi lida
(**NÃO CONFIRMADO** se era defeito ou instabilidade).

**Conclusão de tempo:** a bateria **não é lenta pela quantidade**. 609
GameTests cabem em 26 s e 1.300 unitários em menos de 2 s. O custo está
concentrado em três pontos: um teste de auditoria (64 s), o PIT no CI
(165 s) e a falta de filtro, que obriga a rodar tudo para testar uma coisa.

### 7.2 Organização: o que atrapalha quem desenvolve

| ID | Achado | Evidência | Efeito no dia a dia |
|---|---|---|---|
| ORG-1 | **Sem filtro de GameTest.** A API desta versão só tem `fabric-api.gametest`, `.command` e `.report-file` | strings de `fabric-gametest-api-v1-2.0.5` | testar uma regra = rodar 610 testes (≈ 2,5 min) |
| ORG-2 | **Registro manual no `fabric.mod.json`**, sem trava | `MineOverflowStorageGameTest` ficou fora na linha Codex e a contagem "585" não o incluía (achado em 06-10) | teste novo pode nunca rodar, e a bateria diz verde |
| ORG-3 | **Fixture só limpa, não monta.** `ColonyFixture` tem `create/owning/cleanUp`; cada teste monta a colônia à mão | 127 `COLONIES.register(` em 48 arquivos; `new Colony(` 19× só no `MinerGameTest` | cenário novo = copiar 20–40 linhas de outro teste |
| ORG-4 | **Trabalhador equipado é exceção.** `TestWorkers.createEquippedWorker` existe, 2 arquivos usam | grep | mede a mão nua, não a profissão (memória `gametest-nao-equipa-trabalhador`) |
| ORG-5 | **Helpers privados copiados** entre classes | `ground` ×6, `forget` ×5, `forceChunks` ×4, `at` ×4, `cleanUp`/`chest`/`floor`/`pen`/`count`/`setUp` ×3 | a mesma correção em vários lugares |
| ORG-6 | **Zero parametrização.** 0 `@ParameterizedTest`, 0 `CustomTestProvider` | grep; `CustomTestProvider`, `BeforeBatch` e `AfterBatch` **existem** no 1.21.1 (jar mapeado) | famílias copiadas (copa, terreno do lote, minério) |
| ORG-7 | **Dois lugares para GameTest.** 74 arquivos em `gametest/`, 31 espalhados em `fabric/event`, `fabric/integration`, `fabric/work`, `fabric/world` | `find` | sem regra de onde pôr o próximo |
| ORG-8 | **Arquivos-monstro.** `MinerGameTest` 85 testes / 5.890 linhas; `BuildSiteGameTest` 49; `LumberjackGameTest` 44 | `wc` | difícil achar o teste da regra que se está mexendo |
| ORG-9 | **JaCoCo sempre ligado no `runGametest`** | `build.gradle` | **medido:** ≈ 2,4 s (5%), 49,1 s com × 46,7 s sem — fica ligado |

### 7.3 Oportunidades, por ganho ÷ custo

Ganhos medidos onde há número; "estimado" onde não há. Nada implementado.

| ID | Oportunidade | Ganho | Custo | Risco | Vale? |
|---|---|---|---|---|---|
| **OP-1** | Tirar `ConstructionSupplyAuditGameTest` da bateria comum: tarefa própria (`runGametestAudit`) que o CI roda **só** quando mudam `structure/`, `catalog/`, `VillageStructures` ou `BiomeConstructionSupply` (filtro `paths`), e antes de release | **−64 s por rodada local (−71% do tempo de teste, ≈ −45% do `runGametest`)**; ≈ −60 s no CI | baixo | perder a auditoria num push que mude o suprimento por outro caminho → filtro de paths amplo + rodar antes de release | **sim, primeiro** |
| **OP-2** | Teste de paridade `@GameTest` × `fabric.mod.json` (T-05), unitário junto do `ModMetadataTest` | evita o ORG-2, que já aconteceu | muito baixo (ms) | nenhum | **sim, primeiro** |
| **OP-3** | Filtro próprio: `-PgametestOnly=Miner*` faz o `processGametestResources` gravar só as entradas que casam | rodar uma família em ≈ 1 min em vez de 2,5 min (estimado) | baixo–médio | esquecer de rodar a bateria inteira antes do commit → o CI continua rodando tudo | **sim** |
| **OP-4** | PIT só em PR e na `main` (ou incremental, com histórico em cache) | ≈ −165 s em cada push de branch | baixo | mutação vista mais tarde | **sim** |
| OP-5 | Fixture que **monta**: `ColonyFixture.colonyAt(context, …)` registrando e já sendo dona; `TestWorkers` equipado por padrão; helpers comuns (`ground`, `forceChunks`, `forget`) num `Scenario` | menos 20–40 linhas por teste novo; fim do ORG-4 | médio | migrar tudo de uma vez quebra testes estáveis → **só ao tocar no teste** | sim, gradual |
| OP-6 | Comando `/test` interativo (`-Dfabric-api.gametest.command`) numa configuração de execução de servidor | depurar um cenário olhando o mundo | muito baixo | nenhum | sim |
| OP-7 | Parametrizar famílias: JUnit `@ParameterizedTest` nos unitários de claims/marks; `CustomTestProvider` nas famílias de GameTest (copa, terreno, minério) | −30% de linhas nesses arquivos (estimado) | médio | teste gerado com nome ruim é difícil de achar quando falha | depois de OP-1..OP-5 |
| OP-8 | Dividir `MinerGameTest` por assunto (descida, galeria, salão, transbordo, encalhe) | achar o teste da regra | médio | só move código; a contagem tem que bater antes e depois | depois |
| OP-9 | Regra de pasta: GameTest em `gametest/`, salvo quando precisa de acesso de pacote (aí no pacote da classe testada) | previsibilidade | muito baixo | — | sim, como regra escrita |
| OP-10 | Medir antes de mexer: Error Prone no `compileJava` e JaCoCo no `runGametest` (com e sem) | decide se vale um modo local rápido | muito baixo | — | sim, só medição |

### 7.4 O que **não** fazer

| Ideia | Por que não |
|---|---|
| Apagar GameTests para ganhar tempo | os 609 comuns custam 26 s; o ganho é desprezível e o risco não |
| Paralelizar os unitários (`maxParallelForks`) | o tempo líquido deles é < 2 s; o resto é custo fixo de 2 classes |
| Aumentar `tickLimit` ou folgas para tirar instabilidade | já refutado (memória `bateria-gametest-instavel`) |
| `maxAttempts`/`requiredSuccesses` como regra geral | esconde defeito de mecanismo; só para instabilidade de ambiente provada |
| Tirar o ArchUnit ou o PIT de vez | são os contratos que mais protegem; mudar **quando** rodam, não **se** rodam |
| Migração em massa para a fixture nova | quebra testes estáveis sem ganho imediato |

### 7.5 Ciclo de desenvolvimento proposto (depois de OP-1..OP-4)

| Situação | Comando | Tempo |
|---|---|---|
| mexeu em `core/` | `gradlew test --tests "*Classe*"` | ≈ 30 s (a maior parte é `compileJava`) |
| mexeu em `fabric/` | `gradlew runGametest -PgametestOnly=Familia` | ≈ 1 min (estimado) |
| antes do commit | `gradlew build runGametest` | ≈ 2 min, sem a auditoria |
| mexeu em estrutura ou suprimento | `gradlew runGametestAudit` | ≈ 1,5 min |
| fechar P0 ou release | bateria ×2 `--rerun-tasks` + auditoria + PIT | como hoje |

### 7.6 Ordem recomendada

1. **OP-2** (paridade) e **OP-1** (auditoria fora da bateria comum): o maior
   ganho com o menor risco. Um commit cada.
2. **OP-4** (PIT no CI) e **OP-3** (filtro local).
3. **OP-10** (medir Error Prone e JaCoCo): decide se há um modo rápido local.
4. **OP-9** como regra escrita; **OP-5** gradual, a cada teste tocado.
5. **OP-7** e **OP-8** só depois de uma bateria estável com a fixture nova.

### 7.7 Estado em 2026-10-06 (ADR-035 §7)

| ID | Estado | Commit / evidência |
|---|---|---|
| OP-1 | ✅ feito | `0f64a2fe` — bateria comum 611/611 em 25 s de servidor; auditoria em passo próprio |
| OP-2 | ✅ feito | `54298eea` — `GameTestRegistryTest`, provado por mutação; `test` declara os arquivos que lê |
| OP-3 | ✅ feito | `0f64a2fe` — `-PgametestOnly`; uma família em 22 s |
| OP-4 | ✅ feito | `37a7e4b0` — PIT em PR, `main` e disparo manual |
| OP-5 | ✅ iniciado | `7b090a64` — fixture pública que monta; migração só ao tocar |
| OP-6 | ✅ feito | `0057183a` — `runGametestServer`; o `/test` digitado não foi exercitado |
| OP-7 | ⏳ depois | parametrização, conforme 7.6 |
| OP-8 | ⏳ depois | divisão do `MinerGameTest` |
| OP-9 | ✅ feito | `docs/technical/Testing-Strategy.md` §10 |
| OP-10 | ✅ medido | Error Prone ≈ 15 s do `compileJava`; JaCoCo ≈ 2,4 s. **Decisão:** nenhum modo local sem Error Prone por enquanto — ele é o `-Werror` do NullAway, e um modo que o pule deixa passar o erro até o CI. Reavaliar se o ciclo de unitário incomodar |
| — | registrado | KF-003 em `docs/behavioral-tests/known-failures.md`: duas intermitentes de fabricação, 1/10 cada |
