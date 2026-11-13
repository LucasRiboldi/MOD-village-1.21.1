# Avaliação — Bloco A: arquitetura e código (2026-11-13)

Snapshot: worktree em `ac2f1977` (0.4.74). Só diagnóstico: nenhum arquivo do repositório foi alterado (`git status` limpo
ao fim; saídas só em `build/`, que é ignorado, e no scratchpad da sessão).

## O que rodou de verdade e o que só foi lido

**Rodado nesta sessão (JDK 21.0.12+8):**

| Comando | Resultado |
|---|---|
| `./gradlew.bat compileJava pmdMain test --tests 'com.villagecolony.architecture.*'` | BUILD SUCCESSFUL em 3 min. `compileJava` com Error Prone + NullAway e `-Werror`: **0 avisos**. 18 de 18 testes de arquitetura PASSED (6 ArchUnit, 3 ConversionBoundary, 5 DependencyRule, 1 FileSizeRule, 1 GameTestRegistry, 2 ModMetadata). PMD do build (só `CognitiveComplexity`, nível 15): **148 violações**. |
| `jdeps -verbose:package` e `-verbose:class` sobre `build/classes/java/main` | 676 classes, 171 arestas entre pacotes do mod; ciclos e acoplamento por script (`scratchpad/cyc.py`, `cbo.py`, `dead.py`). |
| PMD por fora, com init script fora do repositório (`bestpractices`, `errorprone`, `design`) | **2.876 violações** (detalhe em D6). Saída em `build/audit/pmd-audit.xml`. |
| SpotBugs por init script | **Não rodou**: a verificação de dependências do Gradle (`gradle/verification-metadata.xml`) recusou o plugin. É o comportamento correto do build; liberar exigiria editar o repositório. |
| Contagens por `find`/`wc`/`grep` e heurística de métodos (`scratchpad/meth.py`) | tamanho, comentários, estado estático, duplicação. |
| `git ls-tree` em `fc11a631` (antes de 15-10) e `43c6d89b` (antes de 01-11) | crescimento do código. |

**Só lido:** `HANDOFF.md`, `STATE.md` (60 linhas), `CLAUDE.md`, `AGENTS.md`, `build.gradle`, `config/pmd/complexity.xml`,
`ArchitectureRulesTest.java`, `FileSizeRuleTest.java`, `archunit_store/`, `ServerMemory.java`, os 8 mixins, cabeçalhos de
estado das 54 ADRs e trechos de `ADR-006` e `ADR-024`, memória `f1-subpacotes-work-nao-fazer`.

## Números de base

- Código de produção: 683 arquivos `.java`, 111.314 linhas. `fabric/` 463 arquivos e 85.877 linhas (77%); `core/` 190 e
  21.588; `data/` 17 e 2.640; `client/` 13 e 974; raiz 1 (`VillageColonyMod`, 235).
- `fabric/work`: **250 arquivos, 49.603 linhas, num pacote plano**. `fabric/integration`: 147 arquivos, 28.049 linhas, plano.
- Crescimento: 504 arquivos antes de 15-10, 604 antes de 01-11, 683 hoje (+36% em um mês). `fabric/work`: 175 para 250.
- Faixas de linhas por arquivo: ≥500: 5 (todos com exatamente 500) · 450–499: 39 · 300–449: 77 · 150–299: 152 · <150: 411.
  24 arquivos têm 490 linhas ou mais.

---

### D1 — Arquitetura e modularidade (ISO 25010: manutenibilidade — modularidade, reusabilidade, modificabilidade, testabilidade)

Nota proposta: **6/10** — o núcleo puro e os mixins são exemplares e vigiados por teste, mas a camada fabric, que é 77% do
código, é um bloco só de dependências mútuas, preso a singletons estáticos.

Evidências:
- **CONFIRMADO (rodado)** — a fronteira `core` sem Minecraft é respeitada e testada no bytecode e no fonte:
  `ArchitectureRulesTest.theCoreDoesNotTouchMinecraftOrFabric`, `theCoreHasNoPackageCycle`, `DependencyRuleTest` (5 regras)
  e `ConversionBoundaryTest` passaram. O `jdeps` confirma: `core.*` só aponta para `core.type` (e `core.coordination` para os
  outros domínios do core, exceção declarada). `data.save` só depende de `core`. Nenhum código comum referencia
  `net.minecraft.client` (`grep` vazio em `core/ fabric/ data/`).
- **CONFIRMADO (rodado)** — **toda a camada fabric é um único componente fortemente conexo**, passando pela raiz:
  `com.villagecolony` ↔ `fabric.{command, content, event, integration, network, overlay, work, world}` (8 pares
  bidirecionais no `jdeps`), e o componente conexo reúne a raiz e 9 pacotes fabric. A causa é o localizador de serviço
  estático em `VillageColonyMod.java:70-162` (`COLONIES`, `WORKERS`, `TASKS`, `CONSTRUCTIONS`, `BUILDINGS`, `MINES`,
  `STORAGES`…), usado por **259 arquivos**. O ArchUnit não vê isto: a fatia `com.villagecolony.fabric.(*)..`
  (`ArchitectureRulesTest.java:96`) exclui a raiz, e o registro congelado de ciclos está vazio
  (`archunit_store/bd1afc07…`, 0 linhas), o que dá falsa impressão de camada sem ciclo.
- **CONFIRMADO (rodado)** — dentro de `fabric/work`, **146 das 250 classes formam um único ciclo de dependência** entre
  classes (componente fortemente conexo pelo `jdeps -verbose:class`). Fan-out médio de 11,6 tipos do mod por classe. O plano
  F1 de subpacotes foi cancelado em 15-10 por boa razão (147 métodos package-private na mina, memória
  `f1-subpacotes-work-nao-fazer`), mas o pacote cresceu de 175 para 250 classes desde então sem nenhuma fronteira lógica.
- **CONFIRMADO (rodado)** — classes-deus por acoplamento: `fabric.event.ColonyCycleRunner` referencia 121 tipos (83 do mod),
  `runCycleOf` tem cerca de 113 linhas de código (`ColonyCycleRunner.java:183`); `fabric.work.ColonyState` 92 tipos;
  `fabric.command.BiomeCheck` 90; `SurfaceGatheringWork` 87; `MaeSites` 84. PMD `GodClass`: 58 classes (28 em `fabric/work`,
  18 em `fabric/integration`, 8 em `core/construction`); `CouplingBetweenObjects`: 15.
- **CONFIRMADO (lido e rodado)** — estado global estático com disciplina de limpeza: 294 coleções `static final … = new`
  (109 em `fabric/work`, 45 em `fabric/integration`), 197 delas `Map<UUID,…>`; 179 classes se inscrevem em `ServerMemory`
  (`core/type/ServerMemory.java:47`), que esquece tudo na troca de mundo. A regra ArchUnit
  `aStaticMutableCollectionIsServerMemory` passou, com 10 violações congeladas (`archunit_store/9f6005cc…`); amostra de
  `SweepState` mostrou que é limpo por `BuildSiteScanner.clearAll` (`BuildSiteScanner.java:431-438`), ou seja, parte é falso
  positivo da regra. Sem threads, `CompletableFuture` ou executores no código (`grep` vazio): tudo no thread do servidor.
- **PROVÁVEL** — risco do estado estático para a 1.0: servidor dedicado e LAN funcionam (um servidor por JVM, chaves por
  UUID de colônia ou aldeão, saves só no Overworld, por exemplo `data/save/ColonySavedData.java:187`); o custo real é
  testabilidade (cenários de GameTest se contaminam, memória `gametest-interferencia-global`) e qualquer caminho que troque
  de mundo sem passar por `ServerLifecycleHandler`. Não reproduzido.
- **CONFIRMADO (lido)** — mixins: 8 (7 de servidor e 1 de cliente; `villagecolony.mixins.json`,
  `villagecolony.client.mixins.json`), 334 linhas no total, nenhum `@Overwrite`, `defaultRequire: 1`. Todos delegam
  (`aMixinOnlyDelegates` passou). Dois respondem pelo Vanilla com `setReturnValue`: `HostileSpawnMixin.java:28-33`
  (`canSpawnInDark` em HEAD, só perto da tocha da colônia) e `LandPathNodeMakerMixin.java:52-85` (tipo de nó e pulo, em
  RETURN, para todo `MobEntity` que passa pelo filtro de `ColonyPathing`). Ambos justificados na própria classe pela ADR-004.
  Porém `HostileSpawnMixin`, `ItemStackCraftMixin` e `MoveToTargetTaskMixin` não aparecem em nenhuma ADR (`grep` em
  `docs/decisions`), contra "Mixin novo exige ADR" do `AGENTS.md`.
- **CONFIRMADO (lido)** — ADRs: 54 arquivos, 53 números (**ADR-025 duplicada**: `client-side-pixel-art-overlays` e
  `mineiro-autonomo`), sem índice; estado escrito de 5 formas (Accepted, Aceita, aceita, Estado, Status), 3 sem estado na
  primeira linha (019, 020, 026), 043 a 046 ainda dizem "implementação em andamento". Conflito com o código: a ADR-024
  (aceita) define o `WarehouseIndex`, que **não é usado por nenhuma classe de produção** (só pelo teste e por javadoc). Várias
  ADRs são atas de decisões do autor (036, 037, 039, 041), não decisões de arquitetura.

Riscos para a 1.0:
- Qualquer mudança em `fabric/work` pode atingir 146 classes; o custo de revisão e de regressão cresce com cada versão.
- O ArchUnit dá verde num ciclo que existe (a raiz fica fora da fatia).
- Singletons estáticos impedem rodar duas colônias isoladas em teste e dificultam um modo de "recarregar" seguro.
- ADRs sem estado confiável: quem chega não sabe qual decisão vale.

O que falta para 7/10 e para 9/10:
- 7/10: incluir a raiz na regra de ciclos (ou proibir `fabric.*` → `com.villagecolony` exceto `LOGGER`/`MOD_ID`) e congelar o
  que existe (P). Regra ArchUnit por convenção de nome (`Mine*`/`Miner*`, `Shepherd*`/`Pen*`, `Farmer*`/`Crop*`) como fatia
  lógica de `fabric/work`, congelada, sem mover arquivo, como a própria memória F1 sugere (M). Índice de ADRs com estado
  único (vale, emendada por, substituída por, retirada), corrigir ADR-025 duplicada e 043–046 (P). Remover ou religar
  `WarehouseIndex` e decidir a ADR-024 (P).
- 9/10: trocar o acesso estático `VillageColonyMod.COLONIES/WORKERS/…` por um contexto por servidor passado aos sistemas
  (G). Dividir `ColonyCycleRunner` em fases registradas (M). Reduzir o componente cíclico de `fabric/work` a grupos por
  profissão com dependência num só sentido (G). ADR para cada mixin (P).

Bloqueia a 1.0? **Não.** É dívida de manutenção, não defeito em jogo; o núcleo e os mixins, que são o que mais quebra
compatibilidade, estão bem contidos. Passa a bloquear o ritmo depois da 1.0 se o pacote continuar crescendo 40% ao mês.

---

### D6 — Qualidade do código (ISO 25010: manutenibilidade — analisabilidade, modificabilidade)

Nota proposta: **6/10** — compila limpo com Error Prone travado e quase sem duplicação, mas a nulidade só é verificada em um
quinto do código, a complexidade se concentra em `fabric/`, o teto de 500 linhas virou meta e o comentário conta história.

Evidências:
- **CONFIRMADO (rodado)** — Error Prone com `-Werror` em `compileJava`: 0 avisos (`build.gradle`, bloco
  `tasks.withType(JavaCompile)`; `ReferenceEquality` desligada com justificativa). Supressões no código: 13 `UnusedMethod` e
  9 `UnusedVariable` (os handlers dos mixins) e 1 `ImmutableEnumChecker`.
- **CONFIRMADO (rodado)** — **NullAway só no core**: 23 dos 43 pacotes têm `@NullMarked`, todos em `core/`; nenhum em
  `fabric/`, `data/`, `client/` (`grep @NullMarked package-info.java`). Os cerca de 89 mil linhas fora do core não têm
  verificação de nulidade, embora o `build.gradle` diga que "a camada fabric entra pacote a pacote".
- **CONFIRMADO (rodado)** — complexidade: PMD `CognitiveComplexity ≥ 15` = 148 métodos (mediana 20, máximo 51, 27 com 30 ou
  mais); 74 em `fabric/work`, 41 em `fabric/integration`, 18 em `core/construction`. Piores: `Corridor.plan` 51
  (`fabric/integration/Corridor.java:146`, 10 parâmetros), `SitePreparation.clearTrees` 49, `LotLevel.flatGroundAt` 49,
  `ReachMap.step` 48, `MineCuts.nextCut` 47, `VillageGround.plan` 44. PMD amplo: `CyclomaticComplexity` 226,
  `NPathComplexity` 37, `ExcessiveParameterList` 7, `TooManyMethods` 100.
- **CONFIRMADO (rodado, heurística)** — métodos longos (linhas de código sem comentário): 157 acima de 30, 35 acima de 50,
  11 acima de 80. Maiores: `ConstructionResume.resume` cerca de 122 (`fabric/work/ConstructionResume.java:79`),
  `ColonyCycleRunner.runCycleOf` 113, `ShepherdHerding.step` 108, `VillageLogCommand.show` 107, `BuilderWork.step` 96.
  Contagem por script de chaves, não por ferramenta: margem de erro pequena, os três primeiros da lista bruta eram records e
  foram descartados.
- **CONFIRMADO (rodado)** — o teto virou alvo: 44 arquivos entre 450 e 500 linhas, 24 com 490 ou mais, 5 com exatamente
  500 (`SurfaceGatheringWork`, `StrandedEscape`, `HousePlans`, `VillageGround`, `ColonyCycleRunner`). O
  `FileSizeRuleTest` passou e a lista congelada está vazia. Divisões feitas por tamanho e não por responsabilidade aparecem
  no próprio javadoc, por exemplo `MinerProbe`: "separado de MinerWork em 2026-09-24, quando ele passou de 500 linhas".
- **CONFIRMADO (rodado)** — duplicação baixa: só 7 blocos de 8 linhas normalizadas se repetem entre arquivos diferentes (3
  entre `ProfessionDetailScreen` e `ProfessionMenuScreen`, 2 entre `VillagerLifecycleHandler` e `VillagerRegistration`).
- **CONFIRMADO (rodado)** — comentário que conta história, contra a ADR-035 §6: 28% das linhas são comentário (31.612 de
  111.314); **1.588 linhas de comentário trazem data**, em 494 dos 683 arquivos; 616 citam playtest, sessão, Spark ou "pedido
  do autor". Ao mesmo tempo, 1.068 linhas citam Regra, ADR ou KF, que é o formato pedido. O próprio
  `ArchitectureRulesTest.java:90` ainda diz "Em 2026-09-30 havia quatro" ciclos, quando o registro está vazio.
- **CONFIRMADO (rodado e conferido por grep)** — código morto em produção: 7 classes (cerca de 730 linhas) sem nenhum uso no
  código de produção, só em teste ou javadoc: `core.construction.model.RoadGraphRoutes`, `core.coordination.ColonyFocus`,
  `core.storage.model.WarehouseIndex`, `fabric.integration.VillageInventoryObserver`, `fabric.work.EnduranceReport`,
  `fabric.work.MinerProbe`, `fabric.work.PlanAffordability`. Vão para o JAR publicado.
- **CONFIRMADO (rodado)** — ruído do PMD amplo que **não** é defeito: `LawOfDemeter` 1.156, `GuardLogStatement` 350,
  `CloseResource` 18 (todos sobre `ServerWorld`, `MinecraftClient`, `PointOfInterestStorage`, falso positivo),
  `UnusedPrivateMethod` 13 (handlers de mixin). Captura ampla de exceção: 7 `catch (RuntimeException|Exception)`, todas em
  fronteira (save, ciclo, brain), por exemplo `ColonyCycleRunner.java:155`. Nomes de código em inglês, como pede o
  `AGENTS.md`; a sigla `Mae*` (10 classes) é a única opaca. Só 2 `TODO` no código, os dois em javadoc.

Riscos para a 1.0:
- `NullPointerException` na camada fabric sem rede de proteção da compilação (é onde mora quase todo defeito visto em jogo).
- Métodos de complexidade 40 a 51 nos sistemas de terreno, mina e obra: correção nova tende a criar regressão.
- Comentário datado envelhece: o leitor não sabe se a frase de 2026-09 ainda vale.

O que falta para 7/10 e para 9/10:
- 7/10: `@NullMarked` em `data/save` e `fabric/adapter` primeiro, depois os pacotes menores (P–M). Apagar as 7 classes mortas
  ou movê-las para `src/test` (P). Quebrar os 10 métodos com complexidade 40 ou mais, com teste de caracterização antes (M).
- 9/10: NullAway em toda a camada fabric (G). Nenhum método com complexidade cognitiva acima de 25 e o PMD `CognitiveComplexity`
  passando a reprovar com registro congelado (M). Enxugar os comentários datados dos arquivos tocados em cada ciclo até a
  contagem cair abaixo de 300 (M, contínuo). Trocar o teto de 500 linhas por um teto de complexidade por classe, para parar
  a divisão por tamanho (P).

Bloqueia a 1.0? **Não.** Nenhum achado é defeito comprovado em jogo. O NullAway ausente na camada fabric é o item com mais
chance de virar crash de usuário; recomendo tratá-lo antes da 1.0, pelo menos em `data/save` (leitura de save).

---

## Top 5 problemas do bloco (por gravidade)

1. **Camada fabric é um único ciclo de dependências, preso a singletons estáticos.** `VillageColonyMod` com 8 serviços
   `public static final`, usados por 259 arquivos; a raiz e 9 pacotes fabric formam um componente conexo; o ArchUnit não vê
   porque a fatia exclui a raiz. Afeta testabilidade, isolamento em GameTest e qualquer evolução para estado por servidor.
2. **`fabric/work`: 250 classes num pacote plano, 146 num só ciclo, +43% desde 15-10.** `ColonyCycleRunner` referencia 121
   tipos. Sem fronteira lógica, cada correção de playtest pode tocar o pacote inteiro.
3. **NullAway só no core (21% do código).** `fabric/`, `data/` e `client/` (cerca de 89 mil linhas) compilam sem
   verificação de nulidade; inclui a leitura de save.
4. **Complexidade concentrada e teto de 500 linhas virando meta.** 148 métodos com complexidade cognitiva 15 ou mais (27 com 30
   ou mais, máximo 51); 44 arquivos entre 450 e 500 linhas; divisões por tamanho registradas nos próprios javadocs.
5. **Higiene de decisão e de comentário.** 1.588 linhas de comentário datadas em 494 arquivos (ADR-035 §6); ADR-025
   duplicada, ADRs sem estado ou com estado vencido, sem índice; `WarehouseIndex` da ADR-024 morto no código; 3 mixins sem
   ADR; 7 classes mortas no JAR.

## O que eu não consegui verificar

- **SpotBugs:** bloqueado pela verificação de dependências do Gradle; rodar exigiria editar `gradle/verification-metadata.xml`.
  **Checkstyle:** não existe no build e não foi rodado.
- **Efeito do estado estático em jogo:** multijogador, servidor dedicado, troca de mundo e `/reload` não foram exercitados;
  as conclusões sobre risco são por leitura (PROVÁVEL).
- **GameTests e a bateria completa de unitários:** não rodei (bloco B); só os 18 testes de `architecture`.
- **LCOM (coesão):** não medido. Acoplamento foi medido só por tipos referenciados (fan-out/fan-in pelo `jdeps`).
- **Métodos longos:** por script de chaves, não por ferramenta (o PMD `NcssCount` amplo achou 9 métodos acima do limite
  padrão, o que confirma a ordem de grandeza).
- **Validade de cada ADR contra o código:** só conferi estado, numeração, ADR-006 (camadas), ADR-024 (`WarehouseIndex`) e
  a cobertura dos mixins; as 53 ADRs não foram lidas por inteiro.
- **Mapas estáticos sem limite:** 119 arquivos têm `static final Map<UUID…>`, a maioria sem `remove` no próprio arquivo; como as
  chaves parecem ser id de colônia (poucas) e tudo é limpo na troca de mundo, tratei como SUSPEITO de baixo risco, sem medir.
