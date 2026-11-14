# Varredura de falhas — Village Colony (2026-11-12)

Análise **só leitura** da `main` em `733eb0c1` (release `v0.4.68-alpha`). Nenhum código de produção ou teste foi
alterado. Método aprovado pelo autor: fontes automáticas, fontes de execução, revisão dos pontos frágeis, registro único
e método de aplicação. A varredura anterior (`2026-10-14-varredura-engenharia.md`) foi lida para não repetir o que já
mudou; onde um item dela piorou ou melhorou, o achado diz.

## Resumo por severidade

| Severidade | Quantos | IDs |
|---|---|---|
| 🔴 Crítico | 0 | — |
| 🟠 Alto | 4 | VF-001, VF-002, VF-003, VF-004 |
| 🟡 Médio | 10 | VF-005 a VF-014 |
| 🟢 Baixo | 6 | VF-015 a VF-020 |

**Nenhum crítico.** Não há exceção com quadro do mod nos 43 logs de novembro (42 compactados de `2026-11-01-1` a `2026-11-12-1`, mais o `latest.log`; versões
0.4.0 a 0.4.57); o último relatório de crash é de 2026-10-22 (`Map.get` nulo, já corrigido). Nenhum save corrompido.

Os quatro altos: o primeiro ciclo depois de carregar o mundo trava o servidor de 0,5 a 1,2 s (VF-001); o teste instável
`workerCollectsWhileWalkingPast` derruba o Sentinela em um terço das baterias (VF-002); o PIT passa com folga de 7
mutantes num limiar que o CI cobra (VF-003); quinze versões seguidas, da 0.4.54 à 0.4.68, sem jogo (VF-004).

## O que rodou nesta sessão, e o que não

| Fonte | Rodou? | Resultado |
|---|---|---|
| Error Prone + NullAway (`compileJava --rerun-tasks`) | sim | 0 avisos, build ok |
| PMD (`pmdMain`, `config/pmd/complexity.xml`, limiar 15) | sim | 150 violações de complexidade cognitiva em 118 arquivos; 27 com 30 ou mais |
| PIT (`./gradlew.bat pitest`, só `core`) | sim | 4.067 mutações, 85,17%, limiar 85%, folga 7; 465 sobreviventes, 138 sem cobertura, 31 por tempo |
| JaCoCo | **não rodou** — lidos os XML de hoje (17:01 e 17:03), da bateria anterior à 0.4.68 | ver VF-019 |
| SpotBugs (`scripts/audit/static-extra.init.gradle`) | **não rodou** | a verificação de dependências (`gradle/verification-metadata.xml`) recusa o plugin; ver VF-015 |
| Mixins (`scripts/audit/mixin_safety.py`) | sim | 2 configurações, 0 falhas |
| Regras (`scripts/rule_coverage.py`) | sim | 261 regras, 7 "sem teste" (33, 47, 201 a 205); ver VF-017 |
| Logs do jogo | sim | 43 logs de novembro, 38 diários JSONL, 3 crash reports (outubro) |
| Sentinela | sim | 80 execuções em `reports/test-runs/`, análise `reports/sentinel-analysis/20261112T200402Z` |

## Tabela completa

Custo: **P** = até 1 ciclo curto (teste + correção pequena); **M** = 1 ciclo com diagnóstico; **G** = mais de 1 ciclo ou
decisão de arquitetura do autor.

| ID | Sev. | Categoria | Achado | Evidência | Causa provável | Correção sugerida | Custo |
|---|---|---|---|---|---|---|---|
| VF-001 | 🟠 | desempenho | O primeiro ciclo da colônia depois de carregar o mundo leva 471 a 1.212 ms — o servidor trava | Em 21 das 31 sessões de novembro com ciclo medido, os primeiros 150 s depois de `Starting integrated` têm um ciclo de 471 a 1.212 ms: `2026-11-06-4 17:47:14` 1.212 ms (planner 401, workers 378, assign 269); `2026-11-12-1 00:07:55` 629 ms (planner 337, workers 216) | `PlanningBudget.DEADLINE_MS` (15 ms) só cerca a varredura de lote (`ColonyCycleRunner.java:280`); o resto do planejador e os trabalhadores reconstroem do zero a memória que o `ServerMemory` apagou ao abrir o mundo (grafo, adiantamento da Regra 258, índices) | Medir por fase o primeiro ciclo (`VC_COST`) e espalhar a reconstrução por vários ciclos, ou estender o prazo ao planejador inteiro | M |
| VF-002 | 🟠 | teste instável | `GroundPickupGameTest.workerCollectsWhileWalkingPast` falha em 11 das últimas 30 baterias (19 de 78 no total) | Sentinela; nas 8 falhas com diagnóstico desde 12-11: `colônias=2, trabalhadores da colônia=0, coleta agora=0`; nas outras: `a terra recolhida deve chegar ao baú: 0` (item sumiu sem chegar) | **Achada na análise de opções:** a sonda de abandono marca a colônia do teste `ABANDONED` (sem vila Vanilla, 1 aldeão contra `PEOPLE_TO_STAY = 3`) e dispensa o aldeão, quando o ciclo de 600 tiques cai dentro do teste. O `TODO.md` ainda diz "1 falha em 12" e "relógio global, não provado" | Ver "Opções de correção" | P |
| VF-003 | 🟠 | teste / CI | O PIT passa com 85,17% contra limiar 85% (folga 7 mutantes), e o CI cobra o limiar | `build/audit/raw/pitest.log`: `Generated 4067 mutations Killed 3464 (85%)`; `.github/workflows/ci.yml:88` roda `./gradlew pitest` sem `continue-on-error`; `build.gradle:168` `mutationThreshold = 85`. Sobreviventes: `MineShaft` 33 (código novo da 0.4.60: `waypoints:189-197`, `ramp:128`, `rampCell:277`), `RoadRouting` 27, `RoadLines` 24, `VillageSurface` 22, `RoadThinning` 22. 13 `Minion exited abnormally due to TIMED_OUT` | Código novo do `core` entra sem asserção de limite (fronteira, soma/subtração); os tempos esgotados contam como mortos e variam entre rodadas | Escrever asserções de borda para `MineShaft.waypoints` e `ramp` (mata ~10 mutantes de uma vez) antes da próxima mudança no `core` | P |
| VF-004 | 🟠 | processo | Quinze versões seguidas sem jogo: 0.4.54 a 0.4.68 (Regras 243 a 262) | `STATE.md`, todas "Não jogada"; último log de jogo: 0.4.57 (`latest.log`, 12-11 00:36) | O ciclo corrige mais rápido do que o jogo valida (B3 da varredura de 10-14, que voltou) | Jogar a 0.4.68 antes de abrir o próximo ciclo de correção; a fila desta varredura começa pelo que não depende de jogo | P (jogo do autor) |
| VF-005 | 🟡 | desempenho | Fora do primeiro ciclo, 870 avisos de ciclo acima de 50 ms em novembro; p50 68 ms, p90 138 ms, 42 acima de 250 ms | Logs de novembro, `Colony cycle took`; maiores em sessão: `2026-11-08-2 14:19:15` 758 ms, `2026-11-02-3 13:05:22` 690 ms; 196 avisos só em `2026-11-06-4` | Mesmo motivo do VF-001 em escala menor: planejador e trabalhadores sem orçamento comum | Depois do VF-001; usar `cost_ledger.py` por fase antes de otimizar (ADR-035 §5) | M |
| VF-006 | 🟡 | perda de material | Item colhido some quando o baú do fazendeiro ou do pastor está cheio | `FarmerChores.java:347` e `ShepherdWork.java:369` registram `filled up — N of N lost` e descartam; 21 vezes nos logs de novembro, 14 em `2026-11-11-1` no baú `2878, 99, -3425` (fazendeiro e pastor no mesmo baú) | O depósito só tenta o baú do ofício; a Regra 243 (qualquer baú da colônia) não chegou a esses dois caminhos | Tentar os outros baús da colônia (`ColonyChests`) e, sem lugar, largar no chão para o `GroundPickup` em vez de apagar | P |
| VF-007 | 🟡 | estado | Trabalhadores "fantasma": 54 avisos `has been missing for N cycles` em 3 sessões de 11-11 (0.4.52 a 0.4.56); `VC_AUDIT` com `workersNotInTheWorld` 11 de 36 | `2026-11-11-1 09:27:54` e seguintes; `2026-11-11-1 09:45:17` `"workers":36,"workersNotInTheWorld":8` | Não separado ainda: aldeão em chunk não carregado (vila de 5 colônias) ou registro sem dono (`docs/research/estado-que-sobrevive-ao-dono.md`); a Regra 244 tratou só o `WorkStall` | Contar no próximo jogo `missing for` contra a distância do jogador; se for chunk descarregado, rebaixar o aviso | M |
| VF-008 | 🟡 | cadeia de material | `VC_SUPPLY_ERROR clay` do fundidor (`NOT_FOUND`, quatro buscas) 54 vezes de 0.4.1 a 0.4.57, ainda 2 no último jogo | `2026-11-02-1 02:26:59` em diante; `2026-11-12-1` 2 vezes | Argila não existe no raio em muitos biomas; a Regra 257 (0.4.63) mexe na reserva, não na origem | Conferir no jogo da 0.4.68; se seguir, a origem (pedreira ou troca) é decisão do autor, como O2b | P (conferir) |
| VF-009 | 🟡 | cadeia de material | Obra esperando peça sem rota: `smooth_sandstone` 21 + 4 (`STALLED`), `glass_pane` 5 + 2 (`NO_ROUTE`), `acacia_wood` 15, `cut_sandstone` 6 | `2026-11-08-5 15:59:50`, `2026-11-11-1`, `2026-11-12-1` | "A receita existe" não é "a vila faz" (memória `rota-na-receita-nao-e-rota-no-mundo`) | Uma linha por peça: qual ofício falta na cadeia; tratar a mais frequente | M |
| VF-010 | 🟡 | teste instável | `PenAccessGameTest.animalsWalkInThroughEachOfTheFourGates` falha em 10 de 78 baterias; última em 12-11 15:18, "um animal saiu depois de entrar" | Sentinela `20261112T151858Z`; antes, "a vaca não entrou andando pelo portão" com a vaca a até 9 blocos | Pathfinding do Vanilla dentro do prazo do teste, mais o mesmo ambiente global do VF-002 | Testar a regra (portão, piso, alvo) em unidade e deixar a caminhada para o jogo (D1 de 10-14) | M |
| VF-011 | 🟡 | teste frágil | Relógio global nos GameTests: 101 chamadas a `setTimeOfDay` em 31 arquivos, 207 `COLONIES.register` e 14 `runCycleNow` no mesmo mundo | `rg setTimeOfDay src/gametest` | Todos os testes de um lote dividem a hora e os registros estáticos; um teste muda o expediente do outro (memórias `gametest-relogio-global-e-relatorio-velho`, `gametest-interferencia-global`) | Raiz comum de VF-002 e VF-010; um utilitário que fixe a hora por lote e a regra "teste de expediente não roda em lote compartilhado" | G |
| VF-012 | 🟡 | arquitetura | Teto de 500 linhas apertado: 31 arquivos entre 480 e 500 (eram 15 em 10-14), 6 em 500 exatas; o `FileSizeRuleTest` caiu 6 vezes no Sentinela | `SurfaceGatheringWork`, `StrandedEscape`, `SmelterWork`, `HousePlans`, `VillageGround`, `ColonyCycleRunner` com 500; `VillageDetectionHandler` 499 | A3 de 10-14 piorou: comentário é podado para caber em vez de extrair responsabilidade | Ao tocar um destes arquivos, extrair uma classe antes da correção; nunca podar javadoc | M por arquivo |
| VF-013 | 🟡 | arquitetura | `fabric/work/` tem 251 classes num pacote só (eram 172 em 10-14) | `ls src/main/java/com/villagecolony/fabric/work/*.java` | A2 de 10-14 cresceu; a divisão em subpacotes foi cancelada de propósito (memória `f1-subpacotes-work-nao-fazer`: visibilidade de pacote) | Não dividir; registrar como dívida aceita e cobrir com ArchUnit por prefixo de nome, se o autor quiser | G (decisão) |
| VF-014 | 🟡 | complexidade | 27 métodos com complexidade cognitiva 30 ou mais; os maiores: `Corridor.plan` 51, `SitePreparation.clearTrees` 49, `LotLevel.flatGroundAt` 49, `ReachMap.step` 48, `MineCuts.nextCut` 47, `VillageGround.plan` 44 | `build/reports/pmd/main.xml` | Lugar onde os "o guarda X não via o caso Y" nascem; o `MinerSteps` (62 em 10-14) saiu do topo | Reduzir só o método que uma correção tocar, com teste antes | M por método |
| VF-015 | 🟢 | ferramenta | SpotBugs não roda por fora: a verificação de dependências recusa `spotbugs-gradle-plugin:6.5.12` e `kotlin-stdlib:2.0.21` | `build/audit/raw/static.log`: `Dependency verification failed for configuration 'classpath'` | O init script da auditoria entrou antes do `verification-metadata.xml` | Acrescentar as somas desses artefatos ao `verification-metadata.xml` (mudança de build: pede o autor) e rodar o SpotBugs | P |
| VF-016 | 🟢 | estado | Valores de configuração estáticos e mutáveis sem `ServerMemory`: `MineDigging.surfaceRadius`, `SandGathering.sandRadius`, `TreeChoice.stallLimit`, `MineLease.limit`, `MineSite.mineDistance`, `MinerDetours.perTarget`; e `UnreachablePieces.failed`, que avisa uma vez por processo | Varredura de `static` não final em `src/main` | Ajustados por teste ou comando, sobrevivem ao mundo seguinte | Conferir quem escreve cada um; registrar os que um teste muda | P |
| VF-017 | 🟢 | documentação | `rule_coverage.py` conta as revogadas 201 a 205 como "sem teste", e a Regra 52 não tem linha no `RULES.md` | `docs/RULES.md:247-251` (linhas sem a coluna de estado); Regra 52 citada só pela 67 | O script lê o estado na 4ª coluna, que as linhas revogadas não têm | Dar às cinco linhas a coluna "revogada" e uma linha à 52 apontando a 67 | P |
| VF-018 | 🟢 | ferramenta | Um diário de ações começa com bytes nulos | `logs/villagecolony-actions-20261110-221918.jsonl`, linha 1 | Jogo fechado sem `flush` (o `ActionJournal` grava em lote); o `action_report.py` já pula a linha | Nada a corrigir agora; anotar em PATTERNS como "não é defeito" | P |
| VF-019 | 🟢 | medição | Cobertura cega: `fabric.mixin` 0% nas duas baterias (o corpo do mixin vai para a classe Vanilla e o JaCoCo não o credita); `fabric.command` 31,9% na bateria e 31,4% em unidade; `data.save` 54% na bateria (91% em unidade) | `scripts/audit/coverage_report.py` sobre os XML de hoje | Instrumentação e comandos sem teste | Não buscar número para mixin; um GameTest por comando `/vc` usado na triagem | M |
| VF-020 | 🟢 | processo | `TODO.md` com 666 linhas, 214 itens abertos, 21 "conferir em jogo" e 42 seções; taxas de instabilidade velhas ("1 falha em 12") | `TODO.md` | O "vivo e curto" do `CLAUDE.md` §0.1 não se mantém; seções de jogos antigos (0.4.18 a 0.4.38) nunca fecham | Uma passada de limpeza com o autor: o que o jogo da 0.4.68 conferir sai; o resto vai para o histórico | M |

### Conferido e sem achado

- Error Prone e NullAway: 0 avisos na compilação limpa.
- Mixins: 8 em `fabric.mixin` e 1 no cliente; nenhum `@Overwrite`/`@Redirect`, prefixo `villagecolony$`, `defaultRequire: 1`.
  O `ChunkGeneratorMixin` roda na geração de chunk, fora da linha do servidor; o estado dele (`VillageGround.PLANS`) é
  sincronizado.
- `ServerMemory`: 165 classes com estado se inscrevem; as 20 que não se inscrevem são configuração, cliente ou limpas por
  outra (`SweepState` pelo `BuildSiteScanner.clearAll`, `ColonyCycleRunner.lastStock` pelo `VillageDetectionHandler`).
- Crash reports: os três são de outubro (0.3.x) e as causas foram corrigidas.
- Os demais `ChestDepositor.deposit` que ignoram o retorno (`CraftingSteps`, `ColonySupply`, `FarmerBakery`) conferem o
  espaço antes ou devolvem o que tiraram.

## Método de aplicação

### Fila: severidade × custo

| Ordem | ID | Por quê nesta posição |
|---|---|---|
| 1 | VF-004 | jogar a 0.4.68 alimenta VF-007, VF-008 e VF-009 e dá o log do VF-001 com a versão atual |
| 2 | VF-003 | alto e barato; protege o CI antes de qualquer mudança no `core` |
| 3 | VF-006 | médio e barato, perda de material visível |
| 4 | VF-002 | alto; destrava o Sentinela verde de primeira |
| 5 | VF-001 | alto, mas pede medição por fase antes |
| 6 | VF-010, VF-011 | mesma raiz do VF-002 |
| 7 | VF-007, VF-008, VF-009 | dependem do jogo |
| 8 | VF-005, VF-012, VF-014 | dívida, tratada quando um ciclo tocar o arquivo |
| 9 | VF-015 a VF-020 | baixos; VF-015 e VF-017 cabem num commit de ferramenta |

### Um ciclo por correção (`CLAUDE.md` §0.2.2)

1. O autor escolhe o item; para cada um, 2 ou 3 opções com efeito e risco antes de editar.
2. Teste que falha antes da correção (GameTest em `src/gametest` ou unitário), pela causa certa.
3. A menor correção que o faz passar.
4. Sentinela: `python scripts/test_lab.py fast` e `python scripts/sentinel_analysis.py`, com o Minecraft fechado; mudou
   regra ou teste, `python scripts/rule_coverage.py`.
5. Commit atômico `P0.x: descrição (teste que prova)`, numa branch da `main` atualizada.
6. `./gradlew.bat remapJar`, cópia para `downloads/` e para o TLauncher, SHA-256 das três cópias.
7. Candidata até o próximo jogo do autor.

### Falha recorrente: 5 porquês

Para VF-002, VF-010 e VF-001, antes de corrigir:

1. Por que falhou? (a linha do diagnóstico, não a suposição)
2. Por que aquilo aconteceu? (quem mudou o estado, com log)
3. Por que o código deixou? (a guarda que faltou)
4. Por que o teste não pegou antes? (ambiente compartilhado, prazo)
5. Por que o processo não pegou? (taxa velha no TODO, falha tratada como "instável conhecido")

VF-002, feito na análise de opções (seção seguinte): (1) o aldeão não recolheu → (2) ele não está mais em `WORKERS` da
colônia do teste (`trabalhadores da colônia=0`) → (3) a sonda de abandono declarou a colônia do teste abandonada e o
dispensou: `[16:20:01] Colony 1f0b0756… is now ABANDONED — was STABLE, probe found no village (probed from
ColonyPos[x=-14693512…` e `Worker def3f5cc… left the abandoned colony … — profession freed`, na arena da falha
(`reports/test-runs/20261112T191727Z/suites/attempt-01-gametest.stdout.log`) → (4) a colônia de teste não tem vila Vanilla
e tem 1 aldeão, menos que `VillageAdoption.PEOPLE_TO_STAY = 3`; a sonda roda no ciclo de detecção (600 tiques) e o teste
dura 600 tiques, então pegar ou não a sonda depende da fase do relógio do servidor → (5) a falha foi lida como "instável
conhecido" desde a 0.4.50, com a suspeita do relógio global e a taxa "1 em 12" no `TODO.md`.

### Texto proposto para `docs/PATTERNS.md` (não aplicado; espera o autor)

Em "Bateria e teste":

```markdown
### GameTest falha com `trabalhadores da colônia=0` e outra colônia viva

**Causa:** a sonda de abandono do ciclo de detecção achou a colônia do teste sem vila
Vanilla e com menos de 3 aldeões em casa (`VillageAdoption.PEOPLE_TO_STAY`), marcou
`ABANDONED` e dispensou o aldeão. Só acontece quando o ciclo de 600 tiques cai dentro
do teste.

**Onde olhar:** `is now ABANDONED` e `left the abandoned colony` no stdout da bateria,
na coordenada da arena que falhou. `GroundPickupGameTest.workerCollectsWhileWalkingPast`,
11 falhas em 30 baterias (VF-002).
```

Em "Ciclo e tempo":

```markdown
### `Colony cycle took` de 500 a 1.200 ms cerca de 90 s depois de abrir o mundo

**Causa:** primeiro ciclo em que a colônia pode trabalhar (tique ~1.800: espera de
assentamento de 1.200 tiques mais o ciclo de 600). Nele saem a primeira obra, a primeira
leva de pedidos de fabricação e a primeira distribuição de tarefas, além da memória que o
servidor esqueceu ao abrir o mundo. O prazo de 15 ms (`PlanningBudget.DEADLINE_MS`) cerca
só a varredura de lote.

**Onde olhar:** `ColonyCycleRunner` (fase `planner`), `cost_ledger.py` do primeiro ciclo. VF-001.
```

Em "Não é defeito (mas parece)":

```markdown
### Diário de ações começa com bytes nulos

Jogo fechado sem gravar o lote final. O `action_report.py` pula a linha. VF-018.
```

## Opções de correção

Pedido do autor depois do registro: corrigir todos, com análise de causa e 2 ou 3 opções por achado, pesando o quanto
cada solução é pesada. Nada foi corrigido; cada opção espera a escolha. A recomendada vem primeiro e marcada com ★.

**Peso** diz o tamanho da mudança: **Leve** = 1 ou 2 arquivos, 1 teste, sem custo de tique, sem ADR; **Média** = 3 a 6
arquivos ou um subsistema, GameTest novo, custo de tique desprezível; **Pesada** = mais de um subsistema, vários testes,
custo de tique a medir, ou ADR.

**Ferramenta que muda o build ou demora** (SpotBugs com mudança no `verification-metadata.xml`, PIT, JaCoCo, bateria
inteira) só roda com permissão do autor; onde uma opção precisa disso, ela diz.

### VF-001 — o primeiro ciclo trava o servidor de 0,5 a 1,2 s

**Causa, mais funda.** O pico cai sempre no tique ~1.800 (em `2026-11-12-1`, `"tick":1817`): é o primeiro ciclo depois
da espera de assentamento (`Colony.SETTLE_TICKS = 1_200`) somada ao ciclo de 600. Nesse ciclo saem, de uma vez, a
primeira obra (`build_planned`), a primeira leva de "could not make" para cada peça da casa, o primeiro mapa de alcance
(`VC_REACH cells=14972`) e a primeira distribuição de tarefas. A fase `planner` do log não é só o `ConstructionPlanner`:
inclui `PhantomWorkerLog.probe`, `WoodInPieces`, `HousePlans.paletteOf`, `BiomePieces.refresh` e todo o `WorkMaterials`
(`ColonyCycleRunner.java:255-357`). O prazo de 15 ms cobre só o `ConstructionPlanner.plan`, e só a varredura de lote.
Sem medida por subfase não dá para dizer qual delas pesa: os `VC_COST` comuns do mesmo jogo têm `planner_us` de 4 a
19 ms, então o primeiro ciclo custa 20 a 80 vezes o normal.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Medir primeiro**: o autor grava um Spark do carregamento (`/spark profiler start --timeout 180` ao entrar no mundo) e eu acrescento uma linha `VC_COST` só do primeiro ciclo de cada colônia, com as subfases do `planner` | Diz qual subfase pesa; o comportamento não muda | Nenhum; custa um jogo curto | Leve: `ColonyCycleRunner` (em 500 linhas: extrair uma classe antes) e `CycleCost`; teste unitário da linha |
| B. **Espalhar o primeiro ciclo**: no ciclo em que a colônia sai do assentamento roda só o planejador; tarefas e trabalho começam no ciclo seguinte | Corta o pico pela metade sem saber a causa exata | A vila nova começa 30 s mais tarde; GameTests que contam com o primeiro ciclo completo podem mudar | Média: `ColonyCycleRunner`, `Colony`; GameTest de cronologia |
| C. **Prazo para a fase inteira**: o prazo de relógio vale para todo o `planner` (o que estourar fica para o próximo ciclo) | Teto real por ciclo | Demanda calculada pela metade pode pedir material errado; exige decidir o que vale mais | Pesada: `PlanningBudget`, `WorkMaterials`, `ConstructionPlanner`; ADR nova (mexe na ADR-003) |

### VF-002 — `workerCollectsWhileWalkingPast` instável

**Causa achada (5 porquês na seção anterior).** A sonda de abandono do ciclo de detecção (`VillageAdoption.judgeAbandonment`,
`ColonyAbandonment.judge`) não vê vila Vanilla na arena, e a colônia tem 1 aldeão contra `PEOPLE_TO_STAY = 3`; marca
`ABANDONED`, e o `VillagerLifecycleHandler.dismissIfAbandoned` tira o aldeão da colônia. Prova: `20261112T191727Z`,
`[16:20:01] Colony 1f0b0756… is now ABANDONED … probed from ColonyPos[x=-14693512, …]`, na arena da falha. Não é defeito
do jogo: vila de verdade tem vila Vanilla. O mesmo mecanismo pode derrubar qualquer GameTest com colônia de menos de 3
aldeões que dure perto de 600 tiques (`20261111T052543Z` mostra outra colônia de teste abandonada às 02:28:38).

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Só o teste**: a colônia do teste ganha 2 aldeões parados (IA desligada) dentro da caixa e longe do item, somando 3 em casa | Fecha este instável sem tocar o código do jogo | Outros testes com colônia pequena seguem expostos | Leve: `GroundPickupGameTest`; prova com a bateria `fast` 3 vezes (com permissão) |
| B. **A bateria inteira**: o `ColonyFixture` e os testes com colônia própria marcam a colônia como "de teste", e a sonda de abandono a ignora | Fecha a classe de defeito para os ~94 testes com fixture | Um gancho de teste no código do jogo (inerte em jogo); precisa de teste que prove que em jogo nada muda | Média: `ColonyAbandonment` ou `VillageAdoption`, `ColonyFixture`, ~10 testes com colônia própria |
| C. Desligar a detecção durante a bateria | Some toda interferência da sonda | Os GameTests de detecção e abandono deixam de medir o que medem | Média, e esconde defeito real |

### VF-003 — PIT com folga de 7 mutantes

**Causa.** Código novo do `core` entrou com teste de forma, não de valor. `MineShaft.waypoints` (`MineShaft.java:185-203`)
não tem teste que confira a lista de pontos; `ramp:128`, `rampCell:277-278` e `floorOf:207` só são exercitados de
passagem. Os 31 `TIMED_OUT` contam como mortos e variam entre rodadas, então a folga real oscila.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Testes de valor no `MineShaft`**: a lista exata de `waypoints()` num poço conhecido, as bordas de `ramp` e `rampCell` | Mata ~15 dos 33 sobreviventes; folga sobe para ~20 | Nenhum (só teste) | Leve: `MineShaftTest`; confirmar com PIT (≈8 min, com permissão) |
| B. A + os sobreviventes de `RoadRouting`, `RoadLines` e `RoadThinning` (73 juntos) | Folga de ~60; o CI deixa a beira | Nenhum; mais tempo de escrita | Média: 3 a 4 classes de teste; PIT com permissão |
| C. Baixar o limiar para 84 | CI com folga sem teste novo | Esconde o que o PIT mede; contraria a memória `ci-verde-esconde-pit-parado` | Leve, não recomendado |

### VF-004 — quinze versões sem jogo

**Causa.** O ciclo de correção roda mais rápido que o jogo do autor; cada versão é candidata e a próxima empilha.
Ferramenta nenhuma corrige isto.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Jogar a 0.4.68 antes** de qualquer correção de comportamento; enquanto isso, só as correções sem efeito no jogo (VF-002, VF-003, VF-015, VF-017) | O jogo confere as Regras 243 a 262 de uma vez e alimenta VF-001 e VF-007 a VF-009 | Atrasa as correções de jogo em um jogo | Leve (tempo do autor) |
| B. Corrigir tudo e jogar uma versão só no fim | Menos jogos | Se algo quebrar, 20 mudanças a separar | Pesada para diagnosticar depois |
| C. Um jogo curto (30 min) a cada 3 correções de comportamento | Meio-termo | Jogo curto não pega vila madura | Média |

### VF-005 — ciclos acima de 50 ms ao longo da sessão

**Causa.** Mesma família do VF-001: não há orçamento comum entre `planner`, `assign` e `workers`; em vila grande
(`2026-11-06-4`, 196 avisos) a fase `workers` passa de 300 ms.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Esperar o VF-001** e reler com o `cost_ledger.py` do jogo da 0.4.68 | Decisão com número (ADR-035 §5) | Nenhum | Leve |
| B. Orçamento global por ciclo, repartido entre as fases | Teto em vila grande | Trabalho de aldeão atrasa em vila grande | Pesada: ADR nova |

### VF-006 — item colhido destruído com baú cheio

**Causa.** `FarmerChores.store` (`FarmerChores.java:340`) e `ShepherdWork` (`ShepherdWork.java:365`) chamam
`ChestDepositor.deposit` só no baú do ofício e descartam a sobra. `ColonyChests.deposit(world, ColonyChests.ownFirst(…),
item, n)` já existe e faz o próprio baú primeiro e os da colônia depois, que é a Regra 243.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Próprio baú, depois os da colônia, e a sobra vai ao chão** junto do baú (o `GroundPickup` recolhe) | Zero perda enquanto houver baú; aviso só quando nem o chão serve | Item no chão some em 5 min se ninguém recolher | Leve: `FarmerChores`, `ShepherdWork`; 1 GameTest (baú cheio, outro com lugar) |
| B. Só os baús da colônia (sem chão) | Igual ao A enquanto houver lugar | Vila toda cheia ainda perde | Leve |
| C. Parar de colher com o baú cheio | Nada se perde | Fazendeiro e pastor ociosos (Regra 51) | Média |

### VF-007 — trabalhadores "fantasma"

**Causa, até onde o log deixa ver.** `PhantomWorkerLog.probe` chama de ausente quem `world.getEntity` não acha, com a
colônia ativa. A colônia `9105dea9` tinha 36 trabalhadores e 8 a 11 sempre ausentes, com 5 colônias vizinhas; a caixa é
maior que a distância de simulação (16 chunks), então "ausente" pode ser só chunk descarregado. O log não diz a distância
do jogador e não separa os dois casos. A regra do código é "ausência não é morte".

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Medir**: a linha passa a dizer a última posição conhecida, a distância ao jogador e se o chunk está carregado | O próximo jogo diz se é fantasma ou chunk longe | Nenhum | Leve: `PhantomWorkerLog`; teste unitário da linha |
| B. Comando `/vc fantasmas soltar`, que libera vaga e baú de quem está ausente com o chunk carregado | Conserto manual e seguro | Depende do autor rodar | Média: comando novo + GameTest |
| C. Poda automática após N dias ausente com o chunk carregado | Conserto automático | Apagar trabalhador legítimo se a regra errar | Pesada: ADR (contraria "ausência não é morte") |

### VF-008 e VF-009 — argila e vidro sem origem; peça sem rota

**Causa.** As duas pontas são a mesma cadeia: a fornalha sem cru. `glass_pane` falta porque `glass` falta (o log de 12-11
mostra "needs 8 minecraft:glass and has 0" para cada vidro colorido), e `glass` vem de areia na fornalha; a argila
(`clay`) não existe no raio em muitos biomas e falha 4 buscas. `smooth_sandstone` e `cut_sandstone` (deserto) param pelo
mesmo motivo do lado do arenito. A Regra 257 (0.4.63) só tirou a reserva de metade; a decisão O2b (pedreiro faz arenito,
ou a pedreira roda com ramal) segue aberta.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Conferir no jogo da 0.4.68** (`VC_SUPPLY_ERROR` por peça) e então decidir O2b | Decide com o número da versão atual | Nenhum | Leve |
| B. A obra troca a peça sem rota pela do mesmo papel que a vila faz (vidro colorido → vidro comum), como a Regra 262 troca a madeira | A casa não espera peça impossível | A casa sai diferente da planta | Média: `BiomeBlockSwap`, conferência por papel; GameTest |
| C. Fonte nova de areia e argila (pedreira de areia fora do deserto, ou argila pela exceção da Regra 128) | Fecha a cadeia na origem | Recurso sem origem física pede ADR-028/034 | Pesada: ADR |

### VF-010 — `animalsWalkInThroughEachOfTheFourGates` instável

**Causa provável, não provada.** O teste leva 4 vacas, uma por portão, em até 1.200 tiques. São duas falhas diferentes:
"a vaca não entrou andando" (o caminho do Vanilla não chega no prazo) e "um animal saiu depois de entrar" (uma vaca de
dentro sai por um portão aberto para a seguinte; a vaca tem o passeio do Vanilla). O lote `pen_large_walk` roda ao mesmo
tempo que `shepherd_pens_size` e `pen_escape_scan_race` (`20261112T151858Z`, 12:20:36 a 12:20:38).

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Medir no teste**: na falha, dizer qual vaca saiu, por qual portão e se ele estava aberto | Separa as duas causas | Nenhum | Leve: `PenAccessGameTest` |
| B. Se for o portão aberto: a vaca de dentro fica parada enquanto o portão seguinte abre (só no teste) | Fecha o instável | Pode esconder o mesmo defeito em jogo (animal fugindo do curral) | Leve |
| C. Se for o portão aberto também em jogo: o `FencedIn` só abre o portão seguinte com o anterior fechado | Corrige jogo e teste | Pastor mais lento com rebanho grande | Média: `FencedIn`, `ShepherdPens`; GameTest |

### VF-011 — relógio e memória globais nos GameTests

**Causa.** Todos os testes dividem um mundo: 101 `setTimeOfDay` em 31 arquivos, ~180 `clearAll()` de estado estático
(`BuildSiteScanner.clearAll` 34, `MineClaims.clearAll` 30, `LotRefusals.clearAll` 22…) e sete constantes de jogo que os
testes encurtam e devolvem (`TreeChoice.shortenStallLimitTo`, `SandGathering.shortenSandRadiusTo`…). Lotes rodam ao mesmo
tempo; o `clearAll` ou o raio curto de um teste vale para o outro.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Regra de teste nova + checagem**: teste que muda a hora, limpa estado global ou encurta constante roda em lote próprio e sozinho (`batchId` único, conferido por um teste de arquitetura) | Tira a sobreposição sem mexer no jogo | Bateria um pouco mais lenta | Média: ~40 `batchId`, 1 teste de arquitetura |
| B. Trocar `clearAll()` em teste por limpeza da própria colônia (`forget(colonyId)`) | Remove a raiz | Muitas classes do jogo ganham `forget` | Pesada: ~15 classes de produção, ~180 chamadas |
| C. Só documentar em PATTERNS e seguir | Nada muda | Instáveis continuam | Leve |

### VF-012 — 31 arquivos no teto de 500 linhas

**Causa.** O `FileSizeRuleTest` congela o teto; quem precisa de linha poda comentário ou espreme.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Extrair ao tocar**: cada ciclo que mexer num destes arquivos começa extraindo uma classe por responsabilidade | Diminui sem projeto à parte | Ciclo um pouco maior | Leve por vez |
| B. Uma rodada de extração dos 6 arquivos em 500 exatas (`SurfaceGatheringWork`, `StrandedEscape`, `SmelterWork`, `HousePlans`, `VillageGround`, `ColonyCycleRunner`) | Folga imediata | Refatoração sem defeito a corrigir; regressão possível | Pesada: 6 subsistemas, bateria inteira com permissão |

### VF-013 — 251 classes em `fabric/work`

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Aceitar como dívida registrada** (a divisão foi cancelada de propósito pela visibilidade de pacote; memória `f1-subpacotes-work-nao-fazer`) | Nada muda; o registro diz por quê | Nenhum | Leve (documento) |
| B. Regra ArchUnit por prefixo de nome (`Mine*` não chama `Builder*` direto, por exemplo) | Fronteira sem mudar pacote | Falsos positivos no começo | Média |

### VF-014 — métodos de complexidade alta

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Reduzir só o método que uma correção tocar**, com teste antes | Sem refatoração solta | Lento | Leve por vez |
| B. Uma rodada nos 6 maiores (`Corridor.plan` 51, `SitePreparation.clearTrees` 49, `LotLevel.flatGroundAt` 49, `ReachMap.step` 48, `MineCuts.nextCut` 47, `VillageGround.plan` 44) | Complexidade cai já | Regressão em caminho, lote e mina; pede jogo | Pesada |

### VF-015 — SpotBugs não roda

**Causa.** `gradle/verification-metadata.xml` não tem as somas do plugin do SpotBugs e do `kotlin-stdlib` que ele puxa.
A flag `--dependency-verification=lenient` foi negada pelo controle de permissões desta sessão.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Gravar as somas** (`./gradlew.bat -I scripts/audit/static-extra.init.gradle --write-verification-metadata sha256 help`) e rodar o SpotBugs; muda o build, **pede permissão** | Mais uma fonte de defeito (NPE, igualdade, concorrência) | Somas novas entram sem revisão manual de cada artefato | Leve: 1 arquivo de build |
| B. Rodar o SpotBugs pelo JAR solto sobre `build/classes`, sem Gradle | Não toca o build | Baixar o JAR do SpotBugs; pede permissão | Leve |
| C. Ficar sem SpotBugs | Nada | Perde a fonte | Leve |

### VF-016 — constantes de jogo mutáveis e estáticas

**Causa.** Sete valores (`MineDigging.surfaceRadius`, `SandGathering.sandRadius`, `TreeChoice.stallLimit`,
`MineSite.mineDistance`, `MinerDetours.perTarget`, `MineLease.limit`, `UnreachablePieces.failed`) existem para o teste
encurtar; se um teste falha antes do `restore…`, o valor curto vaza para o resto da bateria.

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Junto do VF-011 A**: quem encurta roda em lote próprio e devolve em `finally` (conferir os 7 usos) | Fecha o vazamento | Nenhum | Leve |
| B. As 7 entram no `ServerMemory` (voltam ao valor de jogo ao trocar de mundo) | Protege o jogo, não a bateria | Nenhum | Leve |

### VF-017 — regras revogadas contadas como "sem teste"

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Dar às linhas 201 a 205 do `RULES.md` a coluna de estado** ("revogada") e uma linha à Regra 52 ("substituída pela 67") | `rule_coverage.py` passa a 2 sem teste (33 e 47, que não pedem teste) | Nenhum | Leve: 1 documento |
| B. O script aceita "Revogada" em qualquer coluna | Mesmo efeito | Pode esconder texto que só cita "revogada" | Leve |

### VF-018 — diário com bytes nulos

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Nada no código**; entra em PATTERNS como "não é defeito" (texto proposto acima) | — | Nenhum | Leve |
| B. O `ActionJournal` grava também a cada 5 s | Perde menos linhas num fechamento forçado | Mais escrita em disco | Leve |

### VF-019 — cobertura cega em mixin e comandos

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **GameTest para os comandos de triagem** (`/vc dados`, `/vc bioma`, `/vc log`, `/vc bioma nota`) e aceitar 0% no mixin (o corpo vai para a classe Vanilla) | Os comandos que a triagem usa não quebram calados | Nenhum | Média: 4 GameTests |
| B. Aceitar como está | — | Comando quebrado só aparece no jogo | Leve |

### VF-020 — `TODO.md` longo

| Opção | Efeito | Risco | Peso |
|---|---|---|---|
| ★ A. **Depois do jogo da 0.4.68**, eu proponho o TODO limpo: o que o jogo conferiu sai, seções de jogos até a 0.4.38 vão para `docs/archive/technical/` com o mesmo texto; o autor aprova antes do commit | Volta a ser "vivo e curto" | Item ainda válido escondido no histórico | Leve (documento) |
| B. Limpar agora, sem o jogo | Mais cedo | Itens "conferir em jogo" saem sem conferir | Leve |

## Escolhas do autor (12-11)

| ID | Escolha | Quando |
|---|---|---|
| VF-001 | A: medir primeiro (Spark do autor + `VC_COST` do primeiro ciclo com subfases) | antes do jogo (só log) |
| VF-002 | A: só este teste, com 3 aldeões em casa | antes do jogo |
| VF-003 | A: testes de valor no `MineShaft` | antes do jogo |
| VF-004 | A: jogar a 0.4.68 antes das correções de comportamento | — |
| VF-005 | A: esperar a medida do VF-001 | depois do jogo |
| VF-006 | A: próprio baú, baús da colônia e, sobrando, o chão | depois do jogo |
| VF-007 | A: medir (posição, distância ao jogador, chunk carregado) | antes do jogo (só log) |
| VF-008/009 | **Regra nova do autor**: areia e argila que o fundidor não consegue recolher em 5 minutos tentando surgem no baú do fundidor, na quantidade toda que a obra pede, com linha `VC_SUPPLY` como os outros surgidos. Recurso sem origem física: pede emenda à ADR-028/034 | depois do jogo |
| VF-010/011 | A: lote separado e checagem; no curral, medir antes qual vaca sai | antes do jogo |
| VF-012 | A: dividir ao tocar | sempre |
| VF-013 | A: aceitar e registrar | — |
| VF-014 | **B: rodada nos 6 maiores** (`Corridor.plan`, `SitePreparation.clearTrees`, `LotLevel.flatGroundAt`, `ReachMap.step`, `MineCuts.nextCut`, `VillageGround.plan`) | depois do jogo, um por ciclo |
| VF-015 | A: autorizado gravar as somas no `verification-metadata.xml` e rodar o SpotBugs | antes do jogo |
| VF-016 | A: junto do lote separado, devolver em `finally` | antes do jogo |
| VF-017/018 | Arrumar a tabela de regras; diário só anotado em PATTERNS | antes do jogo |
| VF-019 | A: GameTest dos 4 comandos de triagem | antes do jogo |
| VF-020 | A: limpar o TODO depois do jogo | depois do jogo |
| PATTERNS | Textos propostos aprovados; entram com a primeira correção | — |
| Ferramentas | Sentinela `fast` e PIT autorizados sempre que a correção pedir | — |

## Progresso das correções (13-11)

Branch `fix/varredura-falhas`, da `main` em `733eb0c1`, sem push. Até aqui só teste, documento e ferramenta: o JAR do
mod não muda e nada foi gerado nem instalado.

| ID | Commit | Prova |
|---|---|---|
| VF-002 | `722f7ec5` | Sentinela `fast` verde (`20261113T023226Z`), nenhuma colônia de teste abandonada no log; PATTERNS.md com as 3 assinaturas aprovadas |
| VF-017 | `68a47bc0` | `rule_coverage.py`: 262 regras, sem teste só 33 e 47 |
| VF-003 | `0a6b36cc` | `MineShaftWaypointsTest`, 10 testes; PIT do zero 86,03%, folga 42; `MineShaft` de 33 para 14 sobreviventes, a maioria equivalente (grade de tochas simétrica) |
| VF-011/016 | `b0e2364f` | 126 GameTests em lote próprio; `GameTestBatchRuleTest` (sabotagem pega); `MinerGameTest` devolve o raio da mina; Sentinela verde (`20261113T031655Z`) |
| VF-015 | `81689813` | SpotBugs rodou: 111 achados, nenhum defeito confirmado; sobra o campo morto `CraftingWork.stalled` |
| VF-019 | `df58cc57` | `BiomeCheckCommandGameTest` (3 comandos); Sentinela verde (`20261113T033731Z`), 1.112 GameTests |

**Achados novos no caminho.**

- O PIT local usa histórico incremental (`build/pitHistory.txt`, `enableDefaultIncrementalAnalysis`): o número local
  pode diferir do CI, que roda do zero. A rodada do zero leva cerca de 29 minutos.
- A separação de lotes expôs um teste que dependia da hora do vizinho
  (`ShepherdGameTest.theShepherdShearsOverTheFenceWithoutGoingIn`, corrigido no mesmo commit). Outros 25 GameTests
  perderam o vizinho que acertava a hora e nenhum caiu nas duas baterias seguintes; ficam aqui para o autor decidir se
  ganham hora própria: `BiomePiecesGameTest` (3), `CraftStationGameTest`, `HouseDoorsGameTest`,
  `ShepherdLooseFlockGameTest`, `WorkMemoryGameTest`, `WorkMemorySaveGameTest`, `WorkTimeSwingGameTest`,
  `WorkHoursGameTest.restingStopsWorkMovementButKeepsHomeAndThePendingJob`, `BuilderRefusedSpotGameTest` (2),
  `CraftingAdvanceGameTest` (2), `CraftingGameTest`, `ShepherdPensGameTest` (2), `BuildProgressGameTest` (2),
  `BuilderGameTest` (3), `LeafInTheWayGameTest`, `LumberjackGameTest.theHarvestTakesTimeAndEndsInTheChest`.
- `ShepherdHerdingGameTest.theShepherdLeadsALooseCowToTheFence` caiu uma vez em 82 rodadas (outro animal escolhido); a
  mensagem agora diz qual.

**Falta antes do jogo:** VF-001 A (linha de custo por subfase do `planner`; exige extrair uma classe do
`ColonyCycleRunner`, que está em 500 linhas), VF-007 A (linha do fantasma com posição, distância e chunk) e VF-010 A
(qual vaca sai do curral). As duas primeiras mudam o JAR: pedem versão nova, JAR e cópia para o TLauncher antes do jogo.

**Depois do jogo:** VF-006 A, VF-008/009 (regra nova do autor), VF-014 B, VF-020, VF-005.

