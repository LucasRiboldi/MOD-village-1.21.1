# TODO

**Atualizado:** 2026-09-24. Playtest real de ~6h30 analisado (5 colonias,
221.814 linhas de log). **Dois erros novos, E47 e E48** — ver "Erros
abertos" abaixo. Achado central: um construtor ficou preso fisicamente
longe do lote por 3h30 seguidas (E47), o que impediu qualquer obra de
terminar e por consequencia a alternancia casa/infraestrutura nunca
escolheu casa (E48) — zero "house is up" na sessao inteira. `VC_ACTIVITY`
(Task 7) confirmado funcionando em jogo pela primeira vez: 1.132 linhas.

Sessao anterior: Tasks 3-13 do plano de confiabilidade operacional
concluidas (10 implementadas, 2 investigadas e recusadas por decisao, 1
ja coberta antes da sessao). Task 14 (verificacao + playtests): a parte
automatizada fechou verde — `gradlew test` 992/992, `gradlew build`
sucesso, `runGametest --rerun-tasks` 423/423 GAME TESTS COMPLETE. O JAR
0.3.0 foi publicado por decisao do autor antes dos playtests confirmarem
(ver `docs/proxima-sessao.md`); este playtest de 24-09 e o primeiro
resultado real contra ele.

## Pendências de correção levantadas pela avaliação — viabilidade (2026-09-24)

Ordem recomendada. O esforço e o risco são estimativa; o aceite é o que a
próxima avaliação mede.

| # | Correção | Esforço / risco | Aceite |
|---|---|---|---|
| 1 ✅ | Limite de **tempo** para o planejador (`SweepDeadline`, 15 ms) — **feito**, ⬜ ver `cycle_over_tick` cair em jogo | baixo / baixo | `cycle_over_tick` cai; C13 ≥ 3 |
| 2 ✅ | Cota ajustável (`PlanningBudget`), e em jogo **só a vila foco planeja e é sondada** (`VillageFocus`, decisão do autor) — **feito** | baixo / baixo | teste unitário da regra |
| 3 ✅ | Registro único (`ServerMemory.resetAll`) no lugar de 78 `clearAll` à mão — **feito**. Achou `BiomeConstructionSupply` sem limpeza nenhuma e as duas listas divergentes. **C05 não muda:** os campos continuam; o que acabou foi o esquecimento | médio / baixo | teste de inscrição ✅; C05 ≥ 3 pede consolidar os campos (R2) |
| 4 | Matar sobreviventes do PIT (`MineShaft`, `ProfessionAssigner`, `ColonyCycle`) | médio / nulo | C08 ≥ 85% |
| 5 | JaCoCo na bateria de jogo (cobertura do `fabric`) | baixo-médio / nulo | cobertura do `fabric` medida |
| 6 | PR do branch para a `main` (85 commits) | baixo / publica | CI verde no PR — **pede aval do autor** |
| 7 | `STATE.md` até 150 linhas | baixo / nulo | contagem |
| 8 | 39 avisos de javadoc e 2 variáveis sem uso | baixo / nulo | C10 = 4 |
| 9 | Ciclo de tarefa comum aos 7 ofícios | alto / médio | `JOBS` ≤ 2, `giveUp` = 1 — **ADR antes** |
| 10 | Regras de decisão do `fabric` para o `core` | médio-alto / médio | teste unitário e PIT cobrem as regras — **ADR antes** |

Não recomendados:
- trocar o `ordinal()` usado como prioridade, que é deliberado e documentado;
- reescrever o `toResourceType`, que é uma tabela legítima.

## Avaliação técnica 2026-09-24 — B (3,21/4) — `docs/technical/avaliacao/2026-09-24-17613fa/RELATORIO.md`

As recomendações, cada uma com um aceite que a próxima avaliação mede
(metodologia em `docs/technical/avaliacao/METODOLOGIA.md`):

- [ ] 🔴 **R1**: atacar o custo do ciclo. **C13 de 1 para ≥3.** Medido no log de
  24-09 (196 ciclos lentos): o **planejador é 91% do custo** (49,9 s de 54,7 s;
  média 255 ms, máx 554 ms), a detecção 8%, e baús, ofícios e atribuição
  somam menos de 1%. A causa: `PlannerTurns.PER_CYCLE = 8`, com 8 colônias
  carregadas, e cada uma varre até 1.024 colunas. A cota foi calibrada em
  09-15 com 2,5 ms por colônia; hoje custa cerca de 30 ms.
- [ ] 🟠 **R2**: estado global num contexto por servidor. **C05 ≤ 3/kLOC**, sem
  `clearAll` à mão no ciclo de vida.
- [ ] 🟠 **R3**: ciclo de tarefa comum aos 7 ofícios. **`JOBS` de 8 para ≤2** e
  **`giveUp` de 6 para 1**.
- [x] 🟠 **R4**: branch levado para a `main` pelo PR #2 (merge `692d8b9`, CI verde).
- [x] 🟡 **R5**: `STATE.md` ≤ 150 linhas (95; o texto antigo foi arquivado no `Historico`).
- [ ] 🟡 **R6**: sobreviventes do PIT (`MineShaft`, `ProfessionAssigner`,
  `ColonyCycle`). **C08 ≥ 85%.**
- [ ] 🟡 **R7**: tabelas de `if` viram `Map`/`switch`. CC máx ≤ 20.
- [ ] 🟡 **R8**: JaCoCo no `runGametest` para medir o `fabric`.
- [ ] 🟢 **R9**: convenção de mensagem de commit (sem prefixo < 10%).
- [ ] 🟢 **R10**: história datada sai dos comentários ao tocar o arquivo
  (C11 ≤ 0,6).

## Qualidade e verificação — 2026-09-24 (pesquisa de métodos, decisão do autor)

- [x] **1. CI nos branches `codex/**`** (`0b1e8b6`). Antes o CI só rodava na `main`.
- [x] **2. PIT no core** (`987edc9`): `./gradlew pitest`, 77% das mutações
  mortas, força de teste 86%. No CI roda sem reprovar e sobe o relatório.
  - [x] Parado do `78e7efc` ao `7619b1d` sem ninguém ver: o
    `ServerMemoryRegistrationTest` sobe o jogo, o PIT roda sem ele, e o
    `continue-on-error` do CI calava a falha. Corrigido em 25-09; rodada
    local com 1312 mutações, 78% mortas, força 86%.
  - [ ] 🟡 Matar os sobreviventes (160 em 25-09): `MineShaft` 37,
    `Building` 12, `ColonyCycle` 12, `Worker` 10, `ProfessionAssigner` 9, `Mine` 9.
  - [ ] 🟡 O passo do PIT no CI não pode falhar calado: `continue-on-error`
    serve para o número baixo, não para o PIT que nem começa.
- [x] **3. GameTests intermitentes isolados** (`668c915` e `4590e4c`):
  lote na rocha, viveiro de dez e `ChestMarker`. Três rodadas 433/433,
  mas ainda sem prova de cura; seguir medindo por repetição.
- [x] **4. spark instalado** em `mods` (1.10.109, SHA-512 conferido), com
  o procedimento em `docs/technical/Profiling-spark.md` (`e91f6be`).
  ⬜ gravar o perfil no próximo playtest (196 ciclos acima de um tique).
- [x] **5. Error Prone só com avisos** (`7040c23`). Código morto removido.
  - [ ] 🟡 72 avisos restantes, quase todos de javadoc (InvalidLink 24,
    MissingSummary 12, NotJavadoc 7); `EnumOrdinal` 7, `LongDoubleConversion` 4.
- [x] **6. fabric-loader-junit** (`0ed2e52`). Teste unitário lê o registro;
  as tags não, porque vêm do datapack do servidor.
- [ ] 🟢 **7. Testes de cliente (`fabric-client-gametest`)**: guardado
  para o futuro por decisão do autor. Serviria para ver placa, nome e
  quadro sem playtest (screenshot + XVFB no CI).
- [x] **8. JaCoCo** (`987edc9`): `build/reports/jacoco/test/html`.
- [x] **9. Arquivos de produção abaixo de 500 linhas**: os 17 foram
  divididos por movimento puro, com 33 classes novas (`42cf8a6` a
  `4596c8d`). Cada lote passou por build e bateria completa, e as strings
  de log continuam idênticas. Os arquivos de **teste** acima de 500 linhas
  (`MinerGameTest` 5802, `BuildSiteGameTest` 2471…) ficaram de fora.
- [x] **10. Depuração de mixin** em todo run de desenvolvimento (`15bdcfd`).
- [~] **11. Hooks** (`d98bcff`): os scripts estão prontos e testados.
  ⬜ o autor liga no `.claude/settings.json` (trecho em `scripts/hooks/README.md`).
- [x] **12. Analisador de log** com 19 assinaturas e as peças mais
  esperadas (`99e1ac0`).

## Revisão de naturalidade 2026-09-24 (docs/technical/Revisao-Naturalidade-2026-09-24.md)

Decisão do autor aplicada no mesmo dia. Todos os itens têm teste
automatizado; nenhum foi visto em jogo.

- [x] 🔴 **N1**: fundação uma vez, 1 aldeão por cama da BigHouseMOD, sem
  reposição; `VillageMeals` para a procriação (`fc9d9b8`). ⬜ ver em jogo
  um filhote nascer e ganhar ofício.
- [~] 🔴 **N2**: recusado pelo autor; a BigHouseMOD fica.
- [x] 🔴 **N3**: equivalente antes da peça pronta, 119 → 102 automáticas (`bd7072e`).
- [~] 🔴 **N4**: fica como está por ora (decisão do autor).
- [x] 🟠 **N5**: a rua já era `dirt_path` desde o P0.7; nada a fazer.
- [x] 🟠 **N6/N7**: ficam ligados; placa em topo + 5 (`8e78fa7`). ⬜ ver a leitura em jogo.
- [~] 🟠 **N8**: recusado; o viveiro fica em `rooted_dirt`.
- [x] 🟡 **N9**: casa → oficina de ofício → o que sobrou (`4590e4c`).
- [x] 🟡 **N10**: tampão da escada da fuga (`553faaa`).
- [x] 🟡 **N11**: alcance 24 + 2 por cama (`74a273a`).
- [ ] 🟡 Lenhador, mineiro e construtor não têm oficina no catálogo
  Vanilla, e o N9 os pula. Falta o autor dizer se algum deve ter.
- [ ] 🟡 `TreeNurseryGameTest.theNurseryStopsAtTenTrees` falhou 1 vez em
  8 rodadas (9 árvores de 10). Não mexi; talvez seja a mesma instabilidade
  de `aVillageOnBedrockStillHasLots`.

## Plano de confiabilidade operacional (docs/superpowers/plans/2026-09-23-operational-reliability.md)

- [x] **Task 1 — elegibilidade unificada de trabalho:** ja commitada em sessao
  anterior (WorkAssignmentTest, 419/419 GameTests).
- [x] **Task 2 — peca de construcao parcial:** ja commitada em sessao anterior
  (ConstructionOutcomeTest, BuildProgressGameTest, 421/421 GameTests).
- [x] **Task 3 — politica e custo do scanner separados:** `ScanReport` e
  `ScanRefusalReason` (LOT/BED/ROAD/TERRAIN) por fatia de varredura;
  `PlannerTurns` delega ao novo `ColonyScanScheduler` reutilizavel. O
  GameTest `bedAndRoadRefusalsAreIndependent` tinha footprint de rua menor
  que a area do cenario e foi corrigido. `runGametest --rerun-tasks`:
  **422/422 GameTests**. Commit `a953489`.
- [x] **Task 4 — migracoes de save idempotentes:** `SaveMigration.migrate`
  roda antes de qualquer leitor em `ColonySavedData.readNbt`, com
  `saveVersion` monotonico. Escopo reduzido de proposito: `MineSave.SHAPE_VERSION`
  ficou intocado (decisao com o autor — ja testado em producao, semantica de
  descarte deliberado). `SaveMigrationTest` cobre idempotencia, normalizacao
  de `BREEDER`, e preservacao de versao futura. `runGametest --rerun-tasks`:
  **422/422 GameTests**. Commit `04a6f1e`.
- [x] **Task 5 — recuperacao pura de mina:** `MineRecovery.recover(Mine)`
  extrai a decisao (NO_ACTION/REROUTE/EXHAUST_MOUTH) que ja vivia dentro de
  `MineDigging.rerouteOrBlameTheMouth`. Vocabulario proprio, nao os nomes
  do plano original (decisao tomada com o autor). `runGametest --rerun-tasks`:
  **422/422 GameTests**. Commit `5a12ec3`.
- [x] **Task 6 — integrar recuperacao de mina sem reconstruir portal/arco:**
  ja estava implementada antes desta sessao. Conferido lendo o codigo real:
  `furnishAndLight` so roda em mina nova/boca oposta valida/boca nova apos
  helices esgotadas; arco so sobe uma vez; prioridade do carvao ja coberta
  por `MinerGameTest.priorityRunsFromCoalDownToTheRareOnes`. Nenhum codigo
  novo necessario; sem commit proprio.
- [x] **Task 8 — armazem fisico por snapshot:** `WarehouseIndex` (reserva
  por ciclo, `reserveBatch` ordenado por `SupplyPriority`, fotografia
  incompleta bloqueia tudo). `WarehouseIndexTest` (9 casos). ADR-024 (o
  plano pedia ADR-023, ja usado pela Task 2). Commit `a1599ac`.
- [ ] **Task 9 — investigada e NAO implementada, por decisao.** O
  cenario central do plano ("bau cheio pausa o produtor sem perder item")
  ja esta coberto por dois mecanismos corretos e apropriados ao contexto:
  `MinerWork.java` (reativo — encerra a tarefa quando `haul.stored()==0`,
  log "the chest that serves him is full", 2026-09-22) e
  `ColonySupply.craft` (preventivo — `firstWithRoomFor` verifica espaco
  ANTES de fabricar, recusa sem gastar ingrediente). Reescrever
  `ColonyChests`/`ChestInventoryReader`/`MinerHaul`/`ColonySupply` para
  rotear por `WarehouseIndex` trocaria codigo fisico ja testado por uma
  abstracao sem resolver defeito real observado. O terceiro ponto do
  plano ("route sticks, apples, and saplings") e o **E38 ja catalogado**
  (`TODO.md`, linha ~148) como decisao de projeto pendente, nao lacuna
  tecnica — fora de escopo de uma integracao mecanica de armazem.
  Revisitar `WarehouseIndex`/Task 9 se um playtest real mostrar duas
  tarefas reservando o mesmo estoque escasso no mesmo ciclo, caso que os
  dois mecanismos atuais nao cobrem.
- [ ] **Task 10 — investigada e NAO implementada, por decisao.** O plano
  pedia fixar `UUID.randomUUID()` por identidade fixa em
  `ShepherdGameTest`/`SmelterGameTest`/`ConstructionResumeGameTest` para
  resolver E4/E21. Conferido no codigo: nesses tres arquivos o UUID e
  sempre chave de mapa opaca (`Colony.create(UUID.randomUUID(), ...)`),
  nunca hasheado para derivar geometria/direcao — diferente do caso real
  ja corrigido em `SurfaceGatheringGameTest` (sessao anterior a esta).
  Fixar esses UUIDs nao mudaria determinismo nenhum. E4 e E21 foram
  investigadas e fechadas (ver a tabela de historico, entradas E4/E21)
  por falta de evidencia e obsolescencia, respectivamente — nao ha mais
  causa a diagnosticar que justifique `OperationalMatrixGameTest`.
- [x] **Task 11 — observar inventario da vila sem mutar crescimento:**
  `VillageInventory` (raio-x imutavel: adultos, camas, profissoes, obras
  completas/ativas, cobertura de bau, recursos observados) montado por
  `VillageInventoryObserver.observe`, que nunca chama
  `ConstructionPlanner.plan`. `ColonyProfession`/`ChestCoverage`
  espelham `ProfessionType`/`ChestSurvey` em `core/colony/model` —
  decisao tomada com o usuario, path exato do plano em vez de mover para
  `core/coordination`. `VillageInventoryObserverTest` (unit puro) nao
  foi criado: `observe()` depende de `ServerWorld` real, sem precedente
  de mock no projeto. Cobertura real no GameTest novo
  `observingInventoryDoesNotChangeHouseAlternation`
  (`FarmPlanGameTest`). `runGametest --rerun-tasks`: **423 testes,
  422/423** (unica falha e a intermitencia pre-existente conhecida).
  Commit `c6b4abf`.
- [x] **Task 7 — traco circular de atividade persistido:** `ActivityTrace`
  (16.384 eventos/colonia) persistido em `ColonySavedData` via
  `ActivityTraceSave`. **Limite de escopo conhecido:** so
  `WorkerStrikes.gaveUp` gera evento — e o unico ponto com UUID de
  trabalhador disponivel de fato. `IdleLog` (waiting/recovered, 45
  chamadores, por colonia+assunto sem trabalhador identificavel) fica de
  fora; se um dia fizer sentido rastrear "colonia sem executor" no traco,
  precisa de um desenho novo (workerId opcional, ou evento por colonia em
  vez de por trabalhador). `runGametest --rerun-tasks`: 421/422 em duas
  rodadas (unica falha e a intermitencia pre-existente conhecida). Commit
  `ae1bef6`.
- [ ] **Task 7 — traco de atividade persistido e limitado** (ActivityTrace,
  16.384 eventos por colonia).
- [ ] **Task 8 — contrato do armazem fisico** (WarehouseIndex, SupplyRequest,
  ADR-023).
- [ ] **Task 9 — observar baus e rotear pedidos fisicamente**
  (WarehouseObserver).
- [ ] **Task 10 — nao implementada.** Ver entrada detalhada acima: E4 e
  E21 foram investigadas e fechadas (falta de evidencia / obsolescencia),
  e o UUID dos tres testes que a task pedia fixar nao tem efeito sobre
  determinismo. Sem motivo remanescente para a task.
- [x] **Task 11 — observar inventario da vila sem mutar crescimento:**
  `VillageInventory`/`VillageInventoryObserver`, GameTest
  `observingInventoryDoesNotChangeHouseAlternation`. Ver entrada
  detalhada acima. Commit `c6b4abf`.
- [x] **Task 12 — endurance com seed fixa:** `EnduranceReport`/
  `LatencySummary`; E41 descoberto ja fechado (ver acima). Commit
  `d25faf4`.
- [x] **Task 13 — auditoria de exclusao e evidencia de release:**
  `RemovalAudit` (3 motivos, `PATIENCE_ABANDONMENT` cobre
  `WAITING_RESOURCES` e `BUILDING`), `scripts/release_manifest.py`
  (compara 3 hashes SHA-256, `--dry-run`). `runGametest --rerun-tasks`:
  **422/423** (unica falha e a intermitencia conhecida). Commit
  `e16d363`.
- [~] **Task 14 — verificacao completa, playtests de save e handoff de
  release.** Parte automatizada (passo 1) **feita**: `gradlew test`
  992/992 zero falha, `gradlew build` sucesso, `runGametest --rerun-tasks`
  **423/423 GAME TESTS COMPLETE** na rodada final. **Passo 2 (cinco
  playtests reais) continua pendente** — nao foi observado nesta
  sessao. **Passos 3-4 foram executados por decisao explicita do autor,
  antes dos playtests** — SHA-256 `C1244064...` copiado para
  `build/libs/`, `downloads/` e mods (as tres copias comparadas
  identicas), `release_manifest.py --dry-run` confirmou. Isso diverge
  do que o plano original pede ("only close a save playtest after
  observed user confirmation") e do que esta mesma entrega recomendava
  horas antes; registrado aqui para honestidade, nao como o caminho
  normal. Os cinco itens de playtest continuam sem confirmacao e devem
  ser verificados assim que possivel — ver `docs/proxima-sessao.md`.

**Atualizado:** 2026-09-24, peca de construcao sem apoio mantida pendente.

**Auditoria técnica:** [`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md).
Nesta sessao: uma obra que pede uma peca sem rota fisica no bioma recebe a peca
final no bau que atende o construtor. A verificacao percorre receitas Vanilla;
alternativas e cadeias locais, como `argila -> fornalha -> terracota`, continuam
sob responsabilidade dos oficios. A rodada atual de `runGametest` executou 410
testes, 408 aprovados; restam duas falhas independentes: alternancia de
`FarmPlanGameTest` e a fixture de terra de `SurfaceGatheringGameTest`. Os 76
testes Python da auditoria passaram nesta sessao.

## Entrega 2026-09-23 - telemetria, BigHouse e baus Vanilla

- [x] **P1.4 - telemetria de atividade:** `VC_ACTIVITY` registra espera,
  recuperacao, erro operacional e abandono por profissao, atividade e motivo
  controlado. O analisador le o formato, migra o historico para esquema 2 e
  inclui a tabela de atividades no relatorio.
- [x] **P1.4 - BigHouse no nivel da rua:** o NBT perdeu apenas a base original
  e desceu um nivel; as seis camas, seis baus e a porta foram preservados.
  `BigHouseModBlueprintGameTest` cobre dimensoes, contagem de blocos e porta.
- [x] **P1.4 - bau seguro por cama Vanilla:** no primeiro reconhecimento de
  vila, cada cama original do agrupamento so recebe bau quando quarto, porta,
  parede, suporte, tampa e orientacao forem inequivocos. A BigHouse e excluida;
  nenhum ciclo de trabalhador cria ou tenta novamente o bau, e o estoque
  publico ignora os baus privados conformes.
- [ ] **Playtest P1.4:** com uma vila Vanilla ainda nao adotada, confirmar que
  apenas quartos validos recebem um bau ao lado da cama, que a BigHouse nao e
  alterada e que `VC_ACTIVITY` aparece no `latest.log` apos espera/recuperacao
  ou abandono controlado.

## Playtest 2026-09-22, 20:07 — tres defeitos corrigidos, nenhum confirmado em jogo

- [x] 🔴 **Obra terminada recomecando:** `BuilderWork.placeOne` risca sem assentar (`ladder`, `wall_torch`, por *nothing holds it*), a obra e dada por terminada com `0 blocks placed`, e o reparo reencontra a mesma lacuna a cada trinta segundos. Como a vaga de obra e unica, o laco impedia qualquer construcao nova. O planejador compara os blocos de pe da abertura com os do fechamento e nao repete tentativa que nao avancou. `FoundationRepairGameTest.aRepairThatPlacedNothingIsNotOpenedAgain`.
- [x] 🔴 **Terracota branca sem rota real:** a familia tem rota na receita (argila → fornalha → terracota) e nao no mundo — o fundidor passou a sessao inteira sem achar argila. Passada a carencia de dez ciclos, a peca preferida e depositada no bau da obra. A constante saiu do log: cinco ciclos foi a espera mais longa atendida, vinte a que nunca foi. `BuilderGameTest.aRouteThatNeverDeliversStopsHoldingTheBuild`.
- [x] 🔴 **Mina cavada sem fim:** bau cheio, pedregulho no chao quarenta e sete vezes, mineiro alternando entre duas posicoes. O `took 0` nao era da picareta — a hipotese da ferramenta foi testada contra o log e revertida. Bau cheio encerra a tarefa. `MinerGameTest.aFullChestStopsTheMinerInsteadOfPilingStoneOnTheFloor`, verificado por mutacao.
- [ ] 🔴 **Playtest dos tres:** reabrir o save e confirmar que nenhuma obra terminada reabre, que a terracota aparece no bau e a obra anda, e que o mineiro para com o bau cheio. JAR instalado com SHA-256 `73deea9739...`.
- [x] 🟠 **P1.2 - peca sem apoio permanece parcial:** `ladder`, `wall_torch`
  e equivalentes nao saem da planta nem contam como progresso quando o apoio
  fisico falta. O projeto persiste motivo e assinatura do entorno; o construtor
  encerra a tarefa e o planejador so abre outra quando essa assinatura mudar.
  `ConstructionOutcomeTest`, `ConstructionProjectTest`, `ConstructionSaveTest`
  e `BuildProgressGameTest`.
- [ ] 🟠 **Playtest P1.2:** em um save, deixar uma peca sem apoio falhar,
  confirmar que a obra permanece parcial sem nova tarefa repetida, criar o
  apoio e confirmar que uma unica tarefa volta a abrir e conclui a peca.

## Fila operacional 2026-09-22

O relatorio atual, incluindo responsabilidades, fluxo de obra, suprimento por
bioma e estatistica do `latest.log`, esta em
[`docs/technical/Operational-Status-2026-09-22.md`](docs/technical/Operational-Status-2026-09-22.md).
Ordem canonica desta sessao: P0 fechar os GameTests residuais; P1 reproduzir as
repeticoes medidas de mina/obra/atribuicao; P2 reduzir a leitura repetida de
baus e medir o resultado em save; P3 endurance. Nao tratar contagem de log
como defeito confirmado sem contexto e regressao automatizada.

## Próximas atividades corrigíveis sem acessar o jogo

Esta fila separa falhas reproduzíveis ou coberturas que podem ser tratadas
com código e testes locais das validações que continuam dependendo de um save.

- [x] **Criador encerrado:** `SHEPHERD` assume vagas, fundação, tarefas, nome e baú do antigo `BREEDER`; saves antigos são migrados na leitura. Unitarios e GameTests de nome, bau, fundacao e trabalho do Pastor passaram.
- [x] **Cadeia da terracota da obra:** a peca exata continua preferida, mas a tag Vanilla de terracotas pode substitui-la; `CLAY` alimenta a fornalha para terracota neutra. O `CARPENTER` ainda fabrica o fermentador pela receita Vanilla quando houver haste e pedregulho; sem rota local para a haste, a politica de suprimento da obra entrega o fermentador final. Os tres GameTests de substituicao, cadeia de argila e bolas de argila falharam antes da correcao e passam depois.
- [x] **Suprimento de obra sem rota no bioma:** a construcao consulta sua familia de alternativas e a arvore de receitas Vanilla. Se nenhuma rota local existir, a peca preferida aparece no bau da obra quando demandada; se qualquer rota existir, ela permanece tarefa dos oficios. `BuilderGameTest` cobre fermentador sem haste de blaze e porta de carvalho em planicie.
- [x] **Inventario por bioma das plantas construtiveis:** `ConstructionSupplyAuditGameTest` le todos os 143 NBTs permitidos e registra, por estilo, as estruturas, recursos por rota local, itens automaticos e blocos formados no local. O resultado versionado esta em `docs/technical/Auditoria-2026-09-22-Suprimento-Estruturas-Vanilla.md`.
- [x] **P2.1 — leitura de baus do ciclo:** estoque, capacidade de `WOOD` e capacidade de `PLANKS` agora saem da mesma fotografia por ciclo; a varredura continua sem carregar chunks. `StorageGameTest.theSurveyKeepsCapacityForWoodAndPlanks` compara slots vazios, pilhas parciais e item do jogador com a regra de deposito anterior.
- [ ] **Medir P2.1 no save:** confirmar, pela linha `Colony cycle took`, se o ciclo que mediu 112 ms fica abaixo de 50 ms. O teste automatizado prova equivalencia funcional, nao milissegundos de uma maquina real.
- [ ] Playtest da migração: abrir save antigo, confirmar Pastor sobre a cabeça, tesoura no baú e continuidade das tarefas; testar Tocha das Almas dentro de uma obra real e observar liberação da fila, sem esperar demolição.
- [x] **P0.8/P1.2 — pegada da `BigHouseMOD` reservada:** a busca e a retomada recusam qualquer lote que use uma coluna horizontal da fundacao, mesmo em outra altura ou com projeto pendente. `BuildSiteGameTest.noProfessionLotCanUseTheBigHouseFootprint` falhou antes da correcao e passou depois.
- [x] **P0.8/P1.2 — BigHouse fundacional nao vira reparo profissional:** o save real tinha projetos `big_house_mod` na mesma origem de casas concluidas. O reparador ignora essa estrutura exclusiva, e a retomada descarta o reparo antigo antes de tocar no terreno. Dois GameTests falharam antes da correcao e passaram depois; um terceiro preserva o reparo de casas profissionais.
- [ ] Playtest da pegada e do reparo: reabrir o mesmo save e confirmar que o projeto `big_house_mod` antigo desaparece, a casa permanece intacta e o proximo lote profissional nao usa nenhuma coluna de sua pegada. Nao cancelar manualmente a fundacao.

- [x] 🔴 **P0.9 — catálogo e terreno seguro:** as construções do mod agora usam somente a whitelist explícita de ids Vanilla reais por bioma; areia e relva são coletadas pelo fundidor quando uma obra pede o recurso, enquanto terra comum virou solo do fazendeiro com raio protegido ampliado; bocas de mina rejeitam água, exigem entrada livre e preferem a posição seca mais distante/elevada. `VillageStructuresGameTest`, `SurfaceGatheringGameTest` e `MinerGameTest` passaram.
- [x] 🔴 **P0.10 — reparo cíclico de obras:** depois de concluir, abandonar ou liberar uma obra parada, o planejador varre construções registradas pelo mod, recompõe a planta a partir dos blocos que ainda existem e tenta fechar a incompleta antes de abrir outra. Uma tentativa sem avanço cede uma passagem e é repetida depois, sem travar a vila. `ConstructionProjectTest` e `BuildingRegistryTest` passaram.
- [x] 🟠 **P0.11 — reserva de árvores da vila:** agricultor e lenhador compartilham o viveiro do bioma, plantam em terra enraizada no anel distante de 48 a 56 blocos do centro e param ao atingir dez árvores marcadas. `TreeNurseryGameTest.theNurseryStopsAtTenTrees` passou.
- [ ] Playtest P0.10/P0.11: confirmar no save que uma obra abandonada é retomada sem duplicar blocos, que uma tentativa impossível não congela a fila e que cada vila mantém dez árvores fora do centro.
- [x] **Residual de `FarmPlanGameTest.thenextturnafterahouseisnonresidential` fechado em 22-09:** o defeito era do cenário, não de `HousePlans`. O teste registrava a casa anterior só no registro de construções; desde o reparo cíclico (P0.10), `ConstructionPlanner.plan` roda `BuildingRepairPlanner.open` antes da alternância, e o reparo adotava essa casa sem blocos (0 de pé contra 151 da planta) e a devolvia como obra nova. O cenário agora assenta a planta no mundo antes de planejar. Três rodadas de `runGametest`: 409/410 antes, teste verde nas duas seguintes, a última 410/410 completa. A regra de alternância não foi tocada.
- [x] Residual de `SmelterGameTest.theOreInTheMineMouthChestIsCountedAndSmelted`: não repetiu na rodada completa de 21-09; manter a fixture em observação antes de alterar a coleta do fundidor.
- [x] 🔴 **P0.8 — fundação absoluta da vila:** toda vila detectada cria a `BigHouseMOD`, cópia editada da big house Vanilla sem móveis/decorações, com seis camas e seis baús distintos. Os seis titulares (`MINER`, `LUMBERJACK`, `MASON`, `SMELTER`, `SHEPHERD` e `BUILDER`) recebem adulto, cama `HOME` e baú dentro dela; `FARMER` e `CARPENTER` continuam profissões ativas, mas sem cama/baú fundacionais na casa, e entram normalmente no crescimento. A Vanilla permanece intacta. `VillageFoundationGameTest` passou.
- [ ] Playtest P0.8: entrar em um save com vila recém-detectada e confirmar a `BigHouseMOD`, os seis aldeões, suas camas, seus baús e a ausência de sobreposição com estruturas existentes.
- [ ] 🔴 **E42 — impasse entre profissões:** criar o GameTest da roça fora do alcance do fazendeiro, com duas passagens do planejador, e corrigir a fila se a segunda passagem não abrir o projeto de casa.
- [x] 🟠 **E43 — descanso respeitado:** a decisao 1A torna uma capacidade em descanso inelegivel para toda reserva de `WorkAssignment`; `WorkAssignmentTest` falhou antes da retirada do fallback e `ColonyCycleGameTest.aRestingMinerLeavesTheStoneTaskAvailable` protege o registro usado em jogo. A rodada completa passou com 419/419.
- [ ] Playtest E43: com o JAR desta entrega, provocar uma desistência do mineiro e confirmar que `COLLECT_STONE` fica disponivel pelos quatro ciclos de descanso antes de nova reserva.
- [x] 🟠 **P1.1 — trabalhador ocioso sem `COLLECT_STONE` ou `CRAFT_WOOD`:** `CraftingGameTest.theCycleOpensTheCraftingTaskByItself` cobre `CRAFT_WOOD`; `MinerGameTest.theCycleAssignsStoneToTheMinerAndItReachesTheChest` parte de um mineiro ocioso e prova pedido, reserva para o oficio correto e entrega no bau, sem tarefa criada pelo cenario. A bateria passou com 414/414.
- [x] 🟠 **Intermitencia de `SurfaceGatheringGameTest` fechada em 22-09.** A falha era da fixture: alvo e trabalhador nasciam 65 ou 97 blocos fora da arena, numa direcao derivada de `UUID.randomUUID()`, e por vezes a entidade nao era registrada pelo mundo de teste. Os quatro cenarios agora mantem alvo e trabalhador na arena; somente o centro e o bau da colonia ficam alem do raio protegido. UUIDs fixos fazem o fallback de setor apontar para leste. Tres `runGametest --rerun-tasks` consecutivos passaram com 413/413; os quatro GameTests seguem cobrindo coleta e raio protegido.
- [ ] ⚙️ **E38 — resíduos no inventário pessoal:** definir o destino sustentável de varas, maçãs e mudas antes de alterar armazenamento ou descarte.
- [x] 🟠 **E41 — endurance: fechada em 11-09, esta entrada estava desatualizada.** `ColonyEnduranceGameTest.theColonyDoesNotAccumulateAcrossTwoHundredCycles` (P1.13) roda 200 ciclos com baú vazio como fonte de trabalho infinita, mede deriva (não valor absoluto) em quatro contagens — total de tarefas, fila aberta, obras registradas, baús registrados —, descontando 50 ciclos de aquecimento. Nunca falhou em nenhuma das onze rodadas completas de `runGametest --rerun-tasks` desta sessão (24-09).
- [x] 🟠 **P1.3 — casas consecutivas:** o log mostrou `house → house`. `HousePlans` agora alterna `casa → tipo não residencial A → casa → tipo não residencial B`, exclui o tipo A anterior e mantém todas as famílias sob o mesmo `BuildSiteScanner`; `HousePlansTest` e o GameTest de rotação passaram.
- [ ] Playtest P1.3: confirmar no save a sequência casa, infraestrutura A, casa e infraestrutura B diferente de A.

- [x] 🔴 **E44 residual — perna da boca acima do degrau:** o log confirmou que o recálculo de `job.approach` não limitava a perna efetivamente entregue por `WorkTargets`. `MinerWork` agora escolhe um patamar pisável dentro de `CLIMB`; `MinerApproachGameTest` cobre a queda abaixo da boca e a suíte passou com **379/379 GameTests**. Falta confirmar no mundo que o mineiro volta a entregar pedra.
- [x] 🔴 **P1.2 — obra retomada sobre estrutura existente:** `BuildSiteScanner` agora rejeita interseção com peças de estruturas Vanilla da vila, blocos físicos ocupados e volumes já registrados, inclusive durante a retomada de projeto salvo. `BuildSiteGameTest.theResumedProjectRejectsAnOccupiedVolume` reproduz o caso físico.
- [x] 🔴 **P1.2 — zona sobre construção existente:** a seleção consulta peças Vanilla referenciadas pela `StructureAccessor`, inclusive quando o início está em outra chunk, e a `BigHouseMOD` rejeita telhados ou outros blocos de construção como camada de apoio. `BigHouseFoundationGameTest.theFoundationDoesNotStartOnTopOfAnExistingBuilding` reproduz a falha que aceitava o lote.
- [x] 🔴 **P1.2 — projeto pendente reserva o lote no carregamento:** o save agora participa da consulta de sobreposição antes de `ConstructionPlanner.resume`, e `BigHouseFoundation.ensure` não duplica uma `BigHouseMOD` ainda pendente. `BigHouseFoundationGameTest.aPendingProjectStillReservesItsSavedBox` e `aPendingBigHouseIsNotPlacedAgain` cobrem a janela observada no log de 21-09.
- [x] 🔴 **P1.2 — volume vertical do lote:** a varredura agora testa todas as colunas a partir do nível-base comum da obra; degraus, blocos voando e restos de outra construção dentro da caixa real recusam o lote antes de iniciar a casa. `BuildSiteGameTest.anElevatedColumnInsideTheBaseRefusesTheLot` reproduz a falha e passa após a correção.
- [x] 🔴 **P1.2 — folga vertical absoluta de 25 blocos:** nenhum lote, projeto pendente ou fundação da `BigHouseMOD` aceita um bloco físico na janela completa acima da pegada. `BuildSiteGameTest.aBlockTwentyFiveAboveTheLotRefusesTheLot` e `BigHouseFoundationGameTest.theFoundationRejectsABlockTwentyFiveAboveTheFloor` cobrem o limite.
- [x] 🟠 **P1.2 — cancelamento manual por Tocha das Almas:** colocar uma Soul Torch dentro de uma obra profissional cancela a zona, o projeto, as tarefas e o registro parcial, liberando o ciclo; a `BigHouseMOD` não é cancelada. `ConstructionCancellationGameTest` cobre os dois contratos.
- [x] 🔴 **P1.2 — obra não desaparecia ao colocar o último bloco:** a tarefa reservada agora entra em execução antes de concluir, então o último bloco marca `BUILD` como `COMPLETED` em vez de liberá-la para o próximo ciclo. O evento de mudança também prioriza a Soul Torch antes de filtrar edições do próprio mod; `BuilderGameTest` e `ConstructionCancellationGameTest` reproduzem os dois sintomas.
- [x] 🟠 **P1.2 — `BigHouseMOD` fora do catálogo profissional:** a fundação obrigatória continua nascendo com a vila e não pode ser escolhida como casa/oficina por profissões. `HousePlansTest.theModBigHouseIsNeverAProfessionBuild` fixa a regra.
- [x] 🟠 **P1.2 — frente arenosa cíclica:** `MinerWork` mantém a posição após cada quebra, espera a queda de areia/gravilha assentar e só então reavalia a frente. A coleta integral continua em `MinerHaul.deposit`, inclusive para overflow no chão. Falta confirmar a progressão e a entrega no save.
- [ ] Playtest P1.2: iniciar uma obra perto de construções existentes e acompanhar o mineiro no deserto até confirmar que não há acavalamento, que a frente avança após os assentamentos e que os drops chegam ao baú/overflow.
- [ ] Playtest P1.2: reabrir o save com projeto pendente e confirmar que a zona antiga permanece reservada, a `BigHouseMOD` não é duplicada e nenhuma obra nova é salva sobre ela.
- [ ] Playtest P0.9: criar vilas nos cinco biomas e confirmar no save que somente as estruturas listadas aparecem, que areia/relva são buscadas longe da vila quando solicitadas e que a entrada da mina permanece seca e acessível.

## Prioridade atual — obra aberta impede a próxima construção

- [x] Revisar os dois commits locais: espera entre ofícios (`e37708c`) e recálculo de aproximação do mineiro (`b7ef9e2`). `fetch` e `pull --ff-only`: nenhuma novidade remota; alterações locais preservadas.
- [x] Completar o guarda de obra em `BUILDING` sem progresso: liberar a vaga após 12.000 tiques de expediente, preservando a construção parcial e seu lote. Descontar a noite sem zerar a espera; renovar o prazo quando uma peça é assentada.
- [x] Corrigir e registrar `BuildProgressGameTest`: API de marcação, relógio de idade do mundo, conclusão dos testes e quatro cenários (parada, progresso, noite e mudança de horário). Regressão noturna reproduzida antes da correção.
- [x] Corrigir a escrita do relatório de `ChainRootsGameTest`, que falhava em `createDirectories(null)`. Suíte final: **376/376 GameTests**.
- [x] Encerrar tarefas `BUILD` no abandono da obra e limpar job/destino do construtor. Limpar o destino também quando uma tarefa encerrada é vista no tick. Dois novos GameTests reproduziram as falhas antes da correção; `WorkerLossGameTest` distingue morte (devolve tarefa) de abandono (cancela). Suíte posterior: **378/378 GameTests**.
- [ ] Playtest: confirmar recuperação da fila, preservação do lote e segunda casa. Comparar `SweepLog.busy` com passadas; não declarar a varredura resolvida pelo contador antigo.
- [ ] Playtest dos commits locais: estabilidade de ofício e entrega de pedra depois de limitar também a perna real da boca ao próximo patamar.
- [ ] Investigar a falha histórica de `ColonyDetectionGameTest` (24/30), não reproduzida na execução de 09-20.

---

## 🔴 P1.0 — Nenhum lote aprovado: a vila não chega a planejar obra

Playtest de 2026-09-17, 00:09. O autor: *"não vi as zonas de construção
marcadas, não vi casa crescendo"*. E o log concorda: **`planned` = 0**,
`opened a build task` = 0 em 6 minutos, com 8 pedidos de lote.

```text
lot refusals: 174912 candidates turned down
  92972 (53%) the ground is inside a reserved road area
  33597 (19%) village-original or player-placed, Rule 3 protects it
sweep: 8 planner runs, 2055 columns, 0 complete rounds
```

**É anterior ao E46:** aquele conserta a obra que morre depois de nascer, e
aqui nenhuma nasceu. A causa suspeita é `BuildSiteScanner.isRoadArea`, que
trata **todo calçamento original da vila** como reserva de estrada — com a
Regra 3, 72% do território fica bloqueado.

⚠️ **Duas leituras, e a sessão não separou:** `0 complete rounds` também
explica tudo (a varredura precisa de ~17 ciclos e teve 6). Ver
`lento-nao-e-travado` — este diagnóstico já foi errado antes.

- [x] ✅ **Instrumentar** quantas colunas sobrevivem a todas as recusas — `LotRefusals.accepted`, com linha própria no relatório. **Verificado:** build + 902 unitários + 336 GameTests em 5/5. Gametest `theSurvivingColumnsAreCounted` conferido no XML.
- [x] ✅ **Playtest de 42 min** — a resposta foi **zero**: `0 survived every check, 960672 were turned down`. Não é orçamento de varredura; a vila não tem chão que sirva. Distribuição idêntica à da sessão curta, então não é amostra pequena.
- [x] ✅ **Decidido pelo autor (opção 1):** a reserva vale só para a rua que a colônia calçou. `isReservedAgainstLots` é nova e substitui `isRoadArea` **só na recusa de lote** — a função antiga continua servindo aos três usos positivos, senão a colônia deixaria de reconhecer as ruas Vanilla. **Verificado:** build + 902 unitários + 337 GameTests em 5/5.
- [x] ✅ **Playtest do JAR novo (08:23)** — **abriu chão: 313 colunas aprovadas**, contra zero antes, e **uma obra foi planejada**. A reserva caiu de 54,8% para 49,6% das recusas.

---

## 🔴 P1.1 — A obra espera uma peça que ninguém fabrica (playtest 08:23)

A biblioteca nasceu às 08:43 e passou **8 minutos em `WAITING_RESOURCES` com
628 de 628 blocos**, esperando `cobblestone_stairs`. A colônia tinha **69
`COBBLESTONE`** no baú.

```text
Builder … stopped — no minecraft:cobblestone_stairs in the colony chests
no collect_stone work: no worker in the village can do it — COBBLESTONE needs COLLECT_STONE
no craft_wood_material work: no worker in the village can do it — OAK_PLANKS needs CRAFT_WOOD
```

**Não é falta de material — é falta de quem transforme.** Estoque: 4.258
tábuas, 1.686 toras, 69 pedregulhos. É o laço fechado que
`analise-plano-crescimento.md` já descrevia em 09-12.

- [x] ✅ **Corrigido em 09-17** (`4cee104`). A causa: `ColonyCycle.requestMissing` itera `Map<ResourceType,Integer>`, e `cobblestone_stairs` **não é um `ResourceType`** — escada, porta e cerca são peças da planta, não recursos contados. Agora a obra abre a tarefa **pela peça que espera**, e quem decide se dá para fazer é o jogo (`ColonySupply.canProvide` → `CraftingLookup`), não uma lista. **Verificado:** build + 902 unitários + 338 GameTests em 5/5. ⚠️ sem playtest.
- [ ] 🔴 Confirmar em jogo que o P1.1 destrava o pedreiro e o construtor
- [ ] 🟠 `no worker in the village can do it` para COLLECT_STONE e CRAFT_WOOD com trabalhadores ociosos — pode ter a mesma raiz, não foi investigado

---

## 🟠 Revisão completa de 2026-09-17 — o que ficou sinalizado

Inventário em
[`docs/technical/Revisao-2026-09-17.md`](docs/technical/Revisao-2026-09-17.md).
**391 arquivos `.md`, 40.838 linhas, 23 documentos parados desde 08-08.**

**Decisões do autor, pendentes** (o pedido foi sinalizar para decidir depois):

- [ ] 🔴 Destino de `Class-Architecture.md`, `Fabric-Implementation-Plan.md`, `Data-Model.md` — descrevem `ColonyManager`, `TaskManager`, `BuildingStatus` e outras classes que **nunca existiram**. Recomendado: `docs/historical/`.
- [ ] 🟠 Destino de `MVP.md`, `MVP-Tasks.md`, `START_PROJECT.md`, `Development-Roadmap.md` — planos concluídos ou superados
- [ ] 🟠 **`STATE.md` tem 1.193 linhas** contra o teto de 150 que ele mesmo declara. Precisa de poda.
- [ ] 🟠 `ConstructionService.forget` sem chamador em `src/main` — confirmar se é gancho ou resto
- [ ] 🟠 **Pastor tem 2 gametests; mineiro tem 79.** As profissões calmas são as menos protegidas, não as mais sólidas.
- [ ] 🟠 Playtest que exercite **fundidor** e **pastor**

---

## 🔴 P1.2 — Água trava o mineiro (pedido do autor + confirmado no log)

O autor: *"quando aldeão encontra água ou lava ele se perde e trava, ele
deve tentar fechar o bloco que gerou a água ou lava rapidamente e trocar de
caminho"*.

**Confirmado:** 15 desistências com `the place to stand is …, which is
flooded`, e `flooded` 30 vezes no log de 28 min. O aldeão fica parado até o
guarda de imobilidade expirar (300 tiques) e devolve a tarefa.

**O que já existe:** `MineDigging.flooded` vira o ramal
(`The branch turns away from the water`, 5×). **O que falta:** tapar o bloco
que verte e seguir, em vez de esperar o guarda. Lava não apareceu
(`lava` = 0).

- [x] ✅ **Corrigido em 09-17.** A pesquisa mostrou que *tapar a fonte e virar o ramal* **já existia** desde 2026-09-03 (`MineFlooding.seal` + `MineDigging.flooded`) e funciona — agiu 5 vezes no log. O que faltava era o **outro caso**: a água que já estava lá antes de a picareta bater, com o aldeão a 11 blocos andando para um `place to stand` alagado. `BuilderApproach.isDry` recusa na escolha. **Verificado:** 340 GameTests, 4/5 rodadas.
- [x] ✅ **Lava entra pela mesma porta** — `getFluidState` não distingue.
- [ ] 🟠 Confirmar em jogo que `which is flooded` caiu dos 30 medidos

---

## 🔴 P1.4 — A escolha de área: a varredura nunca fecha a volta

Varredura completa do subsistema em
[`docs/technical/Escolha-de-area-varredura-2026-09-17.md`](docs/technical/Escolha-de-area-varredura-2026-09-17.md).

**O achado que manda:** a vila varreu **22.116 colunas** em 27 passagens —
mais que os **16.641** de uma volta inteira — e ainda marcou
`0 complete rounds`. O reinício por deriva de centro custou a volta toda.

- [x] ✅ **S5** — `LotRefusals.clearAll` entrou no ciclo de vida. Era o único dos cinco vizinhos que ficava de fora, e a assimetria piorou quando o `ACCEPTED` entrou nele hoje.
- [x] ✅ **S3 verificado, já funciona** — `coloniesNearPlayers` faz a vila observada furar a fila do rodízio. Derruba metade do custo estimado: 32 `planner runs` em 32 min é vez quase todo ciclo. **Nada a fazer.**
- [x] 🟢 ~~**S1** — transladar o cursor em vez de descartar~~ — **descartado pela aritmética**, e o motivo está no §4.1: com deriva de 40, o anel seguro no centro novo é **0**. Translação não recupera nada.
- [x] ✅ **A deriva do centro: investigada, e NÃO é defeito.** A colônia foi de **37 para 69 camas** — a sonda descobriu quase o dobro da vila e o centro se mudou para o meio verdadeiro, uma vez só. Só a sonda move o centro (ADR-003 Emenda 4), e ela parte do centro e a ele volta. **O movimento estava certo; o preço é que estava errado.**
- [x] ✅ **Conserto aplicado: o cursor cai, o índice fica.** `ColonyRoads.rebasedTo` reancora o índice de ruas no centro novo e descarta só as colunas fora do raio. O cursor é anel **relativo** e vira lixo; o índice guarda **coluna absoluta**, e rua não deixa de ser rua. Custava **16 de 32** consultas. **Verificado:** 905 unitários (+3), 340 GameTests em **5/5**.
- [ ] 🟠 **C3 do E46 fica aberto por outro motivo:** o `colony.center()` fica a 77 blocos do aglomerado de camas que a detecção **vê e recusa adotar** (`view not provably complete`, 50×). Isso é diferente da deriva — é o centro que **não** se move quando deveria.
- [ ] 🟠 **S6 → S4** — duas réguas de chão: a **estrada** usa `isNaturalGround` (5 blocos) e o **lote** usa `isLotGround` (qualquer sólido). A estrada é mais exigente que a casa. Medir antes de unificar.
- [ ] 🟡 `BuildSiteScanner` tem **1.509 linhas** contra o teto de 500 do `CLAUDE.md`, com 36 métodos e 5 mapas de estado

⚠️ **Correção de afirmação minha:** na sessão anterior escrevi que *"a vila
não consegue estender estrada"*. **Errado** — `extended/grew the road` = 10
contra 3 recusas. A estrada cresce.

---

## 🟢 B1 — Cortador de pedra (melhoria de economia, 09-17)

`CraftingLookup` não lia `RecipeType.STONECUTTING`, e o pedreiro recebe
`Items.STONECUTTER` do `ChestMarker`. A escada de pedregulho custava **6→4**
na bancada; no cortador é **1→1**.

- [x] ✅ `cutFor` + `cheaperOf`: os dois caminhos são consultados e vence o que gasta menos **por peça**. Empate fica com a bancada.

---

## ✅ P1.3 — Ciclo finito da mina (2026-09-23)

- [x] **Geometria decidida pelo autor:** cada nivel abre um caracol de 10
  degraus, limpa 50 blocos, abre 4 ramais de 10 degraus e limpa 50 blocos em
  cada ramal. Somente um mineiro recebe a escada e a area comuns; os quatro
  ramais abrem depois dela. `MineShaftTest` e `MineTest` fixam os limites.
- [x] **Veio continua prioritario:** `MineArm.followVein` e `OreVein.beside`
  mantem a coleta do veio antes de voltar a ordem geometrica; os GameTests de
  prioridade de minerio passaram na rodada completa.
- [x] **Fim no limite:** o nivel mais fundo e esgotado, a nova boca e buscada
  estritamente no lado oposto da vila e a mesma abertura nao e recriada.
  `MineRegistryTest`, `MineSaveTest` e `MinerGameTest` cobrem o encerramento,
  a migracao da forma 6, o portal quebrado e a direcao oposta.
- [ ] **Playtest P1.3:** acompanhar uma mina de ponta a ponta no save: um
  caracol unico, quatro ramais apenas depois da area comum, foco em veios,
  portal quebrado sem reconstruir e troca de boca no lado oposto ao chegar ao
  fundo. A sessao de 23-09 ainda usou o JAR de SHA-256
  `446554572D466B748107D5CD65A4687BAD4BFF535F8C028F6158501BDE4AB2A0`; repetir
  somente apos instalar o artefato atual de SHA-256
  `62FCECB70ACF7864DA852F707A1ADBA2197FEC8613A952A2E691DE7BE13BF1EF`.

⚠️ **Expectativa honesta:** `dirt_path` não é bloco sólido cheio e continuará
caindo em `NOT_NATURAL_GROUND` adiante. Quem passa a poder virar lote é o
calçamento de **gravel e terracotta** — quanto disso existe na vila do
playtest, ninguém mediu ainda. **O efeito pode ser bem menor que os 54,8%.**

- [ ] 🟠 **A vila também não estende estrada** — `none of the road ends this colony can see may be paved`, 23 tentativas na sessão de 42 min. Sem lote a regra manda crescer a rua, e a rua não cresce: é um círculo fechado, e vale investigação própria.

---

## ✅ P0.8 — E45: a mina presa na boca (CONFIRMADO EM JOGO 09-17)

**O conserto funcionou.** Playtest de 00:09: `Miner … took` = **37** (era
**0**), `hit stone with nowhere to stand` = **15** (era 17.518), e
`turning the helix` = **4** — o guarda novo girou a hélice em vez de travar.
Primeira sessão desde 09-15 com pedra saindo do mundo.

Reproduzido no playtest de 21:41–22:12: 17.518 recusas em 31 min, 10/s,
**zero pedra quebrada** (em 03:44 foram 166.559 em 4h40 — mesma assinatura).

⚠️ **A causa mudou na revisão de 22:30.** Não é o reinício por
`deepenIfEveryOpenArmIsDone`/`restartAt` — o log prova que esse caminho
**nunca é alcançado**. O laço é a **mesma passagem repetida**: o braço fecha
em 8 recusas dentro de um orçamento de 64, `claimArm` o solta, e a passagem
seguinte o reocupa no mesmo `cut`. Detalhe em
[`docs/technical/E45-mina-presa-na-boca.md`](docs/technical/E45-mina-presa-na-boca.md).
**Nada foi alterado no código.** Ordem revisada:

- [x] ✅ **C4 + C2** — **corrigidos em 09-16. Decisão do autor: (b) com (a) como escalada.** `Mine.turnsWithoutAPickaxe` conta as voltas sem pedra, mora na mina (o braço é solto a cada passagem) e **só a picareta zera**. Cheias 3 voltas → gira a hélice com o `rerouted()` existente; esgotadas as 4 → boca nova; sem boca melhor → `exposedStone`. **6 testes novos**, e o principal fecha e reinicia os ramais exigindo que a conta sobreviva. **Verificado:** build + 902 unitários (0 falhas) + 335 GameTests em 4/5. ⚠️ sem playtest.
- [x] ✅ **C1** — resolvido junto: o giro e a escalada emitem linha própria (`turning the helix from … to …`, `tried all 4 helices …`), e o laço deixou de ser mudo.
- [x] 🟢 ~~**C3** — `everyOpenArmIsDone`~~ — **retirado:** premissa refutada pelo log, e mexer ali arriscaria o limbo de 09-04

**Defeito de fundo a tratar junto:** `stall`, `still` e `adrift` ficaram
zerados nas duas sessões — o caminho de falha zera os contadores no mesmo
tique. Terceira volta do mesmo laço; os consertos anteriores fecharam portas
específicas, e o guarda genérico nunca disparou em nenhuma das três.

---

## ✅ P0.9 — E46: a obra nasce condenada (C4 corrigido 09-16, espera playtest)

Playtest de 21:41. A biblioteca foi planejada **duas vezes** e as duas
morreram em ~30 s com **628 blocos restantes de 628**, sem um bloco posto.

**O C2 mudou a causa raiz.** Não é a divergência quadrado/círculo — as obras
estavam fora das **duas** réguas. É o **índice de ruas sem teto de raio**:
`findAmongRoads` percorre o índice inteiro sem consultar o raio, e
`remember` acrescenta qualquer rua nova. A vila calçou estrada 31 vezes, e o
lote passou a nascer de onde a estrada chegou. Prova: `40 answered by the
index — 0 by drift`, do `SweepLog`. Detalhe em
[`docs/technical/E46-obra-nasce-condenada.md`](docs/technical/E46-obra-nasce-condenada.md).

**É independente do E45** — corrigir o mineiro sozinho não faz a vila
construir.

- [x] ✅ **C2** — instrumentado: a linha `planned …` diz o centro e as duas distâncias; o scanner emite `WARN` ao servir lote de fora do raio. **Verificado:** build + 886 testes, 0 falhas (XML conferido). ⚠️ sem gametest e sem jogo.
- [x] ✅ **C4** — **corrigido em 09-16. Decisão do autor: (c)** — o scanner estava certo, o guarda errado. A estrada é projetada para sair do raio (`RoadExtension` ordena as pontas da mais distante para a mais perto), e o raio de 64 é da **detecção de vila**, não um limite de crescimento. O guarda passou a medir da **rede de ruas** (`ColonyRoads.blocksToTheNearestRoad`), larga a obra a mais de 16 blocos de qualquer rua, e **sem índice cai na régua antiga**. **Verificado:** build + 896 unitários (0 falhas) + 335 GameTests em 4/5 rodadas. ⚠️ sem playtest.
- [ ] 🟡 **C1** — **rebaixado pelo C4.** O círculo só governa agora o caso **sem índice de ruas**; no caminho normal quem decide é a distância à rua, em quadrado. A divergência continua no caso residual, mas os 21% já não descrevem a vila em operação.
- [ ] 🟠 **C3** — o centro que a detecção recusa mover (59× `view not provably complete`, centro a 77 blocos do aglomerado real). **Não causou o E46.** ⚠️ investigação própria — afrouxar a regra de completude ressuscita o E2.
- [x] 🟢 ~~`isSupersededBy` do planejador~~ — **descartado:** `drops the untouched` = 0
- [x] 🟢 ~~`PatienceClock` / obra sem material~~ — **descartado:** `WAITING_RESOURCES` não aparece no log, e a paciência é de 10 min contra 29 s observados
- [x] 🟢 ~~`ConstructionService.forget` chamado de outro lugar~~ — **descartado:** único chamador é `WaitingWork:190`
- [x] 🟢 ~~o scanner partir de outro centro~~ — **descartado pelo C2:** parte de `colony.center()`, e `0 by drift`

---

## 🟠 P1 — Village Growth Planner (especificado 09-16, nada implementado)

A vila que **nunca termina**: diagnóstico do que já existe → `NEED_SCORE`
por estrutura → estrutura → lote → construção, em ciclo permanente. Não uma
fila fixa que acaba na vigésima casa. Especificação inteira do autor em
[`docs/technical/village-growth-planner.md`](docs/technical/village-growth-planner.md).

A [ADR-009](docs/decisions/ADR-009-Autonomous-Village-Evolution.md) já
estabelece o princípio desde 08-22 e **não é contradita** por isto. A lacuna
real é concreta: `HousePlans.houseFor` escolhe a planta **pelo tamanho** (a
maior que cabe), e não por necessidade da vila.

- [ ] 🟠 **Playtest primeiro** — confirmar E45 e E46 antes de construir por cima
- [ ] 🟠 ADR do Growth Planner: onde mora, como se liga ao `ConstructionPlanner`, o que acontece com a Regra 25
- [ ] 🟠 `VillageInventory` (Estágio 0) — o diagnóstico do que a vila já tem. É `core` puro e testável sem servidor
- [ ] 🟡 `NEED_SCORE` — substitui `houseFor` como quem escolhe o alvo
- [ ] 🟢 Estágios de desbloqueio e expansão permanente, em fatias

---

## 🟠 Achados de fundo do playtest de 21:41

- [ ] 🟠 **36 colônias ativas**, `Colony cycle took` até **486 ms** — 40 ciclos acima de um tique do servidor. O custo do ciclo cresce com o número de colônias, e o playtest carrega muito mais que os cenários de teste.
- [ ] 🟠 **Só uma colônia planeja** (`9da5460c`); as outras 35 dão `assigned 0 tasks (0 open)`. Conferir se é esperado — é o padrão de `roca-sem-lote-trava-a-vila`.

---

> **Este arquivo é a lista viva.** Só o que está aberto agora.
> O histórico — sessões por data, ciclos fechados, erros resolvidos — está
> em [`TODO-archive.md`](TODO-archive.md).
>
> Onde este arquivo e o `Backlog.md` discordarem, **vale este**.
> Onde este arquivo e o `Plano-de-Correcao.md` discordarem sobre *o que já
> foi feito*, vale este. Sobre *o que fazer e em que ordem*, vale o plano.

---

## Plano de trabalho por lotes — 2026-09-14

Plano solicitado para manter mineiros em atividade, variar as estruturas
tentadas pelos construtores, ampliar a avaliação de espaços e revalidar após
falhas repetidas: [plano completo](docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md).

**Lote 1 concluído em código:** emenda da ADR-012 e reconciliação local de
alterações do jogador no scanner; 324/324 GameTests e `build` passaram. Ainda
aguarda validação em jogo. **Lote 2 teve a primeira correção integrada:** job
fechado libera imediatamente a claim do ramal do mineiro; o novo
`MinerWorkLifecycleTest` prova a limpeza no mesmo tique. A
continuidade/recuperação restante segue sujeita a revisão do autor; lotes
seguintes cobrem variedade dos construtores, estratégias de avaliação de
espaço e revalidação escalonada.
“Continuar trabalhando” respeitará expediente, recursos, perigo e chunks
carregados, sem criar recursos ou tarefas fisicamente impossíveis.

**AUD-001 continua aberto:** a asserção Linux histórica foi recuperada e a
contraprova confirmou que o teste acusa a ausência real de coleta quando a
regra de terra é desligada. A suíte local fresca passa 324/324; porém a perda
do aldeao entre `spawnEntity` e o tick 1 ainda nao tem causa deterministica,
e a execucao Linux da revisao ainda nao existe. Nao fechar o Lote 0 nem
aumentar timeout por este achado; P0.7 e uma decisao independente ja entregue.

---

## ⏭️ Por onde começar

**P0.7 entregue em codigo e no JAR, espera playtest.** A politica aceita qualquer piso
solido disponivel, sem taxonomia geologica e sem terraplanagem. Os unicos
materiais de estrada sao `dirt_path`, `gravel` e `terracotta`, e so bloqueiam
o lote dentro de `ROAD_AREA`. A decisao e a verificacao de 327/327 GameTests
estao na ADR-017. O JAR 0.3.0 atual tambem inclui a liberacao imediata de
claim do job mineiro encerrado; foi distribuido em 2026-09-15 com SHA-256
`C5D0790F996082CE3B7D2AA55CED93936DF04063568A03B0B521F50245A0BA1A`.

**E a varredura não era a culpada — o instrumento do projeto disse isso por
escrito.** O `SweepLog` gravou no encerramento:
`47 planner runs, 0 passes over 0 columns, 0 complete rounds`, com
`46 of 47 planner runs gave up before reaching the sweep` e o aviso
*"the sweep is not why: most cycles gave up before reaching it. Look at
what the planner refused, not at the sweep."* **Zero passagens em 47
tentativas.** A hipótese de orçamento de varredura foi descartada por
medição, não por leitura.

✅ **Resolvido em 2026-09-12:** o defeito latente do `CropPatch.survey` foi
confirmado com GameTest e corrigido. O canteiro vazio achado numa fatia
pausada agora é lembrado por colônia enquanto o `RingSweep` não fecha a
volta; a lembrança é validada contra o mundo antes de ser reaproveitada e
limpa junto com a varredura do fazendeiro.

✅ **Auditoria 1.9 registrada em 2026-09-12:** nenhum CRÍTICO novo foi
confirmado. O ALTO era de release: não havia workflow de CI/CD, e o
manifesto publicado aceitava qualquer Fabric API com `"*"`. Entrou
`.github/workflows/ci.yml` com actions pinados por SHA, Java 21,
unitários, Python, `build`, `runGametest`, artefato do jar e relatórios
em falha. O `fabric.mod.json` agora exige a Fabric API da matriz por
expansão do Gradle, e `ModMetadataTest` impede o curinga de voltar.
Registro completo em
[`docs/technical/Auditoria-2026-09-12.md`](docs/technical/Auditoria-2026-09-12.md).

---

## Playtest de 2026-09-14 — casa sem avanço visível

O log registra seleção e abertura de `plains_butcher_shop_2` às 00:50:15,
com 382 blocos. Dois foram assentados; a obra ficou com 380 em
`WAITING_RESOURCES` esperando `minecraft:dirt` por mais de seis minutos. Os
18 baús tinham 33 `grass_block` e nenhum `dirt`. A causa imediata era uma
lacuna na cadeia de produção: o catálogo reconhecia areia e grama, mas não
terra, então nenhum trabalho de superfície do fundidor atendia essa demanda.

**Corrigido e testado:** `DIRT` entrou no catálogo de recursos e no conversor
Vanilla; o fundidor busca terra exposta fora do raio protegido, no setor
cardinal definido para coleta externa, sem carregar chunks. GameTests cobrem
tipo/tarefa e o ciclo físico de quebrar e guardar terra. `build` passou e
`runGametest` passou com 320/320. **Ainda requer JAR atualizado e confirmação
em jogo:** coleta de terra, retomada dos 380 blocos e eventual bloqueio de
alcance observado depois da espera por material. Não foi alterada a escolha
da construção: o log prova que ela já foi aberta.

---

## 🎮 Sessão de 2026-09-13

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3`, em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu:**

- Casa parada no meio.
- Mineiro invisível / parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a
   `STABLE`, reduzindo o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco
faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A
carpintaria fabricava composteiras, mas usava a porta de consumo
(`ColonySupply.take`) e retirava do baú a peça que acabou de produzir.
Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`,
que fabrica e deixa a peça no estoque para o construtor assentar.

**Reinvestigação após o playtest:** novo GameTest reproduziu `minecraft:dirt`
sendo colocado sem estoque porque `BlockTags.DIRT` era tratado como terra
moldada no local. A exceção agora só cobre farmland, água, `dirt_path` e
cultivos. `runGametest` (313) e `build` passaram. **Pendente:** confirmar
em jogo com o JAR atualizado se a obra deixa de assentar terra sem material
e continua construindo; o log anterior veio de um JAR antigo e não registra
o bloco/posição exata da terra vista.

## Reanálise do playtest de 2026-09-14

O log usado pelo launcher veio de JAR anterior à correção de terra. As casas
foram selecionadas, mas aguardaram `dirt`; o mineiro estava sem tarefa, não
preso na mina: havia apenas 3 carvões e nenhuma reserva de carvão/ferro definida.
Lenhadores cortavam sem consultar a proteção de estruturas e podiam confundir
troncos de casas com árvore de copa compartilhada.

**Implementado neste ciclo:** metas de 64 carvões e 64 minérios de ferro brutos,
somadas à demanda da obra; proteção da árvore no planejamento e revalidação de
cada bloco antes de quebrar. Testes de jogo cobrem estrutura da colônia
registrada antes/depois do plano. Troncos manuais sem marca ou folha persistente
continuam indistinguíveis de árvores; proteção adicional exige decisão sobre
marcação/persistência.

**Verificado e distribuído:** `build` passou, 322/322 GameTests passaram, e
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/` têm SHA-256
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.

**Próximo passo: validar em jogo.** A casa deve avançar além de 380 blocos; o
mineiro deve abrir tarefas e manter os pisos minerais; o lenhador deve cortar
árvores naturais por inteiro e preservar estruturas Vanilla e da colônia.
Fazenda e demais profissões não têm falha comprovada neste log.

**Playtest seguinte, 2026-09-13:** nenhuma construção visível e baús de
profissões recebendo produção cruzada. O log associado mostra somente três
peças assentadas; construtor encerrou por falta de `grass_block` e
`cobblestone`; mineiros repetiram falta de espaço para ficar em pé e
produziram zero; lenhadores e agricultores colheram; fundidores pararam por
falta de areia. A produção agora vai ao baú pessoal do profissional, com
insumos compartilhados. Sem espaço, transformações devolvem o insumo e
drops ficam no mundo. `build` e 313 GameTests verdes; **aguarda validação em
jogo**. A investigação da construção e da mineração segue aberta.

**Diagnóstico do log de 09-13:** o mineiro completou 64/64 pedregulhos e
ficou sem tarefa; depois recebeu areia para vidro, mas `MinerWork.Job` usava
a pedra da paleta e mantinha o progresso em `0 of 3`. Corrigido e coberto
por teste; `build`: 810 unitários, `runGametest`: 314/314. Depois da correção,
a tarefa ainda depende de encontrar areia: o log diz repetidamente que não
há areia num raio de 48 blocos. **Aberto:** implementar/validar o retorno
explícito do mineiro à boca quando a tarefa termina; `MinerWork.tick` hoje
descarta o Job encerrado sem criar uma rota de volta.

A obra `plains_butcher_shop_2` esperou `grass_block` (382 blocos restantes),
desistiu e manteve lote/obra parcial. Nenhuma profissão do catálogo produz
`grass_block`. **Aberto:** impedir seleção/reserva de blueprint sem cadeia de
materiais possível e definir recuperação da obra parcial, respeitando a
propriedade do lote. Fazenda: sem cultivo maduro ou lote vazio em 32 blocos.
Lenhadores cortaram e produziram durante a mesma sessão, embora às vezes
tenham ficado sem árvore próxima. Carpinteiros, fundidores, pedreiros e
pastor registraram "no task open"; o carpinteiro também ficou no baú em
`0/20` ticks fora do expediente. Isso aponta falta de demanda/insumos no
recorte observado, não falha de execução confirmada dessas profissões.

**Alterações do jogador, aplicadas em 2026-09-13 (ADR-012):** quebras e
interações que mudam blocos agora invalidam a busca local de lotes; trechos
abertos pelo jogador reabrem braços já percorridos da mina. A busca continua
limitada por tick e sem forçar chunks. `build` e 314 GameTests verdes. **Na
próxima sessão, verificar retomada da casa e reabertura da mina.** ADR-013
resolveu E45 (geometria/ramais), sem cadastrar baús arbitrários: o estoque
é vivo nos baús já vinculados ao trabalhador e na boca da mina.

**E45 resolvido em 2026-09-13 (ADR-013):** no fundo da mina, quando todos os
braços terminam, a hélice muda de orientação sem mover a boca. A galeria
continua usando `turned()` para seus quatro braços. A geometria agora é
`SHAPE_VERSION` 5; saves v4 zeram os cursores e preservam entrada, rumo e
arco. Testes focados reproduziram as duas falhas antes da correção; `build`
passou com 808 unitários e `runGametest` com 314/314. Falta confirmar no
mundo do autor se o mineiro percorre e trabalha na nova hélice.

---

## 🔴 P0 — bloqueadores

Um por vez, teste antes de seguir.

| item | o quê | estado |
|---|---|---|
| **P0.1** | O planejador não acha lote | ✅ entregue 09-11 · **visto em jogo** |
| **P0.1-b** | O caminho de terra não sai de baú | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.1-c** | A recusa de lote diz por quê | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.3** | Mineiro → armazenamento → fundidor | ✅ conserto entregue 09-11 · ⬜ **espera sessão** |
| **P0.5** | Perda de item por inventário cheio (E3) | ✅ corrigido e testado · ⬜ **espera sessão**; depósito pessoal revisto em 09-13 |
| **P0.6** | A enxurrada da areia calou | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.7** | Elegibilidade simplificada de lotes | ✅ entregue 09-15 · ⬜ **espera playtest** |

---

## 🔴 Erros abertos

| | erro | estado |
|---|---|---|
| **E47** | **Trabalhador cai/fica preso longe do lote e nunca se recupera — visto em jogo 24-09, sessão de ~6h30.** `Builder 4b8df153` ficou parado em `-202, 62, -937` de **03:10 a 06:47** (97 linhas de `walking for N ticks without reaching the block`, subindo até 2400 e disparando o guarda de travamento repetidas vezes), sempre reatribuído à mesma obra em `y=72`, ~45-47 blocos de distância horizontal. `ClimbLimit`/`BuilderApproach.footOf` (correção de 09-16) só resolve diferença **vertical** de até 2 blocos quando o construtor está **na mesma coluna aproximada da obra** (em cima dela, descendo); não cobre um trabalhador preso **longe** — provavelmente dentro de depressão/ravina/caverna de superfície que a navegação Vanilla não atravessa sozinha para voltar. O guarda de 2400 ticks devolve a tarefa, mas o trabalhador continua fisicamente preso e é reatribuído à mesma armadilha. **Consequência observada:** nenhuma casa foi construída em ~6h30 de jogo (zero `house is up` no log inteiro, 5 colônias). Ver `docs/technical/Development-Log.md` para o diagnóstico completo. **Corrigido em código 24-09 (`e02fbf8`), decisão do autor:** dois congelamentos do `WorkStall` no mesmo ponto marcam o encalhado (`StrandedWorkers`); ele sai da escala em qualquer capacidade (`Worker.strand`, `WorkEligibility`) e cava uma escada de um bloco rumo ao centro da vila (`StrandedEscape`) — só terreno natural, via `BlockProtection`, sem abrir água nem deixar areia sobre a cabeça, entulho para o baú dele. `StrandedEscapeGameTest` 3/3. **Pendente:** teto de distância para "dar uma mão" (só chamar quem está perto da obra); origem das armadilhas não confirmada — nenhum código do mod cavou naqueles pontos, parecem terreno natural. | ✅ código e GameTest; ⬜ validar em jogo (procurar `is stranded at` / `dug a step` / `is out at`) |
| **E48** | **Alternância casa→infraestrutura nunca escolhe casa nesta sessão.** Colônia `78fa1bb4` planejou 5 posições distintas ao longo da sessão, **todas `plains_temple_4`** (nenhuma casa). `HousePlans.nextConstructionIsHouse` só considera obra **terminada** (`Building.finished()`); como o E47 acima impede qualquer obra de terminar, a alternância nunca chega a alternar — ela está correta no código, mas nunca é exercitada porque a causa raiz (E47) trava toda conclusão de obra antes. **Correção do diagnóstico, mesma data: o E48 tinha causa própria, não dependia só do E47.** O templo foi abandonado 13 vezes pelo caminho `lets go of` (`blamePlan=false`), e `lastFinished`/`lastNonHouseType` ignoravam obra abandonada — a vez nunca voltava à casa e o templo nunca era excluído. **Corrigido em código 24-09 (`03ea6fb`), decisão do autor:** obra abandonada conta no rodízio, e com mais adultos do que camas a próxima obra é casa (zero camas = ainda não contado). `HouseRotationGameTest`: com a regra desligada por mutação, abriu `plains_temple_4` — o mesmo do jogo. | ✅ código e GameTest; ⬜ validar em jogo (procurar `house is up`) |
| **E44** | A escada de recusas já existe em `MineMarks` e é consultada pela mineração e pela fronteira da galeria; há unitários e GameTests. O playtest ainda observou o mineiro parado, então a integração completa segue **sem validação em jogo**. Não reabrir a decisão original sem reproduzir um defeito residual. | ⬜ validar em jogo |
| **E43** | A decisao 1A foi implementada: `WorkEligibility` impede reserva de capacidade em descanso e removeu o fallback que devolvia `COLLECT_STONE` no ciclo seguinte. `WorkAssignmentTest` teve fase vermelha, e `ColonyCycleGameTest.aRestingMinerLeavesTheStoneTaskAvailable` passou na bateria 419/419. | ✅ codigo e GameTest; ⬜ validar em save |
| **E41** | Nada mede degradação ao longo de muitos ciclos. O teste mais longo do projeto tem centenas de tiques. **Fechado em 11-09 (P1.13), esta linha estava desatualizada.** `ColonyEnduranceGameTest` mede deriva em 200 ciclos; nunca falhou em onze rodadas completas nesta sessão (24-09). | ✅ fechado |
| **E42** | Nenhum teste de impasse entre profissões. Os dois casos reais — a roça que travava toda a construção, e o fabricante que nunca descascava — foram achados **em jogo**, não pela bateria. **A tentativa de 09-09 à noite foi retirada pelo gauntlet-verifier** — os três casos escritos eram a invariante de vários trabalhadores, com outro nome. **O trabalho de verdade é outro gametest:** lote de roça fora do alcance do fazendeiro, planta de casa disponível, duas passagens do planejador, e a segunda tem de abrir projeto de CASA | 🔴 aberto |
| **E38** | O baú pessoal pode assorear com vara, maçã e muda sem consumidor; a colheita não transborda para outra profissão e itens sem espaço viram drops no mundo. Definir tratamento sustentável dos resíduos sem misturar depósitos | ⚙️ aberto |
| **KF-001** | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard`. **Fechado em 09-09:** a afirmação lia o estado da tarefa depois que o ciclo podia reservá-la novamente; o teste passou a registrar o instante da devolução e força a fase do ciclo. O orçamento global de uma busca/tique continua sendo um risco separado de vazão, não a causa provada da falha. | ✅ teste corrigido; medir vazão se houver evidência |
| **E21** | `theStoneLeavesTheWorldAndReachesTheChest` disse "a pedra não chegou ao baú" uma vez. Suspeita: custo de ler estrutura no tique. **Fechada em 24-09 por falta de evidência:** dez rodadas completas de `runGametest --rerun-tasks` (~4.220 execuções do teste) nesta sessão, zero falhas. Sem recorrência para investigar. | ✅ fechada, sem evidência |
| **E4** | `path held: no` e o aldeão chega assim mesmo. Provável, nunca verificado. **Fechada em 24-09 por obsolescência:** o log que gerou a suspeita (commit `379f1dd`, 2026-08-08) foi removido; o mecanismo de aproximação do lenhador que ele diagnosticava foi substituído por `WorkStall`/`LumberjackReport`/`TreeChoice.stallLimit`. A suspeita ficou órfã de um sistema que não existe mais no código atual. | ✅ fechada, obsoleta |
| **E3** | Sobra de colheita é perda de item. **Metade fechada em 09-04** — o lenhador deixou de destruir; **o mineiro continua sem teto de inventário** | ⚙️ metade fechada |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte. **Mitigado em 09-13** — precisa de duas leituras positivas seguidas | ⚙️ mitigado |

---

## 🟠 Pendências abertas

| | o quê |
|---|---|
| **Mina de vila nova sem portal visível** | Confirmado no escalonador: uma única busca global por tique ficava presa no primeiro trabalho sem alvo, impedindo os mineiros seguintes de iniciar a busca. O rodízio foi corrigido sem aumentar a cota; `build` e 314 GameTests verdes. **Aguardar validação em jogo**: portal aparece e o mineiro inicia a escavação. |
| **Terra comum em blueprint** | Corrigido localmente: agora exige estoque. 313 GameTests e build verdes; **aguarda validação em jogo**. |
| **P0.7 — elegibilidade simplificada de lotes** | ✅ entregue 09-15; piso solido e elegivel, estrada exige `ROAD_AREA`; 327/327 GameTests e JAR atualizado. Espera playtest. |
| **A casa ainda sobe com a barreira de teste** | Última medição: 47 de 169 peças em 09-04. É o item que fecha a Fase 2 de verdade |
| **O lenhador rejeita as paredes da própria vila** | 118 rejeições sobre 28 posições. O castigo escalona (6.000→48.000) e funciona, mas ele redescobre a mesma parede seis vezes. **Nenhuma recusa veio com o número 24** |
| **O fundidor não tem o que fundir** | `nothing in the colony chests to smelt`, 34× na sessão de 09-04. Deve seguir o E44 |
| **Cobertura de qualquer material de construção** | ADR-015: fallback para fundidor e criador e estoque limitado pela capacidade física. Em 09-14, contagem e localização por `ResourceId` entraram em `ResourceTally`/`ColonyResources` e no leitor de baús (317 GameTests verdes). Metas, tarefas e execução profissional para IDs arbitrários continuam pendentes; não declarar cobertura funcional ainda. ADR-016, Lote 1. |
| **Mineiro longe do corredor não tem resgate** | Sem posição da ordem a uma perna dele, não há passo a dar e a boca continua sendo a resposta. Decisão de projeto: caminhar em linha reta, ou devolver a tarefa |
| **A arena de gametest não hospeda a galeria** | Ela assenta no fundo do mundo. Toda a bateria de mineração exercita **só o poço**. A divergência dos ramais é provada por unitário, não de ponta a ponta |
| **Os 49 `assign()` que criam trabalhador de mãos vazias** | Passam hoje por folga no `tickLimit`, não por estarem certos |
| **A proteção não consulta o registro de construções em todos os caminhos** | `BlockProtection` passou a consultar (`isColonyBuilt`), mas o caminho que decide o que pode ser quebrado ainda tem furos |
| **`Colony cycle took 122 ms`** | Contra o limite de 50. Medido em 09-11. **Instrumentar antes de otimizar** — ver P2.1 do plano |

---

## 📋 Pendências por nível de progressão lógica

A ordem é de dependência: cada nível precisa do anterior de pé.

### Nível 0 — o que já roda em jogo

Detecção · identidade estável · aldeões e profissões · baús · lenhador **em cinco espécies** · fabricante, **inclusive descascando e fazendo o que a obra pede dois degraus abaixo** · construtor **chegando ao bloco** · centro parado pela sonda · **a rua crescendo** · **a obra parada saindo da frente** · **a mina abrindo e sendo mobiliada** · **o mineiro cavando** · **o pastor tosquiando** · casa terminada.

### Nível 1 — a raiz do material *(aberta, e ela se fecha sozinha)*

- **A mina abriu em 2026-08-26** — a busca acertou na primeira camada.
- **O mineiro cavou** — 43 blocos numa tarefa só, descendo até y≈44.
- **A galeria o engoliu, e o E30 fechou em 08-27**.
- **Continuar ainda não está provado em jogo**. O conserto tem teste e **nenhuma sessão o viu rodar**. E o **E32** ficou de pé.
- **A pedra de superfície continua sem prova** — nunca foi exercitada.

### Nível 2 — material processado *(feito, e ainda passando fome)*

- **E18 fechado em 08-22**, pelo caminho genérico que a ADR pediu.
- **Nenhuma sessão viu isso rodar** — a linha a procurar é `Smelter ... made minecraft:smooth_sandstone`.
- O fundidor espera **a areia**, e a cadeia da areia começou em 08-20.

### Nível 3 — a obra termina sem o jogador

- **A casa terminou sozinha em 2026-08-26** — 149 blocos planejados, **127 assentados**, em 4 min 57 s.
- **Mas dezenove peças foram da barreira**, não da colônia. *Casa feita inteira com material da própria colônia* **continua sem prova**.
- **A barreira risca antes de a cadeia ter chance**. Enquanto a Regra 28 valer, a soma da sessão superestima o que está quebrado.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
- ✅ **A varredura recomeçar a cada obra fechada** — resolvido em 08-27, o cursor fica onde achou o lote.
- ✅ **Perguntar só às ruas** — resolvido em 08-27 pelo índice.
- ✅ **O cursor da varredura** — resolvido em 08-27, e **confirmado em jogo**.
- ✅ **O veio que desce** — resolvido em 08-27.
- 🟠 **O mineiro cavando de verdade ainda não foi visto em jogo.** **Falta a sessão que confirme.**
- 🟠 **Mineiro longe demais não caminha até a mina.** Não investigado.

### Nível 5 — o motor da ADR-009

`VillageProfile` · inventário de território · escassez e distância · orçamento de recursos · detecção de dependência circular · reserva mínima de sobrevivência · objetivos graduais. **Nada disso existe.**

### Nível 6 — o que nem modelo tem

Comida · água · o fazendeiro (tem enxada e baú desde a Fase 4 e nunca teve código) · população por capacidade · defesa · especialização · comércio entre vilas.

### Fora dos níveis — dívida que não bloqueia

- **13 arquivos de código acima de 500 linhas**, e 11 de teste. `VillageDetectionHandler` é o pior com **1.107**, e o corte dele é o próximo.
- **ADR-008** (orientação) e **ADR-007** (fusão), decididas e por escrever.
- **Regra 16** — distância mínima e máxima entre construções.
- **O ícone** — 1,95 MB num jar de 2,29 MB.
- **Cenário de teste por bioma.** A planície escondeu **duas vezes** que o deserto estava quebrado.
- **O `Development-Log`** está atualizado até 09-15. Cada lote futuro deve registrar ali a evidência, o escopo e o artefato distribuído.

---

## ⚠️ Incompatibilidades — o que se contradiz hoje

| | o quê |
|---|---|
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |

---

## 👤 Decisões que faltam, na ordem em que travam

| | decisão | trava |
|---|---|---|
| 1 | **E43 — o descanso de 4 ciclos deve valer sempre?** | Anulado pela 2ª passagem do `takeOneTask`. Decisão de projeto |
| 2 | **TASK-048 — o que uma colônia ABANDONED deixa de fazer?** | Hoje nada. Ela é marcada e continua sendo simulada |
| 3 | **TASK-044 — a fusão de vilas** | ADR-007 escrita em 08-21, não implementada |
| 4 | **TASK-046 — a orientação dos blocos** | ADR-008 escrita em 08-21, forma (a). Metade do E8 fechou em 08-15; a orientação fica |
| 6 | **E38 — o baú do trabalhador assoreia** | Dar consumidor ou descarte a vara, maçã e muda. **Decisão de projeto** |
| 7 | **E45 — como a mina troca de rota no fundo?** | Geometria, boca estável, migração do save e novo GameTest; não há ADR atual |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto.

| | o quê | a linha que prova |
|---|---|---|
| **1** | **Portal da mina em vila recém-descoberta** | log `opens a mine at ...`; confirmar entrada visível e mineiro iniciando trabalho |
| **1** | **A mina velha destravando** | `Mine ... finished every branch and went one level deeper` |
| **1** | **A varredura acabando num ciclo** | `no building work: still sweeping` aparecendo **uma vez** e não a sessão inteira |
| **2** | **P0.1-b, P0.1-c, P0.3, P0.5, P0.6** | entregues em 09-11, **nunca vistos** |
| **2** | **As quatro correções de 09-13** | sessão do autor |
| **2** | **O mineiro parando à noite** | o contador de `stall` **congelado** com `off hours` |
| **2** | **A ferramenta de ferro na mão** | ferro, e não madeira nem diamante |
| **2** | **Profissões e crescimento (ADR-011)** | sete funções produtoras, ordem de vagas nos adultos 15/16/30/31/32 e produtor construindo sem perder o ofício |
| **2** | **O nome colorido** | sete cores distintas para as profissões ativas, e o nome do jogador **sem** cor |
| **2** | **O arco da boca** | dois pilares, verga e lanterna — **junto** com o baú do mineiro |
| **2** | **Coleta de superfície do fundidor (ADR-014)** | pá com Toque Suave I; areia no baú pessoal e `grass_block` só com obra solicitando, além de 64 blocos e longe das estruturas |
| **3** | **A cadeia da areia inteira** | meta de vidro → fundidor busca areia → vidro → vidraça |
| **4** | **A casa inteira sem a barreira** | `TEST BARRIER covered for nothing` |
| **5** | **A rua crescendo e a casa nascendo junto** | `extended the road N blocks ...` seguido de `planned ... at ...` no mesmo ciclo |
| **6** | **A casa de deserto subindo** | `blocks left` caindo de 113 |
| **7** | **Fechar e reabrir o mundo com mina aberta** | a **galeria retomada** |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície — taiga, savana, nevada e deserto nunca rodaram, e todas as sessões até hoje foram em planície.

---

## ⚙️ Ciclo de 2026-09-11 — recusados

**Três itens do plano não se fazem**, e os três pelo mesmo motivo: o plano foi escrito a partir deste arquivo, e as linhas que ele copiou **já estavam vencidas**.

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto**: é o primeiro critério de ordenação da obra, e é o que põe mobília depois da casa inteira |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |
| **P1.7** | Separar `WOOD` por espécie **refaria o defeito de 09-10** |

---

## 📌 Notas do ciclo de 09-12

### ✅ 2026-09-12 — duas sessões de jogo, dois defeitos de "uma vez só"

**O arco da mina voltava depois de quebrado.** `Mine.archRaised` entrou no save sob a chave `arch`, **fora do `SHAPE_VERSION` de propósito**. O critério levou três reprovações do `gauntlet-verifier`, e as três viraram gametest.

**O lenhador deixava tronco de pé — e era o E39.** A causa não era alcance. **E o ciclo vicioso que ninguém havia medido:** expulsão deixa tora de pé → `markUnreachable` pula a árvore por 6.000 tiques → a copa decai nesse tempo → o resto vira `N logs without a living canopy`, que é recusa **definitiva**.

**Verificado:** 780 unitários e 304 gametests. Cada um dos quatro testes novos foi visto falhando contra a versão que acusa.

---

## ⚙️ Ciclo de 2026-09-10 — recusados

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto** |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |

---

## 📎 Referências rápidas

- **Plano de correção:** [`docs/technical/Plano-de-Correcao.md`](docs/technical/Plano-de-Correcao.md)
- **Histórico por data:** [`TODO-archive.md`](TODO-archive.md)
- **Estado vivo:** [`STATE.md`](STATE.md)
- **Assinaturas de defeito:** [`docs/PATTERNS.md`](docs/PATTERNS.md)
- **Regras do autor:** [`docs/RULES.md`](docs/RULES.md)
- **Estado detalhado (histórico):** [`docs/technical/Project-State.md`](docs/technical/Project-State.md)
- **Próxima sessão de jogo:** [`docs/proxima-sessao.md`](docs/proxima-sessao.md)
- **Responsabilidade das profissões:** [`docs/technical/Profession-Responsibility.md`](docs/technical/Profession-Responsibility.md)
- **Regressões catalogadas:** [`docs/behavioral-tests/REGRESSION-HISTORY.md`](docs/behavioral-tests/REGRESSION-HISTORY.md)
- **Falhas conhecidas:** [`docs/behavioral-tests/known-failures.md`](docs/behavioral-tests/known-failures.md)

---

## Nota sobre este arquivo

**Regra de manutenção:** quando um item fecha, ele **sai** daqui e vai para o `TODO-archive.md`, na seção `✅ Resolvido` do mês correspondente. Quando um item novo abre, ele entra na seção apropriada — **nunca** em bloco de "ciclo".

**Meta de tamanho:** 150 linhas. Se passar, algo está sendo arquivado no ritmo errado.

**O que este arquivo não é:** não é log de sessão, não é histórico de decisão, não é lugar de guardar "como se chegou aqui". Isso vai para `Development-Log.md` e `TODO-archive.md`.
