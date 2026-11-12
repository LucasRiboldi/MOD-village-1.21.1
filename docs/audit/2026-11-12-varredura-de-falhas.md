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
| VF-002 | 🟠 | teste instável | `GroundPickupGameTest.workerCollectsWhileWalkingPast` falha em 11 das últimas 30 baterias (19 de 78 no total) | Sentinela; nas 8 falhas com diagnóstico desde 12-11: `colônias=2, trabalhadores da colônia=0, coleta agora=0`; nas outras: `a terra recolhida deve chegar ao baú: 0` (item sumiu sem chegar) | O aldeão sai do registro da colônia do teste durante a bateria: há outra colônia viva ao mesmo tempo e o ciclo global (registros estáticos, relógio único) mexe nos dois. O `TODO.md` ainda diz "1 falha em 12" e "relógio global, não provado" | 5 porquês sobre `trabalhadores da colônia=0`: logar quem chama `WORKERS.remove` (`VillagerLifecycleHandler.java:167`) no teste; isolar o lote ou impedir que o ciclo de uma colônia toque o aldeão de outra | M |
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

Exemplo, VF-002, com o que já se sabe: (1) o aldeão não recolheu → (2) ele não está mais em `WORKERS` da colônia do teste
(`trabalhadores da colônia=0`) → (3) a confirmar: quem chama `WORKERS.remove` ou reatribui com `colônias=2` → (4) o lote
`ground_pickup` divide mundo, hora e registros com outra colônia viva → (5) a falha passou a ser lida como ruído desde a
0.4.50 e a taxa no `TODO.md` ficou em "1 em 12".

### Texto proposto para `docs/PATTERNS.md` (não aplicado; espera o autor)

Em "Bateria e teste":

```markdown
### GameTest falha com `trabalhadores da colônia=0` e outra colônia viva

**Causa:** o aldeão do teste saiu do registro da colônia durante a bateria. Os registros
(`COLONIES`, `WORKERS`) e a hora do mundo são globais, e outra colônia de outro teste
roda o ciclo ao mesmo tempo.

**Onde olhar:** quem chama `VillageColonyMod.WORKERS.remove`
(`VillagerLifecycleHandler`), e `setTimeOfDay` nos testes do mesmo lote.
`GroundPickupGameTest.workerCollectsWhileWalkingPast`, 11 falhas em 30 baterias (VF-002).
```

Em "Ciclo e tempo":

```markdown
### `Colony cycle took` de 500 a 1.200 ms cerca de 90 s depois de abrir o mundo

**Causa:** primeiro ciclo depois do carregamento; a memória do servidor foi esquecida e o
planejador e os trabalhadores a reconstroem num ciclo só. O prazo de 15 ms
(`PlanningBudget.DEADLINE_MS`) cerca só a varredura de lote.

**Onde olhar:** `ColonyCycleRunner` (fase `planner`), `cost_ledger.py` do primeiro ciclo. VF-001.
```

Em "Não é defeito (mas parece)":

```markdown
### Diário de ações começa com bytes nulos

Jogo fechado sem gravar o lote final. O `action_report.py` pula a linha. VF-018.
```
